package com.peaceray.quantro;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.utils.nfc.NfcAdapter;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


public class HtmlViewerActivity extends QuantroActivity {
	
	private static final String TAG = "HtmlViewerActivity" ;
	
	// Input comes from either URL or a file path.
	public static final String INTENT_EXTRA_URL = "com.peaceray.quantro.HtmlViewerActivity.INTENT_EXTRA_URL" ;
	public static final String INTENT_EXTRA_LOAD_PATH_RELATIVE_TO = "com.peaceray.quantro.HtmlViewerActivity.INTENT_EXTRA_PATH_ABSOLUTE_PORTION" ;
	public static final String INTENT_EXTRA_ASSET_PATH = "com.peaceray.quantro.HtmlViewActivity.INTENT_EXTRA_ASSET_PATH" ;
	public static final String INTENT_EXTRA_PATH = "com.peaceray.quantro.HtmlViewActivity.INTENT_EXTRA_PATH_RELATIVE_PORTION" ;
	
	String mURL = null ;
	String mPathRelativeTo = "file://" ;		// the portion of the path at which all HTML references are rooted
	String mPath = null ;
	String mAssetPath = null ;
	
	WebView mWebView ;
	
	Handler mHandler ;
	
	DialogManager mDialogManager ;
	
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState) ;
		setupQuantroActivity( QUANTRO_ACTIVITY_MENU, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
		Log.d(TAG, "onCreate") ;
		
		// Force portrait layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;
		
		Intent i = getIntent() ;
		
		if ( i.hasExtra(INTENT_EXTRA_URL) ) 
			mURL = i.getStringExtra(INTENT_EXTRA_URL) ;
		if ( i.hasExtra(INTENT_EXTRA_PATH ) )
			mPath = i.getStringExtra(INTENT_EXTRA_PATH) ;
		if ( i.hasExtra(INTENT_EXTRA_LOAD_PATH_RELATIVE_TO) ) {
			mPathRelativeTo = "file://" + i.getStringExtra(INTENT_EXTRA_LOAD_PATH_RELATIVE_TO) ;
			if ( !mPathRelativeTo.substring(mPathRelativeTo.length() -1, mPathRelativeTo.length()).equals("/") )
				mPathRelativeTo = mPathRelativeTo + "/" ;
		}
		if ( i.hasExtra(INTENT_EXTRA_ASSET_PATH ) )
			mAssetPath = i.getStringExtra(INTENT_EXTRA_ASSET_PATH) ;
		
		
		// LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
        setContentView(R.layout.html_viewer) ;
		
		// get the webview and set its content
		mHandler = new Handler() ;
		mWebView = (WebView)findViewById( R.id.html_viewer_web_view ) ;
		// anchor links cause a problem.  Is this a fix?
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				int found = failingUrl.indexOf('#');
				Log.d(TAG, "errorCode: " + errorCode + " found = " + found) ;
		        if (found > 0) {
		            view.loadUrl(failingUrl.substring(0, found));
		            mHandler.postDelayed(new LoadURLRunnable(view, failingUrl), 400) ;
		        } else
		        	super.onReceivedError(view, errorCode, description, failingUrl) ;
			}
			
			@Override
		    public boolean shouldOverrideUrlLoading(WebView view, String url) {
				String lowerURL = url.toLowerCase() ;
				if ( url.contains("http://") || url.contains("https://") ) {
					// send to a browser activity
					Uri uri = Uri.parse(url);
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent); 
					return true ;
				} else if ( url.substring(0, 7).equals("mailto:") ) {
					String address = url.substring(7) ;
					Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
					emailIntent .setType("plain/text");
					emailIntent .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{address});
					startActivity(Intent.createChooser(emailIntent, "Send mail..."));
					return true ;
				} else if ( lowerURL.contains("activity://") ) {
					String activityName = lowerURL.substring("activity://".length()) ;
					if ( activityName.equals("quantropreferences") ) {
						Intent intent = new Intent( HtmlViewerActivity.this, QuantroPreferences.class ) ;
				    	intent.setAction( Intent.ACTION_MAIN ) ;
				    	
				    	startActivity(intent) ;
				    	return true ;
					} else {
						Toast.makeText(HtmlViewerActivity.this, "Don't recognize " + url, Toast.LENGTH_SHORT).show() ;
					}
				}
		        view.loadUrl(url);
		        return true;
		    }

		}) ;
		
		boolean loaded = false ;
		if ( mURL != null ) {
			Log.d(TAG, "setting URL to " + mURL) ;
			mWebView.loadUrl(mURL) ;
			loaded = true ;
		}
		else if ( mAssetPath != null ) {
			/*
			Log.d(TAG, "loading asset " + mAssetPath) ;
			String data = assetToString( mAssetPath ) ;
			if ( data != null ) {
				mWebView.loadDataWithBaseURL("file:///android_asset/", data, "text/html", "utf-8", null) ;
				loaded = true ;
			}
			*/
			mURL = "file:///android_asset/" + mAssetPath ;
			Log.d(TAG, "loading asset at URL " + mURL ) ;
			mWebView.loadUrl( mURL ) ;
			loaded = true ;
		}
		else {
			try { 
				Log.d(TAG, "loading file " + mPath) ;
				Log.d(TAG, "references relative to " + mPathRelativeTo) ;
				String data = fileToString( mPath ) ;
				mWebView.loadDataWithBaseURL(mPathRelativeTo, data, "text/html", "utf-8", null) ;
				loaded = true ;
			} catch(IOException e) {
				// whelp, we couldn't open the file.  Not much to do here.
			}
		}
		
		
		mDialogManager = new DialogManager(this) ;
		
		if ( !loaded )
			finish() ;

        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}
	
	
	@Override
	public synchronized void onResume() {
		super.onResume() ;
		
		mDialogManager.revealDialogs() ;
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause(); ;
		
		mDialogManager.hideDialogs() ;
	}
	
	
	
	
	
	/*
	 * *************************************************************************
	 * 
	 * MENU CALLBACKS
	 * 
	 * For creating, displaying, and processing touches to an options menu.
	 * 
	 * *************************************************************************
	 */
    
    //
    // SETTINGS ACTIVITIES /////////////////////////////////////////////////////
    //
    
    protected void startQuantroPreferencesActivity() {
    	// launch settings
		Intent intent = new Intent( this, QuantroPreferences.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	startActivity(intent) ;
    	//Log.d(TAG, "onOptionsItemSelected bracket OUT") ;
    }
    
    
	/*
	 * *************************************************************************
	 * 
	 * DIALOGS
	 * 
	 * *************************************************************************
	 */
    
	
	
	private String assetToString( String assetPath ) {
		try {
			int len ;
			char[] chr = new char[4096] ;
			final StringBuffer buffer = new StringBuffer();
			BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open(assetPath)));
			while ( (len = br.read(chr))  > 0 ) {
				buffer.append(chr, 0, len) ;
			}
			return buffer.toString() ;
		} catch( Exception e ) {
			e.printStackTrace() ;
			return null ;
		}
	}
	
	private String fileToString( String filename ) throws IOException {
		int len;
	    char[] chr = new char[4096];
	    final StringBuffer buffer = new StringBuffer();
	    final FileReader reader = new FileReader(filename);
	    try {
	        while ((len = reader.read(chr)) > 0) {
	            buffer.append(chr, 0, len);
	        }
	    } finally {
	        reader.close();
	    }
	    return buffer.toString();
	}
	
	
	private class LoadURLRunnable implements Runnable {
		WebView mWV ;
		String mUrl ;
		
		public LoadURLRunnable( WebView wv, String url ) {
			mWV = wv ;
			mUrl = url ;
		}
		
		public void run() {
			Log.d(TAG, "LoadURLRunnable: " + mUrl) ;
			mWV.loadUrl(mUrl) ;
		}
	}
}
