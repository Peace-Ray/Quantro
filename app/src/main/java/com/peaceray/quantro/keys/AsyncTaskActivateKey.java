package com.peaceray.quantro.keys;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.utils.AsyncTaskPlus;

import android.content.Context;
import android.os.PowerManager.WakeLock;

public class AsyncTaskActivateKey extends AsyncTaskPlus<Object, Object, Integer> {
	
	
	public interface Listener {
		
		/**
		 * Indicates to the Listener that we are starting our activation.
		 * We remind the Listener of our parameter.
		 * 
		 * @param task
		 * @param key
		 */
		public void atakl_preActivation( AsyncTaskActivateKey task, Key key ) ;
	
		
		/**
		 * Gives the Listener the chance to short-circuit operation by providing a fake result.
		 * This method should return the result code, which will be provided back to the Listener
		 * in ataxlkl_postActivation.  No server communication will occur if a fake result is
		 * provided.
		 * 
		 * To continue execution, including eventually providing a REAL result (i.e., to proceed
		 * normally), return RESULT_NO_FAKE.
		 * 
		 * @param task
		 * @param key
		 * @return
		 */
		public int atakl_onExecuteFakeResult( AsyncTaskActivateKey task, Key key ) ;
	
		/**
		 * Indicates to the Listener that our activation is finished.  We
		 * provide a new key to our Listener reflecting any changes (which 
		 * might be null if we weren't able to accomplish anything).  We also
		 * inform the listener of the result code.  It is the listener's
		 * responsibility to store the result (or not).
		 * 
		 * @param task
		 * @param key
		 * @param resultCode
		 * @return
		 */
		public void atakl_postActivation( AsyncTaskActivateKey task, Key key, int resultCode ) ;
	}
	
	
	public static final int RESULT_NO_FAKE = -1 ;
	public static final int RESULT_ACTIVATED = 0 ;
	public static final int RESULT_FAILED_NO_SERVER = 1 ;
	public static final int RESULT_FAILED_INVALID = 2 ;
	public static final int RESULT_FAILED_ISSUED = 3 ;
	public static final int RESULT_FAILED_NOT_AVAILABLE = 4 ;
	
	public static final int RESULT_FAILED_REFUSED = 5 ;
	public static final int RESULT_FAILED_WAITING = 6 ;
	public static final int RESULT_FAILED_STORAGE = 7 ;	
	public static final int RESULT_FAILED_UNSPECIFIED = 8 ;
	
	
	// We can activate from a String (keyVal) or an existing QuantroXLKey object.
	private WeakReference<Listener> mwrListener ;
	private WakeLock mWakeLock ;
	private boolean mDidLock ;
	private Key mKey ;
	
	private WeakReference<Context> mwrStoreContext = null ;
	private boolean mStore	= false ;	// Default: do NOT store the key as we progress.
	private int mDelay		= 1000 ;	// Default: 1 second wait
	private int mTimeout 	= 10000 ;	// Default: 10 second timeout
	private long mMillisToWait = 0 ;	// Default: no time to wait.
	
	
	private Object mTag = null ;		

	
	public AsyncTaskActivateKey( Listener listener, WakeLock wakeLock, Key key ) {
		mwrListener = new WeakReference<Listener>(listener) ;
		mWakeLock = wakeLock ;
		mDidLock = false ;
		mKey = key ;
		
		// We require a listener and a key value; wakeLock is optional.
		if ( listener == null )
			throw new NullPointerException("Provided listener cannot be null") ;
		if ( key == null )
			throw new NullPointerException("Provided key cannot be null") ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// GETTERS SETTERS
	//
	// Some important settings can be provided (optionally) before execution,
	// such as a delay (before ataxlkl_onExecuteFakeResult and then the first
	// server contact) and a timeout (time spent waiting for each server response).
	//
	
	/**
	 * Sets whether this AsyncTask should store the key as it makes progress,
	 * rather than allow the Listener to do this.
	 * 
	 * If 'true', we store after every potential key change, overwriting the key
	 * currently in storage.  Note that this will likely write "partially activated"
	 * keys to storage, so be careful of this.
	 * 
	 * Call this before execute() to ensure it has an effect.
	 * 
	 * Default: false.
	 * @param doStore Should we store the key after every change?
	 * @return This object, for chaining.
	 */
	public AsyncTaskActivateKey storing( Context context, boolean doStore ) {
		if ( doStore && context == null )
			throw new NullPointerException("If storing, must provide a non-null context") ;
		mwrStoreContext = new WeakReference<Context>(context) ;
		mStore = doStore ;
		return this ;
	}
	
	/**
	 * Returns whether we are storing the key to KeyStorage after every change.
	 * @return The value set by the most recent call to 'storing( boolean )', 'false' by default.
	 */
	public boolean storing() {
		return mStore ;
	}
	
	/**
	 * Sets the delay to be used before the call to ataxlkl_onExecuteFakeResult
	 * and then the first server contact.  Should be called before execute() or
	 * may have no effect.
	 * 
	 * @param delay The milliseconds to delay
	 * @return This object to allow chaining.
	 */
	public AsyncTaskActivateKey delay( int delay ) {
		mDelay = delay ;
		return this ;
	}
	
	/**
	 * Returns the currently set delay.
	 * @return
	 */
	public int delay() {
		return mDelay ;
	}
	
	
	/**
	 * Sets the timeout to be used for any server communication.  Should be called
	 * before execute() or may have no effect.
	 * 
	 * @param timeout The milliseconds for each timeout
	 * @return This object to allow chaining.
	 */
	public AsyncTaskActivateKey timeout( int timeout ) {
		mTimeout = timeout ;
		return this ;
	}
	
	/**
	 * Returns the currently set timeout.
	 * @return
	 */
	public int timeout() {
		return mTimeout ;
	}
	
	
	/**
	 * Sets an arbitrary Tag for this object.  Will never be internally
	 * accessed, used, or reassigned.
	 * 
	 * @param tag
	 * @return
	 */
	public AsyncTaskActivateKey tag( Object tag ) {
		mTag = tag ;
		return this ;
	}
	
	
	/**
	 * Returns the currently set tag, identical to that most recently set
	 * by tag( obj ), or 'null' if that method was not called.
	 * 
	 * @return
	 */
	public Object tag() {
		return mTag ;
	}
	
	
	/**
	 * Returns the milliseconds we should wait before the next activation
	 * attempt.  Will return 0 unless we have the result "RESULT_FAILED_WAITING."
	 * @return
	 */
	public long millisecondsToWait( ) {
		return mMillisToWait ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	// OVERRIDE STATUS METHODS
	
	
	@Override
	protected void onPreExecute() {
		// Lock this up tight!
		if ( mWakeLock != null ) {
			synchronized ( mWakeLock ) {
				mWakeLock.acquire() ;
				mDidLock = true ;
			}
		}
		
		// Let the listener know.  'try' this because we can't
		// afford to let any exceptions slip through (we need to release
		// the lock).
		try {
			mwrListener.get().atakl_preActivation(this, mKey) ;
		} catch( Exception e ) {
			e.printStackTrace() ;
		}
	}
	

	@Override
	protected Integer doInBackground(Object... params) {
		
		// A massive 'try' block because we can't afford any exceptions.
		// We MUST release the lock in onPostExecute.
		// Additional bonus: if our listener is collected, we
		// quit out of this loop upon the first NullPointerException.
		try {
			
			// First: delay.
			try {
				Thread.sleep(mDelay) ;
			} catch (InterruptedException e) {
				// whoops!
				return null ;
			}
			
			// Second: fake result?
			int fakeResult = mwrListener.get().atakl_onExecuteFakeResult(this, mKey) ;
			if ( fakeResult != RESULT_NO_FAKE )
				return fakeResult ;
			
			// Third: communication.
			Key.Updater updater = mKey.newUpdater() ;
			// We want to activate and to update Validity.  If JSON only
			// we need to activate first; otherwise, we check validity first.
			boolean checkValidityFirst = mKey.getKey() != null ;
			boolean ok ;
			
			if ( checkValidityFirst ) {
				ok = updater.updateValidity(mTimeout) ;
				mKey = updater.getKey() ;
				if ( mStore && !KeyStorage.updateKey(mwrStoreContext.get(), mKey) )
					return RESULT_FAILED_STORAGE ;
				if ( !ok )
					return processError( updater ) ;
			}
			
			// activate.
			ok = updater.updateActivation(mTimeout) ;
			mKey = updater.getKey() ;
			if ( mStore && !KeyStorage.updateKey(mwrStoreContext.get(), mKey) )
				return RESULT_FAILED_STORAGE ;
			if ( !ok )
				return processError( updater ) ;
			
			if ( !checkValidityFirst ) {
				ok = updater.updateValidity(mTimeout) ;
				mKey = updater.getKey() ;
				if ( mStore && !KeyStorage.updateKey(mwrStoreContext.get(), mKey) )
					return RESULT_FAILED_STORAGE ;
				if ( !ok )
					return processError( updater ) ;
			}
			
			// If we got here, everything seemed to work OK.
			return RESULT_ACTIVATED ;
		} catch( Exception e ) {
			e.printStackTrace() ;
			return null ;
		}
	}
	
	
	@Override
	protected void onPostExecute(Integer result) {
		
		// We want to make sure we unlock, so...
		try {
			// Let the listener know.
			int resInt = result == null ? RESULT_FAILED_UNSPECIFIED : result.intValue() ;
			Listener l = mwrListener.get() ;
			if ( l != null )
				l.atakl_postActivation(this, mKey, resInt) ;
			
		} finally {
			if ( mWakeLock != null ) {
				synchronized ( mWakeLock ) {
					if ( mDidLock )
						mWakeLock.release() ;
				}
			}
		}
	}
	
	
	/**
	 * Returns the appropriate error code according to what went wrong.
	 * Also stores relevant information; e.g., if we have ERROR_WAITING,
	 * we note the time to wait in our own records.
	 * @param updater
	 * @return
	 */
	private int processError( Key.Updater updater ) {
		switch( updater.getLastError() ) {
        case Key.Updater.ERROR_INVALID:
        	return RESULT_FAILED_INVALID ;
        case Key.Updater.ERROR_ISSUED:
        	return RESULT_FAILED_ISSUED ;
        case Key.Updater.ERROR_NOT_AVAILABLE:
        	return RESULT_FAILED_NOT_AVAILABLE ;
        case Key.Updater.ERROR_REFUSED:
        case Key.Updater.ERROR_FAILED:
        	return RESULT_FAILED_REFUSED ;
        case Key.Updater.ERROR_WAITING:
        	mMillisToWait = updater.minimumWaitToActivate() ;
        	return RESULT_FAILED_WAITING ;
        case Key.Updater.ERROR_BLANK:
        case Key.Updater.ERROR_MALFORMED:
        case Key.Updater.ERROR_TIMEOUT:
        	return RESULT_FAILED_NO_SERVER ;
        	
        default:
        	return RESULT_FAILED_UNSPECIFIED ;
        }
	}

}
