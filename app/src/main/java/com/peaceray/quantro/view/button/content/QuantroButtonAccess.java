package com.peaceray.quantro.view.button.content;

import com.peaceray.quantro.view.button.QuantroContentWrappingButton;

import android.graphics.drawable.Drawable;

/**
 * The QuantroButtonContentSetter interface provides a means to configure 
 * simplified, standardized QCWB content.  The purpose of this interface
 * is threefold:
 * 
 * First, an instance of this class allows for a convenient "reference
 * container" for all relevant ButtonContent Views and structures.  This
 * eliminates the need to look up by "findViewById" every time content should
 * be altered, as well as reducing the number of references button content
 * setters need to keep.
 * 
 * Second, references to View content can be kept alongside the Views themselves.
 * We can compare new settings against existing settings, only updating the
 * View if a change has occurred.  Is this more efficient?  I don't know.
 * 
 * Second, as this is an interface, different implementations can exist.
 * The most obvious is a "Direct Content Setter", which merely performs
 * the change in a convenient, one-method way.  However, other implementations
 * such as a Pass-Through could be used to send data to another source:
 * for example, a QuantroButtonStrip might keep "overflow" data in a separate
 * location.  The class configuring button content need not know whether a particular
 * button is instantiated or relegated to "Overflow", since its interaction should
 * be through a ButtonSetter instance and not an actual Button instance.
 * 
 * @author Jake
 *
 */
public interface QuantroButtonAccess {
	
	
	/**
	 * Sets the ContentState for this button.
	 * @param content
	 * @return
	 */
	public boolean setContentState( QuantroContentWrappingButton.ContentState contentState ) ;

	/**
	 * Sets an alpha value for this button.
	 * @param alphaVal
	 * @return Did this call visually change the button?
	 */
	public boolean setContentAlpha( int alphaVal ) ;
	
	/**
	 * Sets the title for this button.
	 * @param desc
	 * @return Did this call visually change the button?
	 */
	public boolean setTitle( CharSequence title ) ;
	
	/**
	 * Sets the "short form" description of this button.
	 * @param desc
	 * @return Did this call visually change the button?
	 */
	public boolean setDescription( CharSequence desc ) ;
	
	/**
	 * Sets the "long form" description of this button.
	 * @param desc
	 * @return Did this call visually change the button?
	 */
	public boolean setLongDescription( CharSequence desc ) ;
	
	
	/**
	 * Sets the time remaining for this button.
	 * @param timeRemaining
	 * @return Did this call visually change the button?
	 */
	public boolean setTimeRemaining( CharSequence timeRemaining ) ;
	
	
	/**
	 * Sets the full "view" background for this button; that
	 * is, sets the Background for the View representing the
	 * button, NOT a content "Background" subview.
	 * @param background
	 * @return
	 */
	public boolean setFullBackground( Drawable background ) ;
	
	/**
	 * Sets the background for this Button
	 * @param background
	 * @return Did this call visually change the button?
	 */
	public boolean setBackground( Drawable background ) ;
	
	/**
	 * Sets that the image drawable for this button should be hidden (made invisible).
	 * @return Did this call visually change the button?
	 */
	public boolean setImageInvisible() ;
	
	/**
	 * Sets the image drawable for this button.
	 * @param show
	 * @param image
	 * @return Did this call visually change the button?
	 */
	public boolean setImage( boolean show, Drawable image ) ;
	
	/**
	 * Sets the image drawable for this button.
	 * @param show
	 * @param image
	 * @param imageColor
	 * @return Did this call visually change the button?
	 */
	public boolean setImage( boolean show, Drawable image, int imageColor ) ;
	
	/**
	 * Sets that the alert drawable for this button should be hidden (made invisible).
	 * @return Did this call visually change the button?
	 */
	public boolean setAlertInvisible() ;
	
	/**
	 * Sets the alert drawable for this button.
	 * @param show
	 * @param alert
	 * @return Did this call visually change the button?
	 */
	public boolean setAlert( boolean show, Drawable alert ) ;
	
	/**
	 * Sets the alert drawable for this button.
	 * @param show
	 * @param alert
	 * @param alertColor
	 * @return Did this call visually change the button?
	 */
	public boolean setAlert( boolean show, Drawable alert, int alertColor ) ;
	
	
	/**
	 * Sets the color for this button.
	 * 
	 * @param color
	 * @return Did this call change the visual appearance of the button?
	 */
	public boolean setColor( int color ) ;
	
	
	/**
	 * Sets the "local vote" Drawable for this button.
	 * 
	 * The drawable, if non-null and shown, will be colored with the
	 * specified color.h
	 * 
	 * @param show
	 * @param d
	 * @param color
	 * @return Did this call change any visible button content?  If yes,
	 * 		a refresh is in order.
	 */
	public boolean setVoteLocal( boolean show, Drawable d, int color ) ;
	
	/**
	 * Sets the 'vote' Drawable for player 'number' in this button.
	 * 
	 * The drawable, if non-null and shown, will be colored with the specified
	 * color.
	 * 
	 * @param number
	 * @param show
	 * @param d
	 * @param color
	 * @return Did this call change any visible button content?  If yes,
	 * 		a refresh is in order.
	 */
	public boolean setVote( int number, boolean show, Drawable d, int color ) ;
	
	
	public enum LobbyDetail {
		MEMBERSHIP,
		CREATION,
		HOST,
		ADDRESS_OR_TYPE,
		SESSION_ID
	}
	
	/**
	 * Sets the 'detail text' for the lobby to the provided CharSequence.
	 * @param detail
	 * @param text
	 * @return Did this call change any visible button content?
	 */
	public boolean setLobbyDetail( LobbyDetail detail, CharSequence text ) ;
	
	
	/**
	 * Sets whether the 'pinwheel' progress bar is visible in this button.
	 * @param visible
	 * @return
	 */
	public boolean setPinwheel( boolean visible ) ;
	
	
	public QuantroContentWrappingButton.ContentState getContentState() ;

	public int getContentAlpha() ;

	public CharSequence getTitle() ;

	public CharSequence getDescription() ;

	public CharSequence getLongDescription() ;

	public CharSequence getTimeRemaining() ;

	public Drawable getFullBackground() ;
	
	public Drawable getBackground() ;
	
	public boolean getImageShown() ;

	public Drawable getImage() ;

	public int getImageColor() ;

	public boolean getAlertShown() ;

	public Drawable getAlert() ;

	public int getAlertColor() ;

	public int getColor() ;
	
	public boolean getVoteLocalShown() ;

	public Drawable getVoteLocal() ;

	public int getVoteLocalColor() ;

	public boolean getVoteShown( int player ) ;

	public Drawable getVote( int player ) ;

	public int getVoteColor( int player ) ;
	
	public CharSequence getLobbyDetail( LobbyDetail detail ) ;
	
	public boolean getPinwheel() ;
	
}
