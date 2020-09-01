package com.peaceray.quantro.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.view.game.DrawSettings;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

/**
 * A class for static access to user assets.  We use 'assets' for the storage
 * of glorified text files in HTML format and such, things that we don't mind
 * every other part of the phone looking at (probably not writing to, though).
 * 
 * Upon Application create, AssetAccessor.updateAssets should be called.  This
 * method has no effect if the assets are up to date.
 * 
 * NOTE:  A strict design decision is now in place.  Assets are organized in
 * 
 * 	assets/<type>/<subdir>/file
 * 
 * where type indicates the asset type (e.g. "html"), subdir is a sorting subdirectory
 * for the purpose of the file (e.g. "info"), and within it it are files NOT directories.
 * 
 * Note: android local files are terrible.  They must be root-level, and possibl
 * 
 * @author Jake
 *
 */
public class AssetAccessor {
	
	private static final String TAG = "AssetAccessor" ;
	
	private static final String LOCAL_STORAGE_ASSET_DIR = "assets" ;
	
	/**
	 * Converts a file path relative to our assets directory, e.g.
	 * "html/app_info/credits.html", to one relative to our local
	 * storage path, e.g. "assets/html/app_info/credits.html"
	 * 
	 * @param relPath
	 * @return
	 */
	public static String assetRelativeToLocalStorageRelative( String relPath ) {
		
		return new File( LOCAL_STORAGE_ASSET_DIR, relPath ).getPath() ;
	}
	
	
	/**
	 * Converts a file path relative to our assets directory, e.g.
	 * "html/app_info/credits.html", to one that is absolute and
	 * within our local storage, e.g. "/local/com.peaceray.quantro/assets/html/app_info/credits.html".
	 * 
	 * Since everything in Assets in world-readable, this path is probably
	 * suitable for passing to other Activities, such as a browser (just 
	 * be sure to prepend "file://" in that case!)
	 * 
	 * @param relPath
	 * @return
	 */
	public static String assetRelativeToLocalStorageAbsolute( Context context, String relPath ) {
		return new File(
				context.getFilesDir(),
				assetRelativeToLocalStorageRelative( relPath )
			).getPath() ;
	}
	
	/**
	 * Returns a path to the directory within which we have stored the asset
	 * files.  A convenience method for things like
	 * WebView.loadDataWithBaseURL, which likes a base URL for references.
	 * 
	 * @param context
	 * @return
	 */
	public static String getLocalStorageAbsolute( Context context ) {
		return new File( context.getFilesDir(), LOCAL_STORAGE_ASSET_DIR ).getPath();
	}

	/**
	 * Updates the local storage assets from those in the application
	 * package's 'assets' directory.  Returns whether the local assets
	 * are up to date (i.e., if they were up to date before, or we
	 * successfully updated them, returns true).
	 * 
	 * @param context
	 * @return
	 */
	public static boolean updateAssets( Context context ) {
		if ( assetsNeedUpdate( context ) ) {
			// Read all asset files, copy them one at a time to our
			// internal storage.  By convention, our assets are organized
			// as assets/dir1/dir2/*.suffix; exactly 2 nested directories.
			
			// Get an asset manager
			AssetManager assetManager = context.getAssets() ;
			
			// Use a 1k buffer
			byte [] buffer = new byte[1024] ;
			
			try {
				// List of dir1s in the assets...
				String [] dir1s = assetManager.list("") ;
				for ( int i = 0; i < dir1s.length; i++ ) {
					String [] dir2s = assetManager.list(dir1s[i]) ;
					for ( int j = 0; j < dir2s.length; j++ ) {
						String relPath = new File( dir1s[i], dir2s[j] ).getPath() ;
						if ( !copyAssetDirectoryToLocalStorage( context, assetManager, relPath, buffer ) ) {
							Log.e(TAG, "Problem updating assets") ;
							return false ;
						}
					}
				}
				
				return true ;
			
			} catch (IOException e) {
				Log.e(TAG, "IOException in updateAssets: " + e.getLocalizedMessage()) ;
				return false ;
			}
		}
		
		return true ;
	}
	
	
	public static boolean assetsNeedUpdate( Context context ) {
		// TODO: Check the asset version
		return true ;
	}
	
	
	/**
	 * Copies every file in the given asset directory, which is assumed
	 * to contain only files and not subdirectories.
	 * 
	 * @param context
	 * @param assetManager
	 * @param relPath
	 * @param buffer
	 * @return
	 */
	private static boolean copyAssetDirectoryToLocalStorage( Context context, AssetManager assetManager, String relPath, byte [] buffer ) {
		try {
			String [] filePaths = assetManager.list(relPath) ;
			for ( int i = 0; i < filePaths.length; i++ ) {
				String path = new File( relPath, filePaths[i] ).getPath() ;
				if ( !copyAssetFileToLocalStorage( context, assetManager, path, buffer ) )
					return false ;
			}
			return true ;
		} catch (IOException e) {
			Log.e(TAG, "IOException when copying asset directory " + relPath + " to local storage: " + e.getLocalizedMessage()) ;
			return false ;
		}
	}
	
	
	private static boolean copyAssetFileToLocalStorage( Context context, AssetManager assetManager, String relPath, byte [] buffer ) {
		try {
			// Now write (appending) to the file.  We notice that including separators
			// fails unless we use mode Append.  That's why we deleted above.
			InputStream is = assetManager.open(relPath) ;
			
			// Get a world-readable directory to put it in.
			File targetFile = new File( assetRelativeToLocalStorageAbsolute(context, relPath) ) ;
			targetFile.delete() ;
			targetFile.getParentFile().mkdirs() ;
			FileOutputStream fos = new FileOutputStream(targetFile) ;
			
			int bytesRead = 0 ;
			while ( (bytesRead = is.read(buffer)) > 0 )
				fos.write(buffer, 0, bytesRead) ;
			
			is.close() ;
			fos.close() ;
			
			Log.d(TAG, "Wrote to " + targetFile.getPath()) ;
			
			return true ;
			
		} catch (IOException e) {
			Log.e(TAG, "IOException when copying asset file " + relPath + " to local storage: " + e.getLocalizedMessage()) ;
			return false ;
		}
	}

	
	public static String assetPathFromBackground( Background.Template t, Background.Shade s, int bgLoadSize, int maxWidth, int maxHeight ) {
		return assetPathFromBackground( Background.get(t, s), bgLoadSize, maxWidth, maxHeight, false ) ;
	}
	
	public static String assetPathFromBackground( Background.Template t, Background.Shade s, int bgLoadSize, int maxWidth, int maxHeight, boolean oversize ) {
		return assetPathFromBackground( Background.get(t, s), bgLoadSize, maxWidth, maxHeight, oversize ) ;
	}
	
	public static String assetPathFromBackground( Background bg, int bgLoadSize, int maxWidth, int maxHeight ) {
		if ( bg.hasImage() ) {
			return assetPathFromBackground( bg.getImageName(), bgLoadSize, maxWidth, maxHeight, false ) ;
		}
		return null ;
	}
	
	public static String assetPathFromBackground( Background bg, int bgLoadSize, int maxWidth, int maxHeight, boolean oversize ) {
		if ( bg.hasImage() ) {
			return assetPathFromBackground( bg.getImageName(), bgLoadSize, maxWidth, maxHeight, oversize ) ;
		}
		return null ;
	}
	
	
	public static String assetPathFromBackground( String bgName, int bgLoadSize, int maxWidth, int maxHeight ) {
		return assetPathFromBackground( bgName, bgLoadSize, maxWidth, maxHeight, false ) ;
	}
	
	public static String assetPathFromBackground( String bgName, int bgLoadSize, int maxWidth, int maxHeight, boolean oversize ) {
    	String resStr = null ;
    	if ( bgLoadSize >= DrawSettings.IMAGES_SIZE_LARGE && maxWidth > 240 && maxHeight > 400 )
    		resStr = "480x800" ;
    	else if ( bgLoadSize >= DrawSettings.IMAGES_SIZE_MID && maxWidth > 120 && maxHeight > 200 ) {
    		if ( oversize )
    			resStr = "480x800" ;
    		else
    			resStr = "240x400" ;
    	} else {
    		if ( oversize )
    			resStr = "240x400" ;
    		else
    			resStr = "120x200" ;
    	}
    	
    	return new File(new File(new File(new File("game"), "backgrounds"),
				resStr), bgName + ".png").getPath();
    }
	
	
	public static String assetPathForSkinThumbnail( Skin skin, int maxWidth, int maxHeight ) {
		return assetPathForSkinThumbnail( skin, maxWidth, maxHeight, false ) ;
	}
	
	
	public static String assetPathForSkinThumbnail( Skin skin, int maxWidth, int maxHeight, boolean oversize ) {
		String resStr = null ;
		if ( maxWidth > 300 && maxHeight > 400 )
			resStr = "300x400" ;
		else if ( maxWidth > 150 && maxHeight > 200 ) {
			if ( oversize )
				resStr = "300x400" ;
			else
				resStr = "150x200" ;
		} else {
			if ( oversize )
				resStr = "300x400" ;
			else
				resStr = "75x100" ;
		}
		
		String skinThumbName = skin.getGame() + "_" + skin.getTemplate() + "_" + skin.getColor() + ".png" ;
		
		return new File(new File(new File(new File("png"), "skin"), resStr), skinThumbName).getPath() ;
	}
	
	
	public static String assetToString( Context context, String assetPath ) {
		//Log.d(TAG, "assetToString bracket IN") ;
		try {
			int len ;
			char[] chr = new char[4096] ;
			final StringBuffer buffer = new StringBuffer();
			BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open(assetPath)));
			while ( (len = br.read(chr))  > 0 ) {
				buffer.append(chr, 0, len) ;
			}
			//Log.d(TAG, "assetToString bracket OUT (buffer to string)") ;
			return buffer.toString() ;
		} catch( Exception e ) {
			e.printStackTrace() ;
			//Log.d(TAG, "assetToString bracket OUT (null)") ;
			return null ;
		}
	}
	
}
