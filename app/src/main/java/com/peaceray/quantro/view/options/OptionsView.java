package com.peaceray.quantro.view.options;

import android.app.Activity;
import android.view.View;

import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.colors.ColorScheme;

public abstract class OptionsView {

	
	protected QuantroSoundPool mSoundPool ;
	protected boolean mSoundControls ;
	protected ColorScheme mColorScheme ;
	
	/**
	 * Sets a sound pool for button presses.
	 * @param soundPool
	 */
	public void setSoundPool( QuantroSoundPool soundPool ) {
		mSoundPool = soundPool ;
	}
	
	/**
	 * Sets whether we play sounds for button presses.
	 * @param soundControls
	 */
	public void setSoundControls( boolean soundControls ) {
		mSoundControls = soundControls ;
	}
	
	
	/**
	 * Sets the color scheme to use in coloring buttons and such
	 * @param scheme
	 */
	public void setColorScheme( ColorScheme scheme ) {
		mColorScheme = scheme ;
	}
	
	
	/**
	 * Called once, after the most basic settings (e.g. SoundPool,
	 * ColorScheme) have been provided.  Use this opportunity to 
	 * get references to your content views and perform basic
	 * initial setup.
	 * 
	 * @param activity
	 * @param root
	 */
	public abstract void init( Activity activity, View root ) ;
	
	/**
	 * Called after every significant setting change.  Use this 
	 * opportunity to refresh any content views which depend on
	 * these settings.
	 */
	public abstract void refresh() ;
	
	
	private boolean mIsRelaxed = false ;
	
	/**
	 * Called to indicate that the view will not be displayed to
	 * the screen for a while.  Use this opportunity to relax any
	 * high-memory caches, such as bitmap thumbnails and the content
	 * of their associated views.
	 * 
	 * This method may be called several times in a row.
	 */
	public final void relaxCache() {
		relaxCache( mIsRelaxed ) ;
		mIsRelaxed = true ;
	}
	
	protected abstract void relaxCache( boolean isRelaxed ) ;
	
	/**
	 * Called to indicate that the view will soon be displayed, so
	 * any cache content forgotten by 'relaxCache' should be restored.
	 * 
	 * This method may be called several times in a row.
	 */
	public final void refreshCache() {
		refreshCache( mIsRelaxed ) ;
	}
	
	protected abstract void refreshCache( boolean isRelaxed ) ;
	
}
