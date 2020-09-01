package com.peaceray.quantro.communications.messagepassing.matchseeker;

import java.net.SocketAddress;

import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingWrappedSocketConnection;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.AdministratedWrappedSocketAdministrator;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.utils.Debug;



/**
 * A variation of the "MessagePassingMediatedConnection."  This class
 * uses UDP packets to and from quantro_matchmaker to schedule a match
 * with a play partner.
 * 
 * 
 * 
 * @author Jake
 *
 */
public class MessagePassingMatchSeekerConnection extends
		MessagePassingWrappedSocketConnection
		implements MatchRouter.Requester {
	
	private static final boolean DEBUG_LOG = true && Debug.LOG ;
	private static final String TAG = "MessagePassingMatchSeekerConnection" ;
	
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
	
	private boolean mActive ;
	
	// Personal info
	Nonce mNonce ;
	Nonce mPersonalNonce ;
	String mName ;
	Nonce mRemotePersonalNonce ;
	String mRemoteName ;
	boolean mAcceptOnlyRemoteNonce ;
	
	// Our Match Router
	MatchRouter mMatchRouter ;
	
	public MessagePassingMatchSeekerConnection(
			MatchRouter matchRouter,
			AdministratedWrappedSocketAdministrator administrator,
			Nonce nonce,
			Nonce personalNonce, String name,
			Nonce remotePersonalNonce, Class<?> msgClass ) throws IllegalArgumentException, InstantiationException, IllegalAccessException {
		
		mMatchRouter = matchRouter ;
		
		mPersonalNonce = personalNonce ;
		mName = name ;
		
		mNonce = nonce ;
		
		mRemotePersonalNonce = remotePersonalNonce ;
		
		mAcceptOnlyRemoteNonce = mRemotePersonalNonce != null ;
		
		// Allocate queue
		this.allocateIncomingMessageQueue(msgClass, 5) ;
	}
	
	

	@Override
	public void activate() throws IllegalStateException {
		if ( mActive ) {
			throw new IllegalStateException("Already active") ;
		}
		
		if ( !mAcceptOnlyRemoteNonce )
			mRemotePersonalNonce = null ;
		mRemoteName = null ;
		
		mActive = true ;
		this.setConnectionStatus(Status.NEVER_CONNECTED) ;
	}

	@Override
	public void deactivate() throws IllegalStateException {
		if ( !mActive )
			throw new IllegalStateException("Already inactive") ;
			
		// remember the message queue our superclass uses?  This queue
		// is emptied upon disconnect().  That means, if we are in 
		// status_never_connected or status_disconnect, our message
		// queue must be empty.
		Status status = this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.CONNECTED
				|| status == MessagePassingConnection.Status.BROKEN
				|| status == MessagePassingConnection.Status.PEER_DISCONNECTED
				|| status == MessagePassingConnection.Status.FAILED )
			throw new IllegalStateException("Connected, or at least not disconnected.") ;
		
		// do eeeeeet
		this.setConnectionStatus(Status.INACTIVE) ;
		mActive = false ;
	}

	@Override
	public boolean isActive() {
		return mActive ;
	}

	@Override
	public void connect() throws IllegalStateException {
		Status status = this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.CONNECTED
				|| status == MessagePassingConnection.Status.BROKEN
				|| status == MessagePassingConnection.Status.PEER_DISCONNECTED
				|| status == MessagePassingConnection.Status.FAILED )
			throw new IllegalStateException("Connected, or at least not disconnected.") ;
		
		// A connection attempt means opening a MatchSeeker -- ASSUMING we have a matchticket.
		
		try {
			this.setConnectionStatus(MessagePassingConnection.Status.PENDING) ;
			
			mMatchRouter.request(this, mRemotePersonalNonce) ;
		} catch( Exception e ) {
			try {
				mMatchRouter.cancel(this) ;
			} catch ( Exception ex ) { }
			e.printStackTrace() ;
			this.setConnectionStatus(MessagePassingConnection.Status.FAILED) ;
		}
	}
	
	
	@Override
	public synchronized void disconnect() throws IllegalStateException {
		if ( this.connectionStatus() == MessagePassingConnection.Status.PENDING ) {
			// Tell the MatchSeekerListener to stop
			mMatchRouter.cancel(this) ;
		}
		
		mRemoteName = null ;
		if ( !mAcceptOnlyRemoteNonce )
			mRemotePersonalNonce = null ;
		
		super.disconnect() ;
	}

	@Override
	public Nonce getNonce() {
		return mNonce ;
	}

	@Override
	public Nonce getLocalPersonalNonce() {
		return mPersonalNonce ;
	}

	@Override
	public String getLocalName() {
		return mName ;
	}

	@Override
	public Nonce getRemotePersonalNonce() {
		return mRemotePersonalNonce ;
	}

	@Override
	public String getRemoteName() {
		return mRemoteName ;
	}
	
	@Override
	public SocketAddress getRemoteSocketAddress() {
		// we might have a pass-through; this is a meaningless
		// value.
		return null ;
	}

	
	@Override
	public synchronized void updateLocalName( String name ) {
		mName = name ;
	}
	
	@Override
	protected synchronized void updateRemoteName( String name ) {
		mRemoteName = name ;
	}

	
	
	////////////////////////////////////////////////////////////////////////////
	// 
	// MATCH ROUTING
	
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
	public boolean mrr_matchFound( WrappedSocket ws, String name, Nonce personalNonce ) {
		if ( connectionStatus() != Status.PENDING ) {
			return false ;
		}
		
		if ( mAcceptOnlyRemoteNonce && !personalNonce.equals(mRemotePersonalNonce) ) {
			return false ;
		}
		
		log("mrr_matchFound with " + name + " at " + ws.getRemoteSocketAddress()) ;
		
		mRemotePersonalNonce = personalNonce ;
		mRemoteName = name ;
		
		// set ourselves up as an open connection.
		try {
			this.didConnect(ws) ;
			this.setConnectionStatus(Status.CONNECTED) ;
			return true ;
		} catch ( Exception e ) {
			log(e, "mrr_matchFound error occurred; transitioning to FAILED") ;
			this.setConnectionStatus(Status.FAILED) ;
			return false ;
		}
	}
	
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
	public boolean mrr_matchFailed( boolean recoverable, int failure, int error, int reason ) {
		if ( connectionStatus() != Status.PENDING ) {
			return false ;
		}
		
		log("mrr_matchFailed") ;
		
		boolean retry = true ;
		
		switch( failure ) {
		case MatchSeekingListener.FAILURE_MATCHSEEKER_CONTACT:
			retry = false ;
			break ;
		}
		
		if ( !recoverable || !retry )
			setConnectionStatus(Status.FAILED) ;
		
		return retry && recoverable ;
	}
	
	/**
	 * Are we still requesting a match?
	 */
	public boolean mrr_stillRequesting() {
		return connectionStatus() == Status.PENDING ;
	}
	
	
	//
	////////////////////////////////////////////////////////////////////////////

}
