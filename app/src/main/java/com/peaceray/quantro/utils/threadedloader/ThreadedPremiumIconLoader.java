package com.peaceray.quantro.utils.threadedloader;

import java.io.File;

import com.peaceray.quantro.premium.PremiumSKU;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

public class ThreadedPremiumIconLoader extends ThreadedBitmapLoader {
	
	public class Params extends ThreadedBitmapLoader.Params {
		String mPremiumSKU ;
		
		public Params setSKU( String itemSKU ) {
			mPremiumSKU = itemSKU ;
			return this ;
		}
		
		public String getSKU() {
			return mPremiumSKU ;
		}
	}
	
	public class Result extends ThreadedLoader.Result {
		Bitmap mBitmap ;
		
		protected Result setBitmap( Bitmap bitmap ) {
			mBitmap = bitmap ;
			return this ;
		}
		
		public Bitmap getBitmap() {
			return mBitmap ;
		}
	}
	
	AssetManager mAssetManager ;
	
	public ThreadedPremiumIconLoader( AssetManager am ) {
		mAssetManager = am ;
	}
	

	@Override
	protected Params newParams() {
		return new Params() ;
	}

	@Override
	protected ThreadedLoader.Result load(ThreadedLoader.Params p) {
		
		if ( !(p instanceof Params) ) {
			// wrong Params...
			return new Result().setSuccess(false) ;
		}
		
		Params params = (Params)p ;
		String itemSKU = params.getSKU() ;
		
		// make sure this is an actual sku
		boolean has = false ;
		for ( int i = 0; i < PremiumSKU.ALL.length; i++ ) {
			if ( PremiumSKU.ALL[i].equals(itemSKU) )
				has = true ;
		}
		if ( !has ) {
			return new Result().setSuccess(false) ;
		}
		
		// Access the highest-resolution file that fits.
		int maxWidth = params.getScaleType() == ThreadedBitmapLoader.Params.ScaleType.FULL
				? Integer.MAX_VALUE : params.getWidth() ;
		int maxHeight = params.getScaleType() == ThreadedBitmapLoader.Params.ScaleType.FULL
				? Integer.MAX_VALUE : params.getHeight() ;
		
		
		// asset path?
		String filePath = premiumIconAssetPath( itemSKU, maxWidth, maxHeight ) ;
		
		// load.
		Bitmap bitmap = this.decodeAsset(mAssetManager, filePath, params) ;
		
		// done, return.
		return new Result().setBitmap(bitmap).setSuccess(true) ;
	}
	
	
	private String premiumIconAssetPath( String itemSKU, int maxWidth, int maxHeight ) {
		// We load the next largest  icon; it will be scaled back to the specified size.
		String resolution ;
		if ( maxWidth > 200 && maxHeight > 200 )
			resolution = "400x400" ;
		else if ( maxWidth > 100 && maxHeight > 100 )
			resolution = "200x200" ;
		else if ( maxWidth > 50 && maxHeight > 50 )
			resolution = "100x100" ;
		else
			resolution = "50x50" ;
		
		// we use the item SKU as the image name.
		return new File(new File(new File(new File("png"), "premium"),
				resolution), itemSKU + ".png").getPath();
	}

}
