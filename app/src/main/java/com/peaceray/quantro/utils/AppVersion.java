package com.peaceray.quantro.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.peaceray.quantro.communications.HttpURLConnectionInterruptThread;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class AppVersion {

	private static final String NAME = "com.peaceray.quantro" ;
	private static final String MIN_VERSION_CODE_URL = "http://secure.peaceray.com/quantro/min_version.txt" ;
	private static final int DEFAULT_TIMEOUT = 5000 ;
	
	
	private static int CODE_CACHED = -1 ;
	private static String NAME_CACHED = null ;
	
	
	private static String NAME_FLAG_ALPHA = "a" ;
	private static String NAME_FLAG_BETA = "b" ;
	private static String NAME_FLAG_DEMO = "d" ;
	private static String NAME_FLAG_RELEASE_CANDIDATE = "rc" ;
	
	
	@SuppressLint("DefaultLocale")
	private static boolean nameContains( Context context, String flag ) {
		if ( context == null )
			throw new NullPointerException("Must provide non-null context") ;
		
		String name = null ;
		if ( NAME_CACHED != null  )
			name = NAME_CACHED ;
		else
			name = name( context ) ;
			
		return name.toLowerCase().indexOf(flag) != -1 ;
	}
	
	public static boolean isAlpha( Context context ) {
		return nameContains( context, NAME_FLAG_ALPHA ) ;
	}
	
	public static boolean isBeta( Context context ) {
		return nameContains( context, NAME_FLAG_BETA ) ;
	}
	
	public static boolean isDemo( Context context ) {
		return nameContains( context, NAME_FLAG_DEMO ) ;
	}
	
	public static boolean isReleaseCandidate( Context context ) {
		return nameContains( context, NAME_FLAG_RELEASE_CANDIDATE ) ;
	}
	
	public static boolean isRelease( Context context ) {
		return !isAlpha(context) && !isBeta(context) && !isReleaseCandidate(context) ;
	}
	
	
	public static int code( Context context ) {
		if ( context == null )
			throw new NullPointerException("Must provide non-null context") ;
		if ( CODE_CACHED > -1 )
			return CODE_CACHED ;
			
		PackageInfo info = getPackageInfo(context) ;
		
		if ( info != null ) {
			CODE_CACHED = info.versionCode ;
			return CODE_CACHED ;
		}
		return -1 ;
	}
	
	public static String name( Context context ) {
		if ( context == null )
			throw new NullPointerException("Must provide non-null context") ;
		if ( NAME_CACHED != null )
			return NAME_CACHED ;
		
		PackageInfo info = getPackageInfo(context) ;
		
		if ( info != null ) {
			NAME_CACHED = info.versionName ;
			return NAME_CACHED ;
		}
		return null ;
	}
	
	
	
	private static final PackageInfo getPackageInfo( Context c ) {
		try {
			PackageManager manager = c.getPackageManager() ;
			PackageInfo info = manager.getPackageInfo(NAME, 0) ;
			return info ;
		} catch ( NameNotFoundException nnf ) {
			nnf.printStackTrace() ;
			return null ;
		}
	}
	
	
	/**
	 * Attempts to determine the minimum version code to allow
	 * Internet multiplayer.  Queries www.peaceray.com/quantro/min_version.txt.
	 * 
	 * Timeout is REQUIRED.
	 * 
	 * @return The minimum version number for Internet multiplayer, or
	 * 		-1 if the minimum version number could not be queried.
	 */
	public static int minMultiplayerCode() {
		return minMultiplayerCode( DEFAULT_TIMEOUT ) ;
	}
	
	/**
	 * Attempts to determine the minimum version code to allow
	 * Internet multiplayer.  Queries www.peaceray.com/quantro/min_version.txt.
	 * 
	 * Timeout is REQUIRED.
	 * 
	 * @param timeout The maximum number of milliseconds to wait.  Must be > 0.
	 * @return The minimum version number for Internet multiplayer, or
	 * 		-1 if the minimum version number could not be queried.
	 */
	public static int minMultiplayerCode( int timeout ) {
		if ( timeout <= 0 )
			throw new IllegalArgumentException("Must provide a positive timeout.") ;
		
		HttpURLConnection conn = null ;
		BufferedReader rd = null ;
		try {
			URL url = new URL(MIN_VERSION_CODE_URL);
			conn = (HttpURLConnection) url.openConnection();
		    conn.setConnectTimeout(timeout) ;
		    conn.setReadTimeout(timeout) ;
		    conn.setDoOutput(true);
		    conn.connect() ;
		    // wrap a interrupt thread
		    new HttpURLConnectionInterruptThread( conn, timeout ).start() ;
		    
		    // Get the version code
		    rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		    String line = rd.readLine();
		    rd.close();
		    
		    // Return the minimum version code to use.
		    return Integer.parseInt(line) ;
		    
		} catch ( IOException e ) {
			e.printStackTrace() ;
			return -1 ;
		} catch (Exception e) {
			e.printStackTrace();
			return -1 ;
		} finally {
			try {
				conn.disconnect() ;
			} catch ( Exception e ) { }
			try {
				rd.close() ;
			} catch ( Exception e ) { }
			
		}
	}
	
}
