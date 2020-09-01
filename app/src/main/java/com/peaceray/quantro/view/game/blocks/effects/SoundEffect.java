package com.peaceray.quantro.view.game.blocks.effects;

import android.util.Log;

import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;

public class SoundEffect extends Effect {
	
	private static final String TAG = "SoundEffect" ;
	
	public enum Type {
		LAND,
		LOCK,
		CLEAR_EMPHASIS,
		CLEAR,
		METAMORPHOSIS,
		UNLOCK,
		RISE_AND_FADE,
		GARBAGE_ROWS,
		PUSH_ROWS,
		ENTER,
		PENALTY,
	}
	
	
	private Type mType ;
	
	private int mPieceType ;
	private boolean mIsPieceComponent ;
	private int mQCombination ;
	private int mQCombinationPrevious ;
	
	private int mClearCascade ;
	
	private int mNumRows ;
	
	
	
	public SoundEffect() {
		super() ;
	}
	
	public SoundEffect( Object key ) {
		super(key) ;
	}
	
	
	public void play( QuantroSoundPool pool ) {
		switch( mType ) {
		case LAND:
			pool.land(mPieceType, mIsPieceComponent, mQCombination) ;
			break ;
		case LOCK:
			pool.lock(mPieceType, mIsPieceComponent, mQCombination) ;
			break ;
		case CLEAR_EMPHASIS:
			pool.clearEmph(mPieceType, mClearCascade, null, null) ;
			break ;
		case CLEAR:
			pool.clear(mPieceType, mClearCascade, null, null) ;
			break ;
		case METAMORPHOSIS:
			pool.metamorphosis(mQCombinationPrevious, mQCombination) ;
			break ;
		case UNLOCK:
			pool.columnUnlocked(mPieceType) ;
			break ;
		case RISE_AND_FADE:
			pool.pieceRiseFade(mPieceType) ;
			break ;
		case GARBAGE_ROWS:
			pool.garbageRows(mNumRows) ;
			break ;
		case PUSH_ROWS:
			pool.pushRows(mNumRows) ;
			break ;
		case ENTER:
			pool.enter(mQCombination) ;
			break ;
		case PENALTY:
			pool.penalty(mQCombination) ;
			break ;
		}
	}

	@Override
	protected Setter makeSetter() {
		return new Setter() ;
	}
	
	
	public class Setter extends Effect.Setter {
		
		boolean mIsSet ;
		
		protected Setter() {
			super() ;
			mIsSet = false ;
		}
		
		public Setter lock( int pieceType, boolean isPieceComponent, int qCombination ) {
			throwIfSet() ;
			
			mType = Type.LOCK ;
			mPieceType = pieceType ;
			mIsPieceComponent = isPieceComponent ;
			mQCombination = qCombination ;
			
			mIsSet = true ;
			
			return this ;
		}
		
		public Setter land( int pieceType, boolean isPieceComponent, int qCombination ) {
			throwIfSet() ;
			
			mType = Type.LAND ;
			mPieceType = pieceType ;
			mIsPieceComponent = isPieceComponent ;
			mQCombination = qCombination ;
			
			mIsSet = true ;
			
			return this ;
		}
		
		public Setter clearEmphasis( int pieceType, int cascadeNumber, int [] clears, boolean [] monoClears ) {
			throwIfSet() ;
			
			mType = Type.CLEAR_EMPHASIS ;
			mPieceType = pieceType ;
			mClearCascade = cascadeNumber ;
			
			// TODO: Store 'clears'
			// TODO: Store 'monoClears'
			
			mIsSet = true ;
			
			return this ;
		}
		
		public Setter clear( int pieceType, int cascadeNumber, int [] clears, boolean [] monoClears ) {
			throwIfSet() ;
			
			mType = Type.CLEAR ;
			mPieceType = pieceType ;
			mClearCascade = cascadeNumber ;
			
			// TODO: Store 'clears'
			// TODO: Store 'monoClears'
			
			mIsSet = true ;
			
			return this ;
		}
		
		
		
		public Setter metamorphosis( int qCombinationFrom, int qCombinationTo ) {
			throwIfSet() ;
			
			mType = Type.METAMORPHOSIS ;
			mQCombinationPrevious = qCombinationFrom ;
			mQCombination = qCombinationTo ;
			
			mIsSet = true ;
			
			return this ;
		}

		public Setter riseAndFade( int pieceType ) {
			throwIfSet() ;
			
			mType = Type.RISE_AND_FADE ;
			mPieceType = pieceType ;
			
			mIsSet = true ;
			
			return this ;
		}
		
		public Setter columnUnlocked( int pieceType ) {
			throwIfSet() ;
			
			mType = Type.UNLOCK ;
			mPieceType = pieceType ;
			
			mIsSet = true ;
			
			return this ;
		}
		
		public Setter garbageRows( int rows ) {
			throwIfSet() ;
			
			mType = Type.GARBAGE_ROWS ;
			mNumRows = rows ;
			
			mIsSet = true ;
			
			return this ;
		}

		public Setter pushRows( int rows ) {
			throwIfSet() ;
			
			mType = Type.PUSH_ROWS ;
			mNumRows = rows ;
			
			mIsSet = true ;
			
			return this ;
		}
		
		public Setter enter( int qCombination ) {
			throwIfSet() ;
			
			mType = Type.ENTER ;
			mQCombination = qCombination ;
			
			mIsSet = true ;
			
			return this ;
		}
		
		public Setter penalty( int qCombination ) {
			throwIfSet() ;
			
			mType = Type.PENALTY ;
			mQCombination = qCombination ;
			
			mIsSet = true ;
			
			return this ;
		}
		
		@Override
		protected boolean performSet() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		protected boolean performReset() {
			// TODO Auto-generated method stub
			return false;
		}
		
		
		////////////////////////////////////////////////////////////////////////
		// 
		// Override superclass setters for easier chaining
		
		public Setter startTimeSlice( long time ) {
			return (SoundEffect.Setter) super.startTimeSlice(time) ;
		}

		@Override
		public Setter startTimeUnpaused( long time ) {
			return (SoundEffect.Setter) super.startTimeUnpaused(time) ;
		}
		
		@Override
		public Setter startTime( BlockDrawerSliceTime.RelativeTo relTo, long time ) {
			return (SoundEffect.Setter) super.startTime(relTo, time) ;
		}
		
		@Override
		public Setter duration( long duration ) {
			return (SoundEffect.Setter) super.duration(duration) ;
		}
		
	}

}
