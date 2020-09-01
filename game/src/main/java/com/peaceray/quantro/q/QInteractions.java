package com.peaceray.quantro.q;

import com.peaceray.quantro.exceptions.QOrientationConflictException;

/**
 * A class which implements QInteractions defines the way
 * the various QOrientations react to each other.  Changing
 * the QInteractions will fundamentally change the game.
 * 
 * Any systems controlling the behavior or interactions of
 * pieces, blocks, etc., should draw its cues from an implementation
 * of QInteractions.  This lets these systems remain generically
 * functional during prototyping and design changes, as well as
 * letting them be swappable between game modes with different internal functionality.
 * 
 * @author Jake
 *
 */
public interface QInteractions {

	///////////////////////////////////////////////////////////////////
	// GENERAL BEHAVIOR SUMMARIES:
	//
	// Certain game-behavior code blocks benefit from optimization that
	// relies on QInteraction assumptions.  These blocks are REQUIRED
	// to implement a fully general-purpose alternative that is used
	// when these assumptions fail, even at the cost of efficiency.
	//
	// However, because optimization is so important, the following 
	// 'behavior description' functions are made available.  If the
	// behavior of this QI object matches the preconditions of your
	// efficient code path, feel free to use it.
	
	/**
	 * Returns 'true' collides( i, j ) returns true for EVERY
	 * QOrientations i,j, with the exception of i == NO or j == NO,
	 * which returns 'false.'
	 * 
	 * @return The above
	 */
	boolean allQOrientationsCollide() ;
	
	/**
	 * Returns 'true' if the 'mergeInto' method performs a simple
	 * value-copy; in other words, merging {i, 0} and {0, j} will
	 * always produce {i, j}, and a merge-collision {i, j} <-> {k, l}
	 * will always fail.
	 * 
	 * @return The above
	 */
	boolean mergeIntoKeepsQOrientation() ;
	
	
	///////////////////////////////////////////////////////////////////
	// WHERE DO FALLING BLOCKS LAND??
	//
	// When falling (player controlled or not), does 
	// qo1 lock to qo2 when qo1 is in the specified position,
	// relative to qo2?
	// e.g. locksToFromAbove( qo1, qo2 ) returns True if
	// qo1 will "land" on qo2.
	//
	// Directions:
	//		Above:	qo1 is on top of qo2
	//		Below:  qo1 is under qo2 (converse situation of above, although NOT necessarily the same result)
	//		Side:	qo1 is left or right of qo2
	//		Quantum:	qo1 and qo2 are in the same row and column,
	//					but not the same pane.
	boolean locksToFromAbove( byte qo1, byte qo2 ) ;
	boolean locksToFromBelow( byte qo1, byte qo2 ) ;
	boolean locksToFromSide( byte qo1, byte qo2 ) ;
	boolean locksToFromQuantum( byte qo1, byte qo2 ) ;
	// Does it lock to the wall?  To the floor?
	boolean locksToWall( byte qo1 ) ;
	boolean locksToFloor( byte qo1 ) ;
	
	// Same as above, but check sets of blocks instead of just 1.
	boolean locksToFromAbove( byte [] ar1, byte [] ar2 ) ;
	boolean locksToFromBelow( byte [] ar1, byte [] ar2 ) ;
	boolean locksToFromSide( byte [] ar1, byte [] ar2 ) ;
	// Does it lock to the wall?
	boolean locksToWall( byte [] ar ) ;
	boolean locksToFloor( byte [] ar ) ;
	
	// Same as above, except checks within a block field.
	boolean locksToFromAbove( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) ;
	boolean locksToFromBelow( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) ;
	boolean locksToFromSide( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) ;
	// Does it lock to the wall?
	boolean locksToWall( byte [][][] ar, int row, int col ) ;
	boolean locksToFloor( byte [][][] ar, int row, int col ) ;
	
	
	
	
	///////////////////////////////////////////////////////////////////
	// DO LOCKED BLOCKS COME APART?
	//
	// When determining if pieces should come apart or stick
	// together, use separatesFromWhen*, which may have slightly
	// different behavior than locksToFrom*.  For example, in the
	// Quantro prototype,
	//	locksToFromSide( S1, S1 ) = locksToFromSide( U1, S1 ) = false.
	// However,
	//	separatesFromWhenSide( S1, S1 ) = false
	//	separatesFromWhenSide( U1, S1 ) = true
	//
	// In essence, locksTo indicates the behavior of "moving" objects.
	// separatesFrom indicates the behavior of "locked" objects, w.r.t.
	// their rel. locked neighbors.
	boolean separatesFromWhenAbove( byte qo1, byte qo2 ) ;
	boolean separatesFromWhenBelow( byte qo1, byte qo2 ) ;
	boolean separatesFromWhenSide( byte qo1, byte qo2 ) ;
	// All sides does NOT include Quantum.  Returns 'true' only if there
	// is NOTHING to which this block will stick (except the floor).
	boolean separatesFromAllSides( byte qo1 ) ;
	boolean separatesFromWhenQuantum( byte qo1, byte qo2 ) ;
	// Does it separate from the wall?
	boolean separatesFromWall( byte qo1 ) ;
	boolean separatesFromFloor( byte qo1 ) ;
	
	
	
	///////////////////////////////////////////////////////////////////
	// DO BLOCKS COLLIDE?
	//
	// Blocks "collide" with each other if they attempt to occupy the same space
	// at the same time.  Check for that condition before moving blocks.
	//
	// TODO: To optimize, check assumption that collision ==
	//		(qo1 != NO) && (qo2 != NO) and in exact same location.  Then inline the check.
	//
	boolean collides( byte qo1, byte qo2 ) ;				// Collide in the same block?
	boolean collides( byte [] ar1, byte [] ar2 ) ;		// Collision for these arrays?
	boolean collides( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) ;
															// Equivalent to 
															// collides( ar1[0][row][col], ar2[0][row][col] )
															// 		|| collides( ar1[1][row][col], ar2[1][row][col] )

	
	
	///////////////////////////////////////////////////////////////////
	// COMBINING BLOCKS
	//
	// Blocks may need to be merged together, without one overwriting 
	// the other.  For instance, locking an S0 piece where there is
	// already S1, the two should "merge" into what is conceptually SS
	// (but actually represented as {S0,S1}.
	//
	void mergeInto( byte [][][] src, int rowSrc, int colSrc, byte [][][] dest, int destRow, int destCol ) throws QOrientationConflictException ;
	
	
	
	///////////////////////////////////////////////////////////////////
	// METAMORPHOSIZING BLOCKS
	//
	// Blocks may metamorphosize into other blocks under certain
	// circumstances.  For example, current plans are for Quantro
	// ST blocks to become ST_INACTIVE upon the end of the cycle.
	// We give 2 types of methods: checks for whether a metamorphosis
	// will have any effect (e.g. "activates") and the call to perform
	// the metamorphosis ("activate").
	//
	boolean activates( byte qo ) ;
	boolean activates( byte [][][] ar, int qPane, int row, int col ) ;
	void activate( byte [][][] ar, int qPane, int row, int col ) ;
	boolean deactivates( byte qo ) ;
	boolean deactivates( byte [][][] ar, int qPane, int row, int col ) ;
	void deactivate( byte [][][] ar, int qPane, int row, int col ) ;
	
	///////////////////////////////////////////////////////////////////
	// PUSH FLIPS
	//
	// Certain blocks change QOrientation ("flip") when pushed to the
	// opponent.
	//
	byte pushFlip( byte qo ) ;
	
	
	///////////////////////////////////////////////////////////////////
	// FLIPPING BLOCKS
	//
	// Certain custom game types allow the user to 'flip' the falling
	// piece.  For most QOrientations / QCombinations the result is
	// the same QO, but for a few we "flip" into the opposite QPane.
	// 
	// For QCombinations that have an "opposite," use this to represent
	// the reversal of qpanes 0 and 1.
	//
	int flip( int qc ) ;
	void flip( byte [][][] ar, int row, int col ) ;
	
	
	///////////////////////////////////////////////////////////////////
	// TESTING FOR OCCLUSION
	//
	// This test is a hack to prevent "unstable" blocks from each becoming
	// their own piece, potentially increasing the memory requirements
	// by a lot.  A block A is "occluded by" the quantum block B if there
	// is no possible way for a third block C, of whatever orientation,
	// to move through B and strike A.
	//
	// For instance, an unstable block U0 resting on S0 is "occluded by"
	// it.  However, an unstable block UL resting on S0 is not, because
	// an S1 block could pass through S0 and strike UL.  UL *is*
	// occluded by SS, though.
	//
	// Note that "occluded by" assumes that block B will not move or
	// separate, as is assumed in the UL / SS example.
	//
	// Further note that because this method works on the block level,
	// it does not describe the behavior of entire pieces.  See this
	// situation:
	//		(a)	S0	S0 (c)
	// 		(b)	U0	NO
	// Although (a) is occluded by (b), it is also connected to (c),
	// and thus they should not be considered as one contiguous block:
	// (b) might fall farther than (ac) because (c) could land on something
	// in its own column.
	boolean occludedBy( byte qo, byte [] ar ) ;
	boolean occludedBy( byte qo, byte [][][] ar, int row, int col ) ;
	
	
	///////////////////////////////////////////////////////////////////
	// CLEARED BY CERTAIN CONDITIONS?
	//
	// We assume there are 3 valid "styles" of clear: S0, S1, SL.
	// 
	boolean contributesToClear( byte [] ar ) ;
	boolean contributesToClear( byte [] ar, int qoClear ) ;
	boolean contributesToClear( byte [][][] ar, int row, int col ) ;
	boolean contributesToClear( byte [][][] ar, int row, int col, int qoClear ) ;
	
	// Perform the clear (block level)
	void clearWith( byte [] ar, int qoClear ) ;
	void clearWith( byte [][][] ar, int row, int col, int qoClear ) ;
	
	// Perform the clear (inverse at block level - show those which are cleared)
	void inverseClearWith( byte [] ar, int qoClear ) ;
	void inverseClearWith( byte [][][] ar, int row, int col, int qoClear ) ;
}
