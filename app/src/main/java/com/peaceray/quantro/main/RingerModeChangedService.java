package com.peaceray.quantro.main;

import com.peaceray.quantro.QuantroApplication;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class RingerModeChangedService extends IntentService {
	
	private static final String NAME = "com.peaceray.quantro.main.RingerModeChangedService" ;
	
	private static final String LOCK_NAME_STATIC="com.peaceray.quantro.main.RingerModeChangedService";
	private static volatile PowerManager.WakeLock lockStatic=null;
	

	public RingerModeChangedService() {
		super(NAME) ;
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
			               						LOCK_NAME_STATIC);
			lockStatic.setReferenceCounted(true);
		}
	  
		return(lockStatic);
	}
	
	@Override
	protected void onHandleIntent(Intent arg0) {
		// wake lock!
		getLock(this).acquire();
		// Everything in a 'try' so we can 'finally' the hell out of this lock.
		try {
			// ringer mode changed.  Tell the SoundPool, if we have one.
			QuantroApplication app = ((QuantroApplication)getApplication()) ;
			if ( app != null )
				app.ringerDidChangeMode() ;
			
		} finally {
			getLock(this).release() ;
		}
	}

}
