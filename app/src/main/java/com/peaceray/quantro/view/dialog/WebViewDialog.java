package com.peaceray.quantro.view.dialog;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.peaceray.quantro.R;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;

public class WebViewDialog extends AlertDialog {
	
	WebView mWebView ;
	
	Handler mHandler ;
	Runnable mWebViewTransparentBackgroundRunnable ;
	
	public WebViewDialog(Context context) {
		super(context);
		
		// load a custom view
		setContentView(R.layout.dialog_webview) ;
		
		mHandler = new Handler() ;
		mWebViewTransparentBackgroundRunnable = new Runnable() {
			@Override
			public void run() {
				if ( mWebView != null ) {
					mWebView.setVisibility(View.VISIBLE) ;
					mWebView.setBackgroundColor(0x00) ;
					mWebView.forceLayout() ;
				}
			}
		} ;
	}
	

	public void setURL( String url ) {
		if ( mWebView != null ) {
			if ( url == null  )
				mWebView.setVisibility(View.GONE) ;
			else {
				mWebView.setVisibility(View.GONE); 
				mWebView.loadUrl(url) ;
				mHandler.postDelayed(mWebViewTransparentBackgroundRunnable, 300) ;
			}
		} else if ( url != null )
			throw new NullPointerException("Current content view does not have a WebView") ;
		
		mContent.requestLayout() ;
	}
	
	
	public void setWebData( String pathRelativeTo, String data ) {
		if ( mWebView != null ) {
			if ( data == null  )
				mWebView.setVisibility(View.GONE) ;
			else {
				mWebView.loadDataWithBaseURL(pathRelativeTo, data, "text/html", "utf-8", null) ;
				mWebView.setVisibility(View.VISIBLE) ;
				mHandler.postDelayed(mWebViewTransparentBackgroundRunnable, 300) ;
			}
		} else if ( data != null )
			throw new NullPointerException("Current content view does not have a WebView") ;
		
		mContent.requestLayout() ;
	}
	
	
	
	
	
	@Override
	/**
	 * A good candidate to override if you need access to custom content.
	 * @param content
	 */
	protected void getSubviews( ViewGroup content) {
		super.getSubviews(content) ;
		mWebView = (WebView) content.findViewById(R.id.dialog_content_webview) ;
		// disable hardware accel to prevent flickering (bug) in 4.1,
		// and possibly other 
		if ( mWebView != null )
		 	VersionSafe.disableHardwareAcceleration(mWebView) ;
	}
	
	
	public static class Builder extends AlertDialog.Builder {
		
		WebViewDialog mWebViewDialog ;
		
		public Builder( Context context ) {
			super( context, new WebViewDialog(context) ) ;
			mWebViewDialog = (WebViewDialog)mAlertDialog ;
		}
		
		
		@Override
		public WebViewDialog create() {
			return (WebViewDialog)super.create() ;
		}
		
		public WebViewDialog.Builder setURL( String url ) {
			throwIfCreated() ;
			mWebViewDialog.setURL(url) ;
			return this ;
		}
		
		public WebViewDialog.Builder setWebData( String pathRelativeTo, String data ) {
			throwIfCreated() ;
			mWebViewDialog.setWebData(pathRelativeTo, data) ;
			return this ;
		}
		
		
	}

}
