package com.peaceray.quantro.view.controls;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;
import com.peaceray.quantro.consts.GlobalTestSettings;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class InvisibleButtonSimple extends View
		implements InvisibleButtonInterface {

	@SuppressWarnings("unused")
	private static final String TAG = "InvisibleButtonSimple" ;
	
	public long MAX_MILLIS_TAP ;
	public long MAX_MILLIS_DOUBLE_TAP ;

	private String name ;
	private String doubleTapName ;
	private String text ;
	private TextView textView ;
	private WeakReference<Delegate> mwrDelegate ;
	private boolean visible ;
	
	private boolean touchable ;
	
	private int backgroundColor ;
	private boolean showWhenPressed ;
	private boolean captureTouches ;
	private Drawable drawable ;
	private boolean drawableFits ;
	private Drawable drawableAlt ;
	private boolean drawableAltFits ;
	private boolean useDrawableAlt ;
	
	private boolean pressed ;
	
	private Paint onDraw_paint ;
	private Rect textBounds ;
	
	private long mLastPressTime ;
	private long mLastPressDuration ;
	private long mPressTime ;
	private int mNumPreviousTaps ;
	
	// default
	private boolean default_showWhenPressed ;
	
	// Implementing a custom view - necessary methods.
	public InvisibleButtonSimple( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		setFocusableInTouchMode(true) ;
		
		// Center the text view.
		// textView = new TextView( context ) ;
		// textView.setBackgroundColor(0xFFFFFF00) ;
		
		// Set our background to a 9-patch.
		// This is done later, in show()
		// Resources res = context.getResources() ;
		// Drawable ninePatch = res.getDrawable( R.drawable.button ) ;
		// this.setBackgroundDrawable( ninePatch ) ;
		
		setDefaults(context) ;
		setAttributes(context,attrs) ;
		
		hide() ;
	}
	
	public InvisibleButtonSimple( Context context, AttributeSet attrs, int defStyle ) {
		super(context,attrs,defStyle) ;
		setFocusableInTouchMode(true) ;
		
		// Center the text view.
		// textView = new TextView( context ) ;
		
		// Set our background to a 9-patch.
		// Resources res = context.getResources() ;
		// Drawable ninePatch = res.getDrawable( R.drawable.button ) ;
		// this.setBackgroundDrawable( ninePatch ) ;
		
		setDefaults(context) ;
		setAttributes(context, attrs) ;
		
		hide() ;
	}
	
	private void setDefaults( Context context ) {
		onDraw_paint = new Paint() ;
		textBounds = new Rect() ;
		
		onDraw_paint.setTextSize(24);
		onDraw_paint.setAntiAlias(true) ;
		onDraw_paint.setColor(0xFFFFFFFF);
		// paint before name; setText will measure bounds with this paint.
		
		this.setName(null) ;
		this.setText(null) ;
		
		touchable = true ;
		captureTouches = true ;
		showWhenPressed = true ;
		
		backgroundColor = 0x00000000 ;
		
		Resources res = getResources() ;
		MAX_MILLIS_TAP = 
				GlobalTestSettings.scaleGameInputTimeWindow( res.getInteger(R.integer.controls_button_tap_max_millis) ) ;
		MAX_MILLIS_DOUBLE_TAP =
				GlobalTestSettings.scaleGameInputTimeWindow( res.getInteger(R.integer.controls_button_double_tap_max_millis) ) ;
		
		mLastPressTime = 0 ;
		mLastPressDuration = 0 ;
		mPressTime = 0 ;
		mNumPreviousTaps = 0 ;
		
		drawable = null ;
		drawableAlt = null ;
		useDrawableAlt = false ;
		
		default_showWhenPressed = true ;
	}
	
	private void setAttributes( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.InvisibleButtonInterface);
			
			final int N = a.getIndexCount();
			for (int i = 0; i < N; ++i)
			{
			    int attr = a.getIndex(i);
			    switch (attr)
			    {
			        case R.styleable.InvisibleButtonInterface_button_name:
			            String myName = a.getString(attr);
			            this.setName(myName) ;
			            //Log.d(TAG, "Setting button name to " + myName) ;
			            break;
			        case R.styleable.InvisibleButtonInterface_button_double_tap_name:
			        	doubleTapName = a.getString(attr) ;
			        	break ;
			        case R.styleable.InvisibleButtonInterface_button_text:
			            String myText = a.getString(attr);
			            this.setText(myText) ;
			            //Log.d(TAG, "Setting button text to " + myText) ;
			            break;
			        case R.styleable.InvisibleButtonInterface_button_icon:
			        	drawable = a.getDrawable(attr) ;
			        	break ;
			        case R.styleable.InvisibleButtonInterface_button_icon_alt:
			        	drawableAlt = a.getDrawable(attr) ;
			        	break ;
			        case R.styleable.InvisibleButtonInterface_button_show:
			        	showWhenPressed = a.getBoolean(attr, true) ;
			        	default_showWhenPressed = a.getBoolean(attr, true) ;
			        	break ;
			        case R.styleable.InvisibleButtonInterface_button_capture_touches:
			        	captureTouches = a.getBoolean(attr, true) ;
			        	break ;
			        case R.styleable.InvisibleButtonInterface_button_background_color:
			        	backgroundColor = a.getColor(attr, 0x00000000) ;
			        	break ;
			    }
			}
			a.recycle();
	}
	
	// Changing sizes - we must also resize the subordinate text view.
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
		super.onLayout(changed, left, top, right, bottom) ;
		
		resizeContent( right - left, bottom - top ) ;
		//textView.layout(0, 0, right-left, bottom-top) ;
	}
	
	protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
		super.onSizeChanged(w, h, oldw, oldh) ;
		
		resizeContent( w, h ) ;
	}
	
	protected void resizeContent( int w, int h ) {
		drawableFits = resizeDrawable( w, h, drawable ) ;
		drawableAltFits = resizeDrawable( w, h, drawableAlt ) ;
	}
	
	protected boolean resizeDrawable( int w, int h, Drawable d ) {
		if ( d != null ) {
			int dW = w - (getPaddingLeft() + getPaddingRight()) ;
			int dH = h - (getPaddingTop() + getPaddingBottom()) ;
			if ( d.getIntrinsicWidth() > -1 )
				dW = Math.min( dW, d.getIntrinsicWidth() ) ;
			if ( d.getIntrinsicHeight() > -1 )
				dH = Math.min( dH, d.getIntrinsicHeight() ) ;
			
			// center the bounds within the padded area.
			int dMarginWidth = ((w - (getPaddingLeft() + getPaddingRight())) - dW)/2 ;
			int dMarginHeight = ((h - (getPaddingTop() + getPaddingBottom())) - dH)/2 ;
			
			int l = getPaddingLeft() + dMarginWidth ;
			int t = getPaddingTop() + dMarginHeight ;
			int r = getPaddingLeft() + dMarginWidth + dW ;
			int b = getPaddingBottom() + dMarginHeight + dH ;
			
			
			d.setBounds( l, t, r, b ) ;
			return ( d.getIntrinsicWidth() == -1 || d.getIntrinsicWidth() <= (r - l) )
					&& ( d.getIntrinsicHeight() == -1 || d.getIntrinsicHeight() <= (b - t) ) ;		
		}
		
		return false ;
	}
	
	protected void onDraw( Canvas canvas ) {
		super.onDraw(canvas) ;
		if ( visible ) {
			super.onDraw(canvas) ;
	        canvas.drawColor(backgroundColor) ;
	        
	        // Get bounds for this text...
	        if ( text != null ) {
		        int h = this.getBottom() - this.getTop() ;
		        int w = this.getRight() - this.getLeft() ;
		        
		        // Position center of text at center of this.  In other words,
		        // put top-left of text at center - 1/2 text bounds.
		        int posX = w/2 - textBounds.width()/2 ;
		        int posY = h/2 - textBounds.height()/2 ;
	
		        //Log.d(TAG, "drawing text " + text + " at " + posX + "  " + posY ) ;
				canvas.drawText(text, posX, posY, onDraw_paint) ;
				//textView.draw(canvas) ;
	        }
	        
	        if ( !useDrawableAlt && drawable != null && drawableFits )
	        	drawable.draw(canvas) ;
	        else if ( useDrawableAlt && drawableAlt != null && drawableAltFits )
	        	drawableAlt.draw(canvas) ;
		}
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		if ( !captureTouches || !touchable )
			return false ;
		
		int actionMasked = event.getAction() & MotionEvent.ACTION_MASK ;
		
		Log.d(TAG, "touch event") ;
		
		switch( actionMasked ) {
		case MotionEvent.ACTION_DOWN:
			if ( !pressed )
				press(true) ;
			break ;
			
		case MotionEvent.ACTION_POINTER_DOWN:
			if ( !pressed )
				press(true) ;
			break ;
			
		case MotionEvent.ACTION_UP:
			if ( pressed )
				release(true) ;
			break ;
			
		case MotionEvent.ACTION_POINTER_UP:
			if ( pressed )
				release(true) ;
			break ;
			
		case MotionEvent.ACTION_CANCEL:
			if ( pressed )
				release(true) ;
			break ;
		}
		
		return true ;
	}
	
	public void setTouchable( boolean touchable ) {
		this.touchable = touchable ;
	}
	
	public void setUseAlt( boolean useAlt ) {
		this.useDrawableAlt = useAlt ;
	}
	
	public boolean isTouchable() {
		return touchable ;
	}
	
	public void press( boolean tellDelegate ) {
		press( tellDelegate, false ) ;
	}
	
	public void press( boolean tellDelegate, boolean forceResetTaps ) {
		if ( !pressed ) {
			mPressTime = System.currentTimeMillis() ;
			if ( !forceResetTaps
					&& mLastPressDuration < MAX_MILLIS_TAP
					&& mPressTime - mLastPressTime + mLastPressDuration < MAX_MILLIS_DOUBLE_TAP )
				mNumPreviousTaps++ ;
			else
				mNumPreviousTaps = 0 ;
		}
		if ( showWhenPressed )
			this.show();
		boolean wasPressed = this.pressed ;
		this.pressed = true ;
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null && tellDelegate && !wasPressed )
			delegate.buttonPressed(this, mNumPreviousTaps) ;
	}
	
	public void release( boolean tellDelegate ) {
		if ( pressed ) {
			mLastPressDuration = System.currentTimeMillis() - mPressTime ;
			mLastPressTime = mPressTime ;
		}
		if ( showWhenPressed )
			this.hide();
		boolean wasPressed = this.pressed ;
		this.pressed = false ;
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null && tellDelegate && wasPressed )
			delegate.buttonReleased(this, mNumPreviousTaps) ;
	}
	
	@Override
	public void hide() {
		// Make invisible.
		visible = false ;
		//this.setBackgroundDrawable( empty ) ;
		invalidate() ;
	}

	@Override
	public void show() {
		visible = true ;
		invalidate() ;
	}

	@Override
	public boolean shown() {
		return visible ;
	}
	
	@Override
	public boolean pressed() {
		return pressed ;
	}

	@Override
	public String name() {
		return name;
	}
	
	@Override
	public String name( int taps ) {
		if ( taps % 2 == 1 && doubleTapName != null )
			return doubleTapName ;
		return name ;
	}

	@Override
	public String text() {
		return textView.getText().toString() ;
	}

	@Override
	public Delegate delegate() {
		return mwrDelegate.get() ;
	}

	@Override
	public void setName(String n) {
		name = n ;
	}

	@Override
	public void setText(String t) {
		//textView.setText(t) ;
		text = t ;
		if ( text == null )
			textBounds.set(0,0,0,0) ;
		else
			onDraw_paint.getTextBounds(text,0,text.length(),textBounds) ;
		invalidate() ;
	}
	
	@Override
	public void setDelegate( Delegate invCont ) {
		mwrDelegate = new WeakReference<Delegate>(invCont) ;
	}
	
	
	// Do we respond directly to touches?
	public void setCaptureTouches( boolean on ) {
		captureTouches = on ;
	}
	
	public void setShowWhenPressed( boolean on ) {
		showWhenPressed = on ;
	}
	
	public void setShowWhenPressedDefault() {
		showWhenPressed = default_showWhenPressed ;
	}

}
