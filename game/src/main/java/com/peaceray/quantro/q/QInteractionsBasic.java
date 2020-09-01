package com.peaceray.quantro.q;

import com.peaceray.quantro.exceptions.QOrientationConflictException;



public final class QInteractionsBasic implements QInteractions {
	
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
	public boolean allQOrientationsCollide() {
		return true ;
	}
	
	/**
	 * Returns 'true' if the 'mergeInto' method performs a simple
	 * value-copy; in other words, merging {i, 0} and {0, j} will
	 * always produce {i, j}, and a merge-collision {i, j} <-> {k, l}
	 * will always fail.
	 * 
	 * @return The above
	 */
	public boolean mergeIntoKeepsQOrientation() {
		return true ;
	}

	// We preset these interactions for easy usage
	private static boolean [][] locksToFromAboveArray ;
	//private static boolean [][] locksToFromBelowArray ;
	//private static boolean [][] locksToFromSideArray ;
	//private static boolean [][] locksToFromQuantumArray ;
	//private static boolean [] locksToWallArray ;
	
	private static boolean [][] separatesFromWhenAboveArray ;
	private static boolean [][] separatesFromWhenBelowArray ;
	private static boolean [][] separatesFromWhenSideArray ;
	private static boolean [][] separatesFromWhenQuantumArray ;
	//private static boolean [][] separatesFromWallArray ;
	
	private static boolean [][] collidesArray ;
	
	private static boolean [] activatesArray ;
	private static byte [] activateArray ;
	private static boolean [] deactivatesArray ;
	private static byte [] deactivateArray ;
	
	private static byte [] pushFlipArray ;
	private static int [] flipArray ;
	
	
	private static boolean [][][] occludedByArray ;
	
	
	static {
		// Initialize everything.  Lock to each other?  Collide with each other?
		locksToFromAboveArray = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		collidesArray = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		// Because of the 2-pane design simplicity, everything collides with everything (but NO).
		// as S0 lives entirely in [0] and S1 lives entirely in [1].
		for ( int i = 0; i < QOrientations.NUM; i++ ) {
			for ( int j = 0; j < QOrientations.NUM; j++ ) {
				// set locksToFromAbove, collides.  As mentioned, anything living
				// in the same pane is assumed to collide.
				
				// NOTE: think long and hard before changing collidesArray (i.e.,
				// the assumption that ALL non-NO QOs collide if in the same space).
				// We rely on this assumption for a simple implementation of
				// 'spaceBelow' in the EarlyCollisionSystem (correct behavior will still
				// occur if this changes, but the efficient code path will not be used).
				collidesArray[i][j] = (i != 0) && (j != 0) ;
				locksToFromAboveArray[i][j] = collidesArray[i][j] ;
			}
		}
		
		// Separates from is the more complicated one.  Behavior is as follows:
		// certain pieces stay locked together when touching.  Some don't.
		// Unstable pieces, for example, will unlock from anything.  Start by
		// setting all separatesFromWhen... to false (except for NO), the set special
		// cases.
		separatesFromWhenAboveArray = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		separatesFromWhenBelowArray = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		separatesFromWhenSideArray = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		separatesFromWhenQuantumArray = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		for ( int i = 0; i < QOrientations.NUM; i++ ) {
			for ( int j = 0; j < QOrientations.NUM; j++ ) {
				// set locksToFromAbove, collides.  As mentioned, anything living
				// in the same pane is assumed to collide.
				separatesFromWhenAboveArray[i][j] = (i == 0) || (j == 0) ;
				separatesFromWhenBelowArray[i][j] = separatesFromWhenAboveArray[i][j] ;
				separatesFromWhenSideArray[i][j] = separatesFromWhenAboveArray[i][j] ;
			}
		}
		// Special cases: Unstable separates from everything, everything separates from it.
		for ( int i = 0; i < QOrientations.NUM; i++ ) {
			separatesFromWhenAboveArray[QOrientations.U0][i] = true ;
			separatesFromWhenAboveArray[QOrientations.U1][i] = true ;
			separatesFromWhenAboveArray[QOrientations.UL][i] = true ;
			
			separatesFromWhenBelowArray[QOrientations.U0][i] = true ;
			separatesFromWhenBelowArray[QOrientations.U1][i] = true ;
			separatesFromWhenBelowArray[QOrientations.UL][i] = true ;
			
			separatesFromWhenSideArray[QOrientations.U0][i] = true ;
			separatesFromWhenSideArray[QOrientations.U1][i] = true ;
			separatesFromWhenSideArray[QOrientations.UL][i] = true ;
			
			separatesFromWhenAboveArray[i][QOrientations.U0] = true ;
			separatesFromWhenAboveArray[i][QOrientations.U1] = true ;
			separatesFromWhenAboveArray[i][QOrientations.UL] = true ;
			
			separatesFromWhenBelowArray[i][QOrientations.U0] = true ;
			separatesFromWhenBelowArray[i][QOrientations.U1] = true ;
			separatesFromWhenBelowArray[i][QOrientations.UL] = true ;
			
			separatesFromWhenSideArray[i][QOrientations.U0] = true ;
			separatesFromWhenSideArray[i][QOrientations.U1] = true ;
			separatesFromWhenSideArray[i][QOrientations.UL] = true ;
			
			
		}
		
		// Another special case!  Although there are no quantum locks (yet),
		// we should note that quantum separation happens in most - but not all - cases.
		for ( int i = 0; i < QOrientations.NUM; i++ ) {
			for ( int j = 0; j < QOrientations.NUM; j++ ) {
				separatesFromWhenQuantumArray[i][j] = true ;
			}
		}
		separatesFromWhenQuantumArray[QOrientations.SL][QOrientations.SL] = false ;
		separatesFromWhenQuantumArray[QOrientations.SL_ACTIVE][QOrientations.SL_ACTIVE] = false ;
		separatesFromWhenQuantumArray[QOrientations.SL_INACTIVE][QOrientations.SL_INACTIVE] = false ;
		
		separatesFromWhenQuantumArray[QOrientations.UL][QOrientations.UL] = false ;
		separatesFromWhenQuantumArray[QOrientations.ST][QOrientations.ST] = false ; 
		separatesFromWhenQuantumArray[QOrientations.ST_INACTIVE][QOrientations.ST_INACTIVE] = false ; 
		
		
		
		// Metamorphosis!
		activatesArray = new boolean[QOrientations.NUM] ;
		activateArray = new byte[QOrientations.NUM] ;
		for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
			activatesArray[qo] = false ;
			activateArray[qo] = qo ;
		}
		// SL can activate.  Activating also changes those attached to SL.
		activatesArray[QOrientations.SL] = true ;
		activateArray[QOrientations.SL] = QOrientations.SL_ACTIVE ;
		activatesArray[QOrientations.S0_FROM_SL] = true ;
		activateArray[QOrientations.S0_FROM_SL] = QOrientations.S0 ;
		activatesArray[QOrientations.S1_FROM_SL] = true ;
		activateArray[QOrientations.S1_FROM_SL] = QOrientations.S1 ;
		activatesArray[QOrientations.PUSH_DOWN] = true ;
		activateArray[QOrientations.PUSH_DOWN]= QOrientations.PUSH_DOWN_ACTIVE ; 
		
		deactivatesArray = new boolean[QOrientations.NUM] ;
		deactivateArray = new byte[QOrientations.NUM] ;
		for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
			deactivatesArray[qo] = false ;
			deactivateArray[qo] = qo ;
		}
		// SL and ST can become inactive.
		deactivatesArray[QOrientations.SL] = true ;
		deactivateArray[QOrientations.SL] = QOrientations.SL_INACTIVE ;
		deactivatesArray[QOrientations.ST] = true ;
		deactivateArray[QOrientations.ST] = QOrientations.ST_INACTIVE ;
		deactivatesArray[QOrientations.S0_FROM_SL] = true ;
		deactivateArray[QOrientations.S0_FROM_SL] = QOrientations.S0 ;
		deactivatesArray[QOrientations.S1_FROM_SL] = true ;
		deactivateArray[QOrientations.S1_FROM_SL] = QOrientations.S1 ;
		
		pushFlipArray = new byte[QOrientations.NUM];
		for ( byte qo = 0; qo < QOrientations.NUM; qo++ )
			pushFlipArray[qo] = qo ;
		pushFlipArray[QOrientations.PUSH_DOWN_ACTIVE] = QOrientations.PUSH_UP_ACTIVE ;
		pushFlipArray[QOrientations.PUSH_UP_ACTIVE] = QOrientations.PUSH_DOWN_ACTIVE ;
		
		
		flipArray = new int[QCombinations.NUM] ;
		// by default, flip to the same.
		for ( byte qc = 0; qc < QCombinations.NUM; qc++ )
			flipArray[qc] = qc ;
		// all single-pane elements flip to the other pane
		flipArray[QCombinations.S0] = QCombinations.S1 ;
		flipArray[QCombinations.S1] = QCombinations.S0 ;
		flipArray[QCombinations.F0] = QCombinations.F1 ;
		flipArray[QCombinations.F1] = QCombinations.F0 ;
		flipArray[QCombinations.U0] = QCombinations.U1 ;
		flipArray[QCombinations.U1] = QCombinations.U0 ;
		flipArray[QCombinations.S0_FROM_SL] = QCombinations.S1_FROM_SL ;
		flipArray[QCombinations.S1_FROM_SL] = QCombinations.S0_FROM_SL ;
		// mismatched QOrientations get flipped as well.
		flipArray[QCombinations.SF] = QCombinations.FS ;
		flipArray[QCombinations.FS] = QCombinations.SF ;
		flipArray[QCombinations.SU] = QCombinations.US ;
		flipArray[QCombinations.US] = QCombinations.SU ;
		flipArray[QCombinations.FU] = QCombinations.UF ;
		flipArray[QCombinations.UF] = QCombinations.FU ;
		// As above, but replacing S with Sfromsl
		flipArray[QCombinations.SfromslS] = QCombinations.SSfromsl ;
		flipArray[QCombinations.SSfromsl] = QCombinations.SfromslS ;
		flipArray[QCombinations.SfromslF] = QCombinations.FSfromsl ;
		flipArray[QCombinations.FSfromsl] = QCombinations.SfromslF ;
		flipArray[QCombinations.SfromslU] = QCombinations.USfromsl ;
		flipArray[QCombinations.USfromsl] = QCombinations.SfromslU ;
		
		// Occluded by!  Note the assumption of occludedBy: the
		// occluding qslice is "connected" and will not separate.
		
		// A is occluded by B if B has an entry in the orientation(s)
		// in which A occurs.
		byte [] A = new byte[2] ;
		byte [] B = new byte[2] ;
		occludedByArray = new boolean[QOrientations.NUM][QOrientations.NUM][QOrientations.NUM] ; 
		for ( int qcB = 0; qcB < QCombinations.NUM; qcB++ ) {
			// setAs B <- the combination
			QCombinations.setAs(B, qcB) ;
			for ( int qoA = 0; qoA < QOrientations.NUM; qoA++ ) {
				// setAs A.  This is as a convenience so we can get empty columns
				// that are easily checked - i.e., we will look specifically for NO
				// entries when checking occlusion, rather than enumerate all QOrientation
				// possibilities.
				QCombinations.setAs(A, qoA) ;
				// Check occlusion.  If B fills everything, then A is occluded
				// no matter what it is.  Otherwise, check that A is also empty
				// in the q panes where B is empty.
				if ( B[0] != QOrientations.NO && B[1] != QOrientations.NO ) {
					occludedByArray[qoA][B[0]][B[1]] = true ;
				}
				else if ( (A[0] == QOrientations.NO || B[0] != QOrientations.NO )
						&& (A[1] == QOrientations.NO || B[1] != QOrientations.NO ) ) {
					// The non-empty panes of A are occluded by a non-empty pane of B.
					occludedByArray[qoA][B[0]][B[1]] = true ;
				}
				else {
					occludedByArray[qoA][B[0]][B[1]] = false ;
				}
			}
		}
	}
	
	
	
	// A helper!
	/**
	 * Returns the disjunction of the values in the specified boolean array, where
	 * qo1 and qo2 are arrays of co-indices.
	 * 
	 * @return OR[  array[qo1[i]][qo2[i]]  ]_i
	 */
	private boolean arDisjunction( boolean [][] array, byte [] qo1, byte [] qo2 ) {
		boolean disj = false ;
		for ( int i = 0; i < 2; i++ ) {
			disj = disj || array[qo1[i]][qo2[i]] ;
		}
		return disj ;
	}
	
	/**
	 * Returns the disjunction of the values in the specified boolean array, where
	 * qo1[*][row1][col1] and qo2[*][row2][col2] are aligned arrays of co-indices.
	 * 
	 * @param array
	 * @param qo1
	 * @param row1
	 * @param col1
	 * @param qo2
	 * @param row2
	 * @param col2
	 * @return
	 */
	private boolean arDisjunction( boolean [][] array,
			byte [][][] qo1, int row1, int col1,
			byte [][][] qo2, int row2, int col2 ) {
		boolean disj = false ;
		for ( int i = 0; i < 2; i++ ) {
			disj = disj || array[qo1[i][row1][col1]][qo2[i][row2][col2]] ;
		}
		return disj ;
	}
	
	// Locking to stuff
	public boolean locksToFromAbove( byte qo1, byte qo2 ) {return locksToFromAboveArray[qo1][qo2];}
	public boolean locksToFromBelow( byte qo1, byte qo2 ) {return false;}
	public boolean locksToFromSide( byte qo1, byte qo2 ) {return false;}
	public boolean locksToFromQuantum( byte qo1, byte qo2 ) {return false;}
	public boolean locksToWall( byte qo1 ) {return false;}
	public boolean locksToFloor( byte qo1 ) {return qo1 != QOrientations.NO;}
	// Same as above, but check sets of blocks instead of just 1.
	public boolean locksToFromAbove( byte [] ar1, byte [] ar2 ) {return arDisjunction(locksToFromAboveArray,ar1,ar2);}
	public boolean locksToFromBelow( byte [] ar1, byte [] ar2 ) {return false;}
	public boolean locksToFromSide( byte [] ar1, byte [] ar2 ) {return false;}
	public boolean locksToWall( byte [] ar ) {return false;}
	public boolean locksToFloor( byte [] ar ) {
		for ( int i = 0; i < 2; i++ ) {
			if ( ar[i] != QOrientations.NO ) {
				return true ;
			}
		}
		return false ;
	}
	
	// Same as above, except checks within a block field.
	public boolean locksToFromAbove( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) 
			{return arDisjunction(locksToFromAboveArray,ar1,row1,col1,ar2,row2,col2);}
	public boolean locksToFromBelow( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) 
			{return false;}
	public boolean locksToFromSide( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) 
			{return false;}
	// Does it lock to the wall?
	public boolean locksToWall( byte [][][] ar, int row, int col ) 
			{return false;}
	public boolean locksToFloor( byte [][][] ar, int row, int col ) {
		for ( int i = 0; i < 2; i++ ) {
			if ( ar[i][row][col] != QOrientations.NO ) {
				return true ;
			}
		}
		return false ;
	}
	
	
	
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
	public boolean separatesFromWhenAbove( byte qo1, byte qo2 ) {return separatesFromWhenAboveArray[qo1][qo2];}
	public boolean separatesFromWhenBelow( byte qo1, byte qo2 ) {return separatesFromWhenBelowArray[qo1][qo2];}
	public boolean separatesFromWhenSide( byte qo1, byte qo2 ) {return separatesFromWhenSideArray[qo1][qo2];}
	// All sides does NOT include Quantum.  Returns 'true' only if there
	// is NOTHING to which this block will stick (except the floor).
	public boolean separatesFromAllSides( byte qo1 )
			{return qo1 == QOrientations.U0 || qo1 == QOrientations.U1 || qo1 == QOrientations.UL ;}
	public boolean separatesFromWhenQuantum( byte qo1, byte qo2 ) {return separatesFromWhenQuantumArray[qo1][qo2];}
	// Does it separate from the wall?
	public boolean separatesFromWall( byte qo1 ) {return true;}
	public boolean separatesFromFloor( byte qo1 ) {return false;}
	
	
	
	///////////////////////////////////////////////////////////////////
	// DO BLOCKS COLLIDE?
	//
	// Blocks "collide" with each other if they attempt to occupy the same space
	// at the same time.  Check for that condition before moving blocks.
	//
	// TODO: To optimize, check assumption that collision ==
	//		(qo1 != NO) && (qo2 != NO) and in exact same location.  Then inline the check.
	//
	public boolean collides( byte qo1, byte qo2 ) {return collidesArray[qo1][qo2];}
	public boolean collides( byte [] ar1, byte [] ar2 ) {return arDisjunction(collidesArray, ar1, ar2);}
	public boolean collides( byte [][][] ar1, int row1, int col1, byte [][][] ar2, int row2, int col2 ) 
			{return arDisjunction(collidesArray,ar1,row1,col1,ar2,row2,col2);}
	
	
	
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
	public boolean activates( byte qo ) { return activatesArray[qo] ; }
	public boolean activates( byte [][][] ar, int qPane, int row, int col ) { return activatesArray[ ar[qPane][row][col] ] ; }
	public void activate( byte [][][] ar, int qPane, int row, int col )  { ar[qPane][row][col] = activateArray[ ar[qPane][row][col] ] ; }
	public boolean deactivates( byte qo ) { return deactivatesArray[qo] ; }
	public boolean deactivates( byte [][][] ar, int qPane, int row, int col ) { return deactivatesArray[ ar[qPane][row][col] ] ; }
	public void deactivate( byte [][][] ar, int qPane, int row, int col )  { ar[qPane][row][col] = deactivateArray[ ar[qPane][row][col] ] ; }
	
	
	
	///////////////////////////////////////////////////////////////////
	// PUSH FLIPS
	//
	// Certain blocks change QOrientation ("flip") when pushed to the
	// opponent.
	//
	public byte pushFlip( byte qo ) { return pushFlipArray[qo] ; }
	
	
	
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
	public int flip( int qc ) { return flipArray[qc] ; }
	public void flip( byte [][][] ar, int row, int col ) { QCombinations.setAs( ar, row, col , flipArray[ QCombinations.encode(ar, row, col) ] ) ; }
	
	
	///////////////////////////////////////////////////////////////////
	// COMBINING BLOCKS
	//
	// Blocks may need to be merged together, without one overwriting 
	// the other.  For instance, locking an S0 piece where there is
	// already S1, the two should "merge" into what is conceptually SS
	// (but actually represented as {S0,S1}.
	//
	public void mergeInto( byte [][][] src, int rowSrc, int colSrc, byte [][][] dest, int destRow, int destCol ) throws QOrientationConflictException {
		
		// In this system, one can only "merge" where there is empty space.
		for ( int q = 0; q < 2; q++ ) {
			byte qo1 = src[q][rowSrc][colSrc] ;
			if ( dest[q][destRow][destCol] == QOrientations.NO )
				dest[q][destRow][destCol] = qo1 ;
			else if ( qo1 != QOrientations.NO ){
				// CONFLICT!  Both are none-NO!  No such merging allowed!
				throw new QOrientationConflictException( qo1,
						dest[q][destRow][destCol],
						"QInteractionBasic.mergeInto: cannot merge none-NO blocks together: " + qo1 + " into " + dest[q][destRow][destCol] ) ;
			}
		}
	}
	
	
	
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
	public boolean occludedBy( byte qo, byte [] ar ) {return occludedByArray[qo][ar[0]][ar[1]];}
	public boolean occludedBy( byte qo, byte [][][] ar, int row, int col )
			{return occludedByArray[qo][ar[0][row][col]][ar[1][row][col]];}
	
	
	
	///////////////////////////////////////////////////////////////////
	// CLEARED BY CERTAIN CONDITIONS?
	//
	// We assume there are 3 valid "styles" of clear: S0, S1, SL.
	// 
	// For now, anything that occurs in pane 0 contributes to an S0
	// clear, and same for pane 1.  A block with both panes full contributes
	// to S0, S1, and SL.
	//
	// TODO: Change these functions to allow more complex block types: for instance,
	// "glass" blocks that fill space but cannot be cleared and do not contribute
	// to clears, "obsidian" blocks that require SL clearing (i.e. do not contribute
	// to S0 or S1 clears and are not altered by them), etc.
	public boolean contributesToClear( byte [] ar )
			{return ar[0] != QOrientations.NO || ar[1] != QOrientations.NO ;}
	public boolean contributesToClear( byte [] ar, int qoClear ) {
		if ( qoClear == QOrientations.S0 ) {
			return ar[0] != QOrientations.NO ;
		}
		if ( qoClear == QOrientations.S1 ) {
			return ar[1] != QOrientations.NO ;
		}
		if ( qoClear == QOrientations.SL ) {
			return ar[0] != QOrientations.NO && ar[1] != QOrientations.NO ;
		}
		return false ;
	}
	public boolean contributesToClear( byte [][][] ar, int row, int col )
			{return ar[0][row][col] != QOrientations.NO || ar[1][row][col] != QOrientations.NO ;}
	public boolean contributesToClear( byte [][][] ar, int row, int col, int qoClear )  {
		if ( qoClear == QOrientations.S0 ) {
			return ar[0][row][col] != QOrientations.NO ;
		}
		if ( qoClear == QOrientations.S1 ) {
			return ar[1][row][col] != QOrientations.NO ;
		}
		if ( qoClear == QOrientations.SL ) {
			return ar[0][row][col] != QOrientations.NO && ar[1][row][col] != QOrientations.NO ;
		}
		return false ;
	}
	
	// Perform the clear (block level)
	public void clearWith( byte [] ar, int qoClear ) {
		if ( qoClear == QOrientations.S0 ) {
			// Sometimes this has an effect on q-1.
			if ( ar[0] == QOrientations.SL || ar[0] == QOrientations.SL_ACTIVE || ar[0] == QOrientations.SL_INACTIVE )
				ar[1] = QOrientations.S1 ;
			if ( ar[0] == QOrientations.ST || ar[0] == QOrientations.ST_INACTIVE )
				ar[1] = QOrientations.NO ;
			if ( ar[0] == QOrientations.UL )
				ar[1] = QOrientations.U1 ;
			ar[0] = QOrientations.NO ;
		}
		else if ( qoClear == QOrientations.S1 ) {
			// Sometimes this has an effect on q-0.
			if ( ar[1] == QOrientations.SL || ar[0] == QOrientations.SL_ACTIVE || ar[0] == QOrientations.SL_INACTIVE )
				ar[0] = QOrientations.S0 ;
			if ( ar[1] == QOrientations.ST || ar[0] == QOrientations.ST_INACTIVE )
				ar[0] = QOrientations.NO ;
			if ( ar[1] == QOrientations.UL )
				ar[0] = QOrientations.U0 ;
			ar[1] = QOrientations.NO ;
		}
		else if ( qoClear == QOrientations.SL ) {
			ar[0] = ar[1] = QOrientations.NO ;
		}
	}
	public void clearWith( byte [][][] ar, int row, int col, int qoClear ) {
		if ( qoClear == QOrientations.S0 ) {
			// Sometimes this has an effect on q-1.
			if ( ar[0][row][col] == QOrientations.SL || ar[0][row][col] == QOrientations.SL_ACTIVE || ar[0][row][col] == QOrientations.SL_INACTIVE )
				ar[1][row][col] = QOrientations.S1 ;
			if ( ar[0][row][col] == QOrientations.ST || ar[0][row][col] == QOrientations.ST_INACTIVE )
				ar[1][row][col] = QOrientations.NO ;
			if ( ar[0][row][col] == QOrientations.UL )
				ar[1][row][col] = QOrientations.U1 ;
			ar[0][row][col] = QOrientations.NO ;
		}
		else if ( qoClear == QOrientations.S1 ) {
			// Sometimes this has an effect on q-0.
			if ( ar[1][row][col] == QOrientations.SL || ar[0][row][col] == QOrientations.SL_ACTIVE || ar[0][row][col] == QOrientations.SL_INACTIVE )
				ar[0][row][col] = QOrientations.S0 ;
			if ( ar[1][row][col] == QOrientations.ST || ar[0][row][col] == QOrientations.ST_INACTIVE )
				ar[0][row][col] = QOrientations.NO ;
			if ( ar[1][row][col] == QOrientations.UL )
				ar[0][row][col] = QOrientations.U0 ;
			ar[1][row][col] = QOrientations.NO ;
		}
		else if ( qoClear == QOrientations.SL ) {
			ar[0][row][col] = ar[1][row][col] = QOrientations.NO ;
		}
	}
	
	
	public void inverseClearWith( byte [] ar, int qoClear ) {
		// By default, this clears pane 0 (so we KEEP pane 0
		// in the provided array, as we are doing an inverse clear).
		// however, it also occasionally has an effect on pane 1,
		// and sometimes 
		if ( qoClear == QOrientations.S0 ) {
			// Sometimes this has an effect on S1.
			if ( ar[0] == QOrientations.ST )
				ar[1] = QOrientations.ST ;		// pane 1 IS cleared!
			else if ( ar[0] == QOrientations.ST_INACTIVE )
				ar[1] = QOrientations.ST_INACTIVE ;
			else
				ar[1] = QOrientations.NO ;
			
			// certain blocks are "cleared" as something else
			if ( ar[0] == QOrientations.UL )
				ar[0] = QOrientations.U0 ;
			else if ( ar[0] == QOrientations.SL || ar[0] == QOrientations.SL_ACTIVE || ar[0] == QOrientations.SL_INACTIVE )
				ar[0] = QOrientations.S0 ;
		}
		else if ( qoClear == QOrientations.S1 ) {
			if ( ar[1] == QOrientations.ST )
				ar[0] = QOrientations.ST ;		// pane 0 IS cleared!
			else if ( ar[1] == QOrientations.ST_INACTIVE )
				ar[0] = QOrientations.ST ;		// pane 0 IS cleared!
			else
				ar[0] = QOrientations.NO ;
			
			// certain blocks are "cleared" as something else
			if ( ar[1] == QOrientations.UL )
				ar[1] = QOrientations.U1 ;
			else if ( ar[1] == QOrientations.SL || ar[1] == QOrientations.SL_ACTIVE || ar[1] == QOrientations.SL_INACTIVE )
				ar[1] = QOrientations.S1 ;
		}
		else if ( qoClear == QOrientations.NO ) {
			ar[0] = ar[1] = QOrientations.NO ;
		}
	}
	
	
	public void inverseClearWith( byte [][][] ar, int row, int col, int qoClear ) {
		// By default, this clears pane 0 (so we KEEP pane 0
		// in the provided array, as we are doing an inverse clear).
		// however, it also occasionally has an effect on pane 1.
		if ( qoClear == QOrientations.S0 ) {
			// Sometimes this has an effect on S1.
			if ( ar[0][row][col] == QOrientations.ST )
				ar[1][row][col] = QOrientations.ST ;		// pane 1 IS cleared!
			else if ( ar[0][row][col] == QOrientations.ST_INACTIVE )
				ar[1][row][col] = QOrientations.ST_INACTIVE ;		// pane 1 IS cleared!
			else
				ar[1][row][col] = QOrientations.NO ;
			
			// certain blocks are "cleared" as something else
			if ( ar[0][row][col] == QOrientations.UL )
				ar[0][row][col] = QOrientations.U0 ;
			else if ( ar[0][row][col] == QOrientations.SL || ar[0][row][col] == QOrientations.SL_ACTIVE || ar[0][row][col] == QOrientations.SL_INACTIVE )
				ar[0][row][col] = QOrientations.S0 ;
		}
		else if ( qoClear == QOrientations.S1 ) {
			if ( ar[1][row][col] == QOrientations.ST )
				ar[0][row][col] = QOrientations.ST ;		// pane 0 IS cleared!
			else if ( ar[1][row][col] == QOrientations.ST_INACTIVE )
				ar[0][row][col] = QOrientations.ST_INACTIVE ;
			else
				ar[0][row][col] = QOrientations.NO ;
			
			// certain blocks are "cleared" as something else
			if ( ar[1][row][col] == QOrientations.UL )
				ar[1][row][col] = QOrientations.U1 ;
			else if ( ar[1][row][col] == QOrientations.SL || ar[1][row][col] == QOrientations.SL_ACTIVE || ar[1][row][col] == QOrientations.SL_INACTIVE )
				ar[1][row][col] = QOrientations.S1 ;
		}
		else if ( qoClear == QOrientations.NO ) {
			ar[0][row][col] = ar[1][row][col] = QOrientations.NO ;
		}
	}
	
}
