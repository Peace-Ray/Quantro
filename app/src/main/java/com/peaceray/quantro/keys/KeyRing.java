package com.peaceray.quantro.keys;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import com.peaceray.quantro.premium.PremiumSKU;

import android.content.Context;


/**
 * KeyRing is the first step towards a single, unified point of access
 * for checking and verifying keys.
 * 
 * A KeyRing acts as a container and abstraction layer for a collection
 * of Keys.  It also performs signature verification, which previously was
 * handled by QuantroActivity.
 * 
 * KeyRing instances also act as a one-way interface with KeyStorage.  Hopefully
 * you won't need to directly touch KeyStorage, once you have a KeyRing instance.
 * 
 * However, to avoid complications with synchronization, KeyRings have only
 * read-access to KeyStorage.  If you want to update keys in storage, do
 * it yourself.
 * 
 * @author Jake
 *
 */
public class KeyRing {
	
	/**
	 * A list of current key objects, indexed by Type.ordinal().  May be 'null.'
	 */
	QuantroXLKey mXLKey ;
	PremiumContentKey [] mPremiumContentKeys ;
	PromotionalKey [] mPromotionalKeys ;
	
	
	/**
	 * Stores a Boolean cacheing the result of a signature check.  This value is
	 * automatically recalculated when the Key is loaded or stored.
	 */
	Object mXLSignatureCheck ;
	Object [] mPremiumContentSignatureCheck ;
	Object [] mPromotionalSignatureCheck ;
	
	
	/**
	 * Copy-Constructor for KeyRing.  Keys are immutable so
	 * we can just copy directly.
	 * 
	 * @param keyRing
	 */
	public KeyRing( KeyRing keyRing ) {
		mXLKey = keyRing.mXLKey ;
		mXLSignatureCheck = keyRing.mXLSignatureCheck ;
		
		mPremiumContentKeys = new PremiumContentKey[PremiumSKU.ALL.length] ;
		mPremiumContentSignatureCheck = new Object[PremiumSKU.ALL.length] ;
		for ( int i = 0; i < PremiumSKU.ALL.length; i++ ) {
			mPremiumContentKeys[i] = keyRing.mPremiumContentKeys[i] ;
			mPremiumContentSignatureCheck[i] = keyRing.mPremiumContentSignatureCheck[i] ;
		}
		
		mPromotionalKeys = new PromotionalKey[keyRing.mPromotionalKeys.length] ;
		mPromotionalSignatureCheck = new Object[keyRing.mPromotionalSignatureCheck.length] ;
		for ( int i = 0; i < keyRing.mPromotionalKeys.length; i++ ) {
			mPromotionalKeys[i] = keyRing.mPromotionalKeys[i] ;
			mPromotionalSignatureCheck[i] = keyRing.mPromotionalSignatureCheck[i] ;
		}
	}
	
	private KeyRing() {
		mXLKey = null ;
		mXLSignatureCheck = null ;
		
		mPremiumContentKeys = new PremiumContentKey[PremiumSKU.ALL.length] ;
		mPremiumContentSignatureCheck = new Object[PremiumSKU.ALL.length] ;
		
		mPromotionalKeys = new PromotionalKey[0] ;
		mPromotionalSignatureCheck = new Object[0] ;
	}

	/**
	 * Loads a fresh KeyRing instance from storage, using KeyStorage
	 * methods as appropriate.
	 * 
	 */
	public static KeyRing loadNewKeyRing( Context context ) {
		KeyRing keyRing = new KeyRing() ;
		
		// load each key.
		keyRing.loadAll(context) ;
		
		return keyRing ;
	}
	
	
	/**
	 * Equivalent to iterating through .Type.values() and calling
	 * load( ... ) for all.
	 * 
	 * @param context
	 */
	public void loadAll( Context context ) {
		loadXL( context ) ;
		for ( int i = 0; i < PremiumSKU.ALL.length; i++ ) {
			loadPremium( context, PremiumSKU.ALL[i] ) ;
		}
		loadPromotions( context ) ;
	}
	
	
	/**
	 * Loads the specified key type from storage.  Returns whether
	 * the key exists in storage, and thus the return value of 'has.'
	 * 
	 * @param type
	 * @return
	 */
	public boolean loadXL( Context context ) {
		Key k = mXLKey = KeyStorage.getXLKey(context) ;
		
		// attempt a signature check.
		if ( k != null ) {
			mXLSignatureCheck = checkSignature( context, k ) ;
		} else {
			mXLSignatureCheck = null ;
		}
		
		return k != null ;
	}
	
	
	public boolean loadPremium( Context context, String item ) {
		int index = getPremiumIndex( item ) ;
		
		if ( index < 0 )
			throw new IllegalArgumentException("Don't recognize item type " + item) ;
		
		Key k = mPremiumContentKeys[index] = KeyStorage.getPremiumContentKey(context, item) ;
		
		// attempt a signature check.
		if ( k != null ) {
			mPremiumContentSignatureCheck[index] = checkSignature( context, k ) ;
		} else {
			mPremiumContentSignatureCheck[index] = null ;
		}
		
		return k != null ;
	}
	
	
	public boolean loadPromotions( Context context ) {
		PromotionalKey [] keys = KeyStorage.getPromotionalKeys(context) ;
		if ( keys == null ) {
			mPromotionalKeys = new PromotionalKey[0] ;
			mPromotionalSignatureCheck = new Object[0] ;
		} else {
			mPromotionalKeys = keys ;
			mPromotionalSignatureCheck = new Object[ keys.length ] ;
			for ( int i = 0; i < keys.length; i++ )
				mPromotionalSignatureCheck[i] = checkSignature( context, keys[i] ) ;
		}
		
		return keys != null ;
	}
	
	
	public boolean hasAny() {
		if ( mXLKey != null )
			return true ;
		for ( int i = 0; i < mPremiumContentKeys.length; i++ ) {
			if ( mPremiumContentKeys[i] != null )
				return true ;
		}
		for ( int i = 0; i < mPromotionalKeys.length; i++ ) {
			if ( mPromotionalKeys[i] != null )
				return true ;
		}
		
		return false ;
	}
	
	
	/**
	 * Returns whether this key ring has an XL key.
	 * @param type
	 * @return
	 */
	public boolean hasXL() {
		return mXLKey != null ;
	}
	
	
	public boolean hasPremium( String item ) {
		int index = getPremiumIndex( item ) ;
		if ( index < 0 )
			return false ;
		return mPremiumContentKeys[index] != null ;
	}
	
	
	public boolean signedAny() {
		Object obj ;
		obj = mXLSignatureCheck ;
		if ( obj != null && ((Boolean)obj).booleanValue() )
			return true ;
		for ( int i = 0; i < mPremiumContentSignatureCheck.length; i++ ) {
			obj = mPremiumContentSignatureCheck[i] ;
			if ( obj != null && ((Boolean)obj).booleanValue() )
				return true ;
		}
		for ( int i = 0; i < mPromotionalSignatureCheck.length; i++ ) {
			obj = mPromotionalSignatureCheck[i] ;
			if ( obj != null && ((Boolean)obj).booleanValue() )
				return true ;
		}
		
		return false ;
	}
	
	
	/**
	 * Returns whether the XL key is present and signed.
	 * 
	 * @param type
	 * @return
	 */
	public boolean signedXL() {
		Object obj = mXLSignatureCheck ;
		return obj != null && ((Boolean)obj).booleanValue() ;
	}
	
	
	/**
	 * Returns whether the specified premium key is present and signed.
	 * 
	 * @param type
	 * @return
	 */
	public boolean signedPremium( String item ) {
		int index = getPremiumIndex( item ) ;
		if ( index < 0 )
			return false ;
		Object obj = mPremiumContentSignatureCheck[index] ;
		return obj != null && ((Boolean)obj).booleanValue() ;
	}
	
	
	/**
	 * Returns whether we have a signed promotional key for the
	 * specified content item.
	 * 
	 * @param item
	 * @return
	 */
	public boolean signedPromotion( String item ) {
		for ( int i = 0; i < mPromotionalKeys.length; i++ ) {
			if (  mPromotionalKeys[i] != null && mPromotionalKeys[i].getHasSKU(item) ) {
				Object obj = mPromotionalSignatureCheck[i] ;
				if ( obj != null && ((Boolean)obj).booleanValue() )
					return true ;
			}
		}
		
		return false ;
	}
	
	
	
	/**
	 * Returns a Key object of the specified type, or 'null'
	 * if none is present.
	 * 
	 * @param type
	 * @return
	 */
	public Key getXL() {
		return mXLKey ;
	}
	
	public Key getPremium( String item ) {
		int index = getPremiumIndex( item ) ;
		return index < 0 ? null : mPremiumContentKeys[index] ;
	}
	
	public Key getPromotion( String item ) {
		// prefer a signed key.  Otherwise, return the first we got.
		for ( int i = 0; i < mPromotionalKeys.length; i++ ) {
			if (  mPromotionalKeys[i] != null && mPromotionalKeys[i].getHasSKU(item) ) {
				Object obj = mPremiumContentSignatureCheck[i] ;
				if ( obj != null && ((Boolean)obj).booleanValue() )
					return mPromotionalKeys[i] ;
			}
		}
		
		for ( int i = 0; i < mPromotionalKeys.length; i++ ) {
			if (  mPromotionalKeys[i] != null && mPromotionalKeys[i].getHasSKU(item) ) {
				return mPromotionalKeys[i] ;
			}
		}
		
		return null ;
	}
	
	/**
	 * Inserts the provided key into the KeyRing, overwriting
	 * whatever key is present.
	 * 
	 * @param key
	 */
	public void put( Context context, Key key ) {
		if ( key == null ) {
			throw new NullPointerException("Don't know what to do with null key.") ;
		}
		
		Object sig = checkSignature( context, key ) ;
		if ( key instanceof QuantroXLKey ) {
			mXLKey = (QuantroXLKey)key ;
			mXLSignatureCheck = sig ;
		} else if ( key instanceof PremiumContentKey ) {
			int index = getPremiumIndex( ((PremiumContentKey)key).getItem()) ;
			mPremiumContentKeys[index] = (PremiumContentKey)key ;
			mPremiumContentSignatureCheck[index] = sig ;
		} else if ( key instanceof PromotionalKey ) {
			throw new IllegalArgumentException("Can't insert promotion keys into the key ring directly (not implemented)") ;
		} else {
			throw new IllegalArgumentException("Don't know what to do with this key: " + key) ;
		}
	}
	
	
	public void removeXL() {
		mXLKey = null ;
		mXLSignatureCheck = null ;
	}
	
	public void removePremium( String item ) {
		int index = getPremiumIndex( item ) ;
		mPremiumContentKeys[index] = null ;
		mPremiumContentSignatureCheck[index] = null ;
	}
	
	public void removePromo( PromotionalKey promo ) {
		int index = -1 ;
		for ( int i = 0; i < mPromotionalKeys.length; i++ ) {
			if ( mPromotionalKeys[i].getKeyValueWithoutWrapper().equals( promo.getKeyValueWithoutWrapper() ) ) {
				index = i ;
				break ;
			}
		}
		
		if ( index > -1 ) {
			PromotionalKey [] keys = new PromotionalKey[mPromotionalKeys.length-1] ;
			Object [] sigs = new Object[mPromotionalKeys.length-1] ;
			for ( int i = 0; i < keys.length; i++ ) {
				int prevIndex = i < index ? i : i+1 ;
				keys[i] = mPromotionalKeys[prevIndex] ;
				sigs[i] = mPromotionalSignatureCheck[prevIndex] ;
			}
			
			mPromotionalKeys = keys ;
			mPromotionalSignatureCheck = sigs ;
		}
	}
	
	private int getPremiumIndex( String item ) {
		int index = -1 ;
		for ( int i = 0; i < PremiumSKU.ALL.length; i++ ) {
			if ( PremiumSKU.ALL[i].equals(item) ) {
				index = i ;
				break ;
			}
		}
		
		return index ;
	}
	
	
	
	/**
	 * Performs a signature check for the specified key, returning an Object
	 * indicating whether the signature is valid ( and signed for this device ).
	 * 
	 * Returns a Boolean instance True if the specified key type exists,
	 * is valid, and the signature checks out.  Returns 'false' if the
	 * signature is not OK or the key is not valid.
	 * 
	 * Returns 'null' if, for whatever reason, we can't perform a signature check.
	 * 
	 * @param type
	 * @return
	 */
	private Object checkSignature( Context context, Key instanceKey ) {
		Object result = null ;
		
		try {

			// THE BELOW IS MEANT TO BE COPY-PASTA'ED WHEREEVER A KEYCHECK IS
			// NEEDED.
			// COPY THE PRIVATE STATIC STRUCTURES AS WELL.

			// Classes used
			Class<?> classBoolean = Class.forName(LANG_PACKAGE + AU[B] + Al[O]
					+ Al[O] + Al[L] + Al[E] + Al[A] + Al[N]);
			Class<?> classKey = Class.forName(KEY_PACKAGE + AU[K] + Al[E] + Al[Y]);
			Class<?> classAndroidID = Class.forName(UTILS_PACKAGE + AU[A] + Al[N]
					+ Al[D] + Al[R_letter] + Al[O] + Al[I] + Al[D] + AU[I]
					+ AU[D]);
			Class<?> classBase64 = Class.forName(B64_PACKAGE + AU[B] + Al[A]
					+ Al[S] + Al[E] + DG[6] + DG[4]);
			Class<?> classSignature = Class.forName(SECURITY_PACKAGE + AU[S]
					+ Al[I] + Al[G] + Al[N] + Al[A] + Al[T] + Al[U]
					+ Al[R_letter] + Al[E]);
			Class<?> classPublicKey = Class.forName(SECURITY_PACKAGE + AU[P]
					+ Al[U] + Al[B] + Al[L] + Al[I] + Al[C] + AU[K] + Al[E]
					+ Al[Y]);
			Class<?> classKeyFactory = Class.forName(SECURITY_PACKAGE + AU[K]
					+ Al[E] + Al[Y] + AU[F] + Al[A] + Al[C] + Al[T] + Al[O]
					+ Al[R_letter] + Al[Y]);
			Class<?> classKeySpec = Class.forName(SECURITY_SPEC_PACKAGE + AU[K]
					+ Al[E] + Al[Y] + AU[S] + Al[P] + Al[E] + Al[C]);
			Class<?> classX509EncodedKeySpec = Class.forName(SECURITY_SPEC_PACKAGE
					+ AU[X] + DG[5] + DG[0] + DG[9] + AU[E] + Al[N] + Al[C]
					+ Al[O] + Al[D] + Al[E] + Al[D] + AU[K] + Al[E] + Al[Y]
					+ AU[S] + Al[P] + Al[E] + Al[C]);

			// Methods used;
			Method methodKeyIsActivated = classKey.getMethod(Al[I] + Al[S]
					+ AU[A] + Al[C] + Al[T] + Al[I] + Al[V] + Al[A] + Al[T]
					+ Al[E] + Al[D]);
			Method methodKeyIsValid = classKey.getMethod(Al[I] + Al[S] + AU[V]
					+ Al[A] + Al[L] + Al[I] + Al[D]);
			Method methodKeyGetSignedString = classKey.getMethod(Al[G] + Al[E]
					+ Al[T] + AU[S] + Al[I] + Al[G] + Al[N] + Al[E] + Al[D]
					+ AU[S] + Al[T] + Al[R_letter] + Al[I] + Al[N] + Al[G]);
			Method methodKeyGetKeySignature = classKey.getMethod(Al[G] + Al[E]
					+ Al[T] + AU[K] + Al[E] + Al[Y] + AU[S] + Al[I] + Al[G]
					+ Al[N] + Al[A] + Al[T] + Al[U] + Al[R_letter] + Al[E]);
			Method methodKeyGetAndroidID = classKey.getMethod(Al[G] + Al[E]
					+ Al[T] + AU[A] + Al[N] + Al[D] + Al[R_letter] + Al[O]
					+ Al[I] + Al[D] + AU[I] + AU[D]);
			Method methodAndroidIDGet = classAndroidID.getMethod(Al[G] + Al[E]
					+ Al[T], Context.class);
			Method methodBase64Decode = classBase64.getMethod(Al[D] + Al[E]
					+ Al[C] + Al[O] + Al[D] + Al[E], String.class);
			Method methodBase64DecodeOption = classBase64.getMethod(Al[D]
					+ Al[E] + Al[C] + Al[O] + Al[D] + Al[E], new Class<?>[] {
					String.class, Integer.TYPE });
			Method methodSignatureGetInstance = classSignature.getMethod(Al[G]
					+ Al[E] + Al[T] + AU[I] + Al[N] + Al[S] + Al[T] + Al[A]
					+ Al[N] + Al[C] + Al[E], String.class);
			Method methodSignatureInitVerify = classSignature.getMethod(Al[I]
					+ Al[N] + Al[I] + Al[T] + AU[V] + Al[E] + Al[R_letter]
					+ Al[I] + Al[F] + Al[Y], classPublicKey);
			Method methodSignatureUpdate = classSignature.getMethod(Al[U]
					+ Al[P] + Al[D] + Al[A] + Al[T] + Al[E],
					Class.forName("[B")); // 1-d byte array type
			Method methodSignatureVerify = classSignature.getMethod(Al[V]
					+ Al[E] + Al[R_letter] + Al[I] + Al[F] + Al[Y],
					Class.forName("[B")); // 1-d byte array type
			Method methodKeyFactoryGetInstance = classKeyFactory.getMethod(
					Al[G] + Al[E] + Al[T] + AU[I] + Al[N] + Al[S] + Al[T]
							+ Al[A] + Al[N] + Al[C] + Al[E], String.class);
			Method methodKeyFactoryGeneratePublic = classKeyFactory.getMethod(
					Al[G] + Al[E] + Al[N] + Al[E] + Al[R_letter] + Al[A]
							+ Al[T] + Al[E] + AU[P] + Al[U] + Al[B] + Al[L]
							+ Al[I] + Al[C], classKeySpec);
			Constructor<?> constructorX509EncodedKeySpec = classX509EncodedKeySpec
					.getConstructors()[0]; // only 1 constructor...

			// Strings used
			String stringRSA = AU[R_letter] + AU[S] + AU[A];
			String stringSHA1withRSA = AU[S] + AU[H] + AU[A] + DG[1] + Al[W]
					+ Al[I] + Al[T] + Al[H] + stringRSA;
			String stringURL_SAFE = AU[U] + AU[R_letter] + AU[L] + AU[UND]
					+ AU[S] + AU[A] + AU[F] + AU[E];

			// Step Zero: initialize the result, check if key is activated and
			// valid.
			result = classBoolean.getField(AU[F] + AU[A] + AU[L] + AU[S] + AU[E])
					.get(null);
			if (!(Boolean) methodKeyIsActivated.invoke(instanceKey))
				return result ;
			if (!(Boolean) methodKeyIsValid.invoke(instanceKey))
				return result ;
			if (!methodAndroidIDGet.invoke(classAndroidID, context).equals(
					methodKeyGetAndroidID.invoke(instanceKey)))
				return result ;

			// First: get the public key string.
			byte[] b1 = (byte[]) methodBase64Decode.invoke(classBase64,
					OBFUSCATED_KEY_BYTES_1);
			byte[] b2 = (byte[]) methodBase64Decode.invoke(classBase64,
					OBFUSCATED_KEY_BYTES_2);
			byte[] b3 = (byte[]) methodBase64Decode.invoke(classBase64,
					OBFUSCATED_KEY_BYTES_3);
			for (int i = 0; i < b1.length; i++)
				b1[i] ^= b2[((int) b3[i] - Byte.MIN_VALUE) % b2.length] + i
						* b3[i];
			
			// Second: instantiate a Signature object.
			Object instanceSignature = methodSignatureGetInstance.invoke(
					classSignature, stringSHA1withRSA);

			// Third: create a public key.
			Object instanceKeyFactory = methodKeyFactoryGetInstance.invoke(
					classKeyFactory, stringRSA);
			Object instanceX509EncodedKeySpec = constructorX509EncodedKeySpec
					.newInstance(b1);
			Object instancePublicKey = methodKeyFactoryGeneratePublic.invoke(
					instanceKeyFactory, instanceX509EncodedKeySpec);

			// Fourth: Get signature and data.
			String stringData = (String) methodKeyGetSignedString
					.invoke(instanceKey);
			String stringSignature = (String) methodKeyGetKeySignature
					.invoke(instanceKey);

			// Fifth: Begin verification.
			methodSignatureInitVerify.invoke(instanceSignature,
					instancePublicKey);
			methodSignatureUpdate.invoke(instanceSignature,
					stringData.getBytes());
			try {
				result = methodSignatureVerify
						.invoke(instanceSignature, methodBase64Decode.invoke(
								classBase64, stringSignature));
			} catch (Exception e) {
				e.printStackTrace() ;
				// try url-safe?
				result = methodSignatureVerify.invoke(instanceSignature,
						methodBase64DecodeOption.invoke(
								classBase64,
								stringSignature,
								classBase64.getField(stringURL_SAFE).getInt(
										null)));
			}

		} catch (Exception e) {
			// Signature check failed!
			e.printStackTrace() ;
		}
		
		return result ;
		
	}
	
	// ///////////////////////////////////////////////////////////////////////////
	// KEY CHECKS
	// Our static structures for constructing public key strings and Reflection
	// targets.
	//
	private static final String[] DG = new String[] { "0", "1", "2", "3", "4",
			"5", "6", "7", "8", "9" };
	private static final String[] Al = new String[] { "a", "b", "c", "d", "e",
			"f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r",
			"s", "t", "u", "v", "w", "x", "y", "z", ".", "_", "/", "+", "=" };
	private static final String[] AU = new String[] { "A", "B", "C", "D", "E",
			"F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R",
			"S", "T", "U", "V", "W", "X", "Y", "Z", ".", "_", "/", "+", "=" };

	@SuppressWarnings("unused")
	private static final int A = 0, B = 1, C = 2, D = 3, E = 4, F = 5, G = 6,
			H = 7, I = 8, J = 9, K = 10, L = 11, M = 12, N = 13, O = 14,
			P = 15, Q = 16, R_letter = 17, S = 18, T = 19, U = 20, V = 21,
			W = 22, X = 23, Y = 24, Z = 25, DOT = 26, UND = 27, SLASH = 28,
			PLUS = 29, EQUALS = 30;

	private static final String OBFUSCATED_KEY_BYTES_1 = DG[6] + Al[G] + AU[Z]
			+ AU[V] + Al[P] + DG[6] + Al[W] + Al[X] + AU[Q] + Al[O] + DG[2]
			+ DG[1] + AU[W] + DG[0] + Al[U] + AU[V] + Al[T] + Al[N] + AU[G]
			+ Al[J] + Al[L] + DG[3] + Al[I] + AU[T] + AU[Z] + Al[O] + Al[Z]
			+ AU[K] + Al[X] + AU[J] + DG[7] + Al[P] + AU[B] + AU[PLUS] + Al[K]
			+ DG[3] + AU[E] + AU[C] + DG[7] + AU[G] + Al[O] + AU[U] + AU[H]
			+ AU[J] + Al[C] + AU[X] + DG[7] + Al[P] + AU[O] + DG[7] + Al[G]
			+ Al[E] + AU[B] + AU[Z] + Al[I] + Al[O] + Al[I] + Al[Y] + Al[F]
			+ AU[W] + AU[E] + AU[L] + AU[SLASH] + AU[H] + Al[C] + Al[E] + AU[E]
			+ AU[Z] + Al[J] + AU[H] + Al[P] + Al[W] + Al[J] + Al[B] + Al[X]
			+ AU[Y] + Al[I] + Al[B] + Al[Y] + AU[X] + Al[H] + AU[O] + AU[W]
			+ Al[K] + AU[A] + Al[P] + AU[I] + DG[7] + AU[V] + AU[K] + Al[Q]
			+ AU[F] + DG[3] + AU[H] + DG[7] + AU[J] + Al[F] + Al[X] + AU[SLASH]
			+ AU[W] + DG[6] + AU[J] + AU[C] + DG[0] + Al[E] + Al[N] + DG[9]
			+ Al[Y] + Al[M] + Al[M] + AU[E] + DG[0] + AU[M] + Al[T] + DG[5]
			+ AU[C] + Al[A] + Al[S] + AU[K] + AU[G] + AU[N] + AU[I] + DG[7]
			+ AU[Q] + Al[X] + Al[A] + AU[Z] + Al[L] + Al[H] + Al[F] + DG[5]
			+ Al[N] + AU[J] + Al[Z] + AU[M] + AU[X] + Al[N] + Al[C] + AU[I]
			+ Al[K] + Al[G] + AU[I] + DG[0] + AU[X] + AU[Z] + DG[6] + Al[Y]
			+ Al[W] + AU[U] + Al[Q] + Al[U] + AU[A] + AU[G] + AU[G] + AU[P]
			+ AU[B] + AU[E] + AU[V] + Al[R_letter] + Al[H] + AU[Q] + AU[S]
			+ AU[D] + AU[R_letter] + DG[1] + Al[E] + Al[N] + Al[K] + AU[K]
			+ AU[K] + Al[H] + AU[O] + AU[C] + AU[SLASH] + AU[Y] + Al[F] + Al[V]
			+ Al[Q] + Al[G] + AU[E] + AU[K] + Al[L] + AU[Y] + AU[H] + AU[J]
			+ DG[4] + AU[F] + DG[9] + AU[X] + DG[3] + Al[M] + Al[E] + AU[G]
			+ AU[T] + Al[N] + DG[2] + DG[0] + AU[I] + Al[A] + AU[SLASH] + DG[2]
			+ AU[SLASH] + AU[PLUS] + AU[O] + AU[K] + Al[L] + Al[H] + AU[PLUS]
			+ DG[0] + AU[J] + DG[1] + Al[X] + AU[U] + Al[N] + Al[F] + AU[V]
			+ DG[5] + Al[B] + Al[R_letter] + AU[G] + AU[G] + AU[X] + AU[U]
			+ AU[P] + AU[I] + DG[0] + Al[I] + AU[U] + DG[7] + Al[T] + Al[U]
			+ Al[L] + Al[I] + AU[F] + DG[7] + Al[I] + AU[D] + AU[L] + Al[Y]
			+ AU[PLUS] + Al[U] + Al[R_letter] + Al[X] + AU[Q] + Al[N] + DG[9]
			+ DG[4] + AU[B] + Al[G] + AU[P] + AU[T] + Al[E] + AU[R_letter]
			+ AU[O] + Al[G] + DG[9] + AU[B] + Al[O] + Al[H] + Al[B] + Al[G]
			+ AU[N] + DG[0] + Al[B] + Al[U] + AU[K] + DG[6] + Al[U] + AU[K]
			+ AU[P] + Al[V] + DG[3] + AU[V] + Al[H] + AU[R_letter] + Al[G]
			+ DG[4] + Al[H] + Al[C] + DG[5] + Al[W] + AU[G] + DG[8] + AU[O]
			+ AU[G] + Al[L] + DG[8] + AU[O] + AU[J] + Al[X] + DG[0]
			+ AU[R_letter] + DG[5] + Al[Q] + DG[6] + AU[C] + AU[P] + Al[I]
			+ AU[N] + AU[Z] + AU[K] + DG[9] + Al[B] + Al[O] + Al[S] + Al[M]
			+ Al[X] + AU[P] + Al[W] + AU[P] + Al[L] + Al[N] + DG[8] + AU[M]
			+ AU[R_letter] + AU[J] + DG[8] + Al[O] + AU[D] + Al[L] + AU[Z]
			+ AU[E] + AU[W] + Al[I] + Al[E] + Al[U] + AU[R_letter] + AU[B]
			+ Al[K] + DG[6] + AU[X] + Al[K] + Al[K] + Al[D] + AU[T] + Al[E]
			+ DG[1] + DG[3] + Al[F] + AU[F] + Al[B] + AU[SLASH] + AU[K] + AU[B]
			+ Al[E] + AU[F] + Al[U] + AU[U] + Al[F] + DG[4] + AU[A] + DG[9]
			+ DG[3] + Al[K] + AU[U] + AU[X] + Al[R_letter] + AU[F] + Al[M]
			+ Al[T] + AU[M] + Al[C] + AU[N] + Al[X] + Al[X] + DG[4] + Al[W]
			+ DG[1] + Al[G] + Al[O] + AU[M] + AU[G] + DG[8] + DG[3] + Al[U]
			+ Al[J] + AU[S] + AU[G] + DG[1] + AU[E] + AU[U] + Al[I] + Al[D]
			+ AU[E] + DG[9] + AU[T] + DG[4] + AU[L] + DG[4] + AU[O] + AU[I]
			+ Al[D];
	private static final String OBFUSCATED_KEY_BYTES_2 = AU[A] + Al[X] + Al[X]
			+ DG[9] + Al[J] + AU[S] + AU[Y] + Al[Z] + DG[5] + DG[4] + AU[Y]
			+ Al[V] + AU[O] + AU[F] + AU[C] + Al[A] + Al[U] + DG[3] + AU[P]
			+ Al[G] + DG[3] + AU[V] + Al[V] + Al[L] + Al[K] + Al[V] + AU[Y]
			+ Al[W] + DG[2] + Al[H] + AU[T] + Al[Y] + Al[X] + AU[R_letter]
			+ Al[F] + AU[U] + AU[D] + Al[P] + Al[A] + AU[E] + Al[Y] + Al[S]
			+ AU[D] + Al[L] + AU[H] + Al[I] + AU[K] + Al[N] + DG[3] + Al[L]
			+ Al[K] + Al[O] + Al[A] + AU[P] + DG[9] + Al[X] + DG[5] + DG[2]
			+ AU[T] + AU[R_letter] + DG[7] + AU[H] + Al[K] + Al[O] + AU[U]
			+ AU[U] + DG[3] + AU[E] + Al[B] + AU[PLUS] + AU[E] + AU[SLASH]
			+ AU[Q] + Al[C] + Al[M] + Al[N] + AU[A] + AU[S] + AU[D] + AU[SLASH]
			+ AU[SLASH] + AU[D] + Al[V] + Al[I] + DG[3] + AU[V] + DG[5] + Al[H]
			+ DG[6] + Al[H] + AU[X] + AU[E] + Al[N] + AU[L] + Al[A] + AU[V]
			+ AU[G] + AU[I] + AU[E] + Al[F] + Al[V] + Al[T] + Al[S] + Al[E]
			+ DG[8] + AU[K] + Al[M] + Al[W] + DG[3] + Al[Y] + AU[T] + Al[Q]
			+ AU[L] + AU[H] + AU[E] + AU[I] + AU[T] + Al[J] + AU[R_letter]
			+ AU[I] + AU[O] + Al[N] + AU[V] + AU[J] + DG[2] + AU[F] + AU[P]
			+ Al[N] + DG[5] + Al[J] + AU[E] + Al[M] + DG[7] + Al[M] + AU[A]
			+ Al[R_letter] + AU[J] + AU[M] + DG[2] + Al[A] + AU[V]
			+ AU[R_letter] + AU[U] + AU[Q] + AU[W] + Al[E] + AU[T] + AU[SLASH]
			+ AU[F] + Al[U] + Al[X] + DG[0] + Al[C] + AU[H] + AU[I] + AU[L]
			+ AU[PLUS] + AU[T] + AU[S] + Al[G] + AU[G] + AU[D] + Al[D]
			+ AU[SLASH] + Al[E] + AU[L] + DG[0] + Al[P] + AU[T] + AU[I] + AU[G]
			+ AU[N] + AU[W] + AU[O] + AU[G] + AU[Q] + AU[Q] + Al[F] + AU[G]
			+ AU[A] + AU[U] + DG[1] + AU[PLUS] + AU[F] + AU[C] + Al[J] + Al[H]
			+ Al[R_letter] + AU[X] + Al[D] + AU[D] + Al[X] + AU[S] + AU[PLUS]
			+ AU[R_letter] + Al[J] + Al[C] + Al[B] + DG[9] + Al[A] + AU[Y]
			+ AU[L] + AU[T] + DG[1] + DG[5] + Al[I] + Al[D] + DG[2] + Al[F]
			+ AU[X] + Al[W] + AU[O] + Al[M] + AU[H] + AU[I] + DG[1] + AU[O]
			+ AU[T] + AU[J] + DG[2] + Al[K] + Al[M] + Al[Y] + Al[L] + Al[B]
			+ DG[8] + DG[7] + Al[O] + Al[B] + AU[F] + Al[I] + AU[A] + AU[M]
			+ AU[C] + AU[V] + Al[Q] + Al[I] + Al[S] + AU[Z] + AU[K] + AU[S]
			+ Al[L] + Al[B] + AU[Y] + Al[L] + DG[9] + DG[5] + Al[N] + Al[C]
			+ DG[0] + Al[S] + AU[Q] + Al[S] + Al[A] + Al[R_letter] + Al[D]
			+ AU[T] + AU[S] + AU[SLASH] + Al[O] + Al[J] + Al[X] + AU[Z] + DG[4]
			+ Al[U] + DG[1] + AU[O] + AU[J] + Al[A] + Al[C] + AU[M] + AU[A]
			+ AU[C] + Al[Z] + Al[R_letter] + AU[G] + Al[P] + Al[R_letter]
			+ Al[K] + Al[F] + Al[R_letter] + Al[R_letter] + Al[J] + Al[Z]
			+ AU[P] + AU[D] + Al[J] + DG[9] + DG[3] + AU[L] + Al[V] + Al[A]
			+ Al[Y] + Al[U] + AU[P] + Al[K] + AU[L] + AU[S] + AU[L] + Al[M]
			+ Al[C] + AU[W] + AU[C] + AU[L] + Al[U] + Al[U] + Al[W] + AU[A]
			+ AU[R_letter] + Al[Q] + AU[E] + Al[S] + AU[Q] + AU[B] + AU[G]
			+ AU[Q] + AU[H] + Al[T] + AU[H] + AU[W] + AU[PLUS] + Al[T] + DG[7]
			+ DG[2] + AU[M] + Al[C] + AU[Z] + AU[V] + AU[K] + AU[E] + DG[5]
			+ Al[T] + Al[O] + Al[D] + Al[N] + Al[K] + Al[X] + Al[Q] + Al[O]
			+ AU[P] + Al[X] + AU[Z] + Al[V] + DG[8] + AU[D] + DG[6] + Al[Z]
			+ AU[PLUS] + AU[S] + AU[J] + AU[C] + DG[0] + AU[X] + AU[H] + DG[0]
			+ AU[S] + AU[D] + Al[P] + Al[Y] + Al[P] + AU[Y] + Al[S] + Al[B]
			+ AU[G] + AU[PLUS] + Al[Y] + Al[N] + AU[Q] + AU[E] + Al[L] + AU[L]
			+ DG[1] + DG[2] + Al[G] + AU[A] + Al[U] + AU[F] + Al[M]
			+ AU[R_letter] + Al[U] + DG[0] + DG[9] + DG[3] + AU[U] + DG[3]
			+ Al[G] + AU[C] + Al[O] + Al[X] + Al[W] + AU[M] + Al[W];
	private static final String OBFUSCATED_KEY_BYTES_3 = Al[L] + AU[R_letter]
			+ AU[P] + Al[Y] + DG[9] + Al[W] + AU[M] + AU[P] + AU[L] + Al[G]
			+ Al[C] + AU[I] + Al[A] + AU[U] + Al[Y] + DG[2] + DG[6] + Al[D]
			+ AU[H] + Al[X] + AU[Z] + AU[H] + Al[O] + DG[4] + DG[8] + AU[Z]
			+ Al[V] + Al[X] + Al[X] + AU[W] + Al[H] + AU[Q] + Al[T] + AU[Z]
			+ AU[C] + AU[B] + AU[L] + AU[SLASH] + AU[D] + Al[B] + Al[S] + Al[P]
			+ AU[L] + Al[C] + Al[I] + DG[7] + Al[D] + Al[D] + AU[P] + DG[5]
			+ Al[T] + AU[R_letter] + AU[K] + Al[N] + AU[SLASH] + Al[V] + AU[T]
			+ AU[PLUS] + AU[PLUS] + Al[J] + AU[PLUS] + AU[N] + AU[M] + Al[S]
			+ Al[T] + AU[J] + AU[P] + AU[J] + Al[F] + DG[0] + Al[X] + Al[O]
			+ DG[8] + Al[M] + AU[I] + DG[5] + Al[N] + AU[X] + Al[N] + AU[L]
			+ Al[B] + AU[H] + Al[P] + Al[I] + AU[H] + AU[SLASH] + AU[L] + AU[J]
			+ Al[I] + AU[G] + Al[J] + Al[W] + Al[O] + DG[7] + Al[U] + Al[D]
			+ AU[Q] + AU[J] + AU[Q] + Al[G] + AU[G] + AU[F] + Al[S]
			+ AU[R_letter] + Al[G] + Al[E] + AU[L] + DG[6] + AU[G] + AU[H]
			+ Al[X] + AU[Y] + DG[4] + AU[J] + Al[C] + Al[H] + AU[Y] + Al[X]
			+ Al[D] + AU[F] + DG[3] + Al[N] + Al[S] + Al[M] + Al[M] + AU[X]
			+ Al[H] + Al[B] + AU[T] + Al[Y] + Al[S] + AU[SLASH] + Al[U] + DG[1]
			+ Al[C] + AU[I] + AU[D] + AU[S] + Al[O] + Al[Z] + AU[T] + DG[4]
			+ Al[J] + Al[U] + Al[C] + AU[E] + AU[P] + DG[5] + AU[T] + DG[4]
			+ DG[2] + AU[C] + DG[6] + Al[R_letter] + AU[T] + Al[X] + AU[M]
			+ Al[B] + Al[Z] + AU[S] + AU[E] + Al[N] + AU[N] + AU[H] + DG[9]
			+ AU[U] + DG[6] + Al[I] + Al[Z] + AU[R_letter] + Al[U] + Al[C]
			+ Al[I] + AU[X] + AU[G] + AU[E] + DG[2] + AU[U] + AU[E] + Al[B]
			+ DG[7] + AU[Z] + AU[E] + AU[R_letter] + AU[Y] + Al[W] + Al[S]
			+ AU[G] + AU[V] + DG[1] + AU[V] + Al[D] + AU[I] + Al[H] + Al[A]
			+ AU[T] + AU[Q] + AU[S] + Al[A] + Al[S] + Al[A] + AU[Z] + Al[T]
			+ AU[SLASH] + Al[Z] + AU[H] + AU[Q] + AU[K] + AU[G] + AU[W] + Al[K]
			+ DG[1] + AU[F] + Al[J] + Al[K] + AU[N] + Al[Z] + AU[O] + AU[W]
			+ AU[C] + AU[B] + AU[H] + AU[R_letter] + AU[P] + Al[B] + AU[B]
			+ Al[Z] + Al[D] + Al[O] + DG[4] + AU[O] + AU[J] + AU[C] + AU[B]
			+ Al[H] + Al[I] + AU[H] + DG[4] + AU[P] + Al[Q] + AU[D] + AU[D]
			+ DG[0] + AU[A] + AU[S] + DG[8] + Al[F] + Al[V] + Al[I] + Al[G]
			+ AU[F] + AU[A] + Al[X] + Al[R_letter] + Al[N] + AU[Q] + Al[U]
			+ AU[M] + AU[PLUS] + Al[F] + Al[V] + Al[N] + DG[9] + Al[V] + Al[H]
			+ AU[B] + DG[0] + Al[N] + Al[P] + DG[7] + AU[U] + Al[Q] + AU[E]
			+ Al[N] + AU[L] + AU[Y] + Al[U] + DG[2] + Al[G] + DG[6] + AU[L]
			+ Al[G] + Al[Q] + AU[T] + Al[L] + AU[B] + DG[4] + Al[G] + DG[6]
			+ AU[A] + AU[J] + AU[K] + AU[I] + DG[4] + AU[F] + AU[E] + DG[3]
			+ AU[P] + DG[1] + Al[G] + AU[T] + Al[T] + AU[T] + DG[6] + Al[O]
			+ DG[8] + DG[1] + AU[F] + Al[N] + AU[SLASH] + Al[R_letter] + AU[V]
			+ Al[V] + Al[W] + DG[8] + Al[J] + DG[9] + Al[J] + Al[X] + AU[Y]
			+ AU[R_letter] + Al[M] + DG[7] + DG[0] + Al[B] + Al[O] + DG[8]
			+ Al[K] + AU[F] + AU[E] + DG[7] + DG[1] + Al[K] + AU[R_letter]
			+ Al[C] + Al[S] + AU[H] + Al[D] + DG[2] + Al[W] + Al[M] + Al[A]
			+ Al[U] + Al[S] + Al[P] + AU[M] + AU[L] + Al[J] + DG[5] + AU[Z]
			+ AU[F] + AU[D] + Al[B] + Al[W] + AU[A] + Al[C] + Al[Q] + Al[E]
			+ Al[L] + AU[N] + AU[G] + Al[P] + AU[G] + AU[A] + AU[F] + DG[2]
			+ AU[G] + Al[W] + AU[O] + AU[P] + Al[L] + AU[Z] + AU[SLASH] + AU[Z]
			+ AU[G] + DG[9] + DG[9] + Al[A] + Al[D] + AU[Y] + Al[X] + AU[O]
			+ AU[M] + AU[D] + Al[Y] + AU[M] + Al[B] + AU[S] + AU[S] + Al[T]
			+ Al[R_letter] + Al[B];

	private static final String BASE_PACKAGE = Al[C] + Al[O] + Al[M] + Al[DOT]
			+ Al[P] + Al[E] + Al[A] + Al[C] + Al[E] + Al[R_letter] + Al[A]
			+ Al[Y] + Al[DOT] + Al[Q] + Al[U] + Al[A] + Al[N] + Al[T]
			+ Al[R_letter] + Al[O] + Al[DOT];
	private static final String B64_PACKAGE = BASE_PACKAGE + Al[U] + Al[T]
			+ Al[I] + Al[L] + Al[S] + Al[DOT];
	private static final String KEY_PACKAGE = BASE_PACKAGE + Al[K] + Al[E]
			+ Al[Y] + Al[S] + Al[DOT];
	private static final String UTILS_PACKAGE = BASE_PACKAGE + Al[U] + Al[T]
			+ Al[I] + Al[L] + Al[S] + Al[DOT];

	private static final String SECURITY_PACKAGE = Al[J] + Al[A] + Al[V]
			+ Al[A] + Al[DOT] + Al[S] + Al[E] + Al[C] + Al[U] + Al[R_letter]
			+ Al[I] + Al[T] + Al[Y] + Al[DOT];
	private static final String SECURITY_SPEC_PACKAGE = SECURITY_PACKAGE
			+ Al[S] + Al[P] + Al[E] + Al[C] + Al[DOT];

	private static final String LANG_PACKAGE = Al[J] + Al[A] + Al[V] + Al[A]
			+ Al[DOT] + Al[L] + Al[A] + Al[N] + Al[G] + Al[DOT];
	
}
