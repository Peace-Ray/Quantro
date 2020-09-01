package com.peaceray.quantro.utils.threadedloader;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.utils.AssetAccessor;
import com.peaceray.quantro.view.game.DrawSettings;



public class ThreadedBackgroundThumbnailLoader extends ThreadedBitmapLoader {
	
	public class Params extends ThreadedBitmapLoader.Params {
		Background mBackground ;
		
		public Params setBackground( Background background ) {
			mBackground = background ;
			return this ;
		}
		
		public Background getBackground() {
			return mBackground ;
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
	
	public ThreadedBackgroundThumbnailLoader( AssetManager am ) {
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
		Background bg = params.getBackground() ;
		
		if ( bg == null || !bg.hasImage() ) {
			// we require a background with an image...
			return new Result().setSuccess(false) ;
		}
		
		// our ultimate goal is to get a background file.  Access the highest-resolution
		// file that fits.
		int maxWidth = params.getScaleType() == ThreadedBitmapLoader.Params.ScaleType.FULL
				? Integer.MAX_VALUE : params.getWidth() ;
		int maxHeight = params.getScaleType() == ThreadedBitmapLoader.Params.ScaleType.FULL
				? Integer.MAX_VALUE : params.getHeight() ;

		String filePath = AssetAccessor.assetPathFromBackground(
				bg.getImageName(), DrawSettings.IMAGES_SIZE_MID, maxWidth, maxHeight, true ) ;
		// load!
		Bitmap bitmap = this.decodeAsset(mAssetManager, filePath, params) ;
		
		if ( bitmap == null ) {
			return new Result().setSuccess(false) ;
		}
		
		return new Result().setBitmap(bitmap).setSuccess(true) ;
	}
	

}
