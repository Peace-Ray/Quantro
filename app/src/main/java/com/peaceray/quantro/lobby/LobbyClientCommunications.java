package com.peaceray.quantro.lobby;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.Hashtable;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.communications.LobbyMessage;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.ThreadSafety;

/**
 * Part of our refactoring of Lobbies.  The LobbyClientCommunications
 * uses a Lobby object, making direct changes to it (rather than working
 * through a Controller as in the original design).  That Lobby object
 * makes Delegate calls when its contents change; this LobbyClientCommunications
 * object also makes Delegate calls, when connection status changes, and
 * when especially relevant messages come in (such as Server Closing,
 * or Game Launch).
 * 
 * @author Jake
 *
 */
public class LobbyClientCommunications implements MessagePassingConnection.Delegate {
	
	public static final String TAG = "LCCommunications" ;
	
	
	/**
	 * The LobbyClientCommunications object doesn't know how to handle most
	 * message types or connections updates, so it passes responsibility to
	 * a delegate which is set from outside.  Delegates must implement this
	 * interface.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {

		/*
		 * ************************************************************************
		 * 
		 * CONNECTION UPDATES
		 * 
		 * Once the LobbyClientCommunications starts, we attempt to Connect to the 
		 * server.  These methods inform the delegate of our connection status
		 * changes, and request guidance as to our next move.
		 * 
		 * *************************************************************************
		 */
		
		
		/**
		 * Notifies the delegate that our connection status has updated,
		 * and provides the new status.  This call is made every time we
		 * notice a connection status change.
		 */
		public void lccd_connectionStatusChanged( MessagePassingConnection.Status connectionStatus ) ;
		
		
		/**
		 * We received garbage data which, for whatever reason, we could not
		 * process.  We will disconnect and attempt to reconnect; we just wanted
		 * to update you on what happened.
		 */
		public void lccd_connectionReceivedIllegalMessage() ;
		
		
		
		
		/*
		 * ************************************************************************
		 * 
		 * MESSAGES
		 * 
		 * Messages which can be handled entirely with an update to the Lobby object
		 * will be handled by such an update (the Lobby itself will notify its own
		 * delegate of the change), and LobbyClientCommunications will not tell its
		 * own delegate.  These methods apply in cases where additional action is required.
		 * 
		 * *************************************************************************
		 */
		
		
		/**
		 * We were welcomed to the server.  Awwww!  Thanks man!
		 * 
		 * This message will be received after a successful connection
		 * and negotiation.  In other words, when we transition to CONNECTED
		 * status, we should be considered "negotiating" until we get this
		 * message.
		 */
		public void lccd_messageWelcomeToServer() ;
		
		
		/**
		 * The server wants us to know about the host.  If slot is >= 0,
		 * the host is a participant in the lobby (and has a permanent slot).
		 * @param name
		 */
		public void lccd_messageHost( int slot, String name ) ;
		
		
		/**
		 * The server told us the total number of player slots for this game.
		 * @param slots
		 */
		public void lccd_messageTotalPlayerSlots( int slots ) ;
		
		
		/**
		 * The server told us which player slot is OUR player slot.
		 * @param slot
		 */
		public void lccd_messageLocalPlayerSlot( int slot ) ;
		
		
		/**
		 * The server provided an authorization token for the specified game mode.
		 * 
		 * If a previous token existed, this token should replace it.
		 * 
		 * @param gameMode
		 * @param authToken
		 */
		public void lccd_messageGameModeAuthToken( int gameMode, Serializable authToken ) ;
		
		
		/**
		 * The server has revoked the most recent game authorization token.
		 * 
		 * If this game mode is premium content, or otherwise requires an auth
		 * token to be used, it should be disabled.
		 * 
		 * @param gameMode
		 */
		public void lccd_messageGameModeRevokeAuthToken( int gameMode ) ;
		
		
		/**
		 * The server provided an XML file for a particular game mode.
		 * @param gameMode
		 * @param xml
		 */
		public void lccd_messageGameModeXML( int gameMode, String xml ) ;
		
		
		/**
		 * A game is launching, but we aren't participating.
		 * 
		 * @param gameMode
		 * @param playersInLaunch
		 */
		public void lccd_messageGameModeLaunchAsAbsent( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers ) ;
		
		
		/**
		 * A game is launching, and we participate.
		 * 
		 * @param gameMode
		 * @param playersInLaunch
		 */
		public void lccd_messageGameModeLaunchAsDirectClient( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
				Nonce nonce, SocketAddress socketAddress ) ;
		
		
		/**
		 * A game is launching, and we participate.
		 * 
		 * @param gameMode
		 * @param playersInLaunch
		 */
		public void lccd_messageGameModeLaunchAsDirectHost( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
				Nonce nonce, SocketAddress socketAddress ) ;
		
		
		/**
		 * A game is launching, and we participate.
		 * 
		 * @param gameMode
		 * @param playersInLaunch
		 */
		public void lccd_messageGameModeLaunchAsMatchseekerClient( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
				Nonce nonce, SocketAddress socketAddress ) ;
		
		
		/**
		 * A game is launching, and we participate.
		 * 
		 * @param gameMode
		 * @param playersInLaunch
		 */
		public void lccd_messageGameModeLaunchAsMatchseekerHost( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
				Nonce nonce, Nonce editKey, SocketAddress socketAddress ) ;
		
		
		/**
		 * The server has kicked a player, and provided a reason.  If
		 * playerSlot is < 0, or is OUR slot number, then WE were kicked.
		 * @param playerSlot
		 * @param msg
		 */
		public void lccd_messageKick( int slot, String msg ) ; 
		
		
		/**
		 * A player has exited the server; this message indicates that
		 * the player left.
		 * 
		 * @param playerSlot
		 */
		public void lccd_messageQuit( int playerSlot ) ;
		
		
		/**
		 * The server is going down FOREVER and nicely told us so.
		 */
		public void lccd_messageServerClosingForever() ;
		
		
		/**
		 * The server wants us to use this color for this person.
		 * @param playerSlot
		 * @param color
		 */
		public void lccd_messagePreferredColor( int playerSlot, int color ) ;
		
		
		
		/*
		 * ************************************************************************
		 * 
		 * INFORMATION REQUESTS
		 * 
		 * While almost all the delegate methods are "updates for delegate" type
		 * messages, with the LobbyClientCommunications continuing on without any problems,
		 * the Communications will occasionally require information from the delegate.
		 * These methods request that information.
		 * 
		 * *************************************************************************
		 */
		
		/**
		 * This method is called before each connection attempt.  We will delay the
		 * specified number of milliseconds before attempting a connection.  It
		 * is probably wise to return 0 the first time this is called, but
		 * to implement some kind of delay like exponential back-off after
		 * repeated calls.
		 * 
		 * BTW, the order of calls will look something like:
		 * 
		 * lccd_connectionStatusChanged( BROKEN(or something else) )
		 * lccd_connectionStatusChanged( DISCONNECTED )
		 * lccd_requestDelayUntilConnect()
		 * lccd_connectionStatusChanged( PENDING(or connected, or...) )
		 * 
		 * @return The number of seconds before our next reconnection.
		 */
		public long lccd_requestDelayUntilConnect() ;
		
		
		/**
		 * A request from the Delegate of a host priority number.  This number should
		 * 1. be partially based on circumstances, e.g., if we have previously become
		 * a client to for some other host, maybe we should have a low priority now?,
		 * and 2. be partially randomized, to prevent .
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
		public int lccd_requestHostPriority() ;
		
		
		
		
		/*
		 * ************************************************************************
		 * 
		 * CLIENT/HOST TRANSITION
		 * 
		 * Based on messages received from the other party, and the host priority set
		 * by the delegate, we should transition from client to host.
		 * 
		 * *************************************************************************
		 */
		
		/**
		 * Tells the Delegate that we believe we should become the host, rather than
		 * act as the client.  Our currently connected "host" should be a client instead.
		 */
		public void lccd_shouldBecomeHost() ;
		
		
		/**
		 * Tells the delegate we have stopped operation with an open connection, as requested.
		 * This method will only be called if stopAndProvideOpenConnection() is called to stop
		 * the LCC.
		 * @param conn
		 */
		public void lccd_stoppedWithOpenConnection( MessagePassingConnection conn ) ;

	}


	// Orders from above!  Connect or disconnect our connections.
	private static final int ANDROID_MESSAGE_TYPE_CONNECT 							= 0 ;
	private static final int ANDROID_MESSAGE_TYPE_STOP 								= 1 ;
	private static final int ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTION 	= 2 ;
	
	// Forward-looking message.  When we connect to the Mediator, we may not want
	// to remain a client forever.  This delayed message is started when a Client
	// connects to the Mediator (mediated games only!).  If it is not canceled 
	// before it fires, it will perform the same actions as StopAndProvideOpenConnection.
	private static final int ANDROID_MESSAGE_TYPE_BECOME_HOST_IF_NO_CONNECTION		= 3 ;
	
	// The MessagePassingConnection status updated.
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED 			= 4 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_FAILED				= 5 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_BROKE				= 6 ;
	private static final int ANDROID_MESSAGE_TYPE_CONNECTION_PEER_DISCONNECTED	= 7 ;
	
	// The MessagePassing layer has a message for us!
	private static final int ANDROID_MESSAGE_TYPE_MESSAGE_RECEIVED 				= 8 ;
	private static final int ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES 		= 9 ;
	
	// Incoming user actions.  We send if everything is OK and we have been welcomed;
	// otherwise we drop them on the floor.
	private static final int ANDROID_MESSAGE_TYPE_SEND_REQUEST_GAME_XML			= 10 ;
	private static final int ANDROID_MESSAGE_TYPE_SEND_VOTE_FOR					= 11 ;
	private static final int ANDROID_MESSAGE_TYPE_SEND_UNVOTE_FOR				= 12 ;
	private static final int ANDROID_MESSAGE_TYPE_SEND_ACTIVE					= 13 ;
	private static final int ANDROID_MESSAGE_TYPE_SEND_INACTIVE					= 14 ;
	private static final int ANDROID_MESSAGE_TYPE_SEND_IN_GAME					= 15 ;
	private static final int ANDROID_MESSAGE_TYPE_SEND_TEXT_MESSAGE				= 16 ;
	// Self-passed message for a broken or peer-disconnected connection, just in case
	// DONE_RECEIVING_MESSAGES is never received.  We can't rely on that assumption for some reason.
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION	 	= 17 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION	= 18 ;
	private static final int ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION	= 19 ;
	// how many?
	private static final int NUM_ANDROID_MESSAGE_TYPES							= 20 ;
	
	
	private static final long SELF_LAUNCH_WAIT_PERIOD = 200 ;
		// Previously we waited for an explicit launch message from the server
		// before launching a game.  To resolve an intermittent bug involving
		// the server launching and leaving the client behind, we now allow clients
		// to launch even if they haven't been told.  When a countdown expires, 
		// we wait this many milliseconds for a server launch message and, if
		// none arrives, launch ourselves.
	private static final long MAX_TIME_BETWEEN_DISCONNECT_AND_LAUNCH = 300 ;
		// on very rare occasions, the server will start a game launch
		// and move to the game so fast that it severs its connection with
		// the client before the client receives launch instructions.  As part
		// of an effort to rectify this, we allow the client to automatically
		// launch from a countdown if at most this many milliseconds have passed
		// between losing the server connection and the launch countdown 
		// ending.
		// NOTE: this measures the time between "disconnect" and "countdown expire."
		// If we perform a self-launch after SELF_LAUNCH_WAIT_PERIOD, then obviously
		// the maximum time between it and the last disconnect is the sum of 
		// the two values.
	private static final long DELAYED_HANDLE_DELAY = 5000 ;	// 5 seconds
	
	
	class LobbyClientCommunicationsThread extends Thread {
		
		private static final String TAG = "LCCommunicationsThread" ;
		
		public static final long millisecondsBetweenUpdates = 100 ;
		
		public boolean running ;
		public boolean welcomed ;
		public boolean acceptedAsClient ;
		
		public long timeLastDisconnect = 0 ;
		
		int lastPrioritySent ;
		
		boolean [] booleanPlayerArray = null ;
		int [] intPlayerArray = null ;
		Handler handler ;
		
		LobbyMessage outgoingMessage = new LobbyMessage() ;
		
		public LobbyClientCommunicationsThread() {
			running = true ;
			welcomed = false ;
		}
		
		
		@Override
		public void run() {
			
			Log.v(TAG,"Lobby thread starting!") ;
			
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
	        		
            		handler.removeMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION) ;
            		handler.removeMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION) ;
            		handler.removeMessages(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION) ;
	        	}
	        	

	            public void handleMessage(android.os.Message msg) {
	            
	            	int type = msg.what ;
	            	int arg1 = msg.arg1 ;
	            	//int arg2 = msg.arg2 ;
	            	Object obj = msg.obj ;
	            	LobbyMessage lm ;
	            	android.os.Message selfMessage ;
	            	
	            	Delegate delegate = mwrDelegate.get() ;
	            	if ( delegate == null && type != ANDROID_MESSAGE_TYPE_STOP ) {
	            		handler.sendMessage( handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP) ) ;
	            		return ;
	            	}
	            	
	            	switch( type ) {
	            	case ANDROID_MESSAGE_TYPE_CONNECT:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_CONNECT") ;
	            		// If currently connected, send an I AM CLIENT message.
	            		// (probably this Connection has been passed in in a Connected state
	            		welcomed = false ;
	            		acceptedAsClient = false ;
	            		if ( connection.isConnected() ) {
	            			connection.sendMessage( outgoingMessage.setAsIAmClient() ) ;
	            			delegate.lccd_connectionStatusChanged(MessagePassingConnection.Status.CONNECTED) ;
	            		}
	            		else {
	            			removeAllMessagesRegardingConnection() ;
	            			if ( connection.isAbleToDisconnect() ) {
	            				//Log.d(TAG, "ANDROID_MESSAGE_TYPE_CONNECT: disconnecting preemptively") ;
	            				connection.disconnect() ;
	            				if ( welcomed ) {
	            					lobbyLog.logDisconnected(playerSlot, name, lobby.getPlayerStatus(lobby.playerSlot), null) ;
	            					welcomed = false ;
	            				}
	            				delegate.lccd_connectionStatusChanged(MessagePassingConnection.Status.DISCONNECTED) ;
	            			}
	            			//Log.v(TAG, "connecting!") ;
	            			connection.connect() ;
	            			//Log.v(TAG, "new status is " + connection.connectionStatus()) ;
	            			delegate.lccd_connectionStatusChanged( MessagePassingConnection.Status.PENDING ) ;
	            		}
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_STOP:
	            		Log.v(TAG, "ANDROID_MESSAGE_TYPE_STOP") ;
	            		// Shut.  Down.  Everything.
	            		if ( connection.isConnected() ) {
	            			Log.d(TAG, "sending EXIT message") ;
	            			connection.sendMessage( outgoingMessage.setAsExit() ) ;
	            		}
	            		if ( connection.isAbleToDisconnect() ) {
	            			//Log.d(TAG, "ANDROID_MESSAGE_TYPE_STOP: disconnecting") ;
	            			connection.disconnect() ;
	            			// We are disconnecting and terminating the looper.  If welcomed,
	            			// we should log a disconnect here, because we won't be able to
	            			// later on - this is the last message we will process.
	            			if ( welcomed )
	            				lobbyLog.logDisconnected(playerSlot, name, lobby.getPlayerStatus(playerSlot), null) ;
	            		}
	            		getLooper().quit();
	            		lobby.resetMembership() ;
	            		running = false ;
            			welcomed = false ;
            			acceptedAsClient = false ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTION:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTION") ;
	            		// Stop looping, but send no data and don't disconnect.
	            		getLooper().quit();
	            		lobby.resetMembership() ;
	            		running = false ;
	            		connection.setDelegate(null) ;
	            		delegate.lccd_stoppedWithOpenConnection(connection) ;
	            		break ;
	            		
	            		
	            	case ANDROID_MESSAGE_TYPE_BECOME_HOST_IF_NO_CONNECTION:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_BECOME_HOST_IF_NO_CONNECTION") ;
	            		// If we are not connected, time to become the host.
	            		if ( connection.connectionStatus() == MessagePassingConnection.Status.PENDING )
	            			delegate.lccd_shouldBecomeHost() ;
	            		break ;
	            		
	            		
	            	// The MessagePassing layer status updated.
	            	case ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_CONNECTION_CONNECTED") ;
	            		// We connected!  But we weren't welcomed.  Send an I AM CLIENT message
	            		// and tell the delegate
	            		//Log.v(TAG, "connected!") ;
	            		handler.removeMessages(ANDROID_MESSAGE_TYPE_BECOME_HOST_IF_NO_CONNECTION) ;
	            		delegate.lccd_connectionStatusChanged(MessagePassingConnection.Status.CONNECTED) ;
	            		welcomed = false ;
	            		acceptedAsClient = false ;
	            		//Log.v(TAG, "sending I AM CLIENT") ;
	            		connection.sendMessage( outgoingMessage.setAsIAmClient() ) ;
	            		//Log.v(TAG, "sent?") ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_CONNECTION_FAILED:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_FAILED") ;
	            		// Whoops.  Inform the delegate, but do NOT attempt to reconnect.
	            		// We will soon receive a call to DONE_RECEIVING_MESSAGES.
	            		delegate.lccd_connectionStatusChanged( MessagePassingConnection.Status.FAILED ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_CONNECTION_BROKE:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_BROKE") ;
	            		// Whoops.  Inform the delegate, but do NOT attempt to reconnect.
	            		// We will soon receive a call to DONE_RECEIVING_MESSAGES.
	            		timeLastDisconnect = System.currentTimeMillis() ;
	            		delegate.lccd_connectionStatusChanged( MessagePassingConnection.Status.BROKEN ) ;
	            		selfMessage = handler.obtainMessage(
	            				ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION) ;
	            		handler.sendMessageDelayed(selfMessage, DELAYED_HANDLE_DELAY) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_CONNECTION_PEER_DISCONNECTED:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_DISCONNETED") ;
	            		// Whoops.  Inform the delegate, but do NOT attempt to reconnect.
	            		// We will soon receive a call to DONE_RECEIVING_MESSAGES.
	            		timeLastDisconnect = System.currentTimeMillis() ;
	            		delegate.lccd_connectionStatusChanged( MessagePassingConnection.Status.PEER_DISCONNECTED ) ;
	            		selfMessage = handler.obtainMessage(
	            				ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION) ;
	            		handler.sendMessageDelayed(selfMessage, DELAYED_HANDLE_DELAY) ;
	            		break ;
	            		
	            	// The MessagePassing layer has a message for us!
	            	case ANDROID_MESSAGE_TYPE_MESSAGE_RECEIVED:
	            		//Log.v(TAG, "ANDROID_MESSAGE_TYPE_RECEIVED") ;
	            		// We have either been welcomed, or not.  If we have not been welcomed,
	            		// we expect one of three messages: I am host, I am client, or a host priority.
	            		// Otherwise, we have a dedicated method for handling messages.
	            		if ( connection.connectionStatus() == MessagePassingConnection.Status.DISCONNECTED )
	            			break ;	// do nothing
	            		if ( !connection.moreMessages() || !connection.hasMessage() ) {
	            			// Received a message, but no messages to get?  WTF?  Kill.
	            			// schedule a disconnect
	            			handler.sendMessageDelayed( handler.obtainMessage(ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION),
	            					2000) ;
	            			break ;
	            		}
	            		lm = (LobbyMessage) connection.getMessage() ;
	            		if ( acceptedAsClient ) {
	            			if ( !handleIncomingMessage( lm ) ) {
	            				//Log.d(TAG, "disconnecting due to failure to handle message type " + lm.getType()) ;
		            			// disconnect!
		            			delegate.lccd_connectionReceivedIllegalMessage() ;
								connection.disconnect() ;
								if ( welcomed ) {
									lobbyLog.logDisconnected(playerSlot, name, lobby.getPlayerStatus(playerSlot), null) ;
									welcomed = false ;
								}
								delegate.lccd_connectionStatusChanged(MessagePassingConnection.Status.DISCONNECTED) ;
								// queue up a reconnect
								handler.sendMessageDelayed(
										handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECT),
										delegate.lccd_requestDelayUntilConnect() ) ;
		            		}
	            		}
	            		else {
	            			// Handle a "host negotiation" message.
	            			switch( lm.getType() ) {
	            			case LobbyMessage.TYPE_YOU_ARE_CLIENT:
	            				//Log.v(TAG, "MESSAGE: you are client") ;
	            				// HUZZAH!
	            				acceptedAsClient = true ;
	            				break ;
	            				
	            			case LobbyMessage.TYPE_I_AM_HOST:
	            				//Log.v(TAG, "MESSAGE: I am host") ;
	            				// Yes you are.  We wait for a YOU ARE CLIENT.
	            				break ;
	            				
	            			case LobbyMessage.TYPE_I_AM_CLIENT:
	            				//Log.v(TAG, "MESSAGE: I am client") ;
	            				// D: WHAT DO WEE DOOOO??
	            				lastPrioritySent = delegate.lccd_requestHostPriority() ;
	            				connection.sendMessage( outgoingMessage.setAsHostPriority(lastPrioritySent) ) ;
	            				break ;
	            				
	            			case LobbyMessage.TYPE_HOST_PRIORITY:
	            				//Log.v( TAG, "MESSAGE: host priority " + lm.getPriority() + " compare against my priority " + lastPrioritySent ) ;
	            				// Let's see who becomes the host.
	            				if ( lastPrioritySent == lm.getPriority() ) {
	            					lastPrioritySent = delegate.lccd_requestHostPriority() ;
		            				connection.sendMessage( outgoingMessage.setAsHostPriority(lastPrioritySent) ) ;
	            				}
	            				else if ( lastPrioritySent < lm.getPriority() ) {
	            					// Looks like the other party is becoming the host.  We just
	            					// wait for a "You are client" message to come through.
	            				}
	            				else {
	            					// WE have to become the host!  Tell the delegate and hope for the best.
	            					delegate.lccd_shouldBecomeHost() ;
	            				}
	            				break ;
	            				
	            			default:
	            				// THIS IS UNACCEPTABLE
	            				Log.v(TAG, "MESSAGE: Receive unknown message type " + lm.getType() + " before YOU ARE CLIENT!") ;
	            				delegate.lccd_connectionReceivedIllegalMessage() ;
								connection.disconnect() ;
								if ( welcomed ) {
									lobbyLog.logDisconnected(playerSlot, name, lobby.getPlayerStatus(playerSlot), null) ;
									welcomed = false ;
								}
								delegate.lccd_connectionStatusChanged(MessagePassingConnection.Status.DISCONNECTED) ;
								// queue up a reconnect
								handler.sendMessageDelayed(
										handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECT),
										delegate.lccd_requestDelayUntilConnect() ) ;
								break ;
	            				
	            			}
	            		}
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION:
	            	case ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_NO_MESSAGES_ON_CONNECTION:
	            		//if ( type == ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES )
	            		//	Log.v(TAG, "ANDROID_MESSAGE_TYPE_DONE_RECEIVING_MESSAGES") ;
	            		//else if ( type == ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION )
	            		//	Log.v(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_BROKEN_CONNECTION") ;
	            		//else
	            		//	Log.v(TAG, "ANDROID_MESSAGE_TYPE_DELAYED_HANDLE_PEER_DISCONNECTED_CONNECTION") ;
	            		removeAllMessagesRegardingConnection() ;
	            		// Everything's done.  Disconnect, tell the delegate, and queue up a reconnect.
	            		if ( connection.connectionStatus() != MessagePassingConnection.Status.DISCONNECTED ) {
	            			// Cut the connection; queue up a reconnect.
	            			//Log.d(TAG, "ANDROID_MESSAGE_TYPE_**: Disconnecting: Broken connection or done receiving.") ;
	            			connection.disconnect();
	            			delegate.lccd_connectionStatusChanged(MessagePassingConnection.Status.DISCONNECTED) ;
		            		handler.sendMessageDelayed( handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECT ), delegate.lccd_requestDelayUntilConnect() ) ;
		            		if ( welcomed ) {
		            			lobbyLog.logDisconnected(playerSlot, name, lobby.getPlayerStatus(lobby.playerSlot), null) ;
		            			welcomed = false ;
		            		}
		            	}
	            		break ;
	            	
	            	// Something from outside has demanded an action from us.  Send
	            	// the appropriate info to server, if possible (otherwise, SILENT FAIL!)
	            	case ANDROID_MESSAGE_TYPE_SEND_REQUEST_GAME_XML:
	            		if ( welcomed && connection.connectionStatus() == MessagePassingConnection.Status.CONNECTED )
	            			connection.sendMessage( outgoingMessage.setAsGameModeXMLRequest(arg1) ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_SEND_VOTE_FOR:
	            		if ( welcomed && connection.connectionStatus() == MessagePassingConnection.Status.CONNECTED )
	            			connection.sendMessage( outgoingMessage.setAsVote( playerSlot, arg1) ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_SEND_UNVOTE_FOR:
	            		if ( welcomed && connection.connectionStatus() == MessagePassingConnection.Status.CONNECTED )
	            			connection.sendMessage( outgoingMessage.setAsUnvote( playerSlot, arg1) ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_SEND_ACTIVE:
	            		if ( welcomed && connection.connectionStatus() == MessagePassingConnection.Status.CONNECTED )
	            			connection.sendMessage( outgoingMessage.setAsActive( playerSlot ) ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_SEND_INACTIVE:
	            		if ( welcomed && connection.connectionStatus() == MessagePassingConnection.Status.CONNECTED )
	            			connection.sendMessage( outgoingMessage.setAsInactive( playerSlot ) ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_SEND_IN_GAME:
	            		if ( welcomed && connection.connectionStatus() == MessagePassingConnection.Status.CONNECTED )
	            			connection.sendMessage( outgoingMessage.setAsInGame( playerSlot ) ) ;
	            		break ;
	            		
	            	case ANDROID_MESSAGE_TYPE_SEND_TEXT_MESSAGE:
	            		if ( welcomed && connection.connectionStatus() == MessagePassingConnection.Status.CONNECTED )
	            			connection.sendMessage( outgoingMessage.setAsTextMessage( playerSlot, (String)obj ) ) ;
	            		break ;
	            		
	            	
	            	}
	            	
	            }
	        } ;
	        
	        /*
	         * Start looping the message queue of this thread.
	         */
	        //Log.v(TAG,"LobbyClientCommunications entering loop.") ;
	        Looper.loop();
			
			// Finished.
			//Log.v(TAG, "LobbyClientCommunications leaving loop") ;
		}
		
		
		/**
		 * Handles the incoming message, possibly by sending 
		 * a response.  Will alter the contents of the field 'message'.
		 * @param m The incoming message.  WILL NOT ALTER THE CONTENT OF THIS OBJECT.
		 * 
		 * @return Whether we successfully handled the message.
		 */
		private boolean handleIncomingMessage( LobbyMessage m ) {
			int slot, len, gameMode ;
			String text ;
			
			// NOTE: If adding a new message type, make sure you implement the
			// case both here and in mesageContentOK.
			if ( !messageContentOK( m ) ) {
				Log.v(TAG, "message content " + m.getType() + " not OK!") ;
				return false ;
			}
			
			Delegate delegate = mwrDelegate.get() ;
			if ( delegate == null )
				return false ;	// need a delegate to proceed
			
			switch( m.getType() ) {
			
			// Message types that a Client should never receive...
			case Message.TYPE_UNKNOWN:
			case Message.TYPE_EXIT:
			case Message.TYPE_NONCE:
			case LobbyMessage.TYPE_GAME_MODE_LIST_REQUEST:
			case LobbyMessage.TYPE_GAME_MODE_XML_REQUEST:
			case LobbyMessage.TYPE_VOTE:
			case LobbyMessage.TYPE_UNVOTE:
			case LobbyMessage.TYPE_GAME_MODE_VOTE_REQUEST:
			case Message.TYPE_I_AM_CLIENT:
			case Message.TYPE_HOST_PRIORITY:
				//Log.v(TAG, "Incoming message: nothing to do with this. " + m.getType() ) ;
				return false ;
				
			// Redundant but okay
			case Message.TYPE_I_AM_HOST:
			case Message.TYPE_YOU_ARE_CLIENT:
				break ;
				
			case Message.TYPE_KICK:
				//Log.v(TAG, "Incoming message: kick" ) ;
				slot = m.getPlayerSlot();
				text = m.getText();
				lobbyLog.logPlayerKicked(slot, lobby.getPlayerName(slot), text, lobby.getPlayerStatus(slot), null) ;
				delegate.lccd_messageKick(slot, text) ;
				break ;
				
			case Message.TYPE_PERSONAL_NONCE:
				//Log.v(TAG, "Incoming message: personal nonce") ;
				lobby.setPersonalNonce(m.getPlayerSlot(), m.getNonce()) ;
				break ;
				
			case Message.TYPE_PLAYER_NAME:
				//Log.v(TAG, "Incoming message: name") ;
				text = m.getName() ;
				if ( text.length() > 32 )		// HACK to sanitize name length
					text = text.substring(0,32) ;
				lobby.setName(m.getPlayerSlot(),text) ;
				break ;
				
			case Message.TYPE_NONCE_REQUEST:
				// Send our nonce in return.
				//Log.v(TAG, "Incoming message: nonce request" ) ;
				connection.sendMessage(outgoingMessage.setAsNonce(nonce)) ;
				break ;
				
			case Message.TYPE_PERSONAL_NONCE_REQUEST:
				// Send our personal nonce in return.
				//Log.v(TAG, "Incoming message: personal nonce request" ) ;
				connection.sendMessage(outgoingMessage.setAsPersonalNonce(playerSlot, personalNonce)) ;
				break ;
				
			case Message.TYPE_NAME_REQUEST:
				// Send our name.
				// Send our nonce in return.
				//Log.v(TAG, "Incoming message: name request" ) ;
				connection.sendMessage(outgoingMessage.setAsMyName(name)) ;
				break ;
				
			case Message.TYPE_TOTAL_PLAYER_SLOTS:
				//Log.v(TAG, "Incoming message: total player slots") ;
				booleanPlayerArray = new boolean[m.getPlayerSlot()] ;
				intPlayerArray = new int[m.getPlayerSlot()] ;
				lobby.setMaxPlayers(m.getPlayerSlot()) ;
				delegate.lccd_messageTotalPlayerSlots( m.getPlayerSlot() ) ;
				break ;
				
			case Message.TYPE_PERSONAL_PLAYER_SLOT:
				//Log.v(TAG, "Incoming message: personal player slot") ;
				playerSlot = m.getPlayerSlot() ;
				lobby.setLocalPlayerSlot(playerSlot) ;
				lobby.setName(playerSlot, name) ;
				lobby.setPersonalNonce(playerSlot, personalNonce) ;
				delegate.lccd_messageLocalPlayerSlot( playerSlot ) ;
				break ;
			
			case Message.TYPE_WELCOME_TO_SERVER:
				Log.v(TAG, "Incoming message: welcome to server" ) ;
				welcomed = true ;
				System.err.println("welcomed to server: calling logConnected") ;
				lobbyLog.logConnected(lobby.playerSlot, lobby.playerName, lobby.getPlayerStatus(lobby.playerSlot), null) ;
				// Tell the delegate
				delegate.lccd_messageWelcomeToServer() ;
				// Spam the server with all the current lobby information that wasn't contained
				// in the data they just sent.
				sendServerAllLocalStatusDiscrepancies() ;
				sendServerAllAuthTokens() ;
				break ;
				
			case Message.TYPE_SERVER_CLOSING:
			case Message.TYPE_SERVER_CLOSING_FOREVER:
				Log.v(TAG, "Incoming message: closing" ) ;
				// For now, we don't distinguish between these.
				delegate.lccd_messageServerClosingForever() ;
				break ;
			
			case Message.TYPE_PLAYER_QUIT:
				//Log.v(TAG, "Incoming message: player quit") ;
				boolean [] inLobby = lobby.playerInLobby ;
				boolean [] inLobbyCopy = new boolean [inLobby.length] ;
				for ( int i = 0; i < inLobby.length; i++ )
					inLobbyCopy[i] = inLobby[i] ;
				slot = m.getPlayerSlot() ;
				inLobbyCopy[slot] = false ;
				lobby.setPlayersInLobby(inLobbyCopy) ;
				break ;
			
			case Message.TYPE_HOST:
				//Log.v(TAG, "Incoming message: host name") ;
				delegate.lccd_messageHost( m.getPlayerSlot(), m.getName() ) ;
				break ;
				
			case LobbyMessage.TYPE_LOBBY_STATUS:
				//Log.v(TAG, "Incoming message: lobby status") ;
				// includes num players, max players, age, and possibly a lobby
				// name and address.
				lobby.setMaxPlayers( m.getMaxPlayers() ) ;
				lobby.setAge( m.getAge() ) ;
				booleanPlayerArray = new boolean[lobby.getMaxPeople()] ;
				text = m.getText(); 
				if ( text != null ) {
					if ( text.length() > 32 )		// HACK to sanitize name length
						text = text.substring(0,32) ;
					Log.v(TAG, "Received lobby name: is " + text ) ;
					lobby.setLobbyName( text ) ;
				}
				break ;
			
			case LobbyMessage.TYPE_PLAYERS_IN_LOBBY:
				//Log.v(TAG, "Incoming message: players in lobby") ;
				m.getPlayers(booleanPlayerArray) ;
				// We do not log our OWN entrance and departure here; we
				// handle that in other places.
				for ( int i = 0; i < lobby.getMaxPeople(); i++ ) {
					if ( welcomed && i != lobby.playerSlot && !lobby.playerInLobby[i] && booleanPlayerArray[i] )
						lobbyLog.logPlayerJoined(i, lobby.getPlayerName(i), Lobby.PLAYER_STATUS_ACTIVE, null) ;
					else if ( welcomed && i != lobby.playerSlot && lobby.playerInLobby[i] && !booleanPlayerArray[i] ) {
						lobbyLog.logPlayerLeft(i, lobby.getPlayerName(i), Lobby.PLAYER_STATUS_ACTIVE, null) ;
						// reset their votes.
						lobby.resetPlayerVotes(i) ;
					}
				}
				lobby.setPlayersInLobby(booleanPlayerArray) ;
				break ;
			
			case LobbyMessage.TYPE_TEXT_MESSAGE:
				//Log.v(TAG, "Incoming text message!") ;
				slot = m.getPlayerSlot() ;
				lobbyLog.logChat(slot, lobby.getPlayerName(slot), m.getText(), lobby.getPlayerStatus(slot), null) ;
				break ;
			
			case LobbyMessage.TYPE_GAME_MODE_LIST:
				//Log.v(TAG, "Incoming Game Mode List") ;
				lobby.setGameModes( m.getGameModeList() ) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_TOKEN:
				delegate.lccd_messageGameModeAuthToken( m.getGameMode(), m.getAuthToken() ) ;
				mAuthTokens.put(m.getGameMode(), m.getAuthToken()) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_REVOKE:
				gameMode = m.getGameMode() ;
				delegate.lccd_messageGameModeRevokeAuthToken( gameMode ) ;
				// respond with our own auth token, if we have one.
				Serializable token = mLocalAuthTokens.get(gameMode) ;
				if ( token != null )
					connection.sendMessage(outgoingMessage.setAsGameModeAuthorizationToken(gameMode, token)) ;
				break ;
			
			case LobbyMessage.TYPE_GAME_MODE_XML:
				//Log.v(TAG, "Incoming message: Game Mode XML") ;
				delegate.lccd_messageGameModeXML( m.getGameMode(), m.getGameModeXML() ) ;
				break ;
			
			case LobbyMessage.TYPE_GAME_MODE_VOTES:
				//Log.v(TAG, "Incoming message: game mode votes") ;
				m.getPlayers(booleanPlayerArray) ;
				lobby.setVotesForGameMode(m.getGameMode(), booleanPlayerArray) ;
				Log.v(TAG, "LobbyClientCommunications: set votes in Lobby.") ;
				break ;
				
			case LobbyMessage.TYPE_ACTIVE:
				//Log.v(TAG, "Incoming message: active") ;
				slot = m.getPlayerSlot() ;
				lobbyLog.logPlayerStatusChange(slot, lobby.getPlayerName(slot), Lobby.PLAYER_STATUS_ACTIVE, lobby.getPlayerStatus(slot)) ;
				lobby.setPlayerActive(slot) ;
				break ;
				
			case LobbyMessage.TYPE_INACTIVE:
				//Log.v(TAG, "Incoming message: inactive") ;
				slot = m.getPlayerSlot() ;
				lobbyLog.logPlayerStatusChange(slot, lobby.getPlayerName(slot), Lobby.PLAYER_STATUS_INACTIVE, lobby.getPlayerStatus(slot)) ;
				lobby.setPlayerInactive(slot) ;
				break ;
				
			case LobbyMessage.TYPE_IN_GAME:
				//Log.v(TAG, "Incoming message: inactive") ;
				slot = m.getPlayerSlot() ;
				lobbyLog.logPlayerStatusChange(slot, lobby.getPlayerName(slot), Lobby.PLAYER_STATUS_INACTIVE, lobby.getPlayerStatus(slot)) ;
				lobby.setPlayerInactive(slot) ;
				break ;
				
			case LobbyMessage.TYPE_PLAYER_STATUSES:
				//Log.v(TAG, "Incoming message: player statuses") ;
				len = m.getStatuses(intPlayerArray) ;
				// log any changes from previous activity to new activity.
				for ( int i = 0; i < len; i++ ) {
					// Log.d(TAG, "player " + i + " has status " + intPlayerArray[i]) ;
					if ( intPlayerArray[i] != lobby.getPlayerStatus(i) ) {
						lobbyLog.logPlayerStatusChange(
								i,
								lobby.getPlayerName(i),
								intPlayerArray[i],
								lobby.getPlayerStatus(i)) ;
					}
				}
				lobby.setPlayersStatuses(intPlayerArray) ;
				break ;
			
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_COUNTDOWN:
				//Log.v(TAG, "Incoming message: game mode launch countdown") ;
				m.getPlayers( booleanPlayerArray ) ;
				int countdownNumber = m.getCountdownNumber() ;
				int countdownStatus = m.getCountdownStatus() ;
				lobby.setCountdownDelay(countdownNumber, m.getGameMode(), booleanPlayerArray, m.getDelay(), countdownStatus, new Integer(countdownNumber) ) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_ABORTED:
				//Log.v(TAG, "Incoming message: game mode launch aborted") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.setCountdownAborted(m.getCountdownNumber(), false) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_HALTED:
				//Log.v(TAG, "Incoming message: game mode launch halted") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.setCountdownHalted(m.getCountdownNumber(), m.getGameMode(), booleanPlayerArray) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_FAILED:
				//Log.v(TAG, "Incoming message: game mode launch failed") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.setCountdownAborted( m.getCountdownNumber(), true ) ;
				break ;
				
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_ABSENT:
				//Log.v(TAG, "Incoming message: game mode launch as absent") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.countLaunch() ;
				lobbyLog.logLaunch(m.getPlayerSlot(), lobby.getPlayerName(m.getPlayerSlot()), m.getGameMode(), null) ;
				delegate.lccd_messageGameModeLaunchAsAbsent( m.getCountdownNumber(), m.getGameMode(), booleanPlayerArray, ArrayOps.count(booleanPlayerArray) ) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_DIRECT_CLIENT:
				//Log.v(TAG, "Incoming message: game mode launch as direct client") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.countLaunch() ;
				lobbyLog.logLaunch(m.getPlayerSlot(), lobby.getPlayerName(m.getPlayerSlot()), m.getGameMode(), null) ;
				delegate.lccd_messageGameModeLaunchAsDirectClient(
						m.getCountdownNumber(), m.getGameMode(), booleanPlayerArray, ArrayOps.count(booleanPlayerArray),
						m.getNonce(), m.getSocketAddress() ) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_DIRECT_HOST:
				//Log.v(TAG, "Incoming message: game mode launch as direct host") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.countLaunch() ;
				lobbyLog.logLaunch(m.getPlayerSlot(), lobby.getPlayerName(m.getPlayerSlot()), m.getGameMode(), null) ;
				delegate.lccd_messageGameModeLaunchAsDirectHost(
						m.getCountdownNumber(), m.getGameMode(), booleanPlayerArray, ArrayOps.count(booleanPlayerArray),
						m.getNonce(), m.getSocketAddress() ) ;
				break ;
				
			
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_CLIENT:
				//Log.v(TAG, "Incoming message: game mode launch as matchseeker client") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.countLaunch() ;
				lobbyLog.logLaunch(m.getPlayerSlot(), lobby.getPlayerName(m.getPlayerSlot()), m.getGameMode(), null) ;
				delegate.lccd_messageGameModeLaunchAsMatchseekerClient(
						m.getCountdownNumber(), m.getGameMode(), booleanPlayerArray, ArrayOps.count(booleanPlayerArray),
						m.getNonce(), m.getSocketAddress() ) ;
				break ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST:
				//Log.v(TAG, "Incoming message: game mode launch as matchseeker host") ;
				m.getPlayers( booleanPlayerArray ) ;
				lobby.countLaunch() ;
				lobbyLog.logLaunch(m.getPlayerSlot(), lobby.getPlayerName(m.getPlayerSlot()), m.getGameMode(), null) ;
				delegate.lccd_messageGameModeLaunchAsMatchseekerHost(
						m.getCountdownNumber(), m.getGameMode(), booleanPlayerArray, ArrayOps.count(booleanPlayerArray),
						m.getNonce(), m.getEditKey(), m.getSocketAddress() ) ;
				break ;
				
			case LobbyMessage.TYPE_PREFERRED_COLOR:
				//Log.v(TAG, "Incoming message: preferred color") ;
				delegate.lccd_messagePreferredColor(m.getPlayerSlot(), m.getColor()) ;
				break ;
				
				
			default:
				Log.v(TAG, "Incoming message: we have no special way of handling message type " + m.getType()) ;
				return false ;
			}
			
			return true ;
		}
		
		
		/**
		 * Checks whether the message content is acceptable - all values are within
		 * OK ranges and using them directly won't cause any crashes or anything.
		 * @param m
		 * @return
		 */
		private boolean messageContentOK( LobbyMessage m ) {
			int slot = m.getPlayerSlot() ;
			String text ;
			
			int type = m.getType() ;
			
			switch( type ) {
			
			// Message types that a Client should never receive...
			case Message.TYPE_UNKNOWN:
			case Message.TYPE_EXIT:
			case Message.TYPE_NONCE:
			case LobbyMessage.TYPE_GAME_MODE_LIST_REQUEST:
			case LobbyMessage.TYPE_GAME_MODE_XML_REQUEST:
			case LobbyMessage.TYPE_VOTE:
			case LobbyMessage.TYPE_UNVOTE:
			case LobbyMessage.TYPE_GAME_MODE_VOTE_REQUEST:
			case Message.TYPE_HOST_PRIORITY:
			case Message.TYPE_I_AM_CLIENT:
				return false ;
				
			// Redundant, but OK
			case Message.TYPE_I_AM_HOST:
			case Message.TYPE_YOU_ARE_CLIENT:
				return true ;
				
			// Authorization Tokens...
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_TOKEN:
				return m.getAuthToken() != null ;
				
			case LobbyMessage.TYPE_GAME_MODE_AUTHORIZATION_REVOKE:
				return true ;
				
			case Message.TYPE_KICK:
				text = m.getText();
				return slot >= 0 && slot < lobby.getMaxPeople() ;
				
			case Message.TYPE_PERSONAL_NONCE:
				return slot >= 0 && slot < lobby.getMaxPeople() && m.getNonce() != null ;
				
			case Message.TYPE_PLAYER_NAME:
				text = m.getName() ;
				return slot >= 0 && slot < lobby.getMaxPeople() && text != null ;
				
			case Message.TYPE_NONCE_REQUEST:	
			case Message.TYPE_PERSONAL_NONCE_REQUEST:
			case Message.TYPE_NAME_REQUEST:
				return true ;	// no content
				
			case Message.TYPE_TOTAL_PLAYER_SLOTS:
				return slot >= 0 && slot <= 64 ;		// 64 is more than we'll ever have.
				
			case Message.TYPE_PERSONAL_PLAYER_SLOT:
				return slot >= 0 && slot < lobby.getMaxPeople() ;
			
			case Message.TYPE_WELCOME_TO_SERVER:
			case Message.TYPE_SERVER_CLOSING:
			case Message.TYPE_SERVER_CLOSING_FOREVER:
				return true ;		// no content
			
			case Message.TYPE_PLAYER_QUIT:
				return slot >= 0 && slot < lobby.getMaxPeople() ;
			
			case Message.TYPE_HOST:
				text = m.getName() ;
				return text != null ;		// slot is unnecessary, possibly this is a dedicated server.
				
			case LobbyMessage.TYPE_LOBBY_STATUS:
				return m.getMaxPlayers() > 0 && m.getMaxPlayers() <= 64
						&& m.getAge() >= 0 ;
			
			case LobbyMessage.TYPE_PLAYERS_IN_LOBBY:
				return m.getPlayers(null) <= lobby.getMaxPeople() ;
			
			case LobbyMessage.TYPE_TEXT_MESSAGE:
				return slot >= 0 && slot < lobby.getMaxPeople() && m.getText() != null ;
			
			case LobbyMessage.TYPE_GAME_MODE_LIST:
				return true ;		// should be okay
			
			case LobbyMessage.TYPE_GAME_MODE_XML:
				return m.getGameMode() >= 0 && m.getGameModeXML() != null ;
			
			case LobbyMessage.TYPE_GAME_MODE_VOTES:
				return m.getGameMode() >= 0 && m.getPlayers(null) <= lobby.getMaxPeople() ;
				
			case LobbyMessage.TYPE_ACTIVE:
				return slot >= 0 && slot < lobby.getMaxPeople() ;
				
			case LobbyMessage.TYPE_INACTIVE:
				return slot >= 0 && slot < lobby.getMaxPeople() ;
				
			case LobbyMessage.TYPE_PLAYER_STATUSES:
				if ( intPlayerArray == null || intPlayerArray.length != lobby.getMaxPeople() )
					intPlayerArray = new int[lobby.getMaxPeople()] ;
				m.getStatuses(intPlayerArray) ;
				for ( int i = lobby.getMaxPeople(); i < intPlayerArray.length; i++ )
					if ( intPlayerArray[i] != Lobby.PLAYER_STATUS_NOT_CONNECTED )
						return false ;
				return true ;
			
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_COUNTDOWN:
				return m.getCountdownNumber() >= 0 && m.getGameMode() >= 0 && m.getPlayers(null) <= lobby.getMaxPeople() && m.getDelay() >= 0 ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_ABORTED:
				return m.getCountdownNumber() >= 0 && m.getPlayers(null) <= lobby.getMaxPeople() ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_HALTED:
				return m.getGameMode() >= 0 && m.getCountdownNumber() >= 0 && m.getPlayers(null) <= lobby.getMaxPeople() ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_FAILED:
				return m.getGameMode() >= 0 && m.getCountdownNumber() >= 0 && m.getPlayers(null) <= lobby.getMaxPeople() ;
				
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_ABSENT:
				return slot >= 0 && slot < lobby.getMaxPeople()
						&& m.getGameMode() >= 0 && m.getCountdownNumber() >= 0
						&& m.getPlayers(null) <= lobby.getMaxPeople() ;
			
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_DIRECT_CLIENT:
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_DIRECT_HOST:
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_CLIENT:
			case LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST:
				boolean ok = slot >= 0 && slot < lobby.getMaxPeople()
						&& m.getGameMode() >= 0 && m.getCountdownNumber() >= 0
						&& m.getPlayers(null) <= lobby.getMaxPeople()
						&& m.getNonce() != null && m.getSocketAddress() != null ;
				if ( type == LobbyMessage.TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST )
					ok = ok && m.getEditKey() != null ;
				return ok ;

				
			case LobbyMessage.TYPE_PREFERRED_COLOR:
				return slot >= 0 && slot < lobby.getMaxPeople() ;
				
			default:
				//Log.v(TAG, "Incoming message: have no way to check messageContentOK for message type " + m.getType()) ;
				return false ;
			}
		}
		
		
		
		/**
		 * The Lobby object contains what it thinks the server status is, which we assume is
		 * up-to-date, and the local status, which we assume corresponds to what the user
		 * has entered.
		 * 
		 * Sends message which, if obeyed, will bring the server up-to-date as to our local status.
		 */
		private void sendServerAllLocalStatusDiscrepancies() {
			// Send our votes / unvotes for game modes
			int [] gameModes = lobby.getGameModes() ;
			for ( int i = 0; i < gameModes.length; i++ ) {
				int gameMode = gameModes[i] ;
				boolean [] playerVotedFor = lobby.getVotes(gameMode) ;
				boolean localVotedFor = lobby.getLocalVote(gameMode) ;
				// if local doesn't match the array, send an update.
				if ( localVotedFor != playerVotedFor[lobby.playerSlot] )
					connection.sendMessage( localVotedFor ? outgoingMessage.setAsVote(0, gameMode) : outgoingMessage.setAsUnvote(0, gameMode) ) ;
			}
			// If the lobby thinks we are active, but we're not...
			
			
			if ( status != ( lobby.getPlayerStatus(lobby.playerSlot) ) ) {
				//Log.v(TAG, "sending discrepancy: we actually have status:" + status) ;
				switch( status ) {
				case Lobby.PLAYER_STATUS_ACTIVE:
					connection.sendMessage( outgoingMessage.setAsActive(0) ) ;
					break ;
				case Lobby.PLAYER_STATUS_INACTIVE:
					connection.sendMessage( outgoingMessage.setAsInactive(0) ) ;
					break ;
				case Lobby.PLAYER_STATUS_IN_GAME:
					connection.sendMessage( outgoingMessage.setAsInGame(0) ) ;
					break ;
				
				}
			}
			
			// That's really it.  We don't send the lobby any "queued messages" or anything.
		}
		
		
		/**
		 * Send all our non-null authorization tokens to the server.
		 */
		private void sendServerAllAuthTokens() {
			Log.d(TAG, "sendServerAllAuthTokens") ;
			// Send our auth tokens for game modes.
			Enumeration<Integer> enumeration = mLocalAuthTokens.keys() ;
			for ( ; enumeration.hasMoreElements() ; ) {
				Integer gameMode = enumeration.nextElement() ;
				Serializable token = mLocalAuthTokens.get(gameMode) ;
				if ( token != null )
					connection.sendMessage( outgoingMessage.setAsGameModeAuthorizationToken(gameMode, token ) ) ;
			}
		}
		
	}
	
	
	// Connection to the lobby
	MessagePassingConnection connection ;
	
	// Here's some info set by the constructor
	Nonce nonce ;
	Nonce personalNonce ;
	String name ;
	
	// And additional info about me.
	int playerSlot ;
	
	// Are we active in this lobby?
	int status ;
	
	// A controller!
	WeakReference<Delegate> mwrDelegate ;
	
	// A Lobby!
	Lobby lobby ;
	LobbyLog lobbyLog ;
	
	// A thread!
	LobbyClientCommunicationsThread thread ;
	
	// A message for us to use!
	LobbyMessage myMessage ;
	
	// Authorization tokens!  Indexed by GameMode.
	Hashtable<Integer, Serializable> mLocalAuthTokens ;
	Hashtable<Integer, Serializable> mAuthTokens ;
	
	
	public LobbyClientCommunications( Delegate delegate, Lobby lobby, LobbyLog lobbyLog,
			Nonce nonce, Nonce personalNonce, String name, Hashtable<Integer, Serializable> authTokens ) {
		
		
		if ( delegate == null )
			throw new NullPointerException("Provided delegate is null!") ;
		
		this.nonce = nonce ;
		this.personalNonce = personalNonce ;
		this.name = name ;
		
		this.lobby = lobby ;
		this.lobbyLog = lobbyLog ;
		
		this.status = Lobby.PLAYER_STATUS_NOT_CONNECTED ;
		
		this.mLocalAuthTokens = (Hashtable<Integer, Serializable>)authTokens.clone() ;
		this.mAuthTokens = new Hashtable<Integer, Serializable>() ;
		
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
		thread = null ;
		
		myMessage = new LobbyMessage() ;
	}
	
	public void setConnection( MessagePassingConnection connection ) {
		this.connection = connection ;
		this.connection.setDelegate(this) ;
	}
	
	/**
	 * This method allows users to change their name when in a lobby.
	 * Calling this method also changes the name stored for this
	 * LobbyClientCommunications; it may be safely called  regardless of
	 * connection status.
	 * 
	 * Because this method has a guaranteed effect, we do not return success.
	 * Either we send our name immediately, or we send it upon connection.
	 * 
	 * @param name
	 */
	synchronized public void sendName( String name ) {
		this.name = name ;
		myMessage.setAsMyName(name) ;
		
		try {
			if ( thread != null && thread.isAlive() && thread.welcomed ) {
				this.connection.sendMessage(myMessage) ;
			}
		} catch ( IllegalStateException e ) { }	// don't care
	}
	
	
	/**
	 * This method allows users to change their name when in a lobby.
	 * Calling this method also changes the name stored for this
	 * LobbyClientCommunications; it may be safely called  regardless of
	 * connection status.
	 * 
	 * Because this method has a guaranteed effect, we do not return success.
	 * Either we send our name immediately, or we send it upon connection.
	 * 
	 * @param name
	 */
	synchronized public void sendNameIfChanged( String name ) {
		if ( (this.name == null) != (name == null) || (name != null && !name.equals(this.name)) ) {
			sendName(name) ;
		}
	}
	
	
	/**
	 * Send a request for 'rewelcome' messages.  This is useful if a new Activity
	 * has come in but we're using the same connection; the Welcome messages will
	 * contain all the info we need to correctly display player information and votes
	 * and such.
	 */
	synchronized public void sendRewelcomeRequest() {
		myMessage.setAsRewelcomeRequest() ;
		
		try {
			if ( thread != null && thread.isAlive() && thread.welcomed ) {
				this.connection.sendMessage(myMessage) ;
			}
		} catch ( IllegalStateException e ) { }	// don't care
	}
	
	
	synchronized public void updateGameModeAuthToken( int gameMode, Serializable authToken, boolean force ) {
		// Log.d(TAG, "updateGameModeAuthToken " + gameMode + ", " +  authToken + ", force: " + force) ;
		Serializable previousToken = mLocalAuthTokens.get(gameMode) ;
		
		// compare.
		boolean different = false ;
		if ( ( previousToken == null ) != ( authToken == null ) )
			different = true ;
		if ( authToken != null && ( !authToken.equals(previousToken) ) )
			different = true ;
		
		if ( !different && !force )
			return ;
		
		mLocalAuthTokens.put(gameMode, authToken) ;
		
		if ( authToken != null ) {
			myMessage.setAsGameModeAuthorizationToken(gameMode, authToken) ;
		} else {
			myMessage.setAsGameModeAuthorizationRevoke(gameMode) ;
		}
			
		try {
			if ( thread != null && thread.isAlive() && thread.welcomed ) {
				this.connection.sendMessage(myMessage) ;
			}
		} catch ( IllegalStateException e ) { }	// don't care
	}
	
	public Serializable getCurrentAuthToken( int gameMode ) {
		return mAuthTokens.get(gameMode) ;
	}
	
	public synchronized boolean isStarted() {
		return thread != null && thread.isAlive() ;
	}
	
	
	/**
	 * Starts things going.  Very, very simple.
	 */
	public synchronized void start() throws IllegalStateException {
		Log.d(TAG, "start") ;
		if ( thread != null && thread.isAlive() )
			throw new IllegalStateException("Can't start a ClientCommunications twice!") ;
		
		// We require that nonce, personalNonce, and name have been set
		// (although setting them to zero or empty string is fine).
		if ( nonce == null || personalNonce == null || name == null )
			throw new IllegalStateException("Can't start LobbyClientCommunications without providing nonce, pnonce and name.") ;
		
		// We also require a delegate.
		if ( mwrDelegate.get() == null )
			throw new IllegalStateException("Can't start LobbyClientCommunications without a delegate!") ;
		
		thread = new LobbyClientCommunicationsThread() ;
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
	public synchronized void stop() throws IllegalStateException {
		Log.d(TAG, "stop") ;
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
	
	
	/**
	 * The stop() method queues up a STOP command on the underlying thread, which will nicely
	 * declare that it is exiting before closing the connection and quitting the looper.  This is
	 * the proper way to handle things in most circumstances.  However, in some cases we might
	 * want assurances that the thread has closed down, without the luxury of calling 'join()'
	 * - for instance, if we are trying to stop() and join() the Communications from inside
	 * the Client's loop.
	 * 
	 * This method attempts to break down as much of the thread as possible.  Rather than nicely
	 * sending a STOP command, it manually destroys as much of the thread process as it can, in
	 * an attempt to halt operations as quickly as possible.
	 * 
	 * @throws IllegalStateException
	 */
	public synchronized void stopImmediately() throws IllegalStateException {
		Log.d(TAG, "stopImmediately") ;
		if ( thread == null )
			throw new IllegalStateException("Can't stop a ClientCommunications that wasn't started!") ;
		
		if ( thread.running ) {
			while ( thread.handler == null ) {
				try {
					Thread.sleep(10) ;
				} catch( InterruptedException e ) { }
			}
			
			thread.running = false ;
			thread.handler.getLooper().quit() ;
			try {
				connection.disconnect() ;
			} catch( Exception e ) { }
		}
		
		lobby.resetLocalPlayerVotes() ;
	}
	
	
	public synchronized void stopAndProvideOpenConnection() throws IllegalStateException {
		Log.d(TAG, "stopAndProvideOpenConnection") ;
		if ( thread == null )
			throw new IllegalStateException("Can't stop a ClientCommunications that wasn't started!") ;
		
		if ( thread.running ) {
			while ( thread.handler == null ) {
				try {
					Thread.sleep(10) ;
				} catch( InterruptedException e ) { }
			}
			
			// Remove any pending messages other than STOP and STOP_AND_PROVIDE_OPEN_CONNECTION
			for ( int i = 0; i < NUM_ANDROID_MESSAGE_TYPES; i++ )
				if ( i != ANDROID_MESSAGE_TYPE_STOP && i != ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTION )
					thread.handler.removeMessages(i) ;
			
			// Queue up the stop.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP_AND_PROVIDE_OPEN_CONNECTION) ) ;
		}
	}
	
	
	public synchronized void becomeHostIfNoConnectionIn( int millisDelay ) {
		if ( thread == null )
			throw new IllegalStateException("Can't stop a ClientCommunications that wasn't started!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		// Queue up the "become host" step.
		if ( connection.connectionStatus() != MessagePassingConnection.Status.CONNECTED )
			thread.handler.sendMessageDelayed(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_BECOME_HOST_IF_NO_CONNECTION),
					millisDelay ) ;
	}
	
	
	public synchronized void cancelBecomeHostIfNoConnection() {
		if ( thread == null )
			throw new IllegalStateException("Can't stop a ClientCommunications that wasn't started!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		// Queue up the "become host" step.
		thread.handler.removeMessages(ANDROID_MESSAGE_TYPE_BECOME_HOST_IF_NO_CONNECTION) ;
	}
	

	public synchronized void join() throws IllegalStateException {
		if ( thread == null )
			throw new IllegalStateException("Can't join a ClientCommunications that wasn't started!") ;
		ThreadSafety.waitForThreadToTerminate(thread, 1000) ;
	}
	
	
	public void requestGameModeXML( int gameMode ) {
		if ( thread == null )
			throw new IllegalStateException("Can't send any messages when the thread isn't there!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_REQUEST_GAME_XML, gameMode, 0) ) ;
	}
	
	
	public void voteFor( int gameMode ) {
		if ( thread == null )
			throw new IllegalStateException("Can't send any messages when the thread isn't there!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_VOTE_FOR, gameMode, 0) ) ;
	}
	
	public void unvoteFor( int gameMode ) {
		if ( thread == null )
			throw new IllegalStateException("Can't send any messages when the thread isn't there!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_UNVOTE_FOR, gameMode, 0) ) ;
	}
	
	
	public void sendActive() {
		if ( thread == null )
			throw new IllegalStateException("Can't send any messages when the thread isn't there!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		this.status = Lobby.PLAYER_STATUS_ACTIVE ;
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_ACTIVE) ) ;
	}
	
	
	public void sendInactive() {
		if ( thread == null )
			throw new IllegalStateException("Can't send any messages when the thread isn't there!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		this.status = Lobby.PLAYER_STATUS_INACTIVE ;
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_INACTIVE) ) ;
	}
	
	public void sendInGame() {
		if ( thread == null )
			throw new IllegalStateException("Can't send any messages when the thread isn't there!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		this.status = Lobby.PLAYER_STATUS_IN_GAME ;
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_IN_GAME) ) ;
	}
	
	
	public void sendTextMessage( String text ) {
		if ( thread == null )
			throw new IllegalStateException("Can't send any messages when the thread isn't there!") ;
		
		while ( thread.handler == null ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) { }
		}
		
		thread.handler.sendMessage(
				thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_SEND_TEXT_MESSAGE, text) ) ;
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
		// Log.v(TAG,"mpcd_messagePassingConnectionDidReceiveMessage") ;
		
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
		// Log.v(TAG,"mpcd_messagePassingConnectionDoneReceivingMessages") ;
		
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
		// Log.v(TAG,"mpcd_messagePassingConnectionDidConnect") ;
		
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
		// Log.v(TAG,"mpcd_messagePassingConnectionDidFailToConnect") ;
		
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
		// Log.v(TAG,"mpcd_messagePassingConnectionDidBreak") ;
		
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
		// Log.v(TAG,"mpcd_messagePassingConnectionDidDisconnectByPeer") ;
		
		// Tell the handler.
		if ( thread != null ) {
			// Assume we have a handler.  We connect as the first Handled
			// operation, and 'disconnect' as the last, so we shouldn't
			// receive any of these "didReceive" messages after that.
			thread.handler.sendMessage(
					thread.handler.obtainMessage(ANDROID_MESSAGE_TYPE_CONNECTION_PEER_DISCONNECTED)) ;
		}
	}
	
}
