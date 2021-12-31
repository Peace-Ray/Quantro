package com.peaceray.quantro.view.options;

import com.peaceray.quantro.R;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class OptionsControlsStripView extends OptionsView implements
		OptionsControlsView, com.peaceray.quantro.view.button.strip.CustomButtonStrip.Delegate {
	
	private static final String TAG = "OCStripView" ;
	
	protected static int enm ;
	
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
	
	
	protected String [] mTextControls ;			// indexed by has controls
	protected String [] mTextControlsExpanded ;	// indexed by INDEX_CONTROLS_EXPANDED.
	protected String [][][] mTextControlsExerpt ;	// indexed by INDEX_CONTROLS_AVAILABLE, hasTurns, hasFlips
	
	protected String mTextControlsFull ;
	protected String mTextControlsBasePlaceholder ;
	protected String mTextControlsExpansionPlaceholder ;
	protected String mTextControlsExpandedExerptPlaceholder ;
	
	protected String ACTION_NAME_GAMEPAD ;
	protected String ACTION_NAME_GESTURE ;
	protected String ACTION_NAME_SETTINGS ;
	
	protected Delegate mDelegate ;
	
	protected View mContentView ;
	protected CustomButtonStrip mActionBar ;
	protected ImageView mControlsThumbnailView ;
	protected TextView mControlsDescriptionTextView ;
	protected TextView mControlsDescriptionExpansionTextView ;
	protected TextView mControlsDescriptionFullTextView ;
	
	
	// current settings
	protected Drawable mThumbnail ;
	protected boolean mControlsGamepad ;
	protected boolean mControlsGamepadQuickSlide ;
	protected boolean mControlsGamepadDownFall ;
	protected boolean mControlsGamepadDoubleTap ;
	protected boolean mControlsGestureQuickSlide ;
	protected boolean mControlsGestureTurnButtons ;
	protected boolean mControlsGestureDragAutolocks ;
	protected boolean mControlsHasTurns ;
	protected boolean mControlsHasFlips ;
	

	@Override
	public void setDelegate(Delegate delegate) {
		mDelegate = delegate ;
	}

	@Override
	public void setControlsThumbnail(Drawable thumbnail) {
		mThumbnail = thumbnail ;
		
		// Set the thumbnail.  Image view is VISIBLE / INVISIBLE
		// depending on null / not null.
		if ( mControlsThumbnailView != null ) {
			if ( mThumbnail == null ) {
				Log.d(TAG, "setting controls thumbnail to (nothing) and visibility to INVISIBLE") ;
				mControlsThumbnailView.setVisibility( View.INVISIBLE ) ;
			} else {
				Log.d(TAG, "setting controls thumbnail to " + mThumbnail) ;
				mControlsThumbnailView.setImageDrawable(mThumbnail) ;
				mControlsThumbnailView.setVisibility( View.VISIBLE ) ;
			}
		}
	}

	@Override
	public void setControlsGamepad(boolean quickslide,
			boolean gamepadDownFall,
			boolean gamepadDoubleTap) {
		
		mControlsGamepad = true ;
		mControlsGamepadQuickSlide = quickslide ;
		mControlsGamepadDownFall = gamepadDownFall ;
		mControlsGamepadDoubleTap = gamepadDoubleTap ;
	}

	@Override
	public void setControlsGesture(boolean quickslide, boolean turnButtons, boolean dragAutolocks) {
		
		mControlsGamepad = false ;
		mControlsGestureQuickSlide = quickslide ;
		mControlsGestureTurnButtons = turnButtons ;
		mControlsGestureDragAutolocks = dragAutolocks ;
	}

	@Override
	public void setControlsHas(boolean hasTurns, boolean hasFlips) {
		mControlsHasTurns = hasTurns ;
		mControlsHasFlips = hasFlips ;
	}
	
	@Override
	public void init( Activity activity, View root ) {
		Resources res = activity.getResources() ;
		
		// load base strings
		mTextControls = new String[NUM_INDEX_HAS_CONTROLS] ;
		mTextControlsExpanded = new String[NUM_INDEX_CONTROLS_EXPANDED] ;
		mTextControlsExerpt = new String[NUM_INDEX_CONTROLS_AVAILABLE_EXERPT][2][2] ;
		
		constructControlsStrings( res ) ;
		
		// button names
		ACTION_NAME_SETTINGS = res.getString(R.string.action_strip_name_settings) ;
		ACTION_NAME_GAMEPAD = res.getString(R.string.action_strip_name_gamepad) ;
		ACTION_NAME_GESTURE = res.getString(R.string.action_strip_name_gesture) ;
		
		mContentView = root ;
		
		// get reference to the action bar
		mActionBar = (CustomButtonStrip)root.findViewById(R.id.game_options_controls_action_strip) ;
		// set enabled for all buttons.
		mActionBar.setDelegate(this) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_SETTINGS), true ) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_GAMEPAD), true ) ;
		mActionBar.setEnabled( mActionBar.getButton(ACTION_NAME_GESTURE), true ) ;
		
		// get reference to content views
		// get reference to thumbnail image view
		mControlsThumbnailView = (ImageView)mContentView.findViewById(R.id.game_options_controls_thumbnail) ;
		
		// get reference to text description views
		mControlsDescriptionTextView = (TextView)mContentView.findViewById(R.id.game_options_controls_description) ;
		mControlsDescriptionExpansionTextView = (TextView)mContentView.findViewById(R.id.game_options_controls_description_expansion) ;
		mControlsDescriptionFullTextView = (TextView)mContentView.findViewById(R.id.game_options_controls_description_full) ;
		
	}

	@Override
	public void refresh() {
		// Set the available buttons.  If gamepad, gesture is visible.
		// If gesture, gamepad is visible.
		mActionBar.setVisible( mActionBar.getButton(ACTION_NAME_GAMEPAD), !mControlsGamepad ) ;
		mActionBar.setVisible( mActionBar.getButton(ACTION_NAME_GESTURE), mControlsGamepad ) ;

		// Set the current content strings describing our settings.
		if ( mControlsDescriptionTextView != null ) {
			mControlsDescriptionTextView.setText(getDescriptionBaseString() ) ;
		}
		
		if ( mControlsDescriptionExpansionTextView != null ) {
			mControlsDescriptionExpansionTextView.setText( getDescriptionExpansionString() ) ;
		}
		
		if ( mControlsDescriptionFullTextView != null ) {
			String base = getDescriptionBaseString() ;
			String expansion = getDescriptionExpansionString() ;
			
			String full = mTextControlsFull
					.replace(mTextControlsBasePlaceholder, base)
					.replace(mTextControlsExpansionPlaceholder, expansion) ;
			
			mControlsDescriptionFullTextView.setText( full ) ;
		}
	}
	
	
	@Override
	protected void relaxCache(boolean isRelaxed) {
		// no effect; we don't cache images
	}

	@Override
	protected void refreshCache(boolean isRelaxed) {
		// no effect; we don't cache images
	}
	
	
	protected String getDescriptionBaseString() {
		int index ;
		if ( mControlsGamepad ) {
			index = mThumbnail == null
					? INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_NO
					: INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_YES ;
		} else {
			index = mThumbnail == null
					? INDEX_HAS_CONTROLS_GESTURE_BITMAP_NO
					: INDEX_HAS_CONTROLS_GESTURE_BITMAP_YES ;
		}
		
		return mTextControls[index] ;
	}
	
	
	protected String getDescriptionExpansionString() {
		int index, sindex ;
		String s ;
		
		if ( mControlsGamepad ) {
			if ( mControlsGamepadDownFall && mControlsGamepadDoubleTap )
				index = INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL_DOUBLE_TAP ;
			else if ( mControlsGamepadDownFall && !mControlsGamepadDoubleTap )
				index = INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL ;
			else if ( !mControlsGamepadDownFall && mControlsGamepadDoubleTap )
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
				this.mTextControlsExerpt[sindex][mControlsHasTurns ? 1 : 0][mControlsHasFlips ? 1 : 0]) ;
		return s ;
	}
	
	
	protected void constructControlsStrings( Resources res ) {
		mTextControls[INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_NO] = res.getString(R.string.game_options_controls_gamepad_instructions_thumbnail_no) ;
		mTextControls[INDEX_HAS_CONTROLS_GAMEPAD_BITMAP_YES] = res.getString(R.string.game_options_controls_gamepad_instructions_thumbnail_yes) ;
		mTextControls[INDEX_HAS_CONTROLS_GESTURE_BITMAP_NO] = res.getString(R.string.game_options_controls_gesture_instructions_thumbnail_no) ;
		mTextControls[INDEX_HAS_CONTROLS_GESTURE_BITMAP_YES] = res.getString(R.string.game_options_controls_gesture_instructions_thumbnail_yes) ;
	
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL_DOUBLE_TAP] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_fall_double_tap) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_FALL] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_fall) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP_DOUBLE_TAP] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_drop_double_tap) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GAMEPAD_DROP] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_drop) ;
		mTextControlsExpanded[INDEX_CONTROLS_EXPANDED_GESTURE] = res.getString(R.string.game_options_controls_gesture_instructions_expansion) ;

		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][1][0] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_exerpt_has_turn) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][0][1] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_exerpt_has_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][1][1] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_exerpt_has_turn_and_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GAMEPAD][0][0] = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_exerpt_has_move_only) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][1][0] = res.getString(R.string.game_options_controls_gestures_instructions_expansion_excerpt_has_turn) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][0][1] = res.getString(R.string.game_options_controls_gestures_instructions_expansion_excerpt_has_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][1][1] = res.getString(R.string.game_options_controls_gestures_instructions_expansion_excerpt_has_turn_and_flip) ;
		mTextControlsExerpt[INDEX_CONTROLS_AVAILABLE_EXERPT_GESTURE][0][0] = res.getString(R.string.game_options_controls_gestures_instructions_expansion_excerpt_has_move_only) ;
		
		this.mTextControlsFull = res.getString(R.string.game_options_controls_instructions_full) ;
		this.mTextControlsBasePlaceholder = res.getString(R.string.game_options_controls_gamepad_instructions_base_placeholder) ;
		this.mTextControlsExpansionPlaceholder = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_placeholder) ;
		
		this.mTextControlsExpandedExerptPlaceholder = res.getString(R.string.game_options_controls_gamepad_instructions_expansion_exerpt_placeholder) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ACTION BAR METHODS
	//

	@Override
	public boolean customButtonStrip_onButtonClick(CustomButtonStrip strip,
			int buttonNum, String name, boolean asOverflow ) {
		
		boolean performAction = false ;
		
		// play sound
		if ( mSoundPool != null && mSoundControls && !asOverflow )
			mSoundPool.menuButtonClick() ;
		
		if ( mDelegate != null ) {
			if ( ACTION_NAME_GAMEPAD.equals(name) ) {
				performAction = true ;
				mDelegate.ocvd_userSetControlsGamepad(this) ;
			} 
			
			if ( ACTION_NAME_GESTURE.equals(name) ) {
				performAction = true ;
				mDelegate.ocvd_userSetControlsGesture(this) ;
			}
			
			if ( ACTION_NAME_SETTINGS.equals(name) ) {
				performAction = true ;
				mDelegate.ocvd_userAdvancedConfiguration(this) ;
			}
		}
		
		return performAction ;
		
	}

	@Override
	public boolean customButtonStrip_onButtonLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		// no long-clicks
		return false ;
	}

	@Override
	public boolean customButtonStrip_supportsLongClick(CustomButtonStrip strip,
			int buttonNum, String name) {
		// no long-clicks
		return false ;
	}
	
	@Override
	public void customButtonStrip_onPopupOpen(
			CustomButtonStrip strip ) {
		
		if ( mDelegate == null )
			return ;
		
		if ( mSoundPool != null && mSoundControls )
			mSoundPool.menuButtonClick() ;
	}

}
