// enable / disable "unhide" depending on if we have hidden lobbies.
package com.peaceray.quantro;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.database.InternetLobbyDatabaseAdapter;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.dialog.GlobalDialog;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.LobbyStringEncoder;
import com.peaceray.quantro.lobby.MutableInternetLobby;
import com.peaceray.quantro.lobby.WebConsts;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.dialog.ProgressDialog;
import com.peaceray.quantro.view.lobby.InternetLobbyManagerView;

/**
 * An Activity for managing / creating / joining Internet lobbies.
 * 
 * Similar in construction to the ChallengeManagerActivity, but hopefully with
 * retrospect we can simplify things a bit?
 * 
 * Lobby List: at this moment, I'm establishing a one-time (w/ refresh button?)
 * lobby list retrieval. Trying to make rolling-queries is just too complicated
 * given that the XL-linked lobbies show up in a prioritized order.
 * 
 * Instead, we get a one-time list from the server (which hopefully is long
 * enough to cover all our bases?) and individually query statuses for those
 * lobbies.
 * 
 * We can then:
 * 
 * 1. Refresh the entire list periodically (like every 5 minutes or so, or when
 * the number of open lobbies drops below a certain margin) 2. Allow manually
 * refreshes for when the user presses a button.
 * 
 * @author Jake
 * 
 */
public class InternetLobbyManagerActivity extends QuantroActivity implements
		InternetLobbyManagerView.Delegate, GlobalDialog.DialogContext {

	public static final String TAG = "InternetLobbyManagerActivity";
	
	public static final String BUNDLE_EXTRA_MIN_VERSION = "com.peaceray.quantro.InternetLobbyManagerActivity.BUNDLE_EXTRA_MIN_VERSION" ;
	public static final String BUNDLE_EXTRA_SHOWED_INTRO = "com.peaceray.quantro.InternetLobbyManagerActivity.BUNDLE_EXTRA_SHOWED_INTRO" ;

	// //////////////////////////////////////////////////////////////////////////
	//
	// DIALOGS

	// Quit while working.
	private static final int DIALOG_ID_QUITTING = 0;
	
	// Getting the minimum version code
	private static final int DIALOG_ID_APP_OUT_OF_DATE = 1;
	private static final int DIALOG_ID_GETTING_APP_VERSION = 2 ;
	private static final int DIALOG_ID_GETTING_APP_VERSION_NO_RESPONSE = 3 ;

	// Basic Internet Multiplayer intro
	private static final int DIALOG_ID_INTRO = 4;

	// Refreshing the lobby list.
	private static final int DIALOG_ID_REFRESHING_LIST = 5;
	private static final int DIALOG_ID_REFRESHING_LIST_FAILED_COMMUNICATION = 6;
	private static final int DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE = 7;

	// Confirmation?
	private static final int DIALOG_ID_CONFIRM_JOIN_LOBBY = 8;
	private static final int DIALOG_ID_CONFIRM_HIDE_LOBBY = 9;

	// Examination
	private static final int DIALOG_ID_EXAMINE_LOBBY = 10;
	
	// Refresh before a join
	private static final int DIALOG_ID_REFRESHING_FOR_JOIN = 11 ;
	private static final int DIALOG_ID_REFRESHING_FOR_JOIN_FAILURE_COMMUNICATION = 12 ;
	
	
	// Invitations!
	private static final int DIALOG_ID_RECEIVING_INVITATION = 13 ;
	private static final int DIALOG_ID_RECEIVING_INVITATION_FAILED_MALFORMED = 14 ;
	private static final int DIALOG_ID_RECEIVING_INVITATION_FAILED_COMMUNICATION = 15 ;
	private static final int DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE = 16 ;
	
	

	// DIALOG TIERS
	// We place all interactive dialogs in a higher tier.
	int DIALOG_TIER_WORKING = 0;
	int DIALOG_TIER_INTERACTION = 1;
	int DIALOG_TIER_EXAMINATION = 2;
	int DIALOG_TIER_QUITTING = 3;

	int[] DIALOG_TIER_INTERACTIVE_IDS = new int[] {
			DIALOG_ID_INTRO,
			DIALOG_ID_REFRESHING_LIST_FAILED_COMMUNICATION,
			DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE,
			DIALOG_ID_CONFIRM_JOIN_LOBBY, DIALOG_ID_CONFIRM_HIDE_LOBBY,
			DIALOG_ID_REFRESHING_FOR_JOIN_FAILURE_COMMUNICATION,
			DIALOG_ID_RECEIVING_INVITATION_FAILED_MALFORMED,
			DIALOG_ID_RECEIVING_INVITATION_FAILED_COMMUNICATION,
			DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE};

	int[] DIALOG_TIER_EXAMINATION_IDS = new int[] { DIALOG_ID_EXAMINE_LOBBY };

	int[] DIALOG_TIER_QUITTING_IDS = new int[] {
			DIALOG_ID_APP_OUT_OF_DATE,
			DIALOG_ID_QUITTING };

	private long REFRESH_LIST_ON_RESUME_AFTER = 1000 * 60 * 2; // 2 minutes
	private long REFRESH_EXAMINED_LOBBY_EVERY = 1000 * 10; // 15 seconds
	private long MAXIMUM_TIME_SINCE_REFRESH_FOR_JOIN = 1000 * 30 ;	// must have been refreshed w/o the last 30 seconds.

	// our main view.
	private InternetLobbyManagerView mInternetLobbyManagerView;

	// we will be showing dialogs, most likely...
	private DialogManager mDialogManager;
	private InternetLobby mDialogLobby; // the 'lobby' object associated with
										// the currently displayed dialog.

	// worker thread
	private InternetLobbyManagerWorkerThread mThread;

	// InternetLobbies! May or may not have been refreshed.
	private long mInternetLobbiesLastRefreshAt;
	private ArrayList<InternetLobby> mInternetLobbies;
	private ArrayList<Nonce> mHiddenNonces;
	
	private int mMinVersionCode = -1 ;
		// The minimum version code necessary to participate in Internet Multiplayer.
		// Defaults to -1 until the version code is acquired.  CHECK THIS VALUE
		// BEFORE ANY COMMUNICATION.

	private QuantroSoundPool mSoundPool;
	private boolean mSoundControls;

	// The time at which we performed the last action. We mandate a minimum
	// delay between actions; most actions put stress on the server, so we
	// don't want them spammed. Additionally, this delay will help with the
	// slight (but non-zero) delay between a user request, as conferred through
	// a ChallengeManagerView.Delegate method, and the actual execution of that
	// action by the work thread.
	private static final int MINIMUM_DELAY = 600;
	private long mLastActionTime = System.currentTimeMillis();

	// sometimes we refresh when we resume the activity.
	private boolean mRefreshLobbyListOnResume;
	private boolean mMustRefreshLobbyListOnResume;
	private boolean mReceiveInvitationStringOnResume;
	private String mInvitationString ;

	// intro'd?
	private boolean mShowedIntro ;
	
	private boolean mResumed;

	// confirmation?
	private boolean mShouldConfirmJoin = false;
	private boolean mShouldConfirmHide = !VersionCapabilities.supportsPopupMenu() ;
			// only need to confirm if there is no "Undo" button in the overflow.
	private boolean mShouldConfirmHidePrivateNotClosedForever = true ;

	Resources res;

	Handler mHandler;

	// Lobby Examination. Long-press a lobby to display this dialog.
	// While displayed (and while activity is Resumed) we perform a periodic
	// refresh of the lobby itself.
	private AlertDialog mExamineLobbyDialog = null;
	private View mExamineLobbyDialogContent = null;
	private View[] mExamineLobbyDialogContentViews = null;
	private boolean mExamineLobbyDialogShown = false;
	private Runnable mExamineLobbyDialogRefreshRunnable = null;
	private int mExamineLobbyDialogStatusOnLastRefresh ;
	private static final int EXAMINE_LOBBY_DIALOG_NAME = 0;
	private static final int EXAMINE_LOBBY_DIALOG_DESCRIPTION = 1;
	private static final int EXAMINE_LOBBY_DIALOG_CREATED = 2;
	private static final int EXAMINE_LOBBY_DIALOG_HOSTING = 3;
	private static final int EXAMINE_LOBBY_DIALOG_QUERY = 4;
	private static final int EXAMINE_LOBBY_DIALOG_BOTTOM_DIVIDER = 5;
	private static final int NUM_EXAMINE_LOBBY_DIALOG_ELEMENTS = 6;

	
	
	@Override
	protected void onNewIntent( Intent intent ) {
		super.onNewIntent(intent);
		try {
			Uri data = intent.getData() ;
			if ( data != null ) {
				mInvitationString = data.getQuery() ;
				mReceiveInvitationStringOnResume = mInvitationString != null ;
			}
			
			// if currently resumed, do it now.
			if ( mResumed && mReceiveInvitationStringOnResume ) {
				mThread.queue_receiveInvitation( mInvitationString, getNextDelay() ) ;
				mInvitationString = null ;
				mReceiveInvitationStringOnResume = false ;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
		}
	}

	@Override
	public synchronized void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupQuantroActivity(QUANTRO_ACTIVITY_MENU,
				QUANTRO_ACTIVITY_CONTENT_FULL);

		Log.d(TAG, "onCreate");

		if ( savedInstanceState != null ) {
			if ( savedInstanceState.containsKey(BUNDLE_EXTRA_MIN_VERSION) ) {
				this.mMinVersionCode = Math.max(mMinVersionCode, savedInstanceState.getInt(BUNDLE_EXTRA_MIN_VERSION));
			}
			this.mShowedIntro = savedInstanceState.getBoolean(BUNDLE_EXTRA_SHOWED_INTRO, mShowedIntro);
		}
		
		// Force portrait layout
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;

		// Set initial values for important stuff.
		// Dialog manager: we store interactive, non-"failed" dialogs in tier 1.
		// Everything else
		// is tier 0.
		mDialogManager = new DialogManager(this, new int[][] { new int[0],
				DIALOG_TIER_INTERACTIVE_IDS, DIALOG_TIER_EXAMINATION_IDS,
				DIALOG_TIER_QUITTING_IDS });

		// ArrayLists for storing LobbyDescriptions.
		mInternetLobbies = new ArrayList<InternetLobby>();
		mHiddenNonces = new ArrayList<Nonce>();

		// LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
		setContentView(R.layout.internet_lobby_manager_strip_view);
		
		// Get a reference to the main view.
		// challengeManagerView = (ChallengeManagerView)
		// findViewById(R.id.challenge_manager_view_hack) ;
		mInternetLobbyManagerView = (InternetLobbyManagerView) findViewById(R.id.internet_lobby_manager_strip_view);
		mInternetLobbyManagerView.setDelegate(this);

		// Refresh on resume.
		mRefreshLobbyListOnResume = true;
		mMustRefreshLobbyListOnResume = false;
		mInternetLobbiesLastRefreshAt = 0;

		// Our examine dialog refresher.
		mExamineLobbyDialogRefreshRunnable = new Runnable() {
			@Override
			public void run() {
				// first: remove all references.
				mHandler.removeCallbacks(this);

				// second: if not examining or not resumed, that's it.
				if (!mResumed && !mExamineLobbyDialogShown)
					return;

				// third: queue a lobby refresh.
				if (mThread != null) {
					mThread.queue_refreshLobby(mDialogLobby, 0);
				}

				// fourth, repost ourselves.
				mHandler.postDelayed(this, REFRESH_EXAMINED_LOBBY_EVERY);
			}
		};

		res = getResources();

		mHandler = new Handler();
		
		
		
		
		////////////////////////////////////////////////////////////////////////
		// TEST INVITATION PROCESSING
		/*
		try {
			Uri data = getIntent().getData() ;
			if ( data == null )
				Toast.makeText(this, "Intent data is null", Toast.LENGTH_LONG).show() ;
			else {
				String scheme = data.getScheme() ;
				String host = data.getHost() ;
				String path = data.getPath() ;
				List<String> params = data.getPathSegments() ;
				String query = data.getQuery() ;
				String datastr = "scheme:" + scheme + " " ;
				datastr = datastr + "host:" + host + " " ;
				datastr = datastr + "path:" + path + " " ;
				for ( int i = 0; i < params.size(); i++ )
					datastr = datastr + "path" + i + ":"  + params.get(i) + " " ;
				datastr = datastr + "query:" + query ;
				Toast.makeText(this, "Intent data is " + datastr, Toast.LENGTH_LONG).show() ;
				Log.d(TAG, "Intent data is " + datastr) ;
			}
		} catch (Exception e) {
			e.printStackTrace() ;
			Toast.makeText(this, "Exception getting URI data", Toast.LENGTH_LONG).show() ;
		}
		*/
		
		
		// invitation!
		mReceiveInvitationStringOnResume = false ;
		mInvitationString = null ;
		try {
			Uri data = getIntent().getData() ;
			if ( data != null ) {
				mInvitationString = data.getQuery() ;
				mReceiveInvitationStringOnResume = mInvitationString != null ;
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
	protected synchronized void onStart() {
		super.onStart();

		Log.d(TAG, "onStart");

		// start up our worker thread...
		mThread = new InternetLobbyManagerWorkerThread();
		mThread.start();

		// start up our view...
		mInternetLobbyManagerView.setHiddenLobbies(mHiddenNonces.size() > 0) ;
		mInternetLobbyManagerView.start();
	}

	@Override
	protected synchronized void onResume() {
		super.onResume();

		mResumed = true;

		Log.d(TAG, "onResume");

		// Should we refresh our lobby list?

		// Retrieve sound pool and sound controls
		mSoundPool = ((QuantroApplication) getApplication()).getSoundPool(this);
		mSoundControls = QuantroPreferences.getSoundControls(this);
		// Provide to lobby manager view
		mInternetLobbyManagerView.setSoundPool(mSoundPool);
		mInternetLobbyManagerView.setSoundControls(mSoundControls);

		// Reveal any previously hidden dialogs.
		mDialogManager.revealDialogs();
		
		if ( mReceiveInvitationStringOnResume ) {
			mThread.queue_receiveInvitation(mInvitationString, getNextDelay()) ;
			mReceiveInvitationStringOnResume = false ;
			mInvitationString = null ;
		}

		if (mRefreshLobbyListOnResume
				|| mMustRefreshLobbyListOnResume
				|| System.currentTimeMillis() - mInternetLobbiesLastRefreshAt > REFRESH_LIST_ON_RESUME_AFTER) {
			if (mDialogManager.isShowingDialog()
					&& !mMustRefreshLobbyListOnResume)
				mRefreshLobbyListOnResume = false;
			else {
				mThread.queue_refreshList(getNextDelay());
				mRefreshLobbyListOnResume = false;
				mMustRefreshLobbyListOnResume = false;
			}
		}

		if (this.mExamineLobbyDialogShown) {
			mHandler.post(mExamineLobbyDialogRefreshRunnable);
		}

		// Show an intro if appropriate
		if (!mShowedIntro) {
			mDialogManager.showDialog(DIALOG_ID_INTRO);
			mShowedIntro = true;
		}
	}
	
	protected void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(BUNDLE_EXTRA_MIN_VERSION, mMinVersionCode) ;
		outState.putBoolean(BUNDLE_EXTRA_SHOWED_INTRO, mShowedIntro);
	}

	@Override
	protected synchronized void onPause() {
		super.onPause();

		mResumed = false;
	}
	
	@Override
	public int getActivityTypeForMusic() {
		// we use lobby music
		return QUANTRO_ACTIVITY_LOBBY ;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			// We might put up a dialog; we might just quit.
			if (mSoundControls)
				mSoundPool.menuButtonBack();
			if (mThread != null && mThread.isAlive())
				mThread.queue_stop(true);
			else
				finish();
			return true;
		}

		return super.onKeyDown(keyCode, event);
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
        	id = R.menu.lobby_manager_internet_overflow_normal ;
        	break ;
    	case VersionSafe.SCREEN_SIZE_LARGE:
    		id = R.menu.lobby_manager_internet_overflow_large ;
        	break ;	
    	case VersionSafe.SCREEN_SIZE_XLARGE:
    		id = R.menu.lobby_manager_internet_overflow_xlarge ;
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
    			mHiddenNonces.size() > 0 ) ;
    	
    	return true ;
    }
	
	public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent ;
    	switch( item.getItemId() ) {
    	case R.id.overflow_options_menu_unhide:
    		Toast.makeText(this, R.string.lobby_manager_toast_unhide_lobbies, Toast.LENGTH_LONG).show() ;
    		mHiddenNonces.clear();
			mThread.queue_refreshList(getNextDelay());
			return true;
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

	// //////////////////////////////////////////////////////////////////////////
	//
	// Activity Result listener (making a new challenge)

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ( super.handleActivityResult(requestCode, resultCode, data) )
    		return ;
    	
		
		if (resultCode == RESULT_OK) {

			Bundle extras = data.getExtras();

			switch (requestCode) {
			case IntentForResult.REQUEST_NEW_INTERNET_LOBBY:
				// Check for the appropriate extra.
				if (data.hasExtra(NewInternetLobbyActivity.INTENT_EXTRA_LOBBY)) {
					MutableInternetLobby l = (MutableInternetLobby) extras
							.get(NewInternetLobbyActivity.INTENT_EXTRA_LOBBY);
					if (l != null ) {
						// If an itinerant lobby, put it in our database.
						if ( l.isItinerant() ) {
							InternetLobbyDatabaseAdapter dba = new InternetLobbyDatabaseAdapter(this) ;
							try {
								dba.open() ;
								if ( dba.hasLobby(l) )
									dba.updateLobby(l) ;
								else
									dba.insertLobby(l) ;
							} catch ( Exception e ) {
								Toast.makeText(
										this,
										R.string.internet_lobby_manager_toast_database_failure,
										Toast.LENGTH_SHORT).show() ;
							} finally {
								dba.close() ;
							}
						}
						// enter this lobby as a host.
						hostLobby(l);
					}
				} else if ( data.hasExtra(NewInternetLobbyActivity.INTENT_FORCE_REFRESH) ) {
					if ( data.getBooleanExtra(NewInternetLobbyActivity.INTENT_FORCE_REFRESH, true) ) {
						if ( mResumed )
							mThread.queue_refreshList(getNextDelay()) ;
						else
							this.mMustRefreshLobbyListOnResume = true ;
					}
				}
				break;
			}

		} else {
			// gracefully handle failure
			Log.w(TAG, "Warning: activity result not ok");
		}
	}

	//
	// //////////////////////////////////////////////////////////////////////////

	@Override
	protected Dialog onCreateDialog(int id) {
		
		if ( GlobalDialog.hasDialog(id) ) {
			return GlobalDialog.onCreateDialog(this, id, mDialogManager) ;
		}
		
		ProgressDialog pd;
		AlertDialog.Builder builder;

		int display = TextFormatting.DISPLAY_DIALOG;
		int type;
		int role = TextFormatting.ROLE_CLIENT; // irrelevant

		View layout; // if we use a custom layout, we put it here.

		int typePosButton;
		int typeNeutButton;
		int typeNegButton;

		res = getResources();

		switch (id) {
		case DIALOG_ID_APP_OUT_OF_DATE:
			// Let's use "negative" for cancel, as a basic convention, since it
			// will be displayed last.
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_app_out_of_date_title);
			builder.setMessage(R.string.internet_lobby_manager_app_out_of_date_message);
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert (and quits)
			builder.setPositiveButton(
					R.string.internet_lobby_manager_app_out_of_date_update_yes_button,
					res.getColor(R.color.internet_lobby_refresh),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// Dismiss the dialog, quit the LobbyManager, and 
							// start a Google Play activite to update the app.
							mDialogManager.dismissDialog(DIALOG_ID_APP_OUT_OF_DATE) ;
							Intent intent = new Intent(Intent.ACTION_VIEW ,Uri.parse("market://details?id=com.peaceray.quantro"));
							startActivity(intent);  
							mThread.queue_stop(true) ;
						}
					});
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_refresh_failed_communication_button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// Dismiss the dialog, and quit the lobby manager.
							mDialogManager.dismissDialog(DIALOG_ID_APP_OUT_OF_DATE) ;
							mThread.queue_stop(true) ;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_APP_OUT_OF_DATE) ;
					mThread.queue_stop(true) ;
				}
			});
			return builder.create();
			
	
		case DIALOG_ID_GETTING_APP_VERSION:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(R.string.internet_lobby_manager_dialog_getting_app_version_message);
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					InternetLobbyManagerActivity.this.quit();
				}
			});
			return pd;
			
			
		case DIALOG_ID_GETTING_APP_VERSION_NO_RESPONSE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_dialog_getting_app_version_no_response_title);
			builder.setMessage(R.string.internet_lobby_manager_dialog_getting_app_version_no_response_message);
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_getting_app_version_no_response_button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_ID_GETTING_APP_VERSION_NO_RESPONSE);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager
							.dismissDialog(DIALOG_ID_GETTING_APP_VERSION_NO_RESPONSE);
				}
			});
			return builder.create();


		case DIALOG_ID_QUITTING:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(R.string.internet_lobby_manager_dialog_quitting_message); // Message
																					// is
																					// set
																					// in
																					// onPrepare.
																					// Using
																					// ""
																					// resolves
																					// a
																					// display
																					// bug.
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					InternetLobbyManagerActivity.this.quit();
				}
			});
			return pd;

		case DIALOG_ID_INTRO:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_dialog_intro_title);
			builder.setMessage(R.string.internet_lobby_manager_dialog_intro_message);
			builder.setCancelable(false); // cancelable so "back" button
			// dismisses alert
			builder.setPositiveButton(
					R.string.internet_lobby_manager_dialog_intro_button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager.dismissDialog(DIALOG_ID_INTRO);
						}
					});
			return builder.create();

		case DIALOG_ID_REFRESHING_LIST:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(R.string.internet_lobby_manager_dialog_refreshing_message); // Message
																						// is
																						// set
																						// in
																						// onPrepare.
																						// Using
																						// ""
																						// resolves
																						// a
																						// display
																						// bug.
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					InternetLobbyManagerActivity.this.quit();
				}
			});
			return pd;

		case DIALOG_ID_REFRESHING_LIST_FAILED_COMMUNICATION:
			// Let's use "negative" for cancel, as a basic convention, since it
			// will be displayed last.
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_dialog_refresh_failed_communication_title);
			builder.setMessage(R.string.internet_lobby_manager_dialog_refresh_failed_communication_message); // Message
																												// is
																												// set
																												// in
																												// onPrepareDialog.
																												// Using
																												// ""
																												// resolves
																												// a
																												// display
																												// bug.
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setPositiveButton(
					R.string.internet_lobby_manager_dialog_refresh_failed_communication_button_retry,
					res.getColor(R.color.internet_lobby_refresh),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_REFRESHING_LIST_FAILED_COMMUNICATION); // dismiss
																									// the
																									// "failed"
																									// message
							mThread.queue_refreshList(getNextDelay());
						}
					});
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_refresh_failed_communication_button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_REFRESHING_LIST_FAILED_COMMUNICATION); // dismiss
																									// the
																									// "failed"
																									// message
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager
							.dismissDialog(DIALOG_ID_REFRESHING_LIST_FAILED_COMMUNICATION);
				}
			});
			return builder.create();

		case DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE:
			// Let's use "negative" for cancel, as a basic convention, since it
			// will be displayed last.
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_dialog_refresh_failed_database_title);
			builder.setMessage(R.string.internet_lobby_manager_dialog_refresh_failed_database_message); // Message
																										// is
																										// set
																										// in
																										// onPrepareDialog.
																										// Using
																										// ""
																										// resolves
																										// a
																										// display
																										// bug.
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setPositiveButton(
					R.string.internet_lobby_manager_dialog_refresh_failed_database_button_fix,
					res.getColor(R.color.internet_lobby_refresh),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE); // dismiss
																								// the
																								// "failed"
																								// message
							mThread.queue_clearDatabase(0);
							mThread.queue_refreshList(getNextDelay());
						}
					});
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_refresh_failed_database_button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE); // dismiss
																								// the
																								// "failed"
																								// message
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager
							.dismissDialog(DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE);
				}
			});
			return builder.create();

		case DIALOG_ID_CONFIRM_JOIN_LOBBY:
			throw new IllegalArgumentException(
					"DIALOG_ID_CONFIRM_JOIN_LOBBY is not implemented.");

		case DIALOG_ID_CONFIRM_HIDE_LOBBY:
			typePosButton = TextFormatting.TYPE_LOBBY_CONFIRM_HIDE_YES_BUTTON;
			typeNegButton = TextFormatting.TYPE_LOBBY_CONFIRM_HIDE_NO_BUTTON;
			builder = new AlertDialog.Builder(this);
			builder.setMessage(""); // Message is set in onPrepareDialog. Using
									// "" resolves a display bug.
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setPositiveButton(TextFormatting.format(this, res, display,
					typePosButton, role, (InternetLobby) mDialogLobby), res
					.getColor(R.color.lobby_hide),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_CONFIRM_HIDE_LOBBY);
							mThread.queue_hideLobby(mDialogLobby,
									getNextDelay());
							mDialogLobby = null;
							// don't change
							// waitingForWorkingThreadToStartAction; we wait for
							// the thread to start working
						}
					});
			builder.setNegativeButton(TextFormatting.format(this, res, display,
					typeNegButton, role, (InternetLobby) mDialogLobby),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_CONFIRM_HIDE_LOBBY);
							mDialogLobby = null;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_CONFIRM_HIDE_LOBBY);
					mDialogLobby = null;
				}
			});
			return builder.create();

		case DIALOG_ID_EXAMINE_LOBBY:
			// We need references to most everything here, so...
			builder = new AlertDialog.Builder(this);
			builder.setView(R.layout.examine_internet_lobby_layout);
			// cancelable!
			builder.setCancelable(true);
			// buttons!
			builder.setPositiveButton(
					R.string.internet_lobby_examine_lobby_button_join,
					res.getColor(R.color.lobby_join),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_EXAMINE_LOBBY);
							mExamineLobbyDialogShown = false;
							if ( mExamineLobbyDialogStatusOnLastRefresh == WebConsts.STATUS_OPEN ) {
								joinLobbyImmediately(mDialogLobby) ;
							} else if ( mExamineLobbyDialogStatusOnLastRefresh == WebConsts.STATUS_CLOSED || mExamineLobbyDialogStatusOnLastRefresh == WebConsts.STATUS_REMOVED ) {
								if ( mDialogLobby.isItinerant() )
									rehostLobby(mDialogLobby) ;		// rehost
								else {
									mThread.queue_hideLobby(mDialogLobby, getNextDelay()) ;
								}
							}
							mDialogLobby = null;
							// don't change
							// waitingForWorkingThreadToStartAction; we wait for
							// the thread to start working
						}
					});
			builder.setNegativeButton(
					R.string.internet_lobby_examine_lobby_button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_EXAMINE_LOBBY);
							mExamineLobbyDialogShown = false;
							mDialogLobby = null;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_EXAMINE_LOBBY);
					mExamineLobbyDialogShown = false;
					mDialogLobby = null;
				}
			});
			mExamineLobbyDialog = builder.create();
			mExamineLobbyDialogContent = mExamineLobbyDialog.getContentView();
			mExamineLobbyDialogContentViews = new View[NUM_EXAMINE_LOBBY_DIALOG_ELEMENTS];
			// get those views!

			mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_NAME] = mExamineLobbyDialogContent
					.findViewById(R.id.examine_internet_lobby_name);
			mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_DESCRIPTION] = mExamineLobbyDialogContent
					.findViewById(R.id.examine_internet_lobby_description);
			mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_CREATED] = mExamineLobbyDialogContent
					.findViewById(R.id.examine_internet_lobby_created);
			mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_HOSTING] = mExamineLobbyDialogContent
					.findViewById(R.id.examine_internet_lobby_population);
			mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_QUERY] = mExamineLobbyDialogContent
					.findViewById(R.id.examine_internet_lobby_action_query);
			mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_BOTTOM_DIVIDER] = mExamineLobbyDialogContent
					.findViewById(R.id.examine_internet_lobby_bottom_divider);
			
			// that's it. Everything else is updated in a dedicated method for
			// it.
			return mExamineLobbyDialog;
			
			
			
			
		case DIALOG_ID_REFRESHING_FOR_JOIN:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true);
			pd.setMessage(R.string.internet_lobby_manager_dialog_refresh_for_join_message); 
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					InternetLobbyManagerActivity.this.quit();
				}
			});
			return pd;
			
			
		case DIALOG_ID_REFRESHING_FOR_JOIN_FAILURE_COMMUNICATION:
			builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.internet_lobby_manager_dialog_refresh_for_join_failure_communication_message);
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_refresh_for_join_failure_communication_button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_REFRESHING_FOR_JOIN_FAILURE_COMMUNICATION);
							mDialogLobby = null;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_REFRESHING_FOR_JOIN_FAILURE_COMMUNICATION);
					mDialogLobby = null;
				}
			});
			return builder.create();
			
			
		case DIALOG_ID_RECEIVING_INVITATION:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true);
			pd.setMessage(R.string.internet_lobby_manager_dialog_receiving_invitation); 
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					InternetLobbyManagerActivity.this.quit();
				}
			});
			return pd;
			
		case DIALOG_ID_RECEIVING_INVITATION_FAILED_MALFORMED:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_dialog_receiving_invitation_failed_title) ;
			builder.setMessage(R.string.internet_lobby_manager_dialog_receiving_invitation_failed_malformed_message) ;
			
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_receiving_invitation_failed_button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_MALFORMED);
							mDialogLobby = null;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_MALFORMED);
					mDialogLobby = null;
				}
			});
			return builder.create();
			
		case DIALOG_ID_RECEIVING_INVITATION_FAILED_COMMUNICATION:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_dialog_receiving_invitation_failed_title) ;
			builder.setMessage(R.string.internet_lobby_manager_dialog_receiving_invitation_failed_communication_message) ;
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_receiving_invitation_failed_button_ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_COMMUNICATION);
							mDialogLobby = null;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_COMMUNICATION);
					mDialogLobby = null;
				}
			});
			return builder.create();
			
		case DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE:
			// Let's use "negative" for cancel, as a basic convention, since it
			// will be displayed last.
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.internet_lobby_manager_dialog_receiving_invitation_failed_title) ;
			builder.setMessage(R.string.internet_lobby_manager_dialog_receiving_invitation_failed_database_message);
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses alert
			builder.setPositiveButton(
					R.string.internet_lobby_manager_dialog_receiving_invitation_failed_database_button_fix,
					res.getColor(R.color.internet_lobby_refresh),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE);
							mThread.queue_clearDatabase(0);
							mThread.queue_refreshList(getNextDelay());
						}
					});
			builder.setNegativeButton(
					R.string.internet_lobby_manager_dialog_receiving_invitation_failed_database_button_cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							mDialogManager
									.dismissDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE); 
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					mDialogManager
							.dismissDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE);
				}
			});
			return builder.create();
			
		}

		return null;

	}

	@Override
	synchronized protected void onPrepareDialog(int id, Dialog dialog) {
		if ( GlobalDialog.hasDialog(id) ) {
			GlobalDialog.onPrepareDialog(this, id, dialog) ;
			return ;
		}
		
		// THIS METHOD IS DEPRECIATED
		// (and I don't care! the DialogFragment class will call through
		// to this method for compatibility)

		// display, role for DialogFormatting
		int display = TextFormatting.DISPLAY_DIALOG;
		int type;
		int role = TextFormatting.ROLE_CLIENT; // irrelevant

		String text;
		switch (id) {
		case DIALOG_ID_APP_OUT_OF_DATE:
			// nothing...
			return ;
			
		case DIALOG_ID_GETTING_APP_VERSION:
			// nothing...
			return ;
			
		case DIALOG_ID_GETTING_APP_VERSION_NO_RESPONSE:
			// nothing...
			return ;

		case DIALOG_ID_QUITTING:
			// nothing...
			return;

		case DIALOG_ID_REFRESHING_LIST:
			// nothing...
			return;

		case DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE:
			// nothing...
			return;

		case DIALOG_ID_CONFIRM_JOIN_LOBBY:
			throw new IllegalArgumentException(
					"DIALOG_ID_CONFIRM_JOIN_LOBBY is not implemented.");

		case DIALOG_ID_CONFIRM_HIDE_LOBBY:
			type = TextFormatting.TYPE_LOBBY_CONFIRM_HIDE;
			text = TextFormatting.format(this, res, display, type, role,
					mDialogLobby);
			((AlertDialog) dialog).setMessage(text);
			break;

		case DIALOG_ID_EXAMINE_LOBBY:
			mExamineLobbyDialogShown = true;
			refreshExamineLobbyDialog();
			// start up the lobby refreshing runnable.
			mHandler.post(mExamineLobbyDialogRefreshRunnable);
			break;
			
		case DIALOG_ID_REFRESHING_FOR_JOIN:
			break ;
			
		case DIALOG_ID_REFRESHING_FOR_JOIN_FAILURE_COMMUNICATION:
			break ;
			
		case DIALOG_ID_RECEIVING_INVITATION:
			break ;
			
		case DIALOG_ID_RECEIVING_INVITATION_FAILED_MALFORMED:
			break ;
			
		case DIALOG_ID_RECEIVING_INVITATION_FAILED_COMMUNICATION:
			break ;
			
		case DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE:
			break ;

		default:
			Log.d(TAG, "Cannot prepare dialog; no dialog case with id: " + id);
			return;
		}
	}

	public void quit() {
		if (threadIsWorking())
			mDialogManager.showDialog(DIALOG_ID_QUITTING);
		else
			mDialogManager.dismissAllDialogs();
		mThread.running = false;
		if (mThread.isAlive())
			mThread.queue_stop(true);
		else
			finish();
	}

	private synchronized boolean threadIsWorking() {
		return false;
	}

	private synchronized int getNextDelay() {
		long currentTime = System.currentTimeMillis();
		int diff = (int) (currentTime - mLastActionTime);
		mLastActionTime = currentTime;
		return Math.max(0, MINIMUM_DELAY - diff);
	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// Methods to perform certain functions. methods.
	//
	
	private void appOutOfDate() {
		mDialogManager.showDialog(DIALOG_ID_APP_OUT_OF_DATE) ;
	}

	private void createLobby() {
		if ( mMinVersionCode == -1 )
			mThread.queue_getMinAppVersionIfUnknown(getNextDelay()) ;
		else if ( mMinVersionCode > AppVersion.code(this) )
			appOutOfDate() ;
		else {
			// An "Intent for action" so we know whether
			// a new challenge was sent (and can then conditionally refresh).
			Intent intent = new Intent(this, NewInternetLobbyActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
	
			startActivityForResult(intent,
					IntentForResult.REQUEST_NEW_INTERNET_LOBBY);
		}
	}
	
	
	private void rehostLobby( InternetLobby il ) {
		if ( mMinVersionCode == -1 )
			mThread.queue_getMinAppVersionIfUnknown(getNextDelay()) ;
		else if ( mMinVersionCode > AppVersion.code(this) )
			appOutOfDate() ;
		else {
			// An "Intent for action" so we know whether
			// a new challenge was sent (and can then conditionally refresh).
			// Toast.makeText(this, "rehostLobby( " + il + ")", Toast.LENGTH_SHORT).show() ;
			Intent intent = new Intent(this, NewInternetLobbyActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			intent.putExtra(NewInternetLobbyActivity.INTENT_EXTRA_LOBBY, il) ;
	
			startActivityForResult(intent,
					IntentForResult.REQUEST_NEW_INTERNET_LOBBY);
		}
	}

	private void hostLobby(MutableInternetLobby lobby) {
		// join the provided lobby as a client.
		if (lobby == null)
			return;

		if ( mMinVersionCode == -1 )
			mThread.queue_getMinAppVersionIfUnknown(getNextDelay()) ;
		else if ( mMinVersionCode > AppVersion.code(this) )
			appOutOfDate() ;
		else {
			// TODO: perform the refresh. It's more convenient for the user if
			// we check for a new altered status now, rather than waiting to attempt
			// to join a lobby.
	
			Toast.makeText(this,
					R.string.menu_internet_lobby_manager_toast_host_lobby,
					Toast.LENGTH_LONG).show();
	
			
			// Launch as an ASYNC TASK.  Android is strict about network access
			// in the UI thread, and constructing an InetSocketAddress counts.
			new AsyncTask<MutableInternetLobby, Object, LobbyIntentPackage>()  {
				@Override
				protected LobbyIntentPackage doInBackground(MutableInternetLobby... params) {
					MutableInternetLobby lobby = params[0] ;
					LobbyIntentPackage lip;
					lip = new LobbyIntentPackage(
							((QuantroApplication) getApplicationContext()).personalNonce,
							QuantroPreferences.getMultiplayerName(InternetLobbyManagerActivity.this),
							lobby.getLobbyNonce(), lobby.getLobbyName(),
							lobby.getMaxPeople());

					SocketAddress mediatorSockAddr = new InetSocketAddress("peaceray.com", lobby.getMediatorPort());
					lip.setAsMatchseekerHost(mediatorSockAddr, lobby);
					return lip ;
				}
				
				protected void onPostExecute (LobbyIntentPackage lip) {
					// Launch!
					mMustRefreshLobbyListOnResume = true ;
					launchLobbyIntent( lip ) ;
				}
			}.execute(lobby) ;
		}
	}

	/**
	 * Join the provided lobby as a client.
	 * 
	 * @param lobby
	 */
	private void joinLobby(InternetLobby lobby) {
		// join the provided lobby as a client.
		if (lobby == null)
			return;

		if ( mMinVersionCode == -1 )
			mThread.queue_getMinAppVersionIfUnknown(getNextDelay()) ;
		else if ( mMinVersionCode > AppVersion.code(this) )
			appOutOfDate() ;
		else {
			if (lobby.getHostedPlayers() == lobby.getMaxPeople()) {
				Toast.makeText(
						this,
						R.string.menu_internet_lobby_manager_toast_join_fail_lobby_full,
						Toast.LENGTH_SHORT).show();
				return;
			}
	
			if (lobby.getStatus() == WebConsts.STATUS_CLOSED) {
				Toast.makeText(
						this,
						R.string.menu_internet_lobby_manager_toast_join_fail_lobby_closed,
						Toast.LENGTH_SHORT).show();
				return;
			}
	
			if (lobby.getStatus() == WebConsts.STATUS_REMOVED) {
				Toast.makeText(
						this,
						R.string.menu_internet_lobby_manager_toast_join_fail_lobby_removed,
						Toast.LENGTH_SHORT).show();
				return;
			}
	
			// perform the refresh. It's more convenient for the user if
			// we check for a new altered status now, rather than waiting to attempt
			// to join a lobby.
			if ( lobby.getLastRefreshTimeSince() > MAXIMUM_TIME_SINCE_REFRESH_FOR_JOIN
					|| lobby.getStatus() == WebConsts.STATUS_EMPTY ) {
				mThread.queue_refreshLobbyForJoin(lobby, getNextDelay()) ;
				return ;
			}
	
			Toast.makeText(this,
					R.string.menu_internet_lobby_manager_toast_join_lobby,
					Toast.LENGTH_LONG).show();
			
			// Launch as an ASYNC TASK.  Android is strict about network access
			// in the UI thread, and constructing an InetSocketAddress counts.
			new AsyncTask<InternetLobby, Object, LobbyIntentPackage>()  {
				@Override
				protected LobbyIntentPackage doInBackground(InternetLobby... params) {
					InternetLobby lobby = params[0] ;
					LobbyIntentPackage lip;
					lip = new LobbyIntentPackage(
							((QuantroApplication) getApplicationContext()).personalNonce,
							QuantroPreferences.getMultiplayerName(InternetLobbyManagerActivity.this),
							lobby.getLobbyNonce(), lobby.getLobbyName(),
							lobby.getMaxPeople());
					SocketAddress mediatorSockAddr = new InetSocketAddress("peaceray.com",
							lobby.getMediatorPort());
					lip.setAsMatchseekerClient(mediatorSockAddr, lobby);
					
					return lip ;
				}
				
				protected void onPostExecute (LobbyIntentPackage lip) {
					// Launch!
					launchLobbyIntent( lip ) ;
				}
			}.execute(lobby) ;
		}
	}
	
	private void joinLobbyImmediately( InternetLobby lobby ) {
		// join the provided lobby as a client.
		if (lobby == null)
			return;

		if ( mMinVersionCode == -1 )
			mThread.queue_getMinAppVersionIfUnknown(getNextDelay()) ;
		else if ( mMinVersionCode > AppVersion.code(this) )
			appOutOfDate() ;
		else {
			if (lobby.getHostedPlayers() == lobby.getMaxPeople()) {
				Toast.makeText(
						this,
						R.string.menu_internet_lobby_manager_toast_join_fail_lobby_full,
						Toast.LENGTH_SHORT).show();
				return;
			}
	
			if (lobby.getStatus() == WebConsts.STATUS_CLOSED) {
				Toast.makeText(
						this,
						R.string.menu_internet_lobby_manager_toast_join_fail_lobby_closed,
						Toast.LENGTH_SHORT).show();
				return;
			}
	
			if (lobby.getStatus() == WebConsts.STATUS_REMOVED) {
				Toast.makeText(
						this,
						R.string.menu_internet_lobby_manager_toast_join_fail_lobby_removed,
						Toast.LENGTH_SHORT).show();
				return;
			}
	
			Toast.makeText(this,
					R.string.menu_internet_lobby_manager_toast_join_lobby,
					Toast.LENGTH_LONG).show();
	
			
			// Launch as an ASYNC TASK.  Android is strict about network access
			// in the UI thread, and constructing an InetSocketAddress counts.
			new AsyncTask<InternetLobby, Object, LobbyIntentPackage>()  {
				@Override
				protected LobbyIntentPackage doInBackground(InternetLobby... params) {
					InternetLobby lobby = params[0] ;
					LobbyIntentPackage lip;
					lip = new LobbyIntentPackage(
							((QuantroApplication) getApplicationContext()).personalNonce,
							QuantroPreferences.getMultiplayerName(InternetLobbyManagerActivity.this),
							lobby.getLobbyNonce(), lobby.getLobbyName(),
							lobby.getMaxPeople());
					SocketAddress mediatorSockAddr = new InetSocketAddress("peaceray.com",
							lobby.getMediatorPort());
					lip.setAsMatchseekerClient(mediatorSockAddr, lobby);
					
					return lip ;
				}
				
				protected void onPostExecute (LobbyIntentPackage lip) {
					// Launch!
					launchLobbyIntent( lip ) ;
				}
			}.execute(lobby) ;
		}
	}
	
	private void launchLobbyIntent ( LobbyIntentPackage lip ) {
		if ( mMinVersionCode == -1 )
			mThread.queue_getMinAppVersionIfUnknown(getNextDelay()) ;
		else if ( mMinVersionCode > AppVersion.code(this) )
			appOutOfDate() ;
		else {
			// Launch!  Give the LIP and a "ClearTop" flag.
			// In most operation, CLEAR_TOP will have no effect (the Lobby Activity)
			// will NOT be running on the stack.
			// However, in a few cases -- such as when we are in a lobby or MP game
			// and receive an Android Beam'd invitation -- there may already be
			// a LobbyActivity.  In this case, we want to clear all Activities
			// over it (including this activity, and any ongoing Games).  If you didn't
			// want to quit your game WHY DID YOU ACCEPT ANOTHER INVITATION???
			Intent intent = new Intent(this, LobbyActivity.class);
			intent.setAction(Intent.ACTION_MAIN);
			intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP ) ;
			intent.putExtra(
					getResources().getString(
							R.string.intent_extra_lobby_intent_package), lip);
	
			mMustRefreshLobbyListOnResume = true ;
			startActivity(intent);
		}
	}
	
	
	public void examineLobby( InternetLobby lobby ) {
		mDialogLobby = lobby ;
		mDialogManager.showDialog(DIALOG_ID_EXAMINE_LOBBY) ;
	}
	

	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// InternetLobbyManagerView.Delegate methods.
	//

	@Override
	public void ilmvd_createNewInternetLobby(InternetLobbyManagerView view) {
		createLobby();
	}

	@Override
	public void ilmvd_joinInternetLobby(InternetLobby lobby,
			InternetLobbyManagerView view) {
		
		int status = lobby.getStatus() ;
		if ( lobby.isItinerant() && ( status == WebConsts.STATUS_CLOSED || status == WebConsts.STATUS_REMOVED ) ) {
			// examine!
			examineLobby(lobby) ;
		} else if (mShouldConfirmJoin) {
			mDialogLobby = lobby;
			mDialogManager.showDialog(DIALOG_ID_CONFIRM_JOIN_LOBBY);
		} else
			joinLobby(lobby);
	}

	@Override
	public void ilmvd_refreshInternetLobbyList(InternetLobbyManagerView view) {
		// tell the worker thread to do this.
		mThread.queue_refreshList(getNextDelay());
	}

	@Override
	public void ilmvd_hideInternetLobby(InternetLobby lobby,
			InternetLobbyManagerView view) {

		// tell the worker thread to do this
		if ( !lobby.isPublic() && ( lobby.isOpen() || lobby.isItinerant() ) && mShouldConfirmHidePrivateNotClosedForever ) {
			mDialogLobby = lobby;
			mDialogManager.showDialog(DIALOG_ID_CONFIRM_HIDE_LOBBY);
		} else if ( lobby.isPublic() && mShouldConfirmHide) {
			mDialogLobby = lobby;
			mDialogManager.showDialog(DIALOG_ID_CONFIRM_HIDE_LOBBY);
		} else {
			mThread.queue_hideLobby(lobby, 0);
		}
	}

	@Override
	public void ilmvd_examineInternetLobby(InternetLobby lobby,
			InternetLobbyManagerView view) {

		examineLobby(lobby) ;
	}
	
	
	@Override
	public void ilmvd_unhideInternetLobbies( InternetLobbyManagerView view ) {
		Toast.makeText(this, R.string.lobby_manager_toast_unhide_lobbies, Toast.LENGTH_LONG).show() ;
		mHiddenNonces.clear();
		mThread.queue_refreshList(getNextDelay());
		view.setHiddenLobbies(false) ;
	}
	
	
	@Override
	public void ilmvd_help( InternetLobbyManagerView view ) {
		mDialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
	}
	
	
	@Override
	public void ilmvd_settings( InternetLobbyManagerView view ) {
		Intent intent = new Intent(this, QuantroPreferences.class);
		intent.setAction(Intent.ACTION_MAIN);
		startActivity(intent);
	}

	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// Update Methods
	//

	private void refreshExamineLobbyDialog() {
		InternetLobby il = mDialogLobby;
		String text;
		int text_id ;
		if (il != null && this.mExamineLobbyDialogShown && mResumed) {
			int color = getExamineLobbyColor( il ) ;
			int status = il.getStatus();
			
			// set default Join button (changes to Host under special circumstances)
			mExamineLobbyDialog.setButtonTitle(Dialog.BUTTON_POSITIVE,
					res.getString(R.string.internet_lobby_examine_lobby_button_join)) ;
			
			// description based on public / private / itinerant
			if ( il.isPublic() )
				text_id = R.string.internet_lobby_examine_lobby_description_public ;
			else if ( il.isItinerant() )
				text_id = R.string.internet_lobby_examine_lobby_description_itinerant ;
			else
				text_id = R.string.internet_lobby_examine_lobby_description_private ;
			text = res.getString(text_id) ;
			((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_DESCRIPTION])
					.setText(TextFormatting.replacePlaceholdersWithLobby(
							this, res, il, text));
			
			switch (status) {
			case WebConsts.STATUS_EMPTY:
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_NAME])
						.setText(R.string.internet_lobby_examine_lobby_name_empty);
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_CREATED])
						.setText(R.string.internet_lobby_examine_lobby_created_empty);
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_HOSTING])
						.setText(null);
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_QUERY])
						.setText(R.string.internet_lobby_examine_lobby_empty_query);
				mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_BOTTOM_DIVIDER].setBackgroundColor(color) ;
				mExamineLobbyDialog.setButtonColor(AlertDialog.BUTTON_POSITIVE,
						color);
				mExamineLobbyDialog.setButtonEnabled(
						AlertDialog.BUTTON_POSITIVE, false);
				break;

			case WebConsts.STATUS_OPEN:
			case WebConsts.STATUS_CLOSED:
				text = res
						.getString(R.string.internet_lobby_examine_lobby_name);
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_NAME])
						.setText(TextFormatting.replacePlaceholdersWithLobby(
								this, res, il, text));
				// 'created' is the same in all cases, except itinerant.
				if ( il.isItinerant() )
					text = res.getString(R.string.internet_lobby_examine_lobby_itinerant_created);
				else
					text = res.getString(R.string.internet_lobby_examine_lobby_created);
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_CREATED])
						.setText(TextFormatting.replacePlaceholdersWithLobby(
								this, res, il, text));
				// 'hosting' only if open
				if (status == WebConsts.STATUS_OPEN) {
					text = res
							.getString(R.string.internet_lobby_examine_lobby_population);
					text = TextFormatting.replacePlaceholdersWithLobby(this,
							res, il, text);
				} else
					text = null;
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_HOSTING])
						.setText(text);
				// query is based on closed / open_full / open_space.
				// color is based on closed / open_invited / open_public
				// button color is based on closed / open_invited / open_public
				if (status == WebConsts.STATUS_CLOSED) {
					// set all three.
					if ( il.isItinerant() )
						text_id = R.string.internet_lobby_examine_lobby_itinerant_closed_query ;
					else
						text_id = R.string.internet_lobby_examine_lobby_closed_query ;
					((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_QUERY])
							.setText(text_id);
					mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_BOTTOM_DIVIDER].setBackgroundColor(color) ;
					mExamineLobbyDialog.setButtonColor(
							AlertDialog.BUTTON_POSITIVE, color);
					
					// set button text to "host" if itinerant.
					if ( il.isItinerant() ) {
						mExamineLobbyDialog.setButtonTitle(AlertDialog.BUTTON_POSITIVE,
								res.getString(R.string.internet_lobby_examine_lobby_button_host)) ;
						mExamineLobbyDialog.setButtonEnabled(AlertDialog.BUTTON_POSITIVE, true) ;
					} else {
						mExamineLobbyDialog.setButtonTitle(AlertDialog.BUTTON_POSITIVE,
								res.getString(R.string.internet_lobby_examine_lobby_button_remove)) ;
						mExamineLobbyDialog.setButtonEnabled(
								AlertDialog.BUTTON_POSITIVE, true);
					}
				} else {
					// query and button activation based on fullness. Color
					// based on invitation.
					if (il.getHostedPlayers() >= il.getMaxPeople()) {
						((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_QUERY])
								.setText(R.string.internet_lobby_examine_lobby_full_query);
						mExamineLobbyDialog.setButtonEnabled(
								AlertDialog.BUTTON_POSITIVE, false);
					} else {
						((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_QUERY])
								.setText(R.string.internet_lobby_examine_lobby_open_query);
						mExamineLobbyDialog.setButtonEnabled(
								AlertDialog.BUTTON_POSITIVE, true);
					}
					mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_BOTTOM_DIVIDER].setBackgroundColor(color) ;
					mExamineLobbyDialog.setButtonColor(
							AlertDialog.BUTTON_POSITIVE, color);
				}
				break;

			case WebConsts.STATUS_REMOVED:
				if ( il.isItinerant() && il.getLobbyName() != null )
					text = res.getString(R.string.internet_lobby_examine_lobby_name) ;
				else
					text = res.getString(R.string.internet_lobby_examine_lobby_name_removed) ;
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_NAME])
						.setText( TextFormatting.replacePlaceholdersWithLobby(this, res, il, text) ) ;
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_CREATED])
						.setText(R.string.internet_lobby_examine_lobby_created_removed);
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_HOSTING])
						.setText(null);
				if ( il.isItinerant() )
					text_id = R.string.internet_lobby_examine_lobby_itinerant_removed_query ;
				else
					text_id = R.string.internet_lobby_examine_lobby_removed_query ;
				((TextView)mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_QUERY])
						.setText(text_id);
				mExamineLobbyDialogContentViews[EXAMINE_LOBBY_DIALOG_BOTTOM_DIVIDER].setBackgroundColor(color) ;
				mExamineLobbyDialog.setButtonColor(AlertDialog.BUTTON_POSITIVE,
						color);
				// set button text to "host" if itinerant.
				if ( il.isItinerant() ) {
					mExamineLobbyDialog.setButtonTitle(AlertDialog.BUTTON_POSITIVE,
							res.getString(R.string.internet_lobby_examine_lobby_button_host)) ;
					mExamineLobbyDialog.setButtonEnabled(AlertDialog.BUTTON_POSITIVE, true) ;
				} else {
					mExamineLobbyDialog.setButtonTitle(AlertDialog.BUTTON_POSITIVE,
							res.getString(R.string.internet_lobby_examine_lobby_button_remove)) ;
					mExamineLobbyDialog.setButtonEnabled(
							AlertDialog.BUTTON_POSITIVE, true);
				}
				break;
			}
			
			mExamineLobbyDialogStatusOnLastRefresh = status ;
		}
	}
	
	
	/**
	 * Returns a color appropriate for use when examining this lobby.
	 * 
	 * Generally speaking, this color is based on public / private / itinerant,
	 * lobby status, and lobby population.
	 * 
	 * @param lobby
	 * @return
	 */
	private int getExamineLobbyColor( InternetLobby lobby ) {
		
		switch ( lobby.getStatus() ) {
		case WebConsts.STATUS_EMPTY:
			// unrefreshed gets a blank color.
			return res.getColor(R.color.internet_lobby_unknown) ;
			
		case WebConsts.STATUS_OPEN:
			// an open lobby is colored based on whether it is full.
			boolean room = lobby.getHostedPlayers() < lobby.getMaxPeople() ;
			if ( lobby.isPublic() )
				return room
						? res.getColor(R.color.internet_lobby_button_strip_public_open_main)
						: res.getColor(R.color.internet_lobby_button_strip_public_full_main) ;
			else if ( lobby.isItinerant() )
				return room
						? res.getColor(R.color.internet_lobby_button_strip_itinerant_open_main)
						: res.getColor(R.color.internet_lobby_button_strip_itinerant_full_main) ;
			else
				return room
						? res.getColor(R.color.internet_lobby_button_strip_private_open_main)
						: res.getColor(R.color.internet_lobby_button_strip_private_full_main) ;
						
		case WebConsts.STATUS_CLOSED:
		case WebConsts.STATUS_REMOVED:
			if ( lobby.isPublic() )
				return res.getColor(R.color.internet_lobby_button_strip_public_closed_main) ;
			else if ( lobby.isItinerant() )
				return res.getColor(R.color.internet_lobby_button_strip_itinerant_closed_main) ;
			else
				return res.getColor(R.color.internet_lobby_button_strip_private_closed_main) ;
			
		}
		
		return res.getColor(R.color.internet_lobby_unknown) ;
		
	}

	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// Update Runnables
	//
	// Useful runnable classes for performing actions on the UI thread.
	//

	private class RunnableSetLobbies implements Runnable {
		ArrayList<InternetLobby> mRunnableLobbies;

		private RunnableSetLobbies(ArrayList<InternetLobby> lobbies) {
			if (lobbies == null)
				mRunnableLobbies = null;
			else
				mRunnableLobbies = (ArrayList<InternetLobby>) lobbies.clone();
		}

		@Override
		public void run() {
			mInternetLobbyManagerView.setLobbies(mRunnableLobbies);
		}
	}

	private class RunnableSetServerResponse implements Runnable {
		int mRunnableResponse;

		private RunnableSetServerResponse(int response) {
			mRunnableResponse = response;
		}

		@Override
		public void run() {
			mInternetLobbyManagerView.setServerResponse(mRunnableResponse);
		}
	}

	private class RunnableRefreshLobby implements Runnable {
		InternetLobby mLobby;

		private RunnableRefreshLobby(InternetLobby l) {
			mLobby = l;
		}

		@Override
		public void run() {
			mInternetLobbyManagerView.refreshView(mLobby);
		}
	}

	private class RunnableRemoveLobby implements Runnable {
		InternetLobby mLobby;

		private RunnableRemoveLobby(InternetLobby l) {
			mLobby = l;
		}

		@Override
		public void run() {
			mInternetLobbyManagerView.removeLobby(mLobby);
			mInternetLobbyManagerView.setHiddenLobbies( mHiddenNonces.size() > 0 ) ;
		}
	}

	//
	// //////////////////////////////////////////////////////////////////////////

	// //////////////////////////////////////////////////////////////////////////
	//
	// The Worker Thread
	//
	// The thread upon which most work is conducted.
	//

	// //////////////////////////////////
	//
	// Status: is, did, failed.

	private void thread_queueStopping() {
		// put up a spinner
		// put up a spinner
		mDialogManager.showDialog(DIALOG_ID_QUITTING);
	}
	
	
	private void thread_queueGettingMinVersionCode() {
		// put up a spinner
		mDialogManager.showDialog(DIALOG_ID_GETTING_APP_VERSION) ;
	}
	
	
	/**
	 * We have received the minimum required version code.
	 * Returns whether we should continue with our operation, whatever it was.
	 * @param code
	 * @return
	 */
	private boolean thread_receivedMinVersionCode( int code ) {
		Log.d(TAG, "thread_receivedMinVersionCode: " + code) ;
		if ( code != -1 )
			mMinVersionCode = code ;
		mDialogManager.dismissDialog(DIALOG_ID_GETTING_APP_VERSION) ;
		if ( mMinVersionCode > -1 && mMinVersionCode > AppVersion.code(this) ) {
			appOutOfDate() ;
			return false ;
		}
		return true ;
	}
	
	private void thread_noResponseMinVersionCode() {
		mDialogManager.dismissDialog(DIALOG_ID_GETTING_APP_VERSION) ;
		mDialogManager.showDialog(DIALOG_ID_GETTING_APP_VERSION_NO_RESPONSE) ;
	}
	

	private void thread_queueRefreshingList() {
		// put up a spinner
		mDialogManager.showDialog(DIALOG_ID_REFRESHING_LIST);
		runOnUiThread(new RunnableSetServerResponse(
				InternetLobbyManagerView.SERVER_RESPONSE_PENDING));
	}

	private void thread_isRefreshingList() {
		// nothing
	}

	private void thread_doneRefreshingList(ArrayList<InternetLobby> newLobbies) {
		// set the new lobbies...
		this.mInternetLobbies = newLobbies;
		runOnUiThread(new RunnableSetLobbies(mInternetLobbies));
		runOnUiThread(new RunnableSetServerResponse(
				InternetLobbyManagerView.SERVER_RESPONSE_RECEIVED));
		mInternetLobbiesLastRefreshAt = System.currentTimeMillis();

		// TODO: start the process of updating the currently displayed and
		// non-displayed
		// lobbies.

		// dismiss the spinner
		mDialogManager.dismissDialog(DIALOG_ID_REFRESHING_LIST);
	}

	private void thread_failedRefreshingList_communication() {
		// dismiss the spinner and let the user know
		mDialogManager.dismissDialog(DIALOG_ID_REFRESHING_LIST);
		mDialogManager
				.showDialog(DIALOG_ID_REFRESHING_LIST_FAILED_COMMUNICATION);
		runOnUiThread(new RunnableSetServerResponse(
				InternetLobbyManagerView.SERVER_RESPONSE_NONE));
	}

	private void thread_failedRefreshingList_database() {
		// dismiss the spinner and let the user know
		mDialogManager.dismissDialog(DIALOG_ID_REFRESHING_LIST);
		mDialogManager.showDialog(DIALOG_ID_REFRESHING_LIST_FAILED_DATABASE);
		runOnUiThread(new RunnableSetServerResponse(
				InternetLobbyManagerView.SERVER_RESPONSE_RECEIVED));
	}

	private void thread_refreshedLobby(InternetLobby lobby) {
		// two effects. First, we refresh it in the InternetLobbyManagerView.
		// second, IF this lobby is currently displayed, we refresh it too.
		int status = lobby.getStatus() ;
		if ( lobby.getOrigin() == InternetLobby.ORIGIN_PUBLIC_LIST && ( status == WebConsts.STATUS_CLOSED || status == WebConsts.STATUS_REMOVED ) )
			runOnUiThread(new RunnableRemoveLobby(lobby)) ;
		else
			runOnUiThread(new RunnableRefreshLobby(lobby));
		if (lobby == this.mDialogLobby && this.mExamineLobbyDialogShown)
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					refreshExamineLobbyDialog();
				}
			});
	}
	
	
	private void thread_isRefreshingLobbyForJoin( InternetLobby lobby ) {
		mDialogManager.showDialog(DIALOG_ID_REFRESHING_FOR_JOIN) ;
	}
	
	private void thread_doneRefreshingLobbyForJoin( InternetLobby lobby ) {
		// remove the spinner and join the lobby -- IF there's room.
		mDialogManager.dismissDialog(DIALOG_ID_REFRESHING_FOR_JOIN) ;
		if ( lobby.getHostedPlayers() < lobby.getMaxPeople() )
			this.joinLobbyImmediately(lobby) ;
		else
			Toast.makeText(
					this,
					R.string.menu_internet_lobby_manager_toast_join_fail_lobby_full,
					Toast.LENGTH_SHORT).show();
	}
	
	private void thread_failedRefreshingLobbyForJoin_status( InternetLobby lobby ) {
		// remove the spinner and tell the user the problem by way of the
		// "examine" dialog.  If not itinerant or invited, we also remove the lobby from
		// the lobby list and the view.
		mDialogManager.dismissDialog(DIALOG_ID_REFRESHING_FOR_JOIN) ;
		

		if ( !lobby.isItinerant() && lobby.getOrigin() == InternetLobby.ORIGIN_PUBLIC_LIST ) {
			Nonce n = lobby.getLobbyNonce() ;
			for ( int i = 0; i < mInternetLobbies.size(); i++ ) {
				InternetLobby l = mInternetLobbies.get(i) ;
				if ( l.getLobbyNonce().equals(n) ) {
					mInternetLobbies.remove(i) ;
					break ;
				}
			}
			runOnUiThread(new RunnableRemoveLobby(lobby));
		}
		
		// .....aaaaaaaand show the examine dialog.
		examineLobby(lobby) ;
	}
	
	private void thread_failedRefreshingLobbyForJoin_communication( InternetLobby lobby, int error, int reason ) {
		mDialogManager.dismissDialog(DIALOG_ID_REFRESHING_FOR_JOIN) ;
		mDialogManager.showDialog(DIALOG_ID_REFRESHING_FOR_JOIN_FAILURE_COMMUNICATION) ;
	}
	
	

	private void thread_hidLobby(InternetLobby lobby) {
		runOnUiThread(new RunnableRemoveLobby(lobby));
	}
	
	
	private void thread_isReceivingInvitation() {
		mDialogManager.showDialog(DIALOG_ID_RECEIVING_INVITATION) ;
	}
	
	private void thread_failedReceivingInvitation_malformed() {
		// drop the spinner; show an error message.
		mDialogManager.dismissDialog(DIALOG_ID_RECEIVING_INVITATION) ;
		mDialogManager.showDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_MALFORMED) ;
			// no dialog lobby
	}
	
	
	private void thread_doneReceivingInvitation( InternetLobby lobby ) {
		// drop the spinner and show an examination dialog.  The
		// lobby has been added to mInternetLobbies, so refresh the view
		// to include it.
		mDialogManager.dismissDialog(DIALOG_ID_RECEIVING_INVITATION) ;
		mDialogLobby = lobby;
		mDialogManager.showDialog(DIALOG_ID_EXAMINE_LOBBY);
		
		runOnUiThread(new RunnableRefreshLobby(lobby));
	}
	
	
	private void thread_failedReceivingInvitation_refresh( InternetLobby lobby ) {
		// drop the spinner.  We failed to refresh the lobby, meaning
		// a communication error occurred or there's something wrong on the
		// server.  Tell the user.  The lobby has been added to mInternetLobbies,
		// so refresh the view to include it.
		mDialogManager.dismissDialog(DIALOG_ID_RECEIVING_INVITATION) ;
		mDialogManager.showDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_COMMUNICATION) ;
		// no dialog lobby
		
		runOnUiThread(new RunnableRefreshLobby(lobby));
	}
	
	private void thread_failedReceivingInvitation_database( InternetLobby lobby ) {
		// drop the spinner.  We failed to add the invitation to the database.
		// Put up the standard "fix database" dialog.
		mDialogManager.dismissDialog(DIALOG_ID_RECEIVING_INVITATION) ;
		mDialogManager.showDialog(DIALOG_ID_RECEIVING_INVITATION_FAILED_DATABASE) ;
		// no dialog lobby
		
		// ALSO: For consistency, we need to either add this lobby to the (fixed) database,
		// or REMOVE the lobby from mInternetLobbies (it has been added for us).
		Nonce n = lobby.getLobbyNonce() ;
		for ( int i = 0; i < mInternetLobbies.size(); i++ ) {
			InternetLobby l = mInternetLobbies.get(i) ;
			if ( l.getLobbyNonce().equals(n) ) {
				mInternetLobbies.remove(i) ;
				break ;
			}
		}
	}
	
	
	

	//
	// //////////////////////////////////

	private class InternetLobbyManagerWorkerThread extends Thread {

		// Here's some constants for our messages to ourself.
		private static final int ANDROID_MESSAGE_TYPE_STOP = 0;
		private static final int ANDROID_MESSAGE_TYPE_GET_MIN_VERSION_CODE_IF_NOT_KNOWN = 1 ;
		private static final int ANDROID_MESSAGE_TYPE_CLEAR_DATABASE = 2;
		private static final int ANDROID_MESSAGE_TYPE_REFRESH_LIST = 3;
		private static final int ANDROID_MESSAGE_TYPE_REFRESH_LOBBY_IN_BACKGROUND = 4;
		private static final int ANDROID_MESSAGE_TYPE_REFRESH_LOBBY_FOR_JOIN = 5;
		private static final int ANDROID_MESSAGE_TYPE_HIDE_LOBBY = 6;
		private static final int ANDROID_MESSAGE_TYPE_RECEIVE_INVITATION = 7 ;

		private static final int NUM_ANDROID_MESSAGE_TYPES = 8;

		public Handler handler = null;
		public int timeout;
		public boolean running;
		public boolean started = false;

		public InternetLobbyManagerWorkerThread() {
			timeout = 5000;
			running = true;
			started = false;
		}

		public void waitUntilReadyToQueue() {
			if (!started)
				throw new IllegalStateException(
						"Must start InternetLobbyManagerWorkerThread before queueing any actions!");

			// Wait for a handler.
			while (running && handler == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}
		
		public void queue_getMinAppVersionIfUnknown(int millisDelay) {
			waitUntilReadyToQueue();
			thread_queueGettingMinVersionCode() ;
			handler.sendMessage(handler.obtainMessage(
					ANDROID_MESSAGE_TYPE_GET_MIN_VERSION_CODE_IF_NOT_KNOWN, 0, millisDelay));
		}

		public void queue_clearDatabase(int millisDelay) {
			waitUntilReadyToQueue();
			handler.sendMessage(handler.obtainMessage(
					ANDROID_MESSAGE_TYPE_CLEAR_DATABASE, 0, millisDelay));
		}

		public void queue_refreshList(int millisDelay) {
			waitUntilReadyToQueue();
			thread_queueRefreshingList();
			handler.sendMessage(handler.obtainMessage(
					ANDROID_MESSAGE_TYPE_REFRESH_LIST, 0, millisDelay));
		}

		public void queue_refreshLobby(InternetLobby lobby, int millisDelay) {
			if (lobby == null)
				return;
			waitUntilReadyToQueue();
			handler.sendMessage(handler.obtainMessage(
					ANDROID_MESSAGE_TYPE_REFRESH_LOBBY_IN_BACKGROUND, 0, millisDelay, lobby));
		}
		
		
		public void queue_refreshLobbyForJoin(InternetLobby lobby, int millisDelay) {
			if ( lobby == null )
				return ;
			waitUntilReadyToQueue() ;
			handler.sendMessage(handler.obtainMessage(
					ANDROID_MESSAGE_TYPE_REFRESH_LOBBY_FOR_JOIN, 0, millisDelay, lobby));
		}
		

		public void queue_hideLobby(InternetLobby lobby, int millisDelay) {
			if (lobby == null)
				return;
			waitUntilReadyToQueue();
			handler.sendMessage(handler.obtainMessage(
					ANDROID_MESSAGE_TYPE_HIDE_LOBBY, 0, millisDelay, lobby));
		}
		
		public void queue_receiveInvitation( String invitation, int millisDelay ) {
			if ( invitation == null )
				return ;
			waitUntilReadyToQueue() ;
			handler.sendMessage(handler.obtainMessage(
					ANDROID_MESSAGE_TYPE_RECEIVE_INVITATION, 0, millisDelay, invitation));
		}

		@Override
		public void start() {
			started = true;
			super.start();
		}

		public void queue_stop(boolean stop_activity) {
			thread_queueStopping();
			running = false;
			if (handler != null) {
				for (int i = 0; i < NUM_ANDROID_MESSAGE_TYPES; i++)
					handler.removeMessages(i);
				if (isAlive())
					handler.sendMessage(handler
							.obtainMessage(ANDROID_MESSAGE_TYPE_STOP,
									stop_activity ? 1 : 0, 0));
			}
		}

		@Override
		public void run() {

			if (!running)
				return;

			// You have to prepare the looper before creating the handler.
			Looper.prepare();

			// Create the handler so it is bound to this thread's message queue.
			handler = new Handler() {

				@SuppressWarnings("unchecked")
				public void handleMessage(android.os.Message msg) {

					int index, code;
					Nonce nonce;

					int type = msg.what;
					int arg1 = msg.arg1;
					int delay = msg.arg2;
					InternetLobby lobby = msg.obj instanceof InternetLobby ? (InternetLobby) msg.obj
							: null;
					ArrayList<InternetLobby> lobby_list = msg.obj instanceof ArrayList<?> ? (ArrayList<InternetLobby>) msg.obj
							: null;
					String invitation = msg.obj instanceof String ? (String) msg.obj : null ;

					InternetLobbyDatabaseAdapter dba;
					
					boolean ok ;
					boolean failed_refresh ;
					boolean failed_database ;
				
					// Put it all in a try, quitting upon error.
					try {
						switch (type) {

						case ANDROID_MESSAGE_TYPE_STOP:
							running = false;
							getLooper().quit();
							if (arg1 == 1)
								finish();
							break;
							
							
						case ANDROID_MESSAGE_TYPE_GET_MIN_VERSION_CODE_IF_NOT_KNOWN:
							delayFor(delay) ;
							code = mMinVersionCode ;
							if ( code == -1 )
								code = AppVersion.minMultiplayerCode() ;
							if ( code == -1 )
								thread_noResponseMinVersionCode() ;
							else
								thread_receivedMinVersionCode(code) ;
							break ;
							

						case ANDROID_MESSAGE_TYPE_CLEAR_DATABASE:
							delayFor(delay);
							dba = new InternetLobbyDatabaseAdapter(
									InternetLobbyManagerActivity.this);
							try {
								dba.open();
								dba.deleteDatabase();
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								dba.close();
							}
							break;

						case ANDROID_MESSAGE_TYPE_REFRESH_LIST:
							// Refresh the list. Put up a refreshing spinner,
							// wait out the delay, then perform the refresh.
							thread_isRefreshingList();
							delayFor(delay + 1000);		// MANDATORY 1 second delay.
							
							// Now refresh.
							ArrayList<InternetLobby> lobbies = new ArrayList<InternetLobby>();
							dba = new InternetLobbyDatabaseAdapter(
									InternetLobbyManagerActivity.this);
							try {
								dba.open();

								// first thing: empty database of very old
								// lobbies.
								// remove those which were received more than 7
								// days ago,
								// or which have been removed.
								Date oneWeekAgo = new Date(
										(new Date()).getTime()
												- (1000 * 60 * 60 * 24 * 7));
								dba.deleteClosedAndNonItinerantLobbiesCreatedBefore(oneWeekAgo);
								dba.deleteClosedAndNonItinerantLobbiesReceivedBefore(oneWeekAgo);
								
								// check for version code
								code = mMinVersionCode ;
								if ( code == -1 ) {
									code = AppVersion.minMultiplayerCode() ;
									ok = true ;
									if ( code != -1 )
										ok = thread_receivedMinVersionCode(code) ;
									
									if ( code == -1 || !ok ) {
										Log.d(TAG, "Communication failure, or version code not OK.") ;
										throw new CommunicationErrorException().setError(-1, -1) ;
									}
								}
									
								

								InternetLobby.getLobbyList(lobbies, null,
										false, timeout);

								// load current invitations
								ArrayList<InternetLobby> invitations = dba
										.getAllLobbies();

								// for each invitation, check whether its nonce
								// is in our list.
								// if so, remove it from the list.
								for (int i = 0; i < invitations.size(); i++) {
									Nonce n = invitations.get(i)
											.getLobbyNonce();
									for (int j = 0; j < lobbies.size(); j++) {
										if (n.equals(lobbies.get(j)
												.getLobbyNonce())) {
											lobbies.remove(j);
											j--;
										}
									}
								}

								// remove those which have been hidden.
								index = 0;
								while (index < lobbies.size()) {
									if (mHiddenNonces.contains(lobbies.get(
											index).getLobbyNonce()))
										lobbies.remove(index);
									else
										index++;
								}
								
								for (int j = 0; j < lobbies.size(); j++)
									invitations.add(lobbies.get(j));
								lobbies = invitations;

								// Next: refresh all those lobbies.
								InternetLobby.refreshAll(lobbies, timeout);

								// Update those entries in the database...
								for (int j = 0; j < lobbies.size(); j++) {
									InternetLobby il = lobbies.get(j);
									if (il.getOrigin() != InternetLobby.ORIGIN_PUBLIC_LIST && il.getOrigin() != InternetLobby.ORIGIN_UNKNOWN) {
										if (dba.hasLobby(il))
											dba.updateLobby(il);
										else
											dba.insertLobby(il);
									}
								}

								// finally remove any closed or removed lobbies.

								// that's the list. We don't refresh individual
								// lobbies here, of course.
								thread_doneRefreshingList(lobbies);

							} catch (CommunicationErrorException cee) {
								thread_failedRefreshingList_communication();
							} catch (IOException ioe) {
								thread_failedRefreshingList_database();
							} finally {
								dba.close();
							}

							break;

						case ANDROID_MESSAGE_TYPE_REFRESH_LOBBY_IN_BACKGROUND:
							delayFor(delay);

							// refresh!
							try {
								
								// check for version code
								code = mMinVersionCode ;
								if ( code == -1 ) {
									code = AppVersion.minMultiplayerCode() ;
									ok = true ;
									if ( code != -1 )
										ok = thread_receivedMinVersionCode(code) ;
									
									if ( code == -1 || !ok )
										throw new CommunicationErrorException().setError(-1, -1) ;
								}
								
								lobby.refresh();
								thread_refreshedLobby(lobby);
							} catch (CommunicationErrorException cee) {
								// do nothing. This was a silent update, and it
								// gets a silent failure.
							}
							break;
							
							
						case ANDROID_MESSAGE_TYPE_REFRESH_LOBBY_FOR_JOIN:
							thread_isRefreshingLobbyForJoin( lobby ) ;
							delayFor(delay) ;
							
							// refresh!
							try {
								
								// check for version code
								code = mMinVersionCode ;
								if ( code == -1 ) {
									code = AppVersion.minMultiplayerCode() ;
									ok = true ;
									if ( code != -1 )
										ok = thread_receivedMinVersionCode(code) ;
									
									if ( code == -1 || !ok )
										throw new CommunicationErrorException().setError(-1, -1) ;
								}
								
								lobby.refresh() ;
								if ( lobby.getStatus() == WebConsts.STATUS_OPEN )
									thread_doneRefreshingLobbyForJoin( lobby ) ;
								else
									thread_failedRefreshingLobbyForJoin_status( lobby ) ;
							} catch ( CommunicationErrorException cee ) {
								thread_failedRefreshingLobbyForJoin_communication( lobby, cee.getError(), cee.getErrorReason() ) ;
							}
							break ;
							

						case ANDROID_MESSAGE_TYPE_HIDE_LOBBY:
							// remove the specified lobby.
							delayFor(delay);
							nonce = lobby.getLobbyNonce();

							// first: remove from our list of invitations.
							dba = new InternetLobbyDatabaseAdapter(
									InternetLobbyManagerActivity.this);
							try {
								dba.open();
								dba.deleteLobby(lobby);
							} finally {
								dba.close();
							}

							// third: if present in our list of lobbies (check
							// nonce, not lobby)
							// remove it and tell the view.
							index = -1;
							for (int j = 0; j < mInternetLobbies.size(); j++) {
								if (mInternetLobbies.get(j).getLobbyNonce()
										.equals(nonce))
									index = j;
							}
							
							if (index > -1) {
								mInternetLobbies.remove(index);
								if ( lobby.isPublic() )
									mHiddenNonces.add(nonce);
								thread_hidLobby(lobby);
							}

							break;
							
							
						case ANDROID_MESSAGE_TYPE_RECEIVE_INVITATION:
							// this is a multistep process. 
							// First, we process the invitation string and get an
							// 	InternetLobby out of it.  Failure = malformed.
							// Second, we attempt to refresh the lobby.  Failure can be a 
							// 	communication error or the non-presence of a private or public
							// 	lobby on the server.
							// Third, we add the lobby to our database (if not present) or
							//	update it (if present) and the current lobby list.
							// Fourth, we tell the Activity that we're done.  The Activity
							//	presumably throws up an Examination dialog, maybe starts a 
							//  List Refresh, etc.
							thread_isReceivingInvitation() ;
							delayFor(delay) ;
							ok = true ;
							failed_refresh = false ;
							failed_database = false ;
							
							// Step one: process the invitation string into an Internet Lobby.
							// This might fail!
							lobby = LobbyStringEncoder.toLobby(invitation) ;
							if ( lobby == null ) {
								thread_failedReceivingInvitation_malformed() ;
								break ;		// break out of this case; we're done.
							}
							
							// Step two: the lobby is non-null.  Try for a refresh.
							try {
								// check for version code
								code = mMinVersionCode ;
								if ( code == -1 ) {
									code = AppVersion.minMultiplayerCode() ;
									ok = true ;
									if ( code != -1 )
										ok = thread_receivedMinVersionCode(code) ;
									
									if ( code == -1 || !ok )
										throw new CommunicationErrorException().setError(-1, -1) ;
								}
								
								lobby.refresh() ;
							} catch ( CommunicationErrorException cee ) {
								ok = false ;
								failed_refresh = true ;
							}
							
							
							// Step three: database and lobby list insertion.  This happens
							// regardless of possible refresh failure above.
							dba = new InternetLobbyDatabaseAdapter(
									InternetLobbyManagerActivity.this);
							try {
								dba.open() ;
								if ( dba.hasLobby(lobby) )
									dba.updateLobby(lobby) ;
								else
									dba.insertLobby(lobby) ;
								
								Log.d(TAG, "just inserted or updated invited lobby.") ;
							} catch( Exception e ) {
								e.printStackTrace() ;
								ok = false ;
								failed_database = true ;
							} finally {
								dba.close() ;
							}
							// Put it in the list?
							nonce = lobby.getLobbyNonce() ;
							for ( int i = 0; i < mInternetLobbies.size(); i++ ) {
								InternetLobby l = mInternetLobbies.get(i) ;
								if ( l.getLobbyNonce().equals(nonce) ) {
									mInternetLobbies.remove(i) ;
									i-- ;
								}
							}
							mInternetLobbies.add(lobby) ;
							
							// Step four: tell the activity the result.
							if ( ok || lobby.isItinerant() )
								thread_doneReceivingInvitation(lobby) ;
							else if ( failed_refresh )
								thread_failedReceivingInvitation_refresh(lobby) ;
							else
								thread_failedReceivingInvitation_database(lobby) ;
							
							break ;

						}
					} catch (Exception e) {
						// MAJOR PROBLEM! HIDE YOUR KIDS, HIDE YOUR WIFE!
						e.printStackTrace();
						getLooper().quit();
					}

				}

			};

			Log.d(TAG, "Looper.loop");

			Looper.loop();

			Log.d(TAG, "Looper has quit()");

			// If we get here, we're done (we've exited the loop).

		}

		private void delayFor(int millis) {
			if (millis > 0) {
				try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {
				}
			}
		}

	}

	//
	// //////////////////////////////////////////////////////////////////////////

	
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
		return "help/lobby_manager_internet_activity.html" ;
	}
	
	@Override
	public String getHelpDialogContextName() {
		return getResources().getString(R.string.global_dialog_help_name_internet_lobby_manager) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
}
