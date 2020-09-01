package com.peaceray.quantro.communications.messagepassing;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.communications.wrapper.AutonomousWrappedSocketFactory;


/**
 * MessagePassingDirectClientConnection.
 * 
 * A MessagePassingSocket that uses a direct connection to a server.
 * 
 * Apart from sending a DisconnectMessagePassingConnection message
 * upon disconnect, and absorbing one when the peer sends it, this Connection 
 * does not send or receive messages on its own (for instance, it does
 * NOT respond to nonce requests - they are passed to the user with
 * hasMessage(), getMessage()).
 * 
 * Calls to connect() succeed or fail within the call; a
 * MessagePassingDirectClientConnection will never enter the "Pending"
 * state.  You can't even get this state using multiple threads, because
 * methods are synchronized.
 * 
 * Instantiated with a SocketAddress, the DirectConnection will
 * merely open a TCP socket to that address.  If successful, it
 * will wrap that socket and its remaining operation is determined
 * by MessagePassingWrappedSocketConnection.
 * 
 * @author Jake
 *
 */
public class MessagePassingDirectClientConnection
		extends MessagePassingWrappedSocketConnection
		implements MessagePassingConnectionBecomesClient {
	
	
	private boolean active ;
	private ArrayList<SocketAddress> socketAddresses ;
	private int activeSocketAddress ;
	private int timeoutMillis ;
	
	private Nonce nonce ;
	private Nonce localPersonalNonce ;
	private String localName ;
	
	ClientInterrogationResponderThread currentInterrogationResponderThread ;
	
	
	public MessagePassingDirectClientConnection( Class<?> msgClass, Nonce nonce, Nonce localPersonalNonce, String localName, SocketAddress ... addrs ) throws IllegalArgumentException, InstantiationException, IllegalAccessException {
		this( msgClass, nonce, localPersonalNonce, localName, 1000, addrs ) ;		// default 1 second timeout
	}
	
	public MessagePassingDirectClientConnection( Class<?> msgClass, Nonce nonce, Nonce localPersonalNonce, String localName, int timeoutMillis, SocketAddress ... addrs ) throws IllegalArgumentException, InstantiationException, IllegalAccessException {
		this.socketAddresses = new ArrayList<SocketAddress>() ;
		for ( int i = 0; i < addrs.length; i++ )
			this.socketAddresses.add(addrs[i]) ;
		this.activeSocketAddress = -1 ;
		this.timeoutMillis = timeoutMillis ;
		
		this.nonce = nonce ;
		this.localPersonalNonce = localPersonalNonce ;
		this.localName = localName ;
		
		this.currentInterrogationResponderThread = null ;
		
		this.active = false ;
		this.setConnectionStatus(Status.INACTIVE) ;
		
		// Allocate queue
		this.allocateIncomingMessageQueue(msgClass, 5) ;
	}

	@Override
	public synchronized void activate() throws IllegalStateException {
		if ( active ) {
			throw new IllegalStateException("Already active") ;
		}
		
		active = true ;
		this.setConnectionStatus(Status.NEVER_CONNECTED) ;
	}

	@Override
	public synchronized void deactivate() throws IllegalStateException {
		if ( !active ) {
			throw new IllegalStateException("Already inactive") ;
		}
		
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
		active = false ;
	}

	@Override
	public synchronized boolean isActive() {
		return active ;
	}

	@Override
	public synchronized void connect() throws IllegalStateException {
		Status status = this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.CONNECTED
				|| status == MessagePassingConnection.Status.BROKEN
				|| status == MessagePassingConnection.Status.PEER_DISCONNECTED
				|| status == MessagePassingConnection.Status.FAILED )
			throw new IllegalStateException("Connected, or at least not disconnected.") ;
		
		
		this.setConnectionStatus(MessagePassingConnection.Status.PENDING) ;
		
		// attempt to start a connection attempt, iterating through socket addresses
		// until we run out or one "takes."
		for ( activeSocketAddress = 0; activeSocketAddress < socketAddresses.size(); activeSocketAddress++ ) {
			if ( startConnectionAttempt( socketAddresses.get(activeSocketAddress) ) )
				return ;	// done!
		}
		
		// if we get here, we failed.
		this.setConnectionStatus(MessagePassingConnection.Status.FAILED) ;
	}
	
	
	/**
	 * Attempts to start a connection to the specified SocketAddress.  Returns success.
	 * 
	 * If returns 'true', a WrappedSocket will have been created (to the specified address
	 * with 'timeoutMillis' as its timeout) and given to a ClientInterrogationResponderThread.
	 * The member variable 'currentInterrogationResponderThread' will be set to that CIRThread
	 * instance, and the thread will have been started.
	 * 
	 * If returns 'false', no open sockets will remain (if opened by this method before
	 * failure, they will be closed) and currentInterrogationResponderThread will 
	 * be set to 'null.'
	 * 
	 * This method neither checks nor changes the current Connection Status.
	 * 
	 * @param sa
	 * @return
	 */
	private boolean startConnectionAttempt( SocketAddress sa ) {
		Socket bareSocket = null ;
		WrappedSocket wsock = null ;
		boolean ok = false ;
		
		try {
			bareSocket = new Socket( ) ;
			bareSocket.connect( sa, timeoutMillis ) ;	// 1 second timeout
			wsock = AutonomousWrappedSocketFactory.wrap( bareSocket ) ;
			
			// Launch an interrogation responder thread.
			currentInterrogationResponderThread = new ClientInterrogationResponderThread(
					this, wsock, 5000) ;		// max 5 second interrogation
			
			currentInterrogationResponderThread.start() ;
			ok = true ;
		} catch( IOException e ) {
			// whoops
		}
		
		if (!ok) {
			try {
				wsock.close() ;
			} catch ( Exception e ) { }
			try {
				bareSocket.close() ;
			} catch ( Exception e ) { }
			
			currentInterrogationResponderThread = null ;
		}
		
		return ok ;
	}
	
	@Override
	public synchronized void disconnect() throws IllegalStateException {
		// make sure we're not tracking an InterrogationResponderThread...
		currentInterrogationResponderThread = null ;
		
		super.disconnect() ;
	}
	
	
	/**
	 * This basic Connection does not have a Nonce.
	 * 
	 * @return null
	 */
	public synchronized Nonce getNonce() {
		return nonce ;
	}
	
	/**
	 * This basic client Connection does not have personal or remote information.
	 * 
	 * @return null
	 */
	public synchronized Nonce getLocalPersonalNonce() {
		return localPersonalNonce ;
	}
	
	/**
	 * This basic client Connection does not have personal or remote information.
	 * 
	 * @return null
	 */
	public synchronized String getLocalName() {
		return localName ;
	}

	/**
	 * This basic client Connection does not have personal or remote information.
	 * 
	 * @return null
	 */
	public synchronized Nonce getRemotePersonalNonce() {
		return null ;
	}
	
	/**
	 * This basic client Connection does not have personal or remote information.
	 * 
	 * @return null
	 */
	public synchronized String getRemoteName() {
		return null ;
	}
	
	@Override
	public synchronized void updateLocalName( String name ) {
		localName = name ;
	}
	
	@Override
	protected synchronized void updateRemoteName( String name ) {
		// do nothing!
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// EXTRA METHODS FOR MessagePassingConnectionBecomesClient
	//
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The Connection, which must be 'pending', should accept the
	 * provided socket and transition into Connected state (or
	 * Broken, or some such, but not remain Pending).
	 * 
	 * Returns whether the socket was accepted and the connection
	 * connected.  Returns 'false' if, for whatever reason, the
	 * socket was rejected - however, in this case we guarantee
	 * that the state is no longer pending.
	 * 
	 * There will be exactly 1 call to 'becomeClient' or 'failedToBecomeClient'
	 * per attempt.  If 'becomeClient' returns 'false', 'failedToBecomeClient'
	 * will NOT be called.
	 * 
	 * Throws IllegalStateException if not pending.
	 * 
	 * @param wsock
	 * @param cinfo
	 * @return Are we now connected using the provided wrapped socket?
	 */
	public synchronized boolean becomeClient( Object caller, WrappedSocket wsock ) throws IllegalStateException {
		if ( caller != currentInterrogationResponderThread || currentInterrogationResponderThread == null )
			return false ;
		if ( this.connectionStatus() != MessagePassingConnection.Status.PENDING )
			return false ;
		
		currentInterrogationResponderThread = null ;
		boolean ok = false ;
		this.setConnectionStatus(MessagePassingConnection.Status.CONNECTED) ;
		try {
			this.didConnect(wsock) ;
			// reorder our socket addresses to prioritize this address from now on
			if ( activeSocketAddress != 0 ) {
				socketAddresses.add(0, socketAddresses.remove(activeSocketAddress)) ;
				activeSocketAddress = 0 ;
			}
			ok = true ;
		} catch (InstantiationException e) {
			e.printStackTrace() ;
			// whoops
		} catch (IllegalAccessException e) {
			e.printStackTrace() ;
			// whoops
		} catch (InterruptedException e) {
			e.printStackTrace() ;
			// whoops
		}
		
		
		return ok ;
	}

	/**
	 * The Connection, which must be 'pending', will transition
	 * to FAILED status after this call.
	 * 
	 * Make this call when, for whatever reason, our attempt to connect
	 * with a client failed.
	 * 
	 * There will be exactly 1 call to 'becomeClient' or 'failedToBecomeClient'
	 * per attempt.  If 'becomeClient' returns 'false', 'failedToBecomeClient'
	 * will NOT be called.
	 * 
	 * @throws IllegalStateException
	 */
	public synchronized void failedToBecomeClient( Object caller ) throws IllegalStateException {
		if ( caller != currentInterrogationResponderThread || currentInterrogationResponderThread == null )
			return ;
		if ( this.connectionStatus() != MessagePassingConnection.Status.PENDING )
			return ;
		
		// try the next socket address in line.
		activeSocketAddress++ ;
		if ( activeSocketAddress < socketAddresses.size() ) {
			if ( !startConnectionAttempt( socketAddresses.get(activeSocketAddress) ) ) {
				this.setConnectionStatus(MessagePassingConnection.Status.FAILED) ;
			}
		} else {
			// we've tried them all.  Terminate this connection attempt.
			currentInterrogationResponderThread = null ;
			this.setConnectionStatus(Status.FAILED) ;
		}
	}
}
