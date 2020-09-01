package com.peaceray.quantro.view.button.popup;

import android.view.View;

import com.peaceray.quantro.view.button.QuantroButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;

public abstract class QuantroButtonPopupWrapper implements View.OnClickListener, View.OnLongClickListener, QuantroContentWrappingButton.SupportsLongClickOracle {

	private QuantroContentWrappingButton mButton ;
	private QuantroButtonPopup.Listener mListener ;
	
	QuantroButtonPopup.Listener getListener() {
		return mListener ;
	}
	
	QuantroButtonPopupWrapper setListener( QuantroButtonPopup.Listener listener ) {
		mListener = listener ;
		return this ;
	}
	
	QuantroContentWrappingButton getButton() {
		return mButton ;
	}
	
	QuantroButtonPopupWrapper setButton( QuantroContentWrappingButton button ) {
		mButton = button ;
		
		button.setOnClickListener(this) ;
		button.setOnLongClickListener(this) ;
		button.setSupportsLongClickOracle(this) ;
		
		return this ;
	}
	
	public abstract QuantroButtonPopupWrapper setEnabled( int index, boolean enabled ) ;
	
	public abstract QuantroButtonPopupWrapper setEnabled( Object tag, boolean enabled ) ;
	
	
}
