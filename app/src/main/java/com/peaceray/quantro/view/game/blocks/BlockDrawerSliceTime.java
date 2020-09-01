package com.peaceray.quantro.view.game.blocks;


/**
 * Drawing a Slice requires providing a SliceTime object.
 * 
 * New objects should be constructed at the same time the BlockDrawer
 * is instantiated.  Re-setting DrawSettings should not necessarily
 * be accompanied by a new BlockDrawerSliceTime.  Basically, the same
 * SliceTime object should be used for the entirety of the Slice
 * sequence.  This could be a preconstructed Sequence (e.g. Q logo),
 * or a game in progress.
 * 
 * SliceTime is meant to hold the different timing measurements needed
 * to properly animate game events.  Necessary information:
 * 
 * 1. amount of time animating the current slice.  Potentially represented
 * 		as the (relative) time at which the slice was generated.  However,
 * 		we use this for some tricks; in particular, we typically advance
 * 		this only 1 tick when a new slice begins.  This allows the 'new slice'
 * 		calculations to occur without significantly delaying the animation.
 * 
 * 2. amount of time progressing (e.g., time spent unpaused).  This is used
 * 		for everything that advances with "game time;" for example, pulses,
 * 		lingering glows and fades, etc.
 * 
 * 3. amount of time since instantiation.  This is used for everything that
 * 		advances with "real time" (as opposed to "game time"): for example,
 * 		veils need to appear and disappear when the game is paused.
 * 
 * 
 * Here are some important features that should be available (abstractly):
 * 
 * - Hold back on advancing slice time until ready.  Possible reasons: 
 * 		1. initial "wait 1" to perform first-time slice examination without
 * 			jumping ahead in the animation
 * 		2. glows and effects lingering from previous slice(s) that we
 * 			need to play-out before starting a new animation (for example,
 * 			we might not want to clear if we are still finishing glows).
 * 
 * 			This introduces our first problem: the first time a slice is
 *			drawn, we deliberately delay advancement in order to allow time
 *			to process before beginning animations for it.  The question is,
 *			we initially implemented this delay to cover the processing time
 *			for new slices.  We CAN'T perform that processing if we are animating
 *			previous details, since it would delay an existing animation and the
 *			stutter would be apparent.
 *
 *			For that reason, we can only advance glow animations when extremely
 *			little processing is required -- for example, for stable or
 *			Piece Falling slices.
 *
 *			Another problem: what if another piece locks in place almost immediately,
 *			before the last 'glow' ends?  Do we delay the start of the lock animation?
 *			This is disastrous.  Do we allow the choppiness to creep in in this case?
 *			
 * 			Hrmm.  Lock animations are close to a 1/2 second.  At high levels that's
 * 			enough time to fall 4-6 rows, or fall 3 rows and start locking.  We want
 * 			the lock animation to APPEAR to start immediately.  We also want 'metamorphosis fades'
 * 			to proceed through.
 * 
 * 			Two types of glow effects: in-slice and post-slice.  in-slice animations
 * 			play normally using the "time spent animating slice" value.  post-slice,
 * 			or "linger" animations, play through the end of the slice and continue
 * 			to the next draw.  One way to do this is to switch any currently playing
 * 			animation to 'linger.'  At the time of the change, we adjust the times
 * 			set for them.  They then advance via 'unpaused' time, not 'slice' time.
 * 
 * 			Converting to lingering is the final step of a slice.  It is the NEXT slice
 * 			that determines whether it advances until the lingering animations are
 * 			finished.
 * 
 * 
 * So.  Here's a typical draw cycle:
 * 
 * while true:
 * 
 * 		tick games.
 * 			They will advance, or not, depending.
 * 
 * 		get new slices.
 * 			If the games produced them.
 * 
 * 		advance time.
 * 			If a new slice is available, use the block drawer to 
 * 			advance to a new slice.  Otherwise, just advance.
 * 
 * 			four options:
 * 			advanceNewSliceInBackground
 * 			advanceSliceInBackground
 * 			advanceNewSliceInForeground
 * 			advanceSliceInForeground
 * 
 * 		draw current slice with the current time object.
 * 			This is the only part that changes the BlockDrawer's
 * 			internal structure.  We allow one BlockDrawer to 
 * 			'administrate' many Slices and their SliceTime, but
 * 			only actually DRAW one sequence out of many (so we
 * 			can advance background animations at a reasonable
 * 			speed w/o having to calculate the specifics of their
 * 			animations each time).
 * 
 * 		check whether we're done animating
 * 
 * @author Jake
 *
 */
public class BlockDrawerSliceTime {
	
	public enum RelativeTo {
		/**
		 * Slice-relative time: time this slice has been
		 * displayed and unpaused.
		 */
		SLICE,
		
		/**
		 * Unpaused-relative time: time we have been unpaused since the start
		 * of the game.
		 */
		UNPAUSED,
		
		/**
		 * Total time since the game started (even time unpaused).
		 */
		TOTAL,
	}
	
	boolean mSliceHeld ;

	long mSliceTimeNeeded ; 
	
	long mSlice ;
	long mUnpaused ;
	long mTotal ;
	
	long mLastDrawnSlice ;
	long mLastDrawnUnpaused ;
	long mLastDrawnTotal ;
	
	boolean mSliceAdvanced ;
	
	
	public BlockDrawerSliceTime() {
		mSliceHeld = false ;
		
		mSliceTimeNeeded = 0 ;
		
		mSlice = 0 ;
		mUnpaused = 0 ;
		mTotal = 0 ;
		
		mLastDrawnSlice = -1 ;
		mLastDrawnUnpaused = -1 ;
		mLastDrawnTotal = -1 ;
		
		mSliceAdvanced = false ;
	}
	
	
	/**
	 * Are we finished animating this slice?  (in other words,
	 * ready to advance to a new slice, if one is available?)
	 * @return
	 */
	public boolean finished() {
		return mSlice >= mSliceTimeNeeded ;
	}
	
	public long timeSinceFinished() {
		return mSlice - mSliceTimeNeeded ;
	}
	
	void takeVals( BlockDrawerSliceTime sliceTime ) {
		mSliceHeld = sliceTime.mSliceHeld ;
		
		mSliceTimeNeeded = sliceTime.mSliceTimeNeeded ;
		
		mSlice = sliceTime.mSlice ;
		mUnpaused = sliceTime.mUnpaused ;
		mTotal = sliceTime.mTotal ;
		
		mLastDrawnSlice = sliceTime.mLastDrawnSlice ;
		mLastDrawnUnpaused = sliceTime.mLastDrawnUnpaused ;
		mLastDrawnTotal = sliceTime.mLastDrawnTotal ;
		
		mSliceAdvanced = sliceTime.mSliceAdvanced ;
	}
	
	public long getSlice() {
		return mSlice ;
	}
	
	public long getUnpaused() {
		return mUnpaused ;
	}
	
	public long getTotal() {
		return mTotal ;
	}
	
	public long get( RelativeTo relTo ) {
		switch ( relTo ) {
		case SLICE:
			return mSlice ;
		case UNPAUSED:
			return mUnpaused ;
		case TOTAL:
			return mTotal ;
		}
		
		throw new IllegalArgumentException("Don't recognize RelativeTo " + relTo) ;
	}
	
	
	/**
	 * Uses the current settings of "time" to convert the provided number
	 * of milliseconds from its current relative format to a different one.
	 * 
	 * The provided 'time' (a long) is counted from zero in the relToCurrent
	 * notation.  The returned value will represent the same moment in time,
	 * counted from zero in the relToResult notation.
	 * 
	 * @param time
	 * @param relToCurrent
	 * @return
	 */
	public long convert( long time, RelativeTo relToCurrent, RelativeTo relToResult ) {
		long diff = get(relToResult) - get(relToCurrent) ;
		return time + diff ;
	}
	
	long getLastDrawnSlice() {
		return mLastDrawnSlice ;
	}
	
	long getLastDrawnUnpaused() {
		return mLastDrawnUnpaused ; 
	}
	
	public long getLastDrawn( RelativeTo relTo ) {
		switch ( relTo ) {
		case SLICE:
			return mLastDrawnSlice ;
		case UNPAUSED:
			return mLastDrawnUnpaused ;
		case TOTAL:
			return mLastDrawnTotal ;
		}
		
		throw new IllegalArgumentException("Don't recognize RelativeTo " + relTo) ;
	}

	boolean getSliceHeld() {
		return mSliceHeld ;
	}
	
	boolean getSliceAdvanced() {
		return mSliceAdvanced ;
	}
	
	// three different values, each starting at 0.
	// advancing time increases each (usually).
	
	void advance( long millis ) {
		if ( !mSliceHeld ) {
			mSlice += millis ;
			mSliceAdvanced = true ;
		}
		mUnpaused += millis ;
		mTotal += millis ;
	}
	
	void advancePaused( long millis ) {
		mTotal += millis ;
	}
	
	void advanceToNewSlice( long millis ) {
		mSlice = 0 ;
		mSliceAdvanced = false ;
		mUnpaused += millis ;
		mTotal += millis ;
		
		mLastDrawnSlice = -1 ;
		// very important to start this at -1.
		// happenedSinceLastDrawnSlice( 0 ) will return
		// true up to the first call to 'setLastDrawn',
		// and false thereafter.
		// If mLastDrawnSlice is set to 0, then
		// happenedSinceLastDrawnSlice( 0 ) will always
		// return false.
	}
	
	void advanceToNewSlicePaused( long millis ) {
		mSlice = 0 ;
		mSliceAdvanced = false ;
		mTotal += millis ;
		
		mLastDrawnSlice = -1 ;
		// very important to start this at -1.
		// happenedSinceLastDrawnSlice( 0 ) will return
		// true up to the first call to 'setLastDrawn',
		// and false thereafter.
		// If mLastDrawnSlice is set to 0, then
		// happenedSinceLastDrawnSlice( 0 ) will always
		// return false.
	}
	
	/**
	 * Advances this object by the positive milliseconds difference between
	 * its current Unpaused time and the provided object's (future) Unpaused
	 * time.
	 * 
	 * @param sliceTime
	 */
	void advanceToReferenceUnpaused( BlockDrawerSliceTime sliceTime ) {
		long diff = sliceTime.mUnpaused - mUnpaused ;
		if ( diff > 0 ) {
			this.advance(diff) ;
		}
	}
	
	void setSliceHeld( boolean held ) {
		mSliceHeld = held ;
	}
	
	void setSliceTimeNeeded( long millis ) {
		mSliceTimeNeeded = millis ;
	}
	
	void setLastDrawn() {
		mLastDrawnSlice = mSlice ;
		mLastDrawnUnpaused = mUnpaused ;
		mLastDrawnTotal = mTotal ;
	}
	
	boolean hasBeenDrawnEver() {
		return mLastDrawnSlice >= 0 ;
	}
	
	
	boolean hasBeenDrawnAtThisSliceTime() {
		return mLastDrawnSlice == mSlice ;
	}
	
	
	/**
	 * Return whether the given time (in milliseconds since
	 * the start of the slice) has occurred since the last time
	 * this slice was drawn (according to setLastDrawn).
	 * 
	 * One usage is to play sound effects at the right time.
	 * If you know the sound effect 'row clear' occurs at
	 * time 300 in a RowClear Slice, then:
	 * 
	 * draw( slice, sliceTime ):
	 * 		
	 *		// draw code
	 *
	 *		long SOUND_EFFECT_TIME = 300
	 *		if ( sliceTime.happenedSinceLastDrawnSlice( SOUND_EFFECT_TIME ) )
	 *				playRowClearSound()
	 *
	 *		sliceTime.setLastDrawn()
	 *
	 * will play the sound effect at the first draw cycle SINCE
	 * the right moment.
	 * 
	 * Note: this usage will "queue" sound effects from the 
	 * first moment they should be played to the end of the slice.
	 * This may not be what you want.  For example, it's probably
	 * inappropriate to start a "row emph." sound effect when
	 * the row clear animation is already playing.  For that 
	 * reason, use something like
	 * 
	 * happenedSinceLastDrawnSlice( time1 ) && !happenedSlice( time2 )
	 * 
	 * Boundaries: finished() returns whether current time is greater
	 * than OR EQUAL TO the time needed.  In other words, a 10 millisecond
	 * animation is finished once 10 milliseconds have passed since creation
	 * (not 11).  Consider two sound effects; one that STARTs the animation
	 * and one that ENDs it.  When should they play?
	 * 
	 * happenedSinceLastDrawnSlice( START )  <-- should return true
	 * 												for the first draw
	 * 												(before call to setLastDrawn).
	 * 
	 * happenedSinceLastDrawnSlice( END )	<-- should return true
	 * 												for the last draw
	 * 												(when mSlice == END).
	 * 
	 * Thus, for a new slice, mLastDrawnSlice is -1, and:
	 * 
	 * -1 < START <= mSlice
	 * END - n < END <= mSlice
	 * 
	 * In other words, the bounds for this are exclusive below, inclusive above.
	 * That may be counter-intuitive but that's how it works out:
	 * 
	 * If getSlice() == TIME, then happenedSinceLastDrawnSlice( TIME ) is true
	 * unless the lastDrawnSlice was also at TIME.
	 * 
	 * @param millis
	 * @return
	 */
	public boolean happenedSinceLastDrawnSlice( long millis ) {
		return mLastDrawnSlice < millis && millis <= mSlice ;
	}
	
	
	/**
	 * Has the specified time (in slice-relative milliseconds) happened yet?
	 * 
	 * This call has UPPER-INCLUSIVE bounds.  In other words, if the
	 * current time is passed in, the result is 'true.'
	 * 
	 * @param millis
	 * @return
	 */
	public boolean happenedSlice( long millis ) {
		return millis <= mSlice ;
	}
	
	
	/**
	 * Return whether the given time (in milliseconds unpaused since the
	 * beginning of time) has occurred since the last time
	 * a draw occurred (according to setLastDrawn).
	 * 
	 * @param millis
	 * @return
	 */
	public boolean happenedSinceLastDrawnUnpaused( long millis ) {
		return mLastDrawnUnpaused < millis && millis <= mUnpaused ;
	}
	
	
	/**
	 * Has the specified time (in unpaused milliseconds) happened yet?
	 * 
	 * This call has UPPER-INCLUSIVE bounds.  In other words, if the
	 * current time is passed in, the result is 'true.'
	 * 
	 * @param millis
	 * @return
	 */
	public boolean happenedUnpaused( long millis ) {
		return millis <= mUnpaused ;
	}
	
	/**
	 * Return whether the given time (in milliseconds since the
	 * beginning of time) has occurred since the last time
	 * a draw occurred (according to setLastDrawn).
	 * 
	 * @param millis
	 * @return
	 */
	boolean happenedSinceLastDrawnTotal( long millis ) {
		return mLastDrawnTotal < millis && millis <= mTotal ;
	}
	
	
	/**
	 * Has the specified time (in total milliseconds) happened yet?
	 * 
	 * This call has UPPER-INCLUSIVE bounds.  In other words, if the
	 * current time is passed in, the result is 'true.'
	 * 
	 * @param millis
	 * @return
	 */
	boolean happenedTotal( long millis ) {
		return millis <= mTotal ;
	}
	
	
	/**
	 * Return whether the given time (in milliseconds relative to
	 * the provided perspective) has occurred since the last time
	 * a draw occurred (according to setLastDrawn).
	 * 
	 * @param millis
	 * @return
	 */
	public boolean happenedSinceLastDrawn( RelativeTo relTo, long millis ) {
		switch( relTo ) {
		case SLICE:
			return mLastDrawnSlice < millis && millis <= mSlice ;
		case UNPAUSED:
			return mLastDrawnUnpaused < millis && millis <= mUnpaused ;
		case TOTAL:
			return mLastDrawnTotal < millis && millis <= mTotal ;
		}
		
		throw new IllegalArgumentException("Don't recognize RelativeTo " + relTo) ;
	}
	
	
	public boolean happened( RelativeTo relTo, long millis ) {
		switch( relTo ) {
		case SLICE:
			return millis <= mSlice ;
		case UNPAUSED:
			return millis <= mUnpaused ;
		case TOTAL:
			return millis <= mTotal ;
		}
		
		throw new IllegalArgumentException("Don't recognize RelativeTo " + relTo) ;
	}
	
	
	/**
	 * Returns, in milliseconds, the slice-relative time
	 * that has passed since the specified slice-relative time.
	 * 
	 * If happenedSlice( millis ) is true, then this will return
	 * 		nonnegative value.
	 * 
	 * If happenedSlice( millis ) is false, this will return a negative value.
	 * @param millis
	 * @return
	 */
	long timeSinceSlice( long millis ) {
		return mSlice - millis ;
	}
	
	
	/**
	 * Returns, in milliseconds, the unpaused-relative time
	 * that has passed since the specified unpaused-relative time.
	 * 
	 * If happenedUnpaused( millis ) is true, then this will return
	 * 		nonnegative value (i.e. it may be 0)
	 * 
	 * If happenedUnpaused( millis ) is false, this will return a negative value.
	 * @param millis
	 * @return
	 */
	long timeSinceUnpaused( long millis ) {
		return mUnpaused - millis ;
	}
	
	
	/**
	 * Returns, in milliseconds, the total-relative time
	 * that has passed since the specified total-relative time.
	 * 
	 * If happenedTotal( millis ) is true, then this will return
	 * 		nonnegative value (i.e. it may be 0)
	 * 
	 * If happenedTotal( millis ) is false, this will return a negative value.
	 * @param millis
	 * @return
	 */
	long timeSinceTotal( long millis ) {
		return mTotal - millis ;
	}
	
	
	long timeSince( RelativeTo relTo, long millis ) {
		switch( relTo ) {
		case SLICE:
			return mSlice - millis ;
		case UNPAUSED:
			return mUnpaused - millis ;
		case TOTAL:
			return mTotal - millis ;
		}
		
		throw new IllegalArgumentException("Don't recognize RelativeTo " + relTo) ;
	}
	
	
	long timeSinceLastSliceDraw() {
		if ( this.mLastDrawnSlice == -1 )
			return 0 ;
		return mSlice - mLastDrawnSlice ;
	}
	
	
}
