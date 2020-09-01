package com.peaceray.quantro.view.button;

import com.peaceray.quantro.R;
import com.peaceray.quantro.view.colors.ColorOp;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;

/**
 * A simple subclass of QuantroContentWrappingView.  This class defines
 * a simple Quantro button using QuantroContentWrappingView around a text
 * view with plain white (or black) text.  By default, uses centered and
 * bottom text.  You can specify alternate text resource XMLs when loading
 * from XML.
 * 
 * COLORS: A QuantroTextButton simplifies the 
 * @author Jake
 *
 */
public class QuantroButton extends QuantroContentWrappingButton {
	
	private static final String TAG = "QuantroButton" ;
	
	private int mContentViewResource = 0 ;
	private static final int TEXT_VIEW_RESOURCE = R.layout.quantro_content_wrapping_view_text_content ;
	
	private float DEFAULT_ENABLED_ALPHA = 1.0f ;
	private float DEFAULT_DISABLED_ALPHA = 0.5f ;
	private float DEFAULT_GRADIENT_ALPHA = 0.3f ;
	private float [] DEFAULT_TOP_GRADIENT_ALPHA = new float[]{0.7f, 0.55f, 0.2f, 0.0f} ;
	
	
	private float DEFAULT_BLOCK_BORDER_ALPHA = 1.0f ;
	private float DEFAULT_BLOCK_FILL_ALPHA = 0.4f ;
	private float DEFAULT_BLOCK_FILL_ALPHA_MAX = 0.8f ;
	private float DEFAULT_BLOCK_DISABLED_ALPHA = 0.4f ;
	
	private boolean mHasLoadedColor = false ;
	private int mLoadedColor = COLOR_DEFAULT ;
	private Style mLoadedStyle = Style.BLOCK_UNDERLINE ;
	
	public QuantroButton(Context context) {
		super(context);
		
		// Set basic defaults
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public QuantroButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public QuantroButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues(Context context) {
		// Set the mininum "touchable" area as 50 dip.
		int minDim = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)50, getResources().getDisplayMetrics()) ;
		setMinSize(minDim, minDim) ;
		int minTouchableDim = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				(float)44.1, getResources().getDisplayMetrics()) ;
		setMinTouchableSize(minTouchableDim, minTouchableDim) ;
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		// Read the special QuantroTextButton attributes.
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.QuantroButton);
		
		int size ;
		int ref ;
		
		final int N = a.getIndexCount() ;
		
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
		    switch (attr) {
		    case R.styleable.QuantroButton_content_layout:
		    	mContentViewResource = a.getResourceId(attr, TEXT_VIEW_RESOURCE) ;
		    	break ;
		    case R.styleable.QuantroButton_button_color:
		    	mLoadedColor = a.getColor(attr, mLoadedColor) ;
		    	mHasLoadedColor = true ;
		    	Log.d(TAG, "button_color: setting default to " + ColorOp.toString( mLoadedColor ) ) ;
		    	break ;
		    case R.styleable.QuantroButton_button_min_width:
		    	mMinWidth = a.getDimensionPixelSize(attr, -1) ;
		    	setMinSize( mMinWidth, mMinHeight ) ;
		    	break ;
		    case R.styleable.QuantroButton_button_min_height:
		    	mMinHeight = a.getDimensionPixelSize(attr, -1) ;
		    	setMinSize( mMinWidth, mMinHeight ) ;
		    	break ;
		    case R.styleable.QuantroButton_button_min_touchable_width:
		    	mMinTouchableWidth = a.getDimensionPixelSize(attr, -1) ;
		    	setMinTouchableSize( mMinTouchableWidth, mMinTouchableHeight ) ;
		    	break ;
		    case R.styleable.QuantroButton_button_min_touchable_height:
		    	mMinTouchableHeight = a.getDimensionPixelSize(attr, -1) ;
		    	setMinTouchableSize( mMinTouchableWidth, mMinTouchableHeight ) ;
		    	break ;
		    case R.styleable.QuantroButton_qb_style:
		    	ref = a.getInt(attr, 1) ;
		    	switch( ref ) {
		    	case 0:
		    		mLoadedStyle = Style.PLAIN ;
		    		break ;
		    	case 1:
		    		mLoadedStyle = Style.BLOCK_UNDERLINE ;
		    		break ;
		    	case 2:
		    		mLoadedStyle = Style.BLOCK_THREE_TIER_ENCLOSE_CONTENT ;
		    		break ;
		    	case 3:
		    		mLoadedStyle = Style.BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE ;
		    		break ;
		    	case 4:
		    		mLoadedStyle = Style.BLOCK_TWO_TIER_ENCLOSE_CONTENT ;
		    		break ;
		    	case 5:
		    		mLoadedStyle = Style.BLOCK_TWO_TIER_ENCLOSE_CONTENT_REVERSE ;
		    		break ;
		    	case 6:
		    		mLoadedStyle = Style.BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE ;
		    		break ;
		    	case 7:
		    		mLoadedStyle = Style.BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE ;
		    		break ;
		    	case 8:
		    		mLoadedStyle = Style.BLOCK_TWO_TIER_ALWAYS_ENCLOSE_CONTENT ;
		    		break ;
		    	}
		    }
		}
	}
	
	
	private void constructor_allocateAndInitialize(Context context) {
		// Set appearance
		setStyleToDefault( context, mLoadedStyle ) ;
		if ( mHasLoadedColor )
			setColor( mLoadedColor ) ;
		
		// load the text view from the provided resource.
		if ( mContentViewResource != 0 ) {
			View v = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(mContentViewResource,this,false);
			v.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT)) ;
			v.setClickable(false) ;
			setContentView(v) ;
		}

		generateAllPaints() ;
		
		// We are enabled and selectable by default.
		setEnabled(true) ;
		setFocusable(true) ;
		
		postInvalidate() ;
		
		setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.d("QuantroButton", "onClick") ;
			}
			
		}) ;
		
		setOnLongClickListener( new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				Log.d("QuantroButton", "onLongClick") ;
				return true ;
			}
			
		}) ;
	}
	
	
	private void setStyleToDefault( Context context, Style style ) {
		switch( mLoadedStyle ) {
		case PLAIN:
			throw new IllegalStateException("Unfortunately, QuantroButton doesn't support Style.PLAIN right now.") ;
		case BLOCK_UNDERLINE:
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT:
		case BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT:
		case BLOCK_UNDERLINE_TWO_TIER_ENCLOSE_CONTENT_REVERSE:
		case BLOCK_TWO_TIER_ALWAYS_ENCLOSE_CONTENT:
			resetToDefaultAppearance( ) ;
			resetToDefaultBlockAppearance( context, mLoadedStyle ) ;
			break ;
		}
	}
	
	
	protected void resetToDefaultBlockAppearance( Context context, Style style ) {
		setStyle( style ) ;
		setBlockColor( COLOR_DEFAULT, COLOR_DEFAULT ) ;
		setBlockAlpha(
				DEFAULT_BLOCK_BORDER_ALPHA, DEFAULT_BLOCK_FILL_ALPHA,
				DEFAULT_BLOCK_FILL_ALPHA_MAX, DEFAULT_BLOCK_DISABLED_ALPHA ) ;
		setBlockContentShadow( context.getResources().getDrawable(R.drawable.quantro_button_inner_shadow) ) ;
	}
	
	
	protected void resetToDefaultAppearance() {
		resetToDefaultAppearance( DEFAULT_ENABLED_ALPHA, DEFAULT_DISABLED_ALPHA, DEFAULT_GRADIENT_ALPHA, DEFAULT_TOP_GRADIENT_ALPHA ) ;
	}
	
	
	protected void resetToDefaultAppearance( int color ) {
		mColorDefault = color ;
		resetToDefaultAppearance() ;
	}
	
	
	/**
	 * Sets this button to its default appearance, using its current color.  By default:
	 * 
	 * For enabled buttons, the bottom border is always "enabledAlpha."  Selected content
	 * 		is disabledAlpha, touched is enabledAlpha.  The glow gradient is always applied,
	 * 		and goes through the values set from bottom to top.  The bottom gradient is
	 * 		applied for enabled buttons and goes from the specified gradientAlpha to 0.
	 * 
	 * @param enabledAlpha
	 * @param disabledAlpha
	 * @param gradientAlpha
	 * @param glowAlpha
	 */
	protected void resetToDefaultAppearance( float enabledAlpha, float disabledAlpha, float gradientAlpha, float [] glowAlpha ) {
		
		int color = mColorDefault ;
		reset() ;
		mColorDefault = color ;
		
		// Default text view resource
		// mTextViewResource = TEXT_VIEW_RESOURCE ;
		// We set default alphas, so the declaration can use setColor.  Use empty
		// gradient for non-enabled, default gradient for enabled.
		float [] gradDisabled = new float[]{0.0f, 0.0f} ;
		float [] gradEnabled = new float[]{gradientAlpha, 0.0f} ;
		float [] topGradGlow = glowAlpha ;
		float [] gradPos = replaceNullArrayWithEquidistantPositions(null, 2) ;
		float [] gradPos3 = replaceNullArrayWithEquidistantPositions(null, topGradGlow.length) ; 
		for ( int f = 0; f < 2; f++ ) {
			for ( int p = 0; p < 2; p++ ) {
				setIfNotSet_AlphaMultGradient_enabledFocusedPressedGlow( 0, f, p, 0, gradDisabled, gradPos ) ;
				setIfNotSet_AlphaMultGradient_enabledFocusedPressedGlow( 1, f, p, 0, gradEnabled, gradPos ) ;
				setIfNotSet_AlphaMultTopGradient_enabledFocusedPressedGlow( 1, f, p, 1, topGradGlow, gradPos3 ) ;
				
			}
		}
		
		// Set enabled border-bottom to full alpha, disabled to default.
		// Set background and border to same color always: 0.0f for all disabled,
		// default for selected, full for pressed.
		
		for ( int g = 0; g < 2; g++ ) {
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BACKGROUND, 0, 0, 0, g, 0.0f) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BACKGROUND, 1, 0, 0, g, 0.0f) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BACKGROUND, 1, 0, 1, g, enabledAlpha) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BACKGROUND, 1, 1, 0, g, disabledAlpha) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BACKGROUND, 0, 1, 0, g, 0.0f) ;
		}
		
		for ( int g = 0; g < 2; g++ ) {
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BORDER, 0, 0, 0, g, 0.0f) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BORDER, 1, 0, 0, g, 0.0f) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BORDER, 1, 0, 1, g, enabledAlpha) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BORDER, 1, 1, 0, g, disabledAlpha) ;
			setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow(SECTION_BORDER, 0, 1, 0, g, disabledAlpha) ;
		}
		
		for ( int f = 0; f < 2; f++ ) {
			for ( int p = 0; p < 2; p++ ) {
				for ( int g = 0; g < 2; g++ ) {
					setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow( SECTION_BORDER_BOTTOM, 0, f, p, g, disabledAlpha ) ;
					setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow( SECTION_BORDER_BOTTOM, 1, f, p, g, enabledAlpha ) ;
				}
			}
		}
		
		generateAllPaints() ;
	}
	
	/**
	 * Sets this button to its default appearance
	 * @param enabledAlpha
	 * @param disabledAlpha
	 * @param gradientAlpha
	 * @param glowAlpha
	 */
	public void resetToDefaultAppearance( int baseColor, float enabledAlpha, float disabledAlpha, float gradientAlpha, float [] glowAlpha ) {
		mColorDefault = baseColor ;
		resetToDefaultAppearance( enabledAlpha, disabledAlpha, gradientAlpha, glowAlpha ) ;
	}
	
	
	private void setIfNotSet_AlphaMultGradient_enabledFocusedPressedGlow( int e, int f, int p, int g, float [] alphaMult, float [] pos ) {
		if ( mSetGradient_enabledFocusedPressedGlow[e][f][p][g] == SET_NONE ) {
			mSetGradient_enabledFocusedPressedGlow[e][f][p][g] = SET_ALPHA_MULT ;
			mAlphaMultGradient_enabledFocusedPressedGlow[e][f][p][g] = alphaMult ;
			mPositionsGradient_enabledFocusedPressedGlow[e][f][p][g] = pos ;
		}
	}
	
	private void setIfNotSet_AlphaMultTopGradient_enabledFocusedPressedGlow( int e, int f, int p, int g, float [] alphaMult, float [] pos ) {
		if ( mSetTopGradient_enabledFocusedPressedGlow[e][f][p][g] == SET_NONE ) {
			mSetTopGradient_enabledFocusedPressedGlow[e][f][p][g] = SET_ALPHA_MULT ;
			mAlphaMultTopGradient_enabledFocusedPressedGlow[e][f][p][g] = alphaMult ;
			mPositionsTopGradient_enabledFocusedPressedGlow[e][f][p][g] = pos ;
		}
	}
	
	private void setIfNotSet_mAlphaMultSection_enabledFocusedPressedGlow( int section, int e, int f, int p, int g, float mult ) {
		if ( mSetSection_enabledFocusedPressedGlow[section][e][f][p][g] == SET_NONE ) {
			mSetSection_enabledFocusedPressedGlow[section][e][f][p][g] = SET_ALPHA_MULT ;
			mAlphaMultSection_enabledFocusedPressedGlow[section][e][f][p][g] = mult ;
		}
	}
	
	
	
	@Override
	synchronized public void setEnabled(boolean enabled) {
		super.setEnabled(enabled) ;
		// enabled means clickable - this is a button, after all.
		super.setClickable(true) ;
	}
	
	
}
