package com.peaceray.quantro.view.button.popup;

import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;

/**
 * A small wrapper class that can be attached to a QuantroButton, providing
 * -- if available -- a popup menu of options to select from.
 * 
 * The main goal here is to make popup functionality available without making
 * new View layouts or requiring special code for different API versions.
 * 
 * @author Jake
 *
 */
public class QuantroButtonPopup {

	public interface Listener {
		/**
		 * The button has been used to select the specified popup option.
		 * 
		 * Menu items are numbered using indexes from 0 upward, but an optional
		 * Tag object can be specified for each, and it is provided as well.
		 * 
		 * 'asPopup' indicates whether the button was pressed through a popup
		 * menu, or directly as the "default" option.
		 * 
		 * @param button
		 * @param asPopup
		 * @param menuItem
		 * @param menuItemTag
		 */
		public void qbpw_onButtonUsed( QuantroContentWrappingButton button, boolean asPopup, int menuItem, Object menuItemTag ) ;
	
	
		/**
		 * The button has been pressed and the popup menu displayed.
		 * @param button
		 */
		public void qbpw_onMenuOpened( QuantroContentWrappingButton button ) ;
	}
	
	
	
	
	public static QuantroButtonPopupWrapper wrap( QuantroContentWrappingButton button, Listener listener, int defaultOption, CharSequence [] titles ) {
		return wrap( button, listener, defaultOption, titles, null ) ;
	}
	
	public static QuantroButtonPopupWrapper wrap( QuantroContentWrappingButton button, Listener listener, int defaultOption, CharSequence [] titles, Object [] tags ) {
		if ( button == null || listener == null || titles == null ) {
			throw new NullPointerException("Must provide non-null button, listener and titles.") ;
		} else if ( defaultOption < 0 || defaultOption >= titles.length ) {
			throw new IllegalArgumentException("Default option must be an index into 'titles'") ;
		}
		
		// perform the wrapping
		QuantroButtonPopupWrapper instance = null ;
		
		// Check for support for actual popups.
		if ( VersionCapabilities.supportsPopupMenu() ) {
			instance = new QuantroButtonPopupWrapperInstancePopupMenu(
					titles, tags) ;
		} else {
			instance = new QuantroButtonPopupWrapperInstanceSingleOption(
					defaultOption, tags == null ? null : tags[defaultOption]) ;
		}
		
		if ( instance != null ) {
			instance.setListener(listener).setButton(button) ;
		}
		
		return instance ;
	}
	
	
	public static QuantroButtonPopupWrapper update( QuantroButtonPopupWrapper wrapper, int defaultOption, CharSequence [] titles ) {
		return update( wrapper, defaultOption, titles, null ) ;
	}
	
	public static QuantroButtonPopupWrapper update( QuantroButtonPopupWrapper wrapper, int defaultOption, CharSequence [] titles, Object [] tags ) {
		if ( wrapper == null )
			throw new NullPointerException("Must provide non-null wrapper.") ;
		
		if ( wrapper instanceof QuantroButtonPopupWrapperInstanceSingleOption ) {
			((QuantroButtonPopupWrapperInstanceSingleOption)wrapper).set(
					defaultOption, tags == null ? null : tags[defaultOption]) ;
		} else if ( VersionCapabilities.supportsPopupMenu() ) {
			((QuantroButtonPopupWrapperInstancePopupMenu)wrapper).set(titles, tags) ;
		} 
		
		return wrapper ;
	}
}
