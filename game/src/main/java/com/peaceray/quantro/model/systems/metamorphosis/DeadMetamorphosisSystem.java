package com.peaceray.quantro.model.systems.metamorphosis;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.q.QInteractions;

public class DeadMetamorphosisSystem extends QuantroMetamorphosisSystem {

	// Constructor!
	public DeadMetamorphosisSystem( GameInformation ginfo, QInteractions qi ) {
		super( ginfo, qi ) ;
	}
	
	@Override
	public Result endCycle( byte [][][] field ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		result.clear() ;
		return result ;
	}
	
}
