package com.peaceray.quantro.communications;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.channels.WritableByteChannel;
import java.util.Enumeration;
import java.util.Hashtable;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;
import com.peaceray.quantro.model.communications.GameMessage;

/**
 * A client listener in some way accepts incoming connections.
 * These connections are passed as 'socket' objects to whatever
 * "AcceptsClientSockets" object is appropriate.
 * 
 * Some listeners, such as ClientListenerUnmediated, simply accepts
 * incoming sockets.  This listener is used for WiFi games.
 * 
 * Others, such as ClientListenerMediated, maintains an outgoing connection
 * to a public "mediator" server.  Instructions from the mediator dictate
 * how other connections are made - by TCP hole punching, by pass-through
 * sockets, etc.  Both have the same basic interface, although mediated client
 * listeners obviously require additional information.
 * 
 * @author Jake
 *
 */
public abstract class ClientListener {

	
	protected long minInterrogationTime ;
	protected long maxInterrogationTime ;
	
	
	protected Hashtable<Nonce, AcceptsClientSockets> targets ;
	protected Hashtable<String, AcceptsClientSockets> specializedTargets ;
	
	
	public ClientListener(long minTime, long maxTime) {
		targets = new Hashtable<Nonce,AcceptsClientSockets>() ;
		specializedTargets = new Hashtable<String,AcceptsClientSockets>() ;
		setInterrogationTime( minTime, maxTime ) ;
	}
	
	// GETTERS/SETTERS
	
	private void setInterrogationTime( long minTime, long maxTime ) {
		this.minInterrogationTime = minTime ;
		this.maxInterrogationTime = maxTime ;
	}
	
	// LISTENING METHODS
	
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
	public abstract boolean startListening(Nonce nonce, Nonce personalNonce, AcceptsClientSockets acs) ;
	
	/**
	 * Are we currently listening for the specified nonce?  This is a way to query the
	 * status of a listener.
	 * 
	 * @param nonce Which nonce we are concerned with.
	 * @return
	 */
	public abstract boolean isListening(Nonce nonce, Nonce personalNonce) ;
	
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
	public abstract boolean stopListening(Nonce nonce, Nonce personalNonce) ;
	
	
	
	/**
	 * Equivalent to iterating through all current 'nonces' which we
	 * are listening to, stopping each one.
	 * 
	 * POSTCONDITION: We are no longer listening for any incoming connections at all.
	 * 
	 * @return
	 */
	public abstract void stopListening() ;
	
	/**
	 * Adds the provided nonce / acs combination to our list of targets.
	 * Returns success (i.e., if we weren't currently using the Nonce).
	 * 
	 * @param nonce
	 * @param acs
	 * @return
	 */
	protected boolean addNonceToTargets( Nonce nonce, AcceptsClientSockets acs ) {
		for ( Enumeration<Nonce> e = targets.keys(); e.hasMoreElements(); ) {
			if ( e.nextElement().equals(nonce) )
				return false ;
		}
		
		targets.put(nonce, acs) ;
		return true ;
	}
	
	
	protected boolean addSpecializedTarget( Nonce n1, Nonce n2, AcceptsClientSockets acs ) {
		String string = combineNoncesIntoString(n1, n2) ;
		for ( Enumeration<String> e = specializedTargets.keys(); e.hasMoreElements(); ) {
			if ( e.nextElement().equals(string) )
				return false ;
		}
		
		specializedTargets.put(string, acs) ;
		return true ;
	}
	
	
	/**
	 * Removes the provided nonce / acs combination to our list of targets.
	 * Returns success (i.e., if the target was there and is now removed).
	 * 
	 * @param nonce
	 * @param acs
	 * @return
	 */
	protected boolean removeNonceFromTargets( Nonce nonce ) {
		for ( Enumeration<Nonce> e = targets.keys(); e.hasMoreElements(); ) {
			Nonce key = e.nextElement() ;
			if ( key.equals(nonce) ) {
				targets.remove(key) ;
				return true ;
			}
		}
		
		return false ;
	}
	
	
	protected boolean removeFromSpecializedTargets( Nonce n1, Nonce n2 ) {
		String string = combineNoncesIntoString(n1, n2) ;
		for ( Enumeration<String> e = specializedTargets.keys(); e.hasMoreElements(); ) {
			String key = e.nextElement() ;
			if ( key.equals(string) ) {
				specializedTargets.remove(key) ;
				return true ;
			}
		}
		
		return false ;
	}
	
	/**
	 * Returns whether the specified nonce is present in the list of targets.
	 * 
	 * @param nonce
	 * @param acs
	 * @return
	 */
	protected boolean hasTarget( Nonce nonce ) {
		return targets.get(nonce) != null ;
	}
	
	
	protected boolean hasSpecializedTarget( Nonce n1, Nonce n2 ) {
		String string = combineNoncesIntoString(n1, n2) ;
		return specializedTargets.get(string) != null ;
	}
	
	
	/**
	 * Removes all nonces and targets.
	 */
	protected void removeAllTargets() {
		for ( Enumeration<Nonce> e = targets.keys(); e.hasMoreElements(); ) {
			targets.remove(e.nextElement()) ;
		}
	}
	
	protected void removeAllSpecializedTargets() {
		for ( Enumeration<String> e = specializedTargets.keys(); e.hasMoreElements(); ) {
			specializedTargets.remove(e.nextElement()) ;
		}
	}
	
	
	
	protected int numberOfTargets() {
		return targets.size();
	}
	
	protected int numberOfSpecializedTargets() {
		return specializedTargets.size();
	}
	
	private String combineNoncesIntoString( Nonce n1, Nonce n2 ) {
		return n1.toString() + ":" + n2.toString() ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////////////
	//
	// Interrogates a socket for nonce, personalNonce and name.  Passes it to the
	// appropriate AcceptsClientSockets object.  We don't particularly care how the connection was formed.
	//
	////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * A class for interrogating a 
	 * @author Jake
	 *
	 */
	public class ClientListenerClientInterrogationThread extends Thread {
		
		//private static final String TAG = "ClientListenerClientInterrogationThread" ;
		
		WeakReference<ClientListener> listener ;
		WrappedSocket socket ;
		long startTime ;
		
		public ClientListenerClientInterrogationThread (ClientListener listener, WrappedSocket socket ){
			this.listener = new WeakReference<ClientListener>(listener) ;
			this.socket = socket ;
		}
		
		@Override
		public void run() {
			startTime = System.currentTimeMillis() ;
			
			// Attempt to get a straight answer out of this socket.
			MessageReader mr ;
			WritableByteChannel wbc ;
			CoordinationMessage out_m ;
			CoordinationMessage in_m ;
			
			boolean failed = false ;
			ClientInformation ci = new ClientInformation() ;
			
			try {
				mr = new MessageReader( new CoordinationMessage(), socket.getSourceChannel() ) ;
				wbc = socket.getSinkChannel() ;
				out_m = new CoordinationMessage() ;
				
				// Ask for a nonce.
				if ( !failed ) {
					out_m.setAsNonceRequest() ;
					System.out.println("requesting nonce") ;
					in_m = this.sendAndGetResponse(mr, wbc, out_m) ;
					System.out.println("response received") ;
					
					if ( in_m == null || in_m.getType() != GameMessage.TYPE_NONCE )
						failed = true ;
					else {
						ci.setNonce( in_m.getNonce() ) ;
						System.out.println("ClientListener.ClientListenerInterrogationThread Nonce received as " + ci.getNonce()) ;
					}
				}
				
				// Ask for name
				if ( !failed ) {
					out_m.setAsNameRequest(0) ;
					in_m = this.sendAndGetResponse(mr, wbc, out_m) ;
					
					if ( in_m == null || in_m.getType() != GameMessage.TYPE_MY_NAME )
						failed = true ;
					else
						ci.setName( in_m.getName() ) ;
				}
				
				// Ask for personal nonce
				if ( !failed ) {
					out_m.setAsPersonalNonceRequest() ;
					in_m = this.sendAndGetResponse(mr, wbc, out_m) ;
					
					if ( in_m == null || in_m.getType() != GameMessage.TYPE_PERSONAL_NONCE )
						failed = true ;
					else {
						System.out.println("ClientListener.ClientListenerInterrogationThread: setting personal nonce as " + in_m.getNonce()) ;
						ci.setPersonalNonce( in_m.getNonce() ) ;
					}
				}
				
				mr.stop();
				
				// Busy wait until min time
				long curTime = System.currentTimeMillis() ;
				long millisLeft = minInterrogationTime - (curTime - startTime) ;
				if ( millisLeft > 0) {
					try {
						Thread.sleep(millisLeft) ;
					} catch( Exception e ) {
						e.printStackTrace() ;
						failed = true ;
					}
				}
				
				
				// First look for an exact match.  If none, look
				// for 'zero'.
				AcceptsClientSockets acs ;
				ClientListener cl = listener.get() ;
				if ( !failed && cl != null ) {
					acs = cl.specializedTargets.get( combineNoncesIntoString( ci.getNonce(), ci.getPersonalNonce() ) ) ;
					if ( acs == null )
						acs = cl.targets.get(ci.getNonce()) ;
					if ( acs == null )
						acs = cl.targets.get(Nonce.ZERO) ;
					
					System.out.println("ClientListener: acs is " + acs) ;
					System.out.println("nonce provided is " + ci.getNonce() ) ;
					for ( Enumeration<Nonce> e = cl.targets.keys() ; e.hasMoreElements() ; ) {
						System.out.println("available nonce: " + e.nextElement()) ;
					}
					
					if ( acs != null ) {
						if ( acs.setClientSocket(socket, ci) >= 0 )
							return ;
						else {
							System.err.println("failed to accept socket.") ;
							failed = true ;
						}
					}
					else {
						try {
							out_m.setAsKick(0, "No accepter found for nonce").write(wbc) ;
						} catch( IOException exception ) {
							// nothing
						} finally {
							failed = true ;
						}
					}
				}

				if ( failed || cl == null ) {
					wbc.close() ;
					this.socket.close() ;
				}
			} catch ( Exception e ) {
				e.printStackTrace() ;
				return ;
			}
		}
		
		public CoordinationMessage sendAndGetResponse( MessageReader mr, WritableByteChannel wbc, CoordinationMessage outgoingMessage ) {
			try {
				outgoingMessage.write(wbc) ;
				mr.okToReadNextMessage() ;
				
				// Wait for the response.
				while( !mr.messageReady() && System.currentTimeMillis() - startTime < maxInterrogationTime ) {
					// sleep
					//Log.d(TAG, "waiting for response.  mr status is " + mr.status()) ;
					Thread.sleep(50) ;
				}
				if ( !mr.messageReady() ) {
					//System.out.println("CientListener:Message not ready after " + maxInterrogationTime + ".  Status is " + mr.status()  ) ;
					wbc.close() ;
					mr.stop() ;
					socket.close() ;
					return null ;
				}
				
				return (CoordinationMessage) mr.getMessage() ;
			} catch ( Exception e ) {
				return null ;
			}
		}
	}
	
}
