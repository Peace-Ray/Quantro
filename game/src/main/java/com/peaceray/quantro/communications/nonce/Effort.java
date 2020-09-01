package com.peaceray.quantro.communications.nonce;

import java.io.IOException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.peaceray.quantro.utils.Base64;



/**
 * A quick, easy, effort-producing class.
 * 
 * Constructed with a integer indicating the
 * amount of effort (i.e. initial zero-bits in the resulting hash),
 * a Nonce which can be retrieved later if needed,
 * and an arbitrary number of additional salt nonces.
 * 
 * getSalt() will return a Nonce object which, when
 * concatenated as
 * 
 * [nonce|salt_in_1|...|salt_in_n|salt_out]
 * 
 * with nonce and salt_in_* the construction parameters
 * and salt_out the value returned by getSalt, will produce
 * a SHA-1 hash value with the correct number of initial zero-bits.
 * 
 * Construction should be lightning-quick.  The first
 * call to getSalt() can and will produce a significant delay.
 * hasSalt() will indicate whether you can expect a fast return.
 * 
 * NOTE: calls to getSalt() on a single instance will always return
 * the same value (in fact, the same object), but a new Effort instance
 * constructed with the same parameters will not necessarily produce
 * the same salt.
 * 
 * NOTE2: although Nonce objects are used, and Nonces have an easily
 * accessed "writeAsBytes" method, we do NOT use this method as it
 * includes an explicit length-encoding.  Instead, we use direct byte
 * access to ensure that only the nonce content bytes (which are wholly
 * random and independent) are used.  Be sure to use direct byte access
 * when checking the effort.
 * 
 * @author Jake
 *
 */
public class Effort {

	private int mEffortBits ;
	
	private byte [] mData ;		// the final Nonce.MAXIMUM_NUMBER_OF_BYTES is reserved for our proof
	private int mInputBytes ;
	
	private Nonce mSaltOut ;
	
	// if we have generated salt, this might be useful.
	private long mEffortTime ;
	private long mEffortTrials ;
	
	public Effort( int effort, Nonce nonce, Nonce ... saltIn ) {
		// check validity?
		if ( effort < 0 )
			throw new IllegalArgumentException("Effort must be non-negative.  Use 0 for no effort expended.") ;
		if ( nonce == null )
			throw new NullPointerException("Nonce must not be null.") ;
		for ( int i = 0; i < saltIn.length; i++ )
			if ( saltIn[i] == null )
				throw new NullPointerException("Salt must not be null.") ;
		
		mEffortBits = effort ;
		
		mInputBytes = nonce.numberOfBytes() ;
		for ( int i = 0; i < saltIn.length; i++ )
			mInputBytes += saltIn[i].numberOfBytes() ;
		
		// allocate a byte array, and copy constants.
		mData = new byte[mInputBytes + Nonce.MAXIMUM_NUMBER_OF_BYTES] ;
		int index = 0 ;
		for ( int i = 0; i < nonce.numberOfBytes(); i++ )
			mData[index++] = nonce.directByteAccess(i) ;
		for ( int j = 0; j < saltIn.length; j++ )
			for ( int i = 0; i < saltIn[j].numberOfBytes(); i++ )
				mData[index++] = saltIn[j].directByteAccess(i) ;
		
		
		if ( effort > Nonce.MAXIMUM_NUMBER_OF_BYTES * 8 )
			throw new IllegalArgumentException("That amount of effort is impossible.") ;
		
		mSaltOut = null ;
	}
	
	
	public Effort( int effort, String b64data ) {
		// check validity?
		if ( effort < 0 )
			throw new IllegalArgumentException("Effort must be non-negative.  Use 0 for no effort expended.") ;
		if ( b64data == null )
			throw new NullPointerException("Data must not be null.") ;
		
		mEffortBits = effort ;
		
		byte[] data;
		try {
			data = Base64.decode(b64data, Base64.URL_SAFE);
		} catch (IOException e) {
			throw new IllegalArgumentException("Provided data string failed url-safe b64 decode.") ;
		}
		
		mInputBytes = data.length ;
		mData = new byte[mInputBytes + Nonce.MAXIMUM_NUMBER_OF_BYTES] ;
		for ( int i = 0; i < mInputBytes; i++ )
			mData[i] = data[i] ;
		
		if ( effort > Nonce.MAXIMUM_NUMBER_OF_BYTES * 8 )
			throw new IllegalArgumentException("That amount of effort is impossible.") ;
		
		mSaltOut = null ;
	}
	
	
	public boolean hasSalt() {
		return mSaltOut != null ;
	}
	
	
	public Nonce getSalt() {
		return getSalt( 0 ) ;
	}
	
	public Nonce getSalt( long maxMilliseconds ) {
		if ( mSaltOut == null )
			mSaltOut = makeSalt( maxMilliseconds ) ;
		
		return mSaltOut ;
	}
	
	
	public long getNumEffortTrials() {
		return mEffortTrials ;
	}
	
	public long getEffortTime() {
		return mEffortTime ;
	}
	
	
	private Nonce makeSalt( long maxMilliseconds ) {
		mEffortTrials = 0 ;
		mEffortTime = 0 ;
		long time = System.currentTimeMillis() ;
		
		// a mutable nonce for a salt.   When we find the right
		// value we will render immutable.
		MutableNonce salt = new MutableNonce(Nonce.MAXIMUM_NUMBER_OF_BYTES) ;
		
		// A digest object.
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null ;
		}
		byte [] digest = new byte[md.getDigestLength()] ;
			// avoid unnecessary allocation with each digest attempt.
		
		while ( true ) {
			mEffortTrials++ ;
			// make a new mutation and check the number of
			// leading zeros.
			salt.regenerate() ;
			
			for ( int i = 0; i < Nonce.MAXIMUM_NUMBER_OF_BYTES; i++ )
				mData[mInputBytes + i] = salt.directByteAccess(i) ;
			
			md.update(mData) ;
			try {
				md.digest(digest, 0, md.getDigestLength()) ;
			} catch (DigestException e) {
				e.printStackTrace() ;
				return null ;
			}
			
			// check the digest.
			if ( numberLeadingZeroes(digest) >= mEffortBits ) {
				mEffortTime = System.currentTimeMillis() - time ;
				return salt.makeImmutable() ;
			}
			
			if ( maxMilliseconds > 0 && (System.currentTimeMillis() - time) > maxMilliseconds )
				return null ;
		}
	}
	
	/**
	 * Returns the number of zeroes loading this byte array.
	 * This value is between 0 and the 8 * bytes.length, inclusive.
	 * @param bytes
	 * @return
	 */
	private int numberLeadingZeroes( byte [] bytes ) {
		int num = 0 ;
		for ( int i = 0; i < bytes.length; i++ ) {
			if ( bytes[i] == 0 )
				num += 8 ;
			else {
				// number the bits from 0 (ones place) to 7 (128ths place).
				// the value of bit n can be acquired by shifting the 
				// bits n places and then masking with 0x01.
				for ( int bitNum = 7; bitNum >= 0; bitNum-- ) {
					if ( ( ( bytes[i] >>> bitNum ) & 0x01 ) == 0 )
						num++ ;
					else
						break ;
				}
				
				return num ;
			}
		}
		
		return bytes.length * 8 ;
	}
}
