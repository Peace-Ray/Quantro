package com.peaceray.quantro.exceptions;

public class QOrientationConflictException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8824156506123923412L;

	
	public int qo1 ;
	public int qo2 ;
	
	public QOrientationConflictException( int qo1, int qo2, String mesg ) {
		super( mesg ) ;
	}
	
}
