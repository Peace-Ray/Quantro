package com.peaceray.quantro.communications.messagepassing.matchseeker;

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.AdministratedWrappedSocketAdministrator;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.InternetLobbyGame;
import com.peaceray.quantro.lobby.MatchTicketOrder;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;


/**
 * Earlier implementations of MessagePassingMatchSeekerConnection used their
 * own local management of MatchTicketAcquisitionThread, MatchSeeker, and 
 * IdentityExchangeThread, which allow very granular control by the Connection.
 * However, a major weakness of this approach was the possibility for different
 * Connections to attempt to connect simultaneously, trampling over each other
 * in terms of port and requests to the Matchseeker.
 * 
 * We've already addressed this problem in an (untested) approach used for WiFi
 * connections, which listen for requests on a single TCP server socket and provides
 * WrappedSockets to match seekers.
 * 
 * MatchSeeking uses UDP, but the principle is the same.  Connections will fail
 * if we try using the same local port each time, and multiple matchseekers will
 * overrun each other.  Finally, MatchTickets can be applied multiple times to
 * connect to multiple users in a row, so there's no need for each to independently
 * attempt a matchseeking effort deally.
 * 
 * @author Jake
 *
 */
public class MatchSeekingListener {
	
	private static final boolean DEBUG_LOG = true ;
	private static final String TAG = "MatchSeekingListener" ;
	
	private static final void log(RequesterData rd, String str) {
		if ( DEBUG_LOG ) {
			System.out.println(TAG + " " + requesterTag(rd) + " : " + str) ;
		}
	}
	
	private static final void log(RequesterData rd, Exception e, String str) {
		if ( DEBUG_LOG ) {
			System.err.println(TAG + " " + requesterTag(rd) + " : " + str + " : exception below") ;
			e.printStackTrace() ;
		}
	}
	
	private static final String requesterTag(RequesterData rd) {
		if ( rd == null )
			return "MISSINGNO" ;
		
		InternetLobby lobby = rd.mRequester.mslr_getInternetLobby(rd.mSerial) ;
		InternetLobbyGame game = rd.mRequester.mslr_getInternetLobbyGame(rd.mSerial) ;
		
		if ( lobby != null )
			return "Lobby (" + rd.mSerial + ")" ;
		else if ( game != null )
			return "Game (" + rd.mSerial + ")" ;
		return "NoInternetObject (" + rd.mSerial + ")" ;
	}
	
	public interface Requester {
		
		
		/**
		 * Provides the match found.  Returns whether the match was accepted.
		 * 
		 * If 'false', we will attempt to close the wrapped socket.  Otherwise,
		 * it is the callees responsibility.
		 * 
		 * @param msl
		 * @param ws
		 * @param name
		 * @param personalNonce
		 * @return
		 */
		public boolean mslr_matchFound( long requestSerial, Nonce sessionNonce, WrappedSocket ws, String name, Nonce personalNonce ) ;
		
		/**
		 * We failed to find a match.  The failure type is reported.  For web communication
		 * errors, 'error' and 'reason' are given as well.
		 * 
		 * If 'recoverable' is false, we cannot proceed, regardless of the return value.s
		 * 
		 * @param msl
		 * @return Whether we should re-attempt.  We might move this connection to the back of the queue,
		 * 		or we might continue exactly as we were.  If 'recoverable' is false the return value
		 * 		will be ignored.
		 */
		public boolean mslr_matchFailed( long requestSerial, Nonce sessionNonce, boolean recoverable, int failure, int error, int reason ) ;
		
		
		/**
		 * We provide updates as we move along the process.
		 * 
		 * @param msl
		 * @param update
		 */
		public void mslr_matchUpdate( long requestSerial, int update ) ;
		
		
		/**
		 * Returns whether we are still interested in finding a match.
		 * This method might be called rather often.
		 * 
		 * @param requestSerial the serial number of this request
		 * @return If 'true', we continue processing the request.  If 'false'
		 * 		we stop processing the request, and in fact will never call
		 * 		either mslr_matchFound or mslr_matchFailed.
		 */
		public boolean mslr_requesting( long requestSerial ) ;
		
		
		/**
		 * Returns 'null' or a reference to the associated InternetLobby.
		 * 
		 * Requesters are required to return non-null for EVERY call of either
		 * this or getInternetLobbyGame (but only ONE of those two methods!)
		 * 
		 * @param msl
		 * @return
		 */
		public InternetLobby mslr_getInternetLobby( long requestSerial ) ;
		
		/**
		 * Returns 'null' or a reference to the associated ILG.
		 * 
		 * Requesters are required to return non-null for EVERY call of either
		 * this or getInternetLobby (but only ONE of those two methods!)
		 * 
		 * @param msl
		 * @return
		 */
		public InternetLobbyGame mslr_getInternetLobbyGame( long requestSerial ) ;
		
		
		public Nonce mslr_getPersonalNonce( long requestSerial ) ;
		
		public String mslr_getName( long requestSerial ) ;
		
		public byte mslr_getIntent( long requestSerial ) ;
		
		
		public String mslr_getUserID( long requestSerial ) ;
		
		
		public String mslr_getXL( long requestSerial ) ;
		
		
		public SocketAddress mslr_getMatchmakerAddress( long requestSerial ) ;
		
		/**
		 * Returns the range of ports within which we should attempt to seek
		 * a match.  We will select an open port within this range.
		 * 
		 * This method will be called once per attempt to connect to the matchseeker;
		 * it is therefore acceptable for the port range to change between calls
		 * (so long as the actual array instance returned is not altered).
		 * 
		 * @param msl
		 * @return
		 */
		public int [] mslr_getCommunicationPortRange( long requestSerial ) ;
		
		
		/**
		 * Returns the Administrator to use for wrapping a DatagramChannel.
		 * 
		 * If 'null', we instead wrap the socket in its own Autonomous
		 * WrappedSocket.
		 * 
		 * @param requestSerial
		 * @return
		 */
		public AdministratedWrappedSocketAdministrator mslr_getWrappedSocketAdministrator( long requestSerial ) ;
		
		
		/**
		 * Returns the message class to be used by this listener.
		 * @return
		 */
		public Class<?> mslr_getMessageClass() ;
	}
	
	
	private static class RequesterData {
		Requester mRequester ;
		long mSerial ;
		int mHolePunchFailures ;
		
		private RequesterData( Requester r, long serial ) {
			mRequester = r ;
			mSerial = serial ;
			mHolePunchFailures = 0 ;
		}
		
		private RequesterData( Requester r, long serial, int holePunchFailures ) {
			mRequester = r ;
			mSerial = serial ;
			mHolePunchFailures = holePunchFailures ;
		}
		
		
		
		private Requester getRequester() {
			return mRequester ;
		}
		
		private long getSerial() {
			return mSerial ;
		}
		
		private int getHolePunchFailures() {
			return mHolePunchFailures ;
		}
	}
	
	
	public static final int FAILURE_UNRESPONSIVE_REQUESTER 	= 0 ;		// requester did not correctly respond to queries.
	public static final int FAILURE_MATCH_TICKET_REQUEST_COMMUNICATION = 1 ;	// communication failure getting a match ticket.
	public static final int FAILURE_MATCHSEEKER_CONTACT = 2 ;		// some major failure when connecting to the matchseeker.
	public static final int FAILURE_MATCHSEEKER_REJECTION = 3 ;		// a rejection from the MatchSeeker.  We provide the Delegate.REJECTION_* as the 'error.'
	public static final int FAILURE_MATCHSEEKER_EXCEPTION = 4 ;
	public static final int FAILURE_MATCHED_SOCKET_MAKER_FAILED = 5 ;
	public static final int FAILURE_IDENTITY_EXCHANGE_FAILED = 6 ;
	
	
	
	// This 
	public static final int UPDATE_STARTING 				= 0 ;
	public static final int UPDATE_REQUESTING_MATCH_TICKET	= 1 ;
	public static final int UPDATE_RECEIVED_MATCH_TICKET 	= 2 ;
	public static final int UPDATE_PERFORMING_MATCH_TICKET_WORK = 3 ;
	public static final int UPDATE_FINISHED_MATCH_TICKET_WORK = 4 ;
	public static final int UPDATE_SENDING_MATCH_REQUEST	= 5 ;
	public static final int UPDATE_RECEIVED_MATCH_PROMISE 	= 6 ;
	public static final int UPDATE_RECEIVED_MATCH 			= 7 ;
	public static final int UPDATE_MAKING_MATCHED_SOCKET	= 8 ;
	public static final int UPDATE_MADE_MATCHED_SOCKET		= 9 ;
	public static final int UPDATE_STARTING_INFORMATION_EXCHANGE	= 10 ;
	public static final int UPDATE_FINISHED_INFORMATION_EXCHANGE	= 11 ;
	
	
	
	
	
	// We instantiate, start, and stop DirectClientPortListenerThreads as needed.
	// When one is running, we place it in the HashTable (keyed by Integer port
	// number).
	//
	// We synchronize access in two steps.  First we lock the hash table itself,
	// to make sure no other threads perform concurrent edits.  Second we check
	// for an entry and, if found, we lock on it before checking its number of
	// connections and whether it is currently running.
	
	private static Hashtable<Nonce, MatchSeekingListener> mySessionInstances = new Hashtable<Nonce, MatchSeekingListener>() ;
	private static long myNextSerial = 0 ;
	
	
	// These are the requesters we're waiting on.
	private ArrayList<RequesterData> mRequesters = new ArrayList<RequesterData>() ;
	// Our current thread.  Spins and spins until no listeners left.
	private MatchSeekingListenerThread mThread = null ;
	private MatchTicketOrder mMatchTicketOrder = null ;
	private String mMatchTicketOrderUserID = null ;
	private Nonce mSessionNonce = null ;
	
	
	private MatchSeekingListener( Nonce sessionNonce ) {
		mSessionNonce = sessionNonce ;
	}
	
	
	/**
	 * Starts a request.
	 * 
	 * It is CRITICAL that different requesters provide different
	 * session nonces.
	 * 
	 * Session Nonces are used to determine when a new MatchSeeking
	 * thread is needed.  We assume that for a given session,
	 * only one match seeking attempt is needed at a time, **even
	 * if multiple requests are made**.  In other words, if multiple
	 * requests are made at once, they will be handled in sequence
	 * and the results returned in request order.  This works great
	 * for (e.g.) large lobby hosts, because only one slot is "open"
	 * at a time, and when it is filled the next slot can be immediately
	 * opened.
	 * 
	 * However, consider launching a game at the same time?  If the
	 * game connection is placed in the same queue, it will wait
	 * until the lobby is completely full before connecting!  Unacceptable.
	 * 
	 * @param requester
	 * @return A unique serial number used for all interaction.  Feel free to ignore
	 * 		or refuse interaction which does not provide this serial number.
	 */
	public static long request( Requester requester, Nonce sessionNonce ) {
		synchronized( mySessionInstances ) {
			MatchSeekingListener instance = mySessionInstances.get(sessionNonce) ;
			
			if ( instance == null ) {
				instance = new MatchSeekingListener(sessionNonce) ;
				mySessionInstances.put(sessionNonce, instance) ;
			}
			
			MatchSeekingListenerThread thread = instance.mThread ;
			RequesterData rd = new RequesterData( requester, myNextSerial++ ) ;
			
			boolean inserted = false ;
			
			synchronized( instance ) {
			
				if ( thread != null ) {
					if ( thread.mRunning ) {
						instance.mRequesters.add(rd) ;
						inserted = true ;
					}
				} 
				
				if ( !inserted ) {
					// either there is no thread, or it isn't running.  Either way
					// we need to start a new thread going.
					// First: add the requester to our list.
					instance.mRequesters.add(rd) ;
					// Second: make the thread.
					instance.mThread = instance.new MatchSeekingListenerThread() ;
					// Third: start the thread.
					instance.mThread.start() ;
					// Done.
				}
			}
			
			// Now's a good time to clear out old Instances.
			// WARNING: for unknown reasons, this method causes connection problems.
			// We have changed it to now check for both non-running threads and
			// empty requesters, AND it is synchronized within mySessionInstances,
			// as is all access to that structure.
			removeDeadListeners() ;
			
			return rd.getSerial() ;
		}
	}
	
	
	/**
	 * Technically speaking this method is unnecessary, because requesters
	 * are required to implement 'mslr_requesting.'  Nice requesters will
	 * call this, though, so we don't waste time on them.
	 * 
	 * @param requester
	 */
	public static void unrequest( Requester requester, Nonce sessionNonce, long serial ) {
		synchronized( mySessionInstances ) {
			// which listener?
			MatchSeekingListener instance = mySessionInstances.get(sessionNonce) ;
			
			if ( instance == null )
				return ;
			
			// if possible, we synchronize on the thread for this.
			MatchSeekingListenerThread thread = instance.mThread ;
			
			if ( thread != null ) {
				synchronized( instance ) {
					if ( thread.mRunning ) {
						for ( int i = 0; i < instance.mRequesters.size(); i++ ) {
							RequesterData rd = instance.mRequesters.get(i) ;
							if ( rd.getRequester() == requester && rd.getSerial() == serial ) {
								instance.mRequesters.remove(i) ;
								return ;	// found
							}
						}
						return ;	// not found
					}
				}
			}
			
			for ( int i = 0; i < instance.mRequesters.size(); i++ ) {
				RequesterData rd = instance.mRequesters.get(i) ;
				if ( rd.getRequester() == requester && rd.getSerial() == serial ) {
					instance.mRequesters.remove(i) ;
					return ;	// found
				}
			}
		}
	}
	
	
	protected static void removeDeadListeners() {
		synchronized( mySessionInstances ) {
			// Now's a good time to clear out old Instances.
			ArrayList<Nonce> removeKeys = new ArrayList<Nonce>() ;
			Set<Entry<Nonce, MatchSeekingListener>> listeners = mySessionInstances.entrySet() ;
			Iterator<Entry<Nonce, MatchSeekingListener>> entry_iter = listeners.iterator() ;
			for ( ; entry_iter.hasNext() ; ) {
				Entry<Nonce, MatchSeekingListener> entry = entry_iter.next() ;
				MatchSeekingListener instance = entry.getValue() ;
				synchronized( instance ) {
					if ( !instance.mThread.mRunning && instance.mRequesters.size() == 0 ) {
						removeKeys.add(entry.getKey()) ;
					} else if ( !instance.mThread.mRunning ) {
						instance.mThread = instance.new MatchSeekingListenerThread() ;
						// Third: start the thread.
						instance.mThread.start() ;
					}
				}
			}
			Iterator<Nonce> iter = removeKeys.iterator() ;
			for ( ; iter.hasNext() ; ) {
				mySessionInstances.remove( iter.next() ) ;
			}
		}
	}
	
	
	
	private class MatchSeekingListenerThread extends Thread {
		
		boolean mRunning = true ;
		
		RequesterData mRequesterData = null ;

		MatchData mMatchData ;
		MatchedSocketData mMatchedSocketData ;
		IdentityExchangeData mIdentityExchangeData ;
		
		public void run() {
			while ( mRunning ) {
				
				// put this all in a try.  We null out any of
				// our instance vars in the finally block.
				try {
					//System.err.println("MatchSeekingListener loop start") ;
					// get a requester!
					byte intent ;
					Nonce nonce ;
					Nonce personalNonce ;
					String name ;
					SocketAddress matchmakerAddress ;
					String userID ;
					synchronized( MatchSeekingListener.this ) {
						// pop 
						if ( MatchSeekingListener.this.mRequesters.size() == 0 ) {
							log(null, "No requesters: stopping") ;
							//System.err.println("MatchSeekingListener no requesters") ;
							mRunning = false ;
							continue ;
						}
						mRequesterData = MatchSeekingListener.this.mRequesters.remove(0) ;
						intent = mRequesterData.mRequester.mslr_getIntent(mRequesterData.mSerial) ;
						nonce = getNonce( mRequesterData.mRequester, mRequesterData.mSerial ) ;
						personalNonce = mRequesterData.mRequester.mslr_getPersonalNonce(mRequesterData.mSerial) ;
						name = mRequesterData.mRequester.mslr_getName(mRequesterData.mSerial) ;
						matchmakerAddress = mRequesterData.mRequester.mslr_getMatchmakerAddress(mRequesterData.mSerial) ;
						userID = mRequesterData.mRequester.mslr_getUserID(mRequesterData.mSerial) ;
						//System.err.println("MatchSeekingListener " + rd) ;
					}
					
					if ( nonce == null || personalNonce == null || userID == null ) {
						log(mRequesterData, "failed to respond") ;
						mRequesterData.mRequester.mslr_matchFailed(mRequesterData.mSerial, mSessionNonce, false, FAILURE_UNRESPONSIVE_REQUESTER, 0, 0) ;
						continue ;
					}
					
					// now we attempt to fulfill its request.
					if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
						log(mRequesterData, "no longer requesting") ;
						//System.err.println("MatchSeekingListener no longer requesting") ;
						continue ;
					}
					
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_STARTING) ;
					
					
					////////////////////////////////////////////////////////////////
					//
					// MATCHTICKET
					//
					
					// First: we get a matchticket, if we don't have one already.
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_REQUESTING_MATCH_TICKET) ;
					if ( mMatchTicketOrder == null || !userID.equals(mMatchTicketOrderUserID) ) {
						log(mRequesterData, "getting a fresh match ticket") ;
						try {
							mMatchTicketOrder = getMatchTicket( mRequesterData.mRequester, mRequesterData.mSerial, userID ) ;
							if ( mMatchTicketOrder == null )
								continue ;		// requester doesn't want a match anymore.
							mMatchTicketOrderUserID = userID ;
							mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_RECEIVED_MATCH_TICKET) ;
						} catch (IllegalArgumentException e) {
							log(mRequesterData, e, "requester being a jerk") ;
							// the requester is being a jerk.
							mRequesterData.mRequester.mslr_matchFailed(
									mRequesterData.mSerial, mSessionNonce, false, FAILURE_UNRESPONSIVE_REQUESTER, 0, 0) ;
							continue ;		// try next mRequesterData.mRequester.
						} catch (CommunicationErrorException e) {
							log(mRequesterData, e, "communication error when getting match ticket") ;
							// whoops.  we can, however, recover from communication errors.
							try {
								Thread.sleep(4000) ;		// easy on the server there...
							} catch (InterruptedException e1) { }
							boolean retry = mRequesterData.mRequester.mslr_matchFailed(
									mRequesterData.mSerial, mSessionNonce, true, FAILURE_MATCH_TICKET_REQUEST_COMMUNICATION,
									e.getError(), e.getErrorReason()) ;
							if ( retry )
								MatchSeekingListener.this.mRequesters.add(new RequesterData(mRequesterData.mRequester, mRequesterData.mSerial, mRequesterData.mHolePunchFailures)) ;
							continue ;		// try next mRequesterData.mRequester.
						}
					}
					
					// Now perform the work (if needed).
					log(mRequesterData, "performing work (if needed)") ;
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_PERFORMING_MATCH_TICKET_WORK) ;
					while ( !mMatchTicketOrder.isComplete() ) {
						if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
							log(mRequesterData, "no longer requesting") ;
							break ;
						}
						mMatchTicketOrder.performWork(1000) ;		// 1 second of work
					}
					
					// if work is still not complete, the requester wants to stop.
					if ( !mMatchTicketOrder.isComplete() )
						continue ;
					
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_FINISHED_MATCH_TICKET_WORK) ;
					
					//
					////////////////////////////////////////////////////////////////
					
					
					
					////////////////////////////////////////////////////////////////
					//
					// MATCH SEEK
					//
					
					// matchseeker is currently implemented as a thread with callbacks.
					// That might be confusing to implement here.  However, running the
					// thread and "spinning" until we get a response might be the best
					// solution, because we can remain relatively responsive to requesters
					// that give up their connection ambitions.
					//
					// We need to be very careful in keeping track of server responses.
					// Since we register ourself as the delegate we will get announcement
					// callbacks.  Further complication comes from the huge number of callbacks
					// the MessagePassingConnection expects to see.
					//
					// MatchSeeker allows for two different Delegate interfaces; we prefer
					// the simpler one, "Delegate".  We have two options.  First, create
					// and start() a new MatchSeeker with ourself as the delegate, then spin
					// (Thread.sleep()) while we wait for it to complete or report a
					// nonrecoverable failure.  Second, create and run() a new MatchSeeker
					// so its run-loop occurs on this thread.  We still need to provide
					// delegate callbacks.
					//
					// Approach A:
					//		+ We can remain responsive to cancelations and stops, since our .sleep()
					//			can be interrupted.
					//		+ This is a well-tested use case for MatchSeeker.
					//		+ Failures and exceptions in MatchSeeker do not break our working thread.
					//		- Starts a new thread.
					//		- Complicates communication and interaction.
					//		- Requires "self communication" regarding callbacks and such.
					//
					// Approach B:
					//		+ No extra thread.
					//		+ Inlined Delegate declaration allows clear code locality.
					//		+ run() call means we don't spin; we progress naturally when run()
					//			ends.
					//		+ We know EXACTLY when the operation ends.  No risk of a thread
					//			not terminating.
					//		+ Failures and exceptions in MatchSeeker can be wrapped in a try / catch
					//			and handled immediately by local code.
					//		- Direct calls to run() are untested.
					//		- No guarantee of responsiveness to cancel calls, and very infrequent
					//			queries to requesting().
					//
					// Despite the downsides, I think I'm going with approach B.
					//		
					//		
					//
					
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_SENDING_MATCH_REQUEST) ;
					
					mMatchData = null ;
					int [] portRange = mRequesterData.mRequester.mslr_getCommunicationPortRange(mRequesterData.mSerial) ;
					if ( portRange == null ) {
						portRange = new int[]{ 0, -1 } ;
					}
					MatchSeeker ms = new MatchSeeker(
							new MatchSeeker.Delegate() {
								
								@Override
								public boolean msd_receivedRejection(MatchSeeker ms, int rejection) {
									log(mRequesterData, "msd_receivedRejection called by MatchSeeker with rejection reason " + MatchSeeker.rejectionToString(rejection)) ;
									boolean restart = false ;	// requires a complete restart in the loop
									
									// It's possible that we got a MatchTicket-based rejection, in which
									// case we should restart this whole bloody mess.
									switch( rejection ) {
									case REJECTION_MATCHTICKET_ADDRESS:
									case REJECTION_MATCHTICKET_CONTENT:
									case REJECTION_MATCHTICKET_SIGNATURE:
									case REJECTION_MATCHTICKET_PROOF:
									case REJECTION_MATCHTICKET_EXPIRED:
										restart = true ;
									}
									
									// Ask the requester if we keep going.
									boolean retry = mRequesterData.mRequester.mslr_matchFailed(
											mRequesterData.mSerial, mSessionNonce, true, FAILURE_MATCHSEEKER_REJECTION, rejection, 0) ;
									
									if ( !retry ) {
										mMatchData = MatchData.newFailed(false, false) ;
										return false ;
									}
									
									if ( restart ) {
										mMatchData = MatchData.newFailed(true, true) ;
										return false ;
									}
									
									// keep trying!
									return true ;
								}
								
								
								@Override
								public boolean msd_receivedPromise(MatchSeeker ms) {
									log(mRequesterData, "msd_receivedPromise called by MatchSeeker") ;
									// Update the requester and see if they want to continue.
									mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_RECEIVED_MATCH_PROMISE) ;
									if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
										mMatchData = MatchData.newRequesterFinished() ;
										return false ;
									}
									// keep on keeping on
									return true ;
								}
								
								
								@Override
								public void msd_receivedMatch(MatchSeeker ms, String localUserID,
										String localRequestID, DatagramChannel dChannel,
										String remoteUserID, String remoteRequestID,
										SocketAddress[] remoteAddresses) {
									log(mRequesterData, "msd_receivedMatch called by MatchSeeker") ;
									// Store the result in mMatchData.
									mMatchData = MatchData.newRequesterMatched(
											localUserID, localRequestID, dChannel,
											remoteUserID, remoteRequestID, remoteAddresses) ;
								}
								
								
								@Override
								public void msd_failedToContact(MatchSeeker ms) {
									log(mRequesterData, "msd_failedToContact called by MatchSeeker") ;
									// Tell the receiver.
									boolean retry = mRequesterData.mRequester.mslr_matchFailed(mRequesterData.mSerial, mSessionNonce, true, FAILURE_MATCHSEEKER_CONTACT, 0, 0) ;
									mMatchData = MatchData.newFailed(retry, false) ;
									return ;
								}
							},
							intent, nonce, mMatchTicketOrder.getTicket(), mMatchTicketOrder.getProof(),
							matchmakerAddress,
							portRange[0], portRange[1],
							mRequesterData.mRequester.mslr_getUserID(mRequesterData.mSerial),
							mRequesterData.mHolePunchFailures) ;
					
					// run it!
					try {
						log(mRequesterData, "Seeking a Match with " + mRequesterData.mHolePunchFailures + " hole punching failures") ;
						//System.err.println("MatchSeekingListener: starting matchseeker with " + mRequesterData.mHolePunchFailures + " hole punch failures") ;
						ms.run() ;
						log(mRequesterData, "Match seeking complete") ;
					}
					catch ( Exception e ) { mMatchData = null ; }
					finally { ms = null ; }
					
					// result should be set in mMatchData.
					if ( mMatchData == null ) {
						log(mRequesterData, "match data is null") ;
						// HUH?
						boolean retry = mRequesterData.mRequester.mslr_matchFailed(mRequesterData.mSerial, mSessionNonce, true, FAILURE_MATCHSEEKER_EXCEPTION, 0, 0) ;
						if ( retry )
							MatchSeekingListener.this.mRequesters.add(new RequesterData(mRequesterData.mRequester, mRequesterData.mSerial, mRequesterData.mHolePunchFailures)) ;
						continue ;		// try next mRequesterData.mRequester.
					}
					
					boolean shouldContinue = false ;
					switch( mMatchData.result ) {
					case MatchData.RESULT_REQUESTER_FINISHED:
						log(mRequesterData, "requester finished") ;
						// The requester doesn't want us to continue.
						shouldContinue = true ;
						break ;
					case MatchData.RESULT_FAILED:
						log(mRequesterData, "failed") ;
						// Failure.  If the MatchData wants us to 'retry,' we put this back in
						// the queue before continuing; if 'restart' we also set MatchTicketOrder to
						// null.
						if ( mMatchData.retry )
							MatchSeekingListener.this.mRequesters.add(new RequesterData(mRequesterData.mRequester, mRequesterData.mSerial, mRequesterData.mHolePunchFailures)) ;
						if ( mMatchData.restart )
							mMatchTicketOrder = null ;
						shouldContinue = true ;
						break ;
					}
					
					if ( shouldContinue ) {
						mMatchData.close() ;
						continue ;
					}
					
					// if we get here, we have an actual match.  Tell the requester, see if they want
					// to continue.
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_RECEIVED_MATCH) ;
					if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
						mMatchData.close() ;
						continue ;
					}
					
					//
					////////////////////////////////////////////////////////////////
					
					
					
					////////////////////////////////////////////////////////////////
					//
					// MATCHED SOCKET MAKER
					//
					
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_MAKING_MATCHED_SOCKET) ;
					if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
						mMatchData.close() ;
						continue ;
					}
					
					mMatchedSocketData = null ;
					MatchedSocketMaker msm = new MatchedSocketMaker(
							new MatchedSocketMaker.Listener() {
								@Override
								public void msml_success(MatchedSocketMaker msm, WrappedSocket ws) {
									mMatchedSocketData = MatchedSocketData.newSuccess(ws) ;
									mRequesterData.mHolePunchFailures = 0 ;
								}
								
								@Override
								public void msml_failure(MatchedSocketMaker msm) {
									mMatchedSocketData = MatchedSocketData.newFailed() ;
									mRequesterData.mHolePunchFailures++ ;
								}
							},
							mMatchData.localUserID, mMatchData.localRequestID, mMatchData.dChannel,
							mMatchData.remoteUserID, mMatchData.remoteRequestID, mMatchData.remoteAddresses,
							mRequesterData.mRequester.mslr_getWrappedSocketAdministrator(mRequesterData.mSerial),
							mRequesterData.mRequester.mslr_getMessageClass()) ;
					
					// run it!
					try {
						log(mRequesterData, "starting a matched socket maker") ;
						msm.run() ;
						log(mRequesterData, "matched socket maker complete") ;
					}
					catch ( Exception e ) { mMatchedSocketData = null ; }
					finally { msm = null ; }
					
					// result should be set in mMatchData.
					if ( mMatchedSocketData == null || mMatchedSocketData.result == MatchedSocketData.RESULT_FAILED ) {
						log(mRequesterData, "matched socket failed(?)") ;
						// HUH?
						boolean retry = mRequesterData.mRequester.mslr_matchFailed(mRequesterData.mSerial, mSessionNonce, true, FAILURE_MATCHED_SOCKET_MAKER_FAILED, 0, 0) ;
						if ( retry )
							MatchSeekingListener.this.mRequesters.add(new RequesterData(mRequesterData.mRequester, mRequesterData.mSerial, mRequesterData.mHolePunchFailures)) ;
						mMatchData.close() ;
						continue ;		// try next mRequesterData.mRequester.
					}
					
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_MADE_MATCHED_SOCKET) ;
					if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
						log(mRequesterData, "user no longer requesting") ;
						mMatchData.close() ;
						continue ;
					}
					
					//
					////////////////////////////////////////////////////////////////
					
					
					////////////////////////////////////////////////////////////////
					//
					// IDENTITY EXCHANGE
					//
					
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_STARTING_INFORMATION_EXCHANGE) ;
					if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
						log(mRequesterData, "user no longer requesting") ;
						mMatchData.close() ;
						continue ;
					}
					
					mIdentityExchangeData = null ;
					IdentityExchangeThread iet = new IdentityExchangeThread(
							new IdentityExchangeThread.Listener() {
								@Override
								public void ietl_informationReceived(IdentityExchangeThread thread,
										WrappedSocket ws, Nonce personalNonce, String name) {
									log(mRequesterData, "ietl_informationReceived") ;
									mIdentityExchangeData = IdentityExchangeData.newSuccess(ws, personalNonce, name) ;
								}
								
								@Override
								public void ietl_informationNotReceived(IdentityExchangeThread thread) {
									log(mRequesterData, "ietl_informationNotReceived") ;
									mIdentityExchangeData = IdentityExchangeData.newFailed() ;
								}
							},
							mMatchedSocketData.wrappedSocket,
							nonce, personalNonce, name ) ;
					
					// run it!
					try {
						log(mRequesterData, "starting identity exchange") ;
						iet.run() ;
						log(mRequesterData, "ident exchange complete") ;
					}
					catch ( Exception e ) { mIdentityExchangeData = null ; }
					finally { iet = null ; }
					
					// result should be set in mMatchData.
					if ( mIdentityExchangeData == null || mIdentityExchangeData.result == IdentityExchangeData.RESULT_FAILED ) {
						log(mRequesterData, "identity exchange failed.") ;
						// HUH?
						boolean retry = mRequesterData.mRequester.mslr_matchFailed(mRequesterData.mSerial, mSessionNonce, true, FAILURE_IDENTITY_EXCHANGE_FAILED, 0, 0) ;
						if ( retry )
							MatchSeekingListener.this.mRequesters.add(new RequesterData(mRequesterData.mRequester, mRequesterData.mSerial, mRequesterData.mHolePunchFailures)) ;
						mMatchData.close() ;
						continue ;		// try next mRequesterData.mRequester.
					}
					
					mRequesterData.mRequester.mslr_matchUpdate(mRequesterData.mSerial, UPDATE_FINISHED_INFORMATION_EXCHANGE) ;
					if ( !mRequesterData.mRequester.mslr_requesting(mRequesterData.mSerial) ) {
						log(mRequesterData, "user no longer requesting") ;
						mMatchData.close() ;
						continue ;
					}
					
					//
					////////////////////////////////////////////////////////////////
					
					
					////////////////////////////////////////////////////////////////
					//
					// FINISHED!  FINISHED!  FINISHED!
					//
					
					log(mRequesterData, "calling match found") ;
					boolean accepted = mRequesterData.mRequester.mslr_matchFound(
							mRequesterData.mSerial,
							mSessionNonce, 
							mIdentityExchangeData.wrappedSocket,
							mIdentityExchangeData.name,
							mIdentityExchangeData.personalNonce ) ;
					
					if ( !accepted )
						mMatchData.close() ;
					
					// finished.
				} catch( Exception e ) {
					e.printStackTrace() ;
				} finally {
					// null out references collected over this loop.
					mRequesterData = null ;

					mMatchData = null ;
					mMatchedSocketData = null ;
					mIdentityExchangeData = null ;
				}
			}
			
			// null out references possibly collected.
			mRequesterData = null ;

			mMatchData = null ;
			mMatchedSocketData = null ;
			mIdentityExchangeData = null ;
			
			// remove some dead weight (including ourself)
			removeDeadListeners() ;
		}
		
		
		private Nonce getNonce(Requester requester, long serial) {
			InternetLobby lobby = requester.mslr_getInternetLobby(serial) ;
			InternetLobbyGame game = requester.mslr_getInternetLobbyGame(serial) ;
			
			if ( lobby != null )
				return lobby.getLobbyNonce() ;
			else if ( game != null )
				return game.getNonce() ;
			
			return null ;
		}
		

		
		/**
		 * Returns a matchticket acquired from the Requester's lobby
		 * or game.
		 * 
		 * Throws an exception upon communication failure.
		 * 
		 * Returns 'null' if the requester is finished.
		 * 
		 * Throws IllegalArgumentException if the provided Requester
		 * 		does not adequately respond to our queries.
		 * 
		 * @param requester
		 * @throws CommunicationErrorException 
		 */
		private MatchTicketOrder getMatchTicket( Requester requester, long serial, String userID ) throws CommunicationErrorException, IllegalArgumentException {
			
			if ( !requester.mslr_requesting(serial) ) {
				return null ;
			}
			
			InternetLobby lobby = requester.mslr_getInternetLobby(serial) ;
			InternetLobbyGame game = requester.mslr_getInternetLobbyGame(serial) ;
			
			String xl = requester.mslr_getXL(serial) ;
			
			if ( userID == null || (lobby == null && game == null) )
				throw new IllegalArgumentException("Requester failed to respond to info requests") ;
			
			MatchTicketOrder mto ;
			if ( lobby != null )
				mto = lobby.matchticketRequest(userID, xl) ;
			else
				mto = game.matchticketRequest(userID, xl) ;
			
			return mto ;
		}
		
	}
	
	
	
	private static class MatchData {
		
		static final int RESULT_REQUESTER_FINISHED = 0 ;
		static final int RESULT_MATCHED = 1 ;
		static final int RESULT_FAILED = 2 ;
		
		int result ;
		
		boolean retry ;
		boolean restart ;
		
		DatagramChannel dChannel ;
		String localUserID ;
		String localRequestID ;
		
		String remoteUserID ;
		String remoteRequestID ;
		SocketAddress [] remoteAddresses ;
		
		static MatchData newRequesterFinished() {
			MatchData md = new MatchData() ;
			md.result = RESULT_REQUESTER_FINISHED ;
			return md ;
		}
		
		static MatchData newRequesterMatched( String localUserID,
				String localRequestID, DatagramChannel dChannel,
				String remoteUserID, String remoteRequestID,
				SocketAddress[] remoteAddresses ) {
			MatchData md = new MatchData() ;
			md.result = RESULT_MATCHED ;
			
			md.dChannel = dChannel ;
			md.localUserID = localUserID ;
			md.localRequestID = localRequestID ;
			
			md.remoteUserID = remoteUserID ;
			md.remoteRequestID = remoteRequestID ;
			md.remoteAddresses = remoteAddresses ;
			
			return md ;
		}
		
		static MatchData newFailed( boolean retry, boolean restart ) {
			MatchData md = new MatchData() ;
			md.result = RESULT_FAILED ;
			md.retry = retry ;
			md.restart = restart ;
			return md ;
		}
		
		
		void close() {
			try {
				dChannel.close() ;
			} catch( Exception e ) { }
		}
	}
	
	
	private static class MatchedSocketData {
		static final int RESULT_SUCCESS = 0 ;
		static final int RESULT_FAILED = 1 ;
		
		int result ;
		
		WrappedSocket wrappedSocket ;
		
		static MatchedSocketData newSuccess( WrappedSocket ws ) {
			MatchedSocketData msd = new MatchedSocketData() ;
			msd.result = RESULT_SUCCESS ;
			msd.wrappedSocket = ws ;
			return msd ;
		}
		
		static MatchedSocketData newFailed() {
			MatchedSocketData msd = new MatchedSocketData() ;
			msd.result = RESULT_FAILED ;
			return msd ;
		}
		
	}
	
	
	private static class IdentityExchangeData {
		static final int RESULT_SUCCESS = 0 ;
		static final int RESULT_FAILED = 1 ;
		
		int result ;
		
		WrappedSocket wrappedSocket ;
		Nonce personalNonce ;
		String name ;
		
		static IdentityExchangeData newSuccess( WrappedSocket ws, Nonce personalNonce, String name ) {
			IdentityExchangeData ied = new IdentityExchangeData() ;
			ied.result = RESULT_SUCCESS ;
			ied.wrappedSocket = ws ;
			ied.personalNonce = personalNonce ;
			ied.name = name ;
			return ied ;
		}
		
		static IdentityExchangeData newFailed() {
			IdentityExchangeData ied = new IdentityExchangeData() ;
			ied.result = RESULT_FAILED ;
			return ied ;
		}
		
	}

}
