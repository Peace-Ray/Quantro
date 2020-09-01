package com.peaceray.quantro.view.controls;

import java.util.Hashtable;

import android.view.MotionEvent;

/**
 * A different implementation of this class will be loaded
 * depending on the underlying system features we want to support.
 * 
 * Usage Contract: a single instance of this interface will be
 * instantiated upon the inflation or creation of an InvisibleControls
 * instance.  This instance will be accessed by that IC, and only by
 * that IC.  This instances's onTouchEvent( ic, event ) method will
 * be called every time IC.onTouchEvent is called, passing in the provided
 * parameter.
 * 
 * @author Jake
 *
 */
public interface InvisibleControlsMotionEventHandler {
	/**
	 * Processes this motion event.  Returns whether any information remains
	 * unprocessed; e.g. if it includes 'moves' for pointers we are not tracking.
	 * 
	 * @param event
	 * @param enabled
	 * @return
	 */
	public boolean onTouchEvent(MotionEvent event, Hashtable<String, Boolean> enabled) ;
	
	/**
	 * Is this relevant to us?  Definition of 'relevant' may differ based on class 
	 * and constructions parameters.
	 * 
	 * @param event
	 * @return
	 */
	public boolean isRelevant(MotionEvent event, Hashtable<String, Boolean> enabled) ;
	
	
	/**
	 * Provides the slide handler dealing with this button's presses.  Slide handlers
	 * provoke an important change in the behavior of a MotionHandler.  Without one,
	 * a button is treated as a physically bordered object on screen.  Touching and releasing
	 * performs the expected result, but sliding a finger ONTO and then OFF OF the button
	 * will perform the same function, like sliding your finger onto and then off of
	 * a physical button - it gets pressed and released.
	 * 
	 * However, with SlideHandlers, things are different.  The button still occupies
	 * physical space on the screen, but you cannot 'slide onto' or 'off of' the button.
	 * Instead, your finger must first touch the screen within the button's bounds.
	 * After that, finger movement is passed to the slide handler: anywhere on screen you
	 * go, it's the slide handler that is affected, with the button itself remaining "pressed."
	 * 
	 * When the finger is released, so is the button.
	 * 
	 * @param buttonName
	 * @param slideHandler
	 */
	public void setSlideHandler( String buttonName, InvisibleControlsSlideHandler slideHandler ) ;
	
}
