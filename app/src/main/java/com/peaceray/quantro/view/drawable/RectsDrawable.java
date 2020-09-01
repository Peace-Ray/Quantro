package com.peaceray.quantro.view.drawable;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

/**
 * A Drawable object which draws collections of Rectangles.  Each Rectangle
 * is drawn as a solid color, specified in advance, and they are drawn in 
 * a specified order.
 * 
 * A note on resizing: at present, RectsDrawable does not support resizing,
 * except with setIntrinsicSize.  All provided Rects will be drawn relative
 * to the current top-left of its bounds.
 * 
 * @author Jake
 *
 */
public class RectsDrawable extends Drawable {
	
	private static class ContentRect {
		
		private ContentRect() {
			
		}
		
		private static ContentRect newSolid( Object tag, int color, Rect bounds ) {
			return newInstance( tag, Type.SOLID, color, 0, bounds.left, bounds.top, bounds.right, bounds.bottom) ;
		}
		
		private static ContentRect newSolid( Object tag, int color, int left, int top, int right, int bottom ) {
			return newInstance( tag, Type.SOLID, color, 0, left, top, right, bottom ) ;
		}
		
		private static ContentRect newEmpty( Object tag, int color, int borderWidth, Rect bounds ) {
			return newInstance( tag, Type.EMPTY, color, borderWidth, bounds.left, bounds.top, bounds.right, bounds.bottom) ;
		}
		
		private static ContentRect newEmpty( Object tag, int color, int borderWidth, int left, int top, int right, int bottom ) {
			return newInstance( tag, Type.EMPTY, color, borderWidth, left, top, right, bottom ) ;
		}
		
		private static ContentRect newInstance( Object tag, Type type, int color, int borderWidth, int left, int top, int right, int bottom ) {
			ContentRect cr = new ContentRect() ;
			cr.mColor = color ;
			cr.mBounds = new Rect(left, top, right, bottom) ;
			cr.mType = type ;
			cr.mBorderWidth = borderWidth ;
			cr.mVisible = true ;
			cr.mTag = tag ;
			
			return cr ;
		}
		
		private int mColor ;
		private Rect mBounds ;
		private Type mType ;
		private int mBorderWidth ;
		private boolean mVisible ;
		private Object mTag ;
		
		private boolean mHasShadow = false ;
		private Drawable mDrawableRelativeShadow = null ;
	}
	
	public enum Type {
		/**
		 * Drawn as a solid color.
		 */
		SOLID,
		
		/**
		 * Drawn as a border only, using a fixed-width line.
		 */
		EMPTY,
	}
	
	public enum Layer {
		
		/**
		 * Layer rectangles in the order they were set, drawing the
		 * shadows of later rectangles on top of older rectangles.
		 * 
		 */
		ORDER,
		
		/**
		 * Place all rectangles in the same "layer," drawing all
		 * shadows first, then filling in a separate pass.  No
		 * Rectangle will shadow any other.
		 */
		FLAT,
	}
	
	private int mIntrinsicWidth, mIntrinsicHeight ;
	private Rect mBounds ;
	
	private float mAlphaMult ;

	private ArrayList<ContentRect> mContentRects ;
	private Paint mDrawPaint ;
	private ColorFilter mColorFilter ;
	private Rect mDrawRect ;
	private Rect mClipRect ;
	private Path mDrawPath ;
	
	private Layer mLayer ;
	
	public RectsDrawable( Context context ) {
		mIntrinsicWidth = 0 ;
		mIntrinsicHeight = 0 ;
		
		mBounds = new Rect(0,0,0,0) ;
		mAlphaMult = 1.0f ;
		
		mContentRects = new ArrayList<ContentRect>() ;
		mDrawPaint = new Paint() ;
		mDrawRect = new Rect() ;
		mClipRect = new Rect() ;
		mDrawPath = new Path() ;
		
		mLayer = Layer.ORDER ;
	}

	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONTENT CREATION
	//
	
	public void setIntrinsicSize( int width, int height ) {
		mIntrinsicWidth = width ;
		mIntrinsicHeight = height ;
	}
	
	/**
	 * Sets the intrinsic height and width to center its content
	 * within the available space.  Attempts to match the "left
	 * negative space" (i.e. number of pixels between 0 and the first
	 * content rect) with the "right negative space," and the same
	 * for top and bottom.
	 */
	public void setIntrinsicSizeToCenterContent() {
		
		if ( mContentRects.size() == 0 ) {
			mIntrinsicWidth = 0 ;
			mIntrinsicHeight = 0 ;
			return ;
		}
		
		Rect contentBounds = new Rect(
				Integer.MAX_VALUE, Integer.MAX_VALUE,
				Integer.MIN_VALUE, Integer.MIN_VALUE) ;
		for ( int i = 0; i < mContentRects.size(); i++ ) {
			ContentRect cr = mContentRects.get(i) ;
			
			contentBounds.left 		= Math.min(contentBounds.left, cr.mBounds.left) ;
			contentBounds.top 		= Math.min(contentBounds.top, cr.mBounds.top) ;
			contentBounds.right 	= Math.max(contentBounds.right, cr.mBounds.right) ;
			contentBounds.bottom 	= Math.max(contentBounds.bottom, cr.mBounds.bottom) ;
		}
		
		mIntrinsicWidth = contentBounds.right + contentBounds.left ;
		mIntrinsicHeight = contentBounds.bottom + contentBounds.top ;
	}
	
	
	public void setLayer( Layer layer ) {
		mLayer = layer ;
	}
	
	
	public int getIndex( Object tag ) {
		if ( tag == null )
			return -1 ;
		for ( int i = 0; i < mContentRects.size(); i++ ) {
			if ( tag.equals( mContentRects.get(i).mTag ) ) {
				return i ;
			}
		}
		return -1 ;
	}
	
	
	public void clearRects() {
		mContentRects.clear() ;
	}
	
	
	/**
	 * Adds the specified Rectangle as the "next to draw" in the queue.
	 * Rectangles will be drawn in their add-order.
	 * 
	 * Types:
	 * 
	 * 		SOLID:  The specified color will be drawn to fill the 
	 * 				provided bounds.  'borderWidth' is ignored.
	 * 
	 * 		EMPTY:	The specified color will be drawn as a boundary,
	 * 				with line-width borderWidth, at the edge of bounds.
	 * 
	 * @param type
	 * @param color
	 * @param borderWidth
	 * @param bounds
	 */
	public int addRect( Type type, int color, int borderWidth, Rect bounds ) {
		return addRect(type, color, borderWidth, bounds, null ) ;
	}
	
	
	/**
	 * Adds the specified Rectangle as the "next to draw" in the queue.
	 * Rectangles will be drawn in their add-order.
	 * 
	 * Types:
	 * 
	 * 		SOLID:  The specified color will be drawn to fill the 
	 * 				provided bounds.  'borderWidth' is ignored.
	 * 
	 * 		EMPTY:	The specified color will be drawn as a boundary,
	 * 				with line-width borderWidth, at the edge of bounds.
	 * 
	 * @param type
	 * @param color
	 * @param borderWidth
	 * @param bounds
	 */
	public int addRect( Type type, int color, int borderWidth, Rect bounds, Object tag ) {
		ContentRect cr = null ;
		
		switch( type ) {
		case SOLID:
			cr = ContentRect.newSolid(tag, color, bounds) ;
			break ;
		case EMPTY:
			cr = ContentRect.newEmpty(tag, color, borderWidth, bounds) ;
			break ;
		}
		
		if ( cr == null )
			throw new IllegalArgumentException("Arguments now supported.") ;
		mContentRects.add(cr) ;
		
		return mContentRects.size() -1 ;
	}
	
	/**
	 * Adds the specified Rectangle as the "next to draw" in the queue.
	 * Rectangles will be drawn in their add-order.
	 * 
	 * Types:
	 * 
	 * 		SOLID:  The specified color will be drawn to fill the 
	 * 				provided bounds.  'borderWidth' is ignored.
	 * 
	 * 		EMPTY:	The specified color will be drawn as a boundary,
	 * 				with line-width borderWidth, at the edge of bounds.
	 * 
	 * @param type
	 * @param color
	 * @param borderWidth
	 * @param l
	 * @param t
	 * @param r
	 * @param b
	 */
	public int addRect( Type type, int color, int borderWidth, int l, int t, int r, int b ) {
		return addRect( type, color, borderWidth, l, t, r, b, null ) ;
	}
	
	
	/**
	 * Adds the specified Rectangle as the "next to draw" in the queue.
	 * Rectangles will be drawn in their add-order.
	 * 
	 * Types:
	 * 
	 * 		SOLID:  The specified color will be drawn to fill the 
	 * 				provided bounds.  'borderWidth' is ignored.
	 * 
	 * 		EMPTY:	The specified color will be drawn as a boundary,
	 * 				with line-width borderWidth, at the edge of bounds.
	 * 
	 * @param type
	 * @param color
	 * @param borderWidth
	 * @param l
	 * @param t
	 * @param r
	 * @param b
	 */
	public int addRect( Type type, int color, int borderWidth, int l, int t, int r, int b, Object tag ) {
		ContentRect cr = null ;
		
		switch( type ) {
		case SOLID:
			cr = ContentRect.newSolid(tag, color, l, t, r, b) ;
			break ;
		case EMPTY:
			cr = ContentRect.newEmpty(tag, color, borderWidth, l, t, r, b) ;
			break ;
		}
		
		if ( cr == null )
			throw new IllegalArgumentException("Arguments now supported.") ;
		
		mContentRects.add(cr) ;
		
		return mContentRects.size() -1 ;
	}
	

	public void setRectHasShadow( Context context, Object tag, boolean hasShadow ) {
		setRectHasShadow( context, getIndex(tag), hasShadow ) ;
	}
	
	
	public void setRectHasShadow( Context context, int index, boolean hasShadow ) {
		if ( index < 0 || index >= mContentRects.size() )
			return ;
		
		ContentRect cr = mContentRects.get(index) ;
		
		cr.mHasShadow = hasShadow ;
		if ( hasShadow ) {
			mClipRect.set(cr.mBounds) ;
			mClipRect.offset(mBounds.left, mBounds.top) ;
			cr.mDrawableRelativeShadow = DropShadowDrawableFactory.getDrawable(context, mClipRect)  ;
			cr.mDrawableRelativeShadow = DropShadowDrawableFactory.newInstance(cr.mDrawableRelativeShadow) ;
		} else {
			cr.mDrawableRelativeShadow = null ;
		}
	}
	
	
	public void setRectVisible( Object tag, boolean visible ) {
		setRectVisible( getIndex(tag), visible ) ;
	}
	
	public void setRectVisible( int index, boolean visible ) {
		if ( index < 0 || index >= mContentRects.size() )
			return ;
		
		ContentRect cr = mContentRects.get(index) ;
		cr.mVisible = visible ;
	}
	
	/**
	 * Sets the specified rectangle's position on screen.  Rectangles cannot
	 * be resized once created, but they can be moved.  This method puts
	 * the rectangle's top-left at the specified coordinates (if it exists).
	 * @param tag
	 * @param left
	 * @param top
	 */
	public void setRectPosition( Object tag, int left, int top ) {
		setRectPosition( getIndex(tag), left, top ) ;
	}
	
	/**
	 * Sets the specified rectangle's position on screen.  Rectangles cannot
	 * be resized once created, but they can be moved.  This method puts
	 * the rectangle's top-left at the specified coordinates (if it exists).
	 * @param index
	 * @param left
	 * @param top
	 */
	public void setRectPosition( int index, int left, int top ) {
		if ( index < 0 || index >= mContentRects.size() )
			return ;
		
		ContentRect cr = mContentRects.get(index) ;
		cr.mBounds.offsetTo(left, top) ;
		if ( cr.mHasShadow ) {
			mClipRect.set( cr.mBounds ) ;
			mClipRect.offset(mBounds.left, mBounds.top) ;
			cr.mDrawableRelativeShadow = DropShadowDrawableFactory.setBoundsToFit( cr.mDrawableRelativeShadow, mClipRect ) ;
		}
	}
	
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// OVERRIDE DRAWABLE METHODS
	//
	
	
	@Override
	public int getIntrinsicWidth() {
		return mIntrinsicWidth ;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return mIntrinsicHeight ;
	}
	
	@Override
	public void setBounds( int left, int top, int right, int bottom ) {
		super.setBounds(left, top, right, bottom) ;
		if ( mBounds.left != left || mBounds.top != top || mBounds.right != right || mBounds.bottom != bottom ) {
			mBounds.set(left, top, right, bottom) ;
			
			// resize shadows
			Iterator<ContentRect> iterator = mContentRects.iterator() ;
			for ( ; iterator.hasNext() ; ) {
				ContentRect cr = iterator.next() ;
				if ( cr.mHasShadow ) {
					mClipRect.set( cr.mBounds ) ;
					mClipRect.offset(mBounds.left, mBounds.top) ;
					cr.mDrawableRelativeShadow = DropShadowDrawableFactory.setBoundsToFit( cr.mDrawableRelativeShadow, mClipRect ) ;
				}
			}
		}
	}
	
	@Override
	public void setBounds( Rect bounds ) {
		setBounds( bounds.left, bounds.top, bounds.right, bounds.bottom ) ;
	}

	@Override
	public void draw(Canvas canvas) {
		
		// if layer is FLAT, draw the shadows first.
		if ( mLayer == Layer.FLAT ) {
			for ( int i = 0; i < mContentRects.size(); i++ ) {
				ContentRect cr = mContentRects.get(i) ;
				
				// draw the shadow...
				if ( cr.mHasShadow && cr.mVisible ) {
					cr.mDrawableRelativeShadow.draw(canvas) ;
				}
			}
		}
		
		for ( int i = 0; i < mContentRects.size(); i++ ) {
			ContentRect cr = mContentRects.get(i) ;
			if ( !cr.mVisible )
				continue ;
			
			if ( mLayer == Layer.ORDER ) {
				// draw the shadow...
				if ( cr.mHasShadow ) {
					cr.mDrawableRelativeShadow.draw(canvas) ;
				}
			}
			
			mDrawPaint.setColor(cr.mColor) ;
			mDrawPaint.setAlpha( Math.round( Color.alpha( cr.mColor ) * mAlphaMult ) ) ;
			mDrawPaint.setColorFilter(mColorFilter) ;
			
			mDrawRect.set( cr.mBounds ) ;
			mDrawRect.offset( mBounds.left, mBounds.top ) ;
			
			// custom region behavior based on type
			switch( cr.mType ) {
			case SOLID:
				canvas.drawRect(mDrawRect, mDrawPaint) ;
				break ;
				
			case EMPTY:
				mClipRect.set( mDrawRect ) ;
				mClipRect.inset( cr.mBorderWidth, cr.mBorderWidth ) ;

                mDrawPath.reset() ;
				if ( cr.mBorderWidth > 0 ) {
					// border in
					clipBorderUnion( mDrawPath, mDrawRect, cr.mBorderWidth ) ;
				}
				// draw it.
				canvas.save();
				canvas.clipPath(mDrawPath) ;
				canvas.drawPaint(mDrawPaint) ;
				canvas.restore() ;
				break ;
			}
		}
	}
	
	private void clipBorderUnion( Path path, Rect bound, int borderWidth ) {
		path.addRect(bound.left, bound.top, bound.right + borderWidth, bound.bottom, Path.Direction.CW) ;
		path.addRect(bound.left, bound.top, bound.right, bound.top + borderWidth, Path.Direction.CW) ;
		path.addRect(bound.right - borderWidth, bound.top, bound.right, bound.bottom, Path.Direction.CW) ;
		path.addRect(bound.left, bound.bottom - borderWidth, bound.right, bound.bottom, Path.Direction.CW) ;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT ;
	}

	@Override
	public void setAlpha(int alpha) {
		mAlphaMult = ((float)alpha)/255.0f ;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mColorFilter = cf ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
}
