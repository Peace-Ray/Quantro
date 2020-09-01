package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Hashtable;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.web.WebQuery;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;
import com.peaceray.quantro.lobby.exception.ConfirmErrorException;
import com.peaceray.quantro.lobby.exception.ConfirmTimeoutException;
import com.peaceray.quantro.lobby.exception.RequestErrorException;
import com.peaceray.quantro.lobby.exception.RequestTimeoutException;
import com.peaceray.quantro.lobby.exception.WorkTimeoutException;


/**
 * A MutableInternetLobby is an InternetLobby which we're allowed to change
 * and edit.  Note an important distinction here: any Lobby instance is mutable,
 * in the sense that our local representative can be updated and changed; however,
 * an InternetLobby reflects data stored elsewhere and publicly available, and
 * thus a MutableInternetLobby is one such that that remote and publicly 
 * available data can be changed via this object.
 * 
 * Making changes generally requires an editKey, which is determined at
 * the time the lobby is created.  A further complication: lobby creation
 * and maintenance (and game creation and maintenance) is typically
 * a three-step process, with each step taking a significant amount of
 * time.
 * 
 * 1. Request an action and receive a work order
 * 2. Perform the work
 * 3. Confirm the request w/ completed work
 * 
 * Work orders are meant to demonstrate sincerity, so database rows aren't
 * filled needlessly.  The amount of work required will scale up with the 
 * current popularity of the service (more lobbies available = more work to
 * make a new one), but certain users can skip the queue and get a drastically
 * reduced work order, such as those with valid XL keys.
 * 
 * For flexibility, we provide two methods of getting this three-step
 * process done.  First, there is a simple, one-method approach such as
 * "open" or "maintain" which does all the necessary work.  However, if
 * you prefer more granularity, you might prefer the three-step:
 * 
 * workOrder = openRequest()
 * workOrder.performWork()
 * openConfirm( workOrder )
 * 
 * This is exactly the operation performed by open().  The first and last 
 * calls are wrapped WebQueries, whereas the middle call is local but can
 * take a significant amount of CPU burden.  One complication: a particular
 * error code reported by *Confirm indicates that too much time has expired
 * since the work order was issued.  Unlike most other error codes, this
 * indicates that a complete restart of the three-step process is required;
 * for a timeout, by contrast, the third step should probably be repeated.
 * 
 * The good news: in the high-granularity version, the thread on which
 * performWork was called can be shut down relatively easily and the workOrder
 * will be in a consistent state.  Further granularity is supported by repeated
 * calls to performWork( time ), which provides (in milliseconds) the maximum 
 * amount of time spent performing the work.  Note that there is a slight overhead
 * each time this call is performed, so make 'time' as large as possible without
 * interfering with responsiveness.  50-500 milliseconds is a fairly
 * good range.
 * 
 * These granularity options are evident in the various construction methods
 * for MutableInternetLobbies.  The factory method, newOpenLobby( ... ), 
 * performs a complete 3-step operation and either returns a new MutableInternetLobby
 * that was successfully opened, or throw an exception indicating the problem.
 * 
 * On the other hand, the Opener (similar to a Builder) will allow for a granular
 * process that should never throw an exception, but requires more work
 * and care to use.  Careless Opening can, for example, get you stuck repeatedly
 * sending an expired work order, so be sure to check the error code.
 * 
 * Finally, for granular use, WorkOrder should not be treated as a static class; each
 * WorkOrder belongs to a particular MutableInternetLobby (or an Opener), so don't share them.
 * On the other hand, note that WorkOrders are performed independently of the 
 * Lobby object and then provided as a parameter; this reflects the fact that
 * until a WorkOrder is completed and the operation confirmed on a server,
 * the state of the lobby hasn't actually changed at all, and neither should 
 * the Lobby object.
 * 
 * @author Jake
 *
 */
public class MutableInternetLobby extends InternetLobby {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6987982069612692546L;



	protected MutableInternetLobby() {
		// we CREATED this lobby!
		this( ORIGIN_CREATED ) ;
	}
	
	
	protected MutableInternetLobby( int origin ) {
		super() ;
		// we CREATED this lobby!
		mOrigin = origin ;
		
		mEditKey = null ;
	}
	
	
	
	
	/**
	 * A utility class, similar to a Builder, whose job is to provide granularity
	 * in opening new lobbies.  Unlike the factory method, this class is designed
	 * not to throw exceptions; instead, methods will tend to return success
	 * or failure and provide readable error codes.
	 * 
	 * If you're interested, the factory methods for opening a lobby use the Opener
	 * class, checking its returned and error vals and throwing exceptions when
	 * appropriate.
	 * 
	 * Openers are a stateful object; certain information must be set before the first
	 * opening attempt, and after that attempt certain mutators cannot be used.  Violations
	 * will result in communication failures.
	 * 
	 * Another violation: changing elements specified in a work order (e.g., nonce)
	 * and then providing a previous work order.
	 * 
	 * @author Jake
	 *
	 */
	public static class Opener {
		
		int mOpenerLastError ;
		int mOpenerLastErrorReason ;
		
		// here's what we need.
		Nonce mOpenerNonce ;		// OPTIONAL - will be provided by the server if not set
		Nonce mOpenerEditKey ;		// OPTIONAL - will be provided by the server if not set
		
		String mOpenerXLKey ;		// OPTIONAL
		
		Integer mOpenerMaxPlayers ;	// REQUIRED
		Boolean mOpenerIsPublic ;	// REQUIRED
		Boolean mOpenerIsItinerant ; // REQUIRED
		
		int mOpenerOrigin ;			// REQUIRED; DEFAULTS TO CREATED
		
		String mOpenerName ;		// REQUIRED
		String mOpenerDescription ;	// OPTIONAL
		String mOpenerCreatorName ;	// REQUIRED
		
		public Opener() {
			mOpenerLastError = WebConsts.ERROR_NONE ;
			
			mOpenerNonce = null ;
			mOpenerEditKey = null ;
			
			mOpenerXLKey = null ;
			
			mOpenerMaxPlayers = null ;
			mOpenerIsPublic = null ;
			mOpenerIsItinerant = null ;
			
			mOpenerOrigin = ORIGIN_CREATED ;
			
			mOpenerName = null ;
			mOpenerDescription = null ;
			mOpenerCreatorName = null ;
		}
		
		
		public int getLastError() {
			return mOpenerLastError ;
		}
		
		public int getLastErrorReason() {
			return mOpenerLastErrorReason ;
		}
		
		
		public Opener setNonce( Nonce nonce ) {
			mOpenerNonce = nonce ;
			return this ;
		}
		
		public Opener setEditKey( Nonce key ) {
			mOpenerEditKey = key ;
			return this ;
		}
		
		public Opener setXLKey( String xl ) {
			mOpenerXLKey = xl ;
			return this ;
		}
		
		public Opener setMaxPlayers( int players ) {
			mOpenerMaxPlayers = players ;
			return this ;
		}
		
		public Opener setIsPublic( boolean isPublic ) {
			mOpenerIsPublic = isPublic ;
			return this ;
		}
		
		public Opener setIsItinerant( boolean isItinerant ) {
			mOpenerIsItinerant = isItinerant ;
			return this ;
		}
		
		public Opener setOrigin( int origin ) {
			mOpenerOrigin = origin ;
			return this ;
		}
		
		public Opener setName( String name ) {
			mOpenerName = name ;
			return this ;
		}
		
		public Opener setDescription( String description ) {
			mOpenerDescription = description ;
			return this ;
		}
		
		public Opener setCreatorName( String name ) {
			mOpenerCreatorName = name ;
			return this ;
		}
		
		
		public WorkOrder openRequest(  ) {
			return openRequest( 0 ) ;
		}
		
		
		public WorkOrder openRequest( int timeout ) {
			mOpenerLastErrorReason = WebConsts.REASON_NONE ;
			
			// we don't actually need anything set for this to work.
			Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
			vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_OPEN_REQUEST) ;
			
			// an open request should include nonce, key, and xlKey - IF we have them.
			if ( mOpenerNonce != null )
				vars.put(WebConsts.VAR_NONCE, mOpenerNonce) ;
			if ( mOpenerEditKey != null )
				vars.put(WebConsts.VAR_KEY, mOpenerEditKey) ;
			if ( mOpenerXLKey != null )
				vars.put(WebConsts.VAR_XL, mOpenerXLKey) ;
			
			WebQuery webQuery = makeWebQuery( vars ) ;
			
			WebQuery.Response [] responses ;
			try {
				responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
			} catch (Exception e) {
				mOpenerLastError = WebConsts.ERROR_TIMEOUT ;
				return null ;
			}
			
			if ( responses == null || responses.length == 0 ) {
				mOpenerLastError = WebConsts.ERROR_TIMEOUT ;
				return null ;
			}
			
			if ( responses[0].isNO() ) {
				mOpenerLastError = WebConsts.ERROR_REFUSED ;
				mOpenerLastErrorReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
				return null ;
			} else if ( responses[0].isFAIL() ) {
				mOpenerLastError = WebConsts.ERROR_FAILED ;
				return null ;
			}
			
			// it's OK.  Make a work order; that's all the info we need.
			try {
				mOpenerLastError = WebConsts.ERROR_NONE ;
				return new WorkOrder( this, responses[0] ) ;
			} catch( IllegalArgumentException iae ) {
				mOpenerLastError = WebConsts.ERROR_MALFORMED ;
				return null ;
			}
		}
		
		
		public MutableInternetLobby openConfirm( WorkOrder workOrder ) {
			return openConfirm( workOrder, 0 ) ;
		}
		
		public MutableInternetLobby openConfirm( WorkOrder workOrder, int timeout ) {
			if ( workOrder == null || !workOrder.isComplete() ) {
				System.err.println("incomplete work order") ;
				mOpenerLastError = WebConsts.ERROR_INCOMPLETE_WORK_ORDER ;
				return null ;
			}
			
			// check the work order.  It should contain nonce and editKey,
			// and possibly XL key.  Make sure these match our settings OR
			// (in the case of nonce / editKey) that our settings are null.
			
			if ( mOpenerNonce != null && !mOpenerNonce.equals( workOrder.getNonceString() ) ) {
				System.err.println("work order nonce mismatch") ;
				mOpenerLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
				return null ;
			}
			
			if ( mOpenerEditKey != null && !mOpenerEditKey.equals( workOrder.getEditKeyString() ) ) {
				System.err.println("work order key mismatch") ;
				mOpenerLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
				return null ;
			}
			
			if ( mOpenerXLKey != null && !mOpenerXLKey.equals( workOrder.getXLKeyString() ) ) {
				System.err.println("XL key mismatched; " + mOpenerXLKey + " and " + workOrder.getXLKeyString()) ;
				mOpenerLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
				return null ;
			}
			
			if ( !workOrder.requestIsOpen() ) {
				System.err.println("not an'open' request") ;
				mOpenerLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
				return null ;
			}
			
			// Check our settings.  Everything needs to be completely set for this call to work.
			if ( mOpenerMaxPlayers == null || mOpenerMaxPlayers < 2
					|| mOpenerIsPublic == null || mOpenerIsItinerant == null
					|| mOpenerName == null || mOpenerCreatorName == null ) {
				System.err.println("sumpins missing!") ;
				mOpenerLastError = WebConsts.ERROR_ILLEGAL_STATE ;
				return null ;
			}
			
			// report.  Include all work order info, along with all required and optional
			// variables not included in the work order.
			Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
			vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_OPEN_CONFIRM) ;
			workOrder.getVars(vars) ;
			vars.put(WebConsts.VAR_SIZE, mOpenerMaxPlayers) ;
			vars.put(WebConsts.VAR_PUBLIC, mOpenerIsPublic) ;
			vars.put(WebConsts.VAR_ITINERANT, mOpenerIsItinerant) ;
			vars.put(WebConsts.VAR_NAME, mOpenerName) ;
			vars.put(WebConsts.VAR_HOST, mOpenerCreatorName) ;
			if ( mOpenerDescription != null )
				vars.put(WebConsts.VAR_DESCRIPTION, mOpenerDescription) ;
			
			WebQuery webQuery = makeWebQuery( vars ) ;
			WebQuery.Response [] responses ;
			try {
				try {
					responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
				} catch (Exception e) {
					e.printStackTrace() ;
					mOpenerLastError = WebConsts.ERROR_TIMEOUT ;
					return null ;
				}
				
				if ( responses == null || responses.length == 0 ) {
					System.err.println("null or empty response") ;
					mOpenerLastError = WebConsts.ERROR_TIMEOUT ;
					return null ;
				}
				
				if ( responses[0].isNO() ) {
					System.err.println("response NO") ;
					mOpenerLastError = WebConsts.ERROR_REFUSED ;
					mOpenerLastErrorReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
					return null ;
				} else if ( responses[0].isFAIL() ) {
					System.err.println("response FAIL") ;
					mOpenerLastError = WebConsts.ERROR_FAILED ;
					return null ;
				}
			} catch( Exception e ) {
				e.printStackTrace() ;
				mOpenerLastError = WebConsts.ERROR_MALFORMED ;
				return null ;
			}
			
			// It's OK!  Make a lobby.
			MutableInternetLobby lobby = new MutableInternetLobby(mOpenerOrigin) ;
			try {
				lobby.setLobbyNonce( new Nonce( workOrder.getNonceString() ) ) ;
				lobby.mEditKey = new Nonce( workOrder.getEditKeyString() ) ;
				try { // refresh
					lobby.refresh( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
				} catch( CommunicationErrorException e ) {
					System.err.println("did not refresh") ;
					mOpenerLastError = e.getError() ;
					mOpenerLastErrorReason = e.getErrorReason() ;
					return null ;
				}
				
				// that's it, we made it.
				return lobby ;
			} catch (Exception e) {
				e.printStackTrace() ;
				mOpenerLastError = WebConsts.ERROR_UNKNOWN ;
				return null ;
			}
		}
		
	}
	
	
	/**
	 * Constructs and returns a refreshed InternetLobby instance.
	 * For a Mutable lobby, use the MutableLobbyInstance factory method.
	 * 
	 * Attempts to refresh the created lobby.  Will throw an exception if
	 * it cannot be constructed.  Manual construction is of course
	 * possible even if this fails.
	 * 
	 * @param n
	 * @throws CommunicationErrorException 
	 */
	public static MutableInternetLobby newRefreshedInstance( Nonce n, Nonce editKey ) throws CommunicationErrorException {
		return newRefreshedInstance( n, editKey, DEFAULT_TIMEOUT ) ;
	}
	
	
	public static MutableInternetLobby newRefreshedInstance( Nonce n, Nonce editKey, int timeout ) throws CommunicationErrorException {
		MutableInternetLobby lobby = new MutableInternetLobby() ;
		lobby.setLobbyNonce(n) ;
		lobby.mEditKey = editKey ;
		
		// refresh.  Might throw.
		lobby.refresh( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		
		return lobby ;
	}
	
	
	/**
	 * Constructs and returns a refreshed InternetLobby instance.
	 * For a Mutable lobby, use the MutableLobbyInstance factory method.
	 * 
	 * Attempts to refresh the created lobby.  Will throw an exception if
	 * it cannot be constructed.  Manual construction is of course
	 * possible even if this fails.
	 * 
	 * @param n
	 */
	public static InternetLobby newUnrefreshedInstance( Nonce n, Nonce editKey ) {
		MutableInternetLobby l = new MutableInternetLobby() ;
		l.setLobbyNonce(n) ;
		l.mEditKey = editKey ;
		
		return l ;
	}
	
	
	
	/**
	 * A factory method which either returns a new open MutableInternetLobby 
	 * or throws an exception.
	 * 
	 * This method will probably block for a while.  If you want a timing guarantee,
	 * use the 'maxTime' method.
	 * 
	 * @param xlKey
	 * @param lobbyName
	 * @param lobbyDescription
	 * @param maxPlayers
	 * @param isPublic
	 * @param nonce
	 * @param editKey
	 * @return
	 * @throws RequestTimeoutException 
	 * @throws ConfirmTimeoutException 
	 * @throws WorkTimeoutException 
	 * @throws CommunicationErrorException 
	 */
	public static MutableInternetLobby newOpenLobby(
			String xlKey, 
			String lobbyName,
			String lobbyDescription,
			String creatorName,
			int maxPlayers,
			boolean isPublic,
			boolean isItinerant,
			int origin,
			Nonce nonce,
			Nonce editKey ) throws RequestTimeoutException, WorkTimeoutException, ConfirmTimeoutException, CommunicationErrorException {
		
		return newOpenLobby( xlKey, lobbyName, lobbyDescription, creatorName, maxPlayers, isPublic, isItinerant, origin, nonce, editKey, 0, 0 ) ;
	}
	
	
	/**
	 * A factory method which either returns a new open MutableInternetLobby 
	 * or throws an exception.
	 * 
	 * This method will block for (approximately) a maximum of timeout*2 + maxWork.
	 * 
	 * @param xlKey
	 * @param lobbyName
	 * @param lobbyDescription
	 * @param maxPlayers
	 * @param isPublic
	 * @param nonce
	 * @param editKey
	 * @param timeout
	 * @param maxWork
	 * @return
	 * @throws RequestTimeoutException 
	 * @throws WorkTimeoutException 
	 * @throws ConfirmTimeoutException 
	 * @throws CommunicationErrorException 
	 */
	public static MutableInternetLobby newOpenLobby(
			String xlKey, 
			String lobbyName,
			String lobbyDescription,
			String creatorName,
			int maxPlayers,
			boolean isPublic,
			boolean isItinerant,
			int origin,
			Nonce nonce,
			Nonce editKey,
			int timeout,
			int maxWork ) throws RequestTimeoutException, WorkTimeoutException, ConfirmTimeoutException, CommunicationErrorException {
		
		
		// Make an opener.
		Opener opener = new Opener() ;
		
		// set settings
		opener.setXLKey(xlKey)
				.setName(lobbyName)
				.setDescription(lobbyDescription)
				.setCreatorName(creatorName)
				.setMaxPlayers(maxPlayers)
				.setIsPublic(isPublic)
				.setIsItinerant(isItinerant)
				.setOrigin(origin)
				.setNonce(nonce)
				.setEditKey(editKey) ;
		
		// request a work order
		WorkOrder workOrder = opener.openRequest(timeout) ;
		if ( workOrder == null ) {
			if ( opener.getLastError() == WebConsts.ERROR_TIMEOUT )
				throw new RequestTimeoutException() ;
			else {
				throw new RequestErrorException().setError( opener.getLastError(), opener.getLastErrorReason() ) ;
			}
		}
		
		// perform the work
		workOrder.performWork(maxWork) ;
		if ( !workOrder.isComplete() )
			throw new WorkTimeoutException() ;
		
		
		// confirm the work
		MutableInternetLobby lobby = opener.openConfirm(workOrder, timeout) ;
		if ( lobby == null ) {
			if ( opener.getLastError() == WebConsts.ERROR_TIMEOUT )
				throw new ConfirmTimeoutException() ;
			else {
				throw new ConfirmErrorException().setError( opener.getLastError(), opener.getLastErrorReason() ) ;
			}
		}
		
		return lobby ;
	}
			

	protected Nonce mEditKey ;
	
	
	public Nonce getEditKey() {
		return mEditKey ;
	}
	
	
	/**
	 * Maintains this lobby, if it is currently open.  This method may take
	 * a bit of time.
	 * 
	 * Throws an exception upon failure; otherwise you can assume success.
	 * @throws ConfirmTimeoutException 
	 * @throws WorkTimeoutException 
	 * @throws RequestTimeoutException 
	 * @throws CommunicationErrorException 
	 */
	public void maintain() throws RequestTimeoutException, WorkTimeoutException, ConfirmTimeoutException, CommunicationErrorException {
		maintain(0, 0) ;
	}
	
	/**
	 * Maintains
	 * @param timeout
	 * @param maxWork
	 * @throws RequestTimeoutException 
	 * @throws WorkTimeoutException 
	 * @throws ConfirmTimeoutException 
	 * @throws CommunicationErrorException 
	 */
	public void maintain( int timeout, int maxWork ) throws RequestTimeoutException, WorkTimeoutException, ConfirmTimeoutException, CommunicationErrorException {
		// request a work order
		WorkOrder workOrder = maintainRequest(timeout) ;
		if ( workOrder == null ) {
			if ( mLastError == WebConsts.ERROR_TIMEOUT )
				throw new RequestTimeoutException() ;
			else {
				throw new RequestErrorException().setError( mLastError, mLastReason ) ;
			}
		}
		
		// perform the work
		workOrder.performWork(maxWork) ;
		if ( !workOrder.isComplete() )
			throw new WorkTimeoutException() ;
		
		
		// confirm the work
		maintainConfirm(workOrder, timeout) ;
	}
	
	
	
	
	public WorkOrder maintainRequest() throws RequestErrorException, RequestTimeoutException {
		return maintainRequest( 0 ) ;
	}
	
	public WorkOrder maintainRequest( int timeout ) throws RequestErrorException, RequestTimeoutException {
		mLastReason = WebConsts.REASON_NONE ;
		
		// we only maintain open lobbies.
		if ( mStatus != WebConsts.STATUS_OPEN ) {
			mLastError = WebConsts.ERROR_ILLEGAL_STATE ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		}
		
		// maintenance
		Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
		vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_MAINTAIN_REQUEST) ;
		vars.put(WebConsts.VAR_NONCE, nonce) ;
		vars.put(WebConsts.VAR_KEY, mEditKey) ;
		
		WebQuery webQuery = makeWebQuery( vars ) ;
		
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new RequestTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses == null || responses.length == 0 ) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new RequestTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses[0].isNO() ) {
			mLastError = WebConsts.ERROR_REFUSED ;
			mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		} else if ( responses[0].isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		}
		
		// it's OK.  Make a work order; that's all the info we need.
		try {
			mLastError = WebConsts.ERROR_NONE ;
			return new WorkOrder( this, responses[0] ) ;
		} catch( IllegalArgumentException iae ) {
			mLastError = WebConsts.ERROR_MALFORMED ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		}
	}
	
	
	public void maintainConfirm( WorkOrder workOrder ) throws ConfirmErrorException, ConfirmTimeoutException {
		maintainConfirm( workOrder, 0 ) ;
	}
	
	public void maintainConfirm( WorkOrder workOrder, int timeout ) throws ConfirmErrorException, ConfirmTimeoutException {
		mLastReason = WebConsts.REASON_NONE ;
		
		if ( workOrder == null || !workOrder.isComplete() ) {
			mLastError = WebConsts.ERROR_INCOMPLETE_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		// check the work order.  It should contain nonce and editKey,
		// and possibly XL key.  Make sure these match our settings OR
		// (in the case of nonce / editKey) that our settings are null.
		
		if ( nonce != null && !nonce.equals( workOrder.getNonceString() ) ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( mEditKey != null && !mEditKey.equals( workOrder.getEditKeyString() ) ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( !workOrder.requestIsMaintain() ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		// report.  Include all work order info, along with all required and optional
		// variables not included in the work order.
		Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
		vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_MAINTAIN_CONFIRM) ;
		workOrder.getVars(vars) ;
		
		WebQuery webQuery = makeWebQuery( vars ) ;
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new ConfirmTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses == null || responses.length == 0 ) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new ConfirmTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses[0].isNO() ) {
			mLastError = WebConsts.ERROR_REFUSED ;
			mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		} else if ( responses[0].isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		// we might have more information here, such as the amount of time until the next
		// maintenance is needed.
		refreshHelperRefreshFromResponse( responses[0] ) ;
	}
	
	
	
	public InternetLobbyGame openGame() throws RequestTimeoutException, WorkTimeoutException, ConfirmTimeoutException, CommunicationErrorException {
		return openGame( 0, 0 ) ;
	}
	
	
	public InternetLobbyGame openGame( int timeout, int maxWork ) throws RequestTimeoutException, WorkTimeoutException, ConfirmTimeoutException, CommunicationErrorException {
		// request a work order
		WorkOrder workOrder ;
		try {
			workOrder = openGameRequest(timeout) ;
		}  catch ( CommunicationErrorException cee ) {
			if ( mLastError == WebConsts.ERROR_TIMEOUT )
				throw new RequestTimeoutException() ;
			else {
				throw new RequestErrorException().setError( mLastError, mLastReason ) ;
			}
		}
		
		// perform the work
		workOrder.performWork(maxWork) ;
		if ( !workOrder.isComplete() )
			throw new WorkTimeoutException() ;
		
		
		// confirm the work
		InternetLobbyGame game ;
		try {
			game = openGameConfirm( workOrder, timeout ) ;
		} catch ( CommunicationErrorException cee ) {
			if ( mLastError == WebConsts.ERROR_TIMEOUT )
				throw new ConfirmTimeoutException() ;
			else {
				throw new ConfirmErrorException().setError( mLastError, mLastReason ) ;
			}
		}
		
		return game ;
	}
	
	
	public WorkOrder openGameRequest() throws RequestErrorException, RequestTimeoutException {
		return openGameRequest( 0 ) ;
	}
	
	public WorkOrder openGameRequest( int timeout ) throws RequestErrorException, RequestTimeoutException {
		mLastReason = WebConsts.REASON_NONE ;
		
		// we only maintain open lobbies.
		if ( mStatus != WebConsts.STATUS_OPEN ) {
			mLastError = WebConsts.ERROR_ILLEGAL_STATE ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		}
		
		// maintenance
		Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
		vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_OPEN_GAME_REQUEST) ;
		vars.put(WebConsts.VAR_NONCE, nonce) ;
		vars.put(WebConsts.VAR_KEY, mEditKey) ;
		
		WebQuery webQuery = makeWebQuery( vars ) ;
		
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new RequestTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses == null || responses.length == 0 ) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new RequestTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses[0].isNO() ) {
			mLastError = WebConsts.ERROR_REFUSED ;
			mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		} else if ( responses[0].isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		}
		
		// it's OK.  Make a work order; that's all the info we need.
		try {
			mLastError = WebConsts.ERROR_NONE ;
			return new WorkOrder( this, responses[0] ) ;
		} catch( IllegalArgumentException iae ) {
			mLastError = WebConsts.ERROR_MALFORMED ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		}
	}
	
	
	public InternetLobbyGame openGameConfirm( WorkOrder workOrder ) throws ConfirmErrorException, ConfirmTimeoutException {
		return openGameConfirm( workOrder, 0 ) ;
	}
	
	public InternetLobbyGame openGameConfirm( WorkOrder workOrder, int timeout ) throws ConfirmErrorException, ConfirmTimeoutException {
		mLastReason = WebConsts.REASON_NONE ;
			
		if ( workOrder == null || !workOrder.isComplete() ) {
			mLastError = WebConsts.ERROR_INCOMPLETE_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		// check the work order.  It should contain nonce and editKey,
		// and possibly XL key.  Make sure these match our settings OR
		// (in the case of nonce / editKey) that our settings are null.
		
		if ( nonce != null && !nonce.equals( workOrder.getNonceString() ) ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( !workOrder.requestIsOpenGame() ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		// report.  Include all work order info, along with all required and optional
		// variables not included in the work order.
		Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
		vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_OPEN_GAME_CONFIRM) ;
		workOrder.getVars(vars) ;
		
		WebQuery webQuery = makeWebQuery( vars ) ;
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new ConfirmTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses == null || responses.length == 0 ) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new ConfirmTimeoutException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses[0].isNO() ) {
			mLastError = WebConsts.ERROR_REFUSED ;
			mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		} else if ( responses[0].isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		// TODO: create an InternetLobbyGame from the web response and the work order.
		InternetLobbyGame game = null ;
		try {
			game = InternetLobbyGame.newInternetLobbyGame(
					new Nonce( workOrder.getGameNonceString() ),
					new Nonce( workOrder.getGameEditKeyString() ),
					responses[0], -1) ;
			if ( game == null ) {
				mLastError = WebConsts.ERROR_MALFORMED ;
				throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
			}
		} catch ( IOException ioe ) {
			mLastError = WebConsts.ERROR_MALFORMED ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		return game ;
	}
	
	
	
	/**
	 * Returns an ArrayList of current games, as described by the server.  Returns 'null' upon
	 * failure.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public ArrayList<InternetLobbyGame> listGames() throws CommunicationErrorException {
		return listGames( DEFAULT_TIMEOUT ) ;
	}
	
	
	/**
	 * Returns an ArrayList of current games, as described by the server.  Returns 'null' upon
	 * failure.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public ArrayList<InternetLobbyGame> listGames(int timeout) throws CommunicationErrorException {
		mLastError = WebConsts.ERROR_NONE ;
		mLastReason = WebConsts.REASON_NONE ;
		
		WebQuery webQuery = makeSimpleWebQuery(WebConsts.ACTION_LIST_GAMES, nonce, mEditKey) ;
		
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses == null || responses.length == 0 ) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses[0].isNO() ) {
			mLastError = WebConsts.ERROR_REFUSED ;
			mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		} else if ( responses[0].isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		// okay!  Read some InetLobbyGames.
		int numGames = responses[0].getNumberOfSections(WebConsts.RESPONSE_SEPARATOR_GAME) ;
		ArrayList<InternetLobbyGame> list = new ArrayList<InternetLobbyGame>() ;
		for ( int i = 0; i < numGames; i++ ) {
			InternetLobbyGame game = InternetLobbyGame.newInternetLobbyGame(null, null, responses[0], i) ;
			if ( game != null )
				list.add(game) ;
			else {
				mLastError = WebConsts.ERROR_MALFORMED ;
				throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
			}
		}
		
		return list ;
	}
	
	
	
	/**
	 * Close this lobby.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void close() throws CommunicationErrorException {
		close(DEFAULT_TIMEOUT) ;
	}
	
	
	/**
	 * Close this lobby.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void close( int timeout ) throws CommunicationErrorException {
		mLastError = WebConsts.ERROR_NONE ;
		mLastReason = WebConsts.REASON_NONE ;
		
		WebQuery webQuery = makeSimpleWebQuery(WebConsts.ACTION_CLOSE, nonce, mEditKey) ;
		
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses == null || responses.length == 0 ) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses[0].isNO() ) {
			mLastError = WebConsts.ERROR_REFUSED ;
			mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		} else if ( responses[0].isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		// okay!  Update our status.
		mStatus = WebConsts.STATUS_CLOSED ;
	}
	
	
	/**
	 * Set ourself as the host of this lobby.  This is a necessary step every time
	 * our address changes; the matchmaker will reject us as a host if we don't do this.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void host() throws CommunicationErrorException {
		host(DEFAULT_TIMEOUT) ;
	}
	
	
	/**
	 * Set ourself as the host of this lobby.  This is a necessary step every time
	 * our address changes; the matchmaker will reject us as a host if we don't do this.
	 * 
	 * Automatically reports the number of people currently connected.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void host( int timeout ) throws CommunicationErrorException {
		mLastError = WebConsts.ERROR_NONE ;
		mLastReason = WebConsts.REASON_NONE ;
		
		Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
		vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_HOST) ;
		vars.put(WebConsts.VAR_NONCE, nonce) ;
		vars.put(WebConsts.VAR_KEY, mEditKey) ;
		vars.put(WebConsts.VAR_PLAYERS, getNumPeople()) ;
		
		WebQuery webQuery = makeWebQuery(vars) ;
		
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses == null || responses.length == 0 ) {
			mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( responses[0].isNO() ) {
			mLastError = WebConsts.ERROR_REFUSED ;
			mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString( WebConsts.VAR_REASON ) ) ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		} else if ( responses[0].isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		// okay!  Might as well update.
		refreshFromLobbyResponse( responses[0], -1 ) ;
	}
	
	
	/**
	 * Attempts to acquire a matchticket.  The ticket is for a client connection
	 * to this lobby.  If non-null, the provided XL key string is sent to
	 * the server.
	 * 
	 * Subclasses may override this method for different types of connections.
	 * For example, a MutableInternetLobby will most likely request a hosting
	 * matchticket, rather than a client one.
	 * 
	 * This method will either return a 
	 * 
	 * @param timeout
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public MatchTicketOrder matchticketRequest( String user_id, String xl ) throws CommunicationErrorException {
		return matchticketRequest( user_id, xl, DEFAULT_TIMEOUT ) ;
	}
	
	/**
	 * Attempts to acquire a matchticket.  The ticket is for a client connection
	 * to this lobby.  If non-null, the provided XL key string is sent to
	 * the server.
	 * 
	 * Subclasses may override this method for different types of connections.
	 * For example, a MutableInternetLobby will most likely request a hosting
	 * matchticket, rather than a client one.
	 * 
	 * This method will either return a 
	 * 
	 * @param timeout
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public MatchTicketOrder matchticketRequest( String user_id, String xl, int timeout ) throws CommunicationErrorException {
		mLastReason = WebConsts.REASON_NONE ;
		
		if ( mStatus != WebConsts.STATUS_OPEN ) {
			mLastError = WebConsts.ERROR_ILLEGAL_STATE ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		try {
			// Send a status request and refresh ourselves from the response.
			Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
			vars.put( WebConsts.VAR_ACTION, WebConsts.ACTION_MATCH_TICKET_LOBBY ) ;
			vars.put( WebConsts.VAR_NONCE, this.getLobbyNonce() ) ;
			vars.put( WebConsts.VAR_KEY, mEditKey ) ;
			vars.put( WebConsts.VAR_USER_ID, user_id ) ;
			if ( xl != null )
				vars.put( WebConsts.VAR_XL, xl ) ;
			
			WebQuery webQuery = makeWebQuery( vars ) ;
			
			// Send and get response.
			WebQuery.Response [] responses ;
			try {
				responses = webQuery.query( DEFAULT_TIMEOUT ) ;
			} catch ( Exception e ) {
				e.printStackTrace() ;
				mLastError = WebConsts.ERROR_TIMEOUT ;
				throw new CommunicationErrorException().setError(mLastError, WebConsts.REASON_NONE) ;
			}
			
			if ( responses == null || responses.length == 0 ) {
				mLastError = WebConsts.ERROR_TIMEOUT ;
				throw new CommunicationErrorException().setError(mLastError, WebConsts.REASON_NONE) ;
			}
			
			if ( responses[0].isFAIL() ) {
				mLastError = WebConsts.ERROR_FAILED ;
				throw new CommunicationErrorException().setError(mLastError, WebConsts.REASON_NONE) ;
			}
			
			if ( responses[0].isNO() ) {
				mLastError = WebConsts.ERROR_REFUSED ;
				mLastReason = WebConsts.reasonStringToInt( responses[0].getResponseVariableString(WebConsts.VAR_REASON) ) ;
				throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
			}
			
			String ticketString = responses[0].getResponseVariableString(WebConsts.VAR_MATCH_TICKET) ;
			int effort = responses[0].getResponseVariableInteger(WebConsts.VAR_WORK) ;
			
			return new MatchTicketOrder( ticketString, effort ) ;
			
		} catch ( Exception e ) {
			e.printStackTrace() ;
			if ( e instanceof CommunicationErrorException )
				throw (CommunicationErrorException)e ;
			mLastError = WebConsts.ERROR_UNKNOWN ;
			throw new CommunicationErrorException().setError(mLastError, WebConsts.REASON_NONE) ;
		}
	}
	
	
	/**
	 * From the provided keyed values, updates the "challenge" values for this 
	 * challenge (i.e., those not associated with a lobby or game).
	 * @param line The line of text to consider
	 * @param now A Date instance - the time right now.
	 */
	protected void refreshFromLobbyResponse( WebQuery.Response response, int lobbyNum ) {
		
		String keyValue ;
		if ( lobbyNum < 0 )
			keyValue = response.getResponseVariableString(WebConsts.VAR_KEY) ;
		else
			keyValue = response.getResponseVariableString(WebConsts.VAR_KEY, WebConsts.SECTION_HEADER_LOBBY, lobbyNum) ;
		
		if ( keyValue != null ) {
			try {
				mEditKey = new Nonce( keyValue ) ;
			} catch( IOException ioe ) {
				ioe.printStackTrace() ;
				// do nothing
			}
		}
		
		// do the rest
		super.refreshFromLobbyResponse(response, lobbyNum) ;
	}
	
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeObject(mEditKey) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		mEditKey = (Nonce)stream.readObject() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required Challenge structure.") ;
	}
	
	
	@Override
	public synchronized MutableInternetLobby newInstance() {
		MutableInternetLobby l = new MutableInternetLobby() ;
		cloneIntoInstance( l ) ;
		return l ;
	}
	
	protected void cloneIntoInstance( MutableInternetLobby l ) {
		super.cloneIntoInstance(l) ;
		
		l.mEditKey = mEditKey ;
	}
	
	
}
