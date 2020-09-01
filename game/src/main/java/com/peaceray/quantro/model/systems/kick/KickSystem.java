package com.peaceray.quantro.model.systems.kick;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.collision.CollisionSystem;

public abstract class KickSystem implements SerializableState {

	
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this KickSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public abstract GameInformation getGameInformation() ;
	
	/**
	 * kick: Attempts to find a location in the field where the piece does
	 * not collide; alters 'offset' to this position when it is found.
	 * Returns 'true' if the resulting location (which may be the current
	 * position) is a valid location for the piece; 'false' if no such
	 * location could be found.
	 * 
	 * Different implementations of this interface will have different
	 * algorithms for kick locations to try.
	 * 
	 * POSTCONDITION:
	 * 		* if returns TRUE: 'offset' now indicates a location within
	 * 			'field' where 'piece' does not collide, according to 'cs'
	 * 		* if returns FALSE: 'offset' is unchanged from before the method
	 * 			was called; there is no location near 'piece' (according
	 * 			to the kick rules used by the implementation) where it
	 * 			does not collide.
	 * 
	 * @param cs			The CollisionSystem to use to check block collisions.
	 * @param field			The block field in which 'piece' resides
	 * @param piece			The piece in question
	 * @param offset		The current location of the piece.  May be altered by this call.
	 * @param lean			The "preferred" direction for the piece to kick, usually set by the player holding a direction before rotating.
	 * 							May or may not influence the ultimate kick used.  If used, it 
	 * 							is likely that the values will be considered only as "positive, negative, zero."
	 * @return				Is the post-method 'offset' a non-colliding location for the piece?
	 */
	public abstract boolean kick( CollisionSystem cs, byte [][][] field, Piece piece, Offset offset, Offset lean ) ;
}
