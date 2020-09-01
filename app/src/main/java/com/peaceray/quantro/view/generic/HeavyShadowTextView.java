package com.peaceray.quantro.view.generic;


import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;

public class HeavyShadowTextView extends MagicTextView {

	public HeavyShadowTextView(Context context) {
		super(context);
		init(context) ;
	}
	public HeavyShadowTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context) ;
	}
	public HeavyShadowTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context) ;
	}
	
	private void init(Context context) {
		float r = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.333f, getResources().getDisplayMetrics());
		float x, y ;
		if ( r >= 5 ) {
			x = y = 2 ;
		} else {
			x = y = 1 ;
		}
		
		int alpha = Color.alpha( getCurrentTextColor() ) ;
		int shadow = Color.argb(alpha, 0, 0, 0) ;
		this.clearOuterShadows() ;
		this.addOuterShadow(r, 0, 0, shadow) ;
		this.addOuterShadow(r, x, y, shadow) ;
	}

	
	@Override
	public void setTextColor( int color ) {
		super.setTextColor(color) ;
		
		float r = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.333f, getResources().getDisplayMetrics());
		float x, y ;
		if ( r >= 5 ) {
			x = y = 2 ;
		} else {
			x = y = 1 ;
		}
		int alpha = Color.alpha( getCurrentTextColor() ) ;
		int shadow = Color.argb(alpha, 0, 0, 0) ;
		this.clearOuterShadows() ;
		this.addOuterShadow(r, 0, 0, shadow) ;
		this.addOuterShadow(r, x, y, shadow) ;
	}
	
}
