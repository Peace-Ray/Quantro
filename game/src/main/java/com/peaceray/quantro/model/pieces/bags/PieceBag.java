package com.peaceray.quantro.model.pieces.bags;

import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QOrientations;

/**
 * A PieceBag is a means of generating piece types according
 * to some rule.  Different implementations may follow different
 * rules - for instance, one might generate only S0 tetrominos
 * uniformly at random.  Another may generate the same set
 * of pieces, but ensure that all 7 tetromino categories
 * appear before any repeat.  A very different PieceBag implementation
 * might only generate special pieces according to some
 * other distribution.
 * 
 * Logically, PieceBags are assumed to have an 'infinite supply'
 * of pieces; pop() will ALWAYS provide a valid piece.  If
 * you want to limit the number of available pieces, use a different
 * class.
 * 
 * PieceBags are encouraged to support pushing, which behaves as
 * a Stack implementation would - i.e., if you 'peeked' at a piece
 * and then pushed on top of it, the piece will be popped in the
 * expected order.
 * 
 * Peeking is encouraged, but like 'push', is not necessary.
 * canPeek( i ) should indicate whether peek(i) will return a
 * valid piece type.  lookahead of 0 indicates the next piece
 * to be popped, lookahead of 2 indicates pop,pop,POP, etc.
 * 
 * 
 * @author Jake
 *
 */
public abstract class PieceBag implements SerializableState {
	
	protected static final int [] FREE_TROMINO_TYPES = new int[]{
		PieceCatalog.encodeTromino( PieceCatalog.TRO_CAT_LINE, QOrientations.R5),			// "purple" dash, contrasts with "red" line tetromino
		PieceCatalog.encodeTromino( PieceCatalog.TRO_CAT_V, QOrientations.R1) 				// orange caret
	} ;
	protected static final int [][] FREE_TROMINO_UNIQUE_ROTATIONS = 
		new int[][] { 
				new int[] {0, 1},
				new int[] {0, 3} } ;
	// unique rotations: assumes that flips are VERTICAL.
	
	
	protected static final int [] FREE_TETROMINO_TYPES = new int[]{
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_LINE, QOrientations.R0 ),		// "red" line
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_GUN, QOrientations.R2 ),		// "yellow-gold" gun (7)
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_SQUARE, QOrientations.R3 ),	// "green" square
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_T, QOrientations.R6 ),			// pink T
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_Z, QOrientations.R4 )			// blue Z
	} ;
	protected static final int [][] FREE_TETROMINO_UNIQUE_ROTATIONS = 
		new int[][] {
				new int[] {0, 1},
				new int[] {0, 1, 2, 3},
				new int[] {0},
				new int[] {0, 1, 3},
				new int[] {0, 1} } ;
	// unique rotations: assumes that flips are VERTICAL.
	
	
	protected static final int [] FREE_PENTOMINO_TYPES = new int[]{
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_F, QOrientations.R2 ),  				// yellow
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_I, QOrientations.R4 ),					// blue; contrast dash, line.  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_L, QOrientations.R6 ),  				// pink; contrast blue caret, purple gamma, yellow gun
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_N, QOrientations.R3 ), 				// green.  contrast zig-zag, gamma/gun 
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_P, QOrientations.R1 ),  				// orange
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_T, QOrientations.R1 ),					// orange  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_U, QOrientations.R2 ),					// yellow  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_V, QOrientations.R3 ),					// green; contrast caret, gamma, gun, L, Reverse-L. 
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_W, QOrientations.R6 ),  				// pink steps.
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_X, QOrientations.R0 ),  				// red cross
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_Y, QOrientations.R5 ),  				// purple
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_Z, QOrientations.R0 )  				// red
	} ;
	protected static final int [][] FREE_PENTOMINO_UNIQUE_ROTATIONS = 
		new int[][] {
				new int[] {0, 1, 2, 3},		// F.  Flips reverse.
				new int[] {0, 1},			// I.
				new int[] {0, 1, 2, 3},		// L.  Flips reverse.
				new int[] {0, 1, 2, 3},		// N.  Flips reverse.
				new int[] {0, 1, 2, 3},		// P.  Flips reverse.
				new int[] {0, 1, 3},		// T.  Flips 0<->2
				new int[] {0, 1, 3},		// U.  Flips 0<->2
				new int[] {0, 3},			// V.  Flips 0<->1, 2<->3.
				new int[] {0, 3},			// W.  Flips 0<->1, 2<->3.
				new int[] {0},				// X.  Same all directions.
				new int[] {0, 1, 2, 3},		// Y
				new int[] {0, 1} } ;		// Z
	// unique rotations: assumes that flips are VERTICAL.
	
	
	// ONE SIDED polyominoes are those which cannot be "turned over."
	protected static final int [] ONE_SIDED_TROMINO_TYPES = new int[]{
		PieceCatalog.encodeTromino( PieceCatalog.TRO_CAT_LINE, QOrientations.R5),			// "purple" dash, contrasts with "red" line tetromino
		PieceCatalog.encodeTromino( PieceCatalog.TRO_CAT_V, QOrientations.R1) 				// orange caret, != Gamma, Gun, Pento-V, etc.
	} ;
	protected static final int [] ONE_SIDED_TROMINO_UNIQUE_ROTATIONS = 
		new int[] { 2, 4 } ;
	
	
	protected static final int [] ONE_SIDED_TETROMINO_TYPES = new int[]{
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_LINE, QOrientations.R0 ),		// "red" line
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_GAMMA, QOrientations.R5 ),		// "purple" gamma
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_GUN, QOrientations.R2 ),		// "yellow-gold" gun (7)
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_SQUARE, QOrientations.R3 ),	// "green" square
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_S, QOrientations.R1 ),			// "fiery red" S
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_T, QOrientations.R6 ),			// pink T
		PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_Z, QOrientations.R4 )			// blue Z
	} ;
	protected static final int [] ONE_SIDED_TETROMINO_UNIQUE_ROTATIONS = 
		new int[] { 2, 4, 4, 1, 2, 4, 2 } ;
	
	protected static final int [] ONE_SIDED_PENTOMINO_TYPES = new int[]{
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_F, QOrientations.R2 ),  				// yellow
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_I, QOrientations.R4 ),					// blue; contrast dash, line.  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_L, QOrientations.R6 ),  				// pink; contrast blue caret, purple gamma, yellow gun
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_N, QOrientations.R3 ), 				// green.  contrast zig-zag, gamma/gun 
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_P, QOrientations.R1 ),  				// orange
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_T, QOrientations.R1 ),					// orange  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_U, QOrientations.R2 ),					// yellow  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_V, QOrientations.R3 ),					// green; contrast caret, gamma, gun, L, Reverse-L. 
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_W, QOrientations.R6 ),  				// pink steps.
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_X, QOrientations.R0 ),  				// red cross
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_Y, QOrientations.R5 ),  				// purple
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_Z, QOrientations.R0 ),  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_F_REVERSE, QOrientations.R5 ), 		// purple
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_L_REVERSE, QOrientations.R1 ),  		// orange
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_N_REVERSE, QOrientations.R0 ),			// red.  contrast N, zig-zag, gamma/gun  
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_P_REVERSE, QOrientations.R4 ),  		// blue
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_Y_REVERSE, QOrientations.R2 ),  		// yellow
		PieceCatalog.encodePentomino( PieceCatalog.PENTO_CAT_Z_REVERSE, QOrientations.R3 )
	} ;
	protected static final int [] ONE_SIDED_PENTOMINO_UNIQUE_ROTATIONS = 
		new int[] { 4, 2,	// I: horiz, vert
					4, 4, 4, 4, 4, 4,	4, 1,	// X: only 1!
					4, 2,	// Z: only 2
					4, 4, 4, 4, 4, 2	// Z_REVERSE: only 2
					} ;
	
	/**
	 * Returns an array of all tromino types needed to fully articulate all
	 * available options, given that players have the specified movements available.
	 * 
	 * Types are encoded with a 'recommended' qorientation (suitable for Retro) that
	 * can be easily removed and replaced with whatever qorientation you want.
	 * 
	 * The value returned is [NUM][2], where [i][0] is an encoded piece type,
	 * and [i][1] the default rotation.
	 * 
	 * @param hasRotation
	 * @param hasReflection
	 * @return
	 */
	protected static final int [][] getAllTrominoTypesAndRotations( boolean hasRotation, boolean hasReflection ) {
		int [] types = hasReflection ? FREE_TROMINO_TYPES : ONE_SIDED_TROMINO_TYPES ;
		int [] numRotations = hasReflection ? null : ONE_SIDED_TROMINO_UNIQUE_ROTATIONS ;
		int [][] rotations = hasReflection ? FREE_TROMINO_UNIQUE_ROTATIONS : null ;
		return getAllTypesAndRotations( types, numRotations, rotations, hasRotation ) ;
	}
	
	/**
	 * Returns an array of all tromino types needed to fully articulate all
	 * available options, given that players have the specified movements available.
	 * 
	 * Types are encoded with a 'recommended' qorientation (suitable for Retro) that
	 * can be easily removed and replaced with whatever qorientation you want.
	 * 
	 * The value returned is [NUM][2], where [i][0] is an encoded piece type,
	 * and [i][1] the default rotation.
	 * 
	 * @param hasRotation
	 * @param hasReflection
	 * @return
	 */
	protected static final int [][] getAllTetrominoTypesAndRotations( boolean hasRotation, boolean hasReflection ) {
		int [] types = hasReflection ? FREE_TETROMINO_TYPES : ONE_SIDED_TETROMINO_TYPES ;
		int [] numRotations = hasReflection ? null : ONE_SIDED_TETROMINO_UNIQUE_ROTATIONS ;
		int [][] rotations = hasReflection ? FREE_TETROMINO_UNIQUE_ROTATIONS : null ;
		return getAllTypesAndRotations( types, numRotations, rotations, hasRotation ) ;
	}
	
	/**
	 * Returns an array of all tromino types needed to fully articulate all
	 * available options, given that players have the specified movements available.
	 * 
	 * Types are encoded with a 'recommended' qorientation (suitable for Retro) that
	 * can be easily removed and replaced with whatever qorientation you want.
	 * 
	 * The value returned is [NUM][2], where [i][0] is an encoded piece type,
	 * and [i][1] the default rotation.
	 * 
	 * @param hasRotation
	 * @param hasReflection
	 * @return
	 */
	protected static final int [][] getAllPentominoTypesAndRotations( boolean hasRotation, boolean hasReflection ) {
		int [] types = hasReflection ? FREE_PENTOMINO_TYPES : ONE_SIDED_PENTOMINO_TYPES ;
		int [] numRotations = hasReflection ? null : ONE_SIDED_PENTOMINO_UNIQUE_ROTATIONS ;
		int [][] rotations = hasReflection ? FREE_PENTOMINO_UNIQUE_ROTATIONS : null ;
		return getAllTypesAndRotations( types, numRotations, rotations, hasRotation ) ;
	}
	
	/**
	 * Returns a [NUM][2] array of all types needed to fully articulate available options,
	 * given that players have the specified movements available.
	 * 
	 * 'types' enumerates the encoded piece types, each including a rainbow (R0 to R6) qcombination.
	 * 'numRotations' is an array of the same length, providing the number of unique rotations
	 * needed to express all block configurations.  'hasRotation' indicates whether players will
	 * be allowed to rotate the pieces.
	 * 
	 * @param types
	 * @param numRotations
	 * @param hasRotation
	 * @return
	 */
	private static final int [][] getAllTypesAndRotations( int [] types, int [] numRotations, int [][] rotations, boolean hasRotation ) {
		if ( ( numRotations == null ) == ( rotations == null ) )
			throw new IllegalArgumentException("Must provide exactly one non-null rotations array.") ;
			
		int num = 0 ;
		for ( int i = 0; i < types.length; i++ ) {
			if ( hasRotation )
				num += 1 ;
			else if ( numRotations != null )
				num += numRotations[i] ;
			else
				num += rotations[i].length ;
		}
		
		int [][] result = new int[num][2] ;
		int index = 0 ;
		for ( int t = 0; t < types.length; t++ ) {
			int numR = 1 ;
			if ( !hasRotation ) {
				numR = numRotations == null ? rotations[t].length : numRotations[t] ;
			}
			for ( int r = 0; r < numR; r++ ) {
				int qc = PieceCatalog.getQCombination(types[t]) ;
				int rot = rotations != null ? rotations[t][r] : r ;
				qc += rot ;
				if ( qc > QCombinations.R6 )
					qc -= 7 ;
				int type = types[t] ;
				if ( PieceCatalog.isTromino(type) )
					type = PieceCatalog.encodeTromino( PieceCatalog.getTrominoCategory(type), qc ) ;
				else if ( PieceCatalog.isTetromino(type) )
					type = PieceCatalog.encodeTetromino( PieceCatalog.getTetrominoCategory(type), qc ) ;
				else if ( PieceCatalog.isPentomino(type) )
					type = PieceCatalog.encodePentomino( PieceCatalog.getPentominoCategory(type), qc ) ;
				
				result[index][0] = type ;
				result[index][1] = rot ;
				index++ ;
			}
		}
		
		return result ;
	}
	
	

	/**
	 * Returns an array of the piece types which this 
	 * PieceBag returns.  There are only limited guarantees
	 * about this method.
	 * 
	 * 1. We do not guarantee a newly allocated array with
	 * 			every call.  In other words, if one caller
	 * 			makes changes to the returned array those
	 * 			changes may be reflected in the next call
	 * 			(although they do NOT affect the types
	 * 			of pieces which the bag can produce, so
	 * 			this will give an inaccurate view of its contents.
	 * 
	 * 2. We do not guarantee that this array is allocated ahead of
	 * 			time, so this call may cause a memory allocation.
	 * 
	 * @return An array of exactly those piece types which this
	 * 			bag can produce with pop().
	 */
	public abstract int [] contents() ;
	
	/**
	 * Returns whether the specified piece type is contained
	 * 		within this bag.  Equivalent to calling contents()
	 * 		and performing a linear search, although this call
	 * 		may be more efficient.
	 */
	public abstract boolean has( int pieceType ) ;
	
	public abstract boolean hasTrominoes() ;
	public abstract boolean hasTetrominoes() ;
	public abstract boolean hasPentominoes() ;
	public abstract boolean hasTetracubes() ;
	
	
	/**
	 * peek Peek into the PieceBag at the given lookahead.  This
	 * piece will be on top after the given number of pops.
	 * 
	 * @param lookahead How far into the future to gaze?
	 * @return What piece type will be drawn?
	 */
	public abstract int peek( int lookahead ) ;
	
	/**
	 * pop Pull a piece out of the bag.  The provided piece has
	 * its type, rotation, and default rotation set.  Blocks,
	 * bounds and other values are NOT set.
	 */
	public abstract void pop( Piece p ) ;
	
	/**
	 * push Push a piece on top of the bag.  The provided
	 * object is not modified.
	 */
	public abstract void push( Piece p ) ;
	
	// PieceBag capabilities; may vary between pieces
	
	/**
	 * canPush Does the 'push' method function for this PieceBag?
	 * @return Does pushing work?
	 */
	public abstract boolean canPush() ;
	
	/**
	 * canPeek Does the 'peek' method function with the provided lookahead?
	 * @param lookahead How far ahead we wish to look
	 * @return Is it appropriate to look this far ahead?
	 */
	public abstract boolean canPeek( int lookahead ) ;
}
