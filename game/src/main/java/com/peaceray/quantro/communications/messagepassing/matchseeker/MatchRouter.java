package com.peaceray.quantro.communications.messagepassing.matchseeker;

import java.lang.ref.WeakReference;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

import com.peaceray.quantro.communications.MatchmakingMessage;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.AdministratedWrappedSocketAdministrator;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.InternetLobbyGame;
import com.peaceray.quantro.utils.Debug;


/**
 * MatchRouter: added 10/31/2013.
 * 
 * I have noticed an issue where Matched players get connected to a seemingly
 * random Connection (and thus slot).  This is because the connection process does
 * not concern itself with matching a particular personal Nonce, just creating a stable
 * connection to a user who knows the session nonce (or: the server of the session).
 * 
 * MatchRouter is an intermediate class with one, and only one, purpose: to take matches
 * as they are created and route them to the appropriate connection based on the
 * personal nonce on the other end.
 * 
 * MatchRouters are intended to be exceptionally simple; their functionality is
 * based around routing delegate calls to the right object.
 * 
 * As such, we track the necessary match seeking information ourselves, such as
 * our personal nonce, matchmaker address, etc. (connections
 * don't necessarily need to know this).  Each requester only needs to inform us
 * when they do and do not want a match, and whether we care about the user's
 * personal nonce.
 * 
 * @author Jake
 *
 */
public class MatchRouter implements MatchSeekingListener.Requester {
	
	@SuppressWarnings("unused")
	private static final boolean DEBUG_LOG = true && Debug.LOG ;
	private static final boolean VERBOSE_LOG = true && Debug.LOG ;
	private static final String TAG = "MatchRouter" ;
	
	private final void log( String msg ) {
		if ( DEBUG_LOG ) {
			System.out.println(tagString() + msg) ;
		}
	}
	
	private final void logV( String msg ) {
		if ( VERBOSE_LOG ) {
			System.out.println(tagString() + msg) ;
		}
	}
	
	private final void log( Exception e, String msg ) {
		if ( DEBUG_LOG ) {
			System.err.println(tagString() + msg + " : exception below") ;
			e.printStackTrace() ;
		}
	}
	
	private String tagString() {
		return TAG + " " + (mIntent == 0 ? "Lobby" : "Game") + " " ;
	}
	
	/**
	 * If you are interested in Match updates, we will indiscriminately report
	 * them to you.  Prepare for Spam.
	 * 
	 * @author Jake
	 *
	 */
	public interface UpdateListener {
		
		/**
		 * A new requester has come in, requesting a serial.
		 * 
		 * @param serial
		 */
		public void mrul_didStartMatchRequest( long serial ) ;
		
		/**
		 * We are beginning a fresh match ticket acquisition attempt.  This method will be called
		 * after connect(), or during connect().  Matchticket acquisitions don't happen every time
		 * we need to connect - a single matchticket can cover multiple connection
		 * attempts.
		 * @param mpmsc
		 */
		public void mrul_didBeginMatchTicketAcquisitionAttempt( long serial ) ;
		
		
		/**
		 * We got a matchticket.
		 * 
		 * @param mpmsc
		 */
		public void mrul_didReceiveMatchTicket( long serial ) ;
		
		/**
		 * We failed to acquire a MatchTicket.  We provide an error code and a reason code,
		 * as listed in WebConsts.
		 * 
		 * Return whether to continue or connection attempts.  If 'false' is returned,
		 * we stop and transition to FAILED status.
		 * 
		 * If 'true' is returned, we start a new attempt.
		 * 
		 * @param mpmsc
		 * @param error
		 * @param reason
		 */
		public boolean mrul_didFailMatchTicketAcquisitionAttempt( long serial, int error, int reason ) ;
		
		
		/**
		 * We are beginning a fresh match seeking attempt.  This method will be called shortly
		 * after connect(), or during connect().
		 * 
		 * @param mpmsc
		 */
		public void mrul_didBeginMatchSeekingAttempt( long serial ) ;
		
		/**
		 * We have failed to communicate with the matchmaker, and are giving up.  We will
		 * transition to "failed" status immediately after this method call.
		 * 
		 * Internally, this results after a series of UDP timeouts in requesting a match.
		 * 
		 * @param mpmc
		 */
		public void mrul_didFailToConnectToMatchmaker( long serial ) ;
		
		/**
		 * We received a rejection message from the matchmaker.  The reason is provided,
		 * which is one of REJECTION_REASON_*.
		 * 
		 * @param mpmsc
		 * @param rejectionReason
		 * @return If true, continues to attempt mediation, staying in "pending" status.
		 * 			If false, transitions to "failed" status.
		 */
		public boolean mrul_didReceiveRejectionFromMatchmaker( long serial, Rejection rejectionReason ) ;
		
		public enum Rejection {
			FULL,
			INVALID_NONCE,
			NONCE_IN_USE,
			PORT_RANDOMIZATION,
			UNSPECIFIED,
			MATCHTICKET_ADDRESS,
			MATCHTICKET_CONTENT,
			MATCHTICKET_SIGNATURE,
			MATCHTICKET_PROOF,
			MATCHTICKET_EXPIRED
		}
		
		
		/**
		 * We have received information from the Matchmaker which implies that we are the first
		 * to arrive for the match (e.g., a Promies message).  We don't know how long we'll be
		 * waiting, so it might be wise to transition from Lobby client to Lobby host in a bit (for example).
		 * 
		 * For safety's sake, you should be robust to this method being called multiple times in sequence.
		 * @param mpmc
		 */
		public void mrul_didBeginWaitingForMatch( long serial ) ;
		
		
		/**
		 * We have been matched with a partner!  However, we have not yet begun the hole-punching
		 * attempt (or whatever) and do not have a direct connection with them.
		 * @param mpmc
		 */
		public void mrul_didMatchStart( long serial ) ;
		
		
		/**
		 * We have completed our match with a partner, and now have a communication channel with them!
		 */
		public void mrul_didMatchSuccess( long serial ) ;
		
		
		/**
		 * We started a match attempt, but it failed before completion.  This probably means UDP
		 * hole-punching failed.
		 * 
		 * @return If true, we restart the matchmaking process, requesting another match from the server.
		 * 			If false, transitions to "failed" status.
		 */
		public boolean mrul_didMatchFailure( long serial, int stage );
		public static final int MATCH_FAILURE_STAGE_UDP_HOLE_PUNCH = 0 ;
		public static final int MATCH_FAILURE_STAGE_IDENTITY_EXCHANGE = 1 ;
		
		
		/**
		 * We've ended a match attempt.
		 * 
		 * @param serial
		 * @return
		 */
		public void mrul_didEndMatchRequest( long serial ) ;
		
	}
	
	
	
	public interface Requester {
		/**
		 * A match has been found.  Returns whether the match has been accepted;
		 * if not, the wrapped socket will be closed.
		 * 
		 * @param ws
		 * @param name
		 * @param personalNonce
		 * @return
		 */
		public boolean mrr_matchFound( WrappedSocket ws, String name, Nonce personalNonce ) ;
		
		/**
		 * A match failed.  Returns whether we should attempt to retry.
		 * @param recoverable
		 * @param failure
		 * @param error
		 * @param reason
		 * @return
		 */
		public boolean mrr_matchFailed( boolean recoverable,
			int failure, int error, int reason ) ;
		
		/**
		 * You should call 'cancel' when you're no longer requesting a match, 
		 * but this method provides a back-up just in case.
		 * @return
		 */
		public boolean mrr_stillRequesting() ;
	}
	
	
	private class RequesterPack {
		private Requester mRequester ;
		private Nonce mRemotePersonalNonce ;
		
		private RequesterPack( Requester r, Nonce personalNonce ) {
			mRequester = r ;
			mRemotePersonalNonce = personalNonce ;
		}
	}
	
	
	// The Requesters.  We add/remove to/from this list as needed.
	// Note that there is no connection between mRequesters and mSerials:
	// the purpose of this class is to break any such association.
	private ArrayList<RequesterPack> mRequesters ;
	
	/**
	 * A list of nonces which will ONLY be matched to a Requester that specifies
	 * their remote personanl nonce at request time.
	 */
	private ArrayList<Nonce> mReservedNonces ;
	
	
	private WeakReference<UpdateListener> mwrUpdateListener ;
	
	
	/**
	 * Request a new Match.  If remotePersonalNonce is not null, we will only be
	 * matched with a user who holds that nonce.  If null, we will be matched 
	 * with any user.
	 * 
	 * @param r
	 * @param remotePersonalNonce
	 */
	public void request( Requester r, Nonce remotePersonalNonce ) {
		if ( r == null )
			return ;
		
		UpdateListener ul = mwrUpdateListener.get() ;
		long serial = Long.MIN_VALUE ;
		
		// In a synchronized block, first look for this requester in our
		// current list.  If present, do nothing (assume a request is underway).
		// If NOT present, add it and start a match seeking attempt.
		boolean alreadyPresent = false ;
		synchronized( this ) {
			for ( int i = 0; !alreadyPresent && i < mRequesters.size(); i++ ) {
				RequesterPack rp = mRequesters.get(i) ;
				if ( rp.mRequester == r ) {
					if ( !Nonce.equals(remotePersonalNonce, rp.mRemotePersonalNonce) ) {
						// update
						rp.mRemotePersonalNonce = remotePersonalNonce ;
					}
					alreadyPresent = true ;
				}
			}
			if ( !alreadyPresent ) {
				// add
				RequesterPack rp = new RequesterPack( r, remotePersonalNonce ) ;
				mRequesters.add(rp) ;
				serial = MatchSeekingListener.request(this, mSessionNonce != null ? mSessionNonce : new Nonce(128) ) ;
			}
		}
		
		if ( serial != Long.MIN_VALUE )
			ul.mrul_didStartMatchRequest(serial) ;
	}
	
	/**
	 * Cancels the request by this requester.  Because of threading issues,
	 * it is still possible for the Requester to receive callbacks; simply
	 * respond to these callbacks with 'false.'
	 * 
	 * @param r
	 */
	public void cancel( Requester r ) {
		synchronized( this ) {
			Iterator<RequesterPack> iter = mRequesters.iterator() ;
			for ( ; iter.hasNext() ; ) {
				RequesterPack rp = iter.next() ;
				if ( rp.mRequester == r ) {
					iter.remove() ;
				}
			}
		}
	}
	
	
	public MatchRouter(
			UpdateListener updateListener,
			AdministratedWrappedSocketAdministrator administrator, 
			boolean matchInParallel,
			String xl, SocketAddress matchmakerAddress, int [] contactPortRange,
			InternetLobby lobby, byte intent, Class<?> msgClass,
			Nonce personalNonce, String name, String userID,
			Nonce [] reservedNonces ) {
		
		this( updateListener, administrator, matchInParallel, xl, matchmakerAddress, contactPortRange,
				lobby, null, intent, msgClass, personalNonce, name, userID, reservedNonces ) ;
		
	}
	
	public MatchRouter(
			UpdateListener updateListener,
			AdministratedWrappedSocketAdministrator administrator, 
			boolean matchInParallel,
			String xl, SocketAddress matchmakerAddress, int [] contactPortRange,
			InternetLobbyGame game, byte intent, Class<?> msgClass,
			Nonce personalNonce, String name, String userID,
			Nonce [] reservedNonces ) {
		
		this( updateListener, administrator, matchInParallel, xl, matchmakerAddress, contactPortRange,
				null, game, intent, msgClass, personalNonce, name, userID, reservedNonces ) ;
		
	}
	
	
	public MatchRouter(
			UpdateListener updateListener,
			AdministratedWrappedSocketAdministrator administrator, 
			boolean matchInParallel,
			String xl, SocketAddress matchmakerAddress, int [] contactPortRange,
			InternetLobby lobby, InternetLobbyGame game, byte intent, Class<?> msgClass,
			Nonce personalNonce, String name, String userID,
			Nonce [] reservedNonces ) {
		
		if ( lobby != null && game != null )
			throw new IllegalArgumentException("Either lobby or game non-null, not both or neither.") ;
		if ( lobby == null && game == null )
			throw new IllegalArgumentException("Either lobby or game non-null, not both or neither.") ;
		
		// We assign the same Session Nonce to all Requests if 'matchInParallel'
		// is false.  If true, we give each a different session nonce.
		if ( !matchInParallel )
			mSessionNonce = new Nonce(128) ;
		else
			mSessionNonce = null ;
		
		// assign our member variables
		mMatchmakerAddress = matchmakerAddress ;
		mInternetLobby = lobby ;
		mInternetLobbyGame = game ;
		mIntent = intent ;
		mPersonalNonce = personalNonce ;
		mName = name ;
		mUserID = userID ;
		mContactPortRange = contactPortRange ;
		mMessageClass = msgClass ;
		
		// fix up the UserID if it's too long.
		while ( mUserID.getBytes().length > MatchmakingMessage.MAX_LENGTH_USER_ID_BYTES ) // length-limit in messages.
			mUserID = mUserID.substring(0, mUserID.length()-1) ;
		
		mAdministrator = administrator ;
		
		// allocate our personal structures
		mwrUpdateListener = new WeakReference<UpdateListener> (updateListener) ;
		mRequesters = new ArrayList<RequesterPack>() ;
		mReservedNonces = new ArrayList<Nonce>() ;
		if ( reservedNonces != null ) {
			for ( int i = 0; i < reservedNonces.length; i++ ) {
				mReservedNonces.add(reservedNonces[i]) ;
			}
		}
	}
	
	
	// Information about this router, used for all match seeking attempts.
	private InternetLobby mInternetLobby ;
	private InternetLobbyGame mInternetLobbyGame ;
	private Nonce mPersonalNonce ;
	private String mName ;
	private byte mIntent ;
	private String mUserID ;
	private String mXL ;
	private SocketAddress mMatchmakerAddress ;
	private Nonce mSessionNonce ;
	private int [] mContactPortRange ;
	private AdministratedWrappedSocketAdministrator mAdministrator ;
	private Class<?> mMessageClass ;
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MATCH SEEKING LISTENER REQUESTER METHODS
	//

	@Override
	public boolean mslr_matchFound(
			long requestSerial, Nonce sessionNonce, WrappedSocket ws, String name, Nonce personalNonce) {
		
		logV("MatchFound: have " + mRequesters.size() + " requester(s)") ;
		
		UpdateListener ul = mwrUpdateListener.get() ;
		
		// two ways to exit this loop: if we have no requesters capable
		// of accepting this match, or we find one who does.  The number
		// of requesters decreases with every iteration.
		while( true ) {
			// Find a RequesterPack that is appropriate for this match.
			// A RequesterPack is appropriate IF: it has requested this particular
			// personal Nonce, OR it has not specified a p_nonce and the personal
			// nonce provided is not within our 'reserved nonces' list.
			// If one is found, it is removed from our requesters list.
			RequesterPack rp = null ;
			boolean hasMoreRequesters ;
			synchronized( this ) {
				Iterator<RequesterPack> iter = mRequesters.iterator() ;
				for ( ; iter.hasNext() && rp == null ; ) {
					RequesterPack rp_here = iter.next() ;
					if ( !rp_here.mRequester.mrr_stillRequesting() ) {
						logV("MatchFound: removing Requester which is not still requesting") ;
						iter.remove() ;
					} else {
						logV("MatchFound: comparing pnonce " + personalNonce + " against " + rp_here.mRemotePersonalNonce) ;
						if ( personalNonce.equals(rp_here.mRemotePersonalNonce) ) {
							logV("MatchFound: was a match") ;
							rp = rp_here ;
							iter.remove() ;
						}
					}
				}
				if ( rp == null && !mReservedNonces.contains(personalNonce) ) {
					logV("MatchFound: no reserved nonce found, looking for one not concerned with nonces...") ;
					iter = mRequesters.iterator() ;
					for ( ; iter.hasNext() && rp == null ; ) {
						RequesterPack rp_here = iter.next() ;
						if ( rp_here.mRemotePersonalNonce == null ) {
							rp = rp_here ;
							iter.remove() ;
						}
					}
				}
				hasMoreRequesters = mRequesters.size() > 0 ;
			}
			
			// If no requester was found, return false to end this match attempt.
			if ( rp == null ) {
				if ( hasMoreRequesters ) {
					logV("MatchFound: no Requester found as a match.  Restarting request.") ;
					// use the same session so we don't start a new thread...
					MatchSeekingListener.request(this, sessionNonce ) ;
				} else {
					logV("MatchFound: no Requester found as a match.  Ending request.") ;
					if ( ul != null )
						ul.mrul_didEndMatchRequest(requestSerial) ;
				}
				
				return false ;
			}
			
			// Otherwise, see if the requester wants this match.
			boolean accepted = rp.mRequester.mrr_matchFound(ws, name, personalNonce) ;
			
			// If accepted, return that value.  Otherwise loop again -- maybe there's
			// another requester who is interested in this match?
			if ( accepted ) {
				if ( ul != null ) {
					ul.mrul_didMatchSuccess(requestSerial) ;
					ul.mrul_didEndMatchRequest(requestSerial) ;
				}
				return true ;
			}
		}
	}

	@Override
	public boolean mslr_matchFailed(long requestSerial,
			Nonce sessionNonce,
			boolean recoverable,
			int failure, int error, int reason) {
		
		UpdateListener ul = mwrUpdateListener.get() ;
		boolean retry = informListenerOfFailure( ul, requestSerial, failure, error, reason ) ;
		
		// report failures to the first (chronological) requester.
		RequesterPack rp = null ;
		synchronized( this ) {
			if ( mRequesters.size() > 0 )
				rp = mRequesters.get(0) ;
		}
		
		if ( rp == null ) {
			if ( ul != null )
				ul.mrul_didEndMatchRequest(requestSerial) ;
			return false ;
		}
		
		boolean keepTrying = rp.mRequester.mrr_matchFailed(recoverable && retry, failure, error, reason) ;
		if ( !keepTrying ) {
			ul.mrul_didEndMatchRequest(requestSerial) ;
			synchronized( this ) {
				mRequesters.remove(rp) ;
			}
		}
		
		return keepTrying ;
	}
	
	
	

	@Override
	public void mslr_matchUpdate(long requestSerial, int update) {
		UpdateListener ul = mwrUpdateListener.get() ;
		
		if ( ul != null ) {
			switch( update ) {
			case MatchSeekingListener.UPDATE_STARTING:
				break ;		// who cares?
			case MatchSeekingListener.UPDATE_REQUESTING_MATCH_TICKET:
				ul.mrul_didBeginMatchTicketAcquisitionAttempt(requestSerial) ;
				break ;
			case MatchSeekingListener.UPDATE_RECEIVED_MATCH_TICKET:
				ul.mrul_didReceiveMatchTicket(requestSerial) ;
				break ;		// who cares?
			case MatchSeekingListener.UPDATE_PERFORMING_MATCH_TICKET_WORK:
				break ;		// who cares?
			case MatchSeekingListener.UPDATE_FINISHED_MATCH_TICKET_WORK:
				break ;		// who cares?
			case MatchSeekingListener.UPDATE_SENDING_MATCH_REQUEST:
				ul.mrul_didBeginMatchSeekingAttempt(requestSerial) ;
				break ;
			case MatchSeekingListener.UPDATE_RECEIVED_MATCH_PROMISE:
				ul.mrul_didBeginWaitingForMatch(requestSerial) ;
				break ;
			case MatchSeekingListener.UPDATE_RECEIVED_MATCH:
				ul.mrul_didMatchStart(requestSerial) ;
				break ;
			case MatchSeekingListener.UPDATE_MAKING_MATCHED_SOCKET:
				break ;		// who cares?
			case MatchSeekingListener.UPDATE_MADE_MATCHED_SOCKET:
				break ;		// who cares?
			case MatchSeekingListener.UPDATE_STARTING_INFORMATION_EXCHANGE:
				break ;		// who cares?
			case MatchSeekingListener.UPDATE_FINISHED_INFORMATION_EXCHANGE:
				break ;
			}
		}
	}
	
	
	/**
	 * Informs the provided listener of failure, if non-null, and returns whether a
	 * retry is possible (we only retry if there is still a requester interested in
	 * getting a match).
	 * 
	 * @param ul
	 * @param serial
	 * @param failure
	 * @param error
	 * @param reason
	 * @return
	 */
	private boolean informListenerOfFailure( UpdateListener ul, long serial, int failure, int error, int reason ) {
		boolean retry = true ;
		
		switch( failure ) {
		case MatchSeekingListener.FAILURE_UNRESPONSIVE_REQUESTER:
			return false ;
		case MatchSeekingListener.FAILURE_MATCH_TICKET_REQUEST_COMMUNICATION:
			if ( ul != null )
				retry = ul.mrul_didFailMatchTicketAcquisitionAttempt(serial, error, reason) ;
			return retry ;
		case MatchSeekingListener.FAILURE_MATCHSEEKER_CONTACT:
			if ( ul != null )
				ul.mrul_didFailToConnectToMatchmaker(serial) ;
			return false ;
		case MatchSeekingListener.FAILURE_MATCHSEEKER_REJECTION:
			if ( ul != null )
				retry = ul.mrul_didReceiveRejectionFromMatchmaker(serial, matchSeekerReasonToMRReason(error)) ;
			return retry ;
		case MatchSeekingListener.FAILURE_MATCHSEEKER_EXCEPTION:
			if ( ul != null )
				ul.mrul_didFailToConnectToMatchmaker(serial) ;
			return true ;
		case MatchSeekingListener.FAILURE_MATCHED_SOCKET_MAKER_FAILED:
			if ( ul != null )
				retry = ul.mrul_didMatchFailure(serial, UpdateListener.MATCH_FAILURE_STAGE_UDP_HOLE_PUNCH) ;
			return retry ;
		case MatchSeekingListener.FAILURE_IDENTITY_EXCHANGE_FAILED:
			if ( ul != null )
				retry = ul.mrul_didMatchFailure(serial, UpdateListener.MATCH_FAILURE_STAGE_IDENTITY_EXCHANGE) ;
			return retry ;
		}
		
		return retry ;
	}
	
	public UpdateListener.Rejection matchSeekerReasonToMRReason( int reason ) {
		switch( reason ) {
		case MatchSeeker.Delegate.REJECTION_FULL:
			return UpdateListener.Rejection.FULL ;
		case MatchSeeker.Delegate.REJECTION_INVALID_NONCE:
			return UpdateListener.Rejection.INVALID_NONCE ;
		case MatchSeeker.Delegate.REJECTION_NONCE_IN_USE:
			return UpdateListener.Rejection.NONCE_IN_USE ;
		case MatchSeeker.Delegate.REJECTION_PORT_RANDOMIZATION:
			return UpdateListener.Rejection.PORT_RANDOMIZATION ;
		case MatchSeeker.Delegate.REJECTION_MATCHTICKET_ADDRESS:
			return UpdateListener.Rejection.MATCHTICKET_ADDRESS ;
		case MatchSeeker.Delegate.REJECTION_MATCHTICKET_CONTENT:
			return UpdateListener.Rejection.MATCHTICKET_CONTENT ;
		case MatchSeeker.Delegate.REJECTION_MATCHTICKET_SIGNATURE:
			return UpdateListener.Rejection.MATCHTICKET_SIGNATURE ;
		case MatchSeeker.Delegate.REJECTION_MATCHTICKET_PROOF:
			return UpdateListener.Rejection.MATCHTICKET_PROOF ;
		case MatchSeeker.Delegate.REJECTION_MATCHTICKET_EXPIRED:
			return UpdateListener.Rejection.MATCHTICKET_EXPIRED ;
		case MatchSeeker.Delegate.REJECTION_UNSPECIFIED:
			return UpdateListener.Rejection.UNSPECIFIED ;
		}
		
		throw new IllegalArgumentException("Don't understand reason " + reason) ;
	}

	@Override
	public boolean mslr_requesting(long requestSerial) {
		return mRequesters.size() > 0 ;
	}

	@Override
	public InternetLobby mslr_getInternetLobby(long requestSerial) {
		return mInternetLobby ;
	}

	@Override
	public InternetLobbyGame mslr_getInternetLobbyGame(long requestSerial) {
		return mInternetLobbyGame ;
	}

	@Override
	public Nonce mslr_getPersonalNonce(long requestSerial) {
		return mPersonalNonce ;
	}

	@Override
	public String mslr_getName(long requestSerial) {
		return mName ;
	}

	@Override
	public byte mslr_getIntent(long requestSerial) {
		return mIntent ;
	}

	@Override
	public String mslr_getUserID(long requestSerial) {
		return mUserID ;
	}

	@Override
	public String mslr_getXL(long requestSerial) {
		return mXL ;
	}

	@Override
	public SocketAddress mslr_getMatchmakerAddress(long requestSerial) {
		return mMatchmakerAddress ;
	}

	@Override
	public int [] mslr_getCommunicationPortRange(long requestSerial) {
		return mContactPortRange ;
	}

	@Override
	public AdministratedWrappedSocketAdministrator mslr_getWrappedSocketAdministrator(
			long requestSerial) {
		return mAdministrator ;
	}

	@Override
	public Class<?> mslr_getMessageClass() {
		return mMessageClass ;
	}

	//
	////////////////////////////////////////////////////////////////////////////
	
}
