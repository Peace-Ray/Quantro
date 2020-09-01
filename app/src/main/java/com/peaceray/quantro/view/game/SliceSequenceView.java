package com.peaceray.quantro.view.game;

import com.peaceray.quantro.model.game.GameBlocksSlice;
import com.peaceray.quantro.model.game.GameBlocksSliceSequence;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

public class SliceSequenceView extends BlockFieldView {

	private static final String TAG = "SliceSequenceView" ;

	
	public SliceSequenceView(Context context) {
		super(context);
		
		// Set basic defaults
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public SliceSequenceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public SliceSequenceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	GameBlocksSliceSequence mSequence ;
	long mUpdateEvery ;
	
	// handler!
	Handler mHandler ;
	Runnable mUpdateRunnable ;
	
	// data from the last draw?
	boolean mStillAnimatingFrame ;
	
	private void constructor_setDefaultValues( Context context ) {
		mSequence = null ;
		mUpdateEvery = 20 ;
		
		mHandler = null ;
	}
	
	
	private void constructor_allocateAndInitialize( Context context ) {
		mHandler = new Handler() ;
		
		mUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				// advance to the next slice in the sequence!
				if ( mSlice != null && !mStillAnimatingSlice ) {
					// see if we can get another slice!
					if ( mSequence != null && mSequence.nextIsReady( System.currentTimeMillis() - mTimeSliceSet ) ) {
						mSequence.next(mSlice) ;
						setContentReference(mSlice) ;
					}
					if ( mQCWB != null ) {
						mQCWB.invalidateContent(SliceSequenceView.this) ;
					}
					postInvalidate() ;
					
				} else if ( mSlice != null ){
					// still animating.  Redraw!
					if ( mQCWB != null ) {
						mQCWB.invalidateContent(SliceSequenceView.this) ;
					}
					postInvalidate() ;
				}
			}
		} ;
	}
	
	
	public void setSequence( DrawSettings settings, GameBlocksSliceSequence sequence ) {
		mSequence = sequence ;
		// set content!
		if ( mSequence.hasNext() ) {
			GameBlocksSlice slice = sequence.newNext() ;
			setContentReference( settings, slice ) ;
		}
	}
	
	
	/**
	 * This method is called in onDraw when a slice is drawn
	 */
	@Override
	protected void sliceDrawn() {
		mHandler.postDelayed(mUpdateRunnable, mUpdateEvery) ;
	}
	
	@Override
	protected void sliceDrawnAnimationFinished() {
		mHandler.postDelayed(mUpdateRunnable, mUpdateEvery) ;
	}
	
}
