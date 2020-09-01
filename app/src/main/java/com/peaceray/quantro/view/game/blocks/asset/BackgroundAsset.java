package com.peaceray.quantro.view.game.blocks.asset;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigCanvas;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.utils.AssetAccessor;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.backgroundbuildable.BackgroundBuildable;
import com.peaceray.quantro.utils.backgroundbuildable.BackgroundBuilder;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.BitmapFactory.Options;
import android.util.Log;


/**
 * An "Asset" is a collection of preallocated / precomputed
 * data representing a modular component of the BlockDrawer's
 * internal data.  These components are meant to be individually
 * instantiated, prerendered, and swapped.  An 'Asset' is thus
 * a single, cohesive collection of data, at a level of granularity
 * allowing 1. As much freedom as possible when combining with other
 * assets, and 2. Complete consistency despite those combinations.
 * 
 * The exact lines of division have yet to be determined.  However,
 * we know enough to the background and relevant data in its own
 * container.
 * 
 * @author Jake
 *
 */
public class BackgroundAsset extends BackgroundBuildable.Implementation {
	
	private static final String TAG = "BackgroundAsset" ;

	private Background mBackground ;
	
	private Bitmap mBitmap ;
	private Rect mBitmapBounds ;
	
	private Paint mDrawPaint ;
	private Paint mClearPaint ;
	
	private boolean mRecycled ;
	private boolean mPreallocated ;
	private boolean mPrerendered ;
	
	/**
	 * Constructs a new BackgroundAsset using the provided Background.
	 * The provided 'bitmap' should be taken as 
	 * 
	 * @param background
	 * @param bestFit
	 */
	private BackgroundAsset() {
		mBackground = null ;
		mBitmap = null ;
		mBitmapBounds = new Rect() ;
		mDrawPaint = new Paint() ;
		mClearPaint = new Paint() ;
		mClearPaint.setColor(0x00000000);
		mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		
		mRecycled = false ;
		mPreallocated = false ;
		mPrerendered = false ;
	}
	
	public boolean isPreallocated() {
		bb_blockUntilBuiltOrInBuildingThread() ;
		return mPreallocated ;
	}
	
	public boolean isPrerendered() {
		bb_blockUntilBuiltOrInBuildingThread() ;
		return mPrerendered ;
	}
	
	public boolean isRecycled() {
		bb_blockUntilBuiltOrInBuildingThread() ;
		return mRecycled ;
	}
	
	
	/**
	 * Recycles any non-preallocated structures.
	 */
	public void recycle() {
		bb_blockUntilBuiltOrInBuildingThread() ;
		if ( !mRecycled ) {
			if ( mBitmap != null && !mPreallocated ) {
				mBitmap.recycle() ;
			}
			mRecycled = true ;
		}
	}
	
	
	public Background getBackground() {
		bb_blockUntilBuiltOrInBuildingThread() ;
		return mBackground ;
	}
	
	
	/**
	 * Returns the bitmap we use when drawing to the canvas, or 'null'
	 * if we draw a solid color instead.
	 * 
	 * If 'bounds' is provided, it will be set to the region of the bitmap
	 * which we use in our draw operations.
	 * 
	 * Note: like all public methods, this will block until the object
	 * is fully built.  However, because Locks are recursive, this method
	 * will return immediately if called by a BackgroundBuilderListener.
	 * This can be useful if you want to configure the bitmap, e.g. to
	 * "bake-in" edge shadows, immediately before building is complete.
	 * 
	 * @param bounds
	 * @return
	 */
	public Bitmap getBitmap( Rect bounds ) {
		bb_blockUntilBuiltOrInBuildingThread() ;
		if ( mRecycled )
			throw new IllegalStateException("Bitmap has been recycled!") ;
		if ( bounds != null && mBitmapBounds != null )
			bounds.set(mBitmapBounds) ;
		return mBitmap ;
	}
	
	
	/**
	 * Draws itself to the provided canvas using the specified configuration.
	 * @param canvas
	 * @param configCanvas
	 */
	public void draw( Canvas canvas, BlockDrawerConfigCanvas configCanvas ) {
		// don't block here; we block in the inner call.
		draw( canvas, configCanvas, 1.0f ) ;
	}
	

	/**
	 * Draws itself to the provided canvas using the specified configuration.
	 * @param canvas
	 * @param configCanvas
	 * @param alphaScale A multiplier for the alpha value.  Only obeyed for configCanvas.background == DEFAULT.
	 */
	public void draw( Canvas canvas, BlockDrawerConfigCanvas configCanvas, float alphaScale ) {
		bb_blockUntilBuiltOrInBuildingThread() ;
		if ( mRecycled )
			throw new IllegalStateException("Bitmap has been recycled!") ;
		
		if ( configCanvas.clipRegion != null ) {
			canvas.save() ;
			canvas.clipRect(configCanvas.clipRegion, Region.Op.INTERSECT) ;
		}
		
		draw( canvas, configCanvas.getRegion(), configCanvas.getBackground(), alphaScale ) ;
		
		if ( configCanvas.clipRegion != null ) {
			canvas.restore() ;
		}
	}

	/**
	 * Draws this Background to the provided Canvas.
	 * 
	 * @param canvas
	 * @param targetRegion
	 */
	public void draw( Canvas canvas, Rect targetRegion ) {
		// don't block here; we block in the inner call.
		draw( canvas, targetRegion, BlockDrawerConfigCanvas.Background.DEFAULT, 1.0f ) ;
	}
	
	/**
	 * Draws this background to the provided Canvas.
	 * 
	 * @param canvas
	 * @param targetRegion
	 * @param configCanvasBackground
	 */
	public void draw( Canvas canvas, Rect targetRegion, BlockDrawerConfigCanvas.Background configCanvasBackground ) {
		// don't block here; we block in the inner call.
		draw( canvas, targetRegion, configCanvasBackground, 1.0f ) ;
	}
	
	/**
	 * Draws this background to the provided Canvas.
	 * 
	 * @param canvas
	 * @param targetRegion
	 * @param alphaScale
	 */
	public void draw( Canvas canvas, Rect targetRegion, float alphaScale ) {
		// don't block here; we block in the inner call.
		draw( canvas, targetRegion, BlockDrawerConfigCanvas.Background.DEFAULT, alphaScale ) ;
	}
	
	
	
	
	/**
	 * Draws this Background to the provided Canvas.
	 * @param canvas
	 * @param targetRegion
	 * @param alphaScale
	 */
	public void draw( Canvas canvas, Rect targetRegion,
			BlockDrawerConfigCanvas.Background configCanvasBackground, float alphaScale ) {
		bb_blockUntilBuiltOrInBuildingThread() ;
		if ( mRecycled )
			throw new IllegalStateException("Bitmap has been recycled!") ;
		
		int alpha = Math.min(255, Math.round( Color.alpha( mBackground.getColor() ) * alphaScale ) ) ;
		mDrawPaint.setAlpha(alpha) ;
		
		switch( configCanvasBackground ) {
		case DEFAULT:
			if ( mBitmap != null )
				canvas.drawBitmap(mBitmap, mBitmapBounds, targetRegion, mDrawPaint) ;
			else
				canvas.drawRect(targetRegion, mDrawPaint) ;
			break ;
			
		case CLEAR:
			canvas.drawRect(targetRegion, mClearPaint) ;
			break ;
			
		case NONE:
		default:
			break ;
		}
	}
	
	
	public static class Builder extends BackgroundBuilder<BackgroundAsset> {

		private Context mContext ;
		
		// The Background we will use to draw.  Required.
		private Background mBackground ;
	
		// an example region: most of our background draws will be to
		// a region of this size.  Required.
		private Rect mRegion ;
		
		private int mLoadSize ;
		private int mMaxLoadWidth, mMaxLoadHeight ;
		private boolean mLoadSet ;
		
		private int mBackgroundSize ;
		private boolean mBackgroundSizeSet ;
		
		// a preallocated bitmap provided from outside, in which we will
		// draw the Background data.
		private Bitmap mPreallocatedBitmap ;
		
		// it is possible that a previous BackgroundAsset has rendered
		// a Background bitmap.  If mPrerenderedBounds is provided, and
		// matches the bounds we would otherwise calculate, we don't bother
		// doing any of our own rendering -- instead we trust the caller
		// to know what they're doing.
		private Rect mPrerenderedBounds ;
		
		
		// our background asset generation happens in two steps (assuming no
		// prerendered version is provided): first, we load the source Background
		// which is based on 'loadSize' and the max load dimensions.  Second,
		// we draw a scaled and cropped version of that source bitmap into our 
		// destination Bitmap.  Providing a 'loadInto' bitmap will, under some
		// circumstances, allow that initial load to take place within a preallocated
		// bitmap.
		private Bitmap mLoadIntoBitmap ;
		private WeakReference<Bitmap> mwrLoadIntoBitmap ;
		
		
		public Builder( Context context ) {
			mContext = context ;
			if ( context == null )
				throw new NullPointerException("Must provide non-null context") ;
			mBackground = null ;
			mRegion = null ;
			mLoadSet = false ;
			mBackgroundSize = -1 ;
			
			mPreallocatedBitmap = null ;
			mPrerenderedBounds = null ;
		}
		
		
		@Override
		protected BackgroundAsset bb_newEmptyBuildable() {
			if ( mBackground == null )
				throw new IllegalStateException("Must provide a Background to build with!") ;
			if ( mRegion == null )
				throw new IllegalStateException("Must provide a Region to measure with before building!") ;
			if ( !mLoadSet )
				throw new IllegalStateException("Must provide max load dimensions!") ;
			if ( !mBackgroundSizeSet )
				throw new IllegalStateException("Must provide background size!") ;
			
			return new BackgroundAsset() ;
		}

		@Override
		synchronized protected void bb_build(BackgroundAsset asset) {
			// set the asset background and paint.
			asset.mBackground = mBackground ;
			asset.mDrawPaint.setColor(mBackground.getColor()) ;
			
			// if no image, we're basically done.
			if ( !mBackground.hasImage() || mBackgroundSize == DrawSettings.IMAGES_SIZE_NONE ) {
				this.mLoadIntoBitmap = null ;
				this.mwrLoadIntoBitmap = null ;
				return ;
			}
			
			Rect clipBounds = getPredictedBitmapBounds() ;
			
			// These are the final boundaries for our bitmap (plus maybe an offset).
			// Check our prerendered for a match.
			if ( mPreallocatedBitmap != null && mPrerenderedBounds != null 
					&& mPrerenderedBounds.width() == clipBounds.width() && mPrerenderedBounds.height() == clipBounds.height() ) {
				// MATCH!
				Log.d(TAG, "bb_build Matches prerendered background.") ;
				asset.mBitmap = mPreallocatedBitmap ;
				asset.mBitmapBounds.set(mPrerenderedBounds) ;
				asset.mPreallocated = true ;
				asset.mPrerendered = true ;
				
				this.mLoadIntoBitmap = null ;
				this.mwrLoadIntoBitmap = null ;
				return ;
			}
			
			// Otherwise, we need the image into our bitmap.
			asset.mBitmapBounds = clipBounds ;
			if ( mPreallocatedBitmap != null ) {
				asset.mBitmap = mPreallocatedBitmap ;
				asset.mPreallocated = true ;
			}
			else {
				asset.mBitmap = Bitmap.createBitmap(clipBounds.width(), clipBounds.height(), Bitmap.Config.RGB_565) ;
			}
			
			// We have our bitmap and bounds, but we still need to draw
			// the actual image data in there.  First we need to load the image.
			
			// otherwise, we need to set the bitmap and its bounds.
			// it's worth getting a sense for our eventual bounds
			// dimensions.  We do this by checking the on-disk Bitmap
			// for size, and taking the minimum of its size and the
			// target dimension.  This will be our "background bounds,"
			// although we might offset it slightly.
			String assetPath = AssetAccessor.assetPathFromBackground(
					mBackground, mLoadSize, mMaxLoadWidth, mMaxLoadHeight) ;
			AssetManager am = mContext.getAssets();
			
			// load the image.
			Bitmap b ;
			boolean loadedIntoProvidedBitmap = false ;
			try {
				BitmapFactory.Options options = new BitmapFactory.Options() ;
				options.inSampleSize = 1 ;
				Bitmap loadInto = this.mLoadIntoBitmap ;
				if ( loadInto == null && this.mwrLoadIntoBitmap != null )
					loadInto = this.mwrLoadIntoBitmap.get() ;
				if ( loadInto != null && VersionCapabilities.supportsLoadInBitmap() ) {
					// check dimensions.
					BitmapFactory.Options imgSizeOptions = new BitmapFactory.Options() ;
					imgSizeOptions.inJustDecodeBounds = true ;
					BufferedInputStream buf = new BufferedInputStream(am.open(assetPath));
					BitmapFactory.decodeStream(buf, null, imgSizeOptions);
					buf.close();
					
					if ( imgSizeOptions.outWidth == loadInto.getWidth()
							&& imgSizeOptions.outHeight == loadInto.getHeight() ) {
						// perfect match.
						VersionSafe.setInBitmap(options, loadInto) ;
						loadedIntoProvidedBitmap = true ;
					} else {
						Log.d(TAG, "attempt to load into bitmap FAILED due to mis-matched dimension: provided has "
								+ loadInto.getWidth() + "x" + loadInto.getHeight()
								+ " on-disc asset is " + imgSizeOptions.outWidth + "x" + imgSizeOptions.outHeight) ;
					}
				}
				BufferedInputStream buf = new BufferedInputStream(am.open(assetPath));
				b = BitmapFactory.decodeStream(buf, null, options);
				buf.close();
			} catch ( IOException ioe ) {
				// do our best to mess things up.
				Log.d(TAG, ioe.toString()) ;
				ioe.printStackTrace() ;
				this.mLoadIntoBitmap = null ;
				this.mwrLoadIntoBitmap = null ;
				RuntimeException re = new RuntimeException("BackgroundAsset.Builder: Failed loading bitmap " + assetPath) ;
				throw re ;
			}
			
			// draw into our allocated bitmap.  We need a source rect;
			// we prefer to match the clip's aspect ratio, while fitting
			// exactly the height and/or width of our source bitmap.  We
			// should be centered in the other dimension.
			Rect srcRect = new Rect(0, 0, b.getWidth(), b.getHeight()) ;
			// match aspect ratio
			double clipWidthToHeight = ((double)clipBounds.width()) / ((double)clipBounds.height()) ;
			double srcWidthToHeight = ((double)b.getWidth()) / ((double)b.getHeight()) ;
			
			if ( srcWidthToHeight > clipWidthToHeight ) {
				// our src is too wide.  Shrink it horizontally.
				srcRect.right = Math.min(srcRect.right, (int)Math.round(srcRect.bottom * clipWidthToHeight)) ;
				srcWidthToHeight = ((double)srcRect.width()) / ((double)srcRect.height()) ;
			} else if ( srcWidthToHeight < clipWidthToHeight ) {
				// our src is too tall.  Shrink it vertically.
				srcRect.bottom = Math.min(srcRect.bottom, (int)Math.round(srcRect.right / clipWidthToHeight)) ;
				srcWidthToHeight = ((double)srcRect.width()) / ((double)srcRect.height()) ;
			}
			
			// center w/in the image.
			if ( srcRect.right < b.getWidth() )
				srcRect.offset( (b.getWidth() - srcRect.right)/2, 0 ) ;
			if ( srcRect.bottom < b.getHeight() )
				srcRect.offset( 0, (b.getHeight() - srcRect.bottom)/2 ) ;
			
			// draw
			Paint p = new Paint();
			p.setDither(true);
			p.setFilterBitmap(true);
			
			Canvas c = new Canvas(asset.mBitmap) ;
			c.drawBitmap(b, srcRect, asset.mBitmapBounds, p) ;
			
			// that's all.  Recycle our loaded bitmap, IF we didn't "load into"
			// a provided bitmap.
			if ( !loadedIntoProvidedBitmap ) {
				Log.d(TAG, "bb_build Recycling source image") ;
				b.recycle() ;
			}
			
			// no more 'load into' references...
			this.mLoadIntoBitmap = null ;
			this.mwrLoadIntoBitmap = null ;
		}
		
		
		public Builder setBackground( Background background ) {
			if ( bb_hasBuilt() )
				throw new IllegalStateException("Cannot change build parameters after building; can only build clone objects.") ;
			
			mBackground = background ;
			return this ;
		}
		
		
		public Builder setTarget( Rect target ) {
			if ( bb_hasBuilt() )
				throw new IllegalStateException("Cannot change build parameters after building; can only build clone objects.") ;
			
			mRegion = new Rect(target) ;
			return this ;
		}
		
		public Builder setLoadLimits( int loadSize ) {
			if ( bb_hasBuilt() )
				throw new IllegalStateException("Cannot change build parameters after building; can only build clone objects.") ;
			
			mLoadSize = loadSize ;
			mMaxLoadWidth = Integer.MAX_VALUE ;
			mMaxLoadHeight = Integer.MAX_VALUE ;
			mLoadSet = true ;
			return this ;
		}
		
		public Builder setBackgroundSize( int bgSize ) {
			if ( bb_hasBuilt() )
				throw new IllegalStateException("Cannot change build parameters after building; can only build clone objects.") ;
			
			mBackgroundSize = bgSize ;
			mBackgroundSizeSet = true ;
			return this ;
		}
		
		public Builder setLoadLimits( int loadSize, int maxWidth, int maxHeight ) {
			if ( bb_hasBuilt() )
				throw new IllegalStateException("Cannot change build parameters after building; can only build clone objects.") ;
			
			mLoadSize = loadSize ;
			mMaxLoadWidth = maxWidth >= 0 ? maxWidth : Integer.MAX_VALUE ;
			mMaxLoadHeight = maxHeight >= 0 ? maxHeight : Integer.MAX_VALUE ;
			mLoadSet = true ;
			return this ;
		}
		
		public Builder setPreallocatedBitmap( Bitmap b ) {
			if ( bb_hasBuilt() )
				throw new IllegalStateException("Cannot change build parameters after building; can only build clone objects.") ;
			
			mPreallocatedBitmap = b ;
			mPrerenderedBounds = null ;
			return this ;
		}
		
		public Builder setPrerenderedBitmap( Bitmap b, Rect prerenderedBounds ) {
			if ( bb_hasBuilt() )
				throw new IllegalStateException("Cannot change build parameters after building; can only build clone objects.") ;
			
			mPreallocatedBitmap = b ;
			mPrerenderedBounds = new Rect(prerenderedBounds) ;
			return this ;
		}
		
		/**
		 * Sets the 'loadInto' bitmap for use when loading Bitmap assets from
		 * storage (before cropping or scaling).
		 * 
		 * @param b
		 * @param holdReferenceUntilUse  If 'true', we hold a reference to 'b' until
		 * 		the bitmap is loaded (or this step is skipped), preventing it from
		 * 		being collected as garbage.  If 'false', we use a WeakReference<Bitmap>
		 * 		to hold the provided image, allowing a garbage collector to grab it
		 * 		before we use it.
		 * @return
		 */
		public Builder setLoadIntoBitmap( Bitmap b, boolean holdReferenceUntilUse ) {
			if ( b == null ) {
				this.mLoadIntoBitmap = null ;
				this.mwrLoadIntoBitmap = null ;
			} else {
				if ( holdReferenceUntilUse ) {
					this.mLoadIntoBitmap = b ;
					this.mwrLoadIntoBitmap = null ;
				} else {
					this.mLoadIntoBitmap = null ;
					this.mwrLoadIntoBitmap = new WeakReference<Bitmap>(b) ;
				}
			}
			
			return this; 
		}
		
		public Rect getPredictedBitmapBounds() {
			if ( mBackground == null )
				throw new IllegalStateException("Must provide a Background to build with!") ;
			if ( mRegion == null )
				throw new IllegalStateException("Must provide a Region to measure with before building!") ;
			if ( !mLoadSet )
				throw new IllegalStateException("Must provide max load dimensions!") ;
			if ( !mBackgroundSizeSet )
				throw new IllegalStateException("Must provide background size!") ;
			
			if ( !mBackground.hasImage() )
				return null ;
			
			// okay.  We wish to maintain our target region ratio.
			double targetWidthToHeight = ((double)mRegion.width()) / ((double)mRegion.height()) ;
			// However, our bitmap bounds are upper-bounded by:
			// the 'background size': anything less than HUGE has size-limits.
			// the target region: we keep our bounds within the same dimensions.
			// the preallocated bitmap: if provided, we don't draw outside it.
			
			int clipWidth = Integer.MAX_VALUE, clipHeight = Integer.MAX_VALUE ;
			switch( mBackgroundSize ) {
			case DrawSettings.IMAGES_SIZE_HUGE:
				break ;
			case DrawSettings.IMAGES_SIZE_LARGE:
				clipWidth = Math.min(clipWidth, 480) ;
				clipHeight = Math.min(clipHeight, 800) ;
				break ;
			case DrawSettings.IMAGES_SIZE_MID:
				clipWidth = Math.min(clipWidth, 240) ;
				clipHeight = Math.min(clipHeight, 400) ;
				break ;
			case DrawSettings.IMAGES_SIZE_SMALL:
			default:
				clipWidth = Math.min(clipWidth, 120) ;
				clipHeight = Math.min(clipHeight, 200) ;
				break ;
			}
			
			clipWidth = Math.min(clipWidth, mRegion.width()) ;
			clipHeight = Math.min(clipHeight, mRegion.height()) ;
			
			if ( mPreallocatedBitmap != null ) {
				clipWidth = Math.min(clipWidth, mPreallocatedBitmap.getWidth()) ;
				clipHeight = Math.min(clipHeight, mPreallocatedBitmap.getHeight()) ;
			}
			
			double clipWidthToHeight = ((double)clipWidth) / ((double)clipHeight) ;
			
			// keep our aspect ratio...
			if ( clipWidthToHeight > targetWidthToHeight ) {
				// our clip is too wide.  Shrink it horizontally.
				clipWidth = Math.min(clipWidth, (int)Math.round(clipHeight * targetWidthToHeight)) ;
				clipWidthToHeight = ((double)clipWidth) / ((double)clipHeight) ;
			} else if ( clipWidthToHeight < targetWidthToHeight ) {
				// our clip is too tall.  Shrink it vertically.
				clipHeight = Math.min(clipHeight, (int)Math.round(clipWidth / targetWidthToHeight)) ;
				clipWidthToHeight = ((double)clipWidth) / ((double)clipHeight) ;
			}
			
			// These are the final boundaries for our bitmap (plus maybe an offset).
			// Check our prerendered for a match.
			if ( mPreallocatedBitmap != null && mPrerenderedBounds != null 
					&& mPrerenderedBounds.width() == clipWidth && mPrerenderedBounds.height() == clipHeight ) {
				return new Rect( mPrerenderedBounds ) ;
			}
			
			return new Rect( 0, 0, clipWidth, clipHeight ) ;
		}
		
	}
	
}
