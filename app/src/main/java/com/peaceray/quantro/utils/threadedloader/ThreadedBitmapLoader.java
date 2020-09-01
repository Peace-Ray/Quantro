package com.peaceray.quantro.utils.threadedloader;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.TypedValue;


/**
 * ThreadedBitmapLoader: an abstract class for loading Bitmaps in the background.
 * This class provides a more detailed Params implementation and a convenience method
 * for loading/scaling Bitmaps using those parameter settings.
 * 
 * @author Jake
 *
 */
public abstract class ThreadedBitmapLoader extends ThreadedLoader {
	
	public static class Params extends ThreadedLoader.Params {
		public enum ScaleType {
			/**
			 * Load the image at its full resolution, ignoring
			 * 'width' and 'height'
			 */
			FULL,
			
			/**
			 * Load the image downscaled to fit within the specified width and height.
			 */
			FIT,
			
		}
		
		private ScaleType mScaleType ;
		private int mWidth, mHeight ;
		
		public Params setScaleType( ScaleType scaleType ) {
			mScaleType = scaleType ;
			return this ;
		}
		
		public Params setSize( int width, int height ) {
			mWidth = width ;
			mHeight = height ;
			return this ;
		}
		
		protected ScaleType getScaleType() {
			return mScaleType ;
		}
		
		protected int getWidth() {
			return mWidth ;
		}
		
		protected int getHeight() {
			return mHeight ;
		}
	}
	
	protected Bitmap decodeBitmap( byte [] data, int offset, int length, Params params ) {
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		
		BitmapFactory.decodeByteArray(data, offset, length, options) ;
		Bitmap b = BitmapFactory.decodeByteArray( data, offset, length, getOptions(params, options.outWidth, options.outHeight) ) ;
		return resizeBitmapDestructively( b, params ) ;
	}
	
	protected Bitmap decodeBitmap( String pathName, Params params ) {
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		
		BitmapFactory.decodeFile(pathName, options) ;
		Bitmap b = BitmapFactory.decodeFile( pathName, getOptions(params, options.outWidth, options.outHeight) ) ;
		return resizeBitmapDestructively( b, params ) ;
	}
	
	protected Bitmap decodeBitmap( FileDescriptor fd, Rect outPadding, Params params ) {
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		
		BitmapFactory.decodeFileDescriptor( fd, outPadding, options) ;
		Bitmap b = BitmapFactory.decodeFileDescriptor( fd, outPadding, getOptions(params, options.outWidth, options.outHeight) ) ;
		return resizeBitmapDestructively( b, params ) ;
	}
	
	protected Bitmap decodeBitmap( Resources res, int id, Params params ) {
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		
		BitmapFactory.decodeResource( res, id, options) ;
		Bitmap b = BitmapFactory.decodeResource( res, id, getOptions(params, options.outWidth, options.outHeight) ) ;
		return resizeBitmapDestructively( b, params ) ;
	}
	
	protected Bitmap decodeBitmap( Resources res, TypedValue value, InputStream is, Rect pad, Params params ) {
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		
		BitmapFactory.decodeResourceStream( res, value, is, pad, options) ;
		Bitmap b = BitmapFactory.decodeResourceStream( res, value, is, pad, getOptions(params, options.outWidth, options.outHeight) ) ;
		return resizeBitmapDestructively( b, params ) ;
	}
	
	protected Bitmap decodeAsset( AssetManager am, String assetPath, Params params ) {
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		options.inJustDecodeBounds = true ;
		
		try {
			BufferedInputStream buf = new BufferedInputStream(am.open(assetPath));
			BitmapFactory.decodeStream(buf, null, options) ;
			buf.close();
			
			buf = new BufferedInputStream(am.open(assetPath));
			Bitmap b = BitmapFactory.decodeStream(buf, null, getOptions(params, options.outWidth, options.outHeight)) ;
			buf.close();
			
			return resizeBitmapDestructively( b, params ) ;
		} catch ( IOException ioe ) {
			ioe.printStackTrace() ;
			return null ;
		}
	}
		
	private BitmapFactory.Options getOptions( Params params, int imageWidth, int imageHeight ) {
		// Construct and return a BitmapFactory.Options instance which will load
		// the source image (having the provided source dimensions) according to the
		// specified parameters.
		
		BitmapFactory.Options options = new BitmapFactory.Options() ;
		
		// set options.inSampleSize.  Recall that sample size is >= 1.  For
		// example, if sampleSize is 2, we sample every-other pixel and get 1/2
		// each dimension.
		// Therefore, if our parameter width is half that of the image,
		// imageWidth / params.getWidth() will be 2 and we will have the desired
		// behavior.
		switch( params.getScaleType() ) {
		case FULL:
			// nothing
			break ;
		case FIT:
			// fit within: take the higher of the two sample sizes.
			options.inSampleSize = (int)Math.max(
					Math.floor( (double)imageWidth / params.getWidth() ),
					Math.floor( (double)imageHeight / params.getHeight() ) ) ;
			break ;
		}
		
		return options ;
	}
	
	private Bitmap resizeBitmapDestructively( Bitmap bitmap, Params params ) {
		
		if ( bitmap == null )
			return bitmap ;
		
		int w = bitmap.getWidth(), h = bitmap.getHeight() ;
		
		switch( params.getScaleType() ) {
		case FULL:
			return bitmap ;
		case FIT:
			// fit within the provided bounds...
			double scaleFactor = Math.min(
					params.getWidth() / (double)bitmap.getWidth(),
					params.getHeight() / (double)bitmap.getHeight() ) ;
			w = (int)Math.floor( scaleFactor * bitmap.getWidth() ) ;
			h = (int)Math.floor( scaleFactor * bitmap.getHeight() ) ;
			break ;
		}
		
		
		Bitmap res = Bitmap.createScaledBitmap(bitmap, w, h, true) ;
		if ( res != bitmap )
			bitmap.recycle() ;
		return res ;
	}
	

}
