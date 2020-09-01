package com.peaceray.quantro.view.game.blocks.effects;

import java.util.ArrayList;
import java.util.Iterator;

import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;


/**
 * A relatively self-maintaining list for Effects.
 * Effects should be considered immutable for any outside class.
 * 
 * ELists are meant to be convenient, low-maintenance, low-allocation
 * storage for Es.
 * 
 * The first design (12/21/2012) is meant only a replacement for Slice-owned
 * glow information (no "lingering" effects from slice to slice, no attempts
 * to remove Es upon completion while retaining others, etc.).
 * 
 * @author Jake
 *
 */
public abstract class EffectList<E extends Effect> {

	// We store our glow effects in two ArrayLists:
	// active and inactive.  'Inactive' there only to
	// prevent repeated allocation.
	private Object mKey ;
	
	private ArrayList<E> mActiveEffects ;
	private ArrayList<E> mInactiveEffects ;
	
	private E mPendingEffect ;
	private Effect.Setter mPendingSetter ;
	
	public EffectList() {
		mKey = new Object() ;
		
		mActiveEffects = new ArrayList<E>() ;
		mInactiveEffects = new ArrayList<E>() ;
		
		mPendingEffect = null ;
		mPendingSetter = null ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ACCESSING OR REMOVING GLOW EFFECTS
	//
	////////////////////////////////////////////////////////////////////////////
	
	synchronized public int sizeActive( BlockDrawerSliceTime sliceTime ) {
		int num = 0 ;
		for ( int i = 0; i < mActiveEffects.size(); i++ ) {
			if ( mActiveEffects.get(i).active(sliceTime) )
				num++ ;
		}
		
		return num ;
	}
	
	
	synchronized public int sizeExpired( BlockDrawerSliceTime sliceTime ) {
		int num = 0 ;
		for ( int i = 0; i < mActiveEffects.size(); i++ ) {
			if ( mActiveEffects.get(i).isExpired(sliceTime) )
				num++ ;
		}
		
		return num ;
	}
	
	
	synchronized public int sizeStarted( BlockDrawerSliceTime sliceTime ) {
		int num = 0 ;
		for ( int i = 0; i < mActiveEffects.size(); i++ ) {
			if ( mActiveEffects.get(i).started(sliceTime) )
				num++ ;
		}
		
		return num ;
	}
	
	
	
	
	synchronized public int size() {
		return mActiveEffects.size() ;
	}
	
	synchronized public E get(int index) {
		return mActiveEffects.get(index) ;
	}
	
	synchronized public Iterator<E> iterator() {
		return new SimpleIteratorWrapper() ;
	}
	
	synchronized public void remove(int index) {
		E ge = mActiveEffects.remove(index) ;
		if ( ge != null )
			mInactiveEffects.add(ge) ;
	}
	
	synchronized public boolean remove( E ge ) {
		if ( ge == null )
			return false ;
		for ( int i = 0; i < mActiveEffects.size(); i++ ) {
			if ( mActiveEffects.get(i) == ge ) {
				mInactiveEffects.add( mActiveEffects.remove(i) ) ;
				return true ;
			}
		}
		
		return false ;
	}

	synchronized public void clear() {
		while ( mActiveEffects.size() > 0 )
			mInactiveEffects.add( mActiveEffects.remove( mActiveEffects.size() - 1 ) ) ;
	}
	
	synchronized public void clearExpired( BlockDrawerSliceTime sliceTime ) {
		for ( int i = mActiveEffects.size() -1 ; i >= 0 ; i-- ) {
			Effect e = mActiveEffects.get(i) ;
			if ( e.isExpired(sliceTime) )
				mInactiveEffects.add( mActiveEffects.remove( i ) ) ;
		}
	}
	
	synchronized public void clearStarted( BlockDrawerSliceTime sliceTime ) {
		for ( int i = mActiveEffects.size() -1 ; i >= 0 ; i-- ) {
			Effect e = mActiveEffects.get(i) ;
			if ( e.started(sliceTime) )
				mInactiveEffects.add( mActiveEffects.remove( i ) ) ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// TIME MUTATORS
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	synchronized public void convertStartTimeToSliceRelative( BlockDrawerSliceTime sliceTime ) {
		for ( int i = 0; i < mActiveEffects.size(); i++ ) {
			Effect e = mActiveEffects.get(i) ;
			e.convertStartTimeToSliceRelative(sliceTime) ;
		}
	}
	
	synchronized public void convertStartTimeToUnpausedRelative( BlockDrawerSliceTime sliceTime ) {
		for ( int i = 0; i < mActiveEffects.size(); i++ ) {
			Effect e = mActiveEffects.get(i) ;
			e.convertStartTimeToUnpausedRelative(sliceTime) ;
		}
	}
	
	synchronized public boolean hasStartTimeInSliceRelative() {
		for ( int i = 0; i < mActiveEffects.size(); i++ ) {
			Effect e = mActiveEffects.get(i) ;
			if ( e.startTimeIsSliceRelative() )
				return true ;
		}
		
		return false ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ADDING GLOW EFFECTS
	//
	// The EList is intended as the sole instantiator of E
	// objects.  In other words, one does not "add" E objects directly.
	//
	// Instead, new Es should be "added" by requesting a Setter, 
	// performing 'Set' operations to it, and passing that Setter back in using
	// either 'commit' or 'cancel.'
	//
	// Failing to follow this model results in an inoperable EList.
	//
	// In other words:
	//
	// E.Setter setter = gel.add()
	// ... operations to configure setter ...
	// gel.commit( setter )		// or gel.cancel( setter )
	//
	// Between the 'add' operation and the 'commit' (or 'cancel', no other access
	// should be performed.  EList will freely throw IllegalStateExceptions
	// if this process is not followed, meaning it is possible to put GEL in
	// an unusable state.
	// 
	////////////////////////////////////////////////////////////////////////////

	public abstract <T extends Effect.Setter> T add();

	synchronized protected Effect.Setter addSetter() {
		if ( mPendingEffect != null || mPendingSetter != null )
			throw new IllegalStateException("An 'add' operation has already begun.  Either commit or cancel this operation.") ;
		
		if ( mInactiveEffects.size() == 0 )
			allocate(1) ;

		mPendingEffect = mInactiveEffects.remove( mInactiveEffects.size() - 1 ) ;
		mPendingSetter = mPendingEffect.getSetter(mKey) ;
		
		return mPendingSetter ;
	}
	
	synchronized public void commit( Effect.Setter setter ) {
		if ( mPendingEffect == null || mPendingSetter == null )
			throw new IllegalStateException("No 'add' operation in progress; nothing to commit.") ;
		
		if ( mPendingSetter != setter )
			throw new IllegalStateException("This is not the pending 'add' operation.") ;
		
		// perform the 'set' operation...
		mPendingSetter.set(mKey) ;
		
		// move to the 'active' list.
		mActiveEffects.add(mPendingEffect) ;
		
		// null pending
		mPendingEffect = null ;
		mPendingSetter = null ;
	}
	
	synchronized public void cancel( Object setter ) {
		if ( mPendingEffect == null || mPendingSetter == null )
			throw new IllegalStateException("No 'add' operation in progress; nothing to cancel.") ;
		
		if ( mPendingSetter != setter )
			throw new IllegalStateException("This is not the pending 'add' operation.") ;
		
		// move to the 'inactive' list.
		mInactiveEffects.add(mPendingEffect) ;
		
		// null pending
		mPendingEffect = null ;
		mPendingSetter = null ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MEMORY CONTROL
	//
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Construct and return a new Effect, using the provided key.
	 */
	protected abstract E newEffect( Object key ) ;
	
	synchronized public int numAllocated() {
		return mActiveEffects.size() + mInactiveEffects.size() + (mPendingEffect == null ? 0 : 1) ;
	}
	
	/**
	 * Allocates the specified number of E objects in advance of
	 * their use.
	 */
	synchronized public void allocate( int num ) {
		for ( int i = 0; i < num; i++ ) {
			mInactiveEffects.add( newEffect(mKey) ) ;
		}
	}
	
	/**
	 * Allocates Es options up to the total provided.  If at least
	 * that many Es objects have already been allocated, either
	 * directly or indirectly using setters, this method has no effect.
	 * 
	 * @param total
	 */
	synchronized public void allocateUpTo( int total ) {
		for ( int i = numAllocated(); i < total; i++ ) {
			mInactiveEffects.add( newEffect(mKey) ) ;
		}
	}
	
	/**
	 * Releases any Es not currently in use.
	 */
	synchronized public void release() {
		mInactiveEffects.clear() ;
	}
	
	
	private class SimpleIteratorWrapper implements Iterator<E> {
		
		Iterator<E> mActiveArrayListIterator ;
		E mMostRecent ;
		
		private SimpleIteratorWrapper() {
			mActiveArrayListIterator = mActiveEffects.iterator() ;
			mMostRecent = null ;
		}

		@Override
		public boolean hasNext() {
			return mActiveArrayListIterator.hasNext() ;
		}

		@Override
		public E next() {
			mMostRecent = mActiveArrayListIterator.next() ;
			return mMostRecent ;
		}

		@Override
		public void remove() {
			if ( mMostRecent == null ) {
				throw new IllegalStateException("Either 'next' has not been called yet, or the 'remove' method has already been called since the last call to 'next'") ;
			}

			mActiveArrayListIterator.remove() ;
			// if we didn't throw an exeption, then mMostRecent has
			// been removed from active.  Put it in inactive.
			mInactiveEffects.add( mMostRecent ) ;
			// null out most recent.
			mMostRecent = null ;
		}
		
		
		
	}
	
}
