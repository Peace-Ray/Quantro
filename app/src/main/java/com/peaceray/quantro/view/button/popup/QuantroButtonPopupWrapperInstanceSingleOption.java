package com.peaceray.quantro.view.button.popup;

import android.view.View;

import com.peaceray.quantro.view.button.QuantroButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;

/**
 * Implements a "SingleOption" instance: skips the menu and counts as pressing
 * the same button when clicked.  Long-press is not supported.
 */
public class QuantroButtonPopupWrapperInstanceSingleOption extends
		QuantroButtonPopupWrapper {
	
	int mNum ;
	Object mTag ;
	
	QuantroButtonPopupWrapperInstanceSingleOption( int num, Object tag ) {
		mNum = num ;
		mTag = tag ;
	}
	
	QuantroButtonPopupWrapperInstanceSingleOption set( int num, Object tag ) {
		mNum = num ;
		mTag = tag ;
		return this ;
	}

	@Override
	public void onClick(View v) {
		QuantroButtonPopup.Listener listener = getListener() ;
		if ( listener != null )
			listener.qbpw_onButtonUsed((QuantroButton)v, false, mNum, mTag) ;
	}

	@Override
	public boolean onLongClick(View v) {
		return false;
	}

	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		return false;
	}
	
	public QuantroButtonPopupWrapper setEnabled( int index, boolean enabled ) {
		if ( index == mNum ) {
			QuantroContentWrappingButton b = getButton() ;
			b.setEnabled(enabled) ;
			b.setClickable(enabled) ;
		}
		
		return this ;
	}
	
	public QuantroButtonPopupWrapper setEnabled( Object tag, boolean enabled ) {
		if ( tag != null && tag.equals(mTag) ) {
			QuantroContentWrappingButton b = getButton() ;
			b.setEnabled(enabled) ;
			b.setClickable(enabled) ;
		}
		
		return this ;
	}

}
