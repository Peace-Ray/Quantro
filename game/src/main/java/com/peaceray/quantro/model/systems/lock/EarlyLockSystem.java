package com.peaceray.quantro.model.systems.lock;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


import com.peaceray.quantro.exceptions.QOrientationConflictException;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;

public class EarlyLockSystem extends LockSystem {
	
	@SuppressWarnings("unused")
	private static final String TAG = "EarlyLockSystem" ;
	
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	// This system does not maintain a "state": behavior is set
	// entirely by method parameters and the QInteractions object
	// provided.
	// Nevertheless, for completion's sake, we implement an empty state object.
	private EmptyState state ;
	private boolean configured ;
	
	// These structures are simply for preallocation purposes;
	// they do not require consistency between method calls.
	private boolean [][][] resting ;
	private boolean [][][] marked ;
	private boolean [][][] markedPieceBlocks ;
	private boolean [][][] allMarkedPieceBlocks ;
	
	private int [] qstack ;
	private int [] rstack ;
	private int [] cstack ;
	
	private Offset myLL ;
	private Offset myUR ;
	
	private Offset tightenDeltaLL ;
	
	// Constructor!
	public EarlyLockSystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		state = new EmptyState() ;
		configured = false ;
		
		myLL = new Offset() ;
		myUR = new Offset() ;
		tightenDeltaLL = new Offset() ;
		
		myLL.x = myLL.y = 0 ;
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
	public boolean lockKeepsQOrientation() {
		return qi.mergeIntoKeepsQOrientation() ;
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// STATEFUL METHODS
	//
	// Although this system does not maintain a state,
	// these methods are considered "stateful" under the specification
	// given by SerializableState.
	//
	////////////////////////////////////////////////////////////////
	
	
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
	public boolean shouldLock(byte [][][] blockField, Piece piece, Offset offset) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Checks the pieces for lock conditions: any block in the piece
		// other than QOrientations.NO should be checked against its
		// neighbors in the blockField: if any lock to their neighbors,
		// or to the wall/floor which they are touching, return true.
		// Else, return false.
		
		// TODO: Add checks for horizontal and upwards locking.  Currently,
		//		implemented on the assumption that locks ONLY occur when
		//		blocks land (either on the ground, or on another block).
		
		// Determine limits to save time on referencing fields we
		// don't have to.
		int R = piece.boundsUR.y - piece.boundsLL.y ;
		int C = piece.boundsUR.x - piece.boundsLL.x ;
		
		int fieldRows = blockField[0].length ;
		int fieldCols = blockField[0][0].length ;
		
		// A space to store field row, col and piece row, col,
		// which are relative their internal representations
		// (all offsets accounted for).  This prevents doing
		// the same arithmetic conversion many times.
		int fr ;
		int fc ;
		int pr ;
		int pc ;
		
		// Check each block in the piece.  Instead of going from 0
		// to R, shrink the bounds to only those that are valid within
		// blockField.  In other words, r + offset.y should be >= 0,
		// and < fieldRows.
		int rmin = Math.max(0,-offset.y) ;
		int rmax = Math.min(R, fieldRows-offset.y) ;
		int cmin = Math.max(0, -offset.x) ;
		int cmax = Math.min(C, fieldCols-offset.x) ;
		for ( int r = rmin; r < rmax; r++ ) {
			fr = r + offset.y ;
			pr = r + piece.boundsLL.y ;
			
			for ( int c = cmin; c < cmax; c++ ) {
				fc = c + offset.x ;
				pc = c + piece.boundsLL.x ;
				
				// Locks in place if fr is 0 (the floor) and
				// this piece block locks to floor, or if fr > 0
				// and the piece block locks to the block beneath it.
				if ( fr == 0 && qi.locksToFloor(piece.blocks, pr, pc) ) {
					//System.err.println(TAG + " offset is " + offset.getRow() + " " + offset.getCol()) ;
					//System.err.println(TAG + " shouldLock is YES: " + pr + " " + pc + " ") ;
					return true ;
				}
				if ( fr > 0 && qi.locksToFromAbove(piece.blocks, pr, pc,
													blockField, fr-1, fc) ) {
					//System.err.println(TAG + " shouldLock is YES: " + pr + " " + pc + " " + (fr-1) + " " + fc) ;
					return true ;
				}
			}
		}
		
		// If we get here, it shouldn't lock.
		return false ;
	}
	
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
	public void lock(byte [][][] blockField, Piece piece, Offset offset) throws QOrientationConflictException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Step through the valid blocks, merging the piece into the blockField.
		
		// Determine limits to save time on referencing fields we
		// don't have to.
		int R = piece.boundsUR.y - piece.boundsLL.y ;
		int C = piece.boundsUR.x - piece.boundsLL.x ;
		
		int fieldRows = blockField[0].length ;
		int fieldCols = blockField[0][0].length ;
		
		// A space to store field row, col and piece row, col,
		// which are relative their internal representations
		// (all offsets accounted for).  This prevents doing
		// the same arithmetic conversion many times.
		int fr ;
		int fc ;
		int pr ;
		int pc ;
		
		// Check each block in the piece.  Instead of going from 0
		// to R, shrink the bounds to only those that are valid within
		// blockField.  In other words, r + offset.y should be >= 0,
		// and < fieldRows.
		int rmin = Math.max(0,-offset.y) ;
		int rmax = Math.min(R, fieldRows-offset.y) ;
		int cmin = Math.max(0, -offset.x) ;
		int cmax = Math.min(C, fieldCols-offset.x) ;
		for ( int r = rmin; r < rmax; r++ ) {
			fr = r + offset.y ;
			pr = r + piece.boundsLL.y ;
			
			for ( int c = cmin; c < cmax; c++ ) {
				fc = c + offset.x ;
				pc = c + piece.boundsLL.x ;
				
				// Let QInteractions sort it out.
				qi.mergeInto(piece.blocks, pr, pc, blockField, fr, fc) ;
			}
		}
	}
	
	
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
	public int unlock(byte [][][] blockField, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// TODO: Improve efficiency by NOT unlocking-then-relocking piece(s) sitting on the ground
		// TODO: Allow some kind of preallocation for Piece's .blocks arrays.
		// TODO: When setting resting, also sweep up the walls, checking sticksToWall.
		//			Allow possibility of blocks NOT 'sticksToFloor'
		
		// We unlock pieces as follows.  First, sweep through the blockField, making
		// a note of each block that is legitimately resting on the ground.
		// Mark these blocks in "restingOnGround".
		// Then, sweep through the blockField again, beginning from every non-NO block
		// which is not marked as resting on the ground.  Flood-fill from each one.
		// We perform flood-fill by marking the blocks we mean to take in 'marked',
		// then moving all such blocks into their own piece.
		
		// We need both 'marked' and 'restingOnGround' to match blockField's dimensions.
		resting = ArrayOps.allocateToMatchDimensions( resting, blockField ) ;
		marked = ArrayOps.allocateToMatchDimensions( marked, blockField ) ;
		
		// Clear out restingOnGround; don't want any mis-marked blocks.
		ArrayOps.setEmpty( resting ) ;
		
		// blockField dimensions?
		int R = blockField[0].length ;
		int C = blockField[0][0].length ;
		
		// Now mark anything sitting on the ground.  Sweep across the bottom row,
		// flood-filling anything not yet marked as 'resting'.
		// TODO: Also sweep up outer columns to allow blocks to stick to walls
		// TODO: Sweep ALL blocks, if there are any block types that stick in "space"
		for ( int qo = 0; qo < 2; qo++ ) {
			for ( int c = 0; c < C; c++ ) {
				if ( blockField[qo][0][c] != QOrientations.NO && !resting[qo][0][c] ) {
					// Not resting, but is a block.  Flood here.
					// TODO: Check 'separatesFromFloor' before flooding, if there are
					// blocks which actually do separate from the floor.  If this
					// change is needed, also modify the 'for' loop below to go from
					// r = [0,R) instead of r = [1,R).
					floodAndMarkBlocks( blockField, qo, 0, c, resting ) ;
				}
			}
		}
		
		//System.err.println(TAG + "unlock" + "\n") ;
		//System.err.println(Game.arrayAsString(resting)) ; 
		//System.err.println(TAG + "above is the 'resting' array, check against this:" + "\n") ;
		//System.err.println(Game.arrayAsString(blockField)) ; 
		
		// That's done.  Now that we know which blocks are "safe", i.e. should
		// not be moved into their own pieces, begin separating those that are
		// not.  Each one of these should be moved into its own 'blocks' array
		// within a piece.  This is a four-step process:
		// 1, find a block which has not yet been removed but is not resting.
		// 2, floodAndMarkBlocks from that block.
		// 3, locate the next appropriate Piece/Offset pair in the ArrayLists,
		// either by allocating one or taking one that isn't in-use.  Make
		// sure its .blocks array matches blockField in size.
		// 4, move the marked blocks from blockField into the Piece.
		
		// Pointers to objects so we don't need to keep querying ArrayLists
		Piece p ;
		Offset o ;
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int r = 1; r < R; r++ ) {				// r = [1,R) since r=0 is assumed to be resting
				for ( int c = 0; c < C; c++ ) {
					// Should we flood from here?
					int qo = blockField[qp][r][c] ;
					if ( qo != QOrientations.NO && !resting[qp][r][c] ) {		// 1
						ArrayOps.setEmpty( marked ) ;
						floodAndMarkBlocks( blockField, qp, r, c, marked ) ;					// 2
						if ( ArrayOps.isEmpty(marked) ) {
							// DESTROY DESTROY DESTROY
							throw new RuntimeException("unlocked an empty field starting with " + qo + " at (" + qp + "," + r + "," + c + ")") ;
						}
						if ( chunks.size() <= numChunks ) {										// 3a
							p = new Piece() ;
							o = new Offset() ;
							chunks.add( p ) ;
							offsets.add( o ) ;
						}
						else {
							p = chunks.get(numChunks) ;
							o = offsets.get(numChunks) ;
						}
						numChunks++ ;
						// Make sure 'blocks' is sufficient
						p.blocks = ArrayOps.allocateToMatchDimensions( p.blocks, blockField ) ;			// 3b
						ArrayOps.setEmpty( p.blocks ) ;
						p.setBounds() ;
						o.x = o.y = 0 ;
						// Move marked blocks.													// 4
						for ( int qo2 = 0; qo2 < 2; qo2++ ) {
							for ( int r2 = 1; r2 < R; r2++ ) {
								for ( int c2 = 0; c2 < C; c2++ ) {
									if ( marked[qo2][r2][c2] ) {
										p.blocks[qo2][r2][c2] = blockField[qo2][r2][c2] ;
										blockField[qo2][r2][c2] = QOrientations.NO ;
									}
								}
							}
						}
						// Tighten the bounds on this piece.  This takes a bit of work, but we 
						// expect to do a lot with this piece (including multiple examinations
						// and copies) so getting tight bounds now saves us a lot of time later on.
						p.tightenBounds(tightenDeltaLL) ;
						o.x += tightenDeltaLL.x ;
						o.y += tightenDeltaLL.y ;
					}
				}
			}
		}
		
		// We should be done: all the new chunks are included in pieces.  I guess that's it?
		return numChunks ;
	}
	
	/**
	 * unlock: Unlocks the contiguous components of the provided Piece object.
	 * Distinct from the version taking a blockField in that, e.g., there is
	 * no regard for blocks "sitting" on the ground.  Unlock the blockField
	 * version, the 'piece.blocks' array is unchanged by this operation.
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
	 * 		'piece' is unchanged
	 * 
	 * EFFICIENCY: For better efficiency, reserve a different array for calls to
	 * 		this function as for 'unlock(blockField...)'
	 * 
	 * @param piece			The Piece to be separated into contiguous components
	 * @param components	An ArrayList of N >= 0 Piece objects
	 * @param numComponents	The number of valid Pieces n <= N currently in-use
	 * @return				The number of components currently in use: >= n.
	 */
	public int unlock(Piece piece, ArrayList<Piece> components, int numComponents) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Need a place to mark blocks for each component, as well as a place to
		// mark ALL blocks we've taken (it is not implicit, since we are not altering
		// piece.blocks).
		markedPieceBlocks = ArrayOps.allocateToMatchDimensions( markedPieceBlocks, piece.blocks ) ;
		allMarkedPieceBlocks = ArrayOps.allocateToMatchDimensions( allMarkedPieceBlocks, piece.blocks ) ;
		ArrayOps.setEmpty( allMarkedPieceBlocks ) ;
		
		// Iterate through the rows included in the piece
		int R = piece.boundsUR.y - piece.boundsLL.y ;
		int C = piece.boundsUR.x - piece.boundsLL.x ;
		
		// Storage so we don't need to repeat arithmetic ops or object referencing
		int pr, pc, pr2, pc2 ;
		Piece p ;
		
		// Let's go!
		for ( int qo = 0; qo < 2; qo++ ) {
			for ( int r = 0; r < R; r++ ) {
				pr = r + piece.boundsLL.y ;
				for ( int c = 0; c < C; c++ ) {
					pc = c + piece.boundsLL.x ;
					// Flood if this block is not NO, and has not yet been marked.
					// TODO: This assumes flooding is symmetrical; i.e., if A floods to B, then
					// B would have flooded to A.  If this is not the case, floodAndMarkBlocks needs
					// some way to avoid re-flooding.  For example, if
					// 	B A
					// and we have already marked B in a previous iteration, we need to know this
					// to prevent A from flooding to B again.  This is not an issue if B already
					// flooded to A, as then A will be marked and we will not initiate a flood from it.
					if ( !allMarkedPieceBlocks[qo][pr][pc] && piece.blocks[qo][pr][pc] != QOrientations.NO ) {
						ArrayOps.setEmpty( markedPieceBlocks ) ;
						floodAndMarkBlocks( piece.blocks, qo, pr, pc, piece.boundsLL, piece.boundsUR, markedPieceBlocks ) ;
						
						// Need a place to put this.  Make a 'component' that matches
						// most aspects of piece: it must be very close, since we
						// assume the same 'offset' is appropriate.
						if ( components.size() <= numComponents ) {
							p = new Piece() ;
							components.add(p) ;
						}
						else {
							p = components.get(numComponents) ;
						}
						numComponents++ ;
						
						// Set everything in p appropriately.
						p.blocks = ArrayOps.allocateToMatchDimensions( p.blocks, piece.blocks ) ;
						ArrayOps.setEmpty(p.blocks) ;
						p.boundsLL.takeVals( piece.boundsLL ) ;
						p.boundsUR.takeVals( piece.boundsUR ) ;
						
						// Go through the marked blocks and copy, noting also in allMarkedPieceBlocks
						for ( int qo2 = 0; qo2 < 2; qo2++ ) {
							for ( int r2 = 0; r2 < R; r2++ ) {
								pr2 = r2 + piece.boundsLL.y ;
								for ( int c2 = 0; c2 < C; c2++ ) {
									pc2 = c2 + piece.boundsLL.x ;
									if ( markedPieceBlocks[qo2][pr2][pc2] ) {
										p.blocks[qo2][pr2][pc2] = piece.blocks[qo2][pr2][pc2] ;
										allMarkedPieceBlocks[qo2][pr2][pc2] = true ;
									}
								}
							}
						}
					}
				}
			}
		}
		
		// numComponents is the number of components.  My comments are the BEST.
		return numComponents ;
	}
	
	
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
	public int unlockColumnAbove(byte [][][] blockField, Offset coord, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ; 
		
		// Two-step process.  Perform our own forced unlock of the column above 'coord'.
		// Once this is done, pass the resulting blockField to a standard call to "unlock"
		// to perform the rest of the work (e.g., by unlocking the column we may have
		// freed some blocks horizontally locked to it).
		
		// By using bounds, we can call "floodAndMarkBlocks" only within the specified
		// column.
		marked = ArrayOps.allocateToMatchDimensions( marked, blockField ) ;
		
		// blockField dimensions?
		int R = blockField[0].length ;
		int C = blockField[0][0].length ;
		
		// Bounds?  Remember that we unlock ABOVE the coord given,
		// so increment its row by one for the lower bound.
		Offset boundsLL = new Offset() ;
		boundsLL.setRow( coord.getRow() + 1 ) ;
		boundsLL.setCol( coord.getCol() )  ;
		// The upper bound is loose; use the top of the field and
		// one past the coordinate column.
		Offset boundsUR = new Offset() ;
		boundsUR.setRow( R ) ;
		boundsUR.setCol( coord.getCol() + 1 ) ;
		
		// 1, find a block in the column which has not yet been unlocked.
		// 2, floodAndMarkBlocks from that block.
		// 3, locate the next appropriate Piece/Offset pair in the ArrayLists,
		// either by allocating one or taking one that isn't in-use.  Make
		// sure its .blocks array matches blockField in size.
		// 4, move the marked blocks from blockField into the Piece.
		
		// Pointers to objects so we don't need to keep querying ArrayLists
		Piece p ;
		Offset o ;
		int c = coord.getCol() ;
		for ( int qo = 0; qo < 2; qo++ ) {
			for ( int r = boundsLL.getRow(); r < R; r++ ) {
				// Should we flood from here?
				if ( blockField[qo][r][c] != QOrientations.NO ) {		// 1
					ArrayOps.setEmpty( marked ) ;
					floodAndMarkBlocks( blockField, qo, r, c, boundsLL, boundsUR, marked ) ;	// 2
					if ( chunks.size() <= numChunks ) {					// 3a
						p = new Piece() ;
						o = new Offset() ;
						chunks.add( p ) ;
						offsets.add( o ) ;
					}
					else {
						p = chunks.get(numChunks) ;
						o = offsets.get(numChunks) ;
					}
					numChunks++ ;
					// Make sure 'blocks' is sufficient
					p.blocks = ArrayOps.allocateToMatchDimensions( p.blocks, blockField ) ;			// 3b
					ArrayOps.setEmpty( p.blocks ) ;
					p.setBounds() ;
					o.x = o.y = 0 ;
					// Move marked blocks.													// 4
					for ( int qo2 = 0; qo2 < 2; qo2++ ) {
						for ( int r2 = 1; r2 < R; r2++ ) {
							for ( int c2 = 0; c2 < C; c2++ ) {
								if ( marked[qo2][r2][c2] ) {
									p.blocks[qo2][r2][c2] = blockField[qo2][r2][c2] ;
									blockField[qo2][r2][c2] = QOrientations.NO ;
								}
							}
						}
					}
					// Tighten the bounds on this piece.  This takes a bit of work, but we 
					// expect to do a lot with this piece (including multiple examinations
					// and copies) so getting tight bounds now saves us a lot of time later on.
					p.tightenBounds(tightenDeltaLL) ;
					o.x += tightenDeltaLL.x ;
					o.y += tightenDeltaLL.y ;
				}
			}
		}
		
		// Done unlocking the column.
		return numChunks ;
	}
	
	
	/**
	 * Perform a flood-fill, noting every block reached in 'marked'.  The flood begins
	 * from the block indicated by [qo,r,c] and proceeds across every block adjacent
	 * that does not "separatesFrom".  As an assumption, we do not flood into NO
	 * and do not perform any separatesFrom checks.
	 * 
	 * This flood uses a heuristic to avoid creating too many component chunks.
	 * The main problem is "unstable" blocks U0, U1, UL, which tend to simply
	 * sit on top of other blocks but technically separate from them.  The hueristic
	 * applies only to upward flooding from B to T, and is as follows:
	 * 
	 * floodUp <- false
	 * if !separatesFromWhenAbove( T, B ):
	 * 		floodUp <- true
	 * else if separatesFromAllSides( T ):
	 * 		int [] ar <- marked panes of B
	 * 		if occludedBy( T, ar ):
	 * 			floodUp <- true
	 * 
	 * PRECONDITION:
	 * 		* The provided qoFirst, rFirst, cFirst reference a block within field
	 * 			that is within the bounds, has not been marked, and is a valid block
	 * 			from which to flood.
	 * 
	 * @param field			Field of blocks to flood through and mark
	 * @param qo			quantum pane to begin from
	 * @param r				row to begin from
	 * @param c				column to begin from
	 * @param marked		We set to 'true' any block struck by the flood.
	 */
	private void floodAndMarkBlocks( byte [][][] field, int qoFirst, int rFirst, int cFirst, boolean [][][] marked ) {
		// Call the more general function with bounds set to the size of the field.
		myUR.y = field[0].length ;
		myUR.x = field[0][0].length ;
		
		floodAndMarkBlocks( field, qoFirst, rFirst, cFirst, myLL, myUR, marked ) ;
	}
	
	/**
	 * Perform a flood-fill, noting every block reached in 'marked'.  The flood begins
	 * from the block indicated by [qo,r,c] and proceeds across every block adjacent
	 * that does not "separatesFrom".  As an assumption, we do not flood into NO
	 * and do not perform any separatesFrom checks.
	 * 
	 * We use boundsLL and boundsUR as the boundaries of the flood.  We do not go
	 * past the LL bound, or touch the UR bound (LL is tight, UR is loose).
	 * 
	 * This flood uses a heuristic to avoid creating too many component chunks.
	 * The main problem is "unstable" blocks U0, U1, UL, which tend to simply
	 * sit on top of other blocks but technically separate from them.  The hueristic
	 * applies only to upward flooding from B to T, and is as follows:
	 * 
	 * floodUp <- false
	 * if !separatesFromWhenAbove( T, B ):
	 * 		floodUp <- true
	 * else if separatesFromAllSides( T ):
	 * 		int [] ar <- marked panes of B
	 * 		if occludedBy( T, ar ):
	 * 			floodUp <- true
	 * 
	 * PRECONDITION:
	 * 		* The provided qoFirst, rFirst, cFirst reference a block within field
	 * 			that is within the bounds, has not been marked, and is a valid block
	 * 			from which to flood.
	 * 
	 * @param field			Field of blocks to flood through and mark
	 * @param qo			quantum pane to begin from
	 * @param r				row to begin from
	 * @param c				column to begin from
	 * @param boundsLL		tight lower-left bound of valid area to sweep
	 * @param boundsUR		loose upper-right bound of valid area to sweep
	 * @param marked		We set to 'true' any block struck by the flood.
	 */
	private void floodAndMarkBlocks( byte [][][] field, int qoFirst, int rFirst, int cFirst,
			Offset boundsLL, Offset boundsUR, boolean [][][] marked ) {
		
		// Our flood procedure is meant to avoid using Stack or Queue objects,
		// which would be unnecessarily slow, and avoid an array-formatted
		// "stack" or "queue" which may grow well beyond the total size of
		// the block field.
		
		// Procedure is this: take the top position off the "stack" (actually
		// arrays allocated to at least the total elements in 'field').  Assume
		// that this position has *already* been indicated in 'marked.'
		//
		// For each neighbor that is not NO, is not marked, and does not "separatesFrom"
		// the current block, mark it in marked and add its position to the stack.
		//
		// By setting "marked" for a block well before we handle its neighbors,
		// we ensure that the stack never grows beyond the total number of elements
		// in 'field' (and in most cases will be much smaller than it).
		
		// Make sure our stacks have enough space.
		int minElems = 64 ;	// we double when necessary.
		int maxElems = 4*field[0].length*field[0][0].length ;
		if ( qstack == null || qstack.length < minElems ) {
			qstack = new int[minElems] ;
			rstack = new int[minElems] ;
			cstack = new int[minElems] ;
		}
		
		// First thing's first: mark this block and add it to the stacks.  This
		// allows our general procedure to handle the first block.
		marked[qoFirst][rFirst][cFirst] = true ;
		qstack[0] = qoFirst ;
		rstack[0] = rFirst ;
		cstack[0] = cFirst ;
		int stackSize = 1 ;
		
		// local storage
		int qo, r, c ;
		while( stackSize > 0 ) {
			if ( stackSize + 4 >= qstack.length ) {		// if adding 4 ( -1, +5 ) exceeds dimensions, then extend now.
				qstack = extend(qstack, Math.min(qstack.length*2, maxElems)) ;
				rstack = extend(rstack, Math.min(rstack.length*2, maxElems)) ;
				cstack = extend(cstack, Math.min(cstack.length*2, maxElems)) ;
			}
			
			// Pop!
			stackSize-- ;
			qo = qstack[stackSize] ;
			r = rstack[stackSize] ;
			c = cstack[stackSize] ;
			
			// This block is already marked.  Add neighbors one at a time.
			// Left:
			if ( shouldFloodFromTo( field, qo, r, c, qo, r, c-1, boundsLL, boundsUR, marked ) ) {
				marked[qo][r][c-1] = true ;
				qstack[stackSize] = qo ;
				rstack[stackSize] = r ;
				cstack[stackSize] = c-1 ;
				stackSize++ ;
			}
			// Right:
			if ( shouldFloodFromTo( field, qo, r, c, qo, r, c+1, boundsLL, boundsUR, marked ) ) {
				marked[qo][r][c+1] = true ;
				qstack[stackSize] = qo ;
				rstack[stackSize] = r ;
				cstack[stackSize] = c+1 ;
				stackSize++ ;
			}
			// Down:
			if ( shouldFloodFromTo( field, qo, r, c, qo, r-1, c, boundsLL, boundsUR, marked ) ) {
				marked[qo][r-1][c] = true ;
				qstack[stackSize] = qo ;
				rstack[stackSize] = r-1 ;
				cstack[stackSize] = c ;
				stackSize++ ;
			}
			// Up:
			if ( shouldFloodFromTo( field, qo, r, c, qo, r+1, c, boundsLL, boundsUR, marked ) ) {
				marked[qo][r+1][c] = true ;
				qstack[stackSize] = qo ;
				rstack[stackSize] = r+1 ;
				cstack[stackSize] = c ;
				stackSize++ ;
			}
			// Quantum:
			if ( shouldFloodFromTo( field, qo, r, c, (qo+1)%2, r, c, boundsLL, boundsUR, marked ) ) {
				marked[(qo+1)%2][r][c] = true ;
				qstack[stackSize] = (qo+1)%2 ;
				rstack[stackSize] = r ;
				cstack[stackSize] = c ;
				stackSize++ ;
			}
		}
	}
	
	private int [] extend( int [] ar, int length ) {
		if ( length < ar.length )
			throw new IllegalArgumentException("Can't extend to a smaller size.") ;
		int [] ar2 = new int[length] ;
		for ( int i = 0; i < ar.length; i++ )
			ar2[i] = ar[i] ;
		return ar2 ;
	}
	
	private byte [] shouldFloodFromTo_ar = new byte[2] ;
	/**
	 * shouldFloodFromTo: performs all necessary checks to determine if
	 * a flood fill should spread from A to B within the field.  Returns 'false'
	 * if B is NO, if A separates from B, if B is marked, or if the coordinates
	 * for B are out-of-bound.
	 * 
	 * This flood uses a heuristic to avoid creating too many component chunks.
	 * The main problem is "unstable" blocks U0, U1, UL, which tend to simply
	 * sit on top of other blocks but technically separate from them.  The heuristic
	 * applies only to upward flooding from A to B, and is as follows:
	 * 
	 * floodUp <- false
	 * if !separatesFromWhenAbove( B, A ):
	 * 		floodUp <- true
	 * else if separatesFromAllSides( B ):
	 * 		int [] ar <- marked panes of A
	 * 		if occludedBy( B, ar ):
	 * 			floodUp <- true
	 * 
	 * PRECONDITION:
	 * 		* The markov distance between A and B is exactly 1.
	 * 		* A is a valid position within the field.
	 * 
	 * @param field
	 * @param qoA
	 * @param rA
	 * @param cA
	 * @param qoB
	 * @param rB
	 * @param cB
	 * @param boundsLL
	 * @param boundsUR
	 * @param marked
	 * @return
	 */
	private final boolean shouldFloodFromTo( byte [][][] field,
			int qoA, int rA, int cA, int qoB, int rB, int cB,
			Offset boundsLL, Offset boundsUR, boolean [][][] marked ) {
		
		// Easy checks first: within bounds, marked.
		if ( rB < 0 || rB < boundsLL.y || rB >= field[0].length || rB >= boundsUR.y )
			return false ;
		if ( cB < 0 || cB < boundsLL.x || cB >= field[0][0].length || cB >= boundsUR.x )
			return false ;
		if ( marked[qoB][rB][cB] )
			return false ;
		
		byte A ;
		byte B ;
		shouldFloodFromTo_ar[0] = shouldFloodFromTo_ar[1] = 0 ;
		
		// Set A,B.  We delay setting 'ar' for when we may need it.
		A = field[qoA][rA][cA] ;
		B = field[qoB][rB][cB] ;
		
		// Now try the "separatesFrom" test.  Note that these directional checks assume
		// that the precondition is true: exactly one index differs between A and B,
		// and it differs by exactly 1.
		if ( cB < cA || cB > cA ) {					// Leftwards or rightwards
			return !qi.separatesFromWhenSide( B, A ) ;
		}
		if ( rB < rA ) {							// Downwards
			return !qi.separatesFromWhenBelow( B, A ) ;
		}
		if ( qoA != qoB ) {							// QUANTUM!
			return !qi.separatesFromWhenQuantum( B, A ) ;
		}
		
		// We got here, meaning rB > rA.  This is upward flooding.  Apply the hueristic.
		if ( !qi.separatesFromWhenAbove( B, A ) )
			return true ;
		if ( qi.separatesFromAllSides( B ) ) {
			if ( marked[0][rA][cA] )
				shouldFloodFromTo_ar[0] = field[0][rA][cA] ;
			if ( marked[1][rA][cA] )
				shouldFloodFromTo_ar[1] = field[1][rA][cA] ;
			return qi.occludedBy( B, shouldFloodFromTo_ar ) ;
		}
		
		// Nope.
		return false ;
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
	public EarlyLockSystem finalizeConfiguration() throws IllegalStateException {
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
		return state ;
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
		return new EmptyState( state ) ;
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
		state = (EmptyState)in ;
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
		outStream.writeObject(state) ;
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
	public LockSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}
	

}
