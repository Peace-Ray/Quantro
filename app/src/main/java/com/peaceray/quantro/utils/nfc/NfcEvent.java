package com.peaceray.quantro.utils.nfc;

import com.peaceray.quantro.utils.VersionCapabilities;

/**
 * A Wrapper class for NfcAdapter callback methods.
 * 
 * @author Jake
 *
 */
public class NfcEvent {
	
	protected NfcEvent() {
		throw new IllegalStateException("Can't use empty constructor!") ;
	}
	
	protected NfcEvent( NfcAdapter adapter ) {
		nfcAdapter = adapter ;
	}
	
	static NfcEvent wrapAndroidNfcNfcEvent( Object event ) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_14 ) )
			return NfcEvent_API_14.wrapAndroidNfcNfcEvent(event) ;
		return null ;
	}

	public final NfcAdapter nfcAdapter ;
	
}
