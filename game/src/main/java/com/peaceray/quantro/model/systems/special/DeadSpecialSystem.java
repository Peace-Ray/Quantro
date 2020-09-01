package com.peaceray.quantro.model.systems.special;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.attack.AttackSystem;
import com.peaceray.quantro.q.QInteractions;


/**
 * The special system for all pre-1.0 game types.  Does nothing, all the time.
 * 
 * @author Jake
 *
 */
public class DeadSpecialSystem extends SpecialSystem {
	
	GameInformation ginfo ;
	QInteractions qi ;
	
	boolean configured ;
	EmptyState state ;
	
	public DeadSpecialSystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		configured = false ;
		state = new EmptyState() ;
	}
	
	
	/**
	 * Can this special system produce a 'reserve' special?
	 * @return
	 */
	public boolean canHaveReserve() {
		return false ;
	}
	
	/**
	 * Can this special system produce an 'attack' special?
	 * @return
	 */
	public boolean canHaveAttack() {
		return false ;
	}
	
	
	/**
	 * Is there a Special available for the user to activate?  Note that
	 * this is a separate consideration from whether the user is allowed
	 * to activate a special at this exact moment...
	 * 
	 * @return Is the a special available for the user?
	 */
	public boolean hasSpecial() {
		return false ;
	}
	
	
	
	/**
	 * Provides the most appropriate 'piece type' to represent the effect of
	 * this special to the user; glancing at it, they should have an idea
	 * of what the special would do.
	 * 
	 * @return An appropriate special piece type to "preview" the special
	 * 	for the user.  Note that this is NOT a guarantee that using the 
	 * 	special will in any sense create or introduce a piece of that type;
	 * 	this is merely a preview for the user's convenience.
	 * 	Equal to -1 if hasSpecial() would return 'false.'
	 */
	public int getSpecialPreviewPieceType() {
		return -1 ;
	}
	
	
	/**
	 * The current special, if used, functions as an inserted reserve.
	 * The currently falling piece will be moved to NEXT, and a different
	 * piece will replace it using the RESERVE_INSERT_REENTER behavior.
	 * 
	 * Note: this does NOT necessarily indicate that the current preview,
	 * as returned by getSpecialPreviewPieceType() will be used as the
	 * reserve, nor that we will not have a special after it is used.
	 * 
	 * @return Whether the currently available special would, if used,
	 * 		be inserted into the piece queue in the manner of RESERVE_INSERT_REENTER.
	 * 		Returns 'false' if no special is available.
	 */
	public boolean specialIsReserveInsert() {
		return false ;
	}
	
	
	/**
	 * The current special, if used, will present itself as an interaction
	 * with the AttackSystem.  The Attack may or may not need to be
	 * sent out immediately; that's on you.
	 * 
	 * @return Whether the currently available special would, if used,
	 * 		take effect through the AttackSystem.  Using the effect requires
	 * 		providing the AttackSystem to this SpecialSystem as a method
	 * 		parameter.  Returns 'false' if no special is available.
	 */
	public boolean specialIsAttack() {
		return false ;
	}
	
	
	/**
	 * Informs the special system that we are initializing.
	 * @return Whether the special has changed.
	 */
	public boolean initialize() {
		return false ;
	}
	
	
	/**
	 * Informs the special system that we are about to lock the piece in the specified
	 * location.
	 * 
	 * @param p
	 * @param o
	 * @param blockField
	 * @return Whether the special has changed.
	 */
	public boolean aboutToLock( Piece p, Offset o, byte [][][] blockField ) {
		return false ;
	}
	
	
	/**
	 * We are about to perform the provided clear.  These values are identical
	 * to those provided by the similar AttackSystem method.
	 * 
	 * @param p Null if this clear occurs BEFORE the piece enters.
	 * @param o Null if this clear occurs BEFORE the piece enters.
	 * @param blockField
	 * @param chromaticClears
	 * @param monochromaticClears
	 * @return Did this change the availability of a special,
	 *  	of its effect?
	 */
	public boolean aboutToClear( Piece p, Offset o,
			byte [][][] blockFieldBefore, 
			byte [][][] blockFieldAfter,
			byte [][][] blockFieldClearedInverse,
			int [] chromaticClears, boolean [] monochromaticClears ) {
		
		// nothing
		return false ;
	}
	
	
	/**
	 * We are ending the ActionCycle.
	 * 
	 * @return Did this change the availability of a special,
	 * 		or its effect?
	 */
	public boolean endCycle() {
		return false ;
	}
	
	
	/**
	 * Use the current special as a reserve insert.  Returns the appropriate 
	 * piece type to insert.
	 * 
	 * @return The piece type to insert.
	 */
	public int useSpecialAsReserveInsert() {
		throw new IllegalStateException("DeadSpecialSystem has no reserve inserts.") ;
	}
	
	
	/**
	 * Use the current special as an attack.
	 * 
	 * @param as
	 * @return
	 */
	public void useSpecialAsAttack( AttackSystem as ) {
		throw new IllegalStateException("DeadSpecialSystem has no attacks.") ;
	}
	


	@Override
	public GameInformation getGameInformation() {
		return ginfo ;
	}
	

	@Override
	public QInteractions getQInteractions() {
		return qi ;
	}

	
	
	////////////////////////////////////////////////////////////////////////////
	// SERIALIZABLE STATE
	
	
	@Override
	public SerializableState finalizeConfiguration()
			throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		configured = true ;
		return this ;
	}

	@Override
	public Serializable getStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return state ;
	}

	@Override
	public Serializable getCloneStateAsSerializable()
			throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return new EmptyState( state ) ;
	}

	@Override
	public SerializableState setStateAsSerializable(Serializable in)
			throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)in ;
		return this ;
	}

	@Override
	public void writeStateAsSerializedObject(ObjectOutputStream outStream)
			throws IllegalStateException, IOException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		outStream.writeObject(state) ;
	}

	@Override
	public DeadSpecialSystem readStateAsSerializedObject(
			ObjectInputStream inStream) throws IllegalStateException,
			IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}

}
