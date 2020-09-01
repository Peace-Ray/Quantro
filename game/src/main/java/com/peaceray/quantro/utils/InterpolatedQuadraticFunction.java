package com.peaceray.quantro.utils;


/**
 * An InterpolatedQuadraticFunction combines two input QuadraticFuctions,
 * using a custom interpolation scheme which is dependent on those input
 * functions and their intersections.
 * 
 * If the quadratics provided have two intersection points, the interpolation
 * occurs between those intersections.  If none, the interpolation occurs between
 * their optimums.  If exactly one, behavior is dependent on first coefficients:
 * if they move in opposite directions, we assume the intersection point is already
 * 1st-derivative continuous; otherwise, we interpolate between optimums.
 * 
 * @author Jake
 *
 */
public class InterpolatedQuadraticFunction {

	QuadraticFunction [] mQF ;
	double [][] mQFRange ;
	
	QuadraticFunction [] mQFLast ;
	double [][] mQFRangeLast ;
	
	public InterpolatedQuadraticFunction() {
		mQF = new QuadraticFunction[2] ;
		mQFRange = new double[2][2] ;
		
		for ( int i = 0; i < 2; i++ )
			mQF[i] = new QuadraticFunction() ;
		mQF[0].setWithCoefficients(-1, 0, 0) ;
		mQF[1].setWithCoefficients( 1, 0, 0) ;
		
		findAndSetRanges() ;
		
		mQFLast = new QuadraticFunction[2] ;
		mQFRangeLast = new double[2][2] ;
		for ( int i = 0; i < 2; i++ ) {
			mQFLast[i] = new QuadraticFunction() ;
		}
	}
	
	
	public InterpolatedQuadraticFunction set( QuadraticFunction qf1, QuadraticFunction qf2 ) {
		
		try {
			storeLast() ;
			mQF[0].setWithQuadraticFunction(qf1) ;
			mQF[1].setWithQuadraticFunction(qf2) ;
			
			findAndSetRanges() ;
			return this ;
		} catch (RuntimeException e) {
			revertToLast() ;
			throw e ;
		}
	}
	
	
	private void storeLast() {
		copyTo( mQF, mQFRange, mQFLast, mQFRangeLast ) ;
	}
	
	private void revertToLast() {
		copyTo( mQFLast, mQFRangeLast, mQF, mQFRange ) ;
	}
	
	private void copyTo( QuadraticFunction [] srcQF, double [][] srcRange, QuadraticFunction [] dstQF, double [][] dstRange ) {
		for ( int i = 0; i < srcQF.length; i++ ) {
			dstQF[i].setWithQuadraticFunction(srcQF[i]) ;
			for ( int j = 0; j < srcRange[i].length; j++ ) {
				dstRange[i][j] = srcRange[i][j] ;
			}
		}
	}
	
	
	
	
	
	private double [] intersection = new double[2] ;
	private void findAndSetRanges() {
		// Finds and sets the appropriate ranges to use when interpolating
		// the functions.
		
		int numIntersections = QuadraticFunction.getIntersections(mQF[0], mQF[1], intersection) ;
		if ( numIntersections == 2 ) {
			// easiest (best!) case.  Interpolate from one intersection to the other.
			mQFRange[0][0] = Double.NEGATIVE_INFINITY ;
			mQFRange[0][1] = intersection[0] ;
			mQFRange[1][0] = intersection[1] ;
			mQFRange[1][1] = Double.POSITIVE_INFINITY ;
			return ;
		}
		
		if ( numIntersections == 0 ) {
			 // if both have optimums, transition between them.
			if ( mQF[0].hasOptimum() && mQF[1].hasOptimum() ) {
				double opt0 = mQF[0].getOptimumLocation() ;
				double opt1 = mQF[1].getOptimumLocation() ;
				mQFRange[0][0] = Double.NEGATIVE_INFINITY ;
				mQFRange[0][1] = Math.min(opt0, opt1) ;
				mQFRange[1][0] = Math.max(opt0, opt1) ;
				mQFRange[1][1] = Double.POSITIVE_INFINITY ;
				return ;
			}
			
			// Otherwise there is nothing to do.
			throw new IllegalArgumentException("Quadratics do not have compatible interaction.") ;
		}
		
		if ( numIntersections == 1 ) {
			if ( ( mQF[0].getA() > 0 && mQF[1].getA() < 0 )
					|| ( mQF[0].getA() < 0 && mQF[1].getA() > 0 ) ) {
				// assume this intersection is the point of equal slope.
				mQFRange[0][0] = Double.NEGATIVE_INFINITY ;
				mQFRange[0][1] = intersection[0] ;
				mQFRange[1][0] = intersection[0] ;
				mQFRange[1][1] = Double.POSITIVE_INFINITY ;
				return ;
			}
			
			else if ( mQF[0].getA() == 0 || mQF[1].getA() == 0 ) {
				// at least one is linear.  Use this point for interpolation.
				mQFRange[0][0] = Double.NEGATIVE_INFINITY ;
				mQFRange[0][1] = intersection[0] ;
				mQFRange[1][0] = intersection[0] ;
				mQFRange[1][1] = Double.POSITIVE_INFINITY ;
				return ;
			}
			
			else if ( (mQF[0].getA() > 0) == (mQF[1].getA() > 0) ) {
				// they curve the same direction and have 1 intersection.
				// They are in a "W" shape.  Interpolate between optimums.
				double opt0 = mQF[0].getOptimumLocation() ;
				double opt1 = mQF[1].getOptimumLocation() ;
				mQFRange[0][0] = Double.NEGATIVE_INFINITY ;
				mQFRange[0][1] = Math.min(opt0, opt1) ;
				mQFRange[1][0] = Math.max(opt0, opt1) ;
				mQFRange[1][1] = Double.POSITIVE_INFINITY ;
				return ;
			}
		}
		
		throw new IllegalArgumentException("Quadratics do not have compatible interaction.") ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// Accessors
	
	public double at( double x ) {
		
		// locate the right qf.  If interoplating, take
		// the lower.
		int quad = 0 ;
		boolean between = false ;
		
		for ( int i = 0; i < mQFRange.length; i++ ) {
			System.err.println("InterpolatedQuadraticFunction at " + x + " range " + i + " is " + mQFRange[i][0] + ", " + mQFRange[i][1]) ;
			if ( x < mQFRange[i][0] ) {
				// just before this.
				quad = i-1 ;
				between = true ;
				break ;
			} else if ( x <= mQFRange[i][1] ) {
				quad = i ;
				between = false ;
				break ;
			}
		}
		
		if ( !between ) {
			return mQF[quad].at(x) ;
		}
		
		double y1 = mQF[quad].at(x) ;
		double y2 = mQF[quad+1].at(x) ;
		
		double alpha = ( x - mQFRange[quad][1] ) / ( mQFRange[quad+1][0] - mQFRange[quad][1] ) ;
		return y1 + alpha*( y2 - y1 ) ;
	}
	
	
}
