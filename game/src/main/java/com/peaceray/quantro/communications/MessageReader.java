package com.peaceray.quantro.communications;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

import com.peaceray.quantro.utils.ThreadSafety;

/**
 * The MessageReader is a threaded class for the purpose of reading
 * raw, unencoded messages from an InputStream object.  From
 * instantiation until an error or 'stop' is called, it will read
 * one message at a time from the stream.
 * 
 * Message reads must each be explicitly prompted, either by a call
 * to okToReadNextMessage or by the delegate returning true from
 * the call to mrd_messageReaderMessageIsReady.
 * 
 * Although MessageReaders contain an internal thread, they do NOT
 * need to be explicitly started.  Instantiating a MessageReader is
 * enough.
 * 
 * @author Jake
 *
 */
public class MessageReader {
	
	/**
	 * MessageReaderDelegate.
	 * 
	 * NOTE: One should never block in a MessageReader.Delegate method, especially if you
	 * are waiting for results from some other access to the MessageReader.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {
		/**
		 * The message being read by the MessageReader is ready.
		 * This method is called upon successful read of a new Message,
		 * if the Delegate is set at that time.
		 * 
		 * @param mr The MessageReader.
		 * @return Whether the MessageReader should immediately begin
		 * 		to read the next message.  Equivalent to a call to
		 * 		okToReadNextMessage().
		 */
		public boolean mrd_messageReaderMessageIsReady( MessageReader mr ) ;
		
		/**
		 * The MessageReader has encountered an error while reading
		 * a message.  Most likely the InputStream is broken or empty.
		 * This method will be followed soon after by a call to 
		 * mrd_messageReaderStopped.
		 * 
		 * @param mr
		 */
		public void mrd_messageReaderError( MessageReader mr ) ;
		
		/**
		 * The MessageReader has stopped.  This method is called as
		 * the last operation of the MessageReaderThread, and will 
		 * occur whether stopped from inside (
		 * 
		 * @param mr
		 */
		public void mrd_messageReaderStopped( MessageReader mr ) ;
	}

	class MessageReaderThread extends Thread {
		
		Object inputSource ;
		
		Message message ;
		boolean reading ;
		boolean startedReading ;
		
		// Status updates
		long lastStartOfMessageTime ;
		long lastEndOfMessageTime ;
		
		
		// Heap Big Error
		boolean error ;
		
		MessageReaderThread( Message m, InputStream is ) {
			inputSource = is ;
			
			message = m ;
			reading = false ;
			startedReading = false ;
			error = false ;
		}
		
		MessageReaderThread( Message m, ReadableByteChannel rbc ) {
			inputSource = rbc ;
			
			message = m ;
			reading = false ;
			startedReading = false ;
			error = false ;
		}
		
		
		
		@Override
		public void run() {
			
			lastEndOfMessageTime = lastStartOfMessageTime = System.currentTimeMillis() ;
			
			running = true ;
			while(running) {
				
				synchronized( this ) {
					// If we are currently running, but not currently reading,
					// then wait for a notification.
					if ( running && !reading ) {
						try {
							this.wait() ;
						} catch (InterruptedException e) {
							// e.printStackTrace();
							error = true ;
							running = false ;
						}
					}
				}
				
				startedReading = false ;
				
				while ( reading && running ) {
					try {
						if ( !startedReading && reading && running ) {
							lastStartOfMessageTime = System.currentTimeMillis() ;
							message.resetForRead() ;
							startedReading = true ;
						}
						if ( reading && running && message.read(inputSource) ) {
							// We have completed reading a message.
							reading = false ;
							lastEndOfMessageTime = System.currentTimeMillis() ;
							
							// Tell the delegate about it.  Synchronize all delegate access.
							// Try within an if, because delegate may have been set to 'null' after 'if'.
							// We do the 'if' on the assumption that it's faster than catching
							// a null pointer exception every time.
							boolean shouldRead = false ;
							if ( delegate != null ) {
								try {
									synchronized( delegate ) {
										shouldRead = delegate.mrd_messageReaderMessageIsReady(MessageReader.this) ;
									}
								} catch( NullPointerException e ) { }
							}
							
							// perform an update to reading...
							synchronized( this ) {
								reading = reading || shouldRead ;
							}
							break ; 		// break the INNER while loop.
						}
					} catch (IOException e) {
						//e.printStackTrace() ;
						error = true ;
						running = false ;
					} catch (ClassNotFoundException e) {
						//e.printStackTrace() ;
						error = true ;
						running = false ;
					} catch (Exception e) {
						//e.printStackTrace() ;
						error = true ;
						running = false ;
					}
				}
			}
			
			if ( delegate != null ) {
				try {
					synchronized( delegate ) {
						if ( error && delegate != null ) {
							//System.err.println("Error: error is " + error) ;
							//System.err.println("status is " + status()) ;
							delegate.mrd_messageReaderError(MessageReader.this) ;
						}
						if ( delegate != null ) {
							delegate.mrd_messageReaderStopped(MessageReader.this) ;
						}
					}
				} catch( NullPointerException e ) { }		// catch: synchronize after if, but may have set delegate=null between
			}
			
		}
		
		
		// Message access.
		public boolean messageReady() {
			return !reading  ;
		}
	}
	
	public static final int STATUS_INPUT_SOURCE_NULL = -1 ;
	public static final int STATUS_INPUT_SOURCE_BROKEN = 0 ;
	public static final int STATUS_STOPPED = 1 ;
	public static final int STATUS_MESSAGE_READY = 2 ;
	public static final int STATUS_WAITING_FOR_MESSAGE = 3 ;
	public static final int STATUS_READING_MESSAGE = 4 ;
	public static final int STATUS_UNSPECIFIED_ERROR = 5 ;
	
	
	
	ReadableByteChannel rbc ;		// A readable byte channel for reading bytes.
	InputStream is ;				// The input stream provided.
	boolean running ;		// Is the sub-thread currently reading?
	
	MessageReaderThread thread ;
	
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
	public MessageReader(Message m, InputStream is) {
		
		running = true ;
		
		delegate = null ;
		
		this.rbc = null ;
		this.is = is ;
		
		thread = new MessageReaderThread( m, is ) ;
		thread.start() ;
	}
	
	
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
	public MessageReader(Message m, ReadableByteChannel rbc) {
		
		running = true ;
		
		delegate = null ;
		
		this.rbc = rbc ;
		this.is = null ;
		
		thread = new MessageReaderThread( m, rbc ) ;
		thread.start() ;
	}
	
	
	
	
	
	public void setDelegate( Delegate d ) {
		// If delegate is null, just set.  IF non-null, synchronize
		// on the PREVIOUS DELEGATE.  Note: one way to do this is to
		// simply catch the 'nullPointerException' from when we synchronize.
		try {
			synchronized(this.delegate) {
				this.delegate = d ;
			}
		} catch( NullPointerException e ) {
			this.delegate = d ;
		}
	}
	
	
	public int status() {
		if ( is == null && rbc == null )
			return STATUS_INPUT_SOURCE_NULL ;
		
		if ( thread != null && thread.error )
			return STATUS_INPUT_SOURCE_BROKEN ;
		
		if ( !running )
			return STATUS_STOPPED ;
		
		if ( thread != null && messageReady() )
			return STATUS_MESSAGE_READY ;
		
		if ( thread != null && !thread.startedReading )
			return STATUS_WAITING_FOR_MESSAGE ;
		
		if ( thread != null && thread.startedReading )
			return STATUS_READING_MESSAGE ;
		
		return STATUS_UNSPECIFIED_ERROR ;
	}
	
	/**
	 * A non-blocking way to stop the read thread.
	 * Because the thread may continue for an unspecified period
	 * of time after this call, you should NOT expect this
	 * object's Message to remain unchanged after the call,
	 * unless messageReady() was previously true.
	 * @throws  
	 */
	public void stop() {
		// Stop the execution.  Do this in a synchronized block, notifying
		// the thread of the change.
		synchronized(thread) {
			running = false ;
			if ( thread != null )
				thread.notify() ;
		}
	}
	
	/**
	 * Closes the stream provided in the constructor.  Calling this and
	 * 'stop' together may cause the thread to terminate earlier than just
	 * calling 'stop', but it the MessageReader will report an Error condition.
	 */
	public void close() {
		synchronized(this) {
			try {	// can do both in one try; at most one is non-null.
				if ( is != null )
					is.close() ;
				if ( rbc != null )
					rbc.close() ;
			} catch (IOException e) { }
			try {	// can do both in one try; at most one is non-null.
				if ( is != null )
					is.close() ;
				if ( rbc != null )
					rbc.close() ;
			} catch (IOException e) { }
		}
	}
	
	/**
	 * Blocks until the internal thread has terminated.
	 * 
	 * This is a dangerous call to make if you haven't called 'close'; 
	 * the underlying InputStream may block indefinitely, and the
	 * Thread would never terminate!
	 */
	public void join() {
		ThreadSafety.waitForThreadToTerminate(thread) ;
		// NOTE: testing on HTC Incredible showed that the call to waitForThreadToTerminate
		// will sometimes fail, possibly because the OS takes some time to shut down
		// the connection and halts the thread before this happens.  Thus, our Activity
		// (if run on Android) never completes onStop().  THIS IS BAD!!
		
		// The recommended way to resolve this is to replace socket objects
		// with SocketChannels, allowing for reads with a timeout.  For now,
		// though, allow this zombie thread to linger or close down in the background.
		
	}
	
	public boolean messageReady() {
		return ( thread != null && thread.messageReady() ) ;
	}
	
	/**
	 * Returns a REFERENCE to the current incoming message.
	 * Perform whatever processing is required, then call
	 * okToReadNextMessage.
	 * @return
	 */
	public Message getMessage() {
		return thread.message ;
	}
	
	
	public MessageReader okToReadNextMessage() {
		if ( thread == null || !running )
			throw new IllegalStateException("Cannot call okToReadNextMessage when the thread is null or stopped!") ;
		
		synchronized(thread) {
			// If currently reading, do nothing.  Otherwise, empty the message,
			// set reading = true, and notify.
			if ( !thread.reading ) {
				thread.reading = true ;
				thread.notify() ;
			}
		}
		
		return this ;
	}
}
