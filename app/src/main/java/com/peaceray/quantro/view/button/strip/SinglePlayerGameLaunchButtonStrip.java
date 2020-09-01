package com.peaceray.quantro.view.button.strip;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.options.OptionAvailability;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

/**
 * A game-launch button strip for single player games.
 * 
 * CURRENT FEATURES (11/16/11)
 * Nothing; just started
 * 
 * PLANNED FEATURES
 * Uses a saved game key to determine if a save exists.
 * 		If 'no', displays "New Game" as its main button.
 * 		If 'yes', displays "Continue" as its main button and "New Game" as a sub.
 * 
 * Single-press to launch GameActivities.
 * Long-press to open a context menu - for new game, delete save, game info, etc.
 * 		We don't really want to get in the habit of Views launching activities
 * 		or dialogs itself, so we pass these things to the Activity.  However,
 * 		we provide a moderate translation of these events, from e.g. "onLongClick" to
 * 		"longClickOnNewGameButton," "longClickOnContinueGameButton," etc.  These methods
 * 		provide plenty of additional information for the handling object beyond the touched
 * 		view: the view strip, the child number, the game mode and key, etc.
 * 		
 * 
 * @author Jake
 *
 */
public class SinglePlayerGameLaunchButtonStrip extends QuantroButtonStrip 
	implements QuantroButtonStrip.Controller {
	
	public interface Delegate {
		/**
		 * The user has short-clicked a button we for game launches.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param gameMode
		 * @param saveKey
		 */
		public boolean spglbs_onButtonClick(
				SinglePlayerGameLaunchButtonStrip strip,
				int buttonType, int gameMode ) ;
		
		/**
		 * The user has long-clicked a button we for game launches.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param gameMode
		 * @param saveKey
		 */
		public boolean spglbs_onButtonLongClick(
				SinglePlayerGameLaunchButtonStrip strip,
				int buttonType, int gameMode ) ;
		
		
		/**
		 * Will a call to spglbs_onButtonLongClick with these parameters return 'true'?
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param gameMode
		 * @return
		 */
		public boolean spglbs_supportsLongClick(
				SinglePlayerGameLaunchButtonStrip strip,
				int buttonType, int gameMode ) ;
	}

	private static final String TAG = "SinglePlayerGameLaunchButtonStrip" ;
	
	public SinglePlayerGameLaunchButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public SinglePlayerGameLaunchButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	// Set this as the game mode for a button representing a "new custom game mode" (probably just an icon)
	public static final int GAME_MODE_NEW_CUSTOM_GAME_MODE = -2 ;
	
	// we need at least 3 buttons: a new/continue, delete/new, and an edit (custom only)
	private static final int MIN_BUTTONS = 3 ;
	
	public static final int BUTTON_TYPE_MAIN = 0 ;
	public static final int BUTTON_TYPE_FIRST = 1 ;
	public static final int BUTTON_TYPE_RESUME = 2 ;
	public static final int BUTTON_TYPE_NEW = 3 ;
	public static final int BUTTON_TYPE_NEW_GAME_MODE = 4 ;
	public static final int BUTTON_TYPE_EDIT_GAME_MODE = 5 ;
	
	
	
	protected int mGameMode ;
	protected boolean mHasSave ;
	protected boolean mFirstGameIsMain ;
	protected GameResult mGameResult ;
	
	protected OptionAvailability mAvailability ;
	
	// Our listener
	protected WeakReference<Delegate> mwrDelegate ;
	
	// Our color scheme
	protected ColorScheme mColorScheme ;
	
	// Some colors and background drawables for our buttons.
	protected int mBaseColorButtonMain ;
	protected int mBaseColorButtonFirst ;
	protected int mBaseColorButtonResume ;
	protected int mBaseColorButtonNew ;
	protected int mBaseColorButtonNewGameMode ;
	protected int mBaseColorButtonEditGameMode ;
	// Do we have these colors?
	protected boolean mHasBaseColorButtonMain ;
	protected boolean mHasBaseColorButtonFirst ;
	protected boolean mHasBaseColorButtonResume ;
	protected boolean mHasBaseColorButtonNew ;
	protected boolean mHasBaseColorButtonNewGameMode ;
	protected boolean mHasBaseColorButtonEditGameMode ;
	
	// Background drawables for our buttons
	protected Drawable mImageDrawableButtonMain ;
	protected Drawable mImageDrawableButtonFirst ;
	protected Drawable mImageDrawableButtonResume ;
	protected Drawable mImageDrawableButtonNew ;
	protected Drawable mImageDrawableButtonNewGameMode ;
	protected Drawable mImageDrawableButtonEditGameMode ;
	
	protected Drawable mAlertDrawableTimed ;
	
	private void constructor_setDefaultValues(Context context) {
		this.setController(this) ;
		
		mGameMode = -1 ;
		mHasSave = false ;
		mFirstGameIsMain = true ;
		mGameResult = null ;
		
		// Base colors!
		mBaseColorButtonMain = 0 ;
		mBaseColorButtonFirst = 0 ;
		mBaseColorButtonResume = 0 ;
		mBaseColorButtonNew = 0 ;
		mBaseColorButtonNewGameMode = 0 ;
		mBaseColorButtonEditGameMode = 0 ;
		
		// Do we have these colors?
		mHasBaseColorButtonMain = false ;
		mHasBaseColorButtonFirst = false ;
		mHasBaseColorButtonResume = false ;
		mHasBaseColorButtonNew = false ;
		mHasBaseColorButtonNewGameMode = false ;
		mHasBaseColorButtonEditGameMode = false ;
		
		// Background drawables for our buttons
		mImageDrawableButtonMain = null ;
		mImageDrawableButtonFirst = null ;
		mImageDrawableButtonResume = null ;
		mImageDrawableButtonNew = null ;
		mImageDrawableButtonNewGameMode = null ;
		mImageDrawableButtonEditGameMode = null ;
		
		mAlertDrawableTimed = null ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.SinglePlayerGameLaunchButtonStrip);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_game_mode:
				mGameMode = a.getInt(attr, mGameMode) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_first_game_is_main:
				mFirstGameIsMain = a.getBoolean(attr, true) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_base_color_first_game_button:
				mBaseColorButtonFirst = a.getColor(attr, mBaseColorButtonFirst) ;
				mHasBaseColorButtonFirst = true ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_base_color_resume_game_button:
				mBaseColorButtonResume = a.getColor(attr, mBaseColorButtonResume) ;
				mHasBaseColorButtonResume = true ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_base_color_new_game_button:
				mBaseColorButtonNew = a.getColor(attr, mBaseColorButtonNew) ;
				mHasBaseColorButtonNew = true ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_base_color_new_game_mode_button:
				mBaseColorButtonNewGameMode = a.getColor(attr, mBaseColorButtonNewGameMode) ;
				mHasBaseColorButtonNewGameMode = true ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_base_color_edit_game_mode_button:
				mBaseColorButtonEditGameMode = a.getColor(attr, mBaseColorButtonEditGameMode) ;
				mHasBaseColorButtonEditGameMode = true ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_image_drawable_main_game_button:
				mImageDrawableButtonMain = a.getDrawable(attr) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_image_drawable_first_game_button:
				mImageDrawableButtonFirst = a.getDrawable(attr) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_image_drawable_resume_game_button:
				mImageDrawableButtonResume = a.getDrawable(attr) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_image_drawable_new_game_button:
				mImageDrawableButtonNew = a.getDrawable(attr) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_image_drawable_new_game_mode_button:
				mImageDrawableButtonNewGameMode = a.getDrawable(attr) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_image_drawable_edit_game_mode_button:
				mImageDrawableButtonEditGameMode = a.getDrawable(attr) ;
				break ;
			case R.styleable.SinglePlayerGameLaunchButtonStrip_sp_alert_drawable_timed:
				mAlertDrawableTimed = a.getDrawable(attr) ;
				break ;
			}
		}
		
		// recycle the array
		a.recycle() ;
	}
	
	public void constructor_allocateAndInitialize(Context context) {
		// if we have a game mode, set the colors we don't have according
		// to GameModeColors.
		
		refreshColors() ;
	}
	
	
	public void setDelegate( SinglePlayerGameLaunchButtonStrip.Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	public void setColorScheme( ColorScheme cs ) {
		this.mColorScheme = cs ;
	}
	
	
	@Override
	public void refresh() {
		refresh(mGameResult, mHasSave) ;
	}
	
	private boolean refresh_last_hasSave = false ;
	public void refresh(GameResult gr, boolean hasSave) {
		// Determine if the content changed.  If so, call super.refresh.
		boolean changed = false ;
		
		// Make sure we have enough buttons!
		while ( this.getButtonCount() < MIN_BUTTONS ) {
			addButton() ;
			changed = true ;
		}
		
		// We set button content and visiblity based on the provided
		// game mode and save key.  Every button beyond the second should
		// be GONE.
		for ( int i = 3; i < getButtonCount(); i++ )
			setButtonVisible(i, false) ;
		
		// Do we have a save?
		mHasSave = hasSave ;
		mGameResult = gr ;
		
		changed = changed || refresh_last_hasSave != mHasSave ;
		refresh_last_hasSave = mHasSave ;
		
		if ( mFirstGameIsMain ) {
			if ( mGameMode == GAME_MODE_NEW_CUSTOM_GAME_MODE ) {
				// set as BUTTON_TYPE_NEW_GAME_MODE.
				changed = setButtonContent( 0, BUTTON_TYPE_NEW_GAME_MODE, null )	|| changed ;
				setButtonVisible( 0, true ) ;
				setButtonVisible( 1, false ) ;
			}
			else if( mHasSave ) {
				// Load the game result.  We set the "resume" and "new"
				// button content appropriately.
				changed = setButtonContent( 0, BUTTON_TYPE_RESUME, gr ) 	|| changed ;
				changed = setButtonContent( 1, BUTTON_TYPE_NEW, gr ) 		|| changed ; 
				
				// Both buttons are visible.
				setButtonVisible( 0, true ) ;
				setButtonVisible( 1, true ) ;
				
				if ( GameModes.isCustom(mGameMode) )
					changed = setButtonContent( 2, BUTTON_TYPE_EDIT_GAME_MODE, null ) || changed ;
				setButtonVisible( 2, GameModes.isCustom(mGameMode) ) ;
			}
			else {
				// If there is no save, format a START_GAME button as the first,
				// and set the second to GONE.
				changed = setButtonContent( 0, BUTTON_TYPE_FIRST, null ) 	|| changed ;
				
				setButtonVisible( 0, true ) ;
				setButtonVisible( 1, false ) ;
				
				if ( GameModes.isCustom(mGameMode) )
					changed = setButtonContent( 2, BUTTON_TYPE_EDIT_GAME_MODE, null ) || changed ;
				setButtonVisible( 2, GameModes.isCustom(mGameMode) ) ;
			}
		} else {
			if ( mGameMode == GAME_MODE_NEW_CUSTOM_GAME_MODE ) {
				// set as BUTTON_TYPE_NEW_GAME_MODE.
				changed = setButtonContent( 0, BUTTON_TYPE_NEW_GAME_MODE, null )	|| changed ;
				setButtonVisible( 0, true ) ;
				setButtonVisible( 1, false ) ;
			}
			else if( mHasSave ) {
				// Load the game result.  We set the "resume" and "new"
				// button content appropriately.
				changed = setButtonContent( 0, BUTTON_TYPE_RESUME, gr ) 	|| changed ;
				changed = setButtonContent( 1, BUTTON_TYPE_NEW, gr ) 		|| changed ; 
				
				// Both buttons are visible.
				setButtonVisible( 0, true ) ;
				setButtonVisible( 1, true ) ;
				
				if ( GameModes.isCustom(mGameMode) )
					changed = setButtonContent( 2, BUTTON_TYPE_EDIT_GAME_MODE, null ) || changed ;
				setButtonVisible( 2, GameModes.isCustom(mGameMode) ) ;
			}
			else {
				// If there is no save, format a START_GAME button as the first,
				// and set the second to GONE.
				changed = setButtonContent( 0, BUTTON_TYPE_MAIN, null ) 	|| changed ;
				changed = setButtonContent( 1, BUTTON_TYPE_FIRST, null ) 	|| changed ;
				
				setButtonVisible( 0, true ) ;
				setButtonVisible( 1, true ) ;
				
				if ( GameModes.isCustom(mGameMode) )
					changed = setButtonContent( 2, BUTTON_TYPE_EDIT_GAME_MODE, null ) || changed ;
				setButtonVisible( 2, GameModes.isCustom(mGameMode) ) ;
			}
		}

		if ( changed )
			super.refresh() ;
	}
	
	
	
	
	public void setGameMode( int gameMode, OptionAvailability availability ) {
		this.mGameMode = gameMode ;
		this.mAvailability = availability ;
		refreshColors() ;
	}
	
	
	private void refreshColors() {
		// set each color (if not set) according to GameModeColors.
		if ( mGameMode == GAME_MODE_NEW_CUSTOM_GAME_MODE ) {
			int qpanes = GameModes.numberQPanes(mGameMode) ;
			if ( !mHasBaseColorButtonMain && mColorScheme != null )
				mBaseColorButtonMain = GameModeColors.customPrimary(mColorScheme, qpanes) ;
			if ( !mHasBaseColorButtonFirst && mColorScheme != null )
				mBaseColorButtonFirst = GameModeColors.customPrimary(mColorScheme, qpanes) ;
			if ( !mHasBaseColorButtonResume && mColorScheme != null )
				mBaseColorButtonResume = GameModeColors.customPrimary(mColorScheme, qpanes) ;
			if ( !mHasBaseColorButtonNew && mColorScheme != null )
				mBaseColorButtonNew = GameModeColors.customSecondary(mColorScheme, qpanes) ;
			if ( !mHasBaseColorButtonNewGameMode && mColorScheme != null )
				mBaseColorButtonNewGameMode = GameModeColors.customTertiary(mColorScheme, qpanes) ;
			if ( !mHasBaseColorButtonEditGameMode && mColorScheme != null )
				mBaseColorButtonEditGameMode = GameModeColors.customTertiary(mColorScheme, qpanes) ;
		}
		
		else {
			if ( !mHasBaseColorButtonMain && GameModeColors.hasPrimary(mGameMode) && mColorScheme != null )
				mBaseColorButtonMain = GameModeColors.primary(mColorScheme, mGameMode) ;
			if ( !mHasBaseColorButtonFirst && GameModeColors.hasPrimary(mGameMode) && mColorScheme != null )
				mBaseColorButtonFirst = GameModeColors.primary(mColorScheme, mGameMode) ;
			if ( !mHasBaseColorButtonResume && GameModeColors.hasPrimary(mGameMode) && mColorScheme != null )
				mBaseColorButtonResume = GameModeColors.primary(mColorScheme, mGameMode) ;
			if ( !mHasBaseColorButtonNew && GameModeColors.hasSecondary(mGameMode) && mColorScheme != null )
				mBaseColorButtonNew = GameModeColors.secondary(mColorScheme, mGameMode) ;
			if ( !mHasBaseColorButtonNewGameMode && GameModeColors.hasTertiary(mGameMode) && mColorScheme != null )
				mBaseColorButtonNewGameMode = GameModeColors.tertiary(mColorScheme, mGameMode) ;
			if ( !mHasBaseColorButtonEditGameMode && GameModeColors.hasTertiary(mGameMode) && mColorScheme != null )
				mBaseColorButtonEditGameMode = GameModeColors.tertiary(mColorScheme, mGameMode) ;
		}
	}
	
	
	/**
	 * Returns whether this call altered the content of the button in such a way
	 * that their layout (not just their content) should be recalculated.
	 * 
	 * For example, if the only change is a button base color, returns 'false',
	 * but if text changed, returns 'true' (such a change might alter the dimensions
	 * of the button).
	 * 
	 * @param buttonIndex
	 * @param buttonType
	 * @param gr
	 * @return
	 */
	private boolean setButtonContent( int buttonIndex, int buttonType, GameResult gr ) {
		// We use TextFormatting to load and format strings with the appropriate info
		// inserted.  Then, searching the button's content view by Tag for the appropriate
		// view, we insert that value (if a view for it is found).
		
		// TODO: Set to false here, then to 'true' if any content changed.
		boolean changed = false ;
		
		int typeTitle = -1, typeDescription = -1 ;
		int display = TextFormatting.DISPLAY_MENU ;
		int role = TextFormatting.ROLE_HOST ;
		boolean enabled = mAvailability == null ? false : mAvailability.isEnabled() ;
		boolean timed = mAvailability == null ? false : mAvailability.isTimed() ;
		
		// Color and background drawable
		int baseColor = 0 ;
		Drawable imageDrawable ;
		Drawable alertDrawable = null ;
		
		// HACK: We cheat a bit.  To make menus load faster, we allow the "load" button to
		// carry standard text if a game result object is not provided.
		if ( mHasSave && gr == null && buttonType == BUTTON_TYPE_RESUME )
			buttonType = BUTTON_TYPE_FIRST ;
		
		switch( buttonType ) {
		case BUTTON_TYPE_MAIN:
			typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_DESCRIPTION ;
			baseColor = mBaseColorButtonMain ;
			imageDrawable = mImageDrawableButtonMain ;
			alertDrawable = mAlertDrawableTimed ;
			enabled = false ;
			break ;
		case BUTTON_TYPE_FIRST:
			typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_FIRST_GAME_DESCRIPTION ;
			baseColor = mBaseColorButtonFirst ;
			imageDrawable = mImageDrawableButtonFirst ;
			alertDrawable = mAlertDrawableTimed ;
			break ;
		case BUTTON_TYPE_RESUME:
			typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_RESUME_GAME_DESCRIPTION ;
			baseColor = mBaseColorButtonResume ;
			imageDrawable = mImageDrawableButtonResume ;
			alertDrawable = mAlertDrawableTimed ;
			break ;
		case BUTTON_TYPE_NEW:
			typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_NEW_GAME_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_NEW_GAME_DESCRIPTION ;
			baseColor = mBaseColorButtonNew ;
			imageDrawable = mImageDrawableButtonNew ;
			break ;
		case BUTTON_TYPE_NEW_GAME_MODE:
			typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_NEW_GAME_MODE_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_NEW_GAME_MODE_DESCRIPTION ;
			baseColor = mBaseColorButtonNewGameMode ;
			imageDrawable = mImageDrawableButtonNewGameMode ;
			break ;
		case BUTTON_TYPE_EDIT_GAME_MODE:
			typeTitle = TextFormatting.TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_SINGLE_PLAYER_EDIT_GAME_MODE_DESCRIPTION ;
			baseColor = mBaseColorButtonEditGameMode ;
			imageDrawable = mImageDrawableButtonEditGameMode ;
			break ;
		default:
			Log.e(TAG, "setButtonContent: unrecognized buttonType " + buttonType) ;
			return false ;
		}
		
		Context context = getContext() ;
		Resources res = context.getResources() ;
		String textTitle = typeTitle == -1 ? null : TextFormatting.format(context, res, display, typeTitle, role, mGameMode, gr) ;
		String textDescription = typeDescription == -1 ? null : TextFormatting.format(context, res, display, typeDescription, role, mGameMode, gr) ;
		
		
		QuantroButtonAccess qbs = getButtonAccess(buttonIndex) ;
		
		// Now set these values in the appropriate content fields.
		changed = qbs.setTitle(textTitle) 					|| changed ;
		changed = qbs.setDescription(textDescription) 		|| changed ;
		changed = qbs.setImage(imageDrawable != null, imageDrawable) 		|| changed ;
		changed = qbs.setAlert(timed, alertDrawable) 		|| changed ;
		changed = qbs.setColor(baseColor)					|| changed ;
		
		changed = getButtonIsEnabled(buttonIndex) != enabled || changed ;
		setButtonEnabled(buttonIndex, enabled) ;
		
		// Did we change?
		return changed ;
	}
	
	
	private int getButtonType( int buttonNum ) {
		int buttonType = -1 ;
		switch( buttonNum ) {
		case 0:
			if ( mGameMode == GAME_MODE_NEW_CUSTOM_GAME_MODE )
				buttonType = BUTTON_TYPE_NEW_GAME_MODE ;
			else if ( mHasSave )
				buttonType = BUTTON_TYPE_RESUME ;
			else if ( mFirstGameIsMain )
				buttonType = BUTTON_TYPE_FIRST ;
			break ;
		case 1:
			if ( mHasSave )
				buttonType = BUTTON_TYPE_NEW ;
			else
				buttonType = BUTTON_TYPE_FIRST ;
			break ;
		case 2:
			buttonType = BUTTON_TYPE_EDIT_GAME_MODE ;
			break ;
		}
		
		return buttonType ;
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		int buttonType = getButtonType(buttonNum) ;
		if ( buttonType > -1 ) {
			delegate.spglbs_onButtonClick(this, buttonType, mGameMode) ;
		}
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		int buttonType = getButtonType(buttonNum) ;
		if ( buttonType > -1 ) {
			return delegate.spglbs_onButtonLongClick(this, buttonType, mGameMode) ;
		}
		
		return false ;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		int buttonType = getButtonType(buttonNum) ;
		if ( buttonType > -1 ) {
			return delegate.spglbs_supportsLongClick(this, buttonType, mGameMode) ;
		}
		
		return false ;
	}
	
	public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) {
		// nothing
	}
	
}
