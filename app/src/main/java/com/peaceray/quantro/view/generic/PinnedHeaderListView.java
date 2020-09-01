/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * See https://code.google.com/archive/p/android-playground/
 *
 * Changes by Jake Rosin:
 *
 * - Different package
 * - Support fading headers (transition header alpha to 0 based on scroll position). Changes
 *      to PinnedHeaderAdapter interface and `configureHeaderView`
 * - Add getters to PinnedHeaderAdapter to retrieve regions fully occluded or partially filled
 *      by header views (to coordinate fading and header content pinning).
 * - Modifications to pinning position based on actual header content sizes (not view dimensions).
 * - Add PinnedHeaderAdapterDefaults, an abstract implementation of PinnedHeaderAdapter providing
 *      default behavior for the new functions.
 * - Changes to configureHeaderView to support new push-up and fading behaviors.
 * - Changes to dispatchDraw to ensure partially faded headers still occlude list content.
 */

package com.peaceray.quantro.view.generic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * A ListView that maintains a header pinned at the top of the list. The
 * pinned header can be pushed up and dissolved as needed.
 */
public class PinnedHeaderListView extends ListView {

    /**
     * Adapter interface.  The list adapter must implement this interface.
     */
    public interface PinnedHeaderAdapter {

        /**
         * Pinned header state: don't show the header.
         */
        public static final int PINNED_HEADER_STATE_GONE = 0;

        /**
         * Pinned header state: show the header at the top of the list.
         */
        public static final int PINNED_HEADER_STATE_VISIBLE = 1;

        /**
         * Pinned header state: show the header. If the header extends beyond
         * the bottom of the first shown element, push it up and clip.
         */
        public static final int PINNED_HEADER_STATE_PUSHED_UP = 2;

        /**
         * Computes the desired state of the pinned header for the given
         * position of the first visible list item. Allowed return values are
         * {@link #PINNED_HEADER_STATE_GONE}, {@link #PINNED_HEADER_STATE_VISIBLE} or
         * {@link #PINNED_HEADER_STATE_PUSHED_UP}.
         */
        int getPinnedHeaderState(int position);
        
        /**
         * Fade alpha setting: keep headers at 255 alpha always.
         */
        public static final int PINNED_HEADER_FADE_ALPHA_NO = 0 ;
        
        /**
         * Fade alpha setting: fade based on the % of "contentClip" which is visible.
         * If 1/2 of the content clip is pushed off the screen, set alpha at 128.
         */
        public static final int PINNED_HEADER_FADE_ALPHA_CLIP_VISIBLE_PORTION = 1 ;

        /**
         * Fade alpha setting: fade based on the overlap between the "non-content"
         * areas of header views.  When the two views touch, alpha is 255.  When the
         * "content bounds" are in contact, alpha is 0.  Alpha ranges between as the
         * content bounds near each other.
         */
        public static final int PINNED_HEADER_FADE_ALPHA_HEADER_NONCONTENT_OVERLAP = 2 ;
        
        
        /**
         * Returns the fade alpha style used for this list.
         * @return
         */
        int pha_getPinnedHeaderFadeAlphaStyle();
        
        
        /**
         * Copies the "occluding bounds" of the provided header view into 'r', returning
         * whether this operation was successful.  Bounds are relative to the top-left
         * of the view provided.
         * 
         * 'Occluding bounds' is a Rect, <= the view bounds themselves, indicating the
         * opaque content of the provided header.  e.g. if the header has a drop
         * shadow over list items, the content clip does NOT include that shadow.
         * 
         * However, the occluding bounds could possibly content transparent pixels
         * (for example, if you want the background to show through).  These bounds
         * are used to clip list content, so list items disappear as they slide under
         * the bounds.
         * 
         * @param header
         * @param position
         * @param r
         * @return
         */
        boolean copyPinnedHeaderOccludingBounds(View header, int position, Rect r) ;
        
        /**
         * Copies the "visible bounds" of the provided header view into 'r', returning
         * whether this operation was successful.  Bounds are relative to the top-left
         * of the view provided.
         * 
         * 'Visible bounds' is a Rect, <= the view bounds themselves, indicating the
         * visible content of the provided header.  e.g. if the header has a drop
         * shadow over list items, the visible bounds DO include that drop shadow.
         * 
         * However, it does not include "padding" around the header, if said padding
         * is transparent.
         * 
         * @param header
         * @param position
         * @param r
         * @return
         */
        boolean copyPinnedHeaderVisibleBounds(View view, int position, Rect r) ;
        
        
        /**
         * Copies the "occluding bounds" of the header of the provided  view into 'r', returning
         * whether this operation was successful.  Bounds are relative to the top-left
         * of the view provided.
         * 
         * NOTE: this method differs from copyPinnedHeader in that the view passed in is
         * 		a list item, not a pinned header.
         * 
         * 'Occluding bounds' is a Rect, <= the view bounds themselves, indicating the
         * opaque content of the provided header.  e.g. if the header has a drop
         * shadow over list items, the content clip does NOT include that shadow.
         * 
         * However, the occluding bounds could possibly content transparent pixels
         * (for example, if you want the background to show through).  These bounds
         * are used to clip list content, so list items disappear as they slide under
         * the bounds.
         * 
         * @param header
         * @param position
         * @param r
         * @return
         */
        boolean copyHeaderOccludingBounds(View view, int position, Rect r) ;
        
        /**
         * Copies the "occluding bounds" of the header of the provided  view into 'r', returning
         * whether this operation was successful.  Bounds are relative to the top-left
         * of the view provided.
         * 
         * NOTE: this method differs from copyPinnedHeader in that the view passed in is
         * 		a list item, not a pinned header.
         * 
         * 'Visible bounds' is a Rect, <= the view bounds themselves, indicating the
         * visible content of the provided header.  e.g. if the header has a drop
         * shadow over list items, the visible bounds DO include that drop shadow.
         * 
         * However, it does not include "padding" around the header, if said padding
         * is transparent.
         * 
         * @param header
         * @param position
         * @param r
         * @return
         */
        boolean copyHeaderVisibleBounds(View header, int position, Rect r) ;
        
        
        /**
         * Configures the pinned header view to match the first visible list item.
         *
         * @param header pinned header view.
         * @param position position of the first visible list item.
         * @param alpha fading of the header view, between 0 and 255.
         */
        void configurePinnedHeader(View header, int position, int alpha);
        
        /**
         * Returns the next position after the specified one that marks a new
         * header.  If none, returns -1.
         * 
         * @param position
         * @return -1 indicating no more headers, or a value > position indicating
         * 			the next position with a header.
         */
        int nextHeaderAfter(int position) ;
        
    }
    
    public static class PinnedHeaderAdapterDefaults implements PinnedHeaderAdapter {
		@Override
		public int getPinnedHeaderState(int position) {
			throw new RuntimeException("Cannot be implemented as default.") ;
		}

		@Override
		public int pha_getPinnedHeaderFadeAlphaStyle() {
			return PINNED_HEADER_FADE_ALPHA_NO ;
		}

		@Override
		public boolean copyPinnedHeaderOccludingBounds(View header,
				int position, Rect r) {
			r.set(0, 0, header.getWidth(), header.getHeight()) ;
			return true ;
		}

		@Override
		public boolean copyPinnedHeaderVisibleBounds(View header, int position,
				Rect r) {
			r.set(0, 0, header.getWidth(), header.getHeight()) ;
			return true ;
		}

		@Override
		public boolean copyHeaderOccludingBounds(View view, int position, Rect r) {
			r.set(0, 0, view.getWidth(), view.getHeight()) ;
			return true ;
		}

		@Override
		public boolean copyHeaderVisibleBounds(View view, int position, Rect r) {
			r.set(0, 0, view.getWidth(), view.getHeight()) ;
			return true ;
		}

		@Override
		public void configurePinnedHeader(View header, int position, int alpha) {
			throw new RuntimeException("Cannot be implemented as default.") ;
		}

		@Override
		public int nextHeaderAfter(int position) {
			throw new RuntimeException("Cannot be implemented as default.") ;
		}
    }

    private static final int MAX_ALPHA = 255;

    private PinnedHeaderAdapter mAdapter;
    private View mHeaderView;
    private boolean mHeaderViewVisible;
    private Rect mHeaderViewOccludingBounds = new Rect() ;
    private Rect mHeaderViewVisibleBounds = new Rect() ;

    private int mHeaderViewWidth;

    private int mHeaderViewHeight;

    public PinnedHeaderListView(Context context) {
        super(context);
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PinnedHeaderListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setPinnedHeaderView(View view) {
    	mHeaderView = view;

        // Disable vertical fading when the pinned header is present
        // TODO change ListView to allow separate measures for top and bottom fading edge;
        // in this particular case we would like to disable the top, but not the bottom edge.
        if (mHeaderView != null) {
            setFadingEdgeLength(0);
        }
        requestLayout();
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        mAdapter = (PinnedHeaderAdapter)adapter;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView != null) {
            measureChild(mHeaderView, widthMeasureSpec, heightMeasureSpec);
            mHeaderViewWidth = mHeaderView.getMeasuredWidth();
            mHeaderViewHeight = mHeaderView.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mHeaderView != null) {
            mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
            configureHeaderView(getFirstVisiblePosition());
        }
    }

    Rect configureHeaderView_pinnedHeaderVisibleBounds = new Rect() ;
    Rect configureHeaderView_nextHeaderVisibleBounds = new Rect() ;
    
    
    public void configureHeaderView(int position) {
    	configureHeaderView(position, true) ;
    }
    
    /**
     * A more detailed look at configureHeaderView.  'position' indicates the
     * position in-list whose content determines the 'topmost header'; 'positionOnScreen'
     * indicates whether the specified position is on-screen (presumably as child 0).
     * @param position
     */
    public void configureHeaderView(int position, boolean positionIsOnScreen) {
        if (mHeaderView == null) {
            return;
        }

        int state = mAdapter.getPinnedHeaderState(position);
        switch (state) {
            case PinnedHeaderAdapter.PINNED_HEADER_STATE_GONE: {
                mHeaderViewVisible = false;
                break;
            }

            case PinnedHeaderAdapter.PINNED_HEADER_STATE_VISIBLE: {
            	mAdapter.configurePinnedHeader(mHeaderView, position, MAX_ALPHA);
                mAdapter.copyPinnedHeaderOccludingBounds(mHeaderView, position, mHeaderViewOccludingBounds) ;
                mAdapter.copyPinnedHeaderVisibleBounds(mHeaderView, position, mHeaderViewVisibleBounds) ;
                
                /*
                int nextHeaderPosition = mAdapter.nextHeaderAfter(position) ;
                // see if we can get this view.
                View nextHeaderView = null ;
                if ( nextHeaderPosition > -1 && this.getChildCount() > (nextHeaderPosition - position) )
                	nextHeaderView = this.getChildAt(nextHeaderPosition - position) ;
                	*/
                
                if (mHeaderView.getTop() != 0) {
                	 mHeaderView.layout(0, 0, mHeaderViewWidth, mHeaderViewHeight);
                }
                mHeaderViewVisible = true;
                break;
            }

            case PinnedHeaderAdapter.PINNED_HEADER_STATE_PUSHED_UP: {
            	// get the next "headered" View.  We presume it is 1 past the
            	// current view, unless the current position is not onscreen,
            	// in which case we assume 0.
            	View nextHeaderView ;
            	if ( positionIsOnScreen )
            		nextHeaderView = getChildAt(1);
            	else
            		nextHeaderView = getChildAt(0) ;
                if ( nextHeaderView != null ) {
	                int bottom = nextHeaderView.getTop();
	                //int itemHeight = firstView.getHeight();
	                int headerHeight = mHeaderView.getHeight();
	                int y = 0 ;
	                int alpha = MAX_ALPHA;
	                switch( mAdapter.pha_getPinnedHeaderFadeAlphaStyle() ) {
	                case PinnedHeaderAdapter.PINNED_HEADER_FADE_ALPHA_NO:
	                	alpha = MAX_ALPHA ;
	                	if (bottom < headerHeight) {
		                    y = (bottom - headerHeight);
		                } else {
		                    y = 0;
		                }
	                	break ;
	                case PinnedHeaderAdapter.PINNED_HEADER_FADE_ALPHA_CLIP_VISIBLE_PORTION:
	                	if (bottom < headerHeight) {
		                    y = (bottom - headerHeight);
		                    alpha = MAX_ALPHA * (headerHeight + y) / headerHeight;
		                } else {
		                    y = 0;
		                    alpha = MAX_ALPHA;
		                }
	                	break ;
	                case PinnedHeaderAdapter.PINNED_HEADER_FADE_ALPHA_HEADER_NONCONTENT_OVERLAP:
	                	// alpha based on content overlap.
	                	y = 0 ;
	                	mAdapter.copyPinnedHeaderVisibleBounds(mHeaderView, position, configureHeaderView_pinnedHeaderVisibleBounds) ;
	                	mAdapter.copyHeaderVisibleBounds(nextHeaderView, position, configureHeaderView_nextHeaderVisibleBounds) ;
	                	int space = configureHeaderView_nextHeaderVisibleBounds.top + nextHeaderView.getTop() - configureHeaderView_pinnedHeaderVisibleBounds.bottom ;
	                	int max_space = mHeaderViewHeight - configureHeaderView_pinnedHeaderVisibleBounds.bottom 
	                			+ configureHeaderView_nextHeaderVisibleBounds.top ;
	                	// space is the distance between the visible bottom of the pinned header (if displayed at its
	                	// pinned, not pushed-up, location) and the visible top of the next header.  If <= 0,
	                	// we have pushed the pinned header up and it should be completely transparent, and -- in fact
	                	// -- the amount of space left (a negative) is also its y coordinate).
	                	// If this space is > the total "non-visible" area of the views ("max_space", the bottom of pinned visible
	                	// to its bottom, the top of next to the top of visible), we keep pinned at the top and set
	                	// alpha to 255.  Otherwise, the portion of "max_space" covered by 'space' represents the alpha value.
	                	if ( space <= 0 ) {
	                		// put the bottom of visible bounds at the next's visible bounds.
	                		y = space ;
	                		alpha = 0 ;
	                	} else if ( space > max_space ) {
	                		alpha = MAX_ALPHA ;
	                	} else {
	                		alpha = Math.min( MAX_ALPHA, ( MAX_ALPHA * space ) / max_space ) ;
	                	}
	                	break ;
	                }
	                
	                mAdapter.configurePinnedHeader(mHeaderView, position, alpha);
	                
	                int nextHeaderPosition = mAdapter.nextHeaderAfter(position) ;
	                // see if we can get this view.
	                if ( nextHeaderPosition > -1 && this.getChildCount() > (nextHeaderPosition - position) )
	                	nextHeaderView = this.getChildAt(nextHeaderPosition - position) ;
	                
	                if (mHeaderView.getTop() != y || mHeaderViewVisibleBounds.height() == 0) {
	                	mHeaderView.layout(0, y, mHeaderViewWidth, mHeaderViewHeight + y);
	                    mAdapter.copyPinnedHeaderOccludingBounds(mHeaderView, position, mHeaderViewOccludingBounds) ;
	                    mAdapter.copyPinnedHeaderVisibleBounds(mHeaderView, position, mHeaderViewVisibleBounds) ;
	                }
	                mHeaderViewVisible = true;
                }
                else {
                	System.err.println("avoided a null-pointer exception.  current trace is") ;
                	new NullPointerException().printStackTrace() ;
                }
                break;
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
    	
    	// Clip our occluding region.
    	if ( mHeaderViewVisible ) {
    		canvas.save() ;
    		// NOTE: the simplest way is to clip DIFFERENCE.  However, we allow hardware
    		// acceleration for this, and 2D HW accel. ignores the clip types
    		// XOR, Difference and Reverse Difference.
    		// For that reason, rather than clip DIFFERENCE, we construct a 
    		// region that IS the difference, and clip that using standard intersection.
    		canvas.clipRect(0,
    				mHeaderView.getTop() + mHeaderViewOccludingBounds.bottom,
    				getWidth(),
    				Integer.MAX_VALUE) ;
    	}
        super.dispatchDraw(canvas);
        if ( mHeaderViewVisible ) {
        	canvas.restore() ;
        }
        
        
        if (mHeaderViewVisible) {
            drawChild(canvas, mHeaderView, getDrawingTime());
        }
    }
}