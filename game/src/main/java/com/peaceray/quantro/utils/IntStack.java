package com.peaceray.quantro.utils;

public class IntStack {
	
	private int [] vals ;
	private int num ;

	/**
	 * Constructs a new IntStack
	 */
	public IntStack( ) {
		this(32) ;
	}
	
	/**
	 * Constructs a new IntStack with the provided capacity
	 * @param capacity
	 */
	public IntStack( int capacity ) {
		num = 0 ;
		vals = new int[capacity] ;
	}
	
	public IntStack( IntStack is ) {
		vals = ArrayOps.duplicate(is.vals) ;
		num = is.num ;
	}
	
	public IntStack( String stringRepresentation ) {
		fromString( stringRepresentation ) ;
	}
	
	
	public String toString() {
		String result = "" + num + ":" ;
		for ( int i = 0; i < num-1; i++ )
			result = result + vals[i] + "," ;
		result = result + vals[num-1] ;
		return result ;
	}
	
	public void fromString( String str ) {
		String [] parts = str.split(":") ;
		num = Integer.parseInt( parts[0] ) ;
		String [] valueStrings = parts[1].split(",") ;
		// add them
		vals = new int[Math.max(32, num)] ;
		for ( int i = 0; i < num; i++ )
			vals[i] = Integer.parseInt( valueStrings[i] ) ;
	}
	
	/**
	 * Peeks ahead - returns the top value in the stack.
	 * @return The top value of the stack
	 */
	public int peek() {
		return vals[num-1] ;
	}
	
	/**
	 * Pops the top value from the stack and returns it.
	 * @return The top value of the stack
	 */
	public int pop() {
		num-- ;
		return vals[num] ;
	}
	
	/**
	 * Pushes a value on top of the stack
	 * @param newVal The value to push
	 */
	public void push(int newVal) {
		if ( num == vals.length ) {
			int [] newVals = new int [Math.max(vals.length * 2, 32)] ;
			for ( int i = 0; i < vals.length; i++ ) {
				newVals[i] = vals[i] ;
			}
			vals = newVals ;
		}
		
		vals[num] = newVal ;
		num++ ;
	}
	
	public void empty() {
		num = 0 ;
	}
	
	/**
	 * How many items on this stack?
	 * @return Returns the number of items on the stack.
	 */
	public int count() {
		return num ;
	}
}
