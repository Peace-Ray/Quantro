package com.peaceray.quantro.view.button.strip;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class MainMenuButtonStrip extends QuantroButtonStrip implements QuantroButtonStrip.Controller {

	public interface Delegate {
		/**
		 * The user has short-clicked a button on this strip.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.  We also include the menu item number represented here.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param menuItemNumber
		 */
		public boolean mmbs_onButtonClick(
				MainMenuButtonStrip strip,
				int buttonNum, int buttonType, int menuItemNumber, boolean buttonEnabled ) ;
		
		/**
		 * The user has long-clicked a button we for game launches.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.  We also include the menu item number represented here.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param menuItemNumber
		 */
		public boolean mmbs_onButtonLongClick(
				MainMenuButtonStrip strip,
				int buttonNum, int buttonType, int menuItemNumber, boolean buttonEnabled ) ;
		
		
		/**
		 * Without side effects, returns the result of onButtonLongClick.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param menuItemNumber
		 */
		public boolean mmbs_supportsLongClick(
				MainMenuButtonStrip strip,
				int buttonNum, int buttonType, int menuItemNumber, boolean buttonEnabled ) ;
		
		
		
	}
	
	
	public MainMenuButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	public MainMenuButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	
	// For now, we need only 1 button.
	private static final int MIN_BUTTONS = 1 ;
	
	public static final int BUTTON_TYPE_MAIN = 0 ;
	
	public static final int NUM_BUTTON_TYPES = 1 ;
	
	
	public static final int ALERT_NONE = 0 ;
	public static final int ALERT_MINOR = 1 ;
	public static final int ALERT_MAJOR = 2 ;
	public static final int NUM_ALERTS = 3 ;
	
	
	
	// Unlike most button strips, we allow a significant degree
	// of outside configuration for this strip, since we expect
	// a dedicated Activity simply for constructing and configuring
	// these views.  As such, we allow various arrays (indexed by
	// item number) for setting title, descriptions, and other content
	// --pending.
	//
	// By keeping a reference to these objects, the Activity can make
	// easy changes to displayed contents and then simply call "refresh"
	// on all button strips.
	
	private int mMenuItem ;
	private String [] mMenuItemTitle ;
	private String [] mMenuItemDescription ;
	private String [] mMenuItemDescriptionDisabled ;
	private int [] mMenuItemColor ;
	private int [] mMenuItemAlert ;
	private Drawable [] mMenuItemAlertDefaultDrawable ;
	
	private Drawable [] mAlertDrawable ;
	private int [] mAlertColor ;
	
	// The listener!
	private WeakReference<Delegate> mwrDelegate ;
	
	private static final int ENABLED_ALPHA = 255 ;
	private static final int DISABLED_ALPHA = 128 ;
	
	
	private void constructor_setDefaultValues(Context context) {
		// I'm my own grandpa
		this.setController(this) ;
		
		mMenuItem = -1 ;
		mMenuItemTitle = null ;
		mMenuItemDescription = null ;
		mMenuItemDescriptionDisabled = null ;
		mMenuItemColor = null ;
		mMenuItemAlertDefaultDrawable = null ;
		
		Resources res = context.getResources() ;
		mAlertDrawable = new Drawable[NUM_ALERTS] ;
		Drawable alert = res.getDrawable(R.drawable.icon_alert_baked) ;
		mAlertDrawable[ALERT_NONE] = null ;
		mAlertDrawable[ALERT_MINOR] = alert ;
		mAlertDrawable[ALERT_MAJOR] = alert ;
		mAlertColor = new int[NUM_ALERTS] ;
		mAlertColor[ALERT_NONE] = Color.WHITE ;
		mAlertColor[ALERT_MINOR] = res.getColor(R.color.main_menu_alert_minor) ;
		mAlertColor[ALERT_MAJOR] = res.getColor(R.color.main_menu_alert_major) ;
		
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		
		// TODO: Maybe allow our arrays and menu item number to be read in?
		// Note that we have already called our superclass stuff, so we have
		// all the QuantroButtonStrip attributes set.
		
	}
	
	synchronized public void setDelegate( MainMenuButtonStrip.Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	synchronized public void setMenuItem( int item ) {
		boolean needsRefresh = mMenuItem != item ;
		mMenuItem = item ;
		if ( needsRefresh )
			refresh() ;
	}
	
	synchronized public void setMenuItemTitles( String [] titles ) {
		mMenuItemTitle = titles ;
	}
	
	synchronized public void setMenuItemDescriptions( String [] descs ) {
		mMenuItemDescription = descs ;
	}
	
	synchronized public void setMenuItemDescriptionsDisabled( String [] descs ) {
		mMenuItemDescriptionDisabled = descs ;
	}
	
	synchronized public void setMenuItemColors( int [] colors ) {
		mMenuItemColor = colors ;
	}
	
	synchronized public void setMenuItemAlerts( int [] alerts ) {
		mMenuItemAlert = alerts ;
	}
	
	synchronized public void setMenuItemAlertDefaultDrawable( Drawable [] drawables ) {
		mMenuItemAlertDefaultDrawable = drawables ;
	}
	
	synchronized public void refresh() {
		boolean changed = false ;
		
		// Make sure we have enough buttons!
		while ( this.getButtonCount() < MIN_BUTTONS ) {
			changed = true ;
			addButton() ;
		}
		
		// Simple enough.
		changed = setButtonContent( 0, BUTTON_TYPE_MAIN ) || changed ;
		
		if ( changed )
			super.refresh() ;
	}
	
	
	synchronized public void setMenuItemEnabled( boolean enabled ) {
		this.setButtonEnabled(0, enabled) ;
	}
	
	
	private boolean setButtonContent( int buttonNum, int buttonType ) {
		boolean changed = false ;
		
		// Since we only have one button - the main button - we actually
		// ignore both buttonNum and buttonType.
		
		String title, description, descriptionDisabled ;
		int color ;
		boolean showAlert ;
		Drawable alert ;
		int alertColor ;
		
		// Right now, we only allow 1 button.  When we add more, 
		
		if ( mMenuItem < 0 ) {
			title = "No menu item set" ;
			description = "No menu item set" ;
			descriptionDisabled = "No menu item set" ;
			color = 0xffffffff ;
			alert = null ;
			alertColor = 0x00000000 ;
		}
		else {
			if ( mMenuItemTitle != null )
				title = mMenuItemTitle[mMenuItem] ;
			else
				title = "mMenuItemTitle not set" ;
			if ( mMenuItemDescription != null )
				description = mMenuItemDescription[mMenuItem] ;
			else
				description = "mMenuItemDescription not set" ;
			if ( mMenuItemDescriptionDisabled != null && mMenuItemDescriptionDisabled[mMenuItem] != null )
				descriptionDisabled = mMenuItemDescriptionDisabled[mMenuItem] ;
			else 
				descriptionDisabled = description ;
			if ( mMenuItemColor != null )
				color = mMenuItemColor[mMenuItem] ;
			else
				color = 0xffffffff ;
			if ( mMenuItemAlert != null && mMenuItemAlert[mMenuItem] != ALERT_NONE ) {
				alert = mAlertDrawable[mMenuItemAlert[mMenuItem]] ;
				alertColor = mAlertColor[mMenuItemAlert[mMenuItem]] ;
			} else if ( mMenuItemAlertDefaultDrawable != null && mMenuItemAlertDefaultDrawable[mMenuItem] != null ) {
				alert = mMenuItemAlertDefaultDrawable[mMenuItem] ;
				alertColor = Color.WHITE ;
			} else {
				alert = null ;
				alertColor = 0x00000000 ;
			}
		}
		
		showAlert = alert != null ;
		
		QuantroButtonAccess qbs = getButtonAccess(buttonNum) ;
		
		changed = qbs.setTitle(title) || changed ;
		changed = qbs.setDescription( getButtonIsEnabled(buttonNum) ? description : descriptionDisabled ) || changed ;
		changed = qbs.setColor(color) || changed ;
		changed = qbs.setAlert(showAlert, alert, alertColor) || changed ;
		changed = qbs.setContentAlpha( getButtonIsEnabled(buttonNum) ? ENABLED_ALPHA : DISABLED_ALPHA ) || changed ;
		
		// Log.d(TAG, "setting alert for " + mMenuItem + " to " + alert + " with color " + alertColor) ;
 		
		return changed ;
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;		// we did not handle it
		if ( buttonNum == 0 ) {
			delegate.mmbs_onButtonClick(this, 0, BUTTON_TYPE_MAIN, mMenuItem, getButtonIsEnabled(buttonNum)) ;
		}
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;		// we did not handle it
		if ( buttonNum == 0 ) {
			return delegate.mmbs_onButtonLongClick(this, 0, BUTTON_TYPE_MAIN, mMenuItem, getButtonIsEnabled(buttonNum) ) ;
		}
		
		// Didn't handle it.
		return false ;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;		// we did not handle it
		if ( buttonNum == 0 ) {
			return delegate.mmbs_supportsLongClick(this, 0, BUTTON_TYPE_MAIN, mMenuItem, getButtonIsEnabled(buttonNum) ) ;
		}
		
		// Didn't handle it.
		return false ;
	}
	
	public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) {
		// nothing
	}
	
}
