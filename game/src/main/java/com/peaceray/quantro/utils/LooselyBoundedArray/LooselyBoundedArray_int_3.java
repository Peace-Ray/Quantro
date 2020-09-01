package com.peaceray.quantro.utils.LooselyBoundedArray;

/**
 * Subclass of LooselyBoundedArray designed for 3 dimensional integer (int) arrays.
 * 
 * @author Jake
 *
 */
class LooselyBoundedArray_int_3 extends LooselyBoundedArray {

	int [][][] ar ;
	
	LooselyBoundedArray_int_3( int [][][] ar ) {
		super( ar, 3 ) ;
		this.ar = ar ;
	}

	@Override
	public LooselyBoundedArray clear() {
		
		// to avoid repeated array access, store range in local vars.
		int min_0 = mBounds[0][0], max_0 = mBounds[0][1] ;
		int min_1 = mBounds[1][0], max_1 = mBounds[1][1] ;
		int min_2 = mBounds[2][0], max_2 = mBounds[2][1] ;
		
		// if any dimension is Integer.MIN_VALUE, we have nothing to clear.
		if ( min_0 == Integer.MIN_VALUE || min_1 == Integer.MIN_VALUE || min_2 == Integer.MIN_VALUE )
			return this ;
		
		// Adjust min/max to be within array dimensions.
		min_0 = Math.max(0, Math.min(min_0, ar.length)) ;
		max_0 = Math.max(0, Math.min(max_0, ar.length)) ;
		min_1 = Math.max(0, Math.min(min_1, ar[0].length)) ;
		max_1 = Math.max(0, Math.min(max_1, ar[0].length)) ;
		min_2 = Math.max(0, Math.min(min_2, ar[0][0].length)) ;
		max_2 = Math.max(0, Math.min(max_2, ar[0][0].length)) ;
		
		for ( int i = min_0; i < max_0; i++ ) {
			for ( int j = min_1; j < max_1; j++ ) {
				for ( int k = min_2; k < max_2; k++ ) {
					ar[i][j][k] = 0 ;
				}
			}
		}
		
		return this ;
	}
	
	
	@Override
	public LooselyBoundedArray bound( LooselyBoundedArray lba ) {
		if ( !(lba instanceof LooselyBoundedArray_int_3) )
			throw new IllegalArgumentException("Provided argument " + lba + " is not of the correct type") ;
		
		for ( int i = 0; i < 3; i++ ) {
			if ( lba.mBounds[i][0] == Integer.MIN_VALUE )
				continue ;
			if ( mBounds[i][0] == Integer.MIN_VALUE ) {
				// copy
				mBounds[i][0] = lba.mBounds[i][0] ;
				mBounds[i][1] = lba.mBounds[i][1] ;
			} else {
				// expand
				mBounds[i][0] = Math.min( mBounds[i][0], lba.mBounds[i][0] ) ;
				mBounds[i][1] = Math.max( mBounds[i][1], lba.mBounds[i][1] ) ;
			}
		}
		
		return this ;
	}
	
	
	@Override
	public LooselyBoundedArray boundAll() {
		for ( int i = 0; i < 3; i++ )
			mBounds[i][0] = 0 ;
		
		mBounds[0][1] = ar.length ;
		mBounds[1][1] = ar[0].length ;
		mBounds[2][1] = ar[0][0].length ;
		
		
		return this ;
	}
}
