package com.peaceray.quantro.model.communications;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.peaceray.quantro.adapter.action.ActionAdapter;
import com.peaceray.quantro.adapter.action.ActionAdapter.RealtimeData;
import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.model.descriptors.versioned.ActionCycleStateDescriptor;
import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.ThreadSafety;

public class MultiplayerClientCommunications extends ClientCommunications
		implements MessagePassingConnection.Delegate {
	
	private static final String TAG = "MultiplayerClientCommunications" ;
	
	
	// Orders from above!  Connect or disconnect our connections.
	private static final int ANDROID_MESSAGE_TYPE_CONNECT 							= 0 ;
	private static final int ANDROID_MESSAGE_TYPE_STOP 								= 1 ;
	
	// The MessagePassingConnection status updated.
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED 				= 4 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_FAILED					= 5 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_BROKE					= 6 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_PEER_DISCONNECTED		= 7 ;
	
	// The MessagePassing layer has a message for us!
	private static final int ANDROID_MESSAGE_TYPE_MESSAGE_RECEIVED 					= 8 ;
	private static final int ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES 			= 9 ;
	
	// Events related to local actions in the game.
	private static final int ANDROID_MESSAGE_TYPE_DID_ENQUEUE_LOCAL_ACTIONS			= 10 ;
	
	// Send a realtime update!
	private static final int ANDROID_MESSAGE_TYPE_SEND_REALTIME_UPDATE 				= 11 ;
	
	// Give us a cycle update!
	private static final int ANDROID_MESSAGE_TYPE_REQUEST_ACTION_CYCLE_STATE		= 12 ;
	
	
	// Events related to the current Activity status.
	private static final int ANDROID_MESSAGE_TYPE_SEND_PAUSE_REQUEST 				= 13 ;
	private static final int ANDROID_MESSAGE_TYPE_SEND_NAME							= 14 ;
	
	// Handle broken or other changes
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE		= 15 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED = 16 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION = 17 ;
	
	private static final int NUM_ANDROID_MESSAGE_TYPES								= 18 ;
	
	
	private static final long DELAYED_HANDLE_DELAY = 5000 ;
	
	private static final long REALTIME_UPDATE_EVERY = 5000 ;		// every 5 seconds.
			// we have a chance to send a "realtime update" message to the server
			// with every move queue.  We send this update if the game has moved
			// from below 1 to >= 1 displaced rounds, or if this amount of time has
			// passed since the last update.
	
	private static final long REQUEST_ACTION_CYCLE_STATE_FIRST_AFTER = 600 ;	// after 0.6 seconds
	private static final long REQUEST_ACTION_CYCLE_STATE_EVERY = 3000 ;		// every 3 seconds
	
	
	private static final long ACTION_QUEUE_SEND_DELAY = 800 ;		// we allow actions to queue for this
																	// amount of time (unless an end-of-cycle occurs).
	private static final int ACTION_QUEUE_MAX_LENGTH = 14 ;			// we allow up to this many actions to "build up" before
																	// an immediate send.
	
	
	class MultiplayerClientCommunicationsThread extends Thread {
		
		public static final long millisecondsBetweenUpdates = 10 ;
		
		public boolean running ;
		public boolean welcomed ;
		
		boolean [] playerWaitingFor ;
		boolean [] playerPaused ;
		
		GameMessage outgoingMessage ;
		Handler handler ;
		
		long lastRealtimeUpdate = 0 ;
		
		long [] actionCycleStateMostRecentId ;
		protected ActionCycleStateDescriptor [] actionCycleStateMostRecent ;
		
		public MultiplayerClientCommunicationsThread( ) {
			running = true ;
			
			outgoingMessage = new GameMessage() ;
			
			// TODO: Just 8?
			playerWaitingFor = new boolean[8] ;
			playerPaused = new boolean[8] ;
			
			actionCycleStateMostRecentId = new long[8] ;
			actionCycleStateMostRecent = new ActionCycleStateDescriptor[8] ;
			for ( int i = 0; i < 8; i++ )
				actionCycleStateMostRecent[i] = new ActionCycleStateDescriptor(1, 1) ;
		}
		
		
		@Override
		public void run() {
			
			/*
			int status ;
			int prevStatus = connection.connectionStatus() ;
			*/
			
			// You have to prepare the looper before creating the handler.
	        Looper.prepare();
	        
	        // Create the handler so it is bound to this thread's message queue.
	        handler = new Handler() {
	        	
	        	
	        	private void removeAllMessagesRegardingConnection() {
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_CONNECTION_FAILED) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_CONNECTION_BROKE) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_CONNECTION_PEER_DISCONNECTED) ;
	        		
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_MESSAGE_RECEIVED) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES) ;
	        		
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_DID_ENQUEUE_LOCAL_ACTIONS) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_SEND_REALTIME_UPDATE) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_REQUEST_ACTION_CYCLE_STATE) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_SEND_PAUSE_REQUEST) ;
	        		handler.removeMessages(ANDROID_MESSAGE_TYPE_SEND_NAME) ;

            		handler.removeMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE) ;
            		handler.removeMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED) ;
            		handler.removeMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION) ;
	        	}
	        	
	        	
	        	 public void handleMessage(android.os.Message msg) {
	            	int type = msg.what ;
	            	
	            	android.os.Message selfMessage ;
	            	
	            	try {	// sometimes delegate gets set to null, causing a crash here.
		            	switch( type ) {
		            	case ANDROID_MESSAGE_TYPE_CONNECT:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_CONNECT") ;
		            		
		            		// If currently connected, send an I AM CLIENT message.
		            		// (probably this Connection has been passed in in a Connected state
		            		welcomed = false ;
		            		for ( int i = 0; i < actionCycleStateMostRecentId.length; i++ )
		            			actionCycleStateMostRecentId[i] = -1 ;
		            		
		            		if ( connection.isConnected() ) {
		            			// No need to send the coordinator our info yet;
		            			// we will send "pause" or "unpause" in response to the 
		            			// welcome message.
		            			delegate.ccd_connectionStatusChanged(MultiplayerClientCommunications.this, MessagePassingConnection.Status.CONNECTED) ;
		            		}
		            		else {
		            			removeAllMessagesRegardingConnection() ;
		            			if ( connection.isAbleToDisconnect() ) {
		            				connection.disconnect() ;
		            				delegate.ccd_connectionStatusChanged(MultiplayerClientCommunications.this, MessagePassingConnection.Status.DISCONNECTED) ;
		            			}
		            			//Log.d(TAG, "connecting!") ;
		            			connection.connect() ;
		            			//Log.d(TAG, "new status is " + connection.connectionStatus()) ;
		            			delegate.ccd_connectionStatusChanged( MultiplayerClientCommunications.this, MessagePassingConnection.Status.PENDING ) ;
		            		}
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_STOP:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_STOP") ;
		            		// Shut.  Down.  Everything.
		            		if ( connection.isConnected() )
		            			connection.sendMessage( outgoingMessage.setAsExit() ) ;
		            		// wait a tick!
		            		try {
								Thread.sleep(200) ;				// 1/5th of a second
							} catch (InterruptedException e1) { }
		            		if ( connection.isAbleToDisconnect() )
		            			connection.disconnect() ;
		            		getLooper().quit();
		            		running = false ;
		            		break ;
		            	
		            	// The MessagePassingConnection status updated.
		            	case ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED") ;
		            		// We connected!  But we weren't welcomed.  Send an I AM CLIENT message
		            		// and tell the delegate
		            		//Log.d(TAG, "connected!") ;
		            		delegate.ccd_connectionStatusChanged(MultiplayerClientCommunications.this, MessagePassingConnection.Status.CONNECTED) ;
		            		welcomed = false ;
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_CONNECTION_FAILED:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_FAILED") ;
		            		// Whoops.  Inform the delegate, but do NOT attempt to reconnect.
		            		// We will soon receive a call to DONE_RECEIVING_MESSAGES.
		            		delegate.ccd_connectionStatusChanged( MultiplayerClientCommunications.this, MessagePassingConnection.Status.FAILED ) ;
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_CONNECTION_BROKE:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_BROKE") ;
		            		// Whoops.  Inform the delegate, but do NOT attempt to reconnect.
		            		// We will soon receive a call to DONE_RECEIVING_MESSAGES.
		            		delegate.ccd_connectionStatusChanged( MultiplayerClientCommunications.this, MessagePassingConnection.Status.BROKEN ) ;
		            		selfMessage = handler.obtainMessage(
		            				ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE) ;
		            		handler.sendMessageDelayed(selfMessage, DELAYED_HANDLE_DELAY) ;
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_CONNECTION_PEER_DISCONNECTED:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_DISCONNETED") ;
		            		// Whoops.  Inform the delegate, but do NOT attempt to reconnect.
		            		// We will soon receive a call to DONE_RECEIVING_MESSAGES.
		            		delegate.ccd_connectionStatusChanged( MultiplayerClientCommunications.this, MessagePassingConnection.Status.PEER_DISCONNECTED ) ;
		            		selfMessage = handler.obtainMessage(
		            				ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED) ;
		            		handler.sendMessageDelayed(selfMessage, DELAYED_HANDLE_DELAY) ;
		            		break ;
		            	
		            	// The MessagePassing layer has a message for us!
		            	case ANDROID_MESSAGE_TYPE_MESSAGE_RECEIVED:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_MESSAGE_RECEIVED") ;
		            		if ( connection.connectionStatus() == MessagePassingConnection.Status.DISCONNECTED )
		            			break ;	// do nothing
		            		if ( !connection.moreMessages() || !connection.hasMessage() ) {
		            			// Received a message, but no messages to get?  WTF?  Kill.
		            			// schedule a disconnect
		            			handler.sendMessageDelayed( handler.obtainMessage(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION),
		            					2000) ;
		            			break ;
		            		}
		            		// Handle the incoming message.
		            		GameMessage m = (GameMessage) connection.getMessage() ;
		            		boolean ok = false ;
		            		try {
		            			ok = handleIncomingMessage( m ) ;
		            		} catch( Exception e ) {
		            			e.printStackTrace() ;
		            			//Log.d(TAG, "Exception caught " + e.getMessage() ) ;
		            		}
		            		if ( !ok ) {
		            			// disconnect!
		            			//Log.d(TAG, "did not handle message; disconnecting") ;
		            			delegate.ccd_connectionReceivedIllegalMessage(MultiplayerClientCommunications.this) ;
								connection.disconnect() ;
								delegate.ccd_connectionStatusChanged(MultiplayerClientCommunications.this, MessagePassingConnection.Status.DISCONNECTED) ;
								// queue up a reconnect
								handler.sendMessageDelayed(
										handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECT),
										delegate.ccd_requestDelayUntilConnect(MultiplayerClientCommunications.this) ) ;
		            		}
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES:
		            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE:
		            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED:
		            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION:
		            		removeAllMessagesRegardingConnection() ;
		            		//if ( type == ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES )
		            		//	//Log.d(TAG, "ANDROID_MESSAGE_TYPE_DONE_RECEIVING") ;
		            		//else if ( type == ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE )
		            		//	//Log.d(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE") ;
		            		//else if ( type == ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED )
		            		//	//Log.d(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED") ;
		            		
		            		// Everything's done.  Disconnect, tell the delegate, and queue up a reconnect.
		            		if ( connection.connectionStatus() != MessagePassingConnection.Status.DISCONNECTED ) {
		            			// Cut the connection; queue up a reconnect.
		            			connection.disconnect();
		            			delegate.ccd_connectionStatusChanged(MultiplayerClientCommunications.this, MessagePassingConnection.Status.DISCONNECTED) ;
			            		handler.sendMessageDelayed( handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECT ), delegate.ccd_requestDelayUntilConnect(MultiplayerClientCommunications.this) ) ;
		            		}
		            		break ;
		            	
		            	// Events related to local actions in the game.
		            	case ANDROID_MESSAGE_TYPE_DID_ENQUEUE_LOCAL_ACTIONS:
		            		//Log.d(TAG, "ANDROID_MESSAGE_TYPE_DID_ENQUEUE_LOCAL_ACTIONS") ;
		            		// Read the entire action queue.  If there are more than zero
		            		// actions, send them to the coordinator.  If we aren't connected,
		            		// just read the queue and drop it on the floor.
		            		// First: remove any other ENQUEUE_LOCAL_ACTIONS queued on the Handler.  If
		            		// any exist, the data to be read from them will be read by this event.
		            		handler.removeMessages(ANDROID_MESSAGE_TYPE_DID_ENQUEUE_LOCAL_ACTIONS) ;
		            		// Second: send all the information.
		            		if ( lastRealtimeUpdate + REALTIME_UPDATE_EVERY < System.currentTimeMillis() )
		            			sendRealtimeUpdate() ;
		            		int numActions = actionAdapters[localActionAdapter].communications_readOutgoingActionQueue(buffer, 0, buffer.length) ;
		            		if ( numActions > 0 && connection.isConnected() ) {
		            			outgoingMessage.setAsMoveQueue(localActionAdapter, buffer, 0, numActions) ;
		            			connection.sendMessage(outgoingMessage) ;
		            		}
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_SEND_REALTIME_UPDATE:
		            		sendRealtimeUpdate() ;
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_REQUEST_ACTION_CYCLE_STATE:
		            		sendActionCycleStateRequest( msg.arg1 ) ;
		            		break ;
		            	
		            	// Events related to the current Activity status.
		            	case ANDROID_MESSAGE_TYPE_SEND_PAUSE_REQUEST:
		            		if ( connection.isConnected() ) {
		            			if ( pausedByLocalPlayer )
		            				outgoingMessage.setAsPause() ;
		            			else
		            				outgoingMessage.setAsUnpause() ;
		            			connection.sendMessage(outgoingMessage) ;
		            		}
		            		break ;
		            		
		            	case ANDROID_MESSAGE_TYPE_SEND_NAME:
		            		if ( connection.isConnected() ) {
		            			outgoingMessage.setAsMyName(name) ;
		            			connection.sendMessage(outgoingMessage) ;
		            		}
		            	}
	            	} catch ( Exception e ) {
	            		e.printStackTrace() ;
	            	}
	        	 }
	        } ;
	        
	        /*
	         * Start looping the message queue of this thread.
	         */
	        //Log.d(TAG, "MultiplayerClientCommunications entering loop.") ;
	        Looper.loop();
			
			// Finished.
			//Log.d(TAG, "MultiplayerClientCommunications leaving loop") ;
		}
		
		
		/**
		 * Handles the incoming message, possibly by sending 
		 * a response.  Will alter the contents of the field 'message'.
		 * @param m The incoming message.  WILL NOT ALTER THE CONTENT OF THIS OBJECT.
		 * 
		 * @return Whether we successfully handled the message.
		 */
		private boolean handleIncomingMessage( GameMessage m ) {
			
			String text ;
			ActionCycleStateDescriptor acsd ;
			switch( m.getType() ) {
			
			// Message type.
			case Message.TYPE_UNKNOWN:
			case Message.TYPE_EXIT:
			case Message.TYPE_NONCE:
			case Message.TYPE_PERSONAL_NONCE:
			case GameMessage.TYPE_PAUSE:
			case GameMessage.TYPE_UNPAUSE:
			case GameMessage.TYPE_FULL_SYNCHRONIZATION_APPLIED:
				//Log.d(TAG, "Incoming message: nothing to do with this. " + m.getType() ) ;
				return false ;
			
			case Message.TYPE_KICK:
				//Log.d(TAG, "Incoming message: kick" ) ;
				delegate.ccd_messageKick(MultiplayerClientCommunications.this, m.getPlayerSlot(), m.getText()) ;
				break ;
				
			case Message.TYPE_KICK_WARNING:
				// //Log.d(TAG, "Incoming message: kick warning") ;
				delegate.ccd_messageKickWarning(MultiplayerClientCommunications.this,
						m.getPlayerSlot(), m.getText(), System.currentTimeMillis() + m.getWarningDuration()) ;
				break ;
				
			case Message.TYPE_KICK_WARNING_RETRACTION:
				//Log.d(TAG, "Incoming message: kick warning retraction") ;
				delegate.ccd_messageKickWarningRetraction(
						MultiplayerClientCommunications.this,
						m.getPlayerSlot()) ;
				break ;
			
			case Message.TYPE_NONCE_REQUEST:
				// Send our nonce in return.
				//Log.d(TAG, "Incoming message: nonce request" ) ;
				connection.sendMessage(outgoingMessage.setAsNonce(nonce)) ;
				break ;
			
			case Message.TYPE_PERSONAL_NONCE_REQUEST:
				// Send our nonce in return.
				//Log.d(TAG, "Incoming message: personal nonce request" ) ;
				connection.sendMessage(outgoingMessage.setAsPersonalNonce( localActionAdapter, personalNonce)) ;
				break ;
			
			case Message.TYPE_NAME_REQUEST:
				//Log.d(TAG, "Incoming message: name request" ) ;
				connection.sendMessage(outgoingMessage.setAsMyName( name )) ;
				break ;
			
			case Message.TYPE_PLAYER_NAME:
				text = m.getName() ;
				if ( text.length() > 32 )		// HACK to sanitize name length
					text = text.substring(0,32) ;
				//Log.d(TAG, "Incoming message: name for " + m.getPlayerSlot() + " is " + text ) ;
				delegate.ccd_messagePlayerName( MultiplayerClientCommunications.this, m.getPlayerSlot(), text ) ;
				break ;
			
			case Message.TYPE_TOTAL_PLAYER_SLOTS:
				//Log.d(TAG, "Incoming message: total player slots" ) ;
				delegate.ccd_messageTotalPlayerSlots( MultiplayerClientCommunications.this, m.getNumPlayerSlots() ) ;
				break ;
				
			case Message.TYPE_PERSONAL_PLAYER_SLOT:
				//Log.d(TAG, "Incoming message: player slot" ) ;
				localActionAdapter = m.getPlayerSlot() ;
				actionAdapters[localActionAdapter].set_gameShouldUseTimingSystem(true) ;
				actionAdapters[localActionAdapter].set_dequeueActionsDiscards(true) ;
				for ( int i = 0; i < actionAdapters.length; i++ ) {
					if ( i != localActionAdapter && actionAdapters[i] != null ) {
						actionAdapters[i].set_gameShouldUseTimingSystem(false) ;
						actionAdapters[i].set_dequeueActionsDiscards(false) ;
					}
				}
				delegate.ccd_messageLocalPlayerSlot( MultiplayerClientCommunications.this, localActionAdapter ) ;
				break ;
			
			case Message.TYPE_WELCOME_TO_SERVER:
				//Log.d(TAG, "Incoming message: welcome to server" ) ;
				// Let the server know whether we want to pause or not...
				welcomed = true ;
				if ( pausedByLocalPlayer )
					connection.sendMessage( outgoingMessage.setAsPause() ) ;
				else
					connection.sendMessage( outgoingMessage.setAsUnpause() ) ;
				// Tell the delegate
				delegate.ccd_messageWelcomeToServer(MultiplayerClientCommunications.this) ;
				break ;
			
			case Message.TYPE_SERVER_CLOSING:
			case Message.TYPE_SERVER_CLOSING_FOREVER:
				//Log.d(TAG, "Incoming message: closing" ) ;
				// For now, we don't distinguish between these.
				delegate.ccd_messageServerClosingForever(MultiplayerClientCommunications.this) ;
				break ;
			
			case Message.TYPE_PLAYER_QUIT:
				//Log.d(TAG, "Incoming message: quit" ) ;
				delegate.ccd_messageQuit( MultiplayerClientCommunications.this, m.getPlayerSlot(), m.getFatal() ) ;
				break ;
				
			case Message.TYPE_HOST:
				//Log.d(TAG, "Incoming message: host name" ) ;
				delegate.ccd_messageHost( MultiplayerClientCommunications.this, m.getPlayerSlot(), m.getName() ) ;
				break ;
			
			case GameMessage.TYPE_FULL_SYNCHRONIZATION:
				//Log.d(TAG, "Incoming message: full sync for player " + m.getPlayerSlot() ) ;
				// A diffitcult one!  Apply the synchronization,
				// making sure to synchronize() on the game objects.
				Game game = delegate.ccd_requestGameObject(MultiplayerClientCommunications.this, m.getPlayerSlot()) ;
				if ( game == null ) {
					//Log.d(TAG, "Server sent FullSync message for a Game we don't have: " + m.getPlayerSlot()) ;
					return false ;
				}
				synchronized(game) {
					// perform the sync, and empty all move, cycle, and attack queues from the adapter.
					// Before we apply the synchronization, we clear out any action queues.
					m.getFullSynchronization(game) ;
					actionAdapters[m.getPlayerSlot()].emptyAllQueues() ;
					// tell Achievements
					if ( m.getPlayerSlot() == localActionAdapter )
						Achievements.game_fullSynchronization() ;
				}
				// reset our action cycle state for this game
				actionCycleStateMostRecentId[m.getPlayerSlot()] = -1 ;
				// Tell the server we synchronized.
				connection.sendMessage(
						outgoingMessage.setAsFullSynchronizationApplied(
								m.getPlayerSlot(),
								m.getMessageId() ) ) ;
				break ;
			
			case GameMessage.TYPE_WAITING_FOR_PLAYERS:
				//Log.d(TAG, "Incoming message: waiting" ) ;
				m.getParticipantPlayers(playerWaitingFor) ;
				delegate.ccd_messageWaiting(MultiplayerClientCommunications.this, playerWaitingFor) ;
				break ;
			
			case GameMessage.TYPE_PAUSED_BY_PLAYERS:
				//Log.d(TAG, "Incoming message: paused" ) ;
				m.getParticipantPlayers(playerPaused) ;
				delegate.ccd_messagePaused(MultiplayerClientCommunications.this, playerPaused) ;
				break ;
			
			case GameMessage.TYPE_GO:
				//Log.d(TAG, "Incoming message: go" ) ;
				delegate.ccd_messageGo(MultiplayerClientCommunications.this) ;
				break ;
			
			case GameMessage.TYPE_GAME_OVER:
				//Log.d(TAG, "Incoming message: game over, winner is " + m.getPlayerSlot() ) ;
				// specifies the slot of the winning player.
				boolean [] winners = new boolean[actionAdapters.length] ;
				winners[m.getPlayerSlot()] = true ;
				delegate.ccd_messageGameOver(MultiplayerClientCommunications.this, winners) ;
				break ;
				
			case GameMessage.TYPE_MOVE_QUEUE:
				//Log.d(TAG, "Incoming message: move queue" ) ;
				int len = m.getMoveQueue(buffer) ;
				actionAdapters[m.getPlayerSlot()].communications_enqueueActions(buffer, 0, len) ;
				break ;
				
			case GameMessage.TYPE_NEXT_CYCLE:
				//Log.d(TAG, "Incoming message: next cycle" ) ;
				acsd = actionCycleStateMostRecent[m.getPlayerSlot()] ;
				if ( m.getNextCycleIsUpdateFor(actionCycleStateMostRecentId[m.getPlayerSlot()]) ) {
					m.getNextCycleUpdateAndApply(acsd) ;
					actionCycleStateMostRecentId[m.getPlayerSlot()] = m.getMessageId() ;
					actionAdapters[m.getPlayerSlot()].communications_setNextActionCycle(acsd) ;
				} else {
					//Log.d(TAG, "Incoming message: next cycle for player " + m.getPlayerSlot() + " ignored; already seen cycle ID" + m.getMessageId()) ;
				}
				break ;
				
			case GameMessage.TYPE_INCOMING_ATTACK:
				//Log.d(TAG, "Incoming message: incoming attack" ) ;
				m.getIncomingAttack(ad) ;
				actionAdapters[m.getPlayerSlot()].communications_addPendingAttacks(ad) ;
				break ;
				
			case GameMessage.TYPE_PLAYER_WON:
				// //Log.d(TAG, "Incoming message: player won") ;
				delegate.ccd_messageWon(MultiplayerClientCommunications.this, m.getPlayerSlot()) ;
				break ;
				
			case GameMessage.TYPE_PLAYER_LOST:
				// //Log.d(TAG, "Incoming message: player won") ;
				delegate.ccd_messageLost(MultiplayerClientCommunications.this, m.getPlayerSlot()) ;
				break ;
				
			case GameMessage.TYPE_PLAYER_IS_SPECTATOR:
				// //Log.d(TAG, "Incoming message: player won") ;
				delegate.ccd_messagePlayerIsSpectator(MultiplayerClientCommunications.this, m.getPlayerSlot()) ;
				break ;
				
			
				
			default:
				//Log.d(TAG, "Unknown message type: " + m.getType() ) ;
				// WHOOPS!
				return false ;
			}
			
			return true ;
		}
		
		
		RealtimeData sendRealtimeUpdateRD = null ;
		private void sendRealtimeUpdate() {
			sendRealtimeUpdateRD = actionAdapters[localActionAdapter].communications_getDisplacementData(sendRealtimeUpdateRD) ;
			if ( connection.isConnected() ) {
				outgoingMessage.setAsRealTimeUpdate(
						-1,
						sendRealtimeUpdateRD.getMillisecondsTicked(),
						sendRealtimeUpdateRD.getDisplacementSeconds(),
						sendRealtimeUpdateRD.getDisplacementRows()) ;
				connection.sendMessage(outgoingMessage) ;
				lastRealtimeUpdate = System.currentTimeMillis() ;
				// //Log.d(TAG, "sendRealtimeUpdate(): send with displacement " +  displacement) ;
			}
		}
		
		
		private void sendActionCycleStateRequest( int player ) {
			if ( connection.isConnected() ) {
				outgoingMessage.setAsNextCycleRequest(player) ;
				connection.sendMessage(outgoingMessage) ;
			}
		}
		
	}
	
	
	
	MultiplayerClientCommunicationsThread thread ;
	boolean pausedByLocalPlayer ;
	
	
	// Some temporary structures!
	AttackDescriptor ad ;
	byte [] buffer ;
	
	long [] waitingForActionCycleStateSince ;
	
	public MultiplayerClientCommunications() {
		thread = null ;
		pausedByLocalPlayer = false ;
		actionAdapters = new ActionAdapter[8] ;
		// TODO: Max of 8 players...?
		
		// initialize abstract class fields
		nonce = null ;
		personalNonce = null ;
		name = null ;

		localActionAdapter = -1 ;
		
		delegate = null ;
		
		connection = null ;
		
		waitingForActionCycleStateSince = new long[8] ;
	}
	
	
	@Override
	public ClientCommunications setConnection( MessagePassingConnection conn ) {
		super.setConnection(conn) ;
		conn.setDelegate(this) ;
		
		return this ;
	}

	
	/**
	 * Starts things going.  Very, very simple.
	 */
	@Override
	public synchronized void start() throws IllegalStateException {
		if ( thread != null && thread.isAlive() )
			throw new IllegalStateException("Can't start a ClientCommunications twice!") ;
		
		// Now's a good time to allocate these structures.
		buffer = new byte[actionAdapters[0].outgoingActionBufferSize()] ;
		
		// we use this explicitly to contain incoming data,
		// so it doesn't matter what our settings are.
		ad = new AttackDescriptor(1,1) ;

		
		// We require that nonce, personalNonce, and name have been set
		// (although setting them to zero or empty string is fine).
		if ( nonce == null || personalNonce == null || name == null )
			throw new IllegalStateException("Can't start MultiplayerClientCommunications without providing nonce, pnonce and name.") ;
		
		// We also require a delegate.
		if ( delegate == null )
			throw new IllegalStateException("Can't start MultiplayerClientCommunications without a delegate!") ;
		
		thread = new MultiplayerClientCommunicationsThread() ;
		thread.start() ;
		
		// queue up a 'connect' message.
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		// Queue up the stop.
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECT) ) ;
	}

	/**
	 * Stops the thread.  Again, very simple.
	 */
	@Override
	public synchronized void stop() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't stop a ClientCommunications that wasn't started!") ;
		
		if ( thread.running ) {
			while ( thread.handler == null ) {
				try {
					Thread.sleep(10) ;
				} catch( InterruptedException e ) { }
			}
			
			// Remove any pending messages other than STOP.
			for ( int i = 0; i < NUM_ANDROID_MESSAGE_TYPES; i++ )
				if ( i != ANDROID_MESSAGE_TYPE_STOP )
					thread.handler.removeMessages(i) ;
			
			// Queue up the stop.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP) ) ;
		}
	}
	
	@Override
	public synchronized void recycle() {
		super.recycle() ;
		thread = null ;
		// Some temporary structures!
		ad = null ;
		buffer = null ;
	}

	@Override
	public synchronized void join() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't join a ClientCommunications that wasn't started!") ;
		ThreadSafety.waitForThreadToTerminate(thread, 2000) ;
	}
	
	@Override
	public void sendNameIfChanged( String name ) throws IllegalStateException {
		// echo this
		if ( ( (name == null) != (this.name == null) )
				|| (name != null && !name.equals(this.name) ) ) {
			this.name = name ;
			try {
				if ( thread != null && thread.isAlive() && thread.welcomed ) {
					thread.handler.sendMessage(
							thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_NAME) ) ;
				}
			} catch ( IllegalStateException e ) { }	// don't care
		}
	}

	
	/**
	 * Fake pause!  Just pause the game immediately by telling the delegate to.
	 */
	@Override
	public synchronized void sendPauseRequest(boolean pauseOn) throws IllegalStateException {
		
		// Connections are thread-safe!  Huzzah!  BUT sending this might
		// interfere with interrogation, since interrogation assumes a
		// precise set of messages in a precise order.  We only want to
		// send this message if we have been welcomed to the server.
		
		// Update 8/30/2012: problem.  Since updating our Target API, we can
		// no longer perform network communication on the main thread.  Delegate
		// this task to the work thread.
		this.pausedByLocalPlayer = pauseOn ;
		try {
			if ( thread != null && thread.isAlive() && thread.welcomed ) {
				thread.handler.sendMessage(
						thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_PAUSE_REQUEST) ) ;
			}
		} catch ( IllegalStateException e ) { }	// don't care
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MESSAGE PASSING LAYER DELEGATE METHODS
	//
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The MPC received a message, which is immediately available.  hasMessage() and
	 * moreMessages() will return true, and getMessage() will return a Message object.
	 * 
	 * This method is called every time a message is received.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidReceiveMessage( MessagePassingConnection conn ) {
		//Log.d(TAG,"mpcd_messagePassingConnectionDidReceiveMessage") ;
		
		// Put a message for the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_MESSAGE_RECEIVED)) ;
		}
	}
	
	/**
	 * This method is called the first time after a call to connect() that the connection status
	 * has entered the state FAILED_TO_CONNECT, BROKEN, or DISCONNECTED_BY_PEER -AND- it will never
	 * produce another message.
	 * 
	 * This method will not be called if disconnect() has been called on the Connection (or if the
	 * Connection never loses its connection).
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDoneReceivingMessages( MessagePassingConnection conn ) {
		//Log.d(TAG,"mpcd_messagePassingConnectionDoneReceivingMessages") ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES)) ;
		}
	}
	
	/**
	 * This method is called upon the connection entering CONNECTION status.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidConnect( MessagePassingConnection conn ) {
		//Log.d(TAG,"mpcd_messagePassingConnectionDidConnect") ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED)) ;
		}
	}
	
	/**
	 * This method is called when a connection attempt fails.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidFailToConnect( MessagePassingConnection conn ) {
		//Log.d(TAG,"mpcd_messagePassingConnectionDidFailToConnect") ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECTION_FAILED)) ;
		}
	}
	
	/**
	 * This method is called upon the connection breaking.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidBreak( MessagePassingConnection conn ) {
		//Log.d(TAG,"mpcd_messagePassingConnectionDidBreak") ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECTION_BROKE)) ;
		}
	}
	
	/**
	 * This method is called upon the connection being disconnected by a peer.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidDisconnectByPeer( MessagePassingConnection conn ) {
		//Log.d(TAG,"mpcd_messagePassingConnectionDidDisconnectByPeer") ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECTION_PEER_DISCONNECTED)) ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// Game Action Listener methods
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * The provided Game object just Initialized (which is the first step
	 * in a new Game State's operation; a loaded game will not Initialize).
	 * 
	 * @param caller
	 * @param game
	 */
	public synchronized void gal_gameDidInitialize( ActionAdapter caller, Game game ) {
		// we don't care.
	}
	
	/**
	 * The provided 'game' object was given as a parameter
	 * in game_dequeueActions( game ).  This method is called
	 * as the last operation in game_dequeueActions.  We provide
	 * the return value which we will return immediately after this
	 * method call ends.
	 * 
	 * @param game
	 * @param returnValue
	 */
	public synchronized void gal_gameDidDequeueActions( ActionAdapter caller, Game game, boolean returnValue ) {
		// Nothing to do here.
	}
	
	boolean [] cycleRequested = new boolean[8] ;
	
	/**
	 * The provided 'game' object was given as a parameter to
	 * game_beginActionCycle(game).  This method is called 
	 * after retrieving any outgoing action cycles available,
	 * but before applying (or ATTEMPTING to apply) any
	 * incoming ones.
	 * 
	 * Regardless of the result of this method call,
	 * gal_gameDidBeginActionCycle will be called shortly thereafter.
	 * 
	 * The reason this method is included is primarily for the benefit
	 * of Progression mode, so we never have to 'tick' without change
	 * until we're ready to.
	 * 
	 * @param caller
	 * @param game
	 * @param returnValue
	 */
	public void gal_gameBeginningActionCycle( ActionAdapter caller, Game game, boolean outgoingActionCycleStatePending, boolean incomingActionCycleStatePending ) {
		// NOTE: upon calling this method, the ActionAdapter has copied the next CycleState
		// as held by the Game object.  If incomingActionCyclePending is not true,
		// pass it its own ACSD.
		
		// Huh?  What?  Why would we do that?  We ALWAYS want to wait for an update
		// from the server!  We can't guarantee that the update arrives in time,
		// so we should spin, NOT proceed.
		
		// Proceeding early produces a significant bug when attacks are present: the attacks are
		// NOT included in the next cycle state, for unknown reasons.  Recent animation changes
		// cut down on the time between "piece lock" and "next cycle start," increasing
		// the visibliity of this bug.
		
		
		/*
		if ( outgoingActionCycleStatePending && !incomingActionCycleStatePending ) {
			if ( caller.communications_getNextActionCycle(acsd, true) )
				caller.communications_setNextActionCycle(acsd) ;
		}
		*/
		
		while ( caller.communications_getNextOutgoingAttack(ad) ) {
			// drop it on the floor.
		}
		
		// TODO: if incomingActionCycleStatePending is false, we don't yet have
		// a cycle state to apply to begin the next cycle.  Now might be a good time
		// to start a timer (explicit or implicit) for a next cycle request.
		int player = -1 ;
		for ( int i = 0; i < actionAdapters.length; i++ ) {
			if ( caller == actionAdapters[i] )
				player = i ;
		}
		if ( player > -1 ) {
			if ( !incomingActionCycleStatePending ) {
				if ( waitingForActionCycleStateSince[player] < 0 ) {
					waitingForActionCycleStateSince[player] = System.currentTimeMillis() ;
				}
				// how long we been waiting?
				else if ( timeToRequestNextCycle( waitingForActionCycleStateSince[player], cycleRequested[player] ) ) {
					// request a cycle update.
					waitingForActionCycleStateSince[player] = System.currentTimeMillis() ;
					cycleRequested[player] = true ;
					thread.handler.sendMessage(
							thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_REQUEST_ACTION_CYCLE_STATE, player, -1)) ;
				}
			} else {
				// no longer waiting
				waitingForActionCycleStateSince[player] = -1 ;
				cycleRequested[player] = false ;
			}
		}
	}
	
	private boolean timeToRequestNextCycle( long waitingSince, boolean cycleRequested ) {
		if ( cycleRequested )
			return waitingSince + REQUEST_ACTION_CYCLE_STATE_EVERY < System.currentTimeMillis() ;
		else
			return waitingSince + REQUEST_ACTION_CYCLE_STATE_FIRST_AFTER < System.currentTimeMillis() ;
	}
	
	/**
	 * The provided 'game' object was given as a parameter to
	 * game_beginActionCycle(game).  This method is called
	 * as the last operation in game_dequeueActions.  We provide
	 * the return value which we will return immediately after this
	 * method call ends.
	 * 
	 * @param game
	 * @param returnValue
	 */
	public synchronized void gal_gameDidBeginActionCycle( ActionAdapter caller, Game game, boolean returnValue ) {
		// Nothing to do here.
	}
	
	
	/**
	 * The associated Game object was given as a parameter to
	 * game_didMove*, didTurn*, didFall, didLock, didUseReserve, didDrop,
	 * or didEndActionCycle.  In other words, we have added to the 
	 * OutgoingActionQueue.  This method is called as the last operation
	 * of the method called on 'caller,' in other words, the outgoing
	 * action queue has already been updated.
	 * @param game
	 */
	public synchronized void gal_gameDidEnqueueActions( ActionAdapter caller, boolean endCycle ) {
		// If caller == actionAdapters[localActionAdapter], send
		// the appropriate message to the Handler.  Otherwise, we just need
		// to clear out the buffer.  Now is a good opportunity to read all
		// outgoing attacks from the adapter (drop them on the floor), regardless
		// of which adapter we're reading.
		if ( caller == actionAdapters[localActionAdapter] ) {
			// we send immediately if this ends a cycle, or we have enough actions
			// built up.
			boolean sendImmediately = endCycle || caller.communications_getOutgoingActionQueueLength() >= ACTION_QUEUE_MAX_LENGTH ;
			if ( sendImmediately ) {
				thread.handler.sendMessage(
						thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_DID_ENQUEUE_LOCAL_ACTIONS)) ;
			} else {
				thread.handler.sendMessageDelayed(
						thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_DID_ENQUEUE_LOCAL_ACTIONS),
						ACTION_QUEUE_SEND_DELAY) ;
			}
		} else {
			caller.communications_readOutgoingActionQueue(null, 0, buffer.length) ;
		}
		
		// Attacks to dequeue?
		// We must retrieve these attacks so they don't build up.
		while ( caller.communications_getNextOutgoingAttack(ad) ) {
			// drop it on the floor.
		}
	}
	
	public synchronized void gal_gameDidCollide(ActionAdapter caller) {
		// Nothing to do here.
	}
	
	public synchronized void gal_gameDidLevelUp(ActionAdapter caller) {
		// Nothing to do here.
	}
	
	public synchronized void gal_gameAboutToLevelUp(ActionAdapter caller) {
		// Nothing to do here.
	}
	
	public synchronized void gal_gameHasOutOfSequenceAttack(ActionAdapter caller) {
		while ( caller.communications_getNextOutgoingAttack(ad) ) {
			// drop it on the floor.
		}
	}
	
	/**
	 * The game's displacement has passed the threshold for an integer;
	 * for example, going from 0.9 to 1.0, from 1.87 to 2.02, from
	 * 1.1 to 0.1, etc.
	 * @param caller
	 */
	public void gal_gameDisplacementDidChangeInteger(ActionAdapter caller, RealtimeData dd) {
		if ( caller == actionAdapters[localActionAdapter] ) {
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_REALTIME_UPDATE)) ;
		}
	}
}
