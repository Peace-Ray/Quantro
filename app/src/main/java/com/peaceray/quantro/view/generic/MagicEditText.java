package com.peaceray.quantro.view.generic;

import java.util.ArrayList;

import com.peaceray.quantro.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.EditText;

/**
 * MagicEditTextView is meant to echo the functionality of MagicTextView,
 * except for EditText fields.
 * 
 * Features will be added bit-by-bit, as-needed, to mirror those of MagicTextView
 * or introduce new ones.
 * 
 * At present, our main purpose is to allow automatic loading of asset fonts.
 * 
 * @author Jake
 *
 */
public class MagicEditText extends EditText {
	
	private ArrayList<Shadow> outerShadows;
	
	boolean frozen = false ;

	public MagicEditText(Context context) {
		super(context);
		init(null);
	}
	public MagicEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	public MagicEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}
	
	public void init(AttributeSet attrs){
		
		outerShadows = new ArrayList<Shadow>();

		if(attrs != null){
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MagicTextView);
			
            String typefaceName = a.getString( R.styleable.MagicTextView_typeface);
            if(typefaceName != null) {
                Typeface tf = Typeface.createFromAsset(getContext().getAssets(), String.format("fonts/%s.ttf", typefaceName));
                setTypeface(tf);
            }
            
            if(a.hasValue(R.styleable.MagicTextView_outerShadowColor)){
				this.addOuterShadow(a.getFloat(R.styleable.MagicTextView_outerShadowRadius, 0), 
									a.getFloat(R.styleable.MagicTextView_outerShadowDx, 0), 
									a.getFloat(R.styleable.MagicTextView_outerShadowDy, 0),
									a.getColor(R.styleable.MagicTextView_outerShadowColor, 0xff000000));
			}
		}
	}
	
	
	public void addOuterShadow(float r, float dx, float dy, int color){
		if(r == 0){ r = 0.0001f; }
		outerShadows.add(new Shadow(r,dx,dy,color));
	}
	
	
	public void clearOuterShadows(){
		outerShadows.clear();
	}
	
	
	@Override
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		
		freeze();
		Drawable[] restoreDrawables = this.getCompoundDrawables();
		int restoreColor = this.getCurrentTextColor();
		
		this.setCompoundDrawables(null,  null, null, null);

		if ( this.getText().length() > 0 ) {
			for(Shadow shadow : outerShadows){
				this.setShadowLayer(shadow.r, shadow.dx, shadow.dy, shadow.color);
				super.onDraw(canvas);
			}
		}
		this.setShadowLayer(0,0,0,0);
		this.setTextColor(restoreColor);

		if(restoreDrawables != null){
			this.setCompoundDrawablesWithIntrinsicBounds(restoreDrawables[0], restoreDrawables[1], restoreDrawables[2], restoreDrawables[3]);
		}
		this.setTextColor(restoreColor);

		unfreeze();
	}
	
	
	// Keep these things locked while onDraw in processing
	public void freeze(){
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
