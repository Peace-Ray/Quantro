package com.peaceray.quantro.model.systems.special;


import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.attack.AttackSystem;
import com.peaceray.quantro.q.QInteractions;


/**
 * SpecialSystem.
 * 
 * When implementing BitterPill, I realized that no good mechanism exists
 * for the desired functionality.  Although AttackSystem provides a good
 * outlet for transmitting the activation of specials -- or of their effects
 * -- we have no way to build up special actions/abilities (e.g. reserve pieces)
 * or to unleash them.
 * 
 * For example, certain in-game actions cause specials to become available
 * or "power up" as a response to in-game events.  There was no existing mechanism
 * for this; initial plans put it as a Triggered event, but the TriggerSystem doesn't
 * account for the triggering conditions.  Additionally, the complex procedure
 * of creating / editing the special piece was implemented in special-case reasoning
 * that required the TriggerSystem to provide very specific parameters (max galaxy,
 * number to add, etc.).
 * 
 * The current plan: SpecialSystem is responsible for generating specials (implemented
 * as a button to press and represented by a reserve piece) and, when used, translating
 * them into actions or attacks.  This requires additional API for AttackSystem that
 * SpecialSystem can touch and manipulate.
 * 
 * SpecialSystem is responsible for keeping track of all available specials,
 * and constructing a Piece representing the special "preview."
 * 
 * @author Jake
 *
 */
public abstract class SpecialSystem implements SerializableState {
	
	
	/**
	 * Can this special system produce a 'reserve' special?
	 * @return
	 */
	public abstract boolean canHaveReserve() ;
	
	/**
	 * Can this special system produce an 'attack' special?
	 * @return
	 */
	public abstract boolean canHaveAttack() ;
	
	
	/**
	 * Is there a Special available for the user to activate?  Note that
	 * this is a separate consideration from whether the user is allowed
	 * to activate a special at this exact moment...
	 * 
	 * @return Is the a special available for the user?
	 */
	public abstract boolean hasSpecial() ;
	
	
	
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
	public abstract int getSpecialPreviewPieceType() ;
	
	
	
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
	public abstract boolean specialIsReserveInsert() ;
	
	
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
	public abstract boolean specialIsAttack() ;
	
	
	/**
	 * Informs the special system that we are initializing.
	 * @return Whether the special has changed.
	 */
	public abstract boolean initialize() ;
	
	
	/**
	 * Informs the special system that we are about to lock the piece in the specified
	 * location.
	 * 
	 * @param p
	 * @param o
	 * @param blockField
	 * @return Whether the special has changed.
	 */
	public abstract boolean aboutToLock( Piece p, Offset o, byte [][][] blockField ) ;
	
	
	
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
	public abstract boolean aboutToClear( Piece p, Offset o,
			byte [][][] blockFieldBefore, 
			byte [][][] blockFieldAfter,
			byte [][][] blockFieldClearedInverse,
			int [] chromaticClears, boolean [] monochromaticClears ) ;
	
	
	/**
	 * We are ending the ActionCycle.
	 * 
	 * @return Did this change the availability of a special,
	 * 		or its effect?
	 */
	public abstract boolean endCycle() ;
	
	
	
	/**
	 * Use the current special as a reserve insert.  Returns the appropriate 
	 * piece type to insert.
	 * 
	 * @return The piece type to insert.
	 */
	public abstract int useSpecialAsReserveInsert() ;
	
	
	
	/**
	 * Use the current special as an attack.
	 * 
	 * @param as
	 * @return
	 */
	public abstract void useSpecialAsAttack( AttackSystem as ) ;
	
	

	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this SpecialSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public abstract GameInformation getGameInformation() ;
	
	/**
	 * getQInteractions: Specials are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public abstract QInteractions getQInteractions() ;

}
