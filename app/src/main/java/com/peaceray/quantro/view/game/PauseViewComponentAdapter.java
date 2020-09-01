package com.peaceray.quantro.view.game;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.peaceray.quantro.model.game.GameBlocksSlice;
import com.peaceray.quantro.model.game.GameSettings;

/**
 * Draws "pause" information to the canvas.
 * 
 * The pause screen is likely to have this general organization:
 * 
 * 		 __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __
 * 		|
 * 		|	Next: Line Piece.							Reserve: A piece
 * 		|	Everyone's										may be held 
 * 		|	favorite.										until needed.
 * 		|
 * 		|
 * 		|					Falling: Seven.  It's
 * 		|			      			lucky!
 * 		|						
 * 		|					
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|						L O A D I N G
 * 		|							(or)
 * 		|						P A U S E D
 * 		|               		    (or nothing)
 * 		|        				R E A D Y
 * 		|
 * 		|						please wait
 * 		|							(or)
 * 		|						touch anywhere to resume
 * 		|							(or)
 * 		|						touch anywhere to begin
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|		Quantro is controlled using touch screen "buttons."
 * 		|		This thumbnail shows their locations.
 * 		|		 
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 * 		|
 *		|__ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __ __
 * 
 * 
 * This abstract class should be subclassed as needed.  Updates can be handled either
 * by overriding specific 'set' methods (be sure to call super.set* as your first
 * operation!) or through the generic 'didChange' method, which is 
 * called upon every update (AFTER the update has been performed).
 * 
 * Usage: at a minimum, 'setDrawRegion' should be called before any calls to
 * 		'draw.'
 * 
 * The Slices set should reflect the user's game (not an opponent's echoed
 * 		game).  Take care to set the currently displayed game to the user's
 * 		upon pause; otherwise the piece descriptions will not make sense.
 * 
 * This adapter serves a single purpose - altering the contents of various 
 * 		PauseView components (similar to LobbyViewComponentAdapter).  It does
 * 		not handle things like view screen position, click-capturing, etc.
 * 		It does not even handle making the pause screen visible / invisible.
 * 		Its sole purpose is to set text and image views appropriately based
 * 		on the state of the game (or, more accurately, to allow an easy interface
 * 		for GameView to make those changes without troubling itself about the
 * 		specifics involved).
 * 
 * @author Jake
 *
 */
public abstract class PauseViewComponentAdapter {

	public static final int STATE_LOADING = 0 ;
	public static final int STATE_CONNECTING = 1 ;
	public static final int STATE_TALKING = 2 ;
	public static final int STATE_PAUSED = 3 ;
	public static final int STATE_WAITING = 4 ;
	public static final int STATE_READY = 5 ;
	public static final int STATE_STARTING = 6 ;
	public static final int STATE_UNSPECIFIED = 7 ;
	public static final int NUM_STATES = 8 ;
	
	public static final int COMPONENT_LAYOUT = 0 ;
	public static final int COMPONENT_GAME_MODE = 1 ;
	public static final int COMPONENT_DESCRIPTION_NEXT = 2 ;
	public static final int COMPONENT_DESCRIPTION_RESERVE = 3 ;
	public static final int COMPONENT_DESCRIPTION_FALLING = 4 ;
	public static final int COMPONENT_STATE = 5 ;
	public static final int COMPONENT_STATE_DESCRIPTION = 6 ;
	public static final int COMPONENT_DESCRIPTION_CONTROLS = 7 ;
	public static final int COMPONENT_DESCRIPTION_EXPANDED_CONTROLS = 8 ;
	public static final int COMPONENT_LOGO = 9 ;
	public static final int COMPONENT_THUMBNAIL_CONTROLS = 10 ;
	public static final int COMPONENT_DRAW_DETAIL = 11 ;
	public static final int COMPONENT_COLOR_SCHEME = 12 ;
	public static final int COMPONENT_SOUND = 13 ;
	public static final int COMPONENT_MUSIC = 14 ;
	public static final int COMPONENT_MUTE_ALERT = 15 ;
	public static final int NUM_COMPONENTS = 16 ;
	
	protected View [] mComponents = new View[NUM_COMPONENTS];
	
	protected int mState = STATE_READY ;
	protected int mStateAfterLoading ;
	protected Rect mDrawRegion ;
	
	protected GameSettings mGameSettings ;
	protected boolean mGameIsTrial ;
	protected int mGameTrialSeconds ;
	protected int mNumberOfPlayers ;
	protected String [] mPlayerNames ;
	protected boolean [] mPlayersParticipating ;
	protected boolean mLocalPause ;
	protected int mThisPlayer ;
	
	protected boolean [] mPlayerKickWarning = new boolean[8] ;
	protected long [] mPlayerKickTime = new long[8] ;
	
	// How do we know where to place piece descriptions?
	protected Rect mNextPieceRegion ;
	protected Rect mReservePieceRegion ;
	
	protected GameBlocksSlice mSlice = null ;
	protected GameBlocksSlice mSliceNext = null ;
	protected GameBlocksSlice mSliceReserve = null ;
	
	protected boolean mReserveIsSpecial = false ;
	protected boolean mIsHost = false ;
	
	protected Bitmap mLogo ;
	protected Drawable mControlsThumbnail ;
	
	protected int mDrawDetail ;
	protected String mColorSchemeName ;
	protected boolean mSoundOn ;
	protected boolean mIsMutedByRinger ;
	protected int mSoundVolumePercent ;
	protected int mMusicVolumePercent ;
	
	protected boolean mControlsGamepad ;
	protected boolean mControlsGamepadDownFall ;
	protected boolean mControlsGamepadDownDoubleTap ;
	protected boolean mControlsSupportsQuickSlide = true ;
	protected boolean mControlsHasTurns = true ;
	protected boolean mControlsHasFlip = false ;
	
	
	public boolean setVisibility( int vis ) {
		View v = mComponents[COMPONENT_LAYOUT] ;
		if ( v != null ) {
			v.setVisibility(vis) ;
			didChange() ;
			return true ;
		}
		return false ;
	}
	
	public void setGame( GameSettings gs ) {
		mGameSettings = gs ;
		didChange() ;
	}
	
	public void setGameIsTrial( boolean isTrial, int secondsIfIsTrial ) {
		mGameIsTrial = isTrial ;
		mGameTrialSeconds = secondsIfIsTrial ;
		didChange() ;
	}
	
	public void setNumberOfPlayers( int number ) {
		mNumberOfPlayers = number ;
		didChange() ;
	}
	
	public void setPlayerNames( String [] names ) {
		mPlayerNames = names ;
		didChange() ;
	}
	
	public void setThisPlayer( int slot ) {
		mThisPlayer = slot ;
		didChange() ;
	}
	
	public void setComponent( int component, View v ) {
		mComponents[component] = v ;
		didChangeComponentReference( component ) ;
	}
	
	public void setSlice( GameBlocksSlice slice ) {
		mSlice = slice ;
		didChange() ;
	}
	
	public void setSliceNextPiece( GameBlocksSlice slice ) {
		mSliceNext = slice ;
		didChange() ;
	}
	
	public void setSliceReservePiece( GameBlocksSlice slice ) {
		mSliceReserve = slice ;
		didChange() ;
	}
	
	public void setReserveIsSpecial( boolean special ) {
		mReserveIsSpecial = special ;
		didChange() ;
	}
	
	public void setIsHost( boolean host ) {
		mIsHost = host ;
		didChange() ;
	}
	
	public void setLocalPause( boolean localPause ) {
		mLocalPause = localPause ;
		didChange() ;
	}
	
	public void setPlayerKickWarning( boolean warn, int slot, long kickAt ) {
		mPlayerKickWarning[slot] = warn ;
		mPlayerKickTime[slot] = kickAt ;
		if ( mState == STATE_WAITING )
			didChange() ;
	}
	
	// Helpful junk
	public void setStateConnecting() {
		setState( STATE_CONNECTING ) ;
	}
	
	public void setStateTalking() {
		setState( STATE_TALKING ) ;
	}
	
	public void setStatePaused( boolean [] pausedByPlayer ) {
		mPlayersParticipating = pausedByPlayer ;
		setState( STATE_PAUSED ) ;
	}
	
	public void setStateWaiting( boolean [] waitingForPlayer ) {
		mPlayersParticipating = waitingForPlayer ;
		setState( STATE_WAITING ) ;
	}
	
	public void setStateReady() {
		setState( STATE_READY ) ;
	}
	
	public void setStateStarting() {
		setState( STATE_STARTING ) ;
	}
	
	
	
	private void setState( int state ) {
		if ( state != STATE_LOADING ) {
			if ( mState == STATE_LOADING )
				mStateAfterLoading = state ;
			else {
				mState = state ;
				didChange() ;
			}
		}
		else
			throw new IllegalArgumentException("Don't set loading state directly; call setStartLoading") ;
	}
	
	public void setStartLoading() {
		if ( mState != STATE_LOADING ) {
			mStateAfterLoading = mState ;
			mState = STATE_LOADING ;
			didChange() ;
		}
	}
	
	public void setFinishedLoading() {
		if ( mState == STATE_LOADING ) {
			mState = mStateAfterLoading ;
			didChange() ;
		}
	}
	
	public int getState() {
		return mState ;
	}
	
	public void setLogo( Bitmap logo ) {
		mLogo = logo ;
		didChange() ;
	}
	
	public void setControlsThumbnail( Drawable controls ) {
		mControlsThumbnail = controls ;
		didChange() ;
	}
	
	public void setDrawDetail( int detail ) {
		mDrawDetail = detail ;
		didChange() ;
	}
	
	public void setColorSchemeName( String scheme ) {
		mColorSchemeName = scheme ;
		didChange() ;
	}
	
	public void setSoundOn( boolean on, boolean isMutedByRinger ) {
		mSoundOn = on ;
		mIsMutedByRinger = isMutedByRinger ;
		didChange() ;
	}
	
	public void setSoundVolumePercent( int vol ) {
		mSoundVolumePercent = vol ;
		didChange() ;
	}
	
	public void setMusicVolumePercent( int vol ) {
		mMusicVolumePercent = vol ;
		didChange() ;
	}
	
	public void setControlsGamepad( boolean on, boolean gamepadDownFall, boolean gamepadDoubleTap ) {
		mControlsGamepad = on ;
		mControlsGamepadDownFall = gamepadDownFall ;
		mControlsGamepadDownDoubleTap = gamepadDoubleTap ;
		didChange() ;
	}
	
	public void setControlsSupportsQuickSlide( boolean supports ) {
		mControlsSupportsQuickSlide = supports ;
		didChange() ;
	}
	
	public void setControlsHas( boolean hasTurns, boolean hasFlips ) {
		mControlsHasTurns = hasTurns ;
		mControlsHasFlip = hasFlips ;
		didChange() ;
	}
	
	protected abstract void didChangeComponentReference( int component ) ;
	protected abstract void didChange() ;
	
}
