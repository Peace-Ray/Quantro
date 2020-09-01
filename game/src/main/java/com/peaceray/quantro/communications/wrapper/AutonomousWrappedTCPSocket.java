package com.peaceray.quantro.communications.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * As extension of SocketWrapper that functions on TCP Socket
 * objects.  TCP Sockets already have guaranteed delivery and
 * message order, so the main purpose of wrapping them is
 * 1. metadata on connection pnig speed, and 
 * 2. TCP/UDP agnosticism in any class which uses wrapped connections.
 * 
 * @author Jake
 *
 */
class AutonomousWrappedTCPSocket extends AutonomousWrappedSocket {
	
	private Socket sock = null ;
	private InputStream is = null ;
	private OutputStream os = null ;
	
	// Meta.
	private long lastPing ;
	private long averagePing ;
	private static final double NEW_PING_WEIGHT = 0.2 ;
	
	// Conversion byte arrays.
	private byte [] writeArray = null ;
	private byte [] readArray = null ;
	
	AutonomousWrappedTCPSocket( Socket sock ) throws IOException {
		// We have guaranteed delivery, so we never resend.  We
		// do allow a large number of messages to be sent before we get
		// an acknowledgment, though, since - as noted - delivery
		// is guaranteed.  This wrapper uses approximately 18k of RAM.
		super(128, false, 0) ; 		// no timeout for ACKs; wait for the connection to fail
		this.setMaxTimeBetweenMessages(5000) ;		// we use standard TCP socket controls;
													// we needs occassional SYNs (to check for
													// one phone losing TCP connectivity) but not
													// very often.
		
		this.sock = sock ;
		this.is = sock.getInputStream() ;
		this.os = sock.getOutputStream() ;
		
		lastPing = -1 ;
		averagePing = -1 ;
	}

	@Override
	protected int writeToSocket(byte[] b, int offset, int len)
			throws IOException {
		
		// TCP sockets allow direct writes to IO streams.
		// They are also full-duplex, so we don't worry about stepping on
		// the read method's toes.
		this.os.write(b, offset, len) ;
		return len ;
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
	@Override
	protected int writeToSocket( ByteBuffer bb ) throws IOException {
		
		if ( writeArray == null || writeArray.length < bb.capacity() )
			writeArray = new byte[bb.capacity()] ;
		
		int len = bb.remaining() ;
		bb.get( writeArray, 0, len ) ;
		
		return writeToSocket( writeArray, 0, len ) ;
	}

	@Override
	protected int readFromSocket(byte[] b, int offset, int len)
			throws IOException {
		
		// TCP sockets allow direct reads from IO streams.
		// They are also full-duplex, so we don't worry about stepping
		// on the write method's toes.
		// This call will block until input is available, but that is
		// acceptable operation, since we don't block calls to write.
		int lengthRead = this.is.read(b,offset,len) ;
		if ( lengthRead < 0 ) {
			is.close() ;
			throw new IOException("In WrappedTCPSocket, InputStream.read(...) returned -1 indicating EOF") ;
		}
		return lengthRead ;
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
		int numRead = readFromSocket( readArray, 0, len ) ;
		
		// put back in bb.
		bb.put(readArray, 0, numRead) ;
		return numRead ;
	}
	

	@Override
	protected void noteAck(long milliseconds, int numAttempts) {
		// TCP guarantees delivery.  We never resend, so numAttempts should be 1.
		lastPing = milliseconds ;
		if ( averagePing == -1 )
			averagePing = lastPing ;
		else
			averagePing = (long)((double)averagePing * (1 - NEW_PING_WEIGHT) + (double)lastPing * NEW_PING_WEIGHT) ;
	}

	@Override
	public InetAddress getInetAddress() {
		return sock.getInetAddress() ;
	}

	@Override
	public InetAddress getLocalAddress() {
		return sock.getLocalAddress() ;
	}

	@Override
	public int getLocalPort() {
		return sock.getLocalPort() ;
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return sock.getLocalSocketAddress() ;
	}

	@Override
	public int getPort() {
		return sock.getPort() ;
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return sock.getRemoteSocketAddress() ;
	}

	@Override
	public boolean isBound() {
		return sock.isBound() ;
	}

	@Override
	public boolean isClosed() {
		return sock.isClosed() ;
	}

	@Override
	public boolean isConnected() {
		return sock.isConnected() ;
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
		// Remember: guaranteed delivery is a perfect success rate.
		return 1.0 ;
	}
	
	@Override
	public void close() {
		close(false, 0) ;
	}
	
	/**
	 * A general-purpose close method that closes the socket and all associated I/O streams.
	 * Remember to call the superclass method when you override.
	 * 
	 */
	@Override
	public void close(boolean blockForOutgoing, long maximumDelay) {
		//System.err.println("WrappedTCPSocket: close.") ;
		//new Exception().printStackTrace() ;
		
		// close superclass; shut down I/O threads and pipes.  This should give
		// a chance to flush outgoing info.
		super.close( blockForOutgoing, maximumDelay ) ;
		try {
			sock.close();
		} catch( IOException e ) {
			// nothing 
		}
	}

}
