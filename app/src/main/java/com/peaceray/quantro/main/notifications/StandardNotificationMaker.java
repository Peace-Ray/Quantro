package com.peaceray.quantro.main.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;


/**
 * A version of QuantroNotificationMaker meant to suffice when API 11 (Honeycomb)
 * is not available.
 * 
 * This method operates by constructing a wasted Notification object that holds all
 * updates set.  To fit the description of getNotification(), which returns a NEW
 * Notification object when called, we implement this by creating a new Notification
 * and copying fields.
 * 
 * @author Jake
 *
 */
public class StandardNotificationMaker extends QuantroNotificationMaker {

	Context mContext ;
	CharSequence mContentTitle ;
	CharSequence mContentText ;
	PendingIntent mContentIntent ;

	NotificationCompat.Builder mBuilder;
	int mFlags = 0;
	boolean mDidGet ;
	
	StandardNotificationMaker( Context context ) {
		mContext = context ;
		mContentTitle = null ;
		mContentText = null ;
		mContentIntent = null ;

		mBuilder = new NotificationCompat.Builder(mContext);

		mDidGet = false ;
	}
	
	
	/**
	 * Combine all of the options that have been set and return a new Notification object.
	 * @return
	 */
	public Notification getNotification() {
		mDidGet = true;

		Notification not = mBuilder.build();
		not.flags = mFlags;
		return not;
	}
	
	private void setFlag( int flag, boolean on ) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		// first unset the flag using bitwise x AND NOT flag.
		mFlags = mFlags & (~flag) ;
		// now set, if on.
		if ( on )
			mFlags = mFlags | flag ;
	}
	
	/**
	 * Setting this flag will make it so the notification is automatically canceled
	 * when the user clicks it in the panel.
	 */
	public QuantroNotificationMaker setAutoCancel(boolean autoCancel) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		setFlag( Notification.FLAG_AUTO_CANCEL, autoCancel ) ;
		return this ;
	}
	
	
	/**
	 * Supply a custom RemoteViews to use instead of the standard one.
	 * @param views
	 * @return
	 */
	public QuantroNotificationMaker setContent(RemoteViews views) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setContent(views);
		return this ;
	}
	
	/**
	 * Set the large text at the right-hand side of the notification.
	 * @param info
	 * @return
	 */
	public QuantroNotificationMaker setContentInfo(CharSequence info) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		// No effect
		return this ;
	}
	
	/**
	 * Supply a PendingIntent to send when the notification is clicked.
	 * @param intent
	 * @return
	 */
	public QuantroNotificationMaker setContentIntent(PendingIntent intent) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setContentIntent(intent) ;
		return this ;
	}
	
	/**
	 * Set the text (second row) of the notification, in a standard notification.
	 * @param text
	 * @return
	 */
	public QuantroNotificationMaker setContentText(CharSequence text) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setContentText(text) ;
		return this ;
	}
	
	/**
	 * Set the title (first row) of the notification, in a standard notification.
	 * @param title
	 * @return
	 */
	public QuantroNotificationMaker setContentTitle(CharSequence title) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setContentTitle(title) ;
		return this ;
	}
	
	/**
	 * Set the default notification options that will be used.
	 * @param defaults
	 * @return
	 */
	public QuantroNotificationMaker setDefaults(int defaults) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setDefaults(defaults) ;
		return this ;
	}
	
	/**
	 * Supply a PendingIntent to send when the notification is cleared by the user
	 * directly from the notification panel.
	 * @param intent
	 * @return
	 */
	public QuantroNotificationMaker setDeleteIntent(PendingIntent intent) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setDeleteIntent(intent) ;
		return this ;
	}
	
	/**
	 * An intent to launch instead of posting the notification to the status bar. 
	 * @param intent
	 * @param highPriority
	 * @return
	 */
	public QuantroNotificationMaker setFullScreenIntent(PendingIntent intent, boolean highPriority) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		// nothing; we aren't sure we're in API 9.
		return this ;
	}
	
	/**
	 * Set the large icon that is shown in the ticker and notification.
	 * @param icon
	 * @return
	 */
	public QuantroNotificationMaker setLargeIcon(Bitmap icon) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		// nothing; no support for this at our API level.
		return this ;
	}
	
	/**
	 * Set the argb value that you would like the LED on the device to
	 * blink, as well as the rate.
	 * 
	 * @param argb
	 * @param onMs
	 * @param offMs
	 * @return
	 */
	public QuantroNotificationMaker setLights(int argb, int onMs, int offMs) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setLights(argb, onMs, offMs) ;
		return this ;
	}
	
	/**
	 * Set the large number at the right-hand side of the notification.
	 * @param number
	 * @return
	 */
	public QuantroNotificationMaker setNumber(int number) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setNumber(number) ;
		return this ;
	}
	
	/**
	 * Set whether this is an ongoing notification.
	 * @param ongoing
	 * @return
	 */
	public QuantroNotificationMaker setOngoing(boolean ongoing) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		setFlag( Notification.FLAG_ONGOING_EVENT, ongoing ) ;
		return this ;
	}
	
	/**
	 * Set this flag if you would only like the sound, vibrate and ticker
	 * to be played if the notification is not already showing.
	 * @param onlyAlertOnce
	 * @return
	 */
	public QuantroNotificationMaker setOnlyAlertOnce(boolean onlyAlertOnce) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		setFlag( Notification.FLAG_ONLY_ALERT_ONCE, onlyAlertOnce ) ;
		return this ;
	}
	
	/**
	 * Set the progress this notification represents, which may be represented
	 * as a ProgressBar.
	 * @param max
	 * @param progress
	 * @param indeterminate
	 * @return
	 */
	public QuantroNotificationMaker setProgress(int max, int progress, boolean indeterminate) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		// no support
		return this ;
	}
	
	/**
	 * A variant of setSmallIcon(int) that takes an additional level parameter
	 * for when the icon is a LevelListDrawable.
	 * @param icon
	 * @param level
	 * @return
	 */
	public QuantroNotificationMaker setSmallIcon(int icon, int level) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setSmallIcon(icon, level) ;
		return this ;
	}
	
	/**
	 * Set the small icon to use in the notification layouts.
	 * @param icon
	 * @return
	 */
	public QuantroNotificationMaker setSmallIcon(int icon) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setSmallIcon(icon) ;
		return this ;
	}
	
	/**
	 * Set the sound to play.
	 * @param sound
	 * @return
	 */
	public QuantroNotificationMaker setSound(Uri sound) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setSound(sound) ;
		return this ;
	}
	
	/**
	 * Set the sound to play.
	 * @param sound
	 * @param streamType
	 * @return
	 */
	public QuantroNotificationMaker setSound(Uri sound, int streamType) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setSound(sound, AudioManager.STREAM_NOTIFICATION) ;
		return this ;
	}
	
	/**
	 * Set the text that is displayed in the status bar when the notification first arrives, and
	 * also a RemoteViews object that may be displayed instead on some devices.
	 * @param tickerText
	 * @param views
	 * @return
	 */
	public QuantroNotificationMaker setTicker(CharSequence tickerText, RemoteViews views) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setTicker(tickerText) ;
		// no views; they are in API 11.
		return this ;
	}
	
	/**
	 * Set the text that is displayed in the status bar when the
	 * notification first arrives.
	 * @param tickerText
	 * @return
	 */
	public QuantroNotificationMaker setTicker(CharSequence tickerText) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setTicker(tickerText) ;
		return this ;
	}
	
	/**
	 * Set the vibration pattern to use.
	 * @param pattern
	 * @return
	 */
	public QuantroNotificationMaker setVibrate(long[] pattern) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setVibrate(pattern) ;
		return this ;
	}
	
	/**
	 * Set the time that the event occurred.
	 * @param when
	 * @return
	 */
	public QuantroNotificationMaker setWhen(long when) {
		if ( mDidGet )
			throw new IllegalStateException("Once a Notification has been made with getNotification, no mutators may be used.") ;
		mBuilder.setWhen(when) ;
		return this ;
	}
	
}
