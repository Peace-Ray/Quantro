package com.peaceray.quantro.view.dialog;

import com.peaceray.quantro.R;

import android.content.Context;
import android.view.ViewGroup;

public class ProgressDialog extends AlertDialog {
	
	public static final int STYLE_SPINNER = android.app.ProgressDialog.STYLE_SPINNER ;
	public static final int STYLE_HORIZONTAL = android.app.ProgressDialog.STYLE_HORIZONTAL ;
	

	public ProgressDialog(Context context) {
		super(context);
		
		// load a custom view
		setContentView(R.layout.dialog_progress_basic) ;
		mContent.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT ;
		
		// hide everything
		setTitle(null) ;
		setMessage(null) ;
		setIcon(null) ;
	}
	
	public void setProgressStyle(int style) {
		// nothing to do here yet
		if ( style != STYLE_SPINNER )
			throw new IllegalArgumentException("ProgressDialog supports only STYLE_SPINNER at this time") ;
	}

}
