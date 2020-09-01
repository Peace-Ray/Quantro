package com.peaceray.quantro.utils.interpolation;


/**
 * Interpolates between exactly two values (A and B), at two positions
 * p1 and p2.  For p in [p1, p2], the value is determined by:
 * 
 * A + (B-A)*alpha
 *
 * Where 'alpha' is
 * 
 * (1 - cosine( PI * (p-p1)/(p2-p1) ))/2 ;
 * 
 * We can reduce the number of operations using
 * 
 * (1/2)(A+B) + (1/2)(A-B)*(w) where
 * 
 * w = cosine( PI * (p-p1)/(p2-p1) )
 * 
 * Thus at p=p1, cosine( 0 ) = 1 = w and 
 * 
 * val = (1/2)(A+B) + (1/2)(A-B) = A
 * 
 * and at p = p2, w = cosine( PI ) = -1 and
 * 
 * val = (1/2)(A+B) - (1/2)(A-B) = B.
 * 
 * Therefore, to save time, we store (1/2)(A+B) and (1/2)(A-B) as our values.
 * We store these as doubles, regardless of the values over which we operate.
 * 
 * 
 * This function smoothly interpolates betwen A to B
 * such that at p1 and p2 the slope is zero.  The slope is sharpest
 * at (p1+p2)/2, which is also the halfway point between values.  If
 * MIRROR behavior is used, this is a looping "circle" behavior that shouldn't
 * have any rough animation frames; it should all appear smooth as the
 * value oscilates between 0 and 1.
 * 
 * CLAMP behavior should also be a smooth animation.  Only REPEAT will produce
 * a discontinuous rate of change (and in fact a discontinuous value).
 * 
 * @author Jake
 *
 */
public class CosineInterpolation extends Interpolation {
	
	private double [] mScaledAB ;
	
	private int mTypeVal ;
	private int [] mValInt ;
	private long [] mValLong ;
	private float [] mValFloat ;
	private double [] mValDouble ;
	
	
	
	public CosineInterpolation( int A, int B, int behavior ) {
		super( 2, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( long A, long B, int behavior ) {
		super( 2, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( float A, float B, int behavior ) {
		super( 2, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( double A, double B, int behavior ) {
		super( 2, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( int A, int B, int p1, int p2, int behavior ) {
		super( new int[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( long A, long B, int p1, int p2, int behavior ) {
		super( new int[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( float A, float B, int p1, int p2, int behavior ) {
		super( new int[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( double A, double B, int p1, int p2, int behavior ) {
		super( new int[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( int A, int B, long p1, long p2, int behavior ) {
		super( new long[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( long A, long B, long p1, long p2, int behavior ) {
		super( new long[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( float A, float B, long p1, long p2, int behavior ) {
		super( new long[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( double A, double B, long p1, long p2, int behavior ) {
		super( new long[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( int A, int B, float p1, float p2, int behavior ) {
		super( new float[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( long A, long B, float p1, float p2, int behavior ) {
		super( new float[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( float A, float B, float p1, float p2, int behavior ) {
		super( new float[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( double A, double B, float p1, float p2, int behavior ) {
		super( new float[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( int A, int B, double p1, double p2, int behavior ) {
		super( new double[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( long A, long B, double p1, double p2, int behavior ) {
		super( new double[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( float A, float B, double p1, double p2, int behavior ) {
		super( new double[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	public CosineInterpolation( double A, double B, double p1, double p2, int behavior ) {
		super( new double[]{p1, p2}, behavior ) ; setVals(A,B) ;	}
	
	
	private void setVals( int A, int B ) {
		mTypeVal = TYPE_INT ;
		mValInt = new int[]{A,B} ;
		
		double halfA = 0.5*A ;
		double halfB = 0.5*B ;
		mScaledAB = new double[]{ halfA+halfB, halfA-halfB } ;
	}
	
	private void setVals( long A, long B ) {
		mTypeVal = TYPE_LONG ;
		mValLong= new long[]{A,B} ;
		
		double halfA = 0.5*A ;
		double halfB = 0.5*B ;
		mScaledAB = new double[]{ halfA+halfB, halfA-halfB } ;
	}
	
	private void setVals( float A, float B ) {
		mTypeVal = TYPE_FLOAT ;
		mValFloat = new float[]{A,B} ;
		
		double halfA = 0.5*A ;
		double halfB = 0.5*B ;
		mScaledAB = new double[]{ halfA+halfB, halfA-halfB } ;
	}
	
	private void setVals( double A, double B ) {
		mTypeVal = TYPE_DOUBLE ;
		mValDouble = new double[]{A,B} ;
		
		double halfA = 0.5*A ;
		double halfB = 0.5*B ;
		mScaledAB = new double[]{ halfA+halfB, halfA-halfB } ;
	}
	

	@Override
	protected double interpolateDouble(int index, double partial) {
		if ( partial == 0 ) {
			switch( mTypeVal ) {
			case TYPE_INT:
				return mValInt[index] ;
			case TYPE_LONG:
				return mValLong[index] ;
			case TYPE_FLOAT:
				return mValFloat[index] ;
			case TYPE_DOUBLE:
				return mValDouble[index] ;
			}
		}
		
		// (1/2)(A+B) + (1/2)(A-B)*(w) where
		// w = cosine( PI * (p-p1)/(p2-p1) )
		return mScaledAB[0] + mScaledAB[1] * Math.cos( Math.PI * partial ) ;
	}

	@Override
	protected float interpolateFloat(int index, double partial) {
		if ( partial == 0 ) {
			switch( mTypeVal ) {
			case TYPE_INT:
				return mValInt[index] ;
			case TYPE_LONG:
				return mValLong[index] ;
			case TYPE_FLOAT:
				return mValFloat[index] ;
			case TYPE_DOUBLE:
				return (float)mValDouble[index] ;
			}
		}
		
		// (1/2)(A+B) + (1/2)(A-B)*(w) where
		// w = cosine( PI * (p-p1)/(p2-p1) )
		return (float)(mScaledAB[0] + mScaledAB[1] * Math.cos( Math.PI * partial )) ;
	}

	@Override
	protected long interpolateLong(int index, double partial) {
		if ( partial == 0 ) {
			switch( mTypeVal ) {
			case TYPE_INT:
				return mValInt[index] ;
			case TYPE_LONG:
				return mValLong[index] ;
			case TYPE_FLOAT:
				return Math.round(mValFloat[index]) ;
			case TYPE_DOUBLE:
				return Math.round(mValDouble[index]) ;
			}
		}
		
		// (1/2)(A+B) + (1/2)(A-B)*(w) where
		// w = cosine( PI * (p-p1)/(p2-p1) )
		return Math.round(mScaledAB[0] + mScaledAB[1] * Math.cos( Math.PI * partial )) ;
	}

	@Override
	protected int interpolateInt(int index, double partial) {
		if ( partial == 0 ) {
			switch( mTypeVal ) {
			case TYPE_INT:
				return mValInt[index] ;
			case TYPE_LONG:
				return (int)mValLong[index] ;
			case TYPE_FLOAT:
				return Math.round(mValFloat[index]) ;
			case TYPE_DOUBLE:
				return (int)Math.round(mValDouble[index]) ;
			}
		}
		
		// (1/2)(A+B) + (1/2)(A-B)*(w) where
		// w = cosine( PI * (p-p1)/(p2-p1) )
		return (int)Math.round(mScaledAB[0] + mScaledAB[1] * Math.cos( Math.PI * partial )) ;
	}

}
