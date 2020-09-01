package com.peaceray.quantro.lobby;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.SocketAddress;
import java.util.Hashtable;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.SlottedHandler;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingLayer;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.communications.LobbyMessage;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.ThreadSafety;

public class LobbyCoordinator implements MessagePassingLayer.Delegate {
	
	public static final String TAG = "LobbyCoordinator" ;
	
	/**
	 * A LobbyCoordinator can't handle everything needed from a LobbyServer.
	 * Some tasks are offloaded to a delegate, defined as a class implementing
	 * this interface.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {
		/*
		 * Requests.  These methods are called to request specific information from
		 * the delegate.
		 */
		
		
		/**
		 * Do we need a valid Authorization Token for this game mode?  If 'true', we only
		 * begin countdowns for game modes for which we have a verified auth token.
		 */
		public boolean lcd_requestNeedsAuthToken( int gameMode ) ;
		
		
		/**
		 * Requests that the provided Authorization token be verified as OK for use.
		 * 
		 * If 'true' is returned, we assume that the provided Auth token is valid to
		 * access the provided game mode and distribute it (if needed) to the connected
		 * clients.
		 * 
		 * If 'false' is returned, we do not store the token, and might even kick the
		 * sender.
		 * 
		 * For that reason, please assume that 'null' tokens (or Boolean "TRUE", for
		 * example) qualify as acceptable Authorization tokens for non-premium, 
		 * universal content.
		 */
		public boolean lcd_requestVerifyAuthToken( int gameMode, Serializable authToken ) ;
		
		
		/**
		 * Requests the default player color to apply to someone with the provided player slot,
		 * lobby nonce and personalNonce.  This method should always return the same color
		 * if provided the same slot, nonce and pnonce.  It is up to the delegate whether it
		 * returns the same color when given slightly different input (e.g., a different 
		 * player slot).
		 * 
		 * @returns A color for this player, which must not be 0 = 0x00000000.
		 */
		public int lcd_requestDefaultPlayerColor( int playerSlot, Nonce n, Nonce personalNonce ) ;
		
		
		/**
		 * Requests the list of game modes available in this lobby.  The value returned
		 * should be an integer array of length at least 1, with the game mode numbers
		 * of all available game modes.  This method will generally be called once per
		 * LobbyCoordinator.  This list of game modes will be used to allocate structures
		 * in the LobbyCoordinator, and will be provided to connected clients.
		 */
		public int [] lcd_requestGameModesAvailable( LobbyCoordinator lobbyCoordinator ) ;
		
		
		/**
		 * Requests the XML text describing the specified game mode.  We guarantee that
		 * this method will be called after lcd_requestGameModesAvailable (if this method
		 * is called at all) and that the gameMode specified was one of those provided 
		 * in the return value of that call.
		 * 
		 * @param lobbyCoordinator
		 * @param gameMode
		 * @return
		 */
		public String lcd_requestGameModeXMLText( LobbyCoordinator lobbyCoordinator, int gameMode ) ;
		
		
		/**
		 * Requests that the delegate prepare (or at least investigate) a Game server
		 * for an upcoming game, and set the relevant fields in the provided LobbyCoordinator.
		 * 
		 * If this method returns 'true', then the next Game server information has been
		 * determined, and the following methods have been called on the LobbyCoordinator:
		 * 		public void setNextGameNonce( Nonce nonce )
		 * 		public void setNextGamePort( int port ) 
		 * 		public void setNextGameAddress( String addrAsString ) 
		 * 		public void setNextGameHost( int slot ) 
		 * 		public void setNextGameHostIsMediated( boolean mediated ) 
		 * 
		 * @param lobbyCoordinator The LobbyCoordinator making the call
		 * @param gameMode: we will be running a server for the specified game mode.
		 * @param playerIncluded: A boolean array determining whether the specified player is
		 * 				included in the game launch.
		 * @param personalNonces: The PersonalNonces of all players, if relevant.  Specifically,
		 * 				you should note the personal nonces of those players specified in
		 * 				playerIncluded.
		 * @return
		 */
		public boolean lcd_requestGameAddressAndHostingInformation( LobbyCoordinator lobbyCoordinator, int gameMode, boolean [] playerIncluded, Nonce [] personalNonces, SocketAddress [] remoteSocketAddresses ) ;
		
		
		
		/**
		 * A request from the Delegate of a host priority number.  This number should
		 * 1. be partially based on circumstances, e.g., if we have previously become
		 * a client to for some other host, maybe we should have a low priority now?,
		 * and 2. be partially randomized, to prevent deadlock.
		 * 
		 * For example, here is a proposed "host priority algorithm":
		 * 
		 * int k;
		 * If previously client, and 2nd person to connect to mediator, 		k = 0
		 * If previously client, and 1st person to connect to mediator, 		k = 1
		 * If never client or server, and 2nd person to connect to mediator, 	k = 2
		 * If never client or server, and 1st person to connect to mediator, 	k = 3
		 * If previously host, and 2nd person to connect, 						k = 4
		 * If previously host, and 1st person to connect,						k = 5
		 * 
		 * return k * 100000 + Random.nextInt(100000) ;
		 * 
		 * @return
		 */
		public int lcd_requestHostPriority() ;
		
		
		
		
		/*
		 * ************************************************************************
		 * 
		 * CLIENT/HOST TRANSITION
		 * 
		 * Based on messages received from another party, and the host priority set
		 * by the delegate, we should transition from host to client.
		 * 
		 * *************************************************************************
		 */
		
		/**
		 * Tells the Delegate that we believe we should become a client, rather than
		 * act as the host.  The parameter is the currently connected "client" who
		 * will serve as the host for the game.
		 */
		public void lcd_shouldBecomeClient( int hostConnectionNumber ) ;
		
		
		/**
		 * Tells the delegate we have stopped operation with a set of open connections, as requested.
		 * This method will only be called if stopAndProvideOpenConnections() is called to stop
		 * the Coordinator.
		 * @param layer
		 */
		public void lcd_stoppedWithOpenConnections( MessagePassingLayer layer ) ;
	}
	
	
	private static final int COUNTDOWN_DELAY = 5999 ;
	private static final int COUNTDOWN_RETRY_DELAY = 10999 ;	// 10 seconds if failed
	
	// Orders from above!  Connect or disconnect our connections.
	private static final int ANDROID_MESSAGE_TYPE_CONNECT 							= 0 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECT_ALL 						= 1 ;
	private static final int ANDROID_MESSAGE_TYPE_DISCONNECT_ALL 					= 2 ;
	private static final int ANDROID_MESSAGE_TYPE_STOP 								= 3 ;
	private static final int ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTIONS = 4 ;
	
	// A delayed message to disconnect some we haven't welcomed.  This 
	private static final int ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED		= 5 ;
	
	// The MessagePassing layer status updated.
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED 			= 6 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED				= 7 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE				= 8 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED	= 9 ;
	
	// The MessagePassing layer has a message for us!
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED 				= 10 ;
	private static final int ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES 		= 11 ;
	
	// Self-passed messages for game launches.
	private static final int ANDROID_MESSAGE_TYPE_GAME_LAUNCH_COUNTDOWN_EXPIRED 		= 12 ;
	// Self-passed message for a broken or peer-disconnected connection, just in case
	// DONE_RECEIVING_MESSAGES is never received.  We can't rely on that assumption for some reason.
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION	 	= 13 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION	= 14 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION = 15 ;
	
	// For the above, we use android.os.Message instances with what the Type, arg1
	// the connection index.
	
	public static final int MAX_TEXT_MESSAGE_LENGTH = 150 ;
	
	public static final int MAX_HOST_DISPUTE_TIME = 10 * 1000 ;		// no more than 10 seconds
	
	public static final long DELAYED_HANDLE_DELAY = 5000 ;	// 5 seconds
	
	
	class LobbyCoordinatorThread extends Thread {
		
		public SlottedHandler handler ;
		
		boolean running ;
		
		int lastPrioritySent ;
		
		// Connectivity
		LobbyMessage outgoingMessage ;	// reserved for use by handleIncomingMessage.
		LobbyMessage tempMessage ;		// usage of this message is simple: set and send in a single step.
										// any method may use this message, but remember that any method
										// call of the form this.*() could change its contents.  Set
										// then IMMEDIATELY send.
		
		boolean [] booleanPlayerArray ;
		
		// This is used to discuss client/host responsibility.
		boolean [] connectedAsClient ;		// when 'true', the player is connected, and has accepted responsibility as a client.
		boolean [] hostResponsibilityConflict ;		// a Connected player wants to play host!
		
		
		public LobbyCoordinatorThread( ) {
			outgoingMessage = new LobbyMessage() ;
			tempMessage = new LobbyMessage() ;
			
			booleanPlayerArray = new boolean[maxPeople] ;
			connectedAsClient = new boolean[maxPeople] ;
			hostResponsibilityConflict = new boolean[maxPeople] ;
			for ( int i = 0; i < connectedAsClient.length; i++ )
				connectedAsClient[i] = hostResponsibilityConflict[i] = false ;
			
			running = true ;
		}
		
		
		@Override
		public void run() {
			
			Log.v(TAG,"Lobby thread starting!") ;
			
			// You have to prepare the looper before creating the handler.
	        Looper.prepare();
	        
	        // Create the handler so it is bound to this thread's message queue.
	        handler = new SlottedHandler(maxPeople) {
	        	
	        	
	        	private void removeAllMessagesRegardingPlayerConnection(int slot) {
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED, slot) ;
	        		
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED, slot) ;
	        		
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED, slot) ;
	        		removeSlottedMessages(ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES, slot) ;
	        		
            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION, slot) ;
            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION, slot) ;
            		removeSlottedMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION, slot) ;
	        	}
	        	

	            public void handleMessage(android.os.Message msg) {

	            	/*
	            	 * IMPLEMENT MESSAGE HANDLER HERE.
	            	 */
	            	
	            	int type = msg.what ;
	            	int slot = getSlot(msg) ;
	            	int countdownNumber = msg.arg1 ;
	            	
	            	MessagePassingConnection conn ;
	            	android.os.Message selfMessage ;
	            	LobbyMessage lm ;
	            	
	            	Delegate delegate = mwrDelegate.get() ;
	            	if ( delegate == null && type != ANDROID_MESSAGE_TYPE_STOP ) {
	            		handler.sendMessage(handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP)) ;
	            		return ;
	            	}
	            	
	            	switch( type ) {
	            	////////////////////////////////////////////////////////////
	            	// CONNECTION COMMANDS
	            	//
	            	case ANDROID_MESSAGE_TYPE_CONNECT:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_CONNECT") ;
	            		conn = mpLayer.connection(slot) ;
	            		if ( !conn.isConnected() ) {
		            		removeAllMessagesRegardingPlayerConnection(slot) ;
		            		// We should start connecting the specified connection.
		            		// Disconnect if not currently in a connect-ready state.
		            		if ( conn.isAbleToDisconnect() ) {
		            			Log.d(TAG, "disconnecting slot " + slot) ;
		            			conn.disconnect() ;
		            			updateForDisconnectedPlayer(slot) ;
		            		}
		            		// Connect it.
		            		connectedAsClient[slot] = false ;
		            		hostResponsibilityConflict[slot] = false ;
		            		conn.connect() ;
	            		}
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_CONNECT_ALL:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_CONNECT_ALL") ;
	            		// We should connect EVERY connection.  Do this with a call to mpLayer.connect().
	            		// However, there is a complication: some connections might be currently connected.
	            		// For those, we don't connect (note: the call to mpLayer.connect()) will ONLY
	            		// connect those not currently connected!); instead, we send a "I AM HOST" message
	            		// to get the ball rolling on a full connection setup.
	            		for ( int i = 0; i < mpLayer.numConnections(); i++ )
	            			if ( mpLayer.connection(i).isConnected() && playerColor[i] == 0 )
	            				playerColor[i] = delegate.lcd_requestDefaultPlayerColor(i, lobby.getLobbyNonce(), mpLayer.connection(i).getRemotePersonalNonce()) ;
	            		for ( int i = 0; i < mpLayer.numConnections(); i++ ) {
	            			hostResponsibilityConflict[i] = connectedAsClient[i] = false ;
	            			if ( mpLayer.connection(i).isConnected() ) {
	            				// Log.v(TAG, "telling " + i + " they are client") ;
	            				if ( playerColor[i] == 0 )
	            					playerColor[i] = delegate.lcd_requestDefaultPlayerColor(i, lobby.getLobbyNonce(), mpLayer.connection(i).getRemotePersonalNonce()) ;
	            				sendWelcomeMessages( i, true ) ;		// tell everyone
	            				connectedAsClient[i] = true ;
	            			}
	            		}
	            		mpLayer.connect() ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_DISCONNECT_ALL:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_DISCONNECT_ALL") ;
	            		// Disconnect EVERY connection.
	            		mpLayer.disconnect() ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_STOP:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_STOP") ;
	            		// Stop.  Stop this thread.  Stop everything.
	            		lm = new LobbyMessage() ;
	            		lm.setAsServerClosing() ;
	            		mpLayer.broadcast(lm) ;
	            		mpLayer.disconnect() ;
	            		lobby.resetMembership() ;
	            		getLooper().quit() ;
	            		running = false ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTIONS:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTIONS") ;
	            		// Stop.  Stop this thread.  Stop everything.  However, do NOT
	            		// close our mpLayer - don't even broadcast a server closing
	            		// message.  Instead, provide the mpLayer to the delegate
	            		// in a callback.
	            		getLooper().quit() ;
	            		running = false ;
	            		mpLayer.setDelegate(null) ;
	            		lobby.resetMembership() ;
	            		delegate.lcd_stoppedWithOpenConnections(mpLayer) ;
	            		break ;
	            		
	            	
	            	////////////////////////////////////////////////////////////
	            	// SWORD OF DAMACLES
	            	// 
	            	case ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED") ;
	            		if ( !connectedAsClient[slot] && mpLayer.connection(slot).connectionStatus() == MessagePassingConnection.Status.CONNECTED ) {
	            			removeAllMessagesRegardingPlayerConnection(slot) ;
	            			Log.d(TAG, "disconnecting slot " + slot) ;
	            			mpLayer.connection(slot).disconnect() ;
	            			updateForDisconnectedPlayer(slot) ;
	            			
	            			sendSlottedMessage( ANDROID_MESSAGE_TYPE_CONNECT, slot ) ;
            			}
	            		break ;
	            		
	            		
	            	////////////////////////////////////////////////////////////
		            // MESSAGE PASSING LAYER STATUS UPDATES
		            //
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_CONNECTED") ;
	            		// Someone's connected!  DON'T welcome them.  Tell them we are the host.
	            		mpLayer.connection(slot).sendMessage( tempMessage.setAsIAmHost() ) ;
	            		connectedAsClient[slot] = false ;
	            		playerColor[slot] = 0 ;
	            		// Set up a delayed self-message to kick this player if they haven't accepted
	            		// a client role in X seconds.
	            		sendSlottedMessageDelayed(ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED, slot, MAX_HOST_DISPUTE_TIME) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_FAILED") ;
	            		// A connection attempt failed.  NOTE: this call
	            		// will be FOLLOWED by a call to DONE_RECEIVING_MESSAGES;
	            		// to avoid redundant operations, disconnect() here
	            		// to prevent that call.
	            		// TODO: Implement a better way to handle failures.
	            		// For example, maybe we should track the number of failed
	            		// attempts to tell the host?  Maybe let other players
	            		// vote to remove the player in question?
	            		conn = mpLayer.connection(slot) ;
	            		if ( conn.isAbleToDisconnect() ) {
	            			Log.d(TAG, "disconnecting slot " + slot) ;
	            			mpLayer.connection(slot).disconnect() ;
	            			updateForDisconnectedPlayer(slot) ;
	            		}
	            		// Tell ourselves to reconnect.
	            		if ( running ) {
	            			sendSlottedMessage(ANDROID_MESSAGE_TYPE_CONNECT, slot) ;
	            		}
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_BROKE") ;
	            		// A connection broke.  This call will be FOLLOWED by
	            		// a call to DONE_RECEIVING_MESSAGES.  It is at that
	            		// moment that we should set up a reconnect - we
	            		// want to make sure we handle all the messages on this
	            		// connection before kicking them out.
	            		sendSlottedMessageDelayed(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION,
	            				slot, DELAYED_HANDLE_DELAY) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_CONNECTION_PEER_DISCONNECTED") ;
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
	            		sendSlottedMessageDelayed(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION,
	            				slot, DELAYED_HANDLE_DELAY) ;
	            		break ;
	            	
	            	
	            	////////////////////////////////////////////////////////////
			        // MESSAGE PASSING LAYER MESSAGES RECEIVED AND DONE
			        //
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_MESSAGE_RECEIVED") ;
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
	            			// Received a message, but no messages to get?  WTF?  Kill.
	            			// schedule a disconnect
	            			sendSlottedMessageDelayed(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION, slot, 2000) ;
	            			break ;
	            		}
	            		// Otherwise, handle the message.
	            		lm = (LobbyMessage) conn.getMessage() ;
	            		if ( connectedAsClient[slot] ) {
		            		if ( !handleIncomingMessage( lm, slot ) ) {
		            			// disconnect the connection.  Set up a reconnect.
		            			Log.d(TAG, "disconnecting slot " + slot) ;
		            			conn.disconnect() ;
		            			updateForDisconnectedPlayer(slot) ;
		            			sendSlottedMessage(ANDROID_MESSAGE_TYPE_CONNECT, slot) ;
		            		}
	            		}
	            		else {
	            			// Otherwise, this user is still establishing their role
	            			// in our interaction.  We have already sent an "I am host"
	            			// message.  Let's hear them out.  BTW, the client can send
	            			// a preferred color message at this time (before I AM CLIENT)
	            			// so that we don't get an awkward color-change effect after
	            			// they are admitted.
	            			switch( lm.getType() ) {
	            			case LobbyMessage.TYPE_PREFERRED_COLOR:
	            				// I want to be a color!
	            				playerColor[slot] = lm.getColor() ;
	            				// We don't need to broadcast this.  They have not been welcomed yet;
	            				// upon being welcomed, we will send their color to everyone.
	            				break ;
	            				
	            			case LobbyMessage.TYPE_I_AM_CLIENT:
	            				// :D
	            				connectedAsClient[slot] = true ;
	            				hostResponsibilityConflict[slot] = false ;
	            				// Remove the Sword of Damacles.
	            				removeSlottedMessages(ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED, slot) ;
	            				// get a default color if we need it.
	            				if ( playerColor[slot] == 0 )
	            					playerColor[slot] = delegate.lcd_requestDefaultPlayerColor(slot, lobby.getLobbyNonce(), mpLayer.connection(slot).getRemotePersonalNonce()) ;
	            				sendWelcomeMessages( slot, true ) ;		// tell everyone
	            				connectedAsClient[slot] = true ;
	    	            		break ;
	    	            		
	            			case LobbyMessage.TYPE_I_AM_HOST:
	            				// D:
	            				hostResponsibilityConflict[slot] = true ;
	            				lastPrioritySent = delegate.lcd_requestHostPriority() ;
	            				mpLayer.connection(slot).sendMessage( tempMessage.setAsHostPriority(lastPrioritySent ) ) ;
	            				break ;
	            				
	            			case LobbyMessage.TYPE_HOST_PRIORITY:
	            				// :|
	            				if ( !hostResponsibilityConflict[slot] ) {
	            					kickPlayer(slot, "At least tell us you want to be the host before you start firing priorities at us!") ;
	            					conn = mpLayer.connection(slot) ;
	            					if ( conn.isAbleToDisconnect() ) {
	            						Log.d(TAG, "disconnecting slot " + slot) ;
	            						mpLayer.connection(slot).disconnect() ;
	            						updateForDisconnectedPlayer(slot) ;
	            					}
		            				// Remove the Sword of Damacles.
	            					removeSlottedMessages(ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED, slot) ;
	            					sendSlottedMessage(ANDROID_MESSAGE_TYPE_CONNECT, slot) ;
	            				}
	            				else {
	            					// Check against our last sent.
	            					int otherPriority = lm.getPriority() ;
	            					if ( otherPriority == lastPrioritySent ) {
	            						// do it again
	            						lastPrioritySent = delegate.lcd_requestHostPriority() ;
	            						mpLayer.connection(slot).sendMessage( tempMessage.setAsHostPriority(lastPrioritySent ) ) ;
	            					}
	            					else if ( otherPriority < lastPrioritySent ) {
	            						// Good news everyone - we remain the host.  Send nothing; wait
	            						// for them to send a "I AM CLIENT" message, upon which all
	            						// will be forgiven.
	            					}
	            					else if ( otherPriority > lastPrioritySent ) {
	            						// OH NOES!!  The new client wants to be host, and has a higher
	            						// priority!  Tell the delegate; it will know what to do.
	            						// Either way we don't send anything else on the connection.
	            						delegate.lcd_shouldBecomeClient(slot) ;
	            					}
	            				}
	            				break ;
	            				
	            			default:
	            				// something else.  disconnect; queue up connection.
	            				Log.d(TAG, "disconnecting slot " + slot + " for message " + lm.getType()) ;
	            				mpLayer.connection(slot).disconnect() ;
	            				updateForDisconnectedPlayer(slot) ;
	            				
	            				// Remove the Sword of Damacles.
	            				removeSlottedMessages(ANDROID_MESSAGE_TYPE_DISCONNECT_IF_NOT_WELCOMED, slot) ;
            					sendSlottedMessage(ANDROID_MESSAGE_TYPE_CONNECT, slot) ;
	            			}
	            		}
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION:
	            		/*
	            		if ( type == ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES )
	            			Log.v(TAG, "ANDROID_MESSAGE_TYPE_MPLAYER_DONE_RECEIVING_MESSAGES") ;
	            		else if ( type == ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION )
	            			Log.v(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION") ;
	            		else
	            			Log.v(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION") ;
	            			*/
	            		removeAllMessagesRegardingPlayerConnection(slot) ;
	            		
	            		// This connection, which was previously used for passing messages,
	            		// will never send another message again.  Obviously something went
	            		// wrong; for example, the player may have disconnected.
	            		// Update all our stuff.  This player is no longer in the game.
	            		if ( mpLayer.connection(slot).connectionStatus() != MessagePassingConnection.Status.DISCONNECTED ) {
	            			// Cut the connection; queue up a reconnect.
	            			MessagePassingConnection c = mpLayer.connection(slot) ;
	            			Log.d(TAG, "disconnecting slot " + slot) ;
	            			c.disconnect();
	            			// mpLayer.connection(slot).disconnect();
        					sendSlottedMessage(ANDROID_MESSAGE_TYPE_CONNECT, slot) ;
	            		}
	            		updateForDisconnectedPlayer(slot) ;
	            		
	            		break ;
	            		
	            		
	            	case ANDROID_MESSAGE_TYPE_GAME_LAUNCH_COUNTDOWN_EXPIRED:
	            		// Log.v(TAG, "ANDROID_MESSAGE_TYPE_GAME_LAUNCH_COUNTDOWN_EXPIRED") ;
	            		// A countdown has expired.  Check whether the countdown
	            		// is currently still active.  If so, perform the launch.
	            		// If not... don't.
	            		if ( lobby.getCountdownStatus(countdownNumber) == Lobby.COUNTDOWN_STATUS_ACTIVE ) {
	            			// LAUNCH!
	            			launch( countdownNumber ) ;
	            		}
	            		break ;
	            	}
	                
	            }
	        };
	        
	        

	        /*
	         * Start looping the message queue of this thread.
	         */
	        // Log.v(TAG,"Lobby thread entering loop.") ;
	        Looper.loop();
			
			
			// Finished.  Close our connections.
			// outgoingMessage.setAsServerClosing() ;
			// mpLayer.broadcast(outgoingMessage) ;
			// mpLayer.disconnect() ;
	        // NOTE: these are commented out because we want to hand-off open
	        // connections between Coordinator and ClientConnections.
		}
		
		
		/**
		 * Attempts to welcome the specified player by sending them a collection
		 * of status messages, finishing up with a "welcome to server."  Returns
		 * whether it appears that the messages were sent off successfully.
		 * 
		 * Will also send the new player a playersInLobby message, but will NOT
		 * broadcast the players in lobby to all connected users (it is assumed
		 * that they know).
		 * 
		 * If 'tellEveryone,' this method will also send the name of the new player
		 * to all other currently welcomed players.  This is appropriate the first
		 * time a player joins, but not if a resident player has requested a rewelcome.
		 * 
		 *  and will 
		 * 
		 * @param playerSlot
		 * @return Does it look like everything worked?
		 */
		public boolean sendWelcomeMessages( int playerSlot, boolean tellEveryone ) {
			lobby.setName(playerSlot, mpLayer.connection(playerSlot).getRemoteName()) ;
			lobby.setPersonalNonce(playerSlot, mpLayer.connection(playerSlot).getRemotePersonalNonce()) ;
			
			
			try {
				// To welcome a player, we send them updates on the entire lobby status,
				// including votes, available game modes, and countdown(s).
				mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsYouAreClient() ) ;
				mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsHost(ownerSlot, ownerName) ) ;
				mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsLobbyStatus( versionCode, lobby.getLobbyNonce(), lobby.getNumPeople(), lobby.getMaxPeople(), lobby.getAge(), null, lobby.getLobbyName(), ownerName)) ;
				mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsPersonalPlayerSlot(playerSlot) ) ;
				mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsGameModeList(lobby.getGameModes()) ) ;
				
				// Send names of other connected players; send this player's name to them.
				boolean [] playersInLobby = lobby.getPlayersInLobby() ;
				if ( tellEveryone )
					mpLayer.sendTo( tempMessage.setAsPlayerName(
														playerSlot,
														mpLayer.connection(playerSlot).getRemoteName()),
									playersInLobby ) ;
				for ( int i = 0; i < playersInLobby.length; i++ ) {
					if ( playersInLobby[i] ) {
						mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsPlayerName(i, lobby.getPlayerName(i) ) ) ;
					}
				}
				
				// If broadcast pnonces, send this player everyone's pnonce, and
				// broadcast this pnonce to all players.
				if ( broadcastPersonalNonces ) {
					if ( tellEveryone ) {
						tempMessage.setAsPersonalNonce(playerSlot, mpLayer.connection(playerSlot).getRemotePersonalNonce()) ;
						mpLayer.sendTo(tempMessage, playersInLobby) ;
					}
					for ( int i = 0; i < playersInLobby.length; i++ ) {
						// bug: sent the player HIS OWN personal nonce, instead of that of player 'i'!
						// Oddly, this only rarely affected the game.  Occassionally a game
						// would be launched in which the server continually rejected the
						// host for having the wrong nonce.  Don't know what the different was,
						// though...
						if ( playersInLobby[i] ) {
							tempMessage.setAsPersonalNonce(i, mpLayer.connection(i).getRemotePersonalNonce()) ;
							mpLayer.connection(playerSlot).sendMessage(tempMessage) ;
						}
					}
				}
				
				// Game-mode related messages: auth tokens and votes.
				int [] gameModes = lobby.getGameModes() ;
				
				// Send the current Auth tokens.
				for ( int i = 0; i < gameModes.length; i++ ) {
					Integer gameModeInteger = Integer.valueOf(gameModes[i]) ;
					Integer owner = authTokenOwner.get( gameModeInteger ) ;
					if ( owner != null ) {
						// send the associated auth token.
						Serializable token = authTokens_byPlayer[owner.intValue()].get( gameModeInteger ) ;
						if ( token != null ) {
							mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsGameModeAuthorizationToken(gameModes[i], token) ) ;
						}
					}
				}
				
				// Send all game mode votes.
				for ( int i = 0; i < gameModes.length; i++ ) {
					mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsGameModeVotes(gameModes[i], lobby.getVotes(gameModes[i]))) ;
				}
				
				// Send all countdowns.
				int [] countdown_number = new int[lobby.getMaxPeople()] ;
				int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
				long [] countdown_timeLeft = new long[lobby.getMaxPeople()] ;
				int [] countdown_status = new int[lobby.getMaxPeople()] ;
				boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
				int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, countdown_timeLeft, countdown_playerIncluded, countdown_status, null) ;
				for ( int c = 0; c < numCountdowns; c++ ) {
					tempMessage.setAsLaunchCountdown(
							countdown_number[c],
							countdown_gameMode[c],
							countdown_timeLeft[c],
							countdown_status[c],
							countdown_playerIncluded[c]) ;
					mpLayer.connection(playerSlot).sendMessage( tempMessage ) ;
				}
				
				// Send the color assigned to this user to all others, and all other colors to
				// this guy.  Also send this guy his OWN color, just in case he's using the default.
				tempMessage.setAsPreferredColor(playerSlot, playerColor[playerSlot]) ;
				mpLayer.connection(playerSlot).sendMessage(tempMessage) ;
				if ( tellEveryone )
					mpLayer.sendTo(tempMessage, playersInLobby) ;
				for ( int i = 0; i < playersInLobby.length; i++ )
					if ( playersInLobby[i] )
						mpLayer.connection(playerSlot).sendMessage(tempMessage.setAsPreferredColor(i, playerColor[i])) ;
				
				// Finish up and welcome to the server.
				lobby.setPlayerInLobby(playerSlot, true) ;
				lobby.setPlayerActive(playerSlot) ;
				if ( tellEveryone ) {
					mpLayer.sendTo(
	        				tempMessage.setAsPlayersInLobby(lobby.getPlayersInLobby()),
	        				lobby.getPlayersInLobby()) ;
					mpLayer.sendTo(
							tempMessage.setAsPlayerStatuses(lobby.getPlayerStatuses()),
	        				lobby.getPlayersInLobby()) ;
				} else {
					mpLayer.connection(playerSlot).sendMessage(
	        				tempMessage.setAsPlayersInLobby(lobby.getPlayersInLobby())) ;
					mpLayer.connection(playerSlot).sendMessage(
							tempMessage.setAsPlayerStatuses(lobby.getPlayerStatuses())) ;
				}
				mpLayer.connection(playerSlot).sendMessage( tempMessage.setAsWelcomeToServer() ) ;
				
				return ( mpLayer.connection(playerSlot).connectionStatus() == MessagePassingConnection.Status.CONNECTED ) ;
 			} catch( IllegalStateException e ) {
 				e.printStackTrace() ;
 			}
 			return false ;
		}
		
		
		private boolean startNewLaunchCountdownsIfAppropriate() {
			boolean didStart = false ;
			int [] gameModes = lobby.getGameModes() ;
			for ( int i = 0; i < gameModes.length; i++ )
				didStart = startLaunchCountdownIfAppropriate( gameModes[i], 0 ) || didStart ;
			return didStart ;
		}
		
		
		/**
		 * Examines the current votes for the specified game mode, starting a launch
		 * countdown if appropriate.  Will return whether a countdown was started.
		 * 
		 * This method will, if appropriate, make a call to startLaunchCountdown.
		 * That call will broadcast the countdown information.
		 * 
		 * This method will, if appropriate, put a delayed message in its message
		 * handler's queue to remind it when the countdown is up.
		 * 
		 * @param gameMode
		 * @return
		 */
		private boolean startLaunchCountdownIfAppropriate( int gameMode, int attempt ) {
			// Log.d(TAG, "startLaunchCountdownIfAppropriate") ;
			int playersNeeded = GameModes.minPlayers(gameMode) ;
			int playersIncluded = 0 ;
			
			// Count the players who have
			// 1. Voted for this game mode
			// 2. Are currently in the lobby
			// 3. Are NOT included in any other countdown.
			// 4. Are not idle (preferrably).
			boolean [] playerIncluded = new boolean[ lobby.getMaxPeople() ] ;
			boolean [] gameModeVotes = lobby.getVotes(gameMode) ;
			boolean [] playersInLobby = lobby.getPlayersInLobby() ;
			boolean allActive = true ;
			for ( int p = 0; p < playerIncluded.length; p++ ) {
				if ( playersIncluded < playersNeeded && gameModeVotes[p] && !lobby.getPlayerIsInCountdown(p) && playersInLobby[p] && lobby.getPlayerStatus(p) == Lobby.PLAYER_STATUS_ACTIVE ) {
					playersIncluded++ ;
					playerIncluded[p] = true ;
				}
				else
					playerIncluded[p] = false ;
			}

			if ( playersIncluded < playersNeeded ) {
				// try including idle players too
				allActive = false ;
				for ( int p = 0; p < playerIncluded.length; p++ ) {
					if ( !playerIncluded[p] && playersIncluded < playersNeeded && gameModeVotes[p] && !lobby.getPlayerIsInCountdown(p) && playersInLobby[p] && lobby.getPlayerStatus(p) == Lobby.PLAYER_STATUS_INACTIVE ) {
						playersIncluded++ ;
						playerIncluded[p] = true ;
					}
				}
			}
			
			if ( playersIncluded == playersNeeded ) {
				Delegate delegate = mwrDelegate.get() ;
				if ( !delegate.lcd_requestNeedsAuthToken(gameMode) || authTokenOwner.get(gameMode) != null ) {
					setLaunchCountdownInLobbyAndBroadcast( gameMode, playerIncluded, attempt, allActive ) ;
					return true ;
				}
			}
			
			return false ;
		}
		
		
		/**
		 * Start a countdown for the specified game mode index.
		 * Broadcasts the appropriate message.  Returns the countdown
		 * number given to this countdown.  Also establishes an android
		 * message on the Handler that will inform us when the countdown
		 * expires.
		 * 
		 * @param gameModeIndex
		 * @param The countdown number specified for this countdown.
		 */
		private int setLaunchCountdownInLobbyAndBroadcast( int gameMode, boolean [] playerIncluded, int attempt, boolean startCounting ) {
			// Add the actual countdown.
			// TODO: Add actual countdown delays.
			setLaunchCountdownInLobbyAndBroadcast( nextCountdownNumber, gameMode, playerIncluded, attempt, startCounting ) ;
			
			int num = nextCountdownNumber ;
			nextCountdownNumber++ ;
			return num ;
		}
		
		
		/**
		 * Sets up a launch countdown and broadcasts this fact.  Also registers an
		 * android message which will inform us when this countdown expires.
		 * @param countdownNumber
		 * @param gameMode
		 * @param playerIncluded
		 */
		private void setLaunchCountdownInLobbyAndBroadcast( int countdownNumber, int gameMode, boolean [] playerIncluded, int attempt, boolean startCounting ) {
			// Log.d(TAG, "setLaunchCountdownInLobbyAndBroadcast") ;
			Object tag = lobby.getCountdownTag(countdownNumber) ;
			if ( tag != null )
				handler.removeMessages(ANDROID_MESSAGE_TYPE_GAME_LAUNCH_COUNTDOWN_EXPIRED, tag) ;
			
			int status = startCounting ? Lobby.COUNTDOWN_STATUS_ACTIVE : Lobby.COUNTDOWN_STATUS_HALTED ;
			lobby.setCountdownDelay(
					countdownNumber, gameMode, playerIncluded,
					attempt == 0 ? COUNTDOWN_DELAY : COUNTDOWN_RETRY_DELAY,
					status,
					Integer.valueOf(nextCountdownNumber) ) ;	// minimal countdown!
			
			
			android.os.Message msg = handler.obtainMessage(ANDROID_MESSAGE_TYPE_GAME_LAUNCH_COUNTDOWN_EXPIRED,
					countdownNumber,
					0,
					lobby.getCountdownTag(countdownNumber)) ;
			handler.sendMessageDelayed(msg, lobby.getCountdownDelay(countdownNumber)) ;
			
			// Broadcast!
			boolean [] playersInLobby = lobby.getPlayersInLobby() ;
			mpLayer.sendTo(tempMessage.setAsLaunchCountdown(countdownNumber, gameMode, COUNTDOWN_DELAY, status, playerIncluded), playersInLobby) ;
		}
		
		
		
		private void setLaunchAbortedInLobbyAndBroadcast( int countdownNumber, int gameMode, boolean [] playerIncluded, boolean failure ) {
			mpLayer.sendTo(
					tempMessage.setAsLaunchAborted(
							countdownNumber,
							gameMode,
							playerIncluded),
					lobby.getPlayersInLobby() ) ;
			lobby.setCountdownAborted(countdownNumber, failure) ;
		}
		
		
		private boolean launch( int countdownNumber ) {
			// Log.d(TAG, "launch") ;
			// Get the countdown info.
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, null, null) ;
			
			int index = -1 ;
			for ( int c = 0; c < numCountdowns; c++ )
				if ( countdownNumber == countdown_number[c] )
					index = c ;
			
			// Can't launch a countdown that doesn't exist.
			if ( index < 0 )
				return false ;
			
			int number = countdown_number[index] ;
			int gameMode = countdown_gameMode[index] ;
			boolean [] playerIncluded = countdown_playerIncluded[index] ;
			
			Delegate delegate = mwrDelegate.get() ;
			if ( delegate == null )
				return false ;
			
			SocketAddress [] remoteAddresses = new SocketAddress[lobby.getMaxPeople()] ;
			for ( int i = 0; i < mpLayer.numConnections(); i++ ) {
				if ( mpLayer.connection(i) != null )
					remoteAddresses[i] = mpLayer.connection(i).getRemoteSocketAddress() ;
			}
			
			if ( delegate.lcd_requestGameAddressAndHostingInformation(LobbyCoordinator.this, gameMode, playerIncluded, lobby.getPlayerPersonalNonces(), remoteAddresses)) {
				// follow up with server information
				// Tell participating players about the game; include server info...
				boolean [] nextGameClients = new boolean[lobby.getMaxPeople()] ;
				boolean [] nextGameAbsent = new boolean[lobby.getMaxPeople()] ;
				for ( int i = 0; i < maxPeople; i++ ) {
					nextGameClients[i] = playerIncluded[i] && i != nextGameHost ;
					nextGameAbsent[i] = !playerIncluded[i] ;
				}
				
				// Tell host/clients about their responsibility, and perform the launch.
				// If WE are participating (i.e. this device) we will handle that when
				// the client end receives the information we send.
				if ( nextGameHostIsMatchseeker ) {
					// host
					tempMessage.setAsLaunchAsMatchseekerHost(
							number, gameMode, playerIncluded,
							nextGameNonce, nextGameEditKey, nextGameSockAddr ) ;
					mpLayer.connection(nextGameHost).sendMessage( tempMessage ) ;
					// clients
					tempMessage.setAsLaunchAsMatchseekerClient(
							number, gameMode, playerIncluded,
							nextGameNonce, nextGameSockAddr ) ;
					mpLayer.sendTo( tempMessage, nextGameClients ) ;
				} else {
					// host
					tempMessage.setAsLaunchAsDirectHost(
							number, gameMode, playerIncluded,
							nextGameNonce, nextGameSockAddr ) ;
					mpLayer.connection(nextGameHost).sendMessage( tempMessage ) ;
					// clients
					tempMessage.setAsLaunchAsDirectClient(
							number, gameMode, playerIncluded,
							nextGameNonce, nextGameSockAddr ) ;
					mpLayer.sendTo( tempMessage, nextGameClients ) ;
				}

				// tell everyone else about the launch (as absent)
				tempMessage.setAsLaunchAsAbsent(number, gameMode, playerIncluded) ;
				mpLayer.sendTo( tempMessage, nextGameAbsent ) ;
				
				// Remove the countdown from our lobby.  Reset all participating player's votes.
				lobby.setCountdownAborted(countdownNumber, false) ;
				for ( int i = 0; i < countdown_playerIncluded[index].length; i++ ) {
					if ( countdown_playerIncluded[index][i] )
						lobby.resetPlayerVotes(i) ;
				}
				// TODO: This is GROSSLY inefficient for more than a few game modes.
				// We should rely on lobby delegate methods if # of game modes increases.
				// Resend game votes.
				int [] gameModes = lobby.getGameModes() ;
				for ( int i = 0; i < gameModes.length; i++ ) {
					mpLayer.broadcast(tempMessage.setAsGameModeVotes(gameModes[i], lobby.getVotes(gameModes[i]))) ;
				}
				// that's it.
				return true ;
				
			} else {
				Log.d(TAG, "failed retrieving game server information.") ;
			}
			
			// failure.  Tell everyone the launch failed.
			mpLayer.broadcast( tempMessage.setAsLaunchFailed(countdown_number[index], countdown_gameMode[index], countdown_playerIncluded[index]) ) ;
			lobby.setCountdownAborted(countdownNumber, true) ;
			// restart the countdown.
			startLaunchCountdownIfAppropriate( gameMode, 1 ) ;
			return false ;
		}
		
		
		
		private boolean handleIncomingMessage( LobbyMessage m, int sender) {
			boolean hasOutgoing = false ;
			boolean broadcastOutgoing = false ;
			
			int gameMode ;
			int [] gameModeList ;
			
			Serializable token ;
			
			boolean changed ;
			
			// TODO: If new message types are added, support should be included here
			// too.
			if ( !messageContentOK( m ) ) {
				Log.v(TAG, "messageContent not OK!") ;
				return false ;
			}
			
			Delegate delegate = mwrDelegate.get() ;
			if ( delegate == null )
				return false ;
				
			switch ( m.getType() ) {
			// Some are easy.
			case Message.TYPE_EXIT:
				Log.v(TAG, "EXIT MESSAGE") ;
				updateForDisconnectedPlayer(sender) ;
				return false ;
				
			case Message.TYPE_MY_NAME:
				// Name change!
				lobby.setName(sender, m.getName()) ;
				mpLayer.sendTo(
						tempMessage.setAsPlayerName(sender, m.getName()),
						lobby.getPlayersInLobby() ) ;
				break ;
				
			case Message.TYPE_NAME_REQUEST:
				int index = m.getPlayerSlot() ;
				String name = lobby.getPlayerName(index) ;
				if ( index >= 0 && index < maxPeople && name != null ) {
					outgoingMessage.setAsPlayerName(index, name) ;
					hasOutgoing = true ;
				}
				break ;
				
			case Message.TYPE_NONCE_REQUEST:
				if ( !demandNonce ) {
					outgoingMessage.setAsNonce(lobby.getLobbyNonce()) ;
					hasOutgoing = true ;
				}
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_LIST_REQUEST:
				outgoingMessage.setAsGameModeList(lobby.getGameModes()) ;
				hasOutgoing = true ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_XML_REQUEST:
				// Only respond if this is a valid game mode.
				gameMode = m.getGameMode() ;
				gameModeList = lobby.getGameModes() ;
				for ( int i = 0; i < gameModeList.length; i++ ) {
					if ( gameModeList[i] == gameMode ) {
						outgoingMessage.setAsGameModeXML( gameMode, delegate.lcd_requestGameModeXMLText( LobbyCoordinator.this, m.getGameMode() ) ) ;
						hasOutgoing = true ;
					}
				}
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_VOTE_REQUEST:
				gameMode = m.getGameMode() ;
				gameModeList = lobby.getGameModes() ;
				for ( int i = 0; i < gameModeList.length; i++ ) {
					if ( gameModeList[i] == gameMode ) {
						outgoingMessage.setAsGameModeVotes(gameMode, lobby.getVotes(gameMode)) ;
						hasOutgoing = true ;
					}
				}
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_TOKEN:
				// Ask if this token is valid.
				gameMode = m.getGameMode() ;
				token = m.getAuthToken() ;
				if ( delegate.lcd_requestVerifyAuthToken(gameMode, token) ) {
					// store this auth token for the player.
					authTokens_byPlayer[sender].put(Integer.valueOf(gameMode), token) ;
					// if we don't already have an auth-token in use, set it as the
					// token for this game mode.
					Integer prevOwner = authTokenOwner.get(gameMode) ;
					if ( prevOwner == null || prevOwner.intValue() == sender ) {
						authTokenOwner.put(gameMode, sender) ;
						outgoingMessage.setAsGameModeAuthorizationToken(gameMode, token) ;
						mpLayer.sendTo( outgoingMessage, lobby.getPlayersInLobby() ) ;
						// start countdowns for this game mode if we can.
						this.startLaunchCountdownIfAppropriate(gameMode, 0) ;
					}
				} else {
					// kick.
					this.kickPlayer(sender, "Provided Auth token is not valid.") ;
					return false ;
				}
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_REVOKE:
				gameMode = m.getGameMode() ;
				// if this user has an auth token for this game mode, revoke it,
				// and fall back to any other available.
				token = authTokens_byPlayer[sender].get(gameMode) ;
				if ( token != null ) {
					authTokens_byPlayer[sender].remove(gameMode) ;
					Integer curOwner = authTokenOwner.get(gameMode) ;
					if ( curOwner.intValue() == sender ) {
						authTokenOwner.remove(gameMode) ;
						// look for another...
						for ( int i = 0; i < maxPeople; i++ ) {
							token = authTokens_byPlayer[i].get(gameMode) ;
							if ( token != null )
								break ;
						}
						
						if ( token == null ) {
							outgoingMessage.setAsGameModeAuthorizationRevoke(gameMode) ;
						} else {
							outgoingMessage.setAsGameModeAuthorizationToken(gameMode, token) ;
						}
						mpLayer.sendTo( outgoingMessage, lobby.getPlayersInLobby() ) ;
					}
				}
				break ;
				
			
				
			case LobbyMessage.TYPE_TEXT_MESSAGE:
				String t = m.getText();
				if ( t.length() > LobbyCoordinator.MAX_TEXT_MESSAGE_LENGTH )
					t = t.substring(0, MAX_TEXT_MESSAGE_LENGTH-3) + "..." ;
				// TODO: This is where we put it in LobbyLog, if we start
				// using one.
				outgoingMessage.setAsTextMessage( sender, t ) ;
				broadcastOutgoing = true ;
				break ;
				
			case LobbyMessage.TYPE_VOTE:
				// kick if inactive, in game, etc..  Only active players may vote.
				// Bugfixing.  The lobby sometimes enters a state where the connections appear to
				// go down permanently.  Check for that / output it here.
				for ( int i = 0; i < mpLayer.numConnections(); i++ ) {
					Log.d(TAG, "connection " + i + " has status " + mpLayer.connection(i).connectionStatus() + " ") ;
				}
				if ( lobby.getPlayerStatuses()[sender] != Lobby.PLAYER_STATUS_ACTIVE ) {
					this.kickPlayer(sender, "Inactive players may not alter their votes.") ;
					return false ;
				}
				else {
					gameMode = m.getGameMode() ;
					// Set and broadcast the player's vote for this game type.
					// Start a launch countdown (if appropriate!)
					lobby.setPlayerVoteForGameMode(sender, gameMode, true) ;
					mpLayer.sendTo(
							outgoingMessage.setAsGameModeVotes(gameMode, lobby.getVotes(gameMode)),
							lobby.getPlayersInLobby()) ;
					// see if we can get a countdown going for this player.
					// first try including in an existing countdown; if none,
					// try making a new one.
					if ( !addToExistingCountdownIfStillOkay(sender, gameMode) ) {
						startLaunchCountdownIfAppropriate(gameMode, 0) ;
					}
				}
				break ;
				
			case LobbyMessage.TYPE_UNVOTE:
				if ( lobby.getPlayerStatuses()[sender] != Lobby.PLAYER_STATUS_ACTIVE ) {
					this.kickPlayer(sender, "Inactive players may not alter their votes.") ;
					return false ;
				}
				else {
					gameMode = m.getGameMode() ;
					// Set and broadcast the player's unvote for this game type.
					// Abort any launch countdowns they're participating in.
					lobby.setPlayerVoteForGameMode(sender, gameMode, false) ;
					mpLayer.sendTo(
							outgoingMessage.setAsGameModeVotes(gameMode, lobby.getVotes(gameMode)),
							lobby.getPlayersInLobby()) ;
					
					// Step one: remove this player from any countdowns for this game mode.
					// Try just removing (not aborting); if that fails, abort.
					changed = removeFromExistingCountdownIfStillOkay(sender, gameMode) ;
					if ( !changed ) {
						changed = abortCountdownsInvolvingPlayerAndGameMode(sender, gameMode) ;
					}
					
					// Step two: revise countdowns to account for the new space.  First
					// try to merge existing ones, then expand them, and finally start new
					// ones.  Note that we do all three, in this order, regardless of whether
					// earlier steps succeed or fail.
					if ( changed ) {
						mergeCountdownsIfPossible(gameMode) ;
						expandExistingCountdownIfStillOkay(gameMode) ;
						startLaunchCountdownIfAppropriate(gameMode, 0) ;
					}
				}
				break ;
				
			case LobbyMessage.TYPE_ACTIVE:
				// this method does everything we need
				this.updateForActivePlayer(sender) ;
				break ;
				
				
			case LobbyMessage.TYPE_INACTIVE:
				// this method does everything we need
				this.updateForInactivePlayer(sender) ;
				break ;
				
			case LobbyMessage.TYPE_IN_GAME:
				// this method does everything we need
				this.updateForInGamePlayer(sender) ;
				break ;
			
			case LobbyMessage.TYPE_PLAYER_STATUSES_REQUEST:
				outgoingMessage.setAsPlayerStatuses(lobby.getPlayerStatuses()) ;
				hasOutgoing = true ;
				break ;
				
			case LobbyMessage.TYPE_REWELCOME_REQUEST:
				this.sendWelcomeMessages(sender, false) ;	// do NOT tell everyone, just this guy.
				break ;
				
			case LobbyMessage.TYPE_HOST_PRIORITY:
				// echo back a maximum priority.
				outgoingMessage.setAsHostPriority(Integer.MAX_VALUE) ;
				hasOutgoing = true ;
				break ;
				
			case LobbyMessage.TYPE_PREFERRED_COLOR:
				// this user wants to be a color.
				playerColor[sender] = m.getColor() ;
				// broadcast this.
				outgoingMessage.setAsPreferredColor(sender, playerColor[sender]) ;
				broadcastOutgoing = true ;
				break ;
				
			default:
				// Can't handle other message types.
				this.kickPlayer(sender, "Invalid message type " + m.getType()) ;
				return false ;
			}
			
			if ( broadcastOutgoing )
				mpLayer.sendTo( outgoingMessage, lobby.getPlayersInLobby() ) ;
			if ( hasOutgoing )
				mpLayer.connection(sender).sendMessage(outgoingMessage) ;
			
			return true ;
		}
		
		private boolean messageContentOK( LobbyMessage m ) {
			switch ( m.getType() ) {
			// Some are easy.
			case Message.TYPE_EXIT:
				return true ;
				
			case Message.TYPE_MY_NAME:
				return m.getName() != null ;
				
			case Message.TYPE_NAME_REQUEST:
				int index = m.getPlayerSlot() ;
				return index >= 0 && index < maxPeople ;
				
			case Message.TYPE_NONCE_REQUEST:
			case LobbyMessage.TYPE_GAME_MODE_LIST_REQUEST:
				return true ;
				
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_TOKEN:
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_REVOKE:
				return true ;
				
			case LobbyMessage.TYPE_GAME_MODE_XML_REQUEST:	
			case LobbyMessage.TYPE_GAME_MODE_VOTE_REQUEST:
				return m.getGameMode() >= 0 ;
				
			case LobbyMessage.TYPE_TEXT_MESSAGE:
				return m.getText() != null ;
				
			case LobbyMessage.TYPE_VOTE:
			case LobbyMessage.TYPE_UNVOTE:
				return m.getGameMode() >= 0 ;
				
			case LobbyMessage.TYPE_ACTIVE:
			case LobbyMessage.TYPE_INACTIVE:
			case LobbyMessage.TYPE_IN_GAME:
			case LobbyMessage.TYPE_PLAYER_STATUSES_REQUEST:
				return true ;
				
			case LobbyMessage.TYPE_REWELCOME_REQUEST:
				return true ;
				
			case LobbyMessage.TYPE_HOST_PRIORITY:
				return true ;
				
			case LobbyMessage.TYPE_PREFERRED_COLOR:
				return true ;
				
			default:
				// Can't handle other message types.
				return false ;
			}
		}
		
		
		/**
		 * Attempts to add the specified player to an existing countdown for the specified game mode.
		 * 
		 * Returns whether this occurs.
		 * 
		 * Will fail (returning 'false') if:
		 * 
		 * 1. the player is already in a countdown
		 * 2. there is no countdown for the specified game mode
		 * 3. there is one or more countdown(s) for the game mode, but they are all "full."
		 * 
		 * @param playerSlot
		 * @param gameMode
		 * @return
		 */
		private boolean addToExistingCountdownIfStillOkay( int playerSlot, int gameMode ) {
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			int [] countdown_status = new int[lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, countdown_status, tags) ;
			
			// If the player is already in a countdown, then FAIL (return false).
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_playerIncluded[c][playerSlot] ) {
					return false ;
				}
			}
			
			// Look for a countdown for this game mode with space for this player.
			// TODO: change this to prioritize an active countdown, or one
			// that matches player status?
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_gameMode[c] == gameMode ) {
					// check for space.
					int numPlayers = 0 ;
					for ( int p = 0; p < lobby.getMaxPeople(); p++ ) {
						if ( countdown_playerIncluded[c][p] ) {
							numPlayers++ ;
						}
					}
					
					if ( numPlayers < GameModes.maxPlayers(gameMode) ) {
						// space here!
						int status = countdown_status[c] ;
						boolean playerActive = lobby.getPlayerStatus(playerSlot) == Lobby.PLAYER_STATUS_ACTIVE ;
						boolean countdownActive = playerActive && status == Lobby.COUNTDOWN_STATUS_ACTIVE ;
						
						countdown_playerIncluded[c][playerSlot] = true ;
						
						setLaunchCountdownInLobbyAndBroadcast(
								countdown_number[c], 
								gameMode,
								countdown_playerIncluded[c],
								0, countdownActive ) ;
						
						return true ;
					}
				}
			}
			
			// whelp....
			return false ;
		}
		
		/**
		 * Attempts to add otherwise unengaged players to an existing countdown for the specified game mode.
		 * 
		 * Returns whether this occurs.
		 * 
		 * Will fail (returning 'false') if:
		 * 
		 * 1. there are no "unengaged" players.
		 * 2. there is no countdown for the specified game mode
		 * 3. there is one or more countdown(s) for the game mode, but they are all "full."
		 * 4. there is one or more countdown(s) for the game mode, but no unengaged players who voted for them.
		 * 
		 * @param playerSlot
		 * @param gameMode
		 * @return
		 */
		private boolean expandExistingCountdownIfStillOkay( int gameMode ) {
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			int [] countdown_status = new int[lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			boolean didExpand = false ;
				
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, countdown_status, tags) ;
			
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_gameMode[c] == gameMode ) {
					boolean didExpandThis = false ;
					
					// first: find a countdown with lower than max.
					int roomHere = GameModes.maxPlayers(countdown_gameMode[c]) - ArrayOps.count(countdown_playerIncluded[c]) ;
					int status = countdown_status[c] ;
					if ( roomHere > 0 ) {
						// see if we can find votees ready to move to this countdown.
						for ( int p = 0; p < lobby.getMaxPeople() && roomHere > 0; p++ ) {
							if ( !lobby.getPlayerIsInCountdown(p) && lobby.getPlayerVote(p, gameMode) ) {
								didExpandThis = didExpand = true ;
								countdown_playerIncluded[c][p] = true ;
								roomHere-- ;
								
								if ( lobby.getPlayerStatus(p) != Lobby.PLAYER_STATUS_ACTIVE ) {
									status = Lobby.COUNTDOWN_STATUS_HALTED ;
								}
							}
						}
					}
					
					// did we place anyone?
					if ( didExpandThis ) {
						setLaunchCountdownInLobbyAndBroadcast(
								countdown_number[c], 
								gameMode,
								countdown_playerIncluded[c],
								0, status == Lobby.COUNTDOWN_STATUS_ACTIVE ) ;
					}
				}
			}
			
			// whelp....
			return didExpand ;
		}
		
		
		/**
		 * Attempts to add otherwise unengaged players to any existing countdowns.
		 * 
		 * Returns whether this occurs.
		 * 
		 * Will fail (returning 'false') if:
		 * 
		 * 1. there are no "unengaged" players.
		 * 2. there are no countdowns
		 * 3. there is one or more countdown(s) for the game mode, but they are all "full."
		 * 4. there is one or more countdown(s) for the game mode, but no unengaged players who voted for them.
		 * 
		 * @param playerSlot
		 * @param gameMode
		 * @return
		 */
		private boolean expandExistingCountdownsIfStillOkay() {
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			int [] countdown_status = new int[lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			boolean didExpand = false ;
				
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, countdown_status, tags) ;
			
			for ( int c = 0; c < numCountdowns; c++ ) {
				boolean didExpandThis = false ;
				int gameMode = countdown_gameMode[c] ;
				
				// first: find a countdown with lower than max.
				int roomHere = GameModes.maxPlayers(countdown_gameMode[c]) - ArrayOps.count(countdown_playerIncluded[c]) ;
				int status = countdown_status[c] ;
				if ( roomHere > 0 ) {
					// see if we can find votees ready to move to this countdown.
					for ( int p = 0; p < lobby.getMaxPeople() && roomHere > 0; p++ ) {
						if ( !lobby.getPlayerIsInCountdown(p) && lobby.getPlayerVote(p, gameMode) ) {
							didExpandThis = didExpand = true ;
							countdown_playerIncluded[c][p] = true ;
							roomHere-- ;
							
							if ( lobby.getPlayerStatus(p) != Lobby.PLAYER_STATUS_ACTIVE ) {
								status = Lobby.COUNTDOWN_STATUS_HALTED ;
							}
						}
					}
				}
				
				// did we place anyone?
				if ( didExpandThis ) {
					setLaunchCountdownInLobbyAndBroadcast(
							countdown_number[c], 
							gameMode,
							countdown_playerIncluded[c],
							0, status == Lobby.COUNTDOWN_STATUS_ACTIVE ) ;
				}
			}
			
			// whelp....
			return didExpand ;
		}
		
		
		/**
		 * Attempts to remove the specified player from an existing countdown for the specified game mode.
		 * 
		 * Returns whether this occurs.
		 * 
		 * Will fail (returning 'false') if:
		 * 
		 * 1. the player is not in such a countdown
		 * 2. the player is in such a countdown, but remove that player would drop it below the
		 * 		required number of players
		 * 
		 * @param playerSlot
		 * @param gameMode
		 * @return
		 */
		private boolean removeFromExistingCountdownIfStillOkay( int playerSlot, int gameMode ) {
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			int [] countdown_status = new int[lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, countdown_status, tags) ;
			
			boolean didRemove = false ;
			
			// Look for a countdown for this game mode with this player.
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_gameMode[c] == gameMode ) {
					// check for player
					if ( countdown_playerIncluded[c][playerSlot] ) {
						// check that this countdown would still be okay without the player.
						int numPlayers = 0 ;
						for ( int p = 0; p < lobby.getMaxPeople(); p++ ) {
							if ( countdown_playerIncluded[c][p] ) {
								numPlayers++ ;
							}
						}
						
						// Room for one less?
						if ( GameModes.minPlayers(gameMode) < numPlayers ) {
							// remove this player (we'll use this value later)
							countdown_playerIncluded[c][playerSlot] = false ;
							
							int status = countdown_status[c] ;
							boolean playerActive = lobby.getPlayerStatus(playerSlot) == Lobby.PLAYER_STATUS_ACTIVE ;
							if ( status != Lobby.COUNTDOWN_STATUS_ACTIVE && !playerActive ) {
								// it might have been this player holding up the countdown.
								// Check if it should be active.  The player's position in the
								// countdown array has already been set to false.
								status = Lobby.COUNTDOWN_STATUS_ACTIVE ;
								for ( int p = 0; p < lobby.getMaxPeople(); p++ ) {
									if ( countdown_playerIncluded[c][p] ) {
										if ( lobby.getPlayerStatus(p) != Lobby.PLAYER_STATUS_ACTIVE ) {
											status = Lobby.COUNTDOWN_STATUS_HALTED ;
										}
									}
								}
							}
							
							// okay, refresh the countdown.
							setLaunchCountdownInLobbyAndBroadcast(
									countdown_number[c], 
									gameMode,
									countdown_playerIncluded[c],
									0, status == Lobby.COUNTDOWN_STATUS_ACTIVE ) ;
							
							didRemove = true ;
						}
					}
				}
			}
			
			// whelp....
			return didRemove ;
		}
		
		
		/**
		 * Attempts to remove the specified player from an existing countdown for the specified game mode.
		 * 
		 * Returns whether this occurs.
		 * 
		 * Will fail (returning 'false') if:
		 * 
		 * 1. the player is not in such a countdown
		 * 2. the player is in such a countdown, but remove that player would drop it below the
		 * 		required number of players
		 * 
		 * @param playerSlot
		 * @param gameMode
		 * @return
		 */
		private boolean removeFromExistingCountdownIfStillOkay( int playerSlot ) {
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			int [] countdown_status = new int[lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, countdown_status, tags) ;
			
			boolean didRemove = false ;
			
			// Look for a countdown for this game mode with this player.
			for ( int c = 0; c < numCountdowns; c++ ) {
				// check for player
				if ( countdown_playerIncluded[c][playerSlot] ) {
					// check that this countdown would still be okay without the player.
					int numPlayers = 0 ;
					for ( int p = 0; p < lobby.getMaxPeople(); p++ ) {
						if ( countdown_playerIncluded[c][p] ) {
							numPlayers++ ;
						}
					}
					
					// Room for one less?
					if ( GameModes.minPlayers(countdown_gameMode[c]) < numPlayers ) {
						// remove this player (we'll use this value later)
						countdown_playerIncluded[c][playerSlot] = false ;
						
						int status = countdown_status[c] ;
						boolean playerActive = lobby.getPlayerStatus(playerSlot) == Lobby.PLAYER_STATUS_ACTIVE ;
						if ( status != Lobby.COUNTDOWN_STATUS_ACTIVE && !playerActive ) {
							// it might have been this player holding up the countdown.
							// Check if it should be active.  The player's position in the
							// countdown array has already been set to false.
							status = Lobby.COUNTDOWN_STATUS_ACTIVE ;
							for ( int p = 0; p < lobby.getMaxPeople(); p++ ) {
								if ( countdown_playerIncluded[c][p] ) {
									if ( lobby.getPlayerStatus(p) != Lobby.PLAYER_STATUS_ACTIVE ) {
										status = Lobby.COUNTDOWN_STATUS_HALTED ;
									}
								}
							}
						}
						
						// okay, refresh the countdown.
						setLaunchCountdownInLobbyAndBroadcast(
								countdown_number[c], 
								countdown_gameMode[c],
								countdown_playerIncluded[c],
								0, status == Lobby.COUNTDOWN_STATUS_ACTIVE ) ;
						
						didRemove = true ;
					}
				}
			}
			
			// whelp....
			return didRemove ;
		}
		
		
		/**
		 * Finds countdowns of the specified game mode that could benefit from being merged
		 * (i.e., their combined player counts still fit within the maximum number of players).
		 * 
		 * If any exist, they are merged together into a single countdown, aborting one and
		 * resetting the countdown of the other.
		 * 
		 * Returns whether this occurred.  If 'false,' no countdowns were merged
		 * (and this method call had no effect).
		 * 
		 * @param gameMode
		 * @return
		 */
		private boolean mergeCountdownsIfPossible( int gameMode ) {
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			int [] countdown_status = new int[lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			boolean didMerge = false ;
			boolean loop = true ;
			while( loop ) {
				loop = false ;
				
				int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, countdown_status, tags) ;
				
				// Look for a countdown for this game mode with this player.
				// As soon as a merge occurs (and 'loop' is set to true), we
				// exit out of this, because our data is no longer accurate.
				for ( int c = 0; c < numCountdowns && !loop; c++ ) {
					if ( countdown_gameMode[c] == gameMode ) {
						int num = ArrayOps.count(countdown_playerIncluded[c]) ;
						if ( num < GameModes.maxPlayers(gameMode)) {
							// this is a candidate for merging: size is lower
							// than the maximum.
							
							int candidate = -1 ;
							for ( int c2 = c+1; c2 < numCountdowns; c2++ ) {
								// check if a merge is possible.
								if ( countdown_gameMode[c2] == gameMode ) {
									int num2 = ArrayOps.count(countdown_playerIncluded[c2]) ;
									if ( num + num2 <= GameModes.maxPlayers(gameMode) ) {
										// HUZZAH!  Merge this!
										candidate = c2 ;
										break ;
									}
								}
							}
							
							if ( candidate != -1 ) {
								// do the merge.  Transfer players FROM candidate INTO c.
								int status = Lobby.COUNTDOWN_STATUS_ACTIVE ;
								for ( int p = 0; p < lobby.getMaxPeople(); p++ ) {
									countdown_playerIncluded[c][p] =
										countdown_playerIncluded[c][p] || countdown_playerIncluded[candidate][p] ;
									if ( lobby.getPlayerStatus(p) != Lobby.PLAYER_STATUS_ACTIVE )
										status = Lobby.COUNTDOWN_STATUS_HALTED ;
 ;								}
								
								// abort the other, start the new.
								setLaunchAbortedInLobbyAndBroadcast( 
										countdown_number[candidate],
										gameMode, countdown_playerIncluded[candidate], false ) ;
								
								setLaunchCountdownInLobbyAndBroadcast(
										countdown_number[c], 
										countdown_gameMode[c],
										countdown_playerIncluded[c],
										0, status == Lobby.COUNTDOWN_STATUS_ACTIVE ) ;
								
								didMerge = loop = true ;
							}
						}
					}
				}
			}
			
			// whelp....
			return didMerge ;
		}
		
		
		/**
		 * Finds countdowns of the specified game mode that could benefit from being merged
		 * (i.e., their combined player counts still fit within the maximum number of players).
		 * 
		 * If any exist, they are merged together into a single countdown, aborting one and
		 * resetting the countdown of the other.
		 * 
		 * Returns whether this occurred.  If 'false,' no countdowns were merged
		 * (and this method call had no effect).
		 * 
		 * @param gameMode
		 * @return
		 */
		private boolean mergeCountdownsIfPossible() {
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			int [] countdown_status = new int[lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			boolean didMerge = false ;
			boolean loop = true ;
			while( loop ) {
				loop = false ;
				
				int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, countdown_status, tags) ;
				
				// Look for a countdown for this game mode with this player.
				// As soon as a merge occurs (and 'loop' is set to true), we
				// exit out of this, because our data is no longer accurate.
				for ( int c = 0; c < numCountdowns && !loop; c++ ) {
					int num = ArrayOps.count(countdown_playerIncluded[c]) ;
					int gameMode = countdown_gameMode[c] ;
					if ( num < GameModes.maxPlayers(c)) {
						// this is a candidate for merging: size is lower
						// than the maximum.
						
						int candidate = -1 ;
						for ( int c2 = c+1; c2 < numCountdowns; c2++ ) {
							// check if a merge is possible.
							if ( countdown_gameMode[c2] == gameMode ) {
								int num2 = ArrayOps.count(countdown_playerIncluded[c2]) ;
								if ( num + num2 <= GameModes.maxPlayers(gameMode) ) {
									// HUZZAH!  Merge this!
									candidate = c2 ;
									break ;
								}
							}
						}
						
						if ( candidate != -1 ) {
							// do the merge.  Transfer players FROM candidate INTO c.
								int status = Lobby.COUNTDOWN_STATUS_ACTIVE ;
								for ( int p = 0; p < lobby.getMaxPeople(); p++ ) {
									countdown_playerIncluded[c][p] =
										countdown_playerIncluded[c][p] || countdown_playerIncluded[candidate][p] ;
									if ( lobby.getPlayerStatus(p) != Lobby.PLAYER_STATUS_ACTIVE )
										status = Lobby.COUNTDOWN_STATUS_HALTED ;
								}
								
								// abort the other, start the new.
							setLaunchAbortedInLobbyAndBroadcast( 
									countdown_number[candidate],
									gameMode, countdown_playerIncluded[candidate], false ) ;
							
							setLaunchCountdownInLobbyAndBroadcast(
									countdown_number[c], 
									countdown_gameMode[c],
									countdown_playerIncluded[c],
									0, status == Lobby.COUNTDOWN_STATUS_ACTIVE ) ;
							
							didMerge = loop = true ;
						}
					}
				}
			}
			
			// whelp....
			return didMerge ;
		}
		
		
		private boolean abortCountdownsInvolvingPlayerAndGameMode( int playerSlot, int gameMode ) {
			boolean didAbort = false ;
			
			// Look for - and abort - any countdowns which relied
			// on this player's presence.
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, null, null ) ;
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_playerIncluded[c][playerSlot] && countdown_gameMode[c] == gameMode ) {
					// send message, then abort the countdown.
					setLaunchAbortedInLobbyAndBroadcast( 
							countdown_number[c],
							countdown_gameMode[c], 
							countdown_playerIncluded[c],
							false ) ;
					didAbort = true ;
				}
			}
			
			return didAbort ;
		}
		
		
		private boolean abortCountdownsInvolvingGameMode( int gameMode ) {
			boolean didAbort = false ;
			
			// look for - and abort -- any countdowns which involve this game mode.
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, null, null ) ;
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_gameMode[c] == gameMode ) {
					// send message, then abort the countdown.
					setLaunchAbortedInLobbyAndBroadcast( 
							countdown_number[c],
							countdown_gameMode[c], 
							countdown_playerIncluded[c],
							false ) ;
					didAbort = true ;
				}
			}
			
			return didAbort ;
		}
		
		
		private void abortCountdownsInvolvingPlayer( int playerSlot ) {
			// Look for - and abort - any countdowns which relied
			// on this player's presence.  Remove any pending launch
			// messages for those countdowns.
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, null, tags) ;
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_playerIncluded[c][playerSlot] ) {
					// send message, then abort the countdown.
					setLaunchAbortedInLobbyAndBroadcast( 
							countdown_number[c],
							countdown_gameMode[c], 
							countdown_playerIncluded[c],
							false ) ;
					handler.removeMessages(ANDROID_MESSAGE_TYPE_GAME_LAUNCH_COUNTDOWN_EXPIRED, tags[c]) ;
				}
			}
		}
		
		
		private void haltCountdownsInvolvingPlayer( int playerSlot ) {
			// Look for - and halt - any countdowns which relied on this
			// player's presence.  Remove any pending launch messages for
			// those countdowns.
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, null, tags) ;
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_playerIncluded[c][playerSlot] ) {
					// send message, then abort the countdown.
					mpLayer.sendTo(
							tempMessage.setAsLaunchHalted(
									countdown_number[c],
									countdown_gameMode[c],
									countdown_playerIncluded[c]),
							lobby.getPlayersInLobby() ) ;
					lobby.setCountdownHalted(countdown_number[c],
											countdown_gameMode[c],
											countdown_playerIncluded[c]) ;
					handler.removeMessages(ANDROID_MESSAGE_TYPE_GAME_LAUNCH_COUNTDOWN_EXPIRED, tags[c]) ;
				}
			}
		}
		
		private void restartCountdownsInvolvingPlayer( int playerSlot ) {
			// Look for and resume any countdowns involving this player
			// which were previously halted, and which have no reason to delay
			// (i.e., all players in them are currently active).
			int [] countdown_number = new int[lobby.getMaxPeople()] ;
			int [] countdown_gameMode = new int[lobby.getMaxPeople()] ;
			boolean [][] countdown_playerIncluded = new boolean[lobby.getMaxPeople()][lobby.getMaxPeople()] ;
			Object [] tags = new Object[lobby.getMaxPeople()] ;
			
			int [] status = lobby.getPlayerStatuses() ;
			
			int numCountdowns = lobby.getCountdowns(countdown_number, countdown_gameMode, null, countdown_playerIncluded, null, tags) ;
			for ( int c = 0; c < numCountdowns; c++ ) {
				if ( countdown_playerIncluded[c][playerSlot] ) {
					// Check whether restarting this countdown is appropriate.  All involved players
					// must be present and active.
					boolean shouldRestart = true ;
					for ( int i = 0; i < countdown_playerIncluded[c].length; i++ ) {
						if ( countdown_playerIncluded[c][i] && status[i] != Lobby.PLAYER_STATUS_ACTIVE )
							shouldRestart = false ;
					}
					
					if ( shouldRestart ) {
						// restart countdown, send message, put a delayed
						// Handler message to let us know when it expires -
						// this method does it all, baby!
						this.setLaunchCountdownInLobbyAndBroadcast(countdown_number[c], countdown_gameMode[c], countdown_playerIncluded[c], 0, true) ;
					}
				}
			}
		}
		
		
		private void updateForDisconnectedPlayer( int playerSlot ) {
			
			Log.d(TAG, "updateForDisconnectedPlayer " + playerSlot) ;
			
			// Empty out the personal nonces and names!
			lobby.setPlayerInLobby(playerSlot, false) ;
			lobby.setName(playerSlot, null) ;
			lobby.setPersonalNonce(playerSlot, Nonce.ZERO) ;
			
			// Adjust countdowns.  First reduce, then abort, any
			// countdowns the player is involved in.  Second, remember
			// (at the end) to merge, expand, and start countdowns again.
			removeFromExistingCountdownIfStillOkay(playerSlot) ;
			abortCountdownsInvolvingPlayer(playerSlot) ;
			
			// Okay, we've stopped the countdowns and removed
			// all trace of this vile individual.
			// We also need to clear their votes.
			lobby.resetPlayerVotes(playerSlot) ;
			
			mpLayer.sendTo(
					tempMessage.setAsPlayersInLobby( lobby.getPlayersInLobby() ),
					lobby.getPlayersInLobby() ) ;
			
			
			// Clear out their authorization tokens.  Revoke any token currently in use,
			// unless we can replace them with another.
			authTokens_byPlayer[playerSlot].clear() ;
			int [] gameModes = lobby.getGameModes() ;
			for ( int i = 0; i < gameModes.length; i++ ) {
				Integer owner = authTokenOwner.get(gameModes[i]) ;
				if ( owner != null && owner.intValue() == playerSlot ) {
					// look for a replacement.
					Integer replacementOwner = null ;
					Serializable replacementToken = null ;
					for ( int p = 0; p < lobby.getMaxPeople() && replacementToken == null ; p++ ) {
						replacementToken = authTokens_byPlayer[playerSlot].get(gameModes[i]) ;
						if ( replacementToken != null )
							replacementOwner = Integer.valueOf(p) ;
					}
					if ( replacementToken == null ) {
						authTokenOwner.remove(gameModes[i]) ;
						mpLayer.sendTo(tempMessage.setAsGameModeAuthorizationRevoke(gameModes[i]),
								lobby.getPlayersInLobby()) ;
						abortCountdownsInvolvingGameMode( gameModes[i] ) ;
					} else {
						authTokenOwner.put(gameModes[i], replacementOwner) ;
						mpLayer.sendTo(tempMessage.setAsGameModeAuthorizationToken(gameModes[i], replacementToken),
								lobby.getPlayersInLobby()) ;
					}
				}
			}
			
			// merge, expand, and start new countdowns.
			mergeCountdownsIfPossible() ;
			expandExistingCountdownsIfStillOkay() ;
			startNewLaunchCountdownsIfAppropriate() ;
		}
		
		
		/**
		 * This method performs all the necessary updates for a player who just
		 * became Active.  It changes the contents of our Lobby, sends the
		 * ActivePlayers message to all connected players, and restarts any
		 * countdowns that were previously halted when this player when inactive.
		 * @param playerSlot
		 */
		private void updateForActivePlayer( int playerSlot ) {
			// When a player goes inactive, we may have halted countdowns
			// for them.  Restart those countdowns, AFTER sending a
			// message about currently active players.
			lobby.setPlayerActive(playerSlot) ;
			mpLayer.sendTo(
					tempMessage.setAsPlayerStatuses( lobby.getPlayerStatuses() ),
					lobby.getPlayersInLobby() ) ;
			restartCountdownsInvolvingPlayer( playerSlot ) ;
			startNewLaunchCountdownsIfAppropriate() ;
		}
		
		private void updateForInactivePlayer( int playerSlot ) {
			// When a player goes inactive, we make certain changes.  for
			// example, we halt any countdowns that player is participating in.
			
			// we send the player status message FIRST, then halt the countdowns
			lobby.setPlayerInactive( playerSlot ) ;
			mpLayer.sendTo(
					tempMessage.setAsPlayerStatuses( lobby.getPlayerStatuses() ),
					lobby.getPlayersInLobby() ) ;
			// halt this player's countdowns.
			haltCountdownsInvolvingPlayer( playerSlot ) ;
		}
		
		private void updateForInGamePlayer( int playerSlot ) {
			// When a player goes inactive, we make certain changes.  for
			// example, we halt any countdowns that player is participating in.
			
			// we send the player status message FIRST, then halt the countdowns
			lobby.setPlayerInactive( playerSlot ) ;
			mpLayer.sendTo(
					tempMessage.setAsPlayerStatuses( lobby.getPlayerStatuses() ),
					lobby.getPlayersInLobby() ) ;
			// halt this player's countdowns.
			haltCountdownsInvolvingPlayer( playerSlot ) ;
		}
		
		
		
		private void kickPlayer( int playerSlot, String text ) {
			// Let everyone know why
			tempMessage.setAsKick(playerSlot, text) ;
			mpLayer.sendTo( tempMessage, lobby.getPlayersInLobby() ) ;
			updateForDisconnectedPlayer( playerSlot ) ;
		}
		
	}
	
	
	
	int ownerSlot ;
	String ownerName ;
	boolean demandNonce ;
	boolean broadcastPersonalNonces ;
	int maxPeople ;
	
	// Our internal data.
	int versionCode ;
	Lobby lobby ;
	int nextCountdownNumber = 0 ;
	int [] playerColor ;
	// Auth tokens!  Each player is allowed to provide authorization
	// tokens for the use of particular game types.  We store auth tokens
	// in a hashtable indexed by GameMode.
	Hashtable<Integer, Serializable> [] authTokens_byPlayer ;
	// The currently used authorization token for each game mode.
	// We represent this by indicating the PLAYER whose Auth token is
	// currently in-use.  If that player leaves, we switch to a different
	// auth-token or revert to none at all, sending either the new
	// token or a revoke message.
	Hashtable<Integer, Integer> authTokenOwner ;
	
	
	// Next game to launch
	Nonce nextGameNonce ;
	int nextGamePort ;
	SocketAddress nextGameSockAddr ;
	SocketAddress nextGameUDPSockAddr ;
	int nextGameHost ;
	boolean nextGameHostIsMatchseeker ;
	Nonce nextGameEditKey ;
	
	
	// Age info
	private long creationTime ;
	private long lastLaunchTime ;
	
	// mamamamaaaah controlla
	WeakReference<Delegate> mwrDelegate ;
	
	// Connection
	MessagePassingLayer mpLayer ;
	
	
	// THREAD!
	LobbyCoordinatorThread thread ;
	

	
	
	public LobbyCoordinator( Delegate delegate, int versionCode, Nonce sessionNonce, Nonce lobbyNonce, String lobbyName, int ownerSlot, String ownerName, boolean broadcastPersonalNonces, int maxPeople ) {
		if ( delegate == null )
			throw new NullPointerException("Null delegate given") ;
		
		this.ownerSlot = ownerSlot ;
		this.ownerName = ownerName ;
		this.broadcastPersonalNonces = broadcastPersonalNonces ;
		this.maxPeople = maxPeople ;
		
		// Holders for players, lobby state, etc.
		this.versionCode = versionCode ;
		lobby = new Lobby() ;
		lobby.setMaxPlayers(maxPeople) ;
		lobby.setLobbyName(lobbyName) ;
		lobby.setSessionNonce(sessionNonce) ;
		lobby.setLobbyNonce(lobbyNonce) ;
		playerColor = new int[maxPeople] ;
		
		authTokens_byPlayer = (Hashtable<Integer, Serializable> []) new Hashtable<?, ?>[ maxPeople ] ;
		for ( int i = 0; i < maxPeople; i++ ) {
			authTokens_byPlayer[i] = new Hashtable<Integer, Serializable>() ;
		}
		authTokenOwner = new Hashtable<Integer, Integer>() ;
		
		// Age info
		creationTime = System.currentTimeMillis() ;
		lastLaunchTime = 0 ;
		
		// Layer, Thread
		mpLayer = null ;
		thread = null ;
		
		// Delegate
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
		lobby.setGameModes( delegate.lcd_requestGameModesAvailable(this) ) ;
	}
	
	
	public LobbyCoordinator( Delegate delegate, int versionCode, InternetLobby internetLobby, int ownerSlot, String ownerName ) {
		if ( delegate == null )
			throw new NullPointerException("Null delegate given") ;
		
		this.ownerSlot = ownerSlot ;
		this.ownerName = ownerName ;
		this.broadcastPersonalNonces = false ;
		this.maxPeople = internetLobby.getMaxPeople() ;
		
		// Holders for players, lobby state, etc.
		this.versionCode = versionCode ;
		lobby = internetLobby ;
		playerColor = new int[maxPeople] ;
		
		// Age info
		creationTime = System.currentTimeMillis() ;
		lastLaunchTime = 0 ;
		
		// Layer, Thread
		mpLayer = null ;
		thread = null ;
		
		// Delegate
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
		lobby.setGameModes( delegate.lcd_requestGameModesAvailable(this) ) ;
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
	public synchronized LobbyCoordinator setMessagePassingLayer( MessagePassingLayer layer ) throws IllegalStateException {
		if ( thread != null && thread.isAlive() )
			throw new IllegalStateException("Can't set MPLayer after starting the GameCoordinator!") ;
		
		mpLayer = layer ;
		mpLayer.setDelegate(this) ;
		
		return this ;
	}
	
	
	
	public void setNextGameNonce( Nonce nonce ) {
		this.nextGameNonce = nonce ;
	}
	
	public void setNextGamePort( int port ) {
		this.nextGamePort = port ;
	}
	
	public void setNextGameSocketAddress( SocketAddress sockAddr ) {
		this.nextGameSockAddr = sockAddr ;
	}
	
	public void setNextGameUDPSocketAddress( SocketAddress udpSockAddr ) {
		this.nextGameUDPSockAddr = udpSockAddr ;
	}
	
	public void setNextGameHost( int slot ) {
		this.nextGameHost = slot ;
	}
	
	public void setNextGameHostIsMatchseeker( boolean matchseeker ) {
		this.nextGameHostIsMatchseeker = matchseeker ;
	}
	
	public void setNextGameEditKey( Nonce editKey ) {
		this.nextGameEditKey = editKey ;
	}
	
	public void setOwnerName( String ownerName ) {
		this.ownerName = ownerName ;
	}
	
	
	public synchronized void start() throws IllegalStateException {
		if ( thread != null && thread.isAlive() )
			throw new IllegalStateException("Can't start if currently running!") ;
		if ( mpLayer == null )
			throw new IllegalStateException("Can't start without a MessagePassingLayer!" ) ;
		
		thread = new LobbyCoordinatorThread() ;
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
	}
	
	
	public synchronized boolean isStarted() {
		return thread != null && thread.running ;
	}
	
	public synchronized void stop() throws IllegalStateException {
		if ( thread == null ) {
			Log.w(TAG, "Stopping when never started!") ;
			return ;
		}
		
		if ( thread.running ) {
			while ( thread.handler == null ) {
				try {
					Thread.sleep(10) ;
				} catch( InterruptedException e ) { }
			}
			
			// TODO: Remove all other messages from the Handler.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP) ) ;
		}
	}
	
	
	public synchronized void stopAndProvideOpenConnections() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't stop if never started!") ;
		
		if ( thread.running ) {
			while ( thread.handler == null ) {
				try {
					Thread.sleep(10) ;
				} catch( InterruptedException e ) { }
			}
			
			// TODO: Remove all other messages from the Handler.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTIONS) ) ;
		}
	}
	
	public synchronized void join() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't join if never started!") ;
		
		ThreadSafety.waitForThreadToTerminate(thread, 1000) ;
	}
	
	
	public long age() {
		return System.currentTimeMillis() - creationTime ;
	}
	
	
	public long timeSinceLastLaunch() {
		if ( lastLaunchTime == 0 )
			return age() ;
		return System.currentTimeMillis() - lastLaunchTime ;
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
		// Log.v(TAG,"mpld_messagePassingConnectionDidReceiveMessage: " + connNum) ;
		
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
		// Log.v(TAG,"mpld_messagePassingConnectionDoneReceivingMessages: " + connNum) ;
		
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
		// Log.v(TAG,"mpld_messagePassingConnectionDidConnect: " + connNum) ;
		
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
		// Log.v(TAG,"mpld_messagePassingConnectionDidFailToConnect: " + connNum) ;
		
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
		// Log.v(TAG,"mpld_messagePassingConnectionDidBreak: " + connNum) ;
		
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
		// Log.v(TAG,"mpld_messagePassingConnectionDidDisconnectByPeer: " + connNum) ;
		
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
