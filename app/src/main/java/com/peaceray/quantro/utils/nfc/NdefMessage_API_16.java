package com.peaceray.quantro.utils.nfc;

import android.annotation.TargetApi;
import android.nfc.FormatException;

@TargetApi(16)
class NdefMessage_API_16 extends NdefMessage_API_9 {

	/**
	 * Construct an NDEF Message by parsing raw bytes.
	 * @param data
	 * @throws FormatException 
	 */
	public NdefMessage_API_16(byte[] data) throws FormatException {
		mMessage = new android.nfc.NdefMessage(data) ;
	}
	
	
	/**
	 * Construct an NDEF Message from one or more NDEF Records.
	 * @param record
	 * @param records
	 */
	public NdefMessage_API_16(NdefRecord record, NdefRecord... records) {
		android.nfc.NdefRecord ndef_record =
				(android.nfc.NdefRecord) record.getAndroidNfcNdefRecord() ;
		android.nfc.NdefRecord [] ndef_records = new android.nfc.NdefRecord[records.length] ;
		for ( int i = 0; i < records.length; i++ )
			ndef_records[i] = (android.nfc.NdefRecord) records[i].getAndroidNfcNdefRecord() ;
		mMessage = new android.nfc.NdefMessage(ndef_record, ndef_records) ;
	}
	
	
	/**
	 * Construct an NDEF Message from one or more NDEF Records.
	 * @param records
	 */
	public NdefMessage_API_16(NdefRecord[] records) {
		android.nfc.NdefRecord [] ndef_records = new android.nfc.NdefRecord[records.length] ;
		for ( int i = 0; i < records.length; i++ )
			ndef_records[i] = (android.nfc.NdefRecord) records[i].getAndroidNfcNdefRecord() ;
		mMessage = new android.nfc.NdefMessage(ndef_records) ;
	}
	
	/**
	 * Construct an NDEF Message from the android.nfc.NdefMessage instance.
	 * @param message
	 */
	NdefMessage_API_16( Object message ) {
		mMessage = (android.nfc.NdefMessage) message ;
	}
	
	
	@Override
	public int getByteArrayLength() {
		return mMessage.getByteArrayLength() ;
	}
	
}
