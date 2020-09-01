package com.peaceray.quantro.communications.messagepassing;

import com.peaceray.quantro.communications.Message;

/**
 * The MessagePassingLayer acts as an array of MessagePassingConnections,
 * also providing a number of convenience methods for accessing and mutating
 * them.
 * 
 * A MessagePassingLayer is instantiated with a number of connections.
 * We require that all available slots be set before any method other than
 * setConnection is called.
 * 
 * @author Jake
 *
 */
public class MessagePassingLayer implements MessagePassingConnection.Delegate {
	
	/**
	 * The MessagePassingLayer acts as a "wrapper" to the delegate methods of
	 * its included Connections.  See MessagePassingConnection.Delegate for a more
	 * complete description of the delegate method-calling behavior of an MPC.
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
		public void mpld_messagePassingConnectionDidReceiveMessage( MessagePassingLayer layer, int connNum ) ;
		
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
		public void mpld_messagePassingConnectionDoneReceivingMessages( MessagePassingLayer layer, int connNum ) ;
		
		/**
		 * This method is called upon the connection entering CONNECTION status.
		 * 
		 * @param conn
		 */
		public void mpld_messagePassingConnectionDidConnect( MessagePassingLayer layer, int connNum ) ;
		
		/**
		 * This method is called when a connection attempt fails.
		 * 
		 * @param conn
		 */
		public void mpld_messagePassingConnectionDidFailToConnect( MessagePassingLayer layer, int connNum ) ;
		
		/**
		 * This method is called upon the connection breaking.
		 * 
		 * @param conn
		 */
		public void mpld_messagePassingConnectionDidBreak( MessagePassingLayer layer, int connNum ) ;
		
		/**
		 * This method is called upon the connection being disconnected by a peer.
		 * 
		 * @param conn
		 */
		public void mpld_messagePassingConnectionDidDisconnectByPeer( MessagePassingLayer layer, int connNum ) ;
	}
	

	MessagePassingConnection [] mpcs ;
	
	boolean allConnectionsSet ;
	
	// Delegate!
	MessagePassingLayer.Delegate delegate ;
	
	
	public MessagePassingLayer( int numConnections ) {
		mpcs = new MessagePassingConnection[numConnections] ;
		allConnectionsSet = false ;
		
		delegate = null ;
		
		for ( int i = 0; i < numConnections; i++ ) {
			mpcs[i] = null ;
		}
	}
	
	public synchronized void setDelegate( MessagePassingLayer.Delegate delegate ) {
		this.delegate = delegate ;
	}
	
	public synchronized int numConnections() {
		return mpcs.length ;
	}
	
	/**
	 * Sets the specified Connection.
	 * 
	 * Will throw IllegalStateException if the Connection has already been
	 * set.
	 * 
	 * @param index
	 * @param conn
	 */
	public synchronized void setConnection( int index, MessagePassingConnection conn ) {
		if ( mpcs[index] != null )
			throw new IllegalStateException("Connection " + index + " already set!") ;
		mpcs[index] = conn ;
		
		this.allConnectionsSet = true ;
		for ( int i = 0; i < mpcs.length; i++ )
			if ( mpcs[index] == null )
				allConnectionsSet = false ;
		
		mpcs[index].setDelegate(this) ;
	}
	
	/**
	 * Returns the Connection which was set for the specified index.
	 * Allows arbitrary and direct access to Connections.
	 * 
	 * @param index
	 * @return
	 */
	public synchronized MessagePassingConnection connection(int index) {
		return mpcs[index] ;
	}
	
	
	/**
	 * Upon return, all Connections will be in 'active' state, with
	 * status NEVER_CONNECTED.
	 * 
	 * This method will catch any exceptions that result from its calls,
	 * so the previous state of the Connections is not particularly relevant.
	 */
	public synchronized void activate() {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			MessagePassingConnection mpc = mpcs[i] ;
			
			if ( !mpc.isActive() )
				mpc.activate();
			else {
				if ( mpc.connectionStatus() != MessagePassingConnection.Status.NEVER_CONNECTED ) {
					try {
						mpc.disconnect() ;
					} catch( IllegalStateException e ) { }
					mpc.deactivate() ;
					mpc.activate() ;
				}
			}
		}
	}
		
		
	/**
	 * Upon return, all Connections will be in 'inactive' state.
	 * 
	 * This method will catch any exceptions that result from its calls.
	 */
	public synchronized void deactivate() {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			MessagePassingConnection mpc = mpcs[i] ;
			
			if ( mpc.isActive() ) {
				try {
					mpc.disconnect() ;
				} catch( IllegalStateException e ) { }
				mpc.deactivate() ;
			}
		}
	}
	
	
	//////////////////////////////////////////////////////////////////////
	//
	// Connecting, disconnecting, status checks, etc.
	//
	//////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Calls 'connect' on all active Connections, ignoring
	 * inactive ones.  If you want to connect completely call activate() first.
	 * 
	 * Does NOT verify that connections were successful; since we do not require
	 * that Connections complete their connection procedure before returning from
	 * connect(), and we do not want this method to block, we just call connect()
	 * and move on.
	 * 
	 * WITH INCONSISTENT STATUSES:
	 * 		This method attempts to put all Connections in Pending or Connected
	 * 		status.  For this reason, active connections already in one of those
	 * 		states are completely ignored (we do not disconnect or connect).  Any
	 *		other status will require a connect() (possibly after a disconnect()).
	 */
	public synchronized void connect() {
		connect( null ) ;
	}
	
	
	/**
	 * Calls 'connect' on all active Connections, ignoring
	 * inactive ones.  If you want to connect completely call activate() first.
	 * 
	 * Does NOT verify that connections were successful; since we do not require
	 * that Connections complete their connection procedure before returning from
	 * connect(), and we do not want this method to block, we just call connect()
	 * and move on.
	 * 
	 * WITH INCONSISTENT STATUSES:
	 * 		This method attempts to put all Connections in Pending or Connected
	 * 		status.  For this reason, active connections already in one of those
	 * 		states are completely ignored (we do not disconnect or connect).  Any
	 *		other status will require a connect() (possibly after a disconnect()).
	 *
	 *@param didChange When this method returns, didChange[0:numConnections()-1] will
	 *		be altered to reflect whether the state of the Connection changed as a result
	 *		of this call.
	 */
	public synchronized void connect( boolean [] didChange ) {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			MessagePassingConnection mpc = mpcs[i] ;
			if ( didChange != null )
				didChange[i] = false ;
			
			if ( mpc.isActive() ) {
				MessagePassingConnection.Status status = mpc.connectionStatus() ;
				if ( status == MessagePassingConnection.Status.BROKEN
						|| status == MessagePassingConnection.Status.FAILED
						|| status == MessagePassingConnection.Status.PEER_DISCONNECTED ) {
					mpc.disconnect() ;
					if ( didChange != null )
						didChange[i] = true ;
				}
				if ( status != MessagePassingConnection.Status.PENDING
						&& status != MessagePassingConnection.Status.CONNECTED ) {
					mpc.connect() ;
					if ( didChange != null )
						didChange[i] = true ;
				}
			}
		}
	}
	
	
	/**
	 * Calls 'disconnect' on all active Connections, ignoring inactive
	 * ones.
	 * 
	 * Attempts to leave the layer in a disconnected state.  Will call 'disconnect'
	 * on ALL active Connections EXCEPT those in a NEVER_CONNECTED or DISCONNECTED state.
	 */
	public synchronized void disconnect() {
		disconnect( null ) ;
	}
	
	/**
	 * Calls 'disconnect' on all active Connections, ignoring inactive
	 * ones.
	 * 
	 * Attempts to leave the layer in a disconnected state.  Will call 'disconnect'
	 * on ALL active Connections EXCEPT those in a NEVER_CONNECTED or DISCONNECTED state.
	 * 
	 * @param After this call, the entries of didChange will be altered to be 'true'
	 * 			if the Connection was altered as a result of this call.
	 */
	public synchronized void disconnect( boolean [] didChange ) {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			MessagePassingConnection mpc = mpcs[i] ;
			if ( didChange != null )
				didChange[i] = false ;
			
			if ( mpc.isActive() ) {
				MessagePassingConnection.Status status = mpc.connectionStatus() ;
				if ( status != MessagePassingConnection.Status.NEVER_CONNECTED
						&& status != MessagePassingConnection.Status.DISCONNECTED ) {
					mpc.disconnect() ;
					if ( didChange != null )
						didChange[i] = true ;
				}
			}
		}
	}
	
	
	
	/**
	 * Is this layer still "pending?"
	 * 
	 * A layer is considered "pending" if all active connections 
	 * are either in PENDING or CONNECTED status, AND at least one
	 * of them is PENDING.
	 * 
	 * @return Whether we are pending.
	 */
	public synchronized boolean isPending() {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		int numPending = 0 ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			MessagePassingConnection mpc = mpcs[i] ;
			
			if ( mpc.isActive() ) {
				MessagePassingConnection.Status status = mpc.connectionStatus() ;
				if ( status == MessagePassingConnection.Status.PENDING )
					numPending++ ;
				else if ( status != MessagePassingConnection.Status.CONNECTED )
					return false ;
			}
		}
		
		return numPending > 0 ;
	}
	
	/**
	 * Returns true if all active connections are Connected.
	 * @return
	 */
	public synchronized boolean isConnected() {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			MessagePassingConnection mpc = mpcs[i] ;
			
			if ( mpc.isActive() ) {
				MessagePassingConnection.Status status = mpc.connectionStatus() ;
				if ( status != MessagePassingConnection.Status.CONNECTED )
					return false ;
			}
		}
		
		return true ;
	}
	
	
	//////////////////////////////////////////////////////////////////////
	//
	// Sending and receiving messages.
	//
	//////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Broadcasts the specified message to ALL Connected connections.
	 * Returns 'true' if the broadcast was sent successfully to all Connected
	 * connections.
	 * 
	 * Returns 'false' if any of our active connections are not Connected,
	 * or if some Connected connection returned 'false' on the send.
	 */
	public synchronized boolean broadcast( Message m ) {
		return broadcastExcept( m, -1 ) ;
	}
	
	
	/**
	 * Broadcasts the specified message to ALL Connected connections, EXCEPT
	 * for the connection specified by the provided index.  If index is outside
	 * the bounds of our connections array, this is equivalent to broadcast(m).
	 * 
	 * Returns 'true' if the broadcast was sent successfully to all Connected
	 * connections (excluding the provided index).
	 * 
	 * Returns 'false' if any of our active connections are not Connected,
	 * or if some Connected connection returned 'false' on the send.
	 */
	public synchronized boolean broadcastExcept( Message m, int exemptConnection ) {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		boolean ok = true ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			MessagePassingConnection mpc = mpcs[i] ;
			
			if ( mpc.isActive() && i != exemptConnection ) {
				MessagePassingConnection.Status status = mpc.connectionStatus() ;
				if ( status != MessagePassingConnection.Status.CONNECTED ) {
					//System.out.println("MPL.broadcast: NOT sending to " + i) ;
					ok = false ;
				}
				else {
					//System.out.println("MPL.broadcast: sending to " + i) ;
					ok = mpc.sendMessage(m) && ok ;
				}
			}
		}
		
		return ok ;
	}
	
	
	/**
	 * Sends the provided message to the specified targets.
	 * 
	 * Throws an IllegalArgumentException if any inactive Connections
	 * are specified as targets.
	 * 
	 * Returns 'true' if all specified targets are connected, and each
	 * returned 'true' to the send attempt.  Otherwise, returns false.
	 * 
	 * In the case of an exception, the message was probably not sent
	 * to any targets - unless there was some strange multithreaded
	 * interaction where some other thread deactivated the Connection
	 * while we were working.
	 * 
	 * 
	 * @param m
	 * @param targets
	 * @return
	 */
	public synchronized boolean sendTo( Message m, boolean [] targets ) {
		if ( !allConnectionsSet )
			throw new IllegalStateException("Some connections not yet set!") ;
		
		for ( int i = 0; i < mpcs.length; i++ )
			if ( targets[i] && !mpcs[i].isActive() )
				throw new IllegalArgumentException("Inactive Connection specified as a target") ;
		
		boolean ok = true ;
		
		for ( int i = 0; i < mpcs.length; i++ ) {
			if ( targets[i] ) {
				MessagePassingConnection mpc = mpcs[i] ;
				MessagePassingConnection.Status status = mpc.connectionStatus() ;
				if ( status != MessagePassingConnection.Status.CONNECTED )
					ok = false ;
				else {
					ok = mpc.sendMessage(m) && ok ;
				}
			}
		}
		
		return ok ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MESSAGE PASSING CONNECTION DELEGATE METHODS
	//
	// We synchronize these methods to ensure they don't get called simultaneously
	// by multiple connections.
	//
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * The MPC received a message, which is immediately available.  hasMessage() and
	 * moreMessages() will return true, and getMessage() will return a Message object.
	 * 
	 * This method is called every time a message is received.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidReceiveMessage( MessagePassingConnection conn ) {
		if ( delegate != null ) {
			for ( int i = 0; i < mpcs.length; i++ ) {
				if ( mpcs[i] == conn ) {
					delegate.mpld_messagePassingConnectionDidReceiveMessage(this, i) ;
					break ;
				}
			}
		}
	}
	
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
	public void mpcd_messagePassingConnectionDoneReceivingMessages( MessagePassingConnection conn ) {
		if ( delegate != null ) {
			for ( int i = 0; i < mpcs.length; i++ ) {
				if ( mpcs[i] == conn ) {
					delegate.mpld_messagePassingConnectionDoneReceivingMessages(this, i) ;
					break ;
				}
			}
		}
	}
	
	/**
	 * This method is called upon the connection entering CONNECTION status.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidConnect( MessagePassingConnection conn ) {
		if ( delegate != null ) {
			for ( int i = 0; i < mpcs.length; i++ ) {
				if ( mpcs[i] == conn ) {
					delegate.mpld_messagePassingConnectionDidConnect(this, i) ;
					break ;
				}
			}
		}
	}
	
	/**
	 * This method is called when a connection attempt fails.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidFailToConnect( MessagePassingConnection conn ) {
		if ( delegate != null ) {
			for ( int i = 0; i < mpcs.length; i++ ) {
				if ( mpcs[i] == conn ) {
					delegate.mpld_messagePassingConnectionDidFailToConnect(this, i) ;
					break ;
				}
			}
		}
	}
	
	/**
	 * This method is called upon the connection breaking.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidBreak( MessagePassingConnection conn ) {
		if ( delegate != null ) {
			for ( int i = 0; i < mpcs.length; i++ ) {
				if ( mpcs[i] == conn ) {
					delegate.mpld_messagePassingConnectionDidBreak(this, i) ;
					break ;
				}
			}
		}
	}
	
	/**
	 * This method is called upon the connection being disconnected by a peer.
	 * 
	 * @param conn
	 */
	public void mpcd_messagePassingConnectionDidDisconnectByPeer( MessagePassingConnection conn ) {
		if ( delegate != null ) {
			for ( int i = 0; i < mpcs.length; i++ ) {
				if ( mpcs[i] == conn ) {
					delegate.mpld_messagePassingConnectionDidDisconnectByPeer(this, i) ;
					break ;
				}
			}
		}
	}
	
}
