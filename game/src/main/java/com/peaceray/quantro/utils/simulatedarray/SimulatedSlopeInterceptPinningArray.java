package com.peaceray.quantro.utils.simulatedarray;

/**
 * Very similar to SimulatedExplicitPinningArray, except that the "top-bounds" of
 * the array has value = to the pinned limit.
 * 
 * @author Jake
 *
 */
public class SimulatedSlopeInterceptPinningArray extends
		SimulatedSlopeInterceptArray {
	
	int mPinnedToLength ;
	
	SimulatedSlopeInterceptPinningArray( int len, double slope, double intercept, int pinnedToLength ) {
		super( len, slope, intercept ) ;
		if ( pinnedToLength > len )
			throw new IllegalArgumentException("Must be pinned to a length < the maximum length!") ;
		mPinnedToLength = pinnedToLength ;
	}
	
	@Override
	public int getInt(int index) {
		if ( index < 0 || ( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		if ( index >= mPinnedToLength )
			return super.getInt(mPinnedToLength-1) ;
		return super.getInt(index) ;
	}

	@Override
	public double getDouble(int index) {
		if ( index < 0 || ( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		if ( index >= mPinnedToLength )
			return super.getDouble(mPinnedToLength-1) ;
		return super.getDouble(index) ;
	}

}
