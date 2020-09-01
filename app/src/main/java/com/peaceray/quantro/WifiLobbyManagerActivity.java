package com.peaceray.quantro;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.dialog.GlobalDialog;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.lobby.LobbyStringEncoder;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.lobby.WiFiLobbyFinder;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.WifiMonitor;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.lobby.WifiLobbyManagerView;
import com.peaceray.quantro.view.dialog.AlertDialog ;
import com.peaceray.quantro.view.dialog.ProgressDialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity for managing multiplayer lobbies.  Very similar in
 * design and constructions to ChallengeManagerActivity, although
 * somewhat simpler in operation.
 * 
 * @author Jake
 *
 */
public class WifiLobbyManagerActivity extends QuantroActivity 
		implements WifiLobbyManagerView.Delegate, WiFiLobbyFinder.Delegate,
		GlobalDialog.DialogContext {

	public static final String TAG = "WiFiLobbyManagerActivity" ;
	
	////////////////////////////////////////////////////////////////////////////
	//
	// DIALOGS
	// 
	// Here are some IDs for Dialogs we intend to display.
	//
	
	// Single-lobby confirmations.  We have very few lobby functions, so
	// this should be a pretty short list.
	public static final int DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID		= 0 ;
	public static final int DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID 	= 1 ;
	
	// Examine lobbies.
	public static final int DIALOG_EXAMINE_LOBBY_ID					= 2 ;
	
	// WHOOPS!  Tried to do something without WiFi connection!
	public static final int DIALOG_NO_WIFI_ID						= 3 ;
	
	
	
	// DIALOG TIERS
	// We place all interactive dialogs in a higher tier.
	int DIALOG_TIER_WORKING = 0 ;
	int DIALOG_TIER_INTERACTION = 1 ;
	
	int [] DIALOG_TIER_INTERACTIVE_IDS = new int[] {
		DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID,
		DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID,
		DIALOG_EXAMINE_LOBBY_ID,
		DIALOG_NO_WIFI_ID,
	} ;
	
	
	// How do we show these?
	DialogManager mDialogManager ;
	
	
	
	// Extras for our intent
	public static final String INTENT_EXTRA_FIND_IP 			= "com.peaceray.quantro.LobbyManagerActivity.INTENT_EXTRA_FIND_IP" ;
	
	
	
	// Some of these dialogs are related to lobbies.  We store a record of
	// the lobby description being referenced.  This should be an instance
	// available in mDisplayedWiFiLobbyDetails.
	WiFiLobbyDetails mDialogWiFiLobbyDetails ;
	
	// Ways to store the very customized views of the EXAMINE dialogs.
	AlertDialog mExamineLobbyDialog ;
	WiFiLobbyDetails mExamineWiFiLobbyDetails ;
	View [] mExamineLobbyDialogViewColorables ;
	View mExamineLobbyDialogViewName ;
	View mExamineLobbyDialogViewInfo ;
	View mExamineLobbyDialogViewPopulation ;
	View mExamineLobbyDialogViewQuery ;
	private boolean mExamineLobbyDialogRefreshing ;
	private static final int EXAMINE_LOBBY_DELAY = 1000 ;
	
	
	private static final int MAX_HIDDEN_LOBBIES = 2000 ;
	private static final int MAX_UNHIDE_SEARCHES = 5 ;
	
	
	Handler mHandler ;
	Runnable mRunnableRefreshExamine ;
	
	// Resources!
	Resources res ;
	
	// View
	WifiLobbyManagerView mLobbyManagerView ;
	// Finder
	WiFiLobbyFinder mLobbyFinder ;
	// Sound pool and controls
	QuantroSoundPool mSoundPool ;
	boolean mSoundControls ;
	
	
	private boolean mShouldConfirmJoin = false ;
	private boolean mShouldConfirmHideOpen = !VersionCapabilities.supportsPopupMenu() ;
			// only need to confirm if there is no "Undo" button in the overflow.
	private boolean mShouldConfirmHideFull = mShouldConfirmHideOpen ;
	private boolean mShouldConfirmHideTargeted = false ;	// always allow targeted lobbies to be removed
	
	// Here are our lobby descriptions.  Initially all are displayed; they can,
	// one-by-one, be hidden.
	ArrayList<WiFiLobbyDetails> mDisplayedWiFiLobbyDetails ;
	ArrayList<WiFiLobbyDetails> mHiddenWiFiLobbyDetails ;
	
	private boolean mStarted ;
	private boolean mResumed ;
	private boolean mTargetWifiAddressOnResume ;
	
	private boolean mHasTargetWifiAddress ;
	private String mTargetWifiAddress ;
	private Nonce mTargetWifiNonce ;
	private String mTargetWifiLobbyName ;
	private String mTargetWifiHostName ;
	private boolean mTargetWifiIsFromInvitation ;
	
	private String mFindingWifiAddress ;
	
	@Override
	public synchronized void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ( !((QuantroApplication)getApplication()).mSupportsFeatureWifi )
        	throw new IllegalStateException("This device does not support WiFi, but is in LobbyManagerActivity!") ;
        setupQuantroActivity( QUANTRO_ACTIVITY_MENU, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
        
        Log.d(TAG, "onCreate") ;
        
        // Force portrait layout
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;
        
        // Set initial values for important stuff.
        // Dialog manager: we store interactive, non-"failed" dialogs in tier 1.  Everything else
        // is tier 0.
        mDialogManager = new DialogManager(this, new int[][]{ new int[0], DIALOG_TIER_INTERACTIVE_IDS } ) ;
        
        // Initialize all structures needed to examine a Lobby.
        mExamineLobbyDialog = null ;
    	mExamineWiFiLobbyDetails = null ;
    	mExamineLobbyDialogViewColorables = null ;
    	mExamineLobbyDialogViewName = null ;
    	mExamineLobbyDialogViewInfo = null ;
    	mExamineLobbyDialogViewPopulation = null ;
    	mExamineLobbyDialogViewQuery = null ;
    	mExamineLobbyDialogRefreshing = false ;
    	
    	
    	// ArrayLists for storing WiFiLobbyDetails.
    	mDisplayedWiFiLobbyDetails = new ArrayList<WiFiLobbyDetails>() ;
    	mHiddenWiFiLobbyDetails = new ArrayList<WiFiLobbyDetails>() ;
    	
        
        mHandler = new Handler() ;
        mRunnableRefreshExamine = new Runnable() {
			@Override
			public void run() {
				refreshExamineDialog() ;
			}
        } ;
        
        res = getResources() ;
        
        
        // LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
        setContentView(R.layout.wifi_lobby_manager_strip_view) ;
        
        // Get a reference to the main view.
        //challengeManagerView = (ChallengeManagerView) findViewById(R.id.challenge_manager_view_hack) ;
        mLobbyManagerView = (WifiLobbyManagerView) findViewById(R.id.wifi_lobby_manager_strip_view) ;
        mLobbyManagerView.setDelegate(this) ;
        
        // Construct the finder
        mLobbyFinder = new WiFiLobbyFinder( this, getResources().getInteger( R.integer.wifi_multiplayer_lobby_announcement_port),
        		getResources().getInteger( R.integer.wifi_multiplayer_lobby_query_port),
        		getResources().getInteger( R.integer.wifi_multiplayer_lobby_query_response_port),
        		15000, 10000, this ) ;	// takes 15 seconds for a lobby to expire; we request from targeted lobbies every 10 seconds.
        
        // That's basically it.  We reset most metadata in onStart().
        mStarted = false ;
        mResumed = false ;
        
        // One last thing: are we launching to target (find) a WiFi address?
        mTargetWifiAddressOnResume = getIntent().hasExtra(INTENT_EXTRA_FIND_IP)
        		&& getIntent().getBooleanExtra(INTENT_EXTRA_FIND_IP, false) ;
        
        // invitation!
        mHasTargetWifiAddress = false ;
		mTargetWifiAddress = null ;
		try {
			Uri data = getIntent().getData() ;
			if ( data != null ) {
				String encoded = data.getQuery() ;
				String ip = LobbyStringEncoder.decodeWiFiLobbyAddress(encoded).getHostAddress() ;
				if ( ip != null ) {
					mHasTargetWifiAddress = true ;
					mTargetWifiAddress = ip ;
					mTargetWifiNonce = LobbyStringEncoder.decodeWiFiLobbyNonce(encoded) ;
					mTargetWifiLobbyName = LobbyStringEncoder.decodeWiFiLobbyName(encoded) ;
					mTargetWifiHostName = LobbyStringEncoder.decodeWiFiLobbyHostName(encoded) ;
					mTargetWifiIsFromInvitation = true ;
				}
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}
		
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}
	
	@Override
	synchronized protected void onNewIntent( Intent intent ) {
		Log.d(TAG, "onNewIntent") ;
		try {
			Uri data = intent.getData() ;
			if ( data != null ) {
				String encoded = data.getQuery() ;
				String ip = LobbyStringEncoder.decodeWiFiLobbyAddress(encoded).getHostAddress() ;
				if ( ip != null ) {
					mHasTargetWifiAddress = true ;
					mTargetWifiAddress = ip ;
					mTargetWifiNonce = LobbyStringEncoder.decodeWiFiLobbyNonce(encoded) ;
					mTargetWifiLobbyName = LobbyStringEncoder.decodeWiFiLobbyName(encoded) ;
					mTargetWifiHostName = LobbyStringEncoder.decodeWiFiLobbyHostName(encoded) ;
					mTargetWifiIsFromInvitation = true ;
				}
			}
			
			Log.d(TAG, "onNewIntent: received intent for IP " + mTargetWifiAddress) ;
			
			// if currently resumed, do it now.
			if ( mResumed ) {
				targetLobby(mTargetWifiAddress, mTargetWifiNonce, mTargetWifiLobbyName, mTargetWifiHostName, mTargetWifiIsFromInvitation) ;
				mHasTargetWifiAddress = false ;
				mTargetWifiAddress = null ;
				mTargetWifiNonce = null ;
				mTargetWifiLobbyName = null ;
				mTargetWifiHostName = null ;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}
	
	
	@Override
	protected synchronized void onStart() {
		super.onStart();
		
		Log.d(TAG, "onStart") ;
		
		// Reveal any previously hidden dialogs.
		mDialogManager.revealDialogs() ;
		
		// Clear the current list of lobbies and start the view.
		for ( int i = mDisplayedWiFiLobbyDetails.size()-1; i >= 0; i-- ) {
			WiFiLobbyDetails ld = mDisplayedWiFiLobbyDetails.get(i) ;
			if ( ld.getSource() == WiFiLobbyDetails.Source.DISCOVERED ) {
				mDisplayedWiFiLobbyDetails.remove(i) ;
			}
		}
		mDisplayedWiFiLobbyDetails.clear() ;
		mLobbyManagerView.setLobbies(mDisplayedWiFiLobbyDetails) ;
		mLobbyManagerView.setHiddenLobbies(mHiddenWiFiLobbyDetails.size() > 0) ;
		mLobbyManagerView.start();
		
		mStarted = true ;
	}
	
	
//  Only here to 'synchronized'; see the CALL FROM OUTSIDE section below
	@Override
	public synchronized void onResume() {
		super.onResume();
		Log.d(TAG, "onResume") ;
		
    	mResumed = true ;
		
		// Get our finder going
		mLobbyFinder.start() ;
		
		// Retrieve sound pool and sound controls
		mSoundPool = ((QuantroApplication)getApplication()).getSoundPool(this) ;
		mSoundControls = QuantroPreferences.getSoundControls(this) ;
		// Provide to lobby manager view
		mLobbyManagerView.setSoundPool(mSoundPool) ;
		mLobbyManagerView.setSoundControls(mSoundControls) ;
		
    	// Finally, post a Runnable to our handler that will refresh any and all
    	// lobby dialogs displayed.
		mExamineLobbyDialogRefreshing = true ;
    	mHandler.postDelayed(mRunnableRefreshExamine, EXAMINE_LOBBY_DELAY) ;
    	
    	// whoa whoa whoa!  Maybe we stop right away!
    	if ( mTargetWifiAddressOnResume ) {
    		mTargetWifiAddressOnResume = false ;
    		findLobby() ;
    	}
    	
    	if ( mHasTargetWifiAddress ) {
    		targetLobby(mTargetWifiAddress, mTargetWifiNonce, mTargetWifiLobbyName, mTargetWifiHostName, mTargetWifiIsFromInvitation) ;
			mHasTargetWifiAddress = false ;
			mTargetWifiAddress = null ;
			mTargetWifiNonce = null ;
			mTargetWifiLobbyName = null ;
			mTargetWifiHostName = null ;
    	}
	}
	
//  Only here to 'synchronized'; see the CALL FROM OUTSIDE section below
	@Override
	public synchronized void onPause() {
		super.onPause();
		Log.d(TAG, "onPause") ;
		
		// Stop our finder
		mLobbyFinder.stop() ;
		
		Log.d(TAG, "stopped") ;
		
		// Stop our dialog refreshing runnable.
		mExamineLobbyDialogRefreshing = false ;
    	mHandler.removeCallbacks(mRunnableRefreshExamine) ;
    	
    	mResumed = false ;
	}
	
	@Override
	protected synchronized void onStop() {
		super.onStop();
		Log.d(TAG, "onStop") ;
		
		// Hide any dialogs.
		mDialogManager.hideDialogs() ;
		
		mLobbyManagerView.stop() ;
		
		mStarted = false ;
	}
	
	// @Override
	// protected synchronized void onDestroy() { }
	
	@Override
	public int getActivityTypeForMusic() {
		// we use lobby music
		return QUANTRO_ACTIVITY_LOBBY ;
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {	
        	if ( mSoundControls && mSoundPool != null )
        		mSoundPool.menuButtonBack() ;
        }

        return super.onKeyDown(keyCode, event);
    }
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// Activity Result listener (making a new lobby) 
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
    	if ( super.handleActivityResult(requestCode, resultCode, data) )
    		return ;
    	
        if (resultCode == RESULT_OK) {  
        	
        	Bundle extras = data.getExtras() ;
        	
            switch (requestCode) {  
            case IntentForResult.REQUEST_NEW_WIFI_LOBBY:
                // Check for the appropriate extra.
            	if ( data.hasExtra(NewLobbyActivity.INTENT_EXTRA_NEW_WIFI_LOBBY_DETAILS) ) {
            		// get the WiFiLobbyDetails and use it to start a new Lobby
            		// activity.
            		WiFiLobbyDetails ld = (WiFiLobbyDetails)extras.get(NewLobbyActivity.INTENT_EXTRA_NEW_WIFI_LOBBY_DETAILS) ;
            		Log.d(TAG, "creating lobby with occupancy " + ((WiFiLobbyDetails.IntentionStatus)ld.getStatus()).getMaxPeople()) ;
            		createLobby( ld ) ;
            	}
                break;  
                
            case IntentForResult.REQUEST_TARGET_WIFI_ADDRESS:
            	// Check for the appropriate extra.
            	if ( data.hasExtra(TargetWifiAddressActivity.INTENT_RESULT_EXTRA_ACTION)
            			&& data.getIntExtra(TargetWifiAddressActivity.INTENT_RESULT_EXTRA_ACTION, TargetWifiAddressActivity.INTENT_RESULT_EXTRA_ACTION_CANCEL)
            					== TargetWifiAddressActivity.INTENT_RESULT_EXTRA_ACTION_FIND ) {
            		String ip = data.getStringExtra(TargetWifiAddressActivity.INTENT_RESULT_EXTRA_IP) ;
            		targetLobby(ip, null, null, null, false) ;
            		// Activity State safe call; if resumed, we immediately target.  If
            		// not, we set the appropriate member variables to target in onResume.
            	}
            	break ;
            }
      
        } else {  
            // gracefully handle failure  
            Log.w(TAG, "Warning: activity result not ok");  
        } 
    }
	
    
    
	/*
	 * *************************************************************************
	 * 
	 * MENU CALLBACKS
	 * 
	 * For creating, displaying, and processing touches to an options menu.
	 * 
	 * *************************************************************************
	 */
    
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
    	//Log.d(TAG, "onCreateOptionsMenu bracket IN") ;
    	super.onCreateOptionsMenu(menu) ;
    	
    	int id = 0 ;
    	switch( VersionSafe.getScreenSizeCategory(this) ) {
    	case VersionSafe.SCREEN_SIZE_SMALL:
    	case VersionSafe.SCREEN_SIZE_NORMAL:
        	id = R.menu.lobby_manager_overflow_normal ;
        	break ;
    	case VersionSafe.SCREEN_SIZE_LARGE:
    		id = R.menu.lobby_manager_overflow_large ;
        	break ;	
    	case VersionSafe.SCREEN_SIZE_XLARGE:
    		id = R.menu.lobby_manager_overflow_xlarge ;
            break ;
    	}
    	
    	// disable for android 3.0.
    	if ( id != 0 && VersionCapabilities.supportsOptionsMenu() ) {
	    	MenuInflater inflater = getMenuInflater() ;
	    	inflater.inflate(id, menu) ;
	    	return true ;
    	}
    	
    	return false ;
    }
    

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
    	super.onPrepareOptionsMenu(menu) ;
    	
    	// enable / disable "unhide" depending on if we have hidden lobbies.
    	menu.setGroupEnabled(
    			R.id.overflow_options_menu_group_unhide,
    			mHiddenWiFiLobbyDetails.size() > 0 ) ;
    	
    	return true ;
    }
    
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent ;
    	switch( item.getItemId() ) {
    	case R.id.overflow_options_menu_unhide:
    		unhideLobbies() ;
    		return true ;
    	case R.id.overflow_options_menu_help:
    		mDialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
    		return true ;
    	case R.id.overflow_options_menu_settings:
    		// launch settings
    		intent = new Intent( this, QuantroPreferences.class ) ;
        	intent.setAction( Intent.ACTION_MAIN ) ;
        	startActivity(intent) ;
    		return true ;
    	}
    	return false ;
    }
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// DIALOG CONSTRUCTION
	//
	// These methods override Activity methods to let us construct and revise
	// dialog descriptions.
	//
	////////////////////////////////////////////////////////////////////////////
	
	@Override
    synchronized protected Dialog onCreateDialog(int id) {
		if ( GlobalDialog.hasDialog(id) ) {
			return GlobalDialog.onCreateDialog(this, id, mDialogManager) ;
		}
		
        AlertDialog.Builder builder ;
        ProgressDialog pd ;
        
        int display = TextFormatting.DISPLAY_DIALOG ;
        int type ;
        int role = TextFormatting.ROLE_CLIENT ;	// irrelevant
        
        View layout ;	// if we use a custom layout, we put it here.
        
        int typePosButton ;
        int typeNegButton ;
        
        switch(id) {
        case DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID:
        	typePosButton = TextFormatting.TYPE_LOBBY_CONFIRM_JOIN_YES_BUTTON ;
        	typeNegButton = TextFormatting.TYPE_LOBBY_CONFIRM_JOIN_NO_BUTTON ;
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setMessage("") ;			// Message is set in onPrepareDialog.  Using "" resolves a display bug.
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses alert
        	builder.setPositiveButton(
        			TextFormatting.format( this, res, display, typePosButton, role, (WiFiLobbyDetails)null ),
        			res.getColor(R.color.lobby_join),
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					mDialogManager.dismissDialog(DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID) ; 
        					joinLobby(mDialogWiFiLobbyDetails) ;
        					mDialogWiFiLobbyDetails = null ;
        					// don't change waitingForWorkingThreadToStartAction; we wait for the thread to start working
        				}
					}) ;
        	builder.setNegativeButton(TextFormatting.format( this, res, display, typeNegButton, role, (WiFiLobbyDetails)null ),
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID) ;
							mDialogWiFiLobbyDetails = null ;
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID) ;
					mDialogWiFiLobbyDetails = null ;
				}
			}) ;
        	return builder.create() ;
        	
        	
        case DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID:
        	typePosButton = TextFormatting.TYPE_LOBBY_CONFIRM_HIDE_YES_BUTTON ;
        	typeNegButton = TextFormatting.TYPE_LOBBY_CONFIRM_HIDE_NO_BUTTON ;
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setMessage("") ;			// Message is set in onPrepareDialog.  Using "" resolves a display bug.
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses alert
        	builder.setPositiveButton(
        			TextFormatting.format( this, res, display, typePosButton, role, (WiFiLobbyDetails)null ),
        			res.getColor(R.color.lobby_hide),
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					mDialogManager.dismissDialog(DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID) ; 
        					hideLobby(mDialogWiFiLobbyDetails) ;
        					mDialogWiFiLobbyDetails = null ;
        					// don't change waitingForWorkingThreadToStartAction; we wait for the thread to start working
        				}
					}) ;
        	builder.setNegativeButton(TextFormatting.format( this, res, display, typeNegButton, role, (WiFiLobbyDetails)null ),
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID) ;
							mDialogWiFiLobbyDetails = null ;
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID) ;
					mDialogWiFiLobbyDetails = null ;
				}
			}) ;
        	return builder.create() ;
        	
        	
        case DIALOG_EXAMINE_LOBBY_ID:
        	typePosButton = TextFormatting.TYPE_LOBBY_EXAMINE_JOIN_BUTTON ;
        	typeNegButton = TextFormatting.TYPE_LOBBY_EXAMINE_CANCEL_BUTTON ;
        	layout = loadExamineLayout() ;
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setView(layout) ;
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses alert	
        	builder.setPositiveButton(
        			TextFormatting.format( this, res, display, typePosButton, role, (String)null ),
        			res.getColor(R.color.lobby_join),
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							// Play this challenge!
							joinLobby( mExamineWiFiLobbyDetails ) ;
							mDialogManager.dismissDialog(DIALOG_EXAMINE_LOBBY_ID) ;		// dismiss
							mExamineWiFiLobbyDetails = null ;
						}
					}) ;
        	builder.setNegativeButton(TextFormatting.format( this, res, display, typeNegButton, role, (String)null ),
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_EXAMINE_LOBBY_ID) ;		// dismiss
							mExamineWiFiLobbyDetails = null ;
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_EXAMINE_LOBBY_ID) ;		// dismiss
					mExamineWiFiLobbyDetails = null ;
				}
			}) ;
        	mExamineLobbyDialog = builder.create() ;
        	return mExamineLobbyDialog ;
        	
        	
        case DIALOG_NO_WIFI_ID:
        	type = TextFormatting.TYPE_LOBBY_MANAGER_NO_WIFI ;
        	typeNegButton = TextFormatting.TYPE_LOBBY_MANAGER_NO_WIFI_CANCEL_BUTTON ;
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setMessage(TextFormatting.format(this, res, display, type, role, (WiFiLobbyDetails)null)) ;
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses alert
        	builder.setNegativeButton(TextFormatting.format( this, res, display, typeNegButton, role, (WiFiLobbyDetails)null ),
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_NO_WIFI_ID) ;
							mDialogWiFiLobbyDetails = null ;
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_NO_WIFI_ID) ;
					mDialogWiFiLobbyDetails = null ;
				}
			}) ;
        	return builder.create() ;
        	
        default:
    		Log.d(TAG, "Cannot create dialog; no dialog case with id: " + id) ;
            return null ;
        }
	}
	
	
	@Override
    synchronized protected void onPrepareDialog(int id, Dialog dialog) {
		if ( GlobalDialog.hasDialog(id) ) {
			GlobalDialog.onPrepareDialog(this, id, dialog) ;
			return ;
		}
    	// THIS METHOD IS DEPRECIATED
    	// (and I don't care!  the DialogFragment class will call through
    	// to this method for compatibility)
    	
    	// display, role for DialogFormatting
    	int display = TextFormatting.DISPLAY_DIALOG ;
        int type ;
        int role = TextFormatting.ROLE_CLIENT ;	// irrelevant
        
    	String text ;
    	switch ( id ) {
    	case GlobalDialog.DIALOG_ID_WHATS_NEW:
    		GlobalDialog.onPrepareDialog(this, id, dialog) ;
    		return ;
    		
        case DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID:
        	type = TextFormatting.TYPE_LOBBY_CONFIRM_JOIN ;
    		text = TextFormatting.format(this, res, display, type, role, mDialogWiFiLobbyDetails) ;
    		((AlertDialog)dialog).setMessage(text) ;
    		break ;
        	
        case DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID:
        	type = TextFormatting.TYPE_LOBBY_CONFIRM_HIDE ;
    		text = TextFormatting.format(this, res, display, type, role, mDialogWiFiLobbyDetails) ;
    		((AlertDialog)dialog).setMessage(text) ;
    		break ;
        	
        case DIALOG_EXAMINE_LOBBY_ID:
        	mExamineWiFiLobbyDetails = mDialogWiFiLobbyDetails ;
        	refreshExamineDialog() ;
        	break ;
        
        case DIALOG_NO_WIFI_ID:
        	// no prep. necessary
        	break ;
        	
        default:
    		Log.d(TAG, "Cannot prepare dialog; no dialog case with id: " + id) ;
            return ;
    	}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// Action Performers
	//
	// These are actions which could be effectively inlined, but are separated
	// into methods because they can occur from multiple places.  For example,
	// depending on settings we may confirm a lobby join at the moment the
	// button is pressed, or after a dialog is displayed.
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	synchronized private void targetLobby( String ip, Nonce nonce, String lobbyName, String hostName, boolean fromInvitation ) {
		if ( ip == null )
			return ;
		
		// do this in an AsyncTask; otherwise we do network stuff on the main thread.
		new AsyncTask<Object, Object, Integer>() {
			@Override
			protected Integer doInBackground(Object... params) {
				String ip = (String)params[0] ;
				Nonce nonce = (Nonce)params[1] ;
				String lobbyName = (String)params[2] ;
				String hostName = (String)params[3] ;
				boolean fromInvitation = (Boolean)params[4] ;
				
				// Our basic goal is to
				// 1. ensure that we will search out this Lobby if necessary.
				// 2. ensure that this lobby appears on our lobby list as
				//		"targeted."
				// 3. ensure that the user is aware of the lobby if it already
				//		appeared on the list
				
				// our first step: look for the lobby to see if it already exists.
				ArrayList<WiFiLobbyDetails> displayedLobbies = (ArrayList<WiFiLobbyDetails>) mDisplayedWiFiLobbyDetails.clone() ;
				Iterator<WiFiLobbyDetails> iter = displayedLobbies.iterator() ;
				WiFiLobbyDetails receivedDetails = null ;
				for ( ; iter.hasNext() ; ) {
					WiFiLobbyDetails ld = iter.next() ;
					boolean matches = false ;
					if ( ld.hasReceivedStatus() ) {
						if ( ip.equals( ld.getReceivedStatus().getReportedAddressHostName() ) )
							matches = true ;
						else if ( ip.equals( ld.getReceivedStatus().getReceivedFromAddressHostName() ) )
							matches = true ;
					}
					if ( matches ) {
						receivedDetails = ld ;
						break ;
					}
				}
				
				// if the source of receivedDetails is DISCOVERED, or we don't 
				// have a receivedDetails, add the appropriate lobby details.
				// This will be sanely merged with the existing entry (if applicable).
				WiFiLobbyDetails targetedDetails = null ;
				if ( receivedDetails == null || receivedDetails.getSource() == WiFiLobbyDetails.Source.DISCOVERED ) {
					if ( fromInvitation ) {
						targetedDetails = WiFiLobbyDetails.newInvitedInstance(nonce, ip, lobbyName, hostName) ;
					} else {
						targetedDetails = WiFiLobbyDetails.newTargetedInstance(ip) ;
					}
				}
				
				// start looking (if possible).
				if ( mResumed ) {
					// 1. ensure we're searching for the lobby
					if ( receivedDetails == null )
						mFindingWifiAddress = ip ;
					mLobbyFinder.targetIP(ip) ;
					// 2. ensure the lobby appears on the list as 'targeted'
					if ( targetedDetails != null )
						addLobby(targetedDetails) ;
					// 3. if we have the lobby, show the user
					if ( receivedDetails != null ) {
						mDialogWiFiLobbyDetails = receivedDetails ;
						return Integer.valueOf( DIALOG_EXAMINE_LOBBY_ID ) ;
					}
				} else {
					mHasTargetWifiAddress = true ;
					mTargetWifiAddress = ip ;
					mTargetWifiNonce = nonce ;
					mTargetWifiLobbyName = lobbyName ;
					mTargetWifiHostName = hostName ;
					mTargetWifiIsFromInvitation = fromInvitation ;
				}
				
				return Integer.valueOf(-1) ;
			}
			
			protected void onPostExecute(Integer result) {
				// the result is a dialog we should display.
				int id = result.intValue() ;
				if ( id >= 0 ) {
					mDialogManager.showDialog(id) ;
				}
			}
		}.execute(ip, nonce, lobbyName, hostName, Boolean.valueOf(fromInvitation)) ;
	}
	
	
	synchronized private void findLobby() {
		Intent intent = new Intent( this, TargetWifiAddressActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	startActivityForResult(intent,  IntentForResult.REQUEST_TARGET_WIFI_ADDRESS) ;
	}
	
	
	synchronized private void createLobby( WiFiLobbyDetails ld ) {
		if ( ld == null )
			Log.e(TAG, "createLobby called with 'null' WiFiLobbyDetails") ;
		
		
		// Load our name from SharedPreferences
		String name = QuantroPreferences.getMultiplayerName(this) ;
    	LobbyIntentPackage lip = new LobbyIntentPackage(
    			((QuantroApplication)getApplicationContext()).personalNonce,
    			name,
    			ld ) ;
    	lip.setAsDirectHost( 
    			res.getInteger( R.integer.wifi_multiplayer_lobby_port ),
    			res.getInteger( R.integer.wifi_multiplayer_lobby_announcement_port),
    			res.getInteger( R.integer.wifi_multiplayer_lobby_query_port) ) ;
		
    	launchLobbyIntent(lip) ;
	}
	
	synchronized private void joinLobby( WiFiLobbyDetails ld ) {
		// Load the name from SharedPreferences
		String name = QuantroPreferences.getMultiplayerName(this) ;
		LobbyIntentPackage lip = new LobbyIntentPackage(
    			((QuantroApplication)getApplicationContext()).personalNonce,
    			name,
    			ld ) ;
		
		WiFiLobbyDetails.ReceivedStatus rs = ld.getReceivedStatus() ;
		if ( rs.getReceivedFromReportedAddress() )
			lip.setAsDirectClient(rs.getReportedAddress()) ;
		else
			lip.setAsDirectClient(rs.getReportedAddress(),
					new InetSocketAddress( rs.getReceivedFromAddressHostName(), ((InetSocketAddress)rs.getReportedAddress()).getPort() ) ) ;
    	
    	launchLobbyIntent(lip) ;
	}
	
	synchronized private void launchLobbyIntent( LobbyIntentPackage lip ) {
    	Intent intent = new Intent( this, LobbyActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP ) ;
    	intent.putExtra( res.getString( R.string.intent_extra_lobby_intent_package ), lip) ;
    	
    	startActivity(intent) ;
	}
	
	synchronized private void hideLobby( WiFiLobbyDetails ld ) {
		// Look for the lobby description in those displayed - if there,
		// remove.  Look for it in the hidden descriptions - if not there,
		// place it.  Finally, tell the LobbyManagerView to remove this
		// description.
		
		for ( int i = 0; i < mDisplayedWiFiLobbyDetails.size(); i++ ) {
			WiFiLobbyDetails ldHere  = mDisplayedWiFiLobbyDetails.get(i) ;
			if ( ldHere.isSameLobby(ld) || ld.isSameLobby(ldHere) ) {
				mDisplayedWiFiLobbyDetails.remove(i) ;
				break ;
			}
		}
		
		boolean inHidden = false ;
		for ( int i = 0; i < mDisplayedWiFiLobbyDetails.size(); i++ ) {
			WiFiLobbyDetails ldHere  = mDisplayedWiFiLobbyDetails.get(i) ;
			if ( ldHere.isSameLobby(ld) || ld.isSameLobby(ldHere) ) {
				inHidden = true ;
				break ;
			}
		}
		
		if ( !inHidden ) {
			if ( mHiddenWiFiLobbyDetails.size() >= MAX_HIDDEN_LOBBIES )
				mHiddenWiFiLobbyDetails.remove(0) ;
			mHiddenWiFiLobbyDetails.add(ld) ;
		}
		
		mLobbyFinder.untargetIP(ld.getStatus().getIPAddress()) ;
		
		runOnUiThread( new RemoveLobbyFromViewRunnable( mLobbyManagerView, ld ) ) ;
	}
	
	synchronized private void unhideLobbies() {
		// clears our hidden lobby list, after, moving at most MAX_UNHIDE_SEARCHES
		// searches to our displayed list.
		String [] targetIPs = new String[MAX_UNHIDE_SEARCHES] ;
		for ( int i = 0; i < MAX_UNHIDE_SEARCHES; i++ )
			targetIPs[i] = null ;
		synchronized( mDisplayedWiFiLobbyDetails ) {
			int revealedSearches = 0 ;
			for ( int i = this.mHiddenWiFiLobbyDetails.size()-1; i >= 0 && revealedSearches < MAX_UNHIDE_SEARCHES; i-- ) {
				if ( mHiddenWiFiLobbyDetails.get(i).getSource() != WiFiLobbyDetails.Source.DISCOVERED ) {
					WiFiLobbyDetails ld = mHiddenWiFiLobbyDetails.remove(i) ;
					addLobby(ld) ;
					targetIPs[revealedSearches++] = ld.getStatus().getIPAddress() ;
					// we decrement index each time, so it doesn't matter
					// that we are reducing the array size.
				}
			}
		}
		
		mHiddenWiFiLobbyDetails.clear() ;
		// non-searched lobbies will gradually repopulate.
		Toast.makeText(this, R.string.lobby_manager_toast_unhide_lobbies, Toast.LENGTH_LONG).show() ;
		mLobbyManagerView.setHiddenLobbies(false) ;
		
		new AsyncTask<String, Object, Object>() {
			@Override
			protected Object doInBackground(String... params) {
				for ( int i = 0; i < params.length; i++ ) {
					if ( params[i] != null ) {
						mLobbyFinder.targetIP(params[i]) ;
					}
				}
				return null ;
			}
		}.execute(targetIPs) ;
	}
	
	private WiFiLobbyDetails addLobby( WiFiLobbyDetails ld ) {
		synchronized( mDisplayedWiFiLobbyDetails ) {
			// Attempt to add the specified lobby.  If not present in either our
			// list of displayed lobbies or hidden lobbies, adds to displayed
			// and informs the view.  If present in displayed, updates and tells
			// the view.  If hidden, does nothing.
			// All of this assumes that we are currently Started.  If not, do
			// nothing.
			if ( mStarted ) {
				for ( int i = 0; i < mDisplayedWiFiLobbyDetails.size(); i++ ) {
					WiFiLobbyDetails ldHere  = mDisplayedWiFiLobbyDetails.get(i) ;
					if ( ld.isSameLobby(ldHere) ) {
						Log.d(TAG, "addLobby: merging into existing lobby " + i) ;
						ldHere.mergeFrom(ld) ;
						runOnUiThread( new RefreshLobbyInViewRunnable( mLobbyManagerView, ldHere ) ) ;
						return ldHere ;
					}
				}
				
				for ( int i = 0; i < mHiddenWiFiLobbyDetails.size(); i++ ) {
					WiFiLobbyDetails ldHere  = mHiddenWiFiLobbyDetails.get(i) ;
					if ( ld.isSameLobby(ldHere) ) {
						// hidden; do nothing
						ldHere.mergeFrom(ld) ;
						return ldHere ;
					}
				}
				
				// Otherwise, add.
				mDisplayedWiFiLobbyDetails.add(ld) ;
				runOnUiThread( new AddLobbyToViewRunnable( mLobbyManagerView, ld ) ) ;
				return ld ;
			}
		}
		
		return null ;
	}
	
	
	@SuppressWarnings("unused")
	private void removeLobby( WiFiLobbyDetails ld ) {
		removeLobby( ld.getNonce().toString() ) ;
	}
	
	private void removeLobby( String key ) {
		synchronized ( mDisplayedWiFiLobbyDetails ) {
			// Remove only if its displayed and non-targeted.  Hidden lobbies stay hidden.
			if ( mStarted ) {
				for ( int i = 0; i < mDisplayedWiFiLobbyDetails.size(); i++ ) {
					WiFiLobbyDetails ldHere  = mDisplayedWiFiLobbyDetails.get(i) ;
					if ( key.equals(ldHere.getNonce().toString() ) ) {
						// make sure this was not targeted or searched.
						if ( ldHere.getSource() == WiFiLobbyDetails.Source.DISCOVERED ) {
							mDisplayedWiFiLobbyDetails.remove(i) ;
							runOnUiThread( new RemoveLobbyFromViewRunnable( mLobbyManagerView, ldHere ) ) ;
							return ;
						}
					}
				}
			}
		}
	}
	
	
	synchronized private View loadExamineLayout() {
		View layout = getLayoutInflater().inflate(R.layout.examine_lobby_layout, null) ;
		// collect references to all its component views
		
		// colored dividers
		mExamineLobbyDialogViewColorables = new View[] {
			layout.findViewById(R.id.examine_lobby_bottom_divider)	
		} ;
		
		mExamineLobbyDialogViewName = layout.findViewById(R.id.examine_lobby_name) ;
		mExamineLobbyDialogViewInfo = layout.findViewById(R.id.examine_lobby_info) ;
		mExamineLobbyDialogViewPopulation = layout.findViewById(R.id.examine_lobby_population) ;
		mExamineLobbyDialogViewQuery = layout.findViewById(R.id.examine_lobby_text_view_action_query) ;
		
		return layout ;
	}
	
	
	synchronized private void refreshExamineDialog() {
		// This method is called by mRunnable as its only action.  It is
		// synchronized, so no other method here will occur concurrently.
		if ( mExamineWiFiLobbyDetails != null && mExamineLobbyDialog != null ) {
			boolean isSearching = this.mLobbyManagerView.isSearching(mExamineWiFiLobbyDetails) ;
			int display = TextFormatting.DISPLAY_DIALOG ;
			int role = TextFormatting.ROLE_CLIENT ;
			int type ;
			
			type = isSearching ? TextFormatting.TYPE_LOBBY_EXAMINE_NAME_SEARCHING : TextFormatting.TYPE_LOBBY_EXAMINE_NAME ;
			((TextView)mExamineLobbyDialogViewName).setText( TextFormatting.format(this, res, display, type, role, mExamineWiFiLobbyDetails) ) ;
			
			type = isSearching ? TextFormatting.TYPE_LOBBY_EXAMINE_INFO_SEARCHING : TextFormatting.TYPE_LOBBY_EXAMINE_INFO ;
			((TextView)mExamineLobbyDialogViewInfo).setText( TextFormatting.format(this, res, display, type, role, mExamineWiFiLobbyDetails) ) ;
			
			type = isSearching ? TextFormatting.TYPE_LOBBY_EXAMINE_POPULATION_SEARCHING : TextFormatting.TYPE_LOBBY_EXAMINE_POPULATION ;
			((TextView)mExamineLobbyDialogViewPopulation).setText( TextFormatting.format(this, res, display, type, role, mExamineWiFiLobbyDetails) ) ;
			
			// Set the query if we have WiFi.  Otherwise, set a "no wifi" message.
			boolean onWifi = WifiMonitor.getWifiIpAddress(this) != 0 ;
			if ( isSearching ) {
				type = TextFormatting.TYPE_LOBBY_EXAMINE_QUERY_SEARCHING ;
			} else if ( onWifi ) {
				type = TextFormatting.TYPE_LOBBY_EXAMINE_QUERY ;
			} else {
				type = TextFormatting.TYPE_LOBBY_EXAMINE_NO_WIFI ;
			}
			((TextView)mExamineLobbyDialogViewQuery).setText( TextFormatting.format(this, res, display, type, role, mExamineWiFiLobbyDetails) ) ;
			
			// Set colorable items
			int color ;
			if ( isSearching )
				color = res.getColor(R.color.lobby_button_strip_searching_main) ;
			else if ( mExamineWiFiLobbyDetails.getReceivedStatus().getNumPeople() < mExamineWiFiLobbyDetails.getReceivedStatus().getMaxPeople() )
				color = res.getColor(R.color.lobby_button_strip_open_main) ;
			else
				color = res.getColor(R.color.lobby_button_strip_full_main) ;
			for ( int i = 0; i < mExamineLobbyDialogViewColorables.length; i++ ) {
				mExamineLobbyDialogViewColorables[i].setBackgroundColor(color) ;
			}
			
			// enable / disable button.
			if ( mExamineLobbyDialog != null ) {
				boolean open = !isSearching && mExamineWiFiLobbyDetails.getReceivedStatus().getNumPeople() < mExamineWiFiLobbyDetails.getReceivedStatus().getMaxPeople() ;
				mExamineLobbyDialog.setButtonEnabled(Dialog.BUTTON_POSITIVE, open) ;
			}
		}
		
		// Repost ourself IF we are currently running.
		if ( mExamineLobbyDialogRefreshing ) {
			mHandler.postDelayed(mRunnableRefreshExamine, EXAMINE_LOBBY_DELAY) ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// Action Runnables
	//
	// These are "runnable" objects which can be placed on the UI thread to make
	// changes to displayed items.  Each has a specific purpose.
	//
	////////////////////////////////////////////////////////////////////////////
	
	private class AddLobbyToViewRunnable implements Runnable {
		WifiLobbyManagerView lmv ;
		WiFiLobbyDetails ld ;
		public AddLobbyToViewRunnable( WifiLobbyManagerView lmv, WiFiLobbyDetails ld ) {
			this.lmv = lmv ;
			this.ld = ld ;
		}
		@Override
		public void run() {
			lmv.addLobby(ld) ;
		}
	}
	
	private class RemoveLobbyFromViewRunnable implements Runnable {
		WifiLobbyManagerView lmv ;
		WiFiLobbyDetails ld ;
		public RemoveLobbyFromViewRunnable( WifiLobbyManagerView lmv, WiFiLobbyDetails ld ) {
			this.lmv = lmv ;
			this.ld = ld ;
		}
		@Override
		public void run() {
			lmv.removeLobby(ld) ;
			lmv.setHiddenLobbies(mHiddenWiFiLobbyDetails.size() > 0) ;
		}
	}
	
	private class RefreshLobbyInViewRunnable implements Runnable {
		WifiLobbyManagerView lmv ;
		WiFiLobbyDetails ld ;
		public RefreshLobbyInViewRunnable( WifiLobbyManagerView lmv, WiFiLobbyDetails ld ) {
			this.lmv = lmv ;
			this.ld = ld ;
		}
		@Override
		public void run() {
			lmv.refreshView(ld) ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// Lobby Manager View delegate methods
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * The user wants to open a new lobby.  The delegate should take it from here.
	 * 
	 * @param view The view from which the user indicated their desire.
	 */
	synchronized public void wlmvd_createNewWiFiLobby( WifiLobbyManagerView view ) {
		
		if ( WifiMonitor.getWifiIpAddress(this) == 0 ) {
			mDialogManager.showDialog(DIALOG_NO_WIFI_ID) ;
			return ;
		}
		
		// An "Intent for action" so we know whether
		// a new challenge was sent (and can then conditionally refresh).
    	Intent intent = new Intent( this, NewLobbyActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	startActivityForResult(intent, IntentForResult.REQUEST_NEW_WIFI_LOBBY) ;
	}
	
	/**
	 * The user wants to join the lobby described by the specified challenge.  The
	 * delegate should take it from here.
	 * 
	 * @param lobbyDetails The description object for this lobby.
	 * @param view The view from which the user indicated their desire.
	 */
	synchronized public void wlmvd_joinWiFiLobby( WiFiLobbyDetails lobbyDetails, WifiLobbyManagerView view ) {
		
		if ( WifiMonitor.getWifiIpAddress(this) == 0 ) {
			mDialogManager.showDialog(DIALOG_NO_WIFI_ID) ;
			return ;
		}
		
		// We've been given a lobby description, which should be enough info to
		// join the lobby.  Do so now, if we don't confirm.
		// Otherwise, put up a dialog.
		
		if ( mShouldConfirmJoin ) {
			mDialogWiFiLobbyDetails = lobbyDetails ;
			mDialogManager.showDialog(DIALOG_CONFIRM_SINGLE_LOBBY_JOIN_ID) ;
		}
		else
			joinLobby(lobbyDetails) ;
	}
	
	
	synchronized public void wlmvd_findWiFiLobby( WifiLobbyManagerView view ) {
		findLobby() ;
	}
	
	
	/**
	 * The user wants to hide the specified lobby from our view.
	 * 
	 * @param lobbyDetails The description object for this lobby.
	 * @param view
	 */
	synchronized public void wlmvd_hideWiFiLobby( WiFiLobbyDetails lobbyDetails, WifiLobbyManagerView view ) {
		
		// If confirm, put up a dialog.  If not, hide right now.
		boolean targeted = lobbyDetails.getSource() != WiFiLobbyDetails.Source.DISCOVERED ;
		if ( targeted ) {
			if ( this.mShouldConfirmHideTargeted ) {
				mDialogWiFiLobbyDetails = lobbyDetails ;
				mDialogManager.showDialog(DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID) ;
			} else {
				hideLobby(lobbyDetails) ;
			}
		} else {
			boolean full = lobbyDetails.getReceivedStatus().getNumPeople() == lobbyDetails.getReceivedStatus().getMaxPeople() ;
			if ( ( full && mShouldConfirmHideFull ) || ( !full && mShouldConfirmHideOpen ) ) {
				mDialogWiFiLobbyDetails = lobbyDetails ;
				mDialogManager.showDialog(DIALOG_CONFIRM_SINGLE_LOBBY_HIDE_ID) ;
			} else {
				hideLobby(lobbyDetails) ;
			}
		}
	}
	
	
	/**
	 * The user wants to see a list of options regarding hiding this lobby from view.
	 * For instance, perhaps she wants to hide this lobby by IP address, rather than
	 * Nonce (i.e., "Hide this lobby" vs. "Hide all lobbies at this address."
	 * @param lobbyDetails
	 * @param view
	 */
	synchronized public void wlmvd_hideWifiLobbyMenu( WiFiLobbyDetails lobbyDetails, WifiLobbyManagerView view ) {
		
		// TODO: fill this in.
		
	}
	
	
	/**
	 * The user wants to "examine" (whatever that means) the specified lobby.
	 * @param lobbyDetails
	 * @param view
	 */
	synchronized public void wlmvd_examineWiFiLobby( WiFiLobbyDetails lobbyDetails, WifiLobbyManagerView view ) {
		mDialogWiFiLobbyDetails = lobbyDetails ;
		mDialogManager.showDialog(DIALOG_EXAMINE_LOBBY_ID) ;
	}
	
	
	@Override
	public void wlmvd_unhideWifiLobbies( WifiLobbyManagerView view ) {
		unhideLobbies() ;
	}
	
	@Override
	public void wlmvd_help( WifiLobbyManagerView view ) {
		mDialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
	}
	
	@Override
	public void wlmvd_settings( WifiLobbyManagerView view ) {
		Intent intent = new Intent( this, QuantroPreferences.class ) ;
		intent.setAction( Intent.ACTION_MAIN ) ;
		startActivity(intent) ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// Lobby Finder Delegate methods
	//
	////////////////////////////////////////////////////////////////////////////
	
	synchronized public void lobbyFinderFoundNewLobby( WiFiLobbyFinder finder, String key, WiFiLobbyDetails desc ) {
		if ( !finder.isStarted() )
			return ;
		
		WiFiLobbyDetails ld = addLobby(desc) ;
		
		if ( ld == null )
			return ;
		
		String ip = ld.getStatus().getIPAddress() ;
		String finding = mFindingWifiAddress ;
		if ( finding != null && ip.trim().equals(finding.trim()) ) {
			mFindingWifiAddress = null ;
			this.mDialogWiFiLobbyDetails = ld ;
			mDialogManager.showDialog(DIALOG_EXAMINE_LOBBY_ID) ;
		}
	}
	
	
	synchronized public void lobbyFinderHasLobbyUpdate( WiFiLobbyFinder finder, String key, WiFiLobbyDetails desc ) {
		if ( !finder.isStarted() )
			return ;
		
		Log.d(TAG, "lobbyFinderHasLobbyUpdate") ;
		WiFiLobbyDetails ld = addLobby(desc) ;
		
		if ( ld == null )
			return ;
		
		String ip = ld.getStatus().getIPAddress() ;
		String finding = mFindingWifiAddress ;
		if ( finding != null && ip.trim().equals(finding.trim()) ) {
			mFindingWifiAddress = null ;
			this.mDialogWiFiLobbyDetails = ld ;
			mDialogManager.showDialog(DIALOG_EXAMINE_LOBBY_ID) ;
		}
	}
	
	
	synchronized public void lobbyFinderLobbyVanished( WiFiLobbyFinder finder, String key ) {
		if ( !finder.isStarted() )
			return ;
		removeLobby(key) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// GlobalDialog.DialogContext
	// 

	@Override
	public Context getContext() {
		return this ;
	}

	@Override
	public String getHelpDialogHTMLRelativePath() {
		return "help/lobby_manager_wifi_activity.html" ;
	}
	
	@Override
	public String getHelpDialogContextName() {
		return getResources().getString(R.string.global_dialog_help_name_wifi_lobby_manager) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
}
