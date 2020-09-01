package com.peaceray.quantro.view.dialog;

import com.peaceray.quantro.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class CheckBoxDialog extends AlertDialog {
	
	public CheckBoxDialog(Context context) {
		super(context);
		
		// load a custom view
		setContentView(R.layout.dialog_checkbox) ;
	}
	
	public void setOnCheckedChangeListener( CompoundButton.OnCheckedChangeListener listener ) {
		mCheckBox.setOnCheckedChangeListener(listener) ;
	}
	
	public void setChecked( boolean checked ) {
		mCheckBox.setChecked(checked) ;
	}

	public boolean getChecked() {
		return mCheckBox.isChecked() ;
	}
	
	public void setCheckBoxMessage( CharSequence message ) {
		if ( mCheckBoxMessage != null ) {
			if ( message == null  )
				mCheckBoxMessage.setVisibility(View.GONE) ;
			else {
				mCheckBoxMessage.setText(message) ;
				mCheckBoxMessage.setVisibility(View.VISIBLE) ;
			}
		} else if ( message != null )
			throw new NullPointerException("Current content view does not have a checkbox message view") ;
		
		mContent.requestLayout() ;
	}
	
	public void setCheckBoxMessage( int resID ) {
		if ( mCheckBoxMessage != null ) {
			if ( resID == 0  )
				mCheckBoxMessage.setVisibility(View.GONE) ;
			else {
				mCheckBoxMessage.setText(resID) ;
				mCheckBoxMessage.setVisibility(View.VISIBLE) ;
			}
		} else if ( resID != 0 )
			throw new NullPointerException("Current content view does not have a checkbox message view") ;
		
		mContent.requestLayout() ;
	}
	
	
	@Override
	/**
	 * A good candidate to override if you need access to custom content.
	 * @param content
	 */
	protected void getSubviews( ViewGroup content) {
		super.getSubviews(content) ;
		mCheckBox = (CheckBox)content.findViewById(R.id.dialog_content_checkbox) ;
		mCheckBoxMessage = (TextView)content.findViewById(R.id.dialog_content_checkbox_message) ;
	}
	
	private CheckBox mCheckBox ;
	private TextView mCheckBoxMessage ;
	
	public static class Builder extends AlertDialog.Builder {
		
		CheckBoxDialog mCheckBoxDialog ;
		
		public Builder( Context context ) {
			super( context, new CheckBoxDialog(context) ) ;
			mCheckBoxDialog = (CheckBoxDialog)mAlertDialog ;
			mCheckBoxDialog.setCheckBoxMessage(null) ;
			mCheckBoxDialog.setChecked(false) ;
		}
		
		
		@Override
		public CheckBoxDialog create() {
			return (CheckBoxDialog)super.create() ;
		}
		
		public CheckBoxDialog.Builder setChecked( boolean checked ) {
			throwIfCreated() ;
			mCheckBoxDialog.setChecked(checked) ;
			return this ;
		}
		
		public CheckBoxDialog.Builder setCheckBoxMessage( int resID ) {
			throwIfCreated() ;
			mCheckBoxDialog.setCheckBoxMessage(resID) ;
			return this ;
		}
		
		public CheckBoxDialog.Builder setCheckBoxMessage( CharSequence message ) {
			throwIfCreated() ;
			mCheckBoxDialog.setCheckBoxMessage(message) ;
			return this ;
		}
		
		public CheckBoxDialog.Builder setOnCheckedChangeListener( CompoundButton.OnCheckedChangeListener listener ) {
			throwIfCreated() ;
			mCheckBoxDialog.setOnCheckedChangeListener(listener) ;
			return this ;
		}
	}
}
