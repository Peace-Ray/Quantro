package com.peaceray.quantro.utils.simulatedarray;

class SimulatedConstantArray extends SimulatedArray {
	
	int mLen ;
	
	int mValInt ;
	double mValDouble ;
	boolean mValIsInt ;
	
	SimulatedConstantArray( int len, int val ) {
		mLen = len ;
		mValInt = val ;
		mValIsInt = true ;
	}
	
	SimulatedConstantArray( int len, double val ) {
		mLen = len ;
		mValDouble = val ;
		mValIsInt = false ;
	}

	@Override
	public int getInt(int index) {
		if ( index < 0 || ( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		return mValIsInt ? mValInt : (int)Math.round( mValDouble ) ;
	}

	@Override
	public double getDouble(int index) {
		if ( index < 0 ||( mLen >= 0 && index >= mLen ) )
			throw new ArrayIndexOutOfBoundsException("Length " + mLen + ", index " + index) ;
		return mValIsInt ? mValInt : mValDouble ;
	}
	
	@Override
	public int size() {
		return mLen ;
	}

}
