package com.peaceray.quantro.view.lobby;

import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.LobbyLog;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.options.OptionAvailability;


/**
 * An abstract class defining the interface of a LobbyView.
 * 
 * @author Jake
 *
 */
public interface LobbyViewAdapter {

	/**
	 * An Interface defining the required methods for a Delegate of a LobbyView.
	 * @author Jake
	 *
	 */
	public interface Delegate {

		/**
		 * The user has changed their name.
		 */
		public void lvad_userSetName( String name ) ;
		
		/**
		 * The user has voted, or unvoted, for the specified game mode.
		 * @param gameMode
		 * @param v
		 */
		public void lvad_userSetVote( int gameMode, boolean v ) ;
		
		/**
		 * The user has sent a text message.
		 * @param msg
		 */
		public void lvad_userSentTextMessage( String msg ) ;
		
	}

	
	///////////////////////////////////////////////////////////////////////////
	//
	// CONNECTIONS
	//
	// LobbyView needs a controller, and it needs read-access to LobbyClient
	// (although it will probably not make changes to it; that's the
	// Controller's job).
	//
	///////////////////////////////////////////////////////////////////////////	
	
	/**
	 * Sets the controller for this view.
	 */
	public abstract void setDelegate( LobbyViewAdapter.Delegate delegate ) ;
	
	/**
	 * Sets half the "model" for this view; the Lobby object.  This is intended
	 * to be a one-way information stream, with the LobbyView displaying what's
	 * in the LobbyClient, but using the Controller to actually make updates.
	 * @param lc
	 */
	public abstract void setLobby( Lobby lobby ) ;
	
	
	/**
	 * Sets the other half of the "model" for this view: the LobbyLog.  This, like
	 * Lobby, is intended as a one-way information stream, with the LobbyView
	 * displaying either the LobbyLog or a recent (perhaps partial) snapshot of
	 * it, using the Controller to make updates.
	 * @param lobbyLog
	 */
	public abstract void setLobbyLog( LobbyLog lobbyLog ) ;
	
	
	/**
	 * Sets the availability of the specified game mode.  Note that the actual game
	 * modes are read from a lobby object; this method only specifies the
	 * button availability of an existing mode.
	 * 
	 * Note also that, by default, we follow "black list" behavior of "DEFAULT ALLOW."
	 * That is, a particular game mode is assumed to be OptionAvailability.ENABLED
	 * unless this method is called with a different parameter.
	 * 
	 * @param gameMode
	 * @param availability
	 */
	public abstract void setGameModeAvailability( int gameMode, OptionAvailability availability ) ;
	
	
	/**
	 * Sets whether this lobby is local Wifi only, the alternative being
	 * internet.
	 * 
	 * @param wifi
	 */
	public abstract void setIsWifiOnly( boolean wifi ) ;
	
	
	/**
	 * Sets whether this lobby user is acting as the host for the lobby,
	 * the alternative being client-only.
	 * 
	 * @param host
	 */
	public abstract void setIsHost( boolean host ) ;
	
	
	/**
	 * Sets whether the user has Android Beam available at this moment.
	 * @param enabled
	 */
	public abstract void setAndroidBeamIsEnabled( boolean enabled ) ;
	
	/**
	 * Sets the color scheme used by this lobby.  It might be ignored.
	 * @param cs
	 */
	public abstract void setColorScheme( ColorScheme cs ) ;
	
	/**
	 * Sets the sound pool for this lobby view adapter.
	 * @param soundPool
	 */
	public abstract void setSoundPool( QuantroSoundPool soundPool ) ;
	
	/**
	 * Sets the sound pool for this lobby view adapter.
	 * @param soundPool
	 */
	public abstract void setSoundControls( boolean soundControls ) ;
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// UPDATES
	//
	// LobbyView will receive update messages when LobbyClient changes.  These
	// updates are sent by the Controller.  It's up to the LobbyView to handle
	// retrieval of the actual info from LobbyClient and update its own display.
	//
	///////////////////////////////////////////////////////////////////////////	
	
	
	// Updates regarding the lobby object.
	
	/**
	 * The most basic update: player slot and the like.  This should (probably)
	 * only be called once; however, it is called in 'update', so make sure performing
	 * this function doesn't trample other data.
	 */
	public abstract void update_basics() ;
	
	/**
	 * This player's votes have updated.  These updates reflect
	 * local changes - i.e., the LobbyClient knows we voted / unvoted
	 * for something, but the LobbyServer may not know yet.
	 */
	public abstract void update_myVotes() ;
	
	/**
	 * Votes have been updated!
	 */
	public abstract void update_votes() ;
	
	/**
	 * Game launches have updated.  Get new information.
	 */
	public abstract void update_launches() ;
	
	/**
	 * The players in the lobby have changed, either by who's
	 * present, or their names.  Perform an update.
	 */
	public abstract void update_players() ;
	
	/**
	 * Performs all above updates.
	 */
	public abstract void update_lobby() ;
	
	/**
	 * A generic method, called whenever LobbyLog changes.
	 */
	public abstract void update_log() ;
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// THREADING
	//
	// If the LobbyView runs its own thread, these might be useful.  Otherwise,
	// they should do basically nothing.  Possibly 'start' and 'resume' should
	// handle resource allocation and initial state queries, since we are
	// guaranteed to have set a controller and Lobby by the time they
	// are called.
	//
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Starts the thread for a LobbyView
	 */
	public abstract void start() ;
	
	/**
	 * Stops the lobby view thread, if one exists.  This will be 
	 * called before the activity is suspended.  The LobbyView should
	 * stop updating after this call, but should still respond to 'start()'
	 * or 'resume' without loss of functionality.
	 */
	public abstract void stop() ;
	
	
	/**
	 * Blocks until the LobbyView thread is finished.  If the LobbyView
	 * has no dedicated thread, this method should immediately return.
	 */
	public abstract void join() ;
	
	
}
