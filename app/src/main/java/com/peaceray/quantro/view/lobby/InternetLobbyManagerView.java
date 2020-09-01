package com.peaceray.quantro.view.lobby;

import java.util.ArrayList;

import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.sound.QuantroSoundPool;

public interface InternetLobbyManagerView {

	
	public interface Delegate {
		// Completely delegate-run operations; the ChallengeManagerView doesn't even
		// attempt to handle them.
		
		/**
		 * The user wants to open a new lobby.  The delegate should take it from here.
		 * 
		 * @param view The view from which the user indicated their desire.
		 */
		public void ilmvd_createNewInternetLobby( InternetLobbyManagerView view ) ;
		
		/**
		 * The user wants to join the lobby described by the specified challenge.  The
		 * delegate should take it from here.
		 * 
		 * @param lobbyDescription The description object for this lobby.
		 * @param view The view from which the user indicated their desire.
		 */
		public void ilmvd_joinInternetLobby( InternetLobby lobby, InternetLobbyManagerView view ) ;
		
		
		/**
		 * The user wants to refresh their list of lobbies.  This is distinct from
		 * a refresh of an individual lobby, of course.
		 * @param view
		 */
		public void ilmvd_refreshInternetLobbyList( InternetLobbyManagerView view ) ;
		
		
		/**
		 * The user wants to hide the specified lobby from our view.
		 * 
		 * @param lobbyDescription The description object for this lobby.
		 * @param view
		 */
		public void ilmvd_hideInternetLobby( InternetLobby lobby, InternetLobbyManagerView view ) ;
		
		
		/**
		 * The user wants to "examine" (whatever that means) the specified lobby.
		 * @param lobbyDescription
		 * @param view
		 */
		public void ilmvd_examineInternetLobby( InternetLobby lobby, InternetLobbyManagerView view ) ;
	
	
		/**
		 * Any "hidden" lobbies should be unhidden.
		 * @param view
		 */
		public void ilmvd_unhideInternetLobbies( InternetLobbyManagerView view ) ;
		
		
		/**
		 * User wants some help.
		 * @param view
		 */
		public void ilmvd_help( InternetLobbyManagerView view ) ;
		
		
		/**
		 * User wants to open settings.
		 * @param view
		 */
		public void ilmvd_settings( InternetLobbyManagerView view ) ;
	}
	
	
	static final int SERVER_RESPONSE_UNSET = -1 ;
	public static final int SERVER_RESPONSE_PENDING = 0 ;
	public static final int SERVER_RESPONSE_NONE = 1 ;
	public static final int SERVER_RESPONSE_RECEIVED = 2 ;
	
	
	/**
	 * The View may have its own thread.  If so, this call indicates that 
	 * it should begin.  May be called multiple times, but we guarantee
	 * that 'stop()' will be called between them.
	 */
	public void start() ;
	
	/**
	 * The View may have its own thread.  If so, this call indicates that
	 * it should terminate.  May be called multiple times, but we guarantee that
	 * 'start()' will be called before each call.
	 */
	public void stop() ;
	
	
	/**
	 * Sets this View's Delegate.  May be called with 'null'.
	 * 
	 * @param delegate An implementation of Delegate.
	 */
	public void setDelegate( Delegate delegate ) ;
	
	/**
	 * Sets a sound pool for button presses.
	 * @param soundPool
	 */
	public void setSoundPool( QuantroSoundPool soundPool ) ;
	
	/**
	 * Sets whether we play sounds for button presses.
	 * @param soundControls
	 */
	public void setSoundControls( boolean soundControls ) ;
	
	
	
	/**
	 * Set the most recent response from the lobby-listing server.  It's important
	 * to know, e.g., if we have no lobbies - did the server tell us that no lobbies exist?
	 * Are we still waiting for a response from the server?  Or did our communications fail?
	 * 
	 * If we already have a list, it's good to know whether the list ended because communication
	 * failed or if it's actually the end of the list.
	 * 
	 * By default, we assume communication is pending (if no lobbies are given) or 
	 * complete (if we have at least 1 lobby).  For that reason, calling this method
	 * is very important.
	 * 
	 * @param response
	 */
	public void setServerResponse( int response ) ;
	
	
	/**
	 * Sets the Challenges to be displayed.  It's up to the View to determine
	 * how those Challenges are displayed (probably according to challenge status,
	 * age, or the like), whether they are displayed (maybe we omit very old REMOVED
	 * challenges?), and how the user interacts with them.
	 */
	public void setLobbies(ArrayList<InternetLobby> lobbies) ;
	
	
	/**
	 * Adds the specified challenge to those displayed; i.e., to those in the
	 * ArrayList<Challenge> provided to setChallenges.  If the challenge already
	 * appears there, this method has no effect.
	 * 
	 * Note: adding a challenge does not necessarily mean that it will be displayed.
	 * The View reserves the right to not display all challenges.
	 * 
	 * @param c
	 */
	public void addLobby(InternetLobby lobby) ;
	
	
	/**
	 * Refreshes only the portion of the View displaying the information regarding the provided
	 * challenge.  If the challenge provided to this method was not among those given to the
	 * most recent call to setChallenges (or any subsequent calls to addChallenge), this method
	 * has no effect.  It does NOT add the challenge to those being displayed, if it is not there
	 * already.
	 * @param c
	 */
	public void refreshView(InternetLobby lobby) ;
	
	
	/**
	 * Removes the specified challenge from the view and refreshes it.  If the challenge is not
	 * found, no effect.
	 * @param c
	 */
	public void removeLobby(InternetLobby lobby) ;
	
	
	/**
	 * Are there currently hidden lobbies? 
	 * @param hidden
	 */
	public void setHiddenLobbies( boolean hidden ) ;
	
	
}
