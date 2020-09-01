package com.peaceray.quantro.consts;


/**
 * Previously, we used generated IDs to represent unique "request ID" when calling
 * startActivityForResult.  This fails in the presence of Fragments, because FragmentActivities
 * reserve the top 16 bits for their own BS reasons or whatever.
 * 
 * So we have to use integer constants, which seems clumsy and stupid, but whatevs.  The
 * alternative is to define an integer in our Resources and then actually load that
 * integer, which is functionally identical except adds an unnecessary step.
 * 
 * @author Jake
 *
 */
public class IntentForResult {

	// These values can be freely changed; they are not stored between
	// App launches, and are used for nothing but Activity starts.
	
	public static final int REQUEST_NEW_INTERNET_LOBBY 		= 0x0000 ;
	public static final int REQUEST_NEW_WIFI_LOBBY 			= 0x0001 ;
	public static final int REQUEST_TARGET_WIFI_ADDRESS		= 0x0002 ;
	public static final int REQUEST_EXAMINE_GAME_RESULT		= 0x0003 ;
	public static final int NEW_GAME_SETTINGS				= 0x0004 ;
	public static final int LAUNCH_GAME						= 0x0005 ;
	public static final int FIRST_TIME_SETUP				= 0x0006 ;
	public static final int CUSTOM_GAME_MODE				= 0x0007 ;
	
	public static final int PLAY							= 0x0008 ;
	
	// Game Options Menu
	public static final int GAME_OPTIONS_MENU 						= 0x0012 ;
	
	
	public static final int UNUSED									= 0x8888 ;
}
