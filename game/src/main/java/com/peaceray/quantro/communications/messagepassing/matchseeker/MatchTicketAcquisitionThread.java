package com.peaceray.quantro.communications.messagepassing.matchseeker;

import java.util.Hashtable;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.InternetLobbyGame;
import com.peaceray.quantro.lobby.MatchTicketOrder;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;



/**
 * A MatchTicketAcquisitionThread serves the function of acquiring a matchticket
 * for our nonce, intent and hosting status.  The thread works in the background
 * through two basic steps: first, we acquire a matchticket from quantro_lobby_web
 * based on our settings (terminating with a failure report if we can't contact it).
 * Second, we perform the necessary work to get our effort proof for the matchticket
 * and report that proof (along with the ticket string) to a listener.
 * 
 * Threads can be terminated at any point.  We provide a guarantee of exactly one
 * listener callback once begun, even in the case of early termination.
 * 
 * Additionally, status queries are provided if you're interested.  These queries
 * do not interrupt the thread in any way, but the result may lag behind the guaranteed
 * callback (or preceed it) slightly.
 * 
 * Clients must provide an intent, nonce, and (optionally) an XL key for priority.
 * Hosts must provide an intent, nonce, editKey and (optionally) an XL key for priority.
 * 
 * The distinction between a host and a client, as far as this object is concerned,
 * is the presence or absence of an edit key.
 * 
 * @author Jake
 *
 */
public class MatchTicketAcquisitionThread extends Thread {
	
	// Statuses
	public static final int STATUS_UNSTARTED = 0 ;
	public static final int STATUS_REQUESTING_TICKET = 1 ;
	public static final int STATUS_FAILED_REQUESTING_TICKET = 2 ;
	public static final int STATUS_WORKING_TICKET = 3 ;
	public static final int STATUS_CANCELED = 4 ;
	public static final int STATUS_FINISHED = 5 ;
	
	private int mStatus ;

	// useful info
	private Listener mListener ;
	
	private InternetLobby mLobby ;			// exactly one of these two must be non-null.  We treat it as (essentially) immutable, only requesting matchtickets.
	private InternetLobbyGame mGame ; 		// exactly one of these two must be non-null.  We treat it as (essentially) immutable, only requesting matchtickets.
	private String mUserID ;
	private String mXL ;
	
	private long mInitialDelay ;
	
	private boolean mCanceled ;
	
	public static final int INTENT_LOBBY = 0 ;
	public static final int INTENT_GAME = 1 ;
	

	
	public MatchTicketAcquisitionThread( Listener listener, InternetLobby lobby, String userID, String xl ) {
		
		mListener = listener ;
		mLobby = lobby ;
		mGame = null ;
		mUserID = userID ;
		mXL = xl ;
		
		if ( mListener == null || mLobby == null || mUserID == null )
			throw new NullPointerException("Must provide non-null parameters (other than xl)") ;
		
		mStatus = STATUS_UNSTARTED ;
		mCanceled = false ;	
		
		mInitialDelay = 0 ;
	}
	
	public MatchTicketAcquisitionThread( Listener listener, InternetLobbyGame game, String userID, String xl ) {
		
		mListener = listener ;
		mLobby = null ;
		mGame = game ;
		mUserID = userID ;
		mXL = xl ;
		
		if ( mListener == null || mGame == null || mUserID == null )
			throw new NullPointerException("Must provide non-null parameters (other than xl)") ;
		
		mStatus = STATUS_UNSTARTED ;
		mCanceled = false ;	
		
		mInitialDelay = 0 ;
	}
	
	
	
	/**
	 * Queues up a cancelation.  Because this object is a thread, it will not be canceled 
	 * immediately (and may never be canceled, if it terminates for some other reason first).
	 * 
	 * We provide an "exactly one Listener callback" guarantee, and that guarantee is maintained
	 * even if this method is called.
	 * 
	 * This method has no effect if the Listener callback has already occurred.
	 * 
	 */
	public void cancel() {
		mCanceled = true ;
	}
	
	
	public int status() {
		return mStatus ;
	}
	
	public void setInitialDelay( long millis ) {
		mInitialDelay = millis ;
	}
	
	
	@Override
	public void run() {
		
		// canceled?
		if ( mCanceled ) {
			mStatus = STATUS_CANCELED ;
			mListener.mtat_canceled(this) ;
			return ;
		}
		
		mStatus = STATUS_REQUESTING_TICKET ;
		
		// initial delay
		long delay = mInitialDelay ;
		while ( delay > 0 ) {
			try {
				Thread.sleep( Math.min( 500, delay ) );
			} catch ( InterruptedException ie ) {
				ie.printStackTrace() ;
			}
			
			delay -= 500 ;
			
			if ( mCanceled ) {
				mStatus = STATUS_CANCELED ;
				mListener.mtat_canceled(this) ;
				return ;
			}
		}

		// attempt to request a matchticket.
		MatchTicketOrder mto ;
		try {
			if ( mLobby != null )
				mto = mLobby.matchticketRequest(mUserID, mXL) ;
			else
				mto = mGame.matchticketRequest(mUserID, mXL) ;
		} catch( CommunicationErrorException cee ) {
			cee.printStackTrace() ;
			mStatus = STATUS_FAILED_REQUESTING_TICKET ;
			mListener.mtat_failedRequestingMatchTicket(this, cee.getError(), cee.getErrorReason()) ;
			return ;
		}
		
		// canceled?
		if ( mCanceled ) {
			mStatus = STATUS_CANCELED ;
			mListener.mtat_canceled(this) ;
			return ;
		}
		
		// proceed on - perform the work.
		mStatus = STATUS_WORKING_TICKET ;
		
		while ( !mCanceled && !mto.isComplete() ) {
			mto.performWork(1000) ;		// for 1 second
		}
		
		// canceled?
		if ( mCanceled ) {
			mStatus = STATUS_CANCELED ;
			mListener.mtat_canceled(this) ;
			return ;
		}
		
		// finished!
		mStatus = STATUS_FINISHED ;
		mListener.mtat_finished(this, mto.getTicket(), mto.getProof()) ;
	}
	
	
	/**
	 * Callbacks used by the Thread during its operation.  We guarantee that exactly
	 * one of these methods will be called, probably as the final operation of the thread.
	 * 
	 * @author Jake
	 *
	 */
	public interface Listener {
		
		public void mtat_failedRequestingMatchTicket( MatchTicketAcquisitionThread caller, int error, int reason ) ;
		
		public void mtat_canceled( MatchTicketAcquisitionThread caller ) ;
		
		public void mtat_finished( MatchTicketAcquisitionThread caller, String matchticket, Nonce matchticketProof ) ;
		
	}
	
}
