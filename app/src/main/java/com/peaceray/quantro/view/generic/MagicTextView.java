/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Micah Fivecoate
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Original at https://github.com/m5/MagicTextView
 */

package com.peaceray.quantro.view.generic;

import java.util.ArrayList;
import java.util.WeakHashMap;

import com.peaceray.quantro.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.text.style.ParagraphStyle;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.TextView;

public class MagicTextView extends TextView {
	private static final String TAG = "MagicTextView" ;
	
	private ArrayList<Shadow> outerShadows;
	private ArrayList<Shadow> innerShadows;
	
	private WeakHashMap<String, Pair<Canvas, Bitmap>> canvasStore;
	
	private Canvas tempCanvas;
	private Bitmap tempBitmap;
	
	private Drawable foregroundDrawable;
	
	private float strokeWidth;
	private Integer strokeColor;
	private Join strokeJoin;
	private float strokeMiter;
	
	private int indentFirst = 0 ;
	private int indentRest = 0 ;
	
	private int[] lockedCompoundPadding = new int[4];
	private boolean frozen = false;

	public MagicTextView(Context context) {
		super(context);
		init(null);
	}
	public MagicTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	public MagicTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}
	
	public void init(AttributeSet attrs){
		outerShadows = new ArrayList<Shadow>();
		innerShadows = new ArrayList<Shadow>();
		if(canvasStore == null){
		    canvasStore = new WeakHashMap<String, Pair<Canvas, Bitmap>>();
		}
	
		if(attrs != null){
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MagicTextView);
			
            String typefaceName = a.getString( R.styleable.MagicTextView_typeface);
            if(typefaceName != null) {
                Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                setTypeface(tf);
            }
            
			if(a.hasValue(R.styleable.MagicTextView_foreground)){
				Drawable foreground = a.getDrawable(R.styleable.MagicTextView_foreground);	
				if(foreground != null){
					this.setForegroundDrawable(foreground);
				}else{
					this.setTextColor(a.getColor(R.styleable.MagicTextView_foreground, 0xff000000));
				}
			}
		
			if(a.hasValue(R.styleable.MagicTextView_customBackground)){
				Drawable background = a.getDrawable(R.styleable.MagicTextView_customBackground);
				if(background != null){
					this.setBackgroundDrawable(background);
				}else{
					this.setBackgroundColor(a.getColor(R.styleable.MagicTextView_customBackground, 0xff000000));
				}
			}
			
			if(a.hasValue(R.styleable.MagicTextView_innerShadowColor)){
				this.addInnerShadow(a.getFloat(R.styleable.MagicTextView_innerShadowRadius, 0), 
									a.getFloat(R.styleable.MagicTextView_innerShadowDx, 0), 
									a.getFloat(R.styleable.MagicTextView_innerShadowDy, 0),
									a.getColor(R.styleable.MagicTextView_innerShadowColor, 0xff000000));
			}
			
			if(a.hasValue(R.styleable.MagicTextView_outerShadowColor)){
				this.addOuterShadow(a.getFloat(R.styleable.MagicTextView_outerShadowRadius, 0), 
									a.getFloat(R.styleable.MagicTextView_outerShadowDx, 0), 
									a.getFloat(R.styleable.MagicTextView_outerShadowDy, 0),
									a.getColor(R.styleable.MagicTextView_outerShadowColor, 0xff000000));
			}
			
			if(a.hasValue(R.styleable.MagicTextView_strokeColor)){
				float strokeWidth = a.getFloat(R.styleable.MagicTextView_strokeWidth, 1);
				int strokeColor = a.getColor(R.styleable.MagicTextView_strokeColor, 0xff000000);
				float strokeMiter = a.getFloat(R.styleable.MagicTextView_strokeMiter, 10);
				Join strokeJoin = null;
				switch(a.getInt(R.styleable.MagicTextView_strokeJoinStyle, 0)){
				case(0): strokeJoin = Join.MITER; break;
				case(1): strokeJoin = Join.BEVEL; break;
				case(2): strokeJoin = Join.ROUND; break;
				}
				this.setStroke(strokeWidth, strokeColor, strokeJoin, strokeMiter);
			}
			
			if (a.hasValue(R.styleable.MagicTextView_indentFirst)) {
				indentFirst = a.getDimensionPixelSize(R.styleable.MagicTextView_indentFirst, 0) ;
			}
			
			if (a.hasValue(R.styleable.MagicTextView_indentRest)) {
				indentRest = a.getDimensionPixelSize(R.styleable.MagicTextView_indentRest, 0) ;
			}
		}
	}
	
	@Override
	public void setText(CharSequence text, TextView.BufferType type) {
		// If we have an indent to apply, and this CharSequence does not
		// have an existing indent, the make one to apply.
		boolean needsIndent = false ;
		if ( indentFirst != 0 || indentRest != 0 ) {
			if ( type == TextView.BufferType.NORMAL ) {
				needsIndent = true ;
			}
			else if ( type == TextView.BufferType.SPANNABLE ) {
				if ( !( text instanceof SpannableString ) ) {
					needsIndent = true ;
				} else {
					SpannableString ss = (SpannableString) text ;
					LeadingMarginSpan [] spans = ss.getSpans(0, ss.length(), LeadingMarginSpan.class) ;
					if ( spans.length != 0 )
						needsIndent = true ;
				}
			}
		}
		
		if ( needsIndent ) {
			ParagraphStyle style_para = new LeadingMarginSpan.Standard(indentFirst, indentRest);
			SpannableString styledSource = text instanceof SpannableString
					? (SpannableString) text
					: new SpannableString (text);
			styledSource.setSpan (style_para, 0, styledSource.length(),
			        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
			text = styledSource ;
			type = TextView.BufferType.SPANNABLE ;
		}
		
		super.setText(text, type) ;
	}
	
	public void setStroke(float width, int color, Join join, float miter){
		strokeWidth = width;
		strokeColor = color;
		strokeJoin = join;
		strokeMiter = miter;
	}
	
	public void setStroke(float width, int color){
		setStroke(width, color, Join.MITER, 10);
	}
	
	public void addOuterShadow(float r, float dx, float dy, int color){
		if(r == 0){ r = 0.0001f; }
		outerShadows.add(new Shadow(r,dx,dy,color));
	}
	
	public void addInnerShadow(float r, float dx, float dy, int color){
		if(r == 0){ r = 0.0001f; }
		innerShadows.add(new Shadow(r,dx,dy,color));
	}
	
	public void clearInnerShadows(){
		innerShadows.clear();
	}
	
	public void clearOuterShadows(){
		outerShadows.clear();
	}
	
	public void setForegroundDrawable(Drawable d){
		this.foregroundDrawable = d;
	}
	
	public Drawable getForeground(){
		return this.foregroundDrawable == null ? this.foregroundDrawable : new ColorDrawable(this.getCurrentTextColor());
	}

	
	@Override
	synchronized protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		
		freeze();
		Drawable restoreBackground = this.getBackground();
		Drawable[] restoreDrawables = this.getCompoundDrawables();
		int restoreColor = this.getCurrentTextColor();
		
		this.setCompoundDrawables(null,  null, null, null);

		for(Shadow shadow : outerShadows){
			this.setShadowLayer(shadow.r, shadow.dx, shadow.dy, shadow.color);
			super.onDraw(canvas);
		}
		this.setShadowLayer(0,0,0,0);
		this.setTextColor(restoreColor);
		
		if(this.foregroundDrawable != null && this.foregroundDrawable instanceof BitmapDrawable){
			generateTempCanvas();
			super.onDraw(tempCanvas);
			Paint paint = ((BitmapDrawable) this.foregroundDrawable).getPaint();
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
			this.foregroundDrawable.setBounds(canvas.getClipBounds());
			this.foregroundDrawable.draw(tempCanvas);
			canvas.drawBitmap(tempBitmap, 0, 0, null);
			tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		}

		if(strokeColor != null){
			TextPaint paint = this.getPaint();
			paint.setStyle(Style.STROKE);
			paint.setStrokeJoin(strokeJoin);
			paint.setStrokeMiter(strokeMiter);
			this.setTextColor(strokeColor);
			paint.setStrokeWidth(strokeWidth);
			super.onDraw(canvas);
			paint.setStyle(Style.FILL);
			this.setTextColor(restoreColor);
		}
		if(innerShadows.size() > 0){
			generateTempCanvas();
			TextPaint paint = this.getPaint();
			for(Shadow shadow : innerShadows){
				this.setTextColor(shadow.color);
				super.onDraw(tempCanvas);
				this.setTextColor(0xFF000000);
				paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
				paint.setMaskFilter(new BlurMaskFilter(shadow.r, BlurMaskFilter.Blur.NORMAL));
				
                tempCanvas.save();
                tempCanvas.translate(shadow.dx, shadow.dy);
				super.onDraw(tempCanvas);
				tempCanvas.restore();
				canvas.drawBitmap(tempBitmap, 0, 0, null);
				tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
				
				paint.setXfermode(null);
				paint.setMaskFilter(null);
				this.setTextColor(restoreColor);
				this.setShadowLayer(0,0,0,0);
			}
		}
		
		
		if(restoreDrawables != null){
			this.setCompoundDrawablesWithIntrinsicBounds(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
		}
		this.setBackgroundDrawable(restoreBackground);
		this.setTextColor(restoreColor);

		unfreeze();
	}
	
	
	
	private void generateTempCanvas(){
	    String key = String.format("%dx%d", getWidth(), getHeight());
	    Pair<Canvas, Bitmap> stored = canvasStore.get(key);
	    if(stored != null){
	        tempCanvas = stored.first;
	        tempBitmap = stored.second;
	    }else{
            tempCanvas = new Canvas();
            tempBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            tempCanvas.setBitmap(tempBitmap);
            canvasStore.put(key, new Pair<Canvas, Bitmap>(tempCanvas, tempBitmap));
	    }
	}

	
	// Keep these things locked while onDraw in processing
	public void freeze(){
		// EDIT 2/1/2013 Jake Rosin: changes from an array allocation
		// to element assignment.  This is called in onDraw; don't
		// do allocation there!
		lockedCompoundPadding[0] = getCompoundPaddingLeft() ;
		lockedCompoundPadding[1] = getCompoundPaddingRight() ;
		lockedCompoundPadding[2] = getCompoundPaddingTop() ;
		lockedCompoundPadding[3] = getCompoundPaddingBottom() ;
		frozen = true;
	}
	
	public void unfreeze(){
		frozen = false;
	}
	
    
    @Override
    public void requestLayout(){
        if(!frozen) super.requestLayout();
    }
	
	@Override
	public void postInvalidate(){
		if(!frozen) super.postInvalidate();
	}
	
   @Override
    public void postInvalidate(int left, int top, int right, int bottom){
        if(!frozen) super.postInvalidate(left, top, right, bottom);
    }
	
	@Override
	public void invalidate(){
		if(!frozen)	super.invalidate();
	}
	
	@Override
	public void invalidate(Rect rect){
		if(!frozen) super.invalidate(rect);
	}
	
	@Override
	public void invalidate(int l, int t, int r, int b){
		if(!frozen) super.invalidate(l,t,r,b);
	}
	
	@Override
	public int getCompoundPaddingLeft(){
		return !frozen ? super.getCompoundPaddingLeft() : lockedCompoundPadding[0];
	}
	
	@Override
	public int getCompoundPaddingRight(){
		return !frozen ? super.getCompoundPaddingRight() : lockedCompoundPadding[1];
	}
	
	@Override
	public int getCompoundPaddingTop(){
		return !frozen ? super.getCompoundPaddingTop() : lockedCompoundPadding[2];
	}
	
	@Override
	public int getCompoundPaddingBottom(){
		return !frozen ? super.getCompoundPaddingBottom() : lockedCompoundPadding[3];
	}
	
	public static class Shadow{
		float r;
		float dx;
		float dy;
		int color;
		public Shadow(float r, float dx, float dy, int color){
			this.r = r;
			this.dx = dx;
			this.dy = dy;
			this.color = color;
		}
	}
}
