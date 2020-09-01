package com.peaceray.quantro.utils.interpolation;


/**
 * Interpolation: an abstract class allowing smooth interpolation between values
 * at "positions."  Positions are ints, longs, floats or doubles (one of these, set at
 * instantiation) arranged in an array in ascending order.
 * 
 * Concrete implementations of this class need only implement constructors
 * (which call super(...), of course) and the methods
 * interpolateInt( int index, double partial ), interpolateFloat, etc.,
 * which return the appropriate value for interpolation at the position
 * 'partial' of the way from index to index+1.  'index' is a valid
 * index into the positions array; 'partial' is in [0,1), with 0 being
 * exactly at index and 1 exactly at index+1.
 * 
 * appropriate value 
 * 
 * @author Jake
 *
 */
public abstract class Interpolation {
	
	protected static final int TYPE_INT = 0 ;
	protected static final int TYPE_LONG = 1 ;
	protected static final int TYPE_FLOAT = 2 ;
	protected static final int TYPE_DOUBLE = 3 ;
	
	public static final int CLAMP = 0 ;
	public static final int REPEAT = 1 ;
	public static final int MIRROR = 2 ;
	
	protected int mBehavior ;
	protected int mTypePos ;
	protected int [] mPosInt ;
	protected long [] mPosLong ;
	protected float [] mPosFloat ;
	protected double [] mPosDouble ;
	
	protected Interpolation( int numPos, int behavior ) {
		if ( numPos < 1 )
			throw new IllegalArgumentException("Must provide at least 2 values") ;
		// set from 0 to 1.
		double [] pos = new double[numPos] ;
		for ( int i = 0; i < numPos; i++ ) {
			pos[i] = (double)i / (double)(numPos-1) ;
		}
		mPosDouble = pos ;
	}
	
	protected Interpolation( int [] positions, int behavior ) {
		if ( positions == null || positions.length < 2 )
			throw new IllegalArgumentException("Must provide at least 2 positions") ;
		mBehavior = behavior ;
		mPosInt = new int[positions.length] ;
		for ( int i = 0; i < positions.length; i++ )
			mPosInt[i] = positions[i] ;
		mTypePos = TYPE_INT ;
		checkPos() ;
	}
	
	protected Interpolation( long [] positions, int behavior ) {
		if ( positions == null || positions.length < 2 )
			throw new IllegalArgumentException("Must provide at least 2 positions") ;
		mBehavior = behavior ;
		mPosLong = new long[positions.length] ;
		for ( int i = 0; i < positions.length; i++ )
			mPosLong[i] = positions[i] ;
		mTypePos = TYPE_LONG ;
		checkPos() ;
	}
	
	protected Interpolation( float [] positions, int behavior ) {
		if ( positions == null || positions.length < 2 )
			throw new IllegalArgumentException("Must provide at least 2 positions") ;
		mBehavior = behavior ;
		mPosFloat = new float[positions.length] ;
		for ( int i = 0; i < positions.length; i++ )
			mPosFloat[i] = positions[i] ;
		mTypePos = TYPE_FLOAT ;
		checkPos() ;
	}
	
	protected Interpolation( double [] positions, int behavior ) {
		if ( positions == null || positions.length < 2 )
			throw new IllegalArgumentException("Must provide at least 2 positions") ;
		mBehavior = behavior ;
		mPosDouble = new double[positions.length] ;
		for ( int i = 0; i < positions.length; i++ )
			mPosDouble[i] = positions[i] ;
		mTypePos = TYPE_DOUBLE ;
		checkPos() ;
	}
	
	private void checkPos() {
		switch ( mTypePos ) {
		case TYPE_INT:
			for ( int i = 1; i < mPosInt.length; i++ )
				if ( mPosInt[i-1] >= mPosInt[i] )
					throw new RuntimeException("Positions must monotonically increase") ;
			break ;
		case TYPE_LONG:
			for ( int i = 1; i < mPosLong.length; i++ )
				if ( mPosLong[i-1] >= mPosLong[i] )
					throw new RuntimeException("Positions must monotonically increase") ;
			break ;
		case TYPE_FLOAT:
			for ( int i = 1; i < mPosFloat.length; i++ )
				if ( mPosFloat[i-1] >= mPosFloat[i] )
					throw new RuntimeException("Positions must monotonically increase") ;
			break ;
		case TYPE_DOUBLE:
			for ( int i = 1; i < mPosDouble.length; i++ )
				if ( mPosDouble[i-1] >= mPosDouble[i] )
					throw new RuntimeException("Positions must monotonically increase") ;
			break ;
				
		}
	}
	
	
	protected double adjustPos( double pos ) {
		switch( mTypePos ) {
		case TYPE_INT:
			return adjustPos( (int)Math.round(pos), mPosInt, mBehavior ) ;
		case TYPE_LONG:
			return adjustPos( (long)Math.round(pos), mPosLong, mBehavior ) ;
		case TYPE_FLOAT:
			return adjustPos( (float)pos, mPosFloat, mBehavior ) ;
		case TYPE_DOUBLE:
			return adjustPos( pos, mPosDouble, mBehavior ) ;
		default:
			throw new RuntimeException("mTypePos not set") ;
		}
	}
	
	
	protected float adjustPos( float pos ) {
		switch( mTypePos ) {
		case TYPE_INT:
			return adjustPos( (int)Math.round(pos), mPosInt, mBehavior ) ;
		case TYPE_LONG:
			return adjustPos( (long)Math.round(pos), mPosLong, mBehavior ) ;
		case TYPE_FLOAT:
			return adjustPos( pos, mPosFloat, mBehavior ) ;
		case TYPE_DOUBLE:
			return (float)adjustPos( pos, mPosDouble, mBehavior ) ;
		default:
			throw new RuntimeException("mTypePos not set") ;
		}
	}
	
	
	protected long adjustPos( long pos ) {
		switch( mTypePos ) {
		case TYPE_INT:
			return adjustPos( (int)pos, mPosInt, mBehavior ) ;
		case TYPE_LONG:
			return adjustPos( pos, mPosLong, mBehavior ) ;
		case TYPE_FLOAT:
			return (long)Math.round(adjustPos( pos, mPosFloat, mBehavior )) ;
		case TYPE_DOUBLE:
			return (long)Math.round(adjustPos( pos, mPosDouble, mBehavior )) ;
		default:
			throw new RuntimeException("mTypePos not set") ;
		}
	}
	
	protected int adjustPos( int pos ) {
		switch( mTypePos ) {
		case TYPE_INT:
			return adjustPos( pos, mPosInt, mBehavior ) ;
		case TYPE_LONG:
			return (int)adjustPos( pos, mPosLong, mBehavior ) ;
		case TYPE_FLOAT:
			return (int)Math.round(adjustPos( (float)pos, mPosFloat, mBehavior )) ;
		case TYPE_DOUBLE:
			return (int)Math.round(adjustPos( (double)pos, mPosDouble, mBehavior )) ;
		default:
			throw new RuntimeException("mTypePos not set") ;
		}
	}
	
	
	protected int adjustPos( int pos, int [] positions, int behavior ) {
		if ( positions.length < 2 )
			return pos ;
		
		int range = positions[positions.length-1] - positions[0] ;
		
		switch( behavior ) {
		case CLAMP:
			if ( pos < positions[0] )
				return positions[0] ;
			else if ( pos > positions[positions.length-1] )
				return positions[positions.length-1] ;
			return pos ;
			
		case REPEAT:
			if ( pos < positions[0] ) {
				throw new RuntimeException("Case REPEAT with pos < positions[0] not yet implemented") ;
			} else if ( pos > positions[positions.length-1] ) {
				// mod range...
				return (pos - positions[0]) % range + positions[0] ;
			}
			return pos ;
			
		case MIRROR:
			if ( pos < positions[0] ) {
				throw new RuntimeException("Case MIRROR with pos < positions[0] not yet implemented") ;
			} else if ( pos > positions[positions.length-1] ) {
				int distFromStart = pos - positions[0] ;
				// if dist / range is even, same as repeat.  If odd,
				// then use positions[end] - (dist % range)
				if ( (distFromStart / range) % 2 == 0 ) {
					return distFromStart % range + positions[0] ;
				} else {
					return positions[positions.length-1] - distFromStart % range ;
				}
			}
			return pos ;
			
		default:
			throw new RuntimeException("Case " + behavior + " is not defined") ;
		}
	}
	
	
	protected long adjustPos( long pos, long [] positions, int behavior ) {
		if ( positions.length < 2 )
			return pos ;
		
		long range = positions[positions.length-1] - positions[0] ;
		
		switch( behavior ) {
		case CLAMP:
			if ( pos < positions[0] )
				return positions[0] ;
			else if ( pos > positions[positions.length-1] )
				return positions[positions.length-1] ;
			return pos ;
			
		case REPEAT:
			if ( pos < positions[0] ) {
				throw new RuntimeException("Case REPEAT with pos < positions[0] not yet implemented") ;
			} else if ( pos > positions[positions.length-1] ) {
				// mod range...
				return (pos - positions[0]) % range + positions[0] ;
			}
			return pos ;
			
		case MIRROR:
			if ( pos < positions[0] ) {
				throw new RuntimeException("Case MIRROR with pos < positions[0] not yet implemented") ;
			} else if ( pos > positions[positions.length-1] ) {
				long distFromStart = pos - positions[0] ;
				// if dist / range is even, same as repeat.  If odd,
				// then use positions[end] - (dist % range)
				if ( (distFromStart / range) % 2 == 0 ) {
					return distFromStart % range + positions[0] ;
				} else {
					return positions[positions.length-1] - distFromStart % range ;
				}
			}
			return pos ;
			
		default:
			throw new RuntimeException("Case " + behavior + " is not defined") ;
		}
	}
	
	
	protected float adjustPos( float pos, float [] positions, int behavior ) {
		if ( positions.length < 2 )
			return pos ;
		
		if ( pos < positions[0] || pos > positions[positions.length-1] )
			throw new RuntimeException("adjustPos not defined for floats outside range") ;
		
		return pos ;
	}
	
	protected double adjustPos( double pos, double [] positions, int behavior ) {
		if ( positions.length < 2 )
			return pos ;
		
		if ( pos < positions[0] || pos > positions[positions.length-1] )
			throw new RuntimeException("adjustPos not defined for doubles outside range") ;
		
		return pos ;
	}
	
	
	

	public double doubleAt(double pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateDouble(index, partial) ;
	}

	public double doubleAt(float pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateDouble(index, partial) ;
	}

	public double doubleAt(int pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateDouble(index, partial) ;
	}

	public double doubleAt(long pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateDouble(index, partial) ;
	}

	public float floatAt(double pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateFloat(index, partial) ;
	}

	public float floatAt(float pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateFloat(index, partial) ;
	}

	public float floatAt(int pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateFloat(index, partial) ;
	}

	public float floatAt(long pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateFloat(index, partial) ;
	}

	public int intAt(double pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateInt(index, partial) ;
	}

	public int intAt(float pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateInt(index, partial) ;
	}

	public int intAt(int pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateInt(index, partial) ;
	}

	public int intAt(long pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateInt(index, partial) ;
	}

	public long longAt(double pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateLong(index, partial) ;
	}

	public long longAt(float pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateLong(index, partial) ;
	}

	public long longAt(int pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateLong(index, partial) ;
	}

	public long longAt(long pos) {
		pos = adjustPos(pos) ;
		int index = indexAtPosition(pos) ;
		double partial = partialAtPosition(index, pos) ;
		
		return interpolateLong(index, partial) ;
	}
	
	
	private int indexAtPosition( double pos ) {
		switch( mTypePos ) {
		case TYPE_INT:
			for ( int i = 1; i < mPosInt.length; i++ )
				if ( pos < mPosInt[i] )
					return i-1 ;
			if ( pos < mPosInt[0] )
				return 0 ;
			else
				return mPosInt.length - 1 ;
			
		case TYPE_LONG:
			for ( int i = 1; i < mPosLong.length; i++ )
				if ( pos < mPosLong[i] )
					return i-1 ;
			if ( pos < mPosLong[0] )
				return 0 ;
			else
				return mPosLong.length - 1 ;
			
		case TYPE_FLOAT:
			for ( int i = 1; i < mPosFloat.length; i++ )
				if ( pos < mPosFloat[i] )
					return i-1 ;
			if ( pos < mPosFloat[0] )
				return 0 ;
			else
				return mPosFloat.length - 1 ;
			
		case TYPE_DOUBLE:
			for ( int i = 1; i < mPosDouble.length; i++ )
				if ( pos < mPosDouble[i] )
					return i-1 ;
			if ( pos < mPosDouble[0] )
				return 0 ;
			else
				return mPosDouble.length - 1 ;
			
		default:
			throw new RuntimeException("mTypePos not properly set") ;
		}
	}
	
	
	private double partialAtPosition( int index, double pos) {
		if ( index < 0 )
			return 0 ;
		
		switch( mTypePos ) {
		case TYPE_INT:
			if ( index >= mPosInt.length-1 )
				return 0 ;
			return (mPosInt[index] - pos) / (mPosInt[index] - mPosInt[index+1]) ;
			
		case TYPE_LONG:
			if ( index >= mPosLong.length-1 )
				return 0 ;
			return (mPosLong[index] - pos) / (mPosLong[index] - mPosLong[index+1]) ;
			
		case TYPE_FLOAT:
			if ( index >= mPosFloat.length-1 )
				return 0 ;
			return (mPosFloat[index] - pos) / (mPosFloat[index] - mPosFloat[index+1]) ;
			
		case TYPE_DOUBLE:
			if ( index >= mPosDouble.length-1 )
				return 0 ;
			return (mPosDouble[index] - pos) / (mPosDouble[index] - mPosDouble[index+1]) ;
			
		default:
			throw new RuntimeException("mTypePos not valid") ;
		}
		
	}
	
	
	protected abstract double interpolateDouble( int index, double partial ) ;
	protected abstract float interpolateFloat( int index, double partial ) ;
	protected abstract long interpolateLong( int index, double partial ) ;
	protected abstract int interpolateInt( int index, double partial ) ;
	
}
