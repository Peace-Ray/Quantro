package com.peaceray.quantro.utils.nfc;

import android.annotation.TargetApi;
import android.nfc.FormatException;
import android.util.Log;

@TargetApi(9)
class NdefMessage_API_9 extends NdefMessage {
	
	private static final String TAG = "NdefMessage_API_9" ;
	
	protected android.nfc.NdefMessage mMessage ;
	
	protected NdefMessage_API_9() {
		mMessage = null ;
	}
	
	/**
	 * Construct an NDEF Message by parsing raw bytes.
	 * @param data
	 * @throws FormatException 
	 */
	NdefMessage_API_9(byte[] data) throws FormatException {
		mMessage = new android.nfc.NdefMessage(data) ;
	}
	
	/**
	 * Construct an NDEF Message from one or more NDEF Records.
	 * @param records
	 */
	NdefMessage_API_9(NdefRecord[] records) {
		android.nfc.NdefRecord [] ndef_records = new android.nfc.NdefRecord[records.length] ;
		for ( int i = 0; i < records.length; i++ ) {
			Log.d(TAG, "setting android.nfc.NdefRecord " + i + " to inner of " + records[i] + " which is " + records[i].getAndroidNfcNdefRecord()) ;
			ndef_records[i] = (android.nfc.NdefRecord) records[i].getAndroidNfcNdefRecord() ;
		}
		mMessage = new android.nfc.NdefMessage(ndef_records) ;
	}
	
	/**
	 * Construct an NDEF Message from the android.nfc.NdefMessage instance.
	 * @param message
	 */
	NdefMessage_API_9(Object message) {
		mMessage = (android.nfc.NdefMessage) message ;
	}
	

	@Override
	public int describeContents() {
		return mMessage.describeContents() ;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof NdefMessage_API_9 ) {
			return mMessage.equals(((NdefMessage_API_9)obj).mMessage) ;
		}
		return mMessage.equals(obj) ;
	}

	@Override
	public int getByteArrayLength() {
		// the method getByteArrayLength was addid in API 16.
		return mMessage.toByteArray().length ;
	}

	@Override
	public NdefRecord[] getRecords() {
		android.nfc.NdefRecord [] ndef_records = mMessage.getRecords() ;
		if ( ndef_records == null )
			return null ;
		
		NdefRecord [] records = new NdefRecord[ndef_records.length] ;
		for ( int i = 0; i < records.length; i++ )
			records[i] = NdefRecord.createRecord(ndef_records[i]) ;
		return records ;
	}

	@Override
	public int hashCode() {
		return mMessage.hashCode() ;
	}

	@Override
	public byte[] toByteArray() {
		return mMessage.toByteArray() ;
	}

	@Override
	public String toString() {
		return mMessage.toString() ;
	}
	
	/**
	 * Returns an instance of android.nfc.NdefMessage representing the 
	 * same data.
	 * @return
	 */
	public Object getAndroidNfcNdefMessage() {
		return mMessage ;
	}

}
