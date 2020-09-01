package com.peaceray.quantro.model.systems.score;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.trigger.Triggerable;
import com.peaceray.quantro.q.QInteractions;

public abstract class ScoreSystem implements SerializableState, Triggerable {


	
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
	
	/**
	 * scorePrepare Score for preparing a new piece to enter?
	 * 
	 * POSTCONDITION: GameInformation has been updated.
	 * 
	 * @param piece The piece entering
	 * @param offset The offset at which it enters
	 * @return How many points earned for the piece preparing?
	 */
	public abstract int scorePrepare( Piece piece, Offset offset ) ;
	
	/**
	 * scoreEnter Score for this piece entering?
	 * 
	 * @param piece The piece entering
	 * @param offset The offset at which it enters
	 * @return How many points earned for the piece entering?
	 */
	public abstract int scoreEnter( Piece piece, Offset offset ) ;
	
	/**
	 * The reserve piece provided was just used.  It was inserted into
	 * the piece queue (not the field).
	 * @param piece
	 * @return
	 */
	public abstract int scoreUseReserve( Piece piece ) ;
	
	/**
	 * The reserve piece provided was just used.  It replaced a piece already
	 * in the field (and now has the provided offset).
	 * 
	 * @param piece
	 * @param offset
	 * @return
	 */
	public abstract int scoreUseReserve( Piece piece, Offset offset ) ;
	
	/**
	 * The piece we specified was just put back in the next list.
	 * @param piece
	 * @return
	 */
	public abstract int scorePutBack( Piece piece) ;	
	
	/**
	 * The piece specified was put in reserve.  Maybe 
	 * @param piece
	 * @return
	 */
	public abstract int scorePutInReserve( Piece piece ) ;
	
	/**
	 * scoreClear Score for these rows clearing.  Call this if there are
	 * no monochromatic clears associated with this level of cascade.
	 * 
	 * @param chromaticArray Chromatic components of the clear
	 * @return How many points earned for the piece clearing?
	 */
	public abstract int scoreClear( int [] chromaticArray ) ;
	
	/**
	 * scoreClear Score for these rows clearing.  Call this if there are
	 * monochromatic components to the clear.
	 * 
	 * @param chromaticArray Chromatic components of the clear
	 * @param monochromaticArray Monochromatic components of the clear
	 * @return How many points earned for the piece clearing?
	 */
	public abstract int scoreClear( int [] chromaticArray, boolean [] monochromaticArray ) ;
	
	/**
	 * scoreDrop Score for the drop that just happened
	 * 
	 * @param piece The piece that was dropped
	 * @param oldOffset The old offset, before the drop
	 * @param newOffset The new offset, after the drop
	 * @return How many points earned for the drop?
	 */
	public abstract int scoreDrop( Piece piece, Offset oldOffset, Offset newOffset ) ;
	
	/**
	 * scoreFall Score for the fall that just happened
	 * 
	 * @param piece The piece that just fell
	 * @param oldOffset The old offset, before the fall
	 * @param newOffset The new offset, after the fall
	 * @return How many points earned for the fall?
	 */
	public abstract int scoreFall( Piece piece, Offset oldOffset, Offset newOffset ) ;
	
	/**
	 * scoreLock Score a piece locking in place
	 * 
	 * @param blockField The field the piece is locking to.
	 * @param piece The piece being locked
	 * @param offset The offset at which the piece will be locked
	 * @return How many points??
	 */
	public abstract int scoreLock( byte [][][] blockField, Piece piece, Offset offset ) ;
	
	/**
	 * scoreComponentFall Score for falling a component.  A 'component' is 
	 * a contiguous segment of a piece that fell independently of
	 * the rest of the piece.  As a convention, first "scoreLock" will be
	 * called on the complete piece.  This method will only be called if the
	 * piece separated after that.
	 * 
	 * @param chunk The chunk that fell
	 * @param oldOffset The old offset, before the fall
	 * @param newOffset The new offset, after the fall
	 * @return How many points earned for the fall?
	 */
	public abstract int scoreComponentFall( Piece chunk, Offset oldOffset, Offset newOffset ) ;
	
	/**
	 * scoreComponentLock Score a component locking in place.  A 'component' is 
	 * a contiguous segment of a piece that fell independently of
	 * the rest of the piece.  As a convention, first "scoreLock" will be
	 * called on the complete piece.  This method will only be called if the
	 * piece separated into components which locked separately (regardless
	 * of whether they then fell to new depths).
	 * 
	 * @param blockField The field the piece is locking to.
	 * @param component The component being locked
	 * @param offset The offset at which the component will be locked
	 * @return How many points??
	 */
	public abstract int scoreComponentLock( byte [][][] blockField, Piece component, Offset offset ) ;
	
	/**
	 * scoreChunkFall Score for falling a chunk.  A 'chunk' is 
	 * a piece of the block field that was freed by a clear,
	 * or some other effect.
	 * 
	 * @param chunk The chunk that fell
	 * @param oldOffset The old offset, before the fall
	 * @param newOffset The new offset, after the fall
	 * @return How many points earned for the fall?
	 */
	public abstract int scoreChunkFall( Piece chunk, Offset oldOffset, Offset newOffset ) ;
	
	/**
	 * We are at the top of a new action cycle.
	 * @return
	 */
	public abstract int scoreStartActionCycle() ;
}
