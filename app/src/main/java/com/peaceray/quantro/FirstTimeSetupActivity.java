package com.peaceray.quantro;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.utils.nfc.NfcAdapter;

public class FirstTimeSetupActivity extends QuantroActivity {
	
	private static final String TAG = "FirstTimeSetupActivity" ;
	
	private static final String PREFERENCE_COMPLETE = "com.peaceray.quantro.FirstTimeSetupActivity.PREFERENCE_COMPLETE" ;
	private static final String PREFERENCE_COMPLETED_ON_VERSION = "com.peaceray.quantro.FirstTimeSetupActivity.PREFERENCE_COMPLETED_ON_VERSION" ;
	
	DialogManager mDialogManager ;
	
	
	public static boolean isComplete( Context context ) {
		return QuantroPreferences.getPrivateSettingBoolean(context, PREFERENCE_COMPLETE, false) ;
	}
	
	public static int completedOnVersion( Context context ) {
		if ( !isComplete(context) )
			return -1 ;
		return QuantroPreferences.getPrivateSettingInt(
				context,
				PREFERENCE_COMPLETED_ON_VERSION,
				3 ) ;
	}

	@Override
	synchronized public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState) ;
		setupQuantroActivity( QUANTRO_ACTIVITY_UNKNOWN, QUANTRO_ACTIVITY_CONTENT_EMPTY ) ;
		
		Log.d(TAG, "onCreate") ;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		this.setContentView(R.layout.empty_layout) ;
		
		mDialogManager = new DialogManager(this) ;

		mDialogManager.hideDialogs() ;
		
		
		// 0.8.3: COMPLETE YOURSELF
		complete() ;
		
		/*
		// what do?
		if ( ((QuantroApplication)getApplication()).mSupportsFeatureTelephony )
			mDialogManager.showDialog(DIALOG_INTERNET_SMS_ID) ;
		else {
			QuantroPreferences.setInternetMultiplayer(this, false) ;
			complete() ;
		}
		*/
		
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume() ;
		Log.d(TAG, "onResume") ;
        
		// maybe we're already done?
		if ( QuantroPreferences.getPrivateSettingBoolean(this, PREFERENCE_COMPLETE, false) )
			finish() ;
		else
	        mDialogManager.revealDialogs() ;
			// Reveal our dialogs, in case they were previously hidden.
	}
	
	protected void onPause() {
		super.onPause() ;
		Log.d(TAG, "onPause") ;
		
		mDialogManager.hideDialogs() ;
	}
	
	
	
	protected void incomplete() {
		QuantroPreferences.setPrivateSettingBoolean(this, PREFERENCE_COMPLETE, false) ;
		this.setResult(RESULT_CANCELED) ;
    	finish() ;
	}
	
	protected void complete() {
		QuantroPreferences.setPrivateSettingBoolean(this, PREFERENCE_COMPLETE, true) ;
		QuantroPreferences.setPrivateSettingInt(this, PREFERENCE_COMPLETED_ON_VERSION, AppVersion.code(this)) ;
		this.setResult(RESULT_OK) ;
    	finish() ;
	}
	
}
