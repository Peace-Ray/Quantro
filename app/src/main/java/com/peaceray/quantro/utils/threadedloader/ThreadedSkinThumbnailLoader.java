package com.peaceray.quantro.utils.threadedloader;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.utils.AssetAccessor;



public class ThreadedSkinThumbnailLoader extends ThreadedBitmapLoader {
	
	public class Params extends ThreadedBitmapLoader.Params {
		Skin mSkin ;
		
		public Params setSkin( Skin skin ) {
			mSkin = skin ;
			return this ;
		}
		
		public Skin getSkin() {
			return mSkin ;
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
	
	public ThreadedSkinThumbnailLoader( AssetManager am ) {
		mAssetManager = am ;
	}

	@Override
	protected com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Params newParams() {
		return new Params() ;
	}

	@Override
	protected com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Result load(
			com.peaceray.quantro.utils.threadedloader.ThreadedLoader.Params p) {
		
		if ( !(p instanceof Params) ) {
			// wrong Params...
			return new Result().setSuccess(false) ;
		}
		
		Params params = (Params)p ;
		Skin s = params.getSkin() ;
		
		if ( s == null  ) {
			// we require a skin with an image...
			return new Result().setSuccess(false) ;
		}
		
		// our ultimate goal is to get a skin file.  Access the highest-resolution
		// file that fits.
		int maxWidth = params.getScaleType() == ThreadedBitmapLoader.Params.ScaleType.FULL
				? Integer.MAX_VALUE : params.getWidth() ;
		int maxHeight = params.getScaleType() == ThreadedBitmapLoader.Params.ScaleType.FULL
				? Integer.MAX_VALUE : params.getHeight() ;

		String filePath = AssetAccessor.assetPathForSkinThumbnail( s, maxWidth, maxHeight, true ) ;
		// load!
		Bitmap bitmap = this.decodeAsset(mAssetManager, filePath, params) ;
		
		if ( bitmap == null ) {
			return new Result().setSuccess(false) ;
		}
		
		return new Result().setBitmap(bitmap).setSuccess(true) ;
	}
	

}
