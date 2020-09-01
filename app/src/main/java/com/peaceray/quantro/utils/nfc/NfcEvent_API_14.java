package com.peaceray.quantro.utils.nfc;

import android.annotation.TargetApi;

@TargetApi(14)
class NfcEvent_API_14 extends NfcEvent {

	static NfcEvent wrapAndroidNfcNfcEvent( Object event ) {
		return new NfcEvent(
				NfcAdapter.wrapAndroidNfcNfcAdapter(
						((android.nfc.NfcEvent)event).nfcAdapter ) ) ;
	}
	
}
