package com.peaceray.quantro.view.lobby;

import java.util.ArrayList;

import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.sound.QuantroSoundPool;

public interface WifiLobbyManagerView {
	

	public interface Delegate {
		// Completely delegate-run operations; the ChallengeManagerView doesn't even
		// attempt to handle them.
		
		/**
		 * The user wants to open a new lobby.  The delegate should take it from here.
		 * 
		 * @param view The view from which the user indicated their desire.
		 */
		public void wlmvd_createNewWiFiLobby( WifiLobbyManagerView view ) ;
		
		/**
		 * The user wants to join the lobby described by the specified challenge.  The
		 * delegate should take it from here.
		 * 
		 * @param lobbyDescription The description object for this lobby.
		 * @param view The view from which the user indicated their desire.
		 */
		public void wlmvd_joinWiFiLobby( WiFiLobbyDetails details, WifiLobbyManagerView view ) ;
		
		
		/**
		 * The user wants to find a specific lobby.  The delegate should take it from here.
		 * @param view
		 */
		public void wlmvd_findWiFiLobby( WifiLobbyManagerView view ) ;
		
		
		/**
		 * The user wants to hide the specified lobby from our view.
		 * 
		 * @param lobbyDescription The description object for this lobby.
		 * @param view
		 */
		public void wlmvd_hideWiFiLobby( WiFiLobbyDetails details, WifiLobbyManagerView view ) ;
		
		
		/**
		 * The user wants to see a list of options regarding hiding this lobby from view.
		 * For instance, perhaps she wants to hide this lobby by IP address, rather than
		 * Nonce (i.e., "Hide this lobby" vs. "Hide all lobbies at this address."
		 * @param lobbyDescription
		 * @param view
		 */
		public void wlmvd_hideWifiLobbyMenu( WiFiLobbyDetails details, WifiLobbyManagerView view ) ;
		
		
		/**
		 * The user wants to "examine" (whatever that means) the specified lobby.
		 * @param lobbyDescription
		 * @param view
		 */
		public void wlmvd_examineWiFiLobby( WiFiLobbyDetails details, WifiLobbyManagerView view ) ;
	
	
		/**
		 * The user wants to unhide any hidden lobbies.
		 * @param view
		 */
		public void wlmvd_unhideWifiLobbies( WifiLobbyManagerView view ) ;
		
		/**
		 * The user needs some help.
		 * @param view
		 */
		public void wlmvd_help( WifiLobbyManagerView view ) ;
		
		/**
		 * The user wants to check or alter our settings.
		 * @param view
		 */
		public void wlmvd_settings( WifiLobbyManagerView view ) ;
		
		
	}
	
	
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
	 * Sets the interval after which a lobby will be marked as "searching"
	 * even if it has received a status update.  This change will happen
	 * automatically; there is not need to manually specify that certain
	 * lobbies have transitioned.
	 * 
	 * The View will only allow users to join lobbies that we are not
	 * currently searching for, although examination is usually allowed.
	 * 
	 * @param interval
	 */
	public void setMarkAsSearchingInterval( long interval ) ;
	
	
	/**
	 * Returns whether the specified WiFiLobbyDetails would (or should) be marked
	 * as searching.  This can be useful if you'd like to query the searching
	 * status of a Details object to keep things consistent.
	 * @param details
	 * @return
	 */
	public boolean isSearching( WiFiLobbyDetails details ) ;
	
	/**
	 * Sets the Challenges to be displayed.  It's up to the View to determine
	 * how those Challenges are displayed (probably according to challenge status,
	 * age, or the like), whether they are displayed (maybe we omit very old REMOVED
	 * challenges?), and how the user interacts with them.
	 */
	public void setLobbies(ArrayList<WiFiLobbyDetails> lobbies) ;
	
	
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
	public void addLobby(WiFiLobbyDetails details) ;
	
	
	/**
	 * Refreshes only the portion of the View displaying the information regarding the provided
	 * challenge.  If the challenge provided to this method was not among those given to the
	 * most recent call to setChallenges (or any subsequent calls to addChallenge), this method
	 * has no effect.  It does NOT add the challenge to those being displayed, if it is not there
	 * already.
	 * @param c
	 */
	public void refreshView(WiFiLobbyDetails details) ;
	
	
	/**
	 * Removes the specified challenge from the view and refreshes it.  If the challenge is not
	 * found, no effect.
	 * @param c
	 */
	public void removeLobby(WiFiLobbyDetails details) ;
	
	
	/**
	 * Are any lobbies currently hidden?
	 * @param has
	 */
	public void setHiddenLobbies( boolean has ) ;
	
}
