package com.peaceray.quantro.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;

import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.R;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QCombinations;

import android.content.Context;
import android.content.res.Resources;


/**
 * A static-access class with an inner Singleton instance.
 * 
 * Tracks the accumulation of Achievements over time.  Static
 * methods report App events that are relevant to Achievement
 * progress.  This class also tracks the amount of time since
 * the last Achievement push, and whether new information has
 * happened since then.
 * 
 * Achievement progress is loaded in init() and saved with
 * every push.  Pushes save either before (if not signed in)
 * or after the push.
 * 
 * Pushes require a GameClient, passed in as an argument, and
 * a Context (for local saves / loads).
 * 
 * @author Jake
 *
 */
public class Achievements {
	
	@SuppressWarnings("unused")
	private static final String TAG = "Achievements" ;
	// TESTING: to test Achievements, use a find-replace for
	// 	find 	: "//Log.d"
	//  replace : "Log.d"
	// to un-comment the Log lines.
	
	// ACHIEVEMENTS
	private enum Name {
		/**
		 * Get 50 clears in one game on Normal Difficulty.
		 * 
		 * Easy.
		 */
		AUSPICIOUS_BEGINNINGS,
		
		/**
		 * Get 50 clears in one game on Hard Difficulty.
		 * 
		 * Normal.
		 */
		BIG_LEAGUES,
		
		/**
		 * Get 10 clears simultaneously in fg and bg in one game.
		 * 
		 * Normal.
		 */
		TECHNICOLOR,
		
		/**
		 * Use all 4 special pieces successfully in one game.
		 * 
		 * Normal.
		 */
		ADVANCED_TECHNIQUES,
		
		/**
		 * Use all 4 special pieces unsuccessfully in one game.
		 * 
		 * HIDDEN.  Easy.
		 */
		LESS_ADVANCED_TECHNIQUES,
		
		/**
		 * Get a x20 multiplier.
		 * 
		 * Normal.
		 */
		SCORE_SCORE,
		
		/**
		 * Get a x20 multiplier on level 20.
		 * 
		 * Hard.
		 */
		TWENTY_CUBED,
		
		/**
		 * Get 20 clears with a single piece.
		 * 
		 * Hard.
		 */
		SUPERCRITICAL,
		
		/**
		 * Get 1,000,000 points with a single piece.
		 * 
		 * Hard.
		 */
		JACKPOT,
		
		/**
		 * Reach level 21 on Endurance.
		 * 
		 * Normal.
		 */
		BLACKJACK,
		
		/**
		 * 3 Minutes on Normal Flood.
		 * 
		 * Easy.
		 */
		FLOOD_RESISTANT,
		
		/**
		 * 3 Minutes on Hard Flood.
		 * 
		 * Normal.
		 */
		FLOOD_PROOF,
		
		/**
		 * Try every SP game mode.
		 * 
		 * Easy.
		 */
		SOLO_ARTIST,
		
		/**
		 * Try every 2-player game mode.
		 * 
		 * Easy.
		 */
		DUELIST,
		
		/**
		 * Play a custom game mode.
		 * 
		 * HIDDEN.  Easy.
		 */
		I_KNOW_WHAT_I_LIKE,
		
		/**
		 * Get 1,000 clears.
		 * 
		 * Easy.  Incremental.
		 */
		DEDICATED,
		
		/**
		 * Get 10,000 clears.
		 * 
		 * Normal.  Incremental.
		 */
		OBSESSED,
		
		/**
		 * Get 100,000 clears.
		 * 
		 * Hard.  Incremental.
		 */
		SERIOUSLY,
		
		
		/**
		 * Earn a billion points, total.
		 * 
		 * Hard.
		 */
		THE_HIGHEST_OF_ROLLERS,
		
		
		/**
		 * Win an MP game.
		 * 
		 * Normal.
		 */
		GLORIOUS_VICTORY,
		
		/**
		 * Lose an MP game.
		 * 
		 * Easy.
		 */
		A_LEARNING_EXPERIENCE,
		
		/**
		 * Opponent quits an MP game.
		 * 
		 * HIDDEN.  Easy.
		 */
		CONSOLATION_PRIZE,
		
		/**
		 * Open a Public Internet Lobby and host for 30 minutes.
		 * 
		 * Normal.
		 */
		OPEN_HOUSE,
		
		/**
		 * Accept an invitation and join a Private lobby.
		 * 
		 * Normal.
		 */
		GUEST_OF_HONOR,
		
		/**
		 * Take over hosting a roaming lobby and host it for 30 minutes.
		 * 
		 * Normal.
		 */
		HOT_POTATO,
		
		/**
		 * Play an Internet MP game to completion.
		 * 
		 * Normal.
		 */
		THINK_GLOBAL,
		
		/**
		 * Play a WiFi MP game to completion.
		 * 
		 * Normal.
		 */
		ACT_LOCAL,
		
		/**
		 * Pause the game, come back after an hour.
		 * 
		 * HIDDEN.  Easy.
		 */
		EPIC_PROCRASTINATOR
	}
	
	private static final int NUM_ACHIEVEMENTS = Name.values().length ;
	
	
	private static final String [] ACHIEVEMENT_ID = new String[NUM_ACHIEVEMENTS] ;
	
	
	private static final long PUSH_RATE_LIMIT = 120 * 1000 ;		// every 2 minutes.
	private static final long PULL_RATE_LIMIT = 180 * 1000 ;		// no more than once every 3 minutes
	
	private static final long SECONDS_TO_TRY_GAME_MODE = 110 ;		// 1 minute, 50 seconds.
	
	private static final String LOCAL_STORAGE_DIR = "Player" ;
	private static final String LOCAL_STORAGE = "achievements.txt" ;
	private static final String LOCAL_STORAGE_MUTABLE = "achievements_mut.txt" ;
	
	private static final String CHARSET = "UTF-8" ;
	
	
	private static Achievements mInstance = null ;
	
 
	////////////////////////////////////////////////////////////////////////////
	//
	// STATIC ACCESS
	//
	
	synchronized public static void init( Context context ) {
		if ( mInstance != null ) {
			throw new IllegalStateException("Only call 'init' once.") ;
		}
		
		// /////////////////////////////////////////////////////////////////////
		// LOAD ACHIEVEMENT IDs FROM RESOURCES
		Resources res = context.getResources() ;

		Name[] names = Name.values();
		for (int i = 0; i < names.length; i++) {
			// TODO populate with real achievement IDs to support uploads
			ACHIEVEMENT_ID[i] = names[i].toString();
		}
		
		// /////////////////////////////////////////////////////////////////////
		// SANITY CHECK:
		// Check there are no 'null's and no repeats.
		for ( int i = 0; i < NUM_ACHIEVEMENTS; i++ ) {
			if ( ACHIEVEMENT_ID[i] == null ) {
				throw new IllegalStateException("Missing the achievement ID for " + Name.values()[i]) ;
			}
			for ( int j = i+1; j < NUM_ACHIEVEMENTS; j++ ) {
				if ( ACHIEVEMENT_ID[i].equals( ACHIEVEMENT_ID[j] ) ) {
					throw new IllegalStateException("Achievement ID for " + Name.values()[i] + " the same as " + Name.values()[j]) ;
				}
			}
		}
		
		
		// Create our instance
		mInstance = new Achievements(context) ;
		// this should have loaded from file and everything.  We're done.
	}
	
	
	private static void throwIfNotInit() {
		if ( mInstance == null ) {
			throw new IllegalStateException("'init' was never called.") ;
		}
	}
	
	//
	/// SETUP AND CHECKS ///////////////////////////////////////////////////////
	//
	//
	
	/**
	 * Pulls achievements from Google Play if possible and appropriate.
	 * 
	 * By default we only pull if we haven't successfully pulled before.
	 * Set 'force' to 'true' to require a pull even if we've already done
	 * it.
	 * 
	 * This is a safe method call 
	 */
	synchronized public static void pull( QuantroApplication qa, boolean force ) {
		throwIfNotInit() ;
		
		//Log.d(TAG, "pull.  force " + force) ;
		if ( force || mInstance.mLastPull == 0
				|| System.currentTimeMillis() - mInstance.mLastPull > PULL_RATE_LIMIT ) {
			mInstance.pullAchievements(qa, force) ;
		}
	}
	
	
	/**
	 * Pushes achievements to Google Play if possible and appropriate.
	 * 
	 * This method can be safely called on a regular schedule.  If there
	 * are no achievements to push, this method has no effect.
	 * 
	 * If 'ignoreRateLimiting' is true, we perform an immediate push
	 * (if possible).
	 * 
	 * Our policy otherwise: if we have any achievements ready to unlock,
	 * we perform the unlock immediately.  We don't want to rate-limit an
	 * actual achievement; they happen extremely infrequently.
	 * 
	 * On the other hand, if it's only incremental stuff, we obey rate limiting.
	 * Currently our rate limit is once every 2 minutes.  You can provide your own rate 
	 * limiting by reducing the frequency of calls to this method to below once
	 * a minute (maybe every level-up, pause, 5 minutes, etc.).
	 * 
	 * @param qa
	 */
	synchronized public static void push( QuantroApplication qa, boolean ignoreRateLimiting ) {
		throwIfNotInit() ;
		
		boolean signedIn = qa.gpg_isSignedIn() ;
		// determine if it's appropriate to push.
		boolean pushWouldUnlock = false, pushWouldIncrement = false ;
		if ( signedIn ) {
			pushWouldUnlock = mInstance.readyToPushUnlock() ;
			pushWouldIncrement = mInstance.readyToPushIncrement() ;
		} else {
			pushWouldUnlock = mInstance.timeSpentInNeedOfPush(false) > 0 ;
			pushWouldIncrement = mInstance.readyToStoreIncrement() ;
		}
		
		//Log.d(TAG, "push.  signed in " + signedIn + " wouldUnlock " + pushWouldUnlock + " wouldIncrement " + pushWouldIncrement + " time since last " + mInstance.timeSinceLastPush(signedIn)) ;
		if ( pushWouldUnlock || (pushWouldIncrement
				&& (ignoreRateLimiting || mInstance.timeSinceLastPush(signedIn) > PUSH_RATE_LIMIT ) ) ) {
			
			// ready to push.
			mInstance.pushAchievements(qa) ;
			
		}
	}
	
	
	//
	/// PLAYER ACTION UPDATES //////////////////////////////////////////////////
	//
	//
	
	
	/**
	 * A new game is starting up.
	 */
	synchronized public static void game_new() {
		throwIfNotInit() ;
		//Log.d(TAG, "game_new") ;
		mInstance.gameNew() ;
	}
	
	/**
	 * The game has applied a full synchronization.
	 */
	synchronized public static void game_fullSynchronization() {
		throwIfNotInit() ;
		//Log.d(TAG, "game_fullSynchronization") ;
		mInstance.gameDidFullSynchronization() ;
	}
	
	/**
	 * The game has loaded save data.
	 */
	synchronized public static void game_load() {
		throwIfNotInit() ;
		//Log.d(TAG, "game_load") ;
		mInstance.gameDidFullSynchronization() ;
	}
	
	
	/**
	 * Gameplay has stopped.  It might still be resumed.  For example, the
	 * user has paused.
	 */
	synchronized public static void game_stop() {
		throwIfNotInit() ;
		//Log.d(TAG, "game_stop") ;
		mInstance.gameDidStop() ;
	}
	
	/**
	 * Gameplay has started.  If previously stopped, we unpaused or whatever.
	 */
	synchronized public static void game_go() {
		throwIfNotInit() ;
		//Log.d(TAG, "game_go") ;
		mInstance.gameDidGo() ;
	}
	
	/**
	 * A new piece has just entered for the local player's game (the one
	 * earning achievements).
	 * @param pieceType
	 * @param ginfo
	 */
	synchronized public static void game_pieceEnteredForPlayer( int pieceType, GameInformation ginfo ) {
		throwIfNotInit() ;
		//Log.d(TAG, "game_pieceEntered") ;
		mInstance.gamePieceDidEnterForPlayer(pieceType, ginfo) ;
	}
	
	/**
	 * The most recently entered piece was successful for the local player
	 * (the one earning achievements).
	 */
	synchronized public static void game_pieceSuccessfulForPlayer() {
		throwIfNotInit() ;
		//Log.d(TAG, "game_pieceSuccessful") ;
		mInstance.gamePieceWasSuccessfulForPlayer() ;
	}
	
	/**
	 * This game just ended.  Called in ExamineGameResult.
	 * @param gr
	 * @param localPlayer
	 */
	synchronized public static void game_over( GameResult gr, int localPlayer ) {
		throwIfNotInit() ;
		//Log.d(TAG, "game_over") ;
		mInstance.gameOver(gr, localPlayer) ;
	}
	
	
	/**
	 * Just started hosting a WiFi lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_hostWiFi( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_hostWiFi") ;
		mInstance.lobbyHost(lobby, LobbyType.WIFI, true) ;
	}
	
	/**
	 * Just started hosting a Public Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_hostPublicInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_hostPublicInternet") ;
		mInstance.lobbyHost(lobby, LobbyType.PUBLIC_INTERNET, true) ;
	}
	
	/**
	 * Just started hosting a Private Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_hostPrivateInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_hostPrivateInternet") ;
		mInstance.lobbyHost(lobby, LobbyType.PRIVATE_INTERNET, true) ;
	}
	
	/**
	 * Just started hosting a Private Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_hostRoamingInternet( Lobby lobby, boolean firstHost ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_hostRoamingInternet.  firstHost " + firstHost) ;
		mInstance.lobbyHost(lobby, LobbyType.ROAMING_INTERNET, firstHost) ;
	}
	
	
	/**
	 * Just joined a WiFi lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_joinWiFi( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_joinWiFi") ;
		mInstance.lobbyJoin(lobby, LobbyType.WIFI) ;
	}
	
	/**
	 * Just joined a Public Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_joinPublicInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_joinPublicInternet") ;
		mInstance.lobbyJoin(lobby, LobbyType.PUBLIC_INTERNET) ;
	}
	
	/**
	 * Just joined a Private Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_joinPrivateInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_joinPrivateInternet") ;
		mInstance.lobbyJoin(lobby, LobbyType.PRIVATE_INTERNET) ;
	}
	
	/**
	 * Just joined a RoamingInternet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_joinRoamingInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_joinRoamingInternet") ;
		mInstance.lobbyJoin(lobby, LobbyType.ROAMING_INTERNET) ;
	}
	
	
	/**
	 * Just updated a WiFi lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_updateWiFi( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_updateWiFi") ;
		mInstance.lobbyUpdate(lobby, LobbyType.WIFI) ;
	}
	
	/**
	 * Just updated a Public Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_updatePublicInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_updatePublicInternet") ;
		mInstance.lobbyUpdate(lobby, LobbyType.PUBLIC_INTERNET) ;
	}
	
	/**
	 * Just updated a Private Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_updatePrivateInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_updatePrivateInternet") ;
		mInstance.lobbyUpdate(lobby, LobbyType.PRIVATE_INTERNET) ;
	}
	
	/**
	 * Just updated a RoamingInternet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_updateRoamingInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_updateRoamingInternet") ;
		mInstance.lobbyUpdate(lobby, LobbyType.ROAMING_INTERNET) ;
	}
	
	
	/**
	 * A WiFi game has just ended.
	 * 
	 * @param lobby
	 * @param gr
	 * @param localPlayer The local player's slot into GameResult (NOT into the lobby itself!)
	 */
	synchronized public static void lobby_gameOverWiFi( Lobby lobby, GameResult gr, int localPlayer ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_gameOverWiFi") ;
		mInstance.lobbyGameOver(lobby, LobbyType.WIFI, gr, localPlayer) ;
	}
	
	/**
	 * A Public Internet game has just ended.
	 * 
	 * @param lobby
	 * @param gr
	 * @param localPlayer The local player's slot into GameResult (NOT into the lobby itself!)
	 */
	synchronized public static void lobby_gameOverPublicInternet( Lobby lobby, GameResult gr, int localPlayer ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_gameOverPublicInternet") ;
		mInstance.lobbyGameOver(lobby, LobbyType.PUBLIC_INTERNET, gr, localPlayer) ;
	}
	
	/**
	 * A Private Internet game has just ended.
	 * 
	 * @param lobby
	 * @param gr
	 * @param localPlayer The local player's slot into GameResult (NOT into the lobby itself!)
	 */
	synchronized public static void lobby_gameOverPrivateInternet( Lobby lobby, GameResult gr, int localPlayer ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_gameOverPrivateInternet") ;
		mInstance.lobbyGameOver(lobby, LobbyType.PRIVATE_INTERNET, gr, localPlayer) ;
	}
	
	/**
	 * A Roaming Internet game has just ended.
	 * 
	 * @param lobby
	 * @param gr
	 * @param localPlayer The local player's slot into GameResult (NOT into the lobby itself!)
	 */
	synchronized public static void lobby_gameOverRoamingInternet( Lobby lobby, GameResult gr, int localPlayer ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_gameOverRoamingInternet") ;
		mInstance.lobbyGameOver(lobby, LobbyType.ROAMING_INTERNET, gr, localPlayer) ;
	}
	
	
	/**
	 * Just closed a WiFi lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_closeWiFi( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_closeWiFi") ;
		mInstance.lobbyClose(lobby, LobbyType.WIFI) ;
	}
	
	/**
	 * Just closed a Public Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_closePublicInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_closePublicInternet") ;
		mInstance.lobbyClose(lobby, LobbyType.PUBLIC_INTERNET) ;
	}
	
	/**
	 * Just closed a Private Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_closePrivateInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_closePrivateInternet") ;
		mInstance.lobbyClose(lobby, LobbyType.PRIVATE_INTERNET) ;
	}
	
	/**
	 * Just closed a RoamingInternet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_closeRoamingInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_closeRoamingInternet") ;
		mInstance.lobbyClose(lobby, LobbyType.ROAMING_INTERNET) ;
	}
	
	
	/**
	 * Just leaved a WiFi lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_leaveWiFi( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_leaveWiFi") ;
		mInstance.lobbyLeave(lobby, LobbyType.WIFI) ;
	}
	
	/**
	 * Just leaved a Public Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_leavePublicInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_leavePublicInternet") ;
		mInstance.lobbyLeave(lobby, LobbyType.PUBLIC_INTERNET) ;
	}
	
	/**
	 * Just leaved a Private Internet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_leavePrivateInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_leavePrivateInternet") ;
		mInstance.lobbyLeave(lobby, LobbyType.PRIVATE_INTERNET) ;
	}
	
	/**
	 * Just leaved a RoamingInternet lobby.
	 * @param lobby
	 */
	synchronized public static void lobby_leaveRoamingInternet( Lobby lobby ) {
		throwIfNotInit() ;
		//Log.d(TAG, "lobby_leaveRoamingInternet") ;
		mInstance.lobbyLeave(lobby, LobbyType.ROAMING_INTERNET) ;
	}
	
	
	
	
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INSTANCE METHODS
	//
	// 
	
	
	/**
	 * When (as System.currentTimeMilliseconds()) did we last attempt a push?
	 */
	private long mLastPushAttempt ;
	
	/**
	 * When (as System.currentTimeMilliseconds()) did we last successfully push,
	 * i.e., push while the game client was connected?
	 */
	private long mLastPushSuccess ;
	
	/**
	 * When (as System.currentTimeMilliseconds()) did we last change
	 * our local achievement data?  If > LastPush----, it's time to push.
	 */
	private long mLastChange ;
	
	
	/**
	 * When (as System.currentTimeMilliseconds()) did we first change
	 * our local achievement data since the last push attempt?  If 0,
	 * this hasn't happened yet.
	 */
	private long mFirstChangeSincePushAttempt ;
	
	/**
	 * When (as System.currentTimeMilliseconds()) did we first change
	 * our local achievement data since the last push success?  If 0,
	 * this hasn't happened yet.
	 */
	private long mFirstChangeSincePushSuccess ;
	
	
	private long mLastPull ;
	
	
	/**
	 * We don't bother keeping track of "pushed" achievements -- that's what
	 * Games Services are for.  However, we DO remember having successfully
	 * pushed an "unlock" for a given achievement with this instance (not saved
	 * to long-term storage).  This prevents us from continuously pushing
	 * "unlock" messages every tick for things that are "perma-stateful" such
	 * as using special pieces or clearing rows within a single game.
	 * 
	 * Some methods might cause Achievements to load data from Google Play
	 * to populate this array, but we don't do that automatically.
	 * 
	 * If a values is 'true', we don't bother unlocking or incrementing the
	 * corresponding achievement.
	 */
	private boolean [] mDidUnlock ;
	
	
	/**
	 * For incremental achievements, this is the number of steps according to the
	 * most recent 'pull.'  Non-incremental achievements and unset achievements
	 * will have appropriate placeholder values.
	 */
	private int [] mNumSteps ;
	private static final int NUM_STEPS_UNPULLED = -1 ;
	private static final int NUM_STEPS_NON_INCREMENTAL = -2 ;
	
	
	/**
	 * We've met the requirements for an unlock, but haven't pushed it yet.
	 * This is only appropriate for non-incremental achievements.
	 * 
	 * Achievements which can be determined at a single time-slice -- for 
	 * example, 50 clears in a single game -- don't need to store long-term
	 * information and can instead by 
	 */
	private boolean [] mHasUnlockToPush ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// DETAILED INFORMATION FOR SPECIFIC ACHIEVEMENTS
	// Total Rows Cleared
	private int mRowsClearedSincePush ;				// pushed in exact amounts
	private int mRowsClearedSincePushTens ;			// pushed every ten; remainder is kept.
	// local storage
	private int mRowsClearedLastLocalStorage ;
	private int mRowsClearedTensLastLocalStorage ;
	
	////////////////////////////////////////////////////////////////////////////
	// SCORE
	private long mPointsEarnedSincePush100K ;
	private long mPointsEarnedSincePush100KLastLocalStorage ;
	
	// ONGOING GAME INFORMATION
	private GameInformation mGameInformationWhenPieceEntered ;
	private int mGamePieceFalling ;
	private boolean mGamePieceFallingWasSuccessful ;
	
	private long mGameTimeWhenPaused ;
	
	private long mGameTimeWhenOver ;	// we don't push w/in 10 seconds of this
	
	// which special pieces have been successful?
	private boolean mGameSpecialSuccess3D = false ;
	private boolean mGameSpecialSuccessLinks = false ;
	private boolean mGameSpecialSuccessFlash = false ;
	private boolean mGameSpecialSuccessFused = false ;
	
	private boolean mGameSpecialNonSuccess3D = false ;
	private boolean mGameSpecialNonSuccessLinks = false ;
	private boolean mGameSpecialNonSuccessFlash = false ;
	private boolean mGameSpecialNonSuccessFused = false ;
	
	
	// GAME MODES PLAYED INFORMATION
	private boolean [] mPlayedGameModeSinglePlayer ;
	private boolean [] mPlayedGameModeVS ;
	private boolean mPlayedGameModeSinglePlayerChangedSinceLocalStorage ;
	private boolean mPlayedGameModeVSChangedSinceLocalStorage ;
	
	
	private static final int INDEX_MODE_SP_QUANTRO_A = 0 ;
	private static final int INDEX_MODE_SP_QUANTRO_B = 1 ;
	private static final int INDEX_MODE_SP_QUANTRO_C = 2 ;
	private static final int INDEX_MODE_SP_RETRO_A = 3 ;
	private static final int INDEX_MODE_SP_RETRO_B = 4 ;
	private static final int INDEX_MODE_SP_RETRO_C = 5 ;
	// ADD NEW MODES AT THE BOTTOM!
	private static final int NUM_INDEX_MODE_SP = 6 ;
	
	private static int gameModeToSPIndex( int mode ) {
		switch( mode ) {
		case GameModes.GAME_MODE_SP_QUANTRO_A:
			return INDEX_MODE_SP_QUANTRO_A ;
		case GameModes.GAME_MODE_SP_QUANTRO_B:
			return INDEX_MODE_SP_QUANTRO_B ;
		case GameModes.GAME_MODE_SP_QUANTRO_C:
			return INDEX_MODE_SP_QUANTRO_C ;
		case GameModes.GAME_MODE_SP_RETRO_A:
			return INDEX_MODE_SP_RETRO_A ;
		case GameModes.GAME_MODE_SP_RETRO_B:
			return INDEX_MODE_SP_RETRO_B ;
		case GameModes.GAME_MODE_SP_RETRO_C:
			return INDEX_MODE_SP_RETRO_C ;
		}
		return -1 ;
	}
	
	private static final int INDEX_MODE_VS_QUANTRO_A = 0 ;
	private static final int INDEX_MODE_VS_QUANTRO_C = 1 ;
	private static final int INDEX_MODE_VS_QUANTRO_S = 2 ;		// bitter pill
	private static final int INDEX_MODE_VS_RETRO_A = 3 ;
	private static final int INDEX_MODE_VS_RETRO_C = 4 ;
	private static final int INDEX_MODE_VS_RETRO_S = 5 ;		// gravity
	// ADD NEW MODES AT THE BOTTOM!
	private static final int NUM_INDEX_MODE_VS = 6 ;
	
	
	private static int gameModeToVSIndex( int mode ) {
		switch( mode ) {
		case GameModes.GAME_MODE_1V1_QUANTRO_A:
			return INDEX_MODE_VS_QUANTRO_A ;
		case GameModes.GAME_MODE_1V1_QUANTRO_C:
			return INDEX_MODE_VS_QUANTRO_C ;
		case GameModes.GAME_MODE_1V1_QUANTRO_BITTER_PILL:
			return INDEX_MODE_VS_QUANTRO_S ;
		case GameModes.GAME_MODE_1V1_RETRO_A:
			return INDEX_MODE_VS_RETRO_A ;
		case GameModes.GAME_MODE_1V1_RETRO_C:
			return INDEX_MODE_VS_RETRO_C ;
		case GameModes.GAME_MODE_1V1_RETRO_GRAVITY:
			return INDEX_MODE_VS_RETRO_S ;
		}
		return -1 ;
	}
	
	
	private enum LobbyType {
		WIFI,
		
		PUBLIC_INTERNET,
		
		PRIVATE_INTERNET,
		
		ROAMING_INTERNET
	}
	
	
	// This lobby.
	private LobbyType mLobbyType ;
	private boolean mLobbyIsHost ;
	private boolean mLobbyHostIsFirstHost ;
	private long mLobbyTimeEntered ;
	
	
	
	private Achievements(Context ctx) {
		
		// Last Push: has not occurred.
		mLastPushAttempt = 0 ;
		mLastPushSuccess = 0 ;
		
		mLastChange = 0 ;
		mFirstChangeSincePushAttempt = 0 ;
		mFirstChangeSincePushSuccess = 0 ;
		mLastPull = 0 ;
		
		mDidUnlock = new boolean[NUM_ACHIEVEMENTS] ;
		mNumSteps = new int[NUM_ACHIEVEMENTS] ;
		mHasUnlockToPush = new boolean[NUM_ACHIEVEMENTS] ;
		
		for ( int i = 0; i < NUM_ACHIEVEMENTS; i++ ) {
			mDidUnlock[i] = false ;
			mNumSteps[i] = NUM_STEPS_UNPULLED ;
			mHasUnlockToPush[i] = false ;
		}
		
		mRowsClearedSincePush = 0 ;
		mRowsClearedSincePushTens = 0 ;
		
		mRowsClearedLastLocalStorage = 0 ;
		mRowsClearedTensLastLocalStorage = 0 ;

		mPointsEarnedSincePush100K = 0 ;
		mPointsEarnedSincePush100KLastLocalStorage = 0 ;
		
		mGameInformationWhenPieceEntered = null ;
		mGamePieceFalling = -1 ;
		mGamePieceFallingWasSuccessful = false ;
		
		mGameSpecialSuccess3D = false ;
		mGameSpecialSuccessLinks = false ;
		mGameSpecialSuccessFlash = false ;
		mGameSpecialSuccessFused = false ;
		
		mGameSpecialNonSuccess3D = false ;
		mGameSpecialNonSuccessLinks = false ;
		mGameSpecialNonSuccessFlash = false ;
		mGameSpecialNonSuccessFused = false ;
		
		mPlayedGameModeSinglePlayer = new boolean[NUM_INDEX_MODE_SP] ;
		mPlayedGameModeVS = new boolean[NUM_INDEX_MODE_VS] ;
		mPlayedGameModeSinglePlayerChangedSinceLocalStorage = false ;
		mPlayedGameModeVSChangedSinceLocalStorage =  false ;
		// defaults to false
		
		mLobbyType = null ;
		mLobbyIsHost = false ;
		mLobbyHostIsFirstHost = false ;
		mLobbyTimeEntered = 0 ;
		
		// load local.
		loadLocal(ctx) ;
	}
	
	
	private void gameNew() {
		//Log.d(TAG, "gameNew") ;
		mGameInformationWhenPieceEntered = null ;
		mGamePieceFalling = -1 ;
		mGamePieceFallingWasSuccessful = false ;
		
		mGameSpecialSuccess3D = false ;
		mGameSpecialSuccessLinks = false ;
		mGameSpecialSuccessFlash = false ;
		mGameSpecialSuccessFused = false ;
		
		mGameSpecialNonSuccess3D = false ;
		mGameSpecialNonSuccessLinks = false ;
		mGameSpecialNonSuccessFlash = false ;
		mGameSpecialNonSuccessFused = false ;
		
		mGameTimeWhenPaused = 0 ;
	}
	
	private void gameDidFullSynchronization() {
		//Log.d(TAG, "gameDidFullSynchronization") ;
		mGameInformationWhenPieceEntered = null ;
		mGamePieceFalling = -1 ;
		mGamePieceFallingWasSuccessful = false ;
	}
	
	
	/**
	 * Game has paused, been stopped by a network error, etc.
	 */
	private void gameDidStop() {
		//Log.d(TAG, "gameDidStop") ;
		if ( mGameTimeWhenPaused == 0 ) {
			mGameTimeWhenPaused = System.currentTimeMillis() ;
			//Log.d(TAG, "gameDidStop: pausing at " + mGameTimeWhenPaused) ;
		}
	}
	
	/**
	 * Game has ticked forward.
	 * 
	 * This method affects the
	 * 
	 * EPIC_PROCRASTINATOR
	 * 
	 * achievement.
	 */
	private void gameDidGo() {
		//Log.d(TAG, "gameDidGo") ;
		if ( mGameTimeWhenPaused > 0 ) {
			long timePaused = System.currentTimeMillis() - mGameTimeWhenPaused ;
			long minutesPaused = timePaused / (60 * 1000) ;
			//Log.d(TAG, "gameDidGo.  Minutes paused: " + minutesPaused) ;
			// paused for an hour?  EPIC PROCRASTINATOR!
			if ( minutesPaused >= 60 && !mDidUnlock[Name.EPIC_PROCRASTINATOR.ordinal()] ) {
				setReadyToUnlock(Name.EPIC_PROCRASTINATOR) ;
			}
		}
		
		mGameTimeWhenPaused = 0 ;
	}
	
	
	/**
	 * A new piece has entered the game.  The piece type is provided, as well
	 * as the current GameInformation.
	 * 
	 * This game is the one currently being controlled by the player.  Progress
	 * within the GameInformation should be used to advance the player's own 
	 * achievements.
	 * 
	 * This method affects, and is the sole update function for, the following achievements:
	 * 
	 * AUSPICIOUS_BEGINNINGS
	 * BIG_LEAGUES
	 * TECHNICOLOR
	 * SCORE_SCORE
	 * TWENTY_CUBED
	 * SUPERCRITICAL
	 * JACKPOT
	 * BLACKJACK
	 * FLOOD_RESISTANT
	 * FLOOD_PROOF
	 * 
	 * It also affects (but is not the only method that does) these achievements:
	 * 
	 * LESS_ADVANCED_TECHNIQUES
	 * 
	 * and updates "incremental" counters for 
	 * 
	 * DEDICATED
	 * OBSSESSED
	 * SERIOUSLY?
	 * THE_HIGHEST_OF_ROLLERS
	 * 
	 * @param pieceType
	 * @param ginfo
	 */
	private void gamePieceDidEnterForPlayer( int pieceType, GameInformation ginfo ) {
		//Log.d(TAG, "gamePieceDidEnterForPlayer  "+ pieceType) ;
		if ( mGameInformationWhenPieceEntered == null ) {
			//Log.d(TAG, "gamePieceDidEnterForPlayer.  is first piece") ;
			mGameInformationWhenPieceEntered = new GameInformation(ginfo) ;
			mGamePieceFalling = pieceType ;
			mGamePieceFallingWasSuccessful = false ;
			return ;
		}
		
		
		////////////////////////////////////////////////////////////////////////
		// First: note that we have a new piece coming in, so we may have been
		// unsuccessful with the previous piece.
		
		if ( !mGamePieceFallingWasSuccessful ) {
			// check for a special piece type.
			if ( PieceCatalog.isSpecial(mGamePieceFalling) ) {
				// 'flash' pieces count as special
				if ( PieceCatalog.getSpecialCategory(mGamePieceFalling) == PieceCatalog.SPECIAL_CAT_FLASH ) {
					//Log.d(TAG, "gamePieceDidEnterForPlayer.  last was unsuccessful Flash") ;
					mGameSpecialNonSuccessFlash = true ;
				}
			} else if ( PieceCatalog.isTetromino(mGamePieceFalling) ) {
				int qc = PieceCatalog.getQCombination(mGamePieceFalling) ;
				// could be 3d or links.
				if ( qc == QCombinations.UL ) {
					// links
					//Log.d(TAG, "gamePieceDidEnterForPlayer.  last was unsuccessful Links") ;
					mGameSpecialNonSuccessLinks = true ;
				} else if ( qc == QCombinations.ST || qc == QCombinations.ST_INACTIVE ) {
					// 3d
					//Log.d(TAG, "gamePieceDidEnterForPlayer.  last was unsuccessful 3D") ;
					mGameSpecialNonSuccess3D = true ;
				}
			} else if ( PieceCatalog.isTetracube(mGamePieceFalling) ) {
				int qc = PieceCatalog.getQCombination(mGamePieceFalling) ;
				if ( qc == QCombinations.SL || qc == QCombinations.SL_ACTIVE || qc == QCombinations.SL_INACTIVE ) {
					// fused
					//Log.d(TAG, "gamePieceDidEnterForPlayer.  last was unsuccessful Fused") ;
					mGameSpecialNonSuccessFused = true ;
				}
			}
			
			// check if we have "earned" the less-advanced techniques achievement.
			if ( mGameSpecialNonSuccess3D && mGameSpecialNonSuccessLinks
					&& mGameSpecialNonSuccessFlash && mGameSpecialNonSuccessFused
					&& !mDidUnlock[Name.LESS_ADVANCED_TECHNIQUES.ordinal()] ) {
				setReadyToUnlock(Name.LESS_ADVANCED_TECHNIQUES) ;
			}
		}
		
		
		
		////////////////////////////////////////////////////////////////////////
		// Second: compare the difference between the previous GameInformation
		// and the new one.  This should let us count clears for our incremental
		// clear progress, and set unlocks for those which require "with a single piece"
		// criteria.
		
		int numClearsSinceLastPiece ;
		if ( GameModes.numberQPanes(ginfo) == 1 ) {
			numClearsSinceLastPiece =
				(ginfo.moclears + ginfo.sLclears + ginfo.s0clears + ginfo.s1clears) 
					- (mGameInformationWhenPieceEntered.moclears 
							+ mGameInformationWhenPieceEntered.sLclears
							+ mGameInformationWhenPieceEntered.s0clears
							+ mGameInformationWhenPieceEntered.s1clears) ;
		} else {
			numClearsSinceLastPiece =
				2*(ginfo.moclears + ginfo.sLclears) + (ginfo.s0clears + ginfo.s1clears)
				- 2*(mGameInformationWhenPieceEntered.moclears 
						+ mGameInformationWhenPieceEntered.sLclears)
				- (mGameInformationWhenPieceEntered.s0clears
						+ mGameInformationWhenPieceEntered.s1clears) ;
		}
		long numPointsSinceLastPiece = ginfo.score - mGameInformationWhenPieceEntered.score ;
		
		//Log.d(TAG, "gamePieceDidEnterForPlayer. clearsSinceLast " + numClearsSinceLastPiece + " points since last " + numPointsSinceLastPiece) ;
		
		// update unlockable Achievements based on "last piece" stuff.
		// Supercritical: 20 clears for a single piece.
		if ( numClearsSinceLastPiece >= 20 && !mDidUnlock[Name.SUPERCRITICAL.ordinal()]) {
			setReadyToUnlock(Name.SUPERCRITICAL) ;
		}
		
		// Jackpot: earn 1,000,000 points with a single piece.
		if ( numPointsSinceLastPiece >= 1000000L && !mDidUnlock[Name.JACKPOT.ordinal()] ) {
			setReadyToUnlock(Name.JACKPOT) ;
		}
		
		// Increment our un-pushed totals for incremental achievements.
		mRowsClearedSincePush += numClearsSinceLastPiece ;
		mRowsClearedSincePushTens += numClearsSinceLastPiece ;
		
		
		// Increment the total points earned.
		mPointsEarnedSincePush100K += numPointsSinceLastPiece ;
		
		
		////////////////////////////////////////////////////////////////////////
		// Third: update our current GameInformation and falling piece.
		mGameInformationWhenPieceEntered.takeVals(ginfo) ;
		mGamePieceFalling = pieceType ;
		mGamePieceFallingWasSuccessful = false ;
		
		////////////////////////////////////////////////////////////////////////
		// Fourth: check all achievements which don't require before/after comparison.
		// For example, score "in a game," multiplier ceilings, total clears in a game,
		// etc.
		
		int totalClears ;
		int bothPaneClears ;
		if ( GameModes.numberQPanes(ginfo) == 1 ) {
			totalClears =
				(ginfo.moclears + ginfo.sLclears + ginfo.s0clears + ginfo.s1clears) ;
			bothPaneClears = 0 ;
		} else {
			totalClears =
				2*(ginfo.moclears + ginfo.sLclears) + (ginfo.s0clears + ginfo.s1clears) ;
			bothPaneClears = ginfo.moclears + ginfo.sLclears ;
		}
		
		long gameSeconds = ginfo.milliseconds / 1000 ;
		
		//Log.d(TAG, "gamePieceDidEnterForPlayer. total clears " + totalClears + " gameSeconds " + gameSeconds + " game score " + ginfo.score) ;
		
		// Auspicious Beginnings: 50 clears on Normal.
		if ( totalClears >= 50 && ginfo.difficulty >= GameInformation.DIFFICULTY_NORMAL
				&& !mDidUnlock[Name.AUSPICIOUS_BEGINNINGS.ordinal()] ) {
			setReadyToUnlock(Name.AUSPICIOUS_BEGINNINGS) ;
		}
		
		// Big Leagues: 50 clears on Hard.
		if ( totalClears >= 50 && ginfo.difficulty >= GameInformation.DIFFICULTY_HARD
				&& !mDidUnlock[Name.BIG_LEAGUES.ordinal()] ) {
			setReadyToUnlock(Name.BIG_LEAGUES) ;
		}
		
		// Technicolor: 10 FG / BG clears.
		if ( bothPaneClears >= 10 && !mDidUnlock[Name.TECHNICOLOR.ordinal()] ) {
			setReadyToUnlock(Name.TECHNICOLOR) ;
		}
		
		// Score-Score: a multiplier of at least 20.
		if ( ginfo.multiplier >= 20 && !mDidUnlock[Name.SCORE_SCORE.ordinal()] ) {
			setReadyToUnlock(Name.SCORE_SCORE) ;
		}
		
		// 20^3: a multiplier of at least 20 on at least level 20.
		if ( ginfo.multiplier >= 20 && ginfo.level >= 20 && !mDidUnlock[Name.TWENTY_CUBED.ordinal()] ) {
			setReadyToUnlock(Name.TWENTY_CUBED) ;
		}
		
		// BlackJack: level 21 (or higher?), endurance mode.
		if ( ginfo.level >= 21 && GameModes.classCode(ginfo.mode) == GameModes.CLASS_CODE_ENDURANCE
				 && !mDidUnlock[Name.BLACKJACK.ordinal()] ) {
			setReadyToUnlock(Name.BLACKJACK) ;
		}
		
		// Flood Resistant: last at least 3 minutes on Flood Normal.
		if ( gameSeconds >= 180 && ginfo.difficulty >= GameInformation.DIFFICULTY_NORMAL
				&& GameModes.classCode(ginfo.mode) == GameModes.CLASS_CODE_FLOOD
				&& !mDidUnlock[Name.FLOOD_RESISTANT.ordinal()] ) {
			setReadyToUnlock(Name.FLOOD_RESISTANT) ;
		}
		
		// Flood Proof: last at least 3 minutes on Flood Hard.
		if ( gameSeconds >= 180 && ginfo.difficulty >= GameInformation.DIFFICULTY_HARD
				&& GameModes.classCode(ginfo.mode) == GameModes.CLASS_CODE_FLOOD
				&& !mDidUnlock[Name.FLOOD_PROOF.ordinal()] ) {
			setReadyToUnlock(Name.FLOOD_PROOF) ;
		}
		
		// that's it.
	}
	
	
	/**
	 * Informs our Achievements system that the currently falling piece,
	 * controlled by the local player, was used successfully.
	 * 
	 * This method affects and can set the
	 * 
	 * ADVANCED_TECHNIQUES
     *
     * achievement.  It can also affect the
     * 
     * LESS_ADVANCED_TECHNIQUES
     * 
     * achievement, but does not activate it (rather, it prevents its activation
     * by a later method call).
	 */
	private void gamePieceWasSuccessfulForPlayer() {
		//Log.d(TAG, "gamePieceWasSuccessfulForPlayer") ;
		mGamePieceFallingWasSuccessful = true ;
		// look for special piece, set the appropriate "success" variable
		// for specials.
		if ( PieceCatalog.isSpecial(mGamePieceFalling) ) {
			// 'flash' pieces count as special
			if ( PieceCatalog.getSpecialCategory(mGamePieceFalling) == PieceCatalog.SPECIAL_CAT_FLASH ) {
				//Log.d(TAG, "gamePieceWasSuccessfulForPlayer.  last was successful Flash") ;
				mGameSpecialSuccessFlash = true ;
			}
		} else if ( PieceCatalog.isTetromino(mGamePieceFalling) ) {
			int qc = PieceCatalog.getQCombination(mGamePieceFalling) ;
			// could be 3d or links.
			if ( qc == QCombinations.UL ) {
				// links
				//Log.d(TAG, "gamePieceWasSuccessfulForPlayer.  last was successful Links") ;
				mGameSpecialSuccessLinks = true ;
			} else if ( qc == QCombinations.ST || qc == QCombinations.ST_INACTIVE ) {
				// 3d
				//Log.d(TAG, "gamePieceWasSuccessfulForPlayer.  last was successful 3D") ;
				mGameSpecialSuccess3D = true ;
			}
		} else if ( PieceCatalog.isTetracube(mGamePieceFalling) ) {
			int qc = PieceCatalog.getQCombination(mGamePieceFalling) ;
			if ( qc == QCombinations.SL || qc == QCombinations.SL_ACTIVE || qc == QCombinations.SL_INACTIVE ) {
				// fused
				//Log.d(TAG, "gamePieceWasSuccessfulForPlayer.  last was successful Fused") ;
				mGameSpecialSuccessFused = true ;
			}
		}
		
		// check if we have earned the advanced techniques achievement.
		if ( mGameSpecialSuccess3D && mGameSpecialSuccessLinks
				&& mGameSpecialSuccessFlash && mGameSpecialSuccessFused
				&& !mDidUnlock[Name.ADVANCED_TECHNIQUES.ordinal()] ) {
			setReadyToUnlock(Name.ADVANCED_TECHNIQUES) ;
		}
	}
	
	
	/**
	 * A game has ended.  The result is provided.  If 'localPlayer' is >= 0,
	 * it is the index of this player within the result.  Otherwise the player
	 * did not participate.
	 * 
	 * This method affects, and is the sole unlocker of, the following achievements:
	 * 
	 * SOLO_ARTIST
	 * DUELIST
	 * I_KNOW_WHAT_I_LIKE
	 * GLORIOUS_VICTORY
	 * A_LEARNING_EXPERIENCE
	 * CONSOLATION_PRIZE
	 * 
	 * @param gr
	 */
	private void gameOver( GameResult gr, int localPlayer ) {
		//Log.d(TAG, "gameOver") ;
		if ( localPlayer < 0 ) {
			// add any "observation" related achievements here
			return ;
		}
		
		mGameTimeWhenOver = System.currentTimeMillis() ;
		
		// Check the game result for game-mode and game-completion
		// related achievements.
		
		GameSettings gs = gr.getGameSettingsImmutable(localPlayer) ;
		if ( gs == null )
			return ;
		
		int mode = gs.getMode() ;
		int spIndex = gameModeToSPIndex(mode) ;
		int vsIndex = gameModeToVSIndex(mode) ;
		boolean custom = GameModes.isCustom(mode) ;
		int numPlayers = gr.getNumberOfPlayers() ;
		
		boolean playerWon = gr.getWon(localPlayer) ;
		boolean playerLost = gr.getLost(localPlayer) ;
		boolean playerQuit = gr.getQuit(localPlayer) ;
		boolean anotherQuit = false ;
		long playerSeconds = gr.getGameInformation(localPlayer).milliseconds / 1000 ;
		for ( int i = 0; i < numPlayers; i++ ) {
			if ( i != localPlayer && gr.getQuit(i) )
				anotherQuit = true ;
		}
		
		// Solo Artist: all 1-player game modes.
		if ( spIndex > -1 && (playerWon || playerLost || playerSeconds >= SECONDS_TO_TRY_GAME_MODE ) ) {
			mPlayedGameModeSinglePlayerChangedSinceLocalStorage = 
				mPlayedGameModeSinglePlayerChangedSinceLocalStorage || !mPlayedGameModeSinglePlayer[spIndex] ;
			mPlayedGameModeSinglePlayer[spIndex] = true ;
		}
		
		// Duelist: all 1 vs 1 game modes.
		if ( vsIndex > -1 && (playerWon || playerLost || playerSeconds >= SECONDS_TO_TRY_GAME_MODE ) ) {
			mPlayedGameModeVSChangedSinceLocalStorage = 
				mPlayedGameModeVSChangedSinceLocalStorage || !mPlayedGameModeVS[vsIndex] ;
			mPlayedGameModeVS[vsIndex] = true ;
		}
		
		// I Know What I Like: played a custom game mode.
		if ( custom && (playerWon || playerLost || playerSeconds >= SECONDS_TO_TRY_GAME_MODE ) ) {
			if ( !mDidUnlock[Name.I_KNOW_WHAT_I_LIKE.ordinal()] ) {
				setReadyToUnlock(Name.I_KNOW_WHAT_I_LIKE) ;
			}
		}
		
		
		// Glorious Victory: win a multiplayer game.
		if ( numPlayers > 1 && playerWon && !mDidUnlock[Name.GLORIOUS_VICTORY.ordinal()] ) {
			setReadyToUnlock(Name.GLORIOUS_VICTORY) ;
		}
		
		// Learning experience: lose a multiplayer game.
		if ( numPlayers > 1 && playerLost && !mDidUnlock[Name.A_LEARNING_EXPERIENCE.ordinal()] ) {
			setReadyToUnlock(Name.A_LEARNING_EXPERIENCE) ;
		}
		
		// Consolation prize: vs mode opponent quit.
		if ( numPlayers == 2 && !playerQuit && anotherQuit && !mDidUnlock[Name.CONSOLATION_PRIZE.ordinal()] ) {
			setReadyToUnlock(Name.CONSOLATION_PRIZE) ;
		}
	}
	
	
	/**
	 * Just started hosting this lobby.
	 * @param lobby
	 */
	private void lobbyHost( Lobby lobby, LobbyType lobbyType, boolean hostIsFirstHost ) {
		//Log.d(TAG, "lobbyHost " + lobbyType + "  firstHost " + hostIsFirstHost) ;
		mLobbyType = lobbyType ;
		mLobbyIsHost = true ;
		mLobbyHostIsFirstHost = hostIsFirstHost ;
		mLobbyTimeEntered = System.currentTimeMillis() ;
	}
	
	
	/**
	 * Just joined this lobby.
	 * @param lobby
	 */
	private void lobbyJoin( Lobby lobby, LobbyType lobbyType ) {
		//Log.d(TAG, "lobbyJoin " + lobbyType) ;
		mLobbyType = lobbyType ;
		mLobbyIsHost = false ;
		mLobbyHostIsFirstHost = true ;
		mLobbyTimeEntered = System.currentTimeMillis() ;
		
		// this is all it takes to get the "Guest of Honor" achievement.
		if ( lobbyType == LobbyType.PRIVATE_INTERNET && !mDidUnlock[Name.GUEST_OF_HONOR.ordinal()] ) {
			// invited guest of a private lobby.
			setReadyToUnlock(Name.GUEST_OF_HONOR) ;
		}
	}
	
	
	/**
	 * The user is still in this lobby, which was given in
	 * lobbyHost or lobbyJoin.
	 * @param lobby
	 */
	private void lobbyUpdate( Lobby lobby, LobbyType lobbyType ) {
		//Log.d(TAG, "lobbyUpdate " + lobbyType) ;
		if ( mLobbyType == lobbyType ) {
			long timeInLobby = System.currentTimeMillis() - mLobbyTimeEntered ;
			long minutesInLobby = timeInLobby / (60 * 1000) ;
			
			//Log.d(TAG, "lobbyUpdate.  Minutes in lobby " + minutesInLobby) ;
			
			// Open House: hosted a Public Internet lobby for 30 minutes.
			if ( mLobbyIsHost && minutesInLobby >= 30 && lobbyType == LobbyType.PUBLIC_INTERNET && !mDidUnlock[Name.OPEN_HOUSE.ordinal()] ) {
				// hosted for 30 minutes.
				setReadyToUnlock(Name.OPEN_HOUSE) ;
			}
			
			// Hot Potato: re-hosted a roaming lobby for 30 minutes
			if ( mLobbyIsHost && minutesInLobby >= 30 && lobbyType == LobbyType.ROAMING_INTERNET
					&& !mLobbyHostIsFirstHost
					&& !mDidUnlock[Name.HOT_POTATO.ordinal()] ) {
				// hosted for 30 minutes after re-opening
				setReadyToUnlock(Name.HOT_POTATO) ;
			}
		}
	}
	
	
	/**
	 * Within lobby: a game is over.  This method could presumably be given the
	 * responsibility of examining game results, such as "quits" or "wins."
	 * 
	 * However, we do that in gameOver(), which handles both 1-player and multiplayer
	 * games.
	 * 
	 * This method deals only with those Achievements that require lobby information
	 * in addition to game information:
	 * 
	 * THINK_GLOBAL
	 * ACT_LOCAL
	 * 
	 * @param lobby
	 * @param gr
	 * @param localPlayer
	 */
	private void lobbyGameOver( Lobby lobby, LobbyType lobbyType, GameResult gr, int localPlayer ) {
		//Log.d(TAG, "lobbyGameOver  " + lobbyType) ;
		if ( localPlayer < 0 ) {
			// add any "observation" related achievements here
			return ;
		}
		
		// game is over...
		this.gameOver(gr, localPlayer) ;
		
		boolean playerWon = gr.getWon(localPlayer) ;
		boolean playerLost = gr.getLost(localPlayer) ;
		boolean playerQuit = gr.getQuit(localPlayer) ;
		
		//Log.d(TAG, "won  " + playerWon) ;
		//Log.d(TAG, "lost " + playerLost) ;
		//Log.d(TAG, "quit " + playerQuit) ;
		
		// Think Global: finish an Internet multiplayer game.
		if ( (playerWon || playerLost)
				&& ( lobbyType == LobbyType.PUBLIC_INTERNET
						|| lobbyType == LobbyType.PRIVATE_INTERNET || lobbyType == LobbyType.ROAMING_INTERNET )
				&& !mDidUnlock[Name.THINK_GLOBAL.ordinal()] ) {
			setReadyToUnlock(Name.THINK_GLOBAL) ;
		}
		
		// Act Local: finish a Wifi multiplayer game.
		if ( (playerWon || playerLost)
				&& ( lobbyType == LobbyType.WIFI )
				&& !mDidUnlock[Name.ACT_LOCAL.ordinal()] ) {
			setReadyToUnlock(Name.ACT_LOCAL) ;
		}
	}
	
	
	private void lobbyClose( Lobby lobby, LobbyType lobbyType ) {
		//Log.d(TAG, "lobbyClose " + lobbyType) ;
		// one last try...
		this.lobbyUpdate(lobby, lobbyType) ;
		
		// now close.
		mLobbyType = null ;
		mLobbyIsHost = false ;
		mLobbyHostIsFirstHost = false ;
		mLobbyTimeEntered = 0 ;
	}
	

	private void lobbyLeave( Lobby lobby, LobbyType lobbyType ) {
		//Log.d(TAG, "lobbyLeave " + lobbyType) ;
		// one last try...
		this.lobbyUpdate(lobby, lobbyType) ;
		
		// now close.
		mLobbyType = null ;
		mLobbyIsHost = false ;
		mLobbyHostIsFirstHost = false ;
		mLobbyTimeEntered = 0 ;
	}
	
	
	
	private void setReadyToUnlock( Name name ) {
		//Log.d(TAG, "setReadyToUnlock AchievementUnlocked: " + name) ;
		mHasUnlockToPush[name.ordinal()] = true ;
		mLastChange = System.currentTimeMillis() ;
	}
	
	
	private long timeSpentInNeedOfPush( boolean signedIn ) {
		if ( signedIn ) {
			if ( mLastChange > mLastPushSuccess ) {
				return System.currentTimeMillis() - mLastChange ;
			}
			return 0 ;
		} else {
			if ( mLastChange > mLastPushAttempt ) {
				return System.currentTimeMillis() - mLastChange ;
			}
			return 0 ;
		}
	}
	
	
	private long timeSinceLastPush( boolean signedIn ) {
		if ( signedIn ) {
			return System.currentTimeMillis() - mLastPushSuccess ;
		} else {
			return System.currentTimeMillis() - mLastPushAttempt ;
		}
	}
	
	
	private boolean readyToPushUnlock() {
		// only push an unlock if a game has not ended w/in the past 4 seconds.  Typically 
		// we are switching Activities during this window, so we don't want
		// to put up any Activity notifications.
		if ( mGameTimeWhenOver > 0 && Math.abs(System.currentTimeMillis() - mGameTimeWhenOver) < 1000 * 4 ) {
			return false ;
		}
		for ( int i = 0; i < NUM_ACHIEVEMENTS; i++ ) {
			if ( mHasUnlockToPush[i] && !mDidUnlock[i] ) {
				return true ;
			}
		}
		
		return false ;
	}
	
	
	private boolean readyToPushIncrement() {
		if ( mRowsClearedSincePush > 0
				&& ( !mDidUnlock[Name.DEDICATED.ordinal()]
				                 || !mDidUnlock[Name.OBSESSED.ordinal()] ) ) {
			return true ;
		}
		
		if ( mRowsClearedSincePushTens >= 10 && !mDidUnlock[Name.SERIOUSLY.ordinal()] ) {
			return true ;
		}
		
		if ( mPointsEarnedSincePush100K >= 100000 && !mDidUnlock[Name.THE_HIGHEST_OF_ROLLERS.ordinal()] ) {
			return true ;
		}
		
		if ( mNumSteps[Name.SOLO_ARTIST.ordinal()] >= 0 ) {
			// num steps has been pulled; compare against our local result
			int percent = percent(mPlayedGameModeSinglePlayer) ;
			if ( percent > mNumSteps[Name.SOLO_ARTIST.ordinal()] )
				return true ;
		}
		
		if ( mNumSteps[Name.DUELIST.ordinal()] >= 0 ) {
			// num steps has been pulled; compare against our local result
			int percent = percent(mPlayedGameModeVS) ;
			if ( percent > mNumSteps[Name.DUELIST.ordinal()] )
				return true ;
		}
		
		
		
		return false ;
	}
	
	
	private boolean readyToStoreIncrement() {
		if ( mRowsClearedSincePush > mRowsClearedLastLocalStorage
				&& ( !mDidUnlock[Name.DEDICATED.ordinal()]
				                 || !mDidUnlock[Name.OBSESSED.ordinal()] ) ) {
			return true ;
		}
		
		if ( mRowsClearedSincePushTens > mRowsClearedTensLastLocalStorage
				&& !mDidUnlock[Name.SERIOUSLY.ordinal()] ) {
			return true ;
		}
		
		if ( Math.abs( mPointsEarnedSincePush100K - mPointsEarnedSincePush100KLastLocalStorage ) > 1000
				&& !mDidUnlock[Name.THE_HIGHEST_OF_ROLLERS.ordinal()]) {
			// we require at least a small change.
			return true ;
		}
		
		if ( mPlayedGameModeSinglePlayerChangedSinceLocalStorage )
			return true ;
		
		if ( mPlayedGameModeVSChangedSinceLocalStorage )
			return true ;
		
		return false ;
	}
	
	
	private void pushAchievements( QuantroApplication qa ) {
		//Log.d(TAG, "pushAchievements") ;
		if ( !qa.gpg_isSignedIn() ) {
			//Log.d(TAG, "pushAchievements: not signed in") ;
			// not signed in; we can't push.
			mLastPushAttempt = System.currentTimeMillis() ;
			// save locally instead.
			saveLocal(qa) ;
			return ;
		}
		
		// GENERAL NOTE:
		// We push in reverse order (from bottom to top in our achievement list)
		// because Google seems to place "most recently changed" achievements on top.
		
		// TODO: get a client for pushing updates to the cloud

		// First: send asyncronous unlock messages for all
		// easily checked and "unlocked" achievements.
		for ( int i = NUM_ACHIEVEMENTS-1; i >= 0; i-- ) {
			if ( mHasUnlockToPush[i] && !mDidUnlock[i] ) {
				//Log.d(TAG, "****************************************************") ;
				//Log.d(TAG, "pushAchievements -- unlocking :  " + Name.values()[i]) ;
				//Log.d(TAG, "****************************************************") ;
				// TODO push unlock for ACHIEVEMENT_ID[i]
				mHasUnlockToPush[i] = false ;
				mDidUnlock[i] = true ;
			}
		}
		
		// Second: send an incremental update for all "incremental"
		// achievements that have not been unlocked.  At this point that 
		// means "rows cleared", essentially.
		
		// Seriously: count row clears in sets of ten.  We are going to
		// 100,000, which is above the maximum incremental count (by an
		// order of magnitude) so we count 10 clears as 1.
		if ( !mDidUnlock[Name.SERIOUSLY.ordinal()] && mRowsClearedSincePushTens >= 10 ) {
			// TODO push increment for ACHIEVEMENT_ID[Name.SERIOUSLY.ordinal()], mRowsClearedSincePushTens / 10
		}
		//Log.d(TAG, "pushAchievements: increment rows cleared tens" + (mRowsClearedSincePushTens / 10)) ;
		mRowsClearedSincePushTens %= 10 ;
		
		// Dedicated and Obssessed: count row clears exactly.  They are
		// to 1,000 and 10,000, respectively, and we can fit exact amounts.
		if ( !mDidUnlock[Name.OBSESSED.ordinal()] && mRowsClearedSincePush > 0 ) {
			// TODO push increment for ACHIEVEMENT_ID[Name.OBSESSED.ordinal()], mRowsClearedSincePush
		}
		if ( !mDidUnlock[Name.DEDICATED.ordinal()] && mRowsClearedSincePush > 0 ) {
			// TODO push increment for ACHIEVEMENT_ID[Name.DEDICATED.ordinal()], mRowsClearedSincePush
		}
		//Log.d(TAG, "pushAchievements: increment rows cleared " + mRowsClearedSincePush) ;
		mRowsClearedSincePush = 0 ;
		
		
		
		// SOLO ARTIST and DUELIST: push an increment if 1. we have recieved
		// a 'numSteps' from Google Play, and 2. the number of steps we
		// received is lower than our local total.
		int percentSP = percent( mPlayedGameModeSinglePlayer ) ;
		int percentVS = percent( mPlayedGameModeVS ) ;
		if ( !mDidUnlock[Name.DUELIST.ordinal()] && this.mNumSteps[Name.DUELIST.ordinal()] >= 0
				&& percentVS > mNumSteps[Name.DUELIST.ordinal()] ) {
			// TODO push increment for ACHIEVEMENT_ID[Name.DUELIST.ordinal()], percentVS - mNumSteps[Name.DUELIST.ordinal()]
			mNumSteps[Name.DUELIST.ordinal()] = percentVS ;
		}
		if ( !mDidUnlock[Name.SOLO_ARTIST.ordinal()] && this.mNumSteps[Name.SOLO_ARTIST.ordinal()] >= 0
				&& percentSP > mNumSteps[Name.SOLO_ARTIST.ordinal()] ) {
			// TODO push increment for ACHIEVEMENT_ID[Name.SOLO_ARTIST.ordinal()], percentSP - mNumSteps[Name.SOLO_ARTIST.ordinal()]
			mNumSteps[Name.SOLO_ARTIST.ordinal()] = percentSP ;
		}
		
		
		// HIGHEST OF ROLLERS: we use 10,000 steps for this; it's
		// a billion total points, meaning each step is 
		// 100,000 points.  Calculate the appropriate step value as
		// mPointsEarnedTotal / 100000, rounded down.
		int highRollerSteps = (int)( mPointsEarnedSincePush100K / 100000 ) ;
		if ( !mDidUnlock[Name.THE_HIGHEST_OF_ROLLERS.ordinal()] && highRollerSteps > 0 ) {
			//Log.d(TAG, "pushAchievements: increment highest of rollers by " + highRollerSteps + " 100,000 point steps") ;
			// TODO push increment for ACHIEVEMENT_ID[Name.THE_HIGHEST_OF_ROLLERS.ordinal()], highRollerSteps
		}
		mPointsEarnedSincePush100K %= 100000 ;
		
		// Last: this was a successful push.
		mLastPushAttempt = mLastPushSuccess = System.currentTimeMillis() ;
		
		// Save.
		saveLocal(qa) ;
	}
	
	
	private int count( boolean [] var ) {
		int num = 0; 
		for ( int i = 0; i < var.length; i++ ) {
			if ( var[i] )
				num++ ;
		}
		return num ;
	}
	
	/**
	 * Returns, as an integer, the "percent" of the provided array which is true.
	 * Intended for representing "power set" progress as an incremental.  For example,
	 * consider Solo Artist, the achievement for trying all single-player game modes.
	 * 
	 * We know exactly which game modes have been played (locally).  Google Play knows
	 * only a monotonically increasing integer.  We COULD count the number of game modes
	 * and send an increment if the total played is higher than GPG thinks, but what
	 * about if we add a new game mode?  We can't increase the Total Steps in GPG after
	 * publication.
	 * 
	 * Instead, we use a "percentage" approach, where GPG thinks there are 100 steps and
	 * we update when the "percentage complete" exceeds the GPG current steps.  This lets
	 * us easily extend local Achievements (by lengthening the array) without changing
	 * the GPG published version.
	 * 
	 * This method returns an integer between 0 and 100, inclusive.  For an array of
	 * length 'N' with any 'k' set to true, it will always return integer 'p', where
	 * 'p' is either floor( k/N ) or ceil( k/N ).  ( in other words, when k and N are
	 * unchanged, the result will always be the same, but for different k or N the
	 * discretizing function may change between floor and ceil ).
	 * 
	 * We also guarantee that the function will return '0' ONLY IF there are no 'true'
	 * values in the array (returning at least 1 otherwise), and will return '100' ONLY IF
	 * every entry in the array is true and the array has length at least 1 (returning <= 99
	 * otherwise).
	 * 
	 * @param var
	 * @return
	 */
	private int percent( boolean [] var ) {
		int num = count(var) ;
		if ( num == 0 )
			return 0 ;
		if ( num == var.length )
			return 100 ;
		int percent = (int)Math.ceil( (num * 100.0) / var.length ) ;
		if ( percent == 0 )
			return 1 ;
		if ( percent == 100 )
			return 99 ;
		return percent ;
	}
	
	private void saveLocal(Context ctx) {
        // First: write all our data to a byte array.
		try {
			//Log.d(TAG, "saveLocal") ;
			ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
			ObjectOutputStream oos = new ObjectOutputStream(baos) ;
			// write unlocks
			oos.writeObject(mDidUnlock) ;
			oos.writeObject(mHasUnlockToPush) ;
			// write incremental state
			oos.writeInt(mRowsClearedSincePush) ;
			oos.writeInt(mRowsClearedSincePushTens) ;
			oos.writeLong(mPointsEarnedSincePush100K) ;
			// write "slow collection" information leading to unlock
			oos.writeBoolean(mGameSpecialSuccess3D) ;
			oos.writeBoolean(mGameSpecialSuccessLinks) ;
			oos.writeBoolean(mGameSpecialSuccessFlash) ;
			oos.writeBoolean(mGameSpecialSuccessFused) ;
			oos.writeBoolean(mGameSpecialNonSuccess3D) ;
			oos.writeBoolean(mGameSpecialNonSuccessLinks) ;
			oos.writeBoolean(mGameSpecialNonSuccessFlash) ;
			oos.writeBoolean(mGameSpecialNonSuccessFused) ;
			// Write game modes played
			oos.writeObject(mPlayedGameModeSinglePlayer) ;
			oos.writeObject(mPlayedGameModeVS) ;
			
			// that's it.
			byte [] bytes = baos.toByteArray() ;
			oos.close() ;
			baos.close() ;
			
			//Log.d(TAG, "saveLocal written to byte array") ;
			
			// Encrypt to base 64 string...
			String ciphertext = encryptForStorage(ctx, bytes) ;
			
			//Log.d(TAG, "saveLocal encrypted") ;
			
			// write safely to mutable file, then move to real storage name.
			File writeTo = this.getFileForAchievementsMutable(ctx) ;
			File moveTo = this.getFileForAchievements(ctx) ;
			
			// write...
			PrintStream ps = new PrintStream(writeTo, CHARSET) ;
			ps.append(ciphertext) ;
			ps.close() ;
			
			//Log.d(TAG, "saveLocal written to file") ;
			
			// and move...
			moveTo.delete() ;
			writeTo.renameTo(moveTo) ;
			
			//Log.d(TAG, "saveLocal moved to permanent file") ;
			
			// note our incremental data at this point so we can refer back later
			mRowsClearedLastLocalStorage = mRowsClearedSincePush ;
			mRowsClearedTensLastLocalStorage = mRowsClearedSincePushTens ;
			
			mPointsEarnedSincePush100KLastLocalStorage = mPointsEarnedSincePush100K ;
			
			mPlayedGameModeSinglePlayerChangedSinceLocalStorage = false ;
			mPlayedGameModeVSChangedSinceLocalStorage = false ;
		} catch ( Exception e ) {
			// nothing
			e.printStackTrace() ;
		}
    }

    private void loadLocal(Context ctx) {
        try {
        	//Log.d(TAG, "loadLocal") ;
        	// Attempt to open and read the file.
        	StringBuilder sb = new StringBuilder() ;
        	File readFrom = this.getFileForAchievements(ctx) ;
        	FileInputStream fis = new FileInputStream(readFrom);
        	BufferedReader br = new BufferedReader(new InputStreamReader(fis, CHARSET));
        	String line ;
        	while ((line = br.readLine()) != null) {
        	    sb.append(line) ;
        	}
        	br.close() ;
        	fis.close() ;
        	
        	String ciphertext = sb.toString() ;
        	
        	//Log.d(TAG, "loadLocal read into ciphertext") ;
        	
        	
        	// decrypt from base 64 string
        	byte [] bytes = decryptFromStorage(ctx, ciphertext) ;
        	
        	//Log.d(TAG, "loadLocal decrypted to bytes") ;
        	
        	// Turn into an ObjectInputStream...
        	ByteArrayInputStream bais = new ByteArrayInputStream(bytes) ;
        	ObjectInputStream ois = new ObjectInputStream(bais) ;
        	
        	//Log.d(TAG, "loadLocal ObjectInputStream established") ;
        	
        	// read data.  We "safely" read array data by copying the portion
        	// provided.  This assumes that extensions in the future add to the bottom of arrays.
        	// Unlocked...
        	safeCopyInto( mDidUnlock, 		(boolean[])ois.readObject() ) ;
        	safeCopyInto( mHasUnlockToPush, (boolean[])ois.readObject() ) ;
        	// read incremental state
        	mRowsClearedSincePush = ois.readInt() ;
        	mRowsClearedSincePushTens = ois.readInt() ;
        	mPointsEarnedSincePush100K = ois.readLong() ;
        	// Read "slow collection" information leading to unlock
        	mGameSpecialSuccess3D		= ois.readBoolean() ;
			mGameSpecialSuccessLinks	= ois.readBoolean() ;
			mGameSpecialSuccessFlash	= ois.readBoolean() ;
			mGameSpecialSuccessFused	= ois.readBoolean() ;
			mGameSpecialNonSuccess3D	= ois.readBoolean() ;
			mGameSpecialNonSuccessLinks	= ois.readBoolean() ;
			mGameSpecialNonSuccessFlash	= ois.readBoolean() ;
			mGameSpecialNonSuccessFused	= ois.readBoolean() ;
        	// Read game modes played
			safeCopyInto( mPlayedGameModeSinglePlayer, 		(boolean[])ois.readObject() ) ;
			safeCopyInto( mPlayedGameModeVS, 				(boolean[])ois.readObject() ) ;
			
			//Log.d(TAG, "loadLocal instance vars set") ;
			for ( int i = 0; i < NUM_ACHIEVEMENTS; i++ ) {
				if ( mHasUnlockToPush[i] && !mDidUnlock[i] ) {
					//Log.d(TAG, "loadLocal ready to push achievement " + Name.values()[i]) ;
				}
			}
			
			// close
			ois.close() ;
			bais.close() ;
			
			//Log.d(TAG, "loadLocal ObjectInputStream closed") ;
			
			// That's it.  Note that this matches our last local storage.
			mRowsClearedLastLocalStorage = mRowsClearedSincePush ;
			mRowsClearedTensLastLocalStorage = mRowsClearedSincePushTens ;
			
			mPointsEarnedSincePush100KLastLocalStorage = mPointsEarnedSincePush100K ;
			
			mPlayedGameModeSinglePlayerChangedSinceLocalStorage = false ;
			mPlayedGameModeVSChangedSinceLocalStorage = false ;
        } catch ( Exception e ) {
        	// nothing
        	e.printStackTrace() ;
        }
    }
    
    
    private void safeCopyInto( boolean [] dst, boolean [] src ) {
    	int len = Math.min(dst.length, src.length) ;
    	for ( int i = 0; i < len; i++ )
    		dst[i] = src[i] ;
    }
    
    
	private long timeSinceLastPull() {
		if ( mLastPull == 0 ) {
			return Long.MAX_VALUE ;
		} else {
			return System.currentTimeMillis() - mLastPull ;
		}
	}
	
	
	private boolean hasPulled() {
		return mLastPull != 0 ;
	}
    
    
    /**
     * Pulls achievements from server, updating our record of which have
     * been pushed.
     * 
     * Useful during first sign in 
     * @param qa
     * @param forceReload
     */
    private void pullAchievements( QuantroApplication qa, boolean forceReload ) {
    	//Log.d(TAG, "pullAchievements") ;
    	if ( !qa.gpg_isSignedIn() ) {
    		//Log.d(TAG, "pullAchievements.  not signed in") ;
    		return ;
    	}
    	
    	// TODO Get a cloud client and pull achievements w/ 'forceReload'
    }
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// OBFUSCATED STORAGE
	//
	
	
	
	private File getFileForAchievements( Context context ) {
		File dir = context.getDir(LOCAL_STORAGE_DIR, 0) ;
		File achievements = new File( dir, LOCAL_STORAGE ) ;
		return achievements ;
	}
	
	private File getFileForAchievementsMutable( Context context ) {
		File dir = context.getDir(LOCAL_STORAGE_DIR, 0) ;
		File achievements = new File( dir, LOCAL_STORAGE_MUTABLE ) ;
		return achievements ;
	}
	
	
	private String encryptForStorage( Context context, byte [] cleartext ) {
		// TODO encrypt if uploading
		return new String (cleartext);
	}
	
	private byte [] decryptFromStorage( Context context, String ciphertext ) {
		// TODO decrypt if uploading
		return ciphertext.getBytes();
	}
	
	private char [] makePassword() {
		try {
			byte [] b1 = Base64.decode(OBFUSCATED_BYTES1) ;
			byte [] b2 = Base64.decode(OBFUSCATED_BYTES2) ;
			byte [] b3 = Base64.decode(OBFUSCATED_BYTES3) ;
			for ( int i = 0; i < b1.length; i++ ) 
				b1[i] ^= b2[ ((int)b3[i] - Byte.MIN_VALUE) % b2.length ] + i * b3[i] ;
			String password = Base64.encodeBytes(b1) ;
			return Base64.encodeBytes(b1).toCharArray() ;
		} catch ( IOException ioe ) {
			ioe.printStackTrace() ;
			return null ;
		}
	}
	
	private static final String[] DG = new String[] { "0", "1", "2", "3", "4",
			"5", "6", "7", "8", "9" };
	private static final String[] Al = new String[] { "a", "b", "c", "d", "e",
			"f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
			"s", "t", "u", "v", "w", "x", "y", "z", ".", "_", "/", "+", "=" };
	private static final String[] AU = new String[] { "A", "B", "C", "D", "E",
			"F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
			"S", "T", "U", "V", "W", "X", "Y", "Z", ".", "_", "/", "+", "=" };
	
	@SuppressWarnings("unused")
	private static final int A = 0, B = 1, C = 2, D = 3, E = 4, F = 5, G = 6,
			H = 7, I = 8, J = 9, K = 10, L = 11, M = 12, N = 13, O = 14,
			P = 15, Q = 16, R_letter = 17, S = 18, T = 19, U = 20, V = 21,
			W = 22, X = 23, Y = 24, Z = 25, DOT = 26, UND = 27, SLASH = 28,
			PLUS = 29, EQUALS = 30;
	
	String OBFUSCATED_BYTES1 = DG[2]+AU[Z]+Al[G]+Al[P]+AU[C]+AU[L]+Al[O]+AU[P]+Al[O]+Al[W]+AU[K]+Al[W]+DG[5]+AU[V]+AU[O]+AU[J]+AU[PLUS]+Al[G]+DG[9]+AU[C]+AU[S]+Al[Y]+AU[J]+Al[Z]+DG[9]+AU[H]+Al[L]+DG[4]+AU[H]+DG[4]+AU[U]+Al[B]+DG[8]+Al[N]+Al[Z]+Al[I]+AU[F]+AU[L]+Al[R_letter]+AU[A]+Al[A]+Al[O]+AU[A]+Al[S]+Al[M]+Al[Z]+AU[U]+DG[8]+AU[Z]+Al[Y]+Al[W]+AU[N]+AU[J]+Al[M]+AU[I]+Al[S]+Al[E]+DG[1]+DG[9]+DG[6]+AU[X]+AU[K]+DG[1]+AU[E]+DG[8]+AU[F]+AU[Q]+AU[H]+Al[F]+AU[Z]+Al[O]+AU[W]+AU[P]+Al[A]+Al[D]+DG[4]+Al[E]+AU[C]+Al[T]+DG[4]+AU[H]+AU[N]+AU[D]+AU[PLUS]+DG[5]+AU[K]+DG[7]+Al[R_letter]+DG[1]+Al[C]+Al[X]+Al[O]+AU[C]+Al[P]+Al[E]+Al[A]+AU[V]+Al[E]+AU[W]+Al[T]+AU[Q]+Al[X]+AU[U]+Al[F]+Al[M]+Al[K]+DG[0]+Al[V]+AU[U]+Al[L]+Al[Q]+AU[O]+AU[S]+Al[M]+AU[R_letter]+AU[D]+Al[F]+Al[Q]+Al[G]+DG[9]+AU[Z]+Al[E]+Al[H]+Al[D]+Al[D]+AU[C]+DG[5]+Al[O]+Al[O]+AU[I]+Al[B]+Al[D]+Al[V]+Al[P]+Al[M]+Al[S]+Al[R_letter]+Al[W]+AU[O]+AU[Y]+AU[W]+AU[G]+Al[P]+Al[P]+AU[Y]+AU[SLASH]+AU[L]+AU[K]+DG[8]+Al[C]+AU[PLUS]+Al[R_letter]+AU[O]+Al[C]+AU[PLUS]+Al[J]+AU[F]+Al[P]+AU[B]+Al[I]+Al[Y]+DG[7]+Al[T]+AU[W]+Al[Y]+AU[D]+AU[M]+AU[V]+AU[S]+AU[F]+Al[O]+Al[B]+AU[G]+Al[A]+AU[E]+Al[I]+Al[B]+AU[Q]+AU[U]+AU[Y]+Al[J]+AU[L]+AU[J]+DG[4]+DG[4]+Al[W]+Al[M]+Al[J]+Al[T]+Al[B]+AU[I]+Al[M]+AU[E]+Al[J]+AU[K]+Al[V]+DG[8]+DG[9]+Al[U]+DG[3]+DG[0]+Al[S]+AU[SLASH]+AU[Y]+AU[I]+Al[A]+Al[L]+Al[U]+AU[X]+AU[PLUS]+DG[0]+AU[C]+Al[K]+Al[N]+AU[W]+AU[SLASH]+Al[Y]+Al[S]+Al[I]+Al[Z]+AU[R_letter]+DG[5]+AU[K]+AU[K]+AU[H]+Al[Z]+Al[D]+Al[Y]+DG[5]+AU[M]+Al[L]+AU[N]+AU[SLASH]+DG[3]+Al[I]+Al[Q]+Al[J]+AU[W]+AU[X]+DG[5]+Al[K]+AU[Y]+AU[R_letter]+AU[V]+AU[C]+Al[K]+AU[O]+Al[Q]+DG[5]+DG[1]+Al[H]+Al[D]+AU[V]+Al[E]+AU[U]+DG[9]+DG[6]+Al[P]+Al[F]+AU[W]+AU[E]+Al[X]+Al[C]+DG[5]+Al[H]+AU[V]+AU[S]+AU[U]+DG[7]+Al[T]+AU[I]+DG[2]+Al[S]+AU[H]+Al[R_letter]+AU[C]+Al[K]+Al[P]+Al[J]+AU[S]+AU[Q]+Al[V]+AU[B]+AU[B]+Al[A]+AU[B]+Al[Q]+Al[V]+Al[H]+Al[I]+Al[G]+Al[D]+DG[6]+AU[R_letter]+Al[S]+Al[U]+AU[P]+AU[K]+DG[4]+AU[A]+AU[J]+AU[T]+AU[S]+Al[M]+Al[A]+AU[Z]+AU[N]+DG[2]+Al[F]+DG[6]+DG[5]+Al[I]+AU[M]+DG[6]+Al[F]+Al[M]+DG[7]+AU[A]+Al[N]+AU[S]+Al[O]+AU[Y]+AU[F]+AU[F]+AU[C]+Al[U]+AU[PLUS]+AU[SLASH]+DG[3]+DG[0]+AU[Y]+DG[8]+AU[T]+AU[SLASH]+AU[O]+AU[Y]+Al[Z]+Al[U]+Al[V]+AU[PLUS]+Al[U]+AU[SLASH]+AU[PLUS]+AU[B]+AU[K]+AU[Z]+Al[Y]+AU[E]+DG[7]+AU[N]+Al[V]+AU[Q]+AU[E]+Al[V]+AU[C]+AU[H]+Al[U]+Al[T]+DG[2]+AU[K]+DG[8]+Al[A]+AU[L]+AU[F]+Al[W]+AU[G]+Al[Q]+Al[X]+AU[L]+AU[P]+Al[A]+AU[U]+AU[P]+Al[N]+DG[2]+Al[E]+Al[Z]+AU[W]+AU[E]+AU[S]+AU[K]+DG[1]+AU[U]+DG[5]+Al[X]+Al[J]+Al[Y]+Al[G]+AU[J]+Al[U]+AU[E]+Al[S] ;
	String OBFUSCATED_BYTES2 = AU[A]+Al[X]+Al[X]+DG[9]+Al[J]+AU[S]+AU[Y]+Al[Z]+DG[5]+DG[4]+AU[Y]+Al[V]+AU[O]+AU[F]+AU[C]+Al[A]+Al[U]+DG[3]+AU[P]+Al[G]+DG[3]+AU[V]+Al[V]+Al[L]+Al[K]+Al[V]+AU[Y]+Al[W]+DG[2]+Al[H]+AU[T]+Al[Y]+Al[X]+AU[R_letter]+Al[F]+AU[U]+AU[D]+Al[P]+Al[A]+AU[E]+Al[Y]+Al[S]+AU[D]+Al[L]+AU[H]+Al[I]+AU[K]+Al[N]+DG[3]+Al[L]+Al[K]+Al[O]+Al[A]+AU[P]+DG[9]+Al[X]+DG[5]+DG[2]+AU[T]+AU[R_letter]+DG[7]+AU[H]+Al[K]+Al[O]+AU[U]+AU[U]+DG[3]+AU[E]+Al[B]+AU[PLUS]+AU[E]+AU[SLASH]+AU[Q]+Al[C]+Al[M]+Al[N]+AU[A]+AU[S]+AU[D]+AU[SLASH]+AU[SLASH]+AU[D]+Al[V]+Al[I]+DG[3]+AU[V]+DG[5]+Al[H]+DG[6]+Al[H]+AU[X]+AU[E]+Al[N]+AU[L]+Al[A]+AU[V]+AU[G]+AU[I]+AU[E]+Al[F]+Al[V]+Al[T]+Al[S]+Al[E]+DG[8]+AU[K]+Al[M]+Al[W]+DG[3]+Al[Y]+AU[T]+Al[Q]+AU[L]+AU[H]+AU[E]+AU[I]+AU[T]+Al[J]+AU[R_letter]+AU[I]+AU[O]+Al[N]+AU[V]+AU[J]+DG[2]+AU[F]+AU[P]+Al[N]+DG[5]+Al[J]+AU[E]+Al[M]+DG[7]+Al[M]+AU[A]+Al[R_letter]+AU[J]+AU[M]+DG[2]+Al[A]+AU[V]+AU[R_letter]+AU[U]+AU[Q]+AU[W]+Al[E]+AU[T]+AU[SLASH]+AU[F]+Al[U]+Al[X]+DG[0]+Al[C]+AU[H]+AU[I]+AU[L]+AU[PLUS]+AU[T]+AU[S]+Al[G]+AU[G]+AU[D]+Al[D]+AU[SLASH]+Al[E]+AU[L]+DG[0]+Al[P]+AU[T]+AU[I]+AU[G]+AU[N]+AU[W]+AU[O]+AU[G]+AU[Q]+AU[Q]+Al[F]+AU[G]+AU[A]+AU[U]+DG[1]+AU[PLUS]+AU[F]+AU[C]+Al[J]+Al[H]+Al[R_letter]+AU[X]+Al[D]+AU[D]+Al[X]+AU[S]+AU[PLUS]+AU[R_letter]+Al[J]+Al[C]+Al[B]+DG[9]+Al[A]+AU[Y]+AU[L]+AU[T]+DG[1]+DG[5]+Al[I]+Al[D]+DG[2]+Al[F]+AU[X]+Al[W]+AU[O]+Al[M]+AU[H]+AU[I]+DG[1]+AU[O]+AU[T]+AU[J]+DG[2]+Al[K]+Al[M]+Al[Y]+Al[L]+Al[B]+DG[8]+DG[7]+Al[O]+Al[B]+AU[F]+Al[I]+AU[A]+AU[M]+AU[C]+AU[V]+Al[Q]+Al[I]+Al[S]+AU[Z]+AU[K]+AU[S]+Al[L]+Al[B]+AU[Y]+Al[L]+DG[9]+DG[5]+Al[N]+Al[C]+DG[0]+Al[S]+AU[Q]+Al[S]+Al[A]+Al[R_letter]+Al[D]+AU[T]+AU[S]+AU[SLASH]+Al[O]+Al[J]+Al[X]+AU[Z]+DG[4]+Al[U]+DG[1]+AU[O]+AU[J]+Al[A]+Al[C]+AU[M]+AU[A]+AU[C]+Al[Z]+Al[R_letter]+AU[G]+Al[P]+Al[R_letter]+Al[K]+Al[F]+Al[R_letter]+Al[R_letter]+Al[J]+Al[Z]+AU[P]+AU[D]+Al[J]+DG[9]+DG[3]+AU[L]+Al[V]+Al[A]+Al[Y]+Al[U]+AU[P]+Al[K]+AU[L]+AU[S]+AU[L]+Al[M]+Al[C]+AU[W]+AU[C]+AU[L]+Al[U]+Al[U]+Al[W]+AU[A]+AU[R_letter]+Al[Q]+AU[E]+Al[S]+AU[Q]+AU[B]+AU[G]+AU[Q]+AU[H]+Al[T]+AU[H]+AU[W]+AU[PLUS]+Al[T]+DG[7]+DG[2]+AU[M]+Al[C]+AU[Z]+AU[V]+AU[K]+AU[E]+DG[5]+Al[T]+Al[O]+Al[D]+Al[N]+Al[K]+Al[X]+Al[Q]+Al[O]+AU[P]+Al[X]+AU[Z]+Al[V]+DG[8]+AU[D]+DG[6]+Al[Z]+AU[PLUS]+AU[S]+AU[J]+AU[C]+DG[0]+AU[X]+AU[H]+DG[0]+AU[S]+AU[D]+Al[P]+Al[Y]+Al[P]+AU[Y]+Al[S]+Al[B]+AU[G]+AU[PLUS]+Al[Y]+Al[N]+AU[Q]+AU[E]+Al[L]+AU[L]+DG[1]+DG[2]+Al[G]+AU[A]+Al[U]+AU[F]+Al[M]+AU[R_letter]+Al[U]+DG[0]+DG[9]+DG[3]+AU[U]+DG[3]+Al[G]+AU[C]+Al[O]+Al[X]+Al[W]+AU[M]+Al[W] ;
	String OBFUSCATED_BYTES3 = Al[L]+AU[R_letter]+AU[P]+Al[Y]+DG[9]+Al[W]+AU[M]+AU[P]+AU[L]+Al[G]+Al[C]+AU[I]+Al[A]+AU[U]+Al[Y]+DG[2]+DG[6]+Al[D]+AU[H]+Al[X]+AU[Z]+AU[H]+Al[O]+DG[4]+DG[8]+AU[Z]+Al[V]+Al[X]+Al[X]+AU[W]+Al[H]+AU[Q]+Al[T]+AU[Z]+AU[C]+AU[B]+AU[L]+AU[SLASH]+AU[D]+Al[B]+Al[S]+Al[P]+AU[L]+Al[C]+Al[I]+DG[7]+Al[D]+Al[D]+AU[P]+DG[5]+Al[T]+AU[R_letter]+AU[K]+Al[N]+AU[SLASH]+Al[V]+AU[T]+AU[PLUS]+AU[PLUS]+Al[J]+AU[PLUS]+AU[N]+AU[M]+Al[S]+Al[T]+AU[J]+AU[P]+AU[J]+Al[F]+DG[0]+Al[X]+Al[O]+DG[8]+Al[M]+AU[I]+DG[5]+Al[N]+AU[X]+Al[N]+AU[L]+Al[B]+AU[H]+Al[P]+Al[I]+AU[H]+AU[SLASH]+AU[L]+AU[J]+Al[I]+AU[G]+Al[J]+Al[W]+Al[O]+DG[7]+Al[U]+Al[D]+AU[Q]+AU[J]+AU[Q]+Al[G]+AU[G]+AU[F]+Al[S]+AU[R_letter]+Al[G]+Al[E]+AU[L]+DG[6]+AU[G]+AU[H]+Al[X]+AU[Y]+DG[4]+AU[J]+Al[C]+Al[H]+AU[Y]+Al[X]+Al[D]+AU[F]+DG[3]+Al[N]+Al[S]+Al[M]+Al[M]+AU[X]+Al[H]+Al[B]+AU[T]+Al[Y]+Al[S]+AU[SLASH]+Al[U]+DG[1]+Al[C]+AU[I]+AU[D]+AU[S]+Al[O]+Al[Z]+AU[T]+DG[4]+Al[J]+Al[U]+Al[C]+AU[E]+AU[P]+DG[5]+AU[T]+DG[4]+DG[2]+AU[C]+DG[6]+Al[R_letter]+AU[T]+Al[X]+AU[M]+Al[B]+Al[Z]+AU[S]+AU[E]+Al[N]+AU[N]+AU[H]+DG[9]+AU[U]+DG[6]+Al[I]+Al[Z]+AU[R_letter]+Al[U]+Al[C]+Al[I]+AU[X]+AU[G]+AU[E]+DG[2]+AU[U]+AU[E]+Al[B]+DG[7]+AU[Z]+AU[E]+AU[R_letter]+AU[Y]+Al[W]+Al[S]+AU[G]+AU[V]+DG[1]+AU[V]+Al[D]+AU[I]+Al[H]+Al[A]+AU[T]+AU[Q]+AU[S]+Al[A]+Al[S]+Al[A]+AU[Z]+Al[T]+AU[SLASH]+Al[Z]+AU[H]+AU[Q]+AU[K]+AU[G]+AU[W]+Al[K]+DG[1]+AU[F]+Al[J]+Al[K]+AU[N]+Al[Z]+AU[O]+AU[W]+AU[C]+AU[B]+AU[H]+AU[R_letter]+AU[P]+Al[B]+AU[B]+Al[Z]+Al[D]+Al[O]+DG[4]+AU[O]+AU[J]+AU[C]+AU[B]+Al[H]+Al[I]+AU[H]+DG[4]+AU[P]+Al[Q]+AU[D]+AU[D]+DG[0]+AU[A]+AU[S]+DG[8]+Al[F]+Al[V]+Al[I]+Al[G]+AU[F]+AU[A]+Al[X]+Al[R_letter]+Al[N]+AU[Q]+Al[U]+AU[M]+AU[PLUS]+Al[F]+Al[V]+Al[N]+DG[9]+Al[V]+Al[H]+AU[B]+DG[0]+Al[N]+Al[P]+DG[7]+AU[U]+Al[Q]+AU[E]+Al[N]+AU[L]+AU[Y]+Al[U]+DG[2]+Al[G]+DG[6]+AU[L]+Al[G]+Al[Q]+AU[T]+Al[L]+AU[B]+DG[4]+Al[G]+DG[6]+AU[A]+AU[J]+AU[K]+AU[I]+DG[4]+AU[F]+AU[E]+DG[3]+AU[P]+DG[1]+Al[G]+AU[T]+Al[T]+AU[T]+DG[6]+Al[O]+DG[8]+DG[1]+AU[F]+Al[N]+AU[SLASH]+Al[R_letter]+AU[V]+Al[V]+Al[W]+DG[8]+Al[J]+DG[9]+Al[J]+Al[X]+AU[Y]+AU[R_letter]+Al[M]+DG[7]+DG[0]+Al[B]+Al[O]+DG[8]+Al[K]+AU[F]+AU[E]+DG[7]+DG[1]+Al[K]+AU[R_letter]+Al[C]+Al[S]+AU[H]+Al[D]+DG[2]+Al[W]+Al[M]+Al[A]+Al[U]+Al[S]+Al[P]+AU[M]+AU[L]+Al[J]+DG[5]+AU[Z]+AU[F]+AU[D]+Al[B]+Al[W]+AU[A]+Al[C]+Al[Q]+Al[E]+Al[L]+AU[N]+AU[G]+Al[P]+AU[G]+AU[A]+AU[F]+DG[2]+AU[G]+Al[W]+AU[O]+AU[P]+Al[L]+AU[Z]+AU[SLASH]+AU[Z]+AU[G]+DG[9]+DG[9]+Al[A]+Al[D]+AU[Y]+Al[X]+AU[O]+AU[M]+AU[D]+Al[Y]+AU[M]+Al[B]+AU[S]+AU[S]+Al[T]+Al[R_letter]+Al[B] ;
	
	
}
