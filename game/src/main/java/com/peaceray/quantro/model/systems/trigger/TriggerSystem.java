package com.peaceray.quantro.model.systems.trigger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import com.peaceray.quantro.exceptions.QOrientationConflictException;
import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.clear.ClearSystem;
import com.peaceray.quantro.model.systems.collision.CollisionSystem;
import com.peaceray.quantro.model.systems.lock.LockSystem;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;


/**
 * The TriggerSystem.  This system monitors the game progression (technically,
 * it is TOLD when certain things happen - there is no active monitoring) and
 * watches for certain events.  If a particular event or combination of events
 * (conjunction) occurs, the TriggerSystem will call trigObj.pullTrigger( trigNum ),
 * where "trigObj" is some instance of a class implementing Triggerable.
 * Specific values for trigObj and trigNum are set upon construction by a
 * TriggerSystemFactory -- do NOT construct a TriggerSystem, or alter its
 * trigger conditions, once it has been constructed.  This restriction is in
 * place to allow triggerSystems to be saved/loaded without worrying about
 * getting the exact object references later.
 * 
 * *ANY* state information that isn't set by the Factory should be stored within
 * TriggerSystemAtomicConditionsVersioned.
 * 
 * A note on SerializableState: as mentioned above, an instance
 * of TriggerSystemAtomicConditions stores all state information to set
 * by TriggerSystem configuration.
 * 
 * @author Jake
 *
 */
public class TriggerSystem implements SerializableState {
	
	@SuppressWarnings("unused")
	private static final String TAG = "TriggerSystem" ;
	
	// Useful objects
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	private LockSystem ls ;
	private CollisionSystem cs ;
	private ClearSystem cls ;

	// Configuration of this trigger system
	private ArrayList<Integer> conditions ;
	private ArrayList<Triggerable> conditionTriggerObjects ;
	private ArrayList<Integer> conditionTriggerNumbers ;
	private ArrayList<Object []> conditionTriggerParameters ;
	private ArrayList<Boolean> conditionRepeats ;
	
	private ArrayList<int []> conjunctions ;
	private ArrayList<Triggerable> conjunctionTriggerObjects ;
	private ArrayList<Integer> conjunctionTriggerNumbers ;
	private ArrayList<Object []> conjunctionTriggerParameters ;
	
	// State of this trigger system
	private TriggerSystemAtomicConditionsVersioned tsac ;
	private boolean configured = false ;
	
	TriggerSystem(GameInformation ginfo, QInteractions qi) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		conditions = new ArrayList<Integer>() ;
		conditionTriggerObjects = new ArrayList<Triggerable>() ;
		conditionTriggerNumbers = new ArrayList<Integer>() ;
		conditionTriggerParameters = new ArrayList<Object[]>() ;
		conditionRepeats = new ArrayList<Boolean>() ;
		
		conjunctions = new ArrayList<int []>() ;
		conjunctionTriggerObjects = new ArrayList<Triggerable>() ;
		conjunctionTriggerNumbers = new ArrayList<Integer>() ;
		conjunctionTriggerParameters = new ArrayList<Object[]>() ;
		
		tsac = new TriggerSystemAtomicConditionsVersioned() ;
		configured = false ;
	}
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this LockSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public GameInformation getGameInformation() {
		return ginfo ;
	}
	
	/**
	 * getQInteractions: Collisions are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public QInteractions getQInteractions() {
		return qi ;
	}
	
	
	TriggerSystem setClearSystem( ClearSystem cls ) {
		this.cls = cls ;
		return this ;
	}
	
	TriggerSystem setCollisionSystem( CollisionSystem cs ) {
		this.cs = cs ;
		return this ;
	}
	
	TriggerSystem setLockSystem( LockSystem ls ) {
		this.ls = ls ;
		return this ;
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// CONFIGURATION METHODS
	//
	// Although this system does not maintain a state,
	// these methods are considered "configuration" under the specification
	// given by SerializableState.
	//
	////////////////////////////////////////////////////////////////
	
	void clearConditionsAndConjunctions() {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		conditions = new ArrayList<Integer>() ;
		conditionTriggerObjects = new ArrayList<Triggerable>() ;
		conditionTriggerNumbers = new ArrayList<Integer>() ;
		conditionTriggerParameters = new ArrayList<Object[]>() ;
		conditionRepeats = new ArrayList<Boolean>() ;
		
		conjunctions = new ArrayList<int []>() ;
		conjunctionTriggerObjects = new ArrayList<Triggerable>() ;
		conjunctionTriggerNumbers = new ArrayList<Integer>() ;
		conjunctionTriggerParameters = new ArrayList<Object[]>() ;
		
		tsac.conditionCurrentlyTrue = new boolean[TriggerSystemAtomicConditionsVersioned.NUM_TRIGGER_CONDITIONS] ;
		tsac.justHappened = new boolean[TriggerSystemAtomicConditionsVersioned.NUM_TRIGGER_CONDITIONS] ;
		tsac.conditionHasOccurred = new ArrayList<Boolean>() ;
		tsac.conjunctionHasOccurred = new ArrayList<Boolean>() ;
	}
	
	
	
	/**
	 * Sets the specified condition as the sole condition required
	 * for the provided trigger function.  Upon the occurrence of
	 * the trigger condition, this TriggerSystem will call
	 * trigObj.pullTrigger( trigNum ).
	 * @param trigCondition One of TRIGGER_CONDITION_*
	 * @param trigObj A triggerable object
	 * @param trigNum The trigger number to pull on trigNum
	 * @param repeats Should this trigger be pulled every time the condition occurs?
	 * 			alternative is once per Piece that is prepared
	 */
	void setTriggerCondition( int trigCondition, Triggerable trigObj, int trigNum, boolean repeats ) {
		setTriggerCondition( trigCondition, trigObj, trigNum, null, repeats ) ;
	}
	
	/**
	 * Sets the specified condition as the sole condition required
	 * for the provided trigger function.  Upon the occurrence of
	 * the trigger condition, this TriggerSystem will call
	 * trigObj.pullTrigger( trigNum ).
	 * @param trigCondition One of TRIGGER_CONDITION_*
	 * @param trigObj A triggerable object
	 * @param trigNum The trigger number to pull on trigNum
	 * @param repeats Should this trigger be pulled every time the condition occurs?
	 * 			alternative is once per Piece that is prepared
	 */
	void setTriggerCondition( int trigCondition, Triggerable trigObj, int trigNum, Object [] params, boolean repeats ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		// If the trigger condition does not occur in the existing list,
		// add it.  If 'trigObj' is null, remove all instances of the condition.
		if ( trigObj == null ) {
			for ( int t = 0; t < conditions.size(); t++ ) {
				if ( trigCondition == conditions.get(t).intValue() ) {
					// Replace.
					if ( trigObj == null ) {
						conditions.remove(t) ;
						conditionTriggerObjects.remove(t) ;
						conditionTriggerNumbers.remove(t) ;
						conditionTriggerParameters.remove(t) ;
						conditionRepeats.remove(t) ;
						tsac.conditionHasOccurred.remove(t) ;
					}
					
					t-- ;
				}
			}
		}
		else {
			conditions.add(new Integer(trigCondition)) ;
			conditionTriggerObjects.add(trigObj) ;
			conditionTriggerNumbers.add(new Integer(trigNum)) ;
			conditionTriggerParameters.add(copy(params)) ;
			conditionRepeats.add(Boolean.valueOf(repeats)) ;
			tsac.conditionHasOccurred.add(Boolean.FALSE) ;
		}
	}
	
	/**
	 * Sets the specified conjunction as condition required
	 * for the provided trigger function.  Upon the occurrence of
	 * the trigger conditions, this TriggerSystem will call
	 * trigObj.pullTrigger( trigNum ).
	 * @param conditions An array of TRIGGER_CONDITION_* s.
	 * @param trigObj A triggerable object
	 * @param trigNum The trigger number to pull on trigNum
	 */
	void setTriggerConjunction( int [] conditions, Triggerable trigObj, int trigNum ) {
		setTriggerConjunction( conditions, trigObj, trigNum, null ) ;
	}
	
	
	/**
	 * Sets the specified conjunction as condition required
	 * for the provided trigger function.  Upon the occurrence of
	 * the trigger conditions, this TriggerSystem will call
	 * trigObj.pullTrigger( trigNum ).
	 * @param conditions An array of TRIGGER_CONDITION_* s.
	 * @param trigObj A triggerable object
	 * @param trigNum The trigger number to pull on trigNum
	 */
	void setTriggerConjunction( int [] conditions, Triggerable trigObj, int trigNum, Object [] params ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		
		if ( trigObj == null ) {
			for ( int t = 0; t < conjunctions.size(); t++ ) {
				if ( valuesMatch( conditions, conjunctions.get(t) ) ) {
					conjunctions.remove(t) ;
					conjunctionTriggerObjects.remove(t) ;
					conjunctionTriggerNumbers.remove(t) ;
					conjunctionTriggerParameters.remove(t) ;
					tsac.conjunctionHasOccurred.remove(t) ;
					t-- ;
				}
			}
		}
		else {
			int [] condCopy = new int[conditions.length] ;
			for ( int i = 0; i < conditions.length; i++ )
				condCopy[i] = conditions[i] ;
			conjunctions.add(condCopy) ;
			conjunctionTriggerObjects.add(trigObj) ;
			conjunctionTriggerNumbers.add(new Integer(trigNum)) ;
			conjunctionTriggerParameters.add(copy(params)) ;
			tsac.conjunctionHasOccurred.add(Boolean.FALSE) ;
		}
	}
	
	
	private void noteTriggersThatJustHappened() {
		// For every condition, check whether it just happened
		// (and if it hasOccurred, whether it repeats).  For each
		// such trigger, call the trigger method.
		
		// For every conjunction, check whether it has happened;
		// if not, some value is represented in justHappened, and
		// EVERY value is represented in justHappened UNION conditionCurrentlyTrue,
		// pull the trigger.
		
		
		// First note that everything that just happened is currently true.
		for ( int i = 0; i < TriggerSystemAtomicConditionsVersioned.NUM_TRIGGER_CONDITIONS; i++ ) {
			tsac.conditionCurrentlyTrue[i] = tsac.conditionCurrentlyTrue[i] || tsac.justHappened[i] ;
		}
		
		// Check conditions
		for ( int i = 0; i < conditions.size(); i++ ) {
			int condNum = conditions.get(i).intValue() ;
			if ( tsac.justHappened[condNum] &&
					( !tsac.conditionHasOccurred.get(i).booleanValue() ||
							conditionRepeats.get(i).booleanValue() ) ) {
				Object [] params = conditionTriggerParameters.get(i) ;
				if ( params == null )
					conditionTriggerObjects.get(i).pullTrigger(
							conditionTriggerNumbers.get(i).intValue()) ;
				else
					conditionTriggerObjects.get(i).pullTrigger(
							conditionTriggerNumbers.get(i).intValue(), params) ;
				tsac.conditionHasOccurred.set(i, Boolean.TRUE) ;
			}
		}
		
		// Check conjunctions
		for ( int i = 0; i < conjunctions.size(); i++ ) {
			int [] conds = conjunctions.get(i) ;
			if ( !tsac.conjunctionHasOccurred.get(i).booleanValue() ) {
				boolean somethingJust = false ;
				boolean allHave = true ;
				for ( int j = 0; j < conds.length; j++ ) {
					int conditionNum = conds[j] ;
					if ( tsac.justHappened[conditionNum] )
						somethingJust = true ;
					else if ( !tsac.conditionCurrentlyTrue[conditionNum] )
						allHave = false ;
				}
				
				if ( somethingJust && allHave ) {
					// Pull it!
					Object [] params = conjunctionTriggerParameters.get(i) ;
					if ( params == null )
						conjunctionTriggerObjects.get(i).pullTrigger(
								conjunctionTriggerNumbers.get(i).intValue()) ;
					else
						conjunctionTriggerObjects.get(i).pullTrigger(
								conjunctionTriggerNumbers.get(i).intValue(), params) ;
					tsac.conjunctionHasOccurred.set(i, Boolean.TRUE) ;
				}
			}
		}
		
		// Zero-out 'justHappened'
		for ( int i = 0; i < TriggerSystemAtomicConditionsVersioned.NUM_TRIGGER_CONDITIONS; i++ ) {
			tsac.justHappened[i] = false ;
		}
	}
	
	
	/**
	 * Resets all triggers by setting to 'false' conditionCurrentlyTrue
	 * and the condition-specific HasOccurred arrays
	 */
	@SuppressWarnings("unused")
	private void resetAllTriggers() {
		for ( int i = 0; i < TriggerSystemAtomicConditionsVersioned.NUM_TRIGGER_CONDITIONS; i++ )
			tsac.conditionCurrentlyTrue[i] = false ;
		for ( int i = 0; i < tsac.conditionHasOccurred.size(); i++ )
			tsac.conditionHasOccurred.set(i, Boolean.FALSE) ;
		for ( int i = 0; i < tsac.conjunctionHasOccurred.size(); i++ )
			tsac.conjunctionHasOccurred.set(i, Boolean.FALSE) ;
	}
	
	
	
	/**
	 * Compares two arrays of trigger conjunctions; returns 'true' iff
	 * every condition in cond1 occurs in cond2 and vice-versa (i.e.,
	 * if the two are equivalent).
	 * @param cond1
	 * @param cond2
	 * @return Do cond1 and cond2 have matching elements?
	 */
	private boolean valuesMatch( int [] cond1, int [] cond2 ) {
		for ( int i = 0; i < cond1.length; i++ ) {
			boolean found = false ;
			for ( int j = 0; j < cond2.length; j++ ) {
				if ( cond1[i] == cond2[j] ) {
					found = true ;
					break ;
				}
			}
			
			if ( !found )
				return false ;
		}
		
		for ( int j = 0; j < cond2.length; j++ ) {
			boolean found = false ;
			for ( int i = 0; i < cond1.length; i++ ) {
				if ( cond2[j] == cond1[i] ) {
					found = true ;
					break ;
				}
			}
			
			if ( !found )
				return false ;
		}
		
		return true ;
	}
	
	
	
	/**
	 * didMove: Tells the timing system that the piece moved.
	 * moveChange is the "delta" for the movement.
	 * 
	 * @param didMove The delta for the movement
	 */
	public void didMove( Offset moveChange ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// Movement doesn't do a trigger, but it does mean we have
		// to note that it wasn't a turn.
		tsac.mostRecentWasTurn = false ;
	}
	
	/**
	 * didTurn: Tells the trigger system that the piece provided
	 * just turned.  The Piece object contains rotation info
	 * 
	 * @param piece The piece which turned.
	 */
	public void didTurn( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		tsac.mostRecentWasTurn = true ;
	}
	
	/**
	 * didFlip: Tells the trigger system that the piece provided
	 * just flipped.
	 * 
	 * @param piece The piece which turned.
	 */
	public void didFlip( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		tsac.mostRecentWasTurn = false ;
	}
	
	/**
	 * didKick: Tells the timing system that the piece provided
	 * was just kicked to a new location.  Everything is provided.
	 * 
	 * @param piece	The piece that was kicked
	 * @param offset The new offset for the piece
	 */
	public void didKick( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		if ( tsac.mostRecentWasTurn )
			tsac.mostRecentTurnKicked = true ;
	}

	/**
	 * didFall: Tells the timing system that the piece provided
	 * has fallen to the new offset.
	 * @param newOffset
	 */
	public void didFall( Offset newOffset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		tsac.mostRecentWasTurn = false ;
	}
	
	/**
	 * didDrop: Tells the timing system that the user dropped the
	 * piece to the bottom (or as far as it would go)
	 * @param piece The piece the user dropped
	 * @param offset The new offset for the piece
	 */
	public void didDrop( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		tsac.mostRecentWasTurn = false ;
	}
	
	/**
	 * Inserted a "reserve" piece into the queue, but it is not
	 * currently falling (it was inserted as a piece-to-come-later).
	 * For reserves that immediately appear, replacing one currently
	 * falling, use "didSwap".
	 * @param newPiece
	 * @param offset
	 */
	public void didInsert( Piece newPiece ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_RESERVE_USED] = true ;
		noteTriggersThatJustHappened() ;
	}
	
	/**
	 * Took the currently falling piece and moved it to the reserve.  The
	 * piece put in reserve is provided.
	 * @param piece
	 */
	public void didPutInReserve( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// Undo any "piece" specific triggers.
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FLASH] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_PUSH_DOWN] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_NOT_T] = false ;
	}
	
	public void didPutPieceBack( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// Undo any "piece" specific triggers.
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FLASH] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_PUSH_DOWN] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_NOT_T] = false ;
	}
	
	/**
	 * Used a reserve piece, swapping with the piece currently falling.
	 * @param newPiece
	 */
	public void didSwap( Piece newPiece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// Undo any "piece" specific triggers.
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FLASH] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_PUSH_DOWN] = false ;
		tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_NOT_T] = false ;
		
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_RESERVE_USED] = true ;
		noteTriggersThatJustHappened() ;
		
		this.didEnter(newPiece, offset) ;
	}
	
	/**
	 * didLock: Tells the timing system that the piece locked in place.
	 * @param blockField
	 * @param piece
	 * @param offset
	 */
	public void willLock( byte [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// A lot can happen here.
		// First, note whether this was a T-Spin.
		
		int R = blockField[0].length ;
		int C = blockField[0][0].length ;
		
		//Log.d(TAG, " in didLock ") ;
		
		// Note: the piece has locked.
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED] = true ;
		
		if ( PieceCatalog.isTetromino(piece)
				&& PieceCatalog.getTetrominoCategory(piece) == PieceCatalog.TETRO_CAT_T
				&& tsac.mostRecentWasTurn ) {
			// Spin?  We need to check that at least 3 of four corners are occupied.
			// But how?  Use a special block!  Recall that the "center" of the T block
			// is at the index (2,1) into its blocks.
			// TODO: If we change the T-block representation, change this too!
			int numCorners = 0 ;
			boolean againstSide = false ;
			byte qo0 = piece.blocks[0][ piece.boundsLL.getRow() + 2][ piece.boundsLL.getCol() + 1] ;
			byte qo1 = piece.blocks[1][ piece.boundsLL.getRow() + 2][ piece.boundsLL.getCol() + 1] ;
			
			int fr = offset.getRow() + 2 ;
			int fc = offset.getCol() + 1 ;
			
			// Try down-left, down-right, etc.
			for ( int rowOffset = -1; rowOffset <= 1; rowOffset += 2 ) {
				for ( int colOffset = -1; colOffset <= 1; colOffset += 2 ) {
					int frOff = fr + rowOffset ;
					int fcOff = fc + colOffset ;
					
					if ( frOff < 0 || fcOff < 0 || fcOff >= C ) {
						againstSide = true ;
						numCorners++ ;
					}
					else if ( frOff < R ) {
						if ( qi.collides( blockField[0][frOff][fcOff], qo0 )
								|| qi.collides( blockField[1][frOff][fcOff], qo1 ) ) {
							numCorners++ ;
						}
					}
				}
			}
			
			if ( numCorners >= 3 ) {
				tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_T_SPIN] = true ;
				
				// No wall?
				if ( !againstSide )
					tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_T_SPIN_NO_WALL] = true ;
				if ( !tsac.mostRecentTurnKicked ) {
					tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_T_SPIN_NO_KICK] = true ;
				}
			}
		}
		
		// We've handled t-spins.  Check immobility and snugness.
		// A piece is IMMOBILE if it cannot move in any direction.
		boolean immobile = true ;
		for ( int rowOffset = -1; rowOffset <= 1; rowOffset+= 2 ) {
			tsac.myOffset.setRow( offset.getRow() + rowOffset ) ;
			tsac.myOffset.setCol( offset.getCol() ) ;
			
			if ( !cs.collides(blockField, piece, tsac.myOffset) )
				immobile = false ;
		}
		for ( int colOffset = -1; colOffset <= 1; colOffset+= 2 ) {
			tsac.myOffset.setRow( offset.getRow() ) ;
			tsac.myOffset.setCol( offset.getCol() + colOffset ) ;
			
			if ( !cs.collides(blockField, piece, tsac.myOffset) )
				immobile = false ;
		}
		
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_IMMOBILE] = immobile ;
		
		// A piece is SNUG if, when locked, there are no blocks in
		// the piece s.t. a QOrientations.NO is q-adjacent to it.
		// In other words, no red, no blue - only purple, white, etc.
		// TODO: I bet this can be done more efficiently...
		boolean snug = true ;
		for ( int r = 0; r < piece.boundsUR.getRow() - piece.boundsLL.getRow(); r++ ) {
			int pr = r + piece.boundsLL.getRow() ;
			int fr = r + offset.getRow() ;
			for ( int c = 0; c < piece.boundsUR.getCol() - piece.boundsLL.getCol(); c++ ) {
				int pc = c + piece.boundsLL.getCol() ;
				int fc = c + offset.getCol() ;
				
				// Assumption: if it locks, that means no blocks are outside
				// the bounds of the game (except for "above").
				for ( int q = 0; q < 2; q++ ) {
					int notQ = (q+1) % 2 ;
					if ( piece.blocks[q][pr][pc] == QOrientations.NO && piece.blocks[notQ][pr][pc] != QOrientations.NO ) {
						// The piece has block content opposite this position, but not here.
						// Check here: if the field does not have block content, the piece is not snug.
						try {
							if ( fr < R && blockField[q][fr][fc] == QOrientations.NO ) {
								snug = false ;
							}
						} catch ( ArrayIndexOutOfBoundsException e ) {
							e.printStackTrace() ;
							System.err.println("fr:" + fr + " fc:" + fc + " pr:" + pr + " pc:" + pc) ;
							System.err.println("R:" + R + " C:" + C) ;
							System.err.println("offset:" + offset.toString()) ;
							System.err.println("piece:" + piece.toString()) ;
							System.err.println("blockField:" + Game.arrayAsString(blockField)) ;
							throw e ;
						}
					}
				}
			}
		}
		
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_SNUG] = snug ;
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_UNSNUG] = !snug ;
		
		// For our later purposes, note where the piece ended up.
		tsac.pieceInField = ArrayOps.allocateToMatchDimensions(tsac.pieceInField, blockField) ;
		ArrayOps.setEmpty(tsac.pieceInField) ;
		try {
			ls.lock(tsac.pieceInField,piece,offset) ;
		} catch (QOrientationConflictException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		noteTriggersThatJustHappened() ;
	}
	
	
	/**
	 * willLockComponents: Tells the trigger system is about to lock these
	 * components in place.  These are the components of the piece with which
	 * we call 'willLock.'
	 * @param blockField
	 * @param piece
	 * @param offset
	 */
	public void willLockComponents( byte [][][] blockField, ArrayList<Piece> pieces, ArrayList<Offset> offsets, int numComponents ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// STUB
	}
	
	
	/**
	 * didLockComponents: Tells the trigger system that we just finished locking
	 * these components in place.  These are the components of the piece which we
	 * locked in place.
	 * @param blockField
	 * @param pieces
	 * @param offsets
	 * @param numComponents
	 */
	public void didLockComponents( byte [][][] blockField, ArrayList<Piece> pieces, ArrayList<Offset> offsets, int numComponents ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_COMPONENTS_LOCKED_INTO_BLOCKFIELD] = true ;
		
		noteTriggersThatJustHappened() ;
	}
	
	
	/**
	 * didClear: Tells the timing system that a clear occurred.
	 * @param rowArray	The rows that were cleared
	 */
	public void didClear( byte [][][] preBlockField, byte [][][] blockField, int [] rowArray, boolean [] monochromeRowArray ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// Look through the row array and monochromeArray; mark stuff!
		
		// Following standard practice for tree-operations and such,
		// we iterate once for each trigger to detect.
		int R = rowArray.length ;
		
		boolean s0 ;
		boolean s1 ;
		boolean ss ;
		boolean emptyAfter ;
		boolean hurdle ;
		boolean stPre ;
		boolean stPost ;
		
		// Check S0 and S1, separate clears.
		s0 = s1 = false ;
		for ( int i = 0; i < R; i++ ) {
			s0 = s0 || rowArray[i] == QOrientations.S0 ;
			s1 = s1 || rowArray[i] == QOrientations.S1 ;
		}
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_S0_AND_S1_PIECE] = !tsac.clearedOnce && s0 && s1 ;
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_S0_AND_S1_CASCADE] = tsac.clearedOnce && s0 && s1 ;
		if ( tsac.clearUnion != QCombinations.SL ) {
			if ( s0 && tsac.clearUnion != QCombinations.S0 )
				tsac.clearUnion = tsac.clearUnion == QCombinations.NO ? QCombinations.S0 : QCombinations.SL ;
			else if ( s1 && tsac.clearUnion != QCombinations.S1 )
				tsac.clearUnion = tsac.clearUnion == QCombinations.NO ? QCombinations.S1 : QCombinations.SL ;
			if ( tsac.clearUnion == QCombinations.SL )
				tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_S0_AND_S1_UNION] = true ;
		}
		
		// Check for an SS hurdle
		ss = emptyAfter = hurdle = false ;
		for ( int i = 0; i < R; i++ ) {
			ss = ss || rowArray[i] == QOrientations.SL || monochromeRowArray[i] ;
			emptyAfter = emptyAfter || (ss && rowArray[i] != QOrientations.SL) ;
			hurdle = hurdle || (emptyAfter && rowArray[i] == QOrientations.SL) ;
		}
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_SS_HURDLE_PIECE] = !tsac.clearedOnce && hurdle ;
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_SS_HURDLE_CASCADE] = tsac.clearedOnce && hurdle ;
		
		// Check for a non-SS hurdle.
		ss = emptyAfter = hurdle = false ;
		for ( int i = 0; i < R; i++ ) {
			ss = ss || rowArray[i] != QOrientations.NO || monochromeRowArray[i] ;
			emptyAfter = emptyAfter || (ss && rowArray[i] == QOrientations.NO) ;
			hurdle = hurdle || (emptyAfter && rowArray[i] != QOrientations.NO) ;
		}
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_HURDLE_PIECE] = !tsac.clearedOnce && hurdle ;
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_HURDLE_CASCADE] = tsac.clearedOnce && hurdle ;
		
		// Check for SS
		ss = false ;
		for ( int i = 0; i < R; i++ ) {
			ss = ss || rowArray[i] == QOrientations.SL || monochromeRowArray[i]  ;
		}
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_SS_PIECE] = !tsac.clearedOnce && ss ;
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_SS_CASCADE] = tsac.clearedOnce && ss ;
		if ( tsac.clearUnion != QCombinations.SL && ss ) {
			tsac.clearUnion = QCombinations.SL ;
			tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_S0_AND_S1_UNION] = true ;
		}
		
		// Check for any clear at all
		ss = false ;
		for ( int i = 0; i < R; i++ ) {
			ss = ss || rowArray[i] != QOrientations.NO || monochromeRowArray[i] ;
		}
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE] = !tsac.clearedOnce && ss ;
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_CASCADE] = tsac.clearedOnce && ss ;
		
		// Check for the removal of a ST block.
		stPre = stPost = false ;
		for ( int r = 0; r < R; r++ ) {
			for ( int c = 0; c < blockField[0][0].length; c++ ) {
				stPre = stPre || preBlockField[0][r][c] == QOrientations.ST ;
				stPost = stPost || blockField[0][r][c] == QOrientations.ST ;
			}
		}
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_ST_REMOVED] = stPre && !stPost ;
		
		// Lastly, we might have completely cleared the last piece away.
		// TODO: WARNING: this is a very awkward heuristic.  We previously
		// noted the occupied rows by the piece when it was locked; check
		// whether all such rows are effectively cleared.  Note that
		// this doesn't take into effect what happens when a tetracube
		// unlocks from itself, it can't account for cascades, etc.
		if ( !tsac.clearedOnce ) {
			cls.clear(tsac.pieceInField, rowArray) ;
			cls.clearMonochrome(tsac.pieceInField, monochromeRowArray) ;
			int C = tsac.pieceInField[0][0].length ;
			boolean empty = true ;
			
			int ROWS = Math.min(R, tsac.pieceInField[0].length) ;
			int COLS = Math.min(C, tsac.pieceInField[0][0].length) ;
			
			for ( int q = 0; q < 2; q++ ) {
				for ( int r = 0; r < ROWS; r++ ) {
					for ( int c = 0; c < COLS; c++ ) {
						if ( tsac.pieceInField[q][r][c] != QOrientations.NO )
							empty = false ;
					}
				}
			}
			
			//Log.d(TAG, " in didClear, piece gone is " + empty) ;
			//Log.d(TAG, " as an aside, sticky piece is " + tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_TETROMINO]) ;
			tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE_GONE_IN_ONE] = empty ;
			//System.out.println("TriggerSystem. didClear, EMPTY = " + empty) ;
			//System.out.println("by the way, STICKY currently true is " + tsac.conditionCurrentlyTrue[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_TETROMINO]) ;
		}
		
		// We just cleared; note it.
		tsac.clearedOnce = true ;
		
		// Triggers that just happened?
		noteTriggersThatJustHappened() ;
	}
	
	
	/**
	 * didMetamorphosis: we just successfully metamorphosized one block into a different type.
	 * @param preBlockfield
	 * @param postBlockfield
	 */
	public void didMetamorphis( byte [][][] preBlockfield, byte [][][] postBlockfield ) {
		tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_METAMORPHOSIS] = true ;
		
		noteTriggersThatJustHappened() ;
	}
	
	
	/**
	 * didPrepare: a new piece (provided) is preparing to enter the game.
	 * @param piece The piece that entered
	 * @param offset The offset at which it is prepared
	 */
	public void didPrepare( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		// By preparing a piece, we are resetting all previous triggers.
		// resetAllTriggers() ;
		tsac.newCycle() ;
	}
	
	/**
	 * didEnter: a new piece (provided) entered the game.
	 * @param piece The piece that entered
	 * @param offset The offset at which it entered
	 */
	public void didEnter( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must first call finalizeConfiguration()!") ;
		
		boolean triggerOfNote = false ;
		
		//Log.d(TAG, "in didEnter with piece type " + piece.type) ;
		
		try {
			// Note the piece type that entered.
			if ( PieceCatalog.isPolyomino(piece)
					&& PieceCatalog.getQCombination(piece) == QOrientations.ST ) {
				triggerOfNote = tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO] = true ;
				//Log.d(TAG, "just set Sticky Tetromino") ;
			}
			
			else if ( PieceCatalog.isPolyomino(piece)
					&& PieceCatalog.getQCombination(piece) == QOrientations.UL ) {
				triggerOfNote = tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO] = true ;
				//System.err.println(TAG + " just set Links Tetromino") ;
			}
			
			else if ( PieceCatalog.isTetracube(piece)
					&& PieceCatalog.getQCombination(piece) == QOrientations.SL ) {
				triggerOfNote = tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE] = true ;
			}
			
			else if ( PieceCatalog.isSpecial(piece)
					&& PieceCatalog.getSpecialCategory(piece) == PieceCatalog.SPECIAL_CAT_FLASH ) {
				triggerOfNote = tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FLASH] = true ;
			}
			
			else if ( PieceCatalog.isSpecial(piece)
					&& PieceCatalog.getSpecialCategory(piece) == PieceCatalog.SPECIAL_CAT_PUSH_DOWN ) {
				triggerOfNote = tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_PUSH_DOWN] = true ;
			}
			
			// Not a T?
			if ( !PieceCatalog.isTetromino(piece) || PieceCatalog.getTetrominoCategory(piece) != PieceCatalog.TETRO_CAT_T ) {
				triggerOfNote = tsac.justHappened[TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_NOT_T] = true ;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}
		
		if ( triggerOfNote )
			noteTriggersThatJustHappened() ;
	}
	
	
	private static final Object [] copy( Object [] ar ) {
		if ( ar == null )
			return null ;
		
		Object [] res = new Object[ar.length] ;
		for ( int i = 0; i < ar.length; i++ )
			res[i] = ar[i] ;
		return res ;
	}
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE STATE
	//
	// These methods provide the implementation of the SerializableState
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
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
	public TriggerSystem finalizeConfiguration() throws IllegalStateException {
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
		return tsac ;
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
		return new TriggerSystemAtomicConditionsVersioned( tsac, conditions.size(), conjunctions.size() ) ;
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
		if ( in instanceof TriggerSystemAtomicConditions )
			tsac = new TriggerSystemAtomicConditionsVersioned( (TriggerSystemAtomicConditions)in, conditions.size(), conjunctions.size() ) ;
		else
			tsac = (TriggerSystemAtomicConditionsVersioned)in ;
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
		outStream.writeObject(tsac) ;
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
	public TriggerSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		Object in = inStream.readObject() ;
		if ( in instanceof TriggerSystemAtomicConditions )
			tsac = new TriggerSystemAtomicConditionsVersioned( (TriggerSystemAtomicConditions)in, conditions.size(), conjunctions.size() ) ;
		else
			tsac = (TriggerSystemAtomicConditionsVersioned)in ;
		return this ;
	}
	
	
	
}
