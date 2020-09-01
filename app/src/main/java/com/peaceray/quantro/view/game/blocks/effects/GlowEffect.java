package com.peaceray.quantro.view.game.blocks.effects;

import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.view.game.blocks.Consts;

import android.util.Log;


/**
 * A 'glow' represents a unique set of glow information.  Like most effects,
 * 'glows' do NOT contain specific details about how they are to be drawn
 * (e.g. bitmaps, colors, etc.).  Instead, a glow represents "skin-agnostic"
 * information: the glowSet indices, the QOrientation which triggered the
 * glow, the type of glow (lock, clear, etc.), glow timing (represented
 * as millis from slice or unpaused time).
 * 
 * The specifics for drawing an effect is left to the the BlockDrawer.  This
 * is just a glorified container object: since all related metadata relates
 * to a single "event," we package it all together rather than in a set of
 * seemingly unrelated arrays as before this refactor.
 * 
 * Access is expected to occur during a draw loop, so we provide a very
 * lightweight interface (including direct access 2d DrawCode array) to
 * help speed up draw operations.
 * 
 * We also expect GlowEffects to be allocated once and then used to represent
 * many different independent glows (rather than a fresh allocation per
 * in-game glow).  We therefore provide a minimal constructor (allocation
 * of inner arrays, but no data members set) and a helper "Setter" class
 * which can be used to configure data members without additional allocations
 * in a reasonably state-safe way.  GlowEffect instances should be treated
 * as 2-state objects: it is either configured ("set") or unconfigured ("unset").
 * 
 * GlowEffects are constructed "unset."  Data member access is forbidden and
 * access attempts prompt an IllegalStateException.  Calling "getSetter()" will
 * return a Setter object appropriate for configuring the data members.  Once
 * they are configured, calling setter.set() will transition the GlowEffect
 * to a "set" state, where it operates as essentially read-only.
 * 
 * Recommended usage is to treat all data members as immutable (despite having
 * direct access to some of them) and only configure a glow effect
 * through the 'setter.'
 * 
 * One final complication: to help the safe implementation of a "GlowEffectList,"
 * an instance of GlowEffect may have an "owner."  Only the owner is allowed to
 * retrieve the setter, or to call 'set' on it.  Owners should use a privately
 * held "key" (e.g. one constructed with new Object()) to prove their ownership.
 * 
 * Keys are set -- or not set, for unowned GlowEffects -- at construction time.
 * Keys are compared using direct object equality ( "==" ) so use the same instance
 * later, not one with the same value.
 * 
 * @author Jake
 *
 */
public class GlowEffect extends Effect {
	
	private static final String TAG = "GlowEffect" ;

	// Glow Effect Types.  Different glow types may be drawn in different ways;
	// for example, in Quantro standard skin, lock glows are drawn in a single pass
	// using QO color, clear glows in 2 passes -- first with QPane color, second w/
	// white.
	public static final int TYPE_ANY = -2 ;
	public static final int TYPE_NONE = -1 ;
	public static final int TYPE_LOCK = Consts.GLOW_LOCK ;
	public static final int TYPE_CLEAR = Consts.GLOW_CLEAR ;
	public static final int TYPE_METAMORPHOSIS = Consts.GLOW_METAMORPHOSIS ;
	public static final int TYPE_UNLOCK = Consts.GLOW_UNLOCK ;
	public static final int TYPE_ENTER = Consts.GLOW_ENTER ;
	public static final int NUM_TYPES = Consts.NUM_GLOWS ;
	public static final int MIN_TYPE = 0 ;
	
	// A safety feature.  Currently, we use the same TYPE_* codes as
	// those provided by Consts, but this is not guaranteed.
	// This method converts from TYPE to the Consts value.
	public static final int typeToConstGlowIndex( int type ) {
		if ( MIN_TYPE <= type && type < NUM_TYPES )
			return type ;
		throw new IllegalArgumentException("Provided type " + type + " is not one of Consts.GLOW_*") ;
	}
	
	// ******* A GLOW EFFECT HAS: ******** 
	// A glow type
	// A QOrientation (e.g. triggering piece, underlying block, etc.)
	// A start time.  Start time can be represented as either
	//		1. time since slice start, or 2. unpaused time.
	// A Q-Pane
	// A blockfield-sized array of shorts, indicating "draw codes"
	//		for the actual draws.  At present, all glows are represented
	//		using a "neighbor" encoding, with each "neighbor" being a block
	//		of color blurred into the center.  There are thus 256 possible
	//		values, plus an optional offset indicating a custom value.
	//
	//		Other skin types might store different information here in
	//		its own short encoding.
	
	private int mType ;
	private int mQOrientation ;
	private int mQPane ;
	private short [][] mDrawCode ;
	
	public GlowEffect( int R, int C ) {
		super() ;
		mType = TYPE_NONE ;
		mDrawCode = new short[R][C] ;
	}
	
	public GlowEffect( int R, int C, Object key ) {
		super(key) ;
		mType = TYPE_NONE ;
		mDrawCode = new short[R][C] ;
	}
	
	public boolean isType( int type ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		switch( type ) {
		case TYPE_ANY:
			return true ;
		case TYPE_NONE:
			return false ;
		}
		
		return type == mType ;
	}

	public int type() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		return mType ;
	}
	
	public int qOrientation() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		return mQOrientation ;
	}
	
	
	public int qPane() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return mQPane ;
	}
	
	public short [][] directDrawCodeAccess() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		return mDrawCode ;
	}
	
	/**
	 * Translates the content (available in directDrawCodeAccess) by
	 * the specified amount.  This is useful, e.g., if you are adding or
	 * removing rows.
	 * 
	 * @param rOff
	 * @param cOff
	 * @param fillWith: translation raises the issue of what do you
	 * 		fill in from the edges?  This value is used.
	 */
	public void translate( int rOff, int cOff, short fillWith ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		int rMin = Math.min(0, rOff) ;
		int rMax = Math.max(mDrawCode.length, mDrawCode.length + rOff) ;
		int cMin = Math.min(0, cOff) ;
		int cMax = Math.max(mDrawCode[0].length, mDrawCode[0].length + cOff) ;
		
		// translate in one dimension, then the other.  Note that,
		// for safety, we need to iterate in the OPPOSITE direction from the offset,
		// so that (e.g.) we copy from the right ("shift left")
		// before we overwrite that value.
		int rStart, rIter, cStart, cIter ;
		if ( rOff <= 0 ) {
			rStart = rMin ;
			rIter = 1 ;
		} else {
			rStart = rMax-1 ;
			rIter = -1 ;
		}
		
		if ( cOff <= 0 ) {
			cStart = cMin ;
			cIter = 1 ;
		} else {
			cStart = cMax-1 ;
			cIter = -1 ;
		}
		
		// shift by rows
		for ( int r = rStart; rMin <= r && r < rMax; r += rIter ) {
			if ( 0 <= r && r < mDrawCode.length ) {
				int rSrc = r - rOff ;
				for ( int c = 0; c < mDrawCode[0].length; c++ ) {
					mDrawCode[r][c] = (rSrc < 0 || mDrawCode.length <= rSrc)
						? fillWith
						: mDrawCode[rSrc][c] ;
				}
			}
		}
		
		for ( int c = cStart; cMin <= c && c < cMax; c += cIter ) {
			if ( 0 <= c && c < mDrawCode[0].length ) {
				int cSrc = c - cOff ;
				for ( int r = 0; r < mDrawCode.length; r++ ) {
					mDrawCode[r][c] = (cSrc < 0 || mDrawCode[0].length <= cSrc)
							? fillWith
							: mDrawCode[r][cSrc] ;
				}
			}
		}
	}
	
	
	protected Setter makeSetter() {
		return new Setter() ;
	}
	
	
	/**
	 * A 'Setter' provides the benefits of a 'Builder' without performing
	 * any memory allocation.  Setters impose certain patterns of use; once
	 * a Setter is retrieved, direct access to the GlowEffect is prevented
	 * on pain of IllegalStateException.  Only once ALL data members have
	 * been set (or accessed, in the case of DrawCode) and 'set' called
	 * can the GlowEffect be directly accessed again.
	 * 
	 * On the other hand, calling 'set' will invalidate the Setter itself,
	 * and further access will itself produce an IllegalStateException. 
	 * The same is true if 'set' is called prematurely (before all data
	 * members of the GlowEffect have been configured).
	 * 
	 * @author Jake
	 *
	 */
	public class Setter extends Effect.Setter {
		
		boolean mTypeSet ;
		boolean mQOrientationSet ;
		boolean mQPaneSet ;
		boolean mDrawCodeSet ;
		
		private Setter() {
			super() ;
			mTypeSet = false ;
			mQOrientationSet = false ;
			mQPaneSet = false ;
			mDrawCodeSet = false ;
		}
		
		protected boolean performSet() {
			if ( !mTypeSet )
				throw new IllegalStateException("Must provide a type before setting") ;
			if ( !mQOrientationSet )
				throw new IllegalStateException("Must provide a qOrientation before setting") ;
			if ( !mQPaneSet )
				throw new IllegalStateException("Must provide a qPane before setting") ;
			if ( !mDrawCodeSet )
				throw new IllegalStateException("Must provide draw codes before setting") ;
			
			// we have been making changes to the underlying object this whole time.
			return true ;
		}

		protected boolean performReset() {
			mTypeSet = false ;
			mQOrientationSet = false ;
			mQPaneSet = false ;
			mDrawCodeSet = false ;
			
			return true ;
		}
		
		
		public Setter type( int type ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mType = type ;
			mTypeSet = true ;
			
			return this ;
		}
		
		public Setter qOrientation( int qo ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mQOrientation = qo ;
			mQOrientationSet = true ;
			
			return this ;
		}
		
		public Setter qPane( int qPane ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mQPane = qPane ;
			mQPaneSet = true ;
			
			return this ;
		}
		
		public short [][] directDrawCodeAccess() {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mDrawCodeSet = true ;
			
			return mDrawCode ;
		}
		
		
		
		////////////////////////////////////////////////////////////////////////
		// 
		// Override superclass setters for easier chaining
		
		public Setter startTimeSlice( long time ) {
			return (GlowEffect.Setter) super.startTimeSlice(time) ;
		}

		@Override
		public Setter startTimeUnpaused( long time ) {
			return (GlowEffect.Setter) super.startTimeUnpaused(time) ;
		}
		
		@Override
		public Setter duration( long duration ) {
			return (GlowEffect.Setter) super.duration(duration) ;
		}
		
	}
}
