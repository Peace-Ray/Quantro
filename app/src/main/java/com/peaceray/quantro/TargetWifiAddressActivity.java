package com.peaceray.quantro;

import com.peaceray.quantro.R;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.WifiMonitor;
import com.peaceray.quantro.utils.WifiMonitor.Listener;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.button.strip.DialogButtonStrip;
import com.peaceray.quantro.view.generic.IPAddressKeyListener;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;


public class TargetWifiAddressActivity extends QuantroActivity implements Listener {

	// Results
	public static final String INTENT_RESULT_EXTRA_ACTION = "com.peaceray.quantro.TargetWifiAddressActivity.INTENT_RESULT_EXTRA_ACTION" ;
	
	public static final int INTENT_RESULT_EXTRA_ACTION_CANCEL = 0 ;
	public static final int INTENT_RESULT_EXTRA_ACTION_FIND = 1 ;
	// Settings for level, clears, garbage.
	public static final String INTENT_RESULT_EXTRA_IP = "com.peaceray.quantro.TargetWifiAddressActivity.INTENT_RESULT_EXTRA_IP" ;
	
	
	
	protected boolean mOnWifi ;
	protected boolean mIpIsEntered ;
	
	// buttons and views
	protected DialogButtonStrip mDialogButtonStrip ;
	
	protected EditText mIPText ;
	protected TextView mIPDescription ;
	
	protected Handler mHandler ;	// for WifiMonitor runnables.
	protected WifiMonitor mWifiMonitor ;
	
	private QuantroSoundPool mQuantroSoundPool ;
	private boolean mSoundControls ;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupQuantroActivity( QUANTRO_ACTIVITY_UNKNOWN, QUANTRO_ACTIVITY_CONTENT_PERMANENT_DIALOG ) ;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        
		// Set content view.  This takes 3 steps: load our generic "dialog" layout,
		// place our custom content within it, and set the title according to getTitle().
		setContentView(R.layout.dialog) ;
		ViewGroup content = (ViewGroup)findViewById(R.id.dialog_content) ;
		getLayoutInflater().inflate(R.layout.target_wifi_address_layout, content) ;
		TextView titleView = (TextView)content.findViewById(R.id.dialog_content_title) ;
		if ( titleView != null )
			titleView.setText(getTitle()) ;
		
		mIpIsEntered = false ;
		mOnWifi = false ;
		
		Resources res = getResources() ;
		
		mQuantroSoundPool = ((QuantroApplication)getApplicationContext()).getSoundPool(this) ;
		mSoundControls = false ;
		
		// Configure buttons
		mDialogButtonStrip = (DialogButtonStrip)content.findViewById(R.id.dialog_content_button_strip) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_POSITIVE,
				res.getString( R.string.target_wifi_address_button_find ),
				res.getColor(R.color.lobby_join),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ( mSoundControls )
							mQuantroSoundPool.menuButtonClick() ;
						String ipString = mIPText.getText().toString() ;
						if ( WifiMonitor.isIPAddress(ipString) ) {
							Intent i = new Intent() ;
					    	i.putExtra(INTENT_RESULT_EXTRA_IP, ipString) ;
					    	i.putExtra(INTENT_RESULT_EXTRA_ACTION, INTENT_RESULT_EXTRA_ACTION_FIND) ;
					    	TargetWifiAddressActivity.this.setResult(RESULT_OK, i) ;
						}
						finish() ;
					}
				}) ;
		mDialogButtonStrip.configureButton(Dialog.BUTTON_POSITIVE, false, true) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_NEGATIVE,
				res.getString( R.string.target_wifi_address_button_cancel ),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ( mSoundControls )
							mQuantroSoundPool.menuButtonBack() ;
						finish() ;
					}
				}) ;
		mDialogButtonStrip.configureButton(Dialog.BUTTON_NEGATIVE, true, true) ;
		
		// Find our description; this might change over time
		mIPDescription = (TextView)findViewById(R.id.target_wifi_address_description_ip) ;
		
		// Set our KeyListener.
		mIPText = (EditText)findViewById(R.id.target_wifi_address_text_ip) ;
		mIPText.setKeyListener(IPAddressKeyListener.getInstance()) ;
		
		// Set up a listener, so "Find" is only enabled if a full IP address (on local WiFi)
		// is entered, AND if we are on WiFi.
		mIPText.addTextChangedListener( new TextWatcher() {
        	public void beforeTextChanged( CharSequence s, int start, int count, int after ) { }
        	public void onTextChanged( CharSequence s, int start, int before, int count ) { }
        	public void afterTextChanged( Editable s ) {
        		boolean ip = WifiMonitor.isIPAddress(s.toString()) ;
        		if ( mIpIsEntered != ip ) {
        			mIpIsEntered = ip ;
        			refreshViews() ;
        		}
        	}
        }) ;
		
		// Start a Handler.
		mHandler = new Handler() ;
		
		// Make a WifiMonitor, and finally refresh our description.
		mWifiMonitor = new WifiMonitor(this, this) ;
		
		// Set starting IP.
		int ip = mWifiMonitor.getWifiIpAddress() ;
		mOnWifi = mWifiMonitor.getWifiIpAddress() != 0 ;
		if ( ip > 0 ) {
			String ipString = WifiMonitor.ipAddressToString(ip) ;
			// eliminate everything after the last period.
			ipString = ipString.substring(0, ipString.lastIndexOf(".")+1) ;
			mIPText.setText(ipString) ;
			mIPText.setSelection(ipString.length()) ;
		}
		
		// Refresh our views!
		refreshViews() ;
		
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}

	
	@Override
	synchronized public void onResume() {
		super.onResume() ;
		
		mHandler.postDelayed(mWifiMonitor, 500) ;
		
		mSoundControls = QuantroPreferences.getSoundControls(this) ;
	}
	
	@Override
	synchronized public void onPause() {
		super.onPause();
		
		mHandler.removeCallbacks(mWifiMonitor) ;
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	if ( mSoundControls )
        		mQuantroSoundPool.menuButtonBack() ;
        }

        return super.onKeyDown(keyCode, event);
    }
	
	@Override
	synchronized public void wml_hasWifiIpAddress(boolean hasIP, int ip) {
		if ( hasIP != mOnWifi ) {
			mOnWifi = hasIP ;
			refreshViews() ;
			
			
		}
	}
	
	synchronized public void refreshViews() {
		mDialogButtonStrip.configureButton(DialogButtonStrip.BUTTON_POSITIVE, mOnWifi && mIpIsEntered, true) ;
		mIPDescription.setText( mOnWifi ? getResources().getString( R.string.target_wifi_address_address_description )
										: getResources().getString( R.string.target_wifi_address_address_description_no_wifi ) ) ;
	}


	
}
