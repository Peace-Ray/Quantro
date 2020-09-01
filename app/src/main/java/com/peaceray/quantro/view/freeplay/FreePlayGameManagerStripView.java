package com.peaceray.quantro.view.freeplay;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Hashtable;

import com.peaceray.android.graphics.BitmapSoftCache;
import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.R;
import com.peaceray.quantro.model.GameSaver;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.collage.FreePlayGameLaunchButtonCollage;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip;
import com.peaceray.quantro.view.button.strip.SinglePlayerGameLaunchButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.generic.PinnedHeaderListView;
import com.peaceray.quantro.view.options.OptionAvailability;
import com.velosmobile.utils.SectionableAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class FreePlayGameManagerStripView extends RelativeLayout
		implements
		FreePlayGameManagerView, SinglePlayerGameLaunchButtonStrip.Delegate, CustomButtonStrip.Delegate, com.peaceray.quantro.view.button.collage.FreePlayGameLaunchButtonCollage.Delegate {

	
	private static final String TAG = "FreePlayGameManagerStripView" ;
	
	
	
	// Inflate from XML
	public FreePlayGameManagerStripView( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public FreePlayGameManagerStripView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues(Context context) {
		mActionStripID = 0 ;
		mGameModeListID = 0 ;
		mGameModeListPlaceholderID = 0 ;
		mInstructionsTextViewID = 0 ;
		
		mActionStrip = null ;
		mGameModeList = null ;
		mInstructionsTextView = null ;
		
		// Our delegate!
		mwrDelegate = new WeakReference<Delegate>(null) ;
		// Have we started?
		mStarted = false ;
		mChangedWhenStopped = false ;
		
		ACTION_NAME_NEW_MODE = context.getResources().getString(R.string.action_strip_name_new) ;
		ACTION_NAME_ACHIEVEMENTS = context.getResources().getString(R.string.action_strip_name_achievements) ;
		ACTION_NAME_LEADERBOARD = context.getResources().getString(R.string.action_strip_name_leaderboard) ;
		ACTION_NAME_HELP = context.getResources().getString(R.string.action_strip_name_help) ;
		ACTION_NAME_SETTINGS = context.getResources().getString(R.string.action_strip_name_settings) ;
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.FreePlayGameManagerStripView);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.FreePlayGameManagerStripView_fpgmsv_action_strip:
				mActionStripID = a.getResourceId(attr, mActionStripID) ;
				break ;
			case R.styleable.FreePlayGameManagerStripView_fpgmsv_game_list:
				mGameModeListID = a.getResourceId(attr, mGameModeListID) ;
				break ;
			case R.styleable.FreePlayGameManagerStripView_fpgmsv_game_list_placeholder:
				mGameModeListPlaceholderID = a.getResourceId(attr, mGameModeListPlaceholderID) ;
				break ;
			case R.styleable.FreePlayGameManagerStripView_fpgmsv_instructions_text_view:
				mInstructionsTextViewID = a.getResourceId(attr, mInstructionsTextViewID) ;
				break ;
			}
		}
		
		// recycle the array
		a.recycle() ;
	}
	
	
	private void constructor_allocateAndInitialize(Context context) {
		// First: verify that we have the necessary ID fields.
		if ( mActionStripID == 0 )
			throw new RuntimeException("Must specify an Action Strip ID.") ;
		else if ( mGameModeListID == 0 ) 
			throw new RuntimeException("Must specify a GameModeList ID.") ;
		
		// Okay.  Allocate our ArrayLists and ArrayAdapters.
		mSinglePlayerGameModes = new ArrayList<Integer>() ;
		mSinglePlayerGameResults = new ArrayList<GameResult>() ;
		mSinglePlayerGameHasSave = new ArrayList<Boolean>() ;
		mAvailability = new Hashtable<Integer, OptionAvailability>() ;
		// if we only support BLIT NONE as our draw settings, we
		// get extremely flawed thumbnails.  To prevent their display,
		// use the strip list item in that case.
		int layoutID = R.layout.free_play_button_collage_list_item ;
		if ( context instanceof Activity ) {
			if ( ((QuantroApplication)(getContext()).getApplicationContext()).getGameViewMemoryCapabilities((Activity)context).getBlit() == DrawSettings.BLIT_NONE )
				layoutID = R.layout.free_play_button_strip_list_item ;
		}
		mFreePlayGameLaunchButtonCollageAdapter =
			new FreePlayGameLaunchButtonCollageAdapter(
					((Activity)context).getLayoutInflater(),
					layoutID,
					0,
					R.id.free_play_game_mode_list_row) ;
		
		mSinglePlayerThumnbailCache = new BitmapSoftCache() ;
		mSinglePlayerThumbnailLoader = new GameSaver.ThumbnailLoader(context) ;

		// NOTE: the ListViews may not have been inflated yet, so only
		// link them with these adapters in 'start'.
		
		mColorScheme = null ;
		mSoundPool = null ;
		mSoundControls = false ;
		mStarted = false ;
		
		mHandler = new Handler() ;
		mRefreshFreePlayListRunnable = new Runnable() {
			@Override
			public void run() {
				mHandler.removeCallbacks(this) ;
				
				if ( mStarted ) {
					// mFreePlayGameLaunchButtonCollageAdapter.setNotifyOnChange(false) ;
					mFreePlayGameLaunchButtonCollageAdapter.clear() ;
					
					boolean hasGames ;
					
					synchronized( mGameModeMutex ) {
						hasGames = mSinglePlayerGameModes.size() > 0 ;
						for ( int i = 0; i < mSinglePlayerGameModes.size(); i++ ) {
							mFreePlayGameLaunchButtonCollageAdapter.add(
									mSinglePlayerGameModes.get(i),
									mSinglePlayerGameResults.get(i),
									mSinglePlayerGameHasSave.get(i)) ;
							
							if ( mSinglePlayerGameModes.size() > 0 && mGameModeListPlaceholderID != 0 ) {
								// time to hide the placeholder
								View v = findViewById(mGameModeListPlaceholderID) ;
								if ( v != null )
									v.setVisibility(View.GONE) ;
								mGameModeListPlaceholderID = 0 ;
							}
						}
					}
					
					mFreePlayGameLaunchButtonCollageAdapter.notifyDataSetChanged() ;
					mGameModeList.invalidateViews() ;
					
					if ( !mInstructionsTextViewVisibilityAutochange ) {
						if ( hasGames && mInstructionsTextView.getHeight() >= 80 )
							mInstructionsTextView.setVisibility(View.VISIBLE) ;
						else
							mInstructionsTextView.setVisibility(View.INVISIBLE) ;
					}
				}
			}
		};
		
		mRefreshViewRunnable = new Runnable() {
			@Override
			public void run() {
				mFreePlayGameLaunchButtonCollageAdapter.notifyDataSetChanged() ;
				mGameModeList.invalidateViews() ;
				mGameModeList.invalidate() ;
			}
		} ;
	}
	
	
	private Handler mHandler ;
	private Runnable mRefreshFreePlayListRunnable ;
	private Runnable mRefreshViewRunnable ;
	
	private String ACTION_NAME_NEW_MODE ;
	private String ACTION_NAME_ACHIEVEMENTS ;
	private String ACTION_NAME_LEADERBOARD ;
	private String ACTION_NAME_HELP ;
	private String ACTION_NAME_SETTINGS ;
	
	// View IDs.  Help us refresh, add challenges, and the like.
	private int mActionStripID ;
	private int mGameModeListID ;
	private int mGameModeListPlaceholderID ;
	private int mInstructionsTextViewID ;
	
	// Our views!
	private CustomButtonStrip mActionStrip ;
	private PinnedHeaderListView mGameModeList ;
	private TextView mInstructionsTextView ;
	private boolean mInstructionsTextViewVisibilityAutochange = false ;
	

	// Array adapter
	private Object mGameModeMutex = new Object() ;
	private ArrayList<Integer> mSinglePlayerGameModes ;
	private ArrayList<GameResult> mSinglePlayerGameResults ;
	private ArrayList<Boolean> mSinglePlayerGameHasSave ;
	private Hashtable<Integer, OptionAvailability> mAvailability ;
	private FreePlayGameLaunchButtonCollageAdapter mFreePlayGameLaunchButtonCollageAdapter ;
	private BitmapSoftCache mSinglePlayerThumnbailCache ;
	private GameSaver.ThumbnailLoader mSinglePlayerThumbnailLoader ;
	// private SinglePlayerGameLaunchButtonStripArrayAdapter mSinglePlayerGameLaunchButtonStripArrayAdapter ;
	
	
	// Our delegate!
	private WeakReference<FreePlayGameManagerView.Delegate> mwrDelegate ;
	// Sound pool and controls
	private QuantroSoundPool mSoundPool ;
	private boolean mSoundControls ;
	private ColorScheme mColorScheme ;
	
	// Have we started?
	private boolean mStarted ;
	private boolean mChangedWhenStopped ;
	
	
	@Override
	public void start() {
		Log.d(TAG, "start") ;
		mStarted = true ;
		
		Context context = getContext() ;

		// get references, if we don't have them already.
		if ( mActionStrip == null && mActionStripID != 0 ) {
			mActionStrip = (CustomButtonStrip) findViewById(mActionStripID) ;
			mActionStrip.setDelegate(this) ;
			// no need to set ACHIEVEMENTS active; it is a popup menu item for the leaderboards button.
			mActionStrip.setActive( mActionStrip.getButton( ACTION_NAME_LEADERBOARD ) ) ;
			mActionStrip.setActive( mActionStrip.getButton( ACTION_NAME_NEW_MODE ) ) ;
			mActionStrip.setActive( mActionStrip.getButton( ACTION_NAME_HELP ) ) ;
			mActionStrip.setActive( mActionStrip.getButton( ACTION_NAME_SETTINGS ) ) ;
			mActionStrip.refresh() ;
		}
		if ( mGameModeList == null && mGameModeListID != 0 ) {
			mGameModeList = (PinnedHeaderListView) findViewById(mGameModeListID) ;
			
			mGameModeList.setAdapter(mFreePlayGameLaunchButtonCollageAdapter) ;
    		mGameModeList.setOnScrollListener((OnScrollListener)mFreePlayGameLaunchButtonCollageAdapter) ;
    		mGameModeList.setItemsCanFocus(true) ;
    		if ( mGameModeList instanceof PinnedHeaderListView ) {
    			View pinnedHeaderView = ((Activity)context).getLayoutInflater().inflate(R.layout.quantro_list_item_header, mGameModeList, false) ;
    			pinnedHeaderView.setTag( new FreePlayGameLaunchButtonRowTag(pinnedHeaderView ) ) ;
    			((PinnedHeaderListView)mGameModeList).setPinnedHeaderView( pinnedHeaderView ) ;
    			mGameModeList.setDivider(null) ;
    			mGameModeList.setDividerHeight(0);
    		} else
    			Log.d(TAG, "did NOT set pinned header view") ;
		}
		if ( mInstructionsTextView == null && mInstructionsTextViewID != 0 ) {
			mInstructionsTextView = (TextView) findViewById(mInstructionsTextViewID) ;
			mInstructionsTextView.setVisibility(View.INVISIBLE) ;
			mInstructionsTextViewVisibilityAutochange = VersionSafe.addOnLayoutChangeListener(mInstructionsTextView,
					new VersionSafe.OnLayoutChangeListener() {
						@Override
						public void onLayoutChange(View v, int left, int top,
								int right, int bottom, int oldLeft, int oldTop,
								int oldRight, int oldBottom) {
							int height = bottom - top ;
							if ( height >= 80 )
								mInstructionsTextView.setVisibility(View.VISIBLE) ;
							else
								mInstructionsTextView.setVisibility(View.INVISIBLE) ;
						}
					}) ;
		}
		
		
		if ( mChangedWhenStopped )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		mChangedWhenStopped = false ;
	}

	@Override
	public void stop() {
		mStarted = false ;
		// no thread; nothing to do
	}

	@Override
	public void setDelegate(Delegate delegate) {
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}

	@Override
	public void setSoundPool(QuantroSoundPool soundPool) {
		mSoundPool = soundPool ;
	}

	@Override
	public void setSoundControls(boolean soundControls) {
		mSoundControls = soundControls ;
	}
	
	@Override
	public void setColorScheme( ColorScheme scheme ) {
		mColorScheme = scheme ;
		if ( mStarted )
			mGameModeList.invalidateViews() ;
	}
	
	
	@Override
	public void refreshView() {
		mHandler.post(mRefreshViewRunnable) ;
	}
	

	/**
	 * Removes all game modes from the view.
	 */
	public void clearGames() {
		synchronized ( mGameModeMutex ) {
			mSinglePlayerGameModes.clear() ;
			mSinglePlayerGameResults.clear() ;
			mSinglePlayerGameHasSave.clear() ;
			mSinglePlayerThumnbailCache.clear() ;
			mAvailability.clear() ;
		}
		
		if ( mStarted )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else
			mChangedWhenStopped = true ;
	}
	
	
	@Override
	public void setGames(int[] gameModes, OptionAvailability [] availability, boolean[] hasResult ) {
		synchronized ( mGameModeMutex ) {
			mSinglePlayerGameModes.clear() ;
			mSinglePlayerGameResults.clear() ;
			mSinglePlayerGameHasSave.clear() ;
			mSinglePlayerThumnbailCache.clear() ;
			mAvailability.clear() ;
		
			for ( int i = 0; i < gameModes.length; i++ ) {
				int index = findInsertionPosition( gameModes[i] ) ;
				
				if ( index >= 0 ) {
					mSinglePlayerGameModes.add(index, gameModes[i]) ;
					mSinglePlayerGameResults.add( index, null ) ;
					mSinglePlayerGameHasSave.add( index, hasResult[i] ) ;
					mAvailability.put( Integer.valueOf( gameModes[i] ),
							availability[i] ) ;
				}
			}
		}
		
		if ( mStarted )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else 
			mChangedWhenStopped = true ;
	}

	@Override
	public void setGames(int[] gameModes, OptionAvailability [] availability, GameResult[] gameResults ) {
		synchronized ( mGameModeMutex ) {
			mSinglePlayerGameModes.clear() ;
			mSinglePlayerGameResults.clear() ;
			mSinglePlayerGameHasSave.clear() ;
			mSinglePlayerThumnbailCache.clear() ;
			mAvailability.clear() ;
		
			for ( int i = 0; i < gameModes.length; i++ ) {
				int index = findInsertionPosition( gameModes[i] ) ;
				
				if ( index >= 0 ) {
					mSinglePlayerGameModes.add(index, gameModes[i]) ;
					mSinglePlayerGameResults.add( index, gameResults == null ? null : gameResults[i] ) ;
					mSinglePlayerGameHasSave.add( index, gameResults == null ? null : gameResults[i] != null ) ;
					mAvailability.put( Integer.valueOf( gameModes[i] ),
							availability[i] ) ;
				}
			}
		}
		
		if ( mStarted )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else 
			mChangedWhenStopped = true ;
	}

	@Override
	public void setGameResult(int gameMode, GameResult gr) {
		boolean inserted = false ;
		synchronized ( mGameModeMutex ) {
			mSinglePlayerThumnbailCache.remove(Integer.valueOf(gameMode)) ;
			int index = mSinglePlayerGameModes.indexOf(gameMode) ;
			if ( index > -1 ) {
				mSinglePlayerGameResults.set(index, gr) ;
				mSinglePlayerGameHasSave.set(index, gr != null) ;
				inserted = true ;
			}
		}
		
		if ( mStarted && inserted )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else
			mChangedWhenStopped = mChangedWhenStopped || inserted ;
	}
	


	@Override
	public void setGameResult( int gameMode, boolean hasSave ) {
		boolean inserted = false ;
		synchronized ( mGameModeMutex ) {
			mSinglePlayerThumnbailCache.remove(Integer.valueOf(gameMode)) ;
			int index = mSinglePlayerGameModes.indexOf(gameMode) ;
			if ( index > -1 ) {
				mSinglePlayerGameResults.set(index, null) ;
				mSinglePlayerGameHasSave.set(index, hasSave) ;
				inserted = true ;
			}
		}
		
		if ( mStarted && inserted )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else
			mChangedWhenStopped = mChangedWhenStopped || inserted ;
	}
	
	

	@Override
	public void addGame(int gameMode, OptionAvailability availability, GameResult gr) {
		boolean inserted = false ;
		synchronized ( mGameModeMutex ) {
			int index = findInsertionPosition( gameMode ) ;
			if ( index >= 0 ) {
				mSinglePlayerGameModes.add(index, gameMode) ;
				mSinglePlayerGameResults.add(index, gr) ;
				mSinglePlayerGameHasSave.add(index, gr != null) ;
				mAvailability.put(Integer.valueOf(gameMode), availability) ;
				inserted = true ;
			}
		}
		
		if ( mStarted && inserted )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else
			mChangedWhenStopped = mChangedWhenStopped || inserted ;
	}
	
	
	
	@Override
	public void addGame( int gameMode, OptionAvailability availability, boolean hasSave ) {
		boolean inserted = false ;
		synchronized ( mGameModeMutex ) {
			int index = findInsertionPosition( gameMode ) ;
			if ( index >= 0 ) {
				mSinglePlayerGameModes.add(index, gameMode) ;
				mSinglePlayerGameResults.add(index, null) ;
				mSinglePlayerGameHasSave.add(index, hasSave) ;
				mAvailability.put(Integer.valueOf(gameMode), availability) ;
				inserted = true ;
			}
		}
		
		if ( mStarted && inserted )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else
			mChangedWhenStopped = mChangedWhenStopped || inserted ;
	}
	
	

	@Override
	public void removeGame(int gameMode) {
		boolean removed = false ;
		synchronized (mGameModeMutex ) {
			mSinglePlayerThumnbailCache.remove(Integer.valueOf(gameMode)) ;
			int index = mSinglePlayerGameModes.indexOf(gameMode) ;
			if ( index > -1 ) {
				mSinglePlayerGameModes.remove(index) ;
				mSinglePlayerGameResults.remove(index) ;
				mAvailability.remove(Integer.valueOf(gameMode)) ;
				removed = true ;
			}
		}
		
		if ( mStarted && removed )
			mHandler.post(mRefreshFreePlayListRunnable) ;
		else
			mChangedWhenStopped = mChangedWhenStopped || removed ;
	}
	
	
	@Override
	public boolean supportsThumbnails() {
		// If our launch buttons are collages, we support thumbnails.
		// Otherwise, we don't.
		if ( mFreePlayGameLaunchButtonCollageAdapter == null )
			return false ;
		return mFreePlayGameLaunchButtonCollageAdapter.mHasCollages ;
	}
	
	
	/**
	 * This is basically our insertion-sort operation.
	 * Returns the position in mSinglePlayerGameModes where the
	 * game mode should be inserted.
	 * 
	 * Returns -1 if the provided game mode is already present,
	 * or there is no valid place for it.
	 * 
	 * @param gameMode
	 * @return
	 */
	private int findInsertionPosition( int gameMode ) {
		
		// Current order separates built-in and custom games.
		// built-in games are ordered first by dimensions (3D first),
		// then by game mode.
		
		// Custom games are ordered first by dimension (3D first),
		// then by game mode.  Because all
		// custom games appear after all built-in games in terms
		// of their game mode, that is an easy case to consider.
		
		boolean isCustom = GameModes.isCustom(gameMode) ;
		
		if ( !isCustom ) {
			boolean is3d = GameModes.numberQPanes(gameMode) == 2 ;
			for ( int i = 0; i < mSinglePlayerGameModes.size(); i++ ) {
				int gm = mSinglePlayerGameModes.get(i) ;
				boolean gm_is3d = GameModes.numberQPanes(gm) == 2 ;
				
				if ( GameModes.isCustom(gm) )
					return i ;
				
				if ( is3d && !gm_is3d )
					return i ;
				
				if ( is3d == gm_is3d && gameMode < gm )
					return i ;
				
				if ( gameMode == gm )
					return -1 ;
			}
		} else {
			boolean is3d = GameModes.numberQPanes(gameMode) == 2 ;
			for ( int i = 0; i < mSinglePlayerGameModes.size(); i++ ) {
				int gm = mSinglePlayerGameModes.get(i) ;
				boolean gm_is3d = GameModes.numberQPanes(gm) == 2 ;
				
				if ( !GameModes.isCustom(gm) )
					continue ;	// continue past until we find the custom games.
				
				if ( is3d && !gm_is3d )
					return i ;
				
				if ( is3d == gm_is3d && gameMode < gm )
					return i ;
				
				if ( gameMode == gm )
					return -1 ;
			}
		}
		
		return mSinglePlayerGameModes.size() ;
	}
	
	


	@Override
	public boolean customButtonStrip_onButtonClick(CustomButtonStrip strip,
			int buttonNum, String name, boolean asOverflow ) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		boolean didAction = false ;
		
		if ( strip.getId() == mActionStripID ) {
			if ( ACTION_NAME_NEW_MODE.equals(name) ) {
				delegate.fpgmv_createGame(this) ;
				didAction = true ;
			}
			
			if ( ACTION_NAME_ACHIEVEMENTS.equals(name) ) {
				delegate.fpgmv_achievements(this) ;
				didAction = true ;
			}
			
			if ( ACTION_NAME_LEADERBOARD.equals(name) ) {
				delegate.fpgmv_leaderboard(this, -1) ;
				didAction = true ;
			}
			
			if ( ACTION_NAME_HELP.equals(name) ) {
				delegate.fpgmv_help(this) ;
				didAction = true ;
			}
			
			if ( ACTION_NAME_SETTINGS.equals(name) ) {
				delegate.fpgmv_openSettings(this) ;
				didAction = true ;
			}
		}
		
		if ( didAction && mSoundControls && mSoundPool != null && !asOverflow )
			mSoundPool.menuButtonClick() ;
		
		return didAction;
	}

	@Override
	public boolean customButtonStrip_onButtonLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		
		return false;
	}
	
	@Override
	public boolean customButtonStrip_supportsLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		
		return false;
	}
	
	public void customButtonStrip_onPopupOpen(
			CustomButtonStrip strip ) {
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return ;
		
		if ( mSoundPool != null && mSoundControls )
			mSoundPool.menuButtonClick() ;
	}
	

	@Override
	public boolean spglbs_onButtonClick(
			SinglePlayerGameLaunchButtonStrip strip,
			int buttonType, int gameMode) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_FIRST
				|| buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_NEW ) {
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			delegate.fpgmv_newGame(this, gameMode, false) ;
			return true ;
		}
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_RESUME ) {
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			delegate.fpgmv_loadGame(this, gameMode, false) ;
			return true ;
		}
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_EDIT_GAME_MODE ) {
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonClick() ;
			delegate.fpgmv_editGame(this, gameMode) ;
			return true ;
		}
		
		return false;
	}

	@Override
	public boolean spglbs_onButtonLongClick(
			SinglePlayerGameLaunchButtonStrip strip,
			int buttonType, int gameMode) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_FIRST
				|| buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_NEW ) {
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonHold() ;
			delegate.fpgmv_newGame(this, gameMode, true) ;
			return true ;
		}
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_RESUME ) {
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonHold() ;
			delegate.fpgmv_loadGame(this, gameMode, true) ;
			return true ;
		}
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_EDIT_GAME_MODE ) {
			if ( mSoundControls && mSoundPool != null )
				mSoundPool.menuButtonHold() ;
			delegate.fpgmv_editGame(this, gameMode) ;
			return true ;
		}
		
		return false;
	}
	
	
	@Override
	public boolean spglbs_supportsLongClick(
			SinglePlayerGameLaunchButtonStrip strip,
			int buttonType, int gameMode) {
		
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_FIRST
				|| buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_NEW )
			return true ;
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_RESUME )
			return true ;
		
		if ( buttonType == SinglePlayerGameLaunchButtonStrip.BUTTON_TYPE_EDIT_GAME_MODE )
			return true ;
		
		return false;
	}
	
	
	
	private class SinglePlayerGameModeIndexer implements SectionIndexer {
		
		protected static final int SECTION_QUANTRO = 0 ;
		protected static final int SECTION_RETRO = 1 ;
		protected static final int SECTION_CUSTOM_QUANTRO = 2 ;
		protected static final int SECTION_CUSTOM_RETRO = 3 ;
		public static final int NUM_SECTIONS = 4 ;
		
		protected String [] mSections = new String[]{ "Quantro", "Retro", "Custom Quantro", "Custom Retro" } ;
		protected Integer [] mGameModes ;
		
		public SinglePlayerGameModeIndexer( Integer [] gameModes ) {
			mGameModes = gameModes ;
		}
		
		synchronized public void setGameModes( Integer [] gameModes ) {
			mGameModes = gameModes ;
		}
		
		synchronized public Object[] getSections() {
	        return mSections ;
	    }
	    
	    /**
	     * Performs a binary search or cache lookup to find the first row that
	     * matches a given section.
	     * @param sectionIndex the section to search for
	     * @return the row index of the first occurrence, or the nearest next index.
	     * For instance, if section is '1', returns the first index of a received
	     * challenge.  If there are no received challenges, returns the length
	     * of the array.
	     */
		synchronized public int getPositionForSection(int sectionIndex) {
	    	
			// QUANTRO
	    	// If section index is 0, then no matter what, we return 0.
	    	if ( sectionIndex == SECTION_QUANTRO )
	    		return 0 ;
	    	
	    	// RETRO
	    	// If section index is 1, then return the first game which is NOT 3d,
	    	// or is custom.
	    	if ( sectionIndex == SECTION_RETRO ) {
	    		for ( int i = 0; i < mGameModes.length; i++ ) {
	    			if ( GameModes.isCustom(mGameModes[i]) || GameModes.numberQPanes(mGameModes[i]) == 1 ) {
	    				return i ;
	    			}
	    		}
	    	}
	    	
	    	// QUANTRO CUSTOM
	    	// If section index is 2, then return the first custom game.
	    	if ( sectionIndex == SECTION_CUSTOM_QUANTRO ) {
	    		for ( int i = 0; i < mGameModes.length; i++ ) {
	    			if ( GameModes.isCustom(mGameModes[i]) ) {
	    				return i ;
	    			}
	    		}
	    	}
	    	
	    	// RETRO CUSTOM
	    	// If section index is 3, then return the first custom game which is
	    	// 1-paned.
	    	if ( sectionIndex == SECTION_CUSTOM_RETRO ) {
	    		for ( int i = 0; i < mGameModes.length; i++ ) {
	    			if ( GameModes.isCustom(mGameModes[i]) && GameModes.numberQPanes(mGameModes[i]) == 1 ) {
	    				return i ;
	    			}
	    		}
	    	}
	    	
	    	// huh?
	    	return mGameModes.length ;
	    }
	    
	    /**
	     * Returns the section index for a given position in the list by
	     * examining the item.
	     */
		synchronized public int getSectionForPosition(int position) {
			if ( position < 0 || position >= mGameModes.length )
				return -1 ;
			boolean is3d = GameModes.numberQPanes(mGameModes[position]) == 2 ;
			boolean isCustom = GameModes.isCustom(mGameModes[position]) ;
			if ( is3d && !isCustom )
				return SECTION_QUANTRO ;
			if ( !is3d && !isCustom )
				return SECTION_RETRO ;
			if ( is3d && isCustom )
				return SECTION_CUSTOM_QUANTRO ;
			if ( !is3d && isCustom )
				return SECTION_CUSTOM_RETRO ;
			
	    	return 0 ;
	    }
		
		synchronized public boolean isFirstInSection( int position ) {
			if ( position < 0 || position >= mGameModes.length )
				return false ;
			if ( position == 0 )
				return true ;
			boolean is3d = GameModes.numberQPanes(mGameModes[position]) == 2 ;
			boolean isCustom = GameModes.isCustom(mGameModes[position]) ;
			boolean is3d_pre = GameModes.numberQPanes(mGameModes[position-1]) == 2 ;
			boolean isCustom_pre = GameModes.isCustom(mGameModes[position-1]) ;
			
			// a difference indicates change-of-section.
			return is3d != is3d_pre || isCustom != isCustom_pre ;
		}
	}
	
	
	private static class FreePlayGameLaunchButtonCellTag {
		SinglePlayerGameLaunchButtonStrip mSPGLBS ;
		FreePlayGameLaunchButtonCollage mFPGLBC ;
		
		public FreePlayGameLaunchButtonCellTag( View v ) {
			mSPGLBS = (SinglePlayerGameLaunchButtonStrip) v.findViewById( R.id.free_play_game_launch_button_strip ) ;
			mFPGLBC = (FreePlayGameLaunchButtonCollage) v.findViewById( R.id.free_play_game_mode_list_item_button_collage ) ;
		}
	}
	
	private static class FreePlayGameLaunchButtonRowTag {
		View mHeaderView ;
		View mHeaderViewTopSpacer ;
		TextView mHeaderTextView ;
		
		public FreePlayGameLaunchButtonRowTag( View v ) {
			mHeaderView = v.findViewById(R.id.quantro_list_item_header) ;
			mHeaderViewTopSpacer = v.findViewById(R.id.quantro_list_item_header_top_spacer) ;
			mHeaderTextView = (TextView)v.findViewById(R.id.quantro_list_item_header_text) ;
		}
	}
	
	
	private class FreePlayGameLaunchButtonCollageAdapter extends SectionableAdapter
			implements OnScrollListener, PinnedHeaderListView.PinnedHeaderAdapter, SectionIndexer {

		
		
		////////////////////////////////////////////////////////////////////////
		// LIST MANIPULATION
		// Changing the content - game modes - and their results / save data.
		/**
		 * Adds the specified object at the end of the array.
		 */
		synchronized public void add(Integer obj, GameResult gr, Boolean hasSave) {
			
			gameModes.add(obj) ;
			gameResults.add(gr) ;
			gameHasSave.add(hasSave == null ? Boolean.FALSE : hasSave) ;
			
			Integer [] array = new Integer[gameModes.size()] ;
			array = gameModes.toArray(array) ;
			mIndexer.setGameModes(array) ;
		}
		
		
		synchronized public void update( Integer obj, GameResult gr, Boolean hasSave ) {
			
			if ( gameModes.contains(obj) ) {
				gameResults.set( gameModes.indexOf(obj), gr ) ;
				gameHasSave.set( gameModes.indexOf(obj), hasSave ) ;
			}
			
		}
		
		synchronized public void clear() {
			
			gameModes.clear() ;
			gameResults.clear() ;
			gameHasSave.clear() ;
			mIndexer.setGameModes(new Integer[0]) ;
		}
		
		
		synchronized public void insert( Integer obj, GameResult gr, Boolean hasSave, int index ) {
			gameModes.add(index, obj) ;
			gameResults.add(index, gr) ;
			gameHasSave.add(index, hasSave) ;
			
			Integer [] array = new Integer[gameModes.size()] ;
			array = gameModes.toArray(array) ;
			mIndexer.setGameModes(array) ;
		}
		
		synchronized public void remove(Integer obj) {
			// find the index
			int index = gameModes.indexOf(obj) ;
			if ( index > -1 ) {
				gameModes.remove(index) ;
				gameResults.remove(index) ;
				gameHasSave.remove(index) ;
				
				Integer [] array = new Integer[gameModes.size()] ;
				array = gameModes.toArray(array) ;
				mIndexer.setGameModes(array) ;
			}
		}


		ArrayList<Integer> gameModes = new ArrayList<Integer>() ;
    	ArrayList<GameResult> gameResults = new ArrayList<GameResult>() ;
    	ArrayList<Boolean> gameHasSave = new ArrayList<Boolean>() ;
    	private SinglePlayerGameModeIndexer mIndexer ;
    	
    	boolean mScrolledToTop ;
    	
    	boolean mHasCollages = false ;
    	
		public FreePlayGameLaunchButtonCollageAdapter(LayoutInflater inflater,
				int rowLayoutID, int headerID, int itemHolderID) {
			
			super(inflater, rowLayoutID, headerID, itemHolderID);
			
			mIndexer = new SinglePlayerGameModeIndexer(new Integer[0]) ;
			mScrolledToTop = true ;
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
			FreePlayGameLaunchButtonRowTag tag = null ;
			if ( topChild != null )
				tag = ((FreePlayGameLaunchButtonRowTag)topChild.getTag()) ;
			
			mScrolledToTop = firstVisibleItem == 0 && visibleItemCount > 0 && topChild.getTop() == 0 ;
			boolean topHasHeader = mIndexer.isFirstInSection( getRealPosition(firstVisibleItem) ) ;
			if (view instanceof PinnedHeaderListView) {
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
			if (mIndexer == null || getDataCount() == 0) {
                return PINNED_HEADER_STATE_GONE;
            }

            if (position < 0) {
                return PINNED_HEADER_STATE_GONE;
            }
            
            if ( position == 0 && mScrolledToTop )
            	return PINNED_HEADER_STATE_GONE ;

            // The header should get pushed up if the top item shown
            // is the last item in a section for a particular letter.
            int section = getSectionForPosition( position );
            int nextSectionPosition = getPositionForSection( section + 1 ) ;
            
            if (nextSectionPosition != -1 && position == nextSectionPosition - 1) {
                return PINNED_HEADER_STATE_PUSHED_UP;
            }

            return PINNED_HEADER_STATE_VISIBLE;
		}
		
		@Override
        public int pha_getPinnedHeaderFadeAlphaStyle() {
			return PinnedHeaderListView.PinnedHeaderAdapter.PINNED_HEADER_FADE_ALPHA_HEADER_NONCONTENT_OVERLAP ;
			// return PinnedHeaderListView.PHA_DEFAULTS.pha_getPinnedHeaderFadeAlphaStyle() ;
		}
        
		Rect tempPaddingRect = new Rect() ;
        
		@Override
        public boolean copyPinnedHeaderOccludingBounds(View header, int position, Rect r) {
			FreePlayGameLaunchButtonRowTag tag = (FreePlayGameLaunchButtonRowTag)header.getTag() ;
        	Drawable bg = tag.mHeaderTextView.getBackground() ;
        	bg.getPadding(tempPaddingRect) ;
        	r.set(0, tag.mHeaderTextView.getTop() + tempPaddingRect.top,
        			header.getWidth(), tag.mHeaderTextView.getBottom() - tempPaddingRect.bottom) ;
        	return true ;
        }
        
        @Override
        public boolean copyPinnedHeaderVisibleBounds(View header, int position, Rect r) {
        	FreePlayGameLaunchButtonRowTag tag = (FreePlayGameLaunchButtonRowTag)header.getTag() ;
        	r.set(0, tag.mHeaderTextView.getTop(),
        			header.getWidth(), tag.mHeaderTextView.getBottom()) ;
        	return true ;
        }
        
        
        @Override
        public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
        	FreePlayGameLaunchButtonRowTag tag = (FreePlayGameLaunchButtonRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderOccludingBounds( tag.mHeaderView, position, r ) ;
        }
        
        @Override
        public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
        	FreePlayGameLaunchButtonRowTag tag = (FreePlayGameLaunchButtonRowTag)view.getTag() ;
        	if ( tag.mHeaderView.getVisibility() == View.GONE ) {
        		r.set(0, 0, 0, 0) ;
        		return true ;
        	}
        	
        	return copyPinnedHeaderVisibleBounds( tag.mHeaderView, position, r ) ;
        }
		
		@Override
		synchronized public void configurePinnedHeader(View v, int position, int alpha) {
			final int section = getSectionForPosition( position );
			final String title = (String) getSections()[section];
			
			FreePlayGameLaunchButtonRowTag tag = (FreePlayGameLaunchButtonRowTag)v.getTag() ;
			tag.mHeaderTextView.setText(title) ;
			tag.mHeaderViewTopSpacer.setVisibility(View.GONE) ;
			VersionSafe.setAlpha(v, alpha / 255f) ;
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
			return gameModes.get(position) ;
		}


		@Override
		protected int getDataCount() {
			return gameModes.size() ;
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
			return SinglePlayerGameModeIndexer.NUM_SECTIONS ;
		}


		@Override
		protected int getCountInSection(int index) {
			if ( mIndexer == null )
				return 0 ;
			
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
				return -1 ;
			
			return mIndexer.getSectionForPosition(position) ;
		}


		@Override
		protected String getHeaderForSection(int section) {
			return null ;
		}


		@Override
		protected void bindView(View cell, int position) {
			Integer gameModeInteger = gameModes.get(position) ;
			
    		FreePlayGameLaunchButtonCellTag tag = (FreePlayGameLaunchButtonCellTag)cell.getTag() ;
    		if ( tag == null ) {
				tag = new FreePlayGameLaunchButtonCellTag(cell) ;
				if ( tag.mSPGLBS != null ) {
					tag.mSPGLBS.setDelegate(FreePlayGameManagerStripView.this) ;
					tag.mSPGLBS.setColorScheme(mColorScheme) ;
				} 
				if ( tag.mFPGLBC != null ) {
					mHasCollages = true ;
					tag.mFPGLBC.setDelegate(FreePlayGameManagerStripView.this) ;
					tag.mFPGLBC.setColorScheme(mColorScheme) ;
					tag.mFPGLBC.setSoundPool(mSoundPool) ;
					tag.mFPGLBC.setSoundControls(mSoundControls) ;
					tag.mFPGLBC.setThumbnailSource(mSinglePlayerThumnbailCache, mSinglePlayerThumbnailLoader) ;
				}
				cell.setTag(tag) ;
			}
			// Set game mode, set the height to the ideal height, and refresh.
			if ( tag.mSPGLBS != null )
				tag.mSPGLBS.setGameMode(gameModeInteger, mAvailability.get(gameModeInteger)) ;
			if ( tag.mFPGLBC != null ) {
				mHasCollages = true ;
				tag.mFPGLBC.setGameMode(gameModeInteger, mAvailability.get(gameModeInteger)) ;
			}

			int index = gameModes.indexOf(gameModeInteger ) ;
			if ( tag.mSPGLBS != null )
				tag.mSPGLBS.refresh(gameResults.get( index ), gameHasSave.get( index )) ;
			if ( tag.mFPGLBC != null )
				tag.mFPGLBC.refresh(gameResults.get( index ), gameHasSave.get( index )) ;
			
			
			// Setting height causes a measure.  If this happens before we refresh() we get weird results.
			if ( tag.mSPGLBS != null )
				tag.mSPGLBS.getLayoutParams().height = tag.mSPGLBS.getIdealHeight() ;
		}
		
		
		/**
		 * Perform any row-specific customization your grid requires. For example, you could add a header to the
		 * first row or a footer to the last row.
		 * @param row the 0-based index of the row to customize.
		 * @param convertView the inflated row View.
		 */
		@Override
		protected void customizeRow(int row, int firstPosition, View rowView) {
			// This is where we perform necessary header configuration.
			
			FreePlayGameLaunchButtonRowTag tag = ((FreePlayGameLaunchButtonRowTag)rowView.getTag()) ;
			if ( tag == null ) {
				tag = new FreePlayGameLaunchButtonRowTag( rowView ) ;
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
    

	@Override
	public boolean fpglbcd_newGame(FreePlayGameLaunchButtonCollage collage,
			int gameMode, boolean customize, boolean hasSave) {
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		delegate.fpgmv_newGame(this, gameMode, customize) ;
		return mSoundControls ;
	}

	@Override
	public boolean fpglbcd_loadGame(FreePlayGameLaunchButtonCollage collage,
			int gameMode) {
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		delegate.fpgmv_loadGame(this, gameMode, false) ;
		return mSoundControls ;
	}

	@Override
	public boolean fpglbcd_examineGame(FreePlayGameLaunchButtonCollage collage,
			int gameMode) {
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		delegate.fpgmv_loadGame(this, gameMode, true) ;
		return mSoundControls ;
	}

	@Override
	public boolean fpglbcd_editGameMode(FreePlayGameLaunchButtonCollage collage,
			int gameMode) {
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		delegate.fpgmv_editGame(this, gameMode) ;
		return mSoundControls ;
	}


}
