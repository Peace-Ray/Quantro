package com.peaceray.quantro.view.button.collage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.peaceray.android.graphics.BitmapSoftCache;
import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.threadedloader.ThreadedBackgroundThumbnailLoader;
import com.peaceray.quantro.utils.threadedloader.ThreadedBitmapLoader;
import com.peaceray.quantro.utils.threadedloader.ThreadedLoader;
import com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Client;
import com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Params;
import com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Result;
import com.peaceray.quantro.utils.threadedloader.ThreadedSkinThumbnailLoader;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.SupportsLongClickOracle;
import com.peaceray.quantro.view.button.collage.BackgroundButtonCollage.Mode;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.drawable.DropShadowedDrawable;
import com.peaceray.quantro.view.drawable.RectsDrawable;
import com.peaceray.quantro.view.options.OptionAvailability;



/**
 * A SkinButtonCollage, like a MusicButtonCollage, represents
 * a single skin.  It allows several functions, such as setting
 * the skin as current, including it in a shuffle, etc.
 * 
 * @author Jake
 *
 */
public class SkinButtonCollage extends QuantroButtonCollage
		implements View.OnClickListener, View.OnLongClickListener, SupportsLongClickOracle, Client {

	
	private static final String TAG = "SkinButtonCollage" ;
	
	public interface Delegate {
		
		/**
		 * The user wants to use this current Skin as its.. well,
		 * skin.
		 * 
		 * @param mbc
		 * @param skin
		 * @param availability
		 * @return Should the appropriate 'button press' sound effect be played?
		 */
		public boolean sbcd_setSkin( SkinButtonCollage mbc, Skin skin, OptionAvailability availability ) ;
		
	}
	
	
	public SkinButtonCollage(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public SkinButtonCollage(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public SkinButtonCollage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues( Context context ) {
		mSkin = null ;
		
		mDrawableLocked = null ;
		mDrawableActive =  null ;
		mDrawableSkinThumbnail = null ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
		
		mThumbnailLoadWidth = context.getResources().getDimensionPixelSize(R.dimen.button_content_size) ;
	
		mRefreshRunnable = new Runnable() {
			public void run() {
				refresh();
			}
		} ;
	}
	
	
	private void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.SkinButtonCollage);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.SkinButtonCollage_sbc_alert_drawable_locked:
				mDrawableLocked = a.getDrawable(attr) ;
				break ;
			case R.styleable.SkinButtonCollage_sbc_alert_drawable_active:
				mDrawableActive = a.getDrawable(attr) ;
				break ;
			case R.styleable.SkinButtonCollage_sbc_color:
				mColor = a.getColor(attr, 0xffff00ff) ;
				break ;
			case R.styleable.SkinButtonCollage_sbc_thumbnail_load_width:
				mThumbnailLoadWidth = a.getDimensionPixelSize(attr, mThumbnailLoadWidth) ;
			}
		}
	}
	

	private void constructor_allocateAndInitialize( Context context ) {
		// nothing to do.
	}
	
	
	// Runnable
	protected Runnable mRefreshRunnable ;
	
	private WeakReference<Delegate> mwrDelegate ;
	private ColorScheme mColorScheme ;
	private QuantroSoundPool mSoundPool ;
	
	private QuantroContentWrappingButton mButton ;
	private QuantroButtonDirectAccess mButtonContent ;
	
	private Skin mSkin ;
	private OptionAvailability mAvailability ;
	private boolean mActive ;
	
	private Drawable mDrawableLocked ;
	private Drawable mDrawableActive ;
	
	private int mColor ;

	private Drawable mDrawableSkinThumbnail ;
	
	// Loading Thumbnails!
	private int mThumbnailLoadWidth ;
	private BitmapSoftCache mThumbnailSoftCache ;
	private ThreadedSkinThumbnailLoader mThreadedSkinLoader ;

	
	public void setDelegate( Delegate d ) {
		mwrDelegate = new WeakReference<Delegate>(d) ;
	}
	
	public void setColorScheme( ColorScheme cs ) {
		mColorScheme = cs ;
	}
	
	public void setSoundPool( QuantroSoundPool pool ) {
		mSoundPool = pool ;
	}
	
	protected void setThumbnail( Bitmap b ) {
		mDrawableSkinThumbnail = new BitmapDrawable( getContext().getResources(), b ) ;
	}
	
	synchronized public void setThumbnailSource( BitmapSoftCache thumbnailSoftCache, ThreadedSkinThumbnailLoader threadedSkinLoader ) {
		mThumbnailSoftCache = thumbnailSoftCache ;
		mThreadedSkinLoader = threadedSkinLoader ;
	}
	 
	public void setContentSkin( Skin s, OptionAvailability availability, boolean active ) {
		boolean changed = availability != mAvailability ;
		changed = changed || !Skin.equals(s, mSkin) ;
		changed = changed || active != mActive ;

		if ( !Skin.equals(s, mSkin) ) {
			boolean hasThumbnail = false ;
			if ( mThumbnailSoftCache != null ) {
				Bitmap b = mThumbnailSoftCache.get( s ) ;
				if ( b != null ) {
					setThumbnail( b ) ;
					hasThumbnail = true ;
				}
			}
			
			if ( !hasThumbnail ) {
				// Reset the thumbnail while we load...
				mDrawableSkinThumbnail = null ;
				
				// Set ourselves as a client for the load...
				if ( mThreadedSkinLoader != null ) {
					mThreadedSkinLoader.addClient(this) ;
				}
			}
		}
		
		mSkin = s ;
		mAvailability = availability ;
		mActive = active ;
		if ( changed )
			refresh() ;
	}

	public Skin getContentSkin() {
		return mSkin ;
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
		
		ids.add( R.id.button_collage_skin_button_main ) ;
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
		
		Drawable alert = locked ? mDrawableLocked : mDrawableActive ;
		boolean showAlert = locked || mActive ;
		
		QuantroContentWrappingButton.ContentState contentState = QuantroContentWrappingButton.ContentState.OPTIONAL ;
		if ( mActive )
			contentState = QuantroContentWrappingButton.ContentState.EXCLUSIVE ;
		
		String textTitle ;
		int baseColor ;
		
		if ( mSkin == null ) {
			textTitle = null ;
			baseColor = 0xffffffff ;
		} else {
			textTitle = Skin.getName( mSkin.getColor() ) ;
			baseColor = mColor ;
		}
		
		// Now set these values in the appropriate content fields.
		Log.d(TAG, "set title " + textTitle) ;
		changed = content.setTitle(textTitle) 					|| changed ;
		changed = content.setColor(baseColor)					|| changed ;
		changed = content.setAlert(showAlert, alert)			|| changed ;
		changed = content.setImage(mDrawableSkinThumbnail != null, mDrawableSkinThumbnail) 	|| changed ;
		// TODO: Change this when we start loading skin thumbnails
		
		changed = button.getContentState() != contentState		|| changed ;
		button.setContentState(contentState) ;
		
		changed = button.isEnabled() != enabled 				|| changed ;
		button.setEnabled(enabled) ;
		button.setClickable(enabled) ;
		
		return changed ;
	}

	@Override
	public void onClick(View v) {
		Log.d(TAG, "onClick") ;
		
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return ;
		
		if ( v != mButton )
			return ;
		
		boolean sound = delegate.sbcd_setSkin(this, mSkin, mAvailability) ;
		
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

////////////////////////////////////////////////////////////////////////////
	//
	// TLC - Thumbnail Loader Client methods

	@Override
	synchronized public boolean tlc_setParams(ThreadedLoader tl, Params p) {
		if ( mSkin != null ) {
			Bitmap b = this.mThumbnailSoftCache.get(mSkin) ;
			if ( b == null ) {
				ThreadedSkinThumbnailLoader.Params params = (ThreadedSkinThumbnailLoader.Params) p ;
				
				params.setSkin(mSkin) ;
				params.setScaleType( ThreadedBitmapLoader.Params.ScaleType.FIT ) ;
				params.setSize(mThumbnailLoadWidth, Integer.MAX_VALUE) ;
				
				return true ;
			} else {
				setThumbnail(b) ;
				((Activity)getContext()).runOnUiThread(mRefreshRunnable) ;
			}
		}
		
		return false ;
	}

	@Override
	synchronized public void tlc_finished(ThreadedLoader tl, Params p, Result r) {
		if ( !r.getSuccess() )
			return ;
		
		// check that params match.
		ThreadedSkinThumbnailLoader.Params params = (ThreadedSkinThumbnailLoader.Params) p ;
		ThreadedSkinThumbnailLoader.Result result = (ThreadedSkinThumbnailLoader.Result) r ;
		
		if ( params.getSkin().equals(mSkin) ) {
			Bitmap b = result.getBitmap() ;
			mThumbnailSoftCache.put(mSkin, b) ;
			setThumbnail(b) ;
			((Activity)getContext()).runOnUiThread(mRefreshRunnable) ;
		}
	}
	
}
