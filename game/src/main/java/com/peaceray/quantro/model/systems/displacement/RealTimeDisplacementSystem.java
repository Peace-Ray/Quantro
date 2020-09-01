package com.peaceray.quantro.model.systems.displacement;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.utils.simulatedarray.SimulatedArray;


/**
 * A RealTimeDisplamenteSystem adds displacement over time.  The amount of 
 * displacement is customizable and potentially dependent on the current game level.
 * 
 * 
 * @author Jake
 *
 */
public class RealTimeDisplacementSystem extends PrefillingDisplacementSystem {
	
	private boolean configured ;
	
	/**
	 * The number of rows (as a double) displaced per millisecond
	 * at the current level.
	 */
	private SimulatedArray [] rowsPerSecond ;
	
	public static final int STATE_DEFAULT = 0 ;
	public static final int STATE_ACCELERATING = 1 ;
	public static final int STATE_DECELERATING = 2 ;
	public static final int NUM_STATES = 3 ;
	
	// Constructerrr
	public RealTimeDisplacementSystem( GameInformation ginfo, QInteractions qi ) {
		super(ginfo, qi) ;
		
		configured = false ;
		setState( new State() ) ;
		
		rowsPerSecond = new SimulatedArray[NUM_STATES] ;
	}
	

	@Override
	public boolean tick(double seconds) {

		State state = (State)getState() ;
		GameInformation ginfo = getGameInformation() ;
		
		// advance displacement.
		double dispFactorDefault = this.rowsPerSecond[STATE_DEFAULT].getDouble(ginfo.level) ;
		double disp = seconds * dispFactorDefault ;
		
		// acceleration?
		if ( state.pendingAcceleratedRows > 0 ) {
			// Accelerate.
			double dispFactorAccel = this.rowsPerSecond[STATE_ACCELERATING].getDouble(ginfo.level) ;
			double dispAccel = seconds * dispFactorAccel ;
			double dispAccelSolo = dispAccel - disp ;
					// the amount of displacement for which the acceleration
					// is WHOLLY responsible.
			
			// Here's an important point.  dispAccel is the amount of
			// displacement we would apply IF WE SPENT THIS ENTIRE TIME
			// AT THE ACCELERATED PACE.  If dispAccel is less than the total
			// pending, we subtract it and proceed.  Otherwise, we need to figure
			// out the portion of dispAccel which we apply (i.e. (0,1)) and
			// apply the remaining portion of 'disp.'
			if ( dispAccelSolo < state.pendingAcceleratedRows ) {
				disp = dispAccel ;
				state.pendingAcceleratedRows -= dispAccelSolo ;
			} else {
				double portionAccel = state.pendingAcceleratedRows / dispAccelSolo ;
				disp = portionAccel*dispAccel + (1-portionAccel)*disp ;
				state.pendingAcceleratedRows = 0 ;
			}
		} else if ( state.pendingAcceleratedRows < 0 ) {
			// Decelerated.
			double dispFactorDecel = this.rowsPerSecond[STATE_DECELERATING].getDouble(ginfo.level) ;
			double dispDecel = seconds * dispFactorDecel ;
			double dispDecelOmitted = disp - dispDecel ;
			
			// dispDecel is the amount of displacement we apply in this
			// decelerated state; dispDecelOmitted is the number of rows
			// we have NOT added due to being in a decelerated state.
			if ( dispDecelOmitted < -state.pendingAcceleratedRows ) {
				// we still need to omit more rows.
				disp = dispDecel ;
				state.pendingAcceleratedRows += dispDecelOmitted ;
			} else {
				// portion of this slow-speed which should be applied?
				double portionDecel = - state.pendingAcceleratedRows  / dispDecelOmitted ;
				disp = portionDecel*dispDecel + (1-portionDecel)*disp ;
				state.pendingAcceleratedRows = 0 ;
			}
		}
		
		state.displacement += disp ;
		return disp != 0 ;
	}

	@Override
	public void transferDisplacedRows(
			byte[][][] blockfield, byte[][][] displacementRows, int rows) {
		
		super.transfer(blockfield, displacementRows, rows) ;
		
		// adjust 'displacement'\
		State state = (State)getState() ;
		state.displacement -= rows ;
	}
	
	public boolean accelerateDisplacement( double rows ) {
		State state = (State)getState() ;
		state.pendingAcceleratedRows += rows ;
		return rows != 0 ;
	}

	/**
	 * Does this system ever produce displacement?  e.g.,
	 * a DeadDisplacementSystem -- used for gametypes 
	 * pre-April 2013, would return 'false.'
	 * @return
	 */
	public boolean displaces() {
		return true ;
	}
	
	@Override
	public double getDisplacedRows() {
		State state = (State)getState() ;
		return state.displacement ;
	}
	
	@Override
	public double getDisplacedAndTransferredRows() {
		State state = (State)getState() ;
		return state.displacement + state.getRowsTransferred() ;
	}
	
	@Override
	public double getDisplacementSeconds() {
		// don't care, don't remember.
		return 0 ;
	}
	
	@Override
	public void setDisplacementSecondsAndDisplacedAndTransferredRows( double seconds, double rows ) {
		// this method is expected to be a way of transferring
		// information from the clients (who are handling
		// the realtime update for displacement) to the server
		// (who is the ultimate authority on when rows are transferred,
		// but doesn't have its own realtime displacement updates).
		// We therefore keep the same number of rows transferred,
		// placing the difference in our current displacement.
		State state = (State)getState() ;
		state.displacement = rows - state.getRowsTransferred() ;
		
		// we don't keep track of time passed.
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONFIGURATION
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	public final RealTimeDisplacementSystem setRowsPerSecond( SimulatedArray sa, int state ) {
		if ( configured )
			throw new IllegalStateException("Can't set rows per millisecond after configuration is finalized!") ;
		this.rowsPerSecond[state] = sa ;
		return this ;
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
	public RealTimeDisplacementSystem finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		
		// Verify that configuration is consistent and correct.
		// Every displacement value MUST be non-negative.
		// We REQUIRE a default displace.  Acceleration, if
		// provided, must always be greater than default.
		// Decelerated, if provided, must always be less than default.
		
		// We don't actually know how many levels are relevant (i.e.
		// where the simulated array starts to pin), and we don't
		// want to have to check the full million, so just check the
		// first 100.
		int maxLevel = Math.min(100, rowsPerSecond[STATE_DEFAULT].size() ) ;
		for ( int l = 0; l < maxLevel; l++ ) {
			double up = rowsPerSecond[STATE_DEFAULT].getDouble(l) ;
			double upAccel = rowsPerSecond[STATE_ACCELERATING] == null
					? Double.MAX_VALUE : rowsPerSecond[STATE_ACCELERATING].getDouble(l) ;
			double upDecel = rowsPerSecond[STATE_DECELERATING] == null
					? 0 : rowsPerSecond[STATE_DECELERATING].getDouble(l) ;
	
			if ( up < 0 || upAccel < 0 || upDecel < 0 ) {
				throw new IllegalStateException("finalizeConfiguration() has negative displacement for level " + l) ;
			}
			
			if ( upAccel <= up ) {
				throw new IllegalStateException("finalizeConfiguration() has accelerated displacement not greater than standard at level " + l) ;
			}
			
			if ( rowsPerSecond[STATE_DECELERATING] != null && upDecel >= up ) {
				throw new IllegalStateException("finalizeConfiguration() has decelerated displacement not less than standard at level " + l) ;
			}
		}
		
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
		return (Serializable)getState() ;
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
		return new State( (State)getState() ) ;
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
	public RealTimeDisplacementSystem setStateAsSerializable( Serializable in ) throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		setState( (State)in ) ;
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
		outStream.writeObject(getState()) ;
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
	public RealTimeDisplacementSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		setState( (State)inStream.readObject() ) ;
		return this ;
	}
	
	
	private class State implements PrefillingDisplacementSystem.State, Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8316926729892547251L;

		
		private static final int VERSION = 0 ;
		// 0: Initial version.
		
		private int pseudorandom ;
		private long rowsTransferred ;
		private long rowsGenerated ;			// total number of rows generated by 'fill' and 'displace'
		private int columnGap ;
		
		private double displacement ;			// current displacement; ticks up from zero.
		
		private double pendingAcceleratedRows ;
		
		public State() {
			pseudorandom = 0 ;
			rowsTransferred = 0 ;
			rowsGenerated = 0 ;
			columnGap = -1 ;
			
			displacement = 0 ;
			pendingAcceleratedRows = 0 ;
		}
		
		
		public State( State state ) {
			pseudorandom = state.pseudorandom ;
			rowsTransferred = state.rowsTransferred ;
			rowsGenerated = state.rowsGenerated ;
			columnGap = state.columnGap ;
			
			displacement = state.displacement ;
			pendingAcceleratedRows = state.pendingAcceleratedRows ;
		}
		
		public void setPseudorandom( int pseudorandom ) {
			this.pseudorandom = pseudorandom ;
		}
		
		public int getPseudorandom() {
			return this.pseudorandom ;
		}
		
		public void setRowsTransferred( long rowsTransferred ) {
			this.rowsTransferred = rowsTransferred ;
		}
		
		public long getRowsTransferred() {
			return rowsTransferred ;
		}
		
		public void addRowsTransferred( long trans ) {
			this.rowsTransferred += trans ;
		}
		
		public void setRowsGenerated( long rowsGenerated ) {
			this.rowsGenerated = rowsGenerated ;
		}
		
		public long getRowsGenerated() {
			return this.rowsGenerated ;
		}
		
		public long addRowsGenerated( long rows ) {
			this.rowsGenerated += rows ;
			return this.rowsGenerated ;
		}
		
		public void setColumnGap( int col ) {
			this.columnGap = col ;
		}
		
		public int getColumnGap() {
			return columnGap ;
		}
		
		/////////////////////////////////////////////
		// serializable methods
		private void writeObject(java.io.ObjectOutputStream stream) throws IOException { 
			// write version
			stream.writeInt(VERSION) ;
			
			// write super class
			stream.writeInt(getPseudorandom()) ;
			stream.writeLong(getRowsTransferred()) ;
			stream.writeLong(getRowsGenerated()) ;
			stream.writeInt(getColumnGap()) ;
			
			stream.writeDouble(displacement) ;
			stream.writeDouble(pendingAcceleratedRows) ;
		}
		
		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			@SuppressWarnings("unused")
			int version = stream.readInt() ;
			
			// read superclass
			setPseudorandom(stream.readInt()) ;
			setRowsTransferred(stream.readLong()) ;
			setRowsGenerated(stream.readLong()) ;
			setColumnGap(stream.readInt()) ;
			
			displacement = stream.readDouble() ;
			pendingAcceleratedRows = stream.readDouble() ;
		}
		
		@SuppressWarnings("unused")
		private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
			throw new ClassNotFoundException("Stream does not match required system state structure.") ;
		}
		
	}

}
