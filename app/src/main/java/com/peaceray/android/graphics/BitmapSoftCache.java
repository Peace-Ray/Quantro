package com.peaceray.android.graphics;

import java.lang.ref.SoftReference;
import java.util.Hashtable;

import android.graphics.Bitmap;


/**
 * A Bitmap softcache is a way of storing Bitmaps in a cache without
 * worrying about memory issues.  Bitmaps are stored as SoftReferences,
 * so there is no guarantee of long-term retention.
 * 
 * @author Jake
 *
 */
public class BitmapSoftCache {

	private Hashtable<Object, SoftReference<Bitmap>> mBitmaps ;
	
	public BitmapSoftCache() {
		mBitmaps = new Hashtable<Object, SoftReference<Bitmap>>() ;
	}
	
	public Bitmap get(Object key) {
		SoftReference<Bitmap> srb = mBitmaps.get(key) ;
		return srb == null ? null : srb.get() ;
	}
	
	public void put(Object key, Bitmap b) {
		if ( b == null ) {
			if ( mBitmaps.containsKey(key) ) {
				mBitmaps.remove(key) ;
			}
		} else {
			mBitmaps.put(key, new SoftReference<Bitmap>(b)) ;
		}
	}
	
	public Bitmap remove(Object key) {
		SoftReference<Bitmap> srb = mBitmaps.remove(key) ;
		if ( srb != null )
			return srb.get() ;
		return null ;
	}
	
	public void clear() {
		mBitmaps.clear() ;
	}
}
