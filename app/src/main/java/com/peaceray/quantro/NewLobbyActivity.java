package com.peaceray.quantro;

import java.util.regex.Pattern;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.dialog.TextFormatting;

import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.WifiMonitor;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.button.strip.DialogButtonStrip;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class NewLobbyActivity extends QuantroActivity
		implements WifiMonitor.Listener, OnItemSelectedListener  {

	public static final String TAG = "NewLobbyActivity" ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// INTENT STRINGS
	public static final String INTENT_EXTRA_NEW_WIFI_LOBBY_DETAILS			 = "com.peaceray.quantro.NewLobbyActivity.INTENT_EXTRA_NEW_WIFI_LOBBY_DETAILS" ;
	
	DialogButtonStrip mDialogButtonStrip ;
	
	// Text fields.  For now, we don't bother storing or altering
	// IDs for those fields which are fixed; those strings are set
	// in the layout file.
	int mTextViewLobbyNameID ;
	TextView mTextViewLobbyName ;
	int mTextViewLobbyNameCharactersRemainingID ;
	TextView mTextViewLobbyNameCharactersRemaining ;
	int mTextViewLobbyAddressID ;
	TextView mTextViewLobbyAddress ;
	int mSpinnerLobbySizeID ;
	Spinner mSpinnerLobbySize ;
	ArrayAdapter<CharSequence> mSpinnerLobbySizeAdapter ;
	int mSpinnerLobbySizeSelected ;
	
	// CONSTANTS.  These are constants we load from resources.
	private String CHARACTERS_REMAINING_PREFIX ;
	private String CHARACTERS_REMAINING_SUFFIX ;
	private int LOBBY_MAXIMUM_NAME_LENGTH ;
	
	private Pattern mPatternASCIIOnly ;
	
	
	// Handler!  We use this for our WifiMonitor.
	private static final int WIFI_MONITOR_REFRESH_FREQUENCY = 1000 ;		// every second!
	private Handler mHandler ;
	private WifiMonitor mWifiMonitor ;
	
	private Resources res ;
	private boolean mResumed ;
	
	private QuantroSoundPool mQuantroSoundPool ;
	private boolean mSoundControls ;
	
	@Override
	synchronized public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupQuantroActivity( QUANTRO_ACTIVITY_UNKNOWN, QUANTRO_ACTIVITY_CONTENT_PERMANENT_DIALOG ) ;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        
		// Set content view.  This takes 3 steps: load our generic "dialog" layout,
		// place our custom content within it, and set the title according to getTitle().
		setContentView(R.layout.dialog) ;
		ViewGroup content = (ViewGroup)findViewById(R.id.dialog_content) ;
		getLayoutInflater().inflate(R.layout.new_lobby_layout, content) ;
		TextView titleView = (TextView)content.findViewById(R.id.dialog_content_title) ;
		if ( titleView != null )
			titleView.setText(getTitle()) ;
		
		res = getResources() ;
		
		// Contact field IDs
		// mTextViewContactLabelID = R.id.new_challenge_layout_text_contact_label ;
		mTextViewLobbyNameID = R.id.new_lobby_layout_name ;
		mTextViewLobbyName = (TextView)findViewById(mTextViewLobbyNameID) ;
		mTextViewLobbyNameCharactersRemainingID = R.id.new_lobby_layout_name_characters_remaining ;
		mTextViewLobbyNameCharactersRemaining = (TextView)findViewById(mTextViewLobbyNameCharactersRemainingID) ;
		mTextViewLobbyAddressID = R.id.new_lobby_layout_ip_address ;
		mTextViewLobbyAddress = (TextView)findViewById(mTextViewLobbyAddressID) ;
		mSpinnerLobbySizeID = R.id.new_lobby_size ;
		mSpinnerLobbySize = (Spinner)findViewById(mSpinnerLobbySizeID) ;
		
		CHARACTERS_REMAINING_PREFIX = res.getString(R.string.characters_remaining_prefix) ;
		CHARACTERS_REMAINING_SUFFIX = res.getString(R.string.characters_remaining_suffix) ;
		LOBBY_MAXIMUM_NAME_LENGTH = res.getInteger(R.integer.lobby_maximum_name_length) ;
		
		mQuantroSoundPool = ((QuantroApplication)getApplicationContext()).getSoundPool(this) ;
		mSoundControls = false ;
		    
		// Configure buttons
		mDialogButtonStrip = (DialogButtonStrip)content.findViewById(R.id.dialog_content_button_strip) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_POSITIVE,
				res.getString( R.string.new_lobby_button_create ),
				res.getColor(R.color.lobby_new),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ( mSoundControls )
							mQuantroSoundPool.menuButtonClick() ;
						createLobbyDescriptionAndFinish() ;
					}
				}) ;
		mDialogButtonStrip.configureButton(Dialog.BUTTON_POSITIVE, false, true) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_NEGATIVE,
				res.getString( R.string.new_lobby_button_cancel ),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ( mSoundControls )
							mQuantroSoundPool.menuButtonBack() ;
						finish() ;
					}
				}) ;
		mDialogButtonStrip.configureButton(Dialog.BUTTON_NEGATIVE, true, true) ;
		
		mPatternASCIIOnly = Pattern.compile("\\A\\p{ASCII}*\\Z") ;
		
		
		// Create our handler and WifiMonitor.
		mHandler = new Handler() ;
		mWifiMonitor = new WifiMonitor(this, this) ;
		
		mResumed = false ;
		
        
        // Set up the message editor window.  We do two things: set an
        // input-filter to limit message length, and (optionally)
        // a "characters remaining" counter that changes over time.
		mTextViewLobbyName.setFilters( new InputFilter[] {
        		new InputFilter() {
        			public CharSequence filter( CharSequence source, int start, int end, Spanned dest, int dstart, int dend ) {
        				// We don't do any actual string replacement here; that causes too long a 
        				// delay.  Instead, calculate the total length of the result, and make sure
        				// the text to be inserted includes only ascii characters.
        				boolean spaceRemaining = dest.length() - (dend - dstart) + (end - start) <= LOBBY_MAXIMUM_NAME_LENGTH ;
        				boolean resultAscii = mPatternASCIIOnly.matcher( source.subSequence(start,end) ).matches() ;
        				//boolean resultAscii = source  .matches("\\p{ASCII}*") ;
        				//boolean spaceRemaining = result.length() <= CHALLENGE_MAXIMUM_MESSAGE_LENGTH ;
        				// TODO: Toast warning if too long or not ascii
        				if ( !resultAscii ) {
        					/*
        					handler.post( new Runnable() {
        						public void run() {
        							Toast.makeText(NewChallengeActivity.this, R.string.new_challenge_message_ascii_characters_toast_warning, Toast.LENGTH_SHORT ) ;
        						}
        					});
        					*/
        				}
        				else if ( !spaceRemaining ) {
        					/*
        					handler.post( new Runnable() {
        						public void run() {
        							Toast.makeText(NewChallengeActivity.this, R.string.new_challenge_message_ascii_characters_toast_warning, Toast.LENGTH_SHORT ) ;
        						}
        					});
        					*/
        				}
        				
        				if ( resultAscii && spaceRemaining ) {
        					// chars remaining is >= 0; this is valid message content
        					return null ;
        				}
        				else 
        					return dest.subSequence(dstart, dend) ;
        			}
        		}
        }) ;
		mTextViewLobbyName.addTextChangedListener( new TextWatcher() {
        	public void beforeTextChanged( CharSequence s, int start, int count, int after ) { }
        	public void onTextChanged( CharSequence s, int start, int before, int count ) { }
        	public void afterTextChanged( Editable s ) {
        		int len = s.length();
        		if ( len == 0 ) {
        			mTextViewLobbyNameCharactersRemaining.setText(CHARACTERS_REMAINING_PREFIX + LOBBY_MAXIMUM_NAME_LENGTH + CHARACTERS_REMAINING_SUFFIX) ;
        			updateCreateButtonState(false) ;
        		}
        		else {
        			mTextViewLobbyNameCharactersRemaining.setText(CHARACTERS_REMAINING_PREFIX + (LOBBY_MAXIMUM_NAME_LENGTH - len) + CHARACTERS_REMAINING_SUFFIX) ;
        			updateCreateButtonState(false) ;
        		}
        	}
        }) ;
		
		// Set the lobby name to the default.
		mTextViewLobbyName.setText(QuantroPreferences.getDefaultWiFiLobbyName(this)) ;
		
		// spinner.
		mSpinnerLobbySizeAdapter = ArrayAdapter.createFromResource(this,
				R.array.new_lobby_size_array, android.R.layout.simple_spinner_item) ;
		mSpinnerLobbySizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		// apply the adapter
		mSpinnerLobbySizeSelected = QuantroPreferences.getDefaultWiFiLobbySize(this) ;
		int position = sizeSpinnerValueToPosition( mSpinnerLobbySizeSelected ) ;
		mSpinnerLobbySize.setAdapter(mSpinnerLobbySizeAdapter) ;
		mSpinnerLobbySize.setSelection(position) ;
		mSpinnerLobbySize.setOnItemSelectedListener(this) ;
		
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}


	
	@Override
	synchronized public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume") ;
		
		// Force a button update
		updateCreateButtonState(true) ;
		
		// Post the monitor for a future update
		mResumed = true ;
		mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_REFRESH_FREQUENCY) ;
		
		mSoundControls = QuantroPreferences.getSoundControls(this) ;
	}
	
	@Override
	synchronized public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause") ;
		
		// Remove the monitor.
		mResumed = false ;
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
	
	// These are used to minimize the overhead of calling this method.
	// If called with force == false, we only update the portions that
	// seem to have changed.
	int lastIP = 0 ;
	synchronized public void updateCreateButtonState(boolean force) {
		int ip = mWifiMonitor.getWifiIpAddress() ;
		boolean onWifi = ip != 0 ;
		boolean hasName = mTextViewLobbyName.getText().length() > 0 ;
		
		// Set the button enabled status
		boolean shouldEnable = onWifi && hasName ;
		mDialogButtonStrip.configureButton(DialogButtonStrip.BUTTON_POSITIVE, shouldEnable, true) ;
		
		// update the relevant parts of our layout.  For now
		// that's just the address field.
		int display = TextFormatting.DISPLAY_MENU ;
		int role = TextFormatting.ROLE_HOST ;
		int type = TextFormatting.TYPE_NEW_LOBBY_ADDRESS ;
		
		if ( force || ip != lastIP )
			mTextViewLobbyAddress.setText(
					TextFormatting.format(
							this, res, display, type, role,
							ip == 0 ? null : WifiMonitor.ipAddressToString(ip))) ;
		lastIP = ip ;
	}
	
	
	

	@Override
	synchronized public void wml_hasWifiIpAddress(boolean hasIP, int ip) {
		// oh look, an update.  Update the create button state and re-post
		// ourself (if mResumed).
		updateCreateButtonState(false) ;
		if ( mResumed )
			mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_REFRESH_FREQUENCY) ;
	}
	
	
	synchronized public void createLobbyDescriptionAndFinish() {
		// We create a LobbyDescription object according to our current settings.
		CharSequence lobbyName = mTextViewLobbyName.getText() ;
		WiFiLobbyDetails details = WiFiLobbyDetails.newIntentionInstance(
				lobbyName.toString(),
				QuantroPreferences.getMultiplayerName(this),
				mSpinnerLobbySizeSelected) ;
		
		QuantroPreferences.setDefaultWiFiLobbyName(this, lobbyName.toString()) ;
		QuantroPreferences.setDefaultWiFiLobbySize(this, mSpinnerLobbySizeSelected) ;
		
		// Set ld as our result.
		Intent i = new Intent() ;
    	i.putExtra(INTENT_EXTRA_NEW_WIFI_LOBBY_DETAILS, details) ;
    	this.setResult(RESULT_OK, i) ;
    	
    	finish() ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SPINNER CALLBACKS.

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		
		mSpinnerLobbySizeSelected = sizeSpinnerPositionToValue(pos) ;
		Log.d(TAG, "onItemSelected: set size to " + mSpinnerLobbySizeSelected) ;
	}


	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// nothing to do.
		Log.d(TAG, "onNothingSelected: size is still " + mSpinnerLobbySizeSelected ) ;
	}
	
	private int sizeSpinnerPositionToValue( int position ) {
		if ( position == 0 )
			return 2 ;
		if ( position == 1 )
			return 4 ;
		if ( position == 2 )
			return 6 ;
		if ( position == 3 )
			return 8 ;
		return 4 ;
	}
	
	private int sizeSpinnerValueToPosition( int value ) {
		if ( value == 2 )
			return 0 ;
		if ( value == 4 )
			return 1 ;
		if ( value == 6 )
			return 2 ;
		if ( value == 8 )
			return 3 ;
		return 1 ;
	}
	
}
