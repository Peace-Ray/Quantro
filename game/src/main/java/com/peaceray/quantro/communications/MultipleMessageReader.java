package com.peaceray.quantro.communications;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.peaceray.quantro.utils.Debug;

/**
 * The MultipleMessageReader is a threaded class for the purpose of reading
 * raw, unencoded messages from a Pipe.SourceChannel.  Unlike MessageReader,
 * we only allow Pipe.SourceChannels which are set in non-blocking mode.
 * As a trade-off, we allow a large number of delegates to each read,
 * independently, from their own Channel.  A Selector is used to switch
 * between Channels, meaning our overhead is actually extremely low.
 * 
 * Message reads must each be explicitly prompted, either by a call
 * to okToReadNextMessage or by the delegate returning true from
 * the call to mrd_messageReaderMessageIsReady.
 * 
 * Although MessageReaders contain an internal thread, they do NOT
 * need to be explicitly started.  Instantiating a MessageReader is
 * enough; its thread will start / stop as needed as delegates are added.
 * 
 * @author Jake
 *
 */
public class MultipleMessageReader {
	
	@SuppressWarnings("unused")
	private static final boolean DEBUG_LOG = false && Debug.LOG ; ;
	private static final String TAG = "MultipleMessageReader" ;
	
	private final void log( String msg ) {
		if ( DEBUG_LOG ) {
			System.out.println(TAG + " " + mInstance + " : " + msg) ;
		}
	}
	
	private final void log( Exception e, String msg ) {
		if ( DEBUG_LOG ) {
			System.err.println(TAG + " " + mInstance + " : " + msg + " : exception below") ;
			e.printStackTrace() ;
		}
	}
	
	
	/**
	 * MultipleMessageReaderDelegate.
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
		public boolean mmrd_messageReaderMessageIsReady( MultipleMessageReader mr, Object token ) ;
		
		/**
		 * The MessageReader has encountered an error while reading
		 * a message.  Most likely the InputStream is broken or empty.
		 * This method may be followed soon after by a call to 
		 * mrd_messageReaderStopped.
		 * 
		 * @param mr
		 */
		public void mmrd_messageReaderError( MultipleMessageReader mr, Object token ) ;
		
		/**
		 * The MessageReader has stopped.  This method is called as
		 * the last operation of the MessageReaderThread, and will 
		 * occur whether stopped from inside or outside.  This indicates
		 * that the message reader terminated before this Delegate could
		 * ask for (and receive) removal.
		 * 
		 * @param mr
		 */
		public void mmrd_messageReaderStopped( MultipleMessageReader mr, Object token ) ;
	}

	class MessageReaderThread extends Thread {
		
		// Timeout for our selector.  Normally we would be OK selecting
		// forever, since we notify() on change, but some versions of Android (e.g. Honeycomb)
		// have a  a race condition bug
		// such that if the notify() occurs before the select() is
		// called, select() will function as if no notification has taken
		// place (contrary to documented behavior).  We therefore
		// select() with a timeout, so we can be responsive to outside changes.
		private static final long MAX_SELECTION_TIME = 500 ;
		
		private boolean mHasFlags = true ;
		private boolean mRunning = true ;
		
		MessageReaderThread() {
			
		}
		
		
		@Override
		public void run() {
			
			// TODO NOTE: There's a lot of synchronization going on here.
			// To avoid a deadlock, it is EXTREMELY important that
			// we make no calls to delegate methods inside Synchronized
			// blocks.  Make all such calls OUTSIDE of synchronized
			// blocks, delaying them if necessary.  Otherwise, we could
			// enter a state where the Delegate is making a synchronized
			// call into this MMR at the same time we are making a synchronized
			// call out to the delegate.
			//
			// This is complicated by the fact that many Delegate callbacks
			// are followed or preceded by a removal of that Delegate from our
			// managed records (i.e. its DelegateData container object).
			// We only want to do this WITHIN a synchronized block.
			// As you might imagine, this is complicated.
			
			boolean endingWithError = false; 
			ArrayList<DelegateData> ddRemove = new ArrayList<DelegateData>() ;
				// normally we report errors as they occur.  However, as mentioned
				// above, we don't want to make any delegate calls while synchronized.
				// In those cases, instead of making the call, collect the DelegateData
				// instances in this array and report the errors the moment synchronization
				// ends.
			
			// make selector
			Selector selector = null ;
			try {
				selector = Selector.open();
			} catch (IOException e) {
				log( e, "Error occurred in Selector.open(), unknown cause.  Fatal.") ;
				synchronized( this ) {
					endingWithError = true ;
					mRunning = false ;
				}
			}
			
			while( mRunning && !mKilled ) {
				// clear errors
				ddRemove.clear() ;
				
				synchronized( this ) {
					// First: check flags.
					if ( mHasFlags ) {
						// perform flags in an order that doesn't require us
						// to remember previous changes
						Enumeration<Object> keys = mDelegateData.keys() ;
						for ( ; keys.hasMoreElements() ; ) {
							Object key = keys.nextElement() ;
							DelegateData dd = mDelegateData.get(key) ;
							if ( !dd.mError && dd.getFlag(DelegateData.FLAG_NEW ) ) {
								log("Work thread -- adding new delegate") ;
								dd.clearFlag(DelegateData.FLAG_NEW) ;
								dd.mControllingThread = this ;
								// a new object to consider.  We want it
								// non-blocking.
								try {
									dd.mPipeSourceChannel.configureBlocking(false) ;
								} catch (IOException e) {
									log( e, "failed when adding new delegate: could not configure blocking false") ;
									ddRemove.add(dd) ;
									dd.mError = true ;
								}
							}
							
							if ( !dd.mError && dd.mControllingThread == this && dd.getFlag(DelegateData.FLAG_OK_TO_READ) ) {
								log("Work thread -- starting read for delegate") ;
								dd.clearFlag(DelegateData.FLAG_OK_TO_READ) ;
								if ( !dd.mStartedReading && dd.mSelectionKeyRead == null ) {
									// mark reading
									dd.mReading = true ;
									dd.mStartedReading = false ;
									// clear message
									dd.mCurrentMessage.resetForRead() ;
									// set up selection key
									try {
										// This call produces a CancelledKeyException if
										// we have previously registered the channel with the selector,
										// then cancel()ed the key, but have not yet performed
										// a select().  This can happen if the delegate call
										// returns 'false' (don't keep reading) but then very soon 
										// afterwards the delegate calls okToReadNextMessage().
										// To prevent this, we selectNow to remove any canceled keys
										// from our set.
										try {
											selector.selectNow() ;
										} catch (IOException e) { }
										dd.mSelectionKeyRead = dd.mPipeSourceChannel.register(selector, SelectionKey.OP_READ, dd) ;
									} catch (ClosedChannelException e) {
										log( e, "failed when registering channel with selector") ;
										ddRemove.add(dd) ;
										dd.mError = true ;
									}
								}
							}
							
							if ( !dd.mError && dd.mControllingThread == this &&  dd.getFlag(DelegateData.FLAG_CLOSE) ) {
								log("Work thread -- closing delegate source channel") ;
								dd.clearFlag(DelegateData.FLAG_CLOSE) ;
								if ( dd.mSelectionKeyRead != null ) {
									dd.mSelectionKeyRead.cancel() ;
									dd.mSelectionKeyRead = null ;
								}
								try {
									dd.mPipeSourceChannel.close() ;
								} catch ( Exception e ) { }
								// also remove this
								dd.setFlag(DelegateData.FLAG_REMOVE) ;
							}
							
							if ( !dd.mError && dd.mControllingThread == this &&  dd.getFlag(DelegateData.FLAG_REMOVE) ) {
								log("Work thread -- removing delegate") ;
								dd.clearFlag(DelegateData.FLAG_REMOVE) ;
								if ( dd.mSelectionKeyRead != null ) {
									dd.mSelectionKeyRead.cancel() ;
									dd.mSelectionKeyRead = null ;
								}
								// remove from list
								ddRemove.add(dd) ;
							}
						}
						
						mHasFlags = false ;
					}
				}
				
				// report errors that occurred OUTSIDE a synchronized block...
				if ( ddRemove.size() > 0 ) {
					for ( int i = 0; i < ddRemove.size(); i++ ) {
						DelegateData dd = ddRemove.get(i) ;
						if ( dd.mError )
							dd.callDelegateError() ;
						dd.callDelegateStopped() ;
					}
					
					// perform the removal INSIDE a synchronized block...
					synchronized( this ) {
						for ( int i = 0; i < ddRemove.size(); i++ ) {
							remove(ddRemove.get(i)) ;
						}
					}
					// call stop
					
					ddRemove.clear() ;
				}
				
				// check for delegates
				synchronized( this ) {
					if ( mDelegateData.size() == 0 ) {
						log("Work thread -- has no delegates, terminating") ;
						mRunning = false ;
					}
				}
				
				if ( mRunning && !mKilled ) {
					// select and read.  Normally we would be OK selecting
					// forever, but some versions of Android (e.g. Honeycomb)
					// have a bug where a select() operation will terminate
					// if the thread is notify()ed, BUT there is a race condition
					// such that if the notify() occurs before the select() is
					// called, select() will function as if no notification has taken
					// place.  We therefore select() with a timer, so we can be
					// responsive to outside changes.
					try {
						selector.select(MAX_SELECTION_TIME) ;
					} catch (IOException ioe) {
						log( ioe, "Error occurred in selector.select("+MAX_SELECTION_TIME+"), unknown cause.  Fatal.") ;
						endingWithError = true ;
						mRunning = false ;
					} catch ( CancelledKeyException cke ) {
						log( cke, "Work thread -- Canceled key exception in selector.select("+MAX_SELECTION_TIME+").  A Channel has likely been closed from outside.  Finding...") ;
						// A Canceled Key Exception can occur if a Channel was closed
						// by another thread during the select() operation (a race condition).
						// These channels could be closed for whatever reason, so take this 
						// opportunity to evaluate whether any particular Key should be in our
						// Selector.
						synchronized( this ) {
							ddRemove.clear() ;
							for ( int i = 0; i < mDelegateData.size(); i++ ) {
								DelegateData dd = mDelegateData.get(i) ;
								boolean ok = false ;
								try {
									Pipe.SourceChannel psc = dd.mPipeSourceChannel ;
									// check if OK in a way that Exceptions count as failure (default 'false',
									// only set 'true' if all checks work out okay).
									ok = psc.isOpen() && dd.mSelectionKeyRead.isReadable() && dd.mSelectionKeyRead.isValid() ;
								} catch ( Exception e ) {
									// nothing
								} finally {
									if ( !ok ) {
										log("Work thread -- Removing a culprit Key") ;
										ddRemove.add(dd) ;
									}
								}
							}
							if ( ddRemove.size() > 0 ) {
								for ( int i = 0; i < ddRemove.size(); i++ ) {
									remove(ddRemove.get(i)) ;
								}
								ddRemove.clear() ;
							}
						}
						
						// error and remove
						for ( int i = 0; i < ddRemove.size(); i++ ) {
							DelegateData dd = ddRemove.get(i) ;
							dd.callDelegateError() ;
							dd.callDelegateStopped() ;
						}
						
						if ( ddRemove.size() > 0 ) {
							synchronized( this ) {
								for ( int i = 0; i < ddRemove.size(); i++ ) {
									remove(ddRemove.get(i)) ;
								}
								ddRemove.clear() ;
							}
						}
					}
					
					Set<SelectionKey> selectionKeys = selector.selectedKeys() ;
					
					Iterator<SelectionKey> iter = selectionKeys.iterator() ;
					for ( ; iter.hasNext() ; ) {
						SelectionKey skey = iter.next() ;
						DelegateData dd = (DelegateData)skey.attachment() ;
						dd.mStartedReading = true ;
						
						try {
							if ( dd.mCurrentMessage.read(dd.mPipeSourceChannel) ) {
								dd.mMessageReady = true ;
								dd.mReading = false ;
								dd.mStartedReading = false ;
								// tell the delegate
								if ( dd.callDelegateMessageReady() ) {
									log("Work thread -- delegate consumed message and requested another") ;
									// begin another read -- immediately.
									dd.mReading = true ;
									dd.mCurrentMessage.resetForRead() ;
									// we are already registered with the selector.
								} else {
									log("Work thread -- delegate consumed message but did not request another") ;
									// don't start another read just yet -- unregister.
									dd.mSelectionKeyRead.cancel() ;
									dd.mSelectionKeyRead = null ;
								}
							}
						} catch ( Exception e ) {
							log(e, "Work thread -- error reading message.  Removing delegate.") ;
							dd.mError = true ;
							dd.callDelegateError() ;
							if ( dd.mSelectionKeyRead != null )
								dd.mSelectionKeyRead.cancel() ;
							ddRemove.add(dd) ;
						}
						
						iter.remove() ;
					}
				}
				
				// remove any that errored out during that process (in a synchronized block).
				if ( ddRemove.size() > 0 ) {
					for ( int i = 0; i < ddRemove.size(); i++ ) {
						ddRemove.get(i).callDelegateStopped() ;
					}
					synchronized( this ) {
						for ( int i = 0; i < ddRemove.size(); i++ ) {
							remove(ddRemove.get(i)) ;
						}
					}
					ddRemove.clear() ;
				}
			}
			
			try {
				selector.close() ;
			} catch (IOException e) {
				log(e, "Error occurred in selector.close(), unknown cause.  We were already terminating.") ;
				endingWithError = true ;
			}
			
			// if stopping with an error, mark every (controlled) thread
			// as having erred.  In any event, tell every (controlled) thread
			// that we are stopping.
			// Remember to obey our rules for synchronization:
			// make all delegate calls when unsynchronized, only
			// remove DelegateDatas while synchronized.
			
			// first: compile a list of those to remove, while synchronized.
			// Perform the actual removal during this step.
			ddRemove.clear() ;
			synchronized( this ) {
				Enumeration<Object> keys = mDelegateData.keys() ;
				for ( ; keys.hasMoreElements() ; ) {
					Object key = keys.nextElement() ;
					DelegateData dd = mDelegateData.get(key) ;
					if ( dd.mControllingThread == this ) {
						ddRemove.add(dd) ;
						remove(dd) ;
					}
				}
			}
			
			// second: delegate calls.  Unsynchronized.
			for ( int i = 0; i < ddRemove.size(); i++ ) {
				DelegateData dd = ddRemove.get(i) ;
				if ( endingWithError ) {
					dd.mError = true ;
					dd.callDelegateError() ;
				}
				dd.callDelegateStopped() ;
			}
			
			log("Work thread -- TERMINATING.  Have " + mDelegateData.size() + " remaining delegates currently.") ;
		}
		
		/**
		 * A helper for removing Delegates: removes from records and our selector.
		 * This method should only be called when synchronized.
		 * 
		 * @param dd
		 */
		private void remove( DelegateData dd ) {
			try {
				mDelegateData.remove(dd.mToken) ;
			} catch ( Exception e ) { }
			try {
				if ( dd.mSelectionKeyRead != null )
					dd.mSelectionKeyRead.cancel() ;
			} catch ( Exception e ) { }
			// fallback
			try {
				if ( mDelegateData.contains(dd) ) {
					Set<Entry<Object, DelegateData>> entries = mDelegateData.entrySet() ;
					Iterator<Entry<Object, DelegateData>> iter = entries.iterator() ;
					for ( ; iter.hasNext(); ) {
						Entry<Object, DelegateData> entry = iter.next() ;
						if ( dd == entry.getValue() ) {
							mDelegateData.remove(entry.getKey()) ;
							return ;
						}
					}
				}
			} catch ( Exception e ) { }
		}
	}
	
	public static final int STATUS_INPUT_SOURCE_NULL = -1 ;
	public static final int STATUS_INPUT_SOURCE_BROKEN = 0 ;
	public static final int STATUS_STOPPED = 1 ;
	public static final int STATUS_MESSAGE_READY = 2 ;
	public static final int STATUS_WAITING_FOR_MESSAGE = 3 ;
	public static final int STATUS_READING_MESSAGE = 4 ;
	public static final int STATUS_UNSPECIFIED_ERROR = 5 ;
	public static final int STATUS_NOT_FOUND = 6 ;
	
	
	
	MessageReaderThread mThread ;
	
	Hashtable<Object, DelegateData> mDelegateData ;
	
	boolean mKilled = false ;
	
	private static long INSTANCES_CREATED = 0 ;
	long mInstance = 0 ;
	// Used only for log messages, to distinguish between logs by different
	// readers.
	
	/**
	 * Instantiates a new MultipleMessageReader.
	 * 
	 * MultipleMessageReader will not attempt to recover from read errors
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
	public MultipleMessageReader() {
		mInstance = INSTANCES_CREATED++ ;
		mDelegateData = new Hashtable<Object, DelegateData>() ;
	}
	
	
	/**
	 * Permanently and immediately kills this MessageReader.
	 * This method is dangerous and frightening, resulting in
	 * exceptions being thrown by all publically-facing methods
	 * from now on.
	 * 
	 * Useful if you are absolutely sure that this MMR is finished,
	 * and any Client currently using it is also irrelevant.  One
	 * such case is when an Activity or Service is finishing().
	 * 
	 */
	public void kill() {
		log(" | -- KILLED -- | ") ;
		mKilled = true ;
		mDelegateData.clear() ;
		MessageReaderThread thread = mThread ;
		if ( thread != null ) {
			thread.mRunning = false ;
		}
	}
	
	
	/**
	 * Adds a client for reading.
	 * 
	 * @param token A unique token, used to retrieve data and for Delegate notification
	 * 		methods.  You can safely provide a reference to the Delegate itself, but
	 * 		only if you can guarantee that this will be the only time the client is 
	 * 		added.  Using the same token as is currently in use is an error.
	 * @param d The delegate who will respond to events.  If null, you will need to query
	 * 		accessors using the provided token.
	 * @param m The message instance into which we will read data.  This instance
	 * 		WILL be modifieds by MultipleMessageReader, but only after 'okToReadMessage'
	 * 		is called.  Once this.messageReady( token ) returns true, the object will
	 * 		not be modified again until okToReadMessage is called again.
	 * @param rbc The Pipe.SourceChannel from which we will read.  We will set this
	 * 		Channel as non-blocking and select on it.
	 */
	public void addClient( Object token, Delegate d, Message m, Pipe.SourceChannel psc ) {
		throwIfKilled() ;
		
		if ( token == null )
			throw new NullPointerException("Must provide non-null Token object") ;
		if ( psc == null )
			throw new NullPointerException("Must provide non-null Pipe.SourceChannel") ;
		if ( m == null )
			throw new NullPointerException("Must provide non-null message instance.") ;
		
		log("Adding a new client") ;
		
		DelegateData dd = new DelegateData( token, d, psc ) ;
		dd.mCurrentMessage = m ;
		
		// do we need a new thread for this?
		boolean needsThread = false ;
		if ( mThread == null || !mThread.isAlive() || !mThread.mRunning ) {
			log("Making a new thread for new client") ;
			needsThread = true ;
			mThread = new MessageReaderThread() ;
		}
		
		synchronized( mThread ) {
			if ( !mThread.mRunning ) {
				log("Making a SECOND new thread for new client") ;
				needsThread = true ;
				mThread = new MessageReaderThread() ;
			}
			
			// add our delegate to the current set of those handled by the thread
			mDelegateData.put(token, dd) ;
			
			// start the thread (if we created it before)
			mThread.mHasFlags = true ;
			if ( needsThread ) {
				mThread.start() ;
			}
		}
	}
	
	
	public int status( Object token ) {
		throwIfKilled() ;
		
		if ( token == null )
			return STATUS_NOT_FOUND ;
		
		DelegateData dd = mDelegateData.get(token) ;
		if ( dd != null ) {
			synchronized( dd ) {
				if ( dd.mPipeSourceChannel == null )
					return STATUS_INPUT_SOURCE_NULL ;
				
				if ( dd.mError )
					return STATUS_INPUT_SOURCE_BROKEN ;
				
				if ( dd.mMessageReady )
					return STATUS_MESSAGE_READY ;
				
				if ( dd.mReading && !dd.mStartedReading )
					return STATUS_WAITING_FOR_MESSAGE ;
				
				if ( dd.mReading && dd.mStartedReading )
					return STATUS_READING_MESSAGE ;
				
				return STATUS_UNSPECIFIED_ERROR ;
			}
		}
		
		return STATUS_NOT_FOUND ;
	}
	
	/**
	 * A non-blocking way to stop reading messages for this Delegate.
	 * Because the thread may continue for an unspecified period
	 * of time after this call, you should NOT expect this
	 * object's Message to remain unchanged after the call,
	 * unless messageReady() was previously true.
	 * @throws  
	 */
	public void remove( Object token ) {
		throwIfKilled() ;
		
		if ( token == null )
			return ;
		DelegateData dd = mDelegateData.get(token) ;
		if ( dd != null ) {
			flagAndNotify( dd, DelegateData.FLAG_REMOVE ) ;
		}
	}
	
	/**
	 * A non-blocking way to stop reading messages for this Delegate
	 * and close the associated channel.
	 * 
	 * @param d
	 */
	public void closeAndRemove( Object token ) {
		throwIfKilled() ;
		
		if ( token == null )
			return ;
		DelegateData dd = mDelegateData.get(token) ;
		if ( dd != null ) {
			flagAndNotify( dd, DelegateData.FLAG_CLOSE ) ;
		}
	}
	
	/**
	 * Immediately closes the channel and schedules a removal.
	 * This method may result in an error to the delegate.
	 * 
	 * @param d
	 */
	public void closeNowAndRemove( Object token ) {
		throwIfKilled() ;
		
		if ( token == null )
			return ;
		DelegateData dd = mDelegateData.get(token) ;
		if ( dd != null ) {
			try {
				dd.mPipeSourceChannel.close() ;
			} catch( Exception e ) { }
			flagAndNotify( dd, DelegateData.FLAG_CLOSE ) ;
		}
	}
	
	public boolean messageReady( Object token ) {
		throwIfKilled() ;
		
		if ( token == null )
			return false ;
		DelegateData dd = mDelegateData.get(token) ;
		if ( dd != null ) {
			return dd.mMessageReady ;
		}
		return false ;
	}
	
	/**
	 * Returns a REFERENCE to the current incoming message.
	 * Perform whatever processing is required, then call
	 * okToReadNextMessage.
	 * @return
	 */
	public Message getMessage( Object token ) {
		throwIfKilled() ;
		
		if ( token == null )
			return null ;
		DelegateData dd = mDelegateData.get(token) ;
		if ( dd != null ) {
			return dd.mCurrentMessage ;
		}
		return null ;
	}
	
	
	public MultipleMessageReader okToReadNextMessage( Object token ) {
		throwIfKilled() ;
		
		if ( token == null )
			return this ;
		DelegateData dd = mDelegateData.get(token) ;
		if ( dd != null ) {
			// If currently reading, do nothing.  Otherwise, empty the message,
			// set reading = true, and notify.
			if ( !dd.mReading ) {
				flagAndNotify( dd, DelegateData.FLAG_OK_TO_READ ) ;
			}
		}
		
		return this ;
	}
	
	private void flagAndNotify( DelegateData dd, int flag ) {
		dd.setFlag(flag) ;
		MessageReaderThread thread = mThread ;
		if ( thread != null ) {
			synchronized( thread ) {
				thread.mHasFlags = true ;
				thread.notify() ;
			}
		}
	}
	
	private void throwIfKilled() {
		if ( mKilled ) 
			throw new IllegalStateException("This MMR has been killed.") ;
	}
	
	
	private class DelegateData {
		Object mToken ;
		
		Delegate mDelegate ;
		Pipe.SourceChannel mPipeSourceChannel ;
		SelectionKey mSelectionKeyRead ;
		
		MessageReaderThread mControllingThread ;
		
		Message mCurrentMessage ;
		boolean mReading = false ;
		boolean mStartedReading = false ;
		
		boolean mMessageReady = false ;
		
		boolean mError = false ;
		
		int mFlags ;
		
		private static final int FLAG_NEW = 0x1 ;
		private static final int FLAG_OK_TO_READ = 0x2 ;
		private static final int FLAG_REMOVE = 0x4 ;
		private static final int FLAG_CLOSE = 0x8 ;
		
		DelegateData( Object token, Delegate d, Pipe.SourceChannel psc ) {
			mToken = token ;
			mDelegate = d ;
			mPipeSourceChannel = psc ;
			mSelectionKeyRead = null ;
			
			mFlags = FLAG_NEW ;
		}
		
		/**
		 * Calls the delegate's MessageReady message, returning the result.
		 * 
		 * Captures any exceptions safely.
		 * 
		 * @return
		 */
		private boolean callDelegateMessageReady() {
			try {
				return mDelegate.mmrd_messageReaderMessageIsReady(MultipleMessageReader.this, mToken) ;
			} catch ( Exception e ) {
				return false ;
			}
		}
		
		/**
		 * Calls the delegates Error message.  Captures any exceptions safely.
		 */
		private void callDelegateError() {
			try {
				mDelegate.mmrd_messageReaderError(MultipleMessageReader.this, mToken) ;
			} catch ( Exception e ) { }
		}
		
		/**
		 * Calls the delegate's Stopped message.  Captures any exceptions safely.
		 */
		private void callDelegateStopped() {
			try {
				mDelegate.mmrd_messageReaderStopped(MultipleMessageReader.this, mToken) ;
			} catch ( Exception e ) { }
		}
		
		private void setFlag( int flag ) {
			mFlags = mFlags | flag ;
		}
		
		private boolean getFlag( int flag ) {
			return (mFlags & flag) != 0 ;
		}
		
		private void clearFlag( int flag ) {
			mFlags = mFlags & (~flag) ;
		}
		
		private boolean hasFlags() {
			return mFlags != 0 ;
		}
	}
}
