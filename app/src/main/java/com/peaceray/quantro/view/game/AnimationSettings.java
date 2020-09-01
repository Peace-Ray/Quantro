package com.peaceray.quantro.view.game;

import com.peaceray.quantro.utils.InterpolatedQuadraticFunction;
import com.peaceray.quantro.utils.QuadraticFunction;
import com.peaceray.quantro.utils.interpolation.CosineInterpolation;
import com.peaceray.quantro.utils.interpolation.Interpolation;

/**
 * A class whose instances provide a centralized method of providing settings
 * and basic parameters for animation.  It is assumed that these parameters
 * will not change during the course of the game, or possibly at all, once
 * good settings have been found.  However, this provides an easy means of
 * testing different animation speeds.  It also allows information that *does*
 * change to be passed in.
 * 
 * @author Jake
 *
 */
public class AnimationSettings {
	
	public float veil_blocksPerSecond2 ;
	public float veil_maxBlocksPerSecond ;
	public long veil_timeToMaxSpeed ;
	public boolean veil_accelerateIn ;
			// if true, we accelerate while moving in.  If false, we decelerate moving in.
	
	public float unveil_blocksPerSecond2 ;
	public float unveil_maxBlocksPerSecond ;
	public long unveil_timeToMaxSpeed ;
	public boolean unveil_accelerateOut ;
			// if true, we accelerate while moving out.  If false, we decelerate moving out.
		
	public long shuffle_timeToChangeBackground ;
	
	public boolean shuffle_stopWhenPaused ;

	public long [] fall_timeToFallDistance ;
		// A generated array.  timeToFallDistance[3] gives the number of milliseconds
		// required for a piece to fall 3 blocks.  Note that this is an ANIMATED FALL,
		// not a "gameplay" fall.  In other words, changing these values should
		// affect animation but not gameplay difficulty (except by altering the time
		// between a piece locking and its effects being resolved).
	public float fall_blocksPerSecond2 ;
	public float fall_maxBlocksPerSecond ;
	public long fall_timeToMaxSpeed ;
		// Acceleration rate for blocks.  Velocity at time t is t*fall_blocksPerSecond2.
		// Position (change in height) at time t is (1/2) * t^2 * fall_blocksPerSecond2.
	
	// Conversion: push in / push out.
	public float rowPushIn_blocksPerSecond2 ;
	public float rowPushIn_overExtend ;
	public float rowPushOut_blocksPerSecond2 ;
	public float rowPushOut_overExtend ;
	private long [] rowPushIn_timeToPushDistance ;		// includes the overextended value, but not the return.
	private long [] rowPushOut_timeToPushDistance ;		// includes the overextended value, but not the return.
	private QuadraticFunction rowPushIn_positionAtSeconds ;
	private QuadraticFunction rowPushOut_positionAtSeconds ;
	private InterpolatedQuadraticFunction rowPushOutIn_positionAtSeconds ;
	private int rowPushIn_last, rowPushOut_last ;
	
	// To push in (w/o push out), fit the parabola such
	// that it maximizes at in + overextend and starts at zero.
	//
	// To push out, w/ push in up to the same amount, fit the push-out
	// parabola such that it minimizes at (out - overextend) and starts at zero.
	//
	// To push out, w/ push in greater than the same amount, fit the push-out
	// parabola such that it minimizes at (out - overextend) and starts at zero,
	// and the push-in parabola such that it maximizes at (in + overextend) and
	// starts at 2 * out-parabola min (the other zero).
	//
	// THEN fit a cubic to that curve, using:
	// m1 = out - overextend
	// m2 = in - out + overextend
	// i = timeOutMin
	// j = timeInMax
	// M1 = - m1 / i^2
	// M2 = m2 / ( j^2 - 2ij )
	// w = (M1 - M2) / (i - j)
	// a = M1/w - i
	// f(x) = w * x * (x - 2i) * (x + a)
	//
	// This cubic is deterministic given the animation parameters, rowsIn, and rowsOut.
	// Note that time to animate is identical to the component parabolas.  Thus:
	//
	// timeToPushDistance( rowsOut, rowsIn ):
	//		if ( rowsOut < 0 )
	//			rowsInParabolaSize(to rowsOut + overextend) + rowsInParabolaSize(to overextend) <-- other side of parabola
	//		else if ( rowsIn <= rowsOut )
	//			rowsOutParabolaSize(to rowsOut + overextend) + rowsOutParabolaSize(to rowsIn + overextend) <-- other side of parabola.
	// 		else
	//			2 * rowsOutParabolaSize(to rowsOut + overextend) + rowsInParabolaSize(rowsIn - rowsOut + overextend) + rowsInParabolaSize(to overextend)
	
	
	// Chunks that rise-and-fade when locked have a maximum distance and a
	// time to fade.  Rise/fade are both linear, with the distance passing during
	// the specified time period.
	public int riseFade_maxDistance ;
	public long time_riseFade ;
	
	public long time_boxFade ;
	
	// 'rates' are stored as '1s per second', i.e., alpha value should
	// change by secondsSinceEvent * rate_...
	// It is important to note that by using a fade rate, altering the endpoints
	// will change the time required to animate.
	public float rate_lockFillAlphaFade ;
	public float rate_lockTopAlphaFade ;
	public float rate_lockBorderAlphaFade ;
	
	// 'times' are stored as 'number of milliseconds', i.e., the transition should
	// complete in the specified number of seconds.
	// It is important to note that by using a fade time, altering the endpoints
	// will make the animation play faster, to complete in the same amount of time.
	public long time_lockFillAlphaFade ;
	public long time_lockTopAlphaFade ;
	public long time_lockBorderAlphaFade ;
	
	// A 3 step animation for locking in Retro.  First we bring a glowing
	// cover in from 0 alpha to an alpha defined here (linearly).  Second, we linger
	// at that alpha for a period of time.  Finally, we fade the glowing cover away.
	public long time_lockGlowFadeIn ;
	public long time_lockGlowLinger ;
	public long time_lockGlowFadeOut ;
	
	public long time_metamorphosisGlowFadeIn ;
	public long time_metamorphosisGlowLinger ;
	public long time_metamorphosisGlowFadeOut ;
	
	public long time_unlockColumnGlowFadeIn ;
	public long time_unlockColumnGlowLinger ;
	public long time_unlockColumnGlowFadeOut ;
	
	public long time_enterGlowFadeIn ;
	public long time_enterGlowLinger ;
	public long time_enterGlowFadeOut ;
	
	// A staged animation for Retro clears.  1st, we glow a cover using the
	// same procedure as locking (although with possibly different timings).
	// We silently add walls and shading at this point too, so we need to know
	// if and when the glow has reached max.
	public long time_clearGlowFadeIn ;
	public long time_clearGlowLinger ;
	public long time_clearGlowFadeOut ;
	// After this has faded, we then remove the row by sweeping it off
	// the screen in an arbitrary direction, using independent X/Y quadratic
	// functions.  These are defined in "blockHeight / blockWidth" terms.
	// NOTE: the AnimationSettings instance doesn't care about the order
	// of these events.  It is up to the BlockDrawer to provide the correct
	// number of milliseconds from the start of the particular sub-animation
	// in question.
	public float clear_xBlocksPerSecond2 ;	// horizontal acceleration rate
	public float clear_xBlocksPerSecond ;	// horizontal initial velocity
	public float clear_yBlocksPerSecond2 ;	// vertical acceleration rate
	public float clear_yBlocksPerSecond ;	// vertical initial velocity
	
	// For the above transitions, should we use rate, or time?
	// In both cases, linear interpolation is used.
	public boolean useRate_lockFillAlphaFade ;
	public boolean useRate_lockTopAlphaFade ;
	public boolean useRate_lockBorderAlphaFade ;
	
	// Animation for "pulsing" blocks.  Some blocks will pulse their alpha
	// to indicate special significance.  For example, ST blocks will pulse,
	// as will LI_ACTIVE.
	public CosineInterpolation interpolation_pulseCycle ;
	
	// Animation for the unlock ring.  It fades in over a period
	// (see fade settings), then remains for a bit, then launches.
	public long time_unlockColumnFade ;
	public long time_unlockColumnLinger ;
	public long unlockColumn_blocksPerSecond ;
	public long unlockColumn_blocksPerSecond2 ;
	
	// Clearing.
	public long time_clearEmphFade ;
	public long time_clearEmphLinger ;
	
	
	public AnimationSettings() {
		// Allocate
		fall_timeToFallDistance = new long[100] ;		// Let's hope no block fields are this big!
		
		rowPushIn_timeToPushDistance = new long[100] ;
		rowPushOut_timeToPushDistance = new long[100] ;
		
		veil_blocksPerSecond2 = 25 ;
		veil_maxBlocksPerSecond =  100 ;
		veil_timeToMaxSpeed = veil_maxBlocksPerSecond > 0
				? (long)(1000*veil_maxBlocksPerSecond / veil_blocksPerSecond2)
				: Long.MAX_VALUE ;
		veil_accelerateIn = false ;
		
		unveil_blocksPerSecond2 = 25 ;
		unveil_maxBlocksPerSecond = 100 ;
		unveil_timeToMaxSpeed = unveil_maxBlocksPerSecond > 0
				? (long)(1000*unveil_maxBlocksPerSecond / unveil_blocksPerSecond2)
				: Long.MAX_VALUE ;
		unveil_accelerateOut = true ;
		
		shuffle_timeToChangeBackground = 1600 ;
		shuffle_stopWhenPaused = true ;
		
		riseFade_maxDistance = 3 ;
		time_riseFade = 500 ;
		
		time_boxFade = 500 ;
		
		rate_lockFillAlphaFade = 1000 ;
		rate_lockTopAlphaFade = 1000 ;
		rate_lockFillAlphaFade = 1000 ;
		
		time_lockFillAlphaFade = 150 ;
		time_lockTopAlphaFade = 150 ;
		time_lockBorderAlphaFade = 150 ;
		
		time_clearEmphFade = 300 ;
		time_clearEmphLinger = 150 ;
		
		// the lack of "fade in" time is a stopgap to prevent block regions from
		// joining BEFORE they are obscured by a glow.  If we revise BlockDrawer
		// to let the glows peak before redrawing borders, we may want to re-introduce
		// a fade in.
		time_lockGlowFadeIn = 0 ;			
		time_lockGlowLinger = 100 ;
		time_lockGlowFadeOut = 300 ;
		
		time_metamorphosisGlowFadeIn = 10 ;
		time_metamorphosisGlowLinger = 120 ;
		time_metamorphosisGlowFadeOut = 350 ;
		
		time_unlockColumnGlowFadeIn = 100 ;
		time_unlockColumnGlowLinger = 40 ;
		time_unlockColumnGlowFadeOut = 200 ;
		
		time_enterGlowFadeIn = 0 ;			
		time_enterGlowLinger = 100 ;
		time_enterGlowFadeOut = 300 ;
		
		time_clearGlowFadeIn = 100 ;
		time_clearGlowLinger = 0 ;		// both were 10, but I'm tweaking this a bit.
		time_clearGlowFadeOut = 450 ;

		clear_xBlocksPerSecond2 = 40 ;	// horizontal acceleration rate
		clear_xBlocksPerSecond = -2 ;	// horizontal initial velocity
		clear_yBlocksPerSecond2 = 0 ;	// vertical acceleration rate
		clear_yBlocksPerSecond = 0 ;	// vertical initial velocity
		
		useRate_lockFillAlphaFade = false ;
		useRate_lockTopAlphaFade = false ;
		useRate_lockBorderAlphaFade = false ;
		
		interpolation_pulseCycle = new CosineInterpolation( 0.0, 1.0, (long)0, (long)1200, Interpolation.MIRROR ) ;
		
		// Unlocking columns
		time_unlockColumnFade = 50 ;
		time_unlockColumnLinger = 25 ;
		
		// fall speed.
		// snappy animation has a good speed (in 'normal mode' at least).
		setFallAcceleration( 20, 26 ) ;		// accel at 20 rows/second^2, max at 26 rps.
		
		// row pushes.
		// Set and precalculate time-to-go-distance.
		setRowPushAcceleration( 30, 30 ) ;
	}
	
	
	public float getFallAcceleration() {
		return fall_blocksPerSecond2 ;
	}
	
	public float getFallMaxSpeed() {
		return fall_maxBlocksPerSecond ;
	}
	
	public void setFallAcceleration( float accel, float maxSpeed ) {
		fall_blocksPerSecond2 = accel ;
		fall_maxBlocksPerSecond = maxSpeed ;
		// speed at time 't' is t*fall_blocksPerSecond2
		// max = t*fbps
		// t = max / fbps
		fall_timeToMaxSpeed = fall_maxBlocksPerSecond > 0
				? (long)(1000*fall_maxBlocksPerSecond / fall_blocksPerSecond2)
				: Long.MAX_VALUE ;
		precalculate_fall_timeToFallDistance() ;
	}
	
	
	public float getRowPushInAcceleration() {
		return this.rowPushIn_blocksPerSecond2 ;
	}
	
	public float getRowPushOutAcceleration() {
		return this.rowPushOut_blocksPerSecond2 ;
	}
	
	public void setRowPushAcceleration( float rowPushInAccel, float rowPushOutAccel ) {
		rowPushIn_blocksPerSecond2 = rowPushInAccel ;
		rowPushOut_blocksPerSecond2 = rowPushOutAccel ;
		
		rowPushIn_overExtend = 0 ;
		rowPushOut_overExtend = 0.5f ;
		rowPushIn_positionAtSeconds = new QuadraticFunction() ;
		rowPushOut_positionAtSeconds = new QuadraticFunction() ;
		rowPushOutIn_positionAtSeconds = new InterpolatedQuadraticFunction() ;
		rowPushIn_last = Integer.MIN_VALUE ;
		rowPushOut_last = Integer.MIN_VALUE ;
		
		precalculate_rowPushOut_timeToPushDistance() ;
		precalculate_rowPushIn_timeToPushDistance() ;
	}
	
	public long getTimeRiseFade() {
		return this.time_riseFade ;
	}
	
	public void setTimeRiseFade( long time ) {
		this.time_riseFade = time ;
	}
	
	
	/**
	 * Accelerates the animations that deal with blocks moving in
	 * position -- contrast this with "locking" or "clearing" (etc.)
	 * animations, which put a flashy effect in place but don't actually
	 * move anything.
	 * 
	 * The distinction?  "Flashy" animations can linger in place while
	 * the rest of the animation plays out (or gameplay continues).  They
	 * are only visual noise, after all.  "Movement" animations need to
	 * be completed before any other effect can begin.  For fast game speeds,
	 * it makes sense to accelerate movement animations for gameplay reasons,
	 * but accerelating "flashy" animations may be more trouble than it's
	 * worth.
	 * 
	 * @param accelFactor
	 */
	public void accelerateMovementAnimations( double accelFactor ) {
		setFallAcceleration(
				(float)(getFallAcceleration() * accelFactor),
				(float)(getFallMaxSpeed() * accelFactor) ) ;
		setRowPushAcceleration(
				(float)(getRowPushInAcceleration() * accelFactor) ,
				(float)(getRowPushOutAcceleration() * accelFactor) ) ;
		setTimeRiseFade( (long)Math.round( getTimeRiseFade() / accelFactor ) ) ;
	}
	
	
	public void precalculate_fall_timeToFallDistance() {
		// Using g := fall_blocksPerSecond2, the time to
		// fall distance d comes from:
		//
		// d = (1/2) g t^2
		// (2d / g) = t^2
		// sqrt( 2d / g ) = t
		//
		// With 't' giving the answer in seconds.
		
		// how far will it have fallen once it reaches terminal velocity?
		float distAtMaxSpeed = distanceFallen( fall_timeToMaxSpeed ) ;
		
		for ( int d = 0; d < fall_timeToFallDistance.length; d++ ) {
			double t = Math.sqrt((double) 2*d / fall_blocksPerSecond2) ;
			fall_timeToFallDistance[d] = (int) Math.round( t * 1000 ) ;
			
			if ( d > distAtMaxSpeed ) {
				// unfortunately, we reach terminal velocity before reaching this.
				// we need to adjust the settings here...
				fall_timeToFallDistance[d] = fall_timeToMaxSpeed + (long)(1000*(d - distAtMaxSpeed)/fall_maxBlocksPerSecond) ;
			}
		}
	}

	
	
	private float distanceTraveledDecelerating(
			long millis,
			float blocksPerSecond2, float maxBlocksPerSecond, long timeToMaxSpeed, float maxDistance ) {
		
		// First step: we were given the time to reach our maximum speed.  It's worth taking the time
		// to calculate the distance we will have traveled at the time we reach that speed.
		float distanceToMaxSpeed = (maxBlocksPerSecond >= 0 && timeToMaxSpeed > 0)
				? distanceTraveledAccelerating( timeToMaxSpeed, blocksPerSecond2, -1, -1, -1 )
				: Float.MAX_VALUE ;
		
		// Remember: 'millis' is time spent moving TOWARDS maxDistance.  In other words if we
		// know the time to reach maxDistance (Time), we should calculate our position at 
		// Time - millis moving away from maxDistance.
				
		// If the distance to reach "max speed" is <= maxDistance, we can use the simpler
		// accelerating distance calculation to find our position.
		if ( distanceToMaxSpeed >= maxDistance ) {	
			double timeDeltaZeroToMax = Math.sqrt( 2 * blocksPerSecond2 * maxDistance ) / (blocksPerSecond2) ;
			double t = Math.max(0, timeDeltaZeroToMax - ((double)millis/1000) ) ;
			// 't' is essentially the amount of time we would have to spend moving AWAY from 
			// maxDistance to reach our current position.  However, if 't' * 1000 > timeToMaxSpeed,
			// 
			double height = maxDistance - (0.5f) * t * t * blocksPerSecond2 ;
			
			return (float)(height) ;
		}
		
		// Otherwise, we spend the first (maxDistance - distanceToMaxSpeed) traveling
		// at maximum speed.
		float distanceAtMaxSpeed = maxDistance - distanceToMaxSpeed ;
		long timeToCoverDistanceAtMaxSpeed = (long)Math.round( 1000 * distanceAtMaxSpeed / maxBlocksPerSecond ) ;
		if ( timeToCoverDistanceAtMaxSpeed > millis ) {
			// We have been traveling at max speed this entire time.
			double height = maxDistance - millis / 1000.0f * maxBlocksPerSecond ;
			return (float)height ;
		}
		
		// Okay.  We have spent a certain time traveling at max speed and a certain
		// time decelerating down the parabola.  The total distance traveled is
		// distanceAtMaxSpeed + distanceTraveledDecelerating( timeDecel, bps2, -1, -1, maxDist - distanceAtMaxSpeed ).
		double height = maxDistance - ( distanceAtMaxSpeed + distanceTraveledDecelerating(
				millis - timeToCoverDistanceAtMaxSpeed,
				blocksPerSecond2,
				-1, -1, maxDistance - distanceAtMaxSpeed) ) ;
		return (float)height ;
		
	}
	
	private float distanceTraveledAccelerating(
			long millis, float blocksPerSecond2, float maxBlocksPerSecond, long timeToMaxSpeed, float maxDistance ) {
		
		long millisAccelerating = ( timeToMaxSpeed >= 0  ) ?  Math.min(millis, timeToMaxSpeed) : millis ;
		long millisTopSpeed = Math.max(0, millis - millisAccelerating) ;
		float t = ((float)millisAccelerating)/1000 ;
		float dist = (0.5f) * t * t * blocksPerSecond2 ;
			
		if ( millisTopSpeed > 0 && maxBlocksPerSecond > 0 )
			dist += maxBlocksPerSecond * millisTopSpeed / 1000 ;

		return maxDistance < 0 ? dist : Math.min( dist, maxDistance ) ;
	}
	
	
	public float distanceFallen( long millisecondsSpentFalling ) {
		return distanceTraveledAccelerating( millisecondsSpentFalling,
				fall_blocksPerSecond2, fall_maxBlocksPerSecond, fall_timeToMaxSpeed, Float.MAX_VALUE) ;
	}
	
	public float distanceVeiled( long millisecondsSpentVeiling, float distanceToFullVeil ) {
		if ( veil_accelerateIn )
			return distanceTraveledAccelerating(
					millisecondsSpentVeiling,
					veil_blocksPerSecond2,
					veil_maxBlocksPerSecond,
					veil_timeToMaxSpeed,
					distanceToFullVeil) ;
		else
			return distanceTraveledDecelerating(
					millisecondsSpentVeiling,
					veil_blocksPerSecond2,
					veil_maxBlocksPerSecond,
					veil_timeToMaxSpeed,
					distanceToFullVeil) ;
	}
	
	public float distanceUnveiled( long millisecondsSpentUnveiling, float distanceToFullUnveil ) {
		if ( unveil_accelerateOut )
			return distanceTraveledAccelerating(
					millisecondsSpentUnveiling,
					unveil_blocksPerSecond2,
					unveil_maxBlocksPerSecond,
					unveil_timeToMaxSpeed,
					distanceToFullUnveil) ;
		else
			return distanceTraveledDecelerating(
					millisecondsSpentUnveiling,
					unveil_blocksPerSecond2,
					unveil_maxBlocksPerSecond,
					unveil_timeToMaxSpeed,
					distanceToFullUnveil) ;
	}
	
	
	public float shuffle_backgroundAlphaMult( long millisecondsSpentFadingIn ) {
		if ( millisecondsSpentFadingIn <= 0 )
			return 0 ;
		if ( millisecondsSpentFadingIn > shuffle_timeToChangeBackground )
			return 1 ;
		return (float)(((double)millisecondsSpentFadingIn) / ((double)shuffle_timeToChangeBackground)) ;
	}
	
	public boolean shuffle_stopWhenPaused() {
		return this.shuffle_stopWhenPaused ;
	}
	
	
	private void precalculate_rowPushIn_timeToPushDistance() {
		
		for ( int d = 0; d < this.rowPushIn_timeToPushDistance.length; d++ ) {
			//	height = tHTR - (1/2)(t)^2 * rR_bPS2
			//  height = tHTR - (1/2)(MAX(0, tDZTM - seconds))^2 * rR_bPS2
			// 	height = tHTR- (1/2)(MAX(0, (SQRT( 2 * rR_bPS2 * tHTR ) / rR_bPS2) - seconds))^2 * rR_bPS2
			
			// therefore
			
			// d = d - (1/2)(MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds))^2 * rR_bPS2
			// (1/2)(MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds))^2 * rR_bPS2 = 0
			// (MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds))^2 = 0 
			// MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds) = 0
			// ... is true when SQRT( 2 * rR_bPS2 * d ) / rR_bPS2 <= seconds and so becomes true when
			// seconds = SQRT( 2 * rR_bPS2 * d ) / rR_bPS2
			
			// However, note that we always overextend by a certain amount, then return to
			// position!  We use this structure to hold the time to REACH the overextended
			// position, but not to return: note that the return time is
			// rowPushIn_timeToPushDistance[0], so adding this value will provide
			// the total trip time.
			
			double toOverextend = 1000 * Math.sqrt( 2 * rowPushIn_blocksPerSecond2 * (d + rowPushIn_overExtend) ) / this.rowPushIn_blocksPerSecond2 ;
			rowPushIn_timeToPushDistance[d] = Math.round(toOverextend) ;
		}
	}
	
	
	private void precalculate_rowPushOut_timeToPushDistance() {
		
		for ( int d = 0; d < this.rowPushOut_timeToPushDistance.length; d++ ) {
			//	height = tHTR - (1/2)(t)^2 * rR_bPS2
			//  height = tHTR - (1/2)(MAX(0, tDZTM - seconds))^2 * rR_bPS2
			// 	height = tHTR- (1/2)(MAX(0, (SQRT( 2 * rR_bPS2 * tHTR ) / rR_bPS2) - seconds))^2 * rR_bPS2
			
			// therefore
			
			// d = d - (1/2)(MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds))^2 * rR_bPS2
			// (1/2)(MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds))^2 * rR_bPS2 = 0
			// (MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds))^2 = 0 
			// MAX(0, (SQRT( 2 * rR_bPS2 * d ) / rR_bPS2) - seconds) = 0
			// ... is true when SQRT( 2 * rR_bPS2 * d ) / rR_bPS2 <= seconds and so becomes true when
			// seconds = SQRT( 2 * rR_bPS2 * d ) / rR_bPS2
			
			// However, note that we always overextend by a certain amount, then return to
			// position!  We use this structure to hold the time to REACH the overextended
			// position, but not to return: note that the return time is
			// rowPushIn_timeToPushDistance[0], so adding this value will provide
			// the total trip time.
			
			double toOverextend = 1000 * Math.sqrt( 2 * rowPushOut_blocksPerSecond2 * (d + rowPushOut_overExtend) ) / this.rowPushOut_blocksPerSecond2 ;
			rowPushOut_timeToPushDistance[d] = Math.round(toOverextend) ;
		}
	}
	
	
	
	public long timeToPushOut( int rows ) {
		return timeToPushDistance( rows, -1 ) ;
	}
	
	public long timeToPushIn( int rows ) {
		return timeToPushDistance( -1, 0 ) ;
	}
	
	
	/**
	 * How many milliseconsd to push out the specified number of rows, then
	 * push in the specified number?
	 * 
	 * @param rowsPushedOut  Rows to push out.  If < 0, treat this as if nothing is pushed out.
	 * @param rowsPushedIn  Rows to push in AFTER handling the push-out.  Summing them
	 * 			(assuming rowsPushOut is >= 0) gives the ultimate position.
	 * @return
	 */
	public long timeToPushDistance( int rowsPushedOut, int rowsPushedIn ) {
		
		// if rowsPushedOut < 0, we push out immediately without any push-in.
		// Include the time to rise to the overextended position, and to fall back into place.
		if ( rowsPushedOut < 0 ) {
			return this.rowPushIn_timeToPushDistance[rowsPushedIn] +
					this.rowPushIn_timeToPushDistance[0] ;
		}
		
		// if rowsPushIn < 0, perform the same in the opposite direction.
		if ( rowsPushedIn < 0 ) {
			return this.rowPushOut_timeToPushDistance[rowsPushedOut] +
					this.rowPushOut_timeToPushDistance[0] ;
		}
		
		// If rowsPushedIn <= rowsPushedOut, we end at a position lower than or
		// equal to where we started.  We use only the 'pushOut' parabola for this.
		// Time to push to overextended position, then return to the number pushed in.
		if ( rowsPushedIn <= rowsPushedOut ) {
			return this.rowPushOut_timeToPushDistance[rowsPushedOut] +
					this.rowPushOut_timeToPushDistance[rowsPushedIn] ;
		}
		
		// Otherwise, we perform the complete 'pushOut' parabola (returning to
		// our starting location), and then 'pushIn' the remaining rows.
		// This push-in includes its own overextension.
		return 2 * this.rowPushOut_timeToPushDistance[rowsPushedOut] +
				this.rowPushIn_timeToPushDistance[rowsPushedIn - rowsPushedOut] +
				this.rowPushIn_timeToPushDistance[0] ;
	}
	
	
	
	public long timeToPushMinimum( int rowsPushedOut, int rowsPushedIn ) {
		if ( rowsPushedOut < 0 ) {
			// we immediately start to rise.
			return 0 ;
		}
		
		return this.rowPushOut_timeToPushDistance[rowsPushedOut] ;
	}
	
	
	/**
	 * Returns, as a real number of rows, the distance from 0 after animating
	 * the push for the specified number of milliseconds.
	 * 
	 * @param millis
	 * @param rowsToPushOut
	 * @param rowsToPushIn
	 * @return
	 */
	public float distanceRowsPushed( long millis, int rowsToPushOut, int rowsToPushIn ) {
		// truncate: once we reach our destination, we stop moving.
		if ( this.timeToPushDistance(rowsToPushOut, rowsToPushIn) < millis )
			return (rowsToPushIn >= 0 ? rowsToPushIn : 0 ) - ( rowsToPushOut >= 0 ? rowsToPushOut : 0 ) ;
		
		// set the appropriate quadratics.
		if ( rowsToPushOut != this.rowPushOut_last || rowsToPushIn != this.rowPushIn_last ) {
			// set 'push out' to have a root at 0 (starting time) and an optimum
			// of -( rowsToPushOut + overextend ).
			if ( rowsToPushOut >= 0 ) {
				this.rowPushOut_positionAtSeconds.setWithAccelOptimumAndFirstRoot(
						this.rowPushOut_blocksPerSecond2,
						-rowsToPushOut - this.rowPushOut_overExtend,
						0 ) ;
			}
			
			// setting 'push in' is slightly more complicated.
			if ( rowsToPushIn > 0 ) {
				if ( rowsToPushOut < 0 ) {
					// If no push-out, 'push in' has root 0 and optimum
					// rowsToPushIn + overextend.
					this.rowPushIn_positionAtSeconds.setWithAccelOptimumAndFirstRoot(
							-this.rowPushIn_blocksPerSecond2,
							rowsToPushIn + this.rowPushIn_overExtend,
							0 ) ;
				}
				
				else if ( rowsToPushOut < rowsToPushIn ) {
					// net increase.  Set 'push in' with optimum 
					// rowsToPushIn + overextend - rowsToPushOut, root
					// at the moment the push-out quadratic returns to 0.
					double pushOutZeroX = this.rowPushOut_positionAtSeconds.getRoot(
							this.rowPushOut_positionAtSeconds.numRoots() - 1) ;
					this.rowPushIn_positionAtSeconds.setWithAccelOptimumAndFirstRoot(
							-this.rowPushIn_blocksPerSecond2,
							rowsToPushIn + this.rowPushIn_overExtend - rowsToPushOut,
							pushOutZeroX) ;
					
					// this is the only condition in which we use an interpolated function.
					this.rowPushOutIn_positionAtSeconds.set(
							rowPushOut_positionAtSeconds,
							rowPushIn_positionAtSeconds) ;
				}
				
				else {
					// we push out and then in, but not by enough to grow past
					// our starting position.  This does not require a push-in
					// curve; we just return down the other side of the push-out.
				}
			}
			
			this.rowPushOut_last = rowsToPushOut ;
			this.rowPushIn_last = rowsToPushIn ;
		}
		
		// If we are only pushing IN, we use the standard IN parabola.
		if ( rowsToPushOut < 0 ) {
			return (float)this.rowPushIn_positionAtSeconds.at( millis / 1000.0 ) ;
		}
		
		// If we are only pushing OUT, or the push IN leaves us <= to where we started,
		// use the standard OUT parabola.
		if ( rowsToPushIn <= rowsToPushOut ) {
			return (float)this.rowPushOut_positionAtSeconds.at( millis / 1000.0 ) ;
		}
		
		// If we are pushing OUT then IN, use the interpolated quadratic.
		return (float)this.rowPushOutIn_positionAtSeconds.at( millis / 1000.0 ) ;
	}
	
	public float riseFadeDistanceRisen( long millisecondsSpentRising, int heightAvailable ) {
		// distance risen is linear; after time_riseFade we will be
		// at Math.min(heightAvailable, riseFade_maxDistance).
		double alpha = Math.min( ((double)millisecondsSpentRising) / (double)time_riseFade, 1.0) ;
		double height = alpha * Math.min( heightAvailable, riseFade_maxDistance ) ;
		return (float)height ;
	}
	
	public int riseFadeAlpha( int startAlpha, int endAlpha, long millisecondsSpentRising ) {
		double alpha = Math.min( ((double)millisecondsSpentRising) / (double)time_riseFade, 1.0) ;
		return (int)Math.round(startAlpha + alpha*(endAlpha - startAlpha)) ;
	}
	
	public long riseFadeTotalTime( float startAlpha, float endAlpha, int heightAvailable ) {
		return time_riseFade ;
	}
	
	public float riseFadeAlpha( float startAlpha, float endAlpha, long millisecondsSpentRising ) {
		double alpha = Math.min( ((double)millisecondsSpentRising) / (double)time_riseFade, 1.0) ;
		return (float)(startAlpha + alpha*(endAlpha - startAlpha)) ;
	}
	
	public float unlockColumnYBlockOffset( long millisecondsSpent ) {
		if ( millisecondsSpent < this.time_unlockColumnFade + this.time_unlockColumnLinger )
			return 0 ;
		float t = ((float) (millisecondsSpent - this.time_unlockColumnFade - this.time_unlockColumnLinger) /1000) ;
		return (0.5f) * t * t * unlockColumn_blocksPerSecond2 + t * unlockColumn_blocksPerSecond ;
	}
	
	public long unlockColumnTimeToReachYBlockOffset( float blockOffset ) {
		long stationaryTime = this.time_unlockColumnFade + this.time_unlockColumnLinger ;
		
		// After this amount of time, we start moving.
		// Use the quadratic formula:
		// 0 = at^2 + bt + c
		// t = (-b + sqrt(b^2 - 4ac)) / 2a
		// a = unlockColumn_blocksPerSecond2
		// b = unlockColumn_blocksPerSecond
		// c = -blockOffset
		
		float a = (0.5f)*unlockColumn_blocksPerSecond2 ;
		float b = unlockColumn_blocksPerSecond ;
		float c = -blockOffset ;
		double t = (-b + Math.sqrt(b*b - 4*a*c)) / ( 2 * a ) ;
		return Math.round(t * 1000) + stationaryTime ;
	}
	
	public float distanceClearedX( long millisecondsSpentClearing ) {
		float t = ((float)millisecondsSpentClearing)/1000 ;
		return (0.5f) * t * t * clear_xBlocksPerSecond2 + t * clear_xBlocksPerSecond ;
	}
	
	public float distanceClearedY( long millisecondsSpentClearing ) {
		float t = ((float)millisecondsSpentClearing)/1000 ;
		return (0.5f) * t * t * clear_yBlocksPerSecond2 + t * clear_yBlocksPerSecond ;
	}
	
	
	public int boxFadeAlpha( int startAlpha, int endAlpha, long millisecondsSpentFading ) {
		double alpha = Math.min( ((double)millisecondsSpentFading) / (double)time_boxFade, 1.0) ;
		return (int)Math.round(startAlpha + alpha*(endAlpha - startAlpha)) ;
	}
	
	public long boxFadeTotalTime( float startAlpha, float endAlpha ) {
		return time_boxFade ;
	}
	
	public float boxFadeAlpha( float startAlpha, float endAlpha, long millisecondsSpentFading ) {
		double alpha = Math.min( ((double)millisecondsSpentFading) / (double)time_boxFade, 1.0) ;
		return (float)(startAlpha + alpha*(endAlpha - startAlpha)) ;
	}
	
	public int lockFillAlpha( int startAlpha, int endAlpha, long millisecondsSpent ) {
		return interpolate( startAlpha, endAlpha, millisecondsSpent,
				useRate_lockFillAlphaFade, rate_lockFillAlphaFade, time_lockFillAlphaFade) ;
	}
	
	public int lockTopAlpha( int startAlpha, int endAlpha, long millisecondsSpent ) {
		return interpolate( startAlpha, endAlpha, millisecondsSpent,
				useRate_lockTopAlphaFade, rate_lockTopAlphaFade, time_lockTopAlphaFade) ;
	}
	
	public int lockBorderAlpha( int startAlpha, int endAlpha, long millisecondsSpent ) {
		return interpolate( startAlpha, endAlpha, millisecondsSpent,
				useRate_lockBorderAlphaFade, rate_lockBorderAlphaFade, time_lockBorderAlphaFade) ;
	}
	
	public int clearEmphAlpha( int startAlpha, int endAlpha, long millisecondsSpent ) {
		return interpolateByTime( startAlpha, endAlpha, millisecondsSpent,
				time_clearEmphFade) ;
	}
	
	public boolean clearEmphFinished( int startAlpha, int endAlpha, long millisecondsSpent ) {
		return interpolateByTime( startAlpha, endAlpha, millisecondsSpent, time_clearEmphFade ) == endAlpha ;
	}
	
	public long clearEmphTotalTime( int startAlpha, int endAlpha ) {
		if ( startAlpha == endAlpha )
			return 0 ;
		else
			return time_clearEmphFade ;
	}

	public int unlockRingAlpha( int startAlpha, int endAlpha, long millisecondsSpent ) {
		return interpolateByTime( startAlpha, endAlpha, millisecondsSpent,
				time_unlockColumnFade ) ;
	}
	
	public long lockGlowTimeToPeak( int startAlpha, int maxAlpha, int endAlpha ) {
		return time_lockGlowFadeIn ;
	}

	public boolean lockGlowFinished( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_lockGlowFadeIn + time_lockGlowLinger + time_lockGlowFadeOut ;
	}
	
	public boolean lockGlowFinished( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_lockGlowFadeIn + time_lockGlowLinger + time_lockGlowFadeOut ;
	}
	
	public long lockGlowTimeToFinish( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_lockGlowFadeIn + time_lockGlowLinger + time_lockGlowFadeOut ;
	}
	
	public boolean metamorphosisGlowFinished( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_metamorphosisGlowFadeIn + time_metamorphosisGlowLinger + time_metamorphosisGlowFadeOut ;
	}
	
	public boolean metamorphosisGlowFinished( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_metamorphosisGlowFadeIn + time_metamorphosisGlowLinger + time_metamorphosisGlowFadeOut ;
	}
	
	public boolean metamorphosisGlowPeaked( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_metamorphosisGlowFadeIn ;
	}
	
	public long metamorphosisGlowTimeToPeak( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_metamorphosisGlowFadeIn ;
	}
	
	public long metamorphosisGlowTimeToFinish( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_metamorphosisGlowFadeIn + time_metamorphosisGlowLinger + time_metamorphosisGlowFadeOut ;
	}

	
	
	public boolean unlockColumnGlowFinished( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger + time_unlockColumnGlowFadeOut ;
	}
	
	public boolean unlockColumnGlowFinished( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger + time_unlockColumnGlowFadeOut ;
	}
	
	
	public int lockGlowAlpha( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_lockGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_lockGlowFadeIn ) ;
		else if ( millisecondsSpent < time_lockGlowFadeIn + time_lockGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_lockGlowFadeIn + time_lockGlowLinger ), time_lockGlowFadeOut ) ;
	}
	
	public float lockGlowAlpha( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_lockGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_lockGlowFadeIn ) ;
		else if ( millisecondsSpent < time_lockGlowFadeIn + time_lockGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_lockGlowFadeIn + time_lockGlowLinger ), time_lockGlowFadeOut ) ;
	}
	
	public int metamorphosisGlowAlpha( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_metamorphosisGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_metamorphosisGlowFadeIn ) ;
		else if ( millisecondsSpent < time_metamorphosisGlowFadeIn + time_metamorphosisGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_metamorphosisGlowFadeIn + time_metamorphosisGlowLinger ), time_metamorphosisGlowFadeOut ) ;
	}
	
	public float metamorphosisGlowAlpha( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_metamorphosisGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_metamorphosisGlowFadeIn ) ;
		else if ( millisecondsSpent < time_metamorphosisGlowFadeIn + time_metamorphosisGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_metamorphosisGlowFadeIn + time_metamorphosisGlowLinger ), time_metamorphosisGlowFadeOut ) ;
	}
	
	public int unlockColumnGlowAlpha( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_unlockColumnGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_unlockColumnGlowFadeIn ) ;
		else if ( millisecondsSpent < time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger ), time_unlockColumnGlowFadeOut ) ;
	}
	
	public float unlockColumnGlowAlpha( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_unlockColumnGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_unlockColumnGlowFadeIn ) ;
		else if ( millisecondsSpent < time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger ), time_unlockColumnGlowFadeOut ) ;
	}
	
	public int enterGlowAlpha( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_enterGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_enterGlowFadeIn ) ;
		else if ( millisecondsSpent < time_enterGlowFadeIn + time_enterGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_enterGlowFadeIn + time_enterGlowLinger ), time_enterGlowFadeOut ) ;
	}
	
	public float enterGlowAlpha( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_enterGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_enterGlowFadeIn ) ;
		else if ( millisecondsSpent < time_enterGlowFadeIn + time_enterGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_enterGlowFadeIn + time_enterGlowLinger ), time_enterGlowFadeOut ) ;
	}
	
	public long enterGlowTimeToFinish( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_enterGlowFadeIn + time_enterGlowLinger + time_enterGlowFadeOut ;
	}
	
	public boolean enterGlowFinished( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_enterGlowFadeIn + time_enterGlowLinger + time_enterGlowFadeOut ;
	}
	
	public boolean enterGlowFinished( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_enterGlowFadeIn + time_enterGlowLinger + time_enterGlowFadeOut ;
	}
	
	public long enterGlowTimeToPeak( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_enterGlowFadeIn ;
	}
	
	public boolean enterGlowPeaked( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_enterGlowFadeIn ;
	}
	
	public long enterGlowTimeToExitPeak( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_enterGlowFadeIn + time_enterGlowLinger ;
	}
	
	public boolean enterGlowFinishedPeaked( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_enterGlowFadeIn + time_enterGlowLinger ;
	}
	
	
	public int pulseAlpha( int startAlpha, int endAlpha, long millisecondsSpent ) {
		return (int)(startAlpha + (endAlpha - startAlpha) * interpolation_pulseCycle.doubleAt(millisecondsSpent)) ;
	}
	
	public float pulseAlpha( float [] alphaScales, long millisecondsSpent ) {
		return (float)(alphaScales[0] + (alphaScales[1] - alphaScales[0]) * interpolation_pulseCycle.doubleAt(millisecondsSpent)) ;
	}
	
	
	public long unlockColumnGlowExitPeakTime( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger ;
	}
	
	public long unlockColumnGlowTotalTime( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_unlockColumnGlowFadeIn + time_unlockColumnGlowLinger + time_unlockColumnGlowFadeOut ;
	}
	
	public long clearGlowTimeToFinish( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_clearGlowFadeIn + time_clearGlowLinger + time_clearGlowFadeOut ;
	}
	
	public boolean clearGlowFinished( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_clearGlowFadeIn + time_clearGlowLinger + time_clearGlowFadeOut ;
	}
	
	public boolean clearGlowFinished( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_clearGlowFadeIn + time_clearGlowLinger + time_clearGlowFadeOut ;
	}
	
	public long clearGlowTimeToPeak( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_clearGlowFadeIn ;
	}
	
	public boolean clearGlowPeaked( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_clearGlowFadeIn ;
	}
	
	public long clearGlowTimeToExitPeak( float startAlpha, float maxAlpha, float endAlpha ) {
		return time_clearGlowFadeIn + time_clearGlowLinger;
	}
	
	public boolean clearGlowFinishedPeaked( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		return millisecondsSpent >= time_clearGlowFadeIn + time_clearGlowLinger ;
	}
	
	public int clearGlowAlpha( int startAlpha, int maxAlpha, int endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_lockGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_clearGlowFadeIn ) ;
		else if ( millisecondsSpent < time_clearGlowFadeIn + time_clearGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_clearGlowFadeIn + time_clearGlowLinger ), time_clearGlowFadeOut ) ;
	}
	
	public float clearGlowAlpha( float startAlpha, float maxAlpha, float endAlpha, long millisecondsSpent ) {
		if ( millisecondsSpent < time_clearGlowFadeIn )
			return interpolateByTime( startAlpha, maxAlpha, millisecondsSpent, time_clearGlowFadeIn ) ;
		else if ( millisecondsSpent < time_clearGlowFadeIn + time_clearGlowLinger )
			return maxAlpha ;
		else
			return interpolateByTime( maxAlpha, endAlpha,
					millisecondsSpent - ( time_clearGlowFadeIn + time_clearGlowLinger ), time_clearGlowFadeOut ) ;
	}
	
	
	private int interpolate( int startVal, int endVal, long millisSpent, boolean useRate, float rate, long time ) {
		if ( useRate )
			return interpolateByRate( startVal, endVal, millisSpent, rate ) ;
		else
			return interpolateByTime( startVal, endVal, millisSpent, time ) ;
	}
	
	private int interpolateByRate( int startVal, int endVal, long millisSpent, float rate ) {
		if ( endVal < startVal )
			rate *= -1 ;
		int val = (int) Math.round( startVal + rate * (millisSpent / 1000.0f) ) ;
		return setWithinBounds( startVal, endVal, val ) ;
	}
	
	private int interpolateByTime( int startVal, int endVal, long millisSpent, long time ) {
		int val = (int) Math.round( startVal + (endVal - startVal) * ((double)millisSpent / time) ) ;
		return setWithinBounds( startVal, endVal, val ) ;
	}
	
	public int setWithinBounds( int a, int b, int val ) {
		return ( a < b ) ? Math.max(a, Math.min(b, val)) : Math.max(b, Math.min(a, val)) ;
	}
	
	// FLOATERPOLATE!
	@SuppressWarnings("unused")
	private float interpolate( float startVal, float endVal, long millisSpent, boolean useRate, float rate, long time ) {
		if ( useRate )
			return interpolateByRate( startVal, endVal, millisSpent, rate ) ;
		else
			return interpolateByTime( startVal, endVal, millisSpent, time ) ;
	}
	
	private float interpolateByRate( float startVal, float endVal, long millisSpent, float rate ) {
		if ( endVal < startVal )
			rate *= -1 ;
		float val = startVal + rate * (millisSpent / 1000.0f) ;
		return setWithinBounds( startVal, endVal, val ) ;
	}
	
	private float interpolateByTime( float startVal, float endVal, long millisSpent, long time ) {
		float val = (float)(startVal + (endVal - startVal) * ((double)millisSpent / time)) ;
		return setWithinBounds( startVal, endVal, val ) ;
	}
	
	public float setWithinBounds( float a, float b, float val ) {
		return ( a < b ) ? Math.max(a, Math.min(b, val)) : Math.max(b, Math.min(a, val)) ;
	}
}
