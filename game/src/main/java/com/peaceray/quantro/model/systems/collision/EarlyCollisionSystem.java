package com.peaceray.quantro.model.systems.collision;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;

public class EarlyCollisionSystem extends CollisionSystem {
	
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	// The EarlyCollisionSystem has no state, but just to
	// implement SerializableState in an intuitive way,
	// we keep an empty "state" object.
	private EmptyState state ;
	private boolean configured ;
	
	// Boundaries used when "no bounds" methods are called.
	// These are set to the blockField array limits and then
	// the bounded method versions are used.
	private Offset blockfield_bounds_LL = new Offset() ;
	private Offset blockfield_bounds_UR = new Offset() ;
	
	// Constructor!
	public EarlyCollisionSystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		state = new EmptyState() ;
		configured = false ;
	}
	
	
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this CollisionSystem.
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
	
	
	/**
	 * Returns whether "collides( qo0, qo1 ) equiv. qo0 != 0 && qo1 != 0":
	 * in other words, whether two qorientations necessarily collide IFF they
	 * are both nonzero.
	 * 
	 * @return
	 */
	public boolean exactlyNonzeroQOrientationsCollide() {
		return qi.allQOrientationsCollide() ;
	}
	
	////////////////////////////////////////////////////////////////
	//
	// STATEFUL METHODS
	//
	// Although the EarlyCollisionSystem does not maintain a state,
	// these methods are considered "stateful" under the specification
	// given by SerializableState.
	//
	////////////////////////////////////////////////////////////////
	
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
	public boolean within( byte [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Determine limits to save time on referencing fields we
		// don't have to.
		int R = piece.boundsUR.y - piece.boundsLL.y ;
		int C = piece.boundsUR.x - piece.boundsLL.x ;
		
		//int fieldRows = blockField[0].length ;
		int fieldCols = blockField[0][0].length ;
		
		// A space to store field row, col and piece row, col,
		// which are relative their internal representations
		// (all offsets accounted for).  This prevents doing
		// the same arithmetic conversion many times.
		int fr ;
		int fc ;
		int pr ;
		int pc ;
		
		// What's the qorientation at the given location in the piece?
		// This is allocated to prevent sequential accesses.
		int pqo ;
		
		// Here are the meaty bits.
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				// Set fr, pr
				fr = r + offset.y ;
				pr = r + piece.boundsLL.y ;
				for ( int c = 0; c < C; c++ ) {
					// Set fc, pc, and pqo
					fc = c + offset.x ;
					pc = c + piece.boundsLL.x ;
					pqo = piece.blocks[q][pr][pc] ;
					                          
					// Skip if the piece has NO here
					if ( pqo != QOrientations.NO ) {
						// Outside the bounds of blockField?  It is
						// okay to be too high, but all other dimensions
						// must be within the field.  If outside,
						// there is a collision.
						if ( fr < 0 || fc < 0 || fc >= fieldCols )
							return false ;
					}
				}
			}
		}
		
		// If we got here, there was no collision detected.
		return true ;
	}

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
	synchronized public boolean collides( byte [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return collides( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, true ) ;
	}
	
	
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
	synchronized public boolean collides( boolean [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return collides( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, true ) ;
	}
	
	
	
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
	synchronized public boolean collides( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return collides( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, walls ) ;
	}
	
	
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
	synchronized public boolean collides( boolean [][][] blockField, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return collides( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, walls ) ;
	}
	
	
	
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
	 * @return
	 */
	public boolean collides( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Determine a collision by checking every block within the piece.
		// 'offset' refers to the offset of the lower-left corner of the piece,
		// i.e. that defined by piece.boundsLL
		
		// Determine limits to save time on referencing fields we
		// don't have to.
		int R = piece.boundsUR.y - piece.boundsLL.y ;
		int C = piece.boundsUR.x - piece.boundsLL.x ;
		
		// A space to store field row, col and piece row, col,
		// which are relative their internal representations
		// (all offsets accounted for).  This prevents doing
		// the same arithmetic conversion many times.
		int fr ;
		int fc ;
		int pr ;
		int pc ;
		
		// What's the qorientation at the given location in the piece?
		// This is allocated to prevent sequential accesses.
		byte pqo ;
		
		// Here are the meaty bits.
		int Q = 2 ; // GameModes.numberQPanes(ginfo) ;		// simple optimization
		for ( int q = 0; q < Q; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				// Set fr, pr
				fr = r + bf_boundsLL.y + offset.y ;
				pr = r + piece.boundsLL.y ;
				for ( int c = 0; c < C; c++ ) {
					// Set fc, pc, and pqo
					fc = c + bf_boundsLL.x + offset.x ;
					pc = c + piece.boundsLL.x ;
					pqo = piece.blocks[q][pr][pc] ;
					                          
					// Skip if the piece has NO here
					if ( pqo != QOrientations.NO ) {
						// Outside the bounds of blockField?  It is
						// okay to be too high, but all other dimensions
						// must be within the field.  If outside,
						// there is a collision.
						if ( fr < bf_boundsLL.y || fc < bf_boundsLL.x || fc >= bf_boundsUR.x ) {
							if ( walls ) {
								return true ;
							} 
						}
						else if ( fr < bf_boundsUR.y ) {
							// Otherwise, if within the field, check
							// whether there is a collision.
							if ( qi.collides(pqo, blockField[q][fr][fc]) ) {
								return true ;
							}
						}
					}
				}
			}
		}
		
		// If we got here, there was no collision detected.
		return false ;
	}
	
	
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
	 * @return
	 */
	public boolean collides( boolean [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Determine a collision by checking every block within the piece.
		// 'offset' refers to the offset of the lower-left corner of the piece,
		// i.e. that defined by piece.boundsLL
		
		// Determine limits to save time on referencing fields we
		// don't have to.
		int R = piece.boundsUR.y - piece.boundsLL.y ;
		int C = piece.boundsUR.x - piece.boundsLL.x ;
		
		// A space to store field row, col and piece row, col,
		// which are relative their internal representations
		// (all offsets accounted for).  This prevents doing
		// the same arithmetic conversion many times.
		int fr ;
		int fc ;
		int pr ;
		int pc ;
		
		// What's the qorientation at the given location in the piece?
		// This is allocated to prevent sequential accesses.
		int pqo ;
		
		// Here are the meaty bits.
		int Q = 2 ; // GameModes.numberQPanes(ginfo) ;		// simple optimization
		for ( int q = 0; q < Q; q++ ) {
			for ( int r = 0; r < R; r++ ) {
				// Set fr, pr
				fr = r + bf_boundsLL.y + offset.y ;
				pr = r + piece.boundsLL.y ;
				for ( int c = 0; c < C; c++ ) {
					// Set fc, pc, and pqo
					fc = c + bf_boundsLL.x + offset.x ;
					pc = c + piece.boundsLL.x ;
					pqo = piece.blocks[q][pr][pc] ;
					                          
					// Skip if the piece has NO here
					if ( pqo != QOrientations.NO ) {
						// Outside the bounds of blockField?  It is
						// okay to be too high, but all other dimensions
						// must be within the field.  If outside,
						// there is a collision.
						if ( fr < bf_boundsLL.y || fc < bf_boundsLL.x || fc >= bf_boundsUR.x ) {
							if ( walls ) {
								return true ;
							} 
						}
						else if ( fr < bf_boundsUR.y ) {
							// Otherwise, if within the field, check
							// whether there is a collision.
							if ( blockField[q][fr][fc] ) {
								return true ;
							}
						}
					}
				}
			}
		}
		
		// If we got here, there was no collision detected.
		return false ;
	}
	
	
	
	
	Offset spaceBelow_steps = new Offset() ;
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
	 */
	synchronized public int spaceBelow( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return spaceBelow( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, walls ) ;
	}
	
	
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
	 * @return
	 */
	public int spaceBelow( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// There may be ways to optimize this process, possibly by 
		// sweeping just the "front edge" of the piece, but I don't really
		// care at the moment.  Just repeatedly call 'collides' at different
		// offsets.
		
		// Make an Offset, to represent the direction of our
		// "step".  We use this to offload most of the work to a helper
		// function, that works for all space____ methods.
		
		// We step down.
		spaceBelow_steps.y = -1 ;
		spaceBelow_steps.x = 0 ;
		
		// Finally, we need a maximum distance, in case
		// 'collides' never returns true (for instance, maybe
		// the Piece is totally empty?)
		int maxSteps = Math.min( 2*blockField[0].length,
								offset.y + piece.blocks[0].length) ;
		
		// Call the helper.
		return spaceBySteps( blockField, bf_boundsLL, bf_boundsUR, piece, offset, spaceBelow_steps, maxSteps, walls ) ;
	}
	
	
	
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
	public int spaceBelow( boolean [][][] blockField, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// essentially a copy of the method above.
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return spaceBelow( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, walls ) ;
	}
	
	
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
	 * @return
	 */
	public int spaceBelow( boolean [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// essentially a copy of the method above.
		
		// We step down.
		spaceBelow_steps.y = -1 ;
		spaceBelow_steps.x = 0 ;
		
		// Finally, we need a maximum distance, in case
		// 'collides' never returns true (for instance, maybe
		// the Piece is totally empty?)
		int maxSteps = Math.min( 2*blockField[0].length,
								offset.y + piece.blocks[0].length) ;
		
		// Call the helper.
		return spaceBySteps( blockField, bf_boundsLL, bf_boundsUR, piece, offset, spaceBelow_steps, maxSteps, walls ) ;
	}
	
	
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
	synchronized public int spaceAbove( byte [][][] blockField, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return spaceAbove( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset ) ;
	}
	
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
	 * @return
	 */
	public int spaceAbove( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		// We step up.
		Offset steps = new Offset() ;
		steps.y = 1 ;
		steps.x = 0 ;
		
		int maxSteps = Math.min( 2*blockField[0].length,
								offset.y + piece.blocks[0].length) ;
		
		// Call the helper.
		return spaceBySteps( blockField, bf_boundsLL, bf_boundsUR, piece, offset, steps, maxSteps, true ) ;
	}
	
	/**
	 * spaceLeft: How far can this piece move to the left without a collision?
	 * Returns N, where moving N+1 spaces left results in collision.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				How far the piece can move left before collision.
	 */
	synchronized public int spaceLeft( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return spaceLeft( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, walls ) ;
	}
	
	
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
	 * @return
	 */
	public int spaceLeft( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		// We step left.
		Offset steps = new Offset() ;
		steps.y = 0 ;
		steps.x = -1 ;
		
		int maxSteps = Math.min( 2*blockField[0][0].length,
								offset.x + piece.blocks[0][0].length) ;
		
		// Call the helper.
		return spaceBySteps( blockField, bf_boundsLL, bf_boundsUR, piece, offset, steps, maxSteps, walls ) ;
	}
	
	
	/**
	 * spaceRight: How far can this piece move to the right without a collision?
	 * Returns N, where moving N+1 spaces right results in collision.
	 * 
	 * @param blockField	A 3D array of QOrientations representing blocks
	 * 						in their current, locked positions
	 * @param piece			A new piece existing within the blockField
	 * @param offset		The offset for the piece
	 * @return				How far the piece can move right before collision.
	 */
	synchronized public int spaceRight( byte [][][] blockField, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		blockfield_bounds_LL.setRowCol( 0, 0 ) ;
		blockfield_bounds_UR.setRowCol( blockField[0].length, blockField[0][0].length ) ;
		
		return spaceRight( blockField, blockfield_bounds_LL, blockfield_bounds_UR, piece, offset, walls ) ;
	}
	
	
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
	 * @return
	 */
	public int spaceRight( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR, Piece piece, Offset offset, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		// We step right.
		Offset steps = new Offset() ;
		steps.y = 0 ;
		steps.x = 1 ;
		
		int maxSteps = Math.min( 2*blockField[0][0].length,
								offset.x + piece.blocks[0][0].length) ;
		
		// Call the helper.
		return spaceBySteps( blockField, bf_boundsLL, bf_boundsUR, piece, offset, steps, maxSteps, walls ) ;
	}
	
	
	// to reduce allocation in-game
	private Offset spaceBySteps_offset = new Offset() ;
	/**
	 * spaceBySteps: determine the amount of space available for the block,
	 * taking steps in the indicated direction (defined as an offset from
	 * the previous location).  Will take at most maxSteps, and will return
	 * that value if no collision occurs.
	 * @param blockField
	 * @param piece
	 * @param offset
	 * @param step
	 * @param maxSteps
	 * @return
	 */
	private int spaceBySteps( byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR,
			Piece piece, Offset offset, Offset step, int maxSteps, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Optimization check: if step == (0,-1), and simpleCollisions is true,
		// call the efficient version of this method.
		int space = -1 ;
		if ( qi.allQOrientationsCollide() && step.x == 0 && step.y == -1 ) {
			space = spaceBySteps_stepDownEfficiently( blockField, bf_boundsLL, bf_boundsUR, piece, offset, maxSteps, walls ) ;
			return space ;
			// to check for correctness, comment this line and uncomment the checks below.
		}
		
		// Allocate a new Offset, so we don't overwrite "offset"
		// which may have been sent from outside this class.
		spaceBySteps_offset.x = offset.x ;
		spaceBySteps_offset.y = offset.y ;
		
		// Step through.
		for ( int s = 0; s < maxSteps; s++ ) {
			// Take the step!  We do this first, so
			// that 's' corresponds to the number of steps
			// NOT INCLUDING the collision (e.g., in iter 0,
			// a collision indicates 0 space).  We
			// assume that the piece has already been
			// checked for a collision at its current location.
			spaceBySteps_offset.x += step.x ;
			spaceBySteps_offset.y += step.y ;
			
			// Look for a collision; return s if so.
			if ( collides( blockField, bf_boundsLL, bf_boundsUR, piece, spaceBySteps_offset, walls) ) {
				// To check for correctness, uncomment the lines below.
				//if ( space > -1 && s != space ) {
				//	System.err.println("WARNING: efficient result differs from standard result!") ;
				//	System.err.println("efficient result is " + space + ", true result is " + s) ;
				//	throw new RuntimeException("WHOOPSIE") ;
				//}
				return s ;
			}
		}
		
		// No collision found.  Return maxSteps.
		return Integer.MAX_VALUE/10 ;
	}
	
	/**
	 * spaceBySteps: determine the amount of space available for the block,
	 * taking steps in the indicated direction (defined as an offset from
	 * the previous location).  Will take at most maxSteps, and will return
	 * that value if no collision occurs.
	 * @param blockField
	 * @param piece
	 * @param offset
	 * @param step
	 * @param maxSteps
	 * @return
	 */
	private int spaceBySteps( boolean [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR,
			Piece piece, Offset offset, Offset step, int maxSteps, boolean walls ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Optimization check: if step == (0,-1), and simpleCollisions is true,
		// call the efficient version of this method.
		int space = -1 ;
		if ( step.x == 0 && step.y == -1 ) {
			space = spaceBySteps_stepDownEfficiently( blockField, bf_boundsLL, bf_boundsUR, piece, offset, maxSteps, walls ) ;
			return space ;
			// to check for correctness, comment this line and uncomment the checks below.
		}
		
		// Allocate a new Offset, so we don't overwrite "offset"
		// which may have been sent from outside this class.
		spaceBySteps_offset.x = offset.x ;
		spaceBySteps_offset.y = offset.y ;
		
		// Step through.
		for ( int s = 0; s < maxSteps; s++ ) {
			// Take the step!  We do this first, so
			// that 's' corresponds to the number of steps
			// NOT INCLUDING the collision (e.g., in iter 0,
			// a collision indicates 0 space).  We
			// assume that the piece has already been
			// checked for a collision at its current location.
			spaceBySteps_offset.x += step.x ;
			spaceBySteps_offset.y += step.y ;
			
			// Look for a collision; return s if so.
			if ( collides( blockField, bf_boundsLL, bf_boundsUR, piece, spaceBySteps_offset, walls) ) {
				// To check for correctness, uncomment the lines below.
				//if ( space > -1 && s != space ) {
				//	System.err.println("WARNING: efficient result differs from standard result!") ;
				//	System.err.println("efficient result is " + space + ", true result is " + s) ;
				//	throw new RuntimeException("WHOOPSIE") ;
				//}
				return s ;
			}
		}
		
		// No collision found.  Return maxSteps.
		return Integer.MAX_VALUE/10 ;
	}
	
	
	/**
	 * The method spaceBySteps is a wholly general-purpose method, which is
	 * nice, but as it turns out a significant % of CPU cycles are spent stepping
	 * pieces down by 1 to check the space below a piece.
	 * 
	 * This method (this one here) is intended as a more efficient implementation
	 * of the specific case where spaceBySteps is called with 'step' == (0, -1).
	 * 
	 * Implementation: QInteractions allows collisions only within the same
	 * space - same row, same col, same QOrientation.  Additionally, if our 
	 * precondition(s) are true we do not need to explicitly check for QO collisions;
	 * 'simpleCollisions' means that non-NO blocks always collide.  Therefore,
	 * we implement this as follows: 
	 * 
	 * - for each column of blocks in piece:
	 * 		set internalGap <- { maxSteps, maxSteps }
	 * 		- for each row of blocks in piece:
	 * 			- for each non-NO block in this row/col:
	 * 				set internalGap <- (first in qp, col ? maxSteps : distance from last row seen)
	 * 				set distanceToCheck <- min( maxSteps, internalGap[qp] )
	 * 				scan this many rows downward, looking for a collision.  If one
	 * 					is found, set maxSteps <- the distance.
	 * 
	 * return maxSteps, or if found to be 0 mid-method, return immediately.
	 * 
	 * PRECONDITIONS:
	 * 		'simpleCollisions' was set to true according to our QInteractions
	 * 			object.
	 * 		the piece does not collide at its current position.
	 * 
	 * 
	 * 
	 * @param blockField
	 * @param piece
	 * @param offset
	 * @param maxSteps
	 * @return
	 */
	private int spaceBySteps_stepDownEfficiently(
			byte [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR,
			Piece piece, Offset offset, int maxSteps, boolean walls ) {
		
		int pR = piece.boundsUR.y - piece.boundsLL.y ; ;
		int pC = piece.boundsUR.x - piece.boundsLL.x ; ;
		
		boolean didCollide = false ;
		
		int Q = 2 ; // GameModes.numberQPanes(ginfo) ;		// simple optimization
		
		for ( int c = 0; c < pC; c++ ) {
			int pc = c + piece.boundsLL.x ;
			int fc = c + bf_boundsLL.x + offset.x ;
			if ( fc >= bf_boundsLL.x && fc < bf_boundsUR.x ) {
				for ( int qp = 0; qp < Q; qp++ ) {
					int distSince = maxSteps ;		// distance since the last non-no block seen in column
					for ( int r = 0; r < pR; r++ ) {
						int pr = r + piece.boundsLL.y ;
						int fr = bf_boundsLL.y + r + offset.y ;
						// BUG FIX 4/27: Removed a bounds check for fr w/in bf_bounds.
						// fr is the current position of the (pr, pc) block in the 
						// field.  However, we will be falling downward, so we actually check the
						// FELL-TO position for w/in bounds, NOT the "at current place" position.
						if ( piece.blocks[qp][pr][pc] != QOrientations.NO ) {
							// here's one to check.  Examine up to Math.min(maxSteps, distSince)
							// spaces below.
							int checkDistance = Math.min(maxSteps, distSince) ;
							boolean collide = false ;
							for ( int stepSize = 1; stepSize <= checkDistance; stepSize++ ) {
								if ( fr - stepSize < bf_boundsLL.y  )
									collide = walls ;
								else if ( fr - stepSize >= bf_boundsUR.y )
									collide = walls ;
								else if ( blockField[qp][fr-stepSize][fc] != QOrientations.NO ) {
									collide = true ; 
								}
								
								if ( collide ) {
									// Collision at this distance!  Note that a collision after
									// 'stepSize' steps implies there is room for 'stepSize - 1' drops.
									didCollide = true ;
									maxSteps = stepSize - 1 ;		// our max from now on
									if ( maxSteps == 0 )
										return 0 ;
									break ;
								}
							}
							// we continue moving up the piece's blocks, but now spaceBelow
							// is 0.
							distSince = 0 ;
						}  else {
							// this is NOT a block, but empty space.  Increment distSince.
							distSince++ ;
						}
					}
				}
			}
		}
		
		return didCollide ? maxSteps : Integer.MAX_VALUE/10 ;
	}
	
	
	/**
	 * The method spaceBySteps is a wholly general-purpose method, which is
	 * nice, but as it turns out a significant % of CPU cycles are spent stepping
	 * pieces down by 1 to check the space below a piece.
	 * 
	 * This method (this one here) is intended as a more efficient implementation
	 * of the specific case where spaceBySteps is called with 'step' == (0, -1).
	 * 
	 * Implementation: QInteractions allows collisions only within the same
	 * space - same row, same col, same QOrientation.  Additionally, if our 
	 * precondition(s) are true we do not need to explicitly check for QO collisions;
	 * 'simpleCollisions' means that non-NO blocks always collide.  Therefore,
	 * we implement this as follows: 
	 * 
	 * - for each column of blocks in piece:
	 * 		set internalGap <- { maxSteps, maxSteps }
	 * 		- for each row of blocks in piece:
	 * 			- for each non-NO block in this row/col:
	 * 				set internalGap <- (first in qp, col ? maxSteps : distance from last row seen)
	 * 				set distanceToCheck <- min( maxSteps, internalGap[qp] )
	 * 				scan this many rows downward, looking for a collision.  If one
	 * 					is found, set maxSteps <- the distance.
	 * 
	 * return maxSteps, or if found to be 0 mid-method, return immediately.
	 * 
	 * PRECONDITIONS:
	 * 		'simpleCollisions' was set to true according to our QInteractions
	 * 			object.
	 * 		the piece does not collide at its current position.
	 * 
	 * 
	 * 
	 * @param blockField
	 * @param piece
	 * @param offset
	 * @param maxSteps
	 * @return
	 */
	private int spaceBySteps_stepDownEfficiently(
			boolean [][][] blockField, Offset bf_boundsLL, Offset bf_boundsUR,
			Piece piece, Offset offset, int maxSteps, boolean walls ) {
		
		int pR = piece.boundsUR.y - piece.boundsLL.y ; ;
		int pC = piece.boundsUR.x - piece.boundsLL.x ; ;
		
		boolean didCollide = false ;
		
		int Q = 2 ; // GameModes.numberQPanes(ginfo) ;		// simple optimization
		
		for ( int c = 0; c < pC; c++ ) {
			int pc = c + piece.boundsLL.x ;
			int fc = c + bf_boundsLL.x + offset.x ;
			if ( fc >= bf_boundsLL.x && fc < bf_boundsUR.x ) {
				for ( int qp = 0; qp < Q; qp++ ) {
					int distSince = maxSteps ;		// distance since the last non-no block seen in column
					for ( int r = 0; r < pR; r++ ) {
						int pr = r + piece.boundsLL.y ;
						int fr = bf_boundsLL.y + r + offset.y ;
						// BUG FIX 4/27: Removed a bounds check for fr w/in bf_bounds.
						// fr is the current position of the (pr, pc) block in the 
						// field.  However, we will be falling downward, so we actually check the
						// FELL-TO position for w/in bounds, NOT the "at current place" position.
						if ( piece.blocks[qp][pr][pc] != QOrientations.NO ) {
							// here's one to check.  Examine up to Math.min(maxSteps, distSince)
							// spaces below.
							int checkDistance = Math.min(maxSteps, distSince) ;
							boolean collide = false ;
							for ( int stepSize = 1; stepSize <= checkDistance; stepSize++ ) {
								if ( fr - stepSize < bf_boundsLL.y  )
									collide = walls ;
								else if ( fr - stepSize >= bf_boundsUR.y )
									collide = walls ;
								else if ( blockField[qp][fr-stepSize][fc] ) {
									collide = true ; 
								}
								
								if ( collide ) {
									// Collision at this distance!  Note that a collision after
									// 'stepSize' steps implies there is room for 'stepSize - 1' drops.
									didCollide = true ;
									maxSteps = stepSize - 1 ;		// our max from now on
									if ( maxSteps == 0 )
										return 0 ;
									break ;
								}
							}
							// we continue moving up the piece's blocks, but now spaceBelow
							// is 0.
							distSince = 0 ;
						}  else {
							// this is NOT a block, but empty space.  Increment distSince.
							distSince++ ;
						}
					}
				}
			}
		}
		
		return didCollide ? maxSteps : Integer.MAX_VALUE/10 ;
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
	public EarlyCollisionSystem finalizeConfiguration() throws IllegalStateException {
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
		return new EmptyState( state ) ;
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
		state = (EmptyState)in ;
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
	public EarlyCollisionSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}
	
	
}