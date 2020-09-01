package com.peaceray.quantro.utils.nfc;

import android.annotation.TargetApi;
import android.net.Uri;
import android.nfc.FormatException;

/**
 * Extends NdefRecord_API_14 with support for those methods
 * added in API_16.
 * 
 * @author Jake
 *
 */
@TargetApi(16)
class NdefRecord_API_16 extends NdefRecord_API_14 {
	
	NdefRecord_API_16() {
		mRecord = null ;
	}
	
	NdefRecord_API_16( short tnf, byte[] type, byte[] id, byte[] payload ) {
		mRecord = new android.nfc.NdefRecord( tnf, type, id, payload ) ;
	}
	
	
	NdefRecord_API_16( byte[] data ) throws FormatException {
		mRecord = new android.nfc.NdefRecord( data ) ;
	}

	/**
	 * Create a new Android Application Record (AAR).
	 * @param packageName
	 * @return
	 */
	public static NdefRecord createApplicationRecord(String packageName) {
		NdefRecord_API_16 instance = new NdefRecord_API_16() ;
		instance.mRecord = android.nfc.NdefRecord.createApplicationRecord(packageName) ;
		return instance.mRecord == null ? null : instance ;
	}
	
	/**
	 * Create a new NDEF Record containing external (application-specific) data.
	 * @param domain
	 * @param type
	 * @param data
	 * @return
	 */
	public static NdefRecord createExternal(String domain, String type, byte[] data) {
		NdefRecord_API_16 instance = new NdefRecord_API_16() ;
		instance.mRecord = android.nfc.NdefRecord.createExternal(domain, type, data) ;
		return instance.mRecord == null ? null : instance ;
	}
	
	/**
	 * Create a new NDEF Record containing MIME data.
	 * @param mimeType
	 * @param mimeData
	 * @return
	 */
	public static NdefRecord createMime(String mimeType, byte[] mimeData) {
		NdefRecord_API_16 instance = new NdefRecord_API_16() ;
		instance.mRecord = android.nfc.NdefRecord.createMime(mimeType, mimeData) ;
		return instance.mRecord == null ? null : instance ;
	}
	
	/**
	 * Create a new NDEF Record containing a URI.
	 * @param uriString
	 * @return
	 */
	public static NdefRecord createUri(String uriString) {
		NdefRecord_API_16 instance = new NdefRecord_API_16() ;
		instance.mRecord = android.nfc.NdefRecord.createUri(uriString) ;
		return instance.mRecord == null ? null : instance ;
	}
	
	/**
	 * Create a new NDEF Record containing a URI.
	 * @param uri
	 * @return
	 */
	public static NdefRecord createUri(Uri uri) {
		NdefRecord_API_16 instance = new NdefRecord_API_16() ;
		instance.mRecord = android.nfc.NdefRecord.createUri(uri) ;
		return instance.mRecord == null ? null : instance ;
	}
	
	public static NdefRecord createRecord( Object record ) {
		if ( record instanceof android.nfc.NdefRecord ) {
			NdefRecord_API_16 instance = new NdefRecord_API_16() ;
			instance.mRecord = (android.nfc.NdefRecord) record ;
			return instance.mRecord == null ? null : instance ;
		}
		
		return null ;
	}
	
	
	/**
	 * Returns true if the specified NDEF Record contains identical tnf, type, id and payload fields.
	 */
	public boolean equals(Object obj) {
		if ( obj instanceof NdefRecord_API_16 )
			return mRecord.equals(((NdefRecord_API_16)obj).mRecord) ;
		return mRecord.equals(obj) ;
	}

	
	
	/**
	 * Map this record to a MIME type, or return null if it cannot be mapped.
	 * @return
	 */
	public String toMimeType() {
		return mRecord.toMimeType() ;
	}
	
	
	/**
	 * Map this record to a URI, or return null if it cannot be mapped.
	 * @return
	 */
	public Uri toUri() {
		return mRecord.toUri() ;
	}
	
}
