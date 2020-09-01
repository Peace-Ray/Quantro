package com.peaceray.quantro.utils.interpolation;


public class LinearInterpolation extends Interpolation {
	

	
	private int mTypeVal ;
	
	private int [] mValInt ;
	private long [] mValLong ;
	private float [] mValFloat ;
	private double [] mValDouble ;
	
	
	public LinearInterpolation( int [] vals, int behavior ) {
		super( vals.length, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( long [] vals, int behavior ) {
		super( vals.length, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( float [] vals, int behavior ) {
		super( vals.length, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( double [] vals, int behavior ) {
		super( vals.length, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( int [] vals, int [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( long [] vals, int [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( float [] vals, int [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( double [] vals, int [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( int [] vals, long [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( long [] vals, long [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( float [] vals, long [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( double [] vals, long [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( int [] vals, float [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( long [] vals, float [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( float [] vals, float [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( double [] vals, float [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( int [] vals, double [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( long [] vals, double [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( float [] vals, double [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	public LinearInterpolation( double [] vals, double [] pos, int behavior ) {
		super( pos, behavior ) ; setVals(vals) ;		checkLens() ; }
	
	
	public void setValueAtIndex( int index, int val ) {
		switch( mTypeVal ) {
		case TYPE_INT:
			mValInt[index] = val ;
			break ;
		case TYPE_LONG:
			mValLong[index] = val ;
			break ;
		case TYPE_FLOAT:
			mValFloat[index] = val ;
			break ;
		case TYPE_DOUBLE:
			mValDouble[index] = val ;
			break ;
		}
	}
	
	public void setValueAtIndex( int index, long val ) {
		switch( mTypeVal ) {
		case TYPE_INT:
			mValInt[index] = (int)val ;
			break ;
		case TYPE_LONG:
			mValLong[index] = val ;
			break ;
		case TYPE_FLOAT:
			mValFloat[index] = val ;
			break ;
		case TYPE_DOUBLE:
			mValDouble[index] = val ;
			break ;
		}
	}
	
	public void setValueAtIndex( int index, float val ) {
		switch( mTypeVal ) {
		case TYPE_INT:
			mValInt[index] = (int)Math.round(val) ;
			break ;
		case TYPE_LONG:
			mValLong[index] = (int)Math.round(val) ;
			break ;
		case TYPE_FLOAT:
			mValFloat[index] = val ;
			break ;
		case TYPE_DOUBLE:
			mValDouble[index] = val ;
			break ;
		}
	}
	
	public void setValueAtIndex( int index, double val ) {
		switch( mTypeVal ) {
		case TYPE_INT:
			mValInt[index] = (int)Math.round(val) ;
			break ;
		case TYPE_LONG:
			mValLong[index] = (int)Math.round(val) ;
			break ;
		case TYPE_FLOAT:
			mValFloat[index] = (float)val ;
			break ;
		case TYPE_DOUBLE:
			mValDouble[index] = val ;
			break ;
		}
	}
	
	
	
	
	private void setVals( int [] vals ) {
		if ( vals == null || vals.length < 2 )
			throw new IllegalArgumentException("Must provide an array of at least 2 values") ;
		mValInt = new int[vals.length] ;
		for ( int i = 0; i <vals.length; i++ )
			mValInt[i] = vals[i] ;
		mTypeVal = TYPE_INT ;
	}
	
	private void setVals( long [] vals ) {
		if ( vals == null || vals.length < 2 )
			throw new IllegalArgumentException("Must provide an array of at least 2 values") ;
		mValLong = new long[vals.length] ;
		for ( int i = 0; i <vals.length; i++ )
			mValLong[i] = vals[i] ;
		mTypeVal = TYPE_LONG ;
	}
	
	private void setVals( float [] vals ) {
		if ( vals == null || vals.length < 2 )
			throw new IllegalArgumentException("Must provide an array of at least 2 values") ;
		mValFloat = new float[vals.length] ;
		for ( int i = 0; i <vals.length; i++ )
			mValFloat[i] = vals[i] ;
		mTypeVal = TYPE_FLOAT ;
	}
	
	private void setVals( double [] vals ) {
		if ( vals == null || vals.length < 2 )
			throw new IllegalArgumentException("Must provide an array of at least 2 values") ;
		mValDouble = new double[vals.length] ;
		for ( int i = 0; i <vals.length; i++ )
			mValDouble[i] = vals[i] ;
		mTypeVal = TYPE_DOUBLE ;
	}
	
	
	private void checkLens( ) {
		int valLen, posLen ;
		switch( mTypeVal ) {
		case TYPE_INT:
			valLen = mValInt.length ;
			break ;
		case TYPE_LONG:
			valLen = mValLong.length ;
			break ;
		case TYPE_FLOAT:
			valLen = mValFloat.length ;
			break ;
		case TYPE_DOUBLE:
			valLen = mValDouble.length ;
			break ;
		default:
			throw new RuntimeException("Strange error in checkLens") ;
		}
		
		switch( mTypePos ) {
		case TYPE_INT:
			posLen = mPosInt.length ;
			break ;
		case TYPE_LONG:
			posLen = mPosLong.length ;
			break ;
		case TYPE_FLOAT:
			posLen = mPosFloat.length ;
			break ;
		case TYPE_DOUBLE:
			posLen = mPosDouble.length ;
			break ;
		default:
			throw new RuntimeException("Strange error in checkLens") ;
		}
		
		if ( valLen != posLen )
			throw new IllegalArgumentException("Values and positions must have same length") ;
	}
	


	
	
	
	
	protected double interpolateDouble( int index, double partial) {
		switch( mTypeVal ) {
		case TYPE_INT:
			if ( index >= mValInt.length-1 )
				return mValInt[mValInt.length-1] ;
			return mValInt[index] + partial*(mValInt[index+1] - mValInt[index]) ;
			
		case TYPE_FLOAT:
			if ( index >= mValFloat.length-1 )
				return mValFloat[mValFloat.length-1] ;
			return mValFloat[index] + partial*(mValFloat[index+1] - mValFloat[index]) ;
			
		case TYPE_LONG:
			if ( index >= mValLong.length-1 )
				return mValLong[mValLong.length-1] ;
			return mValLong[index] + partial*(mValLong[index+1] - mValLong[index]) ;
			
		case TYPE_DOUBLE:
			if ( index >= mValDouble.length-1 )
				return mValDouble[mValDouble.length-1] ;
			return mValDouble[index] + partial*(mValDouble[index+1] - mValDouble[index]) ;
			
		default:
			throw new RuntimeException("mTypeVal invalid") ;
		}
	}
	
	protected float interpolateFloat( int index, double partial) {
		return (float) interpolateDouble( index, partial ) ;
	}
	
	
	protected long interpolateLong( int index, double partial ) {
		switch( mTypeVal ) {
		case TYPE_INT:
			if ( index >= mValInt.length-1 )
				return mValInt[mValInt.length-1] ;
			return (long)(mValInt[index] + Math.round(partial*(mValInt[index+1] - mValInt[index]))) ;
			
		case TYPE_FLOAT:
			if ( index >= mValFloat.length-1 )
				return (long)Math.round(mValFloat[mValFloat.length-1]) ;
			return (long)(mValFloat[index] + Math.round(partial*(mValFloat[index+1] - mValFloat[index]))) ;
			
		case TYPE_LONG:
			if ( index >= mValLong.length-1 )
				return mValLong[mValLong.length-1] ;
			return (long)(mValLong[index] + Math.round(partial*(mValLong[index+1] - mValLong[index]))) ;
			
		case TYPE_DOUBLE:
			if ( index >= mValDouble.length-1 )
				return (long)Math.round(mValDouble[mValDouble.length-1]) ;
			return (long)(mValDouble[index] + Math.round(partial*(mValDouble[index+1] - mValDouble[index]))) ;
			
		default:
			throw new RuntimeException("mTypeVal invalid") ;
		}
	}
	
	protected int interpolateInt( int index, double partial) {
		return (int) interpolateLong( index, partial ) ;
	}

}
