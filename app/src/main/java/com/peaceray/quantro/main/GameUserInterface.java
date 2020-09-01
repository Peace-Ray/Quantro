package com.peaceray.quantro.main;

import android.graphics.Bitmap;

/**
 * GameUserInterface
 * @author Jake
 * 9/27/11 refactoring
 * 
 * This interface defines the methods which the game Service, responsible
 * for most game-related stuffs (e.g. communications, gamecoordinator,
 * ActionAdaptors, etc.) will call on the Activity (one presumes) to
 * update the game UI.
 * 
 * Most UI -> Game communication happens behind the scenes automatically,
 * through the ActionAdapter objects.  For SP/MP, user touches in the UI
 * will pass through ControlsToActionAdapter to the ActionAdapter, which
 * is primarily under Service control.  ClockTicks occur in the GameView,
 * and events triggered by ticks travel through the ActionAdapter.
 * 
 * In other words, there is a minimum of direct Activity->Service interaction,
 * apart from setup and dismissal.  However, Service->Activity 
 * communication will probably be pretty frequent; among other things,
 * the Activity needs to know about game updates like pause, resume,
 * connecting, etc.  These updates prompt the display of messages.
 * 
 * Note: setup (i.e., linking together objects that need to be linked)
 * is Activity-driven.  Most of these methods assume the setup is complete
 * and we are providing updates.
 *
 */
public interface GameUserInterface {
	
	
	////////////////////////////////////////////////////////////////////////////
	// RESULTS!
	public static final int RESULT_SINGLE_PLAYER_GAME_OVER = 0 ;
	public static final int RESULT_SINGLE_PLAYER_CHECKPOINT = 1 ;		// nothing to "resolve," but iterates through the resolution steps anyway.
	public static final int RESULT_MULTI_PLAYER_GAME_OVER = 2 ;
	public static final int RESULT_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER = 3 ;
	public static final int RESULT_SINGLE_PLAYER_REWINDABLE_GAME_OVER = 4 ;
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * MEDIATOR CONNECTION UPDATES
	 * 
	 * Methods for informing the UI about our efforts to connect to the
	 * Peace Ray mediation server.
	 * 
	 * *************************************************************************
	 */
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * MATCH SEEKER UPDATES
	 * 
	 * Methods for informing the UI about our efforts to connect to the
	 * Peace Ray match maker service.  The MatchMaker is a udp-implemented
	 * alternative to TCP Mediation, which - although functional - is
	 * resource-heavyy and limited by the number of open files.  UDP does not
	 * have these limitations and is probably more appropriate for hole-punching.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * We are requesting a ticket in preparation for a match request.
	 */
	public void gui_matchmakingRequestingTicket() ;
	
	
	/**
	 * We received no response from the server when requesting a ticket.
	 * We're going to keep trying, but you may want to let the user know.
	 */
	public void gui_matchmakingNoTicketNoResponse() ;
	
	
	/**
	 * The lobby is closed.
	 */
	public void gui_matchmakingGameClosed() ;
	
	/**
	 * We are requesting a match from the matchmaker.
	 */
	public void gui_matchmakingRequestingMatch() ;

	
	/**
	 * We received a match promise from the matchmaker.
	 */
	public void gui_matchmakingReceivedPromise() ;
	
	/**
	 * We successfully matched and are now connected (probably)
	 * to our match!
	 */
	public void gui_matchmakingReceivedMatch() ;
	
	/**
	 * We successfully matched and are now connected (probably)
	 * to our match!
	 */
	public void gui_matchmakingComplete() ;
	
	
	
	/**
	 * We have failed to communicate with the matchmaker.  Because this command is quite
	 * serious, it is unlikely to be called unless numerous connection attempts have
	 * failed, or we have remained in a "disconnected" state for a long period of time.
	 */
	public void gui_matchmakingNoResponse() ;
	
	/**
	 * Rejected by the Matchmaker; it is too busy.  Distinct from "NoResponse"
	 * in that we DID receive information from the matchmaker, we just shouldn't expect
	 * to be matched.
	 */
	public void gui_matchmakingRejectedFull() ;
	
	/**
	 * Rejected by the Matchmaker; our Nonce is invalid.  This is not an error 
	 * we can recover from.
	 */
	public void gui_matchmakingRejectedInvalidNonce() ;
	
	
	/**
	 * This is a strange error.  Just display a message, stop connected,
	 * and be done with it.
	 */
	public void gui_matchmakingRejectedNonceInUse() ;
	
	/**
	 * The Matchmaker is refusing to match us, because our chosen communication
	 * port does not match the port from which our request was sent.  This means
	 * some form of port randomization or re-mapping is taking place on our end;
	 * it also means that UDP hole-punching will fail as currently implemented.
	 * 
	 * This is not completely unrecoverable.  For example, if the user can switch
	 * from a WiFi network to their data plan (or vice-versa) this problem might
	 * be resolved.
	 * 
	 */
	public void gui_matchmakingRejectedPortRandomization() ;
	
	/**
	 * An unspecified error.
	 */
	public void gui_matchmakingRejectedUnspecified() ;
	
	
	/**
	 * We received a match, but then were unable to form a connection with
	 * that match.
	 */
	public void gui_matchmakingFailed() ;
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * SERVER WEB UPDATES
	 * 
	 * Methods for informing the UI about our interactions with the peaceray
	 * servers regarding the lobby.
	 * 
	 * *************************************************************************
	 */	
	
	/**
	 * Hey, guess what?  If we were having problems communicating with the
	 * server, everything's OK now.  This is only a guaranteed call in the event
	 * that 
	 * 1. Server communication seems OK, and
	 * 2. We previously called gui_peacerayServerCommunicationFailure()
	 */
	public void gui_peacerayServerCommunicationOK() ;
	
	
	/**
	 * We are having trouble communicating with the peaceray servers.
	 */
	public void gui_peacerayServerCommunicationFailure() ;
	
	
	/**
	 * According to the peaceray server(s), this lobby is closed.
	 */
	public void gui_peacerayServerLobbyClosed() ;
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * GAME CONNECTION UPDATES
	 * 
	 * Methods for informing the UI about our efforts to connect to the 
	 * server.  For single player games, we will likely skip right to
	 * 'connected,' so don't rely on a smooth progression between states.
	 * 
	 * For convenience, we provide updates upon each connection attempt.
	 * The UI shouldn't really do anything about this - the service determines
	 * our reconnection policy - but it might be worth informing the player
	 * about what's happening.
	 * 
	 * Our progress
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * We are attempting to connect to the server.
	 * This method is called every time we begin a conneciton attempt.
	 */
	public void gui_connectionToServerConnecting() ;

	
	/**
	 * We failed to connect to the server (once).  This method
	 * is called every time a connection attempt fails before we
	 * can connect.
	 */
	public void gui_connectionToServerFailedToConnect() ;
	
	
	/**
	 * We have failed enough times that we're giving up connecting.
	 * Calling this method might prompt the Activity to end, or
	 * put up a "quit" message, or something.
	 */
	public void gui_connectionToServerGaveUpConnecting() ;
	
	
	/**
	 * We have connected to the server (finally?) and we are currently
	 * negotiating the particulars of our connection.  It is HIGHLY
	 * likely that gameConnectionConnected() will be called immediately
	 * after this method, since negotiation usually happens very quickly.
	 */
	public void gui_connectionToServerNegotiating() ;
	
	
	/**
	 * We have successfully connect!  The server has welcomed us.
	 */
	public void gui_connectionToServerConnected() ;
	
	
	/**
	 * This method is called if the Connection drops without warning.
	 * We call this method if we are Negotiating or Connected,
	 * and the connection unexpectedly drops.
	 */
	public void gui_connectionToServerDisconnectedUnexpectedly() ;
	
	
	/**
	 * This method is called if the server kicked us.  This is 
	 * a Disconnect message; our Connection is broken.  The user
	 * might be interested in seeing the message.
	 * 
	 * @param msg
	 */
	public void gui_connectionToServerKickedByServer(String msg) ;
	
	
	/**
	 * Another disconnect message; the server has closed forever.  We
	 * will make no further attempts to connect because, as noted, the
	 * server closed FOREVER.
	 */
	public void gui_connectionToServerServerClosedForever() ;
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * PLAYER UPDATES
	 * 
	 * We don't want the UI needing to query the Service whenever it needs things
	 * like player names (for status messages) or the slot we've been given.
	 * 
	 * Instead, we call these methods when these things update, and it's up
	 * to the UI to remember them (or not, and to manually query the service for
	 * them later, but that might cause a prolonged wait based on synchronized
	 * methods).
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * Updates the UI as to the number of players in the game.
	 * This method is as-yet unused (we don't expect the number of
	 * players to ever change during an Activity lifecycle) but it's
	 * worth having available.
	 */
	public void gui_updateNumberOfPlayers( int numPlayers ) ;
	
	
	/**
	 * Updates the UI about which slot has been dedicated to the
	 * local player.  This is IMPORTANT, because without this information,
	 * we don't know which ActionAdapter we should hook our controls up
	 * to.
	 * 
	 * @param slotNumber
	 */
	public void gui_updatePlayerLocalSlot( int slotNumber ) ;
	
	
	/**
	 * Updates the UI about the names of players in this game.  We guarantee
	 * that this method will be called AFTER at least one call to
	 * gui_updatePlayerNumber, and that the (most recent) number provided
	 * will be the length of 'names'.
	 * 
	 * @param names
	 */
	public void gui_updatePlayerNames( String [] names ) ;
	
	
	/**
	 * Updates the UI about the host name.  We don't currently expect to
	 * do anything with this value - in fact, we don't expect this to be
	 * called - but put it here as a placeholder in case we want to do
	 * something later.
	 * @param name
	 */
	public void gui_updateHostName( String name ) ;
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * GAME STATE UPDATES
	 * 
	 * Methods for informing the UI about the state of the game; paused,
	 * waiting, ready.  The main purpose of these methods is to display
	 * or dismiss messages.  NONE of these methods should change whether
	 * we are ticking the Game objects.  We leave it to the Service to
	 * determine when the starts / stops ticking.  (explicitly starting/stopping
	 * the game leaves open the possibility of the Service forcing the 
	 * game to tick forward while the UI thinks it is paused - possibly
	 * a useful feature later on).
	 * 
	 * *************************************************************************
	 */
	
	
	/**
     * The indicated player(s) have paused the game.  Informs all
     * relevant systems and components.  If pausedByPlayerSlot is
     * null, we assume the game was paused for some other reason
     * (perhaps because a context menu was brought up, for example).
     * 
     * The callee gets to determine how we react, but probably this
     * should put up a "paused by" message with a pinwheel?
     * 
     * @param pausedByPlayerSlot An array of length #players, with
     * 'true' indicating that the player has paused the game.
     */
    public void gui_gameStateIsPaused( boolean [] pausedByPlayerSlot ) ;
    
    /**
     * Indicates that we are waiting for particular player(s).
     * We may want to give the player the option of canceling if the
     * wait goes on long enough.
     * 
     * The callee can determine its reaction, but probably this should
     * put up a "waiting for" message with a pinwheel?
     * 
     * @param waitingForPlayerSlot
     */
    public void gui_gameStateIsWaiting( boolean [] waitingForPlayerSlot ) ;
    
    /**
     * This method is called by the Service to indicate that everything
     * is ready to go, as far as it is concerned.  Likely this method will
     * be immediately followed by a setTickingGames().
     * 
     */
    public void gui_gameStateIsReady(  ) ;
    
    /**
     * Indicates that the game is over; winning players are provided.
     * This method will probably have no effect now that we have a dedicated
     * 'resolve result' method.
     * 
     * @param winningPlayers
     */
    public void gui_gameStateIsOver( boolean [] winningPlayers ) ;
    
    /**
     * Indicates that the game is "over" (as far as the Service
     * is concerned) and that it is up to the GameUserInterface
     * to "resolve" the result with the player of the game.
     * 
     */
    public void gui_resolveResult( int resultType ) ;
    
    /**
     * Indicates that a resolution is over, and the GameUI
     * should revert any changes it made to resolve a result.
     * This method will be called no more than once after
     * gui_resolveResult, and there will never be more than
     * one resolveResult call in a row without a call to
     * continueAfterResolvingResult in between.  Additionally,
     * as of 12/29/11, the Activity will be able to predict
     * in advance when this call occurs, since it is a result
     * of a call to gameService.resolvedTo*().
     * 
     */
    public void gui_continueAfterResolvingResult( ) ;
    
    
    /**
     * Indicates that the specified player quit the game.
     * @param playerSlot
     */
    public void gui_gameStatePlayerQuit( int playerSlot, boolean fatal ) ;
    
    
    /**
     * Indicates that the specified player won the game.
     * @param playerSlot
     */
    public void gui_gameStatePlayerWon( int playerSlot ) ;
    
    
    /**
     * Indicates that the specified player lose the game.
     * @param playerSlot
     */
    public void gui_gameStatePlayerLost( int playerSlot ) ;
    
    
    /**
     * Indicates that the specified player quit the game.
     * @param playerSlot
     */
    public void gui_gameStatePlayerIsSpectator( int playerSlot ) ;
    
    
    
    /**
     * The specified player was kicked.  This method is called ONLY IF
     * WE were NOT the kicked player.  If we were kicked, 
     * gui_connectionToServerKickedByServer is called instead.
     * @param playerSlot
     * @param txt
     */
    public void gui_gameStatePlayerKicked( int playerSlot, String txt ) ;
    
    
    
    /**
     * The specified player has been warned of an upcoming kick.
     * @param playerSlot
     * @param txt
     * @param kickAtTime
     */
    public void gui_gameStatePlayerKickWarning( int playerSlot, String txt, long kickAtTime ) ;
    
    
    /**
     * The specified player has had their kick warning retracted.
     * 
     * @param playerSlot
     * @param txt
     * @param kickAtTime
     */
    public void gui_gameStatePlayerKickWarningRetraction( int playerSlot ) ;
    
    
    
    
	/*
	 * ************************************************************************
	 * 
	 * GAME TICKS
	 * 
	 * Before the refactor, when only Activities were used, the GameView called
	 * tick() on all game objects when the game was not paused.  Calling tick()
	 * has different functionality depending on whether a game is under user
	 * control (which the Activity does not need to know, really; it's just a
	 * matter of hooking the Controls to the right Adapter).  We also set one
	 * Game object to advance by time, with the rest advancing by explicit
	 * events, but that's set up by the Service, not the UI.
	 * 
	 * However, we DO need to know whether we are currently calling tick() on
	 * our game objects.  If yes, we call tick() within the rendering thread,
	 * so that we get 1 game update every 1 frame.  This is important because
	 * a few game events are set to update no more than once per tick, such
	 * as falling (regardless of time-between-frames).
	 * 
	 * The purposes of these methods:
	 * 
	 * 1. As game state updates, we may want to sometimes pause the game and
	 * 		sometimes resume.  We leave the Service to determine that, not
	 * 		the Activity; leaving it up to the Activity would conflate status
	 *		updates such as "gameReady" with explicit commands to unpause.
	 *
	 * 2. We may eventually want to advance the games even when the UI is not
	 * 		available to tick, such as (e.g.) when a menu is displayed or when
	 * 		the player has pressed the Home button.
	 * 
	 * *************************************************************************
	 */
    
    
    /**
     * If called with 'true', the UI will begin calling tick() on all game()
     * objects with every frame update.  We assume that no other thread is
     * calling tick() concurrently.
     * 
     * If called with 'false', the UI will stop calling tick() on its game
     * objects (if it was calling tick() before.  Until called with 'true',
     * it will NOT tick() any game objects.
     * 
     * NOTE ALSO: If the GUI is advancing games, it should also be accepting
     * 		Controls from the user.  If NOT advancing games, user input should
     * 		be ignored.
     */
    public void gui_toggleAdvancingGames( boolean shouldAdvance ) ;
    
    
    
	/*
	 * ************************************************************************
	 * 
	 * RETRIEVING INFORMATION FROM THE GUI
	 * 
	 * Returns a reference to a thumbnail, if available, showing the current game
	 * state.
	 * 
	 * *************************************************************************
	 */
    
    public Bitmap gui_getGameThumbnail() ;
    
    
    /**
     * Inform the GUI that the game has been saved, just in case
     * they want to do anything with that information.
     */
    public void gui_didSaveGame() ;
	
}
