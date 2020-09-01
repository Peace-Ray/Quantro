package com.peaceray.quantro.lobby;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.peaceray.quantro.communications.nonce.Nonce;


/**
 * A LobbyInvitationFactory is a 'conversion' class for creating
 * lobby invitations (URL strings) from InternetLobbies, and vice-versa.
 * 
 * Factory methods do NOT make any network access calls; they merely
 * convert input data to output.
 * 
 * @author Jake
 *
 */
public class LobbyStringEncoder {

	private static final String URL = "http://peaceray.com/quantro/lobby" ;
	private static final String PREFIX = "l?" ;
	private static final String QUERY = "l=" ;
	private static final String PREFIX_AND_QUERY = PREFIX + QUERY ;
	private static final String FULL_PREFIX = URL + "/" + PREFIX_AND_QUERY ;
	
	private static final String SEPARATOR = ":" ;
	
	private static final String TYPE_PUBLIC = "u" ;
	private static final String TYPE_PRIVATE = "r" ;
	private static final String TYPE_ITINERANT = "i" ;
	
	
	// EXAMPLE WIFI FORMAT:
	// 		com.peaceray.quantro://wifi.lobby/l?l=192.168.0.5;<nonceString>;Wifi Lobby;Majestic Host
	//	address: 192.168.0.5
	//  lobby name: Wifi Lobby
	//  host name: Majestic Host
	private static final String WIFI_URL = "com.peaceray.quantro://wifi.lobby" ;
	private static final String WIFI_PREFIX = "l?" ;
	private static final String WIFI_QUERY = "l=" ;
	private static final String WIFI_PREFIX_AND_QUERY = WIFI_PREFIX + WIFI_QUERY ;
	private static final String WIFI_FULL_PREFIX = WIFI_URL + "/" + WIFI_PREFIX_AND_QUERY ;
	
	private static final String WIFI_SEPARATOR = ";" ;
	
	/**
	 * Given an InternetLobby instance, returns an invitation string.
	 * 
	 * @param lobby
	 * @return
	 */
	public static String toFullString( InternetLobby lobby ) {
		// The only important information to encode is lobby nonce
		// and public/private/itinerant.
		
		String nonce = lobby.getLobbyNonce().toString() ;
		String type ;
		if ( lobby.isPublic() )
			type = TYPE_PUBLIC ;
		else if ( lobby.isItinerant() )
			type = TYPE_ITINERANT ;
		else
			type = TYPE_PRIVATE ;
		
		return FULL_PREFIX + nonce + SEPARATOR + type ;
	}

	
	public static String toFullString( String data ) {
		if ( FULL_PREFIX.length() < data.length() && data.substring(0, FULL_PREFIX.length()).equals(FULL_PREFIX) )
			return data ;
		else if ( PREFIX_AND_QUERY.length() < data.length() && data.substring(0, PREFIX_AND_QUERY.length()).equals(PREFIX_AND_QUERY) )
			return URL + "/" + data ;
		else if ( QUERY.length() < data.length() && data.substring(0, QUERY.length()).equals(QUERY) )
			return URL + "/" + PREFIX + data ;
		
		return FULL_PREFIX + data ;
	}
	
	
	/**
	 * Given a string representation (as returned by toString( ... ))
	 * returns an unrefreshed InternetLobby object which represents
	 * the same data.
	 * 
	 * In particular, this lobby will contain the nonce and 
	 * 
	 * @param str
	 * @return
	 */
	public static InternetLobby toLobby( String str ) {
		try {
			if ( str.length() < FULL_PREFIX.length() || !str.substring(0, FULL_PREFIX.length()).equals(FULL_PREFIX) )
				str = toFullString(str) ;
			String data = str.substring(FULL_PREFIX.length()) ;
			String [] elems = data.split(SEPARATOR) ;
			
			String nonce = elems[0] ;
			String type = elems[1] ;
			
			try {
				return InternetLobby.newUnrefreshedInstance(
						new Nonce(nonce),
						type.equals(TYPE_PUBLIC),
						type.equals(TYPE_ITINERANT), InternetLobby.ORIGIN_INVITATION) ;
			} catch (IOException e) {
				return null ;
			}
		} catch ( Exception e ) {
			return null ;
		}
	}
	
	
	
	public static String encodeWiFiLobbyAddress( InetAddress inetAddress ) {
		return WIFI_FULL_PREFIX + inetAddress.getHostAddress() ;
	}
	
	public static String encodeWiFiLobbyAddress( String address ) {
		if ( WIFI_FULL_PREFIX.length() < address.length() && address.substring(0, WIFI_FULL_PREFIX.length()).equals(FULL_PREFIX) )
			return address ;
		else if ( WIFI_PREFIX_AND_QUERY.length() < address.length() && address.substring(0, WIFI_PREFIX_AND_QUERY.length()).equals(WIFI_PREFIX_AND_QUERY) )
			return WIFI_URL + "/" + address ;
		else if ( WIFI_QUERY.length() < address.length() && address.substring(0, WIFI_QUERY.length()).equals(WIFI_QUERY) )
			return WIFI_URL + "/" + WIFI_PREFIX + address ;
		
		return WIFI_FULL_PREFIX + address ;
	}
	
	public static String encodeWiFiLobby( String ipAddress, Nonce lobbyNonce, String lobbyName, String hostName ) {
		
		if ( WIFI_FULL_PREFIX.length() < ipAddress.length() && ipAddress.substring(0, WIFI_FULL_PREFIX.length()).equals(FULL_PREFIX) )
			throw new IllegalArgumentException("Provided ipAddress includes LobbyString formatting??") ;
		else if ( WIFI_PREFIX_AND_QUERY.length() < ipAddress.length() && ipAddress.substring(0, WIFI_PREFIX_AND_QUERY.length()).equals(WIFI_PREFIX_AND_QUERY) )
			throw new IllegalArgumentException("Provided ipAddress includes LobbyString formatting??") ;
		else if ( WIFI_QUERY.length() < ipAddress.length() && ipAddress.substring(0, WIFI_QUERY.length()).equals(WIFI_QUERY) )
			throw new IllegalArgumentException("Provided ipAddress includes LobbyString formatting??") ;
		
		StringBuilder sb = new StringBuilder() ;
		sb.append(WIFI_FULL_PREFIX) ;
		// join components
		sb.append(ipAddress) ;
		sb.append(WIFI_SEPARATOR).append(lobbyNonce == null ? "" : lobbyNonce.toString()) ;
		sb.append(WIFI_SEPARATOR).append(lobbyName == null ? "" : lobbyName) ;
		sb.append(WIFI_SEPARATOR).append(hostName == null ? "" : hostName) ;
		
		return sb.toString() ;
	}
	
	public static InetAddress decodeWiFiLobbyAddress( String str ) {
		try {
			if ( str.length() < WIFI_FULL_PREFIX.length() || !str.substring(0, WIFI_FULL_PREFIX.length()).equals(WIFI_FULL_PREFIX) )
				str = encodeWiFiLobbyAddress(str) ;
			String data = str.substring(WIFI_FULL_PREFIX.length()) ;
			
			String [] elems = data.split(WIFI_SEPARATOR) ;
			
			String ip = elems[0] ;
			
			try {
				return InetAddress.getByName(ip) ;
			} catch (UnknownHostException e) {
				return null ;
			}
		} catch ( Exception e ) {
			return null ;
		}
	}
	
	public static Nonce decodeWiFiLobbyNonce( String str ) {
		try {
			if ( str.length() < WIFI_FULL_PREFIX.length() || !str.substring(0, WIFI_FULL_PREFIX.length()).equals(WIFI_FULL_PREFIX) )
				str = encodeWiFiLobbyAddress(str) ;
			String data = str.substring(WIFI_FULL_PREFIX.length()) ;
			
			String [] elems = data.split(WIFI_SEPARATOR) ;
			if ( elems.length < 2 )
				return null ;
			
			String nonceString = elems[1] ;
			
			try {
				return new Nonce(nonceString) ;
			} catch (UnknownHostException e) {
				return null ;
			}
		} catch ( Exception e ) {
			return null ;
		}
	}
	
	
	public static String decodeWiFiLobbyName( String str ) {
		try {
			if ( str.length() < WIFI_FULL_PREFIX.length() || !str.substring(0, WIFI_FULL_PREFIX.length()).equals(WIFI_FULL_PREFIX) )
				str = encodeWiFiLobbyAddress(str) ;
			String data = str.substring(WIFI_FULL_PREFIX.length()) ;
			
			String [] elems = data.split(WIFI_SEPARATOR) ;
			if ( elems.length < 3 )
				return null ;
			String name = elems[2].trim() ;
			if ( name.length() == 0 )
				return null ;
			return name ;
		} catch ( Exception e ) {
			return null ;
		}
	}
	
	public static String decodeWiFiLobbyHostName( String str ) {
		try {
			if ( str.length() < WIFI_FULL_PREFIX.length() || !str.substring(0, WIFI_FULL_PREFIX.length()).equals(WIFI_FULL_PREFIX) )
				str = encodeWiFiLobbyAddress(str) ;
			String data = str.substring(WIFI_FULL_PREFIX.length()) ;
			
			String [] elems = data.split(WIFI_SEPARATOR) ;
			if ( elems.length < 4 )
				return null ;
			String name = elems[3].trim() ;
			if ( name.length() == 0 )
				return null ;
			return name ;
		} catch ( Exception e ) {
			return null ;
		}
	}
	
	
	
}
