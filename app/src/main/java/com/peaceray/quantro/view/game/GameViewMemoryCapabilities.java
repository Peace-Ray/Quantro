package com.peaceray.quantro.view.game;

import android.app.Activity;
import android.util.DisplayMetrics;

import com.peaceray.quantro.utils.DeviceModel;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;


/**
 * A GameViewMemoryCapabilities instance has the purpose of predicting
 * how much memory the GameView will require at different 
 * levels of detail.  This information is not directly provided;
 * instead, this object informs the user regarding the best settings
 * for a GameView to avoid exceeding the heap size.
 * 
 * Generally speaking, memory capabilities prioritizes frame rate
 * over hi-detail, and in-game performance over load times.
 * 
 * @author Jake
 *
 */
public class GameViewMemoryCapabilities {
	
	private static final String TAG = "GVMCapabilities" ;
	
	private static final int MAX_SHEETS = 4 ;
	private static final int MAX_CUSTOM_SHADOW_Q_ORIENTATIONS = 24 ;		// over-estimate
	private static final int MAX_PRERENDERED_BORDER_Q_ORIENTATIONS = 16 ;	// over-estimate
	private static final int MAX_PRERENDERED_BORDER_WIDTH = 2 ;
	private static final int MAX_PRERENDERED_BLOCKS_Q_ORIENTATIONS = 2 ;	// over-estimate
	private static final int MAX_BACKGROUNDS = 2 ;
	
	private static final int MIN_SCREEN_WIDTH_FOR_SCALE = 360 ;
	
	private static final int LARGE_HEAP_MINIMUM_EMPTY_MEGABYTES = 12 ;
	
	private static final long REQUIRED_BYTES_TO_OVERLAY_GAME = 6 * 1024L * 1024L ;
				// 5 MB required to display the game options menu as a transparent
				// overlay of the game.  If we don't have this much memory left over,
				// we instead show the menu as an opaque set of options in their own
				// activity.
	private static final long REQUIRED_BYTES_TO_SHUFFLE_BACKGROUND = 8 * 1024L * 1024L ;		// Shuffling backgrounds requires loading a background asset while the game
																								// is ongoing, something that can take quite a bit of RAM.  Starting in 3.0, we
																								// are allowed to load Bitmaps into existing Bitmap objects.  This is referred
																								// to as "inBitmap" as a Factory option.  If we have this capability,
																								// than shuffling backgrounds does not require any more RAM than storing 2 and loading 1.
																								// 8 MB required to enable shuffling.
																								// Although every other game element is loaded once -- when a game view is
																								// started -- background shuffling requires in-place loads
																								// of large files.  There may not be heap space available for that,
																								// since the Android garbage collector is non-compacting.
																								// We require this much available space before enabling shuffling;
																								// way more than the image needs, but hopefully enough that contiguous
																								// space is available.
	private static final long RESERVED_BYTES = 18 * 1024L * 1024L ;			// 16 megabytes reserved for other things (was 12)
	private static final float RESERVED_BYTES_RATIO = 0.55f ;				// 50% of heap reserved (if higher than fixed value reserved bytes) (was 42%)
	private static final long MAX_RESERVED_BYTES = 26 * 1024L * 1024L ;		// Was 21.
	private static final double BYTES_SCALAR = 1.1 ;						// just-in-case, assume we need 110% of our calculated bytes.
	
	private static final long BYTES_PENALTY_NEXUS_S = 10 * 1024L * 1024L ;
	
	private static final int YES = 0 ;
	private static final int NO = 1 ;
	
	// We select the first settings group whose memory requirements we can meet.
	// If none, we default to WORST_CASE_SETTINGS.  We will attempt to include 
	// backgrounds of various sizes at each step.
	private static final int [][] SETTINGS = {
		new int [] { DrawSettings.IMAGES_SIZE_LARGE, 	DrawSettings.BLIT_SEPTUPLE, NO },
		new int [] { DrawSettings.IMAGES_SIZE_MID, 		DrawSettings.BLIT_SEPTUPLE, NO },
		new int [] { DrawSettings.IMAGES_SIZE_SMALL, 	DrawSettings.BLIT_SEPTUPLE, NO },
		
		new int [] { DrawSettings.IMAGES_SIZE_LARGE, 	DrawSettings.BLIT_FULL, NO },
		new int [] { DrawSettings.IMAGES_SIZE_MID, 		DrawSettings.BLIT_FULL, NO },
		new int [] { DrawSettings.IMAGES_SIZE_SMALL, 	DrawSettings.BLIT_FULL, NO }
	} ;
	private static final  int [] WORST_CASE_SETTINGS = { DrawSettings.IMAGES_SIZE_SMALL, DrawSettings.BLIT_NONE, YES } ;
	
	private static final int SETTINGS_INDEX_LOAD_IMAGE_SIZE = 0 ;
	private static final int SETTINGS_INDEX_BLIT = 1 ;
	private static final int SETTINGS_INDEX_RECYCLE_TO_VEIL = 2 ;
	
	
	// We might have "premptively displaced" as much as 1.5 rows.
	private static final float SEPTUPLE_EXTRA_HEIGHT_STEP = 0.18f ;	// 2 * 1/12th; 12 is minimum height in rows.
	private static final float SEPTUPLE_MIN_HEIGHT_FACTOR = 1.0f + SEPTUPLE_EXTRA_HEIGHT_STEP ;
	private static final float SEPTUPLE_MAX_HEIGHT_FACTOR = 1.4f ;
	
	
	private long mScreenWidth  ;
	private long mScreenHeight ;
	private int mScreenDpi ;
	private long mHeap ;		// in bytes
	private int mHeapMB ;
	// We try to keep things under "large heap" unless temporary stuff is
	// happening, e.g. displaying an overlay.
	private long mLargeHeap ;
	private int mLargeHeapMB ;
	
	
	private long mBytes ;
	
	// indexed by draw detail
	private int mLoadImagesSize ;		// DrawSettings constant
	private int mLoadBackgroundSize ;
	private int mNumBackgrounds ;
	private int mBlit ;						// DrawSettings constant.  Refactor: we require the same blit setting, regardless of drawSettings.
	private int mScale ;					//
	private boolean mRecycleToVeil ;	// true or false
	
	private float mSeptupleHeightFactor ;
	
	
	@Override
	public boolean equals( Object obj ) {
		if ( !(obj instanceof GameViewMemoryCapabilities) )
			return false ;
		
		GameViewMemoryCapabilities gvmc = (GameViewMemoryCapabilities)obj ;
		if ( mScreenWidth != gvmc.mScreenWidth )
			return false ;
		if ( mScreenHeight != gvmc.mScreenHeight )
			return false ;
		if ( mHeap != gvmc.mHeap )
			return false ;
		if ( mHeapMB != gvmc.mHeapMB )
			return false ;
		if ( mLargeHeap != gvmc.mLargeHeap )
			return false ;
		if ( mLargeHeapMB != gvmc.mLargeHeapMB )
			return false ;
		
		
		if ( mBytes != gvmc.mBytes )
			return false ;
		
		if ( mLoadImagesSize != gvmc.mLoadImagesSize )
			return false ;
		if ( mLoadBackgroundSize != gvmc.mLoadBackgroundSize )
			return false ;
		if ( mNumBackgrounds != gvmc.mNumBackgrounds )
			return false ;
		if ( mBlit != gvmc.mBlit )
			return false ;
		if ( mRecycleToVeil != gvmc.mRecycleToVeil )
			return false ;
		
		if ( mSeptupleHeightFactor != gvmc.mSeptupleHeightFactor )
			return false ;
		
		return true ;
	}
	
	/**
	 * Creates a new GVMC using the provided information.
	 * This object will make recommendations regarding
	 * LOAD_IMAGES, CACHE_IMAGES and BLIT settings.
	 * 
	 * @param screenWidth
	 * @param screenHeight
	 * @param heapMB
	 */
	private GameViewMemoryCapabilities( int screenWidth, int screenHeight, int densityDpi, int heapMB, int largeHeapMB ) {
		setup( screenWidth, screenHeight, densityDpi, heapMB, largeHeapMB, true, true ) ;
	}
	
	private GameViewMemoryCapabilities( Activity activity, boolean allowDisabledFeatures, boolean allowLowImageQuality ) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		
		int heap = VersionSafe.getMemoryClass(activity) ;
		int largeHeap = Math.max(heap, VersionSafe.getLargeMemoryClass(activity) - LARGE_HEAP_MINIMUM_EMPTY_MEGABYTES) ;
		
		int width = metrics.widthPixels ;
		int height = metrics.heightPixels ;

		activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);

		int densityDpi = metrics.densityDpi;
		
		setup( width, height, densityDpi, heap, largeHeap, allowDisabledFeatures, allowLowImageQuality ) ;
	}
	
	
	/**
	 * Creates and returns a new GameViewMemoryCapabilities
	 * instance, configured to produce the best possible result graphically.
	 * 
	 * If 'allowDisabledFeatures,' we produce the best possible result, even
	 * if it requires disabling background shuffling and/or the in-game menu
	 * overlay.
	 * 
	 * If 'allowLowImageQuality', we allow substantial downgrades of image quality
	 * (background and block images) in order to improve framerate or activate
	 * other features.
	 * 
	 * @param activity
	 * @param allowDisabledFeatures
	 * @param allowLowImageQuality
	 */
	public static GameViewMemoryCapabilities newInstance( Activity activity, boolean allowDisabledFeatures, boolean allowLowImageQuality ) {
		return new GameViewMemoryCapabilities( activity, allowDisabledFeatures, allowLowImageQuality ) ;
	}
	
	
	private void setup( int screenWidth, int screenHeight, int densityDpi, int heapMB, int largeHeapMB, boolean allowDisabledFeatures, boolean allowLowImageQuality ) {
		if ( screenHeight > screenWidth ) {
			mScreenWidth = screenWidth ;
			mScreenHeight = screenHeight ;
		} else {		// landscape dimensions?
			mScreenWidth = screenHeight ;
			mScreenHeight = screenWidth ;
		}
		mScreenDpi = densityDpi ;
		mHeapMB = heapMB ;
		mHeap = ((long)heapMB) * 1024L * 1024L ;
		
		mLargeHeapMB = largeHeapMB ;
		mLargeHeap = ((long)largeHeapMB) * 1024L * 1024L ;

		setBestSettings( allowDisabledFeatures, allowLowImageQuality ) ;
		validateSettings() ;
	}
	
	public int getScreenWidth() {
		return (int)mScreenWidth ;
	}
	
	public int getScreenHeight() {
		return (int)mScreenHeight ;
	}

	public int getScreenDensityDpi() {
		return mScreenDpi ;
	}
	
	public int getHeapMB() {
		return mHeapMB ;
	}
	
	public int getLargeHeapMB() {
		return mHeapMB ;
	}
	
	public int getLoadImagesSize() {
		return mLoadImagesSize ;
	}
	
	public int getBackgroundImageSize( ) {
		return mLoadBackgroundSize ;
	}
	
	public int getNumBackgrounds() {
		return mNumBackgrounds ;
	}
	
	public boolean getShuffleSupported() {
		return mNumBackgrounds > 1 ;
	}
	
	public int getBlit( ) {
		return mBlit ;
	}

	public int getScale( ) {
		return mScale ;
	}
	
	public float getBlitSeptupleStableHeightFactor() {
		return mSeptupleHeightFactor ;
	}
	
	public boolean getRecycleToVeil() {
		return mRecycleToVeil ;
	}
	
	public boolean getGameOverlaySupported() {
		// We need space in teh large heap for this.  Also, we need
		// it to not be a Nexus S.
		return ( mLargeHeapMB * 1024L * 1024L ) - mBytes >= REQUIRED_BYTES_TO_OVERLAY_GAME 
				&& !DeviceModel.is( DeviceModel.Name.NEXUS_S ) ;
	}
	
	public String getScreenWidthString() {
		return "" + (int)mScreenWidth ;
	}
	
	public String getScreenHeightString() {
		return "" + (int)mScreenHeight ;
	}
	
	public String getHeapMBString() {
		return "" + mHeapMB ;
	}
	
	public String getLargeHeapMBString() {
		return "" + mLargeHeapMB ;
	}
	
	public String getLoadImagesSizeString() {
		return loadImagesSizeString( mLoadImagesSize ) ;
	}
	
	public String getBackgroundImageSizeString( ) {
		return backgroundImageSizeString( mLoadBackgroundSize ) ;
	}
	
	public String getBlitString( ) {
		return blitString( mBlit, mScale ) ;
	}
	
	
	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder() ;
		sb.append("GameViewMemoryCapabilities") ;
		sb.append("(") ;
		sb.append(" HeapMB ").append(mHeapMB).append(" to ").append(mLargeHeapMB).append(", ") ;
		sb.append(" ScreenDim ").append(mScreenWidth).append("x").append(mScreenHeight).append("@").append(mScreenDpi).append(", ") ;
		sb.append(" LoadImagesSize ").append(loadImagesSizeString(mLoadImagesSize)).append(", ") ;
		sb.append(" BGImagesSize ")
				.append("" + mNumBackgrounds + " at ")
				.append(backgroundImageSizeString(mLoadBackgroundSize)).append(", ") ;
		sb.append(" Blit ").append(blitString(mBlit, mScale)) ;
		if ( mBlit == DrawSettings.BLIT_SEPTUPLE )
			sb.append(" x").append(mSeptupleHeightFactor) ;
		sb.append(", ") ;
		sb.append(" RecycleToVeil ").append(mRecycleToVeil).append(", ") ;
		sb.append(" OptionsOverlay ").append( getGameOverlaySupported() ).append(" :: ") ;
		sb.append(" total MB ").append( mBytes / (1024.0 * 1024.0) ).append(" )") ;
		return sb.toString() ;
	}
	
	private String loadImagesSizeString( int imagesSize ) {
		switch( imagesSize ) {
		case DrawSettings.IMAGES_SIZE_NONE:
			return "None" ;
		case DrawSettings.IMAGES_SIZE_SMALL:
			return "Small" ;
		case DrawSettings.IMAGES_SIZE_MID:
			return "Mid" ;
		case DrawSettings.IMAGES_SIZE_LARGE:
			return "Large" ;
		default:
			return "UNKNOWN" ;
		}
	}
	
	private String backgroundImageSizeString( int imagesSize ) {
		switch( imagesSize ) {
		case DrawSettings.IMAGES_SIZE_NONE:
			return "None" ;
		case DrawSettings.IMAGES_SIZE_SMALL:
			return "Small" ;
		case DrawSettings.IMAGES_SIZE_MID:
			return "Mid" ;
		case DrawSettings.IMAGES_SIZE_LARGE:
			return "Large" ;
		case DrawSettings.IMAGES_SIZE_HUGE:
			return "Huge" ;
		default:
			return "UNKNOWN" ;
		}
	}
	
	private String blitString( int blit, int scale ) {
		String suffix = "@(1 / " + scale;
		switch( blit ) {
		case DrawSettings.BLIT_NONE:
			return "None" + scale;
		case DrawSettings.BLIT_FULL:
			return "Full" + scale;
		case DrawSettings.BLIT_SEPTUPLE:
			return "Septuple" + scale;
		default:
			return "UNKNOWN" + scale;
		}
	}
	
	private String animationsString( int animations ) {
		switch( animations ) {
		case DrawSettings.DRAW_ANIMATIONS_NONE:
			return "None" ;
		case DrawSettings.DRAW_ANIMATIONS_STABLE_STUTTER:
			return "StableStutter" ;
		case DrawSettings.DRAW_ANIMATIONS_ALL:
			return "All" ;
		default:
			return "Unknown" ;
		}
	}
	
	
	/**
	 * Presently
	 */
	private void setBestSettings( boolean allowDisabledFeatures, boolean allowLowImageQuality ) {
		// Here's our preferred settings, in order.  We take the first
		// setting whose total bytes used is less than the number required.
		// If none qualify, use the worst.
		int scaleBase = (int)Math.ceil(mScreenDpi / 360.0);	// TODO play around with this. Nexus 4 = 320. Pixel 5 = 440.
		for ( int scaleTrial = 0; scaleTrial < 2; scaleTrial++ ) {
			int scale = scaleBase + scaleTrial;
			for ( int s = 0; s < SETTINGS.length; s++ ) {
				int loadImagesSize = SETTINGS[s][SETTINGS_INDEX_LOAD_IMAGE_SIZE] ;
				int loadBackgroundSize = minimumBackgroundSize(allowLowImageQuality, mScreenWidth, mScreenHeight) ;
				int blit = SETTINGS[s][SETTINGS_INDEX_BLIT] ;
				boolean recycleToVeil = SETTINGS[s][SETTINGS_INDEX_RECYCLE_TO_VEIL] == YES ;
				int numBackgrounds = allowDisabledFeatures ? minNumberBackgrounds() : maxNumberBackgrounds() ;
				float factor = SEPTUPLE_MIN_HEIGHT_FACTOR ;

				// if we don't allow low image quality, we might need to skip
				// this setting based on its 'loadImagesSize.'
				if ( !allowLowImageQuality && loadImagesSize < minimumLoadImageSize( allowLowImageQuality, blit, scale, mScreenHeight ) ) {
					continue ;
				}

				long bytes = bytes(loadImagesSize, loadBackgroundSize, numBackgrounds, blit, scale, factor ) ;

				if ( bytesOK( bytes, allowDisabledFeatures ) ) {
					mLoadImagesSize = loadImagesSize ;
					mLoadBackgroundSize = loadBackgroundSize ;
					mBlit = blit ;
					mScale = blit == DrawSettings.BLIT_NONE ? 1 : scale ;
					mRecycleToVeil = recycleToVeil ;
					mNumBackgrounds = maxNumberBackgrounds() ;
					mSeptupleHeightFactor = factor ;

					// try for MAX_BACKGROUNDS, then drop down to 1.
					while ( mNumBackgrounds >= 1 ) {
						mLoadBackgroundSize = loadBackgroundSize ;
						// expand the background as much as possible...
						while( mLoadBackgroundSize < DrawSettings.IMAGES_SIZE_HUGE
								&& bytesOK( bytes( mLoadImagesSize, mLoadBackgroundSize+1, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ), allowDisabledFeatures ) ) {
							mLoadBackgroundSize++ ;
						}

						if ( bytesOK( bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ), allowDisabledFeatures ) )
							break ;

						// If we allow disabling of features, we should be willing to decrease
						// the number of backgrounds.
						if ( allowDisabledFeatures ) {
							mNumBackgrounds-- ;
						} else {
							break ;
						}
					}

					// try expanding septuple factor.  We prefer a background overlay to
					// an expanded septuple height factor.
					bytes = bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ) ;
					while( mBlit == DrawSettings.BLIT_SEPTUPLE
							&& mSeptupleHeightFactor + SEPTUPLE_EXTRA_HEIGHT_STEP
							<= SEPTUPLE_MAX_HEIGHT_FACTOR ) {

						factor = mSeptupleHeightFactor + SEPTUPLE_EXTRA_HEIGHT_STEP ;
						if ( bytesOK( bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, factor ),
								allowDisabledFeatures ) ) {
							mSeptupleHeightFactor = factor ;
						} else {
							break ;
						}
					}

					// if we get here, these are the best settings available.
					mBytes = bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ) ;
					return ;
				}
			}
		}
		
		mLoadImagesSize = WORST_CASE_SETTINGS[SETTINGS_INDEX_LOAD_IMAGE_SIZE] ;
		mLoadBackgroundSize = DrawSettings.IMAGES_SIZE_SMALL ;
		mBlit = WORST_CASE_SETTINGS[SETTINGS_INDEX_BLIT] ;
		mScale = 1;	// drawing directly; no scale
		mRecycleToVeil = WORST_CASE_SETTINGS[SETTINGS_INDEX_RECYCLE_TO_VEIL] == YES ;
		
		mSeptupleHeightFactor = SEPTUPLE_MIN_HEIGHT_FACTOR ;
		
		// try for as many backgrounds as possible...
		while ( mNumBackgrounds >= 1 ) {
			mLoadBackgroundSize = DrawSettings.IMAGES_SIZE_SMALL ;
			// expand the background as much as possible...
			while( mLoadBackgroundSize < DrawSettings.IMAGES_SIZE_HUGE
					&& bytes( mLoadImagesSize, mLoadBackgroundSize+1, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ) < mHeap )
				mLoadBackgroundSize++ ;
			
			if ( bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ) < mHeap )
				break ;
			
			mNumBackgrounds-- ;
		}
		
		// try expanding septuple factor.  We prefer a background overlay to
		// an expanded septuple height factor.
		long bytes = bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ) ;
		boolean hasOverlay = bytes + REQUIRED_BYTES_TO_OVERLAY_GAME < mHeap ;
		while( mBlit == DrawSettings.BLIT_SEPTUPLE
				&& mSeptupleHeightFactor + SEPTUPLE_EXTRA_HEIGHT_STEP
				<= SEPTUPLE_MAX_HEIGHT_FACTOR ) {
			
			float factor = mSeptupleHeightFactor + SEPTUPLE_EXTRA_HEIGHT_STEP ;
			if ( bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, factor )
					< mHeap - ( hasOverlay ? REQUIRED_BYTES_TO_OVERLAY_GAME : 0 ) ) {
				mSeptupleHeightFactor = factor ;
			} else {
				break ;
			}
		}
		
		mBytes = bytes( mLoadImagesSize, mLoadBackgroundSize, mNumBackgrounds, mBlit, mScale, mSeptupleHeightFactor ) ;
	}
	
	
	/**
	 * Is the specified number of bytes an OK number to use
	 * @param bytes
	 * @param allowDisableFeatures
	 * @return
	 */
	private boolean bytesOK( long bytes, boolean allowDisableFeatures ) {
		long extraLargeHeapBytes = (allowDisableFeatures ? 0 : REQUIRED_BYTES_TO_OVERLAY_GAME) ;
		if ( mHeap == mLargeHeap )
			extraLargeHeapBytes = 0 ;
		
		return bytes < mHeap
				&& bytes + extraLargeHeapBytes < mLargeHeap ;
	}
	
	private void validateSettings() {
		if ( mScreenWidth / mScale < MIN_SCREEN_WIDTH_FOR_SCALE ) {
			mBlit = DrawSettings.BLIT_NONE;
			mScale = 1;
		}
	}
	
	
	private long bytes( int imageSize, int bgImageSize, int numBackgrounds, int blit, int scale, float septBlitFactor ) {
		long total = 0 ;
		total += bytes_custom( imageSize, blit, scale ) ;
		total += bytes_ui() ;
		total += bytes_background( bgImageSize, numBackgrounds, blit, scale ) ;
		total += bytes_load_background(bgImageSize) ;
		total += bytes_blit( blit, scale, septBlitFactor ) ;
		
		total += bytes_sheets( imageSize, blit, scale ) ;
		total += bytes_load_sheet( imageSize ) ;
		
		if ( DeviceModel.is( DeviceModel.Name.NEXUS_S ) ) {
			total += BYTES_PENALTY_NEXUS_S ;
		}
		
		return (long)Math.ceil(total * BYTES_SCALAR)
				+ Math.min( MAX_RESERVED_BYTES, Math.max( RESERVED_BYTES, Math.round( mHeap * RESERVED_BYTES_RATIO) ) ) ;
	}
	
	
	private long bytes_sheets( int imageSize, int blit, int scale ) {
		// "Sheets" are large bitmaps holding a total of 256 (16x16)
		// images representing effects applied to a block based on
		// the blocks surrounding it.  For example, a sheet may be used
		// to draw self-shadowing inside a block, dependent on the borders
		// drawn around its edges (blocks merge with edge other sometimes,
		// and different shadows result).
		// 
		// Currently, there are 4 sheets kept in memory for drawing a game:
		// inner shadows, drop shadows, and 2 types of glow effects.  This is 
		// dependent on DrawSettings and may increase for different glow styles.
		//
		// It is also important to note that CONSTRUCTING these sheets requires
		// significant memory, but that memory is accounted for in a different
		// method.
		//
		// There are a number of other custom structures, but they are handled
		// elsewhere.
		
		// Finally, remember that at present there is no scaling of the sheets
		// based on LoadImageSize.  Instead, they exactly fit the block sizes.
		// We overestimate block height as mScreenHeight / 18, adjusting for
		// the provided blit setting.
		
		int blockHeight = (int)Math.ceil( mScreenHeight / 18.0 ) / scale ;
		
		// image size is currently irrelevant.
		return blockHeight * blockHeight * 256L * ( MAX_SHEETS ) ;
	}
	
	
	private long bytes_custom( int imageSize, int blit, int scale ) {
		// Sheets are used for most drawing, but there is addition
		// content that must be stored as bitmaps, such as custom shadows
		// and precached borders.
		// 
		// Presently these structures are determined by blit settings,
		// not image size.
		//
		// Structures: custom (block-size) shadows, both inner and drop.
		// We also store borders in a pre-generated bitmap form.
		
		// These values are unrelated to imageSize; they use 'blit'
		// settings instead.
		
		int blockHeight = (int)Math.ceil( mScreenHeight / 18.0 ) / scale ;
		
		// drop and inner shadows
		long total = 2 * (long)(blockHeight * blockHeight) * MAX_CUSTOM_SHADOW_Q_ORIENTATIONS ;
		
		// borders
		for ( int w = 1; w < MAX_PRERENDERED_BORDER_WIDTH + 1; w++ )
			total += w * (long)(blockHeight * blockHeight * 0.1) * 4 * MAX_PRERENDERED_BORDER_Q_ORIENTATIONS ;
	
		total += (long)(blockHeight * blockHeight) * MAX_PRERENDERED_BLOCKS_Q_ORIENTATIONS ;
		
		return total ;
	}
	
	
	private long bytes_ui() {
		// As an overestimate, we assume that 20% of the screen is dedicated
		// to these custom bitmaps (the edges).  The ScorePanel also contains
		// prerendered structures, but those are quite small and are assumed
		// to be subsumed into the 20% side-panel total.
		return (long)(mScreenHeight * mScreenWidth * 0.2) ;
	}
	
	private long bytes_background( int imagesSize, int numBackgrounds, int blit, int scale ) {
		// Our background is stored at a size determined by the
		// MINIMUM of loadable image size and the current blit dimensions.
		
		long bb_image = 0 ;
		switch( imagesSize ) {
		case DrawSettings.IMAGES_SIZE_NONE:
			bb_image = 0 ;
			break ;
		case DrawSettings.IMAGES_SIZE_SMALL:
			bb_image = 200 * 120 * 2 ;		// RGB_565 image
			break ;
		case DrawSettings.IMAGES_SIZE_MID:
			bb_image = 400 * 240 * 2 ;		// RGB_565 image
			break ;
		case DrawSettings.IMAGES_SIZE_LARGE:
			bb_image = 800 * 480 * 2 ;		// RGB_565 image
			break ;
		case DrawSettings.IMAGES_SIZE_HUGE:
			bb_image = Math.max(
					mScreenWidth * mScreenHeight * 2,
					bb_image = 800 * 480 * 2 ) ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize imagesSize setting " + imagesSize) ;
		}
		
		long bb_blit = mScreenHeight * mScreenWidth * 2L / (scale * scale);	// RGB_565 image
		
		if ( VersionCapabilities.supportsLoadInBitmap() ) {
			// if we can load into an existing bitmap, it takes no more memory to
			// do so during a game (we don't need the "buffer" of a large amount of free
			// RAM).
			return numBackgrounds * Math.min(bb_blit, bb_image) ;
		}
		return numBackgrounds * Math.min(bb_blit, bb_image) + (numBackgrounds > 1 ? REQUIRED_BYTES_TO_SHUFFLE_BACKGROUND : 0) ;
	}
	
	/**
	 * Determines and returns the minimum background size which can reasonably
	 * be used.  If we don't allowLowImageQuality, this is the largest size
	 * which does not need to be downscaled in both directions.  If we
	 * allowLowImageQuality, this one size-category smaller.
	 * 
	 * @return
	 */
	private int minimumBackgroundSize( boolean allowLowImageQuality, long screenWidth, long screenHeight ) {
		if ( !allowLowImageQuality ) {
			if ( screenWidth < 240 && screenHeight < 400 ) {
				return DrawSettings.IMAGES_SIZE_SMALL ;
			} else if ( screenWidth < 480 && screenHeight < 800 ) {
				return DrawSettings.IMAGES_SIZE_MID ;
			} else {
				return DrawSettings.IMAGES_SIZE_LARGE ;
			}
		} else {
			if ( screenWidth < 480 && screenHeight < 800 ) {
				return DrawSettings.IMAGES_SIZE_SMALL ;
			} else {
				return DrawSettings.IMAGES_SIZE_MID ;
			}
		}
	}
	
	private int minNumberBackgrounds() {
		return 1 ;
	}
	
	private int maxNumberBackgrounds() {
		boolean shuffleSupported = VersionCapabilities.versionAtLeast(VersionCapabilities.VERSION_HONEYCOMB)
				&& !DeviceModel.is(DeviceModel.Name.NEXUS_S) ;
		return shuffleSupported ? MAX_BACKGROUNDS : 1 ;
	}
	
	
	
	/**
	 * Determines and returns the minimum image size which can reasonably
	 * be considered a "high quality" result.  We define "high quality" as the
	 * largest image load size which does not need to be downscaled in both
	 * dimensions to fit the screen.
	 *
	 * @return
	 */
	private int minimumLoadImageSize( boolean allowLowImageQuality, int blit, int scale, long screenHeight) {
		int blockHeight = (int)Math.ceil( screenHeight / 18.0 ) / scale;
		
		if ( !allowLowImageQuality ) {
			if ( blockHeight < 40 )
				return DrawSettings.IMAGES_SIZE_SMALL ;
			if ( blockHeight < 80 )
				return DrawSettings.IMAGES_SIZE_MID ;
			return DrawSettings.IMAGES_SIZE_LARGE ;
		} else {
			if ( blockHeight < 80 )
				return DrawSettings.IMAGES_SIZE_SMALL ;
			return DrawSettings.IMAGES_SIZE_MID ;
		}
	}
	
	
	/**
	 * Returns the memory required to load and/or cache source sheets
	 * from which draw sheets are constructed.
	 *
	 * @return
	 */
	private long bytes_load_sheet( int imagesSize ) {
		
		// Unlock draw sheets, these sheets are based on imageSize, not
		// screen dimensions.
		int blockHeight = 0 ;
		switch( imagesSize ) {
		case DrawSettings.IMAGES_SIZE_NONE:
			blockHeight = 0 ;
			break ;
		case DrawSettings.IMAGES_SIZE_SMALL:
			blockHeight = 20 ;
			break ;
		case DrawSettings.IMAGES_SIZE_MID:
			blockHeight = 40 ;
			break ;
		case DrawSettings.IMAGES_SIZE_LARGE:
			blockHeight = 80 ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize imagesSize or cacheSize " + imagesSize) ;
		}
		
		return blockHeight * blockHeight * 256L * 4	; // ARGB_8888 image.
		/*  * ( animations == DrawSettings.DRAW_ANIMATIONS_NONE ? MAX_SOURCE_SHEETS_NO_ANIMATION : MAX_SOURCE_SHEETS )  ;  */
	}
	
	
	private long bytes_load_background( int imagesSize ) {
		switch( imagesSize ) {
		case DrawSettings.IMAGES_SIZE_NONE:
			return 0 ;
		case DrawSettings.IMAGES_SIZE_SMALL:
			return 200 * 120 * 4 ;
		case DrawSettings.IMAGES_SIZE_MID:
			return 400 * 240 * 4 ;
		case DrawSettings.IMAGES_SIZE_LARGE:
		case DrawSettings.IMAGES_SIZE_HUGE:
			return 800 * 480 * 4 ;
		}
		
		throw new IllegalArgumentException("imagesSize value " + imagesSize + " is not recognized") ;
	}
	
	
	private long bytes_blit( int blit, int scale, float septFactor ) {
		// blit images are typically based around the draw area
		// dimensions.  GameView currently restricts the game area
		// to at most the middle 80% of the screen width (full height).
		
		int fullHeightMult = 2 ;
		double scaleMult = (1.0 / (scale * scale));
		
		switch( blit ) {
		case DrawSettings.BLIT_NONE:
			// no blitting; no extra space required
			return 0 ;
		case DrawSettings.BLIT_FULL:
			return (long)Math.ceil( (mScreenWidth * mScreenHeight * 0.8) * 4 * fullHeightMult * scaleMult) ;
		case DrawSettings.BLIT_SEPTUPLE:
			return (long)Math.ceil( (mScreenWidth * mScreenHeight * septFactor * 0.8) * 7 * 4 * scaleMult ) ;			// 7 with extra height factor
			// although we used to use an RGB_565 image for the seventh blit target,
			// we have always measured for full-color images.
		}
		
		throw new IllegalArgumentException("Blit value " + blit + " is not recognized") ;
	}
	
}
