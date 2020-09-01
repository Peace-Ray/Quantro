package com.peaceray.quantro.model.systems.level;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.utils.simulatedarray.SimulatedArray;

public class CustomLevelSystem extends LevelSystem {
	
	private static class State implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7194877521905325246L;
		
		private int offset_ginfo_s0clearsSinceLevel ;
		private int offset_ginfo_s1clearsSinceLevel ;
		private int offset_ginfo_sLclearsSinceLevel ;
		private int offset_ginfo_moclearsSinceLevel ;
		
		private boolean waitingForPieceAfterLevelUp ;
		
		private State() {
			offset_ginfo_s0clearsSinceLevel = 0 ;
			offset_ginfo_s1clearsSinceLevel = 0 ;
			offset_ginfo_sLclearsSinceLevel = 0 ;
			offset_ginfo_moclearsSinceLevel = 0 ;
			
			waitingForPieceAfterLevelUp = false ;
		}
		
		private State( State s ) {
			offset_ginfo_s0clearsSinceLevel = s.offset_ginfo_s0clearsSinceLevel ;
			offset_ginfo_s1clearsSinceLevel = s.offset_ginfo_s1clearsSinceLevel ;
			offset_ginfo_sLclearsSinceLevel = s.offset_ginfo_sLclearsSinceLevel ;
			offset_ginfo_moclearsSinceLevel = s.offset_ginfo_moclearsSinceLevel ;
			
			waitingForPieceAfterLevelUp = s.waitingForPieceAfterLevelUp ;
		}
		
		/////////////////////////////////////////////
		// serializable methods
		private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
			stream.writeInt( offset_ginfo_s0clearsSinceLevel ) ;
			stream.writeInt( offset_ginfo_s1clearsSinceLevel ) ;
			stream.writeInt( offset_ginfo_sLclearsSinceLevel ) ;
			stream.writeInt( offset_ginfo_moclearsSinceLevel ) ;
		}
		
		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			offset_ginfo_s0clearsSinceLevel = stream.readInt() ;
			offset_ginfo_s1clearsSinceLevel = stream.readInt()  ;
			offset_ginfo_sLclearsSinceLevel = stream.readInt()  ;
			offset_ginfo_moclearsSinceLevel = stream.readInt()  ;
		}
		
		@SuppressWarnings("unused")
		private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
			throw new ClassNotFoundException("Stream does not match required system state structure.") ;
		}
	}

	@SuppressWarnings("unused")
	private static final String TAG = "CustomLevelSystem" ;
	
	// Information we have and/or need
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	// Here's our configuration information.
	private int maxLevel ;
	private SimulatedArray [][] clearCountNeeded ;
			// Indexed by TYPE, SUBTYPE, current level.  Gives the # of clears necessary,
			// at provided level, before a level-up.
	private SimulatedArray [][][] initial_clearCountNeeded ;
			// Indexed by i, TYPE, SUBTYPE, current level, where 'i' is the number
			// of level gains that already occurred (not necessarily = to level-1, since
			// player may have started at a high level).  If initial_clearCountNeeded.length == null,
			// or i >= initial_clearCountNeeded.length, use clearCountNeeded instead.
	
	
	// This level system does not maintain a "state": behavior configuration
	// is set above, and specific behavior is determined by the GameInformation object.
	// Nevertheless, for completion's sake, we implement an empty state object.
	private State state ;
	private boolean configured ;
	
	
	/**
	 * Constructs a new CustomLevelSystem with the provided information.
	 * @param ginfo The GameInformation object
	 * @param qi The QInteractions object (ignored for this class, but subclasses might use it)
	 * @param initialClearTiers The number of initial level-ups that use special clear information.
	 * 			If the number of previous level-gains is irrelevant - i.e., if going from
	 * 			level k to level k+1 requires the same accomplishments whether you started
	 * 			at level 1 or level k - then set this to 0.
	 */
	public CustomLevelSystem( GameInformation ginfo, QInteractions qi, int maxLevel, int initialClearTiers ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		this.maxLevel = maxLevel ;
		clearCountNeeded = new SimulatedArray[LevelSystem.NUM_TYPES][LevelSystem.NUM_SUBTYPES] ;
		if ( initialClearTiers == 0 )
			initial_clearCountNeeded = null ;
		else
			initial_clearCountNeeded = new SimulatedArray[initialClearTiers][LevelSystem.NUM_TYPES][LevelSystem.NUM_SUBTYPES] ;
		
		state = new State() ;
		configured = false ;
	}
	
	
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this ClearSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public GameInformation getGameInformation() {
		return ginfo ;
	}
	
	
	/**
	 * getQInteractions: Clears are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public QInteractions getQInteractions() {
		return this.qi ;
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
	 * For the provided type and subtype, copy the provided array, which is
	 * indexed by current level.  If we have ar[L-1] clears of the provided type
	 * and subtype, then it is time to go from level L to L+1.
	 * 
	 * This behavior will be reflected in levelUps after the first 'initialClearTiers'
	 * gains (remember the constructor?)
	 * 
	 * @param type	One of LevelSystem.TYPE_CLEARS*
	 * @param subtype One of LevelSystem.SUBTYPE_CLEARS*
	 * @param ar An array of ints of length >= the 
	 */
	public void setClearCountArray( int type, int subtype, SimulatedArray sa ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration after finalizeConfiguration()!") ;
		
		clearCountNeeded[type][subtype] = sa ;
	}
	
	
	
	/**
	 * For the provided type and subtype, copy the provided array, which is
	 * indexed by current level.  If we have ar[L-1] clears of the provided type
	 * and subtype, then it is time to go from level L to L+1.
	 * 
	 * This behavior will be reflected only in the "initTier"th levelUp.
	 * e.g., if initTier is 1, and the user began at level 7, this behavior
	 * will be used in moving from level 8 to level 9.
	 * 
	 * @param type	One of LevelSystem.TYPE_CLEARS*
	 * @param subtype One of LevelSystem.SUBTYPE_CLEARS*
	 * @param ar An array of ints of length >= the 
	 */
	public void setInitialClearCountArray( int initTier, int type, int subtype, SimulatedArray sa ) {
		if ( configured )
			throw new IllegalStateException("Cannot change configuration aften finalizeConfiguration()!") ;
		
		initial_clearCountNeeded[initTier][type][subtype] = sa ;
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
	 * Uses the information currently stored in ginfo to determine if
	 * a level gain is appropriate at this moment.
	 * @return
	 */
	public boolean shouldGainLevel() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		boolean levelUp = false ;
		if ( ginfo.level < maxLevel && !ginfo.levelLock ) {
			// Retrieve the appropriate array.
			SimulatedArray[][] clearSA = null ;
			
			if ( initial_clearCountNeeded != null && initial_clearCountNeeded.length > ginfo.numLevelChanges ) {
				//System.err.println("CustomLevelSystem: inial_clearCountNeeded") ;
				clearSA = initial_clearCountNeeded[ginfo.numLevelChanges] ;
			}
			else {
				//System.err.println("CustomLevelSystem: clearCountNeeded") ;
				clearSA = clearCountNeeded ;
			}
			
			// Check available types / subtypes.
			SimulatedArray sa ;
			
			int levelIndex = ginfo.level - 1 ;
			
			if ( clearSA != null ) {
				sa = clearSA[LevelSystem.TYPE_CLEARS][LevelSystem.SUBTYPE_CLEARS_S0] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s0clears ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_s0 levelIndex " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + "compare against " + ginfo.s0clears) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS][LevelSystem.SUBTYPE_CLEARS_S1] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s1clears ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_s1 levelIndex " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + "compare against " + ginfo.s1clears) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS][LevelSystem.SUBTYPE_CLEARS_SS] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.sLclears ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_sL levelIndex " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + "compare against " + ginfo.sLclears) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS][LevelSystem.SUBTYPE_CLEARS_MO] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.moclears ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_mo levelIndex " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + "compare against " + ginfo.moclears) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS][LevelSystem.SUBTYPE_CLEARS_ANY] ;
				levelUp = levelUp || ( sa != null && ( (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s0clears || (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s1clears || (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.sLclears || (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.moclears ) ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_any levelIndex " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex))) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS][LevelSystem.SUBTYPE_CLEARS_SS_AND_MO] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.sLclears + ginfo.moclears ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_ss_and_mo levelIndex " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + (ginfo.sLclears + ginfo.moclears)) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS][LevelSystem.SUBTYPE_CLEARS_TOTAL] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s0clears + ginfo.s1clears + ginfo.sLclears + ginfo.moclears ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears levelIndex " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + (ginfo.s0clears + ginfo.s1clears + ginfo.sLclears + ginfo.moclears)) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS_SINCE_LAST][LevelSystem.SUBTYPE_CLEARS_S0] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s0clearsSinceLevel - state.offset_ginfo_s0clearsSinceLevel ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_s0_since " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + ginfo.s0clearsSinceLevel + ", " + state.offset_ginfo_s0clearsSinceLevel) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS_SINCE_LAST][LevelSystem.SUBTYPE_CLEARS_S1] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s1clearsSinceLevel - state.offset_ginfo_s1clearsSinceLevel ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_s1_since " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + ginfo.s1clearsSinceLevel + ", " + state.offset_ginfo_s1clearsSinceLevel) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS_SINCE_LAST][LevelSystem.SUBTYPE_CLEARS_SS] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.sLclearsSinceLevel - state.offset_ginfo_sLclearsSinceLevel ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_sL_since " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + ginfo.sLclearsSinceLevel + ", " + state.offset_ginfo_sLclearsSinceLevel) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS_SINCE_LAST][LevelSystem.SUBTYPE_CLEARS_MO] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.moclearsSinceLevel - state.offset_ginfo_moclearsSinceLevel ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_mo_since " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + ginfo.moclearsSinceLevel + ", " + state.offset_ginfo_moclearsSinceLevel) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS_SINCE_LAST][LevelSystem.SUBTYPE_CLEARS_ANY] ;
				levelUp = levelUp || ( sa != null && ( (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s0clearsSinceLevel - state.offset_ginfo_s0clearsSinceLevel || (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s1clearsSinceLevel - state.offset_ginfo_s1clearsSinceLevel || (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.sLclearsSinceLevel - state.offset_ginfo_sLclearsSinceLevel || (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.moclearsSinceLevel - state.offset_ginfo_moclearsSinceLevel ) ) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_any_since " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex))) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS_SINCE_LAST][LevelSystem.SUBTYPE_CLEARS_SS_AND_MO] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.sLclearsSinceLevel + ginfo.moclearsSinceLevel - state.offset_ginfo_sLclearsSinceLevel - state.offset_ginfo_moclearsSinceLevel ) ;		// safe to do this subtraction (it won't push things negative) because at most, offset_** is == to the value it is subtracted from
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_ss_and_mo_since " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + ginfo.sLclearsSinceLevel +", "+ ginfo.moclearsSinceLevel +", "+ state.offset_ginfo_sLclearsSinceLevel +", "+ state.offset_ginfo_moclearsSinceLevel) ;
				
				sa = clearSA[LevelSystem.TYPE_CLEARS_SINCE_LAST][LevelSystem.SUBTYPE_CLEARS_TOTAL] ;
				levelUp = levelUp || ( sa != null && (sa == null ? -1 : sa.getInt(levelIndex)) <= ginfo.s0clearsSinceLevel + ginfo.s1clearsSinceLevel + ginfo.sLclearsSinceLevel + ginfo.moclearsSinceLevel - state.offset_ginfo_s0clearsSinceLevel - state.offset_ginfo_s1clearsSinceLevel - state.offset_ginfo_sLclearsSinceLevel - state.offset_ginfo_moclearsSinceLevel) ;
				//System.err.println("levelUp now " + levelUp) ;
				//System.err.println("CustomLevelSystem: clears_total_since " + levelIndex + " has val " + (sa == null ? -1 : sa.getInt(levelIndex)) + " compare against " + ginfo.s0clearsSinceLevel +", "+ ginfo.s1clearsSinceLevel + ", " + ginfo.sLclearsSinceLevel + ", " + ginfo.moclearsSinceLevel + ", "+ state.offset_ginfo_s0clearsSinceLevel + ", "+ state.offset_ginfo_s1clearsSinceLevel+ ", "+ state.offset_ginfo_sLclearsSinceLevel +", "+ state.offset_ginfo_moclearsSinceLevel) ;
				
			}
		}
		
		return levelUp ;
	}
	
	
	/**
	 * The method "shouldGainLevel" is meant to be called repeatedly, with no
	 * change to any game element.  This method, on the other hand, notifies the
	 * LevelSystem that a level has just been gained.
	 */
	public void didGainLevel() {
		// awesome.
		state.waitingForPieceAfterLevelUp = true ;
	}
	
	
	/**
	 * Notifies the LevelSystem that a piece just entered.
	 * 
	 * This may or may not be significant.  For example, a LevelSystem may
	 * want to ignore some specific Level-Up criteria if they occur between 
	 * gaining a level (see 'didGainLevel') and the next piece appearing.
	 * 
	 * To be more specific, Progression mode features level gains after
	 * a fixed number of "clears since last level up."  It also introduces
	 * garbage row(s) in the next PREPARING state after a level up.  These
	 * garbage rows might cause a clear cascade, resulting in clears being
	 * collected towards the NEXT level-up before the PREVIOUS level-up
	 * is even indicated to the player!  This is BAD.
	 * 
	 * Therefore, such a LevelSystem may want to follow this procedure:
	 * 
	 * didGainLevel()
	 * 		set justGainedLevel=true
	 * 
	 * didEnterPiece()
	 * 		if "justGainedLevel"
	 * 			set offset = ginfo.clearsSinceLevelUp
	 * 			set justGainedLevel=false
	 * 
	 * shouldGainLevel()
	 * 		return levelsNeeded <= ginfo.clearsSinceLevelUp - offset
	 * 
	 * @param piece
	 */
	public void didEnterPiece() {
		if ( state.waitingForPieceAfterLevelUp ) {
			state.offset_ginfo_s0clearsSinceLevel = ginfo.s0clearsSinceLevel ;
			state.offset_ginfo_s1clearsSinceLevel = ginfo.s1clearsSinceLevel ;
			state.offset_ginfo_sLclearsSinceLevel = ginfo.sLclearsSinceLevel ;
			state.offset_ginfo_moclearsSinceLevel = ginfo.moclearsSinceLevel ;
			
			state.waitingForPieceAfterLevelUp = false ;
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
	public CustomLevelSystem finalizeConfiguration() throws IllegalStateException {
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
		return new State( state ) ;
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
		state = (State)in ;
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
	public LevelSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (State)inStream.readObject() ;
		return this ;
	}

}
