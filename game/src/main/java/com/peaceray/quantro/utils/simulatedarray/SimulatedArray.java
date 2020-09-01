package com.peaceray.quantro.utils.simulatedarray;

/**
 * An abstract "array" class, holding ints or floats, indexed from 0 on.
 * 
 * Distinct from true arrays in that values may or may not be
 * 		explicitly defined in memory.  The advantages to this:
 * 
 * 1. If values are represented in some simplified form (e.g., a function
 * 		based on index) they can be defined for a large number of indices
 * 		without requiring additional memory for those indices
 * 
 * 2. Values beyond the defined limits of the array may possibly be accessed
 * 		(using e.g. truncated representation).
 * 
 * We do not GUARANTEE either of the above.  Perhaps an instance of this class
 * requires as much memory (or more!) than the equivalent int [] or float [] would.
 * Perhaps it defines only a limited range of values [0, x) and throws an ArrayOutOfBoundsException
 * if any are accessed beyond it.
 * 
 * Convention: the value returned by "getInt(x)" == "Math.round( getFloat(x) )."
 * 
 * @author Jake
 *
 */
public abstract class SimulatedArray {

	public abstract int getInt( int index ) ;
	public abstract double getDouble( int index ) ;
	
	public abstract int size() ;
	
}
