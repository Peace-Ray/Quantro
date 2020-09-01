package com.peaceray.quantro.utils.nfc;

import com.peaceray.quantro.utils.VersionCapabilities;

import android.app.Activity;
import android.content.Context;

public abstract class NfcAdapter {

	/**
	 * A callback to be invoked when another NFC device capable of
	 * NDEF push (Android Beam) is within range.
	 *
	 * Implement this interface and pass it to setNdefPushMessageCallback()
	 * in order to create an NdefMessage at the moment that another device
	 * is within range for NFC. Using this callback allows you to create a
	 * message with data that might vary based on the content currently
	 * visible to the user. Alternatively, you can call setNdefPushMessage()
	 * if the NdefMessage always contains the same data.
	 * 
	 * @author Jake
	 *
	 */
	public interface CreateNdefMessageCallback {
		/**
		 * Called to provide a NdefMesage to push.
		 * @param event
		 * @return
		 */
		public abstract NdefMessage createNdefMessage( NfcEvent event ) ;
	}
	
	/**
	 * A callback to be invoked when the system successfully
	 * delivers your NdefMessage to another device.
	 */
	public interface OnNdefPushCompleteCallback {
		/**
		 * Called on successful NDEF push.
		 * @param event
		 */
		public abstract void onNdefPushComplete(NfcEvent event) ;
	}
	
	
	
	public static NfcAdapter getDefaultAdapter( Context context ) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return NfcAdapter_API_14.getDefaultAdapter(context) ;
		return null ;
	}
	
	static NfcAdapter wrapAndroidNfcNfcAdapter( Object obj ) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return NfcAdapter_API_14.wrapAndroidNfcNfcAdapter(obj) ;
		return null ;
	}
	
	/**
	 * Return true if this NFC Adapter has any features enabled.
	 * @return
	 */
	public abstract boolean isEnabled() ;
	
	/**
	 * Set a static NdefMessage to send using Android Beam (TM).
	 * @param message
	 * @param activity
	 * @param activities
	 * @return Whether this instance supports this method.
	 */
	public abstract boolean setNdefPushMessage(NdefMessage message, Activity activity, Activity... activities) ;
	
	/**
	 * Set a callback that dynamically generates NDEF messages to send using Android Beam (TM).
	 * @param callback
	 * @param activity
	 * @param activities
	 * @return Whether this instance supports this method.
	 */
	public abstract boolean setNdefPushMessageCallback(NfcAdapter.CreateNdefMessageCallback callback, Activity activity, Activity... activities) ;
	
	/**
	 * Set a callback on successful Android Beam (TM).
	 * @param callback
	 * @param activity
	 * @param activities
	 * @return Whether this instance supports this method.
	 */
	public abstract boolean setOnNdefPushCompleteCallback(NfcAdapter.OnNdefPushCompleteCallback callback, Activity activity, Activity... activities) ;
	
	
	
}
