package com.peaceray.quantro.view.controls;

import com.peaceray.quantro.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class InvisibleControlsGestureOfRows extends InvisibleControlsGesture {
	
	// Button Customization
	public static final int BUTTON_CCW = 0 ;
	public static final int BUTTON_CW = 1 ;
	public static final int EXTRA_NEXT = 2 ;
	public static final int EXTRA_RESERVE = 3 ;
	public static final int EXTRA_VS = 4 ;
	public static final int EXTRA_SCORE = 5 ;
	public static final int NUM_BUTTONS = 6 ;
	public static final int NUM_CONTROL_BUTTONS = 2 ;
	
	public static final int ROW_TOP = 0 ;
	public static final int ROW_EXTRAS_TOP = 1 ;
	public static final int ROW_EXTRAS_BOTTOM = 2 ;
	public static final int NUM_ROWS = 3 ;
	public static final int NUM_CONTROL_ROWS = 1 ;
		// We use the fact that the first NUM_CONTROL_ROWS are the 
		// control rows, and from NUM_CONTROL_ROWS to NUM_ROWS are
		// the other rows.  Don't change this ordering.
	
	
	private int [] mButtonId ;
	private int [] mRowId ;
	
	private View [] mButton ;
	private View [] mRow ;
	
	private boolean mHasFlipGesture ;
	private boolean mHasTurnGestures ;
	private boolean mNextThenReserve ;

	private boolean [] mHasGesture ;

	// Inflate from XML
	public InvisibleControlsGestureOfRows( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		init(context) ;
	}

	public InvisibleControlsGestureOfRows( Context context, AttributeSet attrs, int defStyle ) {
		super(context, attrs, defStyle) ;
		init(context) ;
	}
	
	
	private void init( Context context ) {
		mButtonId = new int[NUM_BUTTONS] ;
		mButtonId[BUTTON_CCW] = R.id.controls_button_ccw ;
		mButtonId[BUTTON_CW] = R.id.controls_button_cw ;
		mButtonId[EXTRA_NEXT] = R.id.controls_extras_next ;
		mButtonId[EXTRA_RESERVE] = R.id.controls_extras_reserve ;
		mButtonId[EXTRA_VS] = R.id.controls_extras_vs ;
		mButtonId[EXTRA_SCORE] = R.id.controls_extras_score ;
		
		
		mRowId = new int[NUM_ROWS] ;
		mRowId[ROW_TOP] = R.id.controls_row_top ;
		mRowId[ROW_EXTRAS_TOP] = R.id.controls_extras_row_top ;
		mRowId[ROW_EXTRAS_BOTTOM] = R.id.controls_extras_row_bottom ;
		
		mHasFlipGesture = false ;
		mHasTurnGestures = true ;
		mNextThenReserve = true ;
		
		mHasGesture = new boolean[NUM_GESTURES] ;
		for ( int i = 0; i < NUM_GESTURES; i++ )
			mHasGesture[i] = true ;
		
		mRow = new View[NUM_ROWS] ;
		mButton = new View[NUM_BUTTONS] ;
		for ( int i = 0; i < NUM_ROWS; i++ )
			mRow[i] = null ;
		for ( int i = 0; i < NUM_BUTTONS; i++ )
			mButton[i] = null ;
	}
	
	@Override
	protected void doGesture( int gesture ) {
		if ( gesture < 0 )
			return ;
		if ( !mHasGesture[gesture] )
			return ;
		super.doGesture(gesture) ;
	}
	
	/**
	 * Prepares the (loaded) controls by setting delegates for all included
	 * invisible buttons.
	 */
	synchronized public void prepare() {
		super.prepare() ;
	
		collectRows() ;
		collectButtons() ;
		setHasGestures() ;
		positionButtonsInRows() ;
	}
	
	
	
	private void collectRows() {
		for ( int i = 0; i < NUM_ROWS; i++ ) {
			int rowId = mRowId[i] ;
			mRow[i] = findViewById(rowId) ;
		}
	}
	
	private void collectButtons() {
		for ( int i = 0; i < NUM_BUTTONS; i++ ) {
			int buttonId = mButtonId[i] ;
			mButton[i] = findViewById(buttonId) ;
		}
	}
	
	private void setHasGestures() {
		String flipName = this.getResources().getString(R.string.controls_flip_button_name) ;
		String turnCCWName = this.getResources().getString(R.string.controls_ccw_button_name) ;
		String turnCWName = this.getResources().getString(R.string.controls_cw_button_name) ;
		for ( int i = 0; i < NUM_GESTURES; i++ ) {
			mHasGesture[i] = true ;
			String name = mButtonNamesByGesture.get(i) ;
			if ( flipName.equals( name ) ) {
				mHasGesture[i] = mHasFlipGesture ;
			}
			if ( turnCCWName.equals(name) || turnCWName.equals(name) ) {
				 mHasGesture[i] = mHasTurnGestures ;
			}
		}
	}
	
	private void positionButtonsInRows() {
		positionExtraButtonsInRows() ;
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
	
	
	////////////////////////////////////////////////////////////////////////////
	// GESTURE OF ROWS SPECIFIC CUSTOMIZATION
	
	public void customize_setHasFlipGesture( boolean hasFlip ) {
		if ( mHasFlipGesture != hasFlip ) {
			mHasFlipGesture = hasFlip ;
			setHasGestures() ;
		}
	}
	
	public void customize_setHasTurnGestures( boolean hasTurns ) {
		if ( mHasTurnGestures != hasTurns ) {
			mHasTurnGestures = hasTurns ;
			setHasGestures() ;
		}
	}
	
	public void customize_setHasGestures( boolean hasTurns, boolean hasFlip ) {
		if ( mHasTurnGestures != hasTurns || mHasFlipGesture != hasFlip ) {
			mHasTurnGestures = hasTurns ;
			mHasFlipGesture = hasFlip ;
			setHasGestures() ;
		}
	}
	
	public void customize_setNextThenReserve( boolean nextThenReserve ) {
		if ( mNextThenReserve != nextThenReserve ) {
			mNextThenReserve = nextThenReserve ;
			positionExtraButtonsInRows() ;
		}
	}
	
}
