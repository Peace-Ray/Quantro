package com.peaceray.quantro.communications.messagepassing;

import com.peaceray.quantro.communications.ClientInformation;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;


/**
 * MessagePassingDirectServerConnection: a direct, unmediated connection
 * to a client.  We don't make assumptions about the game role played by
 * the users on either end of the connection; "client" and "server" refer
 * only to who connects to whom using TCP sockets.
 * 
 * A direct server connection is based on an open and listening ServerSocket.
 * However, we do not open a unique ServerSocket for each Connection.  Instead,
 * the first Connection using a particular port will launch a background thread
 * for the purpose of receiving connections.  This thread, and an interrogation
 * thread, will listen for and interrogate connections.  Upon completion of 
 * interrogation, the thread will examine the available DirectServerConnections
 * - available meaning those which are connecting - and give the WrappedSocket
 * to the Connection with appropriate nonce / pnonce values.
 * 
 * DirectServerConnections are instantiated with a nonce value and, optionally,
 * a pnonce.  They have two basic settings: whether the nonce is required or
 * provided (we either require that clients possess the correct nonce at
 * interrogation, or ignore the nonce they provided and instead send them our
 * nonce value upon connection), and whether pnonce is retained between connections
 * or forgotten (if retained, the Connection functions as a dedicated slot for
 * a particular user; if forgotten, it is an "open" slot that, once a person 
 * disconnects, may be used by another player).
 * 
 * @author Jake
 *
 */
public class MessagePassingDirectServerConnection
		extends MessagePassingWrappedSocketConnection
		implements MessagePassingConnectionAcceptsClients {
	
	private Nonce nonce ;
	private Nonce personalNonce ;
	private String name ;
	
	private boolean active ;
	
	// connection parameters
	private int port ;
	private boolean requiresNonce ;
	private boolean openSlot ;
	
	
	/**
	 * Instantiate a MessagePassingDirectServerConnection.
	 * 
	 * The provided 'port' is the port number on which we should listen
	 * for incoming connections.  When we call 'connect()', we will (attempt to)
	 * listen for connections on that port; either on a new thread which we
	 * start, or on an existing thread which we notify of our intention to 
	 * receive a connection.
	 * 
	 * Nonce must not be null.  personalNonce may be null.
	 * 
	 * If requireNonce, we require that the connected client knows the
	 * Nonce value and provides it.
	 * 
	 * If openSlot, we accept connections from players with ANY personal nonce.
	 * Otherwise, we require them to provide the personalNonce we specify.
	 * If openSlot=false, and personalNonce=null, then we leave the slot open
	 * to whoever UNTIL someone connects and provides a personalNonce; then
	 * we lock that pnonce to that user, and if they disconnect, the Connection
	 * is then left for the same user (or at least the same personal nonce).
	 * 
	 * Finally, a note on the listening thread: as mentioned, multiple
	 * Connections with the same port will be collected within the same 
	 * thread using the same ServerSocket (if, of course, they are connect()ing 
	 * simultaneously).  After a new connection is interrogated, it will be provided
	 * to a connect()ing Connection, based on the following priority (provide to the
	 * connect()ing socket whose parameters match the highest available on this list)
	 * 
	 * requireNonce:true openSlot:false nonce=nonce, personalNonce=personalNonce
	 * requireNonce:false openSlot:false nonce=? personalNonce=personalNonce
	 * requireNonce:true openSlot:false nonce=nonce personalNonce:? <-- a "closed slot" with no personalNonce yet assigned
	 * requireNonce:false openSlot:false nonce=? personalNonce:?
	 * requireNonce:true openSlot:true nonce=nonce
	 * requireNonce:false openSlot:true nonce=?
	 * 
	 * In other words, we first prefer closed slots; we match players to
	 * the slot for their personalNonce.  Next we prefer closed slots which
	 * do not have personal nonces assigned.  Finally, open slots are used.
	 * In all of these scenarios, we first try to fit to "require nonce"
	 * connections, then fall back to "provide nonce" connections.
	 * 
	 * Of course, this progression only applies to Connections which are
	 * connect()ing simultaneously on the same port.
	 * 
	 * @param port
	 * @param nonce
	 * @param personalNonce
	 * @param requireNonce
	 * @param openSlot
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IllegalArgumentException 
	 */
	public MessagePassingDirectServerConnection( int port, Class<?> msgClass,
			Nonce nonce, Nonce personalNonce,
			boolean requireNonce, boolean openSlot ) throws IllegalArgumentException, InstantiationException, IllegalAccessException {
		
		this.port = port ;
		this.requiresNonce = requireNonce ;
		this.openSlot = openSlot ;
		
		if ( nonce == null )
			throw new IllegalArgumentException("Must specify a nonce") ;
		
		if ( openSlot && personalNonce != null )
			throw new IllegalArgumentException("Open sockets should not specify a personal nonce.") ;
		
		this.nonce = nonce ;
		this.personalNonce = personalNonce ;
		this.name = null ;
		
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
		if ( !active ) 
			throw new IllegalStateException("Not active!") ;
		
		Status status = this.connectionStatus() ;
		if ( status == MessagePassingConnection.Status.PENDING
				|| status == MessagePassingConnection.Status.CONNECTED
				|| status == MessagePassingConnection.Status.BROKEN
				|| status == MessagePassingConnection.Status.PEER_DISCONNECTED
				|| status == MessagePassingConnection.Status.FAILED )
			throw new IllegalStateException("Connected, or at least not disconnected.") ;
		
		// We let the DirectClientListener know we're interested in a connection.
		this.setConnectionStatus(Status.PENDING) ;
		DirectClientListener.listenOnPort(this.port, this) ;
	}
	
	
	/**
	 * Our superclass has no idea how to handle a pending disconnect.
	 * We handle that specific case ourselves.  Otherwise, we let
	 * the superclass take over.
	 */
	@Override
	public synchronized void disconnect() throws IllegalStateException {
		if ( active ) {
			Status status = this.connectionStatus() ;
			if ( status == MessagePassingConnection.Status.PENDING ) {
				DirectClientListener.stopListeningOnPort(this.port, this) ;
				this.setConnectionStatus(Status.DISCONNECTED) ;
				return ;
			}
		}
		
		// superclass
		super.disconnect() ;
	}

	
	
	/////////////////////////////////////////////////////////////////////////////
	//
	// MessagePassingConnectionAcceptsClients interface methods.
	//

	@Override
	public synchronized boolean acceptClient(Object caller, WrappedSocket wsock, ClientInformation cinfo) throws IllegalStateException {
		if ( !active ) 
			throw new IllegalStateException("Not active!") ;
		
		Status status = this.connectionStatus() ;
		if ( status != MessagePassingConnection.Status.PENDING )
			throw new IllegalStateException("Not pending!") ;
		
		// do all the checks we need to.
		// Matches nonce?
		boolean ok = true ;
		if ( this.requiresNonce && !cinfo.getNonce().equals(this.nonce) )
			ok = false ;
		
		// Matches personal nonce?
		if ( !this.openSlot && this.personalNonce != null && !cinfo.getPersonalNonce().equals(this.personalNonce) ) 
			ok = false ;
		
		if ( ok ) {
			// everything seems to check out.  Take note of the name and personal nonce.
			this.personalNonce = cinfo.getPersonalNonce() ;
			this.name = cinfo.getName() ;
			
			this.setConnectionStatus(MessagePassingConnection.Status.CONNECTED) ;
			try {
				this.didConnect(wsock) ;
				return true ;
			} catch( Exception e ) {
				this.setConnectionStatus(Status.FAILED) ;
			}
		}
		
		return false ;
		
	}
	
	

	/**
	 * The Connection, which must be 'pending', will transition
	 * to FAILED status after this call.
	 * 
	 * Make this call when, for whatever reason, our attempt to connect
	 * with a client failed.
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	@Override
	public synchronized void failedToFindClient( Object caller ) throws IllegalStateException {
		if ( !active ) 
			throw new IllegalStateException("Not active!") ;
		
		Status status = this.connectionStatus() ;
		if ( status != MessagePassingConnection.Status.PENDING )
			throw new IllegalStateException("Not pending!") ;
		
		this.setConnectionStatus(MessagePassingConnection.Status.FAILED) ;
	}


	@Override
	public synchronized boolean requiresNonce() {
		return this.requiresNonce ;
	}


	@Override
	public synchronized boolean requiresPersonalNonce() {
		return !this.openSlot && this.personalNonce != null ;
	}


	@Override
	public synchronized boolean remembersPersonalNonce() {
		return !this.openSlot ;
	}


	/**
	 * The Nonce associated with this connection.
	 * @return Our nonce.
	 */
	public synchronized Nonce getNonce() {
		return this.nonce ;
	}
	
	/**
	 * The server does not place a personalNonce on the local side of the connection.
	 * 
	 * @return null
	 */
	public synchronized Nonce getLocalPersonalNonce() {
		return null ;
	}
	
	/**
	 * The server does not place a name on the local side of the connection.
	 * 
	 * @return null
	 */
	public synchronized String getLocalName() {
		return null ;
	}

	/**
	 * The personal Nonce associated with the user on the other end of the
	 * connection.  Can be set at construction for closed slots,
	 * @return
	 */
	public synchronized Nonce getRemotePersonalNonce() {
		return this.personalNonce ;
	}
	
	/**
	 * The name associated with the OTHER side of this Connection.
	 * @return
	 */
	public synchronized String getRemoteName() {
		return this.name ;
	}
	
	@Override
	public synchronized void updateLocalName( String name ) {
		// do nothing
	}

	@Override
	protected synchronized void updateRemoteName( String name ) {
		this.name = name ;
	}
	
}
