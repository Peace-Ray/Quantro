package com.peaceray.quantro.view.game.blocks.asset.skin;

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.Context;

import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.utils.backgroundbuildable.BackgroundBuildable; 
import com.peaceray.quantro.utils.backgroundbuildable.BackgroundBuilder;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.border.BorderComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.fill.FillComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.neighborhood.NeighborhoodComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;


/**
 * A SkinAsset represents the method and tools needed to draw blocks to
 * the screen.
 * 
 * At time of creation, we handle all this in 'DrawSettings.'  Unlike the
 * BackgroundAsset, a SkinAsset is not intended as a fully-versatile class.
 * Instead, this is an abstract class that specific Skins subclasses will
 * override as-needed.
 * 
 * For example, we currently allow the following settings for colors:
 * 
 * RETRO:				QUANTRO:
 * 	Standard			 Standard
 * 	NE0N				 Nile
 * 	Protonopia			 Protonopia
 * 	Deuteranopia		 Deuteranopia
 * 	Tritanopia			 Tritanopia
 * 
 * Additionally, Quantro skins allow "colorblind mode" which adds visual
 * detail to help distinguish foreground and background.  We want most,
 * if not all, Skins to allow customized color schemes, but some are naturally
 * drawn differently than others and that is the function of different
 * SkinAsset configurations.  Here is an initial plan for actual Skin
 * implementations:
 * 
 * RetroStandard
 * 		Standard
 * 		Protonopia
 * 		Deuteranopia
 * 		Tritanopia
 * 
 * RetroNeon
 * 		NE0N
 * 
 * QuantroStandard
 * 		Standard
 * 		Nile
 * 		Protonopia
 * 		Deuteranopia
 * 		Tritanopia
 * 
 * QuantroColorblind
 * 		Standard
 * 		Nile
 * 		Protonopia
 * 		Deuteranopia
 * 		Tritanopia
 * 
 * A few things to note.  First, ColorBlind help is now represented in
 * a dedicated Skin.  Among other things, this allows us add new color
 * schemes if we want, but more importantly we can create new Quantro
 * designs without worrying about colorblind support -- there's a
 * dedicated skin for it, not a button.
 * 
 * Second, we have official split standard Retro and official Quantro.
 * We no longer need to support both styles in a single unit.  However,
 * implementation could very easily share components between classes,
 * and these two especially are extremely similar.
 * 
 * 
 * USE OF A SKIN ASSET:
 * 
 * At present, we plan to draw skins in a very regular and formuliac way,
 * but potentially this control could be moved to the skins themselves, in
 * the same way "glow behavior" allows a customized draw order.
 * 
 * This is a MAJOR refactor; potentially the largest refactor we have done
 * or will ever do in Quantro.  BlockDrawer is an absolutely massive, monolithic
 * class with almost 30,000 lines of code, and it is almost entirely dedicated to drawing
 * skins.
 * 
 * Our plan: gradual, in-place refactor in steps, rather than the massive one-time
 * swap we did with BackgroundAsset.  The first step is to get empty SkinAsset
 * implementations that do nothing, are used for nothing, but smoothly "fit in"
 * where we would use them.  Gradually they can take over BlockDrawer responsibilities,
 * with versatile components being shared between similar but non-identical Skins
 * (such as a "gaussian shadow sheet" component).
 * 
 * Once Skins are in-place, I hope the gradual expansion of their functionality
 * will seem less daunting than it does right now (serious design block).
 * 
 * @author Jake
 *
 */
public abstract class SkinAsset extends BackgroundBuildable.Implementation {
	
	protected Skin mSkin ;

	protected FillComponent [] mFillComponents ;
	protected NeighborhoodComponent [] mNeighborhoodComponents ;
	protected BorderComponent [] mBorderComponents ;
	
	protected Hashtable<BlockSize, Regions> mRegions = new Hashtable<BlockSize, Regions>() ;
	
	public Skin getSkin() {
		return mSkin ;
	}
	
	
	
	
	protected SkinAsset() {
		mSkin = null ;
	}
	
	/**
	 * Meant as a builder helper.
	 * @param skin
	 */
	protected void setSkin( Skin skin ) {
		mSkin = skin ;
	}
	
	protected void setBlockSizes( ArrayList<BlockSize> blockSizes ) {
		mRegions.clear() ;
		for ( int i = 0; i < blockSizes.size(); i++ ) {
			BlockSize bs = blockSizes.get(i) ;
			mRegions.put(bs, newRegionsInstance(bs)) ;
		}
	}
	
	protected abstract Regions newRegionsInstance( BlockSize blockSizes ) ;
	
	protected void clearFillComponents() {
		mFillComponents = null ;
	}
	
	protected void addFillComponents( FillComponent ...fillComponents ) {
		if ( mFillComponents == null )
			mFillComponents = fillComponents ;
		else {
			FillComponent [] fc = new FillComponent[mFillComponents.length + fillComponents.length] ;
			for ( int i = 0; i < mFillComponents.length; i++ )
				fc[i] = mFillComponents[i] ;
			for ( int i = 0; i < fillComponents.length; i++ )
				fc[i + mFillComponents.length] = fillComponents[i] ;
			
			mFillComponents = fc ;
		}
	}
	
	protected void setFillComponents( FillComponent ...fillComponents ) {
		mFillComponents = fillComponents ;
	}
	
	
	protected void clearNeighborhoodComponents() {
		mNeighborhoodComponents = null ;
	}
	
	protected void addNeighborhoodComponents( NeighborhoodComponent ...neighborhoodComponents ) {
		if ( mNeighborhoodComponents == null )
			mNeighborhoodComponents = neighborhoodComponents ;
		else {
			NeighborhoodComponent [] nc = new NeighborhoodComponent[mNeighborhoodComponents.length + neighborhoodComponents.length] ;
			for ( int i = 0; i < mNeighborhoodComponents.length; i++ )
				nc[i] = mNeighborhoodComponents[i] ;
			for ( int i = 0; i < neighborhoodComponents.length; i++ )
				nc[i + mNeighborhoodComponents.length] = neighborhoodComponents[i] ;
			
			mNeighborhoodComponents = nc ;
		}
	}
	
	protected void setNeighborhoodComponents( NeighborhoodComponent ...neighborhoodComponents ) {
		mNeighborhoodComponents = neighborhoodComponents ;
	}
	
	
	protected void clearBorderComponents() {
		mBorderComponents = null ;
	}
	
	protected void addBorderComponents( BorderComponent ...borderComponents ) {
		if ( mBorderComponents == null )
			mBorderComponents = borderComponents ;
		else {
			BorderComponent [] bc = new BorderComponent[mBorderComponents.length + borderComponents.length] ;
			for ( int i = 0; i < mBorderComponents.length; i++ )
				bc[i] = mBorderComponents[i] ;
			for ( int i = 0; i < borderComponents.length; i++ )
				bc[i + mBorderComponents.length] = borderComponents[i] ;
			
			mBorderComponents = bc ;
		}
	}
	
	protected void setBorderComponents( BorderComponent ...borderComponents ) {
		mBorderComponents = borderComponents ;
	}
	

	public abstract static class Builder extends BackgroundBuilder<SkinAsset> {
		
		protected Context mContext ;
		
		protected Skin mSkin ;
		protected ColorScheme mColorScheme ;
		
		protected ArrayList<BlockSize> mBlockSizes = new ArrayList<BlockSize>() ;
		protected int mDrawDetail ;
		protected int mDrawAnimations ;
		
		protected int mImageSize, mLoadSize ;
		
		protected Builder( Context context, Skin skin ) {
			mContext = context ;
			mSkin = skin ;
		}
		
		public Builder setColorScheme( ColorScheme colorScheme ) {
			mColorScheme = colorScheme ;
			return this ;
		}
		
		public Builder addBlockSize( BlockSize blockSize ) {
			mBlockSizes.add(blockSize) ;
			
			return this ;
		}
		
		public Builder clearBlockSizes() {
			mBlockSizes.clear() ;
			
			return this ;
		}
		
		public Builder setDrawDetail( int drawDetail ) {
			mDrawDetail = drawDetail ;
			
			return this ;
		}
		
		public Builder setDrawAnimations( int drawAnimations ) {
			mDrawAnimations = drawAnimations ;
			
			return this ;
		}
		
		public Builder setImageSize( int imageSize, int loadSize ) {
			mImageSize = imageSize ;
			mLoadSize = loadSize ;
			
			return this ;
		}
		
	}
	
}
