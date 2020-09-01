package com.peaceray.quantro.view.game.blocks.asset.skin.regions;

import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;

import android.graphics.Rect;


/**
 * A Meta-Object containing each type of Region.
 * 
 * @author Jake
 *
 */
public class Regions {

	private BlockRegions mBlockRegions ;
	private FillRegions mFillRegions ;
	
	
	public static Regions newInstance( int blockWidth, int blockHeight ) {
		BlockRegions br = new BlockRegions() ;
		FillRegions fr = new FillRegions(blockWidth, blockHeight) ;
		
		return new Regions( br, fr ) ;
	}
	
	public Regions( BlockRegions blockRegions, FillRegions fillRegions ) {
		mBlockRegions = blockRegions ;
		mFillRegions = fillRegions ;
	}
	
	public BlockRegions getBlockRegions() {
		return mBlockRegions ;
	}
	
	public FillRegions getFillRegions() {
		return mFillRegions ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// PASS-THROUGH ACCESSORS
	//
	// Accessors that typically involve a single, direct call to one of the
	// underlying components.  You could just as easily retrieve the object
	// and call the appropriate method for it.
	//
	
	public final void setBlockRegion( int qPane, int qOrientation, BlockRegion blockRegion ) {
		mBlockRegions.setBlockRegion(qPane, qOrientation, blockRegion) ;
	}
	
	public final BlockRegion getBlockRegion( int qPane, int qOrientation ) {
		return mBlockRegions.getBlockRegion(qPane, qOrientation) ;
	}
	
	/**
	 * Returns a Rect fit to the specified block region, for the block
	 * with its top-left corner at X,Y.  DON'T MUTATE THE RETURNED OBJECT.
	 * @param region
	 * @param x
	 * @param y
	 * @return
	 */
	public Rect fitFillRect( BlockRegion region, int x, int y ) {
		return mFillRegions.fitRect(region, x, y) ;
	}
	
	
	/**
	 * Returns a Rect fit to the specified block region, for the block
	 * with its top-left corner at X,Y.  DON'T MUTATE THE RETURNED OBJECT.
	 * @param region
	 * @param x
	 * @param y
	 * @return
	 */
	public Rect fitFillRectInsideBorder( BlockRegion region, int x, int y ) {
		return mFillRegions.fitRectInsideBorder(region, x, y) ;
	}
	
	//
	// PASS-THROUGH ACCESSORS
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// COMPLEX ACCESSORS
	//
	// Accessors that involve more than one component in calculating the
	// result.  Motivating example: BlockRegions tells us the BlockRegion for
	// a given qPane/qOrientation, and FillRegions will give us a Rect for
	// that BlockRegion.
	//
	
	/**
	 * Returns a Rect fit to the specified block region, for the block
	 * with its top-left corner at X,Y.  DON'T MUTATE THE RETURNED OBJECT.
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @param x
	 * @param y
	 * @return
	 */
	public Rect fitFillRect( int qPane, int qOrientation, int x, int y ) {
		return mFillRegions.fitRect(
				mBlockRegions.getBlockRegion(qPane, qOrientation),
				x, y) ;
	}
	
	
	/**
	 * Returns a Rect fit to the specified block region, for the block
	 * with its top-left corner at X,Y.  DON'T MUTATE THE RETURNED OBJECT.
	 * 
	 * @param region
	 * @param x
	 * @param y
	 * @return
	 */
	public Rect fitFillRectInsideBorder( int qPane, int qOrientation, int x, int y ) {
		return mFillRegions.fitRectInsideBorder(
				mBlockRegions.getBlockRegion(qPane, qOrientation),
				x, y) ;
	}
	
	//
	// COMPLEX ACCESSORS 
	//
	////////////////////////////////////////////////////////////////////////////
	
}
