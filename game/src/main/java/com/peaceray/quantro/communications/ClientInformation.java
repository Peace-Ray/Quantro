package com.peaceray.quantro.communications;

import com.peaceray.quantro.communications.nonce.Nonce;

/**
 * The ClientInformation class is meant to store metadata
 * about a client, whether for a lobby, a game, etc.  This
 * information is collected by a ClientListener before the
 * socket is passed (along with ClientInformation) to the
 * appropriate user.
 * 
 * @author Jake
 *
 */
public class ClientInformation {

	private Nonce nonce ;
	private Nonce personalNonce ;
	private String name ;
	
	// Any other information we need?
	
	
	public ClientInformation() {
		nonce = Nonce.ZERO ;
		personalNonce = Nonce.ZERO ;
		name = null ;
	}
	
	/////////////////////////////////////////////
	//
	// GETTERS/SETTERS
	//
	/////////////////////////////////////////////
	
	public ClientInformation setNonce( Nonce nonce ) {
		this.nonce = nonce ;
		return this ;
	}
	
	public Nonce getNonce() {
		return nonce ;
	}
	
	public ClientInformation setPersonalNonce( Nonce nonce ) {
		this.personalNonce = nonce ;
		return this ;
	}
	
	public Nonce getPersonalNonce() {
		return personalNonce ;
	}
	
	public ClientInformation setName( String name ) {
		this.name = name ;
		return this ;
	}
	
	public String getName() {
		return name ;
	}
	
	
	
}

