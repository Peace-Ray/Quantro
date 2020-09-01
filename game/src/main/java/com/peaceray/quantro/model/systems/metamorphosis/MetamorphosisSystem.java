package com.peaceray.quantro.model.systems.metamorphosis;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;


/**
 * A MetamorphosisSystem handles the matter of transforming certain
 * piece types into others.  This metamorphosis is assumed to be
 * completely determined by the local game state; i.e., it does NOT
 * originate from a different game.  If such a game DOES originate
 * there, it is under the purview of the AttackSystem.  Furthermore,
 * the MetamorphosisSystem affects ONLY the blocks in the field
 * (and possibly upcoming/reserve pieces, a potential future feature);
 * it has no effect on current level or other details, which are
 * AttackSystem responsibilities; finally, there is a qualitative
 * difference between altering blocks (metamorphosis) and adding
 * and removing blocks (attack and clear, resp.).
 * 
 * The motivating example is Quantro piece types that have different
 * effects depending on how long they've been in the field.  For
 * example, sticky blocks are intended to produce a column-unlock if
 * they are completely removed before the end of the cycle where they
 * lock.  At present (1/17/12), this is hacked before that occurs
 * only if the piece clears AT THE EXACT MOMENT it is placed (and
 * not through a cascade).  Another example is the LI piece which,
 * if locked in a particular way, should allow monochromatic clears
 * within its row, but not if it is locked in a different way.
 * 
 * The most natural way to accomplish this a certain default behavior
 * at the end of a cycle - for instance, ST_ACTIVE becomes ST at cycle
 * end.  The trigger system can enact other changes with specific calls.
 * 
 * 
 * @author Jake
 *
 */
public abstract class MetamorphosisSystem implements SerializableState {

	
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
	 * Performs an "end cycle metamorphosis" on the provided block field.
	 * Will return whether any blocks have changed within the field.
	 * 
	 * As of now (1/17/12), it is assumed that this process is equivalent
	 * to calling 'metamorphosize' with METAMORPHOSIS_DEACTIVATE
	 * as its parameter, but this may not always be the case.
	 * 
	 * @param field
	 * @return 'Result', a container object indicating whether any specific
	 * 	metamorphosis types occurred.  NOTE: the same instance may be mutated by
	 *  subsequent calls to this object.  'Results' are not thread safe.
	 */
	public abstract Result endCycle( byte [][][] field ) ;
	
	
	public static final int METAMORPHOSIS_ACTIVATE 		= 0 ;
	public static final int METAMORPHOSIS_DEACTIVATE 	= 1 ;
	public static final int NUM_METAMORPHOSIS_TYPES = 2 ;
	
	
	/**
	 * Performs a metamorphosis of the specified type within the field.
	 * Certain QO pieces
	 * transform into others upon metamorphosis.  Will return whether any
	 * pieces changed.  It is not necessarily the case that a block that
	 * is "inactive" can activate or vice-versa.
	 * 
	 * @param field
	 * @return Did any metamorphosis occur as a result of this call?
	 */
	public abstract boolean metamorphosize( int metamorphType, byte [][][] field ) ;
	
	
	
	public static class Result {
		protected boolean [] mDid ;
		protected boolean mDidAny ;
		
		protected Result() {
			mDid = new boolean[NUM_METAMORPHOSIS_TYPES] ;
			clear() ;
		}
		
		protected void clear() {
			for ( int i = 0; i < NUM_METAMORPHOSIS_TYPES; i++ )
				mDid[i] = false ;
			mDidAny = false ;
		}
		
		protected void setDid(int metamorphType) {
			mDid[metamorphType] = true ;
			mDidAny = true ;
		}
		
		protected void setDidUntyped() {
			mDidAny = true ;
		}
		
		public boolean did( int metamorphType ) {
			return mDid[metamorphType] ;
		}
		
		public boolean didAny() {
			return mDidAny ;
		}
	}
	
}
