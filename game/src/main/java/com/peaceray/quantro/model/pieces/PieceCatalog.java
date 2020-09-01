package com.peaceray.quantro.model.pieces;

import com.peaceray.quantro.exceptions.InvalidPieceException;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QUpconvert;

import java.util.Random ;

/**
 * This PieceCatalog class defines codes for indicating piece types:
 * this code is a int, passed to a Piece object as "type."
 * The Catalog provides methods for interpreting
 * these codes.  Piece type generation (and the generation of
 * the Piece objects themselves) is left to other classes.  Those
 * classes may freely generate whatever Pieces they want,
 * so long as those generations fit the definitions provided here.
 * 
 * Do NOT define entirely new piece types outside of this Catalog.
 * Good practice will use the "arbitrary" piece code for any self-constructed
 * pieces, but that may not be necessary depending on how the Piece is used.
 * 
 * Here we define the code.  Assumption: there are no more than 100 QOrientations,
 * numbered 0-99.
 * 
 * Tetrominoes: 	[	0,	999	]
 * 		Standard shapes composed entirely of 1 QOrientation.
 * 		
 * 		t := Q*A
 * 			A	:=	Tetromino type
 * 			Q*	:=	QOrientation
 * 
 * 		Tetromino types:
 * 			0	:=	Line piece.
 * 			1	:=	Gamma.  Rotates to be a backwards-L, or a backwards-7.
 * 			2	:=	Gun (reverse-gamma).  Rotates to be an L or a 7.
 * 			3	:=	Square.
 * 			4	:=	An S.  Reverse-squiggly.
 * 			5	:=	T-block.
 * 			6	:=	A Z.  Squiggly.
 * 
 * 
 * Tetracubes:		[	10000,	19999	]
 * 		Extended shapes: made of 4 cubes, not drawable as tetromino.
 * 
 * 		t := 1Q*AB
 * 			A	:=	Tetracube category (set closed under reflection / 3DRotation)
 * 			B	:=	Tetracube subcategory (instance of category)
 * 			Q*	:=	QOrientation of "special" block(s).  Most tetracubes
 * 					have S1 and/or S2 components; this indicates the orientation
 * 					of all other block(s) (usually only 1 of these).
 * 
 * 		Tetracube categories / subcategories:
 * 			0	:=	3D L.  1 special then 2 standard in a straight line.
 * 					Subcategories {0,1}:
 * 					0	:= Standard blocks are S1
 * 					1	:= Standard blocks are S2
 * 			
 * 			1	:=	Rectangle.  2 special blocks in a straight line.
 * 					Subcategories {0}
 * 
 * 			2	:=	Zig-Zag.  S1, then special, then S2 in a line.
 * 					Subcategories {0}
 * 
 * 			3	:=	3DT.  A special flanked by standard blocks.
 * 					Subcategories {0,1}
 * 					0	:= Special is flanked by S1
 * 					1	:= Special is flanked by S2
 * 
 * 			4	:=	Branch.  A special with 2 adjacent and identical std. blocks: 1 U, 1 R.
 * 					Subcategories {0,1}
 * 					0	:= Standard blocks U and R are both S1
 * 					1	:= Standard blocks U and R are both S2
 * 
 * 			5	:=	Screw.  Same shape as Branch - except the std. blocks are different.
 * 					Subcategories {0,1}
 * 					0	:= S1 is Up, S2 is Right.  "Right Screw"
 * 					1	:= S2 is Up, S1 is Right.	"Left Screw"
 * 
 * 			6	:=	Corner.  From special block:  Step (L/R), std.  Step Up, std.  Both std. the same.
 * 					Subcategories {0,1,2,3}
 * 					0	:= S1 is used.  Step Right then Up.  Left hand.
 * 					1	:= S1 is used.  Step Left then Up.   Right hand.
 * 					2	:= S2 is used.  Step Right then Up.  Right hand.
 * 					3	:= S2 is used.  Step Left then Up.   Left hand.
 * 
 * Special pieces:	[	20000,	29999	]
 * 		Special piece types, neither Tetromino nor Tetracube.
 * 
 * 		t := 2Q*AB
 * 			A	:=	Special piece category
 * 			B 	:=	Special piece subcategory
 * 			Q*	:=	QOrientation, if needed.
 * 
 * 		Special piece categories
 * 			00	:=	1-block.  A single, solitary block.  No special info.
 * 			01	:=	1-block flash.  QOrientation must be F0 or F1.
 * 			02	:=	Arbitrary blocks.  QOrientation is ignored.
 * 			03  :=	Push down block.
 * 
 * 			1X 	:=	(X-1) block galaxy.
 * 			
 * 
 * 
 * Tronimoes: 		[	30000, 30999 	]
 * 		Standard shapes composed entirely of 1 QOrientation.
 * 
 * 		t := 30QQA
 * 			A	:=	Tromino type
 * 			Q*	:=	QOrientation
 * 
 *  	Tronimo types:
 * 			0	:=	Line piece (I).
 * 			1	:=	L-piece (V).
 * 
 * 
 * Pentominoes:		[ 	40000, 49999	]
 * 		Standard shapes composed entirely of 1 QOrientation.
 * 
 * 		t := 4QQAA
 * 			AA	:=	Pentomino type
 * 			QQ	:=	QOrientation
 * 
 * 		Pentomino types:
 * 			0	:= F
 * 			1	:= I
 * 			2	:= L
 * 			3	:= N
 * 			4	:= P
 * 			5	:= T
 * 			6	:= U
 * 			7	:= V
 * 			8	:= W
 * 			9	:= X
 * 			10	:= Y
 * 			11	:= Z
 * 			12	:= reverse-F
 * 			13	:= reverse-L
 * 			14	:= reverse-N
 * 			15	:= reverse-P
 * 			16	:= reverse-Y
 * 			17	:= reverse-Z
 * 
 * 
 * @author Jake Rosin
 */
public final class PieceCatalog {
	
	// Some helpful finalants
	public static final int NUMBER_TETROMINO_CATEGORIES = 7 ;
	public static final int NUMBER_TETRACUBE_CATEGORIES = 7 ;
	public static final int [] NUMBER_TETRACUBE_SUBCATEGORIES = { 2, 1, 1, 2, 2, 2, 4 } ;
	public static final int NUMBER_SPECIAL_CATEGORIES = 13 ;
	public static final int [] NUMBER_SPECIAL_SUBCATEGORIES = { 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 10 } ;
	public static final int NUMBER_TROMINO_CATEGORIES = 2 ;
	public static final int NUMBER_PENTOMINO_CATEGORIES = 18 ;
	
	
	// Other helpful definitions
	public static final int TETRO_CAT_LINE = 0 ;
	public static final int TETRO_CAT_GAMMA = 1 ;
	public static final int TETRO_CAT_GUN = 2 ;
	public static final int TETRO_CAT_SQUARE = 3 ;
	public static final int TETRO_CAT_S = 4 ;
	public static final int TETRO_CAT_T = 5 ;
	public static final int TETRO_CAT_Z = 6 ;
	
	public static final int TETRA_CAT_L = 0 ;
	public static final int TETRA_CAT_RECT = 1 ;
	public static final int TETRA_CAT_S = 2 ;
	public static final int TETRA_CAT_T = 3 ;
	public static final int TETRA_CAT_BRANCH = 4 ;
	public static final int TETRA_CAT_SCREW = 5 ;
	public static final int TETRA_CAT_CORNER = 6 ;
	
	public static final int SPECIAL_CAT_BLOCK = 0 ;
	public static final int SPECIAL_CAT_FLASH = 1 ;
	public static final int SPECIAL_CAT_ARBITRARY = 2 ;
	public static final int SPECIAL_CAT_PUSH_DOWN = 3 ;
	
	public static final int SPECIAL_CAT_GALAXY = 10 ;
	
	
	public static final int TRO_CAT_LINE = 0 ;
	public static final int TRO_CAT_V = 1 ;
	
	public static final int PENTO_CAT_F = 0 ;
	public static final int PENTO_CAT_I = 1 ;
	public static final int PENTO_CAT_L = 2 ;
	public static final int PENTO_CAT_N = 3 ;
	public static final int PENTO_CAT_P = 4 ;
	public static final int PENTO_CAT_T = 5 ;
	public static final int PENTO_CAT_U = 6 ;
	public static final int PENTO_CAT_V = 7 ;
	public static final int PENTO_CAT_W = 8 ;
	public static final int PENTO_CAT_X = 9 ;
	public static final int PENTO_CAT_Y = 10 ;
	public static final int PENTO_CAT_Z = 11 ;
	public static final int PENTO_CAT_F_REVERSE = 12 ;
	public static final int PENTO_CAT_L_REVERSE = 13 ;
	public static final int PENTO_CAT_N_REVERSE = 14 ;
	public static final int PENTO_CAT_P_REVERSE = 15 ;
	public static final int PENTO_CAT_Y_REVERSE = 16 ;
	public static final int PENTO_CAT_Z_REVERSE = 17 ;
	
	
	
	
	// Used for encoding pieces
	private static final int Q_COMBINATION_DIGITS = (int)Math.ceil(Math.log10( QCombinations.NUM )) ;
	private static final int TETRO_MIN_TYPE = 0 ;
	private static final int TETRO_MAX_TYPE = (int)Math.pow(10, Q_COMBINATION_DIGITS+1) - 1 ;
	private static final int TETRA_MIN_TYPE = (int)Math.pow(10, Q_COMBINATION_DIGITS+2) ;
	private static final int TETRA_MAX_TYPE = TETRA_MIN_TYPE*2 -1 ; 
	private static final int SPECIAL_MIN_TYPE = TETRA_MIN_TYPE*2 ;
	private static final int SPECIAL_MAX_TYPE = TETRA_MIN_TYPE*3 - 1 ;
	private static final int TRO_MIN_TYPE = TETRA_MIN_TYPE*3 ;
	private static final int TRO_MAX_TYPE = TETRA_MIN_TYPE*4 -1 ;
	private static final int PENTO_MIN_TYPE = TETRA_MIN_TYPE*4 ;
	private static final int PENTO_MAX_TYPE = TETRA_MIN_TYPE*5 -1 ;
	
	
	
	// This is a random generator.  Java is nonsense.
	// Try inlining random category functions now, compiler!
	private static final Random r = new Random() ;
	
	
	
	////////////////////////////////////////////////////////////////////////
	// STATIC FUNCTIONS FOR ANALYZING PIECE TYPES
	
	/*
	 * 'is' functions do not check validity of the type given; a "false" is definitive,
	 * but a 'true' indicates only that the type *appears* to be the given category.
	 */
	
	
	/**
	 * isTetromino
	 * Is this a tetromino?  'false' is definitive, but false positives are possible.
	 * 
	 * @param type	The piece type.
	 * @return boolean - does this appear to be a tetromino?
	 */
	public static final boolean isTetromino( int type ) {
		//return 0 <= type && type <= 999 ;
		// Changed for fluid QCombination.NUM changes.
		// A tetromino has a code Q*A, with Q the Q combination digits.
		return TETRO_MIN_TYPE <= type && type <= TETRO_MAX_TYPE ;
	}

	/**
	 * isTetromino
	 * Is this a tetromino?  'false' is definitive, but false positives are possible.
	 * 
	 * @param p	The piece.
	 * @return boolean - does this appear to be a tetromino?
	 */
	public static final boolean isTetromino( Piece p ) {
		return isTetromino( p.type ) ;
	}
	
	/**
	 * isTetracube
	 * Is this a tetracube?  'false' is definitive, but false positives are possible.
	 * 
	 * @param type	The piece type.
	 * @return boolean - does this appear to be a tetracube?
	 */
	public static final boolean isTetracube( int type ) {
		//return 10000 <= type && type <= 19999 ;
		// Changed for fluid QCombination.NUM changes.
		// Form is 1Q*AB
		return TETRA_MIN_TYPE <= type && type <= TETRA_MAX_TYPE ;
	}
	
	/**
	 * isTetracube
	 * Is this a tetracube?  'false' is definitive, but false positives are possible.
	 * 
	 * @param p	The piece.
	 * @return boolean - does this appear to be a tetracube?
	 */
	public static final boolean isTetracube( Piece p ) {
		return isTetracube( p.type ) ;
	}

	/**
	 * isSpecial
	 * Is this a special piece?  'false' is definitive, but false positives are possible.
	 * 
	 * @param type	The piece type.
	 * @return boolean - does this appear to be a special piece?
	 */
	public static final boolean isSpecial( int type ) {
		//return 20000 <= type && type <= 29999 ;
		// Changed for fluid QCombination.NUM changes
		// Form is 2Q*AA
		return SPECIAL_MIN_TYPE <= type && type <= SPECIAL_MAX_TYPE ;
	}

	/**
	 * isSpecial
	 * Is this a special piece?  'false' is definitive, but false positives are possible.
	 * 
	 * @param p	The piece.
	 * @return boolean - does this appear to be a special piece?
	 */
	public static final boolean isSpecial( Piece p ) {
		return isSpecial( p.type ) ;
	}
	
	
	public static final boolean isPolyomino( int type ) {
		return isTromino(type) || isTetromino(type) || isPentomino(type) ;
	}
	
	public static final boolean isPolyomino( Piece p ) {
		return isPolyomino(p.type) ;
	}
	
	
	
	/**
	 * isTromino
	 * Is this a tromino?  'false' is definitive, but false positives are possible.
	 * 
	 * @param type The piece type
	 * @return boolean - does this appear to be a tromino?
	 */
	public static final boolean isTromino( int type ) {
		// return 30000 <= type && type <= 39999
		return TRO_MIN_TYPE <= type && type <= TRO_MAX_TYPE ;
	}
	
	/**
	 * isTromino
	 * Is this a tromino?  'false' is definitive, but false positives are possible.
	 * 
	 * @param piece The piece type
	 * @return boolean - does this appear to be a tromino?
	 */
	public static final boolean isTromino( Piece p ) {
		return isTromino(p.type) ;
	}
	
	
	/**
	 * isPentomino
	 * Is this a pentomino?  'false' is definitive, but false positives are possible.
	 * 
	 * @param type The piece type
	 * @return boolean - does this appear to be a pentomino?
	 */
	public static final boolean isPentomino( int type ) {
		// return 30000 <= type && type <= 39999
		return PENTO_MIN_TYPE <= type && type <= PENTO_MAX_TYPE ;
	}
	
	/**
	 * isPentomino
	 * Is this a pentomino?  'false' is definitive, but false positives are possible.
	 * 
	 * @param piece The piece type
	 * @return boolean - does this appear to be a pentomino?
	 */
	public static final boolean isPentomino( Piece p ) {
		return isPentomino(p.type) ;
	}
	
	
	
	
	
	
	public static final boolean isValid( int type ) {
		return isTetromino(type) || isTetracube(type) || isSpecial(type) || isTromino(type) || isPentomino(type) ;
	}
	
	public static final boolean isValid( Piece p ) {
		return isValid( p.type ) ;
	}

	

	/*
	 * Information functions about pieces.  For example, some
	 * have a QOrientation label.
	 */

	/**
	 * Gets the QOrientation of the piece, assuming its meaning is defined
	 * in the Catalog.  Works for Tetrominoes, Tetracubes, and some special
	 * blocks.  For instance, "flash" blocks don't have a well-defined
	 * QOrientation, since they can switch between orientations with a rotation.
	 * 
	 * @param type	Piece type
	 * @return What is the QOrientation of the piece?
	 */
	public static final int getQCombination( int type ) throws InvalidPieceException {
		if ( isTetromino( type ) ) {
			// Code is at Q*0
			return (int)Math.floor(type / 10) ;
		}
		
		if ( isTetracube( type) ) {
			// Code is at 1Q*00
			return (int)(Math.floor(type / 100) % Math.pow(10, Q_COMBINATION_DIGITS)) ;
		}
		
		if ( isSpecial( type ) ) {
			// Code is at 2Q*00
			return (int)(Math.floor(type / 100) % Math.pow(10, Q_COMBINATION_DIGITS)) ;
		}
		
		if ( isTromino( type ) ) {
			// Code is at 30Q*0
			return (int)(Math.floor(type / 10) % Math.pow(10, Q_COMBINATION_DIGITS)) ;
		}
		
		if ( isPentomino( type ) ) {
			// Code is at 4Q*00
			return (int)(Math.floor(type / 100) % Math.pow(10, Q_COMBINATION_DIGITS)) ;
		}
		
		
		
		// We have problem!
		throw new InvalidPieceException(type, "PieceCatalog.getQCombination cannot resolve piece type " + type) ;
	}

	/**
	 * Gets the QOrientation of the piece, assuming its meaning is defined
	 * in the Catalog.  Works for Tetrominoes, Tetracubes, and some special
	 * blocks.  For instance, "flash" blocks don't have a well-defined
	 * QOrientation, since they can switch between orientations with a rotation.
	 * 
	 * @param p	The Piece
	 * @return What is the QOrientation of the piece?
	 */
	public static final int getQCombination( Piece p ) throws InvalidPieceException {
		return getQCombination( p.type ) ;
	}

	/**
	 * getTetrominoCategory - returns the category number {0,1,...,6}
	 * for the given tetromino type.  This number is one of
	 * the TETRO_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tetromino; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'type' is a valid tetromino
	 * 
	 * @param type	Piece type to analyze
	 * @return		The tetromino category
	 */
	public static final int getTetrominoCategory( int type ) {
		// Category is given by the lowest digit
		return type % 10 ;
	}
	
	/**
	 * getTetrominoCategory - returns the category number {0,1,...,6}
	 * for the given tetromino.  This number is one of
	 * the TETRO_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tetromino; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'p.type' is a valid tetromino type
	 * 
	 * @param p		Piece to analyze
	 * @return		The tetromino category
	 */
	public static final int getTetrominoCategory( Piece p ) {
		return getTetrominoCategory( p.type ) ;
	}
	
	
	/**
	 * getTetracubeCategory - returns the category number {0,1,...,6}
	 * for the given tetracube type.  This number is one of
	 * the TETRA_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tetracube; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'type' is a valid tetracube
	 * 
	 * @param type	Piece type to analyze
	 * @return		The tetracube category
	 */
	public static final int getTetracubeCategory( int type ) {
		// Category is given by the second lowest digit
		return (type/10) % 10 ;
	}
	
	/**
	 * getTetracubeCategory - returns the category number {0,1,...,6}
	 * for the given tetracube piece.  This number is one of
	 * the TETRA_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tetracube; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'p.type' is a valid tetracube type
	 * 
	 * @param p		Piece to analyze
	 * @return		The tetracube category
	 */
	public static final int getTetracubeCategory( Piece p ) {
		// Category is given by the second lowest digit
		return getTetracubeCategory( p.type ) ;
	}
	
	/**
	 * getTetracubeSubcategory - returns the subcategory number
	 * for the given tetracube type.  Subcategories define the
	 * various ways for a tetracube category to present itself:
	 * reflections, etc.  Note that different tetracube categories
	 * have different ranges for acceptable subcategories.
	 * 
	 * PRECONDITION: 'type' is a valid tetracube
	 * 
	 * @param type	Piece type to analyze
	 * @return		The tetracube subcategory
	 */
	public static final int getTetracubeSubcategory( int type ) {
		// Subcategory is given by the lowest digit.
		return type % 10 ;
	}
	
	/**
	 * getTetracubeSubcategory - returns the subcategory number
	 * for the given tetracube.  Subcategories define the
	 * various ways for a tetracube category to present itself:
	 * reflections, etc.  Note that different tetracube categories
	 * have different ranges for acceptable subcategories.
	 * 
	 * PRECONDITION: 'p.type' is a valid tetracube type
	 * 
	 * @param p		Piece to analyze
	 * @return		The tetracube subcategory
	 */
	public static final int getTetracubeSubcategory( Piece p ) {
		// Subcategory is given by the lowest digit.
		return getTetracubeSubcategory( p.type ) ;
	}
	
	/**
	 * getSpecialCategory - returns the category number {0,1,...,99}
	 * for the given special type.  This number is one of
	 * the SPECIAL_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid special piece; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'type' is a valid special type
	 * 
	 * @param type	Piece type to analyze
	 * @return		The special category
	 */
	public static final int getSpecialCategory( int type ) {
		// Category is given by the two lowest digits
		int cat = type % 100 ;
		if ( cat < 10 )
			return cat ;
		return ( cat / 10 ) * 10 ; 	// discard ones place.
	}
	
	/**
	 * getSpecialCategory - returns the subcategory number {0,...9}
	 * for the given special Piece.  This number is one of
	 * the SPECIAL_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid special piece; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'type' is a valid special type
	 * 
	 * @param type	Piece type to analyze
	 * @return		The special category
	 */
	public static final int getSpecialSubcategory( int type ) {
		int cat = type % 100 ;
		if ( cat < 10 )
			return 0 ;
		return cat % 10 ;
	}
	
	/**
	 * getSpecialCategory - returns the category number {0,1,...,99}
	 * for the given special Piece.  This number is one of
	 * the SPECIAL_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid special piece; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'p.type' is a valid special type
	 * 
	 * @param p		Piece to analyze
	 * @return		The special category
	 */
	public static final int getSpecialCategory( Piece p ) {
		// Category is given by the two lowest digits
		return getSpecialCategory( p.type ) ;
	}
	

	/**
	 * getSpecialCategory - returns the subcategory number {0,...9}
	 * for the given special Piece.  This number is one of
	 * the SPECIAL_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid special piece; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'p.type' is a valid special type
	 * 
	 * @param p		Piece to analyze
	 * @return		The special category
	 */
	public static final int getSpecialSubcategory( Piece p ) {
		return getSpecialSubcategory( p.type ) ;
	}
	
	
	
	/**
	 * getTrominoCategory - returns the category number {0,1}
	 * for the given tromino type.  This number is one of
	 * the TRO_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tromino; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'type' is a valid tromino
	 * 
	 * @param type	Piece type to analyze
	 * @return		The tromino category
	 */
	public static final int getTrominoCategory( int type ) {
		// Category is given by the lowest digit
		return type % 10 ;
	}
	
	/**
	 * getTrominoCategory - returns the category number {0,1}
	 * for the given tromino.  This number is one of
	 * the TRO_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tromino; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'p.type' is a valid tromino type
	 * 
	 * @param p		Piece to analyze
	 * @return		The tromino category
	 */
	public static final int getTrominoCategory( Piece p ) {
		return getTrominoCategory( p.type ) ;
	}
	
	
	/**
	 * getPentominoCategory - returns the category number {0,1}
	 * for the given tromino type.  This number is one of
	 * the TRO_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tromino; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'type' is a valid tromino
	 * 
	 * @param type	Piece type to analyze
	 * @return		The tromino category
	 */
	public static final int getPentominoCategory( int type ) {
		// Category is given by the lowest 2 digits
		return type % 100 ;
	}
	
	/**
	 * getPentominoCategory - returns the category number {0,1}
	 * for the given tromino.  This number is one of
	 * the TRO_CAT_* final ints.  Behavior unspecified if the type
	 * is not a valid tromino; e.g., the value returned may not be
	 * one of the defined constants.
	 * 
	 * PRECONDITION: 'p.type' is a valid tromino type
	 * 
	 * @param p		Piece to analyze
	 * @return		The tromino category
	 */
	public static final int getPentominoCategory( Piece p ) {
		return getPentominoCategory( p.type ) ;
	}
	
	
	
	/*
	 * Generating piece types at random (uniformly):
	 */
	
	/**
	 * randomTetrominoCategory - Returns a uniformly random tetromino
	 * category number; one of TETRO_CAT_*.
	 * 
	 * @return One of t = TETRO_CAT_*, with 0 <= t < NUMBER_TETROMINO_CATEGORIES
	 */
	public static final int randomTetrominoCategory() {
		return r.nextInt( NUMBER_TETROMINO_CATEGORIES ) ;
	}
	
	/**
	 * randomFreeTetrominoCategory - Returns a uniformly random free tetromino
	 * category number; one of TETRO_CAT_*.  Only includes 'free' tetromino
	 * types, which means dropping S and GAMMA.
	 * 
	 * @return One of t = TETRO_CAT_*, with 0 <= t < NUMBER_TETROMINO_CATEGORIES
	 */
	public static final int randomFreeTetrominoCategory() {
		int t = r.nextInt( NUMBER_TETROMINO_CATEGORIES -2 ) ;
		if ( t >= TETRO_CAT_GAMMA )
			t++ ;
		if ( t >= TETRO_CAT_S )
			t++ ;
		return t ;
	}
	
	public static final int [] getTetrominoCategories() {
		return new int[] {
				TETRO_CAT_LINE,
				TETRO_CAT_GAMMA,
				TETRO_CAT_GUN,
				TETRO_CAT_SQUARE,
				TETRO_CAT_S,
				TETRO_CAT_T,
				TETRO_CAT_Z
		} ;
	}
	
	public static final int [] getFreeTetrominoCategories() {
		return new int[] {
				TETRO_CAT_LINE,
				TETRO_CAT_GUN,
				TETRO_CAT_SQUARE,
				TETRO_CAT_T,
				TETRO_CAT_Z
		} ;
	}
	
	
	/**
	 * randomTetracubeCategory - Returns a uniformly random tetracube
	 * category number; one of TETRA_CAT_*.
	 * 
	 * @return One of t = TETRA_CAT_*, with 0 <= t < NUMBER_TETRACUBE_CATEGORIES
	 */
	public static final int randomTetracubeCategory() {
		return r.nextInt( NUMBER_TETRACUBE_CATEGORIES ) ;
	}
	
	/**
	 * randomTetracubeCategory - Returns a uniformly random free tetracube
	 * category number; one of TETRA_CAT_*.  Includes only 'free' tetracubes,
	 * which actually makes no difference; each tetracube category is itself "free."
	 * 
	 * @return One of t = TETRA_CAT_*, with 0 <= t < NUMBER_TETRACUBE_CATEGORIES
	 */
	public static final int randomFreeTetracubeCategory() {
		return randomTetracubeCategory() ;
	}
	
	public static final int [] getTetracubeCategories() {
		return new int[] {
				TETRA_CAT_L,
				TETRA_CAT_RECT,
				TETRA_CAT_S,
				TETRA_CAT_T,
				TETRA_CAT_BRANCH,
				TETRA_CAT_SCREW,
				TETRA_CAT_CORNER
		} ;
	}
	
	public static final int [] getFreeTetracubeCategories() {
		return getTetracubeCategories() ;
	}
	
	/**
	 * randomTetracubeSubcategory - Returns a uniformly random tetracube
	 * subcategory number; the subcategory is valid for the given category.
	 * 
	 * @param cat	The tetracube category which we wish to subcategorize
	 * @return Subcategory for 'cat', selected uniformly at random
	 */
	public static final int randomTetracubeSubcategory( int cat ) {
		return r.nextInt( NUMBER_TETRACUBE_SUBCATEGORIES[cat] ) ;
	}
	
	/**
	 * randomFreeTetracubeSubcategory - Returns a uniformly random free tetracube
	 * subcategory number; the subcategory is valid for the given category.
	 * Returns only 'free' subcategories, which are those that can represent
	 * every subcategory given reflection and rotation.
	 * 
	 * @param cat	The tetracube category which we wish to subcategorize
	 * @return Subcategory for 'cat', selected uniformly at random
	 */
	public static final int randomFreeTetracubeSubcategory( int cat ) {
		switch( cat ) {
		case TETRA_CAT_L:
		case TETRA_CAT_BRANCH:
			return 0 ;		// L, T and Branch are 'free.'
		case TETRA_CAT_T:
			return 1 ;		// ... mix things up with subcat 1 instead of 0.
		case TETRA_CAT_CORNER:
			return 2 * r.nextInt(2) ;		// 0/3 are left hands, 1/2 are right hands.  Use 0 and 2.
		default:
			return r.nextInt( NUMBER_TETRACUBE_SUBCATEGORIES[cat] ) ;
		}
	}
	
	
	/**
	 * randomSpecialCategory - Returns a uniformly random special
	 * category number; one of SPECIAL_CAT_*.
	 * 
	 * @return One of t = SPECIAL_CAT_*, with 0 <= t < NUMBER_SPECIAL_CATEGORIES
	 */
	public static final int randomSpecialCategory() {
		int random = r.nextInt( NUMBER_SPECIAL_CATEGORIES ) ;
		
		if ( random <= 2 )
			return random ;
		
		return random - 2 + 10 ;
	}
	
	/**
	 * randomTrominoCategory - Returns a uniformly random tromino
	 * category number; one of TRO_CAT_*.
	 * 
	 * @return One of t = TRO_CAT_*, with 0 <= t < NUMBER_TROMINO_CATEGORIES
	 */
	public static final int randomTrominoCategory() {
		return r.nextInt( NUMBER_TROMINO_CATEGORIES ) ;
	}
	
	/**
	 * randomTrominoCategory - Returns a uniformly random free tromino
	 * category number; one of TRO_CAT_*.
	 * 
	 * @return One of t = TRO_CAT_*, with 0 <= t < NUMBER_TROMINO_CATEGORIES
	 */
	public static final int randomFreeTrominoCategory() {
		// trominoes are free.
		return r.nextInt( NUMBER_TROMINO_CATEGORIES ) ;
	}
	
	public static final int [] getTrominoCategories() {
		return new int[] {
				TRO_CAT_LINE,
				TRO_CAT_V
		} ;
	}
	
	public static final int [] getFreeTrominoCategories() {
		return getTrominoCategories() ;
	}
	
	/**
	 * randomPentominoCategory - Returns a uniformly random pentomino
	 * category number; one of PENTO_CAT_*.
	 * 
	 * @return One of t = PENTO_CAT_*, with 0 <= t < NUMBER_PENTOMINO_CATEGORIES
	 */
	public static final int randomPentominoCategory() {
		return r.nextInt( NUMBER_PENTOMINO_CATEGORIES ) ;
	}
	
	/**
	 * randomPentominoCategory - Returns a uniformly random pentomino
	 * category number; one of PENTO_CAT_*.
	 * 
	 * @return One of t = PENTO_CAT_*, with 0 <= t < NUMBER_PENTOMINO_CATEGORIES
	 */
	public static final int randomFreePentominoCategory() {
		// Easy one; dropping those labeled "REVERSE" will result
		// in a set of those which are free.
		return r.nextInt( PENTO_CAT_Z + 1 ) ;
	}
	
	
	public static final int [] getPentominoCategories() {
		return new int[] {
				PENTO_CAT_F,
				PENTO_CAT_I,
				PENTO_CAT_L,
				PENTO_CAT_N,
				PENTO_CAT_P,
				PENTO_CAT_T,
				PENTO_CAT_U,
				PENTO_CAT_V,
				PENTO_CAT_W,
				PENTO_CAT_X,
				PENTO_CAT_Y,
				PENTO_CAT_Z,
				PENTO_CAT_F_REVERSE,
				PENTO_CAT_L_REVERSE,
				PENTO_CAT_N_REVERSE,
				PENTO_CAT_P_REVERSE,
				PENTO_CAT_Y_REVERSE,
				PENTO_CAT_Z_REVERSE
		} ;
	}
	
	public static final int [] getFreePentominoCategories() {
		return new int[] {
				PENTO_CAT_F,
				PENTO_CAT_I,
				PENTO_CAT_L,
				PENTO_CAT_N,
				PENTO_CAT_P,
				PENTO_CAT_T,
				PENTO_CAT_U,
				PENTO_CAT_V,
				PENTO_CAT_W,
				PENTO_CAT_X,
				PENTO_CAT_Y,
				PENTO_CAT_Z
		} ;
	}
	
	
	
	
	/*
	 * Encode information into a canonical piece type.
	 */
	
	/**
	 * encodeTetromino Encodes a tetromino as in integer type.
	 * Does not check validity, so ensure the category
	 * and qOrientation are valid for a tetromino.
	 * 
	 * @param category	A valid tetromino category
	 * @param qOrientation	A valid QOrientation
	 * @return A 'type' code for this piece.
	 */
	public static final int encodeTetromino( int category, int qOrientation ) {
		// Tetrominoes are Q*A
		return (qOrientation * 10) + category ;
	}
	
	/**
	 * encodeTetracube Encodes a tetracube as in integer type.
	 * Does not check validity, so ensure the category, subcategory
	 * and qOrientation are valid for a tetracube.
	 * 
	 * @param category	A valid tetracube category
	 * @param subcategory	A valid subcategory for 'category'
	 * @param qOrientation	A valid QOrientation
	 * @return A 'type' code for this piece.
	 */
	public static final int encodeTetracube( int category, int subcategory, int qOrientation ) {
		// Tetracubes are 1Q*AB
		// Combine 	AB 		(easy)
		//			Q*00 	(qOrientation * 100)
		// 			1**00	(TETRA_MIN_TYPE)
		return TETRA_MIN_TYPE + ( qOrientation * 100 ) + ( category * 10 ) + subcategory ;
	}
	
	/**
	 * encodeSpecial Encodes a special piece as an integer type.
	 * Like the other encode functions, does not check validity.
	 * @param category	A valid special category
	 * @param qOrientation	A valid QOrientation for the category
	 * @return	A 'type' code for this piece.
	 */
	public static final int encodeSpecial( int category, int qOrientation ) {
		// Special blocks are 2Q*AB
		// Combine 	AB 		(easy)
		//			Q*00 	(qOrientation * 100)
		// 			2**00	(SPECIAL_MIN_TYPE)
		if ( category < 10 )
			return SPECIAL_MIN_TYPE + ( qOrientation * 100 ) + category ;
		throw new IllegalArgumentException("Categories above 10 require subcategory.") ;
	}
	
	
	/**
	 * encodeSpecial Encodes a special piece as an integer type.
	 * @param category
	 * @param subcategory
	 * @param qOrientation
	 * @return
	 */
	public static final int encodeSpecial( int category, int subcategory, int qOrientation ) {
		if ( category >= 10 || subcategory == 0 )
			return SPECIAL_MIN_TYPE + ( qOrientation * 100 ) + ( subcategory ) + ( category ) ;
		throw new IllegalArgumentException("Category " + category + " does not have subcategories") ;
	}
	
	
	/**
	 * encodeTromino Encodes a tromino as in integer type.
	 * Does not check validity, so ensure the category
	 * and qOrientation are valid for a tromino.
	 * 
	 * @param category	A valid tromino category
	 * @param qOrientation	A valid QOrientation
	 * @return A 'type' code for this piece.
	 */
	public static final int encodeTromino( int category, int qOrientation ) {
		// Trominos are 30Q*A
		return TRO_MIN_TYPE + (qOrientation * 10) + category ;
	}
	
	
	/**
	 * encodePentomino Encodes a pentomino as in integer type.
	 * Does not check validity, so ensure the category
	 * and qOrientation are valid for a pentomino.
	 * 
	 * @param category	A valid pentomino category
	 * @param qOrientation	A valid QOrientation
	 * @return A 'type' code for this piece.
	 */
	public static final int encodePentomino( int category, int qOrientation ) {
		// Pentomino are 3QQAA
		return PENTO_MIN_TYPE + (qOrientation * 100) + category ;
	}
	
	
	
	/**
	 * Attempts to upconvert the provided piece type to the new QCombination
	 * representation.
	 * @param type
	 * @return
	 */
	public static final int qUpconvert( int type ) {
		if ( type < 0 )
			return type ;
		
		int qc = QUpconvert.upConvert( getQCombination(type) ) ;
		
		if ( isTetromino(type) ) {
			return encodeTetromino( getTetrominoCategory(type), qc ) ;
		} else if ( isTromino(type) ) {
			return encodeTromino( getTrominoCategory(type), qc ) ;
		} else if ( isPentomino(type) ) {
			return encodePentomino( getPentominoCategory(type), qc ) ;
		} else if ( isTetracube(type) ) {
			return encodeTetracube(
					getTetracubeCategory(type),
					getTetracubeSubcategory(type),
					qc) ;
		} else if ( isSpecial(type) ) {
			return encodeSpecial(
					getSpecialCategory(type),
					getSpecialSubcategory(type),
					qc) ;
		}
		
		throw new IllegalArgumentException("Don't know what piece type " + type + " is supposed to be.") ;
	}
	
	
}
