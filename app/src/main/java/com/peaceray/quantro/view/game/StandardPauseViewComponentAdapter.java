package com.peaceray.quantro.view.game;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.model.game.GameBlocksSlice;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class StandardPauseViewComponentAdapter extends
		PauseViewComponentAdapter {
	
	private static final String TAG = "SPVComponentAdapter" ;
	
	// We don't want to do a lot of string construction,
	// so we pre-construct strings in advance.
	protected static int enm = 0 ;
	////////////////// GAME ROLE: next, reserve, special, falling
	protected static final int INDEX_GAME_ROLE_NEXT 	= enm = 0 ;
	protected static final int INDEX_GAME_ROLE_RESERVE 	= ++enm ;
	protected static final int INDEX_GAME_ROLE_SPECIAL 	= ++enm ;
	protected static final int INDEX_GAME_ROLE_FALLING 	= ++enm ;
	protected static final int NUM_INDEX_GAME_ROLES 	= ++enm ;
	
	////////////////// PIECE CATEGORIES
	// tetromino categories
	protected static final int INDEX_CATEGORY_TETRO_LINE 	= 0 ;
	protected static final int INDEX_CATEGORY_TETRO_GAMMA 	= 1 ;
	protected static final int INDEX_CATEGORY_TETRO_GUN	 	= 2 ;
	protected static final int INDEX_CATEGORY_TETRO_SQUARE 	= 3 ;
	protected static final int INDEX_CATEGORY_TETRO_S	 	= 4 ;
	protected static final int INDEX_CATEGORY_TETRO_Z	 	= 5 ;
	protected static final int INDEX_CATEGORY_TETRO_T	 	= 6 ;
	// tetracube categories
	protected static final int INDEX_CATEGORY_TETRA_L		= 7 ;
	protected static final int INDEX_CATEGORY_TETRA_RECT	= 8 ;
	protected static final int INDEX_CATEGORY_TETRA_ZIG_ZAG	= 9 ;
	protected static final int INDEX_CATEGORY_TETRA_3DT		= 10 ;
	protected static final int INDEX_CATEGORY_TETRA_BRANCH	= 11 ;
	protected static final int INDEX_CATEGORY_TETRA_SCREW	= 12 ;
	protected static final int INDEX_CATEGORY_TETRA_DEXTER 	= 13 ;
	protected static final int INDEX_CATEGORY_TETRA_SINISTER= 14 ;
	// special categories
	protected static final int INDEX_CATEGORY_SPECIAL_FLASH = 15 ;
	protected static final int INDEX_CATEGORY_SPECIAL_NONE  = 16 ;
	protected static final int NUM_INDEX_CATEGORIES  = 17 ;
	
	
	////////////////// QOrientation
	protected static final int INDEX_QORIENTATION_RETRO 	= enm = 0 ;
	protected static final int INDEX_QORIENTATION_S0	 	= ++enm ;
	protected static final int INDEX_QORIENTATION_S1	 	= ++enm ;
	protected static final int INDEX_QORIENTATION_SS	 	= ++enm ;
	protected static final int INDEX_QORIENTATION_ST	 	= ++enm ;
	protected static final int INDEX_QORIENTATION_UL	 	= ++enm ;
	protected static final int INDEX_QORIENTATION_SL	 	= ++enm ;
	protected static final int INDEX_QORIENTATION_F	 		= ++enm ;
	protected static final int INDEX_QORIENTATION_NONE 		= ++enm ;
	protected static final int NUM_INDEX_QORIENTATIONS 		= ++enm ;
	
	////////////////// Controls bitmap
	protected static final int INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_NO		= enm = 0 ;
	protected static final int INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_YES	= ++enm ;
	protected static final int INDEX_HAS_CONTROLS_GESTURE_BITMAP_NO		= ++enm ;
	protected static final int INDEX_HAS_CONTROLS_GESTURE_BITMAP_YES	= ++enm ;
	
	protected static final int NUM_INDEX_HAS_CONTROLS 		= ++enm ;
	
	////////////////// CONTROLS expanded
	protected static final int INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL_DOUBLE_TAP = enm = 0 ;
	protected static final int INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL = ++enm ;
	protected static final int INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP_DOUBLE_TAP = ++enm ;
	protected static final int INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP = ++enm ;
	protected static final int INDEX_CONTROLS_EXPANDED_GESTURE = ++enm ;
	
	protected static final int NUM_INDEX_CONTROLS_EXPANDED = ++enm ;
	
	////////////////// CONTROLS moves available
	protected static final int INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD = enm = 0 ;
	protected static final int INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE = ++enm ;
	
	protected static final int NUM_INDEX_CONTROLS_AVAILABLE_EXERPT =  ++enm ;
	
	
	////////////////// Sound/music on
	protected static final int INDEX_SOUND_ON_NO			= enm = 0 ;
	protected static final int INDEX_SOUND_ON_NO_BY_RINGER	= ++enm ;
	protected static final int INDEX_SOUND_ON_YES			= ++enm ;
	protected static final int INDEX_MUSIC_ON_NO			= ++enm ;
	protected static final int INDEX_MUSIC_ON_NO_BY_RINGER	= ++enm ;
	protected static final int INDEX_MUSIC_ON_YES			= ++enm ;
	
	protected static final int NUM_INDEX_SOUND_ON 			= ++enm ;
	
	
	
	protected String [][][] mTextPiece ;		// indexed by game role, category, orientation.
	protected String [] mTextState ;			// indexed by game state
	protected String [] mTextStateAbrv;			// indexed by game state
	protected String [] mTextStateDescription ;		// indexed by game state
	protected String [] mTextControls ;			// indexed by has controls
	protected String [] mTextControlsExpanded ;	// indexed by INDEX_CONTROLS_EXPANDED.
	protected String [][][] mTextControlsExerpt ;	// indexed by INDEX_CONTROLS_AVAILABLE, hasTurns, has
	protected String [] mTextDrawDetail ;		// indexed by draw detail
	protected String [] mTextSound ;			// indexed by sound on
	protected String [] mTextMuteAlert ;		// indexed by sound on
	protected String mTextGameMode ;			// no index; just a string.
	
	protected String mTextControlsExpandedExerptPlaceholder ;
	
	protected WeakReference<Activity> mwrActivity ;
	protected Handler mHandler ;
	protected Runnable mRunnableUpdate ;
	protected Runnable mRunnableUpdateKickCountdown ;
	
	protected boolean [] mNeedsUpdate ;
	
	public StandardPauseViewComponentAdapter( Activity activity ) {
		// initialize views to null
		mNeedsUpdate = new boolean[NUM_COMPONENTS] ;
		for ( int i = 0; i < NUM_COMPONENTS; i++ ) {
			mComponents[i] = null ;
			mNeedsUpdate[i] = false ;
		}
		
		
		
		mTextPiece = new String[NUM_INDEX_GAME_ROLES][NUM_INDEX_CATEGORIES][NUM_INDEX_QORIENTATIONS] ;
		mTextState = new String[NUM_STATES] ;
		mTextStateAbrv = new String[NUM_STATES] ;
		mTextStateDescription = new String[NUM_STATES] ;
		mTextControls = new String[NUM_INDEX_HAS_CONTROLS] ;
		mTextControlsExpanded = new String[NUM_INDEX_CONTROLS_EXPANDED] ;
		mTextControlsExerpt = new String[NUM_INDEX_CONTROLS_AVAILABLE_EXERPT][2][2] ;
		mTextDrawDetail = new String[DrawSettings.DRAW_DETAIL_HIGH + 1] ;
		mTextSound = new String[NUM_INDEX_SOUND_ON] ;
		mTextMuteAlert = new String[NUM_INDEX_SOUND_ON] ;
		mTextGameMode = null ;
		
		mwrActivity = new WeakReference<Activity>(activity) ;
		
		mRunnableUpdate = new Runnable() {
			@Override
			public void run() {
				for ( int i = 0; i < NUM_COMPONENTS; i++ ) {
					if ( mNeedsUpdate[i] ) {
						mNeedsUpdate[i] = false ;
						updateComponentForce(i) ;
					}
				}
			}
		} ;
		
		mRunnableUpdateKickCountdown = new Runnable() {
			@Override
			public void run() {
				mHandler.removeCallbacks(mRunnableUpdateKickCountdown) ;
				int dropPlayer = getNextDropPlayer() ;
				if ( dropPlayer > -1 ) {
					mNeedsUpdate[COMPONENT_STATE_DESCRIPTION] = true ;
					mwrActivity.get().runOnUiThread(mRunnableUpdate) ;
					mHandler.postDelayed(mRunnableUpdateKickCountdown, 1000) ;
				}
			}
		} ;
		
		mHandler = new Handler() ;
		
		// preconstruct strings
		constructStrings() ;
	}
	
	
	private int getNextDropPlayer() {
		if ( mState == STATE_WAITING ) {
			// see if we're waiting for a drop
			int playerToDrop = -1 ;
			long timeToDrop = Long.MAX_VALUE ;
			for ( int i = 0; i < this.mPlayerKickWarning.length; i++ ) {
				if ( mPlayerKickWarning[i] && mPlayerKickTime[i] < timeToDrop ) {
					playerToDrop = i ;
					timeToDrop = mPlayerKickTime[i] ;
				}
			}
			return playerToDrop ;
		}
		return -1 ;
	}
	
	@Override
	synchronized public boolean setVisibility( int vis ) {
		Activity activity = mwrActivity.get() ;
		boolean result = super.setVisibility(vis) ;
		if ( result && activity != null ) {
			if ( vis == View.VISIBLE ) {
				// perform the updates we were waiting on.
				mwrActivity.get().runOnUiThread(mRunnableUpdate) ;
			}
		}
		return result ;
	}
	
	@Override
	synchronized public void setGame( GameSettings gs ) {
		super.setGame(gs) ;
		updateGameModeString() ;
		updateComponent( COMPONENT_GAME_MODE ) ;
	}
	
	@Override
	synchronized public void setGameIsTrial( boolean isTrial, int secondsIfIsTrial ) {
		super.setGameIsTrial(isTrial, secondsIfIsTrial) ;
		updateGameModeString() ;
		updateComponent( COMPONENT_GAME_MODE ) ;
	}
	
	@Override
	synchronized public void setSlice( GameBlocksSlice slice ) {
		super.setSlice( slice ) ;
		updateComponent( COMPONENT_DESCRIPTION_FALLING ) ;
	}
	
	@Override
	synchronized public void setSliceNextPiece( GameBlocksSlice slice ) {
		super.setSliceNextPiece( slice ) ;
		updateComponent( COMPONENT_DESCRIPTION_NEXT ) ;
	}
	
	@Override
	synchronized public void setSliceReservePiece( GameBlocksSlice slice ) {
		super.setSliceReservePiece( slice ) ;
		updateComponent( COMPONENT_DESCRIPTION_RESERVE ) ;
	}
	
	@Override
	synchronized public void setReserveIsSpecial( boolean special ) {
		super.setReserveIsSpecial(special) ;
		updateComponent( COMPONENT_DESCRIPTION_RESERVE ) ;
	}
	
	@Override
	synchronized public void setIsHost( boolean host ) {
		super.setIsHost(host) ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	synchronized public void setLocalPause( boolean localPause ) {
		super.setLocalPause(localPause) ;
		if ( mState == STATE_PAUSED )
			updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	synchronized public void setPlayerKickWarning( boolean warn, int slot, long kickAt ) {
		super.setPlayerKickWarning(warn, slot, kickAt) ;
		if ( mState == STATE_WAITING )
			updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
		if ( warn )
			mHandler.postDelayed(mRunnableUpdateKickCountdown, 1000) ;
	}
	
	// Helpful junk
	@Override
	synchronized public void setStateConnecting() {
		super.setStateConnecting() ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	@Override
	synchronized public void setStateTalking() {
		super.setStateTalking() ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	@Override
	synchronized public void setStatePaused( boolean [] pausedByPlayer ) {
		super.setStatePaused( pausedByPlayer ) ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	@Override
	synchronized public void setStateWaiting( boolean [] waitingForPlayer ) {
		super.setStateWaiting( waitingForPlayer ) ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	@Override
	synchronized public void setStateReady() {
		super.setStateReady() ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	@Override
	synchronized public void setStateStarting() {
		super.setStateStarting() ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	synchronized public void setStartLoading() {
		super.setStartLoading() ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	synchronized public void setFinishedLoading() {
		super.setFinishedLoading() ;
		updateComponent( COMPONENT_STATE ) ;
		updateComponent( COMPONENT_STATE_DESCRIPTION ) ;
	}
	
	@Override
	synchronized public void setLogo( Bitmap logo ) {
		super.setLogo( logo ) ;
		updateComponent( COMPONENT_LOGO ) ;
	}
	
	@Override
	synchronized public void setControlsThumbnail( Drawable controls ) {
		super.setControlsThumbnail(controls) ;
		updateComponent( COMPONENT_DESCRIPTION_CONTROLS ) ;
		updateComponent( COMPONENT_THUMBNAIL_CONTROLS ) ;
	}
	
	@Override
	synchronized public void setDrawDetail( int detail ) {
		super.setDrawDetail(detail) ;
		updateComponent( COMPONENT_DRAW_DETAIL ) ;
	}
	
	
	public void setColorSchemeName( String scheme ) {
		super.setColorSchemeName(scheme) ;
		updateComponent( COMPONENT_COLOR_SCHEME ) ;
	} 
	

	@Override
	synchronized public void setSoundOn( boolean on, boolean isMutedByRinger ) {
		super.setSoundOn(on, isMutedByRinger) ;
		updateComponent( COMPONENT_SOUND ) ;
		updateComponent( COMPONENT_MUSIC ) ;
		updateComponent( COMPONENT_MUTE_ALERT ) ;
	}
	
	@Override
	public void setSoundVolumePercent( int vol ) {
		if ( vol != mSoundVolumePercent ) {
			super.setSoundVolumePercent(vol) ;
			refreshVolumeText( mwrActivity.get().getResources() ) ;
			updateComponent( COMPONENT_SOUND ) ;
		}
	}
	
	@Override
	public void setMusicVolumePercent( int vol ) {
		if ( vol != mMusicVolumePercent ) {
			super.setMusicVolumePercent(vol) ;
			refreshVolumeText( mwrActivity.get().getResources() ) ;
			updateComponent( COMPONENT_MUSIC ) ;
		}
	}
	
	@Override
	public void setControlsGamepad( boolean on, boolean fall, boolean doubleTap ) {
		if ( mControlsGamepad != on || mControlsGamepadDownFall != fall || mControlsGamepadDownDoubleTap != doubleTap ) {
			super.setControlsGamepad(on, fall, doubleTap) ;
			updateComponent( COMPONENT_DESCRIPTION_CONTROLS ) ;
			updateComponent( COMPONENT_DESCRIPTION_EXPANDED_CONTROLS ) ;
		}
	}
	
	@Override
	public void setControlsHas( boolean hasTurns, boolean hasFlips ) {
		if ( hasTurns != mControlsHasTurns || hasFlips != mControlsHasFlip ) {
			super.setControlsHas(hasTurns, hasFlips) ;
			updateComponent( COMPONENT_DESCRIPTION_CONTROLS ) ;
			updateComponent( COMPONENT_DESCRIPTION_EXPANDED_CONTROLS ) ;
		}
	}
	
	protected void updateComponent( int component ) {
		View v = mComponents[COMPONENT_LAYOUT] ;
		mNeedsUpdate[component] = true ;
		if ( v != null && v.getVisibility() == View.VISIBLE || v.getVisibility() == View.INVISIBLE )
			mwrActivity.get().runOnUiThread(mRunnableUpdate) ;
	}
	
	protected void updateComponentForce( int component ) {
		
		ImageView iv ;
		TextView tv ;
		View v = mComponents[component] ;
		
		String s ;
		
		int index ;
		int sindex ;
		
		switch ( component ) {
		case COMPONENT_GAME_MODE:
			tv = (TextView)v ;
			if ( tv != null ) {
				tv.setText(mTextGameMode) ;
			}
			break ;
		
		case COMPONENT_DESCRIPTION_NEXT:
		case COMPONENT_DESCRIPTION_RESERVE:
		case COMPONENT_DESCRIPTION_FALLING:
			int gameRole ;
			GameBlocksSlice slice ;
			if ( component == COMPONENT_DESCRIPTION_NEXT ) {
				gameRole = INDEX_GAME_ROLE_NEXT ;
				slice = mSliceNext ;
			} else if ( component == COMPONENT_DESCRIPTION_RESERVE ) {
				gameRole = mReserveIsSpecial ? INDEX_GAME_ROLE_SPECIAL : INDEX_GAME_ROLE_RESERVE ;
				slice = mSliceReserve ;
			} else {
				gameRole = INDEX_GAME_ROLE_FALLING ;
				slice = mSlice ;
			}
			
			tv = (TextView)v ;
			if ( tv != null ) {
				if ( slice == null )
					tv.setText(null) ;
				else {
					int pieceType = slice.getPieceType() ;
					int catIndex = getCategoryIndex( pieceType ) ;
					int qoIndex = getQOrientationIndex( pieceType ) ;
					tv.setText( mTextPiece[gameRole][catIndex][qoIndex] ) ;
				}
			}
			break ;
			
		case COMPONENT_STATE:
			tv = (TextView)v ;
			if ( tv != null ) {
				refreshGameStateText(mState) ;
				refreshGameStateAbrvText(mState) ;
				index = getGameStateIndex( mState ) ;
				tv.setText( mTextState[index] ) ;
				if ( tv.getLineCount() > 1 )
					tv.setText( mTextStateAbrv[index] ) ;
			}
			break ;
			
		case COMPONENT_STATE_DESCRIPTION:
			tv = (TextView)v ;
			if ( tv != null ) {
				refreshGameStateDescriptionText(mState) ;
				index = getGameStateIndex( mState ) ;
				tv.setText( mTextStateDescription[index] ) ;
			}
			break ;
			
		case COMPONENT_DESCRIPTION_CONTROLS:
			tv = (TextView)v ;
			if ( tv != null ) {
				if ( mControlsGamepad ) {
					index = mControlsThumbnail == null && mComponents[COMPONENT_THUMBNAIL_CONTROLS] != null
							? INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_NO
							: INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_YES ;
				} else {
					index = mControlsThumbnail == null && mComponents[COMPONENT_THUMBNAIL_CONTROLS] != null
							? INDEX_HAS_CONTROLS_GESTURE_BITMAP_NO
							: INDEX_HAS_CONTROLS_GESTURE_BITMAP_YES ;
				}
				
				tv.setText( mTextControls[index] ) ;
			}
			break ;
			
		case COMPONENT_DESCRIPTION_EXPANDED_CONTROLS:
			tv = (TextView)v ;
			if ( tv != null ) {
				if ( mControlsGamepad ) {
					if ( mControlsGamepadDownFall && mControlsGamepadDownDoubleTap )
						index = INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL_DOUBLE_TAP ;
					else if ( mControlsGamepadDownFall && !mControlsGamepadDownDoubleTap )
						index = INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL ;
					else if ( !mControlsGamepadDownFall && mControlsGamepadDownDoubleTap )
						index = INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP_DOUBLE_TAP ;
					else
						index = INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP ;
					sindex = INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD ;
				} else {
					index = INDEX_CONTROLS_EXPANDED_GESTURE ;
					sindex = INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE ;
				}
				
				s = mTextControlsExpanded[index] ;
				s = s.replace(this.mTextControlsExpandedExerptPlaceholder,
						this.mTextControlsExerpt[sindex][mControlsHasTurns ? 1 : 0][mControlsHasFlip ? 1 : 0]) ;
				tv.setText( s ) ;
			}
			break ;
			
		case COMPONENT_LOGO:
			iv = (ImageView)v ;
			if ( iv != null )
				iv.setImageBitmap(mLogo) ;
			break ;
			
		case COMPONENT_THUMBNAIL_CONTROLS:
			iv = (ImageView)v ;
			if ( iv != null ) {
				iv.setImageDrawable(mControlsThumbnail) ;
			}
			break ;
			
		case COMPONENT_DRAW_DETAIL:
			tv = (TextView)v ;
			if ( tv != null )
				tv.setText(mTextDrawDetail[mDrawDetail]) ;
			break ;
			
		case COMPONENT_COLOR_SCHEME:
			tv = (TextView)v ;
			if ( tv != null ) {
				s = TextFormatting.format(
						mwrActivity.get(),
						null,
						TextFormatting.DISPLAY_MENU,
						TextFormatting.TYPE_GAME_PAUSE_OVERLAY_COLORS,
						TextFormatting.ROLE_CLIENT,
						mColorSchemeName) ;
				Log.d(TAG, "setting text view for color scheme to " + s) ;
				tv.setText(s) ;
			} else
				Log.d(TAG, "no view to set") ;
			break ;
		
		case COMPONENT_SOUND:
			tv = (TextView)v ;
			if ( tv != null ) {
				if ( mSoundOn && !mIsMutedByRinger )
					index = INDEX_SOUND_ON_YES ;
				else if ( mIsMutedByRinger )
					index = INDEX_SOUND_ON_NO_BY_RINGER ;
				else
					index = INDEX_SOUND_ON_NO ;
				tv.setText( mTextSound[index] ) ;
			}
			break ;
			
		case COMPONENT_MUSIC:
			tv = (TextView)v ;
			if ( tv != null ) {
				if ( mSoundOn && !mIsMutedByRinger )
					index = INDEX_MUSIC_ON_YES ;
				else if ( mIsMutedByRinger )
					index = INDEX_MUSIC_ON_NO_BY_RINGER ;
				else
					index = INDEX_MUSIC_ON_NO ;
				tv.setText( mTextSound[index] ) ;
			}
			
		case COMPONENT_MUTE_ALERT:
			tv = (TextView)v ;
			if ( tv != null ) {
				if ( mSoundOn && !mIsMutedByRinger )
					index = INDEX_SOUND_ON_YES ;
				else if ( mIsMutedByRinger )
					index = INDEX_SOUND_ON_NO_BY_RINGER ;
				else
					index = INDEX_SOUND_ON_NO ;
				tv.setText( mTextMuteAlert[index] ) ;
			}
		}
	}
	
	
	protected void updateGameModeString() {
		
		if ( mGameSettings == null ) {
			mTextGameMode = null ;
			return ;
		}
		
		Resources res = mwrActivity.get().getResources() ;
		
		if ( mGameSettings.hasDefaultsIgnoringDifficulty() ) {
			if ( mGameSettings.hasDifficulty() )
				mTextGameMode = res.getString(R.string.game_interface_pause_game_mode_settings_default_has_difficulty) ;
			else
				mTextGameMode = res.getString(R.string.game_interface_pause_game_mode_settings_default) ;
		} else {
			if ( mGameSettings.hasDifficulty() )
				mTextGameMode = res.getString(R.string.game_interface_pause_game_mode_settings_custom_has_difficulty) ;
			else
				mTextGameMode = res.getString(R.string.game_interface_pause_game_mode_settings_custom) ;
			
			// create custom string.
    		boolean has = false ;
    		StringBuilder sb = new StringBuilder() ;
    		
    		String sep = res.getString(R.string.play_list_separator) ;
    		
    		// Custom level?
    		if ( mGameSettings.hasLevel() ) {
    			String str ;
    			if ( mGameSettings.hasLevelLock() && mGameSettings.getLevelLock() )
    				str = res.getString(R.string.play_list_level_locked) ;
    			else
    				str = res.getString(R.string.play_list_level_starting) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_level),
    					"" + mGameSettings.getLevel()) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		// Custom clears-per?
    		if ( mGameSettings.hasClearsPerLevel() ) {
    			String str = res.getString(R.string.play_list_clears_per_level) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_clears_per_level),
    					"" + mGameSettings.getClearsPerLevel()) ;
    			if ( has )
    				sb.append(sep) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		// Custom Garbage?
    		if ( mGameSettings.hasGarbage() || mGameSettings.hasGarbagePerLevel() ) {
    			String strDefault = res.getString(R.string.play_list_default) ;
    			String str = res.getString(R.string.play_list_garbage) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_garbage),
    					mGameSettings.hasGarbage() ? "" + mGameSettings.getGarbage() : strDefault) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_garbage_per_level),
    					mGameSettings.hasGarbagePerLevel() ? "" + mGameSettings.getGarbagePerLevel() : strDefault) ;
    			
    			if ( has )
    				sb.append(sep) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		// Fixed Rate?
    		if ( mGameSettings.hasDisplacementFixedRate() ) {
    			String str = res.getString(R.string.play_list_displacement_fixed_rate) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_displacement_fixed_rate),
    					"" + String.format("%.2f", mGameSettings.getDisplacementFixedRate())) ;
    			if ( has )
    				sb.append(sep) ;
    			sb.append(str) ;
    			has = true ;
    		}
       		
    		// place the string in mTextGameMode.
    		mTextGameMode = mTextGameMode.replace(
    				res.getString(R.string.placeholder_game_settings_custom_list),
    				sb.toString()) ;
		}
		
		mTextGameMode = mTextGameMode.replace(
				res.getString(R.string.placeholder_game_mode_name),
				GameModes.name(mGameSettings.getMode())) ;
		
		int difficultyResID = R.string.game_interface_pause_game_mode_settings_difficulty_normal ;
		switch( mGameSettings.getDifficulty() ) {
		case GameInformation.DIFFICULTY_PRACTICE:
			difficultyResID = R.string.game_interface_pause_game_mode_settings_difficulty_practice ;
			break ;
		case GameInformation.DIFFICULTY_HARD:
			difficultyResID = R.string.game_interface_pause_game_mode_settings_difficulty_hard ;
			break ;
		}
		
		mTextGameMode = mTextGameMode.replace(
				res.getString(R.string.placeholder_game_settings_custom_value_difficulty),
				res.getString(difficultyResID)) ;
		
		
		// that's the standard game mode string.  If this is a timed trial, we
		// include trial details as a second line.
		
		if ( mGameIsTrial ) {
			String timeTrialString = res.getString(R.string.game_interface_pause_game_mode_is_trial) ;
			// we represent this as minutes, to the nearest 1/2.
			int wholeMinutes = mGameTrialSeconds / 60 ;
			boolean halfMinute = mGameTrialSeconds % 60 >= 30 ;
			
			StringBuilder sb = new StringBuilder() ;
			sb.append(wholeMinutes) ;
			if ( halfMinute )
				sb.append(".5") ;
			String minutesStr = sb.toString() ;
			
			timeTrialString = timeTrialString.replace(
					res.getString(R.string.placeholder_game_is_trial_duration_number),
					minutesStr) ;
			
			mTextGameMode = mTextGameMode + "\n\n" + timeTrialString ;
		}
	}
	

	@Override
	protected void didChangeComponentReference(int component) {
		// update this component
		updateComponent( component ) ;
	}

	@Override
	protected void didChange() {
		// nothing here; empty implementation
	}
	
	
	protected int getCategoryIndex( int type ) {
		if ( PieceCatalog.isTetromino(type) ) {
			int cat = PieceCatalog.getTetrominoCategory(type) ;
			switch( cat ) {
			case PieceCatalog.TETRO_CAT_LINE:
				return INDEX_CATEGORY_TETRO_LINE ;
			case PieceCatalog.TETRO_CAT_GAMMA:
				return INDEX_CATEGORY_TETRO_GAMMA ;
			case PieceCatalog.TETRO_CAT_GUN:
				return INDEX_CATEGORY_TETRO_GUN ;
			case PieceCatalog.TETRO_CAT_SQUARE:
				return INDEX_CATEGORY_TETRO_SQUARE ;
			case PieceCatalog.TETRO_CAT_S:
				return INDEX_CATEGORY_TETRO_S ;
			case PieceCatalog.TETRO_CAT_Z:
				return INDEX_CATEGORY_TETRO_Z ;
			case PieceCatalog.TETRO_CAT_T:
				return INDEX_CATEGORY_TETRO_T ;
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		} else if ( PieceCatalog.isTetracube(type) ) {
			int cat = PieceCatalog.getTetracubeCategory(type) ;
			int scat = PieceCatalog.getTetracubeSubcategory(type) ;
			switch( cat ) {
			case PieceCatalog.TETRA_CAT_L:
				return INDEX_CATEGORY_TETRA_L ;
			case PieceCatalog.TETRA_CAT_RECT:
				return INDEX_CATEGORY_TETRA_RECT ;
			case PieceCatalog.TETRA_CAT_S:
				return INDEX_CATEGORY_TETRA_ZIG_ZAG ;
			case PieceCatalog.TETRA_CAT_T:
				return INDEX_CATEGORY_TETRA_3DT ;
			case PieceCatalog.TETRA_CAT_BRANCH:
				return INDEX_CATEGORY_TETRA_BRANCH ;
			case PieceCatalog.TETRA_CAT_SCREW:
				return INDEX_CATEGORY_TETRA_SCREW ;
			case PieceCatalog.TETRA_CAT_CORNER:
				if ( scat == 0 || scat == 3 )
					return INDEX_CATEGORY_TETRA_SINISTER ;
				else
					return INDEX_CATEGORY_TETRA_DEXTER ;	
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		} else if ( PieceCatalog.isSpecial(type) ) {
			int cat = PieceCatalog.getSpecialCategory(type) ;
			switch ( cat ) {
			case PieceCatalog.SPECIAL_CAT_FLASH:
				return INDEX_CATEGORY_SPECIAL_FLASH ;
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		} else
			return INDEX_CATEGORY_SPECIAL_NONE ;
	}
	
	protected int getQOrientationIndex( int type ) {
		int qc = PieceCatalog.getQCombination(type) ;
		
		// retro?
		if ( QOrientations.R0 <= qc && qc <= QOrientations.R6 )
			return INDEX_QORIENTATION_RETRO ;
		if ( qc == QOrientations.S0 )
			return INDEX_QORIENTATION_S0 ;
		if ( qc == QOrientations.S1 )
			return INDEX_QORIENTATION_S1 ;
		if ( qc == QCombinations.SS )
			return INDEX_QORIENTATION_SS ;
		if ( qc == QOrientations.ST )
			return INDEX_QORIENTATION_ST ;
		if ( qc == QCombinations.UL )
			return INDEX_QORIENTATION_UL ;
		if ( qc == QCombinations.SL )
			return INDEX_QORIENTATION_SL ;
		if ( qc == QOrientations.F0 || qc == QOrientations.F1 )
			return INDEX_QORIENTATION_F ;
		
		return INDEX_QORIENTATION_NONE ;
	}
	
	protected int getGameStateIndex( int state ) {
		return state ;
	}
	
	
	protected void constructStrings() {
		Resources res = mwrActivity.get().getResources() ;
		constructPieceStrings( res ) ;
		constructStateStrings( res ) ;
		constructStateActionStrings( res ) ;
		constructControlsStrings( res ) ;
		constructDrawDetailStrings( res ) ;
		refreshVolumeText( res ) ;
		constructMuteAlertStrings( res ) ;
	}

	protected void constructPieceStrings( Resources res ) {
		String [] prefixes = new String[NUM_INDEX_GAME_ROLES] ;
		
		prefixes[INDEX_GAME_ROLE_NEXT] = res.getString(R.string.game_interface_pause_header_next) ;
		prefixes[INDEX_GAME_ROLE_RESERVE] = res.getString(R.string.game_interface_pause_header_reserve) ;
		prefixes[INDEX_GAME_ROLE_SPECIAL] = res.getString(R.string.game_interface_pause_header_special) ;
		prefixes[INDEX_GAME_ROLE_FALLING] = res.getString(R.string.game_interface_pause_header_falling) ;

		for ( int role = 0; role < NUM_INDEX_GAME_ROLES; role++ ) {
			String prefix = prefixes[role] ;
			
			// Tetrominoes!
			constructAndSetTetrominoStrings( res, role, prefix, INDEX_CATEGORY_TETRO_LINE ) ;
			constructAndSetTetrominoStrings( res, role, prefix, INDEX_CATEGORY_TETRO_GAMMA ) ;
			constructAndSetTetrominoStrings( res, role, prefix, INDEX_CATEGORY_TETRO_GUN ) ;
			constructAndSetTetrominoStrings( res, role, prefix, INDEX_CATEGORY_TETRO_SQUARE ) ;
			constructAndSetTetrominoStrings( res, role, prefix, INDEX_CATEGORY_TETRO_S ) ;
			constructAndSetTetrominoStrings( res, role, prefix, INDEX_CATEGORY_TETRO_Z ) ;
			constructAndSetTetrominoStrings( res, role, prefix, INDEX_CATEGORY_TETRO_T ) ;
			
			// Tetracubes!
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_L ) ;
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_RECT ) ;
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_ZIG_ZAG ) ;
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_3DT ) ;
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_BRANCH ) ;
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_SCREW ) ;
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_DEXTER ) ;
			constructAndSetTetracubeStrings( res, role, prefix, INDEX_CATEGORY_TETRA_SINISTER ) ;
			
			// Other specials!  (flash!)
			constructAndSetSpecialStrings( res, role, prefix, INDEX_CATEGORY_SPECIAL_FLASH ) ;
		}
		
		empty_CASTS(res) ;
	}
	
	
	
	protected void constructStateStrings( Resources res ) {
		for ( int i = 0; i < NUM_STATES; i++ ) {
			refreshGameStateText(i) ;
			refreshGameStateAbrvText(i) ;
		}
	}
	
	protected void constructStateActionStrings( Resources res ) {
		for ( int i = 0; i < NUM_STATES; i++ )
			refreshGameStateDescriptionText(i) ;
	}
	
	protected void constructControlsStrings( Resources res ) {
		mTextControls[INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_NO] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_thumbnail_no) ;
		mTextControls[INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_YES] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_thumbnail_yes) ;
		mTextControls[INDEX_HAS_CONTROLS_GESTURE_BITMAP_NO] = res.getString(R.string.game_interface_pause_controls_gesture_instructions_thumbnail_no) ;
		mTextControls[INDEX_HAS_CONTROLS_GESTURE_BITMAP_YES] = res.getString(R.string.game_interface_pause_controls_gesture_instructions_thumbnail_yes) ;
	
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL_DOUBLE_TAP] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_fall_double_tap) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_fall) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP_DOUBLE_TAP] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_drop_double_tap) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_drop) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GESTURE] = res.getString(R.string.game_interface_pause_controls_gesture_instructions_expanded) ;

		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][1][0] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_exerpt_has_turn) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][0][1] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_exerpt_has_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][1][1] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_exerpt_has_turn_and_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][0][0] = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_exerpt_has_move_only) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][1][0] = res.getString(R.string.game_interface_pause_controls_gestures_instructions_expanded_excerpt_has_turn) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][0][1] = res.getString(R.string.game_interface_pause_controls_gestures_instructions_expanded_excerpt_has_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][1][1] = res.getString(R.string.game_interface_pause_controls_gestures_instructions_expanded_excerpt_has_turn_and_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][0][0] = res.getString(R.string.game_interface_pause_controls_gestures_instructions_expanded_excerpt_has_move_only) ;
		
		this.mTextControlsExpandedExerptPlaceholder = res.getString(R.string.game_interface_pause_controls_gamepad_instructions_expanded_exerpt_placeholder) ;
	}
	
	protected void constructDrawDetailStrings( Resources res ) {
		mTextDrawDetail[DrawSettings.DRAW_DETAIL_MINIMAL] = res.getString(R.string.game_interface_pause_draw_detail_minimal) ;
		mTextDrawDetail[DrawSettings.DRAW_DETAIL_LOW] = res.getString(R.string.game_interface_pause_draw_detail_low) ;
		mTextDrawDetail[DrawSettings.DRAW_DETAIL_MID] = res.getString(R.string.game_interface_pause_draw_detail_mid) ;
		mTextDrawDetail[DrawSettings.DRAW_DETAIL_HIGH] = res.getString(R.string.game_interface_pause_draw_detail_high) ;
	}
	
	protected void constructSoundStrings( Resources res ) {
		mTextSound[INDEX_SOUND_ON_YES] = res.getString(R.string.game_interface_pause_sound_on_yes) ;
		mTextSound[INDEX_SOUND_ON_NO] = res.getString(R.string.game_interface_pause_sound_on_no) ;
		mTextSound[INDEX_SOUND_ON_NO_BY_RINGER] = res.getString(R.string.game_interface_pause_sound_on_no_by_ringer) ;
	}
	
	protected void constructMuteAlertStrings( Resources res ) {
		mTextMuteAlert[INDEX_SOUND_ON_YES] = null ;
		mTextMuteAlert[INDEX_SOUND_ON_NO] = res.getString(R.string.game_interface_pause_mute_alert_muted) ;
		mTextMuteAlert[INDEX_SOUND_ON_NO_BY_RINGER] = res.getString(R.string.game_interface_pause_mute_alert_muted_by_ringer) ;
	}
	
	protected void refreshGameStateText( int state ) {
		Resources res = mwrActivity.get().getResources() ;
		
		int display = TextFormatting.DISPLAY_MENU ;
		int type = -1 ;
		int role = mIsHost ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT ;
		boolean local = mNumberOfPlayers <= 1 ;
		switch( state ) {
		case STATE_LOADING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_STATE_LOADING ;
			break ;
		case STATE_CONNECTING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_CONNECTING ;
			break ;
		case STATE_TALKING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_NEGOTIATING ;
			break ;
		case STATE_WAITING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_WAITING ;
			break ;
		case STATE_PAUSED:
			if ( local )
				type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_PAUSED ;
			else {
				if ( this.mPlayersParticipating[this.mThisPlayer] )
					type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_PAUSED_BY_PLAYER ;
				else
					type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_PAUSED_BY_OTHER ;
			}
			break ;
		case STATE_READY:
			type = local
					? TextFormatting.TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_READY
					: TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_READY ;
			break ;
		case STATE_STARTING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_STARTING ;
			break ;
		}
		
		// Consider: if PAUSED_BY_PLAYER, then the format string does not contain space for names.
		// Otherwise, it does, but this player's name is not included.
		mTextState[state] = TextFormatting.format(null, res, display, type, role,
				ArrayOps.clone(mPlayerNames),
				ArrayOps.clone(mPlayersParticipating)) ;
	}
	
	protected void refreshGameStateAbrvText( int state ) {
		Resources res = mwrActivity.get().getResources() ;
		String s = null ;
		
		boolean local = mNumberOfPlayers <= 1 ;
		switch( state ) {
		case STATE_LOADING:
			s = res.getString(R.string.game_interface_pause_state_loading_abrv) ;
			break ;
		case STATE_CONNECTING:
			s = res.getString(R.string.game_interface_pause_state_connecting_abrv) ;
			break ;
		case STATE_TALKING:
			s = res.getString(R.string.game_interface_pause_state_talking_abrv) ;
			break ;
		case STATE_WAITING:
			s = res.getString(R.string.game_interface_pause_state_waiting_abrv) ;
			break ;
		case STATE_PAUSED:
			s = res.getString(R.string.game_interface_pause_state_paused_abrv) ;
			break ;
		case STATE_READY:
			s = res.getString(R.string.game_interface_pause_state_ready_abrv) ;
			break ;
		case STATE_STARTING:
			s = res.getString(R.string.game_interface_pause_state_starting_abrv) ;
			break ;
		}
		mTextStateAbrv[state] = s ;
	}
	
	
	
	
	protected void refreshGameStateDescriptionText( int state ) {
		Resources res = mwrActivity.get().getResources() ;
		
		if ( state == STATE_WAITING ) {
			// see if we're waiting for a drop
			int playerToDrop = getNextDropPlayer() ;
			if ( playerToDrop >= 0 ) {
				long timeToDrop = this.mPlayerKickTime[playerToDrop] ;
				String str = res.getString(R.string.game_interface_pause_state_description_client_waiting_to_drop) ;
				str = str.replace(res.getString(R.string.placeholder_duration_seconds),
						"" + (int)Math.ceil(
								Math.max(1, timeToDrop - System.currentTimeMillis()) / 1000))
						.replace(res.getString(R.string.placeholder_names_array_formatted),
								mPlayerNames[playerToDrop]) ;
				mTextStateDescription[state] = str ;
				return ;
			}
		}
		
		int display = TextFormatting.DISPLAY_MENU ;
		int type = -1 ;
		int role = mIsHost ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT ;
		boolean local = mNumberOfPlayers <= 1 ;
		boolean other = false ;
		switch( state ) {
		case STATE_LOADING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_STATE_DESCRIPTION_LOADING ;
			break ;
		case STATE_CONNECTING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_CONNECTING ;
			break ;
		case STATE_TALKING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_NEGOTIATING ;
			break ;
		case STATE_WAITING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_WAITING ;
			break ;
		case STATE_PAUSED:
			if ( local )
				type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_DESCRIPTION_PAUSED ;
			else {
				for ( int i = 0; i < mPlayersParticipating.length; i++ ) {
					if ( this.mPlayersParticipating[i] && i != mThisPlayer )
						other = true ;
				}
				if ( this.mPlayersParticipating[this.mThisPlayer] && this.mLocalPause )
					type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_PLAYER ;
				else if ( other )
					type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_OTHER ;
				else
					type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_PAUSED_BY_PLAYER_REMOTE_ONLY ;
			}
			break ;
		case STATE_READY:
			type = local
					? TextFormatting.TYPE_GAME_PAUSE_OVERLAY_LOCAL_STATE_DESCRIPTION_READY
					: TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_READY ;
			break ;
		case STATE_STARTING:
			type = TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MULTIPLAYER_STATE_DESCRIPTION_STARTING ;
			break ;
		}
		
		mTextStateDescription[state] = TextFormatting.format(null, res, display, type, role,
				ArrayOps.clone(mPlayerNames),
				ArrayOps.clone(mPlayersParticipating)) ;
	}
	
	protected void refreshVolumeText( Resources res ) {
		int display = TextFormatting.DISPLAY_MENU ;
		int role = mIsHost ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT ;
		
		this.mTextSound[INDEX_SOUND_ON_NO_BY_RINGER]
		                = TextFormatting.formatInt(null, res, display,
		                		TextFormatting.TYPE_GAME_PAUSE_OVERLAY_SOUND_OFF_BY_RINGER,
		                		role, mSoundVolumePercent) ;
		this.mTextSound[INDEX_SOUND_ON_NO]
		                = TextFormatting.formatInt(null, res, display,
		                		TextFormatting.TYPE_GAME_PAUSE_OVERLAY_SOUND_OFF,
		                		role, mSoundVolumePercent) ;
		this.mTextSound[INDEX_SOUND_ON_YES]
		                = TextFormatting.formatInt(null, res, display,
		                		TextFormatting.TYPE_GAME_PAUSE_OVERLAY_SOUND_ON,
		                		role, mSoundVolumePercent) ;
		this.mTextSound[INDEX_MUSIC_ON_NO_BY_RINGER]
		                = TextFormatting.formatInt(null, res, display,
		                		TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MUSIC_OFF_BY_RINGER,
		                		role, mMusicVolumePercent) ;
		this.mTextSound[INDEX_MUSIC_ON_NO]
		                = TextFormatting.formatInt(null, res, display,
		                		TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MUSIC_OFF,
		                		role, mMusicVolumePercent) ;
		this.mTextSound[INDEX_MUSIC_ON_YES]
		                = TextFormatting.formatInt(null, res, display,
		                		TextFormatting.TYPE_GAME_PAUSE_OVERLAY_MUSIC_ON,
		                		role, mMusicVolumePercent) ;	
	}
	
	
	
	protected void constructAndSetTetrominoStrings( Resources res, int role, String prefix, int catIndex ) {
		// serves as a wrapper for the additional-arguments version.
		String category = null ;
		int categID, omSepID, ttSepID, omakeID, tltipID ;
		
		switch( catIndex ) {
		case INDEX_CATEGORY_TETRO_LINE:
			categID = R.string.game_interface_piece_category_tetro_line ;
			omSepID = R.string.game_interface_piece_category_tetro_line_omake_sep ;
			omakeID = R.string.game_interface_piece_category_tetro_line_omake ;
			ttSepID = R.string.game_interface_piece_category_tetro_line_tooltip_sep ;
			tltipID = R.string.game_interface_piece_category_tetro_line_tooltip ;
			break ;
		case INDEX_CATEGORY_TETRO_GUN:
			categID = R.string.game_interface_piece_category_tetro_gun ;
			omSepID = R.string.game_interface_piece_category_tetro_gun_omake_sep ;
			omakeID = R.string.game_interface_piece_category_tetro_gun_omake ;
			ttSepID = R.string.game_interface_piece_category_tetro_gun_tooltip_sep ;
			tltipID = R.string.game_interface_piece_category_tetro_gun_tooltip ;
			break ;
		case INDEX_CATEGORY_TETRO_GAMMA:
			categID = R.string.game_interface_piece_category_tetro_gamma ;
			omSepID = R.string.game_interface_piece_category_tetro_gamma_omake_sep ;
			omakeID = R.string.game_interface_piece_category_tetro_gamma_omake ;
			ttSepID = R.string.game_interface_piece_category_tetro_gamma_tooltip_sep ;
			tltipID = R.string.game_interface_piece_category_tetro_gamma_tooltip ;
			break ;
		case INDEX_CATEGORY_TETRO_SQUARE:
			categID = R.string.game_interface_piece_category_tetro_square ;
			omSepID = R.string.game_interface_piece_category_tetro_square_omake_sep ;
			omakeID = R.string.game_interface_piece_category_tetro_square_omake ;
			ttSepID = R.string.game_interface_piece_category_tetro_square_tooltip_sep ;
			tltipID = R.string.game_interface_piece_category_tetro_square_tooltip ;
			break ;
		case INDEX_CATEGORY_TETRO_S:
			categID = R.string.game_interface_piece_category_tetro_s ;
			omSepID = R.string.game_interface_piece_category_tetro_s_omake_sep ;
			omakeID = R.string.game_interface_piece_category_tetro_s_omake ;
			ttSepID = R.string.game_interface_piece_category_tetro_s_tooltip_sep ;
			tltipID = R.string.game_interface_piece_category_tetro_s_tooltip ;
			break ;
		case INDEX_CATEGORY_TETRO_Z:
			categID = R.string.game_interface_piece_category_tetro_z ;
			omSepID = R.string.game_interface_piece_category_tetro_z_omake_sep ;
			omakeID = R.string.game_interface_piece_category_tetro_z_omake ;
			ttSepID = R.string.game_interface_piece_category_tetro_z_tooltip_sep ;
			tltipID = R.string.game_interface_piece_category_tetro_z_tooltip ;
			break ;
		case INDEX_CATEGORY_TETRO_T:
			categID = R.string.game_interface_piece_category_tetro_t ;
			omSepID = R.string.game_interface_piece_category_tetro_t_omake_sep ;
			omakeID = R.string.game_interface_piece_category_tetro_t_omake ;
			ttSepID = R.string.game_interface_piece_category_tetro_t_tooltip_sep ;
			tltipID = R.string.game_interface_piece_category_tetro_t_tooltip ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize tetromino index " + catIndex) ;
		}
		
		boolean omake = role == INDEX_GAME_ROLE_RESERVE || role == INDEX_GAME_ROLE_SPECIAL ;
		category = res.getString(categID) ;
		constructAndSetTetrominoStrings(
				res, role, prefix, catIndex, category,
				omake ? omSepID : ttSepID,
				omake ? omakeID : tltipID ) ;
	}
	
	
	protected void constructAndSetTetracubeStrings( Resources res, int role, String prefix, int catIndex ) {
		// serves as a wrapper for the additional-arguments version.
		String category = null ;
		int categID ;
		
		switch( catIndex ) {
		case INDEX_CATEGORY_TETRA_L:
			categID = R.string.game_interface_piece_category_tetra_l ;
			break ;
		case INDEX_CATEGORY_TETRA_RECT:
			categID = R.string.game_interface_piece_category_tetra_rect ;
			break ;
		case INDEX_CATEGORY_TETRA_ZIG_ZAG:
			categID = R.string.game_interface_piece_category_tetra_s ;
			break ;
		case INDEX_CATEGORY_TETRA_3DT:
			categID = R.string.game_interface_piece_category_tetra_t ;
			break ;
		case INDEX_CATEGORY_TETRA_BRANCH:
			categID = R.string.game_interface_piece_category_tetra_branch ;
			break ;
		case INDEX_CATEGORY_TETRA_SCREW:
			categID = R.string.game_interface_piece_category_tetra_screw ;
			break ;
		case INDEX_CATEGORY_TETRA_DEXTER:
			categID = R.string.game_interface_piece_category_tetra_corner_right ;
			break ;
		case INDEX_CATEGORY_TETRA_SINISTER:
			categID = R.string.game_interface_piece_category_tetra_corner_left ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize tetracube index " + catIndex) ;
		}
		
		category = res.getString(categID) ;
		constructAndSetTetracubeStrings(
				res, role, prefix, catIndex, category  ) ;
	}
	
	
	protected void constructAndSetSpecialStrings( Resources res, int role, String prefix, int catIndex ) {
		switch( catIndex ) {
		case INDEX_CATEGORY_SPECIAL_FLASH:
			mTextPiece[role][catIndex][INDEX_QORIENTATION_F] = 
				constructPieceString( res, prefix, placeholder, "", fl_cw, fl_sp, fl_tt ) ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize special index " + catIndex) ;
		}
	}

	
	protected void constructAndSetTetrominoStrings( Resources res, int role, String prefix, int catIndex, String category, int sepID, int descID ) {
		
		set_CASTS( res ) ;
		
		mTextPiece[role][catIndex][INDEX_QORIENTATION_RETRO] = 
			constructPieceString( res, prefix, category, sepID, descID ) ;
		mTextPiece[role][catIndex][INDEX_QORIENTATION_S0] = 
			constructPieceString( res, prefix, placeholder, category,
				s0_cw, s0_sp, s0_tt ) ;
		mTextPiece[role][catIndex][INDEX_QORIENTATION_S1] = 
			constructPieceString( res, prefix, placeholder, category,
				s1_cw, s1_sp, s1_tt ) ;
		mTextPiece[role][catIndex][INDEX_QORIENTATION_ST] = 
			constructPieceString( res, prefix, placeholder, category,
				st_cw, st_sp, st_tt ) ;
		mTextPiece[role][catIndex][INDEX_QORIENTATION_UL] = 
			constructPieceString( res, prefix, placeholder, category,
				ul_cw, ul_sp, ul_tt ) ;
	}
	
	protected void constructAndSetTetracubeStrings( Resources res, int role, String prefix, int catIndex, String category ) {
		
		set_CASTS( res ) ;
		
		mTextPiece[role][catIndex][INDEX_QORIENTATION_SS] = 
			constructPieceString( res, prefix, placeholder, category,
				ss_cw, ss_sp, ss_tt ) ;
		mTextPiece[role][catIndex][INDEX_QORIENTATION_SL] = 
			constructPieceString( res, prefix, placeholder, category,
				sl_cw, sl_sp, sl_tt ) ;
	}
	
	protected String constructPieceString( Resources res, String prefix, String categoryString, int sepStringID, int descStringID ) {
		return prefix
				+ categoryString
				+ res.getString(sepStringID) 
				+ res.getString(descStringID) ;
	}
	
	protected String constructPieceString( Resources res, String prefix, String categoryPlaceholder, String category, String wrapper, String sep, String desc ) {
		return prefix
				+ wrapper.replace(categoryPlaceholder, category)
				+ sep + desc ;
	}
	
	String s0_tt, s0_sp, s0_cw ;
	String s1_tt, s1_sp, s1_cw ;
	String ss_tt, ss_sp, ss_cw ;
	String fl_tt, fl_sp, fl_cw ;
	String ul_tt, ul_sp, ul_cw ;
	String st_tt, st_sp, st_cw ;
	String sl_tt, sl_sp, sl_cw ;
	String placeholder ;
	boolean casts_set = false ;
	
	protected void set_CASTS( Resources res ) {
		if ( !casts_set ) {
			s0_tt = res.getString(R.string.game_interface_piece_qpane_0_tooltip) ;
			s0_sp = res.getString(R.string.game_interface_piece_qpane_0_tooltip_sep) ;
			s0_cw = res.getString(R.string.game_interface_piece_qpane_0_wrapper) ;
			
			s1_tt = res.getString(R.string.game_interface_piece_qpane_1_tooltip) ;
			s1_sp = res.getString(R.string.game_interface_piece_qpane_1_tooltip_sep) ;
			s1_cw = res.getString(R.string.game_interface_piece_qpane_1_wrapper) ;
			
			ss_tt = res.getString(R.string.game_interface_piece_qpane_both_tooltip) ;
			ss_sp = res.getString(R.string.game_interface_piece_qpane_both_tooltip_sep) ;
			ss_cw = res.getString(R.string.game_interface_piece_qpane_both_wrapper) ;
			
			fl_tt = res.getString(R.string.game_interface_piece_special_f_tooltip) ;
			fl_sp = res.getString(R.string.game_interface_piece_special_f_tooltip_sep) ;
			fl_cw = res.getString(R.string.game_interface_piece_special_f_wrapper) ;
			
			ul_tt = res.getString(R.string.game_interface_piece_special_ul_tooltip) ;
			ul_sp = res.getString(R.string.game_interface_piece_special_ul_tooltip_sep) ;
			ul_cw = res.getString(R.string.game_interface_piece_special_ul_wrapper) ;
			
			st_tt = res.getString(R.string.game_interface_piece_special_st_tooltip) ;
			st_sp = res.getString(R.string.game_interface_piece_special_st_tooltip_sep) ;
			st_cw = res.getString(R.string.game_interface_piece_special_st_wrapper) ;
			
			sl_tt = res.getString(R.string.game_interface_piece_special_sl_tooltip) ;
			sl_sp = res.getString(R.string.game_interface_piece_special_sl_tooltip_sep) ;
			sl_cw = res.getString(R.string.game_interface_piece_special_sl_wrapper) ;
			
			placeholder = res.getString(R.string.placeholder_piece_category) ;
			
			casts_set = true ;
		}
	}
	
	protected void empty_CASTS( Resources res ) {
		s0_tt = s0_sp = s0_cw = null ;
		
		s1_tt = s1_sp = s1_cw = null ;
		
		ss_tt = ss_sp = ss_cw = null ;
		
		fl_tt = fl_sp = fl_cw = null ;
		
		ul_tt = ul_sp = ul_cw = null ;
		
		st_tt = st_sp = st_cw = null ;
		
		sl_tt = sl_sp = sl_cw = null ;
		
		placeholder = null ;
	}
}
