package com.peaceray.quantro.view.freeplay;

import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.options.OptionAvailability;

public interface FreePlayGameManagerView {

	public interface Delegate {
		// we don't attempt to handle these.
		
		/**
		 * Player wants to start a new game of the specified type.
		 * 
		 * @param view The caller
		 * @param gameMode The game mode in which a new game should be started
		 * @param setup The user wants to perform custom setup before starting the game.
		 */
		public void fpgmv_newGame( FreePlayGameManagerView view, int gameMode, boolean setup ) ;
		
		/**
		 * Player wants to start a load their game of the specified type.
		 * 
		 * @param view The caller
		 * @param gameMode The game mode to be loaded
		 * @param examine The user wants to examine the game before loading it.
		 */
		public void fpgmv_loadGame( FreePlayGameManagerView view, int gameMode, boolean examine ) ;
		
		
		/**
		 * The player wants to examine a leaderboard.  If gameMode is non-negative,
		 * it is the specific leaderboard we want to look at.
		 * 
		 * @param view
		 * @param gameMode
		 */
		public void fpgmv_leaderboard( FreePlayGameManagerView view, int gameMode ) ;
		
		
		/**
		 * The user wants to examine achievements.
		 * @param view
		 */
		public void fpgmv_achievements( FreePlayGameManagerView view ) ;
		
		
		/**
		 * The player wants to create a game mode.
		 * 
		 * @param view The caller
		 */
		public void fpgmv_createGame( FreePlayGameManagerView view ) ;
		
		
		/**
		 * The player wants to edit the specified game mode.
		 * @param view
		 * @param gameMode
		 */
		public void fpgmv_editGame( FreePlayGameManagerView view, int gameMode ) ;
		
		
		/**
		 * The player needs some help cause he's a big dumb idiot who
		 * doesn't even lift probably.
		 * 
		 * @param view
		 */
		public void fpgmv_help( FreePlayGameManagerView view ) ;
		
		
		/**
		 * User wants to look at some settings.
		 * @param view
		 */
		public void fpgmv_openSettings( FreePlayGameManagerView view ) ;
		
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
	 * Sets the color scheme to use in coloring buttons and such
	 * @param scheme
	 */
	public void setColorScheme( ColorScheme scheme ) ;
	
	
	/**
	 * Refreshes the view content
	 */
	public void refreshView() ;
	
	/**
	 * Removes all game modes from the view.
	 * 
	 * Can be called from any thread.
	 */
	public void clearGames() ;
	
	
	/**
	 * Sets the game modes available for launch, along with whether
	 * we have game results.
	 * 
	 * @param gameModes
	 * @param availability
	 * @param hasResult
	 */
	public void setGames(int[] gameModes, OptionAvailability [] availability, boolean[] hasResult ) ;
	
	/**
	 * Sets the game modes available for launch, along with current
	 * game results.  If gameResults is not null, it must be the same
	 * length as gameModes ('null' content is allowed, meaning no current
	 * save).
	 * 
	 * Can be called from any thread.
	 */
	public void setGames( int [] gameModes, OptionAvailability [] availability, GameResult [] gameResults ) ;
	
	
	/**
	 * Sets the current game result ('null' is allowed) for the specified
	 * game mode.
	 * 
	 * Can be called from any thread.
	 * 
	 * @param gameMode
	 * @param gr
	 */
	public void setGameResult( int gameMode, GameResult gr ) ;
	
	
	/**
	 * Sets the current game result to 'null' for the specified
	 * game mode.
	 * 
	 * If 'hasSave' is true, we will behave as if there is an unknown
	 * save available to load.
	 * 
	 * Can be called from any thread.
	 * 
	 * @param gameMode
	 * @param gr
	 */
	public void setGameResult( int gameMode, boolean hasSave ) ;
	
	
	
	
	
	/**
	 * Adds the specified game mode along with its current result ('null'
	 * is allowed).
	 * 
	 * Can be called from any thread.
	 * 
	 * @param gameMode
	 * @param gr
	 */
	public void addGame( int gameMode, OptionAvailability availability, GameResult gr ) ;
	
	
	/**
	 * Adds the specified game mode.
	 * 
	 * If 'hasSave' is true, we will behave as if there is an unknown
	 * save available to load.
	 * 
	 * Can be called from any thread.
	 * 
	 * @param gameMode
	 * @param gr
	 */
	public void addGame( int gameMode, OptionAvailability availability, boolean hasSave ) ;
	
	
	
	
	
	
	/**
	 * Removes the specified game mode, if present.
	 * 
	 * Can be called from any thread.
	 * 
	 * @param gameMode
	 */
	public void removeGame( int gameMode ) ;
	
	
	
	/**
	 * Determines, and returns, whether there is space to display thumbnails within
	 * this manager.  Note that thumbnails (if existent) will AUTOMATICALLY be loaded
	 * and displayed by a GameManagerView, so this call is purely for your own
	 * edification, not to check whether certain other methods need to be called.
	 * 
	 * For example, if this view does not support thumbnails, you may want to avoid
	 * saving thumbnails in any launched games, just in case.
	 * @return
	 */
	public boolean supportsThumbnails() ;
	
	
	
}
