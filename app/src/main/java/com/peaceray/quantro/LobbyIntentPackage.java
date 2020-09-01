package com.peaceray.quantro;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.SocketAddress;

import android.util.Log;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.MutableInternetLobby;
import com.peaceray.quantro.lobby.WiFiLobbyDetails;

/**
 * LobbyActivityIntentPackage provides a single class
 * for all information necessary to set up and/or
 * join a lobby.  It assumes that we ONLY allow built-in
 * game modes (and thus we don't need access to external XML
 * files, either included in the Intent or available online).
 * It also assumes all game modes launch at exactly 2 votes,
 * so there is no need to specify voting thresholds.  Finally,
 * since there are exactly 2 people per lobby and games launch at
 * 2 votes, the lobby creator is always included in the launch and
 * the Lobby does not need to remain operating while the game is being played.
 * 
 * TODO: Resolve the above for larger lobbies, != 2P game types, persistent
 * lobbies after game launch, etc., additional game types not included
 * in APK, etc.
 * 
 * 
 * 
 * @author Jake
 *
 */
public class LobbyIntentPackage implements Serializable {
	
	private static final String TAG = "LobbyIntentPackage" ;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8158734043743262602L;
	
	
	
	public static final int LOBBY_DIRECT_CLIENT = 0 ;		// connect to someone else's lobby
	public static final int LOBBY_DIRECT_HOST = 1 ;			// run your own lobby and connect to it as a client
	public static final int LOBBY_MATCHSEEKER_CLIENT = 2 ;
	public static final int LOBBY_MATCHSEEKER_HOST = 3 ;
	
	
	
	public int connection ;
	
	// Information on the user.
	public Nonce personalNonce ;
	public String name ;
	
	// Lobby information.
	public Nonce nonce ;
	public String lobbyName ;
	public int lobbySize ;
	
	// Clients need this, but Hosts connect on "localhost"
	public SocketAddress [] socketAddress ;
	
	public SocketAddress udpUpdateSocketAddress ;
	
	// Hosts need this.
	public int lobbyAnnouncerPort ;
	public int lobbyListenPort ;
	
	// Mediator information.  We use 'lobbyPort' for our local
	// port, connecting to the mediator port / address separately.
	public int localPort ;
	
	public InternetLobby internetLobby ;
	
	
	public LobbyIntentPackage( Nonce personalNonce, String name, WiFiLobbyDetails lobbyDetails ) {
		this.connection = -1 ;
		
		this.personalNonce = personalNonce ;
		this.name = name ;
		
		this.nonce = lobbyDetails.getNonce() ;
		this.lobbyName = lobbyDetails.getStatus().getLobbyName() ;
		this.lobbySize = lobbyDetails.getStatus().getMaxPeople() ;
		
		this.localPort = 0 ;
		this.socketAddress = null ;
		
		this.internetLobby = null ;
	}
	
	
	public LobbyIntentPackage( Nonce personalNonce, String name, Nonce lobbyNonce, String lobbyName, int lobbySize ) {
		
		this.connection = -1 ;
		
		this.personalNonce = personalNonce ;
		this.name = name ;
		this.nonce = lobbyNonce ;
		this.lobbyName = lobbyName ;
		this.lobbySize = lobbySize ;
		
		this.localPort = 0 ;
		this.socketAddress = null ;
		
		this.internetLobby = null ;
	}
	
	/**
	 * Returns whether the provided IntentPackage represents the same
	 * lobby intent.  This means it represents the same lobby (same type,
	 * same nonce, same address (if applc.)), AND that our intention
	 * (hosting or joining) is the same.
	 * 
	 * @param lip
	 * @return
	 */
	public boolean isSameLobbyIntent( LobbyIntentPackage lip ) {
		if ( lip == null ) {
			return false ;
		}
		
		if ( isHost() != lip.isHost() ) {
			return false ;
		}
			
		if ( connection != lip.connection ) {
			return false ;
		}
		
		if ( !nonce.equals(lip.nonce) ) {
			return false ;
		}
		
		if ( isDirect() ) {
			if ( isHost() ) {
				if ( localPort != lip.localPort )
					return false ;
				if ( lobbyAnnouncerPort != lip.lobbyAnnouncerPort )
					return false ;
				if ( lobbyListenPort != lip.lobbyListenPort )
					return false ;
			} else {
				if ( socketAddress.length != lip.socketAddress.length )
					return false ;
				for ( int i = 0; i < socketAddress.length; i++ ) {
					if ( !socketAddress[i].equals(lip.socketAddress[i]) )
						return false ;
				}
			}
			
			if ( !nonce.equals(lip.nonce) )
				return false ;
			
			// hey guess what!
			return true ;
		}
		
		if ( isMatchseeker() ) {
			if ( !internetLobby.getLobbyNonce().equals(lip.internetLobby.getLobbyNonce()) ) {
				return false ;
			}
			
			// hey guess what!
			return true ;
		}
		
		// fall-through: was this not a recognized connection type?
		return false ;
	}
	
	public boolean isClient() {
		return connection == LOBBY_DIRECT_CLIENT || connection == LOBBY_MATCHSEEKER_CLIENT ;
	}
	
	public boolean isHost() {
		return connection == LOBBY_DIRECT_HOST || connection == LOBBY_MATCHSEEKER_HOST ;
	}
	
	public boolean isDirect() {
		return connection == LOBBY_DIRECT_HOST || connection == LOBBY_DIRECT_CLIENT ;
	}
	
	public boolean isMatchseeker() {
		return connection == LOBBY_MATCHSEEKER_CLIENT || connection == LOBBY_MATCHSEEKER_HOST ;
	}
	
	
	public LobbyIntentPackage setAsDirectClient( SocketAddress ... socketAddress ) {
		connection = LOBBY_DIRECT_CLIENT ;
		
		this.socketAddress = socketAddress.clone() ;
		return this ;
	}
	
	
	public LobbyIntentPackage setAsDirectHost( int localPort, int lobbyAnnouncerPort, int lobbyListenPort ) {
		connection = LOBBY_DIRECT_HOST ;
		
		this.localPort = localPort ;
		this.lobbyAnnouncerPort = lobbyAnnouncerPort ;
		this.lobbyListenPort = lobbyListenPort ;
		return this ;
	}
	
	
	public LobbyIntentPackage setAsMatchseekerClient( SocketAddress matchseekerSocketAddress, InternetLobby lobby ) {
		connection = LOBBY_MATCHSEEKER_CLIENT ;
		
		this.nonce = lobby.getLobbyNonce() ;
		this.lobbyName = lobby.getLobbyName() ;
		this.socketAddress = new SocketAddress[] { matchseekerSocketAddress } ;
		this.internetLobby = lobby.newInstance() ;
		
		return this ;
	}
	
	public LobbyIntentPackage setAsMatchseekerHost( SocketAddress matchseekerSocketAddress, MutableInternetLobby lobby ) {
		connection = LOBBY_MATCHSEEKER_HOST ;
		
		this.nonce = lobby.getLobbyNonce() ;
		this.lobbyName = lobby.getLobbyName() ;
		this.socketAddress = new SocketAddress[] { matchseekerSocketAddress } ;
		this.internetLobby = lobby.newInstance() ;
		
		return this ;
	}
	
	
	
	
	//////////////////////////////////////////////////////////////////////
	// METHODS FOR SERIALIZING
	//////////////////////////////////////////////////////////////////////
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt( connection ) ;
		
		// Information on the user.
		stream.writeObject(personalNonce) ;
		stream.writeObject(name) ;
		
		// Lobby information.  Some of this is needed by
		// client, some for hosting, some for both.
		stream.writeObject(nonce) ;
		stream.writeObject(lobbyName) ;
		stream.writeInt(lobbySize) ;
		
		// Clients need this, but Hosts connect on "localhost"
		stream.writeObject( socketAddress ) ;
		stream.writeObject( udpUpdateSocketAddress ) ;
		
		// Hosts need this.
		stream.writeInt( lobbyAnnouncerPort ) ;
		stream.writeInt( lobbyListenPort ) ;
		
		// Mediated challenge hosts and clients need this.
		stream.writeInt( localPort ) ;
		
		// Mediated hosts and clients need this.
		stream.writeObject(internetLobby) ;
	}
	
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		connection = stream.readInt() ;
		
		// Information on the user.
		personalNonce = (Nonce) stream.readObject();
		name = (String) stream.readObject() ;
		
		// Lobby information.  Some of this is needed by
		// client, some for hosting, some for both.
		nonce = (Nonce) stream.readObject();
		lobbyName = (String) stream.readObject();
		lobbySize = stream.readInt() ;
		
		// Clients need this, but Hosts connect on "localhost"
		socketAddress = (SocketAddress []) stream.readObject() ;
		udpUpdateSocketAddress = (SocketAddress) stream.readObject() ;
		
		// Hosts need this.
		lobbyAnnouncerPort = stream.readInt();
		lobbyListenPort = stream.readInt() ;
		
		// Mediated challenge hosts and clients need these.
		localPort = stream.readInt();
		
		// Mediated hosts and clients need this.
		internetLobby = (InternetLobby)stream.readObject() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required ASAC structure.") ;
	}
	
}
