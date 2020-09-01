package com.peaceray.quantro.utils.onlayoutchangelistener;
import com.peaceray.quantro.utils.onlayoutchangelistener.OnLayoutChangeListenerFactory.Implementation;

import android.view.View ;

public class OnLayoutChangeListener_API_11 implements View.OnLayoutChangeListener {

	Implementation mImplementation ;
	
	public OnLayoutChangeListener_API_11( Implementation implementation ) {
		mImplementation = implementation ;
	}
	
	@Override
	public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
		if ( mImplementation != null ) {
			mImplementation.onLayoutChange(v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ;
		}
	}

}
