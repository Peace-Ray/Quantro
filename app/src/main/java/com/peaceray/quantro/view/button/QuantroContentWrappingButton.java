package com.peaceray.quantro.view.button;

import com.peaceray.quantro.R;
import com.peaceray.quantro.view.colors.ColorOp;
import com.peaceray.quantro.view.drawable.DropShadowDrawableFactory;
import com.peaceray.quantro.view.drawable.LineShadingDrawable;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable ;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * A QuantroContentWrappingButton is a ViewGroup containing a single user-specifiable
 * child - a content view (which may be image, text, a layout of both, etc.).  This
 * class is responsible for drawing gradient flair and a partial or complete border
 * around the content, as well as opaque or semi-opaque background (which will be drawn
 * to the canvas BEFORE the child view is drawn).  The WrappingButton is considered clickable
 * whenever it is enabled; however, only the area inside the border (including the border itself)
 * is considered a valid area to click.
 * 
 * Will call setPressed(true) on its content when pressed, and setPressed(false) when released.
 * If the content should change based on this - for instance, if text should change color - 
 * handle it there.  In addition, setEnabled, setFocused, etc. will be called when appropriate
 * on the child.  By default, these calls will be made only on the immediate child, which (if
 * it contains subviews) is responsible for formatting them appropriately.  Call 
 * setStateChangeShouldRecur if you wish this View to recursively perform the state change on all
 * children of its child.
 * 
 * ContentWrapping button allows only one child.  Any attempt to access or set a child
 * with index > 0 will result in an exception being thrown.
 * 
 * There are several important values worth setting in a QCWB.  In addition to margin,
 * which is applied between the QCWB boundary and all drawn content, you may set border
 * width and flair height.  Together with margin, these values determine the space available
 * for the content view, which will be layed out flush with the border (the content's margins
 * can be used to inset its content from the drawn border).
 * 
 * Color is also important.  The following content areas may be set independently
 * (although the current aesthetic plan is to use differing alpha / hue values for
 * the same color within a button):
 * 
 * 		Content Background
 * 		Border
 * 		BorderBottom (if omitted, will be drawn as Border).
 * 		Gradient (list of colors and positions, as in the LinearGradient construction).
 * 
 * For each of the above, colors for the following states are allowed:
 * 
 * 		Disabled
 * 		Enabled
 * 		Pressed (implies enabled)
 * 		Focused
 * 		DisabledFocused
 * 
 * These need not all be specified: one may also set a "default" color for each content
 * area, or for the entire button, and the "most specific available" color will be used
 * in all cases.  By default, colors are an obnoxious pink, to encourage use of "default
 * color" settings and for bug-checking.  Finally, one can specify any color other than
 * the base default by using an alpha multiplier rather than a full color; it will be
 * applied to the next-most-specific color available.
 * 
 * DRAWING:
 * 
 * The QCWB is drawn using solid-colors (right now; possibly extensible later?) for the 
 * border and content background, and a linear gradient pointing downward below the border.
 * The content background is drawn exactly within the border - by matching border color to 
 * background color, you can draw a solid "un-bordered" shape.
 * 
 * GRADIENT DEFAULT AND MULTIPLIERS: Note: gradients are defined by arrays of color/positions.
 * While these two arrays must be the same length for any setting (state, default, etc.), the 
 * arrays used for a specific state might differ in length from those for the default.
 * We apply the alpha multiplier NOT to the specific values in the array, but to the gradient
 * that results; in other words, the "alpha multiplier / position array" defines a filter
 * gradient which we apply to the underlying default gradient.
 * 
 * FANCY EFFECTS IN BACKGROUND, BUTTON STRIP AND GRADIENT:
 * 
 * By default, the "wrapper" of a QCWB is drawn as a mixture of solid color (w/ alpha)
 * and gradient objects.  It is drawn to the canvas in onDraw; the 'draw' method
 * then takes care of drawing whatever content view is held by the button.
 * 
 * Perhaps you want the functionality and wrapper effects of a QCWB, but you want something
 * drawn that is more complex than simple colors and gradients?  Maybe, for example, you
 * want to apply the "wrapper" as an alpha filter on some other image, texture, or drawable?
 * 
 * One way to do this is for the containing ViewGroup to call setDrawWrapperOnDraw( false ).
 * This deactivates the automatic wrapper drawing; instead, onDraw(canvas) will do nothing,
 * and the effect of draw(canvas) is to draw ONLY THE CONTENT VIEW of the button.
 * 
 * Then, override onDraw() in the containing ViewGroup, to do the following:
 * 
 * 		Canvas c = ... <- some process to generate new canvas
 * 		qcwb.drawWrapper( c ) ;
 * 		// Now do something with the wrapper, such as applying filters or whatnot.
 * 
 * One idea is to drawWrapper on a new canvas, extract the alpha channel, and apply it
 * to some other structure.
 * 
 * @author Jake
 *
 */
public class QuantroContentWrappingButton extends FrameLayout {
	

	
	/**
	 * The content of a QuantroContentWrappingButton is not guaranteed to be
	 * part of the View hierarchy (i.e., calls which assumes such such as "postInvalidate"
	 * may not succeed).  For that reason, we ask that Views which expect to alter
	 * their own content implement this interface.  A reference to the button
	 * will be provided via the interface; from there, it is the responsibility
	 * of the content view to call "invalidateContent( View v )" upon each update.
	 * The QCWB will handle the rest.
	 * 
	 * @author Jake
	 *
	 */
	public interface SelfUpdatingButtonContent {
		
		public void setContainingButton( QuantroContentWrappingButton qcwb ) ;
		
	}
	
	
	
	public interface SupportsLongClickOracle {
		public boolean supportsLongClick( QuantroContentWrappingButton qcwb ) ;
	}
	
	
	@SuppressWarnings("unused")
	private static final String TAG = "QuantroContentWrappingButton" ;
	
	private static final long DEFAULT_LONG_CLICK_TIME = 700 ;
	
	
	public QuantroContentWrappingButton(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public QuantroContentWrappingButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		// require only one child
		//if ( this.getChildCount() > 1 )
		//	throw new RuntimeException("QuantroContentWrappingButton: has more than one child!") ;
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
		
		
	}
	
	public QuantroContentWrappingButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		
		// require only one child
		//if ( this.getChildCount() > 1 )
		//	throw new RuntimeException("QuantroContentWrappingButton: has more than one child!") ;
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues( Context context ) {
		mContent = null ;
		
		mContentState = ContentState.OPTIONAL ;
		mDrawWrapperOnDraw = true ;
		mDrawContentOnDraw = true ;
		
		mRecurStateChange = false ;
		
		mMeasureContent = true ;
		
		mHideUnpressed = false ;
		mDropShadow = false ;
		
		mProportions = Proportions.RECT ;
		
		mFillDrawable = null ;
		mFillDrawableUnpressed = false ;
		
		// Default values: a reasonable border width and gradient size,
		// null arrays where needed, lets of "SET_NONE"s, etc.
		mBorderWidth = new int[NUM_BOXES] ;
		mBorderWidthBySide = new Rect[NUM_BOXES] ;
		
		mBorderWidth[BOX_STANDARD] = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)8, getResources().getDisplayMetrics()) ;
		mBorderWidth[BOX_ALT] = -1 ;
		for ( int i = 0; i < NUM_BOXES; i++ )
			mBorderWidthBySide[i] = new Rect(-1,-1,-1,-1) ;
		mGradientHeight = 0 ;
		
		mMinWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)20, getResources().getDisplayMetrics()) ;
		mMinHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)20, getResources().getDisplayMetrics()) ;
		mMinTouchableWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)20, getResources().getDisplayMetrics()) ;
		mMinTouchableHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)20, getResources().getDisplayMetrics()) ;
		
		
		// Set all 'set' values to SET_NONE.
		for ( int e = 0; e < 2; e++ ) {
			for ( int f = 0; f < 2; f++ ) {
				for ( int p = 0; p < 2; p++ ) {
					for ( int g = 0; g < 2; g++ ) {
						for ( int section = 0; section < NUM_SECTIONS; section++ ) {
							mSetSection_enabledFocusedPressedGlow[section][e][f][p][g] = SET_NONE ;
						}
						mSetGradient_enabledFocusedPressedGlow[e][f][p][g] = SET_NONE ;
						mSetTopGradient_enabledFocusedPressedGlow[e][f][p][g] = SET_NONE ;
					}
				}
			}
		}
		for ( int section = 0; section < NUM_SECTIONS; section++ )
			mSetDefaultSection[section] = SET_NONE ;
		mSetDefaultGradient = SET_NONE ;
		mSetDefaultTopGradient = SET_NONE ;
		
		// Finally, set the default color.
		mColorDefault = COLOR_DEFAULT ;
		
		mBackgroundDrawable = null ;
		
		mStyle = Style.PLAIN ;
		
		mSupportsLongClickOracle = null ;
		
		
		mBlockFillColor = COLOR_DEFAULT ;
		mBlockBorderColor = COLOR_DEFAULT ;
		
		// Drop shadow region arrays
		mDropShadowDrawable = new Drawable[NUM_BOXES] ;
		mDropShadowBottomDrawable = new Drawable[NUM_BOXES] ;
		mDropShadowPadding = new Rect[NUM_BOXES] ;
		
		// here are some drawables - gradients, basically.
		// we use these for enabled states, including pressed.
		mBlockDrawableFull = new GradientDrawable[NUM_BOXES] ;
		mBlockDrawableCorner = new GradientDrawable[NUM_BOXES][4] ;
		mBlockDrawableSide = new GradientDrawable[NUM_BOXES][4] ; ;
		
		mBlockDrawableContentShadow = null ;
		
		mBlockDrawableDisabledFull = new GradientDrawable[NUM_BOXES] ;
		mBlockDrawableDisabledBottom = new GradientDrawable[NUM_BOXES] ;
		
		mBlockAlpha = new int[4] ;
		
		mBlockPaintInnerEdge = new Paint() ;
		mBlockPaintInnerEdge.setColor( context.getResources().getColor(R.color.quantro_content_wrapping_button_inner_edge) ) ;
		mBlockPaintOuterEdge = new Paint() ;
		mBlockPaintOuterEdge.setColor( context.getResources().getColor(R.color.quantro_content_wrapping_button_outer_edge) ) ;
		
		
		mHandler = new Handler() ;
		mRunnablePostInvalidate = new Runnable() {
			@Override
			public void run() {
				postInvalidate() ;
			}
		} ;
		
		mTimeLongClickAlphaChange = DEFAULT_LONG_CLICK_TIME ;
		
		mRect = new Rect[NUM_BOXES][NUM_RECTS] ;
	}
	
	
	/**
	 * Reads all QuantroContentWrappingButton attributes from the AttributeSet,
	 * and sets the appropriate fields.  Does NOT generate paints.
	 * @param context
	 * @param attrs
	 */
	private void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.QuantroContentWrappingButton);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			int ref ;
			int [] cArray ;
			float [] fArray ;
			float [] aArray ;
		    switch (attr) {
		    ////////////////////////////////////////////////////////////////
	    	////////////////////////////////////////////////////////////////
	    	// RECURSIVE STATE CHANGES /////////////////////////////////////
	    	////////////////////////////////////////////////////////////////
		    case R.styleable.QuantroContentWrappingButton_recursive_state_change:
		    	mRecurStateChange = a.getBoolean(attr, false) ;
		    	break ;
		    	
		    ////////////////////////////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    // MEASURE CONTENT /////////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    case R.styleable.QuantroContentWrappingButton_qcwb_measure_content:
		    	mMeasureContent = a.getBoolean(attr, true) ;
		    	break ;
		    	
	    	////////////////////////////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    // HIDE WHEN UNPRESSED /////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    case R.styleable.QuantroContentWrappingButton_hide_unpressed:
		    	mHideUnpressed = a.getBoolean(attr, false) ;
		    	break ;
			    	
	    	////////////////////////////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    // DROP SHADOWS ////////////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    case R.styleable.QuantroContentWrappingButton_drop_shadow:
		    	mDropShadow = a.getBoolean(attr, false) ;
		    	break ;
		    
    		////////////////////////////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    // PROPORTIONS /////////////////////////////////////////////////
		    ////////////////////////////////////////////////////////////////
		    case R.styleable.QuantroContentWrappingButton_proportions:
		    	ref = a.getInt(attr, 0) ;
		    	switch( ref ) {
		    	case 0:
		    		mProportions = Proportions.RECT ;
		    		break ;
		    	case 1:
		    		mProportions = Proportions.SQUARE_GROW ;
		    		break ;
		    	case 2:
		    		mProportions = Proportions.SQUARE_SHRINK ;
		    		break ;
		    	}
		    	break ;
		    
			    
		    	
		    ////////////////////////////////////////////////////////////////
	    	////////////////////////////////////////////////////////////////
	    	// DIMENSIONS //////////////////////////////////////////////////
	    	////////////////////////////////////////////////////////////////
		    case R.styleable.QuantroContentWrappingButton_border_width:
		    	mBorderWidth[BOX_STANDARD] = a.getDimensionPixelOffset(attr, 8) ;
		    	break ;
		    case R.styleable.QuantroContentWrappingButton_border_width_bottom:
		    	mBorderWidthBySide[BOX_STANDARD].bottom = a.getDimensionPixelOffset(attr, 8) ;
		    	break ;
		    case R.styleable.QuantroContentWrappingButton_border_width_top:
		    	mBorderWidthBySide[BOX_STANDARD].top = a.getDimensionPixelOffset(attr, 8) ;
		    	break ;
		    	
		    case R.styleable.QuantroContentWrappingButton_border_width_alt:
		    	mBorderWidth[BOX_ALT] = a.getDimensionPixelOffset(attr, 4) ;
		    	break ;
		    case R.styleable.QuantroContentWrappingButton_border_width_bottom_alt:
		    	mBorderWidthBySide[BOX_ALT].bottom = a.getDimensionPixelOffset(attr, 4) ;
		    	break ;
		    case R.styleable.QuantroContentWrappingButton_border_width_top_alt:
		    	mBorderWidthBySide[BOX_ALT].top = a.getDimensionPixelOffset(attr, 4) ;
		    	break ;
		    	
		    case R.styleable.QuantroContentWrappingButton_gradient_height:
		    	mGradientHeight = a.getDimensionPixelOffset(attr, 10) ;
		    	break ;

	    
	    	////////////////////////////////////////////////////////////////
	    	////////////////////////////////////////////////////////////////
	    	// COLORS //////////////////////////////////////////////////////
	    	////////////////////////////////////////////////////////////////
	    	// Default color
	        case R.styleable.QuantroContentWrappingButton_color_default:
	        	mColorDefault = a.getColor(attr, COLOR_DEFAULT) ;
	            break;
	        
	        ////////////////////////////////////////////////////////////////
	        // COLORS FOR SECTIONS /////////////////////////////////////////
	        // Default section color
	        case R.styleable.QuantroContentWrappingButton_color_background_default:
	        	mColorDefaultSection[SECTION_BACKGROUND] = a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetDefaultSection[SECTION_BACKGROUND] = SET_COLOR ;
	            break;
	        case R.styleable.QuantroContentWrappingButton_color_border_default:
	        	mColorDefaultSection[SECTION_BORDER] = a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetDefaultSection[SECTION_BORDER] = SET_COLOR ;
	            break;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_default:
	        	mColorDefaultSection[SECTION_BORDER_BOTTOM] = a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetDefaultSection[SECTION_BORDER_BOTTOM] = SET_COLOR ;
	            break;
	        // Section state color: DISABLED
	        case R.styleable.QuantroContentWrappingButton_color_background_disabled:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][0][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][0][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_disabled:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][0][0][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][0][0][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_disabled:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][0][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][0][0][0] = SET_COLOR ;
	        	break ;
	        // Section state color: ENABLED
	        case R.styleable.QuantroContentWrappingButton_color_background_enabled:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_enabled:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_enabled:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][0] = SET_COLOR ;
	        	break ;
	        // Section state color: PRESSED
	        case R.styleable.QuantroContentWrappingButton_color_background_pressed:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_pressed:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_pressed:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][0] = SET_COLOR ;
	        	break ;
	        // Section state color: SELECTED
	        case R.styleable.QuantroContentWrappingButton_color_background_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][0] = SET_COLOR ;
	        	break ;
	        // Section state color: DISABLED SELECTED
	        case R.styleable.QuantroContentWrappingButton_color_background_disabled_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][1][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][1][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_disabled_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][0][1][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][0][1][0][0] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_disabled_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][1][0][0]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][1][0][0] = SET_COLOR ;
	        	break ;
	        // Section state color: GLOW
	        case R.styleable.QuantroContentWrappingButton_color_background_glow:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][1] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_glow:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][1] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_glow:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][1] = SET_COLOR ;
	        	break ;
	        // Section state color: GLOW FOCUSED
	        case R.styleable.QuantroContentWrappingButton_color_background_glow_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][1] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_glow_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][1] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_glow_focused:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][1] = SET_COLOR ;
	        	break ;
	        // Section state color: GLOW PRESSED
	        case R.styleable.QuantroContentWrappingButton_color_background_glow_pressed:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][1] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_glow_pressed:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][1] = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_border_bottom_glow_pressed:
	        	mColorSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][1]
	        			= a.getColor(attr, COLOR_DEFAULT) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][1] = SET_COLOR ;
	        	break ;
	        	
	        ////////////////////////////////////////////////////////////////
        	// ALPHA MULT FOR SECTIONS /////////////////////////////////////
	        // Default section color
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_default:
	        	mAlphaMultDefaultSection[SECTION_BACKGROUND] = a.getInt(attr, 1) ;
	        	mSetDefaultSection[SECTION_BACKGROUND] = SET_ALPHA_MULT ;
	            break;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_default:
	        	mAlphaMultDefaultSection[SECTION_BORDER] = a.getInt(attr, 1) ;
	        	mSetDefaultSection[SECTION_BORDER] = SET_ALPHA_MULT ;
	            break;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_default:
	        	mAlphaMultDefaultSection[SECTION_BORDER_BOTTOM] = a.getInt(attr, 1) ;
	        	mSetDefaultSection[SECTION_BORDER_BOTTOM] = SET_ALPHA_MULT ;
	            break;
	        // Section state alpha_mult: DISABLED
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_disabled:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][0][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][0][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_disabled:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][0][0][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][0][0][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_disabled:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][0][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][0][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        // Section state alpha_mult: ENABLED
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_enabled:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_enabled:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_enabled:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        // Section state alpha_mult: PRESSED
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_pressed:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_pressed:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_pressed:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][0] = SET_ALPHA_MULT ;
	        	break ;
	        // Section state alpha_mult: SELECTED
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        // Section state alpha_mult: DISABLED SELECTED
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_disabled_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][1][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][0][1][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_disabled_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][0][1][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][0][1][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_disabled_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][1][0][0]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][0][1][0][0] = SET_ALPHA_MULT ;
	        	break ;
	        // Section state alpha_mult: GLOW
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_glow:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][0][1] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_glow:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][0][1] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_glow:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][0][1] = SET_ALPHA_MULT ;
	        	break ;
	        // Section state alpha_mult: GLOW FOCUSED
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_glow_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][1][0][1] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_glow_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][1][0][1] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_glow_focused:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][1][0][1] = SET_ALPHA_MULT ;
	        	break ;
	        // Section state alpha_mult: GLOW PRESSED
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_background_glow_pressed:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BACKGROUND][1][0][1][1] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_glow_pressed:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER][1][0][1][1] = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_border_bottom_glow_pressed:
	        	mAlphaMultSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][1]
	        			= a.getInt(attr, 1) ;
	        	mSetSection_enabledFocusedPressedGlow[SECTION_BORDER_BOTTOM][1][0][1][1] = SET_ALPHA_MULT ;
	        	break ;
	        	
        	////////////////////////////////////////////////////////////////
        	// COLOR GRADIENTS /////////////////////////////////////////////
	        // Default gradient color    
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_default:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorDefaultGradient = cArray ;
	        	mPositionsDefaultGradient = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        // State gradient color
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_disabled:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[0][0][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[0][0][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_enabled:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_pressed:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][1][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][1][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_focused:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[1][1][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][1][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_disabled_focused:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[0][1][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[0][1][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_glow:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][0][1] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][0][1] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_glow_focused:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[1][1][0][1] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][1][0][1] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_top_glow_pressed:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][1][1] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][1][1] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        
	        ////////////////////////////////////////////////////////////////
	        // ALPHA MULT GRADIENTS ////////////////////////////////////////
	        // Default gradient alpha mult    
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_default:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultDefaultGradient = aArray ;
	        	mPositionsDefaultGradient = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        // State gradient color
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_disabled:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[0][0][0][0] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[0][0][0][0] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_enabled:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[1][0][0][0] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][0][0] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_pressed:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[1][0][1][0] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][1][0] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_focused:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[1][1][0][0] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][1][0][0] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_disabled_focused:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[0][1][0][0] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[0][1][0][0] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_glow:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[1][0][0][1] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][0][1] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_glow_focused:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[1][1][0][1] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][1][0][1] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_gradient_top_glow_pressed:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[1][0][1][1] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][1][1] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        
	        
	        ////////////////////////////////////////////////////////////////
		    // INT ARRAY COLOR GRADIENTS ///////////////////////////////////
	        // Default gradient color array 
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_default:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorDefaultGradient = cArray ;
	        	mPositionsDefaultGradient = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        // State gradient color array.
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_disabled:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[0][0][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[0][0][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_enabled:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_pressed:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][1][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][1][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_focused:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[1][1][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][1][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_disabled_focused:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[0][1][0][0] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[0][1][0][0] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_glow:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][0][1] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][0][1] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_glow_focused:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[1][1][0][1] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][1][0][1] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_gradient_array_glow_pressed:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorGradient_enabledFocusedPressedGlow[1][0][1][1] = cArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[1][0][1][1] = fArray ;
	        	mSetDefaultGradient = SET_COLOR ;
	        	break ;
	        	
        	////////////////////////////////////////////////////////////////
        	// COLOR GRADIENTS /////////////////////////////////////////////
	        // Default gradient color    
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_default:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorDefaultTopGradient = cArray ;
	        	mPositionsDefaultTopGradient = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        // State gradient color
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_disabled:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[0][0][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[0][0][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_enabled:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_pressed:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][1][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][1][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_focused:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][1][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][1][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_disabled_focused:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[0][1][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[0][1][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_glow:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][0][1] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][0][1] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_glow_focused:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][1][0][1] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][1][0][1] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_bottom_glow_pressed:
	        	cArray = new int[2] ;
	        	cArray[0] = a.getInt(attr, COLOR_DEFAULT) ;
	        	cArray[1] = Color.argb(0, Color.red(cArray[0]), Color.green(cArray[0]), Color.blue(cArray[0])) ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][1][1] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][1][1] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        
	        ////////////////////////////////////////////////////////////////
	        // ALPHA MULT GRADIENTS ////////////////////////////////////////
	        // Default gradient alpha mult    
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_default:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultDefaultTopGradient = aArray ;
	        	mPositionsDefaultTopGradient = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        // State gradient color
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_disabled:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultGradient_enabledFocusedPressedGlow[0][0][0][0] = aArray ;
	        	mPositionsGradient_enabledFocusedPressedGlow[0][0][0][0] = fArray ;
	        	mSetDefaultGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_enabled:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultTopGradient_enabledFocusedPressedGlow[1][0][0][0] = aArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_pressed:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultTopGradient_enabledFocusedPressedGlow[1][0][1][0] = aArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][1][0] = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_focused:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultTopGradient_enabledFocusedPressedGlow[1][1][0][0] = aArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][1][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_disabled_focused:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultTopGradient_enabledFocusedPressedGlow[0][1][0][0] = aArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[0][1][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_glow:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultTopGradient_enabledFocusedPressedGlow[1][0][0][1] = aArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][0][1] = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_glow_focused:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultTopGradient_enabledFocusedPressedGlow[1][1][0][1] = aArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][1][0][1] = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_alpha_mult_top_gradient_bottom_glow_pressed:
	        	aArray = new float[2] ;
	        	aArray[0] = a.getFloat(attr, 1.0f) ;
	        	aArray[1] = 0.0f ;
	        	fArray = new float[]{0.0f,1.0f} ;
	        	mAlphaMultTopGradient_enabledFocusedPressedGlow[1][0][1][1] = aArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][1][1] = fArray ;
	        	mSetDefaultTopGradient = SET_ALPHA_MULT ;
	        	break ;
	        
	        
	        ////////////////////////////////////////////////////////////////
		    // INT ARRAY COLOR GRADIENTS ///////////////////////////////////
	        // Default gradient color array 
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_default:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorDefaultTopGradient = cArray ;
	        	mPositionsDefaultTopGradient = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        // State gradient color array.
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_disabled:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[0][0][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[0][0][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_enabled:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_pressed:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][1][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][1][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_focused:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][1][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][1][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_disabled_focused:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[0][1][0][0] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[0][1][0][0] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_glow:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][0][1] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][0][1] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_glow_focused:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][1][0][1] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][1][0][1] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        case R.styleable.QuantroContentWrappingButton_color_top_gradient_array_glow_pressed:
	        	ref = a.getResourceId(attr, 0) ;
	        	if ( ref == 0 ) throw new RuntimeException("Resource id not found.") ;
	        	cArray = context.getResources().getIntArray(ref) ;
	        	if ( cArray == null || cArray.length < 2 ) throw new RuntimeException("Gradient array must be length at least 2") ;
	        	fArray = replaceNullArrayWithEquidistantPositions(null, cArray.length) ;
	        	mColorTopGradient_enabledFocusedPressedGlow[1][0][1][1] = cArray ;
	        	mPositionsTopGradient_enabledFocusedPressedGlow[1][0][1][1] = fArray ;
	        	mSetDefaultTopGradient = SET_COLOR ;
	        	break ;
	        
		    }
		}
		a.recycle();
	}
	
	
	/**
	 * Generates paint, rectangles, etc. for an initial View.
	 * @param context
	 */
	private void constructor_allocateAndInitialize(Context context) {
		// Drop shadows!  Fairly simple.  We don't have layout size, so just
		// get copies in both box indices.
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			if ( mDropShadow ) {
				mDropShadowPadding[boxIndex] = DropShadowDrawableFactory.getPadding(context) ;
			} else {
				mDropShadowPadding[boxIndex] = new Rect(0,0,0,0) ;
			}
		}
		mDropShadowDrawable = new Drawable[NUM_BOXES] ;
		mDropShadowBottomDrawable = new Drawable[NUM_BOXES] ;
		
		// Make the rectangles
		for ( int i = 0; i < NUM_BOXES; i++ ) {
			for ( int j = 0; j < NUM_RECTS; j++ ) {
				mRect[i][j] = new Rect() ;
			}
		}
		
		// Make the paints
		generateAllPaints() ;
		
		// By the way, we should draw this view.
		this.setWillNotDraw(false) ;

		// replace any -1s in our bySides by the default.
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			replaceNegativeOnes( mBorderWidthBySide[boxIndex], mBorderWidth[boxIndex], mBorderWidthBySide[BOX_STANDARD] ) ;
		}
		
		mBorderWidthBoundingBox = new Rect() ;
		setToMaxInEachDimension( mBorderWidthBoundingBox, mBorderWidthBySide ) ;
		
		mColorFilterToWhite = new LightingColorFilter( Color.WHITE, Color.WHITE ) ;
	}
	
	private void setToMaxInEachDimension( Rect rectMax, Rect [] candidates ) {
		rectMax.set(0,0,0,0) ;
		for ( int index = 0; index < NUM_BOXES; index++ ) {
			rectMax.left = Math.max(rectMax.left, candidates[index].left) ;
			rectMax.right = Math.max(rectMax.right, candidates[index].right) ;
			rectMax.top = Math.max(rectMax.top, candidates[index].top) ;
			rectMax.bottom = Math.max(rectMax.bottom, candidates[index].bottom) ;
		}
	}
	
	private void replaceNegativeOnes( Rect fillIn, int val, Rect backupVals ) {
		if ( val != -1 ) {
			if ( fillIn.top == -1 )
				fillIn.top = val ;
			if ( fillIn.bottom == -1 )
				fillIn.bottom = val ;
			if ( fillIn.left == -1 )
				fillIn.left = val ;
			if ( fillIn.right == -1 )
				fillIn.right = val ;
		} else {
			if ( fillIn.top == -1 )
				fillIn.top = backupVals.top ;
			if ( fillIn.bottom == -1 )
				fillIn.bottom = backupVals.bottom ;
			if ( fillIn.left == -1 )
				fillIn.left = backupVals.left ;
			if ( fillIn.right == -1 )
				fillIn.right = backupVals.right ;
		}
	}
	
	/**
	 * Proportions: We size ourselves as a rectangle drawn around content.  However,
	 * there are different options available in terms of our rectangle proportions.
	 * 
	 * @author Jake
	 *
	 */
	protected enum Proportions {
		/**
		 * We form ourselves as a rectangle around content.  We obey all
		 * settings and view limitations, but given that those limitations
		 * are met, we tend to bound content at any arbitrary proportion.
		 */
		RECT,
		
		
		/**
		 * We do our best to contort ourselves into a square, obeying all settings
		 * and view limitations.  We tend to reach the square shape by GROWING from
		 * a bounding rect, if possible.
		 * 
		 * This doesn't guarantee a square shape, but we will do our best.  If possible,
		 * this shape will be reached by growing OUTWARD from the content's preferred
		 * size; only if this fails will be shrink INWARD to compress the content.
		 */
		SQUARE_GROW,
		
		/**
		 * We do our best to contort ourselves into a square, obeying all settings
		 * and view limitations.  We tend to reach the square shape by SHRINKING into
		 * a bounding rect, if possible.
		 * 
		 * This doesn't guarantee a square shape, but we will do our best.  After
		 * content has been measured, we will reach a square shape by COMPRESSING
		 * that content in the appropriate axis.
		 */
		SQUARE_SHRINK
	}
	
	/**
	 * ContentState describes the in-model interpretation of the button
	 * content.  Typical State is 'OPTIONAL', meaning it fuctions like a
	 * standard button - touch, release, and the program "goes."  Some
	 * buttons can be long-pressed for a different result.
	 * 
	 * Other states include 'EXCLUSIVE', meaning that the content is currently
	 * selected by the model (over and above any other content), and 'ACTIVE',
	 * meaning this content is currently selected at the same "tier" as other
	 * content.
	 * 
	 * For example, the buttons in a dialog box are all OPTIONAL, whether or
	 * not they are enabled.
	 * 
	 * However, in a list of available music tracks, the currently playing track
	 * would be EXCLUSIVE.
	 * 
	 * In a list of "shuffling background," the currently displayed background is
	 * EXCLUSIVE, the other backgrounds included in the rotation are ACTIVE, and
	 * any non-included background is OPTIONAL.
	 * 
	 * @author Jake
	 *
	 */
	public enum ContentState {
		
		/**
		 * Default state.  The content represents an option the user can take,
		 * but it is not currently "active," meaning it does not represent the
		 * "current program state" in any way.  
		 * 
		 * Note: even menu buttons that retain current program state should be
		 * considered OPTIONAL, e.g. the 'cancel' and 'ok' buttons on a dialog
		 * should be OPTIONAL, even though one maintains program state and the 
		 * other clearly has "program preference."  This is possibly an important
		 * distinction for the button, but *not* for the *content*.
		 */
		OPTIONAL,
		
		
		/**
		 * This content is first among peers.  Not only does it represent a piece
		 * of content with other "equivalent" alternatives (e.g. a music track, a
		 * game background, etc.) but this content is the single currently "in-use"
		 * or "priority" example among them.
		 * 
		 * Note the distinction between this and ACTIVE, where ACTIVE allows for
		 * equally-valued peers, and EXCLUSIVE is always the one-and-only.
		 * 
		 * One rule of thumb: if activating a different piece of content could
		 * render this content inactive, it is probably EXCLUSIVE (not just ACTIVE).
		 * 
		 * Example: the currently playing music track, the currently selected
		 * tab-button in an action bar tab selector, the currently displayed
		 * background among a set of "rotated" backgrounds.
		 */
		EXCLUSIVE,
		
		
		/**
		 * This content, among its peers, is currently "activated."  Not only does
		 * it represent a piece of content with other "equivalent" alternatives
		 * (e.g. a music track, a game background, etc.) , but this content is
		 * currently "in-use" where others might not be.
		 * 
		 * Note the distinction between this and EXCLUSIVE, where EXCLUSIVE requires
		 * that this content is the one-and-only, whereas ACTIVE allows for equally-valued
		 * peers.  Note also that EXCLUSIVE can be considered a "higher-level state"
		 * of ACTIVE.
		 * 
		 * One rule of thumb: if other pieces of content can be activated without
		 * affecting the activation of this piece, it is probably ACTIVE (not EXCLUSIVE).
		 * 
		 * Example: a background which is included in the current set of "rotated"
		 * backgrounds (among others), but is NOT the background currently displayed
		 * (the currently displayed background would be EXCLUSIVE).
		 */
		ACTIVE,
		
	}
	
	/**
	 * Style determines how the rectangular button drawn (e.g. shadows, shine, etc.)
	 * along with what elements are drawn (underline, STANDARD / ALT box, etc.).
	 * 
	 * @author Jake
	 * 
	 */
	public enum Style {
		
		/**
		 * Completely plain.  Standard colors without
		 * shine or shadow.  Not really used anymore, anywhere.
		 * 
		 * This style uses only the STANDARD box, in all circumstances.
		 */
		PLAIN,
		
		
		/**
		 * As a Quantro block -- shined and shadowed.  When unpressed,
		 * drawn as an underline.
		 * 
		 * This style uses only the STANDARD box, in all circumstances.
		 */
		BLOCK_UNDERLINE,
		
		
		
		/**
		 * As a Quantro block -- shined and shadowed.
		 * 
		 * Represents three different "tiers" of content selection.
		 * Unselected (OPTIONAL) content is drawn without any surrounding
		 * box information; i.e., the button wrapper is invisible.
		 * When touched, the STANDARD box is drawn.
		 * 
		 * Selected (ACTIVE) content is drawn with the STANDARD box, no glow.
		 * 
		 * EXCLUSIVE content is drawn with the ALT box.
		 */
		BLOCK_THREE_TIER_ENCLOSE_CONTENT,
		
		
		/**
		 * As BLOCK_THREE_TIER_ENCLOSE_CONTENT, except the ALT box is used for ACTIVE
		 * and OPTIONAL content, and STANDARD is used for EXCLUSIVE.
		 */
		BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE,
		
		
		
		
		/**
		 * As a Quantro block -- shined and shadowed.
		 * 
		 * Represents two different "tiers" of content selection.
		 * Unselected (OPTIONAL) content is drawn without any surrounding
		 * box information; i.e., the button wrapper is invisible.
		 * When touched, the STANDARD box is drawn.  Selected (ACTIVE
		 * or EXCLUSIVE) content is drawn with an ALT box.
		 */
		BLOCK_TWO_TIER_ENCLOSE_CONTENT,
		
		
		/**
		 * As BLOCK_TWO_TIER_ENCLOSE_CONTENT, except the ALT box is drawn
		 * for touched OPTIONAL content, and STANDARD box is drawn for the
		 * ACTIVE or EXCLUSIVE.
		 */
		BLOCK_TWO_TIER_ENCLOSE_CONTENT_REVERSE,
		
		
		/**
		 * As a Quantro block -- shined and shadowed.
		 * 
		 * Represents two different "tiers" of content selection.
		 * Unselected (OPTIONAL) content is drawn with an underline.
		 * When touched, the STANDARD box is drawn.  Selected (ACTIVE
		 * or EXCLUSIVE) content is drawn with an ALT box.
		 */
		BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT,
		
		
		/**
		 * As BLOCK_TWO_TIER_ENCLOSE_CONTENT, except the ALT box is drawn
		 * for touched OPTIONAL content, and STANDARD box is drawn for the
		 * ACTIVE or EXCLUSIVE.
		 */
		BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE,
		
		
		/**
		 * As BLOCK_TWO_TIER_ENCLOSE_CONTENT, except that the ALT box is
		 * drawn for all non-touched content (even unselected -- OPTIONAL --
		 * content).
		 */
		BLOCK_TWO_TIER_ALWAYS_ENCLOSE_CONTENT,
		
		
	}
	
	protected View mContent ;
	
	// Current content state
	protected ContentState mContentState ;
	// Current draw state
	protected boolean mDrawWrapperOnDraw ;		// if false, we only draw content in "onDraw"
	protected boolean mDrawContentOnDraw ;
	
	// Recur when setting state
	protected boolean mRecurStateChange ;
	
	// OnMeasure should measure content?
	protected boolean mMeasureContent ;
	
	// Hide when unpressed?
	protected boolean mHideUnpressed ;
	
	// Display drop shadows?
	protected boolean mDropShadow ;
	
	// Proportions?
	protected Proportions mProportions ;
	
	private Drawable mFillDrawable ;
	private boolean mFillDrawableUnpressed ;
	
	// SECTION DIMENSIONS
	protected int [] mBorderWidth ;
	protected Rect [] mBorderWidthBySide ;
	protected int mGradientHeight ;
	
	protected Rect mBorderWidthBoundingBox ;		//  the maximum "borderWidth" across all boxes.
	
	protected int mMinWidth ;
	protected int mMinHeight ;
	protected int mMinTouchableWidth ;
	protected int mMinTouchableHeight ;
	
	// INNER DEFAULT
	private static final int EMPTY_CONTENT_HEIGHT = 10 ;
	private static final int EMPTY_CONTENT_WIDTH = 10 ;
	
	private static final int BOX_STANDARD = 0 ;
	private static final int BOX_ALT = 1 ;
	private static final int NUM_BOXES = 2 ;
	
	private static final int RECT_GRADIENT = 0 ;
	private static final int RECT_TOP_GRADIENT = 1 ;
	private static final int RECT_BORDER_LEFT = 2 ;
	private static final int RECT_BORDER_RIGHT = 3 ;
	private static final int RECT_BORDER_TOP = 4 ;
	private static final int RECT_BORDER_BOTTOM = 5 ;
	private static final int RECT_CONTENT = 6 ;
	private static final int RECT_BORDERED_CONTENT = 7 ;
	private static final int NUM_RECTS = 8 ;
	
	// Rectangles for the area covered by our boundaries.
	private Rect [][] mRect ;
	
	private Drawable mBackgroundDrawable ;
	
	private Drawable [] mDropShadowDrawable ;
	private Drawable [] mDropShadowBottomDrawable ;
	private Rect [] mDropShadowPadding ;
	
	public static final int SECTION_BACKGROUND = 0 ;
	public static final int SECTION_BORDER = 1 ;
	public static final int SECTION_BORDER_BOTTOM = 2 ;
	public static final int NUM_SECTIONS = 3 ;
	
	public static final int STATE_DISABLED = 0 ;
	public static final int STATE_ENABLED = 1 ;
	public static final int STATE_PRESSED = 2 ;
	public static final int STATE_FOCUSED = 3 ;
	public static final int STATE_DISABLED_FOCUSED = 4 ;
	public static final int STATE_GLOW = 5 ;		// enabled, emphasized, NOT pressed or focused.
	public static final int STATE_GLOW_FOCUSED = 6 ;
	public static final int STATE_GLOW_PRESSED = 7 ;
	public static final int NUM_STATES = 8 ;
	
	protected static final int SET_NONE = 0 ;
	protected static final int SET_COLOR = 1 ;
	protected static final int SET_ALPHA_MULT = 2 ;
	
	// Stores the values set from outside - by XML, by method calls, etc. -
	// so that we can construct Paints from the appropriate sources (a
	// color, an alpha multiplier, etc.).
	protected static final int COLOR_DEFAULT = 0xffff6ec7 ;
	protected int mColorDefault ;
	
	protected int [] mColorDefaultSection = new int [NUM_SECTIONS] ;
	protected float [] mAlphaMultDefaultSection = new float [NUM_SECTIONS] ;
	protected int [] mSetDefaultSection = new int [NUM_SECTIONS] ;
	
	protected int [] mColorDefaultGradient ;
	protected float [] mAlphaMultDefaultGradient ;
	protected float [] mPositionsDefaultGradient ;
	protected int mSetDefaultGradient ;
	
	protected int [] mColorDefaultTopGradient ;
	protected float [] mAlphaMultDefaultTopGradient ;
	protected float [] mPositionsDefaultTopGradient ;
	protected int mSetDefaultTopGradient ;
	
	// Specific settings.
	protected int [][][][][] mColorSection_enabledFocusedPressedGlow = new int[NUM_SECTIONS][2][2][2][2] ;
	protected float [][][][][] mAlphaMultSection_enabledFocusedPressedGlow = new float[NUM_SECTIONS][2][2][2][2] ;
	protected int [][][][][] mSetSection_enabledFocusedPressedGlow = new int [NUM_SECTIONS][2][2][2][2] ;
	
	protected int [][][][][] mColorGradient_enabledFocusedPressedGlow = new int[2][2][2][2][] ;
	protected float [][][][][] mAlphaMultGradient_enabledFocusedPressedGlow = new float[2][2][2][2][] ;
	protected float [][][][][] mPositionsGradient_enabledFocusedPressedGlow = new float[2][2][2][2][] ;
	protected int [][][][] mSetGradient_enabledFocusedPressedGlow = new int[2][2][2][2] ;
	
	protected int [][][][][] mColorTopGradient_enabledFocusedPressedGlow = new int[2][2][2][2][] ;
	protected float [][][][][] mAlphaMultTopGradient_enabledFocusedPressedGlow = new float[2][2][2][2][] ;
	protected float [][][][][] mPositionsTopGradient_enabledFocusedPressedGlow = new float[2][2][2][2][] ;
	protected int [][][][] mSetTopGradient_enabledFocusedPressedGlow = new int[2][2][2][2] ;
	
	
	// Store the Paints we will use to draw each section, so we don't
	// need to make them fresh each time we call onDraw().  Although
	// the COLORS are not box-specific, paints are: 
	private Paint [][][][][][] mPaintSection_enabledFocusedPressedGlow = new Paint[NUM_BOXES][NUM_SECTIONS][2][2][2][2] ;
	private Paint [][][][][] mPaintGradient_enabledFocusedPressedGlow = new Paint[NUM_BOXES][2][2][2][2] ;
	private Paint [][][][][] mPaintTopGradient_enabledFocusedPressedGlow = new Paint[NUM_BOXES][2][2][2][2] ;
	
	// Color filters for special draw functions: these filters are not used in normal operation.
	// They are only applied for the special draw functions, such as drawWrapperAlpha.
	private ColorFilter mColorFilterToWhite ;
	
	
	
	private SupportsLongClickOracle mSupportsLongClickOracle ;
	private long mTimeLastPressed = 0 ;
	private long mTimeLongClickAlphaChange ;
	
	protected Style mStyle ;
	
	protected static final int CORNER_TOP_LEFT = 0 ;
	protected static final int CORNER_TOP_RIGHT = 1 ;
	protected static final int CORNER_BOTTOM_LEFT = 2 ;
	protected static final int CORNER_BOTTOM_RIGHT = 3 ;
	
	protected static final int SIDE_TOP = 0 ;
	protected static final int SIDE_LEFT = 1 ;
	protected static final int SIDE_RIGHT = 2 ;
	protected static final int SIDE_BOTTOM = 3 ;
	
	protected int mBlockFillColor ;
	protected int mBlockBorderColor ;
	
	// here are some drawables - gradients, basically.
	// we use these for enabled states, including pressed.
	protected GradientDrawable [] mBlockDrawableFull ;
	protected GradientDrawable [][] mBlockDrawableCorner ;
	protected GradientDrawable [][] mBlockDrawableSide ;
	
	// identical for standard rect and Alt rect
	protected Drawable mBlockDrawableContentShadow ;
	
	// identical for standard rect and Alt rect
	protected Drawable mFillImageDrawable ;
	
	protected GradientDrawable [] mBlockDrawableDisabledFull ;
	protected GradientDrawable [] mBlockDrawableDisabledBottom ;
	
	// alphas
	protected static final int INDEX_BORDER = 0 ;
	protected static final int INDEX_FILL = 1 ;
	protected static final int INDEX_FILL_MAX = 2 ;
	protected static final int INDEX_DISABLED = 3 ;
	
	protected int [] mBlockAlpha ;
	
	protected Paint mBlockPaintInnerEdge ;
	protected Paint mBlockPaintOuterEdge ;
	
	protected Rect mBlockRectInnerEdge ;
	protected Rect mBlockRectOuterEdge ;
	
	
	protected Handler mHandler ;
	protected Runnable mRunnablePostInvalidate ;
	
	protected Rect mTempRect = new Rect() ;
	

	// /////////////////////////////////////////////////////////////////////////
	// onMeasure, onLayout, onDraw
	//
	
	
	/**
	 * Returns a Rect representing the bottom border region for this button.
	 * 
	 * There is no guarantee that the returned Rect is newly created, or that
	 * it is not internal storage used by this object.  Make no changes, and
	 * do not rely on a constant value.
	 */
	synchronized public Rect getRectBorderBottom() {
		return mRect[BOX_STANDARD][RECT_BORDER_BOTTOM] ;
	}
	
	
	/**
	 * Returns a Rect representing the exterior border of the button.
	 * 
	 * There is no guarantee that the returned Rect is newly created, or that
	 * it is not internal storage used by this object.  Make no changes, and
	 * do not rely on a constant value.
	 */
	synchronized public Rect getRectBorder() {
		return mRect[BOX_STANDARD][RECT_BORDERED_CONTENT] ;
	}
	
	/**
	 * Returns a Rect representing the bottom border region for this button.
	 * 
	 * There is no guarantee that the returned Rect is newly created, or that
	 * it is not internal storage used by this object.  Make no changes, and
	 * do not rely on a constant value.
	 */
	synchronized public void copyRectBorderBottom( Rect r ) {
		r.set( mRect[BOX_STANDARD][RECT_BORDER_BOTTOM] ) ;
	}
	
	
	/**
	 * Returns a Rect representing the exterior border of the button.
	 * 
	 * There is no guarantee that the returned Rect is newly created, or that
	 * it is not internal storage used by this object.  Make no changes, and
	 * do not rely on a constant value.
	 */
	synchronized public void copyRectBorder( Rect r ) {
		r.set( mRect[BOX_STANDARD][RECT_BORDERED_CONTENT] ) ;
	}
	
	
	
	/**
	 * The ideal height of this button is the height of its contents when measured without
	 * limitations, plus the height of its extra information.  Careful when calling this
	 * method; calls 'measure' with certain parameters and thus can affect future calls to
	 * "draw."  It is good practice to invalidate() this view after getting its ideal height.
	 */
	synchronized public int getIdealHeight() {
		int height ;
		if ( mContent == null || !mMeasureContent )
			height = EMPTY_CONTENT_HEIGHT ;
		else {
			mContent.measure(
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)) ;
			height = mContent.getMeasuredHeight() ;
		}
		
		// Include padding and border heights; also make sure its within minimum bounds
		// Calculate the space occupied by border heights.  Note that because we sync
		// the content area between all boxes, the necessary "top height" is the max
		// of all block "top heights", and 
		int vertBorder = 0 ;
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			vertBorder = Math.max( vertBorder, mBorderWidthBySide[boxIndex].bottom + mBorderWidthBySide[boxIndex].top ) ;
		}
		
		return Math.max( Math.max( height + vertBorder, mMinTouchableHeight )
				+ mGradientHeight, mMinHeight )
				+ getPaddingTop() + getPaddingBottom() ;
	}
	
	
	@Override
	synchronized protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		//long time = System.currentTimeMillis() ;
		//Log.d(TAG, "onMeasure: " + widthMeasureSpec + ", " + heightMeasureSpec) ;
		// Measure the content.  This will, among other things, measure the child
		// in its current restrictions to see how large it WANTS to be, if it could fill
		// the space available to it by our parent (not including our extra shapes).
		if ( mContent != null )
			mContent.measure(widthMeasureSpec, heightMeasureSpec) ;

		int widthMode = MeasureSpec.getMode(widthMeasureSpec) ;
		int widthSize = MeasureSpec.getSize(widthMeasureSpec) ;
		int heightMode = MeasureSpec.getMode(heightMeasureSpec) ;
		int heightSize = MeasureSpec.getSize(heightMeasureSpec) ;
		
		int borderVertical = mBorderWidthBoundingBox.top + mBorderWidthBoundingBox.bottom ;
		int borderHorizontal = mBorderWidthBoundingBox.left + mBorderWidthBoundingBox.right ;
		
		
		// Log.d(TAG, "******* onMeasure.  " + widthMode + ":" + widthSize + ", " + heightMode + ":" + heightSize) ;
		// Log.d(TAG, "******* onMeasure border Vert / Horiz: " + borderVertical + " " + borderHorizontal) ;
		
		// Measurement is based on our LayoutParams and the provided measure specs.
		// If wrap_content, we wrap the child and add our necessary padding and draw
		// space.
		ViewGroup.LayoutParams layoutParams = this.getLayoutParams() ;
		int idealWidth = 0 ;
		int idealHeight = 0 ;
		if ( layoutParams.width == ViewGroup.LayoutParams.WRAP_CONTENT )
			idealWidth = ((mContent == null || !mMeasureContent) ? EMPTY_CONTENT_WIDTH : mContent.getMeasuredWidth())
						+ getPaddingLeft() + getPaddingRight() + borderHorizontal ;
		else if ( layoutParams.width == ViewGroup.LayoutParams.FILL_PARENT )
			idealWidth = Integer.MAX_VALUE ;
		else if ( layoutParams.width >= 0 )
			idealWidth = layoutParams.width ;
		else
			idealWidth = Integer.MAX_VALUE ;
		if ( layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT )
			idealHeight = ((mContent == null || !mMeasureContent) ? EMPTY_CONTENT_HEIGHT : mContent.getMeasuredHeight())
						+ getPaddingTop() + getPaddingBottom() + borderVertical + mGradientHeight ;
		else if ( layoutParams.height == ViewGroup.LayoutParams.FILL_PARENT )
			idealHeight = Integer.MAX_VALUE ;
		else if ( layoutParams.height >= 0 )
			idealHeight = layoutParams.height ;
		else
			idealHeight = Integer.MAX_VALUE ;
		
		// Log.d(TAG, "onMeasure; after measure child, ideal is " + idealWidth + ", " + idealHeight) ;
		// Log.d(TAG, "onMeasure; ideal height comprised of EMPTY_CONTENT_HEIGHT " + EMPTY_CONTENT_HEIGHT
		// 		+ ", mContent.getMeasuredHeight() " + mContent.getMeasuredHeight()
		// 		+ ", getPaddingTop() " + getPaddingTop()
		// 		+ ", getPaddingBottom() " + getPaddingBottom()
		// 		+ ", borderVertical " + borderVertical
		// 		+ ", mGradientHeight " + mGradientHeight ) ;
		
		// Make this at least the minimum size (counting padding and "drawn elements" like the gradient)
		idealWidth = Math.max(idealWidth, mMinWidth) ;
		idealHeight = Math.max(idealHeight, mMinHeight) ;
		// Make this at least the minimum touchable size (counting the border and background,
		// but not the gradient, drop shadow or the padding).  Do this by getting the max of
		// all drop shadow paddings.
		this.setToMaxInEachDimension( mTempBoundingBox, mDropShadowPadding ) ;
		idealWidth = Math.max(idealWidth, mMinTouchableWidth + getPaddingLeft() + getPaddingRight() + mTempBoundingBox.left + mTempBoundingBox.right) ;
		idealHeight = Math.max(idealHeight, mMinTouchableHeight + getPaddingTop() + getPaddingBottom() + mGradientHeight + mTempBoundingBox.top + mTempBoundingBox.bottom) ;
		
		//Log.d(TAG, "onMeasure; after minDim, ideal is " + idealWidth + ", " + idealHeight) ;
		
		// if our proportions are set to SQUARE_GROW, this is where we should
		// grow.  We might exceed the limits of our measure spec, but that's fine;
		// we'll shrink back to fit in a bit.
		if ( mProportions == Proportions.SQUARE_GROW ) {
			int widthExtra = getPaddingLeft() + getPaddingRight() ;
			int heightExtra = getPaddingTop() + getPaddingBottom() + mGradientHeight ;
			
			int diff = (idealWidth + widthExtra) - (idealHeight + heightExtra) ;
			// if diff is positive, width is wider and height should be adjusted by +diff.
			// if diff is negative, height is higher and width should be adjusted by -diff.
			if ( diff > 0 )
				idealHeight += diff ;
			else if ( diff < 0 )
				idealWidth -= diff ;
		}
		
		// Log.d(TAG, "onMeasure; after squareGrow, ideal is " + idealWidth + ", " + idealHeight) ;
		
		// Now truncate idealWidth/idealHeight according to the measure specs given.
		int width, height ;
		if ( widthMode == View.MeasureSpec.AT_MOST )
			width = Math.min(idealWidth, widthSize) ;
		else if ( widthMode == View.MeasureSpec.EXACTLY )
			width = widthSize ;
		else
			width = idealWidth ;
		if ( heightMode == View.MeasureSpec.AT_MOST )
			height = Math.min(idealHeight, heightSize) ;
		else if ( heightMode == View.MeasureSpec.EXACTLY )
			height = heightSize ;
		else
			height = idealHeight ;
		
		// Log.d(TAG, "onMeasure; after measureSpec, dims are " + width + ", " + height) ;
		
		
		// 'height' and 'width' are now our measured dimensions.  This is a good opportunity
		// to finally "square up" the result if we have SQUARE_* as our proportions.
		// We have already grown if "square_grow" is in place; now our dimensions
		// are the best-fit available in our measure spec requirements, so we must shrink.
		if ( mProportions == Proportions.SQUARE_GROW || mProportions == Proportions.SQUARE_SHRINK ) {
			int widthExtra = getPaddingLeft() + getPaddingRight() ;
			int heightExtra = getPaddingTop() + getPaddingBottom() + mGradientHeight ;
			
			// Log.d(TAG, "square shrink: " + getPaddingLeft() + ", " + getPaddingRight() + ", " + getPaddingTop() + ", " + getPaddingBottom() + ", " + mGradientHeight) ;
			
			int diff = (width + widthExtra) - (height + heightExtra) ;
			// if diff is positive, width is wider and should be adjusted by -diff.
			// if diff is negative, height is higher should be adjusted by +diff.
			if ( diff > 0 )
				width -= diff ;
			else if ( diff < 0 )
				height += diff ;
		}
		
		// Log.d(TAG, "onMeasure; after squareShrink, dims are " + width + ", " + height) ;
		
			
		// We now have the height and width we should set for our measured
		// size.  Tell the child of its new dimensions.
		if ( mContent != null ) {
			//int childHeight = mContent.getMeasuredHeight() ;
			this.setToMaxInEachDimension( mTempBoundingBox, mDropShadowPadding ) ;
			int w = width - getPaddingLeft() - getPaddingRight() - (borderHorizontal) - (mTempBoundingBox.left + mTempBoundingBox.right) ;
			int h = height - getPaddingTop() - getPaddingBottom() - (borderVertical) - (mTempBoundingBox.top + mTempBoundingBox.bottom) - mGradientHeight ;
			mContent.measure(
					View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY) ) ;
		}
		this.setMeasuredDimension(width, height) ;
	}
	
	
	protected Rect mTempBoundingBox = new Rect() ;
	protected Rect mTempBoundingBox2 = new Rect() ;
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		//long time = System.currentTimeMillis() ;
		//Log.d(TAG, "calling super onLayout") ;
		//super.onLayout(changed, l, t, r, b) ;
		//Log.d(TAG, "onLayout " + l + ", " + t + ", " + r + ", " + b) ;
		
		if ( mBackgroundDrawable != null && mBackgroundDrawable instanceof LineShadingDrawable ) {
			int [] location = new int[2] ;
			getLocationOnScreen( location ) ;
			((LineShadingDrawable)mBackgroundDrawable).setOrigin(location[0], location[1]) ;
		}
		
		//Log.d(TAG, "" + this + " on Layout " + changed + ", "+ l + ", "+ t + ", " + r + ", " + b) ;
		
		// We do two things here.  First, if changed, resize our draw
		// rectangles.  Second, calculate the boundaries of our content
		// area, and call layout on our child.
		if ( changed ) {
			// Initially, we sized our drawable rectangles as relative to the parent
			// (i.e., we started at l,t,r,b and offset from there).
			// Try setting our drawable rectangles  as relative to OURSELF, with (0,0)
			// in the top-left.
			// THIS WORKS!
			int left = getPaddingLeft() ;
			int right = (r-l) - getPaddingRight() ;
			int top = getPaddingTop() ;
			int bottom = (b-t) - getPaddingBottom() ;
			
			this.setToMaxInEachDimension(mTempBoundingBox2, mDropShadowPadding) ;
			
			// SQUARE.  If we are using a Square layout, this is where we make a final adjustment.
			// We need to inset (slightly?) to make a square; we've lost the opportunity to grow
			// to form a square.  We use the BOX_STANDARD sizing to line this up.
			int boxExtraLeftInset = 0, boxExtraRightInset = 0, boxExtraTopInset = 0, boxExtraBottomInset = 0 ;
			if ( mProportions == Proportions.SQUARE_GROW || mProportions == Proportions.SQUARE_SHRINK ) {
				int w = right - left - mTempBoundingBox2.right - mTempBoundingBox2.left ;
				int h = bottom - top - Math.max(mTempBoundingBox2.bottom, mGradientHeight) - mTempBoundingBox2.top ;
				if ( w > h ) {
					float halfDiff = (w-h) / 2.0f ;
					boxExtraLeftInset = (int)Math.floor( halfDiff ) ;
					boxExtraRightInset = (int)Math.ceil( halfDiff ) ;
				} else if ( h > w ) {
					float halfDiff = (h-w) / 2.0f ;
					boxExtraTopInset = (int)Math.floor( halfDiff ) ;
					boxExtraBottomInset = (int)Math.ceil( halfDiff ) ;
				}
			}
			
			// Where's the box go?
			// remember: tempBoundingBox2 currently set to the max of all drop shadow padding.
			mTempBoundingBox.left = left + mTempBoundingBox2.left + boxExtraLeftInset;
			mTempBoundingBox.right = right - mTempBoundingBox2.right - boxExtraRightInset ;
			mTempBoundingBox.top = top + mTempBoundingBox2.top + boxExtraTopInset ;
			mTempBoundingBox.bottom = bottom - Math.max(mTempBoundingBox2.bottom, mGradientHeight) - boxExtraBottomInset ;
			
			// this is a containing box for all content, including gradients, content, etc.
			// Each box has its own border width, with the max in each direction stored
			// in mBorderWidthBoundingBox.  The difference between this max and a particular
			// box's border width is the corresponding INSET for that box (since we match the
			// content area of each box, not the outer borders).
			for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
				mTempBoundingBox2.set(mTempBoundingBox) ;
				// perform inset...
				mTempBoundingBox2.left += ( mBorderWidthBoundingBox.left - mBorderWidthBySide[boxIndex].left ) ;
				mTempBoundingBox2.top += ( mBorderWidthBoundingBox.top - mBorderWidthBySide[boxIndex].top ) ;
				mTempBoundingBox2.right -= ( mBorderWidthBoundingBox.right - mBorderWidthBySide[boxIndex].right ) ;
				mTempBoundingBox2.bottom -= ( mBorderWidthBoundingBox.bottom - mBorderWidthBySide[boxIndex].bottom ) ;
				// set the rects at this bounding box
				setBoxRect( mRect[boxIndex], mBorderWidthBySide[boxIndex], mTempBoundingBox2 ) ;
				//Log.d(TAG, "onLayout have bounded at " + mBorderWidthBoundingBox + " inset to "+ mTempBoundingBox2 + " by borderWidth " + mBorderWidthBySide[boxIndex]) ;
				//for ( int i = 0; i < NUM_RECTS; i++ ) {
				//	Log.d(TAG, "onLayout has rect " + boxIndex + ", " + i + " at " + mRect[boxIndex][i]) ;
				//}
			}
			
			// Generate new gradient paints - we may have changed the gradient rectangle!
			generateGradientPaints() ;

			setBlockDrawableBounds() ;
			setDropShadowDrawables(false, 0, 0) ;
		}
		
		
		// Now call layout on our child.  Values passed to layout
		// should be relative to this view.
		if ( mContent != null ) {
			
			// if we did everything right, both box and alt-box should have exactly
			// the same content area.
			
			//Log.d(TAG, "onLayout layout content at " + mRect[BOX_STANDARD][RECT_CONTENT]) ;
			
			mContent.layout(
					mRect[BOX_STANDARD][RECT_CONTENT].left,
					mRect[BOX_STANDARD][RECT_CONTENT].top,
					mRect[BOX_STANDARD][RECT_CONTENT].right,
					mRect[BOX_STANDARD][RECT_CONTENT].bottom) ;
		}
		
		//Log.d(TAG, "onLayout took " + (System.currentTimeMillis() - time)) ;
	}
	
	
	private void setBoxRect( Rect [] rects, Rect borderSizes, Rect boundingBox ) {
		// Gradient: full left to right, snug at bottom, height is mGradientHeight
		rects[RECT_GRADIENT].left = boundingBox.left ;
		rects[RECT_GRADIENT].right = boundingBox.right ;
		rects[RECT_GRADIENT].bottom = boundingBox.bottom + mGradientHeight ;
		rects[RECT_GRADIENT].top = boundingBox.bottom ;
		// BorderBottom: full left to right, bottom is gradient top, height is given
		rects[RECT_BORDER_BOTTOM].left = boundingBox.left ;
		rects[RECT_BORDER_BOTTOM].right = boundingBox.right ;
		rects[RECT_BORDER_BOTTOM].bottom = boundingBox.bottom ;
		rects[RECT_BORDER_BOTTOM].top = boundingBox.bottom - borderSizes.bottom ;
		// BorderTop: full left to right, snug at top, height is given.
		rects[RECT_BORDER_TOP].left = boundingBox.left ;
		rects[RECT_BORDER_TOP].right = boundingBox.right ;
		rects[RECT_BORDER_TOP].top = boundingBox.top ;
		rects[RECT_BORDER_TOP].bottom = boundingBox.top + borderSizes.top ;
		// BorderLeft and BorderRight: top and bottom given by BorderTop, BorderBottom.
		// Outer edge snug, inner edge inset by mBorderWidth.
		rects[RECT_BORDER_LEFT].top = rects[RECT_BORDER_RIGHT].top = rects[RECT_BORDER_TOP].bottom ;
		rects[RECT_BORDER_LEFT].bottom = rects[RECT_BORDER_RIGHT].bottom = rects[RECT_BORDER_BOTTOM].top ;
		rects[RECT_BORDER_LEFT].left = boundingBox.left ;
		rects[RECT_BORDER_LEFT].right = boundingBox.left + borderSizes.left ;
		rects[RECT_BORDER_RIGHT].right = boundingBox.right ;
		rects[RECT_BORDER_RIGHT].left = boundingBox.right - borderSizes.right ;
		// The "top" gradient.  This gradient matches the dimensions of the button,
		// including the left, right and top.
		rects[RECT_TOP_GRADIENT].bottom = rects[RECT_BORDER_BOTTOM].top ;
		rects[RECT_TOP_GRADIENT].left = rects[RECT_BORDER_LEFT].left ;
		rects[RECT_TOP_GRADIENT].top = rects[RECT_BORDER_TOP].top ;
		rects[RECT_TOP_GRADIENT].right = rects[RECT_BORDER_RIGHT].right ;
		// Finally, the content.  This rectangle abuts the border on all sides.
		rects[RECT_CONTENT].top = rects[RECT_BORDER_TOP].bottom ;
		rects[RECT_CONTENT].left = rects[RECT_BORDER_LEFT].right ;
		rects[RECT_CONTENT].right = rects[RECT_BORDER_RIGHT].left ;
		rects[RECT_CONTENT].bottom = rects[RECT_BORDER_BOTTOM].top ;
		
		//Log.d(TAG, "onLayout set content "+ rects[RECT_CONTENT]) ;
		
		// Bordered content.  This is not drawn; it is a convenient holder
		// for the "touchable" area.
		rects[RECT_BORDERED_CONTENT].top = rects[RECT_BORDER_TOP].top ;
		rects[RECT_BORDERED_CONTENT].left = rects[RECT_BORDER_LEFT].left ;
		rects[RECT_BORDERED_CONTENT].right = rects[RECT_BORDER_RIGHT].right ;
		rects[RECT_BORDERED_CONTENT].bottom = rects[RECT_BORDER_BOTTOM].bottom ;
	}
	

	@Override
	synchronized public void onDraw( Canvas canvas ) {
		if ( mDrawWrapperOnDraw )
			drawWrapper( canvas, getBox() ) ;
		
		// draw content.  Automatically; we're a view-group.
	}
	
	
	synchronized public void drawWrapper( Canvas canvas ) {
		drawWrapper( canvas, getBox() ) ;
	}
	
	synchronized public void drawDropShadow( Canvas canvas ) {
		drawDropShadow( canvas, getBox() ) ;
	}
	
	synchronized private void drawWrapper( Canvas canvas, int boxIndex ) {
		
		if ( mHideUnpressed && !isPressed() )
			return ;
		
		switch( mStyle ) {
		case PLAIN:
			drawStandardWrapper( canvas, boxIndex, null, null ) ;
			break ;
		case BLOCK_UNDERLINE:
			drawBlockUnderline( canvas, boxIndex ) ;
			break ;
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT:
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
			drawBlockEncloseContent( canvas, boxIndex ) ;
			break ;
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
			drawBlockUnderlineEncloseContent( canvas, boxIndex ) ;
			break ;
		case BLOCK_TWO_TIER_ALWAYS_ENCLOSE_CONTENT:
			drawBlockAlwaysEncloseContent( canvas, boxIndex ) ;
			break ;
		}
	}
	
	synchronized private void drawDropShadow( Canvas canvas, int boxIndex ) {
		
		if ( mHideUnpressed && !isPressed() )
			return ;
		
		switch( mStyle ) {
		case PLAIN:
			break ;
		case BLOCK_UNDERLINE:
			drawBlockUnderlineDropShadow( canvas, boxIndex ) ;
			break ;
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT:
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
			drawBlockEncloseContentDropShadow( canvas, boxIndex ) ;
			break ;
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
			drawBlockUnderlineEncloseContentDropShadow( canvas, boxIndex ) ;
			break ;
		case BLOCK_TWO_TIER_ALWAYS_ENCLOSE_CONTENT:
			drawBlockAlwaysEncloseContentDropShadow( canvas, boxIndex ) ;
			break ;
		}
	}
	
	synchronized public void drawBlockUnderline( Canvas canvas ) {
		drawBlockUnderline( canvas, getBox() ) ;
	}
	
	synchronized private void drawBlockUnderline( Canvas canvas, int boxIndex ) {
		
		
		// Bug: on slow phones (e.g. Nexus S) we get a weird bright->dim->bright issue with the button.
		if ( isPressed() && isEnabled() && mTimeLastPressed == 0 )
			mTimeLastPressed = System.currentTimeMillis() ;
		else if ( !(isPressed() && isEnabled()) && mTimeLastPressed > 0 )
			mTimeLastPressed = 0 ;
		
		
		// states:
		// pressed: draw the box.  Fill alpha is determined by the time its been pressed and whether we respond to long-presses.
		// selected: draw the fill and the bottom border
		// enabled: draw the bottom border
		// disabled selected: draw the disabled fill and disabled bottom border
		// disabled: draw the disabled bottom border
		
		// draw glow
		int e = isEnabled() ? 1 : 0 ;
		int f = isFocused() ? 1 : 0 ;
		int p = isPressed() && e==1 ? 1 : 0 ;
		int g = getGlow() ? 1 : 0 ;
		
		canvas.drawRect( mRect[boxIndex][RECT_TOP_GRADIENT], this.mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g] ) ;
		
		if ( isPressed() && isEnabled() ) {
			// alpha?
			int fillAlpha = this.mBlockAlpha[INDEX_FILL] ;
			long timePressed = System.currentTimeMillis() - this.mTimeLastPressed ;
			if ( mSupportsLongClickOracle != null && mSupportsLongClickOracle.supportsLongClick(this) ) {
				if ( timePressed >= this.mTimeLongClickAlphaChange )
					fillAlpha = this.mBlockAlpha[INDEX_FILL_MAX] ;
				else {
					double a = ((double)timePressed) / mTimeLongClickAlphaChange ;
					fillAlpha = (int)Math.round( (1 - a) * mBlockAlpha[INDEX_FILL] + a * mBlockAlpha[INDEX_FILL_MAX] ) ;
					
					mHandler.postDelayed(mRunnablePostInvalidate, 30) ;
				}
			}
			
			mBlockDrawableFull[boxIndex].setAlpha(fillAlpha) ;
			
			// set shadow bounds (useful because multiple buttons might use the same drawable for this)
			// Content rects should be identical for mRect and its alternate.
			mBlockDrawableContentShadow.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
			
			// draw everything.  We draw the full, then the shadow (set bounds!), then border corners and sides.
			mBlockDrawableFull[boxIndex].draw(canvas) ;
			if ( mFillDrawable != null ) {
				mFillDrawable.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
				mFillDrawable.draw(canvas) ;
			}
			mBlockDrawableContentShadow.draw(canvas) ;
			for ( int i = 0; i < this.mBlockDrawableSide[boxIndex].length; i++ )
				mBlockDrawableSide[boxIndex][i].draw(canvas) ;
			for ( int i = 0; i < this.mBlockDrawableCorner[boxIndex].length; i++ )
				mBlockDrawableCorner[boxIndex][i].draw(canvas) ;
		}
		
		else if ( isEnabled() && isSelected() ) {
			mBlockDrawableFull[boxIndex].setAlpha( mBlockAlpha[INDEX_FILL] ) ;
			// now draw
			mBlockDrawableFull[boxIndex].draw(canvas) ;
			if ( mFillDrawable != null && mFillDrawableUnpressed ) {
				mFillDrawable.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
				mFillDrawable.draw(canvas) ;
			}
			// draw bottom side and corners
				mBlockDrawableSide[boxIndex][SIDE_BOTTOM].draw(canvas) ;
				mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_LEFT].draw(canvas) ;
				mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_RIGHT].draw(canvas) ;
		}
		
		else if ( isEnabled() ) {
			if ( mFillDrawable != null && mFillDrawableUnpressed ) {
				mFillDrawable.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
				mFillDrawable.draw(canvas) ;
			}
			// draw bottom side and corners
			mBlockDrawableSide[boxIndex][SIDE_BOTTOM].draw(canvas) ;
			mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_LEFT].draw(canvas) ;
			mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_RIGHT].draw(canvas) ;
		}
		
		else if ( isSelected() ) {
			// selected but disabled.  Draw disabled parts.
			mBlockDrawableDisabledFull[boxIndex].draw(canvas) ;
			mBlockDrawableDisabledBottom[boxIndex].draw(canvas) ;
		}
		
		else {
			// disabled.  Draw the disabled bottom.
			mBlockDrawableDisabledBottom[boxIndex].draw(canvas) ;
		}
		
		canvas.drawRect(mBlockRectInnerEdge, mBlockPaintInnerEdge) ;
		canvas.drawRect(mBlockRectOuterEdge, mBlockPaintOuterEdge) ;
		
		// draw drop shadow.  It's over everything else.
		if ( mDropShadow ) {
			drawBlockUnderlineDropShadow( canvas, boxIndex ) ;
		}
	}
	
	synchronized private void drawBlockUnderlineDropShadow( Canvas canvas, int boxIndex ) {
		// draw drop shadow.  It's over everything else.
		if ( isEnabled() ) {
			if ( isPressed() && mDropShadowDrawable[boxIndex] != null ) {
				mDropShadowDrawable[boxIndex].draw(canvas) ;
			} else if ( mDropShadowBottomDrawable != null ) {
				mDropShadowBottomDrawable[boxIndex].draw(canvas) ;
			}
		}
	}
	
	synchronized private void drawBlockEncloseContent( Canvas canvas, int boxIndex) {
		drawBlockEncloseContent( canvas, boxIndex, false, false ) ;
	}
	
	synchronized private void drawBlockUnderlineEncloseContent( Canvas canvas, int boxIndex) {
		drawBlockEncloseContent( canvas, boxIndex, true, false ) ;
	}
	
	synchronized private void drawBlockAlwaysEncloseContent( Canvas canvas, int boxIndex) {
		drawBlockEncloseContent( canvas, boxIndex, false, true ) ;
	}
	
	synchronized private void drawBlockEncloseContentDropShadow( Canvas canvas, int boxIndex) {
		drawBlockEncloseContentDropShadow( canvas, boxIndex, false, false ) ;
	}
	
	synchronized private void drawBlockUnderlineEncloseContentDropShadow( Canvas canvas, int boxIndex) {
		drawBlockEncloseContentDropShadow( canvas, boxIndex, true, false ) ;
	}
	
	synchronized private void drawBlockAlwaysEncloseContentDropShadow( Canvas canvas, int boxIndex) {
		drawBlockEncloseContentDropShadow( canvas, boxIndex, false, true ) ;
	}
	
	
	synchronized private void drawBlockEncloseContent( Canvas canvas, int boxIndex, boolean underlineOptional, boolean encloseOptional ) {
		
		
		// Bug: on slow phones (e.g. Nexus S) we get a weird bright->dim->bright issue with the button.
		if ( isPressed() && isEnabled() && mTimeLastPressed == 0 )
			mTimeLastPressed = System.currentTimeMillis() ;
		else if ( !(isPressed() && isEnabled()) && mTimeLastPressed > 0 )
			mTimeLastPressed = 0 ;
		

		// draw glow
		int e = isEnabled() ? 1 : 0 ;
		int f = isFocused() ? 1 : 0 ;
		int p = isPressed() && e==1 ? 1 : 0 ;
		int g = getGlow() ? 1 : 0 ;
		
		canvas.drawRect( mRect[boxIndex][RECT_TOP_GRADIENT], this.mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g] ) ;
		
		
		// states:
		// pressed: draw the box.  Fill alpha is determined by the time its been pressed and whether we respond to long-presses.
		// otherwise:
		// 		if SELECTED, draw the fill.
		// 		if ACTIVE or EXCLUSIVE, draw the box w/ shadow.
		// selected: draw the fill and the bottom border
		// enabled: draw the bottom border
		// disabled selected: draw the disabled fill and disabled bottom border
		// disabled: draw the disabled bottom border
		
		boolean boxShown = false ;
		boolean underlineShown = false ;
		
		if ( isPressed() && isEnabled() ) {
			// alpha?
			int fillAlpha = this.mBlockAlpha[INDEX_FILL] ;
			long timePressed = System.currentTimeMillis() - this.mTimeLastPressed ;
			if ( mSupportsLongClickOracle != null && mSupportsLongClickOracle.supportsLongClick(this) ) {
				if ( timePressed >= this.mTimeLongClickAlphaChange )
					fillAlpha = this.mBlockAlpha[INDEX_FILL_MAX] ;
				else {
					double a = ((double)timePressed) / mTimeLongClickAlphaChange ;
					fillAlpha = (int)Math.round( (1 - a) * mBlockAlpha[INDEX_FILL] + a * mBlockAlpha[INDEX_FILL_MAX] ) ;
					
					mHandler.postDelayed(mRunnablePostInvalidate, 30) ;
				}
			}
			
			mBlockDrawableFull[boxIndex].setAlpha(fillAlpha) ;
			
			// set shadow bounds (useful because multiple buttons might use the same drawable for this)
			// Content rects should be identical for mRect and its alternate.
			mBlockDrawableContentShadow.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
			
			// draw everything.  We draw the full, then the shadow (set bounds!), then border corners and sides.
			mBlockDrawableFull[boxIndex].draw(canvas) ;
			if ( mFillDrawable != null ) {
				mFillDrawable.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
				mFillDrawable.draw(canvas) ;
			}
			mBlockDrawableContentShadow.draw(canvas) ;
			for ( int i = 0; i < this.mBlockDrawableSide[boxIndex].length; i++ )
				mBlockDrawableSide[boxIndex][i].draw(canvas) ;
			for ( int i = 0; i < this.mBlockDrawableCorner[boxIndex].length; i++ )
				mBlockDrawableCorner[boxIndex][i].draw(canvas) ;
			
			boxShown = true ;
		}
		
		else {
			if ( isSelected() ) {
				mBlockDrawableFull[boxIndex].setAlpha( mBlockAlpha[INDEX_FILL] ) ;
				// now draw
				mBlockDrawableFull[boxIndex].draw(canvas) ;
			}
			
			if ( mContentState == ContentState.EXCLUSIVE || mContentState == ContentState.ACTIVE
					|| ( mContentState == ContentState.OPTIONAL && encloseOptional ) ) {
				// draw everything.  We draw the full, then the shadow (set bounds!), then border corners and sides.
				if ( mFillDrawable != null ) {
					mFillDrawable.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
					mFillDrawable.draw(canvas) ;
				}
				
				// set shadow bounds (useful because multiple buttons might use the same drawable for this)
				// Content rects should be identical for mRect and its alternate.
				mBlockDrawableContentShadow.setBounds(mRect[boxIndex][RECT_CONTENT]) ;
				mBlockDrawableContentShadow.draw(canvas) ;
				
				for ( int i = 0; i < this.mBlockDrawableSide[boxIndex].length; i++ )
					mBlockDrawableSide[boxIndex][i].draw(canvas) ;
				for ( int i = 0; i < this.mBlockDrawableCorner[boxIndex].length; i++ )
					mBlockDrawableCorner[boxIndex][i].draw(canvas) ;
				
				boxShown = true ;
			} else if ( mContentState == ContentState.OPTIONAL && underlineOptional ) {
				// draw bottom side and corners
				mBlockDrawableSide[boxIndex][SIDE_BOTTOM].draw(canvas) ;
				mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_LEFT].draw(canvas) ;
				mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_RIGHT].draw(canvas) ;
				
				underlineShown = true ;
			}
		}
		
		canvas.drawRect(mBlockRectInnerEdge, mBlockPaintInnerEdge) ;
		canvas.drawRect(mBlockRectOuterEdge, mBlockPaintOuterEdge) ;
		
		// draw drop shadow.  It's over everything else.
		if ( mDropShadow ) {
			drawBlockEncloseContentDropShadow( canvas, boxIndex, underlineOptional, encloseOptional ) ;
		}
	}
	
	
	synchronized private void drawBlockEncloseContentDropShadow( Canvas canvas, int boxIndex, boolean underlineOptional, boolean encloseOptional ) {
		boolean boxShown = false ;
		boolean underlineShown = false ;
		
		if ( isPressed() && isEnabled() ) {
			boxShown = true ;
		} else {
			switch( mContentState ) {
			case EXCLUSIVE:
			case ACTIVE:
				boxShown = true ;
				break ;
			case OPTIONAL:
				underlineShown = underlineOptional ;
				boxShown = encloseOptional ;
				break ;
			}
		}
		if ( boxShown && mDropShadowDrawable[boxIndex] != null ) {
			mDropShadowDrawable[boxIndex].draw(canvas) ;
		} else if ( underlineShown && mDropShadowBottomDrawable[boxIndex] != null ) {
			mDropShadowBottomDrawable[boxIndex].draw(canvas) ;
		}
	}
	
	
	synchronized public void drawStandardWrapper( Canvas canvas, ColorFilter cf, Xfermode xf ) {
		drawStandardWrapper( canvas, getBox(), cf, xf ) ;
	}
	
	synchronized private void drawStandardWrapper( Canvas canvas, int boxIndex, ColorFilter cf, Xfermode xf ) {
		// FIRST: Determine our state.
		// SECOND: Paint our rectangles, using the appropriate Paints for our state.
		// THIRD: we don't need to draw our children; draw() is taking care of tha
		
		int e = isEnabled() ? 1 : 0 ;
		int f = isFocused() ? 1 : 0 ;
		int p = isPressed() && e==1 ? 1 : 0 ;
		int g = getGlow() ? 1 : 0 ;
		
		// Set our filters and xfer mode
		if ( cf != null ) {
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BACKGROUND][e][f][p][g].setColorFilter(cf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setColorFilter(cf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setColorFilter(cf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setColorFilter(cf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER_BOTTOM][e][f][p][g].setColorFilter(cf) ;
			this.mPaintGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setColorFilter(cf) ;
			this.mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setColorFilter(cf) ;
		}
		if ( xf != null ) {
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BACKGROUND][e][f][p][g].setXfermode(xf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setXfermode(xf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setXfermode(xf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setXfermode(xf) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER_BOTTOM][e][f][p][g].setXfermode(xf) ;
			this.mPaintGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setXfermode(xf) ;
			this.mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setXfermode(xf) ;
		}
		
		// Draw some stuff
		canvas.drawRect( mRect[boxIndex][RECT_CONTENT], this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BACKGROUND][e][f][p][g] ) ;
		canvas.drawRect( mRect[boxIndex][RECT_BORDER_LEFT], this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g] ) ;
		canvas.drawRect( mRect[boxIndex][RECT_BORDER_RIGHT], this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g] ) ;
		canvas.drawRect( mRect[boxIndex][RECT_BORDER_TOP], this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g] ) ;
		canvas.drawRect( mRect[boxIndex][RECT_BORDER_BOTTOM], this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER_BOTTOM][e][f][p][g] ) ;
		canvas.drawRect( mRect[boxIndex][RECT_GRADIENT], this.mPaintGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g] ) ;
		canvas.drawRect( mRect[boxIndex][RECT_TOP_GRADIENT], this.mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g] ) ;
	
		// Reset our ColorFilter and Xfermode.
		if ( cf != null ) {
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BACKGROUND][e][f][p][g].setColorFilter(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setColorFilter(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setColorFilter(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setColorFilter(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER_BOTTOM][e][f][p][g].setColorFilter(null) ;
			this.mPaintGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setColorFilter(null) ;
			this.mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setColorFilter(null) ;
		}
		if ( xf != null ) {
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BACKGROUND][e][f][p][g].setXfermode(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setXfermode(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setXfermode(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER][e][f][p][g].setXfermode(null) ;
			this.mPaintSection_enabledFocusedPressedGlow[boxIndex][SECTION_BORDER_BOTTOM][e][f][p][g].setXfermode(null) ;
			this.mPaintGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setXfermode(null) ;
			this.mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g].setXfermode(null) ;
		}
	}
	
	
	synchronized public void drawWrapperAlpha( Canvas canvas ) {
		drawWrapperAlpha( canvas, getBox() ) ;
	}
	
	/**
	 * Like drawWrapper, but instead of using the normal color and alpha settings, draws
	 * itself as fully white, maintaining the alpha values of the wrapper gradients and rectangles.
	 * @param canvas
	 */
	synchronized private void drawWrapperAlpha( Canvas canvas, int boxIndex ) {
		// Simple enough.  Normally we do not have color filters for the object paints.
		// Apply a LightingColorFilter (mult = white, add = white) - this brings all pixels to
		// white.  Draw with this filter.  Finally, remove the filter so this call does not effect
		// future draws.
		
		drawStandardWrapper( canvas, boxIndex, mColorFilterToWhite, null ) ;
	}
	
	synchronized public void drawWrapperAlphaMask( Canvas canvas ) {
		drawWrapperAlphaMask( canvas, getBox() ) ;
	}
	
	/**
	 * Like drawWrapper, but instead of using the normal color and alpha settings, draws
	 * a fully opaque representation of its alpha channel in opaque pixels, using black as
	 * "fully transparent" and white as "fully opaque."
	 * @param canvas
	 */
	synchronized private void drawWrapperAlphaMask( Canvas canvas, int boxIndex ) {
		canvas.drawRGB(0, 0, 0) ;
		drawStandardWrapper( canvas, boxIndex, mColorFilterToWhite, null ) ;
	}
	
	
	
	synchronized public void setSupportsLongClickOracle( SupportsLongClickOracle slco ) {
		mSupportsLongClickOracle = slco ;
	}
	
	
	// BUG FIX: Although we prefer to use native handling for clicks and longClicks, 
	// doing so makes "onClick" very difficult to trigger.  It seems that any delay between
	// down and up causes a failure to "click", whereas we prefer a "click" trigger if
	// the user releases their finger any time before a longClick (assuming they haven't
	// moved their finger off the button in the mean time).
	//
	// To avoid redundancy, we short-circuit native processing for clicks, while allowing
	// them for longClicks.  If the event is a ACTION_UP, or an ACTION_MOVE that moves the
	// MotionEvent outside the button bounds, we cancelLongPress.  If it's up, within the
	// bounds, and we are still "performing touch", call performClick() and return true
	// (do not pass the MotionEvent to super.)
	
	boolean performingTouch = false ;
	
	@Override
	synchronized public boolean performClick() {
		return super.performClick() ;
	}
	
	@Override
	synchronized public boolean performLongClick() {
		boolean didConsume = super.performLongClick() ;
		if ( didConsume ) {
			this.setPressed(false) ;
			performingTouch = false ;
		}
		return didConsume ;
	}
	
	
	
	/**
	 * We override onTouchEvent so we can call setPressed() to adjust our draw settings.
	 * We still pass the event to super(), so that onClick and onLongClick fire appropriately.
	 * The only time we interfere with this process is when the touch moves outside the bounds
	 * 
	 */
	@Override
	synchronized public boolean onTouchEvent(MotionEvent event) {
		// touches are registered according to STANDARD box.
		int action = event.getAction();
		boolean within = mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].left <= event.getX() && event.getX() <= mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].right 
						&& mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].top <= event.getY() && event.getY() <= mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].bottom ;
		for ( int i = 0; i < event.getHistorySize(); i++ ) {
			if ( !within )
				break ;
			float x = event.getHistoricalX(i) ;
			float y = event.getHistoricalY(i) ;
			within = mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].left <= x && x <= mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].right 
					&& mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].top <= y && y <= mRect[BOX_STANDARD][RECT_BORDERED_CONTENT].bottom ;
		}
		
		// within now holds whether the ENTIRE EVENT took place within the button area.
		if ( !within ) {
			this.cancelLongPress() ;
			this.performingTouch = false ;
			this.setPressed(false) ;
			return false ;
		}
		
		if ( !this.isEnabled() ) {
			this.cancelLongPress() ;
			this.performingTouch = false ;
			this.setPressed(false) ;
			return false ;
		}
		
		if ( action == MotionEvent.ACTION_DOWN ) {
			performingTouch = true ;
			this.setPressed(true) ;
		}
		else if ( !performingTouch )
			return false ;		// discard
		
		if ( action == MotionEvent.ACTION_CANCEL ) {
			performingTouch = false ;
			this.setPressed(false) ;
		}
		
		if ( action == MotionEvent.ACTION_UP ) {
			// Click?
			if ( performingTouch ) {
				this.performClick() ;
			}
			this.cancelLongPress() ;
			this.performingTouch = false ;
			this.setPressed(false) ;
			// fall through so the long-press timer sees this too.
			// It appears that canceling the long-press also cancel the touch,
			// so this will NOT call performClick() twice.
		}
		
		if ( action == MotionEvent.ACTION_OUTSIDE ) {
			//Log.d(TAG, "onTouchEvent: ACTION_OUTSIDE") ;
		}
		return super.onTouchEvent(event) ;
	}
	
	
	synchronized public void invalidateContent( View v ) {
		// for now, invalidate our entire content
		// TODO: If this becomes a problem, maybe try locating a smaller rect
		// within the button to invalidate?
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			postInvalidate( mRect[boxIndex][RECT_CONTENT].left, mRect[boxIndex][RECT_CONTENT].top, mRect[boxIndex][RECT_CONTENT].right, mRect[boxIndex][RECT_CONTENT].bottom ) ;
		}
	}
	
	synchronized public View getContentView() {
		return mContent ;
	}
	
	synchronized protected void setContentView( View v ) {
		if ( mContent != null ) {
			setContainingButtonOnChildren(mContent, false) ;
		}
		removeAllViews() ;
		mContent = v ;
		// loop for implementations of our interface...
		if ( mContent != null ) {
			setContainingButtonOnChildren(mContent, true) ;
			addView(mContent) ;
		}
	}
	
	synchronized private void setContainingButtonOnChildren( View v, boolean set ) {
		if ( v instanceof SelfUpdatingButtonContent )
			if ( set )
				((SelfUpdatingButtonContent) v).setContainingButton(this) ;
			else
				((SelfUpdatingButtonContent) v).setContainingButton(null) ;
		if ( v instanceof ViewGroup ) {
			ViewGroup vg = (ViewGroup)v ;
			for ( int i = 0; i < vg.getChildCount(); i++ )
				setContainingButtonOnChildren(vg.getChildAt(i), set) ;
		}
	}
	
	
	
	synchronized public void setContentState( ContentState contentState ) {
		boolean change = mContentState != contentState ;
		mContentState = contentState ;
		if ( change )
			postInvalidate() ;
	}
	
	synchronized public ContentState getContentState() {
		return mContentState ;
	}
	
	synchronized public int getBox() {
		switch( mStyle ) {
		case PLAIN:
			return BOX_STANDARD ;
		case BLOCK_UNDERLINE:
			return BOX_STANDARD ;
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT:
			// EXCLUSIVE gets the ALT box.
			return mContentState == ContentState.EXCLUSIVE ? BOX_ALT : BOX_STANDARD ;
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE:
			// Reverse of the standard 3-tier.
			return mContentState == ContentState.EXCLUSIVE ? BOX_STANDARD : BOX_ALT ;
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT:
			// STANDARD for optional, otherwise ALT.
			return mContentState == ContentState.OPTIONAL ? BOX_STANDARD : BOX_ALT ;
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
			// ALT for optional, otherwise STANDARD.
			return mContentState == ContentState.OPTIONAL ? BOX_ALT : BOX_STANDARD ;
		case BLOCK_TWO_TIER_ALWAYS_ENCLOSE_CONTENT:
			// STANDARD for optional and not touched, otherwise ALT.
			return mContentState == ContentState.OPTIONAL && !this.isPressed() ? BOX_STANDARD : BOX_ALT ;
		}
		
		return BOX_STANDARD ;
	}
	
	synchronized public boolean getGlow() {
		switch( mStyle ) {
		case PLAIN:
			return mContentState != ContentState.OPTIONAL ;
		case BLOCK_UNDERLINE:
			return mContentState != ContentState.OPTIONAL ;
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT:
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
			return mContentState == ContentState.EXCLUSIVE ;
		case BLOCK_TWO_TIER_ALWAYS_ENCLOSE_CONTENT:
			return mContentState == ContentState.EXCLUSIVE ;
		}
		
		return false ;
	}
	
	
	synchronized public void setDrawWrapperOnDraw(boolean d) {
		mDrawWrapperOnDraw = d ;
	}
	
	synchronized public void setDrawContentOnDraw(boolean d) {
		mDrawContentOnDraw = d ;
	}
	
	
	synchronized public void setFillDrawable( Drawable d, boolean showUnpressed ) {
		this.mFillDrawable = d ;
		this.mFillDrawableUnpressed = showUnpressed ;
	}
	
	@Override
	synchronized public void setBackgroundDrawable(Drawable d) {
		super.setBackgroundDrawable(d) ;
		mBackgroundDrawable = d ;
	}
	
	synchronized public void setBackground(Drawable d) {
		// Deliberately calling depreciated method; cannot
		// directly call setBackground() with min API 7.
		super.setBackgroundDrawable(d) ;
		mBackgroundDrawable = d ;
	}
	
	@Override
	synchronized public void setEnabled(boolean e) {
		postInvalidate() ;
		// We post invalidate BEFORE enabling the children or setting enabled.  If not,
		// the view will be drawn with a clipRectangle showing the children only,
		// then AGAIN with clip rectangle showing everything.  This causes a visually
		// unappealing "two-step update".
		super.setEnabled(e) ;
		super.setClickable(e) ;
		if ( mContent != null )
			enableChild(mContent, e) ;
	}
	
	synchronized private void enableChild( View child, boolean enabled ) {
		child.setEnabled(enabled) ;
		if ( mRecurStateChange && child instanceof ViewGroup ) {
			ViewGroup vg = (ViewGroup)child ;
			for ( int i = 0; i < vg.getChildCount(); i++ )
				enableChild( vg.getChildAt(i), enabled ) ;
		}
	}
	
	@Override
	synchronized public void setPressed(boolean p) {
		if ( p == isPressed() )
			return ;
		invalidate() ;
		((View)getParent()).invalidate() ;
		// We post invalidate BEFORE enabling the children or setting enabled.  If not,
		// the view will be drawn with a clipRectangle showing the children only,
		// then AGAIN with clip rectangle showing everything.  This causes a visually
		// unappealing "two-step update".
		super.setPressed(p) ;
		if ( mContent != null )
			pressChild( mContent, p ) ;
	}
	
	synchronized private void pressChild( View child, boolean pressed ) {
		child.setPressed(pressed) ;
		if ( mRecurStateChange && child instanceof ViewGroup ) {
			ViewGroup vg = (ViewGroup)child ;
			for ( int i = 0; i < vg.getChildCount(); i++ )
				pressChild( vg.getChildAt(i), pressed ) ;
		}
	}
	
	
	synchronized public void setStateChangeShouldRecur( boolean recur ) {
		this.mRecurStateChange = recur ;
		if ( recur && isPressed() )
			setPressed(true) ;
		if ( recur && isEnabled() )
			setEnabled(true) ;
	}
	
	synchronized public void setBorderWidth( int width, Rect border ) {
		mBorderWidth[BOX_STANDARD] = width ;
		mBorderWidthBySide[BOX_STANDARD] = new Rect(border) ;
		setToMaxInEachDimension( mBorderWidthBoundingBox, mBorderWidthBySide ) ;
		postInvalidate() ;
	}
	
	synchronized public void setBorderWidthAlt( int width, Rect border ) {
		mBorderWidth[BOX_ALT] = width ;
		mBorderWidthBySide[BOX_ALT] = new Rect( border ) ;
		setToMaxInEachDimension( mBorderWidthBoundingBox, mBorderWidthBySide ) ;
		postInvalidate() ;
	}
	
	synchronized public void setGradientHeight( int height ) {
		this.mGradientHeight = height ;
		postInvalidate() ;
	}
	
	
	synchronized public void setMinSize(int w, int h) {
		mMinWidth = w ;
		mMinHeight = h ;
		postInvalidate() ;
	}
	
	synchronized public void setMinWidth(int w) {
		mMinWidth = w ;
		postInvalidate() ;
	}
	
	synchronized public void setMinHeight(int h) {
		mMinHeight = h ;
		postInvalidate() ;
	}
	
	synchronized public void setMinTouchableSize(int w, int h) {
		mMinTouchableWidth = w ;
		mMinTouchableHeight = h ;
		postInvalidate() ;
	}
	
	synchronized public void setMinTouchableWidth(int w) {
		mMinTouchableWidth = w ;
		postInvalidate() ;
	}
	
	synchronized public void setMinTouchableHeight(int h) {
		mMinTouchableHeight = h ;
		postInvalidate() ;
	}
	
	
	synchronized public void setStyle( Style style ) {
		if ( style == mStyle )
			return ;
		
		Style prev = mStyle ;
		mStyle = style ;
		
		if ( ( prev == null || prev == Style.PLAIN ) && mStyle != Style.PLAIN ) {
			generateBlockGradients() ;
			setBlockDrawableBounds() ;
			setBlockGradientAlphas() ;
		}
		
		postInvalidate() ;
	}
	
	synchronized public Style getStyle() {
		return mStyle ;
	}
	
	
	synchronized public void setBlockColor( int borderColor, int fillColor ) {
		mBlockBorderColor = borderColor ;
		mBlockFillColor = fillColor ;
		
		generateBlockGradients() ;
		setBlockDrawableBounds() ;
		setBlockGradientAlphas() ;
		postInvalidate() ;
	}
	
	
	synchronized public void setBlockAlpha(
			float borderAlphaMult,
			float fillAlphaMult, float fillAlphaMultMax,
			float disabledAlphaMult ) {
		
		mBlockAlpha[INDEX_BORDER] = Math.round(255 * borderAlphaMult) ;
		mBlockAlpha[INDEX_FILL] = Math.round(255 * fillAlphaMult) ;
		mBlockAlpha[INDEX_FILL_MAX] = Math.round(255 * fillAlphaMultMax) ;
		mBlockAlpha[INDEX_DISABLED] = Math.round(255 * disabledAlphaMult) ;
		
		setBlockGradientAlphas() ;
		postInvalidate() ;
	}
	
	synchronized public void setBlockContentShadow( Drawable d ) {
		mBlockDrawableContentShadow = d ;
		postInvalidate() ;
	}
	
	
	/**
	 * Sets the default color for this QCWB.  The default color is displayed for
	 * any component in a state for which no more specific color is defined, and
	 * for gradients when in a state with no specific gradient defined.  Alpha
	 * multipliers are applied to the most specific color available.
	 * 
	 * @param color In Android format: 0xAARRGGBB.
	 */
	synchronized public void setDefaultColor( int color ) {
		mColorDefault = color ;
		generateAllPaints() ;
		// Tell the UI thread that we need to be redrawn.
		postInvalidate() ;
	}
	
	synchronized public void setColor( int color ) {
		setDefaultColor(color) ;
		setBlockColor( color, color ) ;
	}
	
	synchronized public void reset() {
		for ( int state = 0; state < NUM_STATES; state++ ) {
			for ( int section = 0; section < NUM_SECTIONS; section++ ) {
				resetColorForSectionAtState(section, state) ;
				if ( state == 0 )
					resetDefaultColorForSection(section) ;
			}
			resetGradientColors(state) ;
			resetTopGradientColors(state) ;
		}
		resetDefaultGradientColors() ;
		resetDefaultTopGradientColors() ;
		
		setColor(COLOR_DEFAULT) ;
	}
	
	/**
	 * Sets the default color for the specified section of this QCWB.
	 * The default color is displayed for any state for which no more specific
	 * color is defined.  Alpha multipliers are applied to the most specific color available.
	 * 
	 * @param color In Android format: 0xAARRGGBB.
	 * @param section One of SECTION_*
	 */
	synchronized public void setDefaultColorForSection( int color, int section ) {
		assert 0 <= section && section < NUM_SECTIONS : "Must specify a section number from 0 to " + (NUM_SECTIONS - 1) + ", inclusive" ;
		mColorDefaultSection[section] = color ;
		mSetDefaultSection[section] = SET_COLOR ;
		// Generate paint for this section only.
		generateSectionPaints(section) ;
		// Tell the UI thread that we need to be redrawn.
		postInvalidate() ;
	}
	
	/**
	 * Sets the default alpha multiplier for the specified section of this QCWB.
	 * This multiplier is applied to the default color when no more specific color
	 * is defined.  Alpha multipliers are applied to the most specific color available,
	 * but DO NOT apply recursively; they are applied to the most specific COLOR defined.
	 * 
	 * @param mult Any real, but probably in [0,1].
	 * @param section One of SECTION_*
	 */
	synchronized public void setDefaultAlphaMultForSection( float mult, int section ) {
		assert 0 <= section && section < NUM_SECTIONS : "Must specify a section number from 0 to " + (NUM_SECTIONS - 1) + ", inclusive" ;
		mAlphaMultDefaultSection[section] = mult ;
		mSetDefaultSection[section] = SET_ALPHA_MULT ;
		// Generate paint for this section only.
		generateSectionPaints(section) ;
		// Tell the UI thread that we need to be redrawn.
		postInvalidate() ;
	}
	
	/**
	 * Resets the default color for the specified section of this QCWB.
	 * Removes the default section color from consideration when coloring.
	 * 
	 * @param section One of SECTION_*
	 */
	synchronized public void resetDefaultColorForSection( int section ) {
		assert 0 <= section && section < NUM_SECTIONS : "Must specify a section number from 0 to " + (NUM_SECTIONS - 1) + ", inclusive" ;
		mSetDefaultSection[section] = SET_NONE ;
		// Generate paint for this section only.
		generateSectionPaints(section) ;
		// Tell the UI thread that we need to be redrawn.
		postInvalidate() ;
	}
	
	
	/**
	 * Sets the color for the specified section of this QCWB when in the 
	 * specified state.
	 * 
	 * @param color In Android format: 0xAARRGGBB.
	 * @param section One of SECTION_*
	 * @param state One of STATE_*
	 */
	synchronized public void setColorForSectionAtState( int color, int section, int state  ) {
		assert 0 <= section && section < NUM_SECTIONS : "Must specify a section number from 0 to " + (NUM_SECTIONS - 1) + ", inclusive" ;
		assert 0 <= state && state < NUM_STATES : "Must specify a state number from 0 to " + (NUM_STATES - 1) + ", inclusive" ;
		setPaintAndInvalidate( color, 0, section, state, SET_COLOR ) ;
	}
	
	
	/**
	 * Sets the alpha multiplier for the specified section when in the
	 * specified state.  Alpha multipliers are applied to the most specific
	 * color set - they are not applied recursively.
	 * @param alphaMult Any real, but probably in [0,1].
	 * @param section One of SECTION_*
	 * @param state One of STATE_*
	 */
	synchronized public void setAlphaMultForSectionAtState( float alphaMult, int section, int state  ) {
		assert 0 <= section && section < NUM_SECTIONS : "Must specify a section number from 0 to " + (NUM_SECTIONS - 1) + ", inclusive" ;
		assert 0 <= state && state < NUM_STATES : "Must specify a state number from 0 to " + (NUM_STATES - 1) + ", inclusive" ;
		setPaintAndInvalidate( 0, alphaMult, section, state, SET_ALPHA_MULT ) ;
	}
	
	/**
	 * Resets the color the specified section in the specified state.  From
	 * now on this section, in this state, will use the most specific default
	 * color available.
	 * 
	 * @param section One of SECTION_*
	 * @param state One of STATE_*
	 */
	synchronized public void resetColorForSectionAtState( int section, int state  ) {
		assert 0 <= section && section < NUM_SECTIONS : "Must specify a section number from 0 to " + (NUM_SECTIONS - 1) + ", inclusive" ;
		assert 0 <= state && state < NUM_STATES : "Must specify a state number from 0 to " + (NUM_STATES - 1) + ", inclusive" ;
		setPaintAndInvalidate( 0, 0, section, state, SET_NONE ) ;
	}
	
	synchronized protected void setPaintAndInvalidate( int color, float mult, int section, int state, int set ) {
		int e=0, f=0, p=0, g=0 ;		// enabled, focused, pressed
		switch( state ) {
		case STATE_DISABLED:
			// everything is as initialized
			break ;
		case STATE_ENABLED:
			e = 1 ;
			break ;
		case STATE_PRESSED:
			e = 1 ;
			p = 1 ;
			break ;
		case STATE_FOCUSED:
			e = 1 ;
			f = 1 ;
			break ;
		case STATE_DISABLED_FOCUSED:
			f = 1 ;
			break ;
		case STATE_GLOW:
			e = 1 ;
			g = 1 ;
			break ;
		case STATE_GLOW_FOCUSED:
			e = 1 ;
			f = 1 ;
			g = 1 ;
			break ;
		case STATE_GLOW_PRESSED:
			e = 1 ;
			p = 1 ;
			g = 1 ;
			break ;
		}
		mColorSection_enabledFocusedPressedGlow[section][e][f][p][g] = color ;
		mAlphaMultSection_enabledFocusedPressedGlow[section][e][f][p][g] = mult ;
		mSetSection_enabledFocusedPressedGlow[section][e][f][p][g] = set ;
		// Invalidate!
		if ( (e == 1) == isEnabled() || (f == 1) == isFocused() || (p == 1) == isPressed() || (g == 1) == getGlow() )
			postInvalidate() ;
	}
	
	
	synchronized public void setDefaultGradientColors( int [] color, float [] positions ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, color.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( color.length != positions.length ) 
			throw new RuntimeException("Gradient color and positions lengths must be the same") ;
		mColorDefaultGradient = new int[color.length] ;
		mPositionsDefaultGradient = new float[color.length] ;
		for ( int i = 0; i < color.length; i++ ) {
			mColorDefaultGradient[i] = color[i] ;
			mPositionsDefaultGradient[i] = positions[i] ;
		}
		mSetDefaultGradient = SET_COLOR ;
		// Generate gradient paints
		generateGradientPaints() ;
		// Tell the UI thread to redraw
		postInvalidate() ;
	}
	
	synchronized public void setDefaultTopGradientColors( int [] color, float [] positions ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, color.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( color.length != positions.length ) 
			throw new RuntimeException("Gradient color and positions lengths must be the same") ;
		mColorDefaultTopGradient = new int[color.length] ;
		mPositionsDefaultTopGradient = new float[color.length] ;
		for ( int i = 0; i < color.length; i++ ) {
			mColorDefaultTopGradient[i] = color[i] ;
			mPositionsDefaultTopGradient[i] = positions[i] ;
		}
		mSetDefaultTopGradient = SET_COLOR ;
		// Generate gradient paints
		generateGradientPaints() ;
		// Tell the UI thread to redraw
		postInvalidate() ;
	}
	
	
	
	synchronized public void setDefaultGradientAlphaMults( float [] mults, float [] positions ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, mults.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( mults.length != positions.length ) 
			throw new RuntimeException("Gradient alpha mults and positions lengths must be the same") ;
		mAlphaMultDefaultGradient = new float[mults.length] ;
		mPositionsDefaultGradient = new float[mults.length] ;
		for ( int i = 0; i < mults.length; i++ ) {
			mAlphaMultDefaultGradient[i] = mults[i] ;
			mPositionsDefaultGradient[i] = positions[i] ;
		}
		mSetDefaultGradient = SET_ALPHA_MULT ;
		// Generate gradient paints
		generateGradientPaints() ;
		// Tell the UI thread to redraw
		postInvalidate() ;
	}
	
	synchronized public void setDefaultTopGradientAlphaMults( float [] mults, float [] positions ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, mults.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( mults.length != positions.length ) 
			throw new RuntimeException("Gradient alpha mults and positions lengths must be the same") ;
		mAlphaMultDefaultTopGradient = new float[mults.length] ;
		mPositionsDefaultTopGradient = new float[mults.length] ;
		for ( int i = 0; i < mults.length; i++ ) {
			mAlphaMultDefaultTopGradient[i] = mults[i] ;
			mPositionsDefaultTopGradient[i] = positions[i] ;
		}
		mSetDefaultTopGradient = SET_ALPHA_MULT ;
		// Generate gradient paints
		generateGradientPaints() ;
		// Tell the UI thread to redraw
		postInvalidate() ;
	}
	
	
	
	synchronized public void resetDefaultGradientColors( ) {
		mSetDefaultGradient = SET_NONE ;
		generateGradientPaints() ;
		// Tell the UI thread to redraw
		postInvalidate() ;
	}
	
	synchronized public void resetDefaultTopGradientColors( ) {
		mSetDefaultTopGradient = SET_NONE ;
		generateGradientPaints() ;
		// Tell the UI thread to redraw
		postInvalidate() ;
	}
	
	
	
	synchronized public void setGradientColors( int [] color, float [] positions, int state ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, color.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( color.length != positions.length ) 
			throw new RuntimeException("Gradient color and positions lengths must be the same") ;
		int [] col = new int[color.length] ;
		float [] pos = new float[positions.length] ;
		for ( int i = 0; i < col.length; i++ ) {
			col[i] = color[i] ;
			pos[i] = positions[i] ;
		}
		setPaintAndInvalidate( col, null, positions, state, SET_COLOR ) ;
	}
	
	synchronized public void setTopGradientColors( int [] color, float [] positions, int state ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, color.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( color.length != positions.length ) 
			throw new RuntimeException("Gradient color and positions lengths must be the same") ;
		int [] col = new int[color.length] ;
		float [] pos = new float[positions.length] ;
		for ( int i = 0; i < col.length; i++ ) {
			col[i] = color[i] ;
			pos[i] = positions[i] ;
		}
		setTopPaintAndInvalidate( col, null, positions, state, SET_COLOR ) ;
	}
	
	
	
	
	synchronized public void setGradientAlphaMults( float [] mults, float [] positions, int state ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, mults.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( mults.length != positions.length ) 
			throw new RuntimeException("Gradient alpha mults and positions lengths must be the same") ;
		float [] mul = new float[mults.length] ;
		float [] pos = new float[positions.length] ;
		for ( int i = 0; i < mul.length; i++ ) {
			mul[i] = mults[i] ;
			pos[i] = positions[i] ;
		}
		setPaintAndInvalidate( null, mults, positions, state, SET_ALPHA_MULT ) ;
	}
	
	synchronized public void resetGradientColors( int state ) {
		setPaintAndInvalidate( null, null, null, state, SET_NONE ) ;
	}
	
	synchronized public void setTopGradientAlphaMults( float [] mults, float [] positions, int state ) {
		positions = replaceNullArrayWithEquidistantPositions( positions, mults.length ) ;
		if ( !gradientPositionsOK( positions ) )
			throw new RuntimeException("Gradient positions are not valid.") ;
		if ( mults.length != positions.length ) 
			throw new RuntimeException("Gradient alpha mults and positions lengths must be the same") ;
		float [] mul = new float[mults.length] ;
		float [] pos = new float[positions.length] ;
		for ( int i = 0; i < mul.length; i++ ) {
			mul[i] = mults[i] ;
			pos[i] = positions[i] ;
		}
		setTopPaintAndInvalidate( null, mults, positions, state, SET_ALPHA_MULT ) ;
	}
	
	synchronized public void resetTopGradientColors( int state ) {
		setTopPaintAndInvalidate( null, null, null, state, SET_NONE ) ;
	}
	
	synchronized protected void setPaintAndInvalidate( int [] colors, float [] alphaMults, float [] positions, int state, int set ) {
		int e=0, f=0, p=0, g=0 ;		// enabled, focused, pressed, glow
		switch( state ) {
		case STATE_DISABLED:
			// everything is as initialized
			break ;
		case STATE_ENABLED:
			e = 1 ;
			break ;
		case STATE_PRESSED:
			e = 1 ;
			p = 1 ;
			break ;
		case STATE_FOCUSED:
			e = 1 ;
			f = 1 ;
			break ;
		case STATE_DISABLED_FOCUSED:
			f = 1 ;
			break ;
		case STATE_GLOW:
			e = 1 ;
			g = 1 ;
			break ;
		case STATE_GLOW_FOCUSED:
			e = 1 ;
			f = 1 ;
			g = 1 ;
			break ;
		case STATE_GLOW_PRESSED:
			e = 1 ;
			p = 1 ;
			g = 1 ;
			break ;
		}
		
		mColorGradient_enabledFocusedPressedGlow[e][f][p][g] = colors ;
		mAlphaMultGradient_enabledFocusedPressedGlow[e][f][p][g] = alphaMults ;
		mPositionsGradient_enabledFocusedPressedGlow[e][f][p][g] = positions ;
		mSetGradient_enabledFocusedPressedGlow[e][f][p][g] = set ;
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ )
			mPaintGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g] = newGradientPaint( boxIndex, e, f, p, g ) ;
		// Tell the UI thread to redraw
		if ( (e == 1) == isEnabled() || (f == 1) == isFocused() || (p == 1) == isPressed() || (g == 1) == getGlow() )
			postInvalidate() ;
	}
	
	
	synchronized protected void setTopPaintAndInvalidate( int [] colors, float [] alphaMults, float [] positions, int state, int set ) {
		int e=0, f=0, p=0, g=0 ;		// enabled, focused, pressed, glow
		switch( state ) {
		case STATE_DISABLED:
			// everything is as initialized
			break ;
		case STATE_ENABLED:
			e = 1 ;
			break ;
		case STATE_PRESSED:
			e = 1 ;
			p = 1 ;
			break ;
		case STATE_FOCUSED:
			e = 1 ;
			f = 1 ;
			break ;
		case STATE_DISABLED_FOCUSED:
			f = 1 ;
			break ;
		case STATE_GLOW:
			e = 1 ;
			g = 1 ;
			break ;
		case STATE_GLOW_FOCUSED:
			e = 1 ;
			f = 1 ;
			g = 1 ;
			break ;
		case STATE_GLOW_PRESSED:
			e = 1 ;
			p = 1 ;
			g = 1 ;
			break ;
		}
		
		mColorTopGradient_enabledFocusedPressedGlow[e][f][p][g] = colors ;
		mAlphaMultTopGradient_enabledFocusedPressedGlow[e][f][p][g] = alphaMults ;
		mPositionsTopGradient_enabledFocusedPressedGlow[e][f][p][g] = positions ;
		mSetTopGradient_enabledFocusedPressedGlow[e][f][p][g] = set ;
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ )
			mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][e][f][p][g] = newGradientPaint( boxIndex, e, f, p, g ) ;
		// Tell the UI thread to redraw
		if ( (e == 1) == isEnabled() || (f == 1) == isFocused() || (p == 1) == isPressed() || (g == 1) == getGlow() )
			postInvalidate() ;
	}
	
	
	int [] color = new int[4] ;
	int [][] c = new int[11][2] ;
	synchronized protected void generateBlockGradients() {
		// We apply shines as needed.  For tinted blocks, we use a standard white shine.
		// For greyscale, we use a more subdued shine that moves the tone up or down,
		// but not necessarily towards white.
		int r = Color.red(mBlockBorderColor) ;
		int g = Color.green(mBlockBorderColor) ;
		int b = Color.blue(mBlockBorderColor) ;
		boolean grey = r == g && g == b ;
		
		if ( grey ) {
			if ( r <= 200 ) {
				int lumen = r + 55 ;
				color[CORNER_TOP_LEFT] = ColorOp.composeAOverB( Color.argb(128, lumen, lumen, lumen), mBlockBorderColor ) ;
				color[CORNER_TOP_RIGHT] = ColorOp.composeAOverB( Color.argb(85, lumen, lumen, lumen), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_LEFT] = ColorOp.composeAOverB( Color.argb(43, lumen, lumen, lumen), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_RIGHT] = mBlockBorderColor ;
			} else {
				color[CORNER_TOP_LEFT] = ColorOp.composeAOverB( Color.argb(0, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
				color[CORNER_TOP_RIGHT] = ColorOp.composeAOverB( Color.argb(43, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_LEFT] = ColorOp.composeAOverB( Color.argb(85, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_RIGHT] = ColorOp.composeAOverB( Color.argb(128, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
			}
		} else {
			if ( Color.red(mBlockBorderColor) < 250 || Color.green(mBlockBorderColor) < 250 || Color.blue(mBlockBorderColor) < 250 ) {
				color[CORNER_TOP_LEFT] = ColorOp.composeAOverB( Color.argb(128, 255, 255, 255), mBlockBorderColor ) ;
				color[CORNER_TOP_RIGHT] = ColorOp.composeAOverB( Color.argb(85, 255, 255, 255), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_LEFT] = ColorOp.composeAOverB( Color.argb(43, 255, 255, 255), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_RIGHT] = mBlockBorderColor ;
			} else {
				color[CORNER_TOP_LEFT] = ColorOp.composeAOverB( Color.argb(0, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
				color[CORNER_TOP_RIGHT] = ColorOp.composeAOverB( Color.argb(43, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_LEFT] = ColorOp.composeAOverB( Color.argb(85, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
				color[CORNER_BOTTOM_RIGHT] = ColorOp.composeAOverB( Color.argb(128, 0xa0, 0xa0, 0xa0), mBlockBorderColor ) ;
			}
		}
		// these are our "shined" border colors.
		
		// full!
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			int i = 0 ;
			c[i][0] = c[i][1] = mBlockFillColor ;
			mBlockDrawableFull[boxIndex] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
			
			// corners; solid colors
			i++ ;
			c[i][0] = c[i][1] = color[CORNER_TOP_LEFT] ;
			mBlockDrawableCorner[boxIndex][CORNER_TOP_LEFT] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
			i++ ;
			c[i][0] = c[i][1] = color[CORNER_TOP_RIGHT] ;
			mBlockDrawableCorner[boxIndex][CORNER_TOP_RIGHT] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
			i++ ;
			c[i][0] = c[i][1] = color[CORNER_BOTTOM_LEFT] ;
			mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_LEFT] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
			i++ ;
			c[i][0] = c[i][1] = color[CORNER_BOTTOM_RIGHT] ;
			mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_RIGHT] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
			
			// sides; gradients
			i++ ;
			c[i][0] = color[CORNER_TOP_LEFT] ;
			c[i][1] = color[CORNER_BOTTOM_LEFT] ;
			mBlockDrawableSide[boxIndex][SIDE_LEFT] = new GradientDrawable(
					GradientDrawable.Orientation.TOP_BOTTOM, c[i] ) ;
			i++ ;
			c[i][0] = color[CORNER_TOP_RIGHT] ;
			c[i][1] = color[CORNER_BOTTOM_RIGHT] ;
			mBlockDrawableSide[boxIndex][SIDE_RIGHT] = new GradientDrawable(
					GradientDrawable.Orientation.TOP_BOTTOM, c[i] ) ;
			i++ ;
			c[i][0] = color[CORNER_TOP_LEFT] ;
			c[i][1] = color[CORNER_TOP_RIGHT] ;
			mBlockDrawableSide[boxIndex][SIDE_TOP] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
			i++ ;
			c[i][0] = color[CORNER_BOTTOM_LEFT] ;
			c[i][1] = color[CORNER_BOTTOM_RIGHT] ;
			mBlockDrawableSide[boxIndex][SIDE_BOTTOM] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
		
			// disabled are solid colors.
			i++ ;
			c[i][0] = c[i][1] = mBlockFillColor ;
			mBlockDrawableDisabledFull[boxIndex] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
			i++ ;
			c[i][0] = c[i][1] = mBlockBorderColor ;
			mBlockDrawableDisabledBottom[boxIndex] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT, c[i] ) ;
		}
	}
	
	
	synchronized protected void setBlockGradientAlphas() {
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			// set alphas for things we don't set at-the-moment.
			// DrawableFull has an adjusting alpha, but the rest are fixed.
			for ( int i = 0; i < mBlockDrawableCorner.length; i++ )
				mBlockDrawableCorner[boxIndex][i].setAlpha( mBlockAlpha[INDEX_BORDER] ) ;
			for ( int i = 0; i < mBlockDrawableSide.length; i++ )
				mBlockDrawableSide[boxIndex][i].setAlpha( mBlockAlpha[INDEX_BORDER] ) ;
			
			mBlockDrawableDisabledFull[boxIndex].setAlpha( mBlockAlpha[INDEX_DISABLED] ) ;
			mBlockDrawableDisabledBottom[boxIndex].setAlpha( mBlockAlpha[INDEX_DISABLED] ) ;
		}
	}
	
	
	synchronized protected void generateAllPaints() {
		for ( int i = 0; i < 2; i++ ) {
			for ( int j = 0; j < 2; j++ ) {
				for ( int k = 0; k < 2; k++ ) {
					for ( int m = 0; m < 2; m++ ) {
						for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
							for ( int section = 0; section < NUM_SECTIONS; section++ ) {
								mPaintSection_enabledFocusedPressedGlow[boxIndex][section][i][j][k][m] = newSectionPaint( section, i, j, k, m ) ;
							}
							mPaintGradient_enabledFocusedPressedGlow[boxIndex][i][j][k][m] = newGradientPaint( boxIndex, i, j, k, m ) ;
							mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][i][j][k][m] = newTopGradientPaint( boxIndex, i, j, k, m ) ;
						}
					}
				}
			}
		}
	}
	
	synchronized protected void generateSectionPaints( int section ) {
		for ( int i = 0; i < 2; i++ ) 
			for ( int j = 0; j < 2; j++ ) 
				for ( int k = 0; k < 2; k++ ) 
					for ( int m = 0; m < 2; m++ )
						for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ )
							mPaintSection_enabledFocusedPressedGlow[boxIndex][section][i][j][k][m] = newSectionPaint( section, i, j, k, m ) ;
	}
	
	synchronized protected void generateGradientPaints() {
		for ( int i = 0; i < 2; i++ ) {
			for ( int j = 0; j < 2; j++ ) {
				for ( int k = 0; k < 2; k++ ) {
					for ( int m = 0; m < 2; m++ ) {
						for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
							mPaintGradient_enabledFocusedPressedGlow[boxIndex][i][j][k][m] = newGradientPaint( boxIndex, i, j, k, m ) ;
							mPaintTopGradient_enabledFocusedPressedGlow[boxIndex][i][j][k][m] = newTopGradientPaint( boxIndex, i, j, k, m ) ;
						}
					}
				}
			}
		}
	}
	
	
	synchronized protected void setBlockDrawableBounds() {
		
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			// this is the inner content, not including borders
			mBlockDrawableFull[boxIndex].setBounds(mRect[boxIndex][RECT_CONTENT]) ;
			
			// corners
			mBlockDrawableCorner[boxIndex][CORNER_TOP_LEFT].setBounds(
					mRect[boxIndex][RECT_CONTENT].left - mBorderWidthBySide[boxIndex].left,
					mRect[boxIndex][RECT_CONTENT].top - mBorderWidthBySide[boxIndex].top,
					mRect[boxIndex][RECT_CONTENT].left,
					mRect[boxIndex][RECT_CONTENT].top) ;
			mBlockDrawableCorner[boxIndex][CORNER_TOP_RIGHT].setBounds(
					mRect[boxIndex][RECT_CONTENT].right,
					mRect[boxIndex][RECT_CONTENT].top - mBorderWidthBySide[boxIndex].left,
					mRect[boxIndex][RECT_CONTENT].right + mBorderWidthBySide[boxIndex].right,
					mRect[boxIndex][RECT_CONTENT].top) ;
			mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_LEFT].setBounds(
					mRect[boxIndex][RECT_CONTENT].left - mBorderWidthBySide[boxIndex].left,
					mRect[boxIndex][RECT_CONTENT].bottom,
					mRect[boxIndex][RECT_CONTENT].left,
					mRect[boxIndex][RECT_CONTENT].bottom + mBorderWidthBySide[boxIndex].bottom) ;
			mBlockDrawableCorner[boxIndex][CORNER_BOTTOM_RIGHT].setBounds(
					mRect[boxIndex][RECT_CONTENT].right,
					mRect[boxIndex][RECT_CONTENT].bottom,
					mRect[boxIndex][RECT_CONTENT].right + mBorderWidthBySide[boxIndex].right,
					mRect[boxIndex][RECT_CONTENT].bottom + mBorderWidthBySide[boxIndex].bottom) ;
			
			// sides
			mBlockDrawableSide[boxIndex][SIDE_LEFT].setBounds(
					mRect[boxIndex][RECT_CONTENT].left - mBorderWidthBySide[boxIndex].left,
					mRect[boxIndex][RECT_CONTENT].top,
					mRect[boxIndex][RECT_CONTENT].left,
					mRect[boxIndex][RECT_CONTENT].bottom) ;
			mBlockDrawableSide[boxIndex][SIDE_RIGHT].setBounds(
					mRect[boxIndex][RECT_CONTENT].right,
					mRect[boxIndex][RECT_CONTENT].top,
					mRect[boxIndex][RECT_CONTENT].right + mBorderWidthBySide[boxIndex].right,
					mRect[boxIndex][RECT_CONTENT].bottom) ;
			mBlockDrawableSide[boxIndex][SIDE_TOP].setBounds(
					mRect[boxIndex][RECT_CONTENT].left,
					mRect[boxIndex][RECT_CONTENT].top - mBorderWidthBySide[boxIndex].top,
					mRect[boxIndex][RECT_CONTENT].right,
					mRect[boxIndex][RECT_CONTENT].top ) ;
			mBlockDrawableSide[boxIndex][SIDE_BOTTOM].setBounds(
					mRect[boxIndex][RECT_CONTENT].left,
					mRect[boxIndex][RECT_CONTENT].bottom,
					mRect[boxIndex][RECT_CONTENT].right,
					mRect[boxIndex][RECT_CONTENT].bottom + mBorderWidthBySide[boxIndex].bottom ) ;
			
			// content shadow is re-set when the button is drawn
			
			// disabled full covers everything but the bottom.
			mBlockDrawableDisabledFull[boxIndex].setBounds(
					mRect[boxIndex][RECT_CONTENT].left - mBorderWidthBySide[boxIndex].left,
					mRect[boxIndex][RECT_CONTENT].top - mBorderWidthBySide[boxIndex].top,
					mRect[boxIndex][RECT_CONTENT].right + mBorderWidthBySide[boxIndex].right,
					mRect[boxIndex][RECT_CONTENT].bottom  ) ;
			
			// disabled bottom is the full-width bottom bar.  It doesn't get a shine.
			mBlockDrawableDisabledBottom[boxIndex].setBounds(
					mRect[boxIndex][RECT_CONTENT].left - mBorderWidthBySide[boxIndex].left,
					mRect[boxIndex][RECT_CONTENT].bottom,
					mRect[boxIndex][RECT_CONTENT].right + mBorderWidthBySide[boxIndex].right,
					mRect[boxIndex][RECT_CONTENT].bottom + mBorderWidthBySide[boxIndex].bottom ) ;
			
			int innerBottom = Math.min(
					mRect[boxIndex][RECT_CONTENT].bottom
							+ mBorderWidthBySide[boxIndex].bottom
							+ (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
										(float)1, getResources().getDisplayMetrics()),
					mRect[boxIndex][RECT_BORDERED_CONTENT].bottom ) ;
			
			mBlockRectInnerEdge = new Rect(
					mRect[boxIndex][RECT_CONTENT].left - mBorderWidthBySide[boxIndex].left,
					mRect[boxIndex][RECT_CONTENT].bottom + mBorderWidthBySide[boxIndex].bottom,
					mRect[boxIndex][RECT_CONTENT].right + mBorderWidthBySide[boxIndex].right,
					innerBottom ) ;
			
			mBlockRectOuterEdge = new Rect(
					mRect[boxIndex][RECT_CONTENT].left - mBorderWidthBySide[boxIndex].left,
					innerBottom,
					mRect[boxIndex][RECT_CONTENT].right + mBorderWidthBySide[boxIndex].right,
					mRect[boxIndex][RECT_BORDERED_CONTENT].bottom ) ;
			
		}
	}
	

	synchronized public void setDropShadowDrawables( boolean force, int xOffset, int yOffset ) {
		for ( int boxIndex = 0; boxIndex < NUM_BOXES; boxIndex++ ) {
			if ( (mDropShadow || force) &&
					( mRect[boxIndex][RECT_BORDERED_CONTENT].width() >= DropShadowDrawableFactory.RESIZEABLE_WIDTH
							|| mRect[boxIndex][RECT_BORDERED_CONTENT].height() >= DropShadowDrawableFactory.RESIZEABLE_HEIGHT ) ) {
				Resources res = getContext().getResources() ;
				
				mTempRect.set( mRect[boxIndex][RECT_BORDERED_CONTENT] ) ;
				mTempRect.offset(xOffset, yOffset) ;
				mDropShadowDrawable[boxIndex] = DropShadowDrawableFactory.getDrawable(
						mDropShadowDrawable[boxIndex], res, mTempRect) ;
				
				mTempRect.set( mRect[boxIndex][RECT_BORDER_BOTTOM] ) ;
				mTempRect.offset(xOffset, yOffset) ;
				mDropShadowBottomDrawable[boxIndex] = DropShadowDrawableFactory.getDrawable(
						mDropShadowBottomDrawable[boxIndex], res, mTempRect) ;
			} else {
				mDropShadowDrawable[boxIndex] = null ;
				mDropShadowBottomDrawable[boxIndex] = null ;
			}
		}
	}
	
	
	/**
	 * Constructs and returns the appropriate paint to use when coloring the specified section
	 * in the specified state.
	 * 
	 * @param section
	 * @param enabled
	 * @param focused
	 * @param pressed
	 * @return
	 */
	synchronized protected Paint newSectionPaint( int section, int enabled, int focused, int pressed, int glow ) {
		Paint p ;
		int set = mSetSection_enabledFocusedPressedGlow[section][enabled][focused][pressed][glow] ;
		if ( set == SET_COLOR ) {
			p = new Paint() ;
			p.setStrokeWidth(0) ;
			p.setColor(mColorSection_enabledFocusedPressedGlow[section][enabled][focused][pressed][glow]) ;
		}
		else if ( set == SET_ALPHA_MULT ) {
			// Generate the next-most-specific paint, and then change its alpha appropriately.
			p = newDefaultSectionPaint( section ) ;
			applyAlphaMult( p, mAlphaMultSection_enabledFocusedPressedGlow[section][enabled][focused][pressed][glow] ) ;
		}
		else {
			// Generate the next-most-specific paint.
			p = newDefaultSectionPaint( section ) ;
		}
		return p ;
	}
	
	/**
	 * Constructs and returns the appropriate paint to use for the generic state on 
	 * the specified section.
	 * @return
	 */
	synchronized protected Paint newDefaultSectionPaint( int section ) {
		Paint p ;
		int set = mSetDefaultSection[section] ;
		if ( set == SET_COLOR ) {
			p = new Paint() ;
			p.setStrokeWidth(0) ;
			p.setColor(mColorDefaultSection[section]) ;
		}
		else if ( set == SET_ALPHA_MULT ) {
			// Generate the next-most-specific paint, and then change its alpha appropriately.
			p = newDefaultPaint() ;
			applyAlphaMult( p, mAlphaMultDefaultSection[section] ) ;
		}
		else {
			// Generate the generic paint
			p = newDefaultPaint( ) ;
		}
		return p ;
	}
	
	/**
	 * Constructs and returns a new Paint instance using the default color.
	 * @return
	 */
	synchronized protected Paint newDefaultPaint() {
		Paint p = new Paint() ;
		p.setStrokeWidth(0) ;
		p.setColor(mColorDefault) ;
		return p ;
	}
	
	
	synchronized protected Paint newGradientPaint( int boxIndex, int enabled, int focused, int pressed, int glow ) {
		int [] colors = getGradientColors( enabled, focused, pressed, glow ) ;
		float [] positions = getGradientPositions( enabled, focused, pressed, glow ) ;
		// Our gradient covers the gradient rectangle.
		// TODO: If using more than one gradient, make a new paint for each!
		LinearGradient grad = new LinearGradient(0, mRect[boxIndex][RECT_GRADIENT].top, 0, mRect[boxIndex][RECT_GRADIENT].bottom, colors, positions, Shader.TileMode.CLAMP) ;
		Paint p = new Paint();
		p.setStrokeWidth(0) ;
	    p.setDither(true);
	    p.setShader(grad);
	    
	    return p ;
	}
	
	synchronized protected Paint newTopGradientPaint( int boxIndex, int enabled, int focused, int pressed, int glow ) {
		int [] colors = getTopGradientColors( enabled, focused, pressed, glow ) ;
		float [] positions = getTopGradientPositions( enabled, focused, pressed, glow ) ;
		// Our gradient covers the gradient rectangle.
		// TODO: If using more than one gradient, make a new paint for each!
		LinearGradient grad = new LinearGradient(0, mRect[boxIndex][RECT_TOP_GRADIENT].bottom, 0, mRect[boxIndex][RECT_TOP_GRADIENT].top, colors, positions, Shader.TileMode.CLAMP) ;
		Paint p = new Paint();
		p.setStrokeWidth(0) ;
	    p.setDither(true);
	    p.setShader(grad);
	    
	    return p ;
	}
	
	
	
	
	synchronized protected int [] getGradientColors( int enabled, int focused, int pressed, int glow ) {
		int set = mSetGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		if ( set == SET_COLOR )
			return mColorGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		else if ( set == SET_ALPHA_MULT ) {
			// get the default, and apply the multiplier.
			int [] defaultColors = getGradientColors() ;
			float [] defaultPositions = getGradientPositions() ;
			int [] resultColors = applyGradientAlphaMultToGradient(
					mAlphaMultGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow],
					mPositionsGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow],
					defaultColors, defaultPositions) ;
			return resultColors ;
		}
		else {
			return getGradientColors() ;
		}
	}
	
	synchronized protected int [] getGradientColors() {
		int [] colors ;
		int set = mSetDefaultGradient ;
		if ( set == SET_COLOR ) {
			colors = mColorDefaultGradient ;
		}
		else if ( set == SET_ALPHA_MULT ) {
			// Apply the alpha multiplier, step-by-step, to the default color.
			colors = new int[mAlphaMultDefaultGradient.length] ;
			for ( int i = 0; i < mAlphaMultDefaultGradient.length; i++ ) {
				colors[i] = applyAlphaMult( mColorDefault, mAlphaMultDefaultGradient[i] ) ;
			}
		}
		else {
			// Use the base color, with 0 alpha mult at the bottom,
			// 1 alpha mult at the top.
			colors = new int[2] ;
			colors[0] = applyAlphaMult( mColorDefault, 1.0f ) ;
			colors[1] = applyAlphaMult( mColorDefault, 0.0f ) ;
		}
		return colors ;
	}
	
	
	synchronized protected float [] getGradientPositions( int enabled, int focused, int pressed, int glow ) {
		int set = mSetGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		if ( set == SET_COLOR || set == SET_ALPHA_MULT )
			return mPositionsGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		else
			return getGradientPositions() ;
	}
	
	synchronized protected float [] getGradientPositions() {
		int set = mSetDefaultGradient ;
		if ( set == SET_COLOR || set == SET_ALPHA_MULT )
			return mPositionsDefaultGradient ;
		else
			return new float[] {0.0f, 1.0f} ;
	}
	
	
	synchronized protected int [] getTopGradientColors( int enabled, int focused, int pressed, int glow ) {
		int set = mSetTopGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		if ( set == SET_COLOR )
			return mColorTopGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		else if ( set == SET_ALPHA_MULT ) {
			// get the default, and apply the multiplier.
			int [] defaultColors = getGradientColors() ;
			float [] defaultPositions = getGradientPositions() ;
			int [] resultColors = applyGradientAlphaMultToGradient(
					mAlphaMultTopGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow],
					mPositionsTopGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow],
					defaultColors, defaultPositions) ;
			return resultColors ;
		}
		else {
			return getTopGradientColors() ;
		}
	}
	
	synchronized protected int [] getTopGradientColors() {
		int [] colors ;
		int set = mSetDefaultTopGradient ;
		if ( set == SET_COLOR ) {
			colors = mColorDefaultTopGradient ;
		}
		else if ( set == SET_ALPHA_MULT ) {
			// Apply the alpha multiplier, step-by-step, to the default color.
			colors = new int[mAlphaMultDefaultTopGradient.length] ;
			for ( int i = 0; i < mAlphaMultDefaultTopGradient.length; i++ ) {
				colors[i] = applyAlphaMult( mColorDefault, mAlphaMultDefaultTopGradient[i] ) ;
			}
		}
		else {
			// By default, we have NO top gradient.
			colors = new int[2] ;
			colors[0] = applyAlphaMult( mColorDefault, 0.0f ) ;
			colors[1] = applyAlphaMult( mColorDefault, 0.0f ) ;
		}
		return colors ;
	}
	
	
	synchronized protected float [] getTopGradientPositions( int enabled, int focused, int pressed, int glow ) {
		int set = mSetTopGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		if ( set == SET_COLOR || set == SET_ALPHA_MULT )
			return mPositionsTopGradient_enabledFocusedPressedGlow[enabled][focused][pressed][glow] ;
		else
			return getTopGradientPositions() ;
	}
	
	synchronized protected float [] getTopGradientPositions() {
		int set = mSetDefaultTopGradient ;
		if ( set == SET_COLOR || set == SET_ALPHA_MULT )
			return mPositionsDefaultTopGradient ;
		else
			return new float[] {0.0f, 1.0f} ;
	}
	
	
	synchronized protected int [] applyGradientAlphaMultToGradient(
			float [] mult, float [] multPos, int [] grad, float [] gradPos ) {
		int [] resColors = new int[mult.length] ; 
		
		// For each multiplier, find the color of the provided gradient at that
		// position, then apply the multiplier.
		for ( int i = 0; i < resColors.length; i++ ) {
			int color = gradientColorAtPosition( multPos[i], grad, gradPos ) ;
			resColors[i] = applyAlphaMult( color, mult[i] ) ;
		}
		return resColors ;
	}
	
	/**
	 * Returns as a color integer an interpolated color at position 'pos' based
	 * on the linear gradient defined by the provided grad and gradPos arrays.
	 * 
	 * Assumptions: the gradient is clamped at its edges, the gradPos values are
	 * in ascending order, and are all in [0,1].  'pos' is also in [0,1].
	 * @param pos
	 * @param grad
	 * @param gradPos
	 * @return
	 */
	synchronized protected int gradientColorAtPosition( float pos, int [] grad, float [] gradPos ) {
		
		int resColor ;
		// Look through gradPos for the first position >= pos, then interpolate.
		
		int firstGreaterPos = -1 ;
		for ( int i = 0; i < gradPos.length ; i++ ) {
			if ( gradPos[i] >= pos ) {
				firstGreaterPos = i ;
				break ;
			}
		}
		
		if ( firstGreaterPos == -1 )
			// nothing greater; clamp.
			resColor = grad[grad.length-1] ;
		else if ( firstGreaterPos == 0 )
			// Before the first; clamp.
			resColor = grad[0] ;
		else {
			float posA = gradPos[firstGreaterPos-1] ;
			float posB = gradPos[firstGreaterPos] ;
			// Region is from posA to posB.
			// If there is no space, use the color at posA
			if ( posA >= posB )
				resColor = grad[firstGreaterPos-1] ;
			else {
				// Interpolate!
				float interpPos = ( pos - posA ) / ( posB - posA ) ;
				// from 0 to 1 along [posA, posB].
				resColor = interpolate( grad[firstGreaterPos-1], grad[firstGreaterPos], interpPos ) ;
			}
		}
		
		return resColor ;
	}
	
	/**
	 * Using the provided paint, changes its alpha value to be the result
	 * of p.getAlpha * alphaMult - truncated to within [0,255]
	 * @param p
	 * @param alphaMult
	 */
	synchronized protected void applyAlphaMult( Paint p, float alphaMult ) {
		int alpha = p.getAlpha() ;
		p.setAlpha( Math.min(255, Math.max(0, (int)(alpha * alphaMult))) ) ;
	}
	
	synchronized protected int applyAlphaMult( int color, float alphaMult ) {
		int alpha = Color.alpha(color) ;
		return Color.argb(
				Math.min(255, Math.max(0, (int)(alpha * alphaMult))),
				Color.red(color),
				Color.green(color),
				Color.blue(color) );
	}

	synchronized protected int interpolate( int colorA, int colorB, float amountB ) {
		float amountA = 1 - amountB ;
		return Color.argb(
				Math.round( amountA * Color.alpha(colorA) + amountB * Color.alpha(colorB) ),
				Math.round( amountA * Color.red(colorA) + amountB * Color.red(colorB) ),
				Math.round( amountA * Color.green(colorA) + amountB * Color.green(colorB) ),
				Math.round( amountA * Color.blue(colorA) + amountB * Color.blue(colorB) ) ) ;
	}
	
	/**
	 * If the provided array 'pos' is null, returns an array of 'num' equidistant values
	 * from 0 to 1 inclusive; 'num' must be at least 2, or an exception will be thrown.
	 * 
	 * If 'pos' is not null, returns pos unchanged.
	 * 
	 * @param pos
	 * @param num
	 * @return
	 */
	synchronized protected float [] replaceNullArrayWithEquidistantPositions( float [] pos, int num ) {
		if ( pos != null )
			return pos ;
		if ( num < 2 )
			throw new IllegalArgumentException("Must provide a value array of at least length 2") ;
		pos = new float[num] ;
		for ( int i = 0; i < num; i++ )
			pos[i] = ((float)i)/(num-1) ;
		return pos ;
	}
	
	/**
	 * Returns whether the provided array is an OK list of gradient positions.  Such
	 * an array is OK if:
	 * 
	 * it is non-null,
	 * it is length at least 2,
	 * all values are in [0,1],
	 * values do not decrease.
	 * 
	 * @param pos
	 * @return
	 */
	synchronized protected boolean gradientPositionsOK( float [] pos ) {
		if ( pos == null )
			return false ;
		if ( pos.length < 2 )
			return false ;
		float prev = 0 ;
		for ( int i = 0; i < pos.length; i++ ) {
			if ( pos[i] < 0 || pos[i] > 1 )
				return false ;
			if ( pos[i] < prev )
				return false ;
			prev = pos[i] ;
		}
		return true ;
	}
}
