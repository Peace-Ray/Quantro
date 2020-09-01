package com.peaceray.quantro.communications;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.communications.wrapper.AutonomousWrappedSocketFactory;
import com.peaceray.quantro.utils.ThreadSafety;

/**
 * The ClientListener class keeps an open socket within a thread,
 * and waits for incoming connections.  Should a client attempt to
 * connect - and should it provide the correct nonce - the ClientListener
 * will ask for personalNonce and name, and pass the socket to the GameCoordinator.
 * 
 * @author Jake
 *
 */
public class ClientListenerUnmediated extends ClientListener {

	class ClientListenerUnmediatedThread extends Thread {
		
		public static final String TAG = "ClientListenerThread" ;
		
		ClientListener listener ;
		private int port ;
		
		boolean listening ;
		
		ServerSocket ss ;
		
		public ClientListenerUnmediatedThread( ClientListener listener, int port ) {
			this.listener = listener ;
			this.port = port ;
			
			listening = true ;
			
			ss = null ;
		}
		
		
		@Override
		public void run() {
			System.out.println("ClientListener starting up with port " + port) ;
			ServerSocket ss = null ;
			try {
				ss = new ServerSocket(port, 2, null) ;
				ss.setSoTimeout(100) ;		// accept timeout set to 0.1 seconds
			} catch( Exception e ){
				// Something failed, don't know why.
				// listening is currently set to 'false'
				// so don't worry about it.
				System.out.println("ClientListener failed to open server socket." ) ;
				e.printStackTrace() ;
				listening = false ;
			}
			
			
			
			Socket cs ;
			
			while(listening) {
				// Incoming!
				
				try {
					cs = ss.accept() ;
					WrappedSocket ws = AutonomousWrappedSocketFactory.wrap(cs) ;
					// Pass this socket to a client interrogation thread.  If
					// it checks out, the socket will be passed to the gameCoordinator.
					if ( cs != null )
						new ClientListenerClientInterrogationThread( listener, ws ).start();
					//System.out.println("ClientListener: client connection opened; interrogation thread launched") ;
				} catch( SocketTimeoutException e ) {
					// This is the good kind.  We don't do anything.
					//Log.d(TAG, "Client listener socket timeout") ;
					continue ;
				} catch( IOException e ) {
					// Wha..?
					System.out.println("ClientListener: " + e.getStackTrace() ) ;
				}
			}
			
			try {
				ss.close();
			} catch (Exception e) {
			}
		}
		
	}
	
	
	
	
	private int port ;
	private ClientListenerUnmediatedThread thread ;
	
	
	
	/**
	 * Starts a new client listener for the provided class implementing AcceptsClientSockets.
	 * Will collect nonce, personalNonce, and name from all those who attempt to connect.
	 * 
	 * If 'nonce' is non-zero in this constructor, we will verify the client's provided
	 * nonce before passing the socket on, closing the connection instead if not.
	 * 
	 * Wh
	 * 
	 * @param minInterrogationTime Regardless of how quickly information comes in, we take
	 * 			at least this much time (milliseconds) holding the client socket before passing it on.
	 * @param maxInterrogationTime We don't allow clients any longer than this (milliseconds)
	 * 			to provide all the information we request.
	 * @param port
	 * @param nonce
	 * @param acceptsClientSockets
	 */
	public ClientListenerUnmediated( long minInterrogationTime, long maxInterrogationTime,
			int port ) {
		
		super( minInterrogationTime, maxInterrogationTime ) ;
		
		this.port = port ;
	}
	
	/**
	 * Start listening for clients for the specified AcceptsClientSockets
	 * using the specified nonce.  Using only the nonce to identify the game
	 * restricts how the listener can function.  For example, using a listener
	 * which accepts incoming sockets, then it must be the case that either
	 * 1. Lobbies and their Games use different nonces, or 
	 * 2. Lobbies and their Games use different ports and different listener object.
	 * 
	 * @param nonce If the client knows the nonce, he will be passed to the ACS instance.
	 * 			If Nonce.ZERO is provided, players will be asked for a nonce but it will
	 * 			not be checked.
	 * @param acs An object which AcceptsClientSockets.  We will pass sockets to this
	 * 			object along with ClientInformation.
	 * @return Whether this object has begun listening for clients using the specified nonce.
	 * 			Will return 'false' in cases such as already listening (for 1 ACS implementations),
	 * 			nonce already in use (in which case we did not "start" listening), etc.
	 */
	public boolean startListening(Nonce nonce, Nonce personalNonce, AcceptsClientSockets acs) {
		if ( personalNonce == null ) {
			if ( this.hasTarget( nonce ) )
				return false ;
			this.addNonceToTargets(nonce, acs) ;
		}
		else {
			if ( this.hasSpecializedTarget( nonce, personalNonce ) )
				return false ;
			this.addSpecializedTarget(nonce, personalNonce, acs) ;
		}
		
		if ( thread == null ) {
			thread = new ClientListenerUnmediatedThread( this, port ) ;
			thread.start() ;
		}
		
		return true ;
	}
	
	
	
	/**
	 * Are we currently listening for the specified nonce?  This is a way to query the
	 * status of a listener.
	 * 
	 * @param nonce Which nonce we are concerned with.
	 * @return
	 */
	public boolean isListening(Nonce nonce, Nonce personalNonce) {
		if ( personalNonce == null )
			return this.hasTarget( nonce ) && thread != null ;
		else
			return this.hasSpecializedTarget( nonce, personalNonce ) && thread != null ;
	}
	
	/**
	 * Stops listening for the specified nonce.  If this nonce has a dedicated thread
	 * and/or socket(s), close them up.
	 * 
	 * POSTCONDITION: We are no longer listening for the specified nonce / acs combination.
	 * 					The require memory footprint has been alleviated, or will be shortly,
	 * 					without further intervention.
	 * 
	 * @param nonce
	 * @return Did we actually STOP listening by this call?  (i.e., if the listener
	 * 			had previously broken, we return 'false').
	 */
	public boolean stopListening(Nonce nonce, Nonce personalNonce) {
		if ( personalNonce == null ) {
			if ( !this.hasTarget( nonce ) )
				return false ;
			this.removeNonceFromTargets(nonce) ;
		}
		else {
			if ( !this.hasSpecializedTarget( nonce, personalNonce ) )
				return false ;
			this.removeFromSpecializedTargets(nonce, personalNonce) ;
		}
		
		
		if ( this.numberOfTargets() == 0 && this.numberOfSpecializedTargets() == 0 ) {
			thread.listening = false ;
			if ( thread.ss != null ) {
				try{
					thread.ss.close() ;
				} catch( Exception e ) {
				}
			}
			ThreadSafety.waitForThreadToTerminate(thread) ;
			thread = null ;
		}
		return true ;
	}
	
	
	
	/**
	 * Equivalent to iterating through all current 'nonces' which we
	 * are listening to, stopping each one.
	 * 
	 * POSTCONDITION: We are no longer listening for any incoming connections at all.
	 * 
	 * @return
	 */
	public void stopListening() {
		if ( thread != null ) {
			thread.listening = false ;
			if ( thread.ss != null ) {
				try{
					thread.ss.close() ;
				} catch( Exception e ) {
				}
			}
			ThreadSafety.waitForThreadToTerminate(thread) ;
			
			thread = null ;
			removeAllTargets() ;
			removeAllSpecializedTargets() ;
		}
	}
}
