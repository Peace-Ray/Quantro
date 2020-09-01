package com.peaceray.quantro.view.button.strip;

import com.peaceray.quantro.R ;
import com.peaceray.quantro.view.button.content.QuantroButtonAccess;
import com.peaceray.quantro.view.drawable.LineShadingDrawable;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

/**
 * An AlertDialogButtonStrip is meant to help bridge the gap between
 * custom buttons and such and default dialogs.  Unlike most button strips,
 * there is no dedicated Listener interface; instead, we accept DialogInterface.
 * OnClickListeners.
 * 
 * Most other methods are meant for easy translation from AlertDialog button
 * access methods.
 * 
 * @author Jake
 *
 */
public class DialogButtonStrip extends QuantroButtonStrip implements QuantroButtonStrip.Controller {

	private static final String TAG = "DialogButtonStrip" ;
	
	
	public DialogButtonStrip(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	public DialogButtonStrip(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
	}
	
	
	private void constructor_setDefaultValues(Context context) {
		
		// self-control
		this.setController(this) ;
		
		Resources res = context.getResources() ;
		
		mAutoRefresh = true ;
		
		// allocate space
		mButtonDefaultColor = new int[3] ;
		mOnClickListener = new DialogInterface.OnClickListener[3] ;
		
		mButtonTitle = new String[3] ;
		mButtonColor = new int[3] ;
		mButtonEnabledBackgroundDrawable = new Drawable[3] ;

		for ( int i = 0; i < 3; i++ ) {
			mButtonTitle[i] = null ;
		}
		mButtonDefaultColor[BUTTON_POSITIVE] = res.getColor(R.color.dialog_button_strip_positive) ;
		mButtonDefaultColor[BUTTON_NEUTRAL] = res.getColor(R.color.dialog_button_strip_neutral) ;
		mButtonDefaultColor[BUTTON_NEGATIVE] = res.getColor(R.color.dialog_button_strip_negative) ;
		
		mEverRefreshed = false ;	
		
		for ( int i = 0; i < 3; i++ ) {
			LineShadingDrawable lsd = new LineShadingDrawable() ;
			lsd.setColor(0xff404040) ;
			lsd.setLineWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					(float)1, getResources().getDisplayMetrics())) ;
			lsd.setLineSeparation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
					(float)5, getResources().getDisplayMetrics())) ;
			mButtonEnabledBackgroundDrawable[i] = lsd ;
		}
		
		// We want onDraw to be called for this view.
		setWillNotDraw(false) ;
	}
	
	
	private void constructor_readAttributeSet(Context context, AttributeSet attrs) {
		
		// Maybe allow some initial configuration of buttons?
		
	}
	
	
	/**
	 * Inflation is finished.
	 */
	protected void onFinishInflate () {
		super.onFinishInflate() ;
		
		// by default, nothing is visible or enabled.
		for ( int i = 0; i < 3; i++ ) {
			setButtonVisible(i, false) ;
			setButtonEnabled(i, false) ;
		}
	}
	
	
	boolean mEverRefreshed = false ;
	
	private static final int ENABLED_ALPHA = 255 ;
	private static final int DISABLED_ALPHA = 128 ;
	
	// Following Android design guidelines, "positive" actions are on the right,
	// "dismissive" actions on the left.
	public static final int BUTTON_POSITIVE = 2 ;
	public static final int BUTTON_NEUTRAL = 1 ;
	public static final int BUTTON_NEGATIVE = 0 ;
	
	int [] DIALOG_INTERFACE_BUTTON = new int[] {
			DialogInterface.BUTTON_NEGATIVE,		// negative on the left
			DialogInterface.BUTTON_NEUTRAL,
			DialogInterface.BUTTON_POSITIVE,
	} ;
	
	int [] mButtonDefaultColor ;
	
	boolean mAutoRefresh ;
	
	DialogInterface mDialogInterface ;
	DialogInterface.OnClickListener [] mOnClickListener ;
	
	CharSequence [] mButtonTitle ;
	int [] mButtonColor ;
	
	Drawable [] mButtonEnabledBackgroundDrawable ;
	
	synchronized public boolean hasVisibleButtons() {
		for ( int i = 0; i < 3; i++ )
			if ( getButtonIsVisible(i) )
				return true ;
		return false ;
	}
	
	synchronized public boolean hasEnabledButtons() {
		for ( int i = 0; i < 3; i++ )
			if ( getButtonIsEnabled(i) )
				return true ;
		return false ;
	}
	
	synchronized public void setDialogInterface( DialogInterface inter ) {
		mDialogInterface = inter ;
	}
	
	synchronized public void setPositiveButton( CharSequence title, DialogInterface.OnClickListener listener ) {
		setButton( BUTTON_POSITIVE, title, listener ) ;
	}
	
	synchronized public void setNeutralButton( CharSequence title, DialogInterface.OnClickListener listener ) {
		setButton( BUTTON_NEUTRAL, title, listener ) ;
	}
	
	synchronized public void setNegativeButton( CharSequence title, DialogInterface.OnClickListener listener ) {
		setButton( BUTTON_NEGATIVE, title, listener ) ;
	}
	
	synchronized public void setPositiveButton( CharSequence title, int color, DialogInterface.OnClickListener listener ) {
		setButton( BUTTON_POSITIVE, title, color, listener ) ;
	}
	
	synchronized public void setNeutralButton( CharSequence title, int color, DialogInterface.OnClickListener listener ) {
		setButton( BUTTON_NEUTRAL, title, color, listener ) ;
	}
	
	synchronized public void setNegativeButton( CharSequence title, int color, DialogInterface.OnClickListener listener ) {
		setButton( BUTTON_NEGATIVE, title, color, listener ) ;
	}
	
	synchronized public void setButton(
			int button, CharSequence title,
			DialogInterface.OnClickListener listener ) {
		
		button = convertDialogInterfaceButtonNumber( button ) ;
		
		mButtonTitle[button] = title ;
		mButtonColor[button] = mButtonDefaultColor[button] ;
		mOnClickListener[button] = listener ;
		setButtonEnabled( button, true ) ;
		setButtonVisible( button, true ) ;
		
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void setButton(
			int button, CharSequence title, int color,
			DialogInterface.OnClickListener listener ) {
		
		button = convertDialogInterfaceButtonNumber( button ) ;
		
		mButtonTitle[button] = title ;
		mButtonColor[button] = color ;
		mOnClickListener[button] = listener ;
		setButtonEnabled( button, true ) ;
		setButtonVisible( button, true ) ;
		
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void enableButton( int which, boolean enabled ) {
		setButtonEnabled( convertDialogInterfaceButtonNumber( which ), enabled ) ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void showButton( int which, boolean visible ) {
		setButtonVisible( convertDialogInterfaceButtonNumber( which ), visible ) ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	
	
	synchronized public void configurePositiveButton( boolean enabled, boolean visible ) {
		configureButton( BUTTON_POSITIVE, enabled, visible ) ;
	}
	
	synchronized public void configureNeutralButton( boolean enabled, boolean visible ) {
		configureButton( BUTTON_NEUTRAL, enabled, visible ) ;
	}
	
	synchronized public void configureNegativeButton( boolean enabled, boolean visible ) {
		configureButton( BUTTON_NEGATIVE, enabled, visible ) ;
	}
	
	synchronized public void configureButton( int button, boolean enabled, boolean visible ) {
		button = convertDialogInterfaceButtonNumber( button ) ;
		setButtonEnabled(button, enabled) ;
		setButtonVisible(button, visible) ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void configureButton( int button, int color, boolean enabled, boolean visible ) {
		button = convertDialogInterfaceButtonNumber( button ) ;
		mButtonColor[button] = color ;
		setButtonEnabled(button, enabled) ;
		setButtonVisible(button, visible) ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void configurePositiveButton( int color, boolean enabled, boolean visible ) {
		configureButton( BUTTON_POSITIVE, color, enabled, visible ) ;
	}
	
	synchronized public void configureNeutralButton( int color, boolean enabled, boolean visible ) {
		configureButton( BUTTON_NEUTRAL, color, enabled, visible ) ;
	}
	
	synchronized public void configureNegativeButton( int color, boolean enabled, boolean visible ) {
		configureButton( BUTTON_NEGATIVE, color, enabled, visible ) ;
	}
	
	synchronized public void setButtonColor( int button, int color ) {
		button = convertDialogInterfaceButtonNumber( button ) ;
		mButtonColor[button] = color ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void setButtonTitle( int button, CharSequence title ) {
		button = convertDialogInterfaceButtonNumber( button ) ;
		mButtonTitle[button] = title ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void setDialogButtonEnabled( int button, boolean enabled ) {
		button = convertDialogInterfaceButtonNumber( button ) ;
		setButtonEnabled( button, enabled ) ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void setDialogButtonVisible( int button, boolean visible ) {
		button = convertDialogInterfaceButtonNumber( button ) ;
		setButtonVisible( button, visible ) ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	synchronized public void setAutoRefresh( boolean refresh ) {
		mAutoRefresh = refresh ;
		if ( mAutoRefresh )
			refresh() ;
	}
	
	
	private int convertDialogInterfaceButtonNumber( int button ) {
		for ( int i = 0; i < DIALOG_INTERFACE_BUTTON.length; i++ )
			if ( DIALOG_INTERFACE_BUTTON[i] == button )
				return i ;
		return button ;
	}
	
	synchronized public void refresh() {
		boolean changed = false ;
		
		// Make sure we have enough buttons!
		while ( this.getButtonCount() < 3 ) {
			Log.d(TAG, "adding button") ;
			changed = true ;
			addButton() ;
		}
		
		if ( !mEverRefreshed ) {
			changed = true ;
			// set LineShadingDrawables.
			while ( getButtonCount() > 3 )
				addButton() ;
		}
		
		for ( int i = 0; i < 3; i++ )
			changed = setButtonContent( i ) || changed ;
		
		if ( changed ) {
			super.refresh() ;
		}
		
		mEverRefreshed = true ;
	}
	
	
	private boolean setButtonContent( int buttonNum ) {
		boolean changed = false ;
		
		QuantroButtonAccess qbs = getButtonAccess(buttonNum) ;
		
		changed = qbs.setTitle( mButtonTitle[buttonNum] ) || changed ;
		changed = qbs.setColor( mButtonColor[buttonNum] ) || changed ;
		
		// enabled / visible?
		changed = qbs.setFullBackground( getButtonIsEnabled(buttonNum)
				? mButtonEnabledBackgroundDrawable[buttonNum]
				: null )  || changed ;
		changed = changed || !this.getButtonViewEnabledIsSet(buttonNum) ;
		changed = changed || !this.getButtonViewVisibilityIsSet(buttonNum) ;
		
		// alpha?
		changed = qbs.setContentAlpha( getButtonIsEnabled(buttonNum) ? ENABLED_ALPHA : DISABLED_ALPHA ) || changed ;
		
		return changed ;
	}

	@Override
	public void qbsc_onClick(QuantroButtonStrip strip, int buttonNum, boolean asOverflow) {
		if ( mOnClickListener[buttonNum] != null && getButtonIsEnabled(buttonNum) )
			mOnClickListener[buttonNum].onClick( mDialogInterface, DIALOG_INTERFACE_BUTTON[buttonNum] ) ;
	}

	@Override
	public boolean qbsc_onLongClick(QuantroButtonStrip strip, int buttonNum) {
		return false ;
	}

	@Override
	public boolean qbsc_supportsLongClick(QuantroButtonStrip strip,
			int buttonNum) {
		return false ;
	}
	
	@Override
	public void qbsc_onOverflowClicked(
			QuantroButtonStrip strip ) {
		// nothing
	}
	
}
