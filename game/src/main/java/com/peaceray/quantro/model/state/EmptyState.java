package com.peaceray.quantro.model.state;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;


/**
 * This generic state object is useful for any class implementing
 * SerializableState which does to require state information.
 * 
 * Simply get/set instances of the EmptyState when implementing
 * the methods.
 * 
 * NOTE: It is good practice to keep an instance of EmptyState
 * available, rather than construct one when needed; its memory
 * footprint will be small compared to the allocation / GC overhead.
 * 
 * @author Jake
 *
 */
public class EmptyState implements Serializable {
	
	// This system does not use state information.
	// Thus, its state class is trivially implemented.

	/**
	 * 
	 */
	private static final long serialVersionUID = -25542918984370041L;

	/////////////////////////////////////////////
	// empty constructor
	public EmptyState() { }
	public EmptyState( EmptyState es ) { }
	
	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException { }
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException { }
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
}
