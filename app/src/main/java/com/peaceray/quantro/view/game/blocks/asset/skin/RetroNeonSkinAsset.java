package com.peaceray.quantro.view.game.blocks.asset.skin;

import android.content.Context;

import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.border.BorderComponentGradient;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.fill.FillComponentColor;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.neighborhood.NeighborhoodComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.neighborhood.NeighborhoodComponentShadows;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.BlockRegions;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;

class RetroNeonSkinAsset extends SkinAsset {
	
	private RetroNeonSkinAsset() {
		super() ;
	}
	
	@Override
	protected Regions newRegionsInstance(BlockSize blockSizes) {
		return Regions.newInstance(blockSizes.getBlockWidth(), blockSizes.getBlockHeight()) ;
	}
	
	static class Builder extends SkinAsset.Builder {
		
		Builder( Context context, Skin skin ) {
			super(context, skin) ;
		}

		@Override
		protected SkinAsset bb_newEmptyBuildable() {
			// TODO: Check all values.
			return new RetroNeonSkinAsset() ;
		}

		@Override
		protected void bb_build(SkinAsset obj) {
			try {
				RetroNeonSkinAsset skinAsset = (RetroNeonSkinAsset) obj ;
				
				// set skin
				skinAsset.setSkin(mSkin) ;
				
				// FILL_REGIONS.
				skinAsset.setBlockSizes(mBlockSizes) ;
				BlockRegions blockRegions = skinAsset.mRegions.get(mBlockSizes.get(0)).getBlockRegions() ;
				
				// fill component: FillComponentColor.
				FillComponentColor fcc = new FillComponentColor( mColorScheme ) ;
				// we use standard fills for everything.  Set alphas.
				fcc.setAlphas(0, 0, 76) ;	// empty blockfield and piece; transparent ghosts.
				// no way to confuse fills with borders; don't tint.
				fcc.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
				skinAsset.setFillComponents(fcc) ;
				
				// NEIGHBORHOOD COMPONENT: Inner / Drop shadows.
				if ( mDrawDetail >= Consts.DRAW_DETAIL_MID ) {
					// bitmap-based inner shadows
					NeighborhoodComponent nc = NeighborhoodComponentShadows.newRetroNeonInnerShadows(blockRegions, mColorScheme) ;
					nc.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
					skinAsset.addNeighborhoodComponents( nc ) ;
					
					// bitmap-based drop shadows.
					nc = NeighborhoodComponentShadows.newRetroNeonOuterShadows(blockRegions, mColorScheme) ;
					nc.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
					skinAsset.addNeighborhoodComponents( nc ) ;
				} else {
					// region-based drop shadows; no inner shadows
					NeighborhoodComponent nc = NeighborhoodComponentShadows.newRetroNeonOuterShadows(blockRegions, mColorScheme) ;
					nc.prepare(mContext, null, null, skinAsset.mRegions, Consts.IMAGES_SIZE_NONE, Consts.IMAGES_SIZE_NONE) ;
					skinAsset.addNeighborhoodComponents( nc ) ;
				}
				
				// BORDER COMPONENT: Gradients.
				boolean useBitmaps = ( mDrawDetail >= Consts.DRAW_DETAIL_MID && mDrawAnimations >= Consts.DRAW_ANIMATIONS_FULL ) ;
				boolean includeShines = mDrawDetail >= Consts.DRAW_DETAIL_MID ;
				BorderComponentGradient bc = BorderComponentGradient.newRetroStandardBorders(blockRegions, mColorScheme, useBitmaps, includeShines) ;
				bc.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
				skinAsset.setBorderComponents( bc ) ;
				
			} catch ( Exception e ) {
				// WHOOPS!  We are allowed to throw RuntimeExceptions, but not any
				// other type.  If this is NOT a RuntimeException, package it up
				// in one and throw that.
				e.printStackTrace() ;
				if ( e instanceof RuntimeException ) {
					throw (RuntimeException)e ;
				} else {
					throw new RuntimeException("A non-Runtime Exception was thrown.") ;
				}
			}
		}
		
	}
}
