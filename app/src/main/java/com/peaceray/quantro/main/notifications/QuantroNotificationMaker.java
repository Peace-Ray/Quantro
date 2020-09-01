package com.peaceray.quantro.main.notifications;

import com.peaceray.quantro.utils.VersionCapabilities;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * This is basically a re-implementation of the Notification.Builder
 * class, provided by API level 11.  However, as an abstract class,
 * it allows for implementations that work even under API 1, as the
 * Notification class itself is.
 * 
 * 
 * @author Jake
 *
 */
public abstract class QuantroNotificationMaker {
	
	
	public static QuantroNotificationMaker getNew( Context context ) {
		if ( VersionCapabilities.supportsNotificationBuilder() )
			return new HoneycombNotificationMaker(context) ;
		return new StandardNotificationMaker(context) ;
	}
	
	
	/**
	 * Combine all of the options that have been set and return a new Notification object.
	 * @return
	 */
	public abstract Notification getNotification() ;
	
	/**
	 * Setting this flag will make it so the notification is automatically canceled
	 * when the user clicks it in the panel.
	 */
	public abstract QuantroNotificationMaker setAutoCancel(boolean autoCancel) ;
	
	
	/**
	 * Supply a custom RemoteViews to use instead of the standard one.
	 * @param views
	 * @return
	 */
	public abstract QuantroNotificationMaker setContent(RemoteViews views) ;
	
	/**
	 * Set the large text at the right-hand side of the notification.
	 * @param info
	 * @return
	 */
	public abstract QuantroNotificationMaker setContentInfo(CharSequence info) ;
	
	/**
	 * Supply a PendingIntent to send when the notification is clicked.
	 * @param intent
	 * @return
	 */
	public abstract QuantroNotificationMaker setContentIntent(PendingIntent intent) ;
	
	/**
	 * Set the text (second row) of the notification, in a standard notification.
	 * @param text
	 * @return
	 */
	public abstract QuantroNotificationMaker setContentText(CharSequence text) ;
	
	/**
	 * Set the title (first row) of the notification, in a standard notification.
	 * @param title
	 * @return
	 */
	public abstract QuantroNotificationMaker setContentTitle(CharSequence title) ;
	
	/**
	 * Set the default notification options that will be used.
	 * @param defaults
	 * @return
	 */
	public abstract QuantroNotificationMaker setDefaults(int defaults) ;
	
	/**
	 * Supply a PendingIntent to send when the notification is cleared by the user
	 * directly from the notification panel.
	 * @param intent
	 * @return
	 */
	public abstract QuantroNotificationMaker setDeleteIntent(PendingIntent intent) ;
	
	/**
	 * An intent to launch instead of posting the notification to the status bar. 
	 * @param intent
	 * @param highPriority
	 * @return
	 */
	public abstract QuantroNotificationMaker setFullScreenIntent(PendingIntent intent, boolean highPriority) ;
	
	/**
	 * Set the large icon that is shown in the ticker and notification.
	 * @param icon
	 * @return
	 */
	public abstract QuantroNotificationMaker setLargeIcon(Bitmap icon) ;
	
	/**
	 * Set the argb value that you would like the LED on the device to
	 * blink, as well as the rate.
	 * 
	 * @param argb
	 * @param onMs
	 * @param offMs
	 * @return
	 */
	public abstract QuantroNotificationMaker setLights(int argb, int onMs, int offMs) ;
	
	/**
	 * Set the large number at the right-hand side of the notification.
	 * @param number
	 * @return
	 */
	public abstract QuantroNotificationMaker setNumber(int number) ;
	
	/**
	 * Set whether this is an ongoing notification.
	 * @param ongoing
	 * @return
	 */
	public abstract QuantroNotificationMaker setOngoing(boolean ongoing) ;
	
	/**
	 * Set this flag if you would only like the sound, vibrate and ticker
	 * to be played if the notification is not already showing.
	 * @param onlyAlertOnce
	 * @return
	 */
	public abstract QuantroNotificationMaker setOnlyAlertOnce(boolean onlyAlertOnce) ;
	
	/**
	 * Set the progress this notification represents, which may be represented
	 * as a ProgressBar.
	 * @param max
	 * @param progress
	 * @param indeterminate
	 * @return
	 */
	public abstract QuantroNotificationMaker setProgress(int max, int progress, boolean indeterminate) ;
	
	/**
	 * A variant of setSmallIcon(int) that takes an additional level parameter
	 * for when the icon is a LevelListDrawable.
	 * @param icon
	 * @param level
	 * @return
	 */
	public abstract QuantroNotificationMaker setSmallIcon(int icon, int level) ;
	
	/**
	 * Set the small icon to use in the notification layouts.
	 * @param icon
	 * @return
	 */
	public abstract QuantroNotificationMaker setSmallIcon(int icon) ;
	
	/**
	 * Set the sound to play.
	 * @param sound
	 * @return
	 */
	public abstract QuantroNotificationMaker setSound(Uri sound) ;
	
	/**
	 * Set the sound to play.
	 * @param sound
	 * @param streamType
	 * @return
	 */
	public abstract QuantroNotificationMaker setSound(Uri sound, int streamType) ;
	
	/**
	 * Set the text that is displayed in the status bar when the notification first arrives, and
	 * also a RemoteViews object that may be displayed instead on some devices.
	 * @param tickerText
	 * @param views
	 * @return
	 */
	public abstract QuantroNotificationMaker setTicker(CharSequence tickerText, RemoteViews views) ;
	
	/**
	 * Set the text that is displayed in the status bar when the
	 * notification first arrives.
	 * @param tickerText
	 * @return
	 */
	public abstract QuantroNotificationMaker setTicker(CharSequence tickerText) ;
	
	/**
	 * Set the vibration pattern to use.
	 * @param pattern
	 * @return
	 */
	public abstract QuantroNotificationMaker setVibrate(long[] pattern) ;
	
	/**
	 * Set the time that the event occurred.
	 * @param when
	 * @return
	 */
	public abstract QuantroNotificationMaker setWhen(long when) ;
}
