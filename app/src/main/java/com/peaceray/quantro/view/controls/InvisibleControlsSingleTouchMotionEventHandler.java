package com.peaceray.quantro.view.controls;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

/**
 * An implementation of InvisibleControlsMotionEventEventHandler that
 * supports only single-touch operations.  
 *
 * This handler will function on any version of Android; it does not
 * require features added in API v5.
 * @author Jake
 *
 */
public class InvisibleControlsSingleTouchMotionEventHandler implements
		InvisibleControlsMotionEventHandler {
	
	public static final String TAG = "InvisibleControlsSingleTouchMotionEventHandler" ;
	
	private Rect tempRect = new Rect() ;

	private WeakReference<InvisibleControls> mwrInvisibleControls ;
	private WeakReference<InvisibleButtonInterface> [] mwrButtons ;
	private Hashtable<String, InvisibleControlsSlideHandler> mButtonSlideHandlers ;
	private int mNumButtons ;
	private int [] mNumPointerIDsOnButton ;
	private int [] mPointerIDButton ;
	private float [][] mPointerIDPosition ;	// x/y relative
	private boolean [] mPointerIDIsDown ;
	
	protected boolean mTrackAnywhere ;
	
	protected int mLastTouchedButton ;
	
	private static final int MAX_BUTTONS = 10 ;
	private static final int MAX_POINTER_ID = 5 ;	// no more than 5 fingers, dude
	
	
	/**
	 * Constructs a new SingleTouchMotion event Handler.  If 'downAnywhere' is true,
	 * we start tracking any motion event, anywhere.  Otherwise, we require that it
	 * be within a button.
	 * 
	 * @param ic
	 * @param downAnywhere
	 */
	public InvisibleControlsSingleTouchMotionEventHandler( InvisibleControls ic, boolean anywhere ) {
		mwrInvisibleControls = new WeakReference<InvisibleControls>(ic) ;
		mButtonSlideHandlers = new Hashtable<String, InvisibleControlsSlideHandler>() ;

		mwrButtons = new WeakReference[MAX_BUTTONS] ;	// at most 20 buttons
		mNumPointerIDsOnButton = new int[MAX_BUTTONS] ;
		mPointerIDPosition = new float[MAX_POINTER_ID][2] ;
		mPointerIDIsDown = new boolean[MAX_POINTER_ID] ;
		mPointerIDButton = new int[MAX_POINTER_ID] ;
		
		mTrackAnywhere = anywhere ;
		
		mLastTouchedButton = -1 ;
		
		collectButtonReferences() ;
		resetButtons() ;
	}
	
	protected void collectButtonReferences() {
		mNumButtons = collectButtonReferences( mwrInvisibleControls.get(), 0 ) ;
	}
	
	protected int collectButtonReferences( ViewGroup vg, int numCollected ) {
		int numPreviouslyCollected = numCollected ;
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			
			// If an invisible button, set delegate.  Otherwise, if
			// a ViewGroup, recur.
			if ( child instanceof InvisibleButtonInterface ) {
				InvisibleButtonInterface ibi = ((InvisibleButtonInterface) child) ;
				mwrButtons[numCollected] = new WeakReference<InvisibleButtonInterface>( ibi ) ;
				numCollected++ ;
			}
			else if ( child instanceof ViewGroup ) {
				numCollected += collectButtonReferences((ViewGroup)child, numCollected) ;
			}
		}
		
		return numCollected - numPreviouslyCollected ;
	}
	
	protected void resetButtons() {
		for ( int i = 0; i < mNumButtons; i++ ) {
			InvisibleButtonInterface ibi = mwrButtons[i].get() ;
			ibi.release(true) ;	// tell the delegate, why not.
			mNumPointerIDsOnButton[i] = 0 ;
			InvisibleControlsSlideHandler slideHandler = mButtonSlideHandlers.get(ibi.name()) ;
			if ( slideHandler != null && slideHandler.isTouching() )
				slideHandler.touchUp() ;
		}
		for ( int i = 0; i < MAX_POINTER_ID; i++ ) {
			mPointerIDIsDown[i] = false ;
			mPointerIDButton[i] = -1 ;
		}
	}
	
	protected boolean getRelativeBoundsWithinInvisibleControls( View v, Rect r ) {
		InvisibleControls ic = mwrInvisibleControls.get() ;
		if ( ic == null )
			return false ;
		
		r.set( v.getLeft(), v.getTop(), v.getRight(), v.getBottom() ) ;
		ViewParent parent = v.getParent() ;
		while ( parent != null && parent != ic ) {
			if ( parent instanceof ViewGroup ) {
				r.offset( ((ViewGroup)parent).getLeft(), ((ViewGroup)parent).getTop() ) ;
				parent = parent.getParent() ;
			} else {
				return false ;
			}
		}
		return parent == ic ;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, Hashtable<String, Boolean> enabled) {
		
		int action = event.getAction() ;
		
		float x, y ;
		
		switch( action ) {
		case MotionEvent.ACTION_CANCEL:
			// cancel EVERYTHING.
			resetButtons() ;
			return mTrackAnywhere ;		// we handle ANY touch without our bounds
			
		case MotionEvent.ACTION_DOWN:
			// A pointer press!  The one and only.
			x = event.getX() ;
			y = event.getY() ;
			
			return handlePointerDown( 0, x, y, enabled ) ;
			
		case MotionEvent.ACTION_MOVE:
			// We want to be sure we cover EVERY button we swipe
			// past.
			for ( int h = 0; h < event.getHistorySize(); h++ ) {
				x = event.getHistoricalX(h) ;
				y = event.getHistoricalY(h) ;
				handlePointerMoveTo( 0, x, y, enabled ) ;
			}
			x = event.getX();
			y = event.getY();
			
			return handlePointerMoveTo( 0, x, y, enabled ) ;
			
		case MotionEvent.ACTION_UP:
			// Finger lifted!  That's all we need.
			boolean didHandle = handlePointerUp( 0, enabled ) ;
			resetButtons() ;
			return didHandle ;
		}
		
		// We only handle those listed.
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
		int action = event.getAction() ;
		
		float x, y ;
		
		switch( action ) {
		case MotionEvent.ACTION_DOWN:
			x = event.getX() ;
			y = event.getY() ;
			return mTrackAnywhere || findEnabledAndTouchableButtonIndex( x, y, enabled ) >= 0 ;
			
		case MotionEvent.ACTION_CANCEL:
			return true ;
			
		case MotionEvent.ACTION_UP:
			return this.mPointerIDIsDown[0] ;
			
		case MotionEvent.ACTION_MOVE:
			return this.mPointerIDIsDown[0] ;
		}
		
		return false ;
	}
	
	

	@Override
	public void setSlideHandler(String buttonName,
			InvisibleControlsSlideHandler slideHandler) {
		if ( buttonName == null )
			throw new NullPointerException("Must provide non-null button name") ;
		if ( slideHandler != null )
			mButtonSlideHandlers.put(buttonName, slideHandler) ;
		else
			mButtonSlideHandlers.remove(buttonName) ;
	}
	
	
	protected boolean getPointerIDIsDown( int id ) {
		if ( id >= mPointerIDIsDown.length )
			return false ;
		return mPointerIDIsDown[id] ;
	}
	
	protected boolean getAnyPointerIDIsDown( ) {
		for ( int i = 0; i < mPointerIDIsDown.length; i++ )
			if ( mPointerIDIsDown[i] )
				return true ;
		return false ;
	}

	protected int findEnabledAndTouchableButtonIndex( float x, float y, Hashtable<String, Boolean> enabled ) {
		for ( int i = 0; i < mNumButtons; i++ ) {
			InvisibleButtonInterface ibi = mwrButtons[i].get() ;
			if ( ibi == null )
				continue ;
			View v = (View)ibi ;
			getRelativeBoundsWithinInvisibleControls(v, tempRect) ;
			Boolean en = enabled.get(ibi.name()) ;
			boolean touchable = ibi.isTouchable() ;
			if ( tempRect.contains(Math.round(x), Math.round(y)) && ( en == null || en ) && touchable ) {
				return i ;
			}
		}
		
		return -1 ;
	}
	
	protected boolean handlePointerDown( int pointerID, float x, float y, Hashtable<String, Boolean> enabled ) {
		if ( pointerID >= mPointerIDIsDown.length )
			return false ;
		
		// look for the button!
		int buttonIndex = findEnabledAndTouchableButtonIndex( x, y, enabled ) ;
		if ( buttonIndex >= 0 ) {
			mNumPointerIDsOnButton[buttonIndex] = 1 ;
			mPointerIDButton[pointerID] = buttonIndex ;
			InvisibleButtonInterface ibi = mwrButtons[buttonIndex].get() ;
			if ( ibi != null ) {
				ibi.press(true, mLastTouchedButton != buttonIndex) ;		// tell the delegate.
				// tell the slide handler
				InvisibleControlsSlideHandler slideHandler = mButtonSlideHandlers.get(ibi.name()) ;
				if ( slideHandler != null ) {
					slideHandler.touchDown((int)x, (int)y) ;
				}
			}
			
			mLastTouchedButton = buttonIndex ;
		}
		
		if ( buttonIndex >= 0 || mTrackAnywhere ) {
			mPointerIDIsDown[pointerID] = true ;
			mPointerIDPosition[pointerID][0] = x ;
			mPointerIDPosition[pointerID][1] = y ;
		}
		
		return mPointerIDIsDown[pointerID] ;
	}
	
	
	
	
	protected boolean handlePointerUp( int pointerID, Hashtable<String, Boolean> enabled ) {
		if ( pointerID >= mPointerIDIsDown.length )
			return false ;
		
		if ( !mPointerIDIsDown[pointerID] )
			return false ;
		
		if ( mPointerIDButton[pointerID] > -1 ) {
			int prevButton = mPointerIDButton[pointerID] ;
			mPointerIDIsDown[pointerID] = false ;
			mPointerIDButton[pointerID] = -1 ;
			
			mNumPointerIDsOnButton[prevButton]-- ;
			if ( mNumPointerIDsOnButton[prevButton] <= 0 ) {
				InvisibleButtonInterface ibi = mwrButtons[prevButton].get() ;
				if ( ibi != null ) {
					ibi.release(true) ;
					// tell the slide handler
					InvisibleControlsSlideHandler slideHandler = mButtonSlideHandlers.get(ibi.name()) ;
					if ( slideHandler != null ) {
						slideHandler.touchUp() ;
					}
				}
			}
		}
		
		return true ;
	}
	
	protected boolean handlePointerMoveTo( int pointerID, float x, float y, Hashtable<String, Boolean> enabled ) {
		if ( pointerID >= mPointerIDIsDown.length )
			return false ;
		
		// only has an effect if the pointer is down.
		if ( mPointerIDIsDown[pointerID] ) {
			
			int inPrev = mPointerIDButton[pointerID] ;
			int inPost = findEnabledAndTouchableButtonIndex( x, y, enabled ) ;
			
			// Slide handling, part 1: if we were previously in a button,
			// slide handling means we can't leave it by moving.
			if ( inPrev > -1 ) {
				InvisibleButtonInterface ibi = mwrButtons[inPrev].get() ;
				if ( ibi != null ) {
					InvisibleControlsSlideHandler slideHandler = mButtonSlideHandlers.get(ibi.name()) ;
					if ( slideHandler != null ) {
						slideHandler.touchMove((int)x, (int)y) ;
						// slide handling means we NEVER 'touch' a new button upon moves.
						// stay within the same button.
						inPost = inPrev ;
					}
				}
			}
			
			// Did we leave a button?  If so, decrement and
			// deactivate.
			if ( inPrev > -1 && inPost != inPrev ) {
				mNumPointerIDsOnButton[inPrev]-- ;
				if ( mNumPointerIDsOnButton[inPrev] <= 0 ) {
					mNumPointerIDsOnButton[inPrev] = 0 ;
					InvisibleButtonInterface ibi = mwrButtons[inPrev].get() ;
					if ( ibi != null )
						ibi.release(true) ;
				}
			}
			
			// Did we move on TOP of a button?  If so, increment
			// and activate.
			if ( inPost > -1 && inPost != inPrev ) {
				mNumPointerIDsOnButton[inPost]++ ;
				if ( mNumPointerIDsOnButton[inPost] <= 1 ) {
					mNumPointerIDsOnButton[inPost] = 0 ;
					InvisibleButtonInterface ibi = mwrButtons[inPost].get() ;
					if ( ibi != null ) {
						ibi.press(true, mLastTouchedButton != inPost) ;
						// tell the slide handler
						InvisibleControlsSlideHandler slideHandler = mButtonSlideHandlers.get(ibi.name()) ;
						if ( slideHandler != null ) {
							slideHandler.touchDown((int)x, (int)y) ;
						}
					}
					
					mLastTouchedButton = inPost ;
				}
			}
			
			// Set current stuff
			mPointerIDPosition[pointerID][0] = x ;
			mPointerIDPosition[pointerID][1] = y ;
			mPointerIDButton[pointerID] = inPost ;
			
			// stop tracking?
			if ( inPost == -1 && !mTrackAnywhere ) {
				// stop tracking it.
				mPointerIDIsDown[pointerID] = false ;
			}
			
			return true ;
		}
		
		return false ;
	}
}
