package com.peaceray.quantro.view.button.strip;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.R;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;
import com.peaceray.quantro.view.button.popup.QuantroButtonPopup;
import com.peaceray.quantro.view.button.popup.QuantroButtonPopupWrapper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

public class CustomButtonStrip extends QuantroButtonStrip
		implements QuantroButtonStrip.Controller {

	public interface Delegate {
		/**
		 * The user has short-clicked a button we for game launches.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param key
		 */
		public boolean customButtonStrip_onButtonClick(
				CustomButtonStrip strip, int buttonNum, String name, boolean asPopupItem ) ;
		
		/**
		 * The user has long-clicked a button we for game launches.  The
		 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
		 * the right action.
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param key
		 */
		public boolean customButtonStrip_onButtonLongClick(
				CustomButtonStrip strip, int buttonNum, String name ) ;
		
		
		/**
		 * Without side effects, returns the result of onButtonLongClick
		 * 
		 * @param strip
		 * @param buttonNum
		 * @param buttonType
		 * @param key
		 */
		public boolean customButtonStrip_supportsLongClick(
				CustomButtonStrip strip, int buttonNum, String name ) ;
		
		
		/**
		 * The user has opened the overflow menu.
		 * @param strip
		 * @return
		 */
		public void customButtonStrip_onPopupOpen(
				CustomButtonStrip strip ) ;
	}
	
	
	@SuppressWarnings("unused")
	private static final String TAG = "CustomButtonStrip" ;
	
	public CustomButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	public CustomButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	
	public void constructor_setDefaultValues( Context context ) {
		mwrDelegate = new WeakReference<Delegate>(null) ;
		
		mName = new String[0] ;
		mNameArray = new String[0][] ;
		mTitle = new String[0] ;
		mTitleArray = new String[0][] ;
		mDescription = new String[0] ;
		mColor = new int[0] ;
		mImageDrawable = new Drawable[0] ;
		
		mPopupWrapper = new QuantroButtonPopupWrapper[0] ;
		
		mAutoRefresh = true ;
		
		// Self-control.
		setController(this) ;
	}
	
	public void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomButtonStrip) ;
		
		// temporarily disable auto-refresh
		boolean wasAuto = mAutoRefresh ;
		setAutoRefresh(false) ;
		
		int num ;
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			
			num = buttonNumFromAttr( attr ) ;
			
			switch( attr ) {
			case R.styleable.CustomButtonStrip_cbs_button_0_name:
			case R.styleable.CustomButtonStrip_cbs_button_1_name:
			case R.styleable.CustomButtonStrip_cbs_button_2_name:
			case R.styleable.CustomButtonStrip_cbs_button_3_name:
			case R.styleable.CustomButtonStrip_cbs_button_4_name:
			case R.styleable.CustomButtonStrip_cbs_button_5_name:	
				setName( num, a.getString(attr) ) ;
				break ;
			case R.styleable.CustomButtonStrip_cbs_button_0_title:
			case R.styleable.CustomButtonStrip_cbs_button_1_title:
			case R.styleable.CustomButtonStrip_cbs_button_2_title:
			case R.styleable.CustomButtonStrip_cbs_button_3_title:
			case R.styleable.CustomButtonStrip_cbs_button_4_title:
			case R.styleable.CustomButtonStrip_cbs_button_5_title:
				setTitle( num, a.getString(attr) ) ;
				break ;
			case R.styleable.CustomButtonStrip_cbs_button_0_description:
			case R.styleable.CustomButtonStrip_cbs_button_1_description:
			case R.styleable.CustomButtonStrip_cbs_button_2_description:
			case R.styleable.CustomButtonStrip_cbs_button_3_description:
			case R.styleable.CustomButtonStrip_cbs_button_4_description:
			case R.styleable.CustomButtonStrip_cbs_button_5_description:
				setDescription( num, a.getString(attr) ) ;
				break ;
			case R.styleable.CustomButtonStrip_cbs_button_0_color:
			case R.styleable.CustomButtonStrip_cbs_button_1_color:
			case R.styleable.CustomButtonStrip_cbs_button_2_color:
			case R.styleable.CustomButtonStrip_cbs_button_3_color:
			case R.styleable.CustomButtonStrip_cbs_button_4_color:
			case R.styleable.CustomButtonStrip_cbs_button_5_color:
				setColor( num, a.getColor(attr, 0xffff00ff) ) ;
				break ;
			case R.styleable.CustomButtonStrip_cbs_button_0_image_drawable:
			case R.styleable.CustomButtonStrip_cbs_button_1_image_drawable:
			case R.styleable.CustomButtonStrip_cbs_button_2_image_drawable:
			case R.styleable.CustomButtonStrip_cbs_button_3_image_drawable:
			case R.styleable.CustomButtonStrip_cbs_button_4_image_drawable:
			case R.styleable.CustomButtonStrip_cbs_button_5_image_drawable:	
				setImageDrawable( num, a.getDrawable(attr) ) ;
				break ;
			case R.styleable.CustomButtonStrip_cbs_button_0_names_array:
			case R.styleable.CustomButtonStrip_cbs_button_1_names_array:
			case R.styleable.CustomButtonStrip_cbs_button_2_names_array:
			case R.styleable.CustomButtonStrip_cbs_button_3_names_array:
			case R.styleable.CustomButtonStrip_cbs_button_4_names_array:
			case R.styleable.CustomButtonStrip_cbs_button_5_names_array:
				setNames( num, getResources().getStringArray( a.getResourceId(attr, 0) ) ) ;
				break ;
			case R.styleable.CustomButtonStrip_cbs_button_0_titles_array:
			case R.styleable.CustomButtonStrip_cbs_button_1_titles_array:
			case R.styleable.CustomButtonStrip_cbs_button_2_titles_array:
			case R.styleable.CustomButtonStrip_cbs_button_3_titles_array:
			case R.styleable.CustomButtonStrip_cbs_button_4_titles_array:
			case R.styleable.CustomButtonStrip_cbs_button_5_titles_array:
				setTitles( num, getResources().getStringArray( a.getResourceId(attr, 0) ) ) ;
				break ;
			}
		}
		
		// re-enable auto-refresh, if necessary
		setAutoRefresh( wasAuto ) ;
		
		// recycle the array; we don't need it anymore.
		a.recycle() ;
	}
	
	
	
	private int buttonNumFromAttr( int attr ) {
		switch( attr ) {
		case R.styleable.CustomButtonStrip_cbs_button_0_name:
		case R.styleable.CustomButtonStrip_cbs_button_0_title:
		case R.styleable.CustomButtonStrip_cbs_button_0_description:
		case R.styleable.CustomButtonStrip_cbs_button_0_color:
		case R.styleable.CustomButtonStrip_cbs_button_0_image_drawable:
		case R.styleable.CustomButtonStrip_cbs_button_0_names_array:
		case R.styleable.CustomButtonStrip_cbs_button_0_titles_array:
			return 0 ;
		case R.styleable.CustomButtonStrip_cbs_button_1_name:
		case R.styleable.CustomButtonStrip_cbs_button_1_title:
		case R.styleable.CustomButtonStrip_cbs_button_1_description:
		case R.styleable.CustomButtonStrip_cbs_button_1_color:
		case R.styleable.CustomButtonStrip_cbs_button_1_image_drawable:
		case R.styleable.CustomButtonStrip_cbs_button_1_names_array:
		case R.styleable.CustomButtonStrip_cbs_button_1_titles_array:
			return 1 ;
		case R.styleable.CustomButtonStrip_cbs_button_2_name:
		case R.styleable.CustomButtonStrip_cbs_button_2_title:
		case R.styleable.CustomButtonStrip_cbs_button_2_description:
		case R.styleable.CustomButtonStrip_cbs_button_2_color:
		case R.styleable.CustomButtonStrip_cbs_button_2_image_drawable:
		case R.styleable.CustomButtonStrip_cbs_button_2_names_array:
		case R.styleable.CustomButtonStrip_cbs_button_2_titles_array:
			return 2 ;
		case R.styleable.CustomButtonStrip_cbs_button_3_name:
		case R.styleable.CustomButtonStrip_cbs_button_3_title:
		case R.styleable.CustomButtonStrip_cbs_button_3_description:
		case R.styleable.CustomButtonStrip_cbs_button_3_color:
		case R.styleable.CustomButtonStrip_cbs_button_3_image_drawable:
		case R.styleable.CustomButtonStrip_cbs_button_3_names_array:
		case R.styleable.CustomButtonStrip_cbs_button_3_titles_array:
			return 3 ;
		case R.styleable.CustomButtonStrip_cbs_button_4_name:
		case R.styleable.CustomButtonStrip_cbs_button_4_title:
		case R.styleable.CustomButtonStrip_cbs_button_4_description:
		case R.styleable.CustomButtonStrip_cbs_button_4_color:
		case R.styleable.CustomButtonStrip_cbs_button_4_image_drawable:
		case R.styleable.CustomButtonStrip_cbs_button_4_names_array:
		case R.styleable.CustomButtonStrip_cbs_button_4_titles_array:
			return 4 ;
		case R.styleable.CustomButtonStrip_cbs_button_5_name:
		case R.styleable.CustomButtonStrip_cbs_button_5_title:
		case R.styleable.CustomButtonStrip_cbs_button_5_description:
		case R.styleable.CustomButtonStrip_cbs_button_5_color:
		case R.styleable.CustomButtonStrip_cbs_button_5_image_drawable:
		case R.styleable.CustomButtonStrip_cbs_button_5_names_array:
		case R.styleable.CustomButtonStrip_cbs_button_5_titles_array:
			return 5 ;
		
		}
		
		return -1 ;
	}
	
	@Override
	protected void onFinishInflate () {
		Log.d(TAG, "onFinishInflate: title length is " + mTitle.length + " with num buttons " + getButtonCount()) ;
		super.onFinishInflate() ;
		// add buttons based on custom data
		while ( getButtonCount() < mTitle.length )
			addButton() ;
		// by default, all buttons are disabled but visible.
		for ( int i = 0; i < getButtonCount(); i++ ) {
			this.setButtonEnabled(i, false) ;
			this.setButtonVisible(i, true) ;
		}
	}
	
	
	private WeakReference<Delegate> mwrDelegate = null ;
	
	private String [] mName ;
	
	
	private String [] mTitle ;
	private String [] mDescription ;
	private int [] mColor ;
	private Drawable [] mImageDrawable ;
	
	private String [][] mTitleArray ;
	private String [][] mNameArray ;
	private QuantroButtonPopupWrapper [] mPopupWrapper ;
	
	private boolean mAutoRefresh ;
	
	
	private void extendStripToIncludeButton( int button ) {
		int buttonsNeeded =  button - mTitle.length + 1 ;
		if ( buttonsNeeded > 0 ) {
			mName = extendBy( mName, buttonsNeeded ) ;
			mNameArray = extendBy( mNameArray, buttonsNeeded ) ;
			mTitle = extendBy( mTitle, buttonsNeeded ) ;
			mTitleArray = extendBy( mTitleArray, buttonsNeeded ) ;
			mDescription = extendBy( mDescription, buttonsNeeded ) ;
			mColor = extendBy( mColor, buttonsNeeded ) ;
			mImageDrawable = extendBy( mImageDrawable, buttonsNeeded ) ;
			mPopupWrapper = extendBy( mPopupWrapper, buttonsNeeded ) ;
		}
	}
	
	private String [][] extendBy( String [][] a, int extra ) {
		String [][] b = new String[a.length + extra][] ;
		for ( int i = 0; i < a.length; i++ )
			b[i] = a[i] ;
		for ( int i = a.length; i < b.length; i++ ) 
			b[i] = null ;
		return b ; 
	}
	
	private String [] extendBy( String [] a, int extra ) {
		String [] b = new String[a.length + extra] ;
		for ( int i = 0; i < a.length; i++ )
			b[i] = a[i] ;
		for ( int i = a.length; i < b.length; i++ ) 
			b[i] = null ;
		return b ; 
	}
	
	private int [] extendBy( int [] a, int extra ) {
		int [] b = new int[a.length + extra] ;
		for ( int i = 0; i < a.length; i++ )
			b[i] = a[i] ;
		for ( int i = a.length; i < b.length; i++ ) 
			b[i] = 0 ;
		return b ; 
	}
	
	private Drawable [] extendBy( Drawable [] a, int extra ) {
		Drawable [] b = new Drawable[a.length + extra] ;
		for ( int i = 0; i < a.length; i++ )
			b[i] = a[i] ;
		for ( int i = a.length; i < b.length; i++ ) 
			b[i] = null ;
		return b ; 
	}
	
	private boolean [] extendBy( boolean [] a, int extra, boolean fill ) {
		boolean [] b = new boolean[a.length + extra] ;
		for ( int i = 0; i < a.length; i++ )
			b[i] = a[i] ;
		for ( int i = a.length; i < b.length; i++ ) 
			b[i] = fill ;
		return b ; 
	}
	
	private QuantroButtonPopupWrapper [] extendBy( QuantroButtonPopupWrapper [] a, int extra ) {
		QuantroButtonPopupWrapper [] b = new QuantroButtonPopupWrapper[a.length + extra] ;
		for ( int i = 0; i < a.length; i++ )
			b[i] = a[i] ;
		for ( int i = a.length; i < b.length; i++ ) 
			b[i] = null ;
		return b ; 
	}
	
	public void setDelegate( Delegate delegate ) {
		mwrDelegate = new WeakReference<Delegate>(delegate) ;
	}
	
	
	public CustomButtonStrip setAutoRefresh( boolean refresh ) {
		mAutoRefresh = refresh ;
		return this ;
	}
	
	public CustomButtonStrip setName( int button, String name ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		mName[button] = name ;
		return this; 
	}
	
	public String getName( int button ) {
		if ( button < 0 )
			return null ; 
		return mName[button] ;
	}
	
	public int getButton( String name ) {
		if ( name == null )
			return -1 ;
		
		for ( int i = 0; i < mName.length; i++ )
			if ( name.equals(mName[i]) )
				return i ;
		
		return -1 ;
	}
	
	public CustomButtonStrip setTitle( int button, String title ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		mTitle[button] = title ;
		if ( mAutoRefresh )
			refresh() ;
		return this; 
	}
	
	public CustomButtonStrip setDescription( int button, String desc ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		mDescription[button] = desc ;
		if ( mAutoRefresh )
			refresh() ;
		return this; 
	}
	
	public CustomButtonStrip setColor( int button, int color ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		mColor[button] = color ;
		if ( mAutoRefresh )
			refresh() ;
		return this; 
	}
	
	public CustomButtonStrip setImageDrawable( int button, Drawable img ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		mImageDrawable[button] = img ;
		if ( mAutoRefresh )
			refresh() ;
		return this; 
	}
	
	public CustomButtonStrip setNames( int button, String [] names ) {
		if ( button < 0 )
			return this ;
		extendStripToIncludeButton(button) ;
		mNameArray[button] = names ;
		if ( mAutoRefresh )
			refresh() ;
		return this ;
	}
	
	public CustomButtonStrip setTitles( int button, String [] titles ) {
		if ( button < 0 )
			return this ;
		extendStripToIncludeButton(button) ;
		mTitleArray[button] = titles ;
		if ( mAutoRefresh )
			refresh() ;
		return this ;
	}
	
	public CustomButtonStrip setEnabled( int button, boolean enabled ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		setButtonEnabled( button, enabled ) ;
		if ( mAutoRefresh )
			refresh() ;
		return this; 
	}
	
	public CustomButtonStrip setVisible( int button, boolean visible ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		setButtonVisible( button, visible ); 
		if ( mAutoRefresh )
			refresh() ;
		return this; 
	}
	
	public CustomButtonStrip setActive( int button ) {
		if ( button < 0 )
			return this ; 
		extendStripToIncludeButton(button) ;
		setButtonEnabled( button, true ) ;
		setButtonVisible( button, true ); 
		if ( mAutoRefresh )
			refresh() ;
		return this; 
	}
	
	@Override
	public void refresh() {
		boolean changed = false ;
		
		if ( getButtonCount() < mTitle.length )
			throw new IllegalStateException("Don't have enough buttons") ;
		
		for ( int i = 0; i < getButtonCount(); i++ )
			changed = setButtonContent(i) || changed ;
		
		if ( changed )
			super.refresh() ;
	}
	
	
	public boolean setButtonContent( int index ) {
		boolean changed = false ;
		
		QuantroButtonAccess qbs = getButtonAccess(index) ;
		
		if ( !this.getButtonViewVisibilityIsSet(index) )
			changed = true ;
		
		
		// Now set these values in the appropriate content fields.
		changed = qbs.setTitle( mTitle[index] ) || changed ;
		changed = qbs.setDescription( mDescription[index] ) || changed ;
		
		// background and image drawable
		/* changed = qbc.setBackground(backgroundDrawable) || changed ; */
		changed = qbs.setImage( true, mImageDrawable[index] ) || changed ;
		
		// set the button base color
		changed = qbs.setColor( mColor[index] ) || changed ;
		
		// set content alpha IF this is not the main button.
		if ( index != 0 ) {
			changed = qbs.setContentAlpha( getButtonIsEnabled(index) ? 255 : 128 )
					|| changed ;
		}
		
		// Popup Menu.
		if ( mTitleArray[index] != null && mNameArray[index] != null ) {
			// find the default option...
			int option = 0 ;
			for ( int i = 0; i < mNameArray[index].length; i++ ) {
				if ( mName[index].equals(mNameArray[index][i]) ) {
					option = i ;
				}
			}
			// make or update a popup.
			if ( mPopupWrapper[index] == null ) {
				mPopupWrapper[index] = QuantroButtonPopup.wrap(
						getButtonDirectReference(index), this, option,
						mTitleArray[index], mNameArray[index]) ;
			} else {
				mPopupWrapper[index] = QuantroButtonPopup.update(
						mPopupWrapper[index], option,
						mTitleArray[index], mNameArray[index]) ;
			}
		} else {
			if ( mPopupWrapper[index] != null ) {
				mPopupWrapper[index] = null ;
				this.resetAsListener(index) ;
			}
		}
		
		
		if ( !this.getButtonViewEnabledIsSet(index) )
			changed = true ;
		
		return changed ;
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null && getButtonIsEnabled(buttonNum) ) 
			delegate.customButtonStrip_onButtonClick(this, buttonNum, mName[buttonNum], asOverflow) ;
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null && getButtonIsEnabled(buttonNum) ) { 
			return delegate.customButtonStrip_onButtonLongClick(this, buttonNum, mName[buttonNum]) ;
		}
		
		return false;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null && getButtonIsEnabled(buttonNum) ) { 
			return delegate.customButtonStrip_supportsLongClick(this, buttonNum, mName[buttonNum]) ;
		}
		
		return false;
	}
	
	@Override
	public void qbsc_onOverflowClicked( QuantroButtonStrip strip ) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null ) 
			delegate.customButtonStrip_onPopupOpen(this) ;
	}
	
	
	@Override
	public void qbpw_onButtonUsed(QuantroContentWrappingButton button, boolean asPopup,
			int menuItem, Object menuItemTag) {
		// if this is a standand button, handle it.  Otherwise, pass to superclass.
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate != null ) {
			for ( int i = 0; i < getButtonCount(); i++ ) {
				if ( getButtonDirectReference(i) == button ) {
					delegate.customButtonStrip_onButtonClick(this, i, (String)menuItemTag, asPopup) ;
					return ;
				}
			}
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
					delegate.customButtonStrip_onPopupOpen(this) ;
					return ;
				}
			}
		}
		super.qbpw_onMenuOpened(button) ;
	}

}
