package com.peaceray.quantro.model.systems.displacement;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.utils.ArrayOps;

/**
 * Never displaces anything.  This is the standard for all game types predating
 * April 2013.  Intended to never change existing gameplay behavior.
 * 
 * @author Jake
 *
 */
public class DeadDisplacementSystem extends DisplacementSystem {
	
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	private boolean configured ;
	private EmptyState state ;
	
	
	
	// Constructerrr
	public DeadDisplacementSystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		configured = false ;
		state = new EmptyState() ;
	}
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this LockSystem.
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
	public final QInteractions getQInteractions() {
		return qi ;
	}
	
	@Override
	public final DeadDisplacementSystem setPseudorandom( int pseudorandom ) {
		return this ;
	}
	

	@Override
	public boolean tick(double seconds) {
		// no effect
		return false ;
	}

	@Override
	public void prefill(byte[][][] displacementRows) {
		ArrayOps.setEmpty(displacementRows) ;
	}

	@Override
	public void transferDisplacedRows(
			byte[][][] blockfield, byte[][][] displacementRows, int rows) {
		
		// shift up
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int r = blockfield[0].length -1; r >= rows; r-- ) {
				for ( int c = 0; c < blockfield[0][0].length; c++ ) {
					blockfield[qp][r][c] = blockfield[qp][r-rows][c] ;
				}
			}
		}
		
		// fake out!  this is an empty system!
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int r = 0; r < rows; r++ ) {
				for ( int c = 0; c < blockfield[0][0].length; c++ ) {
					blockfield[qp][r][c] = 0 ;
				}
			}
		}
	}
	
	public boolean accelerateDisplacement( double rows ) {
		// We don't support displacement, so we don't support displacement.
		return false ;
	}
	
	/**
	 * Does this system ever produce displacement?  e.g.,
	 * a DeadDisplacementSystem -- used for gametypes 
	 * pre-April 2013, would return 'false.'
	 * @return
	 */
	public boolean displaces() {
		return false ;
	}

	@Override
	public double getDisplacedRows() {
		// empty.  We never displace anything.
		return 0;
	}
	
	@Override
	public double getDisplacedAndTransferredRows() {
		// do nothing
		return 0 ;
	}
	
	@Override
	public double getDisplacementSeconds() {
		// don't care
		return 0 ;
	}
	
	@Override
	public void setDisplacementSecondsAndDisplacedAndTransferredRows( double ticks, double rows ) {
		// no effect
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
	public DeadDisplacementSystem finalizeConfiguration() throws IllegalStateException {
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
	public DeadDisplacementSystem setStateAsSerializable( Serializable in ) throws IllegalStateException {
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
	public DeadDisplacementSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}

}
