package com.peaceray.quantro.utils;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;


/**
 * This class provides a lightweight method of monitoring the state
 * of our Wifi connection.  Instances may be directly queried for Wifi
 * state and address; in addition, instances are Runnable, and may be
 * run directly or passed to a Handler.  When run, we guarantee that the
 * Listener method will be called exactly once (if a Listener is set).
 * 
 * Its direct-call methods provide a thin wrapper around WifiManager methods;
 * its main purpose is to be run on a handler.  In fact, if you have no
 * need to 'run' an instance, the static methods work just as well.
 * 
 * If you desire to frequently poll the Wifi state, then place the WifiMonitor
 * instance on a Handler, and post it again (delayed) within the Listener
 * callback.
 * 
 * @author Jake
 *
 */
public class WifiMonitor implements Runnable {
	
	public interface Listener {
		
		public void wml_hasWifiIpAddress( boolean hasIP, int ip ) ;
		
	}
	
	WeakReference<Context> mwrContext ;
	WeakReference<Listener> mwrListener ;
	
	public WifiMonitor( Context context, Listener listener ) {
		if ( context == null )
			throw new NullPointerException("Given null context") ;
		if ( listener == null )
			throw new NullPointerException("Given null listener") ;
		mwrContext = new WeakReference<Context>(context) ;
		mwrListener = new WeakReference<Listener>(listener) ;
	}
	
	/**
	 * Returns the Wifi IP address as an integer (if we are on Wifi);
	 * returns '0' if not available or not currently on a Wifi connection.
	 * @return
	 */
	synchronized public int getWifiIpAddress() {
		Context c = mwrContext.get() ;
		return WifiMonitor.getWifiIpAddress(c) ;
	}
	
	
	synchronized public static int getWifiIpAddress( Context context ) {
		if ( context == null )
			return 0 ;
		WifiManager manager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		
		WifiInfo wifiInfo = manager.getConnectionInfo();
		return wifiInfo.getIpAddress();	
	}
	
	
	synchronized public static String ipAddressToString( int ip ) {
		return String.format(
				"%d.%d.%d.%d",
				(ip & 0xff),
				(ip >> 8 & 0xff),
				(ip >> 16 & 0xff),
				(ip >> 24 & 0xff));
	}
	
	synchronized public static int ipAddressFromString( String hostAddress ) {
		System.err.println("" + hostAddress) ;
		String [] ipBytes = hostAddress.split("\\p{Punct}") ;
		// split on punctuation, not periods, since "." is a control
		// character in regex.
		int ip = Integer.parseInt(ipBytes[0])
				+ (Integer.parseInt(ipBytes[1]) << 8 )
				+ (Integer.parseInt(ipBytes[2]) << 16 )
				+ (Integer.parseInt(ipBytes[3]) << 24 ) ;
		return ip ;
	}
	
	/**
	 * Returns 'true' if the string, when passed to ipAddressFromString, will
	 * return an ip address.
	 * 
	 * @param hostAddress
	 * @return
	 */
	synchronized public static boolean isIPAddress( String hostAddress ) {
		if ( hostAddress == null )
			return false ;
		String [] ipBytes = hostAddress.split("\\p{Punct}") ;
		if ( ipBytes.length != 4 )
			return false ;
		try {
			for ( int i = 0; i < 4; i++ ) {
				int val = Integer.parseInt(ipBytes[i]) ;
				if ( val < 0 || 255 < val )
					return false ;
			}
		} catch( NumberFormatException e ) {
			return false ;
		}
		
		return true ;
	}

	@Override
	synchronized public void run() {
		if ( mwrListener.get() == null )
			return ;
		
		int ip = getWifiIpAddress() ;
		
		Listener listener = mwrListener.get() ;
		if ( listener != null )
			listener.wml_hasWifiIpAddress( ip != 0, ip) ;
	}

}
