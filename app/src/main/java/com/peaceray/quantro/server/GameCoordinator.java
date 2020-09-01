package com.peaceray.quantro.server;

import java.util.Random;

import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.peaceray.quantro.adapter.action.ActionAdapter;
import com.peaceray.quantro.adapter.action.ActionAdapterWithGameIO;
import com.peaceray.quantro.communications.SlottedHandler;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingLayer;
import com.peaceray.quantro.model.communications.GameMessage;
import com.peaceray.quantro.model.descriptors.versioned.ActionCycleStateDescriptor;
import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameEvents;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.ThreadSafety;

/**
 * The GameCoordinator maintains a canonical representation
 * of a single current game.
 * 
 * We make no promises that this object can survive the Activity being
 * paused and restarted.  But if that happens, then outgoing Sockets
 * will necessarily have closed anyway, so let's not worry too much
 * about it.
 * 
 * GameCoordinators are instantiated with a game mode in mind.  They
 * create the necessary Game, GameInformation, etc. objects upon
 * instantiation.  They also have a dedicated thread which performs
 * game updates and handles player I/O.
 * 
 * Speaking of player I/O, this is handled through Socket objects.
 * Socket objects are passed in to the GameCoordinator along with the
 * nonce and personal nonce associated with the connection.  (it is
 * good practice to verify the nonce BEFORE passing the Socket in,
 * because we make no guarantees on what we do with a Socket with
 * a bad nonce.  We could send them TubGirl.png and then disconnect.)
 * 
 * The GameCoordinate maintains externally-available statistics
 * about how the game is going, how long it's been idle, etc.
 * Any serious manager for these objects should attempt to remove
 * GameCoordinators which no longer represent games in-progress.
 * This can be done simply by using a "maximum life" timer, but 
 * you may want more sophisticated stuff, like "idle time" or
 * "time spent waiting for players."
 * 
 * TODO: Improve efficiency and user experience of Synchronization.  Right
 * now, we Synchronize upon transition from Waiting or Paused to 'go'.
 * This is implemented by reseting our "latest synchronization applied"
 * settings when we transition away from GO or SYNCHRONIZATION status to
 * WAITING or PAUSED, and sending synchronization
 * messages when we transition to SYNCHRONIZATION status on the way to GO.
 * 
 * It might be worth doing the following:
 * 	1. Send Synchronization messages to connected players immediately upon transitioning
 * 		away from "GO" status.  Take care for boundary cases, such as the first Sync update,
 * 		or players who never actually receive the sync because they disconnect shortly thereafter.
 *  2. Track the time it takes a player to acknowledge the Sync. message.  If they take too long,
 *  	possibly they never got the message (i.e. they disconnected or something?)  It might also
 *  	be a useful bit of data to track.
 * 
 * @author Jake
 *
 */
public class GameCoordinator implements MessagePassingLayer.Delegate {
	
	public static final String TAG = "GameCoordinator" ;
	
	// Orders from above!  Connect or disconnect our connections.
	private static final int ANDROID_MESSAGE_TYPE_CONNECT 							= 0 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECT_ALL 						= 1 ;
	private static final int ANDROID_MESSAGE_TYPE_DISCONNECT_ALL 					= 2 ;
	private static final int ANDROID_MESSAGE_TYPE_STOP 								= 3 ;
	
	// The MessagePassing layer status updated.
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED 			= 4 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED				= 5 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE				= 6 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED	= 7 ;
	
	// The MessagePassing layer has a message for us!
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED 				= 8 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES 		= 9 ;
	
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE		= 10 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED = 11 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION = 12 ;
	
	// Check the time since the player has provided an update,
	// or that we have waited for a player long enough to prompt a "going to drop"
	// message.
	private static final int ANDROID_MESSAGE_TYPE_CHECK_FOR_HUNG_OR_WAITED_FOR_PLAYERS 	= 13 ;
	
	// Drop a player if they are still disconnected.
	private static final int ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER				= 14 ;
	
	// For the above, we use android.os.Message instances with what the Type, arg1
	// the connection index.
	
	
	// We handle broken and dropped connections on a delay, to provide time for 
	// DONE_RECEIVING_MESSAGES to trigger.  This is the delay before processing
	// broken connections.
	private static final long DELAYED_HANDLE_DELAY = 5000 ;
	
	// A "hung player" is one that is connected, but who hasn't broadcasted any
	// movement updates in a while.  We allow a fairly wide margin for this -- 12
	// seconds -- compared to the potentially normal in-game delay (movement queues
	// may be delayed by up to 0.8 seconds by clients, plus the potential animations
	// played between cycles).
	private static final long HUNG_PLAYER_RECHECK_EVERY = 4000 ;	// 4 seconds
	private static final long HUNG_PLAYER_MAX_DELAY = 12000 ;	// 12 seconds.
	
	// Players who did not receive the most recent Action Cycle State may request that
	// it be sent or resent.  Since we occassionally fail to advance games up to the point
	// where an Action Cycle State is generated, a request for an ACS can trigger a
	// game tick() if at least this much time has passed.
	private static final long REQUEST_CYCLE_PROMPTS_GAME_ADVANCE_AFTER = 2000 ;		// 2 seconds
	
	// For games with > 2 players, one player failing to connect can delay the entire
	// game indefinitely.  Players who don't connect in a timely fashion are considered
	// to have "quit."  We have a slightly longer leeway before the game gets going,
	// but if you disconnect in the middle of a game, god help you.
	private static final long DROP_DISCONNECTED_PLAYER_BEFORE_GAME_AFTER = 75000 ;		// 1:15 seconds
	private static final long DROP_DISCONNECTED_PLAYER_DURING_GAME_AFTER = 60000 ;		// 60 seconds
	
	private static final long DROP_DISCONNECTED_PLAYER_WARN_WHEN_TIME_REMAINING = 20000 ;	// 20 seconds 	(note that we check only ever 4)
	
	
	class GameCoordinatorThread extends Thread {
		
		public SlottedHandler handler ;
		
		boolean running ;
		
		long [] playerWaitingSince ;
		
		boolean [] playerWaitingFor ;
		boolean [] playerPaused ;
		
		boolean [] playerWelcomed ;		// we have sent our welcome package to this player.
		
		// When we send status updates, we keep a record of that sent, so we don't
		// send them redundantly.
		int lastStatusSent ;
		boolean [] lastStatusSentPlayers ;
		
		// Metadata on players
		long [][] lastSentFullSynchronizationId ;
		long [][] lastAppliedFullSynchronizationId ;
		
		long [] totalPlayerPauseTime ;
		long [] totalPlayerWaitingTime ;
		
		long [] lastPlayerGameUpdateTime ;
		
		// Metadata on the game and this object
		long creationTime ;
		long totalPauseTime ;
		long totalWaitingTime ;
		long totalPlayTime ;
		long totalEmptyOfPlayersTime ;
		
		// Metametadata
		long startPauseTime ;
		long startWaitingTime ;
		long startPlayTime ;
		long startEmptyOfPlayersTime ;
		long startGameOverTime ;
		
		long lastTimePlay ;
		
		// Temporary structures, not needed beyond a single method call.
		GameMessage tempMessage ;
		byte [] tempMoveQueue ;
		AttackDescriptor tempAttackDescriptor ;
		ActionCycleStateDescriptor tempActionCycleStateDescriptor ;
		
		Random r ;
		
		private boolean [] tempPlayerBoolean ;
		
		public GameCoordinatorThread() {
			running = true ;
			
			playerWaitingSince = new long[numPlayers] ;
			
			playerWaitingFor = new boolean[numPlayers] ;
			playerPaused = new boolean[numPlayers] ;
			playerWelcomed = new boolean[numPlayers] ;
			
			lastStatusSent = -1 ;
			lastStatusSentPlayers = new boolean[numPlayers] ;
			
			lastSentFullSynchronizationId = new long [numPlayers][numPlayers] ;
			lastAppliedFullSynchronizationId = new long [numPlayers][numPlayers] ;
			
			
			// Set initial values for possibly unknown things, like personal
			// nonces.
			for ( int i = 0; i < numPlayers; i++ ) {
				playerWaitingSince[i] = System.currentTimeMillis() ;
				playerWaitingFor[i] = true ;
				playerPaused[i] = false ;
				playerWelcomed[i] = false ;
				
				for ( int j = 0; j < numPlayers; j++ ) {
					lastAppliedFullSynchronizationId[i][j] = -1 ;
					lastSentFullSynchronizationId[i][j] = 0 ;
				}
			}
			
			
			// Metadata!
			totalPlayerPauseTime = new long[numPlayers] ;
			totalPlayerWaitingTime = new long[numPlayers] ;
			lastPlayerGameUpdateTime = new long[numPlayers] ;
			for ( int i = 0; i < numPlayers; i++ ) {
				totalPlayerPauseTime[i] = totalPlayerWaitingTime[i] = 0 ;
				lastPlayerGameUpdateTime[i] = 0 ;
			}
			totalPauseTime = totalWaitingTime = totalPlayTime = 0 ;
			creationTime = System.currentTimeMillis() ;
			startWaitingTime = creationTime ;
			startEmptyOfPlayersTime = creationTime ;

			lastTimePlay = 0 ;
			
			// TODO: This assumes all games have the same row/column dimensions.
			tempMessage = new GameMessage() ;
			tempMoveQueue = new byte[1024] ;
			tempAttackDescriptor = new AttackDescriptor( playerGame[0].R(), playerGame[0].C() ) ;
			tempActionCycleStateDescriptor = new ActionCycleStateDescriptor( playerGame[0].R(), playerGame[0].C() ) ;
			
			r = new Random() ;
			
			gameStatus = STATUS_WAITING ;
			tempPlayerBoolean = new boolean[numPlayers] ;
		}
		
		
		public void run() {
			
			// You have to prepare the looper before creating the handler.
	        Looper.prepare();
	        
	        // Create the handler so it is bound to this thread's message queue.
	        handler = new SlottedHandler(numPlayers) {
	        	
	        	private void removeAllMessagesRegardingPlayerConnection(int slot) {
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED, slot) ;
	        		
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES, slot) ;
	        		
            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE, slot) ;
            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED, slot) ;
            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION, slot) ;
            		
            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER, slot) ;
	        	}

	            public void handleMessage(Message msg) {

	            	/*
	            	 * IMPLEMENT MESSAGE HANDLER HERE.
	            	 */
	            	
	            	int type = msg.what ;
	            	int slot = getSlot(msg) ;
	            	
	            	MessagePassingConnection conn ;
	            	android.os.Message selfMessage ;
	            	GameMessage gm ;
	            	
	            	switch( type ) {
	            	////////////////////////////////////////////////////////////
	            	// CONNECTION COMMANDS
	            	//
	            	case ANDROID_MESSAGE_TYPE_CONNECT:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_CONNECT " + slot) ;
	            		// We should start connecting the specified connection.
	            		// Disconnect if not currently in a connect-ready state.
	            		conn = mpLayer.connection(slot) ;
	            		if ( !conn.isConnected() ) {
		            		removeAllMessagesRegardingPlayerConnection(slot) ;
		            		if ( conn.isAbleToDisconnect() )
		            			conn.disconnect() ;
		            		// Connect it.
		            		conn.connect() ;
		            		playerWaitingSince[slot] = System.currentTimeMillis() ;
	            		}
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_CONNECT_ALL:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_CONNECT_ALL") ;
	            		// We should connect EVERY connection.  Do this with a call to mpLayer.connect().
	            		mpLayer.connect() ;
	            		for ( int i = 0; i < numPlayers; i++ )
	            			playerWaitingSince[i] = System.currentTimeMillis() ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_DISCONNECT_ALL:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_DISCONNECT_ALL") ;
	            		// Disconnect EVERY connection.
	            		mpLayer.disconnect() ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_STOP:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_STOP") ;
	            		// Stop.  Stop this thread.  Stop everything.
	            		running = false ;
	            		gm = new GameMessage() ;
	            		gm.setAsServerClosing() ;
	            		mpLayer.broadcast(gm) ;
	            		mpLayer.disconnect() ;
	            		getLooper().quit() ;
	            		break ;
	            		
	            	////////////////////////////////////////////////////////////
		            // MESSAGE PASSING LAYER STATUS UPDATES
		            //
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED " + slot) ;
	            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER, slot) ;
	            		// Someone's connected!
	            		playerWaitingSince[slot] = System.currentTimeMillis() ; 		// just in case
	            		playerWaitingFor[slot] = false ;
	            		// Welcome them.
	            		sendWelcomeMessages( slot ) ;
	            		playerWelcomed[slot] = true ;
	            		updateAndBroadcastGameStatus( false ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED " + slot) ;
	            		// A connection attempt failed.  NOTE: this call
	            		// will be FOLLOWED by a call to DONE_RECEIVING_MESSAGES;
	            		// to avoid redundant operations, disconnect() here
	            		// to prevent that call.
	            		// TODO: Implement a better way to handle failures.
	            		// For example, maybe we should track the number of failed
	            		// attempts to tell the host?  Maybe let other players
	            		// vote to remove the player in question?
	            		mpLayer.connection(slot).disconnect() ;
	            		// Tell ourselves to reconnect.
	            		if ( running ) {
	            			sendSlottedMessage(ANDROID_MESSAGE_TYPE_CONNECT, slot) ;
	            		}
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE " + slot) ;
	            		// A connection broke.  This call will be FOLLOWED by
	            		// a call to DONE_RECEIVING_MESSAGES.  It is at that
	            		// moment that we should set up a reconnect - we
	            		// want to make sure we handle all the messages on this
	            		// connection before kicking them out.
	            		sendSlottedMessageDelayed(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE,
	            				slot, DELAYED_HANDLE_DELAY) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED " + slot) ;
	            		// A connection disconnected by a user.  This call will be FOLLOWED by
	            		// a call to DONE_RECEIVING_MESSAGES.  It is at that
	            		// moment that we should set up a reconnect - we
	            		// want to make sure we handle all the messages on this
	            		// connection before kicking them out.
	            		// NOTE: Strictly speaking, we don't need to do this.
	            		// For a PEER_DISCONNECT to occur, the peer must have
	            		// sent the DISCONNECT message.  Having retrieved and processed
	            		// that message we KNOW there will be no more messages
	            		// forthcoming.  For now, though, just hand responsibility
	            		// to the DONE_RECEIVING handler.
	            		sendSlottedMessageDelayed(
	            				ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED,
	            				slot, DELAYED_HANDLE_DELAY) ;
	            		break ;
	            	
	            	
	            	////////////////////////////////////////////////////////////
			        // MESSAGE PASSING LAYER MESSAGES RECEIVED AND DONE
			        //
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED") ;
	            		// Received a message from the user.  If the user is
	            		// currently disconnected (NOT in a "broken" or otherwise
	            		// unconnected state, but actually disconnected) then
	            		// some early message prompted us to disconnect them,
	            		// and we should ignore this message from them.  In fact,
	            		// we probably can't even retrieve that message anymore!
	            		conn = mpLayer.connection(slot) ;
	            		if ( conn.connectionStatus() == MessagePassingConnection.Status.DISCONNECTED )
	            			break ;	// do nothing
	            		if ( !conn.moreMessages() || !conn.hasMessage() ) {
	            			Log.v(TAG, "responding to ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED " + slot + " but connection has no messages") ;
	            			// Received a message, but no messages to get?  WTF?  Kill.
	            			// schedule a disconnect
	            			sendSlottedMessageDelayed(
	            					ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION,
		            				slot, 2000) ;
	            			break ;
	            		}
	            		// Otherwise, handle the message.
	            		gm = (GameMessage) conn.getMessage();
	            		handleIncomingMessage( gm, slot ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION:
	            		
	            		if ( type == ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES )
	            			Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES " + slot) ;
	            		else if ( type == ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE )
	            			Log.v(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_BROKE " + slot) ;
	            		else if ( type == ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_CONNECTION_PEER_DISCONNECTED )
	            			Log.v(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION " + slot) ;
	            		else if ( type == ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION )
	            			Log.v(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION " + slot) ;
	            		
	            		removeAllMessagesRegardingPlayerConnection(slot) ;
	            		
	            		// This connection, which was previously used for passing messages,
	            		// will never send another message again.  Obviously something went
	            		// wrong; for example, the player may have disconnected.
	            		// Update all our stuff.  This player is no longer in the game.
	            		conn = mpLayer.connection(slot) ;
	            		try {
	            			if ( conn.isAbleToDisconnect() ) {
	            				//Log.d(TAG, "disconnecting and scheduling re-connect") ;
	            				sendSlottedMessage(ANDROID_MESSAGE_TYPE_CONNECT, slot) ;
	            				conn.disconnect() ;
	            			}
	            		} catch( IllegalStateException e ) {
	            			// problem.  Probably because we're stopping for another reason?
	            			e.printStackTrace() ;
	            			getLooper().quit() ;
	            			running = false ;
	            			break ;
	            		}
	            		playerWaitingSince[slot] = System.currentTimeMillis() ;
	            		playerWaitingFor[slot] = true ;
	            		playerWelcomed[slot] = false ;
	            		// Tell the world.
	            		updateAndBroadcastGameStatus(false) ;
	            		
	            		break ;
	            		
	            	
	            	case ANDROID_MESSAGE_TYPE_CHECK_FOR_HUNG_OR_WAITED_FOR_PLAYERS:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_CHECK_FOR_HUNG_OR_WAITED_FOR_PLAYERS") ;
	            		if ( gameStatus == STATUS_GO ) {
	            			// look for players whose games are advancing but
	            			// have not given us an update in the allowed window.
	            			for ( int player = 0; player < numPlayers; player++ ) {
	            				if ( playerGame[player].stillPlaying() && !playerSpectating[player]
	            						&& lastPlayerGameUpdateTime[player] + HUNG_PLAYER_MAX_DELAY < System.currentTimeMillis() ) {
	            					// disconnect this player
	            					//Log.d(TAG, "Disconnecting player " + player + " who appears to be hung.") ;
	            					conn = mpLayer.connection(player) ;
	            					if ( conn.isAbleToDisconnect() ) {
	            						conn.disconnect() ;
	            						sendSlottedMessage( ANDROID_MESSAGE_TYPE_CONNECT, player ) ;
	    	            			}
	            					playerWaitingSince[player] = System.currentTimeMillis() ;
	            					playerWaitingFor[player] = true ;
	            					updateAndBroadcastGameStatus( false ) ;
	            				}
	            			}
	            		} else if ( gameStatus == STATUS_WAITING && numPlayers > 2 ) {
	            			// look for players that we have been waiting for.
	            			for ( int player = 0; player < numPlayers; player++ ) {
	            				if ( playerWaitingFor[player] ) {
	            					// we will re-post this message if appropriate
	            					removeSlottedMessages(ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER, player) ;
	            					long dropAfter = getTimeUntilDrop( player ) ;
	            					if ( dropAfter == 0 ) {
	            						// drop immediately
	            						sendSlottedMessage(ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER, player) ;
	            					} else if ( dropAfter > 0 && dropAfter < DROP_DISCONNECTED_PLAYER_WARN_WHEN_TIME_REMAINING ) {
	            						// schedule a drop and warn
	            						mpLayer.broadcast(tempMessage.setAsKickWarning(player, "", dropAfter)) ;
	            						sendSlottedMessageDelayed(ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER, player, dropAfter) ;
	            					}
	            				}
	            			}
	            		}
	            		// repost
	            		this.sendEmptyMessageDelayed(ANDROID_MESSAGE_TYPE_CHECK_FOR_HUNG_OR_WAITED_FOR_PLAYERS, HUNG_PLAYER_RECHECK_EVERY) ;
	            		break ;
	            		
	            		
	            	case ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER " + slot) ;
	            		// slot is also used as the object for the message,
	            		// so we can remove other instances.
	            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DROP_WAITED_FOR_PLAYER, slot) ;
	            		if ( playerWaitingFor[slot] ) {
	            			//Log.d(TAG, "dropping player " + slot) ;
	            			// we are still waiting for this player.  Quit them.
	            			removeAllMessagesRegardingPlayerConnection(slot) ;
	            			conn = mpLayer.connection(slot) ;
	        				if ( conn.isAbleToDisconnect() )
	        					mpLayer.connection(slot).disconnect() ;
	        				mpLayer.connection(slot).deactivate() ;
	        				
	        				// determine if this is a fatal exit.
	        				// TODO: Provide custom status for players that were
	        				// dropped rather than manually quit.
	        				playerPaused[slot] = false ;
	        				playerWaitingFor[slot] = false ;
	        				playerSpectating[slot] = true ;
	        				playerQuit[slot] = true ;
	        				
	        				// player kicked and became a spectator.
	        				tempMessage.setAsKick(slot, "") ;
	        				mpLayer.broadcast( tempMessage ) ;
	        				tempMessage.setAsPlayerIsSpectator( slot ) ;
	        				mpLayer.broadcast( tempMessage ) ;
	        				// TODO: Add support for closing down the server
	        				// and such.
	        				updateAndBroadcastGameStatus( false ) ;
	            		} else {
	            			// TODO broadcast "cancel scheduled drop"
	            		}
	            		break ;
	            	}
	                
	            }
	        };
	        
	        /*
	         * Start looping the message queue of this thread.
	         */
	        Looper.loop();
		}
		
		
		/**
		 * Handles the receipt of the message, which came from the specified sender.
		 * @param m
		 * @param sender
		 */
		private synchronized void handleIncomingMessage( GameMessage m, int sender ) {
			int slot ;
			MessagePassingConnection conn ;
			// process this message.
			switch( m.getType() ) {
			case GameMessage.TYPE_MY_NAME:
				// Player gave us a name!  Nice of them.  Set
				// our local field and broadcast.
				playerName[sender] = m.getName() ;
				tempMessage.setAsPlayerName(sender, playerName[sender]) ;
				mpLayer.broadcast(tempMessage) ;
				break ;
				
			case GameMessage.TYPE_MOVE_QUEUE:
				// Incoming moves from the player.  Extract them,
				// and give them to the actionAdapter.
				// NOTE: We ONLY apply move queue messages if we are currently
				// going.  This prevents move orders from coming in when the game
				// is paused or waiting.
				if ( gameStatus == STATUS_GO ) {
					int len = m.getMoveQueue(tempMoveQueue) ;
					if ( len > 0 ) {
						playerActionAdapter[sender].communications_enqueueActions(tempMoveQueue, 0, len) ;
						lastPlayerGameUpdateTime[sender] = System.currentTimeMillis() ;
					}
					// Advance the game for this player.
					advanceGame(sender) ;
					// This call advances the game, and broadcasts any relevant updates to players.
				}
				break ;
				
			case GameMessage.TYPE_PAUSE:
				// This player wants to pause.  Good on them!
				//System.out.println("GameCoordinator: received pause") ;
				playerPaused[sender] = true ;
				updateAndBroadcastGameStatus( false ) ;
				break ;
				
			case GameMessage.TYPE_UNPAUSE:
				// This player is unpausing.  Maybe we can actually play now.
				//System.out.println("GameCoordinator: received unpause") ;
				playerPaused[sender] = false ;
				updateAndBroadcastGameStatus( true ) ;	// Might be worth explictly sending "go", but for now set to "false"
				break ;
				
			case GameMessage.TYPE_NEXT_CYCLE_REQUEST:
				//System.out.println("GameCoordinator: next cycle request from " + sender + " for game " + m.getPlayerSlot()) ;
				slot = m.getPlayerSlot() ;
				// attempt to advance games if it's been long enough.
				if ( System.currentTimeMillis() - playerActionCycleStateTimeGeneratedOrSent[slot] > REQUEST_CYCLE_PROMPTS_GAME_ADVANCE_AFTER ) {
					this.advanceGame(slot) ;
					playerActionCycleStateTimeGeneratedOrSent[slot] = System.currentTimeMillis() ;
				}
				if ( playerActionAdapter[slot].communications_getMostRecentActionCycle(tempActionCycleStateDescriptor, false) ) {
					tempMessage.setAsNextCycle(slot, playerActionCycleStatesGenerated[slot], tempActionCycleStateDescriptor) ;
					mpLayer.connection(sender).sendMessage(tempMessage) ;
				}
				break ;
			
			case GameMessage.TYPE_FULL_SYNCHRONIZATION_APPLIED:
				//Log.v(TAG, "full sync applied by " + sender + " for game " + m.getPlayerSlot()) ;
				// This player is now synchronized... assuming they responded
				// with the right nonce.
				slot = m.getPlayerSlot();
				long syncId = m.getMessageId();
				lastAppliedFullSynchronizationId[sender][slot] = syncId ;
				updateAndBroadcastGameStatus( false ) ;	// Might be worth explicitly sending "go"
				lastPlayerGameUpdateTime[sender] = System.currentTimeMillis() ;
				break ;
			
			case GameMessage.TYPE_EXIT:
				//Log.v(TAG, "player is quitting") ;
				// This player has quit.  Tell all other players.
				conn = mpLayer.connection(sender) ;
				if ( conn.isAbleToDisconnect() )
					mpLayer.connection(sender).disconnect() ;
				mpLayer.connection(sender).deactivate() ;
				
				// determine if this is a fatal exit.
				boolean gameRunningBefore = !checkAndSendPlayerWonAndGameOver(false) ;
				playerPaused[sender] = false ;
				playerWaitingFor[sender] = false ;
				playerSpectating[sender] = true ;
				playerQuit[sender] = true ;
				boolean gameRunningAfter = !checkAndSendPlayerWonAndGameOver(false) ;
				
				// player quit and became a spectator.
				tempMessage.setAsPlayerQuit( sender, gameRunningBefore && !gameRunningAfter ) ;
				mpLayer.broadcast( tempMessage ) ;
				tempMessage.setAsPlayerIsSpectator( sender ) ;
				mpLayer.broadcast( tempMessage ) ;
				// TODO: Add support for closing down the server
				// and such.
				updateAndBroadcastGameStatus( false ) ;
				break ;
				
			case GameMessage.TYPE_REALTIME_UPDATE:
				// apply this realtime update to the appropriate game...
				// Log.d(TAG, "received realtime displacement " + m.getDisplacedRows()) ;
				playerGame[sender].setRealtimeMillisAndDisplacementSecondsAndDisplacedAndTransferredRows(
						m.getMillisecondsTicked(),
						m.getDisplacedSeconds(),
						m.getDisplacedRows()) ;
				break ;
				
			default:
				// We don't like clients who send other kinds of messages.
				// Get.  Out.
				mpLayer.connection(sender).disconnect() ;
				playerWaitingFor[sender] = true ;
				updateAndBroadcastGameStatus( false ) ;
				break ;
			}
		}
		
		
		private void advanceAllGames() {
			for ( int i = 0; i < playerGame.length; i++ ) {
				if ( playerGame[i] != null ) {
					advanceGame(i) ;
				}
			}
		}
		
		/**
		 * Calls 'tick' on the specified game and handles all the fallout
		 * of this event, including sending status updates to all other players
		 * if necessary.
		 * 
		 * @param slotNumber
		 */
		private void advanceGame( int slot ) {
			// Check for game-over.
			if ( !playerGame[slot].stillPlaying() ) {
				// This game is done.  If we didn't know that before,
				// we might want to send an update.
				if ( !playerGameOver[slot] ) {
					Log.d(TAG, "advanceGame: slot " + slot + " does not have playerGameOver set, but is over") ;
					playerGameOver[slot] = true ;
					playerSpectating[slot] = true ;
					// TODO: For > 2 players, we need cleverer win/loss examination.
					// TODO: For victory conditions other than "your opponent lost", we need better examination.
					if ( playerGame[slot].hasLost() ) {
						Log.d(TAG, "advanceGame: slot " + slot + " has lost") ;
						// This player lost.
						tempMessage.setAsPlayerLost( slot ) ;
						mpLayer.broadcast(tempMessage) ;
						
						// make this player a spectator.
						tempMessage.setAsPlayerIsSpectator( slot ) ;
						mpLayer.broadcast(tempMessage) ;
						
						if ( checkAndSendPlayerWonAndGameOver(true) ) {
							updateAndBroadcastGameStatus(false) ;
						}
					}
				}
				return ;
			}
			
			try {
				//System.out.println("game " + slot + " ticking, state is " + playerGame[slot].s.state) ;
				playerGame[slot].tick(0) ;
				//System.out.println("game " + slot + " just ticked, state is " + playerGame[slot].s.state) ;
			}
			catch( Exception e ) {
				e.printStackTrace() ;
			}
			
			// We have advanced.  Send out outgoing moves, if any.
			int len ;
			while ( 0 < (len = playerActionAdapter[slot].communications_readOutgoingActionQueue(tempMoveQueue, 0, 1024))  ) {
				// Broadcast this to all players - EXCEPT player i.
				tempMessage.setAsMoveQueue(slot, tempMoveQueue, 0, len) ;
				mpLayer.broadcastExcept( tempMessage, slot ) ;
			}
			
			// Send out outgoing attacks, if any.
			while ( playerActionAdapter[slot].communications_getNextOutgoingAttack(tempAttackDescriptor) ) {
				int t ;
				// Determine which players receive this attack.  Set tempPlayerBoolean
				// to whether the indicated player (slot) is a target of this attack.
				ArrayOps.setEmpty(tempPlayerBoolean) ;
				switch( tempAttackDescriptor.target_code ) {
				case AttackDescriptor.TARGET_INCOMING:
					// self-target.
					tempPlayerBoolean[slot] = true ;
					break ;
				case AttackDescriptor.TARGET_CYCLE_NEXT:
					// next in line (rotate UP in player slot number)
					// only affects players who are still in the game.
					t = (slot + 1) % numPlayers ;
					while ( t != slot && ( playerGameOver[t] || playerSpectating[t] ) ) {
						t = (t + 1) % numPlayers ;
					}
					if ( t != slot ) {
						tempPlayerBoolean[t] = true ;
					}
					break ;	
				case AttackDescriptor.TARGET_CYCLE_PREVIOUS:
					// prev in line (rotate DOWN in player slot number)
					// only affects players who are still in the game.
					t = slot == 0 ? numPlayers-1 : slot-1 ;
					while ( t != slot && ( playerGameOver[t] || playerSpectating[t] ) ) {
						t = t == 0 ? numPlayers-1 : t-1 ;
					}
					if ( t != slot ) {
						tempPlayerBoolean[t] = true ;
					}
					break ;	
				case AttackDescriptor.TARGET_ALL:
					// targets all players, including the "creator."
					for ( int i = 0; i < numPlayers; i++ ) {
						tempPlayerBoolean[i] = !( playerGameOver[i] || playerSpectating[i] ) ;
					}
					break ;
				case AttackDescriptor.TARGET_ALL_OTHERS:
					// targets all players, except the "creator."
					for ( int i = 0; i < numPlayers; i++ ) {
						tempPlayerBoolean[i] = !( playerGameOver[i] || playerSpectating[i] ) && i != slot ;
					}
					break ;
				case AttackDescriptor.TARGET_ALL_DIVIDED:
					// targets all players, dividing the result among the total number.
					t = 0 ;
					for ( int i = 0; i < numPlayers; i++ ) {
						if ( !( playerGameOver[i] || playerSpectating[i] ) ) {
							t++ ;
							tempPlayerBoolean[i] = true ;
						}
					}
					// divide attack among 't' players
					if ( t > 0 ) {
						tempAttackDescriptor.divideAmong(t) ;
					}
					break ;
				case AttackDescriptor.TARGET_ALL_OTHERS_DIVIDED:
					// targets all players except the creator, dividing the result among the total number.
					t = 0 ;
					for ( int i = 0; i < numPlayers; i++ ) {
						if ( !( playerGameOver[t] || playerSpectating[t] ) && i != slot ) {
							t++ ;
							tempPlayerBoolean[i] = true ;
						}
					}
					// divide attack among 't' players
					if ( t > 0 ) {
						tempAttackDescriptor.divideAmong(t) ;
					}
					break ;
				case AttackDescriptor.TARGET_UNSET:
				default:
					Log.d(TAG, "WARNING: attack descriptor target is UNSET.  Defaulting to TARGET_ALL_OTHERS.") ;
					for ( int i = 0; i < numPlayers; i++ ) {
						tempPlayerBoolean[i] = !( playerGameOver[i] || playerSpectating[i] ) && i != slot ;
					}
					break ;
				}
				
				// make this an 'incoming attack.'
				tempAttackDescriptor.target_code = AttackDescriptor.TARGET_INCOMING ;
				
				// For each attack target, send a message to ALL
				// users that that player should be hit with this attack
				// (note that Player A needs to know that an attack will
				// soon hit Player B, to properly render things).
				for ( int target = 0; target < numPlayers; target++ ) {
					if ( tempPlayerBoolean[target] ) {
						tempMessage.setAsIncomingAttack(target, tempAttackDescriptor) ;
						// Send to all players: everyone needs to know.
						mpLayer.broadcast( tempMessage ) ;
					}
				}
				
				// Perform local update: add this incoming attack to
				// all targeted players.
				for ( int target = 0; target < numPlayers; target++ ) {
					if ( tempPlayerBoolean[target] ) {
						playerActionAdapter[target].communications_addPendingAttacks(tempAttackDescriptor) ;
					}
				}
			}
			
			// Apply "NextCycle" updates, if any.
			if ( playerActionAdapter[slot].communications_getNextActionCycle(tempActionCycleStateDescriptor, true) ) {
				// We apply this to ourself, but we also broadcast the update to everyone.
				// The message we send is as an update if our ACSD was previously set,
				// explicit Descriptor if not.
				playerActionCycleStatesGenerated[slot]++ ;
				playerActionCycleStateTimeGeneratedOrSent[slot] = System.currentTimeMillis() ;
				if ( playerActionCycleStateSet[slot] ) {
					tempMessage.setAsNextCycle(slot, playerActionCycleStatesGenerated[slot],
							playerActionCycleState[slot],
							tempActionCycleStateDescriptor) ;
				} else {
					tempMessage.setAsNextCycle(slot, playerActionCycleStatesGenerated[slot], tempActionCycleStateDescriptor) ;
				}
				mpLayer.broadcast( tempMessage ) ;
				playerActionAdapter[slot].communications_setNextActionCycle(tempActionCycleStateDescriptor) ;
				
				// swap ACSD with temp so we have a record of this.
				ActionCycleStateDescriptor tempACSD = playerActionCycleState[slot] ;
				playerActionCycleState[slot] = tempActionCycleStateDescriptor ;
				tempActionCycleStateDescriptor = tempACSD ;
				playerActionCycleStateSet[slot] = true ;
			}
			
			// Check whether we're about to lose.
			if ( !playerGame[slot].stillPlaying() ) {
				Log.d(TAG, "end of advanceGame: slot " + slot + " is not still playing.") ;
				// This game is done.  If we didn't know that before,
				// we might want to send an update.
				if ( !playerGameOver[slot] ) {
					Log.d(TAG, "advanceGame: slot " + slot + " does not have game over set") ;
					playerGameOver[slot] = true ;
					playerSpectating[slot] = true ;
					
					// This player lost.
					tempMessage.setAsPlayerLost( slot ) ;
					mpLayer.broadcast(tempMessage) ;
					
					// make this player a spectator.
					tempMessage.setAsPlayerIsSpectator( slot ) ;
					mpLayer.broadcast(tempMessage) ;
					
					if ( checkAndSendPlayerWonAndGameOver(true) ) {
						updateAndBroadcastGameStatus(false) ;
					}
				}
			}
		}
		
		private boolean checkAndSendPlayerWonAndGameOver( boolean sendMessages ) {
			return checkAndSendPlayerWonAndGameOver( sendMessages, -1 ) ;
		}
		
		
		
		
		/**
		 * Checks whether the game is over.  If it is, identifies any winner(s)
		 * (broadcasting the appropriate messages) and sends a game over
		 * message.
		 * 
		 * By convention, the game is over if there is only one player and
		 * their game is over, or > 1 player but at most one is non-spectating and
		 * non-lost.
		 * 
		 * Returns whether the game is over.
		 * 
		 * @return
		 */
		private boolean checkAndSendPlayerWonAndGameOver( boolean sendMessages, int messageTarget ) {
			// Check if the game is over.
			// By convention, the game is over if:
			// There is 1 player, and he has Won or Lost
			// There is > 1 player, but only one non-spectator who has not lost.
			int aPlayerWhoDidNotLose = -1 ;
			int numActive = 0 ; 
			for ( int i = 0; i < numPlayers; i++ ) {
				if ( !playerGame[i].hasLost() && !playerSpectating[i] ) {
					numActive += 1 ;
					aPlayerWhoDidNotLose = i ;
				}
			}
			if ( ( numPlayers > 1 && numActive <= 1 )
					|| ( numPlayers == 1 && !playerGame[0].stillPlaying() ) ) {
				
				if ( sendMessages ) {
					if ( aPlayerWhoDidNotLose > -1 ) {
						tempMessage.setAsPlayerWon(aPlayerWhoDidNotLose) ;
						if ( messageTarget < 0 )
							mpLayer.broadcast(tempMessage) ;
						else
							mpLayer.connection(messageTarget).sendMessage(tempMessage) ;
					}
					tempMessage.setAsGameOver(aPlayerWhoDidNotLose) ;
					if ( messageTarget < 0 )
						mpLayer.broadcast(tempMessage) ;
					else
						mpLayer.connection(messageTarget).sendMessage(tempMessage) ;
				}
				
				return true ;
			}
			
			return false ;
		}
		
		
		private int numRemainingPlayers() {
			int num = 0 ;
			for ( int player = 0; player < numPlayers; player++ ) {
				if ( !playerQuit[player] && !playerSpectating[player] && !playerGameOver[player] ) {
					if ( playerGame[player].stillPlaying() ) {
						num++ ;
					}
				}
			}
			
			return num ;
		}
		
		
		/**
		 * Returns the time until the specified player is dropped due to an overly
		 * long wait.
		 * 
		 * If 0, we should drop them immediately.
		 * If > 0, represents the number of milliseconds until the drop will occur.
		 * If < 0, the player is not in a state where a drop should be scheduled.
		 * @param slot The player in question
		 * @return
		 */
		private long getTimeUntilDrop( int slot ) {
			if ( gameStatus != STATUS_WAITING || numPlayers <= 2 )
				return -1 ;
			
			long dropAfter = totalPlayTime > 0 ? DROP_DISCONNECTED_PLAYER_DURING_GAME_AFTER
					: DROP_DISCONNECTED_PLAYER_BEFORE_GAME_AFTER ;
			long dropIfWaitingSince = System.currentTimeMillis() - dropAfter ;
			if ( playerWaitingFor[slot] && !playerSpectating[slot] ) {
				return Math.max(0, playerWaitingSince[slot] - dropIfWaitingSince) ;
			}
			return -1 ;
		}
		
		
		/**
		 * The player in slot playerSlot just connected.  Send them a welcome!
		 * 
		 * Our welcome message(s) consist of the following: 
		 * 	total slots
		 * 	"your" slot
		 * 	all current player names
		 * 	game won/lost/over?
		 * 	welcome!
		 * 
		 * We also BROADCAST the new player's name to all but the 
		 * 		new player.
		 * 
		 * @param playerSlot
		 */
		private synchronized void sendWelcomeMessages( int playerSlot ) {
			// New players are assumed to be non-synchronized.
			for ( int i = 0; i < numPlayers; i++ )
				lastAppliedFullSynchronizationId[playerSlot][i] = -1 ;
			
			// Messages to the new player
			MessagePassingConnection playerMPC = mpLayer.connection(playerSlot) ;
			playerMPC.sendMessage( tempMessage.setAsTotalPlayerSlots(numPlayers) ) ;
			playerMPC.sendMessage( tempMessage.setAsPersonalPlayerSlot(playerSlot) ) ;
			for ( int i = 0; i < numPlayers; i++ ) {
				String name = mpLayer.connection(i).getRemoteName() ;
				if ( i != playerSlot && name != null ) {
					playerMPC.sendMessage( tempMessage.setAsPlayerName(i, name) ) ;
				}
			}
			checkAndSendPlayerWonAndGameOver(true, playerSlot) ;
			for ( int i = 0; i < numPlayers; i++ ) {
				long timeUntilDrop = getTimeUntilDrop( i ) ;
				if ( timeUntilDrop > 0 && DROP_DISCONNECTED_PLAYER_WARN_WHEN_TIME_REMAINING > timeUntilDrop ) {
					playerMPC.sendMessage( tempMessage.setAsKickWarning(i, "", timeUntilDrop) ) ;
				} else {
					playerMPC.sendMessage( tempMessage.setAsKickWarningRetraction(i) ) ;
				}
			}
			playerMPC.sendMessage( tempMessage.setAsWelcomeToServer() ) ;
			
			// Broadcast player name.
			String name = playerMPC.getRemoteName() ;
			if ( name != null ) {
				mpLayer.broadcast( tempMessage.setAsPlayerName(playerSlot, name) ) ;
			}
			
			// Broadcast a cancel for any scheduled drops for this player (playerSlot).
			mpLayer.broadcast( tempMessage.setAsKickWarningRetraction(playerSlot) ) ;
		}
		
		
		private boolean [] playerIncluded = null; 
		
		/**
		 * Checks playerWaitingFor, playerPaused, and
		 * the game objects (for won/lost status).  Sets the
		 * 'gameStatus' field, and broadcasts the current status
		 * to all connected players.
		 * 
		 * Calls 'broadcast', which checks that outgoing connections
		 * are still OK.  If broadcast fails this method will recur.
		 * 
		 * @param forceBroadcast If true, we will broadcast our state no
		 * matter what.  If false, we only broadcast our state if it is different
		 * from the most recently broadcast state.
		 */
		private synchronized void updateAndBroadcastGameStatus( boolean forceBroadcast ) {
			if ( playerIncluded == null )
				playerIncluded = new boolean[playerWaitingFor.length] ;
			
			// Look for if we are waiting for anyone.
			boolean isWaiting = false ;
			for ( int i = 0; i < numPlayers; i++ ) {
				playerIncluded[i] = ( playerWaitingFor[i] && !playerSpectating[i] && !playerQuit[i] ) ;
				isWaiting = isWaiting || playerIncluded[i] ;
				isWaiting = isWaiting || ( playerWaitingFor[i] && !playerSpectating[i] ) ;
			}
			
			if ( isWaiting ) {
				Log.v(TAG, "updateAndBroadcastGameStatus: waiting") ;
				if ( !sameAsLastStatusSent( STATUS_WAITING, playerIncluded ) || forceBroadcast ) {
					setAsLastStatusSent( STATUS_WAITING, playerIncluded ) ;
					tempMessage.setAsWaitingForPlayers(playerIncluded) ;
					mpLayer.broadcast(tempMessage) ;
				}
				if ( gameStatus != STATUS_WAITING )
					changeStatus(STATUS_WAITING) ;
				
				return ;
			}
			
			// Synchronizing?
			boolean isSync = false ;
			for ( int i = 0; i < numPlayers; i++ ) {
				playerIncluded[i] = false ;
				if ( !playerWaitingFor[i] && !playerQuit[i] ) {
					boolean syncHere = false ;
					for ( int j = 0; j < numPlayers; j++ ) {
						syncHere = syncHere || lastSentFullSynchronizationId[i][j] != lastAppliedFullSynchronizationId[i][j] ;
					}
					playerIncluded[i] = syncHere ;
					isSync = isSync || syncHere ;
				}
			}
			if ( isSync ) {
				Log.v(TAG, "updateAndBroadcastGameStatus: synchronizing") ;
				if ( gameStatus == STATUS_GO ) {
					setAsLastStatusSent( STATUS_WAITING, playerIncluded ) ;
					tempMessage.setAsWaitingForPlayers(playerIncluded) ;
					mpLayer.broadcast(tempMessage) ;
					changeStatus( STATUS_WAITING ) ;
				}
				if ( gameStatus != STATUS_SYNCHRONIZING )
					changeStatus( STATUS_SYNCHRONIZING ) ;
				return ;
			}
			
			// Look for if anyone paused the game.
			boolean isPaused = false ;
			for ( int i = 0; i < numPlayers; i++ ) {
				playerIncluded[i] = ( playerPaused[i] && !playerSpectating[i] && !playerQuit[i] ) ;
				isPaused = isPaused || playerIncluded[i] ;
			}
			if ( isPaused ) {
				if ( gameStatus != STATUS_PAUSED )
					changeStatus( STATUS_PAUSED ) ;
				if ( !sameAsLastStatusSent( gameStatus, playerIncluded ) || forceBroadcast ) {
					setAsLastStatusSent( gameStatus, playerIncluded ) ;
					tempMessage.setAsPausedByPlayers(playerIncluded) ;
					mpLayer.broadcast(tempMessage) ;
				}
 				return ;
			}
			
			// Check if the game is over.
			// By convention, the game is over if:
			// There is 1 player, and he has Won or Lost
			// There is > 1 player, but only one non-spectator who has not lost.
			
			// check for loss
			if ( gameStatus != STATUS_GAME_OVER ) {
				// This player lost.
				if ( checkAndSendPlayerWonAndGameOver(true) ) {
					if ( gameStatus != STATUS_GAME_OVER )
						changeStatus( STATUS_GAME_OVER ) ;
					if ( !sameAsLastStatusSent( gameStatus, null ) || forceBroadcast ) {
						setAsLastStatusSent( gameStatus, null ) ;
					}
					return ;
				}
			}
			
			// Otherwise, the game is going.  We only broadcast
			// if we weren't going before, or if the method parameter
			// says to.
			boolean becameGo = false ;
			//Log.v(TAG, "should go") ;
			if ( gameStatus != STATUS_GO ) {
				becameGo = true ;
				changeStatus( STATUS_GO ) ;
			}
			if ( !sameAsLastStatusSent( gameStatus, null ) || forceBroadcast ) {
				for ( int i = 0; i < numPlayers; i++ ) {
					lastPlayerGameUpdateTime[i] = System.currentTimeMillis() ;
				}
				//Log.v(TAG, "sending go") ;
				setAsLastStatusSent( gameStatus, null ) ;
				tempMessage.setAsGo() ;
				mpLayer.broadcast(tempMessage) ;
				if ( becameGo )
					advanceAllGames() ;
				
				//Log.v(TAG, "advanced games") ;
			}
		}
		
		private boolean sameAsLastStatusSent( int status, boolean [] players ) {
			if ( status != lastStatusSent )
				return false ;
			if ( players != null ) {
				for ( int i = 0; i < numPlayers; i++ ) {
					if ( players[i] != lastStatusSentPlayers[i] )
						return false ;
				}
			}
			return true ;
		}
		
		private void setAsLastStatusSent( int status, boolean [] players ) {
			lastStatusSent = status ;
			if ( players != null ) {
				for ( int i = 0; i < numPlayers; i++ ) {
					lastStatusSentPlayers[i] = players[i] ;
				}
			}
		}
		
		private void changeStatus( int newStatus ) {
			// Transition away from waiting:
			if ( gameStatus == GameCoordinator.STATUS_WAITING ) {
				totalWaitingTime += System.currentTimeMillis() - startWaitingTime ;
			}
			else if ( gameStatus == GameCoordinator.STATUS_PAUSED ) {
				totalPauseTime += System.currentTimeMillis() - startPauseTime ;
			}
			else if ( gameStatus == GameCoordinator.STATUS_GO ) {
				totalPlayTime += System.currentTimeMillis() - startPlayTime ;
				if ( newStatus != STATUS_GO )
					lastTimePlay = System.currentTimeMillis() ;
				
				// If we WERE going, but not anymore, than guess what?
				// everyone is now out of sync!
				if ( newStatus != GameCoordinator.STATUS_GO ) {
					for ( int i = 0; i < numPlayers; i++ ) {
						for ( int j = 0; j < numPlayers; j++ ) {
							lastAppliedFullSynchronizationId[i][j] = -1 ;
							lastSentFullSynchronizationId[i][j] = 0 ;
						}
					}
				}
			}
			
			if ( newStatus == GameCoordinator.STATUS_WAITING ) {
				startWaitingTime = System.currentTimeMillis() ;
			}
			else if ( newStatus == GameCoordinator.STATUS_PAUSED ) {
				startPauseTime = System.currentTimeMillis() ;
			}
			else if ( newStatus == GameCoordinator.STATUS_SYNCHRONIZING ) {
				// Send sync messages!
				for ( int regardingP = 0; regardingP < numPlayers; regardingP++ ) {
					long syncID = r.nextLong() ;
					// Self-apply an action cycle state, if one is available.  This prevents
					// people from Syncing to a state which is frozen, waiting for another 
					// content update.  It should also reduce the total amount of data sent.
					if ( playerActionAdapter[regardingP].communications_getNextActionCycle(tempActionCycleStateDescriptor, true) ) {
						playerActionAdapter[regardingP].communications_setNextActionCycle(tempActionCycleStateDescriptor) ;
					}
					// a full sync resets our cycle-state chain.
					playerActionCycleStateSet[regardingP] = false ;
					tempMessage.setAsFullSynchronization(regardingP, syncID, playerGame[regardingP]) ;
					System.out.println("GameCoordinator: broadcasting sync message regarding " + regardingP) ;
					mpLayer.broadcast(tempMessage) ;
					System.out.println("GameCoordinator: did broadcast regarding " + regardingP) ;
					for ( int toP = 0; toP < numPlayers; toP++ ) {
						lastSentFullSynchronizationId[toP][regardingP] = syncID ;
					}
				}
			}
			else if ( newStatus == GameCoordinator.STATUS_GO ) {
				startPlayTime = System.currentTimeMillis() ;
			}
			else if ( newStatus == GameCoordinator.STATUS_GAME_OVER ) {
				startGameOverTime = System.currentTimeMillis() ;
			}
			
			gameStatus = newStatus ;
		}
		
	}
	

	
	
	
	// Game status
	int gameStatus ;
	private static final int STATUS_WAITING = 0 ;
	private static final int STATUS_PAUSED = 1 ;
	private static final int STATUS_SYNCHRONIZING = 2 ;		// all players are connected, waiting for sync
	private static final int STATUS_GO = 3 ;
	private static final int STATUS_GAME_OVER = 4 ;

	
	GameCoordinatorThread thread ;
	MessagePassingLayer mpLayer ;
	
	// Store information for each player.
	boolean [] playerSpectating ;
	boolean [] playerQuit ;
	boolean [] playerGameOver ;
	Game [] playerGame ;
	GameInformation [] playerGameInformation ;
	GameEvents [] playerGameEvents ;
	String [] playerName ;
	ActionAdapter [] playerActionAdapter ;
	ActionCycleStateDescriptor [] playerActionCycleState ;
	boolean [] playerActionCycleStateSet ;
	long [] playerActionCycleStatesGenerated ;
	long [] playerActionCycleStateTimeGeneratedOrSent ;
	GameResult.Builder grBuilder ;
	
	int numPlayers ;
	int pseudorandom ;
	

	/**
	 * Constructs a new GameCoordinator object using the specified gameMode.
	 * This constructor will use entirely new Game objects.  If you want the
	 * GameCoordinator to copy the State from provided Game objects, pass
	 * them in.
	 * 
	 * @param gameMode
	 */
	public GameCoordinator( int gameMode, int numberOfPlayers, int pseudorandom ) {
		numPlayers = numberOfPlayers ;
		this.pseudorandom = pseudorandom ;
		
		gameStatus = STATUS_WAITING ;
		
		// Allocate
		playerSpectating = new boolean[numPlayers] ;
		playerQuit = new boolean[numPlayers] ;
		playerGameOver = new boolean[numPlayers] ;
		playerGame = new Game[numPlayers] ;
		playerGameInformation = new GameInformation[numPlayers];
		playerGameEvents = new GameEvents[numPlayers] ;
		playerName = new String[numPlayers] ;
		playerActionAdapter = new ActionAdapter[numPlayers] ;
		playerActionCycleState = new ActionCycleStateDescriptor[numPlayers] ;
		playerActionCycleStateSet = new boolean[numPlayers] ;
		playerActionCycleStatesGenerated = new long[numPlayers] ;
		playerActionCycleStateTimeGeneratedOrSent = new long[numPlayers] ;
		
		// Instantiate the things we can: game objects and the like.
		// These are the canonical representations.
		for ( int i = 0; i < numPlayers; i++ ) {
			playerGameInformation[i] = new GameInformation( gameMode, 1 ).finalizeConfiguration() ;
        	playerGameEvents[i] = new GameEvents().finalizeConfiguration() ;
        	
        	playerGame[i] = new Game( GameModes.numberRows(playerGameInformation[i]), GameModes.numberColumns(playerGameInformation[i]) ) ;
        	playerGame[i].setGameInformation(playerGameInformation[i]) ;
        	playerGame[i].setGameEvents( playerGameEvents[i] ) ;
        	playerGame[i].setSystemsFromSerializables(null) ;
        	playerGame[i].setPseudorandom(pseudorandom) ;
        	
        	playerGame[i].makeReady() ;
        	playerGame[i].finalizeConfiguration() ;
        	
        	playerActionAdapter[i] = new ActionAdapterWithGameIO( playerGame[i].R(), playerGame[i].C() ) ;
        	playerActionAdapter[i].set_gameShouldUseTimingSystem(false) ;	// We don't use a timing system.
        	playerActionAdapter[i].set_dequeueActionsDiscards(false) ;		// We don't want to lose actions user request.
        	playerGame[i].setActionAdapter( playerActionAdapter[i] ) ;
        	
        	playerSpectating[i] = false ;
        	playerQuit[i] = false ;
        	
        	
        	playerActionCycleState[i] = new ActionCycleStateDescriptor( playerGame[i].R(), playerGame[i].C() ) ;
        	playerActionCycleStateSet[i] = false ;
        	playerActionCycleStatesGenerated[i] = 0 ;
        	playerActionCycleStateTimeGeneratedOrSent[i] = 0 ;
		}
		
		gameStatus = GameCoordinator.STATUS_WAITING ;
		
		thread = null ;
		mpLayer = null ;
	}
	
	
	/**
	 * Provides the MessagePassingLayer which this GameCoordinator will
	 * use to coordinate between players.
	 * 
	 * NOTE: Responsibility of checking Nonces and PersonalNonces, and retrieving
	 * 			Names upon connection, is left for the provided MessagePassingLayer.
	 * 			We do NOT bother verifying that Nonces and PNonces match - either it
	 * 			doesn't matter at all, or the Layer has been configured to do that
	 * 			for us.
	 * 
	 * 			However, we DO update player names upon each reconnect, just in case
	 * 			the player has changed his/her name.
	 * 
	 * @param layer
	 * @throws IllegalStateException
	 */
	public synchronized GameCoordinator setMessagePassingLayer( MessagePassingLayer layer ) throws IllegalStateException {
		if ( thread != null && thread.isAlive() )
			throw new IllegalStateException("Can't set MPLayer after starting the GameCoordinator!") ;
		
		mpLayer = layer ;
		mpLayer.setDelegate(this) ;
		
		return this ;
	}
	
	
	public synchronized void start() throws IllegalStateException {
		if ( thread != null && thread.isAlive() )
			throw new IllegalStateException("Can't start if currently running!") ;
		if ( mpLayer == null )
			throw new IllegalStateException("Can't start without a MessagePassingLayer!" ) ;
		
		thread = new GameCoordinatorThread() ;
		thread.start() ;
		// Put a "connect all" message on the handler.
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) {
				e.printStackTrace() ;
				return ;
			}
		}
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECT_ALL)) ;

        // start our reposting "hung player check."
        thread.handler.sendMessageDelayed(
        		thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CHECK_FOR_HUNG_OR_WAITED_FOR_PLAYERS),
        		HUNG_PLAYER_RECHECK_EVERY) ;

	}
	
	
	public synchronized void stop() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't stop if never started!") ;
		
		if ( thread.running ) {
			while ( thread.handler == null ) {
				try {
					Thread.sleep(10) ;
				} catch( InterruptedException e ) { }
			}
			
			// Remove all other messages from the Handler.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP) ) ;
		}
	}
	
	public synchronized void recycle() {
		thread = null ;
		mpLayer = null ;
		
		// Store information for each player.
		playerGame = null ;
		playerGameInformation = null ;
		playerGameEvents = null ;
		playerName = null ;
		playerActionAdapter = null ;
		playerActionCycleState = null ;
		grBuilder = null ;
	}
	
	public synchronized void join() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't join if never started!") ;
		
		ThreadSafety.waitForThreadToTerminate(thread, 2000) ;
	}
	
	
	public synchronized long age() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't get age if never started!") ;
		
		return System.currentTimeMillis() - thread.creationTime ;
	}
	
	
	public synchronized long timeSinceGameOver() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't get age if never started!") ;
		
		if ( gameStatus == GameCoordinator.STATUS_GAME_OVER )
			return System.currentTimeMillis() - thread.startGameOverTime ;
		return 0 ;
	}
	
	
	public synchronized long timeSinceGamePlay() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't get age if never started!") ;
		
		if ( gameStatus == GameCoordinator.STATUS_GO )
			return 0 ;
		if ( thread.lastTimePlay == 0 )
			return age() ;
		return System.currentTimeMillis() - thread.lastTimePlay ;
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
	public void mpld_messagePassingConnectionDidReceiveMessage( MessagePassingLayer layer, int connNum ) {
		// System.out.println("GameCoordinator mpld_messagePassingConnectionDidReceiveMessage: " + connNum) ;
		
		// Put a message for the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendSlottedMessage(
					ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED, connNum) ;
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
	public void mpld_messagePassingConnectionDoneReceivingMessages( MessagePassingLayer layer, int connNum ) {
		// System.out.println("GameCoordinator mpld_messagePassingConnectionDoneReceivingMessages: " + connNum) ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendSlottedMessage(
					ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES, connNum) ;
		}
	}
	
	/**
	 * This method is called upon the connection entering CONNECTION status.
	 * 
	 * @param conn
	 */
	public void mpld_messagePassingConnectionDidConnect( MessagePassingLayer layer, int connNum ) {
		// System.out.println("GameCoordinator mpld_messagePassingConnectionDidConnect: " + connNum) ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendSlottedMessage(
					ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED, connNum) ;
		}
	}
	
	/**
	 * This method is called when a connection attempt fails.
	 * 
	 * @param conn
	 */
	public void mpld_messagePassingConnectionDidFailToConnect( MessagePassingLayer layer, int connNum ) {
		// System.out.println("GameCoordinator mpld_messagePassingConnectionDidFailToConnect: " + connNum) ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendSlottedMessage(
					ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED, connNum) ;
		}
	}
	
	/**
	 * This method is called upon the connection breaking.
	 * 
	 * @param conn
	 */
	public void mpld_messagePassingConnectionDidBreak( MessagePassingLayer layer, int connNum ) {
		// System.out.println("GameCoordinator mpld_messagePassingConnectionDidBreak: " + connNum) ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendSlottedMessage(
					ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE, connNum) ;
		}
	}
	
	/**
	 * This method is called upon the connection being disconnected by a peer.
	 * 
	 * @param conn
	 */
	public void mpld_messagePassingConnectionDidDisconnectByPeer( MessagePassingLayer layer, int connNum ) {
		// System.out.println("GameCoordinator mpld_messagePassingConnectionDidDisconnectByPeer: " + connNum) ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendSlottedMessage(
					ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED, connNum) ;
		}
	}
	
}
