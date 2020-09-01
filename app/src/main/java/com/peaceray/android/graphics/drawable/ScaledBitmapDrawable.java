package com.peaceray.android.graphics.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

public class ScaledBitmapDrawable extends Drawable {
	
	Bitmap mBitmap ;
	Rect mBounds ;
	Rect mScaleRect ; 
	ScaleType mScaleType ;
	
	Paint mPaint ;		// for color filtering
	
	public enum ScaleType {
		/**
		 * Center the image in the view, but perform no scaling.
		 */
		CENTER,
		
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so
		 * that both dimensions (width and height) of the image will be equal 
		 * to or larger than the corresponding dimension of the view (minus padding).
		 * The image is then centered in the view.
		 */
		CENTER_CROP,
		
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so
		 * that both dimensions (width and height) of the image will be equal 
		 * to or larger than the corresponding dimension of the view (minus padding),
		 * AND at least one of them matches the dimension exactly.  The
		 * image is then centered in the view.
		 */
		CENTER_MATCH_CROP,
		
		/**
		 * Scale the image uniformly (maintain the image's aspect ratio) so that
		 * both dimensions (width and height) of the image will be equal to or
		 * less than the corresponding dimension of the view (minus padding).
		 * The image is then centered in the view. 
		 */
		CENTER_INSIDE,
		
		/**
		 * Compute a scale that will maintain the original src aspect ratio,
		 * but will also ensure that src fits entirely inside dst. At least 
		 * one axis (X or Y) will fit exactly. The result is centered inside
		 * dst.
		 */
		FIT_CENTER,
		
		/**
		 * Compute a scale that will maintain the original src aspect ratio,
		 * but will also ensure that src fits entirely inside dst. At least one
		 * axis (X or Y) will fit exactly. END aligns the result to the right and 
		 * bottom edges of dst.
		 */
		FIT_END,
		
		/**
		 * Compute a scale that will maintain the original src aspect ratio, but
		 * will also ensure that src fits entirely inside dst. At least one axis
		 * (X or Y) will fit exactly. START aligns the result to the left and top
		 * edges of dst.
		 */
		FIT_START,
		
		/**
		 * Scale in X and Y independently, so that src matches dst exactly.
		 * This may change the aspect ratio of the src.
		 */
		FIT_XY
	}
	
	
	
	public ScaledBitmapDrawable( Bitmap b, ScaleType scaleType ) {
		if ( b == null || scaleType == null )
			throw new NullPointerException("No null pointers!") ;
		mBitmap = b ;
		mScaleType = scaleType ;
		mBounds = new Rect(0,0,0,0) ;
		mScaleRect = new Rect(0,0,0,0) ;
	}
	
	
	@Override
	public int getIntrinsicWidth() {
		return mBitmap.getWidth() ;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return mBitmap.getHeight() ;
	}
	

	@Override
	public void draw(Canvas canvas) {
		canvas.save() ;
		canvas.clipRect(mBounds, Region.Op.INTERSECT) ;
		canvas.drawBitmap(mBitmap, null, mScaleRect, mPaint) ;
		canvas.restore() ;
	}

	@Override
	public int getOpacity() {
		Bitmap.Config config = mBitmap.getConfig() ;
		if ( config != null ) {
			if ( config == Bitmap.Config.RGB_565 ) {
				// alpha determined by our scale type.
				// for now, though, default to TRANSPARENT (1 bit alpha)
				return PixelFormat.TRANSPARENT ;
			}
		}
		
		return PixelFormat.TRANSLUCENT ;
	}

	@Override
	public void setAlpha(int alpha) {
		mPaint.setAlpha(alpha) ;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mPaint.setColorFilter(cf) ;
	}

	public void setSource( Bitmap b ) {
		if ( b == null )
			throw new NullPointerException("No null pointers!") ;
		mBitmap = b ;
		resizeContent() ;
	}
	
	public void setScaleType( ScaleType scaleType ) {
		if ( scaleType == null )
			throw new NullPointerException("No null pointers!") ;
		mScaleType = scaleType ;
		resizeContent() ;
	}
	
	public void setBounds( Rect bounds ) {
		mBounds.set(bounds) ;
		resizeContent() ;
	}
	
	public void setBounds( int left, int top, int right, int bottom ) {
		mBounds.set(left, top, right, bottom) ;
		resizeContent() ;
	}
	
	
	/**
	 * Resizes mScaleRect according to mBounds and our scale type.
	 */
	private void resizeContent() {
		int w = mBitmap.getWidth(), h = mBitmap.getHeight() ;
		int boundsW = mBounds.width(), boundsH = mBounds.height() ;
		double scale = 1 ;
		
		if ( boundsW == 0 || boundsH == 0 || w == 0 || h == 0 ) {
			mScaleRect.set(0,0,0,0) ;
			return ;
		}
		
		// check the javadoc of these scale types for documentation (e.g., hover in Eclipse)
		switch( mScaleType ) {
		case CENTER:
			mScaleRect.set(0,0,w,h) ;
			mScaleRect.offset(
					mBounds.centerX() -w/2,
					mBounds.centerY() -h/2) ;
			break ;
			
		case CENTER_CROP:
			scale = Math.max(scale, ((double)boundsW) / w) ;
			scale = Math.max(scale, ((double)boundsH) / h) ;
			w *= scale ;
			h *= scale ;
			mScaleRect.set(0,0,w,h) ;
			mScaleRect.offset(
					mBounds.centerX() -w/2,
					mBounds.centerY() -h/2) ;
			break ;
			
		case CENTER_MATCH_CROP:
			scale = 0 ;
			// maximum scale to match
			scale = Math.max(scale, ((double)boundsW) / w) ;
			scale = Math.max(scale, ((double)boundsH) / h) ;
			w *= scale ;
			h *= scale ;
			mScaleRect.set(0,0,w,h) ;
			mScaleRect.offset(
					mBounds.centerX() -w/2,
					mBounds.centerY() -h/2) ;
			break ;
			
		case CENTER_INSIDE:
			scale = Math.min(scale, ((double)boundsW) / w) ;
			scale = Math.min(scale, ((double)boundsH) / h) ;
			w *= scale ;
			h *= scale ;
			mScaleRect.set(0,0,w,h) ;
			mScaleRect.offset(
					mBounds.centerX() -w/2,
					mBounds.centerY() -h/2) ;
			break ;
			
		case FIT_CENTER:
		case FIT_END: 
		case FIT_START:
			if ( w > boundsW || h > boundsH ) {
				// requires downscaling to fit
				scale = Math.min(scale, ((double)boundsW) / w) ;
				scale = Math.min(scale, ((double)boundsH) / h) ;
			} else {
				// upscale to fit; take the smaller of the two upscale
				// values to avoid overshooting
				scale = Double.MAX_VALUE ;
				scale = Math.min(scale, ((double)boundsW) / w) ;
				scale = Math.min(scale, ((double)boundsH) / h) ;
			}
			
			w *= scale ;
			h *= scale ;
			mScaleRect.set(0,0,w,h) ;
			
			if ( mScaleType == ScaleType.FIT_CENTER ) {
				mScaleRect.offset(
						mBounds.centerX() -w/2,
						mBounds.centerY() -h/2) ;
			} else if ( mScaleType == ScaleType.FIT_END ) {
				mScaleRect.offset(
						mBounds.right - w,
						mBounds.bottom - h) ;
			} else {
				mScaleRect.offset(
						mBounds.left,
						mBounds.top) ;
			}
			break ;

		case FIT_XY:
			mScaleRect.set(mBounds) ;
			break ;
		}
	}
	
}
