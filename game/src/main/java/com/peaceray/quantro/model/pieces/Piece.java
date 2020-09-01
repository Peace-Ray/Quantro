package com.peaceray.quantro.model.pieces;

//import android.util.Log;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.LooselyBoundedArray.LooselyBoundedArray;

public class Piece {
	
	// NEVER CHANGE THIS
	private static final int HAS_VERSION_CODE = -12399872 ;
	
	
	private static final int VERSION = 1 ;
	// First version to use explicit version coding.  Adds 'defaultRotation.'
	// Adds 'defaultType', just in case rotation and transformations changes current type.
	
	//private static final String TAG = "Piece" ;
	
	// Pieces have mostly public fields, for easy access and alteration.
	// Piece methods are largely a convenience for common things you
	// might want to do.
	
	// By convention, one shouldn't make changes to the .blocks field of
	// a Piece unless you made it yourself.  It should be noted that in
	// the Flash version, .blocks was set to point to a preallocated array
	// belonging to the RotationSystem.
	
	// A piece has a numerical type, a current rotation,
	// a last rotation direction (+1 for increase, -1 for decrease in
	// 'rotation', 0 initially), and a 3D array of blocks.
	public int type ;
	public int defaultType ;
	public int rotation ;
	public int previousRotation ;
	public int defaultRotation ;
	public int rotationDirection ;
	public byte [][][] blocks ;
		// A 3D array.  Assumed convention is [q][a][b] indexes row a, col b,
		// in QPane q.  It is also assumed that rows count UP and cols count
		// RIGHT.  Feel free to point this array to preallocated structures.
	public Offset boundsLL ;
	public Offset boundsUR ;
		// These offsets give a bounding box for the non-empty blocks in
		// this.blocks.  They reference indices into 'blocks'.  LL is a tight
		// lower bound, while UR is a loose upper-bound.
	
	public Piece() {
		boundsLL = new Offset() ;
		boundsUR = new Offset() ;
		
		rotation = 0 ;
		defaultRotation = 0 ;
		
		type = -1 ;
		defaultType = -1 ;
	}
	
	public Piece(int type) {
		boundsLL = new Offset() ;
		boundsUR = new Offset() ;
		
		rotation = 0 ;
		defaultRotation = 0 ;
		
		this.type = type ;
		this.defaultType = type ;
	}
	
	public Piece(String str) {
		this() ;
		fromString(str) ;
	}
	
	public String toString() {
		// String format:
		// For type other than 2**02, the "arbitrary blocks" piece:
		// type || rotation || previousRotation || rotationDirection || <boundsLL> || <boundsUR>
		// 		where <boundsLL> is boundsLL.x || boundsLL.y, etc.
		// For type = 2**02:
		// type || rotation || previousRotation ... || <boundsUR> || blocks_as_array_string
		
		StringBuilder sb = new StringBuilder() ;
		sb.append(HAS_VERSION_CODE).append(":").append(VERSION).append(":") ;
		
		sb.append(type) ;
		sb.append(":").append(defaultType) ;
		sb.append(":").append(rotation) ;
		sb.append(":").append(previousRotation) ;
		sb.append(":").append(defaultRotation) ;
		sb.append(":").append(rotationDirection) ;
		sb.append(":").append(boundsLL.x) ;
		sb.append(":").append(boundsLL.y) ;
		sb.append(":").append(boundsUR.x) ;
		sb.append(":").append(boundsUR.y) ;
		
		// Now, maybe include blocks.
		if ( blocks != null && !PieceCatalog.isValid(type) || ( PieceCatalog.isSpecial(type)
				&& PieceCatalog.getSpecialCategory(type) == PieceCatalog.SPECIAL_CAT_ARBITRARY ) ) {
			sb.append(":").append(ArrayOps.arrayToString(blocks)) ;
		}
		
		return sb.toString() ;
	}
	
	public void fromString( String str ) {
		String [] items = str.split(":") ;
		
		int anInt = Integer.parseInt(items[0]) ;
		if ( anInt == HAS_VERSION_CODE ) {
			int version = Integer.parseInt( items[1] ) ;
			
			if ( version < 0 || version > VERSION )
				throw new IllegalArgumentException("Bad version number; can't process " + version) ;
			
			int index = 2 ;
			type = 				Integer.parseInt(items[index++]) ;
			if ( version >= 1 )
				defaultType =	Integer.parseInt(items[index++]) ;
			else
				defaultType = 	type ;
			rotation = 			Integer.parseInt(items[index++]) ;
			previousRotation = 	Integer.parseInt(items[index++]) ;
			defaultRotation =	Integer.parseInt(items[index++]) ;
			rotationDirection = Integer.parseInt(items[index++]) ;
			boundsLL.x =	 	Integer.parseInt(items[index++]) ;
			boundsLL.y =	 	Integer.parseInt(items[index++]) ;
			boundsUR.x =	 	Integer.parseInt(items[index++]) ;
			boundsUR.y = 		Integer.parseInt(items[index++]) ;
			
			if ( index < items.length ) {
				blocks = ArrayOps.byteArrayFromString(items[index]) ;
			}
		} else {
			int index = 0 ;
			type = 				Integer.parseInt(items[index++]) ;
			rotation = 			Integer.parseInt(items[index++]) ;
			previousRotation = 	Integer.parseInt(items[index++]) ;
			rotationDirection = Integer.parseInt(items[index++]) ;
			boundsLL.x =	 	Integer.parseInt(items[index++]) ;
			boundsLL.y =	 	Integer.parseInt(items[index++]) ;
			boundsUR.x =	 	Integer.parseInt(items[index++]) ;
			boundsUR.y = 		Integer.parseInt(items[index++]) ;
			
			if ( index < items.length ) {
				blocks = ArrayOps.byteArrayFromString(items[index]) ;
			}
			
			defaultType = type ;
			defaultRotation = 0 ;
		}
	}
	
	
	public int boundWidth() { return boundsUR.x - boundsLL.x ; }
	public int boundHeight() { return boundsUR.y - boundsLL.y ; }
	
	/**
	 * setBounds	Unintelligently sets the bounds for the Piece.
	 * Uses the boundaries of the 'blocks' array.
	 */
	public void setBounds() {
		boundsLL.setX((int)0) ;
		boundsLL.setY((int)0) ;
		
		boundsUR.setRow((int) blocks[0].length) ;
		boundsUR.setCol((int) blocks[0][0].length) ;
	}
	
	/**
	 * setBounds	Set the bounds exactly.  A inthand for direct
	 * field access.
	 * 
	 * @param llx	Lowest X (column #) of a relevant block
	 * @param lly	Lowest Y (row #) of a relevant block
	 * @param urx	Loose upper bound on X (column) of relevant blocks
	 * @param ury	Loose upper bound on Y (row) of relevant blocks
	 */
	public void setBounds(int llx, int lly, int urx, int ury) {
		boundsLL.setX(llx) ;
		boundsLL.setY(lly) ;
		
		boundsUR.setRow(urx) ;
		boundsUR.setCol(ury) ;
	}
	
	
	
	/**
	 * A convenience method for use with LooselyBoundedArrays.  
	 * UNIONS the current bound in the provided lba with the bounds of
	 * this piece at the provided offset.  Uses piece-position conventions:
	 * for example, we assume that the provided offset represents the in-array
	 * position of boundsLL, with max at ( o + (boundsUR - boundsLL) ).
	 * 
	 * Because this operation uses UNION, if you are interested in the
	 * exact bounds and nothing else, you should call boundNone() on the
	 * lba before this method.
	 * 
	 * @param lba
	 */
	public void boundLBA( LooselyBoundedArray lba, Offset o ) {
		lba.bound(0, 0, 2) 		// full q-dim
			.bound(1, o.y, o.y + boundHeight())
			.bound(2, o.x, o.x + boundWidth()) ;
	}
	
	
	
	
	
	/**
	 * Adjusts the current bounds inward such that they are "tight"
	 * against the non-zero blocks within the current boundary.
	 * 
	 * Will never expand the boundaries, even if there are nonzero
	 * blocks outside of them.
	 */
	public void tightenBounds() {
		tightenBounds(null) ;
	}
	
	/**
	 * Adjusts the current bounds inward such that they are "tight"
	 * against the non-zero blocks within the current boundary.
	 * 
	 * Will never expand the boundaries, even if there are nonzero
	 * blocks outside of them.
	 * 
	 * If an Offset is provided, it will be set to the CHANGE in boundsLL.
	 * If tightening these boundaries has no effect, the provided Offset
	 * will be set to (0,0).  If tightening moved the bottom-left up 1
	 * and right 2, it will be set to (2, 1).
	 * 
	 * In addition to providing a record of the change here, this parameter
	 * can be used to ensure that a Piece's current Offset can be 
	 * accurately adjusting as bounds change.  Assuming (e.g.) that 
	 * the "Offset" of a piece is the distance from absolute origin
	 * to boundsLL, this procedure will maintain the effective position
	 * of each block:
	 * 
	 * # have piece, offset
	 * piece.tightenBounds( delta )
	 * offset.x += delta.x ;
	 * offset.y += delta.y ;
	 * 
	 * @param o
	 */
	public void tightenBounds( Offset o ) {
		if ( o != null )
			o.setXY(0, 0) ;
		
		// adjust the bottom-left upward and rightward.  Record
		// adjustments in o, if non-null.
		// first adjust rightward
		while( boundsLL.x < boundsUR.x ){
			// look for nonzero here.  If found, break.
			boolean nonzero = false ;
			for ( int y = boundsLL.y; y < boundsUR.y && !nonzero; y++ )
				for ( int q = 0; q < 2 && !nonzero; q++ )
					if ( blocks[q][y][boundsLL.x] != QOrientations.NO )
						nonzero = true ;
			if ( nonzero )
				break ;		// can't shrink anymore.
			// nothing at this x - this column.  Shrink rightward.
			boundsLL.x++ ;
			if ( o != null )
				o.x++ ;
		}
		
		// now adjust upward
		while( boundsLL.y < boundsUR.y ){
			// look for nonzero here.  If found, break.
			boolean nonzero = false ;
			for ( int x = boundsLL.x; x < boundsUR.x && !nonzero; x++ )
				for ( int q = 0; q < 2 && !nonzero; q++ )
					if ( blocks[q][boundsLL.y][x] != QOrientations.NO )
						nonzero = true ;
			if ( nonzero )
				break ;		// can't shrink anymore.
			// nothing at this y - this row.  Shrink upward.
			boundsLL.y++ ;
			if ( o != null )
				o.y++ ;
		}
		
		// next adjust the right-bound leftward.  
		while( boundsLL.x < boundsUR.x ){
			// look for nonzero at boundsUR.x-1.  If found, break.
			int x = boundsUR.x - 1 ;
			boolean nonzero = false ;
			for ( int y = boundsLL.y; y < boundsUR.y && !nonzero; y++ )
				for ( int q = 0; q < 2 && !nonzero; q++ )
					if ( blocks[q][y][x] != QOrientations.NO )
						nonzero = true ;
			if ( nonzero )
				break ;		// can't shrink anymore.
			// nothing at this x - this column.  Shrink leftward.
			boundsUR.x-- ;
		}
		
		// finally, adjust the top-bound downward.
		while( boundsLL.y < boundsUR.y ){
			// look for nonzero at boundsUR.y-1.  If found, break.
			int y = boundsUR.y - 1 ;
			boolean nonzero = false ;
			for ( int x = boundsLL.x; x < boundsUR.x && !nonzero; x++ )
				for ( int q = 0; q < 2 && !nonzero; q++ )
					if ( blocks[q][y][x] != QOrientations.NO )
						nonzero = true ;
			if ( nonzero )
				break ;		// can't shrink anymore.
			// nothing at this y - this row.  Shrink downward.
			boundsUR.y-- ;
		}
	}
	
	
	private static Piece swapSpace = new Piece() ;
	public static void swap( Piece piece1, Piece piece2 ) {
		synchronized( swapSpace ) {
			swapSpace.takeVals( piece1 ) ;
			piece1.takeVals( piece2 ) ;
			piece2.takeVals( swapSpace ) ;
		}
	}
	
	
	public void swap( Piece p ) {
		Piece.swap(this, p) ;
	}
	
	public static void swapDefaults( Piece piece1, Piece piece2 ) {
		synchronized( swapSpace ) {
			swapSpace.takeDefaultVals( piece1 ) ;
			piece1.takeDefaultVals( piece2 ) ;
			piece2.takeDefaultVals( swapSpace ) ;
		}
	}
	
	public void swapDefaults( Piece p ) {
		Piece.swapDefaults(this, p) ;
	}
	
	
	public void clear() {
		type = -1 ;
		defaultType = -1 ;
		rotation = 0 ;
		previousRotation = 0 ;
		defaultRotation = 0 ;
		rotationDirection = 0 ;
		blocks = null ;
		boundsLL.setXY(0, 0) ;
		boundsUR.setXY(0, 0) ;
	}
	
	/**
	 * An unsafe 'copy' operation that leaves the two pieces sharing
	 * a 'blocks' array.
	 * 
	 * @param piece		The piece whose values should be copied.
	 */
	public void takeVals( Piece piece ) {
		type = piece.type ;
		defaultType = piece.defaultType ;
		rotation = piece.rotation ;
		previousRotation = piece.previousRotation ;
		defaultRotation = piece.defaultRotation ;
		rotationDirection = piece.rotationDirection ;
		blocks = piece.blocks ;
		boundsLL.setX( piece.boundsLL.getX() ) ;
		boundsLL.setY( piece.boundsLL.getY() ) ;
		boundsUR.setX( piece.boundsUR.getX() ) ;
		boundsUR.setY( piece.boundsUR.getY() ) ;
	}
	
	
	public void takeDefaultVals( Piece piece ) {
		type = piece.defaultType ;
		defaultType = piece.defaultType ;
		rotation = piece.defaultRotation ;
		previousRotation = piece.defaultRotation ;
		defaultRotation = piece.defaultRotation ;
		rotationDirection = 0 ;
		blocks = null ;
		boundsLL.setXY(0, 0) ;
		boundsUR.setXY(0, 0) ;
	}
	
	
	/**
	 * A safe 'copy' operation that copies everything, including the
	 * relevant values in 'blocks'.
	 * 
	 * @return A duplicate of this piece.
	 */
	public Piece copy() {
		Piece p = new Piece() ;
		
		p.type = this.type ;
		p.defaultType = this.defaultType ;
		p.rotation = this.rotation ;
		p.previousRotation = this.previousRotation ;
		p.defaultRotation = this.defaultRotation ;
		p.rotationDirection = this.rotationDirection ;
		p.blocks = new byte[2][this.blocks[0].length][this.blocks[0][0].length] ;
		for ( int i = 0; i < 2; i++ ) {
			for ( int row = 0; row < this.blocks[0].length; row++ ) {
				for ( int col = 0; col < this.blocks[0][0].length; col++ ) {
					p.blocks[i][row][col] = this.blocks[i][row][col] ;
				}
			}
		}
		
		p.boundsLL.setX( this.boundsLL.getX() ) ;
		p.boundsLL.setY( this.boundsLL.getY() ) ;
		p.boundsUR.setX( this.boundsUR.getX() ) ;
		p.boundsUR.setY( this.boundsUR.getY() ) ;
		
		return p ;
	}
	
	/**
	 * numBlocks - the number of spaces filled by this piece.  Certain
	 * blocks are counted as two - for instance, ST or SL blocks.
	 * @return The number of blocks in this piece.
	 */
	public int numBlocks() {
		int num = 0 ;
		int Rbound = boundsUR.getRow();
		int Cbound = boundsUR.getCol();
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = boundsLL.getRow() ; r < Rbound; r++ ) {
				for ( int c = boundsLL.getCol() ; c < Cbound; c++ ) {
					if ( this.blocks[q][r][c] != QOrientations.NO )
						num++ ;
				}
			}
		}
		return num ;
	}
	
	
	/**
	 * Sets the provided offset as the row/col of this piece's
	 * "center of gravity."  Values are given as offset from boundsLL.
	 * @param o
	 */
	public void centerGravity( Offset o ) {
		int num = 0 ;
		double rowSum = 0 ;
		double colSum = 0 ;
		
		int Rbound = boundsUR.getRow();
		int Cbound = boundsUR.getCol();
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = boundsLL.getRow() ; r < Rbound; r++ ) {
				for ( int c = boundsLL.getCol() ; c < Cbound; c++ ) {
					if ( this.blocks[q][r][c] != QOrientations.NO ) {
						num++ ;
						rowSum += r - boundsLL.getRow() ;
						colSum += c - boundsLL.getCol() ;
					}
				}
			}
		}
		
		double avgRow = rowSum / num + 0.49 ;		// bias towards the middle of the center block, w/o overstepping
		double avgCol = colSum / num + 0.49 ;		// bias towards the middle of the center block, w/o overstepping
		o.setRowCol((int)Math.floor(avgRow), (int)Math.floor(avgCol)) ;
	}
	
	
	/**
	 * centerGravityColumn - returns, as an offset from boundsLL, the column
	 * for this piece's "center of gravity".
	 * @return the center of gravity column, offset from boundsLL.
	 */
	public int centerGravityColumn() {
		int num = 0 ;
		double colSum = 0 ;
		
		int Rbound = boundsUR.getRow();
		int Cbound = boundsUR.getCol();
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = boundsLL.getRow() ; r < Rbound; r++ ) {
				for ( int c = boundsLL.getCol() ; c < Cbound; c++ ) {
					if ( this.blocks[q][r][c] != QOrientations.NO ) {
						num++ ;
						colSum += c - boundsLL.getCol() ;
					}
				}
			}
		}
		
		double avgCol = colSum / num + 0.49 ;		//bias toward the middle of center column, w/o overstepping
		return (int)Math.floor(avgCol) ;
	}
	
	/**
	 * centerGravityRow - returns, as an offset from boundsLL, the row
	 * for this piece's "center of gravity".
	 * @return the center of gravity row, offset from boundsLL.
	 */
	public int centerGravityRow() {
		int num = 0 ;
		double rowSum = 0 ;
		
		int Rbound = boundsUR.getRow();
		int Cbound = boundsUR.getCol();
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = boundsLL.getRow() ; r < Rbound; r++ ) {
				for ( int c = boundsLL.getCol() ; c < Cbound; c++ ) {
					if ( this.blocks[q][r][c] != QOrientations.NO ) {
						num++ ;
						rowSum += r - boundsLL.getRow() ;
					}
				}
			}
		}
		
		double avgRow = rowSum / num + 0.49 ;	// bias towards the middle of center row, w/o overstepping
		return (int)Math.floor(avgRow) ;
	}
	
	
	/**
	 * centerColumn - returns, as an offset from boundsLL, the column
	 * for this piece's "center of gravity".
	 * @return the center of gravity column, offset from boundsLL.
	 */
	public int centerColumn() {
		int minCol = boundsUR.getCol()-1 ;
		int maxCol = boundsLL.getCol() ;
		
		int Rbound = boundsUR.getRow();
		int Cbound = boundsUR.getCol();
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = boundsLL.getRow() ; r < Rbound; r++ ) {
				for ( int c = boundsLL.getCol() ; c < Cbound; c++ ) {
					if ( this.blocks[q][r][c] != QOrientations.NO ) {
						if ( minCol > c )
							minCol = c ;
						if ( maxCol < c )
							maxCol = c ;
					}
				}
			}
		}
		
		return (int)Math.floor( (maxCol + minCol)/2.0f - boundsLL.getCol() ) ;
	}
	
	/**
	 * centerRow - returns, as an offset from boundsLL, the row
	 * for this piece's "center of gravity".
	 * @return the center of gravity row, offset from boundsLL.
	 */
	public int centerRow() {
		int minRow = boundsUR.getRow()-1 ;
		int maxRow = boundsLL.getRow() ;
		
		int Rbound = boundsUR.getRow();
		int Cbound = boundsUR.getCol();
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = boundsLL.getRow() ; r < Rbound; r++ ) {
				for ( int c = boundsLL.getCol() ; c < Cbound; c++ ) {
					if ( this.blocks[q][r][c] != QOrientations.NO ) {
						if ( minRow > r )
							minRow = r ;
						if ( maxRow < r )
							maxRow = r ;
					}
				}
			}
		}
		
		return (int)Math.floor( (maxRow + minRow)/2.0f - boundsLL.getRow() ) ;
	}
	
	
	
	
	/**
	 * bottomRow - returns, as an offset from boundsLL, the row
	 * which contains the first nonzero blocks, counting up from the bottom.
	 * @return
	 */
	public int bottomRow() {
		int Rbound = boundsUR.getRow();
		int Cbound = boundsUR.getCol();
		
		for ( int q = 0; q < 2; q++ ) {
			for ( int r = boundsLL.getRow(); r < Rbound; r++ ) {
				for ( int c = boundsLL.getCol() ; c < Cbound; c++ ) {
					if ( this.blocks[q][r][c] != QOrientations.NO ) {
						return r - boundsLL.getRow() ;
					}
				}
			}
		}
		
		// What!
		return 0 ;
	}
}
