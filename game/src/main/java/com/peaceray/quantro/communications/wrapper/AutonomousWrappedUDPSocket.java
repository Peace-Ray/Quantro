package com.peaceray.quantro.communications.wrapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import com.peaceray.quantro.utils.ByteArrayOps;

/**
 * UDPSocketWrapper wraps a DatagramSocket.  The main purpose here
 * is to impose guaranteed and ordered delivery on a UDP connection.
 * Why not just use TCP?  Because UDP hole-punching is significantly
 * easier than TCP hole-punching.  Implementing this wrapper is
 * a way to avoid having to use TCP hole-punching for direct
 * communication.
 * 
 * In practice, perform your UPD hole-punching step, then wrap the
 * connection.
 * 
 * @author Jake
 *
 */
class AutonomousWrappedUDPSocket extends AutonomousWrappedSocket {
	
	public static final int IDLE_TIMEOUT = 8000 ;		// 5 seconds w/o any data = timeout
														// NOTE: this is an entirely local check, in 
														// our socket receive method; if we go this long
														// without receiving any prefix-labeled information,
														// we throw an exception to close the socket.
														// Compare this behavior to maxTimeWaitingForAck behavior,
														// which doesn't care about the rate data is coming in,
														// and is concerned only with the ACK response to the
														// oldest sent message.
	
	DatagramSocket dsock ;
	SocketAddress destAddr ;
	
	DatagramPacket dp_incoming ;
	DatagramPacket dp_outgoing ;
	
	byte [] writeArray ;
	byte [] readArray ;
	
	byte [] prefix ;
	
	// Meta.
	private long lastPing ;
	private long averagePing ;
	private static final double NEW_PING_WEIGHT = 0.2 ;
	private double strength ;
	
	// Prefix information.  We get our prefix from the bytes used to do UDP pass-through;
	// that way we ensure that both parties use the same prefix.
	
	AutonomousWrappedUDPSocket( DatagramSocket listensock, SocketAddress destinationAddress, byte [] prefix ) throws IOException {
		// Datagrams do not have guaranteed or ordered delivery.
		// We DO resend, but we don't want to get too ahead of ourselves.
		// At present writing, max message length is 128, of which 13 is metadata
		// for a MSG.  For a big message, each chunk contains 115 bytes, meaning
		// a 64 message send-ahead allows 7.2k of data to be sent before we start
		// blocking for acknowledgment.  Hopefully this would eat up a chunk of a full
		// synchronization.
		super( 64, true, 10000 ) ;		// timeout if 10 seconds w/o ACK
		
		this.dsock = listensock ;
		this.destAddr = destinationAddress ;
		
		if ( prefix == null )
			this.prefix = new byte[0] ;
		else {
			this.prefix = new byte[prefix.length] ;
			for ( int i = 0; i < prefix.length; i++ )
				this.prefix[i] = prefix[i] ;
		}
		
		// we want the next largest power-of-two that can include the prefix bytes.
		int msgLength = 2 ;
		while ( msgLength < this.prefix.length + MAX_MESSAGE_LENGTH )
			msgLength *= 2 ;
		
		dp_incoming = new DatagramPacket( new byte[msgLength],
											msgLength ) ;
		dp_outgoing = new DatagramPacket( new byte[msgLength],
											msgLength,
											destinationAddress ) ;
		
		
		lastPing = -1 ;
		averagePing = -1 ;
		strength = 1.0 ;
	}

	@Override
	protected int writeToSocket(byte[] b, int offset, int len)
			throws IOException {
		
		// Calls to this method are always synchronized, so
		// we don't need to worry about accidentally overwriting dp_outgoing
		// while we write.  Copy the buffer provided into dp_outgoing and send.
		byte [] buff = dp_outgoing.getData() ;
		for ( int i = 0; i < prefix.length; i++ )
			buff[i] = prefix[i] ;
		for ( int i = 0; i < len; i++ )
			buff[i+prefix.length] = b[offset+i] ;
		dp_outgoing.setData(buff, 0, len+prefix.length) ;

		if ( b[offset] == DATA && len == 9 )
			System.err.println("WrappedUDPSocket.  SIGNIFICANT PROBLEM: sent a 9-byte message.  Offset is " + offset + ", Number is " + ByteArrayOps.readLongAsBytes(b, offset)) ;
		
		/*
		if ( b[offset] == MSG )
			System.err.println("WrappedUDPSocket.  Writing MSG number " + ByteArrayOps.readLongAsBytes(b, offset+1) + " with length " + len) ;
		if ( b[offset] == SYN )
			System.err.println("WrappedUDPSocket.  Writing SYN number " + ByteArrayOps.readLongAsBytes(b, offset+1) + " with length " + len) ;
		if ( b[offset] == ACK )
			System.err.println("WrappedUDPSocket.  Writing ACK number " + ByteArrayOps.readLongAsBytes(b, offset+1) + " with length " + len) ;
		*/
		
		// Send
		dsock.send(dp_outgoing) ;
		
		return len+prefix.length ;
	}
	
	/**
	 * Writes the provided byte data to the socket.  If a DatagramSocket, writes
	 * in a Datagram.
	 * 
	 * If the underlying network I/O structure does not support full duplex,
	 * it is your responsibility to make it thread-safe.  SocketWrapper only
	 * ensures that at most 1 thread is ever writing, and at most one thread
	 * is ever reading, but may allow simultaneously read/writes.
	 * 
	 * @param bb - the ByteBuffer of data to be sent.  All bytes from position to limit
	 * 		will be sent, once, in order.
	 * @return Whether the data was successfully sent
	 * @throws IOException if the connection failed.
	 */
	protected int writeToSocket( ByteBuffer bb ) throws IOException {
		if ( writeArray == null || writeArray.length < bb.capacity() )
			writeArray = new byte[bb.capacity()] ;
		
		int len = bb.remaining() ;
		bb.get( writeArray, 0, len ) ;
		
		return writeToSocket( writeArray, 0, len ) ;
	}

	@Override
	protected int readFromSocket(byte[] b, int offset, int lenWillBeIgnored)
			throws IOException {
		
		long startTime = System.currentTimeMillis() ;
		int timeRemaining ;
		while ( true ) {
			timeRemaining = (int)(IDLE_TIMEOUT - (System.currentTimeMillis() - startTime)) ;
			dsock.setSoTimeout(timeRemaining) ;
			
			try {
				dsock.receive( dp_incoming ) ;
			} catch( SocketTimeoutException e ) {
				// Whelp, looks like we've been idle for 5 seconds without receiving
				// any information with the prefix we want.  Shut Down Everything.
				throw new IOException("Went 5 seconds without reading any data from the other side.") ;
			}
	 		
			// Check the prefix
			byte[] buff = dp_incoming.getData();
			int length = dp_incoming.getLength();
			int incomingOffset = dp_incoming.getOffset();
			if ( length < prefix.length ) {
				continue ;
			}
			boolean match = true ;
			for ( int i = 0; i < prefix.length; i++ )
				if ( match && buff[i+incomingOffset] != prefix[i] )
					match = false ;
			if ( !match )
				continue ;
			
			// Okay, prefix matches.
			if ( incomingOffset > 0 )
				System.err.println("Incoming offset is > 0: " + incomingOffset) ;
			for ( int i = 0; i < length - prefix.length ; i++ ) {
				b[offset+i] = buff[i+prefix.length+incomingOffset] ;
			}
			
			if ( offset > 0 && (b[0] == SYN || b[0] == ACK))
				new Exception("offset is greater than 0.  Should it be?").printStackTrace() ;
			
			//if ( b[0] == MSG )
			//	System.err.println("WrappedUDPSocket.  Reading MSG number " + ByteArrayOps.readLongAsBytes(b, 1) + " with length " + (length - prefix.length) + " starting offset " + offset) ;
			//if ( b[0] == SYN )
			//	System.err.println("WrappedUDPSocket.  Reading SYN number " + ByteArrayOps.readLongAsBytes(b, 1) + " with length " + (length - prefix.length) + " starting offset " + offset) ;
			//if ( b[0] == ACK )
			//	System.err.println("WrappedUDPSocket.  Reading ACK number " + ByteArrayOps.readLongAsBytes(b, 1) + " with length " + (length - prefix.length) + " starting offset " + offset) ;
			
			
			return length - prefix.length ;
		}
	}
	
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
	 * @return Returns 0 if the connection failed, otherwise returns the number of bytes read.
	 * @throws IOException if the connection failed.
	 */
	@Override
	protected int readFromSocket( ByteBuffer bb ) throws IOException {
		if ( readArray == null || readArray.length < bb.capacity() )
			readArray = new byte[bb.capacity()] ;
		
		int len = bb.remaining() ;
		// release limit; we will likely have to read far more than this.
		int numRead = readFromSocket( readArray, 0, len ) ;
		
		// put back in bb.
		bb.limit(bb.capacity()) ;
		bb.put(readArray, 0, numRead) ;
		bb.limit(bb.position()) ;
		return numRead ;
	}

	@Override
	protected void noteAck(long milliseconds, int numAttempts) {
		lastPing = milliseconds ;
		if ( averagePing == -1 ) {
			averagePing = lastPing ;
			strength = 1.0 / numAttempts ;
		}
		else {
			averagePing = (long)(averagePing * (1 - NEW_PING_WEIGHT) + lastPing * NEW_PING_WEIGHT) ;
			strength = strength * (1-NEW_PING_WEIGHT) + (1.0/numAttempts) * NEW_PING_WEIGHT ;
		}
	}

	@Override
	public InetAddress getInetAddress() {
		return dsock.getInetAddress() ;
	}

	@Override
	public InetAddress getLocalAddress() {
		return dsock.getLocalAddress() ;
	}

	@Override
	public int getLocalPort() {
		return dsock.getLocalPort() ;
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return dsock.getLocalSocketAddress() ;
	}

	@Override
	public int getPort() {
		return dsock.getPort() ;
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return dsock.getRemoteSocketAddress() ;
	}

	@Override
	public boolean isBound() {
		return dsock.isBound();
	}

	@Override
	public boolean isClosed() {
		return dsock.isClosed();
	}

	@Override
	public boolean isConnected() {
		// A UDP socket is considered connected unless it is closed, or
		// has been idle for a long time.
		if ( !this.isClosed() && this.timeSinceLastReceived() > 20000 ) {
			this.close();
		}
		
		return !this.isClosed() ;
	}

	@Override
	public long latestPing() {
		return lastPing ;
	}

	@Override
	public long averagePing() {
		return averagePing ;
	}

	@Override
	public double successRate() {
		return strength ;
	}

	
	/**
	 * A general-purpose close method that closes the socket and all associated I/O streams.
	 * Remember to call the superclass method when you override.
	 * 
	 */
	@Override
	public void close() {
		super.close() ;
		dsock.close() ;
	}
	
	public void close(boolean blockForOutgoing, long maxDelay) {
		super.close(blockForOutgoing, maxDelay) ;
		dsock.close() ;
	}
	
}
