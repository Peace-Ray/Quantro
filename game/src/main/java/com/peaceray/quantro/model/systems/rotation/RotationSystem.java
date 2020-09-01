package com.peaceray.quantro.model.systems.rotation;

import com.peaceray.quantro.exceptions.InvalidPieceException;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;



public abstract class RotationSystem implements SerializableState {
	
	protected static final int VERSION_EMPTY = 0 ;
	protected static final int VERSION_LINE_PIECE_FLIP = 1 ;
		// on this version, we flip the line-piece states to conform with "bottom-heavy" rotation assumptions.
	protected static final int VERSION_LINE_PIECE_STANDARD = 2 ;
		// on this version, we roll back the previous change, and set
		// pieces to prioritize lots of weight in the center column upon entering.
		// Pieces will enter in the position that maximizes "center weight,"
		// # of blocks in center 2 columns, so long as no blocks appear 2
		// columns away from center.
		// Ties broken with:
		//		More blocks at the bottom
		//		More blocks in the "front"
	
	protected static final int DEFAULT_VERSION = VERSION_LINE_PIECE_STANDARD ;
	
	public static final int TURN_0 = 0 ;
	public static final int TURN_CW = -90 ;
	public static final int TURN_CCW = 90 ;
	public static final int TURN_CW180 = -180 ;
	public static final int TURN_CCW180 = 180 ;
	
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this RotationSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public abstract GameInformation getGameInformation() ;
	
	
	
	
	/**
	 * getQInteractions: Flips are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public abstract QInteractions getQInteractions() ;
	
	

	// Does this rotation system support the specified piece type?
	public abstract boolean supportsPiece( int type ) ;
	public abstract boolean supportsPiece( Piece p ) ;
	
	// Here are the rotations available.
	// Left and right:
	public abstract void turnCW( Piece p ) throws InvalidPieceException ;
	public abstract void turnCCW( Piece p ) throws InvalidPieceException ;
	// 180 degrees
	public abstract void turnCW180( Piece p ) throws InvalidPieceException ;
	public abstract void turnCCW180( Piece p ) throws InvalidPieceException ;
	// 0 turn - does nothing, but may be useful.
	// For instance, a rotation system can set
	// the block array based on piece type.
	public abstract void turn0( Piece p ) throws InvalidPieceException ;
	
	public abstract void undoTurn( Piece p ) throws InvalidPieceException ;
	
	public abstract void turnMinimumHorizontalProfile( Piece p ) throws InvalidPieceException ;
	
	
	// Flips!
	public abstract void flipHorizontal( Piece p, Offset o ) throws InvalidPieceException ;
	public abstract void flipVertical( Piece p, Offset o ) throws InvalidPieceException ;
	
}
