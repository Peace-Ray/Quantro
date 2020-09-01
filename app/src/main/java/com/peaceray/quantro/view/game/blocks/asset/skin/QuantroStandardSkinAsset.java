package com.peaceray.quantro.view.game.blocks.asset.skin;

import java.util.Enumeration;

import android.content.Context;

import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.drawable.LineShadingDrawable;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.border.BorderComponentGradient;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.fill.FillComponentClippedDrawable;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.fill.FillComponentColor;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.neighborhood.NeighborhoodComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.neighborhood.NeighborhoodComponentShadows;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.BlockRegions;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;

class QuantroStandardSkinAsset extends SkinAsset {
	
	private QuantroStandardSkinAsset() {
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
			return new QuantroStandardSkinAsset() ;
		}

		@Override
		protected void bb_build(SkinAsset obj) {
			try {
				QuantroStandardSkinAsset skinAsset = (QuantroStandardSkinAsset) obj ;
				
				// set skin
				skinAsset.setSkin(mSkin) ;
				
				// FILL_REGIONS.
				skinAsset.setBlockSizes(mBlockSizes) ;
				BlockRegions blockRegions = skinAsset.mRegions.get(mBlockSizes.get(0)).getBlockRegions() ;
				
				// fill component: FillComponentColor.
				FillComponentColor fcc = new FillComponentColor( mColorScheme ) ;
				// we use standard fills for everything.  Set alphas.
				fcc.setAlphas(102, 102, 76) ;	// transparent locked/piece; more transparent ghosts.
				
				// no fill color for F0 / F1 if our draw detail is >= MID.
				if ( mDrawDetail >= Consts.DRAW_DETAIL_MID ) {
					fcc.setFill(QOrientations.F0, BlockRegion.INSET_MINOR, FillComponentColor.FillType.NONE) ;
					fcc.setFill(QOrientations.F1, BlockRegion.INSET_MINOR, FillComponentColor.FillType.NONE) ;
				}
				
				// friendlies!  SO_FROM_SL connects with SL; S1_FROM_SL connects with SL.
				if ( mSkin.getTemplate() == Skin.Template.STANDARD ) {
					fcc.setFriendly(QOrientations.S0_FROM_SL, QOrientations.SL, true) ;
					fcc.setFriendly(QOrientations.S1_FROM_SL, QOrientations.SL, true) ;
				} else if ( mSkin.getTemplate() == Skin.Template.COLORBLIND ) {
					// actually... if we're colorblind, SL is not friendly.
					// Undo friendly connections.
					fcc.setAlphaFade(QOrientations.S0, 0.5f, 0.5f, 0.5f) ;
					fcc.setAlphaFade(QOrientations.S1, 0.5f, 0.5f, 0.5f) ;
					fcc.setAlphaFade(QOrientations.UL, 0.5f, 0.5f, 0.5f) ;
					fcc.setAlphaFade(QOrientations.U0, 0.5f, 0.5f, 0.5f) ;
					fcc.setAlphaFade(QOrientations.U1, 0.5f, 0.5f, 0.5f) ;
					fcc.setAlphaFade(QOrientations.S0_FROM_SL, 0.5f, 0.5f, 0.5f) ;
					fcc.setAlphaFade(QOrientations.S1_FROM_SL, 0.5f, 0.5f, 0.5f) ;
					
					fcc.setFill(QOrientations.S0_FROM_SL, BlockRegion.FULL, FillComponentColor.FillType.SOLID ) ;
					fcc.setFill(QOrientations.S1_FROM_SL, BlockRegion.FULL, FillComponentColor.FillType.SOLID ) ;
				}
				
				fcc.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
				skinAsset.addFillComponents(fcc) ;
				
				// if COLORBLIND, we need to downscale the alphas of a few fills.
				// Also, I know we SAID certain qOrientations are friendly... but we lied.
				// Undo this.
				if ( mSkin.getTemplate() == Skin.Template.COLORBLIND ) {
					// Now create and populate hash-shaders for the FillComponentClippedDrawable.
					FillComponentClippedDrawable fccd = new FillComponentClippedDrawable() ;
					
					Enumeration<BlockSize> enumeration = skinAsset.mRegions.keys() ;
					
					for ( ; enumeration.hasMoreElements() ; ) {
						
						BlockSize bs = enumeration.nextElement() ;
						int width = bs.getBlockWidth() ;
						
						int [] color = new int[]{
								mColorScheme.getFillColor(QOrientations.S0, 0),
								mColorScheme.getFillColor(QOrientations.S1, 1) } ;
						int [] lineWidth = new int[] {
								(int)Math.round( width * 0.15f ),
								(int)Math.round( width * 0.3f ) } ;
						int [] lineSeparation = new int[] {
								(int)Math.round( width * 0.50f ),
								(int)Math.round( width * 0.50f ) } ;
						float [][] lineVector = new float[][] {
								new float[]{ 1, -3 },
								new float[]{ 3, 1 } } ;
						
						int [] qos = new int[] {
								QOrientations.S0,
								QOrientations.S1,
								QOrientations.U0,
								QOrientations.U1,
								QOrientations.UL,
								QOrientations.S0_FROM_SL,
								QOrientations.S1_FROM_SL
						} ;
						
						for ( int qp = 0; qp < 2; qp++ ) {
							LineShadingDrawable d = new LineShadingDrawable();
							d.setColor(color[qp]);
							d.setLineWidth(lineWidth[qp]);
							d.setLineSeparation(lineSeparation[qp]); 
							d.setLineVector(lineVector[qp]);
							
							fccd.addDrawable(bs, qp, d, qos) ;
						}
						
					}
					
					fccd.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
					skinAsset.addFillComponents(fccd) ;
				}
				
				
				// NEIGHBORHOOD COMPONENT: Inner / Drop shadows.
				if ( mDrawDetail >= Consts.DRAW_DETAIL_MID ) {
					// bitmap-based inner shadows
					NeighborhoodComponent nc = NeighborhoodComponentShadows.newQuantroStandardInnerShadows(blockRegions, mColorScheme) ;
					nc.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
					skinAsset.addNeighborhoodComponents( nc ) ;
					
					// bitmap-based drop shadows.
					nc = NeighborhoodComponentShadows.newQuantroStandardOuterShadows(blockRegions, mColorScheme) ;
					nc.prepare(mContext, null, null, skinAsset.mRegions, mImageSize, mLoadSize) ;
					skinAsset.addNeighborhoodComponents( nc ) ;
				} else {
					// region-based drop shadows; no inner shadows
					NeighborhoodComponent nc = NeighborhoodComponentShadows.newQuantroStandardOuterShadows(blockRegions, mColorScheme) ;
					// skip 'loadAndRender' and it will default to region-based.
					skinAsset.addNeighborhoodComponents( nc ) ;
				}
				
				// BORDER COMPONENT: Gradients.
				boolean useBitmaps = ( mDrawDetail >= Consts.DRAW_DETAIL_MID && mDrawAnimations >= Consts.DRAW_ANIMATIONS_FULL ) ;
				boolean includeShines = mDrawDetail >= Consts.DRAW_DETAIL_MID ;
				BorderComponentGradient bc = BorderComponentGradient.newQuantroStandardBorders(blockRegions, mColorScheme, useBitmaps, includeShines) ;
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
