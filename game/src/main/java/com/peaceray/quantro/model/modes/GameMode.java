package com.peaceray.quantro.model.modes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.bags.*;
import com.peaceray.quantro.model.pieces.history.PieceHistory;
import com.peaceray.quantro.model.systems.attack.*;
import com.peaceray.quantro.model.systems.clear.*;
import com.peaceray.quantro.model.systems.collision.*;
import com.peaceray.quantro.model.systems.displacement.AbsoluteTimeDisplacementSystem;
import com.peaceray.quantro.model.systems.displacement.DeadDisplacementSystem;
import com.peaceray.quantro.model.systems.displacement.DisplacementSystem;
import com.peaceray.quantro.model.systems.displacement.RealTimeDisplacementSystem;
import com.peaceray.quantro.model.systems.kick.*;
import com.peaceray.quantro.model.systems.level.*;
import com.peaceray.quantro.model.systems.lock.*;
import com.peaceray.quantro.model.systems.metamorphosis.*;
import com.peaceray.quantro.model.systems.rotation.*;
import com.peaceray.quantro.model.systems.score.*;
import com.peaceray.quantro.model.systems.special.*;
import com.peaceray.quantro.model.systems.timing.*;
import com.peaceray.quantro.model.systems.trigger.*;
import com.peaceray.quantro.model.systems.valley.*;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QInteractionsBasic;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.Function;
import com.peaceray.quantro.utils.QuadraticFunction;
import com.peaceray.quantro.utils.simulatedarray.SimulatedArray;
import com.peaceray.quantro.utils.simulatedarray.SimulatedArrayFactory;

class GameMode implements Cloneable {
	
	//private static final String TAG = "GameMode" ;
	
	
	// Can read these from GameModes.
	static final int RESERVE_BEHAVIOR_UNKNOWN = 0 ;
	static final int RESERVE_BEHAVIOR_SWAP = 1 ;
	static final int RESERVE_BEHAVIOR_INSERT = 2 ;
	static final int RESERVE_BEHAVIOR_SWAP_REENTER = 3 ;
	static final int RESERVE_BEHAVIOR_INSERT_REENTER = 4 ;
	static final int RESERVE_BEHAVIOR_SPECIAL = 5 ;		// leave it to the special system to determine how the reserve works.
	
	static final int SET_CLEAR_NO = 0 ;
	static final int SET_CLEAR_ANY = 1 ;
	static final int SET_CLEAR_TOTAL = 2 ;
	
	static final int SET_STARTING_GARBAGE_NO = 0 ;
	static final int SET_STARTING_GARBAGE_YES = 1 ;
	
	static final int SET_PER_LEVEL_GARBAGE_NO = 0 ;
	static final int SET_PER_LEVEL_GARBAGE_YES = 1 ;
	
	static final int SET_LEVEL_LOCK_NO = 0 ;
	static final int SET_LEVEL_LOCK_YES = 1 ;
	
	static final int SET_DISPLACEMENT_FIXED_RATE_NO = 0 ;
	static final int SET_DISPLACEMENT_FIXED_RATE_PRACTICE = 1 ;
	static final int SET_DISPLACEMENT_FIXED_RATE_YES = 2 ;
	
	static final int SET_DIFFICULTY_NO = 0 ;
	static final int SET_DIFFICULTY_NO_PRACTICE = 1 ;		// normal / hard.
	static final int SET_DIFFICULTY_YES = 2 ;
	
	static final int LEVEL_UP_SMOOTH = 0 ;
	static final int LEVEL_UP_BREAK = 1 ;
	
	
	static final int CLASS_CODE_ENDURANCE = 0 ;
	static final int CLASS_CODE_PROGRESSION = 1 ;
	static final int CLASS_CODE_FLOOD = 2 ;
	static final int CLASS_CODE_SPECIAL = 3 ;
	
	
	
	// Tag names!
	private static final String TAG_NAME_MODE = "Mode" ;
	// Important stuff
	private static final String TAG_NAME_Q_INTERACTIONS = "QInteractions" ;
	private static final String TAG_NAME_PIECE_BAG = "PieceBag" ;
	private static final String TAG_NAME_RESERVE_BAG = "ReserveBag" ;
	// Systems
	private static final String TAG_NAME_ATTACK_SYSTEM = "AttackSystem" ;
	private static final String TAG_NAME_CLEAR_SYSTEM = "ClearSystem" ;
	private static final String TAG_NAME_COLLISION_SYSTEM = "CollisionSystem" ;
	private static final String TAG_NAME_DISPLACEMENT_SYSTEM = "DisplacementSystem" ;
	private static final String TAG_NAME_KICK_SYSTEM = "KickSystem" ;
	private static final String TAG_NAME_LEVEL_SYSTEM = "LevelSystem" ;
	private static final String TAG_NAME_LOCK_SYSTEM = "LockSystem" ;
	private static final String TAG_NAME_METAMORPHOSIS_SYSTEM = "MetamorphosisSystem" ;
	private static final String TAG_NAME_ROTATION_SYSTEM = "RotationSystem" ;
	private static final String TAG_NAME_SCORE_SYSTEM = "ScoreSystem" ;
	private static final String TAG_NAME_SPECIAL_SYSTEM = "SpecialSystem" ;
	private static final String TAG_NAME_TIMING_SYSTEM = "TimingSystem" ;
	private static final String TAG_NAME_TRIGGER_SYSTEM = "TriggerSystem" ;
	private static final String TAG_NAME_VALLEY_SYSTEM = "ValleySystem" ;
	// System internal nodes
	private static final String TAG_NAME_DISPLACEMENT_SYSTEM_ROWS_PER_SECOND = "displacement_system_rows_per_second" ;
	private static final String TAG_NAME_DISPLACEMENT_SYSTEM_ROWS_PER_SECOND_ACCEL = "displacement_system_rows_per_second_accel" ;
	private static final String TAG_NAME_DISPLACEMENT_SYSTEM_ROWS_PER_SECOND_DECEL = "displacement_system_rows_per_second_decel" ;
	private static final String TAG_NAME_SCORE_SYSTEM_CLEAR_MULTIPLIER = "score_system_clear_multiplier" ;
	private static final String TAG_NAME_SPECIAL_SYSTEM_CLEARS_ = "special_system_points_per_clear" ;
	private static final String TAG_NAME_TIMING_SYSTEM_ENTER_DELAY = "timing_system_enter_delay" ;
	private static final String TAG_NAME_TIMING_SYSTEM_FALL_DELAY = "timing_system_fall_delay" ;
	private static final String TAG_NAME_TIMING_SYSTEM_LOCK_DELAY = "timing_system_lock_delay" ;
	private static final String TAG_NAME_TIMING_SYSTEM_FAST_FALL_DELAY = "timing_system_fast_fall_delay" ;
	private static final String TAG_NAME_TIMING_SYSTEM_ENTER_DELAY_HARD_MODE = "timing_system_enter_delay_hard_mode" ;
	private static final String TAG_NAME_TIMING_SYSTEM_FALL_DELAY_HARD_MODE = "timing_system_fall_delay_hard_mode" ;
	private static final String TAG_NAME_TIMING_SYSTEM_LOCK_DELAY_HARD_MODE = "timing_system_lock_delay_hard_mode" ;
	private static final String TAG_NAME_TIMING_SYSTEM_FAST_FALL_DELAY_HARD_MODE = "timing_system_fast_fall_delay_hard_mode" ;
	
	// Arrays, formulae, etc.
	private static final String TAG_NAME_ARRAY_BY_LEVEL = "array_by_level" ;
	private static final String TAG_NAME_CONSTANT = "constant" ;
	private static final String TAG_NAME_LINEAR_BY_LEVEL = "linear_by_level" ;
	private static final String TAG_NAME_LINEAR_BY_LEVEL_PINNING = "linear_by_level_pinning" ;
	private static final String TAG_NAME_ARRAY_BY_CLEAR = "array_by_clear" ;
	private static final String TAG_NAME_QUADRATIC = "quadratic" ;
	
	
	// Attribute names!
	private static final String ATTRIBUTE_NAME_NAME = "name" ;
	private static final String ATTRIBUTE_NAME_SHORT_NAME = "short_name" ;
	private static final String ATTRIBUTE_NAME_CLASS_NAME = "class_name" ;
	private static final String ATTRIBUTE_NAME_CLASS_CODE = "class_code" ;
	
	
	// "name" is generic
	private static final String ATTRIBUTE_NAME_MODE_DESCRIPTION = "description" ;
	private static final String ATTRIBUTE_NAME_MODE_SHORT_DESCRIPTION = "short_description" ;
	private static final String ATTRIBUTE_NAME_MODE_PLAYERS = "players" ;
	private static final String ATTRIBUTE_NAME_MODE_MAX_LEVEL = "max_level" ;
	private static final String ATTRIBUTE_NAME_MODE_MAX_STARTING_LEVEL = "max_starting_level" ;
	private static final String ATTRIBUTE_NAME_MODE_ROWS = "rows" ;
	private static final String ATTRIBUTE_NAME_MODE_COLS = "cols" ;
	private static final String ATTRIBUTE_NAME_MODE_QPANES = "qpanes" ;
	private static final String ATTRIBUTE_NAME_MODE_RESERVE = "reserve" ;
	private static final String ATTRIBUTE_NAME_MODE_SET_CLEARS = "set_clears" ;
	private static final String ATTRIBUTE_NAME_MODE_SET_STARTING_GARBAGE = "set_starting_garbage" ;
	private static final String ATTRIBUTE_NAME_MODE_SET_PER_LEVEL_GARBAGE = "set_per_level_garbage" ;
	private static final String ATTRIBUTE_NAME_MODE_DEFAULT_GARBAGE = "default_garbage" ;
	private static final String ATTRIBUTE_NAME_MODE_SET_LEVEL_LOCK = "set_level_lock" ;
	private static final String ATTRIBUTE_NAME_MODE_SET_DISPLACEMENT_FIXED_RATE = "set_displacement_fixed_rate" ;
	private static final String ATTRIBUTE_NAME_MODE_SET_DIFFICULTY = "set_difficulty" ;
	private static final String ATTRIBUTE_NAME_MODE_LEVEL_UP = "level_up" ;
	
	
	
	// Class Codes.
	private static final String TAG_VALUE_CLASS_CODE_ENDURANCE = "Endurance" ;
	private static final String TAG_VALUE_CLASS_CODE_PROGRESSION = "Progression" ;
	private static final String TAG_VALUE_CLASS_CODE_FLOOD = "Flood" ;
	private static final String TAG_VALUE_CLASS_CODE_SPECIAL = "Special" ;
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// Specific System names.
	// qinteractions
	private static final String TAG_VALUE_Q_INTERACTIONS_BASIC = "QInteractionsBasic" ;
	// piece bags
	private static final String TAG_VALUE_PIECE_BAG_RETRO = "RetroPieceBag" ;
	private static final String TAG_VALUE_PIECE_BAG_BINOMIAL_TETRACUBE = "BinomialTetracubePieceBag" ;
	private static final String TAG_VALUE_PIECE_BAG_UNIFORM_SPECIAL = "UniformSpecialPieceBag" ;
	// systems
	// attack system
	private static final String TAG_VALUE_ATTACK_SYSTEM_RETRO_VERSUS = "RetroVersusAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_RETRO_VERSUS_GRAVITY = "RetroVersusGravityAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_RETRO_VERSUS_FLOOD = "RetroVersusFloodingAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_RETRO_FREE_4_ALL = "RetroFree4AllAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_RETRO_FREE_4_ALL_FLOOD = "RetroFree4AllFloodingAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_QUANTRO_VERSUS = "QuantroVersusAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_QUANTRO_VERSUS_NO_GARBAGE = "QuantroVersusAttackSystemNoGarbage" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_QUANTRO_VERSUS_FLOOD = "QuantroVersusFloodingAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_QUANTRO_FREE_4_ALL = "QuantroFree4AllAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_QUANTRO_FREE_4_ALL_FLOOD = "QuantroFree4AllFloodingAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_NO = "NoAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_INITIAL_GARBAGE_RETRO = "InitialGarbageRetroAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_INITIAL_GARBAGE_QUANTRO = "InitialGarbageQuantroAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_LEVEL_GARBAGE_RETRO = "LevelGarbageRetroAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_LEVEL_GARBAGE_QUANTRO = "LevelGarbageQuantroAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_MASOCHIST_RETRO = "MasochistRetroAttackSystem" ;
	private static final String TAG_VALUE_ATTACK_SYSTEM_MASOCHIST_QUANTRO = "MasochistQuantroAttackSystem" ;
	// clear system
	private static final String TAG_VALUE_CLEAR_SYSTEM_QUANTRO = "QuantroClearSystem" ;
	// collision system
	private static final String TAG_VALUE_COLLISION_SYSTEM_EARLY = "EarlyCollisionSystem" ;
	// displacement system
	private static final String TAG_VALUE_DISPLACEMENT_SYSTEM_DEAD = "DeadDisplacementSystem" ;
	private static final String TAG_VALUE_DISPLACEMENT_SYSTEM_REAL_TIME = "RealTimeDisplacementSystem" ;
	private static final String TAG_VALUE_DISPLACEMENT_SYSTEM_ABSOLUTE_TIME = "AbsoluteTimeDisplacementSystem" ;
	// kick system
	private static final String TAG_VALUE_KICK_SYSTEM_EARLY = "EarlyKickSystem" ;
	private static final String TAG_VALUE_KICK_SYSTEM_LEANING = "LeaningKickSystem" ;
	// level system
	private static final String TAG_VALUE_LEVEL_SYSTEM_CUSTOM = "CustomLevelSystem" ;
	// lock system
	private static final String TAG_VALUE_LOCK_SYSTEM_EARLY = "EarlyLockSystem" ;
	// metamorphosis system
	private static final String TAG_VALUE_METAMORPHOSIS_SYSTEM_QUANTRO = "QuantroMetamorphosisSystem" ;
	private static final String TAG_VALUE_METAMORPHOSIS_SYSTEM_DEAD = "DeadMetamorphosisSystem" ;
	// rotation system
	private static final String TAG_VALUE_ROTATION_SYSTEM_TETROMINO = "TetrominoRotationSystem" ;
	private static final String TAG_VALUE_ROTATION_SYSTEM_TETRACUBE = "TetracubeRotationSystem" ;
	private static final String TAG_VALUE_ROTATION_SYSTEM_UNIVERSAL = "UniversalRotationSystem" ;
	// score system
	private static final String TAG_VALUE_SCORE_SYSTEM_TRIGGERABLE_LEVEL = "TriggerableLevelScoreSystem" ;
	// special system
	private static final String TAG_VALUE_SPECIAL_SYSTEM_DEAD = "DeadSpecialSystem" ;
	private static final String TAG_VALUE_SPECIAL_SYSTEM_ATTACK_3D = "Attack3DSpecialSystem" ;
	private static final String TAG_VALUE_SPECIAL_SYSTEM_RESERVE_PUSH_DOWN = "ReservePushDownSpecialSystem" ;
	// timing system
	private static final String TAG_VALUE_TIMING_SYSTEM_CUSTOM = "CustomTimingSystem" ;
	// trigger system
	private static final String TAG_VALUE_TRIGGER_SYSTEM_PROTOTYPING = "PrototypingTriggerSystem" ;
	private static final String TAG_VALUE_TRIGGER_SYSTEM_QUANTRO_SINGLE_PLAYER = "QuantroSinglePlayerTriggerSystem" ;
	private static final String TAG_VALUE_TRIGGER_SYSTEM_RETRO_SINGLE_PLAYER = "RetroSinglePlayerTriggerSystem" ;
	// valley system
	private static final String TAG_VALUE_VALLEY_SYSTEM_NO = "NoValleySystem" ;
	private static final String TAG_VALUE_VALLEY_SYSTEM_QUANTRO = "QuantroValleySystem" ;
	private static final String TAG_VALUE_VALLEY_SYSTEM_QUANTRO_UNSTABLE = "QuantroUnstableValleySystem" ;
	private static final String TAG_VALUE_VALLEY_SYSTEM_RETRO = "RetroValleySystem" ;
	private static final String TAG_VALUE_VALLEY_SYSTEM_QUANTRO_3D = "Quantro3DValleySystem" ;
	//
	////////////////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////////////////
	// system parameter strings
	private static final String PARAM_BINOMIAL_PIECE_BAG_C = "C" ;
	private static final String PARAM_BINOMIAL_PIECE_BAG_N = "N" ;
	private static final String PARAM_BINOMIAL_PIECE_BAG_P = "P" ;
	private static final String TAG_PIECE_BAG_PIECES = "pieces" ;
	private static final String TAG_DISPLACEMENT_SYSTEM_DISPLACEMENT = "displacement" ;
	private static final String TAG_LEVEL_SYSTEM_LEVEL_UP = "level_up" ;
	private static final String TAG_LEVEL_SYSTEM_CLEARS = "clears" ;
	private static final String TAG_LEVEL_SYSTEM_CLEARS_SINCE_LAST = "clears_since_last" ;
	private static final String TAG_LEVEL_SYSTEM_CLEAR = "clear" ;
	private static final String TAG_ATTACK_SYSTEM_BLOCKS = "blocks" ;
	private static final String PARAM_ATTACK_SYSTEM_BLOCKS_VALLEY = "valley" ;
	private static final String PARAM_ATTACK_SYSTEM_BLOCKS_JUNCTION = "junction" ;
	private static final String PARAM_ATTACK_SYSTEM_BLOCKS_PEAK = "peak" ;
	private static final String PARAM_ATTACK_SYSTEM_BLOCKS_CORNER = "corner" ;
	private static final String PARAM_ATTACK_SYSTEM_BLOCKS_TROLL = "troll" ;
	
	private static final String PARAM_DISPLACEMENT_SYSTEM_ACCEL = "accel" ;
	private static final String PARAM_DISPLACEMENT_SYSTEM_DECEL = "decel" ;
	private static final String PARAM_DISPLACEMENT_DIFFICULTY = "difficulty" ;
	
	
	private static final String PARAM_LEVEL_SYSTEM_CLEAR_TYPE = "type" ;
	private static final String PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_ANY = "any" ;
	private static final String PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_MO = "mo" ;
	private static final String PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_S0 = "s0" ;
	private static final String PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_S1 = "s1" ;
	private static final String PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_SS = "ss" ;
	private static final String PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_SS_AND_MO = "ss_and_mo" ;
	private static final String PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_TOTAL = "total" ;
	private static final String PARAM_SCORE_SYSTEM_DECREMENT = "decrement" ;
	private static final String PARAM_SCORE_SYSTEM_ROUND = "round" ;
	private static final String PARAM_SCORE_SYSTEM_MAXIMUM_GRACE_MULTIPLIER_PRODUCT = "maximum_grace_multiplier_product" ;
	private static final String PARAM_SCORE_SYSTEM_CHARITY_SLOPE = "charity_slope" ;
	private static final String PARAM_SCORE_SYSTEM_CHARITY_INTERCEPT = "charity_intercept" ;
	private static final String PARAM_SPECIAL_SYSTEM_MAX_BLOCKS = "max_blocks" ;
	private static final String PARAM_SPECIAL_SYSTEM_BLOCKS = "blocks" ;
	private static final String PARAM_SPECIAL_SYSTEM_POINTS_PER_BLOCK = "points_per_block" ;
	private static final String PARAM_TRIGGER_SYSTEM_SCORE_MULTIPLIER_INCREASE_COMPLEX_CLEAR = "score_multiplier_increase_complex_clear" ;
	private static final String PARAM_TRIGGER_SYSTEM_SCORE_MULTIPLIER_INCREASE_SPECIAL_PIECE = "score_multiplier_increase_special_piece" ;
	//
	private static final String ARRAY_ARRAY = "array" ;
	private static final String ARRAY_ELEMENT = "element" ;
	private static final String ARRAY_INDEX = "index" ;
	private static final String ARRAY_VALUE = "value" ;
	private static final String ARRAY_SLOPE = "slope" ;
	private static final String ARRAY_INTERCEPT = "intercept" ;
	private static final String ARRAY_VAL = "val" ;
	private static final String ARRAY_PIN = "pin" ;
	private static final String QUADRATIC_A = "a" ;
	private static final String QUADRATIC_B = "b" ;
	private static final String QUADRATIC_C = "c" ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	

	
	// Basic information about the game mode, extracted from
	// the document and processed.
	
	public String name ;
	public String shortName ;
	public String className ;
	public int classCode ;
	public String description ;
	public String shortDescription ;
	public String URL ;
	public int playersMin ;
	public int playersMax ;
	public int maxLevel ;
	public int maxStartingLevel ;
	public int rows ;
	public int cols ;
	public int qpanes ;
	public int reserve ;
	public int setClears ;
	public int setStartingGarbage ;
	public int setPerLevelGarbage ;
	public int defaultGarbage ;
	public int setLevelLock ;
	public int setDisplacementFixedRate ;
	public int setDifficulty ;
	public int levelUp ;
	
	// More complex information about the game mode.  We extract the XML strings
	// and process them into specific class labels and additional info.  The
	// idea is to be able to release the DOC as soon as possible.
	String mNameQInteractions ;
	String mNamePieceBag ;
	int mParamPieceBagC ;
	int mParamPieceBagN ;
	float mParamPieceBagP ;
	String mNameReserveBag ;
	int [] mPieceBagPieces ;
	int [] mReserveBagPieces ;
	String mNameAttackSystem ;
	int mAttackSystemBlocksValley ;
	int mAttackSystemBlocksJunction ;
	int mAttackSystemBlocksPeak ;
	int mAttackSystemBlocksCorner ;
	int mAttackSystemBlocksTroll ;
	String mNameClearSystem ;
	String mNameCollisionSystem ;
	String mNameDisplacementSystem ;
	SimulatedArray mDisplacementSystemRowsPerSecondArray ;
	SimulatedArray mDisplacementSystemRowsPerSecondAccelArray ;
	SimulatedArray mDisplacementSystemRowsPerSecondDecelArray ;
	Function mDisplacementSystemRowsAtSecondFunctionNormal ;
	Function mDisplacementSystemRowsAtSecondFunctionHard ;
	double mDisplacementSystemRowsPerSecondAccelDouble ;
	double mDisplacementSystemRowsPerSecondDecelDouble ;
	String mNameKickSystem ;
	String mNameLevelSystem ;
	// these three arrays are "linked"
	ArrayList<ArrayList<Integer>> mLevelSystemConditionTypes ;
	ArrayList<ArrayList<Integer>> mLevelSystemConditionSubTypes ;
	ArrayList<ArrayList<SimulatedArray>> mLevelSystemConditionLevelArray ;
	// those are the linked ones
	String mNameLockSystem ;
	String mNameMetamorphosisSystem ;
	String mNameRotationSystem ;
	String mNameScoreSystem ;
	double mScoreSystemDecrement ;
	double mScoreSystemRound ;
	double mScoreSystemMaxMultGrace ;
	double mScoreSystemCharitySlope ;
	double mScoreSystemCharityIntercept ;
	SimulatedArray mScoreSystemLevelIntArray ;
	String mNameSpecialSystem ;
	int mSpecialSystemMaxBlocks ;
	int mSpecialSystemBlocks ;
	int mSpecialSystemPointsPerBlock ;
	SimulatedArray mSpecialSystemPointsPerClearArray ;
	String mNameTimingSystem ;
	SimulatedArray mTimingSystemEnterDelayArray ;
	SimulatedArray mTimingSystemFallDelayArray ;
	SimulatedArray mTimingSystemLockDelayArray ;
	SimulatedArray mTimingSystemFastFallDelayArray ;
	SimulatedArray mTimingSystemEnterDelayArrayHardMode ;
	SimulatedArray mTimingSystemFallDelayArrayHardMode ;
	SimulatedArray mTimingSystemLockDelayArrayHardMode ;
	SimulatedArray mTimingSystemFastFallDelayArrayHardMode ;
	
	String mNameTriggerSystem ;
	double mTriggerSystemIncreaseComplexClear ;
	double mTriggerSystemIncreaseSpecialPiece ;
	String mNameValleySystem ;
	
	
	////////////////////////
	// CUSTOMIZATION?
	CustomGameModeSettings mCustomGameModeSettings ;
	
	
	// cloneable - you can get one from another.
	@Override
	protected Object clone() {
		GameMode gm = new GameMode() ;
		
		gm.name = name ;
		gm.shortName = shortName ;
		gm.className = className ;
		gm.classCode = classCode ;
		gm.description = description ;
		gm.shortDescription = shortDescription ;
		gm.URL = URL ;
		gm.playersMin = playersMin ;
		gm.playersMax = playersMax ;
		gm.maxLevel = maxLevel ;
		gm.maxStartingLevel = maxStartingLevel ;
		gm.rows = rows ;
		gm.cols = cols ;
		gm.qpanes = qpanes ;
		gm.reserve = reserve ;
		gm.setClears = setClears ;
		gm.setStartingGarbage = setStartingGarbage ;
		gm.setPerLevelGarbage = setPerLevelGarbage ;
		gm.defaultGarbage = defaultGarbage ;
		gm.setLevelLock = setLevelLock ;
		gm.setDisplacementFixedRate = setDisplacementFixedRate ;
		gm.levelUp = levelUp ;
		
		// More complex information about the game mode.  We extract the XML strings
		// and process them into specific class labels and additional info.  The
		// idea is to be able to release the DOC as soon as possible.
		gm.mNameQInteractions = mNameQInteractions ;
		gm.mNamePieceBag = mNamePieceBag ;
		gm.mParamPieceBagC = mParamPieceBagC ;
		gm.mParamPieceBagN = mParamPieceBagN ;
		gm.mParamPieceBagP = mParamPieceBagP ;
		gm.mNameReserveBag = mNameReserveBag ;
		gm.mPieceBagPieces = ArrayOps.copyInto( mPieceBagPieces, null ) ;
		gm.mReserveBagPieces = ArrayOps.copyInto( mReserveBagPieces, null ) ;
		gm.mNameAttackSystem = mNameAttackSystem ;
		gm.mAttackSystemBlocksValley = mAttackSystemBlocksValley ;
		gm.mAttackSystemBlocksJunction = mAttackSystemBlocksJunction ;
		gm.mAttackSystemBlocksPeak = mAttackSystemBlocksPeak ;
		gm.mAttackSystemBlocksCorner = mAttackSystemBlocksCorner ;
		gm.mAttackSystemBlocksTroll = mAttackSystemBlocksTroll ;
		gm.mNameClearSystem = mNameClearSystem ;
		gm.mNameCollisionSystem = mNameCollisionSystem ;
		gm.mNameDisplacementSystem = mNameDisplacementSystem ;
		gm.mDisplacementSystemRowsPerSecondArray = SimulatedArrayFactory.copy( mDisplacementSystemRowsPerSecondArray ) ;
		gm.mDisplacementSystemRowsPerSecondAccelArray = SimulatedArrayFactory.copy( mDisplacementSystemRowsPerSecondAccelArray ) ;
		gm.mDisplacementSystemRowsPerSecondDecelArray = SimulatedArrayFactory.copy( mDisplacementSystemRowsPerSecondDecelArray ) ;
		gm.mDisplacementSystemRowsAtSecondFunctionNormal = mDisplacementSystemRowsAtSecondFunctionNormal == null ? null : mDisplacementSystemRowsAtSecondFunctionNormal.copy() ;
		gm.mDisplacementSystemRowsAtSecondFunctionHard = mDisplacementSystemRowsAtSecondFunctionHard == null ? null : mDisplacementSystemRowsAtSecondFunctionHard.copy() ;
		gm.mDisplacementSystemRowsPerSecondAccelDouble = mDisplacementSystemRowsPerSecondAccelDouble ;
		gm.mDisplacementSystemRowsPerSecondDecelDouble = mDisplacementSystemRowsPerSecondDecelDouble ;
		gm.mNameKickSystem = mNameKickSystem ;
		gm.mNameLevelSystem = mNameLevelSystem ;
		// these three arrays are "linked"
		gm.mLevelSystemConditionTypes = copyALALInteger( mLevelSystemConditionTypes ) ;
		gm.mLevelSystemConditionSubTypes = copyALALInteger( mLevelSystemConditionSubTypes ) ;
		gm.mLevelSystemConditionLevelArray = copyALALSimulatedArray( mLevelSystemConditionLevelArray ) ;
		// those are the linked ones
		gm.mNameLockSystem = mNameLockSystem ;
		gm.mNameMetamorphosisSystem = mNameMetamorphosisSystem ;
		gm.mNameRotationSystem = mNameRotationSystem ;
		gm.mNameScoreSystem = mNameScoreSystem ;
		gm.mNameSpecialSystem = mNameSpecialSystem ;
		gm.mScoreSystemDecrement = mScoreSystemDecrement ;
		gm.mScoreSystemRound = mScoreSystemRound ;
		gm.mScoreSystemMaxMultGrace = mScoreSystemMaxMultGrace ;
		gm.mScoreSystemCharitySlope = mScoreSystemCharitySlope ;
		gm.mScoreSystemCharityIntercept = mScoreSystemCharityIntercept ;
		gm.mScoreSystemLevelIntArray = SimulatedArrayFactory.copy( mScoreSystemLevelIntArray ) ;
		gm.mNameSpecialSystem = mNameSpecialSystem ;
		gm.mSpecialSystemMaxBlocks = mSpecialSystemMaxBlocks ;
		gm.mSpecialSystemBlocks = mSpecialSystemBlocks ;
		gm.mSpecialSystemPointsPerBlock = mSpecialSystemPointsPerBlock ;
		gm.mNameTimingSystem = mNameTimingSystem ;
		gm.mTimingSystemEnterDelayArray = SimulatedArrayFactory.copy( mTimingSystemEnterDelayArray ) ;
		gm.mTimingSystemFallDelayArray = SimulatedArrayFactory.copy( mTimingSystemFallDelayArray ) ;
		gm.mTimingSystemLockDelayArray = SimulatedArrayFactory.copy( mTimingSystemLockDelayArray ) ;
		gm.mTimingSystemFastFallDelayArray = SimulatedArrayFactory.copy( mTimingSystemFastFallDelayArray ) ;
		gm.mTimingSystemEnterDelayArrayHardMode = SimulatedArrayFactory.copy( mTimingSystemEnterDelayArrayHardMode ) ;
		gm.mTimingSystemFallDelayArrayHardMode = SimulatedArrayFactory.copy( mTimingSystemFallDelayArrayHardMode ) ;
		gm.mTimingSystemLockDelayArrayHardMode = SimulatedArrayFactory.copy( mTimingSystemLockDelayArrayHardMode ) ;
		gm.mTimingSystemFastFallDelayArrayHardMode = SimulatedArrayFactory.copy( mTimingSystemFastFallDelayArrayHardMode ) ;
		gm.mNameTriggerSystem = mNameTriggerSystem ;
		gm.mTriggerSystemIncreaseComplexClear = mTriggerSystemIncreaseComplexClear ;
		gm.mTriggerSystemIncreaseSpecialPiece = mTriggerSystemIncreaseSpecialPiece ;
		gm.mNameValleySystem = mNameValleySystem ;
		
		
		////////////////////////
		// CUSTOMIZATION?
		gm.mCustomGameModeSettings = null ;
		if ( mCustomGameModeSettings != null )
			gm.mCustomGameModeSettings = (CustomGameModeSettings)mCustomGameModeSettings.clone() ;
		
		return gm ;
	}
	
	private ArrayList<ArrayList<Integer>> copyALALInteger( ArrayList<ArrayList<Integer>> src ) {
		ArrayList<ArrayList<Integer>> dst = new ArrayList<ArrayList<Integer>>() ;
		for ( int i = 0; i < src.size(); i++ ) {
			ArrayList<Integer> src_inner = src.get(i) ;
			ArrayList<Integer> dst_inner = new ArrayList<Integer>() ;
			for ( int j = 0; j < src_inner.size(); j++ )
				dst_inner.add( src_inner.get(j) ) ;
			dst.add( dst_inner ) ;
		}
		
		return dst ;
	}
	
	private ArrayList<ArrayList<SimulatedArray>> copyALALSimulatedArray( ArrayList<ArrayList<SimulatedArray>> src ) {
		ArrayList<ArrayList<SimulatedArray>> dst = new ArrayList<ArrayList<SimulatedArray>>() ;
		for ( int i = 0; i < src.size(); i++ ) {
			ArrayList<SimulatedArray> src_inner = src.get(i) ;
			ArrayList<SimulatedArray> dst_inner = new ArrayList<SimulatedArray>() ;
			for ( int j = 0; j < src_inner.size(); j++ )
				dst_inner.add( SimulatedArrayFactory.copy( src_inner.get(j) ) ) ;
			dst.add( dst_inner ) ;
		}
		
		return dst ;
	}
	
	
	
	private GameMode() {
		// empty
	}
	
	
	// Constructor requires an XML document.
	GameMode( InputStream in ) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc ;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder() ;
			doc = builder.parse(in) ;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new RuntimeException(e) ;
		} catch (SAXException e) {
			e.printStackTrace();
			throw new RuntimeException(e) ;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e) ;
		}
		
		// We have loaded the document.  Retrieve a few useful things...
		Node modeNode = doc.getElementsByTagName(TAG_NAME_MODE).item(0) ;
		NamedNodeMap modeAttrs = modeNode.getAttributes() ;
		
		this.name = modeAttrs.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
		this.shortName = modeAttrs.getNamedItem(ATTRIBUTE_NAME_SHORT_NAME).getNodeValue() ;
		this.className = modeAttrs.getNamedItem(ATTRIBUTE_NAME_CLASS_NAME).getNodeValue() ;
		this.classCode = parseClassCode( modeAttrs.getNamedItem(ATTRIBUTE_NAME_CLASS_CODE).getNodeValue() ) ;
		this.description = modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_DESCRIPTION).getNodeValue() ;
		this.shortDescription = modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_SHORT_DESCRIPTION).getNodeValue() ;
		this.maxLevel = Integer.parseInt( modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_MAX_LEVEL).getNodeValue() ) ;
		this.maxStartingLevel = Integer.parseInt( modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_MAX_STARTING_LEVEL).getNodeValue() ) ;
		this.rows = Integer.parseInt( modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_ROWS).getNodeValue() ) ;
		this.cols = Integer.parseInt( modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_COLS).getNodeValue() ) ;
		this.qpanes = Integer.parseInt( modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_QPANES).getNodeValue() ) ;
		
		String playersString = modeAttrs.getNamedItem(ATTRIBUTE_NAME_MODE_PLAYERS).getNodeValue() ;
		String [] playersMinMax = playersString.split(":") ;
		if ( playersMinMax.length == 1 ) {
			this.playersMin = this.playersMax = Integer.parseInt(playersMinMax[0]) ;
		} else {
			this.playersMin = Integer.parseInt(playersMinMax[0]) ;
			this.playersMax = Integer.parseInt(playersMinMax[1]) ;
		}
		if ( playersMin <= 1 && playersMax > 1 ) {
			throw new IllegalArgumentException("No game mode can be both single-player (1 player) and multiplayer (> 1).") ;
		}
		
		String reserveStr = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_RESERVE ).getNodeValue() ;
		if ( reserveStr.equals("swap"))
			this.reserve = RESERVE_BEHAVIOR_SWAP ;
		else if ( reserveStr.equals("insert"))
			this.reserve = RESERVE_BEHAVIOR_INSERT ;
		else if ( reserveStr.equals("swap_reenter") )
			this.reserve = RESERVE_BEHAVIOR_SWAP_REENTER ;
		else if ( reserveStr.equals("insert_reenter") )
			this.reserve = RESERVE_BEHAVIOR_INSERT_REENTER ;
		else if ( reserveStr.equals("special") )
			this.reserve = RESERVE_BEHAVIOR_SPECIAL ;
		else
			this.reserve = RESERVE_BEHAVIOR_UNKNOWN ;
		
		String setClearsStr = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_SET_CLEARS ).getNodeValue() ;
		if ( setClearsStr.equals("false") || setClearsStr.equals("no") )
			this.setClears = SET_CLEAR_NO ;
		else if ( setClearsStr.equals("any") )
			this.setClears = SET_CLEAR_ANY ;
		else if ( setClearsStr.equals("total") )
			this.setClears = SET_CLEAR_TOTAL ;
		else
			this.setClears = SET_CLEAR_NO ;
		
		String setStartingGarbageStr = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_SET_STARTING_GARBAGE ).getNodeValue();
		if ( setStartingGarbageStr.equals("false") || setStartingGarbageStr.equals("no") )
			this.setStartingGarbage = SET_STARTING_GARBAGE_NO ;
		else if ( setStartingGarbageStr.equals("true") || setStartingGarbageStr.equals("yes") )
			this.setStartingGarbage = SET_STARTING_GARBAGE_YES ;
			
		String setPerLevelGarbageStr = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_SET_PER_LEVEL_GARBAGE ).getNodeValue();
		if ( setPerLevelGarbageStr.equals("false") || setPerLevelGarbageStr.equals("no") )
			this.setPerLevelGarbage = SET_PER_LEVEL_GARBAGE_NO ;
		else if ( setPerLevelGarbageStr.equals("true") || setPerLevelGarbageStr.equals("yes") )
			this.setPerLevelGarbage = SET_PER_LEVEL_GARBAGE_YES ;
			
		this.defaultGarbage = Integer.parseInt( modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_DEFAULT_GARBAGE ).getNodeValue() ) ;
		
		String setLevelLock = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_SET_LEVEL_LOCK ).getNodeValue() ;
		if ( setLevelLock.equals("false") || setLevelLock.equals("no") )
			this.setLevelLock = SET_LEVEL_LOCK_NO ;
		else if ( setLevelLock.equals("true") || setLevelLock.equals("yes") )
			this.setLevelLock = SET_LEVEL_LOCK_YES ;
		
		String displacementFixedRateStr = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_SET_DISPLACEMENT_FIXED_RATE ).getNodeValue() ;
		if ( displacementFixedRateStr.equals("false") || displacementFixedRateStr.equals("no") )
			this.setDisplacementFixedRate = SET_DISPLACEMENT_FIXED_RATE_NO ;
		else if ( displacementFixedRateStr.equals("practice") || displacementFixedRateStr.equals("practice_only") )
			this.setDisplacementFixedRate = SET_DISPLACEMENT_FIXED_RATE_PRACTICE ;
		else if ( displacementFixedRateStr.equals("true") || displacementFixedRateStr.equals("yes") )
			this.setDisplacementFixedRate = SET_DISPLACEMENT_FIXED_RATE_YES ;
		
		String setDifficultyStr = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_SET_DIFFICULTY ).getNodeValue() ;
		if ( setDifficultyStr.equals("false") || setDifficultyStr.equals("no") )
			this.setDifficulty = SET_DIFFICULTY_NO ;
		else if ( setDifficultyStr.equals("no_practice") )
			this.setDifficulty = SET_DIFFICULTY_NO_PRACTICE ;
		else if ( setDifficultyStr.equals("true") || setDifficultyStr.equals("yes") )
			this.setDifficulty = SET_DIFFICULTY_YES ;
		
		
		
		String levelUpStr = modeAttrs.getNamedItem( ATTRIBUTE_NAME_MODE_LEVEL_UP ).getNodeValue() ;
		if ( levelUpStr.equals("break") )
			this.levelUp = LEVEL_UP_BREAK ;
		else
			this.levelUp = LEVEL_UP_SMOOTH ;
		
		
		// Read content of this doc to get system names and such!
		readFromDocumentQInteractions(doc) ;
		readFromDocumentPieceBag(doc) ;
		readFromDocumentReserveBag(doc) ;
		readFromDocumentAttackSystem(doc) ;
		readFromDocumentClearSystem(doc) ;
		readFromDocumentCollisionSystem(doc) ;
		readFromDocumentDisplacementSystem(doc) ;
		readFromDocumentKickSystem(doc) ;
		readFromDocumentLevelSystem(doc) ;
		readFromDocumentLockSystem(doc) ;
		readFromDocumentMetamorphosisSystem(doc) ;
		readFromDocumentRotationSystem(doc) ;
		readFromDocumentScoreSystem(doc) ;
		readFromDocumentSpecialSystem(doc) ;
		readFromDocumentTimingSystem(doc) ;
		readFromDocumentTriggerSystem(doc) ;
		readFromDocumentValleySystem(doc) ;
		
		mCustomGameModeSettings = null ;
	}
	
	
	int parseClassCode( String code ) {
		if ( TAG_VALUE_CLASS_CODE_ENDURANCE.equals(code) )
			return CLASS_CODE_ENDURANCE ;
		if ( TAG_VALUE_CLASS_CODE_PROGRESSION.equals(code) )
			return CLASS_CODE_PROGRESSION ;
		if ( TAG_VALUE_CLASS_CODE_FLOOD.equals(code) )
			return CLASS_CODE_FLOOD ;
		if ( TAG_VALUE_CLASS_CODE_SPECIAL.equals(code) )
			return CLASS_CODE_SPECIAL ;
		
		throw new IllegalArgumentException("Don't recognize class code string " + code) ;
	}
	
	
	void apply( CustomGameModeSettings cgms ) {
		name = cgms.getName() + " " + className ;
		shortName = cgms.getName() ;
		shortDescription = cgms.getSummary() ;
		description = cgms.getDescription() ;
		
		rows = cgms.getRows() ;
		cols = cgms.getCols() ;
		
		mCustomGameModeSettings = cgms ;
	}
	
	
	
	private void readFromDocumentQInteractions( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_Q_INTERACTIONS).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameQInteractions = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	private void readFromDocumentPieceBag( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_PIECE_BAG).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNamePieceBag = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
		
		if ( TAG_VALUE_PIECE_BAG_BINOMIAL_TETRACUBE.equals(mNamePieceBag) ) {
			mParamPieceBagC = Integer.parseInt( systemAttributes.getNamedItem(PARAM_BINOMIAL_PIECE_BAG_C).getNodeValue() ) ;
			mParamPieceBagN = Integer.parseInt( systemAttributes.getNamedItem(PARAM_BINOMIAL_PIECE_BAG_N).getNodeValue() ) ;
			mParamPieceBagP = Float.parseFloat( systemAttributes.getNamedItem(PARAM_BINOMIAL_PIECE_BAG_P).getNodeValue() ) ;
		} else {
			mParamPieceBagC = 0 ;
			mParamPieceBagN = 0 ;
			mParamPieceBagP = 0 ;
		}
		
		mPieceBagPieces = readPieces( systemNode ) ;
	}
	
	private void readFromDocumentReserveBag( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_RESERVE_BAG).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameReserveBag = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
		
		mReserveBagPieces = readPieces( systemNode ) ;
	}
	
	private int [] readPieces( Node bagNode ) {
		 Node piecesNode = findChildWithTagName( bagNode, TAG_PIECE_BAG_PIECES ) ;
		 // Load the int array
		 int [] pieceTypes = loadIntArrayFromNode( piecesNode ) ;
		 return pieceTypes ;
	}
	
	private void readFromDocumentAttackSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_ATTACK_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameAttackSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
		
		mAttackSystemBlocksValley = 0 ;
		mAttackSystemBlocksJunction = 0 ;
		mAttackSystemBlocksPeak = 0 ;
		mAttackSystemBlocksTroll = 0 ;
		
		ArrayList<Node> blocks = allDescendentsWithTagName( systemNode, TAG_ATTACK_SYSTEM_BLOCKS, null ) ;
		// TODO: support more than one.
		if ( blocks.size() > 0 ) {
			Node n ;
			NamedNodeMap attributes = blocks.get(0).getAttributes() ;
			n = attributes.getNamedItem(PARAM_ATTACK_SYSTEM_BLOCKS_VALLEY) ;
			mAttackSystemBlocksValley = n == null ? 0 : Integer.parseInt( n.getNodeValue() ) ;
			n = attributes.getNamedItem(PARAM_ATTACK_SYSTEM_BLOCKS_JUNCTION) ;
			mAttackSystemBlocksJunction = n == null ? 0 : Integer.parseInt( n.getNodeValue() ) ;
			n = attributes.getNamedItem(PARAM_ATTACK_SYSTEM_BLOCKS_PEAK) ;
			mAttackSystemBlocksPeak = n == null ? 0 : Integer.parseInt( n.getNodeValue() ) ;
			n = attributes.getNamedItem(PARAM_ATTACK_SYSTEM_BLOCKS_CORNER) ;
			mAttackSystemBlocksCorner = n == null ? 0 : Integer.parseInt( n.getNodeValue() ) ;
			n = attributes.getNamedItem(PARAM_ATTACK_SYSTEM_BLOCKS_TROLL) ;
			mAttackSystemBlocksTroll = n == null ? 0 : Integer.parseInt( n.getNodeValue() ) ;
		}
	}
	
	private void readFromDocumentClearSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_CLEAR_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameClearSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	private void readFromDocumentCollisionSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_COLLISION_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameCollisionSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	private void readFromDocumentDisplacementSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_DISPLACEMENT_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameDisplacementSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	
		if ( GameMode.TAG_VALUE_DISPLACEMENT_SYSTEM_REAL_TIME.equals(mNameDisplacementSystem) ) {
			mDisplacementSystemRowsPerSecondArray = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_DISPLACEMENT_SYSTEM_ROWS_PER_SECOND) ) ;
			mDisplacementSystemRowsPerSecondAccelArray = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_DISPLACEMENT_SYSTEM_ROWS_PER_SECOND_ACCEL) ) ;
			mDisplacementSystemRowsPerSecondDecelArray = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_DISPLACEMENT_SYSTEM_ROWS_PER_SECOND_DECEL) ) ;
		}
		
		if ( GameMode.TAG_VALUE_DISPLACEMENT_SYSTEM_ABSOLUTE_TIME.equals(mNameDisplacementSystem) ) {
			double accel = Double.parseDouble( systemAttributes.getNamedItem(PARAM_DISPLACEMENT_SYSTEM_ACCEL).getNodeValue() ) ;
			double decel = Double.parseDouble( systemAttributes.getNamedItem(PARAM_DISPLACEMENT_SYSTEM_DECEL).getNodeValue() ) ;
			
			ArrayList<Node> displacements = this.allDescendentsWithTagName(systemNode, TAG_DISPLACEMENT_SYSTEM_DISPLACEMENT, null) ;
			for ( int i = 0; i < displacements.size(); i++ ) {
				Node disp = displacements.get(i) ;
				QuadraticFunction qf = loadQuadraticFunction(disp) ;
				NamedNodeMap attributes = disp.getAttributes() ;
				Node nodeDifficulty = attributes.getNamedItem(PARAM_DISPLACEMENT_DIFFICULTY) ;
				int difficulty = GameInformation.DIFFICULTY_NORMAL ;
				if ( nodeDifficulty != null ) {
					String difStr = nodeDifficulty.getNodeValue() ;
					if ( "normal".equals(difStr) )
						difficulty = GameInformation.DIFFICULTY_NORMAL ;
					else if ( "hard".equals(difStr) )
						difficulty = GameInformation.DIFFICULTY_HARD ;
				}
				if ( difficulty == GameInformation.DIFFICULTY_NORMAL ) {
					if ( this.mDisplacementSystemRowsAtSecondFunctionNormal != null ) {
						throw new IllegalStateException("Displacement System: Normal Difficulty displacement specified at least twice.  Only do this once!") ;
					}
					mDisplacementSystemRowsAtSecondFunctionNormal = qf ;
				} else if ( difficulty == GameInformation.DIFFICULTY_HARD ) {
					if ( this.mDisplacementSystemRowsAtSecondFunctionHard != null ) {
						throw new IllegalStateException("Displacement System: Hard Difficulty displacement specified at least twice.  Only do this once!") ;
					}
					mDisplacementSystemRowsAtSecondFunctionHard = qf ;
				}
			}
			
			mDisplacementSystemRowsPerSecondAccelDouble = accel ;
			mDisplacementSystemRowsPerSecondDecelDouble = decel ;
			
		}
	}
	
	private void readFromDocumentKickSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_KICK_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameKickSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	private void readFromDocumentLevelSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_LEVEL_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameLevelSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
		
		mLevelSystemConditionTypes = new ArrayList<ArrayList<Integer>>() ;
		mLevelSystemConditionSubTypes = new ArrayList<ArrayList<Integer>>() ;
		mLevelSystemConditionLevelArray = new ArrayList<ArrayList<SimulatedArray>>() ;
		
		if ( TAG_VALUE_LEVEL_SYSTEM_CUSTOM.equals( mNameLevelSystem ) ) {
			ArrayList<Node> level_ups = allDescendentsWithTagName( systemNode, TAG_LEVEL_SYSTEM_LEVEL_UP, null ) ;
			
			for ( int tier = 0; tier < level_ups.size()-1; tier++ ) {
				ArrayList<Integer> types = new ArrayList<Integer>() ;
				ArrayList<Integer> subtypes = new ArrayList<Integer>() ;
				ArrayList<SimulatedArray> levelArray = new ArrayList<SimulatedArray>() ;
				
				Node level_up = level_ups.get( tier ) ;
				NodeList level_up_conditions = level_up.getChildNodes() ;
				for ( int condition = 0; condition < level_up_conditions.getLength(); condition++ ) {
					Node conditionNode = level_up_conditions.item(condition) ;
					loadCustomLevelSystemCondition( types, subtypes, levelArray, conditionNode ) ;
				}
				
				mLevelSystemConditionTypes.add(types) ;
				mLevelSystemConditionSubTypes.add(subtypes) ;
				mLevelSystemConditionLevelArray.add(levelArray) ;
			}
			
			// Last one... the "generic" one.
			ArrayList<Integer> types = new ArrayList<Integer>() ;
			ArrayList<Integer> subtypes = new ArrayList<Integer>() ;
			ArrayList<SimulatedArray> levelArray = new ArrayList<SimulatedArray>() ;
			
			Node level_up = level_ups.get( level_ups.size()-1 ) ;
			NodeList level_up_conditions = level_up.getChildNodes() ;
			for ( int condition = 0; condition < level_up_conditions.getLength(); condition++ ) {
				Node conditionNode = level_up_conditions.item(condition) ;
				loadCustomLevelSystemCondition( types, subtypes, levelArray, conditionNode ) ;
			}
			
			mLevelSystemConditionTypes.add(types) ;
			mLevelSystemConditionSubTypes.add(subtypes) ;
			mLevelSystemConditionLevelArray.add(levelArray) ;
		}
	}
	
	
	private void loadCustomLevelSystemCondition(
			ArrayList<Integer>types,
			ArrayList<Integer>subtypes,
			ArrayList<SimulatedArray>levelArray,
			Node conditionNode ) {
		
		int type = -1 ;
		if ( TAG_LEVEL_SYSTEM_CLEARS.equals( conditionNode.getNodeName() ) )
			type = LevelSystem.TYPE_CLEARS ;
		else if ( TAG_LEVEL_SYSTEM_CLEARS_SINCE_LAST.equals( conditionNode.getNodeName() ) )
			type = LevelSystem.TYPE_CLEARS_SINCE_LAST ;
		
		ArrayList<Node> subtypeNodes = allDescendentsWithTagName( conditionNode, TAG_LEVEL_SYSTEM_CLEAR, null ) ;
		
		for ( int i = 0; i < subtypeNodes.size(); i++ ) {
			int subtype = -1 ;
			Node subtypeNode = subtypeNodes.get(i) ;
			NamedNodeMap conditionAttributes = subtypeNode.getAttributes() ;
			String clearType = conditionAttributes.getNamedItem(PARAM_LEVEL_SYSTEM_CLEAR_TYPE).getNodeValue() ;
			if ( clearType.equals(PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_ANY) )
				subtype = LevelSystem.SUBTYPE_CLEARS_ANY ;
			else if ( clearType.equals(PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_MO) )
				subtype = LevelSystem.SUBTYPE_CLEARS_MO ;
			else if ( clearType.equals(PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_S0) )
				subtype = LevelSystem.SUBTYPE_CLEARS_S0 ;
			else if ( clearType.equals(PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_S1) )
				subtype = LevelSystem.SUBTYPE_CLEARS_S1 ;
			else if ( clearType.equals(PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_SS) )
				subtype = LevelSystem.SUBTYPE_CLEARS_SS ;
			else if ( clearType.equals(PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_SS_AND_MO) )
				subtype = LevelSystem.SUBTYPE_CLEARS_SS_AND_MO ;
			else if ( clearType.equals(PARAM_VALUE_LEVEL_SYSTEM_CLEAR_TYPE_TOTAL) )
				subtype = LevelSystem.SUBTYPE_CLEARS_TOTAL ;
			
			// That describes the condition.
			if ( type != -1 && subtype != -1 ) {
				SimulatedArray sa = loadLevelIntArrayFromNode( conditionNode ) ;
				types.add(type) ;
				subtypes.add(subtype) ;
				levelArray.add(sa) ;
			}
		}
	}
	
	private void readFromDocumentLockSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_LOCK_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameLockSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	private void readFromDocumentMetamorphosisSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_METAMORPHOSIS_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameMetamorphosisSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	private void readFromDocumentRotationSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_ROTATION_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameRotationSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	private void readFromDocumentScoreSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_SCORE_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameScoreSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	
		if ( TAG_VALUE_SCORE_SYSTEM_TRIGGERABLE_LEVEL.equals(mNameScoreSystem) ) {
			mScoreSystemDecrement = systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_DECREMENT) != null ? Double.parseDouble( systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_DECREMENT).getNodeValue() ) : 1 ;
			mScoreSystemRound = systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_ROUND) != null ? Double.parseDouble( systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_ROUND).getNodeValue() ) : 0.1 ;
			mScoreSystemMaxMultGrace = systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_MAXIMUM_GRACE_MULTIPLIER_PRODUCT) != null ? Double.parseDouble( systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_MAXIMUM_GRACE_MULTIPLIER_PRODUCT).getNodeValue() ) : 1 ;
			mScoreSystemCharitySlope = systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_CHARITY_SLOPE) != null ? Double.parseDouble( systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_CHARITY_SLOPE).getNodeValue() ) : 1 ;
			mScoreSystemCharityIntercept = systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_CHARITY_INTERCEPT) != null ? Double.parseDouble( systemAttributes.getNamedItem(PARAM_SCORE_SYSTEM_CHARITY_INTERCEPT).getNodeValue() ) : 1 ;
			
			mScoreSystemLevelIntArray = loadLevelIntArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_SCORE_SYSTEM_CLEAR_MULTIPLIER) ) ;
		} else {
			mScoreSystemDecrement = 1 ;
			mScoreSystemRound = 0.1 ;
			mScoreSystemMaxMultGrace = 1 ;
			mScoreSystemCharitySlope = 1 ;
			mScoreSystemCharityIntercept = 1 ;
			
			mScoreSystemLevelIntArray = null ;
		}
	}
	
	private void readFromDocumentSpecialSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_SPECIAL_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameSpecialSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
		
		// maximum blocks?
		if ( TAG_VALUE_SPECIAL_SYSTEM_ATTACK_3D.equals( mNameSpecialSystem)
				|| TAG_VALUE_SPECIAL_SYSTEM_RESERVE_PUSH_DOWN.equals( mNameSpecialSystem ) ) {
			String maxBlocksStr = systemAttributes.getNamedItem(PARAM_SPECIAL_SYSTEM_MAX_BLOCKS).getNodeValue() ;
			mSpecialSystemMaxBlocks = maxBlocksStr == null ? 0 : Integer.parseInt(maxBlocksStr) ;
		
			String blocksStr = systemAttributes.getNamedItem(PARAM_SPECIAL_SYSTEM_BLOCKS).getNodeValue() ;
			mSpecialSystemBlocks = blocksStr == null ? 0 : Integer.parseInt(blocksStr) ;
			
			
			// points per blocks
			String pointsPerBlockStr = systemAttributes.getNamedItem(PARAM_SPECIAL_SYSTEM_POINTS_PER_BLOCK).getNodeValue() ;
			mSpecialSystemPointsPerBlock = pointsPerBlockStr == null ? 0 : Integer.parseInt(pointsPerBlockStr) ;
			// points per clear
			mSpecialSystemPointsPerClearArray = loadClearIntArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_SPECIAL_SYSTEM_CLEARS_) ) ;
		}
	}
	
	private void readFromDocumentTimingSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_TIMING_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameTimingSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	
		if ( TAG_VALUE_TIMING_SYSTEM_CUSTOM.equals(mNameTimingSystem) ) {
			mTimingSystemEnterDelayArray = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_ENTER_DELAY) ) ;
			mTimingSystemFallDelayArray = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_FALL_DELAY) ) ;
			mTimingSystemLockDelayArray = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_LOCK_DELAY) ) ;
			mTimingSystemFastFallDelayArray = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_FAST_FALL_DELAY) ) ;
			
			mTimingSystemEnterDelayArrayHardMode = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_ENTER_DELAY_HARD_MODE) ) ;
			mTimingSystemFallDelayArrayHardMode = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_FALL_DELAY_HARD_MODE) ) ;
			mTimingSystemLockDelayArrayHardMode = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_LOCK_DELAY_HARD_MODE) ) ;
			mTimingSystemFastFallDelayArrayHardMode = loadLevelDoubleArrayFromNode( findChildWithTagName(systemNode, TAG_NAME_TIMING_SYSTEM_FAST_FALL_DELAY_HARD_MODE) ) ;
		}
	}
	
	
	private void readFromDocumentTriggerSystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_TRIGGER_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameTriggerSystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	
		mTriggerSystemIncreaseComplexClear = systemAttributes.getNamedItem(PARAM_TRIGGER_SYSTEM_SCORE_MULTIPLIER_INCREASE_COMPLEX_CLEAR) != null ? Double.parseDouble( systemAttributes.getNamedItem(PARAM_TRIGGER_SYSTEM_SCORE_MULTIPLIER_INCREASE_COMPLEX_CLEAR).getNodeValue() ) : 0 ;
		mTriggerSystemIncreaseSpecialPiece = systemAttributes.getNamedItem(PARAM_TRIGGER_SYSTEM_SCORE_MULTIPLIER_INCREASE_SPECIAL_PIECE) != null ? Double.parseDouble( systemAttributes.getNamedItem(PARAM_TRIGGER_SYSTEM_SCORE_MULTIPLIER_INCREASE_SPECIAL_PIECE).getNodeValue() ) : 0 ;
	}
	
	private void readFromDocumentValleySystem( Document doc ) {
		Node systemNode = doc.getElementsByTagName(TAG_NAME_VALLEY_SYSTEM).item(0) ;
		NamedNodeMap systemAttributes = systemNode.getAttributes() ;
		mNameValleySystem = systemAttributes.getNamedItem(ATTRIBUTE_NAME_NAME).getNodeValue() ;
	}
	
	
	//////////////////////////////////////////////////////////
	// Some query methods for game behavior.
	
	
	
	boolean isMultiplayer( ) {
		return this.playersMax > 1 ;
	}
	
	
	boolean hasRotation() {
		return mCustomGameModeSettings == null ? true : mCustomGameModeSettings.getHasRotation() ;
	}
	
	boolean hasReflection() {
		return mCustomGameModeSettings == null ? false : mCustomGameModeSettings.getHasReflection() ;
	}
	
	
	
	
	//////////////////////////////////////////////////////////
	// Some methods for making important things
	
	/**
	 * Constructs and returns the appropriate QInteractions class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation of QInteractions constructed and returned
	 * @return An instance of QInteractions (or null)
	 */
	QInteractions newQInteractions() {
		if ( TAG_VALUE_Q_INTERACTIONS_BASIC.equals( mNameQInteractions ) )
			return new QInteractionsBasic() ;
		throw new RuntimeException("QInteractions name, " + mNameQInteractions + ", not recognized.") ;
	}
	
	
	PieceBag newPieceBag( GameInformation ginfo ) {
		PieceBag pb = null ;
		
		if ( TAG_VALUE_PIECE_BAG_RETRO.equals(mNamePieceBag) )
			pb = new RetroPieceBag(
					mCustomGameModeSettings != null && mCustomGameModeSettings.getHasTrominoes(),
					mCustomGameModeSettings == null || mCustomGameModeSettings.getHasTetrominoes(),		// has tetrominoes by default
					mCustomGameModeSettings != null && mCustomGameModeSettings.getHasPentominoes(),
					hasRotation(), hasReflection() ) ;
		else if ( TAG_VALUE_PIECE_BAG_BINOMIAL_TETRACUBE.equals(mNamePieceBag) ) {
			pb = new BinomialTetracubePieceBag(
					mParamPieceBagC, mParamPieceBagN, mParamPieceBagP,
					mCustomGameModeSettings != null && mCustomGameModeSettings.getHasTrominoes(),
					mCustomGameModeSettings == null || mCustomGameModeSettings.getHasTetrominoes(),		// has tetrominoes by default
					mCustomGameModeSettings != null && mCustomGameModeSettings.getHasPentominoes(),
					hasRotation(), hasReflection() ) ;
		}
		else
			throw new RuntimeException("PieceBag name, " + mNamePieceBag + ", not recognized.") ;
		
		// Load in pieces, if necessary.
		Piece p = new Piece() ;
		for ( int i = mPieceBagPieces.length-1; i >= 0; i++ ) {
			p.type = mPieceBagPieces[i] ;
			p.rotation = p.defaultRotation = 0 ;
			pb.push(p) ;
		}
		
		pb.finalizeConfiguration() ;
		return pb ;
	}
	
	
	PieceBag newReserveBag( GameInformation ginfo ) {
		PieceBag pb = null ;
		
		if ( TAG_VALUE_PIECE_BAG_UNIFORM_SPECIAL.equals( mNameReserveBag ) )
			pb = new UniformSpecialPieceBag(
					mCustomGameModeSettings != null && mCustomGameModeSettings.getHasTrominoes(),
					mCustomGameModeSettings == null || mCustomGameModeSettings.getHasTetrominoes(),		// has tetrominoes by default
					mCustomGameModeSettings != null && mCustomGameModeSettings.getHasPentominoes(),
					hasRotation(), hasReflection()) ;
		else
			throw new RuntimeException("ReserveBag name, " + mNameReserveBag + ", not recognized.") ;
		
		// Load in pieces, if necessary.
		Piece p = new Piece() ;
		for ( int i = mReserveBagPieces.length-1; i >= 0; i++ ) {
			p.type = mReserveBagPieces[i] ;
			p.rotation = p.defaultRotation = 0 ;
			pb.push(p) ;
		}
		
		pb.finalizeConfiguration() ;
		return pb ;
	}
	
	
	
	
	PieceHistory newPieceHistory( GameInformation ginfo, PieceBag pieceBag, PieceBag reserveBag, PieceBag ... otherBags ) {
		PieceHistory ph = new PieceHistory(ginfo) ;
		
		// Consolidate based on #qpanes
		ph.setConsolidateClears( qpanes == 1 ) ;
		ph.addBagContents( pieceBag.contents() ) ;
		ph.addBagContents( reserveBag.contents() ) ;
		for ( int i = 0; i < otherBags.length; i++ )
			ph.addBagContents( otherBags[i].contents() ) ;
		
		ph.finalizeConfiguration() ;
		return ph ;
	}
	
	//////////////////////////////////////////////////////////
	// Some methods for making systems.
	
	
	AttackSystem newAttackSystem( GameInformation ginfo, QInteractions qi ) {
		AttackSystem as = null ;
		
		if ( TAG_VALUE_ATTACK_SYSTEM_RETRO_VERSUS.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newRetroVSAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_RETRO_VERSUS_GRAVITY.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newRetroGravityAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_RETRO_VERSUS_FLOOD.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newRetroFloodVSAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_RETRO_FREE_4_ALL.equals(mNameAttackSystem) ) 
			as = AttackSystemFactory.newRetroFree4AllAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_RETRO_FREE_4_ALL_FLOOD.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newRetroFloodFree4AllAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_QUANTRO_VERSUS.equals(mNameAttackSystem) ) 
			as = AttackSystemFactory.newQuantroVSAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_QUANTRO_VERSUS_NO_GARBAGE.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newQuantroVSAttackSystemNoGarbage(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_QUANTRO_VERSUS_FLOOD.equals(mNameAttackSystem) ) 
			as = AttackSystemFactory.newQuantroFloodVSAttackSystemNoGarbage(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_QUANTRO_FREE_4_ALL.equals(mNameAttackSystem) ) 
			as = AttackSystemFactory.newQuantroFree4AllAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_QUANTRO_FREE_4_ALL_FLOOD.equals(mNameAttackSystem) ) 
			as = AttackSystemFactory.newQuantroFloodFree4AllAttackSystemNoGarbage(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_NO.equals(mNameAttackSystem) ) 
			as = AttackSystemFactory.newNoAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_INITIAL_GARBAGE_RETRO.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newInitialGarbageRetroAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_INITIAL_GARBAGE_QUANTRO.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newInitialGarbageQuantroAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_LEVEL_GARBAGE_RETRO.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newLevelGarbageRetroAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_LEVEL_GARBAGE_QUANTRO.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newLevelGarbageQuantroAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_MASOCHIST_RETRO.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newRetroMasochistAttackSystem(ginfo, qi) ;
		else if ( TAG_VALUE_ATTACK_SYSTEM_MASOCHIST_QUANTRO.equals(mNameAttackSystem) )
			as = AttackSystemFactory.newQuantroMasochistAttackSystem(ginfo, qi) ;
		
		
		else {
			throw new RuntimeException("AttackSystem name, " + mNameAttackSystem + ", not recognized.") ;
		}
		
		// set default garbage
		as.setDefault_levelUp_garbageRowNumber(defaultGarbage) ;
		
		// set new game blocks
		as.setNewGame_dropBlocks(
				mAttackSystemBlocksValley,
				mAttackSystemBlocksJunction,
				mAttackSystemBlocksPeak,
				mAttackSystemBlocksCorner,
				mAttackSystemBlocksTroll) ;
		
		as.finalizeConfiguration() ;
		as.newGame();		// this is fine to do; will overwrite whatever is here if we are loading a game in progress
		return as ;
	}
	
	
	/**
	 * Constructs and returns the appropriate ClearSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of ClearSystem (or null)
	 */
	ClearSystem newClearSystem( GameInformation ginfo, QInteractions qi ) {
		ClearSystem cs = null ;
		
		if ( TAG_VALUE_CLEAR_SYSTEM_QUANTRO.equals(mNameClearSystem) )
			cs = new QuantroClearSystem( ginfo, qi ) ;
		else
			throw new RuntimeException("ClearSystem name, " + mNameClearSystem + ", not recognized.") ;

		cs.finalizeConfiguration() ;
		return cs ;
	}
	
	
	/**
	 * Constructs and returns the appropriate CollisionSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of CollisionSystem (or null)
	 */
	CollisionSystem newCollisionSystem( GameInformation ginfo, QInteractions qi ) {
		CollisionSystem cs ;
		
		if ( TAG_VALUE_COLLISION_SYSTEM_EARLY.equals(mNameCollisionSystem) )
			cs = new EarlyCollisionSystem( ginfo, qi ) ;
		else
			throw new RuntimeException("CollisionSystem name, " + mNameCollisionSystem + ", not recognized.") ;
		
		cs.finalizeConfiguration() ;
		return cs ;
	}
	
	
	DisplacementSystem newDisplacementSystem( GameInformation ginfo, QInteractions qi ) {
		DisplacementSystem ds ;
		
		if ( TAG_VALUE_DISPLACEMENT_SYSTEM_DEAD.equals(mNameDisplacementSystem) ) {
			ds = new DeadDisplacementSystem( ginfo, qi ) ;
		} else if ( TAG_VALUE_DISPLACEMENT_SYSTEM_REAL_TIME.equals(mNameDisplacementSystem) ) {
			ds = new RealTimeDisplacementSystem( ginfo, qi ) ;
			((RealTimeDisplacementSystem)ds).setRowsPerSecond(mDisplacementSystemRowsPerSecondArray, RealTimeDisplacementSystem.STATE_DEFAULT) ;
			((RealTimeDisplacementSystem)ds).setRowsPerSecond(mDisplacementSystemRowsPerSecondAccelArray, RealTimeDisplacementSystem.STATE_ACCELERATING) ;
			((RealTimeDisplacementSystem)ds).setRowsPerSecond(mDisplacementSystemRowsPerSecondDecelArray, RealTimeDisplacementSystem.STATE_DECELERATING) ;
		} else if ( TAG_VALUE_DISPLACEMENT_SYSTEM_ABSOLUTE_TIME.equals(mNameDisplacementSystem) ) {
			ds = new AbsoluteTimeDisplacementSystem( ginfo, qi ) ;
			((AbsoluteTimeDisplacementSystem)ds).setDisplacementAtSeconds(
					GameInformation.DIFFICULTY_NORMAL,
					mDisplacementSystemRowsAtSecondFunctionNormal) ;
			((AbsoluteTimeDisplacementSystem)ds).setDisplacementAtSeconds(
					GameInformation.DIFFICULTY_HARD,
					mDisplacementSystemRowsAtSecondFunctionHard) ;
			((AbsoluteTimeDisplacementSystem)ds).setDisplacementAccelRowsPerSecond(mDisplacementSystemRowsPerSecondAccelDouble) ;
			((AbsoluteTimeDisplacementSystem)ds).setDisplacementDecelRowsPerSecond(mDisplacementSystemRowsPerSecondDecelDouble) ;
		} else {
			throw new RuntimeException("DisplacementSystem name, " + mNameDisplacementSystem + ", not recognized.") ;
		}
		
		ds.finalizeConfiguration() ;
		return ds ;
	}
	
	
	/**
	 * Constructs and returns the appropriate KickSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of KickSystem (or null)
	 */
	KickSystem newKickSystem( GameInformation ginfo, QInteractions qi ) {
		KickSystem ks = null ;
		
		if ( TAG_VALUE_KICK_SYSTEM_EARLY.equals(mNameKickSystem) )
			ks = new EarlyKickSystem( ginfo ) ;
		else if ( TAG_VALUE_KICK_SYSTEM_LEANING.equals(mNameKickSystem) )
			ks = new LeaningKickSystem( ginfo ) ;
		else 
			throw new RuntimeException("KickSystem name, " + name + ", not recognized.") ;
		
		ks.finalizeConfiguration() ;
		return ks ;
	}
	
	
	LevelSystem newLevelSystem( GameInformation ginfo, QInteractions qi ) {
		LevelSystem ls = null ;
		
		if ( TAG_VALUE_LEVEL_SYSTEM_CUSTOM.equals(mNameLevelSystem) ) {
			// If the GameInformation does not have levelUp criteria set, we load the values
			// stored here.  Otherwise, we use only and exactly those levelUp criteria set
			// by the GameInformation object.
			if ( !ginfo.clearsSetForLevelUp() || setClears == SET_CLEAR_NO ) {
				// We need the number of tiers and maxLevels to construct a CustomLevelSystem.
				ls = new CustomLevelSystem(
						ginfo, qi, maxLevel, mLevelSystemConditionTypes.size()-1 ) ;
				
				// Add the conditions.
				for ( int tier = 0; tier < mLevelSystemConditionTypes.size()-1; tier++ ) {
					ArrayList<Integer> types = mLevelSystemConditionTypes.get(tier) ;
					ArrayList<Integer> subtypes = mLevelSystemConditionSubTypes.get(tier) ;
					ArrayList<SimulatedArray> levelArray = mLevelSystemConditionLevelArray.get(tier) ;
					
					for ( int condition = 0; condition < types.size(); condition++ ) 
						((CustomLevelSystem)ls).setInitialClearCountArray(
								tier,
								types.get(condition),
								subtypes.get(condition),
								levelArray.get(condition)) ;
				}
				// Last one... the "generic" one.
				int last = mLevelSystemConditionTypes.size()-1;
				ArrayList<Integer> types = mLevelSystemConditionTypes.get(last) ;
				ArrayList<Integer> subtypes = mLevelSystemConditionSubTypes.get(last) ;
				ArrayList<SimulatedArray> levelArray = mLevelSystemConditionLevelArray.get(last) ;
				
				for ( int condition = 0; condition < types.size(); condition++ ) 
					((CustomLevelSystem)ls).setClearCountArray(
							types.get(condition),
							subtypes.get(condition),
							levelArray.get(condition)) ;
			}
			else {
				// Otherwise, set our level system based on ginfo.
				ls = new CustomLevelSystem( ginfo, qi, maxLevel, 0 ) ;
				if ( ginfo.s0clearsSinceLevelForLevelUp > 0 )
					((CustomLevelSystem)ls).setClearCountArray(
							LevelSystem.TYPE_CLEARS_SINCE_LAST,
							LevelSystem.SUBTYPE_CLEARS_S0,
							SimulatedArrayFactory.newConstant(maxLevel, ginfo.s0clearsSinceLevelForLevelUp)) ;
				
				if ( ginfo.s1clearsSinceLevelForLevelUp > 0 )
					((CustomLevelSystem)ls).setClearCountArray(
							LevelSystem.TYPE_CLEARS_SINCE_LAST,
							LevelSystem.SUBTYPE_CLEARS_S1,
							SimulatedArrayFactory.newConstant(maxLevel, ginfo.s1clearsSinceLevelForLevelUp)) ;
				
				if ( ginfo.sLclearsSinceLevelForLevelUp > 0 )
					((CustomLevelSystem)ls).setClearCountArray(
							LevelSystem.TYPE_CLEARS_SINCE_LAST,
							LevelSystem.SUBTYPE_CLEARS_SS,
							SimulatedArrayFactory.newConstant(maxLevel, ginfo.sLclearsSinceLevelForLevelUp)) ;
				
				if ( ginfo.moclearsSinceLevelForLevelUp > 0 )
					((CustomLevelSystem)ls).setClearCountArray(
							LevelSystem.TYPE_CLEARS_SINCE_LAST,
							LevelSystem.SUBTYPE_CLEARS_MO,
							SimulatedArrayFactory.newConstant(maxLevel, ginfo.moclearsSinceLevelForLevelUp)) ;
				
				if ( ginfo.nyclearsSinceLevelForLevelUp > 0 )
					((CustomLevelSystem)ls).setClearCountArray(
							LevelSystem.TYPE_CLEARS_SINCE_LAST,
							LevelSystem.SUBTYPE_CLEARS_ANY,
							SimulatedArrayFactory.newConstant(maxLevel, ginfo.nyclearsSinceLevelForLevelUp)) ;
				
				if ( ginfo.LmclearsSinceLevelForLevelUp > 0 )
					((CustomLevelSystem)ls).setClearCountArray(
							LevelSystem.TYPE_CLEARS_SINCE_LAST,
							LevelSystem.SUBTYPE_CLEARS_SS_AND_MO,
							SimulatedArrayFactory.newConstant(maxLevel, ginfo.LmclearsSinceLevelForLevelUp)) ;
				
				if ( ginfo.tlclearsSinceLevelForLevelUp > 0 )
					((CustomLevelSystem)ls).setClearCountArray(
							LevelSystem.TYPE_CLEARS_SINCE_LAST,
							LevelSystem.SUBTYPE_CLEARS_TOTAL,
							SimulatedArrayFactory.newConstant(maxLevel, ginfo.tlclearsSinceLevelForLevelUp)) ;
			}
		}
		else {
			throw new RuntimeException("LevelSystem name, " + name + ", not recognized.") ;
		}
		
		ls.finalizeConfiguration() ;
		return ls ;
	}
	
	
	
	/**
	 * Constructs and returns the appropriate LockSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of LockSystem (or null)
	 */
	LockSystem newLockSystem( GameInformation ginfo, QInteractions qi ) {
		LockSystem ls = null ;
		
		if ( TAG_VALUE_LOCK_SYSTEM_EARLY.equals(mNameLockSystem) )
			ls = new EarlyLockSystem( ginfo, qi ) ;
		else
			throw new RuntimeException("LockSystem name, " + mNameLockSystem + ", not recognized.") ;
		
		ls.finalizeConfiguration() ;
		return ls ;
	}
	
	
	/**
	 * Constructs and returns the appropriate MetamorphosisSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of MetamorphosisSystem (or null)
	 */
	MetamorphosisSystem newMetamorphosisSystem( GameInformation ginfo, QInteractions qi ) {
		MetamorphosisSystem ms = null ;
		
		if ( TAG_VALUE_METAMORPHOSIS_SYSTEM_QUANTRO.equals(mNameMetamorphosisSystem) )
			ms = new QuantroMetamorphosisSystem( ginfo, qi ) ;
		else if ( TAG_VALUE_METAMORPHOSIS_SYSTEM_DEAD.equals(mNameMetamorphosisSystem) )
			ms = new DeadMetamorphosisSystem( ginfo, qi ) ;
		else
			throw new RuntimeException("MetamorphosisSystem name, " + mNameMetamorphosisSystem + ", not recognized.") ;
		
		ms.finalizeConfiguration() ;
		return ms ;
	}
	
	
	/**
	 * Constructs and returns the appropriate RotationSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of KickSystem (or null)
	 */
	RotationSystem newRotationSystem( GameInformation ginfo, QInteractions qi, PieceBag...bags ) {
		RotationSystem rs = null ;
		
		// We pre-allocate blocks for the piece types which this piece bag can
		// generate.  This gives us a slightly redundant list, but that isn't too
		// important.
		int [] types = new int[0] ;
		for ( int i = 0; i < bags.length; i++ ) {
			PieceBag bp = bags[i] ;
			int [] bagTypes = bp.contents() ;
			
			int [] allTypes = new int[types.length + bagTypes.length] ;
			for ( int j = 0; j < types.length; j++ )
				allTypes[j] = types[j] ;
			for ( int j = 0; j < bagTypes.length; j++ )
				allTypes[j + types.length] = bagTypes[j] ;
			types = allTypes ;
		}

		if ( TAG_VALUE_ROTATION_SYSTEM_TETROMINO.equals(mNameRotationSystem) )
			rs = new TetrominoRotationSystem( ginfo, qi, types ) ;
		else if ( TAG_VALUE_ROTATION_SYSTEM_TETRACUBE.equals(mNameRotationSystem) )
			rs = new TetracubeRotationSystem( ginfo, qi, types ) ;
		else if ( TAG_VALUE_ROTATION_SYSTEM_UNIVERSAL.equals(mNameRotationSystem) )
			rs = new UniversalRotationSystem( ginfo, qi, types ) ;
		else {
			throw new RuntimeException("RotationSystem name, " + mNameRotationSystem + ", not recognized.") ;
		}
		
		rs.finalizeConfiguration() ;
		return rs ;
	}
	
	/**
	 * Constructs and returns the appropriate ScoreSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of ScoreSystem (or null)
	 */
	ScoreSystem newScoreSystem( GameInformation ginfo, QInteractions qi ) {
		ScoreSystem ss = null ;
		
		if ( TAG_VALUE_SCORE_SYSTEM_TRIGGERABLE_LEVEL.equals(mNameScoreSystem) ) {
			ss = new TriggerableLevelScoreSystem(
					ginfo, qi,
					mScoreSystemDecrement,
					mScoreSystemRound,
					mScoreSystemMaxMultGrace,
					mScoreSystemCharitySlope,
					mScoreSystemCharityIntercept )  ;
			
			if ( mScoreSystemLevelIntArray != null )
				((TriggerableLevelScoreSystem)ss).setLevelClearMultiplierArray(mScoreSystemLevelIntArray) ;
		}
		else
			throw new RuntimeException("ScoreSystem name, " + mNameScoreSystem + ", not recognized.") ;
		
		ss.finalizeConfiguration() ;
		return ss ;
	}
	
	
	
	/**
	 * Constructs and returns the appropriate SpecialSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of SpecialSystem (or null)
	 */
	SpecialSystem newSpecialSystem( GameInformation ginfo, QInteractions qi ) {
		SpecialSystem ss = null ;
		
		if ( TAG_VALUE_SPECIAL_SYSTEM_DEAD.equals(mNameSpecialSystem) )
			ss = new DeadSpecialSystem( ginfo, qi ) ;
		else if ( TAG_VALUE_SPECIAL_SYSTEM_ATTACK_3D.equals(mNameSpecialSystem) ) {
			ss = new BlockDropperSpecialSystem( ginfo, qi ) ;
			((BlockDropperSpecialSystem)ss).configurePreviewQCombination(QCombinations.ST_INACTIVE) ;
			((BlockDropperSpecialSystem)ss).configureSpecialType(BlockDropperSpecialSystem.SpecialType.ATTACK) ;
			((BlockDropperSpecialSystem)ss).configureMaxBlocks(mSpecialSystemMaxBlocks) ;
			((BlockDropperSpecialSystem)ss).configureStartBlocks(mSpecialSystemBlocks) ;
			
			
			((BlockDropperSpecialSystem)ss).configureEarnBlocksFromClears(mSpecialSystemPointsPerBlock, mSpecialSystemPointsPerClearArray) ;
			((BlockDropperSpecialSystem)ss).configureReclaimBlocks( new int[] { QCombinations.ST_INACTIVE } ) ;
		} else if ( TAG_VALUE_SPECIAL_SYSTEM_RESERVE_PUSH_DOWN.equals(mNameSpecialSystem) ) {
			ss = new BlockDropperSpecialSystem( ginfo, qi ) ;
			((BlockDropperSpecialSystem)ss).configurePreviewQCombination(QCombinations.PUSH_DOWN) ;
			((BlockDropperSpecialSystem)ss).configureReserveQCombination(QCombinations.PUSH_DOWN) ;
			((BlockDropperSpecialSystem)ss).configureSpecialType(BlockDropperSpecialSystem.SpecialType.RESERVE) ;
			((BlockDropperSpecialSystem)ss).configureMaxBlocks(mSpecialSystemMaxBlocks) ;
			((BlockDropperSpecialSystem)ss).configureStartBlocks(mSpecialSystemBlocks) ;
			
			((BlockDropperSpecialSystem)ss).configureEarnBlocksFromClears(mSpecialSystemPointsPerBlock, mSpecialSystemPointsPerClearArray) ;
			// no reclamation; only earned through clears.
		} else
			throw new RuntimeException("SpecialSystem name, " + mNameSpecialSystem + ", not recognized.") ;
		
		ss.finalizeConfiguration() ;
		return ss ;
	}
	
	
	
	/**
	 * Constructs and returns the appropriate TimingSystem class
	 * according to ginfo.getMode() and the entries of game_mode_*.xml
	 * @param ginfo The GameInformation object whose mode will determine
	 * the implementation constructed and returned
	 * @param qi The QInteractions used in construction
	 * @return A new instance of TimingSystem (or null)
	 */
	TimingSystem newTimingSystem( GameInformation ginfo, QInteractions qi ) {
		TimingSystem ts = null ;
		
		if ( TAG_VALUE_TIMING_SYSTEM_CUSTOM.equals(mNameTimingSystem)) {
			ts = new CustomTimingSystem(ginfo, qi) ;
			
			// Set some arrays.
			if ( ginfo.difficulty != GameInformation.DIFFICULTY_HARD ) {
				if ( mTimingSystemEnterDelayArray != null )
					((CustomTimingSystem)ts).setEnterDelayArray(mTimingSystemEnterDelayArray) ;
				
				if ( mTimingSystemFallDelayArray != null )
					((CustomTimingSystem)ts).setFallDelayArray(mTimingSystemFallDelayArray) ;
				
				if ( mTimingSystemLockDelayArray != null )
					((CustomTimingSystem)ts).setLockDelayArray(mTimingSystemLockDelayArray) ;
				
				if ( mTimingSystemFastFallDelayArray != null )
					((CustomTimingSystem)ts).setFastFallDelayArray(mTimingSystemFastFallDelayArray) ;
			} else {
				if ( mTimingSystemEnterDelayArrayHardMode != null )
					((CustomTimingSystem)ts).setEnterDelayArray(mTimingSystemEnterDelayArrayHardMode) ;
				else if ( mTimingSystemEnterDelayArray != null )
					((CustomTimingSystem)ts).setEnterDelayArray(mTimingSystemEnterDelayArray) ;
				
				if ( mTimingSystemFallDelayArrayHardMode != null )
					((CustomTimingSystem)ts).setFallDelayArray(mTimingSystemFallDelayArrayHardMode) ;
				else if ( mTimingSystemFallDelayArray != null )
					((CustomTimingSystem)ts).setFallDelayArray(mTimingSystemFallDelayArray) ;
				
				if ( mTimingSystemLockDelayArrayHardMode != null )
					((CustomTimingSystem)ts).setLockDelayArray(mTimingSystemLockDelayArrayHardMode) ;
				else if ( mTimingSystemLockDelayArray != null )
					((CustomTimingSystem)ts).setLockDelayArray(mTimingSystemLockDelayArray) ;
				
				if ( mTimingSystemFastFallDelayArrayHardMode != null )
					((CustomTimingSystem)ts).setFastFallDelayArray(mTimingSystemFastFallDelayArrayHardMode) ;
				else if ( mTimingSystemFastFallDelayArray != null )
					((CustomTimingSystem)ts).setFastFallDelayArray(mTimingSystemFastFallDelayArray) ;
			}
		}
		else {
			throw new RuntimeException("TimingSystem name, " + mNameTimingSystem + ", not recognized.") ;
		}
		
		ts.finalizeConfiguration() ;
		return ts ;
	}
	
	
	TriggerSystem newTriggerSystem(
			Game game, GameInformation ginfo, QInteractions qi,
			ClearSystem cls, CollisionSystem cs, LockSystem ls, ScoreSystem ss ) {
		
		TriggerSystem trs = null ;
		
		if ( TAG_VALUE_TRIGGER_SYSTEM_PROTOTYPING.equals(mNameTriggerSystem) )
			trs = TriggerSystemFactory.newPrototypeTriggerSystem(game, ginfo, qi, cls, cs, ls) ;
		else if ( TAG_VALUE_TRIGGER_SYSTEM_QUANTRO_SINGLE_PLAYER.equals(mNameTriggerSystem) ) 
			trs = TriggerSystemFactory.newQuantroSinglePlayerTriggerSystem(game, ginfo, qi, cls, cs, ls, ss,
					mTriggerSystemIncreaseComplexClear,
					mTriggerSystemIncreaseSpecialPiece) ;
		else if ( TAG_VALUE_TRIGGER_SYSTEM_RETRO_SINGLE_PLAYER.equals(mNameTriggerSystem) ) 
			trs = TriggerSystemFactory.newRetroSinglePlayerTriggerSystem(game, ginfo, qi, cls, cs, ls, ss,
					mTriggerSystemIncreaseComplexClear) ;
		else
			throw new RuntimeException("TriggerSystem name, " + mNameTriggerSystem + ", not recognized.") ;
		
		trs.finalizeConfiguration() ;
		return trs ;
	}
	
	
	ValleySystem newValleySystem(
			GameInformation ginfo, QInteractions qi ) {
		
		SimpleValleySystem vs = new SimpleValleySystem( ginfo, qi ) ;
		
		if ( TAG_VALUE_VALLEY_SYSTEM_NO.equals(mNameValleySystem) )
			vs.setBlockQCombinations(SimpleValleySystem.Q_COMBINATIONS_NONE) ;
		else if ( TAG_VALUE_VALLEY_SYSTEM_QUANTRO.equals(mNameValleySystem) )
			vs.setBlockQCombinations(SimpleValleySystem.Q_COMBINATIONS_QPANE) ;
		else if ( TAG_VALUE_VALLEY_SYSTEM_QUANTRO_UNSTABLE.equals(mNameValleySystem) )
			vs.setBlockQCombinations(SimpleValleySystem.Q_COMBINATIONS_QPANE_UNSTABLE) ;
		else if ( TAG_VALUE_VALLEY_SYSTEM_RETRO.equals(mNameValleySystem) )
			vs.setBlockQCombinations(SimpleValleySystem.Q_COMBINATIONS_RAINBOW) ;
		else if ( TAG_VALUE_VALLEY_SYSTEM_QUANTRO_3D.equals(mNameValleySystem) )
			vs.setBlockQCombinations(SimpleValleySystem.Q_COMBINATIONS_3D) ;
		else
			throw new RuntimeException("ValleySystem name, " + mNameValleySystem + ", not recognized.") ;
		
		vs.finalizeConfiguration() ;
		return vs ;
	}
	
	
	
	/////////////////////////////////////////////////////////////////////
	// Document-traversal helper methods
	/**
	 * Locates and returns the first preorder-traversal'ed child
	 * with the specified tag name; null if not found.
	 */
	private Node findChildWithTagName( Node node, String name ) {
		if ( node == null )
			return null ;
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			
			if ( child.getNodeName().equals(name) ) {
				return child ;
			}
		}
		
		// Not found here; recursively try children.
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			
			Node descendent = findChildWithTagName( child, name ) ;
			if ( descendent != null )
				return descendent ;
		}
		
		// nope, not found
		return null ;
	}
	
	/**
	 * Uses a preorder-traversal of the provided subtree, looking for nodes with
	 * the provided name.  All nodes are placed in an ArrayList, which is returned.
	 * Provide 'null' for the partialList and one will be allocated; otherwise, the
	 * results are appended to the list provided (and a reference to it returned).
	 * 
	 * @param node Root of the subtree to traverse
	 * @param name The nag name to look for
	 * @param partialList Either 'null', in which case a new list will be allocated, or
	 * 			a reference to a list to which results will be appended.
	 * @return partialList, if non-null, to which descendents have been appended.  Otherwise,
	 * 			a new NodeList containing all descendents found with the given tag name.
	 */
	private ArrayList<Node> allDescendentsWithTagName( Node node, String name, ArrayList<Node> partialList) {
		if ( partialList == null )
			partialList = new ArrayList<Node>() ;
		
		if ( node == null )
			return partialList ;
		
		// Check this one.
		if ( node.getNodeName().equals(name) ) {
			partialList.add(node) ;
		}
		
		// Check children
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			allDescendentsWithTagName( child, name, partialList ) ;
		}
		
		return partialList ;
	}
	
	
	/**
	 * Allocates and returns an integer array for the values in "node",
	 * which should contain at most 1 child in an array-specifying format.
	 * 
	 * Current supported tag name is "array".
	 * 
	 * This method allocates the array, potentially unnecessarily, because
	 * it makes no assumptions about the number of elements.  If you know
	 * the array is indexed by level, use loadLevelIntArrayFromNode instead.
	 * 
	 * @param node A node with exactly 1 descendent with a supported tag name.
	 * @return A newly allocated integer array (or "null" if no supported child was found).
	 */
	private int [] loadIntArrayFromNode( Node node ) {
		Node arNode = findChildWithTagName( node, ARRAY_ARRAY ) ;
		ArrayList<Node> elements = allDescendentsWithTagName( arNode, ARRAY_ELEMENT, null ) ;
		int [] ar = new int[elements.size()] ;
		for ( int i = 0; i < elements.size(); i++ ) {
			Node element = elements.get(i) ;
			NamedNodeMap attrs = element.getAttributes() ;
			int index = Integer.parseInt(attrs.getNamedItem(ARRAY_INDEX).getNodeValue()) ;
			int value = Integer.parseInt(attrs.getNamedItem(ARRAY_VALUE).getNodeValue()) ;
			ar[index-1] = value ;
		}
		
		return ar ;
	}
	
	
	
	/**
	 * Looks inside the node for a child with a tag name implying 
	 * it contains level-scaling floats; supported tags are
	 * 
	 * TAG_NAME_ARRAY_BY_LEVEL = "array_by_level" ;
	 * TAG_NAME_CONSTANT = "constant" ;
	 * TAG_NAME_LINEAR_BY_LEVEL = "linear_by_level" ;
	 * TAG_NAME_LINEAR_BY_LEVEL_PINNING = "linear_by_level_pinning" ;
	 * 
	 *@param Node - the node which we will traverse.
	 *@return Either a pointer to arLevelInt, which now contains floating
	 *		point levels indexed by level-1, or null, in the case that no
	 *		supported tag was found.
	 */
	private SimulatedArray loadLevelIntArrayFromNode( Node node ) {
		if ( node == null )
			return null ;
		// We traverse the node recursively until we find one.
		if ( node.getNodeName().equals( TAG_NAME_ARRAY_BY_LEVEL ) ) {
			// Load the array.
			NamedNodeMap attributes = node.getAttributes() ;
			ArrayList<Node> elements = allDescendentsWithTagName( node, ARRAY_ELEMENT, null ) ;
			int [] arLevelInt = new int[elements.size()] ;
			for ( int i = 0; i < elements.size(); i++ ) {
				Node element = elements.get(i) ;
				attributes = element.getAttributes() ;
				int index = Integer.parseInt( attributes.getNamedItem(ARRAY_INDEX).getNodeValue() ) ;
				int value = Integer.parseInt( attributes.getNamedItem(ARRAY_VALUE).getNodeValue() ) ;
				arLevelInt[index-1] = value ;
			}
			// "indices" are by level, so they start from 1.
			return SimulatedArrayFactory.newExplicitPinning(maxLevel, arLevelInt, elements.size()) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_LINEAR_BY_LEVEL ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			int slope = Integer.parseInt( attributes.getNamedItem(ARRAY_SLOPE).getNodeValue() ) ;
			int intercept = Integer.parseInt( attributes.getNamedItem(ARRAY_INTERCEPT).getNodeValue() ) ;
			// "slope/intercept" is XML-defined according to level number, i.e., slope + intercept
			// is the number at level 1.  However, we always index using level-1, so include
			// slope*1 in the intercept to compensate (index level x-1 corresponds to level x)
			return SimulatedArrayFactory.newSlopeIntercept(maxLevel, slope, intercept+slope) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_LINEAR_BY_LEVEL_PINNING ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			int slope = Integer.parseInt( attributes.getNamedItem(ARRAY_SLOPE).getNodeValue() ) ;
			int intercept = Integer.parseInt( attributes.getNamedItem(ARRAY_INTERCEPT).getNodeValue() ) ;
			int pin = Integer.parseInt( attributes.getNamedItem(ARRAY_PIN).getNodeValue() ) ;
			// "slope/intercept" is XML-defined according to level number, i.e., slope + intercept
			// is the number at level 1.  However, we always index using level-1, so include
			// slope*1 in the intercept to compensate (index level x-1 corresponds to level x).
			// We pin to the provided level.
			return SimulatedArrayFactory.newSlopeInterceptPinning(maxLevel, slope, intercept+slope, pin) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_CONSTANT ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			int val = Integer.parseInt( attributes.getNamedItem(ARRAY_VAL).getNodeValue() ) ;
			return SimulatedArrayFactory.newConstant(maxLevel, val) ;
		}
		
		// If we get here, the current node is not a supported one.  Try it's kids.
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			SimulatedArray sa = loadLevelIntArrayFromNode( child ) ;
			if ( sa != null )
				return sa ;
		}
		
		// By reaching this point, we have fully traverse the subtree and not found
		// any supported nodes.
		return null ;
	}
	
	
	/**
	 * Looks inside the node for a child with a tag name implying 
	 * it contains level-scaling floats; supported tags are
	 * 
	 * TAG_NAME_ARRAY_BY_LEVEL = "array_by_level" ;
	 * TAG_NAME_CONSTANT = "constant" ;
	 * TAG_NAME_LINEAR_BY_LEVEL = "linear_by_level" ;
	 * 
	 *@param Node - the node which we will traverse.
	 *@return Either a pointer to arLevelFloat, which now contains floating
	 *		point levels indexed by level-1, or null, in the case that no
	 *		supported tag was found.
	 */
	@SuppressWarnings("unused")
	private SimulatedArray loadLevelFloatArrayFromNode( Node node ) {
		if ( node == null )
			return null ;
		// We traverse the node recursively until we find one.
		if ( node.getNodeName().equals( TAG_NAME_ARRAY_BY_LEVEL ) ) {
			// Load the array.
			NamedNodeMap attributes = node.getAttributes() ;
			ArrayList<Node> elements = allDescendentsWithTagName( node, ARRAY_ELEMENT, null ) ;
			float [] arLevelFloat = new float[elements.size()] ;
			for ( int i = 0; i < elements.size(); i++ ) {
				Node element = elements.get(i) ;
				attributes = element.getAttributes() ;
				int index = Integer.parseInt( attributes.getNamedItem(ARRAY_INDEX).getNodeValue() ) ;
				float value = Float.parseFloat( attributes.getNamedItem(ARRAY_VALUE).getNodeValue() ) ;
				arLevelFloat[index-1] = value ;
			}
			// "indices" are by level, so they start from 1.
			return SimulatedArrayFactory.newExplicitPinning(maxLevel, arLevelFloat, elements.size()) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_LINEAR_BY_LEVEL ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			float slope = Float.parseFloat( attributes.getNamedItem(ARRAY_SLOPE).getNodeValue() ) ;
			float intercept = Float.parseFloat( attributes.getNamedItem(ARRAY_INTERCEPT).getNodeValue() ) ;
			// "slope/intercept" is XML-defined according to level number, i.e., slope + intercept
			// is the number at level 1.  However, we always index using level-1, so include
			// slope*1 in the intercept to compensate (index level x-1 corresponds to level x)
			return SimulatedArrayFactory.newSlopeIntercept(maxLevel, slope, intercept+slope) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_LINEAR_BY_LEVEL_PINNING ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			float slope = Float.parseFloat( attributes.getNamedItem(ARRAY_SLOPE).getNodeValue() ) ;
			float intercept = Float.parseFloat( attributes.getNamedItem(ARRAY_INTERCEPT).getNodeValue() ) ;
			int pin = Integer.parseInt( attributes.getNamedItem(ARRAY_PIN).getNodeValue() ) ;
			// "slope/intercept" is XML-defined according to level number, i.e., slope + intercept
			// is the number at level 1.  However, we always index using level-1, so include
			// slope*1 in the intercept to compensate (index level x-1 corresponds to level x).
			// We pin to the provided level.
			return SimulatedArrayFactory.newSlopeInterceptPinning(maxLevel, slope, intercept+slope, pin) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_CONSTANT ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			float val = Float.parseFloat( attributes.getNamedItem(ARRAY_VAL).getNodeValue() ) ;
			return SimulatedArrayFactory.newConstant(maxLevel, val) ;
		}
		
		// If we get here, the current node is not a supported one.  Try it's kids.
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			SimulatedArray sa = loadLevelFloatArrayFromNode( child ) ;
			if ( sa != null )
				return sa ;
		}
		
		// By reaching this point, we have fully traverse the subtree and not found
		// any supported nodes.
		return null ;
	}
	
	
	/**
	 * Looks inside the node for a child with a tag name implying 
	 * it contains level-scaling floats; supported tags are
	 * 
	 * TAG_NAME_ARRAY_BY_LEVEL = "array_by_level" ;
	 * TAG_NAME_CONSTANT = "constant" ;
	 * TAG_NAME_LINEAR_BY_LEVEL = "linear_by_level" ;
	 * 
	 *@param Node - the node which we will traverse.
	 *@return Either a pointer to arLevelDouble, which now contains floating
	 *		point levels indexed by level-1, or null, in the case that no
	 *		supported tag was found.
	 */
	private SimulatedArray loadLevelDoubleArrayFromNode( Node node ) {
		if ( node == null )
			return null ;
		// We traverse the node recursively until we find one.
		if ( node.getNodeName().equals( TAG_NAME_ARRAY_BY_LEVEL ) ) {
			// Load the array.
			NamedNodeMap attributes = node.getAttributes() ;
			ArrayList<Node> elements = allDescendentsWithTagName( node, ARRAY_ELEMENT, null ) ;
			double [] arLevelDouble = new double[elements.size()] ;
			for ( int i = 0; i < elements.size(); i++ ) {
				Node element = elements.get(i) ;
				attributes = element.getAttributes() ;
				int index = Integer.parseInt( attributes.getNamedItem(ARRAY_INDEX).getNodeValue() ) ;
				double value = Double.parseDouble( attributes.getNamedItem(ARRAY_VALUE).getNodeValue() ) ;
				arLevelDouble[index-1] = value ;
			}
			// "indices" are by level, so they start from 1.
			return SimulatedArrayFactory.newExplicitPinning(maxLevel, arLevelDouble, elements.size()) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_LINEAR_BY_LEVEL ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			double slope = Double.parseDouble( attributes.getNamedItem(ARRAY_SLOPE).getNodeValue() ) ;
			double intercept = Double.parseDouble( attributes.getNamedItem(ARRAY_INTERCEPT).getNodeValue() ) ;
			// "slope/intercept" is XML-defined according to level number, i.e., slope + intercept
			// is the number at level 1.  However, we always index using level-1, so include
			// slope*1 in the intercept to compensate (index level x-1 corresponds to level x)
			return SimulatedArrayFactory.newSlopeIntercept(maxLevel, slope, intercept+slope) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_LINEAR_BY_LEVEL_PINNING ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			double slope = Double.parseDouble( attributes.getNamedItem(ARRAY_SLOPE).getNodeValue() ) ;
			double intercept = Double.parseDouble( attributes.getNamedItem(ARRAY_INTERCEPT).getNodeValue() ) ;
			int pin = Integer.parseInt( attributes.getNamedItem(ARRAY_PIN).getNodeValue() ) ;
			// "slope/intercept" is XML-defined according to level number, i.e., slope + intercept
			// is the number at level 1.  However, we always index using level-1, so include
			// slope*1 in the intercept to compensate (index level x-1 corresponds to level x).
			// We pin to the provided level.
			return SimulatedArrayFactory.newSlopeInterceptPinning(maxLevel, slope, intercept+slope, pin) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_CONSTANT ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			double val = Double.parseDouble( attributes.getNamedItem(ARRAY_VAL).getNodeValue() ) ;
			return SimulatedArrayFactory.newConstant(maxLevel, val) ;
		}
		
		// If we get here, the current node is not a supported one.  Try it's kids.
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			SimulatedArray sa = loadLevelDoubleArrayFromNode( child ) ;
			if ( sa != null )
				return sa ;
		}
		
		// By reaching this point, we have fully traverse the subtree and not found
		// any supported nodes.
		return null ;
	}
	
	
	/**
	 * Looks inside the node for a child with a tag name implying 
	 * it contains level-scaling floats; supported tags are
	 * 
	 * TAG_NAME_ARRAY_BY_LEVEL = "array_by_level" ;
	 * TAG_NAME_CONSTANT = "constant" ;
	 * TAG_NAME_LINEAR_BY_LEVEL = "linear_by_level" ;
	 * TAG_NAME_LINEAR_BY_LEVEL_PINNING = "linear_by_level_pinning" ;
	 * 
	 *@param Node - the node which we will traverse.
	 *@return Either a pointer to arLevelInt, which now contains floating
	 *		point levels indexed by level-1, or null, in the case that no
	 *		supported tag was found.
	 */
	private SimulatedArray loadClearIntArrayFromNode( Node node ) {
		if ( node == null )
			return null ;
		// We traverse the node recursively until we find one.
		if ( node.getNodeName().equals( TAG_NAME_ARRAY_BY_CLEAR ) ) {
			// Load the array.
			NamedNodeMap attributes = node.getAttributes() ;
			ArrayList<Node> elements = allDescendentsWithTagName( node, ARRAY_ELEMENT, null ) ;
			int [] arClearInt = new int[elements.size()] ;
			for ( int i = 0; i < elements.size(); i++ ) {
				Node element = elements.get(i) ;
				attributes = element.getAttributes() ;
				int index = Integer.parseInt( attributes.getNamedItem(ARRAY_INDEX).getNodeValue() ) ;
				int value = Integer.parseInt( attributes.getNamedItem(ARRAY_VALUE).getNodeValue() ) ;
				arClearInt[index-1] = value ;
			}
			// "indices" are by level, so they start from 1.
			return SimulatedArrayFactory.newExplicitPinning(Integer.MAX_VALUE, arClearInt, elements.size()) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_CONSTANT ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			int val = Integer.parseInt( attributes.getNamedItem(ARRAY_VAL).getNodeValue() ) ;
			return SimulatedArrayFactory.newConstant(Integer.MAX_VALUE, val) ;
		}
		
		// If we get here, the current node is not a supported one.  Try its kids.
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			SimulatedArray sa = loadClearIntArrayFromNode( child ) ;
			if ( sa != null )
				return sa ;
		}
		
		// By reaching this point, we have fully traverse the subtree and not found
		// any supported nodes.
		return null ;
	}
	
	
	/**
	 * Looks inside the node for a child with a tag name implying 
	 * it contains level-scaling floats; supported tags are
	 * 
	 * TAG_NAME_ARRAY_BY_LEVEL = "array_by_level" ;
	 * TAG_NAME_CONSTANT = "constant" ;
	 * TAG_NAME_LINEAR_BY_LEVEL = "linear_by_level" ;
	 * 
	 *@param Node - the node which we will traverse.
	 *@return Either a pointer to arLevelFloat, which now contains floating
	 *		point levels indexed by level-1, or null, in the case that no
	 *		supported tag was found.
	 */
	@SuppressWarnings("unused")
	private SimulatedArray loadClearFloatArrayFromNode( Node node ) {
		if ( node == null )
			return null ;
		// We traverse the node recursively until we find one.
		if ( node.getNodeName().equals( TAG_NAME_ARRAY_BY_CLEAR ) ) {
			// Load the array.
			NamedNodeMap attributes = node.getAttributes() ;
			ArrayList<Node> elements = allDescendentsWithTagName( node, ARRAY_ELEMENT, null ) ;
			float [] arClearFloat = new float[elements.size()] ;
			for ( int i = 0; i < elements.size(); i++ ) {
				Node element = elements.get(i) ;
				attributes = element.getAttributes() ;
				int index = Integer.parseInt( attributes.getNamedItem(ARRAY_INDEX).getNodeValue() ) ;
				float value = Float.parseFloat( attributes.getNamedItem(ARRAY_VALUE).getNodeValue() ) ;
				arClearFloat[index-1] = value ;
			}
			// "indices" are by level, so they start from 1.
			return SimulatedArrayFactory.newExplicitPinning(Integer.MAX_VALUE, arClearFloat, elements.size()) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_CONSTANT ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			float val = Float.parseFloat( attributes.getNamedItem(ARRAY_VAL).getNodeValue() ) ;
			return SimulatedArrayFactory.newConstant(Integer.MAX_VALUE, val) ;
		}
		
		// If we get here, the current node is not a supported one.  Try it's kids.
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			SimulatedArray sa = loadClearFloatArrayFromNode( child ) ;
			if ( sa != null )
				return sa ;
		}
		
		// By reaching this point, we have fully traverse the subtree and not found
		// any supported nodes.
		return null ;
	}
	
	
	/**
	 * Looks inside the node for a child with a tag name implying 
	 * it contains level-scaling floats; supported tags are
	 * 
	 * TAG_NAME_ARRAY_BY_LEVEL = "array_by_level" ;
	 * TAG_NAME_CONSTANT = "constant" ;
	 * TAG_NAME_LINEAR_BY_LEVEL = "linear_by_level" ;
	 * 
	 *@param Node - the node which we will traverse.
	 *@return Either a pointer to arLevelDouble, which now contains floating
	 *		point levels indexed by level-1, or null, in the case that no
	 *		supported tag was found.
	 */
	private SimulatedArray loadClearDoubleArrayFromNode( Node node ) {
		if ( node == null )
			return null ;
		// We traverse the node recursively until we find one.
		if ( node.getNodeName().equals( TAG_NAME_ARRAY_BY_CLEAR ) ) {
			// Load the array.
			NamedNodeMap attributes = node.getAttributes() ;
			ArrayList<Node> elements = allDescendentsWithTagName( node, ARRAY_ELEMENT, null ) ;
			double [] arClearDouble = new double[elements.size()] ;
			for ( int i = 0; i < elements.size(); i++ ) {
				Node element = elements.get(i) ;
				attributes = element.getAttributes() ;
				int index = Integer.parseInt( attributes.getNamedItem(ARRAY_INDEX).getNodeValue() ) ;
				double value = Double.parseDouble( attributes.getNamedItem(ARRAY_VALUE).getNodeValue() ) ;
				arClearDouble[index-1] = value ;
			}
			// "indices" are by level, so they start from 1.
			return SimulatedArrayFactory.newExplicitPinning(Integer.MAX_VALUE, arClearDouble, elements.size()) ;
		}
		else if ( node.getNodeName().equals( TAG_NAME_CONSTANT ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			double val = Double.parseDouble( attributes.getNamedItem(ARRAY_VAL).getNodeValue() ) ;
			return SimulatedArrayFactory.newConstant(Integer.MAX_VALUE, val) ;
		}
		
		// If we get here, the current node is not a supported one.  Try it's kids.
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			SimulatedArray sa = loadClearDoubleArrayFromNode( child ) ;
			if ( sa != null )
				return sa ;
		}
		
		// By reaching this point, we have fully traverse the subtree and not found
		// any supported nodes.
		return null ;
	}
	
	
	private QuadraticFunction loadQuadraticFunction( Node node ) {
		if ( node == null )
			return null ;
		// We traverse the node recursively until we find one.
		if ( node.getNodeName().equals( TAG_NAME_QUADRATIC ) ) {
			NamedNodeMap attributes = node.getAttributes() ;
			
			double a = Double.parseDouble( attributes.getNamedItem(QUADRATIC_A).getNodeValue() ) ;
			double b = Double.parseDouble( attributes.getNamedItem(QUADRATIC_B).getNodeValue() ) ;
			double c = Double.parseDouble( attributes.getNamedItem(QUADRATIC_C).getNodeValue() ) ;
			
			return new QuadraticFunction().setWithCoefficients(a, b, c) ;
		}
		
		// If we get here, the current node is not a supported one.  Try its kids.
		NodeList children = node.getChildNodes() ;
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item(i) ;
			QuadraticFunction qf = loadQuadraticFunction( child ) ;
			if ( qf != null )
				return qf ;
		}
		
		// We have fully traversed the subtree and not found
		// a quadratic function.
		return null ;
	}
	
}
