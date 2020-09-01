package com.peaceray.quantro.view.controls;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import com.peaceray.quantro.R;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.view.controls.InvisibleControls.SlideHandlerDelegate;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


/**
 * Similar to InvisibleControlsGamepad, with a few differences.
 * 
 * The Gamepad controls uses its component buttons only as locational
 * references and visual indications of presses - the buttons do not
 * capture touches themselves.  Instead, the entire gamepad surface
 * is "touchable" and sliding fingers between buttons will in turn
 * slide the activation of those buttons.
 * 
 * Gesture is different.  We use our entire content area as a gestureable
 * surface, including any contained "buttons."  In other words a gesture
 * that begins in a free area and moves over a button will NOT trigger that
 * button.  On the other hand, the buttons themselves are allowed to respond directly to taps,
 * and a finger that 'taps' a button and slides somewhere else will NOT produce a gesture.
 * 
 * First, any Invisible
 * @author Jake
 *
 */
public class InvisibleControlsGesture extends InvisibleControls
		implements InvisibleControlsFourWayGestureListener.Delegate,
			InvisibleButtonInterface.Delegate {
	
	
	@SuppressWarnings("unused")
	private static final String TAG = "InvisibleControlsGesture" ;
	
	
	
	// These are the gestures we recognize.  They correspond to particular
	// button names, as read from our attributes.
	protected static final int GESTURE_FLING_LEFT 		= 0 ;
	protected static final int GESTURE_FLING_RIGHT 		= 1 ;
	protected static final int GESTURE_FLING_UP 			= 2 ;
	protected static final int GESTURE_FLING_DOWN 		= 3 ;
	
	protected static final int GESTURE_DRAG_LEFT 			= 4 ;
	protected static final int GESTURE_DRAG_RIGHT 		= 5 ;
	protected static final int GESTURE_DRAG_UP 			= 6 ;
	protected static final int GESTURE_DRAG_DOWN 			= 7 ;
	
	protected static final int GESTURE_TAP_TOP_LEFT		= 8 ;
	protected static final int GESTURE_TAP_TOP_RIGHT		= 9 ;
	protected static final int GESTURE_TAP_BOTTOM_LEFT	= 10 ;
	protected static final int GESTURE_TAP_BOTTOM_RIGHT	= 11 ;
	
	protected static final int NUM_GESTURES = 12 ;
	
	
	InvisibleControlsMotionEventHandler mEventHandler ;
	InvisibleControlsFourWayGestureListener mFourWayGestureListener ;
	ArrayList<String> mButtonNamesByGesture ;
	Hashtable<String, InvisibleButtonInterface> mButtonsByName ;
	Hashtable<String, Boolean> mEnabled ;
	
	boolean mLeanNextDrag = false ;
	float mFlingMinInchesPerSecond = 0 ;
	float mFlingMinScreensPerSecond = 0 ;
	float mFlingSensitivity = 1.0f ;
	float mSlideMinInches = 0 ;
	
	float mSlideExaggeration = 1.0f ;
	
	float mScreenPPI = 1 ;
	float mScreenPDiag = 1 ;
	
	float mGridStepX = 1 ;
	float mGridStepY = 1 ;
	
	Handler mHandler ;
	boolean mPrepared ;
	
	Hashtable<String, SlideHandlerDelegate> mSlideHandlerDelegates ;
	
	// Inflate from XML
	public InvisibleControlsGesture( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		
		constructor_setDefaults(context) ;
		constructor_setAttributes(context,attrs) ;
	}

	public InvisibleControlsGesture( Context context, AttributeSet attrs, int defStyle ) {
		super(context, attrs, defStyle) ;

		constructor_setDefaults(context) ;
		constructor_setAttributes(context,attrs) ;
	}
	
	private void constructor_setDefaults( Context context ) {
		mHandler = new Handler() ;
		
		mFourWayGestureListener = new InvisibleControlsFourWayGestureListener( context, this ) ;
		mButtonNamesByGesture = new ArrayList<String>() ;
		mButtonsByName = new Hashtable<String, InvisibleButtonInterface>() ;
		mEnabled = new Hashtable<String, Boolean>() ;

		// load up some 'null' button names.
		for ( int i = 0; i < NUM_GESTURES; i++ )
			mButtonNamesByGesture.add(null) ;
		
		Resources res = context.getResources() ;
		mFlingMinInchesPerSecond = res.getInteger(R.integer.controls_fling_min_inch_hundredths_per_second) / 100.0f ;
		mFlingMinScreensPerSecond = res.getInteger(R.integer.controls_fling_min_screen_hundredths_per_second) / 100.0f ;
		mSlideMinInches = res.getInteger(R.integer.controls_slide_min_inch_hundredths) / 100.0f ;
		
		mEventHandler = VersionCapabilities.supportsMultitouch()
				? new InvisibleControlsMultiTouchMotionEventHandler(this, false)
				: new InvisibleControlsSingleTouchMotionEventHandler(this, false) ;

		mPrepared = false ;
		
		mSlideHandlerDelegates = new Hashtable<String, SlideHandlerDelegate>() ;
	}
	
	private void constructor_setAttributes( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.InvisibleControlsGesture );
			
		final int N = a.getIndexCount();
		for (int i = 0; i < N; ++i)
		{
		    int attr = a.getIndex(i);
		    switch (attr) {
		    // flings //////////////////////////////////////////////////////////
		    case R.styleable.InvisibleControlsGesture_gesture_fling_left_button:
		    	mButtonNamesByGesture.set(GESTURE_FLING_LEFT, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_fling_right_button:
		    	mButtonNamesByGesture.set(GESTURE_FLING_RIGHT, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_fling_up_button:
		    	mButtonNamesByGesture.set(GESTURE_FLING_UP, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_fling_down_button:
		    	mButtonNamesByGesture.set(GESTURE_FLING_DOWN, a.getString(attr));
		    	break ;
		    // drags ///////////////////////////////////////////////////////////
		    case R.styleable.InvisibleControlsGesture_gesture_drag_left_button:
		    	mButtonNamesByGesture.set(GESTURE_DRAG_LEFT, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_drag_right_button:
		    	mButtonNamesByGesture.set(GESTURE_DRAG_RIGHT, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_drag_up_button:
		    	mButtonNamesByGesture.set(GESTURE_DRAG_UP, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_drag_down_button:
		    	mButtonNamesByGesture.set(GESTURE_DRAG_DOWN, a.getString(attr));
		    	break ;
		    // taps ////////////////////////////////////////////////////////////
		    case R.styleable.InvisibleControlsGesture_gesture_tap_top_left_button:
		    	mButtonNamesByGesture.set(GESTURE_TAP_TOP_LEFT, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_tap_top_right_button:
		    	mButtonNamesByGesture.set(GESTURE_TAP_TOP_RIGHT, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_tap_bottom_left_button:
		    	mButtonNamesByGesture.set(GESTURE_TAP_BOTTOM_LEFT, a.getString(attr));
		    	break ;
		    case R.styleable.InvisibleControlsGesture_gesture_tap_bottom_right_button:
		    	mButtonNamesByGesture.set(GESTURE_TAP_BOTTOM_RIGHT, a.getString(attr));
		    	break ;
		    }
		}
		a.recycle();
	}
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INVISIBLE CONTROLS METHODS
	// 
	

	synchronized public boolean prepared() {
		return mPrepared ;
	}

	@Override
	public void prepare() {
		prepareChildren(this) ;
		collectChildrenByName(mButtonsByName, this) ;
		
		Enumeration<String> names = mButtonsByName.keys() ;
		for ( ; names.hasMoreElements() ; )
			mEnabled.put(names.nextElement(), Boolean.TRUE) ;
		Iterator<String> iter = mButtonNamesByGesture.iterator() ;
		for ( ; iter.hasNext() ; ) {
			String n = iter.next() ;
			if ( n != null && !mEnabled.containsKey(n) )
				mEnabled.put(n, Boolean.TRUE) ;
		}
		
		mEventHandler = VersionCapabilities.supportsMultitouch()
				? new InvisibleControlsMultiTouchMotionEventHandler(this, false)
				: new InvisibleControlsSingleTouchMotionEventHandler(this, false) ;
		mPrepared = true ;
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
				ibi.setCaptureTouches(false) ;	// we DON'T like touches!
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
	
	
	@Override
	public boolean setButtonVisibleBoundsByName( String name, Rect r ) {
		// useful to shrink certain buttons to fit the margins.
		
		InvisibleButtonInterface ibi = this.mButtonsByName.get(name) ;
		if ( ibi != null && ibi.isTouchable() ) {
			View v = (View)ibi ;
			if ( r.width() > 0 ) {
				//Log.d(TAG, "setting button " + name + " to width " + r.width()) ;
				ViewGroup.LayoutParams lp = v.getLayoutParams() ;
				lp.width = r.width() ;
				if ( lp instanceof LinearLayout.LayoutParams )
					((LinearLayout.LayoutParams)lp).weight = 0 ;
				v.setLayoutParams(lp) ;
				v.forceLayout() ;
				return true ;
			}
		}
		
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

	@Override
	public boolean getButtonPressedByName(String name) {
		// This method is used to detect when SCORE is pressed.
		// It is irrelevant to our gesture controls.
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
		// for now, we average for a "square" shape.
		mScreenPPI = (xPerInch + yPerInch)/2 ;
		mScreenPDiag = (float)Math.sqrt( x*x + y*y ) ;
		
		setGestureMinFlingAndScroll() ;
	}
	
	/**
	 * Sets the sensitivity for "fling" operations.  Must be > 0.  The higher
	 * this number, the more sensitive the controls are to "flings" and the less
	 * likely a touch event will be interpreted as a tap or drag.
	 * 
	 * @param sensititivy
	 */
	public void setFlingSensitivity( float sensitivity ) {
		mFlingSensitivity = sensitivity ;
		setGestureMinFlingAndScroll() ;
	}
	
	
	/**
	 * Uses the current values of mScreenPPI, mScreenPDiag, to convert from
	 * mFlingMinInchesPerSecond, mFlingMinScreensPerSecond, and mFlingSensitivity
	 * to actual screen-pixel settings.
	 * 
	 * Gives those settings to our FourWayGestureListener.
	 * 
	 */
	private void setGestureMinFlingAndScroll() {
		float flingMinPixelsPerSecond = Math.min(
				(mFlingMinInchesPerSecond * mScreenPPI),
				(mFlingMinScreensPerSecond * mScreenPDiag) );
		float slideMinPixels = (mSlideMinInches * mScreenPPI) ;
		
		//Log.d(TAG, "setPixels") ;
		//Log.d(TAG, "flingMinInches as pixels " + (mFlingMinInchesPerSecond * ppi)) ;
		//Log.d(TAG, "flingMinScreens as pixels " + (mFlingMinScreensPerSecond * pdiag)) ;
		
		// It is safe to decrease minScroll pixels -- it will not effect drag.  That's because
		// drags are generally considered on a grid-based basis, the only requirement being that
		// the total distance traveled is greater than the MINIMUM of grid width and 'min scroll.'
		//
		// Once considered as a drag, the drag itself operates on grid size.  Decreasing the
		// min slide (while keeping the grid the same) will thus increase the chance of a FLING
		// and decrease the chance of a TAP, but drags should be largely unaffected.
		//
		// The same is the case for INCREASING minScroll pixels.  The drag activates on the MINIMUM
		// of the scroll and the grid size, which will become "grid size" but so what?
		mFourWayGestureListener.setMinFlingVelocity(flingMinPixelsPerSecond / mFlingSensitivity) ;
		mFourWayGestureListener.setMinScrollPixels(slideMinPixels / mFlingSensitivity) ;
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
		mGridStepX = stepWidth ;
		mGridStepY = stepHeight ;
		setGestureDragGrid() ;
	}
	
	
	public void setSlideExaggeration( float exaggerate ) {
		mSlideExaggeration = exaggerate ;
		setGestureDragGrid() ;
	}
	
	/**
	 * Uses the settings mGridStepX, mGridStepY, and mSlideExaggeration to
	 * set the 4-way listeners Drag Grid.
	 */
	private void setGestureDragGrid() {
		mFourWayGestureListener.setDragGrid(
				(int)Math.ceil(mGridStepX / this.mSlideExaggeration),
				(int)Math.ceil(mGridStepY / this.mSlideExaggeration)) ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// FOUR WAY GESTURE LISTENER DELEGATE METHODS
	// 

	@Override
	public void gestureTap(int horizontalHalf, int verticalHalf) {
		int gesture = -1 ;
		if ( horizontalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_LEFT
				&& verticalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_UP )
			gesture = GESTURE_TAP_TOP_LEFT ;
		else if ( horizontalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_LEFT
				&& verticalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_DOWN )
			gesture = GESTURE_TAP_BOTTOM_LEFT ;
		else if ( horizontalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_RIGHT
				&& verticalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_UP )
			gesture = GESTURE_TAP_TOP_RIGHT ;
		else if ( horizontalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_RIGHT
				&& verticalHalf == InvisibleControlsFourWayGestureListener.DIRECTION_DOWN )
			gesture = GESTURE_TAP_BOTTOM_RIGHT ;
		
		doGesture( gesture ) ;
	}

	@Override
	public void gestureFling(int direction) {
		int gesture = -1 ;
		switch( direction ) {
		case InvisibleControlsFourWayGestureListener.DIRECTION_LEFT:
			gesture = GESTURE_FLING_LEFT ;
			break ;
		case InvisibleControlsFourWayGestureListener.DIRECTION_RIGHT:
			gesture = GESTURE_FLING_RIGHT ;
			break ;
		case InvisibleControlsFourWayGestureListener.DIRECTION_UP:
			gesture = GESTURE_FLING_UP ;
			break ;
		case InvisibleControlsFourWayGestureListener.DIRECTION_DOWN:
			gesture = GESTURE_FLING_DOWN ;
			break ;
		}
		
		doGesture(gesture) ;
	}

	@Override
	public void gestureDrag(int direction) {
		int gesture = -1 ;
		switch( direction ) {
		case InvisibleControlsFourWayGestureListener.DIRECTION_LEFT:
			gesture = GESTURE_DRAG_LEFT ;
			break ;
		case InvisibleControlsFourWayGestureListener.DIRECTION_RIGHT:
			gesture = GESTURE_DRAG_RIGHT ;
			break ;
		case InvisibleControlsFourWayGestureListener.DIRECTION_UP:
			gesture = GESTURE_DRAG_UP ;
			break ;
		case InvisibleControlsFourWayGestureListener.DIRECTION_DOWN:
			gesture = GESTURE_DRAG_DOWN ;
			break ;
		}
		
		doGesture(gesture) ;
		
		if ( mLeanNextDrag )
			leanGesture(gesture) ;
	}
	
	
	/**
	 * We started dragging... or at least we THINK we did.
	 */
	public void gestureDragStarted() {
		mLeanNextDrag = true ;
	}

	
	
	@Override
	public void gestureDragFinished() {
		mLeanNextDrag = false ;
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.invisibleControlsUserDoneLeaningOnButtons(this) ;
	}
	
	
	protected void doGesture( int gesture ) {
		if ( gesture < 0 )
			return ;
		
		String button = mButtonNamesByGesture.get(gesture) ;
		doGesture(button) ;
	}
	
	protected void doGesture( String buttonName ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( buttonName != null && mEnabled.get(buttonName) && delegate != null )
			delegate.invisibleControlsUserDidTapButton(this, buttonName) ;
	}
	
	private void leanGesture( int gesture ) {
		if ( gesture < 0 )
			return ;
		
		String button = mButtonNamesByGesture.get(gesture) ;
		Delegate delegate = mwrDelegate.get() ;
		if ( button != null && mEnabled.get(button) && delegate != null )
			delegate.invisibleControlsUserIsLeaningOnButton(this, button) ;
	}
	
	
	private String gestureToString( int gesture ) {
		switch( gesture ) {
			case GESTURE_FLING_LEFT:
				return "Fling Left" ;
			case GESTURE_FLING_RIGHT:
				return "Fling Right" ;
			case GESTURE_FLING_UP:
				return "Fling Up" ;
			case GESTURE_FLING_DOWN:
				return "Fling Down" ;
			
			case GESTURE_DRAG_LEFT:
				return "Drag Left" ;
			case GESTURE_DRAG_RIGHT:
				return "Drag Right" ;
			case GESTURE_DRAG_UP:
				return "Drag Up" ;
			case GESTURE_DRAG_DOWN:
				return "Drag Down" ;
			
			case GESTURE_TAP_TOP_LEFT:
				return "Tap Top Left" ;
			case GESTURE_TAP_TOP_RIGHT:
				return "Tap Top Right" ;
			case GESTURE_TAP_BOTTOM_LEFT:
				return "Tap Bottom Left" ;
			case GESTURE_TAP_BOTTOM_RIGHT:
				return "Tap Bottom Right" ;
		}
		return "Unknown Gesture" ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INVISIBLE BUTTON INTERFACE DELEGATE METHODS
	// 

	@Override
	public void buttonPressed(InvisibleButtonInterface ibi, int taps ) {
		String button = ibi.name(taps) ;
		Delegate delegate = mwrDelegate.get() ;
		if ( mEnabled.get(button) && delegate != null )
			delegate.invisibleControlsUserDidPressButton( this, button ) ;
	}

	@Override
	public void buttonReleased(InvisibleButtonInterface ibi, int taps) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null )
			delegate.invisibleControlsUserDidReleaseButton( this, ibi.name(taps) ) ;
		// release is okay always.
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SLIDE HANDLERS
	//
	
	public boolean removeSlideHandler( String buttonName ) {
		mEventHandler.setSlideHandler(buttonName, null) ;
		return mSlideHandlerDelegates.remove(buttonName) != null ;
	}
	
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
		shd.onLayoutControls(onLayout_rect.left, onLayout_rect.top, onLayout_rect.right, onLayout_rect.bottom) ;
		shd.setDelegate(slideDelegate) ;
		
		// Put it in our hashtable and pass to the MotionEventHandler.
		mSlideHandlerDelegates.put(buttonName, shd) ;
		mEventHandler.setSlideHandler(buttonName, sh) ;
	}
	
	public Rect [] getSlideRegionsByName( String buttonName ) {
		SlideHandlerDelegate shd = mSlideHandlerDelegates.get(buttonName) ;
		if ( shd != null ) {
			return shd.getRegions(onLayout_rect.left, onLayout_rect.top, onLayout_rect.right, onLayout_rect.bottom) ;
		}
		
		return null ;
	}
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean passToGesture = true ;
		if ( mEventHandler.isRelevant(event, mEnabled) )
			passToGesture = !mEventHandler.onTouchEvent(event, mEnabled) ;
		
		if ( passToGesture )
			mFourWayGestureListener.onTouchEvent(event) ;
		return true ;
	}
	
	
	Rect onLayout_rect = new Rect() ;
	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b) ;
		
		onLayout_rect.set(l, t, r, b) ;
		mFourWayGestureListener.setGestureBounds(onLayout_rect) ;
		Enumeration<String> keys = mSlideHandlerDelegates.keys() ;
		for ( ; keys.hasMoreElements() ; ) {
			String key = keys.nextElement() ;
			mSlideHandlerDelegates.get(key).onLayoutControls(l, t, r, b) ;
		}
	}
	

}
