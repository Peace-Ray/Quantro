package com.peaceray.quantro.lobby.exception;


public class RequestErrorException extends CommunicationErrorException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8377339517613818912L;
	
	@Override
	public RequestErrorException setError( int error, int reason ) {
		mExceptionError = error ;
		mExceptionErrorReason = reason ;
		return this ;
	}

}