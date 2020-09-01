package com.peaceray.quantro.utils;

import com.peaceray.quantro.R;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;


/**
 * 'Scores' is a wrapper class similar to Analytics: its purpose is to obfuscate the
 * specifics of the score API used.  As of right (11/27/12) this api was ScoreLoop,
 * using the standard (non-custom) ScoreLoopUI.
 * 
 * We are moving to a Google Play games implementation instead.
 * 
 * @author Jake
 *
 */
public class Scores {
	
	@SuppressWarnings("unused")
	private static final String TAG = "Scores" ;
	
	
	protected static int enm = 0;
	protected static final int LEADERBOARD_INDEX_SP_QUANTRO_A_NORMAL 	= enm++ ;	// 0
	protected static final int LEADERBOARD_INDEX_SP_QUANTRO_B_NORMAL 	= enm++ ;	// 1
	protected static final int LEADERBOARD_INDEX_SP_QUANTRO_C_NORMAL 	= enm++ ;	// 2
	protected static final int LEADERBOARD_INDEX_SP_RETRO_A_NORMAL 		= enm++ ;	// 3
	protected static final int LEADERBOARD_INDEX_SP_RETRO_B_NORMAL 		= enm++ ;	// 4
	protected static final int LEADERBOARD_INDEX_SP_RETRO_C_NORMAL 		= enm++ ;	// 5
	
	protected static final int LEADERBOARD_INDEX_SP_QUANTRO_A_HARD 		= enm++ ;	// 6
	protected static final int LEADERBOARD_INDEX_SP_QUANTRO_B_HARD 		= enm++ ;	// 7
	protected static final int LEADERBOARD_INDEX_SP_QUANTRO_C_HARD 		= enm++ ;	// 8
	protected static final int LEADERBOARD_INDEX_SP_RETRO_A_HARD 		= enm++ ;	// 9
	protected static final int LEADERBOARD_INDEX_SP_RETRO_B_HARD 		= enm++ ;	// 10
	protected static final int LEADERBOARD_INDEX_SP_RETRO_C_HARD 		= enm++ ;	// 11
	
	protected static final int NUM_LEADERBOARDS = enm ;		// 12
	
	
	protected static final String [] LEADERBOARD_IDS = new String[NUM_LEADERBOARDS] ;
	
	public static final void init( Context context ) {
		Resources res = context.getResources() ;

		for (int i = 0; i < NUM_LEADERBOARDS; i++) {
			// TODO populate with real achievement IDs to support uploads
			LEADERBOARD_IDS[i] = "Leaderboard_" + i;
		}
	}
	
	public static final void destroy() {
		// nothing
	}
	
	/**
	 * Returns the leaderboard ID for the specified game mode.  Note that additional
	 * information, such as GameSettings, can change the result -- for example, certain
	 * difficulty levels have their own leaderboards.
	 * 
	 * @param gameMode
	 * @return
	 */
	public static final String getLeaderboardID( int gameMode ) {
		int index = -1 ;		// index into LEADERBOARD_IDS.
		switch( gameMode ) {
		case GameModes.GAME_MODE_SP_QUANTRO_A:
			index = LEADERBOARD_INDEX_SP_QUANTRO_A_NORMAL ;
			break ;
		case GameModes.GAME_MODE_SP_QUANTRO_B:
			index = LEADERBOARD_INDEX_SP_QUANTRO_B_NORMAL ;
			break ;
		case GameModes.GAME_MODE_SP_QUANTRO_C:
			index = LEADERBOARD_INDEX_SP_QUANTRO_C_NORMAL ;
			break ;
		case GameModes.GAME_MODE_SP_RETRO_A:
			index = LEADERBOARD_INDEX_SP_RETRO_A_NORMAL ;
			break ;
		case GameModes.GAME_MODE_SP_RETRO_B:
			index = LEADERBOARD_INDEX_SP_RETRO_B_NORMAL ;
			break ;
		case GameModes.GAME_MODE_SP_RETRO_C:
			index = LEADERBOARD_INDEX_SP_RETRO_C_NORMAL ;
			break ;
		}
		
		if ( index >= 0 )
			return LEADERBOARD_IDS[index] ;
		return null ; 
	}
	
	
	/**
	 * Returns the leaderboard ID for the specified game settings.
	 * 
	 * @param gameMode
	 * @return
	 */
	public static final String getLeaderboardID( GameSettings gs ) {
		int mode = gs.getMode() ;
		int difficulty = gs.hasDifficulty() ? gs.getDifficulty() : GameInformation.DIFFICULTY_NORMAL ;
		
		int index = -1 ;		// index into LEADERBOARD_IDS.
		
		switch( mode ) {
		case GameModes.GAME_MODE_SP_QUANTRO_A:
			switch( difficulty ) {
			case GameInformation.DIFFICULTY_NORMAL:
				index = LEADERBOARD_INDEX_SP_QUANTRO_A_NORMAL ;
				break ;
			case GameInformation.DIFFICULTY_HARD:
				index = LEADERBOARD_INDEX_SP_QUANTRO_A_HARD ;
				break ;
			}
			break ;
		case GameModes.GAME_MODE_SP_QUANTRO_B:
			switch( difficulty ) {
			case GameInformation.DIFFICULTY_NORMAL:
				index = LEADERBOARD_INDEX_SP_QUANTRO_B_NORMAL ;
				break ;
			case GameInformation.DIFFICULTY_HARD:
				index = LEADERBOARD_INDEX_SP_QUANTRO_B_HARD ;
				break ;
			}
			break ;
		case GameModes.GAME_MODE_SP_QUANTRO_C:
			switch( difficulty ) {
			case GameInformation.DIFFICULTY_NORMAL:
				index = LEADERBOARD_INDEX_SP_QUANTRO_C_NORMAL ;
				break ;
			case GameInformation.DIFFICULTY_HARD:
				index = LEADERBOARD_INDEX_SP_QUANTRO_C_HARD ;
				break ;
			}
			break ;
		case GameModes.GAME_MODE_SP_RETRO_A:
			switch( difficulty ) {
			case GameInformation.DIFFICULTY_NORMAL:
				index = LEADERBOARD_INDEX_SP_RETRO_A_NORMAL ;
				break ;
			case GameInformation.DIFFICULTY_HARD:
				index = LEADERBOARD_INDEX_SP_RETRO_A_HARD ;
				break ;
			}
			break ;
		case GameModes.GAME_MODE_SP_RETRO_B:
			switch( difficulty ) {
			case GameInformation.DIFFICULTY_NORMAL:
				index = LEADERBOARD_INDEX_SP_RETRO_B_NORMAL ;
				break ;
			case GameInformation.DIFFICULTY_HARD:
				index = LEADERBOARD_INDEX_SP_RETRO_B_HARD ;
				break ;
			}
			break ;
		case GameModes.GAME_MODE_SP_RETRO_C:
			switch( difficulty ) {
			case GameInformation.DIFFICULTY_NORMAL:
				index = LEADERBOARD_INDEX_SP_RETRO_C_NORMAL ;
				break ;
			case GameInformation.DIFFICULTY_HARD:
				index = LEADERBOARD_INDEX_SP_RETRO_C_HARD ;
				break ;
			}
			break ;
		}
		
		if ( index >= 0 )
			return LEADERBOARD_IDS[index] ;
		return null ; 
	}
	
	
	public static final String getLeaderboardID( GameResult gr ) {
		return getLeaderboardID( gr.getGameSettings(0) ) ;
	}
	
	
	/**
	 * Returns whether the provided game mode, to the best of our knowledge,
	 * supports the online leaderboards.
	 * 
	 * Errs on the side of acceptance ('true').  Obviously we can't know
	 * for certain until we have a GameResult object, and even an otherwise
	 * acceptable game mode can be rendered incompatible with the leaderboards
	 * under certain settings.
	 * 
	 * @param gameMode
	 * @return
	 */
	public static final boolean isLeaderboardSupported( int gameMode ) {
		return getLeaderboardID(gameMode) != null ;
	}
	
	
	
	/**
	 * Returns whether the provided GameSettings object, to the best
	 * of our knowledge, would allow us to submit to the leaderboards.
	 * 
	 * Errs on the side of acceptance ('true').  Obviously we can't know
	 * for certain whether a score can be submitted until a GameResult
	 * object is available.
	 * 
	 * @param gs
	 * @return
	 */
	public static final boolean isLeaderboardSupported( GameSettings gs ) {
		if ( getLeaderboardID(gs) == null )
			return false ;
		// make sure clears/level are set to default (10)
		if ( gs.hasClearsPerLevel() && gs.getClearsPerLevel() != 10 )
			return false ;
		// Make sure we are not on "practice" mode.
		if ( gs.hasDifficulty() && gs.getDifficulty() == GameInformation.DIFFICULTY_PRACTICE )
			return false ;
		
		// Otherwise, this is okay.
		return true ;
	}
	
	
	public static final boolean isLeaderboardSupported( GameResult gr ) {
		try {
			getRawScore( gr ) ;
			return true ;
		} catch( Exception e ) {
			return false ;
		}
	}
	
	
	/**
	 * Provided with a game result, returns a raw score.
	 * @param gr
	 * @return
	 */
	public static final long getRawScore( GameResult gr ) {
		if ( gr == null )
			throw new NullPointerException("Provided GameResult is null.") ;
		if ( !gr.getTerminated() )
			throw new IllegalStateException("Provided GameResult is not terminated.") ;
		if ( gr.getNumberOfPlayers() != 1 )
			throw new IllegalArgumentException("Provided GameResult is not 1-player.") ;
		
		GameSettings gs = gr.getGameSettings(0) ;
		if ( !Scores.isLeaderboardSupported(gs) )
			throw new IllegalArgumentException("Provided GameResult settings do not support a leaderboard.") ;
		
		int measurePerformance = GameModes.measurePerformanceBy(gs.getMode()) ;
		switch( measurePerformance ) {
		case GameModes.MEASURE_PERFORMANCE_BY_SCORE:
			// use player score
			return gr.getGameInformation(0).score ;
		case GameModes.MEASURE_PERFORMANCE_BY_TIME:
			// use time: milliseconds.
			return gr.getGameInformation(0).milliseconds ;
		}
		
		throw new IllegalArgumentException("Don't know what to do with this GameResult.") ;
	}
	
	
}
