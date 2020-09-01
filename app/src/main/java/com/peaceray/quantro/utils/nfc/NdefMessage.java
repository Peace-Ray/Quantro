package com.peaceray.quantro.utils.nfc;

import android.nfc.FormatException;

import com.peaceray.quantro.utils.VersionCapabilities;

public abstract class NdefMessage {
	
	/**
	 * Construct an NDEF Message by parsing raw bytes.
	 * @param data
	 * @return
	 * @throws FormatException 
	 */
	public static NdefMessage newNdefMessage( byte [] data ) throws FormatException {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return new NdefMessage_API_16( data ) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_9 ) )
			return new NdefMessage_API_9( data ) ;
		return null ;
	}
	
	/**
	 * Construct an NDEF Message from one or more NDEF Records.
	 * @param record
	 * @param records
	 */
	public static NdefMessage newNdefMessage(NdefRecord record, NdefRecord... records) {
		NdefRecord [] array = new NdefRecord[records.length + 1] ;
		array[0] = record ;
		for ( int i = 0; i < records.length; i++ )
			array[i+1] = records[i] ;
		
		return newNdefMessage(array) ;
	}
	
	/**
	 * Construct an NDEF Message from one or more NDEF Records.
	 * @param records
	 */
	public static NdefMessage newNdefMessage(NdefRecord[] records) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return new NdefMessage_API_16( records ) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_9 ) )
			return new NdefMessage_API_9( records ) ;
		return null ;
	}
	
	
	static NdefMessage wrapAndroidNfcNdefMessage( Object message ) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_16 ) )
			return new NdefMessage_API_16( message ) ;
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_9 ) )
			return new NdefMessage_API_9( message ) ;
		return null ;
	}
	

	/**
	 * Describe the kinds of special objects contained in this Parcelable's marshalled representation.
	 * @return
	 */
	public abstract int describeContents() ;
	
	/**
	 * Returns true if the specified NDEF Message contains identical NDEF Records.
	 */
	public abstract boolean equals(Object obj) ;
	
	/**
	 * Return the length of this NDEF Message if it is written to a byte array with toByteArray().
	 * @return
	 */
	public abstract int getByteArrayLength() ;
	
	/**
	 * Get the NDEF Records inside this NDEF Message.
	 * @return
	 */
	public abstract NdefRecord[] getRecords() ;
	
	/**
	 * Returns an integer hash code for this object.
	 */
	public abstract int hashCode() ;
	
	/**
	 * Return this NDEF Message as raw bytes.
	 * @return
	 */
	public abstract byte[] toByteArray() ;
	
	/**
	 * Returns a string containing a concise, human-readable description of this object.
	 */
	public abstract String toString() ;
	
	
	/**
	 * Returns an instance of android.nfc.NdefMessage representing the 
	 * same data.
	 * @return
	 */
	public abstract Object getAndroidNfcNdefMessage() ;
	
}
