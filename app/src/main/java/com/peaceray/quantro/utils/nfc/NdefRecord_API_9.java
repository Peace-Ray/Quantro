package com.peaceray.quantro.utils.nfc;

import android.annotation.TargetApi;
import android.net.Uri;
import android.nfc.FormatException;
import android.os.Parcel;

/**
 * This subclass of com.peaceary.quantro.utils.nfc.NdefRecord implements its methods
 * in a way that is safe for API 9 or higher.
 * 
 * @author Jake
 *
 */
@TargetApi(9)
class NdefRecord_API_9 extends NdefRecord {

	protected android.nfc.NdefRecord mRecord = null ;
	
	NdefRecord_API_9() {
		mRecord = null ;
	}
	
	NdefRecord_API_9( short tnf, byte[] type, byte[] id, byte[] payload ) {
		mRecord = new android.nfc.NdefRecord( tnf, type, id, payload ) ;
	}
	
	
	NdefRecord_API_9( byte[] data ) throws FormatException {
		mRecord = new android.nfc.NdefRecord( data ) ;
	}
	
	
	public static NdefRecord createRecord( Object record ) {
		if ( record instanceof android.nfc.NdefRecord ) {
			NdefRecord_API_9 instance = new NdefRecord_API_9() ;
			instance.mRecord = (android.nfc.NdefRecord) record ;
			return instance.mRecord == null ? null : instance ;
		}
		
		return null ;
	}
	
	
	/**
	 * Describe the kinds of special objects contained in this Parcelable's marshalled representation.
	 * @return
	 */
	public int describeContents() {
		return mRecord.describeContents() ;
	}
	
	/**
	 * Returns true if the specified NDEF Record contains identical tnf, type, id and payload fields.
	 */
	public boolean equals(Object obj) {
		if ( obj instanceof NdefRecord_API_9 )
			return mRecord.equals(((NdefRecord_API_9)obj).mRecord) ;
		return mRecord.equals(obj) ;
	}
	
	/**
	 * Returns the variable length ID.
	 * @return
	 */
	public byte[] getId() {
		return mRecord.getId() ;
	}
	
	/**
	 * Returns the variable length payload.
	 * @return
	 */
	public byte[] getPayload() {
		return mRecord.getPayload() ;
	}
	
	/**
	 * Returns the 3-bit TNF.
	 * @return
	 */
	public short getTnf() {
		return mRecord.getTnf() ;
	}
	
	/**
	 * Returns the variable length Type field.
	 * @return
	 */
	public byte[] getType() {
		return mRecord.getType() ;
	}
	
	/**
	 * Returns an integer hash code for this object.
	 */
	public int hashCode() {
		return mRecord.hashCode() ;
	}
	
	/**
	 * This method was deprecated in API level 16. use toByteArray() instead
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public byte[] toByteArray() {
		return mRecord.toByteArray() ;
	}
	
	/**
	 * Map this record to a MIME type, or return null if it cannot be mapped.
	 * @return
	 */
	public String toMimeType() {
		// UNSUPPORTED
		return null ;
	}
	
	/**
	 * Returns a string containing a concise, human-readable description of this object.
	 */
	public String toString() {
		return mRecord.toString() ;
	}
	
	/**
	 * Map this record to a URI, or return null if it cannot be mapped.
	 * @return
	 */
	public Uri toUri() {
		// UNSUPPORTED
		return null ;
	}
	
	/**
	 * Returns an instance of android.nfc.NdefRecord representing
	 * the same data as this object.
	 * @return
	 */
	public Object getAndroidNfcNdefRecord() {
		return mRecord ;
	}
	
}
