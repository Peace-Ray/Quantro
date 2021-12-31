package com.peaceray.quantro.view.button.collage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.peaceray.android.graphics.BitmapSoftCache;
import com.peaceray.android.graphics.drawable.ScaledBitmapDrawable;
import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.model.GameSaver;
import com.peaceray.quantro.model.GameSaver.ThumbnailLoader;
import com.peaceray.quantro.model.GameSaver.ThumbnailLoader.Params;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.SupportsLongClickOracle;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.options.OptionAvailability;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;


public class FreePlayGameLaunchButtonCollage extends QuantroButtonCollage implements OnClickListener, OnLongClickListener, SupportsLongClickOracle, GameSaver.ThumbnailLoader.Client {
	
	public static final String TAG = "FPGLButtonCollage" ;
	
	public interface Delegate {
		
		/**
		 * Tells the delegate that the user wants to start a new game.
		 * Indicates whether there is an existing save for this game.
		 * 
		 * @param collage
		 * @param gameMode
		 * @return Should the appropriate button-press sound effect be played?
		 */
		public boolean fpglbcd_newGame( FreePlayGameLaunchButtonCollage collage, int gameMode, boolean customize, boolean hasSave ) ;
		
		/**
		 * Tells the delegate that the user wants to load an existing game.
		 * 
		 * @param collage
		 * @param gameMode
		 * @return Should the appropriate button-press sound effect be played?
		 */
		public boolean fpglbcd_loadGame( FreePlayGameLaunchButtonCollage collage, int gameMode ) ;
		
		/**
		 * Tells the delegate that the user wants to examine an existing game save.
		 * 
		 * @param collage
		 * @param gameMode
		 * @return Should the appropriate button-press sound effect be played?
		 */
		public boolean fpglbcd_examineGame( FreePlayGameLaunchButtonCollage collage, int gameMode ) ;
		
		
		/**
		 * Tells the delegate that the user wants to edit this game type.
		 * @param collage
		 * @param gameMode
		 * @return Should the appropriate button-press sound effect be played?
		 */
		public boolean fpglbcd_editGameMode( FreePlayGameLaunchButtonCollage collage, int gameMode ) ;
	}
	

	public FreePlayGameLaunchButtonCollage(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public FreePlayGameLaunchButtonCollage(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
		
		
	}
	
	public FreePlayGameLaunchButtonCollage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues( Context context ) {
		mButtonTypeColor = new int[NUM_BUTTON_TYPES] ;
		mButtonTypeImageDrawable = new Drawable[NUM_BUTTON_TYPES] ;
		mTimedAlertDrawable = null ;
		
		mRefreshRunnable = new Runnable() {
			public void run() {
				refresh();
			}
		} ;
	}
	
	
	private void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.FreePlayGameLaunchButtonCollage);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.FreePlayGameLaunchButtonCollage_image_drawable_first_game_button:
				mButtonTypeImageDrawable[BUTTON_TYPE_FIRST] = a.getDrawable(attr) ;
				break ;
			case R.styleable.FreePlayGameLaunchButtonCollage_image_drawable_resume_game_button:
				mButtonTypeImageDrawable[BUTTON_TYPE_RESUME] = a.getDrawable(attr) ;
				break ;
			case R.styleable.FreePlayGameLaunchButtonCollage_image_drawable_new_game_button:
				mButtonTypeImageDrawable[BUTTON_TYPE_NEW] = a.getDrawable(attr) ;
				break ;
			case R.styleable.FreePlayGameLaunchButtonCollage_image_drawable_edit_game_mode_button:
				mButtonTypeImageDrawable[BUTTON_TYPE_EDIT_GAME_MODE] = a.getDrawable(attr) ;
				break ;
			case R.styleable.FreePlayGameLaunchButtonCollage_alert_drawable_timed:
				mTimedAlertDrawable = a.getDrawable(attr) ;
				break ;
			}
		}
		
		a.recycle() ;
	}
	
	
	private void constructor_allocateAndInitialize( Context context ) {
		refreshColors() ;
	}
	
	
	private static final int BUTTON_TYPE_FIRST = 0 ;
	private static final int BUTTON_TYPE_RESUME = 1 ;
	private static final int BUTTON_TYPE_NEW = 2 ;
	private static final int BUTTON_TYPE_EDIT_GAME_MODE = 3 ;
	private static final int NUM_BUTTON_TYPES = 4 ;
	
	
	private static final int BUTTON_NUM_MAIN = 0 ;
	private static final int BUTTON_NUM_NEW = 1 ;
	private static final int BUTTON_NUM_EDIT = 2 ;
	private static final int NUM_BUTTONS = 3 ;
	
	
	// We need references to a bunch of stuff.  There are two, possibly three
	// buttons to be concerned with: our main launch button and a 'new game' button,
	// and possibly a "custom edit" button.
	private QuantroContentWrappingButton [] mButtons ;
	private QuantroButtonDirectAccess [] mButtonContent ;
	private int [] mButtonTypeColor ;
	private Drawable [] mButtonTypeImageDrawable ;
	
	private Drawable mTimedAlertDrawable ;
	
	// we also need a place for the label
	private TextView mLabelView ;
	private String mLabel ;
	
	
	// Defines our content
	private int mGameMode ;
	private boolean mHasSave ;
	private GameResult mGameResult ;
	private OptionAvailability  mAvailability ;
	
	private ColorScheme mColorScheme ;
	private QuantroSoundPool mSoundPool ;
	private boolean mSoundControls ;
	
	// Thumbnail loaders and cachers!
	private BitmapSoftCache mThumbnailSoftCache ;
	private GameSaver.ThumbnailLoader mThumbnailLoader ;
	private int mLastLoadedThumbnailGameMode ;
	
	// Delegate!
	protected WeakReference<Delegate> mwrDelegate ;
	
	// Runnable
	protected Runnable mRefreshRunnable ;
	
	
	public void setDelegate( Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	public void setColorScheme( ColorScheme cs ) {
		this.mColorScheme = cs ;
	}
	
	public void setSoundPool( QuantroSoundPool pool ) {
		this.mSoundPool = pool ;
	}
	
	public void setSoundControls( boolean soundControls ) {
		this.mSoundControls = soundControls ;
	}
	
	public void setThumbnailSource( BitmapSoftCache cache, GameSaver.ThumbnailLoader loader ) {
		this.mThumbnailSoftCache = cache ;
		this.mThumbnailLoader = loader ;
	}
	
	public void setGameMode( int gameMode, OptionAvailability availability ) {
		this.mGameMode = gameMode ;
		this.mLastLoadedThumbnailGameMode = -1 ;
		this.mAvailability = availability ;
		refreshColors() ;
	}
	
	
	private void refreshColors() {
		// set each color (if not set) according to GameModeColors.
		for ( int i = 0; i < NUM_BUTTON_TYPES; i++ )
			mButtonTypeColor[i] = 0xffff00ff ;
		
		if ( GameModeColors.hasPrimary(mGameMode) && mColorScheme != null ) {
			mButtonTypeColor[BUTTON_TYPE_FIRST] = mButtonTypeColor[BUTTON_TYPE_RESUME] =
				GameModeColors.primary(mColorScheme, mGameMode) ;
		}
		
		if ( GameModeColors.hasSecondary(mGameMode) && mColorScheme != null )
			mButtonTypeColor[BUTTON_TYPE_NEW] = GameModeColors.secondary(mColorScheme, mGameMode) ;
		
		if ( GameModeColors.hasTertiary(mGameMode) && mColorScheme != null )
			mButtonTypeColor[BUTTON_TYPE_EDIT_GAME_MODE] = GameModeColors.tertiary(mColorScheme, mGameMode) ;
	}
	
	
	@Override
	public void refresh() {
		refresh(mGameResult, mHasSave) ;
	}
	
	private boolean refresh_ever = false ;
	private boolean refresh_last_hasSave = false ;
	private int refresh_last_gameMode = -1 ;
	private Bitmap refresh_last_gameResultThumbnail = null ;
	public void refresh(GameResult gr, boolean hasSave) {
		//Log.d(TAG, "refresh.  game mode " + mGameMode + " from previous " + refresh_last_gameMode) ;
		//long time = System.currentTimeMillis() ;
		// Determine if the content changed.  If so, call super.refresh.
		boolean changed = false ;
		
		if ( !refresh_ever ) {
			collectAllContent() ;
			refresh_ever = true ;
			changed = true ;
			//Log.d(TAG, "past collect content in " + (System.currentTimeMillis() - time)) ;
		}
		
		// Do we have a save?
		mHasSave = hasSave ;
		mGameResult = gr ;
		
		changed = changed || refresh_last_hasSave != mHasSave ;
		changed = changed || refresh_last_gameMode != mGameMode ;
		refresh_last_hasSave = mHasSave ;
		refresh_last_gameMode = mGameMode ;
		
		if ( !mHasSave ) {
			changed = setButtonContent( mButtons[BUTTON_NUM_MAIN], mButtonContent[BUTTON_NUM_MAIN], BUTTON_TYPE_FIRST ) || changed ;
			//Log.d(TAG, "past set main button content " + (System.currentTimeMillis() - time)) ;
			mButtons[BUTTON_NUM_MAIN].setVisibility(View.VISIBLE) ;
			mButtons[BUTTON_NUM_NEW].setVisibility(View.GONE) ;
		} else {
			changed = setButtonContent( mButtons[BUTTON_NUM_MAIN], mButtonContent[BUTTON_NUM_MAIN], BUTTON_TYPE_RESUME ) || changed ;
			//Log.d(TAG, "past set main button content " + (System.currentTimeMillis() - time)) ;
			changed = setButtonContent( mButtons[BUTTON_NUM_NEW], mButtonContent[BUTTON_NUM_NEW], BUTTON_TYPE_NEW ) || changed ;
			//Log.d(TAG, "past set new button content " + (System.currentTimeMillis() - time)) ;
			
			mButtons[BUTTON_NUM_MAIN].setVisibility(View.VISIBLE) ;
			mButtons[BUTTON_NUM_NEW].setVisibility(View.VISIBLE) ;
		}
		//Log.d(TAG, "past setButtonContent in " + (System.currentTimeMillis() - time)) ;
		
		if ( GameModes.isCustom(mGameMode) ) {
			changed = setButtonContent( mButtons[BUTTON_NUM_EDIT], mButtonContent[BUTTON_NUM_EDIT], BUTTON_TYPE_EDIT_GAME_MODE ) || changed ;
			mButtons[BUTTON_NUM_EDIT].setVisibility(View.VISIBLE) ;
		} else {
			mButtons[BUTTON_NUM_EDIT].setVisibility(View.GONE) ;
		}
		//Log.d(TAG, "past set edit button in " + (System.currentTimeMillis() - time)) ;
		
		
		// set the label.
		// String label = GameModes.className(mGameMode) ;
		String label = GameModes.name(mGameMode) ;
		if ( !label.equals(mLabel) ) {
			mLabelView.setText(label) ;
			changed = true ;
			//Log.d(TAG, "past set label in " + (System.currentTimeMillis() - time)) ;
		}
		
		// set the main background.
		Bitmap bg = null ;
		if ( hasSave && mThumbnailLoader != null && mThumbnailSoftCache != null ) {
			//Log.d(TAG, "refresh hasSave, getting thumbnail") ;
			bg = mThumbnailSoftCache == null ? null : mThumbnailSoftCache.get(Integer.valueOf(mGameMode)) ;
			if ( bg != null && bg != refresh_last_gameResultThumbnail ) {
				//Log.d(TAG, "refresh bg != null, setting as drawable") ;
				ScaledBitmapDrawable sbd = new ScaledBitmapDrawable( bg, ScaledBitmapDrawable.ScaleType.CENTER_MATCH_CROP ) ;
				mButtons[BUTTON_NUM_MAIN].setFillDrawable(sbd, true) ;
				changed = true ;
			} else if ( bg == null ) {
				// load?
				if ( mLastLoadedThumbnailGameMode != mGameMode ) {
					//Log.d(TAG, "refresh bg == null, adding as loading client") ;
					mThumbnailLoader.addClient(this) ;
				}
				if ( refresh_last_gameResultThumbnail != null ) {
					//Log.d(TAG, "refresh bg == null, setting null background") ;
					mButtons[BUTTON_NUM_MAIN].setFillDrawable(null, false) ;
					changed = true ;
				}
			}
			
			refresh_last_gameResultThumbnail = bg ;
		} else {
			//Log.d(TAG, "refresh does not have save") ;
			if ( refresh_last_gameResultThumbnail != null ) {
				//Log.d(TAG, "refresh setting null background") ;
				mButtons[BUTTON_NUM_MAIN].setFillDrawable(null, false) ;
				changed = true ;
			}
			refresh_last_gameResultThumbnail = null ;
		}
		
		if ( changed )
			super.refresh() ;
		
		//Log.d(TAG, "refresh took " + (System.currentTimeMillis() - time)) ;
	}
	
	
	/**
	 * Collects necessary references, sets listeners, creates 
	 * QuantroButtonContent objects, etc.
	 */
	private void collectAllContent() {
		
		// collect buttons.
		ArrayList<Integer> ids = new ArrayList<Integer>() ;
		
		ids.add( R.id.button_collage_free_play_button_main ) ;
		ids.add( R.id.button_collage_free_play_button_new ) ;
		ids.add( R.id.button_collage_free_play_button_edit ) ;
		
		ArrayList<QuantroContentWrappingButton> buttons = collectButtons(ids) ;
		
		mButtons = new QuantroContentWrappingButton[NUM_BUTTONS] ;
		mButtons[BUTTON_NUM_MAIN] = buttons.get(0) ;
		mButtons[BUTTON_NUM_NEW] = buttons.get(1) ;
		mButtons[BUTTON_NUM_EDIT] = buttons.get(2) ;
		
		// construct and associate contents.
		mButtonContent = new QuantroButtonDirectAccess[NUM_BUTTONS] ;
		for ( int i = 0; i < NUM_BUTTONS; i++ ) {
			QuantroContentWrappingButton qcwb = mButtons[i] ;
			if ( qcwb != null ) {
				mButtonContent[i] = (QuantroButtonDirectAccess)QuantroButtonDirectAccess.wrap( qcwb ) ;
				qcwb.setTag(mButtonContent[i]) ;
			}
		}
		
		// assign listeners
		for ( int i = 0; i < NUM_BUTTONS; i++ ) {
			QuantroContentWrappingButton qcwb = mButtons[i] ;
			if ( qcwb != null ) {
				qcwb.setOnClickListener(this) ;
				qcwb.setOnLongClickListener(this) ;
				qcwb.setSupportsLongClickOracle(this) ;
			}
		}
		
		
		
		// label
		mLabelView = (TextView)findViewById(R.id.button_collage_label) ;
		mLabel = mLabelView.getText().toString() ;
	}
	
	
	private boolean setButtonContent( QuantroContentWrappingButton button, QuantroButtonDirectAccess content, int buttonType ) {
		boolean changed = false ;
		
		int typeTitle = -1, typeDescription = -1, typeLongDescription = -1 ;
		int display = TextFormatting.DISPLAY_MENU ;
		int role = TextFormatting.ROLE_HOST ;
		
		boolean enabled = mAvailability.isEnabled() ;
		boolean timed = mAvailability.isTimed() ;
		
		// Color and background drawable
		int baseColor = 0 ;
		Drawable imageDrawable ;
		Drawable alertDrawable = null ;
		
		// HACK: We cheat a bit.  To make menus load faster, we allow the "load" button to
		// carry standard text if a game result object is not provided.
		/*	Weird; shows 2 'plus' signs.
		if ( mHasSave && mGameResult == null && buttonType == BUTTON_TYPE_RESUME )
			buttonType = BUTTON_TYPE_FIRST ;
			*/
		
		switch( buttonType ) {
		case BUTTON_TYPE_FIRST:
			//typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_TITLE ;
			//typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_DESCRIPTION ;
			alertDrawable = timed ? mTimedAlertDrawable : null ;
			break ;
		case BUTTON_TYPE_RESUME:
			//typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_TITLE ;
			//typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_DESCRIPTION ;
			typeLongDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_LONG_DESCRIPTION ;
			alertDrawable = timed ? mTimedAlertDrawable : null ;
			break ;
		case BUTTON_TYPE_NEW:
			//typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_NEW_GAME_TITLE ;
			//typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_NEW_GAME_DESCRIPTION ;
			break ;
		case BUTTON_TYPE_EDIT_GAME_MODE:
			//typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_TITLE ;
			//typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_DESCRIPTION ;
			break ;
		default:
			//Log.e(TAG, "setButtonContent: unrecognized buttonType " + buttonType) ;
			return false ;
		}
		
		baseColor = mButtonTypeColor[buttonType] ;
		imageDrawable = mButtonTypeImageDrawable[buttonType] ;
		
		Context context = getContext() ;
		Resources res = context.getResources() ;
		String textTitle = typeTitle == -1 ? null : TextFormatting.format(context, res, display, typeTitle, role, mGameMode, mGameResult) ;
		String textDescription = typeDescription == -1 ? null : TextFormatting.format(context, res, display, typeDescription, role, mGameMode, mGameResult) ;
		String textLongDescription = ( typeLongDescription == -1 || mGameResult == null ) ? null : TextFormatting.format(context, res, display, typeLongDescription, role, mGameMode, mGameResult) ;
		
		// Now set these values in the appropriate content fields.
		changed = content.setTitle(textTitle) 					|| changed ;
		changed = content.setDescription(textDescription) 		|| changed ;
		changed = content.setLongDescription(textLongDescription) || changed ;
		changed = ( imageDrawable == null
				? content.setImageInvisible()
				: content.setImage(true, imageDrawable) )		|| changed ;
		changed = ( alertDrawable == null
				? content.setAlertInvisible()
				: content.setAlert(true, alertDrawable)	)		|| changed ;
		changed = content.setColor(baseColor)					|| changed ;
		
		changed = button.isEnabled() != enabled 				|| changed ;
		button.setEnabled(enabled) ;
		
		return changed ;
	}
	
	

	@Override
	public void onClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		boolean sound = false ;
		
		if ( v == mButtons[BUTTON_NUM_MAIN] ) {
			// could be start new or resume.
			if ( mHasSave )
				sound = delegate.fpglbcd_loadGame(this, mGameMode) ;
			else
				sound = delegate.fpglbcd_newGame(this, mGameMode, false, false) ;
		}
		else if ( v == mButtons[BUTTON_NUM_NEW] ) {
			// is only and always a new game
			sound = delegate.fpglbcd_newGame(this, mGameMode, false, mHasSave) ;
		}
		else if ( v == mButtons[BUTTON_NUM_EDIT] ) {
			// is only and always 'edit'
			sound = delegate.fpglbcd_editGameMode(this, mGameMode) ;
		}
		
		if ( mSoundPool != null && mSoundControls && sound )
			mSoundPool.menuButtonClick() ;
	}
	

	@Override
	public boolean onLongClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		boolean sound = false ;
		
		if ( v == mButtons[BUTTON_NUM_MAIN] ) {
			// could be start new or examine.
			if ( mHasSave )
				sound = delegate.fpglbcd_examineGame(this, mGameMode) ;
			else
				sound = delegate.fpglbcd_newGame(this, mGameMode, true, false) ;
			
			// play sound
			if ( mSoundPool != null && mSoundControls && sound )
				mSoundPool.menuButtonHold() ;
			
			return true ;
		}
		else if ( v == mButtons[BUTTON_NUM_NEW] ) {
			// is only and always a new game
			sound = delegate.fpglbcd_newGame(this, mGameMode, true, mHasSave) ;
			if ( mSoundPool != null && mSoundControls && sound )
				mSoundPool.menuButtonHold() ;
			
			return true ;
		}
		
		return false ;
	}

	

	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		if ( qcwb == mButtons[BUTTON_NUM_MAIN] ) {
			// Either 'new' or 'remove.'
			return true ;
		}
		else if ( qcwb == mButtons[BUTTON_NUM_NEW] ) {
			// new game
			return true ;
		}
		else if ( qcwb == mButtons[BUTTON_NUM_EDIT] ) {
			return false ;
		}
		return false ;
	}

	@Override
	public Params tlc_getParams(ThumbnailLoader tl) {
		//Log.d(TAG, "tlc_getParams with game mode " + mGameMode + " obj " + this) ;
		boolean loaded = mLastLoadedThumbnailGameMode == mGameMode ;
		mLastLoadedThumbnailGameMode = mGameMode ;
		if ( mHasSave && !loaded && mGameMode >= 0 ) {
			//Log.d(TAG, "tlc_getParams requires a load") ;
			Params params = new Params() ;
			// set our scale -- match the big content size.
			int dimen = getContext().getResources().getDimensionPixelSize(R.dimen.button_big_content_size) ;
			params.scaleType = Params.ScaleType.FIT_X_OR_Y ;
			params.width = params.height = dimen ;
			params.loadKey = GameSaver.freePlayGameModeToSaveKey(mGameMode) ;
			params.gameMode = mGameMode ;
			return params ;
		}
		
		return null ;
	}

	@Override
	public void tlc_hasLoaded(ThumbnailLoader tl, Params p, Bitmap b) {
		//Log.d(TAG, "tlc_hasLoaded with game mode " + mGameMode + " obj " + this) ;
		if ( p.gameMode == mGameMode && b != null ) {
			//Log.d(TAG, "tlc_hasLoaded game mode matches with bitmap " + b + " config " + b.getConfig() +  " has alpha " + b.hasAlpha()) ;
			mThumbnailSoftCache.put(Integer.valueOf(mGameMode), b) ;
			((Activity)getContext()).runOnUiThread(mRefreshRunnable) ;
		}
	}
	
}
