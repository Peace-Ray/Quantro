package com.peaceray.quantro.communications.wrapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import com.peaceray.quantro.communications.AsynchronousReadableByteChannel;
import com.peaceray.quantro.utils.ThreadSafety;


/**
 * A WrappedSocketAutonomous performs all the WrappedSocket functions on its
 * own terms.  It keeps its own threads running, deals with read/writes itself,
 * and relies on no other class or instance to perform its basic functions (apart
 * from a class providing data to, and reading data from, the socket).
 * 
 * This class offers abstract "read from" and "write to" methods that should be 
 * implemented by subclasses, based on the specifics of the communication method
 * -- Socket, DatagramChannel, etc.
 * 
 * The advantages of an Autonomous socket should be clear: it's a simple, self-contained
 * communication channel.  This makes it ideal for clients of a game or lobby,
 * and, in a pinch, the host of such a game.
 * 
 * However, the disadvantages should be clear as well.  A server hosting the maximum
 * size lobby and game -- six and six, including themself, resp. -- will need to maintain
 * 10 different Autonomous sockets, each with 2 threads running for read/write.  That's
 * a lot of overhead for a format that would be better served with 2 or 4 threads
 * -- read and write for each of lobby and game.  A non-autonomous solution would
 * be preferable in that case.
 * 
 * 
 * @author Jake
 *
 */
public abstract class AutonomousWrappedSocket extends WrappedSocket {
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// SUBCLASSING
	//
	// This abstract class can be subclassed by overriding the following helper
	// methods (and all other public abstract methods).
	//
	// Note that 
	//
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Writes the provided byte data to the socket.  If a DatagramSocket, writes
	 * in a Datagram.
	 * 
	 * If the underlying network I/O structure does not support full duplex,
	 * it is your responsibility to make it thread-safe.  SocketWrapper only
	 * ensures that at most 1 thread is ever writing, and at most one thread
	 * is ever reading, but may allow simultaneously read/writes.
	 * 
	 * Upon return, the entire set of bytes should have been sent.
	 * 
	 * NOTE: the return value, number of bytes sent, is used only for bookkeeping.
	 * If no exception is thrown, we assume the entire message was successfully sent.
	 * 
	 * @param b A byte arra
	 * @param offset Where in 'b' to begin reading
	 * @param len How many bytes to read
	 * @return The number of bytes sent.  Note
	 * @throws IOException if the connection failed.
	 */
	protected abstract int writeToSocket( byte [] b, int offset, int len ) throws IOException ;
	
	/**
	 * Writes the provided byte data to the socket.  If a DatagramSocket, writes
	 * in a Datagram.
	 * 
	 * If the underlying network I/O structure does not support full duplex,
	 * it is your responsibility to make it thread-safe.  SocketWrapper only
	 * ensures that at most 1 thread is ever writing, and at most one thread
	 * is ever reading, but may allow simultaneously read/writes.
	 * 
	 * Upon return, the entire set of bytes should have been sent.
	 * 
	 * NOTE: the return value, number of bytes sent, is used only for bookkeeping.
	 * If no exception is thrown, we assume the entire message was successfully sent.
	 * 
	 * @param bb - the ByteBuffer of data to be sent.  All bytes from position to limit
	 * 		will be sent, once, in order.
	 * @return Whether the data was successfully sent
	 * @throws IOException if the connection failed.
	 */
	protected abstract int writeToSocket( ByteBuffer bb ) throws IOException ;
	
	/**
	 * Reads byte data from the socket; blocks until data is available, or the connection
	 * has failed.  Returns the number of bytes read into b, beginning at 'offset'.  Nice
	 * implementations will only read 'len' bytes.  Mean implementations will only read
	 * 'len' bytes, or the complete Datagram (yes, I'm looking at you, UDP connections).
	 * Reading past teh end of the Datagram is a critical error.
	 * 
	 * If the underlying network I/O structure does not support full duplex,
	 * it is your responsibility to make it thread-safe.  SocketWrapper only
	 * ensures that at most 1 thread is ever writing, and at most one thread
	 * is ever reading, but may allow simultaneously read/writes.
	 * 
	 * IF you do not have full-duplex I/O and implement locks in these methods,
	 * make sure that neither blocks inside the lock.  Blocking reads are OK if writes are
	 * still possible during that time.
	 * 
	 * POSTCONDITION: We have read a number of bytes equal to k the returned value,
	 * 		with either k <= len OR k = all bytes up to and including the last byte of
	 * 		a message, with NONE of the bytes of the next message.
	 * 
	 * @param b	A byte array to read into
	 * @param offset	Where to begin writing to 'b'
	 * @param len	How many bytes to read.  If <= 0, will read however many it wants.
	 * @return Returns 0 if the connection failed, otherwise returns the number of bytes read.
	 * @throws IOException if the connection failed.
	 */
	protected abstract int readFromSocket( byte[] b, int offset, int len ) throws IOException ;
	
	
	/**
	 * Reads byte data from the socket; blocks until data is available, or the connection
	 * has failed.  Returns the number of bytes read into b, beginning at 'offset'.  Nice
	 * implementations will only read 'bb.limit() - bb.position()' bytes.  Mean implementations will only read
	 * that many bytes, or the complete Datagram (yes, I'm looking at you, UDP connections).
	 * Reading past the end of the Datagram is a critical error.
	 * 
	 * If the underlying network I/O structure does not support full duplex,
	 * it is your responsibility to make it thread-safe.  SocketWrapper only
	 * ensures that at most 1 thread is ever writing, and at most one thread
	 * is ever reading, but may allow simultaneously read/writes.
	 * 
	 * IF you do not have full-duplex I/O and implement locks in these methods,
	 * make sure that neither blocks inside the lock.  Blocking reads are OK if writes are
	 * still possible during that time.
	 * 
	 * POSTCONDITION: We have read a number of bytes equal to k the returned value,
	 * 		with either k <= len OR k = all bytes up to and including the last byte of
	 * 		a message, with NONE of the bytes of the next message.
	 * 
	 * @param bb The ByteBuffer into which we will read.  Respects 'limit' except in
	 * 			the cases outlined above.
	 * @return Returns -1 if the connection failed, otherwise returns the number of bytes read.
	 * @throws IOException if the connection failed.
	 */
	protected abstract int readFromSocket( ByteBuffer bb ) throws IOException ;
	
	
	/**
	 * Notes that a message sent by this SocketWrapper has been acknowledged by the recipient.
	 * This message took 'numAttempts' sending attempts before it was acknowledged.
	 * 
	 * Calls to this message by the superclass are the way that subclasses provide
	 * latestPing, averagePing and successRate.
	 * 
	 * @param milliseconds The number of milliseconds elapsed between sending the message 
	 * 			and receiving the ACK.
	 * @param numAttempts The number of 'sends' this message required before we received the ACK.
	 */
	protected abstract void noteAck( long milliseconds, int numAttempts ) ;
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// WRAPPER METADATA
	//
	// Want some information on the connection?  Here goes.
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Returns the time since the last received message - either a SYN or a DATA.
	 */
	public long timeSinceLastReceived() {
		return System.currentTimeMillis() - timeLastMessageReceived ;
	}
	
	/**
	 * Returns the amount of time this socket has been waiting for an acknowledgment.
	 * A 0 means there are no pending sends, either DATAs or SYNs.
	 * @return
	 */
	public long timeWaitingForAck() {
		long maxTime = 0; 
		for ( int i = 0; i < numToSend; i++ ) {
			if ( waitingForAck[i] ) {
				maxTime = Math.max( maxTime, System.currentTimeMillis() - this.sentTime[i] ) ;
			}
		}
		return maxTime ;
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// Byte Sent / Received
	//
	// Here's how you interact with a SocketWrapper.  Data goes in, data comes
	// out, never a miscommunication.
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	@Override
	public long bytesSent() {
		return totalBytesSent ;
	}
	
	
	@Override
	public long bytesReceived() {
		return totalBytesReceived ;
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// I/O
	//
	// Here's how you interact with a SocketWrapper.  Data goes in, data comes
	// out, never a miscommunication.
	//
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Returns an InputStream to allow socket reads.  This Stream is of an
	 * implementation allowing calls to 'available'.
	 * 
	 * If subclasses have a specialized method of handling input and output,
	 * override this method.  Remember that wrapperOutgoingPOS
	 * is the method of writing to sockets, and wrapperIncomingPIS
	 * is the method of reading, so implement your I/O on top of these streams.
	 */
	public Pipe.SourceChannel getSourceChannel() {
		return wrapperIncomingSourceChannel ;
	}
	
	/**
	 * Returns an OutputStream to allow Socket writes.  Data written to the
	 * stream is guaranteed to arrive in-order at its destination, but ONLY
	 * IF the other end is also Wrapped.
	 * 
	 * @return
	 */
	public Pipe.SinkChannel getSinkChannel() {
		return wrapperOutgoingSinkChannel ;
	}
	
	@Override
	public boolean isByteAware() {
		return true ;
	}
	
	
	@Override
	public boolean isObjectAware( Object o ) {
		return false ;
	}
	
	@Override
	public boolean isObjectAware( Class<?> c ) {
		return false ;
	}
	
	@Override
	public BlockingQueue<?> getDataObjectSourceQueue() {
		throw new UnsupportedOperationException("This WrappedSocket is not object aware.") ;
	}
	
	@Override
	public BlockingQueue<?> getDataObjectSinkQueue() {
		throw new UnsupportedOperationException("This WrappedSocket is not object aware.") ;
	}
	
	@Override
	public Object getDataObjectEmptyInstance() {
		throw new UnsupportedOperationException("This WrappedSocket is not object aware.") ;
	}
	
	@Override
	public void recycleDataObjectInstance( Object o ) {
		throw new UnsupportedOperationException("This WrappedSocket is not object aware.") ;
	}
	
	@Override
	public void setDataObjectSenderReceiver( DataObjectSenderReceiver<?> receiver ) {
		throw new UnsupportedOperationException("This WrappedSocket is not object aware.") ;
	}
	
	@Override
	public void dataObjectAvailable() {
		// nothing
	}
	
	
	public void close() {
		close(false, 0) ;
	}
	
	private boolean hasClosed = false ;
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
	public void close( boolean flushOutgoing, long maximumWait ) {
		//new Exception("closing wrapped socket").printStackTrace() ;
		if ( hasClosed )
			return ;
		hasClosed = true ;
		// If we flush outgoing, close our outgoing PIS and block until outgoingThread stops.
		if ( flushOutgoing ) {
			try {
				wrapperOutgoingSinkChannel.close() ;
			} catch( IOException e ) {
				// nothing
			}
			
			// First "flush" nicely, waiting for ACKs.  Once time expires,
			// close the outgoing thread and spam the remaining messages.
			//System.out.println("waiting on outgoing thread; max wait is " + maximumWait) ;
			ThreadSafety.waitForThreadToTerminate(outgoingThread, maximumWait) ;
			outgoingThread.running = false ;
			//System.out.println("waiting for termination") ;
			ThreadSafety.waitForThreadToTerminate(outgoingThread) ;
			//System.out.println("final flush") ;
			outgoingThread.finalFlush() ;
			//System.out.println("final flush complete") ;
		}
		outgoingThread.running = false ;
		//incomingThread.running = false ;
		
		// At this moment, we should disallow any further writes;
		// close our outgoing streams.  However, we are still interested
		// in any residual data left on the socket.  The relevant thread
		// will terminate when it stops being able to read data.
		try {
			wrapperOutgoingSourceChannel.close() ;
		} catch( IOException e ) {
			// nothing
		}
		try {
			wrapperOutgoingSinkChannel.close() ;
		} catch( IOException e ) {
			// nothing 
		}
		try {
			wrapperIncomingSinkChannel.close() ;
		} catch( IOException e ) {
			// nothing
		}
		
		
		/*
		try {
			wrapperIncomingPIS.close() ;
		} catch( IOException e ) {
			// nothing 
		}
		try {
			wrapperIncomingPOS.close() ;
		} catch( IOException e ) {
			// nothing 
		}
		*/
		
		// Just in case: our outgoing thread using an AsynchronousInputStream to
		// read from wrapperOutgoingPIS.  It may be in a "waiting" state, in which case
		// the current state of wrapperOutgoingPIS (which is closed) is irrelevant, and
		// the thread will wait forever.  We need to explicitly tell it to close.
		try {
			outgoingThread.arbc.close();
		} catch( Exception e ) { } 
	}
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// ACTUAL INSTANCE FUNCTIONALITY.
	//
	///////////////////////////////////////////////////////////////////////////
	
	protected long minDelayBeforeResend = 100 ;		// Even if our ping appears very low, we wait at least this long between resends.
	protected long maxDelayBeforeResend = 3000 ;		// After 3 seconds, resend the previous message.
														// If we have low average ping, we may attempt to resend
														// sooner.
	protected long maxTimeBetweenMessages = 2000 ;	// If we haven't sent anything for 2 second, send a SYN.
	protected boolean sendSyn = true ;
	
	// These streams are the data input and output streams for the   Data written
	// to wrapperIS is sent along the socket (possibly with additional metadata that is stripped
	// by the wrapped receiver).  Data received, with metadata removed, is placed on wrapperOS.
	
	// Data for sending should be written to wrapperOutgoingSinkChannel.
	// It is piped to wrapperOutgoingSourceChannel, and
	// from there it is sent by the SocketWrapper.
	protected Pipe.SourceChannel wrapperOutgoingSourceChannel ;
	protected Pipe.SinkChannel wrapperOutgoingSinkChannel ;
	
	// Data read from the socket is written by SocketWrapper to wrapperIncomingSinkChannel,
	// and should be read by users from wrapperIncomingSourceChannel.
	protected Pipe.SinkChannel wrapperIncomingSinkChannel ;
	protected Pipe.SourceChannel wrapperIncomingSourceChannel ;
	
	
	private SocketWrapperOutgoingThread outgoingThread ;
	private SocketWrapperIncomingThread incomingThread ;
	
	// The number of messages (including Pings) which we will send while waiting for 
	// acknowledgements of previous messages.  Setting this to 1 will force a strict
	// DATA->ACK->DATA->ACK... cycle, with a ACK for the previous message required before
	// the next is sent.  If set to k > 1 will allow up to k total messages to be sent
	// w/out acknowledgment, although sends will be blocked until the earliest of those
	// messages receives an ACK.  This allows the guarantee of ordered delivery to only
	// minimally impact efficiency.
	// sendAhead must be <= 128.
	private int numToSend ;
	private boolean resend ;
	private int maxTimeWaitingForAck ;
	private boolean waitForAck ;
	
	Object writeMutex ;		// Both threads write (one DATAs and SYNs, the other ACKs).  Synchronize their writes.
	Object metaMutex ;		// Be sure to synchronize any access to these structures!
	private long lastMessageTimeSent ;
	private long lastMessageSentNumber ;
	private int numWaitingForAck ;
	private boolean [] waitingForAck ;

	private long [] sentTime ;
	private int [] numSends ;
	private ByteBuffer [] messageBytes ;		// Only stored if 'resend' is true.
	private long [] messageNumber ;
	private int [] totalMessageLength ;
	
	private long timeLastMessageReceived ;
	
	private int outgoingReadNeedsMessageAckIndex = -1 ;
		// If positive, we are waiting to start an outgoing read based on
		// receiving an ACK for the specified message.
	
	private int outputID = new Random().nextInt(100000) ;
	
	
	private long totalBytesSent ;
	private long totalBytesReceived ;
	
	
	/**
	 * Constructs a SocketWrapper.  As this is an abstract class, this constructor should be called 
	 * by a subclass's constructor.
	 * 
	 * @param numToSend The number of messages this WrappedSocket will send "in advance" of receiving
	 * 			acknowledgement of the first.
	 * @param resend Whether messages should be resent if it seems like we've waited too long for acknowledgement.
	 * @param maxTimeWaitingForAck If we spend longer than this many milliseconds waiting for an acknowledgement
	 * 			for a message we've sent, call the connection Broken.  You may want to implement your own
	 * 			stricter timeout in "readFromSocket" to kick in if we receive NO information at all for
	 * 			a while.
	 * 		  If negative, we do not wait for ACK messages at all; ACKs are ignored.
	 */
	protected AutonomousWrappedSocket( int numToSend, boolean resend, int maxTimeWaitingForAck ) throws IOException {
		
		assert( numToSend >= 1 && numToSend <= 256 ) ;
		
		writeMutex = new Object() ;
		metaMutex = new Object() ;
		
		this.resend = resend ;
		this.numToSend = numToSend ;
		this.maxTimeWaitingForAck = maxTimeWaitingForAck ;
		this.waitForAck = maxTimeWaitingForAck > 0 ;
		
		// Allocate!
		lastMessageTimeSent = System.currentTimeMillis() ;
		lastMessageSentNumber = -1 ;
		numWaitingForAck = 0 ;
		waitingForAck = new boolean[numToSend] ;
		sentTime = new long[numToSend] ;
		numSends = new int[numToSend] ;
		messageNumber = new long[numToSend] ;
		// if ( resend )
		messageBytes = new ByteBuffer[numToSend] ;
		for ( int i = 0; i < numToSend; i++ )
			messageBytes[i] = ByteBuffer.allocate(MAX_MESSAGE_LENGTH) ;
		// else
			// messageBytes = new byte[1][MAX_MESSAGE_LENGTH] ;	// Used 
		totalMessageLength = new int[numToSend] ;
		
		Pipe p_outgoing = Pipe.open() ;
		wrapperOutgoingSourceChannel = p_outgoing.source() ;
		wrapperOutgoingSinkChannel = p_outgoing.sink() ;
		
		Pipe p_incoming = Pipe.open() ;
		wrapperIncomingSinkChannel = p_incoming.sink() ;
		wrapperIncomingSourceChannel = p_incoming.source() ;
		
		
		
		outgoingThread = new SocketWrapperOutgoingThread(  ) ;
		incomingThread = new SocketWrapperIncomingThread(  ) ;
		
		timeLastMessageReceived = System.currentTimeMillis() ;
		
		totalBytesSent = 0 ;
		totalBytesReceived = 0 ;
	}
	
	protected void setMaxTimeBetweenMessages( long timeBetween ) {
		this.maxTimeBetweenMessages = timeBetween ;
		this.sendSyn = timeBetween > 0 ;
	}
	
	void start() {
		outgoingThread.start() ;
		incomingThread.start() ;
	}
	
	
	private class SocketWrapperOutgoingThread extends Thread implements AsynchronousReadableByteChannel.Delegate {
		
		boolean running ;
		
		public AsynchronousReadableByteChannel arbc ;
		
		long messageNum ;
		
		ByteBuffer nextOutgoingBB ;
		boolean readingOutgoingMessage ;
		
		public SocketWrapperOutgoingThread( ) {
			running = true ;
			
			messageNum = 0 ;
			
			nextOutgoingBB = ByteBuffer.allocate(MAX_MESSAGE_LENGTH) ;
			readingOutgoingMessage = false ;
		}
		
		/**
		 * An emergency method: flushes our entire outgoing pipe, ignoring acknowledgements.
		 * Can only be called after the thread is terminated.
		 */
		public void finalFlush() {
			if ( running || isAlive() )
				throw new IllegalStateException("can't flush when still running!") ;
			
			//System.out.println("WrappedSocket: outgoing flush beginning") ;
			
			try {
				arbc.stop() ;		// don't call close(), that would close the underlying input stream.
			} catch( Exception e ) { } 		// ignore.
			try {
				wrapperOutgoingSinkChannel.close();
			} catch( Exception e ) { }
			
			// We may have a read ongoing.  If so, because we have closed the other
			// end of the stream, it will shortly finish.  Wait for this.
			//System.err.println("joining arbc") ;
			//new Exception("what what what?").printStackTrace() ;
			arbc.join( 100 ) ;	// 1/10th second
			if ( arbc.alive() ) {
				return ;
			}
			//System.err.println("after join arbc") ;
			
			try {
				// read to the end of the outgoing source channel.
				while ( true ) {
					ByteBuffer bb = messageBytes[0] ;
					
					long nextMessageNum = lastMessageSentNumber == Long.MAX_VALUE
							?	((lastMessageSentNumber % numToSend) + 1) % numToSend
							:	lastMessageSentNumber + 1 ;
					
					boolean msgOK = setAsDATA( bb, nextMessageNum, wrapperOutgoingSourceChannel ) ;
					if ( !msgOK ) {
						break ;			// end of stream
					}
					
					lastMessageTimeSent = System.currentTimeMillis() ;
					lastMessageSentNumber = nextMessageNum ;
					
					// send it!
					//System.err.println("WrappedSocket: flushing length " + bb.remaining()) ;
					totalBytesSent += writeToSocket( bb ) ;
				}
			} catch ( IOException e ) {
				// nothing
			}
			//System.err.println("WrappedSocket: flush complete") ;
		}
		
		@Override
		public void run() {
			for ( int i = 0; i < waitingForAck.length; i++ )
				waitingForAck[i] = false ;
			//System.out.println("WrappedSocket starting an async input stream") ;
			arbc = new AsynchronousReadableByteChannel( wrapperOutgoingSourceChannel ) ;
			//System.out.println("WrappedSocket started an async input stream") ;
			// Start off by starting a send.
			synchronized ( metaMutex ) {
				try {
					startReadForOutgoingMessage() ;
				} catch (Exception e) {
					System.err.println("WrappedSocket.run fatal error: can't startReadForOutgoingMessage") ;
					running = false ;
				}
			}
			while ( running ) {
				
				// This loop handles resends and SYN messages.  We also do a basic check:
				// if there is no "next outgoing" message but there IS an open slot for
				// a message, i.e., we are not waiting for ACK on messageNum, then start
				// another read.  We can enter this condition when:
				// 1. All message bytes are currently in-use (i.e., the oldest
				//			is still waiting for ACK).
				// 2. The "next outgoing" message asynchronously got data, and wrote it;
				//			we could not start another read, because all message bytes
				//			were in-use.
				// 3. We received at least one ACK, freeing up message bytes.
				
				// This loop is pretty simple.  First, see whether we've entered the state
				// described above.  If so, start another read.  Then examine the current
				// state of things, and determine the MINIMUM amount of time before we'll
				// need to send data (either a SYN or a resend).  If that amount of time
				// is non-positive, actually send the data (and recalculate).  Otherwise,
				// sleep for that long.
				
				long minTimeInMillis = maxTimeBetweenMessages ;
				//System.err.println("SocketWrapperOutgoingThread.run entering synchronized") ;
				synchronized( metaMutex ) {
					//System.err.println("SocketWrapperOutgoingThread.run within synchronized") ;
					
					if ( !running )
						break ;
					
					// Now calculate the MINIMUM TIME before we need to send data.
					// We need to send data in two cases: once the most recent message
					// is more than "max time between messages" old, we send a SYN.
					// If we are resending, and there is a message which has not been
					// acknowledged, it will be resent in <timestep calculation>.
					// Take the minimum of these as the time-before-we-send something.
					// If either of these is <= 0, send IMMEDIATELY (and include the REVISED
					// version in our calculations; remember, we are calculating the
					// time to sleep!).
					//
					// NOTE: here in this check, we also include the time before we require
					// an acknowledgement, which is INF if there are no messages waiting for ACK,
					// and maxTimeWaitingForAck - (currentTime - sentTime) if there are.  If this
					// value is negative, we have broken due to a timeout and should close up shop.
					
					// First: calculate time to resend.
					if ( resend ) {
						long avgPing = averagePing() ;
						long timestep = avgPing >= 0 	? Math.min(2 * avgPing, maxDelayBeforeResend)
														: maxDelayBeforeResend ;
						timestep = Math.max( timestep, minDelayBeforeResend) ;
						for ( int i = 0; i < numToSend; i++ ) {
							// If we are waiting for an ACK, check the time before resend.
							if ( waitingForAck[i] && System.currentTimeMillis() - sentTime[i] > timestep * numSends[i] ) {
								// resend this!
								numSends[i]++ ;
								ByteBuffer bb = messageBytes[i] ;
								int len = totalMessageLength[i] ;
								bb.position(0).limit(len).mark() ;
								// System.out.println("WrappedSocket: resending message " + messageNumber[i] + ": " + numSends[i] + " total sends in " + ((System.currentTimeMillis() - sentTime[i]) / 1000.0) + " seconds") ;
								
								try {
									lastMessageTimeSent = System.currentTimeMillis() ;
									synchronized(writeMutex) {
										bb.reset() ;
										totalBytesSent += writeToSocket(bb) ;
									}
								} catch (IOException e) {
									//e.printStackTrace() ;
									running = false ;
									break ;
								}
							}
							// Time until send?
							if ( waitingForAck[i] ) {
								long timeTillResend = (timestep * numSends[i]) - (System.currentTimeMillis() - sentTime[i]) ;
								minTimeInMillis = Math.min(timeTillResend, minTimeInMillis) ;
							}
						}
					}
					// Next: calculate time to socket timeout based on lack-of-Ack
					
					int timeBeforeTimeout = Integer.MAX_VALUE ;
					if ( waitForAck ) {
						for ( int i = 0; i < numToSend; i++ ) {
							// If we are waiting for an ACK, find the total time waiting, subtract
							// this from the maximum wait time.
							if ( waitingForAck[i] ) {
								int timeWaiting = (int)(System.currentTimeMillis() - sentTime[i]) ;
								int timeRemaining = maxTimeWaitingForAck - timeWaiting ;
								
								timeBeforeTimeout = Math.min(timeRemaining, timeBeforeTimeout) ;
								//System.err.println("waiting " + messageNumber[i] + " time left " + timeBeforeTimeout) ;
							}
						}
					}

					// Shut down if negative.
					if ( timeBeforeTimeout <= 0 ) {
						System.err.println("WrappedSocket: too much time without message acknowledgement") ;
						running = false ;
					}
					
					// minTimInMillis is now the minimum time before a required
					// message resend.  What about the minimum time before a SYN message?
					long timeBeforeSyn = maxTimeBetweenMessages - (System.currentTimeMillis() - lastMessageTimeSent) ;
					if ( sendSyn && timeBeforeSyn <= 0 && outgoingReadNeedsMessageAckIndex < 0 ) {
						// make and send a SYN.
						int messageIndex = (int)(messageNum % numToSend) ;
						if ( !waitingForAck[messageIndex]) {
							// send it.
							ByteBuffer bb = messageBytes[messageIndex] ;
							setAsSYN( bb, messageNum ) ;
							
							// System.out.println("WrappedSocket: sending SYN as message " + messageNum) ;
							if ( waitForAck ) {
								numWaitingForAck++ ;
								waitingForAck[messageIndex] = true ;
							} else {
								waitingForAck[messageIndex] = false ;
							}
							numSends[messageIndex] = 1 ;
							sentTime[messageIndex] = System.currentTimeMillis() ;
							messageNumber[messageIndex] = messageNum ;
							totalMessageLength[messageIndex] = 9 ;
							messageNum++ ;
							try {
								//System.err.println("sending SYN " + (messageNum-1) + " " + new Date()) ;
								synchronized( writeMutex ) {
									totalBytesSent += writeToSocket(bb) ;
								}
							} catch (IOException e) {
								//e.printStackTrace() ;
								running = false ;
								break ;
							}
								
						} else {
							//System.err.println("NOT sending SYN: waitingForAck from " + messageIndex) ;
						}
						lastMessageTimeSent = System.currentTimeMillis() ;
						timeBeforeSyn = maxTimeBetweenMessages - (System.currentTimeMillis() - lastMessageTimeSent) ;
					} else {
						//System.err.println("NOT sending SYN: timeBeforeSyn is " + timeBeforeSyn) ;
					}
						
					minTimeInMillis = Math.min(minTimeInMillis, timeBeforeTimeout) ;
					minTimeInMillis = Math.min(minTimeInMillis, timeBeforeSyn) ;
					minTimeInMillis = Math.max(minTimeInMillis, 1) ;
					// This the time to sleep.  Sleep OUTSIDE of the synchronization.
					
					//System.err.println("SocketWrapperOutgoingThread.run leaving synchronized") ;
				}
				
				if ( !running )
					break ;
				
				try {
					Thread.sleep( minTimeInMillis ) ;
				} catch( InterruptedException e ) {
					// This is one way of terminating the loop...
					//e.printStackTrace() ;
					//System.out.println("" + outputID + "WrappedSocket: interrupted") ;
					running = false ;
					break ;
				}
			}
			
			// If we get here, either our attempt to read from the InputStream has failed
			// (and running set to false) or an attempt to write to the socket failed.
			// Either way, this connection is done.  Note that in that case we CANNOT
			// flush outgoing data, so we don't bother trying.  Close immediately.
			close() ;
			//System.out.println("" + outputID + "WrappedSocket: outgoing thread terminated") ;
		}
		
		
		/**
		 * Starts preparing an outgoing message using the provided message number.
		 * Once started, if everything goes well, a message with the provided number
		 * will eventually be sent out.
		 * @param msgNum
		 * @return Whether we have successfully started a read for an outgoing message.
		 */
		public boolean startReadForOutgoingMessage(  ) {
			//System.out.println("start read for outgoing...") ;
			//System.err.println("SocketWrapperOutgoingThread.startReadForOutgoingMessage entering synchronized") ;
			synchronized( metaMutex ) {
				//System.err.println("SocketWrapperOutgoingThread.startReadForOutgoingMessage within synchronized") ;
				if ( readingOutgoingMessage ) {
					running = false ;
					this.interrupt() ;
					throw new IllegalStateException("Can't start an outgoing read if already reading an outgoing message") ;
				}
				
				outgoingReadNeedsMessageAckIndex = -1 ;
				
				// DATA, length, etc. will go here.
				nextOutgoingBB.position(0).mark().limit(MAX_MESSAGE_LENGTH-13) ;
				readingOutgoingMessage = true ;
				arbc.read(nextOutgoingBB, this) ;
				//System.err.println("SocketWrapperOutgoingThread.startReadForOutgoingMessage leaving synchronized") ;
				
				return true ;
			}
		}
		
		/**
		 * The InputStreamReader did read data into the provided byte array,
		 * beginning at offset, with length len.  'len' is the value returned
		 * by the call to 'read', and should be examined by the callee for the
		 * possibility of an error.
		 * 
		 * The InputStreamReader will not attempt another read until (at least)
		 * this method returns.
		 */
		public void arbc_didRead( ByteBuffer bb, int len ) {
			// If len is -1, error.  Stop everything.
			// If len is 0, do nothing.
			// If len is >1, send off the message.
			if ( len < 0 ) {
				//System.err.println("WrappedSocket.SocketWrapperOutgoingThread.arbc_didRead has len < 0") ;
				running = false ;
				this.interrupt() ;
				return ;
			}
			
			// We have already set up the prefix for this byte array,
			// except for the length.  Set the length and send the data off.
			// Send the data off.
			//System.err.println("SocketWrapperOutgoingThread.arbc_didRead entering synchronized") ;
			synchronized( metaMutex ) {
				readingOutgoingMessage = false ;
				
				//System.err.println("SocketWrapperOutgoingThread.arbc_didRead within synchronized") ;
				bb.flip().position(0).limit(len) ;
				// what is our message number?
				int messageIndex = (int)(messageNum % numToSend) ;
				outgoingReadNeedsMessageAckIndex = messageIndex ;
				if ( !waitingForAck[messageIndex] )
					sendOutgoingMessageNow() ;
			}
			//System.err.println("SocketWrapperOutgoingThread.arbc_didRead leaving synchronized") ;
		}
		
		
		/**
		 * Sends the current outgoing message, stored in 'nextOutgoingBB'
		 * which is already configured with length and position.
		 * 
		 * PRECONDITION: the current message index is available for an immediate send.
		 * 				'outgoingReadNeedsMessageAckIndex' == messageNum % numToSend
		 * 				and that index is NOT awaiting an acknowledgement.
		 * 
		 * POSTCONDITION: If no exception was thrown, we have written the message and started
		 * 				another asynchronous read.
		 */
		private void sendOutgoingMessageNow() {
			synchronized ( metaMutex ) {
				int messageIndex = (int)(messageNum % numToSend) ;
				if ( messageIndex != outgoingReadNeedsMessageAckIndex )
					throw new IllegalStateException("Message number does not match outgoing message ack index.") ;
				
				outgoingReadNeedsMessageAckIndex = -1 ;
				
				// copy to the byte buffer.
				ByteBuffer msgBB = messageBytes[messageIndex] ;
				setAsDATA( msgBB, messageNum, nextOutgoingBB ) ;
				
				lastMessageTimeSent = System.currentTimeMillis() ;
				if ( waitForAck ) {
					numWaitingForAck++ ;
					waitingForAck[messageIndex] = true ;
				} else {
					waitingForAck[messageIndex] = false ;
				}
				sentTime[messageIndex] = lastMessageTimeSent ;
				numSends[messageIndex] = 1 ;
				messageNumber[messageIndex] = messageNum ;
				totalMessageLength[messageIndex] = msgBB.remaining() ;
				messageNum++ ;
				
				// Send it
				try {
					synchronized (writeMutex) {
						//System.err.println("Writing message " + (messageNum-1) + " has length " + len) ;
						totalBytesSent += writeToSocket( msgBB ) ;
					}
				} catch( IOException e ) {
					System.err.println("IOException when writing arbc-read message") ;
					//e.printStackTrace() ;
					running = false ;
					this.interrupt() ;
					return ;
				}
				
				// Try reading more!
				if ( running )
					startReadForOutgoingMessage( ) ;
			}
		}
		
		
		/**
		 * An IO Exception was thrown while reading from the stream.  The
		 * exception is provided.  The InputStreamReader will terminate after
		 * this call.
		 * 
		 * @param e
		 */
		public void arbc_didThrowException( Exception e ) {
			// Something went wrong.
			//System.err.println("arbc_didThrowException") ;
			//e.printStackTrace() ;
			running = false ;
			this.interrupt() ;
		}
	}
	
	
	private class SocketWrapperIncomingThread extends Thread {
		
		boolean running ;
		
		
		
		public SocketWrapperIncomingThread() {
			running = true ;
		}
		
		@Override
		public void run() {
			
			// We allocate some structures up-front, swapping them
			// around as needed.  'bb' is where we write an incoming message,
			// then we place it in the appropriate location in b_queue.
			ByteBuffer bb = ByteBuffer.allocate(MAX_MESSAGE_LENGTH);
			ArrayList<ByteBuffer> bb_queue = new ArrayList<ByteBuffer>() ;
			for ( int i = 0; i < numToSend; i++ )
				bb_queue.add( ByteBuffer.allocate(MAX_MESSAGE_LENGTH) ) ;
			
			// The last consecutive message number we received.
			long lastReceivedInSequenceMessageNum = -1 ;
			ByteBuffer bb_ack = ByteBuffer.allocate(9);
			
			while ( running ) {
				//System.out.println("" + outputID + "Top of incoming loop") ;
				// We monitor the incoming messages for three types of content:
				// DATA are incoming messages with content we pass up to the user.
				// SYN are synchronization ("ping") messages that should be invisible to the user
				// ACK are indications that a message we sent was received by the other side.
				
				// For DATA and SYN receives, we respond with an ACK providing the same message number.
				// Our main concern is ensuring that we don't repeatedly report the same DATA
				// if it comes in more than once (e.g., if our ACK response isn't received and
				// the other party resends).
				// To prevent this, we perform two actions: we recall the number
				// of the last message received (we send consecutive message numbers), and 
				// store any messages that come in with numbers > that number+1.  Remember that
				// the other side can send a maximum of 128 messages before receiving an acknowledgement
				// of the first.  When message number+1 finally comes in, we sequentially
				// write all consecutive messages to output until we hit another missing
				// message.
				
				// We only need to lock when performing I/O and when accepting ACK messages.
				// However, because the other thread ONLY writes, and Java Sockets are full
				// duplex, we don't lock calls to 
				
				// Look for incoming messages.  All messages are at least 9 bytes - type
				// and message number.  If a DATA, there are at least four bytes providing
				// the length of the message.  We read the whole thing into 'b' then decide
				// what to do with it.
				
				// Read an entire message.
				try {
					int bytesRead = 0 ;
					// Read 1
					//System.out.println("" + outputID + " blocking on first read") ;
					bb.position(0) ;
					bb.limit(1) ;
 					bytesRead += readFromSocket(bb) ;
 					totalBytesReceived += bytesRead ;
					if ( bytesRead < 0 )
						throw new IOException("" + outputID + "end of wrapped socket stream") ;
					//System.out.println("SocketWrapper: " + bytesRead + " bytes read") ;
					// Discard if not a wrapped message
					if ( bb.get(0) != DATA && bb.get(0) != SYN && bb.get(0) != ACK ) {
						System.err.println("DROPPING READ BYTE") ;
						continue ;
					}
					if ( AutonomousWrappedSocket.this instanceof AutonomousWrappedUDPSocket && bb.get(0) == DATA && bytesRead < 13 ) {
						// very very bad.  I don't know the cause of this, so here is a stopgap.
						System.err.println("Dropping incomplete UDP read") ;
						continue ;
					}
					
					while ( bytesRead < 9 ) {
						//System.out.println("SocketWrapper: reading more...") ;
						bb.position(bytesRead).limit(9) ;
						int b_read = readFromSocket( bb ) ;
						if ( b_read < 0 )
							throw new IOException("" + outputID + "end of wrapped socket stream") ;
						bytesRead += b_read ;
						totalBytesReceived += b_read ;
					}
					//System.out.println("" + outputID + "SocketWrapper: at least 9 bytes.") ;
					//if ( bb.get(0) == DATA )
					//	System.out.println("" + outputID + "SocketWrapper: receiving DATA " + bb.getLong(1)) ;
					//else if ( bb.get(0) == SYN )
					//	System.out.println("" + outputID + "SocketWrapper: receiving SYN " + bb.getLong(1)) ;
					
					// We might need to read the next four and beyond.
					if ( bb.get(0) == DATA ) {
						// Read to the length
						while ( bytesRead < 13 ) {
							bb.position(bytesRead).limit(13) ;
							int b_read = readFromSocket( bb ) ;
							if ( b_read < 0 )
								throw new IOException("" + outputID + "end of wrapped socket stream") ;
							bytesRead += b_read ;
							totalBytesReceived += b_read ;
						}
						//System.out.println("" + outputID + "SocketWrapper: read at least 13") ;
						//System.out.println("reading message number " + bb.getLong(1) ) ;
						
						// Read the content
						int contentLength = bb.getInt(9) ;
						//System.err.println("content length is " + contentLength) ;
						while ( bytesRead < contentLength + 13 ) {
							bb.position(bytesRead).limit(contentLength + 13) ;
							int b_read = readFromSocket( bb ) ;
							if ( b_read < 0 )
								throw new IOException("" + outputID + "end of wrapped socket stream") ;
							bytesRead += b_read ;
							totalBytesReceived += b_read ;
						}
					}
					//System.out.println("" + outputID + "SocketWrapper: all message bytes read") ;
					
					// That's it: we have read the entire message into b.
				} catch( IOException e ) {
					//close(  ) ;
					try {
						//e.printStackTrace() ;
						close() ;		// we are receiving no more data.
					} catch( Exception e2 ) { } ;
					//System.out.println("" + outputID + "WrappedSocket: incoming thread terminated") ;
					return ;
				}
				
				long receivedMessageNumber = bb.getLong(1) ;
				//System.out.println("SocketWrapper: received message " + receivedMessageNumber + " type " + bb.get(0)) ;
				
				// Now that we have the message, decide what to do with it.
				// If an ACK message, update our metadata and loop.  Otherwise
				// we have more processing to do.
				if ( bb.get(0) == ACK ) {
					//System.err.println("SocketWrapperIncomingThread.run entering synchronized (ACK)") ;
					if ( waitForAck && receivedMessageNumber >= 0 ) {
						synchronized( metaMutex ) {
							//System.err.println("SocketWrapperIncomingThread.run within synchronized (ACK " + receivedMessageNumber + ")") ;
							//System.err.println("(ACK " + receivedMessageNumber + ")") ;
							int messageIndex = (int)(receivedMessageNumber % numToSend) ;
							if ( messageNumber[messageIndex] == receivedMessageNumber && waitingForAck[messageIndex] ) {
								numWaitingForAck-- ;
								waitingForAck[messageIndex] = false ;
								
								// Note the acknowledgment
								noteAck( System.currentTimeMillis() - sentTime[messageIndex], numSends[messageIndex] ) ;
								
								// can we use this message index to send a waiting message?
								if ( outgoingReadNeedsMessageAckIndex == messageIndex ) {
									// time to send the current outgoing message and start a new
									// read.
									AutonomousWrappedSocket.this.outgoingThread.sendOutgoingMessageNow() ;
								}
							}
						}
					}
					//System.err.println("SocketWrapperIncomingThread.run leaving synchronized (ACK)") ;
				}
				
				// Otherwise it is a DATA or a SYN.  Either way this goes to our message queue.
				else {
					// It's not an ACK message; send an ACK in response.
					timeLastMessageReceived = System.currentTimeMillis() ;
					setAsACK( bb_ack, receivedMessageNumber ) ;
					
					try {
						//System.err.println("SocketWrapperIncomingThread.run entering synchronized (DATA or SYN " + receivedMessageNumber + ")") ;
						synchronized( writeMutex ) {
							//System.err.println("SocketWrapperIncomingThread.run within synchronized (DATA or SYN " + receivedMessageNumber + ")") ;
							bb_ack.position(0).limit( 9 ) ;
							totalBytesSent += writeToSocket(bb_ack) ;
						}
						//System.err.println("SocketWrapperIncomingThread.run leaving synchronized (DATA or SYN)") ;
						//System.out.println("" + outputID + "written ack " + receivedMessageNumber) ;
					} catch( IOException e ) {
						//e.printStackTrace() ;
						// We do NOT fail if we can't write acknowledgements.  We want to keep
						// reading input, if there is any available.
					}
										
					// Now put this in our queue of messages.
					int queueOffset = (int)(receivedMessageNumber - lastReceivedInSequenceMessageNum - 1) ;
					// If the received message is 1 greater than the last in sequence,
					// then we index into b_queue with 0.
					// If we have room in the queue, swap b with the current entry.
					// Otherwise, allocate and add.
					if ( queueOffset > 255 ) {
						// IT TRIES TO TRICKS US!  WE WON'T ALLOCATE THAT CRAP FOR YOU!
						close(  ) ;
						System.out.println("WrappedSocket: incoming thread terminated due to overly long queue offset") ;
						return ;
					}
					
					if ( queueOffset < 0 ) {
						// We received a resend of a message we've already
						// processed.  We've already sent the ACK (above), so 
						// drop it on the floor.
						// System.out.println("WrappedSocket: resending ACK for re-received message " +receivedMessageNumber) ;
						continue ;
					}
					
					while ( queueOffset >= bb_queue.size() )
						bb_queue.add( ByteBuffer.allocate(MAX_MESSAGE_LENGTH) ) ;
					ByteBuffer bb_temp = bb_queue.get(queueOffset) ;
					bb_queue.set(queueOffset, bb) ;
					bb = bb_temp ;
					
					// Okay, try processing messages.
					long msgNum = receivedMessageNumber ;
					ByteBuffer bb_process = bb_queue.get(0) ;
					while( lastReceivedInSequenceMessageNum + 1 == msgNum ) {
						//System.out.println("" + outputID + "loop") ;
						// The message at the front of the queue is the next message in
						// the sequence.
						if ( isDATA( bb_process ) ) {
							// Put data on the stream/channel
							try {
								getMessageDATA( bb_process, true, wrapperIncomingSinkChannel ) ;
							} catch ( IOException e ) {
								close(  ) ;
								//System.out.println("" + outputID + "WrappedSocket: incoming thread terminated") ;
								return ;
							}
						}
						
						// Advance our queue by one.
						bb_queue.remove(0) ;
						bb_queue.add(bb_process) ;	// put it on the end.
						bb_process = bb_queue.get(0) ;
						
						// Update msg num.
						lastReceivedInSequenceMessageNum = msgNum ;
						msgNum = bb_process.getLong(1) ;
					}
				}
			}
			// flush this out.
			try {
				wrapperIncomingSourceChannel.close();
			} catch (IOException e) {
				// ignore
			}
			
			//System.out.println("WrappedSocket: incoming thread terminated") ;
			
		}
	}

}
