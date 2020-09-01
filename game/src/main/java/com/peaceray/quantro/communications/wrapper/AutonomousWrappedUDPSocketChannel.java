package com.peaceray.quantro.communications.wrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

public class AutonomousWrappedUDPSocketChannel extends AutonomousWrappedSocket {

	public static final int IDLE_TIMEOUT = 10000 ;		// 10 seconds w/o any data = timeout
	// NOTE: this is an entirely local check, in 
	// our socket receive method; if we go this long
	// without receiving any prefix-labeled information,
	// we throw an exception to close the socket.
	// Compare this behavior to maxTimeWaitingForAck behavior,
	// which doesn't care about the rate data is coming in,
	// and is concerned only with the ACK response to the
	// oldest sent message.

	DatagramChannel dchannel ;
	SelectionKey selectionKey ;
	SocketAddress destAddr ;
	
	ByteBuffer buffer_outgoing ;
	ByteBuffer buffer_incoming ;
	
	byte [] prefix ;
	
	// Meta.
	private long lastPing ;
	private long averagePing ;
	private static final double NEW_PING_WEIGHT = 0.2 ;
	private double strength ;
	
	// Prefix information.  We get our prefix from the bytes used to do UDP pass-through;
	// that way we ensure that both parties use the same prefix.
	
	AutonomousWrappedUDPSocketChannel( DatagramChannel channel, SocketAddress destinationAddress, byte [] prefix ) throws IOException {
		// Datagrams do not have guaranteed or ordered delivery.
		// We DO resend, but we don't want to get too ahead of ourselves.
		// At present writing, max message length is 128, of which 13 is metadata
		// for a MSG.  For a big message, each chunk contains 115 bytes, meaning
		// a 64 message send-ahead allows 7.2k of data to be sent before we start
		// blocking for acknowledgment.  Hopefully this would eat up a chunk of a full
		// synchronization.
		super( 64, true, 8000 ) ;		// timeout if 8 seconds w/o ACK
		
		this.dchannel = channel ;
		channel.configureBlocking(false) ;
		this.selectionKey = channel.register(Selector.open(), SelectionKey.OP_READ) ;
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
		
		buffer_outgoing = ByteBuffer.allocate(msgLength) ;
		buffer_incoming = ByteBuffer.allocate(msgLength) ;
		
		
		lastPing = -1 ;
		averagePing = -1 ;
		strength = 1.0 ;
	}
	
	@Override
	protected int writeToSocket(byte[] b, int offset, int len)
			throws IOException {
		
		// Calls to this method are always synchronized, so
		// we don't need to worry about accidentally overwriting buffer_outgoing
		// while we write.  Copy the buffer provided into dp_outgoing and send.
		buffer_outgoing.clear() ;
		buffer_outgoing.put(prefix) ;
		buffer_outgoing.put(b, offset, len) ;
		buffer_outgoing.flip();
		
		int bytes = buffer_outgoing.remaining() ;
		
		// Send
		dchannel.send(buffer_outgoing, destAddr) ;
		
		return bytes ;
	}
	
	
	@Override
	protected int writeToSocket( ByteBuffer bb ) throws IOException {
		buffer_outgoing.clear() ;
		buffer_outgoing.put(prefix) ;
		buffer_outgoing.put(bb) ;
		buffer_outgoing.flip() ;
		
		int bytes = buffer_outgoing.remaining() ;
		
		// send
		dchannel.send(buffer_outgoing, destAddr) ;
		
		return bytes ;
	}
	
	@Override
	protected int readFromSocket(byte[] b, int offset, int lenWillBeIgnored)
			throws IOException {
		
		long startTime = System.currentTimeMillis() ;
		int timeRemaining ;
		while ( true ) {
			timeRemaining = (int)(IDLE_TIMEOUT - (System.currentTimeMillis() - startTime)) ;
			if ( timeRemaining <= 0 )
				throw new IOException("Went " + IDLE_TIMEOUT + " milliseconds without reading any data from the other side.") ;
			
			SocketAddress sa = null ;
			try {
				// select for a receive.  Stupid use for a Selector, right?  WRONG!
				// Android 2.3 has a SERIOUS bug where timeouts fail on blocking channels,
				// and non-blocking channels block forever!  There is no good way to
				// receive directly if we don't know there is a packet available!
				// Selection is ABSOLUTELY NECESSARY.
				selectionKey.selector().selectNow() ;
				Set<SelectionKey> selectedKeys = selectionKey.selector().selectedKeys();
				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
				while(keyIterator.hasNext()) { 
				    keyIterator.next();
				    buffer_incoming.clear() ;
					sa = dchannel.receive(buffer_incoming) ;
					buffer_incoming.flip() ;
				    keyIterator.remove();
				}
			} catch( SocketTimeoutException e ) {
				// Whelp, looks like we've been idle for 5 seconds without receiving
				// any information with the prefix we want.  Shut Down Everything.
				throw new IOException("Went " + IDLE_TIMEOUT + " milliseconds without reading any data from the other side.") ;
			}
	 		
			
			
			// Check the prefix
			if ( sa == null || buffer_incoming.remaining() < prefix.length ) {
				// continue.  If sa is null, we received no data and should sleep.  Otherwise continue
				// immediately.
				if ( sa == null ) {
					try {
						Thread.sleep(3) ;
					} catch( InterruptedException e ) {
						throw new IOException("Interrupted when sleeping between read attempts.") ;
					}
				}
				continue ;
			}
			
			boolean match = true ;
			for ( int i = 0; i < prefix.length; i++ )
				if ( match && buffer_incoming.get() != prefix[i] )
					match = false ;
			
			if ( !match )
				continue ;
			
			// Prefix matches.  Read the rest into b.
			int length = buffer_incoming.remaining() ;
			buffer_incoming.get(b, offset, length) ;
			
			
			return length ;
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
	protected int readFromSocket( ByteBuffer bb ) throws IOException {
		long startTime = System.currentTimeMillis() ;
		int timeRemaining ;
		while ( true ) {
			timeRemaining = (int)(IDLE_TIMEOUT - (System.currentTimeMillis() - startTime)) ;
			if ( timeRemaining <= 0 )
				throw new IOException("Went " + IDLE_TIMEOUT + " milliseconds without reading any data from the other side.") ;
			
			SocketAddress sa = null ;
			try {
				// select for a receive.  Stupid use for a Selector, right?  WRONG!
				// Android 2.3 has a SERIOUS bug where timeouts fail on blocking channels,
				// and non-blocking channels block forever!  There is no good way to
				// receive directly if we don't know there is a packet available!
				// Selection is ABSOLUTELY NECESSARY.
				selectionKey.selector().selectNow() ;
				Set<SelectionKey> selectedKeys = selectionKey.selector().selectedKeys();
				Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
				while(keyIterator.hasNext()) { 
				    keyIterator.next();
				    buffer_incoming.clear() ;
					sa = dchannel.receive(buffer_incoming) ;
					buffer_incoming.flip() ;
				    keyIterator.remove();
				}
			} catch( SocketTimeoutException e ) {
				// Whelp, looks like we've been idle for 5 seconds without receiving
				// any information with the prefix we want.  Shut Down Everything.
				throw new IOException("Went " + IDLE_TIMEOUT + " milliseconds without reading any data from the other side.") ;
			} catch( ClosedSelectorException cse ) {
				throw new IOException("WrappedSocket has been closed: selectionKey canceled.") ;
			}
			
			// Check the prefix
			if ( sa == null || buffer_incoming.remaining() < prefix.length ) {
				// continue.  If sa is null, we received no data and should sleep.  Otherwise continue
				// immediately.
				if ( sa == null ) {
					try {
						Thread.sleep(3) ;
					} catch( InterruptedException e ) {
						throw new IOException("Interrupted when sleeping between read attempts.") ;
					}
				}
				continue ;
			}
			
			boolean match = true ;
			for ( int i = 0; i < prefix.length; i++ )
				if ( match && buffer_incoming.get() != prefix[i] )
					match = false ;
			
			if ( !match )
				continue ;
			
			// Prefix matches.  Read the rest into bb.
			int length = buffer_incoming.remaining() ;
			bb.position(0).limit(bb.capacity()) ;
			bb.put(buffer_incoming) ;
			
			return length ;
		}
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
		return dchannel.socket().getInetAddress() ;
	}

	@Override
	public InetAddress getLocalAddress() {
		return dchannel.socket().getLocalAddress() ;
	}

	@Override
	public int getLocalPort() {
		return dchannel.socket().getLocalPort() ;
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return dchannel.socket().getLocalSocketAddress() ;
	}

	@Override
	public int getPort() {
		return dchannel.socket().getPort() ;
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return dchannel.socket().getRemoteSocketAddress() ;
	}

	@Override
	public boolean isBound() {
		return dchannel.socket().isBound();
	}

	@Override
	public boolean isClosed() {
		return dchannel.socket().isClosed();
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
		dchannel.socket().close() ;
		try {
			if ( selectionKey != null ) {
				selectionKey.cancel() ;
				selectionKey.selector().close() ;
			}
		} catch ( Exception e ) { }
	}
	
	public void close(boolean blockForOutgoing, long maxDelay) {
		super.close(blockForOutgoing, maxDelay) ;
		dchannel.socket().close() ;
		try {
			if ( selectionKey != null ) {
				selectionKey.cancel() ;
				selectionKey.selector().close() ;
			}
		} catch ( Exception e ) { }
	}
	
}
