package com.peaceray.quantro.model.systems.attack;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QUpconvert;
import com.peaceray.quantro.utils.ArrayOps;

/**
 * This class serves two functions, similar to TriggerSystemAtomicConditions.
 * 
 * Statically it defines a large number of constants for attack system
 * behaviors.  New attack systems these constants to define their behavior
 * upon construction.
 * 
 * Instance-wise, objects of this class store necessary state information
 * for an AttackSystem.  One can effectively regenerate an AttackSystem
 * by constructing it with the same constant behavior settings, hooking it
 * to the appropriate systems and objects, and loading the previous
 * ASAC instance from a Parcel.
 * 
 * AttackSystemAtomicConditions instances do not have the luxury of storing
 * well-formed attacks, such as in an AttackDescriptor; instead, it has
 * to track enough information to determine whether sending an attack is
 * appropriate.  As an atomic step, at the end of every cycle, we package
 * up this situational data into zero or more attacks and add them to the
 * outgoing attack queue.  At that point it is the Game object's responsibility
 * to "dequeue" the attacks and send them off, as appropriate.
 * 
 * In other words, here is the time-ordered functionality of collecting and producing
 * outgoing attacks:
 * 
 * CycleBegins.										Reset any cycle-based parameters
 * 
 * Game events, e.g. clears, cascades, etc.			Store information, partially-collected attacks, etc.
 * 													an AttackSystemAtomicConditions
 * 
 * CycleEnds.										Convert collected parameters into AttackDescriptors.
 * 													Make a queue of outgoing attacks.
 * 
 * "getOutgoingAttackQueue"							A method call (to AttackSystem) to retrieve the
 * 													current queue of outgoing attacks.  This also
 * 													clears the outgoing attack queue for this object.
 * 
 * 
 * @author Jake
 *
 */
class AttackSystemAtomicConditionsVersioned implements Serializable  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2495331425169614648L;


	private static final int VERSION = 1 ; 
	// 0: exact copy of AttackSystemAtomicConditions, with the addition of explicitly representing
	//			the blocks of the most recently locked piece.
	// 1: adds "last target" for many possible attack types.  Used for
	//			certain target behaviors that switch targets based on
	//			previous action (e.g. alternating between cycle next and cycle previous)
	
	// Here are some behaviors for how an attack system generally behaves.
	// Note that this has nothing to do with "TARGET"; in fact, any setting
	// other what OUTGOING will result in target being irrelevant.
	// Target is only relevant to a GameCoordinator, which determines the
	// Game objects towards which AttackDescriptors should be sent;
	// ATTACK_SYSTEM_ATTACKS determines how this AttackSystem deals with
	// its outgoing attack descriptors (discarding them, echoing to incoming,
	// or sending them out).
	private static int en_as = 0 ;
	public static final int ATTACK_SYSTEM_ATTACKS_NONE = en_as++ ;
	public static final int ATTACK_SYSTEM_ATTACKS_SELF = en_as++ ;
	public static final int ATTACK_SYSTEM_ATTACKS_OUTGOING = en_as++ ;
	public static final int ATTACK_SYSTEM_ATTACKS_NUMBER = en_as ;
	
	
	// Here are some behaviors for AttackSystems with regard to garbage rows.
	// First, the behavior for choosing the composition of garbage rows.
	// CLEAR_PIECE: Takes the QC of the piece used to clear.
	// CLEAR_PIECE_CASCADE_MAJORITY: Takes the QC of the piece (for initial clear),
	//				and the majority QC within each cascade set.
	// MAJORITY: Take majority QC among all those rows cleared.
	// CLEAR_ROWS: Ignore the piece; copy the way each row was cleared.
	//				monochromatic is considered equivalent to SS.
	// SS: Ignore the piece; always use SS.
	// S0_S1: Ignore the piece; take Math.floor(#rows / 2) SS, and then (#rows % 2) of either S0 or S1 (random).
	// RAINBOW: Use a "random" value from R0 to R6 for each block.
	// BLAND: Use RAINBOW_BLAND for 1 qpane, 
	private static int en_grc = 0 ;
	public static final int GARBAGE_ROW_COMPOSITION_CLEAR_PIECE = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_CLEAR_PIECE_CASCADE_MAJORITY = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_MAJORITY = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_CLEAR_ROWS = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_SS = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_S0_S1 = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_RAINBOW = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_BLAND = en_grc++ ;
	public static final int GARBAGE_ROW_COMPOSITION_NUMBER = en_grc ;
	
	
	
	// Behavior for choosing the "density" of garbage rows.
	// CLEAR_PIECE_COLUMN_GAP: A 1-block gap near the "center" of the piece.
	// RANDOM_COLUMN_GAP: A 1-block gap in a random column.
	// CLEAR_PIECE_CASCADE_RANDOM_COLUMN_GAP: A 1-block gap near the piece center
	//				for the first clear, 1 random block for cascades.
	// SWISS_CHEESE: Several random gaps within the row; does not match row to row.
	// CLEAR_PIECE_CASCADE_SWISS_CHEESE: A 1-block gap near the piece center for
	//				the first clear, swiss-cheese configuration for cascade clears.
	// QUANTUM_CHESE: Populates the row twice, using consecutive swiss cheese values;
	//				first uses the q=0 value of its COMPOSITION, second using the
	//				q=1 value.  Be VERY VERY CAREFUL of the composition used;
	//				provides unspecified results if invalid QCombinations are formed
	//				by this process.
	// PIECE_NEGATIVE_SYMMETRIC_PREFER_PIECE_CLEAR_NEGATIVE: Gaps are determined by the size and position of
	//				the piece locked during the cycle which triggered the attack.  When possible
	//				(if at least one row in the qPane is cleared by the piece when it locks) than we use
	//				only the piece-negatives from those row(s) which were cleared.  Otherwise, we
	//				we use the negative of the entire piece as a basis.  This 'fallback' case is
	//				obviously intended for cases when garbage is introduced in one pane without
	//				a piece clear happening in that pane; for example, when a clear in one pane
	//				causes (via a 3d block connecting them) a cascade clear in the other pane.
	//				We present a mirrored "back and forth" copy of the piece negative.
	// PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE: Same as the above,
	//				except that we start with vertically mirroring the piece negative rather
	//				than copying it.
	private static int en_grd = 0 ;
	public static final int GARBAGE_ROW_DENSITY_CLEAR_PIECE_COLUMN_GAP = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_RANDOM_COLUMN_GAP = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_CLEAR_PIECE_CASCADE_RANDOM_COLUMN_GAP = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_SWISS_CHEESE = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_CLEAR_PIECE_CASCADE_SWISS_CHEESE = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_QUANTUM_CHEESE = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_PREFER_PIECE_CLEAR_NEGATIVE = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_MANUAL = en_grd++ ;
	public static final int GARBAGE_ROW_DENSITY_NUMBER = en_grd++ ;
	
	
	// Behavior for choosing the number of garbage rows.
	// PIECE_CLEARS_LESS_ONE: The total number of clears, minus one for the initial set
	// CLEARS_LESS_ONE: For each piece clear / cascade, subtract one.
	// PIECE_CLEARS_LESS_ONE_SINGLE_PANE: In the initial set, if only single-pane
	//				clears occur within a single pane (either S0 or S1, not both)
	//				then remove 1.
	// CLEARS_LESS_ONE_SINGLE_PANE: For each set, if only single-pane clears occurs
	//				within a single pane (either S0 or S1, not both) then remove 1.
	// CLEARS_SINCE_PIECE_AT_LEAST_TWO: The total number of clears since the piece locked,
	//				if that number is at least 2.  The number considered is as the sum total
	//				of all sets, including the initial.  Finally, note that Retro game modes
	//				count attack system clears in sets of 2.
	// NONE: Don't use any garbage rows, no matter what.
	private static int en_grn = 0 ;
	public static final int GARBAGE_ROW_NUMBER_NONE = en_grn++ ;
	public static final int GARBAGE_ROW_NUMBER_PIECE_CLEARS_LESS_ONE = en_grn++ ;
	public static final int GARBAGE_ROW_NUMBER_CLEARS_LESS_ONE = en_grn++ ;
	public static final int GARBAGE_ROW_NUMBER_PIECE_CLEARS_LESS_ONE_SINGLE_PANE = en_grn++ ;
	public static final int GARBAGE_ROW_NUMBER_CLEARS_LESS_ONE_SINGLE_PANE = en_grn++ ;
	public static final int GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO = en_grn++ ;
	public static final int GARBAGE_ROW_NUMBER_NUMBER = en_grn ;
	
	
	// SHOULD BE SELF-EXPLANATORY.
	private static int en_gri = 0 ;
	public static final int GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_YES = en_gri++ ;
	public static final int GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO = en_gri++ ;
	public static final int GARBAGE_ROW_INCLUDE_NUMBER = en_gri ;
	
	
	
	// NONE: No garbage rows on level-up.
	// LEVEL_UP_GARBAGE_GAME_INFORMATION: The number specified
	//			in the GameInformation instance upon each level-up.
	// LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_NEW_ONLY: We apply 
	//			the GameInformation garbage ONLY when a new game begins
	//			(as indicated by finalizeConfiguration()), and not again
	//			later.
	// LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW: We apply
	//			upon a new game, and at each level-up.
	private static int en_lug = 0 ;
	public static final int LEVEL_UP_GARBAGE_ROW_NUMBER_NONE = en_lug++ ;
	public static final int LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION = en_lug++ ;
	public static final int LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_NEW_ONLY = en_lug++ ;
	public static final int LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW = en_lug++ ;
	
	
	// Behavior for queueing up garbage rows.
	// SETS_TOGETHER: Queue every row cleared from the piece lock to a new piece
	//				entering (i.e., including cascades) as a single event.
	// SETS_SEQUENTIAL: Queue the initial clears, then each cascade set, as
	//				separate sequential events.
	private static int en_grq = 0 ;
	public static final int GARBAGE_ROW_QUEUE_SETS_TOGETHER = en_grq++ ;
	public static final int GARBAGE_ROW_QUEUE_SETS_SEQUENTIAL = en_grq++ ;
	public static final int GARBAGE_ROW_QUEUE_NUMBER = en_grq++ ;

	
	////////////////////////////////////////////////////////////////////////////
	// PENALTY ROWS
	// PUSH DOWN CLEAR: a penalty row every time a push-down block is cleared.
	private static int en_penr = 0 ;
	public static final int PENALTY_GARBAGE_ROW_NONE = en_penr++ ;
	public static final int PENALTY_GARBAGE_ROW_PUSH_DOWN_CLEAR = en_penr++ ;
	public static final int PENALTY_GARBAGE_ROW_NUMBER = en_penr ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// ROW PUSHES
	// PUSH_DOWN_BLOCK_ACTIVATES: We push a single row out for every 'pushDownBlock' that
	//				activates.  Checks the expansion of QCombinations.PUSH_DOWN
	//				and QCombinations.PUSH_DOWN_ACTIVE to make sure we have an exact
	//				match.
	// PUSH_DOWN_BLOCK_ACTIVATES_OR_PUSH_UP_BLOCK_CLEARS: We push a single row out for every
	//				'pushDownBlock' that activates, or 'pushUpBlock' that clears.
	private static int en_pr = 0 ;
	public static final int PUSH_ROWS_NONE = en_pr++ ;
	public static final int PUSH_ROWS_PUSH_DOWN_BLOCK_ACTIVATES = en_pr++ ;
	public static final int PUSH_ROWS_PUSH_DOWN_BLOCK_ACTIVATES_OR_PUSH_UP_BLOCK_CLEARS = en_pr++ ;
	public static final int PUSH_ROWS_NUMBER = en_pr ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// DISPLACEMENT ROWS
	// DISPLACE_ACCELERATE_CLEARS_IGNORE_ONE: If we get a single clear, ignore it;
	//				otherwise, accelerate exactly the number of clears.
	// DISPLACE_ACCELERATE_HALF_CLEARS_IGNORE_ONE: If we get a single clear, ignore it;
	//				otherwise, accelerate 1/2 the number of rows.  Useful if, e.g.,
	//				you are playing Quantro VS where displacing "2 rows" actually introduces
	//				4 rows of blocks.
	private static int en_da = 0 ;
	public static final int DISPLACE_ACCELERATE_NONE = en_da++ ;
	public static final int DISPLACE_ACCELERATE_CLEARS_IGNORE_ONE = en_da++ ;
	public static final int DISPLACE_ACCELERATE_HALF_CLEARS_IGNORE_ONE = en_da++ ;
	public static final int DISPLACE_ACCELERATE_NUMBER = en_da ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// SYNCHRONIZED LEVEL UPS!
	// MAINTAIN_DIFFERENCE: "level difference" is the distance between starting level
	// 				and current level.  For multiplayer games, this synchronizes
	//				level-ups across players but maintains a handicap caused by
	//				an initial difference in level.
	private static int en_slu = 0 ;
	public static final int SYNCRONIZED_LEVEL_UP_NONE = en_slu++ ;
	public static final int SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE = en_slu++ ;
	
	
	
	///////////////////////////////////////////////////////////////////////
	// Game information!  We never expect this to change.
	int R, C ;
	
	///////////////////////////////////////////////////////////////////////
	// The attacks we will unleash in the current cycle.
	AttackDescriptor attacksToPerformThisCycle ;
	
	///////////////////////////////////////////////////////////////////////
	// Information for incoming attacks that have not yet been executed.
	// These attacks have been fully constructed and determined (although the
	// Descriptor may require processing, the result of that processing is
	// fully determined), but have not yet been retrieved from the AttackSystem.
	// These attacks fit the same format as those constructed by this system
	// instance and placed in outgoingAttackDescriptors, BUT we assume
	// (although this assumption can be ignored, if appropriate) that they
	// originated in another AttackSystem, linked to another Game, possibly
	// running on a different device.  These attacks will be executed on
	// the associated Game.
	int numIncomingAttackDescriptors ;
	ArrayList<AttackDescriptor> incomingAttackDescriptors ;
	
	
	///////////////////////////////////////////////////////////////////////
	// Information for outgoing attacks that have not yet been retrieved.
	// These attacks have been fully constructed and determined (although the
	// Descriptor may require processing, the result of that processing is
	// fully determined), but have not yet been retrieved from the AttackSystem.
	// Ultimately, these attacks are destined for another Game with another
	// AttackSystem, and thus another AttackSystemAtomicConditions.
	int numOutgoingAttackDescriptors ;
	ArrayList<AttackDescriptor> outgoingAttackDescriptors ;
	
	
	///////////////////////////////////////////////////////////////////////
	// Information for collecting and constructing outgoing attacks.  These
	// fields track the current progress the player has made.  Upon the end
	// of a cycle, it will be converted to 0 or more AttackDescriptors, which
	// should be placed in the outgoingAttackQueue.
	//
	// Attack type: garbage rows.  we track clears based on column, piece
	// type, pseudorandom values, etc.  An assumption: with R the number of rows
	// provided by a clear array, there can be at most R*2 clears in a cycle:
	// a clear removes at least 1 full row's worth of blocks.  We therefore allocate
	// R*2 of space when needed.
	
	// Here's what we need to track a particular "state."
	// Tracking garbage rows
	// As clears are performed, note important row information in "incomingGarbageRows."
	// Once the next piece is prepared (meaning the game is in a stable state),
	// convert them to "outgoing," performing any necessary processing along the way.
	//
	// For a given cycle, we need to retain: the piece type that caused the clear,
	// the cascade number for each row, and the QCombination associated with the clear
	// (which is specified by a BEHAVIOR setting).  Other values, such as Pseudorandom Number
	// generation, is deterministic from the above.
	//
	// Upon ending a cycle, we combine rows, such as S0 and S1 being represented as SS.
	// Otherwise, there is very little change from the collected version to AttackDescriptor
	// version.
	
	// randomization value
	int pseudorandom ;
	
	// game state info
	int pieceType ;
	int pieceColumn ;
	int cascadeNumber ;
	
	int [][] pieceLockedBlocks ;
	boolean [][] pieceCleared ;
	// represents the encoding of the most recently locked piece
	// at its locked horizontal position, and whether a piece-clear occurred
	// across that row of the piece.
	
	// clear row info:
	int [] clearRowQCombinations ;
	int [] clearRowCascadeNumber ;
	boolean [] clearRowBothPanesCleared ;
	int numClearedRows ;
	int numClearedRowsBeforePieceLock ;
	
	
	// Target info
	// Some target behavior requires remembering previous targets
	// (for example, "cycle alternate" will send alternating Attacks
	// to CYCLE_NEXT and CYCLE_PREVIOUS; we need to remember the target
	// of the previous attack.
	int lastTargetCode_clearedAndSent ;
	int lastTargetCode_pushRow ;
	int lastTargetCode_displaceRow ;
	int lastTargetCode_dropBlocks ;

	
	AttackSystemAtomicConditionsVersioned( int rows, int cols ) {
		// Stuff we don't expect to change
		R = rows ;
		C = cols ;
		pseudorandom = 0 ;
		
		attacksToPerformThisCycle = new AttackDescriptor(R,C) ;
		
		numIncomingAttackDescriptors = 0 ;
		incomingAttackDescriptors = new ArrayList<AttackDescriptor>() ;
		
		// Fully-formed and realized attacks.
		numOutgoingAttackDescriptors = 0 ;
		outgoingAttackDescriptors = new ArrayList<AttackDescriptor>() ;
		
		
		///////////////////////////////////////////////////////////////
		// Attacks under-construction.
		// Space for storing cleared row information.  We will eventually
		// package these up into AttackDescriptors, if indeed the situation
		// described merits an attack.
		numClearedRows = 0 ;
		numClearedRowsBeforePieceLock = 0 ;
		clearRowQCombinations = new int[R*2] ;		// Assumption: we can never have more than R*2 row clears
		clearRowCascadeNumber = new int[R*2] ;		// in any given cycle.  This assumption is derived from the
		clearRowBothPanesCleared = new boolean[R*2] ;	// underlying assumption that a clear will remove at LEAST
														// "c" blocks from a maximum of 2*R*C in the blockField.
														// TODO: If clears can occur in sparse rows (with fewer than C blocks),
														// lengthen these arrays.
		
		
		cascadeNumber = 0 ;
		pieceColumn = 0 ;
		pieceType = -1 ;
		
		pieceLockedBlocks = new int[5][2] ;
		pieceCleared = new boolean[5][2] ;
		
		lastTargetCode_clearedAndSent = AttackDescriptor.TARGET_UNSET ;
		lastTargetCode_pushRow = AttackDescriptor.TARGET_UNSET ;
		lastTargetCode_displaceRow = AttackDescriptor.TARGET_UNSET ;
		lastTargetCode_dropBlocks = AttackDescriptor.TARGET_UNSET ;
	}
	
	
	AttackSystemAtomicConditionsVersioned( AttackSystemAtomicConditions asac ) {
		///////////////////////////////////////////////////////////////////////
		// Game information!  We never expect this to change.
		R = asac.R ;
		C = asac.C ;
		
		///////////////////////////////////////////////////////////////////////
		// The attacks we will unleash in the current cycle.
		attacksToPerformThisCycle = new AttackDescriptor(asac.attacksToPerformThisCycle) ;
		
		///////////////////////////////////////////////////////////////////////
		// Information for incoming attacks that have not yet been executed.
		// These attacks have been fully constructed and determined (although the
		// Descriptor may require processing, the result of that processing is
		// fully determined), but have not yet been retrieved from the AttackSystem.
		// These attacks fit the same format as those constructed by this system
		// instance and placed in outgoingAttackDescriptors, BUT we assume
		// (although this assumption can be ignored, if appropriate) that they
		// originated in another AttackSystem, linked to another Game, possibly
		// running on a different device.  These attacks will be executed on
		// the associated Game.
		numIncomingAttackDescriptors = asac.numIncomingAttackDescriptors ;
		incomingAttackDescriptors = new ArrayList<AttackDescriptor>() ;
		Iterator<com.peaceray.quantro.model.descriptors.AttackDescriptor> iadIter = asac.incomingAttackDescriptors.iterator() ;
		for ( ; iadIter.hasNext() ; )
			incomingAttackDescriptors.add( new AttackDescriptor( iadIter.next() ) ) ;
		
		
		///////////////////////////////////////////////////////////////////////
		// Information for outgoing attacks that have not yet been retrieved.
		// These attacks have been fully constructed and determined (although the
		// Descriptor may require processing, the result of that processing is
		// fully determined), but have not yet been retrieved from the AttackSystem.
		// Ultimately, these attacks are destined for another Game with another
		// AttackSystem, and thus another AttackSystemAtomicConditions.
		outgoingAttackDescriptors = new ArrayList<AttackDescriptor>() ;
		Iterator<com.peaceray.quantro.model.descriptors.AttackDescriptor> oadIter = asac.outgoingAttackDescriptors.iterator() ;
		for ( ; oadIter.hasNext() ; )
			outgoingAttackDescriptors.add( new AttackDescriptor( oadIter.next() ) ) ;
		
		
		///////////////////////////////////////////////////////////////////////
		// Information for collecting and constructing outgoing attacks.  These
		// fields track the current progress the player has made.  Upon the end
		// of a cycle, it will be converted to 0 or more AttackDescriptors, which
		// should be placed in the outgoingAttackQueue.
		//
		// Attack type: garbage rows.  we track clears based on column, piece
		// type, pseudorandom values, etc.  An assumption: with R the number of rows
		// provided by a clear array, there can be at most R*2 clears in a cycle:
		// a clear removes at least 1 full row's worth of blocks.  We therefore allocate
		// R*2 of space when needed.
		
		// Here's what we need to track a particular "state."
		// Tracking garbage rows
		// As clears are performed, note important row information in "incomingGarbageRows."
		// Once the next piece is prepared (meaning the game is in a stable state),
		// convert them to "outgoing," performing any necessary processing along the way.
		//
		// For a given cycle, we need to retain: the piece type that caused the clear,
		// the cascade number for each row, and the QCombination associated with the clear
		// (which is specified by a BEHAVIOR setting).  Other values, such as Pseudorandom Number
		// generation, is deterministic from the above.
		//
		// Upon ending a cycle, we combine rows, such as S0 and S1 being represented as SS.
		// Otherwise, there is very little change from the collected version to AttackDescriptor
		// version.
		
		// randomization value
		pseudorandom = asac.pseudorandom ;
		
		// game state info
		pieceType = asac.pieceType ;
		pieceColumn = asac.pieceColumn ;
		cascadeNumber = asac.cascadeNumber ;
		
		pieceLockedBlocks = new int[5][2] ;
		pieceCleared = new boolean[5][2] ;
		
		// clear row info:
		clearRowQCombinations = ArrayOps.duplicate(asac.clearRowQCombinations) ;
		clearRowCascadeNumber = ArrayOps.duplicate(asac.clearRowCascadeNumber) ;
		clearRowBothPanesCleared = new boolean[R*2] ;
		numClearedRows = asac.numClearedRows ;
		numClearedRowsBeforePieceLock = 0 ;
		
		lastTargetCode_clearedAndSent = AttackDescriptor.TARGET_UNSET ;
		lastTargetCode_pushRow = AttackDescriptor.TARGET_UNSET ;
		lastTargetCode_displaceRow = AttackDescriptor.TARGET_UNSET ;
		lastTargetCode_dropBlocks = AttackDescriptor.TARGET_UNSET ;
	}
	
	
	AttackSystemAtomicConditionsVersioned( AttackSystemAtomicConditionsVersioned asac ) {
		///////////////////////////////////////////////////////////////////////
		// Game information!  We never expect this to change.
		R = asac.R ;
		C = asac.C ;
		
		///////////////////////////////////////////////////////////////////////
		// The attacks we will unleash in the current cycle.
		attacksToPerformThisCycle = new AttackDescriptor(asac.attacksToPerformThisCycle) ;
		
		///////////////////////////////////////////////////////////////////////
		// Information for incoming attacks that have not yet been executed.
		// These attacks have been fully constructed and determined (although the
		// Descriptor may require processing, the result of that processing is
		// fully determined), but have not yet been retrieved from the AttackSystem.
		// These attacks fit the same format as those constructed by this system
		// instance and placed in outgoingAttackDescriptors, BUT we assume
		// (although this assumption can be ignored, if appropriate) that they
		// originated in another AttackSystem, linked to another Game, possibly
		// running on a different device.  These attacks will be executed on
		// the associated Game.
		numIncomingAttackDescriptors = asac.numIncomingAttackDescriptors ;
		incomingAttackDescriptors = new ArrayList<AttackDescriptor>() ;
		Iterator<AttackDescriptor> iadIter = asac.incomingAttackDescriptors.iterator() ;
		for ( ; iadIter.hasNext() ; )
			incomingAttackDescriptors.add( new AttackDescriptor( iadIter.next() ) ) ;
		
		
		///////////////////////////////////////////////////////////////////////
		// Information for outgoing attacks that have not yet been retrieved.
		// These attacks have been fully constructed and determined (although the
		// Descriptor may require processing, the result of that processing is
		// fully determined), but have not yet been retrieved from the AttackSystem.
		// Ultimately, these attacks are destined for another Game with another
		// AttackSystem, and thus another AttackSystemAtomicConditions.
		outgoingAttackDescriptors = new ArrayList<AttackDescriptor>() ;
		Iterator<AttackDescriptor> oadIter = asac.outgoingAttackDescriptors.iterator() ;
		for ( ; oadIter.hasNext() ; )
			outgoingAttackDescriptors.add( new AttackDescriptor( oadIter.next() ) ) ;
		
		
		///////////////////////////////////////////////////////////////////////
		// Information for collecting and constructing outgoing attacks.  These
		// fields track the current progress the player has made.  Upon the end
		// of a cycle, it will be converted to 0 or more AttackDescriptors, which
		// should be placed in the outgoingAttackQueue.
		//
		// Attack type: garbage rows.  we track clears based on column, piece
		// type, pseudorandom values, etc.  An assumption: with R the number of rows
		// provided by a clear array, there can be at most R*2 clears in a cycle:
		// a clear removes at least 1 full row's worth of blocks.  We therefore allocate
		// R*2 of space when needed.
		
		// Here's what we need to track a particular "state."
		// Tracking garbage rows
		// As clears are performed, note important row information in "incomingGarbageRows."
		// Once the next piece is prepared (meaning the game is in a stable state),
		// convert them to "outgoing," performing any necessary processing along the way.
		//
		// For a given cycle, we need to retain: the piece type that caused the clear,
		// the cascade number for each row, and the QCombination associated with the clear
		// (which is specified by a BEHAVIOR setting).  Other values, such as Pseudorandom Number
		// generation, is deterministic from the above.
		//
		// Upon ending a cycle, we combine rows, such as S0 and S1 being represented as SS.
		// Otherwise, there is very little change from the collected version to AttackDescriptor
		// version.
		
		// randomization value
		pseudorandom = asac.pseudorandom ;
		
		// game state info
		pieceType = asac.pieceType ;
		pieceColumn = asac.pieceColumn ;
		cascadeNumber = asac.cascadeNumber ;
		
		pieceLockedBlocks = ArrayOps.duplicate(asac.pieceLockedBlocks) ;
		pieceCleared = ArrayOps.duplicate(asac.pieceCleared) ;
		
		// clear row info:
		clearRowQCombinations = ArrayOps.duplicate(asac.clearRowQCombinations) ;
		clearRowCascadeNumber = ArrayOps.duplicate(asac.clearRowCascadeNumber) ;
		clearRowBothPanesCleared = ArrayOps.duplicate(asac.clearRowBothPanesCleared) ; 
		numClearedRows = asac.numClearedRows ;
		numClearedRowsBeforePieceLock = asac.numClearedRowsBeforePieceLock ;
		
		// last target code:
		lastTargetCode_clearedAndSent = asac.lastTargetCode_clearedAndSent ;
		lastTargetCode_pushRow = asac.lastTargetCode_pushRow ;
		lastTargetCode_displaceRow = asac.lastTargetCode_displaceRow ;
		lastTargetCode_dropBlocks = asac.lastTargetCode_dropBlocks ;
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(VERSION) ;
		// Write all our info.
		// Write constant information.
		stream.writeInt(R) ;
		stream.writeInt(C) ;
		stream.writeInt(pseudorandom) ;
		
		attacksToPerformThisCycle.writeObject(stream) ;
		
		// Write incoming attack descriptions
		stream.writeInt( this.numIncomingAttackDescriptors ) ;
		for ( int i = 0; i < numIncomingAttackDescriptors; i++ )
			incomingAttackDescriptors.get(i).writeObject(stream) ;		
		
		// Write outgoing attack descriptions
		stream.writeInt( this.numOutgoingAttackDescriptors ) ;
		for ( int i = 0; i < numOutgoingAttackDescriptors; i++ )
			outgoingAttackDescriptors.get(i).writeObject(stream) ;
		
		// Write game state information.
		stream.writeInt(pieceType) ;
		stream.writeInt(pieceColumn) ;
		stream.writeInt(cascadeNumber) ;
		
		stream.writeObject(pieceLockedBlocks) ;
		stream.writeObject(pieceCleared) ;
		
		// Write cleared row information
		stream.writeInt(numClearedRows) ;
		stream.writeInt(numClearedRowsBeforePieceLock) ;
		// Write the full array, because why not.
		stream.writeObject(clearRowQCombinations) ;
		stream.writeObject(clearRowCascadeNumber) ;
		stream.writeObject(clearRowBothPanesCleared) ;
		
		// We DO have more.
		stream.writeBoolean(true) ;
		
		// Write 'last target' codes.
		stream.writeInt(lastTargetCode_clearedAndSent) ;
		stream.writeInt(lastTargetCode_pushRow) ;
		stream.writeInt(lastTargetCode_displaceRow) ;
		stream.writeInt(lastTargetCode_dropBlocks) ;
		
		// We do NOT have more.
		stream.writeBoolean(false) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Read version.
		int version = stream.readInt() ;
		
		// Read info.
		// Read constant information.
		R = stream.readInt() ;
		C = stream.readInt() ;
		pseudorandom = stream.readInt() ;
		
		attacksToPerformThisCycle = new AttackDescriptor(R,C) ;
		attacksToPerformThisCycle.readObject(stream) ;
		
		// Read incoming attack descriptions
		this.numIncomingAttackDescriptors = stream.readInt() ;
		incomingAttackDescriptors = new ArrayList<AttackDescriptor>( numIncomingAttackDescriptors ) ;
		for ( int i = 0; i < numIncomingAttackDescriptors; i++ ) {
			incomingAttackDescriptors.add( new AttackDescriptor( R, C ) ) ;
			incomingAttackDescriptors.get(i).readObject(stream) ;
		}
		
		// Read outgoing attack descriptions
		this.numOutgoingAttackDescriptors = stream.readInt() ;
		outgoingAttackDescriptors = new ArrayList<AttackDescriptor>( numOutgoingAttackDescriptors ) ;
		for ( int i = 0; i < numOutgoingAttackDescriptors; i++ ) {
			outgoingAttackDescriptors.add( new AttackDescriptor( R, C ) ) ;
			outgoingAttackDescriptors.get(i).readObject(stream) ;
		}
		
		// Read game state information.
		pieceType = stream.readInt() ;
		pieceColumn = stream.readInt() ;
		cascadeNumber = stream.readInt() ;
		
		pieceLockedBlocks = (int [][])stream.readObject() ;
		pieceCleared = (boolean [][])stream.readObject() ;
		
		// Read cleared row information
		numClearedRows = stream.readInt() ;
		numClearedRowsBeforePieceLock = stream.readInt() ;
		// Read the full array.
		clearRowQCombinations = (int [])stream.readObject() ;
		clearRowCascadeNumber = (int [])stream.readObject() ;
		clearRowBothPanesCleared = (boolean [])stream.readObject() ;
		
		if ( version >= 1 ) {
			// read the "finished" boolean
			stream.readBoolean() ;
			lastTargetCode_clearedAndSent = stream.readInt() ;
			lastTargetCode_pushRow = stream.readInt() ;
			lastTargetCode_displaceRow = stream.readInt() ;
			lastTargetCode_dropBlocks = stream.readInt() ;
		} else {
			lastTargetCode_clearedAndSent = AttackDescriptor.TARGET_UNSET ;
			lastTargetCode_pushRow = AttackDescriptor.TARGET_UNSET ;
			lastTargetCode_displaceRow = AttackDescriptor.TARGET_UNSET ;
			lastTargetCode_dropBlocks = AttackDescriptor.TARGET_UNSET ;
		}
		
		// read the 'false' indicating no more.
		stream.readBoolean() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required ASAC structure.") ;
	}
	
	
	public AttackSystemAtomicConditionsVersioned qUpconvert() {
		
		AttackSystemAtomicConditionsVersioned asacv = new AttackSystemAtomicConditionsVersioned(this) ;
		
		asacv.attacksToPerformThisCycle = asacv.attacksToPerformThisCycle.qUpconvert() ;
		
		for ( int i = 0; i < numIncomingAttackDescriptors; i++ ) {
			asacv.incomingAttackDescriptors.set(i,
					incomingAttackDescriptors.get(i).qUpconvert()) ;
		}
		for ( int i = 0; i < numOutgoingAttackDescriptors; i++ ) {
			asacv.outgoingAttackDescriptors.set(i,
					outgoingAttackDescriptors.get(i).qUpconvert()) ;
		}
		
		asacv.pieceType = PieceCatalog.qUpconvert(pieceType) ;
		
		for ( int i = 0; i < clearRowQCombinations.length; i++ )
			asacv.clearRowQCombinations[i] = QUpconvert.upConvert( clearRowQCombinations[i] ) ;
		
		return asacv ;
	}
	
}
