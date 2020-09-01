package com.peaceray.quantro.utils.simulatedarray;

/**
 * An array with explicit entries provided as an integer or floating-point list.
 * The difference between this and a true array is that positive indices greater
 * than the length are acceptable; they will be "pinned" to the largest valid index
 * and the resulting value returned.
 * 
 * @author Jake
 *
 */
class SimulatedExplicitPinningArray extends SimulatedArray {
	
	int mLen ;
	
	int [] mValInt ;
	double [] mValDouble ;
	boolean mValIsInt ;
	
	/**
	 * Create a new PinningArray containing the provided values.  The length
	 * specified may be:
	 * 
	 *  	Negative, in which case there is no simulated length
	 * 			limit and all positive indices are allowed.
	 * 		Non-negative, in which case any attempt to index beyond
	 * 			the length will result in an ArrayIndexOutOfBoundsException.
	 * 			If len > values.length; any index in [values.length, len) will
	 * 			hold values[values.length-1].
	 * @param len
	 * @param values
	 */
	SimulatedExplicitPinningArray( int len, int [] values ) {
		mLen = len ;
		mValInt = values ;
		mValIsInt = true ;
	}
	
	SimulatedExplicitPinningArray( int len, double [] values ) {
		mLen = len ;
		mValDouble = values ;
		mValIsInt = false ;
	}
	
	

	@Override
	public int getInt(int index) {
		if ( index < 0 || ( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		if ( mValIsInt ) {
			if ( index < mValInt.length )
				return mValInt[index] ;
			return mValInt[mValInt.length-1] ;
		} else {
			if ( index < mValDouble.length )
				return (int)Math.round(mValDouble[index]) ;
			return (int)Math.round(mValDouble[mValDouble.length-1]) ;
		}
	}

	@Override
	public double getDouble(int index) {
		if ( index < 0 || ( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		if ( mValIsInt ) {
			if ( index < mValInt.length )
				return mValInt[index] ;
			return mValInt[mValInt.length-1] ;
		} else {
			if ( index < mValDouble.length )
				return mValDouble[index] ;
			return mValDouble[mValDouble.length-1] ;
		}
	}
	
	@Override
	public int size() {
		return mLen ;
	}

}
