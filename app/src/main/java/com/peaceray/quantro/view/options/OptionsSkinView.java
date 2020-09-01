package com.peaceray.quantro.view.options;

import java.util.Collection;
import java.util.Map;

import com.peaceray.quantro.content.Skin;


/**
 * The OptionsSkinView represents in-game options for selecting the current
 * skin.  When and if skin-shuffling becomes available, this will be represented
 * here as well.
 * 
 * As a View, this class does not attempt to read current skin settings, nor
 * make changes to them.  Those things are handled by mutator methods and
 * delegate calls.
 * 
 * @author Jake
 *
 */
public interface OptionsSkinView {
	
	public interface Delegate {
		
		/**
		 * The user is attempting to set the provided Skin as the current
		 * skin used for gameplay.  The OptionsSkinView (provided) will
		 * not take any action unless instructed to by the delegate.
		 * 
		 * One example for behavior is to call osv.setCurrentSkin( skin ),
		 * which informs the view that the user selection was successful.
		 * 
		 * The return value of this method will be taken as a shorthand for
		 * the suggested call.  If 'true' is returned, osv will respond by
		 * calling osv.setCurrentSkin( skin ) on itself.
		 * 
		 * @param view
		 * @param skin
		 * @param availability As a reminder, this is the currently set
		 * 			OptionAvailability for this skin.
		 * @return
		 */
		public void osvd_userSetCurrentSkin( OptionsSkinView osv, Skin skin, OptionAvailability availability ) ;
		
		
		/**
		 * The user wants to perform advance configuration.  We have made no internal
		 * changes.
		 * 
		 * @param osmv
		 */
		public void osvd_userAdvancedConfiguration( OptionsSkinView osvd ) ;
		
	}
	
	
	
	/**
	 * Sets this View's Delegate.  May be called with 'null'.
	 * 
	 * @param delegate An implementation of Delegate.
	 */
	public void setDelegate( Delegate delegate ) ;
	
	
	/**
	 * Clear all skins set in this view.
	 */
	public void clearSkins() ;

	
	/**
	 * Adds the specified Skin to those displayed by the Options view.
	 * 'unlocked' indicates whether this skin is available.
	 * 
	 * @param skin
	 * @param unlocked
	 */
	public void addSkin( Skin skin, OptionAvailability availability ) ;
	
	
	/**
	 * Sets the specified skins and availability as the set displayed by this
	 * Options view.
	 * 
	 * Equivalent to calling clearSkins(), then addSkin() in succession.
	 * 
	 * @param skins
	 * @param availability
	 */
	public void setSkins( Collection<Skin> skins, Map<Skin, OptionAvailability> availability ) ;
	
	
	/**
	 * Sets the availability of the specified skin, which must have been 
	 * previously added.
	 * 
	 * @param skin
	 * @param availability
	 */
	public void setSkinAvailability( Skin skin, OptionAvailability availability ) ;
	
	
	/**
	 * Sets the availability of the provided skins, which must have been
	 * previously added.
	 * 
	 * @param availability
	 */
	public void setSkinAvailability( Map<Skin, OptionAvailability> availability ) ;
	
	
	/**
	 * Sets the skin currently selected by the user, either in response to
	 * user actions or via loading parameters.  Note that this OptionsView
	 * is meant to allow users to select options; it does not perform any
	 * underlying changes.  That happens through the Delegate, which (one
	 * presumes) is the same entity calling this method.
	 * 
	 * @param skin
	 */
	public void setCurrentSkin( Skin skin ) ;
}
