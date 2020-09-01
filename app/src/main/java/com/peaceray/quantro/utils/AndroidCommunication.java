package com.peaceray.quantro.utils;

import java.io.IOException;
import java.net.InetAddress;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class AndroidCommunication {
	
	// Code borrowed from http://code.google.com/p/boxeeremote/wiki/AndroidUDP
	/**
	 * Note: this method will sometimes return an address even if we are
	 * not on a WiFi network (for whatever reason, getDhcpInfo does not
	 * return 'null' if Dhcp occurred on 3G).
	 */
	public static InetAddress getWifiBroadcastAddress( Context context ) throws IOException {
		
	    WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcp = wifi.getDhcpInfo();
	    // handle null somehow
	    
	    if ( dhcp == null )
	    	return null ;

	    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
	    byte[] quads = new byte[4];
	    for (int k = 0; k < 4; k++)
	      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
	    return InetAddress.getByAddress(quads) ;
	}
	
}
