package com.peaceray.quantro.lobby.exception;

public class RequestTimeoutException extends CommunicationErrorException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8974013493346483395L;
	
	@Override
	public RequestTimeoutException setError( int error, int reason ) {
		mExceptionError = error ;
		mExceptionErrorReason = reason ;
		return this ;
	}
	
}