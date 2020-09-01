package com.peaceray.quantro.model.state;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This generic state object is useful for any class implementing
 * SerializableState which does to require state information, apart
 * from (possibly) a version number.
 * 
 * One use for this version number is to maintain backwards-compatibility 
 * with saved games; if a System changes its behavior in an update, 
 * the different behaviors should be left as legacy code dependent on 
 * the version stored in the state.
 * 
 * Simply get/set instances of the VersionedState when implementing
 * the methods.
 * 
 * NOTE: It is good practice to keep an instance of VersionedState
 * available, rather than construct one when needed; its memory
 * footprint will be small compared to the allocation / GC overhead.
 * 
 * @author Jake
 *
 */
public class VersionedState implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 521194776002888362L;
	public int version ;
	
	/////////////////////////////////////////////
	// constructor
	public VersionedState( int version ) {
		this.version = version ;
	}
	
	public VersionedState( VersionedState vs ) {
		this.version = vs.version ;
	}
	
	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException { 
		stream.writeInt(version) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		version = stream.readInt() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
	
}
