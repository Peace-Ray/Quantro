package com.peaceray.quantro.main;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.LobbyLog;


/**
 * LobbyUserInterface
 * @author Jake
 * 10/3/11 refactoring
 * 
 * This interface defines the methods which the lobby Service, responsible
 * for most lobby-related stuffs (e.g. communications, lobbycoordinator,
 * etc.) will call on the Activity (one presumes) to update the lobby UI.
 * 
 * A lot of UI -> Service interaction occurs through the Service directly, using
 * public-facing methods.  Any UI that wishes to receive Service updates should
 * implement this interface.
 * 
 * Note: setup (i.e., linking together objects that need to be linked)
 * is Activity-driven.  Most of these methods assume the setup is complete
 * and we are providing updates.
 *
 */
public interface LobbyUserInterface {


	
	
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
	public void lui_matchmakingRequestingTicket() ;
	
	
	/**
	 * We received no response from the server when requesting a ticket.
	 * We're going to keep trying, but you may want to let the user know.
	 */
	public void lui_matchmakingNoTicketNoResponse() ;
	
	/**
	 * The lobby is closed.
	 */
	public void lui_matchmakingLobbyClosed() ;
	
	
	/**
	 * We are requesting a match from the matchmaker.
	 */
	public void lui_matchmakingRequestingMatch() ;

	
	/**
	 * We received a match promise from the matchmaker.
	 */
	public void lui_matchmakingReceivedPromise() ;
	
	/**
	 * We successfully matched and are now connected (probably)
	 * to our match!
	 */
	public void lui_matchmakingReceivedMatch() ;
	
	/**
	 * We successfully matched and are now connected (probably)
	 * to our match!
	 */
	public void lui_matchmakingComplete() ;
	
	
	
	/**
	 * We have failed to communicate with the matchmaker.  Because this command is quite
	 * serious, it is unlikely to be called unless numerous connection attempts have
	 * failed, or we have remained in a "disconnected" state for a long period of time.
	 */
	public void lui_matchmakingNoResponse() ;
	
	/**
	 * Rejected by the Matchmaker; it is too busy.  Distinct from "NoResponse"
	 * in that we DID receive information from the matchmaker, we just shouldn't expect
	 * to be matched.
	 */
	public void lui_matchmakingRejectedFull() ;
	
	/**
	 * Rejected by the Matchmaker; our Nonce is invalid.  This is not an error 
	 * we can recover from.
	 */
	public void lui_matchmakingRejectedInvalidNonce() ;
	
	
	/**
	 * This is a strange error.  Just display a message, stop connected,
	 * and be done with it.
	 */
	public void lui_matchmakingRejectedNonceInUse() ;
	
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
	public void lui_matchmakingRejectedPortRandomization() ;
	
	/**
	 * An unspecified error.
	 */
	public void lui_matchmakingRejectedUnspecified() ;
	
	
	/**
	 * We received a match, but then were unable to form a connection with
	 * that match.
	 */
	public void lui_matchmakingFailed() ;
	
	
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
	 * 2. We previously called lui_peacerayServerCommunicationFailure()
	 */
	public void lui_peacerayServerCommunicationOK() ;
	
	
	/**
	 * We are having trouble communicating with the peaceray servers.
	 */
	public void lui_peacerayServerCommunicationFailure() ;
	
	
	/**
	 * According to the peaceray server(s), this lobby is closed.
	 */
	public void lui_peacerayServerLobbyClosed() ;
	
	
	/**
	 * According to the peaceray server(s), this lobby is full.
	 */
	public void lui_peacerayServerLobbyFull() ;
	
	
	/**
	 * According to the peaceray server(s), this lobby is not full.
	 */
	public void lui_peacerayServerLobbyNotFull() ;
	
	
	

	
	/*
	 * ************************************************************************
	 * 
	 * LOBBY CONNECTION UPDATES
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
	 * *************************************************************************
	 */
	
	
	/**
	 * We are attempting to connect to the server.
	 * This method is called every time we begin a conneciton attempt.
	 */
	public void lui_connectionToServerConnecting() ;

	
	/**
	 * We failed to connect to the server (once).  This method
	 * is called every time a connection attempt fails before we
	 * can connect.
	 */
	public void lui_connectionToServerFailedToConnect() ;
	
	
	/**
	 * We have failed enough times that we're giving up connecting.
	 * Calling this method might prompt the Activity to end, or
	 * put up a "quit" message, or something.
	 */
	public void lui_connectionToServerGaveUpConnecting() ;
	
	
	/**
	 * We have connected to the server (finally?) and we are currently
	 * negotiating the particulars of our connection.  It is HIGHLY
	 * likely that gameConnectionConnected() will be called immediately
	 * after this method, since negotiation usually happens very quickly.
	 */
	public void lui_connectionToServerNegotiating() ;
	
	
	/**
	 * We have successfully connect!  The server has welcomed us.
	 */
	public void lui_connectionToServerConnected() ;
	
	
	/**
	 * This method is called if the Connection drops without warning.
	 * We call this method if we are Negotiating or Connected,
	 * and the connection unexpectedly drops.
	 */
	public void lui_connectionToServerDisconnectedUnexpectedly() ;
	
	
	/**
	 * This method is called if the server kicked us.  This is 
	 * a Disconnect message; our Connection is broken.  The user
	 * might be interested in seeing the message.
	 * 
	 * @param msg
	 */
	public void lui_connectionToServerKickedByServer(String msg) ;
	
	
	/**
	 * Another disconnect message; the server has closed forever.  We
	 * will make no further attempts to connect because, as noted, the
	 * server closed FOREVER.
	 */
	public void lui_connectionToServerServerClosedForever() ;
	
	
	/*
	 * ************************************************************************
	 * 
	 * LOBBY DISPLAY SPECIFICS
	 * 
	 * Allows the Service to set, retrieve, unset specific display information
	 * that is relevant to the server connection and possibly game launches.
	 * For example, player color comes in from the server and is sent directly
	 * to the LUI (not through a Lobby instance, because it is irrelevant to
	 * Lobby objects).  We may want to retrieve that color later, for the
	 * purposes of game launches.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * Informs the GUI that the specified gameMode, which itself is among the lobby's
	 * game modes, should be available.
	 * 
	 * If the provided game mode is not premium content, this call should be ignored;
	 * all non-premium game modes are available by default.
	 * 
	 * If it is premium content, the availability should be determined by the
	 * provided boolean.  If 'false' you may want to either dim the VOTE button,
	 * or 
	 * 
	 */
	public void lui_setPremiumGameModeAvailable( int gameMode, boolean available ) ;
	
	
	/**
	 * Sets the color for use in displaying this player.  Should be remembered 
	 * until the next call to setPlayerColor.
	 */
	public void lui_setPlayerColor( int slot, int color ) ;
	
	/**
	 * Retrieves the color most recently set for this player slot, or 0 if no color
	 * has been set.
	 * 
	 * @param slot
	 * @return
	 */
	public int lui_getPlayerColor( int slot ) ;
	
	/**
	 * Returns the default player color for this slot using the provided nonce and pNonce.
	 * The result of this function should be deterministic based on its parameters.
	 * @param slot
	 * @param nonce
	 * @param personalNonce
	 * @return
	 */
	public int lui_getDefaultPlayerColor( boolean isWifi, int slot, Nonce nonce, Nonce personalNonce ) ;
	
	
	/*
	 * ************************************************************************
	 * 
	 * LOBBY CHANGES
	 * 
	 * These methods allow the UI to be informed of changes to the Lobby.  It can
	 * thus update whatever views are displaying the Lobby information.  Note the
	 * UI may want to make direct changes to the Lobby - and that's fine!  These
	 * callbacks will be triggered (if appropriate) in this case, so the UI doesn't
	 * need to handle the display update immediately; things can go like
	 * User touch -> UI -> Service call -> Lobby change
	 * 		-> Lobby callback -> LUI call -> Display update.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * Called when the membership of the lobby has changed; someone entered or left,
	 * or a name change occurred.
	 */
	public void lui_updateLobbyMembership() ;
	
	
	/**
	 * Called when new message(s) have appeared in the Lobby.
	 */
	public void lui_updateLobbyMessages() ;
	
	
	/**
	 * Called when the lobby's game modes have changed.
	 */
	public void lui_updateLobbyGameModes() ;
	
	
	/**
	 * Called when the lobby votes have changed.
	 */
	public void lui_updateLobbyVotes() ;
	
	
	/**
	 * Called when lobby countdowns have updated.
	 */
	public void lui_updateLobbyCountdowns() ;
	
	/**
	 * Called when a new countdown has been added.  Treat this
	 * as a call to "lui_updateLobbyCountdowns", but provides some
	 * additional information (for e.g. playing a sound effect).
	 */
	public void lui_updateLobbyCountdowns_newCountdown() ;
	
	/**
	 * Called when a countdown has been CANCELED (NOT just removed,
	 * for example in preparation for a launch).  Treat this
	 * as a call to "lui_updateLobbyCountdowns", but provides some
	 * additional information (for e.g. playing a sound effect).
	 * 
	 * @param failure: this cancelation represents an attempted launch that failed.
	 */
	public void lui_updateLobbyCountdowns_cancelCountdown( boolean failure ) ;
	
	/**
	 * If we are going to make lobby countdown updates for the purposes
	 * of a game launch, we call this first and after.
	 * @param forLaunch
	 */
	public void lui_updatingLobbyCountdownsForLaunch( boolean forLaunch ) ;
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * LOBBY LAUNCHES
	 * 
	 * These methods inform the Activity of a Game launch.
	 * Note that the previous set of methods imply a change in Lobby data - users
	 * connected, votes changed, etc. - while these methods describe launches, which
	 * are major changes to the Lobby itself.
	 * 
	 * Convention: we leave the Service to handle most operations, with the Activity
	 * displaying useful messages and informing the Service of changes.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * A launch is occurring for which we will act as the host.  Note that this is 
	 * a USER INTERFACE call; we should display something appropriate to the
	 * fact that we are launching, but not actually launch the activity.
	 */
	public void lui_launchAsHost( int gameMode, boolean [] players ) ;
	
	
	/**
	 * A launch is occurring for which we will participate as a client.
	 * @param gameMode
	 * @param players
	 * @param host
	 */
	public void lui_launchAsClient( int gameMode, boolean [] players ) ;
	
	
	/**
	 * A launch is occurring for which we will not participate (we'll probably
	 * stay in the lobby during this time?)
	 * 
	 * @param gameMode
	 * @param players
	 * @param host
	 */
	public void lui_launchAsAbsent( int gameMode, boolean [] players ) ;
	
	
	/*
	 * ************************************************************************
	 * 
	 * LOBBY LOG
	 * 
	 * There is only one generic callback for LobbyLog updates.  The LobbyService
	 * registers itself as the delegate, and will pass the message along to the
	 * LUI at that time.
	 * 
	 * *************************************************************************
	 */
	
	public void lui_lobbyLogNewEvent( LobbyLog lobbyLog, int id, int type ) ;
	
}
