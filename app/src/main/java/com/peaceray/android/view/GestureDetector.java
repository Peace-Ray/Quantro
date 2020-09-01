package com.peaceray.android.view;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

/**
 * This GestureDetector is intended as a replacement for Android's class of
 * the same name.  Its purpose is to provide single-touch support for arbitrary
 * pointer numbers on a Multi-touch device; in other words, although only 1-touch gestures
 * are detected, those gestures may originate from any current pointer (not just
 * the first to touch the screen, as in Android's implementation).
 * 
 * Gestures by each pointer are independent of each other.  If you are interested
 * in gestures only by a single pointer - for example, by a pointer which had a DOWN
 * event at a particular time, in a particular area, or simply first to meet some arbitrary
 * qualification - that is your responsibility to track in your attached Listener.
 * The pointer ID and index will be provided in every callback via MotionEvents;
 * just ignore the ones that don't matter.
 * 
 * GestureDectectors are safe to instantiate by the factory method new(), which will
 * return either an instance of this class or (if multitouch is supproted) the subclass
 * GestureDetectorMultitouch.
 * 
 * The single-touch version, and any version if ACTION_POINTER_DOWN / ACTION_POINTER_UP
 * is ignored, is theoretically equivalent to Android's implementation, but not functionally
 * equivalent.  Reported "swipe" velocities will differ, as will the number of milliseconds
 * allowed between double-taps, and the length of single-taps and long-presses.  These values
 * can be altered based on your preferences.  Possibly more significant is the minutia of
 * unspecified behavior.  For example, in a practical 4.1 test, it seems that
 * android.view.GestureDetector will report Scroll and Swipe events for a pointer (ID 0) which
 * has NO corresponding "DOWN" event, and thus report either a 'null' MotionEvent in the
 * callback or an obsolete one from a previous touch.  This class does not exhibit that
 * behavior, and requires that all gestures begin with an ACTION_DOWN or ACTION_POINTER_DOWN.
 * Again, to limit to Android's implementation, just check the motion event provided in callbacks
 * to match the action ACTION_DOWN.
 * 
 * To configure this class, alter the values of the static fields LAMBDA, SINGLE_TAP_MAX_MILLIS_HELD,
 * DOUBLE_TAP_MAX_MILLIS_BETWEEN, and/or the static function F.  You can even refactor them as
 * non-static and load from resources or set in the constructor, if you want, though I can't
 * imagine why you would.
 * 
 * @author Jake Rosin
 *
 */
public class GestureDetector {
	
	private static final String TAG = "GestureDetector" ;
	
	
	/**
	 * A parameterized family of functions used in averaging velocities with the following properties:
	 * 
	 * 1. F_l(0) = 0
	 * 2. F_l(inf.) = 1
	 * 3. F_l(a) < F_l(a + alpha) for a >= 0, alpha > 0  --  F_l monotonically increases
	 * 4. F_l'(a) > F_l'(a + alpha) for a >= 0, alpha > 0  -- F_l', derivative of F_l, monotonically decreases
	 * 5. 'l' > 0, the 'rate of decay' or 'myopia' parameter, controls the extent to which old data is forgotten
	 * 		and new data prioritized.  For l1 > l2, it should generally be the case that
	 * 		F_l1(a) > F_l2(a) for small a, and F_l1(x+a) - F_l1(x) < F_l2(x+a) - F_l2(x) for big x.
	 * 
	 * F is used to smoothly average over time-series velocity measurements (MotionEvent.ACTION_MOVE
	 * positions).
	 * 
	 * Feel free to tweak LAMBDA, the myopia parameter, or write your own version of F that conforms to the above
	 * specifications.  The current implementation is the cumulative distribution function (CDF) for the
	 * exponential distribution.  You may even have an optimization for it.
	 * 
	 * @param x
	 * @param w
	 * @return
	 */
	protected static double F( double a ) {
		// ignore the possibility of a < 0
		return 1.0 - Math.exp(-a * LAMBDA) ;
	}
	protected static final double LAMBDA = 0.04 ;
	protected static final long SINGLE_TAP_MAX_MILLIS_HELD = 150 ;
	protected static final long DOUBLE_TAP_MAX_MILLIS_BETWEEN = 300 ;
	protected static final long LONG_PRESS_MILLIS = 700 ;
	private static final double DEFAULT_SCROLL_MIN_PIXELS = 10.0 ;		// must move at least this many to scroll.
	private static final double DEFAULT_FLING_MIN_VELOCITY = 50 ;
	
	protected static double scrollMinPixels ;
	protected static double flingMinVelocity ;
	
	protected OnGestureListener mListener ;
	
	protected boolean [] mTrackingMotion ;
	protected MotionEvent [] mMotionEventDown ;
	protected float [] mMotionDownX ;
	protected float [] mMotionDownY ;
	
	protected float [] mLastX ;
	protected float [] mLastY ;
	protected long [] mLastTime ;
	protected boolean [] mHasScrolled ;
	
	protected double [] mVelocityX ;
	protected double [] mVelocityY ;
	protected double [] mVelocityLastXPosition ;
	protected double [] mVelocityLastYPosition ;
	protected long [] mVelocityLastTime ;
	protected long [] mVelocityW ;		// width of this averaged velocity.
	
	

	public static GestureDetector newGestureDetector( Context context, OnGestureListener listener ) {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < 5)	// Eclair
			return new GestureDetector(listener) ;
		return new GestureDetectorMultitouch(listener) ; 	
	}
	
	
	protected GestureDetector( OnGestureListener listener ) {
		mListener = listener ;
		setDefaults() ;
		allocate() ;
	}
	
	protected void setDefaults() {
		scrollMinPixels = DEFAULT_SCROLL_MIN_PIXELS ;
		flingMinVelocity = DEFAULT_FLING_MIN_VELOCITY ;
	}
	
	protected void allocate() {
		allocate(1) ;
	}

	protected void allocate( int maxPointers ) {
		mTrackingMotion = new boolean[maxPointers];
		mMotionEventDown = new MotionEvent[maxPointers] ;
		mMotionDownX = new float[maxPointers] ;
		mMotionDownY = new float[maxPointers] ;
		
		mLastX = new float[maxPointers] ;
		mLastY = new float[maxPointers] ;
		mLastTime = new long[maxPointers] ;
		mHasScrolled = new boolean[maxPointers] ;
		
		mVelocityX = new double[maxPointers] ;
		mVelocityY = new double[maxPointers] ;
		mVelocityLastXPosition = new double[maxPointers] ;
		mVelocityLastYPosition = new double[maxPointers] ;
		mVelocityLastTime = new long[maxPointers] ;
		mVelocityW = new long[maxPointers] ;
	}
	
	
	
	public void setScrollMinPixels( double pixels ) {
		scrollMinPixels = pixels ;
	}
	
	public void setFlingMinPixelsPerSecond( double pixelsPerSecond ) {
		Log.d(TAG, "setFlingMinPixelsPerSecond was " + flingMinVelocity + " now is " + pixelsPerSecond) ;
		flingMinVelocity = pixelsPerSecond ;
	}
	
	
	
	
	
	
	public boolean isLongpressEnabled() {
		return false ;
	}

	public boolean onTouchEvent( MotionEvent event ) {
		int action = event.getAction() ;
		
		switch( action ) {
		case MotionEvent.ACTION_DOWN:
			return startMotionEvent( 0, event, event.getX(), event.getY() ) ;
		
		case MotionEvent.ACTION_UP:
			// sometimes we get an "up" without a corresponding "move," even
			// if the pointer is in a new position.
			moveMotionEventSingleTouch( event ) ;
			return endMotionEvent( 0, event, event.getX(), event.getY() ) ;
		
		case MotionEvent.ACTION_CANCEL:
			return cancelMotionEvents() ;
			
		case MotionEvent.ACTION_MOVE:
			return moveMotionEventSingleTouch( event ) ;
		}
		
		return false ;
	}
	
	public void setIsLongpressEnabled(boolean isLongpressEnabled) {
		throw new IllegalStateException("Longpress is not yet supported") ;
	}
	
	public void setOnDoubleTapListener(GestureDetector.OnDoubleTapListener onDoubleTapListener) {
		throw new IllegalStateException("Sorry, DoubleTapListener support is not implemented.  Add the appropriate functionality if you want.") ;
	}
	
	
	
	/**
	 * Starts tracking a new motion event, which is assumed to be DOWN
	 * or POINTER_DOWN (the action and pointer is NOT checked in this method).
	 * 
	 * @param pointerID
	 * @param event
	 * @param x
	 * @param y
	 * @return
	 */
	protected boolean startMotionEvent( int pointerID, MotionEvent event, float x, float y ) {
		mTrackingMotion[pointerID] = true ;
		mMotionEventDown[pointerID] = MotionEvent.obtain(event) ;
		
		mMotionDownX[pointerID] = x ;
		mMotionDownY[pointerID] = y ;
		
		mLastX[pointerID] = x ;
		mLastY[pointerID] = y ;
		mLastTime[pointerID] = event.getEventTime() ;
		mHasScrolled[pointerID] = false ;
		
		mVelocityX[pointerID] = 0 ;
		mVelocityY[pointerID] = 0 ;
		mVelocityLastXPosition[pointerID] = x ;
		mVelocityLastYPosition[pointerID] = y ;
		mVelocityLastTime[pointerID] = event.getEventTime() ;
		mVelocityW[pointerID] = 0 ;		
		
		return mListener.onDown(event) ;
	}
	
	
	protected boolean endMotionEvent( int pointerID, MotionEvent event, float x, float y ) {
		// could be a fling, a single tap, or nothing at all.
		if ( mTrackingMotion[pointerID] ) {
			
			boolean consumed = true ;
			
			float xDiff = (x - mMotionDownX[pointerID]) ;
			float yDiff = (y - mMotionDownY[pointerID]) ;
			float dist = (float) Math.sqrt(( xDiff * xDiff + yDiff * yDiff )) ;
			float vel = (float) Math.sqrt( mVelocityX[pointerID]*mVelocityX[pointerID]
			              +  mVelocityY[pointerID]*mVelocityY[pointerID] ) ;
			
			//Log.d(TAG, "up.  scrolled: " + mHasScrolled[pointerID] + ", pointer time " + (event.getEventTime() - mMotionEventDown[pointerID].getEventTime())
			//		+ " velocity " + (Math.sqrt( mVelocityX[pointerID]*mVelocityX[pointerID]
			//           +  mVelocityY[pointerID]*mVelocityY[pointerID]) ) ) ;
			//Log.d(TAG, "moved " + dist + " with min scroll " + scrollMinPixels ) ;
			// Log.d(TAG, "Fling velocity is " + vel + " compare against min " + flingMinVelocity) ;
			
			if ( !mHasScrolled[pointerID]
			                   && event.getEventTime() - mMotionEventDown[pointerID].getEventTime() <= SINGLE_TAP_MAX_MILLIS_HELD )
				consumed = mListener.onSingleTapUp(event) ;
			
			else if ( mHasScrolled[pointerID]
			        && Math.sqrt( mVelocityX[pointerID]*mVelocityX[pointerID]
			            +  mVelocityY[pointerID]*mVelocityY[pointerID] ) >= flingMinVelocity )
  				consumed = mListener.onFling(
  						mMotionEventDown[pointerID],
  						event,
  						(float)mVelocityX[pointerID],
  						(float)mVelocityY[pointerID]) ;
			                      			
			removeMotionEvent(pointerID) ;
			
			return consumed ;
		}
		
		return false ;
	}
	
	
	/**
	 * 
	 * @param event
	 * @return
	 */
	protected boolean moveMotionEventSingleTouch( MotionEvent event ) {
		
		if ( !mTrackingMotion[0] )
			return false ;
		
		// the first position to include in our velocity calculations.
		int firstVelocityHistory = 0 ;
		
		// our convention is to only measure velocity and the like once
		// an event scrolls.
		for ( int i = 0; i < event.getHistorySize(); i++ ) {
			if ( !mHasScrolled[0] ) {
				// check distance between this and down position.
				double xDiff = event.getHistoricalX(i) - mMotionDownX[0] ;
				double yDiff = event.getHistoricalY(i) - mMotionDownY[0] ;
				double dist = Math.sqrt( xDiff*xDiff + yDiff*yDiff ) ;
				if ( dist >= scrollMinPixels )
					mHasScrolled[0] = true ;
				else
					firstVelocityHistory++ ;		// do NOT include this one.
			}
		}
		
		if ( !mHasScrolled[0] ) {
			// check distance between this and down position.
			double xDiff = event.getX() - mMotionDownX[0] ;
			double yDiff = event.getY() - mMotionDownY[0] ;
			double dist = Math.sqrt( xDiff*xDiff + yDiff*yDiff ) ;
			if ( dist >= scrollMinPixels )
				mHasScrolled[0] = true ;
			else
				firstVelocityHistory++ ;		// do NOT include this one.
		}
		
		boolean consumed = true ;
		
		// scroll?
		if ( mHasScrolled[0] ) {
			// compare against last.
			consumed = mListener.onScroll(
					mMotionEventDown[0],
					event,
					event.getX() - mLastX[0],
					event.getY() - mLastY[0]) ;
			mLastX[0] = event.getX();
			mLastY[0] = event.getY();
			
			// compute current average fling velocity.
			includeInAverageSingleTouch( event, firstVelocityHistory ) ;
		}
		
		return consumed ;
	}
	
	
	protected boolean cancelMotionEvents() {
		for ( int i = 0; i < mTrackingMotion.length; i++ )
			if ( mTrackingMotion[i] )
				removeMotionEvent(i) ;
		return true ;
	}
	
	
	/**
	 * Includes the specified MOVE motion event in our average velocity
	 * calculations.  If firstHistory is > 0, it indicates the first index
	 * into historical positions which we should consider.  If == historySize,
	 * only include the current position.
	 * @param event
	 * @param firstHistory
	 */
	protected void includeInAverageSingleTouch( MotionEvent event, int firstHistory ) {
		// our averaging function requires each calculation to use the
		// difference between F(w_all) and F(w_all + w_i).  We then
		// set w_all += w_i, meaning we can get away with 1 F calculation
		// per step.
		
		long w_all = 0 ;
		double Fw_all = 0 ;
		double X_all = 0 ;
		double Y_all = 0 ;
		
		for ( int i = event.getHistorySize(); i > firstHistory-1; i-- ) {
			// moving backwards in time
			double x, y ;
			double prevX, prevY ;
			long t, prevT ;
			if ( i == event.getHistorySize() ) {
				x = event.getX() ;
				y = event.getY() ;
				t = event.getEventTime() ;
			} else {
				x = event.getHistoricalX(i) ;
				y = event.getHistoricalY(i) ;
				t = event.getHistoricalEventTime(i) ;
			}
			if ( i == firstHistory ) {
				prevX = mVelocityLastXPosition[0] ;
				prevY = mVelocityLastYPosition[0] ;
				prevT = mVelocityLastTime[0] ;
			} else {
				prevX = event.getHistoricalX( i -1 ) ;
				prevY = event.getHistoricalY( i -1 ) ;
				prevT = event.getHistoricalEventTime( i -1 ) ;
			}
			
			double w = t - prevT ;
			
			if ( w > 0 ) {
				double velX = 1000 * ( x - prevX ) / w ;
				double velY = 1000 * ( y - prevY ) / w ;
				
				// include in rolling average.
				w_all += w ;
				double Fw_all_plus = F( w_all ) ;
				X_all = velX * (Fw_all_plus - Fw_all) ;
				Y_all = velY * (Fw_all_plus - Fw_all) ;
				
				Fw_all = Fw_all_plus ;
			}
		}
		
		// include last.
		if ( mVelocityW[0] > 0 ) {
			w_all += mVelocityW[0] ;
			double Fw_all_plus = F( w_all ) ;
			X_all = mVelocityX[0] * (Fw_all_plus - Fw_all) ;
			Y_all = mVelocityY[0] * (Fw_all_plus - Fw_all) ;
			Fw_all = Fw_all_plus ;
		}
		
		mVelocityX[0] = X_all / Fw_all ;
		mVelocityY[0] = Y_all / Fw_all ;
		
		mVelocityLastXPosition[0] = event.getX();
		mVelocityLastYPosition[0] = event.getY();
		mVelocityLastTime[0] = event.getEventTime() ;
		mVelocityW[0] = w_all ;
	}
	
	
	protected void removeMotionEvent(int pointerID) {
		mTrackingMotion[pointerID] = false ;
		if ( mMotionEventDown[pointerID] != null )
			mMotionEventDown[pointerID].recycle() ;
		mMotionEventDown[pointerID] = null ;
		
		mMotionDownX[pointerID] = 0 ;
		mMotionDownY[pointerID] = 0 ;
		
		mLastX[pointerID] = 0 ;
		mLastY[pointerID] = 0 ;
		mLastTime[pointerID] = 0 ;
		mHasScrolled[pointerID] = false ;
		
		mVelocityX[pointerID] = 0 ;
		mVelocityY[pointerID] = 0 ;
		mVelocityW[pointerID] = 0 ;		
	}
	
	
	
	
	public interface OnGestureListener {
		/**
		 * Notified when a tap occurs with the down MotionEvent that triggered it.
		 * @param e
		 * @return
		 */
		public abstract boolean onDown(MotionEvent e) ;
		
		/**
		 * Notified of a fling event when it occurs with the initial on down MotionEvent and the matching up MotionEvent.
		 * @param e1
		 * @param e2
		 * @param velocityX
		 * @param velocityY
		 * @return
		 */
		public abstract boolean	onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) ;
		
		/**
		 * Notified when a long press occurs with the initial on down MotionEvent that trigged it.
		 * @param e
		 */
		public abstract void onLongPress(MotionEvent e) ;
		
		/**
		 * Notified when a scroll occurs with the initial on down MotionEvent and the current move MotionEvent.
		 * 
		 * @param e1
		 * @param e2
		 * @param distanceX
		 * @param distanceY
		 * @return
		 */
		public abstract boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) ;
		
		/**
		 * 		The user has performed a down MotionEvent and not performed a move or up yet.
		 * @param e
		 */
		public abstract void onShowPress(MotionEvent e) ;

		/**
		 * Notified when a tap occurs with the up MotionEvent that triggered it.
		 * @param e
		 * @return
		 */
		public abstract boolean onSingleTapUp(MotionEvent e) ;
		
	}
	
	
	
	
	
	public interface OnDoubleTapListener {
		
		/**
		 * Notified when a double-tap occurs.
		 * @param e The down motion event of the first tap of the double-tap.
		 * @return True if the event is consumed, else false.
		 */
		public abstract boolean onDoubleTap(MotionEvent e) ;

		/**
		 * Notified when an event within a double-tap gesture occurs,
		 * including the down, move, and up events.
		 * @param e The motion event that occurred during the double-tap gesture.
		 * @return true if the event is consumed, else false 
		 */
		public abstract boolean onDoubleTapEvent(MotionEvent e) ;
		
		/**
		 * Notified when a single-tap occurs.
		 * 
		 * Unlike onSingleTapUp(MotionEvent), this will only be called after
		 * the detector is confident that the user's first tap is not followed
		 * by a second tap leading to a double-tap gesture.
		 * 
		 * @param e The down motion event of the single-tap.
		 * @return true if the event is consumed, else false 
		 */
		public abstract boolean onSingleTapConfirmed(MotionEvent e) ;
		
	}
}
