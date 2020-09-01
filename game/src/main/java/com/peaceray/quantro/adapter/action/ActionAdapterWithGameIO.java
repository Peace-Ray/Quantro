package com.peaceray.quantro.adapter.action;

import java.util.ArrayList;

import com.peaceray.quantro.model.descriptors.versioned.ActionCycleStateDescriptor;
import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.game.Game;

public class ActionAdapterWithGameIO extends ActionAdapter {
	
	//private static final String TAG = "ActionAdapterWithGameIO" ;
	
	public static int QUEUE_SIZE = 1024 ;
	
	
	boolean useTimingSystem ;
	boolean shouldTick ;
	boolean dequeueActionsDiscards ;
		// If 'true', the method game_dequeueActions will attempt to perform
		// queued actions (in order), but will discard those action which cannot
		// be applied.  The action adapter for a user-controlled game should follow
		// this model - if the player presses "left", but the game is not in a state
		// where a leftward movement is possible, discard the action rather than
		// retain it.
		// If 'false', the method game_dequeueActions will attempt to perform
		// queued actions (in order), but if the state of the game prevents one
		// from being applied, the action will be retained and game_dequeueActions
		// will return.  This setting is appropriate for games echoing the actions
		// of another game.  To explain: for those actions to have occurred, they 
		// must have occurred when the original game was in a state where those
		// actions appropriate.  Note that there is a call to dequeueActions with
		// every iteration of the "state updating loop" in Game.tick(), so
		// we will probably reach that state very rapidly.
	
	// Here are places to store these values.
	byte [] incomingActionQueue ;
	int numIncomingActions ;
	boolean slidingLeft ;
	boolean slidingRight ;
	boolean fastFalling ;
	boolean fastFallingJustStarted ;
	boolean fastFallAutoLock ;
	
	boolean slideLeftOnce ;
	boolean slideRightOnce ;
	
	byte [] outgoingActionQueue ;
	int numOutgoingActions ;
	
	RealtimeData realtimeData ;
	
	// ActionCycleStateDescriptor?
	ActionCycleStateDescriptor incomingActionCycleStateDescriptor ;
	ActionCycleStateDescriptor outgoingActionCycleStateDescriptor ;
	ActionCycleStateDescriptor outgoingActionCycleStateDescriptorMostRecent ;
	
	// Outgoing attacks?
	ArrayList<AttackDescriptor> outgoingAttackQueue ;
	int numOutgoingAttacks ;
	// Incoming attacks?
	ArrayList<AttackDescriptor> incomingAttackQueue ;
	int numIncomingAttacks ;
	// Incoming after this cycle?
	ArrayList<AttackDescriptor> incomingAttackQueueForNextCycle ;
	int numIncomingAttacksForNextCycle ;
	
	boolean incomingActionCycleStatePending ;
	boolean outgoingActionCycleStatePending ;
	boolean outgoingActionCycleStateMostRecentSet ;
	
	
	
	public ActionAdapterWithGameIO( int rows, int cols ) {
		useTimingSystem = true ;
		shouldTick = true ;
		dequeueActionsDiscards = true ;
		
		incomingActionQueue = new byte[QUEUE_SIZE] ;
		numIncomingActions = 0 ;
		slidingLeft = false ;
		slidingRight = false ;
		slideLeftOnce = false ;
		slideRightOnce = false ;
		
		fastFalling = false ;
		fastFallingJustStarted = false ;
		
		outgoingActionQueue = new byte[QUEUE_SIZE];
		numOutgoingActions = 0 ;
		
		realtimeData = new RealtimeData() ;
		
		// ActionCycleStateDescriptor?
		incomingActionCycleStateDescriptor = new ActionCycleStateDescriptor( rows, cols ) ;
		outgoingActionCycleStateDescriptor = new ActionCycleStateDescriptor( rows, cols ) ;
		outgoingActionCycleStateDescriptorMostRecent = new ActionCycleStateDescriptor( rows, cols ) ;
		
		outgoingAttackQueue = new ArrayList<AttackDescriptor>() ;
		incomingAttackQueue = new ArrayList<AttackDescriptor>() ;
		incomingAttackQueueForNextCycle = new ArrayList<AttackDescriptor>() ;
		numOutgoingAttacks = 0 ;
		numIncomingAttacks = 0 ;
		numIncomingAttacksForNextCycle = 0 ;
		
		incomingActionCycleStatePending = false ;
		outgoingActionCycleStatePending = false ;
		outgoingActionCycleStateMostRecentSet = false ;
	}
	
	
	
	
	//////////////////////////////////////////////////////////////
	//
	// Important maintainance.  (<-- still can't spell that word)
	// If we have received a full synchronization message, it is important
	// that we empty the action adapter of any queued events.  As
	// of the moment of Sync we are COMPLETELY UP TO DATE - any incoming
	// actions are now irrelevant, and any OUTGOING actions are incorrect,
	// since they were set for a now-incorrect game state.
	
	public synchronized void emptyActionQueues() {
		numIncomingActions = 0 ;
		numOutgoingActions = 0 ;
	}
	
	public synchronized void emptyAllQueues() {
		numIncomingActions = 0 ;
		numOutgoingActions = 0 ;
		
		numOutgoingAttacks = 0 ;
		numIncomingAttacks = 0 ;
		numIncomingAttacksForNextCycle = 0 ;
		
		incomingActionCycleStatePending = false ;
		outgoingActionCycleStatePending = false ;
	}
	
	// Queueing incoming/outgoing actions
	protected void enqueue_move( int moveDirection, byte [] q, int index ) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_LEFT:
			q[index] = CODE_MOVE_LEFT;
			break ;
		case MOVE_DIRECTION_RIGHT:
			q[index] = CODE_MOVE_RIGHT;
			break ;
		case MOVE_DIRECTION_DOWN:
			q[index] = CODE_DROP;
			break ;
		}
	}
	protected void enqueueIncoming_move( int moveDirection ) { enqueue_move( moveDirection, incomingActionQueue, numIncomingActions++ ) ; }
	protected void enqueueOutgoing_move( int moveDirection ) { enqueue_move( moveDirection, outgoingActionQueue, numOutgoingActions++ ) ; }
	
	protected void enqueue_turnCW(int moveDirection, byte [] q, int index) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_NONE:
			q[index] = CODE_TURN_CW;
			break ;
		case MOVE_DIRECTION_LEFT:
			q[index] = CODE_TURN_CW_LEAN_LEFT;
			break ;
		case MOVE_DIRECTION_RIGHT:
			q[index] = CODE_TURN_CW_LEAN_RIGHT;
			break ;
		case MOVE_DIRECTION_DOWN:
			q[index] = CODE_TURN_CW_LEAN_DOWN;
			break ;
		case MOVE_DIRECTION_DOWN_LEFT:
			q[index] = CODE_TURN_CW_LEAN_DOWN_LEFT;
			break ;
		case MOVE_DIRECTION_DOWN_RIGHT:
			q[index] = CODE_TURN_CW_LEAN_DOWN_RIGHT;
			break ;
		}
	}
	protected void enqueueIncoming_turnCW(int moveDirection) { enqueue_turnCW( moveDirection, incomingActionQueue, numIncomingActions++ ) ; }
	protected void enqueueOutgoing_turnCW(int moveDirection) { enqueue_turnCW( moveDirection, outgoingActionQueue, numOutgoingActions++ ) ; }
	
	protected void enqueue_turnCCW(int moveDirection, byte [] q, int index) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_NONE:
			q[index] = CODE_TURN_CCW;
			break ;
		case MOVE_DIRECTION_LEFT:
			q[index] = CODE_TURN_CCW_LEAN_LEFT;
			break ;
		case MOVE_DIRECTION_RIGHT:
			q[index] = CODE_TURN_CCW_LEAN_RIGHT;
			break ;
		case MOVE_DIRECTION_DOWN:
			q[index] = CODE_TURN_CCW_LEAN_DOWN;
			break ;
		case MOVE_DIRECTION_DOWN_LEFT:
			q[index] = CODE_TURN_CCW_LEAN_DOWN_LEFT;
			break ;
		case MOVE_DIRECTION_DOWN_RIGHT:
			q[index] = CODE_TURN_CCW_LEAN_DOWN_RIGHT;
			break ;
		}
	}
	protected void enqueueIncoming_turnCCW(int moveDirection) { enqueue_turnCCW( moveDirection, incomingActionQueue, numIncomingActions++ ) ; }
	protected void enqueueOutgoing_turnCCW(int moveDirection) { enqueue_turnCCW( moveDirection, outgoingActionQueue, numOutgoingActions++ ) ; }
	
	protected void enqueue_turnCW180(int moveDirection, byte [] q, int index) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_NONE:
			q[index] = CODE_TURN_CW_180;
			break ;
		case MOVE_DIRECTION_LEFT:
			q[index] = CODE_TURN_CW_180_LEAN_LEFT;
			break ;
		case MOVE_DIRECTION_RIGHT:
			q[index] = CODE_TURN_CW_180_LEAN_RIGHT;
			break ;
		case MOVE_DIRECTION_DOWN:
			q[index] = CODE_TURN_CW_180_LEAN_DOWN;
			break ;
		case MOVE_DIRECTION_DOWN_LEFT:
			q[index] = CODE_TURN_CW_180_LEAN_DOWN_LEFT;
			break ;
		case MOVE_DIRECTION_DOWN_RIGHT:
			q[index] = CODE_TURN_CW_180_LEAN_DOWN_RIGHT;
			break ;
		}
	}
	protected void enqueueIncoming_turnCW180(int moveDirection) { enqueue_turnCW180( moveDirection, incomingActionQueue, numIncomingActions++ ) ; }
	protected void enqueueOutgoing_turnCW180(int moveDirection) { enqueue_turnCW180( moveDirection, outgoingActionQueue, numOutgoingActions++ ) ; }
	
	protected void enqueue_turnCCW180(int moveDirection, byte [] q, int index) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_NONE:
			q[index] = CODE_TURN_CCW_180;
			break ;
		case MOVE_DIRECTION_LEFT:
			q[index] = CODE_TURN_CCW_180_LEAN_LEFT;
			break ;
		case MOVE_DIRECTION_RIGHT:
			q[index] = CODE_TURN_CCW_180_LEAN_RIGHT;
			break ;
		case MOVE_DIRECTION_DOWN:
			q[index] = CODE_TURN_CCW_180_LEAN_DOWN;
			break ;
		case MOVE_DIRECTION_DOWN_LEFT:
			q[index] = CODE_TURN_CCW_180_LEAN_DOWN_LEFT;
			break ;
		case MOVE_DIRECTION_DOWN_RIGHT:
			q[index] = CODE_TURN_CCW_180_LEAN_DOWN_RIGHT;
			break ;
		}
	}
	protected void enqueueIncoming_turnCCW180(int moveDirection) { enqueue_turnCCW180( moveDirection, incomingActionQueue, numIncomingActions++ ) ; }
	protected void enqueueOutgoing_turnCCW180(int moveDirection) { enqueue_turnCCW180( moveDirection, outgoingActionQueue, numOutgoingActions++ ) ; }
	
	protected void enqueue_flip(int moveDirection, byte [] q, int index) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_NONE:
			q[index] = CODE_FLIP;
			break ;
		case MOVE_DIRECTION_LEFT:
			q[index] = CODE_FLIP_LEAN_LEFT;
			break ;
		case MOVE_DIRECTION_RIGHT:
			q[index] = CODE_FLIP_LEAN_RIGHT;
			break ;
		case MOVE_DIRECTION_DOWN:
			q[index] = CODE_FLIP_LEAN_DOWN;
			break ;
		case MOVE_DIRECTION_DOWN_LEFT:
			q[index] = CODE_FLIP_LEAN_DOWN_LEFT;
			break ;
		case MOVE_DIRECTION_DOWN_RIGHT:
			q[index] = CODE_FLIP_LEAN_DOWN_RIGHT;
			break ;
		}
	}
	protected void enqueueIncoming_flip(int moveDirection) { enqueue_flip( moveDirection, incomingActionQueue, numIncomingActions++ ) ; }
	protected void enqueueOutgoing_flip(int moveDirection) { enqueue_flip( moveDirection, outgoingActionQueue, numOutgoingActions++ ) ; }
	
	
	
	protected void enqueue_useReserve(int moveDirection, byte [] q, int index) { 
		switch( moveDirection ) {
		case MOVE_DIRECTION_NONE:
			q[index] = CODE_USE_RESERVE;
			break ;
		case MOVE_DIRECTION_LEFT:
			q[index] = CODE_USE_RESERVE_LEAN_LEFT;
			break ;
		case MOVE_DIRECTION_RIGHT:
			q[index] = CODE_USE_RESERVE_LEAN_RIGHT;
			break ;
		case MOVE_DIRECTION_DOWN:
			q[index] = CODE_USE_RESERVE_LEAN_DOWN;
			break ;
		case MOVE_DIRECTION_DOWN_LEFT:
			q[index] = CODE_USE_RESERVE_LEAN_DOWN_LEFT;
			break ;
		case MOVE_DIRECTION_DOWN_RIGHT:
			q[index] = CODE_USE_RESERVE_LEAN_DOWN_RIGHT;
			break ;
		}
	}
	protected void enqueueIncoming_useReserve(int moveDirection) { enqueue_useReserve( moveDirection, incomingActionQueue, numIncomingActions++ ) ; } 
	protected void enqueueOutgoing_useReserve(int moveDirection) { enqueue_useReserve( moveDirection, outgoingActionQueue, numOutgoingActions++ ) ; } 
	
	protected void enqueueIncoming_issueSpecialAttack() { enqueue_useReserve( MOVE_DIRECTION_NONE, incomingActionQueue, numIncomingActions++ ) ; } 
	protected void enqueueOutgoing_issueSpecialAttack() { enqueue_useReserve( MOVE_DIRECTION_NONE, outgoingActionQueue, numOutgoingActions++ ) ; } 
	
	protected void enqueue_fall(byte [] q, int index) { q[index] = CODE_FALL ; }
	protected void enqueueIncoming_fall() { enqueue_fall( incomingActionQueue, numIncomingActions++ ) ; } 
	protected void enqueueOutgoing_fall() { enqueue_fall( outgoingActionQueue, numOutgoingActions++ ) ; } 
	
	protected void enqueue_lock(byte [] q, int index) { q[index] = CODE_LOCK ; }
	protected void enqueueIncoming_lock() { enqueue_lock( incomingActionQueue, numIncomingActions++ ) ; } 
	protected void enqueueOutgoing_lock() { enqueue_lock( outgoingActionQueue, numOutgoingActions++ ) ; } 
	
	protected void enqueue_endActionCycle(byte [] q, int index) { q[index] = CODE_END_ACTION_CYCLE ; }
	protected void enqueueIncoming_endActionCycle() { enqueue_endActionCycle( incomingActionQueue, numIncomingActions++ ) ; } 
	protected void enqueueOutgoing_endActionCycle() { enqueue_endActionCycle( outgoingActionQueue, numOutgoingActions++ ) ; } 
	
	protected void enqueue_advance(byte [] q, int index) { q[index] = CODE_ADVANCE ; }
	protected void enqueueIncoming_advance() { enqueue_advance( incomingActionQueue, numIncomingActions++ ) ; } 
	protected void enqueueOutgoing_advance() { enqueue_advance( outgoingActionQueue, numOutgoingActions++ ) ; } 
	
	
	
	protected void enqueue_autolock(byte [] q, int index) { q[index] = CODE_AUTOLOCK ; }
	protected void enqueueIncoming_autolock() { enqueue_autolock( incomingActionQueue, numIncomingActions++ ) ; }
	
	
	protected void enqueue_fall_or_autolock(byte [] q, int index) { q[index] = CODE_FALL_OR_AUTOLOCK ; }
	protected void enqueueIncoming_fall_or_autolock() { enqueue_fall_or_autolock( incomingActionQueue, numIncomingActions++ ) ; }
	
	
	
	// How to queue incoming actions from the controls?  Just call our
	// "enqueue" methods.
	public synchronized void controls_move(int moveDirection) { enqueueIncoming_move(moveDirection) ; }
	public synchronized void controls_turnCW(int moveDirection) { enqueueIncoming_turnCW(moveDirection) ; /*System.err.println("AAWGIO: turn CW 90 lean " + moveDirectionToString(moveDirection)) ; */ }
	public synchronized void controls_turnCCW(int moveDirection) { enqueueIncoming_turnCCW(moveDirection) ; /*System.err.println("AAWGIO: turn CCW 90 lean " + moveDirectionToString(moveDirection)) ; */ }
	public synchronized void controls_turnCW180(int moveDirection) { enqueueIncoming_turnCW180(moveDirection) ; /*System.err.println("AAWGIO: turn CW 180 lean " + moveDirectionToString(moveDirection)) ; */ }
	public synchronized void controls_turnCCW180(int moveDirection) { enqueueIncoming_turnCCW180(moveDirection) ; /*System.err.println("AAWGIO: turn CCW 180 lean " + moveDirectionToString(moveDirection)) ; */ }
	public synchronized void controls_flip(int moveDirection) { enqueueIncoming_flip(moveDirection) ; /*System.err.println("AAWGIO: flip lean " + moveDirectionToString(moveDirection)) ; */ }
	public synchronized void controls_useReserve(int moveDirection) { enqueueIncoming_useReserve(moveDirection) ; /*System.err.println("AAWGIO: reserve lean " + moveDirectionToString(moveDirection)) ; */ } 
	
	public synchronized void controls_fall() { enqueueIncoming_fall() ; }
	public synchronized void controls_autolock() { enqueueIncoming_autolock() ; }
	public synchronized void controls_fall_or_autolock() { enqueueIncoming_fall_or_autolock() ; } ;
	// performs a piece lock IF a long enough wait would lock the piece in
	// its current position.  i.e., if the piece is currently resting on
	// the ground and waiting on a ts.canLock() response, this will lock 
	// immediately.  Otherwise, it has no effect.
	
	public synchronized void controls_slide( int moveDirection, boolean isSliding ) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_LEFT:
			slidingLeft = isSliding ;
			break ;
		case MOVE_DIRECTION_RIGHT:
			slidingRight = isSliding ;
			break ;
				
		}
	}
	
	public synchronized void controls_slideOnce( int moveDirection ) {
		switch( moveDirection ) {
		case MOVE_DIRECTION_LEFT:
			slideLeftOnce = true ;
			break ;
		case MOVE_DIRECTION_RIGHT:
			slideRightOnce = true ;
			break ;
		
		}
	}
	
	public synchronized void controls_fastFall( boolean isFastFalling ) {
		// if we're fast-falling now but weren't before, we just
		// started.  Otherwise, we keep the previous value.
		if ( isFastFalling && !fastFalling )
			fastFallingJustStarted = true ;
		fastFalling = isFastFalling ;
		fastFallAutoLock = false ;
	}
	
	public synchronized void controls_fastFall_and_autolock( boolean isFastFalling ) {
		// if we're fast-falling now but weren't before, we just
		// started.  Otherwise, we keep the previous value.
		if ( isFastFalling && !fastFalling )
			fastFallingJustStarted = true ;
		fastFalling = isFastFalling ;
		fastFallAutoLock = true ;
	}
	
	
	
	/*
	private String moveDirectionToString( int dir ) {
		switch( dir ) {
		case MOVE_DIRECTION_NONE:
			return "no" ;
		case MOVE_DIRECTION_LEFT:
			return "L" ;
		case MOVE_DIRECTION_RIGHT:
			return "R" ;
		case MOVE_DIRECTION_DOWN_LEFT:
			return "DL" ;
		case MOVE_DIRECTION_DOWN_RIGHT:
			return "DR" ;
		case MOVE_DIRECTION_DOWN:
			return "D" ;
		}
		return "?" ;
	}
	*/

	// In this implementation, communications_enqueue* are functionally 
	// equivalent to controls_* methods.  We simply call the
	// protected "enqueue_*" methods.
	public synchronized void communications_enqueueMove(int moveDirection) { enqueueIncoming_move(moveDirection) ; } 
	public synchronized void communications_enqueueTurnCW(int moveDirection) { enqueueIncoming_turnCW(moveDirection) ; } 
	public synchronized void communications_enqueueTurnCCW(int moveDirection) { enqueueIncoming_turnCCW(moveDirection) ; } 
	public synchronized void communications_enqueueTurnCW180(int moveDirection) { enqueueIncoming_turnCW180(moveDirection) ; } 
	public synchronized void communications_enqueueTurnCCW180(int moveDirection) { enqueueIncoming_turnCCW180(moveDirection) ; } 
	public synchronized void communications_enqueueUseReserve(int moveDirection) { enqueueIncoming_useReserve(moveDirection) ; } 
	
	public synchronized void communications_enqueueFall() { enqueueIncoming_fall() ; } 
	public synchronized void communications_enqueueLock() { enqueueIncoming_lock() ; } 
	
	public synchronized void communications_enqueueEndActionCycle() { enqueueIncoming_endActionCycle() ; } 
	
	public synchronized void communications_enqueueAdvance() { enqueueIncoming_advance() ; }
	
	
	/**
	 * Equivalent to calling the appropriate method above for each move in the provided queue.
	 * @param moveQ
	 */
	public synchronized void communications_enqueueActions( byte [] moveQ, int index, int length ) {
		for ( int i = index; i < index+length; i++ ) {
			switch( moveQ[i] ) {
			// simple movement
			case CODE_MOVE_LEFT: 		enqueueIncoming_move(MOVE_DIRECTION_LEFT) ; 		break ;
			case CODE_MOVE_RIGHT:		enqueueIncoming_move(MOVE_DIRECTION_RIGHT) ; 		break ;
			case CODE_DROP: 			enqueueIncoming_move(MOVE_DIRECTION_DOWN) ; 		break ;
			// directionless rotation
			case CODE_TURN_CW: 			enqueueIncoming_turnCW(MOVE_DIRECTION_NONE) ;		break ;
			case CODE_TURN_CCW: 		enqueueIncoming_turnCCW(MOVE_DIRECTION_NONE) ;		break ;
			case CODE_TURN_CW_180: 		enqueueIncoming_turnCW180(MOVE_DIRECTION_NONE) ;	break ;
			case CODE_TURN_CCW_180: 	enqueueIncoming_turnCCW180(MOVE_DIRECTION_NONE) ;	break ;
			// rotation with leftward motion
			case CODE_TURN_CW_LEAN_LEFT: 		enqueueIncoming_turnCW(MOVE_DIRECTION_LEFT) ;		break ;
			case CODE_TURN_CCW_LEAN_LEFT: 		enqueueIncoming_turnCCW(MOVE_DIRECTION_LEFT) ;		break ;
			case CODE_TURN_CW_180_LEAN_LEFT: 	enqueueIncoming_turnCW180(MOVE_DIRECTION_LEFT) ;	break ;
			case CODE_TURN_CCW_180_LEAN_LEFT: 	enqueueIncoming_turnCCW180(MOVE_DIRECTION_LEFT) ;	break ;
			// rotation with rightward motion
			case CODE_TURN_CW_LEAN_RIGHT: 		enqueueIncoming_turnCW(MOVE_DIRECTION_RIGHT) ;		break ;
			case CODE_TURN_CCW_LEAN_RIGHT: 		enqueueIncoming_turnCCW(MOVE_DIRECTION_RIGHT) ;		break ;
			case CODE_TURN_CW_180_LEAN_RIGHT: 	enqueueIncoming_turnCW180(MOVE_DIRECTION_RIGHT) ;	break ;
			case CODE_TURN_CCW_180_LEAN_RIGHT: 	enqueueIncoming_turnCCW180(MOVE_DIRECTION_RIGHT) ;	break ;
			// rotation with downward motion
			case CODE_TURN_CW_LEAN_DOWN: 		enqueueIncoming_turnCW(MOVE_DIRECTION_DOWN) ;		break ;
			case CODE_TURN_CCW_LEAN_DOWN: 		enqueueIncoming_turnCCW(MOVE_DIRECTION_DOWN) ;		break ;
			case CODE_TURN_CW_180_LEAN_DOWN: 	enqueueIncoming_turnCW180(MOVE_DIRECTION_DOWN) ;	break ;
			case CODE_TURN_CCW_180_LEAN_DOWN: 	enqueueIncoming_turnCCW180(MOVE_DIRECTION_DOWN) ;	break ;
			// rotation with leftward motion
			case CODE_TURN_CW_LEAN_DOWN_LEFT: 		enqueueIncoming_turnCW(MOVE_DIRECTION_DOWN_LEFT) ;		break ;
			case CODE_TURN_CCW_LEAN_DOWN_LEFT: 		enqueueIncoming_turnCCW(MOVE_DIRECTION_DOWN_LEFT) ;		break ;
			case CODE_TURN_CW_180_LEAN_DOWN_LEFT: 	enqueueIncoming_turnCW180(MOVE_DIRECTION_DOWN_LEFT) ;	break ;
			case CODE_TURN_CCW_180_LEAN_DOWN_LEFT: 	enqueueIncoming_turnCCW180(MOVE_DIRECTION_DOWN_LEFT) ;	break ;
			// rotation with rightward motion
			case CODE_TURN_CW_LEAN_DOWN_RIGHT: 			enqueueIncoming_turnCW(MOVE_DIRECTION_DOWN_RIGHT) ;		break ;
			case CODE_TURN_CCW_LEAN_DOWN_RIGHT: 		enqueueIncoming_turnCCW(MOVE_DIRECTION_DOWN_RIGHT) ;	break ;
			case CODE_TURN_CW_180_LEAN_DOWN_RIGHT: 		enqueueIncoming_turnCW180(MOVE_DIRECTION_DOWN_RIGHT) ;	break ;
			case CODE_TURN_CCW_180_LEAN_DOWN_RIGHT: 	enqueueIncoming_turnCCW180(MOVE_DIRECTION_DOWN_RIGHT) ;	break ;
			// reserves
			case CODE_USE_RESERVE:					enqueueIncoming_useReserve(MOVE_DIRECTION_NONE) ;			break ;
			case CODE_USE_RESERVE_LEAN_LEFT:		enqueueIncoming_useReserve(MOVE_DIRECTION_LEFT) ;			break ;
			case CODE_USE_RESERVE_LEAN_RIGHT:		enqueueIncoming_useReserve(MOVE_DIRECTION_RIGHT) ;			break ;
			case CODE_USE_RESERVE_LEAN_DOWN:		enqueueIncoming_useReserve(MOVE_DIRECTION_DOWN) ;			break ;
			case CODE_USE_RESERVE_LEAN_DOWN_LEFT:	enqueueIncoming_useReserve(MOVE_DIRECTION_DOWN_LEFT) ;		break ;
			case CODE_USE_RESERVE_LEAN_DOWN_RIGHT:	enqueueIncoming_useReserve(MOVE_DIRECTION_DOWN_RIGHT) ;		break ;
			
			// other stuff
			case CODE_FALL: 			enqueueIncoming_fall() ; 					break ;
			case CODE_LOCK:				enqueueIncoming_lock() ; 					break ;
			case CODE_END_ACTION_CYCLE:	enqueueIncoming_endActionCycle() ;			break ;
			
			// advance
			case CODE_ADVANCE:			enqueueIncoming_advance() ;					break ;
			}
		}
	}
	
	
	/**
	 * Removes all pending actions in the queue.  This method is
	 * dangerous to use!  Only call this when you're certain it's time
	 * to completely clear the pending actions, such as when a full 
	 * synchronization update is pending.
	 */
	synchronized public void communications_clearForSynchronization() {
		numIncomingActions = 0 ;
		numOutgoingActions = 0 ;
		numOutgoingAttacks = 0 ;
		numIncomingAttacks = 0 ;
		numIncomingAttacksForNextCycle = 0 ;
		incomingActionCycleStatePending = false ;
		outgoingActionCycleStatePending = false ;
	}
	
	
	// How to set up outgoing actions?
	public synchronized void game_didMove(int moveDirection) { enqueueOutgoing_move(moveDirection) ; if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	public synchronized void game_didTurnCW(int moveDirection) { enqueueOutgoing_turnCW(moveDirection) ; if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	public synchronized void game_didTurnCCW(int moveDirection) { enqueueOutgoing_turnCCW(moveDirection) ; if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	public synchronized void game_didTurnCW180(int moveDirection) { enqueueOutgoing_turnCW180(moveDirection) ; if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	public synchronized void game_didTurnCCW180(int moveDirection) { enqueueOutgoing_turnCCW180(moveDirection) ; if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	public synchronized void game_didFlip(int moveDirection) { enqueueOutgoing_flip(moveDirection) ; if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	public synchronized void game_didUseReserve(int moveDirection) { enqueueOutgoing_useReserve(moveDirection); if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	
	/**
	 * The player used a special that issued an attack.  We provide a specific
	 * method for this, because there is no obvious and easy time to check,
	 * as there is for most attacks (which are issued at the end of an 
	 * action cycle).
	 * @param game
	 */
	public synchronized void game_didIssueSpecialAttack( Game game ) {
		enqueueOutgoing_issueSpecialAttack() ;
		
		// copy the outgoing attacks.
		numOutgoingAttacks = game.aggregateAndClearOutgoingAttackQueue(outgoingAttackQueue, numOutgoingAttacks) ;
		if ( gal != null )
			gal.gal_gameHasOutOfSequenceAttack(this) ;
	}
	
	public synchronized void game_didFall() { enqueueOutgoing_fall(); if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	public synchronized void game_didLock() { enqueueOutgoing_lock(); if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	
	public synchronized void game_didEnter() {
		// if a piece just entered, we don't maintain the "JustStarted"
		// value for fast falling.  Otherwise, tapping the drop button
		// between pieces will queue up a single fall, which is weird.
		fastFallingJustStarted = false ;
	}
	
	// If, for whatever reason, there is no longer a falling piece being
	// moved about (e.g. it locked, it was moved to Reserve and no piece
	// replaced it, etc.).
	public synchronized void game_didEndActionCycle() { enqueueOutgoing_endActionCycle(); if (gal != null) gal.gal_gameDidEnqueueActions(this, true); }
	
	public synchronized void game_didAdvance() { enqueueOutgoing_advance(); if (gal != null) gal.gal_gameDidEnqueueActions(this, false); }
	
	
	public synchronized void game_hasOutgoingAttack( Game game ) {
		numOutgoingAttacks = game.aggregateAndClearOutgoingAttackQueue( outgoingAttackQueue, numOutgoingAttacks ) ;
		if ( gal != null )
			gal.gal_gameHasOutOfSequenceAttack(this) ;
	}
	

	/**
	 * A collision occurred!  This happens when attempting to enter a piece.
	 */
	public void game_didCollide() {
		// Do we track this value?  No, we just inform the GAL.
		if ( gal != null )
			gal.gal_gameDidCollide(this) ;
	}
	
	/**
	 * A level-up occurred!
	 */
	public void game_didLevelUp() {
		if ( gal != null )
			gal.gal_gameDidLevelUp(this) ;
	}
	
	/**
	 * A level-up is pending!
	 */
	public void game_aboutToLevelUp() {
		if ( gal != null )
			gal.gal_gameAboutToLevelUp(this) ;
	}
	
	
	/**
	 * Called once for a GameState, immediately before it transitions out of
	 * INITIALIZATION and to SYNCHRONIZATION.
	 */
	public void game_aboutToInitialize(Game game) {
		if  ( gal != null )
			gal.gal_gameDidInitialize(this, game) ;
	}
	
	/**
	 * Indicates that the current row displacement for the game has changed.
	 * @param displacement
	 */
	public synchronized void game_didChangeRealtime( Game game, long millis, double displacementSeconds, double rowsDisplacedAndTransferred ) {
		double prevR = this.realtimeData.getDisplacementRows() ;
		realtimeData.set(millis, displacementSeconds, rowsDisplacedAndTransferred) ;
		if ( gal != null && Math.round(rowsDisplacedAndTransferred) != Math.round(prevR) ) {
			gal.gal_gameDisplacementDidChangeInteger(this, realtimeData) ;
		}
	}
	
	
	
	////////////////////////////////////////////////////////////////////////
	//
	// GAME OBJECT UPDATE REQUESTS!
	//
	////////////////////////////////////////////////////////////////////////
	
	/**
	 * Performs on the 'game' object the queued actions, from either 
	 * a ControlsAdapter or some other source (e.g. if adapting for
	 * an echoed game).  Will call methods on 'game' such as 
	 * moveLeft, moveRight, drop, fall, lock, useReserve, etc.
	 * 
	 * @param game The game object on which to dequeue the actions.
	 * @return Whether any actions were dequeued.
	 */
	public synchronized boolean game_dequeueActions( Game game ) {
		// We dequeue all actions in the 'incomingActionQueue', until
		// we hit either the end, or an END_ACTION_CYCLE action.
		// Note that in the latter case, only beginning a new cycle will
		// pass this action and allow the game to continue.
		
		// Enqueue any incoming attacks.
		int numDequeued = 0 ;
		synchronized( game ) {
			for ( int i = 0; i < numIncomingAttacks; i++ ) {
				AttackDescriptor ad = incomingAttackQueue.get(i) ;
				game.enqueueIncomingAttack(ad, true) ;
				ad.makeEmpty() ;
			}
			numIncomingAttacks = 0 ;
			
			while ( numDequeued < numIncomingActions && incomingActionQueue[numDequeued] != CODE_END_ACTION_CYCLE ) {
				byte code = incomingActionQueue[numDequeued] ;
				
				// Note: in the case that dequeueActionsDiscards is false, we should immediately
				// terminate this loop if this action cannot be performed.
				boolean can = can_game_dequeueActions( game, code ) ;
				if ( can )
					do_game_dequeueActions( game, code ) ;
				else if ( !dequeueActionsDiscards )
					break ;
				
				// iterate
				numDequeued++ ;
			}
			
			// If we haven't ended, do some sliding and/or fast falling.
			if ( numDequeued == numIncomingActions ) {
				// Convention: we want slides to take priority.  In other words,
				// we slide left/right at each fall step.
				do {
					while( (slidingLeft || slideLeftOnce) && game.stateOK_moveLeftOnce() && game.moveLeftOnce() ) ;		// No action; moveLeftOnce returns 'false' once moving is impossible.
					while( (slidingRight || slideRightOnce) && game.stateOK_moveRightOnce() && game.moveRightOnce() ) ;		// Again, will loop until cannot move right.
					
					// fast fall?  We fast fall if fastFalling and the game is OK to
					// do so (both stateOK and timingOK), OR fastFallingJustStarted
					// and we are stateOK for it (ignoring timing).
					boolean stateOK = game.stateOK_fall() || ( fastFallAutoLock && game.stateOK_autolock() ) ;
					if ( stateOK
							&& ( fastFallingJustStarted 
									|| ( fastFalling && game.timingOK_fastFall() ) ) ) {
						// System.err.println("ActionAdapterWithGameIO: fastfall.  Autolock " + fastFallAutoLock ) ;
						if ( fastFallAutoLock )
							game.fall_or_autolock() ;
						else
							game.fall() ;
						
						fastFallingJustStarted = false ;
					}
				} while ( fastFalling && game.stateOK_fall() && game.timingOK_fastFall() ) ;
				// while we are still fastfalling and the game says doing this is OK.
			}
			
			// Slide incomingActionQueue back a bit.
			for ( int i = numDequeued; i < numIncomingActions; i++ )
				incomingActionQueue[i-numDequeued] = incomingActionQueue[i] ;
			numIncomingActions -= numDequeued ;
		}
		
		// no longer "slide once"
		slideLeftOnce = false ;
		slideRightOnce = false ;
		
		// Return whether we've done anything; also, inform the delegate.
		boolean returnVal = numDequeued > 0 ;
		if ( gal != null )
			gal.gal_gameDidDequeueActions(this, game, returnVal) ;
		return returnVal ;
	}
	
	
	/**
	 * If this adapter discards actions that cannot be dequeued (e.g.
	 * if it is a live game played by the user and button presses should
	 * have no effect when they cannot immediately affect the game),
	 * those actions are removed from the queue.  Otherwise (e.g., for
	 * an echoed game in multiplayer) this has no effect.
	 * 
	 * Recommended usage is in the place of game_dequeueActions when
	 * the game is paused or veiled.
	 * 
	 * @param game
	 */
	public void game_fakeDequeueActions( Game game ) {
		if ( !dequeueActionsDiscards ) {
			int numRemoved = 0 ;
			while ( numRemoved < numIncomingActions
					&& incomingActionQueue[numRemoved] != CODE_END_ACTION_CYCLE ) {
				numRemoved++ ;
			}
			
			// Slide incomingActionQueue back a bit.
			for ( int i = numRemoved; i < numIncomingActions; i++ )
				incomingActionQueue[i-numRemoved] = incomingActionQueue[i] ;
			numIncomingActions -= numRemoved ;
			
			slideLeftOnce = false ;
			slideRightOnce = false ;
		}
	}
	
	
	private boolean can_game_dequeueActions( Game game, byte code ) {
		switch( code ) {
		// Movement
		case CODE_MOVE_LEFT:	return game.stateOK_moveLeftOnce() ;
		case CODE_MOVE_RIGHT:	return game.stateOK_moveRightOnce() ;
		
		// Rotation (lean doesn't matter for "stateOK_")
		case CODE_TURN_CW:	
		case CODE_TURN_CW_LEAN_LEFT:	
		case CODE_TURN_CW_LEAN_RIGHT:	
		case CODE_TURN_CW_LEAN_DOWN:	
		case CODE_TURN_CW_LEAN_DOWN_LEFT:	
		case CODE_TURN_CW_LEAN_DOWN_RIGHT:	
			return game.stateOK_turnCW() ;
		case CODE_TURN_CCW:	
		case CODE_TURN_CCW_LEAN_LEFT:	
		case CODE_TURN_CCW_LEAN_RIGHT:	
		case CODE_TURN_CCW_LEAN_DOWN:	
		case CODE_TURN_CCW_LEAN_DOWN_LEFT:	
		case CODE_TURN_CCW_LEAN_DOWN_RIGHT:	
			return game.stateOK_turnCCW() ;
		case CODE_TURN_CW_180:	
		case CODE_TURN_CW_180_LEAN_LEFT:	
		case CODE_TURN_CW_180_LEAN_RIGHT:	
		case CODE_TURN_CW_180_LEAN_DOWN:	
		case CODE_TURN_CW_180_LEAN_DOWN_LEFT:	
		case CODE_TURN_CW_180_LEAN_DOWN_RIGHT:	
			return game.stateOK_turnCW180() ;
		case CODE_TURN_CCW_180:	
		case CODE_TURN_CCW_180_LEAN_LEFT:	
		case CODE_TURN_CCW_180_LEAN_RIGHT:	
		case CODE_TURN_CCW_180_LEAN_DOWN:	
		case CODE_TURN_CCW_180_LEAN_DOWN_LEFT:	
		case CODE_TURN_CCW_180_LEAN_DOWN_RIGHT:	
			return game.stateOK_turnCCW180() ;
		
		// Flip
		case CODE_FLIP:
		case CODE_FLIP_LEAN_LEFT:
		case CODE_FLIP_LEAN_RIGHT:
		case CODE_FLIP_LEAN_DOWN:
		case CODE_FLIP_LEAN_DOWN_LEFT:
		case CODE_FLIP_LEAN_DOWN_RIGHT:
			return game.stateOK_flip() ;
			
		// Reserve
		case CODE_USE_RESERVE:
		case CODE_USE_RESERVE_LEAN_LEFT:	
		case CODE_USE_RESERVE_LEAN_RIGHT:	
		case CODE_USE_RESERVE_LEAN_DOWN:	
		case CODE_USE_RESERVE_LEAN_DOWN_LEFT:	
		case CODE_USE_RESERVE_LEAN_DOWN_RIGHT:	
			return game.stateOK_useReserve() ;
		
		case CODE_DROP:			return game.stateOK_drop() ;
		case CODE_FALL:			return game.stateOK_fall() ;
		case CODE_LOCK:			return game.stateOK_lockPiece() ;
		case CODE_AUTOLOCK:		return game.stateOK_autolock() ;
		case CODE_FALL_OR_AUTOLOCK:		return game.stateOK_fall() || game.stateOK_autolock() ;
		
		// Begin cycle.  No effect; serves only to jump-start dequeues.
		case CODE_ADVANCE:		return true ;
		default:				return false ;
		}
	}
	
	private void do_game_dequeueActions( Game game, byte code ) {
		switch( code ) {
		// Movement?
		case CODE_MOVE_LEFT:	game.moveLeftOnce() ;		break ;
		case CODE_MOVE_RIGHT:	game.moveRightOnce() ;		break ;
		
		// Rotation w/o leans
		case CODE_TURN_CW:		game.turnCW( Game.LEAN_NONE ) ;				break ;
		case CODE_TURN_CCW:		game.turnCCW( Game.LEAN_NONE ) ;			break ;
		case CODE_TURN_CW_180:	game.turnCW180( Game.LEAN_NONE ) ;			break ;
		case CODE_TURN_CCW_180:	game.turnCCW180( Game.LEAN_NONE ) ;			break ;
		
		// Rotation: lean left
		case CODE_TURN_CW_LEAN_LEFT:		game.turnCW( Game.LEAN_LEFT ) ;				break ;
		case CODE_TURN_CCW_LEAN_LEFT:		game.turnCCW( Game.LEAN_LEFT ) ;			break ;
		case CODE_TURN_CW_180_LEAN_LEFT:	game.turnCW180( Game.LEAN_LEFT ) ;			break ;
		case CODE_TURN_CCW_180_LEAN_LEFT:	game.turnCCW180( Game.LEAN_LEFT ) ;			break ;
		
		// Rotation: lean right
		case CODE_TURN_CW_LEAN_RIGHT:		game.turnCW( Game.LEAN_RIGHT ) ;				break ;
		case CODE_TURN_CCW_LEAN_RIGHT:		game.turnCCW( Game.LEAN_RIGHT ) ;			break ;
		case CODE_TURN_CW_180_LEAN_RIGHT:	game.turnCW180( Game.LEAN_RIGHT ) ;			break ;
		case CODE_TURN_CCW_180_LEAN_RIGHT:	game.turnCCW180( Game.LEAN_RIGHT ) ;			break ;
		
		// Rotation: lean down
		case CODE_TURN_CW_LEAN_DOWN:		game.turnCW( Game.LEAN_DOWN ) ;				break ;
		case CODE_TURN_CCW_LEAN_DOWN:		game.turnCCW( Game.LEAN_DOWN ) ;			break ;
		case CODE_TURN_CW_180_LEAN_DOWN:	game.turnCW180( Game.LEAN_DOWN ) ;			break ;
		case CODE_TURN_CCW_180_LEAN_DOWN:	game.turnCCW180( Game.LEAN_DOWN ) ;			break ;
		
		// Rotation: lean down-left
		case CODE_TURN_CW_LEAN_DOWN_LEFT:		game.turnCW( Game.LEAN_DOWN_LEFT ) ;				break ;
		case CODE_TURN_CCW_LEAN_DOWN_LEFT:		game.turnCCW( Game.LEAN_DOWN_LEFT ) ;			break ;
		case CODE_TURN_CW_180_LEAN_DOWN_LEFT:	game.turnCW180( Game.LEAN_DOWN_LEFT ) ;			break ;
		case CODE_TURN_CCW_180_LEAN_DOWN_LEFT:	game.turnCCW180( Game.LEAN_DOWN_LEFT ) ;			break ;
		
		// Rotation: lean down-right
		case CODE_TURN_CW_LEAN_DOWN_RIGHT:		game.turnCW( Game.LEAN_DOWN_RIGHT ) ;				break ;
		case CODE_TURN_CCW_LEAN_DOWN_RIGHT:		game.turnCCW( Game.LEAN_DOWN_RIGHT ) ;			break ;
		case CODE_TURN_CW_180_LEAN_DOWN_RIGHT:	game.turnCW180( Game.LEAN_DOWN_RIGHT ) ;			break ;
		case CODE_TURN_CCW_180_LEAN_DOWN_RIGHT:	game.turnCCW180( Game.LEAN_DOWN_RIGHT ) ;			break ;
		
		// Flips
		case CODE_FLIP:							game.flip( Game.LEAN_NONE ) ;			break ;
		case CODE_FLIP_LEAN_LEFT:				game.flip( Game.LEAN_LEFT ) ;			break ;
		case CODE_FLIP_LEAN_RIGHT:				game.flip( Game.LEAN_RIGHT ) ;			break ;
		case CODE_FLIP_LEAN_DOWN:				game.flip( Game.LEAN_DOWN ) ;			break ;
		case CODE_FLIP_LEAN_DOWN_LEFT:			game.flip( Game.LEAN_DOWN_LEFT ) ;			break ;
		case CODE_FLIP_LEAN_DOWN_RIGHT:			game.flip( Game.LEAN_DOWN_RIGHT ) ;			break ;
		
		// Reserves
		case CODE_USE_RESERVE:					game.useReserve( Game.LEAN_NONE ) ;			break ;
		case CODE_USE_RESERVE_LEAN_LEFT:		game.useReserve( Game.LEAN_LEFT ) ;			break ;
		case CODE_USE_RESERVE_LEAN_RIGHT:		game.useReserve( Game.LEAN_RIGHT ) ;			break ;
		case CODE_USE_RESERVE_LEAN_DOWN:		game.useReserve( Game.LEAN_DOWN ) ;			break ;
		case CODE_USE_RESERVE_LEAN_DOWN_LEFT:	game.useReserve( Game.LEAN_DOWN_LEFT ) ;			break ;
		case CODE_USE_RESERVE_LEAN_DOWN_RIGHT:	game.useReserve( Game.LEAN_DOWN_RIGHT ) ;			break ;
		
		case CODE_DROP:			game.drop() ;				break ;
		case CODE_FALL:			game.fall() ;				break ;
		case CODE_LOCK:			game.lockPiece() ;			break ;
		case CODE_AUTOLOCK:		game.autolock() ;			break ;
		case CODE_FALL_OR_AUTOLOCK:		game.fall_or_autolock() ;			break ;
		
		case CODE_ADVANCE:		break ;		// no effect; used only to jump-start game advancement
		}
	}
	
	
	/**
	 * Asks the adapter whether to follow the TimingSystem's prompts
	 * to drop, fall, lock, etc.  Whatever this method returns, the
	 * Game object should not begin a new "cycle" (i.e., preparing
	 * and entering a new piece) until the adapter allows it.
	 * 
	 * Note that if this method returns false, it is entirely up to the
	 * ActionAdapter to provide explicit 'fall' and 'lock' instructions to
	 * the game object during dequeueActions.  Otherwise, the Game object
	 * will never perform these actions and thus will never progress from
	 * one cycle to the next.
	 * 
	 * Typically, a user-controlled game should receive "true" from this method
	 * call.  Echoed games, and games running on the Coordinator, should
	 * receive "fales" as they are expected to have explicit "fall" and "lock"
	 * actions performed by game_dequeueActions( Game game ).
	 * 
	 * @param game Specifies the game in question.  Probably not needed, but why
	 * 			not keep these prototypes consistent?
	 * @return Whether the game object should listen to the timingSystem in
	 * 			performing game actions.
	 */
	public synchronized boolean game_shouldUseTimingSystem( Game game ) {
		return useTimingSystem ;
	}
	
	
	/**
	 * Set, for this ActionAdapter, whether Game objects should use
	 * the TimingSystem to advance events.
	 * @param useTS
	 */
	public synchronized void set_gameShouldUseTimingSystem( boolean useTS ) {
		useTimingSystem = useTS ;
	}
	
	/**
	 * When dequeueing actions, should this adapter DISCARD those actions that
	 * the game cannot perform in its given state?  e.g. if a "move left" action
	 * is queued, and the Game is waiting for an action state, should the "move left"
	 * action be removed from the queue upon a call to game_dequeueActions?
	 * 
	 * 'true': appropriate for user-controlled games.  We don't want the user to
	 * 		"queue up" actions, so any such actions are discarded if the occur
	 * 		when the game is not ready for them.
	 * 
	 * 'false': appropriate for echoed games.  By the fact that an action appears,
	 * 		we know it occurred in a different game when it was in a state such that
	 * 		that action was appropriate.  Instead of discarding, keep the action around
	 * 		for when the game is in the right state to receive it.
	 * @param game
	 * @return
	 */
	public synchronized boolean get_dequeueActionsDiscards( Game game ) {
		return dequeueActionsDiscards ;
	}
	
	/**
	 * When dequeueing actions, should this adapter DISCARD those actions that
	 * the game cannot perform in its given state?  e.g. if a "move left" action
	 * is queued, and the Game is waiting for an action state, should the "move left"
	 * action be removed from the queue upon a call to game_dequeueActions?
	 * 
	 * 'true': appropriate for user-controlled games.  We don't want the user to
	 * 		"queue up" actions, so any such actions are discarded if the occur
	 * 		when the game is not ready for them.
	 * 
	 * 'false': appropriate for echoed games.  By the fact that an action appears,
	 * 		we know it occurred in a different game when it was in a state such that
	 * 		that action was appropriate.  Instead of discarding, keep the action around
	 * 		for when the game is in the right state to receive it.
	 */
	public synchronized void set_dequeueActionsDiscards( boolean discard ) {
		dequeueActionsDiscards = discard ;
	}
	
	
	/**
	 * By default, this is "on."  This provides a slightly more fine-grained
	 * control than shouldUseTimingSystem.  When off, the timing system will
	 * not be ticked.
	 * @param game
	 * @return
	 */
	public synchronized boolean game_shouldTick( Game game ) {
		return shouldTick ;
	}
	
	/**
	 * Sets the value which will be retrieved using game_shouldTick.
	 * @param st
	 */
	public synchronized void set_gameShouldTick( boolean st ) {
		shouldTick = st ; 
	}
	
	
	/**
	 * This method serves double-duty in a way that is (hopefully) hidden
	 * from the Game object.  When this method returns 'true', the Game
	 * object is free to begin a game cycle (beginning with a Piece entering,
	 * ending with the piece leaving, either via a Lock or a move into the
	 * reserve that does not immediately put a replacement in-play).
	 * 
	 * However, the actual function of this method is to coordinate game
	 * states between games, echoes, and coordinators.  Thus, when called
	 * on a coordinated game (a game which holds the "canonical game state"),
	 * all relevant state information is extracted from the Game object
	 * and stored for broadcast.  When called on a user-controlled game
	 * with a coordinated counterpart, or on an echo, this method returning
	 * 'true' should indicate:
	 * 
	 * PRECONDITION: A GameCycleInitialState was available from the coordinated,
	 * 		canonical game.
	 * 
	 * POSTCONDITION: The GameCycleInitialState has been applied, game's fields
	 * 		possibly altered to match it, and the game is ready to begin, by
	 * 		entering a piece.
	 * 
	 * This method should be called once per tick when in the PREPARING game
	 * state, until it returns 'true', in which case we should PREPARE and 
	 * move to the ENTERING game state.
	 * 
	 * @param game The game in question; its state will be read, or altered, or both.
	 * @return Whether it is appropriate to begin a cycle for this game at this time.
	 */
	public synchronized boolean game_beginActionCycle( Game game ) {
		if ( !outgoingActionCycleStatePending ) {
			numOutgoingAttacks = game.aggregateAndClearOutgoingAttackQueue(outgoingAttackQueue, numOutgoingAttacks) ;
			game.copyStateIntoActionCycleStateDescriptor(outgoingActionCycleStateDescriptor) ;
			outgoingActionCycleStatePending = true ;
			
			outgoingActionCycleStateDescriptorMostRecent.takeVals(outgoingActionCycleStateDescriptor) ;
			outgoingActionCycleStateMostRecentSet = true ;
		}
		
		// By default, we should NOT begin a cycle.
		boolean returnValue = false ;
		
		if ( gal != null )
			gal.gal_gameBeginningActionCycle(this, game, outgoingActionCycleStatePending, incomingActionCycleStatePending) ;
		
		if ( incomingActionCycleStatePending ) {
			game.setStateFromActionCycleStateDescriptor(incomingActionCycleStateDescriptor) ;
			outgoingActionCycleStatePending = false ;
			incomingActionCycleStatePending = false ;
			
			// Swap incoming and 'next cycle' incoming attacks.
			ArrayList<AttackDescriptor> temp = incomingAttackQueue ;
			incomingAttackQueue = incomingAttackQueueForNextCycle ;
			incomingAttackQueueForNextCycle = temp ;
			
			numIncomingAttacks = numIncomingAttacksForNextCycle ;
			numIncomingAttacksForNextCycle = 0 ;
			
			// Step past any "end cycle" events in the action queue.
			if ( numIncomingActions > 0 && incomingActionQueue[0] == CODE_END_ACTION_CYCLE ) {
				for ( int i = 1; i < numIncomingActions; i++ )
					incomingActionQueue[i-1] = incomingActionQueue[i] ;
				numIncomingActions-- ;
			}
			
			// Ready to go!
			returnValue = true ;
		}
		
		// Return whether we should start a cycle; also let the listener know,
		// if there is one.
		if ( gal != null )
			gal.gal_gameDidBeginActionCycle(this, game, returnValue) ;
		
		return returnValue ;
	}

	
	
	//////////////////////////////////////////////////////////////
	//
	// Action Cycle Updates
	// They go like this:
	// 
	// When communications gives an incoming update, set the incoming
	// state as "pending."
	//
	// When game gives an update, set the outgoing state as "pending".
	// 		Then, if both are pending, perform the update and give the OK to proceed.
	
	
	
	/**
	 * Adds this attack descriptor to a list of those pending.  These attacks will
	 * be stored until an ActionCycleStateDescriptor arrives that
	 * explicitly uses a cycle's worth.  This allows attacks to "pile up" and
	 * be given an on-screen warning, while still synchronizing their execution.
	 * 
	 * Implementations of this method should assume the provided AttackDescriptor
	 * is fully mutable, and in fact, that its fields may change immediately after this
	 * call.  All necessary information should be extracted from the object before this
	 * call returns.
	 * 
	 * @param attackDescriptor Describes an attack which should be added to a queue.
	 */
	public synchronized void communications_addPendingAttacks( AttackDescriptor attackDescriptor ) {
		// System.err.println("ActionAdapterWithGameIO communications_addPendingAttacks with descriptor " + attackDescriptor.toString()) ;
		if ( incomingActionCycleStatePending ) {
			// If we have already received an ActionCycleState, then pending
			// attacks should be added to the "nextCycle" attack queue.
			if ( numIncomingAttacksForNextCycle == incomingAttackQueueForNextCycle.size() )
				incomingAttackQueueForNextCycle.add( new AttackDescriptor(attackDescriptor) ) ;
			else
				incomingAttackQueueForNextCycle.get(numIncomingAttacksForNextCycle).copyValsFrom(attackDescriptor) ;
			
			numIncomingAttacksForNextCycle++ ;
		}
		else {
			// Otherwise, add them to the current queue.
			if ( numIncomingAttacks == incomingAttackQueue.size() )
				incomingAttackQueue.add( new AttackDescriptor(attackDescriptor) ) ;
			else
				incomingAttackQueue.get(numIncomingAttacks).copyValsFrom(attackDescriptor) ;
			
			numIncomingAttacks++ ;
		}
	}
	
	
	/**
	 * The ActionCycleStateDescriptor includes, as an optional field, a complete
	 * list of all pending attacks.  It also indicates whether the next queued
	 * attack should be executed during the specified cycle, whether or not the
	 * attacks are explicitly provided by the StateDescriptor (if not, they
	 * are assumed to have been retained from previously received AttackDescriptors).
	 * 
	 * Upon receiving this message, the game should be considered free to begin
	 * the next cycle (when the relevant animations and such have concluded,
	 * and the game is otherwise ready to begin).  In other words, calling this
	 * method will result in game_beginActionCycle() returning 'true' the next
	 * time it is called.
	 * 
	 * Implementations of this method should assume the provided ActionCycleStateDescriptor
	 * is fully mutable, and in fact, that its fields may change immediately after this
	 * call.  All necessary information should be extracted from the object before this
	 * call returns.
	 * 
	 * POSTCONDITION: No reference to 'cycleState' or any of its fields is retained by
	 * the ActionAdapter.  The caller may freely change its contents, or rely on them
	 * to be unchanging, as appropriate.
	 * 
	 * @param cycleState
	 */
	public synchronized void communications_setNextActionCycle( ActionCycleStateDescriptor cycleState ) {
		incomingActionCycleStateDescriptor.takeVals( cycleState ) ;		// copyFrom: use exactly the information provided,
																		// allow cycleState to set which fields are included
																		// and which are not.
		incomingActionCycleStatePending = true ;
	}

	
	
	/**
	 * Retrieves the next "outgoing" attack created by the associated
	 * game, and removes it from the outgoing queue.  Returns 'true'
	 * if such an attack was available and was dequeued.  Makes no
	 * guarantees that there isn't ANOTHER outgoing attack after
	 * this one, or that the attacks are properly compressed (i.e.,
	 * that this one and the next one couldn't occur simultaneously).
	 * 
	 * @param ad A storage object for the results.
	 * 
	 * @return Whether there was an outgoing attack available from the game.
	 */
	public synchronized boolean communications_getNextOutgoingAttack( AttackDescriptor ad ) {
		if ( numOutgoingAttacks == 0 )
			return false ;
		
		AttackDescriptor myAttackReference = outgoingAttackQueue.get(0) ;
		
		ad.copyValsFrom( myAttackReference ) ;
		// Move everything down.  The quickest way to do this
		// is to move references, keeping the same objects.
		// Hold 0 temp, shift everything down one from 1 to numOutgoingAttacks-2,
		// then put the old 0 at numOutgoingAttacks-1.
		for ( int i = 0; i < numOutgoingAttacks-1; i++ )
			outgoingAttackQueue.set(i, outgoingAttackQueue.get(i+1)) ;
		outgoingAttackQueue.set(numOutgoingAttacks-1, myAttackReference) ;
		
		// Finally, decrement numOutgoing
		numOutgoingAttacks-- ;
		
		return true ;
	}
	
	
	/**
	 * Retrieves an ActionCycleStateDescriptor from the adapter, assuming one
	 * is ready.  Returns 'true' if the call succeeds and the game is ready to provide
	 * its cycle state.  In that case, the fields of cycleState have been set according
	 * to the current Game state.
	 * 
	 * @param cycleState A storage object for the results
	 * @param useParameterInclusions If 'true', only the 'isIncluded_*' fields in cycleState will be
	 * 			updated.  If 'false', all fields will be updated.
	 * 
	 * @return Whether an update was ready from the game.  If 'true', the fields of cycleState
	 * 			have been appropriately adjusted.
	 */
	public synchronized boolean communications_getNextActionCycle( ActionCycleStateDescriptor cycleState, boolean useParameterInclusions ) {
		if ( outgoingActionCycleStatePending ) {
			cycleState.takeVals(outgoingActionCycleStateDescriptor) ;
			outgoingActionCycleStatePending = false ;
			return true ;
		}
		return false ;
	}
	
	
	/**
	 * Retrieves an ActioncycleStateDescriptor from the adapter, assuming one is ready.
	 * 'communications_getNextActionCycle' clears a buffer, and will return 'false' until
	 * a new ActionCycle is ready.  This method, by contrast, will return the most
	 * recent action cycle that has been available.
	 * 
	 * @param cycleState
	 * @param useParameterInclusions
	 * @return
	 */
	public synchronized boolean communications_getMostRecentActionCycle( ActionCycleStateDescriptor cycleState, boolean useParameterInclusions ) {
		if ( outgoingActionCycleStateMostRecentSet ) {
			cycleState.takeVals(outgoingActionCycleStateDescriptorMostRecent) ;
			return true ;
		}
		return false ;
	}
	
	
	
	/**
	 * Copies up to the next 'length' outgoing actions from the outgoing queue
	 * into 'b', begining at b[ind].
	 * 
	 * These actions are considered as read, and will be removed from the outgoing action
	 * queue (i.e., the next call to this method will not receive those actions).
	 * 
	 * Returns the number of bytes read.
	 * 
	 * @param b
	 * @param ind
	 * @param length
	 * @return
	 */
	public synchronized int communications_readOutgoingActionQueue( byte [] b, int ind, int length ) {
		
		int numDequeued = 0 ;
		
		if ( b == null ) {
			// no need to copy into a null array; feign dequeue.
			numDequeued = Math.max(0, Math.min(length - ind, numOutgoingActions)) ;
		} else {
			while ( numDequeued < numOutgoingActions && numDequeued < length ) {
				b[ind+numDequeued] = outgoingActionQueue[numDequeued] ;
				numDequeued++ ;
			}
		}
		
		// Slide outgoingActionQueue back a bit.
		for ( int i = numDequeued; i < numOutgoingActions; i++ )
			outgoingActionQueue[i-numDequeued] = outgoingActionQueue[i] ;
		numOutgoingActions -= numDequeued ;
		
		return numDequeued ;
		
	}
	
	
	public int communications_getOutgoingActionQueueLength() {
		return numOutgoingActions ;
	}
	
	
	@Override
	public RealtimeData communications_getDisplacementData( RealtimeData rd ) {
		if ( rd == null ) {
			rd = new RealtimeData() ;
		}
		
		rd.set(realtimeData) ;
		return rd ;
	}
	
	
	public int incomingActionBufferSize() {
		return QUEUE_SIZE ;
	}
	
	public int outgoingActionBufferSize() {
		return QUEUE_SIZE ;
	}
	
	
}
