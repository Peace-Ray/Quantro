package com.peaceray.quantro.communications.messagepassing;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;


/**
 * A counterpart interface to MessagePassingConnectionAcceptsClients, just
 * as "ClientInterrogationResponderThread" is a counterpart to "ClientInterrogatorThread."
 * 
 * @author Jake
 *
 */
public interface MessagePassingConnectionBecomesClient {

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
	public boolean becomeClient( Object caller, WrappedSocket wsock ) throws IllegalStateException ;

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
	public void failedToBecomeClient( Object caller ) throws IllegalStateException ;
	
	
	/**
	 * What is the Nonce for this connection?
	 * @return
	 */
	public Nonce getNonce() ;
	
	/**
	 * Personal Nonce for the user on the this end of the connection?
	 * @return
	 */
	public Nonce getLocalPersonalNonce() ;
	
	
	/**
	 * Name for the user on this side of the connection?
	 * @return
	 */
	public String getLocalName() ;
	
}
