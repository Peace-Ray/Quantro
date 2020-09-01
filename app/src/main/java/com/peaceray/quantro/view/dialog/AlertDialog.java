package com.peaceray.quantro.view.dialog;

import com.peaceray.quantro.R;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.strip.DialogButtonStrip;
import com.peaceray.quantro.QuantroActivity;
import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.QuantroPreferences;

import android.app.Dialog;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class AlertDialog extends Dialog {
	
	private static final String TAG = "AlertDialog" ;
	
	QuantroSoundPool mQuantroSoundPool ;
	boolean mSoundControls ;

	public AlertDialog(Context context) {
		// Use the NoTitleBar theme for this, UNLESS we are displayed over
		// and activity that is full screened and we don't support immersive.
		// If we DO support immersive, we will be setting the window ourselves
		// in just a bit.
		super( context,
				(context instanceof QuantroActivity
						&& !((QuantroActivity)context).getFlagShowNotification()
						&& !VersionCapabilities.supportsImmersiveFullScreen() )
				? android.R.style.Theme_Translucent_NoTitleBar_Fullscreen
				: android.R.style.Theme_Translucent_NoTitleBar) ;
		
		if ( context instanceof QuantroActivity ) {
			QuantroActivity qa = (QuantroActivity) context ;
			if ( !qa.getFlagShowNavigation() ) {
				VersionSafe.setupUIImmersive(this) ;
			} else if ( !qa.getFlagShowNotification() ) {
				VersionSafe.setupUIShowNavigationBar(this, qa.isActivityGame()) ;
			}
		}
		
		this.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation ;
		
		if ( context instanceof Activity )
			this.setOwnerActivity((Activity)context) ;
		
		Resources res = context.getResources() ;
		
		// load dialog as our content view...
		super.setContentView(R.layout.dialog) ;
		
		// get content, fill with basic alert dialog
		mContent = (ViewGroup)super.findViewById(R.id.dialog_content) ;
		setContentView( R.layout.dialog_alert_basic ) ;
		
		// tag for color-filterable content
		mTagColorFilterable = res.getString(R.string.tag_color_filterable) ;
		
		// set colors for filterable elements
		setFilterableContentColor( res.getColor(R.color.dialog_default_color_filter) ) ;
		
		// get references to our elements now.
		getSubviews( mContent ) ;
		
		// lastly: set an onCancelListener.
		setOnCancelListener(null) ;
		
		mQuantroSoundPool = (context instanceof QuantroActivity)
				? ( (QuantroApplication)( context.getApplicationContext() ) ).getSoundPool((QuantroActivity)context)
				: null ;
	}
	
	@Override
	protected void onStart() {
		mSoundControls = QuantroPreferences.getSoundControls(getContext()) ;
	}

	private class OnClickListenerWithSound implements DialogInterface.OnClickListener {
		DialogInterface.OnClickListener mListener ;
		
		public OnClickListenerWithSound( DialogInterface.OnClickListener listener ) {
			mListener = listener ;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if ( mQuantroSoundPool != null && mSoundControls ) {
				if ( which == DialogInterface.BUTTON_NEGATIVE )
					mQuantroSoundPool.menuButtonBack() ;
				else
					mQuantroSoundPool.menuButtonClick() ;
			}
			mListener.onClick(dialog, which) ;
		}
	}
	
	protected DialogInterface.OnClickListener wrap( DialogInterface.OnClickListener listener ) {
		if ( listener == null )
			return null ;
		return new OnClickListenerWithSound( listener ) ;
	}

	private class OnCancelListenerWithSound implements DialogInterface.OnCancelListener {
		DialogInterface.OnCancelListener mListener ;
		
		public OnCancelListenerWithSound( DialogInterface.OnCancelListener listener ) {
			mListener = listener ;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			if ( mQuantroSoundPool != null && mSoundControls )
				mQuantroSoundPool.menuButtonBack() ;
			if ( mListener != null )
				mListener.onCancel(dialog) ;
		}
	}
	
	protected DialogInterface.OnCancelListener wrap( DialogInterface.OnCancelListener listener ) {
		return new OnCancelListenerWithSound( listener ) ;
	}
	
	
	
	
	public void setInset( int insetPixels ) {
		insetPixels = Math.max(0, insetPixels) ;
		View wrapper = super.findViewById(R.id.dialog_wrapping_panel) ;
		wrapper.setPadding(insetPixels, insetPixels, insetPixels, insetPixels) ;
		wrapper.requestLayout() ;
	}
	
	public void setInset( int horizPixels, int vertPixels ) {
		horizPixels = Math.max(0, horizPixels) ;
		vertPixels = Math.max(0, vertPixels) ;
		View wrapper = super.findViewById(R.id.dialog_wrapping_panel) ;
		wrapper.setPadding(horizPixels, vertPixels, horizPixels, vertPixels) ;
		wrapper.requestLayout() ;
	}
	
	@Override
	public void setOnCancelListener( DialogInterface.OnCancelListener listener ) {
		//Log.d(TAG, "in setOnCancelListener") ;
		super.setOnCancelListener(wrap(listener)) ;
	}
	
	public void setButton(int whichButton, CharSequence text, DialogInterface.OnClickListener listener) {
		if ( mContentButtonStrip != null ) {
			if ( text != null ) {
				mContentButtonStrip.setButton(whichButton, text, wrap(listener)) ;
				setButtonVisible( whichButton, true ) ;
			} else {
				mContentButtonStrip.setButton(whichButton, text, wrap(listener)) ;
				setButtonVisible( whichButton, false ) ;
			}
		} else if ( text != null || listener != null )
			throw new NullPointerException("Current content view does not have a button strip") ;
		
		mContent.requestLayout() ;
	}
	
	public void setButton(int whichButton, CharSequence text, int color, DialogInterface.OnClickListener listener) {
		if ( mContentButtonStrip != null ) {
			if ( text != null ) {
				mContentButtonStrip.setButton(whichButton, text, color, wrap(listener)) ;
				setButtonVisible( whichButton, true ) ;
			} else {
				mContentButtonStrip.setButton(whichButton, text, color, wrap(listener)) ;
				setButtonVisible( whichButton, false ) ;
			}
		} else if ( text != null || listener != null )
			throw new NullPointerException("Current content view does not have a button strip") ;
		
		mContent.requestLayout() ;
	}
	
	public void setButtonEnabled( int whichButton, boolean enabled ) {
		if ( mContentButtonStrip != null )
			mContentButtonStrip.enableButton(whichButton, enabled) ;
		else
			throw new NullPointerException("Current content view does not have a button strip") ;
		
		mContent.requestLayout() ;
	}
	
	public void setButtonVisible( int whichButton, boolean visible ) {
		if ( mContentButtonStrip != null ) {
			mContentButtonStrip.showButton(whichButton, visible) ;
			if ( mContentButtonStrip.hasVisibleButtons() )
				mContentButtonStrip.setVisibility( View.VISIBLE ) ;
			else
				mContentButtonStrip.setVisibility( View.GONE ) ;
		} else if ( visible )
			throw new NullPointerException("Current content view does not have a button strip") ;
		
		mContent.requestLayout() ;
	}
	
	public void setButtonColor( int whichButton, int color ) {
		if ( mContentButtonStrip != null ) {
			mContentButtonStrip.setButtonColor(whichButton, color) ;
		} else
			throw new NullPointerException("Current content view does not have a button strip") ;
		
		mContent.requestLayout() ;
	}
	
	public void setButtonTitle( int whichButton, String title ) {
		if ( mContentButtonStrip != null )
			mContentButtonStrip.setButtonTitle(whichButton, title) ;
		else
			throw new NullPointerException("Current content view does not have a button strip") ;
		
		mContent.requestLayout() ;
	}
	
	
	@Override
	public void setTitle( int resID ) {
		if ( mContentTitle != null ) {
			if ( resID == 0  )
				mContentTitle.setVisibility(View.GONE) ;
			else {
				mContentTitle.setText(resID) ;
				mContentTitle.setVisibility(View.VISIBLE) ;
				mContent.requestLayout() ;
			}
		} else if ( resID != 0 )
			throw new NullPointerException("Current content view does not have a title view") ;
		
		mContent.requestLayout() ;
	}
	
	@Override
	public void setTitle( CharSequence title ) {
		if ( mContentTitle != null ) {
			if ( title == null  )
				mContentTitle.setVisibility(View.GONE) ;
			else {
				mContentTitle.setText(title) ;
				mContentTitle.setVisibility(View.VISIBLE) ;
			}
		} else if ( title != null )
			throw new NullPointerException("Current content view does not have a title view") ;
		
		mContent.requestLayout() ;
	}
	
	
	public void setMessage( int resID ) {
		if ( mContentMessage != null ) {
			if ( resID == 0  ) {
				mContentMessage.setVisibility(View.GONE) ;
				if ( mContentMessageContainer != null )
					mContentMessageContainer.setVisibility(View.GONE); 
			}
			else {
				mContentMessage.setText(resID) ;
				mContentMessage.setVisibility(View.VISIBLE) ;
				if ( mContentMessageContainer != null )
					mContentMessageContainer.setVisibility(View.VISIBLE); 
			}
		} else if ( resID != 0 )
			throw new NullPointerException("Current content view does not have a message view") ;
		
		mContent.requestLayout() ;
	}
	
	public void setMessage( CharSequence message ) {
		if ( mContentMessage != null ) {
			if ( message == null  ) {
				mContentMessage.setVisibility(View.GONE) ;
				if ( mContentMessageContainer != null )
					mContentMessageContainer.setVisibility(View.GONE); 
			}
			else {
				mContentMessage.setText(message) ;
				mContentMessage.setVisibility(View.VISIBLE) ;
				if ( mContentMessageContainer != null )
					mContentMessageContainer.setVisibility(View.VISIBLE); 
			}
		} else if ( message != null )
			throw new NullPointerException("Current content view does not have a message view") ;
		
		mContent.requestLayout() ;
	}
	
	public void setIcon( Drawable icon ) {
		if ( mContentIcon != null ) {
			if ( icon == null )
				mContentIcon.setVisibility(View.GONE) ;
			else {
				mContentIcon.setImageDrawable( icon ) ;
				mContentIcon.setVisibility(View.VISIBLE) ;
			}
		} else if ( icon != null )
			throw new NullPointerException("Current content view does not have an icon view") ;
		
		mContent.requestLayout() ;
	}
	
	public void setIcon( int resID ) {
		if ( mContentIcon != null ) {
			if ( resID == 0 )
				mContentIcon.setVisibility(View.GONE) ;
			else {
				mContentIcon.setImageDrawable( getContext().getResources().getDrawable(resID) ) ;
				mContentIcon.setVisibility(View.VISIBLE) ;
			}
		} else if ( resID != 0 )
			throw new NullPointerException("Current content view does not have an icon view") ;
		
		mContent.requestLayout() ;
	}
	
	/**
	 * A good candidate to override if you need access to custom content.
	 * @param content
	 */
	protected void getSubviews( ViewGroup content) {
		mContentTitle = (TextView) content.findViewById(R.id.dialog_content_title) ;
		mContentIcon = (ImageView) content.findViewById(R.id.dialog_content_icon) ;
		mContentMessage = (TextView) content.findViewById(R.id.dialog_content_message) ;
		mContentMessageContainer = content.findViewById(R.id.dialog_content_message_container) ;
		mContentButtonStrip = (DialogButtonStrip) content.findViewById(R.id.dialog_content_button_strip) ;
		if ( mContentButtonStrip != null )
			mContentButtonStrip.setDialogInterface(this) ;
	}
	
	protected void setFilterableContentColor( int color ) {
		Log.d("AlertDialog", "Looking for filterable elements to filter with " + color) ;
		if ( mContent != null )
			setFilterableContentColor( color, mContent ) ;
	}
	
	protected void setFilterableContentColor( int color, ViewGroup vg ) {
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View v = vg.getChildAt(i) ;
			if ( v instanceof ViewGroup )
				setFilterableContentColor( color, (ViewGroup)v ) ;
			if ( mTagColorFilterable.equals( v.getTag() ) && v.getBackground() != null ) {
				Log.d("AlertDialog", "applying color filter: color is " + color) ;
				v.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP) ;
			}
		}
	}
	
	protected ViewGroup mContent ;
	protected String mTagColorFilterable ;
	
	// alert dialog elements
	protected TextView mContentTitle ;
	protected ImageView mContentIcon ;
	protected TextView mContentMessage ;
	protected View mContentMessageContainer ;
	protected DialogButtonStrip mContentButtonStrip ;


	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// OVERRIDE TO BREAK AND / OR RE-ROUTE TO CONTENT VIEW
	
	@Override
	public void addContentView(View view, ViewGroup.LayoutParams params) {
		throw new RuntimeException("Error: com.peaceray.quantro.dialog.AlertDialog does not support addContentView") ;
	}
	
	@Override
	public View findViewById( int id ) {
		return mContent.findViewById(id) ;
	}
	
	@Override
	public void setContentView( View v ) {
		mContent.removeAllViews() ;
		mContent.addView(v) ;
		getSubviews( mContent ) ;
		mContent.requestLayout() ;
	}
	
	@Override
	public void setContentView( int resLayout ) {
		mContent.removeAllViews() ;
		getLayoutInflater().inflate(resLayout, mContent) ;
		getSubviews( mContent ) ;
		mContent.requestLayout() ;
	}
	
	@Override
	public void setContentView( View view, ViewGroup.LayoutParams params ) {
		mContent.removeAllViews() ;
		mContent.addView(view, params) ;
		getSubviews( mContent ) ;
		mContent.requestLayout() ;
	}
	
	
	public View getContentView() {
		return mContent ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// BUILDER
	
	public static class Builder {
		
		protected AlertDialog mAlertDialog ;
		protected boolean mCreated ;
		
		public Builder( Context context ) {
			this( context, new AlertDialog( context ) ) ;
		}
		
		protected Builder( Context context, AlertDialog alertDialog ) {
			mAlertDialog = alertDialog ;
			mAlertDialog.setTitle(null) ;
			mAlertDialog.setIcon(null) ;
			mAlertDialog.setMessage(null) ;
			mAlertDialog.setButtonVisible(BUTTON_POSITIVE, false) ;
			mAlertDialog.setButtonVisible(BUTTON_NEUTRAL, false) ;
			mAlertDialog.setButtonVisible(BUTTON_NEGATIVE, false) ;
			mCreated = false ;
		}
		
		protected AlertDialog newAlertDialogInstance( Context context ) {
			return new AlertDialog( context ) ;
		}
		
		protected void throwIfCreated() {
			if ( mCreated )
				throw new IllegalStateException("An AlertDialog.Builder can only build one AlertDialog in its lifetime, and cannot be mutated afterwards") ;
		}
		
		public AlertDialog create() {
			throwIfCreated() ;
			mCreated = true ;
			return mAlertDialog ;
		}
		
		public Context getContext() {
			return mAlertDialog.getContext() ;
		}
		
		public AlertDialog.Builder setCancelable( boolean cancelable ) {
			throwIfCreated() ;
			mAlertDialog.setCancelable(cancelable) ;
			return this; 
		}
		
		public AlertDialog.Builder setIcon(Drawable icon) {
			throwIfCreated() ;
			mAlertDialog.setIcon( icon ) ;
			return this ;
		}
		
		public AlertDialog.Builder setIcon(int resID) {
			throwIfCreated() ;
			mAlertDialog.setIcon( resID ) ;
			return this ;
		}
		
		public AlertDialog.Builder setMessage(CharSequence msg) {
			throwIfCreated() ;
			mAlertDialog.setMessage( msg ) ;
			return this ;
		}
		
		public AlertDialog.Builder setMessage(int resID) {
			throwIfCreated() ;
			mAlertDialog.setMessage( resID ) ;
			return this ;
		}
		
		public AlertDialog.Builder setNegativeButton( CharSequence text, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEGATIVE, text, listener ) ;
		}
		
		public AlertDialog.Builder setNegativeButton( int resID, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEGATIVE, resID, listener ) ;
		}
		
		public AlertDialog.Builder setNeutralButton( CharSequence text, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEUTRAL, text, listener ) ;
		}
		
		public AlertDialog.Builder setNeutralButton( int resID, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEUTRAL, resID, listener ) ;
		}
		
		public AlertDialog.Builder setPositiveButton( CharSequence text, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_POSITIVE, text, listener ) ;
		}
		
		public AlertDialog.Builder setPositiveButton( int resID, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_POSITIVE, resID, listener ) ;
		}
		
		public AlertDialog.Builder setNegativeButton( CharSequence text, int color, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEGATIVE, text, color, listener ) ;
		}
		
		public AlertDialog.Builder setNegativeButton( int resID, int color, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEGATIVE, resID, color, listener ) ;
		}
		
		public AlertDialog.Builder setNeutralButton( CharSequence text, int color, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEUTRAL, text, color, listener ) ;
		}
		
		public AlertDialog.Builder setNeutralButton( int resID, int color, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_NEUTRAL, resID, color, listener ) ;
		}
		
		public AlertDialog.Builder setPositiveButton( CharSequence text, int color, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_POSITIVE, text, color, listener ) ;
		}
		
		public AlertDialog.Builder setPositiveButton( int resID, int color, DialogInterface.OnClickListener listener ) {
			return setButton( BUTTON_POSITIVE, resID, color, listener ) ;
		}
		
		private AlertDialog.Builder setButton( int which, CharSequence text, DialogInterface.OnClickListener listener ) {
			throwIfCreated() ;
			mAlertDialog.setButton(which, text, listener) ;
			return this ;
		}
		
		private AlertDialog.Builder setButton( int which, int resID, DialogInterface.OnClickListener listener ) {
			throwIfCreated() ;
			mAlertDialog.setButton(which, getContext().getResources().getString(resID), listener) ;
			return this ;
		}
		
		private AlertDialog.Builder setButton( int which, CharSequence text, int color, DialogInterface.OnClickListener listener ) {
			throwIfCreated() ;
			mAlertDialog.setButton(which, text, color, listener) ;
			return this ;
		}
		
		private AlertDialog.Builder setButton( int which, int resID, int color, DialogInterface.OnClickListener listener ) {
			throwIfCreated() ;
			mAlertDialog.setButton(which, getContext().getResources().getString(resID), color, listener) ;
			return this ;
		}
		
		public AlertDialog.Builder setOnCancelListener(DialogInterface.OnCancelListener onCancelListener) {
			throwIfCreated() ;
			mAlertDialog.setOnCancelListener(onCancelListener) ;
			return this ;
		}
		
		public AlertDialog.Builder setOnDismissListener( DialogInterface.OnDismissListener onDismissListener ) {
			throwIfCreated() ;
			mAlertDialog.setOnDismissListener(onDismissListener) ;
			return this ;
		}
		
		
		public AlertDialog.Builder setFilterableContentColor( int color ) {
			throwIfCreated() ;
			mAlertDialog.setFilterableContentColor(color) ;
			return this ;
		}
		
		public AlertDialog.Builder setInset( int pixels ) {
			throwIfCreated() ;
			mAlertDialog.setInset(pixels) ;
			return this ;
		}
		
		public AlertDialog.Builder setInset( int horizPixels, int vertPixels ) {
			throwIfCreated() ;
			mAlertDialog.setInset(horizPixels, vertPixels) ;
			return this ;
		}
		
		
		public AlertDialog.Builder setTitle(CharSequence title) {
			throwIfCreated() ;
			mAlertDialog.setTitle( title ) ;
			return this ;
		}
		
		public AlertDialog.Builder setTitle(int resID) {
			throwIfCreated() ;
			mAlertDialog.setTitle( resID ) ;
			return this ;
		}
		
		public AlertDialog.Builder setView( View v ) {
			throwIfCreated() ;
			mAlertDialog.setContentView(v) ;
			return this ;
		}
		
		public AlertDialog.Builder setView( int layout ) {
			throwIfCreated() ;
			mAlertDialog.setContentView(layout) ;
			return this ;
		}
		
		
		
		public AlertDialog.Builder setBackground( Drawable d ) {
			throwIfCreated() ;
			VersionSafe.setBackground(mAlertDialog.getContentView(), d) ;
			return this ;
		}
	}
}
