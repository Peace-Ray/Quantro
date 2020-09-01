package com.peaceray.quantro.communications.messagepassing;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Hashtable;


import com.peaceray.quantro.communications.ClientInformation;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.communications.wrapper.AutonomousWrappedSocketFactory;

/**
 * A class whose static methods allow us to listen for
 * direct clients on arbitrary ports.
 * 
 * 
 * 
 * @author Jake
 *
 */
class DirectClientListener {

	
	// We instantiate, start, and stop DirectClientPortListenerThreads as needed.
	// When one is running, we place it in the HashTable (keyed by Integer port
	// number).
	//
	// We synchronize access in two steps.  First we lock the hash table itself,
	// to make sure no other threads perform concurrent edits.  Second we check
	// for an entry and, if found, we lock on it before checking its number of
	// connections and whether it is currently running.
	private static Hashtable<Integer, DirectClientPortListenerThread> portListenerThreads
		= new Hashtable<Integer, DirectClientPortListenerThread>() ;
	
	private static DirectClientListener myInstance = new DirectClientListener() ;
	
	/**
	 * Tell the DirectClientListener that this Connection, which implements
	 * the MessagePassingConnectionAcceptsClients interface, is currently
	 * connect()ing and is interested in receiving connections on the
	 * specified port.
	 * 
	 * @param port
	 * @param mpc
	 */
	public static void listenOnPort( int port, MessagePassingConnectionAcceptsClients mpc ) {
		synchronized( portListenerThreads ) {
			// First step: get and synchronize on a portListenerThread which is running
			// - or ready to start.
			Integer key = new Integer(port) ;
			DirectClientPortListenerThread t = null ;
			if ( portListenerThreads.containsKey(key) )
				t = portListenerThreads.get(key) ;
			
			// We have a thread to test.  Synchronize on it; if it's running,
			// give it the thread.  Otherwise we'll need to make a new thread.
			if ( t != null ) {
				synchronized( t ) {
					if ( t.running ) {
						// it is running, so add our mpc.
						t.addAcceptor(mpc) ;
						return ;
					}
				}
			}
			
			// If we reach this point, we either couldn't find a thread,
			// or we found it but it's already finished.  Either way,
			// we make a new one, add our acceptor, and start it going.
			t = myInstance.new DirectClientPortListenerThread( port ) ;
			portListenerThreads.put(key, t) ;
			t.addAcceptor(mpc) ;
			t.start() ;
		}
	}

	/**
	 * Tells the DirectClientListener that this Connection, which implements
	 * the MessagePassingConnectionAcceptsClients interface, is no longer
	 * connect()ing and is therefore no longer interested in receiving
	 * connections on the specified port.
	 * 
	 * Calling this method for an mpc which is NOT currently listening on the
	 * provided port has no effect.
	 * 
	 * @param port
	 * @param mpc
	 */
	public static void stopListeningOnPort( int port, MessagePassingConnectionAcceptsClients mpc ) {
		synchronized( portListenerThreads ) {
			// Get it, synchronize it, remove it.
			Integer key = new Integer(port) ;
			DirectClientPortListenerThread t = null ;
			if ( portListenerThreads.containsKey(key) )
				t = portListenerThreads.get(key) ;
			
			if ( t != null ) {
				synchronized( t ) {
					t.removeAcceptor(mpc) ;
				}
			}
		}
	}
	
	
	/**
	 * A thread for listening on the provided port.
	 * @author Jake
	 *
	 */
	class DirectClientPortListenerThread extends Thread implements ClientInterrogatorThread.ClientInterrogatorThreadLauncher {

		private int port ;
		private boolean running ;
		
		ArrayList<MessagePassingConnectionAcceptsClients> acceptors ;
		
		DirectClientPortListenerThread( int port ) {
			this.port = port ;
			this.running = true ;
			
			this.acceptors = new ArrayList<MessagePassingConnectionAcceptsClients>() ;
		}
		
		@Override
		public void run() {
			// While running, we listen on a ServerSocket (if possible!).
			// We synchronize any checks for whether to keep running, including
			// binding on the ServerSocket.
			
			ServerSocket ss = null ;
			if ( this.running ) {
				// Start up a listening socket.
				synchronized(this) {
					try {
						ss = new ServerSocket(port, 2, null) ;
						ss.setSoTimeout(100) ;		// accept timeout set to 0.1 seconds
					} catch( Exception e ) {
						this.running = false ;
						try {
							ss.close() ;
						} catch( Exception e2 ) { }
					}
				}
			}
			
			while( this.running ) {
				// Listen.
				Socket in_sock ;
				try {
					in_sock = ss.accept() ;
					WrappedSocket ws = AutonomousWrappedSocketFactory.wrap(in_sock) ;
					
					// Launch an interrogator thread.
					ClientInterrogatorThread t = new ClientInterrogatorThread(this, ws, 100, 10000) ;
					t.start() ;
					
				} catch( SocketTimeoutException e ) {
					// This is the good kind.  We don't do anything, other than
					// check our current acceptors - if we have none, it's time to stop.
				} catch( IOException e ) {
					// Hmm.  Socket error?
					synchronized( this ) {
						this.running = false ;
						// close immediately here so another thread can open a socket.
						try {
							ss.close() ;
						} catch( Exception e2 ) { } 
					}
				}
				
				// If we're still running, check whether we have any acceptors available... if 
				// not, it's time to stop.
				synchronized(this) {
					if ( this.acceptors.size() == 0 ) {
						this.running = false ;
						try {
							ss.close();
						} catch( Exception e ) { }
					}
				}
			}
			
			// Try closing our server socket.
			try {
				ss.close() ;
			} catch( Exception e ) { }
			
			// If we reach this state, we should clean up after ourselves.
			// Any acceptors still on our list should be told we failed.
			synchronized(this) {
				for ( int i = 0; i < this.acceptors.size(); i++ ) {
					MessagePassingConnectionAcceptsClients mpc = this.acceptors.get(i) ;
					mpc.failedToFindClient(this) ;
				}
				this.acceptors.clear() ;
			}
		}
		
		
		boolean addAcceptor( MessagePassingConnectionAcceptsClients mpc ) {
			synchronized( this ) {
				if (!running)
					return false ;
				this.acceptors.add(mpc) ;
				return true ;
			}
		}
		
		boolean removeAcceptor( MessagePassingConnectionAcceptsClients mpc ) {
			synchronized( this ) {
				return this.acceptors.remove(mpc) ;
			}
		}
		
		@Override
		/**
		 * Called when an interrogator thread finishes its work to pass the
		 * wrapped socket back with its client information.
		 */
		public boolean takeClientSocket(WrappedSocket wsock, ClientInformation ci) {
			
			// Synchronize on our acceptors list.
			synchronized( this ) {
				
				//System.err.println("DirectClientListener: taking socket.  Running:" + this.running) ;
				//System.err.println("Client information: " + ci.getName() + " " + ci.getNonce() + " " + ci.getPersonalNonce()) ;
				
				if ( !this.running )
					return false ;
				
				// See if we have an acceptor for this client.  We search the
				// list using a formal priority order:
				// requireNonce:true openSlot:false nonce=nonce, personalNonce=personalNonce
				// requireNonce:false openSlot:false nonce=? personalNonce=personalNonce
				// requireNonce:true openSlot:false nonce=nonce personalNonce:? <-- a "closed slot" with no personalNonce yet assigned
				// requireNonce:false openSlot:false nonce=? personalNonce:?
				// requireNonce:true openSlot:true nonce=nonce
				// requireNonce:false openSlot:true nonce=?
				
				int entry = -1 ;
				// A series of checks; keep calling the method with parameters in
				// priority-order until we find the entry.
				entry = entry >= 0 ? entry : findAcceptor( this.acceptors, true, true, true, ci ) ;
				entry = entry >= 0 ? entry : findAcceptor( this.acceptors, false, true, true, ci ) ;
				entry = entry >= 0 ? entry : findAcceptor( this.acceptors, true, false, true, ci ) ;
				entry = entry >= 0 ? entry : findAcceptor( this.acceptors, false, false, true, ci ) ;
				entry = entry >= 0 ? entry : findAcceptor( this.acceptors, true, false, false, ci ) ;
				entry = entry >= 0 ? entry : findAcceptor( this.acceptors, false, false, false, ci ) ;
				
				//System.err.println("DirectClientListener: taking socket.  Found entry:" + entry) ;
				
				// Whelp, if we found it, give it up and remove it from our accessors.
				if ( entry >= 0 ) {
					MessagePassingConnectionAcceptsClients mpc = this.acceptors.get(entry) ;
					// try adding it.
					try {
						if ( !mpc.acceptClient(this, wsock, ci) ) {
							entry = -1 ;
						}
					} catch ( IllegalStateException e ) {
						entry = -1 ;
					}
				}
				
				//System.err.println("DirectClientListener: taking socket.  Accepted client:" + (entry != -1)) ;
				
				// If entry is -1, we did NOT place the connection.
				if ( entry != -1 ) {
					this.acceptors.remove(entry) ;
					return true ;
				}
				
				//System.err.println("DirectClientListener: taking socket.  did not accept.") ;
			}
			
			// whatevs
			return false ;
			
		}
		
		/**
		 * Linear search in the provided ArrayList for an acceptor that fits the
		 * provided criteria.  Returns the index of the first entry found to meet
		 * the criteria, or -1 if no such entry was found.
		 * 
		 * requireNonce: if True, entry.requiresNonce() and the nonce itself
		 * 		matches that in the client information.
		 * 
		 * requirePersonalNonce: if True, entry.requiresPersonalNonce() and the
		 * 		personal nonce itself matches that in the client information.
		 * 
		 * remembersPersonalNonce: if True, entry.remembersPersonalNonce().
		 * 
		 * @param acceptors
		 * @param requireNonce
		 * @param requirePersonalNonce
		 * @param rememberNonce
		 * @return
		 */
		private int findAcceptor( ArrayList<MessagePassingConnectionAcceptsClients> acceptors,
				boolean requireNonce, boolean requirePersonalNonce, boolean rememberNonce,
				ClientInformation cinfo ) {
			
			//System.out.println("finding Acceptor out of " + acceptors.size()) ;
			//System.out.println("for client with nonce " + cinfo.getNonce() + " " + " ponce " + cinfo.getPersonalNonce() + " name " + cinfo.getName()) ;
			
			for ( int i = 0; i < acceptors.size(); i++ ) {
				MessagePassingConnectionAcceptsClients acceptor = acceptors.get(i) ;
				
				//System.out.println("acceptor " + i + " requiresNonce:" + acceptor.requiresNonce() + " nonce is " + acceptor.getNonce()) ;
				//System.out.println("           requiresPNonce:" + acceptor.requiresPersonalNonce() + " pnonce is " + acceptor.getRemotePersonalNonce()) ;
				//System.out.println("           remembersPNonce:" + acceptor.remembersPersonalNonce()) ;
				
				
				boolean match = true ;
				
				// Check nonce status.  Either we don't require the nonce, or
				// the nonce must match.
				if ( requireNonce != acceptor.requiresNonce() )
					match = false ;
				else {
					if ( requireNonce && !cinfo.getNonce().equals(acceptor.getNonce()) )
						match = false ;
				}
				
				
				// Check personal nonce.  Either we don't require the personal
				// nonce, or the personal nonce matches.
				if ( requirePersonalNonce != acceptor.requiresPersonalNonce() )
					match = false ;
				else {
					if ( requirePersonalNonce && !cinfo.getPersonalNonce().equals(acceptor.getRemotePersonalNonce()) )
						match = false ;
				}
				
				
				// Check remember nonce.  we just require that it matches; we don't care
				// whether the nonce is actually teh same.
				if ( rememberNonce != acceptor.remembersPersonalNonce() )
					match = false ;
				
				
				// If a match, go ahead and return it.
				if ( match )
					return i ;
			}
			
			return -1 ;
		}
		
		
		
	}
	
}
