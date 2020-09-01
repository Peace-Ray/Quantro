package com.peaceray.quantro.view.controls;

import java.util.Enumeration;
import java.util.Hashtable;

import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.view.controls.InvisibleControls.SlideRegionType;
import com.peaceray.quantro.view.controls.InvisibleControlsSlideHandler.Delegate;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class InvisibleControlsGamepad extends InvisibleControls
		implements InvisibleButtonInterface.Delegate {

	
	InvisibleControlsMotionEventHandler mEventHandler ;
	
	Hashtable<String, InvisibleButtonInterface> mButtonsByName ;
	Hashtable<String, Boolean> mEnabled ;
	boolean mPrepared ;
	
	Hashtable<String, SlideHandlerDelegate> mSlideHandlerDelegates ;
	
	// Inflate from XML
	public InvisibleControlsGamepad( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		init( context ) ;
	}

	public InvisibleControlsGamepad( Context context, AttributeSet attrs, int defStyle ) {
		super(context, attrs, defStyle) ;
		init( context) ;
	}
	
	private void init( Context context ) {
		mEventHandler = VersionCapabilities.supportsMultitouch()
				? new InvisibleControlsMultiTouchMotionEventHandler(this, true)
				: new InvisibleControlsSingleTouchMotionEventHandler(this, true) ;
		mButtonsByName = new Hashtable<String, InvisibleButtonInterface>() ;
		mEnabled = new Hashtable<String, Boolean>() ;
		mPrepared = false ;
		mSlideHandlerDelegates = new Hashtable<String, SlideHandlerDelegate>() ;
	}
	
	
	synchronized public boolean prepared() {
		return mPrepared ;
	}
	
	
	/**
	 * Prepares the (loaded) controls by setting delegates for all included
	 * invisible buttons.
	 */
	synchronized public void prepare() {
		if ( mPrepared )
			throw new IllegalStateException("Can only prepare once.") ;
		
		prepareChildren(this) ;
		collectChildrenByName(mButtonsByName, this) ;
		
		Enumeration<String> names = mButtonsByName.keys() ;
		for ( ; names.hasMoreElements() ; )
			mEnabled.put(names.nextElement(), Boolean.TRUE) ;
		
		if ( VersionCapabilities.supportsMultitouch() )
			mEventHandler = new InvisibleControlsMultiTouchMotionEventHandler( this, true ) ;
		else
			mEventHandler = new InvisibleControlsSingleTouchMotionEventHandler( this, true ) ;
	}
	
	
	/**
	 * Partially prepares the controls by setting delegates for all
	 * invisible buttons taken as subviews of vg.
	 * @param child
	 */
	private void prepareChildren( ViewGroup vg ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			
			// If an invisible button, set delegate.  Otherwise, if
			// a ViewGroup, recur.
			if ( child instanceof InvisibleButtonInterface ) {
				InvisibleButtonInterface ibi = ((InvisibleButtonInterface) child) ;
				ibi.setDelegate(this) ;
				ibi.setCaptureTouches(false) ;	// we use our custom MotionEventHandler to deal with touches.
				//ibi.show();		// remove this line to start invisible.
			}
			else if ( child instanceof ViewGroup ) {
				prepareChildren((ViewGroup)child) ;
			}
		}
	}
	
	/**
	 * Collects all the invisible button interfaces within the ViewGroup
	 * provided and stores them in the provided hash table by name.
	 * @param child
	 */
	private void collectChildrenByName( Hashtable<String, InvisibleButtonInterface>ht, ViewGroup vg ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			
			// If an invisible button, set delegate.  Otherwise, if
			// a ViewGroup, recur.
			if ( child instanceof InvisibleButtonInterface ) {
				InvisibleButtonInterface ibi = ((InvisibleButtonInterface) child) ;
				ht.put(ibi.name(), ibi) ;
			}
			else if ( child instanceof ViewGroup ) {
				collectChildrenByName(ht, (ViewGroup)child) ;
			}
		}
	}
	
	
	@Override
	public void setEnabled(boolean on) {
		super.setEnabled(on) ;
		setEnabledChildren(on, this) ;
		Enumeration<String> names = mEnabled.keys() ;
		for ( ; names.hasMoreElements() ; )
			mEnabled.put(names.nextElement(), Boolean.valueOf(on)) ;
	}
	
	private void setEnabledChildren( boolean on, ViewGroup vg ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			child.setEnabled(on) ;
			if ( child instanceof ViewGroup )
				setEnabledChildren(on, (ViewGroup)child) ;
		}
	}
	
	
	public void setEnabled(boolean on, String [] gameButtonNames) {
		setEnabledChildren( on, this, gameButtonNames ) ;
		for ( int i = 0; i < gameButtonNames.length; i++ )
			mEnabled.put(gameButtonNames[i], Boolean.valueOf(on)) ;
	}
	
	private void setEnabledChildren( boolean on, ViewGroup vg, String [] names ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			if ( child instanceof InvisibleButtonInterface ) {
				InvisibleButtonInterface ibi = (InvisibleButtonInterface)child ;
				for ( int j = 0; j < names.length; j++ ) {
					if ( ibi.name().equals( names[j] ) ) {
						child.setEnabled(on) ;
						break ;
					}
				}
			}
			if ( child instanceof ViewGroup )
				setEnabledChildren(on, (ViewGroup)child, names) ;
		}
	}
	
	
	/**
	 * Set the provided buttons to show (or not) when pressed.
	 * @param on
	 * @param buttonNames
	 */
	public void setShowWhenPressed(boolean on, String [] buttonNames) {
		setShowWhenPressedChildren( on, this, buttonNames ) ;
	}
	
	private void setShowWhenPressedChildren( boolean on, ViewGroup vg, String [] names ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			if ( child instanceof InvisibleButtonInterface ) {
				InvisibleButtonInterface ibi = (InvisibleButtonInterface)child ;
				for ( int j = 0; j < names.length; j++ ) {
					if ( ibi.name().equals( names[j] ) ) {
						ibi.setShowWhenPressed(on) ;
						break ;
					}
				}
			}
			if ( child instanceof ViewGroup )
				setShowWhenPressedChildren(on, (ViewGroup)child, names) ;
		}
	}
	
	/**
	 * Set the provided buttons to show (or not) when pressed.
	 * @param on
	 * @param buttonNames
	 */
	public void setShowWhenPressedDefault(String [] buttonNames) {
		setShowWhenPressedDefaultChildren( this, buttonNames ) ;
	}
	
	private void setShowWhenPressedDefaultChildren(ViewGroup vg, String [] names ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			if ( child instanceof InvisibleButtonInterface ) {
				InvisibleButtonInterface ibi = (InvisibleButtonInterface)child ;
				for ( int j = 0; j < names.length; j++ ) {
					if ( ibi.name().equals( names[j] ) ) {
						ibi.setShowWhenPressedDefault() ;
						break ;
					}
				}
			}
			if ( child instanceof ViewGroup )
				setShowWhenPressedDefaultChildren((ViewGroup)child, names) ;
		}
	}
	
	
	public void setTouchable( boolean on, String name ) {
		InvisibleButtonInterface ibi = mButtonsByName.get(name) ;
		if ( ibi != null )
			ibi.setTouchable(on) ;
	}
	
	/**
	 * Sets whether the specified buttons should be considered "touchable" - i.e.,
	 * whether there is a dedicated screen region for them.  The buttons are
	 * still considered enabled and button output may still occur if other input
	 * methods are possible, such as gestures.
	 * @param on
	 * @param gameButtonNames
	 */
	public void setTouchable(boolean on, String [] gameButtonNames) {
		for ( int i = 0; i < gameButtonNames.length; i++ ) {
			InvisibleButtonInterface ibi = mButtonsByName.get(gameButtonNames[i]) ;
			if ( ibi != null )
				ibi.setTouchable(on) ;
		}
	}
	
	
	public void setUseAlt( boolean on, String name ) {
		InvisibleButtonInterface ibi = mButtonsByName.get(name) ;
		if ( ibi != null )
			ibi.setUseAlt(on) ;
	}
	
	
	
	/**
	 * A convenience method which may have an effect depending on the InvisibleControls 
	 * implementation.  Provides the area on-screen in which the invisible controls will
	 * be "drawn."  This is likely to be smaller than or equal to the current view bounds.
	 * 
	 * One possible effect: for gesture controls, where Reserve, Score and Opponent buttons
	 * are necessary but should not take up too much screen real-estate, we can 'layout'
	 * them as very large and let the GameView design the touchable area based
	 * on how much space is needed to draw the game.  The GameView then provides the visible
	 * bounds in this method, and InvisibleControls could then resize the views, or
	 * the onscreen area it considers "touchable."
	 * 
	 * @param name
	 * @param r
	 */
	public boolean setButtonVisibleBoundsByName( String name, Rect r ) {
		// Gamepads ignore this method.
		return false ;
	}
	
	
	public Rect getButtonBoundsByName(String name) {
		InvisibleButtonInterface ibi = mButtonsByName.get(name) ;
		if ( ibi == null )
			return null ;
		
		View v = (View) ibi ;
		if ( v.getVisibility() == View.GONE )
			return null ;
		Rect r = new Rect( v.getLeft(), v.getTop(), v.getRight(), v.getBottom() ) ;
		while ( v != this ) {
			v = (View) v.getParent() ;
			r.offset( v.getLeft(), v.getTop() ) ;
		}
		
		return r ;
	}
	
	
	@Override
	public Rect getTouchableButtonBoundsByName(String name) {
		InvisibleButtonInterface ibi = mButtonsByName.get(name) ;
		if ( ibi == null || !ibi.isTouchable() )
			return null ;
		
		View v = (View) ibi ;
		if ( v.getVisibility() == View.GONE )
			return null ;
		Rect r = new Rect( v.getLeft(), v.getTop(), v.getRight(), v.getBottom() ) ;
		while ( v != this ) {
			v = (View) v.getParent() ;
			r.offset( v.getLeft(), v.getTop() ) ;
		}
		
		return r ;
	}
	
	
	
	/**
	 * Returns whether the specified button is currently being pressed.
	 * @param name A name of a button
	 * @return A new Rect bounding the button; null if the name was not found
	 */
	public boolean getButtonPressedByName( String name ) {
		if ( mButtonsByName.containsKey(name) )
			return mButtonsByName.get(name).pressed() ;
		return getButtonPressedByName(name, this) ;
	}
	
	private boolean getButtonPressedByName( String name, ViewGroup vg ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View child = vg.getChildAt(i) ;
			
			// If an invisible button, set delegate.  Otherwise, if
			// a ViewGroup, recur.
			if ( child instanceof InvisibleButtonInterface ) {
				InvisibleButtonInterface ibi = ((InvisibleButtonInterface) child) ;
				
				if ( name.equals( ibi.name() ) ) {
					return ibi.pressed() ;
				}
			}
			else if ( child instanceof ViewGroup ) {
				boolean p = getButtonPressedByName( name, (ViewGroup)child ) ;
				if ( p )
					return true ;
			}
		}
		return false ;
	}
	
	
	/**
	 * Sets the number of pixels per inch.  For certain controls
	 * types, this is used to help convert between pixel measurements
	 * and hand sizes / hand movements.
	 * 
	 * @param x
	 * @param y
	 */
	public void setPixels( float x, float y, float xPerInch, float yPerInch ) {
		// don't care
	}
	
	/**
	 * 
	 * Sets, in pixels, the onscreen dimensions of vertical and horizontal
	 * movement.  This is useful (e.g.) for gesture controls, when "pulling"
	 * a piece in one direction or the other.
	 * 
	 * @param width
	 * @param height
	 */
	public void setMovementGrid( int stepWidth, int stepHeight ) {
		// don't care
	}
	
	
	/**
	 * buttonPressed - called by the buttons to inform the controls
	 * of an important event.
	 * @param ibi
	 */
	public void buttonPressed( InvisibleButtonInterface ibi, int taps ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.invisibleControlsUserDidPressButton( this, ibi.name(taps) ) ;
	}
	
	/**
	 * buttonPressed - called by the buttons to inform the controls
	 * of an important event.
	 * @param ibi
	 */
	public void buttonReleased( InvisibleButtonInterface ibi, int taps ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.invisibleControlsUserDidReleaseButton( this, ibi.name(taps) ) ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SLIDE HANDLERS
	//
	
	@Override
	public boolean removeSlideHandler( String buttonName ) {
		mEventHandler.setSlideHandler(buttonName, null) ;
		return mSlideHandlerDelegates.remove(buttonName) != null ;
	}
	
	@Override
	public void putSlideHandler( SlideDelegate slideDelegate, String buttonName, SlideRegionType regionType, int numRegions ) {
		// First: make a SlideHandler.
		InvisibleControlsSlideHandler sh = new InvisibleControlsSlideHandler(
				null,
				TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
						(float)15, getResources().getDisplayMetrics()),
				null ) ;
		// Make a container Delegate object
		SlideHandlerDelegate shd = new SlideHandlerDelegate( sh, buttonName, regionType, numRegions,
				(int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
						(float)140, getResources().getDisplayMetrics()))) ;
		shd.onLayoutControls(mLeft, mTop, mRight, mBottom) ;
		shd.setDelegate(slideDelegate) ;
		
		// Put it in our hashtable and pass to the MotionEventHandler.
		mSlideHandlerDelegates.put(buttonName, shd) ;
		mEventHandler.setSlideHandler(buttonName, sh) ;
	}
	
	public Rect [] getSlideRegionsByName( String buttonName ) {
		SlideHandlerDelegate shd = mSlideHandlerDelegates.get(buttonName) ;
		if ( shd != null ) {
			return shd.getRegions(mLeft, mTop, mRight, mBottom) ;
		}
		
		return null ;
	}
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// OVERRIDES FOR VIEW METHODS
	// 
	
	int mLeft = 0, mTop = 0, mRight = 0, mBottom = 0 ;
	
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom) ;
		if ( changed ) {
			mLeft = left ;
			mTop = top ;
			mRight = right ;
			mBottom = bottom ;
			Enumeration<String> keys = mSlideHandlerDelegates.keys() ;
			for ( ; keys.hasMoreElements() ; ) {
				String key = keys.nextElement() ;
				mSlideHandlerDelegates.get(key).onLayoutControls(mLeft, mTop, mRight, mBottom) ;
			}
		}
	}
	
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if ( mEventHandler == null )
			return false ;
		return mEventHandler.onTouchEvent(event, mEnabled) ;
	}
	
}
