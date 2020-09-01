package com.peaceray.quantro;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;

import com.peaceray.quantro.R;
import com.peaceray.quantro.adapter.controls.ControlsToActionsAdapter;
import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.main.GameOptionsMenuFragment;
import com.peaceray.quantro.main.GameService;
import com.peaceray.quantro.main.GameUserInterface;
import com.peaceray.quantro.main.ServicePassingBinder;
import com.peaceray.quantro.model.GameSaver;
import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameEvents;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.WifiMonitor;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.controls.InvisibleControls;
import com.peaceray.quantro.view.controls.InvisibleControls.SlideDelegate;
import com.peaceray.quantro.view.controls.InvisibleControlsGamepad;
import com.peaceray.quantro.view.controls.InvisibleControlsGamepadOfRows;
import com.peaceray.quantro.view.controls.InvisibleControlsGesture;
import com.peaceray.quantro.view.controls.InvisibleControlsGestureOfRows;
import com.peaceray.quantro.view.game.GameView;
import com.peaceray.quantro.view.game.GameView.ChangeListener;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.GameViewMemoryCapabilities;
import com.peaceray.quantro.view.game.PauseViewComponentAdapter;
import com.peaceray.quantro.view.game.StandardPauseViewComponentAdapter;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.dialog.ProgressDialog;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class GameActivity extends QuantroActivity
		implements ServiceConnection,
					GameUserInterface,
					ControlsToActionsAdapter.Delegate,
					WifiMonitor.Listener,
					GameView.Listener,
					View.OnClickListener, ChangeListener,
					GameOptionsMenuFragment.Listener, SlideDelegate {
	
	
	////////////////////////////////////////////////////////////////////////////
	// INTENT STRINGS
	public static final String INTENT_ACTION_QUIT_TO_LOBBY = "com.peaceray.quantro.GameActivity.INTENT_EXTRA_GAME_QUIT_TO_LOBBY" ;
	
	public static final String INTENT_EXTRA_GAME_INTENT_PACKAGE  = "com.peaceray.quantro.GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE" ;
	public static final String INTENT_EXTRA_GAME_RESULT			 = "com.peaceray.quantro.GameActivity.INTENT_EXTRA_GAME_RESULT" ;
	
	public static final String INTENT_EXTRA_SAVE_THUMBNAIL = "com.peaceray.quantro.GameActivity.INTENT_EXTRA_SAVE_THUMBNAIL" ;
	
	public static final int RESULT_CODE_FINISHED 	= 1000100 ;
	
	private static final String TAG = "GameActivity" ;
	
	///////////////////////////////////////////////////////////////////////////
	//
	// DIALOGS
	// 
	// Some important metagame events (connecting to server, trying to quit,
	// etc.) are important enough for a dialog to be displayed to the user.
	// Here we define some of them; i.e., we define their letter codes.
	//
	
	// For multiplayer, we need more than a few dialogs.  The most basic is
	// the 2-stage connection process, "connecting to server" and "talking
	// to server."  These dialogs indicate the connection process, using
	// a spinning pinwheel to show that we don't know how long they will take.
	
	public static final int DIALOG_CONNECTING_TO_SERVER_ID	= 0 ;
	public static final int DIALOG_TALKING_TO_SERVER_ID		= 1 ;
	
	// Next, we have the "waiting for" and "paused by" dialogs.  These
	// are also progress dialogs, but unlike the above, they are not generic.
	// They must indicate the player(s) being waited on or paused by.
	// Therefore, unlike the two above, these will have special handling
	// in "onPrepareDialog."
	
	public static final int DIALOG_WAITING_FOR_PLAYERS_ID	= 2 ;
	public static final int DIALOG_PAUSED_BY_PLAYERS_ID		= 3 ;
	
	// Sometimes we want to warn the player when the hit the "back" button
	// or in some other way attempt to quit.  Specifically, we do this
	// in multiplayer games, and in single player games if 'warn before quit'
	// is activated.  A dialog is not, strictly speaking, required in SP games,
	// because we save automatically, but watching Terry play it looks like
	// accidental quits are pretty common.  Since I can't reproduce the
	// "force quit" bug people complain about, maybe this is a good stopgap?
	
	public static final int DIALOG_QUIT_SINGLE_PLAYER_ID	= 4 ;
	public static final int DIALOG_QUIT_MULTIPLAYER_ID		= 5 ;
	
	// What if a player goes ahead and quits anyway?  Then his opponent(s)
	// get a dialog informing them of this.  There's not much to
	// do but read it and then dismiss.
	
	public static final int DIALOG_OPPONENT_QUIT_MULTIPLAYER_ID	= 6 ;
	
	// MATCHMAKING: Requesting, and receiving, a matchmaking connection.
	public static final int DIALOG_MATCHMAKING_NO_RESPONSE_ID = 12 ;
	public static final int DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID = 13 ;
	public static final int DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID = 14 ;
	
	public static final int DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID = 15 ;
	public static final int DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID = 16 ;
	public static final int DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID = 17 ;
	
	// WIFI: Only relevant for WiFi lobbies.  Warns the user that they do not
	// seem to have a WiFi connection.
	public static final int DIALOG_NO_WIFI_CONNECTION = 18 ;
	
	// WEB CGI_BIN: Communication with the cgi script regarding internet games.
	public static final int DIALOG_GAME_WEB_COMMUNICATION_FAILURE = 19;
	public static final int DIALOG_GAME_WEB_CLOSED = 20;
	
	
	// Dialog tiers
	public static final int DIALOG_TIER_NETWORK_STATUS = 0 ;
	public static final int DIALOG_TIER_MATCHMAKING_PROBLEMS = 1 ;
	public static final int DIALOG_TIER_WIFI_PROBLEMS = 2 ;			// INCOMPATIBLE WITH MEDIATOR PROBLEMS
	public static final int DIALOG_TIER_INTERACTIVE = 3 ;
	
	// Dialogs grouped by tier.
	public static final int [] DIALOGS_NETWORK_STATUS
			= new int[] {DIALOG_CONNECTING_TO_SERVER_ID,
						 DIALOG_TALKING_TO_SERVER_ID,
						 DIALOG_WAITING_FOR_PLAYERS_ID,
						 DIALOG_PAUSED_BY_PLAYERS_ID} ;

	
	public static final int [] DIALOGS_MATCHMAKING_PROBLEMS
	= new int[] { DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID,
				  DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID,
				  DIALOG_MATCHMAKING_NO_RESPONSE_ID,
				  DIALOG_GAME_WEB_COMMUNICATION_FAILURE } ;
	
	public static final int [] DIALOGS_WIFI_PROBLEMS
	= new int[] {DIALOG_NO_WIFI_CONNECTION} ;
	
	public static final int [] DIALOGS_INTERACTIVE
			= new int[] {DIALOG_QUIT_SINGLE_PLAYER_ID,
						 DIALOG_QUIT_MULTIPLAYER_ID,
						 DIALOG_OPPONENT_QUIT_MULTIPLAYER_ID,
						 DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID,
						 DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID,
						 DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID,
						 DIALOG_GAME_WEB_CLOSED } ;
	
	
	
	/**
	 * Number of milliseconds between a thumbnail update and a game save
	 * during which we will manually save the thumbnail.
	 */
	private static final long SAVED_GAME_THUMBNAIL_LEEWAY = 3000 ;
	
	
	
	// How do we display these?
	DialogManager dialogManager ;
	
	// A GameActivity needs to know the associated service.
	GameService gameService = null ;
	
	GameResult mostRecentGameResult = null ;
	
	// A GameActivity needs to keep track of a number of
	// structures, independent of the thread-based operation
	// of those structures.  This is the model.
	GameInformation [] ginfo ;
	GameEvents [] gevents ;
	Game [] game ;
	
	int numberOfPlayers ;
	int playerSlot = 0 ;
	int opponentSlot = 0 ;
	int playerSlotLastGUIUpdate = -1 ;
	String [] playerName = null ;
	
	// Here's some view information.
	GameView gameView ;
	InvisibleControlsGamepad controlsGamepad ;
	InvisibleControlsGesture controlsGesture ;
	QuantroSoundPool soundPool ;
	PauseViewComponentAdapter pauseViewComponentAdapter ;
	View pauseOverlay ;
	View greyOverlay ;		// display this whenever the pause view, OR game options menu, is visible.
	GameOptionsMenuFragment gameOptionsMenuFragment ;
	// We may need Runnables to change pause view visibility.
	Runnable pauseViewComponentAdapterSetVisibilityVisibleRunnable ;
	Runnable pauseViewComponentAdapterSetVisibilityGoneRunnable ;
	boolean pauseViewShown ;
	Runnable gameOptionsMenuFragmentSetVisibilityVisibleRunnable ;
	Runnable gameOptionsMenuFragmentSetVisibilityGoneRunnable ;
	boolean gameOptionsViewShown = false ;
	boolean gameOptionsFragmentUsed = false ;
	
	// here's a place for data we might need to send to the gameOptionsMenu.
	Drawable controlsThumbnail ;

	// here's a place for saving saved game thumbnails.
	Bitmap thumbnail ;
	long timeLastThumbnailUpdate = 0 ;
	long timeLastSaved = 0 ;
	
	ColorScheme mColorScheme ;
	Skin mSkin ;
	Background mBackground ;
	Collection<Background> mBackgroundsInShuffle ;
	boolean mBackgroundShuffles ;
	Music mMusic ;
	
	// Some useful storage
	Resources res ;
	
	// Information from our intent
	GameIntentPackage gip ;
	// Here's a place to keep a reference to a saved instance state.
	// If this is not 'null', we apply it to the Service immediately upon
	// the service announcing itself.
	Bundle savedInstanceState ;
	
	// Reference to ourself!
	// Used as a hack to test Connection-based games, before moving some
	// of the functionality into a Service.
	// Eventually, wherever you see gameUserInterface.<>(), it will be
	// the Service making the call, and this the callee.
	GameUserInterface gameUserInterface = this ;
	
	
	// Controlling a Game object: the pieces that interact with each
	// other to allow a game object to proceed.
	ControlsToActionsAdapter controlsToActionsAdapter ;
	
	
	// As the game progresses, we may get updates about certain player(s).
	boolean [] wasWaitingForPlayers = null ;
	boolean [] wasPausedByPlayers = null ;
	int wasLeftByPlayer ;
	boolean gameHasTicked = false ;
	boolean gameIsGoing = false ;
	
	
	// For checking whether the app is displayed.
	boolean activityShown ;
	
	// For restarting the gameView.  We pause gameView
	// in onPause, but break connections in onStop; if
	// we call onPause and then onResume, without losing
	// a connection, we will stay paused unless we specifically
	// account for this possibility.
	boolean gameViewLastPauseWasOnPause ;
	boolean pausedButNotStopped ;
	boolean mExaminingResult = false ;		// if we are currently examining a game result.
	
	boolean mAcquirePremiumForSet = false ;
	boolean mAcquirePremiumForShuffle = false ;
	
	
	// A note on WiFi connectivity.  For mediated games, we put up messages
	// when we have trouble connecting to the mediator.  That should be sufficient
	// for them.  For WiFi games, though, it isn't enough that we have a network
	// connection and can query arbitrary sites - we must be on WIFI or things are
	// broken.
	// The Service doesn't need to know this.  It uses "Connections" which are either
	// connected or not, irrespective of antennas in-use.  This is purely feedback
	// for the player.
	Handler mHandler ;
	WifiMonitor mWifiMonitor ;		// will be null for non-WiFi games.
	boolean mNoWifiDialogDisplayed = false ;
	private static final int WIFI_MONITOR_UPDATE_FREQUENCY = 5000 ;		// every 5 seconds
	int mHostIP ;
	GameViewMemoryCapabilities mGameViewMemoryCapabilities ;
	
	
	
	// ACTIVITY STATE:
	// To interact with GameService, we must send it a complete update as
	// to our current state (started, resumed, paused, etc.) immediately
	// upon binding.  These state variables serve one purpose - allowing
	// us to perform that update.
	// NOTE: THE VALUE OF THESE VARIABLES IS IMPORTANT!  WE USE THE FACT
	// THAT THEY ARE VALUED IN ASCENDING ORDER.
	int activityState = -1 ;
	public static final int ACTIVITY_STATE_CREATED = 0 ;
	public static final int ACTIVITY_STATE_STARTED = 1 ;
	public static final int ACTIVITY_STATE_RESUMED = 2 ;
	public static final int ACTIVITY_STATE_PAUSED = 3 ;
	public static final int ACTIVITY_STATE_STOPPED = 4 ;
	public static final int ACTIVITY_STATE_DESTROYED = 5 ;
	
	
	// GAME END TRANSITIONS:
	// Certain in-game events halt the game until the user makes some
	// specific choice.  For example, they have lost a single-player
	// game, and should either restart or quit.  Or they have completed
	// an SP stage and can continue, replay, or quit.  Or a multiplayer
	// game is over.  In whatever case, we want to launch (for result)
	// an Activity whose only job is to collect this choice from the
	// user (while also informing the user of the result, etc.)
	// However, we don't do this as an IMMEDIATE response to particular
	// events; there is a transition towards this display that
	// works in two steps.
	int mResult ;
	boolean mResultNeedsDisplay ;	// because the game could potentially end
									// when the Activity is not in front, we set
									// this flag to indicate the result should be
									// shown immediately upon onResume.
	boolean mResolvingResult ;		// indicates the particular state
									// in which we are moving TOWARDS
									// showing a GameResult.  Will be
									// false when we display one.
	
	
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent) ;
		
		if ( intent.getAction().equals(INTENT_ACTION_QUIT_TO_LOBBY) ) {
			Log.d(TAG, "onNewIntent: is INTENT_ACTION_QUIT_TO_LOBBY") ;
			dialogManager.showDialog(DIALOG_QUIT_MULTIPLAYER_ID) ;
			return ;
		}
		
		// this might be a "return to previous" intent, when coming back from the background.
		// it also might be an ENTIRELY NEW game launch with a different GIP.
		GameIntentPackage newGip = (GameIntentPackage)intent.getSerializableExtra( INTENT_EXTRA_GAME_INTENT_PACKAGE ) ;
		
		if ( newGip != null ) {
			// replace the old GIP?
			if ( gip == null || !gip.nonce.equals(newGip.nonce) ) {
				// bind to the service (if unbound); otherwise set
				// self.
				if ( gameService == null )
					bindToService() ;
				else
					setUpGameService() ;
			}
			// otherwise we received a new Intent (for some reason) but it
			// represents the same data.
		}
	}
	
	
    /** Called when the activity is first created. */
    @Override
    synchronized public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupQuantroActivity( QUANTRO_ACTIVITY_GAME, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
		
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		
        // Force portrait layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;
        
        System.gc() ;
		
		this.savedInstanceState = savedInstanceState ;
		
		Intent intent = getIntent() ;
		if ( intent.getAction().equals(INTENT_ACTION_QUIT_TO_LOBBY) ) {
			Log.d(TAG, "onCreate: is INTENT_ACTION_QUIT_TO_LOBBY") ;
			finish() ;
			return ;
		}
		
		if ( intent.getBooleanExtra(INTENT_EXTRA_SAVE_THUMBNAIL, true) ) {
			int thumbnailSize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					(float)140, getResources().getDisplayMetrics()) ;
			thumbnail = Bitmap.createBitmap(thumbnailSize, thumbnailSize, Bitmap.Config.ARGB_8888) ;
		} else {
			thumbnail = null ;
		}
		
		// We need a dialog manager.  Network status is tier 0, interactive is tier 1.
		dialogManager = new DialogManager( this, new int[][] {
				DIALOGS_NETWORK_STATUS, DIALOGS_WIFI_PROBLEMS, DIALOGS_INTERACTIVE} ) ;
		
		activityShown = false ;
		
		gameViewLastPauseWasOnPause = false ;
		pausedButNotStopped = false ;
		
		mResolvingResult = false ;
        
        Log.d(TAG, "Intent has action " + getIntent().getAction() + " compare against " + Intent.ACTION_MAIN) ;
        Log.d(TAG, "savedInstanceState is " + savedInstanceState) ;
        
        // Set resources for GameModes.
        res = getResources() ;
        
        // Get game mode, save_to, load_from.
        gip = (GameIntentPackage)getIntent().getSerializableExtra( INTENT_EXTRA_GAME_INTENT_PACKAGE ) ;
        int gameMode = gip.gameSettings.getMode() ;
        
		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		mSkin = GameModes.numberQPanes(gameMode) == 1
				? QuantroPreferences.getSkinRetro(this)
				: QuantroPreferences.getSkinQuantro(this) ;
        
        Log.d(TAG, "have game mode: " + gameMode + ", saveTo: " + gip.saveToKey + ", loadFrom " + gip.loadFromKey ) ;
        
        numberOfPlayers = gip.gameSettings.getPlayers() ;
    	playerName = new String[numberOfPlayers] ;
    	for ( int i = 0; i < numberOfPlayers; i++ ) {
    		if ( gip.playerNames != null ) {
    			playerName[i] = gip.playerNames[i] ;
    		}
    	}
    	
    	
    	// LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
    	setContentView(R.layout.game_layout);
        
        // Set up the GameOptionsLayout: if we're including it.
        if ( ((QuantroApplication)getApplication()).getGameViewMemoryCapabilities(this).getGameOverlaySupported() ) {
        	gameOptionsFragmentUsed = true ;
        	// Only load the fragment if we are not restoring from a previous state.
            if (savedInstanceState == null) {
                Fragment fragment = new GameOptionsMenuFragment() ;
                
                // Add the fragment to the container FrameLayout
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.game_options_menu_fragment_container, fragment).commit();
            }
        }
        
        // Set up the game view.
        opponentSlot = (playerSlot + 1) % numberOfPlayers ;
        gameView = (GameView) findViewById(R.id.game_view) ;
        gameView.setNumberOfPlayers(numberOfPlayers) ;
        for ( int i = 0; i < numberOfPlayers; i++ ) {
        	gameView.setPlayerName(i, playerName[i]) ;
        	gameView.setPlayerStatus(i, false, false, false, false, false) ;
        }
        gameView.setDisplayedGames(playerSlot, opponentSlot) ;
        gameView.setFPSView((TextView)findViewById(R.id.view_fps_text)) ;
        gameView.setSoundPreferences( QuantroPreferences.getSoundControls(this) ) ;
        int qpPieceTips = QuantroPreferences.getPieceTips(this) ;
        int gvPieceTips ;
        switch( qpPieceTips ) {
        case QuantroPreferences.PIECE_TIPS_NEVER: gvPieceTips = GameView.PIECE_TIPS_NEVER ; break ;
        case QuantroPreferences.PIECE_TIPS_OFTEN: gvPieceTips = GameView.PIECE_TIPS_OFTEN ; break ;
        default: gvPieceTips = GameView.PIECE_TIPS_OCCASIONALLY ; break ;
        }
        gameView.setGamePreferences(gvPieceTips) ;
        
        
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        
        // Set up our controls.
    	// First allocate adapters, controls...
        Log.d(TAG, "finding and setting up controls") ;
        controlsGamepad = (InvisibleControlsGamepad) findViewById(R.id.controls_gamepad_of_rows) ;
        controlsGesture = (InvisibleControlsGesture) findViewById(R.id.controls_gesture) ;
        controlsGamepad.setPixels(
        		metrics.widthPixels, metrics.heightPixels,
        		metrics.xdpi, metrics.ydpi ) ;
        controlsGamepad.prepare() ;
        controlsGesture.setPixels(
        		metrics.widthPixels, metrics.heightPixels,
        		metrics.xdpi, metrics.ydpi ) ;
        controlsGesture.prepare() ;
        
        // Set up controls to actions adapter.  Don't link yet; we do this
        // upon prompting by the Service.
        controlsToActionsAdapter = new ControlsToActionsAdapter() ;
        controlsToActionsAdapter.loadResources( getResources() ) ;
        controlsToActionsAdapter.setDelegate(this) ;
        
        // Set up the sound pool
        soundPool = prepareQuantroSoundPool() ;
        soundPool.stopMusic() ;
        // Hand to GameView.
        gameView.setQuantroSoundPool( soundPool ) ;
        // Get default music, load, and pass to GameOptions
        // (if we have a reference to it).
        if ( mMusic == null )
        	mMusic = getDefaultMusic() ;
    	soundPool.loadMusic(this, mMusic) ;
    	if ( gameOptionsMenuFragment != null )
    		gameOptionsMenuFragment.setCurrentMusic(mMusic) ;
        
        
        // Create the pause view adapter and link its components up.
        greyOverlay = findViewById(R.id.game_layout_grey_overlay) ;
        pauseOverlay = findViewById(R.id.pause_overlay) ;
        pauseViewComponentAdapter = preparePauseViewComponentAdapter( pauseOverlay ) ;
        pauseViewComponentAdapter.setGame(gip.gameSettings) ;
        pauseViewComponentAdapter.setReserveIsSpecial( GameModes.reserveBehavior( gip.gameSettings.getMode() ) == GameModes.RESERVE_BEHAVIOR_INSERT ) ;
        pauseViewComponentAdapter.setPlayerNames(gip.playerNames) ;
        pauseViewComponentAdapter.setIsHost(!gip.isClient()) ;
        // set ourself as the onClick listener to allow attempts to dismiss.
        if ( pauseOverlay != null )
        	pauseOverlay.setOnClickListener(this) ;
        pauseViewComponentAdapter.setVisibility(View.VISIBLE) ;
        pauseViewComponentAdapter.setSoundOn( !soundPool.isMuted(), soundPool.isMutedByRinger() ) ;
        pauseViewComponentAdapter.setSoundVolumePercent(QuantroPreferences.getVolumeSoundPercent(this)) ;
        pauseViewComponentAdapter.setMusicVolumePercent(QuantroPreferences.getVolumeMusicPercent(this)) ;
        pauseViewComponentAdapter.setControlsGamepad(
        		QuantroPreferences.getControls(this) == QuantroPreferences.CONTROLS_GAMEPAD,
        		QuantroPreferences.getControlsGamepadDropButton(this) == QuantroPreferences.CONTROLS_DROP_FALL,
        		QuantroPreferences.getControlsGamepadDoubleDownDrop(this) ) ;
        pauseViewComponentAdapter.setControlsSupportsQuickSlide(
        		QuantroPreferences.getControls(this) == QuantroPreferences.CONTROLS_GAMEPAD
        			? QuantroPreferences.getControlsGamepadQuickSlide(GameActivity.this)
        			: QuantroPreferences.getControlsGestureQuickSlide(GameActivity.this)) ;
        pauseViewComponentAdapter.setControlsHas(
				GameModes.hasRotation(gameMode),
				GameModes.hasReflection(gameMode)) ;
        if ( gip.isLocal() )
        	pauseViewComponentAdapter.setStartLoading() ;	
        else
        	pauseViewComponentAdapter.setStateConnecting() ;
        		// HACK: This last line, while unnecessary and confusing, forces the
        		// Pause Overlay to display a "LOADING" message even before GameView 
        		// has started loading information.  GameView will make a redundant call
        		// when it starts loading, and then setStopLoading upon completion.
        pauseViewComponentAdapterSetVisibilityVisibleRunnable = new Runnable() {
			public void run() {
				if ( !gameOptionsViewShown ) {
					pauseViewComponentAdapter.setVisibility(View.VISIBLE) ;
					greyOverlay.setVisibility(View.VISIBLE) ;
				}
				pauseViewShown = true ;
			} };
		pauseViewComponentAdapterSetVisibilityGoneRunnable = new Runnable() {
			public void run() {
				pauseViewComponentAdapter.setVisibility(View.GONE) ;
				pauseViewShown = false ;
				greyOverlay.setVisibility( (pauseViewShown || gameOptionsViewShown) ? View.VISIBLE : View.GONE ) ;
			} } ;
			
	        
        // Hand to GameView.
        gameView.setPauseViewComponentAdapter(pauseViewComponentAdapter) ;
			
        
        // GAME OPTIONS MENU?
		gameOptionsMenuFragmentSetVisibilityVisibleRunnable = new Runnable() {
			public void run() {
				if ( gameOptionsMenuFragment != null ) {
					gameOptionsMenuFragment.show() ;
				}
				gameOptionsViewShown = true ;
				greyOverlay.setVisibility(View.VISIBLE) ;
				if ( pauseViewShown ) {
					pauseViewComponentAdapter.setVisibility( View.GONE ) ;
				}
			} } ;
			
		gameOptionsMenuFragmentSetVisibilityGoneRunnable = new Runnable() {
			public void run() {
				if ( gameOptionsMenuFragment != null ) {
					gameOptionsMenuFragment.dismiss() ;
				}
				gameOptionsViewShown = false ;
				if ( pauseViewShown ) {
					greyOverlay.setVisibility( View.VISIBLE ) ;
					pauseViewComponentAdapter.setVisibility( View.VISIBLE ) ;
				} else {
					greyOverlay.setVisibility( View.GONE ) ;
				}
			} } ;

        
        
        // NOTE: At the moment, gameView, controls, and controlsToActionsAdapter
        // are completely separate from any other game elements (such as Game
        // objects).  In fact, we don't HAVE any Game objects.  The Service takes
        // care of this for us.  Once we bind, we should establish these.

        
    	mHandler = new Handler() ;
    	mWifiMonitor = null ;
        // We should monitor Wifi status, if indeed this is a Wifi game.
        if ( !gip.isMediatedChallenge() && !gip.isMatchseeker() && !gip.isLocal() ) {
        	Log.d(TAG, "establishing mWifiMonitor, since is not mediated and is not local") ;
        	mWifiMonitor = new WifiMonitor(this, this) ;
        	mHostIP = WifiMonitor.ipAddressFromString(((InetSocketAddress)gip.socketAddress).getAddress().getHostAddress()) ;
        }
        
        activityState = ACTIVITY_STATE_CREATED ;
        
        // If the menu fragment attaches before this (I can't image how it would...)
        // here is where we perform the setup.
        if ( gameOptionsMenuFragment != null ) {
        	gomfl_onAttach( gameOptionsMenuFragment ) ;
        }
        
        bindToService() ;
        
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
		
		// Reload necessary structures
		if ( savedInstanceState != null ) {		// antismell brackets
			readStateFromBundle(savedInstanceState) ;
		}
    }
    
    
    
    private void bindToService() {
        ///////////////////////////////////////////////
        // BIND TO THE SERVICE
        // If we are in a multiplayer game, we bind here - the service should be running at all times,
        // even when we are stopped and the activity is not in front.
        Intent serviceIntent = new Intent( this, GameService.class ) ;
        serviceIntent.putExtra( INTENT_EXTRA_GAME_INTENT_PACKAGE, gip) ;
    	
        bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    }
    
    private QuantroSoundPool prepareQuantroSoundPool() {
    	setVolumeControlStream(AudioManager.STREAM_MUSIC) ;
    	// Load music.  Load based on game mode.
    	QuantroSoundPool qsp = ((QuantroApplication)getApplication()).getSoundPool(this) ;
    	// Mute according to shared settings.
    	if ( QuantroPreferences.getMuted(this) )
    		qsp.mute() ;
    	else
    		qsp.unmute() ;
    	
    	return qsp ;
    }
    
    private Music getDefaultMusic() {
    	Music music = Music.getDefaultTrackForGameMode(gip.gameSettings.getMode()) ;
    	// TODO: Alter this to restrict music to those owned by the user,
    	// and obeying an "default music" settings.
    	
    	return music ;
    }
    
    
    private PauseViewComponentAdapter preparePauseViewComponentAdapter( View pauseOverlay ) {
    	PauseViewComponentAdapter adapter = new StandardPauseViewComponentAdapter(this) ;
        if ( pauseOverlay != null ) {
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_LAYOUT,
        			pauseOverlay) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_GAME_MODE,
        			pauseOverlay.findViewById(R.id.pause_overlay_game_mode)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_DESCRIPTION_FALLING,
        			pauseOverlay.findViewById(R.id.pause_overlay_falling_description)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_DESCRIPTION_NEXT,
        			pauseOverlay.findViewById(R.id.pause_overlay_next_description)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_DESCRIPTION_RESERVE,
        			pauseOverlay.findViewById(R.id.pause_overlay_reserve_description)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_STATE,
        			pauseOverlay.findViewById(R.id.pause_overlay_state)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_STATE_DESCRIPTION,
        			pauseOverlay.findViewById(R.id.pause_overlay_state_description)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_DESCRIPTION_CONTROLS,
        			pauseOverlay.findViewById(R.id.pause_overlay_controls_description)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_DESCRIPTION_EXPANDED_CONTROLS,
        			pauseOverlay.findViewById(R.id.pause_overlay_controls_description_expanded)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_LOGO,
        			pauseOverlay.findViewById(R.id.pause_overlay_image_logo)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_THUMBNAIL_CONTROLS,
        			pauseOverlay.findViewById(R.id.pause_overlay_image_controls)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_DRAW_DETAIL,
        			pauseOverlay.findViewById(R.id.pause_overlay_graphics_description)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_COLOR_SCHEME,
        			pauseOverlay.findViewById(R.id.pause_overlay_colors_description)) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_SOUND,
        			pauseOverlay.findViewById(R.id.pause_overlay_sound_description) ) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_MUSIC,
        			pauseOverlay.findViewById(R.id.pause_overlay_music_description) ) ;
        	adapter.setComponent(
        			PauseViewComponentAdapter.COMPONENT_MUTE_ALERT,
        			pauseOverlay.findViewById(R.id.pause_overlay_mute_alert) ) ;
        }
        
        return adapter ;
    }
    
    
    @Override
    protected void onStart() {
    	super.onStart();
    	activityState = ACTIVITY_STATE_STARTED ;
    	Log.d(TAG, "onStart") ;
    	
        activityShown = true ;
        
        // Maybe we changed preferences?  Tell the GameView our
    	// new settings.  Note that we assume GameView handles these calls
    	// very smoothly and efficiently in the case that preferences
    	// did not actually change.
    	mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
    	mSkin = GameModes.numberQPanes(gip.gameSettings.getMode()) == 1
				? QuantroPreferences.getSkinRetro(this)
				: QuantroPreferences.getSkinQuantro(this) ;
		mBackground = QuantroPreferences.getBackgroundCurrent(this) ;
		mBackgroundsInShuffle = QuantroPreferences.getBackgroundsInShuffle(this) ;
		mBackgroundShuffles = QuantroPreferences.getBackgroundShuffles(this) ;
		
		if ( mMusic == null )
        	mMusic = getDefaultMusic() ;
    	soundPool.loadMusic(this, mMusic) ;
    	if ( gameOptionsMenuFragment != null )
    		gameOptionsMenuFragment.setCurrentMusic(mMusic) ;
		
		// Inform the fragment
        if ( gameOptionsMenuFragment != null )
        	gameOptionsMenuFragment.setCurrentBackground( mBackground ) ;
    	
    	// Inform the Service.
    	if ( gameService != null )
    		gameService.gameActivityDidStart(this) ;
    }
    
    
    
    @Override
    synchronized protected void onResume() {
    	Log.d(TAG, "onResume") ;
    	super.onResume();
    	getWindow().setBackgroundDrawable(null) ;
    	
		activityState = ACTIVITY_STATE_RESUMED ;
    	
        // Reveal our dialogs, in case they were previously hidden.
        dialogManager.revealDialogs() ;
    	
    	// RESUME: we DIM_LOCK whenever the game is in front.
    	// We don't want the screen to ever turn off when the game
    	// is going.  The GameService has its own criteria; we
    	// are only concerned with 'resumed' games, so we release this
    	// lock immediately upon onPause (such as when the user 
    	// navigates away, or presses the "Lock Screen" button
    	getDimLock(this).acquire() ;
    	
    	// Special handling: if we are "resolving result,"
    	// and that result needs a display, do it now.
    	if ( mResultNeedsDisplay ) {
    		examineResult( mResult ) ;
    		mResultNeedsDisplay = false ;
    		mResolvingResult = false ;
    	}
    	
    	// This sets up a crash later, as the thread doesn't get replaced.
    	// gameView.setControls(null) ;
    	
    	int drawDetail = QuantroPreferences.getGraphicsGraphicalDetail(this) ;
    	if ( mGameViewMemoryCapabilities == null )
    		mGameViewMemoryCapabilities = ((QuantroApplication)getApplicationContext()).getGameViewMemoryCapabilities(this) ;
		int scaleAdditive = QuantroPreferences.getGraphicsScaleAdditive(this);
    	
    	ArrayList<Background> justCurrentBackground = new ArrayList<Background>() ;
    	justCurrentBackground.add(mBackground) ;
    	
		for ( int i = 0; i < numberOfPlayers; i++ ) {
        	gameView.setPlayerName(i, playerName[i]) ;
        	gameView.setPlayerStatus(i,
        			gameService == null ? false : gameService.getPlayerWon(i),
        			gameService == null ? false : gameService.getPlayerLost(i),
        			gameService == null ? false : gameService.getPlayerKicked(i),
        			gameService == null ? false : gameService.getPlayerQuit(i),
        			gameService == null ? false : gameService.getPlayerSpectating(i)) ;
        }
    	gameView.setAllGraphics(
        		drawDetail,
        		QuantroPreferences.getGraphicsPiecePreviewMinimumProfile(this),
        		mGameViewMemoryCapabilities.getLoadImagesSize(  ),
        		mGameViewMemoryCapabilities.getBackgroundImageSize(  ),
        		mGameViewMemoryCapabilities.getBlit(  ),
        		mGameViewMemoryCapabilities.getScale(  ) + scaleAdditive,
        		mGameViewMemoryCapabilities.getRecycleToVeil(  ),
        		QuantroPreferences.getGraphicsSkipAnimations(this)
        				? DrawSettings.DRAW_ANIMATIONS_STABLE_STUTTER
        				: DrawSettings.DRAW_ANIMATIONS_ALL,
        		mSkin, mColorScheme,
        		mBackground, mBackgroundShuffles ? mBackgroundsInShuffle : justCurrentBackground,
				mBackgroundShuffles,
    			res.getInteger(R.integer.background_shuffle_min_gap),
    			res.getInteger(R.integer.background_shuffle_max_gap),
    			true ) ;
    	
        gameView.setSoundPreferences( QuantroPreferences.getSoundControls(this) ) ;
        int qpPieceTips = QuantroPreferences.getPieceTips(this) ;
        int gvPieceTips ;
        switch( qpPieceTips ) {
        case QuantroPreferences.PIECE_TIPS_NEVER: gvPieceTips = GameView.PIECE_TIPS_NEVER ; break ;
        case QuantroPreferences.PIECE_TIPS_OFTEN: gvPieceTips = GameView.PIECE_TIPS_OFTEN ; break ;
        default: gvPieceTips = GameView.PIECE_TIPS_OCCASIONALLY ; break ;
        }
        gameView.setGamePreferences(gvPieceTips) ;
        
        
        // Refresh pause overlay based on new prefs.
        pauseViewComponentAdapter.setDrawDetail(drawDetail) ;
        pauseViewComponentAdapter.setColorSchemeName( mSkin.getName() ) ;
        pauseViewComponentAdapter.setSoundVolumePercent(QuantroPreferences.getVolumeSoundPercent(this)) ;
        pauseViewComponentAdapter.setMusicVolumePercent(QuantroPreferences.getVolumeMusicPercent(this)) ;
        pauseViewComponentAdapter.setSoundOn( !soundPool.isMuted(), soundPool.isMutedByRinger() ) ;
        pauseViewComponentAdapter.setGameIsTrial(
        		false,
        		getPremiumLibrary().numTrialSecondsGameMode(gip.gameSettings.getMode()) ) ;
        
        // THUMBNAIL
        if ( gip.saveToKey != null )
        	gameView.setGameThumbnail(thumbnail) ;
        else
        	gameView.setGameThumbnail(null) ;
        
        gameView.setChangeListener(this) ;
        
        // CONTROLS
        if ( QuantroPreferences.getControls(this) == QuantroPreferences.CONTROLS_GAMEPAD )
        	setControlsGamepad() ;
        else
        	setControlsGesture() ;
        
        
    	
    	// Only unpause if we paused (and gameView stopped) 
    	if ( gameViewLastPauseWasOnPause && pausedButNotStopped )
    		gameView.unpause() ;
    	
    	if ( !mExaminingResult
    			&& (gameService != null && !(gameService.isPausedByPlayer() || gameService.isPausedByTimedDemoExpired()))
    			&& gameView.ready() )
    		soundPool.playMusic() ;
    	
    	// Start wifi monitor
    	if ( mHandler != null && mWifiMonitor != null ) {
    		mWifiMonitor.run() ;
    		mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_UPDATE_FREQUENCY) ;
    	}
    	
    	//gameView.unpause();
    	// Inform the Service.
    	if ( gameService != null )
    		gameService.gameActivityDidResume(this) ;
    }
    
    @Override
    synchronized protected void onPause() {
    	super.onPause();
    	activityState = ACTIVITY_STATE_PAUSED ;
    	Log.d(TAG, "onPause") ;
    	
    	// Hide any dialog currently displayed...
    	dialogManager.hideDialogs() ;
    	
    	// PAUSE: we DIM_LOCK whenever the game is in front.
    	// We don't want the screen to ever turn off when the game
    	// is going.  The GameService has its own criteria; we
    	// are only concerned with 'resumed' games, so we release this
    	// lock immediately now that we are pausing (e.g., if the
    	// user pressed the "LockScreen" button).
    	getDimLock(this).release() ;
    	
    	// stop all user controls
    	controlsToActionsAdapter.releaseAllButtons() ;
    	
    	gameViewLastPauseWasOnPause = !gameView.isPaused() ;
    	pausedButNotStopped = true ;
    	
    	gameView.pause();
    	Log.d(TAG, "gameView paused") ;
    	
    	// stop wifi monitor.
    	if ( mHandler != null && mWifiMonitor != null )
    		mHandler.removeCallbacks(mWifiMonitor) ;
    	Log.d(TAG, "wifi callbacks removed") ;
    	
    	// Should we save?
    	// Commented out to avoid current crash bug.
    	//if ( gaip.saveToKey != null )
    	//	this.saveGame(gaip.saveToKey) ;
    	
    	// Inform the Service.
    	if ( gameService != null ) {
    		gameService.gameActivityDidPause(this) ;
    		
    		// Previously we only put a 'user-pause' on the game
    		// if this was purely local, letting MP games resume
    		// immediately on user return.
    		//
    		// However, the new Game Options overlay takes quite a bit
    		// of time to recreate upon return to the Activity.  Therefore
    		// we now put a user pause in place regardless of whether this
    		// is a local game or not.
    		if ( !mExaminingResult /* && gip.isLocal() */  ) {
    			if ( pauseViewComponentAdapter != null )
    				pauseViewComponentAdapter.setLocalPause(true) ;
    			gameService.pauseByPlayer() ;
    		}
    	}
    	
    	if ( !mExaminingResult )
    		soundPool.pauseMusic() ;
    	
    	Log.d(TAG, "told service we paused") ;
    }
    
    @Override
    synchronized protected void onStop() {
    	super.onStop() ;
    	activityState = ACTIVITY_STATE_STOPPED ;
    	Log.d(TAG, "onStop") ;
    	
        activityShown = false ;
        
        pausedButNotStopped = false ;
    	

    	
    	// Inform the Service.
    	if ( gameService != null )
    		gameService.gameActivityDidStop(this) ;
    }
    
    @Override
    synchronized protected void onDestroy() {
    	super.onDestroy();
    	activityState = ACTIVITY_STATE_DESTROYED ;
    	Log.d(TAG, "onDestroy") ;
    	
    	// Unbind service (after telling them we've been destroyed)
    	if ( gameService != null )
    		gameService.gameActivityDidDestroy(this) ;
    	
    	// Unbind only when the Activity is destroyed.
    	if ( gameService != null ) {
    		unbindService(this) ;
    	}
    	
    	// Unlink some stuff.
    	if ( controlsToActionsAdapter != null ) {
    		controlsToActionsAdapter.recycle() ;
    		controlsToActionsAdapter = null ;
    	}
    	gameView.recycle(true) ;
    }
    
    
    @Override
    synchronized public void finish() {
    	Log.d(TAG, "finish") ;
    	if ( gameService != null || mostRecentGameResult != null ) {
    		if ( gameService != null )
    			mostRecentGameResult = gameService.gameActivityFinishing(this) ;
    		
    		// Make a game result Intent.
    		Intent i = new Intent() ;
    		i.putExtra(GameActivity.INTENT_EXTRA_GAME_RESULT, mostRecentGameResult) ;
    		this.setResult(RESULT_CODE_FINISHED, i) ;
    	}
    	
    	// make sure we always shut our sounds off and empty out our music.
    	if ( soundPool != null ) {
	    	soundPool.stopMusic() ;
	    	soundPool.unloadMusic() ;
    	}
    	
    	// Unbind only when the Activity is destroyed.
    	if ( gameService != null )
    		unbindService(this) ;
    	
    	gameService = null ;
    	
    	Log.d(TAG, "super.finish") ;
    	super.finish();
    }
    
    
    
    @Override
    synchronized protected void onRestoreInstanceState( Bundle savedInstanceState ) {
    	super.onRestoreInstanceState(savedInstanceState) ;
    	Log.d(TAG, "onRestoreInstanceState") ;
    	
    	// If we have a gameService, read the game from the Bundle.
    	// Otherwise, store the reference to the Bundle for later use
    	// (specifically, when the Service binds with us).
    	if ( gameService != null )
    		gameService.readStateFromBundle(savedInstanceState) ;
    	else {
    		this.savedInstanceState = savedInstanceState ;
    	}
    	// Reload necessary structures
    	readStateFromBundle(savedInstanceState) ;
    }
    
    @Override
    synchronized protected void onSaveInstanceState( Bundle outState ) {
    	super.onSaveInstanceState(outState) ;
    	Log.d(TAG, "onSaveInstanceState") ;
    	
    	if ( gameService != null )
    		gameService.writeStateToBundle(outState) ;
    	writeStateToBundle( outState ) ;
    }
    
    
    
    protected void readStateFromBundle( Bundle inState ) {
    	dialogManager = inState.getParcelable("GameActivity.dialogManager") ;
    	dialogManager.setActivity(this) ;
    	
    	numberOfPlayers = inState.getInt("GameActivity.numberOfPlayers") ;
    	playerSlot = inState.getInt("GameActivity.playerSlot") ;
    	playerName = inState.getStringArray("GameActivity.playerName") ;
    	
    	wasWaitingForPlayers = inState.getBooleanArray("GameActivity.wasWaitingForPlayers") ;
    	wasPausedByPlayers = inState.getBooleanArray("GameActivity.wasPausedByPlayers") ;
    	wasLeftByPlayer = inState.getInt("GameActivity.wasLeftByPlayer") ;
    	gameHasTicked = inState.getBoolean("GameActivity.gameHasTicked") ;
    	// As the game progresses, we may get updates about certain player(s).
    	
    	activityShown = inState.getBoolean("GameActivity.activityShown") ;
    	
    	gameViewLastPauseWasOnPause = inState.getBoolean("GameActivity.gameViewLastPauseWasOnPause") ;
    	pausedButNotStopped = inState.getBoolean("GameActivity.pausedButNotStopped") ;
    	mExaminingResult = inState.getBoolean("GameActivity.mExaminingResult") ;
    	
    	mNoWifiDialogDisplayed = inState.getBoolean("GameActivity.mNoWifiDialogDisplayed") ;
    	mHostIP = inState.getInt("GameActivity.mHostIP") ;
    	
    	mMusic = Music.fromStringEncoding( inState.getString("GameActivity.mMusic") ) ;
    	
    	// GAME END TRANSITIONS:
    	// Certain in-game events halt the game until the user makes some
    	// specific choice.  For example, they have lost a single-player
    	// game, and should either restart or quit.  Or they have completed
    	// an SP stage and can continue, replay, or quit.  Or a multiplayer
    	// game is over.  In whatever case, we want to launch (for result)
    	// an Activity whose only job is to collect this choice from the
    	// user (while also informing the user of the result, etc.)
    	// However, we don't do this as an IMMEDIATE response to particular
    	// events; there is a transition towards this display that
    	// works in two steps.
    	mResult = inState.getInt("GameActivity.mResult") ;
    	mResultNeedsDisplay = inState.getBoolean("GameActivity.mResultNeedsDisplay") ;
    	mResolvingResult = inState.getBoolean("GameActivity.mResolvingResult") ;
    }
    
    
    protected void writeStateToBundle( Bundle outState ) {
    	outState.putParcelable("GameActivity.dialogManager", dialogManager) ;
    	
    	outState.putInt("GameActivity.numberOfPlayers", numberOfPlayers) ;
    	outState.putInt("GameActivity.playerSlot", playerSlot) ;
    	outState.putStringArray("GameActivity.playerName", playerName) ;
    	
    	outState.putBooleanArray("GameActivity.wasWaitingForPlayers", wasWaitingForPlayers) ;
    	outState.putBooleanArray("GameActivity.wasPausedByPlayers", wasPausedByPlayers) ;
    	outState.putInt("GameActivity.wasLeftByPlayer", wasLeftByPlayer) ;
    	outState.putBoolean("GameActivity.gameHasTicked", gameHasTicked) ;
    	// As the game progresses, we may get updates about certain player(s).
    	
    	outState.putBoolean("GameActivity.activityShown", activityShown) ;
    	
    	outState.putBoolean("GameActivity.gameViewLastPauseWasOnPause", gameViewLastPauseWasOnPause) ;
    	outState.putBoolean("GameActivity.pausedButNotStopped", pausedButNotStopped) ;
    	outState.putBoolean("GameActivity.mExaminingResult", mExaminingResult) ;
    	
    	outState.putBoolean("GameActivity.mNoWifiDialogDisplayed", mNoWifiDialogDisplayed) ;
    	outState.putInt("GameActivity.mHostIP", mHostIP) ;
    	
    	outState.putString("GameActivity.mMusic", Music.toStringEncoding(mMusic)) ;
    	
    	// GAME END TRANSITIONS:
    	// Certain in-game events halt the game until the user makes some
    	// specific choice.  For example, they have lost a single-player
    	// game, and should either restart or quit.  Or they have completed
    	// an SP stage and can continue, replay, or quit.  Or a multiplayer
    	// game is over.  In whatever case, we want to launch (for result)
    	// an Activity whose only job is to collect this choice from the
    	// user (while also informing the user of the result, etc.)
    	// However, we don't do this as an IMMEDIATE response to particular
    	// events; there is a transition towards this display that
    	// works in two steps.
    	outState.putInt("GameActivity.mResult", mResult) ;
    	outState.putBoolean("GameActivity.mResultNeedsDisplay", mResultNeedsDisplay) ;
    	outState.putBoolean("GameActivity.mResolvingResult", mResolvingResult) ;
    }
    
    
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      // ignore orientation/keyboard change
      super.onConfigurationChanged(newConfig);
    }
    
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if ( super.handleActivityResult(requestCode, resultCode, data) )
    		return ;
    	
    	switch( requestCode ) {
    	case IntentForResult.REQUEST_EXAMINE_GAME_RESULT:
    		mExaminingResult = false ;
    		if (resultCode == RESULT_OK) {  
            	
            	if ( data.hasExtra(ExamineGameResultActivity.INTENT_RESULT_EXTRA) ) {
            		int action = data.getIntExtra(ExamineGameResultActivity.INTENT_RESULT_EXTRA, -1) ;
            		if ( action == ExamineGameResultActivity.INTENT_RESULT_CANCEL ) {
            			// quit.
            			quitGame( true ) ;
            			
            			if ( QuantroPreferences.getAnalyticsActive(this) )
            				Analytics.logInGameExamineResolvedToQuit(gip.gameSettings.getMode(),
            						mResult == GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER
            						|| mResult == GameUserInterface.RESULT_MULTI_PLAYER_GAME_OVER ) ;
            		}
            		else if ( action == ExamineGameResultActivity.INTENT_RESULT_CONTINUE ) {
            			// used in type-B, where we are given the option of "continuing"
            			// the game having completed a level.
            			gameService.resolvedToContinue() ;
            			
            			if ( QuantroPreferences.getAnalyticsActive(this) )
            				Analytics.logInGameExamineResolvedToContinue(gip.gameSettings.getMode(),
            						mResult == GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER
            						|| mResult == GameUserInterface.RESULT_MULTI_PLAYER_GAME_OVER ) ;
            		}
            		else if ( action == ExamineGameResultActivity.INTENT_RESULT_REPLAY ) {
            			// used in a few dialogs.  We basically restart from the checkpoint.
            			// Tell the Service.
            			gameService.resolvedToReplay() ;
            			
            			if ( QuantroPreferences.getAnalyticsActive(this) )
            				Analytics.logInGameExamineResolvedToReplay(gip.gameSettings.getMode(),
            						mResult == GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER
            						|| mResult == GameUserInterface.RESULT_MULTI_PLAYER_GAME_OVER ) ;
            		}
            		else if ( action == ExamineGameResultActivity.INTENT_RESULT_REWIND ) {
            			// used for Progression game over and level up.
            			gameService.resolvedToRewind() ;
            			
            			if ( QuantroPreferences.getAnalyticsActive(this) )
            				Analytics.logInGameExamineResolvedToRewind(gip.gameSettings.getMode(),
            						mResult == GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER
            						|| mResult == GameUserInterface.RESULT_MULTI_PLAYER_GAME_OVER ) ;
            		}
            	}
          
            } else {  
            	// Error displaying result. Just go ahead and quit.
            	quitGame( true ) ;
            } 
    		break ;
    		
    		
    		
    	case IntentForResult.GAME_OPTIONS_MENU:
    		Music m = Music.fromStringEncoding( data.getStringExtra(GameOptionsMenuActivity.INTENT_EXTRA_CURRENT_MUSIC) ) ;
    		if ( m != null && !Music.equals(m, mMusic) ) {
    			mMusic = m ;
    			soundPool.loadMusic(this, m) ;
    		}
    		
    		if ( resultCode == GameOptionsMenuActivity.RESULT_QUIT ) {
    			this.quitGame(true) ;
    		}
    		
    		break ;
    	}
    }
    
    
    /*
	 * *************************************************************************
	 * 
	 * WAKE LOCK COLLECTION
	 * 
	 * *************************************************************************
	 */
    
    private static final String LOCK_NAME_PARTIAL_STATIC="quantro:game_partial_wake_lock";
    private static final String LOCK_NAME_FULL_STATIC="quantro:game_full_wake_lock" ;
	private volatile PowerManager.WakeLock lockPartialStatic=null;
	private volatile PowerManager.WakeLock lockFullStatic=null ;
    
    synchronized private PowerManager.WakeLock getPartialLock(Context context) {
		if (lockPartialStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lockPartialStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
										LOCK_NAME_PARTIAL_STATIC);
			lockPartialStatic.setReferenceCounted(true);
		}
	  
		return(lockPartialStatic);
	}
    
    synchronized private PowerManager.WakeLock getDimLock(Context context) {
    	if (lockFullStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lockFullStatic=mgr.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
										LOCK_NAME_FULL_STATIC);
			lockFullStatic.setReferenceCounted(true);
		}
	  
		return(lockFullStatic);
    }
    
    
    
	/*
	 * *************************************************************************
	 * 
	 * SERVICE CONNECTION METHODS
	 * 
	 * These methods define our ServiceConnection interface.  The GameService handles
	 * most of our processing and game logic, whereas the GameActivity handles
	 * the GUI.
	 * 
	 * *************************************************************************
	 */
    
    
    /**
     * This method is called when the GameService connects to this Activity.  Now
     * is a good time to collect the references we need, such as game objects, and
     * connect them with our views and stuff.  In addition, we provide the Service
     * with a reference to ourself and give it 
     */
    synchronized public void onServiceConnected (ComponentName name, IBinder service) {
    	Log.d(TAG, "onServiceConnected") ;
    	// We ignore the component name, because we only bind with GameService.
    	// Get the service from the binder, and store a reference.
    	gameService = (GameService)( ((ServicePassingBinder)service).getService() ) ;
        
        setUpGameService() ;
    }
    
    
    private void setUpGameService() {
    	if ( gameService == null )
    		return ;
    	
    	try {
	    	// Give the service a reference to this, and make any state update
	        // calls needed.
	        gameService.setActivity(this, this.gip) ;
	        gameService.setGameUserInterface(this, this) ;
	        
	        if ( this.gip.isLocal() ) {
	        	if ( pauseViewComponentAdapter != null )
    				pauseViewComponentAdapter.setLocalPause(true) ;
	        	gameService.pauseByPlayer() ;
	        }
	        
	        // If we had a saved instance state, now is the time to tell the Service
	        // to read game data from it.
	        if ( savedInstanceState != null )
	        	gameService.readStateFromBundle(savedInstanceState) ;
	        
	        // Link our gameView to the service's game.
	        gameView.setGames(gameService.game, gameService.ginfo, gameService.gevents) ;
	        for ( int i = 0; i < numberOfPlayers; i++ ) {
	        	gameView.setPlayerStatus(i,
	        			gameService.getPlayerWon(i),
	        			gameService.getPlayerLost(i),
	        			gameService.getPlayerKicked(i),
	        			gameService.getPlayerQuit(i),
	        			gameService.getPlayerSpectating(i)) ;
	        }
	        // Don't yet link our controls; we do that upon notification of which player
	        // slot has been alloted to us.
	        
	        // We make successive calls using activityState; if activityState is RESUMED,
	        // for example, we make all calls up-to and including the call for resume.
	        // This would be easy if we made calls in REVERSE (just do a fall-through switch)
	        // but they need to be in-order.
	        
	        // Instead, exploit the fact that the ACTIVITY_STATE values are in ascending order.
	        Log.d(TAG, "setUpGameService maybe DidCreate with activity state " + activityState) ;
	        if ( activityState >= ACTIVITY_STATE_CREATED )
	        	gameService.gameActivityDidCreate(this) ;
	        Log.d(TAG, "setUpGameService maybe DidStart with activity state " + activityState) ;
	        if ( activityState >= ACTIVITY_STATE_STARTED )
	        	gameService.gameActivityDidStart(this) ;
	        Log.d(TAG, "setUpGameService maybe DidResume with activity state " + activityState) ;
	        if ( activityState >= ACTIVITY_STATE_RESUMED )
	        	gameService.gameActivityDidResume(this) ;
	        Log.d(TAG, "setUpGameService maybe DidPause with activity state " + activityState) ;
	        if ( activityState >= ACTIVITY_STATE_PAUSED )
	        	gameService.gameActivityDidPause(this) ;
	        Log.d(TAG, "setUpGameService maybe DidStop with activity state " + activityState) ;
	        if ( activityState >= ACTIVITY_STATE_STOPPED )
	        	gameService.gameActivityDidStop(this) ;
	        Log.d(TAG, "setUpGameService maybe DidDestroy with activity state " + activityState) ;
	        if ( activityState >= ACTIVITY_STATE_DESTROYED )
	        	gameService.gameActivityDidDestroy(this) ;
	        
    	} catch( Exception e ) {
    		e.printStackTrace() ;
    	}
    }
    
    
    /**
     * The service has disconnected.  Why?  We ONLY unbind upon destruction!
     */
    synchronized public void onServiceDisconnected(ComponentName className) {
        // Meh.
    	mostRecentGameResult = gameService.getGameResult() ;
    	gameService = null ;
    }
    
    
	/*
	 * *************************************************************************
	 * 
	 * DIALOG CALLBACKS
	 * 
	 * For creating and setting the text of any dialogs we might need to display.
	 * 
	 * *************************************************************************
	 */

    
    
    protected Dialog onCreateDialog(int id) {
    	// TODO: This method still uses the "old style" formatting, directly
    	// accessing string IDs.  We are (gradually) migrating to
    	// use of the DialogFormatting class static methods.  Make the
    	// changes (see onPrepareDialog for examples).
        ProgressDialog pd ;
        AlertDialog.Builder builder ;
        int role = gip.isClient() ? TextFormatting.ROLE_CLIENT : TextFormatting.ROLE_HOST ;
        int display = TextFormatting.DISPLAY_DIALOG ;
        int type ;
        
        
        switch(id) {
        // Spinning pinwheel for "connecting;" differs depending
        // on whether we are the host or a client.
        case DIALOG_CONNECTING_TO_SERVER_ID:
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage("") ;		// Resolves a bug - see OPPONENT_QUIT case.
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					GameActivity.this.networkDialogCanceled() ;
        				}
					}) ;
        	return pd ;
        	
        // Spinning pinwheel for "talking to"
        case DIALOG_TALKING_TO_SERVER_ID:
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage("") ;		// Resolves a bug - see OPPONENT_QUIT case.
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					GameActivity.this.networkDialogCanceled() ;
        				}
					}) ;
        	return pd ;
    	
        // "Waiting for" dialog.  Since this dialog is NOT generic, we instantiate
        // everything but the message shown, which will be edited in onPrepareDialog.
        case DIALOG_WAITING_FOR_PLAYERS_ID:
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage("") ;		// Resolves a bug - see OPPONENT_QUIT case.
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					GameActivity.this.networkDialogCanceled() ;
        				}
					}) ;
        	return pd ;
        	
    	// "Paused by" dialog.  Since this dialog is NOT generic, we instantiate
        // everything but the message shown, which will be edited in onPrepareDialog.
        case DIALOG_PAUSED_BY_PLAYERS_ID:
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage("") ;		// Resolves a bug - see OPPONENT_QUIT case.
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					GameActivity.this.networkDialogCanceled() ;
        				}
					}) ;
        	return pd ;

        // "Quit singleplayer" dialog
        case DIALOG_QUIT_SINGLE_PLAYER_ID:
        	// This is a true dialog, with choices.
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setMessage(R.string.in_game_dialog_quit_single_player_message) ;
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses
        										// alert, but does not quit game.
        	builder.setPositiveButton(
        			res.getString( R.string.in_game_dialog_quit_single_player_yes_button ),
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					dialogManager.dismissDialog(DIALOG_QUIT_SINGLE_PLAYER_ID) ;
        					mHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									GameActivity.this.quitGame( true ) ;
								}
        					}, 500) ;
        				}
					}) ;
        	builder.setNeutralButton(
        			res.getString( R.string.in_game_dialog_quit_single_player_menu_button ),
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					dialogManager.dismissDialog(DIALOG_QUIT_SINGLE_PLAYER_ID) ;
        					userActionLaunchGameOptions() ;
        				}
					}) ;
        	builder.setNegativeButton(
        			res.getString( R.string.in_game_dialog_quit_single_player_no_button ),
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							dialogManager.dismissDialog(DIALOG_QUIT_SINGLE_PLAYER_ID) ;
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					// dismiss this
					dialogManager.dismissDialog(DIALOG_QUIT_SINGLE_PLAYER_ID) ;
				}
			}) ;
        	
        	return builder.create();
        	
        // "Quit multiplayer" dialog.
        case DIALOG_QUIT_MULTIPLAYER_ID:
        	// This is a true dialog - with choices.
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setMessage("") ;
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses
        										// alert, but does not quit game.
        	builder.setPositiveButton(
        			( gip.isClient() )
        					? res.getString( R.string.in_game_dialog_quit_multiplayer_as_client_yes_button )
        					: res.getString( R.string.in_game_dialog_quit_multiplayer_as_host_yes_button ),
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					dialogManager.dismissDialog(DIALOG_QUIT_MULTIPLAYER_ID) ;
        					mHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									GameActivity.this.quitGame( true ) ;
								}
        					}, 500) ;
        				}
					}) ;
        	builder.setNeutralButton(
        			res.getString( R.string.in_game_dialog_quit_multiplayer_menu_button ),
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					dialogManager.dismissDialog(DIALOG_QUIT_MULTIPLAYER_ID) ;
        					userActionLaunchGameOptions() ;
        				}
					}) ;
        	builder.setNegativeButton(
        			( gip.isClient() )
							? res.getString( R.string.in_game_dialog_quit_multiplayer_as_client_no_button )
							: res.getString( R.string.in_game_dialog_quit_multiplayer_as_host_no_button ),
        			new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id) {
							dialogManager.dismissDialog(DIALOG_QUIT_MULTIPLAYER_ID) ;
						}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					// dismiss this
					dialogManager.dismissDialog(DIALOG_QUIT_MULTIPLAYER_ID) ;
				}
			}) ;
        	
        	return builder.create();
        	
        // The opponent quit.  For now, we must quit as well.
        case DIALOG_OPPONENT_QUIT_MULTIPLAYER_ID:
        	// This is a true dialog, but with only one choice.  However,
        	// because WHO QUIT is significant, we leave it to onPrepareDialog
        	// to actually configure the message.
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setCancelable(false) ;		// NOT cancelable - there is no game to play after this.
        	builder.setPositiveButton( res.getString( R.string.in_game_dialog_opponent_quit_multiplayer_quit_button ),
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					GameActivity.this.quitGame( true ) ;
        				}
					}) ;
        	builder.setTitle(R.string.in_game_dialog_opponent_quit_multiplayer_title) ;
        	builder.setMessage("") ;		// Resolves a bug - if instanted
        									// with 'null' message, i.e. if no
        									// call to 'setMessage' here, then 
        									// message visibily will be set to
        									// 'false' and a message set later
        									// will not display.
        	return builder.create();
        	
        case DIALOG_MATCHMAKING_NO_RESPONSE_ID:
        	// A spinny pinwheel.
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage( res.getString(R.string.in_game_dialog_matchmaking_no_response) ) ;	
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					GameActivity.this.queryOrMenuOrQuit() ;
        				}
					}) ;
        	return pd ;
        	
        case DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID:
        	// A spinny pinwheel.
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage( res.getString(R.string.in_game_dialog_matchmaking_reject_full) ) ;	
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					GameActivity.this.queryOrMenuOrQuit() ;
        				}
					}) ;
        	return pd ;
        
        case DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID:
        	// A spinny pinwheel.
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage( res.getString(R.string.in_game_dialog_matchmaking_reject_port_randomization) ) ;	
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					GameActivity.this.queryOrMenuOrQuit() ;
        				}
					}) ;
        	return pd ;
        
        case DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID:
        	// An interactive dialog; the button quits.
        	// Nothing here is dependent on player names or any other session-specific info.
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setCancelable(true) ;		// Cancelable.  Hitting "back" will quit.
        	builder.setTitle(R.string.in_game_dialog_matchmaking_reject_title) ;
        	builder.setMessage(R.string.in_game_dialog_matchmaking_reject_invalid_nonce) ;
        	builder.setNegativeButton( R.string.in_game_dialog_matchmaking_reject_button_quit,
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					finish() ;
        				}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					finish() ;
					}
				}) ;
        	return builder.create();
        	
        case DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID:
        	// An interactive dialog; the button quits.
        	// Nothing here is dependent on player names or any other session-specific info.
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setCancelable(true) ;		// Cancelable.  Hitting "back" will quit.
        	builder.setTitle(R.string.in_game_dialog_matchmaking_reject_title) ;
        	builder.setMessage(R.string.in_game_dialog_matchmaking_reject_nonce_in_use) ;
        	builder.setNegativeButton( R.string.in_game_dialog_matchmaking_reject_button_quit,
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					finish() ;
        				}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					finish() ;
					}
				}) ;
        	return builder.create();
        
        case DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID:
        	// An interactive dialog; the button quits.
        	// Nothing here is dependent on player names or any other session-specific info.
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setCancelable(true) ;		// Cancelable.  Hitting "back" will quit.
        	builder.setTitle(R.string.in_game_dialog_matchmaking_reject_title) ;
        	builder.setMessage(R.string.in_game_dialog_matchmaking_reject_unspecified) ;
        	builder.setNegativeButton( R.string.in_game_dialog_matchmaking_reject_button_quit,
        			new DialogInterface.OnClickListener() {
        				public void onClick( DialogInterface dialog, int id) {
        					finish() ;
        				}
					}) ;
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					finish() ;
					}
				}) ;
        	return builder.create();
        	
        	
        case DIALOG_NO_WIFI_CONNECTION:
        	// A progress dialog.
        	role = TextFormatting.ROLE_CLIENT ;
        	display = TextFormatting.DISPLAY_DIALOG ;
        	pd = new ProgressDialog( this ) ;
        	pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
        	pd.setCancelable(true) ;	// Cancelable so 'back' button still works to quit
        	pd.setMessage( TextFormatting.format(
        			this, res,
        			TextFormatting.DISPLAY_DIALOG,
        			TextFormatting.TYPE_GAME_NO_WIFI,
        			TextFormatting.ROLE_CLIENT,
        			(String[])null) ) ;	
        	pd.setOnCancelListener( new DialogInterface.OnCancelListener() {
        				public void onCancel( DialogInterface dialog ) {
        					queryOrMenuOrQuit() ;
        				}
					}) ;
        	return pd ;
        	
        	
        case DIALOG_GAME_WEB_COMMUNICATION_FAILURE:
			// trouble communicating.
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(R.string.in_game_dialog_cgi_communication_failure_message) ;
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					GameActivity.this.queryOrMenuOrQuit();
				}
			});
			return pd;
			
		case DIALOG_GAME_WEB_CLOSED:
			// permanent communication failure
			builder = new AlertDialog.Builder(this);
			builder.setCancelable(true); // Cancelable. Hitting "back" will
											// quit.
			builder.setTitle(R.string.in_game_dialog_cgi_game_closed_title) ;
			builder.setMessage(R.string.in_game_dialog_cgi_game_closed_message) ;
			builder.setNegativeButton(
					R.string.in_game_dialog_cgi_game_closed_button_quit,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							quitGame(true);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					quitGame(true);
				}
			});
			return builder.create();
			
			
    	default:
            return null ;
        }
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	// THIS METHOD IS DEPRECIATED
    	// (and I don't care!  the DialogFragment class will call through
    	// to this method for compatibility)
    	
    	// DialogFormatting.DISPLAY_*, DialogFormatting.ROLE_*.
    	int display = TextFormatting.DISPLAY_DIALOG ;
    	int role = ( gip.isClient() )
    			? TextFormatting.ROLE_CLIENT 
    			: TextFormatting.ROLE_HOST ;
    	int type ;
    	String text ;
    	
    	String opponentName = null ;
        for ( int i = 0; i < this.playerName.length; i++ ) {
        	if ( this.playerName != null && this.playerName[i] != null && !this.playerName[i].equals( gip.name ) ) {
        		opponentName = this.playerName[i] ;
        		break ;
        	}
        }
        if ( opponentName == null ) 
        	opponentName = "opponent" ;
    	
    	switch ( id ) {
        case DIALOG_CONNECTING_TO_SERVER_ID:
        	type = TextFormatting.TYPE_GAME_CONNECTING ;
        	text = TextFormatting.format(this, res, display, type, role, opponentName ) ;
        	((ProgressDialog)dialog).setMessage(text) ;
        	break ;
        	
        case DIALOG_TALKING_TO_SERVER_ID:
        	type = TextFormatting.TYPE_GAME_NEGOTIATING ;
        	text = TextFormatting.format(this, res, display, type, role, opponentName ) ;
        	((ProgressDialog)dialog).setMessage(text) ;
        	break ;
    	
        // "Waiting for" dialog.  This is NOT generic;
        // we indicate which players are being waited for.
        case DIALOG_WAITING_FOR_PLAYERS_ID:
        	// Set as the appropriate string, with placeholder replaced.
        	text = TextFormatting.format(this, res, display,
        			TextFormatting.TYPE_GAME_WAITING,
        			role,
        			ArrayOps.clone(playerName),
        			ArrayOps.clone(this.wasWaitingForPlayers) ) ;
        	((ProgressDialog)dialog).setMessage(text) ;
        	break ;
        	
    	// "Paused by" dialog.  Indicate which players
        // we are waiting for.
        case DIALOG_PAUSED_BY_PLAYERS_ID:
        	// Set as the appropriate string, with placeholder replaced.
        	text = TextFormatting.format(this, res, display,
        			TextFormatting.TYPE_GAME_PAUSED,
        			role,
        			ArrayOps.clone(playerName),
        			ArrayOps.clone(this.wasPausedByPlayers) ) ;
        	((ProgressDialog)dialog).setMessage(text) ;
        	break ;

        // "Quit multiplayer" dialog.
        case DIALOG_QUIT_MULTIPLAYER_ID:
        	type = TextFormatting.TYPE_GAME_QUIT ;
        	((AlertDialog)dialog).setMessage( TextFormatting.format(this, res, display, type, role, opponentName) ) ;
        	break ;
        
        // The opponent quit.  Our message must be configured
        // to show the coward's name.
        case DIALOG_OPPONENT_QUIT_MULTIPLAYER_ID:
        	// Set as the appropriate string, with placeholder replaced.
        	// Note: for this type of dialog, the "client/host" role
        	// indicates the OPPONENT WHO QUIT, not ourselves.
        	text = TextFormatting.format(this, res, display,
        			TextFormatting.TYPE_GAME_OPPONENT_QUIT, 
        			wasLeftByPlayer == 0 ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT,
        			playerName[wasLeftByPlayer]) ;
        	Log.d(TAG, "prepareDialog with text " + text) ;
        	((AlertDialog)dialog).setMessage(text) ;
        	break ;
        	
        case DIALOG_NO_WIFI_CONNECTION:
        	break ;
        	
        // The huh?
    	default:
            return ;
    	}
    }
    
    
    boolean toggle_fall = false ;
    boolean toggle_slide_left = false ;
    boolean toggle_slide_right = false ;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	//Log.d(TAG, "onKeyDown") ;
    	
    	// try a keyboard actions!
    	if ( GlobalTestSettings.GAME_ALLOW_KEYBOARD ) {
    		boolean didUse = true ;
    		if ( keyCode == KeyEvent.KEYCODE_J ) {
    			controlsToActionsAdapter.userDidTapButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    			if ( toggle_slide_right ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    				toggle_slide_right = false ;
    			}
    		}
    		else if ( keyCode == KeyEvent.KEYCODE_H ) {
    			if ( !toggle_slide_left )
    				controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    			else
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    			toggle_slide_left = !toggle_slide_left ;
    			
    			if ( toggle_slide_right ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    				toggle_slide_right = false ;
    			}
    		}
    		else if ( keyCode == KeyEvent.KEYCODE_K ) {
    			if ( !toggle_fall )
    				controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.DOWN_BUTTON) ;
    			else
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.DOWN_BUTTON) ;
    			toggle_fall = !toggle_fall ;
    		}
    		else if ( keyCode == KeyEvent.KEYCODE_L ) {
    			controlsToActionsAdapter.userDidTapButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    			if ( toggle_slide_left ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    				toggle_slide_left = false ;
    			}
    		}
    		else if ( keyCode == KeyEvent.KEYCODE_SEMICOLON ) {
    			if ( !toggle_slide_right )
    				controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    			else
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    			toggle_slide_right = !toggle_slide_right ;
    			
    			if ( toggle_slide_left ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    				toggle_slide_left = false ;
    			}
    		}
    		else if ( keyCode == KeyEvent.KEYCODE_U ) {
    			controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.CCW_BUTTON) ;
    			if ( toggle_slide_left ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    				toggle_slide_left = false ;
    			}
    			if ( toggle_slide_right ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    				toggle_slide_right = false ;
    			}
    		}
    		else if ( keyCode == KeyEvent.KEYCODE_I )
    			controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.RESERVE_BUTTON) ;
    		else if ( keyCode == KeyEvent.KEYCODE_O ) {
    			controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.CW_BUTTON) ;
    			if ( toggle_slide_left ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    				toggle_slide_left = false ;
    			}
    			if ( toggle_slide_right ) {
    				controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    				toggle_slide_right = false ;
    			}
    		}
    		else if ( keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_COMMA )
    			controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.SLIDE_DOWN_BUTTON ) ;
    		else
    			didUse = false ;
    		
    		if ( didUse )
    			return true ;
    	}
    	
    	
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	soundPool.menuButtonBack() ;
        	// If the options menu is shown, tell it what happened.  Otherwise,
        	// pause and put up the menu.
        	if ( gameOptionsViewShown && gameOptionsFragmentUsed ) {
        		gameOptionsMenuFragment.backButtonPressed() ;
        	} else {
        		queryOrMenuOrQuit() ;
        	}
        	return true ;
        }
        
        if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
        	soundPool.menuButtonClick() ;
        	if ( !gameOptionsViewShown || !gameOptionsFragmentUsed ) {
        		if ( gameOptionsFragmentUsed ) {
            		userActionPauseOn() ;
            		runOnUiThread( gameOptionsMenuFragmentSetVisibilityVisibleRunnable ) ;
            	} else {
            		userActionPauseOn() ;
            		runOnUiThread( pauseViewComponentAdapterSetVisibilityVisibleRunnable ) ;
            		userActionLaunchGameOptions() ;
            	}
        	} else {
        		userActionPauseOff() ;
        		runOnUiThread( gameOptionsMenuFragmentSetVisibilityGoneRunnable ) ;
        	}
        	return true ;
        }
        
        if ( this.gameService != null && gameService.isPausedByPlayer()
        		&& ( keyCode == KeyEvent.KEYCODE_DPAD_LEFT 
        				|| keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        				|| keyCode == KeyEvent.KEYCODE_DPAD_DOWN
        				|| keyCode == KeyEvent.KEYCODE_DPAD_CENTER
        				|| keyCode == KeyEvent.KEYCODE_DPAD_UP ) ) {
        	userActionTogglePause() ;
        	return true ;
        }
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !mResolvingResult ) {
        	controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
        	return true ;
        }
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && !mResolvingResult ) {
        	controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
        	return true ;
        }
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_DOWN && !mResolvingResult ) {
        	controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.DOWN_BUTTON) ;
        	return true ;
        }
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_CENTER && !mResolvingResult ) {
        	controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.RESERVE_BUTTON) ;
        	return true ;
        }
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_UP && !mResolvingResult ) {
        	controlsToActionsAdapter.userDidPressButton(ControlsToActionsAdapter.CW_BUTTON) ;
        	return true ;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)  {
    	
    	// try a keyboard actions!
    	if ( GlobalTestSettings.GAME_ALLOW_KEYBOARD ) {
    		boolean didUse = true ;
    		if ( keyCode == KeyEvent.KEYCODE_J )
    			controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
    		else if ( keyCode == KeyEvent.KEYCODE_L )
    			controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
    		else if ( keyCode == KeyEvent.KEYCODE_U )
    			controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.CCW_BUTTON) ;
    		else if ( keyCode == KeyEvent.KEYCODE_I )
    			controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RESERVE_BUTTON) ;
    		else if ( keyCode == KeyEvent.KEYCODE_O )	
    			controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.CW_BUTTON) ;
    		else if ( keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_COMMA )
    			controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.SLIDE_DOWN_BUTTON ) ;
    		else
    			didUse = false ;
    		
    		if ( didUse )
    			return true ;
    	}
    	
    	//Log.d(TAG, "onKeyUp") ;
        if ( keyCode == KeyEvent.KEYCODE_DPAD_LEFT )
        	controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.LEFT_BUTTON) ;
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_RIGHT )
        	controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.RIGHT_BUTTON) ;
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_DOWN )
        	controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.DOWN_BUTTON) ;
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_CENTER )
        	controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.CCW_BUTTON) ;
        else if ( keyCode == KeyEvent.KEYCODE_DPAD_UP )
        	controlsToActionsAdapter.userDidReleaseButton(ControlsToActionsAdapter.CW_BUTTON) ;
        else
        	return super.onKeyUp(keyCode, event);
        
        return true ;
    }
    
    
    
    
    public void queryOrMenuOrQuit() {
    	// If this is a local game, just quit.
    	// If a multiplayer game, put up a dialog.
		switch( QuantroPreferences.getBackButtonBehavior(this) ) {
		case QuantroPreferences.BACK_BUTTON_QUIT:
			this.quitGame(true) ;
			break ;
		case QuantroPreferences.BACK_BUTTON_OPTIONS:
			// pause no matter what
			userActionPauseOn() ;
			if ( gameOptionsFragmentUsed ) {
        		runOnUiThread( gameOptionsMenuFragmentSetVisibilityVisibleRunnable ) ;
        	} else {
        		runOnUiThread( pauseViewComponentAdapterSetVisibilityVisibleRunnable ) ;
        		userActionLaunchGameOptions() ;
        	}
    		break ;
		case QuantroPreferences.BACK_BUTTON_ASK:
			if ( gip.isLocal() ) {
    			userActionPauseOn() ;
    			dialogManager.showDialog( GameActivity.DIALOG_QUIT_SINGLE_PLAYER_ID ) ;
			} else {
				dialogManager.showDialog( GameActivity.DIALOG_QUIT_MULTIPLAYER_ID ) ;	
			}
			break ;
		}
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // CONTROLS METHODS
    //
    
    
    private Runnable setControlsGamepadConfigureViewsRunnable = null ;
    private Runnable setControlsGamepadTellViewsRunnable = null ;
    
    private void setControlsGamepad() {
    	// Because this method is usually called from the UI thread, we
    	// previously had this in-lined.  However, occasionally the ClientCommunications
    	// starts a controls setting, leading to View-access errors that break the connection.
    	// We therefore set up Runnables and run them on the UI thread.
    	if ( setControlsGamepadConfigureViewsRunnable == null ) {
    		setControlsGamepadConfigureViewsRunnable = new Runnable() {
				@Override
				public void run() {
					controlsGamepad.setVisibility(View.VISIBLE) ;
			    	controlsGesture.setVisibility(View.INVISIBLE) ;
			    	// opponent button?
			    	String [] opponentButtonNames = controlsToActionsAdapter.getButtonNames(ControlsToActionsAdapter.BUTTON_TYPE_OPPONENT) ;
			    	controlsGamepad.setTouchable(
			    			numberOfPlayers > 1,
			    			opponentButtonNames ) ;
			    	if ( numberOfPlayers > 2 ) {
			    		// slideable between all opponents.
			    		InvisibleControls.SlideRegionType regionType ;
			    		// based on screen height.
			    		if ( VersionSafe.getScreenSizeCategory(GameActivity.this) >= VersionSafe.SCREEN_SIZE_LARGE ) {
			    			regionType = InvisibleControls.SlideRegionType.UNIFORM_VERTICAL_CENTERED_AROUND ;
			    		} else {
			    			regionType = InvisibleControls.SlideRegionType.UNIFORM_VERTICAL ;
			    		}
			    		controlsGamepad.putSlideHandler(
			    				GameActivity.this,
			    				opponentButtonNames[0],
			    				regionType,
			    				numberOfPlayers - 1) ;
			    	} else {
			    		// no slideable
			    		controlsGamepad.removeSlideHandler(opponentButtonNames[0]) ;
			    	}
			    	
			    	if ( QuantroPreferences.getControlsShowButtons(GameActivity.this) ) {
			    		controlsGamepad.setShowWhenPressedDefault(
			    				controlsToActionsAdapter.getButtonNames(ControlsToActionsAdapter.BUTTON_TYPE_ALL)) ;
			    	} else {
			    		controlsGamepad.setShowWhenPressed( false,
			    				controlsToActionsAdapter.getButtonNames(ControlsToActionsAdapter.BUTTON_TYPE_ALL)) ;
			    	}
			    	
			    	controlsToActionsAdapter.setQuickSlideSupported(
			    			QuantroPreferences.getControlsGamepadQuickSlide(GameActivity.this)) ;
			    	
			    	controlsToActionsAdapter.setDownButtonBehavior(
			    			QuantroPreferences.getControlsGamepadDropButton(GameActivity.this) == QuantroPreferences.CONTROLS_DROP_FALL
				        			? ControlsToActionsAdapter.BEHAVIOR_DOWN_AUTOFALL
				        			: ControlsToActionsAdapter.BEHAVIOR_DOWN_DROP,
			    			QuantroPreferences.getControlsGamepadDropAutolocks(GameActivity.this)) ;
			    	
			    	controlsToActionsAdapter.setSlideDownButtonBehavior(
			    			QuantroPreferences.getControlsGamepadDoubleDownDrop(GameActivity.this)
			    					? ControlsToActionsAdapter.BEHAVIOR_DOWN_DROP
			    					: ControlsToActionsAdapter.BEHAVIOR_DOWN_NONE,
			    			true) ;
					
					int gameMode = gip.gameSettings.getMode() ;
					
			    	if ( controlsGamepad instanceof InvisibleControlsGamepadOfRows ) {
			    		InvisibleControlsGamepadOfRows gamepadOfRows
			    				= (InvisibleControlsGamepadOfRows)controlsGamepad ;
			
			    		
			    		gamepadOfRows.customize_setHasButtons(
			    				GameModes.hasRotation(gameMode),
			    				GameModes.hasReflection(gameMode)) ;
			    		
			    		gamepadOfRows.customize_setTurnButtonsAboveMoveButtons(
			    				!QuantroPreferences.getControlsGamepadSwapTurnAndMove(GameActivity.this)) ;
			    		
			    		gamepadOfRows.customize_setNextThenReserve(
			    				!QuantroPreferences.getGraphicsPiecePreviewsSwapped(GameActivity.this)) ;
			    		
			    		switch( QuantroPreferences.getControlsGamepadCenterButtonWidth(GameActivity.this) ) {
			    		case QuantroPreferences.CENTER_BUTTON_WIDTH_STANDARD:
			    			gamepadOfRows.customize_setCenterButtonWidthScale( 1.0f ) ;
			    			break ;
			    		case QuantroPreferences.CENTER_BUTTON_WIDTH_PANEL_TO_PANEL:
			    			gamepadOfRows.customize_setCenterButtonWithinNextVisibleBounds() ;
			    			break ;
			    		case QuantroPreferences.CENTER_BUTTON_WIDTH_CUSTOM:
			    			gamepadOfRows.customize_setCenterButtonWidthScale( QuantroPreferences.getControlsGamepadCenterButtonWidthScaleFactor(GameActivity.this) ) ;
			    			break ;
			    		}
			    		
			    		gamepadOfRows.customize_setControlButtonsHeightScale( QuantroPreferences.getControlsGamepadButtonHeightScaleFactor(GameActivity.this) ) ;
			    		
			    		gamepadOfRows.remeasure() ;
			    	}
			    	
			    	QuantroPreferences.setControls(GameActivity.this, QuantroPreferences.CONTROLS_GAMEPAD) ;
				}
    			
    		} ;
    	}
    	
    	if ( setControlsGamepadTellViewsRunnable == null ) {
    		setControlsGamepadTellViewsRunnable = new Runnable() {
				@Override
				public void run() {
					int gameMode = gip.gameSettings.getMode() ;
			    	gameView.setControls(controlsGamepad) ;
			        pauseViewComponentAdapter.setControlsGamepad(
			        		true,
			        		QuantroPreferences.getControlsGamepadDropButton(GameActivity.this) == QuantroPreferences.CONTROLS_DROP_FALL,
			        		QuantroPreferences.getControlsGamepadDoubleDownDrop(GameActivity.this) ) ;
			        pauseViewComponentAdapter.setControlsSupportsQuickSlide(
			        		QuantroPreferences.getControlsGamepadQuickSlide(GameActivity.this)) ;
			        pauseViewComponentAdapter.setControlsHas(
							GameModes.hasRotation(gameMode),
							GameModes.hasReflection(gameMode)) ;
				}
    		} ;
    	}
    	
    	runOnUiThread( setControlsGamepadConfigureViewsRunnable ) ;
    	runOnUiThread( setControlsGamepadTellViewsRunnable ) ;
    }
    
    
    private Runnable setControlsGestureConfigureViewsRunnable = null ;
    private Runnable setControlsGestureTellViewsRunnable = null ;
    
    private void setControlsGesture() {
    	// Because this method is usually called from the UI thread, we
    	// previously had this in-lined.  However, occasionally the ClientCommunications
    	// starts a controls setting, leading to View-access errors that break the connection.
    	// We therefore set up Runnables and run them on the UI thread.
    	if ( setControlsGestureConfigureViewsRunnable == null ) {
    		setControlsGestureConfigureViewsRunnable = new Runnable() {
				@Override
				public void run() {
					controlsGamepad.setVisibility(View.INVISIBLE) ;
			    	controlsGesture.setVisibility(View.VISIBLE) ;
			    	// opponent button?
			    	String [] opponentButtonNames = controlsToActionsAdapter.getButtonNames(ControlsToActionsAdapter.BUTTON_TYPE_OPPONENT) ;
			    	controlsGesture.setTouchable(
			    			numberOfPlayers > 1,
			    			opponentButtonNames) ;
			    	if ( numberOfPlayers > 2 ) {
			    		// slideable between all opponents.
			    		InvisibleControls.SlideRegionType regionType ;
			    		// based on screen height.
			    		if ( VersionSafe.getScreenSizeCategory(GameActivity.this) >= VersionSafe.SCREEN_SIZE_LARGE ) {
			    			regionType = InvisibleControls.SlideRegionType.UNIFORM_VERTICAL_CENTERED_AROUND ;
			    		} else {
			    			regionType = InvisibleControls.SlideRegionType.UNIFORM_VERTICAL ;
			    		}
			    		
			    		controlsGesture.putSlideHandler(
			    				GameActivity.this,
			    				opponentButtonNames[0],
			    				regionType,
			    				numberOfPlayers - 1) ;
			    	} else {
			    		// no slideable
			    		controlsGesture.removeSlideHandler(opponentButtonNames[0]) ;
			    	}
					if ( QuantroPreferences.getControlsShowButtons(GameActivity.this) ) {
						controlsGesture.setShowWhenPressedDefault(
			    				controlsToActionsAdapter.getButtonNames(ControlsToActionsAdapter.BUTTON_TYPE_ALL)) ;
			    	} else {
			    		controlsGesture.setShowWhenPressed( false,
			    				controlsToActionsAdapter.getButtonNames(ControlsToActionsAdapter.BUTTON_TYPE_ALL)) ;
			    	}
					
					// drag exagg.
					controlsGesture.setSlideExaggeration(
							QuantroPreferences.getControlsGestureDragExaggeration(GameActivity.this)) ;
					
					// fling sensitivity
					controlsGesture.setFlingSensitivity(
							QuantroPreferences.getControlsGestureFlingSensitivity(GameActivity.this)) ;
			    	
			    	controlsToActionsAdapter.setQuickSlideSupported(
			    			QuantroPreferences.getControlsGestureQuickSlide(GameActivity.this)) ;
			    	
			    	controlsToActionsAdapter.setDownButtonBehavior(
			    			ControlsToActionsAdapter.BEHAVIOR_DOWN_FALL,
			    			QuantroPreferences.getControlsGestureDragDownAutolock(GameActivity.this)) ;
			    	
			    	controlsToActionsAdapter.setSlideDownButtonBehavior(
			    			ControlsToActionsAdapter.BEHAVIOR_DOWN_DROP, true) ;
			    	
			    	int gameMode = gip.gameSettings.getMode() ;
			    	
			    	String [] turnButtons = controlsToActionsAdapter.getButtonNames( ControlsToActionsAdapter.BUTTON_TYPE_TURN ) ;
			    	controlsGesture.setTouchable(
			    			QuantroPreferences.getControlsGestureTurnButtons(GameActivity.this) && GameModes.hasRotation(gameMode),
			    			turnButtons) ;
			    	
			    	if ( controlsGesture instanceof InvisibleControlsGestureOfRows ) {
			    		InvisibleControlsGestureOfRows gestureOfRows
			    				= (InvisibleControlsGestureOfRows)controlsGesture ;
			    		
			    		gestureOfRows.customize_setHasGestures(
			    				GameModes.hasRotation(gameMode),
			    				GameModes.hasReflection(gameMode)) ;
			    		
			    		gestureOfRows.customize_setNextThenReserve(
			    				!QuantroPreferences.getGraphicsPiecePreviewsSwapped(GameActivity.this)) ;

			    		gestureOfRows.remeasure() ;
			    	}
			    	
			    	QuantroPreferences.setControls(GameActivity.this, QuantroPreferences.CONTROLS_GESTURE) ;

				}
    			
    		} ;
    	}
    	
    	if ( setControlsGestureTellViewsRunnable == null ) {
    		setControlsGestureTellViewsRunnable = new Runnable() {

				@Override
				public void run() {
					int gameMode = gip.gameSettings.getMode() ;
					gameView.setControls(controlsGesture) ;
			    	pauseViewComponentAdapter.setControlsGamepad(
			        		false,
			        		QuantroPreferences.getControlsGamepadDropButton(GameActivity.this) == QuantroPreferences.CONTROLS_DROP_FALL,
			        		QuantroPreferences.getControlsGamepadDoubleDownDrop(GameActivity.this) ) ;
			    	pauseViewComponentAdapter.setControlsSupportsQuickSlide(
			    			QuantroPreferences.getControlsGestureQuickSlide(GameActivity.this)) ;
			    	pauseViewComponentAdapter.setControlsHas(
							GameModes.hasRotation(gameMode),
							GameModes.hasReflection(gameMode)) ;
				}
    		} ;
    	}
    	
    	runOnUiThread( setControlsGestureConfigureViewsRunnable ) ;
    	runOnUiThread( setControlsGestureTellViewsRunnable ) ;
    }
    
    
    @Override
    public void invisibleControlsUserDidSlideIn( InvisibleControls invisControls, String buttonName, int slideRegion ) {
    	// slide regions are numbered from 0 to numPlayers-2.
    	// This changes who is displayed as the current thumbnail.
    	if ( playerSlot > slideRegion ) {
    		opponentSlot = slideRegion ;
    	} else {
    		opponentSlot = slideRegion + 1 ;
    	}
    	gameView.setOpponentGame(opponentSlot) ;
    }
    
    
    @Override
	public void invisibleControlsUserDidSlideOut( InvisibleControls invisControls, String buttonName, int slideRegion ) {
		// we don't care.
	}
    
    
    //
    ////////////////////////////////////////////////////////////////////////////
    
    
    
    
    /////////////////////////////////////////////////////////////////////////
    //
    // DIALOG HELPERS
    //
    // Methods to help with dialogs, including callbacks.
    //
    ////////////////////////////////////////////////////////////////////////// 
    
    
    /**
     * This method is called if a "network dialog" is canceled by the user
     * hitting "back".  It's highly likely that we should show a quit dialog now.
     */
    public void networkDialogCanceled() {
    	// Note: we do NOT call dialogManager.dismiss() for the "network dialog"
    	// canceled.  This is because we want the network dialog to reappear open
    	// the QUIT_MULTIPLAYER dialog being dismissed.
    	Log.d(TAG, "networkDialogCanceled") ;
    	dialogManager.showDialog(DIALOG_QUIT_MULTIPLAYER_ID) ;
    }
    
    

    
    /////////////////////////////////////////////////////////////////////////
    //
    // HELPERS FOR GAME ACTIVITY STATE METHODS
    //
    // These methods provide nice ways of altering the game activity state.
    //
    //////////////////////////////////////////////////////////////////////////    

    /**
     * Quits the current game.  If multiplayer, sends
     * appropriate messages first.  If 'callFinish' is true, this
     * method will explicitly call this.finish() once everything else
     * is taken care of.
     * 
     * If 'callFinish' is false, we assume some other process is
     * finishing, and this method call occurred in the middle of it -
     * we do not "finish".
     */
    public void quitGame( boolean callFinish ) {
    	Log.d(TAG, "quitGame") ;
    	
    	// quit service
    	if ( gameService != null )
    		gameService.gameActivityQuitting(this) ;
    	
    	if ( gameView != null )
    		gameView.recycle(false) ;
    	if ( callFinish )
    		this.finish() ;
    }
    
    
    protected void examineResult( int resultType ) {
    	
    	// FIRST: if this is a "checkpoint" style resolve, immediately resolveToContinue
    	// and return.  We don't actually put up any Activities for this.
    	if ( resultType == GameUserInterface.RESULT_SINGLE_PLAYER_CHECKPOINT ) {
    		// resolvedToContinue will sometimes touch views / instantiate handlers.
    		// Need to do this on a UI thread.
    		runOnUiThread( new Runnable() {
    			public void run() {
    				gameService.resolvedToContinue() ;
    			}
    		}) ;
    		return ;
    	}
    	
    	// EXAMINE VEILED
    	mHandler.post( new ExamineResultRunnable( resultType, 400 ) )  ;
    }
    
    private class ExamineResultRunnable implements Runnable {
    	
    	int mResultType ;
    	long mRepost ;
    	boolean mSoundPlayed ;
    	
    	private ExamineResultRunnable( int resultType, long repostMillis ) {
    		mResultType = resultType ;
    		mRepost = repostMillis ;
    		mSoundPlayed = false ;
    	}
    	
    	@Override
		public void run() {
    		try {
				if ( !gameView.veiled() )
					gameView.veil() ;
				
				if ( !mSoundPlayed ) {
			    	// play sound!
			    	if ( mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER )
			    		soundPool.gameWin() ;
			    	else if ( mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER || mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
			    		soundPool.gameLoss() ;
			    	else {
			    		if ( gameService.getPlayerWon() )
			    			soundPool.gameWin() ;
			    		else if ( gameService.getPlayerLost() )
			    			soundPool.gameLoss() ;
			    	}
			    	
			    	mSoundPlayed = true ;
				}
				
				if ( !gameView.veiledFully() && gameView.running() ) {
					mHandler.postDelayed(this, mRepost) ;
					return ;
				}
				
				// If examining an MP game over, recycle the game view.
				if ( mResultType == GameUserInterface.RESULT_MULTI_PLAYER_GAME_OVER ) {
					gameView.recycle(true) ;
				}
				
				// Get the GameResult from the Service.  All examinations
		    	// require at least the most current game result; some require
		    	// a "checkpoint" game result as well.
		    	GameResult grCurrent = gameService.getGameResult() ;
		    	GameResult [] grArray ;
		    	
		    	// For now, that's all we need.  In a progression game, though,
		    	// we also use the checkpoint GameResult.
		    	if ( mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER ) {
		    		grArray = new GameResult[] {
		    				gameService.getCheckpointResult(),
		    				grCurrent
		    		} ;
		    	}
		    	else
		    		grArray = new GameResult[] {
		    			grCurrent 
		    			} ;
		    	
		    	// Start up an examination intent.
		    	Intent intent = new Intent( GameActivity.this, ExamineGameResultActivity.class ) ;
		    	intent.setAction( Intent.ACTION_MAIN ) ;
		    	if ( mGameViewMemoryCapabilities.getRecycleToVeil(  ) ) // very low-memory; don't display this in the background.
		    		intent.putExtra(ExamineGameResultActivity.INTENT_EXTRA_FULLSCREEN, true) ;
		    		
		    	if ( mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER )
			    	intent.putExtra(
			    			ExamineGameResultActivity.INTENT_EXTRA_STYLE,
			    			ExamineGameResultActivity.STYLE_SINGLE_PLAYER_GAME_OVER) ;
		    	else if ( mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER )
		    		intent.putExtra(
			    			ExamineGameResultActivity.INTENT_EXTRA_STYLE,
			    			ExamineGameResultActivity.STYLE_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER) ;
		    	else if ( mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
		    		intent.putExtra(
			    			ExamineGameResultActivity.INTENT_EXTRA_STYLE,
			    			ExamineGameResultActivity.STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER) ;
		    	else
		    		intent.putExtra(
			    			ExamineGameResultActivity.INTENT_EXTRA_STYLE,
			    			ExamineGameResultActivity.STYLE_MULTI_PLAYER_GAME_OVER) ;
		    	
		    	
		    	
		    	intent.putExtra(
		    			ExamineGameResultActivity.INTENT_EXTRA_GAME_RESULT_ARRAY,
		    			grArray) ;
		    	
		    	intent.putExtra(
		    			ExamineGameResultActivity.INTENT_EXTRA_GAME_SETTINGS,
		    			gameService.getGameSettings()) ;
		    	
		    	mExaminingResult = true ;
		    	startActivityForResult(intent, IntentForResult.REQUEST_EXAMINE_GAME_RESULT) ;
		    
		    	if ( QuantroPreferences.getAnalyticsActive(GameActivity.this) )
					Analytics.logInGameExamineResult(gip.gameSettings.getMode(),
							mResultType == GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER
							|| mResultType == GameUserInterface.RESULT_MULTI_PLAYER_GAME_OVER ) ;
    		} catch ( Exception e ) {
    			e.printStackTrace() ;
    			finish() ;
    		}
    	}
    }
    
    
    
    /////////////////////////////////////////////////////////////////////////
    //
    // GAME CONTROL METHODS
    //
    // Certain game components (like, say, the client communications, or an
    // overlayed menu, or the Game object itself) need to be able to adjust
    // the current "game state", in the sense of Paused, Waiting, Won, Lost,
    // etc.
    //
    // These methods allow the GameActivity to act as a Controller (more like
    // a Broadcaster, but whatever) for altering the game state in these ways.
    //
    //////////////////////////////////////////////////////////////////////////
    
    

    
	/////////////////////////////////////////////////////////////
	// CONTROLS TO ACTIONS ADAPTER DELEGATE
    //
	
	/**
	 * The user has pressed the "opponent" button.  Probably should
	 * show them the opponent's game!
	 */
	public void ctaad_opponentButtonPressed() {
		// TODO: Revise for fancier opponent buttons, e.g. with > 2 players.
		gameView.setDisplayOpponentGame(true) ;
	}
	
	/**
	 * The user has released the "opponent" button.  Probably show
	 * their own game again!
	 */
	public void ctaad_opponentButtonReleased() {
		if ( gameService != null && gameService.getPlayerLost() && gameService.getPlayerSpectating() )
			return ;	// don't "undisplay" opponent
		gameView.setDisplayOpponentGame(false) ;
	}
	
	/**
	 * The user has pressed the "score" button.  Probably should
	 * show them a detailed score display!
	 * 
	 * We can't handle this directly via our connection to an
	 * ActionsAdapter; whatever View is displaying the game
	 * should also be updated.
	 */
	public void ctaad_scoreButtonPressed() {
		// TODO: display score
	}
	
	/**
	 * The user has released the "score" button.  Probably should stop showing them
	 * a detailed score display!
	 * 
	 * We can't handle this directly via our connection to an ActionsAdapter;
	 * whatever View is displaying the game should also be updated.
	 */
	public void ctaad_scoreButtonReleased() {
		// TODO: stop displaying score
	}
    
	/////////////////////////////////////////////////////////////
	// GAME USER INTERFACE
    //
	
	
	
	/**
	 * We are requesting a ticket in preparation for a match request.
	 */
	public void gui_matchmakingRequestingTicket() {
		Log.d(TAG, "gui_matchmakingRequestingTicket") ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRequestTicket(true) ;
	}
	
	
	/**
	 * We received no response from the server when requesting a ticket.
	 * We're going to keep trying, but you may want to let the user know.
	 */
	public void gui_matchmakingNoTicketNoResponse() {
		Log.d(TAG, "gui_matchmakingNoTicketNoResponse") ;
		dialogManager.showDialog( DIALOG_MATCHMAKING_NO_RESPONSE_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingNoTicketNoResponse(true) ;
	}
	
	
	/**
	 * The lobby is closed.
	 */
	public void gui_matchmakingGameClosed() {
		Log.d(TAG, "gui_matchmakingRejectedInvalidNonce") ;
		dialogManager.showDialog( DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRejectedNonceInvalid(true) ;
	}
	
	
	/**
	 * We are requesting a match from the matchmaker.
	 */
	public void gui_matchmakingRequestingMatch() {
		Log.d(TAG, "gui_matchmakingRequestingMatch") ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRequest(true) ;
	}

	
	/**
	 * We received a match promise from the matchmaker.
	 */
	public void gui_matchmakingReceivedPromise() {
		Log.d(TAG, "gui_matchmakingReceivedPromise") ;
		dialogManager.dismissDialogAtTier(DIALOG_TIER_MATCHMAKING_PROBLEMS) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingPromise(true) ;
	}
	
	/**
	 * We successfully matched and are now connected (probably)
	 * to our match!
	 */
	public void gui_matchmakingReceivedMatch() {
		Log.d(TAG, "gui_matchmakingReceivedMatch") ;
		dialogManager.dismissDialogAtTier(DIALOG_TIER_MATCHMAKING_PROBLEMS) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingMatch(true) ;
	}
	
	/**
	 * We successfully matched and are now connected (probably)
	 * to our match!
	 */
	public void gui_matchmakingComplete() {
		Log.d(TAG, "gui_matchmakingComplete") ;
		dialogManager.dismissDialogAtTier(DIALOG_TIER_MATCHMAKING_PROBLEMS) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingUDPHolePunchSucceeded(true) ;
	}
	
	
	
	/**
	 * We have failed to communicate with the matchmaker.  Because this command is quite
	 * serious, it is unlikely to be called unless numerous connection attempts have
	 * failed, or we have remained in a "disconnected" state for a long period of time.
	 */
	public void gui_matchmakingNoResponse() {
		Log.d(TAG, "gui_matchmakingNoResponse") ;
		dialogManager.showDialog( DIALOG_MATCHMAKING_NO_RESPONSE_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingNoResponse(true) ;
	}
	
	/**
	 * Rejected by the Matchmaker; it is too busy.  Distinct from "NoResponse"
	 * in that we DID receive information from the matchmaker, we just shouldn't expect
	 * to be matched.
	 */
	public void gui_matchmakingRejectedFull() {
		Log.d(TAG, "gui_matchmakingRejectedFull") ;
//		dialogManager.showDialog( DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRejectedTooBusy(true) ;
	}
	
	/**
	 * Rejected by the Matchmaker; our Nonce is invalid.  This is not an error 
	 * we can recover from.
	 */
	public void gui_matchmakingRejectedInvalidNonce() {
		Log.d(TAG, "gui_matchmakingRejectedInvalidNonce") ;
		dialogManager.showDialog( DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRejectedNonceInvalid(true) ;
	}
	
	
	/**
	 * This is a strange error.  Just display a message, stop connected,
	 * and be done with it.
	 */
	public void gui_matchmakingRejectedNonceInUse() {
		Log.d(TAG, "gui_matchmakingRejectedNonceInUse") ;
		dialogManager.showDialog( DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRejectedNonceInUse(true) ;
	}
	
	/**
	 * The Matchmaker is refusing to match us, because our chosen communication
	 * port does not match the port from which our request was sent.  This means
	 * some form of port randomization or re-mapping is taking place on our end;
	 * it also means that UDP hole-punching will fail as currently implemented.
	 * 
	 * This is not completely unrecoverable.  For example, if the user can switch
	 * from a WiFi network to their data plan (or vice-versa) this problem might
	 * be resolved.
	 * 
	 */
	public void gui_matchmakingRejectedPortRandomization() {
		Log.d(TAG, "gui_matchmakingRejectedPortRandomization") ;
		dialogManager.showDialog( DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRejectedPortRandomization(true) ;
	}
	
	/**
	 * An unspecified error.
	 */
	public void gui_matchmakingRejectedUnspecified() {
		Log.d(TAG, "gui_matchmakingRejectedUnspecified") ;
		dialogManager.showDialog( DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID ) ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingRejectedUnspecified(true) ;
	}
	
	
	/**
	 * We received a match, but then were unable to form a connection with
	 * that match.
	 */
	public void gui_matchmakingFailed() {
		Log.d(TAG, "gui_matchmakingFailed") ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logMatchmakingUDPHolePunchFailed(true) ;
	}
	
	
	/**
	 * Hey, guess what? If we were having problems communicating with the
	 * server, everything's OK now. This is only a guaranteed call in the event
	 * that 1. Server communication seems OK, and 2. We previously called
	 * gui_peacerayServerCommunicationFailure()
	 */
	public void gui_peacerayServerCommunicationOK() {
		// this is a safe call, even if the specified dialog is not displayed.
		dialogManager.dismissDialog(DIALOG_GAME_WEB_COMMUNICATION_FAILURE);
	}

	/**
	 * We are having trouble communicating with the peaceray servers.
	 */
	public void gui_peacerayServerCommunicationFailure() {
		// put up a spinner? We don't want much happening
		// while this is going on...
		dialogManager.showDialog(DIALOG_GAME_WEB_COMMUNICATION_FAILURE);
	}

	/**
	 * According to the peaceray server(s), this lobby is closed.
	 */
	public void gui_peacerayServerLobbyClosed() {
		dialogManager.showDialog(DIALOG_GAME_WEB_CLOSED);
	}
	
    
	@Override
	public void gui_connectionToServerConnecting() {
		Log.d(TAG, "gui_connectionToServerConnectiong()") ;
		
		this.gameIsGoing = false ;
		gameViewLastPauseWasOnPause = false ;
		gameView.pause() ;
		
		// Now display the "openedConnection" dialog.
		//dialogManager.showDialog( DIALOG_CONNECTING_TO_SERVER_ID ) ;
		pauseViewComponentAdapter.setStateConnecting() ;
		runOnUiThread( pauseViewComponentAdapterSetVisibilityVisibleRunnable ) ;
		soundPool.pauseMusic() ;
	}

	@Override
	public void gui_connectionToServerFailedToConnect() {
		Log.d(TAG, "gui_connectionToServerFailedToConnect()") ;
		// TODO Fill in
		
	}

	@Override
	public void gui_connectionToServerGaveUpConnecting() {
		Log.d(TAG, "gui_connectionToServerGaveUpConnecting()" ) ;
		// TODO fill in
	}

	@Override
	public void gui_connectionToServerNegotiating() {
		Log.d(TAG, "gui_connectionToServerNegotiating()") ;
		this.gameIsGoing = false ;
		
		//dialogManager.showDialog( DIALOG_TALKING_TO_SERVER_ID ) ;
		pauseViewComponentAdapter.setStateTalking() ;
		runOnUiThread( pauseViewComponentAdapterSetVisibilityVisibleRunnable ) ;
		soundPool.pauseMusic() ;
	}

	@Override
	public void gui_connectionToServerConnected() {
		Log.d(TAG, "gui_connectionToServerConnected()") ;
		
		// Dismiss any network status dialogs.
		dialogManager.dismissDialogAtTier(DIALOG_TIER_NETWORK_STATUS) ;
	}

	@Override
	public void gui_connectionToServerDisconnectedUnexpectedly() {
		Log.d(TAG, "gui_connectionToServerDisconnectedUnexpectedly()") ;
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gui_connectionToServerKickedByServer(String msg) {
		Log.d(TAG, "gui_connectionToServerKickedByServer:" + msg) ;
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gui_connectionToServerServerClosedForever() {
		Log.d(TAG, "gui_connectionToServerServerClosedForever()") ;
		wasLeftByPlayer = 0 ;		// host is always slot 0.
		dialogManager.showDialog( DIALOG_OPPONENT_QUIT_MULTIPLAYER_ID ) ;
	}

	@Override
	public void gui_updateNumberOfPlayers(int numPlayers) {
		Log.d(TAG, "gui_updateNumberOfPlayers:" + numPlayers) ;
		pauseViewComponentAdapter.setNumberOfPlayers(numPlayers) ;
	}

	@Override
	public void gui_updatePlayerLocalSlot(int slotNumber) {
		// Warning: this method is sometimes called unnecessarily
		// (or, at least, redundantly).  For example, Progression games
		// will tend to call this several times; generally, once per
		// 'Resolved To Resume,' including the first go.  We don't make
		// any changes if this won't make any changes.
		
		if ( playerSlotLastGUIUpdate > -1 && playerSlotLastGUIUpdate == slotNumber ) {
			//Log.d(TAG, "gui_updatePlayerLocalSlot: provided slot number " + slotNumber + " matches last update; skipping") ;
			return ;
		}
		
		//Log.d(TAG, "gui_updatePlayerLocalSlot:" + slotNumber) ;
		
		// Tell the pause overlay...
		pauseViewComponentAdapter.setThisPlayer(slotNumber) ;
		
		// Hook the controls up...
		//Log.d(TAG, "gui_updatePlayerLocalSlot: attaching controls") ;
    	controlsToActionsAdapter = new ControlsToActionsAdapter() ;
        controlsToActionsAdapter.loadResources( getResources() ) ;
        controlsToActionsAdapter.setDelegate(this) ;
        
        // CONTROLS
        if ( QuantroPreferences.getControls(this) == QuantroPreferences.CONTROLS_GAMEPAD ) {
        	setControlsGamepad() ;
        } else {
        	setControlsGesture() ;
        }
        
        //Log.d(TAG, "gui_updatePlayerLocalSlot: setting delegate for controls") ;
        controlsGamepad.setDelegate(controlsToActionsAdapter) ;
        controlsGesture.setDelegate(controlsToActionsAdapter) ;
        controlsToActionsAdapter.setActionAdapter(gameService.actionAdapter[slotNumber]) ;
        
        // adjust the opponent slot and player slot.  We don't want to display our 
        // current game as the opponent game.  Only change opponent if we need to.
        if ( opponentSlot == slotNumber ) {
        	// if previous player slot lower than this, take --.
        	// otherwise, take ++.
        	if ( playerSlot < slotNumber )
        		opponentSlot-- ;
        	else
        		opponentSlot++ ;
        	opponentSlot %= numberOfPlayers ;
        }
        playerSlot = slotNumber ;
        playerSlotLastGUIUpdate = slotNumber ;
        
        gameView.setDisplayedGames(playerSlot, opponentSlot) ;
        //Log.d(TAG, "gui_updatePlayerLocalSlot: done touching gameView") ;
        playerSlot = slotNumber ;
	}

	@Override
	public void gui_updatePlayerNames(String[] names) {
		Log.d(TAG, "gui_updatePlayerNames") ;
		for ( int i = 0; i < names.length; i++ ) {
			playerName[i] = names[i] ;
			gameView.setPlayerName(i, names[i]) ;
		}
		pauseViewComponentAdapter.setPlayerNames(playerName) ;
	}
	
	@Override
	public void gui_updateHostName( String name ) {
		Log.d(TAG, "gui_updateHostName:" + name) ;
		// TODO: Something.
	}

	@Override
	public void gui_gameStateIsPaused(boolean[] pausedByPlayerSlot) {
		Log.d(TAG, "gui_gameStateIsPaused") ;
		
		gameIsGoing = false ;
		this.wasPausedByPlayers = pausedByPlayerSlot ;
    	
    	// First pause the GameView, so we stop updating the games
    	// and drawing animations.
    	gameViewLastPauseWasOnPause = false ;
    	gameView.pause() ;
    	
    	// Paused dialog
    	//dialogManager.showDialog( DIALOG_PAUSED_BY_PLAYERS_ID ) ;
    	
    	// Paused overlay
    	if ( !gameHasTicked && (pausedByPlayerSlot == null || numberOfPlayers == 1 ))
    		pauseViewComponentAdapter.setStateReady() ;
    	else
    		pauseViewComponentAdapter.setStatePaused(wasPausedByPlayers) ;
    	// put up the pause view if the pause contains someone other than
    	// local player.  If it contains local player, put up the game options
    	// fragment.  These are independent actions; they are configured to
    	// layer correctly even when both are activated.
    	// However, we DON'T put it up if we are paused by the demo expiring.
    	if ( activityState == ACTIVITY_STATE_RESUMED && gameOptionsFragmentUsed && ( wasPausedByPlayers != null && wasPausedByPlayers[playerSlot] && !gameService.isPausedByTimedDemoExpired()) ) {
    		if ( QuantroPreferences.getBackButtonBehavior(this) == QuantroPreferences.BACK_BUTTON_OPTIONS ) {
    			runOnUiThread( gameOptionsMenuFragmentSetVisibilityVisibleRunnable ) ;
    		}
    	}
    	
    	// We might only want to put up an overlay if this is a multiplayer game,
    	// but as a test, we always put it up.  
    	runOnUiThread( pauseViewComponentAdapterSetVisibilityVisibleRunnable ) ;
    	/*
    	boolean showOverlay = wasPausedByPlayers == null || wasPausedByPlayers.length > 1 ;
    	if ( showOverlay )
    		runOnUiThread( pauseViewComponentAdapterSetVisibilityVisibleRunnable ) ;
    		*/
    	
    	// pause music
    	soundPool.pauseMusic() ;
    	
    	if ( QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGamePause(gip.gameSettings.getMode(),
					pausedByPlayerSlot != null && pausedByPlayerSlot.length < playerSlot && pausedByPlayerSlot[playerSlot] ) ;
	}

	@Override
	public void gui_gameStateIsWaiting(boolean[] waitingForPlayerSlot) {
		Log.d(TAG, "gui_gameStateIsWaiting") ;
		
		gameIsGoing = false ;
		this.wasWaitingForPlayers = waitingForPlayerSlot ;
    	
    	// First pause the GameView, so we stop updating the games
    	// and drawing animations.
    	gameViewLastPauseWasOnPause = false ;
    	gameView.pause() ;
    	
    	//dialogManager.showDialog( DIALOG_WAITING_FOR_PLAYERS_ID ) ;
    	
    	// Paused overlay
    	pauseViewComponentAdapter.setStateWaiting(wasWaitingForPlayers) ;
    	runOnUiThread( pauseViewComponentAdapterSetVisibilityVisibleRunnable ) ;
    	
    	// pause music
    	soundPool.pauseMusic() ;
    	
    	// stub
	}

	@Override
	public void gui_gameStateIsReady() {
		Log.d(TAG, "gui_gameStateIsReady") ;
		
		// Ready!  Dismiss any network status dialogs.
		dialogManager.dismissDialogAtTier(DIALOG_TIER_NETWORK_STATUS) ;
		
		// Overlay!
		if ( numberOfPlayers == 1 )
			pauseViewComponentAdapter.setStateReady() ;
		else {
			if ( !gameIsGoing )
				soundPool.playGetReadySound() ;
			pauseViewComponentAdapter.setStateStarting() ;
		}
		
		gameIsGoing = true ;
	}

	@Override
	public void gui_gameStateIsOver(boolean[] playerWon) {
		Log.d(TAG, "gui_gameStateIsOver") ;
		
		// This call will be followed by gui_resolveResult.
	}
	
	@Override
	public void gui_resolveResult( int resultType ) {
		Log.d(TAG, "gui_resolveResult") ;
		
		// note the result type.  This is important for Progression: we may see
		// one result come in (level up) and then a few ticks later (obviously
		// before we get notified) a different result type (game over).
		mResult = resultType ;
		if ( !mResolvingResult && !mResultNeedsDisplay ) {
			// When a single player game is resolved, we know that our local
			// Game objects are up-to-date.  There is no need for a prolonged
			// "resolution" step.  However, in the interests of a simplified
			// design, we follow the same procedure.
			mResult = resultType ;
			mResolvingResult = true ;
			
			// The Service has already closed any communications it is
			// having with a server, or (at least) put things in a state
			// where the server is waiting for our go-ahead to continue.
			// We have some time.
			
			// We first attempt to minimize the changes that could happen
			// as we await resolution (for us, resolving this just means
			// allowing our local Game displays to catch up to the completed
			// state; for instance, lock-and-clear animations should play out).
			// Three things:
			// 	1 disable user controls that affect game state (controls
			//			for changing onscreen display are fine)
			// 	2 tell all action adapters that they should NOT tick
			//  3 tell the GameView that we want a notification upon games
			//			not changing in tick.
			//
			// Our GameView.Listener methods are called upon the game being
			// "resolved," and we continue from there.
			
			// 1: disable user controls.  Do this on UI thread; only the UI thread
			// can touch the control views.
			Log.d(TAG, "1: disabling user controls") ;
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					String [] gameButtonNames =
						controlsToActionsAdapter.getButtonNames(
								ControlsToActionsAdapter.BUTTON_TYPE_GAME) ;
					controlsGamepad.setEnabled(false, gameButtonNames) ;
					controlsGesture.setEnabled(false, gameButtonNames) ;
					controlsToActionsAdapter.setAllButtonsEnabled(false) ;
					controlsToActionsAdapter.releaseAllButtons() ;
				}
			}) ;
			
			
			// 2: tell action adapters not to tick forward
			// NOTE: this has already been handled for us in the GameService.
			Log.d(TAG, "2: (no effect)") ;
			
			// 3: tell the GameView to notify us upon the games not changing.
			Log.d(TAG, "3: requesting GameView notification") ;
			if ( !gameView.notifyWhenTickWithoutChange(this) ) {
				// either we are not "resumed", in which case we should
				// wait for resumption and examine the game result at that
				// point, or we ARE, in which case this is a fatal error
				// and we should quit.
				if ( activityState == ACTIVITY_STATE_RESUMED ) {
					Log.e(TAG, "When 'resumed', gameView refused to notify.") ;
					examineResult(mResult) ;
					mResolvingResult = false ;
				} else {
					mResultNeedsDisplay = true ;
					mResolvingResult = false ;
				}
			}
			
			Log.d(TAG, "done gui_resolveResult") ;
		}
	}
	
	@Override
    /**
     * Indicates that a resolution is over, and the GameUI
     * should revert any changes it made to resolve a result.
     * This method will be called no more than once after
     * gui_resolveResult, and there will never be more than
     * one resolveResult call in a row without a call to
     * continueAfterResolvingResult in between.  Additionally,
     * as of 12/29/11, the Activity will be able to predict
     * in advance when this call occurs, since it is a result
     * of a call to gameService.resolvedTo*().
     */
    public void gui_continueAfterResolvingResult() {
		Log.d(TAG, "gui_continueAfterResolvingResult") ;
		mResult = -1 ;
		mResolvingResult = false ;
		mResultNeedsDisplay = false ;
		
		// re-enable game buttons, tell GameView to refresh everything.
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				String [] gameButtonNames =
					controlsToActionsAdapter.getButtonNames(
							ControlsToActionsAdapter.BUTTON_TYPE_GAME) ;
				controlsGamepad.setEnabled(true, gameButtonNames) ;
				controlsGesture.setEnabled(true, gameButtonNames ) ;
				controlsToActionsAdapter.setAllButtonsEnabled(true) ;
				if ( gameView.veiled() )
					gameView.unveil() ;
				gameView.refresh() ;
			}
		}) ;
	}

	@Override
	public void gui_gameStatePlayerQuit(int playerSlot, boolean fatal) {
		Log.d(TAG, "gui_gameStatePlayerQuit") ;
		gameView.setPlayerStatus(playerSlot,
				gameService.getPlayerWon(playerSlot),
				gameService.getPlayerLost(playerSlot),
				gameService.getPlayerKicked(playerSlot),
				true,
				gameService.getPlayerSpectating(playerSlot)) ;
		
		if ( fatal ) {
			wasLeftByPlayer = playerSlot ;
			dialogManager.showDialog( DIALOG_OPPONENT_QUIT_MULTIPLAYER_ID ) ;
		} else if ( !gameService.getPlayerSpectating(playerSlot) ) {
			// toast?
			wasLeftByPlayer = playerSlot ;
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					Resources res = getResources() ;
					String toastText = res.getString(R.string.in_game_toast_opponent_quit_multiplayer) ;
					String placeholder = res.getString(R.string.placeholder_names_array_unformatted) ;
					toastText = toastText.replace(placeholder,
							playerName[wasLeftByPlayer] == null ? "Player " + wasLeftByPlayer : playerName[wasLeftByPlayer]) ;
					Toast.makeText(GameActivity.this, toastText, Toast.LENGTH_SHORT).show() ;
				}
			}) ;
			
		} else {
			// don't care if a spectating player quit
		}
	}
	
	@Override
	public void gui_gameStatePlayerWon(int playerSlot) {
		gameView.setPlayerStatus(playerSlot,
				true,
				gameService.getPlayerLost(playerSlot),
				gameService.getPlayerKicked(playerSlot),
				gameService.getPlayerQuit(playerSlot),
				gameService.getPlayerSpectating(playerSlot)) ;
	}
	
	@Override
	public void gui_gameStatePlayerLost(int playerSlot) {
		gameView.setPlayerStatus(playerSlot,
				gameService.getPlayerWon(playerSlot),
				true,
				gameService.getPlayerKicked(playerSlot),
				gameService.getPlayerQuit(playerSlot),
				gameService.getPlayerSpectating(playerSlot)) ;
	}
	
	@Override
	public void gui_gameStatePlayerIsSpectator(int playerSlot) {
		gameView.setPlayerStatus(playerSlot,
				gameService.getPlayerWon(playerSlot),
				gameService.getPlayerLost(playerSlot),
				gameService.getPlayerKicked(playerSlot),
				gameService.getPlayerQuit(playerSlot),
				true) ;
		// disable controls if we are this player.
		if ( playerSlot == this.playerSlot ) {
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					String [] buttons = controlsToActionsAdapter.getButtonNames(ControlsToActionsAdapter.BUTTON_TYPE_GAME) ;
					controlsGamepad.setEnabled(false, buttons) ;
					controlsGesture.setEnabled(false, buttons) ;
				}
			}) ;
		}
	}
	
	
	
	
	
	@Override
	public void gui_gameStatePlayerKicked( int playerSlot, String msg ) {
		Log.d(TAG, "gui_gameStatePlayerQuit") ;
		gameView.setPlayerStatus(playerSlot,
				gameService.getPlayerWon(playerSlot),
				gameService.getPlayerLost(playerSlot),
				true,
				gameService.getPlayerQuit(playerSlot),
				gameService.getPlayerSpectating(playerSlot)) ;
		
		pauseViewComponentAdapter.setPlayerKickWarning(false, playerSlot, 0) ;
		
		if ( !gameService.getPlayerSpectating(playerSlot) ) {
			// toast?
			wasLeftByPlayer = playerSlot ;
			runOnUiThread( new Runnable() {
				@Override
				public void run() {
					Resources res = getResources() ;
					String toastText = res.getString(R.string.in_game_toast_opponent_kicked_multiplayer) ;
					String placeholder = res.getString(R.string.placeholder_names_array_unformatted) ;
					toastText = toastText.replace(placeholder,
							playerName[wasLeftByPlayer] == null ? "Player " + wasLeftByPlayer : playerName[wasLeftByPlayer]) ;
					Toast.makeText(GameActivity.this, toastText, Toast.LENGTH_SHORT).show() ;
				}
			}) ;
			
		} else {
			// don't care if a spectating player was kicked
		}
	}
	
	

    /**
     * The specified player has been warned of an upcoming kick.
     * @param playerSlot
     * @param txt
     * @param kickAtTime
     */
    public void gui_gameStatePlayerKickWarning( int playerSlot, String txt, long kickAtTime ) {
    	pauseViewComponentAdapter.setPlayerKickWarning(true, playerSlot, kickAtTime) ;
    }
    
    
    /**
     * The specified player has had their kick warning retracted.
     * 
     * @param playerSlot
     */
    public void gui_gameStatePlayerKickWarningRetraction( int playerSlot ) {
    	pauseViewComponentAdapter.setPlayerKickWarning(false, playerSlot, 0) ;
    }
    
    
	
	private long mAnalyticsPauseTime = Long.MAX_VALUE ;
	private boolean mAnalyticsPauseWasUserAction ;

	@Override
	public void gui_toggleAdvancingGames(boolean shouldAdvance) {
		Log.d(TAG, "gui_toggleAdvancingGames: " + shouldAdvance) ;
		
		if ( shouldAdvance ) {
			// achievements!
			Achievements.game_go() ;
			
	    	// Immediately start the game.
	    	gameView.unpause() ;
	    	runOnUiThread( pauseViewComponentAdapterSetVisibilityGoneRunnable ) ;
	    	soundPool.playMusic() ;
	    	gameHasTicked = true ;
	    	
	    	if ( QuantroPreferences.getAnalyticsActive(this) )
				Analytics.logInGameUnpause(gip.gameSettings.getMode(),
						System.currentTimeMillis() - mAnalyticsPauseTime,
						false,
						mAnalyticsPauseWasUserAction) ;
		}
		else {
			// achievements!
			Achievements.game_stop() ;
			
			gameViewLastPauseWasOnPause = false ;
			gameView.pause() ;
			
			mAnalyticsPauseWasUserAction = wasPausedByPlayers != null && wasPausedByPlayers.length < playerSlot && wasPausedByPlayers[playerSlot] ;
	    	mAnalyticsPauseTime = System.currentTimeMillis() ;
	    	
			if ( QuantroPreferences.getAnalyticsActive(this) )
				Analytics.logInGamePause(gip.gameSettings.getMode(), mAnalyticsPauseWasUserAction) ;
						
		}
	}
	
	
	public Bitmap gui_getGameThumbnail() {
		return thumbnail ;
	}

    public void gui_didSaveGame() {
    	timeLastSaved = System.currentTimeMillis() ;
		if ( timeLastSaved - timeLastThumbnailUpdate < SAVED_GAME_THUMBNAIL_LEEWAY && thumbnail != null ) {
			new GameSaver()
					.start(this, null, null)
					.saveGameThumbnail(gip.saveToKey, thumbnail)
					.stop() ;
		}
    }
	
	
	
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	// WIFI MONITOR CALLBACK
	////////////////////////////////////////////////////////////////////////////
	
	@Override
	synchronized public void wml_hasWifiIpAddress(boolean hasIP, int ip) {
		if ( !gip.isMediatedChallenge() && !gip.isMatchseeker() && gip.isServer() ) {
			// an unmediated host.  Check that ip hasn't change.
			if ( hasIP && ip != mHostIP ) {
				// WHOOPS!  IT'S CHANGED!
				// TODO: DO SOMETHING HERE!  No one will be able to connect anymore.
			}
		}
		// oh look, an update.  Put up a dialog, or don't.
		if ( hasIP && mNoWifiDialogDisplayed ) {
			dialogManager.dismissDialog(DIALOG_NO_WIFI_CONNECTION) ;
			mNoWifiDialogDisplayed = false ;
		}
		else if ( !hasIP && !mNoWifiDialogDisplayed ) {
			dialogManager.showDialog(DIALOG_NO_WIFI_CONNECTION) ;
			mNoWifiDialogDisplayed = true ;
		}
		
 		if ( activityShown )
			mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_UPDATE_FREQUENCY) ;
	}

	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// GAME VIEW CHANGE LISTENER METHODS
	//
	// When the game view updates the thumbnail, we save it.  When the background
	// shuffles, we store it.
	//
	////////////////////////////////////////////////////////////////////////////
	
	@Override
	synchronized public void gvcl_thumbnailUpdated( Bitmap thumbnail ) {
		//Log.d(TAG, "gvts_thumbnailUpdated") ;
		// If we have recently saved, now is a good time to
		// update the thumbnail.
		timeLastThumbnailUpdate = System.currentTimeMillis() ;
		if ( timeLastThumbnailUpdate - timeLastSaved < SAVED_GAME_THUMBNAIL_LEEWAY ) {
			new GameSaver()
					.start(this, null, null)
					.saveGameThumbnail(gip.saveToKey, thumbnail)
					.stop() ;
		}
	}
	
	@Override
	synchronized public void gvcl_backgroundChanged( Background bg ) {
		// Store this background as our new "starting shuffle background."
		// It's also important to notify the MenuFragment.
		if ( !Background.equals(bg, mBackground) && bg != null ) {
			mBackground = bg ;
			QuantroPreferences.setBackgroundCurrent(this, bg) ;
			if ( gameOptionsMenuFragment != null )
				gameOptionsMenuFragment.setCurrentBackground(bg) ;
		}
	}
	
	synchronized public void gvcl_controlsUpdated( Drawable thumbnail ) {
		controlsThumbnail = thumbnail ;
		if ( gameOptionsMenuFragment != null )
			gameOptionsMenuFragment.setControlsThumbnail(controlsThumbnail) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// GAME VIEW LISTENER METHODS
	//
	// When we start resolving a game result, we tell the GameView to let us know
	// the first time it 'ticks' and no games change.
	//
	////////////////////////////////////////////////////////////////////////////
	
	synchronized public void gvl_tickedGamesWithoutChange() {
		Log.d(TAG, "gvl_tickedGamesWithoutChange") ;
		if ( activityState == ACTIVITY_STATE_RESUMED ) {
			examineResult(mResult) ;
			mResolvingResult = false ;
		}
		else
			mResultNeedsDisplay = true ;
	}
	
	synchronized public void gvl_canceled() {
		Log.d(TAG, "gvl_canceled") ;
		// Huh?  Let's just assume that we should put up the result.
		if ( activityState == ACTIVITY_STATE_RESUMED ) {
			examineResult(mResult) ;
			mResolvingResult = false ;
		}
		else
			mResultNeedsDisplay = true ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// VIEW. ON CLICK LISTENER METHODS
	//
	// We try not to directly interact with views.  Primarily this is here to
	// allow "tap-to-unpause."
	//
	////////////////////////////////////////////////////////////////////////////


	@Override
	synchronized public void onClick(View v) {
		Log.d(TAG, "onClick " + v ) ;
		if ( v == pauseOverlay && this.gameService != null && gameService.isPausedByPlayer() ) {
			// Whelp, the user wants to unpause.  Do we let them?
			// That question is answered within the following method.
			// userActionTogglePause() ;
			userActionPauseOff() ;
		}
	}
    
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// USER GUI ACTIONS
	//
	// Easy methods to allow certain user actions.
	//
	////////////////////////////////////////////////////////////////////////////
	
	synchronized private void userActionTogglePause() {
		if ( gameService != null ) {
			if ( gameService.isPausedByPlayer() && gameView.ready() ) {
				Log.d(TAG, "toggle pause: UNPAUSE") ;
				soundPool.playResumeSound() ;
				gameService.unpauseByPlayer() ;
				if ( pauseViewComponentAdapter != null )
    				pauseViewComponentAdapter.setLocalPause(false) ;
			}
			else if ( !gameService.isPausedByPlayer() ) {
				Log.d(TAG, "toggle pause: PAUSE") ;
				soundPool.playPauseSound() ;
				gameService.pauseByPlayer() ;
				if ( pauseViewComponentAdapter != null )
    				pauseViewComponentAdapter.setLocalPause(true) ;
			}
		}
	}
	
	synchronized private void userActionPauseOn() {
		if ( gameService != null ) {
			if ( !gameService.isPausedByPlayer() ) {
				Log.d(TAG, "toggle pause: PAUSE") ;
				if ( soundPool != null )
					soundPool.playPauseSound() ;
				gameService.pauseByPlayer() ;
				if ( pauseViewComponentAdapter != null )
    				pauseViewComponentAdapter.setLocalPause(true) ;
			}
		}
	}
	
	synchronized private void userActionPauseOff() {
		if ( gameService != null ) {
			if ( gameService.isPausedByPlayer() && gameView.ready() ) {
				Log.d(TAG, "toggle pause: UNPAUSE") ;
				if ( soundPool != null )
					soundPool.playResumeSound() ;
				gameService.unpauseByPlayer() ;
				if ( pauseViewComponentAdapter != null )
    				pauseViewComponentAdapter.setLocalPause(false) ;
			}
		}
	}
	
	
	/**
	 * The user wants to toggle the "mute" setting.
	 */
	synchronized private void userActionToggleMute() {
		if ( soundPool.isMuted() )
			userActionUnmute() ;
		else
			userActionMute() ;
	}
	
	
	synchronized private void userActionMute() {
		soundPool.mute() ;
		QuantroPreferences.setMuted(this, true) ;
		Toast.makeText(this,
				R.string.setting_toast_sound_muted,
				Toast.LENGTH_SHORT).show();
		pauseViewComponentAdapter.setSoundOn( !soundPool.isMuted(), soundPool.isMutedByRinger() ) ;
	}
	
	synchronized private void userActionUnmute() {
		soundPool.unmute() ;
		QuantroPreferences.setMuted(this, false) ;
		pauseViewComponentAdapter.setSoundOn( !soundPool.isMuted(), soundPool.isMutedByRinger() ) ;
	}
	
	
	synchronized private void userActionLaunchSettings() {
		// User is opening settings!  Silence music, pause,
		// open settings menu.  Note that this will happen automagically
		// if we just open up the intent.
		Intent intent = new Intent( this, QuantroPreferences.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	startActivity(intent) ;
	}
	
	
	synchronized private void userActionLaunchGameOptions() {
		// User is opening settings!  Silence music, pause,
		// open settings menu.  Note that this will happen automagically
		// if we just open up the intent.
		Intent intent = new Intent( this, GameOptionsMenuActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	intent.putExtra( GameOptionsMenuActivity.INTENT_EXTRA_CURRENT_MUSIC, Music.toStringEncoding(mMusic) ) ;
    	intent.putExtra( GameOptionsMenuActivity.INTENT_EXTRA_GAME_MODE, gip.gameSettings.getMode() ) ;
    	this.startActivityForResult(intent, IntentForResult.GAME_OPTIONS_MENU) ;
	}

	
	////////////////////////////////////////////////////////////////////////////
	//
	// GAME OPTIONS MENU FRAGMENT
	//

	@Override
	synchronized public void gomfl_onAttach(GameOptionsMenuFragment fragment) {
		gameOptionsMenuFragment = fragment ;
		
		// this causes some null pointer exceptions... for unknown reasons.
		// maybe it was called before onCreate() was finished (before this method
		// was synchronized).
		if ( activityState >= ACTIVITY_STATE_CREATED ) {
			try {
				int gameMode = gip.gameSettings.getMode() ;
				
				// necessary updates
				gameOptionsMenuFragment.setGameMode(gameMode) ;
				gameOptionsMenuFragment.setCurrentBackground(mBackground) ;
				gameOptionsMenuFragment.setCurrentMusic(mMusic) ;
				gameOptionsMenuFragment.setControlsThumbnail(controlsThumbnail) ;
			} catch ( NullPointerException npe ) {
				Log.d(TAG, "Prevented a NullPointerException in gomfl_onAttach") ;
			}
			
			if ( gameOptionsViewShown )
				gameOptionsMenuFragment.show() ;
			else
				gameOptionsMenuFragment.dismiss() ;
		}
	}


	@Override
	public void gomfl_setCurrentSkinAndColors(Skin skin, ColorScheme colorScheme) {
		if ( getPremiumLibrary().has(skin) ) {
			mSkin = skin ;
			mColorScheme = colorScheme ;
			if ( gameView != null )
				gameView.setSkinAndColors( skin, colorScheme ) ;
		}
	}


	@Override
	public void gomfl_setCurrentBackground(Background background) {
		if ( getPremiumLibrary().has(background) ) { 
			mBackground = background ;
			if ( gameView != null )
				gameView.setBackgrounds( background, null ) ;
		}
	}
	
	@Override
	public void gomfl_setBackgroundsInShuffle( Collection<Background> backgrounds ) {
		if ( mBackgroundsInShuffle != null ) {
			mBackgroundsInShuffle.clear() ;
			for ( Background bg : backgrounds ) {
				if ( getPremiumLibrary().has(bg) ) {
					mBackgroundsInShuffle.add(bg) ;
				}
			}
			// always have at least none...
			if ( mBackgroundsInShuffle.isEmpty() )
				mBackgroundsInShuffle.add(Background.get(Background.Template.NONE, Background.Shade.BLACK)) ;
		}
		
		if ( gameView != null && mBackgroundShuffles )
			gameView.setBackgrounds(null, mBackgroundsInShuffle) ;
	}
	
	@Override
	public void gomfl_setBackgroundShuffles( boolean shuffles ) {
		mBackgroundShuffles = shuffles ;
		if ( gameView != null ) {
			if ( mBackgroundShuffles )
				gameView.setBackgrounds(null, mBackgroundsInShuffle) ;
			gameView.setShuffleParameters(
	    			mBackgroundShuffles,
	    			res.getInteger(R.integer.background_shuffle_min_gap),
	    			res.getInteger(R.integer.background_shuffle_max_gap),
	    			true) ;
		}
	}
	
	@Override
	public void gomfl_setCurrentMusic(Music music) {
		// Set the current music track.
		// First check that we own this music.
		boolean owned = getPremiumLibrary().has(music) ;
		if ( owned && !Music.equals(music, mMusic) ) {
			mMusic = music ;
			if ( soundPool != null )
				soundPool.loadMusic(this, music) ;
		}
	}
	
	@Override
	public void gomfl_setCurrentControlsGamepad() {
		if ( activityState >= ACTIVITY_STATE_CREATED ) {
			setControlsGamepad() ;
		}
	}
	
	@Override
	public void gomfl_setCurrentControlsGesture() {
		if ( activityState >= ACTIVITY_STATE_CREATED ) {
			setControlsGesture() ;
		}
	}


	@Override
	public void gomfl_optionsMenuFragmentDismissed() {
		// unpause.
		if ( activityState >= ACTIVITY_STATE_CREATED ) {
			if ( gameOptionsViewShown )
				userActionPauseOff() ;
			runOnUiThread( gameOptionsMenuFragmentSetVisibilityGoneRunnable ) ;
		}
	}


	@Override
	public void gomfl_quit() {
		// Dismiss the menu and quit.
		if ( activityState >= ACTIVITY_STATE_CREATED ) {
			runOnUiThread(gameOptionsMenuFragmentSetVisibilityGoneRunnable) ;
			quitGame(true) ;
		}
	}
    
}