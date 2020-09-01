package com.peaceray.quantro.model.systems.displacement;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.utils.Function;


/**
 * An AbsoluteTimeBasedDisplacementSystem creates displacement in a 
 * different manner than RealTimeDS.
 * 
 * This DisplacementSystem calculates an explicit displacement at any given
 * "in-game" time.  We count up ticks as they occur, adding the time between
 * to a total time passed.  Displacement is thus, in the simplest sense,
 * measured as an absolute function output from this number.
 * 
 * For example, we might define a quadratic function resulting in a gradual
 * and accelerating increase of displacement over time.  This necessarily 
 * results in an inevitable "lose state" where the player is unable
 * to keep up with displacement, no matter their skill.  The challenge thus
 * comes from lasting as long as you can.
 * 
 * On the other hand, this system can be configured with a "plateauing" function
 * that allows a maximum speed and arbitrary acceleration.
 * 
 * The main deal here is that this displacement system ignores level and focuses
 * purely on the absolute time passed since the beginning of the game.
 * 
 * However, it still accelerates.  This is a bit of a complication because
 * we need to remember the number of "accelerated" and "decelerated" rows,
 * as an absolute difference from the easily-calculable "current expected
 * displacement".
 * 
 * Assume, for the moment, that we have a function giving an absolute number
 * of displacement rows at an absolute time 't' from the start of the game.  This
 * function begins at 0 and does not decrease as 't' increases.  How do
 * we "temporarily accelerate" this function?  Here are the options:
 * 
 * Another function which has its value added to the first.  We tick a
 * different variable, t_accel, until this other function has contributed
 * enough rows.  PROBLEM: we will probably overshoot the expected number
 * of rows, and without an inverse, we can't know exactly how far to back up.
 * 
 * The time itself is accelerated, say doubling in speed until all accel.
 * rows are expressed.  PROBLEM: if the function has a "lose state," we
 * have decreased the time to reach it rather than just adding new rows.
 * 
 * A fixed-value acceleration, e.g. "rows per second," that is added on
 * a per-tick basis.  PROBLEM: not versatile.  Still, it may be our best
 * bet, as it is the most straightforward and easiest to implement.  We can
 * include a "calculated displacement delta" in our state that is applied
 * every time displacement is queried.
 * 
 * @author Jake
 *
 */
public class AbsoluteTimeDisplacementSystem extends
		PrefillingDisplacementSystem {
	
	
	boolean configured ;
	
	Function [] mDisplacementRowsAtSeconds_byDifficulty ;
	Function mDisplacementRowsAtSeconds ;
	
	// A velocity offset (added to current speed) when accelerating.
	double mDisplacementAccelRPS ;
	
	// A velocity offset (positive value subtracted from current speed) when
	// decelerating.
	double mDisplacementDecelRPS ;
	
	// Seconds we add to our starting time.  This does NOT change our starting
	// displacement (we offset to keep our starting displacement the same),
	// but it DOES affect speed if our displacement function is not constant.
	double mHeadStart ;
	
	// Constructerrr
	public AbsoluteTimeDisplacementSystem( GameInformation ginfo, QInteractions qi ) {
		super(ginfo, qi) ;
		
		configured = false ;
		setState( new State() ) ;
		
		mDisplacementRowsAtSeconds_byDifficulty = new Function[GameInformation.NUM_DIFFICULTIES] ;
		mDisplacementRowsAtSeconds = null ;
		mDisplacementAccelRPS = 0 ;
		mDisplacementDecelRPS = 0 ;
	}

	
	public AbsoluteTimeDisplacementSystem setDisplacementAtSeconds( int difficulty, Function das ) {
		if ( configured )
			throw new IllegalStateException("Do this before finalizeConfiguration") ;
		mDisplacementRowsAtSeconds_byDifficulty[difficulty] = das ;
		return this ;
	}
	
	/**
	 * Sets the normal difficulty acceleration function.
	 * @param das
	 * @return
	 */
	public AbsoluteTimeDisplacementSystem setDisplacementAtSeconds( Function das ) {
		setDisplacementAtSeconds( GameInformation.DIFFICULTY_NORMAL, das ) ;
		return this ;
	}
	
	
	
	public AbsoluteTimeDisplacementSystem setDisplacementAccelRowsPerSecond( double rps ) {
		if ( configured )
			throw new IllegalStateException("Do this before finalizeConfiguration") ;
		mDisplacementAccelRPS = rps ;
		return this ;
	}
	
	public AbsoluteTimeDisplacementSystem setDisplacementDecelRowsPerSecond( double rps ) {
		if ( configured )
			throw new IllegalStateException("Do this before finalizeConfiguration") ;
		mDisplacementDecelRPS = rps ;
		return this ;
	}
	
	public AbsoluteTimeDisplacementSystem setHeadStart( double seconds ) {
		if ( configured )
			throw new IllegalStateException("Do this before finalizeConfiguration") ;
		mHeadStart = seconds ;
		return this ;
	}
	
	
	private double calculateTotalDisplacement( State state ) {
		GameInformation ginfo = getGameInformation() ;
		double fixedRate = ginfo.displacementFixedRate ;
		
		if ( fixedRate <= 0 ) {
			return mDisplacementRowsAtSeconds.at(state.mSeconds) + state.mDisplacementOffset ;
		} else {
			return fixedRate * state.mSeconds + state.mDisplacementOffset ;
		}
	}

	@Override
	public boolean tick(double seconds) {
		if ( !configured )
			throw new IllegalStateException("Do this after finalizeConfiguration") ;
		
		// ticking adds to our current state's 'tick'.  However, if we're
		// accelerating (or decelerating) we also need to adjust our Offset.
		
		State state = (State)getState() ;
		
		double prevDisplacement = calculateTotalDisplacement( state ) ;
		state.mSeconds += seconds ;
		double nextDisplacement = calculateTotalDisplacement( state ) ;
		
		// apply acceleration / deceleration to the state.  If fixed rate,
		// we don't allow acceleration or slowing.
		if ( getGameInformation().displacementFixedRate <= 0 ) {
			if ( state.mPendingAcceleratedRows > 0 ) {
				double additionalDisplacement = seconds * mDisplacementAccelRPS ;
				if ( additionalDisplacement > state.mPendingAcceleratedRows ) {
					additionalDisplacement = state.mPendingAcceleratedRows ;
				}
				
				state.mDisplacementOffset += additionalDisplacement ;
				state.mPendingAcceleratedRows -= additionalDisplacement ;
			} else {
				double subtractedDisplacement = seconds * mDisplacementDecelRPS ;
				// can't decel more than the pending rows...
				if ( subtractedDisplacement > -state.mPendingAcceleratedRows ) {
					subtractedDisplacement = -state.mPendingAcceleratedRows ;
				}
				// can't decel below 0...
				if ( subtractedDisplacement > (nextDisplacement - prevDisplacement) ) {
					subtractedDisplacement = (nextDisplacement - prevDisplacement) ;
				}
				
				state.mDisplacementOffset -= subtractedDisplacement ;
				state.mPendingAcceleratedRows += subtractedDisplacement ;
			}
		}
		
		// having adjusted the state for acceleration, recalc displacement and return.
		double displacement = calculateTotalDisplacement( state ) ;
		return prevDisplacement != displacement ;
	}

	@Override
	public void transferDisplacedRows(byte[][][] blockfield,
			byte[][][] displacementRows, int rows) {
		
		if ( !configured )
			throw new IllegalStateException("Do this after finalizeConfiguration") ;
		
		
		super.transfer(blockfield, displacementRows, rows) ;
	}

	@Override
	public boolean accelerateDisplacement(double rows) {
		if ( !configured )
			throw new IllegalStateException("Do this after finalizeConfiguration") ;
		
		State state = (State)getState() ;
		state.mPendingAcceleratedRows += rows ;
		return rows != 0 ;
	}

	@Override
	public boolean displaces() {
		return true ;
	}

	@Override
	public double getDisplacedRows() {
		if ( !configured )
			throw new IllegalStateException("Do this after finalizeConfiguration") ;
		
		State state = (State)getState() ;
		return calculateTotalDisplacement( state ) - state.getRowsTransferred() ;
	}

	@Override
	public double getDisplacedAndTransferredRows() {
		if ( !configured )
			throw new IllegalStateException("Do this after finalizeConfiguration") ;
		
		return calculateTotalDisplacement( (State)getState() ) ;
	}
	
	@Override
	public double getDisplacementSeconds() {
		if ( !configured )
			throw new IllegalStateException("Do this after finalizeConfiguration") ;
		
		State state = (State)getState() ;
		return state.mSeconds ;
	}

	@Override
	public void setDisplacementSecondsAndDisplacedAndTransferredRows(double seconds, double rows) {
		if ( !configured )
			throw new IllegalStateException("Do this after finalizeConfiguration") ;
		
		State state = (State)getState() ;
		state.mSeconds = seconds ;
		
		// We can't alter the number of rows generated or transferred
		// (those are "history" variables), nor can we change the output
		// of our displacement function at the provided time.  All we
		// can do is adjust the displacement offset (which is included
		// in the output of getDisplacedAndTransferredRows()) so that
		// the output matches.
		double rowsLocal = getDisplacedAndTransferredRows() ;
		state.mDisplacementOffset += rows - rowsLocal ;
	}
	
	

	@Override
	public SerializableState finalizeConfiguration()
			throws IllegalStateException {
	
		// What's our displacement function?
		mDisplacementRowsAtSeconds = mDisplacementRowsAtSeconds_byDifficulty[getGameInformation().difficulty] ;
		if ( mDisplacementRowsAtSeconds == null ) {
			mDisplacementRowsAtSeconds = mDisplacementRowsAtSeconds_byDifficulty[GameInformation.DIFFICULTY_NORMAL] ;
		}
			
		// Check the configuration for errors.
		if ( mDisplacementRowsAtSeconds == null ) {
			throw new IllegalStateException("Must specify a DisplacementRowsAtSeconds function.") ;
		}
		if ( mDisplacementRowsAtSeconds.at(0) < 0 ) {
			throw new IllegalStateException("DisplacementRowsAtSeconds function must be >= 0 at time 0") ;
		}
		if ( mDisplacementAccelRPS < 0 || mDisplacementDecelRPS < 0 ) {
			throw new IllegalStateException("Displacement Accel / Decel must be nonnegative.") ;
		}
		
		// fast forward!  Set our seconds to the head start
		// start, and set the offset to compensate and get the same
		// row displacement as at 0.
		State state = (State)getState() ;
		state.mSeconds += mHeadStart ;
		state.mDisplacementOffset
				= mDisplacementRowsAtSeconds.at(0) - mDisplacementRowsAtSeconds.at(state.mSeconds) ;
		
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
	public AbsoluteTimeDisplacementSystem setStateAsSerializable( Serializable in ) throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		if ( in != null && !(in instanceof State) ) {
			throw new IllegalArgumentException("Provided Serializable is not the right state!") ;
		}
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
	public AbsoluteTimeDisplacementSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		Object in = inStream.readObject() ;
		if ( in != null && !(in instanceof State) ) {
			throw new IllegalArgumentException("Provided Serializable is not the right state!") ;
		}
		setState( (State)in ) ;
		return this ;
	}
	
	private class State implements PrefillingDisplacementSystem.State, Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3401472400141081430L;

		private static final int VERSION = 0 ;
		// 0: Initial version.
		
		private int pseudorandom ;
		private long rowsTransferred ;
		private long rowsGenerated ;			// total number of rows generated by 'fill' and 'displace'
		private int columnGap ;
		
		
		private double mSeconds ;			// total current seconds
		private double mDisplacementOffset ;	// our displacement is measured
												// as the output of our displacement
												// function.  This might "drift" from
												// correct if we have a 
		
		private double mPendingAcceleratedRows ;
		
		public State() {
			pseudorandom = 0 ;
			rowsTransferred = 0 ;
			rowsGenerated = 0 ;
			columnGap = -1 ;
			
			mSeconds = 0 ;
			mDisplacementOffset = 0 ;
			mPendingAcceleratedRows = 0 ;
		}
		
		
		public State( State state ) {
			pseudorandom = state.pseudorandom ;
			rowsTransferred = state.rowsTransferred ;
			rowsGenerated = state.rowsGenerated ;
			columnGap = state.columnGap ;
			
			mSeconds = state.mSeconds ;
			mDisplacementOffset = state.mDisplacementOffset ;
			mPendingAcceleratedRows = state.mPendingAcceleratedRows ;
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
			
			stream.writeDouble(mSeconds) ;
			stream.writeDouble(mDisplacementOffset) ;
			stream.writeDouble(mPendingAcceleratedRows) ;
			
		}
		
		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			@SuppressWarnings("unused")
			int version = stream.readInt() ;
			
			// read superclass
			setPseudorandom(stream.readInt()) ;
			setRowsTransferred(stream.readLong()) ;
			setRowsGenerated(stream.readLong()) ;
			setColumnGap(stream.readInt()) ;
			
			mSeconds = stream.readDouble() ;
			mDisplacementOffset = stream.readDouble() ;
			mPendingAcceleratedRows = stream.readDouble() ;
		}
		
		@SuppressWarnings("unused")
		private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
			throw new ClassNotFoundException("Stream does not match required system state structure.") ;
		}
		
	}

}
