package com.peaceray.quantro.view.button.collage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.peaceray.quantro.R;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.SupportsLongClickOracle;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.options.OptionAvailability;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;



public class MultiPlayerGameLaunchButtonCollage extends QuantroButtonCollage implements OnClickListener, OnLongClickListener, SupportsLongClickOracle {

	
	private static final String TAG = "MPGLButtonCollage" ;
	
	public interface Delegate {
		
		/**
		 * The player wants to vote for the specified game type.
		 * @param collage
		 * @param mGameMode
		 * @return should we play the appropriate "button press" sound effect?
		 */
		public boolean mpglbcd_vote( MultiPlayerGameLaunchButtonCollage collage, int mGameMode ) ;
		
		/**
		 * The player wants to rescind their vote for the specified game type.
		 * @param collage
		 * @param mGameMode
		 * @returns should we play the appropriate "button press" sound effect?
		 */
		public boolean mpglbcd_unvote( MultiPlayerGameLaunchButtonCollage collage, int mGameMode ) ;
		
		
		
	}
	
	public MultiPlayerGameLaunchButtonCollage(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public MultiPlayerGameLaunchButtonCollage(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
		
		
	}
	
	public MultiPlayerGameLaunchButtonCollage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues( Context context ) {
		// For now, we have only 2 players in a lobby, so we have other_0.
		
		mGameMode = -1 ;
		
		// Base colors and drawables!
		mButtonTypeColor = new int[NUM_BUTTON_TYPES] ;
		
		mLocalVoteDrawable = null ;
		mLocalLaunchingDrawable = null ;
		mOtherVoteDrawable = new Drawable[MAX_PLAYERS-1] ;
		mOtherLaunchingDrawable = new Drawable[MAX_PLAYERS-1] ;
		mAlertDrawableLocked = null ;
		mAlertDrawableProxyUnlocked = null ;
		
		// Colors for players?
		mPlayerColor = new int[MAX_PLAYERS] ;
		for ( int i = 0; i < mPlayerColor.length; i++ )
			mPlayerColor[i] = Color.WHITE ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	
	private void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.MultiPlayerGameLaunchButtonCollage);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.MultiPlayerGameLaunchButtonCollage_image_drawable_local_vote:
				mLocalVoteDrawable = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonCollage_image_drawable_other_vote:
				for ( int j = 0; j < MAX_PLAYERS-1; j++ )
					mOtherVoteDrawable[j] = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonCollage_image_drawable_local_launching:
				mLocalLaunchingDrawable = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonCollage_image_drawable_other_launching:
				for ( int j = 0; j < MAX_PLAYERS-1; j++ )
					mOtherLaunchingDrawable[j] = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonCollage_alert_locked_drawable:
				mAlertDrawableLocked = a.getDrawable(attr) ;
				break ;
			case R.styleable.MultiPlayerGameLaunchButtonCollage_alert_proxy_unlocked_drawable:
				mAlertDrawableProxyUnlocked = a.getDrawable(attr) ;
				break ;
			}
		}
	}
	
	
	private void constructor_allocateAndInitialize( Context context ) {
		// if we have set 'vote' drawables, but not 'launch' drawables,
		// copy them over so we always display the vote drawables.
		if ( mLocalLaunchingDrawable == null )
			mLocalLaunchingDrawable = mLocalVoteDrawable ;
		if ( mOtherLaunchingDrawable[0] == null ) {
			for ( int i = 0; i < mOtherLaunchingDrawable.length; i++ ) {
				mOtherLaunchingDrawable[i] = mOtherVoteDrawable[i] ;
			}
		}
		
		// nothing else to do.  If we allow manual gamemode settings,
		// refresh colors here.
	}
	
	
	private static final int MAX_PLAYERS = 8 ;
	
	
	private static final int BUTTON_TYPE_VOTE = 0 ;
	private static final int BUTTON_TYPE_UNVOTE = 1 ;
	private static final int NUM_BUTTON_TYPES = 2 ;
	
	
	private static final int BUTTON_NUM_MAIN = 0 ;
	private static final int NUM_BUTTONS = 1 ;
	
	private int mGameMode ;	// The game mode we represent.
	private OptionAvailability mAvailability ;
	private Lobby mLobby ;	// Lobby from which we will pull most of our content.
	
	// Our listener
	private WeakReference<Delegate> mwrDelegate ;
	
	// Color scheme
	private ColorScheme mColorScheme ;
	
	// Sound Pool
	private QuantroSoundPool mSoundPool ;
	
	// Drawable for votes?  We load this multiple times, and apply
	// different color filters.
	private Drawable mLocalVoteDrawable ;
	private Drawable mLocalLaunchingDrawable ;
	private Drawable [] mOtherVoteDrawable ;
	private Drawable [] mOtherLaunchingDrawable ;
	private Drawable mAlertDrawableLocked ;
	private Drawable mAlertDrawableProxyUnlocked ;
	
	// Colors for players?
	private int [] mPlayerColor ;	// Color assigned to each player.  Applied as a filter to
									// any vote drawables.
	
	
	//////////////////
	// BUTTON CONTENT
	
	private QuantroContentWrappingButton [] mButtons ;
	private QuantroButtonDirectAccess [] mButtonContent ;
	private int [] mButtonTypeColor ;
	
	// we also need a place for the label
	private TextView mLabelView ;
	private String mLabel ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// SETUP / CONFIG
	
	public void setDelegate( Delegate d ) {
		mwrDelegate = new WeakReference<Delegate>(d) ;
	}
	
	public void setColorScheme( ColorScheme cs ) {
		mColorScheme = cs ;
	}
	
	public void setSoundPool( QuantroSoundPool soundPool ) {
		mSoundPool = soundPool ;
	}
	
	public void setLobby( Lobby lobby ) {
		mLobby = lobby ;
	}
	
	public void setPlayerColor( int playerSlot, int color ) {
		boolean refresh = mPlayerColor[playerSlot] != color ;
		mPlayerColor[playerSlot] = color ;
		if ( refresh )
			this.refresh() ;
	}
	
	public void setGameMode( int gameMode, OptionAvailability availability ) {
		if ( gameMode != mGameMode || availability != mAvailability ) {
			mGameMode = gameMode ;
			mAvailability = availability ;
			refreshColors() ;
			refresh() ;
		}
	}
	
	public int getGameMode() {
		return mGameMode ;
	}
	
	public void refreshIfGameMode( int gameMode ) {
		if ( mGameMode >= 0 && mGameMode == gameMode )
			refresh() ;
	}
	
	
	
	private void refreshColors() {
		// set each color (if not set) according to GameModeColors.
		if ( GameModeColors.hasPrimary(mGameMode) && mColorScheme != null ) {
			mButtonTypeColor[BUTTON_TYPE_VOTE] = GameModeColors.primary(mColorScheme, mGameMode) ;
			mButtonTypeColor[BUTTON_TYPE_UNVOTE] = mButtonTypeColor[BUTTON_TYPE_VOTE] ;
		}
	}
	

	private boolean refresh_ever = false ;
	@Override
	public void refresh() {
		boolean changed = false ;
		
		if ( !refresh_ever ) {
			collectAllContent() ;
			refresh_ever = true ;
			changed = true ;
			//Log.d(TAG, "past collect content in " + (System.currentTimeMillis() - time)) ;
		}
		
		if ( mLobby.getLocalVote(mGameMode) )
			changed = setButtonContent( mButtons[BUTTON_NUM_MAIN], mButtonContent[BUTTON_NUM_MAIN], BUTTON_TYPE_UNVOTE ) || changed ;
		else
			changed = setButtonContent( mButtons[BUTTON_NUM_MAIN], mButtonContent[BUTTON_NUM_MAIN], BUTTON_TYPE_VOTE ) || changed ;
		
		mButtons[BUTTON_NUM_MAIN].setVisibility(View.VISIBLE) ;
		
		// set the label.
		// String label = GameModes.className(mGameMode) ;
		String label = GameModes.name(mGameMode) ;
		if ( !label.equals(mLabel) ) {
			mLabelView.setText(label) ;
			changed = true ;
			//Log.d(TAG, "past set label in " + (System.currentTimeMillis() - time)) ;
		}
		
		if ( changed )
			super.refresh() ;
	}
	
	
	/**
	 * Collects necessary references, sets listeners, creates 
	 * QuantroButtonContent objects, etc.
	 */
	private void collectAllContent() {
		
		// collect buttons.
		ArrayList<Integer> ids = new ArrayList<Integer>() ;
		
		ids.add( R.id.button_collage_multi_player_button_main ) ;
		
		ArrayList<QuantroContentWrappingButton> buttons = collectButtons(ids) ;
		
		mButtons = new QuantroContentWrappingButton[NUM_BUTTONS] ;
		mButtons[BUTTON_NUM_MAIN] = buttons.get(0) ;
		
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
		// TODO: Set to false here, then to 'true' if any content changed.
		boolean changed = false ;
		
		// int display = TextFormatting.DISPLAY_MENU ;
		// int role = TextFormatting.ROLE_HOST ;
		boolean enabled = mAvailability.isEnabled() ;
		boolean locked = mAvailability.isLocked() ;
		boolean proxy_unlocked = mAvailability.isProxyUnlocked() ;
		boolean localVote ;
		
		String textTitle = null, textDescription = null ;
		
		// Color and background drawable
		int baseColor = 0 ;
		
		Drawable alertDrawable = null ;
		if ( locked ) {
			alertDrawable = mAlertDrawableLocked ;
		} else if ( proxy_unlocked ) {
			alertDrawable = mAlertDrawableProxyUnlocked ;
		}
		
		switch( buttonType ) {
		case BUTTON_TYPE_VOTE:
			textDescription = GameModes.shortDescription(mGameMode) ;
			localVote = false ;
			break ;
		case BUTTON_TYPE_UNVOTE:
			localVote = true ;
			break ;
		default:
			Log.e(TAG, "setButtonContent: unrecognized buttonType " + buttonType) ;
			return false ;
		}
		
		baseColor = mButtonTypeColor[buttonType] ;
		
		// Now set these values in the appropriate content fields.
		changed = content.setTitle(textTitle) 					|| changed ;
		changed = content.setDescription(textDescription) 		|| changed ;
		changed = content.setColor(baseColor)					|| changed ;
		
		changed = button.isEnabled() != enabled 				|| changed ;
		button.setEnabled(enabled) ;
		button.setClickable(enabled) ;
		
		changed = content.setAlert(alertDrawable != null, alertDrawable) || changed ;
		
		// votes!
		changed = content.setVoteLocal(
				localVote,
				mLobby.getPlayerIsInGameModeCountdown(mLobby.getLocalPlayerSlot(), mGameMode)
						? mLocalLaunchingDrawable
						: mLocalVoteDrawable,
				mPlayerColor[mLobby.getLocalPlayerSlot()])		|| changed ;
		
		boolean [] votes = mLobby.getVotes(mGameMode) ;
		for ( int i = 0; i < mOtherVoteDrawable.length ; i++ ) {
			int playerSlot = mLobby.getLocalPlayerSlot() <= i ? i+1 : i ;
			boolean votedFor = playerSlot < votes.length ? votes[playerSlot] : false ;
			
			Drawable d = mLobby.getPlayerIsInGameModeCountdown(playerSlot, mGameMode)
					? mOtherLaunchingDrawable[i]
					: mOtherVoteDrawable[i] ;
			int color = playerSlot < mPlayerColor.length ? mPlayerColor[playerSlot] : Color.WHITE ;
			changed = content.setVote(i, votedFor, d, color)	|| changed ;
		}
		
		return changed ;
	}
	
	
	@Override
	public void onClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		if ( v == mButtons[BUTTON_NUM_MAIN] ) {
			// could be start new or resume.
			if ( mLobby.getLocalVote(mGameMode) )
				delegate.mpglbcd_unvote(this, mGameMode) ;
			else
				delegate.mpglbcd_vote(this, mGameMode) ;
			
			// play sound
			if ( mSoundPool != null )
				mSoundPool.menuButtonClick() ;
		}
	}
	

	@Override
	public boolean onLongClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		if ( v == mButtons[BUTTON_NUM_MAIN] ) {
			// To add handicaps, support long clicks.
			return false ;
		}
		
		return false ;
	}

	

	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		if ( qcwb == mButtons[BUTTON_NUM_MAIN] ) {
			// vote/unvote.  Add long-press for handicapping.
			return false ;
		}
		return false ;
	}
	
}
