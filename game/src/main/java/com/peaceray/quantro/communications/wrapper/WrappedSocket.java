package com.peaceray.quantro.communications.wrapper;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A SocketWrapper provides a means of interacting with TCP (Socket) or
 * UDP (DatagramSocket) socket objects that forces guaranteed in-order delivery of 
 * messages.  It also provides metadata on the connection, such
 * as (most obviously) address and port, but less obviously the ping,
 * and in the case of UDP, an early indication of whether the DatagramSocket
 * is "connected" (i.e. messages can be sent and received).
 * 
 * To support these capabilities, SocketWrappers will send metadata in addition
 * to normal data.  As a user of SocketWrapper, you must ensure that BOTH
 * ends of the Socket (local and remote host) are wrapped in a SocketWrapper.
 * SocketWrappers automatically generate and send metadata, and automatically
 * collect and process it.  This is largely invisible to the user, but only so
 * long as both ends use a Wrapper.  Otherwise, the unwrapped host will receive
 * sent information interspersed with bizarre inserted bytes.
 * 
 * Always double-bag it.
 * 
 * WHEN TO WRAP: Wrap a connected Socket or other network communication
 * object once you are assured that the other side is also wrapping it.
 * When one side is wrapped, it will discard all incoming bytes that are
 * not 0, 1 or 2.  Wrapped messages always begin with 0 1 and 2.  If data
 * is sent over the connection before wrapping, either ensure that no residual
 * data will be received after wrapping, or ensure that all bytes sent are not
 * 0, 1 or 2.  Here's a few examples as to how:
 * 
 * TCP Sockets:
 * 		Allow one end of the connection to drive the conversation,
 * 		using a query-response paradigm.  When it comes time to wrap the
 * 		socket, the master sends a "wrap now" message or similar.  The
 * 		client MUST wrap their socket immediately upon receipt, OR should
 * 		respond with some explicit confirmation message and then wrap, etc.
 * 		The important point is that one party is responsible for declaring
 * 		that it is time to wrap and wrapping occurs deterministically from there.
 * 
 * UDP Sockets:
 * 		Establish a UDP hole-punched connection sending Datagrams that
 * 		begin with non 0,1,2 bytes.  Ensure the subclass implementation
 * 		always returns full Datagrams upon readFromSocket.  This way,
 * 		even if a connection establishing Datagram comes in after the
 * 		socket is wrapped, the first byte will be checked and the entire
 * 		Datagram discarded.
 * 
 * 		Wrapping is relatively safe, because Wrapped Datagrams will be
 * 		sent and resent and resent until acknowledgment - if one side
 * 		is wrapped before the other, the first-wrapped side will keep
 * 		sending messages until the other is Wrapped and responds.
 * 
 * NOTE: A SocketWrapper may preemptively close a connection if it experiences
 * unacceptable delays or strange behavior by the connected host.
 * 
 * 
 * KNOWN BUGS: Messages are sent with sequential message numbers.  Both reads and
 * writes will fail once sequence numbers are exhausted.
 * 
 * This is not considered a serious problem, as sequence numbers are positive longs,
 * and thus by default the connection will last until it transmits a minimum of 
 * 2^63 bytes (2^39 GB) or remains open for 2^64 seconds.
 * 
 * @author Jake
 *
 */
public abstract class WrappedSocket {
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// SOCKET DATA ACCESS
	//
	// The wrapper assumes you don't directly access a wrapped socket.  Here
	// are some safe accessors to use; they function as if called on the wrapped
	// Socket or DatagramSocket.
	//
	///////////////////////////////////////////////////////////////////////////
	public abstract InetAddress getInetAddress() ;
	public abstract InetAddress getLocalAddress() ;
	public abstract int getLocalPort() ;
	public abstract SocketAddress getLocalSocketAddress() ;
	public abstract int getPort() ;
	public abstract SocketAddress getRemoteSocketAddress() ;
	
	public abstract boolean isBound() ;
	public abstract boolean isClosed() ;
	public abstract boolean isConnected() ;
	
	
	/**
	 * Returns the time since the last received message - either a SYN or a DATA.
	 */
	public abstract long timeSinceLastReceived() ;
	
	/**
	 * Returns the amount of time this socket has been waiting for an acknowledgment.
	 * A 0 means there are no pending sends, either DATAs or SYNs.
	 * @return
	 */
	public abstract long timeWaitingForAck() ;
	
	
	
	/**
	 * A non-blocking method which returns latest ping time.  Pings are one feature handled
	 * automatically by SocketWrapper; you never need to worry about explicitly sending
	 * a Ping.  Will return -1 if no Ping has yet been sent; otherwise, the result is non-negative.
	 * 
	 * Some 'pings' are invisible - if the socket is frequently used to transmit messages,
	 * it will never need to explicitly send a ping, since we can measure the time-to-acknowledge
	 * of those messages instead.
	 * 
	 * NOTE: Some connection types do not guarantee the successful delivery of
	 * a message (e.g. DatagramSocket).  Thus, a 'ping' is not guaranteed a response.
	 * In this case, the SocketWrapper will resend the 'ping' after a short delay, repeating
	 * until a response arrives.  The total time between the first 'ping' sent and the first
	 * 'pong' received is considered the round-trip time (i.e., it includes delays caused
	 * by lost messages).
	 * 
	 * @return In milliseconds, the approximate round-trip time for the latest ping (includes
	 * time needed for the thread to read and recognize the 'pong' response).
	 */
	public abstract long latestPing() ;
	
	/**
	 * A non-blocking method which returns a weighted average of ping time.  The specific 
	 * averaging method is left to subclasses to decide; it is recommended that a weighting scheme
	 * be used that biases towards recent ping attempts.
	 * 
	 * NOTE: As in latestPing, the result gives round-trip time including processing time and
	 * delays caused by packet loss.
	 * 
	 * @return In milliseconds, a weighted average of round-trip ping time.
	 */
	public abstract long averagePing() ;
	
	/**
	 * Provides a per-message (ping or content message) estimate of "success rate" - the
	 * proportion of messages sent that reached their destination without issue.
	 * This value is the number of successful sends (defined as a send for which a
	 * corresponding ACK is then received) divided by the number of attempts (the
	 * total number of sends, including those not ACKed).  Higher values are better;
	 * a 1.0 should be considered a perfect connection (e.g. TCP, with guaranteed delivery,
	 * should always have a success rate of 1), while a 0.5 means that on average
	 * a message must be sent twice before an acknowledgment is received.  This rate includes
	 * both automatic Ping messages and explicitly sent messages (which may not correspond
	 * exactly in number to logical messages sent by the user; SocketWrapper reserves the
	 * right to send in multiple consecutive pieces, or combine messages into one piece).
	 * 
	 * NOTE: The above gives only a generalized description of the way this value is
	 * calculated.  Subclasses should feel free to implement weighting schemes that bias
	 * towards recent performance.
	 * 
	 * FURTHER NOTE: Users of the SocketWrapper need not be concerned about the details
	 * described above.  ACK messages are invisible to users, assuming both ends are
	 * Wrapped.
	 * 
	 * @return A value in 0-1 representing the success rate of the connection.  Higher
	 * is better.  1.0 / <returnedValue> gives the approximate number of times a message
	 * must be sent before it is acknowledged.
	 */
	public abstract double successRate() ;
	
	
	/**
	 * The number of bytes sent over our communication channel.  This includes any
	 * synchronization or metadata: not just the number of bytes an outside user
	 * has placed on this WS, but the number that have actually been transmitted by it.
	 * @return
	 */
	public abstract long bytesSent() ;
	
	
	/**
	 * The number of bytes received over our communication channel.  This includes any
	 * synchronization or metadata: not just the number of bytes an outside user
	 * has sent to this WS, but the number that have actually been transmitted to it.
	 * @return
	 */
	public abstract long bytesReceived() ;
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// I/O
	//
	// Here's how you interact with a SocketWrapper.  Data goes in, data comes
	// out, never a miscommunication.
	//
	// By default, a WrappedSocket communicates using Piped byte channels.
	// However, some instances may instead prefer to pass instances of a particular
	// Object type.  We call this 'Object Awareness.'
	//
	// WrappedSockets which are ObjectAware can verify whether a particular
	// object meets their standards.
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Returns an InputStream to allow socket reads.  This Stream is of an
	 * implementation allowing calls to 'available'.
	 * 
	 * If subclasses have a specialized method of handling input and output,
	 * override this method.  Remember that wrapper.wrapperOutgoingPOS
	 * is the method of writing to sockets, and wrapper.wrapperIncomingPIS
	 * is the method of reading, so implement your I/O on top of these streams.
	 * 
	 * @throws IllegalStateException if this WrappedSocket has been setup
	 * with an ObjectSourceDelegate
	 */
	public abstract Pipe.SourceChannel getSourceChannel() ;
	
	/**
	 * Returns an OutputStream to allow Socket writes.  Data written to the
	 * stream is guaranteed to arrive in-order at its destination, but ONLY
	 * IF the other end is also Wrapped.
	 * 
	 * @throws IllegalStateException if this WrappedSocket has been setup
	 * with an ObjectSourceDelegate
	 * @return
	 */
	public abstract Pipe.SinkChannel getSinkChannel() ;
	
	
	/**
	 * Is this WrappedSocket byte aware?  i.e., is it appropriate to
	 * communicate in/out with Pipe channels?
	 * @return
	 */
	public abstract boolean isByteAware() ;
	
	
	/**
	 * Is this WrappedSocket Object aware?  If 'null' is passed, returns whether
	 * this wrapped socket is aware of the specified class.
	 * 
	 * If 'true' is returned, we assume this WS can handle passing any instance
	 * of the specified class (even subclasses).
	 */
	public abstract boolean isObjectAware( Object o ) ;
	
	/**
	 * Is this WrappedSocket Object aware?  If 'null' is passed, returns whether
	 * this wrapped socket is aware of the specified Object type.
	 * 
	 * If 'true' is returned, we assume this WS can handle passing any instance
	 * of the specified class (even subclasses).
	 * 
	 * @param c
	 * @return
	 */
	public abstract boolean isObjectAware( Class<?> c ) ;
	
	
	/**
	 * Interaction with an Object Aware WS requires callbacks.  Implement this
	 * interface to receive Objects from the WrappedSocket.
	 */
	public interface DataObjectSenderReceiver<T> {
		/**
		 * A convenience method, called after we place a DataObject on the outgoing
		 * BlockingQueue, 1 call per message.
		 * 
		 * @param ws
		 * @param dataObject as a convenience, the data object itself.  This object
		 * 		has also been added to the SourceQueue, allowing us to either operate
		 * 		on the object immediately (and pull if from the Queue ourselves) or
		 * 		leave it in the queue for later, but checking certain data now.
		 */
		public void dosr_dataObjectAvailable( WrappedSocket ws, Object dataObject ) ;
		
		/**
		 * The DOSR will never again receive a data object from this socket.
		 * @param ws
		 */
		public void dosr_dataObjectsExhaustedForever( WrappedSocket ws ) ;
	}
	
	/**
	 * Returns a BlockingQueue used to READ INCOMING DATA from the connection.
	 * @return
	 */
	public abstract BlockingQueue<?> getDataObjectSourceQueue() ;
	
	/**
	 * Returns a BlockingQueue used to WRITE OUTGOING DATA to the connection.
	 * @return
	 */
	public abstract BlockingQueue<?> getDataObjectSinkQueue() ;
	
	/**
	 * Returns a new instance of our DataObject, which can be filled with
	 * whatever data we want and then placed on a queue.
	 * 
	 * This method is used by both the WrappedSocket and the controlling
	 * object to acquire object instances for setting and communication,
	 * so it makes no assumptions about the usage of the instance.
	 * 
	 * The "recipient" of this object (i.e., if called by an outside 
	 * user, the "recipient" is probably this WrappedSocket after it
	 * is placed on the SinkQueue) should recycle it when finished.
	 * 
	 * Note: we do NOT retain a reference to the object after this call.
	 * It is therefore completely up to you whether you want to recycle
	 * it or just let the garbage collector reclaim it.
	 * @return
	 */
	public abstract Object getDataObjectEmptyInstance() ;
	
	/**
	 * Recycles an instance of the data object originally returned by
	 * getDataObjectEmptyInstance.
	 * 
	 * @param o
	 */
	public abstract void recycleDataObjectInstance( Object o ) ;
	
	/**
	 * Sets the sender / receiver for this WrappedSocket.
	 */
	public abstract void setDataObjectSenderReceiver( DataObjectSenderReceiver<?> receiver ) ;
	
	/**
	 * Informs the WrappedSocket that a data object has just been written to the
	 * sink queue.  Some implementations might constantly poll the other end of
	 * the SinkQueue making this call unnecessary (those implementations will
	 * have an immediately-returning implementation), but others require this
	 * method for responsiveness.
	 */
	public abstract void dataObjectAvailable() throws IllegalStateException ;
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// CLOSING CONNECTIONS
	//
	// Simple enough.  There is no 'open' method because we use Factory methods
	// to wrap existing connections.
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	public abstract void close() ;
	
	/**
	 * A general-purpose close method that closes the socket and all associated I/O streams.
	 * Remember to call the superclass method when you override.
	 * 
	 * NOTE: It is the subclass's responsibility to close any underlying I/O.
	 * 			Once this method has terminated, we have finished writing data;
	 * 			however, we will continue READING until readFromSocket() throws an
	 * 			exception.
	 * 
	 * @param flushOutgoing: Should we block and flush our outgoing messages?
	 * 
	 */
	public abstract void close( boolean flushOutgoing, long maximumWait ) ;
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// SUBCLASS HELPERS
	//
	// The basic notion of a WrappedSocket is that the underlying communication
	// channel is abstracted away, and reduced to a simple ByteChannel (two,
	// actually, one for reading, one for writing).  This communication channel
	// is reliable, and guarantees message delivery (or an explicit and automatic
	// transition to a "closed" or "broken" status).
	//
	// Internally, WrappedSockets help to maintain connectivity by sending 
	// specifically formatted messages.  These messages can be divided into
	// several types: DATA, SYN, ACK.  Additionally, every message carries a
	// unique "message number," which is an incrementing 'long.'  WrappedSockets
	// should either be 1. capable of ignoring and/or discarding messages
	// that have already been processed (previously seen message number), or
	// 2. guarantee that no such "repeats" are seen or sent.
	//
	// All messages are prefaced with 9 bytes of information; the first
	//		byte being the message type, the next 8 being the message number.
	//		All WrappedSocket implementations should use messages of this format,
	//		to allow new implementations to communicate with old ones that
	//		rely on the same underlying communication channel (e.g. an
	//		AutonomousWrappedUDPSocket communicating with a group server
	//		WS set).
	//
	// Message types and formatting as follows:
	//
	// Type		PrefixLength	MaxLength
	//	DATA		13				 512	
	//	SYN		 	9				 9
	//	ACK		 	9				 9
	// 	OBJ			13				 512
	//  OBJ_BYTES_START		13		 512
	//  OBJ_BYTES_MIDDLE	13		 512
	// 	OBJ_BYTES_END		13		 512
	//
	// Usage:
	//
	//	DATA: Data bytes sent by a user or controller of a WrappedSocket.  The
	//		first 13 bytes of this message are "bookkeeping data" added by the
	//		WS implementation; the rest are actual data.  Those 13 bytes:
	//		[ 0 | <unique msg Num as 8B long> | <remaining message Length as 4B int> ]
	//
	//	SYN: Simple PING, sent instead of a DATA if the connection lingers without
	//		data sent for a minimum period of time (some implementations only).
	//		Exactly 9 bytes long:
	//		[ 1 | <unique msg Num as 8B long> ]
	//
	//	ACK: Single PONG, an acknowledgement of message received.  ACKs are sent
	//		in response to both DATA and SYN messages -- meaning that DATA and SYN
	//		messages should draw from the same "message number" pool (no DATA should
	//		have the same msg Num as a SYN from the same source).  Exactly 9 bytes:
	//		[ 1 | <unique msg Num of DATA or SYN, as 8B long> ]
	//
	//	OBJ: For Object-aware connections, the object is written to the data of this
	//		message in a form from which it can construct itself.  This necessarily
	//		requires that the object write its own length explicitly.
	//		Instead of a 'data length', we provide the number of consecutive objects
	//		which have been packed into this message.  Each message is fully contained
	//		within.
	//
	//	OBJ_BYTES_START: For Objects which are too large to fit into a single message,
	//		this marks the beginning of a single Object in byte representation.  Number
	//		of bytes is indicated.
	//
	//	OBJ_BYTES_MIDDLE: Continuing an existing byte-representation for a single object.
	//		Length is explicitly provided.
	//
	//	OBJ_BYTES_END: The last in a series of Byte-wise Object representations.
	//
	//
	// Something to note: "message num" is an incrementing 'long' that counts
	// from 0 as messages are sent from a particular source.  The two parties
	// on either end of a WrappedSocket maintain their own message nums, incrementing
	// them independently as data is sent.
	//
	// Further note: as you can see, nowhere in the WS message protocol is the
	// "source" of a message identified.  This level of abstraction assumes
	// that the underlying communication channel is "paired" and that only
	// data from the paired user's own WrappedSocket will ever be received.
	// For underlying communication channels that do not fit this assumption
	// (e.g. DatagramSockets, which might receive packets from other users or
	// a different source on the user's device), implementations are encouraged
	// (read: required) to implement their own protection.  For example,
	// the current implementation of AutonomousWrappedUPDSocket uses a unique
	// "message prefix" that is prepended to every datagram before sending, and
	// stripped from every datagram received before processing; datagrams which
	// not carry this unique prefix are silently discarded.
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	// All messages are prepended with
	// [CODE] [NUM] [LEN?]
	//
	// Where 	[CODE] is one byte, among those defined below
	// 			[NUM] is a message number; all DATA and SYN messages
	//					will receive an ACK response with the same number.  8 bytes long.
	//			[LEN?] is 4-bytes defining the length of the message itself, in bytes.
	//					It is only included for DATA messages, since SYN/ACK have no content.
	protected static final byte DATA = 0 ;
	protected static final byte SYN = 1 ;
	protected static final byte ACK = 2 ;
	protected static final byte OBJ = 3 ;
	protected static final byte OBJ_BYTES_START = 4 ;
	protected static final byte OBJ_BYTES_MIDDLE = 5 ;
	protected static final byte OBJ_BYTES_END = 6 ;
	
	private static final int POSITION_TYPE = 0 ;
	private static final int POSITION_NUM = 1 ;
	
	// data...
	private static final int POSITION_DATA_LEN = 9 ;
	private static final int POSITION_DATA = 13 ;
	
	// obj...
	private static final int POSITION_OBJ_COUNT = 9 ;
	private static final int POSITION_OBJ = 13 ;
	
	// obj bytes...
	private static final int POSITION_OBJ_BYTES_LEN = 9 ;
	private static final int POSITION_OBJ_BYTES = 13 ;
	
	protected static final int MAX_MESSAGE_LENGTH = 512 ;
	protected static final int MAX_MESSAGE_OBJ_BYTES_LEN = MAX_MESSAGE_LENGTH - POSITION_OBJ_BYTES ;
	
	
	/**
	 * Sets the provided ByteBuffer to a DATA message with the
	 * provided message number.  "len" bytes are read from the provided
	 * array beginning at "offset": the provided ByteBuffer MUST be
	 * long enough to contain the message.  Upon return, the byte buffer
	 * will have limit len + 13 and will be positioned at 0 (unless an error
	 * occurs and an exception is thrown).
	 */
	protected static void setAsDATA( ByteBuffer bb, long messageNum, byte [] bytes, int offset, int len ) {
		bb.clear() ;
		bb.position(0) ;
		bb.mark() ;
		bb.limit(13 + len) ;
		
		// content
		bb.put(DATA) ;
		bb.putLong(messageNum) ;
		bb.putInt(len) ;
		bb.put(bytes, offset, len) ;
		bb.flip() ;	// limit is now len+13.
	}
	
	
	/**
	 * Sets the provided ByteBuffer to a DATA message with the
	 * provided message number.  'bbSource' is used as the message bytes
	 * in its entirety, without WS prefix information ('DATA' or message Num
	 * information).
	 * 
	 * bbSource is assumed to contain only and exactly the message bytes,
	 * with its limit() set appropriately.  Its limit() must be <= MAX_MESSAGE_LENGTH - 13.
	 * 
	 * After this call, bb will be formatted as a DATA message, with limit
	 * bbSource.limit() + 13.
	 * 
	 * bbSource will have its position() adjusted by this call.
	 */
	protected static void setAsDATA( ByteBuffer bb, long messageNum, ByteBuffer bbSource ) {
		bb.clear() ;
		bb.position(0) ;
		bb.mark() ;
		bb.limit(MAX_MESSAGE_LENGTH) ;
		
		// Reset bbSource
		bbSource.position(0) ;
		
		// content
		bb.put(DATA) ;
		bb.putLong(messageNum) ;
		bb.putInt(bbSource.limit()) ;
		bb.put(bbSource) ;
		bb.flip() ;	// limit is now len+13.
	}
	
	
	
	/**
	 * Sets 'bb' as an outgoing DATA message, using data read from the provided 'source.'
	 * Return success: whether 'bb' has been set to a DATA message containing
	 * at least 1 byte of data.  Returns 'false' if a non-blocking channel is
	 * provided and a 'read' attempt read no bytes.
	 *
	 * Return values:
	 * 'true': 'bb' is now a well-formatted DATA message with
	 * 		at least one byte of content data
	 * 'false': the provided channel is not blocking and 0 bytes were
	 * 		read.  If you desperately need to read from this channel, try
	 * 		calling this method again or converting the channel to blocking
	 * 'throws exception': the channel is not in a state to allow reads.
	 * 		It may have thrown an exception, or returned -1 upon read
	 * 		(indicating EOF).
	 * 
	 * Because we are reading from a Channel, there are a few complications.
	 * 
	 * 1: If the ReadableByteChannel is in a non-readable state, an exception
	 * 		may be thrown.
	 * 
	 * 2: This method may block if a block would occur from a single 'read' operation
	 * 		performed on the channel.  Depending on the Channel itself,
	 * 		this may or may not be predictable at the time of this call.
	 * 
	 * 		In particular, a channel configured to 'block' will guarantee that
	 * 		at least 1 byte is read, and will block until this byte is available.
	 * 
	 * @param bb The buffer into which we read our message data.  If 'true' is
	 * 		returned, this will be a well-formatted message, positioned at 0
	 * 		with limit appropriately set.  Otherwise, its contents are unspecified.
	 * @param messageNum
	 * @param source
	 * @throws IOException 
	 */
	protected static boolean setAsDATA( ByteBuffer bb, long messageNum, ReadableByteChannel source ) throws IOException {
		bb.clear().limit(MAX_MESSAGE_LENGTH).position(POSITION_DATA) ;
		int len = source.read(bb) ;
		
		if ( len < 0 ) {
			throw new EOFException("End of File Reached; no data read") ;
		} else if ( len == 0 ) {
			// no data available; channel is non-blocking
			return false ;
		}
		
		// set the message num, length, and MSG code.
		bb.position(0) ;
		bb.put(DATA).putLong(messageNum).putInt(len) ;
		bb.position(0) ;
		bb.limit(len + POSITION_DATA) ;
		return true ;
	}
	
	
	
	/**
	 * Sets the provided ByteBuffer to a SYN message with the provided
	 * message number.  Upon return, the byte buffer will have limit 9
	 * and will be positioned at 0.
	 * 
	 * @param bb
	 * @param messageNum
	 */
	protected static void setAsSYN( ByteBuffer bb, long messageNum ) {
		bb.position(0) ;
		bb.mark() ;
		bb.limit(9) ;
		
		// content
		bb.put(SYN) ;
		bb.putLong(messageNum) ;
		bb.reset() ;
	}
	
	
	/**
	 * Sets the provided ByteBuffer to an ACK message with the provided
	 * message number.  Upon return, the byte buffer will have limit 9
	 * and will be positioned at 0.
	 * 
	 * @param bb
	 * @param messageNum
	 */
	protected static void setAsACK( ByteBuffer bb, long messageNum ) {
		bb.position(0) ;
		bb.mark() ;
		bb.limit(9) ;
		
		// content
		bb.put(ACK) ;
		bb.putLong(messageNum) ;
		bb.reset() ;
	}
	
	
	/**
	 * Sets the provided ByteBuffer to an ACK message with message number
	 * appropriate to the received message.  It is an error to provide a
	 * received message is not a well-formatted DATA or SYN.
	 * 
	 * @param bb
	 * @param messageNum
	 */
	protected static void setAsACK( ByteBuffer bb, ByteBuffer msgReceived ) {
		switch( getMessageType( msgReceived ) ) {
		case DATA:
		case SYN:
			bb.position(0) ;
			bb.mark() ;
			bb.limit(9) ;
			
			// content
			bb.put(ACK) ;
			bb.putLong(getMessageNum(msgReceived)) ;
			bb.reset() ;
			return ;
		}
		
		throw new IllegalArgumentException("Provided message is not a well-formatted DATA or ACK.") ;
	}
	
	
	/**
	 * Sets the provided ByteBuffer to prepare for OBJ style writing.
	 * From here objects can be written at the current position; just 
	 * call 'setAsObj_incrementCount()' with each write.
	 * @param bb
	 */
	protected static void setAsOBJ_positionForFirst( ByteBuffer bb, long messageNum ) {
		bb.clear().limit(MAX_MESSAGE_LENGTH) ;
		
		bb.put(OBJ) ;
		bb.putLong(messageNum) ;
		bb.putInt(0) ;
		// currently holds 0 objects
	}
	
	/**
	 * Increments the number of objects counted as being included in the ByteBuffer.
	 * Make this call after every object write to 'bb'.
	 * @param bb
	 */
	protected static void setAsOBJ_incrementCount( ByteBuffer bb ) {
		int prev = bb.getInt(POSITION_OBJ_COUNT) ;
		bb.putInt(POSITION_OBJ_COUNT, prev+1) ;
	}
	
	
	
	/**
	 * Writes bytes to the provided ByteBuffer from the provided array.
	 * 
	 * Returns the number of bytes written, which should be added to 'pos'
	 * for the next call (if < len).
	 * 
	 * The specific message type written is determined by 'start', 'pos',
	 * and 'len.'
	 * 
	 * If start == pos, we assume this is a OBJ_BYTES_START messgage.  Else,
	 * we assume either _MIDDLE or _END, depending on whether we reach the
	 * end of the message bytes.
	 * 
	 * @param bb
	 * @param objBytes
	 * @param pos
	 * @param len
	 * @return
	 */
	protected static int setAsOBJBytes( ByteBuffer bb, long messageNum, byte [] objBytes, int start, int pos, int len ) {
		bb.clear().limit(MAX_MESSAGE_LENGTH) ;
		// write as many bytes as we can...
		int numToWrite = Math.min(MAX_MESSAGE_LENGTH - POSITION_OBJ_BYTES, len - pos) ;
		byte type ;
		if ( start == pos )
			type = OBJ_BYTES_START ;
		else if ( pos + numToWrite == len )
			type = OBJ_BYTES_END ;
		else
			type = OBJ_BYTES_MIDDLE ;
		
		bb.put(type) ;
		bb.putLong(messageNum) ;
		bb.putInt(numToWrite) ;
		bb.put(objBytes, pos, numToWrite) ;
		bb.flip() ;
		
		return numToWrite ;
	}
	
	
	/**
	 * Does this message have a labeled "type": DATA, SYN, ACK, OBJ, OBJ_BYTES_*, etc.?
	 * @param bb
	 * @return
	 */
	protected static boolean isType( ByteBuffer bb ) {
		byte type = getMessageType(bb) ;
		switch( type ) {
		case DATA:
		case SYN:
		case ACK:
		case OBJ:
		case OBJ_BYTES_START:
		case OBJ_BYTES_MIDDLE:
		case OBJ_BYTES_END:
			return true ;
		}
		return false ;
	}
	
	/**
	 * Is this message of type DATA?
	 * @param bb The ByteBuffer holding the message.
	 * 		Position, limit, mark, etc. are NOT affected.
	 * @return
	 */
	protected static boolean isDATA( ByteBuffer bb ) {
		return DATA == bb.get(POSITION_TYPE) ;
	}
	
	/**
	 * Is this message of type SYN?
	 * @param bb The ByteBuffer holding the message.
	 * 		Position, limit, mark, etc. are NOT affected.
	 * @return
	 */
	protected static boolean isSYN( ByteBuffer bb ) {
		return SYN == bb.get(POSITION_TYPE) ;
	}
	
	/**
	 * Is this message of type ACK?
	 * @param bb The ByteBuffer holding the message.
	 * 		Position, limit, mark, etc. are NOT affected.
	 * @return
	 */
	protected static boolean isACK( ByteBuffer bb ) {
		return ACK == bb.get(POSITION_TYPE) ;
	}
	
	protected static boolean isOBJ( ByteBuffer bb ) {
		return OBJ == bb.get(POSITION_TYPE) ;
	}
	
	protected static boolean isOBJ_BYTES( ByteBuffer bb ) {
		byte type = bb.get(POSITION_TYPE) ;
		switch( type ) {
		case OBJ_BYTES_START:
		case OBJ_BYTES_MIDDLE:
		case OBJ_BYTES_END:
			return true ;
		}
		return false ;
	}
	
	protected static boolean isOBJ_BYTES_START( ByteBuffer bb ) {
		return OBJ_BYTES_START == bb.get(POSITION_TYPE) ;
	}
	
	protected static boolean isOBJ_BYTES_MIDDLE( ByteBuffer bb ) {
		return OBJ_BYTES_MIDDLE == bb.get(POSITION_TYPE) ;
	}
	
	protected static boolean isOBJ_BYTES_END( ByteBuffer bb ) {
		return OBJ_BYTES_END == bb.get(POSITION_TYPE) ;
	}
	
	
	
	
	/**
	 * Returns the message type -- DATA, SYN or ACK.  Appropriate
	 * for switch-statement constructions rather than if statements.
	 * @param bb The ByteBuffer holding the message.
	 * 		Position, limit, mark, etc. are NOT affected.
	 * @return
	 */
	protected static byte getMessageType( ByteBuffer bb ) {
		return bb.get(POSITION_TYPE) ;
	}
	
	
	/**
	 * Returns the message num.
	 * @param bb The ByteBuffer holding the message.
	 * 		Position, limit, mark, etc. are NOT affected.
	 * @return
	 */
	protected static long getMessageNum( ByteBuffer bb ) {
		return bb.getLong(POSITION_NUM) ;
	}
	
	
	protected static int getMessageDATALength( ByteBuffer msg ) {
		if ( isDATA( msg ) )
			return msg.getInt(POSITION_DATA_LEN) ;
		
		throw new IllegalArgumentException("Provided message is not a well-formatted DATA type.") ;
	}
	
	
	/**
	 * Reads the DATA bytes from the provided message into 'bytes'.\
	 * It is an error for 'bytes' to lack the remaining space to hold
	 * the message data, or for the provided 'msg' to not be a well-formatted
	 * DATA message.
	 * @param msg: The received DATA message.  Its position will be adjusted
	 * 		by this call.
	 * @param bytes
	 * @param offset
	 * @return
	 */
	protected static int getMessageDATA( ByteBuffer msg, byte [] bytes ) {
		return getMessageDATA( msg, bytes, 0 ) ;
	}
	
	/**
	 * Reads the DATA bytes from the provided message into 'bytes', beginning
	 * at 'offset.'  It is an error for 'bytes' to lack the remaining space to hold
	 * the message data, or for the provided 'msg' to not be a well-formatted
	 * DATA message.
	 * @param msg: The received DATA message.  Its position will be adjusted
	 * 		by this call.
	 * @param bytes
	 * @param offset
	 * @return
	 */
	protected static int getMessageDATA( ByteBuffer msg, byte [] bytes, int offset ) {
		if ( isDATA( msg ) ) {
			int len = msg.getInt(POSITION_DATA_LEN) ;
			msg.position(POSITION_DATA) ;
			msg.get(bytes, offset, msg.remaining()) ;
			return len ;
		}
		
		throw new IllegalArgumentException("Provided ByteBuffer is not a well-formatted DATA message.") ;
	}
	
	
	
	/**
	 * Reads the message's DATA bytes (and ONLY those bytes, not type, len or
	 * message num) into the provided destination.  Throws an exception if
	 * 'msg' is not a well-formatted DATA message, or 'dst' has insufficient
	 * space to contain it.
	 * 
	 * @param msg The DATA message provided.  Its position will be adjusted
	 * 		by this call.  Its 'limit' will be set according to its
	 * 		specified DATA length.
	 * @param dst The destination for the DATA bytes in 'msg.'  If this call
	 * 		returns without an exception, its position will be 0, its limit
	 * 		the 'data length' of 'msg', and the bytes between set to the actual
	 * 		message DATA.
	 */
	protected static void getMessageDATA( ByteBuffer msg, ByteBuffer dst ) {
		if ( isDATA(msg) ) {
			int len = msg.getInt(POSITION_DATA_LEN) ;
			dst.clear() ;
			msg.position(POSITION_DATA).limit(POSITION_DATA + len) ;
			dst.put(msg) ;
			dst.flip() ;
			return ;
		}
		
		throw new IllegalArgumentException("Provided message is not a well-formatted DATA message.") ;
	}
	
	
	/**
	 * Retrieves the messages's DATA bytes from 'msg' and writes them to 'dst.'
	 * 
	 * If 'false' is returned, then no data has been written to the destination channel.
	 * If 'true' is returned, then the entire messages has been written.  In no case
	 * will this method only write part of the message data but not all.
	 * 
	 * How to guarantee this?  We attempt one write.  If that write sends 0 bytes,
	 * we terminate (unless 'force' is true).  Otherwise, we loop until all bytes
	 * have been written.
	 * 
	 * Unlike the 'write' method in WriteableByteChannel, this method will guarantee
	 * that either all or no message data has been written to the channel.
	 * 
	 * This method will potentially 'loop forever' if provided with a non-blocking
	 * channel that is not ready to accept bytes.  This 'active spin' can
	 * be avoided by using getMessageDATABytes() instead.
	 * 
	 * @param msg
	 * @param dst
	 * @throws IOException 
	 */
	protected static boolean getMessageDATA( ByteBuffer msg, boolean force, WritableByteChannel dst ) throws IOException {
		if ( isDATA(msg) ) {
			int len = msg.getInt(POSITION_DATA_LEN) ;
			msg.position(POSITION_DATA).limit(POSITION_DATA + len) ;
			// first write attempt.
			int bytes = dst.write(msg) ;
			// if we wrote at least 1 byte -- OR 'force' is true -- loop
			// until the entire message is consumed.
			while( (force || bytes > 0) && msg.remaining() > 0 ) {
				bytes += dst.write(msg) ;
			}
			return bytes > 0 ;
		}
		
		throw new IllegalArgumentException("Provided message is not a well-formatted DATA message.") ;
	}
	
	
	/**
	 * Retrieves the messages's DATA bytes from 'msg' and writes them to 'dst.'
	 * 
	 * In most cases, the entire message length (data bytes) will be written; 
	 * however, this method does not make such a guarantee.  It will return
	 * the number of bytes written by this call.
	 * 
	 * One possible use case:
	 * 
	 * int len = getMessageDATALength( msg ) ;
	 * int written = 0 ;
	 * while( written < len ) {
	 * 		int writtenHere = getMessageDATABytes( msg, written, dst ) ;
	 * 		written += writtenHere ;
	 * 		
	 * 		if ( writtenHere == 0 ) {
	 * 			// handle the use of a non-blocking channel with no space
	 * 			// available.  Maybe sleep for a bit to prevent a busy-spin?
	 * 		}
	 * }
	 * 
	 * This method will block if the channel is blocking, but will not
	 * 'busy spin' in any case, unlike getMessageDATA().
	 * 
	 * @param msg
	 * @param bytesWritten
	 * @param dst
	 * @throws IOException 
	 */
	protected static int getMessageDATABytes( ByteBuffer msg, int bytesWritten, WritableByteChannel dst ) throws IOException {
		if ( isDATA(msg) ) {
			int len = msg.getInt(POSITION_DATA_LEN) ;
			msg.position(POSITION_DATA + bytesWritten).limit(POSITION_DATA + len) ;
			int written = dst.write(msg) ;
			return written ;
		}
		
		throw new IllegalArgumentException("Provided message is not a well-formatted DATA message.") ;
	}
	
	
	/**
	 * Returns the number of Objects encoded in the OBJ message.
	 * @param msg
	 * @return
	 */
	protected static int getMessageOBJCount( ByteBuffer msg ) {
		if ( isOBJ(msg) ) {
			return msg.getInt(POSITION_OBJ_COUNT) ;
		}
		throw new IllegalArgumentException("Provided message is not a well-formatted OBJ message.") ;
	}
	
	/**
	 * Position this ByteBuffer message to prepare for the first object read.
	 * We do not handle object reads ourself; do that after this call.
	 * @return
	 */
	protected static void getMessageOBJ_positionForFirst( ByteBuffer msg ) {
		if ( isOBJ(msg) ) {
			msg.position(POSITION_OBJ) ;
			return ;
		}
		throw new IllegalArgumentException("Provided message is not a well-formatted OBJ message.") ;
	}
	
	/**
	 * Positions this ByteBuffer message so its bytes can be read.
	 * @param msg
	 * @return
	 */
	protected static void getMessageOBJ_BYTES_positionForRead( ByteBuffer msg ) {
		if ( isOBJ_BYTES(msg) ) {
			msg.position(POSITION_OBJ_BYTES) ;
			msg.limit( POSITION_OBJ_BYTES + msg.getInt(POSITION_OBJ_BYTES_LEN) ) ;
			return ;
		}
		throw new IllegalArgumentException("Provided message is not a well-formatted OBJ_BYTES message.") ;
	}
	
}
