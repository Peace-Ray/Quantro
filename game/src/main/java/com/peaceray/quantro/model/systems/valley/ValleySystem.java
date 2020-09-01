package com.peaceray.quantro.model.systems.valley;

import java.util.ArrayList;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;

/**
 * A ValleySystem is responsible for finding valleys and dropping blocks in them,
 * as a player-reward for certain actions.  It will deterministically locate
 * a valley to fill (i.e., a column) and provide a QOrientation to use in filling
 * the column.  Finally, it will place blocks as individual chunks in the provided
 * chunk list.
 * 
 * @author Jake
 *
 */
public abstract class ValleySystem implements SerializableState {
	
	protected static final int [] PRIMES = new int [] { 31, 37, 41, 43, 47, 53, 59, 61, 67, 71  } ;
	
	protected static final int [] RANDOM_NUMBERS =
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
	
	
	protected static final int randPosInt( int ... seeds ) {
		return randPosInt( null, seeds ) ;
	}
	
	protected static final int randPosInt( int [][] seedSet, int ... seeds ) {
		int index = 0 ;
		
		int numSeedsUsed = 0 ;
		if ( seedSet != null ) {
			for ( int i = 0; i < seedSet.length; i++ ) {
				for ( int j = 0; j < seedSet[i].length; j++ ) {
					index += ( seedSet[i][j] * PRIMES[numSeedsUsed % PRIMES.length] ) % RANDOM_NUMBERS.length ;
					index %= RANDOM_NUMBERS.length ;
					numSeedsUsed++ ;
				}
			}
		}
		
		for ( int i = 0; i < seeds.length; i++ ) {
			index += ( seeds[i] * PRIMES[numSeedsUsed % PRIMES.length] ) % RANDOM_NUMBERS.length ;
			index %= RANDOM_NUMBERS.length ;
			numSeedsUsed++ ;
		}
		
		return RANDOM_NUMBERS[index] ;
	}
	
	
	
	
	/**
	 * Sets the value used for pseudorandom number generation.  Should 
	 * be consistent across all echoed games.
	 * 
	 * @param pseudorandom
	 * @return
	 */
	public abstract ValleySystem setPseudorandom( int pseudorandom ) ;
	
	
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
	 * Adds at most maxBlocks new chunks, which should be dropped and
	 * locked into the blockfield.  Returns the number chunks in the
	 * list after adding blocks.
	 * The ValleySystem guarantees that the blocks will not interfere;
	 * they will not "connect" with each other inappropriately, and
	 * under some basic assumptions should fall as individual blocks.
	 * 
	 * It is up to the caller to ensure a few things.  'minRow' should
	 * be set to the minimum possible row in which it is safe to place a block
	 * that we want to fall independently.  In other words, if there are
	 * already chunks about to fall, minRow should be set above the maximum
	 * height of any blocks in those chunks.  Additionally, minRow should
	 * be set to allow smooth displays, meaning above the top of the screen
	 * and - if there are other chunks - with a 1 row buffer to prevent
	 * inappropriate merges.
	 * 
	 * Additionally, if there are other chunks, they should be preemptively
	 * falled ("fallen?"  It is transitive...) into the blockfield, since
	 * the ValleySystem cannot do this itself.
	 * 
	 * The present version of the ValleySystem uses a very simple abstraction
	 * regarding valley locations.  It may need access to other systems
	 * (such as CollisionSystem, LockSystem) for more sophisticated processes,
	 * although possibly this could be handled by QInteractions.
	 * 
	 * @param minBlocks We guarantee at least minBlocks blocks will be dropped.  If not
	 * 			enough valleys are available, the rest will be randomly positioned.
	 * @param maxBlocks The maximum number of blocks to add.  Return value will be {0,...,maxBlocks} + numChunks.
	 * 			We will drop up to maxBlocks if that many valleys exist.
	 * @param minRow The minimum row in which to place a block.  Should provide a buffer
	 * 			above any falling chunks, as well as allow good-looking draws.
	 * @param field The blockfield in which we search for valleys ( if there are
	 * 			chunks currently falling, fall and lock them into the blockfield before calling)
	 * @param chunks The list of chunks.  We will place blocks starting at [numChunks].
	 * @param offsets The offset of those chunks.  We will place blocks starting at [numChunks].
	 * @param numChunks The number of chunks currently in the provided lists.
	 * @return The number of chunks in the provided lists after this call.
	 */
	public abstract int dropBlocksInValleys( int minBlocks, int maxBlocks,
			int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks ) ;
	
	
	
	/**
	 * Adds at most maxBlocks new chunks, which should be dropped and
	 * locked into the blockfield.  Returns the number chunks in the
	 * list after adding blocks.
	 * The ValleySystem guarantees that the blocks will not interfere;
	 * they will not "connect" with each other inappropriately, and
	 * under some basic assumptions should fall as individual blocks.
	 * 
	 * It is up to the caller to ensure a few things.  'minRow' should
	 * be set to the minimum possible row in which it is safe to place a block
	 * that we want to fall independently.  In other words, if there are
	 * already chunks about to fall, minRow should be set above the maximum
	 * height of any blocks in those chunks.  Additionally, minRow should
	 * be set to allow smooth displays, meaning above the top of the screen
	 * and - if there are other chunks - with a 1 row buffer to prevent
	 * inappropriate merges.
	 * 
	 * Additionally, if there are other chunks, they should be preemptively
	 * falled ("fallen?"  It is transitive...) into the blockfield, since
	 * the ValleySystem cannot do this itself.
	 * 
	 * The present version of the ValleySystem uses a very simple abstraction
	 * regarding junction locations.  It may need access to other systems
	 * (such as CollisionSystem, LockSystem) for more sophisticated processes,
	 * although possibly this could be handled by QInteractions.
	 * 
	 * @param minBlocks The minimum number of blocks to add.  If not
	 * 			enough valleys are available, the rest will be randomly positioned.
	 * @param maxBlocks The maximum number of blocks to add.  Return value will be {0,...,maxBlocks}.
	 * @param minRow The minimum row in which to place a block.  Should provide a buffer
	 * 			above any falling chunks, as well as allow good-looking draws.
	 * @param field The blockfield in which we search for valleys ( if there are
	 * 			chunks currently falling, fall and lock them into the blockfield before calling)
	 * @param chunks The list of chunks.  We will place blocks starting at [numChunks].
	 * @param offsets The offset of those chunks.  We will place blocks starting at [numChunks].
	 * @param numChunks The number of chunks currently in the provided lists.
	 * @return The number of chunks in the provided lists after this call.
	 */
	public abstract int dropBlocksOnJunctions( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks ) ;

	
	
	/**
	 * Drops the specified number of blocks on local peaks.
	 * @param minBlocks
	 * @param maxBlocks
	 * @param minRow
	 * @param field
	 * @param chunks
	 * @param offsets
	 * @param numChunks
	 * @return
	 */
	public abstract int dropBlocksOnPeaks( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks ) ;
	
	
	/**
	 * Drops the specified number of blocks on local corners.
	 * @param minBlocks
	 * @param maxBlocks
	 * @param minRow
	 * @param field
	 * @param chunks
	 * @param offsets
	 * @param numChunks
	 * @return
	 */
	public abstract int dropBlocksOnCorners( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks ) ;
	
	
	
	
	/**
	 * Drops the specified number of blocks on the specified type of location, in order.
	 * We drop up to maxBlocks, and at least minBlocks, on the types of locations specified.
	 * 
	 * Given types[], for each block, we try:
	 * 
	 * For b in maxBlocks:
	 * 		dropped = false
	 * 		for t in types:
	 * 			loc = findBest( t )
	 * 			if loc exists:
	 * 				dropBlockOnLoc
	 * 				dropped = true
	 * 				break ;
	 * 		if not dropped:
	 * 			if b > minBlocks
	 * 				break ;
	 * 			drop on random location
	 * 
	 * @param minBlocks
	 * @param maxBlocks
	 * @param minRow
	 * @param field
	 * @param chunks
	 * @param offsets
	 * @param numChunks
	 * @param types
	 * @return
	 */
	public abstract int dropBlocks( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks,
			Type ... types ) ;
	
	
	private int [][] mCandidatePriority = null ;
	private boolean [][] mIsCandidate = null ;
	
	
	
	
	public enum Type {
		VALLEY, JUNCTION, PEAK, CORNER
	} ;
	
	synchronized protected int findBestCandidate( Type type, boolean qPanesInteract, int [][] heights, boolean [][]candidate, int pseudorandom ) {
		// We can find this in one pass.  A column qualifies
		// as a valley if there are walls on either side
		// (though they may not be immediately adjacent).
		
		// We prefer to fill deep, narrow valleys before shallow,
		// wide ones.  Priority is leftSideHeight + rightSideHeight - width*2;
		// pick the highest number.  In the event of a tie, use
		// pseudorandom to select one.
		
		if ( mCandidatePriority == null || mCandidatePriority.length != heights.length || mCandidatePriority[0].length != heights[0].length ) {
			mCandidatePriority = new int[heights.length][heights[0].length] ;
			mIsCandidate = new boolean[heights.length][heights[0].length] ;
		}
		
		int qpanes = heights.length ;
		int cols = heights[0].length ;
		
		// build a blacklist.  Start with an assumption of being a valley;
		// mark those which we know cannot be.  Then spread "unvalley-ness"
		// for those at the same height.
		for ( int q = 0; q < qpanes; q++ ) {
			for ( int i = 0; i < cols; i++ ) {
				if ( candidate != null && !candidate[q][i] )
					mIsCandidate[q][i] = false ;
				else {
					switch( type ) {
					case VALLEY:
						mIsCandidate[q][i] = isValley( qPanesInteract, heights, q, i ) ;
						break ;
					case JUNCTION:
						mIsCandidate[q][i] = isJunction( qPanesInteract, heights, q, i ) ;
						break ;
					case PEAK:
						mIsCandidate[q][i] = isPeak( qPanesInteract, heights, q, i ) ;
						break ;
					case CORNER:
						mIsCandidate[q][i] = isCorner( qPanesInteract, heights, q, i ) ;
						break ;
					}
				}
			}
		}
		
		
		
		// further revision?
		if ( type == Type.VALLEY ) {
			// propagate 'falses' right and left
			for ( int q = 0; q < qpanes; q++ ) {
				for ( int i = 1; i < cols; i++ ) {
					if ( !mIsCandidate[q][i-1] && heights[q][i] == heights[q][i-1] )
						mIsCandidate[q][i] = false ;
				}
				for ( int i = cols-2; i >= 0; i-- ) {
					if ( !mIsCandidate[q][i+1] && heights[q][i] == heights[q][i+1] ) {
						mIsCandidate[q][i] = false ;
					}
				}
			}
			
			// if everything is a valley, nothing is.
			boolean hasCandidates = false ;
			for ( int q = 0; q < qpanes; q++ ) {
				for ( int i = 0; i < cols; i++ ) {
					if ( !mIsCandidate[q][i] )
						hasCandidates = true ;
				}
				if ( !hasCandidates ) 
					for ( int i = 0; i < cols; i++ )
						mIsCandidate[q][i] = false ;
			}
		}
		
		
		boolean hasCandidates = false ;
		for ( int q = 0; q < qpanes; q++ )
			for ( int i = 0; i < cols; i++ )
				if ( mIsCandidate[q][i] )
					hasCandidates = true ;
		
		if ( !hasCandidates )
			return -1 ;
			
		// have candidates.  Calculate their priority.  Note: adjacent
		// valley columns necessarily have the same height.
		int maxPriority = Integer.MIN_VALUE ;
		int colsWithMax = 0 ;
		boolean spreadPriority = false ;
		switch( type ) {
		case VALLEY:
			spreadPriority = true ;
			break ;
		case JUNCTION:
			spreadPriority = false ;
			break ;
		case PEAK:
			spreadPriority = false ;
		}
		
		
		for ( int q = 0; q < qpanes; q++ ) {
			for ( int i = 0; i < cols; i++ ) {
				if ( mIsCandidate[q][i] && ( !spreadPriority || i == 0 || !mIsCandidate[q][i-1] ) ) {
					// start of a candidate.
					int width = 1 ;
					if ( spreadPriority ) {
						for ( int j = i; j < cols; j++ ) {
							if ( j == cols-1 || !mIsCandidate[q][j+1] ) {
								width = j - i + 1 ;
								break ;
							}
						}
					}
					
					// assign priority
					int priority = -1 ;
					switch( type ) {
					case VALLEY:
						priority = valleyPriority( qPanesInteract, heights, mIsCandidate, q, i, width ) ;
						break ;
					case JUNCTION:
						priority = junctionPriority( qPanesInteract, heights, mIsCandidate, q, i ) ;
						break ;
					case PEAK:
						priority = peakPriority( qPanesInteract, heights, mIsCandidate, q, i ) ;
						break ;
					case CORNER:
						priority = cornerPriority( qPanesInteract, heights, mIsCandidate, q, i ) ;
						break ;
					}
	
					for ( int j = 0; j < width; j++ ) 
						mCandidatePriority[q][i+j] = priority ;
					
					if ( priority > maxPriority ) {
						maxPriority = priority ;
						colsWithMax = width ;
					} else if ( priority == maxPriority ) {
						colsWithMax += width ;
					}
				}
			}
		}
		
		// we have set mCandidatePriority, maxPriority, and colsWithMin.
		// Get a pseudorandom "offset" and count through those with 
		// the maximum priority by that number.
		for ( int q = 0; q < qpanes; q++ )
			for ( int i = 0 ; i < cols; i++ )
				pseudorandom += heights[q][i] * PRIMES[(q*cols + i) % PRIMES.length] ;
		int offset = RANDOM_NUMBERS[pseudorandom % RANDOM_NUMBERS.length] % colsWithMax ;
		
		int count = 0 ;
		for ( int q = 0; q < qpanes; q++ ) {
			for ( int i = 0; i < cols; i++ ) {
				if ( mIsCandidate[q][i] && mCandidatePriority[q][i] == maxPriority && count == offset )
					return q * cols + i ;
				else if ( mIsCandidate[q][i] && mCandidatePriority[q][i] == maxPriority )
					count++ ;
			}
		}
		
		// if we get here, then 'offset' was set too high for the 
		// number of columns.
		throw new RuntimeException("Reached the end of ValleySystem.findBestCandidate without selecting a candidate.") ;
	}
	
	
	private boolean isValley( boolean qPanesInteract, int [][] heights, int qp, int c ) {
		int cols = heights[0].length ;
		int height = heights[qp][c] ;
		int qp_alt = ( qp + 1 ) % 2 ;
		
		// If qPanes interact, make sure this qPane is on top (at least).
		if ( qPanesInteract && height < heights[qp_alt][c] )
			return false ;
		
		return ( c == 0 || heights[qp][c-1] >= heights[qp][c] )
					&& ( c == cols-1 || heights[qp][c] <= heights[qp][c+1] ) ;
	}
	
	
	
	/**
	 * findValley calls this method to assign a priority to each valley.  Parameters are
	 * 'heights' and 'isValley', arrays of the height of each column (as provided by
	 * the caller of findValley, a ValleySystem subclass) and a boolean indication of 
	 * whether the column is valley - defined as a local minimum, possibly several
	 * blocks wide.
	 * 
	 * Overriding this method, and changing the way the "heights" array is determined
	 * for findValley (as well as the 'candidate' array), will significantly change
	 * the behavior of the ValleySystem.  ValleySystem.findValley will select the
	 * highest-priority valley.
	 * 
	 * The initial implementation of this method was a very simple procedure based on
	 * the local height of the valley on each side, and its width.  However, this lead to some
	 * strange behavior.  For example, consider this scenario:
	 *  
	 *     []
	 *     []
	 *     .
	 *     .
	 *     .
	 *     []
	 *     []  
	 *     []
	 *   B []
	 * A [][]    C
	 * [][][][][][][][]
	 * 
	 * The intuitively best place to drop a block is on the far left, at 'A'.  However,
	 * this location has a local depth of 1.  'B' is not a valley at all.  This leaves
	 * 'C', a wide open space without any apparent "gaps," as the high priority valley.
	 * 
	 * One way of prioritizing 'A' over 'C' is to examine slightly larger "canyons,"
	 * for instance, consider the "canyon" comprised of A and B.  This canyon has a width
	 * of 2 and a depth of N, which is the same depth of C.
	 * 
	 * In other words, rather than purely local examination, it may be worth applying a
	 * watershed algorithm to measure valleys.  Basing the priority on the combination
	 * of valley and the canyon (e.g. a "canyon priority" that is dropped to MIN_INT if
	 * not a valley - should help).
	 * 
	 * This implementation will measure priority as
	 * 
	 * 2 * Math.max((watershedCanyonDepth - watershedCanyonWidth),
	 * 					(valleyDepth - localValleyWidth))
	 * 		- localValleyWidth
	 * 
	 * for any valley, and MIN_INT otherwise.
	 * 
	 * @param heights
	 * @param isValley
	 * @param qpane
	 * @param c
	 * @param valleyWidth
	 * @return
	 */
	protected int valleyPriority( boolean qPanesInteract, int [][] heights, boolean [][] isValley, int qpane, int c, int valleyWidth ) {
		if ( !isValley[qpane][c] )
			return Integer.MIN_VALUE ;
		
		int qp_alt = (qpane + 1) % 2 ;
		int cols = heights[0].length ;
		
		// watershed to the left...
		int canyonMin = heights[qpane][c] ;
		int leftEdge ;
		for ( leftEdge = c-1; leftEdge >= 0; leftEdge-- ) {
			if ( heights[qpane][leftEdge] < heights[qpane][leftEdge+1] ||
					( qPanesInteract && heights[qp_alt][leftEdge] < heights[qpane][leftEdge+1] ) )
				break ;
		}
		// to the right...
		int rightEdge ;
		for ( rightEdge = c+1; rightEdge < heights[0].length; rightEdge++ ) {
			if ( heights[qpane][rightEdge] < heights[qpane][rightEdge-1]||
					( qPanesInteract && heights[qp_alt][rightEdge] < heights[qpane][rightEdge+1] )  )
				break ;
		}
		
		if ( leftEdge < 0 && rightEdge >= heights[0].length )
			return 0 ;
		
		int leftHeight = leftEdge >= 0 ? heights[qpane][leftEdge] : -1 ;
		if ( qPanesInteract && leftEdge >= 0 )
			leftHeight = Math.max( leftHeight, heights[qp_alt][leftEdge] ) ;
		
		int rightHeight = rightEdge < cols ? heights[qpane][rightEdge] : -1 ;
		if ( qPanesInteract && rightEdge < c )
			rightHeight = Math.max( rightHeight, heights[qp_alt][rightEdge] ) ;
		
		// Canyon: wide valleys
		int canyonHeightL = ( leftEdge >= 0 ? leftHeight - canyonMin : rightHeight - canyonMin ) ;
		int canyonHeightR = ( rightEdge < cols ? rightHeight - canyonMin : leftHeight - canyonMin ) ;
		
		int canyonPriority = Math.max(canyonHeightL, canyonHeightR) ;
		canyonPriority -= (rightEdge - leftEdge -2 ) ;
		
		// Valley: 1 block wide
		leftHeight = c > 0 ? heights[qpane][c-1] : -1 ;
		if ( qPanesInteract && c > 0 )
			leftHeight = Math.max( leftHeight, heights[qp_alt][c-1] ) ;
		
		rightHeight = c < cols-1 ? heights[qpane][c+1] : -1 ;
		if ( qPanesInteract && c < cols - 1 )
			rightHeight = Math.max( rightHeight, heights[qp_alt][c] ) ;
		
		int valleyHeightL = ( c > 0 ? leftHeight : rightHeight ) - heights[qpane][c] ;
		int valleyHeightR = ( c < cols-1 ? rightHeight : leftHeight ) - heights[qpane][c] ;
		
		int valleyPriority = Math.max(valleyHeightL, valleyHeightR) ;
		valleyPriority -= valleyWidth ;
		
		return 2*Math.max(canyonPriority, valleyPriority) - valleyWidth ;
	}
	
	
	
	/**
	 * A 'junction' is a location where both QPanes could potentially interact.
	 * This is a place where the local QPanes appear superficially different.
	 * 
	 * @param heights
	 * @param qp
	 * @param c
	 * @return
	 */
	private boolean isJunction( boolean qPanesInteract, int [][] heights, int qp, int c ) {
		int qp_alt = (qp + 1) % 2 ;
		int cols = heights[qp].length ;
		int height = Math.max( heights[qp][c], heights[qp_alt][c] ) ;
		
		if ( qPanesInteract && heights[qp][c] < heights[qp_alt][c] )
			return false ;
		
		if ( qPanesInteract && heights[qp][c] == heights[qp_alt][c] && qp == 1 )
			return false ;
		
		boolean qp_left = c > 0 && heights[qp][c-1] > height ;
		boolean qp_under = height > 0 && heights[qp][c] >= heights[qp_alt][c] ;
		boolean qp_right = c < cols-1 && heights[qp][c+1] > height ;
		
		boolean qp_alt_left = c > 0 && heights[qp_alt][c-1] > height ;
		boolean qp_alt_under = heights[qp_alt][c] > 0 && heights[qp_alt][c] >= height ;
		boolean qp_alt_right = c < cols-1 && heights[qp_alt][c+1] > height ;
		
		// This is a junction if 1. QP height is >= QP_alt height,
		// 2. There is one side where ( qp_* ) is true but qp_alt_* is not,
		// 3. and vice-versa.
		
		// check for bordering both QP and QP_ALT.
		if ( !( qp_left || qp_under || qp_right ) )
			return false ;
		if ( !( qp_alt_left || qp_alt_under || qp_alt_right ) )
			return false ;
		
		// check for a side where we border QP but not ALT, and vice-versa.
		if ( !( ( qp_left && !qp_alt_left ) || ( qp_under && !qp_alt_under ) || ( qp_right && !qp_alt_right) ) )
			return false ;
		if ( !( ( !qp_left && qp_alt_left ) || ( !qp_under && qp_alt_under ) || ( !qp_right && qp_alt_right) ) )
			return false ;
		
		// whelp, that's it.  This is a junction.
		return true ;
	}
	
	
	/**
	 * findBestCandidate calls this method to assign a priority to each valley.  Parameters are
	 * 'heights' and 'isJunction', arrays of the height of each column (as provided by
	 * the caller of findJunction, a ValleySystem subclass) and a boolean indication of 
	 * whether the column is junction.
	 * 
	 * Overriding this method, and changing the way the "heights" array is determined
	 * for findJunction (as well as the 'candidate' array), will significantly change
	 * the behavior of the ValleySystem.  ValleySystem.findJunction will select the
	 * highest-priority junction.
	 * 
	 * Junctions require at least some inconsistency on the part of the qPanes; we prioritize
	 * junctions that are EXTREMELY inconsistent.  The "best possible" junction would be one
	 * where a wall in one Qpane meets a corner in another, like this:
	 * 
	 * 			]
	 * 			] |
	 * 		 ___]_|		<- this spot
	 * 			]
	 * 
	 * Wherever possible, we prefer places where the QPanes have different heights, as dropping
	 * a non-separating 3d block into this location would alter height difference significantly.
	 * Likewise, we prefer those which would alter it most significantly.
	 * 
	 * In short: we determine priority according to the number of dissimilar "edges;" sides
	 * where one QPane borders but the other doesn't.  Corners are counted higher than
	 * valleys: A qpane bordering on both sides is an immediate downgrade,  but not to the
	 * extent that we prefer one with fewer dissimilar edges.
	 * 
	 * Ties are broken by the height difference between qPanes.
	 * 
	 * @param heights
	 * @param isValley
	 * @param qpane
	 * @param c
	 * @param valleyWidth
	 * @return
	 */
	protected int junctionPriority( boolean qPanesInteract, int [][] heights, boolean [][] isJunction, int qp, int c ) {
		if ( !isJunction[qp][c] )
			return Integer.MIN_VALUE ;
		
		int qp_alt = (qp + 1) % 2 ;
		int cols = heights[qp].length ;
		int height = Math.max( heights[qp][c], heights[qp_alt][c] ) ;
		
		
		boolean qp_left = c > 0 && heights[qp][c-1] > height ;
		boolean qp_under = heights[qp][c] > 0 && height >= heights[qp_alt][c] ;
		boolean qp_right = c < cols-1 && heights[qp][c+1] > height ;
		
		boolean qp_alt_left = c > 0 && heights[qp_alt][c-1] > height ;
		boolean qp_alt_under = heights[qp_alt][c] > 0 && heights[qp_alt][c] >= height ;
		boolean qp_alt_right = c < cols-1 && heights[qp_alt][c+1] > height ;
		
		
		// count dissimilarities.
		int priority = 1 ;
		if ( qp_left != qp_alt_left )
			priority++ ;
		if ( qp_under != qp_alt_under )
			priority++ ;
		if ( qp_right != qp_alt_right )
			priority++ ;
		
		// "valley?"  If the same qpane touches both sides, downgrade.
		priority *= 2 ;
		if ( ( qp_left && qp_right ) || ( qp_alt_left && qp_alt_right ) )
			priority-- ;
		
		// Height priority?  The previous gives us "categories."  Within each,
		// priority is given to a greater height difference between qPanes.
		// We assume there is no way the height difference could be greater than 1000.
		priority *= 1000 ;
		
		priority += qPanesInteract ? Math.abs( heights[qp][c] - heights[qp_alt][c] ) : 0 ;
		
		return priority ;
	}
	
	
	/**
	 * A 'peak' is a location where one QPane is higher than, or as high as,
	 * all the places around it.
	 * 
	 * @param heights
	 * @param qp
	 * @param c
	 * @return
	 */
	private boolean isPeak( boolean qPanesInteract, int [][] heights, int qp, int c ) {
		int qp_alt = (qp + 1) % 2 ;
		int cols = heights[qp].length ;
		int height = Math.max( heights[qp][c], heights[qp_alt][c] ) ;
		
		// We require that this QPane is higher than its counterpart.
		if ( qPanesInteract && height <= heights[qp_alt][c] )
			return false ;
		
		// peak if nothing is locally higher.
		if ( c > 0 && heights[qp][c-1] > height )
			return false ;
		
		if ( c < cols-1 && heights[qp][c+1] > height )
			return false ;
		
		if ( qPanesInteract && c > 0 && heights[qp_alt][c-1] > height )
			return false ;
		
		if ( qPanesInteract && c < cols-1 && heights[qp_alt][c+1] > height )
			return false ;
		
		// this is a peak
		return true ;
	}
	
	
	/**
	 * findBestCandidate calls this method to assign a priority to each valley.  Parameters are
	 * 'heights' and 'isPeak', arrays of the height of each column (as provided by
	 * the caller of findPeak, a ValleySystem subclass) and a boolean indication of 
	 * whether the column is junction.
	 * 
	 * Overriding this method, and changing the way the "heights" array is determined
	 * for findJunction (as well as the 'candidate' array), will significantly change
	 * the behavior of the ValleySystem.  ValleySystem.findPeak will select the
	 * highest-priority junction.
	 * 
	 * Peak priority is determined by the degree of separation from sides.  We count
	 * each relative height separately and just sum them.
	 * 
	 * @param heights
	 * @param isValley
	 * @param qpane
	 * @param c
	 * @param valleyWidth
	 * @return
	 */
	protected int peakPriority( boolean qPanesInteract, int [][] heights, boolean [][] isPeak, int qp, int c ) {
		if ( !isPeak[qp][c] )
			return Integer.MIN_VALUE ;
		
		int qp_alt = (qp + 1) % 2 ;
		int cols = heights[qp].length ;
		int height = Math.max( heights[qp][c], heights[qp_alt][c] ) ;
		
		int diffQ = qPanesInteract ? height - heights[qp_alt][c] : 0 ;
		int diffL = c > 0 ? height - heights[qp][c-1] : 0 ;
		int diffR = c < cols-1 ? height - heights[qp][c+1] : 0 ;
		int diffQL = c > 0 && qPanesInteract ? height - heights[qp_alt][c-1] : 0 ;
		int diffQR = c < cols-1 && qPanesInteract ? height - heights[qp_alt][c+1] : 0 ;
		
		return diffQ + diffL + diffR + diffQL + diffQR ;
	}
	
	
	/**
	 * A 'corner' is a location where a location is bordered on one side but
	 * not the other.  Walls do not count as 'borders.'
	 * 
	 * @param heights
	 * @param qp
	 * @param c
	 * @return
	 */
	private boolean isCorner( boolean qPanesInteract, int [][] heights, int qp, int c ) {
		int qp_alt = (qp + 1) % 2 ;
		int cols = heights[qp].length ;
		int height = Math.max( heights[qp][c], heights[qp_alt][c] ) ;
		
		// We require that this QPane is higher than its counterpart.
		if ( qPanesInteract && height <= heights[qp_alt][c] )
			return false ;
		
		boolean borderLeft = false ;
		boolean borderRight = false ;
		
		boolean wallLeft = c == 0 ;
		boolean wallRight = c == cols-1 ;
		
		if ( c > 0 && heights[qp][c-1] > height )
			borderLeft = true ;
		if ( c > 0 && ( qPanesInteract && heights[qp_alt][c-1] > height ) )
			borderLeft = true ;
		
		if ( c < cols-1 && heights[qp][c+1] > height )
			borderRight = true ;
		if ( c < cols-1 && ( qPanesInteract && heights[qp_alt][c+1] > height ) )
			borderRight = true ;
		
		// corner if border on one and not the other.
		return borderLeft != borderRight && !(wallLeft || wallRight) ;
	}
	
	
	/**
	 * The priority of this corner.
	 * 
	 * If qPanes don't interact, corner priority is basically 2 for standard
	 * corners, 1 for those which use the wall.
	 * 
	 * If the DO interact, then we count up each contributing side: 0 for a wall,
	 * 1 for one qPane present, 2 for both.
	 * 
	 * Note: it is up to isCorner to determine if walls count as corner sides.
	 * 
	 * @param heights
	 * @param isValley
	 * @param qpane
	 * @param c
	 * @param valleyWidth
	 * @return
	 */
	protected int cornerPriority( boolean qPanesInteract, int [][] heights, boolean [][] isCorner, int qp, int c ) {
		if ( !isCorner[qp][c] )
			return Integer.MIN_VALUE ;
		
		int qp_alt = (qp + 1) % 2 ;
		int cols = heights[qp].length ;
		int height = Math.max( heights[qp][c], heights[qp_alt][c] ) ;
		
		int priority = 0 ;
		
		// below
		priority += qPanesInteract && heights[qp_alt][c] == height ? 2 : 1 ;
		// left
		if ( c > 0 ) {
			priority += heights[qp][c-1] > height ? 1 : 0 ;
			priority += qPanesInteract && heights[qp_alt][c-1] > height ? 1 : 0 ;
		}
		// right
		if ( c < cols-1 ) {
			priority += heights[qp][c+1] > height ? 1 : 0 ;
			priority += qPanesInteract && heights[qp_alt][c+1] > height ? 1 : 0 ;
		}
		
		return priority ;
	}
	

}