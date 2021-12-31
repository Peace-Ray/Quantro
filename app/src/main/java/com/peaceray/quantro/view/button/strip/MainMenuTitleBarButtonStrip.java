package com.peaceray.quantro.view.button.strip;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.peaceray.quantro.R;
import com.peaceray.quantro.model.game.GameBlocksSliceSequence;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;
import com.peaceray.quantro.view.button.popup.QuantroButtonPopup;
import com.peaceray.quantro.view.button.popup.QuantroButtonPopupWrapper;
import com.peaceray.quantro.view.button.strip.CustomButtonStrip.Delegate;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.SliceSequenceView;


/**
 * 
 * This title bar button strip rests atop the main menu list.  It has
 * several important functions.
 * 
 * 1. It shows the title of the currently displayed menu item.  At root,
 * 		this is probably something like "Quantro" or "Quantro XL."
 * 
 * 2. It (optionally) animates the transition between title texts.
 * 
 * 3. It shows the Quantro logo as a button.  For free players, it also
 * 		displays an "unlock" button.
 * 
 * Change: as part of a refactor, I am internalizing a lot of the MainMenuButtonStrip
 * functionality.  We've pretty much settled on a button sequence.
 *		
 * @author Jake
 *
 */
public class MainMenuTitleBarButtonStrip extends QuantroButtonStrip implements QuantroButtonStrip.Controller {

	
	public static final String TAG = "MMTBButtonStrip" ;
	
	public interface Delegate {
		/**
		 * The user has short-clicked a button on this strip.  If buttonNum == 0,
		 * be assured that it is the main button.  If > 0, it is some other button,
		 * and as mentioned in the class description, the content and effect are
		 * completely up to the outside world to determine.  If you need to check
		 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
		 * 
		 * The menuItemNumber provided is the currently displayed menu item
		 * according to the strip's records.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param menuItemNumber
		 */
		public boolean mmtbbs_onButtonClick(
				MainMenuTitleBarButtonStrip strip,
				int buttonType, int menuItemNumber, boolean asOverflow ) ;
		
		/**
		 * The user has long-clicked a button on this strip.  If buttonNum == 0,
		 * be assured that it is the main button.  If > 0, it is some other button,
		 * and as mentioned in the class description, the content and effect are
		 * completely up to the outside world to determine.  If you need to check
		 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
		 * 
		 * The menuItemNumber provided is the currently displayed menu item
		 * according to the strip's records.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param menuItemNumber
		 */
		public boolean mmtbbs_onButtonLongClick(
				MainMenuTitleBarButtonStrip strip,
				int buttonType, int menuItemNumber ) ;
		
		
		/**
		 * Returns the result of onButtonLongClick without side effects.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param menuItemNumber
		 */
		public boolean mmtbbs_supportsLongClick(
				MainMenuTitleBarButtonStrip strip,
				int buttonType, int menuItemNumber ) ;
		
		
		/**
		 * The user has opened the overflow menu.
		 * @param strip
		 */
		public void mmtbbs_onPopupOpened( MainMenuTitleBarButtonStrip strip ) ;
	}
	
	
	public MainMenuTitleBarButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	public MainMenuTitleBarButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	
	// For now, we need only 1 button.
	private static final int MIN_BUTTONS = 2 ;
	
	public static final int BUTTON_TYPE_MAIN = 0 ;
	public static final int BUTTON_TYPE_QUANTRO = 1 ;
	public static final int BUTTON_TYPE_ACHIEVEMENTS = 2 ;
	public static final int BUTTON_TYPE_LEADERBOARDS = 3 ;
	public static final int BUTTON_TYPE_HELP = 4 ;
	public static final int BUTTON_TYPE_SETTINGS = 5 ;
	
	
	
	
	// Unlike most button strips, we allow a significant degree
	// of outside configuration for this strip, since we expect
	// a dedicated Activity simply for constructing and configuring
	// these views.  As such, we allow various arrays (indexed by
	// item number) for setting title, descriptions, and other content
	// --pending.  Additionally, while most ButtonStrips assume that
	// users will not directly access or alter button content, this
	// Strip is based on the explicit assumption that content and settings for
	// buttons 1...N are controlled from outside.
	
	private int mMenuItem ;
	private String [] mMenuItemTitle ;
	private String [] mMenuItemDescription ;
	private int [] mMenuItemColor ;
	private Drawable [] mMenuItemDrawable ;
	
	// The listener!
	private WeakReference<Delegate> mwrDelegate ;
	
	private boolean mPremiumIsUnlocked ;
	private QuantroButtonPopupWrapper mLeaderboardPopupWrapper = null ;
	
	private void constructor_setDefaultValues(Context context) {
		// self control
		this.setController(this) ;
		
		mMenuItem = -1 ;
		mMenuItemTitle = null ;
		mMenuItemDescription = null ;
		mMenuItemColor = null ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
		
		mPremiumIsUnlocked = false ;
		
		// We want onDraw to be called for this view.
		setWillNotDraw(false) ;
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		
		// TODO: Maybe allow our arrays and menu item number to be read in?
		// Note that we have already called our superclass stuff, so we have
		// all the QuantroButtonStrip attributes set.
		
	}
	
	
	synchronized public void setDelegate( MainMenuTitleBarButtonStrip.Delegate delegate ) {
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
	
	synchronized public void setMenuItemColors( int [] colors ) {
		mMenuItemColor = colors ;
	}
	
	synchronized public void setMenuItemDrawables( Drawable [] drawables ) {
		mMenuItemDrawable = drawables ;
	}
	
	synchronized public void setPremiumIsUnlocked( boolean unlocked ) {
		mPremiumIsUnlocked = unlocked ;
	}
	
	synchronized public void setSliceSequence( GameBlocksSliceSequence gbss, DrawSettings ds ) {
		setSliceSequence( gbss, gbss.cols(), gbss.rows(), ds ) ;
	}
	
	synchronized public void setSliceSequence( GameBlocksSliceSequence gbss, int contentCols, int contentRows, DrawSettings ds ) {
		QuantroContentWrappingButton qcwb = getButtonDirectReference(1) ;
    	
    	SliceSequenceView ssv = (SliceSequenceView)qcwb.getContentView().findViewById(R.id.button_content_slice_sequence_view) ;
    	ssv.setContentSizeInBlocks(contentCols, contentRows) ;
    	ssv.setSequence(ds, gbss) ;
    	// turn off the preview
    	ImageView iv = (ImageView)qcwb.getContentView().findViewById(R.id.button_content_image_drawable) ;
    	if ( iv != null )
    		iv.setVisibility(View.GONE) ;
	}
	
	synchronized public void setDrawSettings( DrawSettings ds ) {
		QuantroContentWrappingButton qcwb = getButtonDirectReference(1) ;
    	
		SliceSequenceView ssv = (SliceSequenceView)qcwb.getContentView().findViewById(R.id.button_content_slice_sequence_view) ;
		ssv.setDrawSettings( ds ) ; 
	}
	
	synchronized public void recycleBlockDrawerUntilDraw() {
		QuantroContentWrappingButton qcwb = getButtonDirectReference(1) ;
    	
		SliceSequenceView ssv = (SliceSequenceView)qcwb.getContentView().findViewById(R.id.button_content_slice_sequence_view) ;
    	ssv.recycleUntilDraw() ;
	}
	
	
	private int buttonTypeToIndex( int buttonType ) {
		switch( buttonType ) {
		case BUTTON_TYPE_MAIN:
			return 0;
		case BUTTON_TYPE_QUANTRO:
			return 1 ;
		}
		
		return -1 ;
	}
	
	private int buttonIndexToType( int buttonIndex ) {
		return buttonIndex ;
	}
	
	synchronized public void refresh() {
		boolean changed = false ;
		
		// Make sure we have enough buttons!
		while ( this.getButtonCount() < MIN_BUTTONS ) {
			changed = true ;
			addButton() ;
		}
		setButtonVisible(0, true) ;
		
		// Visiblity!
		for ( int i = 0; i < this.getButtonCount(); i++ ) {
			boolean shown = MIN_BUTTONS > i ;
			if ( getButtonIsVisible( i ) != shown ) {
				setButtonVisible(i, shown) ;
				changed = true ;
			}
		}
		
		// Simple enough.
		changed = setButtonContent( 0, BUTTON_TYPE_MAIN ) || changed ;
		changed = setButtonContent( 1, BUTTON_TYPE_QUANTRO ) || changed ;
		setButtonEnabled( 0, false ) ;
		
		if ( changed )
			super.refresh() ;
	}
	
	
	private boolean setButtonContent( int buttonNum, int buttonType ) {
		boolean changed = false ;
		
		// Since we only have one button whose content we set
		// - the main button - we actually
		// ignore both buttonNum and buttonType.
		
		Resources res = getResources() ;
		
		String title = null, description = null ;
		int color = Color.MAGENTA ;
		Drawable image = null ;
		
		// Right now, we only allow control of 1 button.  When we add more, this
		// method should change.
		
		switch( buttonType ) {
		case BUTTON_TYPE_MAIN:
			if ( mMenuItem < 0 ) {
				title = "No menu item set" ;
				description = "No menu item set" ;
				color = 0xffffffff ;
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
				if ( mMenuItemColor != null )
					color = mMenuItemColor[mMenuItem] ;
				else
					color = 0xffffffff ;
				if ( mMenuItemDrawable != null )
					image = mMenuItemDrawable[mMenuItem] ;
				else
					image = null ;
			}
			break ;
			
		case BUTTON_TYPE_QUANTRO:
			title = res.getString(R.string.main_menu_action_bar_quantro_title) ;
			color = Color.WHITE ;
			break ;

		case BUTTON_TYPE_LEADERBOARDS:
			title = res.getString(R.string.main_menu_action_bar_leaderboard_title) ;
			image = res.getDrawable(R.drawable.action_leaderboard_baked) ;
			color = res.getColor(R.color.main_menu_leaderboard) ;
			if ( this.mLeaderboardPopupWrapper == null ) {
				// make the wrapper.
				String [] titles = res.getStringArray(R.array.main_menu_action_bar_google_play_menu_titles) ;
				Object [] tags = new Object[] { TAG, TAG } ;
				int defaultOption = 0 ;
				for ( int i = 0; i < titles.length; i++ ) {
					if ( titles[i].equals(title) ) {
						defaultOption = i ;
					}
				}
				mLeaderboardPopupWrapper = QuantroButtonPopup.wrap(
						getButtonDirectReference(buttonNum), this,
						defaultOption, titles, tags) ;
			}
			break ;

		case BUTTON_TYPE_HELP:
			title = res.getString(R.string.main_menu_action_bar_help_title) ;
			image = res.getDrawable(R.drawable.action_help_baked) ;
			color = res.getColor(R.color.main_menu_help) ;
			break ;

		case BUTTON_TYPE_SETTINGS:
			title = res.getString(R.string.main_menu_action_bar_settings_title) ;
			image = res.getDrawable(R.drawable.action_settings_baked) ;
			color = res.getColor(R.color.main_menu_settings) ;
			break ;


		}
		
		QuantroButtonAccess qbs = getButtonAccess(buttonNum) ;
		
		//Log.d(TAG, "setting title " + title + " and desc " + description) ;
		changed = qbs.setTitle(title) || changed ;
		changed = qbs.setDescription(description) || changed ;
		changed = qbs.setColor(color) || changed ;
		changed = qbs.setImage(image != null, image)	|| changed ;
		
		setButtonEnabled( buttonNum, buttonType != BUTTON_TYPE_MAIN ) ;
		setButtonVisible( buttonNum, true ) ;
		changed = changed || !getButtonViewEnabledIsSet(buttonNum) ;
		return changed ;
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return ;		// we did not handle it
		
		if ( buttonNum < 1 )
			return ;
		
		delegate.mmtbbs_onButtonClick(this, buttonIndexToType(buttonNum), mMenuItem, asOverflow) ;
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;		// we did not handle it
		
		if ( buttonNum < 1 )
			return false ;
		
		return delegate.mmtbbs_onButtonLongClick(this, buttonIndexToType(buttonNum), mMenuItem) ;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		
		if ( delegate == null )
			return false ;		// we did not handle it
		
		if ( buttonNum < 1 )
			return false ;
		
		return delegate.mmtbbs_supportsLongClick(this, buttonIndexToType(buttonNum), mMenuItem) ;
	}
	
	@Override
	public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		delegate.mmtbbs_onPopupOpened(this) ;
	}
	
	@Override
	public void qbpw_onButtonUsed(QuantroContentWrappingButton button, boolean asPopup,
			int menuItem, Object menuItemTag) {
		// if this is a standand button, handle it.  Otherwise, pass to superclass.
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null && TAG.equals(menuItemTag) ) {
			delegate.mmtbbs_onButtonClick(this,
					BUTTON_TYPE_ACHIEVEMENTS + menuItem,		// number from Achievements on
					this.mMenuItem, true ) ;
			return ;
		}
		super.qbpw_onButtonUsed(button, asPopup, menuItem, menuItemTag) ;
	}
	
	@Override
	public void qbpw_onMenuOpened(QuantroContentWrappingButton button) {
		// if this is a standard button. handle it.  Otherwise, pass to superclass.
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null ) {
			for ( int i = 0; i < getButtonCount(); i++ ) {
				if ( getButtonDirectReference(i) == button ) {
					delegate.mmtbbs_onPopupOpened(this) ;
					return ;
				}
			}
		}
		super.qbpw_onMenuOpened(button) ;
	}
}
