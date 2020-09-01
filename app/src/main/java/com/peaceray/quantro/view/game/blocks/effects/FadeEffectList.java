package com.peaceray.quantro.view.game.blocks.effects;

public class FadeEffectList extends EffectList<FadeEffect> {
	
	int mR, mC ;
	
	public FadeEffectList( int R, int C ) {
		super() ;
		mR = R ;
		mC = C ;
		
		if ( mR <= 0 || mC <= 0 ) {
			throw new IllegalArgumentException("Must provide positive Rows / Cols") ;
		}
	}

	@Override
	protected FadeEffect newEffect(Object key) {
		return new FadeEffect( mR, mC, key ) ;
	}

	public synchronized FadeEffect.Setter add() {
		Effect.Setter setter = super.addSetter();
		return (FadeEffect.Setter)setter;
	}
}
