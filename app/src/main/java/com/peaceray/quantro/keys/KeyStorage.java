package com.peaceray.quantro.keys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.util.Log;

/**
 * A class for storing / retrieving keys used by Quantro.
 * 
 * As of this writing, 4/4/2012, there are two types of keys
 * planned - internet multiplayer ('internet' or 'mp') keys, which
 * are distributed to potential app testers, and XL keys, which remove
 * ads and allows certain in-game options such as expanding the game window
 * to cover the title and notification bar.
 * 
 * KeyStorage does not validate or process keys in any real way; its sole
 * purpose is to provide permanent storage for keys and any associated
 * data.  For example, XL keys will typically be associated with a JSON
 * string and signature (if purchased in-app), and a quantro_xl_web signature
 * and activation tag.
 * 
 * Update 6/21/2013: Promotional Keys are stored as a set, since we can't
 * tell before loading how many we'll have.
 * 
 * PromotionalKeys: 
 * 
 * @author Jake
 *
 */
public class KeyStorage {
	
	
	private static final Object KEY_STORAGE_MUTEX = new Object() ;

	private static final String TAG = "KeyStorage" ;
	
	private static final String KEY_DIRECTORY = "Keys" ;
	private static final String FILENAME_XL_KEY = "xl_key.bin" ;
	private static final String FILENAME_XL_KEY_BACKUP = "xl_key_backup.bin" ;
	
	
	private static final String FILENAME_PREMIUM_KEY_PREFIX = "premium_" ;
	private static final String FILENAME_PREMIUM_KEY_POSTFIX = ".bin" ;
	private static final String FILENAME_PREMIUM_KEY_POSTFIX_BACKUP = "_backup.bin" ;

	private static final String FILENAME_PROMO_KEY = "promotional_keys.bin" ;
	private static final String FILENAME_PROMO_KEY_BACKUP = "promotional_keys_backup.bin" ;

	
	
	public static PromotionalKey [] getPromotionalKeys( Context context ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPromotionalKeyFile( context ) ;
			File backup_file = getPromotionalKeyBackupFile( context ) ;
	
			Key [] keys = getKeys( key_file, backup_file ) ;
			if ( keys == null ) {
				return null ;
			}
			
			PromotionalKey [] pkeys = new PromotionalKey[ keys.length ] ;
			for ( int i = 0; i < pkeys.length; i++ ) {
				pkeys[i] = (PromotionalKey) keys[i] ;
			}
			
			return pkeys ;
		}
	}
	
	
	public static PremiumContentKey getPremiumContentKey( Context context, String item ) {
		// Attempt to load the premium key.  If that fails, attempt to
		// load the backup and then write it to premium key.  Should BOTH
		// fail, return null.
		
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPremiumContentKeyFile( context, item ) ;
			File backup_file = getPremiumContentKeyBackupFile( context, item ) ;
	
			PremiumContentKey k = (PremiumContentKey) getKey( key_file, backup_file ) ;
			
			return k ;
		}
	}
	
	
	/**
	 * Loads and returns the XL key currently stored for this account.
	 * Returns 'null' if no key is found.
	 * 
	 * Note that simply returning a key object does not necessarily indicate
	 * that the key should be 
	 * 
	 * @return
	 */
	public static QuantroXLKey getXLKey( Context context ) {
		// Attempt to load the xl_key.  If that fails, attempt to
		// load the backup and then write it to xl_key.  Should BOTH
		// fail, return null.
		
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getXLKeyFile( context ) ;
			File backup_file = getXLKeyBackupFile( context ) ;
	
			QuantroXLKey k = (QuantroXLKey) getKey( key_file, backup_file ) ;
			
			return k ;
		}
	}
	
	private static Key [] getKeys( File key_file, File backup_file ) {
		Key [] keys = readMultiKeyFile( key_file ) ;
		if ( keys == null ) {
			keys = readMultiKeyFile( backup_file ) ;
			
			if ( keys != null )
				writeMultiKeyFile( keys, key_file ) ;
		}
		return keys ;
	}
	
	
	private static Key getKey( File key_file, File backup_file ) {
		Key key = readKeyFile( key_file ) ;
		if ( key == null ) {
			key = readKeyFile( backup_file ) ;
			
			if ( key != null )
				writeKeyFile( key, key_file ) ;
		}
		return key ;
	}
	
	
	public static boolean hasPromotionalKeys( Context context ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPromotionalKeyFile( context ) ;
			File backup_file = getPromotionalKeyFile( context ) ;
			
			return key_file.exists() || backup_file.exists() ;
		}
	}
	
	
	public static boolean hasPremiumContentKey( Context context, String item ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPremiumContentKeyFile( context, item ) ;
			File backup_file = getPremiumContentKeyBackupFile( context, item ) ;
			
			return key_file.exists() || backup_file.exists() ;
		}
	}
	
	
	public static boolean hasXLKey( Context context ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getXLKeyFile( context ) ;
			File backup_file = getXLKeyBackupFile( context ) ;
			
			return key_file.exists() || backup_file.exists() ;
		}
	}
	
	
	private static boolean putPromotionalKeys( Context context, PromotionalKey [] keys ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPromotionalKeyFile( context ) ;
			File backup_file = getPromotionalKeyBackupFile( context ) ;
			
			return putKeys( keys, key_file, backup_file ) ;
		}
	}
	
	
	/**
	 * Puts the provided PremimuContentKey in local storage.
	 * @param context
	 * @param key
	 * @return
	 */
	private static boolean putPremiumContentKey( Context context, PremiumContentKey key ) {
		// Do the following: open the previous key and write it to the backup.
		// Write this key to that file.  Finally, write it to the backup as well.
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPremiumContentKeyFile( context, key.getItem() ) ;
			File backup_file = getPremiumContentKeyBackupFile( context, key.getItem() ) ;
			
			return putKey( key, key_file, backup_file ) ;
		}
	}
	
	
	/**
	 * Puts the provided QuantroXLKey in local storage.
	 * @param context
	 * @param key
	 * @return
	 */
	private static boolean putXLKey( Context context, QuantroXLKey key ) {
		// Do the following: open the previous key and write it to the backup.
		// Write this key to that file.  Finally, write it to the backup as well.
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getXLKeyFile( context ) ;
			File backup_file = getXLKeyBackupFile( context ) ;
			
			return putKey( key, key_file, backup_file ) ;
		}
	}
	
	
	
	private static boolean putKeys( Key [] keys, File key_file, File backup_file ) {
		// Read the previous file and move it to backup.
		Key [] prevKeys = readMultiKeyFile( key_file ) ;
		if ( prevKeys != null )
			writeMultiKeyFile( prevKeys, backup_file ) ;
		
		// Write the new key to main, then backup.
		if ( keys == null )
			throw new IllegalArgumentException("You'd best not provide a null key!") ;
		for ( int i = 0; i < keys.length; i++ ) {
			if ( keys[i] == null ) {
				throw new IllegalArgumentException("You'd best not provide a null key!") ;
			}
		}
		
		boolean ok = true ;
		ok = ok && writeMultiKeyFile( keys, key_file ) ;
		ok = ok && writeMultiKeyFile( keys, backup_file ) ;
		
		return ok ;
	}
	
	
	private static boolean putKey( Key key, File key_file, File backup_file ) {
		// Read the previous file and move it to backup.
		Key prevKey = readKeyFile( key_file ) ;
		if ( prevKey != null )
			writeKeyFile( prevKey, backup_file ) ;
		
		// Write the new key to main, then backup.
		if ( key == null )
			throw new IllegalArgumentException("You'd best not provide a null key!") ;
		
		boolean ok = true ;
		ok = ok && writeKeyFile( key, key_file ) ;
		ok = ok && writeKeyFile( key, backup_file ) ;
		
		return ok ;
	}
	
	
	public static boolean updateKey( Context context, Key key ) {
		if ( key instanceof QuantroXLKey )
			return updateXLKey( context, (QuantroXLKey)key ) ;
		if ( key instanceof PremiumContentKey )
			return updatePremiumContentKey( context, (PremiumContentKey)key ) ;
		if ( key instanceof PromotionalKey )
			return updatePromotionalKey( context, (PromotionalKey)key ) ;
		
		// don't know what to do with this!
		return false ;
	}
	
	
	/**
	 * Updates the previous key with new information, using the same procedure
	 * as QuantroXLKey.Builder.update().
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static boolean updatePromotionalKey( Context context, PromotionalKey key ) {
		if ( key == null ) 
			throw new NullPointerException("Provided PremiumContentKey is null") ;
		
		synchronized( KEY_STORAGE_MUTEX ) {
			PromotionalKey [] keys = getPromotionalKeys( context ) ;
			// find the key.
			int index = -1 ;
			if ( keys != null ) {
				for ( int i = 0; i < keys.length; i++ ) {
					if ( keys[i].getKeyValueWithoutWrapper().equals(key.getKeyValueWithoutWrapper()) ) {
						index = i ;
						break ;
					}
				}
			}
			if ( index != -1 ) {
				// update the key.
				PromotionalKey.Builder builder = new PromotionalKey.Builder( keys[index] ) ;
				builder.update(key) ;
				
				keys[index] = (PromotionalKey) builder.build() ;
			} else if ( keys != null ) {
				// put the key at the end of the list.
				PromotionalKey [] moreKeys = new PromotionalKey[keys.length+1] ;
				for ( int i = 0; i < keys.length; i++ )
					moreKeys[i] = keys[i] ;
				moreKeys[keys.length] = key ;
				keys = moreKeys ;
			} else {
				// make a new list
				keys = new PromotionalKey[]{ key } ;
			}
			
			return putPromotionalKeys( context, keys ) ;
		}
	}
	
	
	/**
	 * Updates the previous key with new information, using the same procedure
	 * as QuantroXLKey.Builder.update().
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static boolean updatePremiumContentKey( Context context, PremiumContentKey key ) {
		if ( key == null ) 
			throw new NullPointerException("Provided PremiumContentKey is null") ;
		
		synchronized( KEY_STORAGE_MUTEX ) {
			PremiumContentKey prevKey = getPremiumContentKey( context, key.getItem() ) ;
			PremiumContentKey.Builder builder = new PremiumContentKey.Builder( prevKey ) ;
			builder.update(key) ;
			
			PremiumContentKey k = (PremiumContentKey) builder.build() ;
			
			return putPremiumContentKey( context, k ) ;
		}
	}
	
	
	/**
	 * Updates the previous key with new information, using the same procedure
	 * as QuantroXLKey.Builder.update().
	 * 
	 * @param context
	 * @param key
	 * @return
	 */
	public static boolean updateXLKey( Context context, QuantroXLKey key ) {
		if ( key == null ) 
			throw new NullPointerException("Provided QuantroXLKey is null") ;
		
		synchronized( KEY_STORAGE_MUTEX ) {
			QuantroXLKey prevKey = getXLKey( context ) ;
			QuantroXLKey.Builder builder = new QuantroXLKey.Builder( prevKey ) ;
			builder.update(key) ;
			
			QuantroXLKey k = (QuantroXLKey) builder.build() ;
			
			return putXLKey( context, k ) ;
		}
	}
	
	
	
	public static void forgetPromotionalKeys( Context context ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPromotionalKeyFile( context ) ;
			File backup_file = getPromotionalKeyBackupFile( context ) ;
			
			key_file.delete() ;
			backup_file.delete() ;
		}
	}
	
	
	public static void forgetPremiumContentKey( Context context, String item ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getPremiumContentKeyFile( context, item ) ;
			File backup_file = getPremiumContentKeyBackupFile( context, item ) ;
			
			key_file.delete() ;
			backup_file.delete() ;
		}
	}
	
	
	public static void forgetXLKey( Context context ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			File key_file = getXLKeyFile( context ) ;
			File backup_file = getXLKeyBackupFile( context ) ;
			
			key_file.delete() ;
			backup_file.delete() ;
		}
	}
	
	
	
	public static void forgetPromotionalKey( Context context, PromotionalKey key ) {
		if ( key == null ) 
			throw new NullPointerException("Provided PremiumContentKey is null") ;
		
		synchronized( KEY_STORAGE_MUTEX ) {
			PromotionalKey [] keys = getPromotionalKeys( context ) ;
			// find the key.
			int index = -1 ;
			if ( keys != null ) {
				for ( int i = 0; i < keys.length; i++ ) {
					if ( keys[i].getKeyValueWithoutWrapper().equals(key.getKeyValueWithoutWrapper()) ) {
						index = i ;
						break ;
					}
				}
			}
			if ( index != -1 && keys.length > 1 ) {
				// remove the key.
				PromotionalKey [] lessKeys = new PromotionalKey[keys.length-1] ;
				for ( int i = 0; i < lessKeys.length; i++ ) {
					if ( i < index )
						lessKeys[i] = keys[i] ;
					else 
						lessKeys[i] = keys[i+1] ;
				}
				putPromotionalKeys( context, lessKeys ) ;
			} else if ( index != -1 ) {
				forgetPromotionalKeys( context ) ;
			}
		}
	}
	
	
	public static boolean forgetPremiumContentKey( Context context, PremiumContentKey key ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			PremiumContentKey prevKey = getPremiumContentKey(context, key.getItem()) ;
			if ( prevKey != null ) {
				// if this key and the previous key are "the same key", forget it.
				// they are the same if they share a JSON string or a key value.
				if ( ( key.getKey() != null && key.getKey().equals( prevKey.getKey() ) )
						|| ( key.getJSON() != null && key.getJSON().equals( prevKey.getJSON() ) ) ) {
					forgetPremiumContentKey( context, key.getItem() ) ;
					return true ;
				}
			}
		}
		return false ;
	}
	
	
	public static boolean forgetXLKey( Context context, QuantroXLKey key ) {
		synchronized( KEY_STORAGE_MUTEX ) {
			QuantroXLKey prevKey = getXLKey(context) ;
			if ( prevKey != null ) {
				// if this key and the previous key are "the same key", forget it.
				// they are the same if they share a JSON string or a key value.
				if ( ( key.getKey() != null && key.getKey().equals( prevKey.getKey() ) )
						|| ( key.getJSON() != null && key.getJSON().equals( prevKey.getJSON() ) ) ) {
					forgetXLKey( context ) ;
					return true ;
				}
			}
		}
		return false ;
	}
	
	
	
	private static File getXLKeyFile( Context context ) {
		File keyDir = context.getDir(KEY_DIRECTORY, 0) ;
		File keyFile = new File( keyDir, FILENAME_XL_KEY ) ;
		return keyFile ;
	}
	
	private static File getXLKeyBackupFile( Context context ) {
		File keyDir = context.getDir(KEY_DIRECTORY, 0) ;
		File keyFile = new File( keyDir, FILENAME_XL_KEY_BACKUP ) ;
		return keyFile ;
	}
	
	
	private static File getPremiumContentKeyFile( Context context, String item ) {
		StringBuilder sb = new StringBuilder() ;
		sb.append(FILENAME_PREMIUM_KEY_PREFIX) ;
		sb.append(item) ;
		sb.append(FILENAME_PREMIUM_KEY_POSTFIX) ;
		
		File keyDir = context.getDir(KEY_DIRECTORY, 0) ;
		File keyFile = new File( keyDir, sb.toString() ) ;
		return keyFile ;
	}
	
	private static File getPremiumContentKeyBackupFile( Context context, String item ) {
		StringBuilder sb = new StringBuilder() ;
		sb.append(FILENAME_PREMIUM_KEY_PREFIX) ;
		sb.append(item) ;
		sb.append(FILENAME_PREMIUM_KEY_POSTFIX_BACKUP) ;
		
		File keyDir = context.getDir(KEY_DIRECTORY, 0) ;
		File keyFile = new File( keyDir, sb.toString() ) ;
		return keyFile ;
	}
	
	
	private static File getPromotionalKeyFile( Context context ) {
		File keyDir = context.getDir(KEY_DIRECTORY, 0) ;
		File keyFile = new File( keyDir, FILENAME_PROMO_KEY ) ;
		return keyFile ;
	}
	
	private static File getPromotionalKeyBackupFile( Context context ) {
		File keyDir = context.getDir(KEY_DIRECTORY, 0) ;
		File keyFile = new File( keyDir, FILENAME_PROMO_KEY_BACKUP ) ;
		return keyFile ;
	}
	
	
	private static Key readKeyFile( File file ) {
		Key key = null ;
		try {
			FileInputStream fis = new FileInputStream( file ) ;
			ObjectInputStream ois = new ObjectInputStream( fis ) ;
			
			key = (Key)ois.readObject() ;
		} catch (IOException ioe) {
			//Log.d(TAG, ioe.getMessage()) ;
		} catch (ClassNotFoundException e) {
			//Log.d(TAG, e.getMessage()) ;
		}
		
		return key ;
	}
	
	
	private static boolean writeKeyFile( Key key, File file ) {
		if ( key == null )
			throw new NullPointerException("Null Key provided") ;
		try {
			FileOutputStream fos = new FileOutputStream( file ) ;
			ObjectOutputStream oos = new ObjectOutputStream( fos ) ;
			oos.writeObject(key) ;
			
			return true ;
		} catch( IOException ioe ) {
			//Log.d(TAG, ioe.getMessage()) ;
		}
		
		return false ;
	}
	

	private static Key [] readMultiKeyFile( File file ) {
		Key [] keys = null ;
		try {
			FileInputStream fis = new FileInputStream( file ) ;
			ObjectInputStream ois = new ObjectInputStream( fis ) ;
			
			int numKeys = ois.readInt() ;
			keys = new Key[numKeys] ;
			for ( int i = 0; i < numKeys; i++ )
				keys[i] = (Key)ois.readObject() ;
		} catch (IOException ioe) {
			//Log.d(TAG, ioe.getMessage()) ;
			return null ;
		} catch (ClassNotFoundException e) {
			//Log.d(TAG, e.getMessage()) ;
			return null ;
		}
		
		return keys ;
	}
	
	
	private static boolean writeMultiKeyFile( Key [] keys, File file ) {
		if ( keys == null )
			throw new NullPointerException("Null array provided") ;
		for ( int i = 0; i < keys.length; i++ ) {
			if ( keys[i] == null ) {
				throw new NullPointerException("Null Key provided in array") ;
			}
		}
		if ( keys.length == 0 )
			throw new NullPointerException("Empty Key array provided; need at least 1.") ;
		try {
			FileOutputStream fos = new FileOutputStream( file ) ;
			ObjectOutputStream oos = new ObjectOutputStream( fos ) ;
			
			oos.writeInt( keys.length ) ;
			for ( int i = 0; i < keys.length; i++ )
				oos.writeObject(keys[i]) ;
			
			return true ;
		} catch( IOException ioe ) {
			//Log.d(TAG, ioe.getMessage()) ;
		}
		
		return false ;
	}
	
}
