package com.peaceray.quantro.utils.LooselyBoundedArray;


/**
 * A LooselyBoundedArray provides a single object reference
 * containing:
 * 
 * - an array object, likely one in the format "int [][][]"
 * - boundaries for nonzero elements in the array, in the form
 * 		[4,8), [0,3), [0,1)
 * 	 meaning that arr[4][0][0] is the "first" nonzero value, and
 *   arr[7][2][0] is the "last."
 *
 * This object allows direct access to the array itself, and 
 * a quick method call (clear()) to set all bounded elements
 * to zero.
 * 
 * However, remember that this is a LOOSELY BOUNDED array.  There are
 * no bounds-checks when elements in the array are changed, and thus
 * no guarantee that "clear" will actually zero-out the array.  It is
 * the user's responsibility to inform the LBA of any value set within
 * the array, so that its boundaries can be expended (if necessary).
 * 
 * Also remember that the bounds are represented as a rectangular
 * hyperprism.  This boundary will grow as needed but will not inform
 * users of zeros within the bounds; it is an OVERESTIMATE of nonzeroes.
 * 
 * Finally, we leave it to users to ensure that the provided objects
 * are, in fact, arrays sized to be rectangular prisms.
 * 
 * @author Jake
 *
 */
public abstract class LooselyBoundedArray {

	// We allow any "array", so store as an object.
	protected Object mArray ;
	
	// mBounds[d][0] is inclusive min in dimension d
	// mBounds[d][1] is exclusive max in dimension d
	protected int [][] mBounds ;
	protected int mDimensions ;
	
	
	public static LooselyBoundedArray newLooselyBoundedArray( Object ar ) {
		if ( ar instanceof int[][][] )
			return new LooselyBoundedArray_int_3( (int[][][])ar ) ;
		else if ( ar instanceof boolean[][] )
			return new LooselyBoundedArray_boolean_2( (boolean[][])ar ) ;
		else if ( ar instanceof byte[][][] ) 
			return new LooselyBoundedArray_byte_3( (byte[][][])ar ) ;
		
		// whoops!
		throw new RuntimeException("The object type " + ar + " is not yet supported.") ;
	}
	
	protected LooselyBoundedArray( Object array, int dimensions ) {
		mArray = array ;
		mDimensions = dimensions ;
		mBounds = new int[mDimensions][2] ;
		
		boundNone() ;
	}
	
	
	/**
	 * Returns the underlying array (as an object - cast as needed).
	 * @return
	 */
	public Object array() {
		return mArray ;
	}
	
	
	/**
	 * Returns the number of dimensions for the underlying array.
	 * @return
	 */
	public int dimensions() {
		return mDimensions ;
	}
	
	
	/**
	 * Sets every element within the current bounds to 'zero' or some other
	 * default value (e.g., 'false' for booleans).
	 */
	public abstract LooselyBoundedArray clear() ;

	
	/**
	 * Expands (or sets, if empty) the boundaries in the specified dimension to
	 * include the specified range.
	 * @param dimension
	 * @param minInclusive
	 * @param maxExclusive
	 */
	public LooselyBoundedArray bound( int dimension, int minInclusive, int maxExclusive ) {
		if ( mBounds[dimension][0] == Integer.MIN_VALUE ) {
			// set to exactly this
			mBounds[dimension][0] = minInclusive ;
			mBounds[dimension][1] = maxExclusive ;
		} else {
			// expand to include this.
			mBounds[dimension][0] = Math.min( mBounds[dimension][0], minInclusive ) ;
			mBounds[dimension][1] = Math.max( mBounds[dimension][1], maxExclusive ) ;
		}
		
		return this ;
	}
	
	
	/**
	 * Expands (or sets) the bounds to include the specified value in the specified dimension.
	 * @param dimension
	 * @param index
	 */
	public LooselyBoundedArray bound( int dimension, int index ) {
		if ( mBounds[dimension][0] == Integer.MIN_VALUE ) {
			// not yet set
			mBounds[dimension][0] = index ;
			mBounds[dimension][1] = index+1 ;
		} else {
			mBounds[dimension][0] = Math.min(mBounds[dimension][0], index ) ;
			mBounds[dimension][1] = Math.max(mBounds[dimension][1], index+1 ) ;
		}
		
		return this ;
	}
	
	
	/**
	 * Expands (or sets) the bounds to include the boundaries of the provided object.
	 * This operation may fail if the provided argument is not of the correct subtype.
	 * @param lba
	 * @return
	 */
	public abstract LooselyBoundedArray bound( LooselyBoundedArray lba ) ;
	
	/**
	 * Resets the boundaries, making no change to the underlying array.
	 */
	public LooselyBoundedArray boundNone() {
		for ( int i = 0; i < mDimensions; i++ )
			mBounds[i][0] = mBounds[i][1] = Integer.MIN_VALUE ;
		return this ;
	}
	
	public abstract LooselyBoundedArray boundAll() ;
}
