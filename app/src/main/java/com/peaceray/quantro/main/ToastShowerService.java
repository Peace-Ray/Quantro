package com.peaceray.quantro.main;

import java.lang.reflect.Field;

import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.dialog.ToastBuilder;
import com.peaceray.quantro.utils.Analytics;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ToastShowerService extends Service {
	
	private final static String TAG = "ToastShowerService" ;
	
	// INTENT MUST HAVE TOAST!
	public final static String INTENT_EXTRA_TOAST_BUILDER = "com.peaceray.quantro.main.ToastShowerService" ;
	
	@Override
	public IBinder onBind(Intent intent) {
		// This is not a bound service.  Generally,
		// if an Activity wants to show a toast, it should show it itself.
		return null;
	}

	Handler handler ;
	private static final int ANDROID_MESSAGE_TYPE_STOP_SELF = 0 ;
	private static final long ANDROID_MESSAGE_DELAY_STOP_SELF = 60000 ;	// if no toasts in 1 minute, stop the shower service.
		// we use this to stop ourselves after a reasonable delay;
		// this delay is reset each time we receive a toast to display.
	
	private boolean mAnalyticsInSession = false ;
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate") ;
		super.onCreate() ;
		
		// Nows a good time to start the handler.
		handler = new Handler() {
			public void handleMessage(android.os.Message msg) {
				int type = msg.what ;
				
				switch(type) {
				case ANDROID_MESSAGE_TYPE_STOP_SELF:
					Log.d(TAG, "Stopping self") ;
					stopSelf() ;
					break ;
				}
			}
		} ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) ) {
			mAnalyticsInSession = true ;
			Analytics.startSession(this) ;
		}
	}
	
	public void onDestroy() {
		super.onDestroy() ;
		
		if ( mAnalyticsInSession )
			Analytics.stopSession(this) ;
	}
	
	
	// This is the old onStart method that will be called on the pre-2.0
	// platform.  On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "onStart") ;
	    handleCommand(intent);
	}

	// This is an "override" method for v. 2.1 and greater.
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand") ;
	    handleCommand(intent);
	    // We don't care if this service stops, so return START_NOT_STICKY.
	    try {
	    	Class<?> c = this.getClass();
	    	Field f = c.getField("START_NOT_STICKY") ;
	    	return f.getInt(null) ;
	    } catch (Exception e) { 
	    	return Service.START_STICKY_COMPATIBILITY ;		// shouldn't ever happen
	    }
	}
	
	
	/**
	 * Cancels any pending stopping of the service,
	 * displays a toast generated by the ToastBuilder included as
	 * an extra, and launches a pending intent 
	 */
	private void handleCommand(Intent intent) {
		if ( intent.hasExtra(INTENT_EXTRA_TOAST_BUILDER) ) {
			// Cancel any pending stops - we apply the full
			// delay from the most recent Toast, which is right now.
			handler.removeMessages(ANDROID_MESSAGE_TYPE_STOP_SELF) ;
			
			// Retrieve the ToastBuilder, build the toast, and show it.
			Log.d(TAG, "showing toast!") ;
			ToastBuilder tb = (ToastBuilder) intent.getSerializableExtra(INTENT_EXTRA_TOAST_BUILDER) ;
			tb.build(this).show() ;
			
			// Launch a delayed message to stop this service.
			handler.sendMessageDelayed(
					handler.obtainMessage(ANDROID_MESSAGE_TYPE_STOP_SELF),
					ANDROID_MESSAGE_DELAY_STOP_SELF) ;
		}
		else
			Log.d(TAG, "handleCommand: has no TOAST_BUILDER extra.") ;
	}
	
}
