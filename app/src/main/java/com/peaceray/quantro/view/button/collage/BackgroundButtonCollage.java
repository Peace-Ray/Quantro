package com.peaceray.quantro.view.button.collage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.peaceray.android.graphics.BitmapSoftCache;
import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.threadedloader.ThreadedBackgroundThumbnailLoader;
import com.peaceray.quantro.utils.threadedloader.ThreadedBitmapLoader;
import com.peaceray.quantro.utils.threadedloader.ThreadedLoader;
import com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Params;
import com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Result;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton;
import com.peaceray.quantro.view.button.QuantroContentWrappingButton.SupportsLongClickOracle;
import com.peaceray.quantro.view.button.content.QuantroButtonDirectAccess;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.drawable.DropShadowedDrawable;
import com.peaceray.quantro.view.options.OptionAvailability;



/**
 * A BackgroundButtonCollage, like a MusicButtonCollage, represents
 * a single background.  It allows several functions, such as setting
 * the background as current, including it in a shuffle, etc.
 * 
 * It has two different "modes of operation."  In STANDARD mode,
 * it responds to taps by setting its content as the current background
 * (via the delegate).  In SHUFFLE mode, it responds to taps and holds:
 * for a tap, it toggles its status in the current shuffle (via delegate);
 * for a hold, it adds itself to the shuffle and sets itself as the
 * current background.
 * 
 * In this iteration, coloration and glow is based on mode.  Specifically:
 * 
 * 1. in STANDARD mode, buttons use STANDARD color.  Glow is activated
 * 		if the background is Active (i.e. currently in-use).
 * 2. in SHUFFLE mode, buttons use SHUFFLE color.  Glow is activated if
 * 		the background is in the shuffle.  However, if the background is
 * 		Active (currently in-use), then we use the STANDARD color and
 * 		glow instead.
 * 
 * @author Jake
 *
 */
public class BackgroundButtonCollage extends QuantroButtonCollage
		implements View.OnClickListener, View.OnLongClickListener, SupportsLongClickOracle, ThreadedLoader.Client {
	
	
	private static final String TAG = "BackgroundButtonCollage" ;

	public interface Delegate {
		
		/**
		 * The user wants to use this current Background as its.. well,
		 * background.
		 * 
		 * @param mbc
		 * @param background
		 * @param availability
		 * @return Should the appropriate 'button press' sound effect be played?
		 */
		public boolean bbcd_setBackground( BackgroundButtonCollage mbc, Background background, OptionAvailability availability ) ;
		
		/**
		 * The user wants to use this current Background as its background.
		 * It also should be placed in (or removed from) the current shuffle.
		 * 
		 * @param mbc
		 * @param background
		 * @param availability
		 * @param inShuffle
		 * @return
		 */
		public boolean bbcd_setBackgroundAndIncludeInShuffle( BackgroundButtonCollage mbc, Background background, OptionAvailability availability ) ;
		
		/**
		 * The user wants to set this background as an element of the current shuffle.
		 * 
		 * @param mbc
		 * @param background
		 * @param availability
		 * @param inShuffle Whether the background should be included in the current shuffle.
		 * @return Should the appropriate 'button press' sound effect be played?
		 */
		public boolean bbcd_setBackgroundInShuffle( BackgroundButtonCollage mbc, Background background, OptionAvailability availability, boolean inShuffle ) ;
		
		
		
	}
	
	
	public BackgroundButtonCollage(Context context) {
		super(context);
		
		// Set basic defaults
		
		constructor_setDefaultValues(context) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public BackgroundButtonCollage(Context context, AttributeSet attrs) {
		super(context, attrs);
	
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	public BackgroundButtonCollage(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		constructor_setDefaultValues(context) ;
		constructor_readAttributeSet(context, attrs) ;
		constructor_allocateAndInitialize(context) ;
	}
	
	
	private void constructor_setDefaultValues( Context context ) {
		mMode = Mode.STANDARD ;
		
		mBackground = null ;
		
		mDrawableLocked = null ;
		mDrawableActive =  null ;
		mDrawableNone = null ;
		mDrawableBackgroundThumbnail = null ;
		
		mwrDelegate = new WeakReference<Delegate>(null) ;
		
		mColor = new int[Mode.values().length] ;
		for ( int i = 0; i < mColor.length; i++ )
			mColor[i] = 0xffff00ff ;
		
		mThumbnailLoadWidth = context.getResources().getDimensionPixelSize(R.dimen.button_content_size) ;
		
		mRefreshRunnable = new Runnable() {
			public void run() {
				refresh();
			}
		} ;
	}
	
	
	private void constructor_readAttributeSet( Context context, AttributeSet attrs ) {
		TypedArray a = context.obtainStyledAttributes(attrs,
			    R.styleable.BackgroundButtonCollage);
		
		final int N = a.getIndexCount() ;
		for ( int i = 0; i < N; i++ ) {
			int attr = a.getIndex(i);
			switch( attr ) {
			case R.styleable.BackgroundButtonCollage_bbc_alert_drawable_locked:
				mDrawableLocked = a.getDrawable(attr) ;
				break ;
			case R.styleable.BackgroundButtonCollage_bbc_alert_drawable_active:
				mDrawableActive = a.getDrawable(attr) ;
				break ;
			case R.styleable.BackgroundButtonCollage_bbc_image_drawable_background_none:
				mDrawableNone = a.getDrawable(attr) ;
				break ;
			case R.styleable.BackgroundButtonCollage_bbc_color_standard:
				mColor[Mode.STANDARD.ordinal()] = a.getColor(attr, 0xffff00ff) ;
				break ;
			case R.styleable.BackgroundButtonCollage_bbc_color_shuffle:
				mColor[Mode.SHUFFLE.ordinal()] = a.getColor(attr, 0xffff00ff) ;
				break ;
			case R.styleable.BackgroundButtonCollage_bbc_thumbnail_load_width:
				mThumbnailLoadWidth = a.getDimensionPixelSize(attr, mThumbnailLoadWidth) ;
				break ;
			}
		}
	}
	

	private void constructor_allocateAndInitialize( Context context ) {
		// nothing to do.
	}
	
	
	
	public enum Mode {
		
		/**
		 * Use Standard color, with a glow if this is the currently active background.
		 */
		STANDARD,
		
		/**
		 * Use Shuffle color, with a glow if this background is included in the shuffle.
		 * If this is the currently active background, 
		 */
		SHUFFLE
	}
	
	// Runnable
	protected Runnable mRefreshRunnable ;
	
	private WeakReference<Delegate> mwrDelegate ;
	private ColorScheme mColorScheme ;
	private QuantroSoundPool mSoundPool ;
	
	private QuantroContentWrappingButton mButton ;
	private QuantroButtonDirectAccess mButtonContent ;
	
	private Mode mMode ;
	
	private Background mBackground ;
	private OptionAvailability mAvailability ;
	private boolean mActive ;
	private boolean mInShuffle ;
	
	private Drawable mDrawableLocked ;
	private Drawable mDrawableActive ;
	private Drawable mDrawableNone ;
	
	private int [] mColor ;		// by mode

	private Drawable mDrawableBackgroundThumbnail ;
	
	// Loading Thumbnails!
	private int mThumbnailLoadWidth ;
	private BitmapSoftCache mThumbnailSoftCache ;
	private ThreadedBackgroundThumbnailLoader mThreadedBackgroundLoader ;

	
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
		mDrawableBackgroundThumbnail = new DropShadowedDrawable( getContext().getResources(), b ) ;
	}
	
	synchronized public void setThumbnailSource( BitmapSoftCache thumbnailSoftCache, ThreadedBackgroundThumbnailLoader threadedBackgroundLoader ) {
		mThumbnailSoftCache = thumbnailSoftCache ;
		mThreadedBackgroundLoader = threadedBackgroundLoader ;
	}
	 
	synchronized public void setContentBackground( Background bg, OptionAvailability availability, boolean active, boolean inShuffle ) {
		boolean changed = availability != mAvailability ;
		changed = changed || !Background.equals(bg, mBackground) ;
		changed = changed || active != mActive ;
		changed = changed || inShuffle != mInShuffle ;
		

		if ( !Background.equals(bg, mBackground) ) {
			mDrawableBackgroundThumbnail = null ;
			
			if ( bg != null && bg.hasImage() ) {
				// Look for the appropriate bitmap...
				boolean hasThumbnail = false ;
				if ( mThumbnailSoftCache != null ) {
					Bitmap b = mThumbnailSoftCache.get( bg ) ;
					if ( b != null ) {
						setThumbnail( b ) ;
						hasThumbnail = true ;
					}
				}
				
				if ( !hasThumbnail ) {
					// Put in a "placeholder" thumbnail while we load...
					ColorDrawable cd = new ColorDrawable( bg.getColor() ) ;
					DropShadowedDrawable dsd = new DropShadowedDrawable( getContext().getResources(), cd ) ;
					dsd.setForceIntrinsicContentSize(true, mThumbnailLoadWidth, (int)(mThumbnailLoadWidth*(8.0f/4.8f))) ;
					mDrawableBackgroundThumbnail = dsd ;
					
					// Set ourselves as a client for the load...
					if ( mThreadedBackgroundLoader != null ) {
						mThreadedBackgroundLoader.addClient(this) ;
					}
				}
			} else if ( bg != null && bg.getTemplate() == Background.Template.NONE ) {
				mDrawableBackgroundThumbnail = mDrawableNone ;
			}
		}
		
		mBackground = bg ;
		mAvailability = availability ;
		mActive = active ;
		mInShuffle = inShuffle ;
		if ( changed )
			refresh() ;
	}

	public Background getContentBackground() {
		return mBackground ;
	}
	
	public void setMode( Mode mode ) {
		boolean changed = mode != mMode ;
		mMode = mode ;
		if ( changed )
			refresh() ;
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
		
		ids.add( R.id.button_collage_background_button_main ) ;
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
		
		String textTitle ;
		int baseColor ;
		
		QuantroContentWrappingButton.ContentState contentState = QuantroContentWrappingButton.ContentState.OPTIONAL ;
		if ( mActive )
			contentState = QuantroContentWrappingButton.ContentState.EXCLUSIVE ;
		else if ( mInShuffle && mMode == Mode.SHUFFLE )
			contentState = QuantroContentWrappingButton.ContentState.ACTIVE ;
		
		QuantroContentWrappingButton.Style style ;
		if ( mMode == Mode.SHUFFLE )
			style = QuantroContentWrappingButton.Style.BLOCK_THREE_TIER_ENCLOSE_CONTENT_REVERSE ;
		else
			style = QuantroContentWrappingButton.Style.BLOCK_TWO_TIER_ENCLOSE_CONTENT ;
		
		
		if ( mBackground == null ) {
			textTitle = null ;
			baseColor = 0xffffffff ;
		} else {
			textTitle = mBackground.getName() ;
			baseColor = ( mMode == Mode.STANDARD || mActive )
					? mColor[Mode.STANDARD.ordinal()]
					: mColor[Mode.SHUFFLE.ordinal()] ;
		}
		
		// Now set these values in the appropriate content fields.
		changed = content.setTitle(textTitle) 					|| changed ;
		changed = content.setColor(baseColor)					|| changed ;
		changed = content.setAlert(showAlert, alert)			|| changed ;
		changed = content.setImage(true, mDrawableBackgroundThumbnail)	|| changed ;
		
		changed = button.isEnabled() != enabled 				|| changed ;
		button.setEnabled(enabled) ;
		button.setClickable(enabled) ;
		
		changed = button.getStyle() != style 					|| changed ;
		button.setStyle(style) ;
		
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
		
		boolean sound = false ;
		
		if ( mMode == Mode.STANDARD )
			sound = delegate.bbcd_setBackground(this, mBackground, mAvailability) ;
		else if ( mMode == Mode.SHUFFLE )
			sound = delegate.bbcd_setBackgroundInShuffle(this, mBackground, mAvailability, !mInShuffle) ;
		
		if ( sound && mSoundPool != null )
			mSoundPool.menuButtonClick() ;
	}
	
	@Override
	public boolean onLongClick(View v) {
		Delegate delegate = mwrDelegate.get() ;
		if ( delegate == null )
			return false ;
		
		if ( v != mButton )
			return false ;
		
		if ( mMode == Mode.STANDARD )
			return false ;		// no long-presses in STANDARD.
		
		// for a long press: add to shuffle and set as active.
		boolean soundSet = delegate.bbcd_setBackgroundAndIncludeInShuffle(this, mBackground, mAvailability) ;
		
		if ( mSoundPool != null && soundSet ) {
			mSoundPool.menuButtonHold() ;
		}
		
		return true ;
	}
	
	@Override
	public boolean supportsLongClick(QuantroContentWrappingButton qcwb) {
		return qcwb == mButton && mMode == Mode.SHUFFLE ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// TLC - Thumbnail Loader Client methods

	@Override
	synchronized public boolean tlc_setParams(ThreadedLoader tl, Params p) {
		if ( mBackground != null && mBackground.hasImage() ) {
			Bitmap b = this.mThumbnailSoftCache.get(mBackground) ;
			if ( b == null ) {
				ThreadedBackgroundThumbnailLoader.Params params = (ThreadedBackgroundThumbnailLoader.Params) p ;
				
				params.setBackground(mBackground) ;
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
		ThreadedBackgroundThumbnailLoader.Params params = (ThreadedBackgroundThumbnailLoader.Params) p ;
		ThreadedBackgroundThumbnailLoader.Result result = (ThreadedBackgroundThumbnailLoader.Result) r ;
		
		if ( params.getBackground().equals(mBackground) ) {
			Bitmap b = result.getBitmap() ;
			mThumbnailSoftCache.put(mBackground, b) ;
			setThumbnail(b) ;
			((Activity)getContext()).runOnUiThread(mRefreshRunnable) ;
		}
	}
	
}
