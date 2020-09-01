/*
 * Copyright 2011 Micah Hainline
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
 * See http://stackoverflow.com/questions/2160619/android-ellipsize-multiline-textview
 *
 * Changes by Jake Rosin:
 *
 * - Different package
 * - Extend from HeavyShadowTextView (not TextView) to support Quantro app text style.
 * - Add support for shrinking text size to fit in view bounds and max line count.
 * - Add readAttrs: set maxLines and shrinkText from XML view layout.
 * - Override setTextSize to support shrinking text feature.
 * - Override onMeasure to support shrinking text feature (prevent measuring already-shrunk text).
 * - Override onLayout to mark content stale (for later measuring / ellipsizing).
 * - Alter resetText to support both resizing and ellipsizing text; alter createWorkingLayout
 * 		to allow the use of either measured on layout view dimensions (depending on context).
 */

package com.peaceray.quantro.view.generic;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class EllipsizingTextView extends HeavyShadowTextView {
    private static final String ELLIPSIS = "...";

    public interface EllipsizeListener {
        void ellipsizeStateChanged(boolean ellipsized);
    }

    private final List<EllipsizeListener> ellipsizeListeners = new ArrayList<EllipsizeListener>();
    private boolean isEllipsized;
    private boolean isStale;
    private boolean programmaticChange;
    private String fullText;
    private int maxLines = -1;
    private float lineSpacingMultiplier = 1.0f;
    private float lineAdditionalVerticalPadding = 0.0f;
    
    private float fullSize ;
    private float workingSize ;
    private boolean mShrinkText = false ;
    
    private DisplayMetrics mDisplayMetrics = null ;

    public EllipsizingTextView(Context context) {
        super(context);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttrs(attrs) ;
    }

    public EllipsizingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readAttrs(attrs) ;
    }
    
    private void readAttrs( AttributeSet attrs ) {
    	// Get the maxLines property, if set by XML.
        // We must read from 'attrs' as there is no TextView
        // accessor to retrieve this value.
        this.setMaxLines(attrs.getAttributeIntValue(
        		"http://schemas.android.com/apk/res/android",
        		"maxLines", -1) ) ;
        mShrinkText = attrs.getAttributeBooleanValue(
        		"http://schemas.android.com/apk/res/com.peaceray.quantro",
        		"shrinkText",
        		false) ;
       workingSize = fullSize = getTextSize() ;		// plain old exact pixels.
    }

    synchronized public void addEllipsizeListener(EllipsizeListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        ellipsizeListeners.add(listener);
    }

    synchronized public void removeEllipsizeListener(EllipsizeListener listener) {
        ellipsizeListeners.remove(listener);
    }

    synchronized public boolean isEllipsized() {
        return isEllipsized;
    }

    @Override
    synchronized public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        this.maxLines = maxLines;
        isStale = true;
    }

    synchronized public int getMaxLines() {
        return maxLines;
    }

    @Override
    synchronized public void setLineSpacing(float add, float mult) {
        this.lineAdditionalVerticalPadding = add;
        this.lineSpacingMultiplier = mult;
        super.setLineSpacing(add, mult);
    }

    @Override
    synchronized protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        if (!programmaticChange) {
            fullText = text.toString();
            isStale = true;
        }
    }
    
    
    @Override
    synchronized public void setTextSize( float size ) {
    	super.setTextSize(size) ;
    	if ( !programmaticChange ) {
    		isStale = true ;
    		if ( mDisplayMetrics == null ) {
    			mDisplayMetrics = new DisplayMetrics() ;
    			((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics) ;
    		}
    		
    		// store as exact dimensions
    		this.fullSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, mDisplayMetrics) ;
    	}
    }
    
    @Override
    synchronized public void setTextSize( int unit, float size ) {
    	super.setTextSize(unit, size) ;
    	if ( !programmaticChange ) {
    		isStale = true ;
    		if ( mDisplayMetrics == null ) {
    			mDisplayMetrics = new DisplayMetrics() ;
    			((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics) ;
    		}
    		
    		// store as exact dimensions
    		this.fullSize = TypedValue.applyDimension(unit, size, mDisplayMetrics) ;
    	}
    }
    
    
    @Override
    synchronized protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	
    	// Log.d(TAG, "onMeasure.  Full text is " + fullText) ;
    	// This method's purpose is to synchronized this call.
    	// We measure with the full text size.
    	
    	// There is a bug caused by the "wrap_content" layout dimension, where the
    	// measured ("wrapped") size is just a little too small for its content.
    	// I'm not sure where this comes from.  Here is a proposed fix:
    	
    	// First, measure using FILL_PARENT layout params.  This gives the maximum
    	// available space if we don't ellipsize.  Ellipsize using these dimensions to
    	// get the true text to be displayed.  Finally, measure again using the actual
    	// layout parameters.
    	CharSequence workingText = null ;
    	boolean didSetText = false ;
    	if ( fullText != null ) {
    		programmaticChange = true ;
    		workingText = getText() ;
    		setText(fullText) ;
    		didSetText = true ;
    	}
    	
    	boolean didSetSize = false ;
    	if ( fullSize != workingSize ) {
    		programmaticChange = true ;
    		setTextSize(TypedValue.COMPLEX_UNIT_PX, fullSize) ;
    		didSetSize = true ;
    	}
    	
    	// Previously, we simply called super.onMeasure() at this point.
    	// This causes a strange bug where layouts progressively decrease
    	// in size as they are measured slightly smaller than the available space.
    	// Instead, we perform slightly different measurement when we are allowed
    	// to scale down the text by altering its size.  This only works when
    	// our max lines is 1.
    	if ( maxLines == 1 && this.mShrinkText ) {
    		// the problem: we measure text that spreads across multiple
    		// lines, when actually we should prefer fitting it all on one line.
    		// Our potential solution: measure with unlimited width (fitting it
    		// all on one line) and then reset the measured width based on our 
    		// allowed space.  This will overestimate both width and height, but
    		// hopefully not to too severe an extent?
    		super.onMeasure(
    				MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE/2, MeasureSpec.AT_MOST),
    				heightMeasureSpec) ;
    		int specWidth = MeasureSpec.getSize(widthMeasureSpec) ;
    		switch( MeasureSpec.getMode(widthMeasureSpec) ) {
    		case MeasureSpec.AT_MOST:
    			this.setMeasuredDimension(
    					Math.min( getMeasuredWidth(), specWidth ),
    					getMeasuredHeight()) ;
    			break ;
    		case MeasureSpec.EXACTLY:
    			this.setMeasuredDimension(
    					specWidth,
    					getMeasuredHeight()) ;
    			break ;
    		}
    		//Log.d(TAG, "onMeasure has measured text " + getText() + " with size " + fullSize + " original measure spec " + MeasureSpec.toString(widthMeasureSpec) + " by " + MeasureSpec.toString(heightMeasureSpec) + " measured to " + getMeasuredWidth() + ", " + getMeasuredHeight() ) ;
    	} else {
    		super.onMeasure(widthMeasureSpec, heightMeasureSpec) ;
    	}
    	
    	if ( didSetText ) {
    		setText(workingText) ;
    	}
    	
    	if ( didSetSize ) {
    		setTextSize(TypedValue.COMPLEX_UNIT_PX, workingSize) ;
    	}
    	
    	if ( didSetText || didSetSize ) {
    		programmaticChange = false ;
    	}
    }
    
    
    @Override
    synchronized protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	if ( changed )
    		isStale = true ;
    	super.onLayout(changed, left, top, right, bottom) ;
    }

    @Override
    synchronized protected void onDraw(Canvas canvas) {
        if (isStale) {
            super.setEllipsize(null);
            resetText(false);
        }
        super.onDraw(canvas);
    }

    @SuppressLint("NewApi")
	synchronized private void resetText( boolean useMeasured ) {
    	if ( getWidth() == 0 )
    		return ;
        int maxLines = getMaxLines();
        String workingText = fullText ;
        workingSize = fullSize ;
        TextPaint workingPaint = getPaint() ;
        if ( mShrinkText ) {
        	workingPaint = new TextPaint(workingPaint) ;
        	workingPaint.setTextSize(workingSize) ;
        }
        boolean ellipsized = false;
        if (maxLines != -1) {
        	//Log.d(TAG, "workingText: " + workingText) ;
        	Layout layout = createWorkingLayout(workingText, workingPaint, useMeasured);
            if (layout.getLineCount() > maxLines) {
            	// shrink or ellipsize?
            	if ( !mShrinkText ) {
	            	//Log.d(TAG, "too long.  Lines = " + layout.getLineCount()) ;
	            	//Log.d(TAG, "character length is " + workingText.length()) ;
	            	//Log.d(TAG, "line end is " + layout.getLineEnd(maxLines)) ;
	            	// Remove any line beyond max+1.  This gives text that exceeds our
	            	// maximum lines by one line.
	                workingText = fullText.substring(0, layout.getLineEnd(maxLines)).trim();
	                Layout workingLayout = createWorkingLayout(workingText + ELLIPSIS, workingPaint, useMeasured) ;
	                while ( workingLayout.getLineCount() > maxLines && workingText.length() > 0) {
	                    int lastSpace = workingText.lastIndexOf(' ');
	                    if (lastSpace == -1 && workingText.length() > 0) {
	                    	// No spaces, but still oversized?  Truncate all but the last
	                    	// overshooting line, then remove character-by-character.
	                    	// Note: this may seem a degenerate case, but many languages,
	                    	// e.g. Chinese, do not use spaces in their text.
	                    	if ( workingLayout.getLineEnd(maxLines+1) -1 > maxLines + 1 ) {
	                    		int end = Math.max( 0, Math.min( workingLayout.getLineEnd(maxLines+1) - 1, workingText.length() -2 ) ) ;
	                    		workingText = workingText.substring( 0, end ) ;
	                    	}
	                    	else {
	                    		int end = Math.max( 0, workingText.length() - 2 ) ;
	                    		workingText = workingText.substring( 0, end ) ; 
	                    		// Log.d(TAG, "reducing workingText to " + end) ;
	                    	}
	                    }
	                    else
	                    	workingText = workingText.substring(0, lastSpace);
	                    workingLayout = createWorkingLayout(workingText + ELLIPSIS, workingPaint, useMeasured) ;
	                }
	                workingText = workingText + ELLIPSIS;
	                ellipsized = true;
            	} else {
            		// shrink.
            		if ( layout.getLineCount() > maxLines*2 )
            			workingSize /= 2 ;
            		else
            			workingSize *= 0.9 ;
            		workingPaint.setTextSize(workingSize) ;
            		Layout workingLayout = createWorkingLayout(workingText, workingPaint, useMeasured) ;
            		while ( workingLayout.getLineCount() > maxLines && workingSize > 4 ) {
            			// reduce size further
            			if ( workingLayout.getLineCount() > maxLines*2 )
                			workingSize /= 2 ;
                		else
                			workingSize *= 0.9 ;
            			workingPaint.setTextSize(workingSize) ;
                		workingLayout = createWorkingLayout(workingText, workingPaint, useMeasured) ;
            		}
            	}
            }
        }
        if (!workingText.equals(getText())) {
            programmaticChange = true;
            try {
            	setText(workingText);
            } finally {
                programmaticChange = false;
            }
        }
        if (workingSize != fullSize) {
        	programmaticChange = true ;
        	try {
        		this.setTextSize(TypedValue.COMPLEX_UNIT_PX, workingSize) ;
        	} finally {
        		programmaticChange = false ;
        	}
        }
        isStale = false;
        if (ellipsized != isEllipsized) {
            isEllipsized = ellipsized;
            for (EllipsizeListener listener : ellipsizeListeners) {
                listener.ellipsizeStateChanged(ellipsized);
            }
        }
    }

    private Layout createWorkingLayout(String workingText, TextPaint workingPaint, boolean useMeasured) {
    	if ( !useMeasured )
	    	return new StaticLayout(workingText, workingPaint, Math.max(0,getWidth() - getPaddingLeft() - getPaddingRight()),
	                Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
    	else
    		return new StaticLayout(workingText, workingPaint, Math.max(0,getMeasuredWidth() - getPaddingLeft() - getPaddingRight()),
	                Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
    }

    @Override
    synchronized public void setEllipsize(TruncateAt where) {
        // Ellipsize settings are not respected
    }
}
