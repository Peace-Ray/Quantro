package com.peaceray.quantro.view.game.blocks.asset.skin.regions;

import java.util.Enumeration;
import java.util.Hashtable;

import android.graphics.Rect;

import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;

public class FillRegions {

	// DEFAULTS:
	// Settings for BlockRegion, size information, fill-regions, border sizes,
	// etc.  May want to create your own for subclasses.
	private static final Hashtable<BlockRegion, Float[]> BLOCK_REGION_INSET = new Hashtable<BlockRegion, Float[]>() ;
	private static final Hashtable<BlockRegion, Float[]> BLOCK_REGION_BORDER_WIDTH = new Hashtable<BlockRegion, Float[]>() ;
	
	static {
		// set default insets for the blocks.
		BLOCK_REGION_INSET.put(
				BlockRegion.FULL,
				new Float[]{ Float.valueOf(0), Float.valueOf(0), Float.valueOf(0), Float.valueOf(0) }  ) ;
		BLOCK_REGION_INSET.put(
				BlockRegion.INSET_MINOR,
				new Float[]{ Float.valueOf(0.05f), Float.valueOf(0.05f), Float.valueOf(0.05f), Float.valueOf(0.05f) }  ) ;
		BLOCK_REGION_INSET.put(
				BlockRegion.INSET_MAJOR,
				new Float[]{ Float.valueOf(0.15f), Float.valueOf(0.15f), Float.valueOf(0.15f), Float.valueOf(0.15f) }  ) ;
		BLOCK_REGION_INSET.put(
				BlockRegion.INSET_MAJOR_NONOVERLAP,
				BLOCK_REGION_INSET.get(BlockRegion.INSET_MAJOR)  ) ;
		
		// set default border widths for blocks.
		BLOCK_REGION_BORDER_WIDTH.put(
				BlockRegion.FULL,
				new Float[]{ Float.valueOf(0.1f), Float.valueOf(0.1f), Float.valueOf(0.1f), Float.valueOf(0.1f) }  ) ;
		BLOCK_REGION_BORDER_WIDTH.put(
				BlockRegion.INSET_MINOR,
				BLOCK_REGION_BORDER_WIDTH.get(BlockRegion.FULL)  ) ;
		BLOCK_REGION_BORDER_WIDTH.put(
				BlockRegion.INSET_MAJOR,
				BLOCK_REGION_BORDER_WIDTH.get(BlockRegion.FULL)  ) ;
		BLOCK_REGION_BORDER_WIDTH.put(
				BlockRegion.INSET_MAJOR_NONOVERLAP,
				BLOCK_REGION_BORDER_WIDTH.get(BlockRegion.FULL)  ) ;
	}
	
	protected static final int INDEX_LEFT = 0 ;
	protected static final int INDEX_TOP = 1 ;
	protected static final int INDEX_RIGHT = 2 ;
	protected static final int INDEX_BOTTOM = 3 ;
	protected static final int NUM_DIRECTION_INDICES = 4 ;
	
	protected static final int INDEX_X = 0 ;
	protected static final int INDEX_Y = 1 ;
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// NOTE: OVERRIDE THESE TO CREATE A SUBCLASS OR DIFFERENT BEHAVIOR.

	protected Hashtable<BlockRegion, Float[]> getDefaultBlockRegionInsets() {
		if ( this.getClass() != FillRegions.class )
			throw new UnsupportedOperationException("This subclass did not override getDefaultBlockRegionInsets") ;
		return deepCopy( BLOCK_REGION_INSET ) ;
	}
	
	protected Hashtable<BlockRegion, Float[]> getDefaultBlockRegionBorderWidths() {
		if ( this.getClass() != FillRegions.class )
			throw new UnsupportedOperationException("This subclass did not override getDefaultBlockRegionBorderWidths") ;
		return deepCopy( BLOCK_REGION_BORDER_WIDTH ) ;
	}
	
	protected static final <T,U> Hashtable<T,U []> deepCopy( Hashtable<T,U []> ht ) {
		Hashtable<T,U[]> res = new Hashtable<T,U[]>() ;
		Enumeration<T> enumeration = ht.keys() ;
		for ( ; enumeration.hasMoreElements() ; ) {
			T t = enumeration.nextElement() ;
			res.put( t, ht.get(t).clone() ) ;
		}
		return res ;
	}
	
	
	/**
	 * Returns a Rect object for the provided blockRegion.  It is
	 * positioned to put (0,0) at the top-left of a block (the top-left
	 * of the Rect will possibly be != 0, indicating that the top-left of
	 * the BlockRegion is not aligned to the grid exactly).
	 * 
	 * The Rect returned indicates the boundaries of the block as positioned on
	 * the grid.  For example, in the standard Skin, Flash blocks are inset.
	 * 
	 * Returns a new instance with each call.
	 * 
	 * @param blockRegion
	 * @return
	 */
	protected Rect getBounds( BlockRegion blockRegion, Hashtable<BlockRegion, Float[]> blockRegionInsets,
			int blockWidth, int blockHeight ) {
		
		
		if ( blockRegion == BlockRegion.NONE )
			return null ;
		
		Float [] insets = blockRegionInsets.get(blockRegion) ;
		int [] insetPixels = new int[NUM_DIRECTION_INDICES] ;
		insetPixels[INDEX_LEFT] = (int)Math.round( blockWidth * insets[INDEX_LEFT] ) ;
		insetPixels[INDEX_RIGHT] = (int)Math.round( blockWidth * insets[INDEX_RIGHT] ) ;
		insetPixels[INDEX_TOP] = (int)Math.round( blockHeight * insets[INDEX_TOP] ) ;
		insetPixels[INDEX_BOTTOM] = (int)Math.round( blockHeight * insets[INDEX_BOTTOM] ) ;
		
		return new Rect(
				insetPixels[INDEX_LEFT], insetPixels[INDEX_TOP],
				blockWidth - insetPixels[INDEX_RIGHT], blockHeight - insetPixels[INDEX_BOTTOM] ) ;
	}
	
	/**
	 * Returns a Rect object for the provided blockRegion.  It is
	 * positioned to put (0,0) at the top-left of a block (the top-left
	 * of the Rect will possibly be != 0, indicating that the top-left of
	 * the BlockRegion is not aligned to the grid exactly).
	 * 
	 * The Rect returned indicates the boundaries of the block as positioned on
	 * the grid.  For example, in the standard Skin, Flash blocks are inset.
	 * 
	 * Returns a new instance with each call.
	 * 
	 * @param blockRegion
	 * @return
	 */
	protected Rect getInsideBorderBounds( BlockRegion blockRegion,
			Hashtable<BlockRegion, Float[]> blockRegionInsets,
			Hashtable<BlockRegion, Float[]> blockRegionBorderWidths,
			int blockWidth, int blockHeight ) {
		
		
		if ( blockRegion == BlockRegion.NONE )
			return null ;
		
		Float [] insets = blockRegionInsets.get(blockRegion) ;
		Float [] borders = blockRegionBorderWidths.get(blockRegion) ;
		int [] insetPixels = new int[NUM_DIRECTION_INDICES] ;
		insetPixels[INDEX_LEFT] = (int)Math.round( blockWidth * insets[INDEX_LEFT] ) 
				+ (int)Math.round( blockWidth * borders[INDEX_LEFT] );
		insetPixels[INDEX_RIGHT] = (int)Math.round( blockWidth * insets[INDEX_RIGHT] ) 
				+ (int)Math.round( blockWidth * borders[INDEX_RIGHT] );
		insetPixels[INDEX_TOP] = (int)Math.round( blockHeight * insets[INDEX_TOP] ) 
				+ (int)Math.round( blockHeight * borders[INDEX_TOP] );
		insetPixels[INDEX_BOTTOM] = (int)Math.round( blockHeight * insets[INDEX_BOTTOM] ) 
				+ (int)Math.round( blockHeight * borders[INDEX_BOTTOM] );
		
		return new Rect(
				insetPixels[INDEX_LEFT], insetPixels[INDEX_TOP],
				blockWidth - insetPixels[INDEX_RIGHT], blockHeight - insetPixels[INDEX_BOTTOM] ) ;
	}
	
	private Hashtable<BlockRegion, Float[]> mBlockRegionInset ;
	private Hashtable<BlockRegion, Float[]> mBlockRegionBorderWidth ;
	
	private int [] mBlockSize_XY ;
	
	private Rect [] mBoundsRect_byBlockRegion ;
	private int [][] mBoundsRectTopLeft_byBlockRegion_XY ;
	
	private Rect [] mInsideBorderBoundsRect_byBlockRegion ;
	private int [][] mInsideBorderBoundsRectTopLeft_byBlockRegion_XY ;
	
	
	FillRegions( int blockWidth, int blockHeight ) {
		mBlockRegionInset = getDefaultBlockRegionInsets() ;
		mBlockRegionBorderWidth = getDefaultBlockRegionBorderWidths() ;
		
		mBlockSize_XY = new int[2] ;
		mBlockSize_XY[INDEX_X] = blockWidth ;
		mBlockSize_XY[INDEX_Y] = blockHeight ;
		

		
		BlockRegion [] blockRegions = BlockRegion.values() ;
		mBoundsRect_byBlockRegion = new Rect[blockRegions.length] ;
		mBoundsRectTopLeft_byBlockRegion_XY = new int[blockRegions.length][2] ;
		
		mInsideBorderBoundsRect_byBlockRegion = new Rect[blockRegions.length] ;
		mInsideBorderBoundsRectTopLeft_byBlockRegion_XY = new int[blockRegions.length][2] ;
		
		// set rects!
		for ( int i = 0; i < blockRegions.length; i++ ) {
			BlockRegion blockRegion = blockRegions[i] ;
			int region = blockRegion.ordinal() ;
			
			Rect rect = getBounds(blockRegion, mBlockRegionInset, blockWidth, blockHeight) ;
			if ( rect != null ) {
				mBoundsRect_byBlockRegion[region] = rect ;
				mBoundsRectTopLeft_byBlockRegion_XY[region][INDEX_X] = rect.left ;
				mBoundsRectTopLeft_byBlockRegion_XY[region][INDEX_Y] = rect.top ;
			}
			
			rect = getInsideBorderBounds(blockRegion, mBlockRegionInset, mBlockRegionBorderWidth, blockWidth, blockHeight) ;
			if ( rect != null ) {
				mInsideBorderBoundsRect_byBlockRegion[region] = rect ;
				mInsideBorderBoundsRectTopLeft_byBlockRegion_XY[region][INDEX_X] = rect.left ;
				mInsideBorderBoundsRectTopLeft_byBlockRegion_XY[region][INDEX_Y] = rect.top ;
			}
		}
	}
	
	
	public final int getBlockWidth() {
		return mBlockSize_XY[INDEX_X] ;
	}
	
	public final int getBlockHeight() {
		return mBlockSize_XY[INDEX_Y] ;
	}
	
	
	
	
	/**
	 * Returns a Rect fit to the specified block region, for the block
	 * with its top-left corner at X,Y.  DON'T MUTATE THE RETURNED OBJECT.
	 * @param region
	 * @param x
	 * @param y
	 * @return
	 */
	public Rect fitRect( BlockRegion region, int x, int y ) {
		int ord = region.ordinal() ;
		Rect rect = mBoundsRect_byBlockRegion[ord] ;
		if ( rect != null ) {
			rect.offsetTo(
					x + this.mBoundsRectTopLeft_byBlockRegion_XY[ord][INDEX_X],
					y + this.mBoundsRectTopLeft_byBlockRegion_XY[ord][INDEX_Y]) ;
		}
		
		return rect ;
	}
	
	
	/**
	 * Returns a Rect fit to the specified block region, for the block
	 * with its top-left corner at X,Y.  DON'T MUTATE THE RETURNED OBJECT.
	 * @param region
	 * @param x
	 * @param y
	 * @return
	 */
	public Rect fitRectInsideBorder( BlockRegion region, int x, int y ) {
		int ord = region.ordinal() ;
		Rect rect = mInsideBorderBoundsRect_byBlockRegion[ord] ;
		if ( rect != null ) {
			rect.offsetTo(
					x + this.mInsideBorderBoundsRectTopLeft_byBlockRegion_XY[ord][INDEX_X],
					y + this.mInsideBorderBoundsRectTopLeft_byBlockRegion_XY[ord][INDEX_Y]) ;
		}
		
		return rect ;
	}
}
