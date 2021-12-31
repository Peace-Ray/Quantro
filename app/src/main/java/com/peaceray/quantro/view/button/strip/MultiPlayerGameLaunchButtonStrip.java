package com.peaceray.quantro.view.button.strip;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.options.OptionAvailability;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

public class MultiPlayerGameLaunchButtonStrip extends QuantroButtonStrip
	implements QuantroButtonStrip.Controller {
	
	private static final String TAG = "MPGLButtonStrip" ;

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
		public boolean mpglbs_onButtonClick(
				MultiPlayerGameLaunchButtonStrip strip,
				int buttonNum, int buttonType, int gameMode ) ;
		
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
		public boolean mpglbs_onButtonLongClick(
				MultiPlayerGameLaunchButtonStrip strip,
				int buttonNum, int buttonType, int gameMode ) ;
		
		
		/**
		 * Returns the result of onButtonLongClick without side effects.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param gameMode
		 * @param saveKey
		 */
		public boolean mpglbs_supportsLongClick(
				MultiPlayerGameLaunchButtonStrip strip,
				int buttonNum, int buttonType, int gameMode ) ;
		
		
		
	}
	
	
	public MultiPlayerGameLaunchButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public MultiPlayerGameLaunchButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues(Context context) {
		
		// self-control
		this.setController(this) ;
		
		// For now, we have only 2 players in a lobby, so we have other_0.
		
		mGameMode = -1 ;
		
		// Base colors and drawables!
		mBaseColorVoteButton = 0 ;
		mHasBaseColorVoteButton = false ;
		mBackgroundDrawableVoteButton = null ;
		mAlertDrawableLocked = null ;
		mAlertDrawableProxyUnlocked = null ;
		
		mLocalVoteDrawable = null ;
		mOtherVoteDrawable = new Drawable[MAX_PLAYERS-1] ;
		
		// Colors for players?
		mPlayerColor = new int[MAX_PLAYERS] ;
		mHasPlayerColor = new boolean[MAX_PLAYERS] ;
		for ( int i = 0; i < mHasPlayerColor.length; i++ )
			mHasPlayerColor[i] = false ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.MultiPlayerGameLaunchButtonStrip);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.MultiPlayerGameLaunchButtonStrip_mpglbs_game_mode:
				mGameMode = a.getInt(attr, mGameMode) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonStrip_mpglbs_base_color_vote_button:
				mBaseColorVoteButton = a.getColor(attr, mBaseColorVoteButton) ;
				mHasBaseColorVoteButton = true ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonStrip_mpglbs_background_drawable_vote_button:
				mBackgroundDrawableVoteButton = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonStrip_mpglbs_local_vote_drawable:
				mLocalVoteDrawable = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonStrip_mpglbs_other_vote_drawable:
				for ( int j = 0; j < MAX_PLAYERS-1; j++ )
					mOtherVoteDrawable[j] = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonStrip_mpglbs_alert_locked_drawable:
				mAlertDrawableLocked = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonStrip_mpglbs_alert_proxy_unlocked_drawable:
				mAlertDrawableProxyUnlocked = a.getDrawable(attr) ;
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
	
	public void setGameMode( int gameMode, OptionAvailability availability ) {
		boolean shouldRefresh = gameMode != mGameMode || availability != mAvailability ;
		mGameMode = gameMode ;
		mAvailability = availability ;
		if ( shouldRefresh ) {
			refreshColors() ;
			refresh() ;
			//System.err.println("refreshing with game mode " + gameMode) ;
		}
	}
	
	
	public int getGameMode() {
		return mGameMode ;
	}
	
	
	/**
	 * Calls this.refresh, but only if the provided number is our game mode.
	 * @param gameMode
	 */
	public void refreshIfGameMode( int gameMode ) {
		if ( mGameMode == gameMode )
			refresh() ;
	}
	
	
	@Override
	public void refresh() {
		boolean changed = false ;
		
		// Make sure we have enough buttons!
		while ( this.getButtonCount() < MIN_BUTTONS ) {
			addButton() ;
			changed = true ;
		}
		
		// We set button content and visiblity based on the provided
		// game mode and save key.  Every button beyond the first should
		// be GONE.
		for ( int i = MIN_BUTTONS; i < getButtonCount(); i++ )
			setButtonVisible(i, false) ;
		
		if ( mLobby != null && mLobby.getGameModes().length > 0 )
			changed = setButtonContent( 0, BUTTON_TYPE_VOTE )	|| changed ;
		
		setButtonVisible(0, true) ;
		
		// new Exception("MultiPlayerGameLaunchButtonStrip.refresh() changed " + changed).printStackTrace() ;
		
		if ( changed )
			super.refresh() ;
	}
	
	
	private void refreshColors() {
		// set each color (if not set) according to GameModeColors.
		if ( !mHasBaseColorVoteButton && GameModeColors.hasPrimary(mGameMode) && mColorScheme != null )
			mBaseColorVoteButton = GameModeColors.primary(mColorScheme, mGameMode) ;
	}
	

	////////////////////////////////////////////////////////////////////////////
	// Member vars
	
	private static final int MAX_PLAYERS = 8 ;
	
	// we need at least 1 buttons.  Maybe we'll extend that later.
	private static final int MIN_BUTTONS = 1 ;
	
	public static final int BUTTON_TYPE_VOTE = 0 ;
	
	protected int mGameMode ;	// The game mode we represent.
	protected OptionAvailability mAvailability ;
	protected Lobby mLobby ;	// Lobby from which we will pull most of our content.
	
	// Our listener
	protected WeakReference<Delegate> mwrDelegate ;
	
	// Color scheme
	protected ColorScheme mColorScheme ;
	
	
	// Colors or background drawables for buttons?
	protected int mBaseColorVoteButton ;
	protected boolean mHasBaseColorVoteButton ;
	// Background drawables for our buttons
	protected Drawable mBackgroundDrawableVoteButton ;
	// Drawable to ndicate "locked."
	protected Drawable mAlertDrawableLocked ;
	protected Drawable mAlertDrawableProxyUnlocked ;
	
	// Drawable for votes?  We load this multiple times, and apply
	// different color filters.
	protected Drawable mLocalVoteDrawable ;
	protected Drawable [] mOtherVoteDrawable ;
	
	// Colors for players?
	protected int [] mPlayerColor ;	// Color assigned to each player.  Applied as a filter to
									// any vote drawables.
	protected boolean [] mHasPlayerColor ;		// if 'true', we apply the filter.
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// 
	// HELPER
	// 
	// Some helper methods
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	protected boolean setButtonContent( int buttonIndex, int buttonType ) {
		// We use TextFormatting to load and format strings with the appropriate info
		// inserted.  Then, searching the button's content view by Tag for the appropriate
		// view, we insert that value (if a view for it is found).
		
		// TODO: Set to false here, then to 'true' if any content changed.
		boolean changed = false ;
		
		int typeTitle, typeDescription ;
		int display = TextFormatting.DISPLAY_MENU ;
		int role = TextFormatting.ROLE_HOST ;
		
		// Color and background drawable
		int baseColor = 0 ;
		Drawable backgroundDrawable ;
		Drawable alertDrawable = null ;
		
		boolean enabled = mAvailability == null ? false : mAvailability.isEnabled() ;
		boolean locked = mAvailability == null ? false : mAvailability.isLocked() ;
		boolean proxy_unlocked = mAvailability == null ? false : mAvailability.isProxyUnlocked() ;
		
		if ( !mLobby.getLocalVote(mGameMode) ) {
			if ( locked ) {
				alertDrawable = mAlertDrawableLocked ;
			} else if ( proxy_unlocked ) {
				alertDrawable = mAlertDrawableProxyUnlocked ;
			}
		}
		
		switch( buttonType ) {
		case BUTTON_TYPE_VOTE:
			typeTitle = TextFormatting.TYPE_MENU_MULTI_PLAYER_VOTE_GAME_TITLE ;
			typeDescription = TextFormatting.TYPE_MENU_MULTI_PLAYER_VOTE_GAME_DESCRIPTION ;
			baseColor = mBaseColorVoteButton ;
			backgroundDrawable = mBackgroundDrawableVoteButton ;
			break ;
		default:
			Log.e(TAG, "setButtonContent: unrecognized buttonType " + buttonType) ;
			return false ;
		}
		
		Context context = getContext() ;
		Resources res = context.getResources() ;
		String textTitle = TextFormatting.format(context, res, display, typeTitle, role, mGameMode, mLobby) ;
		String textDescription = TextFormatting.format(context, res, display, typeDescription, role, mGameMode, mLobby) ;
		
		
		// Now set these values in the appropriate content fields.
		QuantroButtonAccess qbs = getButtonAccess(buttonIndex) ;
		
		// set text
		changed = qbs.setTitle(textTitle)  					|| changed ;
		changed = qbs.setDescription(textDescription) 		|| changed ;
		// set background drawable
		changed = qbs.setBackground(backgroundDrawable) 	|| changed ;
		// button base color
		changed = qbs.setColor(baseColor) 					|| changed ;
		// is this locked?
		changed = qbs.setAlert(alertDrawable != null, alertDrawable) || changed ;
		
		// votes!
		changed = qbs.setVoteLocal(mLobby.getLocalVote(mGameMode), mLocalVoteDrawable, mPlayerColor[mLobby.getLocalPlayerSlot()])
															|| changed ;
		boolean [] votes = mLobby.getVotes(mGameMode) ;
		for ( int i = 0; i < mOtherVoteDrawable.length ; i++ ) {
			int playerSlot = mLobby.getLocalPlayerSlot() <= i ? i+1 : i ;
			boolean votedFor = playerSlot < votes.length ? votes[playerSlot] : false ;
			
			Drawable d = mOtherVoteDrawable[i] ;
			int color = playerSlot < mPlayerColor.length ? mPlayerColor[playerSlot] : Color.WHITE ;
			changed = qbs.setVote(i, votedFor, d, color)	|| changed ;
		}
		
		if ( enabled != getButtonIsEnabled(buttonIndex) ) {
			changed = true ;
			setButtonEnabled( buttonIndex, enabled ) ;
		}
		
		return changed ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// 
	// INFO SETTERS
	// 
	// We need some information to determine how we format stuff.  For instance,
	// we allow a color filter for every vote drawable, based on player colors.
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	public void setDelegate( MultiPlayerGameLaunchButtonStrip.Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	public void setColorScheme( ColorScheme cs ) {
		this.mColorScheme = cs ;
	}
	
	
	/**
	 * Sets the lobby from which we will pull our information.  Does NOT refresh.
	 * 
	 * @param lobby The lobby.
	 */
	public void setLobby( Lobby lobby ) {
		mLobby = lobby ;
	}
	
	/**
	 * Sets the color we will use as a filter for the votes by this player.
	 * @param playerSlot
	 * @param color
	 */
	public void setPlayerColor( int playerSlot, int color ) {
		boolean refresh = !mHasPlayerColor[playerSlot] || mPlayerColor[playerSlot] != color ;
		mPlayerColor[playerSlot] = color ;
		mHasPlayerColor[playerSlot] = true ;
		if ( refresh )
			this.refresh() ;
	}
	
	/**
	 * Unsets the color we use for a filter; no filter will be applied for
	 * this player.
	 * @param playerSlot
	 */
	public void unsetPlayerColor( int playerSlot ) {
		mHasPlayerColor[playerSlot] = false ;
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;		// we did not handle it
		// Determine which button has been pressed - 0 or 1 - and tell our
		// listener, providing the button number and current button type.
		int buttonType ;
		if ( buttonNum == 0 ) {
			buttonType = BUTTON_TYPE_VOTE ;
			delegate.mpglbs_onButtonClick(
					this, 0, buttonType, mGameMode) ;
		}
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;		// we did not handle it
		// Determine which button has been pressed - 0 or 1 - and tell our
		// listener, providing the button number and current button type.
		int buttonType ;
		if ( buttonNum == 0 ) {
			buttonType = BUTTON_TYPE_VOTE ;
			return delegate.mpglbs_onButtonLongClick(
					this, 0, buttonType, mGameMode) ;
		}
		return false;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;		// we did not handle it
		// Determine which button has been pressed - 0 or 1 - and tell our
		// listener, providing the button number and current button type.
		int buttonType ;
		if ( buttonNum == 0 ) {
			buttonType = BUTTON_TYPE_VOTE ;
			return delegate.mpglbs_supportsLongClick(
					this, 0, buttonType, mGameMode) ;
		}
		return false;
	}
	
	public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) {
		// nothing
	}
	
}
