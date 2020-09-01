package com.peaceray.quantro.model;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.modes.GameModes;


/**
 * LEGACY CLASS.
 * 
 * This class exists as a legacy instance.  The previous, full implementation
 * of GameSettings which once existed here was moved to QuantroGame at
 * com.peaceray.quantro.model.game.
 * 
 * All we do here is read old versions from ObjectStreams and convert them to
 * GameSettings objects.
 * 
 * @author Jake
 *
 */
public class GameSettings implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1844609369821072858L;

	private com.peaceray.quantro.model.game.GameSettings mGameSettings ;
	
	public com.peaceray.quantro.model.game.GameSettings convertFromLegacy() {
		return new com.peaceray.quantro.model.game.GameSettings( mGameSettings ) ;
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
		throw new IllegalArgumentException("com.peaceray.quantro.model.GameSettings is a LEGACY class, not meant for writing.  Call convertFromLegacy and proceed.") ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		mGameSettings = new com.peaceray.quantro.model.game.GameSettings() ;
		
		// Read version - assume is zero
		stream.readInt() ;
		
		// Mutable?
		boolean mutable = stream.readBoolean() ;
		// mode?
		mGameSettings.setMode( stream.readInt() ) ;
		// players?
		mGameSettings.setPlayers( GameModes.minPlayers(mGameSettings.getMode()) ) ;
		
		////
		// Read everything else.
		
		// Level
		boolean hasLevel = stream.readBoolean() ;
		int level = stream.readInt() ;
		if ( hasLevel )
			mGameSettings.setLevel(level) ;
		// ClearsPerLevel
		boolean hasClearsPerLevel = stream.readBoolean() ;
		int clearsPerLevel = stream.readInt() ;
		if ( hasClearsPerLevel ) 
			mGameSettings.setClearsPerLevel(clearsPerLevel) ;
		// Garbage
		boolean hasGarbage = stream.readBoolean() ;
		int garbage = stream.readInt() ;
		if ( hasGarbage )
			mGameSettings.setGarbage(garbage) ;
		// GarbagePerLevel
		boolean hasGarbagePerLevel = stream.readBoolean() ;
		int garbagePerLevel = stream.readInt() ;
		if ( hasGarbagePerLevel )
			mGameSettings.setGarbagePerLevel(garbagePerLevel) ;
		
		////
		// Is there any more content to read?  Assume 'false'
		stream.readBoolean() ;
		
		if ( !mutable )
			mGameSettings.setImmutable() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required ASAC structure.") ;
	}
	
}
