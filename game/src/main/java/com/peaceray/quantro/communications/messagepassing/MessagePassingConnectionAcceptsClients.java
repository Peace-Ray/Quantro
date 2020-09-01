package com.peaceray.quantro.communications.messagepassing;

import com.peaceray.quantro.communications.ClientInformation;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.WrappedSocket;

/**
 * An interface for any connection which accepts clients.
 * 
 * @author Jake
 *
 */
public interface MessagePassingConnectionAcceptsClients {
	
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
	 * Throws IllegalStateException if not pending.
	 * 
	 * @param wsock
	 * @param cinfo
	 * @return Are we now connected using the provided wrapped socket?
	 */
	public boolean acceptClient( Object caller, WrappedSocket wsock, ClientInformation cinfo ) throws IllegalStateException ;

	/**
	 * The Connection, which must be 'pending', will transition
	 * to FAILED status after this call.
	 * 
	 * Make this call when, for whatever reason, our attempt to connect
	 * with a client failed.
	 * 
	 * @throws IllegalStateException
	 */
	public void failedToFindClient( Object caller ) throws IllegalStateException ;
	
	/**
	 * This Connection requires that the client knows the nonce
	 * value before connecting.
	 * 
	 * @return
	 */
	public boolean requiresNonce() ;
	
	/**
	 * This Connection requires that the client knows the personalNonce
	 * value before connecting.
	 * 
	 * @return
	 */
	public boolean requiresPersonalNonce() ;
	
	/**
	 * This Connection will remember the personal nonce of those
	 * who successfully connect.  A superset of requiresPersonalNonce.
	 * 
	 * @return
	 */
	public boolean remembersPersonalNonce() ;
	
	/**
	 * What is the Nonce for this connection?
	 * @return
	 */
	public Nonce getNonce() ;
	
	/**
	 * Personal Nonce for the user on the other end of this connection?
	 * @return
	 */
	public Nonce getRemotePersonalNonce() ;
}
