package com.peaceray.quantro.communications.wrapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import com.peaceray.quantro.utils.Debug;


/**
 * A Collection of UDPSocketChannels (i.e. DatagramChannels) which
 * administrated by a single instance (i.e.: this object).
 * 
 * Each of those DatagramSockets are treated as a WrappedSocket,
 * and WrappedSocket container objects are available for direct
 * interaction.
 * 
 * The difference between this object, and a simple collection
 * of AutonomousWrappedUDPSockets, is that each of those Autonomous
 * sockets would have its own dedicated read/write threads.  By 
 * contrast, an Administrated socket has no read/write threads of
 * its own; the Administrator (this object) runs its own threads,
 * sending and receiving data to/from a potentially large number of
 * inner DatagramChannels.
 * 
 * @author Jake
 *
 */
public class AdministratedWrappedUDPSocketChannelAdministrator implements AdministratedWrappedSocketAdministrator {

	@SuppressWarnings("unused")
	private static final boolean DEBUG_LOG = false && Debug.LOG ;
	private static final String TAG = "AWUDPSChannelAdministrator" ;
	
	private final void log( String msg ) {
		if ( DEBUG_LOG ) {
			System.out.println(tagString() + msg) ;
		}
	}
	
	private final void log( Exception e, String msg ) {
		if ( DEBUG_LOG ) {
			System.err.println(tagString() + msg + " : exception below") ;
			e.printStackTrace() ;
		}
	}
	
	private final String tagString() {
		AdministratorThread thread = mThread ;
		if ( thread == null )
			return TAG + " " + mInstance + " no thread : " ;
		else if ( thread.mWrappedSockets == null )
			return TAG + " " + mInstance + " no sockets : " ;
		else
			return TAG + " " + mInstance + " has " + thread.mWrappedSockets.size() + " : " ;
	}
	
	AdministratorThread mThread = null ;
	int mInstance ;
	private static int INSTANCE_COUNT = 0 ;
	
	public AdministratedWrappedUDPSocketChannelAdministrator() {
		mThread = null ;
		mInstance = INSTANCE_COUNT++ ;
	}
	
	
	/**
	 * Wraps the provided channel and begins administrating it.  If
	 * 'null' is returned, some error occurred and the channel will not be
	 * administrated.  Otherwise, the return WrappedSocket can be treated
	 * as if it is Autonomous.
	 * 
	 * @param dchannel
	 * @return
	 */
	public synchronized WrappedSocket wrap( Class<?> messageClass, DatagramChannel channel, SocketAddress dest, byte [] prefix ) {
		if ( channel == null || dest == null ) 
			return null ;
		
		try {
			channel.socket().setSoTimeout(0) ;
			AdministratedWrappedUDPSocketChannel ws = new AdministratedWrappedUDPSocketChannel( messageClass, channel, dest, prefix ) ;
			boolean newThread = false ;
			if ( mThread == null || !mThread.isRunning() ) {
				mThread = new AdministratorThread() ;
				newThread = true ;
			}
			
			// add.
			log("Administrating the WrappedSocket") ;
			if ( !mThread.administrate(ws) ) {
				// whoops
				return null ;
			}
			
			if ( newThread ) {
				log("Starting a new thread") ;
				mThread.start() ;
			}
			
			return ws ;
		} catch( Exception e ) {
			return null ;
		}
	}
	
	
	private class AdministratorThread extends Thread {
		
		private static final long MAX_TIME_WITHOUT_RECEIVE = 15000 ;	// 15 seconds without receiving data
		private static final long MAX_TIME_WITHOUT_ACK = 15000 ;		// 15 seconds without ACK
		
		private static final long MIN_TIME_SELECTING = 0 ;			// even if a socket has data in need of immediate send,
																		// we wait a minimum of this much time.
		private static final long MAX_TIME_SELECTING = 1000 ;		// this gives us time to add new administrated threads.
		private static final long MIN_DELAY_BEFORE_RESEND = 100 ;		// Even if our ping appears very low, we wait at least this long between resends.
		private static final long MAX_DELAY_BEFORE_RESEND = 2000 ;		// After 2 seconds, resend the previous message.
															// If we have low average ping, we may attempt to resend
															// sooner.
		private static final long MAX_WRAPPED_SOCKET_SYNC_LEEWAY = 200 ;	// There is overhead every time we step through
															// the .select() loop.  If possible, we'd prefer to do a little more work
															// each time rather than loop often.  However, we still want each socket
															// to remain responsive.  As a middle ground, we allow the "most urgent socket"
															// to wait until the "2nd most urgent socket" is ready, assuming the wait will be
															// no more than this amount.  We only wait if this will cause the operations
															// to occur in the same loop -- we don't delay just to move two operations "closer together."
		private static final long MAX_TIME_BETWEEN_MESSAGES = 2000 ;	// If we haven't sent anything for 2 seconds, send a SYN.
		
		
		private static final long TIME_BETWEEN_BYTES_REPORT = 60000 ;		// every 60 seconds
		private long mLastBytesReport = System.currentTimeMillis() ;
		
		private Object mMetaMutex ;
		private ArrayList<AdministratedWrappedUDPSocketChannel> mWrappedSockets ;
		private ArrayList<AdministratedWrappedUDPSocketChannel> mWrappedSocketsPendingInclusion ;
		
		private Hashtable<SelectionKey, WrappedSocketSelectionKeys> mWrappedSocketDataByKey ;
		private Hashtable<AdministratedWrappedUDPSocketChannel, WrappedSocketSelectionKeys> mWrappedSocketDataBySocket ;
		
		private boolean mRunning ;
		
		private AdministratorThread() {
			mMetaMutex = true ;
			mWrappedSockets = new ArrayList<AdministratedWrappedUDPSocketChannel>() ;
			mWrappedSocketsPendingInclusion = new ArrayList<AdministratedWrappedUDPSocketChannel>() ;
			
			mWrappedSocketDataByKey = new Hashtable<SelectionKey, WrappedSocketSelectionKeys>() ;
			mWrappedSocketDataBySocket = new Hashtable<AdministratedWrappedUDPSocketChannel, WrappedSocketSelectionKeys>() ;
			
			mRunning = true ;
		}
		
		private boolean isRunning() {
			return mRunning ;
		}
		
		
		/**
		 * Sets this thread as the administrator for the provided socket channel.
		 * 
		 * Returns success -- that is, if we will act as an administrator.  Returning
		 * 'false' indicates that the thread is no longer running.
		 * @param ws
		 * @return
		 */
		private boolean administrate( AdministratedWrappedUDPSocketChannel ws ) {
			synchronized( mMetaMutex ) {
				if ( !mRunning )
					return false ;
				if ( mWrappedSockets.contains(ws) || mWrappedSocketsPendingInclusion.contains(ws) )
					return true ;
				
				// add to our pending
				mWrappedSocketsPendingInclusion.add(ws) ;
				return true ;
			}
		}
		
		@Override
		public void run() {
			// verify that we have been started in a good state.
			Selector selector = null ;
			synchronized( mMetaMutex ) {
				if ( mWrappedSocketsPendingInclusion.size() == 0 ) {
					mRunning = false ;
					log("thread started without any sockets to administrate!") ;
				} else {
					try {
						selector = Selector.open() ;
					} catch (IOException e) {
						log(e, "error when opening Selector (fatal).") ;
						mRunning = false ;
					}
				}
			}
			
			
			
			while( mRunning ) {
				// Make necessary changes to our Socket lists.
				synchronized( mMetaMutex ) {
					if ( mWrappedSocketsPendingInclusion.size() > 0 ) {
						while ( mWrappedSocketsPendingInclusion.size() > 0 ) {
							log("--work thread-- moving a new pending socket to our active list") ;
							AdministratedWrappedUDPSocketChannel ws = mWrappedSocketsPendingInclusion.remove(0) ;
							
							SelectionKey readFromChannel = null ;
							SelectionKey readFromPipe = null ;
							
							// register with our read selector and then make a data collection.
							try {
								// Make a Data collection object as our first operation.  That way, if an
								// exception occurs, we can undo any completed operations.
								WrappedSocketSelectionKeys wssk = new WrappedSocketSelectionKeys(
										ws, null, null ) ;
								// we're making these selection keys
								readFromChannel = null ;
								readFromPipe = null ;
								
								readFromChannel = ws.mDatagramChannel.register(selector, SelectionKey.OP_READ) ;
								readFromPipe = ws.mSendSourceChannel.register(selector, SelectionKey.OP_READ) ;
								// from this point, we almost certainly won't throw an exception.
								wssk.mSelectionKeyReadFromDatagramChannel = readFromChannel ;
								wssk.mSelectionKeyReadFromPipe = readFromPipe ;
								
								// put in our records
								mWrappedSockets.add(ws) ;
								mWrappedSocketDataByKey.put(readFromChannel, wssk) ;
								mWrappedSocketDataByKey.put(readFromPipe, wssk) ;
								mWrappedSocketDataBySocket.put(ws, wssk) ;
								// ready for administration!
							} catch ( ClosedChannelException cce ) {
								log( cce, "error adding a pending socket for Administration (fatal for socket)") ;
								if ( readFromChannel != null )
									readFromChannel.cancel() ;
							} catch ( Exception e ) {
								log( e, "Unknown error ading a pending socket for Administration (fatal for socket)") ;
								if ( readFromChannel != null )
									readFromChannel.cancel() ;
								
								if ( ws != null ) {
									mWrappedSockets.remove(ws) ;
									mWrappedSocketDataBySocket.remove(ws) ;
									
									if ( readFromChannel != null )
										mWrappedSocketDataByKey.remove(readFromChannel) ;
									if ( readFromPipe != null )
										mWrappedSocketDataByKey.remove(readFromPipe) ;
								}
							}
							
						}
					}
					
					// check number of administrated sockets
					if ( mWrappedSockets.size() == 0 ) {
						log("--work thread-- no wrapped sockets left") ;
						// no longer running
						mRunning = false ;
						try {
							selector.close() ;
						} catch (IOException e) { }
					}
				}
				
				// check number of administrated sockets
				if ( mRunning ) {
					// SELECT!
					// This is our main operation.  We select on all 'readable' channels, waiting
					// for available data.  Our timeout is the maximum amount of time it is appropriate
					// to wait -- if sockets have information to send, that may be 0.  Otherwise, 
					// select for MAX_TIME_SELECTING.
					long timeSelecting = MAX_TIME_SELECTING ;
					for ( int i = 0; i < mWrappedSockets.size(); i++ ) {
						AdministratedWrappedUDPSocketChannel ws = mWrappedSockets.get(i) ;
						try {
							if ( !ws.isClosed() && ws.timeSinceLastReceived() < MAX_TIME_WITHOUT_RECEIVE && ws.timeWaitingForAck() < MAX_TIME_WITHOUT_ACK ) {
								long timeSelectingThisSocket = MAX_TIME_SELECTING ;
								
								long resendEvery = resendDelay( ws ) ;
								
								long needsSendAfter = ws.getTimeUntilSendNeeded(resendEvery) ;
								if ( needsSendAfter >= 0 )
									timeSelectingThisSocket = Math.min( timeSelectingThisSocket, needsSendAfter ) ;
								
								// what about a SYN?
								long needsSYNAfter = ws.getTimeUntilSYNNeeded(MAX_TIME_BETWEEN_MESSAGES) ;
								if ( needsSYNAfter >= 0 )
									timeSelectingThisSocket = Math.min(timeSelectingThisSocket, needsSYNAfter) ;
								
								// If this time is lower than the "previous min" by more than our leeway,
								// take it as the new time selecting.  Otherwise, keep the old to "sync up"
								// the sends.
								if ( timeSelectingThisSocket + MAX_WRAPPED_SOCKET_SYNC_LEEWAY < timeSelecting ) {
									timeSelecting = timeSelectingThisSocket ;
								}
							} else if ( ws.isClosed() ) {
								log("--work thread-- removing closed socket when determining time to select") ;
								removeFromRecords(selector, ws) ;
							} else {
								log("--work thread-- removing socket after long time without receive") ;
								removeFromRecords(selector, ws) ;
							}
						} catch ( Exception e ) {
							log(e, "--work thread-- removing socket due to Unknown Error when determining time to select") ;
							removeFromRecords(selector, ws) ;
						}
					}
					
					timeSelecting = Math.max(MIN_TIME_SELECTING, timeSelecting) ;
					
					if ( TIME_BETWEEN_BYTES_REPORT > 0 ) {
						if ( this.mLastBytesReport + TIME_BETWEEN_BYTES_REPORT < System.currentTimeMillis() ) {
							
							// byte report!
							long totalSent = 0 ;
							long totalReceived = 0 ;
							for ( int i = 0; i < mWrappedSockets.size(); i++ ) {
								WrappedSocket ws = mWrappedSockets.get(i) ;
								log("BYTES REPORT: socket " + i + ": " + String.format("%.1f", (ws.bytesSent()/1024.0f)) + "kB sent / " + String.format("%.1f", (ws.bytesReceived()/1024.0f)) + "kB received") ;
								totalSent += ws.bytesSent() ;
								totalReceived += ws.bytesReceived() ;
							}
							log("BYTES REPORT: total: " + String.format("%.1f", (totalSent/1024.0f)) + "kB sent / " + String.format("%.1f", (totalReceived/1024.0f)) + "kB received") ;
							
							mLastBytesReport = System.currentTimeMillis() ;
						}
					}
					
					// perform our selection!
					try {
						selector.select(timeSelecting) ;
					} catch (IOException e) {
						// TODO: figure out why the selector might throw here.
						log( e, "Select threw an IOException.  Cause unknown!  (fatal)") ;
						mRunning = false ;
						try {
							selector.close() ;
						} catch (IOException ioe) { }
						return ;
					}
					
					Set<SelectionKey> selectedKeys = selector.selectedKeys() ;
					Iterator<SelectionKey> iter = selectedKeys.iterator() ;
					
					boolean removedKeys = false ;
					
					for ( ; iter.hasNext() ; ) {
						SelectionKey sk = iter.next() ;
						WrappedSocketSelectionKeys wssk = null ;
						AdministratedWrappedUDPSocketChannel ws = null ;
						
						try {
							// Get the associated socket and package.
							wssk = mWrappedSocketDataByKey.get(sk) ;
							ws = wssk.mWrappedSocket ;
							
							if ( !ws.isClosed() ) {
								// Handle "read from pipe" and "read from channel" cases.
								if ( sk == wssk.mSelectionKeyReadFromDatagramChannel ) {
									// Datagram Channel
									// receive one
									if ( ws.isReadyToReceiveIncomingMessage() ) {
										ws.receiveOneWrappedMessageAndSendWrappedACK() ;
									} else {
										// stop selecting on this... we are not ready to receive from here.
										log("--Work thread-- removing ReadFromDatagramChannel key from selector until ready to receive") ;
										sk.cancel() ;
										wssk.mSelectionKeyReadFromDatagramChannel = null ;
										mWrappedSocketDataByKey.remove(sk) ;
										removedKeys = true ;
									}
								} else if ( sk == wssk.mSelectionKeyReadFromPipe ) {
									// Pipe Channel
									if ( ws.isReadyToWrapOutgoingMessage() ) {
										ws.wrapNewOutgoingDATAMessageForSend() ;
									} else {
										// stop selecting on this... we are not ready to receive from the pipe.
										log("--Work thread-- removing ReadFromPipe key from selector until space available to read") ;
										sk.cancel() ;
										wssk.mSelectionKeyReadFromPipe = null ;
										mWrappedSocketDataByKey.remove(sk) ;
										removedKeys = true ;
									}
								}
							} else {
								removedKeys = true ;
								removeFromRecords( null, ws ) ;
							}
						
						} catch ( IOException ioe ) {
							log(ioe, "when trying to read data in or out (fatal for socket)") ;
							removedKeys = true ;
							removeFromRecords( null, ws ) ;
							if ( sk != null )
								sk.cancel() ;
						} catch ( Exception e ) {
							log(e, "exception when trying to read data -- unknown cause") ;
							removedKeys = true ;
							removeFromRecords( null, ws ) ;
							if ( sk != null )
								sk.cancel() ;
						}
						
						// we handled it; remove
						iter.remove() ;
					}
					
					// select now if we removed keys.  Otherwise we can run
					// into a bug where keys are retained.
					if ( removedKeys ) {
						try {
							selector.selectNow() ;
						} catch ( Exception e ) { }
					}
					
					// now handle the "spin" operations: writing out data.
					for ( int i = 0; i < mWrappedSockets.size(); i++ ) {
						boolean ok = true ;
						AdministratedWrappedUDPSocketChannel ws = mWrappedSockets.get(i) ;
						
						try {
							WrappedSocketSelectionKeys wssk = mWrappedSocketDataBySocket.get(ws) ;
							
							if ( ws.isClosed() ) {
								log("--work thread-- removing closed socket when handling 'write' operations") ;
								// remove this socket.
								removeFromRecords( selector, ws ) ;
								i-- ;
							} else {
								long retryEvery = resendDelay(ws) ;
								// perform writes, if needed and possible
								if ( ws.getTimeUntilSYNNeeded(MAX_TIME_BETWEEN_MESSAGES) == 0 ) {
									ws.wrapNewOutgoingSYNMessageForSend() ;
								}
								
								// send out messages on our datagram channel.  We want to
								// send every message that has not yet been sent, and at most
								// 1 resend.
								// Previously we spun to send ALL messages available.  However,
								// that can very quickly result in a terribad situation where 
								// we send out many more messages than we can reasonable read ACKs
								// for.
								int numSends = 0 ;
								while( (ws.hasUnsentOutgoingMessage() || (numSends < 1 && ws.hasOutgoingMessage()))
										&& ws.getTimeUntilSendNeeded(retryEvery) == 0
										&& ws.isReadyToSendOutgoingMessage() ) {
									if ( ws.sendOneWrappedMessage() ) {
										numSends++ ;
									} else {
										// failure.  Stop.
										break ;
									}
								}
								
								// unwrap incoming messages from our queue and write them
								// to our pipe.  Do a big "shotgun blast" style, then 
								// individual iteration.
								while( ws.hasIncomingMessageToUnwrap() && ws.isReadyToUnwrapIncomingMessage() ) {
									if ( !ws.unwrapOneIncomingDATAMessageFromQueue(true) ) {
										// failure.  Stop.
										break ;
									}
								}
								
								// if lacking either selection key, but is capable of
								// handling data-in, register with selector.
								if ( ok && wssk.mSelectionKeyReadFromDatagramChannel == null && ws.isReadyToReceiveIncomingMessage() ) {
									log("--Work thread-- re-establishing ReadFromDatagramChannel key") ;
									SelectionKey readFromChannel ;
									readFromChannel = ws.mDatagramChannel.register(selector, SelectionKey.OP_READ) ;
									// from this point, we won't throw an exception.
									wssk.mSelectionKeyReadFromDatagramChannel = readFromChannel ;
									// put in our records
									mWrappedSocketDataByKey.put(readFromChannel, wssk) ;
								}
									
								if ( ok && wssk.mSelectionKeyReadFromPipe == null && ws.isReadyToWrapOutgoingMessage() ) {
									log("--Work thread-- re-establishing ReadFromPipe key") ;
									SelectionKey readFromPipe ;
									readFromPipe = ws.mSendSourceChannel.register(selector, SelectionKey.OP_READ) ;
									// from this point, we won't throw an exception.
									wssk.mSelectionKeyReadFromPipe = readFromPipe ;
									// put in our records
									mWrappedSocketDataByKey.put(readFromPipe, wssk) ;
								}
							}
						} catch (ClosedChannelException e) {
							log(e, "Closed Channel Error when performing spin on DatagramChannel (fatal for socket).") ;
							removeFromRecords( selector, ws ) ;
							ok = false ;
						} catch ( Exception e ) {
							log( e, "Unknown error when performing spin on DatagramChannel (fatal for socket).") ;
							removeFromRecords( selector, ws ) ;
							ok = false ;
						}
					}
				}
			}
			
			log("--work thread-- EXITING: mRunning is false") ;
		}
		
		
		private long resendDelay( WrappedSocket ws ) {
			long avgPing = ws.averagePing() ;
			long resendEvery = avgPing >= 0 	? Math.min(2 * avgPing, MAX_DELAY_BEFORE_RESEND)
											: MAX_DELAY_BEFORE_RESEND ;
			resendEvery = Math.max( resendEvery, MIN_DELAY_BEFORE_RESEND) ;
			return resendEvery ;
		}
		
		
		private void removeFromRecords( Selector selector, AdministratedWrappedUDPSocketChannel ws ) {
			// Remove from our records.  This is called inside the
			// thread's run loop, so safely touch our members.  Tee hee.
			try {		mWrappedSockets.remove(ws) ;							} catch ( Exception e ) { }
			WrappedSocketSelectionKeys wssk = null ;
			try {		wssk = this.mWrappedSocketDataBySocket.remove(ws) ;		} catch ( Exception e ) { }
			boolean didCancelKey = false ;
			if ( wssk != null && wssk.mSelectionKeyReadFromDatagramChannel != null ) {
				try {	mWrappedSocketDataByKey.remove(wssk.mSelectionKeyReadFromDatagramChannel) ;		} catch ( Exception e ) { }
				try {	wssk.mSelectionKeyReadFromDatagramChannel.cancel() ;							} catch ( Exception e ) { }
				didCancelKey = true ;
			}
			if ( wssk != null && wssk.mSelectionKeyReadFromPipe != null ) {
				try { 	mWrappedSocketDataByKey.remove(wssk.mSelectionKeyReadFromPipe) ;				} catch ( Exception e ) { }
				try { 	wssk.mSelectionKeyReadFromPipe.cancel() ;										} catch ( Exception e ) { }
				didCancelKey = true ;
			}
			
			// Finalize this cancelation by selecting.
			if ( didCancelKey && selector != null ) {
				try {	selector.selectNow() ;															} catch( Exception e ) { }
			}
			
			// Force an immediate close.  If we're interested in flushing
			// outgoing, we will have already closed this WS from outside
			// using the 'flush outgoing' parameter.  That call will take
			// priority and the flush will still occur.
			try { 		ws.close() ; 											} catch ( Exception e ) { }
			try {		ws.mReleasedFromAdministration = true ;					} catch ( Exception e ) { }
		}
		
	}
	
	
	/**
	 * An extremely simple container class.  Used to convert easily from a SelectionKey
	 * to a wrapped socket, or vice-versa (put an instance in a few Hashtables).
	 * @author Jake
	 *
	 */
	private class WrappedSocketSelectionKeys {
		private AdministratedWrappedUDPSocketChannel mWrappedSocket ;
		private SelectionKey mSelectionKeyReadFromPipe ;
		private SelectionKey mSelectionKeyReadFromDatagramChannel ;
		
		private WrappedSocketSelectionKeys( AdministratedWrappedUDPSocketChannel ws,
				SelectionKey readFromPipe, SelectionKey readFromDatagramChannel ) {
			mWrappedSocket = ws ;
			mSelectionKeyReadFromPipe = readFromPipe ;
			mSelectionKeyReadFromDatagramChannel = readFromDatagramChannel ;
		}
	}
	
	
	/**
	 * As an administrated WrappedSocket, instances of this class are not capable
	 * of much, nor is much expected of them.  This class provides a large number
	 * of helpful methods and data-storage capabilities for the Administrator, but
	 * stops just short of actively administrating itself.  It has no operation threads
	 * and requires direct, method-to-method administration to perform even basic operations
	 * such as reading from its "Send" pipe, wrapping the message, and sending it.
	 * 
	 * We'd like to do as much of our own work as we can -- this class, having direct 
	 * access to most of the related data objects, seems like the best place to
	 * implement such methods -- but we CANNOT cause our own sends / receives without
	 * a direct order to do so.  Administrators have the task of juggling multiple Channels
	 * and Selecting between them, determining which channels can send/receive
	 * safely without preventing others from doing so.
	 * 
	 * One way of dividing responsibility: this WrappedSocket will NEVER 'select'
	 * between channels, and will treat all channels as if 1. they are always available
	 * for read / writing and 2. will not block.
	 * 
	 * The obvious exception is the "SelectNow" methods, which return immediately a
	 * simple true/false for whether a particular channel is available for a particular
	 * operation.  These methods are NOT intended for general Administration, but rather
	 * as a simple tool for quickly iterating through operations: for example, having selected
	 * that a particular Socket is ready for reads and writes, and having conducted the read,
	 * we probably sent an ACK.  It might be worth selecting again (using "SelectNow") to
	 * make sure we're still ready to write.
	 * 
	 * The Administrator should call this WS's helpers only once selection has determined
	 * that the associated operation is safe and appropriate.
	 * 
	 * To outside observers, though, this WrappedSocket will behave as if it
	 * is Autonomous.  Administration happens "behind the scenes" to make up
	 * for a lack of internal functionality in this WrappedSocket instance.
	 * 
	 * @author Jake
	 *
	 */
	private class AdministratedWrappedUDPSocketChannel extends WrappedSocket {

		// SHOTGUN: each time we send a message of this type, send it
		// a number of times equal to this value plus the number of previous
		// explicit sends (minimum 1).
		private final int [] START_SHOTGUN = new int[]{ 	-3,		1, 		3 } ;
		private final int [] MAX_SHOTGUN = new int[]{	 	2,		4, 		3 } ;
		//													 DATA,	 SYN,	 ACK
		
		private static final int MAX_MESSAGES_SENT_WITHOUT_ACK = 64 ;
		
		private static final int MAX_MESSAGES_IN_QUEUE = 64 ;
		
		// Constructor data: the underlying communication channel.
		private DatagramChannel mDatagramChannel ;
		private SocketAddress mDestinationAddress ;
		private byte [] mDatagramPrefix ;
		
		private boolean mClosedFromOutside ;
		private boolean mReleasedFromAdministration ;
		
		// Holders for datagrams: incoming and outgoing.  Reads and writes
		// are synchronized over these object.
		private ByteBuffer mDatagramSend ;
		private ByteBuffer mDatagramReceive ;
		
		// Sinks and Sources: we create these piped channels ourselves and
		// act as storage for them, but we NEVER read from or write to them
		// directly.  That's the administrator's job.
		// SEND: Data to send to a remote user.  These are the piped channel
		// 		ends for our "outgoing" data intended for another user.
		private Pipe.SinkChannel mSendSinkChannel ;
		private Pipe.SourceChannel mSendSourceChannel ;
		// RECEIVE: Data received from a remote user.  These are the piped channel
		// 		ends for our "incoming" data sent by another user.
		private Pipe.SinkChannel mReceiveSinkChannel ;
		private Pipe.SourceChannel mReceiveSourceChannel ;
		
		
		// MESSAGES: We store incoming and outgoing messages as ByteBuffers.
		// Outgoing messages get collected in a fixed-length array which
		// we cycle through by message number.
		private long mSentMessageCount = 0 ;
		private ByteBuffer [] mSentMessages ;
		private boolean [] mSentMessageWaitingForACK ;
		private long [] mSentMessageTimeLastSent ;
		private long [] mSentMessageTimeFirstSent ;
		private int [] mSentMessageNumberTimesSent ;
		private long mSentMessageTimeLastMessageSent ;
		
		// Incoming messages are collected in a queue, ordered by message number,
		// and dequeued into our ReceiveSinkChannel as needed.
		private ArrayList<ByteBuffer> mReceivedMessageQueue ;
		private ArrayList<Boolean> mReceivedMessageQueueIsValid ;
		private ByteBuffer mReceivedMessage ;
		private long mReceivedMessageNumberWaitingFor ;
		private long mReceivedMessageLastTimeReceived ;
		// ACKs are sent using this single ByteBuffer, holding a standard ACK message.
		private ByteBuffer mReceivedMessageACK ;
		
		
		// SELECTORS: As a convenience to the administrator, we have selectors
		// which offer immediate checks as to whether our write-to-pipe is available
		// for writing or the DatagramChannel available for writing.  These are
		// operations that we don't want to "constantly monitor," but rather, 
		// that we want to check only when data is available to write.
		private Selector mSelectorWriteToPipe ;
		private SelectionKey mSelectorWriteToPipeSelectionKey ;
		private Selector mSelectorWriteToDatagramChannel ;
		private SelectionKey mSelectorWriteToDatagramChannelSelectionKey ;
		
		
		// Meta (connection strength)
		private long mConnectionLastPing ;
		private double mConnectionAveragePing = -1 ;
		private static final double NEW_PING_WEIGHT = 0.2 ;
		private double mConnectionStrength ;
		private long mTotalBytesSent ;
		private long mTotalBytesReceived ;
		
		private AdministratedWrappedUDPSocketChannel( Class<?> messageClass, DatagramChannel channel, SocketAddress dest, byte [] prefix ) throws IOException {
			// Fill in all necessary data members, including
			// generating sink / source channels for users and administration.
			
			mDatagramChannel = channel ;
			mDestinationAddress = dest ;
			if ( prefix == null ) {
				mDatagramPrefix = new byte[0] ;
			} else {
				mDatagramPrefix = prefix.clone() ;
			}
			
			mClosedFromOutside = false ;
			mReleasedFromAdministration = false ;
			
			// Our datagram data-holders for send/receives.
			mDatagramSend = ByteBuffer.allocate(MAX_MESSAGE_LENGTH + mDatagramPrefix.length) ;
			mDatagramReceive = ByteBuffer.allocate(MAX_MESSAGE_LENGTH + mDatagramPrefix.length) ;
			
			// Piped sink/source for outside users to send data along this wrapped
			// socket.  We create and store references to this, and close them ourselves,
			// but we NEVER read from or write to them (that's the administrator's job).
			Pipe p_send = Pipe.open() ;
			mSendSinkChannel = p_send.sink() ;
			mSendSourceChannel = p_send.source() ;
			
			Pipe p_receive = Pipe.open() ;
			mReceiveSinkChannel = p_receive.sink() ;
			mReceiveSourceChannel = p_receive.source() ;
			
			
			// Space and bookkeeping for outgoing messages.
			mSentMessageCount = 0 ;
			mSentMessages = new ByteBuffer[MAX_MESSAGES_SENT_WITHOUT_ACK] ;
			mSentMessageWaitingForACK = new boolean[MAX_MESSAGES_SENT_WITHOUT_ACK] ;
			mSentMessageTimeLastSent = new long[MAX_MESSAGES_SENT_WITHOUT_ACK] ;
			mSentMessageTimeFirstSent = new long[MAX_MESSAGES_SENT_WITHOUT_ACK] ;
			mSentMessageNumberTimesSent = new int[MAX_MESSAGES_SENT_WITHOUT_ACK] ;
			for ( int i = 0; i < MAX_MESSAGES_SENT_WITHOUT_ACK; i++ ) {
				mSentMessages[i] = ByteBuffer.allocate(MAX_MESSAGE_LENGTH) ;
				mSentMessageWaitingForACK[i] = false ;
				mSentMessageTimeLastSent[i] = 0 ;
				mSentMessageTimeFirstSent[i] = 0 ;
				mSentMessageNumberTimesSent[i] = 0 ;
			}
			mSentMessageTimeLastMessageSent = 0 ;
			
			// Space and bookkeeping for incoming messages.
			mReceivedMessageQueue = new ArrayList<ByteBuffer>() ;
			mReceivedMessageQueueIsValid = new ArrayList<Boolean>() ;
			mReceivedMessage = ByteBuffer.allocate(MAX_MESSAGE_LENGTH) ;
			mReceivedMessageNumberWaitingFor = 0 ;
			mReceivedMessageLastTimeReceived = System.currentTimeMillis() ;
			mReceivedMessageACK = ByteBuffer.allocate(9) ;
			
			// We require all our internal Channels - those which we read
			// or write to, as opposed to those solely under outside control
			// - to be in non-blocking mode.  We intend to select on all of them.
			mDatagramChannel.configureBlocking(false) ;
			mReceiveSinkChannel.configureBlocking(false) ;
			mSendSourceChannel.configureBlocking(false) ;
			
			mSelectorWriteToDatagramChannel = Selector.open() ;
			mSelectorWriteToPipe = Selector.open() ;
			// register
			mSelectorWriteToDatagramChannelSelectionKey = 
				mDatagramChannel.register(mSelectorWriteToDatagramChannel, SelectionKey.OP_WRITE) ;
			mSelectorWriteToPipeSelectionKey = 
				mReceiveSinkChannel.register(mSelectorWriteToPipe, SelectionKey.OP_WRITE) ;
			
			mTotalBytesSent = 0 ;
			mTotalBytesReceived = 0 ;
		}
		
		
		private int messageNumToSentMessageIndex( long messageNum ) {
			return (int)(messageNum % mSentMessages.length) ;
		}
		
		
		/**
		 * Sends the data provided, which is a properly wrapped WS message, to
		 * our destination.
		 * 
		 * This method will block if the underlying socket channel is blocking, and
		 * will return whether or not the wrapped message was sent.  By the nature of
		 * DatagramChannels, a 'true' indicates that the entire message data was sent.
		 * 
		 * This method only 'blocks' if the underlying channel is blocking and is not
		 * ready for a send.
		 * 
		 * ADMINISTRATOR: Call this method only if our mDatagramChannel is ready for
		 * 		a WRITE operation.
		 * 
		 * @param wrappedMessage The message to send.  Its position will be adjusted by this call.
		 * @throws IOException 
		 */
		private boolean sendWrappedMessage( ByteBuffer wrappedMessage, int numSends ) throws IOException {
			synchronized( mDatagramSend ) {
				wrappedMessage.position(0) ;
				
				mDatagramSend.clear() ;
				mDatagramSend.put(mDatagramPrefix) ;
				mDatagramSend.put(wrappedMessage) ;
				mDatagramSend.flip() ;
				
				boolean didSend = false ;
				for ( int i = 0; i < numSends; i++ ) {
					int bytesSent = mDatagramChannel.send(mDatagramSend, mDestinationAddress) ;
					didSend = bytesSent > 0 || didSend ;
					mDatagramSend.position(0) ;
					mTotalBytesSent += bytesSent ;
					
					if ( didSend && !isACK(wrappedMessage) )
						mSentMessageTimeLastMessageSent = System.currentTimeMillis() ;
				}
				
				return didSend ;
			}
		}
		
		
		private int shotgunSentMessageByIndex( int index ) {
			return shotgun( getMessageType(mSentMessages[index]), mSentMessageNumberTimesSent[index] ) ;
		}
		
		
		private int shotgun( byte messageType, int numSends ) {
			return Math.min(MAX_SHOTGUN[messageType], Math.max(1, numSends + START_SHOTGUN[messageType])) ;
		}
		
		
		/**
		 * Receives from the underlying socket into the ByteBuffer provided a message
		 * on our channel.
		 * 
		 * This method will block if the underlying socket channel is blocking, and
		 * will return whether or not a wrapped message was received.  By the nature
		 * of DatagramChannels, a 'true' indicates that the entire message data was
		 * received.
		 * 
		 * This method will discard (and return 'false') datagrams which do not match
		 * our prefix.
		 * 
		 * IMPORTANT NOTE FOR ADMINISTRATION:
		 *		Android 2.3 has a SERIOUS bug where timeouts fail on blocking channels,
		 *		and non-blocking channels block forever!  There is no good way to
		 *		receive directly if we don't know there is a packet available!
		 *		Selection is ABSOLUTELY NECESSARY.  Only call this method if our DatagramChannel
		 *		has been selected as being ready for a READ operation.
		 *
		 * ADMINISTRATOR: Call this method only if our mDatagramChannel is ready for
		 * 		a READ operation.
		 * 
		 * @param wrappedMessage
		 * @return
		 * @throws IOException 
		 */
		private boolean receiveWrappedMessage( ByteBuffer wrappedMessage ) throws IOException {
			synchronized( mDatagramReceive ) {
				mDatagramReceive.clear() ;
				SocketAddress sa = mDatagramChannel.receive(mDatagramReceive) ;
				mDatagramReceive.flip() ;
				
				mTotalBytesReceived += mDatagramReceive.remaining() ;
				
				// check for address, prefix
				if ( sa == null )
					return false ;
				if ( mDatagramReceive.remaining() < mDatagramPrefix.length )
					return false ;
				boolean matchPrefix = true ;
				for ( int i = 0; i < mDatagramPrefix.length && matchPrefix; i++ ) {
					matchPrefix = matchPrefix && mDatagramPrefix[i] == mDatagramReceive.get(i) ;
				}
				if ( !matchPrefix )
					return false ;
				
				// transfer into the wrappedMessage buffer.
				mDatagramReceive.position(mDatagramPrefix.length) ;
				wrappedMessage.clear() ;
				wrappedMessage.put(mDatagramReceive) ;
				wrappedMessage.flip() ;
				
				mReceivedMessageLastTimeReceived = System.currentTimeMillis() ;
				return true ;
			}
		}
		
		private void noteAck(long milliseconds, int numAttempts) {
			mConnectionLastPing = milliseconds ;
			if ( mConnectionAveragePing == -1 ) {
				mConnectionAveragePing = mConnectionLastPing ;
				mConnectionStrength = 1.0 / numAttempts ;
			}
			else {
				mConnectionAveragePing = (long)(mConnectionAveragePing * (1 - NEW_PING_WEIGHT) + mConnectionLastPing * NEW_PING_WEIGHT) ;
				mConnectionStrength = mConnectionStrength * (1-NEW_PING_WEIGHT) + (1.0/numAttempts) * NEW_PING_WEIGHT ;
			}
		}
		
		
		/**
		 * Do we have space available to create a new, wrapped, outgoing message?
		 * This object has a maximum number of "outgoing" messages at one time,
		 * and these messages will remain in memory until acknowledged.  We keep
		 * these messages in-order.
		 * 
		 * If this method returns 'true', we have the space available to read an
		 * outgoing message from the user and prepare a wrapped message for send
		 * -- or, alternatively, to create a SYN message and add it to our outgoing
		 * message collection.
		 * 
		 * This method does NOT check for whether there is outgoing *data* available
		 * on our "Send" pipe, because such a check requires a Selector.
		 * 
		 * ADMINISTRATOR: Call this method to determine if it is worth including this
		 * 		WrappedSocket in a Selection on its Send pipe channel (looking for outgoing
		 * 		data).  
		 * 
		 * 		Call this method before adding a SYN message to our outgoing collection
		 * 		(iff it's time to send a SYN).
		 * 
		 * @return
		 */
		private boolean isReadyToWrapOutgoingMessage() {
			// check whether the next message num ('count') indicates
			// an available byte buffer.  A byte buffer is available if
			// we are NOT waiting for ACK.
			int index = messageNumToSentMessageIndex( this.mSentMessageCount ) ;
			return !mSentMessageWaitingForACK[index] ;
		}
		
		
		/**
		 * Performs a selectNow() operation on our internal Selector, which tells
		 * us whether the DatagramChannel is ready for a Write operation.
		 * 
		 * ADMINISTRATOR: Call this method when we have outgoing messages pending,
		 * 		to determine if a send is appropriate.
		 * 
		 * @return
		 * @throws IOException 
		 */
		private boolean isReadyToSendOutgoingMessage() throws IOException {
			mSelectorWriteToDatagramChannel.selectNow() ;
			Set<SelectionKey> selectedKeys = mSelectorWriteToDatagramChannel.selectedKeys() ;
			if ( selectedKeys.contains(mSelectorWriteToDatagramChannelSelectionKey) ) {
				selectedKeys.clear() ;
				return true ;
			}
			return false ;
		}
		
		private boolean hasOutgoingMessage() {
			for ( int i = 0; i < mSentMessageWaitingForACK.length; i++ ) {
				if ( mSentMessageWaitingForACK[i] )
					return true ;
			}
			return false ;
		}
		
		private boolean hasUnsentOutgoingMessage() {
			for ( int i = 0; i < mSentMessageWaitingForACK.length; i++ ) {
				if ( mSentMessageWaitingForACK[i] && mSentMessageNumberTimesSent[i] == 0 )
					return true ;
			}
			return false ;
		}
		
		
		/**
		 * Wraps a new outgoing message, including it in our message collection.
		 * This message contains data read from our "send data pipe."
		 * 
		 * ADMINISTRATOR: Call this method only after a Selector has determined
		 * 		that there is outgoing *data* available on our "Send" pipe,
		 * 		and only if hasSpaceToWrapOutgoingMessage() is true.
		 * 
		 * @throws IOException 
		 */
		private void wrapNewOutgoingDATAMessageForSend() throws IOException {
			if ( !isReadyToWrapOutgoingMessage() )
				throw new IllegalStateException("wrapOutgoingDATAMessageForSend was called, but have no space for outgoing message.") ;
			
			int index = messageNumToSentMessageIndex( this.mSentMessageCount ) ;
			ByteBuffer msg = mSentMessages[index] ;
			if ( setAsDATA( msg, mSentMessageCount, this.mSendSourceChannel ) ) {
				// mark the appropriate metadata.
				mSentMessageTimeLastSent[index] = 0 ;		// never sent
				mSentMessageTimeFirstSent[index] = 0 ;		// never sent
				mSentMessageNumberTimesSent[index] = 0 ;	// never sent
				mSentMessageWaitingForACK[index] = true ;
				
				// this message will now be sent the next time we trigger a send.
				
				// this is another message...
				mSentMessageCount++ ;
			}
		}
		
		/**
		 * Wraps a new outgoing SYN message, including it in our message collection.
		 * 
		 * This method does not touch any Selectable asset.
		 */
		private void wrapNewOutgoingSYNMessageForSend() {
			if ( !isReadyToWrapOutgoingMessage() )
				throw new IllegalStateException("wrapOutgoingSYNMessageForSend was called, but have no space for outgoing message.") ;
			
			int index = messageNumToSentMessageIndex( this.mSentMessageCount ) ;
			ByteBuffer msg = mSentMessages[index] ;
			setAsSYN( msg, mSentMessageCount ) ;
			// mark the appropriate metadata.
			mSentMessageTimeLastSent[index] = 0 ;		// never sent
			mSentMessageTimeFirstSent[index] = 0 ;		// never sent
			mSentMessageNumberTimesSent[index] = 0 ;	// never sent
			mSentMessageWaitingForACK[index] = true ;
			// this happens last: many methods look for this value
			// before checking the others.
			
			// this message will now be sent the next time we trigger a send.
			
			// this is another message...
			mSentMessageCount++ ;
		}
		
		
		/**
		 * Returns the number of messages which are presently in need of a send.
		 * 
		 * @param timeBetweenSends We need a resend iff more than 'timeBetweenSends'
		 * 		has passed since the last send of a message in need of acknowledgment.
		 * @return
		 */
		@SuppressWarnings("unused")
		private int getNumMessagesNeedingSend( long timeBetweenSends ) {
			long currentTime = System.currentTimeMillis() ;
			int num = 0 ;
			for ( int i = 0; i < mSentMessageWaitingForACK.length; i++ ) {
				if ( mSentMessageWaitingForACK[i] && mSentMessageTimeLastSent[i] + timeBetweenSends < currentTime ) {
					num++ ;
				}
			}
			
			return num ;
		}
		
		
		/**
		 * Returns minimum number of milliseconds after which we will have
		 * a message to resend (assuming no ACKs appear during that time).
		 * 
		 * ADMINISTRATOR: Call this method to determine the appropriate 'timeout'
		 * 		when waiting for new message data or incoming messages from
		 * 		a remote user.  After this amount of time, we will need to resend
		 * 		messages.
		 * 
		 * @return The time until a resend is necessary.  0 means we have resends to
		 * 		send immediately.  Returns -1 if we could wait indefinitely and
		 * 		never need to resend.
		 */
		private long getTimeUntilSendNeeded( long timeBetweenSends ) {
			long currentTime = System.currentTimeMillis() ;
			long minTime = Long.MAX_VALUE ;
			
			for ( int i = 0; i < mSentMessageWaitingForACK.length; i++ ) {
				if ( mSentMessageWaitingForACK[i] ) {
					long timeTill = mSentMessageTimeLastSent[i] + timeBetweenSends - currentTime ;
					minTime = Math.min(minTime, timeTill) ;
				}
			}
			
			if ( minTime < 0 )
				return 0 ;		// resend immediately
			else if ( minTime == Long.MAX_VALUE )
				return -1 ;		// no resends ever
			return minTime ;
		}
		
		
		/**
		 * Returns the estimated amount of time until we need to prepare an
		 * outgoing SYN message.  Will return -1 if outgoing messages already
		 * exist (and no SYN is necessary therefore).
		 * 
		 * @param timeBetweenMessages
		 * @return
		 */
		private long getTimeUntilSYNNeeded( long timeBetweenMessages ) {
			if ( this.hasOutgoingMessage() ) {
				return -1 ;
			}
			
			return Math.max(0, mSentMessageTimeLastMessageSent + timeBetweenMessages - System.currentTimeMillis() ) ;
		}
		
		
		/**
		 * Examines our messages still awaiting acknowledgement, selects one needing
		 * a resend most urgently, and sends it.
		 * 
		 * Our "urgency" policy: we pick the message with the fewest total sends.
		 * In the event of a tie, we pick the message which has gone the longest
		 * without a send.  If there is STILL a tie, take the lowest message number.
		 * 
		 * Note: this method does not check whether any messages are actually in need
		 * of a resend.  If some messages are, though, we guarantee that one of them
		 * will be sent.
		 * 
		 * ADMINISTRATOR: Only call this method if we have at least one message needing
		 * 		a send, AND a Selector has indicated that our DatagramChannel is ready
		 * 		for a WRITE.
		 * 
		 * @return
		 * @throws IOException 
		 */
		private boolean sendOneWrappedMessage() throws IOException {
			int mostUrgentIndex = -1 ;
			int mostUrgentNumSends = Integer.MAX_VALUE ;
			long mostUrgentTimeSinceSend = 0 ;
			long mostUrgentMessageNum = Long.MAX_VALUE ;
			
			long currentTime = System.currentTimeMillis() ;
			
			for ( int i = 0; i < mSentMessages.length; i++ ) {
				if ( this.mSentMessageWaitingForACK[i] ) {
					int numSends = this.mSentMessageNumberTimesSent[i] ;
					long timeSinceSend = currentTime - this.mSentMessageTimeLastSent[i] ;
					long messageNum = getMessageNum( mSentMessages[i] ) ;
					
					// more urgent?
					boolean urgent = false ;
					if ( numSends < mostUrgentNumSends )
						urgent = true ;
					else if ( numSends == mostUrgentNumSends ) {
						if ( timeSinceSend > mostUrgentTimeSinceSend )
							urgent = true ;
						else if ( timeSinceSend == mostUrgentTimeSinceSend ) {
							if ( messageNum < mostUrgentMessageNum ) {
								urgent = true ;
							}
						}
					}
					
					if ( urgent ) {
						mostUrgentIndex = i ;
						mostUrgentNumSends = numSends ;
						mostUrgentTimeSinceSend = timeSinceSend ;
						mostUrgentMessageNum = messageNum ;
					}
				}
			}
			
			if ( mostUrgentIndex > -1 ) {
				// send!
				return sendWrappedMessageWithIndex( mostUrgentIndex ) ;
			}
			
			return false ;
		}
		
		
		/**
		 * Sends all our currently wrapped outgoing messages (those still awaiting
		 * acknowledgment) that are in need of a send or resend, according to timeBetweenSends.
		 * 
		 * Returns the number of messages so sent.
		 * 
		 * Note: this "shotgun approach" method may end up sending multiple Datagrams on
		 * our DatagramChannel.  This is in violation of our usual "Select -> Send" policy
		 * of one operation (read/write) per select.
		 * 
		 * ADMINISTRATOR: Take great care calling this method.  It could potentially perform
		 * 		up to 64 consecutive Datagram sends on our channel before returning.
		 * 
		 * @param timeBetweenSends The amount of time we should wait before resending a message.
		 * 		If <= 0, no time is needed and we resend all messages.
		 * @return The number of messages successfully sent.
		 * @throws IOException 
		 */
		@SuppressWarnings("unused")
		private int sendAllWrappedMessages( long timeBetweenSends ) throws IOException {
			long currentTime = System.currentTimeMillis() ;
			int numSent = 0 ;
			for ( int i = 0; i < mSentMessages.length; i++ ) {
				if ( mSentMessageWaitingForACK[i] ) {
					// should we send or resend this message?
					long timeSinceLast = currentTime - this.mSentMessageTimeLastSent[i] ;
					if ( timeBetweenSends < 0 || timeSinceLast > timeBetweenSends ) {
						boolean sent = sendWrappedMessageWithIndex(i) ;
						if ( sent )
							numSent++ ;
					}
				}
			}
			
			return numSent ;
		}
		
		
		/**
		 * Should only be called internally as a send-message helper.  Performs bookkeeping
		 * and adjustment of metadata.
		 * 
		 * @param index
		 * @return
		 * @throws IOException 
		 */
		private boolean sendWrappedMessageWithIndex( int index ) throws IOException {
			if ( sendWrappedMessage( mSentMessages[index], shotgunSentMessageByIndex(index) ) ) {
				mSentMessageTimeLastSent[index] = System.currentTimeMillis() ;
				if ( mSentMessageNumberTimesSent[index] == 0 )
					mSentMessageTimeFirstSent[index] = System.currentTimeMillis() ;
				mSentMessageNumberTimesSent[index]++ ;
				//if ( mSentMessageNumberTimesSent[index] > 2 ) {
				//	log("sendWrappedMessageWithIndex sent message of type " + getMessageType(mSentMessages[index]) + " for the " + mSentMessageNumberTimesSent[index] + "th time " + (mSentMessageTimeLastSent[index] - mSentMessageTimeFirstSent[index]) + " after initial, avg. ping " + this.averagePing() + ", strength " + this.successRate()) ;
				//}
				// adjust number times sent last: we don't want a situation
				// where we EVERY (even for a single tick) think we have
				// sent a message but have an incorrect 'firstTimeSent' time.
				return true ;
			}
			return false ;
		}
		
		
		/**
		 * We are ready to receive if there is not a pending message waiting at the
		 * start of the queue.  If a valid message is waiting there, it means we
		 * have received (and ACKed, probably) a message that we were unable to
		 * send down the pipe (yet).  We don't want to risk accepting any more
		 * until this is consumed.
		 * 
		 * NOTE: possibly change this, so we can accept and only ACK if space is 
		 * available?
		 * 
		 * @return
		 */
		private boolean isReadyToReceiveIncomingMessage() {
			// TODO: alter this to indicate that space is available.
			// TODO: alter the relevant 'receiveWrapped' methods to only ACK
			// 			if we have space in the queue to store the message.
			return mReceivedMessageQueueIsValid.size() == 0 || !mReceivedMessageQueueIsValid.get(0) ;
		}
		
		
		/**
		 * Receives one wrapped message from our DatagramChannel, automatically sending
		 * an ACK in response.
		 * 
		 * Returns whether a message was received (and put in the queue for sending down
		 * the local incoming pipe).
		 * 
		 * ADMINISTRATOR: This message both receives from, and sends on, our DatagramChannel.
		 * 		Use this only if the channel is Selected for both READ and WRITE operations.
		 * 
		 * 		If 'true' is returned, we have written on the channel.  Otherwise, we have not.
		 * 
		 * @return
		 * @throws IOException 
		 */
		private boolean receiveOneWrappedMessageAndSendWrappedACK() throws IOException {
			if ( receiveWrappedMessage( mReceivedMessage ) ) {
				if ( isACK( mReceivedMessage ) ) {
					consumeReceivedWrappedMessageACK() ;
					return false ;
				} else {
					// a real message.
					long receivedNumber = getMessageNum( mReceivedMessage ) ;
					
					// send the ACK.
					sendNewWrappedMessageACK( receivedNumber ) ;
					
					// enqueue for DATA receive.
					enqueueReceivedWrappedMessageDATA() ;
					
					return true ;
				}
			}
			
			return false ;
		}
		
		
		/**
		 * Receives one wrapped message from our DatagramChannel.
		 * 
		 * The ACK is not sent.  Instead, this method returns the received
		 * message number (>= 0), which can be provided to sendNewWrappedMessageACK()
		 * by the caller to perform the acknowledgment.
		 * 
		 * ACK is EXTREMELY important.  Remember to send it.
		 * 
		 * ADMINISTRATOR: This message receives from our DatagramChannel.  Use this
		 * 		only if the channel has been selected for READ operation.
		 * 
		 * 		This method requires more use on your part, and more possibility of
		 * 		error, than receiveOnWrappedMessageAndSendWrappedACK().  Only
		 * 		use this method if 
		 * 		1. Our DatagramChannel has NOT been selected for WRITE, -or-
		 * 		2. Our DatagramChannel has been selected for WRITE but we intend
		 * 			to use that immediate WRITE for sending some other (non-ACK) message.
		 * 
		 * @return The message number of the received message, or -1 if SYN or DATA message
		 * 			was received.
		 * @throws IOException 
		 */
		@SuppressWarnings("unused")
		private long receiveOneWrappedMessageAndDelegateACKToCaller() throws IOException {
			if ( receiveWrappedMessage( mReceivedMessage ) ) {
				if ( isACK( mReceivedMessage ) ) {
					consumeReceivedWrappedMessageACK() ;
					return -1 ;
				} else {
					// a real message.
					long receivedNumber = getMessageNum( mReceivedMessage ) ;
					// This is where we would send an ACK -- except we don't.
					
					// enqueue for DATA receive.
					enqueueReceivedWrappedMessageDATA() ;
					
					return receivedNumber ;
				}
			}
			
			return -1 ;
		}
		
		
		/**
		 * A Helper method for consuming a received ACK.  The ACK is assumed to
		 * reside in mReceivedMessage.  We mark the appropriate message of ours
		 * as received.
		 */
		private void consumeReceivedWrappedMessageACK() {
			if ( !isACK( mReceivedMessage ) ) {
				throw new IllegalStateException("Received message is not an ACK.") ;
			}
			
			long msgNum = getMessageNum(mReceivedMessage) ;
			// check that it's within our 'sent message' collection.
			if ( msgNum >= 0 && msgNum < this.mSentMessageCount && msgNum >= this.mSentMessageCount - MAX_MESSAGES_SENT_WITHOUT_ACK ) {
				int msgIndex = this.messageNumToSentMessageIndex(msgNum) ;
				if ( this.mSentMessageWaitingForACK[msgIndex] ) {
					// verify message number then mark as ACKed.
					if ( msgNum == getMessageNum( this.mSentMessages[msgIndex] ) ) {
						mSentMessageWaitingForACK[msgIndex] = false ;
					}
					
					this.noteAck(
							System.currentTimeMillis() - mSentMessageTimeFirstSent[msgIndex],
							mSentMessageNumberTimesSent[msgIndex]) ;
				}
			}
			
			
		}
		
		
		/**
		 * A Helper method for enqueueing a received message.  The message received
		 * is assumed to be a SYN or DATA; it is an error to call this method otherwise.
		 * 
		 * After this call, mReceivedMessage no longer points to the received message
		 * data.  Extract whatever information you need from it before this call.
		 * 
		 * @throws IOException 
		 * 
		 */
		private boolean enqueueReceivedWrappedMessageDATA() throws IOException {
			if ( !isDATA( mReceivedMessage ) && !isSYN( mReceivedMessage ) )
				throw new IllegalStateException("Called enqueueReceivedWrappedMessageDATA, but received message is not DATA or SYN.") ;
				
			long receivedNumber = getMessageNum( mReceivedMessage ) ;
			// This is where we would send an ACK -- except we don't.
			
			long queuePositionL = receivedNumber - this.mReceivedMessageNumberWaitingFor ;
			if ( queuePositionL < MAX_MESSAGES_IN_QUEUE && queuePositionL >= 0 ) {
				int queuePosition = (int)queuePositionL ;
				while( queuePosition >= mReceivedMessageQueue.size() ) {
					mReceivedMessageQueue.add( ByteBuffer.allocate(MAX_MESSAGE_LENGTH) ) ;
					mReceivedMessageQueueIsValid.add(Boolean.FALSE) ;
				}
				
				//if ( mReceivedMessageQueueIsValid.get(queuePosition) ) {
				//	log("enqueueReceivedWrappedMessageDATA extra receive of type " + getMessageType(mReceivedMessage) + " with number " + getMessageNum(mReceivedMessage)) ;
				//}
				
				// queue is sufficiently long, and we have the position.  Insert our
				// received message.
				ByteBuffer bbTemp = mReceivedMessageQueue.get(queuePosition) ;
				mReceivedMessageQueue.set(queuePosition, mReceivedMessage) ;
				mReceivedMessageQueueIsValid.set(queuePosition, Boolean.TRUE) ;
				mReceivedMessage = bbTemp ;
				
				// One last thing: read through the queue, popping off SYN messages
				// until we reach the first DATA (or non-valid).
				if ( queuePosition == 0 ) {
					removeLeadingReceivedWrappedMessageSYNFromQueue() ;
				}
				
				return true ;
			}
			
			return false ;
		}
		
		
		private boolean removeLeadingReceivedWrappedMessageSYNFromQueue() {
			int numRemoved = 0 ;
			while( mReceivedMessageQueueIsValid.get(0).equals(Boolean.TRUE)
					&& isSYN(mReceivedMessageQueue.get(0) ) ) {
				// pop it and move to the back (mark invalid)
				mReceivedMessageNumberWaitingFor++ ;
				mReceivedMessageQueue.add( mReceivedMessageQueue.remove(0) ) ;
				mReceivedMessageQueueIsValid.remove(0) ;
				mReceivedMessageQueueIsValid.add(Boolean.FALSE) ;
				numRemoved++ ;
			}
			
			return numRemoved > 0 ;
		}
		
		
		/**
		 * Wraps and sends one ACK message to acknowledge the provided message number.
		 * 
		 * @param messageNum
		 * @return
		 * @throws IOException 
		 */
		private void sendNewWrappedMessageACK( long messageNum ) throws IOException {
			setAsACK(mReceivedMessageACK, messageNum) ;
			sendWrappedMessage(mReceivedMessageACK, shotgun(ACK, 0)) ;
		}
		
		
		private boolean hasIncomingMessageToUnwrap() {
			return mReceivedMessageQueueIsValid.size() > 0 && mReceivedMessageQueueIsValid.get(0) ;
		}
		
		
		private boolean isReadyToUnwrapIncomingMessage() throws IOException {
			if ( hasIncomingMessageToUnwrap() ) {
				mSelectorWriteToPipe.selectNow() ;
				Set<SelectionKey> selectedKeys = mSelectorWriteToPipe.selectedKeys() ;
				if ( selectedKeys.contains(mSelectorWriteToPipeSelectionKey) ) {
					selectedKeys.clear() ;
					return true ;
				}
			}
			return false ;
		}
		
		
		/**
		 * Unwraps the first DATA message in our incoming queue.  If SYNs are in place
		 * before that, we silently discard as many as we need to.
		 * 
		 * Returns whether a DATA message has been written to our incoming Pipe.
		 * 
		 * ADMINISTRATOR: This method writes to our incoming sink Pipe.  Only call
		 * 		if a Selector indicates that that channel is ready for a WRITE operation.
		 * 
		 * 		That Write has been performed iff 'true' is returned.
		 * 
		 * It is an error to call this method when there is no incoming message to unwrap.
		 * 
		 * @param force Loop until the write completes.  We guarantee not to 'incompletely
		 * 		write', but this method might write nothing if the channel is non-blocking
		 * 		and unavailable.  Set 'force' to guarantee a full write.
		 * @throws IOException 
		 */
		private boolean unwrapOneIncomingDATAMessageFromQueue( boolean force ) throws IOException {
			if ( !hasIncomingMessageToUnwrap() )
				throw new IllegalStateException("unwrapOneIncomingDATAMessageFromQueue was called, but have no messages for unwrapping.") ;
			
			// read through the queue, looking for a DATA message.  Discard
			// SYNs as we go.
			while( mReceivedMessageQueueIsValid.get(0).equals(Boolean.TRUE) ) {
				ByteBuffer msg = mReceivedMessageQueue.get(0) ;
				boolean isData = isDATA( msg ) ;
				boolean isWritten = false ;
				
				if ( isData ) {
					// now write to pipe.  Force the entire write.
					isWritten = getMessageDATA(msg, force, mReceiveSinkChannel) ;
				}
				
				// dequeue and advance current queue if we're done with this message:
				// if it is not data (is a SYN) or was successfully written.
				if ( isWritten || !isData ) {
					mReceivedMessageNumberWaitingFor++ ;
					mReceivedMessageQueue.add( mReceivedMessageQueue.remove(0) ) ;
					mReceivedMessageQueueIsValid.remove(0) ;
					mReceivedMessageQueueIsValid.add(Boolean.FALSE) ;
				}
				
				if ( isData  )
					return isWritten ;
			}
			
			return false ;
		}
		
		
		/**
		 * Unwraps all DATA messages in our incoming queue.  If SYNs are in place
		 * in the queue, we discard all we find.
		 * 
		 * Returns the number of DATA message that have been written to our incoming Pipe.
		 * 
		 * ADMINISTRATOR: This method writes to our incoming sink Pipe.  Only call
		 * 		if a Selector indicates that that channel is ready for a WRITE operation.
		 * 
		 * 		NOTE: We may well make many more writes than 1 by this call, and will block
		 * 			until all writes are complete.  Calling this method is therefore dangerous.
		 * 
		 * It is an error to call this method when there is no incoming message to unwrap.
		 * 
		 * @param force If true, we force all messages to be written.  Otherwise, we
		 * 			terminate the first time a (non-blocking) pipe refuses the write attempt.
		 * 			In both cases we guarantee that "partial writes" will not occur; each message
		 * 			will be written in its entirety or not written at all.
		 * @throws IOException 
		 */
		@SuppressWarnings("unused")
		private int unwrapAllIncomingDATAMessagesFromQueue( boolean force ) throws IOException {
			if ( !hasIncomingMessageToUnwrap() )
				throw new IllegalStateException("unwrapOneIncomingDATAMessageFromQueue was called, but have no messages for unwrapping.") ;
			
			int numDATAMessages = 0 ;
			
			// read through the queue, looking for a DATA message.  Discard
			// SYNs as we go.
			while( mReceivedMessageQueueIsValid.get(0).equals(Boolean.TRUE) ) {
				ByteBuffer msg = mReceivedMessageQueue.get(0) ;
				boolean isData = isDATA( msg ) ;
				boolean isWritten = false ;
				
				if ( isData ) {
					// now write to pipe.  Force the entire write.
					isWritten = getMessageDATA(msg, force, mReceiveSinkChannel) ;
				}
				
				// dequeue and advance current queue if we're done with this message:
				// if it is not data (is a SYN) or was successfully written.
				if ( isWritten || !isData ) {
					mReceivedMessageNumberWaitingFor++ ;
					mReceivedMessageQueue.add( mReceivedMessageQueue.remove(0) ) ;
					mReceivedMessageQueueIsValid.remove(0) ;
					mReceivedMessageQueueIsValid.add(Boolean.FALSE) ;
				}
				
				if ( isData ) {
					if ( isWritten )
						numDATAMessages++ ;
					else
						return numDATAMessages ;
				}
			}
			
			return numDATAMessages ;
		}
		

		@Override
		public InetAddress getInetAddress() {
			return mDatagramChannel.socket().getInetAddress() ;
		}

		@Override
		public InetAddress getLocalAddress() {
			return mDatagramChannel.socket().getLocalAddress() ;
		}

		@Override
		public int getLocalPort() {
			return mDatagramChannel.socket().getLocalPort() ;
		}

		@Override
		public SocketAddress getLocalSocketAddress() {
			return mDatagramChannel.socket().getLocalSocketAddress() ;
		}

		@Override
		public int getPort() {
			return mDatagramChannel.socket().getPort() ;
		}

		@Override
		public SocketAddress getRemoteSocketAddress() {
			return mDatagramChannel.socket().getRemoteSocketAddress() ;
		}

		@Override
		public boolean isBound() {
			return mDatagramChannel.socket().isBound();
		}

		@Override
		public boolean isClosed() {
			return mClosedFromOutside || mDatagramChannel.socket().isClosed();
		}

		@Override
		public boolean isConnected() {
			// A UDP socket is considered connected unless it is closed, or
			// has been idle for a long time.
			if ( !this.isClosed() && this.timeSinceLastReceived() > 20000 ) {
				log("Closing socket -- time since received is " + this.timeSinceLastReceived()) ;
				this.close();
			}
			
			return !this.isClosed() ;
		}

		@Override
		public long timeSinceLastReceived() {
			return System.currentTimeMillis() - mReceivedMessageLastTimeReceived ;
		}

		@Override
		public long timeWaitingForAck() {
			long maxTime = 0 ;
			long curTime = System.currentTimeMillis() ;
			for ( int i = 0; i < mSentMessageWaitingForACK.length; i++ ) {
				if ( mSentMessageWaitingForACK[i] && this.mSentMessageNumberTimesSent[i] > 0 ) {
					maxTime = Math.max( maxTime, curTime - mSentMessageTimeFirstSent[i] ) ;
				}
			}
			return maxTime ;
		}

		@Override
		public long latestPing() {
			return mConnectionLastPing ;
		}

		@Override
		public long averagePing() {
			return Math.round(mConnectionAveragePing) ;
		}

		@Override
		public double successRate() {
			return mConnectionStrength ;
		}
		
		@Override
		public long bytesSent() {
			return mTotalBytesSent ;
		}
		
		@Override
		public long bytesReceived() {
			return mTotalBytesReceived ;
		}

		@Override
		public Pipe.SourceChannel getSourceChannel() {
			return mReceiveSourceChannel ;
		}

		@Override
		public Pipe.SinkChannel getSinkChannel() {
			return mSendSinkChannel ;
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

		@Override
		public void close() {
			close(false, 0) ;
		}

		@Override
		public void close(boolean flushOutgoing, long maximumWait) {
			log(new Exception(), "close called from somewhere with flush " + flushOutgoing + " wait " + maximumWait) ;
			if ( mClosedFromOutside )
				return ;
			mClosedFromOutside = true ;
			if ( flushOutgoing ) {
				try {
					// Wait until no longer administrated (if we were).
					long timeWaitBegan = System.currentTimeMillis() ;
					while ( maximumWait < 0 || (System.currentTimeMillis() - timeWaitBegan) < maximumWait ) {
						if ( mReleasedFromAdministration ) {
							// First: try sending any unsent outgoing messages.
							// If any unsent messages exist, they always get
							// top priority for sends.
							if ( hasUnsentOutgoingMessage() ) {
								this.sendOneWrappedMessage() ;
							} else {
								// try reading from pipe / writing to DatagramChannel.
								// We will no longer re-send, SYN, or care about or 
								// current queue of messages.  Just read into a byte buffer.
								ByteBuffer bb = this.mSentMessages[0] ;
								if ( setAsDATA(bb, this.mSentMessageCount, mSendSourceChannel) ) {
									// send!
									sendWrappedMessage(bb, 1) ;
									mSentMessageCount++ ;
								}
							}
						}
						
						// delay a bit.  If this throws an InterruptedException, we quit out
						// of our flush attempt.
						long sleepTime = mReleasedFromAdministration ? 10 : 100 ;
						long timeRemaining = System.currentTimeMillis() - timeWaitBegan - maximumWait ;
						sleepTime = Math.min(sleepTime, timeRemaining) ;
						Thread.sleep(sleepTime) ;
					}
				} catch ( Exception e ) {
					// nothing; final flush failed.
				}
			}
			
			// close our pipes
			try {
				this.mReceiveSinkChannel.close() ;
			} catch( Exception e ) { }
			
			try {
				this.mReceiveSourceChannel.close() ;
			} catch( Exception e ) { }
			
			try {
				this.mSendSinkChannel.close() ;
			} catch( Exception e ) { }
			
			try {
				this.mSendSourceChannel.close() ;
			} catch( Exception e ) { }
			
			
			
			// close our datagram channel
			try {
				mDatagramChannel.close() ;
			} catch( Exception e ) { }
			
			// close selectors
			try {
				this.mSelectorWriteToDatagramChannel.close() ;
			} catch ( Exception e ) { }
			try {
				this.mSelectorWriteToPipe.close() ;
			} catch ( Exception e ) { }
		}
		
	}
	
}
