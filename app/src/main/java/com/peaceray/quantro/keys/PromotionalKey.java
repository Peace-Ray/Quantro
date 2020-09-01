package com.peaceray.quantro.keys;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import com.peaceray.quantro.communications.web.WebQuery;
import com.peaceray.quantro.premium.PremiumSKU;



/**
 * PromotionalKey, like all keys, can self-modify through the use of Internet
 * access.  However, manual changes made to a key should occur through a Builder;
 * they are effectively immutable once created (apart from - again - Internet-based
 * modification).
 * 
 * Premium keys have a set of inner information, including (once activated) a tag and
 * a server signature.  The signature represents the following key:
 * 		values.put(Updater.VAR_CODE, mKey) ;
		values.put(Updater.VAR_ITEM, mItem) ;
		values.put(Updater.VAR_TAG, mKeyTag) ;
		values.put(Updater.VAR_PROMO, mKeyPromo) ;
		values.put(Updater.VAR_ANDROID_ID, mAndroidID) ;
 * 
 * "android_id:<id>
 * code:<premium key value>
 * item:<item content name>
 * promo:<optional promo string>
 * tag:<tag>"
 * 
 * With exactly that formatting, including 1 newline character (removing quotes
 * and pointy brackets).  Note that keys are typically reported in a "wrapped"
 * format, e.g. "[code||_____________]."  This wrapper is NOT included in the signature.
 * 
 * @author Jake
 *
 */
public class PromotionalKey extends Key implements Cloneable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6441374631837610271L;
	
	
	// override!
	protected static final String KEY_PREFIX = "PROMO:" ;
	
	protected static final String SKU_SEPARATOR = "," ;
	protected static final String SKU_ALL = "all" ;
	
	protected String keyPrefix() {
		return KEY_PREFIX ;
	}
	
	
	/**
	 * Returns whether the provided string resembles a key value, a decision
	 * which may be as harsh or liberal as you wish.
	 * 
	 * For instance, a string "resembles" a PremiumKey key value if its
	 * content is base-64, it is optionally prefixed by "PREMIUM_CODE:", and optionally
	 * bracket in "[*]".
	 * 
	 * It is possible for some strings to "Resemble" more than one
	 * Key type.  For a more strict requirement, use "isKey."
	 * 
	 * @param keyValue
	 * @return
	 */
	public static boolean resemblesKeyStatic( String keyValue ) {
		return Key.resemblesKey(keyValue, KEY_PREFIX) ;
	}
	
	public boolean resemblesKey( String keyValue ) {
		return Key.resemblesKey(keyValue, KEY_PREFIX) ;
	}
	
	public static boolean isKeyStatic( String keyValue ) {
		return Key.isKey(keyValue, KEY_PREFIX) ;
	}
	
	public boolean isKey( String keyValue ) {
		return Key.isKey(keyValue, KEY_PREFIX) ;
	}
	
	public String stripKey( String keyValue ) {
		return Key.stripKey(keyValue, KEY_PREFIX) ;
	}
	
	public String wrapKey( String keyValue ) {
		return Key.wrapKey(keyValue, KEY_PREFIX) ;
	}
	
	
	/**
	 * A PromotionalKey.Updater serves to communicate with quantro_xl_web
	 * regarding the key information.  PromotionalKey themselves are immutable,
	 * so instances of this class are instantiated with an existing key
	 * (which must have either mKey or mJSON) and, should any changes be
	 * made according to server responses, a new key generated using
	 * getKey().
	 * 
	 * A note on verification: there are three signatures worthy of note here.
	 * 
	 * 1. The JSON signature provided by Google for a purchase.
	 * 2. The https signing Certificate used in web communication
	 * 3. The SHA1-RSA signature applied by quantro_xl_web to a key value, tag, and android_id
	 * 		during activation.
	 * 
	 * Of these three, the Updater will only concern itself with #2.  You can
	 * 		verify the JSON signature #1 yourself before making the Key object
	 * 		(the quantro_xl_web script will do this too).  As a matter of good
	 * 		practice, you should also signature check the updated key #3 BEFORE
	 * 		committing it to any permanent storage.
	 * 
	 * 		Remember that PromotionalKeys are immutable, and thus Updater will not
	 * 		make any in-place changes to the key provided at construction.  It
	 * 		is safe to provide a key, update[..]() it a few times, call key = getKey(),
	 * 		determine the quantro_xl_signature is invalid and then discard the
	 * 		key using key = null.
	 * 
	 * @author Jake
	 *
	 */
	public static class Updater extends Key.Updater {
		
		// The URL of our cgi-script
		private static final String QUERY_URL = "https://secure.peaceray.com/cgi-bin/quantro_xl_web" ;
		
		private static final String VAR_SKU = "sku" ;
		private static final String VAR_CODE = "code" ;
		
		
		private ArrayList<String> RESPONSE_VARS ;
		private Hashtable<String, Object> RESPONSE_ANSWER ;
		private Hashtable<String, Object> RESPONSE_PROBLEM ;
		
		
		protected String varKey() {
			return VAR_CODE ;
		}
		protected String queryURL() {
			return QUERY_URL ;
		}
		
		
		protected Updater( PromotionalKey key ) {
			super(key) ;
			if ( key == null )
				throw new NullPointerException("Provided PromotionalKey is null") ;
			
			RESPONSE_VARS = new ArrayList<String>() ;
			RESPONSE_VARS.add(VAR_SIGNATURE) ;
			RESPONSE_VARS.add(VAR_TAG) ;
			RESPONSE_VARS.add(VAR_SKU) ;
			RESPONSE_VARS.add(VAR_CODE) ;
			RESPONSE_VARS.add(VAR_PROMO) ;
			RESPONSE_VARS.add(VAR_ANDROID_ID) ;
			RESPONSE_VARS.add(VAR_WAIT) ;
			RESPONSE_VARS.add(VAR_ANSWER) ;
			RESPONSE_VARS.add(VAR_PROBLEM) ;
			
			RESPONSE_ANSWER = new Hashtable<String, Object>() ;
			RESPONSE_ANSWER.put(ANSWER_YES, Boolean.TRUE) ;
			RESPONSE_ANSWER.put(ANSWER_NO, Boolean.FALSE) ;		
			
			RESPONSE_PROBLEM = new Hashtable<String, Object>() ;
			RESPONSE_PROBLEM.put(PROBLEM_INVALID, PROBLEM_INVALID) ;
			RESPONSE_PROBLEM.put(PROBLEM_NOT_AVAILABLE, PROBLEM_NOT_AVAILABLE) ;
			RESPONSE_PROBLEM.put(PROBLEM_ISSUED, PROBLEM_ISSUED) ;
		}
		
		protected void addSubclassResponseVars( ArrayList<String> vars ) {
			// Two response vars are unique to this subclass: SKU and CODE.
			if ( !vars.contains(VAR_SKU) )
				vars.add(VAR_SKU) ;
			if ( !vars.contains(VAR_CODE) )
				vars.add(VAR_CODE) ;
		}
		
		
		protected void updateSubclassActivation( int timeout ) {
			// nothing
		}
		
		protected void updateActivationRegisterPutSubclassVariables( Hashtable<String, Object> args) {
			// nothing
		}
		
		protected void updateActivationRegisterSetSubclassResponseVariables( WebQuery.Response response, Key.Builder builder ) throws IOException {
			String skuList = response.getResponseVariableString(VAR_SKU) ;
			((PromotionalKey.Builder)builder).setSKUList(skuList) ;
		}
		
		protected void updateActivationActivatePutSubclassVariables( Hashtable<String, Object> args) {
			// we need to include SKU for this.
			args.put(VAR_SKU, ((PromotionalKey)mTemplate).mSKUList) ;
		}
	}
	
	
	public static class Builder extends Key.Builder {
		
		/**
		 * Returns 'true' if the provided value is an instance 
		 * of the subclass.
		 * 
		 * Subclasses are required to override this method.
		 * 
		 * @param k
		 * @return
		 */
		protected boolean isClassInstance( Key k ) {
			return k == null || k instanceof PromotionalKey ;
		}
		
		/**
		 * A wrapper method for "clone" which first checks the fields of
		 * the provided key, and if those fields descibe a valid state for
		 * a key object (i.e., one in which "updateActivation" and 
		 * "updateValidity" are reasonable calls to make), returns a 
		 * clone of said object.
		 * 
		 * If 'null' is returned, we assume the key was not buildable
		 * by the definition described above.  However, overriding
		 * implementations may prefer to throw an IllegalStateException
		 * instead.  This allows customized error messages to be included.
		 * 
		 * This method is called in build(), and if 'null' is returned,
		 * a generic IllegalStateException will be thrown.  It is thus
		 * in the interest of debugging that an exception be thrown
		 * instead of 'null' returning.
		 * 
		 * @param k
		 * @return
		 */
		protected Key cloneIfBuildable ( Key k ) throws IllegalStateException, CloneNotSupportedException {
			// An XL Key is buildable if the following are both true:
			
			// It has a JSON string -or- a key value
			// It has an Android ID.
			
			if ( k.mKey == null )
				throw new IllegalStateException("Have not set the key value; cannot build.") ;
			if ( k.mAndroidID == null )
				throw new IllegalStateException("Have not set the android device ID") ;
			if ( k.isActivated() && ((PromotionalKey)k).mSKUList == null )
				throw new IllegalStateException("Activated, but lacks a SKU list.") ;
			if ( k.getKeySignature() != null && ((PromotionalKey)k).mSKUList == null )
				throw new IllegalStateException("Signed, but lacks a SKU list.") ;
			try {
				Key newK = (Key) ((PromotionalKey) k).clone() ;
				return newK ;
			} catch (CloneNotSupportedException e) {
				throw new IllegalStateException("Cannot successfully clone key " + k) ;
			}
		}
		
		
		/**
		 * Constructs and returns a new, empty "Template" (Key) to use when
		 * Building the rest.  clone() will be used in build().
		 * @return
		 */
		protected Key newClassInstance() {
			return new PromotionalKey() ;
		}
		
		
		
		public Builder() {
			super() ;
		}
		
		protected Builder( PromotionalKey key ) {
			super((Key)key) ;
		}
		
		
		/**
		 * Any non-null value in the provided key is copied into the key
		 * being built.  Any null value is ignored and the previously set value
		 * retained.
		 * 
		 * NOTE: This is true even for 'activated,' which is stored as a Boolean
		 * (object) not a boolean (type).  Set it to 'null' if you don't want
		 * to force an update.
		 * 
		 * @param key
		 * @return
		 */
		public Builder update( Key keyArg ) {
			if ( !(keyArg instanceof PromotionalKey) )
				throw new IllegalArgumentException("Instances of QuantroXLKey.Builder may only be updated with QuantroXLKey instances") ;
			
			PromotionalKey template = (PromotionalKey)mTemplate ;
			PromotionalKey key = (PromotionalKey)keyArg ;
			
			if ( key.mSKUList != null ) {
				template.mSKUList = key.mSKUList ;
				template.mSKUs = key.mSKUs.clone() ;
			}
			
			return (PromotionalKey.Builder) super.update(keyArg) ;
		}
		
		public Builder setSKUList( String skuList ) {
			PromotionalKey template = (PromotionalKey)mTemplate ;
			template.mSKUList = skuList ;
			if ( SKU_ALL.equals(skuList) )
				template.mSKUs = new String[] { SKU_ALL } ;
			else
				template.mSKUs = skuList == null ? null : skuList.split(SKU_SEPARATOR) ;
			
			return this ;
		}
	}
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// INSTANCE VARS
	//
	private String mSKUList ;
	private String [] mSKUs ;
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	protected PromotionalKey() {
		setAs(null) ;
	}
	
	
	protected PromotionalKey( PromotionalKey key ) {
		setAs(key) ;
	}
	
	@Override
	protected void setAs( Key key ) {
		if ( key == null ) {
			mSKUList = null ;
			mSKUs = null ;
		} else if ( key instanceof PromotionalKey ) {
			mSKUList = ((PromotionalKey)key).mSKUList ;
			mSKUs = ((PromotionalKey)key).mSKUs.clone() ;
		} else {
			throw new IllegalArgumentException("Can't set as an object other than a PromotionalKey.") ;
		}
		
		super.setAs(key) ;
	}


	@Override
	public Builder newBuilder() {
		return new PromotionalKey.Builder(this) ;
	}
	
	
	@Override
	public Updater newUpdater() {
		return new PromotionalKey.Updater(this) ;
	}
	
	
	@Override
	protected Key newDuplicateInstance() {
		return new PromotionalKey(this) ;
	}
	
	
	/**
	 * Returns, as a String, the string representing the premium
	 * content item for which this acts as a key.
	 * @return
	 */
	public String [] getSKUs() {
		if ( SKU_ALL.equals(mSKUList) )
			return PremiumSKU.ALL.clone() ;
		if ( mSKUs == null )
			return null ;
		return mSKUs.clone() ;
	}
	
	
	public boolean getHasSKU( String sku ) {
		if ( SKU_ALL.equals( mSKUList ) )
			return PremiumSKU.isSKU(sku) ;
		if ( mSKUs == null )
			return false ;
		for ( int i = 0; i < mSKUs.length; i++ ) {
			if ( mSKUs[i].equals(sku) ) {
				return true ;
			}
		}
		return false ;
	}
	
	
	/**
	 * Alias to getKey().
	 * 
	 * Returns the unique and permanent Base64 string representing this particular
	 * content access.
	 * 
	 * @return
	 */
	public String getCode() {
		return getKey() ;
	}
	
	
	
	/**
	 * Keys are signed by the QuantroXL server as a list of variables;
	 * specifically, a key is signed with its value (stripped), tag and androidID.
	 * 
	 * Verification requires these values with these formats, as well as
	 * their variable names (known to the static inner class Updater).
	 * 
	 * We leave actual verification to someone else.
	 * 
	 * @return
	 */
	public Map<String, String> getSignedMap() {
		if ( mKey == null || mKeyTag == null || mAndroidID == null )
			throw new IllegalStateException("Not enough non-null values to produce a signed key table") ;
		
		if ( mKeySignature == null )
			throw new IllegalStateException("Signature is not set.") ;
		
		Hashtable<String, String> values = new Hashtable<String, String> () ;
		
		values.put(Updater.VAR_CODE, mKey) ;
		values.put(Updater.VAR_SKU, mSKUList) ;
		values.put(Updater.VAR_TAG, mKeyTag) ;
		values.put(Updater.VAR_PROMO, mKeyPromo) ;
		values.put(Updater.VAR_ANDROID_ID, mAndroidID) ;
		return values ;
	}
	
}
