package com.peaceray.quantro.model.state;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A class implementing the SerializableState interface must be
 * able to represent its current state in a serial form.
 * 
 * For Android apps, this means it can output its current state information
 * as either a Parcelable object, or into an ObjectOutputStream as
 * a Serialized object; both are required.
 * 
 * However, it is not necessary that the object be fully reconstructable
 * from its serialized state information.  Rather, one can assume
 * that these objects go through 2 distinct "phases of life":
 * 
 * 1. Construction and configuration
 * 		The object is constructed with certain parameters,
 * 		and then any number of "configuration" methods are called
 * 		that produce a certain behavior from the object.  "State"
 * 		is irrelevant to these methods; they neither use nor change
 * 		it (except possibly to set it to a default value).
 * 
 * 2. Stateful use
 * 		Calls are made to a set of methods (distinct from the above)
 * 		whose behavior may be influenced by the "configuration" methods
 * 		called earlier, and which rely on / make changes to the current
 * 		state.
 * 
 * Transitioning from phase 1 to 2 requires an explicit method call to
 * "finalizeConfiguration."  Systems should take care that calls to any
 * configuration method after this call throw an exception, and likewise,
 * that calls to stateful methods before it fail.
 * 
 * At any point after "finalizeConfiguration", the object should be
 * prepared to write out or read in a serialized "state", either in
 * the form of a Parcelable or via an ObjectOutputStream (Input resp.).
 * 
 * In serializing their state into "S", classes implementing this interface may
 * safely assume that, whatever steps were performed in phase 1, those
 * exact steps will ALWAYS have been performed on any future object
 * receiving the state "S".  Any attempt to load a state from a
 * differently-configured object, or from a different class, has
 * unspecified behavior.
 * 
 * @author Jake
 *
 */
public interface SerializableState {
	
	/**
	 * A call to this method transitions the object from "configuration"
	 * phase to "stateful use" phase.  Although this is not programatically
	 * enforced, classes implementing this interface should refuse
	 * (e.g. by throwing an exception) any calls to "stateful use" methods
	 * before this method is called, and likewise, any calls to 
	 * "configuration" methods afterwards.
	 * 
	 * Calls to set or retrieve object state should be considered
	 * "stateful use" - i.e., those methods should be refused if
	 * calls occur before this method.
	 * 
	 * @return A reference to this object
	 * @throws IllegalStateException If called more than once, or
	 * 				before necessary configuration is complete.
	 */
	public SerializableState finalizeConfiguration() throws IllegalStateException ;
	
	
	/**
	 * Returns, as a Parcelable object, the current "state" of
	 * this object, which is assumed to be in "stateful use" phase.
	 * 
	 * Calling 'setStateAsParcelable' on this object at any future
	 * point, or on another instance of the object which had an identical
	 * configuration phase, should produce an object with identical
	 * behavior and state to the one whose Parcelable state was
	 * extracted - no matter what state the object was in before
	 * setState... was called.
	 * 
	 * @return Current state as a Parcelable
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public Serializable getStateAsSerializable() throws IllegalStateException ;
	
	
	/**
	 * Returns as a Serializable object a clone of the current "state" of this
	 * object.  It is acceptable for 'getStateAsSerializable' to return a clone,
	 * but one should NOT rely on that assumption.  If you intend to make any changes
	 * to the resulting object, or if the callee will have mutators called after
	 * this method, always get a clone rather than getState...().
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	public Serializable getCloneStateAsSerializable() throws IllegalStateException ;
	
	
	/**
	 * Sets the object state according to the Parcelable provided,
	 * which can be assumed to have been returned by 'getStateAsParcelable()'
	 * called on an object of the same class which underwent the same
	 * pre-"finalizeConfiguration" config. process.
	 * 
	 * POST-CONDITION: The receiver will have identical state and functionality
	 * to the object upon which "getStateAsParcelable" was called.
	 * 
	 * @param in A Parcelable state from an object
	 * @return This instance
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public SerializableState setStateAsSerializable( Serializable in ) throws IllegalStateException ;
	
	
	
	
	
	/**
	 * Writes the current state, as a Serializable object, to the provided "outStream".
	 * The same assumptions and requirements of getStateAsParcelable are true here
	 * as well.
	 * 
	 * @param outStream	An output stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If writing to the stream fails
	 */
	public void writeStateAsSerializedObject( ObjectOutputStream outStream ) throws IllegalStateException, IOException ;
	
	/**
	 * Reads the current state, as a Serializable object, from the provided "inStream".
	 * The same assumptions and requirements of setStateAsParcelable are true here
	 * as well.
	 * 
	 * @param inStream An input stream for the Serialized object
	 * @return This instance
	 * 
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If reading from the stream fails
	 * @throws ClassNotFoundException	If the stream does not contain the class representing
	 * 			this object's Serializable state.
	 */
	public SerializableState readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException ;

	
}
