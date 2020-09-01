package com.peaceray.quantro.view.drawable;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * A 'Drawable' wrapper; wraps an existing Drawable instance in its own
 * container drawable, which has the purpose of applying a rectangular drop
 * shadow around the boundaries of this Drawable.
 * 
 * Has the effect of 1: revising calls to 'intrinsicHeight / Width' to
 * account for drop shadow size, and 2: wrapping getBounds to shrink
 * enough to fit the draw shadow in the shape.
 * 
 * @author Jake
 *
 */
public class DropShadowedDrawable extends Drawable {
	
	private Drawable mDropShadow ;
	private boolean mDropShadowOK ;
	private Rect mBounds ;
	private Rect mShadowPadding ;
	
	private Drawable mContent ;
	private Rect mContentBounds ;
	private boolean mContentForceIntrinsicSize ;
	private int mContentForcedIntrinsicWidth, mContentForcedIntrinsicHeight ;
	private boolean mContentForceMinimumSize ;
	private int mContentForcedMinimumWidth, mContentForcedMinimumHeight ;
	
	
	public DropShadowedDrawable( Resources res, Drawable d ) {
		if ( d == null )
			throw new NullPointerException("Must provide non-null drawable") ;
		
		mContent = d ;
		mContentBounds = new Rect( d.getBounds() ) ;
		
		mDropShadow = DropShadowDrawableFactory.getDrawable(res) ;
		mDropShadowOK = false ;
		mBounds = new Rect() ;
		mShadowPadding = DropShadowDrawableFactory.getPadding(res) ;
		
		mContentForceIntrinsicSize = false ;
		mContentForceMinimumSize = false ;
	}
	
	public DropShadowedDrawable( Resources res, Bitmap b ) {
		if ( b == null )
			throw new NullPointerException("Must provide non-null bitmap") ;
		
		mContent = new BitmapDrawable( res, b ) ;
		mContentBounds = new Rect( mContent.getBounds() ) ;
		
		mDropShadow = DropShadowDrawableFactory.getDrawable(res) ;
		mDropShadowOK = false ;
		mBounds = new Rect() ;
		mShadowPadding = DropShadowDrawableFactory.getPadding(res) ;
		
		mContentForceIntrinsicSize = false ;
		mContentForceMinimumSize = false ;
	}
	
	public void setContentDrawable( Drawable d ) {
		if ( d == null )
			throw new NullPointerException("Must provide non-null drawable") ;
		
		mContent = d ;
		mContentBounds = new Rect( d.getBounds() ) ;
		
		mDropShadowOK = false ;
		
		mContentForceIntrinsicSize = false ;
		mContentForceMinimumSize = false ;
	}
	
	public void setContentBitmap( Resources res, Bitmap b ) {
		if ( b == null )
			throw new NullPointerException("Must provide non-null bitmap") ;
		
		mContent = new BitmapDrawable( res, b ) ;
		mContentBounds = new Rect( mContent.getBounds() ) ;
		
		mDropShadowOK = false ;
		
		mContentForceIntrinsicSize = false ;
		mContentForceMinimumSize = false ;
	}
	
	
	
	@Override
	public void setBounds( Rect bounds ) {
		setBounds( bounds.left, bounds.top, bounds.right, bounds.bottom ) ;
	}
	
	public void setForceIntrinsicContentSize( boolean force, int w, int h ) {
		this.mContentForceIntrinsicSize = force ;
		this.mContentForcedIntrinsicWidth = w ;
		this.mContentForcedIntrinsicHeight = w ;
	}
	
	public void setForceMinimumContentSize( boolean force, int w, int h ) {
		this.mContentForceMinimumSize = force ;
		this.mContentForcedMinimumWidth = w ;
		this.mContentForcedMinimumHeight = w ;
	}
	
	
	
	
	@Override
	public void setBounds( int l, int t, int r, int b ) {
		super.setBounds(l, t, r, b) ;
		
		// set our bounds...
		mBounds.set(l, t, r, b) ;
		
		// set and INSET our content bounds.
		mContentBounds.set(mBounds) ;
		mContentBounds.left += mShadowPadding.left ;
		mContentBounds.top += mShadowPadding.top ;
		mContentBounds.right -= mShadowPadding.right ;
		mContentBounds.bottom -= mShadowPadding.bottom ;
		mContent.setBounds(mContentBounds) ;
		
		// resize our drop shadow
		try {
			DropShadowDrawableFactory.setBoundsToFit(mDropShadow, mContentBounds) ;
			mDropShadowOK = true ;
		} catch ( Exception e ) {
			mDropShadowOK = false ;
		}
	}
	
	@Override
	public int getIntrinsicWidth() {
		int contentWidth = this.mContentForceIntrinsicSize
				? mContentForcedIntrinsicWidth : mContent.getIntrinsicWidth() ;
		if ( contentWidth <= 0 )
			return contentWidth ;
		return contentWidth + mShadowPadding.left + mShadowPadding.right ;
	}
	
	@Override
	public int getIntrinsicHeight() {
		int contentHeight = this.mContentForceIntrinsicSize
				? mContentForcedIntrinsicHeight : mContent.getIntrinsicHeight() ;
		if ( contentHeight <= 0 )
			return contentHeight ;
		return contentHeight + mShadowPadding.top + mShadowPadding.bottom ;
	}
	
	@Override
	public int getMinimumWidth() {
		int contentWidth = this.mContentForceMinimumSize
				? mContentForcedMinimumWidth : mContent.getMinimumWidth() ;
		if ( contentWidth <= 0 )
			return contentWidth ;
		return contentWidth + mShadowPadding.left + mShadowPadding.right ;
	}
	
	@Override
	public int getMinimumHeight() {
		int contentHeight = this.mContentForceMinimumSize
				? mContentForcedMinimumHeight : mContent.getMinimumHeight() ;
		if ( contentHeight <= 0 )
			return contentHeight ;
		return contentHeight + mShadowPadding.top + mShadowPadding.bottom ;
	}
	
	

	@Override
	public void draw(Canvas canvas) {
		if ( mDropShadowOK )
			mDropShadow.draw(canvas) ;
		mContent.draw(canvas) ;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT ;
	}

	@Override
	public void setAlpha(int alpha) {
		mDropShadow.setAlpha(alpha) ;
		mContent.setAlpha(alpha) ;
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		mDropShadow.setColorFilter(cf) ;
		mContent.setColorFilter(cf) ;
	}

}
