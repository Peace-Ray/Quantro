package com.peaceray.quantro.view.game.blocks.effects;

public class GlowEffectList extends EffectList<GlowEffect> {

	private int mR, mC ;
	
	public GlowEffectList( int R, int C ) {
		super() ;
		mR = R ;
		mC = C ;
		
		if ( mR <= 0 || mC <= 0 ) {
			throw new IllegalArgumentException("Must provide positive Rows / Cols") ;
		}
	}
	
	public void translate( int rOff, int cOff, short fillWith ) {
		for ( int i = 0; i < size(); i++ ) {
			get(i).translate(rOff, cOff, fillWith) ;
		}
	}

	@Override
	protected GlowEffect newEffect(Object key) {
		return new GlowEffect( mR, mC, key ) ;
	}

	public synchronized GlowEffect.Setter add() {
		Effect.Setter setter = super.addSetter();
		return (GlowEffect.Setter)setter;
	}

}
