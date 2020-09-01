package com.peaceray.quantro.model.systems.trigger;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;

class TriggerSystemAtomicConditionsVersioned implements Serializable {


	/**
	 * 
	 */
	private static final long serialVersionUID = -9082291097510609357L;
	
	
	private static final int VERSION = 2 ;
	// 0: An exact copy of TriggerSystemAtomicConditions, except adding new condition: PIECE_PUSH_DOWN.
	// 1: Adds the fields tracking 'just Flipped', etc.
	// 2: Adds 'metamorphosis' as a trigger condition.
	

	// These are the enumerated conditions for triggers
	private static int en = 0 ;
	
	// Conditions for certain types of line clears
	public static final int TRIGGER_CONDITION_CLEAR_S0_AND_S1_PIECE = en++ ;	// A piece cleared both S0 and S1 rows.
	public static final int TRIGGER_CONDITION_CLEAR_S0_AND_S1_CASCADE = en++ ;	// A cascade cleared both S1 and S2 rows.
	public static final int TRIGGER_CONDITION_CLEAR_S0_AND_S1_UNION = en++ ;	// The union of all clears did clear both s1 and s2 rows.
	public static final int TRIGGER_CONDITION_CLEAR_SS_HURDLE_PIECE = en++ ;	// A hurdle of SS rows!
	public static final int TRIGGER_CONDITION_CLEAR_SS_HURDLE_CASCADE = en++ ;	// A cascade for a hurdle of SS rows!
	public static final int TRIGGER_CONDITION_CLEAR_HURDLE_PIECE = en++ ;		// Most recent piece did a "hurdle" clear, but not with SS.
	public static final int TRIGGER_CONDITION_CLEAR_HURDLE_CASCADE = en++ ;		// Most recent cascade did a "hurdle" clear, but not with SS.
	public static final int TRIGGER_CONDITION_CLEAR_SS_PIECE = en++ ;			// Most recent piece cleared at least one row with SS.
	public static final int TRIGGER_CONDITION_CLEAR_SS_CASCADE = en++ ;			// Most recent cascade cleared at least one row with SS.
	public static final int TRIGGER_CONDITION_CLEAR_PIECE = en++ ;				// A piece drop cleared rows any rows at all
	public static final int TRIGGER_CONDITION_CLEAR_CASCADE = en++ ;			// A cascade cleared rows any rows at all
	public static final int TRIGGER_CONDITION_CLEAR_ST_REMOVED = en++ ;			// Clears removed all ST blocks from the field.
	public static final int TRIGGER_CONDITION_CLEAR_PIECE_GONE_IN_ONE = en++ ;	// The first set of clears completely removed the piece.
	// TODO: Add monochromatic conditions!
	
	// Enumerations for piece placements
	public static final int TRIGGER_CONDITION_PIECE_IMMOBILE = en++ ;			// Most recent piece cannot move left, right or up.
	public static final int TRIGGER_CONDITION_PIECE_SNUG = en++ ;				// S0/S1 blocks are now SS.
	public static final int TRIGGER_CONDITION_PIECE_UNSNUG = en++ ;				// At least one S0/S1 block is NOT SS.
	public static final int TRIGGER_CONDITION_PIECE_T_SPIN = en++ ;				// Most recent piece was a T, most recent move was a spin, and 3 corners occupied.
	public static final int TRIGGER_CONDITION_PIECE_T_SPIN_NO_WALL = en++ ;		// More restrictive than T_SPIN - doesn't count walls for 3 corners.
	public static final int TRIGGER_CONDITION_PIECE_T_SPIN_NO_KICK = en++ ;		// More restrictive than T_SPIN - piece must not have "kicked"
	public static final int TRIGGER_CONDITION_PIECE_COMPONENTS_LOCKED_INTO_BLOCKFIELD = en++ ;	// Occurs immediately AFTER the piece blocks have actually been locked into the blockfield.
	
	// Enumerations for piece types
	public static final int TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO = en++ ;	// Piece just entered was a sticky tetromino
	public static final int TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE = en++ ;	// Piece just entered was a linked tetracube
	public static final int TRIGGER_CONDITION_PIECE_FLASH = en++ ;				// Piece just entered is a flash block
	public static final int TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO = en++ ;	// Piece just entered is a "links" tetromino
	public static final int TRIGGER_CONDITION_PIECE_PUSH_DOWN = en++ ;			// Piece just entered was a "push down" block.
	
	public static final int TRIGGER_CONDITION_PIECE_NOT_T = en++ ;				// Piece just entered is NOT a T-block (useful for distinguishing T-spins from other piece types!)
	
	public static final int TRIGGER_CONDITION_PIECE_LOCKED = en++ ;				// Piece locked in place
	
	public static final int TRIGGER_CONDITION_RESERVE_USED = en++ ;				// A generic condition - a piece in reserve was moved out of reserve (either to queue or field).
	
	// Metamorphosis?
	public static final int TRIGGER_CONDITION_METAMORPHOSIS = en++ ;			// A metamorphosis just changed at least 1 block.
	
	public static final int NUM_TRIGGER_CONDITIONS = en ;
	
	
	////////////////////////////////////////////////
	// Condition / conjunction stuff
	ArrayList<Boolean> conditionHasOccurred ;
	ArrayList<Boolean> conjunctionHasOccurred ;
	// Some information.
	boolean mostRecentWasTurn ;
	boolean mostRecentTurnKicked ;
	boolean mostRecentWasFlip ;
	boolean mostRecentFlipKicked ;
	boolean clearedOnce ;
	int clearUnion ;
	
	boolean [] conditionCurrentlyTrue ;
	boolean [] justHappened ;
	
	Piece myBlockPiece ;
	Offset myOffset ;
	byte [][][] pieceInField ;
	
	
	// Constructor.
	TriggerSystemAtomicConditionsVersioned() {
		// Do nothing!  Actually, just to make sure they aren't NULL,
		conditionHasOccurred = new ArrayList<Boolean>() ;
		conjunctionHasOccurred = new ArrayList<Boolean>() ;
		
		mostRecentWasTurn = false ;
		mostRecentTurnKicked = false ;
		mostRecentWasFlip = false ;
		mostRecentFlipKicked = false ;
		clearedOnce = false ;
		clearUnion = QOrientations.NO ;
		
		conditionCurrentlyTrue = new boolean[NUM_TRIGGER_CONDITIONS] ;
		justHappened = new boolean[NUM_TRIGGER_CONDITIONS] ;
		
		myBlockPiece = new Piece() ;
		myBlockPiece.type = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_BLOCK, 1) ;
		myBlockPiece.blocks = new byte [2][1][1] ;
		myBlockPiece.setBounds() ;
		myOffset = new Offset() ;
		
		pieceInField = new byte [2][1][1] ;
	}
	
	
	TriggerSystemAtomicConditionsVersioned( TriggerSystemAtomicConditionsVersioned tsac, int numConditions, int numConjuctions ) {
		conditionHasOccurred = new ArrayList<Boolean>() ;
		conjunctionHasOccurred = new ArrayList<Boolean>() ;
		for ( int i = 0; i < numConditions; i++ )
			conditionHasOccurred.add( tsac.conditionHasOccurred.size() > i ? tsac.conditionHasOccurred.get(i) : Boolean.FALSE ) ;
		for ( int i = 0; i < numConjuctions; i++ )
			conjunctionHasOccurred.add( tsac.conjunctionHasOccurred.size() > i ? tsac.conjunctionHasOccurred.get(i) : Boolean.FALSE ) ;
		
		
		mostRecentWasTurn = tsac.mostRecentWasTurn ;
		mostRecentTurnKicked = tsac.mostRecentTurnKicked ;
		mostRecentWasFlip = tsac.mostRecentWasFlip ;
		mostRecentFlipKicked = tsac.mostRecentFlipKicked ;
		clearedOnce = tsac.clearedOnce ;
		clearUnion = tsac.clearUnion ;
		
		conditionCurrentlyTrue = ArrayOps.duplicate(tsac.conditionCurrentlyTrue) ;
		justHappened = ArrayOps.duplicate(tsac.justHappened) ;
		
		myBlockPiece = new Piece( tsac.myBlockPiece.toString() ) ;
		
		myOffset = new Offset(tsac.myOffset.x, tsac.myOffset.y) ;
		
		pieceInField = ArrayOps.duplicate(tsac.pieceInField) ;
	}
	
	
	TriggerSystemAtomicConditionsVersioned( TriggerSystemAtomicConditions tsac, int numConditions, int numConjuctions ) {
		conditionHasOccurred = new ArrayList<Boolean>() ;
		conjunctionHasOccurred = new ArrayList<Boolean>() ;
		for ( int i = 0; i < numConditions; i++ )
			conditionHasOccurred.add( tsac.conditionHasOccurred.size() > i ? tsac.conditionHasOccurred.get(i) : Boolean.FALSE ) ;
		for ( int i = 0; i < numConjuctions; i++ )
			conjunctionHasOccurred.add( tsac.conjunctionHasOccurred.size() > i ? tsac.conjunctionHasOccurred.get(i) : Boolean.FALSE ) ;
		
		
		mostRecentWasTurn = tsac.mostRecentWasTurn ;
		mostRecentTurnKicked = tsac.mostRecentTurnKicked ;
		mostRecentWasFlip = false ;
		mostRecentFlipKicked = false ;
		clearedOnce = tsac.clearedOnce ;
		clearUnion = tsac.clearUnion ;
		
		conditionCurrentlyTrue = new boolean[NUM_TRIGGER_CONDITIONS] ;
		for ( int i = 0; i < tsac.conditionCurrentlyTrue.length; i++ )
			conditionCurrentlyTrue[ convertConditionToVersioned(i) ] = tsac.conditionCurrentlyTrue[i] ;
		justHappened = new boolean[NUM_TRIGGER_CONDITIONS] ;
		for ( int i = 0; i < tsac.justHappened.length; i++ )
			justHappened[ convertConditionToVersioned(i) ] = tsac.justHappened[i] ;
		
		myBlockPiece = new Piece( tsac.myBlockPiece.toString() ) ;
		
		myOffset = new Offset(tsac.myOffset.x, tsac.myOffset.y) ;
		
		pieceInField = ArrayOps.duplicate(tsac.pieceInField) ;
	}
	
	
	private int convertConditionToVersioned( int condition ) {
		switch( condition ) {
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_S0_AND_S1_PIECE:
			return TRIGGER_CONDITION_CLEAR_S0_AND_S1_PIECE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_S0_AND_S1_CASCADE:
			return TRIGGER_CONDITION_CLEAR_S0_AND_S1_CASCADE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_S0_AND_S1_UNION:
			return TRIGGER_CONDITION_CLEAR_S0_AND_S1_UNION ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_SS_HURDLE_PIECE:
			return TRIGGER_CONDITION_CLEAR_SS_HURDLE_PIECE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_SS_HURDLE_CASCADE:
			return TRIGGER_CONDITION_CLEAR_SS_HURDLE_CASCADE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_HURDLE_PIECE:
			return TRIGGER_CONDITION_CLEAR_HURDLE_PIECE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_HURDLE_CASCADE:
			return TRIGGER_CONDITION_CLEAR_HURDLE_CASCADE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_SS_PIECE:
			return TRIGGER_CONDITION_CLEAR_SS_PIECE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_SS_CASCADE:
			return TRIGGER_CONDITION_CLEAR_SS_CASCADE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_PIECE:
			return TRIGGER_CONDITION_CLEAR_PIECE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_CASCADE:
			return TRIGGER_CONDITION_CLEAR_CASCADE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_ST_REMOVED:
			return TRIGGER_CONDITION_CLEAR_ST_REMOVED ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_CLEAR_PIECE_GONE_IN_ONE:
			return TRIGGER_CONDITION_CLEAR_PIECE_GONE_IN_ONE ;
		
		// Enumerations for piece placements
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_IMMOBILE:
			return TRIGGER_CONDITION_PIECE_IMMOBILE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_SNUG:
			return TRIGGER_CONDITION_PIECE_SNUG ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_UNSNUG:
			return TRIGGER_CONDITION_PIECE_UNSNUG ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_T_SPIN:
			return TRIGGER_CONDITION_PIECE_T_SPIN ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_T_SPIN_NO_WALL:
			return TRIGGER_CONDITION_PIECE_T_SPIN_NO_WALL ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_T_SPIN_NO_KICK:
			return TRIGGER_CONDITION_PIECE_T_SPIN_NO_KICK ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_COMPONENTS_LOCKED_INTO_BLOCKFIELD:
			return TRIGGER_CONDITION_PIECE_COMPONENTS_LOCKED_INTO_BLOCKFIELD ;
		// Enumerations for piece types
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO:
			return TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE:
			return TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_FLASH:
			return TRIGGER_CONDITION_PIECE_FLASH ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO:
			return TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_NOT_T:
			return TRIGGER_CONDITION_PIECE_NOT_T ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_PIECE_LOCKED:
			return TRIGGER_CONDITION_PIECE_LOCKED ;
		case TriggerSystemAtomicConditions.TRIGGER_CONDITION_RESERVE_USED:
			return TRIGGER_CONDITION_RESERVE_USED ;
		default:
			throw new IllegalArgumentException("Not a valid TriggerCondition.") ;
		}
	}
	
	
	public void newCycle() {
		for ( int i = 0; i < TriggerSystemAtomicConditionsVersioned.NUM_TRIGGER_CONDITIONS; i++ )
			conditionCurrentlyTrue[i] = false ;
		for ( int i = 0; i < conditionHasOccurred.size(); i++ )
			conditionHasOccurred.set(i, Boolean.FALSE) ;
		for ( int i = 0; i < conjunctionHasOccurred.size(); i++ )
			conjunctionHasOccurred.set(i, Boolean.FALSE) ;
		for ( int i = 0; i < TriggerSystemAtomicConditionsVersioned.NUM_TRIGGER_CONDITIONS; i++ ) 
			justHappened[i] = false ;
		
		mostRecentWasTurn = false ;
		mostRecentTurnKicked = false ;
		mostRecentWasFlip = false ;
		mostRecentFlipKicked = false ;
		clearedOnce = false ;
		clearUnion = QOrientations.NO ;
	}
	
	
	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(VERSION) ;
		
		// Write all our info.
		// Write conditionHasOccurred, conjunctionHasOccurred
		stream.writeObject( conditionHasOccurred ) ;
		stream.writeObject( conjunctionHasOccurred ) ;
		
		// Write most recent turn/clear info
		stream.writeBoolean( mostRecentWasTurn ) ;		// Write as 0=false, 1=true
		stream.writeBoolean( mostRecentTurnKicked ) ;		// Write as 0=false, 1=true
		stream.writeBoolean( mostRecentWasFlip ) ;
		stream.writeBoolean( mostRecentFlipKicked ) ;
		stream.writeBoolean( clearedOnce ) ;		// Write as 0=false, 1=true
		stream.writeInt( clearUnion ) ;

		// Write current and just
		stream.writeObject( conditionCurrentlyTrue ) ;
		stream.writeObject( justHappened ) ;
		
		// Write piece, offset, block array.
		stream.writeObject( myBlockPiece.toString() ) ;
		stream.writeInt( myOffset.x ) ;
		stream.writeInt( myOffset.y ) ;
		stream.writeObject( ArrayOps.arrayToString(pieceInField) ) ;
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int version = stream.readInt() ;
		
		// Read info.
		// conditionHasOccurred, conjunctionHasOccurred.
		conditionHasOccurred = (ArrayList<Boolean>)stream.readObject() ;
		conjunctionHasOccurred = (ArrayList<Boolean>)stream.readObject() ;
		
		// Most recent turn/clear info
		mostRecentWasTurn = stream.readBoolean() ;
		mostRecentTurnKicked = stream.readBoolean() ;
		if ( version >= 1 ) {
			mostRecentWasFlip = stream.readBoolean() ;
			mostRecentFlipKicked = stream.readBoolean() ;
		}
		clearedOnce = stream.readBoolean() ;
		clearUnion = stream.readInt() ;
		
		// Current and just
		conditionCurrentlyTrue = (boolean [])stream.readObject() ;
		justHappened = (boolean [])stream.readObject() ;
		if ( version <  2 ) {
			// version 2 added the metamorphosis condition.
			boolean [] curTrue = new boolean[NUM_TRIGGER_CONDITIONS] ;
			boolean [] just = new boolean[NUM_TRIGGER_CONDITIONS] ;
			for ( int i = 0; i < conditionCurrentlyTrue.length; i++ ) {
				curTrue[i] = conditionCurrentlyTrue[i] ;
				just[i] = justHappened[i] ;
			}
			conditionCurrentlyTrue = curTrue ;
			justHappened = just ;
		}
		
		// piece, offset, block array.
		myBlockPiece = new Piece( (String)stream.readObject() ) ;
		myOffset = new Offset( stream.readInt(), stream.readInt() ) ;
		pieceInField = ArrayOps.byteArrayFromString( (String)stream.readObject() ) ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
}
