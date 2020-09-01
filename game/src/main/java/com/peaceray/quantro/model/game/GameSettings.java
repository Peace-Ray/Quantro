package com.peaceray.quantro.model.game;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.modes.GameModes;


/**
 * A storage class for the initial settings used for a game.
 * Distinct from global settings, such as draw detail, volume, etc.,
 * these are the user-specified "NewGame" settings.  Previously
 * these had been passed as direct parameters in a GameIntentPackage.
 * By cordoning them off in their own serializable wrapper class,
 * we can easily load / store them independently and (e.g.) use them
 * to quick-launch with ease.
 * 
 * @author Jake
 *
 */
public class GameSettings implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8715227177792870499L;

	// Increase as changes are made for forward-compatibility
	public static final int VERSION = 2 ;
	// 1: includes 'level lock'
	// 2: adds difficulty, displacement fixed rate
	// 2: Adds 'mPlayers'
	
	
	private boolean mMutable ;
	private int mMode ;
	
	private int mPlayers ;

	private boolean mHasLevel ;
	private boolean mHasClearsPerLevel ;
	private boolean mHasGarbage ;
	private boolean mHasGarbagePerLevel ;
	private boolean mHasLevelLock ;
	private boolean mHasDifficulty ;
	private boolean mHasDisplacementFixedRate ;

	private int mLevel ;
	private int mClearsPerLevel ;
	private int mGarbage ;
	private int mGarbagePerLevel ;
	private boolean mLevelLock ;
	private int mDifficulty ;
	private double mDisplacementFixedRate ;

	
	public GameSettings() {
		mMode = Integer.MIN_VALUE ;
		mHasLevel = false ;
		mHasClearsPerLevel = false ;
		mHasGarbage = false ;
		mHasGarbagePerLevel = false ;
		mHasLevelLock = false ;
		mHasDifficulty = false ;
		mHasDisplacementFixedRate = false ;
		
		/// muh
		mDifficulty = GameInformation.DIFFICULTY_NORMAL ;
		
		mMutable = true ;
	}
	
	public GameSettings( int mode ) {
		this() ;
		
		mMode = mode ;
		if ( GameModes.minPlayers(mMode) == GameModes.maxPlayers(mMode) ) {
			mPlayers = GameModes.minPlayers(mMode) ;
		} else {
			throw new IllegalArgumentException("Game Mode " + mMode + " allows varying player counts; specify the number using GameSettings( mode, players ).") ;
		}
	}
	
	public GameSettings( int mode, int players ) {
		this() ;
		
		mMode = mode ;
		if ( GameModes.supportsNumPlayers(mMode, players) ) {
			mPlayers = players ;
		} else {
			throw new IllegalArgumentException("Game Mode " + mMode + " supports " + GameModes.minPlayers(mode) + "-" + GameModes.maxPlayers(mode) + " players, not " + players) ;
		}
	}
	
	public GameSettings( GameSettings gs ) {
		this() ;
		
		if ( gs != null ) {
			mMode 						= gs.mMode ;
			
			mPlayers					= gs.mPlayers ;
			
			mHasLevel 					= gs.mHasLevel ;
			mLevel 						= gs.mLevel ;
			
			mHasClearsPerLevel			= gs.mHasClearsPerLevel ;
			mClearsPerLevel				= gs.mClearsPerLevel ;
			
			mHasGarbage 				= gs.mHasGarbage ;
			mGarbage 					= gs.mGarbage ;
			
			mHasGarbagePerLevel 		= gs.mHasGarbagePerLevel ;
			mGarbagePerLevel 			= gs.mGarbagePerLevel ;
			
			mHasLevelLock 				= gs.mHasLevelLock ;
			mLevelLock 					= gs.mLevelLock ;
			
			mHasDifficulty 				= gs.mHasDifficulty ;
			mDifficulty 				= gs.mDifficulty ;
			
			mHasDisplacementFixedRate 	= gs.mHasDisplacementFixedRate ;
			mDisplacementFixedRate 		= gs.mDisplacementFixedRate ;
			
			mMutable 					= gs.mMutable ;
		}
	}
	
	public int getMode() { return mMode ; }
	
	public GameSettings setMode( int mode ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mMode = mode ;
		return this ;
	}
	
	public int getPlayers() { return mPlayers ; }

	public GameSettings setPlayers( int players ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mPlayers = players ;
		return this ;
	}
	
	public boolean hasLevel() { return mHasLevel ; }
	
	public int getLevel() { return mHasLevel ? mLevel : -1 ; }
	
	public GameSettings setLevel( int level ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mLevel = level ;
		mHasLevel = true ;
		return this ;
	}
	
	public GameSettings unsetLevel() {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mLevel = -1 ;
		mHasLevel = false ;
		return this ;
	}
	
	
	
	public boolean hasClearsPerLevel() { return mHasClearsPerLevel ; }
	
	public int getClearsPerLevel() { return mHasClearsPerLevel ? mClearsPerLevel : -1 ; }
	
	public GameSettings setClearsPerLevel( int clears ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mClearsPerLevel = clears ;
		mHasClearsPerLevel = true ;
		return this ;
	}
	
	public GameSettings unsetClearsPerLevel() {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mClearsPerLevel = -1 ;
		mHasClearsPerLevel = false ;
		return this ;
	}
	
	
	
	public boolean hasGarbage() { return mHasGarbage ; }
	
	public int getGarbage() { return mHasGarbage ? mGarbage : -1 ; }
	
	public GameSettings setGarbage( int rows ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mGarbage = rows ;
		mHasGarbage = true ;
		return this ;
	}
	
	public GameSettings unsetGarbage() {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mGarbage = -1 ;
		mHasGarbage = false ;
		return this ;
	}
	
	
	public boolean hasGarbagePerLevel() { return mHasGarbagePerLevel ; }
	
	public int getGarbagePerLevel() { return mHasGarbagePerLevel ? mGarbagePerLevel : -1 ; }
	
	public GameSettings setGarbagePerLevel( int rows ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mGarbagePerLevel = rows ;
		mHasGarbagePerLevel = true ;
		return this ;
	}
	
	public GameSettings unsetGarbagePerLevel() {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mGarbagePerLevel = -1 ;
		mHasGarbagePerLevel = false ;
		return this ;
	}
	
	
	public boolean hasLevelLock() { return mHasLevelLock ; }
	
	public boolean getLevelLock() { return mLevelLock ; }
	
	public GameSettings setLevelLock( boolean lock ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mLevelLock = lock ;
		mHasLevelLock = true ;
		return this ;
	}
	
	public GameSettings unsetLevelLock() {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mLevelLock = false ;
		mHasLevelLock = false ;
		return this ;
	}
	
	public boolean hasDifficulty() { return mHasDifficulty ; }
	public int getDifficulty() { return mDifficulty ; }
	
	public GameSettings setDifficulty( int diff ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mDifficulty = diff ;
		mHasDifficulty = true ;
		return this ;
	}
	
	public GameSettings unsetDifficulty() {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mDifficulty = GameInformation.DIFFICULTY_NORMAL ;
		mHasDifficulty = false ;
		return this ;
	}
	
	public boolean hasDisplacementFixedRate() { return mHasDisplacementFixedRate ; }
	public double getDisplacementFixedRate() { return mDisplacementFixedRate ; }
	
	public GameSettings setDisplacementFixedRate( double rate ) {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mDisplacementFixedRate = rate ;
		mHasDisplacementFixedRate = true ;
		return this ;
	}
	
	public GameSettings unsetDisplacementFixedRate() {
		if ( !mMutable )
			throw new IllegalStateException("This GameSettings object is not mutable.") ;
		mDisplacementFixedRate =  0 ;
		mHasDisplacementFixedRate = false ;
		return this ;
	}
	
	
	
	
	public boolean isMutable() {
		return mMutable ;
	}
	
	public GameSettings setImmutable() {
		if ( mMode == Integer.MIN_VALUE )
			throw new IllegalStateException("Game mode must be set before rendering immutable") ;
		if ( !GameModes.supportsNumPlayers(mMode, mPlayers) )
			throw new IllegalStateException("Game mode " + mMode + " does not support number of players " + mPlayers) ;
		mMutable = false ;
		return this ;
	}
	
	public boolean hasDefaultsIgnoringDifficulty() {
		return !mHasLevel && !mHasClearsPerLevel && !mHasGarbage && !mHasGarbagePerLevel && !mHasLevelLock && !mHasDisplacementFixedRate ;
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
		stream.writeInt(VERSION) ;
		
		// mutable?
		stream.writeBoolean(mMutable) ;
		// mode?
		stream.writeInt( mMode ) ;
		
		// Players?
		stream.writeInt(mPlayers) ;
		
		////
		// Write everything else.
		
		// Level
		stream.writeBoolean(mHasLevel) ;
		stream.writeInt(mLevel) ;
		// ClearsPerLevel
		stream.writeBoolean(mHasClearsPerLevel) ;
		stream.writeInt(mClearsPerLevel) ;
		// Garbage
		stream.writeBoolean(mHasGarbage) ;
		stream.writeInt(mGarbage) ;
		// GarbagePerLevel
		stream.writeBoolean(mHasGarbagePerLevel) ;
		stream.writeInt(mGarbagePerLevel) ;
		// LevelLock
		stream.writeBoolean(mHasLevelLock) ;
		stream.writeBoolean(mLevelLock) ;
		// Difficulty
		stream.writeBoolean(mHasDifficulty) ;
		stream.writeInt(mDifficulty) ;
		// Fixed Displacement
		stream.writeBoolean(mHasDisplacementFixedRate) ;
		stream.writeDouble(mDisplacementFixedRate) ;
		
		////
		// Is there any more content to read?
		stream.writeBoolean(false) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Read version.
		int version = stream.readInt() ;
		
		// Mutable?
		mMutable = stream.readBoolean() ;
		// mode?
		mMode = stream.readInt() ;
		
		// Players?
		if ( version >= 2 ) {
			mPlayers = stream.readInt() ;
		} else {
			// we know this is a game mode where min = max players.
			mPlayers = GameModes.minPlayers(mMode) ;
		}
		
		////
		// Read everything else.
		
		// Level
		mHasLevel = stream.readBoolean() ;
		mLevel = stream.readInt() ;
		// ClearsPerLevel
		mHasClearsPerLevel = stream.readBoolean() ;
		mClearsPerLevel = stream.readInt() ;
		// Garbage
		mHasGarbage = stream.readBoolean() ;
		mGarbage = stream.readInt() ;
		// GarbagePerLevel
		mHasGarbagePerLevel = stream.readBoolean() ;
		mGarbagePerLevel = stream.readInt() ;
		// LevelLock
		if ( version >= 1 ) {
			mHasLevelLock = stream.readBoolean() ;
			mLevelLock = stream.readBoolean() ;
		} else {
			mHasLevelLock = false ;
			mLevelLock = false ;
		}
		
		// Difficulty and displacement rate
		if ( version >= 2 ) {
			mHasDifficulty = stream.readBoolean() ;
			mDifficulty = stream.readInt() ;
			
			mHasDisplacementFixedRate = stream.readBoolean() ;
			mDisplacementFixedRate = stream.readDouble() ;
		} else {
			mHasDifficulty = false ;
			mDifficulty = GameInformation.DIFFICULTY_NORMAL ;
			
			mHasDisplacementFixedRate = false ;
			mDisplacementFixedRate = 0 ;
		}
		
		////
		// Is there any more content to read?  Assume 'false'
		stream.readBoolean() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required ASAC structure.") ;
	}
}
