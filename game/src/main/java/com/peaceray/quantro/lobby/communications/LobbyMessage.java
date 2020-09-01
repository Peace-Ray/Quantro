package com.peaceray.quantro.lobby.communications;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.SocketAddress;

import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.utils.ByteArrayOps;

public class LobbyMessage extends Message {
	
	private static final int MAX_ARRAY_LENGTH = 128 ;
	
	// Message type.
	public static final byte TYPE_LOBBY_STATUS 				= Message.MIN_TYPE_IN_SUBCLASS	+  0 ;
	public static final byte TYPE_LOBBY_STATUS_REQUEST		= Message.MIN_TYPE_IN_SUBCLASS 	+  1 ;
	public static final byte TYPE_PLAYERS_IN_LOBBY			= Message.MIN_TYPE_IN_SUBCLASS	+  2 ;
	
	public static final byte TYPE_TEXT_MESSAGE				= Message.MIN_TYPE_IN_SUBCLASS	+  3 ;
	public static final byte TYPE_GAME_MODE_LIST_REQUEST	= Message.MIN_TYPE_IN_SUBCLASS	+  4 ;
	public static final byte TYPE_GAME_MODE_LIST			= Message.MIN_TYPE_IN_SUBCLASS	+  5 ;
	public static final byte TYPE_GAME_MODE_XML_REQUEST		= Message.MIN_TYPE_IN_SUBCLASS	+  6 ;
	public static final byte TYPE_GAME_MODE_XML				= Message.MIN_TYPE_IN_SUBCLASS	+  7 ;
	
	public static final byte TYPE_GAME_MODE_AUTHORIZATION_TOKEN = Message.MIN_TYPE_IN_SUBCLASS 	+  8 ;
	public static final byte TYPE_GAME_MODE_AUTHORIZATION_REVOKE = Message.MIN_TYPE_IN_SUBCLASS +  9 ;
	
	public static final byte TYPE_VOTE						= Message.MIN_TYPE_IN_SUBCLASS	+ 10 ;
	public static final byte TYPE_UNVOTE					= Message.MIN_TYPE_IN_SUBCLASS	+ 11 ;
	
	public static final byte TYPE_GAME_MODE_VOTE_REQUEST	= Message.MIN_TYPE_IN_SUBCLASS	+ 12 ;
	public static final byte TYPE_GAME_MODE_VOTES			= Message.MIN_TYPE_IN_SUBCLASS	+ 13 ;
	
	public static final byte TYPE_ACTIVE 					= Message.MIN_TYPE_IN_SUBCLASS 	+ 14 ;
	public static final byte TYPE_INACTIVE	 				= Message.MIN_TYPE_IN_SUBCLASS  + 15 ;
	public static final byte TYPE_IN_GAME					= Message.MIN_TYPE_IN_SUBCLASS  + 16 ;
	
	public static final byte TYPE_PLAYER_STATUSES				= Message.MIN_TYPE_IN_SUBCLASS 	+ 17 ;
	public static final byte TYPE_PLAYER_STATUSES_REQUEST		= Message.MIN_TYPE_IN_SUBCLASS 	+ 18 ;
	public static final byte TYPE_REWELCOME_REQUEST				= Message.MIN_TYPE_IN_SUBCLASS  + 19 ;
	

	public static final byte TYPE_GAME_MODE_LAUNCH_COUNTDOWN = Message.MIN_TYPE_IN_SUBCLASS + 20 ;
	public static final byte TYPE_GAME_MODE_LAUNCH_ABORTED 	= Message.MIN_TYPE_IN_SUBCLASS 	+ 21 ;
	public static final byte TYPE_GAME_MODE_LAUNCH_HALTED 	= Message.MIN_TYPE_IN_SUBCLASS 	+ 22 ;
	public static final byte TYPE_GAME_MODE_LAUNCH_FAILED	= Message.MIN_TYPE_IN_SUBCLASS	+ 23 ;
	
	public static final byte TYPE_GAME_MODE_LAUNCH_AS_ABSENT						= Message.MIN_TYPE_IN_SUBCLASS	+ 24 ;
	public static final byte TYPE_GAME_MODE_LAUNCH_AS_DIRECT_CLIENT					= Message.MIN_TYPE_IN_SUBCLASS	+ 25 ;
	public static final byte TYPE_GAME_MODE_LAUNCH_AS_DIRECT_HOST					= Message.MIN_TYPE_IN_SUBCLASS	+ 26 ;
	// PREVIOUSLY USED; now an open type 			Message.MIN_TYPE_IN_SUBCLASS	+ 27 ;
	// PREVIOUSLY USED; now an open type			Message.MIN_TYPE_IN_SUBCLASS	+ 28 ;
	public static final byte TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_CLIENT			= Message.MIN_TYPE_IN_SUBCLASS	+ 29 ;
	public static final byte TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST				= Message.MIN_TYPE_IN_SUBCLASS	+ 30 ;
	// revised to contain game server information and 'act as' information.
		// Previously LAUNCH was fairly content-free message; it had to be, to 
		// let clients automatically move to the game even if they failed to
		// receive the message.  However, now that Lobbies remain open and running
		// during games, we no longer allow "auto-launch" by clients.
		// All necessary launch information is contained in this message.
	
	// User preferences!
	public static final byte TYPE_PREFERRED_COLOR 				= Message.MIN_TYPE_IN_SUBCLASS	+ 35 ;

	int versionCode ;
	
	String hostPlayerName ;
	
	protected int [] playerSlotArray ;
	int playerSlotArrayLength ;
	
	int gameMode ;
	
	int [] gameModeList ;
	
	int port ;
	SocketAddress sockAddr ;
	SocketAddress sockUDPAddr ;
	int numPlayers ;
	int maxPlayers ;
	
	long delay ;
	
	int countdownNumber ;
	
	int countdownStatus ;
	
	Nonce editKey ;
	
	Serializable authToken ;
	
	
	public LobbyMessage() {
		super() ;
		
		hostPlayerName = null ;
		
		sockAddr = null ;
		
		playerSlotArray = null ;
		playerSlotArrayLength = 0 ;
		
		gameMode = 0 ;
		delay = 0 ;
		
		editKey = null ;
		
		authToken = null ;
	}
	
	
	@Override
	protected void nullOutsideReferences() {
		super.nullOutsideReferences() ;
		
		hostPlayerName = null ;
		
		sockAddr = null ;
		numPlayers = -1 ;
		maxPlayers = -1 ;
		countdownNumber = -1 ;
		
		editKey = null ;
		
		authToken = null ;
	}




	@Override
	protected int messageContentLength() {
		
		int len ;
		
		// If we get here, this is a message that we write as simple bytes.
		switch( type ) {
		
		// Includes the game mode as an int, and then the byte-array
		// representation of the text string.  We use getBytes to
		// send the byte array, so send the resulting length.
		case TYPE_TEXT_MESSAGE:
		case TYPE_GAME_MODE_XML:
			byte [] ar = string.getBytes() ;
			return ar.length + 8 ;
			
			
		// This message has a BUNCH of info! 
		// A nonce, numPlayers, maxPlayers, age (long), a socket address, and a lobby name.
		// The socket address and the lobby name are optional.  We write each as
		// 'sublength' 'subcontent', so take their length and add 4 to get the number
		// of bytes required for each.
		case TYPE_LOBBY_STATUS:
			// Address, name, and host name are optional.  We use length to encode which, if
			// either, is present.
			len = 0 ;
			len += 4 ;				// version code
			len += nonce.lengthAsBytes() ;		// nonce
			len += 4 + 4 + 8 ;		// numPlayers, maxPlayers, age
			len += 4 + (sockAddr == null ? 0 : ByteArrayOps.lengthOfSocketAddressAsBytes( sockAddr ) ) ;
			len += 4 + (string == null ? 0 : string.getBytes().length) ;
			len += 4 + (hostPlayerName == null ? 0 : hostPlayerName.getBytes().length) ;
			return len ;
			
		// Lobby status requset has a port number attached.
		case TYPE_LOBBY_STATUS_REQUEST:
			return 4 ;
			
		// These message have 0 content, so there is nothing to do.
		case TYPE_GAME_MODE_LIST_REQUEST:
			return 0 ;
			
		// These messages have only a game mode as their content.
		case TYPE_GAME_MODE_XML_REQUEST:
		case TYPE_GAME_MODE_VOTE_REQUEST:
			return 4 ;
		
		// These messages have a player slot AND a game mode.
		case TYPE_VOTE:
		case TYPE_UNVOTE:
			return 8 ;
			
		// These messages contain a player slot.
		case TYPE_ACTIVE:
		case TYPE_INACTIVE:
		case TYPE_IN_GAME:
			return 4 ;
		
		// No content
		case TYPE_PLAYER_STATUSES_REQUEST:
			return 0 ;
		
		// These messages contain a list of players.
		case TYPE_PLAYER_STATUSES:
			return playerSlotArrayLength * 4 ;
		
		// This message gives a list of players.  It is variable length,
		// but has playerSlotArrayLength * 4 bytes.
		case TYPE_PLAYERS_IN_LOBBY:
			return playerSlotArrayLength * 4 ;
		
		// This message has a list of game modes.
		case TYPE_GAME_MODE_LIST:
			return gameModeList.length * 4 ;
			
		// Authorization token!  We don't care what the auth token is, so long as
		// it is Serializable.  We also include the game mode int as an additional 4 bytes.
		case TYPE_GAME_MODE_AUTHORIZATION_TOKEN:
			len = 0 ;
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
				ObjectOutputStream oos = new ObjectOutputStream( baos ) ;
				oos.writeObject(authToken) ;
				oos.close() ;
				len = baos.toByteArray().length ;
			} catch ( IOException ioe ) {
				ioe.printStackTrace() ;
			}
			return 4 + len ;
			
		// To revoke, we just need a game mode.
		case TYPE_GAME_MODE_AUTHORIZATION_REVOKE:
			return 4 ;
		
		// This message includes both a game mode and a list of players.
		case TYPE_GAME_MODE_VOTES:
			return playerSlotArrayLength * 4 + 4 ;
			
			
		// Includes the countdown number.  Also, for convenience, includes the game
		// mode and players who were included in the countdown.
		
		case TYPE_GAME_MODE_LAUNCH_ABORTED:
		case TYPE_GAME_MODE_LAUNCH_HALTED:
		case TYPE_GAME_MODE_LAUNCH_FAILED:
			return playerSlotArrayLength * 4 + 4 + 4 ;
		
		// These message include a number, a game mode, a long (launch delay),
		// a status (integer), and a list of players.
		case TYPE_GAME_MODE_LAUNCH_COUNTDOWN:
			return playerSlotArrayLength * 4 + 4 + 4 + 8 + 4 ;
			
		// Includes the countdown number.  Also, for convenience, includes the game
		// mode and the players who are included in the countdown.
		case TYPE_GAME_MODE_LAUNCH_AS_ABSENT:
			return playerSlotArrayLength * 4 + 4 + 4 ;
			
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce, and the direct connection host address.
		// Because nonces have variable length, we need to explicitly include
		// the slot array length as a 4-byte element.
		case TYPE_GAME_MODE_LAUNCH_AS_DIRECT_CLIENT:
		case TYPE_GAME_MODE_LAUNCH_AS_DIRECT_HOST:	
			return playerSlotArrayLength * 4 + 4 + 4 + 4 + nonce.lengthAsBytes() + 8 ;
			
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce and the matchseeker socket address.
		// Because nonces have variable length, we need to explicitly include
		// the slot array length as a 4-byte element.
		case TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_CLIENT:
			return playerSlotArrayLength * 4 + 4 + 4 + 4 + nonce.lengthAsBytes() + 8 ;
		
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce , an edit key, and the matchseeker socket address.
		// Because nonces have variable length, we need to explicitly include
		// the slot array length as a 4-byte element.
		case TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST:
			return playerSlotArrayLength * 4 + 4 + 4 + 4
					+ nonce.lengthAsBytes() + editKey.lengthAsBytes() + 8 ;
			
		case TYPE_PREFERRED_COLOR:
			return 8 ;		// 2 ints; a player slot and a 
			
		default:
			// WHHHAHAAAAAA?A??????
			return 0 ;
		}
		
	}




	@Override
	protected void writeMessageContent(Object outputDest) throws IOException {
		makeByteArrayIfNeeded() ;
		
		int len ;
		byte [] textAsBytes ;

		
		// If we get here, this is amessage that we write as simple bytes.
		switch( type ) {
		
		// This message has a BUNCH of info! 
		// A nonce, numPlayers, maxPlayers, age (long), a socket address, and a lobby name.
		// The socket address and the lobby name are optional.  We write each as
		// 'sublength' 'subcontent', so take their length and add 4 to get the number
		// of bytes required for each.
		case TYPE_LOBBY_STATUS:
			len = nonce.writeAsBytes(byteArray, 0) ;
			ByteArrayOps.writeIntAsBytes( versionCode, byteArray, len ) ;
			ByteArrayOps.writeIntAsBytes( numPlayers, byteArray, len+4 ) ;
			ByteArrayOps.writeIntAsBytes( maxPlayers, byteArray, len+8 ) ;
			ByteArrayOps.writeLongAsBytes( delay, byteArray, len+12 ) ;
			len += 20 ;
			if ( sockAddr == null ) {
				ByteArrayOps.writeIntAsBytes(-1, byteArray, len) ;
				len += 4 ;
			} else {
				ByteArrayOps.writeIntAsBytes(
						ByteArrayOps.lengthOfSocketAddressAsBytes(sockAddr),
						byteArray, len) ;
				len += 4 ;
				len += ByteArrayOps.writeSocketAddressAsBytes(sockAddr, byteArray, len) ;
			}
			if ( string == null ) {
				textAsBytes = null ;
				ByteArrayOps.writeIntAsBytes(-1, byteArray, len) ;
				len += 4 ;
			}
			else {
				textAsBytes = string.getBytes() ;
				ByteArrayOps.writeIntAsBytes(textAsBytes.length, byteArray, len) ;
				len += 4 ;
				for ( int i = 0; i < textAsBytes.length; i++ )
					byteArray[len+i] = textAsBytes[i] ;
				len += textAsBytes.length ;
			}
			if ( hostPlayerName == null ) {
				textAsBytes = null ;
				ByteArrayOps.writeIntAsBytes(-1, byteArray, len) ;
				len += 4 ;
			}
			else {
				textAsBytes = hostPlayerName.getBytes() ;
				ByteArrayOps.writeIntAsBytes(textAsBytes.length, byteArray, len) ;
				len += 4 ;
				for ( int i = 0; i < textAsBytes.length; i++ )
					byteArray[len+i] = textAsBytes[i] ;
				len += textAsBytes.length ;
			}
			
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			
			break ;
				
			
			
		
		// This message has player slot, length, and message bytes.
		case TYPE_TEXT_MESSAGE:
			textAsBytes = string.getBytes() ;
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			ByteArrayOps.writeIntAsBytes(textAsBytes.length, eightByteArray, 4) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 8) ;
			this.writeBytesInByteArray(outputDest, textAsBytes, 0, textAsBytes.length) ;
			break ;
			
			
		// This message has game mode, length, and message bytes
		case TYPE_GAME_MODE_XML:
			textAsBytes = string.getBytes() ;
			ByteArrayOps.writeIntAsBytes(gameMode, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			this.writeBytesInByteArray(outputDest, textAsBytes, 0, textAsBytes.length) ;
			break ;
			
			
		// These message have 0 content, so there is nothing to do.
		case TYPE_GAME_MODE_LIST_REQUEST:
			break ;
			
		// These messages have only a gameMode as their content.
		case TYPE_GAME_MODE_XML_REQUEST:
		case TYPE_GAME_MODE_VOTE_REQUEST:
			ByteArrayOps.writeIntAsBytes(gameMode, byteArray, 0) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, 4) ;
			break ;
			
		// The Lobby Status request message has a port number attached.
		case TYPE_LOBBY_STATUS_REQUEST:
			ByteArrayOps.writeIntAsBytes(port, byteArray, 0) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, 4) ;
			break ;

		
		// These messages have a player slot AND a game mode.
		case TYPE_VOTE:
		case TYPE_UNVOTE:
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			ByteArrayOps.writeIntAsBytes(gameMode, eightByteArray, 4) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 8) ;
			break ;
			
		// Player slot.
		case TYPE_ACTIVE:
		case TYPE_INACTIVE:
		case TYPE_IN_GAME:
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			break ;
			
		// No content
		case TYPE_PLAYER_STATUSES_REQUEST:
			break ;
		
		// These messages contain a list of players.
		case TYPE_PLAYER_STATUSES:
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, playerSlotArrayLength * 4) ;
			break ;
		
		// This message gives a list of players.  It is variable length,
		// but has length/4 integers.
		case TYPE_PLAYERS_IN_LOBBY:
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, playerSlotArrayLength * 4) ;
			break ;
			
		// This message has a list of game modes.
		case TYPE_GAME_MODE_LIST:
			for ( int i = 0; i < gameModeList.length; i++ )
				ByteArrayOps.writeIntAsBytes(gameModeList[i], byteArray, i*4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, gameModeList.length * 4) ;
			break ;
			
			
		// Here's an authorization token.
		case TYPE_GAME_MODE_AUTHORIZATION_TOKEN:
			ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
			ObjectOutputStream oos = new ObjectOutputStream( baos ) ;
			oos.writeObject(authToken) ;
			oos.close() ;
			byte [] barray = baos.toByteArray() ;
			
			ByteArrayOps.writeIntAsBytes(gameMode, byteArray, 0) ;
			for ( int i = 0; i < barray.length; i++ )
				byteArray[i+4] = barray[i] ;
			
			this.writeBytesInByteArray(outputDest, byteArray, 0, barray.length + 4) ;
			break ;
			
			
		// To revoke a token, we just need the game mode.
		case TYPE_GAME_MODE_AUTHORIZATION_REVOKE:
			ByteArrayOps.writeIntAsBytes(gameMode, byteArray, 0) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, 4) ;
			break ;
			
		
		// These messages include both a game mode and a list of players.
		case TYPE_GAME_MODE_VOTES:
			ByteArrayOps.writeIntAsBytes(gameMode, eightByteArray, 0) ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, playerSlotArrayLength*4) ;
			break ;
			
		// Includes countdown number, game mode, and a list of players.
		case TYPE_GAME_MODE_LAUNCH_ABORTED:
		case TYPE_GAME_MODE_LAUNCH_HALTED:
		case TYPE_GAME_MODE_LAUNCH_FAILED:
		case TYPE_GAME_MODE_LAUNCH_AS_ABSENT:
			ByteArrayOps.writeIntAsBytes(countdownNumber, eightByteArray, 0) ;
			ByteArrayOps.writeIntAsBytes(gameMode, eightByteArray, 4) ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 8) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, playerSlotArrayLength*4) ;
			break ;
			
		// This message has a game mode, a delay (long), and a list of players.
		case TYPE_GAME_MODE_LAUNCH_COUNTDOWN:
			ByteArrayOps.writeIntAsBytes(countdownNumber, byteArray, 0) ;		// 0-4
			ByteArrayOps.writeIntAsBytes(gameMode, byteArray, 4) ;				// 4-8
			ByteArrayOps.writeLongAsBytes(delay, byteArray, 8) ;					// 8-16
			ByteArrayOps.writeIntAsBytes(countdownStatus, byteArray, 16) ;			// 16-20
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4 + 20) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, playerSlotArrayLength*4 + 20) ;
			break ;
			
			
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce, and the direct connection host address.
		// Because nonce has variable length, we need to explicitly note the 
		// playerSlotArrayLength (can't infer it from message length).
		case TYPE_GAME_MODE_LAUNCH_AS_DIRECT_CLIENT:
		case TYPE_GAME_MODE_LAUNCH_AS_DIRECT_HOST:	
			len = 0 ;
			ByteArrayOps.writeIntAsBytes(countdownNumber, byteArray, 0) ;
			ByteArrayOps.writeIntAsBytes(gameMode, byteArray, 4) ;
			ByteArrayOps.writeIntAsBytes(playerSlotArrayLength, byteArray, 8) ;
			len = 12 ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4 + len) ;
			len += playerSlotArrayLength * 4 ;
			// include nonce and address.
			len += nonce.writeAsBytes(byteArray, len) ;
			len += ByteArrayOps.writeSocketAddressAsBytes(sockAddr, byteArray, len) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			break ;
			
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce and the matchseeker socket address.
		// Because nonces have variable length, we need to explicitly include
		// the slot array length as a 4-byte element.
		case TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_CLIENT:
			len = 0 ;
			ByteArrayOps.writeIntAsBytes(countdownNumber, byteArray, 0) ;
			ByteArrayOps.writeIntAsBytes(gameMode, byteArray, 4) ;
			ByteArrayOps.writeIntAsBytes(playerSlotArrayLength, byteArray, 8) ;
			len = 12 ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4 + len) ;
			len += playerSlotArrayLength * 4 ;
			// include nonce and address.
			len += nonce.writeAsBytes(byteArray, len) ;
			len += ByteArrayOps.writeSocketAddressAsBytes(sockAddr, byteArray, len) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			break ;
		
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce , an edit key, and the matchseeker socket address.
		// Because nonces have variable length, we need to explicitly include
		// the slot array length as a 4-byte element.
		case TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST:
			len = 0 ;
			ByteArrayOps.writeIntAsBytes(countdownNumber, byteArray, 0) ;
			ByteArrayOps.writeIntAsBytes(gameMode, byteArray, 4) ;
			ByteArrayOps.writeIntAsBytes(playerSlotArrayLength, byteArray, 8) ;
			len = 12 ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				ByteArrayOps.writeIntAsBytes(playerSlotArray[i], byteArray, i*4 + len) ;
			len += playerSlotArrayLength * 4 ;
			// include nonce and address.
			len += nonce.writeAsBytes(byteArray, len) ;
			len += editKey.writeAsBytes(byteArray, len) ;
			len += ByteArrayOps.writeSocketAddressAsBytes(sockAddr, byteArray, len) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			break ;
			
		case TYPE_PREFERRED_COLOR:
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			ByteArrayOps.writeIntAsBytes(gameMode, eightByteArray, 4) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 8) ;
			break ;
			
		}
		

	}




	@Override
	protected boolean readMessageContent(Object inputSource) throws IOException,
			ClassNotFoundException {
		
		makeByteArrayIfNeeded() ;
		
		int len ;
		
		int stringLength ;
		
		// Read the bytes we need here using Message's helper function,
		// then only process them if we have the full buffer length
		// (i.e. readBytesIntoByteBuffer returns true.
		if ( !readBytesIntoByteArray( inputSource ) ) {
			// Incomplete.  Return false.
			return false ;
		}
		
		
		// We have the message contents.  Now set the relevant
		// field for the message type.
		switch( type ) {
		
		case TYPE_LOBBY_STATUS:
			nonce = new Nonce( byteArray ) ;
			len = nonce.lengthAsBytes() ;
			versionCode = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			numPlayers = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			maxPlayers = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			delay = ByteArrayOps.readLongAsBytes(byteArray, len) ;
			len += 8 ;
			
			int sockAddrLen = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			
			if ( sockAddrLen >= 0 ) {
				sockAddr = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
				len += sockAddrLen ;
			}
			
			stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			
			if ( stringLength >= 0 ) {
				string = new String( byteArray, len, stringLength ) ;
				len += stringLength ;
			}
			
			stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			
			if ( stringLength >= 0 ) {
				hostPlayerName = new String( byteArray, len, stringLength ) ;
				len += stringLength ;
			}
			
			break ;
		
		// Have already read player slot and byte array length.
		case TYPE_TEXT_MESSAGE:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			stringLength = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			string = new String( byteArray, 8, stringLength ) ;
			break ;
		
		// Have already read game mode and byte array length.
		case TYPE_GAME_MODE_XML:
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			stringLength = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			string = new String( byteArray, 8, stringLength ) ;
			break ;
		
		
			
		// These message have 0 content, so there is nothing to do.
		case TYPE_GAME_MODE_LIST_REQUEST:
			break ;
			
		// These messages have only a game mode as their content.
		case TYPE_GAME_MODE_XML_REQUEST:
		case TYPE_GAME_MODE_VOTE_REQUEST:
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		// The Lobby Status request message has a port number attached.
		case TYPE_LOBBY_STATUS_REQUEST:
			port = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		// These messages have a player slot AND a game mode.
		case TYPE_VOTE:
		case TYPE_UNVOTE:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			break ;
		
		case TYPE_ACTIVE:
		case TYPE_INACTIVE:
		case TYPE_IN_GAME:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		case TYPE_PLAYER_STATUSES_REQUEST:
			break ;
		
		// These messages contain a list of players.
		case TYPE_PLAYER_STATUSES:
			playerSlotArrayLength = length / 4 ;
			if ( MAX_ARRAY_LENGTH < playerSlotArrayLength )
				throw new IOException("Array too long: " + playerSlotArrayLength) ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, i*4) ;
			break ;

		
		// This message gives a list of players.  It is variable length,
		// but has length/4 integers.
		case TYPE_PLAYERS_IN_LOBBY:
			playerSlotArrayLength = length / 4 ;
			if ( MAX_ARRAY_LENGTH < playerSlotArrayLength )
				throw new IOException("Array too long: " + playerSlotArrayLength) ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, i*4) ;
			break ;
		
		// This message gives a list of game modes.
		case TYPE_GAME_MODE_LIST:
			if ( MAX_ARRAY_LENGTH < length / 4 )
				throw new IOException("Array too long: " + playerSlotArrayLength) ;
			gameModeList = new int[ length / 4 ] ;
			for ( int i = 0; i < gameModeList.length; i++ )
				gameModeList[i] = ByteArrayOps.readIntAsBytes(byteArray, i*4) ;
			break ;
			
		
			// Here's an authorization token.
		case TYPE_GAME_MODE_AUTHORIZATION_TOKEN:
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			ByteArrayInputStream bais = new ByteArrayInputStream( byteArray, 4, length-4 ) ;
			ObjectInputStream ois = new ObjectInputStream( bais ) ;
			authToken = (Serializable)ois.readObject() ;
			break ;
			
			
		// To revoke a token, we just need the game mode.
		case TYPE_GAME_MODE_AUTHORIZATION_REVOKE:
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		
		// These messages include both a game mode and a list of players.
		case TYPE_GAME_MODE_VOTES:
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			length -= 4 ;
			playerSlotArrayLength = length / 4 ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, i*4 + 4) ;
			break ;
			
			
		case TYPE_GAME_MODE_LAUNCH_AS_ABSENT:
		case TYPE_GAME_MODE_LAUNCH_ABORTED:
		case TYPE_GAME_MODE_LAUNCH_HALTED:
		case TYPE_GAME_MODE_LAUNCH_FAILED:
			countdownNumber = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			length -= 8 ;
			playerSlotArrayLength = length / 4 ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, i*4 + 8) ;
			break ;
			
		// This message has a game mode, a delay (long), and a list of players.
		case TYPE_GAME_MODE_LAUNCH_COUNTDOWN:
			countdownNumber = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			delay = ByteArrayOps.readLongAsBytes(byteArray, 8) ;
			countdownStatus = ByteArrayOps.readIntAsBytes(byteArray, 16) ;
			length -= 20 ;
			playerSlotArrayLength = length / 4 ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, i*4 + 20) ;
			break ;
			
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce, and the direct connection host address.
		// Because nonce has variable length, we need to explicitly note the 
		// playerSlotArrayLength (can't infer it from message length).
		case TYPE_GAME_MODE_LAUNCH_AS_DIRECT_CLIENT:
		case TYPE_GAME_MODE_LAUNCH_AS_DIRECT_HOST:	
			countdownNumber = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			playerSlotArrayLength = ByteArrayOps.readIntAsBytes(byteArray, 8) ;
			len = 12 ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, len + i*4 ) ;
			len += playerSlotArrayLength * 4 ;
			nonce = new Nonce( byteArray, len ) ;
			len += nonce.lengthAsBytes() ;
			sockAddr = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
			break ;
		
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce and the matchseeker socket address.
		// Because nonces have variable length, we need to explicitly include
		// the slot array length as a 4-byte element.
		case TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_CLIENT:
			countdownNumber = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			playerSlotArrayLength = ByteArrayOps.readIntAsBytes(byteArray, 8) ;
			len = 12 ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, len + i*4 ) ;
			len += playerSlotArrayLength * 4 ;
			nonce = new Nonce( byteArray, len ) ;
			len += nonce.lengthAsBytes() ;
			sockAddr = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
			break ;
		
		// Standard launch info: countdown number, game mode, and players.
		// Also includes a nonce , an edit key, and the matchseeker socket address.
		// Because nonces have variable length, we need to explicitly include
		// the slot array length as a 4-byte element.
		case TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST:
			countdownNumber = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			playerSlotArrayLength = ByteArrayOps.readIntAsBytes(byteArray, 8) ;
			len = 12 ;
			if ( playerSlotArray == null || playerSlotArray.length < playerSlotArrayLength )
				playerSlotArray = new int[playerSlotArrayLength] ;
			for ( int i = 0; i < playerSlotArrayLength; i++ )
				playerSlotArray[i] = ByteArrayOps.readIntAsBytes(byteArray, len + i*4 ) ;
			len += playerSlotArrayLength * 4 ;
			nonce = new Nonce( byteArray, len ) ;
			len += nonce.lengthAsBytes() ;
			editKey = new Nonce( byteArray, len ) ;
			len += editKey.lengthAsBytes() ;
			sockAddr = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
			break ;
			
		case TYPE_PREFERRED_COLOR:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			gameMode = ByteArrayOps.readIntAsBytes(byteArray, 4) ;
			break ;
			
		}
		
		
		return true ;
	}
	

	
	
	/////////////////////////////////////////////////////
	//
	// GETTER / SETTER FOR MESSAGE TYPES
	//
	/////////////////////////////////////////////////////
	
	public int getVersionCode() {
		return versionCode ;
	}
	
	public int getStatuses( int [] pSlot ) {
		if ( pSlot != null ) {
			try {
				for ( int i = 0; i < pSlot.length; i++ )
					pSlot[i] = Lobby.PLAYER_STATUS_NOT_CONNECTED ;
				for ( int i = 0; i < playerSlotArrayLength; i++ )
					pSlot[i] = playerSlotArray[i] ;
			} catch ( ArrayIndexOutOfBoundsException e ) {
				e.printStackTrace() ;
				for ( int i = 0; i < playerSlotArrayLength; i++ )
					System.err.print("" + playerSlotArray[i] + " "); 
				System.err.println() ;
				
				throw new RuntimeException("Crashing program so you can see how bad you've been.") ;
			}
		}
		
		return playerSlotArrayLength ;
	}
	
	public int getPlayers( boolean [] pSlot ) {
		
		if ( pSlot != null ) {
			try {
				for ( int i = 0; i < pSlot.length; i++ )
					pSlot[i] = false ;
				for ( int i = 0; i < playerSlotArrayLength; i++ )
					pSlot[ playerSlotArray[i] ] = true ;
			} catch ( ArrayIndexOutOfBoundsException e ) {
				e.printStackTrace() ;
				for ( int i = 0; i < playerSlotArrayLength; i++ )
					System.err.print("" + playerSlotArray[i] + " "); 
				System.err.println() ;
				
				throw new RuntimeException("Crashing program so you can see how bad you've been.") ;
			}
		}
		
		return playerSlotArrayLength ;
	}
	
	public String getHostPlayerName() {
		return hostPlayerName ;
	}

	public int getPort() {
		return port ;
	}
	
	public SocketAddress getSocketAddress() {
		return sockAddr ;
	}
	
	public SocketAddress getUDPSocketAddress() {
		return sockUDPAddr ;
	}
	
	public Nonce getEditKey() {
		return editKey ;
	}
	
	public int getNumPlayers() {
		return numPlayers ;
	}
	
	public int getMaxPlayers() {
		return maxPlayers >= 0 ? maxPlayers : playerSlot ;
	}
	
	public long getDelay() {
		return delay ;
	}
	
	public long getAge() {
		return delay ;
	}
	
	public int [] getGameModeList() {
		int [] list = new int[ gameModeList.length ] ;
		for ( int i = 0; i < gameModeList.length; i++ ) 
			list[i] = gameModeList[i] ;
		return list ;
	}
	
	public String getGameModeXML() {
		return string ;
	}
	
	
	public int getGameMode() {
		return gameMode ;
	}
	
	
	public int getCountdownNumber() {
		return countdownNumber ;
	}
	
	public int getCountdownStatus() {
		return countdownStatus ;
	}
	
	public int getColor() {
		return gameMode ;		// we store color info in gameMode.
	}
	
	public Serializable getAuthToken() {
		return authToken ;
	}
	
	
	public LobbyMessage setAsLobbyStatus( int minSupportedVersionCode, Nonce nonce, int numPlayers, int maxPlayers, long age, SocketAddress sockAddr, String lobbyName, String hostPlayerName ) {
		nullOutsideReferences() ;
		
		type = TYPE_LOBBY_STATUS ;
		this.versionCode = minSupportedVersionCode ;
		this.nonce = nonce ;
		this.sockAddr = sockAddr ;
		this.numPlayers = numPlayers ;
		this.maxPlayers = maxPlayers ;
		this.string = lobbyName ;
		this.delay = age ;
		this.hostPlayerName = hostPlayerName ;
		return this ;
	}
	
	
	public LobbyMessage setAsLobbyStatusRequest( int port ) {
		nullOutsideReferences() ;
		
		type = TYPE_LOBBY_STATUS_REQUEST ;
		this.port = port ;
		return this ;
	}


	
	
	public LobbyMessage setAsPlayersInLobby( boolean [] playerInLobby ) {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYERS_IN_LOBBY ;
		if ( playerSlotArray == null || playerSlotArray.length < playerInLobby.length )
			playerSlotArray = new int[playerInLobby.length] ;
		playerSlotArrayLength = 0 ;
		for ( int i = 0; i < playerInLobby.length; i++ ) {
			if ( playerInLobby[i] ) {
				playerSlotArray[ playerSlotArrayLength ] = i ;
				playerSlotArrayLength++ ;
			}
		}
		
		return this ;
	}
	
	public LobbyMessage setAsTextMessage( int playerSlot, String text ) {
		nullOutsideReferences() ;
		
		type = TYPE_TEXT_MESSAGE ;
		this.playerSlot = playerSlot ;
		string = text ;
		return this ;
	}
	
	
	public LobbyMessage setAsGameModeListRequest() {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_LIST_REQUEST ;
		return this ;
	}

	public LobbyMessage setAsGameModeList( int [] gameModes ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_LIST ;
		
		if ( gameModeList == null || gameModeList.length != gameModes.length )
			gameModeList = new int[gameModes.length] ;
		for ( int i = 0; i < gameModeList.length; i++ )
			gameModeList[i] = gameModes[i] ;
		return this ;
	}
	
	
	public LobbyMessage setAsGameModeAuthorizationToken( int gameMode, Serializable token ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_AUTHORIZATION_TOKEN ;
		this.gameMode = gameMode ;
		this.authToken = token ;
		return this ;
	}
	
	
	public LobbyMessage setAsGameModeAuthorizationRevoke( int gameMode ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_AUTHORIZATION_REVOKE ;
		this.gameMode = gameMode ;
		return this ;
	}
	
	
	public LobbyMessage setAsGameModeXMLRequest( int gameMode ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_XML_REQUEST ;
		this.gameMode = gameMode ;
		return this ;
	}
	
	public LobbyMessage setAsGameModeXML( int gameMode, String xmlText ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_XML ;
		
		// Something a little fancy here... we remove redundant whitespace.
		this.gameMode = gameMode ;
		String [] tokens = xmlText.split("\\s+") ;
		string = "" ;
		for ( int i = 0; i < tokens.length; i++ )
			string = string + tokens[i] + " " ;
		return this ;
	}
	
	public LobbyMessage setAsVote( int playerSlot, int gameMode ) {
		nullOutsideReferences() ;
		
		type = TYPE_VOTE ;
		this.playerSlot = playerSlot ;
		this.gameMode = gameMode ;
		return this ;
	}
	
	public LobbyMessage setAsUnvote( int playerSlot, int gameMode ) {
		nullOutsideReferences() ;
		
		type = TYPE_UNVOTE ;
		this.playerSlot = playerSlot ;
		this.gameMode = gameMode ;
		return this ;
	}
	
	public LobbyMessage setAsActive( int playerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_ACTIVE ;
		this.playerSlot = playerSlot ;
		return this ;
	}
	
	public LobbyMessage setAsInactive( int playerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_INACTIVE ;
		this.playerSlot = playerSlot ;
		return this ;
	}
	
	
	public LobbyMessage setAsInGame( int playerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_IN_GAME ;
		this.playerSlot = playerSlot ;
		return this ;
	}
	
	
	public LobbyMessage setAsPlayerStatuses( int [] playerStatus ) {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYER_STATUSES ;
		if ( playerSlotArray == null || playerSlotArray.length < playerStatus.length )
			playerSlotArray = new int[playerStatus.length] ;
		for ( int i = 0; i < playerStatus.length; i++ )
			playerSlotArray[i] = playerStatus[i] ;
		playerSlotArrayLength = playerStatus.length ;
		return this ;
	}
	
	
	public LobbyMessage setAsActivePlayersRequest() {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYER_STATUSES_REQUEST ;
		return this ;
	}
	
	public LobbyMessage setAsRewelcomeRequest() {
		nullOutsideReferences() ;
		
		type = TYPE_REWELCOME_REQUEST ;
		return this ;
	}
	
	
	public LobbyMessage setAsGameModeVotes( int gameMode, boolean [] playerDidVote ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_VOTES ;
		this.gameMode = gameMode ;
		if ( playerSlotArray == null || playerSlotArray.length < playerDidVote.length )
			playerSlotArray = new int[playerDidVote.length] ;
		playerSlotArrayLength = 0 ;
		for ( int i = 0; i < playerDidVote.length; i++ ) {
			if ( playerDidVote[i] ) {
				playerSlotArray[ playerSlotArrayLength ] = i ;
				playerSlotArrayLength++ ;
			}
		}
		return this ;
	}
	
	public LobbyMessage setAsGameModeVoteRequest( int gameMode ) {
		nullOutsideReferences() ;
		
		type = TYPE_GAME_MODE_VOTE_REQUEST ;
		this.gameMode = gameMode ;
		return this ;
	}
	
	
	public LobbyMessage setAsLaunchAborted( int countdownNumber, int gameMode, boolean [] playerInGame ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		type = TYPE_GAME_MODE_LAUNCH_ABORTED ;
		return this ;
	}
	
	public LobbyMessage setAsLaunchCountdown( int countdownNumber, int gameMode, long delay, int countdownStatus, boolean [] playerInGame ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		this.countdownStatus = countdownStatus ;
		this.delay = delay ;
		this.type = TYPE_GAME_MODE_LAUNCH_COUNTDOWN ;
		return this ;
	}
	
	public LobbyMessage setAsLaunchHalted( int countdownNumber, int gameMode, boolean [] playerInGame ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		type = TYPE_GAME_MODE_LAUNCH_HALTED ;
		return this ;
	}
	
	public LobbyMessage setAsLaunchFailed( int countdownNumber, int gameMode, boolean [] playerInGame ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		type = TYPE_GAME_MODE_LAUNCH_FAILED ;
		return this ;
	}
	
	
	public LobbyMessage setAsLaunchAsAbsent( int countdownNumber, int gameMode, boolean [] playerInGame ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		type = TYPE_GAME_MODE_LAUNCH_AS_ABSENT ;
		return this ;
	}
	
	
	public LobbyMessage setAsLaunchAsDirectClient( int countdownNumber, int gameMode, boolean [] playerInGame,
			Nonce nonce, SocketAddress directAddress ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		this.nonce = nonce ;
		this.sockAddr = directAddress ;
		type = TYPE_GAME_MODE_LAUNCH_AS_DIRECT_CLIENT ;
		return this ;
	}
	
	
	public LobbyMessage setAsLaunchAsDirectHost( int countdownNumber, int gameMode, boolean [] playerInGame,
			Nonce nonce, SocketAddress directAddress ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		this.nonce = nonce ;
		this.sockAddr = directAddress ;
		type = TYPE_GAME_MODE_LAUNCH_AS_DIRECT_HOST ;
		return this ;
	}
	
	public LobbyMessage setAsLaunchAsMatchseekerClient( int countdownNumber, int gameMode, boolean [] playerInGame,
			Nonce nonce, SocketAddress matchseekerAddress ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		this.nonce = nonce ;
		this.sockAddr = matchseekerAddress ;
		type = TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_CLIENT ;
		return this ;
	}
	
	public LobbyMessage setAsLaunchAsMatchseekerHost( int countdownNumber, int gameMode, boolean [] playerInGame,
			Nonce nonce, Nonce editKey, SocketAddress matchseekerAddress ) {
		nullOutsideReferences() ;
		
		setLaunchData( countdownNumber, gameMode, playerInGame ) ;
		this.nonce = nonce ;
		this.editKey = editKey ;
		this.sockAddr = matchseekerAddress ;
		type = TYPE_GAME_MODE_LAUNCH_AS_MATCHSEEKER_HOST ;
		return this ;
	}
	
	
	private void setLaunchData( int countdownNumber, int gameMode, boolean [] playerInGame ) {
		this.countdownNumber = countdownNumber ;
		this.gameMode = gameMode ;
		if ( playerSlotArray == null || playerSlotArray.length < playerInGame.length )
			playerSlotArray = new int[playerInGame.length] ;
		playerSlotArrayLength = 0 ;
		for ( int i = 0; i < playerInGame.length; i++ ) {
			if ( playerInGame[i] ) {
				playerSlotArray[ playerSlotArrayLength ] = i ;
				playerSlotArrayLength++ ;
			}
		}
	}
	
	
	public LobbyMessage setAsPreferredColor( int playerSlot, int color ) {
		nullOutsideReferences() ;
		
		type = TYPE_PREFERRED_COLOR ;
		this.playerSlot = playerSlot ;
		this.gameMode = color ;
		return this ;
	}
	
	
	
	@Override
	public Message setAs( Message m ) {
		nullOutsideReferences() ;
		
		if ( !(m instanceof LobbyMessage) )
			throw new IllegalArgumentException("Can't set a LobbyMessage as a non-LobbyMessage!") ;
		
		// Call to super, then set our own instance vars.
		super.setAs(m) ;
		
		LobbyMessage myM = (LobbyMessage)m ;
		
		this.playerSlotArrayLength = myM.playerSlotArrayLength ;
		if ( myM.playerSlotArray != null ) {
			if ( this.playerSlotArray == null || this.playerSlotArray.length < myM.playerSlotArray.length )
				this.playerSlotArray = new int[myM.playerSlotArray.length] ;
			for ( int i = 0; i < myM.playerSlotArrayLength; i++ )
				this.playerSlotArray[i] = myM.playerSlotArray[i] ;
		}
		
		this.gameMode = myM.gameMode ;
		
		if ( myM.gameModeList != null ) {
			if ( this.gameModeList == null || this.gameModeList.length < myM.gameModeList.length )
				this.gameModeList = new int[myM.gameModeList.length] ;
			for ( int i = 0; i < myM.gameModeList.length; i++ )
				this.gameModeList[i] = myM.gameModeList[i] ;
		}
		
		this.sockAddr = myM.sockAddr ;
		this.sockUDPAddr = myM.sockUDPAddr ;
		this.port = myM.port ;
		this.numPlayers = myM.numPlayers ;
		this.maxPlayers = myM.maxPlayers ;
		this.delay = myM.delay ;
		this.countdownNumber = myM.countdownNumber ;
		this.countdownStatus = myM.countdownStatus ;
		this.editKey = myM.editKey ;
		this.authToken = myM.authToken ;
		this.hostPlayerName = myM.hostPlayerName ;
		
		return this ;
	}
	
}
