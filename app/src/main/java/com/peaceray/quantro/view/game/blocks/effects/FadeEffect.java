package com.peaceray.quantro.view.game.blocks.effects;

import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;

/**
 * FadeEffect: an Effect for rendering the slowly-fading "boxes."
 * Possibly extensible to slowly fading pulse, or even blocks
 * (e.g. 'flash') of any kind.  For now it handles 3D boxes
 * exclusively.
 * 
 * @author Jake
 *
 */
public class FadeEffect extends Effect {
	
	
	public static final int TYPE_ANY = -2 ;
	public static final int TYPE_NONE = -1 ;
	public static final int TYPE_BOX = 0 ;
	public static final int NUM_TYPES = 1 ;
	public static final int MIN_TYPE = 0 ;

	int mType ;
	byte[][][] mQOrientations ;
	byte[][][][] mQOrientationCorners ;
	short[][][] mQOrientationEncodedConnections ;
	
	
	public FadeEffect( int R, int C ) {
		super() ;
		
		mType = TYPE_NONE ;
		mQOrientations = new byte[2][R][C] ;
		mQOrientationCorners = new byte[2][4][R][C] ;
		mQOrientationEncodedConnections = new short[2][R][C] ;
	}
	
	public FadeEffect( int R, int C, Object key ) {
		super(key) ;
		
		mType = TYPE_NONE ;
		mQOrientations = new byte[2][R][C] ;
		mQOrientationCorners = new byte[2][4][R][C] ;
		mQOrientationEncodedConnections = new short[2][R][C] ;
	}
	
	public float proportionComplete( BlockDrawerSliceTime sliceTime ) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		
		long since = this.timeSinceStarted(sliceTime) ;
		long duration = this.duration() ;
		
		if ( since <= 0 )
			return 0 ;
		if ( since >= duration )
			return 1 ;
		
		return ((float)since) / ((float)duration) ;
	}
	
	public int type() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		return mType ;
	}
	
	public boolean isType(int type) {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		if ( type == TYPE_ANY )
			return true ;
		if ( type == TYPE_NONE )
			return false ;
		return type == mType ;
	}
	
	public byte[][][] directQOrientationsAccess() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		return mQOrientations ;
	}
	
	public byte[][][][] directQOrientationCornersAccess() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		return mQOrientationCorners ;
	}
	
	public short[][][] directQOrientationEncodedConnectionsAccess() {
		if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
			throwIfUnset() ;
		return mQOrientationEncodedConnections ;
	}
	
	
	@Override
	protected Setter makeSetter() {
		return new Setter() ;
	}
	
	
	public class Setter extends Effect.Setter {
		
		boolean mTypeSet ;
		boolean mQOrientationsSet ;
		boolean mQOrientationsCorners ;
		boolean mQOrientationsEncodedConnections ;
		
		
		private Setter() {
			performReset() ;
		}
		
		public Setter type( int type ) {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mType = type ;
			mTypeSet = true ;
			return this ;
		}
		
		public byte[][][] directQOrientationsAccess() {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mQOrientationsSet = true ;
			return mQOrientations ;
		}
		
		public byte[][][][] directQOrientationCornersAccess() {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mQOrientationsCorners = true ;
			return mQOrientationCorners ;
		}
		
		public short[][][] directQOrientationEncodedConnectionsAccess() {
			if ( GlobalTestSettings.BLOCK_DRAWER_STRICT_EFFECT_CHECKS )
				throwIfSet() ;
			mQOrientationsEncodedConnections = true ;
			return mQOrientationEncodedConnections ;
		}
		

		@Override
		protected boolean performSet() {
			if ( !mTypeSet )
				throw new IllegalStateException("Type not set") ;
			if ( !mQOrientationsSet )
				throw new IllegalStateException("QOrientations not set") ;
			if ( !mQOrientationsCorners )
				throw new IllegalStateException("QOrientationsCorners not set") ;
			if ( !mQOrientationsEncodedConnections )
				throw new IllegalStateException("QOrientationsEncodedConnections not set") ;
			
			return true ;
		}

		@Override
		protected boolean performReset() {
			mQOrientationsSet = false ;
			mQOrientationsCorners = false ;
			mQOrientationsEncodedConnections = false ;
			
			return true ;
		}
		
		////////////////////////////////////////////////////////////////////////
		// 
		// Override superclass setters for easier chaining
		
		public Setter startTimeSlice( long time ) {
			return (FadeEffect.Setter) super.startTimeSlice(time) ;
		}

		@Override
		public Setter startTimeUnpaused( long time ) {
			return (FadeEffect.Setter) super.startTimeUnpaused(time) ;
		}
		
		@Override
		public Setter duration( long duration ) {
			return (FadeEffect.Setter) super.duration(duration) ;
		}
		
	}

}
