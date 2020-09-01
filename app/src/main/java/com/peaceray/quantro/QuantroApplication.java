package com.peaceray.quantro;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.database.CustomGameModeSettingsDatabaseAdapter;
import com.peaceray.quantro.keys.KeyRing;
import com.peaceray.quantro.main.GameService;
import com.peaceray.quantro.main.LobbyService;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.premium.PremiumLibrary;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.utils.AssetAccessor;
import com.peaceray.quantro.utils.Scores;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.GameView;
import com.peaceray.quantro.view.game.GameViewMemoryCapabilities;
import com.peaceray.quantro.view.game.blocks.BlockDrawer;
import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class QuantroApplication extends Application {
	private final static String TAG = "QuantroApplication" ;
	
	
	private final static String PREFERENCE_FIRST_LAUNCH_CODE = "com.peaceray.quantro.QuantroApplication.PREFERENCE_FIRST_LAUNCH_CODE" ;
	private final static String PREFERENCE_GAME_HELPER_AUTOSTART = "com.peaceray.quantro.QuantroApplication.PREFERENCE_GAME_HELPER_AUTOSTART" ;
	

	private Activity activityInFront = null ;
	private WeakReference<LobbyService> mwrLobbyService = new WeakReference<LobbyService>(null) ;
	private WeakReference<GameService> mwrGameService = new WeakReference<GameService>(null) ;
	
	public Handler handler ;
	
	public Nonce personalNonce ;
	
	private QuantroSoundPool mSoundPool ;
	private int mActivityStackSize = 0 ;
	private int mActivityStackSizeNonGame = 0 ;
	private ScreenReceiver mScreenReceiver ;
	
	private GameViewMemoryCapabilities mGameViewMemoryCapabilities ;
	private GameViewMemoryCapabilities mGameViewMemoryCapabilitiesPrioritizeFrameRate ;
	private GameViewMemoryCapabilities mGameViewMemoryCapabilitiesPrioritizeFeatures ;
	
	private BlockDrawerPreallocatedBitmaps mBlockDrawerPreallocatedBitmaps ;
	private Bitmap mBlockDrawerLoadIntoSheetBitmap ;
	private Bitmap mBlockDrawerLoadIntoBackgroundBitmap ;
	
	public boolean mSupportsFeatureWifi ;
	public boolean mSupportsFeatureTelephony ;
	
	public SoftReference<Bitmap> msrMenuBackgroundBitmap = new SoftReference<Bitmap>(null) ;
	public Background mMenuBackground = null ;
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate") ;
		super.onCreate() ;
		handler = new Handler() ;
		personalNonce = new Nonce() ;
		
		mSoundPool = null ;
		mScreenReceiver = new ScreenReceiver() ;
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, filter);
		
		mGameViewMemoryCapabilities = null ;		// initialized the first time an activity shows up.
		
		// what features do we support?
		PackageManager pm = getPackageManager() ;
		FeatureInfo [] features = pm.getSystemAvailableFeatures() ;		// requires API 5!!!
		mSupportsFeatureWifi = false ;
		mSupportsFeatureTelephony = false ;
		for ( int i = 0; i < features.length; i++ ) {
			if ( "android.hardware.telephony".equals(features[i].name) )
				mSupportsFeatureTelephony = true ;
			else if ( "android.hardware.wifi".equals(features[i].name) )
				mSupportsFeatureWifi = true ;
		}
		 
		// Now's a good time to load our custom game settings.
		// Many parts of the code assume these are in place.
		// Any part that makes a change is responsible for updating
		// GameModes.
		GameModes.setCustomGameModeSettings(
				CustomGameModeSettingsDatabaseAdapter.getAll(this) ) ;
		 
		if ( QuantroPreferences.getPrivateSettingInt(this, PREFERENCE_FIRST_LAUNCH_CODE, -1) == -1 )
			QuantroPreferences.setPrivateSettingInt(
					this, PREFERENCE_FIRST_LAUNCH_CODE, AppVersion.code(this)) ;
		
		mBlockDrawerPreallocatedBitmaps = null ;
		mBlockDrawerLoadIntoSheetBitmap = null ;
		mBlockDrawerLoadIntoBackgroundBitmap = null ;
		 
		// init Scores and Achievements
		Scores.init(this) ;
		Achievements.init(this) ;
		
		// Memory Leak Fix:  There are a few memory leaks currently happening in
		// Quantro, with various causes.  One culprit is URLConnection, which caches
		// a large portion of (presumably) the application's JAR over the lifetime of
		// the app.  Here's a fix, which does have performance side effects (it
		// prevents ALL cacheing, not just the problem areas).  Google is aware
		// of this problem from other channels but no fix yet exists to my knowledge.
		// For more detail, see:
		// http://stackoverflow.com/questions/14610350/android-memory-leak-in-apache-harmonys-jarurlconnectionimpl
		// https://code.google.com/p/android/issues/detail?id=60071
		/*
		try {
			new URLConnection( new URL("http://www.peaceray.com") ) {
				public void connect() throws IOException { }
				public void setNoCache() { setDefaultUseCaches(false) ; }
			}.setNoCache() ;
		} catch ( Exception e ) { e.printStackTrace() ; } 
		*/
	}
	
	@Override
	public void onTerminate() {
		// This method is never going to be called on a real device.
		// But who gives a crap?
		Scores.destroy() ;
		super.onTerminate();
	}
	
	
	synchronized public void setLobbyService( LobbyService ls ) {
		mwrLobbyService = new WeakReference<LobbyService>(ls) ;
	}
	
	synchronized public void unsetLobbyService( LobbyService ls ) {
		LobbyService service = mwrLobbyService.get() ;
		if ( service == ls )
			mwrLobbyService = new WeakReference<LobbyService>(null) ;
	}
	
	synchronized public LobbyService getLobbyService() {
		return mwrLobbyService.get() ;
	}
	
	synchronized public void setGameService( GameService gs ) {
		mwrGameService = new WeakReference<GameService>(gs) ;
	}
	
	synchronized public void unsetGameService( GameService gs ) {
		GameService service = mwrGameService.get() ;
		if ( service == gs )
			mwrGameService = new WeakReference<GameService>(null) ;
	}
	
	synchronized public GameService getGameService() {
		return mwrGameService.get() ;
	}
	
	/**
	 * Returns a PremiumLibrary instance.  You should call this
	 * in your Activity's onStart() method if any functions rely on
	 * premium content.
	 * 
	 * @return
	 */
	synchronized public PremiumLibrary getPremiumLibrary() {
		return new PremiumLibrary( KeyRing.loadNewKeyRing(this) ) ;
	}
	
	
	/**
	 * Returns a GameViewMemoryCapabilities instance.  Will likely be the
	 * same instance every time.
	 * 
	 * @param a
	 * @return
	 */
	synchronized public GameViewMemoryCapabilities getGameViewMemoryCapabilities( Activity a ) {
		if ( mGameViewMemoryCapabilities == null ) {
			mGameViewMemoryCapabilitiesPrioritizeFrameRate = GameViewMemoryCapabilities.newInstance( a, true, true ) ;
			mGameViewMemoryCapabilitiesPrioritizeFeatures = GameViewMemoryCapabilities.newInstance( a, false, false ) ;
			
			mGameViewMemoryCapabilities = QuantroPreferences.getGraphicsPrioritizeFrameRate(a)
					? mGameViewMemoryCapabilitiesPrioritizeFrameRate
					: mGameViewMemoryCapabilitiesPrioritizeFeatures ;
			
			Log.d(TAG, "framerate: " + mGameViewMemoryCapabilitiesPrioritizeFrameRate) ;
			Log.d(TAG, "features : " + mGameViewMemoryCapabilitiesPrioritizeFeatures) ;
			Log.d(TAG, mGameViewMemoryCapabilities.toString()) ;
			
			
			if ( QuantroPreferences.getAnalyticsActive(a) )
				Analytics.logGameViewMemoryCapabilities(a, mGameViewMemoryCapabilities) ;
		}
		return mGameViewMemoryCapabilities ;
	}
	
	
	/**
	 * Is it possible to change our GVMC - and thus our graphical
	 * detail, our in-game features, etc. - by altering priorities
	 * and constructions parameters?
	 * 
	 * Generally speaking, only very high-end devices (those with
	 * lots of application RAM) will return 'false' to this.
	 * 
	 * In general, if this method returns 'true,' there is a necessary
	 * trade-off between [Frame Rate] and [Features / Graphical Quality].
	 * If this method returns 'false,' we have enough resources to
	 * max out both at the same time.
	 * @param a
	 * @return
	 */
	synchronized public boolean getGameViewMemoryCapabilitiesCanChangeWithPriority( Activity a ) {
		if ( mGameViewMemoryCapabilitiesPrioritizeFrameRate == null || mGameViewMemoryCapabilitiesPrioritizeFeatures == null ) {
			getGameViewMemoryCapabilities(a) ;
		}

		GameViewMemoryCapabilities g1 = mGameViewMemoryCapabilitiesPrioritizeFrameRate;
		GameViewMemoryCapabilities g2 = mGameViewMemoryCapabilitiesPrioritizeFeatures;

		return g1.getBlit() != g2.getBlit()
				|| g1.getGameOverlaySupported() != g2.getGameOverlaySupported()
				|| g1.getShuffleSupported() != g2.getShuffleSupported()
				|| g1.getRecycleToVeil() != g2.getRecycleToVeil();
	}
	
	synchronized public BlockDrawerPreallocatedBitmaps getBlockDrawerPreallocatedBitmaps( Activity a ) {
		if ( mBlockDrawerPreallocatedBitmaps == null && a != null ) {
			mBlockDrawerPreallocatedBitmaps = createBlockDrawerPreallocatedBitmaps(a) ;
		}
		return mBlockDrawerPreallocatedBitmaps ;
	}
	
	synchronized public Bitmap getBlockDrawerLoadIntoSheetBitmap( Activity a ) {
		if ( VersionCapabilities.supportsLoadInBitmap() && this.mBlockDrawerLoadIntoSheetBitmap == null && a != null ) {
			GameViewMemoryCapabilities gvmc = getGameViewMemoryCapabilities(a) ;
			// create a ARGB_8888 image of the appropriate dimension.
			int dimen ;
			switch( gvmc.getLoadImagesSize() ) {
			case DrawSettings.IMAGES_SIZE_NONE:
				mBlockDrawerLoadIntoSheetBitmap = null ;
				break ;
			case DrawSettings.IMAGES_SIZE_SMALL:
				// 20 by 20.
				dimen = 20 * 16 ;
				mBlockDrawerLoadIntoSheetBitmap = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888) ;
				break ;
			case DrawSettings.IMAGES_SIZE_MID:
				// 40 by 40.
				dimen = 40 * 16 ;
				mBlockDrawerLoadIntoSheetBitmap = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888) ;
				break ;
			case DrawSettings.IMAGES_SIZE_LARGE:
			case DrawSettings.IMAGES_SIZE_HUGE:
				// 80 by 80.
				dimen = 80 * 16 ;
				mBlockDrawerLoadIntoSheetBitmap = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888) ;
				break ;
			}
		}
		
		return mBlockDrawerLoadIntoSheetBitmap ;
	}
	
	synchronized public Bitmap getBlockDrawerLoadIntoBackgroundBitmap( Activity a ) {
		if ( VersionCapabilities.supportsLoadInBitmap() && this.mBlockDrawerLoadIntoBackgroundBitmap == null && a != null ) {
			GameViewMemoryCapabilities gvmc = getGameViewMemoryCapabilities(a) ;
			// create a ARGB_8888 image of the appropriate dimension.
			switch( gvmc.getBackgroundImageSize() ) {
			case DrawSettings.IMAGES_SIZE_NONE:
				mBlockDrawerLoadIntoBackgroundBitmap = null ;
				break ;
			case DrawSettings.IMAGES_SIZE_SMALL:
				// 120 x 200
				mBlockDrawerLoadIntoBackgroundBitmap = Bitmap.createBitmap(120, 200, Bitmap.Config.ARGB_8888) ;
				break ;
			case DrawSettings.IMAGES_SIZE_MID:
				// 240 x 400
				mBlockDrawerLoadIntoBackgroundBitmap = Bitmap.createBitmap(240, 400, Bitmap.Config.ARGB_8888) ;
				break ;
			case DrawSettings.IMAGES_SIZE_LARGE:
			case DrawSettings.IMAGES_SIZE_HUGE:
				// 480 x 800
				mBlockDrawerLoadIntoBackgroundBitmap = Bitmap.createBitmap(480, 800, Bitmap.Config.ARGB_8888) ;
				break ;
			}
		}
		
		return mBlockDrawerLoadIntoBackgroundBitmap ;
	}
	
	
	
	synchronized public Bitmap getMenuBackgroundBitmap( Activity a ) {
		GameViewMemoryCapabilities gvmc = getGameViewMemoryCapabilities(a) ;
		
		// This call will automatically account for whether we use a
		// unique menu background, what it is set to, and the current / shuffled
		// background it we resort to that.
		Background bg = QuantroPreferences.getBackgroundMenu(a) ;
			
		Bitmap b = msrMenuBackgroundBitmap.get() ;
		if ( b == null || !bg.equals(mMenuBackground) ) {
			mMenuBackground = bg ;
			String bgPath = AssetAccessor.assetPathFromBackground(
					bg,
					gvmc.getBackgroundImageSize(),
					gvmc.getScreenWidth(), 
					gvmc.getScreenHeight()) ;
			
			if ( bgPath == null )
				return null ;

			try {
				AssetManager as = a.getAssets();
				BufferedInputStream buf = new BufferedInputStream(as.open(bgPath));
				b = BitmapFactory.decodeStream(buf) ;
				buf.close();
			} catch ( IOException e ) {
				e.printStackTrace() ;
				return null ;
			}
			
			msrMenuBackgroundBitmap = new SoftReference<Bitmap>(b) ;
		}
		
		return b ;
	}
	
	synchronized private void setAsActivityInFront( Activity a ) {
		activityInFront = a ;
		
		Log.d(TAG, "setAsActivityInFront") ;
		if ( mBlockDrawerPreallocatedBitmaps == null ) {
			mBlockDrawerPreallocatedBitmaps = createBlockDrawerPreallocatedBitmaps(a) ;
		}
	}
	
	
	private BlockDrawerPreallocatedBitmaps createBlockDrawerPreallocatedBitmaps( Activity a ) {
		GameViewMemoryCapabilities gvmc = getGameViewMemoryCapabilities(a) ;
		int width = (int)Math.ceil( gvmc.getScreenWidth() * GameView.GAME_FIELD_MAX_WIDTH ) ;
		int height = gvmc.getScreenHeight() ;
		
		int detail = QuantroPreferences.supportsMidDetailGraphics( this ) ? DrawSettings.DRAW_DETAIL_HIGH : DrawSettings.DRAW_DETAIL_LOW ;
		int blit = gvmc.getBlit() ;
		int scale = gvmc.getScale() ;
		float septStableHeightFactor = gvmc.getBlitSeptupleStableHeightFactor() ;
		int animation = DrawSettings.DRAW_ANIMATIONS_ALL ;
		int bgSize = gvmc.getBackgroundImageSize() ;
		int numBGs = gvmc.getNumBackgrounds() ;
		
		int loadSize = gvmc.getLoadImagesSize() ;
		
		int blockWidth = (int)Math.ceil( width / 8 ) ;			// max is 8 across.
		int blockHeight = (int)Math.ceil( height / 18 ) ;			// max is 18 across height
		// square blocks
		blockWidth = blockHeight = Math.min(blockWidth, blockHeight) ;
		
		if ( scale > 1 ) {
			blockWidth = (int) Math.ceil( blockWidth / (float)scale ) ;
			blockHeight = (int) Math.ceil( blockHeight / (float)scale ) ;
		}
		
		BlockDrawerPreallocatedBitmaps bdpb = BlockDrawer.preallocateBitmaps(
				width, height, blockWidth, blockHeight,
				GlobalTestSettings.setBlit(blit), GlobalTestSettings.setScale(scale),
				septStableHeightFactor,
				detail, animation,
				loadSize, bgSize, numBGs ) ;
		
		return bdpb ;
	}
	
	
	synchronized private void setAsActivityNotInFront( Activity a ) {
		if ( activityInFront == a )
			activityInFront = null ;
	}
	
	public Activity getActivityInFront() {
		return activityInFront ;
	}
	
	public boolean getQuantroActivityIsInFront() {
		return activityInFront != null ;
	}
	
	public QuantroSoundPool getSoundPool(Activity activity) {
		if ( mSoundPool == null )
			mSoundPool = prepareQuantroSoundPool() ;
		
		return mSoundPool ;
	}
	
	public void ringerDidChangeMode() {
		if ( mSoundPool != null )
			mSoundPool.ringerDidChangeMode() ;
	}
	
	
	private QuantroSoundPool prepareQuantroSoundPool() {
    	QuantroSoundPool qsp = new QuantroSoundPool(this, 8) ;
    	
    	// Load up all the sounds we need.
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_MOVE_LEFT_RIGHT, 			1, this, R.raw.fx_leftright) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_MOVE_LEFT_RIGHT_FAILED, 	1, this, R.raw.fx_pieceland) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_TURN_CCW_CW, 				1, this, R.raw.fx_rotate2) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_FLIP, 					1, this, R.raw.fx_rotate) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_DROP, 					1, this, R.raw.fx_down2) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_RESERVE, 					1, this, R.raw.fx_reserve) ;
    	
    	// piece and block effects
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOCK, 			1, this, R.raw.fx_piecelock2) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LAND,				1, this, R.raw.fx_pieceland) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_CLEAR_EMPH, 		1, this, R.raw.fx_rowclearpre) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_CLEAR, 			1, this, R.raw.fx_rowclear2) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_ROW_PUSH_GARBAGE, 1, this, R.raw.fx_garbage) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_COLORBLIND_METAMORPHOSIS, 1, this, R.raw.fx_greyscalelock) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_RISE_AND_FADE, 	1, this, R.raw.fx_flashpieceland) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_COLUMN_UNLOCK, 	1, this, R.raw.fx_columnunlock2) ;
    	// game level and victory/defeat  TODO: Different level-up sound depending on progression?
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LEVEL_UP, 		1, this, R.raw.fx_levelup) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_GAME_LOSS, 		1, this, R.raw.fx_youlose) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_GAME_WIN, 		1, this, R.raw.fx_youwin) ;
    	// pause/resume
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_RESUME, 			1, this, R.raw.fx_unpause) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_PAUSE, 			1, this, R.raw.fx_pause) ;
    	// get ready!  This is only used for MP games.
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_GET_READY,  		1, this, R.raw.fx_greyscalelock) ;
    	
    	// New sound effects.
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_ATTACK, 				1, this, R.raw.fx_attack) ;		// a bit discordant
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_DISPLACEMENT_ACCEL, 	1, this, R.raw.fx_blockspeed) ;	// same garbage 
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_ENTER_BLOCKS, 		1, this, R.raw.fx_newblocks) ;	// very minor diff. compared to TYPE_LOCK.
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_SPECIAL_UPGRADED,		1, this, R.raw.fx_ready) ;	// ermm...
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_ATTACKED_BY_BLOCKS, 	1, this, R.raw.fx_incoming) ;	// generic "bad stuff" noise, I guess.
    	// standard row push
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_ROW_PUSH_ADD,		 	1, this, R.raw.fx_rowsadded) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_ROW_PUSH_SUBTRACT,	1, this, R.raw.fx_rowssubtract) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_ROW_PUSH_ZERO,		1, this, R.raw.fx_rowsneutral) ;
    	// penalty.
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_PENALTY,				1, this, R.raw.fx_down) ;
    	
    	
    	
    	// menus!  We re-use some sounds here.
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_MENU_BUTTON_CLICK, 	1, this, R.raw.fx_leftright) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_MENU_BUTTON_HOLD, 	1, this, R.raw.fx_pieceland) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_MENU_BACK_BUTTON_CLICK, 1, this, R.raw.fx_down2) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_MENU_BUTTON_FLIP, 	1, this, R.raw.fx_rotate2) ;
    	
    	// lobbies!
    	// launch
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_LAUNCH_COUNTDOWN_START, 1, this, R.raw.fx_greyscalelock) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_LAUNCH_COUNTDOWN_CANCEL, 1, this, R.raw.fx_down) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_LAUNCH_GO, 1, this, R.raw.fx_youwin) ;
    	// chat
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_USER_CHAT, 1, this, R.raw.fx_garbage) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_CHAT, 1, this, R.raw.fx_garbage) ;
    	// active: unpause.  inactive: pause
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_USER_GO_ACTIVE, 1, this, R.raw.fx_unpause) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_GO_ACTIVE, 1, this, R.raw.fx_unpause) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_USER_GO_INACTIVE, 1, this, R.raw.fx_pause) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_GO_INACTIVE, 1, this, R.raw.fx_pause) ;
    	// join/quit: play lock, column unlock.
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_USER_JOIN, 1, this, R.raw.fx_piecelock) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_JOIN, 1, this, R.raw.fx_piecelock) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_USER_QUIT, 1, this, R.raw.fx_columnunlock2) ;
    	qsp.loadSound(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_QUIT, 1, this, R.raw.fx_columnunlock2) ;
    	
    	
    	// Set starting volume for music and sounds.
    	// TODO: Load from shared prefs., maybe in another method.
    	qsp.setMusicVolume( QuantroPreferences.getVolumeMusic(this) ) ;
    	qsp.setInGameSoundVolume( QuantroPreferences.getVolumeSound(this) ) ;
    	qsp.setOutOfGameSoundVolume( 1.0f ) ;
    	
    	// Set muted / not muted according to Shared Preferences
    	if ( QuantroPreferences.getMuted(this) )
    		qsp.mute() ;
    	if ( QuantroPreferences.getMuteWithRinger(this) )
    		qsp.muteWithRinger() ;
    	
    	return qsp ;
    }
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// QUANTRO ACTIVITY CALLBACKS
	//
	// Android offers a convenient callback registration method... unfortuntately,
	// it was introduced in API 14, whereas we support earlier versions.
	//
	// These methods provide the same basic functionality without requiring
	// any minimum API.
	//
	
	synchronized public void onActivityCreated( Activity activity, Bundle savedInstanceState ) {
		// nothing
	}
	
	synchronized public void onActivityStarted( Activity activity ) {
		// nothing
		if ( activity instanceof QuantroActivity ) {
			QuantroActivity qa = (QuantroActivity)activity ;
			// count as part of the stack
			mActivityStackSize++ ;
			if ( !qa.isActivityGame() ) {
				mActivityStackSizeNonGame++ ;
			}
			
			Log.d(TAG, "onActivityStarted: nongame stack " + mActivityStackSizeNonGame + " " + qa) ;
			
			// we handle music for non-game activities.
			if ( mActivityStackSizeNonGame > 0 && !qa.isActivityGame() ) {
				// load and play music.  Note that these calls are perfectly
				// safe even if quite redundant.
				Music newMusic = qa.getMusicForQuantroActivity() ;
				QuantroSoundPool soundPool = getSoundPool(activity) ;
				soundPool.loadMusic(activity, newMusic) ;
				soundPool.playMusic() ;
				
				Log.d(TAG, "onActivityStarted: load and play " + newMusic + " for menu " + qa.isActivityMenu()) ;
			}
		}
	}
	
	synchronized public void onActivityResumed( Activity activity ) {
		// set in front
		setAsActivityInFront( activity ) ;
	}
	
	synchronized public void onActivityPaused( Activity activity ) {
		// set not in  front
		setAsActivityNotInFront( activity ) ;
	}
	
	synchronized public void onActivityStopped( Activity activity ) {
		if ( activity instanceof QuantroActivity ) {
			QuantroActivity qa = (QuantroActivity)activity ;
			// uncount as part of the stack
			mActivityStackSize-- ;
			if ( !qa.isActivityGame() ) {
				mActivityStackSizeNonGame-- ;
			}
			
			Log.d(TAG, "onActivityStopped: nongame stack " + mActivityStackSizeNonGame + " " + qa) ;
			
			// we handle music for non-game activities.
			if ( mActivityStackSizeNonGame <= 0 && !qa.isActivityGame() ) {
				// stop the music, but only if there is not a game in front.
				if ( activityInFront == null || !((QuantroActivity)activityInFront).isActivityGame() ) {
					QuantroSoundPool soundPool = getSoundPool(activity) ;
					soundPool.pauseMusic() ;
					
					Log.d(TAG, "onActivityStopped: pausing music") ;
				}
				mActivityStackSizeNonGame = 0 ;
			}
		}
	}
	
	synchronized public void onActivityDestroyed( Activity activity ) {
		// nothing
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	
	public class ScreenReceiver extends BroadcastReceiver {
	      
	    // THANKS JASON
	    private boolean mScreenOn = true;
	    private boolean mPausedMusicOnScreenOff = false ;
	 
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
	            // we are switching off the screen.  We pause the music,
	        	// if music is playing, in every case except one: where the
	        	// activity currently on top is a Game activity.
	        	if ( mSoundPool != null && mSoundPool.isPlayingMusic() ) {
	        		if ( activityInFront == null || !((QuantroActivity)activityInFront).isActivityGame() ) {
	        			mSoundPool.pauseMusic() ;
	        			mPausedMusicOnScreenOff = true ;
	        		}
	        	}
	        	// Pause the music in two cases: 
	        	mScreenOn = false;
	        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
	            // the screen is switching back on.  If we had paused the music for
	        	// screen-off, and it is not currently playing, start it up again.
	        	if ( mSoundPool != null && !mSoundPool.isPlayingMusic() && mPausedMusicOnScreenOff ) {
	        		// BUG: sometimes we start playing music when the screen turns on
	        		// via the phone ringing, meaning the activity is no longer
	        		// on top.  However, for some reason, it never gets a "stop"
	        		// message.
        			startMusicEnableIfAppropriateRunnable( handler, mSoundPool ) ;
	        	}
	        	mPausedMusicOnScreenOff = false ;
	        	mScreenOn = true;
	        }
	    }
	    
	    public boolean screenOn() {
	    	return mScreenOn ;
	    }
	 
	}
	
	
	/**
	 * A self-reposting Runnable, meant for a single use per-instance
	 * (it reposts several times in sequence to monitor the "on-screen"
	 * status).
	 * 
	 * If the several consecutive reposts each monitor that the game
	 * is not onscreen, it will (on the final check) disable the music
	 * if it is currently playing.
	 * 
	 * @author Jake
	 *
	 */
	public class MusicToggleRunnable implements Runnable {
		
		public static final long REPOST_STEP = 200 ;
		public static final int NUM_CHECKS = 3 ;

		private int mTimesChecked ;
		private Handler mHandler ;
		private boolean mToggleToOn ;
		
		private WeakReference<QuantroSoundPool> mwrSoundPool ;
		
		private MusicToggleRunnable( Handler handler, QuantroSoundPool pool, boolean on ) {
			mTimesChecked = 0 ;
			mHandler = handler ;
			mToggleToOn = on ;
			
			mwrSoundPool = new WeakReference<QuantroSoundPool>(pool) ;
		}
		
		@Override
		public void run() {
			// if there's no pool, stop immediately.
			QuantroSoundPool pool = mwrSoundPool.get() ;
			if ( pool == null ) {
				return ;
			}
			
			// if the sound pool's current status doesn't match our
			// expectations, stop immediately.
			if ( pool.isPlayingMusic() == mToggleToOn ) {
				// if it IS PLAYING, and we want to TOGGLE ON, then
				// it is already in the soon-to-be appropriate state.
				// Same with negation.
				return ;
			}
			
			// we have checked an additional time.
			mTimesChecked++ ;
			
			// are we done?
			if ( mTimesChecked >= NUM_CHECKS ) {
				QuantroActivity qa = (QuantroActivity)activityInFront ;
				
 				// time to toggle!
				if ( mToggleToOn ) {
					// perform some extensive checks.  We know the music isn't
					// playing, but make sure the Activity's doing SOMETHING:
					// is it displayed?  Does the stack have at least one
					// item on it?
					
					// we handle music for non-game activities.
					if ( mActivityStackSizeNonGame > 0 && ( qa == null || !qa.isActivityGame() ) ) {
						// play!
						pool.playMusic() ;
					}
				} else {
					// perform some checks.  We know the music is playing,
					// but maybe it should be?  Check that the stack is empty
					// and that there is no activity in front before stopping.
					if ( mActivityStackSizeNonGame == 0 && qa == null ) {
						// unplay!
						pool.pauseMusic() ;
					}
				}
				
				// done.
				return ;
			}
			
			// repost?
			if ( mTimesChecked < NUM_CHECKS ) {
				mHandler.postDelayed(this, REPOST_STEP) ;
			}
		}
		
	}
	
	private void startMusicEnableIfAppropriateRunnable( Handler handler, QuantroSoundPool pool ) {
		MusicToggleRunnable mtr = new MusicToggleRunnable( handler, pool, true ) ;
		handler.postDelayed(mtr, MusicToggleRunnable.REPOST_STEP) ;
	}
	
	private void startMusicDisableIfAppropriateRunnable( Handler handler, QuantroSoundPool pool ) {
		MusicToggleRunnable mtr = new MusicToggleRunnable( handler, pool, false ) ;
		handler.postDelayed(mtr, MusicToggleRunnable.REPOST_STEP) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// GAME HELPER ACCESS METHODS
	//

    public void gpg_submitScoreImmediate(Activity activity, String leaderboardID, long score) {
		// TODO
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
	}

	public Intent gpg_getAllLeaderboardsIntent() {
		// TODO
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
		return null;
	}

	public Intent gpg_getLeaderboardIntent(String leaderboardID) {
		// TODO
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
		return null;
	}

	public Intent gpg_getAchievementsIntent() {
		// TODO
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
		return null;
	}
	
	public boolean gpg_handleActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "gpg_handleActivityResult") ;
		// the only thing this could be is a GameHelper result.
		// return mGameHelper.handleActivityResult(requestCode, resultCode, data) ;
		return false;
		// update sign in?
	}
	
	public boolean gpg_isSignedIn() {
        return false;
    }

    public void gpg_beginUserInitiatedSignIn() {
    	QuantroPreferences.setPrivateSettingBoolean(this, PREFERENCE_GAME_HELPER_AUTOSTART, true) ;
        // mGameHelper.beginUserInitiatedSignIn();
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
    }

    public void gpg_signOut( boolean userInitiated ) {
    	if ( userInitiated )
    		QuantroPreferences.setPrivateSettingBoolean(this, PREFERENCE_GAME_HELPER_AUTOSTART, false) ;
        // mGameHelper.signOut();
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
        // PLACEHOLDER: activity should refresh its stinger.
		Activity activity = getActivityInFront() ;
		if ( activity != null && activity instanceof QuantroActivity ) {
			((QuantroActivity)activity).onGooglePlaySignInUpdated(false) ;
		}
    }

    public void gpg_showAlert(String title, String message) {
        // mGameHelper.showAlert(title, message);
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
    }

    public void gpg_showAlert(String message) {
        // mGameHelper.showAlert(message);
		Toast.makeText(this, "Google Play support has been deprecated", Toast.LENGTH_SHORT).show() ;
    }
	
	//
	////////////////////////////////////////////////////////////////////////////
	
}
