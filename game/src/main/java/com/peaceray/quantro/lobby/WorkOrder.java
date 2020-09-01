package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import com.peaceray.quantro.communications.nonce.Effort;
import com.peaceray.quantro.communications.web.WebQuery;
import com.peaceray.quantro.lobby.MutableInternetLobby.Opener;

public class WorkOrder {
	
	private static final int REQUEST_TYPE_OPEN = 0 ;
	private static final int REQUEST_TYPE_MAINTAIN = 1 ;
	private static final int REQUEST_TYPE_OPEN_GAME = 2 ;
	private static final int REQUEST_TYPE_MAINTAIN_GAME = 3 ;
	
	
	
	private static final String REQUEST_OPEN = "open" ;
	private static final String REQUEST_MAINTAIN = "maintain" ;
	private static final String REQUEST_OPEN_GAME = "open_game" ;
	private static final String REQUEST_MAINTAIN_GAME = "maintain_game" ;
	
	int mRequestType ;
	
	MutableInternetLobby mMutableInternetLobby ;
	Opener mOpener ;
	InternetLobbyGame mInternetLobbyGame ;
	
	// as a non-static inner class, a WorkOrder has access to all
	// the containing lobby's information, including nonce and edit key.
	// However, it is worth explicitly storing the entire work order
	// in a Hashtable, since we need to report the same information 
	// back when 
	Effort mEffort ;
	Hashtable<String, String> mWorkOrderVars ;
	long mDeadline ;
	
	/**
	 * Constructs a WorkOrder from the provided server response.
	 * If the server response is malformed or is of a type that
	 * does not contain a work order, will thrown an IllegalArgumentException.
	 * @param resp
	 */
	WorkOrder( MutableInternetLobby mutableInternetLobby, WebQuery.Response resp ) {
		mMutableInternetLobby = mutableInternetLobby ;
		if ( mMutableInternetLobby == null )
			throw new NullPointerException("Cannot provide null lobby") ;
		setWorkOrder( resp ) ;
	}
	
	/**
	 * Constructs a WorkOrder from the provided server response.
	 * If the server response is malformed or is of a type that
	 * does not contain a work order, will thrown an IllegalArgumentException.
	 * @param resp
	 */
	WorkOrder( Opener opener, WebQuery.Response resp ) {
		mOpener = opener ;
		if ( mOpener == null )
			throw new NullPointerException("Cannot provide null opener") ;
		setWorkOrder( resp ) ;
	}
	
	/**
	 * Constructs a WorkOrder from the provided server response.
	 * If the server response is malformed or is of a type that
	 * does not contain a work order, will thrown an IllegalArgumentException.
	 * @param resp
	 */
	WorkOrder( InternetLobbyGame internetLobbyGame, WebQuery.Response resp ) {
		mInternetLobbyGame = internetLobbyGame ;
		if ( mInternetLobbyGame == null )
			throw new NullPointerException("Cannot provide null InternetLobbyGame") ;
		setWorkOrder( resp ) ;
	}
	
	
	
	
	void setWorkOrder( WebQuery.Response resp ) {
		mWorkOrderVars = new Hashtable<String, String>() ;
		
		boolean useGameNonce = false ;
		
		try {
			if ( REQUEST_OPEN.equals( resp.getResponseVariableString(WebConsts.VAR_REQUEST) ) ) {
				mRequestType = REQUEST_TYPE_OPEN ;
				mWorkOrderVars.put(WebConsts.VAR_REQUEST, resp.getResponseVariableString(WebConsts.VAR_REQUEST)) ;
				mWorkOrderVars.put(WebConsts.VAR_NONCE, resp.getResponseVariableString(WebConsts.VAR_NONCE)) ;
				mWorkOrderVars.put(WebConsts.VAR_KEY, resp.getResponseVariableString(WebConsts.VAR_KEY)) ;
				mWorkOrderVars.put(WebConsts.VAR_TIME, resp.getResponseVariableString(WebConsts.VAR_TIME)) ;
				mWorkOrderVars.put(WebConsts.VAR_WORK, resp.getResponseVariableString(WebConsts.VAR_WORK)) ;
				mWorkOrderVars.put(WebConsts.VAR_ADDRESS, resp.getResponseVariableString(WebConsts.VAR_ADDRESS)) ;
				mWorkOrderVars.put(WebConsts.VAR_SALT, resp.getResponseVariableString(WebConsts.VAR_SALT)) ;
				if ( resp.getResponseVariableBoolean(WebConsts.VAR_XL) != null )
					mWorkOrderVars.put(WebConsts.VAR_XL, resp.getResponseVariableString(WebConsts.VAR_XL) ) ;
				mWorkOrderVars.put(WebConsts.VAR_SIGNATURE, resp.getResponseVariableString(WebConsts.VAR_SIGNATURE)) ;
				mDeadline = 0 ;
			} else if ( REQUEST_MAINTAIN.equals( resp.getResponseVariableString(WebConsts.VAR_REQUEST) ) ) {
				mRequestType = REQUEST_TYPE_MAINTAIN ;
				mWorkOrderVars.put(WebConsts.VAR_REQUEST, resp.getResponseVariableString(WebConsts.VAR_REQUEST)) ;
				mWorkOrderVars.put(WebConsts.VAR_NONCE, resp.getResponseVariableString(WebConsts.VAR_NONCE)) ;
				mWorkOrderVars.put(WebConsts.VAR_KEY, resp.getResponseVariableString(WebConsts.VAR_KEY)) ;
				mWorkOrderVars.put(WebConsts.VAR_TIME, resp.getResponseVariableString(WebConsts.VAR_TIME)) ;
				mWorkOrderVars.put(WebConsts.VAR_SECONDS, resp.getResponseVariableString(WebConsts.VAR_SECONDS)) ;
				mWorkOrderVars.put(WebConsts.VAR_WORK, resp.getResponseVariableString(WebConsts.VAR_WORK)) ;
				mWorkOrderVars.put(WebConsts.VAR_SALT, resp.getResponseVariableString(WebConsts.VAR_SALT)) ;
				mWorkOrderVars.put(WebConsts.VAR_SIGNATURE, resp.getResponseVariableString(WebConsts.VAR_SIGNATURE)) ;
				mDeadline = Math.round( System.currentTimeMillis() + resp.getResponseVariableDouble(WebConsts.VAR_SECONDS) / 1000 ) ;
			} else if ( REQUEST_OPEN_GAME.equals( resp.getResponseVariableString(WebConsts.VAR_REQUEST ) ) ) {
				useGameNonce = true ;
				mRequestType = REQUEST_TYPE_OPEN_GAME ;
				mWorkOrderVars.put(WebConsts.VAR_REQUEST, resp.getResponseVariableString(WebConsts.VAR_REQUEST) ) ;
				mWorkOrderVars.put(WebConsts.VAR_NONCE, resp.getResponseVariableString(WebConsts.VAR_NONCE) ) ;
				mWorkOrderVars.put(WebConsts.VAR_KEY, resp.getResponseVariableString(WebConsts.VAR_KEY) ) ;
				mWorkOrderVars.put(WebConsts.VAR_GAME_NONCE, resp.getResponseVariableString(WebConsts.VAR_GAME_NONCE) ) ;
				mWorkOrderVars.put(WebConsts.VAR_GAME_KEY, resp.getResponseVariableString(WebConsts.VAR_GAME_KEY) ) ;
				mWorkOrderVars.put(WebConsts.VAR_TIME, resp.getResponseVariableString(WebConsts.VAR_TIME) ) ;
				mWorkOrderVars.put(WebConsts.VAR_WORK, resp.getResponseVariableString(WebConsts.VAR_WORK) ) ;
				mWorkOrderVars.put(WebConsts.VAR_SALT, resp.getResponseVariableString(WebConsts.VAR_SALT) ) ;
				mWorkOrderVars.put(WebConsts.VAR_SIGNATURE, resp.getResponseVariableString(WebConsts.VAR_SIGNATURE)) ;
				mDeadline = 0 ;
			} else if ( REQUEST_MAINTAIN_GAME.equals( resp.getResponseVariableString(WebConsts.VAR_REQUEST) ) ) {
				mRequestType = REQUEST_TYPE_MAINTAIN_GAME ;
				mWorkOrderVars.put(WebConsts.VAR_REQUEST, resp.getResponseVariableString(WebConsts.VAR_REQUEST)) ;
				mWorkOrderVars.put(WebConsts.VAR_NONCE, resp.getResponseVariableString(WebConsts.VAR_NONCE)) ;
				mWorkOrderVars.put(WebConsts.VAR_KEY, resp.getResponseVariableString(WebConsts.VAR_KEY)) ;
				mWorkOrderVars.put(WebConsts.VAR_TIME, resp.getResponseVariableString(WebConsts.VAR_TIME)) ;
				mWorkOrderVars.put(WebConsts.VAR_SECONDS, resp.getResponseVariableString(WebConsts.VAR_SECONDS)) ;
				mWorkOrderVars.put(WebConsts.VAR_WORK, resp.getResponseVariableString(WebConsts.VAR_WORK)) ;
				mWorkOrderVars.put(WebConsts.VAR_SALT, resp.getResponseVariableString(WebConsts.VAR_SALT)) ;
				mWorkOrderVars.put(WebConsts.VAR_SIGNATURE, resp.getResponseVariableString(WebConsts.VAR_SIGNATURE)) ;
				mDeadline = Math.round( System.currentTimeMillis() + resp.getResponseVariableDouble(WebConsts.VAR_SECONDS) / 1000 ) ;
			} else
				throw new Exception() ;
		} catch ( Exception e ) {
			e.printStackTrace() ;
			throw new IllegalArgumentException("No work order found") ;
		}
		
		try {
			if ( useGameNonce ) {
				mEffort = new Effort(
						resp.getResponseVariableInteger(WebConsts.VAR_WORK),
						resp.getResponseVariableNonce(WebConsts.VAR_GAME_NONCE),
						resp.getResponseVariableNonce(WebConsts.VAR_GAME_KEY),
						resp.getResponseVariableNonce(WebConsts.VAR_SALT)
						) ;
			} else {
				mEffort = new Effort(
						resp.getResponseVariableInteger(WebConsts.VAR_WORK),
						resp.getResponseVariableNonce(WebConsts.VAR_NONCE),
						resp.getResponseVariableNonce(WebConsts.VAR_KEY),
						resp.getResponseVariableNonce(WebConsts.VAR_SALT)
						) ;
			}
		} catch ( IOException ioe ) {
			ioe.printStackTrace() ;
			throw new IllegalArgumentException("Problem getting nonce(s) from work order.") ;
		}
	}
	
	
	boolean requestIsOpen() {
		return mRequestType == REQUEST_TYPE_OPEN ;
	}
	
	boolean requestIsMaintain() {
		return mRequestType == REQUEST_TYPE_MAINTAIN ;
	}
	
	boolean requestIsOpenGame() {
		return mRequestType == REQUEST_TYPE_OPEN_GAME ;
	}
	
	boolean requestIsMaintainGame() {
		return mRequestType == REQUEST_TYPE_MAINTAIN_GAME ;
	}
	
	String getNonceString() {
		return mWorkOrderVars.get(WebConsts.VAR_NONCE) ;
	}
	
	String getEditKeyString() {
		return mWorkOrderVars.get(WebConsts.VAR_KEY) ;
	}
	
	String getGameNonceString() {
		return mWorkOrderVars.get(WebConsts.VAR_GAME_NONCE) ;
	}
	
	String getGameEditKeyString() {
		return mWorkOrderVars.get(WebConsts.VAR_GAME_KEY) ;
	}
	
	String getXLKeyString() {
		return mWorkOrderVars.get(WebConsts.VAR_XL) ;
	}
	
	void getVars( Hashtable<String, Object> vars ) {
		Enumeration<String> keys = mWorkOrderVars.keys() ;
		for ( ; keys.hasMoreElements() ; ) {
			String key = keys.nextElement() ;
			vars.put( key, mWorkOrderVars.get(key) ) ;
		}
		
		if ( mEffort.hasSalt() )
			vars.put(WebConsts.VAR_PROOF, mEffort.getSalt()) ;
	}
	
	
	/**
	 * Performs the work indicated in the work order.
	 * If this work order is already complete, will return immediately.
	 */
	synchronized public void performWork() {
		mEffort.getSalt() ;
	}
	
	/**
	 * Performs the work indicated in the work order, taking
	 * a maximum of maxMillis to do so.  If maxMillis is <= 0,
	 * this is equivalent to performWork().
	 * 
	 * @param maxMillis
	 * @return Whether the work is complete.  If 'false',
	 * 	continue to make granular calls to this method.
	 */
	synchronized public boolean performWork( long maxMillis ) {
		return mEffort.getSalt(maxMillis) != null ;
	}
	
	/**
	 * Returns whether this work order is complete.
	 * @return
	 */
	public boolean isComplete() {
		return mEffort.hasSalt() ;
	}
	
	
	/**
	 * Returns, as a long as returned by System.currentTimeMillis(), the deadline
	 * for this workorder.  Some WorkOrders do not have a deadline; for those, this
	 * method will return 0.
	 * 
	 * Deadlines are a bit counter-intuitive; if we have one, we can safely wait
	 * for it to (almost) expire before reporting the result; if not, we should try
	 * to get it done as quickly as possible.
	 * 
	 * @return
	 */
	public long getDeadline() {
		return mDeadline ;
	}
	
	/**
	 * Returns whether this workOrder has an explicit deadline.
	 * @return
	 */
	public boolean hasDeadline() {
		return mDeadline != 0 ;
	}
	
}