package com.peaceray.quantro.view.drawable;

import com.peaceray.quantro.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

public class BottomBorderDrawable extends Drawable {
	
	private static final double SIDE_PORTION_VISIBLE = 0.4 ;
	private static final float HAIRLINE_SIDE_DP = 1 ;
	private static final float HAIRLINE_BOTTOM_DP = 1.5f ;
	
	private static final int SIDE_LEFT = 0 ;
	private static final int SIDE_RIGHT = 1 ;
	private static final int SIDE_BOTTOM = 2 ;
	
	
	// We draw in several steps.
	// 1: Fill black everywhere except under shadow
	// 2: draw hairline border, with sides and bottom
	// 3: draw shadow under the hairline.
	
	private int hairlineSideDP ;
	private int hairlineBottomDP ;
	
	
	Rect mBounds ;
	
	Rect mRectSolidBox ;
	Rect [] mRectBorders ;
	
	Paint mPaintSolidBox ;
	Paint mPaintSides ;

	public BottomBorderDrawable( Context context ) {
		mBounds = new Rect() ;
		mRectSolidBox = new Rect() ;
		mRectBorders = new Rect[3] ;
		for ( int i = 0 ; i < mRectBorders.length; i++ )
			mRectBorders[i] = new Rect() ;
		
		mPaintSolidBox = new Paint() ;
		mPaintSides = new Paint() ;
		
		mPaintSolidBox.setAntiAlias(true) ;
		mPaintSides.setAntiAlias(true) ;

		mPaintSolidBox.setColor(0xff000000) ;
		mPaintSides.setColor(0xff808080) ;
		
		hairlineSideDP = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				HAIRLINE_SIDE_DP, context.getResources().getDisplayMetrics()) ;
		hairlineBottomDP = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				HAIRLINE_BOTTOM_DP, context.getResources().getDisplayMetrics()) ;
	}
	
	@Override
	public void setBounds( int left, int top, int right, int bottom ) {
		
		int blankPixels = (int)Math.round(( bottom - top ) * SIDE_PORTION_VISIBLE) ;
		
		mBounds.set(left, top, right, bottom) ;
		mRectSolidBox.set(left, top, right, bottom ) ;
		mRectBorders[SIDE_LEFT].set(left, top+blankPixels, left + hairlineSideDP, bottom) ;
		mRectBorders[SIDE_RIGHT].set(right - hairlineSideDP, top+blankPixels, right, bottom) ;
		mRectBorders[SIDE_BOTTOM].set(left, bottom - hairlineBottomDP, right, bottom) ;
	}
	
	@Override
	public void setBounds( Rect bounds ) {
		setBounds( bounds.left, bounds.top, bounds.right, bounds.bottom ) ;
	}

	@Override
	public void draw(Canvas canvas) {
		canvas.drawRect(mRectSolidBox, mPaintSolidBox) ;
		for ( int i = 0; i < mRectBorders.length; i++ )
			canvas.drawRect(mRectBorders[i], mPaintSides) ;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT ;
	}

	@Override
	public void setAlpha(int alpha) {
		mPaintSolidBox.setAlpha(alpha) ;
		mPaintSides.setAlpha(alpha) ;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mPaintSolidBox.setColorFilter(cf) ;
		mPaintSides.setColorFilter(cf) ;
	}

}
