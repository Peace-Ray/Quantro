package com.peaceray.quantro.utils.onlayoutchangelistener;

import com.peaceray.quantro.utils.VersionCapabilities;

import android.view.View;

public class OnLayoutChangeListenerFactory {

	public interface Implementation {
		void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) ;
	}
	
	/**
	 * If this Android version supports OnLayoutChangeListeners,
	 * returns an Object which implements the interface and will call
	 * 'Implementation's' onLayoutChange method when appropriate.
	 * 
	 * Otherwise, returns 'null.'
	 * 
	 * @param implementation
	 * @return
	 */
	public static Object wrap( Implementation implementation ) {
		if ( VersionCapabilities.versionAtLeast( VersionCapabilities.VERSION_API_11 ) ) {
			return new OnLayoutChangeListener_API_11( implementation ) ;
		}
		return null ;
	}
	
}
