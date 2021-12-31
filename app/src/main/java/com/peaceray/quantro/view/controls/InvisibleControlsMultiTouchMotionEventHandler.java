package com.peaceray.quantro.view.controls;

import java.util.Hashtable;

import android.util.Log;
import android.view.MotionEvent;

public class InvisibleControlsMultiTouchMotionEventHandler extends
		InvisibleControlsSingleTouchMotionEventHandler {
	
	public static final String TAG = "ICMTMEventHandler" ;

	public InvisibleControlsMultiTouchMotionEventHandler( InvisibleControls ic, boolean anywhere ) {
		super(ic, anywhere) ;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event, Hashtable<String, Boolean> enabled) {
		
		int actionMasked = event.getAction() & MotionEvent.ACTION_MASK ;
		int actionPointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
					// despite the ID_MASK and ID_SHIFT names, this is actually the
					// pointer index (not an ID!)
		
		int pointerID ;
		
		float x, y ;
		
		switch( actionMasked ) {
		case MotionEvent.ACTION_CANCEL:
			//Log.d(TAG, "ACTION_CANCEL") ;
			// cancel EVERYTHING.
			resetButtons() ;
			return mTrackAnywhere ;
			
		case MotionEvent.ACTION_DOWN:
			//Log.d(TAG, "ACTION_DOWN") ;
			// A pointer press!  The one and only.
			x = event.getX() ;
			y = event.getY() ;
			
			pointerID = event.getPointerId(actionPointerIndex) ;
			return handlePointerDown( pointerID, x, y, enabled ) ;
			
		case MotionEvent.ACTION_POINTER_DOWN:
			//Log.d(TAG, "ACTION_POINTER_DOWN") ;
			// A pointer press!  The one and only.
			x = event.getX(actionPointerIndex) ;
			y = event.getY(actionPointerIndex) ;
			
			pointerID = event.getPointerId(actionPointerIndex) ;
			return handlePointerDown( pointerID, x, y, enabled ) ;
			
			
		case MotionEvent.ACTION_MOVE:
			//Log.d(TAG, "ACTION_MOVE") ;
			// We want to be sure we cover EVERY button we swipe
			// past.
			boolean handledAll = true ;
			for ( int h = 0; h < event.getHistorySize(); h++ ) {
				for ( int p = 0; p < event.getPointerCount(); p++ ) {
					pointerID = event.getPointerId(p) ;
					x = event.getHistoricalX(p, h) ;
					y = event.getHistoricalY(p, h) ;
					handledAll = handlePointerMoveTo( pointerID, x, y, enabled ) && handledAll ;
				}
			}
			for ( int p = 0; p < event.getPointerCount(); p++ ) {
				pointerID = event.getPointerId(p) ;
				x = event.getX(p) ;
				y = event.getY(p) ;
				handledAll = handlePointerMoveTo( pointerID, x, y, enabled ) && handledAll ;
			}
			
			return handledAll ;
			
		case MotionEvent.ACTION_POINTER_UP:
			//Log.d(TAG, "ACTION_POINTER_UP") ;
			pointerID = event.getPointerId(actionPointerIndex) ;
			return handlePointerUp( pointerID, enabled ) ;
			
		case MotionEvent.ACTION_UP:
			//Log.d(TAG, "ACTION_UP") ;
			// Finger lifted!  That's all we need.
			pointerID = event.getPointerId(actionPointerIndex) ;
			boolean didHandle = handlePointerUp( pointerID, enabled ) ;
			resetButtons() ;
			return didHandle ;
		}
		
		// Can't handle this.
		return false;
	}
	
	
	/**
	 * Is this relevant to us?  Definition of 'relevant' may differ based on class 
	 * and constructions parameters.
	 * 
	 * @param event
	 * @return
	 */
	public boolean isRelevant(MotionEvent event, Hashtable<String, Boolean> enabled) {
		int actionMasked = event.getAction() & MotionEvent.ACTION_MASK ;
		int actionPointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) >> MotionEvent.ACTION_POINTER_ID_SHIFT;
					// despite the ID_MASK and ID_SHIFT names, this is actually the
					// pointer index (not an ID!)
		
		float x, y ;
		
		switch( actionMasked ) {
		case MotionEvent.ACTION_DOWN:
			x = event.getX() ;
			y = event.getY() ;
			return mTrackAnywhere || findEnabledAndTouchableButtonIndex( x, y, enabled ) >= 0 ;
			
		case MotionEvent.ACTION_POINTER_DOWN:
			// A pointer press!  The one and only.
			x = event.getX(actionPointerIndex) ;
			y = event.getY(actionPointerIndex) ;
			return mTrackAnywhere || findEnabledAndTouchableButtonIndex( x, y, enabled ) >= 0 ;
			
		case MotionEvent.ACTION_CANCEL:
			return true ;
			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			return getPointerIDIsDown( event.getPointerId(actionPointerIndex) ) ;
			
		case MotionEvent.ACTION_MOVE:
			// a move is relevant if we are tracking ANY pointer index.
			return getAnyPointerIDIsDown() ;
		}
		
		return false ;
	}
	
}
