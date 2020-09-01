package com.peaceray.quantro.utils.simulatedarray;

class SimulatedSlopeInterceptArray extends SimulatedArray {
	
	int mLen ;
	
	double mSlope, mIntercept ;
	
	SimulatedSlopeInterceptArray( int len, double slope, double intercept ) {
		mLen = len ;
		mSlope = slope ;
		mIntercept = intercept ;
	}

	@Override
	public int getInt(int index) {
		if ( index < 0 || ( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		return (int)Math.round( mSlope * index + mIntercept ) ;
	}

	@Override
	public double getDouble(int index) {
		if ( index < 0 || ( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		return mSlope * index + mIntercept ;
	}
	
	@Override
	public int size() {
		return mLen ;
	}

}
