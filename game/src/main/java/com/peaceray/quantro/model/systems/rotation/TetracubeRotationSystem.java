package com.peaceray.quantro.model.systems.rotation;

import com.peaceray.quantro.exceptions.InvalidPieceException;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;

/**
 * The TetracubeRotationSystem extends TetrominoRotationSystem,
 * adding the capacity to rotate Tetracubes.  Tetromino rotation
 * is handled by the superclass.
 * 
 * Note: for proper state recovery, any class implementing SerializableState
 * should take care to maintain a state that is robust against changes
 * to its superclasses.  However, in this case, neither TetracubeRotationSystem
 * nor (at time of writing) TetrominoRotationSystem maintain a state,
 * instead using the provided "EmptyState" class.
 *
 * We therefore leave implementation of the SerializableState methods to
 * the superclass, overriding only finalizeConfiguration;
 * if TetrominoRotationSystem becomes stateful, TetracubeRotationSystem
 * will not notice the change.
 * 
 * @author Jake
 *
 */
public class TetracubeRotationSystem extends TetrominoRotationSystem {
	
	private byte [][][][][][][] preallocatedTetracubesByCategorySubcategoryQOrientationRotation ;
	
	public TetracubeRotationSystem(GameInformation ginfo, QInteractions qi, int [] types) {
		super(ginfo, qi, types);
		allocateTetracubes( types ) ;
	}
	
	////////////////////////////////////////////////////////////////
	//
	// STATEFUL METHODS
	//
	// Although this system does not maintain a state,
	// these methods are considered "stateful" under the specification
	// given by SerializableState.
	//
	////////////////////////////////////////////////////////////////
	
	public void turnMinimumHorizontalProfile( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		p.rotation = 1 ;	// Minimal for all tetracubes, AND tetrominos!!
		this.turn0(p) ;
	}
	
	
	public void flipHorizontal( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( !PieceCatalog.isTetracube(p) ) {
			super.flipHorizontal(p, o) ;
			return ;
		}
		
		// Flips this piece horizontally.  This might change its rotation
		// and/or convert it to a different piece.
		int cat = PieceCatalog.getTetracubeCategory(p) ;
		int scat = PieceCatalog.getTetracubeSubcategory(p) ;
		int qc = qi.flip( PieceCatalog.getQCombination(p) ) ;
		switch( cat ) {
		case PieceCatalog.TETRA_CAT_L:
			// The two subcats are configured to put the main block on opposite
			// ends, with the long offset in either pane 0 or pane 1.  Flip the subcat.
			scat = (scat + 1) % 2 ;
			standardFlipHorizontal( p, PieceCatalog.encodeTetracube( cat, scat, qc ) ) ;
			break ;
			
		case PieceCatalog.TETRA_CAT_RECT:
			// Rectangles have no subcategories and a flip has no effect.
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			turn0(p) ;
			break ;
			
		case PieceCatalog.TETRA_CAT_S:
			// Only one subcategory.  Rotations 1 or 3 do not change
			// anything.  However, rotations 0 or 2 will need to be 180ed
			// to perform the flip.
			standardFlipHorizontal( p, PieceCatalog.encodeTetracube( cat, scat, qc ) ) ;
			break ;
			
		case PieceCatalog.TETRA_CAT_T:
			// simple enough.  All are symmetric to a flip, but 
			// changing the subcategory reverses the Q0 / Q1 positions.
			scat = (scat + 1) % 2 ;
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			turn0(p) ;
			break ;
		
		case PieceCatalog.TETRA_CAT_BRANCH:
			// branch is a three-pronged thingy like a Spathi ship.
			// subcategory determines which qpane contain the nexus
			// and two branches.  Therefore, flip the subcat.
			scat = (scat + 1) % 2 ;
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			// Our rotations are configured s.t. scat 0, rot 0 is a
			// horiz. reflection of scat 1, rot 0.  There's no need
			// to change the rotation for rot 0 or 2.  However, rot
			// 1 and 3 are VERTICAL reflections of the opposite
			// subcat, so we SHOULD 180 them.
			if ( p.rotation % 2 == 1 ) {
				if ( p.rotationDirection < 0 )
					turnCCW180(p) ;
				else
					turnCW180(p) ;
			} else
				turn0(p) ;
			// there is no need to change the offset.
			break ;
			
		case PieceCatalog.TETRA_CAT_SCREW:
			// Unlike most cases, the screw remains in the same subcategory.
			// A horizontal flip is actually equivalent to changing rotation.
			// For scat 0:
			// 0 <--> 3		1 <--> 2
			// For scat 1:
			// 0 <--> 1		2 <--> 3
			// Scary, isn't it?  Examine the default blocks and picture why this
			// is the case.
			if ( scat == 0 ) {
				this.rotSwap0312Flip(p, PieceCatalog.encodeTetracube( cat, scat, qc )) ;
			} else {
				this.rotSwap0123Flip(p, PieceCatalog.encodeTetracube( cat, scat, qc )) ;
			}
			break ;
			
		case PieceCatalog.TETRA_CAT_CORNER:
			// Flips retain "handedness."  0 3, 1 2 are the same handedness.
			switch( scat ) {
			case 0:
				scat = 3 ;
				break ;
			case 1:
				scat = 2 ;
				break ;
			case 2:
				scat = 1 ;
				break ;
			case 3:
				scat = 0 ;
				break ;
			}
			// The subcategory change contains the flip for rotations 0 and 2,
			// but 1 and 3 are now reversed.  Do a 180 for them.
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			if ( p.rotation % 2 == 1 ) {
				if ( p.rotationDirection < 0 )
					turnCCW180(p) ;
				else
					turnCW180(p) ;
			} else
				turn0(p) ;
			break ;
		}
	}
	
	
	public void flipVertical( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( !PieceCatalog.isTetracube(p) ) {
			super.flipVertical(p, o) ;
			return ;
		}
		
		// Flips this piece horizontally.  This might change its rotation
		// and/or convert it to a different piece.
		int cat = PieceCatalog.getTetracubeCategory(p) ;
		int scat = PieceCatalog.getTetracubeSubcategory(p) ;
		int qc = qi.flip( PieceCatalog.getQCombination(p) ) ;
		switch( cat ) {
		case PieceCatalog.TETRA_CAT_L:
			// The two subcats are configured to put the main block on opposite
			// ends, with the long offset in either pane 0 or pane 1.  Flip the subcat.
			scat = (scat + 1) % 2 ;
			standardFlipVertical( p, PieceCatalog.encodeTetracube( cat, scat, qc ) ) ;
			break ;
			
		case PieceCatalog.TETRA_CAT_RECT:
			// Rectangles have no subcategories and a flip has no effect.
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			turn0(p) ;
			break ;
			
		case PieceCatalog.TETRA_CAT_S:
			// Only one subcategory.  Rotations 0 or 2 do not change
			// anything.  However, rotations 1 or 3 will need to be 180ed
			// to perform the flip.
			standardFlipVertical( p, PieceCatalog.encodeTetracube( cat, scat, qc ) ) ;
			break ;
			
		case PieceCatalog.TETRA_CAT_T:
			// simple enough.  All are symmetric to a flip, but 
			// changing the subcategory reverses the Q0 / Q1 positions.
			scat = (scat + 1) % 2 ;
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			turn0(p) ;
			break ;
		
		case PieceCatalog.TETRA_CAT_BRANCH:
			// branch is a three-pronged thingy like a Spathi ship.
			// subcategory determines which qpane contain the nexus
			// and two branches.  Therefore, flip the subcat.
			scat = (scat + 1) % 2 ;
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			// Our rotations are configured s.t. scat 0, rot 0 is a
			// horiz. reflection of scat 1, rot 0.  We therefore
			// flip the rotation for them.  However, rot
			// 1 and 3 are VERTICAL reflections of the opposite
			// subcat, so they remain the same.
			if ( p.rotation % 2 == 0 ) {
				if ( p.rotationDirection < 0 )
					turnCCW180(p) ;
				else
					turnCW180(p) ;
			} else
				turn0(p) ;
			// there is no need to change the offset.
			break ;
			
		case PieceCatalog.TETRA_CAT_SCREW:
			// Unlike most cases, the screw remains in the same subcategory.
			// A vertical flip is actually equivalent to changing rotation.
			// For scat 0:
			// 0 <--> 1		2 <--> 3
			// For scat 1:
			// 0 <--> 3		1 <--> 2
			// Scary, isn't it?  Examine the default blocks and picture why this
			// is the case.
			if ( scat == 0 ) {
				this.rotSwap0123Flip(p, PieceCatalog.encodeTetracube( cat, scat, qc )) ;
			} else {
				this.rotSwap0312Flip(p, PieceCatalog.encodeTetracube( cat, scat, qc )) ;
			}
			break ;
			
		case PieceCatalog.TETRA_CAT_CORNER:
			// Flips retain "handedness."  0 3, 1 2 are the same handedness.
			switch( scat ) {
			case 0:
				scat = 3 ;
				break ;
			case 1:
				scat = 2 ;
				break ;
			case 2:
				scat = 1 ;
				break ;
			case 3:
				scat = 0 ;
				break ;
			}
			// The subcategory change contains the flip for rotations 1 and 3,
			// but 0 and 2 are now reversed.  Do a 180 for them.
			p.type = PieceCatalog.encodeTetracube(cat, scat, qc) ;
			if ( p.rotation % 2 == 0 ) {
				if ( p.rotationDirection < 0 )
					turnCCW180(p) ;
				else
					turnCW180(p) ;
			} else
				turn0(p) ;
			break ;
		}
	}
	
	
	@Override
	protected byte [][][] getPreallocatedBlocks( Piece p ) throws InvalidPieceException {
		if ( PieceCatalog.isTetracube(p.type) ) {
			int cat = PieceCatalog.getTetracubeCategory(p.type) ;
			int scat = PieceCatalog.getTetracubeSubcategory(p.type) ;
			int qo = PieceCatalog.getQCombination(p.type) ;
			int r = p.rotation ;
			
			byte [][][] result ;
			
			try {
				result = preallocatedTetracubesByCategorySubcategoryQOrientationRotation[cat][scat][qo][r] ;
			} catch ( NullPointerException npe ) {
				System.err.println("TetracubeRotationSystem: In-use allocation of blocks for piece type " + p.type) ;
				allocateTetracubes( new int[]{p.type} ) ;
				result = preallocatedTetracubesByCategorySubcategoryQOrientationRotation[cat][scat][qo][r] ;
			} catch ( RuntimeException e ) {
				System.err.println("TetracubeRotationSystem: Failure for piece type " + p.type) ;
				System.err.println("TetracubeRotationSystem: Failure for piece category " + PieceCatalog.getTetracubeCategory(p.type)) ;
				System.err.println("TetracubeRotationSystem: Failure for piece subcategory " + PieceCatalog.getTetracubeSubcategory(p.type)) ;
				throw e ;
			}
			
			// Return the preallocated blocks for this tetromino category,
			// orientation and rotation.
			return result ;
		} else {
			return super.getPreallocatedBlocks(p) ;
		}
	}
	
	// Does this rotation system support the specified piece type?
	@Override
	public boolean supportsPiece( int type ) {
		return PieceCatalog.isTetromino(type) || PieceCatalog.isTetracube(type) ;
	}
	
	@Override
	public boolean supportsPiece( Piece p ) {
		return this.supportsPiece(p.type) ;
	}
	
	// All turns are handled by the subclass; the methods 
	// call "getPreallocatedBlocks"
	
	private void allocateTetracubes( int [] types ) {
		if ( preallocatedTetracubesByCategorySubcategoryQOrientationRotation == null )
			preallocatedTetracubesByCategorySubcategoryQOrientationRotation
					= new byte[PieceCatalog.NUMBER_TETRACUBE_CATEGORIES][][][][][][] ;
		
		byte [][][][][][][] prealloc = preallocatedTetracubesByCategorySubcategoryQOrientationRotation ;
		
		// allocate the blocks for every tetracube in the provided list.
		for ( int t = 0; t < types.length; t++ ) {
			int type = types[t] ;
			if ( PieceCatalog.isTetracube(type) ) {
				int c = PieceCatalog.getTetracubeCategory(type) ;
				int s = PieceCatalog.getTetracubeSubcategory(type) ;
				int qc = PieceCatalog.getQCombination(type) ;
				
				// does this need allocation?
				if ( prealloc[c] == null )
					prealloc[c] = new byte[ PieceCatalog.NUMBER_TETRACUBE_SUBCATEGORIES[c] ][][][][][] ;
				if ( prealloc[c][s] == null )
					prealloc[c][s] = new byte[ QCombinations.NUM ][][][][] ;
				if ( prealloc[c][s][qc] == null ) {
					prealloc[c][s][qc] = new byte[4][][][] ;
					for ( int r = 0; r < 4; r++ )
						// Call another function
						prealloc[c][s][qc][r] = allocateTetraBlocks(c,s,qc,r) ;
				}
			}
		}
	}
	
	private static byte [][][] allocateTetraBlocks( int category, int subCategory, int qOrientation, int rotation ) {
		// To make things more human-readable, this first allocates a
		// 2D array with the qOrientation listed as the QCombination.
		// Then it uses QCombinations to fill it in.
		
		// Actually, we do this is three steps.  First we select a 
		// 0, QOrientation block array that is represented in a
		// human-readable way.  Then we allocate a vertically-flipped copy.
		// Finally we convert the result.
		
		// This long, complex process has one intended side-effect:
		// the tetracube represented is visually shown in the code below,
		// and can be easily modified if desired.  The vertical flip
		// is necessary since by convention row 0 is at the bottom,
		// whereas these pictures necessarily number rows from the top.
		
		// 1/18/12: Adds the new QOrientations S0/1_FROM_SL for when XX is SL.
		
		final int XX = qOrientation ;
		
		final int __ = QOrientations.NO ;
		final int S0 = XX == QOrientations.SL ? QOrientations.S0_FROM_SL : QOrientations.S0 ;
		final int S1 = XX == QOrientations.SL ? QOrientations.S1_FROM_SL : QOrientations.S1 ;
		int SS ;
		
		
		// Initialized to silence warnings
		int [][] readableBlocks = {{0}} ;
		
		
		// Allocate readable with rightside-up blocks.
		// Note that many are padded with an extra row at the bottom;
		// this allows a simple hueristic to position them well upon
		// entry to block field.  (hueristic = center {2,1:2} )
		switch( category ) {
		// 3D L.  1 special then 2 standard in a straight line.
		case PieceCatalog.TETRA_CAT_L:
			switch( subCategory ) {
			case 0:
				// Rest are S0
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ XX, S0, S0, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, XX, __, __ },
							{ __, S0, __, __ },
							{ __, S0, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ S0, S0, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, S0, __, __ },
							{ __, S0, __, __ },
							{ __, XX, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
			case 1:
				// Rest are S1
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ S1, S1, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, S1, __, __ },
							{ __, S1, __, __ },
							{ __, XX, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ XX, S1, S1, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, XX, __, __ },
							{ __, S1, __, __ },
							{ __, S1, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
			}
			break ;
			
		// 3D Rect.  There is only one subcategory of this.
		// TODO: Consider 4-state rotation for this category.
		case PieceCatalog.TETRA_CAT_RECT:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				 	{ { __, __, __, __ },
				 	  { __, XX, XX, __ },
				 	  { __, __, __, __ },
				 	  { __, __, __, __ } } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				 	{ { __, XX, __, __ },
				 	  { __, XX, __, __ },
				 	  { __, __, __, __ },
				 	  { __, __, __, __ } } ;
				break ;
			case 2:
				readableBlocks = new int[][]
				 	{ { __, XX, XX, __ },
				 	  { __, __, __, __ },
				 	  { __, __, __, __ },
				 	  { __, __, __, __ } } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				 	{ { __, __, XX, __ },
				 	  { __, __, XX, __ },
				 	  { __, __, __, __ },
				 	  { __, __, __, __ } } ;
				break ;
			}
			break ;
			
		// 3D S.  There is only one subcategory of this.
		case PieceCatalog.TETRA_CAT_S:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { __, __, __, __ },
						{ S0, XX, S1, __ },
						{ __, __, __, __ },
						{ __, __, __, __ } } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { { __, S0, __, __ },
						{ __, XX, __, __ },
						{ __, S1, __, __ },
						{ __, __, __, __ } } ;
				break ;
			case 2:
				readableBlocks = new int[][]
				      { { __, __, __, __ },
						{ S1, XX, S0, __ },
						{ __, __, __, __ },
						{ __, __, __, __ } } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { { __, S1, __, __ },
						{ __, XX, __, __ },
						{ __, S0, __, __ },
						{ __, __, __, __ } } ;
				break ;
			}
			break ;
			
		// 3D T!  A special flanked by standard blocks.  There are
		// two subcategories of this.  This uses 2-state rotation, but
		// that is appropriate for this block type.
		case PieceCatalog.TETRA_CAT_T:
			switch( subCategory ) {
			case 0:
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ S0, XX, S0, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, S0, __, __ },
							{ __, XX, __, __ },
							{ __, S0, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ S0, XX, S0, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, S0, __, __ },
							{ __, XX, __, __ },
							{ __, S0, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
				
			case 1:
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ S1, XX, S1, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, S1, __, __ },
							{ __, XX, __, __ },
							{ __, S1, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, __, __, __ },
							{ S1, XX, S1, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, S1, __, __ },
							{ __, XX, __, __ },
							{ __, S1, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
			}
			break ;
			
		// Branch: a special with 2 adjacent and identical std. blocks: 1 U, 1 R.
		case PieceCatalog.TETRA_CAT_BRANCH:
			switch( subCategory ) {
			case 0:		// U and R are S0
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, S0, __, __ },
							{ __, XX, S0, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, XX, S0, __ },
							{ __, S0, __, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, S0, XX, __ },
							{ __, __, S0, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, __, S0, __ },
							{ __, S0, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
				
			case 1:		// U and R are S1
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, __, S1, __ },
							{ __, S1, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					 	  { { __, S1, __, __ },
					 		{ __, XX, S1, __ },
					 		{ __, __, __, __ },
					 		{ __, __, __, __ } } ;
					 					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, XX, S1, __ },
							{ __, S1, __, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, S1, XX, __ },
							{ __, __, S1, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
			}
			break ;
			
			// Screw.  Same as branch, except the std. blocks are different.
		case PieceCatalog.TETRA_CAT_SCREW:
			switch( subCategory ) {
			case 0:		// U is S0, R is S1
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, S0, __, __ },
							{ __, XX, S1, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, XX, S0, __ },
							{ __, S1, __, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, S1, XX, __ },
							{ __, __, S0, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, __, S1, __ },
							{ __, S0, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
				
			case 1:		// U is S1, R is S0
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, __, S0, __ },
							{ __, S1, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					 	  { { __, S1, __, __ },
					 		{ __, XX, S0, __ },
					 		{ __, __, __, __ },
					 		{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, XX, S1, __ },
							{ __, S0, __, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, S0, XX, __ },
							{ __, __, S1, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
			}
			break ;
			
		// There are 4 subcategories for corner pieces.
		// 0: S0 is used.  Step right then up.
		// 1: S0 is used.  Step left then up.
		case PieceCatalog.TETRA_CAT_CORNER:
			switch( subCategory ) {
			case 0:
			case 2:
				// Step right, then up.
				if ( subCategory == 0 )
					SS = S0 ;
				else
					SS = S1 ;
				
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, __, SS, __ },
							{ __, XX, SS, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, XX, __, __ },
							{ __, SS, SS, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, SS, XX, __ },
							{ __, SS, __, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, SS, SS, __ },
							{ __, __, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
				
			case 1:
			case 3:
				// Step left, then up.
				if ( subCategory == 1 )
					SS = S0 ;
				else
					SS = S1 ;
				
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { { __, SS, __, __ },
							{ __, SS, XX, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 1:
					readableBlocks = new int[][]
					      { { __, SS, SS, __ },
							{ __, XX, __, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 2:
					readableBlocks = new int[][]
					      { { __, XX, SS, __ },
							{ __, __, SS, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				case 3:
					readableBlocks = new int[][]
					      { { __, __, XX, __ },
							{ __, SS, SS, __ },
							{ __, __, __, __ },
							{ __, __, __, __ } } ;
					break ;
				}
				break ;
			}
			break ;
		}
		
		// We have the readable blocks.  Flip, then convert.
		int [][] flippedBlocks = new int[readableBlocks.length][readableBlocks[0].length] ;
		for ( int r = 0; r < readableBlocks.length; r++ ) {
			for ( int c = 0; c < readableBlocks[0].length; c++ ) {
				int fr = readableBlocks.length -r -1 ;
				flippedBlocks[r][c] = readableBlocks[fr][c] ;
			}
		}
		
		// Convert them using QCombinations handy method.  This turns
		// The 2D array into a 3D array with the appropriate values
		// set.
		return QCombinations.expand(flippedBlocks) ;
		
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE STATE
	//
	// These methods provide the implementation of the SerializableState
	// interface.
	//
	// As described above, we only override finalizeConfiguration,
	// leaving the rest to the superclass.
	//
	////////////////////////////////////////////////////////////////
	
	/**
	 * A call to this method transitions the object from "configuration"
	 * phase to "stateful use" phase.  Although this is not programatically
	 * enforced, classes implementing this interface should refuse
	 * (e.g. by throwing an exception) any calls to "stateful use" methods
	 * before this method is called, and likewise, any calls to 
	 * "configuration" methods afterwards.
	 * 
	 * Calls to set or retrieve object state should be considered
	 * "stateful use" - i.e., those methods should be refused if
	 * calls occur before this method.
	 * 
	 * @throws IllegalStateException If called more than once, or
	 * 				before necessary configuration is complete.
	 */
	@Override
	public TetracubeRotationSystem finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		super.finalizeConfiguration() ;		// this call set configured.
		return this ;
	}
}
