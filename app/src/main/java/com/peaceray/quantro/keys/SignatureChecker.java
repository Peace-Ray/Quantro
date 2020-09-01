package com.peaceray.quantro.keys;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import com.peaceray.quantro.utils.Base64;


/**
 * A utility class for public-key signature verification.  Intended as a replacement
 * for Security's signature check code, as well as a means to verify mine own signed
 * keys.
 * 
 * Signatures are assumed to be SHA1withRSA.
 * 
 * Usage note: inlined public key signatures should be removed and replaced by obfuscated
 * generation of byte strings.
 * 
 * @author Jake
 *
 */
public class SignatureChecker {
	
	public static final String TAG = "SignatureChecker" ;
	
	private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
	
	/*
	 * Public Key Obfuscation
	 * Random Seed 879871493434471
	 * Public Key "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9Glygs07SwX1Fxx9oGur0PjangFZ8dKH8LcQZA64O2Kxa3MpSl/GMntkFV61EJvtFx9QVMsJWl7zaxPEadvqw0mcd8qQFgwmjfJuXbqFtYhfUd5p/v4jkqz4ZWfYLHPYSratklyc9bv6zrC1txtJQO86UuJSaYSj7dZm8gx9WHBfFkADzIsgU7EOZkpoRwMiCx8NYW7dAkPMFOeToKfdoTBlHNxahqmzr+rB7VRk1jb0LCe8Tn1cfY7WxVZIL6+/f4LFYRM5+fYpg3I/AFjhPxoRQc2rZK21f/6i3zY0EZb8Xmvu28tZkMiCGBiOoqdy/M9UvCdnFwYTk2g4qq+p7QIDAQAB"
	 * 
	 * String BYTES1 = "6gZVp6wxQo21W0uVtnGjl3iTZozKxJ7pB+k3EC7GoUHJcX7pO7geBZioiyfWEL/HceEZjHpwjbxYibyXhOWkApI7VKqF3H7Jfx/W6JC0en9ymmE0Mt5CasKGNI7QxaZlhf5nJzMXncIkgI0XZ6ywUquAGGPBEVrhQSDR1enkKKhOC/YfvqgEKlYHJ4F9X3meGTn20Ia/2/+OKlh+0J1xUnfV5brGGXUPI0iU7tuliF7iDLy+urxQn94BgPTeROg9BohbgN0buK6uKPv3VhRg4hc5wG8OGl8OJx0R5q6CPiNZK9bosmxPwPln8MRJ8oDlZEWieuRBk6XkkdTe13fFb/KBeFuUf4A93kUXrFmtMcNxx4w1goMG83ujSG1EUidE9T4L4OId"
	 * String BYTES2 = "Axx9jSYz54YvOFCau3Pg3VvlkvYw2hTyxRfUDpaEysDlHiKn3lkoaP9x52TR7HkoUU3Eb+E/QcmnASD//Dvi3V5h6hXEnLaVGIEfvtse8Kmw3yTqLHEITjRIOnVJ2FPn5jEm7mArJM2aVRUQWeT/Fux0cHIL+TSgGDd/eL0pTIGNWOGQQfGAU1+FCjhrXdDxS+Rjcb9aYLT15id2fXwOmHI1OTJ2kmylb87obFiAMCVqisZKSlbYl95nc0sQsardTS/ojxZ4u1OJacMACzrGprkfrrjzPDj93LvayuPkLSLmcWCLuuwARqEsQBGQHtHW+t72McZVKE5todnkxqoPxZv8D6z+SJC0XH0SDpypYsbG+ynQElL12gAuFmRu093U3gCoxwMw"
	 * String BYTES3 = "lRPy9wMPLgcIaUy26dHxZHo48ZvxxWhQtZCBL/DbspLci7ddP5tRKn/vT++j+NMstJPJf0xo8mI5nXnLbHpiH/LJiGjwo7udQJQgGFsRgeL6GHxY4JchYxdF3nsmmXhbTys/u1cIDSozT4jucEP5T42C6rTxMbzSEnNH9U6izRuciXGE2UEb7ZERYwsGV1VdIhaTQSasaZt/zHQKGWk1FjkNzOWCBHRPbBzdo4OJCBhiH4PqDD0AS8fvigFAxrnQuM+fvn9vhB0np7UqEnLYu2g6LgqTlB4g6AJKI4FE3P1gTtT6o81Fn/rVvw8j9jxYRm70bo8kFE71kRcsHd2wmauspMLj5ZFDbwAcqelNGpGAF2GwOPlZ/ZG99adYxOMDyMbSStrb"
	 * 
	 * Reconstruct with 
	 * 
		byte [] b1 = Base64.decode(BYTES1) ;
		byte [] b2 = Base64.decode(BYTES2) ;
		byte [] b3 = Base64.decode(BYTES3) ;
		for ( int i = 0; i < b1.length; i++ ) 
			b1[i] ^= b2[ ((int)b3[i] - Byte.MIN_VALUE) % b2.length ] + i * b3[i] ;
	 * 
	 * b1 now contains the public key
	 * Sanity check reconstruction: match
	 */
	
	// public static final String BYTES1 = "6gZVp6wxQo21W0uVtnGjl3iTZozKxJ7pB+k3EC7GoUHJcX7pO7geBZioiyfWEL/HceEZjHpwjbxYibyXhOWkApI7VKqF3H7Jfx/W6JC0en9ymmE0Mt5CasKGNI7QxaZlhf5nJzMXncIkgI0XZ6ywUquAGGPBEVrhQSDR1enkKKhOC/YfvqgEKlYHJ4F9X3meGTn20Ia/2/+OKlh+0J1xUnfV5brGGXUPI0iU7tuliF7iDLy+urxQn94BgPTeROg9BohbgN0buK6uKPv3VhRg4hc5wG8OGl8OJx0R5q6CPiNZK9bosmxPwPln8MRJ8oDlZEWieuRBk6XkkdTe13fFb/KBeFuUf4A93kUXrFmtMcNxx4w1goMG83ujSG1EUidE9T4L4OId" ;
	// public static final String BYTES2 = "Axx9jSYz54YvOFCau3Pg3VvlkvYw2hTyxRfUDpaEysDlHiKn3lkoaP9x52TR7HkoUU3Eb+E/QcmnASD//Dvi3V5h6hXEnLaVGIEfvtse8Kmw3yTqLHEITjRIOnVJ2FPn5jEm7mArJM2aVRUQWeT/Fux0cHIL+TSgGDd/eL0pTIGNWOGQQfGAU1+FCjhrXdDxS+Rjcb9aYLT15id2fXwOmHI1OTJ2kmylb87obFiAMCVqisZKSlbYl95nc0sQsardTS/ojxZ4u1OJacMACzrGprkfrrjzPDj93LvayuPkLSLmcWCLuuwARqEsQBGQHtHW+t72McZVKE5todnkxqoPxZv8D6z+SJC0XH0SDpypYsbG+ynQElL12gAuFmRu093U3gCoxwMw" ;
	// public static final String BYTES3 = "lRPy9wMPLgcIaUy26dHxZHo48ZvxxWhQtZCBL/DbspLci7ddP5tRKn/vT++j+NMstJPJf0xo8mI5nXnLbHpiH/LJiGjwo7udQJQgGFsRgeL6GHxY4JchYxdF3nsmmXhbTys/u1cIDSozT4jucEP5T42C6rTxMbzSEnNH9U6izRuciXGE2UEb7ZERYwsGV1VdIhaTQSasaZt/zHQKGWk1FjkNzOWCBHRPbBzdo4OJCBhiH4PqDD0AS8fvigFAxrnQuM+fvn9vhB0np7UqEnLYu2g6LgqTlB4g6AJKI4FE3P1gTtT6o81Fn/rVvw8j9jxYRm70bo8kFE71kRcsHd2wmauspMLj5ZFDbwAcqelNGpGAF2GwOPlZ/ZG99adYxOMDyMbSStrb" ;
	
	
	// PUBLIC KEY
	// This public key should not be in-lined as it is.  This should
	// be obfuscated, for example as the XOR of several bit strings.
	private static final String PUBLIC_KEY = "Nope" ; // "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA9Glygs07SwX1Fxx9oGur0PjangFZ8dKH8LcQZA64O2Kxa3MpSl/GMntkFV61EJvtFx9QVMsJWl7zaxPEadvqw0mcd8qQFgwmjfJuXbqFtYhfUd5p/v4jkqz4ZWfYLHPYSratklyc9bv6zrC1txtJQO86UuJSaYSj7dZm8gx9WHBfFkADzIsgU7EOZkpoRwMiCx8NYW7dAkPMFOeToKfdoTBlHNxahqmzr+rB7VRk1jb0LCe8Tn1cfY7WxVZIL6+/f4LFYRM5+fYpg3I/AFjhPxoRQc2rZK21f/6i3zY0EZb8Xmvu28tZkMiCGBiOoqdy/M9UvCdnFwYTk2g4qq+p7QIDAQAB" ;
    
	
	private byte [] mPublicKey ;
	private boolean mRecycled ;
	
	public SignatureChecker() {
		this( PUBLIC_KEY ) ;
	}
	
	/**
	 * Initializes a signature checker using the specified base64 public key.
	 * Both standard base 64 and URL-SAFE encoding are acceptable.
	 * 
	 * No reference to the publicKey is kept.
	 * 
	 * @param publicKey
	 */
	public SignatureChecker( String publicKey ) {
		// if has _ or -, treat as URL-SAFE.  Otherwise treat as standard.
		set( b64decode( publicKey ) ) ;
	}
	
	
	/**
	 * Initializes a signature checker using the specified base64 public key.
	 * 
	 * This is the recommended constructor for obfuscation and security.  This 
	 * method does not copy the provided byte array; it stores a reference to it.
	 * 
	 * Recommended usage:
	 * 
	 * 1. Construct the public key using deobfuscation operations such as bitwise XOR.
	 * 
	 * 2. Pass that key in to this constructor.
	 * 
	 * 3. Perform signature verification.
	 * 
	 * 4. Call .recycle() on this object to destroy the public key in-memory.
	 * 
	 * 5. Lose reference to this object and allow it to be reclaimed at the leisure
	 * 			of the GC.
	 * 
	 * @param publicKey
	 */
	public SignatureChecker( byte [] publicKey ) {
		set(publicKey) ;
	}
	
	
	private void set( byte [] publicKey ) {
		this.mPublicKey = publicKey ;
		this.mRecycled = false ;
		
		if ( this.mPublicKey == null )
			throw new IllegalArgumentException("Null or poorly-formatted public key.") ;
	}
	
	
	/**
	 * A wrapper method that allows both URL-SAFE and standard base64 decoding.
	 * @param data
	 * @return
	 */
	private byte [] b64decode( String data ) {
		try {
			if ( data.indexOf("-") != -1 || data.indexOf("_") != -1 )
				return Base64.decode(data, Base64.URL_SAFE) ;
			else
				return Base64.decode(data) ;
		} catch( IOException ioe ) {
			return null ;
		}
	}
	
	
	/**
	 * A standard verification method.  Returns 'true' if the provided 
	 * data was signed (after .getBytes()) with the keypair used at construction.
	 * 'signature' is assumed base64-encoded.
	 * 
	 * @param data
	 * @param signature
	 * @return
	 */
	public boolean verify( String data, String signature ) {
		if ( mRecycled )
			throw new IllegalStateException("Can't use after 'recycle'") ;
		
		Signature sig = makeSignature() ;
		try {
			sig.initVerify(generatePublicKey( mPublicKey )) ;
			sig.update(data.getBytes()) ;
			return sig.verify(b64decode(signature)) ;
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
	
	
	/**
	 * A standard verification method.  Returns 'true' if the provided 
	 * data was signed (after .getBytes()) with the keypair used at construction.
	 * 'signature' is assumed base64-encoded.
	 * 
	 * @param data
	 * @param signature
	 * @return
	 */
	public boolean verify( String data, byte [] signature ) {
		if ( mRecycled )
			throw new IllegalStateException("Can't use after 'recycle'") ;
		
		Signature sig = makeSignature() ;
		try {
			sig.initVerify(generatePublicKey( mPublicKey )) ;
			sig.update(data.getBytes()) ;
			return sig.verify(signature) ;
		} catch (Exception e) {
			throw new RuntimeException(e) ;
		}
	}
	
	
	
	public void recycle() {
		mRecycled = true ;
		for ( int i = 0; i < mPublicKey.length; i++ )
			mPublicKey[i] = 0 ;
	}
	
	
	public static Signature makeSignature() {
		try {
			return Signature.getInstance(SIGNATURE_ALGORITHM) ;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e) ;
		}
	}
	
	/**
     * Generates a PublicKey instance from a string containing the
     * Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    public static PublicKey generatePublicKey( byte [] key ) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(key));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    
    
	
}
