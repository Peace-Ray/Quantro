package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.web.WebQuery;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;


/**
 * Represents a Lobby on the Internet.  Internet Lobbies are in most
 * ways similar to Lobbies, with a few exceptions.  First, because information
 * about them is available online, they can update their own values,
 * such as lobbyName, maxPlayers, nonce, etc.  Normal Lobbies do not have
 * the capacity to do this (their values are set from outside), whereas
 * an Internet Lobby is meant to represent - as closely as possible - information
 * accessible via web queries.
 * 
 * Some operations, as they require a web query, will likely produce a significant
 * delay.  You may want to perform them in a separate thread.
 * 
 * @author Jake
 *
 */
public class InternetLobby extends Lobby implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1747451626776795317L;

	// Network behavior
	protected static final int DEFAULT_TIMEOUT = 8000 ;
	
	protected int mStatus ;
	protected String mDescription ;
	protected int mMediatorPort ;
	protected boolean mPublic ;
	protected boolean mItinerant ;
	protected String mHostName ;
	
	protected int mHostedPlayers ;
	
	protected long mMaintenanceNeededBy ;		// as System.currentTimeMillis()
	
	protected int mOrigin ;
	
	protected long mLastRefreshAt ;
	protected long mLastOpenRefreshAt ;
	
	protected int mLastError ;
	protected int mLastReason ;		// in case of refusal, sometimes we get a reason.
	
	
	
	
	public static final int ORIGIN_UNKNOWN = -1 ;
	public static final int ORIGIN_PUBLIC_LIST = 0 ;	// retrieved from a public lobby list.
	public static final int ORIGIN_INVITATION = 1 ;		// an invitation received from outside
	public static final int ORIGIN_CREATED = 2 ;		// locally created.
	
	
	
	public boolean isOpen() {
		return mStatus == WebConsts.STATUS_OPEN ;
	}
	
	public int getStatus() {
		return mStatus ;
	}
	
	public String getDescription() {
		return mDescription ;
	}
	
	public int getMediatorPort() {
		return mMediatorPort ;
	}
	
	public boolean isPublic() {
		return mPublic ;
	}
	
	public boolean isItinerant() {
		return mItinerant ;
	}
	
	public String getHostName() {
		return mHostName ;
	}
	
	public long getMaintenanceNeededBy() {
		return mMaintenanceNeededBy ;
	}
	
	public int getLastError() {
		return mLastError ;
	}
	
	public int getLastReason() {
		return mLastReason ;
	}
	
	public int getHostedPlayers() {
		return mHostedPlayers ;
	}
	
	public int getOrigin() {
		return mOrigin ;
	}
	
	public long getLastRefreshAt() {
		return mLastRefreshAt ;
	}
	
	public long getLastRefreshTimeSince() {
		return System.currentTimeMillis() - mLastRefreshAt ;
	}
	
	@Override
	public Nonce getSessionNonce() {
		return getLobbyNonce() ;
	}
	
	
	protected InternetLobby() {
		super() ;
		
		mStatus = WebConsts.STATUS_EMPTY ;
		mDescription = null ;
		mMediatorPort = -1 ;
		mPublic = false ;
		mItinerant = false ;
		mHostName = null ;
		
		mOrigin = ORIGIN_UNKNOWN ;
		
		mMaintenanceNeededBy = 0 ;
		
		mLastRefreshAt = 0 ;
		
		timeStarted = -1 ;
		
		mLastError = WebConsts.ERROR_NONE ;
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
	public static InternetLobby newRefreshedInstance( Nonce n, int origin ) throws CommunicationErrorException {
		return newRefreshedInstance( n, origin, DEFAULT_TIMEOUT ) ;
	}
	
	
	public static InternetLobby newRefreshedInstance( Nonce n, int origin, int timeout ) throws CommunicationErrorException {
		InternetLobby lobby = new InternetLobby() ;
		lobby.setLobbyNonce(n) ;
		lobby.mOrigin = origin ;
		
		// refresh the lobby.  Might throw.
		lobby.refresh( timeout ) ;
		
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
	 * @throws CommunicationErrorException 
	 */
	public static InternetLobby newUnrefreshedInstance( Nonce n, int origin ) {
		InternetLobby l = new InternetLobby() ;
		l.setLobbyNonce(n) ;
		l.mOrigin = origin ;
		
		return l ;
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
	public static InternetLobby newUnrefreshedInstance( Nonce n, boolean isPublic, boolean isItinerant, int origin ) {
		InternetLobby l = new InternetLobby() ;
		l.setLobbyNonce(n) ;
		l.mPublic = isPublic ;
		l.mItinerant = isItinerant ;
		l.mOrigin = origin ;
		
		return l ;
	}
	
	
	
	public static InternetLobby newUnrefreshedInstance(
			Nonce n, int status, long timeCreated, String host, String lobbyName, String lobbyDescription, boolean isPublic, boolean isItinerant, int origin ) {
		
		InternetLobby l = new InternetLobby() ;
		l.setLobbyNonce(n) ;
		l.mStatus = status ;
		l.setAge( timeCreated == -1 ? -1 : System.currentTimeMillis() - timeCreated ) ;
		l.mHostName = host ;
		l.setLobbyName(lobbyName) ;
		l.mDescription = lobbyDescription ;
		l.mPublic = isPublic ;
		l.mItinerant = isItinerant ;
		l.mOrigin = origin ;
		
		return l ;
	}
	
	
	
	
	/**
	 * Retrieves a lobby list from the server, setting the provided ArrayList to
	 * the lobbies returned.  The list will be cleared if not empty.  'splitNonce'
	 * may be null; if provided, the nonces returned will begin at the provided
	 * nonce and progress from there (looping).  One possible usage is to provide
	 * as a split nonce the last nonce returned by the previous call (thus building
	 * a list piece-by-piece), or retrieving a different chunk of the list between
	 * different users to prevent a concentration of users amongst a portion of lobbies.
	 * 
	 * @param lobbies
	 * @param splitNonce
	 * @param autorefresh - if 'true', we call 'refresh' on each lobby returned. 
	 * 			Each lobby will have its own error code. 
	 * 			if 'false', the lobbies returned will be quite sparse in information
	 * 			and should be manually refreshed before examination.
	 * 
	 * @return The error code of the list attempt.  If 'autorefresh' is true, each Lobby
	 * 			will have its own refresh error code.
	 * @throws CommunicationErrorException 
	 */
	public static void getLobbyList( ArrayList<InternetLobby> lobbies, Nonce splitNonce, boolean autorefresh ) throws CommunicationErrorException {
		getLobbyList( lobbies, splitNonce, autorefresh, DEFAULT_TIMEOUT ) ;
	}
	
	/**
	 * Retrieves a lobby list from the server, setting the provided ArrayList to
	 * the lobbies returned.  The list will be cleared if not empty.  'splitNonce'
	 * may be null; if provided, the nonces returned will begin at the provided
	 * nonce and progress from there (looping).  One possible usage is to provide
	 * as a split nonce the last nonce returned by the previous call (thus building
	 * a list piece-by-piece), or retrieving a different chunk of the list between
	 * different users to prevent a concentration of users amongst a portion of lobbies.
	 * 
	 * @param lobbies
	 * @param splitNonce
	 * @param autorefresh - if 'true', we call 'refresh' on each lobby returned.  
	 * 			if 'false', the lobbies returned will be quite sparse in information
	 * 			and should be manually refreshed before examination.
	 * @param timeout - the maximum amount of time to wait for a response.  
	 * @return The error code of the list attempt.  If 'autorefresh' is true, each Lobby
	 * 			will have its own refresh error code.
	 * @throws CommunicationErrorException 
	 */
	public static void getLobbyList( ArrayList<InternetLobby> lobbies, Nonce splitNonce, boolean autorefresh, int timeout ) throws CommunicationErrorException {
		
		lobbies.clear() ;
		
		try {
			// step one is to get a lobby list.
			WebQuery webQuery = makeSimpleWebQuery( WebConsts.ACTION_LIST, null, null ) ;
			WebQuery.Response [] responses ;
			try {
				responses = webQuery.query( DEFAULT_TIMEOUT ) ;
			} catch ( Exception e ) {
				//e.printStackTrace() ;
				throw new CommunicationErrorException().setError(WebConsts.ERROR_TIMEOUT, WebConsts.REASON_NONE) ;
			}
			
			if ( responses == null || responses.length == 0 ) {
				throw new CommunicationErrorException().setError(WebConsts.ERROR_TIMEOUT, WebConsts.REASON_NONE) ;
			}
			
			if ( responses[0].isNO() )
				throw new CommunicationErrorException().setError(WebConsts.ERROR_REFUSED, WebConsts.REASON_NONE) ;
			else if ( responses[0].isFAIL() )
				throw new CommunicationErrorException().setError(WebConsts.ERROR_FAILED, WebConsts.REASON_NONE) ;
			
			// this should be a list of nonces, separated by [L].
			int numLobbies = responses[0].getNumberOfSections(WebConsts.SECTION_HEADER_LOBBY) ;
			for ( int i = 0; i < numLobbies; i++ ) {
				InternetLobby il = new InternetLobby() ;
				il.setLobbyNonce(responses[0].getResponseVariableNonce(WebConsts.VAR_NONCE, WebConsts.SECTION_HEADER_LOBBY, i)) ;
				il.mOrigin = ORIGIN_PUBLIC_LIST ;
				lobbies.add(il) ;
			}
			
			// now -- SECOND STEP!  If requested, we refreshAll.
			if ( autorefresh )
				refreshAll( lobbies, timeout ) ;
		} catch ( Exception e ) {
			//e.printStackTrace() ;
			if ( e instanceof CommunicationErrorException )
				throw (CommunicationErrorException)e ;
			throw new CommunicationErrorException().setError(WebConsts.ERROR_UNKNOWN, WebConsts.REASON_NONE) ;
		}
	}
	
	
	/**
	 * Refreshes every lobby in the list; equivalent to calling .refresh() on
	 * each and ignoring the returned result.
	 * 
	 * This function is more efficient than individual .refresh() calls, especially
	 * in the case that the connection times out.
	 * 
	 * Error codes should be checked.  In the event of a timeout, 
	 * 
	 * @param lobbies
	 * @throws CommunicationErrorException 
	 */
	public static void refreshAll( ArrayList<InternetLobby> lobbies ) throws CommunicationErrorException {
		refreshAll( lobbies, DEFAULT_TIMEOUT ) ;
	}
	
	public static void refreshAll( ArrayList<InternetLobby> lobbies, int timeout ) throws CommunicationErrorException {
		if ( lobbies.size() == 0 )
			return ;
		
		Hashtable<String, Object> keyedRequest = new Hashtable<String,Object>() ;
		try {
			StringBuilder nonceValueHackBuilder = new StringBuilder() ;
			nonceValueHackBuilder.append( lobbies.get(0).getLobbyNonce() ) ;
			for ( int i = 1 ; i < lobbies.size() ; i++ ) {
				nonceValueHackBuilder
					.append( WebConsts.POST_VALUE_SEPARATOR )
					.append( lobbies.get(i).getLobbyNonce() ) ;
			}
			keyedRequest.put(WebConsts.VAR_ACTION, WebConsts.ACTION_STATUS) ;
			keyedRequest.put(WebConsts.VAR_NONCE, nonceValueHackBuilder.toString() ) ;
		} catch( Exception e ) {
			// note the error.
			for ( int i = 0; i < lobbies.size(); i++ )
				lobbies.get(i).mLastError = WebConsts.ERROR_UNKNOWN ;
			throw new CommunicationErrorException().setError(WebConsts.ERROR_UNKNOWN, WebConsts.REASON_NONE) ;
		}
		
		// Query and get a response.
		WebQuery webQuery = makeWebQuery(keyedRequest) ;
		
		WebQuery.Response [] responses ;
		try {
			responses = webQuery.query(timeout) ;
		} catch( Exception e ) {
			//e.printStackTrace() ;
			responses = null ;
		}
		
		if ( responses == null ) {
			for ( int i = 0; i < lobbies.size(); i++ )
				lobbies.get(i).mLastError = WebConsts.ERROR_TIMEOUT ;
			throw new CommunicationErrorException().setError(WebConsts.ERROR_TIMEOUT, WebConsts.REASON_NONE) ;
		}
		
		if ( responses.length < lobbies.size() ) {
			for ( int i = responses.length; i < lobbies.size(); i++ )
				lobbies.get(i).mLastError = WebConsts.ERROR_BLANK ;
		}
		
		for ( int i = 0; i < responses.length; i++ ) {
			if ( !lobbies.get(i).refreshHelperRefreshFromResponse(responses[i]) ) {
				throw new CommunicationErrorException().setError(WebConsts.ERROR_MALFORMED, WebConsts.REASON_NONE) ;
			}
		}
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
			WebQuery webQuery = makeSimpleWebQuery( WebConsts.ACTION_STATUS, nonce, null ) ;
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
			
			boolean ok = refreshHelperRefreshFromResponse( responses[0] ) ;
			
			if ( ok && mDescription == null )
				refreshDescription(timeout) ;
			
			if ( !ok )
				throw new CommunicationErrorException().setError(mLastError, WebConsts.REASON_NONE) ;
			
		} catch ( Exception e ) {
			//e.printStackTrace() ;
			if ( e instanceof CommunicationErrorException )
				throw (CommunicationErrorException)e ;
			mLastError = WebConsts.ERROR_UNKNOWN ;
			throw new CommunicationErrorException().setError(mLastError, WebConsts.REASON_NONE) ;
		}
	}
	
	public void refreshDescription() throws CommunicationErrorException {
		refreshDescription( DEFAULT_TIMEOUT ) ;
	}
	
	public void refreshDescription( int timeout ) throws CommunicationErrorException {
		mLastReason = WebConsts.REASON_NONE ;
		try {
			// Send a status request and refresh ourselves from the response.
			WebQuery webQuery = makeGetDescriptionWebQuery( nonce ) ;
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
			
			if ( responses[0].isOK() ) {
				mDescription = responses[0].getResponseString() ;
				mLastError = WebConsts.ERROR_NONE ; 
				return ;
			} else if ( responses[0].isNO() ) {
				mLastError = WebConsts.ERROR_REFUSED ;
				throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
			} else {
				mLastError = WebConsts.ERROR_FAILED ;
				throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
			}
		} catch ( Exception e ) {
			//e.printStackTrace() ;
			if ( e instanceof CommunicationErrorException )
				throw (CommunicationErrorException)e ;
			mLastError = WebConsts.ERROR_UNKNOWN ;
			throw new CommunicationErrorException().setError(mLastError, mLastReason) ;
		}
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
				refreshFromLobbyResponse( response, -1 ) ;
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
	protected void refreshFromLobbyResponse( WebQuery.Response response, int lobbyNum ) {
		
		Enumeration<String> keys ;
		if ( lobbyNum < 0 )
			keys = response.getVariables() ;
		else
			keys = response.getVariables(WebConsts.RESPONSE_SEPARATOR_LOBBY, lobbyNum) ;
		
		for ( ; keys.hasMoreElements() ; ) {
			String name = keys.nextElement() ;
			int keyCode = WebConsts.VAR_CODES.get(name) ;
			String value ;
			
			if ( lobbyNum < 0 )
				value = response.getResponseVariableString(name) ;
			else
				value = response.getResponseVariableString(name, WebConsts.SECTION_HEADER_LOBBY, lobbyNum) ;
			
			if ( keyCode == WebConsts.VAR_KEY_CODE_NONCE ) {
				try {
					if ( this.nonce == null || !this.nonce.equals(value) )
						this.nonce = new Nonce(value) ;
				} catch( IOException e ) {
					throw new RuntimeException("Nonce provided is poorly formatted!") ;
				}
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_STATUS ) {
				mStatus = (Integer)WebConsts.STATUS_CODES.get(value.toLowerCase()) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_PORT ) {
				mMediatorPort = Integer.parseInt(value) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_NAME ) {
				this.lobbyName = value ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_DESCRIPTION ) {
				mDescription = value ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_SIZE ) {
				this.setMaxPlayers( Integer.parseInt(value) ) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_PUBLIC ) {
				mPublic = Boolean.parseBoolean(value) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_ITINERANT ) {
				mItinerant = Boolean.parseBoolean(value) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_HOST ) {
				mHostName = value ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_AGE ) {
				timeStarted = System.currentTimeMillis() - (long)Math.round(Double.parseDouble(value) * 1000) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_SECONDS ) {
				mMaintenanceNeededBy = Math.round(
						System.currentTimeMillis()
						+ Math.max(0, Double.parseDouble(value) * 1000) ) ;
			} else if ( keyCode == WebConsts.VAR_KEY_CODE_PLAYERS ) {
				mHostedPlayers = Integer.parseInt(value) ;
			}
		}
		
		mLastRefreshAt = System.currentTimeMillis() ;
	}
	
	
	protected static WebQuery makeWebQuery( Hashtable<String, Object> args ) {
		WebQuery.Builder builder = new WebQuery.Builder() ;
		builder.setURL(WebConsts.QUANTRO_LOBBY_WEB_URL) ;
		builder.setResponseTypeTerseVariableList(WebConsts.VAR_NAMES) ;
		builder.setSectionHeaders(WebConsts.SECTION_HEADERS) ;
		builder.setResponseSeparator(WebConsts.RESPONSE_SEPARATOR_LOBBY) ;
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
		builder.setSectionHeaders(WebConsts.SECTION_HEADERS) ;
		builder.setResponseSeparator(WebConsts.RESPONSE_SEPARATOR_LOBBY) ;
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
		stream.writeInt(mStatus) ;
		stream.writeObject(mDescription) ;
		stream.writeInt( mMediatorPort ) ;
		stream.writeBoolean(mPublic) ;
		stream.writeBoolean(mItinerant) ;
		stream.writeObject(mHostName) ;
		
		stream.writeLong( mMaintenanceNeededBy ) ;
		stream.writeInt( mOrigin ) ;
		
		stream.writeInt(mLastError) ;
		stream.writeInt(mLastReason) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		mStatus = stream.readInt() ;
		mDescription = (String)stream.readObject() ;
		mMediatorPort = stream.readInt() ;
		mPublic = stream.readBoolean() ;
		mItinerant = stream.readBoolean() ;
		mHostName = (String)stream.readObject() ;
		
		mMaintenanceNeededBy = stream.readLong() ;
		mOrigin = stream.readInt() ;
		
		mLastError = stream.readInt() ;
		mLastReason = stream.readInt() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required Challenge structure.") ;
	}
	
	@Override
	public synchronized InternetLobby newInstance() {
		InternetLobby l = new InternetLobby() ;
		cloneIntoInstance( l ) ;
		return l ;
	}
	
	protected void cloneIntoInstance( InternetLobby l ) {
		super.cloneIntoInstance(l) ;
		
		l.mStatus = mStatus ;
		l.mDescription = mDescription ;
		l.mMediatorPort = mMediatorPort ;
		l.mPublic = mPublic ;
		l.mItinerant = mItinerant ;
		l.mHostName = mHostName ;
		
		l.mHostedPlayers = mHostedPlayers ;
		
		l.mMaintenanceNeededBy = mMaintenanceNeededBy ;
		
		l.mOrigin = mOrigin ;
		
		l.mLastError = mLastError ;
		l.mLastReason = mLastReason ;
	}
	
}
