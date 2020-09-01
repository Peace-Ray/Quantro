package com.peaceray.quantro.utils;

import com.peaceray.quantro.consts.GlobalTestSettings;

import android.os.Build;
import android.util.Log;

public class VersionCapabilities {
	
	// remap a few version codes...
	public static int VERSION_DONUT = Build.VERSION_CODES.DONUT ;
	public static int VERSION_1_6 = VERSION_DONUT ;
	public static int VERSION_API_4 = VERSION_DONUT ;
	
	public static int VERSION_ECLAIR = Build.VERSION_CODES.ECLAIR ;
	public static int VERSION_2_0 = VERSION_ECLAIR ;
	public static int VERSION_API_5 = VERSION_ECLAIR ;
	
	public static int VERSION_GINGERBREAD = Build.VERSION_CODES.GINGERBREAD ;
	public static int VERSION_2_3 = VERSION_GINGERBREAD ;
	public static int VERSION_API_9 = VERSION_GINGERBREAD ;
	
	public static int VERSION_HONEYCOMB = Build.VERSION_CODES.HONEYCOMB ;
	public static int VERSION_3 = VERSION_HONEYCOMB ;
	public static int VERSION_API_11 = VERSION_HONEYCOMB ;
	
	public static int VERSION_ICE_CREAM_SANDWICH = Build.VERSION_CODES.ICE_CREAM_SANDWICH ;
	public static int VERSION_4_0 = VERSION_ICE_CREAM_SANDWICH ;
	public static int VERSION_API_14 = VERSION_ICE_CREAM_SANDWICH ;
	
	public static int VERSION_JELLY_BEAN = Build.VERSION_CODES.JELLY_BEAN ;
	public static int VERSION_4_1 = VERSION_JELLY_BEAN ;
	public static int VERSION_API_16 = VERSION_JELLY_BEAN ;
	
	public static int VERSION_KIT_KAT = 19 ;
	public static int VERSION_4_4 = VERSION_KIT_KAT ;
	public static int VERSION_API_19 = VERSION_KIT_KAT ;
	
	
	public static boolean versionAtLeast( int minVersion ) {
		final int version = Build.VERSION.SDK_INT ;
		
		return version >= minVersion ;
	}
	
	public static boolean isVersion( int version ) {
		final int v = Build.VERSION.SDK_INT ;
		
		return version == v ;
	}
	
	public static final int getVersion() {
		return Build.VERSION.SDK_INT ;
	}
	
	
	public static boolean supportsGooglePlayGames() {
		final int version = Build.VERSION.SDK_INT ;
		if ( version >= Build.VERSION_CODES.FROYO )
			return true ;
		return false ;
	}
	
	public static boolean supportsOptionsMenu() {
		switch( GlobalTestSettings.FORCE_POPUPS ) {
		case FORCE_OPTION_MENUS:
			return true ;
		case FORCE_POPUP_MENUS:
			return false ;
		case DEFAULT:
		default:
			final int version = Build.VERSION.SDK_INT ;
			if ( version < Build.VERSION_CODES.HONEYCOMB )
				return true ;
			return false ;
		}
	}
	
	public static boolean supportsPopupMenu() {
		switch( GlobalTestSettings.FORCE_POPUPS ) {
		case FORCE_OPTION_MENUS:
			return false ;
		case FORCE_POPUP_MENUS:
			return true ;
		case DEFAULT:
		default:
			final int version = Build.VERSION.SDK_INT ;
			if ( version < Build.VERSION_CODES.HONEYCOMB )
				return false ;
			return true ;
		}
	}
	
	

	public static boolean supportsMultitouch() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.ECLAIR )
			return false ;
		return true ;
	}
	
	
	public static boolean isEmulator() {
		boolean emul = true ;
		emul = emul && ( "sdk".equals( Build.PRODUCT ) || "google_sdk".equals( Build.PRODUCT ) ) ;
		emul = emul && Build.FINGERPRINT.startsWith("generic") ;
		
		return emul ;
	}
	
	
	public static boolean supportsBlockDrawerWithViewBehind() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.HONEYCOMB )
			return false ;
		return true ;
	}
	
	public static boolean supportsNotificationBuilder() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.HONEYCOMB )
			return false ;
		return true ;
	}
	
	
	public static boolean supportsContentClipboardManager() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.HONEYCOMB )
			return false ;
		return true ;
	}
	
	public static boolean hasDrawableContentImageViewScaleBug() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version == Build.VERSION_CODES.JELLY_BEAN )
			return true ;
		return false ;
	}
	
	/**
	 * NFC was added to 2.3 (Gingerbread) as a primarily
	 * read-only interface.
	 * 
	 * @return
	 */
	public static boolean supportsNfc() {
		// NFC was added in Gingerbread, but the Android Beam
		// APIs changed later on.
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version >= Build.VERSION_CODES.GINGERBREAD )
			return true ;
		return false ;
	}
	
	/**
	 * Returns true if there is any method of pushing data
	 * over NFC available to us.  If this returns 'true',
	 * at least one of supportsNfcForegroundNdefPush() or
	 * supportsNfcNdefPushMessage() will return 'true.'
	 * 
	 * @return
	 */
	public static boolean supportsNfcPush() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version >= Build.VERSION_CODES.GINGERBREAD_MR1 )
			return true ;
		return false ;
	}
	
	/**
	 * Foreground Push is a (now depreciated) API for sending
	 * NFC messages.
	 * 
	 * If this method returns 'true' the (now depreciated) methods
	 * are OK to use.  Therefore, it returns 'true' only in a
	 * range of version APIs not including 14 or higher.
	 * 
	 * @return
	 */
	public static boolean supportsNfcForegroundNdefPush() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version >= Build.VERSION_CODES.GINGERBREAD_MR1 && version < Build.VERSION_CODES.ICE_CREAM_SANDWICH )
			return true ;
		return false ;
	}
	
	/**
	 * Push Message is the new standard for sending NFC messages.
	 * You can 'Android Beam' if either this or
	 * supportsNfcForegroundNdefPush returns 'true', but the
	 * method of doing so will differ.
	 * @return
	 */
	public static boolean supportsNfcNdefPushMessage() {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version >= Build.VERSION_CODES.ICE_CREAM_SANDWICH )
			return true ;
		return false ;
	}
	
	
	/**
	 * Supports loading Bitmaps into an existing bitmap
	 * of the same dimensions, using "BitmapFactory.Options.inBitmap"
	 * @return
	 */
	public static boolean supportsLoadInBitmap() {
		final int version = Build.VERSION.SDK_INT ;
		
		return version >= Build.VERSION_CODES.HONEYCOMB ;
	}
	
	
	/**
	 * Supports putting the interface in "Immersive Mode," in which the Status
	 * Bar and Navigation Bar (if present) are hidden from view and are not restored
	 * by normal touch-events.  The can be displayed be swiping down from the top
	 * or up from the bottom.
	 * 
	 * This mode was added in KitKat (4.4, API 19) specifically for e-readers and
	 * games.  Generally it would be appropriate for in-game windows, but not
	 * for menus and lobbies.
	 * 
	 * @return
	 */
	public static boolean supportsImmersiveFullScreen() {
		final int version = Build.VERSION.SDK_INT ;
		
		// Testing on the 4.4 emulator reveals numerous problems: navigation
		// and status bars re-appear at seemingly the drop of a hat (including
		// when a popup menu is displayed!), system
		// crashes when a dialog-formatted Activity starts, etc.  Needs a lot
		// more testing; immersive full-screen in Quantro is not ready for full-time,
		// especially in the generally menu and lobbies.  Plan for next update
		// (1.0.5), not this one (1.0.4).
		Log.e("VersionCapabilities", "supportsImmersiveFullScreen stub: returns 'false' in all cases.") ;
		Log.e("VersionCapabilities", "Correct this when Immersive is tested and functional in 4.4.") ;
		return false ;
		// return version >= VERSION_KIT_KAT ;
	}
	
}
