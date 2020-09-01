package com.peaceray.quantro.q;

import com.peaceray.quantro.q.QOrientations ;


/**
 * A class with static constants defining the valid
 * QCombinations (note that every valid QOrientation is
 * also a QCombination), as well as some helpful static functions
 * for conversion between a QCombination (encoded as int)
 * and an array of QOrientations (also ints).
 * 
 * Safe assumptions:
 * QOrientations.? = QCombinations.? , for any valid QOrientation
 * QCombinations.NUM <= 100
 * All QCombinations are < NUM
 * All QCombinations are either < QOrientations.NUM or >= 100 - NUM_NOT_QORIENTATIONS.
 * 
 * However,
 * QCombinations.NUM may not necessarily be == QOrientations.NUM
 * 0 ... QCombinations.NUM may not all be valid QCombinations.
 * 
 * A change for 1.0 is the following revision: QOrientations count
 * up from 0, including NO.  QCombinations match these values,
 * but other possible combinations count downwards from 99.
 * 
 * Previous they counted up from the last available QOrientation,
 * leaving no room for future expansion.  See QUpconvert.
 * 
 * @author Jake
 *
 */
public final class QCombinations {
	// Defines the QCombinations.  Note that QCombinations
	// uses these to construct more complex "blocks", such
	// as SS, UU, SF, etc.
	
	// Any valid QOrientation is a valid QCombination; there
	// are more valid combinations as well.
	
	// Some standard blocks
	public static final int NO = QOrientations.NO ;			// No block
	public static final int S0 = QOrientations.S0 ;			// State 0
	public static final int S1 = QOrientations.S1 ;			// State 1
	
	// Some special blocks
	public static final int SL = QOrientations.SL ;			// Linked (white)
	public static final int SL_ACTIVE = QOrientations.SL_ACTIVE ;			// Linked (active)
	public static final int SL_INACTIVE = QOrientations.SL_INACTIVE ;			// Linked (inactive)
	public static final int ST = QOrientations.ST ;			// Sticky (green)
	public static final int ST_INACTIVE = QOrientations.ST_INACTIVE ;			// Sticky and inactive (green)
	public static final int F0 = QOrientations.F0 ;			// Flash (s1 pane)
	public static final int F1 = QOrientations.F1 ;			// Flash (s2 pane)
	public static final int U0 = QOrientations.U0 ;			// Unstable (s1 pane)		// 10
	public static final int U1 = QOrientations.U1 ;			// Unstable (s2 pane)
	public static final int UL = QOrientations.UL ;			// Unstable but linked		// 12
	
	// Even more special: blocks connected to SL.
	public static final int S0_FROM_SL = QOrientations.S0_FROM_SL ;		// A S0 connected to SL.
	public static final int S1_FROM_SL = QOrientations.S1_FROM_SL ;		// A S1 connected to SL.
	
	public static final int R0 = QOrientations.R0 ;
	public static final int R1 = QOrientations.R1 ;
	public static final int R2 = QOrientations.R2 ;
	public static final int R3 = QOrientations.R3 ;
	public static final int R4 = QOrientations.R4 ;
	public static final int R5 = QOrientations.R5 ;
	public static final int R6 = QOrientations.R6 ;
	
	public static final int RAINBOW_BLAND = QOrientations.RAINBOW_BLAND ;
	
	public static final int PUSH_DOWN = QOrientations.PUSH_DOWN ;
	public static final int PUSH_DOWN_ACTIVE = QOrientations.PUSH_DOWN_ACTIVE ;
	public static final int PUSH_UP = QOrientations.PUSH_UP ;
	public static final int PUSH_UP_ACTIVE = QOrientations.PUSH_UP_ACTIVE ;
	
	// Counter for new combinations!
	private static int counter = 99 ;
	
	
	// Combinations of blocks: 1 in one place, 1 in the other.
	public static final int SS = counter-- ;				// Aliased as "BO" - purple
	public static final int FF = counter-- ;				// Two flashes.  Probably never happens, but technically valid
	public static final int UU = counter-- ;				// Aliased as "UB" - two unstable
	public static final int SF = counter-- ;				// S0 and F1
	public static final int FS = counter-- ;				// F0 and S1
	public static final int SU = counter-- ;				// S0 and U1
	public static final int US = counter-- ;				// U0 and S1
	public static final int FU = counter-- ;				// Flash 0 and unstable 1
	public static final int UF = counter-- ;				// Unstable 0 and flash 1
	
	// As above, but replacing S with Sfromsl
	public static final int SfromslSfromsl = counter-- ;		// S0_FROM_SL and S1_FROM_SL
	public static final int SfromslS = counter-- ;				// S0_FROM_SL and S1
	public static final int SSfromsl = counter-- ;				// S0 and S1_FROM_SL
	public static final int SfromslF = counter-- ;				// S0_FROM_SL and F1
	public static final int FSfromsl = counter-- ;				// F0 and S1_FROM_SL
	public static final int SfromslU = counter-- ;				// S0_FROM_SL and U1
	public static final int USfromsl = counter-- ;				// U0 and S1_FROM_SL
	
	public static final int NUM = 100 ;
	public static final int NUM_NOT_QORIENTATIONS = 99 - counter ;
	
	
	private static final Integer [] objects = new Integer[NUM] ;
	
	static {
		if ( counter <= QOrientations.NUM )
			throw new RuntimeException("Too many QCombinations!  Limited to 100 only!") ;
		for ( int i = 0; i < NUM; i++ )
			objects[i] = new Integer(i) ;
	}
	
	public static final Integer getInteger(int qo) {
		return objects[qo] ;
	}
	
	
	// An array for holding the interpretations of QCombinations
	// as sets of QOrientations.  array[QC] is a 1-d array of the
	// QOrientations which are conceptually represented by the combination.
	// For instance,
	// array[S0] = {S0, NO}
	// array[S1] = {NO, S1}
	// array[LI] = {LI, LI}
	// array[BO] = {S0, S1}
	// array[FU] = {F0, U1}
	public static final byte [][] array ;
	public static final String [] str_array ;
	
	
	// Functions for use of this array, if you don't want to
	// do it yourself.  Probably best to use these rather than
	// reimplement everything over and over.
	
	/**
	 * asArray:	Returns the slice of array indicated by the combination.
	 * Java: there's more than one way to do it, but all are stupid!
	 * 
	 * @param combination The QCombination of interest.
	 */
	public static final byte [] asArray( int combination ) {
		return array[combination] ;
	}
	
	/**
	 * setAs: Set the given array (block field slice) to the QOrientations
	 * 			specifying the given QCombination
	 * 
	 * @param ar			A 1-d array of length 2
	 * @param combination	A valid QCombination
	 * 
	 * postcondition: fills in ar with the appropriate QOrientations
	 * 		represented by the given combination
	 */
	public static final void setAs( byte [] ar, int combination ) {
		ar[0] = array[combination][0] ;
		ar[1] = array[combination][1] ;
	}
	
	/**
	 * setAs: Set the given array (block field) to the QOrientations
	 * 			specifying the given QCombination
	 * 
	 * postcondition: fills in ar[*][row][col] with the appropriate QOrientations
	 * 		represented by the given combination
	 * 
	 * @param ar			A 3-d array with ar.length = 2
	 * @param row			A row index such that ar[*][row] is valid
	 * @param col			A col index such that ar[*][row][col] is valid
	 * @param combination	A valid QCombination
	 * 
	 */
	public static final void setAs( byte [][][] ar, int row, int col, int combination ) {
		ar[0][row][col] = array[combination][0] ;
		ar[1][row][col] = array[combination][1] ;
	}
	
	
	
	/**
	 * expand: Using a 2D array, size [R][C], and containing valid
	 * QCombinations, constructs and returns a 3D array of size
	 * [2][R][C] where [*][r][c] is the result of calling
	 * asArray on the combination indicated in ar[r][c].
	 * @param ar	A 2D array of QCombinations
	 * @return		A 3D array of QOrientations matching the input 'ar'.
	 */
	public static final byte [][][] expand( int [][] ar ) {
		// Note size requirements; allocate the space.
		int R = ar.length ;
		int C = ar[0].length ;
		byte [][][] result = new byte[2][R][C] ;
		
		// Expand the entries in ar to fill the appropriate slices
		// result.
		for ( int r = 0; r < R; r++ ) {
			for ( int c = 0; c < C; c++ ) {
				setAs( result, r, c, ar[r][c] ) ;
			}
		}
		
		return result ;
	}
	
	
	public static final byte expandedPane( int qc, int pane ) {
		return array[qc][pane] ;
	}
	
	public static final boolean occursInPane( byte qo, int pane ) {
		for ( int i = 0; i < NUM; i++ ) 
			if ( array[i][pane] == qo )
				return true ;
		return false ;
	}
	
	/**
	 * encode: return the QCombination represented by the given array
	 * 		(block field slice)
	 * 
	 * TODO: If this function is extensively used, it might be worth 
	 * 			optimizing and/or removing from its occurrences.
	 * 
	 * @param ar	A 1-d array of QOrientations
	 * @return		The QCombination represented
	 */
	public static final int encode( byte [] ar ) throws IllegalArgumentException {
		// Return the combination code representing this array.
		for ( int i = 0; i < NUM; i++ ) {
			if ( ar[0] == array[i][0] && ar[1] == array[i][1] )
				return i ;
		}
		
		// Whoops
		throw new IllegalArgumentException("Provided array is not a valid QCombination" + ar) ;
	}
	
	/**
	 * encode: return the QCombination represented by the given array
	 * 		(block field)
	 * 
	 * TODO: If this function is extensively used, it might be worth 
	 * 			optimizing and/or removing from its occurrences.
	 * 
	 * @param ar	A 3-d array of QOrientations
	 * @param row	The row: ar[*][row] must be OK
	 * @param col	The col: ar[*][row][col] must be OK
	 * @return		The QCombination represented
	 */
	public static final int encode( byte [][][] ar, int row, int col ) throws IllegalArgumentException {
		// Return the combination code representing this array.
		for ( int i = 0; i < NUM; i++ ) {
			if ( ar[0][row][col] == array[i][0] && ar[1][row][col] == array[i][1] )
				return i ;
		}
		
		// Whoops
		throw new IllegalArgumentException("Provided entry ("
				+ row + ", " + col
				+ ") is not a valid QCombination: "
				+ ar[0][row][col] + ":" + ar[1][row][col]) ;
	}
	
	
	
	
	
	
	public static final int decodeString( String str ) {
		int val = -1 ;
		try {
			val = Integer.parseInt(str) ;
		} catch ( NumberFormatException nfe ) {
			String cap = str.toUpperCase() ;
			for ( int i = 0; i < NUM; i++ )
				if ( str_array[i] != null && str_array[i].equals(cap) )
					val = i ;
		}
		
		if ( val >= 0 && val < NUM )
			return val ;
		return -1 ;
	}
	
	
	public static final String encodeString( int qc ) {
		return str_array[qc] ;
	}
	
	
	// Initialize array
	static {
		array = new byte[NUM][2] ;
		
		// Default: all to NO
		for ( int i = 0; i < NUM; i++ ) {
			array[i][0] = array[i][1] = QOrientations.NO ;
		}
		
		// Set the QOrientation values (simple enough)
		array[S0][0] = QOrientations.S0 ;
		array[S1][1] = QOrientations.S1 ;
		array[SL][0] = array[SL][1] = QOrientations.SL ;
		array[SL_INACTIVE][0] = array[SL_INACTIVE][1] = QOrientations.SL_INACTIVE ;
		array[SL_ACTIVE][0] = array[SL_ACTIVE][1] = QOrientations.SL_ACTIVE ;
		array[ST][0] = array[ST][1] = QOrientations.ST ;
		array[ST_INACTIVE][0] = array[ST_INACTIVE][1] = QOrientations.ST_INACTIVE ;
		array[F0][0] = QOrientations.F0 ;
		array[F1][1] = QOrientations.F1 ;
		array[U0][0] = QOrientations.U0 ;
		array[U1][1] = QOrientations.U1 ;
		array[UL][0] = array[UL][1] = QOrientations.UL ;
		
		array[S0_FROM_SL][0] = QOrientations.S0_FROM_SL ;
		array[S1_FROM_SL][1] = QOrientations.S1_FROM_SL ;
		
		// Set rainbow values.
		for ( byte i = QOrientations.R0; i <= QOrientations.R6 ; i++ )
			array[i][0] = array[i][1] = i ;
		array[RAINBOW_BLAND][0] = array[RAINBOW_BLAND][1] = QOrientations.RAINBOW_BLAND ;
		
		array[PUSH_DOWN][0] = array[PUSH_DOWN][1] = QOrientations.PUSH_DOWN ;
		array[PUSH_DOWN_ACTIVE][0] = array[PUSH_DOWN_ACTIVE][1] = QOrientations.PUSH_DOWN_ACTIVE ;
		array[PUSH_UP][0] = array[PUSH_UP][1] = QOrientations.PUSH_UP ;
		array[PUSH_UP_ACTIVE][0] = array[PUSH_UP_ACTIVE][1] = QOrientations.PUSH_UP_ACTIVE ;
		
		
		// Set the QCombinations values.  A bit tricker.
		// Doubles:
		array[SS][0] = QOrientations.S0 ;		array[SS][1] = QOrientations.S1 ;
		array[FF][0] = QOrientations.F0 ;		array[FF][1] = QOrientations.F1 ;
		array[UU][0] = QOrientations.U0 ;		array[UU][1] = QOrientations.U1 ;
		
		// Disparate combos:
		array[SF][0] = QOrientations.S0 ;		array[SF][1] = QOrientations.F1 ;
		array[FS][0] = QOrientations.F0 ;		array[FS][1] = QOrientations.S1 ;
		array[SU][0] = QOrientations.S0 ;		array[SU][1] = QOrientations.U1 ;
		array[US][0] = QOrientations.U0 ;		array[US][1] = QOrientations.S1 ;
		array[FU][0] = QOrientations.F0 ;		array[FU][1] = QOrientations.U1 ;
		array[UF][0] = QOrientations.U0 ;		array[UF][1] = QOrientations.F1 ;
		
		// New fancy combos
		array[SfromslSfromsl][0] = QOrientations.S0_FROM_SL ;		array[SfromslSfromsl][1] = QOrientations.S1_FROM_SL ;
		array[SfromslS][0]		 = QOrientations.S0_FROM_SL ;		array[SfromslS][1] = QOrientations.S1 ;
		array[SSfromsl][0]		 = QOrientations.S0 ;		array[SSfromsl][1] = QOrientations.S1_FROM_SL ;
		array[SfromslSfromsl][0] = QOrientations.S0_FROM_SL ;		array[SfromslSfromsl][1] = QOrientations.S1_FROM_SL ;
		array[SfromslF][0] = QOrientations.S0_FROM_SL ;			array[SfromslF][1] = QOrientations.F1 ;
		array[FSfromsl][0] = QOrientations.F0 ;					array[FSfromsl][1] = QOrientations.S1_FROM_SL ;
		array[SfromslU][0] = QOrientations.S0_FROM_SL ;			array[SfromslU][1] = QOrientations.U1 ;
		array[USfromsl][0] = QOrientations.U0 ;					array[USfromsl][1] = QOrientations.S1_FROM_SL ;

		
		// string representations of each.
		str_array = new String[NUM] ;
		str_array[NO] = "NO" ;
		str_array[S0] = "S0" ;
		str_array[S1] = "S1" ;
		// special blocks
		str_array[SL] = "SL" ;
		str_array[SL_ACTIVE] = "SL_ACTIVE" ;
		str_array[SL_INACTIVE] = "SL_INACTIVE" ;
		str_array[ST] = "ST" ;
		str_array[ST_INACTIVE] = "ST_INACTIVE" ;
		str_array[F0] = "F0" ;
		str_array[F1] = "F1" ;
		str_array[U0] = "U0" ;
		str_array[U1] = "U1" ;
		str_array[UL] = "UL" ;
		// Connected
		str_array[S0_FROM_SL] = "S0_FROM_SL" ;
		str_array[S1_FROM_SL] = "S1_FROM_SL" ;
		// Rainbow
		str_array[R0] = "R0" ;
		str_array[R1] = "R1" ;
		str_array[R2] = "R2" ;
		str_array[R3] = "R3" ;
		str_array[R4] = "R4" ;
		str_array[R5] = "R5" ;
		str_array[R6] = "R6" ;
		str_array[RAINBOW_BLAND] = "RAINBOW_BLAND" ;
		str_array[PUSH_DOWN] = "PUSH_DOWN" ;
		str_array[PUSH_DOWN_ACTIVE] = "PUSH_DOWN_ACTIVE" ;
		str_array[PUSH_UP] = "PUSH_UP" ;
		str_array[PUSH_UP_ACTIVE] = "PUSH_UP_ACTIVE" ;
		// Combinations
		str_array[SS] = "SS" ;
		str_array[FF] = "FF" ;
		str_array[UU] = "UU" ;
		str_array[SF] = "SF" ;
		str_array[FS] = "FS" ;
		str_array[SU] = "SU" ;
		str_array[US] = "US" ;
		str_array[FU] = "FU" ;
		str_array[UF] = "UF" ;
		// Sfrom
		str_array[SfromslSfromsl] = "SFROMSLSFROMSL" ;
		str_array[SfromslS] = "SFROMSLS" ;
		str_array[SSfromsl] = "SSFROMSL" ;
		str_array[SfromslF] = "SFROMSLF" ;
		str_array[FSfromsl] = "FSFROMSL" ;
		str_array[SfromslU] = "SFROMSLU" ;
		str_array[USfromsl] = "USFROMSL" ;

		// That's all.
	}
}
