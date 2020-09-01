package com.peaceray.quantro.model.systems.collision;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;

public abstract class CollisionSystem implements SerializableState {
	
	
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this CollisionSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public abstract GameInformation getGameInformation() ;
	
	
	/**
	 * getQInteractions: Collisions are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public abstract QInteractions getQInteractions() ;
	
	
	/**
	 * Returns whether "collides( qo0, qo1 ) equiv. qo0 != 0 && qo1 != 0":
	 * in other words, whether two qorientations necessarily collide IFF they
	 * are both nonzero.
	 * 
	 * @return
	 */
	public abstract boolean exactlyNonzeroQOrientationsCollide() ;

	
	/**
	 * within: Is this piece, at this offset, within the blockField?
	 * This method does NOT check the blocks currently within the blockField;
	 * it only cares about whether the piece is contained within the bounds.
	 * 
	 * For most collision systems, it is assumed that pieces can safely
	 * exist ABOVE the blockfield (i.e., in higher rows) but not outside
	 * it in any other direction (including diagonally up).
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				Is the piece within the blockField at this location?
	 */
	public abstract boolean within( byte [][][] blockField, Piece piece, Offset offset ) ;

	
	/**
	 * collides: Does this Piece, at this Offset, collide with the blocks
	 * currently in blockField?  Will return 'true' the piece cannot exist
	 * in the blockField at its current offset, either because of conflicts
	 * within the field on blocks which have extended outside of it.
	 * 
	 * For most collision systems, it is assumed that pieces can safely
	 * exist ABOVE the blockfield (i.e., in higher rows) but not outside
	 * it in any other direction (including diagonally up).
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				Does the piece collide at this location?
	 */
	public abstract boolean collides( byte [][][] blockField, Piece piece, Offset offset ) ;

	/**
	 * collides: Does this Piece, at this Offset, collide with the blocks
	 * currently in blockField?  Will return 'true' the piece cannot exist
	 * in the blockField at its current offset, either because of conflicts
	 * within the field on blocks which have extended outside of it.
	 * 
	 * For most collision systems, it is assumed that pieces can safely
	 * exist ABOVE the blockfield (i.e., in higher rows) but not outside
	 * it in any other direction (including diagonally up).
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @param walls			Whether the blockfield should be considered as having walls on its left, right and bottom limits.
	 * @return				Does the piece collide at this location?
	 */
	public abstract boolean collides( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) ;
	
	/**
	 * As 'collides,' but considers the blockfield limited by the provided bounds,
	 * and the offset as the difference between the pieces boundsLL and the blockfield's
	 * boundsLL.
	 * 
	 * @param blockField
	 * @param bf_boundsLL
	 * @param bf_boundsUR
	 * @param piece
	 * @param offset
	 * @param walls Whether the blockfield should be considered as having walls on its left, right and bottom limits.
	 * @return
	 */
	public abstract boolean collides( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) ;
	
	
	/**
	 * collides: Does this Piece, at this Offset, collide with the blocks
	 * currently in blockField?  Will return 'true' the piece cannot exist
	 * in the blockField at its current offset, either because of conflicts
	 * within the field on blocks which have extended outside of it.
	 * 
	 * For most collision systems, it is assumed that pieces can safely
	 * exist ABOVE the blockfield (i.e., in higher rows) but not outside
	 * it in any other direction (including diagonally up).
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				Does the piece collide at this location?
	 */
	public abstract boolean collides( boolean [][][] blockField, Piece piece, Offset offset ) ;

	/**
	 * collides: Does this Piece, at this Offset, collide with the blocks
	 * currently in blockField?  Will return 'true' the piece cannot exist
	 * in the blockField at its current offset, either because of conflicts
	 * within the field on blocks which have extended outside of it.
	 * 
	 * For most collision systems, it is assumed that pieces can safely
	 * exist ABOVE the blockfield (i.e., in higher rows) but not outside
	 * it in any other direction (including diagonally up).
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @param walls			Whether the blockfield should be considered as having walls on its left, right and bottom limits.
	 * @return				Does the piece collide at this location?
	 */
	public abstract boolean collides( boolean [][][] blockField, Piece piece, Offset offset, boolean walls ) ;
	
	/**
	 * As 'collides,' but considers the blockfield limited by the provided bounds,
	 * and the offset as the difference between the pieces boundsLL and the blockfield's
	 * boundsLL.
	 * 
	 * @param blockField
	 * @param bf_boundsLL
	 * @param bf_boundsUR
	 * @param piece
	 * @param offset
	 * @param walls Whether the blockfield should be considered as having walls on its left, right and bottom limits.
	 * @return
	 */
	public abstract boolean collides( boolean [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) ;
	
	
	
	/**
	 * spaceBelow: How far can this piece move downwards without a collision?
	 * If 0, the piece is currently resting on a surface.  If 1, it can move
	 * 1 space down and will then be resting... etc.  Returns N, where
	 * moving N+1 spaces downward results in collision.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				How far the piece can move downward before collision.
	 * @param walls Whether the blockfield should be considered as having a "walls" at the limit of its bounds.
	 */
	public abstract int spaceBelow( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) ;
	
	/**
	 * As 'spaceBelow,' but considers the blockfield limited by the provided bounds,
	 * and the offset as the difference between the pieces boundsLL and the blockfield's
	 * boundsLL.
	 * 
	 * @param blockField
	 * @param bf_boundsLL
	 * @param bf_boundsUR
	 * @param piece
	 * @param offset
	 * @param wall Whether the blockfield should be considered as having a "walls" at the limit of its bounds.
	 * @return
	 */
	public abstract int spaceBelow( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) ;
	
	/**
	 * spaceBelow: How far can this piece move downwards without a collision?
	 * If 0, the piece is currently resting on a surface.  If 1, it can move
	 * 1 space down and will then be resting... etc.  Returns N, where
	 * moving N+1 spaces downward results in collision.
	 * 
	 * @param blockField	A 3D array of booleans representing whether that block
	 * 						is filled (and a collision will result if it overlaps with a nonzero
	 * 						block in the piece)
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				How far the piece can move downward before collision.
	 * @param walls Whether the blockfield should be considered as having a "walls" at the limit of its bounds.
	 */
	public abstract int spaceBelow( boolean [][][] blockField, Piece piece, Offset offset, boolean walls ) ;
	
	/**
	 * As 'spaceBelow,' but considers the blockfield limited by the provided bounds,
	 * and the offset as the difference between the pieces boundsLL and the blockfield's
	 * boundsLL.
	 * 
	 * @param blockField	A 3D array of booleans representing whether that block
	 * 						is filled (and a collision will result if it overlaps with a nonzero
	 * 						block in the piece)
	 * @param bf_boundsLL
	 * @param bf_boundsUR
	 * @param piece
	 * @param offset
	 * @param wall Whether the blockfield should be considered as having a "walls" at the limit of its bounds.
	 * @return
	 */
	public abstract int spaceBelow( boolean [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) ;
	
	
	/**
	 * spaceAbove: How far can this piece move upwards without a collision?
	 * Returns N, where moving N+1 spaces upward results in collision.
	 * Behavior is unspecified for pieces with an unobstructed path upwards.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				How far the piece can move upward before collision.
	 */
	public abstract int spaceAbove( byte [][][] blockField, Piece piece, Offset offset ) ;
	
	/**
	 * As 'spaceAbove,' but considers the blockfield limited by the provided bounds,
	 * and the offset as the difference between the pieces boundsLL and the blockfield's
	 * boundsLL.
	 * 
	 * @param blockField
	 * @param bf_boundsLL
	 * @param bf_boundsUR
	 * @param piece
	 * @param offset
	 * @param wall Whether the blockfield should be considered as having a "top wall" at the limit of its bounds.
	 * @return
	 */
	public abstract int spaceAbove( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset ) ;
	
	
	/**
	 * spaceLeft: How far can this piece move to the left without a collision?
	 * Returns N, where moving N+1 spaces left results in collision.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @param walls			Whether the blockfield should be considered as having a "walls" at the limit of its bounds.
	 * @return				How far the piece can move left before collision.
	 */
	public abstract int spaceLeft( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) ;
	
	/**
	 * As 'spaceLeft,' but considers the blockfield limited by the provided bounds,
	 * and the offset as the difference between the pieces boundsLL and the blockfield's
	 * boundsLL.
	 * 
	 * @param blockField
	 * @param bf_boundsLL
	 * @param bf_boundsUR
	 * @param piece
	 * @param offset
	 * @param walls Whether the blockfield should be considered as having a "walls" at the limits of its bounds.
	 * @return
	 */
	public abstract int spaceLeft( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) ;
	
	
	/**
	 * spaceRight: How far can this piece move to the right without a collision?
	 * Returns N, where moving N+1 spaces right results in collision.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @param wall 			Whether the blockfield should be considered as having a "right-hand wall" at the limit of its bounds.
	 * @return				How far the piece can move right before collision.
	 */
	public abstract int spaceRight( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) ;
	
	/**
	 * As 'spaceRight,' but considers the blockfield limited by the provided bounds,
	 * and the offset as the difference between the pieces boundsLL and the blockfield's
	 * boundsLL.
	 * 
	 * @param blockField
	 * @param bf_boundsLL
	 * @param bf_boundsUR
	 * @param piece
	 * @param offset
	 * @param wall 		Whether the blockfield should be considered as having a "walls" at the limit of its bounds.
	 * @return
	 */
	public abstract int spaceRight( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) ;
	
}
