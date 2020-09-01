/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.peaceray.quantro.view.dialog ;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import com.peaceray.quantro.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.preference.DialogPreference;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener
{
  private static final String androidns="http://schemas.android.com/apk/res/android";

  private SeekBar mSeekBar;
  private TextView mSplashText,mValueText;

  private String mDialogMessage, mSuffix;
  private int mDefault, mMax, mValue = 0;

  public SeekBarPreference(Context context, AttributeSet attrs) throws IllegalArgumentException, IllegalAccessException { 
    super(context,attrs); 

    mDialogMessage = attrs.getAttributeValue(androidns,"dialogMessage");
    mSuffix = attrs.getAttributeValue(androidns,"text");
    mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 0);
    mMax = attrs.getAttributeIntValue(androidns,"max", 100);
    
    Log.d("SeekBarPreference", "dialog message: " + mDialogMessage) ;
    
    // attempt to load real strings
    if ( mDialogMessage != null && mDialogMessage.startsWith("@string/") ) {
    	String fieldName = mDialogMessage.substring("@string/".length()) ;
    	try {
			Field f = R.string.class.getDeclaredField(fieldName) ;
			int id = f.getInt(null) ;
			mDialogMessage = context.getResources().getString(id) ;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    } else if ( mDialogMessage != null && mDialogMessage.startsWith("@") ) {
    	try {
    		int id = Integer.parseInt(mDialogMessage.substring(1)) ;
    		mDialogMessage = context.getResources().getString(id) ;
    	} catch( Exception e ) {
    		e.printStackTrace() ;
    	}
    }
    
    if ( mSuffix != null && mSuffix.startsWith("@string/") ) {
    	String fieldName = mSuffix.substring("@string/".length()) ;
    	try {
			Field f = R.string.class.getDeclaredField(fieldName) ;
			int id = f.getInt(null) ;
			mSuffix = context.getResources().getString(id) ;
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    } else if ( mSuffix != null && mSuffix.startsWith("@") ) {
    	try {
    		int id = Integer.parseInt(mSuffix.substring(1)) ;
    		mSuffix = context.getResources().getString(id) ;
    	} catch( Exception e ) {
    		e.printStackTrace() ;
    	}
    }
    
    

  }
  @Override 
  protected View onCreateDialogView() {
	Context context = getContext() ;
    LinearLayout.LayoutParams params;
    LinearLayout layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(6,6,6,6);

    mSplashText = new TextView(context);
    if (mDialogMessage != null)
      mSplashText.setText(mDialogMessage);
    layout.addView(mSplashText);

    mValueText = new TextView(context);
    mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
    mValueText.setTextSize(32);
    params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.FILL_PARENT, 
        LinearLayout.LayoutParams.WRAP_CONTENT);
    layout.addView(mValueText, params);

    mSeekBar = new SeekBar(context);
    mSeekBar.setOnSeekBarChangeListener(this);
    layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

    if (shouldPersist())
      mValue = getPersistedInt(mDefault);

    mSeekBar.setMax(mMax);
    mSeekBar.setProgress(mValue);
    return layout;
  }
  @Override 
  protected void onBindDialogView(View v) {
    super.onBindDialogView(v);
    mSeekBar.setMax(mMax);
    mSeekBar.setProgress(mValue);
  }
  @Override
  protected void onSetInitialValue(boolean restore, Object defaultValue)  
  {
    super.onSetInitialValue(restore, defaultValue);
    if (restore) 
      mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
    else 
      mValue = (Integer)defaultValue;
  }

  public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
  {
    String t = String.valueOf(value);
    mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
    if (shouldPersist())
      persistInt(value);
    callChangeListener(new Integer(value));
  }
  public void onStartTrackingTouch(SeekBar seek) {}
  public void onStopTrackingTouch(SeekBar seek) {}

  public void setMax(int max) { mMax = max; }
  public int getMax() { return mMax; }

  public void setProgress(int progress) { 
    mValue = progress;
    if (mSeekBar != null)
      mSeekBar.setProgress(progress); 
  }
  public int getProgress() { return mValue; }
}

