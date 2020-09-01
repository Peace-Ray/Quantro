package com.peaceray.quantro;

import java.util.regex.Pattern;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.dialog.GlobalDialog;
import com.peaceray.quantro.keys.KeyStorage;
import com.peaceray.quantro.keys.QuantroXLKey;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.MutableInternetLobby;
import com.peaceray.quantro.lobby.WebConsts;
import com.peaceray.quantro.lobby.WorkOrder;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.button.strip.DialogButtonStrip;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.dialog.ProgressDialog;

public class NewInternetLobbyActivity extends QuantroActivity implements OnItemSelectedListener {
	
	
	private static final String TAG = "NewInternetLobbyActivity" ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// INTENT STRINGS
	public static final String INTENT_EXTRA_LOBBY			 = "com.peaceray.quantro.NewInternetLobbyActivity.INTENT_EXTRA_NEW_LOBBY" ;
	public static final String INTENT_FORCE_REFRESH			 = "com.peaceray.quantro.NewInternetLobbyActivity.INTENT_FORCE_REFRESH" ;
	
	////////////////////////////////////////////////////////////////////////////
	// DIALOGS
	
	private static final int DIALOG_ID_CREATING_LOBBY 					= 0 ;
	private static final int DIALOG_ID_COMMUNICATION_FAILURE			= 1 ;
	private static final int DIALOG_ID_LOBBY_EXISTS						= 2 ;
	
	
	
	int DIALOG_INSET ;
	

	DialogButtonStrip mDialogButtonStrip ;
	
	// Text fields.  For now, we don't bother storing or altering
	// IDs for those fields which are fixed; those strings are set
	// in the layout file.
	int mTextViewLobbyNameID ;
	TextView mTextViewLobbyName ;
	int mTextViewLobbyNameCharactersRemainingID ;
	TextView mTextViewLobbyNameCharactersRemaining ;
	
	int mSpinnerLobbySizeID ;
	Spinner mSpinnerLobbySize ;
	ArrayAdapter<CharSequence> mSpinnerLobbySizeAdapter ;
	int mSpinnerLobbySizeSelected ;
	
	int mSpinnerLobbyTypeID ;
	Spinner mSpinnerLobbyType ;
	ArrayAdapter<CharSequence> mSpinnerLobbyTypeAdapter ;
	int mSpinnerLobbyTypeSelected ;
	int mSpinnerLobbyTypeDescriptionID ;
	TextView mSpinnerLobbyTypeDescription ;
	String [] mSpinnerLobbyTypeDescriptionStrings ;
	
	
	private static final int LOBBY_TYPE_PUBLIC = 0 ;
	private static final int LOBBY_TYPE_PRIVATE = 1 ;
	private static final int LOBBY_TYPE_ITINERANT = 2 ;
	private static final int NUM_LOBBY_TYPES = 3 ;
	// these values match the position of these settings in the spinner.
	int [] mLobbyTypeColor ;
	
	
	// CONSTANTS.  These are constants we load from resources.
	private String CHARACTERS_REMAINING_PREFIX ;
	private String CHARACTERS_REMAINING_SUFFIX ;
	private int LOBBY_MAXIMUM_NAME_LENGTH ;
	
	private Pattern mPatternASCIIOnly ;
	
	private DialogManager mDialogManager ;
	private int mDialogFailureError ;
	private int mDialogFailureReason ;
	
	private Resources res ;
	private boolean mResumed ;
	
	private QuantroSoundPool mQuantroSoundPool ;
	private boolean mSoundControls ;
	
	private InternetLobby mLobbyToRehost = null ;
	
	
	@Override
	synchronized public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupQuantroActivity( QUANTRO_ACTIVITY_UNKNOWN, QUANTRO_ACTIVITY_CONTENT_PERMANENT_DIALOG ) ;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		res = getResources() ;
		
		mLobbyToRehost = null ;
		Intent intent = getIntent() ;
		if ( intent.hasExtra(INTENT_EXTRA_LOBBY) )
			mLobbyToRehost = (InternetLobby) intent.getSerializableExtra(INTENT_EXTRA_LOBBY) ;
			
        
		// Set content view.  This takes 3 steps: load our generic "dialog" layout,
		// place our custom content within it, and set the title according to getTitle().
		setContentView(R.layout.dialog) ;
		ViewGroup content = (ViewGroup)findViewById(R.id.dialog_content) ;
		getLayoutInflater().inflate(R.layout.new_internet_lobby, content) ;
		TextView titleView = (TextView)content.findViewById(R.id.dialog_content_title) ;
		if ( titleView != null )
			titleView.setText(res.getString(R.string.new_internet_lobby_title_creating)) ;
		
		DIALOG_INSET = getResources().getDimensionPixelSize(R.dimen.margin_inset_dialog) ;
		
		// Contact field IDs
		// mTextViewContactLabelID = R.id.new_challenge_layout_text_contact_label ;
		mTextViewLobbyNameID = R.id.new_internet_lobby_name ;
		mTextViewLobbyName = (TextView)findViewById(mTextViewLobbyNameID) ;
		mTextViewLobbyNameCharactersRemainingID = R.id.new_internet_lobby_name_characters_remaining ;
		mTextViewLobbyNameCharactersRemaining = (TextView)findViewById(mTextViewLobbyNameCharactersRemainingID) ;
		
		mSpinnerLobbySizeID = R.id.new_internet_lobby_size ;
		mSpinnerLobbySize = (Spinner)findViewById(mSpinnerLobbySizeID) ;
		
		mSpinnerLobbyTypeID = R.id.new_internet_lobby_type ;
		mSpinnerLobbyType = (Spinner)findViewById(mSpinnerLobbyTypeID) ;
		
		mSpinnerLobbyTypeDescriptionID = R.id.new_internet_lobby_type_description ;
		mSpinnerLobbyTypeDescription = (TextView)findViewById(mSpinnerLobbyTypeDescriptionID) ;
		
		
		res = getResources() ;
		// lobby type colors
		mLobbyTypeColor = new int[NUM_LOBBY_TYPES] ;
		mLobbyTypeColor[LOBBY_TYPE_PUBLIC] = res.getColor(R.color.internet_lobby_public) ;
		mLobbyTypeColor[LOBBY_TYPE_PRIVATE] = res.getColor(R.color.internet_lobby_invited) ;
		mLobbyTypeColor[LOBBY_TYPE_ITINERANT] = res.getColor(R.color.internet_lobby_itinerant) ;
		// remaining characters
		CHARACTERS_REMAINING_PREFIX = res.getString(R.string.characters_remaining_prefix) ;
		CHARACTERS_REMAINING_SUFFIX = res.getString(R.string.characters_remaining_suffix) ;
		LOBBY_MAXIMUM_NAME_LENGTH = res.getInteger(R.integer.lobby_maximum_name_length) ;
		
		mQuantroSoundPool = ((QuantroApplication)getApplicationContext()).getSoundPool(this) ;
		mSoundControls = false ;
		    
		// Configure buttons
		mDialogButtonStrip = (DialogButtonStrip)content.findViewById(R.id.dialog_content_button_strip) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_POSITIVE,
				res.getString( mLobbyToRehost == null ? R.string.new_internet_lobby_button_create : R.string.new_internet_lobby_button_rehost ),
				res.getColor(R.color.internet_lobby_new),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if ( mSoundControls )
							mQuantroSoundPool.menuButtonClick() ;
						createLobby() ;
					}
				}) ;
		mDialogButtonStrip.configureButton(Dialog.BUTTON_POSITIVE, false, true) ;
		mDialogButtonStrip.setButton(
				Dialog.BUTTON_NEGATIVE,
				res.getString( R.string.new_internet_lobby_button_cancel ),
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
		mTextViewLobbyName.setText(QuantroPreferences.getDefaultInternetLobbyName(this)) ;
		
		// SIZE spinner.
		mSpinnerLobbySizeAdapter = ArrayAdapter.createFromResource(this,
				R.array.new_internet_lobby_size_array, android.R.layout.simple_spinner_item) ;
		mSpinnerLobbySizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		// apply the adapter
		mSpinnerLobbySizeSelected = QuantroPreferences.getDefaultInternetLobbySize(this) ;
		int position = sizeSpinnerValueToPosition( mSpinnerLobbySizeSelected ) ;
		mSpinnerLobbySize.setAdapter(mSpinnerLobbySizeAdapter) ;
		mSpinnerLobbySize.setSelection(position) ;
		mSpinnerLobbySize.setOnItemSelectedListener(this) ;
		
		// TYPE spinner.
		mSpinnerLobbyTypeAdapter = ArrayAdapter.createFromResource(this,
				R.array.new_internet_lobby_type_array, android.R.layout.simple_spinner_item) ;
		mSpinnerLobbyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		// apply the adapter
		mSpinnerLobbyTypeSelected = typePreferenceToLocal( QuantroPreferences.getDefaultInternetLobbyType(this) ) ;
		position = typeSpinnerValueToPosition( mSpinnerLobbyTypeSelected ) ;
		mSpinnerLobbyType.setAdapter(mSpinnerLobbyTypeAdapter) ;
		mSpinnerLobbyType.setSelection(position) ;
		mSpinnerLobbyType.setOnItemSelectedListener(this) ;
		
		// type descriptions
		mSpinnerLobbyTypeDescriptionStrings = new String[NUM_LOBBY_TYPES] ;
		mSpinnerLobbyTypeDescriptionStrings[LOBBY_TYPE_PUBLIC]
		                                    = res.getString(R.string.new_internet_lobby_type_description_public) ;
		mSpinnerLobbyTypeDescriptionStrings[LOBBY_TYPE_PRIVATE]
		                                    = res.getString(R.string.new_internet_lobby_type_description_private) ;
		mSpinnerLobbyTypeDescriptionStrings[LOBBY_TYPE_ITINERANT]
		                                    = res.getString(R.string.new_internet_lobby_type_description_itinerant) ;
		
		setSpinnerTypeDescription() ;
		
		
		mDialogManager = new DialogManager(this) ;
		
		
		// Now that the defaults have been set, it's time to check for a lobby to start with.
		if ( mLobbyToRehost != null ) {
			// name?
			if ( mLobbyToRehost.getLobbyName() != null )
				mTextViewLobbyName.setText(mLobbyToRehost.getLobbyName()) ;
			// size?
			if ( mLobbyToRehost.getMaxPeople() > 0 ) {
				mSpinnerLobbySizeSelected = mLobbyToRehost.getMaxPeople() ;
				mSpinnerLobbySize.setSelection( sizeSpinnerValueToPosition( mSpinnerLobbySizeSelected ) ) ;
			}
			// type?
			mSpinnerLobbyTypeSelected = LOBBY_TYPE_ITINERANT ;
			mSpinnerLobbyType.setSelection( typeSpinnerValueToPosition( mSpinnerLobbyTypeSelected ) ) ;
			mSpinnerLobbyType.setEnabled(false) ;
			setSpinnerTypeDescription() ;
			updateCreateButtonState(true) ;
			
			if ( titleView != null )
				titleView.setText( res.getString(R.string.new_internet_lobby_title_rehosting) ) ;
		}
		
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
		
		// Force a button update
		updateCreateButtonState(true) ;
		
		mResumed = true ;
		
		mSoundControls = QuantroPreferences.getSoundControls(this) ;
		
		// reveal dialogs
		mDialogManager.revealDialogs() ;
	}
	
	@Override
	synchronized public void onPause() {
		super.onPause();
		
		// Remove the monitor.
		mResumed = false ;
		
		// hide dialogs
		mDialogManager.hideDialogs() ;
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
	synchronized public void updateCreateButtonState(boolean force) {
		boolean hasName = mTextViewLobbyName.getText().length() > 0 ;
		
		// Set the button enabled status
		boolean shouldEnable = hasName ;
		mDialogButtonStrip.configureButton(
				DialogButtonStrip.BUTTON_POSITIVE,
				mLobbyTypeColor[mSpinnerLobbyTypeSelected],
				shouldEnable,
				true) ;
	}
	
	
	synchronized public void createLobby() {
		// We create a LobbyDescription object according to our current settings.
		String creatorName = QuantroPreferences.getMultiplayerName(this) ;
		String lobbyName = mTextViewLobbyName.getText().toString() ;
		
		QuantroXLKey key = KeyStorage.getXLKey(this) ;
		String xl = null ;
		if ( key != null )
			xl = key.getKey() ;
		
		if ( mLobbyToRehost == null ) {
			QuantroPreferences.setDefaultInternetLobbyName(this, lobbyName) ;
			QuantroPreferences.setDefaultInternetLobbySize(this, mSpinnerLobbySizeSelected) ;
			QuantroPreferences.setDefaultInternetLobbyType(this, typeLocalToPreference( mSpinnerLobbyTypeSelected ) ) ;
		}
		
		// run the create lobby handler (this will throw up any necessary dialogs).
		Thread t ;
		if ( mLobbyToRehost == null )
			t = new ThreadCreateLobbyAndFinish(
					creatorName, lobbyName, mSpinnerLobbyTypeSelected, mSpinnerLobbySizeSelected, xl ) ;
		else
			t = new ThreadCreateLobbyAndFinish(
					mLobbyToRehost.getLobbyNonce(), creatorName, lobbyName, mSpinnerLobbyTypeSelected, mSpinnerLobbySizeSelected, xl ) ;
		
		t.start() ;
	}
	
	
	
	synchronized public void finishWithLobby( MutableInternetLobby lobby ) {
		// Set lobby as our result.
		Intent i = new Intent() ;
    	i.putExtra(INTENT_EXTRA_LOBBY, lobby) ;
    	this.setResult(RESULT_OK, i) ;
    	
    	finish() ;
	}
	
	
	synchronized public void finishNeedingRefresh() {
		Intent i = new Intent() ;
		i.putExtra(INTENT_FORCE_REFRESH, true) ;
		this.setResult(RESULT_OK, i) ;
		
		finish() ;
	}
	
	
	
	@Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog pd ;
        AlertDialog.Builder builder ;
        
        switch(id) {
        case DIALOG_ID_CREATING_LOBBY:
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage(mLobbyToRehost == null
        			? R.string.new_internet_lobby_dialog_creating
        			: R.string.new_internet_lobby_dialog_rehosting) ;			// Message is set in onPrepare.  Using "" resolves a display bug.
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					finish() ;
        				}
					}) ;
        	pd.setInset(DIALOG_INSET) ;
        	return pd ;
        	
        case DIALOG_ID_COMMUNICATION_FAILURE:
        	// Let's use "negative" for cancel, as a basic convention, since it will be displayed last.
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setTitle(R.string.new_internet_lobby_dialog_failed_title) ;
        	builder.setMessage("") ;			// Message is set in onPrepareDialog.  Using "" resolves a display bug.
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses alert	
        	builder.setNegativeButton(R.string.new_internet_lobby_dialog_failed_button_ok,
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_ID_COMMUNICATION_FAILURE) ;		// dismiss the "failed" message
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_ID_COMMUNICATION_FAILURE) ;
				}
			}) ;
        	builder.setInset(DIALOG_INSET) ;
        	return builder.create() ;
        	
        	
        case DIALOG_ID_LOBBY_EXISTS:
        	// this lobby is already hosted!
        	builder = new AlertDialog.Builder(this) ;
        	builder.setTitle(R.string.new_internet_lobby_dialog_failed_title) ;
        	builder.setMessage(R.string.new_internet_lobby_dialog_failed_message_exists) ;
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses alert	
        	builder.setNegativeButton(R.string.new_internet_lobby_dialog_failed_button_ok,
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_ID_LOBBY_EXISTS) ;		// dismiss the "failed" message
							finishNeedingRefresh() ;
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_ID_LOBBY_EXISTS) ;
					finishNeedingRefresh() ;
				}
			}) ;
        	builder.setInset(DIALOG_INSET) ;
        	return builder.create() ;
        }
        
        return null ;
	}
	
	
	@Override
    synchronized protected void onPrepareDialog(int id, Dialog dialog) {
    	// THIS METHOD IS DEPRECIATED
    	// (and I don't care!  the DialogFragment class will call through
    	// to this method for compatibility)
        
    	String text ;
    	switch ( id ) {
        case DIALOG_ID_CREATING_LOBBY:
        	break ;
        	
        case DIALOG_ID_COMMUNICATION_FAILURE:
        	Resources res = getResources() ;
        	String error_placeholder = res.getString(R.string.placeholder_web_consts_error_code) ;
        	String reason_placeholder = res.getString(R.string.placeholder_web_consts_reason_code) ;
        	
        	int message_id ;
        	switch( mDialogFailureError ) {
        	case WebConsts.ERROR_TIMEOUT:
        		message_id = R.string.new_internet_lobby_dialog_failed_message_timeout ;
        		break ;
        	case WebConsts.ERROR_REFUSED:
        		message_id = R.string.new_internet_lobby_dialog_failed_message_refused ;
        		break ;
        	case WebConsts.ERROR_FAILED:
        	case WebConsts.ERROR_BLANK:
        	case WebConsts.ERROR_MALFORMED:
        		message_id = R.string.new_internet_lobby_dialog_failed_message_failed ;
        		break ;
        	default:
        		message_id = R.string.new_internet_lobby_dialog_failed_message_unusual ;
        		break ;
        	}
        	
        	text = res.getString(message_id) ;
        	text = text.replace( error_placeholder, "" + mDialogFailureError )
        			.replace( reason_placeholder, "" + mDialogFailureReason ) ;
    	
        	((AlertDialog)dialog).setMessage(text) ;
        	break ;
        	
        case DIALOG_ID_LOBBY_EXISTS:
        	break ;
        	
    	}
    	
	}
	
	
	
	private class ThreadCreateLobbyAndFinish extends Thread {
		
		String mCreatorName ;
		String mName ;
		int mType ;
		int mSize ;
		String mXL ;
		
		Nonce mNonce ;
		
		private ThreadCreateLobbyAndFinish( String creator, String name, int type, int size, String xl ) {
			mCreatorName = creator ;
			mName = name ;
			mType = type ;
			mSize = size ;
			mXL = xl ;
			
			mNonce = null ;
		}
		
		private ThreadCreateLobbyAndFinish( Nonce nonce, String creator, String name, int type, int size, String xl ) {
			mCreatorName = creator ;
			mName = name ;
			mType = type ;
			mSize = size ;
			mXL = xl ;
			
			mNonce = nonce ;
		}
		
		

		@Override
		public void run() {
			
			if ( !mResumed )
				return ;
			
			// WAKE LOCK!
			PowerManager.WakeLock lock = getDimLock(NewInternetLobbyActivity.this) ;
			try {
				Log.d(TAG, "acquiring lock...") ;
				lock.acquire() ;	// released in the finally block.
				Log.d(TAG, "lock acquired...") ;
				
				// put up a working spinner...
				mDialogManager.showDialog(DIALOG_ID_CREATING_LOBBY) ;
				
				Log.d(TAG, "new opener...") ;
				MutableInternetLobby.Opener opener = new MutableInternetLobby.Opener() ;
				opener.setCreatorName( mCreatorName )
					  .setDescription("")
					  .setName(mName)
					  .setXLKey( mXL )
					  .setIsPublic(mType == LOBBY_TYPE_PUBLIC)
					  .setIsItinerant(mType == LOBBY_TYPE_ITINERANT)
					  .setMaxPlayers(mSize) ;
				if ( mLobbyToRehost != null )
					opener.setOrigin( mLobbyToRehost.getOrigin() ) ;
				if ( mNonce != null )
					opener.setNonce(mNonce) ;
				
				Log.d(TAG, "new work order...") ;
				WorkOrder workOrder = opener.openRequest() ;
				if ( !mResumed ) {
					// drop the dialog and quit
					mDialogManager.dismissDialog(DIALOG_ID_CREATING_LOBBY) ;
					return ;
				}
				
				if ( workOrder == null ) {
					// whoops.  error.
					mDialogManager.dismissDialog(DIALOG_ID_CREATING_LOBBY) ;
					mDialogFailureError = opener.getLastError() ;
					mDialogFailureReason = opener.getLastErrorReason() ;
					mDialogManager.showDialog(DIALOG_ID_COMMUNICATION_FAILURE) ;
					return ;
				}
				
				// we work in 1 second chunks.
				Log.d(TAG, "performing work...") ;
				while ( !workOrder.isComplete() ) {
					workOrder.performWork(1000) ;
					if ( !mResumed ) {
						// drop the dialog and quit
						mDialogManager.dismissDialog(DIALOG_ID_CREATING_LOBBY) ;
						return ;
					}
				}
				
				// okay, report the work order result.
				Log.d(TAG, "confirming work...") ;
				MutableInternetLobby lobby = opener.openConfirm(workOrder) ;
				
				if ( lobby == null ) {
					// whoops.  error.
					mDialogManager.dismissDialog(DIALOG_ID_CREATING_LOBBY) ;
					mDialogFailureError = opener.getLastError() ;
					mDialogFailureReason = opener.getLastErrorReason() ;
					if ( mNonce != null && mDialogFailureReason == WebConsts.REASON_EXISTS )
						mDialogManager.showDialog(DIALOG_ID_LOBBY_EXISTS) ;
					else
						mDialogManager.showDialog(DIALOG_ID_COMMUNICATION_FAILURE) ;
					
					return ;
				}
				
				// that's it.
				mDialogManager.dismissDialog(DIALOG_ID_CREATING_LOBBY) ;
				finishWithLobby( lobby ) ;
				
			} finally {
				lock.release() ;
			}
			
		}
		
	}
	
	
	private static final String LOCK_NAME_DIM_STATIC="com.peaceray.quantro.main.NewInternetLobbyActivity.DIM_WAKE_LOCK" ;
	private static volatile PowerManager.WakeLock lockDimStatic=null ;
    
	
	synchronized private static PowerManager.WakeLock getDimLock(Context context) {
    	if (lockDimStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lockDimStatic=mgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
					LOCK_NAME_DIM_STATIC);
			lockDimStatic.setReferenceCounted(true);
		}
	  
		return(lockDimStatic);
    }

	
	////////////////////////////////////////////////////////////////////////////
	//
	// SPINNER CALLBACKS.

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		
		if ( parent == mSpinnerLobbySize )
			mSpinnerLobbySizeSelected = sizeSpinnerPositionToValue(pos) ;
		else if ( parent == mSpinnerLobbyType ) {
			mSpinnerLobbyTypeSelected = typeSpinnerPositionToValue(pos) ;
			setSpinnerTypeDescription() ;
			updateCreateButtonState(false) ;
		}
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
	
	
	private int typeSpinnerPositionToValue( int position ) {
		// we use type position as the constant value itself.
		return position ;
	}
	
	private int typeSpinnerValueToPosition( int value ) {
		// we have set the value (e.g. LOBBY_TYPE_PRIVATE) as
		// exactly the spinner position at which the value occurs.
		return value ;
	}

	private int typePreferenceToLocal( int typePreference ) {
		switch( typePreference ) {
		case QuantroPreferences.INTERNET_LOBBY_TYPE_PUBLIC:
			return LOBBY_TYPE_PUBLIC ;
		case QuantroPreferences.INTERNET_LOBBY_TYPE_PRIVATE:
			return LOBBY_TYPE_PRIVATE ;
		case QuantroPreferences.INTERNET_LOBBY_TYPE_ITINERANT:
			return LOBBY_TYPE_ITINERANT ;
		}
		
		// default 
		return LOBBY_TYPE_PUBLIC ;
	}
	
	
	private int typeLocalToPreference( int typePreference ) {
		switch( typePreference ) {
		case LOBBY_TYPE_PUBLIC:
			return QuantroPreferences.INTERNET_LOBBY_TYPE_PUBLIC ;
		case LOBBY_TYPE_PRIVATE:
			return QuantroPreferences.INTERNET_LOBBY_TYPE_PRIVATE ;
		case LOBBY_TYPE_ITINERANT:
			return QuantroPreferences.INTERNET_LOBBY_TYPE_ITINERANT ;
		}
		
		// default 
		return QuantroPreferences.INTERNET_LOBBY_TYPE_PUBLIC ;
	}
	
	private void setSpinnerTypeDescription() {
		mSpinnerLobbyTypeDescription.setText(
				mSpinnerLobbyTypeDescriptionStrings[mSpinnerLobbyTypeSelected]) ;
	}
	
}
