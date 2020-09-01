package com.peaceray.android.view;

import android.util.Log;
import android.view.MotionEvent;

public class GestureDetectorMultitouch extends GestureDetector {
	
	private static final String TAG = "GestureDetectorMultitouch" ;
	
	protected static final int MAX_POINTERS = 10 ;
	
	protected GestureDetectorMultitouch( OnGestureListener listener ) {
		super(listener) ;
	}
	
	@Override
	protected void allocate() {
		allocate(MAX_POINTERS) ;
	}
	
	public boolean onTouchEvent( MotionEvent event ) {
		int actionMasked = event.getAction() & MotionEvent.ACTION_MASK ;
		int actionPointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
					// despite the ID_MASK and ID_SHIFT names, this is actually the
					// pointer index (not an ID!)
		
		int pointerID = event.getPointerId(actionPointerIndex) ;
		
		switch( actionMasked ) {
		case MotionEvent.ACTION_DOWN:
			return startMotionEvent( pointerID, event, event.getX(), event.getY() ) ;
			
		case MotionEvent.ACTION_POINTER_DOWN:
			return startMotionEvent( pointerID, event, event.getX(actionPointerIndex), event.getY(actionPointerIndex) ) ;
		
		case MotionEvent.ACTION_UP:
			// sometimes we get an "up" without a corresponding "move," even
			// if the pointer is in a new position.
			moveMotionEvent( event ) ;
			return endMotionEvent( pointerID, event, event.getX(), event.getY() ) ;
			
		case MotionEvent.ACTION_POINTER_UP:
			// sometimes we get an "up" without a corresponding "move," even
			// if the pointer is in a new position.
			moveMotionEvent( event ) ;
			return endMotionEvent( pointerID, event, event.getX(actionPointerIndex), event.getY(actionPointerIndex) ) ;
		
		case MotionEvent.ACTION_CANCEL:
			return cancelMotionEvents() ;
			
		case MotionEvent.ACTION_MOVE:
			return moveMotionEvent( event ) ;
		}
		
		return false ;
	}
	
	
	/**
	 * 
	 * @param event
	 * @return
	 */
	protected boolean moveMotionEvent( MotionEvent event ) {
		
		int actionMasked = event.getAction() & MotionEvent.ACTION_MASK ;
		
		boolean consumed = true ;
		
		for ( int i = 0; i < event.getPointerCount(); i++ )
			consumed = moveMotionEventMultiTouch( event.getPointerId(i), i, event, actionMasked == MotionEvent.ACTION_MOVE ) && consumed ;
		
		return consumed ;
	}
	
	
	protected boolean moveMotionEventMultiTouch( int pointerID, int pointerIndex, MotionEvent event, boolean usePointerCalls ) {
		if ( !mTrackingMotion[pointerID] )
			return false ;
		
		// the first position to include in our velocity calculations.
		int firstVelocityHistory = 0 ;
		
		// our convention is to only measure velocity and the like once
		// an event scrolls.
		for ( int i = 0; i < event.getHistorySize(); i++ ) {
			if ( !mHasScrolled[pointerID] ) {
				// check distance between this and down position.
				double xDiff = ( usePointerCalls
									? event.getHistoricalX(pointerIndex, i)
									: event.getHistoricalX(i) )
								- mMotionDownX[pointerID] ;
				double yDiff = event.getHistoricalY(pointerIndex, i) - mMotionDownY[pointerID] ;
				double dist = Math.sqrt( xDiff*xDiff + yDiff*yDiff ) ;
				if ( dist >= scrollMinPixels )
					mHasScrolled[pointerID] = true ;
				else
					firstVelocityHistory++ ;		// do NOT include this one.
			}
		}
		
		if ( !mHasScrolled[pointerID] ) {
			// check distance between this and down position.
			double xDiff = event.getX(pointerIndex) - mMotionDownX[pointerID] ;
			double yDiff = event.getY(pointerIndex) - mMotionDownY[pointerID] ;
			double dist = Math.sqrt( xDiff*xDiff + yDiff*yDiff ) ;
			// Log.d(TAG, "moveMotion dist " + dist) ;
			if ( dist >= scrollMinPixels )
				mHasScrolled[pointerID] = true ;
			else
				firstVelocityHistory++ ;		// do NOT include this one.
		}
		

		
		// Log.d(TAG, "moveMotion.  scrolled " + mHasScrolled[pointerID] + " pointer "+ pointerID + " index " + pointerIndex + " yPosIndex " + event.getY(pointerIndex) + " yPos " + event.getY() + " down " + mMotionDownY[pointerID]) ;
		
		boolean consumed = true ;
		
		// scroll?
		if ( mHasScrolled[pointerID] ) {
			// compare against last.
			consumed = mListener.onScroll(
					mMotionEventDown[pointerID],
					event,
					event.getX(pointerIndex) - mLastX[pointerID],
					event.getY(pointerIndex) - mLastY[pointerID]) ;
			mLastX[pointerID] = event.getX(pointerIndex);
			mLastY[pointerID] = event.getY(pointerIndex);

			// compute current average fling velocity.
			// Log.d(TAG, "INCLUDE IN AVERAGE MULTI TOUCH **************************************") ;
			includeInAverageMultiTouch( pointerID, pointerIndex, event, firstVelocityHistory ) ;
		}
		
		return consumed ;
	}
	
	
	protected void includeInAverageMultiTouch( int id, int pointerIndex, MotionEvent event, int firstHistory ) {
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
				x = event.getX(pointerIndex) ;
				y = event.getY(pointerIndex) ;
				t = event.getEventTime() ;
				//Log.d(TAG, "t is EventTime " + t) ;
			} else {
				x = event.getHistoricalX(pointerIndex, i) ;
				y = event.getHistoricalY(pointerIndex, i) ;
				t = event.getHistoricalEventTime(i) ;
				//Log.d(TAG, "t is HistoricalEventTime " + t) ;
			}
			if ( i == firstHistory ) {
				prevX = mVelocityLastXPosition[id] ;
				prevY = mVelocityLastYPosition[id] ;
				prevT = mVelocityLastTime[id] ;
				//Log.d(TAG, "prevT is VelocityLastTime " + prevT) ;
			} else {
				prevX = event.getHistoricalX( pointerIndex, i -1 ) ;
				prevY = event.getHistoricalY( pointerIndex, i -1 ) ;
				prevT = event.getHistoricalEventTime( i -1 ) ;
				//Log.d(TAG, "prevT is HistoricalEventTime " + prevT) ;
			}
			
			double w = t - prevT ;
			
			if ( w > 0 ) {
				double velX = ( x - prevX ) / w ;
				double velY = ( y - prevY ) / w ;
				
				// include in rolling average.
				w_all += w ;
				double Fw_all_plus = F( w_all ) ;
				
				X_all += 1000 * velX * (Fw_all_plus - Fw_all) ;
				Y_all += 1000 * velY * (Fw_all_plus - Fw_all) ;
				
				//Log.d(TAG, "Fw_all_plus " + Fw_all_plus + " for value " + w_all) ;
				//Log.d(TAG, "X_all " + X_all + " including " + velX + " times Fw_all_plus") ;
				//Log.d(TAG, "Y_all " + Y_all + " including " + velY + " times Fw_all_plus") ;
				
				Fw_all = Fw_all_plus ;
			}
		}
		
		// include last.
		if ( mVelocityW[id] > 0 ) {
			w_all += mVelocityW[id] ;
			double Fw_all_plus = F( w_all ) ;
			
			X_all += mVelocityX[id] * (Fw_all_plus - Fw_all) ;
			Y_all += mVelocityY[id] * (Fw_all_plus - Fw_all) ;
			
			//Log.d(TAG, "Fw_all_plus " + Fw_all_plus) ;
			//Log.d(TAG, "X_all " + X_all + " including " + mVelocityX[id] + " times Fw_all_plus") ;
			//Log.d(TAG, "Y_all " + Y_all + " including " + mVelocityY[id] + " times Fw_all_plus") ;
			
			Fw_all = Fw_all_plus ;
		}
		
		mVelocityX[id] = X_all / Fw_all ;
		mVelocityY[id] = Y_all / Fw_all ;
		
		mVelocityLastXPosition[id] = event.getX(pointerIndex);
		mVelocityLastYPosition[id] = event.getY(pointerIndex);
		mVelocityLastTime[id] = event.getEventTime() ;
		mVelocityW[id] = w_all ;
		
		//Log.d(TAG, "velocity updated to " + mVelocityX[id] + " , " + mVelocityY[id]) ;
	}
	
}
