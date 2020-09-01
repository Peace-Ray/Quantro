package com.peaceray.quantro.view.game.blocks.config;

import com.peaceray.quantro.view.game.blocks.BlockSize;

import android.graphics.Rect;

/**
 * Configuration for block size and placement.
 * 
 * Useful for producing x/y coordinates for blocks within the field.  The
 * ConfigBlocks object does NOT determine the content of each block;
 * in other words, it does not define the border width, the inset for
 * smaller blocks, etc.  All it does is sketch out a grid, within which
 * Skins can place each block.  This means that:
 * 
 * 1. All skins must be reducible to a grid within each qPane.
 * 2. All skins must be compatible with arbitrary X and Y offsets between qPanes.
 * 
 * Although we cache the results of getX / getY calls, and thus require
 * some "range" of positions in which to place them, we do NOT explicitly
 * track the total number of rows and / or columns to be drawn.
 * 
 * The position of a block is given as its top-left corner; thus, by
 * default, block (0,0) will have an X coordinate of 0 and a negative Y
 * coordinate.
 * 
 * However, one aspect of BlockDrawerConfigBlocks is that it allows two
 * different and independently applied "offsets" for the result.  The
 * first is applied at construction-time: it is the bottom-left of the
 * area within which we should, by default, draw.  The second is applied
 * at each draw pass: it is an additional offset (relative to the first)
 * for the blockfield's current position.
 * 
 * @author Jake
 *
 */
public class BlockDrawerConfigBlockGrid {
	
	@SuppressWarnings("unused")
	private static final String TAG = "BlockDrawerConfigBlockGrid" ;
	
	
	private static final int INDEX_OFFSET_QPANE = 1 ;
	private static final int INDEX_OFFSET_X = 5 ;		// beyond boundaries by up to 5
	private static final int INDEX_OFFSET_Y = 5 ;		// beyond boundaries by up to 5

	private int mRegionBottomLeftX ;
	private int mRegionBottomLeftY ;
	
	private BlockSize mBlockSize ;
	
	private int mCurrentOffsetX ;
	private int mCurrentOffsetY ;
	
	// IMMUTABLE.
	// We assume these arrays are allocated and filled upon construction, and
	// then are NEVER CHANGED.  We use this assumption to share arrays between instances.
	private int [][] mBlockXPositionCached_byQPane_X = null ;
	private int [][] mBlockYPositionCached_byQPane_Y = null ;

	
	
	/**
	 * When fitting blocks to provided rectangles, these are the fit options.
	 * @author Jake
	 *
	 */
	public enum Fit {
		/**
		 * Use the 'best fit', trying to optimize all provided 
		 * values.  Will center within the provided rect.
		 */
		BEST_CENTER,
		
		
		/**
		 * Fits the columns exactly within the available horizontal region.
		 * Aligns the vertical region to the bottom, allowing it to expand 
		 * above the provided Rectangle if necessary.
		 */
		EXACT_HORIZONTAL_BOTTOM
	}
	
	
	public enum Align {
		
		/**
		 * Centers the provided number of columns or rows within the block field.
		 * If cannot center (because there are too many rows and/or columns),
		 * revert to grid-alignment.
		 */
		CENTER_ATTEMPT,
		
		/**
		 * Center no matter what.  If there are too many rows/cols, push
		 * off the edge to fit.
		 */
		CENTER,
		
		/**
		 * Aligns blocks to the bottom-left of the draw region.  Block (0,0)
		 * will be drawn with its bottom-left to the bottom-left of the region.
		 */
		GRID
	}
	
	
	public enum Scale {
		
		/**
		 * Use the best-fit possible; the maximum size that will fit within
		 * the provided area, with as accurate a qOffset as possible.
		 */
		BEST,
		
		/**
		 * If at all possible, set this dimension to exactly fit the available space.
		 * This typically requires a nonzero qOffset in the specified dimension.
		 */
		FLUSH,
		
		/**
		 * The provided "maximum dimension" is in fact the EXACT value to use.
		 */
		VALUE
	}
	
	
	/**
	 * Provided with the number of rows and cols to display, and our preferred
	 * qOffsets, slightly shrinks the Rect to best fit these settings.
	 * 
	 * @param fitWithin
	 * @param displayedRows
	 * @param displayedCols
	 * @param qXOffset
	 * @param qYOffset
	 * @param fit Used only to determine if we use square or rectangular blocks.
	 */
	public static void shrinkRectToFit(
			Rect fitWithin, int displayedRows, int displayedCols,
			float qXOffset, float qYOffset ) {
		
		if ( displayedRows == 0 && displayedCols == 0 )
			throw new IllegalArgumentException("Need at least one nonzero displayed rows or cols.") ;
		
		// two steps.  First, determine how many blocks we could
		// fit if we didn't have a QOffset to deal with.    Then
		// shrink block sizes by 1 pixel at a time until our qOffset
		// fits within.  Finally, shrink the rect to fit the exact size.
		
		// result is rounded down.
		int width = displayedCols == 0 ? Integer.MAX_VALUE : fitWithin.width() / displayedCols ;
		int height = displayedRows == 0 ? Integer.MAX_VALUE : fitWithin.height() / displayedRows ;
		
		// shrink width / height to fit
		if ( width != Integer.MAX_VALUE ) {
			while( true ) {
				if ( width * (displayedCols + Math.abs(qXOffset)) <= fitWithin.width() )
					break ;
				width-- ;
			}
		}
		
		if ( height != Integer.MAX_VALUE ) {
			while( true ) {
				if ( height * (displayedRows + Math.abs(qYOffset)) <= fitWithin.height() )
					break ;
				height-- ;
			}
		}
		
		// square up
		width = height = Math.min(width, height) ;
		
		// match the rect to this.
		int diffW = fitWithin.width() - Math.round( width * (displayedCols + Math.abs(qXOffset)) ) ;
		int diffH = fitWithin.height() - Math.round( height * (displayedRows + Math.abs(qYOffset)) ) ;
		
		int inL = diffW / 2 ;
		int inR = (int)Math.ceil( diffW / 2.0 ) ;
		int inT = diffH / 2 ;
		int inB = (int)Math.ceil( diffH / 2.0 ) ;
		
		if ( displayedCols > 0 ) {
			fitWithin.left += inL ;
			fitWithin.right -= inR ;
		}
		if ( displayedRows > 0 ) {
			fitWithin.top += inT ;
			fitWithin.bottom -= inB ;
		}
	}
	
	
	/**
	 * Fits the specified number of displayedRows, displayedColumns,
	 * within the provided rectangle using the specified fit mode.
	 * 
	 * Be careful using 'EXACT' fits; these fits match the exact
	 * dimensions of the provided Rect, potentially by over-extending
	 * the qOffset for that dimension far beyond what you'd like.
	 * 
	 * @param rows
	 * @param cols
	 * @param fitWithin
	 * @param displayedRows
	 * @param displayedCols
	 * @param qXOffset
	 * @param qYOffset
	 * @param fit
	 */
	public BlockDrawerConfigBlockGrid(
			int rows, int cols,
			Rect fitWithin, int displayedRows, int displayedCols,
			float qXOffset, float qYOffset,
			Align alignX, Align alignY, Scale scaleX, Scale scaleY ) {
		
		this( rows, cols, fitWithin, displayedRows, displayedCols,
				Integer.MAX_VALUE, Integer.MAX_VALUE, qXOffset, qYOffset, alignX, alignY, scaleX, scaleY ) ;
		
	}
	
	
	public BlockDrawerConfigBlockGrid(
			int rows, int cols,
			Rect fitWithin, int displayedRows, int displayedCols,
			int maxWidth, int maxHeight, float qXOffset, float qYOffset,
			Align alignX, Align alignY, Scale scaleX, Scale scaleY ) {
		
		if ( ( scaleX == Scale.VALUE && maxWidth == Integer.MAX_VALUE )
				|| ( scaleY == Scale.VALUE && maxHeight == Integer.MAX_VALUE ) )
			throw new IllegalArgumentException("Scale.VALUE provided, but MAX_VALUE given.") ;
		
		// set idealized values independently for width, height.
		int width = scaleBlockDimension( fitWithin.width(), displayedCols, maxWidth, qXOffset, scaleX ) ;
		int height = scaleBlockDimension( fitWithin.height(), displayedRows, maxHeight, qYOffset, scaleY ) ;
		
		// square-up!
		width = height = Math.min( width, height ) ;
		
		// apply final FLUSH, if needed.
		int qx = scaleQOffsetDimension( fitWithin.width(), displayedCols, width, qXOffset, scaleX ) ;
		int qy = scaleQOffsetDimension( fitWithin.height(), displayedRows, height, qYOffset, scaleY ) ;
		
		mBlockSize = new BlockSize( width, height, qx, qy ) ;
		
		// we have block dimension and qOffset.  Set the offsets from left-bottom.
		mRegionBottomLeftX = findAlignLeftOffset( fitWithin, displayedCols, width, qx, alignX ) ;
		mRegionBottomLeftY = findAlignBottomOffset( fitWithin, displayedRows, height, qy, alignY ) ;
		
		this.mCurrentOffsetX = 0 ;
		this.mCurrentOffsetY = 0 ;
		
		this.initCache( rows, cols ) ;
	}
	
	
	private int scaleBlockDimension( int space, int numBlocks, int maxSize, float qOffset, Scale scale ) {
		if ( scale == null )
			throw new NullPointerException("Null scale provided!") ;
		
		float realBlocks = numBlocks + Math.abs( qOffset ) ;
		
		int dim, offset ;
		
		switch( scale ) {
		case BEST:
		case FLUSH:
			// attempt to scale to fit the specified number of blocks
			// within the available space, including the qOffset, and get
			// that offset as close as possible.
			dim = Math.round( space / realBlocks ) ;
			offset = Math.abs( Math.round( dim * qOffset ) ) ;
			
			while ( dim > 0 && dim * numBlocks + offset > space )
				dim-- ;
			
			return Math.min(dim, maxSize) ;
			
		case VALUE:
			return maxSize ;
		}
		
		throw new IllegalArgumentException("Can't calculate scale for " + scale) ;
	}
	
	
	private int scaleQOffsetDimension( int space, int numBlocks, int blockSize, float qOffset, Scale scale ) {
		if ( scale == null )
			throw new NullPointerException("Null scale provided!") ;
		
		int leftover ;
		
		switch( scale ) {
		case BEST:
		case VALUE:
			// exact
			return Math.round( blockSize * qOffset ) ;
			
		case FLUSH:
			leftover = space - numBlocks * blockSize ;
			if ( leftover >= 0 ) {
				if ( qOffset > 0 )
					return leftover ;
				else if ( qOffset < 0 )
					return -leftover ;
			}
			
			return 0 ;
		}
		
		throw new IllegalArgumentException("Can't calculate scale for " + scale) ;
	}
	
	
	private int findAlignLeftOffset( Rect fitWithin, int numCols, int width, int qOffset, Align align ) {
		if ( align == null )
			throw new NullPointerException("Null align provided!") ;
		
		switch( align ) {
		case CENTER_ATTEMPT:
			// if we can fit fully within the rect, center our content horizontally.
			// If not, set at the start.
			if ( numCols * width + qOffset > fitWithin.width() )
				return findAlignLeftOffset( fitWithin, numCols, width, qOffset, Align.GRID ) ;
			else
				return findAlignLeftOffset( fitWithin, numCols, width, qOffset, Align.CENTER ) ;
			
		case CENTER:
			// find the "center" of the blocks displayed; position it in the "center" of
			// the rect.  This can be represented as a positive offset added to the left edge,
			// equivalent to the 1/2 difference between the space available and the space taken
			// up by the blocks.
			return fitWithin.left + ( fitWithin.width() - (numCols * width + qOffset) )/2 ;
		
		case GRID:
			// align exactly to the left edge.
			return fitWithin.left ;
		}
		
		throw new IllegalArgumentException("Can't calculate offset for " + align) ;
	}
	
	private int findAlignBottomOffset( Rect fitWithin, int numRows, int height, int qOffset, Align align ) {
		if ( align == null )
			throw new NullPointerException("Null align provided!") ;
		
		switch( align ) {
		case CENTER_ATTEMPT:
			// if we can fit fully within the rect, center our content horizontally.
			// If not, set at the start.
			if ( numRows * height + qOffset > fitWithin.height() )
				return findAlignBottomOffset( fitWithin, numRows, height, qOffset, Align.GRID ) ;
			else 
				return findAlignBottomOffset( fitWithin, numRows, height, qOffset, Align.CENTER ) ;
			
		case CENTER:
			// find the "center" of the blocks displayed; position it in the "center" of
			// the rect.  This can be represented as a positive offset subtracted from the bottom edge,
			// equivalent to the 1/2 difference between the space available and the space taken
			// up by the blocks.
			return fitWithin.bottom - ( fitWithin.height() - (numRows * height + qOffset) )/2 ;
		
		case GRID:
			// align exactly to the bottom edge.
			return fitWithin.bottom ;
		}
		
		throw new IllegalArgumentException("Can't calculate offset for " + align) ;
	}
	
	
	public BlockDrawerConfigBlockGrid(
			int rows, int displayedRows, int cols,
			int width, int height,
			int qXOffset, int qYOffset,
			int regionBottomLeftX, int regionBottomLeftY ) {
		
		init( width, height,
				qXOffset, qYOffset,
				regionBottomLeftX, regionBottomLeftY,
				rows, cols ) ;
		
	}
	
	
	private void init(
			int width, int height,
			int qXOffset, int qYOffset,
			int regionBottomLeftX, int regionBottomLeftY,
			int numCachedRows, int numCachedCols ) {
		
		this.mRegionBottomLeftX = regionBottomLeftX ;
		this.mRegionBottomLeftY = regionBottomLeftY ;
		
		this.mBlockSize = new BlockSize( width, height, qXOffset, qYOffset ) ;
		
		this.mCurrentOffsetX = 0 ;
		this.mCurrentOffsetY = 0 ;
		
		this.initCache(numCachedRows, numCachedCols) ;
	}
	
	private void initCache( int numCachedRows, int numCachedCols ) {
		numCachedRows += INDEX_OFFSET_Y*2 ;
		numCachedCols += INDEX_OFFSET_X*2 ;
		
		this.mBlockXPositionCached_byQPane_X = new int[3][numCachedCols] ;
		this.mBlockYPositionCached_byQPane_Y = new int[3][numCachedRows] ;
		
		for ( int q = 0; q < 3; q++ ) {
			int qp = q - INDEX_OFFSET_QPANE ;
			for ( int c = 0; c < numCachedCols; c++ ) {
				int col = c - INDEX_OFFSET_X ;
				this.mBlockXPositionCached_byQPane_X[q][c] = calculateX(qp, col) ;
			}
			for ( int r = 0; r < numCachedRows; r++ ) {
				int row = r - INDEX_OFFSET_Y ;
				this.mBlockYPositionCached_byQPane_Y[q][r] = calculateY(qp, row) ;
			}
		}
	}
	
	
	public BlockDrawerConfigBlockGrid( BlockDrawerConfigBlockGrid configBlocks ) {
		this.mRegionBottomLeftX = configBlocks.mRegionBottomLeftX ;
		this.mRegionBottomLeftY = configBlocks.mRegionBottomLeftY ;
		
		this.mBlockSize = new BlockSize( configBlocks.mBlockSize ) ;
		
		this.mCurrentOffsetX = configBlocks.mCurrentOffsetX ;
		this.mCurrentOffsetY = configBlocks.mCurrentOffsetY ;
		
		// FUN FACT: since this is immutable, and deterministic given
		// the values above (excluding mCurrentOffsetX/Y, which is irrelevant),
		// we can just take a reference to the same array.
		this.mBlockXPositionCached_byQPane_X = configBlocks.mBlockXPositionCached_byQPane_X ;
		this.mBlockYPositionCached_byQPane_Y = configBlocks.mBlockYPositionCached_byQPane_Y ;

	}
	
	
	public BlockSize getBlockSize() {
		return mBlockSize ;
	}
	
	
	/**
	 * Returns the block width.
	 * @return
	 */
	public int getWidth() {
		return mBlockSize.getBlockWidth() ;
	}
	
	/**
	 * Returns the block height.
	 * @return
	 */
	public int getHeight() {
		return mBlockSize.getBlockHeight() ;
	}
	
	public int getQXOffset() {
		return mBlockSize.getQXOffset() ;
	}
	
	public int getQYOffset() {
		return mBlockSize.getQYOffset() ;
	}
	
	
	
	
	/**
	 * Returns the top-left X coordinate of the specified column.
	 * qPane may be 0, 1, or -1.  0 and 1 are QPanes; -1 is:
	 * 
	 * getX( -1, C ) = Math.min( getX(0,C), getX(1,C) )
	 * 
	 * In other words, the left-bound of both panes considered together.
	 * 
	 * @param qPane The qPane for the block: 0, 1, or -1 meaning minimum of both.
	 * @param col The block's column.
	 * @return The x-coordinate of the block's left edge.
	 * @throws ArrayIndexOutOfBoundsException If the specified block is outside our
	 * 			constructor-time precomputation.
	 */
	public int getX( int qPane, int col ) throws ArrayIndexOutOfBoundsException {
		
		int cache_index_qPane = qPane + INDEX_OFFSET_QPANE ;
		int cache_index_col = col + INDEX_OFFSET_X ;
		
		return mBlockXPositionCached_byQPane_X[cache_index_qPane][cache_index_col] + mCurrentOffsetX ;
	}
	
	
	/**
	 * Returns the top-left Y coordinate of the specified column.
	 * qPane may be 0, 1, or -1.  0 and 1 are QPanes; -1 is:
	 * 
	 * getY( -1, R ) = Math.min( getY(0,R), getY(1,R) )
	 * 
	 * In other words, the upper-bound of both panes considered together.
	 * 
	 * @param qPane The qPane for the block: 0, 1, or -1 meaning minimum of both.
	 * @param row The block's row.
	 * @return The y-coordinate of the block's top edge.
	 * @throws ArrayIndexOutOfBoundsException If the specified block is outside our
	 * 			constructor-time precomputation.
	 */
	public int getY( int qPane, int row ) throws ArrayIndexOutOfBoundsException {
		
		int cache_index_qPane = qPane + INDEX_OFFSET_QPANE ;
		int cache_index_row = row + INDEX_OFFSET_Y ;
		
		return mBlockYPositionCached_byQPane_Y[cache_index_qPane][cache_index_row] + mCurrentOffsetY ;
	}
	
	
	
	/**
	 * Sets the provided rect to the boundaries of the specified block.
	 * Returns the provided rect.
	 * 
	 * If 'r' is null, allocates a Rect and returns it instead.
	 * 
	 * @param r
	 * @param qPane
	 * @param row
	 * @param col
	 * @throws ArrayIndexOutOfBoundsException If the specified block is outside our
	 * 			constructor-time precomputation.
	 */
	public Rect getBounds( Rect r, int qPane, int row, int col ) throws ArrayIndexOutOfBoundsException {
		if ( r == null )
			r = new Rect() ;
		
		r.left = getX(qPane, col) ;
		r.top = getY(qPane, row) ;
		r.right = getX(qPane, col+1) ;
		r.bottom = getY(qPane, row-1) ;
		
		return r ;
	}
	
	
	/**
	 * Sets the offset used by this ConfigBlocks.  Offsets shift the
	 * entire grid.  This is the 'second offset' applied, the first being
	 * one applied at construction time.  This call represents an offset
	 * from the first.
	 * 
	 * @param x
	 * @param y
	 */
	public void setOffset( int x, int y ) {
		mCurrentOffsetX = x ;
		mCurrentOffsetY = y ;
	}
	
	
	/**
	 * Calculates the top-left X coordinate of the specified
	 * column.  
	 * 
	 * Identical to getX(), except:
	 * 
	 * 1. A slower, more complex operation
	 * 2. No chance of throwing an exception.
	 * 
	 * @param qPane
	 * @param col
	 * @return
	 */
	public int calculateX( int qPane, int col ) {
		if ( qPane < 0 )
			return Math.min(calculateX(0, col), calculateX(1, col)) ;
		
		int pos = mRegionBottomLeftX + mCurrentOffsetX + mBlockSize.getBlockWidth() * col ;
		if ( (qPane == 0) == (mBlockSize.getQXOffset() < 0) )
			pos += Math.abs(mBlockSize.getQXOffset()) ;
		
		return pos ;
	}
	
	
	/**
	 * Calculates the top-left Y coordinate of the specified
	 * column.  
	 * 
	 * Identical to getY(), except:
	 * 
	 * 1. A slower, more complex operation
	 * 2. No chance of throwing an exception.
	 * 
	 * @param qPane
	 * @param col
	 * @return
	 */
	public int calculateY( int qPane, int row ) {
		if ( qPane < 0 )
			return Math.min(calculateY(0, row), calculateY(1, row)) ;
		
		int pos = mRegionBottomLeftY + mCurrentOffsetY - mBlockSize.getBlockHeight() * (row+1) ;
		if ( (qPane == 0) == (mBlockSize.getQYOffset() > 0) )
			pos -= Math.abs(mBlockSize.getQYOffset()) ;
		
		return pos ;
	}
	
	
	/**
	 * Calculates the provided rect to the boundaries of the specified block.
	 * Returns the provided rect.
	 * 
	 * If 'r' is null, allocates a Rect and returns it instead.
	 * 
	 * Identical to getBounds(), except:
	 * 
	 * 1. A slower, more complex operation
	 * 2. No chance of throwing an exception.
	 * 
	 * @param r
	 * @param qPane
	 * @param row
	 * @param col
	 */
	public Rect calculateBounds( Rect r, int qPane, int row, int col ) {
		if ( r == null )
			r = new Rect() ;
		
		r.left = calculateX(qPane, col) ;
		r.top = calculateY(qPane, row) ;
		r.right = calculateX(qPane, col+1) ;
		r.bottom = calculateY(qPane, row-1) ;
		
		return r ;
	}
	
}
