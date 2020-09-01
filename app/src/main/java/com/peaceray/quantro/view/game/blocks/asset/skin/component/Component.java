package com.peaceray.quantro.view.game.blocks.asset.skin.component;

import java.io.IOException;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Bitmap;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.view.game.DrawSettings;
import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.FillRegions;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;

public abstract class Component {
	
	protected enum EncodeBehavior {
		/**
		 * We don't do anything to change the encoded value.  Obviously
		 * we clear it to ENCODED_OPEN, but no change is made after that.  Presumably
		 * you know what to do?  Standard is to do this with QOrientations.NO.
		 */
		SKIP,
		
		/**
		 * Just treats it using our standard behavior.
		 */
		STANDARD,
	}
	
	
	
	/**
	 * BorderStyle is generally reserved for 'nonoverlap' region styles.
	 * 
	 * @author Jake
	 *
	 */
	public enum BorderStyle {
		/**
		 * Draw the border all the way around, omitting the portions where
		 * it vertically intersects with the other qPane (going "under"
		 * vertically, "over" horizontally).
		 */
		OVER_HORIZONTAL,
		
		/**
		 * Draw the border all the way around, omitting the portions where
		 * it vertically intersects with the other qPane (going "under"
		 * horizontally, "over" vertically).
		 */
		OVER_VERTICAL,
	}
	

	protected static final int INDEX_CORNER_TL = 0;
	protected static final int INDEX_CORNER_TR = 1;
	protected static final int INDEX_CORNER_BL = 2;
	protected static final int INDEX_CORNER_BR = 3;
	protected static final int NUM_INDEX_CORNERS = 4;
	
	
	private BlockDrawerConfigRange mTempConfigRange = new BlockDrawerConfigRange(0, 0) ;
	private boolean [][] mTempConnected = new boolean[3][3] ;
	
	
	/**
	 * Encodes local neighborhood 8 connections for all the blocks
	 * within configRange.
	 * 
	 * Connections are encoded in a "connections exists in direction" format.
	 * The result is a number from 0 to 255, with 0 being "no connections in
	 * any direction," and 255 being "a connection in all 8 directions."
	 * 
	 * This method will encode each block in the specified range.  Connections
	 * are determined by the provided qo_countNeighbor array, which will
	 * not be altered and can safely be provided multiple times, even as a 
	 * 'static' field.
	 * 
	 * NOTE: This method allows multiple encodings to be determined from one blockfield.
	 * 		Individual qOrientation appear as "neighbors" in zero on one of these
	 * 		encodings.  Different encoding sets may have values for a single block,
	 * 		each set representing different neighboring blocks to that location.
	 * 
	 * 		For example, NE0N skins allow different QOrientations to "project light"
	 * 		(== cast a shadow) into empty space.  Each unique "color" of light is represented
	 * 		in a different encoding set.  One empty block might have many different
	 * 		colors of light projected into it -- different encoding sets having different
	 * 		values for a single block.  We use 'qo_encodeAsNeighborIn' to represent the
	 * 		encoding set for each qOrientation: for example, if we use set 0 to represent
	 * 		"yellow", than all QOrientations which project "yellow" will have 0 here.
	 * 
	 * SECOND NOTE: If qo_encodeAsNeighborIn is filled with 0s and -1s, it is more efficient
	 * 		to call the method prototype w/o this parameter.
	 * 
	 * @param blockfield
	 * @param qPane
	 * @param configRange 
	 * @param encoding
	 * @param qo_countNeighbor	qo_hasNeighbor[qo1][qo2] indicates whether, when examining qo1,
	 * 			we should consider qo2 as a neighbor (and thus a "wall").  Setting the diagonal
	 * 			to 'false' will ensure that QOs connect with themselves.
	 * @param qo_behavior	our encoding behavior for the block.  If SKIP, we set
	 * 			the value at this block to ENCODED_OPEN.  If STANDARD, we calculate
	 * 			it based on its neighbors.
	 * @param qo_encodeAsNeighborIn		Specifies which of encoding[...] this qorientation
	 * 			will be considered as a neighbor to others.  For example, if 2, then the
	 * 			qOrientation will be represented as "walls" to its 8 neighbors in encoding[2],
	 * 			but not encoding[0], encoding[1], or encoding[n] with n > 2.
	 */
	synchronized protected void encodeNeighbors( byte [][][] blockfield, int qPane,
			final BlockDrawerConfigRange configRange, short [][][][] encoding,
			final boolean [][] qo_countNeighbor,
			final EncodeBehavior [] qo_behavior, final int [] qo_encodeAsNeighborIn ) {
		
		ArrayOps.fill(encoding, Render.SHEET_INDEX_ENCODED_OPEN) ;
		
		// first step: get a local copy of the range, reduced (we reduce only
		// if we are allowed to skip QOrientations.NO.
		mTempConfigRange.set( configRange ) ;
		if ( qo_behavior[QOrientations.NO] == EncodeBehavior.SKIP
				&& !mTempConfigRange.shrinkToFit(qPane, blockfield) ) {
			return ;
		}
		
		// we need to expand by 1 tick in all directions; remember that
		// we project connections into neighboring blocks.
		mTempConfigRange.inset(-1, -1) ;
		
		// There are two basic ways to implement neighbor encoding.
		// 1: For each block B, iterate over its neighbors N and encode the result
		//		in B.
		// 2: For each block B, iterate over its neighbors N and adjust their encoding
		//		to account for B.
		// This implementation uses approach #2.  This allows one iteration through
		// the blockfield, despite needing multiple encoding sets (since each block B
		// will be encoded in at most one set, when examining B we then adjust its
		// neighbors in only the specified set).  The alternative (if approach 1 were
		// used) would require iterating over all encoding sets.
		
		// second step.  Iterate over the entire range.
		int qpaneMin = qPane == Consts.QPANE_ALL ? 0 : qPane ;
		int qpaneMax = qPane == Consts.QPANE_ALL ? 1 : qPane ;
		
		for ( int qp = qpaneMin; qp <= qpaneMax; qp++ ) {
			for ( int row = mTempConfigRange.rowFirst; row < mTempConfigRange.rowBound; row++ ) {
				int r = mTempConfigRange.rowToIndex(row) ;
				
				for ( int col = mTempConfigRange.columnFirst; col < mTempConfigRange.columnBound; col++ ) {
					int c = mTempConfigRange.colToIndex(col) ;
					
					int qo = blockfield[qp][r][c] ;
					
					int set = qo_encodeAsNeighborIn[qo] ;
					
					if ( set < 0 )
						continue ;
					
					// include for all neighbors.
					// encoding is set to "empty."  If this qo is counted as a neighbor
					// for (e.g.) the block to the right, subtract IBN_BIT_1 from its
					// value (since qo is its Leftward neighbor, and +IBN_BIT_1 represents
					// open space to the left -- we remove that empty space).
					
					
					// treat xOff / yOff as the "inverse offset"; the offset
					// from some block B2 to reach the current block.
					for (int xOff = -1; xOff <= 1; xOff++) {
						int c2 = c - xOff ;
						int col2 = mTempConfigRange.indexToCol(c2) ;
						
						for (int yOff = -1; yOff <= 1; yOff++) {
							if (r != 0 || c != 0) {
								int r2 = r - yOff ;
								int row2 = mTempConfigRange.indexToRow(r2) ;
								
								int qo2 = blockfield[qp][r2][c2] ;
								if ( qo_behavior[qo2] == EncodeBehavior.STANDARD
										&& qo_countNeighbor[qo2][qo]
										&& mTempConfigRange.in( row2, col2 ) )
									encoding[set][qp][r2][c2] -= Render.sheetIndexByNeighbors_directionBit(yOff, xOff) ;
							}
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Encodes local neighborhood 8 connections for all the blocks
	 * within configRange.
	 * 
	 * Connections are encoded in a "connections exists in direction" format.
	 * The result is a number from 0 to 255, with 0 being "no connections in
	 * any direction," (walls all around) and 255 being "a connection in all 8 directions"
	 * (no walls).
	 * 
	 * This method will encode each block in the specified range.  Connections
	 * are determined by the provided qo_countNeighbor array, which will
	 * not be altered and can safely be provided multiple times, even as a 
	 * 'static' field.
	 * 
	 * @param blockfield
	 * @param qPane
	 * @param configRange 
	 * @param encoding
	 * @param qo_countNeighbor	qo_hasNeighbor[qo1][qo2] indicates whether, when examining qo1,
	 * 			we should consider qo2 as a neighbor (and thus a "wall").  Setting the diagonal
	 * 			to 'false' will ensure that QOs connect with themselves.
	 * @param qo_behavior	our encoding behavior for the block.  If SKIP, we set
	 * 			the value at this block to ENCODED_OPEN.  If STANDARD, we calculate
	 * 			it based on its neighbors.
	 */
	synchronized protected void encodeNeighbors( byte [][][] blockfield, int qPane,
			final BlockDrawerConfigRange configRange, short [][][] encoding,
			final boolean [][] qo_countNeighbor,
			final EncodeBehavior [] qo_behavior ) {
		
		ArrayOps.fill(encoding, Render.SHEET_INDEX_ENCODED_OPEN) ;
		ArrayOps.setEmpty(mTempConnected) ;
		
		// first step: get a local copy of the range, reduced (we reduce only
		// if we are allowed to skip QOrientations.NO.
		mTempConfigRange.set( configRange ) ;
		if ( qo_behavior[QOrientations.NO] == EncodeBehavior.SKIP
				&& !mTempConfigRange.shrinkToFit(qPane, blockfield) ) {
			return ;
		}
		
		// There are two basic ways to implement neighbor encoding.
		// 1: For each block B, iterate over its neighbors N and encode the result
		//		in B.
		// 2: For each block B, iterate over its neighbors N and adjust their encoding
		//		to account for B.
		// This implementation uses approach #1.  It is the most straightforward.
		
		// second step.  Iterate over the entire range.
		int qpaneMin = qPane == Consts.QPANE_ALL ? 0 : qPane ;
		int qpaneMax = qPane == Consts.QPANE_ALL ? 1 : qPane ;
		
		// assume a self-connection...
		mTempConnected[1][1] = true ;
		
		for ( int qp = qpaneMin; qp <= qpaneMax; qp++ ) {
			for ( int row = mTempConfigRange.rowFirst; row < mTempConfigRange.rowBound; row++ ) {
				int r = mTempConfigRange.rowToIndex(row) ;
				
				for ( int col = mTempConfigRange.columnFirst; col < mTempConfigRange.columnBound; col++ ) {
					int c = mTempConfigRange.colToIndex(col) ;
					
					int qo = blockfield[qp][r][c] ;
					
					// qOrientationBehavior is either SKIP or STANDARD.
					if ( qo_behavior[qo] == EncodeBehavior.SKIP )
						continue ;
					
					for (int xOff = -1; xOff <= 1; xOff++) {
						for (int yOff = -1; yOff <= 1; yOff++) {
							if (r != 0 || c != 0) {
								mTempConnected[1 + c][1 - r] =
									!qo_countNeighbor[qo][blockfield[qp][r+yOff][c+xOff]] ;
							}
						}
					}
					
					short index = Render.sheetIndexByNeighbors(mTempConnected, 1, 1);
					index = Render.sheetIndexByNeighbors_sanityFix( index ) ;
					
					encoding[qp][r][c] = index ;
				}
			}
		}
	}
	
	
	/**
	 * Examines the provided blockfield, collecting corners and setting them in
	 * 'corners.'  qo_connects indicates whether two qOrientations connect with each
	 * other (unlike the similar argument when encoding Neighborhoods, this MUST be
	 * symmetric).
	 * 
	 * PRECONDITION: qo_connects is a symmetric array showing which qOrientations
	 * "connect" with others (and thus do not form corners).  If assymetric, behavior
	 * is unspecified.
	 * 
	 * @param blockfield
	 * @param qPane
	 * @param configRange
	 * @param encoding
	 * @param qo_connects
	 * @param qo_behavior
	 */
	synchronized protected void findCorners( byte [][][] blockfield, int qPane,
			final BlockDrawerConfigRange configRange, byte [][][][] corners,
			final boolean [][] qo_connects,
			final EncodeBehavior [] qo_behavior ) {
		
		ArrayOps.fill(corners, (byte)-1) ;
		
		// first step: get a local copy of the range, reduced (we reduce only
		// if we are allowed to skip QOrientations.NO.
		mTempConfigRange.set( configRange ) ;
		if ( qo_behavior[QOrientations.NO] == EncodeBehavior.SKIP
				&& !mTempConfigRange.shrinkToFit(qPane, blockfield) ) {
			return ;
		}
		
		// second step.  Iterate over the entire range.
		int qpaneMin = qPane == Consts.QPANE_ALL ? 0 : qPane ;
		int qpaneMax = qPane == Consts.QPANE_ALL ? 1 : qPane ;
		
		
		// assume a self-connection...
		mTempConnected[1][1] = true ;
		
		for ( int qp = qpaneMin; qp <= qpaneMax; qp++ ) {
			for ( int row = mTempConfigRange.rowFirst; row < mTempConfigRange.rowBound; row++ ) {
				int r = mTempConfigRange.rowToIndex(row) ;
				
				for ( int col = mTempConfigRange.columnFirst; col < mTempConfigRange.columnBound; col++ ) {
					int c = mTempConfigRange.colToIndex(col) ;
					
					int qo = blockfield[qp][r][c] ;
					
					// qOrientationBehavior is either SKIP or STANDARD.
					if ( qo_behavior[qo] == EncodeBehavior.SKIP )
						continue ;
					
					for (int xOff = -1; xOff <= 1; xOff++) {
						for (int yOff = -1; yOff <= 1; yOff++) {
							if (r != 0 || c != 0) {
								mTempConnected[1 + c][1 - r] =
									!qo_connects[qo][blockfield[qp][r+yOff][c+xOff]] ;
							}
						}
					}
					
					// put that info in this spot, why not.
					int X, Y;

					// top-left
					X = 0;
					Y = 0;
					if (!mTempConnected[X][1] && !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_TL][row][col] = DrawSettings.CORNER_INNER_LEFT_TOP_CONVEX;
					else if (mTempConnected[X][1] && mTempConnected[X][Y]
							&& !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_TL][row][col] = DrawSettings.CORNER_INNER_RIGHT_TOP_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][Y]
							&& !mTempConnected[X][1])
						corners[qp][INDEX_CORNER_TL][row][col] = DrawSettings.CORNER_INNER_LEFT_BOTTOM_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][1]
							&& !mTempConnected[X][Y])
						corners[qp][INDEX_CORNER_TL][row][col] = DrawSettings.CORNER_INNER_LEFT_TOP_CONCAVE;
					// top-right
					X = 2;
					Y = 0;
					if (!mTempConnected[X][1] && !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_TR][row][col] = DrawSettings.CORNER_INNER_RIGHT_TOP_CONVEX;
					else if (mTempConnected[X][1] && mTempConnected[X][Y]
							&& !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_TR][row][col] = DrawSettings.CORNER_INNER_LEFT_TOP_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][Y]
							&& !mTempConnected[X][1])
						corners[qp][INDEX_CORNER_TR][row][col] = DrawSettings.CORNER_INNER_RIGHT_BOTTOM_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][1]
							&& !mTempConnected[X][Y])
						corners[qp][INDEX_CORNER_TR][row][col] = DrawSettings.CORNER_INNER_RIGHT_TOP_CONCAVE;
					// bottom-left
					X = 0;
					Y = 2;
					if (!mTempConnected[X][1] && !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_BL][row][col] = DrawSettings.CORNER_INNER_LEFT_BOTTOM_CONVEX;
					else if (mTempConnected[X][1] && mTempConnected[X][Y]
							&& !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_BL][row][col] = DrawSettings.CORNER_INNER_RIGHT_BOTTOM_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][Y]
							&& !mTempConnected[X][1])
						corners[qp][INDEX_CORNER_BL][row][col] = DrawSettings.CORNER_INNER_LEFT_TOP_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][1]
							&& !mTempConnected[X][Y])
						corners[qp][INDEX_CORNER_BL][row][col] = DrawSettings.CORNER_INNER_LEFT_BOTTOM_CONCAVE;
					// bottom-right
					X = 2;
					Y = 2;
					if (!mTempConnected[X][1] && !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_BR][row][col] = DrawSettings.CORNER_INNER_RIGHT_BOTTOM_CONVEX;
					else if (mTempConnected[X][1] && mTempConnected[X][Y]
							&& !mTempConnected[1][Y])
						corners[qp][INDEX_CORNER_BR][row][col] = DrawSettings.CORNER_INNER_LEFT_BOTTOM_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][Y]
							&& !mTempConnected[X][1])
						corners[qp][INDEX_CORNER_BR][row][col] = DrawSettings.CORNER_INNER_RIGHT_TOP_CONCAVE;
					else if (mTempConnected[1][Y] && mTempConnected[X][1]
							&& !mTempConnected[X][Y])
						corners[qp][INDEX_CORNER_BR][row][col] = DrawSettings.CORNER_INNER_RIGHT_BOTTOM_CONCAVE;
				}
			}
		}
		
	}
	
	
	protected Hashtable<BlockSize, Regions> mPreparedRegions ;
	
	
	protected Regions getPreparedRegions( BlockSize blockSize ) {
		Regions regions = mPreparedRegions.get(blockSize) ;
		if ( regions == null ) {
			throw new IllegalArgumentException("That block size was not prepared!") ;
		}
		return regions ;
	}
	
	protected FillRegions getPreparedFillRegions( BlockSize blockSize ) {
		return getPreparedRegions( blockSize ).getFillRegions() ;
	}
	
	/**
	 * Informs the component that setup (whatever that setup is) is complete.
	 * 'Prepare' should load Bitmaps (if necessary), using the preallocated (if
	 * provided) structure.
	 * 
	 * Note that not all Components need or should load Bitmaps.  We leave it to
	 * individual Components to determine whether they should do anything in
	 * this method, whether we need a non-null preallocated, etc.
	 * @param imageSize TODO
	 * @param loadSize TODO
	 * @throws IOException TODO
	 */
	@SuppressWarnings("unchecked")
	public void prepare(
			Context context,
			BlockDrawerPreallocatedBitmaps preallocated,
			Bitmap scratch,
			Hashtable<BlockSize, Regions> regions,
			int imageSize, int loadSize ) throws IOException {
		
		// shallow copy
		mPreparedRegions = (Hashtable<BlockSize, Regions>)regions.clone() ;
		
	}
	
}
