package com.peaceray.quantro.keys;

import com.peaceray.quantro.keys.Key;

import android.content.Context;
import android.util.Log;

public class FastKeyCheck {
	
	
	private static final String TAG = "FastKeyCheck" ;

	/**
	 * A dangerous method to use, as it supplies a single-point of failure
	 * for key verification.
	 * 
	 * This is included solely for easy implementation of key check blocks.
	 * Once those blocks are functioning, this code should be in-lined as
	 * deeply as possible.
	 * 
	 * Returns 'true' if an XL key is stored, it is both valid and activated,
	 * and the key has been signed by the Quantro server.  Returns 'false'
	 * otherwise.
	 * 
	 * @return
	 */
	public static boolean isXLOK( Context context ) {
		Log.e(TAG, "THIS CALL SHOULD BE IN-LINED AND OBFUSCATED") ;
		QuantroXLKey key = KeyStorage.getXLKey(context) ;
    	return isOK( context, key, true ) ;
	}
	
	public static boolean isXLOKNoSignatureCheck( Context context ) {
		QuantroXLKey key = KeyStorage.getXLKey(context) ;
		return isOK( context, key, false ) ;
	}
	
	
	/**
	 * A dangerous method to use, as it supplies a single-point of failure
	 * for key verification.
	 * 
	 * This is included solely for easy implementation of key check blocks.
	 * Once those blocks are functioning, this code should be in-lined as
	 * deeply as possible.
	 * 
	 * Returns 'true' if the provided key is both valid and activated,
	 * and the key has been signed by the Quantro server.  Returns 'false'
	 * otherwise.
	 * 
	 * @return
	 */
	public static boolean isOK( Context context, Key key, boolean checkSignature ) {
		boolean keyOK = key != null && key.isActivated() && key.isValid() ;
    	if ( keyOK && checkSignature ) {
    		SignatureChecker sc = new SignatureChecker(  ) ;
    		keyOK = sc.verify(key.getSignedString(), key.getKeySignature()) ;
    		sc.recycle() ;
    	}
    	
    	return keyOK ;
	}
	
}
