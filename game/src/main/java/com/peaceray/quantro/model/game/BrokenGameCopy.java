package com.peaceray.quantro.model.game;

import java.io.Serializable;

/**
 * A BrokenGameCopy is a deliberately broken subclass of Game which we
 * expect to fail should any game methods be called on it.  However,
 * methods to access serialized information such as SystemStates, GameInformation,
 * etc., will still function.
 * 
 * The purpose of this class is to allow a rapid duplication of Game object
 * information via "new BrokenGameCopy( game )"; the resulting object
 * can be Saved ( or Loaded into ) without interfering with the original
 * object.  For example, to save a checkpoint without significantly
 * interrupting the game, make a BrokenGameCopy, unpause the game, and
 * save the BrokenGameCopy at your leisure (likely in a separate thread).
 * 
 * @author Jake
 * 
 *
 */
public class BrokenGameCopy extends Game {
	
	Serializable [] mSerializablesFromSystems = null ;
	
	public BrokenGameCopy() {
		super() ;
		configured = true ;
	}

	public BrokenGameCopy( Game game ) {
		super() ;
		configured = true ;
		
		// copy GameInformation, GameEvents, and state.
		s = new GameState( game.s ) ;
		ginfo = new GameInformation( game.ginfo ) ;
		gevents = new GameEvents( game.gevents ) ;
		
		// keep clones of the serializables.
		mSerializablesFromSystems = game.getClonedSerializablesFromSystems() ;
	}
	
	/**
	 * Overrided, as the original method also set this.qi, which we 
	 * don't need to do.
	 */
	public Game setGameInformation( GameInformation ginfo ) {
		this.ginfo = ginfo ;
		return this ;
	}
	
	
	public Game setSystemsFromSerializables( Serializable [] ar ) {
		mSerializablesFromSystems = ar ;
		return this ;
	}
	
	
	public synchronized void setPseudorandom( int pseudorandom ) {
		as.setPseudorandom(pseudorandom) ;
		vs.setPseudorandom(pseudorandom) ;
	}
	
	/**
	 * Puts all the current 
	 * @return
	 */
	public synchronized Serializable [] getSerializablesFromSystems() {
		return mSerializablesFromSystems ;
	}
	
	// break this!
	@Override
	public boolean tick( double time ) {
		throw new IllegalStateException("Can't tick a BrokenGameCopy!") ;
	}

}
