package com.peaceray.quantro.model.systems.timing;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.utils.simulatedarray.SimulatedArray;


/**
 * The CustomTimingSystem allows enter, fall and lock delays to be arbitrarily
 * set using arrays indexed by current level.
 * 
 * Its behavior is relatively straightforward - the specified delay must pass
 * before the given activity is allowed.  By default there is a delay of 0;
 * if fallDelay is set, but not lockDelay, than the entries of "fallDelay" will be
 * used when determining locking behavior.
 * 
 * There are three wrinkles to using this timing system that may justify subclassing
 * for different behavior.
 * 
 * 1. Kicks, drops and falls. The "lock countdown", which determines whether a piece
 * should lock in place, will reset only when the piece actually reaches a lower offset.
 * In other words, if a user rotates a piece to "kick" it to a higher level, the
 * lock delay remains unchanged and the piece will lock very quickly after.  Only
 * a drop, fall, or kick that takes a piece *lower than it has been before* will reset
 * this countdown.  This prevents infinite spins by kicking vertically repeatedly.
 * 
 * 2. One fall per tick.  This timing system allows no more than a single fall per "tick",
 * no matter how much time passed since the last tick.  Thus it cannot be used for 20G
 * behavior, except by manually dropping pieces more than one row in the caller.  Even
 * fall and lock delays of 0 will produce a system that requires some nonzero time to pass
 * between 1 row falls, and once the piece has landed, an additional nonzero time to pass
 * before it locks.
 * 
 * 3. Only level determines delays.  This system does not allow any situational behavior,
 * such as "power ups" and such, to affect delays.  Only the current level (and the time
 * since the last event) is relevant.
 * 
 * A note on SerializableState: The CustomTimingSystem has a state consisting of
 * enter_countdown, fall_countdown, lock_countdown, and minPieceHeight.  These
 * values are ensconced in a Parcelable, Serializable instance of CustomTimingSystemState,
 * defined in this file after CustomTimingSystem.
 * 
 * @author Jake
 *
 */
public class CustomTimingSystem extends TimingSystem {
	
	public static final double TICK_UNDERSHOOT_MARGIN_STRICT = 0.3 ;
	public static final double TICK_UNDERSHOOT_MARGIN_GENEROUS = 0.5 ;
	public static final long TICK_UNDERSHOOT_MIN_TICKS_FOR_AVERAGE = 20 ;
	
	
	// Some storage space for stuff.
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	// Configuration
	private SimulatedArray enterDelay ;
	private SimulatedArray fallDelay ;
	private SimulatedArray lockDelay ;
	
	private SimulatedArray fastFallDelay ;
	
	// Here's our state
	private CustomTimingSystemVersionedState state ;
	private boolean configured ;
	

	
	///////////////////////////////////////////////////
	// CONSTRUCT
	
	/**
	 * Constructs a CustomTimingSystem
	 * 
	 * @param ginfo The GameInformation
	 * @param qi The QInteractions
	 */
	public CustomTimingSystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		state = new CustomTimingSystemVersionedState() ;
		configured = false ;
		
		// By default, no delay.
		enterDelay = null ;
		fallDelay = null ;
		lockDelay = null ;
		fastFallDelay = null ;
	}
	
	///////////////////////////////////////////////////
	// SETTING INFO

	
	
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
	public QInteractions getQInteractions() {
		return qi ;
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// CONFIGURATION METHODS
	//
	// Although this system does not maintain a state,
	// these methods are considered "configuration" under the specification
	// given by SerializableState.
	//
	////////////////////////////////////////////////////////////////
	
	
	/**
	 * Sets the enter delay such that at level k it takes 
	 * ar[k-1] seconds to enter.  Entries in 'ar' are copied.
	 * 
	 * @param ar An array of length numLevelsPossible.
	 */
	public void setEnterDelayArray( SimulatedArray sa ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		enterDelay = sa ;
	}
	
	
	/**
	 * Sets the fall delay such that at level k it takes 
	 * ar[k-1] seconds to enter.  Entries in 'ar' are copied.
	 * 
	 * @param ar An array of length numLevelsPossible.
	 */
	public void setFallDelayArray( SimulatedArray sa ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		fallDelay = sa ;
		if ( lockDelay == null )
			setLockDelayArray( sa ) ;
	}
	
	
	/**
	 * Sets the lock delay such that at level k it takes 
	 * ar[k-1] seconds to enter.  Entries in 'ar' are copied.
	 * 
	 * NOTE: Calling this method may not be necessary.  By default,
	 * pieces take the same amount of time to lock as to fall, so
	 * a single call to setFallDelayArray takes care of this as well.
	 * 
	 * @param ar An array of length numLevelsPossible.
	 */
	public void setLockDelayArray( SimulatedArray sa ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		lockDelay = sa ;
	}
	
	
	public void setFastFallDelayArray( SimulatedArray sa ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		fastFallDelay = sa ;
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
	
	
	///////////////////////////////////////////////////
	// TIME PASSES
	
	/**
	 * Notify the timing system that time has passed.
	 * 
	 * @param time The number of seconds since the last call to 'tick'
	 */
	public void tick( double time ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// update avg.
		state.updateAverageTick(time) ;
		
		state.enter_countdown += time ;
		state.fall_countdown += time ;
		state.lock_countdown += time ;
	}
	
	///////////////////////////////////////////////////
	// THINGS HAPPEN
	
	/**
	 * didMove: Tells the timing system that the piece moved.
	 * moveChange is the "delta" for the movement.
	 * 
	 * @param didMove The delta for the movement
	 */
	public void didMove( Offset moveChange ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		// Moving does not reset anything.
	}
	
	/**
	 * didTurn: Tells the timing system that the piece provided
	 * just turned.  The Piece object contains rotation info
	 * 
	 * @param piece The piece which turned.
	 */
	public void didTurn( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		// Turning does not do anything
	}
	
	/**
	 * didFlip: Tells the timing system that the piece provided just flipped.
	 * @param piece
	 * @param offset
	 */
	public void didFlip( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Flipping might change the vertical position.  If the piece is now
		// lower than it was, we reset "minPieceHeight" and the fall/lock delays.
		if ( offset.y < state.minPieceHeight ) {
			state.fall_countdown = 0 ;
			state.lock_countdown = 0 ;
			state.minPieceHeight = offset.y ;
		}
	}
	
	/**
	 * didKick: Tells the timing system that the piece provided
	 * was just kicked to a new location.  Everything is provided.
	 * 
	 * @param piece	The piece that was kicked
	 * @param offset The new offset for the piece
	 */
	public void didKick( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Kicking might change the vertical position.  If the piece is now
		// lower than it was, we reset "minPieceHeight" and the fall/lock delays.
		if ( offset.y < state.minPieceHeight ) {
			state.fall_countdown = 0 ;
			state.lock_countdown = 0 ;
			state.minPieceHeight = offset.y ;
		}
	}
	
	/**
	 * didFall: Tells the timing system that the piece provided
	 * has fallen to the new offset.
	 * @param newOffset
	 */
	public void didFall( Offset newOffset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// If the piece fell to a new offset, update the lock_countdown.
		// Definitely update the fall countdown.
		
		state.fall_countdown = 0 ;
		if ( newOffset.y < state.minPieceHeight ) {
			state.lock_countdown = 0 ;
			state.minPieceHeight = newOffset.y ;
		}
	}
	
	/**
	 * didDrop: Tells the timing system that the user dropped the
	 * piece to the bottom (or as far as it would go)
	 * @param piece The piece the user dropped
	 * @param offset The new offset for the piece
	 */
	public void didDrop( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// If the piece dropped to a new offset, update the
		// lock_countdown.
		
		state.fall_countdown = 0 ;
		if ( offset.y < state.minPieceHeight ) {
			state.lock_countdown = 0 ;
			state.minPieceHeight = offset.y ;
		}
	}
	
	
	/**
	 * didLock: Tells the timing system that the piece locked in place.
	 * @param blockField
	 * @param piece
	 * @param offset
	 */
	public void didLock( byte [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Nothing happens when we lock, except maybe reset the enter
		// delay.
		state.enter_countdown = 0 ;
	}
	
	/**
	 * didClear: Tells the timing system that a clear occurred.
	 * @param rowArray	The rows that were cleared
	 */
	public void didClear( int [] chromaticRowArray, boolean [] monochromaticRowArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// All... right?  Reset enter delay?
		state.enter_countdown = 0 ;
	}
	
	/**
	 * didClear: Tells the timing system that a clear occurred.
	 * Because this rowArray is boolean, this was a monochrome clear.
	 * @param rowArray	The rows that were cleared
	 */
	public void didClear( boolean [] rowArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// All... right?  Reset enter delay?
		state.enter_countdown = 0 ;
	}
	
	/**
	 * didPrepare: a new piece (provided) is preparing to enter the game.
	 * @param piece The piece that entered
	 * @param offset The offset at which it is prepared
	 */
	public void didPrepare( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		state.enter_countdown = 0 ;
	}
	
	/**
	 * didEnter: a new piece (provided) entered the game.
	 * @param piece The piece that entered
	 * @param offset The offset at which it entered
	 */
	public void didEnter( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Reset some delays.  Also set min height.
		state.enter_countdown = 0 ;
		state.fall_countdown = 0 ;
		state.lock_countdown = 0 ;
		
		state.minPieceHeight = offset.y ;
	}
	
	///////////////////////////////////////////////////
	// SHOULD THINGS HAPPEN?
	
	/**
	 * canMove: Ask the timing system whether, as far as
	 * it's concerned, the piece should move (probably
	 * according to user input).
	 * 
	 * @param moveChange The 'delta' for the proposed move.
	 * @return Can the piece move?
	 */
	public boolean canMove( Offset moveChange ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// For now, the piece can always move.
		return true ;
	}
	
	/**
	 * canTurn: Asks the timing system whether it's appropriate
	 * for the piece to turn now.
	 * 
	 * @param piece The Piece object to be turned.
	 * @param rotDirection One of the constants for TURN_* defined by RotationSystem
	 * @return Can the piece turn the specified way?
	 */
	public boolean canTurn( Piece piece, int rotDirection ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Piece can always turn
		return true ;
	}
	
	/**
	 * canFlip: Asks the timing system whether it's appropriate
	 * for the piece to flip now.
	 * 
	 * @param piece The Piece object to be flipped.
	 * @return Can the piece flip?
	 */
	public boolean canFlip( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Piece can always flip
		return true ;
	}
	
	
	
	/**
	 * canKick: Ask the timing system whether a kick is appropriate
	 * at the moment.
	 * 
	 * @param piece	The piece to be kicked
	 * @return Can it be kicked?
	 */
	public boolean canKick( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Yup.
		return true ;
	}
	
	/**
	 * canFall: Ask the timing system whether it's time for the piece
	 * to fall a bit.
	 * 
	 * @return Appropriate to fall?
	 */
	public boolean canFall( ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Time to fall?
		return canFallTickCheck( state.fall_countdown,
				fallDelay.getDouble(ginfo.level-1 ) ) ;
	}
	
	/**
	 * canDrop: Ask the timing system whether it's "time" to drop the
	 * piece, probably because the user pressed 'down' to drop it.
	 * 
	 * @param piece The piece the user dropped
	 * @param offset The new offset for the piece
	 * @return Appropriate to drop?
	 */
	public boolean canDrop( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Always can.
		return true ;
	}
	
	
	public boolean canFastFall() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Time to fall?
		double minTime = Math.min(
				fallDelay.getDouble(ginfo.level-1),
				fastFallDelay.getDouble(ginfo.level-1)) ;
		
		return canFastFallTickCheck( state.fall_countdown, minTime ) ;
	}
	
	/**
	 * canLock: Is it time to lock the piece?
	 * 
	 * @param blockField
	 * @param piece
	 * @param offset
	 * @return Enough time has passed that locking is appropriate, if
	 * the piece can lock at this location (not checked by TimingSystem)
	 */
	public boolean canLock( byte [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// If lock delay and fall delay are the same, treat this as a fall, including
		// tick consideration.  Otherwise, treat as its own thing.
		double minTime = lockDelay.getDouble(ginfo.level-1) ;
		if ( minTime == fallDelay.getDouble(ginfo.level-1) ) {
			return canFallTickCheck( state.lock_countdown, minTime ) ;
		}
		
		return state.lock_countdown > lockDelay.getDouble(ginfo.level-1) ;
	}
	
	/**
	 * canClear: Can we go ahead and clear some rows?
	 * 
	 * @return Should we do this clear?
	 */
	public boolean canClear( ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Always can.
		return true ;
	}
	
	/**
	 * canEnter: Can the piece provided entered the game.
	 * 
	 * @param piece The piece to be entered
	 * @param offset The offset at which it will enter
	 * @return Timing-wise, is it appropriate to enter the piece?
	 */
	public boolean canEnter( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// By default, there is no delay.
		if ( enterDelay == null )
			return true ;
		
		return state.enter_countdown > enterDelay.getDouble(ginfo.level-1) ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// HELPER METHODS
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Uses current state settings regarding Ticks to determine if we are
	 * ready to fall yet.  This method also updates our 'undershot' variables,
	 * in the assumption that you follow this method's advice.
	 */
	private boolean canFallTickCheck( double timeSince, double minTime ) {
		if ( minTime == 0 )
			return true ;
		
		if ( didOvershoot( timeSince, minTime ) ) {
			// System.err.println("CustomTimingSystem avg tick " + state.averageTickLength + " overshot by " + (timeSince - minTime)) ;
			
			state.lastFallUndershot = false ;
			return true ;
		}
		
		// otherwise, check if we should undershoot.  Adjust the current
		if ( didUndershoot( timeSince, minTime, state.lastFallUndershot ) ) {
			// System.err.println("CustomTimingSystem avg tick " + state.averageTickLength + " undershot by " + (minTime - timeSince)) ;
			
			state.lastFallUndershot = true ;
			return true ;
		}
		
		return false ;
	}
	
	/**
	 * Uses current state settings regarding Ticks to determine if we are
	 * ready to fall yet.  This method also updates our 'undershot' variables,
	 * in the assumption that you follow this method's advice.
	 */
	private boolean canFastFallTickCheck( double timeSince, double minTime ) {
		if ( minTime == 0 )
			return true ;
		
		if ( didOvershoot( timeSince, minTime ) ) {
			state.lastFastFallUndershot = false ;
			return true ;
		}
		
		// otherwise, check if we should undershoot.  Adjust the current
		if ( didUndershoot( timeSince, minTime, state.lastFastFallUndershot ) ) {
			state.lastFastFallUndershot = true ;
			return true ;
		}
		
		return false ;
	}
	
	/**
	 * Overshoot is a simple calculation: has more time passed than the "minimum time"
	 * required?
	 * 
	 * Contrast with: undershoot.
	 * 
	 * @param timeSince
	 * @param minTime
	 * @return
	 */
	private boolean didOvershoot( double timeSince, double minTime ) {
		return timeSince > minTime ;
	}
	
	/**
	 * Undershoot is a more complex operation with a specific function: we "undershoot"
	 * if the minimum amount of time has not passed, BUT we expect a "very wide overshoot"
	 * of the minimum time if we allow another tick to pass.
	 * 
	 * In other words, we sometimes allow an event to happen even if the minimum time has
	 * not yet passed, if we expect that doing so will result in a "closer to ideal" result
	 * than the alternative (waiting).
	 * 
	 * To that end, we track the "average tick length" as a diminishing-window style
	 * average, compare the current elapsed time to the expected elapsed time after the
	 * next "average tick."  We undershoot if the minimum time is within a specific
	 * portion of the the "average tick length;" that portion depends on whether we
	 * have previously undershoot (we prefer to consistently undershoot or overshoot,
	 * to produce the appearance of regular event times).
	 * 
	 * One complication: some devices (e.g. Nexus S) will experience an extraordinarily
	 * long tick at the start of the game, 
	 * 
	 * @param timeSince
	 * @param minTime
	 * @param lastDidUndershoot
	 * @return
	 */
	private boolean didUndershoot( double timeSince, double minTime, boolean lastDidUndershoot ) {
		if ( state.averageTickLength <= 0 ) {
			return false ;
		}
		
		if ( timeSince == 0 && minTime > 0 ) {
			return false ;
		}
		
		if ( state.averageTickNumTicks <= TICK_UNDERSHOOT_MIN_TICKS_FOR_AVERAGE ) {
			return false ;
		}
		
		if ( lastDidUndershoot ) {
			timeSince += state.averageTickLength * TICK_UNDERSHOOT_MARGIN_GENEROUS ;
		} else {
			timeSince += state.averageTickLength * TICK_UNDERSHOOT_MARGIN_STRICT ;
		}
		
		return timeSince > minTime ;
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
	public CustomTimingSystem finalizeConfiguration() throws IllegalStateException {
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
		return new CustomTimingSystemVersionedState( state ) ;
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
		
		// We have changed the state class to a Versioned class.  Maintain
		// compatability with earlier saved games by checking the class
		// of the provided state.
		if ( in instanceof CustomTimingSystemState )
			state = new CustomTimingSystemVersionedState((CustomTimingSystemState)in) ;
		else
			state = (CustomTimingSystemVersionedState)in ;
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
	public TimingSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		Object in = inStream.readObject() ;
		// We have changed the state class to a Versioned class.  Maintain
		// compatibility with earlier saved games by checking the class
		// of the provided state.
		if ( in instanceof CustomTimingSystemState )
			state = new CustomTimingSystemVersionedState((CustomTimingSystemState)in) ;
		else
			state = (CustomTimingSystemVersionedState)in ;
		return this ;
	}
	
	
}



class CustomTimingSystemVersionedState implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5219904529116547108L;
	
	private static final int VERSION = 2 ;
	// VERSION 0 is the moment we transition from CustomTimingSystemState
	// 		to CustomTimingSystemVersionedState.  This state includes
	// 		'fastfall_countdown.'
	// VERSION 1 includes 'tick-level' information.  An average tick
	//		length
	// VERSION 2 includes 'number of ticks.'  Used to prevent the use
	//		of 'average tick length' until it has enough accumulated data.
	
	// Timing stuff
	public double enter_countdown ;
	public double fall_countdown ;
	public double lock_countdown ;
	
	// Here's our record of previous height
	public int minPieceHeight ;
	
	// Tick-level information
	public double averageTickLength ;
	public long averageTickNumTicks ;
	// over/under: we make the decision on whether to advance
	// based on the average tick length.  
	public boolean lastFallUndershot ;
	public boolean lastFastFallUndershot ;
	
	
	public static final double UNSET_AVERAGE_TICK = -1 ;
	public static final double AVERAGE_ALPHA = 0.3 ;			// The higher this number, the more we prioritize new data.
	
	/////////////////////////////////////////////
	// constructor
	public CustomTimingSystemVersionedState() {
		enter_countdown = 0 ;
		fall_countdown = 0 ;
		lock_countdown = 0 ;
		
		minPieceHeight = Integer.MAX_VALUE ; // That's pretty high!
		
		averageTickLength = UNSET_AVERAGE_TICK ;
		averageTickNumTicks = 0 ;
		lastFallUndershot = false ;
		lastFastFallUndershot = false ;
	}

	public CustomTimingSystemVersionedState( CustomTimingSystemVersionedState state ) {
		enter_countdown = state.enter_countdown ;
		fall_countdown = state.fall_countdown ;
		lock_countdown = state.lock_countdown ;
		
		minPieceHeight = state.minPieceHeight ;
		
		averageTickLength = state.averageTickLength ;
		averageTickNumTicks = state.averageTickNumTicks ;
		lastFallUndershot = state.lastFallUndershot ;
		lastFastFallUndershot = state.lastFastFallUndershot ;
	}
	
	
	public CustomTimingSystemVersionedState( CustomTimingSystemState state ) {
		enter_countdown = state.enter_countdown ;
		fall_countdown = state.fall_countdown ;
		lock_countdown = state.lock_countdown ;
		
		minPieceHeight = state.minPieceHeight ;
		
		averageTickLength = UNSET_AVERAGE_TICK ;
		averageTickNumTicks = 0 ;
		lastFallUndershot = false ;
		lastFastFallUndershot = false ;
	}
	
	
	public void updateAverageTick( double seconds ) {
		if ( averageTickLength == UNSET_AVERAGE_TICK ) {
			averageTickLength = seconds ;
		} else {
			averageTickLength = averageTickLength * (1 - AVERAGE_ALPHA)
					+ seconds * AVERAGE_ALPHA ;
		}
		averageTickNumTicks++ ;
	}
	

	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// FIRST: write the version.
		stream.writeInt(VERSION) ;
		
		// Write countdowns...
		stream.writeDouble(enter_countdown) ;
		stream.writeDouble(fall_countdown) ;
		stream.writeDouble(lock_countdown) ;
		
		// And the minimum piece height.
		stream.writeInt(minPieceHeight) ;
		
		// write boolean: has more
		stream.writeBoolean(true) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Read version.
		int version = stream.readInt() ;
		
		// Read countdowns...
		enter_countdown = stream.readDouble() ;
		fall_countdown = stream.readDouble() ;
		lock_countdown = stream.readDouble() ;
		
		// And the minimum piece height.
		minPieceHeight = stream.readInt() ;
		
		// why would we serialize this?  It's pointless.  Just start our
		// average over.
		averageTickLength = UNSET_AVERAGE_TICK ;
		averageTickNumTicks = 0 ;
		lastFallUndershot = false ;
		lastFastFallUndershot = false ;
		
		// read out things we USED to include in a serialization.
		if ( version == 1 ) {
			stream.readDouble() ;
			stream.readBoolean() ;
			stream.readBoolean() ;
		}
		
		// Read boolean: should be false ('has more')
		 stream.readBoolean() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
	
}


class CustomTimingSystemState implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4722801719740208141L;
	
	
	// Timing stuff
	public double enter_countdown ;
	public double fall_countdown ;
	public double lock_countdown ;
	
	// Here's our record of previous height
	public int minPieceHeight ;
	
	/////////////////////////////////////////////
	// constructor
	public CustomTimingSystemState() {
		enter_countdown = 0 ;
		fall_countdown = 0 ;
		lock_countdown = 0 ;
		
		minPieceHeight = Integer.MAX_VALUE ; // That's pretty high!
	}

	public CustomTimingSystemState( CustomTimingSystemState state ) {
		enter_countdown = state.enter_countdown ;
		fall_countdown = state.fall_countdown ;
		lock_countdown = state.lock_countdown ;
		
		minPieceHeight = state.minPieceHeight ;
	}
	

	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		// Write countdowns...
		stream.writeDouble(enter_countdown) ;
		stream.writeDouble(fall_countdown) ;
		stream.writeDouble(lock_countdown) ;
		
		// And the minimum piece height.
		stream.writeInt(minPieceHeight) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Read countdowns...
		enter_countdown = stream.readDouble() ;
		fall_countdown = stream.readDouble() ;
		lock_countdown = stream.readDouble() ;
		
		// And the minimum piece height.
		minPieceHeight = stream.readInt() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
}
