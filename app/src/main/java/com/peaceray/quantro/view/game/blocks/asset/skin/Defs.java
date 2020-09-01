package com.peaceray.quantro.view.game.blocks.asset.skin;

public class Defs {
	
	
	/**
	 * The area of the block covered by this, uh... thing.
	 * @author Jake
	 *
	 */
	public enum BlockRegion{ 
		
		/**
		 * Don't fill.  With anything.
		 */
		NONE,
		
		
		/**
		 * The full block.  Fill edge to edge.
		 */
		FULL,
		
		
		/**
		 * Inset by a small amount.
		 */
		INSET_MINOR,
		
		
		/**
		 * Inset by a large amount
		 */
		INSET_MAJOR,
		
		
		/**
		 * Inset by a large amount, but don't include the
		 * fill region for the opposite qPane.
		 */
		INSET_MAJOR_NONOVERLAP
	}
	
	
}
