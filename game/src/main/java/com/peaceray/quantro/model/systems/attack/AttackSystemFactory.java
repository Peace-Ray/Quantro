package com.peaceray.quantro.model.systems.attack;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.q.QInteractions;

public final class AttackSystemFactory {

	/**
	 * Constructs and returns an attack system designed for retro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).  Prototyping means it can be freely changed.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new retro prototyping attack system.
	 */
	public static AttackSystem newRetroVSAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		// Garbage row behavior.
		
		as.setBehavior_clearedAndSent_garbageRowComposition(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_PIECE) ;
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO) ;
		as.setBehavior_clearedAndSent_garbageRowDensity(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE ) ;
		as.setBehavior_clearedAndSent_garbageRowQueue(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_QUEUE_SETS_TOGETHER) ;
		as.setBehavior_clearedAndSent_garbageRowInclude(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO) ;
		
		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;
		
		return as ;
	}
	
	
	/**
	 * Constructs and returns an attack system designed for retro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).
	 * 
	 * Free4All attacks "alternate" between next and previous players.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new retro prototyping attack system.
	 */
	public static AttackSystem newRetroFree4AllAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		// Garbage row behavior.
		
		as.setBehavior_clearedAndSent_garbageRowComposition(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_PIECE) ;
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO) ;
		as.setBehavior_clearedAndSent_garbageRowDensity(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE ) ;
		as.setBehavior_clearedAndSent_garbageRowQueue(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_QUEUE_SETS_TOGETHER) ;
		as.setBehavior_clearedAndSent_garbageRowInclude(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO) ;
		as.setBehavior_clearedAndSent_garbageRowTarget(
				AttackSystem.TARGET_CYCLE_ALTERNATE) ;
		
		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;
		
		return as ;
	}
	
	
	/**
	 * Constructs and returns an attack system designed for retro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).  Prototyping means it can be freely changed.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new retro prototyping attack system.
	 */
	public static AttackSystem newRetroGravityAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		// Garbage row behavior.

		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;
		
		as.setBehavior_penaltyRow(
				AttackSystemAtomicConditionsVersioned.PENALTY_GARBAGE_ROW_PUSH_DOWN_CLEAR) ;
		
		as.setBehavior_pushRow(
				AttackSystemAtomicConditionsVersioned.PUSH_ROWS_PUSH_DOWN_BLOCK_ACTIVATES_OR_PUSH_UP_BLOCK_CLEARS) ;
		
		return as ;
	}
	
	
	/**
	 * Constructs and returns an attack system designed for retro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).  Prototyping means it can be freely changed.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new retro prototyping attack system.
	 */
	public static AttackSystem newRetroFloodVSAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_NONE) ;
		
		as.setBehavior_displaceRow(
				AttackSystemAtomicConditionsVersioned.DISPLACE_ACCELERATE_CLEARS_IGNORE_ONE) ;
		
		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;
		
		return as ;
	}
	
	
	public static AttackSystem newRetroFloodFree4AllAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		// By default, a Displace Row attack divides among all players.
		// That's exactly how it's supposed to work in Free4All, so just
		// make one of those.
		return newRetroFloodVSAttackSystem( ginfo, qi ) ;
	}
	
	
	/**
	 * Constructs and returns an attack system designed for Quantro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).  Prototyping means it can be freely changed.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new prototyping attack system.
	 */
	public static AttackSystem newQuantroVSAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		// Garbage row behavior.
		
		as.setBehavior_clearedAndSent_garbageRowComposition(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_ROWS) ;
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO) ;
		as.setBehavior_clearedAndSent_garbageRowDensity(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE ) ;
		as.setBehavior_clearedAndSent_garbageRowQueue(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_QUEUE_SETS_TOGETHER) ;
		as.setBehavior_clearedAndSent_garbageRowInclude(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO) ;
				
		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;

		return as ;
	}
	
	
	/**
	 * Constructs and returns an attack system designed for Quantro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).
	 * 
	 * Free-for-all garbage alternates between Next and Previous.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new prototyping attack system.
	 */
	public static AttackSystem newQuantroFree4AllAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		// Garbage row behavior.
		
		as.setBehavior_clearedAndSent_garbageRowComposition(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_ROWS) ;
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO) ;
		as.setBehavior_clearedAndSent_garbageRowDensity(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE ) ;
		as.setBehavior_clearedAndSent_garbageRowQueue(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_QUEUE_SETS_TOGETHER) ;
		as.setBehavior_clearedAndSent_garbageRowInclude(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO) ;
		as.setBehavior_clearedAndSent_garbageRowTarget(
				AttackSystem.TARGET_CYCLE_ALTERNATE) ;		
		
		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;

		return as ;
	}
	
	
	public static AttackSystem newQuantroVSAttackSystemNoGarbage(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		// Garbage row behavior.
		
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_NONE) ;
		
		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;

		return as ;
	}
	
	
	public static AttackSystem newQuantroFloodVSAttackSystemNoGarbage(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_OUTGOING ) ;
		// Garbage row behavior.
		
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_NONE) ;
		
		as.setBehavior_displaceRow(
				AttackSystemAtomicConditionsVersioned.DISPLACE_ACCELERATE_HALF_CLEARS_IGNORE_ONE) ;
		
		as.setBehavior_syncLevelUp(
				AttackSystemAtomicConditionsVersioned.SYNCRONIZED_LEVEL_UP_MAINTAIN_DIFFERENCE) ;

		return as ;
	}
	
	
	public static AttackSystem newQuantroFloodFree4AllAttackSystemNoGarbage(
			GameInformation ginfo, QInteractions qi) {
		// By default, a Displace Row attack divides among all players.
		// That's exactly how it's supposed to work in Free4All, so just
		// make one of those.
		return newQuantroFloodVSAttackSystemNoGarbage( ginfo, qi ) ;
	}
	
	
	public static AttackSystem newNoAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		AttackSystem as = new AttackSystem( ginfo, qi ) ;
		// By default our only behavior is to set no attacks.
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_NONE ) ;
		return as ;
	}
	
	
	public static AttackSystem newInitialGarbageQuantroAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		AttackSystem as = new AttackSystem( ginfo, qi ) ;
		// We hit ourselves.
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) ;
		
		as.setBehavior_levelUp_garbageRowNumber( AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW ) ;
		as.setBehavior_levelUp_garbageRowDensity( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_QUANTUM_CHEESE ) ;
		as.setBehavior_levelUp_garbageRowComposition( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_S0_S1 ) ;
		
		return as ;
	}
	
	public static AttackSystem newInitialGarbageRetroAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		AttackSystem as = new AttackSystem( ginfo, qi ) ;
		// We hit ourselves.
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) ;
		
		as.setBehavior_levelUp_garbageRowNumber( AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW ) ;
		as.setBehavior_levelUp_garbageRowDensity( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_SWISS_CHEESE ) ;
		as.setBehavior_levelUp_garbageRowComposition( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_RAINBOW ) ;
		
		return as ;
	}
	
	public static AttackSystem newLevelGarbageQuantroAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		AttackSystem as = new AttackSystem( ginfo, qi ) ;
		// We hit ourselves.
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) ;
		
		as.setBehavior_levelUp_garbageRowNumber( AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW ) ;
		as.setBehavior_levelUp_garbageRowDensity( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_QUANTUM_CHEESE ) ;
		as.setBehavior_levelUp_garbageRowComposition( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_S0_S1 ) ;
		
		return as ;
	}
	
	public static AttackSystem newLevelGarbageRetroAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		AttackSystem as = new AttackSystem( ginfo, qi ) ;
		// We hit ourselves.
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) ;
		
		as.setBehavior_levelUp_garbageRowNumber( AttackSystemAtomicConditionsVersioned.LEVEL_UP_GARBAGE_ROW_NUMBER_GAME_INFORMATION_AND_NEW ) ;
		as.setBehavior_levelUp_garbageRowDensity( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_SWISS_CHEESE ) ;
		as.setBehavior_levelUp_garbageRowComposition( AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_RAINBOW ) ;
		
		return as ;
	}
	
	
	/**
	 * Constructs and returns an attack system designed for retro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).  Prototyping means it can be freely changed.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new retro prototyping attack system.
	 */
	public static AttackSystem newRetroMasochistAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) ;
		// Garbage row behavior.
		as.setBehavior_clearedAndSent_garbageRowComposition(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_PIECE) ;
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO) ;
		as.setBehavior_clearedAndSent_garbageRowDensity(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE ) ;
		as.setBehavior_clearedAndSent_garbageRowQueue(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_QUEUE_SETS_TOGETHER) ;
		as.setBehavior_clearedAndSent_garbageRowInclude(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO) ;
		
		return as ;
	}
	
	
	/**
	 * Constructs and returns an attack system designed for Quantro
	 * gameplay (at present, that means a certain style of
	 * garbage rows).  Prototyping means it can be freely changed.
	 * 
	 * @param ginfo GameInformation for this game
	 * @param qi	QInteractions for this game
	 * @return		A new prototyping attack system.
	 */
	public static AttackSystem newQuantroMasochistAttackSystem(
			GameInformation ginfo, QInteractions qi ) {
		
		
		AttackSystem as = new AttackSystem(ginfo, qi) ;
		
		// Attack system behavior (outgoing)
		as.setBehavior_attackSystemAttacks( AttackSystemAtomicConditionsVersioned.ATTACK_SYSTEM_ATTACKS_SELF ) ;
		// Garbage row behavior.
		as.setBehavior_clearedAndSent_garbageRowComposition(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_COMPOSITION_CLEAR_ROWS) ;
		as.setBehavior_clearedAndSent_garbageRowNumber(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_NUMBER_CLEARS_SINCE_PIECE_AT_LEAST_TWO) ;
		as.setBehavior_clearedAndSent_garbageRowDensity(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_DENSITY_PIECE_NEGATIVE_SYMMETRIC_MIRRORED_PREFER_PIECE_CLEAR_NEGATIVE ) ;
		as.setBehavior_clearedAndSent_garbageRowQueue(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_QUEUE_SETS_TOGETHER) ;
		as.setBehavior_clearedAndSent_garbageRowInclude(
				AttackSystemAtomicConditionsVersioned.GARBAGE_ROW_INCLUDE_CLEARS_BEFORE_LOCK_NO) ;
		

		return as ;
	}
	
	
}
