package com.peaceray.quantro.lobby.exception;

public class ConfirmTimeoutException extends CommunicationErrorException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4170309714087997475L;
	
	@Override
	public ConfirmTimeoutException setError( int error, int reason ) {
		mExceptionError = error ;
		mExceptionErrorReason = reason ;
		return this ;
	}
	
}