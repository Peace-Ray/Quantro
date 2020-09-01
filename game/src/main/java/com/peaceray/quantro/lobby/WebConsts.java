package com.peaceray.quantro.lobby;

import java.util.ArrayList;
import java.util.Hashtable;

public class WebConsts {

	// STATUS
	public static final int STATUS_EMPTY = -1 ;
	public static final int STATUS_OPEN = 0 ;
	public static final int STATUS_CLOSED = 1 ;
	public static final int STATUS_REMOVED = 2 ;
	
	static final String RESPONSE_STATUS_OPEN = "open" ;
	static final String RESPONSE_STATUS_CLOSED = "closed" ;
	
	
	// The URL of our cgi-script
	static final String QUANTRO_LOBBY_WEB_URL = "https://secure.peaceray.com/cgi-bin/quantro_lobby_web" ;
	// static final String QUANTRO_LOBBY_WEB_URL = "http://www.peaceray.com/cgi-bin/quantro_lobby_web" ;
	
	// action names
	static final String ACTION_LIST = "list" ;
	static final String ACTION_STATUS = "status" ;
	static final String ACTION_DESCRIPTION = "description" ;
	
	// mutable lobby action names
	protected static final String ACTION_OPEN_REQUEST = "open_request" ;
	protected static final String ACTION_OPEN_CONFIRM = "open_confirm" ;
	protected static final String ACTION_MAINTAIN_REQUEST = "maintain_request" ;
	protected static final String ACTION_MAINTAIN_CONFIRM = "maintain_confirm" ;
	protected static final String ACTION_OPEN_GAME_REQUEST = "open_game_request" ;
	protected static final String ACTION_OPEN_GAME_CONFIRM = "open_game_confirm" ;
	protected static final String ACTION_LIST_GAMES = "list_games" ;
	protected static final String ACTION_HOST = "host" ;
	protected static final String ACTION_CLOSE = "close" ;
	protected static final String ACTION_MATCH_TICKET_LOBBY = "match_ticket_lobby" ;
	protected static final String ACTION_MATCH_TICKET_GAME = "match_ticket_game" ;
	
	
	// InternetLobbyGame names
	protected static final String ACTION_STATUS_GAME = "status_game" ;
	protected static final String ACTION_MAINTAIN_GAME_REQUEST = "maintain_game_request" ;
	protected static final String ACTION_MAINTAIN_GAME_CONFIRM = "maintain_game_confirm" ;
	protected static final String ACTION_HOST_GAME = "host_game" ;
	protected static final String ACTION_CLOSE_GAME = "close_game" ;
	
	
	// cgi-script keys
	static final String VAR_ACTION 		= "action" ;		// action to request from lobby_web
	static final String VAR_REQUEST 	= "request" ;		// a note from the server of the request type
	static final String VAR_WORK 		= "work" ;			// how much work should we do?
	static final String VAR_PROOF 		= "proof" ;			// proof that we did the work
	static final String VAR_NONCE 		= "nonce" ;			// nonce for this lobby
	static final String VAR_KEY 		= "key" ;			// edit key for this lobby
	static final String VAR_GAME_NONCE 	= "game_nonce" ;			// nonce for this lobby
	static final String VAR_GAME_KEY 	= "game_key" ;			// edit key for this lobby
	static final String VAR_STATUS 		= "status" ;		// lobby status
	static final String VAR_PORT 		= "port" ;			// port for mediation
	static final String VAR_NAME 		= "name" ;			// lobby name
	static final String VAR_DESCRIPTION = "description" ;	// lobby description
	static final String VAR_SIZE        = "size" ;			// lobby size (minimum of 2)
	static final String VAR_PUBLIC      = "public" ;		// public
	static final String VAR_ITINERANT   = "itinerant" ;		// itinerant
	static final String VAR_TIME        = "time" ;			// time at which message was sent, in a server-readable format.
	static final String VAR_SECONDS     = "seconds" ;		// number of seconds before something happens (e.g., before maintenance is needed)	
	static final String VAR_SIGNATURE   = "signature" ;		// server signature on the work order
	static final String VAR_XL          = "xl" ;			// user's XL key.  Optional, but gives lobby priority.
	static final String VAR_HOST      	= "host" ;		// player name.  Almost certainly the creator of the lobby.
	static final String VAR_ADDRESS     = "address" ;		// address of the creator.  This is used to ensure a "request" comes from the same place as the confirmation.
	static final String VAR_REASON      = "reason" ;		// Refusal (a "NO" message) will sometimes give a reason.
	static final String VAR_SALT        = "salt" ;			// Work orders are salted.  Remember, work is based on nonce and key, 
	static final String VAR_AGE         = "age" ;			// Age of the lobby.
	static final String VAR_PLAYERS		= "players" ;		// Number of players currently connected.
	static final String VAR_USER_ID		= "user_id" ;
	static final String VAR_MATCH_TICKET = "match_ticket" ;
	
	static final int VAR_KEY_CODE_ACTION 		= 0 ;		// action to request from lobby_web
	static final int VAR_KEY_CODE_REQUEST 		= 1 ;		// a note from the server of the request type
	static final int VAR_KEY_CODE_WORK 			= 2 ;			// how much work should we do?
	static final int VAR_KEY_CODE_PROOF 		= 3 ;			// proof that we did the work
	static final int VAR_KEY_CODE_NONCE 		= 4 ;			// nonce for this lobby
	static final int VAR_KEY_CODE_KEY 			= 5 ;			// edit key for this lobby
	static final int VAR_KEY_CODE_GAME_NONCE 	= 6 ;			// nonce for this lobby
	static final int VAR_KEY_CODE_GAME_KEY 		= 7 ;			// edit key for this lobby
	static final int VAR_KEY_CODE_STATUS 		= 8 ;		// lobby status
	static final int VAR_KEY_CODE_PORT 			= 9 ;			// port for mediation
	static final int VAR_KEY_CODE_NAME 			= 10 ;			// lobby name
	static final int VAR_KEY_CODE_DESCRIPTION 	= 11 ;	// lobby description
	static final int VAR_KEY_CODE_SIZE        	= 12 ;			// lobby size (minimum of 2)
	static final int VAR_KEY_CODE_PUBLIC      	= 13 ;		// public
	static final int VAR_KEY_CODE_ITINERANT    	= 14 ;		// public
	static final int VAR_KEY_CODE_TIME        	= 15 ;			// time at which message was sent, in a server-readable format.
	static final int VAR_KEY_CODE_SECONDS     	= 16 ;		// number of seconds before something happens (e.g., before maintenance is needed)	
	static final int VAR_KEY_CODE_SIGNATURE   	= 17 ;		// server signature on the work order
	static final int VAR_KEY_CODE_XL          	= 18 ;			// user's XL key.  Optional, but gives lobby priority.
	static final int VAR_KEY_CODE_HOST      	= 19 ;		// host name.  Almost certainly the creator of the lobby.
	static final int VAR_KEY_CODE_ADDRESS     	= 20 ;		// address of the creator.  This is used to ensure a "request" comes from the same place as the confirmation.
	static final int VAR_KEY_CODE_REASON      	= 21 ;		// Refusal (a "NO" message) will sometimes give a reason.
	static final int VAR_KEY_CODE_SALT        	= 22 ;			// Work orders are salted.  Remember, work is based on nonce and key, 
	static final int VAR_KEY_CODE_AGE         	= 23 ;			// Age of the lobby.
	static final int VAR_KEY_CODE_PLAYERS		= 24 ;
	static final int VAR_KEY_CODE_USER_ID 		= 25 ;
	static final int VAR_KEY_CODE_MATCH_TICKET  = 26 ;
	
	
	
	
	// ERROR CODES
	// In the event of a communication failure of some kind, an error flag
	// is set within the Challenge object.  If you are concerned only with
	// success/failure, examine the return value of the communication functions:
	// refresh, accept, decline, cancel, request_game.  If you need specific
	// error information, examine the flag with getError().
	public static final int ERROR_MISMATCHED_WORK_ORDER = -3 ;	// The work order provided is inappropriate for this lobby or for the called method.
	public static final int ERROR_INCOMPLETE_WORK_ORDER = -2 ;	// The work order provided for confirmation wasn't complete.  No server communication occurred.
	public static final int ERROR_ILLEGAL_STATE = -1 ;	// This object is not in the right state to perform this action.  No server communication occurred.
	public static final int ERROR_NONE = 0 ;		// no error
	public static final int ERROR_TIMEOUT = 1 ;		// could not connect and get info within timeout
	public static final int ERROR_REFUSED = 2 ;		// We received a "no."  Only relevant for actions; for 'refresh', a NO indicates that the nonce was not found; this is not an error, but rather a status update to "REMOVED"
	public static final int ERROR_FAILED = 3 ;		// We received a "fail"
	public static final int ERROR_BLANK = 4 ;		// Empty response from server.
	public static final int ERROR_MALFORMED = 5 ;	// Malformed response from server.
	public static final int ERROR_ILLEGAL_ACTION = 6 ;	// Can't perform that action on this Lobby.
	public static final int ERROR_UNKNOWN = 7 ;		// Some other kind of error.
	
	// REASON CODES
	// for ERROR_REFUSED, the server may optionally provide a reason.
	public static final int REASON_NONE = -1 ;
	public static final int REASON_SIGNATURE = 0 ;	// server's self-signature check failed
	public static final int REASON_TIME = 1 ;	// too much time has passed.
	public static final int REASON_WORK = 2 ;	// insufficient work was performed.
	public static final int REASON_ADDRESS = 3 ;	// The address from which our query originated did not match an earlier request (usually only relevant wih request/confirm pairs).
	public static final int REASON_CLOSED = 4 ;		// We can't perform that action because the Lobby is closed.
	public static final int REASON_PARAMS = 5 ;
	public static final int REASON_DATABASE = 6 ;
	public static final int REASON_FULL = 7 ;
	public static final int REASON_EXISTS = 8 ;
	
	static final String REASON_STRING_NONE = "none" ;
	static final String REASON_STRING_SIGNATURE = "signature" ;
	static final String REASON_STRING_TIME = "time" ;
	static final String REASON_STRING_WORK = "work" ;
	static final String REASON_STRING_ADDRESS = "address" ;
	static final String REASON_STRING_CLOSED = "closed" ;
	static final String REASON_STRING_PARAMS = "params" ;
	static final String REASON_STRING_DATABASE = "database" ;
	static final String REASON_STRING_FULL = "full" ;
	static final String REASON_STRING_EXISTS = "exists" ;
	
	
	static final String RESPONSE_SEPARATOR_LOBBY = "[L]" ;
	static final String RESPONSE_SEPARATOR_GAME = "[G]" ;
	static final String SECTION_HEADER_LOBBY = "<L>" ;
	static final String SECTION_HEADER_GAME = "<G>" ;
	static final String POST_VALUE_SEPARATOR = "." ;
	
	
	// ARRAYS AND ENCODINGS
	// Refactoring to use the WebQuery class requires WebQuery.Builder, which
	// reads variable lists, section headers and encodings as either individual
	// values or ArrayLists / Hashtables.  Since there are pretty consistent throughout,
	// we statically define them here.
	static final ArrayList<String> VAR_NAMES = new ArrayList<String>() ;
	static final ArrayList<String> SECTION_HEADERS = new ArrayList<String>() ;
	static final Hashtable<String, Object> STATUS_CODES = new Hashtable<String, Object>() ;
	static final Hashtable<String, Integer> VAR_CODES = new Hashtable<String, Integer>() ;
	static{
		VAR_NAMES.add(VAR_ACTION) ;
		VAR_NAMES.add(VAR_REQUEST) ;
		VAR_NAMES.add(VAR_WORK) ;
		VAR_NAMES.add(VAR_PROOF) ;
		VAR_NAMES.add(VAR_NONCE) ;
		VAR_NAMES.add(VAR_KEY) ;
		VAR_NAMES.add(VAR_GAME_NONCE) ;
		VAR_NAMES.add(VAR_GAME_KEY) ;
		VAR_NAMES.add(VAR_STATUS) ;
		VAR_NAMES.add(VAR_PORT) ;
		VAR_NAMES.add(VAR_NAME) ;
		VAR_NAMES.add(VAR_DESCRIPTION) ;
		VAR_NAMES.add(VAR_SIZE) ;
		VAR_NAMES.add(VAR_PUBLIC) ;
		VAR_NAMES.add(VAR_ITINERANT) ;
		VAR_NAMES.add(VAR_TIME) ;
		VAR_NAMES.add(VAR_SECONDS) ;
		VAR_NAMES.add(VAR_SIGNATURE) ;
		VAR_NAMES.add(VAR_XL) ;
		VAR_NAMES.add(VAR_HOST) ;
		VAR_NAMES.add(VAR_ADDRESS) ;
		VAR_NAMES.add(VAR_REASON) ;
		VAR_NAMES.add(VAR_SALT) ;
		VAR_NAMES.add(VAR_AGE) ;
		VAR_NAMES.add(VAR_PLAYERS) ;
		VAR_NAMES.add(VAR_USER_ID) ;
		VAR_NAMES.add(VAR_MATCH_TICKET) ;
		
		
		SECTION_HEADERS.add(SECTION_HEADER_LOBBY) ;
		SECTION_HEADERS.add(SECTION_HEADER_GAME) ;
		
		STATUS_CODES.put(RESPONSE_STATUS_OPEN, new Integer(STATUS_OPEN)) ;
		STATUS_CODES.put(RESPONSE_STATUS_CLOSED, new Integer(STATUS_CLOSED)) ;
		
		VAR_CODES.put(VAR_ACTION, VAR_KEY_CODE_ACTION) ;
		VAR_CODES.put(VAR_REQUEST, VAR_KEY_CODE_REQUEST ) ;
		VAR_CODES.put(VAR_WORK, VAR_KEY_CODE_WORK ) ;
		VAR_CODES.put(VAR_PROOF, VAR_KEY_CODE_PROOF ) ;
		VAR_CODES.put(VAR_NONCE, VAR_KEY_CODE_NONCE ) ;
		VAR_CODES.put(VAR_KEY, VAR_KEY_CODE_KEY ) ;
		VAR_CODES.put(VAR_GAME_NONCE, VAR_KEY_CODE_GAME_NONCE ) ;
		VAR_CODES.put(VAR_GAME_KEY, VAR_KEY_CODE_GAME_KEY ) ;
		VAR_CODES.put(VAR_STATUS, VAR_KEY_CODE_STATUS ) ;
		VAR_CODES.put(VAR_PORT, VAR_KEY_CODE_PORT ) ;
		VAR_CODES.put(VAR_NAME, VAR_KEY_CODE_NAME ) ;
		VAR_CODES.put(VAR_DESCRIPTION, VAR_KEY_CODE_DESCRIPTION ) ;
		VAR_CODES.put(VAR_SIZE, VAR_KEY_CODE_SIZE ) ;
		VAR_CODES.put(VAR_PUBLIC, VAR_KEY_CODE_PUBLIC ) ;
		VAR_CODES.put(VAR_ITINERANT, VAR_KEY_CODE_ITINERANT ) ;
		VAR_CODES.put(VAR_TIME, VAR_KEY_CODE_TIME ) ;
		VAR_CODES.put(VAR_SECONDS, VAR_KEY_CODE_SECONDS ) ;
		VAR_CODES.put(VAR_SIGNATURE, VAR_KEY_CODE_SIGNATURE ) ;
		VAR_CODES.put(VAR_XL, VAR_KEY_CODE_XL ) ;
		VAR_CODES.put(VAR_HOST, VAR_KEY_CODE_HOST ) ;
		VAR_CODES.put(VAR_ADDRESS, VAR_KEY_CODE_ADDRESS ) ;
		VAR_CODES.put(VAR_REASON, VAR_KEY_CODE_REASON ) ;
		VAR_CODES.put(VAR_SALT, VAR_KEY_CODE_SALT ) ;
		VAR_CODES.put(VAR_AGE, VAR_KEY_CODE_AGE ) ;
		VAR_CODES.put(VAR_PLAYERS, VAR_KEY_CODE_PLAYERS) ;
		VAR_CODES.put(VAR_USER_ID, VAR_KEY_CODE_USER_ID) ;
		VAR_CODES.put(VAR_MATCH_TICKET, VAR_KEY_CODE_MATCH_TICKET) ;
	}
	
	
	
	public static int reasonStringToInt( String reason ) {
		if ( WebConsts.REASON_STRING_SIGNATURE.equals(reason) )
			return WebConsts.REASON_SIGNATURE ;
		if ( WebConsts.REASON_STRING_TIME.equals(reason) )
			return WebConsts.REASON_TIME ;
		if ( WebConsts.REASON_STRING_WORK.equals(reason) )
			return WebConsts.REASON_WORK ;
		if ( WebConsts.REASON_STRING_ADDRESS.equals(reason) )
			return WebConsts.REASON_ADDRESS ;
		if ( WebConsts.REASON_STRING_CLOSED.equals(reason) )
			return WebConsts.REASON_CLOSED ;
		if ( WebConsts.REASON_STRING_PARAMS.equals(reason) )
			return WebConsts.REASON_PARAMS ;
		if ( WebConsts.REASON_STRING_DATABASE.equals(reason) )
			return WebConsts.REASON_DATABASE ;
		if ( WebConsts.REASON_STRING_FULL.equals(reason) )
			return WebConsts.REASON_FULL ;
		if ( WebConsts.REASON_STRING_EXISTS.equals(reason) )
			return WebConsts.REASON_EXISTS ;
		
		
		return WebConsts.REASON_NONE ;
	}
	
	public static String reasonIntToString( int reason ) {
		switch( reason ) {
		case REASON_NONE:
			return REASON_STRING_NONE ;
		case REASON_SIGNATURE:
			return REASON_STRING_SIGNATURE ;
		case REASON_TIME:
			return REASON_STRING_TIME ;
		case REASON_WORK:
			return REASON_STRING_WORK ;
		case REASON_ADDRESS:
			return REASON_STRING_ADDRESS ;
		case REASON_CLOSED:
			return REASON_STRING_CLOSED ;
		case REASON_PARAMS:
			return REASON_STRING_PARAMS ;
		case REASON_DATABASE:
			return REASON_STRING_DATABASE ;
		case REASON_FULL:
			return REASON_STRING_FULL ;
		case REASON_EXISTS:
			return REASON_STRING_EXISTS ;
		}
		
		return null ;
	}
	
	
	public static String errorIntToString( int error ) {
		switch( error ) {
		case ERROR_MISMATCHED_WORK_ORDER:
			return "MismatchedWorkOrder" ;
		case ERROR_INCOMPLETE_WORK_ORDER:
			return "IncompleteWorkOrder" ;
		case ERROR_ILLEGAL_STATE:
			return "IllegalState" ;
		case ERROR_NONE:
			return "None" ;
		case ERROR_TIMEOUT:
			return "Timeout" ;
		case ERROR_REFUSED:
			return "Refused" ;
		case ERROR_FAILED:
			return "Failed" ;
		case ERROR_BLANK:
			return "Blank" ;
		case ERROR_MALFORMED:
			return "Malformed" ;
		case ERROR_ILLEGAL_ACTION:
			return "IllegalAction" ;
		case ERROR_UNKNOWN:
			return "Unknown" ;
		}
		
		return "<error code string conversion problem>" ;
	}
	
	
}
