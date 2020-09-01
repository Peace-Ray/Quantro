package com.peaceray.quantro.utils;

import android.os.Build;

public class DeviceModel {

	public enum Name {
		/**
		 * The "Nexus S" model.
		 */
		NEXUS_S
	}
	
	
	public static boolean is( Name name ) {
		return Build.MODEL.equalsIgnoreCase( toString( name ) ) ;
	}
	
	public static String toString( Name name ) {
		switch( name ) {
		case NEXUS_S:
			return "Nexus S" ;
		}
		
		return null ;
	}
	
}
