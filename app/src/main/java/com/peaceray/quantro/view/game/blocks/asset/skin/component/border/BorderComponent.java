package com.peaceray.quantro.view.game.blocks.asset.skin.component.border;

import com.peaceray.quantro.view.game.blocks.asset.skin.component.Component;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;


/**
 * BorderComponents are typically drawn after NeighborhoodComponents.  Their 
 * primary difference is in the method of encoding their blockfield metadata.
 * NeighborhoodComponents use a short-encoding of the local 3x3 neighborhood.
 * BorderComponents use explicit placement of top-left, top-right, bottom-left
 * and bottom-right corners of QOrientation blocks.
 * 
 * @author Jake
 *
 */
public abstract class BorderComponent extends DrawComponent {
	
	
	protected static final int DIRECTION_NONE = 0;
	protected static final int DIRECTION_RIGHT = 1;
	protected static final int DIRECTION_DOWN = 2;
	protected static final int NUM_SHINE_DIRECTIONS = 3;
	protected static final int DIRECTION_LEFT = 4;
	protected static final int DIRECTION_UP = 5;
	protected static final int NUM_CONNECTED_DIRECTIONS = 6;
	
	
	// Colors!
	// Corners are specified by direction, concavity, etc.
	// an INNER corner is a corner drawn within the boundaries
	// of the block it represents.
	// 		inner corner direction (e.g. LEFT_TOP) indicates
	//			the direction from the block center to reach
	//			the corner.
	//		concavity indictes whether the interior is concave
	//			or convex.  Convex corners point outward, towards
	//			other blocks than the one it is inside.
	//			Concave corners point inward, towards the block.
	// 			In other words, the top-left corner of block X is:
	//								[ ]
	//				[x]			 [ ][x]
	//
	//				convex		concave
	//			and in both cases it is drawn within the boundaries of x.
	protected static final byte CORNER_INNER_LEFT_TOP_CONVEX = 0 ;
	protected static final byte CORNER_INNER_RIGHT_TOP_CONVEX = 1 ;
	protected static final byte CORNER_INNER_LEFT_BOTTOM_CONVEX = 2 ;
	protected static final byte CORNER_INNER_RIGHT_BOTTOM_CONVEX = 3 ;
	protected static final byte CORNER_INNER_LEFT_TOP_CONCAVE = 4 ;
	protected static final byte CORNER_INNER_RIGHT_TOP_CONCAVE = 5 ;
	protected static final byte CORNER_INNER_LEFT_BOTTOM_CONCAVE = 6 ;
	protected static final byte CORNER_INNER_RIGHT_BOTTOM_CONCAVE = 7 ;
	protected static final int NUM_CORNERS = 8 ;
	
	
	
	/**
	 * Lists the possible combinations of Corners that appear in normal block operation.
	 */
	protected static final int[][] CORNER_COMBINATIONS = new int[][] {
			// top-left to top-right
			new int[] { CORNER_INNER_LEFT_TOP_CONVEX, CORNER_INNER_RIGHT_TOP_CONVEX },
			new int[] { CORNER_INNER_LEFT_TOP_CONVEX, CORNER_INNER_LEFT_TOP_CONCAVE },
			new int[] { CORNER_INNER_RIGHT_TOP_CONCAVE, CORNER_INNER_RIGHT_TOP_CONVEX },
			new int[] { CORNER_INNER_RIGHT_TOP_CONCAVE, CORNER_INNER_LEFT_TOP_CONCAVE },
			// top-left to bottom-left
			new int[] { CORNER_INNER_LEFT_TOP_CONVEX, CORNER_INNER_LEFT_BOTTOM_CONVEX },
			new int[] { CORNER_INNER_LEFT_TOP_CONVEX, CORNER_INNER_LEFT_TOP_CONCAVE },
			new int[] { CORNER_INNER_LEFT_BOTTOM_CONCAVE, CORNER_INNER_LEFT_BOTTOM_CONVEX },
			new int[] { CORNER_INNER_LEFT_BOTTOM_CONCAVE, CORNER_INNER_LEFT_TOP_CONCAVE },
			// bottom-left to bottom-right
			new int[] { CORNER_INNER_LEFT_BOTTOM_CONVEX, CORNER_INNER_RIGHT_BOTTOM_CONVEX },
			new int[] { CORNER_INNER_LEFT_BOTTOM_CONVEX, CORNER_INNER_LEFT_BOTTOM_CONCAVE },
			new int[] { CORNER_INNER_RIGHT_BOTTOM_CONCAVE, CORNER_INNER_RIGHT_BOTTOM_CONVEX },
			new int[] { CORNER_INNER_RIGHT_BOTTOM_CONCAVE, CORNER_INNER_LEFT_BOTTOM_CONCAVE },
			// top-right to bottom-right
			new int[] { CORNER_INNER_RIGHT_TOP_CONVEX, CORNER_INNER_RIGHT_BOTTOM_CONVEX },
			new int[] { CORNER_INNER_RIGHT_TOP_CONVEX, CORNER_INNER_RIGHT_TOP_CONCAVE },
			new int[] { CORNER_INNER_RIGHT_BOTTOM_CONCAVE, CORNER_INNER_RIGHT_BOTTOM_CONVEX },
			new int[] { CORNER_INNER_RIGHT_BOTTOM_CONCAVE, CORNER_INNER_RIGHT_TOP_CONCAVE }, };

	/**
	 * Defines the direction in which the corner combination points.  Indexes
	 * identically to CORNER_COMBINATIONS, and contains values DIRECTION_*.
	 */
	protected static final int [] CORNER_COMBINATION_DIRECTION = new int [] {
			// top-left to top-right
			DIRECTION_RIGHT,
			DIRECTION_RIGHT,
			DIRECTION_RIGHT,
			DIRECTION_RIGHT,
			// top-left to bottom-left
			DIRECTION_DOWN,
			DIRECTION_DOWN,
			DIRECTION_DOWN,
			DIRECTION_DOWN,
			// bottom-left to bottom-right
			DIRECTION_RIGHT,
			DIRECTION_RIGHT,
			DIRECTION_RIGHT,
			DIRECTION_RIGHT,
			// top-right to bottom-right
			DIRECTION_DOWN,
			DIRECTION_DOWN,
			DIRECTION_DOWN,
			DIRECTION_DOWN, };

	
	

	
	
	
	/**
	 * 
	 * @author Jake
	 *
	 */
	public static class BlockfieldMetadata extends DrawComponent.BlockfieldMetadata {
		
		private byte [][][][] mCorners ;
		
		protected BlockfieldMetadata( 
				int blockfieldRows, int blockfieldCols ) {
			
			super() ;
			
			mCorners = new byte[2][NUM_INDEX_CORNERS][blockfieldRows][blockfieldCols] ;
		}
		
		protected byte [][][][] getCorners() {
			return mCorners ;
		}
		
	}
	
	
}
