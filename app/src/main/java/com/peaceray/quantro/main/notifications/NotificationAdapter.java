package com.peaceray.quantro.main.notifications;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.util.Log;

/**
 * We wrap Notification methods in a NotificationAdapter.  The purpose
 * of this Adapter is to ensure that Notifications work regardless of the Android
 * API in use.
 * 
 * @author Jake
 *
 */
public class NotificationAdapter {
	
	private static final String TAG = "NotificationAdapter" ;
	
	private Context context ;
	private int foregroundNotificationID ;
	private boolean foregroundDisplayed ;
	
	
	/*
	 * *************************************************************************
	 * 
	 * FOREGROUND SERVICES API
	 * 
	 * The method calls for establishing a foreground service (needed for MP
	 * games, so we don't lose the server connection when the user presses 
	 * "Home."
	 * 
	 * Unfortunately, the method of establishing foreground / background 
	 * status changed in Android 2.0.  To be backwards compatible with 1.5
	 * and 1.6, we use these wrapper methods, copied from
	 * 
	 * http://developer.android.com/reference/android/app/Service.html#startForeground%28int,%20android.app.Notification%29
	 * 
	 * The methods below make no assumptions about the reasons for needing
	 * foreground / background status.  All they do is provide a means to safely
	 * set foreground and background without worrying about API version number.
	 * 
	 * (WHY is this not a standard part of the API?)
	 * 
	 * *************************************************************************
	 */
	
	
	private static final Class<?>[] mSetForegroundSignature = new Class[] {
	    boolean.class};
	private static final Class<?>[] mStartForegroundSignature = new Class[] {
	    int.class, Notification.class};
	private static final Class<?>[] mStopForegroundSignature = new Class[] {
	    boolean.class};

	private NotificationManager mNM;
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mSetForegroundArgs = new Object[1];
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private boolean notificationMethodsCreated = false ;

	
	synchronized void invokeMethod(Method method, Object[] args) {
	    try {
	        method.invoke(context, args);
	    } catch (InvocationTargetException e) {
	        // Should not happen.
	        Log.w(TAG, "Unable to invoke method", e);
	    } catch (IllegalAccessException e) {
	        // Should not happen.
	        Log.w(TAG, "Unable to invoke method", e);
	    }
	}
	
	public NotificationAdapter(Context context) {
		this.context = context ;
		this.foregroundDisplayed = false ;
		
		createNotificationMethods() ;
	}
	
	public void setContext( Context context ) {
		this.context = context ;
	}
	
	public synchronized NotificationManager getNotificationManager() {
		if ( !notificationMethodsCreated )
			createNotificationMethods() ;
		
		return mNM ;
	}
	
	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	public synchronized void startForegroundCompat(int id, Notification notification) {
		if ( !notificationMethodsCreated )
			createNotificationMethods() ;
		
		// Note the display of this ID.
		this.foregroundNotificationID = id ;
		this.foregroundDisplayed = true ;
		
	    // If we have the new startForeground API, then use it.
	    if (mStartForeground != null) {
	        mStartForegroundArgs[0] = Integer.valueOf(id);
	        mStartForegroundArgs[1] = notification;
	        invokeMethod(mStartForeground, mStartForegroundArgs);
	        return;
	    }

	    // Fall back on the old API.
	    mSetForegroundArgs[0] = Boolean.TRUE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
	    mNM.notify(id, notification);
	}

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	public synchronized void stopForegroundCompat(int id) {
		if ( !notificationMethodsCreated )
			createNotificationMethods() ;
		
		if ( this.foregroundNotificationID == id )
			this.foregroundDisplayed = false ;
		
	    // If we have the new stopForeground API, then use it.
	    if (mStopForeground != null) {
	        mStopForegroundArgs[0] = Boolean.TRUE;
	        try {
	            mStopForeground.invoke(context, mStopForegroundArgs);
	        } catch (InvocationTargetException e) {
	            // Should not happen.
	            Log.w("ApiDemos", "Unable to invoke stopForeground", e);
	        } catch (IllegalAccessException e) {
	            // Should not happen.
	            Log.w("ApiDemos", "Unable to invoke stopForeground", e);
	        }
	        return;
	    }

	    // Fall back on the old API.  Note to cancel BEFORE changing the
	    // foreground state, since we could be killed at that point.
	    mNM.cancel(id);
	    mSetForegroundArgs[0] = Boolean.FALSE;
	    invokeMethod(mSetForeground, mSetForegroundArgs);
	}

	public synchronized void createNotificationMethods() {
		if ( notificationMethodsCreated )
			return ;
		notificationMethodsCreated = true ;
	    mNM = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	    
	    // Foreground / background.  Only relevant if we are using a 'Service' context.
	    if ( Service.class.isInstance(context) ) {
	    	Log.d(TAG, "context is instance of service") ;
		    try {
		        mStartForeground = Service.class.getMethod("startForeground",
		                mStartForegroundSignature);
		        mStopForeground = Service.class.getMethod("stopForeground",
		                mStopForegroundSignature);
		        //Log.d(TAG, "success: methods set to " + mStartForeground + ", " + mStopForeground) ;
		        return ;
		    } catch (NoSuchMethodException e) {
		        // Running on an older platform.
		    	//Log.d(TAG, "exception: methods set to " + mStartForeground + ", " + mStopForeground) ;
		        mStartForeground = mStopForeground = null;
		    }
		    try {
		        mSetForeground = Service.class.getMethod("setForeground",
		                mSetForegroundSignature);
		        
		    } catch (NoSuchMethodException e) {
		    	//Log.d(TAG, "older system: methods set to " + mStartForeground + ", " + mStopForeground) ;
		        throw new IllegalStateException(
		                "OS doesn't have Service.startForeground OR Service.setForeground!");
		    }
	    }
	    else
	    	Log.d(TAG, "context " + context + " is NOT instance of service") ;
	}
	

	public synchronized void dismissForegroundNotification() {
		Log.d(TAG, "dismissAll") ;
	    
		// Run through our list, dismissing all of them.  Note that
		// each call to stop will remove an ID from our list.
		if ( this.foregroundDisplayed )
			this.stopForegroundCompat(this.foregroundNotificationID) ;
		this.foregroundDisplayed = false ;
	}
}
