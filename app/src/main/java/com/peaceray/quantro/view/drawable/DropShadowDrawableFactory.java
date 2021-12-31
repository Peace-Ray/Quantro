package com.peaceray.quantro.view.drawable;

import com.peaceray.quantro.R;
import com.peaceray.quantro.consts.GlobalTestSettings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Drop shadows, applied to rectangular menu objects, are a bit more complex
 * than you might think.  Specifically, the shade and shape of a drop shadow's
 * corner may be very different depending on the size of the drawn object, especially
 * at small scales:
 * while the corner of a 40x40 and 60x60 square may look identical, and a single
 * 9-patch can suffice to represent both, the same is not true of 4x4 and 6x6.
 * 
 * Enter DropShadowDrawableFactory.  This factory is a tool for loading Drawable
 * (9patch) resources representing drop shadows.  The primary method,
 * getDrawable( ... ), functions as a wrapper to res.getDrawable() and thus
 * the Drawable object returned may not be a unique instance.  If a unique instance
 * is required, call .getConstantState().newDrawable() on the returned Drawable.
 * 
 * The Drawable returned will be an idea, or best-available, Drawable to represent
 * a drop shadow for a rectangle of the specified shape.  The Drawable should be drawn
 * such that the shadowing rectangle is fully and exactly within the Drawable's padding.
 * 
 * In other words, if the rectangle is WxH at position (X,Y), and the returned
 * Drawable has padding L-T-R-B, then it will be returned with
 * bounds (X - L, Y - T, X + W + R, Y + H + B).
 * 
 * When the required height changes, it is not recommended that you alter the Drawable
 * again.  Instead, another getDrawable call is appropriate.  For the most general-purpose
 * drop shadow drawable available, call without size parameters.
 * 
 * @author Jake
 *
 */
public class DropShadowDrawableFactory {
	
	public static final String TAG = "DSDrawableFactory" ;
	
	public static final int RESIZEABLE_WIDTH = 20 ;
	public static final int RESIZEABLE_HEIGHT = 20 ;
	
	
	/**
	 * Returns, in a new Rectangle, the padding required to display
	 * a drop shadow.  This is an "optimistic" estimate that covers
	 * all drop shadow scales.  If you are sizing yourself to
	 * fit drop shadows, make sure you have this amount of space available
	 * around every side.
	 * 
	 * @param c
	 * @return
	 */
	public static final Rect getPadding( Context c ) {
		Rect r = new Rect() ;
		return copyPadding( c.getResources(), r ) ? r : null ;
	}
	
	/**
	 * Returns, in a new Rectangle, the padding required to display
	 * a drop shadow.  This is an "optimistic" estimate that covers
	 * all drop shadow scales.  If you are sizing yourself to
	 * fit drop shadows, make sure you have this amount of space available
	 * around every side.
	 * 
	 * @param res
	 * @return
	 */
	public static final Rect getPadding( Resources res ) {
		Rect r = new Rect() ;
		return copyPadding( res, r ) ? r : null ;
	}
	
	/**
	 * Copies into the provided Rect the padding required to display
	 * a drop shadow.  This is an "optimistic" estimate that covers
	 * all drop shadow scales.  If you are sizing yourself to
	 * fit drop shadows, make sure you have this amount of space available
	 * around every side.
	 * @param c
	 * @param r
	 */
	public static final boolean copyPadding( Context c, Rect r ) {
		return copyPadding( c.getResources(), r ) ;
	}
	
	/**
	 * Copies into the provided Rect the padding required to display
	 * a drop shadow.  This is an "optimistic" estimate that covers
	 * all drop shadow scales.  If you are sizing yourself to
	 * fit drop shadows, make sure you have this amount of space available
	 * around every side.
	 * @param res
	 * @param r
	 */
	public static final boolean copyPadding( Resources res, Rect r ) {
		Drawable d = res.getDrawable(R.drawable.rect_drop_shadow) ;
		// this padding is universal, all-drawable padding.
		return d.getPadding(r) ;
	}
	
	
	public static final boolean test( Activity a ) {
		Resources r = a.getResources() ;
		DisplayMetrics metrics = new DisplayMetrics() ;
		a.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		switch(metrics.densityDpi){
			case DisplayMetrics.DENSITY_LOW:
				Log.d(TAG, "Test: with LOW density") ;
				break ;
			case DisplayMetrics.DENSITY_MEDIUM:
				Log.d(TAG, "Test: with MEDIUM density") ;
				break ;
			case DisplayMetrics.DENSITY_HIGH:
				Log.d(TAG, "Test: with HIGH density") ;
				break ;
			default:
				Log.d(TAG, "Test: with other density (probably XHIGH)") ;
				break ;
		}
		
		boolean ok = true ;
		
		// test squares
		for ( int s = 1; s < 30; s++ ) {
			try {
				Drawable d = getDrawable( r, 0, 0, s, s ) ;
				if ( d == null ) {
					Log.d(TAG, "Test: failed to load square with sides " + s) ;
					ok = false ;
				}
			} catch ( Exception e ) {
				Log.d(TAG, "Test: failed to load square with sides "+ s + " msg: " + e.toString() ) ;
				ok = false ;
			}
		}
		
		// test horiz. stretch
		for ( int h = 1; h < 30; h++ ) {
			try {
				Drawable d = getDrawable( r, 0, 0, 30, h ) ;
				if ( d == null ) {
					Log.d(TAG, "Test: failed to load horiz-rect with sides " + 30 + ", " + h) ;
					ok = false ;
				}
			} catch ( Exception e ) {
				Log.d(TAG, "Test: failed to load horiz-rect with sides " + 30 + ", " + h + " msg: " + e.toString() ) ;
				ok = false ;
			}
		}
		
		// test horiz. fixed
		for ( int h = 1; h < 15; h++ ) {
			for ( int wmult = 1; wmult <= 5; wmult++ ) {
				int w = h * wmult ;
				try {
					Drawable d = getDrawable( r, 0, 0, w, h ) ;
					if ( d == null ) {
						Log.d(TAG, "Test: failed to load horiz-rect with sides " + w + ", " + h) ;
						ok = false ;
					}
				} catch ( Exception e ) {
					Log.d(TAG, "Test: failed to load horiz-rect with sides " + w + ", " + h + " msg: " + e.toString() ) ;
					ok = false ;
				}
			}
		}
		
		return ok ;
	}
	
	
	/**
	 * Copies this DropShadowDrawable (including padding, bounds, etc.) into 
	 * a new instance
	 * @param drawable
	 * @return
	 */
	public static final Drawable newInstance( Drawable drawable ) {
		Drawable d = drawable.getConstantState().newDrawable() ;
		d.setBounds(drawable.getBounds()) ;
		return d ;
	}
	

	/**
	 * Returns the most general-purpose drop-shadow drawable available.
	 * 
	 * The returned Drawable has not been sized to any particular bounds,
	 * and may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * The padding of the returned Drawable indicates the area within which
	 * "shadowing" content should be placed.  Shadowing content should be fully
	 * opaque within this bounds.
	 * 
	 * @param c
	 * @return
	 */
	public static final Drawable getDrawable( Context c ) {
		return getDrawable( c.getResources() ) ;
	}
	
	/**
	 * Returns the most general-purpose drop-shadow drawable available.
	 * 
	 * The returned Drawable has not been sized to any particular bounds,
	 * and may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * The padding of the returned Drawable indicates the area within which
	 * "shadowing" content should be placed.  Shadowing content should be fully
	 * opaque within this bounds.
	 * 
	 * @param c
	 * @return
	 */
	public static final Drawable getDrawable( Resources r ) {
		return r.getDrawable(R.drawable.rect_drop_shadow) ;
	}
	
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Context c, Rect bounds ) {
		return getDrawable( null, c.getResources(), bounds.left, bounds.top, bounds.right, bounds.bottom ) ;
	}
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Resources r, Rect bounds ) {
		return getDrawable( null, r, bounds.left, bounds.top, bounds.right, bounds.bottom ) ;
	}
	
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Context c, int left, int top, int right, int bottom ) {
		return getDrawable( null, c.getResources(), left, top, right, bottom ) ;
	}
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Resources r, int left, int top, int right, int bottom ) {
		return getDrawable( null, r, left, top, right, bottom ) ;
	}
	
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Drawable d, Context c, Rect bounds ) {
		return getDrawable( d, c.getResources(), bounds.left, bounds.top, bounds.right, bounds.bottom ) ;
	}
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Drawable d, Resources r, Rect bounds ) {
		return getDrawable( d, r, bounds.left, bounds.top, bounds.right, bounds.bottom ) ;
	}
	
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Drawable d, Context c, int left, int top, int right, int bottom ) {
		return getDrawable( d, c.getResources(), left, top, right, bottom ) ;
	}
	
	/**
	 * Returns the "best-fit" Drawable to represent the drop shadow of the specified
	 * rectangle.
	 * 
	 * The returned Drawable is set to shade the specified area.  Its padding and bounds are
	 * such that the specified rectangle is exactly within its padding.
	 * 
	 * This drawable may not be a unique instance.  If your application requires a
	 * unique instance, call newInstance( val ) with the returned result.
	 * 
	 * @param c
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable getDrawable( Drawable d, Resources r, int left, int top, int right, int bottom ) {
		
		int w = right - left ;
		int h = bottom - top ;
		
		if ( w < 0 || h < 0 )
			throw new IllegalArgumentException("Cannot shade a rectangle with negative area.") ;
		
		if ( w == 0 || h == 0 )
			throw new IllegalArgumentException("Cannot shade an empty area: " + left + ", " + top + " - " + right + ", " + bottom) ;
			
		// If d is null or cannot be resized, get a new one.
		boolean newDrawable = d == null ;
		if ( !newDrawable && !DropShadowDrawableFactory.isFit(d, w, h) ) {
			newDrawable = w < DropShadowDrawableFactory.RESIZEABLE_WIDTH
					|| h < DropShadowDrawableFactory.RESIZEABLE_HEIGHT
					|| !DropShadowDrawableFactory.isResizeable(d) ;
		}
		
		boolean newFit = !newDrawable && !DropShadowDrawableFactory.isFit(d, left, top, right, bottom) ;

		if ( !newDrawable && !newFit ) {
			return d ;
		}
		else if ( newFit && canFit( d, w, h ) ) {
			return setBoundsToFit( d, left, top, right, bottom ) ;
		}
		else {
			if ( w < h )
				return getDrawableRectangleVert( r, left, top, right, bottom, w, h ) ;
			
			if ( w > h )
				return getDrawableRectangleHoriz( r, left, top, right, bottom, w, h ) ;
			
			return getDrawableSquare( r, left, top, right, bottom, w ) ;
		}
	}

	
	private static final Drawable getDrawableRectangleVert( Resources res, int left, int top, int right, int bottom, int width, int height ) {
		// The "general purpose" resizable rectangle requires that we have a width of at least 16.
		// We know the height is >= width, so we don't need to check it.
		if ( width >= 16 )
			return setBoundsToFit( res.getDrawable(R.drawable.rect_drop_shadow), left, top, right, bottom ) ;
			
		throw new IllegalArgumentException("No resource available to cover those dimensions: too small a vertically-stretched rectangle" + (right-left) + " by " + (bottom - top)) ;
		
	}
	
	
	private static final Drawable getDrawableRectangleHoriz( Resources res, int left, int top, int right, int bottom, int width, int height ) {
		// The "general purpose" resizable rectangle requires that we have at least a height of 16.
		// We know the width is >= height, so we don't need to check it.
		if ( height >= 16 )
			return setBoundsToFit( res.getDrawable(R.drawable.rect_drop_shadow), left, top, right, bottom ) ;
		
		int rID = -1 ;
		Drawable d = null ;
		
		switch( height ) {
		case 1:
			// try for an exact match.  If none, use the rescalable version.
			switch( width ) {
			case 2:
				rID = R.drawable.rect_drop_shadow_2_1 ;
				break ;
			case 3:
				rID = R.drawable.rect_drop_shadow_3_1 ;
				break ;
			case 4:
				rID = R.drawable.rect_drop_shadow_4_1 ;
				break ;
			case 5:
				rID = R.drawable.rect_drop_shadow_5_1 ;
				break ;
			default:
				rID = R.drawable.rect_drop_shadow_horiz_1 ;
				break ;
			}
			break ;
		case 2:
			// try for an exact match.  If none, use the rescalable version.
			switch( width ) {
			case 4:
				rID = R.drawable.rect_drop_shadow_4_2 ;
				break ;
			case 6:
				rID = R.drawable.rect_drop_shadow_6_2 ;
				break ;
			case 8:
				rID = R.drawable.rect_drop_shadow_8_2 ;
				break ;
			case 10:
				rID = R.drawable.rect_drop_shadow_10_2 ;
				break ;
			default:
				rID = R.drawable.rect_drop_shadow_horiz_2 ;
				break ;
			}
			break ;
		case 3:
			// try for an exact match.  If none, use the rescalable version.
			switch( width ) {
			case 6:
				rID = R.drawable.rect_drop_shadow_6_3 ;
				break ;
			case 9:
				rID = R.drawable.rect_drop_shadow_9_3 ;
				break ;
			case 12:
				rID = R.drawable.rect_drop_shadow_12_3 ;
				break ;
			case 15:
				rID = R.drawable.rect_drop_shadow_15_3 ;
				break ;
			default:
				rID = R.drawable.rect_drop_shadow_horiz_3 ;
				break ;
			}
			break ;
		case 4:
			// try for an exact match.  If none, use the rescalable version.
			switch( width ) {
			case 8:
				rID = R.drawable.rect_drop_shadow_8_4 ;
				break ;
			case 12:
				rID = R.drawable.rect_drop_shadow_12_4 ;
				break ;
			default:
				rID = R.drawable.rect_drop_shadow_horiz_4 ;
				break ;
			}
			break ;
		case 5:
			// try for an exact match.  If none, use the rescalable version.
			switch( width ) {
			case 10:
				rID = R.drawable.rect_drop_shadow_10_5 ;
				break ;
			case 15:
				rID = R.drawable.rect_drop_shadow_15_5 ;
				break ;
			default:
				rID = R.drawable.rect_drop_shadow_horiz_5 ;
				break ;
			}
			break ;
		case 6:
			if ( width == 12 )
				rID = R.drawable.rect_drop_shadow_12_6 ;
			else
				rID = R.drawable.rect_drop_shadow_horiz_6 ;
			break ;
		case 7:
			if ( width == 14 )
				rID = R.drawable.rect_drop_shadow_14_7 ;
			else
				rID = R.drawable.rect_drop_shadow_horiz_7 ;
			break ;
		case 8:
			rID = R.drawable.rect_drop_shadow_horiz_8 ;
			break ;
		case 9:
			rID = R.drawable.rect_drop_shadow_horiz_9 ;
			break ;
		case 10:
			rID = R.drawable.rect_drop_shadow_horiz_10 ;
			break ;
		case 11:
			rID = R.drawable.rect_drop_shadow_horiz_11 ;
			break ;
		case 12:
			rID = R.drawable.rect_drop_shadow_horiz_12 ;
			break ;
		case 13:
			rID = R.drawable.rect_drop_shadow_horiz_13 ;
			break ;
		case 14:
			rID = R.drawable.rect_drop_shadow_horiz_14 ;
			break ;
		case 15:
			rID = R.drawable.rect_drop_shadow_horiz_15 ;
			break ;
		default:
			throw new IllegalArgumentException("No resource for width-stretched rectangles of height of length " + height) ;
		}

		d = res.getDrawable(rID) ;
		
		// it's possible that the rectangle isn't wide enough to use this drawable...
		if ( !canFit( d, width, height ) && GlobalTestSettings.DROP_SHADOW_STRICT_TESTS )
			throw new IllegalArgumentException("No resource available to cover these dimensions: cannot stretch a horiz. drop shadow with width " + width + ", height " + height) ;
		
		// this should have no effect on the scale; it should only translate.
		return setBoundsToFit( d, left, top, right, bottom ) ;
	}
	
	
	private static final Drawable getDrawableSquare( Resources res, int left, int top, int right, int bottom, int length ) {
		// The "general purpose" resizable rectangle requires that we have at least a length of 16.
		// We know the width is >= height, so we don't need to check it.
		if ( length >= 16 )
			return setBoundsToFit( res.getDrawable(R.drawable.rect_drop_shadow), left, top, right, bottom ) ;
		
		Drawable d = null ;
		switch( length ) {
		case 1:
			d = res.getDrawable(R.drawable.rect_drop_shadow_1) ;
			break ;
		case 2:
			d = res.getDrawable(R.drawable.rect_drop_shadow_2) ;
			break ;
		case 3:
			d = res.getDrawable(R.drawable.rect_drop_shadow_3) ;
			break ;
		case 4:
			d = res.getDrawable(R.drawable.rect_drop_shadow_4) ;
			break ;
		case 5:
			d = res.getDrawable(R.drawable.rect_drop_shadow_5) ;
			break ;
		case 6:
			d = res.getDrawable(R.drawable.rect_drop_shadow_6) ;
			break ;
		case 7:
			d = res.getDrawable(R.drawable.rect_drop_shadow_7) ;
			break ;
		case 8:
			d = res.getDrawable(R.drawable.rect_drop_shadow_8) ;
			break ;
		case 9:
			d = res.getDrawable(R.drawable.rect_drop_shadow_9) ;
			break ;
		case 10:
			d = res.getDrawable(R.drawable.rect_drop_shadow_10) ;
			break ;
		case 11:
			d = res.getDrawable(R.drawable.rect_drop_shadow_11) ;
			break ;
		case 12:
			d = res.getDrawable(R.drawable.rect_drop_shadow_12) ;
			break ;
		case 13:
			d = res.getDrawable(R.drawable.rect_drop_shadow_13) ;
			break ;
		case 14:
			d = res.getDrawable(R.drawable.rect_drop_shadow_14) ;
			break ;
		case 15:
			d = res.getDrawable(R.drawable.rect_drop_shadow_15) ;
			break ;
		default:
			throw new IllegalArgumentException("No resource for squares of length " + length) ;
		}
		
		// squares are exact; they can fit.
		
		// this should have no effect on the scale; it should only translate.
		return setBoundsToFit( d, left, top, right, bottom ) ;
	}
	
	
	
	/**
	 * Can this drop shadow Drawable be made to fit around a rect of the provided
	 * dimensions?
	 * 
	 * @param d
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final boolean canFit( Drawable d, int width, int height ) {
		synchronized( sizeToFit_rect ) {
			if ( !d.getPadding(sizeToFit_rect) )
				throw new IllegalArgumentException("Provided drawable " + d + " does not have padding; can't fit " + width + ", " + height + "!") ;
			
			int paddingWidth = sizeToFit_rect.left + sizeToFit_rect.right ;
			int paddingHeight = sizeToFit_rect.top + sizeToFit_rect.bottom ;
			
			//Log.d(TAG, "shadow minWidth " + d.getMinimumWidth() + " intrinsic width " + d.getIntrinsicWidth() + " required width " + width + " padding width " + paddingWidth) ;
			//Log.d(TAG, "shadow minHeight " + d.getMinimumHeight() + " intrinsic height " + d.getIntrinsicHeight() + " required height " + height + " padding height " + paddingHeight) ;
			
			// A drop shadow "fits" the rectangle by setting the rect to be exactly
			// within its bounds.  To fit, the minimum size of this drop shadow
			// must be <= width + horizPadding, <= height + vertPadding.
			if ( d.getMinimumWidth() > width + paddingWidth )
				return false ;
			
			if ( d.getMinimumHeight() > height + paddingHeight )
				return false ;
			
			return true ;
		}
	}
	
	
	public static final boolean isFit( Drawable d, Rect r ) {
		return isFit( d, r.left, r.top, r.right, r.bottom ) ;
	}
	
	
	public static final boolean isFit( Drawable d, int left, int top, int right, int bottom ) {
		synchronized( sizeToFit_rect ) {
			Rect r = d.getBounds() ;
			if ( !d.getPadding(sizeToFit_rect) )
				throw new IllegalArgumentException("Provided drawable does not have padding!") ;
				
			return r.left == left - sizeToFit_rect.left
					&& r.top == top - sizeToFit_rect.top
					&& r.right == right + sizeToFit_rect.right
					&& r.bottom == bottom + sizeToFit_rect.bottom ;
		}
	}
	
	
	public static final boolean isFit( Drawable d, int width, int height ) {
		synchronized( sizeToFit_rect ) {
			Rect r = d.getBounds() ;
			if ( !d.getPadding(sizeToFit_rect) )
				throw new IllegalArgumentException("Provided drawable does not have padding!") ;
			
			int paddingWidth = sizeToFit_rect.left + sizeToFit_rect.right ;
			int paddingHeight = sizeToFit_rect.top + sizeToFit_rect.bottom ;
			
			return width == r.width() - paddingWidth
					&& height == r.height() - paddingHeight ;
		}
	}
	
	
	
	public static final boolean isResizeable( Drawable d ) {
		synchronized( sizeToFit_rect ) {
			Rect r = d.getBounds() ;
			if ( !d.getPadding(sizeToFit_rect) )
				throw new IllegalArgumentException("Provided drawable does not have padding!") ;
			
			int paddingWidth = sizeToFit_rect.left + sizeToFit_rect.right ;
			int paddingHeight = sizeToFit_rect.top + sizeToFit_rect.bottom ;
				
			return RESIZEABLE_WIDTH <= r.width() - paddingWidth
					&& RESIZEABLE_HEIGHT <= r.height() - paddingHeight ;
		}
	}
	
	
	
	/**
	 * Resizes the provided drop-shadow drawable to represent a shadow of the
	 * specified opaque rectangle.
	 * 
	 * Returns the same drawable instance, for convenience or chaining.
	 * 
	 * @param d
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable setBoundsToFit( Drawable d, Rect r ) {
		return setBoundsToFit( d, r.left, r.top, r.right, r.bottom ) ;
	}
	
	/**
	 * Resizes the provided drop-shadow drawable to represent a shadow of the
	 * specified opaque rectangle.
	 * 
	 * Returns the same drawable instance, for convenience or chaining.
	 * 
	 * @param d
	 * @param left
	 * @param top
	 * @param right
	 * @param bottom
	 * @return
	 */
	public static final Drawable setBoundsToFit( Drawable d, int left, int top, int right, int bottom ) {
		synchronized( sizeToFit_rect ) {
			if ( !d.getPadding(sizeToFit_rect) )
				throw new IllegalArgumentException("Provided drawable does not have padding!") ;
			
			d.setBounds(
					left - sizeToFit_rect.left,
					top - sizeToFit_rect.top,
					right + sizeToFit_rect.right,
					bottom + sizeToFit_rect.bottom) ;
			
			return d ;
		}
	}
	
	private static final Rect sizeToFit_rect = new Rect() ;
}
