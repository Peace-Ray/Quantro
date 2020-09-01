package com.peaceray.quantro.main;

import java.util.Random;

import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;

public class RingerModeChangedBroadcastReceiver extends BroadcastReceiver {

	private static final String TAG = "Quantro" ;
	private static final String RINGER_MODE_CHANGED = AudioManager.RINGER_MODE_CHANGED_ACTION ;

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.d(TAG, "onReceive") ;
		// Hopefully with such a short operation, we don't need a WakeLock?
		// Besides, if this happens immediately after boot, probably the
		// system will be awake for the next few seconds...
		String action = intent.getAction();
		if ( action.equals(RINGER_MODE_CHANGED) ) {
			// start up the refresher.
			Log.d(TAG, "Ringer mode changed: muting or unmuting Quantro") ;
			Intent muteIntent = new Intent( context, RingerModeChangedService.class ) ;
        	context.startService(muteIntent) ;
		}
	}

}
