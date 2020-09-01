package com.peaceray.quantro.view.controls;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Rect;
import com.peaceray.android.view.GestureDetector ; 

import android.util.Log;
import android.view.MotionEvent;

/**
 * A Listener for gestures.  The right way to use this object is to
 * pass every MotionEvent to it directly.
 * 
 * @author Jake
 *
 */
public class InvisibleControlsFourWayGestureListener
		implements GestureDetector.OnGestureListener {
	
	private static final String TAG = "InvisibleControlsFourWayGestureListener" ;
	
	
	// the larger this value, the larger the range of movement which is 
	// locked to cardinal (non-diagonal).  Set it to 1 for completely
	// 4-way movement; set to 0 to allow complete freedom of movement.
	private static final double [] MIN_DIAGONAL_RATIO = new double[] {
		0.41421356237309503,
		0.65,
		0.85,
	} ;
	// this is tan( pi/8 ), the ratio adjacent / opposite for the angle
	// 1/16th of the way around the circle.  This is appropriate for dividing
	// the circle into 8 equal pieces, including horiz. and vertically aligned
	// slices.  If the ratio of small / big is less than this value, we can
	// prevent drift by correcting for the smaller value.
	
	
	public static final int DIRECTION_LEFT = 0 ;
	public static final int DIRECTION_RIGHT = 1 ;
	public static final int DIRECTION_UP = 2 ;
	public static final int DIRECTION_DOWN = 3 ;
	
	
	
	public interface Delegate {
		
		/**
		 * A tap has occurred in this "quadrant."
		 * @param horizontalHalf
		 * @param verticalHalf
		 */
		public void gestureTap( int horizontalHalf, int verticalHalf ) ;
		
		
		/**
		 * A fling has occurred in the specified direction.
		 * @param direction
		 */
		public void gestureFling( int direction ) ;
		
		
		
		/**
		 * We have dragged in this direction.
		 * @param direction
		 */
		public void gestureDrag( int direction ) ;
		
		
		/**
		 * We started dragging... or at least we THINK we did.
		 */
		public void gestureDragStarted() ;

		
		/**
		 * We have finished a drag.  Every instance of gestureDragStarted
		 * will eventually be followed by this method.
		 * 
		 * HOWEVER: order with drags may not be preserved.  You may receive a "gestureDrag"
		 * call AFTER this method.  All we guarantee is that gestureDragStarted
		 * and gestureDragFinished come in pairs.
		 */
		public void gestureDragFinished() ;
	}
	
	private WeakReference<Delegate> mwrDelegate ;
	private GestureDetector mGestureDetector ;
	
	private Rect mBounds ;
	
	private int mGridWidth = 0 ;
	private int mGridHeight = 0 ;
	
	private float mMinFlingVelocity = 0 ;
	private float mMinScrollPixels = 0 ;
	
	private boolean mDragging ;			// does mDraggingPointerID represent an existing drag?
										// In other words, if ( !mDragging ), then the next DOWN event should overwrite mDraggingPointerID.
										// We seperate these (rather than use -1) because an UP event will set mDragging to false
										// but it may immediately be falled by a gesture recognition, so we need to keep track
										// of the pointer ID we were previously tracking.
	private int mDraggingPointerID ;	// the pointer we are currently regarding as the "motion dragging" pointer.
	private float mDraggingOriginX ;		// current (0,0) position for drags
	private float mDraggingOriginY ;		// current (0,0) position for drags

	private int mDraggingLastStepDirection ;	// used to prevent "back-swipes", where a player over-compensates at the end of a drag and swipes in the opposite direction.  Can't swipe in a direction other than movement.
	private int mDraggingStepsInDirection ;
	
	private float mDraggingLastX ;		// used only for drift correction.
	private float mDraggingLastY ;
	
	private double [] mDriftCorrectionMinDiagonalRatio ;	// indexed by number of consecutive steps.
	
	
	public InvisibleControlsFourWayGestureListener( Context context, Delegate delegate ) {
		this( context, delegate, MIN_DIAGONAL_RATIO ) ;
	}
	
	public InvisibleControlsFourWayGestureListener( Context context, Delegate delegate, double [] driftCorrection ) {
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
		
		// defaults
		mBounds = new Rect(0, 0, 0, 0) ;
		
		mGridWidth = 1000 ;		// for safety, to avoid infinite while()s.
		mGridHeight = 1000 ;		// for safety, to avoid infinite while()s.
		
		mMinFlingVelocity = 0 ;		// if our fling has total velocity less than this, we ignore it.
		
		mDragging = false ;
		mDraggingPointerID = -1 ;
		mDraggingOriginX = 0f ;
		mDraggingOriginY = 0f ;
		mDraggingLastStepDirection = -1 ;
		mDraggingStepsInDirection = 0 ;
		
		mDraggingLastX = 0f ;
		mDraggingLastY = 0f ;
		
		// Create a gesture detector.
		mGestureDetector = GestureDetector.newGestureDetector(context, this) ;

		mDriftCorrectionMinDiagonalRatio = new double[ driftCorrection.length ] ;
		for ( int i = 0; i < driftCorrection.length; i++ )
			mDriftCorrectionMinDiagonalRatio[i] = driftCorrection[i] ;
	}
	
	
	
	public void setGestureBounds( Rect bounds ) {
		mBounds.set(bounds) ;
	}
	
	public void setMinScrollPixels( float pixels ) {
		mMinScrollPixels = pixels ;
		
		float min = pixels ;
		if ( mGridWidth > 0 && mGridWidth < min )
			min = mGridWidth ;
		if ( mGridHeight > 0 && mGridHeight < min )
			min = mGridWidth ;
		
		mGestureDetector.setScrollMinPixels(min) ;
	}
	
	public void setMinFlingVelocity( float velocity ) {
		mMinFlingVelocity = velocity ;
		
		mGestureDetector.setFlingMinPixelsPerSecond(velocity) ;
	}
	
	public void setDragGrid( int stepWidth, int stepHeight ) {
		mGridWidth = stepWidth ;
		mGridHeight = stepHeight ;
		
		float min = Math.min(stepWidth, stepHeight) ;
		if ( mMinScrollPixels > 0 && mMinScrollPixels < min )
			min = mMinScrollPixels ;
		
		mGestureDetector.setScrollMinPixels(min) ;
	}
	
	
	synchronized public void onTouchEvent(MotionEvent event) {
		int actionMasked = event.getAction() & MotionEvent.ACTION_MASK ;
		int actionPointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
				// despite the ID_MASK and ID_SHIFT names, this is actually the
				// pointer index (not an ID!)
		
		int pointerID = event.getPointerId(actionPointerIndex) ;

		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		switch( actionMasked ) {
		case MotionEvent.ACTION_CANCEL:
			//Log.d(TAG, "ACTION_CANCEL") ;
			// stop tracking everything.  Stop any "pulls."
			if ( mDragging && delegate != null )
				delegate.gestureDragFinished() ;
			mDragging = false ;
			mDraggingPointerID = -1 ;
			mGestureDetector.onTouchEvent(event) ;
			break ;
			
		case MotionEvent.ACTION_POINTER_DOWN:
		case MotionEvent.ACTION_DOWN:
			//Log.d(TAG, "ACTION_DOWN or ACTION_POINTER_DOWN") ;
			// don't register this as a tap until the GestureRecognizer confirms it.
			// however, if we're not currently tracking a pointer, we should start.
			// We only respond to movement events from this pointer until it is released.
			if ( !mDragging ) {
				mDraggingOriginX = event.getX( actionPointerIndex ) ;
				mDraggingOriginY = event.getY( actionPointerIndex ) ;
				mDraggingLastX = mDraggingOriginX ;
				mDraggingLastY = mDraggingOriginY ;
				mDragging = true ;
				mDraggingPointerID = pointerID ;
				mDraggingLastStepDirection = -1 ;
				mDraggingStepsInDirection = 0 ;
				delegate.gestureDragStarted() ;
			}
			// nothing else to do.
			mGestureDetector.onTouchEvent(event) ;
			break ;
			
		case MotionEvent.ACTION_POINTER_UP:
		case MotionEvent.ACTION_UP:
			//Log.d(TAG, "ACTION_UP or ACTION_POINTER_UP") ;
			// the detector needs to know about this.
			// After that, configure "dragging."
			mGestureDetector.onTouchEvent(event) ;
			if ( mDragging && mDraggingPointerID == pointerID ) {
				//Log.d(TAG, "matches mDraggingPointerID") ;
				mDragging = false ;
				delegate.gestureDragFinished() ;
			}
			break ;
			
		case MotionEvent.ACTION_MOVE:
			//Log.d(TAG, "ACTION_MOVE") ;
			// Perform the move.
			mGestureDetector.onTouchEvent(event) ;
			break ;
		}
		
	}
	

	@Override
	public boolean onDown(MotionEvent e) {
		// we have already handled the "down" event.
		return true ;
	}

	@Override
	synchronized public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		
		if ( e1 == null || e2 == null )
			return true ;
		
		int actionPointerIndex = (e1.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
				// despite the ID_MASK and ID_SHIFT names, this is actually the
				// pointer index (not an ID!)
		
		int pointerID = e1.getPointerId(actionPointerIndex) ;

		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		// Flings are only considered if we are tracking that pointer.
		if ( pointerID == mDraggingPointerID && mDragging ) {
			
			// HEY GUESS WHAT!?? THIS IS A FLING!
			float velocity = (float) Math.sqrt( ((double)velocityX)*((double)velocityX) + ((double)velocityY)*((double)velocityY) ) ;
			boolean horizontal = Math.abs(velocityY) / Math.abs(velocityX) <= mDriftCorrectionMinDiagonalRatio[0] ;
			boolean vertical = Math.abs(velocityX) / Math.abs(velocityY) <= mDriftCorrectionMinDiagonalRatio[0] ;
			boolean fastEnough = mMinFlingVelocity <= velocity ;
			
			//Log.d(TAG, "onFling min vel. " + mMinFlingVelocity + " this fling " + velocity ) ;

			if ( fastEnough ) {
				if ( horizontal && velocityX < 0 && ( mDraggingLastStepDirection < 0 || mDraggingLastStepDirection == DIRECTION_LEFT ) )
					delegate.gestureFling(DIRECTION_LEFT) ;
				else if ( horizontal && velocityX > 0 && ( mDraggingLastStepDirection < 0 || mDraggingLastStepDirection == DIRECTION_RIGHT ) )
					delegate.gestureFling(DIRECTION_RIGHT) ;
				else if ( vertical && velocityY < 0 && ( mDraggingLastStepDirection < 0 || mDraggingLastStepDirection == DIRECTION_UP ) )
					delegate.gestureFling(DIRECTION_UP) ;
				else if ( vertical && velocityY > 0 && ( mDraggingLastStepDirection < 0 || mDraggingLastStepDirection == DIRECTION_DOWN ) )
					delegate.gestureFling(DIRECTION_DOWN) ;
			}
		}
		
		return true ;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// WE DON'T CARE!
		// In fact, it's probably an error for this to occur.
	}

	@Override
	synchronized public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		
		if ( e1 == null || e2 == null )
			return true ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
			
		int downPointerIndex = (e1.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
				// despite the ID_MASK and ID_SHIFT names, this is actually the
				// pointer index (not an ID!)
		
		int pointerID = e1.getPointerId(downPointerIndex) ;

		int e2PointerIndex = -1 ;
		for ( int i = 0; i < e2.getPointerCount(); i++ )
			if ( e2.getPointerId(i) == pointerID )
				e2PointerIndex = i ;
		
		// start dragging?
		if ( !mDragging ) {
			mDraggingOriginX = e2.getX( e2PointerIndex ) ;
			mDraggingOriginY = e2.getY( e2PointerIndex ) ;
			mDraggingLastX = mDraggingOriginX ;
			mDraggingLastY = mDraggingOriginY ;
			mDragging = true ;
			mDraggingPointerID = pointerID ;
			delegate.gestureDragStarted() ;
		}
		
		// Scrolls are only considered if we are tracking that pointer.
		else if ( pointerID == mDraggingPointerID && mDragging )
			drag( e2, e2PointerIndex ) ;
		
		return true ;
		
	}
	
	
	private void drag( MotionEvent e, int pointerIndex ) {
		if ( e == null )
			return ;
		
		// We drap based on offset from a "grid" centered on our
		// down position.  This is stored in mDraggingOriginX, OriginY.
		//
		// If we move more than mGridX pixels horizontally away from OriginX,
		// or likewise Y/Vertically, we consider this a drag in that direction.
		// We call the appropriate delegate method AND we adjust the origin
		// by mGrid* for the next move.
		//
		
		/*
		// history first.
		int historySize = e.getHistorySize() ;
		for ( int i = 0; i < historySize; i++ ) {
			float x = e.getHistoricalX(pointerIndex, i) ;
			float y = e.getHistoricalY(pointerIndex, i) ;
			
			dragTo(x, y) ;
		}
		*/
		
		// current pos
		float x = e.getX(pointerIndex) ;
		float y = e.getY(pointerIndex) ;
		
		dragTo(x, y) ;
	}
	
	private void dragTo( float x, float y ) {
		
		float deltaX = Math.abs( x - mDraggingLastX ) ;
		float deltaY = Math.abs( y - mDraggingLastY ) ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		
		// vertical movement with x-ward drift?
		int index = Math.min(mDriftCorrectionMinDiagonalRatio.length-1, mDraggingStepsInDirection) ;
		if ( deltaX <= deltaY && deltaX / deltaY < mDriftCorrectionMinDiagonalRatio[index] )
			mDraggingOriginX += ( x - mDraggingLastX ) ;
		// horiz. movement with y-ward drift?
		else if ( deltaY < deltaX && deltaY / deltaX < mDriftCorrectionMinDiagonalRatio[index] )
			mDraggingOriginY += ( y - mDraggingLastY ) ;
		
		// left.  While position is more than gridX away from origin...
		while ( x + mGridWidth <= mDraggingOriginX ) {
			delegate.gestureDrag(DIRECTION_LEFT) ;
			mDraggingOriginX -= mGridWidth ;
			if ( mDraggingLastStepDirection == DIRECTION_LEFT )
				mDraggingStepsInDirection++ ;
			else {
				mDraggingLastStepDirection = DIRECTION_LEFT ;
				mDraggingStepsInDirection = 1 ;
			}
		}
		// right
		while ( x - mGridWidth >= mDraggingOriginX ) {
			delegate.gestureDrag(DIRECTION_RIGHT) ;
			mDraggingOriginX += mGridWidth ;
			if ( mDraggingLastStepDirection == DIRECTION_RIGHT )
				mDraggingStepsInDirection++ ;
			else {
				mDraggingLastStepDirection = DIRECTION_RIGHT ;
				mDraggingStepsInDirection = 1 ;
			}
		}
		// up.
		while ( y + mGridHeight <= mDraggingOriginY ) {
			delegate.gestureDrag(DIRECTION_UP) ;
			mDraggingOriginY -= mGridHeight ;
			if ( mDraggingLastStepDirection == DIRECTION_UP )
				mDraggingStepsInDirection++ ;
			else {
				mDraggingLastStepDirection = DIRECTION_UP ;
				mDraggingStepsInDirection = 1 ;
			}
		}
		// down
		while ( y - mGridHeight >= mDraggingOriginY ) {
			delegate.gestureDrag(DIRECTION_DOWN) ;
			mDraggingOriginY += mGridHeight ;
			if ( mDraggingLastStepDirection == DIRECTION_DOWN )
				mDraggingStepsInDirection++ ;
			else {
				mDraggingLastStepDirection = DIRECTION_DOWN ;
				mDraggingStepsInDirection = 1 ;
			}
		}
		
		mDraggingLastX = x ;
		mDraggingLastY = y ;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// don't care.  we don't even synchronzie.
	}

	@Override
	synchronized public boolean onSingleTapUp(MotionEvent e) {
		if ( e == null )
			return true ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		//Log.d(TAG, "onSingleTapUp") ;
		
		// just a tap!  This is our rotation control.
		int actionPointerIndex = (e.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
		// despite the ID_MASK and ID_SHIFT names, this is actually the
		// pointer index (not an ID!)
		
		// determine our quadrant.
		float x = e.getX(actionPointerIndex) ;
		float y = e.getY(actionPointerIndex) ;
		
		delegate.gestureTap(
				x < mBounds.centerX() ? DIRECTION_LEFT : DIRECTION_RIGHT,
				y < mBounds.centerY() ? DIRECTION_UP   : DIRECTION_DOWN ) ;
		
		return true ;
	}

}
