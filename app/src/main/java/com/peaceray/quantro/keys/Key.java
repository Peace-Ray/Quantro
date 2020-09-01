package com.peaceray.quantro.keys;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.Context;

import com.peaceray.quantro.communications.web.WebQuery;
import com.peaceray.quantro.utils.AndroidID;
import com.peaceray.quantro.utils.Base64;


/**
 * An abstract class representing a key value provided by the Quantro
 * server.  Keys unlocks specific features, represented by subclasses;
 * for example, XL keys unlock QuantroXL features (no ads, removing title
 * and notification bars), MP keys unlock Internet multiplayer gameplay.
 * 
 * This abstract class was not explicitly designed.  QuantroXLKey was
 * written first, and was fully-functional (apparently) at the time
 * that this class was begun.  Methods and member vars will be moved
 * to this class as it becomes clear that QuantroMPKey needs them.
 * 
 * Keys have (at least) two update methods - updateActivation and updateValidity.
 * updateValidity is a method to determine whether the key value is still
 * a valid one on the Quantro servers; updateActivation attempts to use
 * server communication to move the key towards a final activation state.
 * 
 * In the latter case, we use this to progress from (e.g.) a purchase 
 * JSON string, to a key value (b-64 string) to an activated, tagged
 * key value which is signed by the server along with the device ID.
 * 
 * Every Activated key will have the following, all represented as
 * String objects: a "key string," an activation tag, the device Android
 * ID, and a server signature that validates at least all the other
 * strings.  The standard method for this is a "signed list", where the
 * strings are represented in a newline-separated format with format
 * <varname>:<value>, such as "android_id:crha8guTEHni8".  Because different
 * keys may use different variable names ("mp:" for MPKeys, "xl:" for xl keys),
 * and even include new variable names, subclasses are required to at 
 * least implement "getSignedMap" even if they do not override getSignedString.
 * 
 */
public abstract class Key implements Cloneable, Serializable {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6986951465126710777L;
	
	
	// Optional brackets that may or may not be placed around the key.
	protected static final String OPEN_BRACKET = "[" ;
	protected static final String CLOSE_BRACKET = "]" ;
	
	// OVERRIDE THESE.
	protected static final int MIN_KEY_BYTES = 4 ;
	
	protected abstract String keyPrefix() ;
	
	/**
	 * Returns whether the provided string resembles a key value, a decision
	 * which may be as harsh or liberal as you wish.
	 * 
	 * For instance, a string "resembles" a QuantroXL key value if its
	 * content is base-64, it is optionally prefixed by "XL:", and optionally
	 * bracket in "[*]".
	 * 
	 * It is possible for some strings to "Resemble" more than one
	 * Key type.  For a more strict requirement, use "isKey."
	 * 
	 * @param keyValue
	 * @return
	 */
	protected static boolean resemblesKey( String keyValue, String keyPrefix ) {
		if ( keyPrefix == null )
			throw new IllegalStateException("Either called on base-class Key, or subclass did not override KEY_PREFIX.") ;
		String str = stripKey( keyValue, keyPrefix ) ;
		if ( str == null ) 
			return false ;
		
		if ( str.length() < 4 )
			return false ;
			
		// is this only base 64 characters?
		try {
			byte [] b = Base64.decode(str, Base64.URL_SAFE) ;
			if ( b.length < MIN_KEY_BYTES )
				return false ;
		} catch( IOException e ) {
			return false ;
		}
		
		return true ;
	}
	
	protected abstract boolean resemblesKey( String keyValue ) ;
	
	
	/**
	 * Returns whether the provided string is a key value.  Unlike
	 * 'resembles key,' this method should be implemented with care
	 * such that, if one subclass returns 'true', no other subclasses
	 * would (obviously this means that subclass design must be
	 * at least partially aware of what other subclasses may exist).
	 * 
	 * For instance, a string "must be" a QuantroXL key value if it has
	 * this format: "[XL:*]".
	 * 
	 * It is possible for some strings to "Resemble" more than one
	 * Key type.  For a more stricter requirement, use "mustBeKey."
	 * 
	 * @param keyValue
	 * @return
	 */
	protected static boolean isKey( String keyValue, String keyPrefix ) {
		if ( keyPrefix == null )
			throw new IllegalStateException("Either called on base-class Key, or subclass did not override KEY_PREFIX.") ;
		keyValue = keyValue.trim() ;
		// Remove brackets.
		if ( keyValue.indexOf(OPEN_BRACKET) != 0
				|| keyValue.lastIndexOf(CLOSE_BRACKET) != keyValue.length() - CLOSE_BRACKET.length() )
			return false ;
		keyValue = keyValue
				.substring(OPEN_BRACKET.length(), keyValue.length() - CLOSE_BRACKET.length())
				.trim() ;
		
		if ( keyValue.indexOf(keyPrefix) != 0 )
			return false ;
		keyValue = keyValue.substring(keyPrefix.length()).trim() ;
		
		if ( keyValue.length() < 4 )		// minimum length of b64 string.
			return false ;
			
		// is this only base 64 characters?
		try {
			byte [] b = Base64.decode(keyValue, Base64.URL_SAFE) ;
			if ( b.length < MIN_KEY_BYTES )
				return false ;
		} catch( IOException e ) {
			e.printStackTrace() ;
			return false ;
		}
		
		return true ;
	}
	
	public abstract boolean isKey( String keyValue ) ;
	
	protected static String stripKey( String keyValue, String prefix ) {
		if ( prefix == null )
			throw new IllegalStateException("Either called on base-class Key, or subclass did not override KEY_PREFIX.") ;
		if ( keyValue == null )
			return null ;
		String str = keyValue.trim() ;
		// brackets?
		boolean hasBracket = false ;
		if ( str.indexOf(OPEN_BRACKET) == 0 ) {
			str = str.substring(1) ;
			hasBracket = true ;
		}
		if ( str.length() > 0 && str.indexOf(CLOSE_BRACKET) == str.length()-1 ) {
			str = str.substring(0, str.length()-1) ;
			if ( !hasBracket )
				return null ;
		} else if ( hasBracket ) {
			return null ;
		}
		str = str.trim() ;
		
		// XL prefix?
		if ( str.indexOf(prefix) == 0 )
			str = str.substring(prefix.length()) ;
		str = str.trim() ;
		
		return str ;
	}
	
	public abstract String stripKey( String keyValue ) ;
	
	protected static String wrapKey( String keyValue, String keyPrefix ) {
		if ( keyPrefix == null )
			throw new IllegalStateException("Either called on base-class Key, or subclass did not override keyPrefix().") ;
		if ( keyValue == null )
			return null ;
		
		// just in case...
		keyValue = stripKey( keyValue, keyPrefix ) ;
		
		// Now add extra crap.
		return OPEN_BRACKET + keyPrefix + keyValue + CLOSE_BRACKET ;
	}
	
	public abstract String wrapKey( String keyValue ) ;
	
	
	public static abstract class Builder {
		protected Key mTemplate ;
		
		/**
		 * Returns 'true' if the provided value is an instance 
		 * of the subclass.
		 * 
		 * Subclasses are required to override this method.
		 * 
		 * @param k
		 * @return
		 */
		protected abstract boolean isClassInstance( Key k ) ;
		
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
		 * This method will be called only with an object returned
		 * by newClassInstance, or the clone of one which passed the
		 * isClassInstance() check.
		 * 
		 * @param k
		 * @return
		 */
		protected abstract Key cloneIfBuildable ( Key k ) throws IllegalStateException, CloneNotSupportedException ;
		
		
		/**
		 * Constructs and returns a new, empty "Template" (Key) to use when
		 * Building the rest.  clone() will be used in build().
		 * @return
		 */
		protected abstract Key newClassInstance() ;
		
		
		public Builder() {
			mTemplate = newClassInstance() ;
		}
		
		protected Builder( Key key ) {
			if ( !isClassInstance(key) )
				throw new IllegalArgumentException("Provided key is not an instance of the specified subclass: " + key) ;
			if ( key == null ) {
				mTemplate = newClassInstance() ;
			} else {
				try {
					mTemplate = cloneIfBuildable(key) ;
				} catch (CloneNotSupportedException e) {
					throw new IllegalArgumentException("Cannot successfully clone provided key " + key) ;
				}
			}
		}
		
		
		/**
		 * A Key cannot be built unless a certain set of values have been
		 * configured.  Obviously, if this Builder was constructed using an
		 * existing key, it is necessarily the case that we have all the values
		 * needed.
		 * 
		 * @return
		 */
		public Key build() {
			try {
				Key k = cloneIfBuildable( mTemplate ) ;
				if ( k == null ) 
					throw new IllegalStateException("No buildable content.") ;
				return k ;
			} catch( CloneNotSupportedException e ) {
				throw new IllegalStateException("Cannot successfully clone content " + mTemplate) ;
			}
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
		public Builder update( Key key ) {
			if ( key.mAndroidID != null )
				mTemplate.mAndroidID = key.mAndroidID ;
			
			if ( key.mKey != null )
				mTemplate.mKey = key.mKey ;
			if ( key.mKeyTag != null )
				mTemplate.mKeyTag = key.mKeyTag ;
			if ( key.mKeyPromo != null )
				mTemplate.mKeyPromo = key.mKeyPromo ;
			if ( key.mKeySignature != null )
				mTemplate.mKeySignature = key.mKeySignature ;
			
			if ( key.mActivated != null )
				mTemplate.mActivated = key.mActivated ;
			if ( key.mValid != null )
				mTemplate.mValid = key.mValid ;
			
			return this ;
		}
		
		public Builder setAndroidID( String android_id ) {
			mTemplate.mAndroidID = android_id ;
			
			return this ;
		}
		
		public Builder setAndroidID( Context context ) {
			
			mTemplate.mAndroidID = AndroidID.get(context) ;
		
			return this ;
		}
		
		public Builder setKey( String key, String tag, String promo, String signature ) throws IOException {
			if ( !mTemplate.resemblesKey( key ) )
				throw new IOException("Provided key " + key + " does not resemble a valid key") ;
			
			mTemplate.mKey = mTemplate.stripKey( key ) ;
			mTemplate.mKeyTag = tag ;
			mTemplate.mKeyPromo = promo ;
			mTemplate.mKeySignature = signature ;
			
			return this ;
		}
		
		public Builder signKey( String tag, String promo, String signature ) {
			mTemplate.mKeyTag = tag ;
			mTemplate.mKeyPromo = promo ;
			mTemplate.mKeySignature = signature ;
			
			return this ;
		}
		
		public Builder setActivated( Boolean activated ) {
			mTemplate.mActivated = activated ;
			
			return this ;
		}
		
		public Builder setValid( Boolean valid ) {
			mTemplate.mValid = valid ;
			
			return this ;
		}
	}
	
	
	
	/**
	 * A Key updater is designed to perform two basic functions - advance
	 * a Key from one state to the next (for example, from a manually
	 * entered XL key code to the signed-and-tagged version of that code).
	 * 
	 * Updaters are mutable objects that operate over immutable keys.  To explain,
	 * a Key instance is provided to start the updater.  Once all updates
	 * are finished, a Key instance may be retrieved from the Updater.
	 * However, these will not be the same instance and no operations performed
	 * on the Updater instance will effect either 1. the Key provided during
	 * Construction or 2. a Key retrieved before the operation.
	 * 
	 * We assume that "register" and "activate" are valid actions for advancing
	 * any subclass.  Additionally, "valid" is an action which produces a 
	 * yes or no response.  ANY OTHER ACTION MUST BE IMPLEMENTED IN THE SUBCLASS.
	 * If a subclass does not allow "register," "activate" or "valid" actions
	 * as described below, consider implementing it as its own base class.
	 * 
	 * Updates are (usually) performed through two different methods,
	 * updateActivation and updateValidity.
	 * 
	 * updateValidity: contacts the server script (more on this later) and
	 * 		makes a query regarding the key's validity.  Handled by the
	 * 		base class unless mKey is null, in which case it is passed to
	 * 		the subclass for possible implementation.  The result will
	 * 		update 'mValid' in the underlying key.
	 * 
	 * updateActivation: moves the Key along the path to complete activation.
	 * 		The base Updater knows what to do if mKey is non-null (call "register")
	 * 		and what to do if mKey, mTag and mSignature are non-null (call "activate").
	 * 		If the Key is not in one of these states, we pass it to a subclass
	 * 		method for handling.  For example, a QuantroXLKey may want to handle
	 * 		a purchase using a JSON string.
	 * 
	 * 
	 * ERROR CODES: There are many things that can go wrong with an update.  Error
	 * 		codes are available after either update method.  For convenience,
	 * 		methods to set these codes are provided for subclasses.  It is inadvisable
	 * 		to directly set error codes in subclasses; use the provided methods instead.
	 * 
	 * 		ALWAYS set an error code when performing even a single update step.  If
	 * 		no error occurred, call setErrorNone().
	 * 		
	 * 		If additional (specialty) error codes are needed, consider implementing
	 * 		the object from-scratch.  The error codes listed here cannot be easily
	 * 		expanded.
	 * 
	 * SERVER COMMUNICATION: Subclasses are responsible for all communication other
	 * 		than "register," "activate," "valid" and "exists."  However, because the
	 * 		base class handles those, it requires access to specific information.
	 * 		
	 * 		Subclasses must override the following:
	 * 		queryURL()
	 * 		varKey()
	 * 
	 * 
	 * 
	 * @author Jake
	 *
	 */
	public static abstract class Updater {
		
		// ACTIONS
		protected static final String ACTION_REGISTER = "register" ;
		protected static final String ACTION_ACTIVATE = "activate" ;
		protected static final String ACTION_EXISTS = "exists" ;
		protected static final String ACTION_VALID = "valid" ;
		
		// Communication variables.  These are also likely to be used
		// in forming "signed Maps" and "signed Strings."
		protected static final String VAR_ACTION = "action" ;
		protected static final String VAR_SIGNATURE = "signature" ;
		protected static final String VAR_TAG = "tag" ;
		protected static final String VAR_PROMO = "promo" ;
		protected static final String VAR_ANDROID_ID = "android_id" ;
		protected static final String VAR_WAIT = "wait" ;
		protected static final String VAR_ANSWER = "answer" ;
		protected static final String VAR_PROBLEM = "problem" ;
		
		

		protected static final String ANSWER_YES = "yes" ;
		protected static final String ANSWER_NO = "no" ;
		
		protected static final String PROBLEM_INVALID = "invalid" ;
		protected static final String PROBLEM_NOT_AVAILABLE = "not_available" ;
		protected static final String PROBLEM_ISSUED = "issued" ;
		
		

		public static final int ERROR_NONE = 0 ;		// no error
		public static final int ERROR_TIMEOUT = 1 ;		// could not connect and get info within timeout
		public static final int ERROR_FAILED = 2 ;		// We received a "fail"
		public static final int ERROR_REFUSED = 3 ;		// Our request was refused.  
		public static final int ERROR_BLANK = 4 ;		// Empty response from server.
		public static final int ERROR_MALFORMED = 5 ;	// Malformed response from server.
		public static final int ERROR_WAITING = 6 ;		// We did not perform the update ; we are waiting.
		public static final int ERROR_INVALID = 7 ;		// Invalid keys do not advance.
		public static final int ERROR_NOT_AVAILABLE = 8 ;	// The response requested is not available.
		public static final int ERROR_ISSUED = 9 ;		// The response has already been issued.
		
		public static final int ERROR_UNNECESSARY = 10 ;	// E.g., an activated key does not need to be activated.
		public static final int ERROR_CANNOT_FORM_QUERY = 11 ;	// The key is not in a state where a query can even be formed; e.g., a "key" without an XL value can't update its validity.
		
		
		private ArrayList<String> RESPONSE_VARS ;
		private Hashtable<String, Object> RESPONSE_ANSWER ;
		private Hashtable<String, Object> RESPONSE_PROBLEM ;
		
		
		protected Key mTemplate ;
		
		private boolean mChanged ;
		private long mWaitUntil = 0 ;
		private int mLastError ;
		
		protected abstract String varKey() ;
		protected abstract String queryURL() ;
		
		protected Updater (Key key) {
			try {
				mTemplate = (Key)key.clone() ;
			} catch (CloneNotSupportedException e) {
				throw new IllegalArgumentException("Cannot clone provided key") ;
			}
			
			if ( varKey() == null )
				throw new IllegalStateException("Subclass is required to override varKey()") ;
			if ( queryURL() == null )
				throw new IllegalStateException("Subclass is required to override queryURL()") ;
			
			
			// Response vars and answers used for generic "activate", etc.
			RESPONSE_VARS = new ArrayList<String>() ;
			RESPONSE_VARS.add(VAR_SIGNATURE) ;
			RESPONSE_VARS.add(VAR_TAG) ;
			RESPONSE_VARS.add(varKey()) ;
			RESPONSE_VARS.add(VAR_PROMO) ;
			RESPONSE_VARS.add(VAR_ANDROID_ID) ;
			RESPONSE_VARS.add(VAR_WAIT) ;
			RESPONSE_VARS.add(VAR_ANSWER) ;
			RESPONSE_VARS.add(VAR_PROBLEM) ;
			addSubclassResponseVars( RESPONSE_VARS ) ;
			
			RESPONSE_ANSWER = new Hashtable<String, Object>() ;
			RESPONSE_ANSWER.put(ANSWER_YES, Boolean.TRUE) ;
			RESPONSE_ANSWER.put(ANSWER_NO, Boolean.FALSE) ;		
			
			RESPONSE_PROBLEM = new Hashtable<String, Object>() ;
			RESPONSE_PROBLEM.put(PROBLEM_INVALID, PROBLEM_INVALID) ;
			RESPONSE_PROBLEM.put(PROBLEM_NOT_AVAILABLE, PROBLEM_NOT_AVAILABLE) ;
			RESPONSE_PROBLEM.put(PROBLEM_ISSUED, PROBLEM_ISSUED) ;
			
		}
		
		protected abstract void addSubclassResponseVars( ArrayList<String> vars ) ;
		
		
		public Key getKey() {
			try {
				return (Key) mTemplate.clone() ;
			} catch (CloneNotSupportedException e) {
				throw new IllegalArgumentException("Cannot clone inner key") ;
			}
		}
		
		
		/**
		 * Performs an update with the intention of activating the key.  Returns
		 * 'true' if we activated the key; in other words, if the key
		 * was in a state allowing advancement, we successfully connected, and
		 * successfully activated.
		 * 
		 * If the underlying Key object has mKey set, this operation will
		 * be handled by this base implementation.  Otherwise, we will pass
		 * to the subclass using updateSubclassActivation.
		 * 
		 * @param timeout
		 * @return
		 */
		public boolean updateActivation( int timeout ) {
			
			if ( minimumWaitToActivate() > 0 ) {
				setErrorWaiting( mWaitUntil ) ;
				return false ;
			}
			
			// Can't activate an invalid key.
			if ( mTemplate.mValid != null && !mTemplate.mValid.booleanValue() ) {
				setErrorInvalid() ;
				return false ;
			}
			
			if ( mTemplate.mActivated != null && mTemplate.mActivated.booleanValue() ) {
				setErrorUnnecessary() ;
				return false ;
			}
			
			setErrorNone() ;
			boolean progressing = true ;
			while ( mLastError == ERROR_NONE && progressing ) {
				if ( mTemplate.mKey == null )
					updateSubclassActivation( timeout ) ;
				else if ( mTemplate.mKeyTag == null || mTemplate.mKeySignature == null )
					updateActivationRegister( timeout ) ;
				else
					updateActivationActivate( timeout ) ;
				
				progressing = canActivateKey() ;
			}
			
			return mLastError == ERROR_NONE && mTemplate.isActivated() ;
		}
		
		
		/**
		 * Subclasses with anything other than "register / activate / exists / valid"
		 * should implement this method, which updates mTemplate a single step.
		 * 
		 * This method should call setError* (always) and setChanged() (if appropriate).
		 * 
		 */
		protected abstract void updateSubclassActivation( int timeout ) ;
		
		
		/**
		 * Updates the activation by performing a "register" query.
		 */
		private void updateActivationRegister( int timeout ) {
			// Make a query!
			WebQuery webQuery = null ;
			
			Hashtable<String, Object> args = new Hashtable<String, Object>() ;
			args.put(VAR_ACTION, ACTION_REGISTER) ;
			args.put(varKey(), mTemplate.getKey()) ;		// wraps the key for us!
			args.put(VAR_ANDROID_ID, mTemplate.mAndroidID) ;
			updateActivationRegisterPutSubclassVariables( args ) ;
			
			webQuery = makeWebQuery( args ) ;
			
			// Query it!
			//
			// This method will handle all but OK responses.  Handling
			// of OK responses is left to the code below this call.
			WebQuery.Response response = performQueryForOneResponse( webQuery, timeout ) ;
			if ( mLastError != ERROR_NONE )
				return ;
			
			if ( response == null ) {
				throw new IllegalStateException("performQuery returned 'null' but did not set error") ;
			}
			
			// set activated to false...
			Builder builder = mTemplate.newBuilder() ;
			mTemplate = builder.setActivated(false).build() ;
			
			// get response variables...
			String key = response.getResponseVariableString(varKey()) ;
			String signature = response.getResponseVariableString(VAR_SIGNATURE) ;
			String tag = response.getResponseVariableString(VAR_TAG) ;
			String promo = response.getResponseVariableString(VAR_PROMO) ;
			// String resp_android_id = response.getResponseVariableString(VAR_ANDROID_ID) ;
			
			if ( key == null || signature == null || tag == null || promo == null ) {
				setErrorMalformed() ;
				return ;
			}
			
			// TODO: Check the signature of this response?  Remember to strip the key!
			
			try {
				// strips the key for us automatically
				builder.setKey(key, tag, promo, signature) ;
				updateActivationRegisterSetSubclassResponseVariables( response, builder ) ;
				
				mTemplate = builder.build() ;
				
			} catch ( IOException ioe ) {
				setErrorMalformed() ;
				return ;
			}
			setDidChange() ;
			setErrorNone() ;
		}
		
		protected abstract void updateActivationRegisterPutSubclassVariables( Hashtable<String, Object> args) ;
		protected abstract void updateActivationRegisterSetSubclassResponseVariables( WebQuery.Response response, Key.Builder builder ) throws IOException ;
		
		
		private void updateActivationActivate( int timeout ) {
			// Make a query!
			WebQuery webQuery = null ;
			
			Hashtable<String, Object> args = new Hashtable<String, Object>() ;
			args.put(VAR_ACTION, ACTION_ACTIVATE) ;
			args.put(varKey(), mTemplate.getKey()) ;		// wraps the key for us!
			args.put(VAR_TAG, mTemplate.getKeyTag()) ;
			args.put(VAR_PROMO, mTemplate.getKeyPromo()) ;
			args.put(VAR_SIGNATURE, mTemplate.getKeySignature()) ;
			args.put(VAR_ANDROID_ID, mTemplate.mAndroidID) ;
			updateActivationActivatePutSubclassVariables( args ) ;
			
			webQuery = makeWebQuery( args ) ;
			
			// Query it!
			//
			// This method will handle all but OK responses.  Handling
			// of OK responses is left to the code below this call.
			WebQuery.Response response = performQueryForOneResponse( webQuery, timeout ) ;
			if ( mLastError != ERROR_NONE )
				return ;
			
			if ( response == null ) {
				throw new IllegalStateException("performQuery returned 'null' but did not set error") ;
			}
			
			// If we make it here, it's activated.
			mTemplate = mTemplate.newBuilder().setActivated(true).build() ;
			setDidChange() ;
			setErrorNone() ;
		}
		
		protected abstract void updateActivationActivatePutSubclassVariables( Hashtable<String, Object> args) ;
		
		public boolean updateValidity( int timeout ) {
			if ( mTemplate.mKey == null ) {
				setErrorCannotFormQuery() ;
				return false ;
			}
			
			else if ( minimumWaitToActivate() > 0 ) {
				setErrorWaiting( mWaitUntil ) ;
				return false ;
			}
			
			WebQuery webQuery = null ;
			Hashtable<String, Object> args = new Hashtable<String, Object>() ;
			args.put(VAR_ACTION, ACTION_VALID) ;
			args.put(varKey(), mTemplate.getKey()) ;
			webQuery = makeWebQuery( args ) ;
			
			// Query it!
			WebQuery.Response response = performQueryForOneResponse( webQuery, timeout ) ;
			if ( mLastError != ERROR_NONE )
				return false ;
			
			if ( response == null ) {
				throw new IllegalStateException("performQuery returned 'null' but did not set error") ;
			}
			
			// Finally, get the answer.
			Boolean resp = response.getResponseVariableBoolean(VAR_ANSWER) ;
			if ( resp != null ) {
				mTemplate = mTemplate.newBuilder().setValid(resp).build() ;
				setDidChange() ;
				setErrorNone() ;
				return true ;
			}
			
			setErrorMalformed() ;
			return false ;
		}
		
		
		protected WebQuery.Response performQueryForOneResponse( WebQuery webQuery, int timeout ) {
			// Query it!
			WebQuery.Response [] responses ;
			try {
				responses = webQuery.query( timeout ) ;
			} catch ( Exception e ) {
				e.printStackTrace() ;
				setErrorTimeout() ;
				return null ;
			}
			
			if ( responses == null || responses.length == 0 ) {
				setErrorBlank() ;
				return null ;
			}
			
			WebQuery.Response response = responses[0] ;
			
			// Update.
			if ( response.isFAIL() ) {
				setErrorFailed() ;
				return null ;
			}
			
			if ( response.isNO() ) {
				setErrorRefused() ;
				Double wait = response.getResponseVariableDouble(VAR_WAIT) ;
				if ( wait != null ) {
					long millisToWait = (long)(wait.doubleValue() * 1000) ;
					long waitUntil = System.currentTimeMillis() + millisToWait ;
					setErrorWaiting( waitUntil ) ;
				}
				return null ;
			}
			
			String problem = response.getResponseVariableString(VAR_PROBLEM) ;
			if ( problem != null ) {
				if ( problem.equals(PROBLEM_INVALID) )
					setErrorInvalid() ;
				else if ( problem.equals(PROBLEM_ISSUED) )
					setErrorIssued() ;
				else if ( problem.equals(PROBLEM_NOT_AVAILABLE) )
					setErrorNotAvailable() ;
				else
					setErrorMalformed() ;
				
				return null ;
			}
			
			return response ;
		}
		
		
		private WebQuery makeWebQuery( Hashtable<String, Object> args ) {
			WebQuery.Builder builder = new WebQuery.Builder() ;
			builder.setURL(queryURL()) ;
			builder.setResponseTypeTerseVariableList(RESPONSE_VARS) ;
			builder.addVariableCode(VAR_ANSWER, RESPONSE_ANSWER) ;
			builder.addVariableCode(VAR_PROBLEM, RESPONSE_PROBLEM) ;
			builder.setPostVariables(args) ;
			return builder.build() ;
		}
		
		/**
		 * Returns the last error to occur, which could be ERROR_NONE.
		 * @return
		 */
		public int getLastError() {
			return mLastError ;
		}
		
		
		/**
		 * Has the underlying key changed due to the actions of this Verifier?
		 * @return
		 */
		public boolean changedKey() {
			return mChanged ;
		}
		
		
		/**
		 * Can this Verifier "Advance" a key?  Keys pass through a series of
		 * states: for example, an XL purchased key (JSON only) must first
		 * have an XL code associated with it, and then that XL code activated.
		 * 
		 * We provide a generic method for doing this: the "advance" method.
		 * This method returns whether such a method could presumably change
		 * the state of the key.
		 * 
		 * @return True if calling 'advance' could possibly change the state
		 * 	of the key.  If this method returns 'false,
		 */
		public boolean canActivateKey() {
			return minimumWaitToActivate() <= 0
					&& ( mTemplate.mActivated == null || !mTemplate.mActivated  )
					&& ( mTemplate.mValid == null || mTemplate.mValid );
		}
		
		
		
		
		/**
		 * Have we been told to wait before attempting the last operation?
		 * If so, this method returns the number of milliseconds we should
		 * wait - at a minimum - before the next call to advance().
		 * 
		 * @return
		 */
		public long minimumWaitToActivate() {
			return Math.max(0, mWaitUntil - System.currentTimeMillis()) ;
		}
		
		
		/**
		 * This key has advanced to completion.
		 * @return
		 */
		public boolean activated() {
			return mTemplate.mActivated != null && mTemplate.mActivated.booleanValue() ;
		}
		
		protected void setDidChange() {
			mChanged = true ;
		}
		
		protected void setErrorNone() {
			mLastError = ERROR_NONE ;
		}
		
		protected void setErrorTimeout() {
			mLastError = ERROR_TIMEOUT ;
		}
		
		protected void setErrorFailed() {
			mLastError = ERROR_FAILED ;
		}
		
		protected void setErrorRefused() {
			mLastError = ERROR_REFUSED ;
		}
		
		protected void setErrorBlank() {
			mLastError = ERROR_BLANK ;
		}
		
		protected void setErrorMalformed() {
			mLastError = ERROR_MALFORMED ;
		}
		
		protected void setErrorWaiting( long waitUntil ) {
			mLastError = ERROR_WAITING ;
			mWaitUntil = waitUntil ;
		}
		
		protected void setErrorInvalid() {
			mLastError = ERROR_INVALID ;
		}
		
		protected void setErrorNotAvailable() {
			mLastError = ERROR_NOT_AVAILABLE ;
		}
		
		protected void setErrorIssued() {
			mLastError = ERROR_ISSUED ;
		}
		
		
		
		protected void setErrorUnnecessary() {
			mLastError = ERROR_UNNECESSARY ;
		}
		
		protected void setErrorCannotFormQuery() {
			mLastError = ERROR_CANNOT_FORM_QUERY ;
		}
	}
	
	
	// All keys are assumed to have these fields.
	protected String mKey ;
	protected String mKeyTag ;
	protected String mKeyPromo ;
	protected String mAndroidID ;
	protected String mKeySignature ;
	
	protected Boolean mValid ;
	protected Boolean mActivated ;
	
	
	protected void setAs( Key k ) {
		if ( k == null ) {
			mKey = null ;
			mKeyTag = null ;
			mKeyPromo = null ;
			mAndroidID = null ;
			mKeySignature = null ;
			
			mValid = null ;
			mActivated = null ;
		} else {
			mKey = k.mKey ;
			mKeyTag = k.mKeyTag ;
			mKeyPromo = null ;
			mAndroidID = k.mAndroidID ;
			mKeySignature = k.mKeySignature ;
			
			mValid = k.mValid ;
			mActivated = k.mActivated ;
		}
	}
	
	
	/**
	 * Creates and returns a new Builder using this Key as its inner
	 * template.  Keys are immutable, so nothing the returned object
	 * does will alter the contents of this Key.  Use build() when
	 * finished.
	 * @return
	 */
	public abstract Builder newBuilder() ;
	
	
	/**
	 * Creates and returns a new Updater using this Key as its inner 
	 * template.  Keys are immutable, so nothing the returned object
	 * does will alter the contents of this Key.  Use getKey() when
	 * finished.
	 * 
	 * @return
	 */
	public abstract Updater newUpdater() ;
	
	
	/**
	 * Creates and returns a new Key, which should be an exact duplicate
	 * of this one.  Keys are immutable, so the returned object cannot be
	 * changed, and we guarantee that they will remain identical from
	 * that point.
	 * 
	 * @return
	 */
	public Key newKey() {
		Key key = newDuplicateInstance() ;
		if ( !this.getClass().equals( key.getClass() ) )
			throw new IllegalStateException("Expecting an instance of  "+ this.getClass() + ", got " + key.getClass() + " instead.  " + this.getClass() + " should implement newInstance()") ;
		return key ;
	}
	
	
	/**
	 * Subclasses are required to implement this method.
	 * EVERY subclass should do it.  Just constructs and returns
	 * a new instance of the Key.
	 * 
	 * @return
	 */
	protected abstract Key newDuplicateInstance() ;
	
	/**
	 * Returns, as a string, the XL key value.
	 * @return
	 */
	public String getKey() {
		return wrapKey( mKey, keyPrefix() ) ;
	}
	
	public String getKeyValueWithoutWrapper() {
		return mKey ;
	}
	
	/**
	 * Returns, as a string, the XL key tag.
	 * @return
	 */
	public String getKeyTag() {
		return mKeyTag ;
	}
	
	/**
	 * A textual note associated with the key for the user's benefit.
	 * @return
	 */
	public String getKeyPromo() {
		return mKeyPromo ;
	}
	
	/**
	 * Returns, as a string, the Android device ID used to activate this key.
	 * @return
	 */
	public String getAndroidID() {
		return mAndroidID ;
	}
	
	/**
	 * Returns, as a string, the signature the quantro_xl_web script gave to
	 * this key.
	 * @return
	 */
	public String getKeySignature() {
		return mKeySignature ;
	}
	

	
	public boolean isActivated() {
		return mActivated != null && mActivated.booleanValue() ;
	}
	
	public boolean isNotActivated() {
		return mActivated != null && !mActivated.booleanValue() ;
	}
	
	public boolean isValid() {
		return mValid != null && mValid.booleanValue() ;
	}
	
	public boolean isInvalid() {
		return mValid != null && !mValid.booleanValue() ;
	}
	
	
	
	public abstract Map<String, String> getSignedMap() ;
	
	public String getSignedString() {
		Map<String, String> values = getSignedMap() ;
		// First: keys in alphabetical order.
		String [] keys = new String[values.size()] ;
		Set<String> keySet = values.keySet() ;
		Iterator<String> iter = keySet.iterator() ;
		int num = 0 ;
		for ( ; iter.hasNext() ; )
			keys[num++] = iter.next() ;
		
		// Second: alphabetical order.
		Arrays.sort(keys) ;
		
		// Third: construct the comparison string.
		StringBuilder sb = new StringBuilder() ;
		for ( int i = 0; i < keys.length; i++ ) {
			sb.append(keys[i]).append(":").append(values.get(keys[i])) ;
			if ( i < keys.length-1 )
				sb.append("\n") ;
		}
		
		return sb.toString() ;
	}

}
