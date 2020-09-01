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

class TriggerSystemAtomicConditions implements Serializable {
	
	//private static final String TAG = "TriggerSystemAtomicConditions" ;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3397350304032945538L;

	// Conditions for certain types of line clears
	public static final int TRIGGER_CONDITION_CLEAR_S0_AND_S1_PIECE = 	0 ;	// A piece cleared both S0 and S1 rows.
	public static final int TRIGGER_CONDITION_CLEAR_S0_AND_S1_CASCADE = 1 ;	// A cascade cleared both S1 and S2 rows.
	public static final int TRIGGER_CONDITION_CLEAR_S0_AND_S1_UNION = 	2 ;	// The union of all clears did clear both s1 and s2 rows.
	public static final int TRIGGER_CONDITION_CLEAR_SS_HURDLE_PIECE = 	3 ;	// A hurdle of SS rows!
	public static final int TRIGGER_CONDITION_CLEAR_SS_HURDLE_CASCADE = 4 ;	// A cascade for a hurdle of SS rows!
	public static final int TRIGGER_CONDITION_CLEAR_HURDLE_PIECE = 		5 ;		// Most recent piece did a "hurdle" clear, but not with SS.
	public static final int TRIGGER_CONDITION_CLEAR_HURDLE_CASCADE = 	6 ;		// Most recent cascade did a "hurdle" clear, but not with SS.
	public static final int TRIGGER_CONDITION_CLEAR_SS_PIECE = 			7 ;			// Most recent piece cleared at least one row with SS.
	public static final int TRIGGER_CONDITION_CLEAR_SS_CASCADE = 		8 ;			// Most recent cascade cleared at least one row with SS.
	public static final int TRIGGER_CONDITION_CLEAR_PIECE = 			9 ;				// A piece drop cleared rows any rows at all
	public static final int TRIGGER_CONDITION_CLEAR_CASCADE = 			10 ;			// A cascade cleared rows any rows at all
	public static final int TRIGGER_CONDITION_CLEAR_ST_REMOVED = 		11 ;			// Clears removed all ST blocks from the field.
	public static final int TRIGGER_CONDITION_CLEAR_PIECE_GONE_IN_ONE = 12 ;	// The first set of clears completely removed the piece.
	// TODO: Add monochromatic conditions!
	
	// Enumerations for piece placements
	public static final int TRIGGER_CONDITION_PIECE_IMMOBILE = 			13 ;			// Most recent piece cannot move left, right or up.
	public static final int TRIGGER_CONDITION_PIECE_SNUG = 				14 ;				// S0/S1 blocks are now SS.
	public static final int TRIGGER_CONDITION_PIECE_UNSNUG = 			15 ;				// At least one S0/S1 block is NOT SS.
	public static final int TRIGGER_CONDITION_PIECE_T_SPIN = 			16 ;				// Most recent piece was a T, most recent move was a spin, and 3 corners occupied.
	public static final int TRIGGER_CONDITION_PIECE_T_SPIN_NO_WALL = 	17 ;		// More restrictive than T_SPIN - doesn't count walls for 3 corners.
	public static final int TRIGGER_CONDITION_PIECE_T_SPIN_NO_KICK =	18 ;		// More restrictive than T_SPIN - piece must not have "kicked"
	public static final int TRIGGER_CONDITION_PIECE_COMPONENTS_LOCKED_INTO_BLOCKFIELD = 19 ;	// Occurs immediately AFTER the piece blocks have actually been locked into the blockfield.
	
	// Enumerations for piece types
	public static final int TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO = 	20 ;	// Piece just entered was a sticky tetromino
	public static final int TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE = 	21 ;	// Piece just entered was a linked tetracube
	public static final int TRIGGER_CONDITION_PIECE_FLASH = 			22 ;				// Piece just entered is a flash block
	public static final int TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO = 	23 ;					// Piece just entered is a "links" tetromino
	
	public static final int TRIGGER_CONDITION_PIECE_NOT_T = 			24 ;				// Piece just entered is NOT a T-block (useful for distinguishing T-spins from other piece types!)
	
	public static final int TRIGGER_CONDITION_PIECE_LOCKED = 			25 ;				// Piece locked in place
	
	public static final int TRIGGER_CONDITION_RESERVE_USED = 			26 ;				// A generic condition - a piece in reserve was moved out of reserve (either to queue or field).
	
	public static final int NUM_TRIGGER_CONDITIONS = 					27 ;
	
	
	////////////////////////////////////////////////
	// Condition / conjunction stuff
	ArrayList<Boolean> conditionHasOccurred ;
	ArrayList<Boolean> conjunctionHasOccurred ;
	// Some information.
	boolean mostRecentWasTurn ;
	boolean mostRecentTurnKicked ;
	boolean clearedOnce ;
	int clearUnion ;
	
	boolean [] conditionCurrentlyTrue ;
	boolean [] justHappened ;
	
	Piece myBlockPiece ;
	Offset myOffset ;
	byte [][][] pieceInField ;
	
	
	// Constructor.
	TriggerSystemAtomicConditions() {
		// Do nothing!  Actually, just to make sure they aren't NULL,
		conditionHasOccurred = new ArrayList<Boolean>() ;
		conjunctionHasOccurred = new ArrayList<Boolean>() ;
		
		mostRecentWasTurn = false ;
		mostRecentTurnKicked = false ;
		
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
	
	
	TriggerSystemAtomicConditions( TriggerSystemAtomicConditions tsac ) {
		conditionHasOccurred = new ArrayList<Boolean>() ;
		conjunctionHasOccurred = new ArrayList<Boolean>() ;
		for ( int i = 0; i < tsac.conditionHasOccurred.size(); i++ )
			conditionHasOccurred.add( tsac.conditionHasOccurred.get(i) ) ;
		for ( int i = 0; i < tsac.conjunctionHasOccurred.size(); i++ )
			conjunctionHasOccurred.add( tsac.conjunctionHasOccurred.get(i) ) ;
		
		
		mostRecentWasTurn = tsac.mostRecentWasTurn ;
		mostRecentTurnKicked = tsac.mostRecentTurnKicked ;
		
		clearedOnce = tsac.clearedOnce ;
		clearUnion = tsac.clearUnion ;
		
		conditionCurrentlyTrue = ArrayOps.duplicate(tsac.conditionCurrentlyTrue) ;
		justHappened = ArrayOps.duplicate(tsac.justHappened) ;
		
		myBlockPiece = new Piece( tsac.myBlockPiece.toString() ) ;
		
		myOffset = new Offset(tsac.myOffset.x, tsac.myOffset.y) ;
		
		pieceInField = ArrayOps.duplicate(tsac.pieceInField) ;
	}
	
	
	public void newCycle() {
		for ( int i = 0; i < TriggerSystemAtomicConditions.NUM_TRIGGER_CONDITIONS; i++ )
			conditionCurrentlyTrue[i] = false ;
		for ( int i = 0; i < conditionHasOccurred.size(); i++ )
			conditionHasOccurred.set(i, Boolean.FALSE) ;
		for ( int i = 0; i < conjunctionHasOccurred.size(); i++ )
			conjunctionHasOccurred.set(i, Boolean.FALSE) ;
		for ( int i = 0; i < TriggerSystemAtomicConditions.NUM_TRIGGER_CONDITIONS; i++ ) 
			justHappened[i] = false ;
		
		mostRecentWasTurn = false ;
		mostRecentTurnKicked = false ;
		clearedOnce = false ;
		clearUnion = QOrientations.NO ;
	}
	
	
	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// Write all our info.
		// Write conditionHasOccurred, conjunctionHasOccurred
		stream.writeObject( conditionHasOccurred ) ;
		stream.writeObject( conjunctionHasOccurred ) ;
		
		// Write most recent turn/clear info
		stream.writeBoolean( mostRecentWasTurn ) ;		// Write as 0=false, 1=true
		stream.writeBoolean( mostRecentTurnKicked ) ;		// Write as 0=false, 1=true
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
		// Read info.
		// conditionHasOccurred, conjunctionHasOccurred.
		conditionHasOccurred = (ArrayList<Boolean>)stream.readObject() ;
		conjunctionHasOccurred = (ArrayList<Boolean>)stream.readObject() ;
		
		// Most recent turn/clear info
		mostRecentWasTurn = stream.readBoolean() ;
		mostRecentTurnKicked = stream.readBoolean() ;
		clearedOnce = stream.readBoolean() ;
		clearUnion = stream.readInt() ;
		
		// Current and just
		conditionCurrentlyTrue = (boolean [])stream.readObject() ;
		justHappened = (boolean [])stream.readObject() ;
		
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
