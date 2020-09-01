package com.peaceray.quantro.model.communications;

import android.util.Log;

import com.peaceray.quantro.adapter.action.ActionAdapter;
import com.peaceray.quantro.adapter.action.ActionAdapter.RealtimeData;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.model.descriptors.versioned.ActionCycleStateDescriptor;
import com.peaceray.quantro.model.descriptors.versioned.AttackDescriptor;
import com.peaceray.quantro.model.game.Game;

/**
 * Refactored NoOpClientCommunications 9/27/11.
 * 
 * NoOpClientCommunications provides a "fake" ClientCommunications
 * object for single player games.  We magically connect immediately
 * (ignoring our actual Connection object, which might as well be
 * 'null'), apply our own updates, etc.  We echo pause requests with an
 * immediate call to "paused by", etc.
 * 
 * NoOpClientCommunications is for single-player games ONLY.  We do not
 * allow - at least for now - a fake 2nd player.
 * 
 * @author Jake
 *
 */
public class NoOpClientCommunications extends ClientCommunications {
	
	private static final String TAG = "NoOpClientCommunications" ;
	
	protected Object BUFFER_RUNNING_MUTEX = new Object() ;
	
	// Are we currently "running?"
	protected boolean running ;
	protected boolean paused = false ;
	
	
	
	// Buffer and other structures for performing basic game maintainence, such
	// as reading actions from the Game and applying its own CycleState.
	
	protected byte[] buffer ;
	
	protected ActionCycleStateDescriptor acsd ;
	protected AttackDescriptor ad ;
	
	/**
	 * Constructs a new NoOpClientCommunications object.
	 */
	public NoOpClientCommunications() {
		//Log.d(TAG, "constructor") ;
		actionAdapters = new ActionAdapter[8] ;
		// TODO: Max of 8 players...?
		
		running = false ;
		paused = false ;
		
		// Initialize the other class fields.
		nonce = null ;
		personalNonce = null ;
		name = null ;

		localActionAdapter = -1 ;
		
		delegate = null ;
		
		connection = null ;
		//Log.d(TAG, "constructor over") ;
	}

	
	/**
	 * Starts things going.  Very, very simple.
	 */
	@Override
	public void start() throws IllegalStateException {
		//Log.d(TAG, "start") ;
		if ( running )
			throw new IllegalStateException("Can't start a NoOpClientCommunications twice!") ;
		
		synchronized ( BUFFER_RUNNING_MUTEX ) {
			running = true ;
			
			// Nows a good time for some allocation.  We assume that 'ActionAdapters'
			// have been set by this point.
			//Log.d(TAG, "allocating buffer") ;
			buffer = new byte[actionAdapters[0].outgoingActionBufferSize()] ;
			
			//Log.d(TAG, "allocating acsd") ;
			acsd = new ActionCycleStateDescriptor(1,1) ;
			
			//Log.d(TAG, "allocating AttackDescription") ;
			ad = new AttackDescriptor(1,1) ;
		}
		
		// Perform a series of fake "connected!" updates to the delegate.
		//Log.d(TAG, "calling delegate methods") ;
		delegate.ccd_connectionStatusChanged(
				this, MessagePassingConnection.Status.CONNECTED ) ;
		//Log.d(TAG, "connection status changed called") ;
		delegate.ccd_messageWelcomeToServer(this) ;
		//Log.d(TAG, "welcome to server called") ;
		delegate.ccd_messageTotalPlayerSlots(this,1) ;
		//Log.d(TAG, "total player slots called") ;
		delegate.ccd_messageLocalPlayerSlot(this,0) ;
		//Log.d(TAG, "local player slot called") ;
		
		// Paused?  Unpaused?
		//Log.d(TAG, "paused? " + paused) ;
		boolean isPaused ;
		synchronized ( BUFFER_RUNNING_MUTEX ) {
			isPaused = paused ;
		}
		if ( isPaused )
			delegate.ccd_messagePaused(this,null) ;
		else
			delegate.ccd_messageGo(this) ;
		//Log.d(TAG, "paused " + paused + " or go " + (!paused) + " called") ;
		//Log.d(TAG, "start done") ;
	}

	/**
	 * Stops the thread.  Again, very simple.
	 */
	@Override
	public void stop() throws IllegalStateException {
		running = false ;
	}
	
	@Override
	public void recycle() {
		super.recycle() ;
		buffer = null ;
		acsd = null ;
		ad = null ;
	}

	@Override
	public void join() throws IllegalStateException {
		while ( running ) {
			try {
				Thread.sleep(10) ;
			} catch( InterruptedException e ) {
				return ;
			}
		}
	}
	
	/**
	 * Sends a name update if different from the most recent one sent.
	 * 
	 * @param name
	 * @throws IllegalStateException
	 */
	public void sendNameIfChanged( String name ) throws IllegalStateException {
		// echo this
		delegate.ccd_messagePlayerName(this, 0, name) ;
	}

	
	/**
	 * Fake pause!  Just pause the game immediately by telling the delegate to.
	 */
	@Override
	public void sendPauseRequest(boolean pauseOn) throws IllegalStateException {
		
		// NoOpClientCommunications don't actually connect to a server,
		// so we never receive pause/unpause status updates.
		
		// Instead, we just set a toggle.  If the Client is currently 
		// running, send the message to the delegate.  Otherwise, 
		// the thread will send the message the next time it starts.x
		boolean sendMessage ;
		synchronized ( BUFFER_RUNNING_MUTEX ) {
			paused = pauseOn ;
			sendMessage = running ;
		}
		
		Log.d(TAG, "sendPauseRequest") ;
		if ( sendMessage ) {
			if ( paused ) {
				Log.d(TAG, "calling delegate.ccd_messagePaused") ;
				delegate.ccd_messagePaused( this, new boolean[] {true} ) ;
			} else {
				Log.d(TAG, "calling delegate.ccd_messageGo") ;
				delegate.ccd_messageGo(this) ;
			}
		}
	}
	
	
	/**
	 * The provided Game object just Initialized (which is the first step
	 * in a new Game State's operation; a loaded game will not Initialize).
	 * 
	 * @param caller
	 * @param game
	 */
	public synchronized void gal_gameDidInitialize( ActionAdapter caller, Game game ) {
		if ( delegate != null )
			delegate.ccd_messageInitialized(this, 0) ;
	}
	
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
	public synchronized void gal_gameDidDequeueActions( ActionAdapter caller, Game game, boolean returnValue ) {
		//Log.d(TAG, "gal_gameDidDequeueActions") ;
		// Not important.  We ignore this.
		//Log.d(TAG, "gal_gameDidDequeueActions done") ;
	}
	
	
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
	public void gal_gameBeginningActionCycle( ActionAdapter caller, Game game, boolean outgoingActionCycleStatePending, boolean incomingActionCycleStatePending ) {
		// NOTE: upon calling this method, the ActionAdapter has copied the next CycleState
		// as held by the Game object.  If incomingActionCyclePending is not true,
		// pass it its own ACSD.
		if ( outgoingActionCycleStatePending && !incomingActionCycleStatePending ) {
			if ( caller.communications_getNextActionCycle(acsd, true) )
				caller.communications_setNextActionCycle(acsd) ;
		}
		while ( caller.communications_getNextOutgoingAttack(ad) ) {
			// eat and eat and eat some more
		}
	}
	
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
	public synchronized void gal_gameDidBeginActionCycle( ActionAdapter caller, Game game, boolean returnValue ) {
		// nothing
	}
	
	
	/**
	 * The associated Game object was given as a parameter to
	 * game_didMove*, didTurn*, didFall, didLock, didUseReserve, didDrop,
	 * or didEndActionCycle.  In other words, we have added to the 
	 * OutgoingActionQueue.  This method is called as the last operation
	 * of the method called on 'caller,' in other words, the outgoing
	 * action queue has already been updated.
	 * @param game
	 */
	public synchronized void gal_gameDidEnqueueActions( ActionAdapter caller, boolean endCycle ) {
		//Log.d(TAG, "gal_gameDidEnqueueActions") ;
		// Perform cursory updates: grab the action updates from
		// the game.  This just prevents the outgoingBuffer from growing too large.
		synchronized( BUFFER_RUNNING_MUTEX ) {
			if ( running ) {
				caller.communications_readOutgoingActionQueue(buffer, 0, buffer.length) ;
				
				// Attacks to dequeue?
				// We must retrieve these attacks so they don't build up.
				while ( caller.communications_getNextOutgoingAttack(ad) ) {
					// What's it look like!!!!!???
					// //Log.d(TAG, "NoOpClientCommunication: outgoing attack descriptor of game 0") ;
					// //Log.d(TAG, ad.toString()) ;
					
					// Queue it up as an echo!
					// TODO: The line below "echoes" attacks back at the player.
					// It should be removed for a normal 1-player experience.
					// However, note that this is actually kinda fun!  Maybe
					// a future game type could be built around this idea?
					// (e.g. an AttackSystem that generates attacks UNLESS rows are cleared?)
					// actionAdapters[0].communications_addPendingAttacks(ad) ;
				}
				//Log.d(TAG, "gal_gameDidEnqueueActions done") ;
			}
		}
	}
	
	
	/**
	 * A collision occurred!  This happens when attempting to enter a piece.
	 */
	public void gal_gameDidCollide(ActionAdapter caller) {
		this.delegate.ccd_messageGameOver(this, new boolean[]{false}) ;
	}
	
	
	/**
	 * A level-up!
	 * @param caller
	 */
	public void gal_gameDidLevelUp(ActionAdapter caller) {
		this.delegate.ccd_messageLevelUp(this, 0) ;
	}
	
	/**
	 * A level-up!
	 * @param caller
	 */
	public void gal_gameAboutToLevelUp(ActionAdapter caller) {
		this.delegate.ccd_messageAboutToLevelUp(this, 0) ;
	}
	
	/**
	 * This game issued an attack out-of-sequence!
	 * @param caller
	 */
	public void gal_gameHasOutOfSequenceAttack(ActionAdapter caller) {
		synchronized( BUFFER_RUNNING_MUTEX ) {
			if ( running ) {
				// Attacks to dequeue?
				// We must retrieve these attacks so they don't build up.
				while ( caller.communications_getNextOutgoingAttack(ad) ) {
					// Queue it up as an echo!
					// TODO: The line below "echoes" attacks back at the player.
					// It should be removed for a normal 1-player experience.
					// However, note that this is actually kinda fun!  Maybe
					// a future game type could be built around this idea?
					// (e.g. an AttackSystem that generates attacks UNLESS rows are cleared?)
					// actionAdapters[0].communications_addPendingAttacks(ad) ;
				}
			}
		}
	}
	

	/**
	 * The game's displacement has passed the threshold for an integer;
	 * for example, going from 0.9 to 1.0, from 1.87 to 2.02, from
	 * 1.1 to 0.1, etc.
	 * @param caller
	 */
	public void gal_gameDisplacementDidChangeInteger(ActionAdapter caller, RealtimeData dd) {
		// no effect.  We don't really care.
	}

}
