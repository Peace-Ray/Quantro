package com.peaceray.quantro.model.systems.kick;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.systems.collision.CollisionSystem;

public class LeaningKickSystem extends KickSystem {
	
	// We store "kicks" as a list of offsets to apply, selecting the appropriate
	// list based on the piece's current and previous rotation and any provided
	// lean.  These Offsets are static, since we do not expect them to change at
	// any point during the execution of the program.
	private static ArrayList< ArrayList<Offset> > GENERIC_KICKS ;
	private static ArrayList<Offset> LINE_EXTRA ;	// indexed by rotational direction
	
	private static final int LEAN_NONE_CCW 		= 0 ;
	private static final int LEAN_NONE_CW 		= 1 ;
	private static final int LEAN_LEFT 			= 2 ;
	private static final int LEAN_RIGHT 		= 3 ;
	private static final int LEAN_DOWN_CCW 		= 4 ;
	private static final int LEAN_DOWN_CW 		= 5 ;
	private static final int LEAN_DOWN_LEFT 	= 6 ;
	private static final int LEAN_DOWN_RIGHT 	= 7 ;
	private static final int NUM_LEANS 			= 8 ;
	
	// Line-piece kicks are a special case.  Although every other piece
	// kicks no more than 1 block in any single dimension, line pieces are
	// allowed to kick up to 2.  Further complicating matters is the fact that
	// line pieces kick up to 2 only in specific directions, to ensure that
	// they always overlap their previous location (not counting very advanced
	// techniques such as 180 rotations).  Still FURTHER complicating it is
	// the fact that we changed the way line piece rotation works to maintain
	// a "bottom-heavy" mentality, rather than a "above the line" mentality
	// from our previous consideration.  Based on this current setup of
	// the UniversalRotationSystem, where
	// 		rotation 0 is a top-half horizontal,
	//		rotation 1 is a right-half vertical,
	// 		rotation 2 is a bottom-half horizontal,
	// 		rotation 3 is a left-half vertical,
	// 
	// We prioritize movement towards the center
	//		0: bottom
	//		1: left
	//		2: top
	//		3: right
	//
	// as well as movement towards the "previous" state, meaning
	// 		0 CW: left		CCW: right
	//		1 CW: up		CCW: down
	//		2 CW: right		CCW: left
	//		3 CW: down		CCW: up
	//
	// equal-eyed readers will notice that "towards the previous state"
	// is equivalent to "away from center" for that previous state, or
	// equivalently, "towards center" for the 'next' state in the same
	// direction.
	//
	// We can thus represent these priorities as offsets (0,-1), (1,0), (0,1), (-1,0)
	// representing the direction to which we can apply a multiple of 2 if
	// the kick in that direction fails.
	//
	// Finally, regarding our kick policy, every other piece has (considering
	// both lean and rotational direction) a consistent order for trying
	// these kicks: {none, L, DL, D, DR, R, U}.  We do not alter or ammend
	// that order for line pieces; instead, when testing (e.g.) L, we test
	// L1, and if (-1,0) is an available ExtraLineOffset, then test (-2,0)
	// before moving on to the next direction.
	//
	// The ambiguous cases, DL and DR, and handled in a fractal order.
	// If the generic order goes L, DL, D, then in checking DL we try
	// D1L1, D1L2, D2L2, D2L1.  If the order is DL, D, L, we instead
	// try D1L1, D2L2, D2L1, D1L2.
	static {
		GENERIC_KICKS = new ArrayList<ArrayList<Offset>>(NUM_LEANS) ;
		for ( int l = 0; l < NUM_LEANS; l++ ) {
			ArrayList<Offset> al = new ArrayList<Offset>() ;
			
			switch( l ) {
			case LEAN_NONE_CCW:
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			case LEAN_NONE_CW:
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			case LEAN_LEFT:
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			case LEAN_RIGHT:
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			case LEAN_DOWN_CCW:
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			case LEAN_DOWN_CW:
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			case LEAN_DOWN_LEFT:
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			case LEAN_DOWN_RIGHT:
				al.add( new Offset( 1,-1) ) ;		// down-right
				al.add( new Offset( 0,-1) ) ;		// down
				al.add( new Offset( 1, 0) ) ;		// right
				al.add( new Offset(-1,-1) ) ;		// down-left
				al.add( new Offset( 0, 0) ) ;		// no kick
				al.add( new Offset(-1, 0) ) ;		// left
				al.add( new Offset( 0, 1) ) ;		// up
				break ;
			}
			
			GENERIC_KICKS.add(al) ;
		}
		
		// For line pieces, as mentioned above, we prioritize movement
		// towards the center
		//		0: bottom		(remember: up is POSITIVE in game space!)
		//		1: left
		//		2: top
		//		3: right
		
		LINE_EXTRA = new ArrayList<Offset>() ;
		LINE_EXTRA.add( new Offset( 0,-1) ) ;
		LINE_EXTRA.add( new Offset(-1, 0) ) ;
		LINE_EXTRA.add( new Offset( 0, 1) ) ;
		LINE_EXTRA.add( new Offset( 1, 0) ) ;
	}
	
	private GameInformation ginfo ;
	
	// The LeaningKickSystem has no state, but just to
	// implement SerializableState in an intuitive way,
	// we keep an empty "state" object.
	private EmptyState state ;
	private boolean configured ;
	
	private Offset tempOffset ;		// used to prevent allocation per-call
	private Offset tempLineExtra ;	
	
	
	
	public LeaningKickSystem( GameInformation ginfo ) {
		this.ginfo = ginfo ;
		
		state = new EmptyState() ;
		configured = false ;		// for SerializableState
		
		tempOffset = new Offset() ;
		tempLineExtra = new Offset() ;
	}
	

	@Override
	public GameInformation getGameInformation() {
		return this.ginfo ;
	}

	@Override
	public boolean kick(CollisionSystem cs, byte[][][] field, Piece piece,
			Offset offset, Offset lean) {
		
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Which kicks do we use?
		ArrayList<Offset> kicks = candidateKicks(piece, lean) ;
		
		// Now try them.
		tempOffset.takeVals( offset ) ;		// used to store "original" values
		Offset kick ;
		Offset extra ;
		
		// Line piece?  We treat this as a special case.
		boolean isLinePiece = PieceCatalog.isTetromino(piece)
				&& PieceCatalog.getTetrominoCategory(piece) == PieceCatalog.TETRO_CAT_LINE ;
		
		// Set the "extra" offset for line pieces.
		if ( isLinePiece ) {
			// use the extras for current rotation and the "next" (see the
			// comments above).
			tempLineExtra.takeVals( LINE_EXTRA.get(piece.rotation) ) ;
			kick = LINE_EXTRA.get((piece.rotation + piece.rotationDirection + 4)%4) ;	// add 4 to avoid negative result
			tempLineExtra.x += kick.x ;
			tempLineExtra.y += kick.y ;
			//System.err.println("LeaningKickSystem.kick, is line piece, using extra " + tempLineExtra.x + ", " + tempLineExtra.y) ;
 		}
		
		boolean checkedOrigin = false ;
		
		for ( int i = 0; i < kicks.size(); i++ ) {
			kick = kicks.get(i) ;
			offset.x = tempOffset.x + kick.x ;
			offset.y = tempOffset.y + kick.y ;
			
			checkedOrigin = kick.x == 0 && kick.y == 0 ;
			
			//System.err.println("LeaningKickSystem.kick, trying " + kick.x + ", " + kick.y) ;
			
			if ( !cs.collides( field, piece, offset )
					&& ( checkedOrigin || !pathExists(cs, field, piece, offset, tempOffset) ) ) {
				//System.err.println("LeaningKickSystem.kick, taking as the correct kick") ;
				return true ;
			}
			
			if ( isLinePiece
					&& ( ( kick.x != 0 && kick.x == tempLineExtra.x )
							|| ( kick.y != 0 && kick.y == tempLineExtra.y ) ) ) {
				
				//System.err.println("LeaningKickSystem.kick, trying line piece extra") ;
				// try extra kicks.  This kick agrees with our extra
				// in at least one dimension.  Try moving "extra"
				// in that dimension(s).  Use the kicks to prioritize
				// the direction of movement.
				for ( int j = 1; j < kicks.size(); j++ ) {
					// we skip the first, which moves us nowhere.
					extra = kicks.get(j) ;
					if ( ( extra.x == 0 || extra.x == tempLineExtra.x)
							&& ( extra.y == 0 || extra.y == tempLineExtra.y ) ) {
						// apply this extra; it is 0 or == tempLineExtra in 
						// both dimensions.
						offset.x = tempOffset.x + kick.x + extra.x ;
						offset.y = tempOffset.y + kick.y + extra.y ;
						
						//System.err.println("LeaningKickSystem.kick, check extra to kick as " + (kick.x + extra.x) + ", " + (kick.y + extra.y)) ;
						
						if ( !cs.collides( field, piece, offset )
								&& ( checkedOrigin || !pathExists(cs, field, piece, offset, tempOffset) ) ) {
							//System.err.println("LeaningKickSystem.kick, taking kick with extra") ;
							return true ;
						}
					}
				}
			}
		}
		
		// No valid kick found.
		offset.x = tempOffset.x ;
		offset.y = tempOffset.y ;
		return false ;
		
	}

	
	/**
	 * Returns a list of candidate kicks to use for the provided
	 * piece (rotation, turn direction, etc.) and lean.
	 * @param piece
	 * @param lean
	 * @return
	 */
	private ArrayList<Offset> candidateKicks( Piece piece, Offset lean ) {
		// TODO: special consideration for line pieces?
		
		// NO LEAN: CCW or CW depending on rotation direction.
		if ( lean == null || ( lean.x == 0 && lean.y == 0 ) )
			return piece.rotationDirection > 0 ? GENERIC_KICKS.get(LEAN_NONE_CW) : GENERIC_KICKS.get(LEAN_NONE_CCW) ;
		
		// Left or right (no vertical component)
		if ( lean.y >= 0 )		// NOTE: We do not support "upward" leans.
			// left or right
			return lean.x < 0 ? GENERIC_KICKS.get(LEAN_LEFT) : GENERIC_KICKS.get(LEAN_RIGHT) ;
		
		// Downward (no horizontal component)
		if ( lean.x == 0 )
			return piece.rotationDirection > 0 ? GENERIC_KICKS.get(LEAN_DOWN_CW) : GENERIC_KICKS.get(LEAN_DOWN_CCW) ;
		
		// Diagonally downward
		return lean.x < 0 ? GENERIC_KICKS.get(LEAN_DOWN_LEFT) : GENERIC_KICKS.get(LEAN_DOWN_RIGHT) ;
	}
	
	
	/**
	 * Returns whether a path exists for the piece from o1 to o2 without any collisions along
	 * the way.  As a heuristic, we take a "manhattan" route, moving only in 1 direction vertically
	 * and horizontally.
	 * 
	 * This method is used to tweak the lean system, which currently (pre 0.8.2) prioritizes
	 * kicks in the lean direction even if no kick is necessary.
	 * 
	 * This is desired behavior, as it helps maneuver pieces in clever ways without relying
	 * on tight spaces, BUT it also produces strange movement when a direction is held
	 * and the piece rotated (it will "step" in that direction with each turn).
	 * 
	 * As an alternative, we alter behavior such that we keep a kick IFF
	 * 
	 * 1. the piece fits there
	 * 2. there is not a complete path from that location to start.
	 * 
	 * Thus we lose the step-each-turn behavior, but will still fit into new spaces when
	 * there is no other way to reach them.
	 * 
	 * @param cs
	 * @param field
	 * @param piece
	 * @param o1	The starting location.  PRECONDITION: No collision here.
	 * @param o2	The ending location.
	 * @return
	 */
	private boolean pathExists( CollisionSystem cs, byte[][][] field, Piece piece, Offset o1, Offset o2 ) {
		pathExistsCurrentOffset.takeVals(o1) ;
		
		return pathExistsRecur( cs, field, piece, pathExistsCurrentOffset, o2 ) ;
	}
	
	Offset pathExistsCurrentOffset = new Offset() ;
	private boolean pathExistsRecur( CollisionSystem cs, byte[][][] field, Piece piece, Offset mutablePos, Offset dest ) {
		if ( cs.collides(field, piece, mutablePos) )
			return false ;
		if ( mutablePos.x == dest.x && mutablePos.y == dest.y )
			return true ;
		
		// try horizontal then vertical movement.
		if ( mutablePos.x != dest.x ) {
			int addDiff = dest.x > mutablePos.x ? 1 : -1 ;
			mutablePos.x += addDiff ;
			if ( pathExistsRecur( cs, field, piece, mutablePos, dest) )
				return true ;
			mutablePos.x -= addDiff ;
		}
		
		if ( mutablePos.y != dest.y ) {
			int addDiff = dest.y > mutablePos.y ? 1 : -1 ;
			mutablePos.y += addDiff ;
			if ( pathExistsRecur( cs, field, piece, mutablePos, dest) )
				return true ;
			mutablePos.y -= addDiff ;
		}
		
		return false ;
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
	public KickSystem finalizeConfiguration() throws IllegalStateException {
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
	public KickSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		state = (EmptyState)inStream.readObject() ;
		return this ;
	}

}
