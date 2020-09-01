package com.peaceray.quantro.communications.nonce;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Random;

import com.peaceray.quantro.communications.SimpleSHA1;
import com.peaceray.quantro.utils.Base64;


/**
 * Defines a special class for "nonces"; randomly generated
 * values which are used to uniquely identify lobbies, challenges
 * and games.  Nonces are immutable once constructed, but can
 * be converted to alternative representations (such as by
 * writing to a byte array, or converting to URL-safe Base64).
 * 
 * By default, a Nonce is 16 bytes (128 bits) randomly generated
 * using java.security.SecureRandom.  Nonces of other lengths may
 * be generating using new Nonce(#bytes).  Nonce length is significant
 * for comparison; Nonces of lengths A and B, with A != B, will always
 * be considered unequal ( according to .equals( ) ) regardless of their
 * content bits.
 * 
 * If you wish to avoid null-checking for nonce comparison,
 * use Nonce.ZERO as a default value.  This value is guaranteed
 * to be unequal to any other constructed Nonce, except those
 * constructed from the byte array or String representation of
 * Nonce.ZERO (i.e., a randomly-generate Nonce will NEVER be equal
 * to Nonce.ZERO).
 * 
 * @author Jake
 *
 */
public class Nonce implements Serializable, Comparable<Nonce> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4514696132856748661L;
	
	public static final int DEFAULT_NUMBER_OF_BYTES = 18 ;
	public static final int MAXIMUM_NUMBER_OF_BYTES = 254 ;		// So that length + data can fit in 255.
	
	private static final int LENGTH_TO_BYTE_REPRESENTATION_OFFSET = Byte.MIN_VALUE ;
	
	protected static final Random r = new SecureRandom() ;
	
	public static final Nonce ZERO = new Nonce( new byte[]{LENGTH_TO_BYTE_REPRESENTATION_OFFSET} ) ;	// 0-length
	
	private byte [] bytes ;
	private String stringRepresentation ;
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// Subclass Access
	//
	// Subclasses can acquire situational access to data members,
	// but be careful.
	//
	///////////////////////////////////////////////////////////////////////////
	
	protected void setBytes( byte [] bytes ) {
		if ( this.bytes.length != bytes.length )
			throw new IllegalArgumentException("Must provide the same number of bytes.") ;
		
		stringRepresentation = null ;
		for ( int i = 0; i < this.bytes.length; i++ )
			this.bytes[i] = bytes[i] ;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// CONSTRUCTING NONCE OBJECTS
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	public static final String toSMSSafe( String nonceStr ) {
		return nonceStr.replace("-", "+").replace("_", "/") ;
	}
	
	public static final String fromSMSSafe( String nonceStr ) {
		return nonceStr.replace("+", "-").replace("/", "_") ;
	}
	
	
	/**
	 * Returns a newly constructed nonce object, using an arbitrary string as input.
	 * The output will be constant for a particular string.
	 * @param str
	 * @return
	 */
	public static Nonce newNonceAsStringHash( String str, int numBytes ) {
		assert( numBytes > 0 ) ;
		assert( numBytes <= MAXIMUM_NUMBER_OF_BYTES ) ;
		
		try {
			byte [] hashbytes = SimpleSHA1.SHA1Bytes(str) ;
			byte [] databytes = new byte[numBytes] ;
			for ( int i = 0; i < numBytes; i++ )
				databytes[i] = hashbytes[i % hashbytes.length] ;
			
			Nonce n = new Nonce() ;
			n.bytes = databytes ;
			n.stringRepresentation = null ;
			return n ;
		} catch( Exception e ) {
			e.printStackTrace() ;
			return null ;
		}
	}
	
	/**
	 * A random nonce constructor.  Produces
	 * a Nonce object with DEFAULT_NUMBER_OF_BYTES
	 * bytes.
	 */
	public Nonce() {
		bytes = new byte[DEFAULT_NUMBER_OF_BYTES] ;
		r.nextBytes(bytes) ;
		
		stringRepresentation = null ;	// only set string when first needed
	}
	
	/**
	 * A random 'numBytes' long Nonce object.
	 * @param numBytes
	 */
	public Nonce( int numBytes ) {
		assert( numBytes > 0 ) ;
		assert( numBytes <= MAXIMUM_NUMBER_OF_BYTES ) ;
		
		bytes = new byte[numBytes] ;
		r.nextBytes(bytes) ;
		
		stringRepresentation = null ;	// only set string when first needed
	}
	
	/**
	 * Copy constructor.  Constructs a Nonce with the same value
	 * as that provided.  We keep a reference to n's string representation,
	 * if it has one.
	 * @param n
	 */
	public Nonce( Nonce n ) {
		bytes = new byte[n.bytes.length] ;
		for ( int i = 0; i < bytes.length; i++ )
			bytes[i] = n.bytes[i] ;
		
		stringRepresentation = n.stringRepresentation ;
	}
	
	/**
	 * Constructs a Nonce object from the provided byte representation.
	 * This representation is assumed to be written by a Nonce object
	 * (not simply as a series of random bytes) and thus the first
	 * byte provides the length of the nonce.  DO NOT CREATE A NONCE
	 * FROM A BYTE ARRAY THAT WAS NOT WRITTEN TO BY A NONCE OBJECT
	 * @param ar
	 */
	public Nonce( byte [] ar ) {
		bytes = new byte[ar[0] - Nonce.LENGTH_TO_BYTE_REPRESENTATION_OFFSET] ;
		for ( int i = 0; i < bytes.length; i++ )
			bytes[i] = ar[i+1] ;
		
		stringRepresentation = null ;
	}
	
	/**
	 * Constructs a Nonce object from the provided byte representation,
	 * beginning reading at offset.
	 * This representation is assumed to be written by a Nonce object
	 * (not simply as a series of random bytes) and thus the first
	 * byte provides the length of the nonce.  DO NOT CREATE A NONCE
	 * FROM A BYTE ARRAY THAT WAS NOT WRITTEN TO BY A NONCE OBJECT
	 * @param ar
	 */
	public Nonce( byte [] ar, int offset ) {
		bytes = new byte[ar[offset] - Nonce.LENGTH_TO_BYTE_REPRESENTATION_OFFSET] ;
		for ( int i = 0; i < bytes.length; i++ )
			bytes[i] = ar[i+offset+1] ;
		
		stringRepresentation = null ;
	}
	
	/**
	 * Constructs a Nonce object from the provided String representation.
	 * We use a URL-safe encoded base-64 representation (i.e., NOT
	 * standard base-64, which is apparently not URL-safe).
	 * 
	 * We keep a reference to strRep, as the string-representation
	 * of this nonce.
	 * 
	 * @param strRep
	 */
	public Nonce( String strRep ) throws IOException {
		// convert +/ to -_ just in case this was not using URL_SAFE encoding.
		String strRepConv = Nonce.fromSMSSafe(strRep) ;
		bytes = Base64.decode(strRepConv, Base64.URL_SAFE) ;
		stringRepresentation = strRepConv ;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// EXTENDING NONCES
	//
	// If you wish to generate new Nonces from a base nonce that are guaranteed
	// to be distinct from every Nonce generated from some other base, these
	// methods can be used for extension and comparison.
	//
	// Note: our guarantee of uniqueness, given different bases, assumes those
	// bases have the same length.
	//
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Extend this nonce with the provided number of additional random bytes.
	 * 
	 * @param numBytes The number of random bytes with which we will extend 'this'
	 * to produce a new Nonce.
	 */
	public Nonce extendWithBytes(int numBytes) {
		
		assert( numBytes + bytes.length <= MAXIMUM_NUMBER_OF_BYTES ) ;
		
		byte [] newBytes = new byte[numBytes] ;
		r.nextBytes(newBytes) ;
		
		byte [] extendedBytes = new byte[bytes.length + numBytes] ;
		for ( int i = 0; i < bytes.length; i++ )
			extendedBytes[i] = bytes[i] ;
		for ( int j = 0; j < newBytes.length; j++ )
			extendedBytes[j + bytes.length] = newBytes[j] ;
		
		Nonce n = new Nonce() ;
		n.bytes = extendedBytes ;
		return n ;
	}
	
	/**
	 * Is it possible that 'this' was generated by a call to n.extendWithBytes( x ) ?
	 * @param n
	 * @return
	 */
	public boolean isExtensionOf( Nonce n ) {
		return n.isBaseOf(this) ;
	}
	
	/**
	 * Is it plausible that 'n' was generated by a call to this.extendWithBytes( x ) ?
	 * @param n
	 * @return
	 */
	public boolean isBaseOf( Nonce n ) {
		if ( bytes.length > n.bytes.length )
			return false ;
		for ( int i = 0; i < bytes.length; i++ )
			if ( bytes[i] != n.bytes[i] )
				return false ;
		return true ;
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// COMPARING NONCE OBJECTS
	//
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns a small, positive integer value ("small" meaning
	 * many orders of magnitude less than Integer.MAX_VALUE) that
	 * is fully determined by the bytes of this Nonce.
	 */
	public int smallInt() {
		int res = 1 ;
		for ( int i = 0; i < bytes.length && i < 3; i++ ) {
			res *= 0x100 ;
			res += Math.abs((int)bytes[i]) ;
		}
		return res ;
	}
	
	@Override
	public int hashCode() {
		if ( bytes.length > 0 )
			return bytes[0] ;
		return 0 ;
	}
	
	/**
	 * Returns whether these two nonces are equal.  If both are 'null'
	 * returns true.
	 * 
	 * @param n1
	 * @param n2
	 * @return
	 */
	public static boolean equals( Nonce n1, Nonce n2 ) {
		if ( n1 == null && n2 == null )
			return true ;
		if ( n1 != null && n2 != null )
			return n1.equals(n2) ;
		return false ;
	}
	
	/**
	 * Is this object equal to the provided nonce?
	 */
	public boolean equals( Nonce n ) {
		if ( n == null )
			return false ;
		if ( bytes.length != n.bytes.length )
			return false ;
		for ( int i = 0; i < bytes.length; i++ )
			if ( bytes[i] != n.bytes[i] )
				return false ;
		
		return true ;
	}
	
	/**
	 * Is this object equal to the provided byte array, which
	 * holds a byte-array representation of a Nonce object?
	 * (note: this is NOT the same as "the bytes of this nonce value,"
	 * since our byte-array representation explicitly encodes length).
	 * @param ar
	 * @return
	 */
	public boolean equals( byte [] ar, int index ) {
		try {
			if ( bytes.length + Nonce.LENGTH_TO_BYTE_REPRESENTATION_OFFSET != ar[index] )
				return false ;
			for ( int i = 0; i < bytes.length; i++ )
				if ( bytes[i] != ar[i+1 + index] )
					return false ;
			
			return true ;
		} catch ( ArrayIndexOutOfBoundsException e ) {
			return false ;
		}
	}
	
	
	/**
	 * Is this object equal to the provided string representation?
	 * 
	 * If the answer is yes, and this object does not currently hold
	 * a string representation, it may generate one for itself and retain it.
	 * 
	 * @param strRep
	 * @return
	 */
	public boolean equals( String strRep ) {
		if ( stringRepresentation == null ) {
			// This method call instantiates and sets a
			// the field 'stringRepresentation', in addition
			// to returning 'null' if it fails (for whatever reason).
			if ( toString() == null )
				return false ;
		}
		
		return stringRepresentation.equals(strRep) ;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj instanceof Nonce )
			return this.equals( (Nonce)obj ) ;
		if ( obj instanceof byte [] )
			return this.equals( (byte[]) obj ) ;
		if ( obj instanceof String ) 
			return this.equals( (String) obj ) ;
		return false ;
	}
	
	
	/**
	 * Comparison.  Returns -1, 0, or 1 if this is less than, equal to,
	 * or greater than the parameter.
	 * 
	 * For different-length nonces, the shorter is the lesser.
	 * 
	 * For the same length, we do a straightforward "big-endian" comparison.
	 * a is smaller than b if a.bytes[i] < b.bytes[i] and a.bytes[j] == b.bytes[j]
	 * for all j < i.
	 * 
	 * @param n
	 * @return
	 */
	public int compareTo(Nonce n) {
		if ( lengthAsBytes() != n.lengthAsBytes() )
			return lengthAsBytes() < n.lengthAsBytes() ? -1 : 1 ;
		
		for ( int i = 0; i < bytes.length; i++ ) {
			if ( bytes[i] != n.bytes[i] )
				return bytes[i] < n.bytes[i] ? -1 : 1 ;
		}
		
		return 0 ;
	}

	
	///////////////////////////////////////////////////////////////////////////
	//
	// INFORMATION FROM NONCE OBJECTS
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Returns the number of "value bytes" in this nonce.
	 * This represents the actual Nonce data, NOT the number of
	 * bytes it would take to encode this object within a byte
	 * array.  Use lengthAsBytes() for that.
	 */
	public int numberOfBytes() {
		return bytes.length ;
	}
	
	///////////////////////////////////////////////////////////////////////////
	//
	// REPRESENTING NONCE OBJECTS
	//
	// Converting Nonce objects to string or byte array formats.
	//
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public String toString() {
		if ( stringRepresentation == null ) {
			try { 
				stringRepresentation = Base64.encodeBytes(bytes, Base64.URL_SAFE) ;
			} catch( Exception e ) {
				return null ;
			}
		}
		
		return stringRepresentation ;
	}
	
	
	/**
	 * Writes the Nonce as bytes in the provided array.
	 * Returns the number of bytes written, which will
	 * probably NOT be equal to the result of 
	 * this.numberOfBytes().
	 * 
	 * @param ar
	 * @param index
	 * @return
	 * 
	 * @throws Array out of bounds exception
	 */
	public int writeAsBytes( byte [] ar, int index ) {
		// Write the length as the first byte, then the rest.
		ar[index] = (byte) ( bytes.length + Nonce.LENGTH_TO_BYTE_REPRESENTATION_OFFSET ) ;
		for ( int i = 0; i < bytes.length; i++ )
			ar[i+index+1] = bytes[i] ;
		
		return bytes.length + 1 ;
	}
	
	/**
	 * Returns the number of bytes this Nonce would use
	 * when writing to a byte array.  This value is distinct
	 * from numberOfBytes in that 'numberOfBytes' describes the
	 * "value-relevant" number of bytes, i.e., the true complexity
	 * of the Nonce.  This method, by contrast, returns the
	 * number of bytes required to represent this Nonce value
	 * in a form from which a Nonce object can be constructed.
	 * This value is necessarily at least as large as numberOfBytes(),
	 * but may be larger.
	 * 
	 * @return
	 */
	public int lengthAsBytes() {
		return bytes.length + 1 ;
	}

	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// DIRECT BYTE ACCESS
	//
	// Dangerous!  You shouldn't really use this for anything.
	//
	///////////////////////////////////////////////////////////////////////////
	
	public byte directByteAccess( int index ) {
		return bytes[index] ;
	}
	
	
	/////////////////////////////////////////////
	// serializable methods
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// Write as byte representation
		byte [] rep = new byte[ this.lengthAsBytes() ] ;
		this.writeAsBytes(rep, 0) ;
		stream.write(rep) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Read as byte representation
		int len = stream.readByte() - Nonce.LENGTH_TO_BYTE_REPRESENTATION_OFFSET ;
		bytes = new byte[len] ;
		for ( int i = 0; i < len; i++ )
			bytes[i] = stream.readByte() ;
		
		stringRepresentation = null ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
}
