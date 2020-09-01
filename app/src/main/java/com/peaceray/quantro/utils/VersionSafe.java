package com.peaceray.quantro.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.peaceray.quantro.utils.onlayoutchangelistener.OnLayoutChangeListenerFactory;
import com.peaceray.quantro.utils.onlayoutchangelistener.OnLayoutChangeListenerFactory.Implementation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class VersionSafe {
	
	public static final String TAG = "VersionSafe" ;
	
	public static void prepareForUDPnio( Context context ) {
		// HACK HACK HACK -- Android 2.2 has a bug involving java.nio,
		// specifically Selectors, where it attempts to use IPv6 even if
		// the device does not support it.  If running 2.2, turn off IPv6.
		
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version == Build.VERSION_CODES.FROYO ) {
			java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
    		java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
		}
	}
	
	
	public static int getLargeMemoryClass( Context context ) {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.HONEYCOMB )
			return getMemoryClass( context ) ;
		
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
		try {
			long bytes = Runtime.getRuntime().maxMemory() ;
			int max_mb = (int)Math.round( bytes / ( 1024 * 1024 ) ) ;
			Method methodGetMemoryClass = ActivityManager.class.getMethod("getLargeMemoryClass") ;
			int memClass = (Integer)(methodGetMemoryClass.invoke(am)) ;
			return Math.min( memClass, max_mb ) ;
		} catch (SecurityException e) {
			
		} catch (NoSuchMethodException e) {
			
		} catch (IllegalArgumentException e) {
			
		} catch (IllegalAccessException e) {
			
		} catch (InvocationTargetException e) {
			
		}
		
		return 16 ;
	}
	
	
	public static int getMemoryClass( Context context ) {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.ECLAIR )
			return 16 ;
		
		ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
		try {
			long bytes = Runtime.getRuntime().maxMemory() ;
			int max_mb = (int)Math.round( bytes / ( 1024 * 1024 ) ) ;
			Method methodGetMemoryClass = ActivityManager.class.getMethod("getMemoryClass") ;
			int memClass = (Integer)(methodGetMemoryClass.invoke(am)) ;
			return Math.min( memClass, max_mb ) ;
		} catch (SecurityException e) {
			
		} catch (NoSuchMethodException e) {
			
		} catch (IllegalArgumentException e) {
			
		} catch (IllegalAccessException e) {
			
		} catch (InvocationTargetException e) {
			
		}
		
		return 16 ;
	}
	
	
	public static final int SCREEN_SIZE_SMALL = Configuration.SCREENLAYOUT_SIZE_SMALL ;
	public static final int SCREEN_SIZE_NORMAL = Configuration.SCREENLAYOUT_SIZE_NORMAL ;
	public static final int SCREEN_SIZE_LARGE = Configuration.SCREENLAYOUT_SIZE_LARGE ;
	public static final int SCREEN_SIZE_XLARGE = 4 ;
	
	
	public static int getScreenSizeCategory( Context context ) {
		int config = context.getResources().getConfiguration().screenLayout
			& Configuration.SCREENLAYOUT_SIZE_MASK ;
		return config ;
	}
	
	
	public static int getScreenWidth( Context context ) {
		return getScreenSize( context ).x ;
	}
	
	public static int getScreenHeight( Context context ) {
		return getScreenSize( context ).y ;
	}
	
	public static Point getScreenSize( Context context ) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		boolean sizeSet = false ;
		Point size = new Point() ;
		
		try {
			Method methodGetSize = display.getClass().getMethod("getSize", Point.class) ;
			methodGetSize.invoke(display, size) ;
			sizeSet = true ;
		} catch (SecurityException e) {
			// reflection failed; fall-through
		} catch (NoSuchMethodException e) {
			// reflection failed; fall-through
		} catch (IllegalArgumentException e) {
			// reflection failed; fall-through
		} catch (IllegalAccessException e) {
			// reflection failed; fall-through
		} catch (InvocationTargetException e) {
			// reflection failed; fall-through
		}
		
		if ( !sizeSet ) {
			size.x = display.getWidth() ;
			size.y = display.getHeight() ;
		}
		return size ;
	}
	
	
	public static boolean disableHardwareAcceleration( View v ) {
		Method methodSetLayerType = null ;
		try {
			methodSetLayerType = v.getClass().getMethod(
					"setLayerType",
				    new Class[]{ int.class, Paint.class } ) ;
		}
		catch (SecurityException e) {  }
		catch (NoSuchMethodException e) {  }
		
		if ( methodSetLayerType != null ) {
			try {
				int LAYER_TYPE_SOFTWARE = 1 ;	// this is the constant value
				methodSetLayerType.invoke(v, new Object[]{LAYER_TYPE_SOFTWARE, null}) ;
				return true ;
			}
			catch (IllegalArgumentException e) {  }
			catch (IllegalAccessException e) {  }
			catch (InvocationTargetException e) {  }
		}
		
		return false ;
	}

	private static Method MethodSetAlpha = null ;
	private static boolean MethodSetAlphaSet = false ;
	/**
	 * A Version-safe approach to setting alpha values.  Uses reflection
	 * to invoke setAlpha; if a 'null' argument is provided, or this
	 * method is run on a version of Android that does not support setAlpha,
	 * this method will have no effect.
	 * @param alphaVal
	 * @param v
	 */
	public static void setAlpha( View caller, float alphaVal ) {
		if ( caller == null ) 
			return ;
		
		if ( !MethodSetAlphaSet ) {
			MethodSetAlphaSet = true ;
			try {
				MethodSetAlpha = caller.getClass().getMethod("setAlpha", float.class) ;
			} catch (SecurityException e) {
				return ;
			} catch (NoSuchMethodException e) {
				return ;
			}
		}
		
		if ( MethodSetAlpha != null ) {
			try {
				MethodSetAlpha.invoke(caller, alphaVal) ;
			} catch (IllegalArgumentException e) {
				return ;
			} catch (IllegalAccessException e) {
				return ;
			} catch (InvocationTargetException e) {
				return ;
			}
		}
	}
	
	
	private static Method MethodSetBackgroundDrawable = null ;
	private static boolean MethodSetBackgroundDrawableSet = false ;
	public static void setBackground( View caller, Drawable d ) {
		if ( caller == null ) 
			return ;
		
		if ( !MethodSetBackgroundDrawableSet
				|| ( MethodSetBackgroundDrawableSet
						&& MethodSetBackgroundDrawable != null && MethodSetBackgroundDrawable.getDeclaringClass() != caller.getClass() ) ) {
			MethodSetBackgroundDrawable = null ;
			MethodSetBackgroundDrawableSet = true ;
			try {
				MethodSetBackgroundDrawable = caller.getClass().getMethod("setBackground", Drawable.class) ;
			}
			catch (SecurityException e) { }
			catch (NoSuchMethodException e) { }
			
			if ( MethodSetBackgroundDrawable == null ) {
				try {
					MethodSetBackgroundDrawable = caller.getClass().getMethod("setBackgroundDrawable", Drawable.class) ;
				}
				catch (SecurityException e) { }
				catch (NoSuchMethodException e) { }
			}
		}
		
		if ( MethodSetBackgroundDrawable != null ) {
			try {
				MethodSetBackgroundDrawable.invoke(caller, d) ;
			} catch (IllegalArgumentException e) {
				return ;
			} catch (IllegalAccessException e) {
				return ;
			} catch (InvocationTargetException e) {
				return ;
			}
		}
	}
	
	
	public static void setInBitmap( BitmapFactory.Options options, Bitmap bitmap ) {
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.HONEYCOMB )
			return ;
		
		// this version supports what we're doing.  Use reflection
		// to set the field.
		if ( options == null )
			return ;
		
		try {
			Field inBitmap = BitmapFactory.Options.class.getDeclaredField("inBitmap") ;
			inBitmap.set(options, bitmap) ;
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace() ;
		}
	}
	
	
	
	public static abstract class OnLayoutChangeListener implements Implementation {
		public abstract void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) ;
	}
	
	
	private static Method MethodAddOnLayoutChangeListener = null ;
	private static boolean MethodAddOnLayoutChangeListenerSet = false ;
	public static boolean addOnLayoutChangeListener( View caller, OnLayoutChangeListener listener ) {
		if ( caller == null ) 
			return false ;
		
		final int version = Build.VERSION.SDK_INT ;
		
		if ( version < Build.VERSION_CODES.HONEYCOMB )
			return false ;
		
		Object onLayoutChangeListener = OnLayoutChangeListenerFactory.wrap(listener) ;
		if ( onLayoutChangeListener != null ) {
			if ( !MethodAddOnLayoutChangeListenerSet ) {
				MethodAddOnLayoutChangeListenerSet = true ;
				try {
					Method [] methods = caller.getClass().getMethods() ;
					// look for the method
					for ( int i = 0; i < methods.length; i++ ) {
						if ( methods[i].getName().equals("addOnLayoutChangeListener") ) {
							// make sure it's the base class version.
							if ( methods[i].getDeclaringClass() == View.class ) {
								MethodAddOnLayoutChangeListener = methods[i] ;
							} else {
								throw new IllegalArgumentException("Provided View subclass overrides addOnlayoutChangeListener.") ;
							}
						}
					}
				} catch (SecurityException e) {
					return false ;
				}
			}
			
			if ( MethodAddOnLayoutChangeListener != null ) {
				// provide listener
				try {
					MethodAddOnLayoutChangeListener.invoke(caller, onLayoutChangeListener) ;
					return true ;
				} catch (IllegalArgumentException e) {
					return false ;
				} catch (IllegalAccessException e) {
					return false ;
				} catch (InvocationTargetException e) {
					return false ;
				}
			}
		}
		
		return false ;
	}
	
	
	
	private static Method MethodSetSystemUiVisibility = null ;
	private static boolean MethodSetSystemUiVisibilitySet = false ;
	
	private static Method getMethodSetSystemUiVisibility() {
		// method was added in API 11
		if ( !MethodSetSystemUiVisibilitySet && Build.VERSION.SDK_INT >= 11 ) {
			try {
				MethodSetSystemUiVisibility = View.class.getMethod("setSystemUiVisibility", Integer.TYPE) ;
				MethodSetSystemUiVisibilitySet = true ;
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				return null ;
			}
		}
		return MethodSetSystemUiVisibility ;
	}
	
	
	/**
	 * Returns whether the setupUI methods should be called after the
	 * ContentView is set for an activity.  If 'true', wait until after
	 * we have a content view (and we can make the call multiple times).
	 * If 'false', call before a content view is set.
	 * @return
	 */
	public static boolean setupUIImmersiveOnResume() {
		// it's this call that makes a difference.  If it
		// returns 'false' we do our configuration of the Window,
		// and thus it needs to happen before a ContentView is set.
		return VersionCapabilities.supportsImmersiveFullScreen() ;
	}
	
	
	/**
	 * Sets the provided Activity such that the status bar is hidden (possibly
	 * permanently, possible shown when needed as an overlay) but onscreen
	 * navigation controls, if present in this device, remain onscreen.
	 * 
	 * @param a
	 */
	public static void setupUIShowNavigationBar( Activity a, boolean useImmersiveIfAvailable ) { 
		// If Immersive mode is supported (i.e. version 19 or higher), then we
		// configure this mode such that swiping from the top of the screen will
		// temporarily show the Status Bar (notifications, etc.).
		Window w = a.getWindow() ;
		setupUIShowNavigationBar(w, useImmersiveIfAvailable) ;
	}
	
	public static void setupUIShowNavigationBar( Dialog d, boolean useImmersiveIfAvailable ) { 
		// If Immersive mode is supported (i.e. version 19 or higher), then we
		// configure this mode such that swiping from the top of the screen will
		// temporarily show the Status Bar (notifications, etc.).
		Window w = d.getWindow() ;
		setupUIShowNavigationBar(w, useImmersiveIfAvailable) ;
	}
	
	
	
	public static void setupUIShowNavigationBar( Window w, boolean useImmersiveIfAvailable ) {
		// Immersive mode does not play nice with popups and/or dialogs.
		// We therefore only use immersive settings when we actually want
		// to be immersive (i.e. hide the navigation bar).
		if ( VersionCapabilities.supportsImmersiveFullScreen() && useImmersiveIfAvailable ) {
			// set the main content view (and therefore its containing window)
			// to hide the navigation.  Also sets the IMMERSIVE_STICKY flag for
			// the view (and thus its window).
			setDecorViewImmersiveFullScreen(w, false) ;
		} else {
			// set the window to permanently hide the status bar.  Supported
			// since API 1.
			w.setFlags(
					WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
	
	
	public static void setupUIImmersive( Activity a ) {
		setupUIImmersive(a.getWindow()) ;
	}
	
	public static void setupUIImmersive( Dialog d ) {
		setupUIImmersive(d.getWindow()) ;
	}
	
	/**
	 * Sets the provided Activity such that the status bar is hidden (possibly
	 * permanently, possible shown when needed as an overlay) but onscreen
	 * navigation controls, if present in this device, remain onscreen.
	 * 
	 * @param a
	 */
	public static void setupUIImmersive( Window w ) { 
		// If Immersive mode is supported (i.e. version 19 or higher), then we
		// configure this mode such that swiping from the top of the screen will
		// temporarily show the Status Bar (notifications, etc.), and swiping
		// from the bottom will temporarily show the Navigation Bar.
		
		// Otherwise, we only hide the status bar -- there is no "Immersive" mode
		// prior to API 19.
		if ( VersionCapabilities.supportsImmersiveFullScreen() ) {
			// set the main content view (and therefore its containing window)
			// to hide the navigation.  Also sets the IMMERSIVE_STICKY flag for
			setDecorViewImmersiveFullScreen(w, true) ;
		} else {
			// set the window to permanently hide the status bar.  Supported
			// since API 1.
			w.setFlags(
					WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
	}
	
	private static void setDecorViewImmersiveFullScreen(Window w, boolean hideNavBar) {
		if ( w == null )
			return ;
		View v = w.getDecorView() ;
		if ( v != null ) {
			Method setSystemUIVis = getMethodSetSystemUiVisibility() ;
			int visFlag = 0 ;
			visFlag |= 0x1000 ;			// View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			visFlag |= 0x400 ;			// View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
			visFlag |= 0x100 ;			// View.SYSTEM_UI_FLAG_LAYOUT_STABLE ;
			visFlag |= 0x001 ;			// View.SYSTEM_UI_FLAG_LOW_PROFILE ;
			visFlag |= 0x004 ;			// View.SYSTEM_UI_FLAG_FULLSCREEN ;
			if ( hideNavBar ) {
				visFlag |= 0x200 ;		// View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION 
				visFlag |= 0x002 ;		// View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			}
			try {
				setSystemUIVis.invoke(v, visFlag) ;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
