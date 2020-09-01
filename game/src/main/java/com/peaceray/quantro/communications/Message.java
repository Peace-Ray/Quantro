package com.peaceray.quantro.communications;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.utils.ByteArrayOps;


/**
 * The abstract "Message" class goes well with MessageReader, a thread-based
 * class for non-blocking reads of messages from InputStreams (usually Sockets).
 * 
 * Individual subclasses of Message can define their own methods for setting / getting
 * content.  We don't expect that a given usage (e.g., "game messages" or "lobby messages"
 * will ever overlap, meaning that for a particular InputStream, we ALWAYS
 * know what subclass of Message will be coming in.
 * 
 * @author Jake
 *
 */
public abstract class Message {
	
	
	private static final int BASE_BUFFER_SIZE = 1024 ;
	
	/**
	 * Override this method if your Message needs a larger buffer than the default.
	 * The result of this method call can differ based on the message content; over
	 * time, the buffer length will approach the maximum required size.
	 * @return
	 */
	protected int BUFFER_SIZE() {
		return 0 ;
	}
	

	// Message type.
	public static final byte TYPE_UNKNOWN					= Byte.MIN_VALUE	+  0 ;		// -128
	public static final byte TYPE_EXIT						= Byte.MIN_VALUE	+  1 ;
	public static final byte TYPE_KICK						= Byte.MIN_VALUE    +  2 ;	
	public static final byte TYPE_NONCE_REQUEST 			= Byte.MIN_VALUE	+  3 ;		// -125
	public static final byte TYPE_NONCE						= Byte.MIN_VALUE	+  4 ;	
	public static final byte TYPE_PERSONAL_NONCE_REQUEST	= Byte.MIN_VALUE 	+  5 ;
	public static final byte TYPE_PERSONAL_NONCE			= Byte.MIN_VALUE	+  6 ;		// -122
	public static final byte TYPE_NAME_REQUEST	 			= Byte.MIN_VALUE 	+  7 ;
	public static final byte TYPE_MY_NAME					= Byte.MIN_VALUE	+  8 ;		// -120
	public static final byte TYPE_PLAYER_NAME				= Byte.MIN_VALUE	+  9 ;		//
	public static final byte TYPE_TOTAL_PLAYER_SLOTS		= Byte.MIN_VALUE 	+ 10 ;		//
	public static final byte TYPE_PERSONAL_PLAYER_SLOT		= Byte.MIN_VALUE 	+ 11 ;		//
	
	public static final byte TYPE_WELCOME_TO_SERVER 		= Byte.MIN_VALUE 	+ 12 ;		//
	
	public static final byte TYPE_SERVER_CLOSING			= Byte.MIN_VALUE	+ 13 ;		//
	public static final byte TYPE_SERVER_CLOSING_FOREVER	= Byte.MIN_VALUE	+ 14 ;		//
	
	public static final byte TYPE_PLAYER_QUIT				= Byte.MIN_VALUE	+ 15 ;		//
	
	public static final byte TYPE_HOST						= Byte.MIN_VALUE	+ 16 ;		//
	
	public static final byte TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION
															= Byte.MIN_VALUE 	+ 17 ;		//
	public static final byte TYPE_INTERROGATION_COMPLETE	= Byte.MIN_VALUE 	+ 18 ;		// -110

	public static final byte TYPE_HOST_PRIORITY				= Byte.MIN_VALUE 	+ 19 ;
	public static final byte TYPE_I_AM_HOST			 		= Byte.MIN_VALUE 	+ 20 ;		// -108
	public static final byte TYPE_I_AM_CLIENT			 	= Byte.MIN_VALUE 	+ 21 ;
	public static final byte TYPE_YOU_ARE_CLIENT			= Byte.MIN_VALUE	+ 22 ;
	
	public static final byte TYPE_PING						= Byte.MIN_VALUE 	+ 23 ;
	public static final byte TYPE_PONG 						= Byte.MIN_VALUE	+ 24 ;
	
	public static final byte TYPE_KEEP_ALIVE				= Byte.MIN_VALUE	+ 25 ;
	
	public static final byte TYPE_KICK_WARNING				= Byte.MIN_VALUE 	+ 26 ;
	public static final byte TYPE_KICK_WARNING_RETRACTION	= Byte.MIN_VALUE	+ 27 ;
	
	protected static final byte MIN_TYPE_IN_SUBCLASS	= 0 ;		// Subclasses must begin their
																	// message counts from this number.
	
	
	
	
	
	// Byte array / byte buffer!
	protected byte [] byteArray = null ;
	protected byte [] eightByteArray = null ;
	protected byte [] writeTypeAndLengthArray = null ;
	// byte buffer
	protected ByteBuffer byteBuffer = null ;
	
	// Current message configuration.
	protected byte type ;
	
	
	// Some necessary information.
	protected long num ;
	protected int playerSlot ;
	protected Nonce nonce ;
	protected String string ;
	protected boolean significant ;
	
	
	// For reading messages!
	// Values for reading a message.
	protected boolean typeRead ;
	protected boolean lengthRead ;
	protected int length ;
	protected int contentBytesRead ;
	protected boolean readComplete ;
	
	
	public Message() {
		byteArray = null ;
		eightByteArray = null ;
		writeTypeAndLengthArray = null ;
		
		type = TYPE_UNKNOWN ;
		
		string = null ;
	}
	
	
	protected void makeByteArrayIfNeeded() {
		int len = Math.max( BUFFER_SIZE(), BASE_BUFFER_SIZE ) ;
		if ( byteArray == null || byteArray.length < len ) {
			byteArray = new byte[len] ;
		}
		
		makeEightByteArrayIfNeeded() ;
	}
	
	
	protected void makeEightByteArrayIfNeeded() {
		if ( eightByteArray == null ) {
			eightByteArray = new byte[8] ;
			writeTypeAndLengthArray = new byte[8] ;
		}
	}
	
	
	protected void makeByteBufferIfNeeded() {
		if ( byteBuffer == null )
			byteBuffer = ByteBuffer.allocateDirect(Math.max( BUFFER_SIZE(), BASE_BUFFER_SIZE )) ;
	}
	
	
	///////////////////////////////////////////////////////////////////
	//
	// METHODS TO IMPLEMENT / OVERRIDE
	// 
	// Subclasses of Message MUST implement and/or override these!
	//
	// For those that are overridden, it is very good practice to
	// call super.methodName() as well.  Certain of these methods should
	// call 'super' for any Type less than MIN_TYPE_IN_SUBCLASS.
	//
	///////////////////////////////////////////////////////////////////
	
	
	/**
	 * Helper function: any fields of the Message that might
	 * hold a reference to some object from outside the Message
	 * should be set to 'null'.  This method also resets 'name'.
	 */
	protected void nullOutsideReferences() {
		string = null ;
		nonce = null ;
	}
	
	
	/**
	 * All messages are written with a prefix (type and length),
	 * currently represented as 5 bytes.  Type is a byte, length
	 * a 4-byte encoding of an int.  For special message types,
	 * we need to know the length so it can be written.  This method
	 * will be called for Messages with type >= MIN_TYE_IN_SUBCLASS;
	 * it should return the exact number of bytes needed to represent
	 * the message in a byte array.
	 * 
	 * @return
	 */
	protected abstract int messageContentLength() ;
	
	
	/**
	 * Assume that any necessary prefix information has already been
	 * written to the OutputStream.  Write all message content.
	 * 
	 * This method may safely use byteBuffer.
	 * 
	 * @param outputDest: an instance of OutputStream or WritableByteChannel.
	 * @throws IOException
	 */
	protected abstract void writeMessageContent( Object outputDest ) throws IOException ;
	

	/**
	 * Read message content from 'is'.  This method will be called
	 * by 'read' after the message type and length have been read;
	 * therefore, we can assume 'typeRead' and 'lengthRead' are
	 * both true.  Additionally, 'length' is the value read as
	 * the message length, and contentBytesRead is zero (for the first
	 * call on this message), or the value set during the last call.
	 * 
	 * This method will be called if the message type being read is
	 * equal to, or above, MIN_TYPE_IN_SUBCLASS.
	 * 
	 * Note that 'is' may not provide all of the message content
	 * in a single read; therefore, this method should return 'true'
	 * if the method content has been completely read and processed.
	 * 
	 * PRECONDITION: 'is' is a stream from which 'type' and 'length'
	 * have been read, plus an additional 'contentBytesRead'.
	 * You may use 'byteBuffer' to store persistent content; it will not be altered
	 * by Message until after this method returns 'true'.
	 * 
	 * POSTCONDITION: A total of 'contentBytesRead' have now been read
	 * from the InputStream.  If 'false' is returned, the stream is still
	 * good and there are additional bytes to read.  If 'true' is returned,
	 * then the entire message content (and NO MORE) has been read
	 * from the stream, and this Message object configured appropriately
	 * (as the method will only be called for subclass message types,
	 * we can't actually process the content ourselves).  You may use
	 * Message fields such as playerSlot or 'string', if appropriate.
	 * Throws an exception if the stream failed for any reason, or seemed
	 * to hold an incomplete message (i.e. a read returned -1).
	 * 
	 * @param inputSource The InputStream or ReadableByteChannel from which to read.
	 * @return Whether ALL message content has been read.
	 */
	protected abstract boolean readMessageContent( Object inputSource ) throws IOException, ClassNotFoundException ;
	
	
	
	
	///////////////////////////////////////////////////////////////////
	//
	// HELPER METHODS SUBCLASSES MAY USE
	// 
	// Subclasses may use these methods, if necessary.  Pay special
	// attention to any Pre/Post condition notes, since calling these
	// methods indiscriminantly can mess up normal Message operation.
	//
	///////////////////////////////////////////////////////////////////
	
	
	
	
	/**
	 * A useful helper function for reading some bytes into 'byteBuffer'.
	 * This method uses 'length', and updates 'contentBytesRead' and 'readComplete'.
	 * Data will be written to byteBuffer[contentBytesRead : contentBytesRead + x],
	 * and then contentBytesRead += x.
	 * 
	 * PRECONDITION: called from within 'readMessageContent' to read bytes
	 * from 'is' into 'byteBuffer'.
	 * 
	 * POSTCONDITION: byteBuffer, contentBytesRead, and readComplete have been
	 * updated.
	 * 
	 * @param is
	 * @return Whether we have finished reading the message.
	 * @throws IOException
	 */
	protected final boolean readBytesIntoByteArray( Object inputSource ) throws IOException {
		makeByteArrayIfNeeded() ;
		// NON-BLOCKING.  Reads once; if full length, continue on and process.  If not,
		// return false.  (note that the call to is.read is technically blocking, but
		// that it might not read the full length of the message; that is the case where
		// we return instead of looping.
		if ( contentBytesRead < length ) {
			int bytesRead = readBytesIntoByteArray(inputSource, byteArray, contentBytesRead, length-contentBytesRead) ;
			//System.out.println("read from " + contentBytesRead + " to " + (contentBytesRead + bytesRead)) ;
			if ( bytesRead == -1 )
				throw new IOException("read failed: end of stream") ;
			
			contentBytesRead += bytesRead ;
			
			if ( contentBytesRead == length )
				readComplete = true ;
			else 
				return false ;
		}
		
		return true ;
	}
	
	
	protected final boolean readAllBytesIntoByteArray( Object inputSource, byte [] barray, int index, int length ) throws IOException {
		if ( inputSource instanceof InputStream )
			return readAllBytesIntoByteArray( (InputStream)inputSource, barray, index, length ) ;
		if ( inputSource instanceof ReadableByteChannel )
			return readAllBytesIntoByteArray( (ReadableByteChannel)inputSource, barray, index, length ) ;
		if ( inputSource instanceof ByteBuffer )
			return readAllBytesIntoByteArray( (ByteBuffer)inputSource, barray, index, length ) ;
		throw new IllegalArgumentException("Provided input source must be an InputStream, a ByteBuffer, or a ReadableByteChannel") ;
	}
	
	
	protected final int readBytesIntoByteArray( Object inputSource, byte [] barray, int index, int length ) throws IOException {
		if ( inputSource instanceof InputStream )
			return readBytesIntoByteArray( (InputStream)inputSource, barray, index, length ) ;
		if ( inputSource instanceof ReadableByteChannel )
			return readBytesIntoByteArray( (ReadableByteChannel)inputSource, barray, index, length ) ;
		if ( inputSource instanceof ByteBuffer )
			return readBytesIntoByteArray( (ByteBuffer)inputSource, barray, index, length ) ;
		throw new IllegalArgumentException("Provided input source must be an InputStream, a ByteBuffer, or a ReadableByteChannel") ;
	}
	
	
	protected final boolean readAllBytesIntoByteArray( InputStream is, byte [] barray, int index, int length ) throws IOException {
		int totalRead = 0 ;
		while ( totalRead < length ) {
			int bytesRead = is.read(barray, index+totalRead, length-totalRead) ;
			
			if ( bytesRead == 0 )
				return false ;
			if ( bytesRead == -1 )
				throw new IOException("Error reading from stream") ;
			
			totalRead += bytesRead ;
		}
		
		return true ;
	}
	
	/**
	 * Performs a read from the InputStream into the provided byte array, beginning at
	 * 'index' and reading at most 'length' bytes.  Returns the number of bytes actually
	 * read.
	 * 
	 * @param is
	 * @param barray
	 * @param index
	 * @param length
	 * @return
	 * @throws IOException 
	 */
	protected final int readBytesIntoByteArray( InputStream is, byte [] barray, int index, int length ) throws IOException {
		int bytesRead = is.read(barray, index, length) ;
		return bytesRead ;
	}
	
	
	protected final boolean readAllBytesIntoByteArray( ReadableByteChannel channel, byte [] barray, int index, int length ) throws IOException {
		int totalRead = 0 ;
		while ( totalRead < length ) {
			int bytesRead = readBytesIntoByteArray( channel, barray, index+totalRead, length-totalRead) ;
			
			if ( bytesRead == 0 )
				return false ;
			
			totalRead += bytesRead ;
		}
		
		return true ;
	}
	
	/**
	 * Attempts to read bytes from the provided channel into the provided byte array.
	 * Returns the number of bytes read, which may be greater than BUFFER_SIZE
	 * but will not exceed 'length.'
	 * 
	 * POSTCONDITION: This method will alter the contents of 'byteBuffer.'
	 * 
	 * @param channel
	 * @param barray
	 * @param index
	 * @param length
	 * @return
	 * @throws IOException 
	 */
	protected final int readBytesIntoByteArray( ReadableByteChannel channel, byte [] barray, int index, int length ) throws IOException {
		// attempts to read at least 1 byte into barray, reading at most 'length'.
		makeByteBufferIfNeeded() ;
		
		int totalRead = 0 ;
		
		while ( totalRead < length ) {
			// configure byteBuffer to read at most 'length.
			byteBuffer.position(0) ;
			byteBuffer.mark() ;
			byteBuffer.limit(Math.min(byteBuffer.capacity(), length - totalRead)) ;
			
			int bytesRead = channel.read(byteBuffer) ;
			
			if ( bytesRead == 0 )
				return totalRead ;
			if ( bytesRead < 0 )
				throw new IOException("Channel closed or cannot read; returned -1") ;
			
			// copy into barray.
			for ( int i = 0; i < bytesRead; i++ )
				barray[index+i+totalRead] = byteBuffer.get(i) ;
			
			totalRead += bytesRead ;
		}
		
		return totalRead ;
	}
	
	protected final boolean readAllBytesIntoByteArray( ByteBuffer bb, byte [] barray, int index, int length ) {
		return readBytesIntoByteArray( bb, barray, index, length ) == length ;
	}
	
	protected final int readBytesIntoByteArray( ByteBuffer bb, byte [] barray, int index, int length ) {
		//System.err.println("Message readBytesIntoByteArray: " + barray.length + ", " + index + ", " + length) ;
		bb.get(barray, index, length) ;
		return length ;
	}
	
	
	
	protected final void writeBytesInByteArray( Object outputDest, byte [] barray, int index, int length ) throws IOException {
		if ( outputDest instanceof OutputStream )
			writeBytesInByteArray( (OutputStream)outputDest, barray, index, length ) ;
		else if ( outputDest instanceof WritableByteChannel )
			writeBytesInByteArray( (WritableByteChannel)outputDest, barray, index, length ) ;
		else if ( outputDest instanceof ByteBuffer )
			writeBytesInByteArray( (ByteBuffer)outputDest, barray, index, length ) ;
		else
			throw new IllegalArgumentException("Provided output dest must be an OutputStream, a ByteBuffer, or a WriteableByteChannel") ;
	}
	
	
	/**
	 * Writes the specified bytes to the provided output stream.
	 * @param os
	 * @param barray
	 * @param index
	 * @param length
	 * @return
	 * @throws IOException
	 */
	protected final void writeBytesInByteArray( OutputStream os, byte [] barray, int index, int length ) throws IOException {
		os.write(barray, index, length) ;
		os.flush() ;
	}
	
	
	/**
	 * Attempts to write bytes to the provided channel from the provided byte array.
	 * Returns the total number of bytes successfully written, which may be larger
	 * than BUFFER_SIZE.
	 * 
	 * POSTCONDITION: This method will alter the contents of 'byteBuffer.'
	 * 
	 * @param channel
	 * @param barray
	 * @param index
	 * @param length
	 * @return
	 * @throws IOException 
	 */
	protected final void writeBytesInByteArray( WritableByteChannel channel, byte [] barray, int index, int length ) throws IOException {
		// attempts to read at least 1 byte into barray, reading at most 'length'.
		makeByteBufferIfNeeded() ;
		
		int totalWritten = 0 ;
		
		while ( totalWritten < length ) {
			// configure byte buffer to hold the minumum of its size and length-totalWritten
			// bytes.
			byteBuffer.position(0) ;
			byteBuffer.mark() ;
			byteBuffer.limit(byteBuffer.capacity()) ;
			
			int num = Math.min(byteBuffer.capacity(), length - totalWritten) ;
			byteBuffer.put(barray, index + totalWritten, num) ;
			byteBuffer.flip() ;

			int bytesWritten = channel.write(byteBuffer) ;
			
			if ( bytesWritten == 0 ) {
				System.err.println("Error: writing to channel, 0 bytes written.") ;
				try {
					Thread.sleep(10) ;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if ( bytesWritten < 0 )
				throw new IOException("Output channel is broken or cannot write") ;
			
			totalWritten += bytesWritten ;
		}
	}
	
	
	protected final void writeBytesInByteArray( ByteBuffer bb, byte [] barray, int index, int length ) {
		bb.put(barray, index, length) ;
	}
	
	
	///////////////////////////////////////////////////////////////////
	//
	// FINAL METHODS FOR ALL MESSAGE TYPES
	// 
	// These methods all reading and writing of ALL message types,
	// including those of subclasses.  For subclasses, specialized
	// methods (prototyped above as 'abstract') will be called.
	//
	///////////////////////////////////////////////////////////////////
	
	
	
	private final int contentLength() {
		if ( type >= MIN_TYPE_IN_SUBCLASS )
			return messageContentLength() ;
		
		byte [] nameAsBytes ;
		
		// Otherwise...
		switch( type ) {
		// These messages have 0 length (no content); only 
		// the type needs to be written.
		case TYPE_UNKNOWN:
		case TYPE_NONCE_REQUEST:
		case TYPE_PERSONAL_NONCE_REQUEST:
		case TYPE_NAME_REQUEST:
		case TYPE_EXIT:
		case TYPE_SERVER_CLOSING:
		case TYPE_SERVER_CLOSING_FOREVER:
		case TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION:
		case TYPE_WELCOME_TO_SERVER:
			return 0 ;
		
		// Global nonce
		case TYPE_NONCE:
			return nonce.lengthAsBytes() ;		// length is determined by the object
			
		// Personal nonce
		case TYPE_PERSONAL_NONCE:
			return 4 + nonce.lengthAsBytes() ;		// an int (4) and a Nonce
			
		// Writes a string.
		case TYPE_MY_NAME:
			nameAsBytes = string.getBytes() ;
			return nameAsBytes.length ;
			
		// Writes a player slot (int) and a string, byte-array format.  Note that this allocates memory (probably),
		// but we expect these calls to be rare, and probably never
		// during gameplay.
		case TYPE_PLAYER_NAME:
		case TYPE_KICK:
		case TYPE_HOST:
			nameAsBytes = string.getBytes() ;
			return nameAsBytes.length + 4 ;
		
		// An integer (not a nonce).  This int conveys a particular
		// player slot, or the number thereof.
		case TYPE_TOTAL_PLAYER_SLOTS:
		case TYPE_PERSONAL_PLAYER_SLOT:
		case TYPE_PLAYER_QUIT:
			return 5 ;		// just an int and a boolean
			
		// No content
		case TYPE_INTERROGATION_COMPLETE:
			return 0 ;
			
		case TYPE_HOST_PRIORITY:
			return 4 ;		// just an int
			
		case TYPE_I_AM_HOST:
		case TYPE_I_AM_CLIENT:
		case TYPE_YOU_ARE_CLIENT:
			return 0 ;		// no content
			
		case TYPE_PING:
		case TYPE_PONG:
			return 8 ;		// has a message number
			
		case TYPE_KEEP_ALIVE:
			return 0 ;		// no content
		
		// player slot, duration (long), message string.
		case TYPE_KICK_WARNING:
			nameAsBytes = string.getBytes() ;
			return nameAsBytes.length + 12 ;
			
		case TYPE_KICK_WARNING_RETRACTION:
			return 4 ;		// player slot
		
		// WHAT!??
		default:
			return 0 ;		// don't know what this is!
		}
	}
	
	
	private final void writeContent( Object outputObject ) throws IOException {
		if ( type >= MIN_TYPE_IN_SUBCLASS ) {
			writeMessageContent(outputObject) ;
			return ;
		}
		
		byte [] nameAsBytes ;
		int len ;
		
		makeByteArrayIfNeeded() ;
		
		// Otherwise, do it ourselves!
		switch( type ) {
		// These messages have 0 length (no content); only 
		// the type needs to be written.
		case TYPE_UNKNOWN:
		case TYPE_NONCE_REQUEST:
		case TYPE_PERSONAL_NONCE_REQUEST:
		case TYPE_NAME_REQUEST:
		case TYPE_EXIT:
		case TYPE_SERVER_CLOSING:
		case TYPE_SERVER_CLOSING_FOREVER:
		case TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION:
		case TYPE_WELCOME_TO_SERVER:
			// Write nothing.  There is no content.
			return ;
		
		// Nonce values, personal or global.  It's
		// basically the same thing.
		case TYPE_NONCE:
			len = nonce.writeAsBytes(byteArray, 0) ;
			this.writeBytesInByteArray(outputObject, byteArray, 0, len) ;
			//System.out.println("Message: writing nonce as " + nonce) ;
			return ;
		
		// Personal nonce - player slot and nonce value.
		case TYPE_PERSONAL_NONCE:
			ByteArrayOps.writeIntAsBytes(playerSlot, byteArray, 0) ;
			len = 4 + nonce.writeAsBytes(byteArray, 4) ;
			this.writeBytesInByteArray(outputObject, byteArray, 0, len) ;
			
			//System.out.println("Message: writing personal nonce:" + nonce + " as bytes " + ByteArrayOps.readLongAsBytes(byteBuffer, 4)) ;
			return ;
			
		case TYPE_MY_NAME:
			nameAsBytes = string.getBytes() ;
			this.writeBytesInByteArray(outputObject, nameAsBytes, 0, nameAsBytes.length) ;
			return ;
			
		// Writes a player slot and a string.  Note that this allocates memory (probably),
		// but we expect these calls to be rare, and probably never
		// during gameplay.
		case TYPE_PLAYER_NAME:
		case TYPE_KICK:
		case TYPE_HOST:
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			nameAsBytes = string.getBytes() ;
			this.writeBytesInByteArray(outputObject, eightByteArray, 0, 4) ;
			this.writeBytesInByteArray(outputObject, nameAsBytes, 0, nameAsBytes.length) ;
			
			return ;
			
		// An integer (not a nonce).  This int conveys a particular
		// player slot, or the number thereof.
		case TYPE_TOTAL_PLAYER_SLOTS:
		case TYPE_PERSONAL_PLAYER_SLOT:
		case TYPE_PLAYER_QUIT:
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			eightByteArray[4] = significant ? (byte)1 : (byte)0 ;
			this.writeBytesInByteArray(outputObject, eightByteArray, 0, 5) ;
			return ;
			
		// No content
		case TYPE_INTERROGATION_COMPLETE:
			return ; 
			
		case TYPE_HOST_PRIORITY:
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;		// we store "priority" as player slot
			this.writeBytesInByteArray(outputObject, eightByteArray, 0, 4) ;
			return ;
			
		case TYPE_I_AM_HOST:
		case TYPE_I_AM_CLIENT:
		case TYPE_YOU_ARE_CLIENT:
			return ;		// no content
			
		case TYPE_PING:
		case TYPE_PONG:
			ByteArrayOps.writeLongAsBytes(num, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputObject, eightByteArray, 0, 8) ;
			return ;
			
		case TYPE_KEEP_ALIVE:
			return ;
			
		case TYPE_KICK_WARNING:
			// player slot, duration, string.
			ByteArrayOps.writeIntAsBytes(playerSlot, byteArray, 0) ;
			ByteArrayOps.writeLongAsBytes(num, byteArray, 4) ;
			nameAsBytes = string.getBytes() ;
			this.writeBytesInByteArray(outputObject, byteArray, 0, 12) ;
			this.writeBytesInByteArray(outputObject, nameAsBytes, 0, nameAsBytes.length) ;
			return ;
			
		case TYPE_KICK_WARNING_RETRACTION:
			// player slot
			ByteArrayOps.writeIntAsBytes(playerSlot, eightByteArray, 0) ;
			this.writeBytesInByteArray(outputObject, eightByteArray, 0, 4) ;
			break ;
		
		// WHAT!??
		default:
			return ;			// I DON'T KNOW WHAT THIS IS!
		}
	}
	
	
	public final boolean fits( Object outputDest ) throws IOException {
		if ( outputDest instanceof OutputStream )
			return true ;
		else if ( outputDest instanceof WritableByteChannel )
			return true ;
		else if ( outputDest instanceof ByteBuffer )
			return contentLength() + 5 <= ((ByteBuffer)outputDest).remaining() ;
		else
			throw new IllegalArgumentException("Provided output dest must be an OutputStream, a ByteBuffer, or a WriteableByteChannel") ;
	}
	
	
	/**
	 * This method writes the message to the provided OutputStream.
	 * Subclass message types will be written using 
	 */
	public final void write( Object outputDest ) throws IOException {
		makeByteArrayIfNeeded() ;
		
		// We write type and length, then write content.
		int len = contentLength() ;
		writeTypeAndLengthArray[0] = type ;
		ByteArrayOps.writeIntAsBytes(len, writeTypeAndLengthArray, 1) ;
		
		this.writeBytesInByteArray(outputDest, writeTypeAndLengthArray, 0, 5) ;
		
		// Now content!
		writeContent(outputDest) ;
		
		// Done.
	}
	
	
	/**
	 * Call this method ONCE for every full message that should
	 * be read from an inputStream.  The Message's "read" method
	 * MAY use non-blocking reads, meaning multiple calls to
	 * "read" may be necessary to retrieve a single message
	 * (this allows the thread to continue even if the entire
	 * message is as yet unavailable; just proceed as if no message
	 * was present.
	 */
	public final void resetForRead() {
		nullOutsideReferences() ;
		type = TYPE_UNKNOWN ;
		readComplete = false ;
		
		typeRead = false ;
		lengthRead = false ;
		contentBytesRead = 0 ;
		length = 0 ;
	}
	
	

	

	/**
	 * This method attempts to read from the provided input stream.
	 * If it returns 'true', then this Message contains the same
	 * information as the Message written to the stream, assuming it
	 * was an instance of the same subclass.  If it returns 'false',
	 * then the read was incomplete and should be repeated.  Will
	 * throw an exception if input is unacceptable.
	 * 
	 * NOTE: Remember that once this method has returned 'true',
	 * it is the caller's obligation to call resetForRead() before
	 * attempting to read the next message.
	 * 
	 * We make no assumptions as to whether this method using blocking
	 * or non-blocking reads on the input stream.
	 * 
	 * @param is
	 * @return
	 * @throws IOException 
	 */
	public final boolean read( Object inputSource ) throws IOException, ClassNotFoundException {
		makeEightByteArrayIfNeeded() ;
		
		if ( readComplete )
			return true ;
		
		// Read the message type, if we haven't read it already.
		if ( !typeRead ) {
			int numBytes = this.readBytesIntoByteArray(inputSource, eightByteArray, 0, 1) ;
			if ( numBytes == 0 )
				return false ;
			if ( numBytes == -1 )
				throw new IOException("read failed: end of stream or input source") ;
			
			type = (byte)eightByteArray[0] ;
			
			typeRead = true ;
		}
		
		// Read the message length.  This blocks until we get 4 bytes, or until -1 is
		// returned (indicating EOF) or an exception is thrown.
		if ( !lengthRead ) {
			int lengthBytesRead = 0 ;
			while ( lengthBytesRead < 4 ) {
				int readThisTime = this.readBytesIntoByteArray(inputSource, eightByteArray, lengthBytesRead, 4 - lengthBytesRead) ;
				if ( readThisTime == -1 )
					throw new IOException("read failed: end of stream") ;
				lengthBytesRead += readThisTime ;
			}
			
			length = ByteArrayOps.readIntAsBytes(eightByteArray, 0) ;
			lengthRead = true ;
			
		}
		
		// We have read the type and length.  If 'type' is a subclass type,
		// pass control to readMessageContent.  Otherwise, read the message
		// ourselves.
		if ( type >= Message.MIN_TYPE_IN_SUBCLASS ) {
			return readMessageContent( inputSource ) ;
		} else
			return readMessageContentBaseClass( inputSource ) ;
	}
	
	
	private boolean readMessageContentBaseClass( Object inputSource ) throws IOException {
		// Read it ourselves!
		if ( !readBytesIntoByteArray( inputSource ) ) {
			// Incomplete.  Return false.
			return false ;
		}
		
		// Okay, we have the whole message.
		// We have the message contents.  Now set the relevant
		// field for the message type.
		switch( type ) {
		// These messages have 0 length (no content); since we
		// have read the message type, we are done.
		case TYPE_UNKNOWN:
		case TYPE_NONCE_REQUEST:
		case TYPE_PERSONAL_NONCE_REQUEST:
		case TYPE_NAME_REQUEST:
		case TYPE_EXIT:
		case TYPE_SERVER_CLOSING:
		case TYPE_SERVER_CLOSING_FOREVER:
		case TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION:
		case TYPE_WELCOME_TO_SERVER:
			break ;
		
		// Global nonce.  Write a long.
		case TYPE_NONCE:
			nonce = new Nonce( byteArray ) ;
			//System.out.println("Read as a nonce with value " + nonce + " bytes " + byteBuffer[0] + " " + byteBuffer[1] + " " + byteBuffer[2] + " " + byteBuffer[3] + " " + byteBuffer[4] + " " + byteBuffer[5] + " " + byteBuffer[6] + " " + byteBuffer[7] ) ;
			break ;
			
		// Personal nonce.  A player slot and a long.
		case TYPE_PERSONAL_NONCE:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			nonce = new Nonce( byteArray, 4 ) ;
			//System.out.println("Read as a nonce with value " + nonce + " bytes " + byteBuffer[0] + " " + byteBuffer[1] + " " + byteBuffer[2] + " " + byteBuffer[3] + " " + byteBuffer[4] + " " + byteBuffer[5] + " " + byteBuffer[6] + " " + byteBuffer[7] ) ;
			break ;
			
		// A string.
		case TYPE_MY_NAME:
			string = new String( byteArray, 0,  length ) ;
			break ;
			
		// A player slot (4 bytes), then a string.
		case TYPE_PLAYER_NAME:
		case TYPE_KICK:
		case TYPE_HOST:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			string = new String( byteArray, 4, length-4 ) ;
			break ;
		
		// An integer (not a nonce).  This int conveys a particular
		// player slot, or the number thereof.
		case TYPE_TOTAL_PLAYER_SLOTS:
		case TYPE_PERSONAL_PLAYER_SLOT:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		// An Integer (playerSlot) and whether this is significant.
		case TYPE_PLAYER_QUIT:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			significant = byteArray[4] != 0 ;
			break ;
			
		// No content
		case TYPE_INTERROGATION_COMPLETE:
			break ;
			
		case TYPE_HOST_PRIORITY:
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;
			
		case TYPE_I_AM_HOST:
		case TYPE_I_AM_CLIENT:
		case TYPE_YOU_ARE_CLIENT:
			break ;		// no content
			
		case TYPE_PING:
		case TYPE_PONG:
			num = ByteArrayOps.readLongAsBytes(byteArray, 0) ;
			break ;
			
		case TYPE_KEEP_ALIVE:
			break ;
			
		case TYPE_KICK_WARNING:
			// player slot, duration, string.
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			num = ByteArrayOps.readLongAsBytes(byteArray, 4) ;
			string = new String( byteArray, 12, length-12 ) ;
			break ;
			
		case TYPE_KICK_WARNING_RETRACTION:
			// player slot
			playerSlot = ByteArrayOps.readIntAsBytes(byteArray, 0) ;
			break ;

		default:
			// WHOOPS!  A message type we don't recognize!
			throw new IOException("Message had invalid type " + type) ;
		}
		
		return true ;
	}
	
	
	
	///////////////////////////////////////////////////////////////////
	//
	// FINAL METHODS FOR SUPERCLASS MESSAGE TYPES
	// 
	// These methods, primarily get/setters, are meant for this class's
	// message types.
	//
	///////////////////////////////////////////////////////////////////
	

	
	/**
	 * What is the type of this message?
	 * @return
	 */
	public final byte getType() {
		return type ;
	}
	
	
	public final Nonce getNonce() {
		return nonce ;
	}
	
	public final int getPlayerSlot() {
		return playerSlot ;
	}
	
	public final int getNumPlayerSlots() {
		return playerSlot ;
	}
	
	public final String getName() {
		return string ;
	}
	
	public final String getText() {
		return string ;
	}
	
	public final int getPriority() {
		return playerSlot ;	//just an int
	}
	
	public final boolean getFatal() {
		return significant; 
	}
	
	public final long getWarningDuration() {
		return num ;
	}
	
	
	/**
	 * Sets this as an unknown message.
	 */
	public final Message setAsUnknown( ) {
		nullOutsideReferences() ;
		
		type = TYPE_UNKNOWN ;
		return this ;
	}
	
	/**
	 * Sets this as an exit message, to allow 
	 * users to gracefully exit a game.
	 */
	public final Message setAsExit() {
		nullOutsideReferences() ;
		
		type = TYPE_EXIT ;
		return this ;
	}
	
	/**
	 * Sets this Message as a nonce request.  Typically, this message
	 * is sent from the Server to the Client to verify that the client
	 * has the privilege of joining.
	 */
	public final Message setAsNonceRequest( ) {
		nullOutsideReferences() ;
		
		type = TYPE_NONCE_REQUEST ;
		return this ;
	}	
	
	
	
	
	/**
	 * Sets this Message as a nonce.  The nonce uniquely identifies
	 * a particular game.
	 * @param nonce
	 */
	public final Message setAsNonce( Nonce nonce ) {
		nullOutsideReferences() ;
		
		type = TYPE_NONCE ;
		this.nonce = nonce ;
		return this ;
	}	
	
	
	/**
	 * Sets this Message as a request for the personal Nonce.  The personal
	 * nonce uniquely identifies a player/game tuple.
	 * @param playerSlot
	 */
	public final Message setAsPersonalNonceRequest() {
		nullOutsideReferences() ;
		
		type = TYPE_PERSONAL_NONCE_REQUEST ;
		return this ;
	}	
	
	/**
	 * Sets this Message as a personal nonce.  The personal nonce uniquely
	 * identifies a player/game tuple.
	 * @param playerSlot
	 * @param nonce
	 */
	public final Message setAsPersonalNonce( int player, Nonce nonce ) {
		nullOutsideReferences() ;
		
		type = TYPE_PERSONAL_NONCE ;
		this.playerSlot = player ;
		this.nonce = nonce ;
		return this ;
	}

	/**
	 * Sets this Message as a name request.  Players can choose
	 * arbitrary names.
	 * @param playerSlot
	 */
	public final Message setAsNameRequest( int playerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_NAME_REQUEST ;
		this.playerSlot = playerSlot ;
		return this ;
	}
	
	
	public final Message setAsMyName( String name ) {
		nullOutsideReferences() ;
		
		type = TYPE_MY_NAME ;
		this.string = name ;
		return this ;
	}
	

	/**
	 * Sets this Message as a player name.
	 * @param namedPlayerSlot
	 * @param name
	 */
	public final Message setAsPlayerName( int namedPlayerSlot, String name ) {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYER_NAME ;
		playerSlot = namedPlayerSlot ;
		this.string = name ;
		return this ;
	}
	
	public final Message setAsKick( int kickedPlayerSlot, String text ) {
		nullOutsideReferences() ;
		
		type = TYPE_KICK ;
		playerSlot = kickedPlayerSlot ;
		this.string = text ;
		return this ;
	}
	
	
	public final Message setAsHost( int slot, String name ) {
		nullOutsideReferences() ;
		
		type = TYPE_HOST;
		this.playerSlot = slot ;
		this.string = name;
		return this ;
	}
	
	/**
	 * Sets this message as the total number of player slots for
	 * the game (usually 2).
	 * @param numSlots
	 */
	public final Message setAsTotalPlayerSlots( int numSlots ) {
		nullOutsideReferences() ;
		
		type = TYPE_TOTAL_PLAYER_SLOTS ;
		playerSlot = numSlots ;
		return this ;
	}
	
	
	/**
	 * Sets this Message as an indication that the recipient (or sender,
	 * for some outlandish circumstances) has been given a certain
	 * player slot, 0,...,numSlots-1.
	 * @param targetPlayerSlot
	 */
	public final Message setAsPersonalPlayerSlot( int targetPlayerSlot ) {
		nullOutsideReferences() ;
		
		type = TYPE_PERSONAL_PLAYER_SLOT ;
		playerSlot = targetPlayerSlot ;
		return this ;
	}
	
	
	public Message setAsWelcomeToServer() {
		nullOutsideReferences() ;
		
		type = TYPE_WELCOME_TO_SERVER ;
		return this ;
	}
	
	
	public final Message setAsServerClosing() {
		nullOutsideReferences() ;
		
		type = TYPE_SERVER_CLOSING ;
		return this ;
	}
	
	public final Message setAsServerClosingForever() {
		nullOutsideReferences() ;
		
		type = TYPE_SERVER_CLOSING_FOREVER ;
		return this ;
	}
	
	
	public final Message setAsPlayerQuit( int playerSlot, boolean fatal ) {
		nullOutsideReferences() ;
		
		type = TYPE_PLAYER_QUIT ;
		this.playerSlot = playerSlot ;
		this.significant = fatal ;
		return this ;
	}
	
	
	/**
	 * RESERVED!  Should only be used by the MessagePassingConnection.
	 * @return
	 */
	public final Message setAsDisconnectMessagePassingConnection( ) {
		nullOutsideReferences() ;
		
		type = TYPE_DISCONNECT_MESSAGE_PASSING_CONNECTION ;
		return this ;
	}
	
	
	public final Message setAsInterrogationComplete() {
		nullOutsideReferences() ;
		
		type = TYPE_INTERROGATION_COMPLETE ;
		return this ;
	}
	
	
	public final Message setAsHostPriority(int priority) {
		nullOutsideReferences() ;
		
		this.playerSlot = priority ;	// just an int
		type = TYPE_HOST_PRIORITY ;
		return this ;
	}
	
	public final Message setAsIAmHost() {
		nullOutsideReferences() ;
		
		type = TYPE_I_AM_HOST ;
		return this ;
	}
	
	public final Message setAsIAmClient() {
		nullOutsideReferences() ;
		
		type = TYPE_I_AM_CLIENT ;
		return this ;
	}
	
	public final Message setAsYouAreClient() {
		nullOutsideReferences() ;
		
		type = TYPE_YOU_ARE_CLIENT ;
		return this ;
	}
	
	public final Message setAsPing(long num) {
		nullOutsideReferences() ;
		
		type = TYPE_PING ;
		this.num = num ;
		return this ;
	}
	
	public final Message setAsPong(long num) {
		nullOutsideReferences() ;
		
		type = TYPE_PONG ;
		this.num = num ;
		return this ;
	}
	
	public final Message setAsKeepAlive() {
		nullOutsideReferences() ;
		
		type = TYPE_KEEP_ALIVE ;
		return this ;
	}
	
	public final Message setAsKickWarning( int kickedPlayerSlot, String msg, long timeUntilKick ) {
		nullOutsideReferences() ;
		
		type = TYPE_KICK_WARNING ;
		playerSlot = kickedPlayerSlot ;
		this.string = msg ;
		this.num = timeUntilKick ;
		return this ;
	}
	
	public final Message setAsKickWarningRetraction( int slot ) {
		nullOutsideReferences() ;
		
		type = TYPE_KICK_WARNING_RETRACTION ;
		playerSlot = slot ;
		return this ;
	}
	
	public Message setAs( Message m ) {
		nullOutsideReferences() ;
		
		this.type = m.getType() ;
		this.playerSlot = m.playerSlot ;
		this.nonce = m.nonce ;
		this.string = m.string ;
		this.num = m.num ;
		this.significant = m.significant ;
		
		return this ;
	}
	
}
