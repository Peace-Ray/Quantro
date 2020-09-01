package com.peaceray.quantro.lobby.exception;

public class ConfirmErrorException extends CommunicationErrorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7943522259345803011L;
	
	@Override
	public ConfirmErrorException setError( int error, int reason ) {
		mExceptionError = error ;
		mExceptionErrorReason = reason ;
		return this ;
	}
	
}