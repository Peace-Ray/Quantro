package com.peaceray.quantro;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.database.CustomGameModeSettingsDatabaseAdapter;
import com.peaceray.quantro.database.GameSettingsDatabaseAdapter;
import com.peaceray.quantro.database.GameStats;
import com.peaceray.quantro.dialog.DialogManager ;
import com.peaceray.quantro.dialog.GlobalDialog;
import com.peaceray.quantro.keys.KeyRing;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;
import com.peaceray.quantro.lobby.WiFiLobbyFinder;
import com.peaceray.quantro.model.GameSaver;
import com.peaceray.quantro.model.game.GameBlocksSliceSequence;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.CustomGameModeSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.utils.AssetAccessor;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.button.strip.MainMenuButtonStrip;
import com.peaceray.quantro.view.button.strip.MainMenuTitleBarButtonStrip;
import com.peaceray.quantro.view.button.strip.QuantroButtonStrip;
import com.peaceray.quantro.view.button.strip.SinglePlayerGameLaunchButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.dialog.WebViewDialog;
import com.peaceray.quantro.view.drawable.DropShadowDrawableFactory;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.GameViewMemoryCapabilities;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.velosmobile.utils.SectionableAdapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.AbsListView.OnScrollListener;

public class MainMenuActivity extends QuantroActivity
		implements MainMenuButtonStrip.Delegate, MainMenuTitleBarButtonStrip.Delegate,
		WiFiLobbyFinder.Delegate, GlobalDialog.DialogContext {

	private static final String TAG = "MainMenuActivity" ;
	
	private static final boolean DEMO_VERSION = false ;		// if true, disables features we don't want to send out
	private static final boolean DEMO_VERSION_ALLOW_WIFI = false ;	// if true, disables InternetMultiplayer.
	
	
	private static final int DIALOG_ID_UPDATE_CONTROLS = 0;
	
	private DialogManager mDialogManager ;
	
	
	private static final String PREFERENCE_UPDATE_CONTROLS_DONE = "com.peaceray.quantro.MainMenuActivity.PREFERENCE_UPDATE_CONTROLS_DONE" ;
	
	
	
	// Menu Views, containers for other stuff including ListViews.
	MainMenuTitleBarButtonStrip mTitleBarButtonStrip ;
	ViewFlipper mViewFlipper ;
	//TextView mInstructionsTextView ;
	
	int VIEW_FLIPPER_FIRST_LAYER = 1 ;
	int VIEW_FLIPPER_LEAF_LAYER = 5 ;
	
	// Title bar tags.  Applied to title bar buttons; an easy way to
	// examine a button w/o caring about button number (we may, e.g., have
	// a Quantro Logo button as a different button depending on whether
	// this is Quantro XL) so we set up the buttons with tags applied, then
	// check those tags upon a button press.
	private static final String TAG_TITLE_BAR_QUANTRO = "TAG_TITLE_BAR_QUANTRO" ;
	private static final String TAG_TITLE_BAR_UNLOCK = "TAG_TITLE_BAR_UNLOCK" ;
	
	// Menu List View and stuff.  These are used exclusively for our general
	// list hierarchy.  These structures do not represent actual menu items;
	// instead, they represent the level of menu "depth.", so they may be reused
	// for different menu items at the same depth.  We use these lists for
	// hierarchical menu items and NOT any special content for leaves.
	int mMenuRowsPerPage = -1 ;		// initial value; will be reset.
	int mMenuItemsPerRow = -1 ;
	ListView [] mMenuHierarchyListView ;
	MenuItemArrayAdapter [] mMenuHierarchyArrayAdapter ;	// Menu item
	int mMenuHierarchyDepth ;		// the length of these.  Will be 1 if we have only a root.
	int mDisplayedMenuLayer ;
	int mDisplayedMenuItem ;
	int [] mMenuLayerItem ;			// The menu item (NOT leaf) represented at hierarchy level i.  Valid
									// for i = 0...mDisplayedMenuLayer, invalid beyond.
	boolean mDisplayedMenuItemIsLeaf ;		// if 'true', put mDisplayedMenuLayer is place on "back".
											// else, actually go back to the previous layer.
	
	boolean mMenuLeavesAreReady = false ;
	boolean mMenuLayersAreReady = false ;
	
	// Menu Hierarchy.  Each entry, indexed by MENU_ITEM_*, gives a list of 
	// other menu items - those are the menu items which appear when the
	// first is pressed.  If 'null' or length 0, it is a special case that
	// deserves special consideration.
	int [][] mMenuHierarchy ;
	int [] mMenuHierarchyItemDepth ;
	int [] mMenuHierarchyCategory ;		// indicates the category to which this
										// menu item belongs.  This may not necessarily
										// be its hierarchy parent, or even a node in the
										// hierarchy.  Instead, it represents a label
										// to be applied in large, multi-item sectionable
										// menus.
	
	
	// Leaf views.  These take the place of the menu hierarchy for
	// those items which have their contents displayed in the main
	// menu.  Right now, I think that's just the SinglePlayer game mode
	// list, but we leave this structure available for more views.
	View [] mMenuHierarchyPlaceholderView ;
	View [] mMenuLeafView ;
	View [] mMenuLeafTemporaryViewStorage ;
	
	// HTML file paths.  Some leaves open as HTML pages in the user's browser.
	// These paths specify the URL to send to the browser.
	String [] mMenuLeafHTMLPath ;
	
	
	// Menu items.  These are fake-constants, set in onCreate from system
	// resources and never altered.
	int MENU_ITEM_ROOT ;
	int MENU_ITEM_SINGLE_PLAYER ;
	int MENU_ITEM_MULTI_PLAYER ;
	int MENU_ITEM_SOCIAL_AND_SETUP ;
	int MENU_ITEM_INFORMATION ;
	int MENU_ITEM_WIFI_MULTI_PLAYER ;
	int MENU_ITEM_INTERNET_MULTI_PLAYER ;
	int MENU_ITEM_SOCIAL ;
	int MENU_ITEM_ACHIEVEMENTS ;
	int MENU_ITEM_LEADERBOARDS ;
	int MENU_ITEM_SETUP ;
	int MENU_ITEM_SETTINGS ;
	int MENU_ITEM_HOW_TO_PLAY ;
	int MENU_ITEM_APP_INFO ;
	int MENU_ITEM_FEEDBACK ;
	int MENU_ITEM_POLICIES ;
	int MENU_ITEM_USER_LICENSE ;
	int MENU_ITEM_PRIVACY_POLICY ;
	int MENU_ITEM_UPDATES ; 
	int MENU_ITEM_WHATS_NEW ;
	int MENU_ITEM_VERSION_HISTORY ;
	int MENU_ITEM_CREDITS ;
	
	int MENU_ITEM_HOW_TO_PLAY_BASICS ;
	int MENU_ITEM_HOW_TO_PLAY_ADVANCED ;
	
	int MENU_ITEM_HOW_TO_PLAY_GAME_BASICS ;
	int MENU_ITEM_HOW_TO_PLAY_CONTROLS ;
	int MENU_ITEM_HOW_TO_PLAY_GAME_MODES ;
	int MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES ;
	int MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES ;
	
	int MENU_ITEM_FEEDBACK_EMAIL ;
	
	int NUM_MENU_ITEMS ;
	
	Integer [] MENU_ITEM_INTEGER_OBJECT ;
	
	int [] mMenuItemCohesion ;
	String [] mMenuItemTitles ;
	String [] mMenuItemDescriptions ;
	String [] mMenuItemDescriptionsDisabled ;
	String [] mMenuItemInstructions ;
	int [] mMenuItemColors ;
	int [] mMenuItemAlerts ;
	Drawable [] mMenuItemAlertOverwriteDrawable ;
	
	// We might need to change-up the root title and description according
	// to XL status.
	String mMenuItemRootTitle ;
	String mMenuItemRootDescription ;
	String mMenuItemRootXLTitle ;
	String mMenuItemRootXLDescription ;
	// We also change the "key management" item.
	String mMenuItemPremiumStoreTitle ;
	String mMenuItemPremiumStoreDescription ;
	String mMenuItemPremiumStoreXLTitle ;
	String mMenuItemPremiumStoreXLDescription ;
	// ...and the "GameSetup" item containing it.
	String mMenuItemGameSetupTitle ;
	String mMenuItemGameSetupDescription ;
	String mMenuItemGameSetupXLTitle ;
	String mMenuItemGameSetupXLDescription ;
	
	// To be more responsive to the Quick-Play button, we preload quick-play strings
	// in onResume.
	String mQuickLoadToastText ;
	String mQuickStartToastText ;
	
	// Handler and tasks
	Handler mHandler ;
	Runnable mInitialSetupRunnable ;
	boolean mHasInitialSetup = false ;	// Have we ever done the initial setup successfully?
	boolean mInFront = false ;
	
	// Here's our AsyncSetup structures.
	boolean [] mAsyncSetupComplete ;		// initialized to all-false
	@SuppressWarnings("rawtypes")
	AsyncTask [] mAsyncSetupTask ;			// created anew in onResume.
	boolean mAsyncSetupIsWakeLocked ;		// are we currently locked?
	boolean mAsyncSetupIsComplete ;			// are we completely finished with async setup?
	
	// We have some specialized style information for drawing the title bar.
	private static final int MAIN_TITLE_STYLE_NONE = 0 ;		// draw a normal button with a background
	private static final int MAIN_TITLE_STYLE_OVER = 1 ;		// draw a line shaded background, with a color-coded LSB over it
	private static final int MAIN_TITLE_STYLE_OFFSET = 2 ;		// as "over", but offset the color-coded by 1/2 separation.
	private static final int MAIN_TITLE_STYLE_OVER_DOUBLE = 3 ;	// as "over", but color-coded has 1/2 separation.
	private static final int MAIN_TITLE_STYLE_OVER_DOUBLE_FADE_BUTTONS = 4 ;	// as "over double", but button elements are set to
																				// scale from "FADE_ALPHA" to 0.  These elements are 
																				// set using calls to setAlphaMultForSectionAtState
																				// and setGradientAlphaMults.
	
	private int mMainTitleStyle = MAIN_TITLE_STYLE_NONE ; // MAIN_TITLE_STYLE_OVER_DOUBLE_FADE_BUTTONS ;
	
	private static final float MAIN_TITLE_LSD_LINE_WIDTH = 1.0f ;
	private static final float MAIN_TITLE_LSD_LINE_SEPARATION = 5.0f ;
	private static final int MAIN_TITLE_LSD_LINE_COLOR = 0xff303030 ;
	
	private static final float MAIN_TITLE_LSD_LINE_FADE_ALPHA_MULT = 0.5f ;		// only used for _FADE styles.  Otherwise, LINE_COLOR is used.
	
	
	
	// Our animated logo.
	private GameBlocksSliceSequence mLogoSequence ;
	
	// Here's our color scheme and skins.
	private Skin mQuantroSkin ;
	private Skin mRetroSkin ;
	private ColorScheme mColorScheme ;
	// Preference: sound upon button press?
	private boolean mButtonSounds ;
	// Sound pool!
	private QuantroSoundPool mSoundPool ;
	// Our Wifi lobby finder.
	private WiFiLobbyFinder mLobbyFinder ;
	private Runnable mRunnableWifiLobbyAlertRefresh ;
			// a self-posting runnable to refresh lobbies.
	private static final int TIME_BETWEEN_WIFI_REFRESH_MIN = 2000 ;		// 2 second MINIMUM.
	private static final int TIME_BETWEEN_WIFI_REFRESH_MAX = 10000 ;	// 10 second MAXIMUM.
	private static final int TIME_BEFORE_LOADING_ADS = 7000 ;			// 7 seconds MAXIMUM.
	private long mWifiLobbyAlertLastRefreshTime ;
	
	// Have we been in front since Creation?  If not, than onStart is a good
	// time to log device and settings with our Analytics.
	private boolean mLoggedDevice = false ;
	
	private boolean mHasResumed = false ;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupQuantroActivity( QUANTRO_ACTIVITY_MENU, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
        //Log.d(TAG, "onCreate bracket IN") ;
        
        long time = System.currentTimeMillis() ;
        
        // Force portrait layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;
        
        mDialogManager = new DialogManager(this) ;

        // TEST TEST TEST TEST TEST
        if ( GlobalTestSettings.DROP_SHADOW_STRICT_TESTS ) {
	        Log.d(TAG, "DROP SHADOW DRAWABLE FACTORY TEST") ;
	        if ( DropShadowDrawableFactory.test(this) ) {
	        	Log.d(TAG, "DROP SHADOW DRAWABLE FACTORY TEST OK") ;
	        } else {
	        	Log.d(TAG, "DROP SHADOW DRAWABLE FACTORY TEST NOT OK") ;
	        }
        }
        
        Log.d(TAG, "onCreate loaded view in " + (System.currentTimeMillis() - time) + "millis") ;
        time = System.currentTimeMillis() ;
        
        Log.d(TAG, "onCreate with heap size " + VersionSafe.getMemoryClass(this) + " large heap " + VersionSafe.getLargeMemoryClass(this)) ;
        
        Resources res = getResources() ;
        
        mQuantroSkin = QuantroPreferences.getSkinQuantro(this) ;
        mRetroSkin = QuantroPreferences.getSkinRetro(this) ;
        mColorScheme = new ColorScheme(
				this,
				mQuantroSkin.getColor(),
				mRetroSkin.getColor() ) ;
        mButtonSounds = QuantroPreferences.getSoundControls(this) ;
        mSoundPool = ((QuantroApplication)getApplication()).getSoundPool(this) ;
        mLobbyFinder = new WiFiLobbyFinder( this, getResources().getInteger( R.integer.wifi_multiplayer_lobby_announcement_port),
        		getResources().getInteger( R.integer.wifi_multiplayer_lobby_query_port),
        		getResources().getInteger( R.integer.wifi_multiplayer_lobby_query_response_port),
        		15000, 10000, this ) ;	// takes 15 seconds for a lobby to expire, 10 seconds between queries for targeted lobbies
        mRunnableWifiLobbyAlertRefresh = new Runnable() {
			@Override
			public void run() {
				if ( mInFront ) {
					//Log.d(TAG, "mRunnableWifiLobbyAlertRefresh bracket IN run") ;
					mHandler.removeCallbacks(this) ;
					updateAlertWifiMultiplayer() ;
					mWifiLobbyAlertLastRefreshTime = System.currentTimeMillis() ;
					mHandler.postDelayed(this, TIME_BETWEEN_WIFI_REFRESH_MAX) ;
					//Log.d(TAG, "mRunnableWifiLobbyAlertRefresh bracket OUT run") ;
				}
			}
        } ;
        mWifiLobbyAlertLastRefreshTime = 0 ;
        
        Log.d(TAG, "onCreate created structures in " + (System.currentTimeMillis() - time) + " millis") ;
        time = System.currentTimeMillis() ;
        
        
        // LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
        setContentView(R.layout.main_menu) ;
        
        // Get references to our views - those which don't get swapped around.
        onCreate_getMenuViews( res ) ;
        onCreate_configureMenuViews( res ) ;
        
        Log.d(TAG, "onCreate got and configured MenuViews in " + (System.currentTimeMillis() - time) + " millis") ;
        time = System.currentTimeMillis() ;
        
        // Set up our structures.  For instance, load and create arrays of
        // colors (integers), titles and descriptions (for menu items), etc.
        // A more complex action is to set up our menu item sets and nesting,
        // since this requires measure on-screen elements and determining what
        // we have space for.  Do the simple part first.
        onCreate_allocateMenuItemStructures( res ) ;
        onCreate_getMenuRoot( res ) ;
        
        Log.d(TAG, "onCreate allocated and got MenuRoots in " + (System.currentTimeMillis() - time) + " millis") ;
        time = System.currentTimeMillis() ;
        
        // set for title bar.  Configures everything without setting the logo.
        onCreate_setTitleBarMenuItems( res ) ;
        
        Log.d(TAG, "onCreate set title bar items in " + (System.currentTimeMillis() - time) + " millis") ;
        
        time = System.currentTimeMillis() ; 
        
        // One quick note: View layout and initial measurement happens
        // AFTER onCreate.  Therefore, we can't actually get the size
        // of any of these elements yet.  Some calculations are based
        // the height of a list item, which -> the number of Menu items 
        // per page, which -> the number of possible layers, etc.
        // Do this stuff in an "initial setup runnable" on the UI thread,
        // that delays itself until height information is available.
        mHandler = new Handler() ;
        mInitialSetupRunnable = new Runnable() {
        	public void run() {
        		
        		Log.d(TAG, "initialSetupRunnable.run() bracket IN") ;
        		if ( mHasInitialSetup || !mInFront ) {
        			Log.d(TAG, "initialSetupRunnable.run() bracket OUT (initial setup or not in front") ;
        			return ;
        		}
                
        		int viewFlipperHeight = mViewFlipper.getMeasuredHeight() ;
                if ( viewFlipperHeight == 0 ) {
                	// Re-post if we're not ready yet.
                	Log.d(TAG, "re-posting mInitialSetupRunnable - mViewFlipper height is zero") ;
                	mHandler.postDelayed(this, 10) ;
                	Log.d(TAG, "initialSetupRunnable.run() bracket OUT (measured height zero)") ;
                	return ;
                }
        		
                // size up the length of a list item, figure out how many can
                // fit on the screen w/o scrolling, and the compress to that
                // width.
                View menuListItem = getLayoutInflater().inflate(R.layout.main_menu_list_item, null);
                double height = onCreate_getMenuItemHeight( menuListItem ) ;
                int itemsPerRow = onCreate_getMenuItemsPerRow( menuListItem ) ;
                Log.d(TAG, "row height: " + height) ;
                Log.d(TAG, "flipper height: " + viewFlipperHeight ) ;
                Log.d(TAG, "items per row: " + itemsPerRow ) ;
                int n = (int)Math.floor( viewFlipperHeight / height ) ;
                mMenuRowsPerPage = n ;
                mMenuItemsPerRow = itemsPerRow ;
                mHasInitialSetup = true ;
        	}
        } ;
        
        Log.d(TAG, "onCreate made initial setup runnable in " + (System.currentTimeMillis() - time) + "millis") ;
        time = System.currentTimeMillis() ; 
        
        // DO NOT REGISTER.  We don't currently use C2DM, so there's no
        // need for this.
        // C2DMReceiver.refreshAppC2DMRegistrationState(this) ;
        //Log.d(TAG, "onCreate bracket OUT") ;
        
        
        
        // initial allocation of AsyncSetup structures.
        mAsyncSetupComplete = new boolean[NUM_ASYNC_SETUP_TASKS] ;
        mAsyncSetupTask = new AsyncTask[NUM_ASYNC_SETUP_TASKS] ;
        for ( int i = 0; i < NUM_ASYNC_SETUP_TASKS; i++ ) {
        	mAsyncSetupComplete[i] = false ;
        	mAsyncSetupTask[i] = null ;
        }
        mAsyncSetupIsWakeLocked = false ;
        mAsyncSetupIsComplete = false ;
        
        
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
		
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // One-time loaders, constructors, configurers, etc.
    //
    ////////////////////////////////////////////////////////////////////////////
    
    private GameBlocksSliceSequence onCreate_makeLogo() {
    	GameBlocksSliceSequence gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/main/logo.txt") ) ;
    	//Log.d(TAG, "onCreate_makeLogo bracket OUT") ;
    	return gbss ;
    }
    
    private void onCreate_getMenuViews( Resources res ) {
    	//Log.d(TAG, "onCreate_getMenuViews bracket IN") ;
    	mTitleBarButtonStrip = (MainMenuTitleBarButtonStrip)findViewById(R.id.main_menu_title_bar_button_strip ) ;
    	mViewFlipper = (ViewFlipper)findViewById(R.id.main_menu_list_flipper ) ;
    	// mInstructionsTextView = (TextView)findViewById(R.id.main_menu_instructions) ;
    	//Log.d(TAG, "onCreate_getMenuViews bracket OUT") ;
    }
    
    private void onCreate_configureMenuViews( Resources res ) {
    	//Log.d(TAG, "onCreate_configureMenuViews bracket IN") ;
    	////////////////////////////////////////////////////////////////////////
    	// CONFIGURE TITLE BAR
    	
    	// Set ourself as the listener.
    	mTitleBarButtonStrip.setDelegate(this) ;
    	
    	/*
    	// Set up a LineShadingDrawable as the background to our title bar button strip.
    	LineShadingDrawable lsd = new LineShadingDrawable() ;
		lsd.setColor(MAIN_TITLE_LSD_LINE_COLOR) ;
		lsd.setLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				MAIN_TITLE_LSD_LINE_WIDTH, getResources().getDisplayMetrics())) ;
		lsd.setLineSeparation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				MAIN_TITLE_LSD_LINE_SEPARATION, getResources().getDisplayMetrics())) ;
		VersionSafe.setBackground(mTitleBarButtonStrip, lsd) ;
		
		// Set up a LineShadingDrawable as a button wrapper drawable.
		switch( mMainTitleStyle ) {
		case MAIN_TITLE_STYLE_NONE:
			lsd = null ;
			break ;
			
		case MAIN_TITLE_STYLE_OVER:
			// exactly over the above.
			lsd = new LineShadingDrawable() ;
			lsd.setColor(MAIN_TITLE_LSD_LINE_COLOR) ;
			lsd.setLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_WIDTH, getResources().getDisplayMetrics())) ;
			lsd.setLineSeparation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_SEPARATION, getResources().getDisplayMetrics())) ;
			break ;
			
		case MAIN_TITLE_STYLE_OFFSET:
			// exact same dimensions, but with 1/2 separation as its offset.
			lsd = new LineShadingDrawable() ;
			lsd.setColor(MAIN_TITLE_LSD_LINE_COLOR) ;
			lsd.setLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_WIDTH, getResources().getDisplayMetrics())) ;
			lsd.setLineSeparation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_SEPARATION, getResources().getDisplayMetrics())) ;
			lsd.setLineOffset(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_SEPARATION/2, getResources().getDisplayMetrics())) ;
			break ;
			
		case MAIN_TITLE_STYLE_OVER_DOUBLE:
			// exact same dimensions EXCEPT for 1/2 separation.
			lsd = new LineShadingDrawable() ;
			lsd.setColor(MAIN_TITLE_LSD_LINE_COLOR) ;
			lsd.setLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_WIDTH, getResources().getDisplayMetrics())) ;
			lsd.setLineSeparation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_SEPARATION/2, getResources().getDisplayMetrics())) ;
			break ;
			
		case MAIN_TITLE_STYLE_OVER_DOUBLE_FADE_BUTTONS:
			// exact same dimensions EXCEPT 1/2 separation.
			// Color has fade alpha applied to it.
			lsd = new LineShadingDrawable() ;
			lsd.setColor( MAIN_TITLE_LSD_LINE_COLOR ) ;
			lsd.setLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_WIDTH, getResources().getDisplayMetrics())) ;
			lsd.setLineSeparation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					MAIN_TITLE_LSD_LINE_SEPARATION/2, getResources().getDisplayMetrics())) ;
			// We also configure the buttons of the title bar to use alpha gradients from 1 to 0.
			for ( int i = 0; i < mTitleBarButtonStrip.getButtonCount(); i++ ) {
				QuantroButton qb = (QuantroButton)mTitleBarButtonStrip.getButton(i) ;
				qb.resetToDefaultAppearance(
						MAIN_TITLE_LSD_LINE_FADE_ALPHA_MULT,
						MAIN_TITLE_LSD_LINE_FADE_ALPHA_MULT/2,
						MAIN_TITLE_LSD_LINE_FADE_ALPHA_MULT,
						new float[]{MAIN_TITLE_LSD_LINE_FADE_ALPHA_MULT,0}) ;
			}
		}
		mTitleBarButtonStrip.setButtonWrapperDrawable(lsd) ;
		*/
    	
		////////////////////////////////////////////////////////////////////////
		// CONFIGURE FLIPPER
    	// TODO: set flipper animations
		
		//Log.d(TAG, "onCreate_configureMenuViews bracket OUT") ;
    }
    
    private void onCreate_allocateMenuItemStructures( Resources res ) {
    	//Log.d(TAG, "onCreate_allocateMenuItemStructures bracket IN") ;
    	NUM_MENU_ITEMS 					= res.getInteger(R.integer.main_menu_num_items) ;
    	MENU_ITEM_INTEGER_OBJECT = new Integer[NUM_MENU_ITEMS] ;
    	for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
    		MENU_ITEM_INTEGER_OBJECT[i] = new Integer(i) ;
    	
    	mMenuItemCohesion = new int[NUM_MENU_ITEMS] ;
    	mMenuItemTitles = new String[NUM_MENU_ITEMS];
    	mMenuItemDescriptions = new String[NUM_MENU_ITEMS] ;
    	mMenuItemDescriptionsDisabled = new String[NUM_MENU_ITEMS] ;
    	mMenuItemInstructions = new String[NUM_MENU_ITEMS] ;
    	mMenuItemColors = new int[NUM_MENU_ITEMS] ;
    	mMenuItemAlerts = new int[NUM_MENU_ITEMS] ;
    	for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
    		mMenuItemAlerts[i] = MainMenuButtonStrip.ALERT_NONE ;
    	mMenuItemAlertOverwriteDrawable = new Drawable[NUM_MENU_ITEMS] ;
    	//Log.d(TAG, "onCreate_allocateMenuItemStructures bracket OUT") ;
    }
    
    private void onCreate_getMenuRoot( Resources res ) {
    	//Log.d(TAG, "onCreate_getMenuRoot bracket IN") ;
    	MENU_ITEM_ROOT 					= res.getInteger(R.integer.main_menu_root_item) ;
    	
    	mMenuItemRootTitle = res.getString(R.string.main_menu_root_title) ;
    	mMenuItemRootDescription = res.getString(R.string.main_menu_root_description) ;
    	mMenuItemRootXLTitle = res.getString(R.string.main_menu_root_xl_title) ;
    	mMenuItemRootXLDescription = res.getString(R.string.main_menu_root_xl_description) ;

    	// title and description set in onResume.
    	mMenuItemCohesion 		[MENU_ITEM_ROOT] 	= res.getInteger(R.integer.main_menu_root_cohesion) ;
    	//mMenuItemTitles			[MENU_ITEM_ROOT] 	= res.getString(R.string.main_menu_root_title) ;
    	//mMenuItemDescriptions	[MENU_ITEM_ROOT] 	= res.getString(R.string.main_menu_root_description) ;
    	mMenuItemInstructions	[MENU_ITEM_ROOT] 	= res.getString(R.string.main_menu_root_instructions) ;
    	mMenuItemColors			[MENU_ITEM_ROOT] 	= res.getColor(R.color.main_menu_root_color) ;
    	//Log.d(TAG, "onCreate_getMenuRoot bracket OUT") ;
    }
    
    
    private void onCreate_getMenuItems( Resources res ) {
    	//Log.d(TAG, "onCreate_getMenuItems bracket IN") ;
    	MENU_ITEM_SINGLE_PLAYER			= res.getInteger(R.integer.main_menu_single_player_item) ;
    	MENU_ITEM_MULTI_PLAYER 			= res.getInteger(R.integer.main_menu_multi_player_item) ;
    	MENU_ITEM_SOCIAL_AND_SETUP		= res.getInteger(R.integer.main_menu_social_and_setup_item) ;
    	MENU_ITEM_INFORMATION 			= res.getInteger(R.integer.main_menu_information_item) ;
    	MENU_ITEM_WIFI_MULTI_PLAYER 	= res.getInteger(R.integer.main_menu_wifi_multi_player_item) ;
    	MENU_ITEM_INTERNET_MULTI_PLAYER = res.getInteger(R.integer.main_menu_internet_multi_player_item) ;
    	MENU_ITEM_SOCIAL				= res.getInteger(R.integer.main_menu_social_item) ;
    	MENU_ITEM_ACHIEVEMENTS			= res.getInteger(R.integer.main_menu_achievements_item) ;
    	MENU_ITEM_LEADERBOARDS			= res.getInteger(R.integer.main_menu_leaderboards_item) ;
    	MENU_ITEM_SETUP					= res.getInteger(R.integer.main_menu_setup_item) ;
    	MENU_ITEM_SETTINGS				= res.getInteger(R.integer.main_menu_settings_item) ;
    	MENU_ITEM_HOW_TO_PLAY 			= res.getInteger(R.integer.main_menu_how_to_play_item) ;
    	MENU_ITEM_APP_INFO				= res.getInteger(R.integer.main_menu_app_info_item) ;
    	MENU_ITEM_FEEDBACK 				= res.getInteger(R.integer.main_menu_feedback_item) ;
    	MENU_ITEM_POLICIES				= res.getInteger(R.integer.main_menu_policies_item) ;
    	MENU_ITEM_USER_LICENSE 			= res.getInteger(R.integer.main_menu_user_license_item) ;
    	MENU_ITEM_PRIVACY_POLICY 		= res.getInteger(R.integer.main_menu_privacy_policy_item) ;
    	MENU_ITEM_UPDATES				= res.getInteger(R.integer.main_menu_updates_item) ; 
    	MENU_ITEM_WHATS_NEW				= res.getInteger(R.integer.main_menu_whats_new_item) ; 
    	MENU_ITEM_VERSION_HISTORY		= res.getInteger(R.integer.main_menu_version_history_item) ; 
    	MENU_ITEM_CREDITS 				= res.getInteger(R.integer.main_menu_credits_item) ;
    	
    	MENU_ITEM_HOW_TO_PLAY_BASICS 				= res.getInteger(R.integer.main_menu_how_to_play_basics_item) ;
    	MENU_ITEM_HOW_TO_PLAY_ADVANCED 				= res.getInteger(R.integer.main_menu_how_to_play_advanced_item) ;
    	
    	MENU_ITEM_HOW_TO_PLAY_GAME_BASICS 			= res.getInteger(R.integer.main_menu_how_to_play_game_basics_item) ;
    	MENU_ITEM_HOW_TO_PLAY_CONTROLS 				= res.getInteger(R.integer.main_menu_how_to_play_controls_item) ;
    	MENU_ITEM_HOW_TO_PLAY_GAME_MODES 			= res.getInteger(R.integer.main_menu_how_to_play_game_modes_item) ;
    	MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES 		= res.getInteger(R.integer.main_menu_how_to_play_special_pieces_item) ;
    	MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES 	= res.getInteger(R.integer.main_menu_how_to_play_advanced_techniques_item) ;
    	
    	MENU_ITEM_FEEDBACK_EMAIL 					= res.getInteger(R.integer.main_menu_feedback_email_item) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_SINGLE_PLAYER] 	= res.getInteger(R.integer.main_menu_single_player_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_SINGLE_PLAYER] 	= res.getString(R.string.main_menu_single_player_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_SINGLE_PLAYER] 	= res.getString(R.string.main_menu_single_player_description) ;
    	mMenuItemInstructions	[MENU_ITEM_SINGLE_PLAYER] 	= res.getString(R.string.main_menu_single_player_instructions) ;
    	mMenuItemColors			[MENU_ITEM_SINGLE_PLAYER] 	= res.getColor(R.color.main_menu_single_player_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_MULTI_PLAYER] 	= res.getInteger(R.integer.main_menu_multi_player_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_MULTI_PLAYER] 	= res.getString(R.string.main_menu_multi_player_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_MULTI_PLAYER] 	= res.getString(R.string.main_menu_multi_player_description) ;
    	mMenuItemDescriptionsDisabled[MENU_ITEM_MULTI_PLAYER] = res.getString(R.string.main_menu_multi_player_description_disabled) ;
    	mMenuItemInstructions	[MENU_ITEM_MULTI_PLAYER] 	= res.getString(R.string.main_menu_multi_player_instructions) ;
    	mMenuItemColors			[MENU_ITEM_MULTI_PLAYER] 	= res.getColor(R.color.main_menu_multi_player_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_SOCIAL_AND_SETUP] 	= res.getInteger(R.integer.main_menu_social_and_setup_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_SOCIAL_AND_SETUP] 	= res.getString(R.string.main_menu_social_and_setup_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_SOCIAL_AND_SETUP] 	= res.getString(R.string.main_menu_social_and_setup_description) ;
    	mMenuItemInstructions	[MENU_ITEM_SOCIAL_AND_SETUP] 	= res.getString(R.string.main_menu_social_and_setup_instructions) ;
    	mMenuItemColors			[MENU_ITEM_SOCIAL_AND_SETUP] 	= res.getColor(R.color.main_menu_social_and_setup_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_WIFI_MULTI_PLAYER] 	= res.getInteger(R.integer.main_menu_wifi_multi_player_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_WIFI_MULTI_PLAYER] 	= res.getString(R.string.main_menu_wifi_multi_player_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_WIFI_MULTI_PLAYER] 	= res.getString(R.string.main_menu_wifi_multi_player_description) ;
    	mMenuItemDescriptionsDisabled[MENU_ITEM_WIFI_MULTI_PLAYER] = res.getString(R.string.main_menu_wifi_multi_player_description_disabled) ;
    	mMenuItemInstructions	[MENU_ITEM_WIFI_MULTI_PLAYER] 	= res.getString(R.string.main_menu_wifi_multi_player_instructions) ;
    	mMenuItemColors			[MENU_ITEM_WIFI_MULTI_PLAYER] 	= res.getColor(R.color.main_menu_wifi_multi_player_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_INTERNET_MULTI_PLAYER] 	= res.getInteger(R.integer.main_menu_internet_multi_player_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_INTERNET_MULTI_PLAYER] 	= res.getString(R.string.main_menu_internet_multi_player_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_INTERNET_MULTI_PLAYER] 	= res.getString(R.string.main_menu_internet_multi_player_description) ;
    	mMenuItemDescriptionsDisabled[MENU_ITEM_INTERNET_MULTI_PLAYER] = res.getString(R.string.main_menu_internet_multi_player_description_disabled) ;
    	mMenuItemInstructions	[MENU_ITEM_INTERNET_MULTI_PLAYER] 	= res.getString(R.string.main_menu_internet_multi_player_instructions) ;
    	mMenuItemColors			[MENU_ITEM_INTERNET_MULTI_PLAYER] 	= res.getColor(R.color.main_menu_internet_multi_player_color) ;

    	// TODO include icons for leaderboard / achievement services
    	mMenuItemCohesion 		[MENU_ITEM_SOCIAL] 	= res.getInteger(R.integer.main_menu_social_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_SOCIAL] 	= res.getString(R.string.main_menu_social_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_SOCIAL] 	= res.getString(R.string.main_menu_social_description) ;
    	mMenuItemInstructions	[MENU_ITEM_SOCIAL] 	= res.getString(R.string.main_menu_social_instructions) ;
    	mMenuItemColors			[MENU_ITEM_SOCIAL] 	= res.getColor(R.color.main_menu_social_color) ;
    	mMenuItemAlertOverwriteDrawable[MENU_ITEM_SOCIAL] = res.getDrawable(R.drawable.action_leaderboard_baked) ;	// TODO custom icon
    	
    	mMenuItemCohesion 		[MENU_ITEM_ACHIEVEMENTS] 	= res.getInteger(R.integer.main_menu_achievements_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_ACHIEVEMENTS] 	= res.getString(R.string.main_menu_achievements_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_ACHIEVEMENTS] 	= res.getString(R.string.main_menu_achievements_description) ;
    	mMenuItemInstructions	[MENU_ITEM_ACHIEVEMENTS] 	= res.getString(R.string.main_menu_achievements_instructions) ;
    	mMenuItemColors			[MENU_ITEM_ACHIEVEMENTS] 	= res.getColor(R.color.main_menu_achievements_color) ;
    	mMenuItemAlertOverwriteDrawable[MENU_ITEM_ACHIEVEMENTS] = res.getDrawable(R.drawable.action_leaderboard_baked) ; // TODO custom icon
    	
    	mMenuItemCohesion 		[MENU_ITEM_LEADERBOARDS] 	= res.getInteger(R.integer.main_menu_leaderboards_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_LEADERBOARDS] 	= res.getString(R.string.main_menu_leaderboards_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_LEADERBOARDS] 	= res.getString(R.string.main_menu_leaderboards_description) ;
    	mMenuItemInstructions	[MENU_ITEM_LEADERBOARDS] 	= res.getString(R.string.main_menu_leaderboards_instructions) ;
    	mMenuItemColors			[MENU_ITEM_LEADERBOARDS] 	= res.getColor(R.color.main_menu_leaderboards_color) ;
    	mMenuItemAlertOverwriteDrawable[MENU_ITEM_LEADERBOARDS] = res.getDrawable(R.drawable.action_leaderboard_baked) ;	// TODO custom icon
    	
    	mMenuItemCohesion 		[MENU_ITEM_SETUP] 	= res.getInteger(R.integer.main_menu_setup_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_SETUP] 	= res.getString(R.string.main_menu_setup_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_SETUP] 	= res.getString(R.string.main_menu_setup_description) ;
    	mMenuItemInstructions	[MENU_ITEM_SETUP] 	= res.getString(R.string.main_menu_setup_instructions) ;
    	mMenuItemColors			[MENU_ITEM_SETUP] 	= res.getColor(R.color.main_menu_setup_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_SETTINGS] 	= res.getInteger(R.integer.main_menu_settings_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_SETTINGS] 	= res.getString(R.string.main_menu_settings_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_SETTINGS] 	= res.getString(R.string.main_menu_settings_description) ;
    	mMenuItemInstructions	[MENU_ITEM_SETTINGS] 	= res.getString(R.string.main_menu_settings_instructions) ;
    	mMenuItemColors			[MENU_ITEM_SETTINGS] 	= res.getColor(R.color.main_menu_settings_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_INFORMATION] 	= res.getInteger(R.integer.main_menu_information_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_INFORMATION] 	= res.getString(R.string.main_menu_information_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_INFORMATION] 	= res.getString(R.string.main_menu_information_description) ;
    	mMenuItemInstructions	[MENU_ITEM_INFORMATION] 	= res.getString(R.string.main_menu_information_instructions) ;
    	mMenuItemColors			[MENU_ITEM_INFORMATION] 	= res.getColor(R.color.main_menu_information_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY] 	= res.getInteger(R.integer.main_menu_how_to_play_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY] 	= res.getString(R.string.main_menu_how_to_play_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY] 	= res.getString(R.string.main_menu_how_to_play_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY] 	= res.getString(R.string.main_menu_how_to_play_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY] 	= res.getColor(R.color.main_menu_how_to_play_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_APP_INFO] 	= res.getInteger(R.integer.main_menu_app_info_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_APP_INFO] 	= res.getString(R.string.main_menu_app_info_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_APP_INFO] 	= res.getString(R.string.main_menu_app_info_description) ;
    	mMenuItemInstructions	[MENU_ITEM_APP_INFO] 	= res.getString(R.string.main_menu_app_info_instructions) ;
    	mMenuItemColors			[MENU_ITEM_APP_INFO] 	= res.getColor(R.color.main_menu_app_info_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_FEEDBACK] 	= res.getInteger(R.integer.main_menu_feedback_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_FEEDBACK] 	= res.getString(R.string.main_menu_feedback_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_FEEDBACK] 	= res.getString(R.string.main_menu_feedback_description) ;
    	mMenuItemInstructions	[MENU_ITEM_FEEDBACK] 	= res.getString(R.string.main_menu_feedback_instructions) ;
    	mMenuItemColors			[MENU_ITEM_FEEDBACK] 	= res.getColor(R.color.main_menu_feedback_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_POLICIES] 	= res.getInteger(R.integer.main_menu_policies_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_POLICIES] 	= res.getString(R.string.main_menu_policies_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_POLICIES] 	= res.getString(R.string.main_menu_policies_description) ;
    	mMenuItemInstructions	[MENU_ITEM_POLICIES] 	= res.getString(R.string.main_menu_policies_instructions) ;
    	mMenuItemColors			[MENU_ITEM_POLICIES] 	= res.getColor(R.color.main_menu_policies_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_USER_LICENSE] 	= res.getInteger(R.integer.main_menu_user_license_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_USER_LICENSE] 	= res.getString(R.string.main_menu_user_license_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_USER_LICENSE] 	= res.getString(R.string.main_menu_user_license_description) ;
    	mMenuItemInstructions	[MENU_ITEM_USER_LICENSE] 	= res.getString(R.string.main_menu_user_license_instructions) ;
    	mMenuItemColors			[MENU_ITEM_USER_LICENSE] 	= res.getColor(R.color.main_menu_user_license_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_PRIVACY_POLICY] 	= res.getInteger(R.integer.main_menu_privacy_policy_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_PRIVACY_POLICY] 	= res.getString(R.string.main_menu_privacy_policy_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_PRIVACY_POLICY] 	= res.getString(R.string.main_menu_privacy_policy_description) ;
    	mMenuItemInstructions	[MENU_ITEM_PRIVACY_POLICY] 	= res.getString(R.string.main_menu_privacy_policy_instructions) ;
    	mMenuItemColors			[MENU_ITEM_PRIVACY_POLICY] 	= res.getColor(R.color.main_menu_privacy_policy_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_UPDATES] 		= res.getInteger(R.integer.main_menu_updates_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_UPDATES] 		= res.getString(R.string.main_menu_updates_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_UPDATES] 		= res.getString(R.string.main_menu_updates_description) ;
    	mMenuItemInstructions	[MENU_ITEM_UPDATES] 		= res.getString(R.string.main_menu_updates_instructions) ;
    	mMenuItemColors			[MENU_ITEM_UPDATES] 		= res.getColor(R.color.main_menu_updates_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_WHATS_NEW] 		= res.getInteger(R.integer.main_menu_whats_new_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_WHATS_NEW] 		= res.getString(R.string.main_menu_whats_new_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_WHATS_NEW] 		= res.getString(R.string.main_menu_whats_new_description) ;
    	mMenuItemInstructions	[MENU_ITEM_WHATS_NEW] 		= res.getString(R.string.main_menu_whats_new_instructions) ;
    	mMenuItemColors			[MENU_ITEM_WHATS_NEW] 		= res.getColor(R.color.main_menu_whats_new_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_VERSION_HISTORY] 	= res.getInteger(R.integer.main_menu_version_history_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_VERSION_HISTORY] 	= res.getString(R.string.main_menu_version_history_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_VERSION_HISTORY] 	= res.getString(R.string.main_menu_version_history_description) ;
    	mMenuItemInstructions	[MENU_ITEM_VERSION_HISTORY] 	= res.getString(R.string.main_menu_version_history_instructions) ;
    	mMenuItemColors			[MENU_ITEM_VERSION_HISTORY] 	= res.getColor(R.color.main_menu_version_history_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_CREDITS] 	= res.getInteger(R.integer.main_menu_credits_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_CREDITS] 	= res.getString(R.string.main_menu_credits_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_CREDITS] 	= res.getString(R.string.main_menu_credits_description) ;
    	mMenuItemInstructions	[MENU_ITEM_CREDITS] 	= res.getString(R.string.main_menu_credits_instructions) ;
    	mMenuItemColors			[MENU_ITEM_CREDITS] 	= res.getColor(R.color.main_menu_credits_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY_BASICS] 	= res.getInteger(R.integer.main_menu_how_to_play_basics_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY_BASICS] 	= res.getString(R.string.main_menu_how_to_play_basics_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY_BASICS] 	= res.getString(R.string.main_menu_how_to_play_basics_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY_BASICS] 	= res.getString(R.string.main_menu_how_to_play_basics_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY_BASICS] 	= res.getColor(R.color.main_menu_how_to_play_basics_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY_ADVANCED] 	= res.getInteger(R.integer.main_menu_how_to_play_advanced_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY_ADVANCED] 	= res.getString(R.string.main_menu_how_to_play_advanced_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY_ADVANCED] 	= res.getString(R.string.main_menu_how_to_play_advanced_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY_ADVANCED] 	= res.getString(R.string.main_menu_how_to_play_advanced_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY_ADVANCED] 	= res.getColor(R.color.main_menu_how_to_play_advanced_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY_GAME_BASICS] 	= res.getInteger(R.integer.main_menu_how_to_play_game_basics_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY_GAME_BASICS] 	= res.getString(R.string.main_menu_how_to_play_game_basics_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY_GAME_BASICS] 	= res.getString(R.string.main_menu_how_to_play_game_basics_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY_GAME_BASICS] 	= res.getString(R.string.main_menu_how_to_play_game_basics_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY_GAME_BASICS] 	= res.getColor(R.color.main_menu_how_to_play_game_basics_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY_CONTROLS] 	= res.getInteger(R.integer.main_menu_how_to_play_controls_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY_CONTROLS] 	= res.getString(R.string.main_menu_how_to_play_controls_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY_CONTROLS] 	= res.getString(R.string.main_menu_how_to_play_controls_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY_CONTROLS] 	= res.getString(R.string.main_menu_how_to_play_controls_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY_CONTROLS] 	= res.getColor(R.color.main_menu_how_to_play_controls_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY_GAME_MODES] 	= res.getInteger(R.integer.main_menu_how_to_play_game_modes_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY_GAME_MODES] 	= res.getString(R.string.main_menu_how_to_play_game_modes_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY_GAME_MODES] 	= res.getString(R.string.main_menu_how_to_play_game_modes_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY_GAME_MODES] 	= res.getString(R.string.main_menu_how_to_play_game_modes_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY_GAME_MODES] 	= res.getColor(R.color.main_menu_how_to_play_game_modes_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES] 	= res.getInteger(R.integer.main_menu_how_to_play_special_pieces_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES] 	= res.getString(R.string.main_menu_how_to_play_special_pieces_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES] 	= res.getString(R.string.main_menu_how_to_play_special_pieces_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES] 	= res.getString(R.string.main_menu_how_to_play_special_pieces_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES] 	= res.getColor(R.color.main_menu_how_to_play_special_pieces_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES] 	= res.getInteger(R.integer.main_menu_how_to_play_advanced_techniques_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES] 	= res.getString(R.string.main_menu_how_to_play_advanced_techniques_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES] 	= res.getString(R.string.main_menu_how_to_play_advanced_techniques_description) ;
    	mMenuItemInstructions	[MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES] 	= res.getString(R.string.main_menu_how_to_play_advanced_techniques_instructions) ;
    	mMenuItemColors			[MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES] 	= res.getColor(R.color.main_menu_how_to_play_advanced_techniques_color) ;
    	
    	mMenuItemCohesion 		[MENU_ITEM_FEEDBACK_EMAIL] 	= res.getInteger(R.integer.main_menu_feedback_email_cohesion) ;
    	mMenuItemTitles			[MENU_ITEM_FEEDBACK_EMAIL] 	= res.getString(R.string.main_menu_feedback_email_title) ;
    	mMenuItemDescriptions	[MENU_ITEM_FEEDBACK_EMAIL] 	= res.getString(R.string.main_menu_feedback_email_description) ;
    	mMenuItemInstructions	[MENU_ITEM_FEEDBACK_EMAIL] 	= res.getString(R.string.main_menu_feedback_email_instructions) ;
    	mMenuItemColors			[MENU_ITEM_FEEDBACK_EMAIL] 	= res.getColor(R.color.main_menu_feedback_email_color) ;
    	
    	
    	
    	// special storage for these!
    	mMenuItemGameSetupTitle = res.getString(R.string.main_menu_setup_title) ;
    	mMenuItemGameSetupDescription = res.getString(R.string.main_menu_setup_description) ;
    	mMenuItemGameSetupXLTitle = res.getString(R.string.main_menu_setup_xl_title) ;
    	mMenuItemGameSetupXLDescription = res.getString(R.string.main_menu_setup_xl_description) ;
    	
    	// pick one!
    	if ( getPremiumLibrary().hasHideAds() ) {
    		mMenuItemTitles			[MENU_ITEM_SETUP]			= mMenuItemGameSetupXLTitle ;
    		mMenuItemDescriptions	[MENU_ITEM_SETUP]			= mMenuItemGameSetupXLDescription ;
    	}
    }
    
    private void onCreate_setTitleBarMenuItems( Resources res ) {
    	//Log.d(TAG, "onCreate_setTitleBarMenuItems bracket IN") ;
    	mTitleBarButtonStrip.setMenuItemTitles(mMenuItemTitles) ;
    	mTitleBarButtonStrip.setMenuItemDescriptions(mMenuItemDescriptions) ;
    	mTitleBarButtonStrip.setMenuItemColors(mMenuItemColors) ;
    	mTitleBarButtonStrip.setMenuItemDrawables(mMenuItemAlertOverwriteDrawable) ;
    	
    	mTitleBarButtonStrip.setMenuItem(MENU_ITEM_ROOT) ;
    }
    
    
    private Integer [] makeSinglePlayerGameModesArray( Resources res ) {
    	
    	int maxCustomGameModeSettings = res.getInteger(R.integer.custom_game_mode_max_saved) ;
    	int numSaved = CustomGameModeSettingsDatabaseAdapter.count(this) ;
    	boolean newButton = numSaved < maxCustomGameModeSettings ;
    	
    	int [] modes = res.getIntArray(R.array.game_modes_single_player) ;
    	int [] customModes = GameModes.getCustomGameModes(true, false) ;
    	// wrap in Integer objects
		Integer [] gameModes = new Integer[modes.length + customModes.length + (newButton ? 1 : 0)] ;
		for ( int gm = 0; gm < modes.length; gm++ )
			gameModes[gm] = modes[gm] ;
		// add custom game modes
		for ( int gm = 0; gm < customModes.length; gm++ )
			gameModes[gm + modes.length] = customModes[gm] ;
		// Add the "new custom mode" button
		if ( newButton )
			gameModes[modes.length + customModes.length] = SinglePlayerGameLaunchButtonStrip.GAME_MODE_NEW_CUSTOM_GAME_MODE ;
		
		Log.d(TAG, "makeSinglePlayerGameModesArray: have "+ gameModes.length + " game modes " ) ;
		return gameModes ;
    }
    
    private void onCreate_setTitleBarSliceSequence( Resources res, GameBlocksSliceSequence sliceSequence ) {
    	//Log.d(TAG, "onCreate_setTitleBarSliceSequence bracket IN") ;
    	// TODO: Different stuff for unlockable version.
    	DrawSettings ds = newLogoDrawSettings( res, true ) ;
    	mTitleBarButtonStrip.setSliceSequence(sliceSequence, 3, 4, ds) ;
    }
    
    
    /**
     * If 'force' is true, or if this method previously produced a DrawSettings object which
     * is significantly different from the one to be returned, returns a new DrawSettings
     * instance.  Otherwise, returns 'null.'
     * 
     * In other words, calling this method in onResume is completely safe; it will return 'null'
     * if the logo drawSettings do NOT need an update, and a DrawSettings instance if they do.
     * @param res
     * @param force
     * @return
     */
    private int newLogoDrawSettingsLastDetail ;
    private Skin newLogoDrawSettingsLastSkin ;
    private boolean newLogoDrawSettingsWasCalled = false ;
    private DrawSettings newLogoDrawSettings( Resources res, boolean force ) {
    	//Log.d(TAG, "newLogoDrawSettings bracket IN") ;
    	
    	int detail = QuantroPreferences.getGraphicsGraphicalDetail(this) ;
    	

    	force = force || newLogoDrawSettingsLastDetail != detail ;
    	force = force || !newLogoDrawSettingsLastSkin.equals( mQuantroSkin );
    	force = force || !newLogoDrawSettingsWasCalled ;
    	
    	if ( !force ) {
    		Log.d(TAG, "newLogoDrawSettings bracket OUT (not force)") ;
    		return null ;
    	}

    	newLogoDrawSettingsLastDetail = detail ;
		newLogoDrawSettingsLastSkin = mQuantroSkin ;
		newLogoDrawSettingsWasCalled = true ;
    	
    	DrawSettings ds = new DrawSettings(
				new Rect(0,0,100,100),
				6, 		// rows
				3, 		// cols
				4,		// displayed rows
				QuantroPreferences.getGraphicsGraphicalDetail(this),
				QuantroPreferences.getGraphicsSkipAnimations(this)
						? DrawSettings.DRAW_ANIMATIONS_STABLE_STUTTER
						: DrawSettings.DRAW_ANIMATIONS_ALL,
				mQuantroSkin,
				this ) ;
    	ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_BLOCK_FILL ;
    	ds.horizontalMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)1, getResources().getDisplayMetrics()) ;
    	ds.verticalMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)1, getResources().getDisplayMetrics()) ;
    	ds.behavior_align_horizontal = DrawSettings.BEHAVIOR_ALIGN_CENTER_GRID ;
    	//Log.d(TAG, "newLogoDrawSettings bracket OUT (new ds)") ;
    	return ds ;
    }
    
    private int [][] onCreate_getMenuHierarchy( Resources res ) {
    	//Log.d(TAG, "onCreate_getMenuHierarchy bracket IN") ;
    	int [][] hierarchy = new int[NUM_MENU_ITEMS][] ;
    	for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
    		hierarchy[i] = null ;
    	
    	// We load the hierarchy exactly as represented in the resources
    	// Hierarchy items represent menu items.  Put those we know about in
    	// an array and iterate.
    	TypedArray [] typedArrays = new TypedArray[NUM_MENU_ITEMS] ;
    	for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
    		typedArrays[i] = null ;
    	
    	typedArrays[MENU_ITEM_ROOT] = res.obtainTypedArray(R.array.main_menu_root_contents) ;
    	typedArrays[MENU_ITEM_MULTI_PLAYER] = res.obtainTypedArray(R.array.main_menu_multi_player_contents) ;
    	typedArrays[MENU_ITEM_SOCIAL_AND_SETUP] = res.obtainTypedArray(R.array.main_menu_social_and_setup_contents) ;
    	typedArrays[MENU_ITEM_SOCIAL] = res.obtainTypedArray(R.array.main_menu_social_contents) ;
    	typedArrays[MENU_ITEM_SETUP] = res.obtainTypedArray(R.array.main_menu_setup_contents) ;
    	typedArrays[MENU_ITEM_INFORMATION] = res.obtainTypedArray(R.array.main_menu_information_contents) ;
    	typedArrays[MENU_ITEM_APP_INFO] = res.obtainTypedArray(R.array.main_menu_app_info_contents) ;
    	typedArrays[MENU_ITEM_POLICIES] = res.obtainTypedArray(R.array.main_menu_policies_contents) ;
    	typedArrays[MENU_ITEM_FEEDBACK] = res.obtainTypedArray(R.array.main_menu_feedback_contents) ;
    	typedArrays[MENU_ITEM_UPDATES] = res.obtainTypedArray(R.array.main_menu_updates_contents) ;
    	typedArrays[MENU_ITEM_HOW_TO_PLAY] = res.obtainTypedArray(R.array.main_menu_how_to_play_contents) ;
    	typedArrays[MENU_ITEM_HOW_TO_PLAY_BASICS] = res.obtainTypedArray(R.array.main_menu_how_to_play_basics_contents) ;
    	typedArrays[MENU_ITEM_HOW_TO_PLAY_ADVANCED] = res.obtainTypedArray(R.array.main_menu_how_to_play_advanced_contents) ;
    	

    	for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
    		if ( typedArrays[i] != null )
    			onCreate_getMenuHierarchyItem( i, hierarchy, typedArrays[i] ) ;
    	
    	//Log.d(TAG, "onCreate_getMenuHierarchy bracket OUT") ;
    	return hierarchy ;
    }
    
    
    /**
     * Reduces the menu hierarchy by removing any unsupported item.
     * @param hierarchy
     * @return
     */
    private int [][] onCreate_reduceMenuHierarchy( int [][] hierarchy ) {
    	// if we have popup menus, we do NOT show Google Play stuff in the menu.
    	// We also remove this if the device does not support Google Play Games.
    	if ( VersionCapabilities.supportsPopupMenu() || !VersionCapabilities.supportsGooglePlayGames() ) {
    		hierarchy[MENU_ITEM_SOCIAL] = null ;
    		// remove SOCIAL where it appears.
    		for ( int i = 0; i < hierarchy.length; i++ ) {
    			if ( hierarchy[i] != null ) {
	    			int j = 0 ;
	    			while ( j < hierarchy[i].length ) {
	    				if ( hierarchy[i][j] == MENU_ITEM_SOCIAL ) {
	    					int [] a = new int[hierarchy[i].length -1] ;
	    					for ( int k = 0; k < a.length; k++ ) {
	    						a[k] = hierarchy[i][k < j ? k : k+1] ;
	    					}
	    					hierarchy[i] = a ;
	    				} else {
	    					j++ ;
	    				}
	    			}
    			}
    		}
    	}
    	
    	return hierarchy ;
    }
    
    
    /**
     * Sets an item as the "category" for each menu hierarchy item.
     * By convention, this "category" is the immediate parent of the item.
     * @param hierarchy
     * @return
     */
    private int [] onCreate_getMenuHierarchyParents( int [][] hierarchy ) {
    	int [] categories = new int[NUM_MENU_ITEMS] ;
    	for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
    		categories[i] = -1 ;
    	for ( int parent = 0; parent < hierarchy.length; parent++ ) {
    		if ( hierarchy[parent] != null ) {
	    		for ( int j = 0; j < hierarchy[parent].length; j++ ) {
	    			int child = hierarchy[parent][j] ;
	    			categories[child] = parent ;
	    		}
    		}
    	}
    	
    	return categories ;
    }
    
    private int onCreate_getMenuItemHeight( View menuItem ) {
    	//Log.d(TAG, "onCreate_getMenuItemHeight bracket IN") ;
    	// TODO: Find a reasonable estimate for the height of an arbitrary
    	// main menu list item.  This may require inflating and measuring one.
    	MenuItemListRowTag rowTag = new MenuItemListRowTag(menuItem) ;
    	ViewGroup rowView = (ViewGroup)menuItem.findViewById(R.id.main_menu_list_row) ;
    	MenuItemListItemTag itemTag = new MenuItemListItemTag(rowView.getChildAt(0)) ;
    	
    	if ( rowTag.mHeaderTextView != null ) {
    		rowTag.mHeaderTextView.setText("AaBbCcGgJjKkPpQqTtWwXxYyZz") ;
    	}
    	menuItem.measure(
    			MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE / 2, MeasureSpec.AT_MOST),
    			MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE / 2, MeasureSpec.AT_MOST)) ;
    	
    	int height = itemTag.mMBS.getIdealHeight() ;
    	if ( rowView.getChildCount() > 1 )
    		height += rowTag.mHeaderView.getMeasuredHeight() ;
    	return height ;
    }
    
    private int onCreate_getMenuItemsPerRow( View menuItem ) {
    	ViewGroup rowView = (ViewGroup)menuItem.findViewById(R.id.main_menu_list_row) ;
    	return rowView.getChildCount() ;
    }
    
    private void onCreate_getMenuHierarchyItem( int item, int [][] hierarchy, TypedArray ta ) {
    	hierarchy[item] = new int[ta.length()] ;
    	for ( int i = 0; i < ta.length(); i++ )
    		hierarchy[item][i] = ta.getInteger(i, -1) ;
    }
    
    private int [][] onCreate_mergeMenuHierarchy( int widthInRows, int itemsPerRow ) {
    	//Log.d(TAG, "onCreate_mergeMenuHierarchy bracket IN") ;
    	// Compress breadth-first, from root down, each item by including the
    	// contents of its immediate children.  Compress each in the order listed;
    	// if no items can be compressed, recur to the children.
    	int [][] menuHierarchy = new int[mMenuHierarchy.length][] ;
    	for ( int i = 0; i < mMenuHierarchy.length; i++ ) {
    		if ( mMenuHierarchy[i] != null ) {
    			menuHierarchy[i] = new int[mMenuHierarchy[i].length] ;
    			for ( int j = 0; j < mMenuHierarchy[i].length; j++ )
    				menuHierarchy[i][j] = mMenuHierarchy[i][j] ;
    		}
    	}
    	
    	//Log.d(TAG, "onCreate_mergeMenuHierarchyItem bracket RECURRING within onCreate_mergeMenuHierarchy") ;
    	onCreate_mergeMenuHierarchyItem( widthInRows, itemsPerRow, menuHierarchy, MENU_ITEM_ROOT ) ;
    	
    	//Log.d(TAG, "onCreate_mergeMenuHierarchy bracket OUT") ;
    	return menuHierarchy ;
    }
    
    private void onCreate_mergeMenuHierarchyItem( int widthInRows, int itemsPerRow, int [][] menuHierarchy, int menuItem ) {
    	//Log.d(TAG, "onCreate_mergeMenuHierarchyItem bracket IN") ;
    	
    	// Only compress those items with content (at least 1).
    	if ( menuHierarchy[menuItem] == null || menuHierarchy[menuItem].length == 0 )
    		return ;
    	
    	// Log.d(TAG, "merging " + menuItem + " to width " + width) ;
    	
    	boolean didCompress = true ;
    	// Iterate until we cannot compress any further.
    	while( didCompress ) {
    		didCompress = false ;
    		
    		// Sanity check: no item may contain itself.
    		for ( int i = 0; i < menuHierarchy[menuItem].length; i++ )
    			if ( menuHierarchy[menuItem][i] == menuItem )
    				throw new IllegalArgumentException("NOPENOPENOPE: Menu item " + menuItem + " contains itself.") ;
    		
    		// find the minimumly cohesive child which fits.
    		int bestCandidate = -1 ;
    		int bestCandidateCohesion = Integer.MAX_VALUE ;
    		
    		for ( int i = 0; i < menuHierarchy[menuItem].length; i++ ) {
    			
    			// Check what we need to see if this is a compression candidate.
    			// This item must represent a menu hierarchy itself, and including
    			// every item within it must not grow menuItem beyond 'width'.
    			
    			if ( onCreate_mergeMenuHierarchyItem_isMergeCandidate(
    		    		widthInRows, itemsPerRow, menuHierarchy, menuItem, i ) ) {
    				int innerItem = menuHierarchy[menuItem][i] ;
    				int [] innerContent = menuHierarchy[innerItem] ;
        			
    				// This is a candidate.  Perform final checks.
    				// This candidate is OK under any of the following conditions:
    				// 1. it has non-negative cohesion, and the parent cohesion is higher.
    				// 2. it has inner content of size 1, and its contents have equal cohesion to itself
    				//		(this has the effect of replacing the item with its single content item).
    				// 3. it has non-negative cohesion, the item is alone in its category,
    				//		and all contents will exist in a single labeled after the merge.
    				boolean ok = mMenuItemCohesion[innerItem] >= 0 && mMenuItemCohesion[innerItem] <= mMenuItemCohesion[menuItem] ;
    				ok = ok || ( innerContent.length == 1 && mMenuItemCohesion[innerItem] == mMenuItemCohesion[innerContent[0]] ) ;
    				if ( innerContent.length > 1 && itemsPerRow > 1 ) {
    					boolean candidateAloneInCategory = 
    						( i == 0 || mMenuHierarchyCategory[menuHierarchy[menuItem][i]] != mMenuHierarchyCategory[menuHierarchy[menuItem][i-1]] )
    						&& ( i == menuHierarchy[menuItem].length-1 || mMenuHierarchyCategory[menuHierarchy[menuItem][i]] != mMenuHierarchyCategory[menuHierarchy[menuItem][i+1]] ) ;
    					boolean allSameCategory = true ;
    					for ( int j = 1; j < innerContent.length; j++ ) {
    						allSameCategory = allSameCategory
    								&& this.mMenuHierarchyCategory[innerContent[j]] == this.mMenuHierarchyCategory[innerContent[j-1]] ;
    					}
    					ok = ok || ( allSameCategory && candidateAloneInCategory ) ;
    				}
    				
    				if ( ok && mMenuItemCohesion[innerItem] < bestCandidateCohesion ) {
    					bestCandidate = i ;
    					bestCandidateCohesion = mMenuItemCohesion[innerItem] ;
    				}
    			}
    		}
    		
    		if ( bestCandidate > -1 ) {
    			int innerItem = menuHierarchy[menuItem][bestCandidate] ;
    			int [] innerContent = menuHierarchy[innerItem] ;
    			
    			// Log.d(TAG, "merging item " + bestCandidate + " into " + menuItem ) ;
				int [] newMenuItemContent = new int[menuHierarchy[menuItem].length + innerContent.length - 1] ;
				// Up to (not including) i, copy from menuHierarchy[menuItem].
				// Then give the full contents of innerContent.  Finally, give
				// the items AFTER (not including) i.
				for ( int j = 0; j < bestCandidate; j++ )
					newMenuItemContent[j] = menuHierarchy[menuItem][j] ;
				for ( int j = 0; j < innerContent.length; j++ ) 
					newMenuItemContent[bestCandidate+j] = innerContent[j] ;
				for ( int j = bestCandidate+1; j < menuHierarchy[menuItem].length; j++ )
					newMenuItemContent[j+innerContent.length -1] = menuHierarchy[menuItem][j] ;
				
				// That's it.  What now, son?  Whelp, we replace menuHierarchy[menuItem] with
				// newMenuItemContent.  However, we don't leave 'i' where it is, since that
				// would make this a weird "depth-first" search.  Instead, advance i to the
				// last item in innerContent in its new position, and our for-loops 'i++' 
				// operation will advance to beyond.
				menuHierarchy[menuItem] = newMenuItemContent ;
				bestCandidate += innerContent.length - 1 ;
				didCompress = true ;
    		}
    	}
    	
    	// Recur into children, compress them first.
    	for ( int i = 0; i < menuHierarchy[menuItem].length; i++ )
    		onCreate_mergeMenuHierarchyItem( widthInRows, itemsPerRow, menuHierarchy, menuHierarchy[menuItem][i] ) ;
    	
    	//Log.d(TAG, "onCreate_mergeMenuHierarchyItem bracket OUT") ;
    }
    
    
    /**
     * Returns whether 'mergeIntoParent''s 'indexToMerge'th child is a merge candidate.
     * Note that it may not be the best available candidate; this is just a boolean
     * check.
     * 
     * An item qualifies as a merge candidate all of the following is true:
     * 
     * 1. The item is a non-leaf: it has a child or children of its own.
     * 2. Merging this item by replacing it with its own contents (within its parent)
     * 		will not split a category label.  If 'itemsPerRow' is > 1, it must be
     * 		the first or last of its category within the parent.  If 'itemsPerRow'
     * 		is 1, this is always true.
     * 3. Merging this item must not grow the parent to greater than 'widthInRows'
     * 		rows.  We assume that no categories will be combined between the parent
     * 		and child (a safe assumption for now, since we set each 'category' to be
     * 		the immediate parent of each node before any merging takes place).
     * @param widthInRows
     * @param itemsPerRow
     * @param menuHierarchy
     * @param mergeIntoParent
     * @param childToMerge
     * @return
     */
    private boolean onCreate_mergeMenuHierarchyItem_isMergeCandidate(
    		int widthInRows, int itemsPerRow, int [][] menuHierarchy,
    		int mergeIntoParent, int indexToMerge ) {
    	
    	int itemToMerge = menuHierarchy[mergeIntoParent][indexToMerge] ;
    	int [] parent = menuHierarchy[mergeIntoParent] ;
    	
    	boolean candidate = true ;
    	// Item is non-leaf.
    	candidate = candidate && menuHierarchy[itemToMerge] != null ;
    	// Merging this item will not split a category label.
    	if ( candidate && itemsPerRow > 1 ) {
    		boolean atCategoryTop = indexToMerge == 0
    				|| mMenuHierarchyCategory[itemToMerge] == mergeIntoParent 
    				|| mMenuHierarchyCategory[itemToMerge] != mMenuHierarchyCategory[parent[indexToMerge-1]] ;
    		boolean atCategoryBottom = indexToMerge +1 == parent.length 
					|| mMenuHierarchyCategory[itemToMerge] == mergeIntoParent 
					|| mMenuHierarchyCategory[itemToMerge] != mMenuHierarchyCategory[parent[indexToMerge+1]] ;
    		
    		candidate = candidate && (atCategoryTop || atCategoryBottom) ;
    	}
    	// Merging this item will not grow the parent past 'widthInRows'.
    	if ( candidate ) {
	    	int parentWidthWithoutCandidate = onCreate_mergeMenuHierarchyItem_countRows( itemsPerRow, menuHierarchy, mergeIntoParent, -1, indexToMerge ) ;
	    	int candidateWidthAfterMerge = onCreate_mergeMenuHierarchyItem_countRows( itemsPerRow, menuHierarchy, itemToMerge, mergeIntoParent, -1 ) ;
	    	candidate = candidate
	    			&& parentWidthWithoutCandidate + candidateWidthAfterMerge <= widthInRows ;
	   }
    	
    	return candidate ;
    }
    
    
    /**
     * Counts the number of rows necessary to represent the specified menu
     * hierarchy item.  We assume up to 'itemsPerRow' can fit on a single row,
     * and a new row is begun under any of the following conditions:
     * 
     * 1. The previous row is full
     * 2. The item has a different HierarchyCategory
     * 3. The item lists itself as its HierarchyCategory
     * 4. The item lists 'this level' as its Hierarchy Category
     * @param itemsPerRow How many items can fit in one row, assuming ideal conditions
     * 			(the same category, etc.)
     * @param menuHierarchy Our full menu hierarchy
     * @param itemToCount The parent items whose contents we should count (as rows).
     * @param actingAsParent If >= 0, we pretend this this value is the parent of 
     * 			the items (as opposed to 'itemToCount', their actual parent).
     * @param childIndexToSkip If >= 0, we pretend this item is not present.
     */
    private int onCreate_mergeMenuHierarchyItem_countRows( int itemsPerRow, int [][] menuHierarchy, int itemToCount, int actingAsParent, int childIndexToSkip ) {
    	int rows = 0 ;
    	int numInRow = itemsPerRow ;	// row "0" is full, so we immediately go to row 1.
    	int rowCategory = Integer.MIN_VALUE ;	// for every new row...
    	
    	int parentCategory = actingAsParent >= 0 ? actingAsParent : itemToCount ;
    	
    	for ( int i = 0; i < menuHierarchy[itemToCount].length; i++ ) {
    		if ( i != childIndexToSkip ) {
	    		int item = menuHierarchy[itemToCount][i] ;
	    		int category = this.mMenuHierarchyCategory[item] ;
	    		boolean newRow = numInRow >= itemsPerRow ;		// previous row is full...
	    		newRow = newRow || rowCategory != category ;	// ...or this is a new category...
	    		newRow = newRow || category == item ;			// ...or it is its own category...
	    		newRow = newRow || category == parentCategory ;	// ...or the immediate parent is its category...
	    		
	    		if ( newRow ) {
	    			rows++ ;
	    			numInRow = 1 ;
	    			rowCategory = category ;
	    		}
    		}
    	}
    	
    	return rows ;
    }
    
    private void onCreate_allocateMenuListObjects( Resources res, int depth ) {
    	//Log.d(TAG, "onCreate_allocateMenuListObjects bracket IN") ;
    	// Find the depth!
    	
    	mMenuLayerItem = new int[mMenuHierarchyDepth] ;
    	
    	// Create and populate our menu hierarchy ArrayAdapter array.
    	mMenuHierarchyArrayAdapter = new MenuItemArrayAdapter[mMenuHierarchyDepth] ;
    	for ( int i = 0; i < mMenuHierarchyDepth; i++ ) {
    		mMenuHierarchyArrayAdapter[i] = new MenuItemArrayAdapter(
    				getLayoutInflater(),
					R.layout.main_menu_list_item,
					0,
					R.id.main_menu_list_row) ;
    	}
    	
    	// Allocate and inflate ListViews.  Use our ViewFlipper as their parent.
    	mMenuHierarchyListView = new ListView[mMenuHierarchyDepth] ;
    	for ( int i = 0; i < mMenuHierarchyDepth; i++ ) {
    		mMenuHierarchyListView[i] = (ListView)(getLayoutInflater().inflate(R.layout.main_menu_list_view, mViewFlipper, false)) ;
    		mMenuHierarchyListView[i].setItemsCanFocus(true) ;
    		mMenuHierarchyListView[i].setDivider(null) ;
    		mMenuHierarchyListView[i].setDividerHeight(0) ;
    	}
    		
    	// Link our adapters to our list views.
    	for ( int i = 0; i < mMenuHierarchyDepth; i++ ) {
    		mMenuHierarchyListView[i].setAdapter( mMenuHierarchyArrayAdapter[i] ) ;
    	}
    	//Log.d(TAG, "onCreate_allocateMenuListObjects bracket OUT") ;
    }
    
    private int onCreate_getMenuHierarchyDepthFromItem( Resources res, int [][] menuHierarchy, int menuItem, int stepsToReach ) {
    	//Log.d(TAG, "onCreate_getMenuHierarchyDepthFromItem bracket IN") ;
    	// stepsToReach is the number of steps before reaching this item; 0 for the root.
    	// If this item contains anything, ask them their depth, given that there
    	// is stepsToReach+1 steps to reach them.
    	
    	if ( stepsToReach > NUM_MENU_ITEMS )
    		throw new IllegalArgumentException("Menu hierarchy has a cycle: " + NUM_MENU_ITEMS + " items, depth at least " + stepsToReach ) ;
    	
    	if ( menuHierarchy[menuItem] == null || menuHierarchy[menuItem].length == 0 ) {
    		//Log.d(TAG, "onCreate_getMenuHierarchyDepthFromItem bracket OUT (return stepsToReach)") ;
    		return stepsToReach ;
    	}
    	
    	int depth = stepsToReach ;
    	for ( int i = 0; i < menuHierarchy[menuItem].length; i++ ) 
    		depth = Math.max(depth, onCreate_getMenuHierarchyDepthFromItem(res, menuHierarchy, menuHierarchy[menuItem][i], stepsToReach+1)) ;
    	
    	//Log.d(TAG, "onCreate_getMenuHierarchyDepthFromItem bracket IN (return depth)") ;
    	return depth ;
    }
    
    private int [] onCreate_getMenuHierarchyItemDepth( Resources res, int [][] menuHierarchy ) {
    	//Log.d(TAG, "onCreate_getMenuHierarchyItemDepth bracket IN") ;
    	int [] itemDepth = new int[NUM_MENU_ITEMS] ;
    	//Log.d(TAG, "onCreate_setMenuHierarchyDepthFromItem bracket RECURRING in onCreate_getMenuHierarchyItemDepth") ;
    	onCreate_setMenuHierarchyChildrenItemDepth( res, menuHierarchy, itemDepth, MENU_ITEM_ROOT ) ;
    	//Log.d(TAG, "onCreate_getMenuHierarchyItemDepth bracket OUT") ;
    	return itemDepth ;
    }
    
    private void onCreate_setMenuHierarchyChildrenItemDepth( Resources res, int [][] menuHierarchy, int [] depth, int menuItem ) {
    	//Log.d(TAG, "onCreate_setMenuHierarchyChildrenItemDepth bracket IN") ;
    	// assume the depth is correctly set for this item (menuItem).
    	// Use this to set the depth for each child, then recur to those children.
    	if ( menuHierarchy[menuItem] != null ) {
    		// first set depths, then recur to each.
	    	for ( int i = 0; i < menuHierarchy[menuItem].length; i++ ) {
	    		int childItem = menuHierarchy[menuItem][i] ;
	    		depth[childItem] = depth[menuItem] + 1 ;
	    	}
	    	for ( int i = 0; i < menuHierarchy[menuItem].length; i++ ) {
	    		int childItem = menuHierarchy[menuItem][i] ;
	    		onCreate_setMenuHierarchyChildrenItemDepth( res, menuHierarchy, depth, childItem ) ;
	    	}
	    	
    	}
    	//Log.d(TAG, "onCreate_setMenuHierarchyChildrenItemDepth bracket OUT") ;
    }
    
    private void onCreate_showMenuListLayer( Resources res, int layer ) {
    	//Log.d(TAG, "onCreate_showMenuListLayer bracket IN") ;
    	// Shows the menu item without any assumption as to which was previously
    	// displayed.  We know that this layer has been prepared.
    	
    	// Do this with the title view
    	mTitleBarButtonStrip.setMenuItem( mMenuLayerItem[layer] ) ;
    	
    	// set the text for the instructions
    	//if ( mInstructionsTextView != null )
    	//	mInstructionsTextView.setText( mMenuItemInstructions[mMenuLayerItem[layer]] ) ;
    	
    	// And the view flipper.
    	
    	mViewFlipper.setDisplayedChild(layer + VIEW_FLIPPER_FIRST_LAYER) ;
    	//Log.d(TAG, "onCreate_showMenuListLayer bracket OUT") ;
    }
    
    
    private void onCreate_allocateLeafViews() {
    	//Log.d(TAG, "onCreate_allocateLeafViews bracket IN") ;
    	mMenuLeafView = new View[NUM_MENU_ITEMS] ;
    	// that's it.  Not much of a method, really.
    	//Log.d(TAG, "onCreate_allocateLeafViews bracket OUT") ;
    }
    
    /**
     * Some leaves represent HTML files for display in a browser
     * or WebView.  We allocate mMenuLeafHTMLPath[i] to a URL valid
     * for access by a web browser; i.e., it will be prepended with
     * http://, file://, or some such.
     * @param res
     */
    private void onCreate_getLeafHTMLPaths( Resources res ) {
    	//Log.d(TAG, "onCreate_getLeafHTMLPaths bracket IN") ;
    	mMenuLeafHTMLPath = new String[NUM_MENU_ITEMS] ;
    	for ( int i = 0; i < NUM_MENU_ITEMS; i++ )
    		mMenuLeafHTMLPath[i] = null ;
    	
    	// Load from resources.
    	mMenuLeafHTMLPath[MENU_ITEM_FEEDBACK] 			= res.getString( R.string.main_menu_feedback_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_USER_LICENSE] 		= res.getString( R.string.main_menu_user_license_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_PRIVACY_POLICY] 	= res.getString( R.string.main_menu_privacy_policy_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_VERSION_HISTORY] 	= res.getString( R.string.main_menu_version_history_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_CREDITS] 			= res.getString( R.string.main_menu_credits_url ) ;
    	
    	mMenuLeafHTMLPath[MENU_ITEM_HOW_TO_PLAY_GAME_BASICS] 			= res.getString( R.string.main_menu_how_to_play_game_basics_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_HOW_TO_PLAY_CONTROLS] 				= res.getString( R.string.main_menu_how_to_play_controls_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_HOW_TO_PLAY_GAME_MODES] 			= res.getString( R.string.main_menu_how_to_play_game_modes_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_HOW_TO_PLAY_SPECIAL_PIECES] 		= res.getString( R.string.main_menu_how_to_play_special_pieces_url ) ;
    	mMenuLeafHTMLPath[MENU_ITEM_HOW_TO_PLAY_ADVANCED_TECHNIQUES] 	= res.getString( R.string.main_menu_how_to_play_advanced_techniques_url ) ;
    	
    	//Log.d(TAG, "onCreate_getLeafHTMLPaths bracket OUT") ;
    }
    
    
    
    
    protected synchronized void onStart() {
    	//Log.d(TAG, "onStart bracket IN") ;
    	super.onStart() ;
    	Log.d(TAG, "onStart") ;
    	
    	// Special analytics?
		if ( !mLoggedDevice && QuantroPreferences.getAnalyticsActive(this) ) {
			
			// ALWAYS log device onStart.
			
			boolean hasAnyPremium = getPremiumLibrary().hasAnyPremium() ;
				
			Analytics.logDevice(this, hasAnyPremium ) ;
			Analytics.logSettingsApp(this, hasAnyPremium) ;
			Analytics.logSettingsGame(this, hasAnyPremium) ;
			Analytics.logKeyRing(this, KeyRing.loadNewKeyRing(this)) ;
				
			mLoggedDevice = true ;
		}
		//Log.d(TAG, "onStart bracket OUT") ;
    }
    
    @SuppressWarnings("unchecked")
	@Override
	protected synchronized void onResume() {
    	//Log.d(TAG, "onResume bracket IN") ;
		super.onResume();
		
		mInFront = true ;
		
		// Preferences may have changed.  Update color scheme and button sounds.
		mButtonSounds = QuantroPreferences.getSoundControls(this) ;
		
		mQuantroSkin = QuantroPreferences.getSkinQuantro(this) ;
        mRetroSkin = QuantroPreferences.getSkinRetro(this) ;
		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		
		DrawSettings ds = newLogoDrawSettings( getResources(), false ) ;
		if ( ds != null ) {
			mTitleBarButtonStrip.setDrawSettings(ds) ;
		}
		
		// Hey now, what about activation?  Do we have XL or not?
    	boolean keyOK = getPremiumLibrary().hasAnyPremium() ;
    	
    	onResume_refreshMainMenuRoot( keyOK ) ;
		onResume_refreshTitleBarButtonStrip( keyOK ) ;
    	mTitleBarButtonStrip.refresh() ;
		
    	// Preload our QuickPlay strings.
    	GameSettings gs = GameSettingsDatabaseAdapter.getMostRecentSinglePlayerInDatabase(this) ;
    	if ( !QuantroPreferences.getRememberCustomSetup(this) )
    		gs = new GameSettings( gs.getMode(), 1 ) ;		// forget custom setup.
    	mQuickLoadToastText = this.makeQuickLoadToast(gs) ;
    	mQuickStartToastText = this.makeQuickStartToast(gs) ;
    	
    	
    	mLobbyFinder.start() ;
		
		if ( !mHasInitialSetup ) {
	        // Post!
	        mHandler.post(mInitialSetupRunnable) ;
		}
		
		mHandler.postDelayed(mRunnableWifiLobbyAlertRefresh, Math.max(0, System.currentTimeMillis() +TIME_BETWEEN_WIFI_REFRESH_MAX)) ;
		
		// WHOA THERE!  First time setup?
		// For 0.8.2, FirstTimeSetup now does nothing but complete() itself.
		if ( !mHasResumed && !FirstTimeSetupActivity.isComplete(this) ) {
			// start FirstTimeSetup.
			Intent intent = new Intent( this, FirstTimeSetupActivity.class ) ;
	    	intent.setAction( Intent.ACTION_MAIN ) ;
	    	this.startActivityForResult(intent, IntentForResult.FIRST_TIME_SETUP) ;
		}
		
		// AsyncSetup?
		if ( !mAsyncSetupIsComplete ) {
			// recreate any canceled task.
			for ( int i = 0; i < NUM_ASYNC_SETUP_TASKS; i++ )
				if ( !mAsyncSetupComplete[i] )	// previously checked for NULL or completed.  This can cause a crash as a started task is re-started (don't know why...)
					mAsyncSetupTask[i] = allocateAsyncSetupTask( i ) ;
			
			// start the first task that remains incomplete.
			for ( int i = 0; i < NUM_ASYNC_SETUP_TASKS; i++ ) {
				if ( !mAsyncSetupComplete[i] ) {
					// lock...
					MainMenuActivity.getDimLock(this).acquire() ;
					MainMenuActivity.getWakeLock(this).acquire() ;
					this.mAsyncSetupIsWakeLocked = true ;
					// execute...
					mAsyncSetupTask[i].execute() ;
					break ;
				}
			}
		}
				
		mHasResumed = true ;
		
		mDialogManager.revealDialogs() ;
    }
    
    
    /**
     * 
     * @param unlocked
     */
    private void onResume_refreshMainMenuRoot( boolean unlocked ) {
    	//Log.d(TAG, "onResume_refreshMainMenuRoot bracket IN") ;
    	// set the text used for the main menu root according 
    	// to whether we are unlocked.
    	if ( unlocked ) {
	    	mMenuItemTitles			[MENU_ITEM_ROOT] 	= mMenuItemRootXLTitle ;
	    	mMenuItemDescriptions	[MENU_ITEM_ROOT] 	= mMenuItemRootXLDescription ;
	    } else {
    		mMenuItemTitles			[MENU_ITEM_ROOT] 	= mMenuItemRootTitle ;
	    	mMenuItemDescriptions	[MENU_ITEM_ROOT] 	= mMenuItemRootDescription ;
	    }
    	//Log.d(TAG, "onResume_refreshMainMenuRoot bracket OUT") ;
    }
    
    private void onResume_refreshTitleBarButtonStrip( boolean unlocked ) {
    	//Log.d(TAG, "onResume_refreshTitleBarButtonStrip bracket IN") ;
    	// set the refresh icon according to whether we are currently
    	// locked or unlocked.
    	mTitleBarButtonStrip.setPremiumIsUnlocked(unlocked) ;
    }
    
    @Override
	protected synchronized void onPause() {
    	//Log.d(TAG, "onPause bracket IN") ;
		super.onPause();
		Log.d(TAG, "onPause") ;
		
		mInFront = false ;
		
		mLobbyFinder.stop() ;
		//Log.d(TAG, "onPause bracket OUT") ;
		
		// Did this interrupt our AsyncSetup?
		if ( mAsyncSetupIsWakeLocked ) {
			// cancel the currently running AsyncTask
			for ( int i = 0; i < NUM_ASYNC_SETUP_TASKS; i++ ) {
				if ( !mAsyncSetupComplete[i] ) {
					// this is the current task.
					mAsyncSetupTask[i].cancel(false) ;
					break ;
				}
			}
			
			// release our lock.
			this.getDimLock(this).release() ;
			this.getWakeLock(this).release() ;
			mAsyncSetupIsWakeLocked = false ;
		}
		
		mDialogManager.hideDialogs() ;
	}
    
    @Override
    protected synchronized void onStop() {
    	super.onStop() ;
    	Log.d(TAG, "onStop") ;
    	
    	recycleLogoUntilDraw() ;
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // ASYNC SETUP
    //////////
    
    // Here are the tasks we need to do.
    // Basic initial stuff
    private static final int ASYNC_SETUP_TASK_GREETING_DIALOG = 0 ;
    private static final int ASYNC_SETUP_TASK_LOAD_MENU_HIERARCHY = 1 ;
    private static final int ASYNC_SETUP_TASK_MERGE_MENU_HIERARCHY = 2 ;
    private static final int ASYNC_SETUP_TASK_NOTE_MENU_ITEM_HIERARCHY_DEPTHS = 3 ;
    private static final int ASYNC_SETUP_TASK_LOAD_LEAF_HTML = 4 ;
    // menu root
    private static final int ASYNC_SETUP_TASK_ALLOCATE_AND_PRECACHE_MENU_ITEM_ROOT_VIEWS = 5 ;
    private static final int ASYNC_SETUP_TASK_SET_MENU_ITEM_ROOT_VIEWS = 6 ;
    // Other menu layers.
    private static final int ASYNC_SETUP_TASK_ALLOCATE_AND_PRECACHE_MENU_ITEM_NONROOT_VIEWS = 7 ;
    private static final int ASYNC_SETUP_TASK_SET_MENU_ITEM_NONROOT_VIEWS = 8 ;
    // Load Logo.
    private static final int ASYNC_SETUP_TASK_LOAD_AND_SET_LOGO_SEQUENCE = 9 ;
    
    private static final int NUM_ASYNC_SETUP_TASKS = 10 ;
    
    
    // When one of these AsyncTasks is fully complete, the last thing it does is
    // call this method.
    @SuppressWarnings("rawtypes")
	private synchronized void onAsyncSetupTaskComplete( AsyncTask task ) {
    	
    	if ( task.isCancelled() )
    		return ;
    	
    	int index = -1 ;
    	for ( int i = 0; i < NUM_ASYNC_SETUP_TASKS; i++ ) {
    		if ( mAsyncSetupTask[i] == task ) {
    			index = i ;
    			break ;
    		}
    	}
    	
    	if ( index == -1 )
    		return ;
    		
    	Log.d(TAG, "onAsyncSetupTaskComplete " + index) ;
    	
    	// mark completed.
    	mAsyncSetupComplete[index] = true ;
    	
    	// are we COMPLETELY done?
    	if ( mAsyncSetupTask.length == index + 1 ) {
    		// finished.
    		mAsyncSetupIsComplete = true ;
    		
    		// unlock.
    		if ( mAsyncSetupIsWakeLocked ) {
    			MainMenuActivity.getDimLock(this).release() ;
    			MainMenuActivity.getWakeLock(this).release() ;
    			mAsyncSetupIsWakeLocked = false ;
    		}
    	} else {
    		// If we're still locked, start the next task.
    		// Otherwise, do nothing.
    		if ( mAsyncSetupIsWakeLocked )
    			mAsyncSetupTask[index+1].execute() ;
    	}
    }
    
 // When one of these AsyncTasks is canceled, the last thing it does is
    // call this method.
    private synchronized void onAsyncSetupTaskCancelled( AsyncTask task ) {
    	int index = -1 ;
    	for ( int i = 0; i < NUM_ASYNC_SETUP_TASKS; i++ ) {
    		if ( mAsyncSetupTask[i] == task ) {
    			index = i ;
    			break ;
    		}
    	}
    	
    	if ( index == -1 )
    		return ;
    	
    	// unlock.
		if ( mAsyncSetupIsWakeLocked ) {
			MainMenuActivity.getDimLock(this).release() ;
			MainMenuActivity.getWakeLock(this).release() ;
			mAsyncSetupIsWakeLocked = false ;
		}
    }
    
    
    @SuppressWarnings("rawtypes")
	private AsyncTask allocateAsyncSetupTask( int taskNum ) {
    	
    	switch( taskNum ) {
    	case ASYNC_SETUP_TASK_GREETING_DIALOG:
    		// An async task to display an optional greeting.  Could be
    		// a premium content first-time setup prompt, a "What's New" dialog,
    		// etc., depending on our saved settings.
    		return new AsyncTask<Object, Object, Integer>() {
				@Override
				protected Integer doInBackground(Object... params) {
					
					// If this is the first time, display the first time dialog
					// (and note our most recent "what's new" as this version).
					// If this is NOT the first time, and the version code has
					// changed, display WHAT'S NEW.
					int code = AppVersion.code(MainMenuActivity.this) ;
					int lastCodeWhatsNew = QuantroPreferences.getPrivateSettingInt(
							MainMenuActivity.this,
							GlobalDialog.PREFERENCE_LAST_WHATS_NEW,
							-1) ;
					boolean hasFirstTime = QuantroPreferences.getPrivateSettingBoolean(
							MainMenuActivity.this,
							GlobalDialog.PREFERENCE_HAS_SHOWN_FIRST_TIME_PREMIUM_UNLOCK,
							false) ;
					
					if ( !hasFirstTime ) {
						QuantroPreferences.setPrivateSettingInt(
								MainMenuActivity.this,
								GlobalDialog.PREFERENCE_LAST_WHATS_NEW,
								code) ;
						return null;
					} else if ( code > lastCodeWhatsNew ) {
						return GlobalDialog.DIALOG_ID_WHATS_NEW ;
					}
					return null ;
				}
				
				@Override
				protected void onPostExecute( Integer result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else {
						if ( result != null )
							mDialogManager.showDialog(result) ;
						onAsyncSetupTaskComplete(this) ;
					}
				}
    		} ;
    	
    	
    	case ASYNC_SETUP_TASK_LOAD_MENU_HIERARCHY:
    		// An async task to load the menu hierarchy from Resources.
    		return new AsyncTask<Object, Object, Object>() {
				@Override
				protected Object doInBackground(Object... params) {
					Resources res = getResources() ;
					onCreate_getMenuItems( res ) ;
					mMenuHierarchy = onCreate_getMenuHierarchy( res ) ;
					mMenuHierarchy = onCreate_reduceMenuHierarchy( mMenuHierarchy ) ;
					mMenuHierarchyCategory = onCreate_getMenuHierarchyParents( mMenuHierarchy ) ;
					return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else
						onAsyncSetupTaskComplete(this) ;
				}
    		} ;
    		
    	case ASYNC_SETUP_TASK_MERGE_MENU_HIERARCHY:
        	// An async task to merge the menu hierarchy once mMenuItemsPerPage is set.
    		return new AsyncTask<Object, Object, Object>() {
				@Override
				protected Object doInBackground(Object... params) {
					while ( true ) {
						if ( isCancelled() )
							return null ;
						if ( mMenuRowsPerPage > 0 ) {
							Log.d(TAG, "ASYNC_SETUP_TASK_MERGE_MENU_HIERARCHY merging to width in rows " + mMenuRowsPerPage + " with " + mMenuItemsPerRow + " items in a row") ;
							mMenuHierarchy = onCreate_mergeMenuHierarchy( mMenuRowsPerPage, mMenuItemsPerRow ) ;
							return null ;
						}
						try {
							Thread.sleep(10) ;
						} catch( InterruptedException ie ) {
							return null ;
						}
					}
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else
						onAsyncSetupTaskComplete(this) ;
				}
    		} ;
    		
        	
    	case ASYNC_SETUP_TASK_NOTE_MENU_ITEM_HIERARCHY_DEPTHS:
        	// An async task to measure the menu hierarchy depth.
        	return new AsyncTask<Object, Object, Object>() {
        		@Override
				protected Object doInBackground(Object... params) {
        			Resources res = getResources() ;
        			mMenuHierarchyDepth = onCreate_getMenuHierarchyDepthFromItem( res, mMenuHierarchy, MENU_ITEM_ROOT, 0 ) ;
        			mMenuHierarchyItemDepth = onCreate_getMenuHierarchyItemDepth( res, mMenuHierarchy ) ;
        			return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else
						onAsyncSetupTaskComplete(this) ;
				}
        	} ;
        	
    	case ASYNC_SETUP_TASK_LOAD_LEAF_HTML:
    		// An async task that loads all HTML leaf data, or at least URLs.
    		return new AsyncTask<Object, Object, Object>() {
        		@Override
				protected Object doInBackground(Object... params) {
        			onCreate_getLeafHTMLPaths( getResources() ) ;
        			return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else
						onAsyncSetupTaskComplete(this) ;
				}
        	} ;
        	
        	
        case ASYNC_SETUP_TASK_ALLOCATE_AND_PRECACHE_MENU_ITEM_ROOT_VIEWS:
        	// an async task which performs allocation and precaching of the root view.
        	return new AsyncTask<Object, Object, Object>() {
        		@Override
				protected Object doInBackground(Object... params) {
        			// Layer items?
        			mMenuLayerItem = new int[mMenuHierarchyDepth] ;
        			// Lists and array adapter arrays?
        	    	mMenuHierarchyArrayAdapter = new MenuItemArrayAdapter[mMenuHierarchyDepth] ;
        	    	mMenuHierarchyListView = new ListView[mMenuHierarchyDepth] ;
        	    	
        	    	// do only the first.
        	    	if ( mMenuHierarchyListView[0] == null ) {
        	    		mMenuHierarchyArrayAdapter[0] = new MenuItemArrayAdapter(
        	    				getLayoutInflater(),
        						R.layout.main_menu_list_item,
        						0,
        						R.id.main_menu_list_row) ;
        	    	}
        	    	
        			return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else {
						Log.d(TAG, "view flipper kids: " + mViewFlipper.getChildCount()) ;
						mMenuHierarchyListView[0] = (ListView)mViewFlipper.getChildAt(VIEW_FLIPPER_FIRST_LAYER) ;
        	    		mMenuHierarchyListView[0].setItemsCanFocus(true) ;
        	    		mMenuHierarchyListView[0].setDivider(null) ;
        	    		mMenuHierarchyListView[0].setDividerHeight(0) ;
        	    		
        	    		mMenuHierarchyListView[0].setAdapter( mMenuHierarchyArrayAdapter[0] ) ;
        	    		
						onAsyncSetupTaskComplete(this) ;
					}
				}
        	} ;
        	
        case ASYNC_SETUP_TASK_SET_MENU_ITEM_ROOT_VIEWS:
        	// return an async task which adds menu item 0 as a ViewFlipper child
        	// and sets it as the current layer.
        	return new AsyncTask<Object, Object, Object>() {
        		@Override
				protected Object doInBackground(Object... params) {
        			return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else {
						// layer 0 is already in the view.  Show it.
						mDisplayedMenuLayer = 0 ;
			            mMenuLayerItem[mDisplayedMenuLayer] = MENU_ITEM_ROOT ;
			            mDisplayedMenuItemIsLeaf = false ;
			            prepareMenuListLayer( mDisplayedMenuLayer, MENU_ITEM_ROOT ) ;
			            // Now show it.
			            onCreate_showMenuListLayer(getResources(), mDisplayedMenuLayer ) ;
 						
						onAsyncSetupTaskComplete(this) ;
					}
				}
        	} ;
        	
        	
        
        // Other menu layers.
        case ASYNC_SETUP_TASK_ALLOCATE_AND_PRECACHE_MENU_ITEM_NONROOT_VIEWS:
        	// an async task which performs allocation and precaching of all 
        	// menu layers other than the root.
        	return new AsyncTask<Object, Object, Object>() {
        		@Override
				protected Object doInBackground(Object... params) {
        			return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else {
						onAsyncSetupTaskComplete(this) ;
					}
				}
        	} ;
        	
        case ASYNC_SETUP_TASK_SET_MENU_ITEM_NONROOT_VIEWS:
        	// return an async task which all >0 menu items to the flipper.
        	return new AsyncTask<Object, Object, Object>() {
        		@Override
				protected Object doInBackground(Object... params) {
        			return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else {
						// Set layers appropriately.
						for ( int i = 1; i < mMenuHierarchyDepth; i++ ) {
							mMenuHierarchyArrayAdapter[i] = new MenuItemArrayAdapter(
				    				getLayoutInflater(),
									R.layout.main_menu_list_item,
									0,
									R.id.main_menu_list_row) ;

	        	    		mMenuHierarchyListView[i] = (ListView)mViewFlipper.getChildAt(VIEW_FLIPPER_FIRST_LAYER + i) ;
	        	    		mMenuHierarchyListView[i].setItemsCanFocus(true) ;
	        	    		mMenuHierarchyListView[i].setDivider(null) ;
	        	    		mMenuHierarchyListView[i].setDividerHeight(0) ;
	        	    		
	        	    		mMenuHierarchyListView[i].setAdapter( mMenuHierarchyArrayAdapter[i] ) ;
						}
						
						mMenuLayersAreReady = true ;
						
						// this can screw up the currently displayed menu item.  Fix it.
						if ( mDisplayedMenuItemIsLeaf )
							showMenuLeaf(mDisplayedMenuItem) ;
						else {
							MainMenuActivity.this.prepareMenuListLayer( mDisplayedMenuLayer, mDisplayedMenuItem) ;
							MainMenuActivity.this.showMenuListLayer(mDisplayedMenuLayer) ;
						}
 						
						onAsyncSetupTaskComplete(this) ;
					}
				}
        	} ;
        	
        // Load Logo.
        case ASYNC_SETUP_TASK_LOAD_AND_SET_LOGO_SEQUENCE:
        	// return an async task which loads the logo sequence then places it in the title bar.
        	return new AsyncTask<Object, Object, Object>() {
        		@Override
				protected Object doInBackground(Object... params) {
        			if ( mLogoSequence == null )
        				mLogoSequence = onCreate_makeLogo() ;
        			return null ;
				}
				
				@Override
				protected void onPostExecute( Object result ) {
					if ( this.isCancelled() )
						onAsyncSetupTaskCancelled(this) ;
					else {
						onCreate_setTitleBarSliceSequence( getResources(), mLogoSequence ) ;
						onAsyncSetupTaskComplete(this) ;
					}
				}
        	} ;
    	}
    	
    	
    	return null ;
    	
    }
    
    
    //////////
    // ASYNC SETUP
    ////////////////////////////////////////////////////////////////////////////
    
    
    
    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	//Log.d(TAG, "onKeyDown bracket IN") ;
    	Log.d(TAG, "onKeyDown " + keyCode + ", event count " + event.getRepeatCount()) ;
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	
            // Go back a level, if possible; otherwise, finish.
        	if ( mDisplayedMenuItemIsLeaf ) {
        		showMenuListLayer( mDisplayedMenuLayer ) ;
        		if ( mButtonSounds )
        			mSoundPool.menuButtonBack() ;
        		//Log.d(TAG, "onKeyDown bracket OUT") ;
        		return true ; 
        	}
        	else if ( mDisplayedMenuLayer > 0 ) {
        		showMenuListLayer( mDisplayedMenuLayer -1 ) ;
        		if ( mButtonSounds )
        			mSoundPool.menuButtonBack() ;
        		//Log.d(TAG, "onKeyDown bracket OUT") ;
        		return true ;
        	}
        	else if ( mButtonSounds )
        			mSoundPool.menuButtonBack() ;
        	
        	finish() ;        	
        	//Log.d(TAG, "onKeyDown bracket OUT") ;
            return true;
        }

        boolean res = super.onKeyDown(keyCode, event) ;
        //Log.d(TAG, "onKeyDown bracket OUT") ;
        return res ;
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
        	id = R.menu.main_menu_overflow_normal ;
        	break ;
    	case VersionSafe.SCREEN_SIZE_LARGE:
    		id = R.menu.main_menu_overflow_large ;
        	break ;	
    	case VersionSafe.SCREEN_SIZE_XLARGE:
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
    
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	//Log.d(TAG, "onOptionsItemSelected bracket IN") ;
    	switch( item.getItemId() ) {
    	case R.id.overflow_options_menu_help:
    		mDialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
    		return true ;
    	case R.id.overflow_options_menu_settings:
    		// launch settings
    		this.startQuantroPreferencesActivity() ;
    		return true ;
    	}
    	//Log.d(TAG, "onOptionsItemSelected bracket OUT") ;
    	return false ;
    }

    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // DIALOGS
    // Used for switching between menu layers.
    
    @Override
    protected Dialog onCreateDialog( int dialog ) {
    	if ( GlobalDialog.hasDialog(dialog) )
    		return GlobalDialog.onCreateDialog(this, dialog, mDialogManager) ;
    	
    	AlertDialog.Builder builder ;
    	WebViewDialog.Builder wvBuilder ;
    	
    	switch( dialog ) {
    	case DIALOG_ID_UPDATE_CONTROLS:
    		builder = new AlertDialog.Builder(this) ;
    		builder.setTitle(R.string.main_menu_update_controls_title) ;
    		builder.setMessage(R.string.main_menu_update_controls_message) ;
    		builder.setCancelable(true) ;
    		builder.setPositiveButton(
    				R.string.main_menu_update_controls_button_yes,
    				getResources().getColor(R.color.main_menu_single_player_color),
    				new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							QuantroPreferences.setControlsDefaults(MainMenuActivity.this) ;
							mDialogManager.dismissDialog(DIALOG_ID_UPDATE_CONTROLS) ;
							Toast.makeText(MainMenuActivity.this,
									R.string.main_menu_toast_updating_controls,
									Toast.LENGTH_SHORT).show() ;
						}
					}) ;
    		builder.setNegativeButton(
    				R.string.main_menu_update_controls_button_no,
    				new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mDialogManager.dismissDialog(DIALOG_ID_UPDATE_CONTROLS) ;
						}
					}) ;
    		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_UPDATE_CONTROLS) ;
				}
    		}) ;
    		return builder.create() ;

    	}
    	
    	return null ;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // MENU ITEM TRANSITION METHODS
    // Used for switching between menu layers.
    
    /**
     * We prepare layer 'layer', through its ArrayAdapter, for displaying
     * the specified menuItem.  Assumption: menuItem is not a leaf.
     * 
     * Upon return, mMenuHierarchyView[layer] is configured to display
     * the items within menuItem, where those items are other menu items.
     */
    private void prepareMenuListLayer( int layer, int menuItem ) {
    	//Log.d(TAG, "prepareMenuListLayer bracket IN") ;
    	mMenuLayerItem[layer] = menuItem ;
    	
    	// TODO: Perform whatever updates are necessary to prepare the title
    	// view for the layer transition.
    	MenuItemArrayAdapter aa = mMenuHierarchyArrayAdapter[layer] ;
    	
    	// Add the contents of the menu item
    	if ( aa != null ) {
    		//Log.d(TAG, "preparing menu item " + menuItem + " with length " + mMenuHierarchy[menuItem].length + " in the hierarchy") ;
	    	aa.setRootItem(menuItem) ;
	    	// Tell the list view to update.
	    	aa.notifyDataSetChanged() ;
	    	//Log.d(TAG, "prepareMenuListLayer bracket OUT") ;
    	}
    }
    
    
    private void showMenuListLayer( int layer ) {
    	//Log.d(TAG, "showMenuListLayer bracket IN") ;
    	if ( mDisplayedMenuItemIsLeaf ) {
    		// back to the displayed layer
    		mViewFlipper.setDisplayedChild(mDisplayedMenuLayer + VIEW_FLIPPER_FIRST_LAYER) ;
    	}
    	
    	// Perform the title view update
    	mTitleBarButtonStrip.setMenuItem( mMenuLayerItem[layer] ) ;
    	
    	mDisplayedMenuItemIsLeaf = false ;
    	
    	if ( mMenuLayersAreReady ) {
	    	while ( layer < mDisplayedMenuLayer ) {
		    	mViewFlipper.showPrevious() ;
		    	mDisplayedMenuLayer-- ;
	    	}
	    	while ( layer > mDisplayedMenuLayer ) {
		    	mViewFlipper.showNext() ;
		    	mDisplayedMenuLayer++ ;
	    	}
    	}
    	
    	if ( !mMenuLayersAreReady && layer != 0 )
    		mViewFlipper.setDisplayedChild(0) ;
    	else if ( mViewFlipper.getDisplayedChild() != layer + VIEW_FLIPPER_FIRST_LAYER )
    		mViewFlipper.setDisplayedChild(layer + VIEW_FLIPPER_FIRST_LAYER) ;
    	
    	// Update the instructions.
    	//if ( mInstructionsTextView != null )
    	//	mInstructionsTextView.setText( mMenuItemInstructions[mMenuLayerItem[layer]] ) ;
    	//Log.d(TAG, "showMenuListLayer bracket OUT") ;
    	
    	mDisplayedMenuItem = mMenuLayerItem[layer] ;
    	mDisplayedMenuLayer = layer ;
    }
    
    
    private void showMenuLeaf( int leafItem ) {
    	//Log.d(TAG, "showMenuLeaf bracket IN") ;
    	if ( mDisplayedMenuItemIsLeaf && leafItem != mDisplayedMenuItem ) {
    		// Shouldn't ever happen, but back up one.
    		Log.e(TAG, "showMenuLeaf when a leaf is already displayed.  Attempting to avoid serious problems.") ;
    		mViewFlipper.setDisplayedChild(mDisplayedMenuLayer) ;
    	}
    	
    	mDisplayedMenuItemIsLeaf = true ;
    	// If the 'leaf' slot is set, remove its view and set the
    	// one for this leaf.
    	if ( mViewFlipper.getChildCount() > VIEW_FLIPPER_LEAF_LAYER )
    		mViewFlipper.removeViewAt(VIEW_FLIPPER_LEAF_LAYER) ;
    	if ( mMenuLeafView != null && mMenuLeafView[leafItem] != null )
    		mViewFlipper.addView(mMenuLeafView[leafItem], VIEW_FLIPPER_LEAF_LAYER) ;
    	
    	// Move to this leaf.
    	// Perform whatever updates are necessary to update the title view.
    	mTitleBarButtonStrip.setMenuItem( leafItem ) ;
    	
    	// Update the flipper to menu content.
    	if ( !mMenuLeavesAreReady )
    		mViewFlipper.setDisplayedChild(0) ;
    	else
    		mViewFlipper.setDisplayedChild(VIEW_FLIPPER_LEAF_LAYER) ;
    	
    	// Update the instructions.
    	//if ( mInstructionsTextView != null )
    	//	mInstructionsTextView.setText( mMenuItemInstructions[leafItem] ) ;
    	
    	mDisplayedMenuItem = leafItem ;
    	//Log.d(TAG, "showMenuLeaf bracket OUT") ;
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // INTENT LAUNCH HELPERS
    // For anything but the most trivial activity launch.
    
    private String makeQuickLoadToast( GameSettings gs ) {
    	return makeLoadToast( true, gs ) ;
    }
    
    private String makeStandardLoadToast( GameSettings gs ) {
    	return makeLoadToast( false, gs ) ;
    }
    
    private String makeLoadToast( boolean quickLoad, GameSettings gs ) {
    	// e.g. "Quick-Load Quantro Endurance"
    	// We expect the resource string to be something like
    	// "Quick-Load GM_N_XXXX"; replace the placeholder with the game mode name.
    	Resources res = getResources() ;
    	String placeholder_name = res.getString( R.string.placeholder_game_mode_name ) ;
    	
    	String base = res.getString( quickLoad ? R.string.quick_play_load : R.string.play_load ) ;
    	return base.replace(placeholder_name, GameModes.name(gs.getMode())) ;
    }
    
    private String makeQuickStartToast( GameSettings gs ) {
    	return makeStartToast( true, gs ) ;
    }
    
    private String makeStandardStartToast( GameSettings gs ) {
    	return makeStartToast( false, gs ) ;
    }
    
    private String makeStartToast( boolean quickLoad, GameSettings gs ) {
    	// e.g. "Quick-Start Quantro Progression"
		// or 	"Quick-Start Quantro Progression: Level 5, Garbage --/4"
    	// We expect the resource string to be something like
    	// "Quick-Load GM_N_XXXX"; replace the placeholder with the game mode name.
    	Resources res = getResources() ;
    	String placeholder_name = res.getString( R.string.placeholder_game_mode_name ) ;
    	if ( gs.hasDefaultsIgnoringDifficulty() ) 
    		return res.getString( quickLoad ? R.string.quick_play_new : R.string.play_new ).replace(placeholder_name, GameModes.name(gs.getMode())) ;
    	else {
    		// create custom string.
    		boolean has = false ;
    		StringBuilder sb = new StringBuilder() ;
    		
    		String sep = res.getString(R.string.play_list_separator) ;
    		
    		// Custom level?
    		if ( gs.hasLevel() ) {
    			String str = res.getString(R.string.play_list_level) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_level),
    					"" + gs.getLevel()) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		// Custom clears-per?
    		if ( gs.hasClearsPerLevel() ) {
    			String str = res.getString(R.string.play_list_clears_per_level) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_clears_per_level),
    					"" + gs.getClearsPerLevel()) ;
    			if ( has )
    				sb.append(sep) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		// Custom Garbage?
    		if ( gs.hasGarbage() || gs.hasGarbagePerLevel() ) {
    			String strDefault = res.getString(R.string.play_list_default) ;
    			String str = res.getString(R.string.play_list_garbage) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_garbage),
    					gs.hasGarbage() ? "" + gs.getGarbage() : strDefault) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_garbage_per_level),
    					gs.hasGarbagePerLevel() ? "" + gs.getGarbagePerLevel() : strDefault) ;
    			
    			if ( has )
    				sb.append(sep) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		
    		// now load the custom new string and place this customized list within it.
    		String str = res.getString( quickLoad ? R.string.quick_play_new_custom : R.string.play_new_custom) ;
    		str = str.replace(
    				res.getString(R.string.placeholder_game_settings_custom_list),
    				sb.toString()) ;
    		str = str.replace(placeholder_name, GameModes.name(gs.getMode())) ;
    		return str ;
    	}
    }
    
    private void quickStartGameActivity() {
    	Log.d(TAG, "quick-starting a game activity") ;
    	
    	GameSettings gs = GameSettingsDatabaseAdapter.getMostRecentSinglePlayerInDatabase(this) ;
    	if ( !QuantroPreferences.getRememberCustomSetup(this) )
    		gs = new GameSettings( gs.getMode(), 1 ) ;		// forget custom setup
    	
    	String saveKey = gameModeToSaveKey(gs.getMode()) ;
    	
    	// check for a saved game.
    	if ( GameSaver.hasGameStates( this, saveKey ) ) {
    		// We should LOAD this game.
    		
    		// Toast
    		// e.g. "Quick-Load Quantro Endurance"
    		Toast.makeText(this, mQuickLoadToastText, Toast.LENGTH_SHORT).show() ;
    		startGameActivity( gs.getMode(), saveKey, saveKey, false, true ) ;
    	} else {
    		
    		// Toast
    		// e.g. "Quick-Start Quantro Progression"
    		// or 	"Quick-Start Quantro Progression: Level 5, Garbage --/4"
    		Toast.makeText(this, mQuickStartToastText, Toast.LENGTH_SHORT).show() ;
    		startGameActivity( saveKey, gs, false, true) ;
    	}
    }
    
    private void startGameActivity( int mode, String loadFromKey, String saveToKey, boolean showToast, boolean wasQuickPlay ) {
    	GameSettings gs = QuantroPreferences.getRememberCustomSetup(this)
    			? GameSettingsDatabaseAdapter.getMostRecentInDatabase(this, mode)
    			: new GameSettings(mode, 1).setImmutable() ;
    	
    	if ( showToast ) {
    		if ( loadFromKey != null )
    			Toast.makeText(this, this.makeStandardLoadToast(gs), Toast.LENGTH_SHORT).show() ;
    		else
    			Toast.makeText(this, this.makeStandardStartToast(gs), Toast.LENGTH_SHORT).show() ;
    	}
    	
    	Intent intent = new Intent( this, GameActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	GameIntentPackage gip = new GameIntentPackage( gs ) ;
    	if ( saveToKey != null )
    		gip.setConnectionAsLocal(loadFromKey, saveToKey) ;
    	
    	// We're about to start a game with the specified mode.  If 'loadFromKey'
    	// is null, this is a new game.  Make a note in our GameStats database.
    	if ( loadFromKey == null )
    		GameStats.DatabaseAdapter.addNewGameStartedToDatabase(this, mode) ;
    	
    	intent.putExtra( GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE, gip) ;
    	startActivityForResult(intent, IntentForResult.LAUNCH_GAME) ;
    	
    	// log
    	if ( QuantroPreferences.getAnalyticsActive(this) ) {
    		if ( loadFromKey == null ) {
    			// no load; a new game.
    			Analytics.logNewGame(gip.gameSettings, wasQuickPlay) ;
    		} else {
    			// a load!
    			GameResult gr = GameSaver.loadGameResult(this, loadFromKey) ;
    			if ( gr != null ) {
    				Date dateStarted = gr.getDateStarted() ;
    				Date dateEnded = gr.getDateEnded() ;
    				long timeSinceStart = -1 ;
    				long timeSinceEnd = -1 ;
    				if ( dateStarted != null )
    					timeSinceStart = System.currentTimeMillis() - dateStarted.getTime() ;
    				if ( dateEnded != null )
    					timeSinceEnd = System.currentTimeMillis() - dateEnded.getTime() ;
    				Analytics.logLoadGame(gip.gameSettings, timeSinceStart, timeSinceEnd, wasQuickPlay) ;
    			}
    		}
    	}
    }
    
    
    private void startGameActivity( String saveToKey, GameSettings gs, boolean showToast, boolean wasQuickPlay ) {
    	if ( gs == null )
    		throw new NullPointerException("Must provide a non-null GameSettings object.") ;
    	
    	if ( showToast )
    		Toast.makeText(this, this.makeStandardStartToast(gs), Toast.LENGTH_SHORT).show() ;
    	
    	Intent intent = new Intent( this, GameActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	GameIntentPackage gip = new GameIntentPackage( gs ) ;
    	if ( saveToKey != null )
    		gip.setConnectionAsLocal(null, saveToKey) ;
    	
    	// We're about to start a game with the specified mode.  Make a note
    	// in our database.
    	GameStats.DatabaseAdapter.addNewGameStartedToDatabase(this, gs.getMode()) ;
    	
    	intent.putExtra( GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE, gip) ;
    	startActivityForResult(intent, IntentForResult.LAUNCH_GAME) ;
    	
    	// log
    	if ( QuantroPreferences.getAnalyticsActive(this) ) {
			// no load; a new game.
			Analytics.logNewGame(gs, wasQuickPlay) ;
    	}
    	//Log.d(TAG, "startGameActivity bracket OUT") ;
    }
    
    
    protected void startCustomGameModeActivity( int gameMode ) {
    	// find the CGMS.
    	if ( GameModes.isCustom(gameMode) )
    		startCustomGameModeActivity( CustomGameModeSettingsDatabaseAdapter.get(this, GameModes.gameModeToCustomID(gameMode)) ) ;
    }
    
    protected void startCustomGameModeActivity( CustomGameModeSettings cgms ) {
    	
    	int id ;
    	boolean hasSaves = false ;
    	
    	if ( cgms == null ) {
    		id = GameModes.getFreeCustomGameModeSettingID() ;
    	} else {
    		id = cgms.getID() ;
    		int [] modes = GameModes.getCustomGameModes(id) ;
    		
    		for ( int i = 0; i < modes.length; i++ )
    			hasSaves = hasSaves || GameSaver.hasGameStates(this, this.gameModeToSaveKey(modes[i])) ;
    	}
    	
    	Intent i = new Intent( this, CustomGameModeActivity.class ) ;
    	if ( cgms != null )
    		i.putExtra(CustomGameModeActivity.INTENT_EXTRA_CGMS, cgms) ;
    	else
    		i.putExtra(CustomGameModeActivity.INTENT_EXTRA_CGMS_ID, id) ;
    	
    	i.putExtra(CustomGameModeActivity.INTENT_EXTRA_HAS_SAVES, hasSaves) ;
    	
    	startActivityForResult( i, IntentForResult.CUSTOM_GAME_MODE ) ;
    	
    	// log
    	if ( QuantroPreferences.getAnalyticsActive(this) ) {
			// no load; a new game.
			Analytics.logCustomGameMode(id, cgms == null, hasSaves) ;
    	}
    }
    
    
    protected void openAchievements() {
    	QuantroApplication qa = (QuantroApplication)getApplication() ;
		if ( qa.gpg_isSignedIn() ) {
			startActivityForResult(qa.gpg_getAchievementsIntent(), IntentForResult.UNUSED);
		} else {
			expandStinger(true) ;
			qa.gpg_showAlert(getResources().getString(R.string.gamehelper_achievements_not_signed_in)) ;
		}
    }
    
    protected void openLeaderboards() {
    	QuantroApplication qa = (QuantroApplication)getApplication() ;
		if ( qa.gpg_isSignedIn() ) {
			startActivityForResult(qa.gpg_getAllLeaderboardsIntent(), IntentForResult.UNUSED);
		} else {
			expandStinger(true) ;
			qa.gpg_showAlert(getResources().getString(R.string.gamehelper_leaderboards_not_signed_in)) ;
		}
    }
    
    protected void emailDeveloper() {
    	Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);  
    	
    	emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ "jake@peaceray.com" } ) ;
    	
    	emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name)) ;
    	
    	emailIntent.setType("plain/text"); 
    	
    	Point size = VersionSafe.getScreenSize(this);
    	GameViewMemoryCapabilities gvmc = ((QuantroApplication)getApplication()).getGameViewMemoryCapabilities(this) ;
    	
    	String YES = "Yes" ;
    	String NO = "No" ;
    	String X = " x " ;
    	
    	StringBuilder sb = new StringBuilder() ;
    	
    	sb.append("-------------------------\n") ;
    	sb.append("If you are reporting a bug or some other problem, ") ;
    	sb.append("this information can be very helpful.  Please ") ;
    	sb.append("include it in your email.\n\n") ;
    	sb.append("SDK").append("              ").append(Build.VERSION.SDK_INT).append("\n") ;
    	sb.append("Release").append("          ").append(Build.VERSION.RELEASE).append("\n") ;
    	sb.append("Screen Size").append("      ").append(size.x + X + size.y).append("\n") ;
    	sb.append("Brand").append("            ").append(Build.BRAND).append("\n") ;
    	sb.append("Manufacturer").append("     ").append(Build.MANUFACTURER).append("\n") ;
    	sb.append("Model").append("            ").append(Build.MODEL).append("\n") ;
    	sb.append("\n") ;
    	sb.append("VersionCode").append("      ").append(AppVersion.code(this)).append("\n") ;
    	sb.append("VersionName").append("      ").append(AppVersion.name(this)).append("\n" ) ;
    	sb.append("XL").append("               ").append(getPremiumLibrary().hasAnyPremium() ? YES : NO).append("\n") ;
    	sb.append("Menu Fullscreen").append("  ").append(QuantroPreferences.getScreenSizeMenu(this) == QuantroPreferences.XL_SIZE_SHOW_NAVIGATION ? YES : NO).append("\n") ;
    	sb.append("Lobby Fullscreen").append(" ").append(QuantroPreferences.getScreenSizeLobby(this) == QuantroPreferences.XL_SIZE_SHOW_NAVIGATION ? YES : NO).append("\n") ;
    	sb.append("Game Fullscreen").append("  ").append(QuantroPreferences.getScreenSizeGame(this) == QuantroPreferences.XL_SIZE_SHOW_NAVIGATION ? YES : NO).append("\n") ;
    	sb.append("\n") ;
    	sb.append("Game Size").append("        ").append(gvmc.getScreenWidthString() + X + gvmc.getScreenHeightString()).append("\n") ;
    	sb.append("Heap MB").append("          ").append(gvmc.getHeapMBString()).append("\n") ;
    	sb.append("Large Heap MB").append("    ").append(gvmc.getLargeHeapMBString()).append("\n") ;
    	sb.append("Load Image Size").append("  ").append(gvmc.getLoadImagesSizeString()).append("\n") ;
    	sb.append("BG Image Size").append("    ").append(gvmc.getBackgroundImageSizeString()).append("\n") ;
    	sb.append("Blit").append("             ").append(gvmc.getBlitString()).append("\n") ;
    	sb.append("Recycle to Veil").append("  ").append(gvmc.getRecycleToVeil() ? YES : NO).append("\n") ;
    	sb.append("Skip Animations").append("  ").append(QuantroPreferences.getGraphicsSkipAnimations(this) ? YES : NO).append("\n") ;
    	sb.append("-------------------------\n\n") ;
    	
    	emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString()) ;
    	
    	startActivity(emailIntent);  
    }
    
    protected void startQuantroPreferencesActivity() {
    	// launch settings
		Intent intent = new Intent( this, QuantroPreferences.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	startActivity(intent) ;
    	//Log.d(TAG, "onOptionsItemSelected bracket OUT") ;
    }
    
    /**
     * "Quick-Starts" a game setup, either ExamineGameResult or NewGameSetup.
     */
    protected void quickStartGameSetupActivity() {
    	Log.d(TAG, "quick-starting a game setup activity") ;
    	
    	GameSettings gs = GameSettingsDatabaseAdapter.getMostRecentSinglePlayerInDatabase(this) ;
    	
    	String saveKey = gameModeToSaveKey(gs.getMode()) ;
    	
    	// check for a saved game.
    	if ( GameSaver.hasGameStates( this, saveKey ) )
    		startExamineGameResultActivity( gs.getMode(), saveKey ) ;
    	else
    		startNewGameSetupActivity( gs.getMode() ) ;
    }
    
    protected void startExamineGameResultActivity( int gameMode ) {
    	startExamineGameResultActivity( gameMode, gameModeToSaveKey(gameMode) ) ;
    }
    
    protected void startExamineGameResultActivity( int gameMode, String saveKey ) {
    	GameResult gr = GameSaver.loadGameResult(this, saveKey) ;
		GameSettings gs = GameSaver.hasGameSettings(this, saveKey) ? GameSaver.loadGameSettings(this, saveKey) : null ;
		
		// Launch an ExamineGameResultActivity.
		Intent intent = new Intent( this, ExamineGameResultActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_STYLE,
    			ExamineGameResultActivity.STYLE_SAVED_GAME) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_GAME_RESULT,
    			gr) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_GAME_SETTINGS,
    			gs) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_ECHO,
    			GameModes.gameModeIntegerObject(gameMode)) ;
    	
    	startActivityForResult(intent, IntentForResult.REQUEST_EXAMINE_GAME_RESULT) ;
    }
    
    protected void startNewGameSetupActivity( int gameMode ) {
    	Intent intent = new Intent( this, NewGameSettingsActivity.class ) ;
		intent.setAction( Intent.ACTION_MAIN ) ;
		intent.putExtra( 
				NewGameSettingsActivity.INTENT_EXTRA_STYLE,
				NewGameSettingsActivity.STYLE_NEW_SINGLE_PLAYER_GAME) ;
		intent.putExtra( 
				NewGameSettingsActivity.INTENT_EXTRA_GAME_MODE,
				gameMode) ;
		intent.putExtra( 
				NewGameSettingsActivity.INTENT_EXTRA_ECHO,
				GameModes.gameModeIntegerObject(gameMode)) ;
		
		startActivityForResult(intent, IntentForResult.NEW_GAME_SETTINGS) ;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // ACTIVITY RESULT METHODS
    // If we launch an activity for a result, it comes back here.
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if ( super.handleActivityResult(requestCode, resultCode, data) )
    		return ;
    	
    	//Log.d(TAG, "onActivityResult bracket IN") ;
    	Log.d(TAG, "onActivityResult") ;
    	switch( requestCode ) {
    	
    	case IntentForResult.FIRST_TIME_SETUP:
    		Log.d(TAG, "result_first_time_setup") ;
    		// We are mainly interested in failure.
    		if ( resultCode != Activity.RESULT_OK ) {
    			this.finish() ;
    		}
    		return ;
    	
    	case IntentForResult.REQUEST_EXAMINE_GAME_RESULT:
    		if (resultCode == RESULT_OK) {  
            	
            	if ( data != null && data.hasExtra(ExamineGameResultActivity.INTENT_RESULT_EXTRA) ) {
            		int action = data.getIntExtra(ExamineGameResultActivity.INTENT_RESULT_EXTRA, -1) ;
            		if ( action == ExamineGameResultActivity.INTENT_RESULT_CONTINUE ) {
            			Integer gameMode = (Integer)data.getSerializableExtra(ExamineGameResultActivity.INTENT_EXTRA_ECHO) ;
            			String saveKey = gameModeToSaveKey(gameMode) ;
            			startGameActivity( gameMode, saveKey, saveKey, false, true ) ;
            			return ;
            		}
            		else if ( action == ExamineGameResultActivity.INTENT_RESULT_DELETE ) {
            			Integer gameMode = (Integer)data.getSerializableExtra(ExamineGameResultActivity.INTENT_EXTRA_ECHO) ;
            			String saveKey = gameModeToSaveKey(gameMode) ;
            			GameResult gr = GameSaver.loadGameResult(this, saveKey) ;
            			GameStats.DatabaseAdapter.addGameResultToDatabase(this, gr) ;
            			GameSaver.deleteGame(this, saveKey) ;
            			
            			if ( QuantroPreferences.getAnalyticsActive(this) )
            				doLogDelete( gameMode, gr, true ) ;
            		}
            	}
          
            } else {  
                // gracefully handle failure 
            } 
    		return ;
    		
    	case IntentForResult.NEW_GAME_SETTINGS:
    		if ( resultCode == RESULT_OK ) {
    			if ( data != null && data.hasExtra(NewGameSettingsActivity.INTENT_RESULT_EXTRA_ACTION) ) {
    				int action = data.getIntExtra(NewGameSettingsActivity.INTENT_RESULT_EXTRA_ACTION, -1) ;
    				if ( action == NewGameSettingsActivity.INTENT_RESULT_EXTRA_ACTION_PLAY ) {
    					Integer gameMode = (Integer)data.getSerializableExtra(NewGameSettingsActivity.INTENT_EXTRA_ECHO) ;
    					String saveKey = gameModeToSaveKey(gameMode) ;
    					
    					// get settings
    					GameSettings gs ;
    					if ( data.hasExtra( NewGameSettingsActivity.INTENT_RESULT_EXTRA_GAME_SETTINGS ) )
    						gs = (GameSettings)data.getSerializableExtra( NewGameSettingsActivity.INTENT_RESULT_EXTRA_GAME_SETTINGS ) ;
    					else
    						gs = QuantroPreferences.getRememberCustomSetup(this)
    								? GameSettingsDatabaseAdapter.getMostRecentInDatabase(this, gameMode)
    								: new GameSettings(gameMode, 1).setImmutable() ;

    					startGameActivity( saveKey, gs, false, true ) ;
    				}
    			}
    		} else {
    			// gracefully handle failure
    		}
    		return ;
    		
    	case IntentForResult.LAUNCH_GAME:
    		Log.d(TAG, "result_launch_game") ;
    		if ( data != null && data.hasExtra(GameActivity.INTENT_EXTRA_GAME_RESULT) ) {
    			GameResult gr = (GameResult)data.getSerializableExtra(GameActivity.INTENT_EXTRA_GAME_RESULT) ;
    			
    			// We can safely assume a single player game.  If the game
    			// is over, put it in our Records database and delete
    			// the save.  Regardless, update the GameResults.
    			if ( gr != null ) {
    				int gameMode = gr.getGameInformation(0).mode ;
    				boolean hasSave = true ;
    				String key = gameModeToSaveKey(gameMode) ;
    				
    				if ( gr.getTerminated() ) {
	    				// update our database...
	    				GameStats.DatabaseAdapter.addGameResultToDatabase(this, gr) ;
	    				// Delete the saved game.
	    				GameSaver.deleteGame(this, key) ;
	    				
	    				if ( QuantroPreferences.getAnalyticsActive(this) )
            				doLogDelete( gameMode, gr, false ) ;
            			
	    				hasSave = false ;
    				} else {
    					hasSave = GameSaver.hasGameStates(this, key) ;
			}
        			
    			}
    			
    			// hey, why not GC?
    			System.gc() ;
    		}
    		return ;
    	
    	}

		super.onActivityResult(requestCode, resultCode, data);
    }
    
    
    private void doLogDelete( int gameMode, GameResult gr, boolean userAction ) {
    	//Log.d(TAG, "doLogDelete bracket IN") ;
    	if ( gr == null ) {
    		//Log.d(TAG, "doLogDelete bracket OUT") ;
    		return ;
    	}
    	
    	long timeSinceStarted = -1 ;
    	long timeSinceLast = -1 ;
    	long timeSpent = gr.getTimeInGame() ;
    	
    	Date dateStarted = gr.getDateStarted() ;
    	Date dateEnded = gr.getDateEnded() ;
    	if ( dateStarted != null )
    		timeSinceStarted = System.currentTimeMillis() - dateStarted.getTime() ;
    	if ( dateEnded != null )
    		timeSinceLast = System.currentTimeMillis() - dateEnded.getTime() ;
    	
    	Analytics.logDeleteGame(gameMode, timeSinceStarted, timeSinceLast, timeSpent, userAction) ;
    	//Log.d(TAG, "doLogDelete bracket OUT") ;
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // ITEM ALERTS
    // Perform the computation necessary to update an alert.
    
    public void updateAlertWifiMultiplayer() {
    	//Log.d(TAG, "updateAlertWifiMultiplayer bracket IN") ;
    	// Use another runnable to start the AsyncTask on the UI thread
        // (possibly helps to resolve an Android 1.6 bug?)
    	// Make sure we run this on the UI thread!
        new RefreshAlertWifiMultiplayerAsyncTask().executeOnUiThread() ;
    	//Log.d(TAG, "updateAlertWifiMultiplayer bracket OUT") ;
    }
    
    public void lobbyFinderFoundNewLobby( WiFiLobbyFinder finder, String key, WiFiLobbyDetails desc ) {
    	//Log.d(TAG, "lobbyFinderFoundNewLobby bracket IN") ;
    	// refresh.
    	mHandler.postDelayed(mRunnableWifiLobbyAlertRefresh,
    			Math.max( MainMenuActivity.TIME_BETWEEN_WIFI_REFRESH_MIN
    						- (System.currentTimeMillis() - this.mWifiLobbyAlertLastRefreshTime),
    					0)) ;
    	//Log.d(TAG, "lobbyFinderFoundNewLobby bracket OUT") ;
    }
    
	public void lobbyFinderHasLobbyUpdate( WiFiLobbyFinder finder, String key, WiFiLobbyDetails desc ) {
		//Log.d(TAG, "lobbyFinderHasLobbyUpdate bracket IN") ;
		// refresh.
    	mHandler.postDelayed(mRunnableWifiLobbyAlertRefresh,
    			Math.max( MainMenuActivity.TIME_BETWEEN_WIFI_REFRESH_MIN
    						- (System.currentTimeMillis() - this.mWifiLobbyAlertLastRefreshTime),
    					0)) ;
    	//Log.d(TAG, "lobbyFinderHasLobbyUpdate bracket OUT") ;
	}
	
	public void lobbyFinderLobbyVanished( WiFiLobbyFinder finder, String key ) {
		//Log.d(TAG, "lobbyFinderLobbyVanished bracket IN") ;
		// refresh.
    	mHandler.postDelayed(mRunnableWifiLobbyAlertRefresh,
    			Math.max( MainMenuActivity.TIME_BETWEEN_WIFI_REFRESH_MIN
    						- (System.currentTimeMillis() - this.mWifiLobbyAlertLastRefreshTime),
    					0)) ;
    	//Log.d(TAG, "lobbyFinderLobbyVanished bracket OUT") ;
	}
    
    
    ////////////////////////////////////////////////////////////////////////////
    // ARRAY ADAPTERS
    // Used to populate our ListViews.
	
	
	// Indexer for MenuItems.
	private class MenuItemIndexer implements SectionIndexer {
		
		private int mNumItems ;
		private String [] mSectionTitles ;
		private int [] mSectionFirstPosition ;
		
		private MenuItemIndexer( int parentMenuItem, int [] menuItems ) {
			// New sections begin every time a new category appears.
			// New categories appear when:
			// 1. The category explicitly changes, or
			// 2. The category is the parent item, or
			// 3. The category is the menu item itself.
			
			// In case 1, we use the category title as our section title.
			// In 2 or 3, we use the menu item title as our section title.
			int numSections = 0 ;
			int lastCategory = Integer.MIN_VALUE ;
			for ( int i = 0; i < menuItems.length; i++ ) {
				int item = menuItems[i] ;
				int category = mMenuHierarchyCategory[item] ;
				if ( category == parentMenuItem )
					category = item ;
				if ( lastCategory != category || category == item ) {
					numSections++ ;
					lastCategory = category ;
				}
			}
			// counted...
			
			// ...set section titles and "first in section" values.
			int index = 0 ;
			mSectionTitles = new String[numSections] ;
			mSectionFirstPosition = new int[numSections] ;
			lastCategory = Integer.MIN_VALUE ;
			for ( int i = 0; i < menuItems.length; i++ ) {
				int item = menuItems[i] ;
				int category = mMenuHierarchyCategory[item] ;
				if ( category == parentMenuItem )
					category = item ;
				if ( lastCategory != category || category == item ) {
					mSectionTitles[index] = mMenuItemTitles[category] ;
					mSectionFirstPosition[index] = i ;
					lastCategory = category ;
					index++ ;
				}
			}
			
			mNumItems = menuItems.length ;
		}

		@Override
		public int getPositionForSection(int section) {
			if ( section < 0 )
				return -1 ;
			if ( section >= mSectionFirstPosition.length )
				return mNumItems ;
			return mSectionFirstPosition[section] ;
		}

		@Override
		public int getSectionForPosition(int position) {
			if ( position < 0 )
				return - 1 ;
			for ( int i = 0; i < mSectionFirstPosition.length -1; i++ ) {
				if ( mSectionFirstPosition[i] <= position && position < mSectionFirstPosition[i+1] ) {
					return i ;
				}
			}
			return mSectionFirstPosition.length -1 ;
		}
		
		public boolean isFirstInSection(int position) {
			for ( int i = 0; i < mSectionFirstPosition.length; i++ ) {
				if ( mSectionFirstPosition[i] == position )
					return true ;
			}
			return false ;
		}

		@Override
		public Object[] getSections() {
			return mSectionTitles ;
		}
		
		public int numSections() {
			return mSectionTitles.length ;
		}
	}
	
	
	private static class MenuItemListRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		
		public MenuItemListRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
		}
	}
	
	
	private static class MenuItemListItemTag {
		MainMenuButtonStrip mMBS ;
		
		public MenuItemListItemTag( View v ) {
			mMBS = (MainMenuButtonStrip) v.findViewById( R.id.main_menu_button_strip ) ;
		}
	}
	
    
    private class MenuItemArrayAdapter extends SectionableAdapter
    		implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {   	
    	
    	int [] mMenuItems ;
    	MenuItemIndexer mIndexer ;
    	Hashtable<Integer, MainMenuButtonStrip> views = new Hashtable<Integer, MainMenuButtonStrip>() ;
    	
    	boolean mScrolledToTop ;
    	
    	public MenuItemArrayAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
    		super(inflater, rowLayoutID, headerID, itemHolderID,
    				SectionableAdapter.CellVisibility.GONE_IF_FIRST_ROW_IN_SECTION);
			
			mMenuItems = new int[0] ;
			mIndexer = null ;
			views = new Hashtable<Integer, MainMenuButtonStrip>() ;
			
			mScrolledToTop = true ;
    	}
    	
    	////////////////////////////////////////////////////////////////////////
    	// Setting Contents
    	// Our operation is complex: we configure category headers, contents, etc.
    	// using our own calculations.  All we need is the "root item" which we are
    	// representing: we read from mMenuHierarchy and mMenuHierarchyCategory from
    	// that data.
    	
    	synchronized public void setRootItem(int rootItem) {
    		mMenuItems = mMenuHierarchy[rootItem].clone() ;
    		mIndexer = mMenuItemsPerRow == 1 ? null : new MenuItemIndexer(rootItem, mMenuItems) ;
    		views.clear() ;
    		
    		mScrolledToTop = true ;
    		notifyDataSetChanged() ;
    	}
    	
    	
    	synchronized public void refreshMenuItem( Integer menuItem ) {
    		//Log.d(TAG, "MenuItemArrayAdapter.refreshMenuItem bracket IN") ;
    		if ( views.containsKey(menuItem) ) {
    			// Log.d(TAG, "MenuItemArrayAdapter, contains " + menuItem + " doing refresh.") ;
    			views.get(menuItem).refresh() ;
    			this.notifyDataSetChanged() ;
    		}
    		//Log.d(TAG, "MenuItemArrayAdapter.refreshMenuItem bracket OUT") ;
    	}
    	
    	synchronized public int getPositionForSection(int sectionIndex) {
            if (mIndexer == null) {
                return -1;
            }

            // this method is called by PinnedHeaderListView, which -- as
            // far as it is aware -- is indexing ROWS, not entries.
            // Perform a conversion.
            return this.getRowPosition( mIndexer.getPositionForSection(sectionIndex) );
        }
		
		synchronized public int getSectionForPosition(int position) {
            if (mIndexer == null) {
                return -1;
            }

            // this method is called by PinnedHeaderListView, which -- as
            // far as it is aware -- is indexing ROWS, not entries.
            // Perform a conversion.
            return mIndexer.getSectionForPosition( getRealPosition(position) );
        }
		
		@Override
		synchronized public Object[] getSections() {
            if (mIndexer == null) {
                return new String[] { " " };
            } else {
                return mIndexer.getSections();
            }
		}
		
		
		@Override
		synchronized public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			View topChild = view.getChildAt(0) ;
		    MenuItemListRowTag tag = null ;
			if ( topChild != null )
				tag = ((MenuItemListRowTag)topChild.getTag()) ;
			
			mScrolledToTop = firstVisibleItem == 0 && visibleItemCount > 0 && topChild.getTop() == 0 ;
			
			if (view instanceof PinnedHeaderListView && mIndexer != null) {
				boolean topHasHeader = mIndexer.isFirstInSection(firstVisibleItem) ;
				// Previously: we assumed that headers have a fixed size, and thus the
				// moment when one header reaches the top of the screen is the moment 
				// when the previous header has vanished.  However, we have started
				// experimenting with variable-height headers: specifically, the pinned
				// header (and top header) is short, w/out much spacing, while in-list
				// headers after the first have a large amount of leading padding.
				// We only present the current position if its EFFECTIVE header has
				// reached the top of the screen.
				// For quantro_list_item_header, this is the case when topChild.y +
				// the in-child y position of the header is <= 0.
				boolean headerNotYetInPosition = ( tag != null ) && topHasHeader && firstVisibleItem != 0
				&& ( topChild.getTop() + tag.mHeaderTextView.getTop() > 0 ) ;
				
                ((PinnedHeaderListView) view).configureHeaderView(
                		headerNotYetInPosition ? firstVisibleItem -1 : firstVisibleItem,
                		!headerNotYetInPosition );
            }	
		}

		@Override
		synchronized public void onScrollStateChanged(AbsListView arg0, int arg1) {
		}

		@Override
		synchronized public int getPinnedHeaderState(int position) {
            if (mIndexer == null || getCount() == 0) {
                return PINNED_HEADER_STATE_GONE;
            }

            if (position < 0) {
                return PINNED_HEADER_STATE_GONE;
            }
            
            if ( position == 0 && mScrolledToTop )
            	return PINNED_HEADER_STATE_GONE ;

            // The header should get pushed up if the top item shown
            // is the last item in a section for a particular letter.
            int section = getSectionForPosition(position);
            if ( section < 0 )
            	return PINNED_HEADER_STATE_GONE ;
            
            int nextSectionPosition = getPositionForSection(section + 1);
            
            if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
                return PINNED_HEADER_STATE_PUSHED_UP;
            }

            return PINNED_HEADER_STATE_VISIBLE;
		}
		
		@Override
        public int pha_getPinnedHeaderFadeAlphaStyle() {
			return PinnedHeaderListView.PinnedHeaderAdapter.PINNED_HEADER_FADE_ALPHA_HEADER_NONCONTENT_OVERLAP ;
		}
        
        
		Rect tempPaddingRect = new Rect() ;
        
		@Override
        public boolean copyPinnedHeaderOccludingBounds(View header, int position, Rect r) {
			MenuItemListRowTag tag = (MenuItemListRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	MenuItemListRowTag tag = (MenuItemListRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	MenuItemListRowTag tag = (MenuItemListRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	MenuItemListRowTag tag = (MenuItemListRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderVisibleBounds( tag.mHeaderView, position, r ) ;
        }
		
		@Override
		synchronized public void configurePinnedHeader(View v, int position, int alpha) {
			final int section = getSectionForPosition(position);
			if ( section >= 0 ) {
				final String title = (String) getSections()[section];
				
				MenuItemListRowTag tag = (MenuItemListRowTag)v.getTag() ;
				tag.mHeaderTextView.setText(title);
				tag.mHeaderViewTopSpacer.setVisibility(View.GONE) ;
				VersionSafe.setAlpha(v, alpha / 255f) ;
			}
		}
		
		@Override
        synchronized public int nextHeaderAfter(int position) {
			int section = getSectionForPosition( position ) ;
			if ( section == -1 )
				return -1 ;
			
			return getPositionForSection(section+1) ;
		}
		
		
		@Override
		public Object getItem(int position) {
			return this.mMenuItems[position] ;
		}


		@Override
		protected int getDataCount() {
			return this.mMenuItems.length ;
		}


		@Override
		protected int getSectionsCount() {
			// NOTE: the current implementation of SectionableAdapter
			// calls this method exactly once, so we can't adjust
			// the sections over time (e.g. a new section for specific
			// custom game modes).  Consider how to change and/or implement
			// this if we need adaptable section numbers.  We might not,
			// even if we add/remove sections, so long as we can bound the
			// number of sections in advance.
			if ( mIndexer == null )
				return 1 ;
			return mIndexer.numSections() ;
		}


		@Override
		protected int getCountInSection(int index) {
			if ( mIndexer == null )
				return mMenuItems.length ;
			
			// returns the number of items within the specified section.
			// this is the difference between getPositionForSection(index+1)
			// and getPositionForSection(index).  getPositionForSection will
			// return the total number of items if called with a section index
			// that is out of bounds.
			
			// note that our implementation of getPositionForSection works
			// on the View (row) level, whereas our indexer works on the item
			// (real position) level.
			return mIndexer.getPositionForSection(index+1) - mIndexer.getPositionForSection(index) ;
		}
		
		
		@Override
		protected int getTypeFor(int position) {
			// called by SectionableAdapter; uses real-positions.
			if ( mIndexer == null )
				return 0 ;
			
			return mIndexer.getSectionForPosition(position) ;
		}


		@Override
		protected String getHeaderForSection(int section) {
			return null ;
		}
		
		
		@Override
		protected void bindView(View cell, int position) {
			int menuItem = mMenuItems[position] ;
			
			MenuItemListItemTag tag = (MenuItemListItemTag)cell.getTag() ;
    		if ( tag == null ) {
				tag = new MenuItemListItemTag(cell) ;
				if ( tag.mMBS != null ) {
					tag.mMBS.setMenuItemTitles(mMenuItemTitles) ;
					tag.mMBS.setMenuItemDescriptions(mMenuItemDescriptions) ;
					tag.mMBS.setMenuItemDescriptionsDisabled(mMenuItemDescriptionsDisabled) ;
					tag.mMBS.setMenuItemColors(mMenuItemColors) ;
					tag.mMBS.setMenuItemAlertDefaultDrawable(mMenuItemAlertOverwriteDrawable) ;
					tag.mMBS.setDelegate(MainMenuActivity.this) ;
					
				} 
				cell.setTag(tag) ;
			}
    		
    		QuantroApplication app = (QuantroApplication)getApplication() ;
			boolean enabled = true ;
			if ( !VersionCapabilities.isEmulator() && menuItem == MENU_ITEM_INTERNET_MULTI_PLAYER
					&& (DEMO_VERSION || DEMO_VERSION_ALLOW_WIFI) )
				enabled = false ;
			else if ( menuItem == MENU_ITEM_WIFI_MULTI_PLAYER
					&& (DEMO_VERSION || !app.mSupportsFeatureWifi) )
				enabled = false ;
			else if ( !VersionCapabilities.isEmulator() && menuItem == MENU_ITEM_MULTI_PLAYER
					&& (DEMO_VERSION || !(app.mSupportsFeatureTelephony || app.mSupportsFeatureWifi)) )
				enabled = false ;
			
			// Set menu item.  This call automatically refreshes.
			if ( tag.mMBS != null ) {
				tag.mMBS.setMenuItemEnabled(enabled) ;
				tag.mMBS.setMenuItem(menuItem) ;
				
				// adjust view in the hashtable
				Set<Entry<Integer, MainMenuButtonStrip>> entrySet = views.entrySet() ;
				Iterator<Entry<Integer, MainMenuButtonStrip>> iter = entrySet.iterator() ;
				for ( ; iter.hasNext() ; ) {
					Entry<Integer, MainMenuButtonStrip> entry = iter.next() ;
					if ( entry.getValue() == tag.mMBS )
						iter.remove() ;
				}
				views.put(menuItem, tag.mMBS) ;
			}
			
			// set height to ideal height.
			if ( tag.mMBS != null )
				tag.mMBS.getLayoutParams().height = tag.mMBS.getIdealHeight() ;
		}
		
		
		
		/**
		 * Perform any row-specific customization your grid requires. For example, you could add a header to the
		 * first row or a footer to the last row.
		 * @param row the 0-based index of the row to customize.
		 * @param rowView the inflated row View.
		 */
		@Override
		protected void customizeRow(int row, int firstPosition, View rowView) {
			// This is where we perform necessary header configuration.
			
			MenuItemListRowTag tag = ((MenuItemListRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new MenuItemListRowTag( rowView ) ;
				rowView.setTag(tag) ;
				if ( tag.mHeaderView != null )
					tag.mHeaderView.setTag(tag) ;
			}
        	
	        final int section = getSectionForPosition(row);
	        final int sectionPosition = getPositionForSection(section) ;
	        if (section >= 0 && sectionPosition == row) {
	            String title = (String) mIndexer.getSections()[section];
	            tag.mHeaderTextView.setText(title);
	            tag.mHeaderView.setVisibility(View.VISIBLE);
		    	// the first item does not get a spacer; the rest of the headers do.
	            tag.mHeaderViewTopSpacer.setVisibility( firstPosition == 0 ? View.GONE : View.VISIBLE ) ;
	        } else {
	        	tag.mHeaderView.setVisibility(View.GONE);
		    	//dividerView.setVisibility(View.VISIBLE);
	        }
		}
    	
    }

    
    
    synchronized public String gameModeToSaveKey( Integer gameMode ) {
		String res = "sp_save_" + gameMode.toString() ;
		return res ;
	}
    
    
    
    /**
     * Attempts to recycle the cached bitmaps used in drawing the logo.
     * These bitmaps will automatically be regenerated the next time the
     * view is drawn.
     */
    public void recycleLogoUntilDraw() {
    	runOnUiThread( new Runnable() {
    			public void run() {
    				mTitleBarButtonStrip.recycleBlockDrawerUntilDraw() ;
    			}
    	})  ;
    }
    
    
    /*
	 * *************************************************************************
	 * 
	 * WAKE LOCK COLLECTION
	 * 
	 * *************************************************************************
	 */
    
    private static final String LOCK_NAME_DIM_STATIC="quantro:main_menu_dim_lock";
    private static final String LOCK_NAME_PARTIAL_STATIC="quantro:main_menu_dim_lock";
	private static volatile PowerManager.WakeLock lockDimStatic=null ;
	private static volatile PowerManager.WakeLock lockPartialStatic=null ;
    
    synchronized private static PowerManager.WakeLock getDimLock(Context context) {
    	if (lockDimStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lockDimStatic=mgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
					LOCK_NAME_DIM_STATIC);
			lockDimStatic.setReferenceCounted(true);
		}
	  
		return(lockDimStatic);
    }
    
    synchronized private static PowerManager.WakeLock getWakeLock(Context context) {
    	if (lockPartialStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lockPartialStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					LOCK_NAME_PARTIAL_STATIC);
			lockPartialStatic.setReferenceCounted(true);
		}
	  
		return(lockPartialStatic);
    }
    
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // MAIN MENU BUTTON STRIP LISTENER METHODS
    // Used when someone touches a main menu item.
    
    /**
	 * The user has short-clicked a button on this strip.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.  We also include the menu item number represented here.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param menuItemNumber
	 */
	public boolean mmbs_onButtonClick(
			MainMenuButtonStrip strip,
			int buttonNum, int buttonType, int menuItemNumber, boolean enabled ) {
		
		Log.d(TAG, "mmbs_onButtonClick button num "+ buttonNum + " type " + buttonType + " menuItemNumber " + menuItemNumber + " enabled " + enabled) ;
		
		// Action is dependent on the menuItemNumber.  For many menu items,
		// the appropriate action is to transition one step deeper in the hierarchy.
		// Some buttons, such as our multiplayer buttons, launch a new Activity.
		// Other buttons, such as the Single Player button, displays a specially
		// configured ListView in the flipper that is NOT part of the main
		// menu hierarchy.
		
		// Unfortunately, since we loaded menu items from resources, they
		// are not considered 'final' values and so we cannot switch on them.
		if ( menuItemNumber == MENU_ITEM_ROOT )
			throw new IllegalStateException("Root doesn't get a list item, and yet it has been touched.") ;
		
		// For right now, we only have 1 button.
		if ( buttonNum != 0 || buttonType != MainMenuButtonStrip.BUTTON_TYPE_MAIN )
			throw new IllegalStateException("Can't handle a button type other than MAIN (was " + buttonType + ") or a number other than 0 (was " + buttonNum + ")") ;
		
		// Generalized handling for any inner list item.
		if ( enabled && mMenuHierarchy[menuItemNumber] != null && mMenuHierarchy[menuItemNumber].length > 0 ) {
			// transition to this item.
			prepareMenuListLayer( mDisplayedMenuLayer+1, menuItemNumber ) ;
			showMenuListLayer( mDisplayedMenuLayer+1 ) ;
			if ( mButtonSounds )
				mSoundPool.menuButtonClick() ;
			//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
			return true ;
		}
		
		////////////////////////////////////////////////////////////////////////
		// Specialized handling for leaf items.
		if ( enabled && menuItemNumber == MENU_ITEM_SINGLE_PLAYER ) {
			// Launch the "Free Play Game Manager Activity."
			Intent intent = new Intent( this, FreePlayGameManagerActivity.class ) ;
	    	intent.setAction( Intent.ACTION_MAIN ) ;
	    	startActivity(intent) ;
	    	if ( mButtonSounds )
	    		mSoundPool.menuButtonClick() ;
	    	//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
	    	return true ;
		}
		
		else if ( enabled && menuItemNumber == MENU_ITEM_WIFI_MULTI_PLAYER ) {
			// Launch the "Lobby Manager Activity."
			Intent intent = new Intent( this, WifiLobbyManagerActivity.class ) ;
	    	intent.setAction( Intent.ACTION_MAIN ) ;
	    	startActivity(intent) ;
	    	if ( mButtonSounds )
	    		mSoundPool.menuButtonClick() ;
	    	//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
	    	return true ;
		}
		
		else if ( menuItemNumber == MENU_ITEM_INTERNET_MULTI_PLAYER ) {
			if ( enabled ) {
				// Launch the "Internet Lobby Manager Activity."
				Intent intent = new Intent( this, InternetLobbyManagerActivity.class ) ;
		    	intent.setAction( Intent.ACTION_MAIN ) ;
		    	startActivity(intent) ;
		    	if ( mButtonSounds )
		    		mSoundPool.menuButtonClick() ;
		    	//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
		    	return true ;
			} 
		}
		
		else if ( enabled && menuItemNumber == MENU_ITEM_ACHIEVEMENTS ) {
			openAchievements() ;
			if ( mButtonSounds )
	    		mSoundPool.menuButtonClick() ;
	    	//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
	    	return true ;
		}
		
		else if ( enabled && menuItemNumber == MENU_ITEM_LEADERBOARDS ) {
			openLeaderboards() ;
			if ( mButtonSounds )
	    		mSoundPool.menuButtonClick() ;
	    	//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
	    	return true ;
		}
		
		
		
		else if ( enabled && menuItemNumber == MENU_ITEM_SETTINGS ) {
			// Launch QuantroPreferencesActivity.
			startQuantroPreferencesActivity() ;
			if ( mButtonSounds )
	    		mSoundPool.menuButtonClick() ;
	    	//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
	    	return true ;
		}
		
		else if ( enabled && menuItemNumber == MENU_ITEM_FEEDBACK_EMAIL ) {
			emailDeveloper() ;
			if ( mButtonSounds )
	    		mSoundPool.menuButtonClick() ;
	    	return true ;
		}
		
		else if ( enabled && menuItemNumber == MENU_ITEM_WHATS_NEW ) {
			mDialogManager.showDialog(GlobalDialog.DIALOG_ID_WHATS_NEW) ;
			if ( mButtonSounds )
	    		mSoundPool.menuButtonClick() ;
			return true ;
		}
		
		// GENERIC CASE: If none of the above, maybe we have HTML to view?
		// Send it to the browser if we do.
		else if ( enabled && mMenuLeafHTMLPath[ menuItemNumber ] != null ) {
			// Two cases.  If prepended by 'http://', it is a real URL
			// and we should use the browser application.  Otherwise,
			// it might not be world-readable and we should give it to
			// our HtmlViewerActivity.
			if ( mMenuLeafHTMLPath[menuItemNumber].startsWith("http://") ) {
				Intent intent = new Intent( Intent.ACTION_VIEW );
				intent.setData( Uri.parse( mMenuLeafHTMLPath[menuItemNumber] ) );
				startActivity(intent);
				if ( mButtonSounds )
					mSoundPool.menuButtonClick() ;
				//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
				return true ;
			}
			else {
				// Looks like a asset file.
				/*
				String localFilePath = AssetAccessor.assetRelativeToLocalStorageAbsolute(
						this,
						mMenuLeafHTMLPath[menuItemNumber]) ;
				String referencesRelativeTo = AssetAccessor.getLocalStorageAbsolute(this) ;
				*/
				String assetFilePath = mMenuLeafHTMLPath[menuItemNumber] ;
				
				Intent intent = new Intent(this, HtmlViewerActivity.class) ;
				intent.setAction( Intent.ACTION_VIEW ) ;
				intent.putExtra( HtmlViewerActivity.INTENT_EXTRA_ASSET_PATH, assetFilePath) ;
				//intent.putExtra( HtmlViewerActivity.INTENT_EXTRA_PATH, localFilePath) ;
				//intent.putExtra( HtmlViewerActivity.INTENT_EXTRA_LOAD_PATH_RELATIVE_TO, referencesRelativeTo) ;
				startActivity(intent) ;
				if ( mButtonSounds )
					mSoundPool.menuButtonClick() ;
				//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
				return true ;
			}
		}
		
		// TODO: Fill in the rest, dude!
		//Log.d(TAG, "mmbs_onButtonClick bracket OUT") ;
		return false ;
	}
	
	/**
	 * The user has long-clicked a button we for game launches.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.  We also include the menu item number represented here.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param menuItemNumber
	 */
	public boolean mmbs_onButtonLongClick(
			MainMenuButtonStrip strip,
			int buttonNum, int buttonType, int menuItemNumber, boolean enabled ) {
		
		//Log.d(TAG, "mmbs_onButtonLongClick bracket IN") ;
		//Log.d(TAG, "mmbs_onButtonLongClick bracket OUT") ;
		
		// TODO: Long-press displays information about the menu item.
		// Put the code in here.
		return false ;
	}
	
	
	
	/**
	 * The user has long-clicked a button we for game launches.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.  We also include the menu item number represented here.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param menuItemNumber
	 */
	public boolean mmbs_supportsLongClick(
			MainMenuButtonStrip strip,
			int buttonNum, int buttonType, int menuItemNumber, boolean enabled ) {
		
		//Log.d(TAG, "mmbs_onButtonLongClick bracket IN") ;
		//Log.d(TAG, "mmbs_onButtonLongClick bracket OUT") ;
		
		// TODO: Long-press displays information about the menu item.
		// Put the code in here.
		return false ;
	}
	
	
    
    ////////////////////////////////////////////////////////////////////////////
    // MAIN MENU TITLE BAR BUTTON STRIP LISTENER METHODS
    // Used when someone touches a button along the main menu title bar.
	
	
	/**
	 * The user has short-clicked a button on this strip.  If buttonNum == 0,
	 * be assured that it is the main button.  If > 0, it is some other button,
	 * and as mentioned in the class description, the content and effect are
	 * completely up to the outside world to determine.  If you need to check
	 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
	 * 
	 * The menuItemNumber provided is the currently displayed menu item
	 * according to the strip's records.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param menuItemNumber
	 */
	public boolean mmtbbs_onButtonClick(
			MainMenuTitleBarButtonStrip strip,
			int buttonNum, int menuItemNumber, boolean asOverflow ) {
		
		Log.d(TAG, "mmtbbs_onButtonClick click button " + buttonNum) ;
		
		// The main button has navigation effects.
		switch( buttonNum ) {
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_MAIN:
			// If we are at root, this (for now) has no effect.
			if ( menuItemNumber == MENU_ITEM_ROOT ) {
				//Log.d(TAG, "mmtbbs_onButtonClick bracket OUT") ;
				return false ;
			}
				
			// If we are somewhere else, this returns to root.
			// Remember that we only need to "prepare" a layer 
			// when going FORWARD - this is going BACK.
			if ( mButtonSounds && !asOverflow )
				mSoundPool.menuButtonBack() ;
			showMenuListLayer( 0 ) ;
			return true ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_QUANTRO:
			// QUICK-PLAY!
			quickStartGameActivity() ;
			if ( mButtonSounds && !asOverflow )
				mSoundPool.menuButtonClick() ;
			return true ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_ACHIEVEMENTS:
			// Open achievements OR alert.
			openAchievements() ;
			if ( mButtonSounds && !asOverflow )
				mSoundPool.menuButtonClick() ;
			return true ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_LEADERBOARDS:
			// Open achievements OR alert.
			openLeaderboards() ;
			if ( mButtonSounds && !asOverflow )
				mSoundPool.menuButtonClick() ;
			return true ;
	    	
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_HELP:
			mDialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
			if ( mButtonSounds && !asOverflow )
				mSoundPool.menuButtonClick() ;
	    	return true ;
	    	
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_SETTINGS:
			startQuantroPreferencesActivity() ;
			if ( mButtonSounds && !asOverflow )
				mSoundPool.menuButtonClick() ;
	    	return true ;
		}
		
		// for now, no other support
		//Log.d(TAG, "mmtbbs_onButtonClick bracket OUT") ;
		return false ;
	}
	
	/**
	 * The user has long-clicked a button on this strip.  If buttonNum == 0,
	 * be assured that it is the main button.  If > 0, it is some other button,
	 * and as mentioned in the class description, the content and effect are
	 * completely up to the outside world to determine.  If you need to check
	 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
	 * 
	 * The menuItemNumber provided is the currently displayed menu item
	 * according to the strip's records.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param menuItemNumber
	 */
	public boolean mmtbbs_onButtonLongClick(
			MainMenuTitleBarButtonStrip strip,
			int buttonNum, int menuItemNumber ) {
		
		// The main button has navigation effects.
		switch( buttonNum ) {
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_MAIN:
			return false ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_QUANTRO:
			// QUICK-PLAY!
			quickStartGameSetupActivity() ;
			if ( mButtonSounds )
				mSoundPool.menuButtonHold() ;
			return true ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_HELP:
			// TODO: long-press the premium button.
			return false ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_SETTINGS:
			// TODO: long-press the premium button.
			return false ;
			
		
		}
		
		//Log.d(TAG, "mmtbbs_onButtonLongClick bracket OUT") ;
		return false ;
	}
	
	/**
	 * The user has long-clicked a button on this strip.  If buttonNum == 0,
	 * be assured that it is the main button.  If > 0, it is some other button,
	 * and as mentioned in the class description, the content and effect are
	 * completely up to the outside world to determine.  If you need to check
	 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
	 * 
	 * The menuItemNumber provided is the currently displayed menu item
	 * according to the strip's records.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param menuItemNumber
	 */
	public boolean mmtbbs_supportsLongClick(
			MainMenuTitleBarButtonStrip strip,
			int buttonNum, int menuItemNumber ) {
		
		// The main button has navigation effects.
		switch( buttonNum ) {
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_MAIN:
			return false ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_QUANTRO:
			return true ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_HELP:
			return false ;
			
		case MainMenuTitleBarButtonStrip.BUTTON_TYPE_SETTINGS:
			return false ;
		}
		
		//Log.d(TAG, "mmtbbs_onButtonLongClick bracket OUT") ;
		return false ;
	}
	
	
	@Override
	public void mmtbbs_onPopupOpened( MainMenuTitleBarButtonStrip strip ) {
		if ( mButtonSounds )
			mSoundPool.menuButtonClick() ;
	}
	
	
	
	private class RefreshAlertWifiMultiplayerAsyncTask extends AsyncTask<Object, Object, Integer> {
		
		Object [] mExecuteOnUiThreadParams ;
		public void executeOnUiThread( Object ... params ) {
			mExecuteOnUiThreadParams = params ;
			MainMenuActivity.this.runOnUiThread(new Runnable() {
				public void run() {
					RefreshAlertWifiMultiplayerAsyncTask.this.execute( mExecuteOnUiThreadParams ) ;
				}
			}) ;
		}
		
		@Override
		protected Integer doInBackground(Object... params) {
			//Log.d(TAG, "RefreshAlertWifiMultiplayerAsyncTask.doInBackground bracket IN") ;
			Hashtable<String, WiFiLobbyDetails> lobbies = mLobbyFinder.getLobbies() ;
			
			boolean minorAlert = false ;
			boolean majorAlert = false ;
			
			Iterator<Entry<String, WiFiLobbyDetails>> iter
					= lobbies.entrySet().iterator() ;
			for ( ; iter.hasNext() ; ) {
				WiFiLobbyDetails lobby = iter.next().getValue() ;
				minorAlert = true ;
				majorAlert = majorAlert || lobby.getReceivedStatus().getNumPeople() < lobby.getReceivedStatus().getMaxPeople() ;
			}
			
			//Log.d(TAG, "RefreshAlertWifiMultiplayerAsyncTask.doInBackground bracket OUT") ;
			
			if ( majorAlert )
				return MainMenuButtonStrip.ALERT_MAJOR ;
			else if ( minorAlert )
				return MainMenuButtonStrip.ALERT_MINOR ;
			return MainMenuButtonStrip.ALERT_NONE ;
		}
		
		
		@Override
		protected void onPostExecute (Integer result) {
			// two steps.  First, put the alert code in place.
			// Second, if there is an array adapter currently serving
			// the item number and the alert code changed, cause a refresh
			// of that view.
			//Log.d(TAG, "RefreshAlertWifiMultiplayerAsyncTask.onPostExecute bracket IN") ;
			
			// Technically, we do this twice; once for internet MP, once
			// for "multiplayer MP" which is the max of internet and wifi.
			if ( mMenuItemAlerts != null ) {
				synchronized ( mMenuItemAlerts ) {
					Integer [] alerts = new Integer[]{
							result, Math.max(result, mMenuItemAlerts[MENU_ITEM_INTERNET_MULTI_PLAYER])} ;
					Integer [] items = new Integer[] {
							MENU_ITEM_WIFI_MULTI_PLAYER, MENU_ITEM_MULTI_PLAYER } ;
					
					boolean refresh ;
					for ( int i = 0; i < alerts.length; i++ ) {
						Integer alert = alerts[i] ;
						Integer item = items[i] ;
						refresh = mMenuItemAlerts[item] != alert ;
						if ( refresh || true ) {
							//Log.d(TAG, "RefreshAlertWifiMultiplayerAsyncTask, refreshing " + item + " to " + alert) ;
							mMenuItemAlerts[item] = alert ;
							if ( mMenuHierarchyArrayAdapter != null ) {
								for ( int j = 0; j < mMenuHierarchyArrayAdapter.length; j++ )
									if ( mMenuHierarchyArrayAdapter[j] != null )
										((MenuItemArrayAdapter)mMenuHierarchyArrayAdapter[j]).refreshMenuItem(item) ;
							}
						}
					}
				}
			}
			
			//Log.d(TAG, "RefreshAlertWifiMultiplayerAsyncTask.onPostExecute bracket OUT") ;
		}
		
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
		return "help/main_menu_activity.html" ;
	}
	
	@Override
	public String getHelpDialogContextName() {
		return getResources().getString(R.string.global_dialog_help_name_main_menu) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
}
