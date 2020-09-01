package com.peaceray.quantro.communications;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.communications.nonce.Nonce;

import android.telephony.SmsMessage;

/**
 * An SMS message is a very simple package for sending and receiving
 * updates via SMS.  SMS messages contain a Nonce, an action
 * (limited to at most 62 distinct actions, currently using 3), and
 * (optionally) message text.  We distinguish between a message with
 * empty text and a message with "no text" (i.e. null), in that any
 * text, including the empty string, will be separated from the data
 * payload by a newline character.
 * 
 * Messages are created from and transformed to Strings which may
 * be used as SmsMessage bodies.
 * 
 * @author Jake
 *
 */
public class QuantroSMSMessageBody implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5070882254778483792L;


	public class MessageTooLongException extends IllegalArgumentException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -7599133001585275061L;
		
		public MessageTooLongException( String msg ) {
			super(msg) ;
		}
		
	}
	
	// All should be >= 0.
	public static final int ACTION_CHALLENGE 		= 0 ;		// The receipient of this message is challenged!
	public static final int ACTION_ACCEPT 			= 1 ;			// Accept a challenge.
	public static final int ACTION_DECLINE 			= 2 ;		// Decline a challenge.

	private static final String CODE_CHALLENGE 		= "C" ;
	private static final String CODE_ACCEPT 		= "A" ;
	private static final String CODE_DECLINE 		= "D" ;
	
	
	private static final String DEFAULT_MESSAGE = "Let's play Quantro multiplayer." ;
	
	
	// What do we need for these message types?
	private static final boolean [] ACTION_REQUIRES_NONCE
			= new boolean[]{ true,		// ACTION_CHALLENGE
							 true,		// ACTION_ACCEPT
							 true		// ACTION_DECLINE
						  } ;
	private static final boolean [] ACTION_ALLOWS_NONCE
			= new boolean[]{ true,		// ACTION_CHALLENGE
					 		 true,		// ACTION_ACCEPT
					 		 true		// ACTION_DECLINE
				  		  } ;
	
	private static final String PAYLOAD_HEADER = "QNTR" ;
	private static final String PAYLOAD_TAIL = "" ;
	private static final String PAYLOAD_SEPARATOR = ":" ;		// MUST include character(s) not used in URL-safe base64.
	private static final String PAYLOAD_MESSAGE_TAIL = "\n" ;	// Included ONLY IF the message is non-null, between message and payload.
	private static final String MESSAGE_SUFFIX_URL = "http://market.android.com/details?id=com.peaceray.quantro" ;		// A suffix applied to the message.
	
	private static final int MAXIMUM_MESSAGE_CHARACTERS = 160 ;	// represents an IDEAL maximum, using 7-bit encoding.
	
	private CharSequence msg ;
	private int action ;
	private Nonce nonce ;
	
	private String stringRep ;
	
	
	
	/**
	 * Estimates the number of remaining characters available for the
	 * Message portion of this text message.  Uses the smallest available
	 * bit encoding given the current message contents.  Adding a single character
	 * to the message might change the bit encoding, thus dramatically
	 * lowering the value returned by this method.
	 * 
	 * If negative, the message is too long, the absolute value of the returned
	 * value is the approximate number of characters that should be removed to
	 * fit in a single message.
	 * 
	 * Remember, you can always construct a QuantroSMSMessageBody; the message will be truncated.
	 * 
	 * @param msg
	 * @param action
	 * @param nonceLength
	 * @return
	 */
	public static int charactersRemaining( CharSequence msg, int action, Nonce nonce ) {
		
		String content = payloadString(action, nonce) ;
		int [] noMesgLength = SmsMessage.calculateLength(content, false) ;
		if ( noMesgLength[0] != 1 )
			throw new RuntimeException("QuantroSMSMessageBody: message body too long even without message characters!") ;
		
		if ( msg == null )
			return noMesgLength[2] ;	// number of "code units" until the next message.  Ideally, we get 1 per character.
		
		String fullContent = msg + PAYLOAD_MESSAGE_TAIL + content ;
		int [] length = SmsMessage.calculateLength(fullContent, false) ;
		//System.err.println("length array: " + length[0] + " " + length[1] + " " + length[2] + " " + length[3] ) ;
		//System.err.println("payload string length: " + content.length()) ;
		//System.err.println("full content length: " + fullContent.length()) ;
		//System.err.println("full content: " + fullContent) ;
		if ( length[0] == 1 )
			return length[2] ;			// the number of "code units" until the next message.  Ideally 1 per character.
		
		// we try to fit the message in a single SMS, progressively truncating (and
		// counting the characters removed) the message.
		// First, assume the ideal case (7-bit encoding) and truncate to 
		// MAXIMUM_MESSAGE_CHARS.
		int truncateTo = Math.min(msg.length(), MAXIMUM_MESSAGE_CHARACTERS - content.length()) ;
		
		// progressively truncate based on SmsMessage.calculateLength.
		while(true) {
			fullContent = msg.subSequence(0, truncateTo) + PAYLOAD_MESSAGE_TAIL + content ;
			length = SmsMessage.calculateLength(fullContent, false) ;
			//System.err.println("length array: " + length[0] + " " + length[1] + " " + length[2] + " " + length[3] ) ;
			if ( length[0] == 1 )
				return truncateTo - msg.length() ;
			
			// otherwise, remove characters.  Estimate the number of characters by
			// which we "overshoot" and remove that many.
			int codeUnitsPerMessage = (length[1] + length[2] -1) / length[0] ;
			int codeUnitsPresent = length[1] ;
			int codeUnitsBeyondFirstMessage = codeUnitsPresent - codeUnitsPerMessage ;
			
			//System.err.println("code units per message: " + codeUnitsPerMessage) ;
			//System.err.println("code units present: " + codeUnitsPresent) ;
			//System.err.println("code units beyond first message: " + codeUnitsBeyondFirstMessage) ;
			
			// This is the number of "code units" which occur past the end of the first
			// message.  One code unit probably equals one character.
			truncateTo -= Math.max(1, codeUnitsBeyondFirstMessage) ;
		}
	}
	
	
	public static CharSequence truncateMessage( CharSequence msg, int action, Nonce nonce ) {
		int chars = charactersRemaining(msg, action, nonce) ;
		//System.err.println("length: " + msg.length()) ;
		//System.err.println("chars remaining: " + chars) ;
		if ( chars < 0 ) {
			//System.err.println("was length " + msg.length()) ;
			//System.err.println("truncating to length " + (msg.length() + chars - 1)) ;
			return msg.subSequence(0, msg.length() + chars  - 4) + "..." ;
		}
		return msg ;
	}
	
	
	/**
	 * A full constructor.  Constructs an object containing the provided message, action
	 * and nonce.  Will throw a MessageTooLongException if the provided message is too long.
	 * @param msg
	 * @param action
	 * @param nonce
	 * @throws MessageTooLongException: if the provided data creates too long of a message.
	 * @throws IllegalArgumentException: if the provided data fails for some other reason.
	 */
	public QuantroSMSMessageBody( CharSequence msg, int action, Nonce nonce ) {
		// Sanity check: is the message too long?
		// How long is all of our bookending stuff?  We include header and tail,
		// the action code (1 character) and 2 separators.
		if ( msg != null )
			msg = msg.toString().trim() ;
		msg = msg == null || msg.length() == 0 ? DEFAULT_MESSAGE : msg ;
		
		this.msg = msg ;
		this.action = action ;
		this.nonce = nonce ;
		
		this.stringRep = toString( this.msg, action, nonce ) ;
	}
	
	
	/**
	 * Attempts to construct and return a QuantroSMSMessageBody according to the
	 * provided string representation.
	 * 
	 * @param stringRep
	 * @return
	 */
	public static QuantroSMSMessageBody parse( String stringRep ) {
		
		System.err.println("QuantroSMSMessageBody parse") ;
		
		// First: locate the payload.
		int payloadIndex = stringRep.lastIndexOf(PAYLOAD_HEADER + PAYLOAD_SEPARATOR) ;
		if ( payloadIndex < 0 )
			return null ;
		
		System.err.println("QuantroSMSMessageBody has payloadIndex") ;
		
		// Second: attempt to extract the payload from this location.
		String payloadSubstring = stringRep.substring(payloadIndex) ;
		String [] payloadElements = payloadSubstring.split(PAYLOAD_SEPARATOR, -1) ;
		System.err.println("QuantroSMSMessageBody has payload elements") ;
		// First element is HEADER; we have already verified this.
		// Second element is ACTION CODE.
		String actionStr = payloadElements[1] ;
		// Third element is NONCE.  (may be an empty string, indicating 'null')
		String nonceStr = payloadElements[2] ;
		
		// Process them.
		int action ;
		Nonce nonce = null ;
		try {
			action = actionStringToInt( actionStr ) ;
			if ( nonceStr.length() > 0 )
				nonce = new Nonce( nonceStr ) ;
		} catch( Exception e ) {
			// Invalid stuff!
			return null ;
		}
		System.err.println("QuantroSMSMessageBody has action string") ;
		
		// Get the message, if present.
		String msg = null ;
		if ( payloadIndex > 0 ) {
			// get substring up-to but not including the message tail.
			try {
				msg = stringRep.substring(0, payloadIndex - PAYLOAD_MESSAGE_TAIL.length()) ;
			} catch( Exception e ) {
				// Something failed.
				return null ;
			}
		}
		System.err.println("QuantroSMSMessageBody has msg") ;
		
		if ( msg != null ) {
			// Try to remove the URL.
			int urlIndex = msg.lastIndexOf(MESSAGE_SUFFIX_URL) ;
			if ( urlIndex > 0 ) {
				msg = msg.substring(0, urlIndex - PAYLOAD_MESSAGE_TAIL.length()) ;
			}
		}
		
		System.err.println("QuantroSMSMessageBody removed URL") ;
		
		// Everything is set.  Make and return a new object.
		try {
			System.err.println("QuantroSMSMessageBody making body with " + msg + " " + action + " " + nonce) ;
			QuantroSMSMessageBody mbody = new QuantroSMSMessageBody( msg, action, nonce ) ;
			return mbody ;
		} catch( MessageTooLongException mtle ) {
			throw new IllegalArgumentException("Provided string representation is faulty: " + mtle.getMessage()) ;
		}
 	}
	
	
	/**
	 * A more liberal version of "parse."  The purpose of this method is to parse SMS
	 * messages that the user has designated as challenges, e.g. by pasting the message
	 * text specifically for the purpose of introducing a challenge.
	 * 
	 * In other words, 'parse' carries the responsibilities of both 1. identifying
	 * whether the message contains a challenge and 2. parsing that challenge.
	 * 
	 * Because of this dual responsibility, 'parse' will reject messages that
	 * could potentially contain valid challenge Nonces if it is not certain they
	 * represent a challenge.  'forceParse' only does #2, and as such is more
	 * lenient about whether a particular message represents a challenge
	 * or information regarding one.
	 * 
	 * @param stringRep
	 * @return
	 */
	public static QuantroSMSMessageBody forceParse( String stringRep ) {
		QuantroSMSMessageBody qbody ;
		System.err.println("QuantroSMSMessageBody forceParse") ;
		// First: attempt a normal parse.  A nice user has pasted the entire
		// message successfully.
		try {
			System.err.println("QuantroSMSMessageBody parse full") ;
			qbody = parse(stringRep) ;
			System.err.println("QuantroSMSMessageBody parsed") ;
			if ( qbody != null )
				return qbody ;
		} catch( IllegalArgumentException iae ) {
			System.err.println("QuantroSMSMessageBody failed") ;
			// don't care, we kinda expected that.
		}
		
		// Second: strip whitespace from the string and try again.
		stringRep = stringRep.trim() ;
		try {
			System.err.println("QuantroSMSMessageBody parse trimmed") ;
			qbody = parse(stringRep) ;
			System.err.println("QuantroSMSMessageBody parsed") ;
			if ( qbody != null )
				return qbody ;
		} catch( IllegalArgumentException iae ) {
			System.err.println("QuantroSMSMessageBody failed") ;
			// don't care, we kinda expected that.
		}
		
		// Third: normal parse failed.  Try a slightly more lenient approach.
		// We don't bother trying to get a message.
		try {
			System.err.println("QuantroSMSMessageBody parse without message") ;
			qbody = parseWithoutMessage(stringRep) ;
			System.err.println("QuantroSMSMessageBody parsed") ;
			if ( qbody != null )
				return qbody ;
		} catch( IllegalArgumentException iae ) {
			System.err.println("QuantroSMSMessageBody failed") ;
			// that's a little frustrating.
		}
		
		// Fourth: we're not done yet.  The message apparently doesn't
		// contain the expected payload format, but we may still be able
		// to get an action code and challenge nonce out of it.
		System.err.println("QuantroSMSMessageBody final attempt...") ;
		String [] whiteSeparated = stringRep.split("\\s+", -1) ;
		for ( int i = 0; i < whiteSeparated.length; i++ ) {
			System.err.println("QuantroSMSMessageBody element " + i + " of " + whiteSeparated.length + " is "  + whiteSeparated[i]) ;
			if ( whiteSeparated != null ) {
				String [] payloadElements = whiteSeparated[i].split(PAYLOAD_SEPARATOR, -1) ;
				// Try every consecutive combination
				for ( int j = payloadElements.length - 2; j >= 0; j-- ) {
					String actionStr = payloadElements[j] ;
					String nonceStr = payloadElements[j+1] ;
					
					int action ;
					Nonce nonce = null ;
					try {
						action = actionStringToInt( actionStr ) ;
						if ( nonceStr.length() > 0 )
							nonce = new Nonce( nonceStr ) ;
					} catch( Exception e ) {
						// Invalid stuff!
						continue ;
					}
					
					try { 
						System.err.println("QuantroSMSMessageBody Constructing message body") ;
						QuantroSMSMessageBody mbody = new QuantroSMSMessageBody( null, action, nonce ) ;
						System.err.println("QuantroSMSMessageBody success") ;
						return mbody ;
					} catch( MessageTooLongException mtle ) {
						System.err.println("QuantroSMSMessageBody failed") ;
						continue ;
					}
				}
			}
		}
		
		// siiiigh.  Well... maybe it's only and exactly a session code?
		if ( whiteSeparated.length == 1 ) {
			try {
				return new QuantroSMSMessageBody(
						null,
						QuantroSMSMessageBody.ACTION_CHALLENGE,
						new Nonce( Nonce.fromSMSSafe( whiteSeparated[0] ) ) ) ;
			} catch (IOException e) {
				// nothing; fall through.
			}
		}
		
		return null ;
 	}
	
	
	private static QuantroSMSMessageBody parseWithoutMessage( String stringRep ) {
		// First: locate the payload.
		int payloadIndex = stringRep.lastIndexOf(PAYLOAD_HEADER + PAYLOAD_SEPARATOR) ;
		if ( payloadIndex < 0 )
			return null ;
		
		// Second: attempt to extract the payload from this location.
		String payloadSubstring = stringRep.substring(payloadIndex) ;
		String [] payloadElements = payloadSubstring.split(PAYLOAD_SEPARATOR, -1) ;
		// First element is HEADER; we have already verified this.
		// Second element is ACTION CODE.
		String actionStr = payloadElements[1] ;
		// Third element is NONCE.  (may be an empty string, indicating 'null')
		String nonceStr = payloadElements[2] ;
		
		// Process them.
		int action ;
		Nonce nonce = null ;
		try {
			action = actionStringToInt( actionStr ) ;
			if ( nonceStr.length() > 0 )
				nonce = new Nonce( nonceStr ) ;
		} catch( Exception e ) {
			// Invalid stuff!
			return null ;
		}
		
		// Everything is set.  Make and return a new object.
		try {
			QuantroSMSMessageBody mbody = new QuantroSMSMessageBody( null, action, nonce ) ;
			return mbody ;
		} catch( MessageTooLongException mtle ) {
			return null ;
		}
	}
	
	
	public CharSequence getMessage() {
		return this.msg ;
	}
	
	public int getAction() {
		return this.action ;
	}
	
	public Nonce getNonce() {
		return this.nonce ;
	}
	
	
	public String toString() {
		if ( stringRep != null )
			return stringRep ;
		throw new IllegalStateException("Provided message, action and nonce do not form a valid message.") ;
	}
	
	// Other action accessors
	public boolean actionIsChallenge() {
		return action == QuantroSMSMessageBody.ACTION_CHALLENGE ;
	}
	
	public boolean actionIsAccept() {
		return action == QuantroSMSMessageBody.ACTION_ACCEPT ;
	}
	
	public boolean actionIsDecline() {
		return action == QuantroSMSMessageBody.ACTION_DECLINE ;
	}
	
	
	
	private static String payloadString( int action, Nonce nonce ) {
		boolean actionOK = true ;
		try {
			if ( ACTION_REQUIRES_NONCE[action] && nonce == null )
				actionOK = false ;
			if ( !ACTION_ALLOWS_NONCE[action] && nonce != null )
				actionOK = false ;
		} catch( Exception e ) {
			actionOK = false ;
		}
		
		if ( !actionOK )
			throw new IllegalArgumentException("Action " + action + " either not supported, or requires null/non-null nonce value.") ;
		
		String actionCode = actionIntToString( action ) ;
		String content = PAYLOAD_HEADER + PAYLOAD_SEPARATOR + actionCode + PAYLOAD_SEPARATOR ;
		if ( nonce == null )
			content = content + PAYLOAD_TAIL ;
		else
			content = content + Nonce.toSMSSafe(nonce.toString()) + PAYLOAD_TAIL ;
		content = MESSAGE_SUFFIX_URL + PAYLOAD_MESSAGE_TAIL+ content ;
		
		return content ;
	}
	
	
	/**
	 * Will either construct and return a string representation, or throw 
	 * a MessageTooLongException.
	 * @param msg
	 * @param action
	 * @param nonce
	 * @return
	 */
	private String toString( CharSequence msg, int action, Nonce nonce ) throws MessageTooLongException {
		System.err.println("QuantroSMSMessageBody toString") ;
		
		String content = payloadString( action, nonce ) ;
		System.err.println("QuantroSMSMessageBody payloadString is " + content) ;
		if ( msg != null )
			content = QuantroSMSMessageBody.truncateMessage(msg, action, nonce) + PAYLOAD_MESSAGE_TAIL + content ;
		
		System.err.println("QuantroSMSMessageBody truncateMessage finished") ;
		
		// Check the length.
		System.err.println("QuantroSMSMessageBody calculating length...") ;
		int [] length = SmsMessage.calculateLength(content, false) ;
		System.err.println("QuantroSMSMessageBody have length") ;
		// length[0] is the number of SMS messages required.  If not 1, we 
		// have a problem.
		if ( length[0] != 1 )
			throw new MessageTooLongException("Message content " + content + " length is " + content.length() + "; requires " + length[0] + " SmsMessages.") ;
		
		return content ;
	}
	
	
	/**
	 * Converts an int in QuantroSMSMessageBody.ACTION_* to a string in
	 * QuantroSMSMessageBody.CODE_*.
	 * @param action
	 * @return
	 */
	private static String actionIntToString( int action ) {
		switch( action ) {
		case QuantroSMSMessageBody.ACTION_CHALLENGE:
			return QuantroSMSMessageBody.CODE_CHALLENGE ;
			
		case QuantroSMSMessageBody.ACTION_ACCEPT:
			return QuantroSMSMessageBody.CODE_ACCEPT ;
			
		case QuantroSMSMessageBody.ACTION_DECLINE:
			return QuantroSMSMessageBody.CODE_DECLINE ;
			
		default:
			throw new IllegalArgumentException( Integer.toString(action) + " is not a valid action code" ) ;
		}
	}
	
	
	private static int actionStringToInt( String actionStr ) {
		if ( QuantroSMSMessageBody.CODE_CHALLENGE.equals(actionStr) )
			return QuantroSMSMessageBody.ACTION_CHALLENGE ;
		
		if ( QuantroSMSMessageBody.CODE_ACCEPT.equals(actionStr) )
			return QuantroSMSMessageBody.ACTION_ACCEPT ;
		
		if ( QuantroSMSMessageBody.CODE_DECLINE.equals(actionStr) )
			return QuantroSMSMessageBody.ACTION_DECLINE ;
		
		throw new IllegalArgumentException( actionStr + " is not a valid action code string" ) ;
	}
	
	
	/////////////////////////////////////////////
	// serializable methods
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// Write as byte representation
		stream.writeObject(msg) ;
		stream.writeInt(action) ;
		stream.writeObject(nonce) ;
		stream.writeObject(stringRep) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Read as byte representation
		msg = (String)stream.readObject();
		action = stream.readInt();
		nonce = (Nonce)stream.readObject();
		stringRep = (String)stream.readObject() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
}
