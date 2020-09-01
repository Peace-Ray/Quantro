package com.peaceray.quantro;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.SocketAddress;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.InternetLobbyGame;
import com.peaceray.quantro.model.game.GameSettings;


public class GameIntentPackage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4482242164892063176L;

	
	
	public static final int CONNECTION_LOCAL = 0 ;
	public static final int CONNECTION_DIRECT_CLIENT = 1 ;
	public static final int CONNECTION_DIRECT_SERVER = 2 ;
	public static final int CONNECTION_MEDIATED_CHALLENGE_CLIENT = 3 ;
	public static final int CONNECTION_MEDIATED_CHALLENGE_SERVER = 4 ;
	public static final int CONNECTION_MATCHSEEKER_CLIENT = 5 ;
	public static final int CONNECTION_MATCHSEEKER_SERVER = 6 ;
	
	
	
	public int connection ;
	
	public GameSettings gameSettings ;
	
	// For SP (LOCAL) games - loading and saving.
	public String loadFromKey ;
	public String saveToKey ;
	
	// For MP games, we need connection information.
	public Nonce nonce ;
	public Nonce personalNonce ;
	public String name ;
	
	// For direct, this is the actual socket address.  For mediated,
	// this is the mediater socket address.
	public SocketAddress socketAddress ;
	public SocketAddress udpUpdateSocketAddress ;
	
	// Mediated games need a challenge and a mediator address
	public int localPort ;
	
	// Matchseeker games need an InternetLobbyGame;
	// for Android Beam purposes, we also include the InternetLobby.
	public InternetLobbyGame internetLobbyGame ;
	public InternetLobby internetLobby ;
	
	// For running a MP server, we can have some additional
	// (optional) information.
	public Nonce [] playerNonces ;
	public String [] playerNames ;
	
	
	public GameIntentPackage( GameSettings gs ) {
		this.connection = -1 ;
		
		this.gameSettings = new GameSettings( gs ) ;
		
		loadFromKey = null ;
		saveToKey = null ;
		
		nonce = Nonce.ZERO ;
		personalNonce = Nonce.ZERO ;
		name = null ;
		socketAddress = null ;
		
		internetLobbyGame = null ;
		internetLobby = null ;
		
		playerNonces = null ;
		playerNames = null ;
	}
	
	
	public boolean isLocal() {
		return this.connection == CONNECTION_LOCAL ;
	}
	
	public boolean isClient() {
		return this.connection == CONNECTION_DIRECT_CLIENT
				|| this.connection == CONNECTION_MEDIATED_CHALLENGE_CLIENT
				|| this.connection == CONNECTION_MATCHSEEKER_CLIENT ;
	}
	
	public boolean isServer() {
		return this.connection == CONNECTION_DIRECT_SERVER
				|| this.connection == CONNECTION_MEDIATED_CHALLENGE_SERVER
				|| this.connection == CONNECTION_MATCHSEEKER_SERVER ;
	}
	
	public boolean isMediatedChallenge() {
		return connection == CONNECTION_MEDIATED_CHALLENGE_CLIENT || connection == CONNECTION_MEDIATED_CHALLENGE_SERVER ;
	}
	
	public boolean isMatchseeker() {
		return connection == CONNECTION_MATCHSEEKER_CLIENT || connection == CONNECTION_MATCHSEEKER_SERVER ;
	}
	
	
	/**
	 * Sets this GameActivityIntentPackage as a "local connection"
	 * game - one with no server, where the game objects are linked
	 * to a NoOp connection that echoes states back at them.
	 * 
	 * This is the only connection type that supports long-term
	 * saving and loading.
	 * 
	 * @param loadFromKey A key for loading the game.  "null" is allowed,
	 * 	in which case no game will be loaded.
	 * @param saveToKey
	 */
	public void setConnectionAsLocal( String loadFromKey, String saveToKey ) {
		this.loadFromKey = loadFromKey ;
		this.saveToKey = saveToKey ;
		this.nonce = new Nonce() ;		// random nonce
		
		this.connection = CONNECTION_LOCAL ;
	}
	
	
	/**
	 * Sets this GameActivityIntentPackage as a "client connection"
	 * game - one where a server exists, reachable at the specified
	 * port and address.
	 * 
	 * Upon joining the server, we will be asked for the game nonce,
	 * our personal nonce, and our name.
	 * 
	 * The server itself maintains the canonical game representation.
	 * We therefore are given no opportunity to load / save.
	 * 
	 * @param nonce
	 * @param personalNonce
	 * @param name
	 * @param serverPort
	 * @param serverAddress
	 */
	public void setConnectionAsDirectClient( 
			Nonce nonce, Nonce personalNonce, String name,
			String [] names,
			SocketAddress socketAddress) {
		this.nonce = nonce ;
		this.personalNonce = personalNonce ;
		this.name = name ;
		if ( names != null ) {
			playerNames = new String[names.length] ;
			for ( int i = 0; i < names.length; i++ ) {
				this.playerNames[i] = names[i] ;
 			}
		}
		this.socketAddress = socketAddress ;
		
		this.connection = CONNECTION_DIRECT_CLIENT ;
	}
	
	
	/**
	 * Sets this GameActivityIntentPackage as a "Wifi Server" game
	 * - this game will run its own server, listening on the specified
	 * port.  The Game will also connect to this server as a client, using
	 * the specified personalNonce and name.
	 * 
	 * Optionally gives a list of allowed player nonces and names, if
	 * names are given (i.e. is not 'null') it must be the same length
	 * as the list of nonces.  This player's personal nonce and name
	 * need not be included in the provided array; we guarantee that
	 * they will be included in the GAIP's fields after this call 
	 * ( i.e. they will be copied there from 'personalNonce' and 'name'
	 * if necessary ).
	 * 
	 * @param nonce
	 * @param personalNonce
	 * @param name
	 * @param serverPort
	 * @param personalNonces
	 * @param names
	 */
	public void setConnectionAsDirectServer(
			Nonce nonce, Nonce personalNonce, String name,
			SocketAddress socketAddress,
			Nonce [] personalNonces, String [] names) {
		
		this.nonce = nonce ;
		this.personalNonce = personalNonce ;
		this.name = name ;
		this.socketAddress = socketAddress ;
		
		assert( names == null || ( personalNonces != null && personalNonces.length == names.length ) ) ;
		
		// Check for whether our personal nonce occurs in the provided
		// list of nonces.
		boolean personalNonceInList = false ;
		if ( personalNonces != null ) {
			for ( int i = 0; i < personalNonces.length; i++ ) {
				personalNonceInList = personalNonceInList || personalNonces[i].equals(personalNonce) ;
			}
		}
		
		if ( personalNonces != null ) {
			this.playerNonces = new Nonce[ personalNonces.length + ((personalNonceInList) ? 0 : 1) ] ;
			this.playerNames = new String[ personalNonces.length + ((personalNonceInList) ? 0 : 1) ] ;
		}
		else {
			this.playerNonces = new Nonce[((personalNonceInList) ? 0 : 1)] ;
			this.playerNames = new String[((personalNonceInList) ? 0 : 1)] ;
		}
		
		if ( personalNonces != null ) {
			for ( int i = 0; i < personalNonces.length; i++ ) {
				this.playerNonces[i] = personalNonces[i] ;
				if ( names != null )
					this.playerNames[i] = names[i] ;
				else
					this.playerNames[i] = null ;
 			}
		}

		
		// Put ourself as the last entry in nonces and names.
		if ( !personalNonceInList ) {
			this.playerNonces[this.playerNonces.length-1] = personalNonce ;
			this.playerNames[this.playerNonces.length-1] = name ;
		}
		
		connection = CONNECTION_DIRECT_SERVER ;
	}
	
	
	
	
	public void setConnectionAsMatchseekerClient(
			InternetLobbyGame ilg, InternetLobby il,
			Nonce personalNonce, String name, String [] names,
			SocketAddress matchseekerSocketAddress ) {
		
		this.internetLobbyGame = ilg.newInstance() ;
		this.internetLobby = il.newInstance() ;
		this.nonce = ilg.getNonce() ;
		this.personalNonce = personalNonce ;
		this.name = name ;
		if ( names != null ) {
			playerNames = new String[names.length] ;
			for ( int i = 0; i < names.length; i++ ) {
				this.playerNames[i] = names[i] ;
 			}
		}
		
		this.socketAddress = matchseekerSocketAddress ;
		
		this.connection = CONNECTION_MATCHSEEKER_CLIENT ;
	}
	
	
	/**
	 * Sets this GameActivityIntentPackage as a "Mediated Server" game
	 * - this game will run its own GameCoordinator, but will accept connections
	 * through a "mediation server" which either sets up pass-throughs, or
	 * aids in TCP hole punching.
	 * 
	 * Optionally gives a list of allowed player nonces and names, if
	 * names are given (i.e. is not 'null') it must be the same length
	 * as the list of nonces.  This player's personal nonce and name
	 * need not be included in the provided array; we guarantee that
	 * they will be included in the GAIP's fields after this call 
	 * ( i.e. they will be copied there from 'personalNonce' and 'name'
	 * if necessary ).
	 * 
	 * @param nonce
	 * @param personalNonce
	 * @param name
	 * @param serverPort
	 * @param personalNonces
	 * @param names
	 */
	public void setConnectionAsMatchseekerServer( 
			InternetLobbyGame ilg, InternetLobby il,
			Nonce personalNonce, String name, SocketAddress matchseekerSocketAddress,
			Nonce [] personalNonces, String [] names ) {
		
		this.internetLobbyGame = ilg.newInstance() ;
		this.internetLobby = il.newInstance() ;
		this.nonce = ilg.getNonce() ;
		this.personalNonce = personalNonce ;
		this.name = name ;
		this.socketAddress = matchseekerSocketAddress ;
		
		this.connection = CONNECTION_MATCHSEEKER_SERVER ;
		
		assert( names == null || ( personalNonces != null && personalNonces.length == names.length ) ) ;
		
		// Check for whether our personal nonce occurs in the provided
		// list of nonces.
		boolean personalNonceInList = false ;
		if ( personalNonces != null ) {
			for ( int i = 0; i < personalNonces.length; i++ ) {
				personalNonceInList = personalNonceInList || personalNonces[i].equals(personalNonce) ;
			}
		}
		
		if ( personalNonces != null ) {
			this.playerNonces = new Nonce[ personalNonces.length + ((personalNonceInList) ? 0 : 1) ] ;
			this.playerNames = new String[ personalNonces.length + ((personalNonceInList) ? 0 : 1) ] ;
		}
		else {
			this.playerNonces = new Nonce[((personalNonceInList) ? 0 : 1)] ;
			this.playerNames = new String[((personalNonceInList) ? 0 : 1)] ;
		}
		
		if ( personalNonces != null ) {
			for ( int i = 0; i < personalNonces.length; i++ ) {
				this.playerNonces[i] = personalNonces[i] ;
				if ( names != null )
					this.playerNames[i] = names[i] ;
				else
					this.playerNames[i] = null ;
 			}
		}

		
		// Put ourself as the last entry in nonces and names.
		if ( !personalNonceInList ) {
			this.playerNonces[this.playerNonces.length-1] = personalNonce ;
			this.playerNames[this.playerNonces.length-1] = name ;
		}
	}
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt( connection ) ;
		
		stream.writeObject(gameSettings) ;
		
		// For SP (LOCAL) games - loading and saving.
		stream.writeObject( loadFromKey ) ;
		stream.writeObject( saveToKey ) ;
		
		// For MP games, we need connection information.
		stream.writeObject(nonce) ;
		stream.writeObject(personalNonce) ;
		stream.writeObject(name) ;
		stream.writeObject(socketAddress) ;
		stream.writeObject(udpUpdateSocketAddress) ;
		
		// Mediator info
		stream.writeInt(localPort) ;
		
		stream.writeObject(internetLobbyGame) ;
		stream.writeObject(internetLobby) ;
		
		// For running a MP server, we can have some additional
		// (optional) information.
		stream.writeObject(playerNonces) ;
		stream.writeObject(playerNames) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		connection = stream.readInt() ;
		
		gameSettings = (GameSettings)stream.readObject() ;
		
		// For SP (LOCAL) games - loading and saving.
		loadFromKey = (String)stream.readObject() ;
		saveToKey = (String)stream.readObject() ;
		
		// For MP games, we need connection information.
		nonce = (Nonce) stream.readObject() ;
		personalNonce = (Nonce) stream.readObject() ;
		name = (String)stream.readObject() ;
		socketAddress = (SocketAddress)stream.readObject();
		udpUpdateSocketAddress = (SocketAddress)stream.readObject() ;
		
		// Mediation info
		localPort = stream.readInt();
		
		internetLobbyGame = (InternetLobbyGame) stream.readObject() ;
		internetLobby = (InternetLobby) stream.readObject() ;
		
		// For running a MP server, we can have some additional
		// (optional) information.
		playerNonces = (Nonce [])stream.readObject() ;
		playerNames = (String [])stream.readObject() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required ASAC structure.") ;
	}
	
}
