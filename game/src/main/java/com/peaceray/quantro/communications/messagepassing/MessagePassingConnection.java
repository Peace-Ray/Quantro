package com.peaceray.quantro.communications.messagepassing;

import java.net.SocketAddress;

import com.peaceray.quantro.communications.Message;
import com.peaceray.quantro.communications.nonce.Nonce;


/**
 * A MessagePassingConnection represents a long-term connection between
 * Quantro users.  The Connection itself (at least, this interface) is unaware
 * of whether it is a client or server; it just passes messages, and reconnects
 * if the connection is lost.
 * 
 * MessagePassingConnections handles the nitty-gritty of connecting, reconnecting, 
 * wrapping sockets, etc.  In particular, it allows connections to go down and
 * reconnect without users of this class worrying about opening up listening
 * sockets, performing UDP hole-punching, etc.; these events are abstracted away.
 * HOWEVER, it should be noted that connecting, disconnecting, reconnecting, are
 * NOT non-events to users of this class.  MessagePassingConnection abstracts the 
 * TYPE of connection, but not the STATE of the connection.  If a connection goes
 * down, attempts to send / receive messages will fail.  The user must explicitly
 * reconnect and wait for the connection to succeed before continuing.
 * 
 * Why?
 * 
 * Because a connection that never failed, and a connection that failed and then
 * was reestablished, are NOT functionally identical.  For example, consider Game
 * connections.  Early bugs wherein games became un-synced after several reconnects
 * were resolved by explicitly sending the entire game state to ALL clients whenever
 * clients have reconnected.  Knowing when to send this update requires that we know
 * when a connection was lost and then reestablished.
 * 
 * This class represents the start of a refactoring for both Lobby and Game set-ups.
 * Previously, the serverside user implementation used a "GameCoordinator" or "Lobby"
 * object which directly interacted with Sockets (and then, later, WrappedSockets).
 * Apart from abstracting away the added complexity of using UDP hole-punched sockets
 * connected through the aid of a mediated socket - necessary for Internet MP - this
 * class should help simplify the various class that use these connections, 
 * eliminating copypasta code.
 * 
 * This class represents a single user-to-user connection (can also double as
 * a self-connection, from a serverside user to his clientside self).  To abstract
 * communication with multiple users, a further layer is helpful.
 * 
 * Any implementing subclass should take care to ensure that the MessagePassingConnection
 * instance is thread-synchronized.
 * 
 * @author Jake
 *
 */
public abstract class MessagePassingConnection {
	
	/**
	 * These methods are called by the MessagePassingConnection upon state
	 * updates.  These updates could come from multiple threads, so it is
	 * highly recommended that any implementation synchronize these methods.
	 * 
	 * Certain patterns of behavior could put the Connection in a state where
	 * it is not connected, but could still receive messages.  For example, if
	 * the underlying WrappedSocket closed but there is pending data which is not
	 * yet read, we may call messagePassingConnectionDidBreak() but then later
	 * make call(s) to messagePassingConnectionDidReceiveMessage.
	 * 
	 * To help resolve this, and to allow easier use of the delegate methods,
	 * the delegate method OutOfMessagesAndNotConnected is included.  This method
	 * will be called the first time after a call to connect() that the Connection
	 * is guaranteed to never receive another message.  This call will occur AFTER
	 * any calls to DidReceiveMessage and DidFailToConnect, DidBreak, DidDisconnectByPeer.
	 * 
	 * You may notice the absent delegate method - DidDisconnect.  Because a Disconnect will
	 * happen ONLY from outside - i.e., a user must call disconnect() on the Connection to
	 * cause this state change - I saw no need to include a Delegate call.
	 * 
	 * Note: if you call 'disconnect()' on a connection, all pending messages will be discarded.
	 * There will be no corresponding call to DidReceiveMessage().  This is the only way to
	 * "prevent" such a call (aside from the Connection remaining connected forever).
	 * 
	 * One final note: OutOfMessages will be called after we know that DidReceiveMessage
	 * 		will never be called, NOT when the message "queue" is empty.  If OutOfMessages
	 * 		is called, it is up to the delegate to call getMessage() as many times as are
	 * 		appropriate.
	 * 
	 * @author Jake
	 *
	 */
	public interface Delegate {
		
		/**
		 * The MPC received a message, which is immediately available.  hasMessage() and
		 * moreMessages() will return true, and getMessage() will return a Message object.
		 * 
		 * This method is called every time a message is received.
		 * 
		 * @param conn
		 */
		public void mpcd_messagePassingConnectionDidReceiveMessage( MessagePassingConnection conn ) ;
		
		/**
		 * This method is called the first time after a call to connect() that the connection status
		 * has entered the state FAILED_TO_CONNECT, BROKEN, or DISCONNECTED_BY_PEER -AND- it will never
		 * produce another message.
		 * 
		 * This method will not be called if disconnect() has been called on the Connection (or if the
		 * Connection never loses its connection).
		 * 
		 * @param conn
		 */
		public void mpcd_messagePassingConnectionDoneReceivingMessages( MessagePassingConnection conn ) ;
		
		/**
		 * This method is called upon the connection entering CONNECTION status.
		 * 
		 * @param conn
		 */
		public void mpcd_messagePassingConnectionDidConnect( MessagePassingConnection conn ) ;
		
		/**
		 * This method is called when a connection attempt fails.
		 * 
		 * @param conn
		 */
		public void mpcd_messagePassingConnectionDidFailToConnect( MessagePassingConnection conn ) ;
		
		/**
		 * This method is called upon the connection breaking.
		 * 
		 * @param conn
		 */
		public void mpcd_messagePassingConnectionDidBreak( MessagePassingConnection conn ) ;
		
		/**
		 * This method is called upon the connection being disconnected by a peer.
		 * 
		 * @param conn
		 */
		public void mpcd_messagePassingConnectionDidDisconnectByPeer( MessagePassingConnection conn ) ;
		
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// 
	// SETTING A DELEGATE
	//
	///////////////////////////////////////////////////////////////////////////
	
	
	public abstract void setDelegate( MessagePassingConnection.Delegate delegate ) ;
	
	
	///////////////////////////////////////////////////////////////////////////
	// 
	// CONNECTING AND RECONNECTING
	//
	// MessagePassingConnections have two basic considerations with regards to
	// connecting and disconnecting.  First: whether the Connection should be
	// considered "active": we want to send/receive messages on it, and our phone
	// is in a state where connecting is OK.
	//
	// An "active" Connection is not necessarily connected!
	// Basically, you should activate your Connection(s) when all of your structures
	// are allocated and your phone is ready to send/receive messages, and deactivate
	// when it's time to shut everything down and stop sending / receiving messages.
	//
	// Second: whether a Connection is "connected": if, at this moment, there is another
	// user (or at least another Connection) at the other end, and we can send/receive
	// messages at this moment. Only an Active connection can be connected.
	//
	// Why bother drawing this distinction?  Because connecting, disconnecting, reconnecting,
	// etc., may involve launching worker threads behind the scenes.  There may also be
	// more sockets than there are Connections - think of a server-side implementation, which
	// might keep a ServerSocket socking listening for incoming connections and .accept()
	// new Sockets from it.
	//
	// We don't want the users of this class to worry about theses threads and/or extra sockets,
	// but we DO want some way for them to close all those threads and/or sockets when it's time
	// to shut down.
	//
	// To mitigate these two opposing desires, we guarantee that a non-active (never activated,
	// or activated and then deactivated) Connection will NEVER be responsible for background
	// threads and/or background sockets.  If there are no active Connections, there will be
	// no background processing associated with them.  Therefore, if you merely want to
	// temporarily close or resestablish a connection (but keep the program going), it is
	// appropriate to use "disconnect", "reconnect", etc.  If, however, you are closing the
	// program done and want to stop all processing, you should "deactivate" your Conections
	// instead.
	//
	// A deactivated connection can be reactivated.  However, reactivating a Connection may
	// take more time and resources than reconnecting a closed (but still active) Connection.
	//
	// In general, only deactivate a connection when the thread(s) accessing the Connection are
	// going to be halted.
	//
	///////////////////////////////////////////////////////////////////////////
	
	//
	// ACTIVE/NON-ACTIVE: A Connection will never transition between Active and non-Active
	// 	for any reason other than activate() and deactivate() being explicitly called.
	//

	/**
	 * Activates this connection, which must not be currently active.
	 * 
	 * @throws IllegalStateException if the connection is already active.
	 */
	public abstract void activate() throws IllegalStateException ;
	
	/**
	 * Deactivates this connection.
	 * The Connection must be active, but not connected; if not active,
	 * or connected, throws an IllegalStateException.
	 * 
	 * @throws IllegalStateException if the connection was not active, or was connected.
	 */
	public abstract void deactivate() throws IllegalStateException ;
	
	
	/**
	 * Returns whether this connection is currently active.
	 * @return Whether this Connection is in an "active" state.
	 */
	public abstract boolean isActive() ;
	
	
	//
	// CONNECTED/NOT-CONNECTED: A Connection may transition from Connected to Not-Connected
	//	without disconnect() being called; for example the Connection may fail in an unanticipated
	//	way or the party on the other end may explicitly disconnect.  A Connection will never
	//	transition from Not-Connected to Connected without connect() being called.
	//
	
	/**
	 * MPC Status
	 */
	public enum Status {
		/**
		 * Not yet activated, or deactivated.
		 */
		INACTIVE,
		
		/**
		 * 'connect' was not called.
		 */
		NEVER_CONNECTED,
		
		/**
		 * 'connect' has been called and we are trying to connect.
		 */
		PENDING,
		
		/**
		 * Our attempt to connect after "connect" did not succeed;
		 * we never connected.
		 */
		FAILED,
		
		/**
		 * We are current connected, in a state where messages can be
		 * sent and received.
		 */
		CONNECTED,
		
		/**
		 * We have been explicitly disconnected by a user.
		 */
		DISCONNECTED,
		
		/**
		 * The user on the other end of the connection has disconnected
		 *  and nicely sent us a "disconnect" message.  How thoughtful!
		 */
		PEER_DISCONNECTED,
		
		/**
		 * This connection broke after it was connected.  We don't 
		 * know what went wrong.  Maybe we haven't received a message in a
		 * while (for UDP).  Maybe our peer close the TCP socket, but never nicely
		 * sent the "disconnect" message.  Who knows?
		 */
		BROKEN
	}
	
	/**
	 * Tells an 'Active' Connection that we wish to connect.  This call 
	 * will fail for a currently connected Connection, or for an inactive
	 * one.  Note that, if you determine that a Connection has disconnected
	 * for any reason, this call should be safe to make - it will not "reconnect"
	 * on its own.
	 * 
	 * @throws IllegalStateException if the Connection is already connected, or not active.
	 */
	public abstract void connect() throws IllegalStateException;
	
	
	/**
	 * Tells an 'Active' Connection to disconnect.  This call will fail (throwing an
	 * exception) for an inactive connection, one that has not connect()ed, or one
	 * which has already disconnect()ed.  Will NOT throw an exception if the Connection
	 * is connected, pending, broken, or disconnected by peer.  In other words, if YOU
	 * call connect(), YOU should expect to call disconnect() once.
	 * 
	 * @throws IllegalStateException Under the circumstances described.
	 */
	public abstract void disconnect() throws IllegalStateException;
	
	/**
	 * Queries the Connection for its current status, which is one of
	 * MessagePassingConnection.CONNECTION_STATUS_*.
	 * 
	 * @return The current connection status of this Connection
	 */
	public abstract Status connectionStatus() ;
	
	
	/**
	 * A convenience method for
	 * connectionStatus() == MessagePassingConnection.CONNECTION_STATUS_CONNECTED
	 * @throws IllegalStateException When the above statement would result in an
	 * exception.
	 */
	public synchronized boolean isConnected() throws IllegalStateException {
		return this.connectionStatus() == Status.CONNECTED ;
	}
	
	
	/**
	 * A convenience method for
	 * connectionStatus() == CONNECTION_STATUS_DISCONNECTED __or__ *_PEER_DISCONNECTED __or__ *_BROKEN.
	 * @return
	 * @throws IllegalStateException
	 */
	public synchronized boolean isDisconnected() throws IllegalStateException {
		Status status = this.connectionStatus() ;
		return status == Status.DISCONNECTED
			|| status == Status.PEER_DISCONNECTED
			|| status == Status.BROKEN ;
	}
	
	
	/**
	 * A convenience method for 
	 * int status = connectionStatus() ;
	 * status != CONNECTION_STATUS_NEVER_CONNECTED && status != CONNECTION_STATUS_DISCONNECTED
	 */
	public synchronized boolean isAbleToDisconnect() throws IllegalStateException {
		if ( !isActive() )
			return false ;
		Status status = this.connectionStatus() ;
		return status != Status.NEVER_CONNECTED
			&& status != Status.DISCONNECTED ;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// 
	// Message Passing!
	// 
	// Obviously, the whole point of this class is to pass messages between
	// peers.  These methods let you do that.  For writing messages, just
	// give the message to the MessagePassingConnection and it will be sent
	// on the connection, if it is open, returning whether the status allows
	// for sending messages (NOT whether the message successfully arrived, but
	// only whether it was successfully sent!)
	//
	// Reading messages is a bit different, because messages might build up in
	// a queue before they are read.  Emphasis on MIGHT, because this abstract class
	// implementation isn't interested in how things happen behind the scenes, and
	// you shouldn't be either.
	// Here's how to use it:
	//
	// mpc.connect()
	// ...stuff...
	// while mpc.isConnection()
	//		if mpc.hasMessage()
	//			m = mpc.getMessage()
	//			...process the message...
	//		...other stuff while we wait for a message...
	//
	// Some caveats: this implementation makes no assumptions regarding whether 
	// a new Message object is returned with every successful call to getMessage,
	// if the SAME instance is returned with different internal state, or some
	// hybrid of the two (potentially with an upper-limit to the number of instances).
	// Internally pre-cacheing instances would allow MPC to have fast communication performance
	// (calls to getMessage and hasMessage would probably be very fast), HOWEVER, it would give
	// poor overall performance (new Message instances would require substantial garbage collection
	// as the Connection is used).  On the other hand, cacheing no more than 1 message would slow
	// calls to hasMessage and getMessage, AND would introduce the disturbing possibility that 
	// m, the message instance returned by getMessage, will be AUTOMATICALLY ALTERED by the MPC
	// after it is returned - but with these costs comes the benefit of no GC overhead caused by
	// message reading.
	//
	// Since this abstract class does not assume either approach, subclasses and users should 
	// follow the following usage and implementation conventions:
	//
	// hasMessage(), if called repeatedly, should only ever change from 'false' to 'true' - never the
	//		reverse, until and unless getMessage() or disconnect() is called.
	// getMessage() should be called ONLY AFTER at least one call to hasMessage().  Once getMessage() is
	//		called, at least one call to hasMessage() must occur before getMessage() is called again.
	// m = getMessage(); 'm' should not be modified in-place by the caller.  If necessary, a COPY of 'm'
	//		should be made, and only that copy altered.
	// m = getMessage(); 'm' is guaranteed to NOT be altered by the MPC until AT LEAST the next call to
	//		hasMessage().  Until hasMessage() is called, in other words, 'm' may be freely accessed and
	//		its contents will not change.
	// getMessage(); never call this method without capturing its return value unless your intention is
	//		to discard the next message without examining it.  NEVER assume that 'm', the instance returned
	//		by getMessage(), will be the SAME instance returned any future call.  Likewise, NEVER assume that
	//		'm' is a new instance of a Message that has not been returned by an earlier call.
	// m1 = mpc1.getMessage(), m2 = mpc2.getMessage();  SEE BELOW
	// m1 = mpc1.getMessage(), mpc2.hasMessage(); 		SEE BELOW
	// mpc1.hasMessage(), m2 = mpc2.getMessage();		SEE BELOW
	//		The Message instances returned by different MPC instances
	//		will, themselves, ALWAYS be different.  There is no static cache of Messages shared between MPC instances.
	//		Therefore, you may freely call hasMessage() and getMessage() on DIFFERENT MPC instances and
	//		these calls will never interfere with each other.  The cavaets listed above are only w.r.t.
	//		sequential calls made to a single MPC instance.
	//
	// It is possible for ( mpc.isDisconnected() && mpc.hasMessage() ).  In this case, it indicates that
	// a message was received over the connection before it was disconnected, but it has not yet been
	// retrieved.  Users may continue to read messages, or ignore them as irrelevant, as they choose.
	//
	// Once .connect() is called on an mpc, leftover messages are discarded, and hasMessage() returning
	// true indicates that a message has been received AFTER reconnecting.
	
	
	/**
	 * Returns 'true' if a call to getMessage() will return a Message instance.
	 * Returns 'false' if no message is available.
	 * 
	 * See the comments above for specific interaction with getMessage().
	 * 
	 * @returns Whether a message is available for retrieval.
	 * @throws IllegalStateException if the Connection is inactive, or has not yet been connected.
	 */
	public abstract boolean hasMessage() throws IllegalStateException ;
	
	
	/**
	 * Returns 'true' there could possibly, at some point in the future, be more
	 * messages on this connection (before a call to connect()).
	 * 
	 * @return
	 * @throws IllegalStateException If the Connection is inactive.
	 */
	public abstract boolean moreMessages() throws IllegalStateException ;
	
	
	/**
	 * Returns a reference to a Message object containing the most recent message.
	 * Will throw an IllegalStateException if no message is available.
	 * 
	 * The message instance is safe to access until the next call to hasMessage(),
	 * but should NOT be modified, nor should it be accessed after the next call to hasMessage().
	 * 
	 * @return The latest message.
	 * @throws IllegalStateException If no message is available.
	 */
	public abstract Message getMessage() throws IllegalStateException ;
	
	/**
	 * Attempts to send the provided message to the connected peer.
	 * @return Whether it appears the message was successfully sent.  If
	 * 		the Connection is PEER_DISCONNECTED or BROKEN at the time this
	 * 		method is called, returns false.
	 * @throws IllegalStateException If this object has been put in a state where
	 * 		messages cannot be sent.  Will only return a result if it is active(),
	 * 		and either connected, or disconnected-by-peer or broken.  If the user
	 * 		has not called connect() and waited for a connection, or has called
	 * 		disconnect(), an exception will be thrown.
	 */
	public abstract boolean sendMessage( Message m ) throws IllegalStateException ;
	
	
	
	//
	// USER INFORMATION
	// Connections can have specific information - nonce, personal nonce, name,
	// that might (depending on context) refer to the user on this end of the connection,
	// or on the other end.  Because 'nonces' are used to refer to specific lobby or
	// game instances as a whole, there is no specific distinction between "our nonce"
	// and "other user nonce."  However, personalNonces and names are associated with
	// one side or the other of the connection, depending on whether they are "ours"
	// (for e.g. client connections) or "another user's" (for e.g. server connections).
	//
	
	
	/**
	 * The Nonce associated with this connection.
	 * @return Our nonce.
	 */
	public abstract Nonce getNonce() ;
	
	/**
	 * The personal Nonce associated with our side of this Connection.
	 * @return Our personal nonce
	 */
	public abstract Nonce getLocalPersonalNonce() ;
	
	/**
	 * The name associated with our side of this connection.
	 * @return Our name.
	 */
	public abstract String getLocalName() ;

	/**
	 * The personal Nonce associated with the OTHER side of this Connection.
	 * @return
	 */
	public abstract Nonce getRemotePersonalNonce() ;
	
	/**
	 * The name associated with the OTHER side of this Connection.
	 * @return
	 */
	public abstract String getRemoteName() ;
	
	
	/**
	 * Returns the socket address associated with the other end of this
	 * connection -- if it exists -- or 'null.'
	 * 
	 * Subclasses should feel free to return 'null' in cases where there
	 * is no obvious meaning to a remote socket address (such as a local
	 * pipe-based connection), or when the remote socket address is not
	 * known (such as when the connection is indirect).
	 * 
	 * @return
	 */
	public abstract SocketAddress getRemoteSocketAddress() ;
	
}
