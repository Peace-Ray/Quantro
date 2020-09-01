package com.peaceray.quantro.model.systems.score;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.trigger.Triggerable;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.simulatedarray.SimulatedArray;


/**
 * The triggerable level score system applies a standard formula
 * to calculate score for a clear, but applies a level-based
 * multiplier (defaulting to 100).
 * 
 * Drops get 1 point per row fallen.
 * 
 * There is a base multiplier (stored in GameInformation) that
 * is applied to all points gained.  Triggers allow this multiplier
 * to be increased (by a constant additive factor) or decreased
 * (again, by a constant additive (subtractive) factor) which is
 * set in the constructor for the TriggerableLevelScoreSystem instance.
 * 
 * Additionally, the multiplier will decrease with every piece that enters
 * the game - after a "grace period" of pieces, which resets every time the
 * mulitplier increases.
 * 
 * The minimum value for the multiplier is 1.
 * 
 * 
 * REDESIGN 5/25/2012:
 * 		As a way to produce a more exciting and more generalized score system
 * 		with a gradually increasing difficulty slope (e.g. going from x1 to x2
 * 		is easier than x11 to x12), the following refactor is applied.
 * 
 * 		PARAMS: d (decrement), r (round), m_g( function giving maximum grace for a multiplier)
 * 		INSTANCE VARS: m (current multiplier), g (current grace)
 * 
 * 		TRIGGERS:
 * 			m_prev = m
 * 			m += t_mult, apply an additive multiplier increase
 * 			g = max(0, g) + t_grace, additive grace increase
 * 			g = max(g, m_prev * charity_slope + charity_intercept ), require a substantial grace period for very low multipliers, degrading to 0 minimum.
 * 			g = min( g, m_g(m) ), upper-bound on grace by current multiplier.
 * 
 * 		CLEARS:
 * 			g += |clears|, increase "grace" by 1 per row cleared
 * 			g = min( g, m_g(m) ), upper-bound on grace by current multiplier.
 * 
 * 		PIECE ENTER:
 * 			g = max( 0, g - 1 ), safe decrement grace by 1
 * 			if grace <= 0:
 * 				m -= ceil( m * d / r ) * r
 * 					// decrement multiplier by a value which grows as the multiplier does,
 * 					// rounded to the next highest 'r'th.
 * 				
 * 
 *  // NOTE: for now, we configure m_g as "MG / m", for parameter MG.  This gives a max grace of 1 at m == MG.
 * 
 * 	Some recommended settings.  "Retro" mode is assumed to trigger only upon clears, with "very good"
 * 		play getting 2 clears every 5 pieces (i.e. 0,0,0,0,2), resulting in 2 mult-ups.  Recommended setting is:
 * 		t_mult = 1
 * 		t_grace = 1
 * 		d = t_mult / M		// 'M' a "soft maximum" for the multiplier, e.g. 100
 * 		r = 0.1
 * 		m_g() a nonincreasing function with m_g(M) >= 3, m_g(M') < 3 for some M' > M.
 * 
 * 	This results in "very good" play producing ~stable multiplier of 100.  "perfect" play could
 * 	get slightly higher, but the value will still stabilize at a maximum.  Setting up a long
 * "mult up cascade" before reaching the stable level is possible, which gives a temporarily
 * higher multiplier, but it will drift down to the stable level at the level.
 * 
 * "Quantro" mode is assumed to trigger upon a special piece being used (once per enter), and with
 * 		some special clears.  "Perfect" play basically entails using only ST or UL tetrominoes to
 * 		get 2 row clears every other piece.  This is VERY, VERY difficult to pull off, and the rewards
 * 		are comparably great.  The idea is that multiplier behavior is a SUPERSET of Retros (aside from
 * 		some issues like grace upper-bounding).
 * 
 * 		t_special_* = (1, 0)
 * 		t_clear_* = (1, 1)		// t_mult = 1, t_grace = 1, to match Retro behavior
 * 		d = 1 / M
 * 		r = 0.1
 * 		m_g() a nonincreasing function with m_g(M) = 1.
 * 
 * This result in "near-perfect" play producing a stable multiplier of about 255, with "perfect" play
 * going higher but still upper-bounding.  Again, remember that perfect or near-perfect is MUCH 
 * more difficult than in Retro, both in terms of skill and because it relies on the randomized
 * "special piece" bag to produce consistent output (whereas Retro can rely on the very consistent,
 * set-of-seven output).
 * 
 * Perfect play which does not rely on special pieces (using normal clears only) has an unknown
 * stable level, but which is probably around 100 or 150?
 * 
 * @author Jake
 *
 */
public class TriggerableLevelScoreSystem extends ScoreSystem implements Triggerable {

	//private static final String TAG = "TriggerableLevelScoreSystem" ;
	
	public static final int TRIGGER_MULTIPLIER_DECREASE = 0 ;
		// param: tuple of Doubles.  For now, param[0] is subtractive decrease.
	
	public static final int TRIGGER_MULTIPLIER_INCREASE = 1 ;
		// param: tuple of (Double, Int) = (t_mult, t_grace).
	
	
	// Some storage space for stuff.
	protected GameInformation ginfo ;
	protected QInteractions qi ;

	// CONFIGURATION INFORMATION:
	// Multipliers and additive stuff
	double decrement, round, maxGracedMult ;		// d, r, MG
	double charitySlope, charityIntercept ;
	
	// Level-based clear amount
	SimulatedArray levelClearMultiplier ;
	
	// STATE INFORMATION:
	protected TriggerableLevelScoreSystemState state ;
	private boolean configured ;
	
	// Convenient for preventing re-allocation.
	private boolean [] falseArray ;
	
	
	/**
	 * Constructor for the early score system.
	 * @param ginfo The GameInformation to use.
	 * @param qi The QInteractions to use.
	 */
	public TriggerableLevelScoreSystem( GameInformation ginfo, QInteractions qi ) {
		this(ginfo, qi, 0.01, 0.1, 100, 0, 0) ;
	}
	
	
	public TriggerableLevelScoreSystem( GameInformation ginfo, QInteractions qi, double decrement, double round, double maxGracedMultiplier, double charitySlope, double charityIntercept ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		this.decrement = decrement ;
		this.round = round ;
		this.maxGracedMult = maxGracedMultiplier ;
		
		this.charitySlope = charitySlope ;
		this.charityIntercept = charityIntercept ;
		
		state = new TriggerableLevelScoreSystemState() ;
		configured = false ;
		
		// Just in case
		falseArray = new boolean[30] ;
		for ( int i = 0; i < falseArray.length; i++ ) {
			falseArray[i] = false ;
		}
		
		levelClearMultiplier = null ;
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
	
	
	public void setLevelClearMultiplierArray( SimulatedArray sa ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		this.levelClearMultiplier = sa ;
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

	
	/**
	 * scorePrepare Score for preparing a new piece to enter?
	 * 
	 * @param piece The piece entering
	 * @param offset The offset at which it enters
	 * @return How many points earned for the piece preparing?
	 */
	public int scorePrepare( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Update the current ginfo
		ginfo.addition = 0 ;
		return 0 ;
	}
	
	/**
	 * scoreEnter Score for this piece entering?
	 * 
	 * @param piece The piece entering
	 * @param offset The offset at which it enters
	 * @return How many points earned for the piece entering?
	 */
	public int scoreEnter( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		//System.err.println("scoreEnter: grace is " + state.grace ) ;
		
		state.L = 1 ;
		
		return 0 ;
	}
	
	
	/**
	 * The reserve piece provided was just used.  It was inserted into
	 * the piece queue (not the field).
	 * @param piece
	 * @return
	 */
	public int scoreUseReserve( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// No effect!
		return 0 ;
	}
	
	/**
	 * The piece we specified was just put back in the next list.
	 * @param piece
	 * @return
	 */
	public int scorePutBack( Piece piece) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// No effect!
		return 0 ;
	}
	
	
	/**
	 * The piece specified was put in reserve.  Maybe 
	 * @param piece
	 * @return
	 */
	public int scorePutInReserve( Piece piece ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// No effect!
		return 0 ;
	}
	
	/**
	 * The reserve piece provided was just used.  It replaced a piece already
	 * in the field (and now has the provided offset).
	 * 
	 * @param piece
	 * @param offset
	 * @return
	 */
	public int scoreUseReserve( Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// No effect!
		return 0 ;
	}
	
	
	
	
	
	/**
	 * scoreClear Score for these rows clearing.  Call this if there are
	 * no monochromatic clears associated with this level of cascade.
	 * 
	 * @param chromaticArray Chromatic components of the clear
	 * @return How many points earned for the piece clearing?
	 */
	public int scoreClear( int [] chromaticArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Just in case, extend falseArray if we need to.
		if ( falseArray.length < chromaticArray.length ) {
			falseArray = new boolean[chromaticArray.length] ;
			for ( int i = 0; i < falseArray.length; i++ ) {
				falseArray[i] = false ;
			}
		}
		
		return scoreClear( chromaticArray, falseArray ) ;
	}
	
	/**
	 * scoreClear Score for these rows clearing.  Call this if there are
	 * monochromatic components to the clear.
	 * 
	 * @param chromaticArray Chromatic components of the clear
	 * @param monochromaticArray Monochromatic components of the clear
	 * @return How many points earned for the piece clearing?
	 */
	public int scoreClear( int [] chromaticArray, boolean [] monochromaticArray ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		int n = 0 ;
		boolean s0 = false ;
		boolean s1 = false ;
		
		// 5/25/2012 at 13:37 (seriously).
		// There is a bug in the below; Retro games same to use SL clears,
		// which results in way too many points being awarded for clears.
		// Check the number of qpanes before doing this.  (fixed)
		
		if ( GameModes.numberQPanes(ginfo) == 1 ) {
			// treat every clear as if it is S0 only.
			for ( int i = 0; i < chromaticArray.length; i++ ) {
				if ( monochromaticArray[i] || chromaticArray[i] != QOrientations.NO ) {
					s0 = true ;
					n++ ;
				}
			}
		} else {
			for ( int i = 0; i < chromaticArray.length; i++ ) {
				if ( monochromaticArray[i] ) {
					s0 = true ;
					s1 = true ;
					n += 2 ;
				}
				else {
					if ( chromaticArray[i] == QOrientations.S0 ) {
						s0 = true ;
						n++ ;
					}
					else if ( chromaticArray[i] == QOrientations.S1 ) {
						s1 = true ;
						n++ ;
					}
					else if ( chromaticArray[i] == QOrientations.SL ){
						s0 = s1 = true ;
						n += 2 ;
					}
				}
			}
		}
		
		// we have 'n', the number of clears.  Extend
		// our current grace period (and apply the maximum afterwards).
		addGrace( n ) ;
		boundGrace() ;
		//System.err.println("scoreClear: grace is " + state.grace ) ;
		
			
		// Now calculate score.
		// Apply... the FORMULA.
		double bo = (s0 && s1) ? 1 : 0 ;
		double BO = (s0 && s1) ? 1 : 1 ;
		
		int amount = (levelClearMultiplier == null) ? (int)Math.floor( ((state.L + n + bo)*2 - 3) * BO * 100)
													: (int)Math.floor( ((state.L + n + bo)*2 - 3) * BO * levelClearMultiplier.getInt(ginfo.level-1)) ;
		
		// Cascade!
		state.L++ ;
		
		if ( amount > 0 ) {
			ginfo.score += (int)Math.floor( amount * ginfo.multiplier ) ;
			ginfo.addition = amount ;
			return amount ;
		}
		
		return 0 ;
	}
	
	/**
	 * scoreDrop Score for the drop that just happened
	 * 
	 * @param piece The piece that was dropped
	 * @param oldOffset The old offset, before the drop
	 * @param newOffset The new offset, after the drop
	 * @return How many points earned for the drop?
	 */
	public int scoreDrop( Piece piece, Offset oldOffset, Offset newOffset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		int amount = (int)Math.floor( ( oldOffset.y - newOffset.y ) ) ;
		
		if ( amount > 0 ) {
			ginfo.score += (int)Math.floor( amount * ginfo.multiplier ) ;
			ginfo.addition = amount ;
			return amount ;
		}
		
		return 0 ;
	}
	
	/**
	 * scoreFall Score for the fall that just happened
	 * 
	 * @param piece The piece that just fell
	 * @param oldOffset The old offset, before the fall
	 * @param newOffset The new offset, after the fall
	 * @return How many points earned for the fall?
	 */
	public int scoreFall( Piece piece, Offset oldOffset, Offset newOffset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return 0 ;
	}
	
	/**
	 * scoreLock Score a piece locking in place
	 * 
	 * @param blockField The field the piece is locking to.
	 * @param piece The piece being locked
	 * @param offset The offset at which the piece will be locked
	 * @return How many points??
	 */
	public int scoreLock( byte [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return 0 ;
	}
	
	/**
	 * scoreComponentFall Score for falling a component.  A 'component' is 
	 * a contiguous segment of a piece that fell independently of
	 * the rest of the piece.  As a convention, first "scoreLock" will be
	 * called on the complete piece.  This method will only be called if the
	 * piece separated after that.
	 * 
	 * @param chunk The chunk that fell
	 * @param oldOffset The old offset, before the fall
	 * @param newOffset The new offset, after the fall
	 * @return How many points earned for the fall?
	 */
	public int scoreComponentFall( Piece chunk, Offset oldOffset, Offset newOffset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// A component fall gets you nothing.
		return 0 ;
	}
	
	/**
	 * scoreComponentLock Score a component locking in place.  A 'component' is 
	 * a contiguous segment of a piece that fell independently of
	 * the rest of the piece.  As a convention, first "scoreLock" will be
	 * called on the complete piece.  This method will only be called if the
	 * piece separated into components which locked separately (regardless
	 * of whether they then fell to new depths).
	 * 
	 * @param blockField The field the piece is locking to.
	 * @param component The component being locked
	 * @param offset The offset at which the component will be locked
	 * @return How many points??
	 */
	public int scoreComponentLock( byte [][][] blockField, Piece component, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Locking gets you nothing
		return 0 ;
	}
	
	/**
	 * scoreChunkFall Score for falling a chunk.  A 'chunk' is 
	 * a piece of the block field that was freed by a clear,
	 * or some other effect.
	 * 
	 * @param chunk The chunk that fell
	 * @param oldOffset The old offset, before the fall
	 * @param newOffset The new offset, after the fall
	 * @return How many points earned for the fall?
	 */
	public int scoreChunkFall( Piece chunk, Offset oldOffset, Offset newOffset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Falling gets you nothing
		return 0 ;
	}
	
	
	/**
	 * We are at the top of a new action cycle.
	 * @return
	 */
	public int scoreStartActionCycle() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( state.grace <= 0 ) {
			// if grace <= 0:
			//	m -= ceil( m * d / r ) * r
			// decrement multiplier by a value which grows as the multiplier does,
			// rounded to the next highest 'r'th.
			ginfo.multiplier -= Math.ceil( ginfo.multiplier * decrement / round ) * round ;
			ginfo.multiplier = Math.max(1, ginfo.multiplier) ;
			ginfo.highestMultiplier = Math.max(ginfo.highestMultiplier, ginfo.multiplier) ;
		}
		else
			state.grace-- ;
		
		return 0 ;
	}

	
	@Override
	public void pullTrigger( int triggerNum ) {
		pullTrigger(triggerNum, (Object [])null) ;
	}

	@Override
	public void pullTrigger(int triggerNum, Object... params) {
		
		
		switch ( triggerNum ) {
		case TRIGGER_MULTIPLIER_DECREASE:
			// simple subtractive decrease.
			ginfo.multiplier = Math.max(1, (float)(ginfo.multiplier - (Double)params[0])) ;
			break ;
		
		case TRIGGER_MULTIPLIER_INCREASE:
			//if ( params.length > 0 && params[params.length-1] instanceof String )
			//	System.err.println("TriggerableLevelScoreSystem pullTrigger: " + params[params.length-1] ) ;
			// has 2 params: additive increase and additive (integer) grace.
			float prevMult = ginfo.multiplier ;
			ginfo.multiplier += (Double)params[0] ;
			addGrace( (Integer)params[1] ) ;
			charityGrace(prevMult) ;
		}
		
		
		// cleanup: check the maximum multiplier, apply maximum grace, etc.
		ginfo.highestMultiplier = Math.max(ginfo.highestMultiplier, ginfo.multiplier) ;
		boundGrace() ;		
		
		//System.err.println("pullTrigger: grace is " + state.grace ) ;
	}
	
	
	private void addGrace( int num ) {
		state.grace = Math.max(0, state.grace) + num ;
	}
	
	private void charityGrace( float m ) {
		state.grace = Math.max( state.grace, (int)Math.round( m * charitySlope + charityIntercept )) ;
	}
	
	private void boundGrace() {
		state.grace = (int)Math.floor( Math.min( state.grace, maxGracedMult / ginfo.multiplier ) ) ;
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
	public TriggerableLevelScoreSystem finalizeConfiguration() throws IllegalStateException {
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
		return new TriggerableLevelScoreSystemState( state ) ;
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
		state = (TriggerableLevelScoreSystemState)in ;
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
	public ScoreSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (TriggerableLevelScoreSystemState)inStream.readObject() ;
		return this; 
	}
	
}


class TriggerableLevelScoreSystemState implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4476218433230655754L;
	
	
	// Here are my data members.  I know about "L",
	// which represents the # of cascades (beginning with 1, i.e.,
	// it counts the number of line clears INCLUDING one it
	// anticipates), and "numPiecesEnteredSinceMultiplierIncrease",
	// which is used to count pieces entering since the last
	// time we increased ginfo.multiplier.
	public int L ;
	public int grace ;
	
	
	/////////////////////////////////////////////
	// constructor
	public TriggerableLevelScoreSystemState() {
		L = 1 ;
		grace = 0 ;
	}
	
	public TriggerableLevelScoreSystemState( TriggerableLevelScoreSystemState state ) {
		L = state.L ;
		grace = state.grace ;
	}
	
	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(L) ;
		stream.writeInt(grace) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		L = stream.readInt();
		grace = stream.readInt();
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
}
