package com.peaceray.quantro.model.systems.timing;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;

public abstract class TimingSystem implements SerializableState {
	
	///////////////////////////////////////////////////
	// SETTING INFO
	

	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this LockSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public abstract GameInformation getGameInformation() ;
	
	
	/**
	 * getQInteractions: Collisions are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public abstract QInteractions getQInteractions() ;
	
	///////////////////////////////////////////////////
	// TIME PASSES
	
	/**
	 * Notify the timing system that time has passed.
	 * 
	 * @param time The number of seconds since the last call to 'tick'
	 */
	public abstract void tick( double time ) ;
	
	///////////////////////////////////////////////////
	// THINGS HAPPEN
	
	/**
	 * didMove: Tells the timing system that the piece moved.
	 * moveChange is the "delta" for the movement.
	 * 
	 * @param didMove The delta for the movement
	 */
	public abstract void didMove( Offset moveChange ) ;
	
	/**
	 * didTurn: Tells the timing system that the piece provided
	 * just turned.  The Piece object contains rotation info
	 * 
	 * @param piece The piece which turned.
	 */
	public abstract void didTurn( Piece piece ) ;
	
	/**
	 * didFlip: Tells the timing system that the piece provided just flipped.
	 * @param piece
	 * @param offset
	 */
	public abstract void didFlip( Piece piece, Offset offset ) ;
	
	/**
	 * didKick: Tells the timing system that the piece provided
	 * was just kicked to a new location.  Everything is provided.
	 * 
	 * @param piece	The piece that was kicked
	 * @param offset The new offset for the piece
	 */
	public abstract void didKick( Piece piece, Offset offset ) ;
	
	/**
	 * didFall: Tells the timing system that the piece provided
	 * has fallen to the new offset.
	 * @param newOffset
	 */
	public abstract void didFall( Offset newOffset ) ;
	
	/**
	 * didDrop: Tells the timing system that the user dropped the
	 * piece to the bottom (or as far as it would go)
	 * @param piece The piece the user dropped
	 * @param offset The new offset for the piece
	 */
	public abstract void didDrop( Piece piece, Offset offset ) ;
	
	/**
	 * didLock: Tells the timing system that the piece locked in place.
	 * @param blockField
	 * @param piece
	 * @param offset
	 */
	public abstract void didLock( byte [][][] blockField, Piece piece, Offset offset ) ;
	
	/**
	 * didClear: Tells the timing system that a clear occurred.
	 * @param chromaticRowArray	The rows that were cleared chromatically
	 * @param monochromaticRowArray The rows that were cleared monochromatically
	 */
	public abstract void didClear( int [] chromaticRowArray, boolean [] monochromaticRowArray ) ;
	
	/**
	 * didPrepare: a new piece (provided) is preparing to enter the game.
	 * @param piece The piece that entered
	 * @param offset The offset at which it is prepared
	 */
	public abstract void didPrepare( Piece piece, Offset offset ) ;
	
	/**
	 * didEnter: a new piece (provided) entered the game.
	 * @param piece The piece that entered
	 * @param offset The offset at which it entered
	 */
	public abstract void didEnter( Piece piece, Offset offset ) ;
	
	///////////////////////////////////////////////////
	// SHOULD THINGS HAPPEN?
	
	/**
	 * canMove: Ask the timing system whether, as far as
	 * it's concerned, the piece should move (probably
	 * according to user input).
	 * 
	 * @param moveChange The 'delta' for the proposed move.
	 * @return Can the piece move?
	 */
	public abstract boolean canMove( Offset moveChange ) ;
	
	/**
	 * canTurn: Asks the timing system whether it's appropriate
	 * for the piece to turn now.
	 * 
	 * @param piece The Piece object to be turned.
	 * @param rotDirection One of the constants for TURN_* defined by RotationSystem
	 * @return Can the piece turn the specified way?
	 */
	public abstract boolean canTurn( Piece piece, int rotDirection ) ;
	
	/**
	 * canTurn: Asks the timing system whether it's appropriate
	 * for the piece to flip now.
	 * 
	 * @param piece The Piece object to be flipped.
	 * @return Can the piece flip?
	 */
	public abstract boolean canFlip( Piece piece ) ;
	
	
	
	/**
	 * canKick: Ask the timing system whether a kick is appropriate
	 * at the moment.
	 * 
	 * @param piece	The piece to be kicked
	 * @return Can it be kicked?
	 */
	public abstract boolean canKick( Piece piece ) ;
	
	/**
	 * canFall: Ask the timing system whether it's time for the piece
	 * to fall a bit.
	 * 
	 * @return Appropriate to fall?
	 */
	public abstract boolean canFall( ) ;
	
	/**
	 * canDrop: Ask the timing system whether it's "time" to drop the
	 * piece, probably because the user pressed 'down' to drop it.
	 * 
	 * @param piece The piece the user dropped
	 * @param offset The new offset for the piece
	 * @return Appropriate to drop?
	 */
	public abstract boolean canDrop( Piece piece, Offset offset ) ;
	
	
	/**
	 * canFastFall: Ask the timing system whether it's "time" to fastFall
	 * the piece.  Note: the timing system has no concept of whether it
	 * is APPROPRIATE to fast fall (just like it doesn't know whether
	 * a piece is in the right place to lock), only whether -- under
	 * the condition that we are currently fast-falling -- enough time
	 * has passed that we should ffall 1 space.
	 * 
	 * FastFall: the Game can sometimes enter a state where it prefers
	 * "fast falls" to normal falls.  One way of entering this state
	 * is for the player to hold the 'down' button.
	 * 
	 * @param newOffset
	 */
	public abstract boolean canFastFall() ;
	
	
	/**
	 * canLock: Is it time to lock the piece?
	 * 
	 * @param blockField
	 * @param piece
	 * @param offset
	 * @return Enough time has passed that locking is appropriate, if
	 * the piece can lock at this location (not checked by TimingSystem)
	 */
	public abstract boolean canLock( byte [][][] blockField, Piece piece, Offset offset ) ;
	
	/**
	 * canClear: Can we go ahead and clear some rows?
	 * 
	 * @return Should we do this clear?
	 */
	public abstract boolean canClear( ) ;
	
	/**
	 * canEnter: Can the piece provided entered the game.
	 * 
	 * @param piece The piece to be entered
	 * @param offset The offset at which it will enter
	 * @return Timing-wise, is it appropriate to enter the piece?
	 */
	public abstract boolean canEnter( Piece piece, Offset offset ) ;
	
}
