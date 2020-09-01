package com.peaceray.quantro.model.systems.trigger;

public interface Triggerable {

	void pullTrigger(int triggerNum) ;
	
	void pullTrigger(int triggerNum, Object...params) ;
}
