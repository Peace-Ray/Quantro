package com.peaceray.quantro.utils;

import java.security.SecureRandom;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;


/**
 * A wrapper class to avoid the pitfalls associated with the raw AndroidID value.
 * 
 * This class provides one method, get(), which returns (as a string) a value which
 * can be used as the device's Android ID.
 * 
 * If possible, this is the same value as returned by Secure.getString(context.getContentResolver(), Secure.ANDROID_ID).
 * There are two exceptions.
 * 
 * First, Android 2.2 had a bug where some installations returned a default Android ID
 * "9774d56d682e549c" instead of a random-per-device ID.  Second, some installations
 * (e.g. the API 4 simulator) return 'null' when android ID is queried.  In these two
 * cases, a random android ID is generated and stored in preferences.
 * 
 * @author Jake
 *
 */
public class AndroidID {
	
	private static final String KEY = "com.peaceray.quantro.utils.AndroidID.get" ;

	public static final String get( Context context ) {
		String id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID) ;
		
		if ( id == null || id.equals("9774d56d682e549c") ) {
			// attempt to load?
			id = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, null) ;
			
			if ( id == null ) {
				// generate a random 64-bit hex string.
				SecureRandom r = new SecureRandom() ;
				long val = r.nextLong() ;
				id = Long.toHexString(val) ;
				
				SharedPreferences.Editor editor = 
					PreferenceManager.getDefaultSharedPreferences(context).edit();
				editor.putString(KEY, id) ;
				editor.commit();
			}
		}
		
		return id ;
	}
	
}
