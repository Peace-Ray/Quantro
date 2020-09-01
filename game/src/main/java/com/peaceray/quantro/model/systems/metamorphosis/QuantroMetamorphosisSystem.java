package com.peaceray.quantro.model.systems.metamorphosis;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;

public class QuantroMetamorphosisSystem extends MetamorphosisSystem {

	private GameInformation ginfo ;
	private QInteractions qi ;
	
	// The EarlyCollisionSystem has no state, but just to
	// implement SerializableState in an intuitive way,
	// we keep an empty "state" object.
	protected EmptyState state ;
	protected boolean configured ;
	
	protected Result result ;		// mutated and returned with each call.
	
	
	// Constructor!
	public QuantroMetamorphosisSystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		state = new EmptyState() ;
		configured = false ;
		
		result = new Result() ;
	}

	
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this CollisionSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public GameInformation getGameInformation() {
		return ginfo ;
	}

	
	/**
	 * getQInteractions: Collisions are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public QInteractions getQInteractions() {
		return qi ;
	}

	
	
	@Override
	public Result endCycle(byte [][][] field) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// We deactivate everything.
		result.clear() ;
		if ( metamorphosize( MetamorphosisSystem.METAMORPHOSIS_DEACTIVATE, field ) ) {
			result.setDid(MetamorphosisSystem.METAMORPHOSIS_DEACTIVATE) ;
		}
		return result ;
	}

	
	
	@Override
	public boolean metamorphosize(int metamorphType, byte [][][] field) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// iterate through the entire field, looking for things we can
		// <perform action> on according to QInteractions.
		switch( metamorphType ) {
		case METAMORPHOSIS_ACTIVATE:
			return metamorphosizeActivate(field) ;
		case METAMORPHOSIS_DEACTIVATE:
			return metamorphosizeDeactivate(field) ;
		}
		
		throw new IllegalArgumentException("metamorphType " + metamorphType + " is not supported.") ;
	}
	
	private boolean metamorphosizeActivate( byte [][][] field ) {
		boolean changed = false ;
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < field[0].length; r++ ) {
				for ( int c = 0; c < field[0][0].length; c++ ) {
					if ( qi.activates(field, q, r, c) ) {
						qi.activate(field, q, r, c) ;
						changed = true ;
					}
				}
			}
		}
		return changed ;
	}
	
	private boolean metamorphosizeDeactivate( byte [][][] field ) {
		boolean changed = false ;
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < field[0].length; r++ ) {
				for ( int c = 0; c < field[0][0].length; c++ ) {
					if ( qi.deactivates(field, q, r, c) ) {
						qi.deactivate(field, q, r, c) ;
						changed = true ;
					}
				}
			}
		}
		return changed ;
	}
	
	
	
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE STATE
	//
	// These methods provide the implementation of the SerializableState
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
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
	 * @throws IllegalStateException If called more than once, or
	 * 				before necessary configuration is complete.
	 */
	public QuantroMetamorphosisSystem finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		configured = true ;
		return this ;
	}
	
	
	
	/**
	 * Returns, as a Serializable object, the current "state" of
	 * this object, which is assumed to be in "stateful use" phase.
	 * 
	 * Calling 'setStateAsParcelable' on this object at any future
	 * point, or on another instance of the object which had an identical
	 * configuration phase, should produce an object with identical
	 * behavior and state to the one whose Serializable state was
	 * extracted - no matter what state the object was in before
	 * setState... was called.
	 * 
	 * @return Current state as a Serializable
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public Serializable getStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return state ;
	}
	
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
	public Serializable getCloneStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return new EmptyState( state ) ;
	}
	
	
	/**
	 * Sets the object state according to the Serializable provided,
	 * which can be assumed to have been returned by 'getStateAsSerializable()'
	 * called on an object of the same class which underwent the same
	 * pre-"finalizeConfiguration" config. process.
	 * 
	 * POST-CONDITION: The receiver will have identical state and functionality
	 * to the object upon which "getStateAsParcelable" was called.
	 * 
	 * @param in A Serializable state from an object
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public SerializableState setStateAsSerializable( Serializable in ) throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)in ;
		return this ;
	}
	
	
	/**
	 * Writes the current state, as a Serializable object, to the provided "outStream".
	 * The same assumptions and requirements of getStateAsParcelable are true here
	 * as well.
	 * 
	 * @param outStream	An output stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If writing to the stream fails
	 */
	public void writeStateAsSerializedObject( ObjectOutputStream outStream ) throws IllegalStateException, IOException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		outStream.writeObject(state) ;
	}
	
	/**
	 * Reads the current state, as a Serializable object, from the provided "inStream".
	 * The same assumptions and requirements of setStateAsParcelable are true here
	 * as well.
	 * 
	 * @param inStream An input stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If reading from the stream fails
	 * @throws ClassNotFoundException	If the stream does not contain the class representing
	 * 			this object's Serializable state.
	 */
	public QuantroMetamorphosisSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}
	
}
