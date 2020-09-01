package com.peaceray.quantro.q;

/**
 * A class with static constants defining the valid
 * QOrientations.
 * 
 * Safe assumptions:
 * QOrientations.NO == 0.
 * QOrientations.NUM <= 99
 * All QOrientations are < NUM.
 * 
 * @author Jake
 *
 */
public final class QOrientations {

	private static byte counter = 0 ;
	
	// Defines the QOrientations.  Note that QCombinations
	// uses these to construct more complex "blocks", such
	// as SS, UU, SF, etc.
	
	// Some standard blocks
	public static final byte NO = counter++ ;				// No block
	public static final byte S0 = counter++ ;				// State 1
	public static final byte S1 = counter++ ;				// State 2
	
	// Some special blocks
	public static final byte SL = counter++ ;				// Linked (white) 		3
	public static final byte SL_ACTIVE = counter++ ;		// Linked and active (will allow MONO-clears)
	public static final byte SL_INACTIVE = counter++ ;		// Linked and inactive (no monochromatic clears)
	public static final byte ST = counter++ ;				// Sticky (green)	6
	public static final byte ST_INACTIVE = counter++ ;		// Sticky, locked, inactive.	7
	public static final byte F0 = counter++ ;				// Flash (s1 pane)		8
	public static final byte F1 = counter++ ;				// Flash (s2 pane)
	public static final byte U0 = counter++ ;				// Unstable (s1 pane)	10
	public static final byte U1 = counter++ ;				// Unstable (s2 pane)
	public static final byte UL = counter++ ;				// Unstable but linked	12
	
	// Even more special: blocks connected to SL.
	public static final byte S0_FROM_SL = counter++ ;		// A S0 connected to SL.
	public static final byte S1_FROM_SL = counter++ ;		// A S1 connected to SL.	14
	
	// Some rainbow blocks!  They do nothing but look pretty.
	public static final byte R0 = counter++ ;			// 15
	public static final byte R1 = counter++ ;
	public static final byte R2 = counter++ ;
	public static final byte R3 = counter++ ;
	public static final byte R4 = counter++ ;
	public static final byte R5 = counter++ ;			// 20
	public static final byte R6 = counter++ ;			// 21
	
	public static final byte RAINBOW_BLAND = counter++ ;	// 22
	
	// Push Blocks!
	public static final byte PUSH_DOWN = counter++ ;
	public static final byte PUSH_DOWN_ACTIVE = counter++ ;
	public static final byte PUSH_UP = counter++ ;
	public static final byte PUSH_UP_ACTIVE = counter++ ;
	
	public static final int NUM = counter ;
	
	
	private static final Integer [] objects = new Integer[NUM] ;
	
	static {
		for ( int i = 0; i < NUM; i++ )
			objects[i] = new Integer(i) ;
	}
	
	public static final Integer getInteger(int qo) {
		return objects[qo] ;
	}
}
