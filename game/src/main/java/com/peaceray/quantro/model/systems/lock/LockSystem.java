package com.peaceray.quantro.model.systems.lock;

import java.util.ArrayList;

import com.peaceray.quantro.exceptions.QOrientationConflictException;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;

public abstract class LockSystem implements SerializableState {
	
	
	
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
	 * A behavior question: does this LockSystem maintain QOrientation.
	 * when locking a piece in place?  This is similar to
	 * qi.mergeIntoKeepsQOrientation(): when locking a piece, we
	 * require that empty space exists in the blockfield for
	 * every non-NO block of the piece, and that the QOs are copied
	 * directly and without change into the blockfield.
	 * 
	 * In other words, this method should return 'true' iff the result
	 * of a lock can be considered the "UNION" of the blockfield and
	 * the piece.
	 * 
	 * @return
	 */
	public abstract boolean lockKeepsQOrientation() ;

	/**
	 * shouldLock: according to the rules defined in QInteractions,
	 * should the specified piece lock into the blockField at the specified
	 * offset?  Assumes the piece is falling according to normal gameplay.
	 * 
	 * PRECONDITION: collides(blockField, piece, offset) returns false
	 * 		using the same QInteractions.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return	Is it appropriate to lock the (falling) piece at this offset?
	 */
	public abstract boolean shouldLock(byte [][][] blockField, Piece piece, Offset offset) ;
	
	/**
	 * lock: uses the appropriate rules to lock the piece in place at
	 * the specified location within the blockField.
	 * 
	 * PRECONDITION: collides(blockField, piece, offset) returns false
	 * 		using the same QInteractions.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 */
	public abstract void lock(byte [][][] blockField, Piece piece, Offset offset) throws QOrientationConflictException ;
	
	
	/**
	 * unlock: Attempts to unlock the contents of the blockField.
	 * 
	 * PRECONDITIONS:
	 * 		* chunks and offsets have the same length.
	 * 		* chunks is an ArrayList of Piece objects, offsets is of Offset objects.
	 * 		* 'numChunks' indicates the number of those Piece/Offset objects which
	 * 			are currently significant; these objects are the first numChunks in
	 * 			their respective ArrayLists.
	 * 
	 * POSTCONDITIONS:
	 * 		* blockField contains only those blocks which sit stablely upon the bottom
	 * 			of the field
	 * 		* elements [numChunks:returnVal-1] of chunks/offsets represent contiguous
	 * 			chunks unlocked from the blockField: these chunks did NOT sit
	 * 			upon the bottom of the field, but should instead fall at least 1
	 * 			step before locking again.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param chunks		An ArrayList of N >= 0 Piece objects
	 * @param offsets		An ArrayList of N >= 0 Offset objects
	 * @param numChunks		The number of pieces/offsets n <= N in chunks
	 * 						that are currently in-use and should not be overwritten
	 * @return				The number of pieces/offsets in chunks/offsets
	 * 						that are currently in-use: >= n.
	 */
	public abstract int unlock(byte [][][] blockField, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks) ;
	
	/**
	 * unlock: Unlocks the contiguous components of the provided Piece object.
	 * Distinct from the version taking a blockField in that, e.g., 1. there is
	 * no regard for blocks "sitting" on the ground, and 2. the 'piece' itself
	 * is left unchanged by the operation (blocks are COPIED into the components,
	 * NOT moved).
	 * 
	 * 
	 * PRECONDITIONS:
	 * 		'numComponents' indicates the number of Piece objects in 'components'
	 * 			which should not be altered.
	 * 
	 * POSTCONDITIONS:
	 * 		'components' now contains all the contiguous elements of the piece,
	 * 			which occur after the numComponents Pieces already there.  The
	 * 			value returned is the total number of valid components in the
	 * 			array.
	 * 		'piece' is completely unaltered.
	 * 
	 * @param piece			The Piece to be separated into contiguous components
	 * @param components	An ArrayList of N >= 0 Piece objects
	 * @param numComponents	The number of valid Pieces n <= N currently in-use
	 * @return				The number of components currently in use: >= n.
	 */
	public abstract int unlock(Piece piece, ArrayList<Piece> components, int numComponents) ;
	
	/**
	 * unlock: Forcibly unlocks any blocks above "coord."
	 * 
	 * PRECONDITIONS:
	 * 		* chunks and offsets have the same length.
	 * 		* chunks is an ArrayList of Piece objects, offsets is of Offset objects.
	 * 		* 'numChunks' indicates the number of those Piece/Offset objects which
	 * 			are currently significant; these objects are the first numChunks in
	 * 			their respective ArrayLists.
	 * 
	 * POSTCONDITIONS:
	 * 		* blockField contains only those blocks which are not directly above coord
	 * 		* elements [numChunks:returnVal-1] of chunks/offsets represent contiguous
	 * 			chunks unlocked from the blockField: these chunks were directly above coord.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param coord			A coordinate within the blockField; blocks above this will be forcibly unlocked
	 * @param chunks		An ArrayList of N >= 0 Piece objects
	 * @param offsets		An ArrayList of N >= 0 Offset objects
	 * @param numChunks		The number of pieces/offsets n <= N in chunks
	 * 						that are currently in-use and should not be overwritten
	 * @return				The number of pieces/offsets in chunks/offsets
	 * 						that are currently in-use: >= n.
	 */
	public abstract int unlockColumnAbove(byte [][][] blockField, Offset coord, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks) ;
	
	
}
