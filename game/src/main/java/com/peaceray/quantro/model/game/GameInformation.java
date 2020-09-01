package com.peaceray.quantro.model.game;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ByteArrayOps;

/**
 * GameInformation: stores relevant metadata about the game, including
 * the game mode, current level, current score, number of line clears,
 * etc.
 * 
 * All of this information is relevant when deciding how particular
 * various game components should behave.  For instance, different
 * game modes may use different types or distributions of pieces,
 * pieces may fall faster as the level increases, etc.
 * 
 * The GameInformation class, however, does not make these decisions.
 * It stores information about the current game.
 * 
 * A note on SerializableState: most game objects implementing this
 * interface require explicit configuration, and store their state
 * is a single member field, which itself is Parcelable and Serializable.
 * 
 * Because of the simplicity of GameInformation, there is no "configuration"
 * to do and, in fact, it is its own state (the call to "getStateAsParcelable"
 * returns a reference to the receiver).
 * 
 * You should call "finalizeConfiguration" immediately after construction.
 * 
 * VERSIONING: This class (and GameResult) provide forward-versioning guarantees:
 * any future revision 'r' of this class can read the Serialized data from all instances
 * with version <= r.  We guarantee that the version number (int >= 0)
 * is the first value in a serialization; as a helpful convention, we will try to
 * serialize data from revision 'r' as follows (using s<k> as the serialization,
 * excluding version number, of all the data introduced in version k that was not
 * present in version k-1):
 * 
 * r s<1> true s<2> true ... true s<r-1> true s<r> false.
 * 
 * @author Jake
 *
 */
public class GameInformation implements SerializableState, Serializable {
	
	public static final int VERSION = 3 ;
	// VERSION 1: adds level lock.
	// VERSION 2: adds difficulty and displacementFixedRate
	// VERSION 3: adds 'milliseconds' to represent total time passed in-game ("ticked")
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8601330517856654513L;

	public static final int DIFFICULTY_PRACTICE = 0 ;
	public static final int DIFFICULTY_NORMAL = 1 ;
	public static final int DIFFICULTY_HARD = 2 ;
	
	public static final int NUM_DIFFICULTIES = 3 ;
	
	
	//private static final String TAG = "GameInformation" ;
	
	////////////////////////////////////////////////////////////////////////////
	// IMMUTABLE: These are kept at a specific value for the entire lifetime
	// of the GameInformation object.
	public int mode ;
	public int firstLevel ;		// What level did the user start on?
	public int firstGarbage ;	// How much garbage did we start with?
	public int garbage ;		// how much garbage per level?
	public boolean levelLock ;	// Is this level "locked"?
	
	// For specific game types, we allow levelUps after a customized number 
	// of clears.  Set these here!
	public int s0clearsSinceLevelForLevelUp ;
	public int s1clearsSinceLevelForLevelUp ;
	public int sLclearsSinceLevelForLevelUp ;
	public int moclearsSinceLevelForLevelUp ;
	public int nyclearsSinceLevelForLevelUp ;		// "any" clear type
	public int LmclearsSinceLevelForLevelUp ;		// sL & mo clear type
	public int tlclearsSinceLevelForLevelUp ;		// total clears
	
	// For specific game types, a flood system (displacement) allows outside
	// configuration, in the form of 'head start' and 'per-second.'  'Per-second'
	// is an explicit setting that overrides normal behavior.
	public int difficulty ;						
	public double displacementFixedRate ;			// if <= 0, use standard
	
	
	////////////////////////////////////////////////////////////////////////////
	// MUTABLE: These values change as the game goes on.
	public int level ;
	public int numLevelChanges ;		// How many level-ups did the user have?
	
	public long score ;
	public int addition ;
	public float multiplier ;
	public float highestMultiplier ;
	
	// number of total clears of each type
	public int s0clears ;
	public int s1clears ;
	public int sLclears ;
	public int moclears ;
	
	// longest cascade (uses total clears)
	public int longestCascade ;
	
	// number of clears since the last level gain
	public int s0clearsSinceLevel ;
	public int s1clearsSinceLevel ;
	public int sLclearsSinceLevel ;
	public int moclearsSinceLevel ;
	
	// number of milliseconds this game has been played.
	public long milliseconds ;
	
	private boolean configured ;
	
	/**
	 * Empty constructor.  Be sure to set a state soon after!
	 */
	public GameInformation() {
		this.mode = -1 ;
		this.firstLevel = -1 ;
	}
	
	/**
	 * Constructs a GameInformation object.
	 * 
	 * @param mode		The game mode for this game
	 * @param level		The level to begin on for this game
	 */
	public GameInformation( int mode, int level ) {
		// TODO: Check if game mode is valid
		this.mode = mode ;
		this.firstLevel = level ;
		this.firstGarbage = 0 ;
		this.garbage = 0 ;
		this.levelLock = false ;
		
		this.s0clearsSinceLevelForLevelUp = 0 ;
		this.s1clearsSinceLevelForLevelUp = 0 ;
		this.sLclearsSinceLevelForLevelUp = 0 ;
		this.moclearsSinceLevelForLevelUp = 0 ;
		this.nyclearsSinceLevelForLevelUp = 0 ;
		this.LmclearsSinceLevelForLevelUp = 0 ;
		this.tlclearsSinceLevelForLevelUp = 0 ;
		
		this.difficulty = DIFFICULTY_NORMAL ;
		this.displacementFixedRate = 0.0 ;
		
		this.level = level ;
		this.numLevelChanges = 0 ;
		
		if ( this.level <= 0 )
			throw new IllegalArgumentException("NON-POSITIVE LEVELS ARE NOT ALLOWED") ;
		
		score = 0 ;
		addition = 0 ;
		multiplier = 1.0f ;
		highestMultiplier = 1.0f ;
		s0clears = s0clearsSinceLevel = 0 ;
		s1clears = s1clearsSinceLevel = 0 ;
		sLclears = sLclearsSinceLevel = 0 ;
		moclears = moclearsSinceLevel = 0 ;
		
		longestCascade = 0 ;
		
		milliseconds = 0 ;
		
		configured = false ;
	}
	
	public GameInformation( GameInformation ginfo ) {
		this() ;
		finalizeConfiguration() ;
		takeVals(ginfo) ;
	}
	
	
	/**
	 * Only adjust our 'time' measurements if it moves us further into
	 * the future: advancing time forward.
	 */
	public static final int FLAG_UPDATE_TIME_FORWARD_ONLY = 0x1 ;
	
	public GameInformation takeVals( GameInformation ginfo ) {
		return takeVals( ginfo, 0 ) ;
	}
	
	public GameInformation takeVals( GameInformation ginfo, int flags ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		this.mode = ginfo.mode ;
		this.firstLevel = ginfo.firstLevel ;
		this.firstGarbage = ginfo.firstGarbage ;
		this.garbage = ginfo.garbage ;
		this.levelLock = ginfo.levelLock ;
		
		this.s0clearsSinceLevelForLevelUp = ginfo.s0clearsSinceLevelForLevelUp ;
		this.s1clearsSinceLevelForLevelUp = ginfo.s1clearsSinceLevelForLevelUp ;
		this.sLclearsSinceLevelForLevelUp = ginfo.sLclearsSinceLevelForLevelUp ;
		this.moclearsSinceLevelForLevelUp = ginfo.moclearsSinceLevelForLevelUp ;
		this.nyclearsSinceLevelForLevelUp = ginfo.nyclearsSinceLevelForLevelUp ;
		this.LmclearsSinceLevelForLevelUp = ginfo.LmclearsSinceLevelForLevelUp ;
		this.tlclearsSinceLevelForLevelUp = ginfo.tlclearsSinceLevelForLevelUp ;
		
		this.difficulty = ginfo.difficulty ;
		this.displacementFixedRate = ginfo.displacementFixedRate ;
		
		this.level = ginfo.level ;
		this.numLevelChanges = ginfo.numLevelChanges ;
		
		if ( this.level <= 0 )
			throw new IllegalArgumentException("NON-POSITIVE LEVELS ARE NOT ALLOWED") ;
		
		this.score = ginfo.score ;
		this.addition = ginfo.addition ;
		this.multiplier = ginfo.multiplier ;
		this.highestMultiplier = ginfo.highestMultiplier ;
		
		this.s0clears = ginfo.s0clears ;
		this.s1clears = ginfo.s1clears ;
		this.sLclears = ginfo.sLclears ;
		this.moclears = ginfo.moclears ;
		
		this.longestCascade = ginfo.longestCascade ;
		
		this.s0clearsSinceLevel = ginfo.s0clearsSinceLevel ;
		this.s1clearsSinceLevel = ginfo.s1clearsSinceLevel ;
		this.sLclearsSinceLevel = ginfo.sLclearsSinceLevel ;
		this.moclearsSinceLevel = ginfo.moclearsSinceLevel ;
		
		if ( this.milliseconds < ginfo.milliseconds || ((flags & FLAG_UPDATE_TIME_FORWARD_ONLY) == 0) ) {
			this.milliseconds = ginfo.milliseconds ;
		}
		
		return this ;
	}
	
	public void passTime( long millis ) {
		this.milliseconds += millis ;
	}
	
	public void levelUp() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		this.level++ ;
		this.s0clearsSinceLevel = 0 ;
		this.s1clearsSinceLevel = 0 ;
		this.sLclearsSinceLevel = 0 ;
		this.moclearsSinceLevel = 0 ;
	}
	
	public void recordClears( int [] chromatic, boolean [] monochromatic ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		for ( int i = 0; i < chromatic.length; i++ ) {
			if ( chromatic[i] == QOrientations.S0 ) {
				s0clears++ ;
				s0clearsSinceLevel++ ;
			}
			else if ( chromatic[i] == QOrientations.S1 ) {
				s1clears++ ;
				s1clearsSinceLevel++ ;
			}
			else if ( chromatic[i] == QOrientations.SL ) {
				sLclears++ ;
				sLclearsSinceLevel++ ;
			}
			else if ( monochromatic[i] ) {
				moclears++ ;
				moclearsSinceLevel++ ;
			}
		}
	}
	
	public int getMode() {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return mode ;
	}
	
	public boolean clearsSetForLevelUp() {
		boolean set = false ;
		set = set || s0clearsSinceLevelForLevelUp > 0 ;
		set = set || s1clearsSinceLevelForLevelUp > 0 ;
		set = set || sLclearsSinceLevelForLevelUp > 0 ;
		set = set || moclearsSinceLevelForLevelUp > 0 ;
		set = set || nyclearsSinceLevelForLevelUp > 0 ;
		set = set || LmclearsSinceLevelForLevelUp > 0 ;
		set = set || tlclearsSinceLevelForLevelUp > 0 ;
		return set ;
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// Byte array methods.  For writing to / reading from byte array. 
	//
	////////////////////////////////////////////////////////////////
	
	
	/**
	 * Writes the current contents of this GameInformation object to
	 * the provided byte array, beginning at 'ind'.
	 * 
	 * Returns the number of bytes written.  It is the caller's responsibliity
	 * that the full size of the GameInformation can fit in the
	 * provided byte array (especially within the "valid writen region",
	 * if there is such a thing).  Will throw an exception if it writes past the
	 * end of the array, or up to (including) topBounds.
	 * 
	 * In the event that an exception is thrown, this method has written from 'ind'
	 * to the maximum bound, whether topBounds or b.length.
	 * 
	 * Take care that topBounds is not < ind, or this method may throw an
	 * exception even if the entire length of the byte representation has been
	 * written.
	 * 
	 * @param b An array of bytes
	 * @param ind the first index to write to.
	 * @param topBounds Will treat the array as if this is its length, including throwing
	 * 			IndexOutOfBoundsExceptions.
	 * 
	 * @return The number of bytes written.
	 * 
	 */
	public int writeToByteArray( byte [] b, int ind, int topBounds ) throws IndexOutOfBoundsException {
		int indOrig = ind ;
		
		// Just write everything.
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(mode, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(firstLevel, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(firstGarbage, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(garbage, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			b[ind] = levelLock ? (byte)1 : (byte)0 ;
		ind++ ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(s0clearsSinceLevelForLevelUp, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(s1clearsSinceLevelForLevelUp, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(sLclearsSinceLevelForLevelUp, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(moclearsSinceLevelForLevelUp, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(nyclearsSinceLevelForLevelUp, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(LmclearsSinceLevelForLevelUp, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(tlclearsSinceLevelForLevelUp, b, ind) ;
		ind += 4 ;
		
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(difficulty, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeDoubleAsBytes(displacementFixedRate, b, ind) ;
		ind += 8 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(level, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(numLevelChanges, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeLongAsBytes(score, b, ind) ;
		ind += 8 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(addition, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeFloatAsBytes(multiplier, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeFloatAsBytes(highestMultiplier, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(s0clears, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(s1clears, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(sLclears, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(moclears, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(longestCascade, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(s0clearsSinceLevel, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(s1clearsSinceLevel, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(sLclearsSinceLevel, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeIntAsBytes(moclearsSinceLevel, b, ind) ;
		ind += 4 ;
		
		if ( b != null )
			ByteArrayOps.writeLongAsBytes(milliseconds, b, ind) ;
		ind += 8 ;
		
		if ( b == null )
			ind++ ;
		else
			b[ind++] = (byte)((configured) ? 1 : 0) ;
		
		if ( ind > topBounds )
			throw new IndexOutOfBoundsException("GameInformation.writeToByteArray wrote past the provided top bounds: wrote to from " + indOrig + " to " + ind + ", with top bounds " + topBounds) ;
		
		return ind - indOrig ;
	}
	
	
	/**
	 * Reads the current contents of this GameInformation object to
	 * the provided byte array, beginning at 'ind'.
	 * 
	 * Returns the number of bytes written.  It is the caller's responsibliity
	 * that the full size of the GameInformation can fit in the
	 * provided byte array (especially within the "valid writen region",
	 * if there is such a thing).  Will throw an exception if it writes past the
	 * end of the array, or up to (including) topBounds.
	 * 
	 * In the event that an exception is thrown, this method has written from 'ind'
	 * to the maximum bound, whether topBounds or b.length.
	 * 
	 * Take care that topBounds is not < ind, or this method may throw an
	 * exception even if the entire length of the byte representation has been
	 * written.
	 * 
	 * @param b An array of bytes
	 * @param ind the first index to write to.
	 * 
	 * @return The number of bytes read.
	 * 
	 */
	public int readFromByteArray( byte [] b, int ind ) throws IndexOutOfBoundsException {
		int indOrig = ind ;
		
		// Just write everything.
		mode = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		firstLevel = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		firstGarbage = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		garbage = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		levelLock = b[ind++] == 1 ;
		
		s0clearsSinceLevelForLevelUp = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		s1clearsSinceLevelForLevelUp = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		sLclearsSinceLevelForLevelUp = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		moclearsSinceLevelForLevelUp = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		nyclearsSinceLevelForLevelUp = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		LmclearsSinceLevelForLevelUp = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		tlclearsSinceLevelForLevelUp = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		difficulty = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		displacementFixedRate = ByteArrayOps.readDoubleAsBytes(b, ind) ;
		ind += 8 ;
		
		level = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		numLevelChanges = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		score = ByteArrayOps.readLongAsBytes(b, ind) ;
		ind += 8 ;
		
		addition = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		multiplier = ByteArrayOps.readFloatAsBytes(b, ind) ;
		ind += 4 ;
		
		highestMultiplier = ByteArrayOps.readFloatAsBytes(b, ind) ;
		ind += 4 ;
		
		s0clears = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		s1clears = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		sLclears = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		moclears = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		longestCascade = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		s0clearsSinceLevel = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		s1clearsSinceLevel = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		sLclearsSinceLevel = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		moclearsSinceLevel = ByteArrayOps.readIntAsBytes(b, ind) ;
		ind += 4 ;
		
		milliseconds = ByteArrayOps.readLongAsBytes(b, ind) ;
		ind += 8 ;
		
		configured = b[ind++] == 1 ;
		
		if ( this.level <= 0 )
			throw new IllegalArgumentException("NON-POSITIVE LEVELS ARE NOT ALLOWED") ;
		
		return ind - indOrig ;
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
		// write version
		stream.writeInt(VERSION) ;
		
		stream.writeInt(mode) ;
		stream.writeInt(firstLevel) ;
		stream.writeInt(firstGarbage) ;
		stream.writeInt(garbage) ;
		stream.writeBoolean(levelLock) ;
		
		stream.writeInt(s0clearsSinceLevelForLevelUp) ;
		stream.writeInt(s1clearsSinceLevelForLevelUp) ;
		stream.writeInt(sLclearsSinceLevelForLevelUp) ;
		stream.writeInt(moclearsSinceLevelForLevelUp) ;
		stream.writeInt(nyclearsSinceLevelForLevelUp) ;
		stream.writeInt(LmclearsSinceLevelForLevelUp) ;
		stream.writeInt(tlclearsSinceLevelForLevelUp) ;
		
		stream.writeInt(difficulty) ;
		stream.writeDouble(displacementFixedRate) ;
		
		stream.writeInt(level) ;
		stream.writeInt(numLevelChanges) ;
		
		stream.writeLong(score) ;
		stream.writeInt(addition) ;
		stream.writeFloat(multiplier) ;
		stream.writeFloat(highestMultiplier) ;
		
		stream.writeInt(s0clears) ;
		stream.writeInt(s1clears) ;
		stream.writeInt(sLclears) ;
		stream.writeInt(moclears) ;
		
		stream.writeInt(longestCascade) ;
		
		stream.writeInt(s0clearsSinceLevel) ;
		stream.writeInt(s1clearsSinceLevel) ;
		stream.writeInt(sLclearsSinceLevel) ;
		stream.writeInt(moclearsSinceLevel) ;
		
		stream.writeLong(milliseconds) ;
		
		// write boolean: has more
		stream.writeBoolean(false) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int version = stream.readInt() ;
		
		this.mode = stream.readInt() ;
		this.firstLevel = stream.readInt() ;
		this.firstGarbage = stream.readInt() ;
		this.garbage = stream.readInt() ;
		if ( version >= 1 )
			levelLock = stream.readBoolean() ;
		else
			levelLock = false ;
		
		this.s0clearsSinceLevelForLevelUp = stream.readInt() ;
		this.s1clearsSinceLevelForLevelUp = stream.readInt() ;
		this.sLclearsSinceLevelForLevelUp = stream.readInt() ;
		this.moclearsSinceLevelForLevelUp = stream.readInt() ;
		this.nyclearsSinceLevelForLevelUp = stream.readInt() ;
		this.LmclearsSinceLevelForLevelUp = stream.readInt() ;
		this.tlclearsSinceLevelForLevelUp = stream.readInt() ;
		
		if ( version >= 2 ) {
			this.difficulty = stream.readInt() ;
			this.displacementFixedRate = stream.readDouble() ;
		} else {
			this.difficulty = DIFFICULTY_NORMAL ;
			this.displacementFixedRate = 0.0 ;
		}
		
		this.level = stream.readInt() ;
		this.numLevelChanges = stream.readInt() ; 
		
		if ( this.level <= 0 )
			throw new IllegalArgumentException("NON-POSITIVE LEVELS ARE NOT ALLOWED") ;
		
		this.score = stream.readLong();
		this.addition = stream.readInt() ;
		this.multiplier = stream.readFloat() ;
		this.highestMultiplier = stream.readFloat() ;
		
		this.s0clears = stream.readInt() ;
		this.s1clears = stream.readInt() ;
		this.sLclears = stream.readInt() ;
		this.moclears = stream.readInt() ;
		
		this.longestCascade = stream.readInt() ;
		
		this.s0clearsSinceLevel = stream.readInt() ;
		this.s1clearsSinceLevel = stream.readInt() ;
		this.sLclearsSinceLevel = stream.readInt() ;
		this.moclearsSinceLevel = stream.readInt() ;
		
		// time passed
		if ( version >= 3 ) {
			this.milliseconds = stream.readLong() ;
		} else {
			this.milliseconds = 0 ;
		}
		
		// read boolean: finished
		stream.readBoolean();
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
	public GameInformation finalizeConfiguration() throws IllegalStateException {
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
		return new GameInformation(this) ;
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
		this.takeVals( (GameInformation)in ) ;
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
	public GameInformation readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		this.readObject(inStream) ;
		return this ;
	}

	
}
