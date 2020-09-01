package com.peaceray.quantro.model.game;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.utils.ArrayOps;

/**
 * The "GameEvents" object maintains a list of important game events
 * that have transpired.  This offloads the problem of maintaining a
 * bunch of booleans (like in the flash prototype) so the View knows
 * what important things to animate.
 * 
 * NOTE: GameEvents should ONLY be used as a convenience for animation.
 * It should NEVER be used to pass information between model components.
 * Information flow should follow this process:
 * 
 * 		View: 	sets "significant" events (events which require pausing
 * 				the game to animate, such as row clears).
 * 
 * 		Model: 	sets "happened" events as they occur
 * 				refuses to advance game state so long as a significant event
 * 				has "happened"
 * 				sometimes clears "happened", preventing animations from playing.
 * 				
 * 
 * 		View: once a "significant" event that "happened" has been animated,
 * 				clear "happened".
 * 
 * Why so strict?  Why not use GameEvents to pass information from one Model
 * component to another?
 * 
 * Because the user can open a new activity at any time.  If that occurs, we
 * need to save the current game state so it can be resumed later.  To ensure
 * the loaded state is consistent (no blocks have disappeared), the Game object
 * will advance time as far as is necessary.  For instance, if we are currently
 * animating some chunks falling and they are not represented in Game's "blockField",
 * Game will tick() forward - clearing "happened" is it goes - until those
 * chunks are locked into the field again.
 * 
 * In other words, skipping animations is OK when we enter or leave the game Activity.
 * Skipping actual gameplay events is not.  Because GameEvents is not guaranteed
 * to be consistent across Activities, it should ONLY be used in the manner
 * described above.
 * 
 * A note on SerializableState: although it may be non-intuitive, we consider
 * events being "significant" or "non-significant" as state information, not
 * configuration information.  In other words, they may be safely changed
 * AFTER a call to finalizeConfiguration() (and not before).
 * 
 * @author Jake
 *
 */
public final class GameEvents implements SerializableState, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5634989772386977247L;

	private static int enm = 0 ;
	
	// GAME EVENT ENUMS
	// Major game events
	public static final int EVENT_GAME_LEVEL_UP = enm++ ;		// event 0
	public static final int EVENT_GAME_LOST = enm++ ;
	
	// Piece queue events
	public static final int EVENT_NEXT_PIECE_CHANGED = enm++ ;
	public static final int EVENT_RESERVE_PIECE_CHANGED = enm++ ;
	
	// State transition events
	public static final int EVENT_PIECE_PREPARED = enm++ ;
	public static final int EVENT_PIECE_ENTERED = enm++ ;		// event 5
	
	// Piece actions
	public static final int EVENT_PIECE_MOVED_LEFT = enm++ ;	// 6
	public static final int EVENT_PIECE_MOVED_RIGHT = enm++ ;
	public static final int EVENT_PIECE_TURNED_CCW = enm++ ;
	public static final int EVENT_PIECE_TURNED_CW = enm++ ;
	public static final int EVENT_PIECE_TURNED_CCW_180 = enm++ ;	// 10
	public static final int EVENT_PIECE_TURNED_CW_180 = enm++ ;		// 11
	public static final int EVENT_PIECE_FLIPPED = enm++ ;			// 12
	public static final int EVENT_PIECE_KICKED = enm++ ;
	public static final int EVENT_PIECE_DROPPED = enm++ ;
	public static final int EVENT_PIECE_FELL = enm++ ;				// 15
	public static final int EVENT_PIECE_LOCKED = enm++ ;			// 16
	// FAILED actions
	public static final int EVENT_PIECE_FAILED_MOVE_LEFT = enm++ ;
	public static final int EVENT_PIECE_FAILED_MOVE_RIGHT = enm++ ;
	
	// Piece Success or Failure?
	public static final int EVENT_PIECE_SUCCESS = enm++ ;			// 19
	public static final int EVENT_PIECE_FAILURE = enm++ ;			// 20
	
	
	// "Clear progression" events
	public static final int EVENT_COMPONENTS_UNLOCKED = enm++ ;
	public static final int EVENT_COMPONENTS_FELL = enm++ ;			// 22
	public static final int EVENT_COMPONENTS_LOCKED = enm++ ;		// 23
	public static final int EVENT_COMPONENTS_UNLOCKED_TOWARDS_METAMORPHOSIS = enm++ ;
	public static final int EVENT_COMPONENTS_FELL_TOWARDS_METAMORPHOSIS = enm++ ;
	public static final int EVENT_COMPONENTS_LOCKED_TOWARDS_METAMORPHOSIS = enm++ ;		// 26
	public static final int EVENT_CLEAR = enm++ ;
	public static final int EVENT_CHUNKS_UNLOCKED = enm++ ;
	public static final int EVENT_CHUNKS_FELL = enm++ ;
	public static final int EVENT_CHUNKS_LOCKED = enm++ ;			// 30
	
	// Triggered events
	public static final int EVENT_COLUMN_UNLOCKED = enm++ ;
	// Reserve usage!
	public static final int EVENT_RESERVE_INSERTED = enm++ ;
	public static final int EVENT_RESERVE_SWAPPED = enm++ ;
	public static final int EVENT_RESERVE_PUSHED = enm++ ;
	public static final int EVENT_RESERVE_ATTACK = enm++ ;
	public static final int EVENT_RESERVE_INSERT_QUEUED = enm++ ;
	public static final int EVENT_RESERVE_SWAP_QUEUED = enm++ ;
	public static final int EVENT_RESERVE_PUSH_QUEUED = enm++ ;
	public static final int EVENT_RESERVE_ATTACK_QUEUED = enm++ ;
	
	public static final int EVENT_RESERVE_DEQUEUED = enm++ ;
	
	// "Attack" events. 
	public static final int EVENT_ATTACK_PUSH_ROWS = enm++ ;
	public static final int EVENT_ATTACK_DROP_BLOCKS = enm++ ;
	public static final int EVENT_ATTACK_DISPLACEMENT_ACCEL = enm++ ;
	
	// "Metamorphosis" events
	public static final int EVENT_METAMORPHOSIS = enm++ ;
	public static final int EVENT_PIECE_LOCK_THEN_METAMORPHOSIS = enm++ ;
	public static final int EVENT_METAMORPHOSIS_DID_ACTIVATE = enm++ ;
	public static final int EVENT_METAMORPHOSIS_DID_DEACTIVATE = enm++ ;
			// includes all EVENT_COMPONENTS_* events, as well as EVENT_PIECE_LOCKED and EVENT_METAMORPHOSIS.
	
	// "Special" system
	public static final int EVENT_SPECIAL_UPGRADE = enm++ ;
	
	// "Displacement" rows transferred.
	public static final int EVENT_DISPLACEMENT_TRANSFERRED = enm++ ;
	public static final int EVENT_DISPLACEMENT_PREFILLED = enm++ ;
	
	// Synchronized.
	public static final int EVENT_SYNCHRONIZED = enm++ ;
	
	// Full synchronization applied by outside message
	public static final int EVENT_FULL_SYNCHRONIZATION_APPLIED = enm++ ;
	
	public static final int NUM_EVENTS = enm ;
	
	
	// Instance vars
	private boolean [] happened ;		// Happened since last clear
	private boolean [] significant ;	// Is this event considered "significant"?
	
	private boolean configured ;
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder() ;
		sb.append("GameEvents[ ") ;
		boolean hasEvent = false ;
		for ( int i = 0; i < happened.length; i++ ) {
			if ( happened[i] ) {
				if ( hasEvent )
					sb.append(", ") ;
				sb.append(i) ;
				if ( significant[i] )
					sb.append("(!)") ;
				hasEvent = true ;
			}
		}
		
		sb.append(" ]") ;
		return sb.toString() ;
	}
	
	
	// Constructor!
	public GameEvents() {
		happened = new boolean[NUM_EVENTS] ;
		significant = new boolean[NUM_EVENTS] ;
		for ( int i = 0; i < NUM_EVENTS; i++ ) {
			happened[i] = significant[i] = false ;
		}
		configured = false ;
	}
	
	public GameEvents(GameEvents gevents) {
		this() ;
		if ( gevents.happened != null && gevents.happened.length == NUM_EVENTS ) {
			configured = gevents.configured ;
			happened = ArrayOps.duplicate(gevents.happened) ;
			significant = ArrayOps.duplicate(significant) ;
		}
	}
	
	public void takeVals( GameEvents gevents ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( gevents.happened != null && gevents.happened.length == NUM_EVENTS ) {
			for ( int i = 0; i < NUM_EVENTS; i++ ) {
				happened[i] = gevents.happened[i] ;
				//significant[i] = gevents.significant[i] ;
			}
		}
	}
	
	public synchronized void clearSignificance() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		for ( int i = 0; i < NUM_EVENTS; i++ ) {
			significant[i] = false ;
		}
	}
	
	public synchronized void setSignificance( int eventNum, boolean sig ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		significant[eventNum] = sig ;
	}
	
	public synchronized boolean getSignificance( int eventNum ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return significant[eventNum] ;
	}
	
	public synchronized void setHappened( int eventNum ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		happened[eventNum] = true ;
	}
	
	public synchronized void forgetHappened( int eventNum ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		happened[eventNum] = false ;
	}
	
	public synchronized boolean getHappened( int eventNum ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return happened[eventNum] ;
	}
	
	public synchronized boolean eventHappened() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		for ( int i = 0; i < NUM_EVENTS; i++ ) {
			if ( happened[i] ) {
				return true ;
			}
		}
		return false ;
	}
	
	public synchronized boolean significantEventHappened() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		for ( int i = 0; i < NUM_EVENTS; i++ ) {
			if ( significant[i] && happened[i] ) {
				return true ;
			}
		}
		return false ;
	}
	
	public synchronized void clearHappened() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		for ( int i = 0; i < NUM_EVENTS; i++ ) {
			happened[i] = false ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE 
	//
	// These methods provide the implementation of the Serializable
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeObject(happened) ;
		stream.writeObject(significant) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		boolean [] happenedIn = (boolean [])stream.readObject() ;
		boolean [] significantIn = (boolean [])stream.readObject() ;
		
		if ( happenedIn.length == NUM_EVENTS ) {
			happened = happenedIn ;
			significant = significantIn ;
		} else {
			happened = new boolean[NUM_EVENTS] ;
			significant = new boolean[NUM_EVENTS] ;
		}
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
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
	public GameEvents finalizeConfiguration() throws IllegalStateException {
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
		return this ;
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
		return new GameEvents(this) ;
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
		this.takeVals( (GameEvents)in ) ;
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
		outStream.writeObject(this) ;
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
	public GameEvents readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		this.readObject(inStream) ;
		return this ;
	}
	
}
