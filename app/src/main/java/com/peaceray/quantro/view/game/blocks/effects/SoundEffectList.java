package com.peaceray.quantro.view.game.blocks.effects;

import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;


/**
 * A SoundEffectList differs for the standard EffectList mold, in the same way that
 * a SoundEffect differs from a standard Effect.
 * 
 * SoundEffects are played exactly once; the "duration" specifies a window in
 * which they SHOULD be played.  If we miss the window completely by not drawing
 * an animation frame during that time, we simply skip the sound effect without
 * playing it.
 * 
 * @author Jake
 *
 */
public class SoundEffectList extends EffectList<SoundEffect> {

	private long mStandardDuration ;
	
	public SoundEffectList( long standardDuration ) {
		super() ;
		mStandardDuration = standardDuration ;
		
		if ( mStandardDuration <= 0 )
			throw new IllegalArgumentException("Must specify a standard duration > 0") ;
	}
	
	
	@Override
	protected SoundEffect newEffect(Object key) {
		return new SoundEffect( key ) ;
	}
	
	public synchronized SoundEffect.Setter add() {
		Effect.Setter setter = super.addSetter();
		return (SoundEffect.Setter)setter;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SOUND EFFECT FACTORY METHODS
	//
	// To simplify SoundEffect creation, these factory methods perform all the
	// necessary 'sets' and 'commits.'
	
	public void addLock( BlockDrawerSliceTime.RelativeTo relTo, long time, int pieceType, boolean isPieceComponent, int qCombination ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.lock(pieceType, isPieceComponent, qCombination) ;
		commit(setter) ;
	}
	
	public void addLand( BlockDrawerSliceTime.RelativeTo relTo, long time, int pieceType, boolean isPieceComponent, int qCombination ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.land(pieceType, isPieceComponent, qCombination) ;
		commit(setter) ;
	}
	
	public void addClearEmphasis( BlockDrawerSliceTime.RelativeTo relTo, long time, int pieceType, int cascadeNumber, int [] clears, boolean [] monoClears ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.clearEmphasis(pieceType, cascadeNumber, clears, monoClears) ;
		commit(setter) ;
	}
	
	public void addClear( BlockDrawerSliceTime.RelativeTo relTo, long time, int pieceType, int cascadeNumber, int [] clears, boolean [] monoClears ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.clear(pieceType, cascadeNumber, clears, monoClears) ;
		commit(setter) ;
	}
	
	public void addMetamorphosis( BlockDrawerSliceTime.RelativeTo relTo, long time, int qCombinationFrom, int qCombinationTo ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.metamorphosis(qCombinationFrom, qCombinationTo) ;
		commit(setter) ;
	}
	
	
	public void addRiseAndFade( BlockDrawerSliceTime.RelativeTo relTo, long time, int pieceType ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.riseAndFade(pieceType) ;
		commit(setter) ;
	}
	
	public void addColumnUnlocked( BlockDrawerSliceTime.RelativeTo relTo, long time, int pieceType ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.columnUnlocked(pieceType) ;
		commit(setter) ;
	}
	
	public void addGarbageRows( BlockDrawerSliceTime.RelativeTo relTo, long time, int rows ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.garbageRows(rows) ;
		commit(setter) ;
	}
	
	public void addAddRows( BlockDrawerSliceTime.RelativeTo relTo, long time, int rows ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.pushRows(rows) ;
		commit(setter) ;
	}
	
	public void addEnter( BlockDrawerSliceTime.RelativeTo relTo, long time, int qCombination ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.enter(qCombination) ;
		commit(setter) ;
	}
	
	public void addPenalty( BlockDrawerSliceTime.RelativeTo relTo, long time, int qCombination ) {
		SoundEffect.Setter setter = (SoundEffect.Setter)add() ;
		setter.startTime(relTo, time).duration(mStandardDuration)
			.penalty(qCombination) ;
		commit(setter) ;
	}
}
