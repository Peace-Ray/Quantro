package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
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
 * An InternetLobbyGame (should probably think of a better name...) represents
 * a game instance in the same way InternetLobby represents a lobby.  Well,
 * not exactly the same; InternetLobby is an instance of Lobby and contains a lot
 * of Lobby information apart from its Internet-accessible data, whereas 
 * InternetLobbyGame is only and exactly a representation of the 
 * publicly-accessible game data -- nonce, editKey (if available),
 * port, maintenance window, etc.
 * 
 * Most relevant data about the game itself is stored separately.  I could
 * probably hand-wave a justification for this, but the main reason is that
 * Games and their related data and operation objects were written long,
 * long before InternetLobbyGame, which was introduced after release.
 * 
 * Legacy isn't always good.
 * 
 * Operations -- especially mutations -- require server communication and
 * local data processing, leading to the high possibility of failure.  Such
 * operations generally provide two modes of operation -- a monolithic function
 * that either succeeds (altering both the internal state of this object and
 * the online representation) or throws an exception, and a series of "step-functions"
 * that allow granular control but require checking of error-state fields
 * upon failure.
 * 
 * InternetLobbyGames are created through a MutableInternetLobby, although individual
 * objects representing existing ILGs can be constructed locally.
 * 
 * @author Jake
 *
 */
public class InternetLobbyGame implements Serializable {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -388190095484069755L;



	// Network behavior
	protected static final int DEFAULT_TIMEOUT = 5000 ;
	
	

	Nonce mNonce ;
	Nonce mEditKey ;
	
	int mStatus ;
	int mMediatorPort ;
	
	long mMaintenanceNeededBy ;
	long mTimeStarted ;
	
	int mLastError ;
	int mLastReason ;		// in case of refusal, sometimes we get a reason.
	
	
	
	public Nonce getNonce() {
		return mNonce ;
	}
	
	public Nonce getEditKey() {
		return mEditKey ;
	}
	
	public boolean isOpen() {
		return mStatus == WebConsts.STATUS_OPEN ;
	}
	
	public int getMediatorPort() {
		return mMediatorPort ;
	}
	
	public long getMaintenanceNeededBy() {
		return mMaintenanceNeededBy ;
	}
	
	public long getAge() {
		return System.currentTimeMillis() - mTimeStarted ;
	}
	
	public int getLastError() {
		return mLastError ;
	}
	
	public int getLastReason() {
		return mLastReason ;
	}
	
	public int getStatus() {
		return mStatus ;
	}
	
	
	private InternetLobbyGame() {
		mStatus = WebConsts.STATUS_EMPTY ;
		mMediatorPort = -1 ;
		mMaintenanceNeededBy = 0 ;
		mTimeStarted = 0 ;
	}
	
	
	/**
	 * Returns a new InetLobbyGame instance which has been refreshed online.
	 * 
	 * Throws an exception instead if the refresh attempt fails.
	 * 
	 * @param nonce
	 * @param timeout
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public static InternetLobbyGame newRefreshedInternetLobbyGame( Nonce nonce, Nonce editKey ) throws CommunicationErrorException {
		return newRefreshedInternetLobbyGame( nonce, editKey, DEFAULT_TIMEOUT ) ;
	}
	
	
	/**
	 * Returns a new InetLobbyGame instance which has been refreshed online.
	 * 
	 * Throws an exception instead if the refresh attempt fails.
	 * 
	 * @param nonce
	 * @param timeout
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public static InternetLobbyGame newRefreshedInternetLobbyGame( Nonce nonce, Nonce editKey, int timeout ) throws CommunicationErrorException {
		if ( nonce == null )
			throw new NullPointerException("Must provide non-null nonce.") ;
		
		try {
			InternetLobbyGame game = new InternetLobbyGame() ;
			game.mNonce = nonce ;
			game.mEditKey = editKey ;
			
			game.refresh(timeout) ;
			
			return game ;
		
		} catch ( Exception e ) {
			throw new CommunicationErrorException().setError(WebConsts.ERROR_UNKNOWN, WebConsts.REASON_NONE) ;
		}
	}
	
	
	/**
	 * Returns a new InetLobbyGame instance which has been refreshed online.
	 * 
	 * Throws an exception instead if the refresh attempt fails.
	 * 
	 * @param nonce
	 * @param timeout
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public static InternetLobbyGame newUnrefreshedInternetLobbyGame( Nonce nonce, Nonce editKey ) {
		if ( nonce == null )
			throw new NullPointerException("Nonce must be provided.") ;
		InternetLobbyGame game = new InternetLobbyGame() ;
		game.mNonce = nonce ;
		game.mEditKey = editKey ;
		
		return game ;
	}
	
	
	
	/**
	 * A specialty constructor.  Makes a new InternetLobbyGame using the web query
	 * response provided.
	 * 
	 * Returns null if the necessary information is not contained within.
	 * 
	 * @param statusResponse A response containing game status.
	 * @param headerNum How deep in the response should we go?
	 */
	static InternetLobbyGame newInternetLobbyGame( Nonce nonce, Nonce editKey, WebQuery.Response statusResponse, int headerNum ) {
		InternetLobbyGame game = new InternetLobbyGame() ;
		
		game.mNonce = nonce ;
		game.mEditKey = editKey ;
		
		game.refreshFromGameResponse( statusResponse, headerNum ) ;
		if (game.mNonce == null
				|| game.mStatus == WebConsts.STATUS_EMPTY
				|| game.mMediatorPort == -1
				|| game.mMaintenanceNeededBy == 0
				|| game.mTimeStarted == 0 )
			return null ;
		
		return game ;
	}
	
	
	
	/**
	 * Maintains this game, if it is currently open.  This method may take
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
		if ( mStatus != WebConsts.STATUS_OPEN && mStatus != WebConsts.STATUS_EMPTY ) {
			mLastError = WebConsts.ERROR_ILLEGAL_STATE ;
			throw new RequestErrorException().setError(mLastError, mLastReason) ;
		}
		
		// maintenance
		Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
		vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_MAINTAIN_GAME_REQUEST) ;
		vars.put(WebConsts.VAR_NONCE, mNonce) ;
		vars.put(WebConsts.VAR_KEY, mEditKey) ;
		
		WebQuery webQuery = makeWebQuery( vars ) ;
		
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query( timeout > 0 ? timeout : DEFAULT_TIMEOUT ) ;
		} catch (Exception e) {
			//e.printStackTrace() ;
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
		
		if ( mNonce != null && !mNonce.equals( workOrder.getNonceString() ) ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( mEditKey != null && !mEditKey.equals( workOrder.getEditKeyString() ) ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		if ( !workOrder.requestIsMaintainGame() ) {
			mLastError = WebConsts.ERROR_MISMATCHED_WORK_ORDER ;
			throw new ConfirmErrorException().setError(mLastError, mLastReason) ;
		}
		
		// report.  Include all work order info, along with all required and optional
		// variables not included in the work order.
		Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
		vars.put(WebConsts.VAR_ACTION, WebConsts.ACTION_MAINTAIN_GAME_CONFIRM) ;
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
		refreshFromGameResponse( responses[0], -1 ) ;
	}
	
	
	
	/**
	 * Refresh as much data as possible from 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void refresh() throws CommunicationErrorException {
		refresh(DEFAULT_TIMEOUT) ;
	}
	
	public void refresh(int timeout) throws CommunicationErrorException {
		mLastReason = WebConsts.REASON_NONE ;
		
		if ( mStatus == WebConsts.STATUS_REMOVED )
			return ;
			
		
		try {
			// Send a status request and refresh ourselves from the response.
			WebQuery webQuery = makeSimpleWebQuery( WebConsts.ACTION_STATUS_GAME, mNonce, null ) ;
			// Send and get response.
			WebQuery.Response [] responses ;
			try {
				responses = webQuery.query( DEFAULT_TIMEOUT ) ;
			} catch ( Exception e ) {
				//e.printStackTrace() ;
				mLastError = WebConsts.ERROR_TIMEOUT ;
				throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
			}
			
			if ( responses == null || responses.length == 0 ) {
				mLastError = WebConsts.ERROR_TIMEOUT ;
				throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
			}
			
			boolean ok = refreshHelperRefreshFromResponse( responses[0] ) ;
			
			if ( !ok )
				throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
			
		} catch ( Exception e ) {
			//e.printStackTrace() ;
			if ( e instanceof CommunicationErrorException )
				throw (CommunicationErrorException)e ;
			mLastError = WebConsts.ERROR_UNKNOWN ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
	}
	
	
	
	/**
	 * Set ourself as the host of this game.  This is a necessary step every time
	 * our address changes; the matchmaker will reject us as a host if we don't do this.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void close() throws CommunicationErrorException {
		close(DEFAULT_TIMEOUT) ;
	}
	
	
	/**
	 * Set ourself as the host of this game.  This is a necessary step every time
	 * our address changes; the matchmaker will reject us as a host if we don't do this.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void close( int timeout ) throws CommunicationErrorException {
		mLastReason = WebConsts.REASON_NONE ;
		
		WebQuery webQuery = makeSimpleWebQuery(WebConsts.ACTION_CLOSE_GAME, mNonce, mEditKey) ;
		
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
	 * Set ourself as the host of this game.  This is a necessary step every time
	 * our address changes; the matchmaker will reject us as a host if we don't do this.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void host() throws CommunicationErrorException {
		host(DEFAULT_TIMEOUT) ;
	}
	
	
	/**
	 * Set ourself as the host of this game.  This is a necessary step every time
	 * our address changes; the matchmaker will reject us as a host if we don't do this.
	 * 
	 * @return
	 * @throws CommunicationErrorException 
	 */
	public void host( int timeout ) throws CommunicationErrorException {
		mLastReason = WebConsts.REASON_NONE ;
		
		WebQuery webQuery = makeSimpleWebQuery(WebConsts.ACTION_HOST_GAME, mNonce, mEditKey) ;
		
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
		refreshFromGameResponse( responses[0], -1 ) ;
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
		
		if ( mStatus != WebConsts.STATUS_OPEN && mStatus != WebConsts.STATUS_EMPTY ) {
			//System.err.println("Status is " + mStatus) ;
			mLastError = WebConsts.ERROR_ILLEGAL_STATE ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
		
		try {
			// Send a status request and refresh ourselves from the response.
			Hashtable<String, Object> vars = new Hashtable<String, Object>() ;
			vars.put( WebConsts.VAR_ACTION, WebConsts.ACTION_MATCH_TICKET_GAME ) ;
			vars.put( WebConsts.VAR_NONCE, mNonce ) ;
			if ( mEditKey != null )
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
				//e.printStackTrace() ;
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
			//e.printStackTrace() ;
			if ( e instanceof CommunicationErrorException )
				throw (CommunicationErrorException)e ;
			mLastError = WebConsts.ERROR_UNKNOWN ;
			throw new CommunicationErrorException().setError(mLastError, WebConsts.REASON_NONE) ;
		}
	}
	
	
	
	protected boolean refreshHelperRefreshFromResponse( WebQuery.Response response ) {
		// Check status of response
		if ( response.isOK() ) {
			// Refresh lobby...
			try {
				refreshFromGameResponse( response, -1 ) ;
			} catch ( Exception e ) {
				//e.printStackTrace() ;
				mLastError = WebConsts.ERROR_MALFORMED ;
				return false ;
			}
		}
		else if ( response.isNO() ) {
			mStatus = WebConsts.STATUS_REMOVED ;
		}
		else if  ( response.isFAIL() ) {
			mLastError = WebConsts.ERROR_FAILED ;
			return false ;
		}
		
	    return true ;
	}
	
	/**
	 * From the provided keyed values, updates the "challenge" values for this 
	 * challenge (i.e., those not associated with a lobby or game).
	 * @param line The line of text to consider
	 * @param now A Date instance - the time right now.
	 */
	protected void refreshFromGameResponse( WebQuery.Response response, int gameNum ) {
		
		Enumeration<String> keys ;
		if ( gameNum < 0 )
			keys = response.getVariables() ;
		else
			keys = response.getVariables(WebConsts.SECTION_HEADER_GAME, gameNum) ;
		
		for ( ; keys.hasMoreElements() ; ) {
			String name = keys.nextElement() ;
			int keyCode = WebConsts.VAR_CODES.get(name) ;
			String value ;
			
			if ( gameNum < 0 )
				value = response.getResponseVariableString(name) ;
			else
				value = response.getResponseVariableString(name, WebConsts.SECTION_HEADER_GAME, gameNum) ;
			
			if ( keyCode == WebConsts.VAR_KEY_CODE_GAME_NONCE ) {
				try {
					if ( this.mNonce == null || !this.mNonce.equals(value) )
						this.mNonce = new Nonce(value) ;
				} catch( IOException e ) {
					throw new RuntimeException("Nonce provided is poorly formatted!") ;
				}
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_GAME_KEY ) {
				try {
					if ( this.mEditKey == null || !this.mEditKey.equals(value) )
						this.mEditKey = new Nonce(value) ;
				} catch( IOException e ) {
					throw new RuntimeException("Edit key provided is poorly formatted!") ;
				}
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_STATUS ) {
				mStatus = (Integer)WebConsts.STATUS_CODES.get(value) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_PORT ) {
				mMediatorPort = Integer.parseInt(value) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_AGE ) {
				mTimeStarted = System.currentTimeMillis() - Math.round(Double.parseDouble(value) * 1000) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_SECONDS ) {
				mMaintenanceNeededBy = Math.round(
						System.currentTimeMillis()
						+ Math.max(0, Double.parseDouble(value) * 1000) ) ;
			}
		}
	}
	
	
	protected static WebQuery makeWebQuery( Hashtable<String, Object> args ) {
		WebQuery.Builder builder = new WebQuery.Builder() ;
		builder.setURL(WebConsts.QUANTRO_LOBBY_WEB_URL) ;
		builder.setResponseTypeTerseVariableList(WebConsts.VAR_NAMES) ;
		builder.setPostVariables(args) ;
		return builder.build() ;
	}
	
	protected static WebQuery makeGetDescriptionWebQuery( Nonce nonce ) {
		WebQuery.Builder builder = new WebQuery.Builder() ;
		builder.setURL(WebConsts.QUANTRO_LOBBY_WEB_URL) ;
		builder.setResponseTypeString() ;
		builder.addPostVariable(WebConsts.VAR_ACTION, WebConsts.ACTION_DESCRIPTION) ;
		builder.addPostVariable(WebConsts.VAR_NONCE, nonce) ;
		return builder.build() ;
	}
	
	
	protected static WebQuery makeSimpleWebQuery( String postAction, Nonce nonce, Nonce key ) {
		WebQuery.Builder builder = new WebQuery.Builder() ;
		builder.setURL(WebConsts.QUANTRO_LOBBY_WEB_URL) ;
		builder.setResponseTypeTerseVariableList(WebConsts.VAR_NAMES) ;
		builder.addPostVariable(WebConsts.VAR_ACTION, postAction) ;
		if ( nonce != null )
			builder.addPostVariable(WebConsts.VAR_NONCE, nonce) ;
		if ( key != null )
			builder.addPostVariable(WebConsts.VAR_KEY, key) ;
		return builder.build() ;
	}
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeObject(mNonce) ;
		stream.writeObject(mEditKey) ;
		
		stream.writeInt(mStatus) ;
		stream.writeInt(mMediatorPort) ;
		
		stream.writeLong(mMaintenanceNeededBy) ;
		stream.writeLong(mTimeStarted) ;
		
		stream.writeInt(mLastError) ;
		stream.writeInt(mLastReason) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		mNonce = (Nonce)stream.readObject() ;
		mEditKey = (Nonce)stream.readObject() ;
		
		mStatus = stream.readInt() ;
		mMediatorPort = stream.readInt() ;
		
		mMaintenanceNeededBy = stream.readLong() ;
		mTimeStarted = stream.readLong() ;
		
		mLastError = stream.readInt() ;
		mLastReason = stream.readInt() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required Challenge structure.") ;
	}
	
	
	public synchronized InternetLobbyGame newInstance() {
		InternetLobbyGame ilg = new InternetLobbyGame() ;
		cloneIntoInstance( ilg ) ;
		return ilg ;
	}
	
	protected void cloneIntoInstance( InternetLobbyGame g ) {
		
		g.mNonce = mNonce ;
		g.mEditKey = mEditKey ;
		
		g.mStatus = mStatus ;
		g.mMediatorPort = mMediatorPort ;
		
		g.mMaintenanceNeededBy = mMaintenanceNeededBy ;
		g.mTimeStarted = mTimeStarted ;
		
		g.mLastError = mLastError ;
		g.mLastReason = mLastReason ;
	}
	
}
