package com.peaceray.quantro.view.game.blocks.effects;

/**
 * A wrapper for all types of EffectLists.  Mostly here for
 * convenience.
 * 
 * @author Jake
 *
 */
public class EffectListCollection {
	
	private static final int LIST_FADE = 0 ;
	private static final int LIST_GLOW = 1 ;
	private static final int LIST_SOUND = 2 ;
	private static final int NUM_LISTS = 3 ;
	

	@SuppressWarnings("rawtypes")
	private EffectList [] mEffectLists ;
	
	
	public EffectListCollection( int R, int C, long standardSoundEffectDuration ) {
		mEffectLists = new EffectList[NUM_LISTS] ;
		mEffectLists[LIST_FADE] = new FadeEffectList( R, C ) ;
		mEffectLists[LIST_GLOW] = new GlowEffectList( R, C ) ;
		mEffectLists[LIST_SOUND] = new SoundEffectList( standardSoundEffectDuration ) ;
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INDIRECT EFFECT ADDITION
	//
	
	public FadeEffect.Setter addFadeEffect() {
		return (FadeEffect.Setter) mEffectLists[LIST_FADE].add() ;
	}
	
	public GlowEffect.Setter addGlowEffect() {
		return (GlowEffect.Setter) mEffectLists[LIST_GLOW].add() ;
	}
	
	public SoundEffect.Setter addSoundEffect() {
		return (SoundEffect.Setter) mEffectLists[LIST_SOUND].add() ;
	}
	
	public void commit( Effect.Setter setter ) {
		if ( setter instanceof FadeEffect.Setter ) {
			commit((FadeEffect.Setter)setter) ;
		} else if ( setter instanceof GlowEffect.Setter ) {
			commit((GlowEffect.Setter)setter) ;
		} else if ( setter instanceof SoundEffect.Setter ) {
			commit((SoundEffect.Setter)setter) ;
		}
	}
	
	public void commit( FadeEffect.Setter setter ) {
		mEffectLists[LIST_FADE].commit(setter) ;
	}
	
	public void commit( GlowEffect.Setter setter ) {
		mEffectLists[LIST_GLOW].commit(setter) ;
	}
	
	public void commit( SoundEffect.Setter setter ) {
		mEffectLists[LIST_SOUND].commit(setter) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
}
