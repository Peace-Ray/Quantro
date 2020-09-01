package com.peaceray.quantro.view.controls;

import com.peaceray.quantro.R;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class InvisibleControlsGamepadOfRows extends InvisibleControlsGamepad
		implements InvisibleButtonInterface.Delegate {
	
	private static final int NO_CHANGE = Integer.MIN_VALUE ;
	
	// Gamepad Customization
	public static final int BUTTON_CCW = 0 ;
	public static final int BUTTON_CW = 1 ;
	public static final int BUTTON_FLIP = 2 ;
	public static final int BUTTON_LEFT = 3 ;
	public static final int BUTTON_RIGHT = 4 ;
	public static final int BUTTON_DOWN = 5 ;
	public static final int EXTRA_NEXT = 6 ;
	public static final int EXTRA_RESERVE = 7 ;
	public static final int EXTRA_VS = 8 ;
	public static final int EXTRA_SCORE = 9 ;
	public static final int NUM_BUTTONS = 10 ;
	public static final int NUM_CONTROL_BUTTONS = 6 ;
	
	public static final int ROW_TOP = 0 ;
	public static final int ROW_BOTTOM = 1 ;
	public static final int ROW_EXTRAS_TOP = 2 ;
	public static final int ROW_EXTRAS_BOTTOM = 3 ;
	public static final int NUM_ROWS = 4 ;
	public static final int NUM_CONTROL_ROWS = 2 ;
		// We use the fact that the first NUM_CONTROL_ROWS are the 
		// control rows, and from NUM_CONTROL_ROWS to NUM_ROWS are
		// the other rows.  Don't change this ordering.
	
	
	
	private int [] mButtonId ;
	private int [] mRowId ;
	
	private View [] mButton ;
	private View [] mRow ;
	
	private float [] mRowInitialWeight ;
	
	private boolean mHasFlipButton ;
	private boolean mHasTurnButtons ;
	
	private boolean mTurnAboveMove ;
	
	private boolean mNextThenReserve ;
	
	private float mCenterButtonScale ;
	private boolean mCenterButtonPanelToPanel ;
	
	private float mControlButtonsVerticalScale ;
	
	// Inflate from XML
	public InvisibleControlsGamepadOfRows( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		init(context) ;
	}

	public InvisibleControlsGamepadOfRows( Context context, AttributeSet attrs, int defStyle ) {
		super(context, attrs, defStyle) ;
		init(context) ;
	}
	
	private void init( Context context ) {
		mButtonId = new int[NUM_BUTTONS] ;
		mButtonId[BUTTON_CCW] = R.id.controls_button_ccw ;
		mButtonId[BUTTON_CW] = R.id.controls_button_cw ;
		mButtonId[BUTTON_FLIP] = R.id.controls_button_flip ;
		mButtonId[BUTTON_LEFT] = R.id.controls_button_left ;
		mButtonId[BUTTON_RIGHT] = R.id.controls_button_right ;
		mButtonId[BUTTON_DOWN] = R.id.controls_button_down ;
		mButtonId[EXTRA_NEXT] = R.id.controls_extras_next ;
		mButtonId[EXTRA_RESERVE] = R.id.controls_extras_reserve ;
		mButtonId[EXTRA_VS] = R.id.controls_extras_vs ;
		mButtonId[EXTRA_SCORE] = R.id.controls_extras_score ;
		
		
		mRowId = new int[NUM_ROWS] ;
		mRowId[ROW_TOP] = R.id.controls_row_top ;
		mRowId[ROW_BOTTOM] = R.id.controls_row_bottom ;
		mRowId[ROW_EXTRAS_TOP] = R.id.controls_extras_row_top ;
		mRowId[ROW_EXTRAS_BOTTOM] = R.id.controls_extras_row_bottom ;
		
		
		mHasFlipButton = false ;
		mHasTurnButtons = true ;
		mTurnAboveMove = true ;
		mNextThenReserve = true ;
		mCenterButtonScale = 1.0f ;
		mCenterButtonPanelToPanel = false ;
		mControlButtonsVerticalScale = 1.0f ;
		
		mRow = new View[NUM_ROWS] ;
		mButton = new View[NUM_BUTTONS] ;
		for ( int i = 0; i < NUM_ROWS; i++ )
			mRow[i] = null ;
		for ( int i = 0; i < NUM_BUTTONS; i++ )
			mButton[i] = null ;
		
		mRowInitialWeight = new float[NUM_ROWS] ;
	}
	
	/**
	 * Prepares the (loaded) controls by setting delegates for all included
	 * invisible buttons.
	 */
	synchronized public void prepare() {
		super.prepare() ;
	
		collectRows() ;
		collectButtons() ;
		setButtonVisibility() ;
		positionButtonsInRows() ;
		setButtonWidthByWeight() ;
	}
	
	
	
	private void collectRows() {
		for ( int i = 0; i < NUM_ROWS; i++ ) {
			int rowId = mRowId[i] ;
			mRow[i] = findViewById(rowId) ;
			mRowInitialWeight[i] = mRow[i] == null ? 0
					: ((LinearLayout.LayoutParams)mRow[i].getLayoutParams()).weight ;
		}
		
		float total = 0 ;
		for ( int i = 0; i < NUM_ROWS; i++ )
			total += mRowInitialWeight[i] ;
		if ( total > 0 )
			for ( int i = 0; i < NUM_ROWS; i++ )
				mRowInitialWeight[i] /= total ;
		
		for ( int i = 0; i < NUM_ROWS; i++ ) {
			if ( mRow[i] != null )
				setLayoutParams( mRow[i], NO_CHANGE, NO_CHANGE, mRowInitialWeight[i] ) ;
		}
	}
	
	private void collectButtons() {
		for ( int i = 0; i < NUM_BUTTONS; i++ ) {
			int buttonId = mButtonId[i] ;
			mButton[i] = findViewById(buttonId) ;
		}
	}
	
	private void setButtonVisibility() {
		mButton[BUTTON_FLIP].setVisibility( mHasFlipButton ? View.VISIBLE : View.GONE ) ;
		mButton[BUTTON_CCW].setVisibility( mHasTurnButtons ? View.VISIBLE : View.GONE ) ;
		mButton[BUTTON_CW].setVisibility( mHasTurnButtons ? View.VISIBLE : View.GONE ) ;
	}
	
	private void positionButtonsInRows() {
		positionMoveTurnButtonsInRows() ;
		positionExtraButtonsInRows() ;
	}
	
	private void positionMoveTurnButtonsInRows() {
		// first: remove turn and move buttons from their
		// parents, wherever they may be.
		for ( int i = 0; i < NUM_CONTROL_ROWS; i++ ) {
			LinearLayout row = (LinearLayout)mRow[i] ;
			if ( row != null ) {
				row.removeView(mButton[BUTTON_LEFT]) ;
				row.removeView(mButton[BUTTON_RIGHT]) ;
				row.removeView(mButton[BUTTON_CCW]) ;
				row.removeView(mButton[BUTTON_CW]) ;
			}
		}
		
		// second: place in the proper rows.
		int turnRowIndex = mTurnAboveMove ? ROW_TOP : ROW_BOTTOM ;
		int moveRowIndex = mTurnAboveMove ? ROW_BOTTOM : ROW_TOP ;
		
		LinearLayout turnRow = (LinearLayout)mRow[turnRowIndex] ;
		LinearLayout moveRow = (LinearLayout)mRow[moveRowIndex] ;
		
		turnRow.addView(mButton[BUTTON_CCW], 0) ;
		turnRow.addView(mButton[BUTTON_CW], turnRow.getChildCount()) ;
		
		moveRow.addView(mButton[BUTTON_LEFT], 0) ;
		moveRow.addView(mButton[BUTTON_RIGHT], moveRow.getChildCount()) ;
	}
	
	private void positionExtraButtonsInRows() {
		// first: remove turn and move buttons from their
		// parents, wherever they may be.
		for ( int i = NUM_CONTROL_ROWS; i < NUM_ROWS; i++ ) {
			LinearLayout row = (LinearLayout)mRow[i] ;
			if ( row != null ) {
				row.removeView(mButton[EXTRA_NEXT]) ;
				row.removeView(mButton[EXTRA_RESERVE]) ;
				row.removeView(mButton[EXTRA_VS]) ;
				row.removeView(mButton[EXTRA_SCORE]) ;
			}
		}
		
		// second: place in the proper rows.  We place 'next' and
		// 'reserve' in the top row, 'vs' and 'score' in the bottom.
		LinearLayout topRow = (LinearLayout)mRow[ROW_EXTRAS_TOP] ;
		LinearLayout botRow = (LinearLayout)mRow[ROW_EXTRAS_BOTTOM] ;
		topRow.addView(mButton[EXTRA_NEXT], this.mNextThenReserve ? 0 : topRow.getChildCount()) ;
		topRow.addView(mButton[EXTRA_RESERVE], this.mNextThenReserve ? topRow.getChildCount() : 0) ;
		botRow.addView(mButton[EXTRA_VS], this.mNextThenReserve ? 0 : botRow.getChildCount()) ;
		botRow.addView(mButton[EXTRA_SCORE], this.mNextThenReserve ? botRow.getChildCount() : 0) ;
	}
	
	
	
	
	private void setButtonWidthByWeight() {
		// The easiest way to scale the button is to use a total
		// row weight of 1.  The center button gets a weight of 1/3 * factor,
		// and the remaining 1 - (centerWeight) is divided evenly between
		// the two side buttons.
		float centerWeight = (float)((1.0 / 3.0) * mCenterButtonScale) ;
		float sideWeight = (1.0f - centerWeight) / 2 ;
		float sideWeightNoCenter = 0.5f ;
		
		// set the widths and weights.
		float moveWeight = ( mTurnAboveMove || mHasFlipButton ) ? sideWeight : sideWeightNoCenter ;
		float turnWeight = ( !mTurnAboveMove || mHasFlipButton ) ? sideWeight : sideWeightNoCenter ;
		setLayoutParams( mButton[BUTTON_CCW],   0, NO_CHANGE, turnWeight ) ;
		setLayoutParams( mButton[BUTTON_CW],    0, NO_CHANGE, turnWeight ) ;
		setLayoutParams( mButton[BUTTON_FLIP], 	0, NO_CHANGE, centerWeight ) ;
		setLayoutParams( mButton[BUTTON_LEFT],  0, NO_CHANGE, moveWeight ) ;
		setLayoutParams( mButton[BUTTON_RIGHT], 0, NO_CHANGE, moveWeight ) ;
		setLayoutParams( mButton[BUTTON_DOWN],  0, NO_CHANGE, centerWeight ) ;
		
		for ( int i = 0; i < NUM_CONTROL_ROWS; i++ )
			mRow[i].requestLayout() ;
	}
	
	
	private void setRowHeightByWeight() {
		// The easiest way to scale the button is to use a total
		// weight of 1.  We apply the scale factor to all control rows,
		// and divide the remaining weight among all non-control rows
		// (proportionally to their previous weights).
		float weightTotalControls = 0.0f ;
		float weightTotalControlsInitial = 0.0f ;
		
		// Set the layout params for all control rows
		for ( int i = 0; i < NUM_CONTROL_ROWS; i++ ) {
			float rowWeight = mRowInitialWeight[i] * mControlButtonsVerticalScale ;
			setLayoutParams( mRow[i], NO_CHANGE, 0, rowWeight ) ;
			weightTotalControls += rowWeight ;
			weightTotalControlsInitial += mRowInitialWeight[i] ;
		}
		
		float weightRemaining = 1.0f - weightTotalControls ;
		float weightRemainingInitial = 1.0f - weightTotalControlsInitial ;
		
		// Proportionally divide the remaining weight among the other rows
		for ( int i = NUM_CONTROL_ROWS; i < NUM_ROWS; i++ ) {
			float weightProportionInitial = mRowInitialWeight[i] / weightRemainingInitial ;
			float weight = weightProportionInitial * weightRemaining ;
			setLayoutParams( mRow[i], NO_CHANGE, 0, weight ) ;
		}
		
		for ( int i = 0; i < NUM_CONTROL_ROWS; i++ )
			mRow[i].requestLayout() ;
	}
	
	
	@Override
	public boolean setButtonVisibleBoundsByName( String name, Rect r ) {
		// useful to shrink certain buttons to fit the margins.
		if ( !this.mCenterButtonPanelToPanel )
			return false ;
		
		InvisibleButtonInterface ibi = this.mButtonsByName.get(name) ;
		if ( ibi != null && ibi.isTouchable() ) {
			View v = (View)ibi ;
			// we only apply the resize operation if the provided button
			// is both 1: within a side panel (width > 0) and 2: inline
			// with a center button ("down").
			boolean moveButton = v == mButton[BUTTON_LEFT] || v == mButton[BUTTON_RIGHT] ;
			boolean turnButton = v == mButton[BUTTON_CCW] || v == mButton[BUTTON_CW] ;
			
			boolean hasCenterButton = 
				( moveButton && ( this.mTurnAboveMove || this.mHasFlipButton ) )
				|| ( turnButton && ( !this.mTurnAboveMove || this.mHasFlipButton ) ) ;
			
			if ( r.width() > 0 && hasCenterButton ) {
				setLayoutParams( v, r.width(), NO_CHANGE, 0 ) ;
				v.forceLayout() ;
				return true ;
			}
		}
		
		return false ;
	}
	
	
	private void setLayoutParams( View v, int width, int height, float weight ) {
		ViewGroup.LayoutParams lp = v.getLayoutParams() ;
		
		if ( width != NO_CHANGE )
			lp.width = width ;
		if ( height != NO_CHANGE )
			lp.height = height ;
		if ( weight != NO_CHANGE && lp instanceof LinearLayout.LayoutParams )
			((LinearLayout.LayoutParams)lp).weight = weight ;
			
		v.setLayoutParams(lp) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// GAMEPAD OF ROWS SPECIFIC CUSTOMIZATION
	
	public void customize_setHasFlipButton( boolean hasFlip ) {
		if ( mHasFlipButton != hasFlip ) {
			mHasFlipButton = hasFlip ;
			setButtonVisibility() ;
			setButtonWidthByWeight() ;
		}
	}
	
	public void customize_setHasTurnButtons( boolean hasTurns ) {
		if ( mHasTurnButtons != hasTurns ) {
			mHasTurnButtons = hasTurns ;
			setButtonVisibility() ;
			setButtonWidthByWeight() ;
		}
	}
	
	public void customize_setHasButtons( boolean hasTurns, boolean hasFlip ) {
		if ( mHasTurnButtons != hasTurns || mHasFlipButton != hasFlip ) {
			mHasTurnButtons = hasTurns ;
			mHasFlipButton = hasFlip ;
			setButtonVisibility() ;
			setButtonWidthByWeight() ;
		}
	}
	
	public void customize_setTurnButtonsAboveMoveButtons( boolean turnAboveMove ) {
		if ( mTurnAboveMove != turnAboveMove ) {
			mTurnAboveMove = turnAboveMove ;
			positionMoveTurnButtonsInRows() ;
			setButtonWidthByWeight() ;
		}
	}
	
	
	public void customize_setNextThenReserve( boolean nextThenReserve ) {
		if ( mNextThenReserve != nextThenReserve ) {
			mNextThenReserve = nextThenReserve ;
			positionExtraButtonsInRows() ;
		}
	}
	
	
	/**
	 * Sets the center button width as a floating point factor.  1.0 indicates
	 * "the same size" as side buttons, < 1.0 indicates smaller, > 1.0 indicates
	 * larger.
	 * 
	 * Sizes will be set using button weights, but the factor is not directly
	 * as the weight.  Instead, we determine the relative size for the center
	 * button if weights were evenly distributed, then assign new weights that
	 * increase the center button's actual size by the specified factor.
	 * 
	 * @param factor The scale factor applied to the width of the center button.
	 * 		Must be [0, 3).
	 */
	public void customize_setCenterButtonWidthScale( float factor ) {
		if ( factor < 0 || factor >= 3 )
			throw new IllegalArgumentException("Factor must be [0, 3.0)") ;
		
		if ( mCenterButtonPanelToPanel || mCenterButtonScale != factor ) {
			mCenterButtonScale = factor ;
			mCenterButtonPanelToPanel = false ;
			setButtonWidthByWeight() ;
		}
	}
	
	
	public void customize_setCenterButtonWithinNextVisibleBounds() {
		if ( !mCenterButtonPanelToPanel ) {
			mCenterButtonPanelToPanel = true ;
			mCenterButtonScale = 1.0f ;
			setButtonWidthByWeight() ;
		}
	}
	
	
	/**
	 * Sets the controls (L, R, D, CCW, CW, etc.) height as a floating point
	 * factor.  1.0 indicates "the same size" as side buttons, < 1.0 indicates
	 * shorter, > 1.0 indicates larger.
	 * 
	 * Sizes will be set using row weights, but the factor is not directly used
	 * as the weight.  Instead, we determine the relative height for the center
	 * button if weights were evenly distributed, then assign new weights that
	 * increase the center button's actual size by the specified factor.
	 * 
	 * @param factor
	 */
	public void customize_setControlButtonsHeightScale( float factor ) {
		float totalWeight = 0 ;
		for ( int i = 0; i < NUM_CONTROL_ROWS; i++ )
			totalWeight += factor * mRowInitialWeight[i] ;
		
		if ( totalWeight <= 0 || totalWeight >= 1 )
			throw new IllegalArgumentException("Factor is too large or too small.") ;
		
		if ( mControlButtonsVerticalScale != factor ) {
			mControlButtonsVerticalScale = factor ;
			setRowHeightByWeight() ;
		}
	}
	
}
