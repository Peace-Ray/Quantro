package com.peaceray.quantro.view.dialog;

import com.peaceray.quantro.R;

import android.content.Context;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

public class TimePickerDialog extends AlertDialog implements OnTimeChangedListener {
	
	public static final String PLACEHOLDER_HOUR = "XXXX_HOUR" ;
	public static final String PLACEHOLDER_MINUTE = "XXXX_HOUR" ;
	
	public interface OnTimeSetListener {
		abstract void onTimeSet(TimePicker view, int hourOfDay, int minute) ;
	}

	public TimePickerDialog(Context context, TimePickerDialog.OnTimeSetListener callBack, int hourOfDay, int minute, boolean is24HourView) {
		super(context);
		
		mOnTimeSetListener = callBack ;
		mHour = hourOfDay ;
		mMinute = minute ;
		
		// load custom content view
		setContentView(R.layout.dialog_time_picker_basic) ;
		
		// set defaults
		setTitle(null) ;
		setMessage(null) ;
		setIcon(null) ;
		mContentTimePicker.setCurrentHour(mHour) ;
		mContentTimePicker.setCurrentMinute(mMinute) ;
		mContentTimePicker.setIs24HourView(is24HourView) ;
		mContentTimePicker.setOnTimeChangedListener(this) ;
		
		// by default we have "set" and "cancel" buttons.  Configure them now.
		setButton( BUTTON_POSITIVE, "Set", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if ( mOnTimeSetListener != null )
					mOnTimeSetListener.onTimeSet(mContentTimePicker, mHour, mMinute) ;
				dialog.dismiss() ;
			}
		}) ;
		
		setButton( BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel() ;
			}
		}) ;
	}

	
	protected TimePicker mContentTimePicker ;
	protected OnTimeSetListener mOnTimeSetListener ;
	
	protected int mHour ;
	protected int mMinute ;
	
	protected String mLabel ;
	
	
	public void setLabel(String label) {
		mLabel = label ;
		updateTitle(mHour, mMinute) ;
	}
	
	
	public void onTimeChanged(TimePicker view, int hour, int minute) {
		if ( view == mContentTimePicker ) {
			mHour = hour ;
			mMinute = minute ;
		    updateTitle(hour, minute);
		}
	}

	public void updateTitle(int hour, int minute) {
	    setTitle(mLabel + hour + ":" + formatNumber(minute));
	}

	private String formatNumber(int number) {
	    String result = "";
	    if (number < 10) {
	        result += "0";
	    }
	    result += number;
	
	    return result;
    }
	
	
	@Override
	/**
	 * A good candidate to override if you need access to custom content.
	 * @param content
	 */
	protected void getSubviews( ViewGroup content) {
		super.getSubviews(content) ;
		mContentTimePicker = (TimePicker)mContent.findViewById(R.id.dialog_content_time_picker) ;
	}
}
