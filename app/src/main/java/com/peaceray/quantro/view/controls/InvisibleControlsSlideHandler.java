package com.peaceray.quantro.view.controls;

import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Hashtable;

import android.graphics.Rect;


/**
 * A very simple class used to handle "slide actions."
 * 
 * Motivating example: sliding a finger up and down after holding on
 * the "opponent button" should allow the player to switch between 
 * displayed players.
 * 
 * A SlideHandler has one basic setting: the minimum distance 
 * a touch needs to move from its "starting position" before
 * it begins reporting slides.  From then on it reports, to
 * a delegate, the rectangle a slide has entered.
 * 
 * Instances of this class will report to the delegate every time
 * a slide enters or leaves a component rectangle.  Slide handlers
 * regard the moment we begin tracking a slide (based on distance
 * traveled) as the moment we enter the rectangle below the tap,
 * if any.
 * 
 * When a slide ends, it "leaves" every rectangle.  When a slide begins,
 * i.e. when a motion event exceeds the minimum slide distance, it
 * enters all rectangles under its current position.
 * 
 * SlideHandlers don't track which finger is being used, so it can potentially
 * produce wacko results when different fingers are passed in.
 * 
 * @author Jake
 *
 */
public class InvisibleControlsSlideHandler {
	
	public interface Delegate {
		/**
		 * Slide has entered the rectangle of the specified tag.
		 * @param handler
		 * @param tag
		 */
		public void icsh_slideEntered( InvisibleControlsSlideHandler handler, Object tag ) ;
		
		/**
		 * Slide has left the rectangle of the specified tag.
		 * @param handler
		 * @param tag
		 */
		public void icsh_slideLeft( InvisibleControlsSlideHandler handler, Object tag ) ;
	}
	
	WeakReference<Delegate> mwrDelegate ;
	
	float mMinimumSlideDistance ;
	Hashtable<Object, Rect> mSlideRects ;
	
	boolean mTouched ;
	boolean mSliding ;
	
	// If currently sliding, hold the x/y coordinates of the
	// last move location.  If touched, but not sliding, holds
	// the "starting" location of the touch.
	int mPrevX, mPrevY ;
	
	
	public InvisibleControlsSlideHandler( Delegate delegate, float minSlideDistance, Hashtable<Object, Rect> slideRects ) {
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
		mMinimumSlideDistance = minSlideDistance ;
		mSlideRects = slideRects == null ? null : (Hashtable<Object, Rect>)slideRects.clone() ;
		
		mTouched = false ;
		mSliding = false ;
	}
	
	
	public void setDelegate( Delegate delegate ) {
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	public Delegate getDelegate() {
		return mwrDelegate.get() ;
	}
	
	
	public void setSlideRects( Hashtable<Object, Rect> slideRects ) {
		if ( mSliding )
			touchCancel() ;
		mSlideRects = slideRects == null ? null : (Hashtable<Object, Rect>)slideRects.clone() ;
	}
	
	
	public Rect getSlideRect( Object tag ) {
		if ( mSlideRects == null )
			return null ;
		return mSlideRects.get(tag) ;
	}
	
	
	public boolean isTouching() {
		return mTouched ;
	}
	
	
	public void touchDown( int x, int y ) {
		mTouched = true ;
		mSliding = false ;
		mPrevX = x ;
		mPrevY = y ;
	}
	
	public void touchMove( int x, int y ) {
		boolean startedSliding = false ;
		if ( !mTouched ) {
			return ;
		}
		if ( !mSliding ) {
			float deltaX = x - mPrevX ;
			float deltaY = y - mPrevY ;
			float dist = (float)Math.sqrt( deltaX*deltaX + deltaY*deltaY ) ;
			if ( dist >= mMinimumSlideDistance ) {
				mSliding = true ;
				startedSliding = true ;
			}
		}
		
		if ( mSliding ) {
			Delegate delegate = mwrDelegate.get() ;
			if ( delegate == null || mSlideRects == null )
				return ;
			
			Enumeration<Object> tempkeys = mSlideRects.keys() ;
			for ( ; tempkeys.hasMoreElements() ; ) {
				Object key = tempkeys.nextElement() ;
				Rect r = mSlideRects.get(key) ;
			}
			
			// determine if we have left any rectangles.  We
			// can only "leave" if we did not just start sliding.
			if ( !startedSliding ) {
				Enumeration<Object> keys = mSlideRects.keys() ;
				for ( ; keys.hasMoreElements() ; ) {
					Object key = keys.nextElement() ;
					Rect r = mSlideRects.get(key) ;
					if ( r.contains(mPrevX, mPrevY) && !r.contains(x, y) ) {
						delegate.icsh_slideLeft(this, key) ;
					}
				}
			}
			
			// determine if we entered any rectangles.  If inside
			// a rectangle, we entered it only if we just started
			// sliding, or if we were not inside last time.
			Enumeration<Object> keys = mSlideRects.keys() ;
			for ( ; keys.hasMoreElements() ; ) {
				Object key = keys.nextElement() ;
				Rect r = mSlideRects.get(key) ;
				if ( r.contains(x, y) && 
						( startedSliding || !r.contains(mPrevX, mPrevY) ) ) {
					delegate.icsh_slideEntered(this, key) ;
				}
			}
			
			mPrevX = x ;
			mPrevY = y ;
		}
	}
	
	public void touchUp() {
		endTouch() ;
	} 

	public void touchCancel() {
		endTouch() ;
	}
	
	private void endTouch() {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null && mTouched && mSliding && mSlideRects != null ) {
			Enumeration<Object> keys = mSlideRects.keys() ;
			for ( ; keys.hasMoreElements() ; ) {
				Object key = keys.nextElement() ;
				Rect r = mSlideRects.get(key) ;
				if ( r.contains((int)mPrevX, (int)mPrevY) ) {
					delegate.icsh_slideLeft(this, key) ;
				}
			}
		}
		
		mTouched = false ;
		mSliding = false ;
	}
	
}
