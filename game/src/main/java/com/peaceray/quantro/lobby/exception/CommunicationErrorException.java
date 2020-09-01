package com.peaceray.quantro.lobby.exception;

import com.peaceray.quantro.lobby.WebConsts;

public class CommunicationErrorException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7943522259345803011L;
	
	int mExceptionError ;
	int mExceptionErrorReason ;
	
	public CommunicationErrorException setError( int error, int reason ) {
		mExceptionError = error ;
		mExceptionErrorReason = reason ;
		return this ;
	}
	
	public int getError() {
		return mExceptionError ;
	}
	
	public int getErrorReason() {
		return mExceptionErrorReason ;
	}
	
	public String toString() {
		return errorString() ;
	}
	
	protected String errorString() {
		return this.getClass().getName() + " error is " + WebConsts.errorIntToString(mExceptionError) + " reason is " + WebConsts.reasonIntToString(mExceptionErrorReason) ;
	}
	
}