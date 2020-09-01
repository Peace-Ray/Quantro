package com.peaceray.quantro.communications;

import java.io.IOException;
import java.net.SocketAddress;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.utils.ByteArrayOps;



/**
 * Messages sent to-and-from the UDP Matchmaker.
 * 
 * We're replacing the TCP connector with a UDP matchmaker.
 * 
 * @author Jake
 *
 */
public class MatchmakingMessage extends Message {
	
	/**
	 * Override this method if your Message needs a larger buffer than the default.
	 * The result of this method call can differ based on the message content; over
	 * time, the buffer length will approach the maximum required size.
	 * @return
	 */
	@Override
	protected int BUFFER_SIZE() {
		return 8192 ;
	}
	
	// Message Type.
	public static final byte TYPE_PROMISE 				= Message.MIN_TYPE_IN_SUBCLASS + 0 ;	// from matchmaker; "I will alert you if a match appears within..."
	// empty space
	
	public static final byte TYPE_MATCHMAKER_REJECT_FULL 				= Message.MIN_TYPE_IN_SUBCLASS + 10 ;	// from matchmaker; "I am too busy to make a promise."
	public static final byte TYPE_MATCHMAKER_REJECT_INVALID_NONCE 		= Message.MIN_TYPE_IN_SUBCLASS + 11 ;	// from matchmaker; "Your nonce is invalid."
	public static final byte TYPE_MATCHMAKER_REJECT_NONCE_IN_USE 		= Message.MIN_TYPE_IN_SUBCLASS + 12 ;	// from matchmaker; "Nonce is in-use (whatever that means)."
	public static final byte TYPE_MATCHMAKER_REJECT_PORT_RANDOMIZATION 	= Message.MIN_TYPE_IN_SUBCLASS + 13 ;	// from matchmaker; "port randomization in-use.  Can't match you up with a partner."
	public static final byte TYPE_MATCHMAKER_REJECT_MATCHTICKET_ADDRESS_MISMATCH = Message.MIN_TYPE_IN_SUBCLASS + 14 ;	// from matchmaker: "Your matchticket was issued to a different address."
	public static final byte TYPE_MATCHMAKER_REJECT_MATCHTICKET_CONTENT_MISMATCH = Message.MIN_TYPE_IN_SUBCLASS + 15 ;	// from matchmaker: "Your matchticket content is invalid"
	public static final byte TYPE_MATCHMAKER_REJECT_MATCHTICKET_SIGNATURE 		 = Message.MIN_TYPE_IN_SUBCLASS + 16 ;	// from matchmaker: "Your matchticket signature is invalid"
	public static final byte TYPE_MATCHMAKER_REJECT_MATCHTICKET_PROOF 			 = Message.MIN_TYPE_IN_SUBCLASS + 17 ;	// from matchmaker: "Your matchticket proof-of-work is invalid"
	public static final byte TYPE_MATCHMAKER_REJECT_MATCHTICKET_EXPIRED 		 = Message.MIN_TYPE_IN_SUBCLASS + 18 ;	// from matchmaker: "Your matchticket has expired."
	// empty space
	public static final byte TYPE_MATCHMAKER_REJECT_UNSPECIFIED		= Message.MIN_TYPE_IN_SUBCLASS + 30 ;
	
	
	// REQUESTS!
	public static final byte TYPE_REQUEST_CHALLENGE		= Message.MIN_TYPE_IN_SUBCLASS + 50 ;
	
	public static final byte TYPE_REQUEST_MATCH			= Message.MIN_TYPE_IN_SUBCLASS + 52 ;
	
	// MATCHES!
	public static final byte TYPE_MATCH_CHALLENGE 		= Message.MIN_TYPE_IN_SUBCLASS + 100 ;	// A match for joo!
	
	public static final byte TYPE_MATCH 				= Message.MIN_TYPE_IN_SUBCLASS + 102 ;
	
	
	public static final byte INTENT_LOBBY = 0 ;
	public static final byte INTENT_GAME = 1 ;
	
	
	
	// Maximum sizes!
	public static final int MAX_LENGTH_USER_ID_BYTES = 60 ;
	public static final int MAX_LENGTH_REQUEST_ID_BYTES = 60 ;
	
	
	
	byte intent ;
	long [] duration ;
	String [] user_id ;
	String [] request_id ;
	int [] requestPort ;
	
	SocketAddress [][] udp_addr ;
	
	
	String matchticket ;
	Nonce matchticket_proof ;
	
	int failed_punches ;
	
	
	public MatchmakingMessage() {
		duration = new long[2] ;
		user_id = new String[2] ;
		request_id = new String[2] ;
		requestPort = new int[2] ;
		
		udp_addr = new SocketAddress[2][2] ;
		
		matchticket = null ;
		matchticket_proof = null ;
	}
	
	
	public byte getIntent() {
		return intent ;
	}
	
	public boolean getIntentIsLobby() {
		return intent == INTENT_LOBBY ;
	}
	
	public boolean getIntentIsGame() {
		return intent == INTENT_GAME ;
	}
	
	public long getLocalDuration()  { return duration[0] ; }
	public long getRemoteDuration() { return duration[1] ; }
	
	public String getLocalUserID()  { return user_id[0] ; }
	public String getRemoteUserID() { return user_id[1] ; }
	
	public String getLocalRequestID()  { return request_id[0] ; }
	public String getRemoteRequestID() { return request_id[1] ; }
	
	public int getLocalNumSocketAddresses() { return udp_addr[0].length ; } 
	public int getRemoteNumSocketAddresses() { return udp_addr[1].length ; } 
	public SocketAddress getLocalSocketAddress( int num )  { return udp_addr[0][num] ; }
	public SocketAddress getRemoteSocketAddress( int num ) { return udp_addr[1][num] ; }
	
	public String getMatchticket() { return matchticket ; }
	public Nonce getMatchticketProof() { return matchticket_proof ; }
	

	@Override
	protected int messageContentLength() {
		int len ;
		
		switch( type ) {
		case TYPE_REQUEST_CHALLENGE:
			// intent
			// nonce
			// unique_id byte length
			// unique_id
			// request_id byte length
			// request_id
			// contact port
			// "my local match socket address"
			len = 1 + nonce.lengthAsBytes() ;	// intent, nonce
			len += 8 + user_id[0].getBytes().length + request_id[0].getBytes().length ; 	// IDs
			return len + 4 + ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[0][0]);		// port, addr
		
		
		case TYPE_REQUEST_MATCH:
			// intent
			// nonce
			// unique_id byte length
			// unique_id
			// request_id byte length
			// request_id
			// contact port
			// "my local match socket address"
			// matchticket byte length
			// matchticket
			// matchticket effort proof nonce
			// # failed hole punch attempts
			len = 1 + nonce.lengthAsBytes() ;	// intent, nonce
			len += 8 + user_id[0].getBytes().length + request_id[0].getBytes().length ; 	// IDs
			len += 4 + ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[0][0]);		// port, addr
			len += 4 + matchticket.getBytes().length ;		// matchticket.
			return len + matchticket_proof.lengthAsBytes() + 4 ;	// extra four: # hole punching failures
		}
		
		return 0 ;
	}

	@Override
	protected void writeMessageContent(Object outputDest) throws IOException {
		makeByteArrayIfNeeded() ;
		
		int len ;
		byte [] textAsBytes ;
		
		switch( type ) {
		case TYPE_REQUEST_CHALLENGE:
			// this is the one of only two types we write.  Its format is:
			// intent
			// nonce
			// unique_id byte length
			// unique_id
			// request_id byte length
			// request_id
			// source_port		<-- port used for this connection (will be compared against originating address)
			// match_address 	<-- my local address (app-visible) and the port we want to match on.
			
			byteArray[0] = intent ;
			len = nonce.writeAsBytes(byteArray, 1) + 1 ;
			
			// write unique_id and request_id
			for ( int s = 0; s < 2; s++ ) {
				textAsBytes = s == 0 ? user_id[0].getBytes() : request_id[0].getBytes() ;
				ByteArrayOps.writeIntAsBytes(textAsBytes.length, byteArray, len) ;
				len += 4 ;
				for ( int i = 0; i < textAsBytes.length; i++ )
					byteArray[len+i] = textAsBytes[i] ;
				len += textAsBytes.length ;
			}
			
			// source port and match address
			ByteArrayOps.writeIntAsBytes(requestPort[0], byteArray, len) ;
			len += 4 ;
			// match address
			ByteArrayOps.writeSocketAddressAsBytes(udp_addr[0][0], byteArray, len) ;
			len += ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[0][0]) ;
			
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			break ;
			
		case TYPE_REQUEST_MATCH:
			// This is the other type we write.  Its format is:
			// intent
			// nonce
			// unique_id byte length
			// unique_id
			// request_id byte length
			// request_id
			// contact port
			// "my local match socket address"
			// matchticket byte length
			// matchticket
			// matchticket effort proof nonce
			
			byteArray[0] = intent ;
			len = nonce.writeAsBytes(byteArray, 1) + 1 ;
			
			// write unique_id and request_id
			for ( int s = 0; s < 2; s++ ) {
				textAsBytes = s == 0 ? user_id[0].getBytes() : request_id[0].getBytes() ;
				ByteArrayOps.writeIntAsBytes(textAsBytes.length, byteArray, len) ;
				len += 4 ;
				for ( int i = 0; i < textAsBytes.length; i++ )
					byteArray[len+i] = textAsBytes[i] ;
				len += textAsBytes.length ;
			}
			
			// source port and match address
			ByteArrayOps.writeIntAsBytes(requestPort[0], byteArray, len) ;
			len += 4 ;
			// match address
			ByteArrayOps.writeSocketAddressAsBytes(udp_addr[0][0], byteArray, len) ;
			len += ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[0][0]) ;
			
			// matchticket
			textAsBytes = matchticket.getBytes() ;
			ByteArrayOps.writeIntAsBytes(textAsBytes.length, byteArray, len) ;
			len += 4 ;
			for ( int i = 0; i < textAsBytes.length; i++ )
				byteArray[len+i] = textAsBytes[i] ;
			len += textAsBytes.length ;
			
			// matchticket proof
			len += matchticket_proof.writeAsBytes(byteArray, len) ;
			
			// # failed hole punches
			ByteArrayOps.writeIntAsBytes(failed_punches, byteArray, len) ;
			len += 4 ;
			
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			break ;
		}
	}

	@Override
	protected boolean readMessageContent(Object inputSource)
			throws IOException, ClassNotFoundException {
		
		makeByteArrayIfNeeded() ;
		
		int len ;
		
		int stringLength ;
		
		byte [] strBytes ;
		
		// Read the bytes we need here using Message's helper function,
		// then only process them if we have the full buffer length
		// (i.e. readBytesIntoByteBuffer returns true.
		if ( !readBytesIntoByteArray( inputSource ) ) {
			// Incomplete.  Return false.
			return false ;
		}
		
		
		switch( type ) {
		case TYPE_PROMISE:
			// a promise contains an echo of the user_id and request_id.  It 
			// also includes the length of time we can expect the promise to
			// linger; in other words, the server has guaranteed to let us
			// know if a match is found within that many milliseconds.
			duration[0] = ByteArrayOps.readLongAsBytes(byteArray, 0) ;
			len = 8 ;
			// get user_id and request_id.
			for ( int s = 0; s < 2; s++ ) {
				stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
				len += 4 ;
				strBytes = new byte[stringLength] ;
				for ( int i = 0; i < stringLength; i++ )
					strBytes[i] = byteArray[i + len] ;
				len += stringLength ;
				if ( s == 0 )
					user_id[0] = new String(strBytes) ;
				else
					request_id[0] = new String(strBytes) ;
			}
			break ;
		
		case TYPE_MATCHMAKER_REJECT_FULL:
		case TYPE_MATCHMAKER_REJECT_INVALID_NONCE:
		case TYPE_MATCHMAKER_REJECT_NONCE_IN_USE:
		case TYPE_MATCHMAKER_REJECT_PORT_RANDOMIZATION:
		case TYPE_MATCHMAKER_REJECT_UNSPECIFIED:
		case TYPE_MATCHMAKER_REJECT_MATCHTICKET_ADDRESS_MISMATCH:
		case TYPE_MATCHMAKER_REJECT_MATCHTICKET_CONTENT_MISMATCH:
		case TYPE_MATCHMAKER_REJECT_MATCHTICKET_SIGNATURE:
		case TYPE_MATCHMAKER_REJECT_MATCHTICKET_PROOF:
		case TYPE_MATCHMAKER_REJECT_MATCHTICKET_EXPIRED:
		
			// these include the user_id and request_id.
			len = 0 ;
			for ( int s = 0; s < 2; s++ ) {
				stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
				len += 4 ;
				strBytes = new byte[stringLength] ;
				for ( int i = 0; i < stringLength; i++ )
					strBytes[i] = byteArray[i + len] ;
				len += stringLength ;
				if ( s == 0 )
					user_id[0] = new String(strBytes) ;
				else
					request_id[0] = new String(strBytes) ;
			}
			break ;
		
		
		// REQUESTS!
		case TYPE_REQUEST_CHALLENGE:
			// why would we ever read this?  Ah, well.
			intent = byteArray[0] ;
			nonce = new Nonce( byteArray, 1 ) ;
			len = nonce.lengthAsBytes() + 1 ;
			
			for ( int s = 0; s < 2; s++ ) {
				stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
				len += 4 ;
				strBytes = new byte[stringLength] ;
				for ( int i = 0; i < stringLength; i++ )
					strBytes[i] = byteArray[i + len] ;
				len += stringLength ;
				if ( s == 0 )
					user_id[0] = new String(strBytes) ;
				else
					request_id[0] = new String(strBytes) ;
			}
			
			requestPort[0] = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			if ( udp_addr == null )
				udp_addr = new SocketAddress[2][2] ;
			udp_addr[0][0] = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
			len += ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[0][0]) ;
			
			break ;
			
		case TYPE_REQUEST_MATCH:
			// why would we ever read this?  Ah, well.
			intent = byteArray[0] ;
			nonce = new Nonce( byteArray, 1 ) ;
			len = nonce.lengthAsBytes() + 1 ;
			
			for ( int s = 0; s < 2; s++ ) {
				stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
				len += 4 ;
				strBytes = new byte[stringLength] ;
				for ( int i = 0; i < stringLength; i++ )
					strBytes[i] = byteArray[i + len] ;
				len += stringLength ;
				if ( s == 0 )
					user_id[0] = new String(strBytes) ;
				else
					request_id[0] = new String(strBytes) ;
			}
			
			requestPort[0] = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			if ( udp_addr == null )
				udp_addr = new SocketAddress[2][2] ;
			udp_addr[0][0] = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
			len += ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[0][0]) ;
			
			stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			strBytes = new byte[stringLength] ;
			for ( int i = 0; i < stringLength; i++ )
				strBytes[i] = byteArray[i + len] ;
			len += stringLength ;
			matchticket = new String(strBytes) ;
			
			matchticket_proof = new Nonce( byteArray, len ) ;
			len += matchticket_proof.lengthAsBytes() ;
			
			failed_punches = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			
			break ;
		
		// MATCHES!
		case TYPE_MATCH_CHALLENGE:
			// includes an intent and nonce, then - in order - the following
			// info about ME (local), followed by the same info about MY MATCH (remote).
			// user_id
			// request_id
			// request age (a 'long' indicating when this request came in - can be
			//			used for priority calculations).
			// # udp_socket_address
			// udp_socket_addresses
			// 
			intent = byteArray[0] ;
			nonce = new Nonce( byteArray, 1 ) ;
			len = nonce.lengthAsBytes() + 1 ;
			
			udp_addr = new SocketAddress[2][] ;
			
			for ( int u = 0; u < 2; u++ ) {
				// user_id, request_id.
				for ( int s = 0; s < 2; s++ ) {
					stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
					len += 4 ;
					strBytes = new byte[stringLength] ;
					for ( int i = 0; i < stringLength; i++ )
						strBytes[i] = byteArray[i + len] ;
					len += stringLength ;
					if ( s == 0 )
						user_id[u] = new String(strBytes) ;
					else
						request_id[u] = new String(strBytes) ;
				}
				
				// request age
				duration[u] = ByteArrayOps.readLongAsBytes(byteArray, len) ;
				len += 8 ;
				
				// addresses.
				int numAddr = ByteArrayOps.readIntAsBytes(byteArray, len) ;
				len += 4 ;
				udp_addr[u] = new SocketAddress[numAddr] ;
				
				for ( int j = 0; j < numAddr; j++ ) {
					udp_addr[u][j] = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
					len += ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[u][j]) ;
				}
			}
			
			break ;
			
			
			
			// MATCHES!
		case TYPE_MATCH:
			// includes an intent and nonce, then - in order - the following
			// info about ME (local), followed by the same info about MY MATCH (remote).
			// user_id
			// request_id
			// request age (a 'long' indicating when this request came in - can be
			//			used for priority calculations).
			// # udp_socket_address
			// udp_socket_addresses
			// 
			intent = byteArray[0] ;
			nonce = new Nonce( byteArray, 1 ) ;
			len = nonce.lengthAsBytes() + 1 ;
			
			udp_addr = new SocketAddress[2][] ;
			
			for ( int u = 0; u < 2; u++ ) {
				// user_id, request_id.
				for ( int s = 0; s < 2; s++ ) {
					stringLength = ByteArrayOps.readIntAsBytes(byteArray, len) ;
					len += 4 ;
					strBytes = new byte[stringLength] ;
					for ( int i = 0; i < stringLength; i++ )
						strBytes[i] = byteArray[i + len] ;
					len += stringLength ;
					if ( s == 0 )
						user_id[u] = new String(strBytes) ;
					else
						request_id[u] = new String(strBytes) ;
				}
				
				// request age
				duration[u] = ByteArrayOps.readLongAsBytes(byteArray, len) ;
				len += 8 ;
				
				// addresses.
				int numAddr = ByteArrayOps.readIntAsBytes(byteArray, len) ;
				len += 4 ;
				udp_addr[u] = new SocketAddress[numAddr] ;
				
				for ( int j = 0; j < numAddr; j++ ) {
					udp_addr[u][j] = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
					len += ByteArrayOps.lengthOfSocketAddressAsBytes(udp_addr[u][j]) ;
				}
			}
			
			break ;
		}
		
		return true ;
	}
	
	
	@Override
	public void nullOutsideReferences() {
		super.nullOutsideReferences() ;
		
		for ( int i = 0; i < 2; i++ ) {
			user_id[i] = null ;
			request_id[i] = null ;
			if ( udp_addr[i] != null ) 
				for ( int j = 0; j < udp_addr[i].length; j++ )
					udp_addr[i][j] = null ;
		}
		
		matchticket = null ;
		matchticket_proof = null ;
	}
	
	public MatchmakingMessage setAsMatchRequestChallengeLobby( Nonce nonce, String user_id, String request_id, int sourcePort, SocketAddress userVisibleMatchAddress ) {
		nullOutsideReferences() ;
		
		if ( sourcePort < 0 )
			throw new IllegalArgumentException("Source port must by >= 0.  For 'same-socket' matchmaking, call w/o port argument.") ;
		
		setAsMatchRequestChallenge( INTENT_LOBBY, nonce, user_id, request_id, sourcePort, userVisibleMatchAddress ) ;
		return this ;
	}
	
	public MatchmakingMessage setAsMatchRequestChallengeGame( Nonce nonce, String user_id, String request_id, int sourcePort, SocketAddress userVisibleMatchAddress ) {
		nullOutsideReferences() ;
		
		if ( sourcePort < 0 )
			throw new IllegalArgumentException("Source port must by >= 0.  For 'same-socket' matchmaking, call w/o port argument.") ;
		
		setAsMatchRequestChallenge( INTENT_GAME, nonce, user_id, request_id, sourcePort, userVisibleMatchAddress ) ;
		return this ;
	}
	
	public void setAsMatchRequestChallenge( byte intent, Nonce nonce, String user_id, String request_id, int sourcePort, SocketAddress userVisibleMatchAddress ) {
		if ( sourcePort < 0 )
			throw new IllegalArgumentException("Source port must by >= 0.  For 'same-socket' matchmaking, call w/o port argument.") ;
		if ( user_id.getBytes().length > MAX_LENGTH_USER_ID_BYTES )
			throw new IllegalArgumentException("UserID is too long; byte representation is " + user_id.getBytes().length) ;
		if ( request_id.getBytes().length > MAX_LENGTH_REQUEST_ID_BYTES )
			throw new IllegalArgumentException("RequestID is too long; byte representation is " + request_id.getBytes().length) ;
		
		
		type = TYPE_REQUEST_CHALLENGE ;
		this.intent = intent ;
		this.nonce = nonce ;
		this.user_id[0] = user_id ;
		this.request_id[0] = request_id ;
		this.requestPort[0] = sourcePort ;
		this.udp_addr = new SocketAddress[2][1] ;
		this.udp_addr[0][0] = userVisibleMatchAddress ;
	}
	
	
	public MatchmakingMessage setAsMatchRequestMatchLobby( Nonce nonce, String user_id, String request_id, int sourcePort, SocketAddress userVisibleMatchAddress, String matchticket, Nonce matchticketProof ) {
		nullOutsideReferences() ;
		
		if ( sourcePort < 0 )
			throw new IllegalArgumentException("Source port must by >= 0.  For 'same-socket' matchmaking, call w/o port argument.") ;
		
		setAsMatchRequestMatch( INTENT_LOBBY, nonce, user_id, request_id, sourcePort, userVisibleMatchAddress, matchticket, matchticketProof ) ;
		return this ;
	}
	
	public MatchmakingMessage setAsMatchRequestMatchGame( Nonce nonce, String user_id, String request_id, int sourcePort, SocketAddress userVisibleMatchAddress, String matchticket, Nonce matchticketProof ) {
		nullOutsideReferences() ;
		
		if ( sourcePort < 0 )
			throw new IllegalArgumentException("Source port must by >= 0.  For 'same-socket' matchmaking, call w/o port argument.") ;
		
		setAsMatchRequestMatch( INTENT_GAME, nonce, user_id, request_id, sourcePort, userVisibleMatchAddress, matchticket, matchticketProof ) ;
		return this ;
	}
	
	public void setAsMatchRequestMatch( byte intent, Nonce nonce, String user_id, String request_id, int sourcePort, SocketAddress userVisibleMatchAddress, String matchticket, Nonce matchticketProof ) {
		if ( sourcePort < 0 )
			throw new IllegalArgumentException("Source port must by >= 0.  For 'same-socket' matchmaking, call w/o port argument.") ;
		if ( user_id.getBytes().length > MAX_LENGTH_USER_ID_BYTES )
			throw new IllegalArgumentException("UserID is too long; byte representation is " + user_id.getBytes().length) ;
		if ( request_id.getBytes().length > MAX_LENGTH_REQUEST_ID_BYTES )
			throw new IllegalArgumentException("RequestID is too long; byte representation is " + request_id.getBytes().length) ;
		
		
		type = TYPE_REQUEST_MATCH ;
		this.intent = intent ;
		this.nonce = nonce ;
		this.user_id[0] = user_id ;
		this.request_id[0] = request_id ;
		this.requestPort[0] = sourcePort ;
		this.udp_addr = new SocketAddress[2][1] ;
		this.udp_addr[0][0] = userVisibleMatchAddress ;
		this.matchticket = matchticket ;
		this.matchticket_proof = matchticketProof ;
	}
	
	
	
	
	public MatchmakingMessage setAsMatchRequestChallengeLobby( Nonce nonce, String user_id, String request_id, SocketAddress userVisibleMatchAddress ) {
		nullOutsideReferences() ;
		
		setAsMatchRequestChallenge( INTENT_LOBBY, nonce, user_id, request_id, userVisibleMatchAddress ) ;
		return this ;
	}
	
	public MatchmakingMessage setAsMatchRequestChallengeGame( Nonce nonce, String user_id, String request_id, SocketAddress userVisibleMatchAddress ) {
		nullOutsideReferences() ;
		
		setAsMatchRequestChallenge( INTENT_GAME, nonce, user_id, request_id, userVisibleMatchAddress ) ;
		return this ;
	}
	
	public void setAsMatchRequestChallenge( byte intent, Nonce nonce, String user_id, String request_id, SocketAddress userVisibleMatchAddress ) {
		if ( user_id.getBytes().length > MAX_LENGTH_USER_ID_BYTES )
			throw new IllegalArgumentException("UserID is too long; byte representation is " + user_id.getBytes().length) ;
		if ( request_id.getBytes().length > MAX_LENGTH_REQUEST_ID_BYTES )
			throw new IllegalArgumentException("RequestID is too long; byte representation is " + request_id.getBytes().length) ;
		
		
		type = TYPE_REQUEST_CHALLENGE ;
		this.intent = intent ;
		this.nonce = nonce ;
		this.user_id[0] = user_id ;
		this.request_id[0] = request_id ;
		this.requestPort[0] = -1 ;
		this.udp_addr = new SocketAddress[2][1] ;
		this.udp_addr[0][0] = userVisibleMatchAddress ;
	}
	
	
	public MatchmakingMessage setAsMatchRequestMatchLobby( Nonce nonce, String user_id, String request_id, SocketAddress userVisibleMatchAddress, String matchticket, Nonce matchticketProof, int numFailedHolePunches ) {
		nullOutsideReferences() ;
		
		setAsMatchRequestMatch( INTENT_LOBBY, nonce, user_id, request_id, userVisibleMatchAddress, matchticket, matchticketProof, numFailedHolePunches ) ;
		return this ;
	}
	
	public MatchmakingMessage setAsMatchRequestMatchGame( Nonce nonce, String user_id, String request_id, SocketAddress userVisibleMatchAddress, String matchticket, Nonce matchticketProof, int numFailedHolePunches ) {
		nullOutsideReferences() ;
		
		setAsMatchRequestMatch( INTENT_GAME, nonce, user_id, request_id, userVisibleMatchAddress, matchticket, matchticketProof, numFailedHolePunches ) ;
		return this ;
	}
	
	public void setAsMatchRequestMatch( byte intent, Nonce nonce, String user_id, String request_id, SocketAddress userVisibleMatchAddress, String matchticket, Nonce matchticketProof, int numFailedHolePunches ) {
		if ( user_id.getBytes().length > MAX_LENGTH_USER_ID_BYTES )
			throw new IllegalArgumentException("UserID is too long; byte representation is " + user_id.getBytes().length) ;
		if ( request_id.getBytes().length > MAX_LENGTH_REQUEST_ID_BYTES )
			throw new IllegalArgumentException("RequestID is too long; byte representation is " + request_id.getBytes().length) ;
		
		
		type = TYPE_REQUEST_MATCH ;
		this.intent = intent ;
		this.nonce = nonce ;
		this.user_id[0] = user_id ;
		this.request_id[0] = request_id ;
		this.requestPort[0] = -1 ;
		this.udp_addr = new SocketAddress[2][1] ;
		this.udp_addr[0][0] = userVisibleMatchAddress ;
		this.matchticket = matchticket ;
		this.matchticket_proof = matchticketProof ;
		this.failed_punches = numFailedHolePunches ;
	}
	
	
	
}
