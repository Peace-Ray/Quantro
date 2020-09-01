package com.peaceray.quantro.model.communications;

import com.peaceray.quantro.adapter.action.ActionAdapter;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.model.game.Game;

public abstract class ClientCommunications implements ActionAdapter.GameActionListener {
	
	
	
	/**
	 * Defines an interface for the delegate set for our ClientCommunications
	 * object.
	 * 
	 * ClientCommunications perform game-related updates themselves, especially
	 * Full-Synchronizations and game action updates.  However, some information
	 * needs to be transfered to other objects, and we rely on the delegate to
	 * handle that.
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
		 * Once the ClientCommunications starts, we attempt to Connect to the 
		 * server.  These methods inform the delegate of our connection status
		 * changes, and request guidance as to our next move.
		 * 
		 * *************************************************************************
		 */
		
		
		/**
		 * Notifies the delegate that our connection status has updated,
		 * and provides the new status.  This call is made every time our
		 * Connection changes status.
		 */
		public void ccd_connectionStatusChanged( ClientCommunications cc, MessagePassingConnection.Status connectionStatus ) ;
		
		
		/**
		 * We received garbage data which, for whatever reason, we could not
		 * process.  We will disconnect and attempt to reconnect; we just wanted
		 * to update you on what happened.
		 */
		public void ccd_connectionReceivedIllegalMessage(ClientCommunications cc) ;
		
		
		/*
		 * ************************************************************************
		 * 
		 * SERVER MESSAGES
		 * 
		 * As noted, we handle most messages from the server, but for some we pass
		 * that responsibility to the delegate.
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
		public void ccd_messageWelcomeToServer(ClientCommunications cc) ;
		
		
		/**
		 * The server wants us to know about the host name and host slot.
		 * @param name
		 */
		public void ccd_messageHost( ClientCommunications cc, int slot, String name ) ;
		
		
		/**
		 * The server told us the total number of player slots for this game.
		 * @param slots
		 */
		public void ccd_messageTotalPlayerSlots( ClientCommunications cc, int slots ) ;
		
		
		/**
		 * The server told us which player slot is OUR player slot.
		 * @param slot
		 */
		public void ccd_messageLocalPlayerSlot( ClientCommunications cc, int slot ) ;
		
		
		/**
		 * The server has kicked a player, and provided a reason.  If
		 * playerSlot is < 0, or is OUR slot number, then WE were kicked.
		 * @param playerSlot
		 * @param msg
		 */
		public void ccd_messageKick( ClientCommunications cc, int playerSlot, String msg ) ;
		
		
		/**
		 * Warns that a player will be kicked (unless something changes) at the specified time.
		 * @param cc
		 * @param playerSlot
		 * @param msg
		 * @param atTime
		 */
		public void ccd_messageKickWarning( ClientCommunications cc, int playerSlot, String msg, long atTime ) ;
		
		
		/**
		 * Retracts the previous warning(s) for the specified player.
		 * @param cc
		 * @param playerSlot
		 */
		public void ccd_messageKickWarningRetraction( ClientCommunications cc, int playerSlot ) ;
		
		/**
		 * A player has exited the server; this message indicates that
		 * the player left.
		 * 
		 * @param playerSlot
		 */
		public void ccd_messageQuit( ClientCommunications cc, int playerSlot, boolean fatal ) ;
		
		
		/**
		 * A player has won; this message indicates which player.
		 * 
		 * @param cc
		 * @param playerSlot
		 */
		public void ccd_messageWon( ClientCommunications cc, int playerSlot ) ;
		
		/**
		 * A player has lost; this message indicates which player.
		 * 
		 * @param cc
		 * @param playerSlot
		 */
		public void ccd_messageLost( ClientCommunications cc, int playerSlot ) ;
		
		
		/**
		 * A player has become a spectator.  They will no longer be updating their
		 * game.
		 * @param cc
		 * @param playerSlot
		 */
		public void ccd_messagePlayerIsSpectator( ClientCommunications cc, int playerSlot ) ;
		
		
		/**
		 * The server is going down FOREVER and nicely told us so.
		 */
		public void ccd_messageServerClosingForever(ClientCommunications cc) ;
		
		
		/**
		 * The server has provided an update: player number 'slot'
		 * is named 'name'.
		 * 
		 * @param slot
		 * @param name
		 */
		public void ccd_messagePlayerName( ClientCommunications cc, int slot, String name ) ;
		
		/**
		 * The server is waiting for the specified players to connect,
		 * negotiate, apply synchronizations, or whatever.
		 * @param players Has length equal to the number of player slots
		 * 		previously sent by the server.  'true' indicates that the
		 * 		player is being waited on.
		 * 
		 * 		If 'null', we are waiting for some other reason.
		 */
		public void ccd_messageWaiting( ClientCommunications cc, boolean [] players ) ;
		
		
		/**
		 * The game is currently paused.
		 * @param players  Has length equal to the number of player slots 
		 * 		previously sent by the server.  'true' indicates that the 
		 * 		player has paused the game.
		 * 
		 * 		If 'null', the game is paused for some other reason.
		 */
		public void ccd_messagePaused( ClientCommunications cc, boolean [] players ) ;
		
		
		/**
		 * Time to go!  The server has indicated that we should start
		 * playing the game.
		 */
		public void ccd_messageGo( ClientCommunications cc ) ;
		
		
		/**
		 * The game is over.  Those players indicated have won.
		 * @param winningPlayers  Has length equal to the number of player slots 
		 * 		previously sent by the server.  'true' indicates that the 
		 * 		player has won.
		 * 
		 * 		If 'null', the game is paused for some other reason.
		 */
		public void ccd_messageGameOver( ClientCommunications cc, boolean [] winningPlayers ) ;
		
		
		/**
		 * The specified player has gone up a level.  Probably this will be ignored,
		 * but maybe not.
		 * 
		 * @param cc
		 * @param playerSlot
		 */
		public void ccd_messageLevelUp( ClientCommunications cc, int playerSlot ) ;
		
		/**
		 * The specified player is about to go up a level.  Probably this will be ignored,
		 * but maybe not.
		 * 
		 * @param cc
		 * @param playerSlot
		 */
		public void ccd_messageAboutToLevelUp( ClientCommunications cc, int playerSlot ) ;
		
		
		/**
		 * The specified player initialized.  Probably this will be ignored, but maybe not.
		 * 
		 * @param cc
		 * @param playerSlot
		 */
		public void ccd_messageInitialized( ClientCommunications cc, int playerSlot ) ;
		
		
		
		/*
		 * ************************************************************************
		 * 
		 * INFORMATION REQUESTS
		 * 
		 * While almost all the delegate methods are "updates for delegate" type
		 * messages, with the ClientCommunications continuing on without any problems,
		 * the Communications will occassionally require information from the delegate.
		 * These methods request that information.
		 * 
		 * *************************************************************************
		 */
		
		
		
		
		/**
		 * This method is called before each connection attempt.  We will delay the
		 * specified number of seconds before attempting a connection.  It
		 * is probably wise to return 0 the first time this is called, but
		 * to implement some kind of delay like exponential back-off after
		 * repeated calls.
		 * 
		 * BTW, the order of calls will look something like:
		 * 
		 * ccd_connectionStatusChanged( CONNECTED, BROKEN(or something else) )
		 * ccd_connectionStatusChanged( BROKEN(...), DISCONNECTED )
		 * ccd_requestDelayUntilConnect()
		 * ccd_connectionStatusChanged( DISCONNECTED, PENDING(or connected, or...) )
		 * 
		 * @return The number of milliseconds before our next reconnection.
		 */
		public long ccd_requestDelayUntilConnect(ClientCommunications cc) ;
		
		
		/**
		 * This method is called when the ClientCommunications needs direct access
		 * to a Game object.  Any changes we make to the Game will be enclosed
		 * in synchronized( game ) tags.
		 * 
		 * @param playerSlot The slot for the game object we need access to.
		 * @return A reference to the Game object, or 'null' for an invalid player slot.
		 */
		public Game ccd_requestGameObject( ClientCommunications cc, int playerSlot ) ;
		
		
	}
	
	
	protected Nonce nonce ;
	protected Nonce personalNonce ;
	protected String name ;
	
	protected ActionAdapter [] actionAdapters ;
	protected int localActionAdapter ;
	
	protected ClientCommunications.Delegate delegate ;
	
	// Here's our connection to the server.
	MessagePassingConnection connection ;
	
	
	public ClientCommunications setConnection( MessagePassingConnection conn ) {
		this.connection = conn ;
		return this ;
	}
	
	
	public ClientCommunications setGameNonce( Nonce nonce ) {
		this.nonce = nonce ;
		return this ;
	}
	
	public ClientCommunications setMyNonce( Nonce nonce ) {
		this.personalNonce = nonce ;
		return this ;
	}
	
	
	public ClientCommunications setName( String name ) {
		this.name = name ;
		return this ;
	}
	
	public ClientCommunications setActionAdapter( ActionAdapter adapter, int adapterNum ) {
		actionAdapters[adapterNum] = adapter ;
		return this ;
	}
	
	public ClientCommunications setDelegate( ClientCommunications.Delegate delegate ) {
		this.delegate = delegate ;
		return this ;
	}
	
	//
	// As part of our refactoring, we don't repeatedly 'connect' and
	// 'disconnect.'  Instead we assume that we 'start' and remain
	// in a communicating mode until 'stop()' is called, and we do not require 
	// that a ClientCommunication instance can restart using start().
	//
	
	/**
	 * Starts our communications.  We attempt to connect, sending
	 * periodic updates to our controller as we do.
	 * 
	 * @throws IllegalStateException if the ClientConnection is not properly
	 * configured, or if it has already started.
	 */
	public abstract void start() throws IllegalStateException ;
	
	
	
	
	
	/**
	 * Closes all connection to the server and (non-blocking)
	 * begins to terminate the thread.  Always call "join"
	 * after this, to wait out the thread.
	 * 
	 * This method will send the server a nice "QUIT" message
	 * before disconnecting - assuming that it is connected at the 
	 * time this method is called.
	 * 
	 * @throws IllegalStateException if never started.
	 */
	public abstract void stop() throws IllegalStateException ;
	
	
	/**
	 * Permanently recycle this CC.
	 */
	public void recycle() {
		actionAdapters = null ;
		delegate = null ;
		connection = null ;
	}
	
	
	/**
	 * Blocks until the underlying Thread has terminated.
	 * 
	 * @throws IllegalStateException if never started.
	 */
	public abstract void join() throws IllegalStateException ;
	
	
	/**
	 * Sends a name update if different from the most recent one sent.
	 * 
	 * @param name
	 * @throws IllegalStateException
	 */
	public abstract void sendNameIfChanged( String name ) throws IllegalStateException ;
	
	
	/**
	 * Sends a pause message to the server.  If 'true', sends PAUSE.
	 * If 'false' says UNPAUSE.
	 * 
	 * This method may not successfully send the message; for example,
	 * we may not be Connected at the time this method is called.  However,
	 * we will send the most recent pause request (pause or unpause)
	 * at our first available opportunity, and every time we reconnect
	 * after this.
	 * 
	 * @throws IllegalStateException if never started.
	 */
	public abstract void sendPauseRequest( boolean pauseOn ) throws IllegalStateException ;
	
	
}
