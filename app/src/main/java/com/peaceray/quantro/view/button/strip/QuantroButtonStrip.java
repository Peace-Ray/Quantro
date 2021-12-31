package com.peaceray.quantro.view.button.strip;

import java.util.ArrayList;

import com.peaceray.quantro.R;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.QuantroButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.SupportsLongClickOracle;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;
import com.peaceray.quantro.view.button.popup.QuantroButtonPopup;
import com.peaceray.quantro.view.button.popup.QuantroButtonPopupWrapper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

/**
 * A horizontal strip of Quantro buttons.  Automatically positions
 * and sizes those component buttons which are currently displayed.
 * 
 * Terminology: has a main button and sub-buttons.  The main button is
 * positioned on the left; sub buttons are listed rightward.  All buttons
 * with Visibility not GONE are spaced horizontally according to settings;
 * GONE-visibility buttons are omitted completely, and the space they would
 * fill is used for other buttons.
 * 
 * Settings:
 * 		mainbuttonEqualHorizontalWeight: if true, all buttons are given equal weight for
 * 				spacing across the strip.  If false (default) then the
 * 				first button will spread to cover the remaining space after
 * 				all other buttons have been sized.  Usually this means
 * 				the first button is emphasized with a larger touchable
 * 				area; in extreme cases, however, the first button will
 * 				shrink to accommodate the rest.
 * 
 * 		horizontalSeparation: the horizontal space between buttons.  This represents
 * 				the true distance between button borders - in other words, it takes
 * 				into account the padding of the buttons.
 * 
 * 		mainbuttonHorizontalOffset: additional horizontal spacing between the
 * 				main button and the first sub button - if displayed.  This
 * 				value will be added to horizontalSeparation to determine the
 * 				distance between the main button and the next.
 * 
 * 		subbuttonBorderInset: the number of pixels which should be removed from the 
 * 				border width all sub-buttons.  The same number of pixels will be
 * 				added to the vertical padding of those buttons, so that the center of the
 * 				touchable area remains in a line with the main button's.  The horizontal
 * 				padding is unchanged.
 * 
 * 		subbuttonGradientInset: the number of pixels which should be removed from the gradient
 * 				height of all sub-buttons.  The same number of pixels will be added to
 * 				the bottom padding of those buttons.
 * 
 * 		subbuttonVerticalOffset: the number of pixels to add to the vertical position of
 * 				subbuttons, bringing them out-of-line from the main button.  Positive = down.
 * 		
 * 		mainbuttonContentMargin: four values, l,t,r,b, setting the margin for the main
 * 				button's content View.  Setting values will inset the content from the button
 * 				border.
 * 
 * 		subbuttonContentMargin: four values, l,t,r,b, setting the margin for the subbuttons
 * 				content Views.
 * 
 * 		subbuttonEqualHorizontalLength: forces subbuttons to share the same horizontal
 * 				length.  If false, horizontal buttons will resize themselves based on
 * 				content.
 * 
 * 		buttonBounds: if set, restricts button height and subbutton width to within these
 * 				bounds.  Compatible with subbuttonEqualHorizontalLength.  If
 * 				mainbuttonEqualHorizontalWeight is true, the main button will be restricted
 * 				to these horizontal bounds as well (if not, it is restricted only to
 * 				the vertical component).
 * 
 * 
 * 
 * @author Jake
 *
 */
public class QuantroButtonStrip extends ViewGroup implements View.OnClickListener, View.OnLongClickListener, SupportsLongClickOracle, QuantroButtonPopup.Listener {
	
	
	/**
	 * A QBS Controller is informed when buttons are pressed or held.
	 * Previously this role was filled by subclasses and Controllers
	 * independently setting themselves as onClickListeners, onLongClickListeners,
	 * and supportsLongClickOracles.
	 * 
	 * @author Jake
	 *
	 */
	public interface Controller {
		/**
		 * The specified button has been clicked.
		 * @param strip
		 * @param buttonNum
		 */
		public void qbsc_onClick( QuantroButtonStrip strip, int buttonNum, boolean wasOverflow ) ;
		
		/**
		 * The specified button has been long-pressed.
		 * @param strip
		 * @param buttonNum
		 * @return Should we activate as if the long click was successful?
		 */
		public boolean qbsc_onLongClick( QuantroButtonStrip strip, int buttonNum ) ;
		
		/**
		 * Should we animate this button as if long click is supported?
		 * @param strip
		 * @param buttonNum
		 * @return
		 */
		public boolean qbsc_supportsLongClick( QuantroButtonStrip strip, int buttonNum ) ;
		
		
		/**
		 * The dedicated "overflow" button has been touched.
		 * @return
		 */
		public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) ;
	}
	
	private static final String TAG = "QButtonStrip" ;
	
	
	/**
	 * Overflow is a behavior where first 'N' visible buttons are
	 * displayed (and sized to almost fill the strip), and any remaining
	 * visible buttons are represented in an Overflow menu instead, hidden
	 * by an "Overflow Button."
	 * 
	 * The Overflow Button is displayed as the final button in the strip.
	 * When touched, a PopupMenu appears below, giving the Titles of all
	 * overflowed buttons.
	 * @author Jake
	 *
	 */
	private enum OverflowStyle {
		/**
		 * No overflow is allowed.  Display all buttons included.
		 */
		NONE,
		
		/**
		 * Overflow is allowed, but only on supported devices:
		 * where a PopupMenu can be displayed.  API >= 11 (Android 3.0).
		 * On these devices the Overflow Button will be displayed (if needed).
		 * On others, all buttons will be displayed.
		 */
		ALLOW,
		
		/**
		 * Overflow is forced.  Identical to ALLOW on API >= 11.  On
		 * API < 11 (Android 2.3 or lower), overflow button will simply
		 * be omitted from the strip.
		 */
		FORCE
	}
	
	
	/**
	 * Trim is a visual detail drawn over the button border of the first button
	 * in the strip.  It is a thin white line with a drop shadow.  Trim is recommended
	 * for "header" strips, such as an action bar at the top of the screen, but
	 * generally not for other functional buttons.
	 * 
	 * @author Jake
	 *
	 */
	private enum TrimStyle {
		/**
		 * No trim for this strip.
		 */
		NONE,
		
		/**
		 * The trim is fully bounded and within the bottom border.
		 */
		BOUNDED,
		
		/**
		 * The trim is bounded on the right, but runs to the left edge
		 * of this button strip (possibly beyond the left edge of the
		 * main button).
		 */
		RUNS_LEFT,
		
		/**
		 * The trim is bounded on the left, but runs to the right edge
		 * of this button strip (possibly beyond the right edge of the
		 * main button).
		 */
		RUNS_RIGHT
	}
	
	protected int mButtonStyle ;
	protected OverflowStyle mOverflowStyle ;
	protected TrimStyle mTrimStyle ;
	
	// settin's
	protected int mButtonLayoutResourceID ;
	protected int mNumButtonsUponCreation ;
	protected boolean mMainbuttonEqualHorizontalWeight ;
	protected int mHorizontalSeparation ;
	protected int mMainbuttonHorizontalOffset ;
	protected int mButtonBorderWidth ;
	protected int mButtonBorderWidthBottom ;
	protected int mButtonBorderWidthTop ;
	protected int mButtonBorderWidthAlt ;
	protected int mButtonBorderWidthBottomAlt ;
	protected int mButtonBorderWidthTopAlt ;
	
	
	protected int mButtonGradientHeight ;
	protected int mSubbuttonBorderInset ;
	protected int mSubbuttonGradientInset ;
	protected int mSubbuttonVerticalOffset ;
	protected Rect mMainbuttonContentMargin ;
	protected Rect mSubbuttonContentMargin ;
	protected boolean mSubbuttonEqualHorizontalLength ;
	protected int mButtonBoundsMinWidth  ;
	protected int mButtonBoundsMaxWidth  ;
	protected int mButtonBoundsMinHeight  ;
	protected int mButtonBoundsMaxHeight  ;
	
	// trim!
	protected Drawable mTrim ;
	protected Rect mTempRect = new Rect() ;
	
	// Button Access!
	protected ArrayList<QuantroButtonAccess> mButtonAccess ;
	protected QuantroButton mOverflowButton ;
	protected int mMaxVisibleButtons ;	// includes 0, the main button
	
	// Buttons can be enabled/disabled, and shown/hidden.  We don't
	// necessarily show every visible button: we might have spillover.
	private boolean [] mButtonEnabled ;
	private boolean [] mButtonVisible ;
	private boolean [] mButtonOverflow ;
	
	protected Controller mController ;

	public QuantroButtonStrip(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public QuantroButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// require that all children are QuantroContentWrappingButtons.
		for ( int i = 0; i < getChildCount(); i++ ) {
			if ( !( getChildAt(i) instanceof QuantroContentWrappingButton ) )
				throw new RuntimeException("A QuantroButtonString may contain only QuantroContentWrappingButtons!") ;
		}
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
		
		
	}
	
	public QuantroButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		// require that all children are QuantroContentWrappingButtons.
		for ( int i = 0; i < getChildCount(); i++ ) {
			if ( !( getChildAt(i) instanceof QuantroContentWrappingButton ) )
				throw new RuntimeException("A QuantroButtonString may contain only QuantroContentWrappingButtons!") ;
		}
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues(Context context) {
		mOverflowStyle = OverflowStyle.NONE ;
		mTrimStyle = TrimStyle.NONE ;
		
		mButtonLayoutResourceID = 0 ;
		mNumButtonsUponCreation = 0 ;
		mMainbuttonEqualHorizontalWeight = false ;
		mHorizontalSeparation = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)12, getResources().getDisplayMetrics()) ;
		mMainbuttonHorizontalOffset = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)1, getResources().getDisplayMetrics()) ;
		mButtonBorderWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)4, getResources().getDisplayMetrics()) ;
		mButtonBorderWidthBottom = -1 ;
		mButtonBorderWidthTop = -1 ;
		mButtonBorderWidthAlt = -1 ;
		mButtonBorderWidthBottomAlt = -1 ;
		mButtonBorderWidthTopAlt = -1 ;
		mButtonGradientHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)10, getResources().getDisplayMetrics()) ;
		mSubbuttonBorderInset = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)1, getResources().getDisplayMetrics()) ;
		mSubbuttonGradientInset = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)0, getResources().getDisplayMetrics()) ;
		mSubbuttonVerticalOffset = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)0, getResources().getDisplayMetrics()) ;
		mMainbuttonContentMargin = new Rect(0,0,0,0) ;
		mSubbuttonContentMargin = new Rect(0,0,0,0) ;
		mSubbuttonEqualHorizontalLength = true ;
		mButtonBoundsMinWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)50, getResources().getDisplayMetrics()) ;
		mButtonBoundsMaxWidth = Integer.MAX_VALUE ;
		mButtonBoundsMinHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)50, getResources().getDisplayMetrics()) ;
		mButtonBoundsMaxHeight = Integer.MAX_VALUE ;
		
		setWillNotDraw(false) ;
		
		mOverflowButton = null ;
		
		// max visible buttons based on screen size
		mMaxVisibleButtons = Integer.MAX_VALUE ;
		switch( VersionSafe.getScreenSizeCategory(context) ) {
		case VersionSafe.SCREEN_SIZE_SMALL:
		case VersionSafe.SCREEN_SIZE_NORMAL:
			mMaxVisibleButtons = 3 ;
			break ;
		case VersionSafe.SCREEN_SIZE_LARGE:
			mMaxVisibleButtons = 4 ;
			break ;
		case VersionSafe.SCREEN_SIZE_XLARGE:
			mMaxVisibleButtons = 5 ;
		}
		// should be = to the number of small extra buttons, plus 1
		// for the main button.
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.QuantroButtonStrip);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			// Some dimension measures are sizes - they must be at least 0.
			// Some are offsets - positive and negative values are acceptable.
			// Different a.getDimensionPixel* calls are used in these two cases.
			case R.styleable.QuantroButtonStrip_button_layout:
				mButtonLayoutResourceID = a.getResourceId(attr, mButtonLayoutResourceID) ;
				break ;
			case R.styleable.QuantroButtonStrip_num_buttons:
				mNumButtonsUponCreation = a.getInt(attr, mNumButtonsUponCreation) ;
				break ;
			case R.styleable.QuantroButtonStrip_overflow:
				int overflow = a.getInt(attr, 0) ;
				switch ( overflow ) {
				case 0:
					mOverflowStyle = OverflowStyle.NONE ;
					break ;
				case 1:
					mOverflowStyle = OverflowStyle.ALLOW ;
					break ;
				case 2:
					mOverflowStyle = OverflowStyle.FORCE ;
					break ;
				}
				break ;
			case R.styleable.QuantroButtonStrip_main_button_equal_horizontal_weight:
				mMainbuttonEqualHorizontalWeight = a.getBoolean(attr, mMainbuttonEqualHorizontalWeight) ;
				break ;
			case R.styleable.QuantroButtonStrip_trim:
				int trim = a.getInteger(attr, 0) ;
				switch ( trim ) {
				case 0:
					mTrimStyle = TrimStyle.NONE ;
					break ;
				case 1:
					mTrimStyle = TrimStyle.BOUNDED ;
					break ;
				case 2:
					mTrimStyle = TrimStyle.RUNS_LEFT ;
					break ;
				case 3:
					mTrimStyle = TrimStyle.RUNS_RIGHT ;
					break ;
				}
				break ;
			case R.styleable.QuantroButtonStrip_button_border_width:
				mButtonBorderWidth = a.getDimensionPixelSize(attr, mButtonBorderWidth) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_border_width_bottom:
				mButtonBorderWidthBottom = a.getDimensionPixelSize(attr, mButtonBorderWidth) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_border_width_top:
				mButtonBorderWidthTop = a.getDimensionPixelSize(attr, mButtonBorderWidthTop) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_gradient_height:
				mButtonGradientHeight = a.getDimensionPixelSize(attr, mButtonGradientHeight) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_horizontal_separation:
				mHorizontalSeparation = a.getDimensionPixelSize(attr, mHorizontalSeparation) ;
				break ;
			case R.styleable.QuantroButtonStrip_main_button_horizontal_offset:
				mMainbuttonHorizontalOffset = a.getDimensionPixelOffset(attr, mMainbuttonHorizontalOffset) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_border_inset:
				mSubbuttonBorderInset = a.getDimensionPixelOffset(attr, mSubbuttonBorderInset) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_gradient_inset:
				mSubbuttonGradientInset = a.getDimensionPixelOffset(attr, mSubbuttonGradientInset) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_vertical_offset:
				mSubbuttonVerticalOffset = a.getDimensionPixelOffset(attr, mSubbuttonVerticalOffset) ;
				break ;
			case R.styleable.QuantroButtonStrip_main_button_content_margin_left:
				mMainbuttonContentMargin.left = a.getDimensionPixelSize(attr, mMainbuttonContentMargin.left) ;
				break ;
			case R.styleable.QuantroButtonStrip_main_button_content_margin_top:
				mMainbuttonContentMargin.top = a.getDimensionPixelSize(attr, mMainbuttonContentMargin.top) ;
				break ;
			case R.styleable.QuantroButtonStrip_main_button_content_margin_right:
				mMainbuttonContentMargin.right = a.getDimensionPixelSize(attr, mMainbuttonContentMargin.right) ;
				break ;
			case R.styleable.QuantroButtonStrip_main_button_content_margin_bottom:
				mMainbuttonContentMargin.bottom = a.getDimensionPixelSize(attr, mMainbuttonContentMargin.bottom) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_content_margin_left:
				mSubbuttonContentMargin.left = a.getDimensionPixelSize(attr, mSubbuttonContentMargin.left) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_content_margin_top:
				mSubbuttonContentMargin.top = a.getDimensionPixelSize(attr, mSubbuttonContentMargin.top) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_content_margin_right:
				mSubbuttonContentMargin.right = a.getDimensionPixelSize(attr, mSubbuttonContentMargin.right) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_content_margin_bottom:
				mSubbuttonContentMargin.bottom = a.getDimensionPixelSize(attr, mSubbuttonContentMargin.bottom) ;
				break ;
			case R.styleable.QuantroButtonStrip_sub_button_equal_width:
				mSubbuttonEqualHorizontalLength = a.getBoolean(attr, mSubbuttonEqualHorizontalLength) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_bounds_width_min:
				mButtonBoundsMinWidth = a.getDimensionPixelSize(attr, mButtonBoundsMinWidth) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_bounds_width_max:
				mButtonBoundsMaxWidth = a.getDimensionPixelSize(attr, mButtonBoundsMaxWidth) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_bounds_height_min:
				mButtonBoundsMinHeight = a.getDimensionPixelSize(attr, mButtonBoundsMinHeight) ;
				break ;
			case R.styleable.QuantroButtonStrip_button_bounds_height_max:
				mButtonBoundsMaxHeight = a.getDimensionPixelSize(attr, mButtonBoundsMaxHeight) ;
				break ;
			}
		}
		
		// recycle the array; don't need it anymore.
		a.recycle() ;
	}
	
	synchronized private void constructor_allocateAndInitialize(Context context) {
		// Try loading children.
		if ( mNumButtonsUponCreation > 20 )
			throw new IllegalArgumentException("Um, why you trying to inflate that many buttons?") ;
		while ( mNumButtonsUponCreation > getChildCount() ) {
			 // Log.d(TAG, "adding button") ;
			 addButton() ;
		}
		// Primarily, our purpose here is to set all our child parameters according
		// to our own settings.  For instance, if we have a content margin we
		// need the buttons to follow, we need to set their own margins.
		for ( int i = 0; i < getChildCount(); i++ ) {
			QuantroContentWrappingButton qcwb = (QuantroContentWrappingButton)getChildAt(i) ;
			if ( i == 0 )
				setParamsForMainbutton(qcwb) ;
			else
				setParamsForSubbutton(qcwb) ;
		}
		
		switch( mTrimStyle ) {
		case NONE:
			mTrim = null ;
			break ;
		case BOUNDED:
			mTrim = context.getResources().getDrawable(R.drawable.rect_trim_horiz) ;
			break ;
		case RUNS_LEFT:
			mTrim = context.getResources().getDrawable(R.drawable.rect_trim_horiz_runs_left) ;
			break ;
		case RUNS_RIGHT:
			mTrim = context.getResources().getDrawable(R.drawable.rect_trim_horiz_runs_right) ;
			break ;
		}
		
		mButtonAccess = new ArrayList<QuantroButtonAccess>() ;
	}
	
	
	/**
	 * Inflation is finished.
	 */
	protected void onFinishInflate () {
		super.onFinishInflate() ;
		
		// set ourselves as the onClick, onLongClick, and supportsLongClick
		// Listener for all buttons.
		for ( int i = 0; i < this.getChildCount(); i++ ) {
			QuantroContentWrappingButton qcwb = (QuantroContentWrappingButton)getChildAt(i) ;

			qcwb.setOnClickListener(this) ;
			qcwb.setOnLongClickListener(this) ;
			qcwb.setSupportsLongClickOracle(this) ;
			mButtonAccess.add( QuantroButtonDirectAccess.wrap(qcwb) ) ;
		}
		
		mButtonEnabled = new boolean[getButtonCount()] ;
		mButtonVisible = new boolean[getButtonCount()] ;
		mButtonOverflow = new boolean[getButtonCount()] ;
		for ( int i = 0; i < getButtonCount(); i++ ) {
			mButtonEnabled[i] = getButton(i).isEnabled() ;
			mButtonVisible[i] = getButton(i).getVisibility() == View.VISIBLE ;
			mButtonOverflow[i] = false ;
		}
		
		// Overflow?
		if ( getButtonCount() > mMaxVisibleButtons && mOverflowStyle != OverflowStyle.NONE ) {
			addOverflowButtonIfPossible() ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// BUTTON ACCESS AND MANIPULATION
	// Useful for direct control over what is displayed, or use by sub-classes
	// to configure content.
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Sets the Controller for this button strip.  The controller will receive
	 * all button presses.
	 */
	synchronized public void setController( Controller controller ) {
		mController = controller ;
	}
	
	
	/**
	 *  Sometimes our content is based on data we can read from some other source.
	 *  This method will prompt that action - override it if you do this, and
	 *  be sure to postInvalidate.
	 */
	synchronized public void refresh() {
		// make changes to 'enabled' and 'visible'.
		int numShown = 0 ;
		int numOverflow = 0 ;
		for ( int i = 0; i < getButtonCount(); i++ ) {
			QuantroContentWrappingButton qcwb = getButton(i) ;
			boolean showButton = mButtonVisible[i] ;
			if ( numShown >= mMaxVisibleButtons ) {
				switch( mOverflowStyle ) {
				case NONE:
					break ;
				case ALLOW:
					showButton = mOverflowButton != null ;
					break ;
				case FORCE:
					showButton = false ;
				}
			}
			if ( showButton ) {
				// Log.d(TAG, "refresh button " + i + ": Visible") ;
				qcwb.setVisibility( View.VISIBLE ) ;
				qcwb.setEnabled( mButtonEnabled[i] ) ;
				qcwb.setClickable( mButtonEnabled[i] ) ;
				numShown++ ;
				mButtonOverflow[i] = false ;
			} else {
				// Log.d(TAG, "refresh button " + i + ": Overflow") ;
				qcwb.setVisibility( View.GONE ) ;
				qcwb.setEnabled( mButtonEnabled[i] ) ;
				qcwb.setClickable( mButtonEnabled[i] ) ;
				mButtonOverflow[i] = mButtonVisible[i] ;
				if ( mButtonVisible[i] )
					numOverflow++ ;
			}
		}
		
		if ( mOverflowButton != null ) {
			if ( numOverflow > 0 ) {
				// resize refresh_temp arrays
				if ( refresh_tempCharSequence == null || refresh_tempCharSequence.length != numOverflow ) {
					refresh_tempCharSequence = new CharSequence[numOverflow] ;
					refresh_tempTag = new Integer[numOverflow] ;
				}
				int j = 0 ;
				for ( int i = 0; i < getButtonCount(); i++ ) {
					if ( mButtonOverflow[i] ) {
						refresh_tempCharSequence[j] = getButtonAccess(i).getTitle() ;
						refresh_tempTag[j] = Integer.valueOf(i) ;
						j++ ;
					}
				}
				// configure the overflow menu according to mButtonOverflow.
				mOverflowButton.setVisibility(View.VISIBLE) ;
				QuantroButtonPopupWrapper wrapper = QuantroButtonPopup.wrap(mOverflowButton, this, 0, refresh_tempCharSequence, refresh_tempTag) ;
				for ( int i = 0; i < getButtonCount(); i++ ) {
					if ( mButtonOverflow[i] ) {
						wrapper.setEnabled(Integer.valueOf(i), mButtonEnabled[i]) ;
					}
				}
			} else {
				mOverflowButton.setVisibility(View.GONE) ;
			}
		}
		
		forceLayout() ;
	}
	
	private CharSequence [] refresh_tempCharSequence = null ;
	private Integer [] refresh_tempTag = null ;
	
	synchronized public int getButtonCount() {
		return getChildCount() - (mOverflowButton != null ? 1 : 0) ;
	}
	
	/**
	 * Returns the number of buttons currently not "Gone".
	 * @return
	 */
	synchronized public int getVisibleButtonCount() {
		int numVis = 0 ;
		for ( int i = 0; i < getButtonCount(); i++ )
			if ( getButtonIsVisible(i) )
				numVis++ ;
		return numVis ;
	}
	
	synchronized public int getOverflowButtonCount() {
		int numPreferredOverflow = Math.max(0, getVisibleButtonCount() - mMaxVisibleButtons) ;
		switch( mOverflowStyle ) {
		case NONE:
			return 0 ;
		case ALLOW:
			return mOverflowButton == null ? 0 : numPreferredOverflow ;
		case FORCE:
			return numPreferredOverflow ;
		}
		
		throw new IllegalStateException("mOverflowStyle is set to 'null'!") ;
	}
	
	synchronized private QuantroContentWrappingButton getButton(int index) {
		return (QuantroContentWrappingButton)getChildAt(index) ;
	}
	
	synchronized protected QuantroContentWrappingButton getButtonDirectReference( int index ) {
		return getButton(index) ;
	}
	
	synchronized public QuantroButtonAccess getButtonAccess(int index) {
		return mButtonAccess.get(index) ;
	}
	
	synchronized public QuantroButtonAccess getMainButtonAccess() {
		return mButtonAccess.get(0) ;
	}
	
	synchronized public void resetAsListener(int index) {
		QuantroContentWrappingButton button = getButtonDirectReference(index) ;
		button.setOnClickListener(this) ;
		button.setOnLongClickListener(this) ;
		button.setSupportsLongClickOracle(this) ;
	}
	
	synchronized protected final void setButtonVisible( int index, boolean visible ) {
		// set visibility later, on 'refresh'.
		mButtonVisible[index] = visible ;
	}
	
	synchronized public boolean getButtonIsVisible( int index ) {
		return mButtonVisible[index] ;
	}
	
	synchronized protected boolean getButtonViewIsVisible( int index ) {
		return getButtonDirectReference(index).getVisibility() == View.VISIBLE ;
	}

	synchronized protected boolean getButtonViewVisibilityIsSet( int index ) {
		return getButtonIsVisible(index) == getButtonViewIsVisible( index ) ;
	}
	
	synchronized protected final void setButtonEnabled( int index, boolean enabled ) {
		// set enabled later, on 'refresh'.
		mButtonEnabled[index] = enabled ;
	}
	
	synchronized public boolean getButtonIsEnabled( int index ) {
		return mButtonEnabled[index] ;
	}
	
	synchronized protected boolean getButtonViewIsEnabled( int index ) {
		return getButtonDirectReference(index).isEnabled() ;
	}
	
	synchronized protected boolean getButtonViewEnabledIsSet( int index ) {
		return getButtonIsEnabled(index) == getButtonViewIsEnabled( index ) ;
	}
	
	
	/**
	 * Attempts to add an Overflow button, if such a button does
	 * not already exist and we support them.
	 * 
	 * Returns whether an overflow button now exists and mOverflowButton
	 * is a reference to it.
	 * @return
	 */
	synchronized private boolean addOverflowButtonIfPossible() {
		if ( mOverflowButton != null )
			return true ;
		if ( mOverflowStyle == OverflowStyle.NONE )
			return false ;
		if ( !VersionCapabilities.supportsPopupMenu() )
			return false ;
		
		View v = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.quantro_overflow_button,this,false);
		addView(v) ;		// add at the end
		mOverflowButton = (QuantroButton) v ;
		return true ;
	}
	
	
	/**
	 * Inflates our button resource into a new button, adding it as a child
	 * at the end of the strip.  Throws an exception if the resource is
	 * not found.
	 */
	synchronized public void addButton() {
		addButton(mButtonLayoutResourceID) ;
	}
	
	synchronized private void addButton( int resID ) {
		View v = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(resID,this,false);
		addView(v, getButtonCount()) ;
		v.setOnClickListener(this) ;
		v.setOnLongClickListener(this) ;
		QuantroContentWrappingButton qcwb = (QuantroContentWrappingButton)v ;
		qcwb.setSupportsLongClickOracle(this) ;
		mButtonAccess.add( QuantroButtonDirectAccess.wrap(qcwb) ) ;
		

		int num = getButtonCount() ;
		
		boolean [] enabled = new boolean[num] ;
		boolean [] visible = new boolean[num] ;
		boolean [] overflow = new boolean[num] ;
		for ( int i = 0; i < num-1; i++ ) {
			enabled[i] = mButtonEnabled[i] ;
			visible[i] = mButtonVisible[i] ;
			overflow[i] = mButtonOverflow[i] ;
		}
		
		enabled[num-1] = qcwb.isEnabled() ;
		visible[num-1] = qcwb.getVisibility() == View.VISIBLE ;
		overflow[num-1] = num > 1 ? overflow[num-2] : false ;
		
		mButtonEnabled = enabled ;
		mButtonVisible = visible ;
		mButtonOverflow = overflow ;
		
		if ( getButtonCount() > mMaxVisibleButtons && mOverflowStyle != OverflowStyle.NONE ) {
			addOverflowButtonIfPossible() ;
		}
		
		// Log.d(TAG, "addButton: now has " + num + " buttons ") ;
	}
	
	synchronized public int getIdealHeight() {
		// TODO: include mSubbuttonVerticalOffset.
		int h = 0 ;
		for ( int i = 0; i < getButtonCount(); i++ ) {
			QuantroContentWrappingButton v = getButton(i) ;
			if ( v.getVisibility() != View.GONE ) {
				int buttonHeight = v.getIdealHeight() ;
				buttonHeight = Math.max(this.mButtonBoundsMinHeight,
						Math.min(this.mButtonBoundsMaxHeight, buttonHeight)) ;
				h = Math.max( h, buttonHeight + getPaddingTop() + getPaddingBottom() ) ;
			}
		}
		return h ;
	}
	
	@Override
	synchronized public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec) ;
		int heightSize = MeasureSpec.getSize(heightMeasureSpec) ;
		int widthMode = MeasureSpec.getMode(widthMeasureSpec) ;
		int widthSize = MeasureSpec.getSize(widthMeasureSpec) ;
		
		// Log.d(TAG, "******* onMeasure.  " + widthMode + ":" + widthSize + ", " + heightMode + ":" + heightSize) ;
		
		// Our vertical measurement is simple enough.  We have button bounds,
		// padding, and the measurement of our children, plus the heightMeasureSpec.
		//
		// First: get the idealized height of each button, if they had our entire
		// space to fill.
		
		int childWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode == MeasureSpec.UNSPECIFIED ? MeasureSpec.UNSPECIFIED : MeasureSpec.AT_MOST) ;
		int childHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode == MeasureSpec.UNSPECIFIED ? MeasureSpec.UNSPECIFIED : MeasureSpec.AT_MOST) ;
		
		
		for ( int i = 0; i < getChildCount(); i++ ) {
			// Log.d(TAG, "onMeasure: measuring child " + i + " to ideal sizes") ;
			getChildAt(i).measure(childWidthSpec, childHeightSpec) ;
		}
		//Log.d(TAG, "calling super.onMeasure") ;
		//super.onMeasure(widthMeasureSpec, heightMeasureSpec) ;
		if ( getChildCount() == 0 )
			return ;
		
		
		int maxButtonHeight = 0 ;
		int numButtonsNotGone = 0 ;
		for ( int i = 0; i < getChildCount(); i++ ) {
			View v = getChildAt(i) ;
			if ( v.getVisibility() != View.GONE ) {
				maxButtonHeight = Math.max(maxButtonHeight, v.getMeasuredHeight()) ;
				if ( v != this.mOverflowButton )
					numButtonsNotGone++ ;
			}
		}
		if ( numButtonsNotGone == 0 )
			return ;
		
		// Next: truncate the height to our button bounds.
		maxButtonHeight = Math.max(
				mButtonBoundsMinHeight, 
				Math.min(mButtonBoundsMaxHeight, maxButtonHeight)) ;
		// Log.d(TAG, "onMeasure: maxButtonHeight initialized to " + maxButtonHeight) ;
		// Note: if mSubbuttonVerticalOffset is not 0, we may need more information
		// then max height.
		// TODO: Include mButtonVerticalOffset.
		int idealHeight ;
		// If we wrap content, apply our padding.  Otherwise (probably FILL_PARENT)
		// take the max height.
		ViewGroup.LayoutParams layoutParams = this.getLayoutParams() ;
		if ( layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT )
			idealHeight = maxButtonHeight + getPaddingTop() + getPaddingBottom() ;
		else
			idealHeight = Integer.MAX_VALUE ;
		// Finally, apply the restrictions from the parent.
		int height ;
		if ( heightMode == View.MeasureSpec.AT_MOST )
			height = Math.min(idealHeight, heightSize) ;
		else if ( heightMode == View.MeasureSpec.EXACTLY )
			height = heightSize ;
		else
			height = idealHeight ;
		/// HEIGHT IS SET
		
		// Our horizontal measurement is comparatively tough.  We have the ideal
		// width of each button (their horizontal padding is set to zero).
		// However, our settings might require that buttons be place within
		// certain bounds, or that they be sized identically, or whatever.
		// First: we have the idealized width of each button, so collect their
		// widths truncated to within bounds.  Keep the main button separate
		// (it may have slightly different settings).
		// 
		boolean mainIsVisible = getChildCount() > 0 && getChildAt(0).getVisibility() != View.GONE ;
		int [] visibleWidths = new int[numButtonsNotGone] ;
		int index = 0 ;
		for ( int i = 0; i < getButtonCount(); i++ ) {
			View v = getButton(i) ;
			if ( v.getVisibility() != View.GONE ) {
				visibleWidths[index] = Math.max(
						mButtonBoundsMinWidth,
						Math.min(mButtonBoundsMaxWidth, v.getMeasuredWidth())) ;
				index++ ;
			}
		}
		// visibleWidths now holds ideal "truncated width" of every visible button
		// -- except the overflow button, if we have one.
		// We have each button set to WRAP_CONTENT, so this length is the minimum
		// possible width that displays the content without difficulty.
		// If mSubbuttonEqualHorizontalLength is true, set each value to the
		// "max of mins" among subbuttons; if mMainbuttonEqualHorizontalWeight,
		// apply to the main button as well.
		int maxOfMins = 0 ;
		
		for ( int i = 0; i < visibleWidths.length; i++ ) {
			// Include all subbuttons, and the main button if it has equal weight.
			if ( i == 0 ) {
				if ( !mainIsVisible || mMainbuttonEqualHorizontalWeight )
					maxOfMins = Math.max(maxOfMins, visibleWidths[i]) ;
			}
			else
				maxOfMins = Math.max(maxOfMins, visibleWidths[i]) ;
		}
		// Apply to all!
		if ( mSubbuttonEqualHorizontalLength ) {
			for ( int i = 0; i < visibleWidths.length; i++ ) {
				// Include all subbuttons, and the main button if it has equal weight.
				if ( i == 0 ) {
					if ( !mainIsVisible || mMainbuttonEqualHorizontalWeight )
						visibleWidths[i] = maxOfMins ;
				}
				else
					visibleWidths[i] = maxOfMins ;
			}
		}
		// visibleWidths now gives the horizontal space each button wants.
		// Find the difference between button width IF DISPLAYED AS THEY ARE
		// and the width of this strip.  To do that, find the "ideal strip width."
		// First: put horizontal spacing between items.
		int stripWidth = (numButtonsNotGone -1) * mHorizontalSeparation ;
		if ( mainIsVisible )
			stripWidth += mMainbuttonHorizontalOffset ;
		// Next: include the visibleWidth of every button.  Yes, including the
		// main button.  We get our initial estimate using the size the
		// main button "wants to be," and if we need to resize later, we can
		// according to mMainbuttonEqualHorizontalWeight.
		for ( int i = 0; i < visibleWidths.length; i++ )
			stripWidth += visibleWidths[i] ;
		// Include overflow
		if ( mOverflowButton != null && mOverflowButton.getVisibility() == View.VISIBLE )
			stripWidth += mOverflowButton.getMeasuredWidth() + mHorizontalSeparation ;
		// Include the padding.
		stripWidth += getPaddingLeft() + getPaddingRight() ;
		int idealWidth ;
		if ( layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT )
			idealWidth = stripWidth ;
		else
			idealWidth = Integer.MAX_VALUE ;
		// Finally, apply the restrictions from the parent.
		int width ;
		if ( widthMode == View.MeasureSpec.AT_MOST ) {
			width = Math.min(idealWidth, widthSize) ;
		}
		else if ( widthMode == View.MeasureSpec.EXACTLY ) {
			width = widthSize ;
		}
		else {
			width = idealWidth ;
		}
		/// WIDTH IS SET
		
		// Congratulations, we now know the dimensions of this Strip!
		// However, we still need to determine the button sizes.  If
		// stripWidth (all buttons, spacing, and padding) differs from
		// width (true width of this view), we need to resize the buttons
		// to compensate.  We print a debug message if impossible.
		int roomToGrow = width - stripWidth ;
		// roomToGrow is the "spare pixels" which we must fill by expanding
		// buttons.  if mMainbuttonEqualHorizontalWeight is false and the
		// main button is not GONE, we grow (or shrink, if negative) the main
		// button by this amount.  Otherwise, add or subtract pixels from the
		// outliers until we match the strip width (within the limits of
		// button bounds).  If we successfully resize, note the new measures
		// of the buttons and give them with 'measure' using exact measures.
		// We worry about laying them out later (i.e., figuring out horizontal
		// and vertical spacing).
		if ( !mMainbuttonEqualHorizontalWeight && mainIsVisible ) {
			// Yay!  Reize the main button!
			visibleWidths[0] += roomToGrow ;
		}
		else {
			// mrrrrh... we need to resize ALL visible buttons (including the
			// main button, if it appears.
			// Do a "tall poppy" approach; identify the outlying button(s), and
			// cut (or grow) them to the next size up.
			boolean [] outlier = new boolean[visibleWidths.length] ;
			//Log.d(TAG, "entering roomToGrow loop") ;
			while ( roomToGrow != 0 ) {
				//Log.d(TAG, "roomToGrow is " + roomToGrow) ;
				// identify current outliers...
				int outlyingValue = visibleWidths[0] ;
				int nextmostOutlyingValue = outlyingValue ;
				for ( int i = 1; i < visibleWidths.length; i++ ) {
					if ( roomToGrow > 0 && visibleWidths[i] < outlyingValue
							|| roomToGrow < 0 && visibleWidths[i] > outlyingValue ) {
						nextmostOutlyingValue = outlyingValue ;
						outlyingValue = visibleWidths[i] ;
					}
				}
				int numOutliers =0 ;
				for ( int i = 0; i < visibleWidths.length; i++ ) {
					if ( visibleWidths[i] == outlyingValue ) {
						outlier[i] = true ;
						numOutliers++ ;
					}
					else
						outlier[i] = false ;
				}
				//Log.d(TAG, "there are " + numOutliers + " outliers of " + visibleWidths.length + " visible buttons.") ;
				//Log.d(TAG, "the most outlying value is " + outlyingValue + ", with runner-up " + nextmostOutlyingValue) ;
				
				
				int pixelDiff = (nextmostOutlyingValue - outlyingValue) * numOutliers ;
				// pixelDiff is the additive difference between width and the width
				// after the outliers are adjusted to the next outlying.
				// If positive, they are growing; if negative, they are shrinking.
				if ( Math.abs(pixelDiff) <= Math.abs(roomToGrow) && outlyingValue != nextmostOutlyingValue ) {
					// Adjust all outliers to exactly nextmostOutlyingValue.
					for ( int i = 0; i < outlier.length; i++ ) {
						if ( outlier[i] )
							visibleWidths[i] = nextmostOutlyingValue ;
					}
					roomToGrow -= pixelDiff ;
					//Log.d(TAG, "set outliers to " + nextmostOutlyingValue) ;
				}
				else {
					// Either adjusting to the nextmost will overshoot our goal,
					// or there is no 'nextmost' outlying value to adjust to.
					// Adjust outliers to fill the remaining space instead.
					pixelDiff = roomToGrow ;
					int adjustment = pixelDiff / numOutliers ;
					int remainder = Math.abs(pixelDiff) % numOutliers ;
					remainder *= adjustment > 0 ? 1 : -1 ;
					// Apply the adjustment to all, then portion out 1 pixel each of the remainder.
					for ( int i = 0; i < outlier.length; i++ ) {
						if ( outlier[i] ) {
							visibleWidths[i] += adjustment ;
							if ( remainder > 0 ) {
								visibleWidths[i] += remainder ;
								remainder += remainder > 0 ? -1 : 1 ;
							}
						}
					}
					roomToGrow = 0 ;
					
					//Log.d(TAG, "adjusted outliers by " + adjustment +"; finished") ;
				}
			}
			//Log.d(TAG, "leaving roomToGrow loop") ;
		}
		
		// visibleWidths now specifies the width we have portioned out for each button.
		// Log an error if there is any violation - if any is negative, or outside our
		// button bounds.
		for ( int i = 0; i < visibleWidths.length; i++ ) {
			if ( visibleWidths[i] <= 0 )
				Log.e(TAG, "onMeasure: button " + i + " given non-positive width " + visibleWidths[i]) ;
			if ( !mainIsVisible || i > 0 || mMainbuttonEqualHorizontalWeight ) {
				// check bounds
				if ( visibleWidths[i] < mButtonBoundsMinWidth )
					Log.e(TAG, "onMeasure: button " + i + " given width " + visibleWidths[i] + " < minimum " + mButtonBoundsMinWidth ) ;
				if ( visibleWidths[i] > mButtonBoundsMaxWidth )
					Log.e(TAG, "onMeasure: button " + i + " given width " + visibleWidths[i] + " > maximum " + mButtonBoundsMaxWidth ) ;
			}
		}
		
		// Okay.  We have calculated visible Widths.  Get visible Heights: take 'height',
		// the true height of this view, and remove our padding to get the space available
		// for the button view.  Ignore the "desired height" of the button (we factored
		// it in earlier when calculating this view's height, but now we take priority).
		// NOTE: For 'border inset' subbuttons, we have compensated
		// by adding vertical padding, so we don't need to include inset right now.
		// TODO: Include vertical offset for subbuttons in our calculations.  Requires
		// implementing the 'todo' above as well.
		int visibleHeight = height - getPaddingTop() - getPaddingBottom() ;
		int visIndex = 0 ;
		for ( int i = 0; i < getButtonCount(); i++ ) {
			// Log.d(TAG, "onMeasure: measuring child " + i + " to exact sizes") ;
			View v = getButton(i) ;
			if ( v.getVisibility() != View.GONE ) {
				v.measure(
						View.MeasureSpec.makeMeasureSpec(visibleWidths[visIndex], View.MeasureSpec.EXACTLY),
						View.MeasureSpec.makeMeasureSpec(visibleHeight, View.MeasureSpec.EXACTLY) ) ;
				visIndex++ ;
			}
			else {
				v.measure(
						View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY),
						View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.EXACTLY) ) ;
			}
		}
		
		// That's it.  Set our height/width.
		this.setMeasuredDimension(width, height) ;
	}
	
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// Log.d(TAG, "" + this + " onLayout: " + changed + ", " + l + ", " + t + ", " + r + ", " + b) ;
		
		// We have measured the visible children.  Place them out relative to ourself.
		int xEdge = getPaddingLeft() ;
		// the leftmost edge of the "next" button placed.
		// All buttons have the same top and bottom - we use their own padding to handle insets
		// TODO: Reconsider when we add vertical offset.
		for ( int i = 0; i < getChildCount(); i++ ) {
			View v = getChildAt(i) ;
			if ( v.getVisibility() != View.GONE ) {
				v.layout(xEdge, getPaddingTop(), xEdge + v.getMeasuredWidth(), (b-t)-getPaddingBottom()) ;
				// Adjust xEdge to include button length and the space to the next button.
				xEdge += v.getMeasuredWidth() ;
				if ( i == 0 )
					xEdge += mMainbuttonHorizontalOffset ;
				xEdge += mHorizontalSeparation ;
			}
		}
		
		// Done.  That was easy!  Because we ignored "vertical offset"...
		
		// BUG FIX: On JellyBean, ImageViews have difficulty displaying resizable
		// content if that content is set before they are laid-out.  This is 
		// the case for most subclasses.  Therefore, we 'refresh' now if our
		// layout changed and we might encounter this bug.
		if ( changed && VersionCapabilities.hasDrawableContentImageViewScaleBug() ) {
			// Log.d(TAG, "JB bug - forcing a refresh") ;
			refresh() ;
		}
		
		// configure draw shadows.  We generate a new drop shadow for each view,
		// EXCEPT IF:
		// 1. the view is GONE
		// 2. the shadow currently fits the view
		// 3. the view was PREVIOUSLY >= 20 pixels in both dimensions, and still is.
		// in some of those cases, we change nothing; in others, we resize the current view.
		for ( int i = 0; i < getChildCount(); i++ ) {
			View v = getChildAt(i) ;
			if ( v.getVisibility() == View.GONE )
				continue ;
			
			QuantroContentWrappingButton qcwb = (QuantroContentWrappingButton) v ;
			if ( qcwb.getWidth() > 0 && qcwb.getHeight() > 0 )
				qcwb.setDropShadowDrawables(true, qcwb.getLeft(), qcwb.getTop()) ;
		}
		
		// ready the trim.  Trim matches the main (first) button, if visible.
		// Otherwise has size zero.
		if ( getChildAt(0).getVisibility() != View.VISIBLE ) {
			if ( mTrim != null ) {
				mTrim.setVisible(false, false) ;
				mTrim.setBounds(0, 0, 0, 0) ;
			}
		} else {
			Rect bottom = ((QuantroContentWrappingButton)getChildAt(0)).getRectBorderBottom() ;
			if ( mTrim != null )
				mTrim.getPadding(mTempRect) ;
			switch( mTrimStyle ) {
			case NONE:
				break ;
			case BOUNDED:
				// trim: match the 'content area' of the drawable
				// to the bottom of the rect, and the left-right 
				// ACTUAL bounds of the bottom.  The top should use
				// its intrinsic height.
				// Remember to apply left/top padding as an offset.
				mTrim.setVisible(true, false) ;
				mTrim.setBounds(
						bottom.left + getPaddingLeft(),
						bottom.bottom + getPaddingTop() + mTempRect.bottom - mTrim.getIntrinsicHeight(),
						bottom.right + getPaddingLeft(),
						bottom.bottom + getPaddingTop() + mTempRect.bottom) ;
				break ;
			case RUNS_LEFT:
				// trim: match the 'content area' of the drawable
				// to the bottom of the rect, and the right 
				// ACTUAL bounds of the bottom.  The top should use
				// its intrinsic height.  Left is where things get tricky:
				// we run all the way to the edge of this ViewGroup.
				// Remember to apply left/top padding as an offset.
				mTrim.setVisible(true, false) ;
				mTrim.setBounds(
						getLeft(),
						bottom.bottom + getPaddingTop() + mTempRect.bottom - mTrim.getIntrinsicHeight(),
						bottom.right + getPaddingLeft(),
						bottom.bottom + getPaddingTop() + mTempRect.bottom) ;
				break ;
			case RUNS_RIGHT:
				// trim: match the 'content area' of the drawable
				// to the bottom of the rect, and the left 
				// ACTUAL bounds of the bottom.  The top should use
				// its intrinsic height.  However, we run the right
				// edge all the way out to the end of this ViewGroup.
				// Remember to apply left/top padding as an offset.
				mTrim.setVisible(true, false) ;
				mTrim.setBounds(
						bottom.left + getPaddingLeft(),
						bottom.bottom + getPaddingTop() + mTempRect.bottom - mTrim.getIntrinsicHeight(),
						getRight(),
						bottom.bottom + getPaddingTop() + mTempRect.bottom) ;
				break ;
			}
		}
	}
	
	@Override
	synchronized protected void onDraw( Canvas canvas ) {
		super.onDraw(canvas) ;
		// onDraw: all we do is draw our children.  We have already given them a layout
		// and measurements.  Nothing special here... EXCEPT drawing drop shadows.
		
		for ( int i = 0; i < getChildCount(); i++ ) {
			View v = getChildAt(i) ;
			if ( v.getVisibility() != View.VISIBLE )
				continue ;	// no shadow if not visible
			
			QuantroContentWrappingButton qcwb = (QuantroContentWrappingButton) v ;
			if ( !qcwb.isEnabled() )
				continue ;	// no shadow if not enabled
			
			qcwb.drawDropShadow(canvas) ;
		}
	}
	
	
	@Override
	synchronized protected void dispatchDraw( Canvas canvas ) {
		// We override this method so we can draw our trim over children.
		super.dispatchDraw(canvas) ;
		if ( mTrim != null )
			mTrim.draw(canvas) ;
	}
	
	
	@Override
	synchronized public void addView( View child ) {
		//Log.d(TAG, "addView noparam " + child + " ; currently have " + getChildCount() + " children") ;
		if ( !(child instanceof QuantroContentWrappingButton) )
			throw new RuntimeException("Button strip can only display QuantroContentWrappingButtons!") ;
		// set its settings, dood.
		
		if ( getChildCount() > 0 )
			setParamsForSubbutton((QuantroContentWrappingButton)child) ;
		else
			setParamsForMainbutton((QuantroContentWrappingButton)child) ;
		
		super.addView(child) ;
	}
	
	@Override
	synchronized public void addView (View child, int index) {
		//Log.d(TAG, "addView noparam" + child + " with index " + index + "; currently have " + getChildCount() + " children") ;
		if ( !(child instanceof QuantroContentWrappingButton) )
			throw new RuntimeException("Button strip can only display QuantroContentWrappingButtons!") ;
		// set its settings, dood.
		
		if ( index > 0 )
			setParamsForSubbutton((QuantroContentWrappingButton)child) ;
		else if ( index == 0 )
			setParamsForMainbutton((QuantroContentWrappingButton)child) ;
		// Strangely, super.addView(child, params) will make a call to this.addView(child, -1, params).
		// I can't explain this and it isn't mentioned in the documentation (that I can see), but I have
		// to assume that any call using index < 0 is meant to NOT add the view as a child, but instead
		// only set the layout parameters.
		
		super.addView(child, index) ;
	}
	
	@Override
	synchronized public void addView (View child, ViewGroup.LayoutParams params) {
		//Log.d(TAG, "addView YAparam" + child + " ; currently have " + getChildCount() + " children") ;
		if ( !(child instanceof QuantroContentWrappingButton) )
			throw new RuntimeException("Button strip can only display QuantroContentWrappingButtons!") ;
		// set its settings, dood.
		
		if ( getChildCount() > 0 )
			setParamsForSubbutton((QuantroContentWrappingButton)child) ;
		else
			setParamsForMainbutton((QuantroContentWrappingButton)child) ;
		super.addView(child, params) ;
	}
	
	
	@Override
	synchronized public void addView (View child, int index, ViewGroup.LayoutParams params) {
		//Log.d(TAG, "addView YAparam" + child + " with index " + index + "; currently have " + getChildCount() + " children") ;
		if ( !(child instanceof QuantroContentWrappingButton) )
			throw new RuntimeException("Button strip can only display QuantroContentWrappingButtons!") ;
		// set its settings, dood.
		
		if ( index > 0 )
			setParamsForSubbutton((QuantroContentWrappingButton)child) ;
		else if ( index == 0 )
			setParamsForMainbutton((QuantroContentWrappingButton)child) ;
		// Strangely, super.addView(child, params) will make a call to this.addView(child, -1, params).
		// I can't explain this and it isn't mentioned in the documentation (that I can see), but I have
		// to assume that any call using index < 0 is meant to NOT add the view as a child, but instead
		// only set the layout parameters.
		
		super.addView(child, index, params) ;
	}
	
	
	/**
	 * Sets relevant parameters on the provided button according to our own display settings.
	 * @param button
	 */
	synchronized private void setParamsForSubbutton( QuantroContentWrappingButton button ) {
		// Apply the border width and gradient height.
		int borderW = mButtonBorderWidth - mSubbuttonBorderInset ;
		Rect border = new Rect(borderW,borderW,borderW,borderW) ;
		if ( mButtonBorderWidthBottom >= 0 )
			border.bottom = mButtonBorderWidthBottom - mSubbuttonBorderInset;
		if ( mButtonBorderWidthTop >= 0 )
			border.top = mButtonBorderWidthTop - mSubbuttonBorderInset ;
		button.setBorderWidth( borderW, border ) ;
		
		borderW = ( mButtonBorderWidthAlt == -1 ? mButtonBorderWidth : mButtonBorderWidthAlt )
				- mSubbuttonBorderInset ;
		border = new Rect(borderW,borderW,borderW,borderW) ;
		if ( mButtonBorderWidthBottomAlt >= 0 )
			border.bottom = mButtonBorderWidthBottomAlt - mSubbuttonBorderInset;
		else if ( mButtonBorderWidthAlt < 0 && mButtonBorderWidthBottom >= 0 )
			border.bottom = mButtonBorderWidthBottom - mSubbuttonBorderInset;
		if ( mButtonBorderWidthTopAlt >= 0 )
			border.top = mButtonBorderWidthTopAlt - mSubbuttonBorderInset;
		else if ( mButtonBorderWidthAlt < 0 && mButtonBorderWidthTop >= 0 )
			border.top = mButtonBorderWidthTop - mSubbuttonBorderInset ;
		button.setBorderWidthAlt( borderW, border) ;
		
		button.setGradientHeight( mButtonGradientHeight - mSubbuttonGradientInset ) ;
		
		// Provide vertical padding to compensate
		button.setPadding(0, mSubbuttonBorderInset, 0, mSubbuttonBorderInset) ;
		
		// Content margins!
		View contentView = button.getContentView() ;
		if ( contentView != null )
			contentView.setPadding(
					mSubbuttonContentMargin.left,
					mSubbuttonContentMargin.top,
					mSubbuttonContentMargin.right,
					mSubbuttonContentMargin.bottom) ;
		//Log.d(TAG, "setting content margins: " + mSubbuttonContentMargin.left
		//			+ " " + mSubbuttonContentMargin.top
		//			+ " " + mSubbuttonContentMargin.right
		//			+ " " + mSubbuttonContentMargin.bottom) ;
	}
	
	/**
	 * Sets relevant parameters on the provided button according to our own display settings.
	 * @param button
	 */
	synchronized private void setParamsForMainbutton( QuantroContentWrappingButton button ) {
		// Apply the border width and gradient height.
		int borderW = mButtonBorderWidth ;
		Rect border = new Rect(borderW,borderW,borderW,borderW) ;
		if ( mButtonBorderWidthBottom >= 0 )
			border.bottom = mButtonBorderWidthBottom ;
		if ( mButtonBorderWidthTop >= 0 )
			border.top = mButtonBorderWidthTop ;
		
		button.setBorderWidth( borderW, border) ;
		
		
		borderW = mButtonBorderWidthAlt == -1 ? mButtonBorderWidth : mButtonBorderWidthAlt ;
		border = new Rect(borderW,borderW,borderW,borderW) ;
		if ( mButtonBorderWidthBottomAlt >= 0 )
			border.bottom = mButtonBorderWidthBottomAlt ;
		else if ( mButtonBorderWidthAlt < 0 && mButtonBorderWidthBottom >= 0 )
			border.bottom = mButtonBorderWidthBottom ;
		if ( mButtonBorderWidthTopAlt >= 0 )
			border.top = mButtonBorderWidthTopAlt ;
		else if ( mButtonBorderWidthAlt < 0 && mButtonBorderWidthTop >= 0 )
			border.top = mButtonBorderWidthTop ;
		button.setBorderWidthAlt( borderW, border) ;
		
		button.setGradientHeight( mButtonGradientHeight ) ;
		
		button.setPadding(0, 0, 0, 0) ;
		
		// Content margins!
		View contentView = button.getContentView() ;
		if ( contentView != null )
			contentView.setPadding(
					mSubbuttonContentMargin.left,
					mSubbuttonContentMargin.top,
					mSubbuttonContentMargin.right,
					mSubbuttonContentMargin.bottom) ;
	}
	
	
	@Override
	public void onClick(View v) {
		Controller c = mController ;
		int i = this.indexOfChild(v) ;
		if ( i >= 0 && c != null )
			mController.qbsc_onClick(this, i, false) ;
	}
	
	@Override
	public boolean onLongClick(View v) {
		Controller c = mController ;
		int i = this.indexOfChild(v) ;
		if ( i >= 0 && c != null )
			return mController.qbsc_onLongClick(this, i) ;
		return false ;
	}

	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		Controller c = mController ;
		int i = this.indexOfChild(qcwb) ;
		if ( i >= 0 && c != null )
			return mController.qbsc_supportsLongClick(this, i) ;
		return false ;
	}

	@Override
	public void qbpw_onButtonUsed(QuantroContentWrappingButton button, boolean asPopup,
			int menuItem, Object menuItemTag) {
		Controller c = mController ;
		if ( c != null ) {
			if ( button == mOverflowButton )
				mController.qbsc_onClick(this, ((Integer)menuItemTag).intValue(), asPopup) ;
		}
	}

	@Override
	public void qbpw_onMenuOpened(QuantroContentWrappingButton button) {
		Controller c = mController ;
		if ( c != null ) {
			if ( button == mOverflowButton ) 
				mController.qbsc_onOverflowClicked(this) ;
		}
	}
}
