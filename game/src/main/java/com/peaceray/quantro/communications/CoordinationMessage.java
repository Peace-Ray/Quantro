package com.peaceray.quantro.communications;

import java.io.IOException;
import java.net.SocketAddress;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.utils.ByteArrayOps;

public class CoordinationMessage extends Message {
	
	// MESSAGE TYPE
	// Sent by the interrogator to prompt an intent, nonce and personal nonce.
	public static final byte TYPE_INTERROGATION							= Message.MIN_TYPE_IN_SUBCLASS  + 0 ;		// 0
	
	// An intent for this coordination.  The user will send this intent to
	// the passive interrogation component of the server.  Intent is the first
	// message we send; we follow it up with nonce, personal nonce, etc.
	public static final byte TYPE_INTENT_TO_SERVER						= Message.MIN_TYPE_IN_SUBCLASS	+ 1 ;		// 0
	
	// Here's the server response.  It might kick the user and provide a
	// basic explanation, OR it might allow the player, which for now means
	// informing them of the current status of the lobby or game they're
	// trying to join - i.e., the number of players currently connected 
	// for it.  This number is particularly important, especially for lobbies,
	// because responsibility of acting like the lobby "host" goes to the first
	// person to show up.
	public static final byte TYPE_SERVER_ACCEPT							= Message.MIN_TYPE_IN_SUBCLASS	+ 2 ;		// 1
	public static final byte TYPE_SERVER_REJECT_FULL					= Message.MIN_TYPE_IN_SUBCLASS	+ 3 ;
	public static final byte TYPE_SERVER_REJECT_INVALID_NONCE			= Message.MIN_TYPE_IN_SUBCLASS	+ 4 ;
	public static final byte TYPE_SERVER_REJECT_NONCE_IN_USE			= Message.MIN_TYPE_IN_SUBCLASS	+ 5 ;
	// NOTE: The use of these message types is VERY STRICTLY limited.
	// The client sends an Intent To Server message first, then the
	// server responds with one of the ACCEPT or REJECT messages.
	// After that, none of these message types is sent on the connection
	// again.  This restriction eases the transition to a "WrappedSocket"
	// when appropriate (i.e., when we start being directly connected to
	// another user).
	
	// 
	// To leave room for other types of server rejections, we start
	// counting the next set of messages at 50.
	//
	
	// For direct-connection.  For a given intent, we might
	// prefer to force people to speak directly to each other.
	public static final byte TYPE_BECOME_PASS_THROUGH					= Message.MIN_TYPE_IN_SUBCLASS	+ 50 ;		// 50
	public static final byte TYPE_NOW_PASS_THROUGH						= Message.MIN_TYPE_IN_SUBCLASS	+ 51 ;
		// This socket should now be treated as a direct connection
		// to the client (or host).  Once the server receives
		// NOW_PASS_THROUGH it will send every message after this 
		// directly to the other party.
	
	// Prepare for UDP pass-through.  The server requires some
	// specific information from clients to attempt pass-through.
	public static final byte TYPE_UDP_ADDRESSES_REQUEST					= Message.MIN_TYPE_IN_SUBCLASS	+ 52 ;
	public static final byte TYPE_UDP_ADDRESSES							= Message.MIN_TYPE_IN_SUBCLASS	+ 53 ;
	public static final byte TYPE_UDP_WHEREAMI_FAILED					= Message.MIN_TYPE_IN_SUBCLASS	+ 54 ;
	
	public static final byte TYPE_ATTEMPT_UDP_HOLE_PUNCH				= Message.MIN_TYPE_IN_SUBCLASS	+ 55 ;
	public static final byte TYPE_UDP_HOLE_PUNCH_FAILED					= Message.MIN_TYPE_IN_SUBCLASS	+ 56 ;
	public static final byte TYPE_UDP_HOLE_PUNCH_SUCCEEDED				= Message.MIN_TYPE_IN_SUBCLASS 	+ 57 ;
		// Mediator says to try a hole punch.  Client and host
		// respond with success / failure.
	
	// Ping/pongs to keep an active connection going with the mediator.
	public static final byte TYPE_PING									= Message.MIN_TYPE_IN_SUBCLASS	+ 58 ;
	public static final byte TYPE_PONG									= Message.MIN_TYPE_IN_SUBCLASS	+ 59 ;
	
	public static final byte TYPE_START_NEW_UDP_HOLE_PUNCH_ATTEMPT 		= Message.MIN_TYPE_IN_SUBCLASS	+ 60 ;
	
	// Mediation data packets.  We use these messages to request
	// connection updates from the mediator, and for the mediator
	// to send those updates to us.
	public static final byte TYPE_REQUEST_MEDIATOR_UPDATES 				= Message.MIN_TYPE_IN_SUBCLASS	+ 61 ;
	public static final byte TYPE_STOP_MEDIATOR_UPDATES 				= Message.MIN_TYPE_IN_SUBCLASS 	+ 62 ;
	public static final byte TYPE_MEDIATOR_WILL_UPDATE 					= Message.MIN_TYPE_IN_SUBCLASS 	+ 63 ;
	public static final byte TYPE_MEDIATOR_TOO_BUSY_TO_UPDATE			= Message.MIN_TYPE_IN_SUBCLASS	+ 64 ;
	public static final byte TYPE_MEDIATOR_UPDATE 						= Message.MIN_TYPE_IN_SUBCLASS 	+ 65 ;
	
	
	// The number of people.
	private int numberOfPeople ;
	
	// UDP hole punching requires two SocketAddresses - private and public.
	// We actually handle this as an array, because why not?
	private SocketAddress [] addresses ;
	// hole punching also requires nonces - a nonce for "me" (stored in 'nonce' the field for
	// messages) and for "target" (stored in 'targetNonce' below)
	private Nonce targetNonce ;
	// UDP hole punching also needs a reasonable timeout, which the server specifies, in milliseconds.
	private int timeout ;
	
	// Here's where we store our intent.
	private byte intent ;
	public static final byte INTENT_LOBBY				= 0 ;
	public static final byte INTENT_GAME 				= 1 ;
	
	// Pass-through sockets require a address, a port, and a special Nonce.
	// We have address and port storage above, and already have a place to put
	// a Nonce (in 'nonce').
	
	
	
	public CoordinationMessage() {
		super() ;
		addresses = null ;
		targetNonce = null ;
	}
	
	
	@Override
	protected void nullOutsideReferences() {
		super.nullOutsideReferences() ;
		addresses = null ;
		targetNonce = null ;
	}

	@Override
	protected int messageContentLength() {
		int len ;
		
		switch( type ) {
		
		case TYPE_INTERROGATION:
			// No content.
			return 0;
		
		case TYPE_INTENT_TO_SERVER:
			// Intent to server: our intent can be LOBBY, GAME HOST,
			// GAME SERVER, etc.  We encode intent as a byte.
			return 1 ;

		case TYPE_SERVER_ACCEPT:
			// The server provides a number - the number of people currently
			// connected using the same nonce.  For convenience we just do an
			// int; this isn't a frequent message, the overhead shouldn't be too
			// bad.
			return 4 ;
			
		case TYPE_SERVER_REJECT_FULL:
		case TYPE_SERVER_REJECT_INVALID_NONCE:
			// These messages, for the moment, do not carry any content.
			return 0 ;
		
		// For direct-connection.  For a given intent, we might
		// prefer to force people to speak directly to each other.
		case TYPE_BECOME_PASS_THROUGH:
		case TYPE_NOW_PASS_THROUGH:
			// Becoming a pass-through does not require any additional metadata;
			// neither does noting that we are now a pass-through.
			return 0 ;
			
			
		case TYPE_UDP_ADDRESSES_REQUEST:
			// No content for this
			return 0 ;
			
		case TYPE_UDP_ADDRESSES:
			// Provides a list of addresses, preceeded by the number of addresses.
			len = 4 ;		// number of addresses.
			for ( int i = 0; i < addresses.length; i++ )
				len += ByteArrayOps.lengthOfSocketAddressAsBytes( addresses[i] ) ;
			return len ;
			
		case TYPE_UDP_WHEREAMI_FAILED:
			// meh.  We don't need to provide any other information.
			return 0 ;
		
		case TYPE_ATTEMPT_UDP_HOLE_PUNCH:
			// A UDP hole punch attempt requires a bunch of information:
			// two nonces, a set of addresses, an int specifying the
			// number of addresses and an int giving the timeout.
			len = 4 + 4 ;		// two ints
			len += nonce.lengthAsBytes() + targetNonce.lengthAsBytes() ;
			for ( int i = 0; i < addresses.length; i++ )
				len += ByteArrayOps.lengthOfSocketAddressAsBytes( addresses[i] ) ;
			return len ;
			
		case TYPE_UDP_HOLE_PUNCH_FAILED:
		case TYPE_UDP_HOLE_PUNCH_SUCCEEDED:
			// We report the nonce we used when trying this hole-punching.
			return nonce.lengthAsBytes() ;

		// Ping/pongs to keep an active connection going with the mediator.
		case TYPE_PING:
		case TYPE_PONG:
			// No content to this message!
			return 0 ;
			
		case TYPE_START_NEW_UDP_HOLE_PUNCH_ATTEMPT:
			// No content.  This message just tells the other party that we will
			// be advancing to the next hole punch attempt number.
			return 0 ;
			
		case TYPE_REQUEST_MEDIATOR_UPDATES:
		case TYPE_STOP_MEDIATOR_UPDATES:
		case TYPE_MEDIATOR_UPDATE:
			// We use 'nonce' and 'targetNonce' as the lobby/game nonce, and
			// our personal nonce, respectively.  Both are included in these
			// message, and there is no other content.
			return 1 + nonce.lengthAsBytes() + targetNonce.lengthAsBytes() ;
			
		case TYPE_MEDIATOR_WILL_UPDATE:
		case TYPE_MEDIATOR_TOO_BUSY_TO_UPDATE:
			// Empty message.
			return 0 ;
			
		default:
			// whaaaaaaa?
			return 0 ;
		}
	}

	@Override
	protected void writeMessageContent(Object outputDest) throws IOException {
		makeByteArrayIfNeeded() ;
		int len ;
		
		switch( type ) {
		case TYPE_INTERROGATION:
			// No content.
			return ;
			
		case TYPE_INTENT_TO_SERVER:
			// Intent to server: our intent can be LOBBY, GAME HOST,
			// GAME SERVER, etc.  We encode intent as a byte.
			eightByteArray[0] = intent ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 1) ;
			return ;

		case TYPE_SERVER_ACCEPT:
			// The server provides a number - the number of people currently
			// connected using the same nonce.  For convenience we just do an
			// int; this isn't a frequent message, the overhead shouldn't be too
			// bad.
			ByteArrayOps.writeIntAsBytes(numberOfPeople, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			return ;
			
		case TYPE_SERVER_REJECT_FULL:
		case TYPE_SERVER_REJECT_INVALID_NONCE:
			return ;
		
		// For direct-connection.  For a given intent, we might
		// prefer to force people to speak directly to each other.
		case TYPE_BECOME_PASS_THROUGH:
		case TYPE_NOW_PASS_THROUGH:
			// Becoming a pass-through does not require any additional metadata;
			// neither does noting that we are now a pass-through.
			return ;
			
			
		case TYPE_UDP_ADDRESSES_REQUEST:
			// No content for this
			return ;
			
		case TYPE_UDP_ADDRESSES:
			// Provides a list of addresses, preceeded by the number of addresses.
			ByteArrayOps.writeIntAsBytes(addresses.length, eightByteArray, 0) ;
			len = 0 ;
			for ( int i = 0; i < addresses.length; i++ )
				len += ByteArrayOps.writeSocketAddressAsBytes(addresses[i], byteArray, len) ;
			// Now write them
			this.writeBytesInByteArray(outputDest, eightByteArray, 0, 4) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			return ;
			
		case TYPE_UDP_WHEREAMI_FAILED:
			// meh.  We don't need to provide any other information.
			return ;
		
		case TYPE_ATTEMPT_UDP_HOLE_PUNCH:
			// A UDP hole punch attempt requires a bunch of information:
			// two nonces, a set of addresses, an int specifying the
			// number of addresses and an int giving the timeout.
			// We write two nonces first.
			len = nonce.writeAsBytes(byteArray, 0) ;
			len += targetNonce.writeAsBytes(byteArray, len) ;
			// Now the number of addresses
			ByteArrayOps.writeIntAsBytes( addresses.length, byteArray, len ) ;
			len += 4 ;
			for ( int i = 0; i < addresses.length; i++ )
				len += ByteArrayOps.writeSocketAddressAsBytes( addresses[i], byteArray, len ) ;
			ByteArrayOps.writeIntAsBytes(timeout, byteArray, len) ;
			len += 4 ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			return ;
			
			
		case TYPE_UDP_HOLE_PUNCH_FAILED:
		case TYPE_UDP_HOLE_PUNCH_SUCCEEDED:
			len = nonce.writeAsBytes(byteArray, 0) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			return ;

		// Ping/pongs to keep an active connection going with the mediator.
		case TYPE_PING:
		case TYPE_PONG:
			// No content to this message!
			return ;
			
		case TYPE_START_NEW_UDP_HOLE_PUNCH_ATTEMPT:
			// no content!
			return ;
			
		case TYPE_REQUEST_MEDIATOR_UPDATES:
		case TYPE_STOP_MEDIATOR_UPDATES:
		case TYPE_MEDIATOR_UPDATE:
			// We use 'nonce' and 'targetNonce' as the lobby/game nonce, and
			// our personal nonce, respectively.  Both are included in these
			// message, after an intent.
			byteArray[0] = intent ;
			len = 1 ;
			len += nonce.writeAsBytes(byteArray, len) ;
			len += targetNonce.writeAsBytes(byteArray, len) ;
			this.writeBytesInByteArray(outputDest, byteArray, 0, len) ;
			break ;
			
		case TYPE_MEDIATOR_WILL_UPDATE:
		case TYPE_MEDIATOR_TOO_BUSY_TO_UPDATE:
			// Empty message.
			return ;

		default:
			// whaaaaaaa?
			return ;
		}
	}

	@Override
	protected boolean readMessageContent(Object inputSource) throws IOException,
			ClassNotFoundException {
		
		makeByteArrayIfNeeded() ;
		
		
		// Attempt to read content bytes
		if ( !readBytesIntoByteArray( inputSource ) ) {
			// Incomplete.  Return false.
			return false ;
		}
		
		int num ;
		int len ;
		
		// No other message type has any content.  However, just
		// to make sure, verify that it is a valid type.
		switch( type ) {
		case TYPE_INTERROGATION:
			// No content.
			break ;
			
		case TYPE_INTENT_TO_SERVER:
			intent = byteArray[0] ;
			// Intent to server: our intent can be LOBBY, GAME HOST,
			// GAME SERVER, etc.  We encode intent as a byte.
			break ;

		case TYPE_SERVER_ACCEPT:
			// The server provides a number - the number of people currently
			// connected using the same nonce.  For convenience we just do an
			// int; this isn't a frequent message, the overhead shouldn't be too
			// bad.
			numberOfPeople = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		case TYPE_SERVER_REJECT_FULL:
		case TYPE_SERVER_REJECT_INVALID_NONCE:
			// No content here
			break ;
		
		// For direct-connection.  For a given intent, we might
		// prefer to force people to speak directly to each other.
		case TYPE_BECOME_PASS_THROUGH:
		case TYPE_NOW_PASS_THROUGH:
			// No content
			break ;
			
			
		case TYPE_UDP_ADDRESSES_REQUEST:
			// No content for this
			break ;
			
		case TYPE_UDP_ADDRESSES:
			// Provides a list of addresses, preceeded by the number of addresses.
			num = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			if ( addresses == null || addresses.length != num )
				addresses = new SocketAddress[num] ;
			len = 4 ;
			for ( int i = 0; i < addresses.length; i++ ) {
				addresses[i] = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
				len += ByteArrayOps.lengthOfSocketAddressAsBytes(addresses[i]) ;
			}
			break ;
			
		case TYPE_UDP_WHEREAMI_FAILED:
			// meh.  We don't need to provide any other information.
			break ;
		
		case TYPE_ATTEMPT_UDP_HOLE_PUNCH:
			// A UDP hole punch attempt requires a bunch of information:
			// two nonces, a set of addresses, an int specifying the
			// number of addresses and an int giving the timeout.
			// We write two nonces first.
			nonce = new Nonce( byteArray, 0 ) ;
			len = nonce.lengthAsBytes() ;
			targetNonce = new Nonce( byteArray, len ) ;
			len += targetNonce.lengthAsBytes() ;
			// Now the number of addresses...
			num = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			len += 4 ;
			if ( addresses == null || addresses.length != num )
				addresses = new SocketAddress[num] ;
			for ( int i = 0; i < addresses.length; i++ ) {
				addresses[i] = ByteArrayOps.readSocketAddressAsBytes(byteArray, len) ;
				len += ByteArrayOps.lengthOfSocketAddressAsBytes(addresses[i]) ;
			}
			// Finally, the timeout
			timeout = ByteArrayOps.readIntAsBytes(byteArray, len) ;
			break ;
			
			
		case TYPE_UDP_HOLE_PUNCH_FAILED:
		case TYPE_UDP_HOLE_PUNCH_SUCCEEDED:
			// Get the nonce which failed.
			nonce = new Nonce( byteArray, 0 ) ;
			break ;

		// Ping/pongs to keep an active connection going with the mediator.
		case TYPE_PING:
		case TYPE_PONG:
			// No content to this message!
			break ;
			
		case TYPE_START_NEW_UDP_HOLE_PUNCH_ATTEMPT:
			// no content!
			break ;
			
		case TYPE_REQUEST_MEDIATOR_UPDATES:
		case TYPE_STOP_MEDIATOR_UPDATES:
		case TYPE_MEDIATOR_UPDATE:
			// We use 'nonce' and 'targetNonce' as the lobby/game nonce, and
			// our personal nonce, respectively.  Both are included in these
			// message, and there is no other content.
			intent = byteArray[0] ;
			nonce = new Nonce( byteArray, 1 ) ;
			targetNonce = new Nonce( byteArray, 1 + nonce.lengthAsBytes() ) ;
			break ;
			
		case TYPE_MEDIATOR_WILL_UPDATE:
		case TYPE_MEDIATOR_TOO_BUSY_TO_UPDATE:
			// Empty message.
			break ;
			
		default:
			return false ;
		}
		
		return true ;
	}
	
	
	
	public int getNumberOfPeople() {
		return numberOfPeople ;
	}
	
	public SocketAddress [] getAddresses() {
		if ( addresses == null )
			return null ;
		// Shallow copy should be okay, since I think SocketAddresse are immutable?
		// At least we treat them as such.
		SocketAddress [] res = new SocketAddress[ addresses.length] ;
		for ( int i = 0; i < addresses.length; i++ )
			res[i] = addresses[i] ;
		return res ;
	}
	
	public Nonce getTargetNonce() {
		return targetNonce ;
	}
	
	public int getTimeout() {
		return timeout ;
	}
	
	public byte getIntent() {
		return intent ;
	}
	
	
	public CoordinationMessage setAsIntentToServer( byte intent ) {
		nullOutsideReferences() ;
		
		type = TYPE_INTENT_TO_SERVER ;
		this.intent = intent ;
		
		return this ;
	}
	
	public CoordinationMessage setAsServerAccept() {
		nullOutsideReferences() ;
		
		type = TYPE_SERVER_ACCEPT ;
		
		return this ;
	}
	
	public CoordinationMessage setAsServerRejectFull() {
		nullOutsideReferences() ;
		
		type = TYPE_SERVER_REJECT_FULL ;
		
		return this ;
	}
	
	public CoordinationMessage setAsServerRejectInvalidNonce() {
		nullOutsideReferences() ;
		
		type = TYPE_SERVER_REJECT_INVALID_NONCE ;
		
		return this ;
	}
	
	public CoordinationMessage setAsBecomePassThrough() {
		nullOutsideReferences() ;
		
		type = TYPE_BECOME_PASS_THROUGH ;
		
		return this ;
	}
	
	public CoordinationMessage setAsNowPassThrough() {
		nullOutsideReferences() ;
		
		type = TYPE_NOW_PASS_THROUGH ;
		
		return this ;
	}
	
	public CoordinationMessage setAsUDPAddressesRequest() {
		nullOutsideReferences() ;
		
		type = TYPE_UDP_ADDRESSES_REQUEST ;
		
		return this ;
	}
	
	public CoordinationMessage setAsUDPAddresses( SocketAddress [] addr ) {
		nullOutsideReferences() ;
		
		type = TYPE_UDP_ADDRESSES ;
		if ( addresses == null || addresses.length != addr.length )
			addresses = new SocketAddress[addr.length] ;
		for ( int i = 0; i < addr.length; i++ )
			addresses[i] = addr[i] ;
		
		return this ;
	}
	
	public CoordinationMessage setAsUDPWhereAmIFailed() {
		nullOutsideReferences() ;
		
		type = TYPE_UDP_WHEREAMI_FAILED ;
		
		return this ;
	}
	
	public CoordinationMessage setAsAttemptUDPHolePunch( Nonce n, Nonce targetN, SocketAddress [] addr, int timeout ) {
		nullOutsideReferences() ;
		
		type = TYPE_ATTEMPT_UDP_HOLE_PUNCH ;
		nonce = n ;
		targetNonce = targetN ;
		if ( addresses == null || addresses.length != addr.length )
			addresses = new SocketAddress[addr.length] ;
		for ( int i = 0; i < addr.length; i++ )
			addresses[i] = addr[i] ;
		this.timeout = timeout ;
		
		return this ;
	}
	
	public CoordinationMessage setAsUDPHolePunchFailed( Nonce nonce ) {
		nullOutsideReferences() ;
		
		type = TYPE_UDP_HOLE_PUNCH_FAILED ;
		this.nonce = nonce ;
		
		return this ;
	}
	
	public CoordinationMessage setAsUDPHolePunchSucceeded( Nonce nonce ) {
		nullOutsideReferences() ;
		
		type = TYPE_UDP_HOLE_PUNCH_SUCCEEDED ;
		this.nonce = nonce ;
		
		return this ;
	}
	
	public CoordinationMessage setAsPing() {
		nullOutsideReferences() ;
		type = TYPE_PING ;
		return this ;
	}
	
	public CoordinationMessage setAsPong() {
		nullOutsideReferences() ;
		type = TYPE_PONG ;
		return this ;
	}
	
	public CoordinationMessage setAsStartNewUDPHolePunchAttempt() {
		nullOutsideReferences() ;
		type = TYPE_START_NEW_UDP_HOLE_PUNCH_ATTEMPT ;
		return this ;
	}
	
	
	public CoordinationMessage setAsRequestMediatorUpdates( byte intent, Nonce nonce, Nonce personalNonce ) {
		nullOutsideReferences() ;
		type = TYPE_REQUEST_MEDIATOR_UPDATES ;
		this.intent = intent ;
		this.nonce = nonce ;
		this.targetNonce = personalNonce ;
		return this ;
	}
	
	public CoordinationMessage setAsStopMediatorUpdates( byte intent, Nonce nonce, Nonce personalNonce ) {
		nullOutsideReferences() ;
		type = TYPE_STOP_MEDIATOR_UPDATES ;
		this.intent = intent ;
		this.nonce = nonce ;
		this.targetNonce = personalNonce ;
		return this ;
	}
	
	public CoordinationMessage setAsMediatorWillUpdate() {
		nullOutsideReferences() ;
		type = TYPE_MEDIATOR_WILL_UPDATE ;
		return this ;
	}
	
	public CoordinationMessage setAsMediatorTooBusyToUpdate() {
		nullOutsideReferences() ;
		type = TYPE_MEDIATOR_TOO_BUSY_TO_UPDATE ;
		return this ;
	}
	
	
	public CoordinationMessage setAsMediatorUpdate( byte intent, Nonce nonce, Nonce personalNonce ) {
		nullOutsideReferences() ;
		type = TYPE_MEDIATOR_UPDATE ;
		this.intent = intent ;
		this.nonce = nonce ;
		this.targetNonce = personalNonce ;
		return this ;
	}
	
	
	
	
	@Override
	public Message setAs( Message m ) {
		nullOutsideReferences() ;
		
		if ( !(m instanceof CoordinationMessage) )
			throw new IllegalArgumentException("Can't set a CoordinationMessage as a non-CoordinationMessage!") ;
		
		// Call to super, then set our own instance vars.
		super.setAs(m) ;
		
		CoordinationMessage myM = (CoordinationMessage)m ;
		
		this.numberOfPeople = myM.numberOfPeople ;
		if ( myM.addresses != null ) {
			if ( this.addresses == null || this.addresses.length != myM.addresses.length )
				this.addresses = new SocketAddress[myM.addresses.length] ;
			for ( int i = 0; i < myM.addresses.length; i++ )
				this.addresses[i] = myM.addresses[i] ;
		}
		
		this.targetNonce = myM.targetNonce ;
		this.timeout = myM.timeout ;
		this.intent = myM.intent ;
		
		return this ;
	}
}
