package com.peaceray.quantro;

import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.premium.PremiumLibrary;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

public class QuantroActivity extends FragmentActivity {

	@SuppressWarnings("unused")
	private static final String TAG = "QuantroActivity";

	protected static final int QUANTRO_ACTIVITY_UNKNOWN = -1;
	protected static final int QUANTRO_ACTIVITY_MENU = 0;
	protected static final int QUANTRO_ACTIVITY_LOBBY = 1;
	protected static final int QUANTRO_ACTIVITY_GAME = 2;

	private static final String LAUNCHER_ACTIVITY_TYPE = "com.peaceray.quantro.QuantroActivity.LAUNCHER_ACTIVITY_TYPE";
	private static final String LAUNCHER_ACTIVITY_TYPE_FOR_MUSIC = "com.peaceray.quantro.QuantroActivity.LAUNCHER_ACTIVITY_TYPE_FOR_MUSIC";

	protected static final int QUANTRO_ACTIVITY_CONTENT_FULL = 0;
	protected static final int QUANTRO_ACTIVITY_CONTENT_PERMANENT_DIALOG = 1;
	protected static final int QUANTRO_ACTIVITY_CONTENT_EMPTY = 2;

	public static final int FLAG_TITLE = 0x01;
	public static final int FLAG_NOTIFICATION = 0x02;
	public static final int FLAG_NAVIGATION = 0x04 ;
	protected int mFlags;

	private boolean mSetup = false;
	private boolean mAnalyticsInSession = false;
	private int mActivityType = -1;
	private int mActivityContent = -1;
	private int mLauncherActivityType = QUANTRO_ACTIVITY_UNKNOWN ;
	private int mLauncherActivityTypeForMusic = QUANTRO_ACTIVITY_UNKNOWN ;

	private Handler mHandler ;
	private Runnable mRunnablePushAchievements ;

	private boolean mResumed = false ;
	private boolean mFinishing = false ;
	
	private PremiumLibrary mPremiumLibrary = null ;
		// a temporary storage location for PremiumLibrary.
		// Retained until a state change.
	
	////////////////////////////////////////////////////////////////////////////
	// STINGER CONTENT
	private View [] mStingerView ;
	private static final int STINGER_VIEW_SLIM_BACKGROUND = 0 ;
	private static final int STINGER_VIEW_SLIM_EXPAND = 1 ;
	private static final int STINGER_VIEW_SLIM_TEXT_APP = 2 ;
	private static final int STINGER_VIEW_SLIM_TEXT_COMPANY = 3 ;
	private static final int STINGER_VIEW_SLIM_TEXT_SIGN_IN_STATUS = 4 ;
	private static final int STINGER_VIEW_SLIM_CARET_EXPAND = 5 ;
	private static final int STINGER_VIEW_FULL_BACKGROUND = 6 ;
	private static final int STINGER_VIEW_FULL_SHADOW = 7 ;
	private static final int STINGER_VIEW_FULL_REDUCE = 8 ;
	private static final int STINGER_VIEW_FULL_CARET_REDUCE = 10 ;
	private static final int STINGER_VIEW_FULL_SIGN_IN_BUTTON = 11 ;
	private static final int STINGER_VIEW_FULL_SIGN_OUT_BUTTON = 12 ;
	private static final int STINGER_VIEW_FULL_TEXT_SIGN_IN_STATUS = 13 ;
	private static final int STINGER_VIEW_CARET_BUTTON = 14 ;
	private static final int NUM_STINGER_VIEWS = 15 ;
	private static final int MIN_STINGER_VIEW_SLIM = 0 ;
	private static final int NUM_STINGER_VIEW_SLIM = 6 ;
	private static final int MIN_STINGER_VIEW_FULL = 6 ;
	private static final int NUM_STINGER_VIEW_FULL = 9 ;
	
	private static final String SETTING_USER_HAS_REDUCED_STINGER = "com.peaceray.quantro.QuantroActivity.SETTING_USER_HAS_REDUCED_STINGER_0" ;
	
	private boolean mStingerLastUpdateSignedIn = false ;
	

	public void setFlagShowTitle(boolean title) {
		if (title)
			mFlags |= FLAG_TITLE;
		else
			mFlags &= ~FLAG_TITLE;
	}

	public void setFlagShowNotification(boolean notification) {
		if (notification)
			mFlags |= FLAG_NOTIFICATION;
		else
			mFlags &= ~FLAG_NOTIFICATION;
	}
	
	public void setFlagShowNavigation(boolean navigation) {
		if ( navigation )
			mFlags |= FLAG_NAVIGATION ;
		else
			mFlags &= ~FLAG_NAVIGATION ;
	}

	public boolean getFlagShowTitle() {
		return (mFlags & FLAG_TITLE) != 0;
	}

	public boolean getFlagShowNotification() {
		return (mFlags & FLAG_NOTIFICATION) != 0;
	}
	
	public boolean getFlagShowNavigation() {
		return (mFlags & FLAG_NAVIGATION) != 0;
	}
	
	public PremiumLibrary getPremiumLibrary() {
		if ( mPremiumLibrary != null )
			return mPremiumLibrary ;
		mPremiumLibrary = ((QuantroApplication)this.getApplication()).getPremiumLibrary() ;
		return mPremiumLibrary ;
	}
	
	public boolean isActivityFull() {
		return mActivityContent == QUANTRO_ACTIVITY_CONTENT_FULL ;
	}
	
	public boolean isActivityGame() {
		return mActivityType == QUANTRO_ACTIVITY_GAME ;
	}
	
	public boolean isActivityMenu() {
		return mActivityType == QUANTRO_ACTIVITY_MENU ;
	}
	
	public boolean isActivityLobby() {
		return mActivityType == QUANTRO_ACTIVITY_LOBBY ;
	}
	
	public boolean isActivityLaunchedByGame() {
		return mLauncherActivityType == QUANTRO_ACTIVITY_GAME ;
	}
	
	public boolean isActivityLaunchedByMenu() {
		return mLauncherActivityType == QUANTRO_ACTIVITY_MENU ;
	}
	
	public boolean isActivityLaunchedByLobby() {
		return mLauncherActivityType == QUANTRO_ACTIVITY_LOBBY ;
	}
	
	public int getActivityType() {
		return mActivityType ;
	}
	
	public int getLauncherActivityType() {
		return mLauncherActivityType ;
	}
	
	public int getActivityTypeForMusic() {
		return QUANTRO_ACTIVITY_UNKNOWN ;
	}
	
	public int getLauncherActivityTypeForMusic() {
		return mLauncherActivityTypeForMusic ;
	}
	
	
	public final Music getMusicForQuantroActivity() {
		int activityType = getActivityTypeForMusic() ;
		if ( activityType == QUANTRO_ACTIVITY_UNKNOWN ) {
			if ( mActivityContent == QUANTRO_ACTIVITY_CONTENT_FULL )
				activityType = getActivityType() ;
			else {
				activityType = getLauncherActivityTypeForMusic() ;
				if ( activityType == QUANTRO_ACTIVITY_UNKNOWN ) {
					activityType = getLauncherActivityType() ;
				}
			}
		}
		switch( activityType ) {
		case QUANTRO_ACTIVITY_MENU:
			return QuantroPreferences.getMusicInMenu(this) ;
		case QUANTRO_ACTIVITY_LOBBY:
			return QuantroPreferences.getMusicInLobby(this) ;
		}
		return null ;
	}
	

	/**
	 * Sets up the activity. Provided with the intent used to launch this
	 * activity (which may contain information from putQuantroExtras).
	 * 
	 * Effects: Applies screen size preferences according to the following: Gets
	 * activity type based on 1. the specified activity type, then ( if 1 is
	 * UNKNOWN ) the extra set in the intent. If both are unknown or unspecified
	 * assumes ACTIVITY_MENU in setting screen size. Applies screen size based
	 * on this value and 'fullScreen' which should be 'false' for dialog-style
	 * activities.
	 * 
	 * @param activityType
	 * @param activityContent
	 */
	protected void setupQuantroActivity(int activityType, int activityContent) {
		mHandler = new Handler();
		mRunnablePushAchievements = new Runnable() {
			@Override
			public void run() {
				// Log.d(TAG, "mRunnablePushAchievements inside " + QuantroActivity.this + " resumed is " + mResumed) ;
				mHandler.removeCallbacks(mRunnablePushAchievements) ;
				if ( mResumed ) {
					Achievements.push((QuantroApplication)getApplication(), false ) ;
					mHandler.postDelayed(this, 10 * 1000) ;		// every 10 seconds
				}
			}
		} ;

		mSetup = true;

		Intent i = getIntent();
		
		if (i.hasExtra(LAUNCHER_ACTIVITY_TYPE)) {
			mLauncherActivityType = i.getIntExtra(LAUNCHER_ACTIVITY_TYPE,
					QUANTRO_ACTIVITY_UNKNOWN);
		}
		if (i.hasExtra(LAUNCHER_ACTIVITY_TYPE_FOR_MUSIC)) {
			mLauncherActivityTypeForMusic = i.getIntExtra(LAUNCHER_ACTIVITY_TYPE_FOR_MUSIC,
					QUANTRO_ACTIVITY_UNKNOWN);
		}
		

		if (activityType == QUANTRO_ACTIVITY_MENU
				|| activityType == QUANTRO_ACTIVITY_LOBBY
				|| activityType == QUANTRO_ACTIVITY_GAME)
			mActivityType = activityType;
		else 
			mActivityType = mLauncherActivityType ;

		mActivityContent = activityContent;

		// Nothing gets a title.
		this.requestWindowFeature(Window.FEATURE_NO_TITLE) ;
		
		// Setup system UI bars.  We setup here if our version requires
		// pre-resume setup, or not a game.
		if ( !VersionSafe.setupUIImmersiveOnResume() || mActivityType != QUANTRO_ACTIVITY_GAME ) {
			setUIShowSystemBars() ;
		}
	}
	
	
	protected static final int FLAG_CONTENT_VIEW_DEFAULT = 0x0 ;
	protected static final int FLAG_CONTENT_VIEW_INCLUDE_STINGER = 0x1 ;
	protected static final int FLAG_CONTENT_VIEW_INCLUDE_ADS = 0x2 ;
	protected static final int FLAG_CONTENT_VIEW_OMIT_STINGER = 0x4 ;
	protected static final int FLAG_CONTENT_VIEW_OMIT_ADS = 0x8 ;
	
	
	/**
	 * Overrides setContentView to ensure that, when appropriate, the
	 * provided content view is wrapped in a stinger_frame and includes ads.
	 * 
	 * We also take this opportunity to set up Listeners and references
	 * between our stingers -- again, IF it was appropriate to include
	 * the stinger in the first place.
	 * 
	 */
	@Override
	public void setContentView( int id ) {
		setContentView( id, FLAG_CONTENT_VIEW_DEFAULT ) ;
	}
	
	/**
	 * Overrides setContentView to ensure that, when appropriate, the
	 * provided content view is wrapped in a stinger_frame and includes ads.
	 * 
	 * We also take this opportunity to set up Listeners and references
	 * between our stingers -- again, IF it was appropriate to include
	 * the stinger in the first place.
	 * 
	 */
	public void setContentView( int id, int flags ) {
		// Check for incompatible flags
		if ( ( (flags & FLAG_CONTENT_VIEW_INCLUDE_STINGER) != 0
					&& (flags & FLAG_CONTENT_VIEW_OMIT_STINGER) != 0 )
			|| ( (flags & FLAG_CONTENT_VIEW_INCLUDE_ADS) != 0
					&& (flags & FLAG_CONTENT_VIEW_OMIT_ADS) != 0 ) )
			throw new IllegalArgumentException("Check your flags: cannot both include and omit view element.") ;
		
		
		// Stinger?  If flagged to include or omit, do so.
		boolean hasStinger = mActivityType != QUANTRO_ACTIVITY_GAME
				&& mActivityContent == QUANTRO_ACTIVITY_CONTENT_FULL ;
		hasStinger = hasStinger || (flags & FLAG_CONTENT_VIEW_INCLUDE_STINGER) != 0 ;
		hasStinger = hasStinger && (flags & FLAG_CONTENT_VIEW_OMIT_STINGER) == 0 ;
		
		if ( !hasStinger ) {
			super.setContentView(id) ;
			return ;
		}

		super.setContentView(R.layout.activity_frame_with_stinger);
		
		// Configure the stinger content, if present
		if ( hasStinger ) {
			setupStinger() ;
		}
		
		// Load the provided ID to replace the placeholder.
		View placeholder = findViewById(R.id.activity_frame_content_placeholder) ;
		if ( placeholder != null ) {
			ViewGroup parent = (ViewGroup) placeholder.getParent() ;
			ViewGroup.LayoutParams layoutParams = placeholder.getLayoutParams() ;
			
			// replace with newly inflated content
			int index = parent.indexOfChild(placeholder) ;
		    parent.removeView(placeholder);
		    View content = getLayoutInflater().inflate(id, parent, false);
		    content.setLayoutParams(layoutParams) ;
		    parent.addView(content, index);
		}
	}
	
	
	private void setUIShowSystemBars() {
		// set fullscreen mode details: status bar, navigation bar?
		switch (mActivityType) {
		case QUANTRO_ACTIVITY_UNKNOWN:
		case QUANTRO_ACTIVITY_MENU:
			if (mActivityContent == QUANTRO_ACTIVITY_CONTENT_FULL)
				QuantroPreferences.getAndApplyScreenSizeMenu(this);
			else
				QuantroPreferences.getAndApplyScreenSizeMenuDialog(this);
			break;
		case QUANTRO_ACTIVITY_LOBBY:
			if (mActivityContent == QUANTRO_ACTIVITY_CONTENT_FULL)
				QuantroPreferences.getAndApplyScreenSizeLobby(this);
			else
				QuantroPreferences.getAndApplyScreenSizeLobbyDialog(this);
			break;
		case QUANTRO_ACTIVITY_GAME:
			if (mActivityContent == QUANTRO_ACTIVITY_CONTENT_FULL)
				QuantroPreferences.getAndApplyScreenSizeGame(this);
			else
				QuantroPreferences.getAndApplyScreenSizeGameDialog(this);
			break;
		}
	}
	
	
	public void setContentView( View v ) {
		throw new RuntimeException("Can't set content view to an explict view; use ID.") ;
	}
	
	public void setContentView (View view, ViewGroup.LayoutParams params) {
		throw new RuntimeException("Can't set content view to an explict view; use ID.") ;
	}
	
	
	
	/**
	 * A one-time per Stinger layout load call to setup relationships
	 * and connections.
	 * 
	 * Among its effects:
	 * 		Give all social badges an OnClickListener.
	 * 		Set up an OnTouchListener for the button that
	 * 			1: brightens the carets when touched
	 * 			2: toggles expanded and reduced when clicked.
	 * 
	 */
	private void setupStinger() {
		mStingerView = new View[NUM_STINGER_VIEWS] ;
		mStingerView[STINGER_VIEW_SLIM_BACKGROUND]		= findViewById(R.id.stinger_slim_background) ;
		mStingerView[STINGER_VIEW_SLIM_EXPAND]			= findViewById(R.id.stinger_expand_button) ;
		mStingerView[STINGER_VIEW_SLIM_TEXT_APP]		= findViewById(R.id.stinger_slim_text_app) ;
		mStingerView[STINGER_VIEW_SLIM_TEXT_COMPANY]	= findViewById(R.id.stinger_slim_text_company) ;
		mStingerView[STINGER_VIEW_SLIM_TEXT_SIGN_IN_STATUS] = findViewById(R.id.stinger_slim_text_signed_in_status) ;
		mStingerView[STINGER_VIEW_SLIM_CARET_EXPAND]	= findViewById(R.id.stinger_caret_expand) ;
		mStingerView[STINGER_VIEW_FULL_BACKGROUND]		= findViewById(R.id.stinger_background) ;
		mStingerView[STINGER_VIEW_FULL_SHADOW]			= findViewById(R.id.stinger_top_shadow) ;
		mStingerView[STINGER_VIEW_FULL_REDUCE]			= findViewById(R.id.stinger_reduce_button) ;
		mStingerView[STINGER_VIEW_FULL_CARET_REDUCE]	= findViewById(R.id.stinger_caret_reduce) ;
		mStingerView[STINGER_VIEW_FULL_SIGN_IN_BUTTON]	= findViewById(R.id.stinger_google_play_games_sign_in_button) ;
		mStingerView[STINGER_VIEW_FULL_SIGN_OUT_BUTTON]	= findViewById(R.id.stinger_google_play_games_sign_out_button) ;
		mStingerView[STINGER_VIEW_FULL_TEXT_SIGN_IN_STATUS]	= findViewById(R.id.stinger_full_text_signed_in_status) ;
		mStingerView[STINGER_VIEW_CARET_BUTTON]			= findViewById(R.id.stinger_caret_button) ;
		
		if ( mStingerView[STINGER_VIEW_SLIM_EXPAND] != null ) {
			mStingerView[STINGER_VIEW_SLIM_EXPAND].setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					expandStinger(true) ;
				}
			}) ;
		}
		
		if ( mStingerView[STINGER_VIEW_FULL_REDUCE] != null ) {
			mStingerView[STINGER_VIEW_FULL_REDUCE].setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					reduceStinger(true) ;
				}
			}) ;
		}
		
		
		if ( mStingerView[STINGER_VIEW_CARET_BUTTON] != null ) {
			mStingerView[STINGER_VIEW_CARET_BUTTON]
			             .setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					int action = event.getAction() ;
					switch( action ) {
					case MotionEvent.ACTION_DOWN:
						// a touch event just went down.  We pressed!
						if ( mStingerView[STINGER_VIEW_SLIM_EXPAND] != null )
							mStingerView[STINGER_VIEW_SLIM_EXPAND].setPressed(true) ;
						if ( mStingerView[STINGER_VIEW_FULL_REDUCE] != null )
							mStingerView[STINGER_VIEW_FULL_REDUCE].setPressed(true) ;
						view.setPressed(true) ;
						return true ;
						
					case MotionEvent.ACTION_UP:
						// a touch event just went up.  We released!
						if ( mStingerView[STINGER_VIEW_SLIM_EXPAND] != null )
							mStingerView[STINGER_VIEW_SLIM_EXPAND].setPressed(false) ;
						if ( mStingerView[STINGER_VIEW_FULL_REDUCE] != null )
							mStingerView[STINGER_VIEW_FULL_REDUCE].setPressed(false) ;
						
						// toggle?
						if ( view.isPressed() )
							toggleStinger(true) ;
						view.setPressed(false) ;
						return true ;
						
					case MotionEvent.ACTION_CANCEL:
						// a touch event just went up.  We released!
						if ( mStingerView[STINGER_VIEW_SLIM_EXPAND] != null )
							mStingerView[STINGER_VIEW_SLIM_EXPAND].setPressed(false) ;
						if ( mStingerView[STINGER_VIEW_FULL_REDUCE] != null )
							mStingerView[STINGER_VIEW_FULL_REDUCE].setPressed(false) ;
						view.setPressed(false) ;
						return true ;
						
					case MotionEvent.ACTION_OUTSIDE:
						// a touch event has gone outside the view!
						if ( mStingerView[STINGER_VIEW_SLIM_EXPAND] != null )
							mStingerView[STINGER_VIEW_SLIM_EXPAND].setPressed(false) ;
						if ( mStingerView[STINGER_VIEW_FULL_REDUCE] != null )
							mStingerView[STINGER_VIEW_FULL_REDUCE].setPressed(false) ;
						view.setPressed(false) ;
						return true ;
						
					case MotionEvent.ACTION_MOVE:
						float x = event.getX() ;
						float y = event.getY() ;
						if ( x < 0 || x > view.getWidth() || y < 0 || y > view.getHeight() ) {
							if ( mStingerView[STINGER_VIEW_SLIM_EXPAND] != null )
								mStingerView[STINGER_VIEW_SLIM_EXPAND].setPressed(false) ;
							if ( mStingerView[STINGER_VIEW_FULL_REDUCE] != null )
								mStingerView[STINGER_VIEW_FULL_REDUCE].setPressed(false) ;
							view.setPressed(false) ;
						}
						return true ;
					}
					
					return false ;
				}
			}) ;
		}

		if ( mStingerView[STINGER_VIEW_FULL_SHADOW] != null ) {
			GradientDrawable gd = new GradientDrawable(
	        		GradientDrawable.Orientation.BOTTOM_TOP,
	        		new int[]{ 0xbb000000, 0x000000 } ) ;
			VersionSafe.setBackground(mStingerView[STINGER_VIEW_FULL_SHADOW], gd) ;
		}
		
		// Set Sign In / Sign Out buttons.
		if ( mStingerView[STINGER_VIEW_FULL_SIGN_IN_BUTTON] != null ) {
			mStingerView[STINGER_VIEW_FULL_SIGN_IN_BUTTON].setOnClickListener( new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if ( VersionCapabilities.supportsGooglePlayGames() ) {
						((QuantroApplication)getApplication()).gpg_beginUserInitiatedSignIn() ;
					} else {
						Toast.makeText(QuantroActivity.this, "Google Play Games is not supported on this device.", Toast.LENGTH_SHORT).show() ;
					}
				}
			}) ;
		}
		
		if ( mStingerView[STINGER_VIEW_FULL_SIGN_OUT_BUTTON] != null ) {
			mStingerView[STINGER_VIEW_FULL_SIGN_OUT_BUTTON].setOnClickListener( new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					((QuantroApplication)getApplication()).gpg_signOut(true) ;
				}
			}) ;
		}
		
		
	}
	
	
	public void updateStingerStatus(boolean force) {
		if ( mStingerView == null )
			return ;
		
		// update sign in / status texts.
		boolean signedIn = ((QuantroApplication)getApplication()).gpg_isSignedIn() ;
		
		if ( force || signedIn != mStingerLastUpdateSignedIn ) {
			
			mStingerLastUpdateSignedIn = signedIn ;
			
			TextView tv = (TextView) mStingerView[STINGER_VIEW_SLIM_TEXT_SIGN_IN_STATUS] ;
			if ( tv != null ) {
				tv.setText( signedIn
						? R.string.stinger_google_is_signed_in_short
						: R.string.stinger_google_is_signed_out_short) ;
			}
			
			tv = (TextView) mStingerView[STINGER_VIEW_FULL_TEXT_SIGN_IN_STATUS] ;
			if ( tv != null ) {
				tv.setText( signedIn
						? R.string.stinger_google_sign_out_description
						: R.string.stinger_google_sign_in_description) ;
			}
			
			// buttons.
			View v = mStingerView[STINGER_VIEW_FULL_BACKGROUND];
			boolean expanded = v != null && v.getVisibility() == View.VISIBLE ;
			if (mStingerView[STINGER_VIEW_FULL_SIGN_IN_BUTTON] != null) {
				mStingerView[STINGER_VIEW_FULL_SIGN_IN_BUTTON].setVisibility(expanded && !signedIn
						? View.VISIBLE : View.INVISIBLE);
			}
			if (mStingerView[STINGER_VIEW_FULL_SIGN_OUT_BUTTON] != null) {
				mStingerView[STINGER_VIEW_FULL_SIGN_OUT_BUTTON].setVisibility(expanded && signedIn
						? View.VISIBLE : View.INVISIBLE);
			}
		}
	}
	
	
	private void toggleStinger( boolean byUser ) {
		if ( mStingerView == null )
			return ;

		View fullBG = mStingerView[STINGER_VIEW_FULL_BACKGROUND];
		if ( fullBG != null && fullBG.getVisibility() == View.INVISIBLE )
			expandStinger( byUser ) ;
		else
			reduceStinger( byUser ) ;
	}
	
	
	protected void expandStinger( boolean byUser ) {
		if ( mStingerView == null )
			return ;
		
		// show all 'full' stinger views.  For sign in / sign out buttons, only
		// show the appropriate one.
		boolean signedIn = ((QuantroApplication)getApplication()).gpg_isSignedIn() ;
		for ( int i = MIN_STINGER_VIEW_FULL ; i < MIN_STINGER_VIEW_FULL + NUM_STINGER_VIEW_FULL ; i++ ) {
			if ( mStingerView[i] != null ) {
				if ( i ==  STINGER_VIEW_FULL_SIGN_IN_BUTTON )
					mStingerView[i].setVisibility(signedIn ? View.INVISIBLE : View.VISIBLE) ;
				else if ( i == STINGER_VIEW_FULL_SIGN_OUT_BUTTON )
					mStingerView[i].setVisibility(signedIn ? View.VISIBLE : View.INVISIBLE) ;
				else
					mStingerView[i].setVisibility(View.VISIBLE) ;
			}
		}
		// hide all 'slim' stinger views.
		for ( int i = MIN_STINGER_VIEW_SLIM ; i < MIN_STINGER_VIEW_SLIM+ NUM_STINGER_VIEW_SLIM ; i++ ) {
			if ( mStingerView[i] != null )
				mStingerView[i].setVisibility(View.INVISIBLE) ;
		}
		
		if ( byUser ) {
			// user has expanded the stinger!
			QuantroPreferences.setPrivateSettingBoolean(this, SETTING_USER_HAS_REDUCED_STINGER, false) ;
		}
	}
	
	protected void reduceStinger( boolean byUser ) {
		if ( mStingerView == null )
			return ;
		
		// hide all 'full' stinger views.
		for ( int i = MIN_STINGER_VIEW_FULL ; i < MIN_STINGER_VIEW_FULL + NUM_STINGER_VIEW_FULL ; i++ ) {
			if ( mStingerView[i] != null )
				mStingerView[i].setVisibility(View.INVISIBLE) ;
		}
		// show all 'slim' stinger views.
		for ( int i = MIN_STINGER_VIEW_SLIM ; i < MIN_STINGER_VIEW_SLIM+ NUM_STINGER_VIEW_SLIM ; i++ ) {
			if ( mStingerView[i] != null )
				mStingerView[i].setVisibility(View.VISIBLE) ;
		}
		
		if ( !VersionCapabilities.supportsGooglePlayGames() ) {
			if ( mStingerView[STINGER_VIEW_SLIM_TEXT_SIGN_IN_STATUS] != null ) {
				mStingerView[STINGER_VIEW_SLIM_TEXT_SIGN_IN_STATUS].setVisibility(View.INVISIBLE) ;
			}
				
		}
		
		if ( byUser ) {
			// user has reduced the stinger!
			QuantroPreferences.setPrivateSettingBoolean(this, SETTING_USER_HAS_REDUCED_STINGER, true) ;
		}
	}
	
	
	public void onGooglePlaySignInUpdated( boolean signedIn ) {
		updateStingerStatus(false) ;
	}

	private void putQuantroActivityLaunchExtras(Intent i) {
		i.putExtra(LAUNCHER_ACTIVITY_TYPE, mActivityType);
		int forMusic = getActivityTypeForMusic() ;
		if ( forMusic != QUANTRO_ACTIVITY_UNKNOWN ) {
			i.putExtra(LAUNCHER_ACTIVITY_TYPE_FOR_MUSIC, forMusic) ;
		}
	}

	public void startActivities(Intent[] intents) {
		throw new RuntimeException(
				"WRONG-O, BOYO!  To maintain low-API compatibility, startActivities (API 11) is disabled.");
		/*
		 * for ( int i = 0; i < intents.length; i++ ) if ( intents[i] != null )
		 * putQuantroActivityLaunchExtras( intents[i] ) ;
		 * 
		 * super.startActivities(intents) ;
		 */
	}

	@Override
	public void startActivity(Intent intent) {
		putQuantroActivityLaunchExtras(intent);
		super.startActivity(intent);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		putQuantroActivityLaunchExtras(intent);
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	public void startActivityFromChild(Activity child, Intent intent,
			int requestCode) {
		if (child instanceof QuantroActivity)
			((QuantroActivity) child).putQuantroActivityLaunchExtras(intent);
		super.startActivityFromChild(child, intent, requestCode);
	}

	/*
	 * @Override public void startActivityFromFragment (Fragment fragment,
	 * Intent intent, int requestCode) { throw new RuntimeException(
	 * "WRONG-O, BOYO!  To maintain low-API compatibility, startActivityFromFragment (API 11) is disabled."
	 * ) ;
	 * 
	 * putQuantroActivityLaunchExtras(intent) ;
	 * super.startActivityFromFragment(fragment, intent, requestCode) ;
	 * 
	 * }
	 */

	@Override
	public boolean startActivityIfNeeded(Intent intent, int requestCode) {
		putQuantroActivityLaunchExtras(intent);
		return super.startActivityIfNeeded(intent, requestCode);
	}
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState) ;
		
		// callback to activity
		((QuantroApplication)this.getApplication()).onActivityCreated(this, savedInstanceState) ;
	}

	@Override
	protected void onStart() {
		if (!mSetup)
			throw new IllegalStateException(
					"setupQuantroActivity not called before onStart!");
		super.onStart();
		stateChange() ;
		
		
		// Analytics?
		if (QuantroPreferences.getAnalyticsActive(this)) {
			Analytics.startSession(this);
			mAnalyticsInSession = true;
		}
		
		// set default stinger state?  If the user has previously
		// reduced a stinger, we assume they know the deal and we
		// start it reduced.  Otherwise, we start in expanded so they
		// can see the new feature.
		if ( QuantroPreferences.getPrivateSettingBoolean(this, SETTING_USER_HAS_REDUCED_STINGER, false)
				|| !VersionCapabilities.supportsGooglePlayGames() )
			reduceStinger(false) ;		// false: not set by user.
		else
			expandStinger(false) ;		// false: not set by user.
		updateStingerStatus(true) ;
		
		// Set background!
		ImageView iv = (ImageView)findViewById(R.id.menu_background) ;
		if ( iv != null ) {
			Bitmap bgBitmap = ((QuantroApplication)getApplicationContext()).getMenuBackgroundBitmap(this) ;
			if ( bgBitmap != null )
				iv.setImageBitmap( bgBitmap ) ;
			else
				iv.setImageDrawable(new ColorDrawable(Color.BLACK)) ;
		}
		
		// callback to activity
		((QuantroApplication)this.getApplication()).onActivityStarted(this) ;
	}

	@Override
	protected void onResume() {
		super.onResume();
		stateChange() ;
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC) ;
		
		updateStingerStatus(false) ;
		
		this.mResumed = true ;
		mHandler.postDelayed(mRunnablePushAchievements, 10 * 1000) ;	// 10 seconds
		
		
		// Setup system UI bars...?
		if ( VersionSafe.setupUIImmersiveOnResume() && mActivityType == QUANTRO_ACTIVITY_GAME ) {
			setUIShowSystemBars() ;
		}
		
		// callback to activity
		((QuantroApplication)this.getApplication()).onActivityResumed(this) ;
	}

	@Override
	protected void onPause() {
		super.onPause();
		stateChange() ;
		
		this.mResumed = false ;
		mHandler.removeCallbacks(mRunnablePushAchievements) ;
		
		// callback to activity
		((QuantroApplication)this.getApplication()).onActivityPaused(this) ;
	}

	@Override
	protected void onStop() {
		super.onStop();
		stateChange() ;

		// Analytics?
		if (mAnalyticsInSession) {
			Analytics.stopSession(this);
			mAnalyticsInSession = false;
		}
		
		// Unset background to save memory.
		ImageView iv = (ImageView)findViewById(R.id.menu_background) ;
		if ( iv != null ) {
			iv.setImageDrawable(null) ;
		}
		
		// callback to activity
		((QuantroApplication)this.getApplication()).onActivityStopped(this) ;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy() ;
		stateChange() ;

		unbindReferences() ;
		
		// callback to activity
		((QuantroApplication)this.getApplication()).onActivityDestroyed(this) ;
	}

	@Override
	public void finish() {
		stateChange() ;
		switch (mActivityContent) {
		case QUANTRO_ACTIVITY_CONTENT_FULL:
		case QUANTRO_ACTIVITY_CONTENT_PERMANENT_DIALOG:
			// immediately quit
			super.finish();
			mFinishing = true ;
			break;

		case QUANTRO_ACTIVITY_CONTENT_EMPTY:
			// give the displayed dialogs time to disappear.
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					QuantroActivity.super.finish();
					mFinishing = true ;
				}
			}, 500);
			break;
		}
	}
	
	
	public void onWindowFocusChanged( boolean hasFocus ) {
		super.onWindowFocusChanged(hasFocus) ;
		if ( VersionSafe.setupUIImmersiveOnResume() && hasFocus && mActivityType == QUANTRO_ACTIVITY_GAME ) {
			// setup ui...
			setUIShowSystemBars() ;
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		handleActivityResult(requestCode, resultCode, data) ;
    }
	
	
	/**
	 * Attempts to handle the Activity result.
	 * 
	 * Both the base class (QuantroActivity) and subclasses (e.g. FreePlayGameManagerActivity)
	 * can handle activity results of different types.
	 * 
	 * This method will attempt to process an activity result, assuming that it was initiated
	 * by this class  and not a superclass.  If it returns 'true', this assumption was
	 * correct and we have processed it.
	 * 
	 * Otherwise, on return 'false,' subclasses should handle the activity result.
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 * @return
	 */
	protected boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		// let the application handle this.
		return ((QuantroApplication)getApplication()).gpg_handleActivityResult(requestCode, resultCode, data) ;
	}
	
	private void stateChange() {
		mPremiumLibrary = null ;
	}

	
	////////////////////////////////////////////////////////////////////////////
	//
	// POSSIBLE OVERKILL, BUT HOPEFULLY REDUCES OUT-OF-MEMORY ERRORS.
	// 
	// Code taken from http://1gravity.com/index.php?option=com_content&view=article&id=71%3aandroid-and-memory-leaks&catid=37%3aandroid&Itemid=59
	//
	
	private void unbindReferences() {
		long time = System.currentTimeMillis() ;
		try {
			unbindReferences(this, this.findViewById(android.R.id.content).getRootView()) ;
		} catch ( Exception e ) { }
		Log.d(TAG, "unbindReferences took " + (System.currentTimeMillis() - time)) ;
	}
	
	private static void unbindReferences(Activity activity, int viewID) {
		unbindReferences( activity, activity.findViewById(viewID) ) ;
	}
	
	private static void unbindReferences(Activity activity, View view) {
		try {
			if (view!=null) {
				// TODO destroy adView (if any)
				unbindViewReferences(view);
				if (view instanceof ViewGroup)
					unbindViewGroupReferences((ViewGroup) view);
			}
			System.gc();
		} catch (Throwable e) {
			// whatever exception is thrown just ignore it because a crash is always worse than this method not doing what it's supposed to do
		}
	}
	
	private static void unbindViewGroupReferences(ViewGroup viewGroup) {
		int nrOfChildren = viewGroup.getChildCount();
		for (int i=0; i < nrOfChildren; i++ ) {
			View view = viewGroup.getChildAt(i);
			unbindViewReferences(view);
			if (view instanceof ViewGroup)
				unbindViewGroupReferences((ViewGroup) view);
		}
		try {
			viewGroup.removeAllViews();
		} catch (Throwable mayHappen) {
			// AdapterViews, ListViews and potentially other ViewGroups don't support the removeAllViews operation
		}
	}
	
	private static void unbindViewReferences(View view) {
		// set all listeners to null (not every view and not every API level supports the methods)
		try {view.setOnClickListener(null);} catch (Throwable mayHappen) {};
		try {view.setOnCreateContextMenuListener(null);} catch (Throwable mayHappen) {};
		try {view.setOnFocusChangeListener(null);} catch (Throwable mayHappen) {};
		try {view.setOnKeyListener(null);} catch (Throwable mayHappen) {};
		try {view.setOnLongClickListener(null);} catch (Throwable mayHappen) {};
		try {view.setOnClickListener(null);} catch (Throwable mayHappen) {};

		// set background to null
		Drawable d = view.getBackground(); 
		if (d!=null) d.setCallback(null);
		if (view instanceof ImageView) {
			ImageView imageView = (ImageView) view;
			d = imageView.getDrawable();
			if (d!=null) d.setCallback(null);
			imageView.setImageDrawable(null);
			VersionSafe.setBackground(view, null);
		}

		// destroy webview
		if (view instanceof WebView) {
			((WebView) view).destroyDrawingCache();
			((WebView) view).destroy();
		}
	}
	
}
