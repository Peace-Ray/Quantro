package com.peaceray.quantro.view.options;

import android.graphics.drawable.Drawable;

public interface OptionsControlsView {

	public interface Delegate {
		
		/**
		 * The user has selected Gamepad controls for this game.  In all
		 * likelihood, the controls were previously "Gesture" style.
		 */
		public void ocvd_userSetControlsGamepad( OptionsControlsView ocv ) ;
		
		
		/**
		 * The user has selected Gesture controls for this game.  In all
		 * likelihood, the controls were previously "Gamepad" style.
		 */
		public void ocvd_userSetControlsGesture( OptionsControlsView ocv ) ;
		
		
		/**
		 * The user wants to perform advanced controller configuration.
		 */
		public void ocvd_userAdvancedConfiguration( OptionsControlsView ocv ) ;
		
	}
	
	
	/**
	 * Sets this View's Delegate.  May be called with 'null'.
	 * 
	 * @param delegate An implementation of Delegate.
	 */
	public void setDelegate( Delegate delegate ) ;
	
	
	/**
	 * Provides a thumbnail for the controls.  If 'null', we remove the thumbnail.
	 * @param thumbnail
	 */
	public void setControlsThumbnail( Drawable thumbnail ) ;
	
	/**
	 * Sets that the current controls use Gamepad.
	 * @param quickslide
	 * @param gamepadDownFall
	 * @param gamepadDoubleTap
	 */
	public void setControlsGamepad( boolean quickslide, boolean gamepadDownFall, boolean gamepadDoubleTap ) ;
	
	/**
	 * Sets that the current controls use Gesture.
	 * @param quickslide
	 * @param turnButtons
	 * @param dragAutolocks
	 */
	public void setControlsGesture( boolean quickslide, boolean turnButtons, boolean dragAutolocks ) ;
	
	/**
	 * Sets the controls available for this game: turns, flips.
	 * @param hasTurns
	 * @param hasFlips
	 */
	public void setControlsHas( boolean hasTurns, boolean hasFlips ) ;
	
	
	/**
	 * Refresh this view.
	 */
	public void refresh() ;

}
