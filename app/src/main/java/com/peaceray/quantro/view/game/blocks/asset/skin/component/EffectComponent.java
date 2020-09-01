package com.peaceray.quantro.view.game.blocks.asset.skin.component;

import java.util.ArrayList;

import android.graphics.Canvas;

import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.IntStack;
import com.peaceray.quantro.utils.LooselyBoundedArray.LooselyBoundedArray;
import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigBlockGrid;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;
import com.peaceray.quantro.view.game.blocks.effects.EffectListCollection;

public abstract class EffectComponent extends Component {
	
	
	/**
	 * Performs a similar function to BlockfieldMetadata in DrawComponent.
	 * However, typically you will collect one instance of this class
	 * per "game area" (unique BlockDrawer instance), not one per
	 * "block field" (of which there may be several in one BlockDrawer instance).
	 * 
	 * This is because Effects are global to the game area: they are drawn over,
	 * behind, and around the other blockfields.
	 * 
	 * Each instance of EffectComponent will typically have a specialized function,
	 * creating its own Effects (and animating them).  Each instance therefore gets
	 * its own Metadatas (one per game area) where it will store the Effects in
	 * use.
	 * 
	 * Additional considerations: who plays sound effects?  To what extent can
	 * a skin "inform" the BlockDrawer of delays (e.g. "wait N milliseconds before
	 * transitioning from pre-clear blockfield to post-clear)?
	 * 
	 * @author Jake
	 *
	 */
	public class GameMetadata {
		
		private EffectListCollection mEffectListCollection ;
		
		private GameMetadata( int R, int C, long soundDuration ) {
			mEffectListCollection = new EffectListCollection( R, C, soundDuration ) ;
		}
		
		protected EffectListCollection getEffectListCollection() {
			return mEffectListCollection ;
		}
		
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INTERFACE: Creating Effects.
	//
	// Subclasses of EffectsComponent are required to implement these methods.
	// They represent the ways that "glow effects" and the like are generated.
	//
	// However, it's worth noting that subclasses are free to "specialize," by
	// returning 0 upon the call to any particular method.
	//
	
	public enum LockType {
		/**
		 * The locking blocks represent a piece component.
		 */
		PIECE,
		
		/**
		 * The locking blocks represent a blockfield chunk that is
		 * not necessarily a piece component.
		 */
		CHUNK,
	}
	
	/**
	 * Creates glow effects for the provided "lock" action, appending them to
	 * 'glowEffects' and returning the number created.
	 * 
	 * If 'glowEffects' is null, no Effect objects are created; instead, the number
	 * which would be created in a call will be returned.
	 * 
	 * @param lockType The lock type provided will potentially influence the
	 * 		behavior of this method; how many effects are created, their shape,
	 * 		etc.  For example, Standard Retro PIECE locks create a glow even
	 * 		when their size doesn't change, whereas Standard Retro CHUNK locks
	 * 		only glow if the piece changes size.
	 * @param mostRecentPieceQC The most recent piece qCombination.  Probably
	 * 		the piece causing this lock.
	 * @param chunkLocking The blockfield showing the chunk locking into place.
	 * @param blockfield The blockfield showing the after-lock field.
	 * @param qPane The qPane we should examine for effect(s).
	 * @param fieldRange The area within the field we should examine.
	 * 		For now, this will probably be equivalent to an 'extraRows'
	 *		integer combined with ds.blockFieldOuterBuffer.
	 * @param gameMetadata The existing Effects and other data.  New effects will
	 * 		be appended to their relevant lists. 
	 * @param millisInSliceLockOccurs We keep our own council about how
	 * 		long an effect should last (and how it should be drawn), but
	 * 		the BlockDrawer determines the moment the lock occurs and we
	 * 		create our Effects to start at this time.  It's likely that
	 * 		Effects will be "pregenerated" before this time has occurred.
	 * @return
	 */
	public abstract void createLockEffects(
			LockType lockType, int mostRecentPieceQC,
			byte[][][] chunkLocking, byte[][][] blockfield, int qPane, BlockDrawerConfigRange fieldRange,
			GameMetadata gameMetadata, long millisInSliceLockOccurs ) ;
	
	
	/**
	 * Creates glow effects for the provided "clear" action, appending them to
	 * 'glowEffects' and returning the number created.
	 * 
	 * If 'glowEffects' is null, no Effect objects are created; instead, the number
	 * which would be created in a call will be returned.
	 * 
	 * @param clears Indicates the type of clear activated in the specified row.
	 * @param monoClears Indicates whether a monochromatic clear occurs in the specified row.
	 * @param mostRecentPieceQC The most recent piece qCombination.  Probably
	 * 		the piece causing this lock.
	 * @param preClearBlockfield The blockfield before this clear occurred.
	 * @param postClearBlockfield The blockfield after this clear is processed.
	 * @param clearedBlocks The blocks which will be removed by this clear.
	 * @param qPane The qPane to examine.
	 * @param fieldRange The blocks within blockfields to scan.
	 * @param gameMetadata The existing Effects and other data.  New effects will
	 * 		be appended to their relevant lists. 
	 * @param millisInSliceClearOccurs We keep our own council about how
	 * 		long an effect should last (and how it should be drawn), but
	 * 		the BlockDrawer determines the moment the lock occurs and we
	 * 		create our Effects to start at this time.  It's likely that
	 * 		Effects will be "pregenerated" before this time has occurred.
	 * @return
	 */
	public abstract void createClearEffects(
			int[] clears, boolean[] monoClears,  int mostRecentPieceQC,
			byte [][][] preClearBlockfield, byte [][][] postClearBlockfield, byte [][][] clearedBlocks,
			int qPane, BlockDrawerConfigRange fieldRange,
			GameMetadata gameMetadata, long millisInSliceClearOccurs ) ;
	
	
	
	/**
	 * Creates effects related to rising block fields.
	 * 
	 * @param rowsRisen
	 * @param mostRecentPieceQC
	 * @param blockfield
	 * @param qPane
	 * @param fieldRange
	 * @param gameMetadata
	 * @param millisInSliceRiseOccurs
	 */
	public abstract void createRisingEffects(
			int rowsRisen, int mostRecentPieceQC,
			byte [][][] blockfield, int qPane, BlockDrawerConfigRange fieldRange,
			GameMetadata gameMetadata, long millisInSliceRiseOccurs ) ;
	
	
	
	public abstract void createUnlockColumnEffects(
			ArrayList<Offset> unlockedColumns, int numUnlockedColumns, int mostRecentPieceQC,
			byte[][][][] blockfields, byte[][][][] fallingChunks, int[] fallDistances, int qPane, BlockDrawerConfigRange fieldRange,
			int firstChunk, int numChunks,
			GameMetadata gameMetadata, long millisInSliceUnlockStarts  ) ;
	
	
	public abstract void createMetamorphosisEffects(
			int mostRecentPieceQC,
			byte[][][] pre_blockfield, byte[][][] post_blockfield, int qPane, BlockDrawerConfigRange fieldRange,
			GameMetadata gameMetadata, long millisInSliceMetamorphosisStarts ) ;
	
	
	
	//
	// INTERFACE: Creating Effects.
	//
	////////////////////////////////////////////////////////////////////////////

	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INTERFACE: Drawing Effects.
	//
	// Subclasses of EffectsComponent are required to implement these methods.
	// They represent the ways that "glow effects" and the like are animated.
	//
	// However, it's worth noting that subclasses are free to "specialize,"
	// by doing absolutely nothing for certain calls.
	//
	
	
	/**
	 * Draws its content (that in gameMetadata) behind the specified qPane,
	 * which may be QPANE_0, QPANE_1, or QPANE_3D.
	 * 
	 * Effects drawn "behind" are drawn before the relevant components.
	 * 
	 * If the metadata has no effects to draw, go ahead and return immediately.
	 */
	public abstract void drawBehind(
			Canvas canvas, BlockDrawerConfigBlockGrid configBlockGrid,
			BlockDrawerSliceTime sliceTime,
			GameMetadata gameMetadata, int qPane ) ;
	
	
	/**
	 * Draws its content (that in gameMetadata) in front the specified qPane,
	 * which may be QPANE_0, QPANE_1, or QPANE_3D.
	 * 
	 * Effects drawn "in front" are drawn after the relevant components.
	 * 
	 * If the metadata has no effects to draw, go ahead and return immediately.
	 */
	public abstract void drawInFront(
			Canvas canvas, BlockDrawerConfigBlockGrid configBlockGrid,
			BlockDrawerSliceTime sliceTime,
			GameMetadata gameMetadata, int qPane ) ;
	
	
	//
	// INTERFACE: Creating Effects.
	//
	////////////////////////////////////////////////////////////////////////////
	

	////////////////////////////////////////////////////////////////////////////
	//
	// FLOODING
	//
	// Flooding is extremely important for determining glows.  Different glow
	// styles have different flood parameters and different processing of those 
	// results, but flooding is central to all current glow styles.
	
	// Flood parameters.
	protected enum FloodNeighborhood {
		/**
		 * No neighborhood.  No flooding, apparently.
		 */
		NONE,
		
		/**
		 * Flood left, right, up down.  Not diagonally.  1 step at a time.
		 */
		FOUR,
	}

	protected static final int[] NEIGHBORHOOD_4_ROW_OFFSET = new int[] { -1, 1,
			0, 0 };
	protected static final int[] NEIGHBORHOOD_4_COL_OFFSET = new int[] { 0, 0,
			-1, 1 };

	
	protected enum FloodStarting {
		/**
		 * Start floods in exactly the provided blocks.
		 */
		EXACT,
		
		/**
		 * Start floods in exactly the provided blocks and
		 * their 4-neighborhood.
		 */
		EXACT_AND_NEIGHBORHOOD_FOUR,
		
		/**
		 * Start floods in exactly the provided blocks and
		 * their 8-neighborhood (l,r,t,b and diagonals).
		 */
		EXACT_AND_NEIGHBORHOOD_EIGHT,
	}

	
	protected enum FloodTo {
		/**
		 * Flood only to the same QOrientation.
		 */
		SAME,
		
		/**
		 * Flood according to the provided 'connected' array.
		 */
		CONNECTED,
		
		/**
		 * Flood if 'connected' in the provided array, or if there
		 * is no starting qOrientation, or no change from the starting qOrientation.
		 */
		CONNECTED_OR_NO_STARTING_OR_NO_CHANGE,
	}
	
	
	protected enum FloodCompareWithin {
		/**
		 * When flooding, we compare within the provided field.
		 */
		FIELD,
		
		/**
		 * When flooding, we compare within the provided field and the starting field.
		 * To flood, we require a flood-condidate in the field, *AND* either a flood
		 * candidate in the starting field or two zeroes in the starting field.
		 */
		FIELD_AND_STARTING
	}
	
	
	protected enum FloodLimit {
		/**
		 * Keep flooding as long as we can.
		 */
		NONE,
		
		/**
		 * Limit our flood to the starting blocks and their local 4-neighborhoods.
		 */
		STARTING_BLOCKS_AND_NEIGHBORHOOD_FOUR,
	}
	
	
	protected enum FloodIncludeRegion {
		/**
		 * Include all flooded regions.
		 */
		ALL,
		
		/**
		 * Include only those flooded regions which have grown from their starting size.
		 */
		GROWN,
		
		/**
		 * Include all flooded regions.  Number them after attempting to merge
		 * available pairs of non-neighboring regions.  Minimizes the number
		 * of distinct regions.
		 */
		ALL_MERGE_NOT_NEIGHBORS,
		
		/**
		 * Include all regions.  Starting regions receive their own number.
		 * All non-starting regions are grouped into the same region with a single
		 * number.
		 */
		ALL_STARTING_AND_OTHER,
	}
	
	

	private byte[][][][] floodRegions_shrinkMarginsFields = new byte[2][][][];
	private boolean[][] floodRegions_tempFlood ;
	private boolean[][] floodRegions_tempFlood2 ;
	private LooselyBoundedArray floodRegions_tempFloodLBA ;
	
	private BlockDrawerConfigRange floodRegions_tempRange = null ;
	
	private IntStack floodRegions_tempStackR = new IntStack() ;
	private IntStack floodRegions_tempStackC = new IntStack() ;
	

	private boolean[] floodRegions_greedilyMergeIntoDisjointRegion_regionOK = new boolean[16];
	
	/**
	 * Performs regions floods within floodWithin, starting in startingBlocks.
	 * Regions are numbered started with firstRegionNum, and marked within
	 * 'markWithin.'
	 * 
	 * As of 1/17/12, the initial implementation, Regions are flooded very
	 * simply; basically, we directly examine QOrientions for equality.
	 * 
	 * Starting 1/18/12, we are starting fancy glow effects for Quantro mode,
	 * which need a slightly more complicated flood behavior. It works as
	 * follows:
	 * 
	 * - we allow flood according to the FloodNeighborhood setting given: no
	 * flood, flood in 4 directions, flood in 8 directions.
	 * 
	 * - we start floods in locations determined by FLOOD_STARTING_*: exactly
	 * those blocks present in startingBlocks, those extended into a
	 * neighborhood of 4 or 8, etc.
	 * 
	 * - we flood from A-to-B according to FLOOD_TO_*: an exact match of the
	 * same QOrientation, or any blocks which do not have a border between them
	 * (e.g. drawn with 'friendly' borders). NO_BORDER_OR_NO_STARTING allows a
	 * flood between blocks with a border, if neither block occurs in
	 * 'starting.'
	 * 
	 * - we flood starting based on our criteria to all blocks to all blocks in
	 * its neighborhood which match FLOOD_TO_, a condition we check according to
	 * FLOOD_COMPARE. For *_WITHIN_FIELD, we flood from A-to-B if it matches the
	 * FLOOD_TO_ considering those blocks shown in floodWithin. For
	 * *_WITHIN_FIELD_AND_STARTING, we flood from A-to-B if it matches in
	 * floodWithin, *and* either matches in startingBlocks or both are NO in
	 * startingBlocks.
	 * 
	 * - we flood away from starting blocks only up to the specified LIMIT.
	 * LIMIT_NONE does not limit by distance. LIMIT_STARTING_AND_NEIGHBORHOOD_
	 * will only allow floods into starting blocks or those within the
	 * neighborhood of them.
	 * 
	 * - we include flooded regions according to INCLUDE_REGION. ALL will
	 * include every region flooded according to the above. GROWN will include
	 * only those regions which have grown beyond the size present in
	 * startingBlocks. ALL_MERGE_NON_NEIGHBORS will include all regions, but
	 * will greedily merge together regions which are NOT neighbors according to
	 * our flood neighborhood: if set, the regions returned may not necessarily
	 * be contiguous, but any contiguous portion will obey all flood settings (a
	 * flood within them is always possible).
	 * 
	 * Given the settings specified above, the original flood behavior can be
	 * replicated using:
	 * 
	 * FloodNeighborhood.FOUR
	 * FLOOD_STARTING_EXACT_BLOCKS FLOOD_TO_SAME
	 * FLOOD_COMPARE_WITHIN_FIELD FLOOD_LIMIT_NONE includeNoGrow ?
	 * FLOOD_INCLUDE_REGION_ALL : FLOOD_INCLUDE_REGION_GROWN
	 * 
	 * 'includeNoGrow' indicates whether a region should be included if it does
	 * not grow beyond the blocks included in 'startingBlocks.'
	 * 
	 * Returns the number of regions added, which is >= 0.
	 * 
	 * 
	 * 6/15: this method is a very hungry one in our stress tests. First step to
	 * optimization is to break it up into component methods so they can be
	 * individually profiled.
	 * 
	 * @param startingBlocks
	 * @param startRowOffset
	 *            : when indexing into startingBlocks, we should add this value
	 *            to get the corresponding index into floodWithin and
	 *            markWithin.
	 * @param startColOffset
	 *            : when indexing into startingBlocks, we should add this value
	 *            to get the corresponding index into floodWithin and
	 *            markWithin.
	 * @param floodWithin
	 * @param qPane
	 * @param markWithin
	 * @param firstRegionNum
	 * @param includeNoGrow
	 * @return
	 */
	protected int floodRegions(boolean [][] qoConnects, EncodeBehavior [] qoEncodeBehavior,
			byte[][][] startingBlocks, BlockDrawerConfigRange startingRange,
			Offset startOffset, byte[][][] floodWithin, BlockDrawerConfigRange floodWithinRange,
			int qPane, int[][] markWithin,
			int firstRegionNum,
			FloodNeighborhood floodNeighborhood,
			FloodStarting floodStarting,
			FloodTo floodTo,
			FloodCompareWithin floodCompareWithin,
			FloodLimit floodLimit,
			FloodIncludeRegion floodIncludeRegion) {

		// check settings!
		if (floodNeighborhood == null)
			throw new NullPointerException("Must provide FloodNeighborhood.");
		if (floodStarting == null)
			throw new NullPointerException("Must provide floodStarting.");
		if (floodTo == null)
			throw new NullPointerException("Must provide floodTo.");
		if (floodCompareWithin == null)
			throw new NullPointerException("Must provide floodCompareWithin.");
		if (floodLimit == null)
			throw new NullPointerException("Must provide floodLimit.");
		if (floodIncludeRegion == null)
			throw new NullPointerException("Must provide floodIncludeRegion.");
		if (floodWithinRange == null)
			throw new NullPointerException("Must provide floodWithinRange.") ;

		// We use floodRegions_tempFlood2 to indicate that a piece has been flooded already.
		// This is necessary because not every flood is added to markWithin,
		// only
		// those that meet our glow settings.
		ArrayOps.setEmpty(floodRegions_tempFlood2);
		ArrayOps.setEmpty(floodRegions_tempFlood);
		floodRegions_tempFloodLBA.boundNone();
		// We use an LBA to avoid constantly reseting false values to false.

		// 6/15/12: After a few optimizations, accounts for 10.9% of Quantro
		// CPU. Can we improve this any more?
		// 1st: use floodRegions_tempFloodLBA to avoid clearing the array every time. Reduced
		// to 7.5%.
		// 2nd: the option FLOOD_INCLUDE_REGION_ALL_MERGE_NOT_NEIGHBORS causes a
		// merge of
		// non-adjacent regions to minimize the number of regions found.
		// However,
		// a quick examination revealed that this option serves no real purpose
		// (it is used in Quantro only, and the regions returned are then
		// compared
		// using != -> there is no iteration through region numbers, which would
		// have
		// benefitted from the change). We are keeping this method the same and
		// calling it with a different option (FLOOD_INCLUDE_REGION_ALL).
		// Before: this 8.7%, greedilyMerge 3.7%, setLockGlows 12.3%,
		// drawGlowSetArray 9.3%.
		// After: this 6.6%, greedilyMerge 0%, setLockGlows 9.6%,
		// drawGlowSetArray 9.7%.
		// 3rd: short-circuit through an immediate floodRegions_tempFlood2 check. If true,
		// skip EVERYTHING for that row/col combo.
		// Before: 6.6%, of which 55.7% is self
		// After: 8.1!!!! what? Redo-ing... 8.1% again. wtf?
		// 4th: restrict the iteration bounds to avoid the
		// "bounds check and continue" that occurred
		// every loop iteration.
		// Before: 8.1
		// After: 5.6%! Of this, 52.7% is still 'this' method.
		// 5th: further restrict iteration bounds to only the non-empty qo's in
		// floodWithin.
		// After: 6.8%. Hrmm. Of course, getting 8.0 after removing this, so...

		int regionNum = firstRegionNum;

		int rowMin, rowMax, colMin, colMax;
		rowMin = floodWithinRange.rowFirst ;
		colMin = floodWithinRange.columnFirst ;
		rowMax = floodWithinRange.rowBound ;
		colMax = floodWithinRange.columnBound ;

		int startOffsetX, startOffsetY;
		if (startOffset == null)
			startOffsetX = startOffsetY = 0;
		else {
			startOffsetX = startOffset.x;
			startOffsetY = startOffset.y;
		}

		int rowMinIter = rowMin, rowMaxIter = rowMax, colMinIter = colMin, colMaxIter = colMax;
		// there are also starting bounds to consider. We use the max/min
		// of current settings and bounding area.
		// If any of the following are true, we do NOT want to consider the
		// row/col (copied from
		// a loop-internal check). Convert these to row/col min/maxes.
		// startingBlocksLL != null && ( startRow < startingBlocksLL.y - margin
		// || startCol < startingBlocksLL.x - margin )
		// startingBlocksUR != null && ( startRow >= startingBlocksUR.y + margin
		// || startCol >= startingBlocksUR.x + margin )
		// startRow < 0 || startRow >= startingBlocks[qPane].length
		// startCol < 0 || startCol >= startingBlocks[qPane][0].length
		// first: make sure row/col stay within startingBounds.

		rowMinIter = Math.max(rowMinIter, startOffsetY); // we subtract this to
															// get row in
															// startingBlocks
		colMinIter = Math.max(colMinIter, startOffsetX); // we subtract this to
															// get col in
															// startingBlocks
		rowMaxIter = Math.min(rowMaxIter, startOffsetY
				+ startingBlocks[qPane].length);
		colMaxIter = Math.min(colMaxIter, startOffsetX
				+ startingBlocks[qPane][0].length);
		// that covers the hard-boundaries. What about the provided ones? Some
		// have
		// an additional margin...
		int margin = floodStarting == FloodStarting.EXACT ? 0 : 1;
		if (startingRange != null) {
			rowMinIter = Math.max(rowMinIter, startOffsetY + startingRange.rowFirst - margin);
			colMinIter = Math.max(colMinIter, startOffsetX + startingRange.columnFirst - margin);
			rowMaxIter = Math.min(rowMaxIter, startOffsetY + startingRange.rowBound - margin);
			colMaxIter = Math.min(colMaxIter, startOffsetX + startingRange.columnBound - margin);
		}
		
		if ( floodRegions_tempRange == null )
			floodRegions_tempRange = new BlockDrawerConfigRange( 0, 0 ) ;
		floodRegions_tempRange.set(floodWithinRange,
				rowMinIter, rowMaxIter,
				colMinIter, colMaxIter ) ;

		floodRegions_shrinkMarginsFields[0] = startingBlocks;
		floodRegions_shrinkMarginsFields[1] = floodWithin;
		if ( !floodRegions_tempRange.shrinkToFit(floodRegions_shrinkMarginsFields) )
			return 0 ;	// no new regions added

		rowMinIter = floodRegions_tempRange.rowFirst ;
		rowMaxIter = floodRegions_tempRange.rowBound ;
		colMinIter = floodRegions_tempRange.columnFirst ;
		colMaxIter = floodRegions_tempRange.columnBound ;

		// we use these for "ONLY_TWO_REGIONS" include settings.
		boolean hasWithinStarting = false;
		boolean hasWithoutStarting = false;

		for (int row = rowMinIter; row < rowMaxIter; row++) {
			int r = floodWithinRange.rowToIndex(row) ;
			int startR = r - startOffsetY;
			// int startRow = startR - ds.blockFieldOuterBuffer ;

			for (int col = colMinIter; col < colMaxIter; col++) {

				// SHORT CIRCUIT!
				if (floodRegions_tempFlood2[row][col])
					continue;

				int c = floodWithinRange.colToIndex(col) ;
				int startC = c - startOffsetX;
				// int startCol = startC - ds.blockFieldOuterBuffer ;

				/*
				 * // check that this block is within our limits. int margin =
				 * floodStarting == FLOOD_STARTING_EXACT_BLOCKS ? 0 : 1 ;
				 * 
				 * if ( startingBlocksLL != null && ( startRow <
				 * startingBlocksLL.y - margin || startCol < startingBlocksLL.x
				 * - margin ) ) continue ; else if ( startingBlocksUR != null &&
				 * ( startRow >= startingBlocksUR.y + margin || startCol >=
				 * startingBlocksUR.x + margin ) ) continue ; else if ( startRow
				 * < 0 || startRow >= startingBlocks[qPane].length ) continue ;
				 * else if ( startCol < 0 || startCol >=
				 * startingBlocks[qPane][0].length ) continue ;
				 */

				boolean startOK = flood_shouldStartHere(floodStarting,
						startingBlocks, qPane, startR, startC);

				if (startOK && markWithin[row][col] < firstRegionNum
						&& !floodRegions_tempFlood2[row][col]) {
					// clear our loosely bounded floodRegions_tempFlood
					floodRegions_tempFloodLBA.clear().boundNone();
					// start marking here.
					floodRegions_tempStackR.empty();
					floodRegions_tempStackC.empty();

					floodRegions_tempStackR.push(row); // changed from r->row for
											// optimization
					floodRegions_tempStackC.push(col); // changed from c->col for
											// optimization.

					boolean startedWithinStartingBlocks = startingBlocks[qPane][startR][startC] != QOrientations.NO;

					while (floodRegions_tempStackR.count() > 0) {
						int rowHere = floodRegions_tempStackR.pop();
						int colHere = floodRegions_tempStackC.pop();

						int qHere = floodWithin[qPane]
						                        [floodWithinRange.rowToIndex(rowHere)]
						                         [floodWithinRange.colToIndex(colHere)];

						if (qHere == 0 || qoEncodeBehavior[qHere] == EncodeBehavior.SKIP)
							continue;

						floodRegions_tempFlood2[rowHere][colHere] = floodRegions_tempFlood[rowHere][colHere] = true;
						// extend floodRegions_tempFlood's bounds...
						floodRegions_tempFloodLBA.bound(0, rowHere).bound(1, colHere);

						// 6/15/2012: moved to a method for profiling
						flood_floodToNeighbors(qoConnects, floodNeighborhood, floodTo,
								floodCompareWithin, floodLimit,
								startingBlocks, startingRange, startOffsetX, startOffsetY,
								floodWithin, floodWithinRange, qPane, rowHere, colHere,
								rowMin, rowMax, colMin, colMax,
								floodRegions_tempStackR, floodRegions_tempStackC, floodRegions_tempFlood2);

					}

					// fill with regionNum, but ONLY if we extended beyond the
					// boundaries of startingBlocks or includeNoGrow is true.
					// TODO: This results in a lot of wasted effort for falling
					// chunks.
					boolean include = flood_shouldIncludeRegion(
							floodIncludeRegion,
							startingBlocks, startingRange, startOffsetX, startOffsetY,
							floodWithin, qPane, rowMin, rowMax, colMin, colMax);

					if (include) {
						if (floodIncludeRegion == FloodIncludeRegion.ALL_STARTING_AND_OTHER) {
							// we need a number for this region. If not
							// startedWithinStarting,
							// use firstRegionNum. Otherwise, take if
							// !hasWithinStarting,
							// set regionNum += 1 and take regionNum. Otherwise,
							// take regionNum.
							int num;
							if (startedWithinStartingBlocks) {
								num = hasWithinStarting ? regionNum
										: regionNum + 1;
								hasWithinStarting = true;
							} else {
								num = firstRegionNum;
								hasWithoutStarting = true;
							}

							for (int i = rowMin; i < rowMax; i++) {
								for (int j = colMin; j < colMax; j++) {
									if (floodRegions_tempFlood[i][j])
										markWithin[i][j] = num;
								}
							}
							if (startedWithinStartingBlocks)
								regionNum = num + 1;

						} else if (floodIncludeRegion == FloodIncludeRegion.ALL_MERGE_NOT_NEIGHBORS) {
							// 6/15: encapsulated in a method for profiling +
							// optimization.
							if (!floodRegions_greedilyMergeIntoDisjointRegion(
									floodRegions_tempFlood, markWithin, firstRegionNum,
									regionNum, rowMin, rowMax, colMin, colMax))
								regionNum++;
						} else {
							for (int i = rowMin; i < rowMax; i++) {
								for (int j = colMin; j < colMax; j++) {
									if (floodRegions_tempFlood[i][j])
										markWithin[i][j] = regionNum;
								}
							}
							regionNum++;

						}

					}
				}

			}
		}

		if (floodIncludeRegion == FloodIncludeRegion.ALL_STARTING_AND_OTHER
				&& regionNum > firstRegionNum) {
			if (!hasWithinStarting)
				return 0; // if nothing to start, include nothing at all.
		}

		return regionNum - firstRegionNum;

	}

	/**
	 * Investigates the local region around qPane,r,c; returns whether we should
	 * start a flood from that location according to our floodStarting behavior.
	 * 
	 * @param floodStarting
	 * @param startingBlocks
	 * @param qPane
	 * @param r
	 * @param c
	 * @return
	 */
	private boolean flood_shouldStartHere(
			FloodStarting floodStarting,
			byte[][][] startingBlocks, int qPane, int r, int c) {
		boolean startOK = false;
		switch (floodStarting) {
		case EXACT_AND_NEIGHBORHOOD_EIGHT:
			// check diagonal 4
			startOK = startOK
					|| (r + 1 < startingBlocks[qPane].length
							&& c + 1 < startingBlocks[qPane][r].length && startingBlocks[qPane][r + 1][c + 1] != 0);
			startOK = startOK
					|| (r - 1 >= 0 && c + 1 < startingBlocks[qPane][r].length && startingBlocks[qPane][r - 1][c + 1] != 0);
			startOK = startOK
					|| (r + 1 < startingBlocks[qPane].length && c - 1 >= 0 && startingBlocks[qPane][r + 1][c - 1] != 0);
			startOK = startOK
					|| (r - 1 >= 0 && c - 1 >= 0 && startingBlocks[qPane][r - 1][c - 1] != 0);
		case EXACT_AND_NEIGHBORHOOD_FOUR:
			// check neighborhood 4. neighborhood-8 falls through to here;
			// it also needs to check its neighborhood.
			startOK = startOK
					|| (r + 1 < startingBlocks[qPane].length && startingBlocks[qPane][r + 1][c] != 0);
			startOK = startOK
					|| (r - 1 >= 0 && startingBlocks[qPane][r - 1][c] != 0);
			startOK = startOK
					|| (c + 1 < startingBlocks[qPane][r].length && startingBlocks[qPane][r][c + 1] != 0);
			startOK = startOK
					|| (c - 1 >= 0 && startingBlocks[qPane][r][c - 1] != 0);
		case EXACT:
			startOK = startOK || startingBlocks[qPane][r][c] != 0;
		}

		return startOK;
	}

	private boolean flood_shouldIncludeRegion(
			FloodIncludeRegion floodIncludeRegion,
			byte[][][] startingBlocks, BlockDrawerConfigRange startingRange,
			int startOffsetX, int startOffsetY, byte[][][] floodWithin,
			int qPane, int rowMin, int rowMax, int colMin, int colMax) {
		boolean include = floodIncludeRegion == FloodIncludeRegion.ALL
				|| floodIncludeRegion == FloodIncludeRegion.ALL_MERGE_NOT_NEIGHBORS
				|| floodIncludeRegion == FloodIncludeRegion.ALL_STARTING_AND_OTHER ;
		for (int row = rowMin; row < rowMax && !include; row++) {
			int rHere = startingRange.rowToIndex(row) ;
			for (int col = colMin; col < colMax && !include; col++) {
				int cHere = startingRange.colToIndex(col) ;
				include = include
						|| (floodRegions_tempFlood[row][col] && startingBlocks[qPane][rHere
								- startOffsetY][cHere - startOffsetX] != floodWithin[qPane][rHere][cHere]);
			}
		}

		return include;
	}

	// we use 'row' to refer to the actual row/column number (and index into
	// floodRegions_tempFlood2, e.g.).
	// 'rowPos' is the blockFieldOuterBuffer-adjusted position within the
	// fields.
	private void flood_floodToNeighbors(boolean [][] qoConnects,
			FloodNeighborhood floodNeighborhood,
			FloodTo floodTo,
			FloodCompareWithin floodCompareWithin,
			FloodLimit floodLimit,
			byte[][][] startingBlocks, BlockDrawerConfigRange startingBlocksRange, int startOffsetX, int startOffsetY,
			byte[][][] floodWithin, BlockDrawerConfigRange floodWithinRange, int qPane, int row, int col, int rowMin,
			int rowMax, int colMin, int colMax, IntStack rStack,
			IntStack cStack, boolean[][] rowColMarkedAlready) {

		// get some important values...
		int rowPos = floodWithinRange.rowToIndex(row) ;
		int colPos = floodWithinRange.colToIndex(col) ;

		int qo = floodWithin[qPane][rowPos][colPos];

		int rowPos_start = rowPos - startOffsetY;
		int colPos_start = colPos - startOffsetX;

		int qo_start = startingBlocks[qPane][rowPos_start][colPos_start];

		if (floodNeighborhood != FloodNeighborhood.NONE) {
			// At least 4-way connections, maybe more.
			for (int i = 0; i < NEIGHBORHOOD_4_ROW_OFFSET.length; i++) {
				int rOff = NEIGHBORHOOD_4_ROW_OFFSET[i];
				int cOff = NEIGHBORHOOD_4_COL_OFFSET[i];

				// Check min/max before any other access attempts...
				if (row + rOff < rowMin || row + rOff >= rowMax)
					continue;
				if (col + cOff < colMin || col + cOff >= colMax)
					continue;

				// Attempt expansion in this direction.
				int rowPosThere = rowPos + rOff;
				int colPosThere = colPos + cOff;
				int qoThere = floodWithin[qPane][rowPosThere][colPosThere];
				int rowThere = row + rOff;
				int colThere = col + cOff;

				int rowPosThere_start = rowPosThere - startOffsetY;
				int colPosThere_start = colPosThere - startOffsetY;

				int qoThere_start = (rowPosThere_start >= 0
						&& rowPosThere_start < startingBlocks[qPane].length
						&& colPosThere_start >= 0 && colPosThere_start < startingBlocks[qPane][rowPosThere_start].length) ? startingBlocks[qPane][rowPosThere_start][colPosThere_start]
						: 0;

				// Already marked?
				if (rowColMarkedAlready[rowThere][colThere])
					continue;
				// a NO?
				if (qoThere == QOrientations.NO)
					continue;
				// Check flood limit.
				if (floodLimit == FloodLimit.STARTING_BLOCKS_AND_NEIGHBORHOOD_FOUR
						&& !(qoThere_start != QOrientations.NO
								|| startingBlocks[qPane][rowPosThere_start - 1][colPosThere_start] != QOrientations.NO
								|| startingBlocks[qPane][rowPosThere_start + 1][colPosThere_start] != QOrientations.NO
								|| startingBlocks[qPane][rowPosThere_start][colPosThere_start - 1] != QOrientations.NO || startingBlocks[qPane][rowPosThere_start][colPosThere_start + 1] != QOrientations.NO))
					continue;
				// Check FLOOD_TO behavior.
				if (!floodsTo(qoConnects, qo, qoThere, floodTo, qo_start,
						qoThere_start, qPane))
					continue;
				// Check FLOOD_TO within other fields
				if (floodCompareWithin == FloodCompareWithin.FIELD_AND_STARTING) {
					if (!(qo_start == 0 && qoThere_start == 0)
							&& !floodsTo(qoConnects, qo_start, qoThere_start, floodTo,
									qo_start, qoThere_start, qPane))
						continue;
				}

				// If we get here, these conditions have been checked:
				// FLOOD_TO,
				// FLOOD_COMPARE_WITHIN,
				// FLOOD_LIMIT
				floodRegions_tempStackR.push(rowThere);
				floodRegions_tempStackC.push(colThere);
			}
		}
	}

	/**
	 * Attempts to merge the provided region (newRegion) into a disjoint region
	 * within 'regions', indicated by its number.
	 * 
	 * If a disjoint region is found, every 'true' in newRegion has its
	 * corresponding location set to that region number within 'regions', and
	 * 'true' is returned.
	 * 
	 * If there is no disjoint region available, we instead add a new region to
	 * 'regions' using 'regionNum' as its number, and return false.
	 * 
	 * @param newRegion
	 * @param regions
	 * @param firstRegionNum
	 * @param regionNum
	 * @param rowMin
	 * @param rowMax
	 * @param colMin
	 * @param colMax
	 * @return
	 */
	private boolean floodRegions_greedilyMergeIntoDisjointRegion(
			boolean[][] newRegion, int[][] regions, int firstRegionNum,
			int regionNum, int rowMin, int rowMax, int colMin, int colMax) {

		// is our array long enough?
		while (floodRegions_greedilyMergeIntoDisjointRegion_regionOK.length < regionNum
				- firstRegionNum)
			floodRegions_greedilyMergeIntoDisjointRegion_regionOK = new boolean[floodRegions_greedilyMergeIntoDisjointRegion_regionOK.length * 2];
		for (int i = 0; i < floodRegions_greedilyMergeIntoDisjointRegion_regionOK.length; i++)
			floodRegions_greedilyMergeIntoDisjointRegion_regionOK[i] = true;

		// we index into this such that floodRegions_gre..._regionOK[0]
		// indicates firstRegionNum is OK.

		// now eliminate those regions which neighbor 'newRegion.' To avoid
		// a lot of redundant checks, we:
		// 1. Iterate through newRegion ONCE. For each 'true,' check all four
		// directions (this could possible by optimized further, but it's
		// a big enough improvement for now) and mark all those regions found
		// as ineligible.

		// ASSUMPTIONS: Regions don't actually overlap.
		// A value in regions[][] >= firstRegionNum is EQUIVALENT to a region
		// (i.e., the "no region" number is < firstRegionNum, and no values
		// >= regionNum exist).

		for (int i = rowMin; i < rowMax; i++) {
			for (int j = colMin; j < colMax; j++) {
				if (newRegion[i][j]) {
					// check here!
					if (i > rowMin && regions[i - 1][j] >= firstRegionNum)
						floodRegions_greedilyMergeIntoDisjointRegion_regionOK[regions[i - 1][j]
								- firstRegionNum] = false;
					if (i < rowMax - 1 && regions[i + 1][j] >= firstRegionNum)
						floodRegions_greedilyMergeIntoDisjointRegion_regionOK[regions[i + 1][j]
								- firstRegionNum] = false;
					if (j > colMin && regions[i][j - 1] >= firstRegionNum)
						floodRegions_greedilyMergeIntoDisjointRegion_regionOK[regions[i][j - 1]
								- firstRegionNum] = false;
					if (j < colMax - 1 && regions[i][j + 1] >= firstRegionNum)
						floodRegions_greedilyMergeIntoDisjointRegion_regionOK[regions[i][j + 1]
								- firstRegionNum] = false;
				}
			}
		}

		// is there an OK region left?
		int newRegionNum = regionNum;
		for (int i = 0; i < regionNum - firstRegionNum; i++) {
			if (floodRegions_greedilyMergeIntoDisjointRegion_regionOK[i]) {
				newRegionNum = i + firstRegionNum;
				break;
			}
		}

		// place!
		for (int i = rowMin; i < rowMax; i++)
			for (int j = colMin; j < colMax; j++)
				if (newRegion[i][j])
					regions[i][j] = newRegionNum;

		// Was this a merged region?
		return newRegionNum < regionNum;

		// THE BELOW IS UNOPTIMIZED CODE.
	}

	private boolean floodsTo(boolean [][] qoConnects,
			int qo1, int qo2,
			FloodTo floodTo,
			int startingQ1, int startingQ2,
			int qPane) {
		if (qo2 == 0)
			return false;
		if (floodTo == FloodTo.SAME && qo1 != qo2)
			return false;
		if ( floodTo == FloodTo.CONNECTED )
			return qoConnects[qo1][qo2] ;
		if ( floodTo == FloodTo.CONNECTED_OR_NO_STARTING_OR_NO_CHANGE && (startingQ1 != 0 || startingQ2 != 0) ) {
			// No change?
			if (qo1 == startingQ1 && qo2 == startingQ2)
				return true;
			return qoConnects[qo1][qo2] ;
		}

		return true;
	}
	
}
