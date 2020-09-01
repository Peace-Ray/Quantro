package com.peaceray.quantro.model.systems.clear;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;

/**
 * The QuantroClearSystem handles clears in a relatively straightforward way,
 * when compared against EarlyClearSystem.  The primary difference is in how
 * it registers monochromatic clears; whereas EarlyClearSystem always marked
 * monochromatic clears and hoped that the Game knew when to use it, this
 * clear system requires the presence of a SL_ACTIVE block in a row to
 * perform a monochromatic clear.
 * @author Jake
 *
 */
public class QuantroClearSystem extends ClearSystem {

	@SuppressWarnings("unused")
	private static final String TAG = "EarlyClearSystem" ;
	
	GameInformation ginfo ;
	QInteractions qi ;
	
	private boolean configured ;
	private EmptyState state ;
	
	
	// Constructerrr
	public QuantroClearSystem( GameInformation ginfo, QInteractions qi ) {
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
	
	
	////////////////////////////////////////////////////////////////
	//
	// STATEFUL METHODS
	//
	// Although the EarlyClearSystem does not maintain a state,
	// these methods are considered "stateful" under the specification
	// given by SerializableState.
	//
	////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////
	// CHROMATIC CLEARS
	
	/**
	 * clearable: sets the entries in 'clearableArray' to the type
	 * of clear which is appropriate for each row of field.  Returns
	 * 'true' if any row is set to something other than NO.  Other options
	 * are S0, S1 and SL.
	 * 
	 * PRECONDITION:
	 * 		'clearableArray' has length equal to field[0].length
	 * 		field is [2][*][*] of QOrientations.
	 * 
	 * POSTCONDITION:
	 * 		the elements of clearableArray are set to the appropriate type
	 * 				of clear for the row in field.  Available types are:
	 * 				NO:		Clear neither pane
	 * 				S0:		Clear pane 0, not 1
	 * 				S1:		Clear pane 1, not 0
	 * 				SL:		Clear both panes
	 * 
	 * @param field				A block field to be cleared (eventually)
	 * @param clearableArray	An array with length = field[0].length.
	 * @return					Whether any entry in clearableArray was set to non-NO.
	 */
	public final boolean clearable( byte [][][] field, int [] clearableArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		// For each row in clearableArray, set it to the value
		// returned by clearable( field, row ).
		int R = field[0].length ;
		int clearType ;
		boolean any = false ;
		for ( int r = 0; r < R; r++ ) {
			clearType = clearable( field, r ) ;
			clearableArray[r] = clearType ;
			any = any || clearType != QOrientations.NO ;
		}
		
		return any ;
	}
	
	/**
	 * clearable: returns the appropriate type of clear for the given 
	 * row in field, where row is [][row][].  Available types are:
	 * 				NO:		Clear neither pane
	 * 				S0:		Clear pane 0, not 1
	 * 				S1:		Clear pane 1, not 0
	 * 				SL:		Clear both panes
	 * 
	 * @param field			A block field to be cleared (eventually)
	 * @param row			The row in block field to investigate
	 * @return				The type of clear appropriate for the row
	 */
	public final int clearable( byte [][][] field, int row ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		boolean S0clear = true ;
		boolean S1clear = true ;
		boolean SLclear = true ;
		
		// We check SL as a separate condition from S0 and S1
		// (rather than as their conjunction).  This is important
		// for cases where, e.g. the proposed "obsidian" blocks,
		// something can be cleared by both but NOT by either S0 or S1
		// by themselves.
		int C = field[0][0].length ;
		for ( int c = 0; c < C; c++ ) {
			S0clear = S0clear && qi.contributesToClear(field, row, c, QOrientations.S0) ;
			S1clear = S1clear && qi.contributesToClear(field, row, c, QOrientations.S1) ;
			SLclear = SLclear && qi.contributesToClear(field, row, c, QOrientations.SL) ;
		}
		
		if ( SLclear )
			return QOrientations.SL ;
		if ( S0clear )
			return QOrientations.S0 ;
		if ( S1clear )
			return QOrientations.S1 ;
		
		return QOrientations.NO ;
	}
	
	/**
	 * clear: performs all the clears listed in clearableArray, i.e.
	 * replaces the appropriate entries in 'field' by NO.
	 * 
	 * PRECONDITION:
	 * 		clearableArray is as set by 'clearable'.
	 * 
	 * POSTCONDITION
	 * 		the appropriate blocks in 'field' have been replaced by NO
	 * 
	 * @param field				A blockfield to clear
	 * @param clearableArray	Indicates how to clear each row
	 */
	public final void clear( byte [][][] field, int [] clearableArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// We offload the effort here to clear( field, row, clearType ) 
		int R = field[0].length ;
		for ( int r = 0; r < R; r++ ) {
			// TODO: We assume that no blocks are cleared by
			// QOrientations.NO.  If that assumption fails, remove
			// this check.
			if ( clearableArray[r] == QOrientations.NO )
				continue ;
			clear( field, r, clearableArray[r] ) ;
		}
		
		// Print it!
		//Log.d(TAG, Game.arrayAsString(field) + "\n\n") ;
	}
	
	/**
	 * clear: performs all clear specified by row, clearType:
	 * i.e., replaces the appropriate blocks in field[*][row][*]
	 * by NO according to clearType, which is as returned by 'clearable'.
	 * 
	 * PRECONDITION:
	 * 		clearType is as set by 'clearable' for the given row
	 * 
	 * POSTCONDITION
	 * 		the appropriate blocks in 'field' have been replaced by NO
	 * 
	 * @param field				A blockfield to clear
	 * @param row				Indexes a particular row in field
	 * @param clearType			Which type of clear to employ?
	 */
	public void clear( byte [][][] field, int row, int clearType ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// QInteractions defines how the specified type of clear
		// affects the blocks.
		
		int C ;
		
		// TODO: We assume that QOrientations.NO clears absolutely
		// nothing.  Revise this function if that assumption fails.
		if ( clearType != QOrientations.NO ) {
			C = field[0][0].length ;
			for ( int c = 0; c < C; c++ ) {
				qi.clearWith( field, row, c, clearType ) ;
			}
		}
	}
	
	
	
	/**
	 * inverseClear: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param clearableArray
	 */
	public void inverseClear( byte [][][] field, int [] clearableArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		int R = field[0].length ;
		for ( int r = 0; r < R; r++ ) {
			// TODO: We assume that no blocks are cleared by
			// QOrientations.NO.  If that assumption fails, remove
			// this check.
			if ( clearableArray[r] == QOrientations.NO )
				continue ;
			inverseClear( field, r, clearableArray[r] ) ;
		}
	}
	
	/**
	 * inverseClear: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param clearableArray
	 */
	public void inverseClear( byte [][][] field, int row, int clearType ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		int C ;
		
		C = field[0][0].length ;
		for ( int c = 0; c < C; c++ ) {
			qi.inverseClearWith( field, row, c, clearType ) ;
		}
	}
	
	
	
	
	////////////////////////////////////////////////////////////////
	// MONOCHROME CLEARS
	
	/**
	 * clearableMonochrome: sets the entries in 'clearableArray' to true
	 * if a monochromatic clear is appropriate for the equivalent row
	 * in field.
	 * 
	 * PRECONDITION:
	 * 		'clearableArray' has length equal to field[0].length
	 * 		field is [2][*][*] of QOrientations.
	 * 
	 * POSTCONDITION:
	 * 		the elements of clearableArray are set according to whether
	 * 				a monochromatic clear is appropriate for the row.
	 * 
	 * @param field				A block field to be cleared (eventually)
	 * @param clearableArray	An array with length = field[0].length.
	 * @return					Whether any entry in clearableArray was set to true.
	 */
	public boolean clearableMonochrome( byte [][][] field, boolean [] clearableArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// For each row in clearableArray, set it to the value
		// returned by clearableMonochrome( field, row ).
		int R = field[0].length ;
		boolean shouldClear ;
		boolean any = false ;
		for ( int r = 0; r < R; r++ ) {
			shouldClear = clearableMonochrome( field, r ) ;
			clearableArray[r] = shouldClear ;
			any = any || shouldClear ;
		}
		
		return any ;
	}
	
	/**
	 * clearableMonochrome: returns whether the indicated row is susceptible
	 * to a monochromatic clear.
	 * 
	 * @param field				A block field to be cleared (eventually)
	 * @param row				Which row should be investigated
	 * @return					Whether the row is (monochromatically) clearable
	 */
	public boolean clearableMonochrome( byte [][][] field, int row ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		boolean monoActivated = false ;
		
		int C = field[0][0].length ;
		for ( int c = 0; c < C; c++ ) {
			// Contributes checks whether ANY clear is allowed.
			// If not, clearMonochrome fails, since there is a gap.
			if ( !qi.contributesToClear(field, row, c) )
				return false ;
			monoActivated = monoActivated || field[0][row][c] == QOrientations.SL_ACTIVE ;
		}
		return monoActivated ;
	}
	
	/**
	 * clearMonochrome: performs a monochromatic clear on the rows indicated
	 * by clearableArray
	 * 
	 * PRECONDITION:
	 * 		'clearableArray' is as set by clearableMonochrome
	 * 
	 * POSTCONDITION:
	 * 		where 'clearableArray' is True, the corresponding row in
	 * 				field has been cleared.
	 * 
	 * @param field				A block field to be cleared
	 * @param clearableArray	Boolean array indicating which rows to clear.
	 */
	public void clearMonochrome( byte [][][] field, boolean [] clearableArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// A monochrome clear is simple: sweep through as if clearing with
		// QOrientations.SL.  Here, though, we offload the work to the row
		// method.
		int R = field[0].length ;
		for ( int r = 0; r < R; r++ ) {
			if ( clearableArray[r] )
				clearMonochrome( field, r, true ) ;
		}
	}
	
	/**
	 * clearMonochrome: performs a monochromatic clear on the given row, if
	 * shouldClear is true.
	 * 
	 * @param field				A block field to be cleared
	 * @param row				The row to clear
	 * @param shouldClear		Should we actually perform a monochrome clear?
	 */
	public void clearMonochrome( byte [][][] field, int row, boolean shouldClear ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		int C = field[0][0].length ;
		for ( int c = 0; c < C; c++ ) {
			// A monochromatic clear should remove EVERYTHING that
			// can be cleared by any normal type of clear.  As an
			// assumption, do this with QOrientations.SL
			qi.clearWith( field, row, c, QOrientations.SL ) ;
		}
	}
	
	
	/**
	 * inverseClearMonochrome: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param clearableArray
	 */
	public void inverseClearMonochrome( byte [][][] field, boolean [] clearableArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		int R = field[0].length ;
		for ( int r = 0; r < R; r++ ) {
			if ( clearableArray[r] )
				inverseClearMonochrome( field, r, true ) ;
		}
	}
	
	/**
	 * inverseClearMonochrome: keeps only those entries which ARE cleared by the provided
	 * clear array.
	 * @param field
	 * @param row
	 * @param shouldClear
	 */
	public void inverseClearMonochrome( byte [][][] field, int row, boolean shouldClear ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		int C = field[0][0].length ;
		for ( int c = 0; c < C; c++ ) {
			// A monochromatic clear should remove EVERYTHING that
			// can be cleared by any normal type of clear.  As an
			// assumption, do this with QOrientations.SL
			qi.inverseClearWith( field, row, c, QOrientations.SL ) ;
		}
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
	public QuantroClearSystem finalizeConfiguration() throws IllegalStateException {
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
	public QuantroClearSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}
	
}
