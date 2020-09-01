package com.peaceray.quantro.communications;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.peaceray.quantro.utils.ThreadSafety;

/**
 * Allows reads from an InputStream to proceed asynchronously, so you
 * can worry about other things while you wait.
 * 
 * This class is preemptively obsoleted by java.nio, but implementing AIS
 * was faster than familiarizing myself with java.nio.  If re-writing this
 * entire codebase from scratch I recommend java.nio instead of this approach.
 * 
 * @author Jake
 *
 */
public class AsynchronousReadableByteChannel {
	
	public interface Delegate {
		
		/**
		 * The AIS did read data into the provided ByteBuffer,
		 * which was not configured after being provided to the AIS.
		 * 
		 * The AIS will not attempt another read until (at least)
		 * this method returns.
		 */
		public void arbc_didRead( ByteBuffer data, int len ) ;
		
		
		
		/**
		 * An IO Exception was thrown while reading from the stream.  The
		 * exception is provided.  The InputStreamReader will terminate after
		 * this call.
		 * 
		 * @param e
		 */
		public void arbc_didThrowException( Exception e ) ;
	}

	class AsynchronousReadableByteChannelReaderThread extends Thread {
		
		boolean reading ;
		
		// Status updates
		long lastStartOfMessageTime ;
		long lastEndOfMessageTime ;
		
		AsynchronousReadableByteChannelReaderThread() {
			reading = false ;
		}
		
		
		
		@Override
		public void run() {
			
			lastEndOfMessageTime = lastStartOfMessageTime = System.currentTimeMillis() ;
			
			while(running) {
				
				synchronized( this ) {
					// If we are currently running, but not currently reading,
					// then wait for a notification.
					//System.out.println("AIS: top") ;
					if ( running && !reading ) {
						try {
							//System.err.println("AIS: waiting") ;
							this.wait() ;
							//System.err.println("AIS: done waiting") ;
						} catch (InterruptedException e) {
							//e.printStackTrace() ;
							//System.out.println("AIS terminating from interruption") ;
							return ;
						}
					}
				}
				
				//System.out.println("AIS: middle") ;
				if ( reading && running ) {
					int val ;
					try {
						val = rbc.read(bb) ;
						reading = false ;
					} catch ( Exception e ) {
						if ( running )
							delegate.arbc_didThrowException(e) ;
						return ;
					}
					// tell the delegate
					delegate.arbc_didRead(bb, val) ;
				}
				
				
			}
		}
		
	}
	
	
	
	ReadableByteChannel rbc ;			// The input channel provided.
	boolean running ;		// Is the sub-thread currently running?
	
	boolean blocking ;		// Are we currently blocking?
	
	AsynchronousReadableByteChannelReaderThread thread ;
	
	// Data for our next read using a ByteBuffer.
	ByteBuffer bb ;
	int position ;
	int limit ;
	
	Delegate delegate ;
	
	
	/**
	 * Instantiates a new ARBC, providing the channel from which to read.
	 * ARBC provides a lightweight method of reading from a Channel in
	 * a blocking, but asynchronous, fashion.  This object will dedicate
	 * a thread to reading from the channel and will notify the delegate
	 * when data is available.
	 * working.
	 * 
	 * @param m
	 * @param is
	 */
	public AsynchronousReadableByteChannel(ReadableByteChannel rbc) {
		
		running = true ;
		blocking = false ;
		
		delegate = null ;
		
		this.rbc = rbc ;
		
		thread = new AsynchronousReadableByteChannelReaderThread() ;
		thread.start() ;
	}
	
	public void read( ByteBuffer bb, Delegate d ) {
		if ( !thread.isAlive() || thread.reading ) 
			throw new IllegalStateException("InputStreamReader: inner thread must be alive and not reading") ;
		if ( rbc == null )
			throw new IllegalArgumentException("AsynchronousInputSource: in InputStream mode, but ByteBuffer provided") ;
		
		this.bb = bb ;
		this.position = bb.position() ;
		this.limit = bb.limit() ;
		this.delegate = d ;
		
		synchronized( thread ) {
			thread.reading = true ;
			thread.notify() ;
		}
	}
	
	/**
	 * Blocks until the internal thread has terminated.
	 */
	public void join() {
		ThreadSafety.waitForThreadToTerminate(thread) ;
	}
	
	/**
	 * Blocks until the internal thread has terminated
	 * or the timeout has expired.
	 */
	public void join( long timeout ) {
		ThreadSafety.waitForThreadToTerminate(thread, timeout) ;
	}
	
	
	public boolean alive() {
		return thread.isAlive() ;
	}
	
	
	/**
	 * Attempts to stop the thread.  The current read will still complete.
	 */
	public void stop() {
		synchronized( thread ) {
			running = false ;
			thread.notify() ;
		}
	}
	
	/**
	 * Closes the underlying ReadableByteChannel.
	 */
	public void close() {
		running = false ;
		try {
			rbc.close() ;
		} catch( IOException e ) {
			// nothing
		}
		synchronized( thread ) {
			thread.notify();
		}
	}
}
