package com.peaceray.quantro.adapter.action;

import com.peaceray.quantro.model.descriptors.versioned.ActionCycleStateDescriptor;
import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.game.Game;


/**
 * An ActionAdapter provides a connective layer between 
 * a ControlsAdapter, a Game, and a CoordinatorCommunicationLayer.
 * 
 * Input/Output is handled using method calls to ActionAdapter which
 * itself does not run a thread, nor retain references to the callees.
 * 
 * @author Jake
 *
 */
public abstract class ActionAdapter {

	/**
	 * An object which is interested in updates from any game
	 * object that connects this ActionAdapter.  If set, these methods
	 * will be called upon the appropriate game_* method being called.
	 * 
	 * Note that these methods will be called from the same thread which
	 * called game_* - in fact, within the method call of game_* itself
	 * - so you should minimize the complexity of these callbacks.  If
	 * complicated processing is needed, transfer the action to a new thread.
	 * 
	 * @author Jake
	 *
	 */
	public interface GameActionListener {
		
		
		/**
		 * The provided Game object just Initialized (which is the first step
		 * in a new Game State's operation; a loaded game will not Initialize).
		 * 
		 * @param caller
		 * @param game
		 */
		public void gal_gameDidInitialize( ActionAdapter caller, Game game ) ;
		
		/**
		 * The provided 'game' object was given as a parameter
		 * in game_dequeueActions( game ).  This method is called
		 * as the last operation in game_dequeueActions.  We provide
		 * the return value which we will return immediately after this
		 * method call ends.
		 * 
		 * @param game
		 * @param returnValue
		 */
		public void gal_gameDidDequeueActions( ActionAdapter caller, Game game, boolean returnValue ) ;
		
		
		/**
		 * The provided 'game' object was given as a parameter to
		 * game_beginActionCycle(game).  This method is called 
		 * after retrieving any outgoing action cycles available,
		 * but before applying (or ATTEMPTING to apply) any
		 * incoming ones.
		 * 
		 * Regardless of the result of this method call,
		 * gal_gameDidBeginActionCycle will be called shortly thereafter.
		 * 
		 * The reason this method is included is primarily for the benefit
		 * of Progression mode, so we never have to 'tick' without change
		 * until we're ready to.
		 * 
		 * @param caller
		 * @param game
		 * @param returnValue
		 */
		public void gal_gameBeginningActionCycle( ActionAdapter caller, Game game, boolean outgoingActionCycleStatePending, boolean incomingActionCycleStatePending ) ;
		
		/**
		 * The provided 'game' object was given as a parameter to
		 * game_beginActionCycle(game).  This method is called
		 * as the last operation in game_dequeueActions.  We provide
		 * the return value which we will return immediately after this
		 * method call ends.
		 * 
		 * @param game
		 * @param returnValue
		 */
		public void gal_gameDidBeginActionCycle( ActionAdapter caller, Game game, boolean returnValue ) ;
		
		
		/**
		 * The associated Game object was given as a parameter to
		 * game_didMove*, didTurn*, didFall, didLock, didUseReserve, didDrop,
		 * or didEndActionCycle.  In other words, we have added to the 
		 * OutgoingActionQueue.  This method is called as the last operation
		 * of the method called on 'caller,' in other words, the outgoing
		 * action queue has already been updated.
		 * @param game
		 * @param endActionCycle Does this end an action cycle?
		 */
		public void gal_gameDidEnqueueActions( ActionAdapter caller, boolean actionCycleEnds ) ;
		
		
		/**
		 * The associated Game object has reported a collision.  This occurs immediately
		 * after an attempt to "enter" a piece
		 * @param caller
		 */
		public void gal_gameDidCollide( ActionAdapter caller ) ;
		
		/**
		 * A level-up!
		 * @param caller
		 */
		public void gal_gameDidLevelUp(ActionAdapter caller) ;
		
		/**
		 * A level-up is pending!
		 * @param caller
		 */
		public void gal_gameAboutToLevelUp(ActionAdapter caller) ;
		
		
		/**
		 * This game issued an attack out-of-sequence!
		 * @param caller
		 */
		public void gal_gameHasOutOfSequenceAttack(ActionAdapter caller) ;
		
		
		/**
		 * The game's displacement has passed the threshold for an integer;
		 * for example, going from 0.9 to 1.0, from 1.87 to 2.02, from
		 * 1.1 to 0.1, etc.
		 * @param caller
		 */
		public void gal_gameDisplacementDidChangeInteger(ActionAdapter caller, RealtimeData dd) ;
		
	}
	
	
	GameActionListener gal = null ;
	
	public synchronized void setGameActionListener( GameActionListener gal ) {
		this.gal = gal ;
	}
	
	
	// Here are some codes for certain actions which might need to be performed.
	
	public final static byte CODE_MOVE_LEFT = (byte)(Byte.MIN_VALUE 		+ 0) ;
	public final static byte CODE_MOVE_RIGHT = (byte)(Byte.MIN_VALUE 		+ 1) ;
	public final static byte CODE_TURN_CW = (byte)(Byte.MIN_VALUE 			+ 2) ;
	public final static byte CODE_TURN_CCW = (byte)(Byte.MIN_VALUE 			+ 3) ;
	public final static byte CODE_TURN_CW_180 = (byte)(Byte.MIN_VALUE 		+ 4) ;
	public final static byte CODE_TURN_CCW_180 = (byte)(Byte.MIN_VALUE 		+ 5) ;
	
	// turns with a kick preference 
	public final static byte CODE_TURN_CW_LEAN_LEFT = (byte)(Byte.MIN_VALUE 		+ 6) ;		// -122
	public final static byte CODE_TURN_CW_LEAN_RIGHT = (byte)(Byte.MIN_VALUE 		+ 7) ;		// -121
	public final static byte CODE_TURN_CW_LEAN_DOWN = (byte)(Byte.MIN_VALUE 		+ 8) ;		// -120
	public final static byte CODE_TURN_CW_LEAN_DOWN_LEFT = (byte)(Byte.MIN_VALUE 	+ 9) ;		// -119
	public final static byte CODE_TURN_CW_LEAN_DOWN_RIGHT = (byte)(Byte.MIN_VALUE 	+ 10) ;		// -118
	
	public final static byte CODE_TURN_CCW_LEAN_LEFT = (byte)(Byte.MIN_VALUE 		+ 11) ;		// -117
	public final static byte CODE_TURN_CCW_LEAN_RIGHT = (byte)(Byte.MIN_VALUE 		+ 12) ;		// -116
	public final static byte CODE_TURN_CCW_LEAN_DOWN = (byte)(Byte.MIN_VALUE 		+ 13) ;		// -115
	public final static byte CODE_TURN_CCW_LEAN_DOWN_LEFT = (byte)(Byte.MIN_VALUE 	+ 14) ;		// -114
	public final static byte CODE_TURN_CCW_LEAN_DOWN_RIGHT = (byte)(Byte.MIN_VALUE 	+ 15) ;		// -113
	
	public final static byte CODE_TURN_CW_180_LEAN_LEFT = (byte)(Byte.MIN_VALUE 		+ 16) ;		// -112
	public final static byte CODE_TURN_CW_180_LEAN_RIGHT = (byte)(Byte.MIN_VALUE 		+ 17) ;		// -111
	public final static byte CODE_TURN_CW_180_LEAN_DOWN = (byte)(Byte.MIN_VALUE 		+ 18) ;		// -110
	public final static byte CODE_TURN_CW_180_LEAN_DOWN_LEFT = (byte)(Byte.MIN_VALUE 	+ 19) ;		// -109
	public final static byte CODE_TURN_CW_180_LEAN_DOWN_RIGHT = (byte)(Byte.MIN_VALUE 	+ 20) ;		// -108
	
	public final static byte CODE_TURN_CCW_180_LEAN_LEFT = (byte)(Byte.MIN_VALUE 		+ 21) ;		// -107
	public final static byte CODE_TURN_CCW_180_LEAN_RIGHT = (byte)(Byte.MIN_VALUE 		+ 22) ;		// -106
	public final static byte CODE_TURN_CCW_180_LEAN_DOWN = (byte)(Byte.MIN_VALUE 		+ 23) ;		// -105
	public final static byte CODE_TURN_CCW_180_LEAN_DOWN_LEFT = (byte)(Byte.MIN_VALUE 	+ 24) ;		// -104
	public final static byte CODE_TURN_CCW_180_LEAN_DOWN_RIGHT = (byte)(Byte.MIN_VALUE 	+ 25) ;		// -103
	
	// flips with a kick preference
	public final static byte CODE_FLIP = (byte) (Byte.MIN_VALUE 						+ 26) ;		// -102
	public final static byte CODE_FLIP_LEAN_LEFT = (byte) (Byte.MIN_VALUE 				+ 27) ;		// -101
	public final static byte CODE_FLIP_LEAN_RIGHT = (byte) (Byte.MIN_VALUE 				+ 28) ;		// -100
	public final static byte CODE_FLIP_LEAN_DOWN = (byte) (Byte.MIN_VALUE 				+ 29) ;		// -99
	public final static byte CODE_FLIP_LEAN_DOWN_LEFT = (byte) (Byte.MIN_VALUE 			+ 30) ;		// -98
	public final static byte CODE_FLIP_LEAN_DOWN_RIGHT = (byte) (Byte.MIN_VALUE 		+ 31) ;		// -97
	
	// RESERVE: include different "leans"
	public final static byte CODE_USE_RESERVE = (byte)(Byte.MIN_VALUE 					+ 32) ;		// -96
	public final static byte CODE_USE_RESERVE_LEAN_LEFT = (byte)(Byte.MIN_VALUE 		+ 33) ;		// -95
	public final static byte CODE_USE_RESERVE_LEAN_RIGHT = (byte)(Byte.MIN_VALUE 		+ 34) ;		// -94
	public final static byte CODE_USE_RESERVE_LEAN_DOWN = (byte)(Byte.MIN_VALUE 		+ 35) ;		// -93
	public final static byte CODE_USE_RESERVE_LEAN_DOWN_LEFT = (byte)(Byte.MIN_VALUE 	+ 36) ;		// -92
	public final static byte CODE_USE_RESERVE_LEAN_DOWN_RIGHT = (byte)(Byte.MIN_VALUE 	+ 37) ;		// -91
	
	
	public final static byte CODE_DROP = (byte)(Byte.MIN_VALUE 				+ 38) ;		// -90
	public final static byte CODE_FALL = (byte)(Byte.MIN_VALUE 				+ 39) ;		// -89
	public final static byte CODE_LOCK = (byte)(Byte.MIN_VALUE 				+ 40) ;		// -88
	public final static byte CODE_AUTOLOCK = (byte)(Byte.MIN_VALUE          + 41) ;		// -87
	public final static byte CODE_FALL_OR_AUTOLOCK = (byte)(Byte.MIN_VALUE 			+ 42) ;		// -86
	
	public final static byte CODE_END_ACTION_CYCLE = (byte)(Byte.MIN_VALUE 	+ 43) ;		// -85
	public final static byte CODE_ADVANCE = (byte)(Byte.MIN_VALUE 			+ 44) ;	// -84
	
	
	
	//////////////////////////////////////////////////////////////
	//
	// Important maintainance.  (<-- still can't spell that word)
	// If we have received a full synchronization message, it is important
	// that we empty the action adapter of any queued events.  As
	// of the moment of Sync we are COMPLETELY UP TO DATE - any incoming
	// actions are now irrelevant, and any OUTGOING actions are incorrect,
	// since they were set for a now-incorrect game state.
	
	public abstract void emptyActionQueues() ;
	
	public abstract void emptyAllQueues() ;
	
	
	
	//////////////////////////////////////////////////////////////
	//
	// Incoming information from the ControlsAdapter.
	// These method calls queue updates which are passed to the
	// local game (my game) when appropriate.
	//
	// Depending on the implementation, a subclass of ActionAdapter may
	// ignore these methods or leave them unimplemented.  For example,
	// the ActionAdapter for an echoed game will never receive user
	// input (instead it receives instructions from a GameCoordinator,
	// which itself is receiving actions that originally derive from user
	// input, probably on another device.
	
	public static final int MOVE_DIRECTION_NONE = 0 ;
	public static final int MOVE_DIRECTION_LEFT = 1 ;
	public static final int MOVE_DIRECTION_RIGHT = 2 ;
	public static final int MOVE_DIRECTION_DOWN = 3 ;
	public static final int MOVE_DIRECTION_DOWN_LEFT = 4 ;
	public static final int MOVE_DIRECTION_DOWN_RIGHT = 5 ;
	
	
	public abstract void controls_move(int moveDirection) ;
	public abstract void controls_turnCW(int moveDirection) ;
	public abstract void controls_turnCCW(int moveDirection) ;
	public abstract void controls_turnCW180(int moveDirection) ;
	public abstract void controls_turnCCW180(int moveDirection) ;
	public abstract void controls_flip(int moveDirection) ;
	public abstract void controls_useReserve(int moveDirection) ;
	
	
	public abstract void controls_fall() ;
		// fall the piece 1 space, if possible.  If no space is available, do nothing
	public abstract void controls_autolock() ;
	public abstract void controls_fall_or_autolock() ;
		// performs a piece lock IF a long enough wait would lock the piece in
		// its current position.  i.e., if the piece is currently resting on
		// the ground and waiting on a ts.canLock() response, this will lock 
		// immediately.  Otherwise, it has no effect.
	
	public abstract void controls_slide( int moveCode, boolean isSliding ) ;
	// Sliding is a "toggleable" action; set slideLeft( true ) and the
	// adapter will apply leftward movements as often as appropriate,
	// until slideLeft( false ) is called.
	
	public abstract void controls_slideOnce( int moveCode ) ;
	// Alternate view of "controls_slide," which toggles sliding state.  This
	// takes the next available opportunity to slide and does so.  If the game
	// is not in a "slideable" state at next opportunity, this method has no effect.

	public abstract void controls_fastFall( boolean isFastFalling ) ;
	public abstract void controls_fastFall_and_autolock( boolean isFastFalling ) ;
	// Fastfalling puts the game in a state where it uses a greatly
	// reduced fall speed.  This has the effect of producing an immediate
	// 'fall' when it activates, and continuing the fall each time actions
	// are dequeued.
	
	//////////////////////////////////////////////////////////////
	//
	// Update requests from a Game object.
	//
	// These methods allow the Game object to request updates and
	// policies from the ActionAdapter.  Some simply return a value
	// (e.g., whether to follow the TimingSystem's suggestion to drop
	// a piece), others perform state changes on the Game object
	// itself.  These methods provide the necessary abstraction for
	// the Game object to be agnostic w.r.t. whether it is echoing
	// moves from another game, performing user-entered actions, etc.
	
	/**
	 * Performs on the 'game' object the queued actions, from either 
	 * a ControlsAdapter or some other source (e.g. if adapting for
	 * an echoed game).  Will call methods on 'game' such as 
	 * moveLeft, moveRight, drop, fall, lock, useReserve, etc.
	 * 
	 * @param game The game object on which to dequeue the actions.
	 * @return Whether any actions were dequeued.
	 */
	public abstract boolean game_dequeueActions( Game game ) ;
	
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
	public abstract void game_fakeDequeueActions( Game game ) ;
	
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
	public abstract boolean game_shouldUseTimingSystem( Game game ) ;
	
	/**
	 * Set, for this ActionAdapter, whether Game objects should use
	 * the TimingSystem to advance events.
	 * @param useTS
	 */
	public abstract void set_gameShouldUseTimingSystem( boolean useTS ) ;
	
	
	
	/**
	 * By default, this is "on."  This provides a slightly more fine-grained
	 * control than shouldUseTimingSystem.  When off, the timing system will
	 * not be ticked.
	 * @param game
	 * @return
	 */
	public abstract boolean game_shouldTick( Game game ) ;
	
	/**
	 * Sets the value which will be retrieved using game_shouldTick.
	 * @param st
	 */
	public abstract void set_gameShouldTick( boolean st ) ;
	
	
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
	public abstract boolean get_dequeueActionsDiscards( Game game ) ;
	
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
	public abstract void set_dequeueActionsDiscards( boolean discard ) ;
	
	
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
	public abstract boolean game_beginActionCycle( Game game ) ;
	
	
	//////////////////////////////////////////////////////////////
	//
	// Update statements from a Game object.
	//
	// When the game changes its state, these methods are called.
	// There is a bit of redundancy here - after all, shouldn't the
	// Adapter know that these updates were coming, based on controls
	// or communication input?
	//
	// Probably, except for a few boundary cases (such as the user
	// queueing a moveLeft command when such a command cannot be
	// executed do to a collision).  However, separating these methods
	// keeps things cleaner, and the ActionAdapter needs to keep track
	// of fewer things.
	//
	// For an echoed game, these methods probably do nothing at all.
	// for a player's game, these updates are sent to the coordinated
	// game.
	
	public abstract void game_didMove( int moveDirection ) ;
	public abstract void game_didTurnCW( int moveDirection ) ;
	public abstract void game_didTurnCCW( int moveDirection ) ;
	public abstract void game_didTurnCW180( int moveDirection ) ;
	public abstract void game_didTurnCCW180( int moveDirection ) ;
	public abstract void game_didFlip( int moveDirection ) ;
	public abstract void game_didUseReserve( int moveDirection ) ;
	
	/**
	 * The player used a special that issued an attack.  We provide a specific
	 * method for this, because there is no obvious and easy time to check,
	 * as there is for most attacks (which are issued at the end of an 
	 * action cycle).
	 * @param game
	 */
	public abstract void game_didIssueSpecialAttack( Game game ) ;
	
	public abstract void game_didFall() ;
	public abstract void game_didLock() ;
	
	public abstract void game_didEnter() ;
	
	// If, for whatever reason, there is no longer a falling piece being
	// moved about but the game has not ended untimely (e.g. it locked,
	// it was moved to Reserve and no piece replaced it, etc.).
	public abstract void game_didEndActionCycle() ;
	
	public abstract void game_didAdvance() ;
	
	public abstract void game_hasOutgoingAttack( Game game ) ;
	
	/**
	 * A collision occurred!  This happens when attempting to enter a piece.
	 */
	public abstract void game_didCollide() ;
	
	/**
	 * A level-up occurred!
	 */
	public abstract void game_didLevelUp() ;
	
	/**
	 * A level-up is pending!
	 */
	public abstract void game_aboutToLevelUp() ;
	
	/**
	 * Called once for a GameState, immediately before it transitions out of
	 * INITIALIZATION and to SYNCHRONIZATION.
	 */
	public abstract void game_aboutToInitialize( Game game ) ;
	
	
	/**
	 * Indicates that the current row displacement for the game has changed.
	 * @param displacement
	 */
	public abstract void game_didChangeRealtime( Game game, long millisecondsTicked, double displacementSeconds, double rowsDisplacedAndTransferred ) ;
	
	
	//////////////////////////////////////////////////////////////
	//
	// Updates from a communications layer
	//
	// These methods allow a communications layer to provide updates.
	// By convention, the communications layer does not examine game
	// information, instead sending and receiving byte arrays.
	
	public abstract void communications_enqueueMove( int moveDirection ) ;
	public abstract void communications_enqueueTurnCW( int moveDirection ) ;
	public abstract void communications_enqueueTurnCCW( int moveDirection ) ;
	public abstract void communications_enqueueTurnCW180( int moveDirection ) ;
	public abstract void communications_enqueueTurnCCW180( int moveDirection ) ;
	public abstract void communications_enqueueUseReserve( int moveDirection ) ;
	
	public abstract void communications_enqueueFall() ;
	public abstract void communications_enqueueLock() ;
	
	public abstract void communications_enqueueEndActionCycle() ;
	
	/**
	 * Equivalent to calling the appropriate method above for each move in the provided queue.
	 * Note that some conversion applies between the "moveDirection" methods and the move
	 * codes in the provided queue, but the method behind it should be pretty straightforward
	 * from the code / direction names.
	 * @param moveQ
	 */
	public abstract void communications_enqueueActions( byte [] moveQ, int index, int length ) ;
	
	
	/**
	 * Removes all pending actions in the queue.  This method is
	 * dangerous to use!  Only call this when you're certain it's time
	 * to completely clear the pending actions, such as when a full 
	 * synchronization update is pending.
	 */
	public abstract void communications_clearForSynchronization() ;
	
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
	public abstract void communications_addPendingAttacks( AttackDescriptor attackDescriptor ) ;
	
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
	public abstract void communications_setNextActionCycle( ActionCycleStateDescriptor cycleState ) ;
	
	
	
	/**
	 * Sets the provided DisplacementData object and returns a reference to it.
	 * If 'null' is provided, allocates a new object, populates it, and
	 * returns it.
	 * 
	 * @param holder
	 * @return
	 */
	public abstract RealtimeData communications_getDisplacementData( RealtimeData holder ) ;
	
	//////////////////////////////////////////////////////////////
	//
	// Updates to a communications layer
	//
	// These methods allow a communications layer to receive updates
	// from the Adapter.
	
	/**
	 * Retrieves the next "outgoing" attack created by the associated
	 * game, and removes it from the outgoing queue.  Returns 'true'
	 * if such an attack was available and was dequeued.
	 * @param ad A storage object for the results.
	 * 
	 * @return Whether there was an outgoing attack available from the game.
	 */
	public abstract boolean communications_getNextOutgoingAttack( AttackDescriptor ad ) ;
	
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
	public abstract boolean communications_getNextActionCycle( ActionCycleStateDescriptor cycleState, boolean useParameterInclusions ) ;
	
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
	public abstract boolean communications_getMostRecentActionCycle( ActionCycleStateDescriptor cycleState, boolean useParameterInclusions ) ;
	
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
	public abstract int communications_readOutgoingActionQueue( byte [] b, int ind, int length ) ;
	
	/**
	 * If communications_readOutgoingActionQueue is called immediately, how many
	 * actions will be read into from the queue (assuming space for all)?
	 * 
	 * @return
	 */
	public abstract int communications_getOutgoingActionQueueLength() ;
	
	
	//////////////////////////////////////////////////////////////
	//
	// Information about this adapter to a communications layer
	//
	// These methods allow a communications layer to receive updates
	// from the Adapter.
	
	public abstract int incomingActionBufferSize() ;
	
	public abstract int outgoingActionBufferSize() ;
	
	
	public static class RealtimeData {
		private long mMillisecondsTicked ;
		private double mDisplacementSeconds ;
		private double mDisplacementRows ;
		
		public RealtimeData() {
			mMillisecondsTicked = 0 ;
			mDisplacementSeconds = 0 ;
			mDisplacementRows = 0 ;
		}
		
		public RealtimeData( RealtimeData rd ) {
			this() ;
			set(rd) ;
		}
		
		public void set( long millis, double displacementSeconds, double displacementRows ) {
			this.mMillisecondsTicked = millis ;
			mDisplacementSeconds = displacementSeconds ;
			mDisplacementRows = displacementRows ;
		}
		
		public void set( RealtimeData rd ) {
			mMillisecondsTicked = rd.mMillisecondsTicked ;
			mDisplacementSeconds = rd.mDisplacementSeconds ;
			mDisplacementRows = rd.mDisplacementRows ;
		}
		
		public long getMillisecondsTicked() {
			return mMillisecondsTicked ;
		}
		
		public double getDisplacementSeconds() {
			return mDisplacementSeconds ;
		}
		
		public double getDisplacementRows() {
			return mDisplacementRows ; 
		}
	}
	
}
