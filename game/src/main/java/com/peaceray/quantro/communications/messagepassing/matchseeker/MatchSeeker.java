package com.peaceray.quantro.communications.messagepassing.matchseeker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import com.peaceray.quantro.communications.MatchmakingMessage;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.utils.Communication;
import com.peaceray.quantro.utils.Debug;

public class MatchSeeker extends Thread {
	
	
	private static final boolean DEBUG_LOG = true && Debug.LOG ;
	private static final String TAG = "MatchSeeker" ;
	
	private static final void log(String str) {
		if ( DEBUG_LOG ) {
			System.out.println(TAG + " : " + str) ;
		}
	}
	
	private static final void log(Exception e, String str) {
		if ( DEBUG_LOG ) {
			System.err.println(TAG + " : " + str + " : exception below") ;
			e.printStackTrace() ;
		}
	}
	
	public interface FullDelegate {
		
		/**
		 * We sent a request to the matchseeker but did not receive a response.
		 * Should we 
		 * @param ms The MatchSeeker reporting results
		 * @param timeout The timeout we're currently using as we wait for responses.
		 * @param numTimeouts The number of timeouts missed, in total.
		 * @param numConsecutiveTimeouts The number of consecutive timeouts missed.
		 * @param receivedResponseRatio What's the ratio of receivedResponses / requests?  Between 0-1.
		 * @return Should we keep trying?
		 */
		public boolean msfd_noResponse( MatchSeeker ms,
				int timeout,
				int numTimeouts,
				int numConsecutiveTimeouts,
				double receivedResponseRatio ) ;
		
		
		/**
		 * We received a promise from the Matchmaker for an update.
		 * @param ms
		 * @param duration
		 * @return Should we continue operation?
		 */
		public boolean msfd_promised( MatchSeeker ms,
				long duration ) ;
		
		
		/**
		 * We received a match from the Matchmaker.  Our operation
		 * terminates after this method returns.  This callback will be used
		 * if the "one-port" constructor is used.
		 * 
		 * @param ms
		 */
		public void msfd_matched( MatchSeeker ms,
				String localUserID, String localRequestID, DatagramChannel dChannel, 
				String remoteUserID, String remoteRequestID, SocketAddress [] remoteAddresses ) ;
		
		
		
		
		/**
		 * We received a rejection message - the matchmaker is full.
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedFull( MatchSeeker ms ) ;
		
		/**
		 * We received a rejection message - our nonce is invalid.
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedInvalidNonce( MatchSeeker ms ) ;
		
		
		/**
		 * We received a rejection message - our nonce is in use.
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedNonceInUse( MatchSeeker ms ) ;
		
		
		/**
		 * We received a rejection message - the port we reported
		 * for contact does not match the port from which our UDP
		 * packet originated.  This probably means that UDP hole-punching
		 * is impossible.
		 * 
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedPortRandomization( MatchSeeker ms ) ;
		
		
		/**
		 * We received a rejection message - the matchticket address
		 * does not match the contact address we're using.
		 * 
		 * Get a new ticket.
		 * 
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketAddress( MatchSeeker ms ) ;
		
		
		/**
		 * We received a rejection message - the matchticket content
		 * does not match the request we sent.
		 * 
		 * Get a new ticket.
		 * 
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketContent( MatchSeeker ms ) ;
		
		/**
		 * The matchticket signature is bad.  This is a server signature,
		 * so there's not much we can do about it.
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketSignature( MatchSeeker ms ) ;
		
		/**
		 * We received a rejection message - the matchticket proof was not
		 * valid.  This probably indicates a bug, so there's not much we can do.
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketProof( MatchSeeker ms ) ;
		
		
		/**
		 * We received a rejection message - the matchticket is expired.
		 * 
		 * It's time to get a new one.
		 * 
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketExpired( MatchSeeker ms ) ;
		
		
		/**
		 * We received an unspecified rejection message from the server.
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedUnspecified( MatchSeeker ms ) ;
		
		
		
		/**
		 * An exception was thrown attempting to open a datagram socket on
		 * the specified port (which was provided at construction as the
		 * contact port).
		 * 
		 * @param ms
		 * @param contactPort
		 */
		public void msfd_failedOpeningDatagramSocket( MatchSeeker ms ) ;
		
		
		/**
		 * Failed for some unspecified reason.
		 * @param ms
		 */
		public void msfd_failedUnspecified( MatchSeeker ms ) ;
		
	}
	
	/**
	 * A lower-responsibility Delegate.  Will receive fewer callbacks and has
	 * lower granularity in terms of responses than the FullDelegate.  In exchange,
	 * many of the calls available to FullDelegate will be handled automatically;
	 * e.g., keep-alive and timeouts will be automatically adjusted.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {
		
		/**
		 * We have had enough timeouts that we've concluded that the connection attempt
		 * is a failure.  This MatchSeeker is basically finished.
		 * 
		 * @param ms
		 * @return Should we keep attempting?
		 */
		public void msd_failedToContact( MatchSeeker ms ) ;
		
		
		/**
		 * We received a promise from the matchmaker.  This might be called multiple
		 * times.  The main point here is that we received a message, and it was
		 * a promise for a later update.
		 * 
		 * @param ms
		 * @return Should we keep attempting?
		 */
		public boolean msd_receivedPromise( MatchSeeker ms ) ;
		
		
		/**
		 * We've matched!  The information is included.
		 * 
		 * @param ms
		 * @param localUserID
		 * @param localRequestID
		 * @param dChannel
		 * @param remoteUserID
		 * @param remoteRequestID
		 * @param remoteAddresses
		 */
		public void msd_receivedMatch( MatchSeeker ms,
				String localUserID, String localRequestID, DatagramChannel dChannel, 
				String remoteUserID, String remoteRequestID, SocketAddress [] remoteAddresses ) ;
		
		
		
		
		
		/**
		 * We received a rejection message from the matchmaker.  The message itself
		 * is specified.
		 * 
		 * @param ms
		 * @param rejection
		 * @return Should we keep attempting?
		 */
		public boolean msd_receivedRejection( MatchSeeker ms, int rejection ) ;
		
		
		public static final int REJECTION_FULL 					= 0 ;
		public static final int REJECTION_INVALID_NONCE			= 1 ;
		public static final int REJECTION_NONCE_IN_USE 			= 2 ;
		public static final int REJECTION_PORT_RANDOMIZATION 	= 3 ;
		public static final int REJECTION_MATCHTICKET_ADDRESS 	= 4 ;
		public static final int REJECTION_MATCHTICKET_CONTENT 	= 5 ;
		public static final int REJECTION_MATCHTICKET_SIGNATURE = 6 ;
		public static final int REJECTION_MATCHTICKET_PROOF 	= 7 ;
		public static final int REJECTION_MATCHTICKET_EXPIRED 	= 8 ;
		public static final int REJECTION_UNSPECIFIED 			= 9 ;
		
	}
	
	public static final String rejectionToString( int rejection ) {
		switch ( rejection ) {
		case Delegate.REJECTION_FULL:
			return "FULL" ;
		case Delegate.REJECTION_INVALID_NONCE:
			return "INVALID_NONCE" ;
		case Delegate.REJECTION_NONCE_IN_USE:
			return "NONCE_IN_USE" ;
		case Delegate.REJECTION_PORT_RANDOMIZATION:
			return "PORT_RANDOMIZATION" ;
		case Delegate.REJECTION_MATCHTICKET_ADDRESS:
			return "MATCHTICKET_ADDRESS" ;
		case Delegate.REJECTION_MATCHTICKET_CONTENT:
			return "MATCHTICKET_CONTENT" ;
		case Delegate.REJECTION_MATCHTICKET_SIGNATURE:
			return "MATCHTICKET_SIGNATURE" ;
		case Delegate.REJECTION_MATCHTICKET_PROOF:
			return "MATCHTICKET_PROOF" ;
		case Delegate.REJECTION_MATCHTICKET_EXPIRED:
			return "MATCHTICKET_EXPIRED" ;
		case Delegate.REJECTION_UNSPECIFIED:
			return "MATCHTICKET_UNSPECIFIED" ;
		}
		return "invalid_code" ;
	}
	
	
	/**
	 * A class which wraps Delegates in a FullDelegate interface, so most callbacks
	 * can be smartly handled.
	 * 
	 * @author Jake
	 *
	 */
	private class DelegateWrapper implements FullDelegate {
		
		////////////////////////////////////////////////////////////////////////////
		//
		// MatchSeeker parameters
		//
		// We are delegate to, and controller of, a MatchSeeker instance.  We have at
		// most 1 instance running at a time.  Its parameters should be controlled by this
		// object; for example, we are responsible for setting the delay before
		// next action, back-off or step-up regarding timeouts and keepAlive messages,
		// etc.
		//
		// For now we keep things simple.  We don't change any parameters, allow a 10
		// second keepalive, etc.  Our main purpose here is to count timeouts, scale
		// the timeouts back, and eventually let our OWN delegate know that we failed
		// to communicate with it.
		//
		
		private static final int TIMEOUT_INITIAL = 1000 ;		// 1 second
		private static final int TIMEOUT_BACKOFF = 2 ;		// multiplicative backoff
		private static final int TIMEOUT_MAXIMUM = 120000 ;		// 2 minutes is the maximum timeout
		
		private static final int TIMEOUT_NUMBER_BEFORE_FAILURE = 6 ;	// after 6 timeouts (63 seconds) declare failure.
		
		private static final int INTERVAL_KEEP_ALIVE = 20000 ;	// 20 seconds
		
		private static final int DELAY_BEFORE_REQUEST_PORT_MULTIPLICATION = 20000 ;	// 20 seconds before next attempt
		
		private static final int DELAY_BEFORE_REQUEST_FULL_BACKOFF_INITIAL = 1000 ;	// 1st backoff is 1 second
		private static final int DELAY_BEFORE_REQUEST_FULL_BACKOFF_MAXIMUM = 1000 * 60 * 10 ;	// max backoff is 10 minutes
		private static final int DELAY_BEFORE_REQUEST_FULL_BACKOFF = 2 ;		// multiplicative
		
		private int mDelayBeforeRequestFull = DELAY_BEFORE_REQUEST_FULL_BACKOFF_INITIAL ;
		
		
		Delegate mDelegate ;
		boolean mHalted ;
		
		private DelegateWrapper( Delegate d ) {
			mDelegate = d ;
			mHalted = false ;
			if ( mDelegate == null )
				throw new NullPointerException("Can't wrap null delegate") ;
		}
		
		
		/**
		 * We sent a request to the matchseeker but did not receive a response.
		 * Should we 
		 * @param ms The MatchSeeker reporting results
		 * @param timeout The timeout we're currently using as we wait for responses.
		 * @param numTimeouts The number of timeouts missed, in total.
		 * @param numConsecutiveTimeouts The number of consecutive timeouts missed.
		 * @param receivedResponseRatio What's the ratio of receivedResponses / requests?  Between 0-1.
		 * @return Should we keep trying?
		 */
		public boolean msfd_noResponse( MatchSeeker ms,
				int timeout,
				int numTimeouts,
				int numConsecutiveTimeouts,
				double receivedResponseRatio ) {
			
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
				
			// backoff
			ms.setRequestTimeout(Math.min(timeout * TIMEOUT_BACKOFF, TIMEOUT_MAXIMUM)) ;
			// too many?
			if ( numConsecutiveTimeouts >= TIMEOUT_NUMBER_BEFORE_FAILURE ) {
				mDelegate.msd_failedToContact(ms) ;
				return false ;
			}
			
			return true ;
		}
		
		
		/**
		 * We received a promise from the Matchmaker for an update.
		 * @param ms
		 * @param duration
		 * @return Should we continue operation?
		 */
		public boolean msfd_promised( MatchSeeker ms,
				long duration ) {
			
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			return mDelegate.msd_receivedPromise(ms) ;
		}
		
		
		
		/**
		 * We received a match from the Matchmaker.  Our operation
		 * terminates after this method returns.
		 * 
		 * @param ms
		 */
		public void msfd_matched( MatchSeeker ms,
				String localUserID, String localRequestID, DatagramChannel dChannel,
				String remoteUserID, String remoteRequestID, SocketAddress [] remoteAddresses  ) {
			
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return ;
			}
			
			// let the delegate know.
			mDelegate.msd_receivedMatch( ms,
					localUserID, localRequestID, dChannel,
					remoteUserID, remoteRequestID, remoteAddresses) ;
		}
		
		
		
		
		
		/**
		 * We received a rejection message - the matchmaker is full.
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedFull( MatchSeeker ms ) {
			
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			int delay = new Random().nextInt(mDelayBeforeRequestFull) ;
			ms.setNextRequestDelay(delay) ;
			mDelayBeforeRequestFull = Math.min(
					mDelayBeforeRequestFull * DELAY_BEFORE_REQUEST_FULL_BACKOFF,
					DELAY_BEFORE_REQUEST_FULL_BACKOFF_MAXIMUM ) ;
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_FULL) ;
		}
		
		/**
		 * We received a rejection message - our nonce is invalid.
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedInvalidNonce( MatchSeeker ms ) {
			
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_INVALID_NONCE) ;
		}
		
		
		/**
		 * We received a rejection message - our nonce is in use.
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedNonceInUse( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_NONCE_IN_USE) ;
		}
		
		
		/**
		 * We received a rejection message - the port we reported
		 * for contact does not match the port from which our UDP
		 * packet originated.  This probably means that UDP hole-punching
		 * is impossible.
		 * 
		 * @param ms
		 * @return Should we continue operation?
		 */
		public boolean msfd_rejectedPortRandomization( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			ms.setNextRequestDelay(DELAY_BEFORE_REQUEST_PORT_MULTIPLICATION) ;
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_PORT_RANDOMIZATION) ;
		}
		
		
		/**
		 * We received a rejection message - the matchticket address
		 * does not match the contact address we're using.
		 * 
		 * Get a new ticket.
		 * 
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketAddress( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_MATCHTICKET_ADDRESS) ;
		}
		
		
		
		/**
		 * We received a rejection message - the matchticket content
		 * does not match the request we sent.
		 * 
		 * Get a new ticket.
		 * 
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketContent( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_MATCHTICKET_CONTENT) ;
		}
		
		/**
		 * The matchticket signature is bad.  This is a server signature,
		 * so there's not much we can do about it.
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketSignature( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_MATCHTICKET_SIGNATURE) ;
		}
		
		/**
		 * We received a rejection message - the matchticket proof was not
		 * valid.  This probably indicates a bug, so there's not much we can do.
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketProof( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_MATCHTICKET_PROOF) ;
		}
		
		
		/**
		 * We received a rejection message - the matchticket is expired.
		 * 
		 * It's time to get a new one.
		 * 
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedMatchticketExpired( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_MATCHTICKET_EXPIRED) ;
		}
		
		
		/**
		 * We received an unspecified rejection message from the server.
		 * @param ms
		 * @return
		 */
		public boolean msfd_rejectedUnspecified( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return false ;
			}
			
			return mDelegate.msd_receivedRejection(ms, Delegate.REJECTION_UNSPECIFIED) ;
		}
		
		
		
		/**
		 * An exception was thrown attempting to open a datagram socket on
		 * the specified port (which was provided at construction as the
		 * contact port).
		 * 
		 * @param ms
		 * @param contactPort
		 */
		public void msfd_failedOpeningDatagramSocket( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return ;
			}
			
			mDelegate.msd_failedToContact(ms) ;
		}
		
		
		/**
		 * Failed for some unspecified reason.
		 * @param ms
		 */
		public void msfd_failedUnspecified( MatchSeeker ms ) {
			if ( ms != MatchSeeker.this || mHalted ) {
				ms.halt() ;
				return ;
			}
			
			mDelegate.msd_failedToContact(ms) ;
		}
	}
	
	

	FullDelegate mDelegate ;
	byte mIntent ;
	Nonce mNonce ;
	
	boolean mRunning ;
	boolean mStarted ;
	
	// addresses and such
	SocketAddress mMatchmakerAddress ;
	int mContactPortMin ;
	int mContactPortMax ;
	
	String mUserID ;
	String mRequestID ;
	
	String mMatchTicket ;
	Nonce mMatchTicketProof ;
	
	int mFailedHolePunchAttempts ;		// we report this number to the matchmaker, as they have an interest in it.
										// Basically, the higher this number, the greater the chance that the 
										// matchmaker will offer to act as a UDP pass-through for communication
										// (implicitly, by providing a designated UDP address for UDP hole-punching).
										// The hole gets punched "through" the server.  However, we don't need to
										// be aware that this is what's happening.
	
	
	// Timing, communication, etc.
	int mRequestTimeout ;		// we expect a response within this amount of time
	long mKeepAliveInterval ;	// millis between "keep alives" while a promise is ongoing?
	long mNextRequestDelay ;
	
	
	// helpful stats
	int mNumTimeouts ;				// lifetime of the Matchseeker
	int mNumConsecutiveTimeouts ;	// since the last Matchmaker response
	int mNumRequests ;				// recvd response interval is ((mNumRequests - mNumTimeouts) / mNumRequests )
	
	ByteBuffer mBB ;
	
	
	
	/**
	 * Constructs a MatchSeeker.  This constructor should be used when different ports
	 * are requested for contacting and matching.  However, this will result in some 
	 * problems with port randomization which can be overcome if we use the same socket
	 * to communicate with the matchmaker and with the matched peer (i.e., the same
	 * port).
	 * 
	 * @param delegate The delegate to which we make callbacks.  Cannot be null;
	 * 			a MatchSeeker without a delegate is useless.
	 * @param nonce	The nonce we use in communication.
	 * @param matchmakerAddress The address we contact.
	 * @param contactPort The port from which we should send our messages.
	 * @param matchPort The port on which we want to UDP hole-punch when a match is found.
	 * @param userID A unique-to-this-user String.  Will be transmitted to the match, so should NOT
	 * 			include personal info.  One suggestion is the salted (w/ nonce?) and hashed
	 * 			Android ID.
	 */
	public MatchSeeker( Delegate delegate, byte intent, Nonce nonce, String matchticket, Nonce matchticketProof,
			SocketAddress matchmakerAddress, int contactPortMin, int contactPortMax,
			String userID, int failedHolePunches ) {
		
		mDelegate = new DelegateWrapper(delegate) ;
		mIntent = intent ;
		mNonce = nonce ;
		mMatchTicket = matchticket ;
		mMatchTicketProof = matchticketProof ;
		mMatchmakerAddress = matchmakerAddress ;
		mContactPortMin = contactPortMin ;
		mContactPortMax = contactPortMax ;
		mUserID = userID ;
		mFailedHolePunchAttempts = failedHolePunches ;
		
		// check for nulls!
		if ( mDelegate == null )
			throw new NullPointerException("Can't give a 'null' delegate!") ;
		if ( mNonce == null )
			throw new NullPointerException("Can't give a 'null' nonce!") ;
		if ( mMatchmakerAddress == null )
			throw new NullPointerException("Can't give a 'null' matchmaker address!") ;
		if ( mUserID == null )
			throw new NullPointerException("Can't give a 'null' user ID!") ;
			
		
		mRequestID = new Nonce().toString() ;
		
		// Set default preferences.
		mRequestTimeout = 1000 * 10 ;		// 10 seconds
		mKeepAliveInterval = 1000 * 20 ; 	// 20 seconds
		mNextRequestDelay = 0 ;
		
		// counters start at 0
		mNumTimeouts = 0 ;
		mNumConsecutiveTimeouts = 0 ;
		mNumRequests = 0 ;
		
		// Running!
		mRunning = true ;
		mStarted = false ;
		
		setRequestTimeout(DelegateWrapper.TIMEOUT_INITIAL) ;
		setKeepAliveInterval(DelegateWrapper.INTERVAL_KEEP_ALIVE) ;
	}
	
	
	/**
	 * Constructs a MatchSeeker.  This constructor should be used when different ports
	 * are requested for contacting and matching.  However, this will result in some 
	 * problems with port randomization which can be overcome if we use the same socket
	 * to communicate with the matchmaker and with the matched peer (i.e., the same
	 * port).
	 * 
	 * @param delegate The delegate to which we make callbacks.  Cannot be null;
	 * 			a MatchSeeker without a delegate is useless.
	 * @param nonce	The nonce we use in communication.
	 * @param matchmakerAddress The address we contact.
	 * @param contactPort The port from which we should send our messages.
	 * @param matchPort The port on which we want to UDP hole-punch when a match is found.
	 * @param userID A unique-to-this-user String.  Will be transmitted to the match, so should NOT
	 * 			include personal info.  One suggestion is the salted (w/ nonce?) and hashed
	 * 			Android ID.
	 */
	public MatchSeeker( FullDelegate delegate, byte intent, Nonce nonce, String matchticket, Nonce matchticketProof,
			SocketAddress matchmakerAddress, int contactPortMin, int contactPortMax,
			String userID, int failedHolePunches ) {
		
		
		mDelegate = delegate ;
		mIntent = intent ;
		mNonce = nonce ;
		mMatchTicket = matchticket ;
		mMatchTicketProof = matchticketProof ;
		mMatchmakerAddress = matchmakerAddress ;
		mContactPortMin = contactPortMin ;
		mContactPortMax = contactPortMax ;
		mUserID = userID ;
		mFailedHolePunchAttempts = failedHolePunches ;
		
		// check for nulls!
		if ( mDelegate == null )
			throw new NullPointerException("Can't give a 'null' delegate!") ;
		if ( mNonce == null )
			throw new NullPointerException("Can't give a 'null' nonce!") ;
		if ( mMatchmakerAddress == null )
			throw new NullPointerException("Can't give a 'null' matchmaker address!") ;
		if ( mUserID == null )
			throw new NullPointerException("Can't give a 'null' user ID!") ;
			
		
		mRequestID = new Nonce().toString() ;
		
		// Set default preferences.
		mRequestTimeout = 1000 * 10 ;		// 10 seconds
		mKeepAliveInterval = 1000 * 20 ; 	// 20 seconds
		mNextRequestDelay = 0 ;
		
		// counters start at 0
		mNumTimeouts = 0 ;
		mNumConsecutiveTimeouts = 0 ;
		mNumRequests = 0 ;
		
		// Running!
		mRunning = true ;
		mStarted = false ;
		
		setRequestTimeout(DelegateWrapper.TIMEOUT_INITIAL) ;
		setKeepAliveInterval(DelegateWrapper.INTERVAL_KEEP_ALIVE) ;
	}
	
	
	
	/**
	 * Sets the number of milliseconds we wait for a response to our request
	 * before notifying the delegate of failure (and then possible re-requesting).
	 * 
	 * If called after the MatchSeeker is started from outside a MatchSeekerDelegate
	 * method, this call might take affect only after the next request.
	 * 
	 * @param millis
	 */
	public void setRequestTimeout( int millis ) {
		mRequestTimeout = millis ;
	}
	
	
	/**
	 * After receiving a promise (and being told to continue operation)
	 * we send a "keep alive" message once every interval.
	 * @param millis
	 */
	public void setKeepAliveInterval( long millis ) {
		mKeepAliveInterval = millis ;
	}
	
	
	/**
	 * If called during a delegate method, or before "start()", will delay
	 * the next request by at least this many milliseconds.  Effect is
	 * unspecified if called at some other time.
	 * @param millis
	 */
	public void setNextRequestDelay( long millis ) {
		mNextRequestDelay = millis ;
	}
	
	
	public void setRequestID( String requestID ) {
		if ( mStarted )
			throw new IllegalStateException("Can only set request ID before starting!") ;
		
		mRequestID = requestID ;
	}
	
	public String getRequestID() {
		return mRequestID ;
	}
	
	public boolean isFinished() {
		return mStarted && !isAlive() ;
	}
	
	
	
	@Override
	public void run() {
		//System.err.println("MatchSeeker.run(): start") ;
		mStarted = true ;
		boolean closeDChannel = false ;
		DatagramChannel dchannel = null ;
		Selector selector = null ;
		SelectionKey selectionKey = null ;
		// our entire purpose is to receive a "Match" message from the matchmaker.
		// we do this in several steps.
		
		// 1st: open a DatagramSocket using contactPort, which we use to send/receive
		// 		messages to the matchmaker.
		// 2nd: in a loop, wait 'mNextRequestDelay' and then send a request message
		//		to the matchmaker.  Timeout on a respones.
		// 3A: If timeout, or response other than "promise" / "match" tell the delegate.
		//		If returns 'true,' loop back to 2,
		// 3B: If received "promise," listen for the full promise duration, with
		//		breaks every "mKeepAliveInterval" to send a keep alive message.
		// 3BA: If do not receive match after full promise duration, go to 2nd.
		// 3BC: If receive "match," either directly after request or after a promise,
		//		check the user_id / request_id (should do this for all messages) and
		//		tell the delegate.
		
		
		// For convenience, we can make a few messages here.  We need 1. a request message,
		// 2. a keep alive message, and 3. an empty message for received data.
		MatchmakingMessage msgRequest = new MatchmakingMessage() ;
		MatchmakingMessage msgKeepAlive = new MatchmakingMessage() ;
		MatchmakingMessage msgReceived = new MatchmakingMessage() ;
		
		msgKeepAlive.setAsKeepAlive() ;
		
		try {
			// first, open socket.
			InetSocketAddress contactAddress = null ;
			try {
				// try to find a port
				for ( int port = mContactPortMin; port <= mContactPortMax; port++ ) {
					try {
						contactAddress = new InetSocketAddress(port) ;
						dchannel = DatagramChannel.open() ;
						dchannel.socket().bind( contactAddress ) ;
						break ;
					} catch( Exception e ) { }
				}
				
				dchannel.configureBlocking(false) ;
				
				selector = Selector.open() ;
				selectionKey = dchannel.register(selector, SelectionKey.OP_READ) ;
				// Remember: DatagramChannel doesn't obey 'soTimeout', which makes blocking
				// pretty much useless (we will block FOREVER).  Use non-blocking and 
				// sleep after null-reads (do NOT sleep after content reads!)
				closeDChannel = true ;
			} catch (Exception e) {
				e.printStackTrace() ;
				mDelegate.msfd_failedOpeningDatagramSocket(this) ;
				mRunning = false ;
				// close the socket!
				closeSafely( dchannel, selectionKey, selector ) ;
				return ;
			}
			// hey, how about a bytebuffer?
			mBB = ByteBuffer.allocate(2048) ;
			
			long timeStarted ;		// we use this var when waiting
			
			while ( mRunning ) {
				// 2nd A: top!  wait 'mNextRequestDelay.'
				try {
					//System.err.println("MatchSeeker.run(): sleeping " + mNextRequestDelay) ;
					if ( mNextRequestDelay > 0 )
						Thread.sleep(mNextRequestDelay) ;
					mNextRequestDelay = 0 ;
				} catch( InterruptedException ie ) {
					ie.printStackTrace() ;
					// whoa whoa WHOA!  Could just mean someone
					// changed the request delay!
					if ( !mRunning )
						break ;
				}
				
				
				// 2nd B: send a request.  Write to byte buffer, then send.
				try {
					contactAddress = (InetSocketAddress)Communication.getPrivateEndpointAsSocketAddress( dchannel.socket().getLocalPort() ) ;
					msgRequest.setAsMatchRequestMatch(mIntent, mNonce, mUserID, mRequestID, contactAddress, mMatchTicket, mMatchTicketProof, mFailedHolePunchAttempts) ;
					mBB.clear() ;
					msgRequest.write(mBB) ;
					mBB.flip() ;
					
					log("sending matchmaker request from local contact address " + contactAddress) ;
					
					// send!
					//System.err.println("MatchSeeker.run(): sending request intent " + this.mIntent + " to " + mMatchmakerAddress) ;
					dchannel.send(mBB, mMatchmakerAddress) ;
					mNumRequests++ ;
				} catch ( Exception e ) {
					log(e, "error when requesting match") ;
					mDelegate.msfd_failedUnspecified(this) ;
					mRunning = false ;
					break ;
				}
				
				// 2nd C: wait for a response, with timeout.
				// put result here.
				
				long promiseDuration = 0 ;
				
				timeStarted = System.currentTimeMillis() ;
				while ( mRunning ) {
					int timeRemaining = (int)( mRequestTimeout - ( System.currentTimeMillis() - timeStarted ) ) ;
					//System.err.println(this.toString() + "MatchSeeker.run(): waiting for msg response...") ;
					boolean received = false ;
					try {
						received = receiveMessage( selectionKey, msgReceived, timeRemaining, null ) ;
					} catch( Exception e ) {
						log(e, "error when receiving message from matchmaker") ;
						// close the socket!
						closeSafely( closeDChannel ? dchannel : null, selectionKey, selector ) ;
						//System.err.println("exception thrown by receiveMessage") ;
						return ;
					}
					if ( !received ) {
						//System.err.println(this.toString() + "MatchSeeker.run(): no response") ;
						// a timeout.
						mNumTimeouts++ ;
						mNumConsecutiveTimeouts++ ;
						mRunning = mDelegate.msfd_noResponse(this, mRequestTimeout,
								mNumTimeouts, mNumConsecutiveTimeouts, (((double)(mNumRequests - mNumTimeouts))/mNumRequests)) ;
						break ;
					} else {
						//System.err.println(this.toString() + "MatchSeeker.run(): response!") ;
						mNumConsecutiveTimeouts = 0 ;
						// received a message!  however, we don't know what kind.  Could be a match,
						// could be a promise, could be something else.
						boolean keepWaiting = true ;
						switch( msgReceived.getType() ) {
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_FULL:
							//System.err.println("MatchSeeker: reject full") ;
							// whoopsie
							mRunning = mDelegate.msfd_rejectedFull(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_INVALID_NONCE:
							//System.err.println("MatchSeeker: reject invalid nonce") ;
							mRunning = mDelegate.msfd_rejectedInvalidNonce(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_NONCE_IN_USE:
							//System.err.println("MatchSeeker: reject nonce in use") ;
							mRunning = mDelegate.msfd_rejectedNonceInUse(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_PORT_RANDOMIZATION:
							//System.err.println("MatchSeeker: reject port randomization") ;
							mRunning = mDelegate.msfd_rejectedPortRandomization(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_ADDRESS_MISMATCH:
							//System.err.println("MatchSeeker: reject ticket address mismatch") ;
							mRunning = mDelegate.msfd_rejectedMatchticketAddress(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_CONTENT_MISMATCH:
							//System.err.println("MatchSeeker: reject ticket content mismatch") ;
							mRunning = mDelegate.msfd_rejectedMatchticketContent(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_SIGNATURE:
							//System.err.println("MatchSeeker: reject ticket signature") ;
							mRunning = mDelegate.msfd_rejectedMatchticketSignature(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_PROOF:
							//System.err.println("MatchSeeker: reject ticket proof") ;
							mRunning = mDelegate.msfd_rejectedMatchticketProof(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_EXPIRED:
							//System.err.println("MatchSeeker: reject ticket expired") ;
							mRunning = mDelegate.msfd_rejectedMatchticketExpired(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_UNSPECIFIED:
							//System.err.println("MatchSeeker: reject unspecified") ;
							mRunning = mDelegate.msfd_rejectedUnspecified(this) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_PROMISE:
							// hey hey hey, a promise!
							//System.err.println("MatchSeeker: promise with duration " + msgReceived.getLocalDuration()) ;
							promiseDuration = msgReceived.getLocalDuration() ;
							mDelegate.msfd_promised(this, promiseDuration) ;
							keepWaiting = false ;
							break ;
							
						case MatchmakingMessage.TYPE_MATCH_CHALLENGE:
						case MatchmakingMessage.TYPE_MATCH:
							/*
							if ( msgReceived.getType() == MatchmakingMessage.TYPE_MATCH )
								System.err.println("MatchSeeker: match challenge") ;
							else
								System.err.println("MatchSeeker: match") ;
								*/
							// MATCH!
							SocketAddress [] localAddress = new SocketAddress[msgReceived.getLocalNumSocketAddresses()] ;
							for ( int j = 0; j < localAddress.length; j++ )
								localAddress[j] = msgReceived.getLocalSocketAddress(j) ;
							SocketAddress [] remoteAddress = new SocketAddress[msgReceived.getRemoteNumSocketAddresses()] ;
							for ( int j = 0; j < remoteAddress.length; j++ )
								remoteAddress[j] = msgReceived.getRemoteSocketAddress(j) ;
							
							StringBuilder sb = new StringBuilder() ;
							sb.append("Local { ") ;
							for ( int i = 0; i < localAddress.length; i++ )
								sb.append(localAddress[i]).append(" ") ;
							sb.append("}") ;
							sb.append("   Remote { ") ;
							for ( int i = 0; i < remoteAddress.length; i++ )
								sb.append(remoteAddress[i]).append(" ") ;
							sb.append("}") ;
							log("match received with " + sb.toString()) ;
							
							closeDChannel = false ;
							mDelegate.msfd_matched(this,
									msgReceived.getLocalUserID(), msgReceived.getLocalRequestID(),
									dchannel,
									msgReceived.getRemoteUserID(), msgReceived.getRemoteRequestID(),
									remoteAddress ) ;
							mRunning = false ;
							keepWaiting = false ;
							break ;
							
						default:
							//System.err.println("Unknown message type " + msgReceived.getType()) ;
							break ;
							
						}
						
						if ( !keepWaiting )
							break ;		// break the while loop
					}
				}
				
				
				
				// Maybe we've been promised?
				if ( promiseDuration > 0 && mRunning ) {
					// we wait for a match.  We expect nothing else.
					timeStarted = System.currentTimeMillis() ;
					while ( mRunning ) {
						int timeRemaining = (int)( promiseDuration - ( System.currentTimeMillis() - timeStarted ) ) ;
						if ( !receiveMessage( selectionKey, msgReceived, timeRemaining, msgKeepAlive ) ) {
							//System.err.println("MatchSeeker.run(): timeout after promise") ;
							break ;	// break the while
						}
						if ( msgReceived.getType() == MatchmakingMessage.TYPE_MATCH_CHALLENGE || msgReceived.getType() == MatchmakingMessage.TYPE_MATCH ) {
							//System.err.println("MatchSeeker.run(): msg received") ;
							// MATCH!
							SocketAddress [] localAddress = new SocketAddress[msgReceived.getLocalNumSocketAddresses()] ;
							for ( int j = 0; j < localAddress.length; j++ )
								localAddress[j] = msgReceived.getLocalSocketAddress(j) ;
							SocketAddress [] remoteAddress = new SocketAddress[msgReceived.getRemoteNumSocketAddresses()] ;
							for ( int j = 0; j < remoteAddress.length; j++ )
								remoteAddress[j] = msgReceived.getRemoteSocketAddress(j) ;
							
							StringBuilder sb = new StringBuilder() ;
							sb.append("Local { ") ;
							for ( int i = 0; i < localAddress.length; i++ )
								sb.append(localAddress[i]).append(" ") ;
							sb.append("}") ;
							sb.append("   Remote { ") ;
							for ( int i = 0; i < remoteAddress.length; i++ )
								sb.append(remoteAddress[i]).append(" ") ;
							sb.append("}") ;
							log("match received with " + sb.toString()) ;
							
							// cancel the selection...
							// close the socket!
							closeSafely( null, selectionKey, selector ) ;
							closeDChannel = false ;
							mDelegate.msfd_matched(this,
									msgReceived.getLocalUserID(), msgReceived.getLocalRequestID(),
									dchannel,
									msgReceived.getRemoteUserID(), msgReceived.getRemoteRequestID(),
									remoteAddress ) ;
							mRunning = false ;
							break ;	// break the while
						}
					}
				}
				
				// loop back around if still running
				
			}
		} finally {
			// close the socket!
			closeSafely( closeDChannel ? dchannel : null, selectionKey, selector ) ;
			
			mDelegate = null ;
			mBB = null ;
		}
	}
	
	
	private void closeSafely( DatagramChannel dchannel, SelectionKey selectionKey, Selector selector ) {
		if ( dchannel != null ) {
			try {
				dchannel.close() ;
			} catch ( Exception ex ) { }
		}
		if ( selectionKey != null ) {
			try {
				selectionKey.cancel() ;
			} catch ( Exception ex ) { }
		}
		if ( selector != null ) {
			try {
				selector.close() ;
			} catch ( Exception ex ) { }
		}
	}
	
	
	/**
	 * Halt's this MatchSeeker's attempt to find a match.
	 */
	public void halt() {
		mRunning = false ;
		try {
			this.interrupt() ;
		} catch( Exception e ) { }
	}
	
	/**
	 * Attempts to receive a message from the matchmaker.
	 * 
	 * Returns 'true' iff:
	 * 		1. data was received within 'timeRemaining' milliseconds
	 * 		2. that data represented a well-formed MatchmakingMessage
	 * 		3. that data included the user_id and request_id used by this object
	 * 		4. that data originated from the IP address of mMatchmakerAddress (although possibly a different port)
	 * 		5. that data has been parsed into the provided 'msg' object.
	 * 
	 * Returns 'false' if no packet was received in the time allotted that matched theb
	 * 		above criteria, or if a fatal error occurred.
	 * 
	 * MEMBER VARIABLES:
	 * 		This method uses the member variables
	 * 		mBB <- to receive packets, send keepAlives
	 * 		mMatchmakerAddress <- for comparison against the origin point of these packets
	 * 		mUserID, mRequestID <- for comparison against data within packet
	 * 
	 * 		mRunning <- set to false if a fatal error occurred.
	 * 
	 * @param dchannel
	 * @param msg
	 * @param timeRemaining
	 * @param msgKeepAlive: if provided, we periodically send a keepAlive message to the server (according to
	 * 			mKeepAliveInterval).  If not, we don't.
	 * @return
	 */
	private boolean receiveMessage( SelectionKey selectionKey, MatchmakingMessage msg, int timeRemaining, MatchmakingMessage msgKeepAlive ) {
		//System.err.println("MatchSeeker: receiveMessage, timeRemaining is " + timeRemaining + " keep alive interval is " + mKeepAliveInterval + " keep alive msg " + msgKeepAlive) ;
		long timeStarted = System.currentTimeMillis() ;
		long timeLastKeepAlive = timeStarted ;
		while ( true ) {
		
			int timeout = timeRemaining - (int)(System.currentTimeMillis() - timeStarted) ;
			if ( timeout <= 0 )
				return false ;
			
			if ( msgKeepAlive != null ) {
				long timeoutKeepAlive = mKeepAliveInterval - ( System.currentTimeMillis() - timeLastKeepAlive ) ;
				if ( timeoutKeepAlive <= 0 ) {
					// send a keepalive message!
					try {
						//System.err.println("MatchSeeker.run(): sending keepalive") ;
						mBB.clear() ;
						msgKeepAlive.write(mBB) ;
						mBB.flip() ;
						((DatagramChannel)selectionKey.channel()).send(mBB, mMatchmakerAddress) ;
					} catch( IOException ioe ) {
						// nothing, we don't care
					}
					timeLastKeepAlive = System.currentTimeMillis() ;
				}
				
				// timeout is min of current timeout and time until keepalive.
				timeoutKeepAlive = mKeepAliveInterval - ( System.currentTimeMillis() - timeLastKeepAlive ) ;
				timeout = Math.min(timeout, (int)timeoutKeepAlive) ;
			}
			
			SocketAddress addr = null ;
			
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
				    mBB.clear() ;
					addr = ((DatagramChannel)selectionKey.channel()).receive(mBB) ; 		// non-blocking; should return immediately.
					mBB.flip() ;
				    keyIterator.remove();
				}
			} catch ( SocketTimeoutException ste ) {
				if ( System.currentTimeMillis() - timeStarted >= timeRemaining )
					return false ;
				continue; 	// loop from top
			} catch (IOException e) {
				// fatal error!
				mDelegate.msfd_failedUnspecified( this ) ;
				mRunning = false ;
				return false ;
			}
			
			// DatagramChannels are great in some ways, terrible in others.  We
			// can't use 'blocking' mode at all
			// Huh?  Even in blocking mode, receive returns 'null' and makes no
			// change to mBB.  This is quite strange.  For resolve this by checking
			// for 0 data and (if no timeout yet) sleep for 5 and repeat.
			if ( addr == null ) {
				//System.err.println("MatchSeeker.run(): received null, length " + mBB.remaining()) ;
				if ( System.currentTimeMillis() - timeStarted >= timeRemaining )
					return false ;
				
				try { Thread.sleep(5) ; }
				catch (InterruptedException e) { return false ; }
				
				if ( System.currentTimeMillis() - timeStarted >= timeRemaining )
					return false ;
				
				continue ;
			} else {
				//System.err.println("MatchSeeker.run(): received from addr " + addr) ;
			}
		
			// well lookie, we just received a data packet.  check orig. address...
			/*
			System.err.println("is blocking : " + dchannel.isBlocking()) ;
			System.err.println("data position : " + mBB.position() + " limit " + mBB.limit() + " remaining " + mBB.remaining()) ;
			System.err.println("addr : " + addr) ;
			System.err.println("mMatchmakerAddress : " + mMatchmakerAddress) ;
			System.err.println("addr.getAddress : " + ((InetSocketAddress)addr).getAddress()) ;
			System.err.println("mMatchmakerAddress.getAddress : " + ((InetSocketAddress)mMatchmakerAddress).getAddress()) ;
			*/
			
			if ( ((InetSocketAddress)addr).getAddress().equals( ((InetSocketAddress)mMatchmakerAddress).getAddress() ) ) {
				// from the right source...
				try {
					msg.resetForRead() ;
					msg.read(mBB) ;
					
					//System.err.println("MatchSeeker.run() receiveMessage, parsed type " + msg.getType()) ;
					
					// we care about only a very small set of message types.
					switch( msg.getType() ) {
					case MatchmakingMessage.TYPE_PROMISE:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_FULL:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_INVALID_NONCE:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_NONCE_IN_USE:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_PORT_RANDOMIZATION:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_ADDRESS_MISMATCH:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_CONTENT_MISMATCH:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_SIGNATURE:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_PROOF:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_MATCHTICKET_EXPIRED:
					case MatchmakingMessage.TYPE_MATCHMAKER_REJECT_UNSPECIFIED:
					case MatchmakingMessage.TYPE_REQUEST_CHALLENGE:
					case MatchmakingMessage.TYPE_MATCH_CHALLENGE:
					case MatchmakingMessage.TYPE_MATCH:
						if ( mUserID.equals(msg.getLocalUserID()) && mRequestID.equals(msg.getLocalRequestID()) )
							return true ;
					}
				} catch (Exception e) {
					e.printStackTrace() ;
					// hm, can't parse.  Whelp, no skin off our back.
					// just repeat and keep listening.
				}
			}
		}
		
	}
	
}
