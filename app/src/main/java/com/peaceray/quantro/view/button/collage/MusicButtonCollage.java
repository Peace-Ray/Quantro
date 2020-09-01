package com.peaceray.quantro.view.button.collage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.SupportsLongClickOracle;
import com.peaceray.quantro.view.button.collage.BackgroundButtonCollage.Mode;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.options.OptionAvailability;

public class MusicButtonCollage extends QuantroButtonCollage implements View.OnClickListener, View.OnLongClickListener, SupportsLongClickOracle {

	
	public interface Delegate {
		
		/**
		 * The user wants to play this music track.
		 * 
		 * @param mbc
		 * @param music
		 * @param availability
		 * @param setAsDefault Should this music track be the default from now on?
		 * @return Should the appropriate 'button press' sound effect be played?
		 */
		public boolean mbcd_playMusic( MusicButtonCollage mbc, Music music, OptionAvailability availability, boolean setAsDefault ) ;
		
	}
	
	
	public MusicButtonCollage(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public MusicButtonCollage(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public MusicButtonCollage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues( Context context ) {
		// For now, we have only 2 players in a lobby, so we have other_0.
		
		mMusic = null ;
		
		mDrawableLocked = null ;
		mDrawableActive =  null ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
	}
	
	
	private void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.MusicButtonCollage);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.MusicButtonCollage_mbc_alert_drawable_locked:
				mDrawableLocked = a.getDrawable(attr) ;
				break ;
			case R.styleable.MusicButtonCollage_mbc_alert_drawable_active:
				mDrawableActive = a.getDrawable(attr) ;
				break ;
			}
		}
	}
	

	private void constructor_allocateAndInitialize( Context context ) {
		// nothing to do.
	}
	
	
	private Music mMusic ;
	private OptionAvailability mAvailability ;
	private boolean mActive ;
	
	private Drawable mDrawableLocked ;
	private Drawable mDrawableActive ;
	
	private WeakReference<Delegate> mwrDelegate ;
	private ColorScheme mColorScheme ;
	private QuantroSoundPool mSoundPool ;
	
	private QuantroContentWrappingButton mButton ;
	private QuantroButtonDirectAccess mButtonContent ;
	
	
	public void setDelegate( Delegate d ) {
		mwrDelegate = new WeakReference<Delegate>(d) ;
	}
	
	public void setColorScheme( ColorScheme cs ) {
		mColorScheme = cs ;
	}
	
	public void setSoundPool( QuantroSoundPool pool ) {
		mSoundPool = pool ;
	}
	 
	public void setMusic( Music m, OptionAvailability availability, boolean active ) {
		boolean changed = availability != mAvailability ;
		changed = changed || !Music.equals(m, mMusic) ;
		changed = changed || active != mActive ;
		
		mMusic = m ;
		mAvailability = availability ;
		mActive = active ;
		if ( changed )
			refresh() ;
	}

	public Music getMusic() {
		return mMusic ;
	}
	
	
	private boolean refresh_ever = false ;
	@Override
	public void refresh() {
		boolean changed = false ;
		
		if ( !refresh_ever ) {
			collectAllContent() ;
			refresh_ever = true ;
			changed = true ;
		}
		
		changed = setButtonContent( mButton, mButtonContent ) || changed ;
		mButton.setVisibility(View.VISIBLE) ;
		
		if ( changed )
			super.refresh() ;
	}
	
	/**
	 * Collects necessary references, sets listeners, creates 
	 * QuantroButtonContent objects, etc.
	 */
	private void collectAllContent() {

		// collect buttons.
		ArrayList<Integer> ids = new ArrayList<Integer>() ;
		
		ids.add( R.id.button_collage_music_button_main ) ;
		ArrayList<QuantroContentWrappingButton> buttons = collectButtons(ids) ;
		
		// set content and tag
		mButton = buttons.get(0) ;
		mButtonContent = (QuantroButtonDirectAccess)QuantroButtonDirectAccess.wrap( mButton ) ;
		mButton.setTag(mButtonContent) ;
		
		// assign listeners
		mButton.setOnClickListener(this) ;
		mButton.setOnLongClickListener(this) ;
		mButton.setSupportsLongClickOracle(this) ;
	}
	
	
	
	private boolean setButtonContent(
			QuantroContentWrappingButton button, QuantroButtonDirectAccess content ) {
		// Set to false here, then to 'true' if any content changed.
		boolean changed = false ;
		
		boolean enabled = mAvailability.isEnabled() ;
		boolean locked = mAvailability.isLocked() ;
		
		Drawable drawable = locked ? mDrawableLocked : mDrawableActive ;
		boolean showDrawable = locked || mActive ;
		
		String textTitle ;
		int baseColor ;
		
		QuantroContentWrappingButton.ContentState contentState = QuantroContentWrappingButton.ContentState.OPTIONAL ;
		if ( mActive )
			contentState = QuantroContentWrappingButton.ContentState.EXCLUSIVE ;
		
		if ( mMusic == null ) {
			textTitle = null ;
			baseColor = 0xffffffff ;
		} else {
			textTitle = mMusic.getName() ;
			baseColor = GameModeColors.primary(mColorScheme, mMusic.getTypicalGameMode()) ;
		}
		
		
		// Now set these values in the appropriate content fields.
		changed = content.setTitle(textTitle) 					|| changed ;
		changed = content.setColor(baseColor)					|| changed ;
		changed = content.setImage(showDrawable, drawable)		|| changed ;
		
		changed = button.isEnabled() != enabled 				|| changed ;
		button.setEnabled(enabled) ;
		button.setClickable(enabled) ;
		
		changed = button.getContentState() != contentState		|| changed ;
		button.setContentState(contentState) ;
		
		return changed ;
	}

	@Override
	public void onClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		if ( v != mButton )
			return ;
		
		boolean sound = delegate.mbcd_playMusic(this, mMusic, mAvailability, false) ;
		
		if ( sound && mSoundPool != null )
			mSoundPool.menuButtonClick() ;
	}
	
	@Override
	public boolean onLongClick(View v) {
		return false ;
	}
	
	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		return false ;
	}
	
}
