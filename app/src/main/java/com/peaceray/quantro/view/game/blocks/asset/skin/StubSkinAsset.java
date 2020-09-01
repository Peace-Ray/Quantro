package com.peaceray.quantro.view.game.blocks.asset.skin;

import android.content.Context;

import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;


/**
 * A SkinAsset that does nothing whatsoever.
 * 
 * Meant as a stand-in to allow some basic BlockDrawer skin support w/out requiring
 * an actual Skin.  This skin should (hopefully) allow parallel development of
 * Skins in the main future branch w/out affecting any existing code.
 * 
 * This class 
 * 
 * @author Jake
 *
 */
public class StubSkinAsset extends SkinAsset {

	@Override
	protected Regions newRegionsInstance(BlockSize blockSizes) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
static class Builder extends SkinAsset.Builder {
		
		Builder( Context context, Skin skin ) {
			super(context, skin) ;
		}

		@Override
		protected SkinAsset bb_newEmptyBuildable() {
			return new StubSkinAsset() ;
		}

		@Override
		protected void bb_build(SkinAsset obj) {
			try {
				StubSkinAsset skinAsset = (StubSkinAsset) obj ;
				
				// set skin
				skinAsset.setSkin(mSkin) ;
				
				// that's all.  This is a stub, remember?
				
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
