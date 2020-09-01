package com.peaceray.quantro.view.game;

/**
 * Players connecting to a game can have different types of "statuses."
 * For example, they can pause the game, disconnect, etc.
 * 
 * This status, by contrast, ignores transient connections for the most part.
 * Instead, it measures the state of the Player's "Game" object: are
 * they still participating?  Have they lost (or won)?  Have they 
 * quit?  Are they a spectator (for some other reason)?
 * 
 * @author Jake
 *
 */
public enum PlayerGameStatusDisplayed {
	
	/**
	 * This player is still playing the game (but may or may not
	 * be connected and playing at this very moment).
	 */
	ACTIVE,
	
	/**
	 * This player won the game.
	 */
	WON,
	
	
	/**
	 * This player lost the game.
	 */
	LOST,
	
	/**
	 * Player has been kicked.
	 */
	KICKED,
	
	/**
	 * This player quit the game before entering a win or lose state.
	 */
	QUIT,
	
	
	/**
	 * This palyer has gone 'inactive' by some other means than
	 * winning, losing or quitting.
	 */
	INACTIVE,
	
}
