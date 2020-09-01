package com.peaceray.quantro.model.modes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.bags.PieceBag;
import com.peaceray.quantro.model.pieces.history.PieceHistory;
import com.peaceray.quantro.model.systems.attack.AttackSystem;
import com.peaceray.quantro.model.systems.clear.ClearSystem;
import com.peaceray.quantro.model.systems.collision.CollisionSystem;
import com.peaceray.quantro.model.systems.displacement.DisplacementSystem;
import com.peaceray.quantro.model.systems.kick.KickSystem;
import com.peaceray.quantro.model.systems.level.LevelSystem;
import com.peaceray.quantro.model.systems.lock.LockSystem;
import com.peaceray.quantro.model.systems.metamorphosis.MetamorphosisSystem;
import com.peaceray.quantro.model.systems.rotation.RotationSystem;
import com.peaceray.quantro.model.systems.score.ScoreSystem;
import com.peaceray.quantro.model.systems.special.SpecialSystem;
import com.peaceray.quantro.model.systems.timing.TimingSystem;
import com.peaceray.quantro.model.systems.trigger.TriggerSystem;
import com.peaceray.quantro.model.systems.valley.ValleySystem;
import com.peaceray.quantro.q.QInteractions;

public class GameModes {
	
	// GAME MODES!  DO NOT CHANGE ANY OF THESE CODES.  AT THE VERY LEAST
	// IT WILL BREAK SINGLE-PLAYER SAVES, WHICH USE THE NUMERICAL CODE
	// TO DETERMINE SAVE LOCATION.
	// Single player type A		(Endurance)
	public static final int GAME_MODE_SP_QUANTRO_A = 0 ;
	public static final int GAME_MODE_SP_RETRO_A = 1 ;
	// Single player type B		(Progression)
	public static final int GAME_MODE_SP_QUANTRO_B = 2 ;
	public static final int GAME_MODE_SP_RETRO_B = 3 ;
	// Single player type C		(Unnamed constant push-up gametype)
	public static final int GAME_MODE_SP_QUANTRO_C = 4 ;
	public static final int GAME_MODE_SP_RETRO_C = 5 ;
	
	
	// 1v1 GENERIC GAME TYPES
	// 1v1 type A
	public static final int GAME_MODE_1V1_QUANTRO_A = 100 ;
	public static final int GAME_MODE_1V1_RETRO_A = 101 ;
	// 1v1 additional generic types
	public static final int GAME_MODE_1V1_QUANTRO_C = 102 ;
	public static final int GAME_MODE_1V1_RETRO_C = 103 ;
	
	
	// 1v1 special types (unique to Quantro or Retro)
	public static final int GAME_MODE_1V1_QUANTRO_BITTER_PILL = 200 ;
	public static final int GAME_MODE_1V1_RETRO_GRAVITY = 201 ;
	
	
	// // FREE 4 ALL TYPES
	public static final int GAME_MODE_FREE4ALL_QUANTRO_A = 300 ;
	public static final int GAME_MODE_FREE4ALL_RETRO_A = 301 ;
	public static final int GAME_MODE_FREE4ALL_QUANTRO_C = 302 ;
	public static final int GAME_MODE_FREE4ALL_RETRO_C = 303 ;
	
	
	
	public static List<Integer> GAME_MODES ;
	public static List<Integer> GAME_MODES_SP_FREEPLAY ;
	public static List<Integer> GAME_MODES_MP ;
	
	static {
		// populate sp freeplay...
		ArrayList<Integer> gameModesSPFreeplay = new ArrayList<Integer>() ;
		gameModesSPFreeplay.add(GAME_MODE_SP_QUANTRO_A) ;
		gameModesSPFreeplay.add(GAME_MODE_SP_QUANTRO_B) ;
		gameModesSPFreeplay.add(GAME_MODE_SP_RETRO_A) ;
		gameModesSPFreeplay.add(GAME_MODE_SP_RETRO_B) ;
		gameModesSPFreeplay.add(GAME_MODE_SP_QUANTRO_C) ;
		gameModesSPFreeplay.add(GAME_MODE_SP_RETRO_C) ;
		
		// populate mp...
		ArrayList<Integer> gameModesMP = new ArrayList<Integer>() ;
		gameModesMP.add(GAME_MODE_1V1_QUANTRO_A) ;
		gameModesMP.add(GAME_MODE_1V1_RETRO_A) ;
		gameModesMP.add(GAME_MODE_1V1_QUANTRO_C) ;
		gameModesMP.add(GAME_MODE_1V1_RETRO_C) ;
		// non-generic game modes
		gameModesMP.add(GAME_MODE_1V1_QUANTRO_BITTER_PILL) ;
		gameModesMP.add(GAME_MODE_1V1_RETRO_GRAVITY) ;
		// free-4-all game modes
		gameModesMP.add(GAME_MODE_FREE4ALL_QUANTRO_A) ;
		gameModesMP.add(GAME_MODE_FREE4ALL_RETRO_A) ;
		gameModesMP.add(GAME_MODE_FREE4ALL_QUANTRO_C) ;
		gameModesMP.add(GAME_MODE_FREE4ALL_RETRO_C) ;
		
		
		// combine into all built-in game modes...
		ArrayList<Integer> gameModes = new ArrayList<Integer>() ;
		for ( int i = 0; i < gameModesSPFreeplay.size(); i++ )
			if ( !gameModes.contains(gameModesSPFreeplay.get(i)) )
				gameModes.add(gameModesSPFreeplay.get(i)) ;
		for ( int i = 0; i < gameModesMP.size(); i++ )
			if ( !gameModes.contains(gameModesMP.get(i)) )
				gameModes.add(gameModesMP.get(i)) ;
		
		// immutable versions!
		GAME_MODES = Collections.unmodifiableList(gameModes) ;
		GAME_MODES_SP_FREEPLAY = Collections.unmodifiableList(gameModesSPFreeplay) ;
		GAME_MODES_MP = Collections.unmodifiableList(gameModesMP) ;
	}
	
	// CUSTOM TEMPLATES!  NOTE: Extend these from the current max; do NOT
	// re-assign template integers.  They are used in game mode numbers, and
	// thus in save files.
	public static final int CUSTOM_TEMPLATE_SP_RETRO_A = 0 ;
	public static final int CUSTOM_TEMPLATE_SP_RETRO_B = 1 ;
	public static final int CUSTOM_TEMPLATE_1V1_RETRO_A = 2 ;
	public static final int CUSTOM_TEMPLATE_SP_QUANTRO_A = 3 ;
	public static final int CUSTOM_TEMPLATE_SP_QUANTRO_B = 4 ;
	public static final int CUSTOM_TEMPLATE_1V1_QUANTRO_A = 5 ;
	// extension: custom quantro / retro C ("tides")
	public static final int CUSTOM_TEMPLATE_SP_RETRO_C = 6 ;
	public static final int CUSTOM_TEMPLATE_SP_QUANTRO_C = 7 ;
	
	public static final int NUM_CUSTOM_TEMPLATES = 8 ;
	
	
	
	// BEHAVIOR: Reserve
	public static final int RESERVE_BEHAVIOR_UNKNOWN = GameMode.RESERVE_BEHAVIOR_UNKNOWN ;
	public static final int RESERVE_BEHAVIOR_SWAP = GameMode.RESERVE_BEHAVIOR_SWAP ;
	public static final int RESERVE_BEHAVIOR_INSERT = GameMode.RESERVE_BEHAVIOR_INSERT ;
	public static final int RESERVE_BEHAVIOR_SWAP_REENTER = GameMode.RESERVE_BEHAVIOR_SWAP_REENTER ;
	public static final int RESERVE_BEHAVIOR_INSERT_REENTER = GameMode.RESERVE_BEHAVIOR_INSERT_REENTER ;
	public static final int RESERVE_BEHAVIOR_SPECIAL = GameMode.RESERVE_BEHAVIOR_SPECIAL ;
	
	public static final int SET_CLEAR_NO = GameMode.SET_CLEAR_NO ;
	public static final int SET_CLEAR_ANY = GameMode.SET_CLEAR_ANY ;
	public static final int SET_CLEAR_TOTAL = GameMode.SET_CLEAR_TOTAL ;
	
	public static final int SET_STARTING_GARBAGE_NO = 0 ;
	public static final int SET_STARTING_GARBAGE_YES = 1 ;
	
	public static final int SET_PER_LEVEL_GARBAGE_NO = 0 ;
	public static final int SET_PER_LEVEL_GARBAGE_YES = 1 ;
	
	public static final int SET_LEVEL_LOCK_NO = GameMode.SET_LEVEL_LOCK_NO ;
	public static final int SET_LEVEL_LOCK_YES = GameMode.SET_LEVEL_LOCK_YES ;
	
	public static final int SET_DISPLACEMENT_FIXED_RATE_NO = GameMode.SET_DISPLACEMENT_FIXED_RATE_NO ;
	public static final int SET_DISPLACEMENT_FIXED_RATE_PRACTICE = GameMode.SET_DISPLACEMENT_FIXED_RATE_PRACTICE ;
	public static final int SET_DISPLACEMENT_FIXED_RATE_YES = GameMode.SET_DISPLACEMENT_FIXED_RATE_YES ;
	
	public static final int SET_DIFFICULTY_NO = GameMode.SET_DIFFICULTY_NO ;
	public static final int SET_DIFFICULTY_NO_PRACTICE = GameMode.SET_DIFFICULTY_NO_PRACTICE ;
	public static final int SET_DIFFICULTY_YES = GameMode.SET_DIFFICULTY_YES ;
	
	public static final int LEVEL_UP_SMOOTH = GameMode.LEVEL_UP_SMOOTH ;
	public static final int LEVEL_UP_BREAK = GameMode.LEVEL_UP_BREAK ;
	
	
	public static final int CLASS_CODE_ENDURANCE = GameMode.CLASS_CODE_ENDURANCE ;
	public static final int CLASS_CODE_PROGRESSION = GameMode.CLASS_CODE_PROGRESSION ;
	public static final int CLASS_CODE_FLOOD = GameMode.CLASS_CODE_FLOOD ;
	public static final int CLASS_CODE_SPECIAL = GameMode.CLASS_CODE_SPECIAL ;
	
	
	public static final int MEASURE_PERFORMANCE_BY_SCORE = 0 ;
	public static final int MEASURE_PERFORMANCE_BY_TIME = 1 ;
	
	
	// A place to store instantiated GameMode objects.
	private static Hashtable<Integer, GameMode> includedGameMode = new Hashtable<Integer, GameMode>() ;
	
	// A place to store instantiated custom GameModes.
	private static ArrayList<GameMode> customGameMode = new ArrayList<GameMode>() ;
	private static ArrayList<Integer> customGameModeInteger = new ArrayList<Integer>() ;
		// same as custom ID.
	
	private static GameMode [] customTemplate = new GameMode[NUM_CUSTOM_TEMPLATES] ;
	
	
	public static boolean has( int gameMode ) {
		if ( GAME_MODES.contains( Integer.valueOf(gameMode) ) )
			return true ;
		for ( int i = 0; i < customGameMode.size(); i++ )
			if ( customGameModeInteger.get(i) == gameMode )
				return true ;
		return false ;
	}
	
	public static Iterator<Integer> iteratorIncluded() {
		return GAME_MODES.iterator() ;
	}
	
	public static Iterator<Integer> iteratorIncludedSinglePlayerFreePlay() {
		return GAME_MODES_SP_FREEPLAY.iterator() ;
	}
	
	public static Iterator<Integer> iteratorIncludedMultiPlayer() {
		return GAME_MODES_MP.iterator() ;
	}
	
	
	public static int numIncluded() {
		return GAME_MODES.size() ;
	}
	
	public static int numIncludedSinglePlayerFreePlay() {
		return GAME_MODES_SP_FREEPLAY.size() ;
	}
	
	public static int numIncludedMultiPlayer() {
		return GAME_MODES_MP.size() ;
	}
	
	
	
	/**
	 * Returns an ID which can be freely assigned to a CustomGameModeSetting.
	 * 
	 * By convention, a new CustomGameModeSetting object will create custom games
	 * with modes based on its ids in a reversible way.  To be as future proof
	 * as possible, we leave space for 1000 game modes within a single ID
	 * (to understand why this is important, consider what happens when multiple
	 * people in a lobby share custom settings with the same ID).  Assuming 100
	 * available single-player game modes, that leaves a space for 900 unique
	 * multiplayer game modes from the same ID.
	 * 
	 * Our convention is thus: read through the game modes present, extracting
	 * the set of used custom IDs.  Return the minimum integer which is >= 1
	 * and does not appear in the list.
	 * 
	 * @return
	 */
	public static int getFreeCustomGameModeSettingID() {
		ArrayList<Integer> ids = new ArrayList<Integer>() ;
		for ( int i = 0; i < customGameMode.size(); i++ ) {
			Integer id = customGameMode.get(i).mCustomGameModeSettings.getID() ;
			if ( !ids.contains(id) )
				ids.add(id) ;
		}
		
		Collections.sort(ids) ;
		int minAvailable = 1 ;
		for ( int i = 0; i < ids.size(); i++ )
			if ( ids.get(i).intValue() == minAvailable )
				minAvailable++ ;
		
		return minAvailable ;
	}
	
	
	/**
	 * A single method for setting the entire list of custom game modes.  After
	 * this method returns, the game modes will include all relevant derivations of
	 * the provided CustomSettings, AND NO OTHERS (except built-in game modes).
	 * 
	 * Order is maintained for later queries.  Individual game modes will be based
	 * on IDs in a reversible operation.
	 * 
	 * @param ar
	 */
	public static void setCustomGameModeSettings( ArrayList<CustomGameModeSettings> ar ) {
		
		// we require unique IDs within the provided list.  Check for that first.
		ArrayList<Integer> ids = new ArrayList<Integer>() ;
		for ( int i = 0; i < ar.size(); i++ )
			ids.add( ar.get(i).getID() ) ;
		Collections.sort(ids) ;
		for ( int i = 0; i < ar.size()-1; i++ )
			if ( ids.get(i).equals(ids.get(i+1) ) )
				throw new IllegalArgumentException("CustomGameModeSettings id " + ids.get(i) + " is used more than once") ;
		
		// two things to note.  First, applying custom settings is a reversible
		// operation - in other words, we can safely re-apply to the appropriate
		// object.
		
		// Second, we do not expect that to actually be necessary.  This call is infrequent
		// and there is no expected penalty to simply making a new list.
		
		ArrayList<GameMode> newCustomGameModes = new ArrayList<GameMode>() ;
		ArrayList<Integer> newCustomGameModeIntegers = new ArrayList<Integer>() ;
		
		// for each setting object provided, create a new GameMode from each
		// available template.
		for ( int i = 0; i < ar.size(); i++ ) {
			CustomGameModeSettings cgms = ar.get(i) ;
			for ( int t = 0; t < NUM_CUSTOM_TEMPLATES; t++ ) {
				GameMode m = (GameMode) retrieveCustomTemplateObject( t ) ;
				
				boolean include = true ;
				// include only if single player, or the CGMS allows multiplayer.
				include = include && ( !m.isMultiplayer() || cgms.getAllowMultiplayer() ) ;
				// include only if the number of QPanes match.
				include = include && ( m.qpanes == cgms.getNumberQPanes() ) ;
				
				if ( include ) {
					m = (GameMode)m.clone() ;
					m.apply( cgms ) ;
					
					newCustomGameModes.add( m ) ;
					newCustomGameModeIntegers.add( customIDToGameMode( cgms.getID(), t ) ) ;
				}
			}
		}
		
		customGameMode = newCustomGameModes ;
		customGameModeInteger = newCustomGameModeIntegers ;
	}
	
	public static int customIDToGameMode( int id, int template ) {
		return id * 1000 + template ;
	}
	
	public static int gameModeToCustomID( int gameMode ) {
		return gameMode / 1000 ;		// integer truncation
	}
	
	public static int gameModeToCustomTemplate( int gameMode ) {
		return gameMode % 1000 ;
	}
	
	
	public static int [] getSinglePlayerFreePlayGameModes() {
		int [] modes = new int[GAME_MODES_SP_FREEPLAY.size()] ;
		for ( int i = 0; i < modes.length; i++ )
			modes[i] = GAME_MODES_SP_FREEPLAY.get(i) ;
		
		return modes ;
	}
	
	public static int [] getMultiPlayerGameModes() {
		int [] modes = new int[GAME_MODES_MP.size()] ;
		for ( int i = 0; i < modes.length; i++ )
			modes[i] = GAME_MODES_MP.get(i) ;
		
		return modes ;
	}
	
	
	/**
	 * Returns a newly-allocated integer array giving the custom
	 * game modes currently available.  This method retains the order
	 * from setCustomGameModeSettings, following the convention that
	 * all derived GameModes from a particular Settings are presented
	 * in order before moving to the next Settings.
	 * 
	 * @param includeSP Include single-player game modes in the list
	 * @param includeMP Include multiplayer game modes in the list
	 * @return
	 */
	public static int [] getCustomGameModes( boolean includeSP, boolean includeMP ) {
		int count = 0 ;
		for ( int i = 0; i < customGameMode.size(); i++ ) {
			GameMode gm = customGameMode.get(i) ;
			if ( (includeSP && !gm.isMultiplayer())
					|| (includeMP && gm.isMultiplayer()) )
				count++ ;
		}
		
		int [] modes = new int[count] ;
		int index = 0 ;
		for ( int i = 0; i < customGameMode.size(); i++ ) {
			GameMode gm = customGameMode.get(i) ;
			if ( (includeSP && !gm.isMultiplayer())
					|| (includeMP && gm.isMultiplayer()) )
				modes[index++] = customGameModeInteger.get(i) ;
		}
		
		return modes ;
	}
	
	
	/**
	 * Returns a newly-allocated integer array giving the custom
	 * game modes associated with this CustomGameModeSettings ID.
	 * 
	 * @param cgmsID
	 * @return
	 */
	public static int [] getCustomGameModes( int cgmsID ) {
		int count = 0 ;
		for ( int i = 0; i < customGameMode.size(); i++ ) {
			GameMode gm = customGameMode.get(i) ;
			if ( gm.mCustomGameModeSettings.getID() == cgmsID )
				count++ ;
		}
		
		int [] modes = new int[count] ;
		int index = 0 ;
		for ( int i = 0; i < customGameMode.size(); i++ ) {
			GameMode gm = customGameMode.get(i) ;
			if ( gm.mCustomGameModeSettings.getID() == cgmsID )
				modes[index++] = customGameModeInteger.get(i) ;
		}
		
		return modes ;
	}
	
	
	/*
	 * *********************************************************************
	 * 
	 * GAME MODE OBJECT REFERENCES
	 * 
	 * Ways to reference a game mode by an object, to avoid allocation.
	 * 
	 * *********************************************************************
	 */
	
	public static Integer gameModeIntegerObject( int gameMode ) {
		// this call sets gameModeInteger
		if ( retrieveGameModeObject( gameMode ) == null )
			return null ;
		
		Integer integer = Integer.valueOf(gameMode) ;
		if ( GAME_MODES.contains(integer) )
			return integer ;
		
		for ( int i = 0; i < customGameModeInteger.size(); i++ )
			if ( customGameModeInteger.get(i).intValue() == gameMode )
				return integer ;
		
		return null ;
	}
	
	
	/*
	 * *********************************************************************
	 * 
	 * GAME MODE INFO QUERIES
	 * 
	 * These methods allow repeated queries regarding basic information about
	 * the game mode, including name, display name, etc.
	 * 
	 * *********************************************************************
	 */
	
	public static boolean isCustom( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? false : gm.mCustomGameModeSettings != null ;
	}
	
	public static String name( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? "" : gm.name ;
	}
	
	public static String shortName( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? "" : gm.shortName ;
	}
	
	public static String className( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? "" : gm.className ;
	}
	
	public static int classCode( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.classCode ;
	}
	
	public static int measurePerformanceBy( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		if ( gm == null )
			return -1 ;
		if ( gm.classCode == CLASS_CODE_FLOOD && gm.playersMax == 1 )
			return MEASURE_PERFORMANCE_BY_TIME ;
		return MEASURE_PERFORMANCE_BY_SCORE ;
	}
	
	public static String description( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? "" : gm.description ;
	}
	
	public static String shortDescription( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? "" : gm.shortDescription ;
	}
	
	public static int maxLevel( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.maxLevel ;
	}
	
	public static int maxStartingLevel( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.maxStartingLevel ;
	}
	
	public static int setClears( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.setClears ;
	}
	
	public static int setStartingGarbage( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.setStartingGarbage ;
	}
	
	public static int setPerLevelGarbage( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.setPerLevelGarbage ;
	}
	
	public static int defaultGarbage( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.defaultGarbage ;
	}
	
	public static int setLevelLock( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.setLevelLock ;
	}
	
	public static int setDisplacementFixedRate( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.setDisplacementFixedRate ;
	}
	
	public static int setDifficulty( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.setDifficulty ;
	}
	
	public static int levelUp( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.levelUp ;
	}
	
	/*
	 * *********************************************************************
	 * 
	 * GAME MODE BEHAVIOR QUERIES
	 * 
	 * These methods allow repeated queries regarding the specific behavioral
	 * quirks of a game mode.  Repeated queries to these are much safer than
	 * the 'new' methods which produce the appropriate systems and piece bags.
	 * 
	 * *********************************************************************
	 */
	
	/**
	 * Returns one of the RESERVE_BEHAVIOR_* constants, according to
	 * the behavior of the indicated game mode ( MODE_* ).  This
	 * behavior is read from the appropriate values/game_mode_*.xml
	 * file, using Resources and R.
	 * 
	 * @param ginfo The game mode info in question
	 * @return The reserve behavior constant which fits this reserve behavior
	 */
	public static int reserveBehavior( GameInformation ginfo ) {
		return reserveBehavior( ginfo.mode ) ;
	}
	
	/**
	 * Returns one of the RESERVE_BEHAVIOR_* constants, according to
	 * the behavior of the indicated game mode ( MODE_* ).  This
	 * behavior is read from the appropriate values/game_mode_*.xml
	 * file, using Resources and R.
	 * 
	 * @param gameMode The game mode in question
	 * @return The reserve behavior constant which fits this reserve behavior
	 */
	public static int reserveBehavior( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.reserve ;
	}
	
	public static int minPlayers( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.playersMin ;
	}
	
	public static int maxPlayers( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.playersMax ;
	}
	
	public static boolean supportsNumPlayers( int gameMode, int numPlayers ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? false : gm.playersMin <= numPlayers && numPlayers <= gm.playersMax ;
	}
	
	public static boolean isMultiplayer( GameInformation ginfo ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm == null ? false : gm.isMultiplayer() ;
	}
	
	public static int numberRows( GameInformation ginfo ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm == null ? -1 : gm.rows ;
	}
	
	public static int numberColumns( GameInformation ginfo ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm == null ? -1 : gm.cols ;
	}
	
	public static int numberRows( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.rows ;
	}
	
	public static int numberColumns( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.cols ;
	}
	
	
	public static int numberQPanes( GameInformation ginfo ) {
		return numberQPanes( ginfo.mode ) ;
	}
	
	public static int numberQPanes( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? -1 : gm.qpanes ;
	}
	
	public static boolean hasRotation( GameInformation ginfo ) {
		return hasRotation( ginfo.mode ) ;
	}
	
	public static boolean hasRotation( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? false : gm.hasRotation() ;
	}
	
	public static boolean hasReflection( GameInformation ginfo ) {
		return hasReflection( ginfo.mode ) ;
	}
	
	public static boolean hasReflection( int gameMode ) {
		GameMode gm = retrieveGameModeObject( gameMode ) ;
		return gm == null ? false : gm.hasReflection() ;
	}
	
	
	
	
	/*
	 * *********************************************************************
	 * 
	 * GAME MODE SYSTEM CONSTRUCTORS
	 * 
	 * These methods construct and return the appropriate systems,
	 * QInteractions, PieceBags, etc. for the provided game mode.
	 * The appropriate actions (i.e. classes) are read from
	 * game_mode_*.xml; these methods convert the string into a
	 * particular class definition.
	 * 
	 * *********************************************************************
	 */
	
	
	/**
	 * Constructs and returns the appropriate QInteractions class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation of QInteractions constructed and returned
	 * @return An instance of QInteractions (or null)
	 */
	public static QInteractions newQInteractions( GameInformation ginfo ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newQInteractions() ;
	}
	
	/**
	 * Constructs and returns the appropriate PieceBag for the given
	 * GameInformation object (specifically, the result of getMode())
	 * 
	 * @param ginfo The GameInformation object used to determine
	 * the appropriate PieceBag and (perhaps) to be given to the bag in
	 * its construction.
	 * 
	 * @return A newly constructed PieceBag - or null, if the game mode
	 * is not recognized.
	 */
	public static PieceBag newPieceBag( GameInformation ginfo ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newPieceBag( ginfo ) ;
	}
	
	/**
	 * Constructs and returns the appropriate reserve PieceBag for the given
	 * GameInformation object (specifically, the result of getMode())
	 * 
	 * @param ginfo The GameInformation object used to determine
	 * the appropriate PieceBag and (perhaps) to be given to the bag in
	 * its construction.
	 * 
	 * @return A newly constructed reserve PieceBag - or null, if the game mode
	 * is not recognized.
	 */
	public static PieceBag newReserveBag( GameInformation ginfo ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newReserveBag( ginfo ) ;
	}
	
	
	public static PieceHistory newPieceHistory( GameInformation ginfo, PieceBag pieceBag, PieceBag reserveBag, PieceBag ... otherBags ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newPieceHistory(ginfo, pieceBag, reserveBag, otherBags) ;
	}
	
	
	public static AttackSystem newAttackSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newAttackSystem( ginfo, qi ) ;
	}

	
	/**
	 * Constructs and returns the appropriate ClearSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of ClearSystem (or null)
	 */
	public static ClearSystem newClearSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newClearSystem( ginfo, qi ) ;
	}
	
	/**
	 * Constructs and returns the appropriate CollisionSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of CollisionSystem (or null)
	 */
	public static CollisionSystem newCollisionSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newCollisionSystem( ginfo, qi ) ;
	}
	
	
	/**
	 * Constructs and returns the appropriate DisplacementSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * 
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of DisplacementSystem (or null)
	 */
	public static DisplacementSystem newDisplacementSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newDisplacementSystem( ginfo, qi ) ;
	}
	
	
	/**
	 * Constructs and returns the appropriate KickSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of KickSystem (or null)
	 */
	public static KickSystem newKickSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newKickSystem( ginfo, qi ) ;
	}
	
	/**
	 * Constructs and returns the appropriate LockSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of LockSystem (or null)
	 */
	public static LockSystem newLockSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newLockSystem( ginfo, qi ) ;
	}
	
	/**
	 * Constructs and returns the appropriate LevelSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of LockSystem (or null)
	 */
	public static LevelSystem newLevelSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newLevelSystem( ginfo, qi ) ;
	}
	
	
	/**
	 * Constructs and returns the appropriate MetamorphosisSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of MetamorphosisSystem (or null)
	 */
	public static MetamorphosisSystem newMetamorphosisSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newMetamorphosisSystem( ginfo, qi ) ;
	}
	
	
	/**
	 * Constructs and returns the appropriate RotationSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of KickSystem (or null)
	 */
	public static RotationSystem newRotationSystem( GameInformation ginfo, QInteractions qi, PieceBag...bags ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newRotationSystem( ginfo, qi, bags ) ;
	}
	
	/**
	 * Constructs and returns the appropriate ScoreSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of ScoreSystem (or null)
	 */
	public static ScoreSystem newScoreSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newScoreSystem( ginfo, qi ) ;
	}
	
	/**
	 * Constructs and returns the appropriate SpecialSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of SpecialSystem (or null)
	 */
	public static SpecialSystem newSpecialSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newSpecialSystem( ginfo, qi ) ;
	}
	
	/**
	 * Constructs and returns the appropriate TimingSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of TimingSystem (or null)
	 */
	public static TimingSystem newTimingSystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newTimingSystem( ginfo, qi ) ;
	}
	
	
	/**
	 * Constructs and returns the appropriate trigger system class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * 
	 * Trigger systems make use of a variety of game components; these must
	 * be provided in this call, so the TriggerSystem can be appropriately
	 * configured.
	 * 
	 * @param game A reference to the associated game object (necessary for triggers)
	 * @param ginfo A reference to the associated ginfo object
	 * @param qi A reference to the associated qInteraction object
	 * @param cls A reference to the associated clear system
	 * @param cs A reference to the associate collision system
	 * @param ls A reference to the associated lock system
	 * @return A newly constructed trigger system.
	 */
	public static TriggerSystem newTriggerSystem(
			Game game, GameInformation ginfo, QInteractions qi,
			ClearSystem cls, CollisionSystem cs, LockSystem ls, ScoreSystem ss) {
		
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newTriggerSystem( game, ginfo, qi, cls, cs, ls, ss ) ;
	}
	
	
	/**
	 * Constructs and returns the appropriate ValleySystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of ValleySystem (or null)
	 */
	public static ValleySystem newValleySystem( GameInformation ginfo, QInteractions qi ) {
		GameMode gm = retrieveGameModeObject( ginfo ) ;
		return gm.newValleySystem( ginfo, qi ) ;
	}
	

	// Access to GameMode objects.
	private static GameMode retrieveGameModeObject( GameInformation ginfo ) {
		return retrieveGameModeObject( ginfo.mode ) ; 
	}
	
	
	private static GameMode retrieveGameModeObject( int gameModeNumber ) {
		GameMode gm = null ;
		Integer gameModeInteger = Integer.valueOf(gameModeNumber) ;
		if ( GAME_MODES.contains(gameModeInteger) ) {
			
			// Try to retrieve an existing object.
			gm = includedGameMode.get(gameModeInteger) ;
			if ( gm == null ) {	// Make a new GameMode object
				switch( gameModeNumber ) {
				case GAME_MODE_SP_QUANTRO_A:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0000_sp_quantro_a.xml") ) ;
					break ;
				case GAME_MODE_SP_RETRO_A:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0001_sp_retro_a.xml") ) ;
					break ;
				case GAME_MODE_SP_QUANTRO_B:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0002_sp_quantro_b.xml") ) ;
					break ;
				case GAME_MODE_SP_RETRO_B:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0003_sp_retro_b.xml") ) ;
					break ;
				case GAME_MODE_SP_QUANTRO_C:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0004_sp_quantro_c.xml") ) ;
					break ;
				case GAME_MODE_SP_RETRO_C:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0005_sp_retro_c.xml") ) ;
					break ;
				case GAME_MODE_1V1_QUANTRO_A:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0100_1v1_quantro_a.xml") ) ;
					break ;
				case GAME_MODE_1V1_RETRO_A:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0101_1v1_retro_a.xml") ) ;
					break ;
				case GAME_MODE_1V1_QUANTRO_C:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0102_1v1_quantro_c.xml") ) ;
					break ;
				case GAME_MODE_1V1_RETRO_C:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0103_1v1_retro_c.xml") ) ;
					break ;
				case GAME_MODE_1V1_QUANTRO_BITTER_PILL:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0200_1v1_quantro_bitter_pill.xml") ) ;
					break ;
				case GAME_MODE_1V1_RETRO_GRAVITY:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0201_1v1_retro_gravity.xml") ) ;
					break ;
				// Free 4 All
				case GAME_MODE_FREE4ALL_QUANTRO_A:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0300_free4all_quantro_a.xml") ) ;
					break ;
				case GAME_MODE_FREE4ALL_RETRO_A:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0301_free4all_retro_a.xml") ) ;
					break ;
				case GAME_MODE_FREE4ALL_QUANTRO_C:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0302_free4all_quantro_c.xml") ) ;
					break ;
				case GAME_MODE_FREE4ALL_RETRO_C:
					gm = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_0303_free4all_retro_c.xml") ) ;
					break ;
				}
				
				if ( gm != null )
					includedGameMode.put(gameModeInteger, gm) ;
			}
		}
		else {
			// Handle custom game mode retrieval.  We do not take responsibility for
			// populating custom game modes, the user should set CustomGameModeSettings
			// before attempting to retrieve any custom modes.
			
			for ( int i = 0; i < customGameMode.size(); i++ )
				if ( customGameModeInteger.get(i).intValue() == gameModeNumber )
					gm = customGameMode.get(i) ;
		}
		
		return gm ;
	}
	
	/**
	 * Returns the specified template object.
	 * 
	 * This method might return the same instance each time.  Do NOT make
	 * any direct changes to the template (it is the TEMPLATE!).  Instead,
	 * make a clone() and add your custom settings to it instead.
	 * @param templateNumber
	 */
	private static GameMode retrieveCustomTemplateObject( int templateNumber ) {
		GameMode t = null ;
		if ( templateNumber >= 0 && templateNumber < NUM_CUSTOM_TEMPLATES ) {
			// try to retrieve an existing object
			if ( customTemplate[templateNumber] != null )
				t = customTemplate[templateNumber] ;
			else {
				// make a new object
				switch( templateNumber ) {
				case CUSTOM_TEMPLATE_SP_RETRO_A:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_sp_retro_a.xml") ) ;
					break ;
				case CUSTOM_TEMPLATE_SP_RETRO_B:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_sp_retro_b.xml") ) ;
					break ;
				case CUSTOM_TEMPLATE_1V1_RETRO_A:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_1v1_retro_a.xml") ) ;
					break ;
				case CUSTOM_TEMPLATE_SP_QUANTRO_A:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_sp_quantro_a.xml") ) ;
					break ;
				case CUSTOM_TEMPLATE_SP_QUANTRO_B:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_sp_quantro_b.xml") ) ;
					break ;
				case CUSTOM_TEMPLATE_1V1_QUANTRO_A:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_1v1_quantro_a.xml") ) ;
					break ;
				case CUSTOM_TEMPLATE_SP_RETRO_C:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_sp_retro_c.xml") ) ;
					break ;
				case CUSTOM_TEMPLATE_SP_QUANTRO_C:
					t = customTemplate[templateNumber] = new GameMode( GameModes.class.getResourceAsStream("/modes/game_mode_template_sp_quantro_c.xml") ) ;
					break ;
				}
			}
		}
		
		return t ;
	}
	
}
