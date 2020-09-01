package com.peaceray.quantro.consts;

import com.peaceray.quantro.view.game.DrawSettings;

public final class GlobalTestSettings {
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// BIG RESOURCE LOAD
	public static final boolean FORCE_LOAD_LARGE_RESOURCES = false ;		// load the biggest supported resource sizes.
	
	
	////////////////////////////////////////////////////////////////////////////
	// HW ACCEL
	// We have two "best guesses" for the problem with Fatal Signal 11.  This would
	// seem to be a relatively recent problem, and diagnosis has been difficult.
	// We currently estimate that Fatal Signal 11 occurs only when hardware 
	// acceleration is on, whether manually via manifest or through "force GPU."
	// Our best guesses are
	// 1. Something in BlockDrawer or its container GameView (in games)
	// 2. Something in the Mobclix AdView.
	
	// Use these to disable them.  Setting to 'true' does not guarantee HW accel, but 
	// to 'false' will disable it.
	public static final boolean ALLOW_HARDWARE_ACCEL_ADVIEW = false ;		// used in QuantroActivity
	
	
	////////////////////////////////////////////////////////////////////////////
	// GAME CONFIG
	// Special settings for displaying additional information in Games.
	
	
	public static final boolean GAME_ALLOW_KEYBOARD = false ;
		// allows keyboard controls using 
		// 			ccw R  cw
		// 		<--	<- 	v  -> -->
	
		//			u   i   o
		//		h	j   k   l   ;
		//				,
	
	
		// This is basically only useful in the emulator.
	
	public static final boolean GAME_SPEED_IS_SCALED = false ;
	public static final double GAME_SPEED_SCALAR = 0.1 ;
		// if GAME_SPEED_IS_SCALED, then we apply GAME_SPEED_SCALAR
		// to all calculations involving the passage of time.  Specifically,
		// if we are measuring the amount of time that has passed, we MULTIPLY
		// by the scalar (thus, scalars < 1 will slow everything down).  If
		// instead we are applying a fixed delay or time window, such as
		// a sendDelayed Handler message, we DIVIDE by the scalar (thus,
		// scalars < 1 will produce longer delays and windows).
	
		// The goal here is that the gameplay and graphics of a game running
		// at GAME_SPEED_SCALAR = 0.25 (e.g.) should be *identical* to that
		// of a video filmed at 4x frame rate... and thus, video capture with
		// this setting can be re-encoded at 4 frames a frame to get a "normal speed"
		// video.  Very useful for working around frame rate issues when filming!
	
		// It is acceptable for frame rate calculations to be messed up by this,
		// but gameplay should be exactly scaled.
	
	// these methods are provided as a convenience.
	public static final long scaleGameAbsoluteTime( long time ) {
		if ( GAME_SPEED_IS_SCALED )
			return (long)(time * GAME_SPEED_SCALAR) ;
		return time ;
	}
	
	public static final long scaleGameInputTimeWindow( long window ) {
		if ( GAME_SPEED_IS_SCALED )
			return (long)(window / GAME_SPEED_SCALAR) ;
		return window ;
	}
	
	public static final long getCurrentGameScaledTime() {
		if ( GAME_SPEED_IS_SCALED )
			return (long)( GAME_SPEED_SCALAR * System.currentTimeMillis() ) ;
		return System.currentTimeMillis() ;
	}
	
	
	public static final boolean GAME_DISABLE_SAVE_ON_LEVEL_UP = false ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	

	////////////////////////////////////////////////////////////////////////////
	// GAME VIEW CONFIG
	// Special settings for displaying additional information in Games.
	
	public static final boolean GAME_SELF_THUMBNAIL = false ;
		// does the thumbnail mirror your game?
	
	public static final boolean GAME_SHOW_FRAME_RATE = false ;
		// display frame rate information on-screen
	
	public static final boolean GAME_CYCLE_VEIL = false ;
		// cycle the veil on and off regularly.
	
	public static final boolean GAME_CYCLE_BACKGROUND_SHUFFLE = false ;
		// cycle through backgrounds regularly
	
	public static final boolean GAME_THUMBNAIL_EVERY_TICK = false ;
	
	public enum GameBlit {
		DEFAULT,
		SEPTUPLE,
		FULL,
		NONE
	}
	
	public static final GameBlit GAME_BLIT = GameBlit.DEFAULT;
	public static final int GAME_SCALE = 0;		// 0 for default
	
	public static int setBlit( int blit ) {
		switch( GAME_BLIT ) {
		case SEPTUPLE:
			return DrawSettings.BLIT_SEPTUPLE ;
		case FULL:
			return DrawSettings.BLIT_FULL ;
		case NONE:
			return DrawSettings.BLIT_NONE ;
		case DEFAULT:
		default:
			return blit ;
		}
	}

	public static int setScale( int scale ) {
		return GAME_SCALE == 0 ? scale : GAME_SCALE;
	}
	
	public static final boolean GAME_SEPTUPLE_BLIT_FULL_CLIP = true ;
		// When arranging septuple blit images into FULL, do not
		// perform "covered / touched" clips and draw the entire image,
		// including empty pixels.
	
	
	public static final boolean GAME_OMIT_SCORE_DRAWER = false ;
	public static final boolean GAME_OMIT_SCORE_DRAWER_ABBREVIATED = false ;
	public static final boolean GAME_OMIT_SCORE_DRAWER_DETAILED = false ;
	
	
	public static final boolean GAME_DISABLE_TOOLTIP_DRAWER = false ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	// /////////////////////////////////////////////////////////////////////////
	// BLOCK DRAWER CONFIG
	// Special BlockDrawer behavior.
	
	public static final boolean BLOCK_DRAWER_LOG_PROFILING = false ;
		// Print periodic method profiling info to the log
	
	public static final boolean BLOCK_DRAWER_LOG_SLICE_STATE = false ;
		// Print to the log when the slice state changes
	
	public static final boolean BLOCK_DRAWER_LOG_SETUP = false ;
	
	public static final boolean BLOCK_DRAWER_PREVENT_PRERENDERED_BORDER_FIXED_WIDTH_OPTIMIZATION = false ;
		// Prevent fixed-width prerendered borders from being drawn as an optimization (rather than GradientDrawable objects)
		// When both are available, a fixed-width border is preferred to a stretchable one.
	
	public static final boolean BLOCK_DRAWER_PREVENT_PRERENDERED_BORDER_STETCHABLE_WIDTH_OPTIMIZATION = false ;
		// Prevent stretchable prerendered borders from being drawn as an optimization (rather than GradientDrawable objects)
		// When both are available, a fixed-width border is preferred to a stretchable one.
	
	public static final boolean BLOCK_DRAWER_FORCE_GLOW_MATRIX_ALLOCATION = false ;
		// Always allocate a new glow matrix when we need to draw a glow.

	
	public static final boolean BLOCK_DRAWER_SHOW_COVERED_AND_TOUCHING_REGIONS = false ;
	
	
	public static final boolean BLOCK_DRAWER_STRICT_EFFECT_CHECKS = false ;
		// Strictly enforce settings on block drawer Effects.  'true' is safe,
		// requiring that (e.g.) Effect objects are treated as stateful objects
		// with 'mutable' / 'accessable' states.  'false' omits these checks,
		// potentially causing errors in usage, but saving some CPU cycles.
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// XL KEY PURCHASE
	//
	
	public static final boolean KEY_MANAGER_TEST_MODE = false ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// DROP SHADOW TESTS
	//
	
	public static final boolean DROP_SHADOW_STRICT_TESTS = false ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// FORCE OPTIONS AND POPUP MENUS
	//
	
	public enum ForcePopups {
		DEFAULT,
		FORCE_OPTION_MENUS,
		FORCE_POPUP_MENUS
	}
	
	public static final ForcePopups FORCE_POPUPS = ForcePopups.DEFAULT ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
}
