package com.peaceray.quantro.view.generic;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * ViewGroup that arranges child views in a similar way to text, with them laid
 * out one line at a time and "wrapping" to the next line as needed.
 * 
 * Code licensed under CC-by-SA
 *  
 * @author Henrik Gustafsson
 * @see http://stackoverflow.com/questions/549451/line-breaking-widget-layout-for-android
 * @license http://creativecommons.org/licenses/by-sa/2.5/
 * 
 * Modified by Nishant Nair without attribution and posted here:
 * http://nishantvnair.wordpress.com/2010/09/28/flowlayout-in-android/
 *
 */

public class FlowLayout extends ViewGroup {

    private int line_height;

    public static class LayoutParams extends ViewGroup.LayoutParams {

        public final int horizontal_spacing;
        public final int vertical_spacing;

        /**
         * @param horizontal_spacing Pixels between items, horizontally
         * @param vertical_spacing Pixels between items, vertically
         */
        public LayoutParams(int width, int height, int horizontal_spacing, int vertical_spacing) {
            super(width, height);
            this.horizontal_spacing = horizontal_spacing;
            this.vertical_spacing = vertical_spacing;
        }
    }

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        assert (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED);

        final int width = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int height = MeasureSpec.getSize(heightMeasureSpec) - getPaddingTop() - getPaddingBottom();
        final int count = getChildCount();
        int line_height = 0;

        int xpos = getPaddingLeft();
        int ypos = getPaddingTop();

        int childHeightMeasureSpec;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
        } else {
            childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }


        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), childHeightMeasureSpec);
                final int childw = child.getMeasuredWidth();
                line_height = Math.max(line_height, child.getMeasuredHeight() + lp.vertical_spacing);

                if (xpos + childw > width) {
                    xpos = getPaddingLeft();
                    ypos += line_height;
                }
                xpos += childw + lp.horizontal_spacing;
            }
        }
        this.line_height = line_height;

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            height = ypos + line_height;

        } else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            if (ypos + line_height < height) {
                height = ypos + line_height;
            }
        }
        setMeasuredDimension(width, height);
    }
    
    
    @Override
    public ViewGroup.LayoutParams generateLayoutParams (AttributeSet attrs) {
    	ViewGroup.LayoutParams lp = super.generateLayoutParams(attrs) ;
    	// Load our new attributes: layout_horizontalSpacing and layout_verticalSpacing,
    	// used in the "FlowLayout" styleable.
    	int h = 1, v = 1 ;
    	TypedArray a = getContext().obtainStyledAttributes(attrs,
			    R.styleable.FlowLayout);
    	final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.FlowLayout_layout_horizontalSpacing:
				h = a.getDimensionPixelOffset(attr, h) ;
				break ;
			case R.styleable.FlowLayout_layout_verticalSpacing:
				v = a.getDimensionPixelOffset(attr, v) ;
				break ;
			}
		}
		
		return new LayoutParams(lp.width, lp.height, h, v) ;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(
        		ViewGroup.LayoutParams.WRAP_CONTENT,
        		ViewGroup.LayoutParams.WRAP_CONTENT,
        		1, 1); // default of 1px spacing
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LayoutParams) {
            return true;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        final int width = r - l;
        int xpos = getPaddingLeft();
        int ypos = getPaddingTop();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final int childw = child.getMeasuredWidth();
                final int childh = child.getMeasuredHeight();
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (xpos + childw > width) {
                    xpos = getPaddingLeft();
                    ypos += line_height;
                }
                // for row top-alignment, use ypos, ypos + childh
                // for row bottom-alignment, use (ypos+line_height-lp.vertical_spacing)-childh, (ypos+line_height-lp.vertical_spacing)
                // for row center-alignment, use <pending>
                // child.layout(xpos, ypos, xpos+childw, ypos + childh) ;
                child.layout(xpos, (ypos+line_height-lp.vertical_spacing)-childh, xpos + childw, ypos+line_height-lp.vertical_spacing);
                xpos += childw + lp.horizontal_spacing;
            }
        }
    }
}

