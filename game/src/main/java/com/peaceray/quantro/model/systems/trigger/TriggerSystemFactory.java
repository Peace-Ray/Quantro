package com.peaceray.quantro.model.systems.trigger;

import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.systems.clear.ClearSystem;
import com.peaceray.quantro.model.systems.collision.CollisionSystem;
import com.peaceray.quantro.model.systems.lock.LockSystem;
import com.peaceray.quantro.model.systems.score.ScoreSystem;
import com.peaceray.quantro.model.systems.score.TriggerableLevelScoreSystem;
import com.peaceray.quantro.q.QInteractions;

public final class TriggerSystemFactory {

	
	public static TriggerSystem newPrototypeTriggerSystem(Game game, GameInformation ginfo, QInteractions qi,
			ClearSystem cls, CollisionSystem cs, LockSystem ls ) {
		TriggerSystem trs = new TriggerSystem(ginfo, qi) ;
		
		// Set systems
		trs.setClearSystem(cls) ;
		trs.setCollisionSystem(cs) ;
		trs.setLockSystem(ls) ;
		
		// Establish triggers.
		int [] conditions ;
		// Here are the systems from our prototype.
		// 1: Locking and clearing a Sticky Tetromino unlocks the best column.
		conditions = new int []
		                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO,
				           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_ST_REMOVED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_UNLOCK_BEST_COLUMN) ;
		
		// 2: Locking a linked tetracube snugly activates the SL block; failing to do so
		// 		deactivates it.
		conditions = new int []
				                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_SNUG,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_LOCK_AND_METAMORPHOSIS_ACTIVATE) ;
		
		conditions = new int []
				                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_UNSNUG,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_FAILURE_LOCK_AND_METAMORPHOSIS_DEACTIVATE) ;
		
		
		// 3: Locking a flash tetromino should cancel the lock,
		//    and unlock the column above it.
		conditions = new int []
		                      { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FLASH,
				                TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_CANCEL_PIECE_LOCK) ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_IF_UNLOCK_COLUMN_ABOVE_F_PIECE_BLOCKS ) ;
		
		
		// Done with setup
		return trs ;
	}
	
	public static TriggerSystem newQuantroSinglePlayerTriggerSystem(Game game, GameInformation ginfo, QInteractions qi,
			ClearSystem cls, CollisionSystem cs, LockSystem ls, ScoreSystem ss, double complexClearAdditiveMult, double specialPieceAdditiveMult ) {
		
		if ( complexClearAdditiveMult < 0 )
			throw new IllegalArgumentException("Complex clear trigger additive mult must be non-negative") ;
		if ( specialPieceAdditiveMult < 0 )
			throw new IllegalArgumentException("Special piece trigger additive mult must be non-negative") ;
		
		
		TriggerSystem trs = new TriggerSystem(ginfo, qi) ;
		
		// Set systems
		trs.setClearSystem(cls) ;
		trs.setCollisionSystem(cs) ;
		trs.setLockSystem(ls) ;
		
		// Establish triggers.
		int [] conditions ;
		// Here are the systems from our prototype.
		// 1: Locking and clearing a Sticky Tetromino unlocks the best column.
		conditions = new int []
		                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_STICKY_POLYOMINO,
				           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_ST_REMOVED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_UNLOCK_BEST_COLUMN_OR_DROP_BLOCKS_IN_VALLEYS, new Object[]{new Integer(2)} ) ;
		// score up (for "success") immediately.
		trs.setTriggerConjunction(conditions, ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(specialPieceAdditiveMult), new Integer(2), "Sticky Activated"}) ;
		
		// 2: Locking a linked tetracube snugly activates the SL block; failing to do so
		// 		deactivates it.
		conditions = new int []
				                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_SNUG,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_LOCK_AND_METAMORPHOSIS_ACTIVATE) ;
		// Success!  Increase multiplier IF we get any clears.
		conditions = new int []
				                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_SNUG,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE } ;
		trs.setTriggerConjunction(conditions, ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(specialPieceAdditiveMult), new Integer(2), "Fused Activated and Cleared"}) ;
		
		// Failure; did not lock.
		conditions = new int []
				                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FUSED_TETRACUBE,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_UNSNUG,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_FAILURE_LOCK_AND_METAMORPHOSIS_DEACTIVATE) ;
		
		
		// 3: Locking a flash tetromino should cancel the lock,
		//    and unlock the column above it.
		conditions = new int []
		                      { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FLASH,
				                TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_CANCEL_PIECE_LOCK) ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_IF_UNLOCK_COLUMN_ABOVE_F_PIECE_BLOCKS ) ;
		// however, we should only increase the multiplier if this results in a clear.
		conditions = new int []
			                      { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_FLASH,
                					TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED,
                					TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE } ;
		trs.setTriggerConjunction(conditions, ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(specialPieceAdditiveMult), new Integer(2), "Flash Caused Clear"}) ;
		
		
		// 4: No game event trigger is necessary for UL pieces, but if we get a clear with it, increase multiplier.
		conditions = new int [] 
		                      { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LINKS_POLYOMINO,
								TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_S0_AND_S1_UNION } ;
		trs.setTriggerConjunction(conditions, ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(specialPieceAdditiveMult), new Integer(2), "Links Cleared S0 / S1 Union"}) ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_NO_OTHER_EFFECT) ;
		
		
		// Finally, score-system triggers!
		// 1. Clearing a hurdle with a piece increases the multiplier by A.
		trs.setTriggerCondition(
				TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_HURDLE_PIECE,
				ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(complexClearAdditiveMult), new Integer(2), "Clear Hurdle Piece"}, false) ;
		
		// 2. Completely clearing a piece also increase the multiplier by A.
		// 		This is mutually exclusive with condition 1.
		/*	CANCEL: no more "complete clearing."  This is a boring condition
		trs.setTriggerCondition( TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE_GONE_IN_ONE,
				ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(complexClearAdditiveMult), new Integer(1), "Clear Piece Gone In One"}, false) ;
				*/
		
		// 3. Landing an immobile piece and getting at least one clear increases
		//		the multiplier by B.  Note that this IS compatible with 1 and 2;
		//		it can occur with the above, or they can occur without it.
		conditions = new int[]
		                     { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_IMMOBILE,
								TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE
		                     } ;
		trs.setTriggerConjunction(conditions, ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(complexClearAdditiveMult), new Integer(2), "Piece Immoble and Clear"}) ;
		
		// Done with setup
		return trs ;
	}
	
	
	
	public static TriggerSystem newRetroSinglePlayerTriggerSystem(Game game, GameInformation ginfo, QInteractions qi,
			ClearSystem cls, CollisionSystem cs, LockSystem ls, ScoreSystem ss, double complexClearAdditiveMult) {
		
		if ( complexClearAdditiveMult < 0 )
			throw new IllegalArgumentException("Complex clear trigger additive mult must be non-negative") ;
		
		TriggerSystem trs = new TriggerSystem(ginfo, qi) ;
		
		// Set systems
		trs.setClearSystem(cls) ;
		trs.setCollisionSystem(cs) ;
		trs.setLockSystem(ls) ;
		
		// Establish triggers.
		int [] conditions ;
		// 1. Clearing a hurdle with a piece increases the multiplier by A.
		trs.setTriggerCondition(
				TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_HURDLE_PIECE,
				ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(complexClearAdditiveMult), new Integer(2), "Clear Hurdle Piece"}, false) ;
		
		// 2. Completely clearing a piece also increase the multiplier by A.
		// 		This is mutually exclusive with condition 1.
		/* Don't use this; it's boring and easy.
		trs.setTriggerCondition( TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE_GONE_IN_ONE,
				ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(complexClearAdditiveMult), new Integer(1), "Clear Piece Gone in One"}, false) ;
				*/
		
		// 3. Landing an immobile piece and getting at least one clear increases
		//		the multiplier by B.  Note that this IS compatible with 1 and 2;
		//		it can occur with the above, or they can occur without it.
		conditions = new int[]
		                     { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_IMMOBILE,
								TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_CLEAR_PIECE
		                     } ;
		trs.setTriggerConjunction(conditions, ss, TriggerableLevelScoreSystem.TRIGGER_MULTIPLIER_INCREASE,
				new Object[]{new Double(complexClearAdditiveMult), new Integer(2), "Clear w/ Immobile Piece"}) ;
		
		
		////////////////////////////////////////////////////////////////////////
		// ADDED 1/16/2013: after the above conditions, because they are added
		// in-order and forward compatibility requires that new conditions occur
		// after old conditions.
		
		// 4. Landing a PUSH_BLOCK will trigger an 'activate' metamorphosis.
		conditions = new int []
				                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_PUSH_DOWN,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED } ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_METAMORPHOSIS_ACTIVATE_BEFORE_END_CYCLE) ;
		
		// 5: If a metamorphosis occurs (i.e., a PUSH_DOWN block was used and then NOT cleared away before it
		// could metamorphosize), that counts as a success.
		conditions = new int []
				                 { TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_PUSH_DOWN,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_PIECE_LOCKED,
		                           TriggerSystemAtomicConditionsVersioned.TRIGGER_CONDITION_METAMORPHOSIS} ;
		trs.setTriggerConjunction(conditions, game, Game.TRIGGER_SUCCESS_NO_OTHER_EFFECT) ;
		
		return trs ;
	}
	
}
