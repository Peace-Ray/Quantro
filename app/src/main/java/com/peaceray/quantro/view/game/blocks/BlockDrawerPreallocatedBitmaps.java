package com.peaceray.quantro.view.game.blocks;

import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.view.game.DrawSettings;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * A convenient storage location for the Bitmaps that a BlockDrawer needs
 * to do its rendering.
 * 
 * We're trying to cut down on the number of Bitmap allocations used by
 * BlockDrawer.  One of the ways we're doing this is expanding the number
 * of preallocated bitmaps which are created upon Activity creation and
 * passed to the BlockDrawer for its own use.
 * 
 * Each expansion requires a new preallocated Bitmap array in QuantroApplication
 * which is passed to the BlockDrawer.  These are redundant operations, so
 * this is a container class to get everything done at once: pass once to
 * a static BlockDrawer method to preallocate, pass once to a BlockDrawer
 * instance to take those structures.
 * 
 * There are other advantages to using a class for this preallocated cache.
 * The first: it allows us to keep track of the most recently rendered
 * DrawSettings object, AND whether that object was completely rendered.
 * Why is this good?  Because we can completely skip the re-rendering process
 * if we're using the same draw settings!  We just need to retrieve the
 * images in the same order, which we guarantee given the same series of
 * calls.
 * 
 * Just do the following:
 * 
 * Before any attempt to retrieve or alter the contained bitmaps, call
 * 'getRenderedDrawSettings' and make whatever comparisons you need.
 * 
 * If you intend to re-use the same draw settings object, call 'resetForReuse.'
 * Otherwise, call 'reset.'
 * 
 * Then load (and prerender) each structure needed.  Finally, call 
 * setRenderedDrawSettings().
 * 
 * @author Jake
 *
 */
public class BlockDrawerPreallocatedBitmaps {
	
	boolean mRecycled = false ;
	
	int mBlockWidth, mBlockHeight ;

	// Rendered draw settings.
	DrawSettings mRenderedDrawSettings ;
	
	Bitmap [] mBlitBitmaps ;
	
	Bitmap [] mBackgroundBitmaps ;
	Background [] mBackgroundBitmapBackgrounds ;
	Rect [] mBackgroundBitmapBounds ;
	boolean [] mBackgroundBitmapInUse ;
	
	Bitmap [] mSheetBitmaps ;
	String [] mSheetBitmapNames ;
	boolean [] mSheetBitmapInUse ;
	
	Bitmap [] mRowNegativeOneBitmaps ;
	
	// Bitmaps storing borders for later draw.  By convention,
	// these bitmaps are drawn in an inner bounds starting at (0,0),
	// and then that bounded area is bitted to the canvas WITHOUT SCALING.
	// Compare against StretchableBorderBitmaps, which are filled with
	// color data but drawn after scaling.
	Bitmap [][] mUnstretchableBorderBitmaps_byDirection ;
	String [][] mUnstretchableBorderBitmapNames_byDirection ;
	boolean [][] mUnstretchableBorderBitmapInUse_byDirection ;
	
	// Bitmaps storing borders for later draw.  By convention,
	// these borders are drawn to completely fill the provided
	// area, and then "stretched to fit" when drawn.
	Bitmap [][] mStretchableBorderBitmaps_byDirection ;
	String [][] mStretchableBorderBitmapNames_byDirection ;
	boolean [][] mStretchableBorderBitmapInUse_byDirection ;
	
	// 'block-size.'  These are set to the size of a single block (although
	// we allow arbitrary sizes).  In use, like most, we simply return the
	// smallest unused image available that still matches the required dimensions.
	// No attempt to "knapsack" the required pixels is made; each block is one
	// single image with one single name.
	Bitmap [] mBlockSizeBitmaps ;
	String [] mBlockSizeBitmapNames ;
	boolean [] mBlockSizeBitmapInUse ;
	
	
	public BlockDrawerPreallocatedBitmaps( int blockWidth, int blockHeight ) {
		mRecycled = false ;
		
		mBlockWidth = blockWidth ;
		mBlockHeight = blockHeight ;
		
		mRenderedDrawSettings = null ;
		
		mBlitBitmaps = null ;
		mBackgroundBitmaps = null ;
		mBackgroundBitmapBackgrounds = null ;
		mBackgroundBitmapBounds = null ;
		mBackgroundBitmapInUse = null ;
		
		mSheetBitmaps = null ;
		mSheetBitmapNames = null ;
		mSheetBitmapInUse = null ;
		
		mRowNegativeOneBitmaps = null ;
		
		mUnstretchableBorderBitmaps_byDirection = null ;
		mUnstretchableBorderBitmapNames_byDirection = null ;
		mUnstretchableBorderBitmapInUse_byDirection = null ;
		
		mStretchableBorderBitmaps_byDirection = null ;
		mStretchableBorderBitmapNames_byDirection = null ;
		mStretchableBorderBitmapInUse_byDirection = null ;
		
		mBlockSizeBitmaps = null ;
		mBlockSizeBitmapNames = null ;
		mBlockSizeBitmapInUse = null ;
	}
	
	public int getBlitWidth() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlitBitmaps == null || mBlitBitmaps.length == 0 )
			return 0 ;
		
		int width = Integer.MAX_VALUE ;
		for ( int i = 0; i < mBlitBitmaps.length; i++ )
			width = Math.min( width, mBlitBitmaps[i].getWidth() ) ;
		return width ;
	}
	
	public int getBlitHeight() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlitBitmaps == null || mBlitBitmaps.length == 0 )
			return 0 ;
		
		int height = Integer.MAX_VALUE ;
		for ( int i = 0; i < mBlitBitmaps.length; i++ )
			height = Math.min( height, mBlitBitmaps[i].getHeight() ) ;
		return height ;
	}
	
	public int getBlockWidth() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return mBlockWidth ;
	}
	
	public int getBlockHeight() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return mBlockHeight ;
	}
	
	/**
	 * Recycles all contained bitmaps.  This is a dangerous call, and should be
	 * the last call made to this object.
	 * 
	 * @return
	 */
	public boolean isRecycled() {
		return mRecycled ;
	}
	
	public void recycle() {
		if ( mRecycled )
			return ;
		
		// recycle all contained bitmaps
		recycle( mBlitBitmaps ) ;
		recycle( mBackgroundBitmaps ) ;
		recycle( mSheetBitmaps ) ;
		
		recycle( mRowNegativeOneBitmaps ) ;
		recycle( mUnstretchableBorderBitmaps_byDirection ) ;
		recycle( mStretchableBorderBitmaps_byDirection ) ;
		recycle( mBlockSizeBitmaps ) ;
		
		mRenderedDrawSettings = null ;
		
		mBlitBitmaps = null ;
		mBackgroundBitmaps = null ;
		mBackgroundBitmapBackgrounds = null ;
		mBackgroundBitmapBounds = null ;
		mBackgroundBitmapInUse = null ;
		
		mSheetBitmaps = null ;
		mSheetBitmapNames = null ;
		mSheetBitmapInUse = null ;
		
		mRowNegativeOneBitmaps = null ;
		
		mUnstretchableBorderBitmaps_byDirection = null ;
		mUnstretchableBorderBitmapNames_byDirection = null ;
		mUnstretchableBorderBitmapInUse_byDirection = null ;
		
		mStretchableBorderBitmaps_byDirection = null ;
		mStretchableBorderBitmapNames_byDirection = null ;
		mStretchableBorderBitmapInUse_byDirection = null ;
		
		mBlockSizeBitmaps = null ;
		mBlockSizeBitmapNames = null ;
		mBlockSizeBitmapInUse = null ;
		
		mRecycled = true ;
	}
	
	
	private void recycle( Bitmap[][] b ) {
		if ( b == null )
			return ;
		for ( int i = 0; i < b.length; i++ )
			recycle( b[i] ) ;
	}
	
	private void recycle( Bitmap [] b ) {
		if ( b == null )
			return ;
		for ( int i = 0; i < b.length; i++ )
			recycle( b[i] ) ;
	}
	
	private void recycle( Bitmap b ) {
		if ( b != null && !b.isRecycled() )
			b.recycle() ;
	}
	
	
	public DrawSettings getRenderedDrawSettings() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return mRenderedDrawSettings ;
	}

	public boolean isRenderOK() {
		// TODO: implement a more straightforward and
		// potentially more granular way of doing this.
		return !mRecycled && mRenderedDrawSettings != null ;
	}
	
	public void setRenderedDrawSettings( DrawSettings ds ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mRenderedDrawSettings = ds == null ? null : new DrawSettings(ds) ;
	}
	
	
	/**
	 * Call this when provided to a new BlockDrawer.  Resets which
	 * bitmaps are "in use," and nulls the previously rendered DrawSettings.
	 */
	public void reset() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		resetForReuse() ;
		mRenderedDrawSettings = null ;
	}
	
	
	/**
	 * We assume that the process of prerendering BlockDrawer
	 * Bitmaps is deterministic given DrawSettings.  Therefore,
	 * if a particular DrawSettings object has already been used for
	 * rendering these structures, then the appropriate image content
	 * is already present in these bitmaps, and -- most importantly --
	 * THEY WILL BE RETRIEVED IN EXACTLY THE SAME ORDER.  This allows
	 * us to save time and image loads by assuming the content has
	 * ALREADY been set.
	 */
	public void resetForReuse() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBackgroundBitmapInUse != null )
			for ( int i = 0; i < mBackgroundBitmapInUse.length; i++ )
				mBackgroundBitmapInUse[i] = false ;
		if ( mSheetBitmapInUse != null )
			for ( int i = 0; i < mSheetBitmapInUse.length; i++ ) 
				mSheetBitmapInUse[i] = false ;
		if ( mUnstretchableBorderBitmapInUse_byDirection != null )
			for ( int i = 0; i < mUnstretchableBorderBitmapInUse_byDirection.length; i++ )
				for ( int j = 0; j < mUnstretchableBorderBitmapInUse_byDirection[i].length; j++ )
					mUnstretchableBorderBitmapInUse_byDirection[i][j] = false ;
		if ( mStretchableBorderBitmapInUse_byDirection != null )
			for ( int i = 0; i < mStretchableBorderBitmapInUse_byDirection.length; i++ )
				for ( int j = 0; j < mStretchableBorderBitmapInUse_byDirection[i].length; j++ )
					mStretchableBorderBitmapInUse_byDirection[i][j] = false ;
		if ( mBlockSizeBitmapInUse != null )
			for ( int i = 0; i < mBlockSizeBitmapInUse.length; i++ )
				mBlockSizeBitmapInUse[i] = false ;
	}
	
	
	
	public void setBlitBitmaps( Bitmap [] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mBlitBitmaps = b ;
	}
	
	public void setBackgroundBitmaps( Bitmap [] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;

		mBackgroundBitmaps = b ;
		mBackgroundBitmapBackgrounds = new Background[b.length] ;
		mBackgroundBitmapBounds = new Rect[b.length] ;
		mBackgroundBitmapInUse = new boolean[b.length] ;
		for ( int i = 0; i < b.length; i++ ) {
			mBackgroundBitmapBackgrounds[i] = null ;
			mBackgroundBitmapBounds[i] = null ;
			mBackgroundBitmapInUse[i] = false ;
		}
	}
	
	public void setSheetBitmaps( Bitmap [] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mSheetBitmaps = b ;
		mSheetBitmapNames = new String[b.length] ;
		mSheetBitmapInUse = new boolean[b.length] ;
		for ( int i = 0; i < b.length; i++ ) {
			mSheetBitmapNames[i] = null ;
			mSheetBitmapInUse[i] = false ;
		}
	}
	
	public void setRowNegativeOneBitmaps( Bitmap [] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mRowNegativeOneBitmaps = b ;
	}
	
	
	public void setBorderBitmaps_byDirection( Bitmap[][] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mUnstretchableBorderBitmaps_byDirection = b ;
		mUnstretchableBorderBitmapNames_byDirection = new String[b.length][] ;
		mUnstretchableBorderBitmapInUse_byDirection = new boolean[b.length][] ;
		for ( int i = 0; i < b.length; i++ ) {
			mUnstretchableBorderBitmapNames_byDirection[i] = new String[b[i].length] ;
			mUnstretchableBorderBitmapInUse_byDirection[i] = new boolean[b[i].length] ;
			for ( int j = 0; j < b[i].length; j++ ) {
				mUnstretchableBorderBitmapNames_byDirection[i][j] = null ;
				mUnstretchableBorderBitmapInUse_byDirection[i][j] = false ;
			}
		}
	}

	public void setUnstretchableBorderBitmaps_byDirection( Bitmap[][] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mUnstretchableBorderBitmaps_byDirection = b ;
		mUnstretchableBorderBitmapNames_byDirection = new String[b.length][] ;
		mUnstretchableBorderBitmapInUse_byDirection = new boolean[b.length][] ;
		for ( int i = 0; i < b.length; i++ ) {
			mUnstretchableBorderBitmapNames_byDirection[i] = new String[b[i].length] ;
			mUnstretchableBorderBitmapInUse_byDirection[i] = new boolean[b[i].length] ;
			for ( int j = 0; j < b[i].length; j++ ) {
				mUnstretchableBorderBitmapNames_byDirection[i][j] = null ;
				mUnstretchableBorderBitmapInUse_byDirection[i][j] = false ;
			}
		}
	}
	
	public void setStretchableBorderBitmaps_byDirection( Bitmap[][] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mStretchableBorderBitmaps_byDirection = b ;
		mStretchableBorderBitmapNames_byDirection = new String[b.length][] ;
		mStretchableBorderBitmapInUse_byDirection = new boolean[b.length][] ;
		for ( int i = 0; i < b.length; i++ ) {
			mStretchableBorderBitmapNames_byDirection[i] = new String[b[i].length] ;
			mStretchableBorderBitmapInUse_byDirection[i] = new boolean[b[i].length] ;
			for ( int j = 0; j < b[i].length; j++ ) {
				mStretchableBorderBitmapNames_byDirection[i][j] = null ;
				mStretchableBorderBitmapInUse_byDirection[i][j] = false ;
			}
		}
	}
	
	
	public void setBlockSizeBitmaps( Bitmap[] b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		mBlockSizeBitmaps = b ;
		mBlockSizeBitmapNames = new String[b.length];
		mBlockSizeBitmapInUse = new boolean[b.length];
	}
	
	
	public Bitmap [] getBlitBitmaps() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return mBlitBitmaps ;
	}
	
	public Bitmap getBackgroundBitmap( Background bg ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( bg == null || !bg.hasImage() )
			return null ;
		
		// Look for a BackgroundBitmap.  We return one that is not
		// in use, and prefer one using the same background.
		for ( int i = 0; i < mBackgroundBitmaps.length; i++ ) {
			if ( !mBackgroundBitmapInUse[i] && bg.equals(mBackgroundBitmapBackgrounds[i]) ) {
				mBackgroundBitmapInUse[i] = true ;
				// don't change the boundary rect; we might have rendered
				// shadows to it.
				return mBackgroundBitmaps[i] ;
			}
		}
		
		// Now try looking for one that is simply not in use right now.
		for ( int i = 0; i < mBackgroundBitmaps.length; i++ ) {
			if ( !mBackgroundBitmapInUse[i] ) {
				mBackgroundBitmapInUse[i] = true ;
				mBackgroundBitmapBounds[i] = null ;
				mBackgroundBitmapBackgrounds[i] = bg ;
				// don't change the boundary rect; we might have rendered
				// shadows to it.
				return mBackgroundBitmaps[i] ;
			}
		}
		
		// all are in use right now.
		return null ;
	}
	
	
	/**
	 * Notes that this bitmap, which was provided as preallocated for the
	 * specified background, is no longer in use and should be "retired" --
	 * i.e., made available for later retrieval, potentially for a different
	 * background.
	 * 
	 * @param bitmap
	 * @param bg
	 * @return Whether this bitmap was successfully retired.
	 */
	public boolean retireBackgroundBitmap( Bitmap bitmap ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		for ( int i = 0; i < mBackgroundBitmaps.length; i++ ) {
			if ( mBackgroundBitmapInUse[i] && bitmap == mBackgroundBitmaps[i] ) {
				mBackgroundBitmapInUse[i] = false ;
				return true ;
			}
		}
		
		return false ;
	}
	
	
	
	/**
	 * Returns a Rectangle indicating the bounds set for the provided
	 * bitmap and Background, or 'null' if this bitmap / background
	 * combination does not exist in our records.
	 * 
	 * Will return 'null' if the provided bounds are empty.
	 * 
	 * @param b
	 * @param bg
	 * @return
	 */
	public Rect getBackgroundBitmapBounds( Bitmap b, Background bg ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBackgroundBitmaps == null )
			return null ;
		
		for ( int i = 0; i < mBackgroundBitmaps.length; i++ ) {
			if ( mBackgroundBitmaps[i] == b && bg.equals(mBackgroundBitmapBackgrounds[i]) ) {
				return mBackgroundBitmapBounds[i] ;
			}
		}
		
		return null ;
	}
	
	public void setBackgroundBitmapBounds( Bitmap b, Background bg, Rect r ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		for ( int i = 0; i < mBackgroundBitmaps.length; i++ ) {
			if ( mBackgroundBitmaps[i] == b && bg.equals(mBackgroundBitmapBackgrounds[i]) ) {
				if ( mBackgroundBitmapBounds[i] != null )
					mBackgroundBitmapBounds[i].set(r) ;
				else
					mBackgroundBitmapBounds[i] = new Rect(r) ;
				return ;
			}
		}
	}
	
	/**
	 * We require a name when retrieving sheet bitmaps.  Will return 'null',
	 * indicating that we cannot provide a sheet bitmap, or a reference to the
	 * Bitmap in question.
	 * 
	 * If a bitmap was previously named the provided name (which MUST NOT be null...),
	 * we provide that bitmap.  Otherwise, we provide a new bitmap and note the provided
	 * name.
	 * 
	 * If a bitmap is returned, it will be considered "in use" and have the
	 * specified name.
	 * 
	 * @param name
	 * @return
	 */
	public Bitmap getSheetBitmap( String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return getNamedBitmapAndSetInUse( name, mSheetBitmaps, mSheetBitmapNames, mSheetBitmapInUse ) ;
	}
	
	
	public int getNumUnusedSheetBitmaps() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return getNumUnusedBitmaps( mSheetBitmapInUse ) ;
	}
	
	public boolean hasSheetBitmap( Bitmap b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return hasBitmap( b, mSheetBitmaps ) ;
	}
	
	public boolean hasSheetBitmap( String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return hasBitmap( name, mSheetBitmapNames ) ;
	}
	
	public boolean hasSheetBitmapInUse( String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return hasBitmapInUse( name, mSheetBitmapNames, mSheetBitmapInUse ) ;
	}
	
	public Bitmap [] getRowNegativeOneBitmaps() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		return mRowNegativeOneBitmaps ;
	}
	
	public boolean hasRowNegativeOneBitmap( Bitmap b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mRowNegativeOneBitmaps == null )
			return false ;
		for ( int i = 0; i < mRowNegativeOneBitmaps.length; i++ )
			if ( mRowNegativeOneBitmaps[i] == b )
				return true ;
		return false ;
	}
	
	
	
	/**
	 * We require a name when retrieving border bitmaps.  Will return 'null',
	 * indicating that we cannot provide a border bitmap, or a reference to the
	 * Bitmap in question.
	 * 
	 * If a bitmap was previously named the provided name (which MUST NOT be null...),
	 * we provide that bitmap.  Otherwise, we provide a new bitmap and note the provided
	 * name.
	 * 
	 * If a bitmap is returned, it will be considered "in use" and have the
	 * specified name.
	 * 
	 * @param name
	 * @return
	 */
	public Bitmap getUnstretchableBorderBitmap( int direction, String name, int minWidth, int minHeight ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mUnstretchableBorderBitmaps_byDirection == null )
			return null ;
		return getNamedBitmapAndSetInUse(
				name, minWidth, minHeight,
				mUnstretchableBorderBitmaps_byDirection[direction],
				mUnstretchableBorderBitmapNames_byDirection[direction],
				mUnstretchableBorderBitmapInUse_byDirection[direction] ) ;
	}
	
	public int getNumUnstretchableBorderBitmaps() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mUnstretchableBorderBitmaps_byDirection == null )
			return 0 ;
		int num = 0 ;
		for ( int i = 0; i < mUnstretchableBorderBitmapInUse_byDirection.length; i++ )
			num += mUnstretchableBorderBitmapInUse_byDirection[i].length ;
		return num ;
	}
	
	public int getNumUnstretchableBorderBitmaps( int direction ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mUnstretchableBorderBitmaps_byDirection == null )
			return 0 ;
		return mUnstretchableBorderBitmapInUse_byDirection[direction].length ;
	}
	
	
	public int getNumUnusedUnstretchableBorderBitmaps( int direction ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mUnstretchableBorderBitmaps_byDirection == null )
			return 0 ;
		return getNumUnusedBitmaps( mUnstretchableBorderBitmapInUse_byDirection[direction] ) ;
	}
	
	public boolean hasUnstretchableBorderBitmap( int direction, Bitmap b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mUnstretchableBorderBitmaps_byDirection == null )
			return false ;
		return hasBitmap( b, mUnstretchableBorderBitmaps_byDirection[direction] ) ;
	}
	
	public boolean hasUnstretchableBorderBitmap( int direction, String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mUnstretchableBorderBitmaps_byDirection == null )
			return false ;
		return hasBitmap( name, mUnstretchableBorderBitmapNames_byDirection[direction] ) ;
	}
	
	public boolean hasUnstretchableBorderBitmapInUse( int direction, String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mUnstretchableBorderBitmaps_byDirection == null )
			return false ;
		return hasBitmapInUse( name,
				mUnstretchableBorderBitmapNames_byDirection[direction],
				mUnstretchableBorderBitmapInUse_byDirection[direction] ) ;
	}
	
	
	/**
	 * We require a name when retrieving border bitmaps.  Will return 'null',
	 * indicating that we cannot provide a border bitmap, or a reference to the
	 * Bitmap in question.
	 * 
	 * If a bitmap was previously named the provided name (which MUST NOT be null...),
	 * we provide that bitmap.  Otherwise, we provide a new bitmap and note the provided
	 * name.
	 * 
	 * If a bitmap is returned, it will be considered "in use" and have the
	 * specified name.
	 * 
	 * @param name
	 * @return
	 */
	public Bitmap getStretchableBorderBitmap( int direction, String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mStretchableBorderBitmaps_byDirection == null )
			return null ;
		return getNamedBitmapAndSetInUse(
				name,
				mStretchableBorderBitmaps_byDirection[direction],
				mStretchableBorderBitmapNames_byDirection[direction],
				mStretchableBorderBitmapInUse_byDirection[direction] ) ;
	}
	
	public int getNumStretchableBorderBitmaps() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mStretchableBorderBitmaps_byDirection == null )
			return 0 ;
		int num = 0 ;
		for ( int i = 0; i < mStretchableBorderBitmapInUse_byDirection.length; i++ )
			num += mStretchableBorderBitmapInUse_byDirection[i].length ;
		return num ;
	}
	
	public int getNumStretchableBorderBitmaps( int direction ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mStretchableBorderBitmaps_byDirection == null )
			return 0 ;
		return mStretchableBorderBitmapInUse_byDirection[direction].length ;
	}
	
	
	public int getNumUnusedStretchableBorderBitmaps( int direction ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mStretchableBorderBitmaps_byDirection == null )
			return 0 ;
		return getNumUnusedBitmaps( mStretchableBorderBitmapInUse_byDirection[direction] ) ;
	}
	
	public boolean hasStretchableBorderBitmap( int direction, Bitmap b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mStretchableBorderBitmaps_byDirection == null )
			return false ;
		return hasBitmap( b, mStretchableBorderBitmaps_byDirection[direction] ) ;
	}
	
	public boolean hasStretchableBorderBitmap( int direction, String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mStretchableBorderBitmaps_byDirection == null )
			return false ;
		return hasBitmap( name, mStretchableBorderBitmapNames_byDirection[direction] ) ;
	}
	
	public boolean hasStretchableBorderBitmapInUse( int direction, String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mStretchableBorderBitmaps_byDirection == null )
			return false ;
		return hasBitmapInUse( name,
				mStretchableBorderBitmapNames_byDirection[direction],
				mStretchableBorderBitmapInUse_byDirection[direction] ) ;
	}
	

	
	/**
	 * We require a name when retrieving block size bitmaps.  Will return 'null',
	 * indicating that we cannot provide a block size bitmap, or a reference to the
	 * Bitmap in question.
	 * 
	 * If a bitmap was previously named the provided name (which MUST NOT be null...),
	 * we provide that bitmap.  Otherwise, we provide a new bitmap and note the provided
	 * name.
	 * 
	 * If a bitmap is returned, it will be considered "in use" and have the
	 * specified name.
	 * 
	 * @param name
	 * @return
	 */
	public Bitmap getBlockSizeBitmap( String name, int minWidth, int minHeight, Bitmap.Config config ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlockSizeBitmaps == null )
			return null ;
		return getNamedBitmapAndSetInUse(
				name, minWidth, minHeight,
				true, config,
				mBlockSizeBitmaps,
				mBlockSizeBitmapNames,
				mBlockSizeBitmapInUse ) ;
	}
	
	public int getNumBlockSizeBitmaps() {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlockSizeBitmaps == null )
			return 0 ;
		return mBlockSizeBitmaps.length ;
	}
	
	public int getNumUnusedBlockSizeBitmaps( ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlockSizeBitmaps == null )
			return 0 ;
		return getNumUnusedBitmaps( mBlockSizeBitmapInUse ) ;
	}
	
	public boolean hasBlockSizeBitmap( Bitmap b ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlockSizeBitmaps == null )
			return false ;
		return hasBitmap( b, mBlockSizeBitmaps ) ;
	}
	
	public boolean hasBlockSizeBitmap( String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlockSizeBitmaps == null )
			return false ;
		return hasBitmap( name, mBlockSizeBitmapNames ) ;
	}
	
	public boolean hasBlockSizeBitmapInUse( String name ) {
		if ( mRecycled )
			throw new IllegalStateException("Recycled") ;
		
		if ( mBlockSizeBitmaps == null )
			return false ;
		return hasBitmapInUse( name,
				mBlockSizeBitmapNames,
				mBlockSizeBitmapInUse ) ;
	}
	
	
	private static Bitmap getNamedBitmapAndSetInUse( String name, Bitmap [] b, String [] names, boolean [] inUse ) {
		return getNamedBitmapAndSetInUse(
				name, -1, -1, false, Bitmap.Config.ARGB_8888, b, names, inUse) ;
	}
	
	private static Bitmap getNamedBitmapAndSetInUse( String name, int minWidth, int minHeight, Bitmap [] b, String [] names, boolean [] inUse ) {
		return getNamedBitmapAndSetInUse(
				name, minWidth, minHeight, false, Bitmap.Config.ARGB_8888, b, names, inUse) ;
	}
	
	
	
	
	private static Bitmap getNamedBitmapAndSetInUse(
			String name,
			int minWidth, int minHeight,
			boolean checkConfig, Bitmap.Config config,
			Bitmap [] b, String [] names, boolean [] inUse ) {
		// We prioritize:
		// 1. returning one with the same name
		// 2. returning the smallest unnamed (in terms of leftover pixels) bitmap
		//			in a tie, take the first.
		// 3. returning (and re-naming) the smallest named but unused (in terms of leftover pixels) bitmap
		//			in a tie, take the last.
		
		int index = -1 ;
		long extraPixels = Long.MAX_VALUE ;
		if ( b != null ) {
			// look for this name...
			for ( int i = 0; i < b.length; i++ ) {
				if ( name.equals(names[i]) ) {
					if ( checkConfig && config != b[i].getConfig() )
						continue ;
					if ( !sizeAtLeast( b[i], minWidth, minHeight ) )
						continue ;
					index = i ;
					break ;
				}
			}
			
			if ( index == -1 ) {
				// look for the FIRST unnamed (and not in use) bitmap...
				for ( int i = 0; i < b.length; i++ ) {
					if ( names[i] == null
							&& !inUse[i]
							&& sizeAtLeast( b[i], minWidth, minHeight )
							&& (!checkConfig || config == b[i].getConfig()) ) {
						long p = pixelsLeftOver(b[i], minWidth, minHeight) ;
						if ( p < extraPixels ) {
							index = i ;
							extraPixels = p ;
						}
					}
				}
			}
			
			if ( index == -1 ) {
				// look for the LAST named (and not in use) bitmap...
				for ( int i = b.length-1; i >= 0; i-- ) {
					if ( !inUse[i]
					            && sizeAtLeast( b[i], minWidth, minHeight )
					            && (!checkConfig || config == b[i].getConfig()) ) {
						long p = pixelsLeftOver(b[i], minWidth, minHeight) ;
						if ( p < extraPixels ) {
							index = i ;
							extraPixels = p ;
						}
					}
				}
			}
		}
		
		if ( index != -1 ) {
			names[index] = name ;
			inUse[index] = true ;
			return b[index] ;
		}
		
		return null ;
	}
	
	
	private static int getNumUnusedBitmaps( boolean [] inUse ) {
		if ( inUse == null )
			return 0 ;
		int num = 0 ;
		for ( int i = 0; i < inUse.length; i++ )
			if ( !inUse[i] )
				num++ ;
		return num ;
	}
	
	private static boolean hasBitmap( Bitmap b, Bitmap [] bitmaps ) {
		if ( bitmaps == null )
			return false ;
		for ( int i = 0; i < bitmaps.length; i++ )
			if ( b == bitmaps[i] )
				return true ;
		return false ;
	}
	
	private static boolean hasBitmap( String name, String [] names ) {
		if ( names == null )
			return false ;
		for ( int i = 0; i < names.length; i++ )
			if ( name.equals(names[i]) )
				return true ;
		return false ;
	}
	
	private static boolean hasBitmapInUse( String name, String [] names, boolean [] inUse ) {
		if ( names == null )
			return false ;
		for ( int i = 0; i < names.length; i++ )
			if ( name.equals(names[i]) )
				return inUse[i] ;
		return false ;
	}
	
	private static boolean sizeAtLeast( Bitmap b, int minWidth, int minHeight ) {
		return ( minWidth == -1 || b.getWidth() >= minWidth )
				&& ( minHeight == -1 || b.getHeight() >= minHeight ) ;
	}
	
	private static long pixelsLeftOver( Bitmap b, int minWidth, int minHeight ) {
		if ( minWidth == -1 || minHeight == -1 )
			return b.getWidth() * b.getHeight() ;
		return (b.getWidth() * b.getHeight()) - (minWidth * minHeight) ;
	}
	
}
