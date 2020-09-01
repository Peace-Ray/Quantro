package com.peaceray.quantro.view.drawable;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.TypedValue;

/**
 * A ColorTabsDrawable functions as a preview of players votes
 * in a Lobby.
 * 
 * With the large number of game modes now available in multiplayer,
 * it's easy to scroll the list the wrong way and hide other player's
 * votes.  We therefore draw little "colored tabs" on the edge of the
 * screen to indicate that votes have been hidden; they are colored by
 * the player color.
 * 
 * However, we don't really care about that.  This class doesn't realize
 * it's displaying player votes or whatever, it's just showing colored
 * tabs.
 * 
 * This version (9/18/2013) always displays tabs on the right edge,
 * which can be configured only by height and width.  Tabs themselves
 * are drawn extending to the right beyond the edge (clipped) in order
 * to properly display drop shadows.
 * 
 * @author Jake
 *
 */
public class ColorTabsDrawable extends RectsDrawable {
	
	private static final float DEFAULT_TAB_HEIGHT_DP = 12 ;
	private static final float DEFAULT_TAB_WIDTH_DP = 4 ;
	
	private static final float DEFAULT_TAB_EXTENSION_DP = 40 ;
	
	private static final float DEFAULT_TAB_VERTICAL_MARGIN_DP = 6 ;
	private static final float DEFAULT_TAB_VERTICAL_SPACING_DP = 2 ;
	
	
	private WeakReference<Context> mwrContext ;
	
	private int mTabHeight ;
	private int mTabWidth ;
	private int mTabExtension ;
	
	private int mTabVerticalMargin ;
	private int mTabVerticalSpacing ;
	
	private int [] mTabColor ;
	private boolean [] mTabTop ;
	private boolean [] mTabBottom ;
	
	private Object [] mTabTopTag ;
	private Object [] mTabBottomTag ;
	
	private boolean mNeedsReset ;
	private boolean mNeedsReposition ;

	public ColorTabsDrawable( Context context ) {
		super(context) ;
		this.setLayer(Layer.FLAT) ;
		
		Resources res = context.getResources() ;
		mTabHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				DEFAULT_TAB_HEIGHT_DP, res.getDisplayMetrics()) ;
		mTabWidth = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				DEFAULT_TAB_WIDTH_DP, res.getDisplayMetrics()) ;
		mTabExtension = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				DEFAULT_TAB_EXTENSION_DP, res.getDisplayMetrics()) ;
		
		mTabVerticalMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				DEFAULT_TAB_VERTICAL_MARGIN_DP, res.getDisplayMetrics()) ;
		mTabVerticalSpacing = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				DEFAULT_TAB_VERTICAL_SPACING_DP, res.getDisplayMetrics()) ;
		
		
		mwrContext = new WeakReference<Context>(context) ;
		
		mTabColor = new int[0] ;
		mTabTop = new boolean[0] ;
		mTabBottom = new boolean[0] ;
		
		generateTags() ;
		
		mNeedsReset = true ;
	}
	
	
	public ColorTabsDrawable setNumberOfTabs( int num ) {
		mTabColor = new int[num] ;
		mTabTop = new boolean[num] ;
		mTabBottom = new boolean[num] ;
		
		generateTags() ;
		
		mNeedsReset = true ;
		return this ;
	}
	
	public int getNumberOfTabs() {
		return mTabColor.length ;
	}
	
	public ColorTabsDrawable setTabColors( int [] colors ) {
		mTabColor = colors.clone() ;
		mTabTop = new boolean[colors.length] ;
		mTabBottom = new boolean[colors.length] ;
		
		generateTags() ;
		
		mNeedsReset = true ;
		return this ;
	}
	
	private void generateTags() {
		mTabTopTag = new Object[mTabColor.length] ;
		mTabBottomTag = new Object[mTabColor.length] ;
		for ( int i = 0; i < mTabColor.length; i++ ) {
			mTabTopTag[i] = "Top_" + i ;
			mTabBottomTag[i] = "Bottom_" + i ;
		}
	}
	
	public ColorTabsDrawable setTabColor( int tabNum, int color ) {
		if ( color != mTabColor[tabNum] ) {
			mTabColor[tabNum] = color ;
			mNeedsReset = true ;
		}
		return this ;
	}
	
	public ColorTabsDrawable setTabOnTop( int tabNum, boolean visible ) {
		if ( mTabTop[tabNum] != visible ) {
			mTabTop[tabNum] = visible ;
			mNeedsReposition = true ;
		}
		return this ;
	}
	
	public ColorTabsDrawable setTabOnBottom( int tabNum, boolean visible ) {
		if ( mTabBottom[tabNum] != visible ) {
			mTabBottom[tabNum] = visible ;
			mNeedsReposition = true ;
		}
		return this ;
	}
	
	@Override
	public void setBounds( int left, int top, int right, int bottom ) {
		Rect bounds = getBounds() ;
		if ( bounds.top != top || bounds.left != left || bounds.right != right || bounds.bottom != bottom )
			mNeedsReposition = true ;
		super.setBounds(left, top, right, bottom) ;
	}
	
	@Override
	public void setBounds( Rect bounds ) {
		if ( !bounds.equals(getBounds()) )
			mNeedsReposition = true ;
		super.setBounds(bounds) ;
	}
	
	@Override
	public void draw( Canvas canvas ) {
		if ( mNeedsReset || mNeedsReposition ) {
			set() ;
		}
		canvas.save() ;
		canvas.clipRect(this.getBounds()) ;
		super.draw(canvas) ;
		canvas.restore() ;
	}
	
	/**
	 * Changes don't take place immediately.  Calling 'set'
	 * performs the change, placing the correct tabs in the right places.
	 */
	public void set() {
		// two steps.  First, set: clear the existing rectangles
		// and make new ones using our current colors.
		// Second: position the rectangles according to the number which
		// are visible.
		if ( mNeedsReset ) {
			Context context = mwrContext.get() ;
			this.clearRects() ;
			
			int width = mTabWidth + mTabExtension ;
			int height = mTabHeight ;
			for ( int i = 0; i < mTabColor.length; i++ ) {
				// top rect...
				int index = this.addRect(RectsDrawable.Type.SOLID,
						mTabColor[i], 0, 0, 0, width, height, mTabTopTag[i]) ;
				setRectHasShadow(context, index, true) ;
				
				// bottom rect...
				index = this.addRect(RectsDrawable.Type.SOLID,
						mTabColor[i], 0, 0, 0, width, height, mTabBottomTag[i]) ;
				setRectHasShadow(context, index, true) ;
			}
			
			mNeedsReset = false ;
			mNeedsReposition = true ;
		}
		
		if ( mNeedsReposition ) {
			// set positions and visibility
			Rect bounds = getBounds() ;
			
			int y_top = bounds.top + mTabVerticalMargin ;
			int y_bottom = bounds.bottom - mTabVerticalMargin - mTabHeight ;
			
			int x_left = bounds.right - mTabWidth ;
			
			for ( int i = 0; i < mTabColor.length; i++ ) {
				// top first...
				int index = this.getIndex(mTabTopTag[i]) ;
				this.setRectVisible(index, mTabTop[i]) ;
				if ( mTabTop[i] ) {
					this.setRectPosition(index, x_left, y_top) ;
					y_top += mTabVerticalSpacing + mTabHeight ;
				}
				
				// now bottom...
				index = this.getIndex(mTabBottomTag[i]) ;
				this.setRectVisible(index, mTabBottom[i]) ;
				if ( mTabBottom[i] ) {
					this.setRectPosition(index, x_left, y_bottom) ;
					y_bottom -= mTabVerticalSpacing + mTabHeight ;
				}
			}
			
			mNeedsReposition = false ;
		}
	}
}
