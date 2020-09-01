package com.peaceray.quantro.view.game.blocks.effects;

import java.lang.ref.WeakReference;

import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;


/**
 * An 'Effect' is a data-storage object with the main purpose of containing
 * one-time calculated information that is then animated according to the
 * current time.
 * 
 * One example is a 'GlowEffect,' which holds necessary information for drawing
 * lock, clear, unlock and metamorphosis "glows."
 * 
 * Effects have data members defined by their subclasses.  The behavior defined
 * here is in regards to how data members should be accessed and mutated.
 * 
 * Generally speaking, one should treat an Effect as immutable once built.  However,
 * since they are used in the draw-loop to store ephemeral data, we do not want
 * to instantiate a new Effect object for each new Effect; instead, we hope to
 * re-use existing objects by setting new data for them, while still maintaining
 * a "sense of immutability."
 * 
 * We do this through a "Setter," analogous to a "Builder" except that the object to
 * be set already exists.  An 'Effect' and its 'Setter' thus operate in two distinct
 * modes -- set (configured) and unset (unconfigured).
 * 
 * A 'set' Effect allows access
 * to its data members in a relatively immutable way; however, the associated Setter
 * will fail (throwing exceptions) if it is used to make any changes.
 * 
 * An 'unset' Effect does not allow access
 * to any of its data members, throwing exceptions upon an attempt; however, the associated
 * Setter can be used to assign new values to each member.
 * 
 * A 'set' Effect can be rendered 'unset' through the use of the "getSetter()" method,
 * which also returns a Setter reference.
 * 
 * An 'unset' Effect can be rendered 'set' through setter.set(), called on the object
 * returned by "getSetter()."
 * 
 * Effects are typically instantiated in an 'unset' state.  If an Effect
 * has a Constructor which produces a "set" state, make sure the Constructor itself
 * using the appropriate Setter interface, to ensure this superclass maintains
 * correct operation.
 * 
 * *****************************************************************************
 * 
 * So.  What is an "Effect?"
 * 
 * An effect is something caused by a slice or progression of slices.  However, they
 * are not actual "slice events" -- for example, in a Rows Clearing slice, a "row being
 * cleared" is not an effect.  Effects are much lower-level, and generally describe
 * the bells and whistles of the slice -- e.g., the visual glow/flashes of the row clearing,
 * the sound(s) that play as the row is being cleared, a "score up" number that appears
 * in the block area, etc.
 * 
 * Obviously different types of effects have very different content, and we leave it to
 * subclasses to determine the necessary data members.  However, there are a few things
 * the every Effect must have, and they are related to the fundamental properties of
 * Effects.
 * 
 * **********************************************
 * 
 * What an Effect has:
 * 
 * An Effect is caused by Slices or a sequence of Slices, but does not necessarily
 * "belong to" a Slice.  Effects may or may not linger beyond the end of a Slice.
 * 
 * An Effect has a moment in time when it "begins," and a moment in time when it "ends."
 * These are fixed at creation.  In other words, the moment when an Effect "ends" (and
 * thus its duration) are known at the moment it begins, if not earlier.
 * 
 * Effects have no impact on the game or the UI before they begin, although they
 * may be calculated and the object configured well in advance of their beginning.
 * 
 * Effects have no lasting impact on the game or the UI after they end.  There is no
 * reason to keep an Effect in memory after its expiration.
 * 
 * The UI impact of an Effect at a given time (BlockDrawerSliceTime) should be fully
 * calculable from the Effect instance, the Slice instance, and the BlockDrawerSliceTime
 * instance.  Note that with only SliceTime and Effect instances, it is possible to 
 * calculate when (if ever) the Effect was most recently processed ("drawn").
 * 
 * 		For example: for GlowEffect, the rendering process depends entirely on the current
 * 					time and metadata.  Animation at a given delta from start time is easily
 * 					calculated without knowing the "previously drawn times."  Duration represents
 * 					the amount of time which the GlowEffect will affect screen pixels.
 * 
 * 		For example: for SoundEffect, no Slice information is needed to play the appropriate sound.
 * 					Duration represents the window in which it is appropriate to "begin" the
 * 					sound effect; the sound will be played at the first draw which falls within
 * 					that window; this information is available directly from SliceTime and the Effect.
 * 
 * 		For example: for TextEffect, the Effect object defines the displayed text and (roughly)
 * 					the animation arc.  The Slice itself is irrelevant once the Effect is calculated.
 * 
 * **********************************************
 * 
 * Effect instances know:
 * 
 * - Whether they are "set" or "unset."  (read-mode or write-mode)
 * - The time they begin
 * 		relative to Slice or to Unpaused
 * - The time they end (and thus duration)
 * 
 * From this, and a TimeSlice, Effect instances can calculate:
 * 
 * - The time they begin / end relative to either Slice or Unpaused
 * - Whether the TimeSlice is within their active window
 * - The amount of time until the Effect begins
 * - The amount of time since an Effect began
 * - The amount of time until the Effect ends
 * - The amount of time since an Effect ended
 * - All of the above relative to the "last draw time" rather than current time.
 * 
 * These time-measurements are particularly useful in 1. Calculating the UI impact
 * (if any) of a given Effect, and 2. Disposing of Effect instances when they
 * can no longer impact the UI.
 * 
 * For 1, we leave it to BlockDrawer.
 * For 2, we provide a bunch of helper methods in EffectList.  These allow
 * 			lists to be cleared of expired Effects while keeping active
 * 			ones, and (pending) retrieve Iterators over the subset of Active
 * 			Effects.
 * 
 * 
 * *********************************************************
 * 
 * Slice / Unpause Relative
 * 
 * As mentioned, Effects measure their start and end times, and current delta,
 * in either a 'slice-relative' or 'unpaused-relative' format.  Conversion between
 * the two requires and "anchor SliceTime" to provide the equivalence between
 * the two measures.
 * 
 * It is recommended that Effects related to and exclusive to the current slice
 * be kept in Slice-relative time, and cleared when the slice changes.  Effects
 * which "linger" from one slice to the next should be kept Slice-relative
 * until the "creating" slice ends, then converted to unpaused-relative.
 * 
 * @author Jake
 *
 */
public abstract class Effect {

	private WeakReference<Object> mwrKey ;
	private Setter mSetter ;
	
	private long mStartTime ;
	private BlockDrawerSliceTime.RelativeTo mStartTimeRelativeTo ;
	private long mDuration ;
	
	protected Effect() {
		mwrKey = null ;
	}
	
	protected Effect( Object key ) {
		if ( key == null )
			throw new NullPointerException("Key provided at construction must be non-null") ;
		mwrKey = new WeakReference<Object> ( key ) ;
		mSetter = null ;
	}

	protected abstract Setter makeSetter() ;
	
	
	
	public boolean started( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return mStartTime <= sliceTime.get(mStartTimeRelativeTo) ;
	}
	
	public boolean startedSinceLastDraw( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return sliceTime.happenedSinceLastDrawn( mStartTimeRelativeTo, mStartTime ) ;
	}
	
	public boolean active( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return timeSinceStarted( sliceTime ) >= 0 && timeTillEnds(sliceTime) >= 0 ;
	}
	
	/**
	 * Returns, in milliseconds, the amount of time until this glow
	 * begins.  If '0' it started at the provided time.  If negative,
	 * it has already started.
	 * 
	 * timeTillStarts( sliceTime ) == -1 * timeSinceStarted( sliceTime ).
	 * 
	 * @param sliceTime
	 * @return
	 */
	public long timeTillStarts( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return mStartTime - sliceTime.get(mStartTimeRelativeTo) ;
	}
	
	/**
	 * Returns, in milliseconds, the amount of time since this glow
	 * began.  If '0' it started at the provided time.  If negative,
	 * it has not yet started.
	 * 
	 * timeTillStarts( sliceTime ) == -1 * timeSinceStarted( sliceTime ).
	 * 
	 * @param sliceTime
	 * @return
	 */
	public long timeSinceStarted( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return sliceTime.get(mStartTimeRelativeTo) - mStartTime ;
	}
	
	/**
	 * Returns, in milliseconds, the amount of time until this glow
	 * begins.  If '0' it started at the provided time.  If negative,
	 * it has already started.
	 * 
	 * timeTillStarts( sliceTime ) == -1 * timeSinceStarted( sliceTime ).
	 * 
	 * @param sliceTime
	 * @return
	 */
	public long timeTillEnds( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return (mStartTime + mDuration) - sliceTime.get(mStartTimeRelativeTo) ;
	}
	
	/**
	 * Returns, in milliseconds, the amount of time since this glow
	 * began.  If '0' it started at the provided time.  If negative,
	 * it has not yet started.
	 * 
	 * timeTillStarts( sliceTime ) == -1 * timeSinceStarted( sliceTime ).
	 * 
	 * @param sliceTime
	 * @return
	 */
	public long timeSinceEnded( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return sliceTime.get(mStartTimeRelativeTo) - (mStartTime + mDuration) ;
	}
	
	public long duration() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return mDuration ;
	}
	
	
	public boolean isExpired( BlockDrawerSliceTime sliceTime ) {
		return timeSinceEnded(sliceTime) > 0 ;
	}
	
	
	/**
	 * One of the few mutators allowed without use of a 'setter.'  In fact,
	 * this mutator requires that the object is set.
	 * 
	 * PRECONDITION: the object is set (a Setter has been used, and set() called on it).
	 * 
	 * POSTCONDITION: start time for this GlowEffect is now slice-relative.
	 * 		If it was slice-relative before, this has no effect.  If it was
	 * 		unpaused-relative before, we use the unpaused time at which the
	 * 		provided slice began to baseline our conversion.
	 * 
	 * @param sliceTime
	 */
	public void convertStartTimeToSliceRelative ( BlockDrawerSliceTime sliceTime ) {
		convertStartTimeToRelative( sliceTime, BlockDrawerSliceTime.RelativeTo.SLICE ) ;
	}
	
	boolean startTimeIsSliceRelative() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return mStartTimeRelativeTo == BlockDrawerSliceTime.RelativeTo.SLICE ;
	}
	
	/**
	 * One of the few mutators allowed without use of a 'setter.'  In fact,
	 * this mutator requires that the object is set.
	 * 
	 * PRECONDITION: the object is set (a Setter has been used, and set() called on it).
	 * 
	 * POSTCONDITION: start time for this GlowEffect is now unpaused-relative.
	 * 		If it was unpaused-relative before, this has no effect.  If it was
	 * 		slice-relative before, we use the unpaused time at which the
	 * 		provided slice began to baseline our conversion.
	 * 
	 * @param sliceTime
	 */
	public void convertStartTimeToUnpausedRelative ( BlockDrawerSliceTime sliceTime ) {
		convertStartTimeToRelative( sliceTime, BlockDrawerSliceTime.RelativeTo.UNPAUSED ) ;
	}
	
	
	public void convertStartTimeToRelative( BlockDrawerSliceTime sliceTime, BlockDrawerSliceTime.RelativeTo relTo ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		mStartTime = sliceTime.convert(mStartTime, mStartTimeRelativeTo, relTo) ;
		mStartTimeRelativeTo = relTo ;
	}
	
	/**
	 * Is this Effect currently set?
	 * @return
	 */
	public final boolean isSet() {
		return mSetter != null && mSetter.mEffectIsSet ;
	}
	
	protected final void throwIfUnset() {
		if ( !isSet() )
			throw new IllegalStateException("This Effect is currently unset.") ;
	}
	
	protected final void throwIfSet() {
		if ( isSet() )
			throw new IllegalStateException("This Effect is currently set.") ;
	}
	
	
	
	
	public final Setter getSetter() {
		if ( mwrKey != null )
			throw new IllegalStateException("Must provide key to access the Setter") ;
		if ( mSetter == null )
			mSetter = makeSetter() ;
		
		mSetter.reset() ;
		return mSetter ;
	}
	
	public final Setter getSetter( Object key ) {
		if ( mwrKey == null )
			throw new IllegalStateException("Created without a key.  Use keyless getSetter method.") ;
		if ( key == null )
			throw new NullPointerException("Provided key is null.") ;
		Object myKey = mwrKey.get() ;
		if ( myKey == null )
			throw new IllegalStateException("Internal key has gone 'null'; did you keep your own reference to it?") ;
		if ( myKey != key )
			throw new IllegalStateException("Provided key does not match key given at construction") ;
		
		if ( mSetter == null )
			mSetter = makeSetter() ;
		mSetter.reset() ;
		return mSetter ;
	}
	
	
	protected abstract class Setter {
		
		private boolean mEffectIsSet ;
		private boolean mStartTimeSet ;
		private boolean mDurationSet ;
		
		
		public Setter startTimeSlice( long time ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mStartTime = time ;
			mStartTimeRelativeTo = BlockDrawerSliceTime.RelativeTo.SLICE ;
			mStartTimeSet = true ;
			
			return this ;
		}

		public Setter startTimeUnpaused( long time ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mStartTime = time ;
			mStartTimeRelativeTo = BlockDrawerSliceTime.RelativeTo.UNPAUSED ;
			mStartTimeSet = true ;
			
			return this ;
		}
		
		public Setter startTimeTotal( long time ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mStartTime = time ;
			mStartTimeRelativeTo = BlockDrawerSliceTime.RelativeTo.TOTAL ;
			mStartTimeSet = true ;
			
			return this ;
		}
		
		public Setter startTime( BlockDrawerSliceTime.RelativeTo relTo, long time ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mStartTime = time ;
			mStartTimeRelativeTo = relTo ;
			mStartTimeSet = true ;
			
			return this ;
		}
		
		public Setter duration( long duration ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mDuration = duration ;
			mDurationSet = true ;
			
			return this ;
		}
		
		
		/**
		 * Performs whatever processing is necessary to transition from an 'unset'
		 * state to a 'set' state.  If returns 'true', the set was successful.
		 * If returns 'false' or throws an exception (preferred, for customized
		 * messages), the set was unsuccessful and the Effect remains unset.
		 */
		protected abstract boolean performSet() ;
		
		/**
		 * Performs whatever local bookkeeping is appropriate for puttting the Effect
		 * in an unset state.
		 * 
		 * @return
		 */
		protected abstract boolean performReset() ;
		
		private void reset() {
			mEffectIsSet = false ;
			mStartTimeSet = false ;
			mDurationSet = false ;
			performReset() ;
		}
		
		public final Effect set() {
			if ( mwrKey != null )
				throw new IllegalStateException("Underlying Effect created with a Key; provide that Key to perform 'set'") ;
			if ( mEffectIsSet )
				throw new IllegalStateException("Underlying Effect is already set.") ;
			if ( !mStartTimeSet )
				throw new IllegalStateException("Start time is not set") ;
			if ( !mDurationSet )
				throw new IllegalStateException("Duration is not set") ;
			
			
			performSet() ;
			
			mEffectIsSet = true ;
			return Effect.this ;
		}
		
		public final Effect set( Object key ) {
			if ( mwrKey == null )
				throw new IllegalStateException("Underlying Effect created without a Key; call 'set()' without arguments.") ;
			if ( mwrKey.get() == null )
				throw new IllegalStateException("Reference to this Effect's key has been lost.  Did you keep a reference in memory?") ;
			if ( key == null )
				throw new NullPointerException("Cannot set using a 'null' key parameter.") ;
			if ( key != mwrKey.get() )
				throw new IllegalArgumentException("Provided key object is not the same instance as used to create this Effect.  Comparison made with '==', not .equals()") ;
			if ( mEffectIsSet )
				throw new IllegalStateException("Underlying Effect is already set.") ;
			if ( !mStartTimeSet )
				throw new IllegalStateException("Start time is not set") ;
			if ( !mDurationSet )
				throw new IllegalStateException("Duration is not set") ;
			
			performSet() ;
			
			mEffectIsSet = true ;
			return Effect.this ;
		}
		
	}
	
}
