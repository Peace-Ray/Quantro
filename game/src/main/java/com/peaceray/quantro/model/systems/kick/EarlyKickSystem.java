package com.peaceray.quantro.model.systems.kick;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;


import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.collision.CollisionSystem;

/**
 * This early kick system applies a generic approach to pieces:
 * it tries horizontal (preferring the rotation direction),
 * then down, then diagonally down (again preferring rotation direction)
 * and finally up.
 * 
 * TODO: Add special case handling for line pieces (and possibly TETRA_CAT_RECT).
 * At present they are processed in a generic fashion, despite the fact that
 * kicking a line up to 2 may be appropriate, and generic kicks for TETRA_CAT_RECT
 * result in it moving to positions which do not intersect with its previous location.
 * 
 * This could be implemented either by changing this class or (more likely)
 * subclassing or writing a new implementation of KickSystem.
 * 
 * @author Jake
 *
 */
public class EarlyKickSystem extends KickSystem {
	
	private GameInformation ginfo ;

	private ArrayList<Offset> genericKicksCW ;
	private ArrayList<Offset> genericKicksCCW ;
	
	// The EarlyKickSystem has no state, but just to
	// implement SerializableState in an intuitive way,
	// we keep an empty "state" object.
	private EmptyState state ;
	private boolean configured ;
	
	// boaConstrictor
	public EarlyKickSystem( GameInformation ginfo ) {
		this.ginfo = ginfo ;
		
		state = new EmptyState() ;
		configured = false ;		// for SerializableState
		
		genericKicksCW = new ArrayList<Offset>() ;
		genericKicksCCW = new ArrayList<Offset>() ;
		
		// Add the kicks.  CW rotation favors rightward movement.
		genericKicksCW.add( new Offset( 0, 0) ) ;		// No kick at all
		genericKicksCW.add( new Offset( 1, 0) ) ;		// Right
		genericKicksCW.add( new Offset(-1, 0) ) ;		// Left
		genericKicksCW.add( new Offset( 0,-1) ) ;		// Down
		genericKicksCW.add( new Offset( 1,-1) ) ;		// Down-right
		genericKicksCW.add( new Offset(-1,-1) ) ;		// Down-left
		genericKicksCW.add( new Offset( 0, 1) ) ;		// Up
		
		// Other one.  CCW rotation favors leftward movement.
		genericKicksCCW.add( new Offset( 0, 0) ) ;		// No kick at all
		genericKicksCCW.add( new Offset(-1, 0) ) ;		// Left
		genericKicksCCW.add( new Offset( 1, 0) ) ;		// Right
		genericKicksCCW.add( new Offset( 0,-1) ) ;		// Down
		genericKicksCCW.add( new Offset(-1,-1) ) ;		// Down-left
		genericKicksCCW.add( new Offset( 1,-1) ) ;		// Down-right
		genericKicksCCW.add( new Offset( 0, 1) ) ;		// Up
	}
	
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this KickSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public GameInformation getGameInformation() {
		return ginfo ;
	}
	
	////////////////////////////////////////////////////////////////
	//
	// STATEFUL METHODS
	//
	// Although this system does not maintain a state,
	// these methods are considered "stateful" under the specification
	// given by SerializableState.
	//
	////////////////////////////////////////////////////////////////
	
	private ArrayList<Offset> candidateKicks(Piece piece) {
		// CW rotation?
		if ( piece.rotationDirection > 0 )
			return genericKicksCW ;
		else
			return genericKicksCCW ;
	}
	
	/**
	 * kick: Attempts to find a location in the field where the piece does
	 * not collide; alters 'offset' to this position when it is found.
	 * Returns 'true' if the resulting location (which may be the current
	 * position) is a valid location for the piece; 'false' if no such
	 * location could be found.
	 * 
	 * Different implementations of this interface will have different
	 * algorithms for kick locations to try.
	 * 
	 * POSTCONDITION:
	 * 		* if returns TRUE: 'offset' now indicates a location within
	 * 			'field' where 'piece' does not collide, according to 'cs'
	 * 		* if returns FALSE: 'offset' is unchanged from before the method
	 * 			was called; there is no location near 'piece' (according
	 * 			to the kick rules used by the implementation) where it
	 * 			does not collide.
	 * 
	 * @param cs			The CollisionSystem to use to check block collisions.
	 * @param field			The block field in which 'piece' resides
	 * @param piece			The piece in question
	 * @param offset		The current location of the piece.  May be altered by this call.
	 * @param lean			The "preferred" direction for a kick.  Ignored by this kick system.
	 * @return				Is the post-method 'offset' a non-colliding location for the piece?
	 */
	public boolean kick( CollisionSystem cs, byte [][][] field, Piece piece, Offset offset, Offset lean ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Which kicks do we use?
		ArrayList<Offset> kicks = candidateKicks(piece) ;
		
		// Now try them.
		Offset origOffset = new Offset( offset.x, offset.y ) ;
		Offset kick ;
		
		for ( int i = 0; i < kicks.size(); i++ ) {
			kick = kicks.get(i) ;
			offset.x = origOffset.x + kick.x ;
			offset.y = origOffset.y + kick.y ;
			
			if ( !cs.collides( field, piece, offset ) ) {
				return true ;
			}
		}
		
		// No valid kick found.
		offset.x = origOffset.x ;
		offset.y = origOffset.y ;
		return false ;
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
	public EarlyKickSystem finalizeConfiguration() throws IllegalStateException {
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
	public KickSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}
	
}