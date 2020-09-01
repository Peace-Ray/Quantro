package com.peaceray.quantro.communications.nonce;


public class MutableNonce extends Nonce {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8674458583976977356L;

	private boolean mMutable ;
	
	private byte [] mByteBuffer ;
	
	public MutableNonce() {
		super() ;
		mMutable = true ;
	}
	
	public MutableNonce(int numBytes) {
		super(numBytes) ;
		mMutable = true ;
	}
	
	// mutators!
	
	public Nonce makeImmutable() {
		mMutable = false ;
		mByteBuffer = null ;
		
		return this ;
	}
	
	public void regenerate() {
		if ( !mMutable )
			throw new IllegalStateException("MutableNonce is no longer mutable.") ;
		
		if ( mByteBuffer == null )
			mByteBuffer = new byte[numberOfBytes()] ;
		
		r.nextBytes(mByteBuffer) ;
		setBytes(mByteBuffer) ;
	}
}
