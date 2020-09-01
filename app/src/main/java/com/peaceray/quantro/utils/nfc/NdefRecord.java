package com.peaceray.quantro.utils.nfc;

import android.net.Uri;
import android.nfc.FormatException;

import com.peaceray.quantro.utils.VersionCapabilities;

/**
 * This abstract class implements the same method signatures as
 * android.nfc.NdefRecord.  However, whereas android.nfc.NdefRecord requires
 * Android API 9 or above, this abstract class is functional under
 * any supported Android version.  Its static methods return subclass
 * instances; those subclasses may support only certain API versions,
 * but that's fine.
 * 
 * Also adds a few extra methods, whose function is to indicate whether
 * instances can be created and the capabilities of those instances.
 * 
 * The underlying NdefRecord can be retrieved using getAndroidNfcNdefRecord()
 * and casting the result.
 * 
 * Creation should be done using the provided factory (new()) or create...() methods.
 * If these return 'null', it indicates that the specified creation method is not
 * supported by your current API version.
 * 
 * @author Jake
 *
 */
public abstract class NdefRecord {

	public static boolean isSupported() {
		return VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_9 ) ;
	}
	
	public static NdefRecord newNdefRecord( short tnf, byte[] type, byte[] id, byte[] payload ) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return new NdefRecord_API_16( tnf, type, id, payload ) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return new NdefRecord_API_14( tnf, type, id, payload ) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_9 ) )
			return new NdefRecord_API_9( tnf, type, id, payload ) ;
		
		return null ;
	}
	
	public static NdefRecord newNdefRecord( byte [] data ) throws FormatException {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return new NdefRecord_API_16( data ) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return new NdefRecord_API_14( data ) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_9 ) )
			return new NdefRecord_API_9( data ) ;
		
		return null ;
	}

	/**
	 * Create a new Android Application Record (AAR).
	 * @param packageName
	 * @return
	 */
	public static NdefRecord createApplicationRecord(String packageName) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return NdefRecord_API_16.createApplicationRecord(packageName) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return NdefRecord_API_14.createApplicationRecord(packageName) ;
		// supported by 14 or higher.
		return null ;
	}
	
	/**
	 * Create a new NDEF Record containing external (application-specific) data.
	 * @param domain
	 * @param type
	 * @param data
	 * @return
	 */
	public static NdefRecord createExternal(String domain, String type, byte[] data) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return NdefRecord_API_16.createExternal(domain, type, data) ;
		// supported by 16 or higher.
		return null ;
	}
	
	/**
	 * Create a new NDEF Record containing MIME data.
	 * @param mimeType
	 * @param mimeData
	 * @return
	 */
	public static NdefRecord createMime(String mimeType, byte[] mimeData) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return NdefRecord_API_16.createMime(mimeType, mimeData) ;
		// supported by 16 or higher.
		return null ;
	}
	
	/**
	 * Create a new NDEF Record containing a URI.
	 * @param uriString
	 * @return
	 */
	public static NdefRecord createUri(String uriString) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return NdefRecord_API_16.createUri(uriString) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return NdefRecord_API_14.createUri(uriString) ;
		// supported by 14 or higher.
		return null ;
	}
	
	/**
	 * Create a new NDEF Record containing a URI.
	 * @param uri
	 * @return
	 */
	public static NdefRecord createUri(Uri uri) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return NdefRecord_API_16.createUri(uri) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return NdefRecord_API_14.createUri(uri) ;
		// supported by 14 or higher.
		return null ;
	}
	
	/**
	 * Creates a new NDEF record containing the provided android.nfc.NdefRecord.
	 * @param obj
	 * @return
	 */
	public static NdefRecord createRecord( Object obj ) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return NdefRecord_API_16.createRecord(obj) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return NdefRecord_API_14.createRecord(obj) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_9 ) )
			return NdefRecord_API_9.createRecord(obj) ;
		
		return null ;
	}
	
	
	/**
	 * Describe the kinds of special objects contained in this Parcelable's marshalled representation.
	 * @return
	 */
	public abstract int describeContents() ;
	
	/**
	 * Returns true if the specified NDEF Record contains identical tnf, type, id and payload fields.
	 */
	public abstract boolean equals(Object obj) ;
	
	/**
	 * Returns the variable length ID.
	 * @return
	 */
	public abstract byte[] getId() ;
	
	/**
	 * Returns the variable length payload.
	 * @return
	 */
	public abstract byte[] getPayload() ;
	
	/**
	 * Returns the 3-bit TNF.
	 * @return
	 */
	public abstract short getTnf() ;
	
	/**
	 * Returns the variable length Type field.
	 * @return
	 */
	public abstract byte[] getType() ;
	
	/**
	 * Returns an integer hash code for this object.
	 */
	public abstract int hashCode() ;
	
	/**
	 * This method was deprecated in API level 16. use toByteArray() instead
	 * @return
	 */
	public abstract byte[] toByteArray() ;
	
	/**
	 * Map this record to a MIME type, or return null if it cannot be mapped.
	 * @return
	 */
	public abstract String toMimeType() ;
	
	/**
	 * Returns a string containing a concise, human-readable description of this object.
	 */
	public abstract String toString() ;
	
	/**
	 * Map this record to a URI, or return null if it cannot be mapped.
	 * @return
	 */
	public abstract Uri toUri() ;
	
	
	/**
	 * Returns an instance of android.nfc.NdefRecord representing
	 * the same data as this object.
	 * @return
	 */
	public abstract Object getAndroidNfcNdefRecord() ;
	
	
}
