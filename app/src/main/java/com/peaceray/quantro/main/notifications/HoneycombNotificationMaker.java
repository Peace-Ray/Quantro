package com.peaceray.quantro.main.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

/**
 * This class represents the API 11 (Honeycomb) Notification maker.  Because Honeycomb
 * supports Notification.Builder, this is a paper-thin wrapper around an instance
 * of that class.
 * 
 * @author Jake
 *
 */
public class HoneycombNotificationMaker extends QuantroNotificationMaker {
	
	Notification.Builder mBuilder ;
	
	HoneycombNotificationMaker( Context context ) {
		mBuilder = new Notification.Builder(context) ;
	}

	@Override
	public Notification getNotification() {
		return mBuilder.getNotification() ;
	}

	@Override
	public QuantroNotificationMaker setAutoCancel(boolean autoCancel) {
		mBuilder.setAutoCancel(autoCancel) ;
		return this ;
 	}

	@Override
	public QuantroNotificationMaker setContent(RemoteViews views) {
		mBuilder.setContent(views) ;
		return this ; 
	}

	@Override
	public QuantroNotificationMaker setContentInfo(CharSequence info) {
		mBuilder.setContentInfo(info) ;
		return this ; 
	}

	@Override
	public QuantroNotificationMaker setContentIntent(PendingIntent intent) {
		mBuilder.setContentIntent(intent) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setContentText(CharSequence text) {
		mBuilder.setContentText(text) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setContentTitle(CharSequence title) {
		mBuilder.setContentTitle(title) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setDefaults(int defaults) {
		mBuilder.setDefaults(defaults) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setDeleteIntent(PendingIntent intent) {
		mBuilder.setDeleteIntent(intent) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setFullScreenIntent(PendingIntent intent,
			boolean highPriority) {
		mBuilder.setFullScreenIntent(intent, highPriority) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setLargeIcon(Bitmap icon) {
		mBuilder.setLargeIcon(icon) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setLights(int argb, int onMs, int offMs) {
		mBuilder.setLights(argb, onMs, offMs) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setNumber(int number) {
		mBuilder.setNumber(number) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setOngoing(boolean ongoing) {
		mBuilder.setOngoing(ongoing) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setOnlyAlertOnce(boolean onlyAlertOnce) {
		mBuilder.setOnlyAlertOnce(onlyAlertOnce) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setProgress(int max, int progress,
			boolean indeterminate) {
		// NOTE: API 11 does not support this.
		return this ;
	}

	@Override
	public QuantroNotificationMaker setSmallIcon(int icon, int level) {
		mBuilder.setSmallIcon(icon, level) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setSmallIcon(int icon) {
		mBuilder.setSmallIcon(icon) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setSound(Uri sound) {
		mBuilder.setSound(sound) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setSound(Uri sound, int streamType) {
		mBuilder.setSound(sound, streamType) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setTicker(CharSequence tickerText,
			RemoteViews views) {
		mBuilder.setTicker( tickerText, views ) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setTicker(CharSequence tickerText) {
		mBuilder.setTicker(tickerText) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setVibrate(long[] pattern) {
		mBuilder.setVibrate(pattern) ;
		return this ;
	}

	@Override
	public QuantroNotificationMaker setWhen(long when) {
		mBuilder.setWhen(when) ;
		return this ;
	}

}
