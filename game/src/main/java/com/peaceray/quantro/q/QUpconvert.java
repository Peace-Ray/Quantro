package com.peaceray.quantro.q;

/**
 * Version 1.0 adds a few more QOrientations and Combinations.  This unfortunately
 * requires re-labeling some of the existing QCombinations, although all previous
 * QOrientations remain the same.
 * 
 * In most cases we don't care about this, since we store QOrientations and we only
 * added to them, not redefined them.  However, in a few cases this change is significant;
 * for example, piece types contain an encoded QCombination (not QOrientation) and that
 * needs to be upconverted.
 * 
 * Upconversion is a risky venture; there are no explicit checks in place to ensure
 * that the value to be convert has not already been converted.
 * 
 * Be very careful in your application of these class methods.
 * 
 * @author Jake
 *
 */
public class QUpconvert {
	
	private static final int PREV_QORIENTATIONS_MAX = 22 ;		// QOrientations.RAINBOW_BLAND ;
	private static final int PREV_QCOMBINATIONS_MAX = 38 ;

	public static final int upConvert( int qc ) {
		// Before this, our maximum QOrientation was RAINBOW_BLAND.  QCombinations
		// which are not equivalent to a QOrientation
		// were represented as RAINBOW_BLAND + X, with X >= 1.
		//
		// We have revised this; now QCombinations not equivalent to a QOrienation
		// are represented as 100 - X (the same X as above).
		
		// NOTE: It is possible to sanity-check a QCombination, but doing so
		// can result in false positives or negatives.
		if ( !allowsUpconversionAllowFalsePositives(qc) )
			return qc ;
		
		if ( qc <= PREV_QORIENTATIONS_MAX )
			return qc ;
		
		int X = qc - PREV_QORIENTATIONS_MAX ;
		return 100 - X ;
	}
	
	
	/**
	 * Returns whether the specified QCombination is in need of upconversion.
	 * False negatives are possible (returning 'false' for a value in need
	 * of upconversion).
	 * @param qc
	 * @return
	 */
	public static final boolean needsUpconversionAllowFalseNegatives( int qc ) {
		// A value is in need of upconversion if it is not a valid
		// QCombination.  The only values we expect to see fitting this description
		// are those which are > QOrientations.NUM but less than QCombinations.
		if ( qc < QOrientations.NUM )
			return false ;
		if ( qc >= 100 - QCombinations.NUM_NOT_QORIENTATIONS )
			return false ;
		
		return true ;
	}
	
	
	/**
	 * Returns whether the specied QCombination is in need of upconversion.
	 * False positives are possible (returning 'true' for a value NOT in need
	 * of upconversion).
	 * @param qc
	 * @return
	 */
	public static final boolean allowsUpconversionAllowFalsePositives( int qc ) {
		// Any value greater than RAINBOW_BLAND, and less than the previous maximum
		// QCombination, is potentially in need of conversion.
		if ( qc <= PREV_QORIENTATIONS_MAX )
			return false ;
		if ( qc > PREV_QCOMBINATIONS_MAX )
			return false ;
		
		return true ;
	}
	
}
