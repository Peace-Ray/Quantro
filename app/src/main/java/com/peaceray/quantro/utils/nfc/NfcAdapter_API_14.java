package com.peaceray.quantro.utils.nfc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;

@TargetApi(14)
class NfcAdapter_API_14 extends NfcAdapter {
	
	protected android.nfc.NfcAdapter mAdapter ;
	

	public static NfcAdapter getDefaultAdapter( Context context ) {
		NfcAdapter_API_14 adapter = new NfcAdapter_API_14() ;
		adapter.mAdapter = android.nfc.NfcAdapter.getDefaultAdapter(context) ;
		return adapter.mAdapter == null ? null : adapter ;
	}
	
	static NfcAdapter wrapAndroidNfcNfcAdapter( Object obj ) {
		NfcAdapter_API_14 adapter = new NfcAdapter_API_14() ;
		adapter.mAdapter = (android.nfc.NfcAdapter) obj ;
		return adapter.mAdapter == null ? null : adapter ;
	}
	

	/**
	 * Return true if this NFC Adapter has any features enabled.
	 * @return
	 */
	public boolean isEnabled() {
		return mAdapter.isEnabled() ;
	}

	/**
	 * Set a static NdefMessage to send using Android Beam (TM).
	 * @param message
	 * @param activity
	 * @param activities
	 */
	public boolean setNdefPushMessage(NdefMessage message, Activity activity, Activity... activities) {
		if ( message == null )
			mAdapter.setNdefPushMessage(null, activity, activities) ;
		else
			mAdapter.setNdefPushMessage(
					(android.nfc.NdefMessage) message.getAndroidNfcNdefMessage(),
					activity, activities) ;
		
		// obviously, we support this call.
		return true ;
	}
	
	/**
	 * Set a callback that dynamically generates NDEF messages to send using Android Beam (TM).
	 * @param callback
	 * @param activity
	 * @param activities
	 */
	public boolean setNdefPushMessageCallback(NfcAdapter.CreateNdefMessageCallback callback, Activity activity, Activity... activities) {
		if ( callback == null )
			mAdapter.setNdefPushMessageCallback(null, activity, activities) ;
		else
			mAdapter.setNdefPushMessageCallback(
				new CreateNdefMessageCallbackWrapper(callback),
				activity, activities) ;
		
		// obviously, we support this call.
		return true ;
	}
	
	/**
	 * Set a callback on successful Android Beam (TM).
	 * @param callback
	 * @param activity
	 * @param activities
	 */
	public boolean setOnNdefPushCompleteCallback(NfcAdapter.OnNdefPushCompleteCallback callback, Activity activity, Activity... activities) {
		if ( callback == null )
			mAdapter.setOnNdefPushCompleteCallback(null, activity, activities) ;
		else
			mAdapter.setOnNdefPushCompleteCallback(
					new OnNdefPushCompleteCallbackWrapper( callback ),
					activity, activities) ;
		
		// obviously, we support this call.
		return true ;
	}
	
	
	
	/**
	 * A wrapper class that sits between instances of CreateNdefMessageCallback,
	 * and an underlying android.nfc.NfcAdapter and its callback.
	 * 
	 * Converts all data to version-safe formatting and back.
	 * 
	 * @author Jake
	 *
	 */
	protected static class CreateNdefMessageCallbackWrapper implements android.nfc.NfcAdapter.CreateNdefMessageCallback {

		NfcAdapter.CreateNdefMessageCallback mCallback ;
		
		protected CreateNdefMessageCallbackWrapper( NfcAdapter.CreateNdefMessageCallback callback ) {
			if ( callback == null )
				throw new NullPointerException("Null argument given") ;
			mCallback = callback ;
		}
		
		@Override
		public android.nfc.NdefMessage createNdefMessage(android.nfc.NfcEvent nfc_event) {
			// wrap the event.
			NfcEvent event = NfcEvent.wrapAndroidNfcNfcEvent(nfc_event) ;
			
			// pass it to our callback
			NdefMessage message = mCallback.createNdefMessage(event) ;
			
			// unwrap and return
			if ( message == null )
				return null ;
			return (android.nfc.NdefMessage)message.getAndroidNfcNdefMessage() ;
		}
		
	}
	
	
	protected static class OnNdefPushCompleteCallbackWrapper implements android.nfc.NfcAdapter.OnNdefPushCompleteCallback {

		NfcAdapter.OnNdefPushCompleteCallback mCallback ;
		
		protected OnNdefPushCompleteCallbackWrapper( NfcAdapter.OnNdefPushCompleteCallback callback ) {
			if ( callback == null )
				throw new NullPointerException("Null argument given") ;
			mCallback = callback ;
		}
		
		@Override
		public void onNdefPushComplete(android.nfc.NfcEvent nfc_event) {
			// wrap the event.
			NfcEvent event = NfcEvent.wrapAndroidNfcNfcEvent(nfc_event) ;
			
			// pass it to our callback
			mCallback.onNdefPushComplete(event) ;
		}
		
	}
	
}
