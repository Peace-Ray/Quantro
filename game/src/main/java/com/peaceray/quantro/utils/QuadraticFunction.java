package com.peaceray.quantro.utils;


/**
 * QuadraticFunction: defines a position (and velocity, natch) at a given
 * point in time, with t >= 0.
 * 
 * A QuadraticFunction instance represents a quadratic equation.  They are
 * mutable, to help avoid repeated allocations.
 * 
 * @author Jake
 *
 */
public class QuadraticFunction implements Function {

	// coeff.
	double [] mCoefficient ;
	
	// roots
	double [] mRoot ;
	int mNumRoots ;
	
	
	public QuadraticFunction() {
		mCoefficient = new double[3] ;
		mRoot = new double[2] ;
		
		for ( int i = 0; i < 3; i++ )
			mCoefficient[i] = 0 ;
		findAndSetRootsFromCoefficients() ;
	}
	
	/**
	 * Returns an instance of this Function that can operate independently.
	 * Will return identical values at all positions.
	 * 
	 * @return
	 */
	public Function copy() {
		QuadraticFunction qf = new QuadraticFunction() ;
		return qf.setWithQuadraticFunction(this) ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// MUTATORS
	
	public QuadraticFunction setWithQuadraticFunction( QuadraticFunction qf ) {
		for ( int i = 0; i < 3; i++ )
			mCoefficient[i] = qf.mCoefficient[i] ;
		for ( int i = 0; i < 2; i++ )
			mRoot[i] = qf.mRoot[i] ;
		mNumRoots = qf.mNumRoots ;
		
		return this ;
	}
	
	/**
	 * Sets this QuadraticFunction with the provided coefficients, using the form
	 * 
	 * f(x) = ax^2 + bx + c.
	 */
	public QuadraticFunction setWithCoefficients( double [] coeff ) {
		return setWithCoefficients( coeff[0], coeff[1], coeff[2] ) ;
	}
	
	/**
	 * Sets this QuadraticFunction with the provided coefficients, using the form
	 * 
	 * f(x) = ax^2 + bx + c.
	 */
	public QuadraticFunction setWithCoefficients( double a, double b, double c ) {
		mCoefficient[0] = a ;
		mCoefficient[1] = b ;
		mCoefficient[2] = c ;
		
		findAndSetRootsFromCoefficients() ;
		return this ;
	}
	
	
	/**
	 * Sets this QuadraticFunction with the provided acceleration and roots.
	 * 
	 * "Acceleration" is the 2nd derivative term; it is 2 times the first
	 * order coefficient.  Acceleration defines the change in slope with
	 * delta-x = 1.
	 * 
	 * Using this constructor requires a nonzero acceleration.
	 * The function resulting is of the form:
	 * 
	 * (accel / 2) * ( x - u ) * (x - v )
	 * 
	 */
	public QuadraticFunction setWithAccelAndRoots( double accel, double u, double v ) {
		if ( accel != 0 && u != v ) {
			// best form.
			mNumRoots = 2 ;
			mRoot[0] = Math.min(u, v) ;
			mRoot[1] = Math.max(u, v) ;
			
			// set coeffs...
			mCoefficient[0] = accel / 2 ;
			mCoefficient[1] = mCoefficient[0] * ( -u -v ) ;
			mCoefficient[2] = mCoefficient[0] * u * v ;
			return this ;
		}
		
		if ( accel != 0 ) {
			// one root.
			mNumRoots = 1 ;
			mRoot[0] = u ;
			mRoot[1] = u ;
			
			// set coeffs...
			mCoefficient[0] = accel / 2 ;
			mCoefficient[1] = mCoefficient[0] * ( -u*2 ) ;
			mCoefficient[2] = mCoefficient[0] * u * u ;
			return this ;
		}
		
		throw new IllegalArgumentException("Must provide nonzero acceleration") ;
	}
	
	
	/**
	 * Sets this QuadraticFunction with the provided acceleration and 1st (lower) root,
	 * along with the 'y' value of the local optimum.
	 * 
	 * If the local optimum is 0, it is assumed to be the specified root.
	 * 
	 * This setter requires that the input paramaters be consistent: if optimum
	 * is positive, accel must be negativie (to curve back towards the x axis)
	 * and vice-versa.
	 * 
	 * @param accel 1/2 coefficient A.  Second derivative.  Must be nonzero.
	 * @param opt 'y' Value of the local optimum.
	 * @param root The first x-axis intersection point.
	 * @return
	 */
	public QuadraticFunction setWithAccelOptimumAndFirstRoot( double accel, double opt, double root ) {
		if ( accel == 0 )
			throw new IllegalArgumentException("Must provide nonzero acceleration") ;
		
		if ( accel > 0 == opt > 0 )
			throw new IllegalArgumentException("Acceleration and optimum must have opposite signs") ;
		
		if ( opt == 0 ) {
			// single-root form.
			return setWithAccelAndRoots( accel, root, root ) ;
		}
		
		// Remember that we have been given the optimum height, not the 'x' value
		// to produce that optimum.  We can get the true quadratic using one which
		// puts its optimum at x = 0, then translate left or right to match the first root.
		// If both roots are set to u, v, translating x + 1 is equivalent to u += 1, v += 1.
		
		// convert to root form, putting f(x) = opt.
		mCoefficient[0] = accel / 2 ;
		mCoefficient[1] = 0 ;
		mCoefficient[2] = opt ;
		
		// find and set roots
		findAndSetRootsFromCoefficients() ;
		
		// translate roots so the smaller (the first) is at 'root'.
		mRoot[1] += ( root - mRoot[0] ) ;
		mRoot[0] = root ;
		
		// re-set coefficients
		mCoefficient[1] = mCoefficient[0] * ( -mRoot[0] -mRoot[1] ) ;
		mCoefficient[2] = mCoefficient[0] * mRoot[0] * mRoot[1] ;
		
		return this ;
	}
	
	
	/**
	 * From the coefficients, which should be set before this method is
	 * called, find the number of roots and set them appropriately.
	 */
	private void findAndSetRootsFromCoefficients() {
		if ( mCoefficient[0] == 0 ) {
			if ( mCoefficient[1] == 0 ) {
				// no roots!
				mNumRoots = 0 ;
				return ;
			}
			
			// linear.  One root.
			// 0 = bx + c
			// -c = bx
			// -c/b = x.
			mNumRoots = 1 ;
			mRoot[0] = -mCoefficient[2] / mCoefficient[1] ;
		}
		
		// True Quadratic.
		// quadratic formula.
		// terms are W +/- U / V.
		// u is U^2.
		
		double u = mCoefficient[1] * mCoefficient[1] - 4 * mCoefficient[0] * mCoefficient[2] ;
		if ( u < 0 ) {
			mNumRoots = 0 ;
			return ;
		}
		
		double W = -mCoefficient[1] ;
		double U = Math.sqrt( u ) ;
		double V = 2 * mCoefficient[0] ;
		
		if ( U == 0 ) {
			mNumRoots = 1 ;
			mRoot[0] = W / V ;
			return ;
		}
		
		mNumRoots = 2 ;
		double r1 = (W - U) / V ;
		double r2 = (W + U) / V ;
		mRoot[0] = Math.min(r1, r2) ;
		mRoot[1] = Math.max(r1, r2) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// ACCESSORS
	public double at(double x) {
		// faster to use roots, if possible.
		if ( mNumRoots == 2 )
			return mCoefficient[0] * (x - mRoot[0]) * (x - mRoot[1]) ;
		return mCoefficient[0] * x * x + mCoefficient[1] * x + mCoefficient[2] ;
	}
	
	
	public boolean hasOptimum() {
		return mCoefficient[0] != 0 ;
	}
	
	public boolean hasMin() {
		return mCoefficient[0] > 0 ;
	}
	
	public boolean hasMax() {
		return mCoefficient[0] < 0 ;
	}
	
	public double getOptimumLocation() {
		// location where first derivative is zero.
		return -mCoefficient[1] / ( mCoefficient[0] * 2 ) ;
	}
	
	public double getOptimum() {
		return at( getOptimumLocation() ) ;
	}
	
	public double getMinLocation() {
		// location where first derivative is zero.
		if ( !hasMin() )
			throw new IllegalStateException("This function does not have a minimum.") ;
		return -mCoefficient[1] / ( mCoefficient[0] * 2 ) ;
	}
	
	public double getMin() {
		return at( getMinLocation() ) ;
	}
	
	public double getMaxLocation() {
		// location where first derivative is zero.
		if ( !hasMax() )
			throw new IllegalStateException("This function does not have a minimum.") ;
		return -mCoefficient[1] / ( mCoefficient[0] * 2 ) ;
	}
	
	public double getMax() {
		return at( getMaxLocation() ) ;
	}	
	
	public double getA() {
		return mCoefficient[0] ;
	}
	
	public double getB() {
		return mCoefficient[0] ;
	}
	
	public double getC() {
		return mCoefficient[0] ;
	}
	
	public int numRoots() {
		return mNumRoots ;
	}

	public double getRoot( int num ) {
		if ( num < mNumRoots )
			return mRoot[num] ;
		throw new IllegalArgumentException("Not e.") ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// STATIC COMPARISON
	
	private static QuadraticFunction sQF = new QuadraticFunction() ;
	
	/**
	 * Determines the intersection(s) of the provided QuadraticFunctions.
	 * Returns 'n', the number of intersection points, and sets the first 'n'
	 * entries in 'intersection' to those values, in ascending order.
	 * 
	 * Intersections are represented as 'x' values.
	 */
	public static int getIntersections( QuadraticFunction qf1, QuadraticFunction qf2, double [] intersection ) {
		
		// all possible quadratic forms include coefficients.
		// Find intersections using the quadratic formula.
		synchronized( sQF ) {
			sQF.setWithCoefficients(
					qf1.mCoefficient[0] - qf2.mCoefficient[0],
					qf1.mCoefficient[1] - qf2.mCoefficient[1],
					qf1.mCoefficient[2] - qf2.mCoefficient[2]) ;
			
			// roots?
			for ( int r = 0; r < sQF.numRoots(); r++ ) {
				intersection[r] = sQF.getRoot(r) ;
			}
			return sQF.numRoots() ;
		}
	}
	
}
