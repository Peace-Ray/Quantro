package com.peaceray.quantro.communications;

import java.io.IOException;
import java.io.InputStream;

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
public class AsynchronousInputStream {
	
	public interface Delegate {
		/**
		 * The AIS did read data into the provided byte array,
		 * beginning at offset, with length len.  'len' is the value returned
		 * by the call to 'read', and should be examined by the callee for the
		 * possibility of an error.
		 * 
		 * The AIS will not attempt another read until (at least)
		 * this method returns.
		 */
		public void ais_didRead( byte [] data, int offset, int len ) ;
		
		
		
		/**
		 * An IO Exception was thrown while reading from the stream.  The
		 * exception is provided.  The InputStreamReader will terminate after
		 * this call.
		 * 
		 * @param e
		 */
		public void ais_didThrowException( Exception e ) ;
	}

	class InputSourceReaderThread extends Thread {
		
		boolean reading ;
		
		// Status updates
		long lastStartOfMessageTime ;
		long lastEndOfMessageTime ;
		
		InputSourceReaderThread() {
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
				if ( reading && running && is != null ) {
					int val ;
					try {
						//System.out.println("beginning read: " + off + " " + len) ;
						val = is.read(b, off, len) ;
						//System.out.println("did read: " + val) ;
						reading = false ;
					} catch ( Exception e ) {
						if ( running )
							delegate.ais_didThrowException(e) ;
						return ;
					}
					// Tell the delegate.
					delegate.ais_didRead(b, off, val) ;
				}
			}
		}
		
	}
	
	
	
	InputStream is ;				// The input stream provided.
	boolean running ;		// Is the sub-thread currently running?
	
	boolean blocking ;		// Are we currently blocking?
	
	InputSourceReaderThread thread ;
	
	// Data for our next read using an input stream
	byte [] b ;
	int off ;
	int len ;
	
	Delegate delegate ;
	
	
	/**
	 * Instantiates a new MessageReader, providing the message and InputStream
	 * objects to be used.  MessageReader will use the two "Message" abstract
	 * class methods to read data into m from the InputStream provided,
	 * using a dedicated thread for this.  It is the responsibility of 
	 * the user to handle thread creation (using 'resume' or 'start')
	 * and destruction (using 'stop') and to explicitly indicate when
	 * to read a message (using okToReadNextMessage).
	 * 
	 * MessageReader will not attempt to recover from read errors
	 * or a message half-read before the thread closed, because doing so
	 * requires more functionality on the part of Message.
	 * 
	 * The provided message object WILL be altered by MessageReader,
	 * but only 1. After 'start' or 'resume' is called, and 2. After
	 * 'okToReadNextMessage' is called.  Once this.messageReady()
	 * returns true, the Message object will not be changed until
	 * okToReadNextMessage() is called again.
	 * 
	 * You can access the Message object later using getMessage().  It's
	 * recommended that you don't retain your own reference to it, to
	 * prevent accidental access / mutation while MessageReader is
	 * working.
	 * 
	 * @param m
	 * @param is
	 */
	public AsynchronousInputStream(InputStream is) {
		
		running = true ;
		blocking = false ;
		
		delegate = null ;
		
		this.is = is ;
		
		thread = new InputSourceReaderThread() ;
		thread.start() ;
	}
	
	
	
	
	
	public void read( byte [] b, Delegate d ) {
		this.read( b, 0, b.length, d ) ;
	}
	
	public void read( byte [] b, int off, int len, Delegate d ) {
		if ( !thread.isAlive() || thread.reading ) 
			throw new IllegalStateException("InputStreamReader: inner thread must be alive and not reading") ;
		if ( is == null )
			throw new IllegalArgumentException("AsynchronousInputSource: in Channel mode, but byte array provided") ;
		
		this.b = b ;
		this.off = off ;
		this.len = len ;
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
	 * Attempts to stop the thread.  The current read will still complete.
	 */
	public void stop() {
		running = false ;
	}
	
	/**
	 * Closes the underlying InputStream.
	 */
	public void close() {
		running = false ;
		try {
			is.close() ;
		} catch( IOException e ) {
			// nothing
		}
		synchronized( thread ) {
			thread.notify();
		}
	}
}
