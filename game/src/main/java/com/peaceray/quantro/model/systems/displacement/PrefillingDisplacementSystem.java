package com.peaceray.quantro.model.systems.displacement;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;


/**
 * A PrefillingDisplacementSystem is an abstract implementation of
 * a certain set of DisplacementSystem methods.  It is meant to perform
 * some basic common functions that any generalized displacement system
 * would need.
 * 
 * @author Jake
 *
 */
public abstract class PrefillingDisplacementSystem extends DisplacementSystem {
	
	private static final int [] RANDOM_NUMBERS =
		new	int	[]	{
		159,	258,	180,	 15,	 75,	 31,	189,	150,
		114,	217,	 45,	135,	239,	 74,	 36,	113,
		184,	137,	204,	134,	 84,	  3,	111,	174,
		168,	 49,	 14,	237,	229,	 66,	241,	 62,
		117,	 68,	162,	268,	247,	273,	218,	 44,
		228,	  7,	 94,	219,	164,	 95,	  4,	188,
		116,	 10,	112,	215,	200,	214,	196,	  0,
		156,	249,	121,	220,	259,	 16,	 30,	 67,
		110,	208,	 82,	131,	144,	126,	160,	 25,
		278,	 18,	167,	127,	 80,	 53,	143,	 38,
		130,	243,	179,	153,	 72,	118,	 52,	 43,
		186,	202,	140,	 60,	129,	115,	141,	244,
		197,	191,	133,	  9,	101,	223,	152,	 39,
		109,	 34,	 89,	276,	181,	252,	267,	224,
		206,	183,	 12,	233,	270,	100,	216,	 78,
		226,	149,	 42,	193,	190,	 20,	234,	 83,
		240,	169,	172,	 33,	151,	147,	198,	 65,
		  1,	 63,	123,	 50,	 76,	 48,	138,	227,
		209,	 35,	 17,	105,	242,	 88,	246,	104,
		211,	263,	182,	 32,	271,	232,	 99,	 87,
		207,	 77,	 46,	178,	235,	132,	275,	 69,
		 85,	 61,	 81,	145,	103,	146,	163,	157,
		 11,	 47,	  2,	 90,	274,	124,	254,	221,
		264,	119,	122,	139,	 93,	158,	262,	 13,
		255,	155,	261,	257,	128,	107,	120,	199,
		 57,	 55,	272,	185,	 91,	266,	 51,	 59,
		 98,	236,	 28,	 97,	 22,	187,	256,	171,
		210,	177,	148,	225,	170,	 41,	173,	 19,
		 58,	165,	  8,	  6,	251,	201,	 92,	203,
		 23,	 37,	 24,	 86,	 73,	260,	 79,	265,
		230,	 96,	125,	250,	166,	 40,	136,	154,
		212,	248,	194,	102,	 26,	253,	 71,	 56,
		279,	161,	106,	 21,	238,	277,	  5,	205,
		 29,	176,	269,	 70,	 54,	231,	195,	222,
		245,	213,	 27,	175,	142,	 64,	192,	108
		}	;
	// "Random numbers"; teh numbers 0...279, in a randomized
	// order.  This is is here to allow "random" behavior without
	// random number generation.  The number of entries is evenly
	// divisible by 2, 7, 8, 10, making it appropriate for
	// fully pseudorandom selection within S0/S1, R0...R6, Quantro columns,
	// Retro columns.
	
	private static final long A_PRIME = 101 ;
	private static final long B_PRIME = 103 ;
	private static final long C_PRIME = 37 ;
	private static final long D_PRIME = 17 ;
	private static final long E_PRIME = 13 ;
	private static final long F_PRIME = 11 ;
	private static final int [] SWISS_CHEESE =
		new int [] {0,1,1,0,1,1,0,1,			
					0,1,0,1,0,1,0,1,
					1,1,0,1,1,1,0,1,
					1,0,1,0,1,0,1,1,
					0,1,1,0,1,1,0,1,
					1,1,0,1,1,0,1,1,
					0,1,1,0,1,1,1,0,
					1,0,1,0,1,1,0,1,
					1,1,0,1,0,1,1,1,
					1,0,1,1,0,1,0,1,
					1,0,1,0,1,0,1,1,
					0,1,1,0,1,0,1,1,
					1,1,0,1,1,0,1,1,
					0,1,0,1,0,1,1,0,
					1,1,1,0,1,1,1,0,
					1,0,1,0,1,1,1,0,
					1,1,0,1,1,0,1,0,
					1,0,1,1,1,0,1,0, 
					1,0,1,1,0,1,1, 
				} ;
	// determined by die roll.  Guaranteed to be random.
	// Actually, the above is a set of random binary numbers
	// from random.org, with runs of zeros reduced to 1.
	// This is intended to give random "looking" swiss
	// cheese in an easily repeatable way.
	// The length is prime (it is 151).
	//
	// Why not just use Random?  Because we are being future-looking,
	// and recognize that it is very difficult to keep Random
	// objects running on different machines "in-sync".  This
	// implementation allows the behavior of the garbage rows
	// to *appear* random, while actually being fully deterministic
	// based on the piece type and piece column.
	//
	// When filling garbage rows with swiss cheese, we use the formula
	// (column + A_PRIME*(rowsGenerated (*2 if 3d) + totalRowsAddedThisGeneration) % SWISS_CHEESE.length
	// as the initial index into the above, then step along as far
	// as is required.  rowsGenerated is doubled in 3d cases to allow 
	// for independent generation of qPanes 0 and 1.
	
	
	public enum Style {
		/**
		 * Complex, swiss-cheese block placement.  Some gaps, some not.
		 */
		SWISS_CHEESE,
		
		/**
		 * A full row except for a one-block gap in a random column.
		 * The gap is the same between qPanes for 3d games.
		 */
		ONE_BLOCK_GAP,
		
		/**
		 * A full row except for a one-block gap in a random column.
		 * The gap is the same between qPanes for 3d games.  It will
		 * not repeat from one row to the next.
		 */
		ONE_BLOCK_GAP_NONREPEATING
	}
	
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	private Style mStyle ;
	
	private State mState = null ;
	
	
	// Constructerrr
	public PrefillingDisplacementSystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		this.mStyle = Style.ONE_BLOCK_GAP_NONREPEATING ;
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
	
	@Override
	public PrefillingDisplacementSystem setPseudorandom( int pseudorandom ) {
		mState.setPseudorandom(pseudorandom) ;
		return this ;
	}
	
	
	protected State getState() {
		return mState ;
	}
	
	protected void setState( State state ) {
		mState = state ;
	}
	
	
	@Override
	public void prefill(byte[][][] displacementRows) {
		fill( displacementRows, 0, displacementRows[0].length ) ;
	}
	
	private void fill( byte [][][] displacementRows, int minRow, int boundRow ) {
		
		// We don't allow a whole lot of customization for this.  For 2D games,
		// we apply randomized Rainbow blocks an a swiss-cheese pattern.
		// For 3D games, we apply qPane-specific blocks in a swiss-cheese
		// pattern which differs between qPanes.
		
		boolean is3d = GameModes.numberQPanes(ginfo) == 2 ;
		long rowsPrev = mState.getRowsGenerated() ;
		int pseudorandom = mState.getPseudorandom() ;
		
		byte qo ;
		int qc ;
		
		switch ( mStyle ) {
		case SWISS_CHEESE:
			// fill with swiss-cheese blocks (that differ between panes for 3d games).
			if ( is3d ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					qo = qp == 0 ? QOrientations.S0 : QOrientations.S1 ;
					for ( int r = minRow; r < boundRow; r++ ) {
						for ( int c = 0; c < displacementRows[0][0].length; c++ ) {
							long randval0 = c + rowsPrev * A_PRIME
								+ (r + qp * (boundRow-minRow)) * B_PRIME
								+ pseudorandom * C_PRIME ;
							
							int index = (int)(randval0 % SWISS_CHEESE.length) ;
							displacementRows[qp][r][c] = SWISS_CHEESE[index] == 1
									? qo : QOrientations.NO ;
						}
					}
				}
			} else {
				for ( int r = minRow; r < boundRow; r++ ) {
					for ( int c = 0; c < displacementRows[0][0].length; c++ ) {
						long randval0 = c + rowsPrev * A_PRIME
							+ r * B_PRIME
							+ pseudorandom * C_PRIME ;
						long randval1 = c + rowsPrev * D_PRIME
							+ r * E_PRIME 
							+ pseudorandom * F_PRIME ;
					
						
						int index0 = (int)(randval0 % SWISS_CHEESE.length) ;
						int index1 = (int)(randval1 % RANDOM_NUMBERS.length) ;
						// qo is from R0 to R6; that is, R0 + a random [0,...,6].
						qc = (QCombinations.R0 + (RANDOM_NUMBERS[index1] % 7)) ;
						if ( SWISS_CHEESE[index0] == 0 )
							qc = QCombinations.NO ;
						QCombinations.setAs(displacementRows, r, c, qc) ;
					}
				}
			}
			break ;
			
		case ONE_BLOCK_GAP:
		case ONE_BLOCK_GAP_NONREPEATING:
			// fill with blocks except for a one-block gap (that is the
			// same between qPanes).
			qc = is3d ? QCombinations.SS : QCombinations.RAINBOW_BLAND ;
			for ( int r = minRow; r < boundRow; r++ ) {
				long randval = r * A_PRIME + rowsPrev * B_PRIME + pseudorandom * C_PRIME ;
				int emptyCol = RANDOM_NUMBERS[(int)(randval%RANDOM_NUMBERS.length)] % displacementRows[0][0].length ;
				if ( mStyle == Style.ONE_BLOCK_GAP_NONREPEATING && emptyCol == mState.getColumnGap() ) {
					randval = emptyCol * A_PRIME + (r+1) * B_PRIME + (rowsPrev+1) * C_PRIME + pseudorandom * D_PRIME ;
					emptyCol = RANDOM_NUMBERS[(int)(randval%RANDOM_NUMBERS.length)] % ( displacementRows[0][0].length -1 );
					if ( emptyCol >= mState.getColumnGap() )
						emptyCol++ ;
				}
				for ( int c = 0; c < displacementRows[0][0].length; c++ ) {
					QCombinations.setAs(displacementRows, r, c, c == emptyCol ? QCombinations.NO : qc) ;
				}
				mState.setColumnGap(emptyCol) ;
			}
			break ;
		}
		
		mState.addRowsGenerated(boundRow - minRow) ;
	}
	
	
	protected void transfer( byte[][][] blockfield, byte[][][] displacementRows, int rows ) {
		// shift up the blockfield
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int r = blockfield[0].length -1; r >= rows; r-- ) {
				ArrayOps.copyInto( blockfield[qp][r-rows], blockfield[qp][r] ) ;
			}
		}
		
		// copy from ready-made displacement data
		int dHeight = displacementRows[0].length ;
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int r = 0; r < rows; r++ ) {
				// copy displacement row r (counted DOWN from the TOP)
				// into blockfield row r (counted DOWN from 'rows')
				ArrayOps.copyInto(
						displacementRows[qp][dHeight - r - 1],
						blockfield[qp][rows - r - 1] ) ;
			}
		}
		
		// shift displacement data up
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int r = displacementRows[0].length -1; r >= rows; r-- ) {
				ArrayOps.copyInto( displacementRows[qp][r-rows], displacementRows[qp][r] ) ;
			}
		}
		
		mState.addRowsTransferred(rows) ;
		
		// fill the bottom 'rows' with new data.
		fill( displacementRows, 0, rows ) ;
	}
	
	
	// This class is 'public' and non-abstract because anything else results
	// in serialization errors.
	public interface State {
		
		public void setPseudorandom( int pseudorandom ) ;
		
		public int getPseudorandom() ;
		
		public void setRowsTransferred( long rowsTransferred ) ;
		
		public long getRowsTransferred() ;
		
		public void addRowsTransferred( long trans ) ;
		
		public void setRowsGenerated( long rowsGenerated ) ;
		
		public long getRowsGenerated() ;
		
		public long addRowsGenerated( long rows ) ;
		
		public void setColumnGap( int col ) ;
		
		public int getColumnGap() ;
		
	}
}
