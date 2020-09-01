package com.peaceray.quantro.model.systems.attack;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.displacement.DisplacementSystem;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.IntStack;

public class AttackSystem implements SerializableState {
	
	private static final int [] RANDOM_NUMBERS =
		new	int	[]	{
		154, 1,   28,  141, 117, 96,  41,  0,   135, 114,
		37,  7,   140, 84,  155, 66,  90,  128, 24,  124,
		53,  33,  123, 81,  51,  12,  156, 127, 35,  36,
		32,  132, 61,  55,  14,  2,   166, 78,  68,  59,
		164, 69,  19,  106, 39,  101, 4,   162, 112, 92,
		149, 161, 8,   131, 44,  150, 95,  42,  100, 46,
		11,  82,  159, 43,  151, 98,  115, 107, 167, 110,
		72,  54,  147, 30,  97,  129, 165, 91,  56,  40,
		93,  118, 153, 57,  136, 18,  148, 160, 137, 47,
		145, 65,  6,   144, 134, 26,  103, 163, 25,  70,
		109, 58,  146, 52,  142, 13,  10,  126, 34,  60,
		139, 16,  5,   23,  62,  89,  105, 29,  71,  125,
		152, 3,   86,  102, 75,  104, 87,  88,  143, 83,
		85,  99,  113, 73,  22,  116, 120, 15,  119, 45,
		130, 63,  74,  133, 38,  111, 79,  21,  31,  27,
		49,  64,  121, 17,  50,  9,   122, 67,  157, 80,
		48,  77,  20,  94,  138, 108, 158, 76 
		}	;
	// "Random numbers"; teh numbers 0...167, in a randomized
	// order.  This is is here to allow "random" behavior without
	// random number generation.  The number of entries is evenly
	// divisible by both 2, 7, 8, 10, making it appropriate for
	// fully pseudorandom selection within S0/S1, R0...R6, Quantro columns,
	// Retro columns.
	
	private static final int A_PRIME = 101 ;
	private static final int B_PRIME = 103 ;
	private static final int C_PRIME = 37 ;
	private static final int D_PRIME = 17 ;
	private static final int E_PRIME = 13 ;
	private static final int [] SWISS_CHEESE =
		new int [] {0,1,1,0,1,1,0,1,			
					0,1,0,1,0,1,0,1,
					1,1,0,1,1,1,0,1,
					1,0,1,0,1,0,1,1,
					0,1,1,0,1,1,0,1,
					1,1,0,1,1,0,1,1,
					0,1,1,0,1,1,1,0,
					1,0,1,0,1,1,0,1,
					1,1,0,1,0,1,1,1,
					1,0,1,1,0,1,0,1,
					1,0,1,0,1,0,1,1,
					0,1,1,0,1,0,1,1,
					1,1,0,1,1,0,1,1,
					0,1,0,1,0,1,1,0,
					1,1,1,0,1,1,1,0,
					1,0,1,0,1,1,1,0,
					1,1,0,1,1,0,1,0,
					1,0,1,1,1,0,1,0, 
					1,0,1,1,0,1,1, 
				} ;
	// determined by die roll.  Guaranteed to be random.
	// Actually, the above is a set of random binary numbers
	// from random.org, with runs of zeros reduced to 1.
	// This is intended to give random "looking" swiss
	// cheese in an easily repeatable way.
	// The length is prime (it is 151).
	//
	// Why not just use Random?  Because we are being future-looking,
	// and recognize that it is very difficult to keep Random
	// objects running on different machines "in-sync".  This
	// implementation allows the behavior of the garbage rows
	// to *appear* random, while actually being fully deterministic
	// based on the piece type and piece column.
	//
	// When filling garbage rows with swiss cheese, we use the formula
	// (column + type + A_PRIME*cascade_number + B_PRIME*garbage_row_number) % SWISS_CHEESE.length
	// as the initial index into the above, then step along as far
	// as is required.
	//
	// Determining a random "column" is similar; we use
	// (column + type + A_PRIME*cascade_number) % C, with C the # columns.
	//
	// Basically, we try to keep random number generation safely within
	// the confines of the pieceBag.	
	
	
	
	// ADDITIONAL TARGET TYPES: Each attack has a designated target;
	// these codes are defined in AttackDescriptor.  However, we allow
	// a few more unique target settings that will be translated into a
	// standard target type when the AttackDescriptor is output.
	static final int TARGET_UNSET = 			AttackDescriptor.TARGET_UNSET ;
	static final int TARGET_INCOMING = 			AttackDescriptor.TARGET_INCOMING ;
	static final int TARGET_CYCLE_NEXT =  		AttackDescriptor.TARGET_CYCLE_NEXT ;
	static final int TARGET_CYCLE_PREVIOUS = 	AttackDescriptor.TARGET_CYCLE_PREVIOUS ;
	static final int TARGET_ALL = 				AttackDescriptor.TARGET_ALL ;
	static final int TARGET_ALL_OTHERS = 		AttackDescriptor.TARGET_ALL_OTHERS ;
	static final int TARGET_ALL_DIVIDED = 		AttackDescriptor.TARGET_ALL_DIVIDED ;
	static final int TARGET_ALL_OTHERS_DIVIDED = AttackDescriptor.TARGET_ALL_OTHERS_DIVIDED ;
	
	// Cycle targets fron NEXT to PREVIOUS.  Change with every attack descriptor
	// output.
	static final int TARGET_CYCLE_ALTERNATE = 	AttackDescriptor.NUM_TARGET_CODES ;
	
	
	// An attack system has its own AtomicConditions
	private AttackSystemAtomicConditionsVersioned asac ;
	
	// Configured?
	private boolean configured ;
	
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	// Behavior for attack system
	private int behavior_attackSystemAttacks ;
	
	// Behavior for cleared and sent garbage rows
	private int behavior_clearedAndSent_garbageRowComposition ;
	private int behavior_clearedAndSent_garbageRowDensity ;
	private int behavior_clearedAndSent_garbageRowNumber ;
	private int behavior_clearedAndSent_garbageRowQueue ;
	private int behavior_clearedAndSent_garbageRowInclude ;
	// target?
	private int behavior_clearedAndSent_target ;
	
	// Behavior for levelUp garbage rows.
	private int behavior_levelUp_garbageRowComposition ;
	private int behavior_levelUp_garbageRowDensity ;
	private int behavior_levelUp_garbageRowNumber ;
	// Defaults for levelUp garbage, if not set.
	private int default_levelUp_garbageRowNumber ;
	
	// Behavior for penalty rows.
	private int behavior_penalty_garbageRow ;
	
	// Behavior for row pushes.
	private int behavior_pushRow ;
	// target?
	private int behavior_pushRow_target ;
	
	// Behavior for displacement rows.
	private int behavior_displaceRow ;
	// target?
	private int behavior_displaceRow_target ;
	
	// Behavior for synchronized level gains.
	private int behavior_syncLevelUp ;
	
	
	// Dropping blocks is a direct-api effect (we don't generally
	// "auto-drop" within ourself, hence the lack of behavior
	// variables).  However, it's worth noting a target.
	private int behavior_dropBlocks_attackTarget ;
	
	// New-game blocks
	private int newGame_blocksForValleys ;
	private int newGame_blocksForJunctions ;
	private int newGame_blocksForPeaks ;
	private int newGame_blocksForCorners ;
	private int newGame_blocksForTrolls ;
	
	
	
	
	// Truly temporary structures, not retained between
	// method calls.
	private int [] qcCount ;
	private AttackDescriptor tempAD ;
	private byte [] testQExpansion ;

	private boolean [][][] tempBoolField = null ;
	private IntStack tempRStack ;
	private IntStack tempCStack ;
	private IntStack tempQStack ;
	
	AttackSystem(GameInformation ginfo, QInteractions qi) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		asac = new AttackSystemAtomicConditionsVersioned( GameModes.numberRows(ginfo)*2, GameModes.numberColumns(ginfo) ) ;
		configured = false ;
		
		// By default, no behavior.
		this.behavior_clearedAndSent_garbageRowNumber = AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_NONE ;
		this.behavior_levelUp_garbageRowNumber = AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_NONE ;
		this.behavior_penalty_garbageRow = AttackSystemAtomicConditionsVersioned.PENALTY_GARBAGE_ROW_NONE ;
		this.behavior_pushRow = AttackSystemAtomicConditionsVersioned.PUSH_ROWS_NONE ;
		this.behavior_displaceRow = AttackSystemAtomicConditionsVersioned.DISPLACE_ACCELERATE_NONE ;
		this.behavior_syncLevelUp = AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_NONE ;
		
		// Defaults are typically 0.
		this.default_levelUp_garbageRowNumber = 0 ;
		
		// targets?
		this.behavior_clearedAndSent_target = TARGET_ALL_OTHERS ;
		this.behavior_pushRow_target = TARGET_ALL_OTHERS ;
		this.behavior_displaceRow_target = TARGET_ALL_OTHERS_DIVIDED ;
		this.behavior_dropBlocks_attackTarget = TARGET_CYCLE_NEXT ;
		
		this.newGame_blocksForValleys = 0 ;
		this.newGame_blocksForJunctions = 0 ;
		this.newGame_blocksForPeaks = 0 ;
		this.newGame_blocksForCorners = 0 ;
		this.newGame_blocksForTrolls = 0 ;
		
		// Allocate temporary stuff
		qcCount = new int[QCombinations.NUM] ;
		tempAD = new AttackDescriptor( GameModes.numberRows(ginfo)*2, GameModes.numberColumns(ginfo) ) ;
		testQExpansion = new byte[2] ;
		
		tempRStack = null ;
		tempCStack = null ;
		tempQStack = null ;
	}
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this ClearSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public GameInformation getGameInformation() {
		return ginfo ;
	}
	
	/**
	 * getQInteractions: Clears are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public QInteractions getQInteractions() {
		return this.qi ;
	}

	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting AttackSystem behaviors
	 *
	 * *******************************************************
	 */
	void setBehavior_attackSystemAttacks( int asa ) {
		if ( configured )
			throw new IllegalStateException("Trying to change attack system attacks to " + asa + " after finalizeConfiguration!") ;
		this.behavior_attackSystemAttacks = asa ;
	}
	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting Cleared and set Garbage Row behaviors.  
	 *
	 * *******************************************************
	 */
	void setBehavior_clearedAndSent_garbageRowComposition( int bgrc ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row composition behavior to " + bgrc + " after finalizeConfiguration!") ;
		this.behavior_clearedAndSent_garbageRowComposition = bgrc ;
	}
	
	void setBehavior_clearedAndSent_garbageRowDensity( int bgrd ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row density behavior to " + bgrd + " after finalizeConfiguration!") ;
		this.behavior_clearedAndSent_garbageRowDensity = bgrd ;
	}
	
	void setBehavior_clearedAndSent_garbageRowNumber( int bgrn ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row number behavior to " + bgrn + " after finalizeConfiguration!") ;
		this.behavior_clearedAndSent_garbageRowNumber = bgrn ;
	}
	void setBehavior_clearedAndSent_garbageRowQueue( int bgrq ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row queue behavior to " + bgrq + " after finalizeConfiguration!") ;
		this.behavior_clearedAndSent_garbageRowQueue = bgrq ;
	}
	void setBehavior_clearedAndSent_garbageRowInclude( int bgri ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row include behavior to " + bgri + " after finalizeConfiguration!") ;
		this.behavior_clearedAndSent_garbageRowInclude = bgri ;
	}
	void setBehavior_clearedAndSent_garbageRowTarget( int targetCode ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row target behavior to " + targetCode + " after finalizeConfiguration!") ;
		this.behavior_clearedAndSent_target = targetCode ;
	}
	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting level-up Garbage Row behaviors.  
	 *
	 * *******************************************************
	 */
	void setBehavior_levelUp_garbageRowComposition( int blugrc ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row composition behavior to " + blugrc + " after finalizeConfiguration!") ;
		this.behavior_levelUp_garbageRowComposition = blugrc ;
	}
	
	void setBehavior_levelUp_garbageRowDensity( int blugrd ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row density behavior to " + blugrd + " after finalizeConfiguration!") ;
		this.behavior_levelUp_garbageRowDensity = blugrd ;
	}
	
	void setBehavior_levelUp_garbageRowNumber( int blugrn ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row number behavior to " + blugrn + " after finalizeConfiguration!") ;
		this.behavior_levelUp_garbageRowNumber = blugrn ;
	}
	
	public void setDefault_levelUp_garbageRowNumber( int dlugrn ) {
		if ( configured )
			throw new IllegalStateException("Trying to change default garbage row number to " + dlugrn + " after finalizeConfiguration!") ;
		this.default_levelUp_garbageRowNumber = dlugrn ;
	}
	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting penalty row behaviors.
	 *
	 * *******************************************************
	 */
	
	public void setBehavior_penaltyRow( int penrb ) {
		if ( configured )
			throw new IllegalStateException("Trying to change penalty row behavior " + penrb + " after finalizeConfiguration!") ;
		this.behavior_penalty_garbageRow = penrb ;
	}
	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting push block behaviors.
	 *
	 * *******************************************************
	 */
	
	public void setBehavior_pushRow( int prb ) {
		if ( configured )
			throw new IllegalStateException("Trying to change push row behavior " + prb + " after finalizeConfiguration!") ;
		this.behavior_pushRow = prb ;
	}
	
	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting displacement acceleration behaviors.
	 *
	 * *******************************************************
	 */
	
	public void setBehavior_displaceRow( int drb ) {
		if ( configured )
			throw new IllegalStateException("Trying to change displace row behavior " + drb + " after finalizeConfiguration!") ;
		this.behavior_displaceRow = drb ;
	}
	
	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting synchronized level ups behaviors.  
	 *
	 * *******************************************************
	 */
	
	public void setBehavior_syncLevelUp( int bslu ) {
		if ( configured )
			throw new IllegalStateException("Trying to change garbage row number behavior to " + bslu + " after finalizeConfiguration!") ;
		this.behavior_syncLevelUp = bslu ;
	}
	
	
	/*
	 * *******************************************************
	 *
	 * Configuration:
	 * Setting new game drop block behaviors.  
	 *
	 * *******************************************************
	 */
	
	
	public void setNewGame_dropBlocks( int valley, int junction, int peak, int corner, int troll ) {
		if ( configured )
			throw new IllegalStateException("Trying to change new game blocks after finalizeConfiguration!") ;
		this.newGame_blocksForValleys = valley ;
		this.newGame_blocksForJunctions = junction ;
		this.newGame_blocksForPeaks = peak ;
		this.newGame_blocksForCorners = corner ;
		this.newGame_blocksForTrolls = troll ;
	}
	
	
	/*
	 * *******************************************************
	 *
	 * Stateful use:
	 *
	 * *******************************************************
	 */
	
	
	////////////////////////////////////////////////////////////
	// 
	// SETTING PSEUDORANDOM
	//
	////////////////////////////////////////////////////////////
	
	public void setPseudorandom( int pseudorandom ) {
		asac.pseudorandom = Math.abs(pseudorandom) ;
	}
	
	
	////////////////////////////////////////////////////////////
	//
	// CONSTRUCTING ATTACKS
	//
	// These methods allow the attack system to monitor the game,
	// constructing "outgoing" attacks that should be applied to other
	// players.  Relevant status update methods are called by the Game
	// during a piece cycle, then 'endCycle' is called upon
	// completion (i.e., upon every possible alteration of the Game
	// state up to, not including, the entry of the next piece).
	//
	// This call clears any cycle-specific information, packaging
	// up zero or more "attacks" and adding them to the outgoing
	// attack queue.  The queue itself can be retrieved (and
	// simultaneously cleared), and should be, frequently, as it
	// is the only means by which one attack system (that for game 1)
	// can transfer attacks to another (game 2) which will unleash
	// those attacks on the game it monitors.
	//
	////////////////////////////////////////////////////////////
	
	
	/**
	 * Called by GameModes - tells the AttackSystem that it is
	 * going into a "new game" state.  If we are loading a state,
	 * don't worry, that should overwrite whatever happens here.
	 */
	public void newGame( ) {
		if ( this.behavior_levelUp_garbageRowNumber == AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_NEW_ONLY 
				|| this.behavior_levelUp_garbageRowNumber == AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW ) {
			
			queueLevelUpGarbageRows( true ) ;
		}
		
		if ( this.newGame_blocksForValleys > 0 || this.newGame_blocksForJunctions > 0
				|| this.newGame_blocksForPeaks > 0 || this.newGame_blocksForCorners > 0
				|| this.newGame_blocksForTrolls > 0 ) {
			
			this.directApi_addIncoming_dropBlocks(
					newGame_blocksForValleys,
					newGame_blocksForJunctions,
					newGame_blocksForPeaks,
					newGame_blocksForCorners,
					newGame_blocksForTrolls) ;
		}
	}
	
	
	private void queueLevelUpGarbageRows( boolean newGame ) {
		// Construct a level-up attack descriptor and put in our outgoing queue.
		tempAD.makeEmpty() ;
		// level-up garbage rows don't get their target set.  We don't handle them
		// in multiplayer games.
		
		int numRows = 0 ;
		if ( newGame )
			numRows = ginfo.firstGarbage >= 0 ? ginfo.firstGarbage : default_levelUp_garbageRowNumber ;
		else
			numRows = ginfo.garbage >= 0 ? ginfo.garbage : default_levelUp_garbageRowNumber ;
		
		tempAD.levelUp_numGarbageRows = numRows ;

		if ( this.behavior_levelUp_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_S0_S1 ) {
			int numSS = tempAD.levelUp_numGarbageRows / 2 ;
			int numS0_S1 = tempAD.levelUp_numGarbageRows % 2 ;
			
			tempAD.levelUp_numGarbageRows = numSS ;
			int ps_int = ( B_PRIME*numSS
    			        + ginfo.firstLevel * C_PRIME + ginfo.level * D_PRIME + ginfo.firstGarbage * E_PRIME
    			        + asac.pseudorandom ) % 2 ;
			for ( int i = 0; i < numSS; i++ ) {
				tempAD.levelUp_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][i] = QCombinations.SS ;
				tempAD.levelUp_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][i] = 0 ;
			}
			
			if ( numS0_S1 > 0 ) {
				tempAD.levelUp_numGarbageRows++ ;
				tempAD.levelUp_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][numSS] = ps_int == 0 ? QOrientations.S0 : QCombinations.S1 ;
				tempAD.levelUp_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][numSS] = 0 ;
			}
		} else {
			for ( int i = 0; i < tempAD.levelUp_numGarbageRows; i++ ) {
				int qc = -1 ;
				if ( this.behavior_levelUp_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_SS )
					qc = QCombinations.SS ;
				else if ( this.behavior_levelUp_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_RAINBOW ) 
					qc = -1 ;		// we will use this value upon expanding the row.
				
				tempAD.levelUp_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][i] = qc ;
				tempAD.levelUp_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][i] = 0 ;
			}
		}
		
		if ( tempAD.levelUp_hasGarbageRows() ) {
			// default target: all.
			tempAD.target_code = AttackDescriptor.TARGET_ALL ;
			asac.numOutgoingAttackDescriptors
					= AttackDescriptor.aggregateIntoQueue( asac.outgoingAttackDescriptors, asac.numOutgoingAttackDescriptors, tempAD) ;
		}
	}
	
	
	/**
	 * At the end of a cycle, we dequeue all the attacks generated during
	 * that cycle.  They go into the provided "AttackDescriptor" object.
	 * 
	 * If 'null', then the attacks are dequeued but not stored.
	 */
	public void endCycle( ) {
		if ( !configured )
			throw new IllegalStateException("finalizeConfiguration has not been called on this AttackSystem!") ;
		
		// By the way, if we just ended a cycle, there are no attacks to perform
		// and we should empty out the relevant field.
		asac.attacksToPerformThisCycle.makeEmpty() ;
		
		// We have a record of changes that occurred during this cycle.  We go
		// through the changes, constructing attacks into our existing 'ad' object,
		// and adding them to the asac queue.  
		
		tempAD.makeEmpty() ;
		
		// First, handle garbage rows.  Our behavior differs depending on several
		// factors.  For now, we ignore row density: density is used when the attack
		// is applied.  We have already considered rowComposition, when we collected
		// clear information.  Primarily we are concerned with number, and queue.
		// Queue determines whether we separately enqueue the attack from each cascade,
		// or combine them all into a single event.  "Number" determines whether 
		// we directly copy each row clear, or omit 1 possibly (either per cascade
		// level or once for the entire set).
		if ( this.behavior_clearedAndSent_garbageRowNumber != AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_NONE ) {
			int rowToConsider = 0 ;
			while ( rowToConsider < asac.numClearedRows ) {
				// QUICK FIX: We may have stored "-2" in asac.clearRowQCombination, when
				// the QCombination of the piece itself was not yet known (probably not, but...)
				// Do a quick fix now.
				if ( asac.clearRowQCombinations[rowToConsider] == -2 )
					asac.clearRowQCombinations[rowToConsider] = PieceCatalog.getQCombination( asac.pieceType ) ;
				
				// Question: do we ignore this row?
				// There are a number of reasons we might ignore it.
				boolean firstInCascade = rowToConsider == 0
						|| asac.clearRowCascadeNumber[rowToConsider] != asac.clearRowCascadeNumber[rowToConsider-1]
						|| ( rowToConsider == asac.numClearedRowsBeforePieceLock && this.behavior_clearedAndSent_garbageRowInclude == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO ) ;
				boolean ignoreThisRow = false ;		// by default, include.
				if ( this.behavior_clearedAndSent_garbageRowInclude == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO
						&& rowToConsider < asac.numClearedRowsBeforePieceLock ) {
					ignoreThisRow = true ;
				} 
				else if ( this.behavior_clearedAndSent_garbageRowNumber == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO ) {
					// Ignore this row (and, in fact, all rows) if we have less than 2 rows total.
					// Naturally, it is only worth examining this row in detail if it is the ONLY
					// row.  If not, we have at least 2.  However, if this is the only row, we need
					// to determine whether it "contains" two row clears.  If 1 qPane, it does not.
					// If 2 qPanes, check whether the clear condition cleared both panes.
					if ( asac.numClearedRows - asac.numClearedRowsBeforePieceLock < 2 ) {
						ignoreThisRow = GameModes.numberQPanes(ginfo) == 1 ||
								!asac.clearRowBothPanesCleared[rowToConsider] ;
					}
				}
				
				if ( !ignoreThisRow ) {
					if ( this.behavior_clearedAndSent_garbageRowNumber == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_PIECE_CLEARS_LESS_ONE ) {
						ignoreThisRow = firstInCascade && asac.clearRowCascadeNumber[rowToConsider] == 0 ;
					}
					else if ( this.behavior_clearedAndSent_garbageRowNumber == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_LESS_ONE) {
						ignoreThisRow = firstInCascade ;
					}
					else if ( this.behavior_clearedAndSent_garbageRowNumber == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_PIECE_CLEARS_LESS_ONE_SINGLE_PANE 
									|| this.behavior_clearedAndSent_garbageRowNumber == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_LESS_ONE_SINGLE_PANE ) {
						// This is a special case.  First, if this is not firstInCascade, or we
						// use NUMBER_PIECE_CLEARS_LESS_ONE_SINGLE_PANE
						// and cascadeNumber > 0, then we do not ignore.  Otherwise, determining
						// whether we ignore this row requires counting through ALL the row clears
						// in this cascade number, to see if they are all S0 or S1.
						if ( !firstInCascade
								|| ( this.behavior_clearedAndSent_garbageRowNumber == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_PIECE_CLEARS_LESS_ONE_SINGLE_PANE
										&& asac.clearRowCascadeNumber[rowToConsider] > 0 )
								|| ( asac.clearRowQCombinations[rowToConsider] != QCombinations.S0 && asac.clearRowQCombinations[rowToConsider] != QCombinations.S1 ) ) {
							ignoreThisRow = false ;
						}
						else {
							// Okay.  If this cascade is all S0 or all S1, we do ignore.
							ignoreThisRow = true ;
							int i = rowToConsider ;
							while ( i < asac.numClearedRows
									&& asac.clearRowCascadeNumber[i] == asac.clearRowCascadeNumber[rowToConsider] ) {
								if ( asac.clearRowQCombinations[i] != asac.clearRowQCombinations[rowToConsider] )
									ignoreThisRow = false ;
								i++ ;
							}
						}
					}
				}
				
				if ( !ignoreThisRow ) {
					// We know that this row will be included.
					// Copy the info.
					tempAD.clearedAndSent_garbageRowPieceType = asac.pieceType ;
					tempAD.clearedAndSent_garbageRowPieceColumn = asac.pieceColumn ;
					tempAD.clearedAndSent_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][tempAD.clearedAndSent_numGarbageRows]
					                                                                 = asac.clearRowQCombinations[rowToConsider] ;
					tempAD.clearedAndSent_garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][tempAD.clearedAndSent_numGarbageRows]
					                                                                 = asac.clearRowCascadeNumber[rowToConsider] ;
					tempAD.clearedAndSent_numGarbageRows++ ;
				}
				
				// Finally, increment.
				rowToConsider++ ;
				
				// Is it time to enqueue this one?
				if ( tempAD.clearedAndSent_numGarbageRows > 0 && ( rowToConsider == asac.numClearedRows
						|| ( asac.clearRowCascadeNumber[rowToConsider] != asac.clearRowCascadeNumber[rowToConsider-1] 
						          && this.behavior_clearedAndSent_garbageRowQueue == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_QUEUE_SETS_SEQUENTIAL ) ) ) {
					
					// Important note: certain types of attacks require information about 
					// the piece and clears.  This is the moment when we enqueue the constructed
					// information, so this is our last chance to encode such information.
					setPreferredSentAndClearedGarbageBlocks( tempAD ) ;
					
					// set the target
					tempAD.target_code = clearedAndSent_getNextTargetCode() ;
					
					// Either that was the last clear, or,
					// we have changed cascades AND our queue behavior dictates that it's time to put
					// in some garbage.
					asac.numOutgoingAttackDescriptors
							= AttackDescriptor.aggregateIntoQueue( asac.outgoingAttackDescriptors, asac.numOutgoingAttackDescriptors, tempAD) ;
					// Note: one side affect of "aggregateInto" is to make the provided descriptor, ad,
					// empty.  It can now be safely used to collect the next set of garbage rows.
				}
			}
		}
		
		if ( this.behavior_displaceRow != AttackSystemAtomicConditionsVersioned.DISPLACE_ACCELERATE_NONE ) {
			// fairly easy.
			boolean hasDisplacement = false ;
			if ( this.behavior_displaceRow == AttackSystemAtomicConditionsVersioned.DISPLACE_ACCELERATE_CLEARS_IGNORE_ONE ){
				// if total clears is > 1, set displacement to that number.
				if ( asac.numClearedRows > 1 ) {
					tempAD.displace_accelerateRows = asac.numClearedRows ;
					hasDisplacement = true ;
				}
			} else if ( this.behavior_displaceRow == AttackSystemAtomicConditionsVersioned.DISPLACE_ACCELERATE_HALF_CLEARS_IGNORE_ONE ) {
				// if total clears is > 1, set displacement to half that number.
				if ( asac.numClearedRows > 1 ) {
					tempAD.displace_accelerateRows = ((double)asac.numClearedRows)/2.0 ;
					hasDisplacement = true ;
				}
			}
			
			if ( hasDisplacement ) {
				tempAD.target_code = displaceRow_getNextTargetCode() ;
				
				asac.numOutgoingAttackDescriptors
						= AttackDescriptor.aggregateIntoQueue( asac.outgoingAttackDescriptors, asac.numOutgoingAttackDescriptors, tempAD) ;
			}
		}
		
		//System.out.println("AttackSystem: asac has " + asac.numOutgoingAttackDescriptors + " outgoing attack descriptors") ;
		
		// Clear out the asac cycle-specific information.
		asac.cascadeNumber = 0 ;
		
		// clear row info:
		asac.numClearedRows = 0 ;
		asac.numClearedRowsBeforePieceLock = 0 ;
		
		ArrayOps.setEmpty(asac.pieceLockedBlocks) ;
		ArrayOps.setEmpty(asac.pieceCleared) ;
	}
	
	
	/**
	 * A helper for constructing AttackDescriptors.  We set the preferred blocks
	 * for garbage according to our current behavior and 
	 * @param ad
	 */
	private void setPreferredSentAndClearedGarbageBlocks( AttackDescriptor ad ) {
		if ( this.behavior_clearedAndSent_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_PREFER_PIECE_CLEAR_NEGATIVE
				|| this.behavior_clearedAndSent_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE ) {
			// For each qPane: if piece blocks were cleared in the pane,
			// we copy and mirror only those blocks.  Otherwise, we copy
			// and mirror all piece blocks in the qPane.
			for ( int qp = 0; qp < 2; qp++ ) {
				boolean allBlocks = true ;
				for ( int r = 0; r < asac.pieceCleared.length; r++ ) {
					if ( asac.pieceCleared[r][qp] &&
							asac.pieceLockedBlocks[r][qp] != 0 )
						allBlocks = false ;
				}
				
				// 'allBlocks' indicate whether we include ALL piece blocks,
				// or just those which were cleared.  Copy the appropriate
				// blocks -- inverting them.  We will make symmetric and extend
				// to the end of the array later.
				// NOTE: This copying of the initial set is the only difference
				// between standard and mirrored.  Specifically, standard will 
				// copy from bottom to top, mirrored from top to bottom (garbage
				// rows are created bottom->top, so following that same direction
				// will keep the same order, reversing it will mirror the order).
				int rFirst, rNum, rIncr ;
				if ( this.behavior_clearedAndSent_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_PREFER_PIECE_CLEAR_NEGATIVE ) {
					rFirst = 0 ;
					rNum = asac.pieceLockedBlocks.length ;
					rIncr = 1 ;
				} else {
					rFirst = asac.pieceLockedBlocks.length -1 ;
					rNum = asac.pieceLockedBlocks.length ;
					rIncr = -1 ;
				}
				int numBlocks = 0 ;
				for ( int iter = 0; iter < rNum; iter++ ) {
					int r = rFirst + iter * rIncr ;
					if ( asac.pieceLockedBlocks[r][qp] != 0 &&
							(allBlocks || asac.pieceCleared[r][qp] ) ) {
						ad.clearedAndSent_garbageRowPreferredBlocks[numBlocks][qp] =
							AttackDescriptor.invertEncoding(asac.pieceLockedBlocks[r][qp], asac.C) ;
						numBlocks++ ;
					}
				}
				
				// We have set the first numBlocks.  Now make them symmetrical.
				for ( int offset = 0; offset < numBlocks; offset++ ) {
					ad.clearedAndSent_garbageRowPreferredBlocks[numBlocks+offset][qp] =
						ad.clearedAndSent_garbageRowPreferredBlocks[numBlocks -1 -offset][qp] ;
				}
				
				numBlocks *= 2 ;
				
				// SPECIAL CASE: Sometimes the block is completely empty.  In that case, place
				// zeroes only in the ending column.
				if ( numBlocks == 0 ) {
					int column = asac.pieceColumn ;
					if ( column < 0 || column >= asac.C )
						column = asac.C / 2 ;
					int encodedColumn = AttackDescriptor.invertEncoding(
							AttackDescriptor.encodeRowBlockPresent( column ),
							asac.C ) ;
					for ( int r = 0; r < ad.clearedAndSent_garbageRowPreferredBlocks.length; r++ )
						ad.clearedAndSent_garbageRowPreferredBlocks[r][qp] = encodedColumn ;
				}
				else {
					// now copy our template until we reach the end of the array.
					for ( int r = numBlocks; r < ad.clearedAndSent_garbageRowPreferredBlocks.length; r++ ) {
						ad.clearedAndSent_garbageRowPreferredBlocks[r][qp] =
							ad.clearedAndSent_garbageRowPreferredBlocks[r % numBlocks][qp] ;
					}
				}
			}
		}
		
		// TODO: The above procedure for all other available densities.  Some of these will require
		// additional method parameters.
	}
	
	
	/**
	 * We are about to enter the specified piece.
	 * @param p
	 * @param o
	 */
	public void aboutToEnterPiece( Piece p, Offset o ) {
		if ( !configured )
			throw new IllegalStateException("finalizeConfiguration has not been called on this AttackSystem!") ;
		
		
		asac.pieceType = p.type ;
		asac.pieceColumn = p.centerGravityColumn() + o.x ;
	}
	
	/**
	 * A clear is about to happen that WASN'T the result of a piece locking in
	 * place (direct or indirect).  For example, if garbage rows were added
	 * and some blocks "fell in" and cause a clear, this is the appropriate method
	 * to call.
	 * 
	 * @param blockField
	 * @param chromaticClears
	 * @param monochromaticClears
	 */
	public void aboutToClearWithoutPiece(
			byte [][][] blockField,
			byte [][][] blockFieldAfter,
			byte [][][] blockFieldInverseClear,
			int [] chromaticClears, boolean [] monochromaticClears ) {
		
		if ( !configured )
			throw new IllegalStateException("finalizeConfiguration has not been called on this AttackSystem!") ;
		
		// We call our helper function.  Unlock 'aboutToClear', we do NOT
		// attempt to track piece location or type, nor do we increment the
		// cascade number.
		aboutToClear( blockField, 
				blockFieldAfter,
				blockFieldInverseClear,
				chromaticClears, monochromaticClears ) ;
		
	}
	
	
	/**
	 * Informs the AttackSystem that the specified piece is about to lock at the
	 * specified offset.
	 * 
	 * This is unlikely to trigger any Attack of any kind; however, it might affect
	 * the nature of any pending attacks.  For example, strategic garbage needs to
	 * examine the piece block contents at its final locking position to know which
	 * columns to leave empty in any produced garbage.
	 * 
	 * @param blockField
	 * @param p
	 * @param o
	 */
	public void aboutToLockPiece( byte [][][] blockField, Piece p, Offset o ) {
		if ( !configured )
			throw new IllegalStateException("finalizeConfiguration has not been called on this AttackSystem!") ;
		
		asac.pieceType = p.type ;
		asac.pieceColumn = p.centerGravityColumn() + o.x ;
		
		// copy the necessary piece block rows into ASAC.  We convert to a better
		// form latter; this version includes zeros (0) representing completely
		// empty (in that QP) piece rows.
		if ( p.blocks[0].length > asac.pieceLockedBlocks.length )
			asac.pieceLockedBlocks = new int[p.blocks[0].length][2] ;
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int row = 0; row < p.blocks[0].length; row++ ) {
				asac.pieceLockedBlocks[row][qp] =
					AttackDescriptor.encodeRowBlocksPresent(
							p.blocks, qp, row, o.x) ;
			}
		}
		
		asac.numClearedRowsBeforePieceLock = asac.numClearedRows ;
	}
	
	
	/**
	 * The given piece has been locked in the blockField at the given
	 * offset.  As a result, the provided clear is about to occur.
	 * 
	 * This method should be called for *every* clear that occurs.
	 * In the event of a cascade, simply make this call again using
	 * the same piece/offset and the newest available values for
	 * blockField and the clear rows.
	 * 
	 * As specified by the method name, this should be called *just before*
	 * the actual clear, not after it (so the blockField includes blocks
	 * which are to be cleared).
	 * @throws Exception 
	 */
	public void aboutToClear( Piece p, Offset o,
			byte [][][] blockField, 
			byte [][][] blockFieldAfter,
			byte [][][] blockFieldInverseClear,
			int [] chromaticClears, boolean [] monochromaticClears ) {
		
		if ( !configured )
			throw new IllegalStateException("finalizeConfiguration has not been called on this AttackSystem!") ;
		
		if ( asac.cascadeNumber == 0 && p != null && p.blocks != null ) {
			if ( p.blocks[0].length > asac.pieceCleared.length ) {
				asac.pieceCleared = new boolean[p.blocks[0].length][2] ;
			}
			for ( int row = 0; row < p.blocks[0].length; row++ ) {
				int blockFieldRow = row + o.y ;
				if ( blockFieldRow >= 0 && blockFieldRow < asac.R ) {
					if ( monochromaticClears[blockFieldRow] ) {
						asac.pieceCleared[row][0] = asac.pieceCleared[row][1] = true ;
					} else {
						int qc = chromaticClears[blockFieldRow] ;
						asac.pieceCleared[row][0] = qc == QCombinations.S0 
								|| qc == QCombinations.SL ;
						asac.pieceCleared[row][1] = qc == QCombinations.S1 
								|| qc == QCombinations.SL ;
					}
				}
			}
		} else {
			ArrayOps.setEmpty( asac.pieceCleared ) ;
		}
		
		aboutToClear( blockField, blockFieldAfter, blockFieldInverseClear, chromaticClears, monochromaticClears ) ;
		
		// Increment cascade number.
		asac.cascadeNumber++ ;
	}
	
	
	
	private byte [][] clear_penalty_qos = null ;
	private byte [][] clear_push_qos = null ;
	
	/**
	 * The given piece has been locked in the blockField at the given
	 * offset.  As a result, the provided clear is about to occur.
	 * 
	 * This method should be called for *every* clear that occurs.
	 * In the event of a cascade, simply make this call again using
	 * the same piece/offset and the newest available values for
	 * blockField and the clear rows.
	 * 
	 * As specified by the method name, this should be called *just before*
	 * the actual clear, not after it (so the blockField includes blocks
	 * which are to be cleared).
	 * @throws Exception 
	 */
	private void aboutToClear(
			byte [][][] blockField, 
			byte [][][] blockFieldAfter,
			byte [][][] blockFieldInverseClear,
			int [] chromaticClears, boolean [] monochromaticClears ) {
		
		int previousNumberRowClears = asac.numClearedRows ;
		
		for ( int i = 0; i < QCombinations.NUM; i++ )
			qcCount[i] = 0 ;
		
		// We always keep track of row clears.  However, the QCombination that
		// we store is determined by our behavior.
		for ( int r = 0; r < asac.R; r++ ) {
			// Was this row cleared?
			int clearQC = chromaticClears[r] ;
			boolean monoClear = monochromaticClears[r] ;
			
			if ( clearQC == QCombinations.SL ) 
				clearQC = QCombinations.SS ;
			
			if ( clearQC != QCombinations.NO || monoClear ) {
				// A clear occurred.  Do we store the piece QOrientation,
				// the majority in the row, or the clear type: S0, S1, SS?
				if ( monoClear )
					clearQC = QCombinations.SS ;
				
				// Count up for the majority
				for ( int c = 0; c < asac.C; c++ ) {
					try {
						qcCount[ QCombinations.encode( blockField, r, c ) ]++ ;
					}
					catch( Exception e ) {
						System.err.println("AttackSystem: problem in aboutToClear") ;
						e.printStackTrace() ;
					}
				}
				
				int qcToStore = -1 ;
				
				// We have all three possibilities.  Which do we store?
				// By the way, "majority" is defined by the overall majority in the
				// entire clear, not this row in particular.  We temporarily store "-1"s,
				// although we are counting QC occurrences, and outside this loop
				// we will go back and replace them.
				if ( this.behavior_clearedAndSent_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_PIECE
						|| ( asac.cascadeNumber == 0 && this.behavior_clearedAndSent_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_PIECE_CASCADE_MAJORITY ) ) {
					// Store the piece QOrientation.
					if ( asac.pieceType < 0 )
						qcToStore = -2 ;
					else
						qcToStore = PieceCatalog.getQCombination(asac.pieceType) ;
				}
				else if ( this.behavior_clearedAndSent_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_ROWS ){
					// Otherwise, we either use clearQC or do not have garbage
					// rows at all.  Luckily, we don't care!  Just store clearQC.
					qcToStore = clearQC ;
				}
				
				// Store stuff.
				asac.clearRowQCombinations[ asac.numClearedRows ] = qcToStore ;
				asac.clearRowCascadeNumber[ asac.numClearedRows ] = asac.cascadeNumber ;
				asac.clearRowBothPanesCleared[ asac.numClearedRows ] = monoClear || clearQC == QCombinations.SS ;
				asac.numClearedRows++ ;
				
				// 
			}
		}
		
		// Are we using majority?  If so, we put a -1 in place.  Replace 
		// it with majority.
		int majorityQC = 0, majorityQCCount = 0 ;
		for ( int i = 0; i < QCombinations.NUM; i++ ) {
			if ( qcCount[i] > majorityQCCount ) {
				majorityQC = i ;
				majorityQCCount = qcCount[i] ;
			}
		}
		for ( int i = previousNumberRowClears; i < asac.numClearedRows; i++ ) {
			if ( asac.clearRowQCombinations[i] == -1 )
				asac.clearRowQCombinations[i] = majorityQC ;
		}
		
		
		// Push Blocks
		if ( this.behavior_pushRow == AttackSystemAtomicConditionsVersioned.PUSH_ROWS_PUSH_DOWN_BLOCK_ACTIVATES_OR_PUSH_UP_BLOCK_CLEARS ) {
			if ( clear_push_qos == null ) {
				clear_push_qos = new byte[1][2] ;
				QCombinations.setAs(clear_push_qos[0], QCombinations.PUSH_UP_ACTIVE) ;
			}
			
			tempAD.makeEmpty() ;
			for ( int r = 0; r < asac.R; r++ ) {
				// check for clear penalty/penalties in this row.
				if ( chromaticClears[r] != 0 || monochromaticClears[r] ) {
					for ( int c = 0; c < asac.C; c++ ) {
						int push = 0 ;
						for ( int t = 0; t < clear_push_qos.length; t++ ) {
							boolean match = true ;
							for ( int qp = 0; qp < 2; qp++ ) {
								match = match && ( clear_push_qos[t][qp] == 0 ||
										clear_push_qos[t][qp] == blockFieldInverseClear[qp][r][c] ) ;
							}
							if ( match ) {
								push++ ;
							}
						}
						
						// apply push
						tempAD.push_numRowsOut += push ;
					}
				}
			}
			
			// aggregate into incoming.
			if ( !tempAD.isEmpty() ) {
				tempAD.target_code = AttackDescriptor.TARGET_INCOMING ;
				asac.numIncomingAttackDescriptors = AttackDescriptor.aggregateIntoQueue(
						asac.incomingAttackDescriptors,
						asac.numIncomingAttackDescriptors, tempAD) ;
			}
		}
		
		
		// HEY GUESS WHAT!?!???  PENALTY!
		if ( this.behavior_penalty_garbageRow == AttackSystemAtomicConditionsVersioned.PENALTY_GARBAGE_ROW_PUSH_DOWN_CLEAR ) {
			if ( clear_penalty_qos == null ) {
				clear_penalty_qos = new byte[2][2] ;
				QCombinations.setAs(clear_penalty_qos[0], QCombinations.PUSH_DOWN) ;
				QCombinations.setAs(clear_penalty_qos[1], QCombinations.PUSH_DOWN_ACTIVE) ;
			}
			
			tempAD.makeEmpty() ;
			for ( int r = 0; r < asac.R; r++ ) {
				// check for clear penalty/penalties in this row.
				if ( chromaticClears[r] != 0 || monochromaticClears[r] ) {
					for ( int c = 0; c < asac.C; c++ ) {
						int penalty = 0 ;
						for ( int t = 0; t < clear_penalty_qos.length; t++ ) {
							boolean match = true ;
							for ( int qp = 0; qp < 2; qp++ ) {
								match = match && ( clear_penalty_qos[t][qp] == 0 ||
										clear_penalty_qos[t][qp] == blockFieldInverseClear[qp][r][c] ) ;
							}
							if ( match ) {
								penalty++ ;
							}
						}
						
						// apply penalty.
						while ( penalty > 0 ) {
							tempAD.penalty_garbageRowPreferredBlocks[tempAD.penalty_numGarbageRows][0] =
								tempAD.penalty_garbageRowPreferredBlocks[tempAD.penalty_numGarbageRows][1] =
									AttackDescriptor.invertEncoding( AttackDescriptor.encodeRowBlockPresent(c), asac.C ) ;
							tempAD.penalty_numGarbageRows++ ;
							penalty-- ;
						}
					}
				}
			}
			
			// aggregate into incoming.
			if ( !tempAD.isEmpty() ) {
				// no target for incoming attack descriptors.
				tempAD.target_code = AttackDescriptor.TARGET_INCOMING ;
				asac.numIncomingAttackDescriptors = AttackDescriptor.aggregateIntoQueue(
						asac.incomingAttackDescriptors,
						asac.numIncomingAttackDescriptors, tempAD) ;
			}
		}
		
	}
	
	
	
	private byte [][][] push_trigger_qos = null ;		// [N][from-to][qos]
	public void metamorphosis( byte [][][] blockField, byte [][][] blockFieldBefore ) {
		// A metamorphosis can possibly create an attack: specifically, metamorphosis
		// that activates PUSH_BLOCKs can introduce a push row attack in our incoming queue.
		if ( push_trigger_qos == null ) {
			push_trigger_qos = new byte[1][2][2] ;
			QCombinations.setAs( push_trigger_qos[0][0], QCombinations.PUSH_DOWN ) ;
			QCombinations.setAs( push_trigger_qos[0][1], QCombinations.PUSH_DOWN_ACTIVE ) ;
		}

		if ( this.behavior_pushRow == AttackSystemAtomicConditionsVersioned.PUSH_ROWS_PUSH_DOWN_BLOCK_ACTIVATES
				|| this.behavior_pushRow == AttackSystemAtomicConditionsVersioned.PUSH_ROWS_PUSH_DOWN_BLOCK_ACTIVATES_OR_PUSH_UP_BLOCK_CLEARS ) {
			tempAD.makeEmpty() ;
			// look for push blocks.
			for ( int r = 0; r < asac.R; r++ ) {
				for ( int c = 0; c < asac.C; c++ ) {
					// look for transformation?
					for ( int t = 0; t < push_trigger_qos.length; t++ ) {
						boolean match = true ;
						for ( int qp = 0; qp < 2; qp++ ) {
							match = match && ( push_trigger_qos[t][0][qp] == 0
									|| push_trigger_qos[t][0][qp] == blockFieldBefore[qp][r][c] ) ;
							match = match && ( push_trigger_qos[t][1][qp] == 0
									|| push_trigger_qos[t][1][qp] == blockField[qp][r][c] ) ;
						}
						
						// a match!
						if ( match ) {
							tempAD.push_numRowsOut++ ;
						}
					}
				}
			}
			
			// aggregate to incoming.  As an incoming attack, there is no target.
			if ( !tempAD.isEmpty() ) {
				tempAD.target_code = AttackDescriptor.TARGET_INCOMING ;
				asac.numIncomingAttackDescriptors = AttackDescriptor.aggregateIntoQueue(
						asac.incomingAttackDescriptors,
						asac.numIncomingAttackDescriptors, tempAD) ;
			}
		}
	}
	
	
	/**
	 * We are about to gain a level, or so we're being told.
	 */
	public void aboutToLevelUp( int ups ) {
		
		// Queue garbage if appropriate
		if ( this.behavior_levelUp_garbageRowNumber == AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION 
				|| this.behavior_levelUp_garbageRowNumber == AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW ) {
			
			queueLevelUpGarbageRows( false ) ;
		}
		
		if ( this.behavior_syncLevelUp == AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE ) {
			// note the current level (+1) and difference.
			tempAD.makeEmpty() ;
			tempAD.syncLevelUp_level = ginfo.level + ups ;
			tempAD.syncLevelUp_levelDifference = (ginfo.level + ups) - ginfo.firstLevel ;
			tempAD.target_code = AttackDescriptor.TARGET_ALL_OTHERS ;
			asac.numOutgoingAttackDescriptors
					= AttackDescriptor.aggregateIntoQueue( asac.outgoingAttackDescriptors, asac.numOutgoingAttackDescriptors, tempAD) ;
		}
	}

	
	
	///////////////////////////////////////////////////////
	//
	// COPYING AND EMPTYING THE OUTGOING ATTACK QUEUE
	//
	// As the Game progresses, the AttackSystem will be queueing
	// up outgoing attacks.  You better clear them or this is a memory leak!
	//
	///////////////////////////////////////////////////////
	
	
	public boolean hasOutgoingAttacks() {
		return asac.numOutgoingAttackDescriptors > 0 ;
	}
	
	
	/**
	 * We copy the current outgoing attack queue into the provided space,
	 * which may be unequally allocated.  However, we assume that there is
	 * NO relevant information in the provided queue, as it will be overwritten.
	 * 
	 * @param queue A provided space in which to store AttackDescriptors.
	 * 
	 * @return The number of "valid" entries in the queue at the completion of this method.
	 */
	public int copyAndClearOutgoingAttackQueue( ArrayList<AttackDescriptor> queue ) {
		return copyAndClearOutgoingAttackQueue( queue, 0 ) ;
	}
	
	/**
	 * We copy the current outgoing attack queue into the provided space,
	 * which may be unequally allocated.  'numDescriptors' provides the number
	 * of items already in the queue; we will add the new items after 
	 * the length'd item already there.
	 * 
	 * @param queue A provided space in which to store AttackDescriptors.
	 * @param numDescriptors How many AttackDescriptors are already in the queue.
	 * @return The number of "valid" entries in the queue at the completion of this method.
	 */
	public int copyAndClearOutgoingAttackQueue( ArrayList<AttackDescriptor> queue, int numDescriptors ) {
		// We take our current list of attacks and move it to the provided queue.
		if ( behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) {
			for ( int i = 0; i < asac.numOutgoingAttackDescriptors; i++ ) {
				if ( queue.size() == i + numDescriptors )
					queue.add( new AttackDescriptor( asac.R, asac.C ) ) ;
				
				queue.get(i + numDescriptors).copyValsFrom( asac.outgoingAttackDescriptors.get(i) ) ;
				asac.outgoingAttackDescriptors.get(i).makeEmpty() ;
			}
			
			numDescriptors += asac.numOutgoingAttackDescriptors ;
			asac.numOutgoingAttackDescriptors = 0 ;
		}
		
		// Take our current outgoing attack queue and copy it to our incoming attack queue.
		else if ( behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) {
			for ( int i = 0; i < asac.numOutgoingAttackDescriptors; i++ ) {
				this.enqueueIncomingAttack(asac.outgoingAttackDescriptors.get(i), true) ;
				asac.outgoingAttackDescriptors.get(i).makeEmpty() ;
			}
			
			asac.numOutgoingAttackDescriptors = 0 ;
		}
		
		else if ( behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_NONE ) {
			// Empty out our outgoingAttackDescriptors.
			for ( int i = 0; i < asac.numOutgoingAttackDescriptors; i++ )
				asac.outgoingAttackDescriptors.get(i).makeEmpty() ;
			asac.numOutgoingAttackDescriptors = 0 ;
		}
		
		return numDescriptors ;
		
	}
	
	
	/**
	 * We copy the current outgoing attack queue into the provided space,
	 * which may be unequally allocated.  'numDescriptors' provides the number
	 * of items already in the queue; we will add the new items after 
	 * the length'd item already there.
	 * 
	 * @param queue A provided space in which to store AttackDescriptors.
	 * @param numDescriptors How many AttackDescriptors are already in the queue.
	 * @return The number of "valid" entries in the queue at the completion of this method.
	 */
	public int aggregateAndClearOutgoingAttackQueue( ArrayList<AttackDescriptor> queue, int numDescriptors ) {
		// We take our current list of attacks and move it to the provided queue.
		if ( behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) {
			// Aggregation is easy!
			for ( int i = 0; i < asac.numOutgoingAttackDescriptors; i++ ) {
				AttackDescriptor ad = asac.outgoingAttackDescriptors.get(i) ;
				numDescriptors = AttackDescriptor.aggregateIntoQueue(queue, numDescriptors, ad) ;
			}
			
			asac.numOutgoingAttackDescriptors = 0 ;
		}
		
		// Take our current outgoing attack queue and copy it to our incoming attack queue.
		else if ( behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) {
			for ( int i = 0; i < asac.numOutgoingAttackDescriptors; i++ ) {
				this.enqueueIncomingAttack(asac.outgoingAttackDescriptors.get(i), true) ;
				asac.outgoingAttackDescriptors.get(i).makeEmpty() ;
			}
			
			asac.numOutgoingAttackDescriptors = 0 ;
		}
		
		else if ( behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_NONE ) {
			// Empty out our outgoingAttackDescriptors.
			for ( int i = 0; i < asac.numOutgoingAttackDescriptors; i++ )
				asac.outgoingAttackDescriptors.get(i).makeEmpty() ;
			asac.numOutgoingAttackDescriptors = 0 ;
		}
		
		return numDescriptors ;
		
	}
	
	
	
	///////////////////////////////////////////////////////
	//
	// DIRECT ATTACK API
	//
	// Methods used by SpecialSystem and others to directly
	// create and issue attacks.
	//
	///////////////////////////////////////////////////////
	
	
	public void directApi_addAttack_dropBlocks( int numForValleys, int numForJunctions, int numForPeaks, int numForCorners, int numTroll ) {
		tempAD.makeEmpty() ;
		tempAD.dropBlocks_numInValleys = numForValleys ;
		tempAD.dropBlocks_numOnJunctions = numForJunctions ;
		tempAD.dropBlocks_numOnPeaks = numForPeaks ;
		tempAD.dropBlocks_numOnCorners = numForCorners ;
		tempAD.dropBlocks_numTroll = numTroll ;
		
		tempAD.target_code = dropBlocks_getNextTargetCode() ;
		
		asac.numOutgoingAttackDescriptors = 
				AttackDescriptor.aggregateIntoQueue(
					asac.outgoingAttackDescriptors,
					asac.numOutgoingAttackDescriptors, tempAD) ;
	}
	
	
	public void directApi_addIncoming_dropBlocks( int numForValleys, int numForJunctions, int numForPeaks, int numForCorners, int numTroll ) {
		tempAD.makeEmpty() ;
		tempAD.dropBlocks_numInValleys = numForValleys ;
		tempAD.dropBlocks_numOnJunctions = numForJunctions ;
		tempAD.dropBlocks_numOnPeaks = numForPeaks ;
		tempAD.dropBlocks_numOnCorners = numForCorners ;
		tempAD.dropBlocks_numTroll = numTroll ;
		
		tempAD.target_code = AttackDescriptor.TARGET_INCOMING ;
		
		asac.numIncomingAttackDescriptors =
			AttackDescriptor.aggregateIntoQueue(
					asac.incomingAttackDescriptors,
					asac.numIncomingAttackDescriptors, tempAD) ;
	}
	
	
	///////////////////////////////////////////////////////
	//
	// ENQUEUEING AND PERFORMING INCOMING ATTACKS!
	//
	// Queries and state-operations that allow the attack system
	// to perform attacks within the game.
	//
	// These also enqueue incoming attacks to begin with.
	//
	///////////////////////////////////////////////////////
	
	
	///////////////////////////////////////////////////////////////
	// Enqueueing incoming attacks
	//
	//
	
	/**
	 * Enqueues the provided attack descriptor, to be unleashed on
	 * the associated game.  If 'destructive' is true, then the
	 * contents of 'ad' may be changed in unspecified ways as a result
	 * of this call.  Otherwise, we guarantee that 'ad' will be left
	 * unchanged, but this guarantee will come with a cost of efficiency.
	 * 
	 * @param ad The AttackDescriptor to include
	 * @param destructive Is it okay to alter the contents of 'ad', if doing so
	 * 			makes the operation more efficient?
	 */
	public void enqueueIncomingAttack( AttackDescriptor ad, boolean destructive ) {
		AttackDescriptor adLocal ;
		if ( destructive )
			adLocal = ad ;
		else {
			adLocal = new AttackDescriptor( asac.R, asac.C ) ;
			adLocal.copyValsFrom(ad) ;
		}
		
		asac.numIncomingAttackDescriptors = AttackDescriptor.aggregateIntoQueue(
				asac.incomingAttackDescriptors, asac.numIncomingAttackDescriptors, adLocal ) ;
	}
	
	
	/**
	 * Enqueues the provided attack descriptors, in order, following the
	 * same convention as enqueueIncomingAttack.
	 * 
	 * @param incomingQueue	The incoming attacks
	 * @param queueLength	The number of incoming attacks which should be enqueued
	 * @param destructive	Is it okay to alter the contents of the provided queue, if
	 * 			doing so makes the operation more efficient?
	 */
	public void enqueueIncomingAttacks( ArrayList<AttackDescriptor> incomingQueue, int queueLength, boolean destructive ) {
		for ( int i = 0; i < queueLength; i++ )
			this.enqueueIncomingAttack(incomingQueue.get(i), destructive) ;
	}
	
	
	/**
	 * Overwrites the current "incomingAttackQueue" with the provided queue.  Typically
	 * this method is unnecessary, if "enqueueIncomingAttack(s)" has been dutifully called.
	 * 
	 * @param queue
	 * @param queueLength
	 */
	public void setIncomingAttackQueue( ArrayList<AttackDescriptor> queue, int queueLength ) {
		for ( int i = 0; i < queueLength; i++ ) {
			if ( asac.incomingAttackDescriptors.size() == i )
				asac.incomingAttackDescriptors.add( new AttackDescriptor( asac.R, asac.C ) ) ;
			
			asac.incomingAttackDescriptors.get(i).copyValsFrom( queue.get(i) ) ;
		}
		
		asac.numIncomingAttackDescriptors = queueLength ;
	}
	
	
	/**
	 * Copies the AttackSystem's incoming attack queue - those attacks ready to
	 * unleash on the associated Game object - into the provided array.  Returns
	 * the number of elements in the array.
	 * 
	 * If an attack is 'dequeued' immediately after this call, then dst.get(0)
	 * describes the attack which will be dequeued.
	 * 
	 * @param dst
	 * @return The number of elements in the attack array.
	 */
	public int copyIncomingAttackQueue( ArrayList<AttackDescriptor> dst ) {
		return AttackDescriptor.copyIntoQueue(
				asac.incomingAttackDescriptors, asac.numIncomingAttackDescriptors, dst) ;
	}
	
	
	///////////////////////////////////////////////////////////////
	// Dequeue incoming attacks for this cycle
	//
	//
	
	
	/**
	 * Has incoming attacks which can be dequeued for this cycle.
	 * 
	 * @return Are there incoming attacks to unleash?
	 */
	public boolean canDequeueIncomingAttacks() {
		return asac.numIncomingAttackDescriptors > 0 ;
	}
	
	
	/**
	 * When it is appropriate to begin a cycle in which attacks will be
	 * unleashed by this attack system, call this method.  It will dequeue
	 * the first item in 'incomingAttackDescriptors', preparing to unleash
	 * those attacks on the game.
	 */
	public void dequeueIncomingAttacksThisCycle() {
		asac.attacksToPerformThisCycle.copyValsFrom( asac.incomingAttackDescriptors.get(0) ) ;
		asac.numIncomingAttackDescriptors-- ;
		// shift the queue down
		for ( int i = 0; i < asac.numIncomingAttackDescriptors; i++ )
			asac.incomingAttackDescriptors.get(i).copyValsFrom(
					asac.incomingAttackDescriptors.get(i+1) ) ;
	}
	
	
	
	
	///////////////////////////////////////////////////////////////
	// Drop Blocks
	//
	//
	
	
	/**
	 * Does this attack system have blocks to drop?
	 */
	public boolean hasDropBlocks() {
		return asac.attacksToPerformThisCycle.dropBlocks_hasBlocks() ;
	}
	
	
	public int unleashDropBlocksInValleys() {
		int num = asac.attacksToPerformThisCycle.dropBlocks_numInValleys ;
		asac.attacksToPerformThisCycle.dropBlocks_numInValleys = 0 ;
		return num ;
	}
	
	public int unleashDropBlocksOnJunctions() {
		int num = asac.attacksToPerformThisCycle.dropBlocks_numOnJunctions ;
		asac.attacksToPerformThisCycle.dropBlocks_numOnJunctions = 0 ;
		return num ;
	}
	
	public int unleashDropBlocksOnPeaks() {
		int num = asac.attacksToPerformThisCycle.dropBlocks_numOnPeaks ;
		asac.attacksToPerformThisCycle.dropBlocks_numOnPeaks = 0 ;
		return num ;
	}
	
	public int unleashDropBlocksOnCorners() {
		int num = asac.attacksToPerformThisCycle.dropBlocks_numOnCorners ;
		asac.attacksToPerformThisCycle.dropBlocks_numOnCorners = 0 ;
		return num ;
	}
	
	public int unleashDropBlocksTroll() {
		int num = asac.attacksToPerformThisCycle.dropBlocks_numTroll ;
		asac.attacksToPerformThisCycle.dropBlocks_numTroll = 0 ;
		return num ;
	}
	
	
	
	
	
	
	///////////////////////////////////////////////////////////////
	// Level Up
	//
	//
	
	/**
	 * Does this attack system have a level change to apply?
	 */
	public boolean hasLevelChange() {
		return asac.attacksToPerformThisCycle.syncLevelUp_hasLevelUp() ;
	}
	
	/**
	 * If we call unleashLevelChange at this very moment, how many level
	 * would we gain?
	 * @return
	 */
	public int numLevelChange() {
		AttackDescriptor ad = asac.attacksToPerformThisCycle ;
		
		int gains = 0 ;
		
		if ( ad.syncLevelUp_hasLevelUp() ) {
			if ( behavior_syncLevelUp == AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE ) {
				int max = GameModes.maxLevel(ginfo.mode) ;
				while ( ginfo.level + gains < max && ginfo.level - ginfo.firstLevel + gains < ad.syncLevelUp_levelDifference ) {
					gains++ ;
				}
			}
		}
		
		return gains ;
	}
	
	/**
	 * Unleashes the level change attack, returning the change in current level.
	 * @return
	 */
	public int unleashLevelChange() {
		int gains = numLevelChange() ;
		
		for ( int i = 0; i < gains; i++ ) {
			ginfo.levelUp() ;
		}

		AttackDescriptor ad = asac.attacksToPerformThisCycle ;
		ad.syncLevelUp_level = 0 ;
		ad.syncLevelUp_levelDifference = 0 ;
		
		return gains ;
	}
	
	///////////////////////////////////////////////////////////////
	// Garbage rows
	//
	//
	
	
	/**
	 * Does this attack system have incoming garbage rows among its
	 * incoming attacks?
	 * 
	 * @return Are garbage rows one of the attacks in the current,
	 * "this cycle" attack set, AND, have we not yet applied those
	 * garbage rows?
	 */
	public boolean hasGarbageRows() {
		return asac.attacksToPerformThisCycle.clearedAndSent_hasGarbageRows()
				|| asac.attacksToPerformThisCycle.levelUp_hasGarbageRows()
				|| asac.attacksToPerformThisCycle.penalty_hasGarbageRows() ;
	}
	
	
	/**
	 * Unleashes the garbage rows for the current attack being performed
	 * by inserting them (at the bottom) of the blockField.
	 * 
	 * Returns the number of rows by which the blockField has
	 * been "raised" (not necessarily the same as the number of 
	 * "garbage rows", because adding one S0 and one S1 garbage row
	 * will raise the blockfield by 1).
	 * 
	 * @param blockField
	 * @return
	 */
	public int unleashGarbageRows( byte [][][] blockField ) {
		
		// We unleash 2 types of garbage rows: clearedAndSent and levelUp.
		// Do them both.
		AttackDescriptor ad = asac.attacksToPerformThisCycle ;
		
		int numNewRows = 0 ;
		
		// Cleared-And-Sent
		if ( ad.clearedAndSent_hasGarbageRows() ) {
			numNewRows += unleashGarbageRows(
					blockField,
					behavior_clearedAndSent_garbageRowComposition, behavior_clearedAndSent_garbageRowDensity,
					ad.clearedAndSent_numGarbageRows,
					ad.clearedAndSent_garbageRows,
					ad.clearedAndSent_garbageRowPreferredBlocks,
					ad.clearedAndSent_garbageRowPieceType, ad.clearedAndSent_garbageRowPieceColumn ) ;
			ad.clearedAndSent_numGarbageRows = 0 ;
		}
		
		// Level-Up
		if ( ad.levelUp_hasGarbageRows() ) {
			numNewRows += unleashGarbageRows(
					blockField,
					behavior_levelUp_garbageRowComposition, behavior_levelUp_garbageRowDensity,
					ad.levelUp_numGarbageRows, ad.levelUp_garbageRows, null ) ;
			ad.levelUp_numGarbageRows = 0 ;
		}
		
		// Penalty
		if ( ad.penalty_hasGarbageRows() ) {
			numNewRows += unleashGarbageRows(
					blockField,
					AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_BLAND,
					AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_MANUAL,
					ad.penalty_numGarbageRows, null, ad.penalty_garbageRowPreferredBlocks ) ;
			ad.penalty_numGarbageRows = 0 ;
		}
		
		// Return the number of rows added.
		return numNewRows ;
	}
	
	
	private int unleashGarbageRows(
			byte [][][] blockField,
			int behavior_garbageRowComposition, int behavior_garbageRowDensity,
			int numGarbageRows, int [][] garbageRows, int [][] garbageRowsPreferredBlocks ) {
		
		return unleashGarbageRows(
				blockField,
				behavior_garbageRowComposition,
				behavior_garbageRowDensity,
				numGarbageRows,
				garbageRows,
				garbageRowsPreferredBlocks,
				0, 0 ) ;
	}
	
	
	private int unleashGarbageRows(
			byte [][][] blockField,
			int behavior_garbageRowComposition, int behavior_garbageRowDensity,
			int numGarbageRows, int [][] garbageRows, int [][] garbageRowsPreferredBlocks,
			int pieceType, int col ) {
		
		// We unleash garbage rows in ALMOST naive way.
		// We read through the garbage rows back-to-front, applying
		// first those that fit in both panes (check QCombinations),
		// then those that exist only in pane 0, then those that
		// exist in pane 1.
		//
		// It's worth noting that, for some densities (e.g. SWISS CHEESE)
		// this will change the order that these rows appear.  However,
		// it's equally worth noting that the result is STILL deterministic,
		// based on the garbage rows.
		
		// First thing, though, count up the number of rows in each pane.
		// We will move blockField up by the max of those amounts and return it.
		int numQ0 = 0, numQ1 = 0 ;
		int numBO = 0 ;
		for ( int i = 0; i < numGarbageRows; i++ ) {
			// in almost all cases, the QCOMBINATION garbageRow is a valid QCombination.
			// In a very few cases (including RAINBOW), it is -1, indicating special
			// consideration.
			if ( garbageRows != null ) {
				if ( garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][i] == -1 ) {
					// special consideration
					if ( behavior_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_RAINBOW ) {
						// we assume rainbow pieces are all the same, in terms of
						// which is in which pane.
						QCombinations.setAs( testQExpansion, QCombinations.R0 ) ;
					}
				}
				else
					QCombinations.setAs( testQExpansion, garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][i] ) ;
				if ( testQExpansion[0] != QOrientations.NO )
					numQ0++ ;
				if ( testQExpansion[1] != QOrientations.NO )
					numQ1++ ;
				if ( testQExpansion[0] != QOrientations.NO && testQExpansion[1] != QOrientations.NO )
					numBO++ ;
			} else {
				numQ0++ ;
				numQ1++ ;
				numBO++ ;
			}
		}
		
		int numNewRows = Math.max(numQ0, numQ1) ;
		
		int blockFieldR = blockField[0].length ;
		int blockFieldC = blockField[0][0].length ;
		
		// Now raise blockField elements in the blockField.
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = blockFieldR-1; r >= numNewRows; r-- ) {
				for ( int c = 0; c < blockFieldC; c++ ) {
					blockField[q][r][c] = blockField[q][r-numNewRows][c] ;
				}
			}
			for ( int r = 0; r < numNewRows; r++ ) {
				for ( int c = 0; c < blockFieldC; c++ ) {
					blockField[q][r][c] = QOrientations.NO ;
				}
			}
		}
		
		// As mentioned, we fill from bottom-up, reading backwards
		// through garbageRows.  To simulate the process of writing
		// the "both" rows first, then the Q0 / Q1 on top of it,
		// we offset by a current count of the number we have already
		// written.
		int q0Offset = numBO ;
		int q1Offset = numBO ;
		int boOffset = 0 ;
		
		boolean usedCheese = false ;
		
		int numQ0Rows = 0 ;
		int numQ1Rows = 0 ;
		
		for ( int i = numGarbageRows-1; i >= 0; i-- ) {
			// We might place an empty column in this row, or use swiss cheese.
			// In the former case we need to know the row; in the latter, we
			// need to know where in the "swiss cheese array" we will begin.
			boolean useSwissCheese = false ;
			int emptyColumn = 0 ;
			int swissCheeseIndex = 0 ;
			boolean qPaneMismatched = false ;
			boolean usePreferredGaps = false ;
			
			int randomNumber = col + pieceType * 2
					+ A_PRIME*( garbageRows == null ? 0 : garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][i] )
			        + B_PRIME*i 
			        + ginfo.firstLevel * C_PRIME + ginfo.level * D_PRIME + ginfo.firstGarbage * E_PRIME
			        + asac.pseudorandom ;
			int randomNumber2 = col + pieceType * 2
  			        + A_PRIME*i 
  			        + ginfo.firstLevel * B_PRIME + ginfo.level * C_PRIME + ginfo.firstGarbage * D_PRIME
  			        + E_PRIME*( garbageRows == null ? 0 : garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][i] )
  			        + asac.pseudorandom ;
			
			// leave an empty column according to piece column
			if ( behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_CLEAR_PIECE_COLUMN_GAP
					|| ( garbageRows != null && garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][i] == 0 &&
							( behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_CLEAR_PIECE_CASCADE_RANDOM_COLUMN_GAP
									|| behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_CLEAR_PIECE_CASCADE_SWISS_CHEESE ) ) ) {
				useSwissCheese = false ;
				emptyColumn = col ;
			}
			// leave a "random" column empty
			else if ( behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_RANDOM_COLUMN_GAP
					|| ( garbageRows != null && garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][i] > 0 &&
							behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_CLEAR_PIECE_CASCADE_RANDOM_COLUMN_GAP ) ) {
				useSwissCheese = false ;
				emptyColumn = RANDOM_NUMBERS[randomNumber % RANDOM_NUMBERS.length] % blockFieldC ;
				// A pseudorandom column number, deterministic from the AttackDescriptor content.
			}
			else if ( behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_SWISS_CHEESE
						|| ( garbageRows != null && garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_CASCADE_NUMBER][i] > 0 &&
								behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_CLEAR_PIECE_CASCADE_SWISS_CHEESE )) {
				// We use Swiss cheese.
				useSwissCheese = true ;
				swissCheeseIndex = RANDOM_NUMBERS[randomNumber % RANDOM_NUMBERS.length] % SWISS_CHEESE.length ;   
			}
			else if ( behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_QUANTUM_CHEESE ) {
				useSwissCheese = true ;
				swissCheeseIndex = RANDOM_NUMBERS[randomNumber % RANDOM_NUMBERS.length] % SWISS_CHEESE.length ;
				qPaneMismatched = true ;
			} else if ( behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_PREFER_PIECE_CLEAR_NEGATIVE
					|| behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE
					|| behavior_garbageRowDensity == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_MANUAL ) {
				useSwissCheese = false ;
				qPaneMismatched = true ;
				emptyColumn = -1 ;
				usePreferredGaps = true ;
			}
			
			usedCheese = usedCheese || useSwissCheese ;
			
			// We have the behavior, and a description of the empty columns.  Fill things.
			// Indicate whether we are filling Q0, Q1 with this pass.
			// Use the specified QCombination unless is -1.
			boolean copyQ0, copyQ1 ;
			if ( garbageRows != null ) {
				if ( behavior_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_BLAND ) {
					QCombinations.setAs( testQExpansion, GameModes.numberQPanes(ginfo) == 1 ? QCombinations.RAINBOW_BLAND : QCombinations.SL_INACTIVE ) ;
				}
				else if ( garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][i] < 0 ) {
					if ( behavior_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_RAINBOW ) {
						QCombinations.setAs(testQExpansion, QCombinations.R0) ;
					}
				}
				else {
					QCombinations.setAs(testQExpansion, garbageRows[AttackDescriptor.INDEX_GARBAGE_ROWS_QCOMBINATION][i]) ;
				}
				copyQ0 = testQExpansion[0] != QOrientations.NO ;
				copyQ1 = testQExpansion[1] != QOrientations.NO ;
			} else {
				if ( behavior_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_BLAND ) {
					QCombinations.setAs( testQExpansion, GameModes.numberQPanes(ginfo) == 1 ? QCombinations.RAINBOW_BLAND : QCombinations.SL_INACTIVE ) ;
				}
				else {
					throw new IllegalArgumentException("No garbage rows provided, but composition behavior requires explicit garbage row info.") ;
				}
				copyQ0 = true ;
				copyQ1 = true ;
			}
			
			int row = 0 ;
			if ( copyQ0 && copyQ1 ) {
				row = boOffset ;
				boOffset++ ;
			}
			else if ( copyQ0 ) {
				row = q0Offset ;
				q0Offset++ ;
			}
			else if ( copyQ1 ) {
				row = q1Offset ;
				q1Offset++ ;
			}
			
			// Write it.  Procedure is universal EXCEPT for mismatched.
			if ( !qPaneMismatched ) {
				for ( int c = 0; c < asac.C; c++ ) {
					if ( ( !useSwissCheese && c != emptyColumn )
							|| ( usePreferredGaps )
							|| ( useSwissCheese && SWISS_CHEESE[(swissCheeseIndex + c) % SWISS_CHEESE.length] == 1 ) ) {
						// Fill this column
						
						// If RAINBOW, expand a random R* value.
						if ( behavior_garbageRowComposition == AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_RAINBOW )
							QCombinations.setAs( testQExpansion, QCombinations.R0 + RANDOM_NUMBERS[(randomNumber2+c) % RANDOM_NUMBERS.length] % 7 ) ;
						if ( copyQ0 )
							blockField[0][row][c] = testQExpansion[0] ;
						if ( copyQ1 )
							blockField[1][row][c] = testQExpansion[1] ;
					}
				}
				// if use preferred gaps, fill in the gaps now.
				if ( usePreferredGaps ) {
					if ( testQExpansion[0] != QOrientations.NO ) {
						AttackDescriptor.decodeRowBlocksPresentBySettingZeros(
								blockField, 0, row,
								garbageRowsPreferredBlocks[numQ0Rows][0]) ;
						numQ0Rows++ ;
					}
					if ( testQExpansion[1] != QOrientations.NO ) {
						AttackDescriptor.decodeRowBlocksPresentBySettingZeros(
								blockField, 1, row,
								garbageRowsPreferredBlocks[numQ1Rows][1]) ;
						numQ1Rows++ ;
					}
				}
			}
			else {
				for ( int q = 0; q < 2; q++ ) {
					if ( usePreferredGaps )
						row = q == 0 ? numQ0Rows : numQ1Rows ;
					for ( int c = 0; c < asac.C; c++ ) {
						if ( ( !useSwissCheese && c != emptyColumn )
								|| ( usePreferredGaps )
								|| ( useSwissCheese && SWISS_CHEESE[(swissCheeseIndex + c + q*asac.C) % SWISS_CHEESE.length] == 1 ) ) {
							// Fill this column
							if ( q == 0 && copyQ0 )
								blockField[0][row][c] = testQExpansion[0] ;
							if ( q == 1 && copyQ1 )
								blockField[1][row][c] = testQExpansion[1] ;
						}
					}
					// if use preferred gaps, fill in the gaps now.
					if ( usePreferredGaps ) {
						if ( testQExpansion[0] != QOrientations.NO && q == 0 ) {
							AttackDescriptor.decodeRowBlocksPresentBySettingZeros(
									blockField, 0, row,
									garbageRowsPreferredBlocks[numQ0Rows][0]) ;
							numQ0Rows++ ;
						}
						if ( testQExpansion[1] != QOrientations.NO && q == 1 ) {
							AttackDescriptor.decodeRowBlocksPresentBySettingZeros(
									blockField, 1, row,
									garbageRowsPreferredBlocks[numQ1Rows][1]) ;
							numQ1Rows++ ;
						}
					}
				}
			}
		}
		
		// fill cheese
		if ( usedCheese ) {
			fillCheese( blockField, numGarbageRows, GameModes.numberQPanes(ginfo) == 2 ) ;
		}
		
		// Return the number of garbage rows used.
		return numNewRows ;
	}
	
	
	/**
	 * This is an ugly hack.
	 * @param blockField
	 * @param numRows
	 * @param twoPanes
	 */
	private void fillCheese( byte [][][] blockField, int numRows, boolean twoPanes ) {
		if ( numRows == 0 )
			return ;
		
		tempBoolField = ArrayOps.allocateToMatchDimensions(tempBoolField, blockField) ;
		ArrayOps.setEmpty(tempBoolField) ;
		
		// Sweep across the bottom, marking outward from each block.
		for ( int q = 0; q < 2 ; q++ )
			for ( int c = 0; c < asac.C; c++ )
				markFrom( tempBoolField, blockField, q, 0, c, 0, numRows-1 ) ;
		
		// We have now marked every block which exists and which is connected
		// to the bottom.  Start filling zeros.
		for ( int q = 0; q < (twoPanes ? 2 : 1); q++ ) {
			for ( int r = 0; r < numRows; r++ ) {
				boolean needsConnect = false ;
				for ( int c = 0; c < asac.C; c++ ) {
					if ( !tempBoolField[q][r][c] && blockField[q][r][c] != QOrientations.NO ) {
						needsConnect = true ;
					}
					else if ( blockField[q][r][c] == QOrientations.NO && needsConnect ) {
						// If 'twoPanes', treat them separately.
						if ( twoPanes ) {
							blockField[q][r][c] = blockField[q][r][c-1] ;
						}
						else {
							blockField[0][r][c] = blockField[0][r][c-1] ;
							blockField[1][r][c] = blockField[1][r][c-1] ;
						}
					}
					else if ( blockField[q][r][c] != QOrientations.NO && needsConnect ) {
						needsConnect = false ;
						markFrom( tempBoolField, blockField, q, r, c-1, 0, numRows-1 ) ;
					}
				}
				needsConnect = false ;
				for ( int c = asac.C - 1; c >= 0; c-- ) {
					if ( !tempBoolField[q][r][c] && blockField[q][r][c] != QOrientations.NO ) {
						needsConnect = true ;
					}
					else if ( blockField[q][r][c] == QOrientations.NO && needsConnect ) {
						// If 'twoPanes', treat them separately.
						if ( twoPanes ) {
							blockField[q][r][c] = blockField[q][r][c+1] ;
						}
						else {
							blockField[0][r][c] = blockField[0][r][c+1] ;
							blockField[1][r][c] = blockField[1][r][c+1] ;
						}
					}
					else if ( blockField[q][r][c] != QOrientations.NO && needsConnect ) {
						needsConnect = false ;
						markFrom( tempBoolField, blockField, q, r, c+1, 0, numRows-1 ) ;
					}
				}
			}
		}
		
		// NOTE: the above procedure can, on occassion, completely fill rows.
		// We don't want that!  Here is a TERRIBLE fix.
		for ( int q = 0; q < (twoPanes ? 2 : 1); q++ ) {
			for ( int r = 0; r < numRows; r++ ) {
				boolean hasHole = false ;
				for ( int c = 0; c < asac.C; c++ ) {
					hasHole = hasHole || blockField[q][r][c] == QOrientations.NO ;
				}
				
				if ( !hasHole ) {
					// poke a hole, test the result.
					for ( int c = 0; c < asac.C; c++ ) {
						byte qo = blockField[q][r][c] ;
						
						if ( twoPanes )
							blockField[q][r][c] = QOrientations.NO ;
						else
							blockField[0][r][c] = blockField[1][r][c] = QOrientations.NO ;
						
						if ( allConnectedToBottom( blockField, 0, numRows-1) ) {
							// okey
							hasHole = true ;
							break ;
						}
						
						if ( twoPanes )
							blockField[q][r][c] = qo ;
						else
							blockField[0][r][c] = blockField[1][r][c] = qo ;
					}
					
					if ( !hasHole ) {
						throw new IllegalStateException("Attack system: failed poking holes in swiss cheese with firstLevel:" + ginfo.firstLevel + ", level " + ginfo.level + ", firstGarbage " + ginfo.firstGarbage ) ;
					}
				}
			}
		}
	}
	
	
	private boolean allConnectedToBottom( byte [][][] blockfield, int minRow, int maxRow ) {
		// first mark the bottom row
		tempBoolField = ArrayOps.allocateToMatchDimensions(tempBoolField, blockfield) ;
		ArrayOps.setEmpty(tempBoolField) ;
		for ( int q = 0; q < 2 ; q++ )
			for ( int c = 0; c < asac.C; c++ )
				markFrom( tempBoolField, blockfield, q, 0, c, minRow, maxRow ) ;
		
		// look for unmarked.
		for ( int q = 0; q < 2; q++ )
			for ( int r = minRow; r <= maxRow; r++ )
				for ( int c = 0; c < asac.C; c++ )
					if ( !tempBoolField[q][r][c] && blockfield[q][r][c] != QOrientations.NO )
						return false ;
		return true ;
	}
	
	
	private void markFrom( boolean [][][] mark, byte [][][] blockfield, int startQ, int startR, int startC, int rMin, int rMax ) {
		if ( tempRStack == null ) {
			tempQStack = new IntStack( blockfield[0].length * blockfield[1].length * 2 ) ;
			tempRStack = new IntStack( blockfield[0].length * blockfield[1].length * 2 ) ;
			tempCStack = new IntStack( blockfield[0].length * blockfield[1].length * 2 ) ;
		}
		
		tempQStack.push(startQ) ;
		tempRStack.push(startR) ;
		tempCStack.push(startC) ;
		
		while ( tempQStack.count() > 0 ) {
			int q = tempQStack.pop() ;
			int r = tempRStack.pop() ;
			int c = tempCStack.pop() ;
			
			if ( mark[q][r][c] || blockfield[q][r][c] == QOrientations.NO )
				continue ;
			
			mark[q][r][c] = true ;
			
			byte qo = blockfield[q][r][c] ;
			
			if ( c > 0 && !mark[q][r][c-1] && !qi.separatesFromWhenSide(qo, blockfield[q][r][c-1]) ) {
				tempQStack.push(q) ;
				tempRStack.push(r) ;
				tempCStack.push(c-1) ;
			}
			
			if ( c < blockfield[0][0].length-1 && !mark[q][r][c+1]  && !qi.separatesFromWhenSide(qo, blockfield[q][r][c+1]) ) {
				tempQStack.push(q) ;
				tempRStack.push(r) ;
				tempCStack.push(c+1) ;
			}
			
			if ( r > rMin && !mark[q][r-1][c]  && !qi.separatesFromWhenAbove(qo, blockfield[q][r-1][c]) ) {
				tempQStack.push(q) ;
				tempRStack.push(r-1) ;
				tempCStack.push(c) ;
			}
			
			if ( r < rMax && !mark[q][r+1][c]  && !qi.separatesFromWhenBelow(qo, blockfield[q][r+1][c]) ) {
				tempQStack.push(q) ;
				tempRStack.push(r+1) ;
				tempCStack.push(c) ;
			}
			
			if (  !mark[(q+1)%2][r][c] && !qi.separatesFromWhenQuantum(qo, blockfield[(q+1)%2][r][c]) ) {
				tempQStack.push((q+1)%2) ;
				tempRStack.push(r) ;
				tempCStack.push(c) ;
			}
			
		}
	}
	
	
	/**
	 * Does the AttackSystem have push rows ready to unleash?
	 * @return
	 */
	public boolean hasPushRows() {
		return asac.attacksToPerformThisCycle.push_hasRows() ;
	}
	
	
	public int numPushDown() {
		return asac.attacksToPerformThisCycle.push_numRowsOut ;
	}
	
	public int numPushUp() {
		return asac.attacksToPerformThisCycle.push_numRowsIn ;
	}
	
	
	
	/**
	 * The net number of rows to be pushed down.
	 * @return
	 */
	public int numPushDownNet() {
		int netDown = asac.attacksToPerformThisCycle.push_numRowsOut
				- asac.attacksToPerformThisCycle.push_numRowsIn ;
		return netDown >= 0 ? netDown : 0 ;
	}
	
	/**
	 * The net number of rows to be pushed up.
	 * @return
	 */
	public int numPushUpNet() {
		int netUp = asac.attacksToPerformThisCycle.push_numRowsIn
				- asac.attacksToPerformThisCycle.push_numRowsOut ;
		return netUp >= 0 ? netUp : 0 ;
	}
	
	
	/**
	 * Unleashes our "push rows," which send rows in and out of this
	 * blockfield.  Calculated as:
	 * 
	 * push in <-- all available rows get pushed into place.
	 * push out <-- the current bottom N rows get pushed out to the opponent.
	 * 
	 * @return Whether this call has introduced a new outgoing attack.
	 */
	public boolean unleashPushRows( byte [][][] blockField ) {
		if ( !hasPushRows() )
			return false ;
		
		tempAD.makeEmpty() ;
		// we push rows in-and-out.  The rows pushed out end up in this
		//  attack descriptor.
		tempAD.push_numRowsIn = pushRowsInAndOut(
				blockField,
				asac.attacksToPerformThisCycle.push_numRowsIn,
				asac.attacksToPerformThisCycle.push_rowsIn,
				asac.attacksToPerformThisCycle.push_numRowsOut,
				tempAD.push_rowsIn, 0) ;
		
		asac.attacksToPerformThisCycle.push_numRowsIn = 0 ;
		asac.attacksToPerformThisCycle.push_numRowsOut = 0 ;
		
		if ( tempAD.push_numRowsIn > 0 ) {
			// queue to outgoing
			if ( this.behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) {
				tempAD.target_code = pushRow_getNextTargetCode() ;
				asac.numOutgoingAttackDescriptors = AttackDescriptor.aggregateIntoQueue(
						asac.outgoingAttackDescriptors,
						asac.numOutgoingAttackDescriptors, tempAD) ;
				return true ;
			}
			else if ( this.behavior_attackSystemAttacks == AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) {
				tempAD.target_code = AttackDescriptor.TARGET_INCOMING ;
				asac.numIncomingAttackDescriptors = AttackDescriptor.aggregateIntoQueue(
						asac.incomingAttackDescriptors,
						asac.numIncomingAttackDescriptors, tempAD) ;
			}
		}
		
		return false ;
	}
	
	
	/**
	 * Performs the actual grunt work of pushing rows in and out of the blockfield.
	 * 
	 * The result is equivalent to:
	 * 
	 * 'numIn' rows, represented in [Q][C] format in 'rowsIn', are pushed in from the
	 * bottom of the blockfield, in order -- i.e., rows 0... numIn will appear top... bottom.
	 * 
	 * 'numOut' rows will then be removed one at a time from the bottom and pushed into
	 * 'rowsOut,' which already has 'numRowsOut' entries.
	 * 
	 * Returns the new length of 'rowsOut.'
	 * 
	 * @param blockField
	 * @param numIn
	 * @param rowsIn
	 * @param numOut
	 * @param rowsOut
	 * @param numRowsOut
	 * @return
	 */
	private int pushRowsInAndOut( byte [][][] blockField,
			int numIn, ArrayList<byte [][]> rowsIn,
			int numOut, ArrayList<byte [][]> rowsOut, int numRowsOut ) {
		
		// Our procedure is to push in all in rows, then push out the
		// out rows.  We can simplify this procedure somewhat, resulting
		// in EITHER a push-out or push-in, but not both, if we feed the right
		// number of rows in directly into rows-out without touching the blockfield.
		while( numIn > 0 && numOut > 0 ) {
			// feed a rowIn into rowOut.  (last-in, first-out)
			while( rowsOut.size() <= numRowsOut ) {
				rowsOut.add( new byte[2][asac.C] ) ;
			}
			byte [][] rowIn = rowsIn.get(numIn-1) ;
			byte [][] rowOut = rowsOut.get(numRowsOut) ;
			for ( int qp = 0; qp < 2; qp++ ) {
				for ( int c = 0; c < asac.C; c++ ) {
					rowOut[qp][c] = qi.pushFlip(rowIn[qp][c]) ;
				}
			}
			
			numIn-- ;
			numOut-- ;
			numRowsOut++ ;
		}
		
		// Now at most one of numIn / numOut is positive.
		if ( numIn > 0 ) {
			// push these rows in.  First clear the space by shifting the blockfield up...
			for ( int qp = 0; qp < 2; qp++ ) {
				for ( int r = asac.R - 1; r >= numIn; r-- ) {
					for ( int c = 0; c < asac.C; c++ ) {
						blockField[qp][r][c] = blockField[qp][r-numIn][c] ;
					}
				}
			}
			
			// now place those rows.
			for ( int r = numIn - 1; r >= 0; r-- ) {
				byte [][] row = rowsIn.get(numIn - r - 1) ;
				for ( int qp = 0; qp < 2; qp++ ) {
					for ( int c = 0; c < asac.C; c++ ) {
						blockField[qp][r][c] = row[qp][c] ;
					}
				}
			}
		}
		
		if ( numOut > 0 ) {
			// push that many rows out.  First copy into rowsIn.
			for ( int r = 0; r < numOut; r++ ) {
				if ( rowsOut.size() <= numRowsOut )
					rowsOut.add(new byte[2][asac.C]) ;
				byte [][] row = rowsOut.get(numRowsOut) ;
				for ( int qp = 0; qp < 2; qp++ ) {
					for ( int c = 0; c < asac.C; c++ ) {
						row[qp][c] = qi.pushFlip( blockField[qp][r][c] ) ;
					}
				}
				numRowsOut++ ;
			}
			
			// now shift the blockfield down to cover.
			for ( int qp = 0; qp < 2; qp++ ) {
				for ( int r = 0; r < asac.R - numOut; r++ ) {
					for ( int c = 0; c < asac.C; c++ ) {
						blockField[qp][r][c] = blockField[qp][r + numOut][c] ;
					}
				}
				for ( int r = asac.R - numOut; r < asac.R; r++ ) {
					for ( int c = 0; c < asac.C; c++ ) {
						blockField[qp][r][c] = 0 ;
					}
				}
			}
		}
		
		return numRowsOut ;
	}
	
	
	public boolean hasDisplaceRows() {
		return asac.attacksToPerformThisCycle.displace_hasRows() ;
	}
	
	public double numDisplaceRows() {
		return asac.attacksToPerformThisCycle.displace_accelerateRows ;
	}
	
	
	/**
	 * Performs the displacement acceleration on the provided system.
	 * @param ds
	 */
	public void unleashDisplacementRows( DisplacementSystem ds ) {
		if ( hasDisplaceRows() ) {
			ds.accelerateDisplacement( asac.attacksToPerformThisCycle.displace_accelerateRows ) ;
			asac.attacksToPerformThisCycle.displace_accelerateRows = 0 ;
		}
	}
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ATTACK TARGET HELPERS
	//
	
	private int clearedAndSent_getNextTargetCode() {
		int code = nextTargetCode(
				this.behavior_clearedAndSent_target,
				asac.lastTargetCode_clearedAndSent ) ;
		asac.lastTargetCode_clearedAndSent = code ;
		return code ;
	}
	
	private int pushRow_getNextTargetCode() {
		int code = nextTargetCode(
				this.behavior_pushRow_target,
				asac.lastTargetCode_pushRow ) ;
		asac.lastTargetCode_pushRow = code ;
		return code ;
	}
	
	private int displaceRow_getNextTargetCode() {
		int code = nextTargetCode(
				this.behavior_displaceRow_target,
				asac.lastTargetCode_displaceRow ) ;
		asac.lastTargetCode_displaceRow = code ;
		return code ;
	}
	
	private int dropBlocks_getNextTargetCode() {
		int code = nextTargetCode(
				this.behavior_dropBlocks_attackTarget,
				asac.lastTargetCode_dropBlocks ) ;
		asac.lastTargetCode_dropBlocks = code ;
		return code ;
	}
	
	
	private int nextTargetCode( int behavior, int lastTargetCode ) {
		switch( behavior ) {
		case TARGET_INCOMING:
			return AttackDescriptor.TARGET_INCOMING ;
		case TARGET_CYCLE_NEXT:
			return AttackDescriptor.TARGET_CYCLE_NEXT ;
		case TARGET_CYCLE_PREVIOUS:
			return AttackDescriptor.TARGET_CYCLE_PREVIOUS ;
		case TARGET_ALL:
			return AttackDescriptor.TARGET_ALL ;
		case TARGET_ALL_OTHERS:
			return AttackDescriptor.TARGET_ALL_OTHERS ;
		case TARGET_ALL_DIVIDED:
			return AttackDescriptor.TARGET_ALL_DIVIDED ;
		case TARGET_ALL_OTHERS_DIVIDED:
			return AttackDescriptor.TARGET_ALL_OTHERS_DIVIDED ;

		case TARGET_CYCLE_ALTERNATE:
			// Cycle targets between NEXT and PREVIOUS.  We begin with NEXT.
			if ( lastTargetCode == AttackDescriptor.TARGET_CYCLE_NEXT )
				return AttackDescriptor.TARGET_CYCLE_PREVIOUS ;
			return AttackDescriptor.TARGET_CYCLE_NEXT ;
		}
		
		throw new IllegalArgumentException("With target behavior " + behavior + ", last  target code " + lastTargetCode + " don't know how to target the next attack!") ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	
	
	/**
	 * A call to this method transitions the object from "configuration"
	 * phase to "stateful use" phase.  Although this is not programatically
	 * enforced, classes implementing this interface should refuse
	 * (e.g. by throwing an exception) any calls to "stateful use" methods
	 * before this method is called, and likewise, any calls to 
	 * "configuration" methods afterwards.
	 * 
	 * Calls to set or retrieve object state should be considered
	 * "stateful use" - i.e., those methods should be refused if
	 * calls occur before this method.
	 * 
	 * @throws IllegalStateException If called more than once, or
	 * 				before necessary configuration is complete.
	 */
	public AttackSystem finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		configured = true ;
		return this ;
	}
	
	
	/**
	 * Returns, as a Serializable object, the current "state" of
	 * this object, which is assumed to be in "stateful use" phase.
	 * 
	 * Calling 'setStateAsParcelable' on this object at any future
	 * point, or on another instance of the object which had an identical
	 * configuration phase, should produce an object with identical
	 * behavior and state to the one whose Serializable state was
	 * extracted - no matter what state the object was in before
	 * setState... was called.
	 * 
	 * @return Current state as a Serializable
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public Serializable getStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return asac ;
	}
	
	
	/**
	 * Returns as a Serializable object a clone of the current "state" of this
	 * object.  It is acceptable for 'getStateAsSerializable' to return a clone,
	 * but one should NOT rely on that assumption.  If you intend to make any changes
	 * to the resulting object, or if the callee will have mutators called after
	 * this method, always get a clone rather than getState...().
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	public Serializable getCloneStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return new AttackSystemAtomicConditionsVersioned( asac ) ;
	}
	
	
	/**
	 * Sets the object state according to the Serializable provided,
	 * which can be assumed to have been returned by 'getStateAsSerializable()'
	 * called on an object of the same class which underwent the same
	 * pre-"finalizeConfiguration" config. process.
	 * 
	 * POST-CONDITION: The receiver will have identical state and functionality
	 * to the object upon which "getStateAsParcelable" was called.
	 * 
	 * @param in A Serializable state from an object
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public SerializableState setStateAsSerializable( Serializable in ) throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		if ( in instanceof AttackSystemAtomicConditions )
			asac = new AttackSystemAtomicConditionsVersioned((AttackSystemAtomicConditions)in) ;
		else
			asac = (AttackSystemAtomicConditionsVersioned)in ;
		return this ;
	}
	
	
	/**
	 * Writes the current state, as a Serializable object, to the provided "outStream".
	 * The same assumptions and requirements of getStateAsParcelable are true here
	 * as well.
	 * 
	 * @param outStream	An output stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If writing to the stream fails
	 */
	public void writeStateAsSerializedObject( ObjectOutputStream outStream ) throws IllegalStateException, IOException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		outStream.writeObject(asac) ;
	}
	
	/**
	 * Reads the current state, as a Serializable object, from the provided "inStream".
	 * The same assumptions and requirements of setStateAsParcelable are true here
	 * as well.
	 * 
	 * @param inStream An input stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If reading from the stream fails
	 * @throws ClassNotFoundException	If the stream does not contain the class representing
	 * 			this object's Serializable state.
	 */
	public AttackSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		Object obj = inStream.readObject() ;
		if ( obj instanceof AttackSystemAtomicConditions )
			asac = new AttackSystemAtomicConditionsVersioned((AttackSystemAtomicConditions)obj) ;
		else
			asac = (AttackSystemAtomicConditionsVersioned)obj ;
		return this ;
	}
	
	
	
}
