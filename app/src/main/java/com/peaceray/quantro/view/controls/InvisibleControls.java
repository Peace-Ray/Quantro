package com.peaceray.quantro.view.controls;



import java.lang.ref.WeakReference;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

public abstract class InvisibleControls extends RelativeLayout {
	
	// Delegate methods for the invisible controls.  If set,
	// the invisible controls object will call these methods when
	// appropriate.
	public interface Delegate {
		
		/**
		 * Called when the user has touched ver finger to a button within the invisibleControls.
		 * @param invisControls	A reference to the InvisibleControls object
		 * @param buttonName	The name of the button being touched
		 */
		public void invisibleControlsUserDidPressButton( InvisibleControls invisControls, String buttonName ) ;

		/**
		 * Called when the user has released ver finger from a button within invisibleControls.
		 * @param invisControls	A reference to the InvisibleControls object
		 * @param buttonName	The name of the button being released
		 */
		public void invisibleControlsUserDidReleaseButton( InvisibleControls invisControls, String buttonName ) ;
		
		/**
		 * Called when the user has "tapped" ver finger on a button within the invisibleControls.
		 * 'tap' is a stateless operation; it replaces "DidPressButton" -> "DidReleaseButton" and should
		 * be considered separately.
		 * 
		 * Usage: for actual buttons (i.e. InvisibleButtonInterfaces), we expect DidPress followed by DidRelease
		 * to be the order of the day.  For fake buttons (e.g. a tap or a swipe in a gesture interface) we expect
		 * this method to be used instead; for example, if the user swipes left, the result is a call to this
		 * with 
		 * 
		 */
		public void invisibleControlsUserDidTapButton( InvisibleControls invisControls, String buttonName ) ;
		
		
		/**
		 * Called when the user is "leaning" on a button, but not necessarily pressing it.
		 * 
		 * For "gamepad" style controls, this is equivalent to holding a button down (although it is a separate
		 * call from UserDidPressButton).
		 * 
		 * For "gesture" style controls, which rely on "tap button" for each movement step, this indicates
		 * that the "tap" is still ongoing and any turns should be in the currently leaned direction
		 * (if any).
		 * 
		 * Leaning does NOT activate any delayed-press actions.
		 * 
		 * @param invisControls
		 * @param buttonName
		 */
		public void invisibleControlsUserIsLeaningOnButton( InvisibleControls invisControls, String buttonName ) ;
		
		
		/**
		 * The user is finised leaning on the indicated button.
		 * 
		 * @param invisControls
		 * @param buttonName
		 */
		public void invisibleControlsUserDoneLeaningOnButton( InvisibleControls invisControls, String buttonName ) ;
		
		
		/**
		 * The user is done leaning on ALL buttons.
		 * 
		 * @param invisControls
		 */
		public void invisibleControlsUserDoneLeaningOnButtons( InvisibleControls invisControls ) ;
		
		
	}
	
	
	public interface SlideDelegate {
		public void invisibleControlsUserDidSlideIn( InvisibleControls invisControls, String buttonName, int slideRegion ) ;
		public void invisibleControlsUserDidSlideOut( InvisibleControls invisControls, String buttonName, int slideRegion ) ;
	}
	
	public static enum SlideRegionType {
		/**
		 * The slide region is the entirety of the controls, with
		 * regions uniformly distributed over the the vertical space
		 * of the Controls: for N regions, each region is W wide,
		 * and H/N tall.  They are numbered from 0 to numRegions-1,
		 * from top to bottom.
		 */
		UNIFORM_VERTICAL,
		
		/**
		 * The slide region is the entirety of the controls, with
		 * regions uniformly distributed over the the horizontal space
		 * of the Controls: for N regions, each region is W/N wide,
		 * and H tall.  They are numbered from 0 to numRegions-1,
		 * from left to right.
		 */
		UNIFORM_HORIZONTAL,
		
		/**
		 * The slide region is a portion of the controls, uniformly
		 * centered (if possible) around a horizontal line.  The 
		 * y-coordinate of that line, and the height of each rectangle,
		 * should be specified.
		 */
		UNIFORM_VERTICAL_CENTERED_AROUND,
		
		/**
		 * The slide region is a portion of the controls, uniformly
		 * centered (if possible) around a vertical line.  The 
		 * x-coordinate of that line, and the width of each rectangle,
		 * should be specified.
		 */
		UNIFORM_HORIZONTAL_CENTERED_AROUND,
		
		
	}
	
	protected class SlideHandlerData {
		private String mButtonName ;
		private SlideRegionType mRegionType ;
		private int mNumRegions ;
		private int mPreferredSize ;
		
		private InvisibleControlsSlideHandler mHandler ;
		
		protected SlideHandlerData( InvisibleControlsSlideHandler handler, String buttonName, SlideRegionType regionType, int numRegions, int preferredSize ) {
			mHandler = handler ;
			mButtonName = buttonName ;
			mRegionType = regionType ;
			mNumRegions = numRegions ;
			mPreferredSize = preferredSize ;
		}
		
		protected InvisibleControlsSlideHandler getHandler() {
			return mHandler ;
		}
		
		protected String getButtonName() {
			return mButtonName ;
		}
		
		protected SlideRegionType getSlideRegionType() {
			return mRegionType ;
		}
		
		protected int getNumRegions() {
			return mNumRegions ;
		}
		
		protected Rect [] getRegions( int l, int t, int r, int b, int centerX, int centerY ) {
			if ( mNumRegions == 0 )
				return null ;
			
			Rect [] rects = new Rect[mNumRegions] ;
			int w = r - l ;
			int h = b - t ;
			int step ;
			int total ;
			int start ;
			
			switch( mRegionType ) {
			case UNIFORM_VERTICAL:
				// step downward from t.
				step = h / mNumRegions ;
				for ( int i = 0; i < mNumRegions; i++ ) {
					rects[i] = new Rect(l, t + i*step, r, t + (i+1)*step) ;
				}
				// correct the last rect to match the bottom.
				rects[mNumRegions-1].bottom = b ;
				break ;
				
			case UNIFORM_HORIZONTAL:
				// step rightward from l.
				step = w / mNumRegions ;
				for ( int i = 0; i < mNumRegions; i++ ) {
					rects[i] = new Rect(l + i*step, t, l + (i+1)*step, b) ;
				}
				// correct the last rect to match the right.
				rects[mNumRegions-1].right = r ;
				break ;
				
			case UNIFORM_VERTICAL_CENTERED_AROUND:
				// center around centerX.
				total = Math.min( h, mNumRegions * mPreferredSize ) ;
				step = total / mNumRegions ;
				// start at centerX - half total, or 't', whichever
				// is lower on the screen (higher in value).
				start = Math.max( t, centerY - total/2 ) ;
				for ( int i = 0; i < mNumRegions; i++ ) {
					rects[i] = new Rect(l, start + i*step, r, start + (i+1)*step) ;
				}
				rects[mNumRegions-1].bottom = Math.min(b, rects[mNumRegions-1].bottom) ;
				break ;
				
			case UNIFORM_HORIZONTAL_CENTERED_AROUND:
				// center around centerY.
				total = Math.min( w, mNumRegions * mPreferredSize ) ;
				step = total / mNumRegions ;
				// start at centerX - half total, or 't', whichever
				// is lower on the screen (higher in value).
				start = Math.max( l, centerX - total/2 ) ;
				for ( int i = 0; i < mNumRegions; i++ ) {
					rects[i] = new Rect(start + i*step, t, start + (i+1)*step, b) ;
				}
				rects[mNumRegions-1].right = Math.min(r, rects[mNumRegions-1].right) ;
				break ;
				
			default:
				return null ;
			}
			
			return rects ;
		}
	}
	
	protected class SlideHandlerDelegate implements InvisibleControlsSlideHandler.Delegate {
		private WeakReference<SlideDelegate> mwrDelegate ;
		private SlideHandlerData mData ;
		
		protected SlideHandlerDelegate( InvisibleControlsSlideHandler handler, String buttonName, SlideRegionType regionType, int numRegions, int preferredSize ) {
			this( null, handler, buttonName, regionType, numRegions, preferredSize ) ;
		}
		
		protected SlideHandlerDelegate( SlideDelegate slideDelegate, InvisibleControlsSlideHandler handler, String buttonName, SlideRegionType regionType, int numRegions, int preferredSize ) {
			mwrDelegate = new WeakReference<SlideDelegate>(slideDelegate) ;
			mData = new SlideHandlerData( handler, buttonName, regionType, numRegions, preferredSize ) ;
			handler.setDelegate(this) ;
		}
		
		protected void setDelegate( SlideDelegate slideDelegate ) {
			mwrDelegate = new WeakReference<SlideDelegate>(slideDelegate) ;
		}
		
		protected SlideDelegate getDelegate() {
			return mwrDelegate.get() ;
		}
		
		protected SlideHandlerData getData() {
			return mData ;
		}
		
		protected void onLayoutControls( int l, int t, int r, int b ) {
			Rect buttonBounds = InvisibleControls.this.getButtonBoundsByName(mData.getButtonName()) ;
			Rect [] rects = mData.getRegions(l, t, r, b, buttonBounds.centerX(), buttonBounds.centerY()) ;
			if ( rects == null ) {
				mData.getHandler().setSlideRects(null) ;
				return ;
			}
			
			// use Integer objects as tags.
			Hashtable<Object, Rect> regions = new Hashtable<Object, Rect>() ;
			for ( int i = 0; i < rects.length; i++ ) {
				regions.put( Integer.valueOf(i), rects[i] ) ;
			}
			mData.getHandler().setSlideRects(regions) ;
		}
		
		protected Rect [] getRegions(int l, int t, int r, int b) {
			Rect buttonBounds = InvisibleControls.this.getButtonBoundsByName(mData.getButtonName()) ;
			return mData.getRegions(l, t, r, b, buttonBounds.centerX(), buttonBounds.centerY()) ;
		}
		
		@Override
		public void icsh_slideEntered(InvisibleControlsSlideHandler handler,
				Object tag) {
			
			if ( handler == mData.getHandler() ) {
				// tag is an integer.
				int num = ((Integer)tag).intValue() ;
				SlideDelegate sd = mwrDelegate.get() ;
				if ( sd != null ) {
					sd.invisibleControlsUserDidSlideIn(InvisibleControls.this, mData.getButtonName(), num) ;
				}
			}
		}
		@Override
		public void icsh_slideLeft(InvisibleControlsSlideHandler handler,
				Object tag) {
			
			if ( handler == mData.getHandler() ) {
				// tag is an integer.
				int num = ((Integer)tag).intValue() ;
				SlideDelegate sd = mwrDelegate.get() ;
				if ( sd != null ) {
					sd.invisibleControlsUserDidSlideOut(InvisibleControls.this, mData.getButtonName(), num) ;
				}
			}
		}
	}
	
	
	@SuppressWarnings("unused")
	private static final String TAG = "InvisibleControls.java" ;

	WeakReference<Delegate> mwrDelegate ;
	
	// Inflate from XML
	public InvisibleControls( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}

	public InvisibleControls( Context context, AttributeSet attrs, int defStyle ) {
		super(context, attrs, defStyle) ;
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	/**
	 * Has this view been prepared?
	 * @return
	 */
	public abstract boolean prepared() ;
	
	/**
	 * Prepares the (loaded) controls in whatever way is necessary, such as
	 * configuration, setting onClickListeners, etc.
	 */
	public abstract void prepare() ;
	
	
	/**
	 * Tells the Controls that it should immediately reposition
	 * its contents using measure() and layout().  The controls
	 * will use their current size and position; the purpose of this
	 * call is to repositon and resize contents.
	 * 
	 * Only call this method from the UI thread.
	 */
	public void remeasure() {
		this.measure(
				MeasureSpec.makeMeasureSpec(this.getWidth(), MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(this.getHeight(), MeasureSpec.EXACTLY)) ;
		
		this.layout(
				this.getLeft(),
				this.getTop(),
				this.getRight(),
				this.getBottom()) ;
	}
	
	public Delegate delegate() {
		return mwrDelegate.get() ;
	}
	
	public void setDelegate( Delegate del ) {
		mwrDelegate = new WeakReference<Delegate>(del) ;
	}
	
	/**
	 * Included as a reminder that you may want to override this method.
	 */
	public void setEnabled(boolean on) {
		super.setEnabled(on) ;
	}
	
	
	/**
	 * Selectively enable specific buttons.
	 * @param on
	 * @param gameButtonNames
	 */
	public abstract void setEnabled(boolean on, String [] gameButtonNames) ;
	
	/**
	 * Set the provided buttons to show (or not) when pressed.
	 * @param on
	 * @param buttonNames
	 */
	public abstract void setShowWhenPressed(boolean on, String [] buttonNames) ;
	
	/**
	 * Set the provided buttons to their default 'show when pressed' behavior.
	 * @param buttonNames
	 */
	public abstract void setShowWhenPressedDefault( String [] buttonNames ) ;
	
	
	public abstract void setTouchable( boolean on, String name ) ;
	
	/**
	 * Sets whether the specified buttons should be considered "touchable" - i.e.,
	 * whether there is a dedicated screen region for them.  The buttons are
	 * still considered enabled and button output may still occur if other input
	 * methods are possible, such as gestures.
	 * @param on
	 * @param gameButtonNames
	 */
	public abstract void setTouchable(boolean on, String [] gameButtonNames) ;
	
	
	/**
	 * Should the specified button use its alternative form (possibly changing
	 * icon and text)?
	 * 
	 * @param on
	 * @param name
	 */
	public abstract void setUseAlt( boolean on, String name ) ;
	
	
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
	 * @return Whether we will be resizing the touchable area.
	 */
	public abstract boolean setButtonVisibleBoundsByName( String name, Rect r ) ;
	
	
	/**
	 * Constructs and returns a new Rect bounding the button of the 
	 * specified name.
	 * @param name A name of a button
	 * @return A new Rect bounding the button; null if the name was not found
	 * 			For buttons which do not have a defined screen-area "sub-set,"
	 * 			this method returns null.
	 * 
	 * 			
	 */
	public abstract Rect getButtonBoundsByName( String name ) ;
	
	
	/**
	 * Constructs and returns a new Rect bounding the button of the 
	 * specified name, if the button is currently touchable.
	 * @param name A name of a button
	 * @return A new Rect bounding the button; null if the name was not found
	 * 			For buttons which do not have a defined screen-area "sub-set,"
	 * 			this method returns null.
	 * 
	 * 			
	 */
	public abstract Rect getTouchableButtonBoundsByName( String name ) ;
	
	
	/**
	 * Returns whether the specified button is currently being pressed.
	 * @param name A name of a button
	 * @return 
	 */
	public abstract boolean getButtonPressedByName( String name ) ;
	
	
	/**
	 * Sets the number of pixels per inch.  For certain controls
	 * types, this is used to help convert between pixel measurements
	 * and hand sizes / hand movements.
	 * 
	 * @param x
	 * @param y
	 */
	public abstract void setPixels( float x, float y, float xPerInch, float yPerInch ) ;
	
	
	/**
	 * 
	 * Sets, in pixels, the onscreen dimensions of vertical and horizontal
	 * movement.  This is useful (e.g.) for gesture controls, when "pulling"
	 * a piece in one direction or the other.
	 * 
	 * @param width
	 * @param height
	 */
	public abstract void setMovementGrid( int stepWidth, int stepHeight ) ;
	
	
	public abstract boolean removeSlideHandler( String buttonName ) ;
	
	public abstract void putSlideHandler( SlideDelegate slideDelegate, String buttonName, SlideRegionType regionType, int numRegions ) ;
	
	public abstract Rect [] getSlideRegionsByName( String buttonName ) ;
	
}
