package com.peaceray.quantro.model.systems.displacement;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.q.QInteractions;


/**
 * Displacement is a real-time phenomenon in certain game types, where
 * new row content "pushes up" existing content.  Displacement functions
 * as an alternative to garbage rows.  The primary differences (apart
 * from actual row content):
 * 
 * 1. Garbage rows are triggered "attacks," resulting from different events
 * 		in-game (such as a level-up, your opponent clearing rows, etc.)
 * 	  Displacement rows are added with the passage of time; no other trigger
 * 		is needed.
 * 
 * 2. Garbage rows appear in a single event which adds them to the current
 * 		blockfield.  This change is animated by the draw code, and then the
 * 		game continues to advance.
 * 	  Displacement rows are introduced gradually, pushing up existing rows, although
 * 		for gameplay purposes they enter play atomically (at the beginning
 * 		of a cycle).
 * 
 * 3. Garbage rows, when pending, are invisible to the player.  Displacement,
 * 		however, gradually queues up and it may be useful to display not just
 * 		the existing rows, but those soon to appear.
 * 
 * @author Jake
 *
 */
public abstract class DisplacementSystem implements SerializableState {

	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this CollisionSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public abstract GameInformation getGameInformation() ;
	
	/**
	 * getQInteractions: Collisions are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public abstract QInteractions getQInteractions() ;
	
	
	/**
	 * Displacement row content is determined by this pseudorandom value.
	 * @param pseudorandom
	 * @return
	 */
	public abstract DisplacementSystem setPseudorandom( int pseudorandom ) ;
	
	
	/**
	 * Adjust displacement according to the number of advanced milliseconds.
	 * 
	 * @param milliseconds
	 * @return Whether this call has altered the number of displaced rows.
	 */
	public abstract boolean tick( double seconds ) ;
	
	
	/**
	 * Fills the provided blockfield with displacement rows.
	 * 
	 * This, generally speaking, should be the first call made.  After
	 * this, call 'displace.'
	 * 
	 * @param displacementRows
	 */
	public abstract void prefill( byte [][][] displacementRows ) ;
	
	
	/**
	 * Displaces 'rows' rows of blocks from the top of displacementRows into
	 * the bottom of 'blockfield.'  
	 * 
	 * Blocks get pushed up in blockfield, and in displacement rows.
	 * 
	 * @param blockfield
	 * @param displacementRows
	 * @param rows
	 */
	public abstract void transferDisplacedRows( byte [][][] blockfield, byte [][][] displacementRows, int rows ) ;
	
	
	/**
	 * A convenience method: equivalent to getDisplacedRows() >= 1.
	 * @return
	 */
	public boolean isReadyToTransfer() {
		return getDisplacedRows() >= 1 ;
	}
	
	
	/**
	 * Informs the displacement system that the specified number
	 * of rows, which may be positive or negative, should be 
	 * "accelerated out."  The system itself determines what this
	 * means.
	 * 
	 * For example, a positive number will indicate that the rate of
	 * displacement should increase until a net of 'rows' extra
	 * rows has been added on top of the normal displacement amount.
	 * 
	 * A negative number indicates that the rate of displacement
	 * should decrease -- possibly stop or even go backwards --
	 * until a net of 'rows' rows which SHOULD have been displacement
	 * have not yet been displaced.
	 * 
	 * This method may be called multiple times before the first displacement
	 * has been wholly accounted for.  For that reason, you should track
	 * 'total accelerated displacement' or 'yet-unprocessed accelerated
	 * displacement.'
	 * 
	 * @param rows The number of rows to displace via acceleration.
	 * 		May be possitive or negative.
	 * @return Whether this call will have any effect on our behavior.
	 */
	public abstract boolean accelerateDisplacement( double rows ) ;
	
	
	/**
	 * Does this system ever produce displacement?  e.g.,
	 * a DeadDisplacementSystem -- used for gametypes 
	 * pre-April 2013, would return 'false.'
	 * @return
	 */
	public abstract boolean displaces() ;
	
	/**
	 * Returns the number of displaced rows.  The integer component
	 * is the number of full-rows ready to introduce to the game.
	 * If displaying displaced rows before their introduction to gameplay,
	 * this double value indicates the real number of rows "below the bottom"
	 * that we should display.
	 * 
	 * Note that this result will be decremented by a call to transferDisplacedRows.
	 * 
	 * @return
	 */
	public abstract double getDisplacedRows() ;
	
	
	/**
	 * Returns the total number of transferred rows.
	 * 
	 * Note that "displaced rows" refers to the number of rows by which
	 * the standard blockfield has been displaced (a real number); transferred
	 * rows, an integer, refers to the number of whole rows which has
	 * been ADDED to the blockfield by 'transferDisplacedRows.'
	 * 
	 * While 'transferDisplacedRows' alters the result of getDisplacedRows(),
	 * it will not alter the result of this method.
	 * 
	 * @return
	 */
	public abstract double getDisplacedAndTransferredRows() ;
	
	
	/**
	 * Returns the total number of ticks that have passed.  Some DisplacementSystems
	 * have behavior based on current time, so it needs to be synced along with
	 * DisplacedAndTransferredRows.
	 * @return
	 */
	public abstract double getDisplacementSeconds() ;
	
	
	/**
	 * Sets the current number of 'displaced rows.'  Useful when transferring
	 * information from clients to server -- the clients actually adjust
	 * "current time," whereas the server does not.  Using this method
	 * and transferring that data lets the server "catch up" to the real time
	 * updates of the clients.
	 */
	public abstract void setDisplacementSecondsAndDisplacedAndTransferredRows( double ticks, double rows ) ;
	
}
