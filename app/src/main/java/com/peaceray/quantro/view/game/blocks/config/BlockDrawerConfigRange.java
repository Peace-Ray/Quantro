package com.peaceray.quantro.view.game.blocks.config;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.game.blocks.Consts;

/**
 * A BlockDrawerConfigRange indicates a range of blocks -- min/max columns and rows
 * -- as well as an "offset" the corresponds into any blockfields.  For example, 
 * we might inset into blockfields by 1 by default, so block column 0 is indicated
 * by index 1.
 * 
 * @author Jake
 *
 */
public class BlockDrawerConfigRange {

	public int columnFirst ;
	public int columnBound ;
	public int rowFirst ;
	public int rowBound ;
	
	public int insetColumn ;
	public int insetRow ;
	
	
	public int indexColFirst ;
	public int indexColBound ;
	public int indexRowFirst ;
	public int indexRowBound ;
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONSTRUCTORS / SETTERS
	// 
	
	public BlockDrawerConfigRange( int rowInset, int columnInset ) {
		this(0, 0, 0, 0, rowInset, columnInset) ;
	}

	public BlockDrawerConfigRange( int rowBound, int colBound, int rowInset, int colInset ) {
		this( 0, rowBound, 0, colBound, rowInset, colInset ) ;
	}
	
	public BlockDrawerConfigRange( int rowFirst, int rowBound, int colFirst, int colBound, int rowInset, int columnInset ) {
		set( rowFirst, rowBound, colFirst, colBound ) ;
		
		this.insetRow = rowInset ;
		this.insetColumn = columnInset ;
		
		this.indexColFirst = colToIndex( columnFirst ) ;
		this.indexColBound = colToIndex( columnBound ) ;
		this.indexRowFirst = colToIndex( rowFirst ) ;
		this.indexRowBound = colToIndex( rowBound ) ;
		
	}
	
	public BlockDrawerConfigRange( BlockDrawerConfigRange configRange ) {
		set(configRange) ;
	}
	
	public void set( int rowBound, int colBound ) {
		set( 0, rowBound, 0, colBound ) ;
	}
	
	public void set( int rowFirst, int rowBound, int colFirst, int colBound ) {
		this.columnFirst = colFirst ;
		this.columnBound = colBound ;
		this.rowFirst = rowFirst ;
		this.rowBound = rowBound ;
		
		this.indexColFirst = colToIndex( columnFirst ) ;
		this.indexColBound = colToIndex( columnBound ) ;
		this.indexRowFirst = colToIndex( rowFirst ) ;
		this.indexRowBound = colToIndex( rowBound ) ;
	}
	
	/**
	 * Sets this object to the specified range of rows and columns, while copying 
	 * the insets from the provided config range.
	 * 
	 * @param configRange
	 * @param rowFirst
	 * @param rowBound
	 * @param colFirst
	 * @param colBound
	 */
	public void set( BlockDrawerConfigRange configRange,
			int rowFirst, int rowBound, int colFirst, int colBound ) {
		this.columnFirst = colFirst ;
		this.columnBound = colBound ;
		this.rowFirst = rowFirst ;
		this.rowBound = rowBound ;
		this.insetColumn = configRange.insetColumn ;
		this.insetRow = configRange.insetRow ;
		
		this.indexColFirst = colToIndex( columnFirst ) ;
		this.indexColBound = colToIndex( columnBound ) ;
		this.indexRowFirst = colToIndex( rowFirst ) ;
		this.indexRowBound = colToIndex( rowBound ) ;
	}
	
	public void set( BlockDrawerConfigRange configRange ) {
		this.columnFirst = configRange.columnFirst ;
		this.columnBound = configRange.columnBound ;
		this.rowFirst = configRange.rowFirst ;
		this.rowBound = configRange.rowBound ;
		this.insetColumn = configRange.insetColumn ;
		this.insetRow = configRange.insetRow ;
		
		this.indexColFirst = colToIndex( columnFirst ) ;
		this.indexColBound = colToIndex( columnBound ) ;
		this.indexRowFirst = colToIndex( rowFirst ) ;
		this.indexRowBound = colToIndex( rowBound ) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// MUTATORS
	// 
	
	
	public boolean inset( int rows, int cols ) {
		this.set(
				rowFirst + rows,
				rowBound - rows,
				columnFirst + cols,
				columnBound - cols ) ;
		
		return rowBound > rowFirst && columnBound > columnFirst ;
	}
	
	
	public boolean shrinkToFit( byte [][][] blockfield ) {
		return shrinkToFit( Consts.QPANE_ALL, blockfield ) ;
	}
	
	/**
	 * Shrinks this range to fit the non-zero content of the provided blockfield.
	 * Uses its current settings, including insetColumn / insetRow, to index into
	 * the provided field.
	 * 
	 * After this call, the Range will represent a subset (not necessarily a 
	 * proper subset) of its previous setting, and there will be nonzero elements
	 * in blockField within the min and max columns, and the min and max rows.
	 * 
	 * @return Whether any nonzero content exists within the new range.
	 */
	public boolean shrinkToFit( int qPane, byte [][][] blockfield ) {
		boolean checkQ0 = qPane == Consts.QPANE_0 || qPane == Consts.QPANE_ALL ;
		boolean checkQ1 = qPane == Consts.QPANE_1 || qPane == Consts.QPANE_ALL ;
		if ( qPane == Consts.QPANE_3D )
			throw new IllegalArgumentException("Can't shrink to fit QPANE_3D") ;
		
		int cFirst = Math.max(indexColFirst, 0) ;
		int cBound = Math.min(indexColBound, blockfield[0][0].length) ;
		int rFirst = Math.max(indexRowFirst, 0) ;
		int rBound = Math.min(indexRowBound, blockfield[0].length) ;
		
		
		// move left wall to the right...
		cFirst = this.shrinkToFit_helperShrinkLeftMargin(blockfield, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// move the floor up...
		rFirst = this.shrinkToFit_helperShrinkTopMargin(blockfield, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// move the right wall to the left...
		cBound = this.shrinkToFit_helperShrinkRightMargin(blockfield, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// lower the ceiling...
		rBound = this.shrinkToFit_helperShrinkBottomMargin(blockfield, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// Set: make sure rows and cols get set appropriately.
		// Inset each entry by the difference between stored index**
		// and the current, "shrunk in" value.
		this.set(
				rowFirst + (rFirst - indexRowFirst),
				rowBound - (indexRowBound - rBound),
				columnFirst + (cFirst - indexColFirst),
				columnBound - (indexColBound - cBound) ) ;
				
		
		return ( rowFirst < rowBound && columnFirst < columnBound ) ;
	}
	
	
	public boolean shrinkToFit( byte [][][] ...blockfields ) {
		return shrinkToFit( Consts.QPANE_ALL, blockfields ) ;
	}
	
	/**
	 * Shrinks this range to fit the non-zero content of the provided blockfield.
	 * Uses its current settings, including insetColumn / insetRow, to index into
	 * the provided field.
	 * 
	 * After this call, the Range will represent a subset (not necessarily a 
	 * proper subset) of its previous setting, and there will be nonzero elements
	 * in blockField within the min and max columns, and the min and max rows.
	 * 
	 * @return Whether any nonzero content exists within the new range.
	 */
	public boolean shrinkToFit( int qPane, byte [][][] ... blockfields ) {
		boolean checkQ0 = qPane == Consts.QPANE_0 || qPane == Consts.QPANE_ALL ;
		boolean checkQ1 = qPane == Consts.QPANE_1 || qPane == Consts.QPANE_ALL ;
		if ( qPane == Consts.QPANE_3D )
			throw new IllegalArgumentException("Can't shrink to fit QPANE_3D") ;
		
		if ( blockfields == null || blockfields.length == 0 ) {
			this.set(0, 0) ;
			return false ;
		}
		
		int cFirst = Math.max(indexColFirst, 0) ;
		int cBound = Math.min(indexColBound, blockfields[0][0][0].length) ;
		int rFirst = Math.max(indexRowFirst, 0) ;
		int rBound = Math.min(indexRowBound, blockfields[0][0].length) ;
		
		
		// move left wall to the right...
		cFirst = this.shrinkToFit_helperShrinkLeftMargin(blockfields, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// move the floor up...
		rFirst = this.shrinkToFit_helperShrinkTopMargin(blockfields, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// move the right wall to the left...
		cBound = this.shrinkToFit_helperShrinkRightMargin(blockfields, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// lower the ceiling...
		rBound = this.shrinkToFit_helperShrinkBottomMargin(blockfields, checkQ0, checkQ1, rFirst, rBound, cFirst, cBound) ;
		
		// Set: make sure rows and cols get set appropriately.
		// Inset each entry by the difference between stored index**
		// and the current, "shrunk in" value.
		this.set(
				rowFirst + (rFirst - indexRowFirst),
				rowBound - (indexRowBound - rBound),
				columnFirst + (cFirst - indexColFirst),
				columnBound - (indexColBound - cBound) ) ;
				
		
		return ( rowFirst < rowBound && columnFirst < columnBound ) ;
	}
	
	
	/**
	 * Shrinks the "left margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of cFirst is returned.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	private int shrinkToFit_helperShrinkLeftMargin( byte [][][] blockfield, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( cFirst < cBound ) {
			for ( int r = rFirst; r < rBound; r++ ) {
				if ( ( checkQ0 && blockfield[0][r][cFirst] != QOrientations.NO )
						|| ( checkQ1 || blockfield[1][r][cFirst] != QOrientations.NO ) ) {
					has = true ;
					break ;
				}
			}
			if ( has )
				break ;
			cFirst++ ;
		}
		return cFirst ;
	}
	
	
	/**
	 * Shrinks the "right margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of cBound is returned.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	private int shrinkToFit_helperShrinkRightMargin( byte [][][] blockfield, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( cFirst < cBound ) {
			int c = cBound - 1 ;
			for ( int r = rFirst; r < rBound; r++ ) {
				if ( ( checkQ0 && blockfield[0][r][c] != QOrientations.NO )
						|| ( checkQ1 || blockfield[1][r][c] != QOrientations.NO ) ) {
					has = true ;
					break ;
				}
			}
			if ( has )
				break ;
			cBound-- ;
		}
		return cBound ;
	}
	
	
	/**
	 * Shrinks the "top margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of rFirst is returned.  "Top Margin" (vs. bottom) is considered to
	 * be the lower-indexed rows.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	public int shrinkToFit_helperShrinkTopMargin( byte [][][] blockfield, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( rFirst < rBound ) {
			for ( int c = cFirst; c < cBound; c++ ) {
				if ( ( checkQ0 && blockfield[0][rFirst][c] != QOrientations.NO )
						|| ( checkQ1 || blockfield[1][rFirst][c] != QOrientations.NO ) ) {
					has = true ;
					break ;
				}
			}
			if ( has )
				break ;
			rFirst++ ;
		}
		return rFirst ;
	}
	
	
	/**
	 * Shrinks the "bottom margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of rFirst is returned.  "Bottom Margin" (vs. top) is considered to
	 * be the higher-indexed rows.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	public int shrinkToFit_helperShrinkBottomMargin( byte [][][] blockfield, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( rFirst < rBound ) {
			int r = rBound - 1 ;
			for ( int c = cFirst; c < cBound; c++ ) {
				if ( ( checkQ0 && blockfield[0][r][c] != QOrientations.NO )
						|| ( checkQ1 || blockfield[1][r][c] != QOrientations.NO ) ) {
					has = true ;
					break ;
				}
			}
			if ( has )
				break ;
			rBound-- ;
		}
		return rBound ;
	}
	
	
	/**
	 * Shrinks the "left margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of cFirst is returned.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	private int shrinkToFit_helperShrinkLeftMargin( byte [][][][] blockfields, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( cFirst < cBound ) {
			for ( int r = rFirst; r < rBound; r++ ) {
				for ( int f = 0; f < blockfields.length; f++ ) {
					if ( ( checkQ0 && blockfields[f][0][r][cFirst] != QOrientations.NO )
							|| ( checkQ1 || blockfields[f][1][r][cFirst] != QOrientations.NO ) ) {
						has = true ;
						break ;
					}
				}
			}
			if ( has )
				break ;
			cFirst++ ;
		}
		return cFirst ;
	}
	
	
	/**
	 * Shrinks the "right margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of cBound is returned.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	private int shrinkToFit_helperShrinkRightMargin( byte [][][][] blockfields, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( cFirst < cBound ) {
			int c = cBound - 1 ;
			for ( int r = rFirst; r < rBound; r++ ) {
				for ( int f = 0; f < blockfields.length; f++ ) {
					if ( ( checkQ0 && blockfields[f][0][r][c] != QOrientations.NO )
							|| ( checkQ1 || blockfields[f][1][r][c] != QOrientations.NO ) ) {
						has = true ;
						break ;
					}
				}
			}
			if ( has )
				break ;
			cBound-- ;
		}
		return cBound ;
	}
	
	
	/**
	 * Shrinks the "top margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of rFirst is returned.  "Top Margin" (vs. bottom) is considered to
	 * be the lower-indexed rows.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	public int shrinkToFit_helperShrinkTopMargin( byte [][][][] blockfields, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( rFirst < rBound ) {
			for ( int c = cFirst; c < cBound; c++ ) {
				for ( int f = 0; f < blockfields.length; f++ ) {
					if ( ( checkQ0 && blockfields[f][0][rFirst][c] != QOrientations.NO )
							|| ( checkQ1 || blockfields[f][1][rFirst][c] != QOrientations.NO ) ) {
						has = true ;
						break ;
					}
				}
			}
			if ( has )
				break ;
			rFirst++ ;
		}
		return rFirst ;
	}
	
	
	/**
	 * Shrinks the "bottom margin" inward until it reaches a nonzero in blockfields.
	 * All blockfields provided (i.e. blockfields[0...n]); the margin is only
	 * shrunk if all are empty.
	 * 
	 * The new value of rFirst is returned.  "Bottom Margin" (vs. top) is considered to
	 * be the higher-indexed rows.
	 * 
	 * @param blockfields
	 * @param checkQ0
	 * @param checkQ1
	 * @param rFirst
	 * @param rBound
	 * @param cFirst
	 * @param cBound
	 * @return
	 */
	public int shrinkToFit_helperShrinkBottomMargin( byte [][][][] blockfields, boolean checkQ0, boolean checkQ1,
			int rFirst, int rBound, int cFirst, int cBound ) {
		boolean has = false ;
		while( rFirst < rBound ) {
			int r = rBound - 1 ;
			for ( int c = cFirst; c < cBound; c++ ) {
				for ( int f = 0; f < blockfields.length; f++ ) {
					if ( ( checkQ0 && blockfields[f][0][r][c] != QOrientations.NO )
							|| ( checkQ1 || blockfields[f][1][r][c] != QOrientations.NO ) ) {
						has = true ;
						break ;
					}
				}
			}
			if ( has )
				break ;
			rBound-- ;
		}
		return rBound ;
	}
	
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ACCESSORS
	// 
	
	public int rows() {
		return rowBound - rowFirst ;
	}
	
	public int cols() {
		return columnBound - columnFirst ;
	}
	
	public int rowToIndex( int row ) {
		return row + insetRow ;
	}
	
	public int colToIndex( int col ) {
		// HOT SPOT: this method is an optimization hot spot.  It is very
		// frequently used unnecessarily (only to make for-loop iteration
		// more clear: we iterate over column, which we never use except as
		// input to this method.  Iterating over column INDICES would be
		// much faster, paying the price of straightforward, readable code.
		
		// If this method is being called, especially inside a loop, you should consider it
		// a good optimization target -- e.g., iterate over indexColFirst -> indexColBound,
		// rather than columnFirst -> columnBound and then converting.
		return col + insetColumn ;
	}
	
	public int indexToRow( int index ) {
		return index - insetRow ;
	}
	
	public int indexToCol( int index ) {
		return index - insetColumn ;
	}
	
	public boolean in( int row, int col ) {
		return rowFirst <= row && row < rowBound && columnFirst <= col && columnBound < col ;
	}
	
}
