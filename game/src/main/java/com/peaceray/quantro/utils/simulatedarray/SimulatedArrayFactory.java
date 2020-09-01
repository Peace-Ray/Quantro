package com.peaceray.quantro.utils.simulatedarray;

public class SimulatedArrayFactory {
	
	public static SimulatedArray copy( SimulatedArray ar ) {
		if ( ar == null )
			return null ;
		
		if ( ar instanceof SimulatedConstantArray ) {
			SimulatedConstantArray sca = (SimulatedConstantArray)ar ;
			if ( sca.mValIsInt )
				return new SimulatedConstantArray( sca.mLen, sca.mValInt ) ;
			else
				return new SimulatedConstantArray( sca.mLen, sca.mValDouble ) ;
		}
		
		if ( ar instanceof SimulatedExplicitPinningArray ) {
			SimulatedExplicitPinningArray sepa = (SimulatedExplicitPinningArray)ar ;
			if ( sepa.mValIsInt ) {
				int [] vals = new int[sepa.mValInt.length] ;
				for ( int i = 0; i < vals.length; i++ )
					vals[i] = sepa.mValInt[i] ;
				return new SimulatedExplicitPinningArray( sepa.mLen, vals ) ;
			} else {
				double [] vals = new double[sepa.mValDouble.length] ;
				for ( int i = 0; i < vals.length; i++ )
					vals[i] = sepa.mValDouble[i] ;
				return new SimulatedExplicitPinningArray( sepa.mLen, vals ) ;
			}
		}
		
		if ( ar instanceof SimulatedSlopeInterceptArray ) {
			SimulatedSlopeInterceptArray ssia = (SimulatedSlopeInterceptArray)ar ;
			return new SimulatedSlopeInterceptArray( ssia.mLen, ssia.mSlope, ssia.mIntercept ) ;
		}
		
		if ( ar instanceof SimulatedSlopeInterceptPinningArray ) {
			SimulatedSlopeInterceptPinningArray ssipa = (SimulatedSlopeInterceptPinningArray)ar ;
			return new SimulatedSlopeInterceptPinningArray( ssipa.mLen, ssipa.mSlope, ssipa.mIntercept, ssipa.mPinnedToLength ) ;
		}
		
		throw new IllegalArgumentException("Don't know how to copy " + ar) ;
	}

	public static SimulatedArray newConstant( int length, int value ) {
		return new SimulatedConstantArray( length, value ) ;
	}
	
	public static SimulatedArray newConstant( int length, double value ) {
		return new SimulatedConstantArray( length, value ) ;
	}
	
	public static SimulatedArray newSlopeIntercept( int length, double slope, double intercept ) {
		return new SimulatedSlopeInterceptArray( length, slope, intercept ) ;
	}
	
	public static SimulatedArray newSlopeInterceptPinning( int length, double slope, double intercept, int pinnedLength ) {
		return new SimulatedSlopeInterceptPinningArray( length, slope, intercept, pinnedLength ) ;
	}
	
	public static SimulatedArray newExplicitPinning( int length, int [] values, int valuesLen ) {
		int [] tempValues = new int[valuesLen] ;
		for ( int i = 0; i < valuesLen; i++ )
			tempValues[i] = values[i] ;
		return new SimulatedExplicitPinningArray( length, tempValues ) ;
	}

	public static SimulatedArray newExplicitPinning( int length, float [] values, int valuesLen ) {
		double [] tempValues = new double[valuesLen] ;
		for ( int i = 0; i < valuesLen; i++ )
			tempValues[i] = values[i] ;
		return new SimulatedExplicitPinningArray( length, tempValues ) ;
	}
	
	public static SimulatedArray newExplicitPinning( int length, double [] values, int valuesLen ) {
		double [] tempValues = new double[valuesLen] ;
		for ( int i = 0; i < valuesLen; i++ )
			tempValues[i] = values[i] ;
		return new SimulatedExplicitPinningArray( length, tempValues ) ;
	}
	
	
}
