package com.peaceray.quantro.view.button.popup;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.peaceray.quantro.view.button.QuantroButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;

/**
 * An implementation using a PopupMenu object, which is bound to the Button
 * and displayed when appropriate.
 * 
 * @author Jake
 *
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class QuantroButtonPopupWrapperInstancePopupMenu extends
		QuantroButtonPopupWrapper implements OnMenuItemClickListener {
	
	CharSequence [] mTitles ;
	Object [] mTags ;
	boolean [] mEnabled ;
	
	PopupMenu mPopupMenu ;
	boolean mPopupStale = false ;
	
	QuantroButtonPopupWrapperInstancePopupMenu( CharSequence [] titles, Object [] tags ) {
		set( titles, tags ) ;
	}
	
	QuantroButtonPopupWrapperInstancePopupMenu set( CharSequence [] titles, Object [] tags ) {
		boolean different = false ;
		different = different || ((titles == null) != (mTitles == null)) ;
		different = different || titles.length != mTitles.length ;
		different = different || ((tags == null) != (mTags == null)) ;
		if ( tags != null ) {
			different = different || tags.length != mTags.length ;
			for ( int i = 0; i < tags.length && !different; i++ ) {
				different = different || ((tags[i] == null) != (mTags[i] == null)) ;
				different = different || !titles[i].equals(mTitles[i]) ;
				different = different || ( tags[i] != null && !tags[i].equals(mTags[i])) ;
			}
		}
		
		if ( different ) {
			mTitles = titles.clone() ;
			mTags = tags == null ? null : tags.clone() ;
			mEnabled = new boolean[mTitles.length] ;
			for ( int i = 0; i < mEnabled.length; i++ )
				mEnabled[i] = true ;
			
			mPopupStale = true ;
		}
		
		return this ;
	}
	
	@Override
	QuantroButtonPopupWrapper setButton( QuantroContentWrappingButton button ) {
		super.setButton(button) ;
		if ( mTitles != null ) {
			mPopupStale = true ;
		}
		
		return this ;
	}
	
	private void constructMenu() {
		QuantroContentWrappingButton button = getButton() ;
		
		// Make the popup menu...
		mPopupMenu = new PopupMenu( button.getContext(), button ) ;
		// Set ourself as listener...
		mPopupMenu.setOnMenuItemClickListener(this) ;
		// Populate it...
		Menu menu = mPopupMenu.getMenu() ;
		for ( int i = 0; i < mTitles.length; i++ ) {
			// add with index as "id"
			menu.add(Menu.NONE, i, Menu.NONE, mTitles[i]) ;
		}
		for ( int i = 0; i < mEnabled.length; i++ ) {
			MenuItem item = menu.getItem(i) ;
			item.setEnabled(mEnabled[i]) ;
		}
	}

	@Override
	public void onClick(View v) {
		if ( mPopupStale ) {
			constructMenu() ;
			mPopupStale = false ;
		}
		// show the popup.
		mPopupMenu.show() ;
		
		// Tell the listener.
		QuantroButtonPopup.Listener listener = getListener() ;
		if ( listener != null )
			listener.qbpw_onMenuOpened((QuantroButton)v) ;
	}

	@Override
	public boolean onLongClick(View v) {
		return false;
	}

	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		return false;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		QuantroButtonPopup.Listener listener = getListener() ;
		if ( listener != null ) {
			int itemId = item.getItemId() ;
			listener.qbpw_onButtonUsed(getButton(), true, itemId, mTags == null ? null : mTags[itemId]) ;
		}
		
		// handled it.
		return true ;
	}
	
	
	public QuantroButtonPopupWrapper setEnabled( int index, boolean enabled ) {
		mEnabled[index] = enabled ;
		if ( mPopupMenu != null ) {
			Menu menu = mPopupMenu.getMenu() ;
			MenuItem menuItem = menu.getItem(index) ;
			menuItem.setEnabled(enabled) ;
		}
		
		return this ;
	}
	
	public QuantroButtonPopupWrapper setEnabled( Object tag, boolean enabled ) {
		if ( tag != null && mTags != null ) {
			for ( int i = 0; i < mTags.length; i++ ) {
				if ( tag.equals(mTags[i]) ) {
					return setEnabled(i, enabled) ;
				}
			}
		}
		return this ;
	}

}
