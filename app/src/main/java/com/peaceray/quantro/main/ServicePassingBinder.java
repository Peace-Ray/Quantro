package com.peaceray.quantro.main;

import android.app.Service;
import android.os.Binder;

public class ServicePassingBinder extends Binder {
	Service service ;

	public ServicePassingBinder( Service s ) {
		super() ;
		service = s ;
	}
	
	public Service getService() {
		return service ;
	}
}
