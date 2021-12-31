package com.peaceray.quantro.adapter.controls;

import java.lang.ref.WeakReference;

import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.peaceray.quantro.R;
import com.peaceray.quantro.adapter.action.ActionAdapter;
import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.view.controls.InvisibleControls;

public class ControlsToActionsAdapter implements InvisibleControls.Delegate {
	
	
	/**
	 * This interface defines a Delegate for handling any operations which
	 * Controls and ActionsAdapter cannot handle on their own.  The
	 * ControlsToActionsAdapter will call these methods on its delegate, if
	 * that delegate has been set, at the appropriate times.
	 * @author Jake
	 *
	 */
	public interface Delegate {

		
		/**
		 * The user has pressed the "opponent" button.  Probably should
		 * show them the opponent's game!
		 * 
		 * We can't handle this directly via our connection to an
		 * ActionsAdapter; whatever View is displaying the game
		 * should also be updated.
		 */
		public void ctaad_opponentButtonPressed() ;
		
		/**
		 * The user has released the "opponent" button.  Probably show
		 * their own game again!
		 * 
		 * We can't handle this directly via our connection to an ActionsAdapter;
		 * whatever View is displaying the game should also be updated.
		 */
		public void ctaad_opponentButtonReleased() ;
		
		
		/**
		 * The user has pressed the "score" button.  Probably should
		 * show them a detailed score display!
		 * 
		 * We can't handle this directly via our connection to an
		 * ActionsAdapter; whatever View is displaying the game
		 * should also be updated.
		 */
		public void ctaad_scoreButtonPressed() ;
		
		/**
		 * The user has released the "score" button.  Probably should stop showing them
		 * a detailed score display!
		 * 
		 * We can't handle this directly via our connection to an ActionsAdapter;
		 * whatever View is displaying the game should also be updated.
		 */
		public void ctaad_scoreButtonReleased() ;
		
	}

	

	private final String TAG = "CTActionsAdapter" ;

	// Structures to track the current state of the buttons.
	private boolean buttonsEnabled ;
	private boolean [] pressedSinceTick ;
	private boolean [] pressedNow ;
	private boolean [] leanedNow ;
	private long [] timeWhenLastPressed ;
	
	// Here are some important controls that might be pressed.
	public static final int RESERVE_BUTTON 		= 0 ;
	public static final int LEFT_BUTTON 		= 1 ;
	public static final int RIGHT_BUTTON 		= 2 ;
	public static final int CCW_BUTTON 			= 3 ;
	public static final int CW_BUTTON 			= 4 ;
	public static final int FLIP_BUTTON 		= 5 ;
	public static final int DOWN_BUTTON 		= 6 ;
	
	public static final int OPPONENT_BUTTON 	= 7 ;
	public static final int SCORE_BUTTON 		= 8 ;
	
	public static final int UP_BUTTON			= 9 ;
	public static final int SLIDE_LEFT_BUTTON 	= 10 ; 
	public static final int SLIDE_RIGHT_BUTTON 	= 11 ; 
	public static final int SLIDE_DOWN_BUTTON 	= 12 ;
	
	public static final int NUM_BUTTONS = 13 ;
	
	
	
	public static final int BEHAVIOR_DOWN_NONE = 0 ;
	public static final int BEHAVIOR_DOWN_DROP = 1 ;
	public static final int BEHAVIOR_DOWN_FALL = 2 ;
	public static final int BEHAVIOR_DOWN_AUTOFALL = 3 ;
	
	
	
	
	
	
	private String [] mButtonNames = new String[NUM_BUTTONS] ;
	
	private ActionAdapter actionAdapter ;
	private WeakReference<ControlsToActionsAdapter.Delegate> delegate = null ;
	
	private boolean mBehavior180Windup ;		// if true, 180s will be in the direction
												// of the 2nd button press (we "wind up" in one
												// direction, then turn in the other)
												// if false, 180s will be in the direction of the
												// 1st button.
	private int mAutoSlideDelay ;
	
	private boolean mBehaviorQuickSlideSupported ;
	private int mBehaviorDownButton ;
	private boolean mBehaviorDownButtonAutoLock ;
	private int mBehaviorSlideDownButton ;
	private boolean mBehaviorSlideDownButtonAutoLock ;
	
	
	
	private Handler mHoldHandler ;
	
	private static final int MESSAGE_WHAT_AUTOSLIDE = 0 ;

	private static final Integer MESSAGE_OBJECT_LEFT = Integer.valueOf(LEFT_BUTTON) ;
	private static final Integer MESSAGE_OBJECT_RIGHT = Integer.valueOf(RIGHT_BUTTON) ;
	
	
	
	
	public static final int BUTTON_TYPE_GAME = 0 ;
	public static final int BUTTON_TYPE_VIEW = 1 ;
	public static final int BUTTON_TYPE_ALL = 2 ;
	public static final int BUTTON_TYPE_TURN = 3 ;
	public static final int BUTTON_TYPE_GAME_NON_TURN = 4 ;
	public static final int BUTTON_TYPE_OPPONENT = 5 ;
	
	public ControlsToActionsAdapter( ) {
		delegate = new WeakReference<Delegate>(null) ;
		buttonsEnabled = true ;
		pressedSinceTick = new boolean[NUM_BUTTONS] ;
		pressedNow = new boolean[NUM_BUTTONS] ;
		leanedNow = new boolean[NUM_BUTTONS] ;
		timeWhenLastPressed = new long[NUM_BUTTONS] ;	
		
		for ( int i = 0; i < NUM_BUTTONS; i++ ) {
			pressedSinceTick[i] = false ;
			leanedNow[i] = false ;
			pressedNow[i] = false ;
		}
		
		mHoldHandler = new Handler() {
			@Override
            public void handleMessage(Message m) {
				if ( actionAdapter == null )
					return ;
				
				switch( m.what ) {
				case MESSAGE_WHAT_AUTOSLIDE:
					// Call this method to benefit from synchronization
					// (so we don't stop-then-start the same slide).
					startSlide( m.arg1  ) ;
					break ;
				
				}
            }
		} ;
	}
	
	public void recycle() {
		actionAdapter = null ;
		delegate = null ;
	}
	
	private void setDefaultBehavior( Resources res ) {
		// TODO: Load this from resources or SharedPreferences,
		// especially "slide time" (Delayed Auto Shift).
		mBehavior180Windup = true ;
		
		mBehaviorQuickSlideSupported = true ;
		mAutoSlideDelay
				= (int)GlobalTestSettings.scaleGameInputTimeWindow( res.getInteger( R.integer.controls_autoslide_hold_millis ) ) ;
		mBehaviorDownButton = BEHAVIOR_DOWN_DROP ;
		mBehaviorSlideDownButton = BEHAVIOR_DOWN_DROP ;
		mBehaviorDownButtonAutoLock = false ;
		mBehaviorSlideDownButtonAutoLock = false ;
	}
	
	// TODO: URGENT: Before beginning a game, call this method!
	public void loadResources( Resources newRes ) {
		setButtonNames(newRes) ;
		setDefaultBehavior(newRes);
	}
	
	public void setQuickSlideSupported( boolean supported ) {
		mBehaviorQuickSlideSupported = supported ;
	}
	
	public void setDownButtonBehavior( int behavior, boolean autolock ) {
		mBehaviorDownButton = behavior ;
		mBehaviorDownButtonAutoLock = autolock ;
	}
	
	public void setSlideDownButtonBehavior( int behavior, boolean autolock ) {
		mBehaviorSlideDownButton = behavior ;
		mBehaviorSlideDownButtonAutoLock = autolock ;
	}
	
	
	
	
	
	synchronized public void setActionAdapter( ActionAdapter adapter ) {
		this.actionAdapter = adapter ;
	}
	
	
	synchronized public void setDelegate( ControlsToActionsAdapter.Delegate delegate ) {
		this.delegate = new WeakReference<Delegate>(delegate) ;
	}
	
	
	public String [] getButtonNames( int type ) {
		switch( type ) {
		case BUTTON_TYPE_GAME:
			// game controls
			return new String[] {
					mButtonNames[RESERVE_BUTTON],
					mButtonNames[LEFT_BUTTON],
					mButtonNames[RIGHT_BUTTON],
					mButtonNames[CW_BUTTON],
					mButtonNames[CCW_BUTTON],
					mButtonNames[FLIP_BUTTON],
					mButtonNames[DOWN_BUTTON],
					mButtonNames[SLIDE_LEFT_BUTTON],
					mButtonNames[SLIDE_RIGHT_BUTTON],
					mButtonNames[SLIDE_DOWN_BUTTON]
					} ;
		case BUTTON_TYPE_VIEW:
			// view-changing controls; do not effect game state
			return new String[] {
					mButtonNames[OPPONENT_BUTTON],
					mButtonNames[SCORE_BUTTON]
			} ;
		case BUTTON_TYPE_ALL:
			// Both game and view buttons.
			return new String[] {
					mButtonNames[RESERVE_BUTTON],
					mButtonNames[LEFT_BUTTON],
					mButtonNames[RIGHT_BUTTON],
					mButtonNames[CW_BUTTON],
					mButtonNames[CCW_BUTTON],
					mButtonNames[FLIP_BUTTON],
					mButtonNames[DOWN_BUTTON],
					mButtonNames[SLIDE_LEFT_BUTTON],
					mButtonNames[SLIDE_RIGHT_BUTTON],
					mButtonNames[SLIDE_DOWN_BUTTON],
					mButtonNames[OPPONENT_BUTTON],
					mButtonNames[SCORE_BUTTON]
					} ;
			
		case BUTTON_TYPE_TURN:
			// return turn buttons
			return new String[] {
					mButtonNames[CW_BUTTON],
					mButtonNames[CCW_BUTTON]
					} ;
		case BUTTON_TYPE_GAME_NON_TURN:
			// game controls
			return new String[] {
					mButtonNames[RESERVE_BUTTON],
					mButtonNames[LEFT_BUTTON],
					mButtonNames[RIGHT_BUTTON],
					mButtonNames[FLIP_BUTTON],
					mButtonNames[DOWN_BUTTON],
					mButtonNames[SLIDE_LEFT_BUTTON],
					mButtonNames[SLIDE_RIGHT_BUTTON],
					mButtonNames[SLIDE_DOWN_BUTTON]
					} ;
		case BUTTON_TYPE_OPPONENT:
			return new String[] {
					mButtonNames[OPPONENT_BUTTON]
					} ;
		}
		
		return null ;
	}
	
	
	private synchronized void startSlide( int buttonNum ) {
		switch( buttonNum ) {
		case LEFT_BUTTON:
    		if ( pressedNow[LEFT_BUTTON] && mBehaviorQuickSlideSupported )
    			actionAdapter.controls_slide(ActionAdapter.MOVE_DIRECTION_LEFT, true) ;
    		break ;
    	case RIGHT_BUTTON:
    		if ( pressedNow[RIGHT_BUTTON] && mBehaviorQuickSlideSupported )
    			actionAdapter.controls_slide(ActionAdapter.MOVE_DIRECTION_RIGHT, true) ;
    		break ;
		}
	}
	
	
	private synchronized void cancelSlide( int buttonNum ) {
		switch ( buttonNum ) {
		case LEFT_BUTTON:
			if ( actionAdapter != null )
				actionAdapter.controls_slide(ActionAdapter.MOVE_DIRECTION_LEFT, false) ;
			mHoldHandler.removeMessages(MESSAGE_WHAT_AUTOSLIDE, MESSAGE_OBJECT_LEFT) ;
    		break ;
    	case RIGHT_BUTTON:
    		if ( actionAdapter != null )
    			actionAdapter.controls_slide(ActionAdapter.MOVE_DIRECTION_RIGHT, false) ;
    		mHoldHandler.removeMessages(MESSAGE_WHAT_AUTOSLIDE, MESSAGE_OBJECT_RIGHT) ;
    		break ;
		}
	}
	
	private synchronized void fall( boolean autolock ) {
		if ( autolock )
			actionAdapter.controls_fall_or_autolock() ;
		else
			actionAdapter.controls_fall() ;
	}
	
	private synchronized void startFastFall( boolean autolock ) {
		if ( autolock )
			actionAdapter.controls_fastFall_and_autolock(true) ;
		else
			actionAdapter.controls_fastFall(true) ;
	}
	
	private synchronized void cancelFastFall( boolean autolock ) {
		if ( autolock )
			actionAdapter.controls_fastFall_and_autolock(false) ;
		else
			actionAdapter.controls_fastFall(false) ;
	}
	
	
	
	/**
	 * A threadsafe method noting that a button has been pressed, so that
	 * the game state can be safely updated at the earliest convenience.
	 * @param Object The invisibleControls object.
	 * @param buttonName Name of the button pressed.
	 */
	@Override
	public synchronized void invisibleControlsUserDidPressButton(InvisibleControls invisControls,
			String buttonName) {
		
		if ( !buttonsEnabled )
			return ;
		
		// Note that this thing has been pressed.
		//Log.d(TAG, "Button pressed with name " + buttonName) ;
		int button_int = buttonNameToInteger(buttonName) ;

		
		if ( button_int > -1 )
			userDidPressButton(button_int) ;
	}
	
	/**
	 * A threadsafe method noting that a button has been released, so that
	 * the game state can be safely updated at the earliest convenience.
	 * @param Object The invisibleControls object.
	 * @param buttonName Name of the button released.
	 */
	@Override
	public synchronized void invisibleControlsUserDidReleaseButton(InvisibleControls invisControls,
			String buttonName) {
		
		// Note that this thing has been pressed.
		//Log.d(TAG, "Button released with name " + buttonName) ;
		int button_int = buttonNameToInteger(buttonName) ;
		userDidReleaseButton( button_int ) ;
	}
	
	@Override
	public synchronized void invisibleControlsUserDidTapButton(InvisibleControls invisControls,
			String buttonName) {
		
		if ( !buttonsEnabled )
			return ;
		
		// Note that this thing has been pressed.
		//Log.d(TAG, "Button pressed with name " + buttonName) ;
		int button_int = buttonNameToInteger(buttonName) ;

		
		if ( button_int > -1 )
			userDidTapButton(button_int) ;
		
	}
	
	
	/**
	 * Called when the user is "leaning" on a button, but not necessarily pressing it.
	 * 
	 * For "gamepad" style controls, this is equivalent to holding a button down (although it is a separate
	 * call from UserDidPressButton).
	 * 
	 * For "gesture" style controls, which rely on "tap button" for each movement step, this indicates
	 * that the "tap" is still ongoing and any turns should be in the currently leaned direction
	 * (if any).
	 * 
	 * Leaning does NOT activate any delayed-press actions.
	 * 
	 * @param invisControls
	 * @param buttonName
	 */
	public void invisibleControlsUserIsLeaningOnButton( InvisibleControls invisControls, String buttonName ) {
		if ( !buttonsEnabled )
			return ;
		int button_int = buttonNameToInteger(buttonName) ;
		if ( button_int > -1 )
			leanedNow[button_int] = true ;
		if ( button_int == LEFT_BUTTON || button_int == SLIDE_LEFT_BUTTON )
			leanedNow[RIGHT_BUTTON] = leanedNow[SLIDE_RIGHT_BUTTON] = false ;
		if ( button_int == RIGHT_BUTTON || button_int == SLIDE_RIGHT_BUTTON )
			leanedNow[LEFT_BUTTON] = leanedNow[SLIDE_LEFT_BUTTON] = false ;
		if ( button_int == DOWN_BUTTON || button_int == SLIDE_DOWN_BUTTON )
			leanedNow[UP_BUTTON] = false ;
		if ( button_int == UP_BUTTON  )
			leanedNow[DOWN_BUTTON] = leanedNow[SLIDE_DOWN_BUTTON] = false ;
		
	}
	
	
	/**
	 * The user is finised leaning on the indicated button.
	 * 
	 * @param invisControls
	 * @param buttonName
	 */
	public void invisibleControlsUserDoneLeaningOnButton( InvisibleControls invisControls, String buttonName ) {
		int button_int = buttonNameToInteger(buttonName) ;
		if ( button_int > -1 )
			leanedNow[button_int] = false ;
	}
	
	
	/**
	 * The user is done leaning on ALL buttons.
	 * 
	 * @param invisControls
	 */
	public void invisibleControlsUserDoneLeaningOnButtons( InvisibleControls invisControls ) {
		//Log.d(TAG, "userDoneLeaningOnButtons") ;
		for ( int i = 0; i < NUM_BUTTONS; i++ )
			leanedNow[i] = false ;
	}
	
	
	
	public synchronized void userDidPressButton( int button_int ) {
		if ( !buttonsEnabled || actionAdapter == null )
			return ;
		
		Delegate d = delegate.get() ;
		
		int action_lean = getActionAdapterLeanMovementDirection() ;
		
		pressedNow[button_int] = true ;
		
		// Queue up the action in the ActionAdapter.
		if ( button_int == LEFT_BUTTON ) {
			cancelSlide( RIGHT_BUTTON ) ;
			actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_LEFT ) ;
			if ( mAutoSlideDelay > 0 ) {
				mHoldHandler.sendMessageDelayed(
						mHoldHandler.obtainMessage(MESSAGE_WHAT_AUTOSLIDE, ActionAdapter.MOVE_DIRECTION_LEFT, 0, MESSAGE_OBJECT_LEFT),
						mAutoSlideDelay ) ;
			}
		}
			
		else if ( button_int == RIGHT_BUTTON ) {
			cancelSlide( LEFT_BUTTON ) ;
			actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_RIGHT ) ;
			if ( mAutoSlideDelay > 0 ) {
				mHoldHandler.sendMessageDelayed(
						mHoldHandler.obtainMessage(MESSAGE_WHAT_AUTOSLIDE, ActionAdapter.MOVE_DIRECTION_RIGHT, 0, MESSAGE_OBJECT_RIGHT),
						mAutoSlideDelay ) ;
			}
		}
		else if ( button_int == CW_BUTTON ) {
			if ( pressedNow[CCW_BUTTON] ) {
				if ( mBehavior180Windup )
					actionAdapter.controls_turnCW180( action_lean ) ;
				else 
					actionAdapter.controls_turnCCW180( action_lean ) ;
			} else
				actionAdapter.controls_turnCW( action_lean ) ;
		}
		else if ( button_int == CCW_BUTTON ) {
			if ( pressedNow[CW_BUTTON] ) {
				if ( mBehavior180Windup )
					actionAdapter.controls_turnCCW180( action_lean ) ;
				else 
					actionAdapter.controls_turnCW180( action_lean ) ;
			} else
				actionAdapter.controls_turnCCW( action_lean ) ;
		}
		else if ( button_int == FLIP_BUTTON ) {
			actionAdapter.controls_flip( action_lean ) ;
		}
		else if ( button_int == DOWN_BUTTON ) {
			if ( mBehaviorDownButton == BEHAVIOR_DOWN_DROP ) {
				actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_DOWN ) ;
				if ( mBehaviorDownButtonAutoLock )
					actionAdapter.controls_autolock() ;
			} else if ( mBehaviorDownButton == BEHAVIOR_DOWN_FALL ) {
				fall(mBehaviorDownButtonAutoLock) ;		// handles autolock
			} else if ( mBehaviorDownButton == BEHAVIOR_DOWN_AUTOFALL ) {
				startFastFall(mBehaviorDownButtonAutoLock) ;
			}
		}
		else if ( button_int == RESERVE_BUTTON )
			actionAdapter.controls_useReserve( action_lean ) ;
		else if ( button_int == OPPONENT_BUTTON && d != null )
			d.ctaad_opponentButtonPressed() ;
		else if ( button_int == SCORE_BUTTON && d != null )
			d.ctaad_scoreButtonPressed() ;
		
		else if ( button_int == SLIDE_LEFT_BUTTON ) {
			cancelSlide( RIGHT_BUTTON ) ;
			if ( mBehaviorQuickSlideSupported ) {
				actionAdapter.controls_slide( ActionAdapter.MOVE_DIRECTION_LEFT, true ) ;
			}
		}
			
		else if ( button_int == SLIDE_RIGHT_BUTTON ) {
			cancelSlide( LEFT_BUTTON ) ;
			if ( mBehaviorQuickSlideSupported ) {
				actionAdapter.controls_slide( ActionAdapter.MOVE_DIRECTION_RIGHT, true ) ;
			}
		}
		
		else if ( button_int == SLIDE_DOWN_BUTTON )  {
			if ( mBehaviorSlideDownButton == BEHAVIOR_DOWN_DROP ) {
				actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_DOWN ) ;
				if ( mBehaviorSlideDownButtonAutoLock )
					actionAdapter.controls_autolock() ;
			} else if ( mBehaviorSlideDownButton == BEHAVIOR_DOWN_FALL ) {
				fall(mBehaviorSlideDownButtonAutoLock) ;
			} else if ( mBehaviorSlideDownButton == BEHAVIOR_DOWN_AUTOFALL ) {
				startFastFall(mBehaviorSlideDownButtonAutoLock) ;
			}
		}
	}

	
	private int getActionAdapterLeanMovementDirection() {
		// Lean is based on the status of 3 buttons: left, right, and down.
		boolean left 	= pressedNow[LEFT_BUTTON]
		             	             || pressedNow[SLIDE_LEFT_BUTTON]
		             	             || leanedNow[LEFT_BUTTON]
		             	             || leanedNow[SLIDE_LEFT_BUTTON] ;
		boolean right 	= pressedNow[RIGHT_BUTTON]
		              	             || pressedNow[SLIDE_RIGHT_BUTTON]
		              	             || leanedNow[RIGHT_BUTTON]
		     		             	 || leanedNow[SLIDE_RIGHT_BUTTON] ;
		boolean down 	= pressedNow[DOWN_BUTTON]
		             	             || leanedNow[DOWN_BUTTON]
		             	             || leanedNow[SLIDE_DOWN_BUTTON] ;
		
		if ( down ) {
			// could be diagonal or just plain down.
			if ( left && !right )
				return ActionAdapter.MOVE_DIRECTION_DOWN_LEFT ;
			else if ( right && !left )
				return ActionAdapter.MOVE_DIRECTION_DOWN_RIGHT ;
			else
				return ActionAdapter.MOVE_DIRECTION_DOWN ;
		}
		
		// Otherwise, is left, right or none.
		if ( left && !right )
			return ActionAdapter.MOVE_DIRECTION_LEFT ;
		else if ( right && !left )
			return ActionAdapter.MOVE_DIRECTION_RIGHT ;
		else
			return ActionAdapter.MOVE_DIRECTION_NONE ;
	}
	

	
	public synchronized void userDidReleaseButton( int button_int ) {
		pressedNow[button_int] = false ;
		
		Delegate d = delegate.get() ;
		
		if ( button_int == OPPONENT_BUTTON && d != null )
			d.ctaad_opponentButtonReleased() ;
		else if ( button_int == SCORE_BUTTON && d != null )
			d.ctaad_scoreButtonReleased() ;
		else if ( button_int == LEFT_BUTTON )
			cancelSlide( LEFT_BUTTON ) ;
		else if ( button_int == RIGHT_BUTTON )
			cancelSlide( RIGHT_BUTTON ) ;
		else if ( button_int == SLIDE_LEFT_BUTTON && actionAdapter != null )
			actionAdapter.controls_slide( ActionAdapter.MOVE_DIRECTION_LEFT, false ) ;
		else if ( button_int == SLIDE_RIGHT_BUTTON && actionAdapter != null )
			actionAdapter.controls_slide( ActionAdapter.MOVE_DIRECTION_RIGHT, false ) ;
		else if ( button_int == DOWN_BUTTON && actionAdapter != null )
			cancelFastFall(mBehaviorDownButtonAutoLock) ;
		else if ( button_int == SLIDE_DOWN_BUTTON && actionAdapter != null )
			cancelFastFall(mBehaviorSlideDownButtonAutoLock) ;
	}
	
	

	
	public synchronized void userDidTapButton( int button_int ) {
		if ( !buttonsEnabled || actionAdapter == null )
			return ;
		
		int action_lean = getActionAdapterLeanMovementDirection() ;
		
		Delegate d = delegate.get() ;
		
		// no "pressed now" for taps.
		
		// Queue up the action in the ActionAdapter.
		if ( button_int == LEFT_BUTTON ) {
			cancelSlide( RIGHT_BUTTON ) ;
			actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_LEFT ) ;
			// no autoSlide
		}
			
		else if ( button_int == RIGHT_BUTTON ) {
			cancelSlide( LEFT_BUTTON ) ;
			actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_RIGHT ) ;
			// no autoSlide
		}
		else if ( button_int == CW_BUTTON ) {
			if ( pressedNow[CCW_BUTTON] ) {
				if ( mBehavior180Windup )
					actionAdapter.controls_turnCW180( action_lean ) ;
				else 
					actionAdapter.controls_turnCCW180( action_lean ) ;
			} else
				actionAdapter.controls_turnCW( action_lean ) ;
		}
		else if ( button_int == CCW_BUTTON ) {
			if ( pressedNow[CW_BUTTON] ) {
				if ( mBehavior180Windup )
					actionAdapter.controls_turnCCW180( action_lean ) ;
				else 
					actionAdapter.controls_turnCW180( action_lean ) ;
			} else
				actionAdapter.controls_turnCCW( action_lean ) ;
		}
		else if ( button_int == FLIP_BUTTON ) {
			actionAdapter.controls_flip( action_lean ) ;
		}
		else if ( button_int == DOWN_BUTTON ) {
			if ( mBehaviorDownButton == BEHAVIOR_DOWN_DROP ) {
				actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_DOWN ) ;
				if ( mBehaviorDownButtonAutoLock )
					actionAdapter.controls_autolock() ;
			} else if ( mBehaviorDownButton == BEHAVIOR_DOWN_FALL ) {
				fall(mBehaviorDownButtonAutoLock) ;		// handles autolock
			} else if ( mBehaviorDownButton == BEHAVIOR_DOWN_AUTOFALL ) {
				fall(mBehaviorDownButtonAutoLock) ;
			}
		}
		else if ( button_int == RESERVE_BUTTON )
			actionAdapter.controls_useReserve( action_lean ) ;
		else if ( button_int == OPPONENT_BUTTON && d != null ) {
			d.ctaad_opponentButtonPressed() ;
			d.ctaad_opponentButtonReleased() ;
		}
		else if ( button_int == SCORE_BUTTON && d != null ) {
			d.ctaad_scoreButtonPressed() ;
			d.ctaad_scoreButtonReleased() ;
		}
		
		else if ( button_int == SLIDE_LEFT_BUTTON ) {
			cancelSlide( RIGHT_BUTTON ) ;
			if ( mBehaviorQuickSlideSupported ) {
				actionAdapter.controls_slideOnce(ActionAdapter.MOVE_DIRECTION_LEFT) ;
			}
		}
			
		else if ( button_int == SLIDE_RIGHT_BUTTON ) {
			cancelSlide( LEFT_BUTTON ) ;
			if ( mBehaviorQuickSlideSupported ) {
				actionAdapter.controls_slideOnce(ActionAdapter.MOVE_DIRECTION_RIGHT) ;
			}
		}
		
		else if ( button_int == SLIDE_DOWN_BUTTON )  {
			if ( mBehaviorSlideDownButton == BEHAVIOR_DOWN_DROP ) {
				actionAdapter.controls_move( ActionAdapter.MOVE_DIRECTION_DOWN ) ;
				if ( mBehaviorSlideDownButtonAutoLock )
					actionAdapter.controls_autolock() ;
			} else if ( mBehaviorSlideDownButton == BEHAVIOR_DOWN_FALL ) {
				fall(mBehaviorSlideDownButtonAutoLock) ;
			} else if ( mBehaviorSlideDownButton == BEHAVIOR_DOWN_AUTOFALL ) {
				fall(mBehaviorSlideDownButtonAutoLock) ;
			}
		}
	}
	
	public synchronized void setAllButtonsEnabled( boolean enabled ) {
		buttonsEnabled = enabled ;
	}
	
	public synchronized void releaseAllButtons() {
		for ( int i = 0; i < NUM_BUTTONS; i++ )
			userDidReleaseButton(i) ;
	}
	
	private int buttonNameToInteger( String buttonName ) {
		for ( int i = 0; i < NUM_BUTTONS ; i++ )
			if ( mButtonNames[i].equals(buttonName) )
				return i ;
		
		return -1 ;
	}
	
	private void setButtonNames(Resources r) {
		mButtonNames[LEFT_BUTTON] = r.getString(R.string.controls_left_button_name) ;
		mButtonNames[RIGHT_BUTTON] = r.getString(R.string.controls_right_button_name) ;
		mButtonNames[CW_BUTTON] = r.getString(R.string.controls_cw_button_name) ;
		mButtonNames[CCW_BUTTON] = r.getString(R.string.controls_ccw_button_name) ;
		mButtonNames[FLIP_BUTTON] = r.getString(R.string.controls_flip_button_name) ;
		mButtonNames[DOWN_BUTTON] = r.getString(R.string.controls_down_button_name) ;
		mButtonNames[SCORE_BUTTON] = r.getString(R.string.controls_score_button_name) ;
		mButtonNames[RESERVE_BUTTON] = r.getString(R.string.controls_reserve_button_name) ;
		mButtonNames[OPPONENT_BUTTON] = r.getString(R.string.controls_opponent_button_name) ;
		
		mButtonNames[UP_BUTTON] = r.getString(R.string.controls_up_button_name) ;
		mButtonNames[SLIDE_LEFT_BUTTON] = r.getString(R.string.controls_slide_left_button_name) ;
		mButtonNames[SLIDE_RIGHT_BUTTON] = r.getString(R.string.controls_slide_right_button_name) ;
		mButtonNames[SLIDE_DOWN_BUTTON] = r.getString(R.string.controls_slide_down_button_name) ;
	}

}
