package com.peaceray.quantro.utils;

public interface Function {

	/**
	 * Returns an instance of this Function that can operate independently.
	 * Will return identical values at all positions.
	 * 
	 * @return
	 */
	public Function copy() ;
	
	/**
	 * Returns the function value at the provided position.
	 * @param x
	 * @return
	 */
	public double at( double x ) ;
	
}
