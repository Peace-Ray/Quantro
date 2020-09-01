package com.peaceray.quantro.view.options;

import java.util.Collection;
import java.util.Map;

import com.peaceray.quantro.content.Background;


/**
 * The BackgroundView provides a way for users to select either the current
 * background (if not shuffling) or the collection of backgrounds used
 * (if shuffling).
 * 
 * @author Jake
 *
 */
public interface OptionsBackgroundView {

	public interface Delegate {
		
		
		/**
		 * Everyday I'm shuffling, or not, depending on what the user sets here.
		 * 
		 * The return value of this method will be taken as a shorthand for the
		 * suggested call.  If 'true' is returned, obv will respond by calling
		 * obv.setBackgroundShuffling( shuffling ) on itself.
		 * 
		 * @param obv
		 * @param shuffling
		 * @return
		 */
		public boolean obvd_userSetBackgroundShuffling( OptionsBackgroundView obv, boolean shuffling ) ;
		
		/**
		 * The user is attempting to set the provided Background as the current
		 * background used for gameplay.  The OptionsBackgroundView (provided) will
		 * not take any action unless instructed to by the delegate.
		 * 
		 * One example for behavior is to call obv.setCurrentBackground( skin ),
		 * which informs the view that the user selection was successful.
		 * 
		 * The return value of this method will be taken as a shorthand for
		 * the suggested call.  If 'true' is returned, obv will respond by
		 * calling obv.setCurrentBackground( background ) on itself.
		 * 
		 * @param view
		 * @param background
		 * @param availability As a reminder, this is the currently set
		 * 			OptionAvailability for this background.
		 * @return
		 */
		public void obvd_userSetCurrentBackground( OptionsBackgroundView obv, Background background, OptionAvailability availability ) ;
		
		
		/**
		 * The user is attempting to set the provided Background as the current
		 * background used for gameplay, as well as include it in the current
		 * shuffle (if not included).
		 * 
		 * @param view
		 * @param background
		 * @param availability As a reminder, this is the currently set
		 * 			OptionAvailability for this background.
		 * @return
		 */
		public void obvd_userSetCurrentBackgroundAndIncludeInShuffle( OptionsBackgroundView obv, Background background, OptionAvailability availability ) ;
		
		
		
		/**
		 * The user is attempting to add or remove the provided Background from the current
		 * shuffle rotation.  The calling View will not take any action unless instructed
		 * to by the delegate.
		 * 
		 * The return value of this method will be taken as a shorthand for
		 * the suggested call.  If 'true' is returned, obv will respond by
		 * calling obv.setBackgroundShuffled( background, inRotation ) on itself.
		 * 
		 * @param obv
		 * @param background
		 * @param inRotation
		 * @param availability
		 * @return
		 */
		public void obvd_userSetBackgroundShuffled( OptionsBackgroundView obv, Background background, boolean inRotation, OptionAvailability availability ) ;
		
		/**
		 * The user wants to perform advance configuration.  We have made no internal
		 * changes.
		 * 
		 * @param osmv
		 */
		public void obvd_userAdvancedConfiguration( OptionsBackgroundView obvd ) ;
		
	}
	
	
	/**
	 * Sets this View's Delegate.  May be called with 'null'.
	 * 
	 * @param delegate An implementation of Delegate.
	 */
	public void setDelegate( Delegate delegate ) ;
	
	
	/**
	 * Clears all Background available (and made visible) by this view.
	 */
	public void clearBackgrounds() ;
	
	
	/**
	 * Adds this background to the set displayed.
	 * 
	 * @param background
	 * @param availability
	 */
	public void addBackground( Background background, OptionAvailability availability ) ;
	
	
	/**
	 * Sets the specified backgrounds and availability as the set displayed by this
	 * Options view.
	 * 
	 * Equivalent to calling clearBackgrounds(), then addBackground() in succession.
	 * 
	 * @param backgrounds
	 * @param availability
	 */
	public void setBackgrounds( Collection<Background> backgrounds, Map<Background, OptionAvailability> availability ) ;
	
	
	/**
	 * Sets the availability of the specified background, which must have been 
	 * previously added.
	 * 
	 * @param background
	 * @param availability
	 */
	public void setBackgroundAvailability( Background background, OptionAvailability availability ) ;
	
	
	/**
	 * Sets the availability of the provided backgrounds, which must have been
	 * previously added.
	 * 
	 * @param availability
	 */
	public void setBackgroundAvailability( Map<Background, OptionAvailability> availability ) ;

	
	/**
	 * Is Shuffle even supported?  If not, the shuffle button will be disabled.
	 * @param supported
	 */
	public void setBackgroundShuffleSupported( boolean supported ) ;
	
	
	/**
	 * Are we shuffling through backgrounds?
	 * @param shuffling
	 */
	public void setBackgroundShuffling( boolean shuffling ) ;
	
	
	/**
	 * Sets the currently displayed background (as mentioned above, OBVs do
	 * not affect actual background displays or permanent settings).
	 * @param background
	 */
	public void setCurrentBackground( Background background ) ;
	
	
	/**
	 * Sets whether the specified background appears in our shuffle rotation.
	 * @param background
	 * @param inRotation
	 */
	public void setBackgroundShuffled( Background background, boolean inRotation ) ;
	
	
	/**
	 * Sets that the provided Backgrounds are exactly those which are rotated
	 * through in a shuffle.  All provided Backgrounds must have been previously
	 * set with addBackground or setBackgrounds.
	 * 
	 * @param backgrounds
	 */
	public void setBackgroundsShuffled( Collection<Background> backgrounds ) ;
	
	/**
	 * Refresh this view.
	 */
	public void refresh() ;
	
}
