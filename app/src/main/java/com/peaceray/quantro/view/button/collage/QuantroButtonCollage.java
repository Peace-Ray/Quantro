package com.peaceray.quantro.view.button.collage;

import java.util.ArrayList;
import java.util.Iterator;

import com.peaceray.quantro.view.button.QuantroContentWrappingButton;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * Unlike QuantroButtonStrip, a ButtonCollage is a much more free-form,
 * nebulously structured item.  Individual buttons are placed and configured
 * in a LayoutFile; the collage itself does not perform any customized
 * layouts or measurements.  We allow superclass (RelativeLayout) functionality
 * for that.
 * 
 * Subclasses have the responsibility to configure contents, which can include
 * buttons and other things.  We include convenience methods: collectButtons
 * and collectViews.
 * 
 * @author Jake
 *
 */
public abstract class QuantroButtonCollage extends RelativeLayout {

	public QuantroButtonCollage(Context context) {
		super(context);
	}
	
	public QuantroButtonCollage(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public QuantroButtonCollage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	
	/**
	 * Collects all QuantroContentWrappingButtons within this view, returning
	 * them in an ArrayList.
	 * 
	 * This method performs allocation; it is recommended that you call this only
	 * once and then retain the references you want.
	 * 
	 * @return
	 */
	public ArrayList<QuantroContentWrappingButton> collectButtons() {
		ArrayList<QuantroContentWrappingButton> buttons = new ArrayList<QuantroContentWrappingButton>() ;
		
		collectButtons( this, buttons ) ;
		return buttons ;
	}
	
	private void collectButtons( View v, ArrayList<QuantroContentWrappingButton> buttons ) {
		if ( v == null )
			return ;
		
		if ( v instanceof QuantroContentWrappingButton ) {
			buttons.add((QuantroContentWrappingButton)v) ;
		}
		
		if ( v instanceof ViewGroup ) {
			ViewGroup vg = (ViewGroup)v ;
			for ( int i = 0; i < vg.getChildCount(); i++ ) {
				collectButtons( vg.getChildAt(i), buttons ) ;
			}
		}
	}
	
	
	/**
	 * Collects the QuantroContentWrappingButtons specified by the provided list
	 * of ids.  Returns an ArrayList of the same length, with each entry indicating
	 * the button of the specified id, or 'null' if none was found.
	 * 
	 * This method performs allocation; it is recommended that you call this only
	 * once and then retain the references you want.
	 * 
	 * @param ids
	 * @return
	 */
	public ArrayList<QuantroContentWrappingButton> collectButtons( ArrayList<Integer> ids ) {
		ArrayList<QuantroContentWrappingButton> buttons = new ArrayList<QuantroContentWrappingButton>() ;
		
		Iterator<Integer> iter = ids.iterator() ;
		for ( ; iter.hasNext() ; ) {
			int id = iter.next().intValue() ;
			View v = findViewById(id) ;
			if ( v != null && v instanceof QuantroContentWrappingButton ) {
				buttons.add((QuantroContentWrappingButton)v) ;
			}
		}
		
		return buttons ;
	}
	
	
	
	/**
	 * Collects the QuantroContentWrappingButtons specified by the provided list
	 * of ids.  Returns an ArrayList of the same length, with each entry indicating
	 * the button of the specified id, or 'null' if none was found.
	 * 
	 * This method performs allocation; it is recommended that you call this only
	 * once and then retain the references you want.
	 * 
	 * @param ids
	 * @return
	 */
	public ArrayList<View> collectViews( ArrayList<Integer> ids ) {
		ArrayList<View> buttons = new ArrayList<View>() ;
		
		Iterator<Integer> iter = ids.iterator() ;
		for ( ; iter.hasNext() ; ) {
			int id = iter.next().intValue() ;
			View v = findViewById(id) ;
			if ( v != null ) {
				buttons.add(v) ;
			}
		}
		
		return buttons ;
	}
	
	
	/**
	 *  Sometimes our content is based on data we can read from some other source.
	 *  This method will prompt that action - override it if you do this, and
	 *  be sure to postInvalidate.
	 */
	synchronized public void refresh() {
		forceLayout() ;
	}
	
}
