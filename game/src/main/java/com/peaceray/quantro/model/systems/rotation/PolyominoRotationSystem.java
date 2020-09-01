package com.peaceray.quantro.model.systems.rotation;

import com.peaceray.quantro.exceptions.InvalidPieceException;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;

public class PolyominoRotationSystem extends TetracubeRotationSystem {

	private byte [][][][][][] preallocatedTrominoesByCategoryQOrientationRotation ;
	private byte [][][][][][] preallocatedPentominoesByCategoryQOrientationRotation ;
	
	public PolyominoRotationSystem(GameInformation ginfo, QInteractions qi, int [] types ) {
		super(ginfo, qi, types);
		
		allocateTrominoes( types ) ;
		allocatePentominoes( types ) ;
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
		p.rotation = 1 ;	// Minimal for all trominoes, pentominoes, tetracubes and tetrominoes
		this.turn0(p) ;
	}
	
	
	public void flipHorizontal( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( PieceCatalog.isTromino(p) ) {
			flipHorizontalTromino( p, o ) ;
		} else if ( PieceCatalog.isPentomino(p) ) {
			flipHorizontalPentomino( p, o ) ;
		} else
			super.flipHorizontal(p, o) ;
	}
	
	public void flipVertical( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( PieceCatalog.isTromino(p) ) {
			flipVerticalTromino( p, o ) ;
		} else if ( PieceCatalog.isPentomino(p) ) {
			flipVerticalPentomino( p, o ) ;
		} else
			super.flipVertical(p, o) ;
	}
	
	
	private void flipHorizontalTromino( Piece p, Offset o ) {
		int cat = PieceCatalog.getTrominoCategory(p) ;
		int qc =  qi.flip( PieceCatalog.getQCombination(p) ) ;
		int rot = p.rotation ;
		switch( cat ) {
		case PieceCatalog.TRO_CAT_LINE:
			// no change except QC.
			p.type = PieceCatalog.encodeTromino( cat, qc ) ;
			turn0(p) ;
			break ;
		case PieceCatalog.TRO_CAT_V:
			// rotation change: 0<->3, 1<->2.
			p.type = PieceCatalog.encodeTromino( cat, qc ) ;
			switch( rot ) {
			case 0:
				rot = 3 ;
				break ;
			case 1:
				rot = 2 ;
				break ;
			case 2:
				rot = 1 ;
				break ;
			case 3:
				rot = 0 ;
				break ;
			}
			p.rotation = rot ;
			turn0(p) ;
			break ;
		}
	}
	
	private void flipVerticalTromino( Piece p, Offset o ) {
		int cat = PieceCatalog.getTrominoCategory(p) ;
		int qc =  qi.flip( PieceCatalog.getQCombination(p) ) ;
		int rot = p.rotation ;
		switch( cat ) {
		case PieceCatalog.TRO_CAT_LINE:
			// no change except QC.
			p.type = PieceCatalog.encodeTromino( cat, qc ) ;
			turn0(p) ;
			break ;
		case PieceCatalog.TRO_CAT_V:
			// rotation change: 0<->1, 2<->3.
			p.type = PieceCatalog.encodeTromino( cat, qc ) ;
			switch( rot ) {
			case 0:
				rot = 1 ;
				break ;
			case 1:
				rot = 0 ;
				break ;
			case 2:
				rot = 3 ;
				break ;
			case 3:
				rot = 2 ;
				break ;
			}
			p.rotation = rot ;
			turn0(p) ;
			break ;
		}
	}
	
	private void flipHorizontalPentomino( Piece p, Offset o ) {
		int cat = PieceCatalog.getPentominoCategory(p) ;
		int qc =  qi.flip( PieceCatalog.getQCombination(p) ) ;
		int rot = p.rotation ;
		switch( cat ) {
		case PieceCatalog.PENTO_CAT_F:
			// A flip converts F to F_REVERSE
			cat = PieceCatalog.PENTO_CAT_F_REVERSE ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_I:
			// no change but qc.
			p.type = PieceCatalog.encodePentomino(cat, qc) ;
			turn0(p) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_L:
			// A flip converts L to L_REVERSE
			cat = PieceCatalog.PENTO_CAT_L_REVERSE ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_N:
			// A flip converts N to N_REVERSE
			cat = PieceCatalog.PENTO_CAT_N_REVERSE ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_P:
			// A flip converts P to P_REVERSE
			cat = PieceCatalog.PENTO_CAT_P_REVERSE ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			// rotations 0 and 2 require a horizontal shift: rightward for 0.
			if ( rot % 2 == 0 ) {
				o.x += rot == 0 ? 1 : -1 ;
			}
			break ;
			
		case PieceCatalog.PENTO_CAT_T:
			// A flip does not change the nature of a T.
			cat = PieceCatalog.PENTO_CAT_T ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_U:
			// A flip does not change the nature of a U.
			cat = PieceCatalog.PENTO_CAT_U ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_V:
			// this is a rot-swap.  0 and 3 have horizontal bars on the bottom.
			rotSwap0312Flip( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_W:
			// this is a rot-swap.  0 and 3 are horiz-flipped.
			rotSwap0312Flip( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_X:
			// no flipping required, other than qc.
			p.type = PieceCatalog.encodePentomino( cat, qc ) ;
			turn0(p) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_Y:
			// A flip converts Y to Y_REVERSE
			cat = PieceCatalog.PENTO_CAT_Y_REVERSE ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_Z:
			// A flip converts Z to Z_REVERSE
			cat = PieceCatalog.PENTO_CAT_Z_REVERSE ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_F_REVERSE:
			// A flip converts F to F_REVERSE
			cat = PieceCatalog.PENTO_CAT_F ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_L_REVERSE:
			// A flip converts L to L_REVERSE
			cat = PieceCatalog.PENTO_CAT_L ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_N_REVERSE:
			// A flip converts N to N_REVERSE
			cat = PieceCatalog.PENTO_CAT_N ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_P_REVERSE:
			// A flip converts P to P_REVERSE
			cat = PieceCatalog.PENTO_CAT_P ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			// rotations 0 and 2 require a horizontal shift: rightward for 2.
			if ( rot % 2 == 0 ) {
				o.x += rot == 0 ? -1 : 1 ;
			}
			break ;
			
		case PieceCatalog.PENTO_CAT_Y_REVERSE:
			// A flip converts Y to Y_REVERSE
			cat = PieceCatalog.PENTO_CAT_Y ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_Z_REVERSE:
			// A flip converts Z to Z_REVERSE
			cat = PieceCatalog.PENTO_CAT_Z ;
			standardFlipHorizontal( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
		}
	}
	
	private void flipVerticalPentomino( Piece p, Offset o ) {
		int cat = PieceCatalog.getPentominoCategory(p) ;
		int qc =  qi.flip( PieceCatalog.getQCombination(p) ) ;
		int rot = p.rotation ;
		switch( cat ) {
		case PieceCatalog.PENTO_CAT_F:
			// A flip converts F to F_REVERSE
			cat = PieceCatalog.PENTO_CAT_F_REVERSE ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_I:
			// no change but qc.
			p.type = PieceCatalog.encodePentomino(cat, qc) ;
			turn0(p) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_L:
			// A flip converts L to L_REVERSE
			cat = PieceCatalog.PENTO_CAT_L_REVERSE ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_N:
			// A flip converts N to N_REVERSE
			cat = PieceCatalog.PENTO_CAT_N_REVERSE ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_P:
			// A flip converts P to P_REVERSE
			cat = PieceCatalog.PENTO_CAT_P_REVERSE ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			// rotations 1 and 3 require a vertical shift: downward for 1.
			if ( rot % 2 == 1 ) {
				o.y += rot == 1 ? -1 : 1 ;
			}
			break ;
			
		case PieceCatalog.PENTO_CAT_T:
			// A flip does not change the nature of a T.
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_U:
			// A flip does not change the nature of a U.
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_V:
			// this is a rot-swap.  0 and 1 have vertical bars on the left.
			rotSwap0123Flip( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_W:
			// this is a rot-swap.  0 and 1 are vert-flipped.
			rotSwap0123Flip( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_X:
			// no flipping required, other than qc.
			p.type = PieceCatalog.encodePentomino( cat, qc ) ;
			turn0(p) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_Y:
			// A flip converts Y to Y_REVERSE
			cat = PieceCatalog.PENTO_CAT_Y_REVERSE ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_Z:
			// A flip converts Z to Z_REVERSE
			cat = PieceCatalog.PENTO_CAT_Z_REVERSE ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_F_REVERSE:
			// A flip converts F to F_REVERSE
			cat = PieceCatalog.PENTO_CAT_F ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_L_REVERSE:
			// A flip converts L to L_REVERSE
			cat = PieceCatalog.PENTO_CAT_L ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_N_REVERSE:
			// A flip converts N to N_REVERSE
			cat = PieceCatalog.PENTO_CAT_N ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_P_REVERSE:
			// A flip converts P to P_REVERSE
			cat = PieceCatalog.PENTO_CAT_P ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			// rotations 1 and 3 require a vertical shift: upward for 1.
			if ( rot % 2 == 1 ) {
				o.y += rot == 1 ? 1 : -1 ;
			}
			break ;
			
		case PieceCatalog.PENTO_CAT_Y_REVERSE:
			// A flip converts Y to Y_REVERSE
			cat = PieceCatalog.PENTO_CAT_Y ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
			
		case PieceCatalog.PENTO_CAT_Z_REVERSE:
			// A flip converts Z to Z_REVERSE
			cat = PieceCatalog.PENTO_CAT_Z ;
			standardFlipVertical( p, PieceCatalog.encodePentomino( cat, qc ) ) ;
			break ;
		}
	}
	
	
	
	
	@Override
	protected byte [][][] getPreallocatedBlocks( Piece p ) throws InvalidPieceException {
		if ( PieceCatalog.isTromino(p.type) ) {
			int cat = PieceCatalog.getTrominoCategory(p.type) ;
			int qo = PieceCatalog.getQCombination(p.type) ;
			int r = p.rotation ;
			
			byte [][][] result ;
			
			try {
				result = preallocatedTrominoesByCategoryQOrientationRotation[cat][qo][r] ;
			} catch ( NullPointerException npe ) {
				System.err.println("PolyominoRotationSystem: In-use allocation of blocks for piece type " + p.type) ;
				allocateTrominoes( new int[]{p.type} ) ;
				result = preallocatedTrominoesByCategoryQOrientationRotation[cat][qo][r] ;
			}
			
			// Return the preallocated blocks for this tetromino category,
			// orientation and rotation.
			return result ;
		} else if ( PieceCatalog.isPentomino(p.type) ) {
			int cat = PieceCatalog.getPentominoCategory(p.type) ;
			int qo = PieceCatalog.getQCombination(p.type) ;
			int r = p.rotation ;
			
			byte [][][] result ;
			
			try {
				result = preallocatedPentominoesByCategoryQOrientationRotation[cat][qo][r] ;
			} catch ( NullPointerException npe ) {
				System.err.println("PolyominoRotationSystem: In-use allocation of blocks for piece type " + p.type) ;
				allocatePentominoes( new int[]{p.type} ) ;
				result = preallocatedPentominoesByCategoryQOrientationRotation[cat][qo][r] ;
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
		return PieceCatalog.isTetromino(type) || PieceCatalog.isTetracube(type) || PieceCatalog.isTromino(type) || PieceCatalog.isPentomino(type) ;
	}
	
	@Override
	public boolean supportsPiece( Piece p ) {
		return this.supportsPiece(p.type) ;
	}
	
	
	// All turns are handled by the subclass; the methods 
	// call "getPreallocatedBlocks"
	
	private void allocateTrominoes( int [] types ) {
		if ( preallocatedTrominoesByCategoryQOrientationRotation == null )
			preallocatedTrominoesByCategoryQOrientationRotation
					= new byte[PieceCatalog.NUMBER_TROMINO_CATEGORIES][][][][][] ;
		
		byte [][][][][][] prealloc = preallocatedTrominoesByCategoryQOrientationRotation ;
		
		// allocate the blocks for every tromino in the provided list.
		for ( int t = 0; t < types.length; t++ ) {
			int type = types[t] ;
			if ( PieceCatalog.isTromino(type) ) {
				int c = PieceCatalog.getTrominoCategory(type) ;
				int qc = PieceCatalog.getQCombination(type) ;
				
				// does this need allocation?
				if ( prealloc[c] == null )
					prealloc[c] = new byte[ QCombinations.NUM ][][][][] ;
				if ( prealloc[c][qc] == null ) {
					prealloc[c][qc] = new byte[4][][][] ;
					for ( int r = 0; r < 4; r++ )
						// Call another function
						prealloc[c][qc][r] = allocateTroBlocks(c,qc,r) ;
				}
			}
		}
	}
	
	
	private void allocatePentominoes( int [] types ) {
		if ( preallocatedPentominoesByCategoryQOrientationRotation == null )
			preallocatedPentominoesByCategoryQOrientationRotation
					= new byte[PieceCatalog.NUMBER_PENTOMINO_CATEGORIES][][][][][] ;
		
		byte [][][][][][] prealloc = preallocatedPentominoesByCategoryQOrientationRotation ;
		
		// allocate the blocks for every pentomino in the provided list.
		for ( int t = 0; t < types.length; t++ ) {
			int type = types[t] ;
			if ( PieceCatalog.isPentomino(type) ) {
				int c = PieceCatalog.getPentominoCategory(type) ;
				int qc = PieceCatalog.getQCombination(type) ;
				
				// does this need allocation?
				if ( prealloc[c] == null )
					prealloc[c] = new byte[ QCombinations.NUM ][][][][] ;
				if ( prealloc[c][qc] == null ) {
					prealloc[c][qc] = new byte[4][][][] ;
					for ( int r = 0; r < 4; r++ )
						// Call another function
						prealloc[c][qc][r] = allocatePentoBlocks(c,qc,r) ;
				}
			}
		}
	}
	
	
	private static byte [][][] allocateTroBlocks( int category, int qOrientation, int rotation ) {
		// To make things more human-readable, this first allocates a
		// 2D array with the qOrientation listed as the QCombination.
		// Then it uses QCombinations to fill it in.
		
		// Actually, we do this is three steps.  First we select a 
		// 0, QOrientation block array that is represented in a
		// human-readable way.  Then we allocate a vertically-flipped copy.
		// Finally we convert the result.
		
		// This long, complex process has one intended side-effect:
		// the tromino represented is visually shown in the code below,
		// and can be easily modified if desired.  The vertical flip
		// is necessary since by convention row 0 is at the bottom,
		// whereas these pictures necessarily number rows from the top.
		
		final int o = QOrientations.NO ;
		final int X = qOrientation ;
		
		// Initialized to silence warnings
		int [][] readableBlocks = {{0}} ;
		
		
		// Allocate readable with rightside-up blocks.
		// Note that many are padded with an extra row at the bottom;
		// this allows a simple hueristic to position them well upon
		// entry to block field.  (hueristic = center {2,1:2} )
		switch( category ) {
		// Line piece.  They rotate around the middle, for now.
		case PieceCatalog.TRO_CAT_LINE:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { o, o, o },
						{ X, X, X },
						{ o, o, o } } ;
				break ;
			case 1:
				readableBlocks = new int[][]
 				      { { o, X, o },
 						{ o, X, o },
 						{ o, X, o } } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
 				      { { o, o, o },
 						{ X, X, X },
 						{ o, o, o } } ;
 				break ;
			case 3:
				readableBlocks = new int[][]
  				      { { o, X, o },
  						{ o, X, o },
  						{ o, X, o } } ;
  				break ;
			}
			break ;
			
		// Gamma pieces are shaped like backwards 7s.
		case PieceCatalog.TRO_CAT_V:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { o, X, o },
						{ o, X, X },
						{ o, o, o } } ;
				break ;
			case 1:
				readableBlocks = new int[][]
 				      { { o, X, X },
 						{ o, X, o },
 						{ o, o, o } } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
 				      { { o, X, X },
 						{ o, o, X },
 						{ o, o, o } } ;
 				break ;
			case 3:
				readableBlocks = new int[][]
  				      { { o, o, X },
  						{ o, X, X },
  						{ o, o, o } } ;
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
	
	
	
	private static byte [][][] allocatePentoBlocks( int category, int qOrientation, int rotation ) {
		// To make things more human-readable, this first allocates a
		// 2D array with the qOrientation listed as the QCombination.
		// Then it uses QCombinations to fill it in.
		
		// Actually, we do this is three steps.  First we select a 
		// 0, QOrientation block array that is represented in a
		// human-readable way.  Then we allocate a vertically-flipped copy.
		// Finally we convert the result.
		
		// This long, complex process has one intended side-effect:
		// the pentomino represented is visually shown in the code below,
		// and can be easily modified if desired.  The vertical flip
		// is necessary since by convention row 0 is at the bottom,
		// whereas these pictures necessarily number rows from the top.
		
		final int o = QOrientations.NO ;
		final int X = qOrientation ;
		
		// Initialized to silence warnings
		int [][] readableBlocks = {{0}} ;
		
		
		// Allocate readable with rightside-up blocks.
		switch( category ) {
		// F pieces.  They rotate around the center block.
		case PieceCatalog.PENTO_CAT_F :
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { X, o, o},
						{ X, X, X },
						{o, X, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, X },
						{ X, X, o},
						{o, X, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
                      { {o, X, o},
						{ X, X, X },
						{o, o, X } } ;
 				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, X, o},
						{o, X, X },
						{ X, X, o} } ;
  				break ;
			}
			break ;
			
		// I pieces rotate around their center block.
		case PieceCatalog.PENTO_CAT_I:
			switch( rotation ) {
			case 0:
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o, o},
						{o, o, o, o, o},
						{ X, X, X, X, X },
						{o, o, o, o, o},
						{o, o, o, o, o} } ;
				break ;
			case 1:
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X, o, o},
						{o, o, X, o, o},
						{o, o, X, o, o},
						{o, o, X, o, o},
						{o, o, X, o, o} } ;
 				break ;
			}
			break ;
			
		// Ls rotate around a slightly offset bounding box center.
		case PieceCatalog.PENTO_CAT_L:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, o, o, X },
						{ X, X, X, X },
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, o, o},
						{o, X, o, o},
						{o, X, o, o},
						{o, X, X, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, X, X, X },
						{ X, o, o, o},
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, X, X, o},
						{o, o, X, o},
						{o, o, X, o},
						{o, o, X, o} } ;
 				break ;
			}
			break ;
			
			
		// Ns rotate around the "center" of the bounding box.
		case PieceCatalog.PENTO_CAT_N:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, X, o, o},
						{o, X, X, X },
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, o, X, o},
						{o, X, X, o},
						{o, X, o, o},
						{o, X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, X, X, o},
						{o, o, X, X },
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X, o},
						{o, o, X, o},
						{o, X, X, o},
						{o, X, o, o} } ;
 				break ;
			}
			break ;
			
		
		// Ps are a little unusual, because there is no clear center
		// for the bounding box.  Instead, we rotate around the center
		// of the square.
		case PieceCatalog.PENTO_CAT_P:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, X, o},
						{o, X, X, X },
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, X, o},
						{o, X, X, o},
						{o, X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, X, X, o},
						{o, X, X, o},
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X, o},
						{o, X, X, o},
						{o, X, X, o},
						{o, o, o, o} } ;
 				break ;
			}
			break ;
			
		// Ts rotate differently than their tetromino brethren.  4-minoes
		// have no bounding box center, so we align with the "heaviest"
		// part of the box.  This T, by virtue of its extended leg, can
		// smoothly rotate inside a 3x3 bounding box around its own center block.
		case PieceCatalog.PENTO_CAT_T:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, X, o},
						{o, X, o},
						{ X, X, X } } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { { X, o, o},
						{ X, X, X },
						{ X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { { X, X, X },
						{o, X, o},
						{o, X, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X },
						{ X, X, X },
						{o, o, X } } ;
 				break ;
			}
			break ;
				
			
		// Us rotate around the center block, very similar to the way Ts work.
		case PieceCatalog.PENTO_CAT_U:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { X, o, X },
						{ X, X, X },
						{o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, X },
						{o, X, o},
						{o, X, X } } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o},
						{ X, X, X },
						{ X, o, X } } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { { X, X, o},
						{o, X, o},
						{ X, X, o} } ;
 				break ;
			}
			break ;
			
		
		// Vs rotate around the center of the bounding box, which - like
		// corners - is outside the bounds of the shape.
		case PieceCatalog.PENTO_CAT_V:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { X, o, o},
						{ X, o, o},
						{ X, X, X } } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { { X, X, X },
						{ X, o, o},
						{ X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { { X, X, X },
						{o, o, X },
						{o, o, X } } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X },
						{o, o, X },
						{ X, X, X } } ;
 				break ;
			}
			break ;
			
		
		// Ws rotate around the center of the bounding box, which is also
		// the center block.  We match this to V's rotation so swapping the
		// two has an intuitive result.
		case PieceCatalog.PENTO_CAT_W:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { X, o, o},
						{ X, X, o},
						{o, X, X } } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, X },
						{ X, X, o},
						{ X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { { X, X, o},
						{o, X, X },
						{o, o, X } } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X },
						{o, X, X },
						{ X, X, o} } ;
 				break ;
			}
			break ;
			
		
		// Xs don't rotate at all.
		case PieceCatalog.PENTO_CAT_X:
			switch( rotation ) {
			case 0:
			case 1:
			case 2:
			case 3:
				readableBlocks = new int[][]
				      { {o, X, o},
						{ X, X, X },
						{o, X, o} } ;
				break ;
			}
			break ;
			
		
		// Ys rotate very similarly to Ls, Ns, etc.
		case PieceCatalog.PENTO_CAT_Y:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, o, X, o},
						{ X, X, X, X },
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, o, o},
						{o, X, o, o},
						{o, X, X, o},
						{o, X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, X, X, X },
						{o, X, o, o},
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X, o},
						{o, X, X, o},
						{o, o, X, o},
						{o, o, X, o} } ;
 				break ;
			
			}
			break ;
			
		
		// Zs!  Repeat every other rotation state.  Unlike squigglies (TETRO_S, TETRO_Z)
		// there is no need to modify rotation; there is a clear center to rotate about.
		case PieceCatalog.PENTO_CAT_Z:
			switch( rotation ) {
			case 0:
			case 2:
				readableBlocks = new int[][]
				      { {o, o, X },
						{ X, X, X },
						{ X, o, o} } ;
				break ;
			case 1:
			case 3:
				readableBlocks = new int[][]
				      { { X, X, o},
						{o, X, o},
						{o, X, X } } ;
 				break ;
			}
			break ;
		
			
		// F pieces.  They rotate around the center block.
		case PieceCatalog.PENTO_CAT_F_REVERSE :
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, X },
						{ X, X, X },
						{o, X, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, o},
						{ X, X, o},
						{o, X, X } } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
                      { {o, X, o},
						{ X, X, X },
						{ X, o, o} } ;
 				break ;
			case 3:
				readableBlocks = new int[][]
				      { { X, X, o},
						{o, X, X },
						{o, X, o} } ;
  				break ;
			}
			break ;
			
		
			
		// Ls rotate around a slightly offset bounding box center.
		case PieceCatalog.PENTO_CAT_L_REVERSE:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, o, o, o},
						{ X, X, X, X },
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, X, o},
						{o, X, o, o},
						{o, X, o, o},
						{o, X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, X, X, X },
						{o, o, o, X },
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X, o},
						{o, o, X, o},
						{o, o, X, o},
						{o, X, X, o} } ;
 				break ;
			}
			break ;
			
			
		// Ns rotate around the "center" of the bounding box.
		case PieceCatalog.PENTO_CAT_N_REVERSE:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, o, X, X },
						{ X, X, X, o},
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, o, o},
						{o, X, o, o},
						{o, X, X, o},
						{o, o, X, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, X, X },
						{ X, X, o, o},
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, X, o, o},
						{o, X, X, o},
						{o, o, X, o},
						{o, o, X, o} } ;
 				break ;
			}
			break ;
			
		
		// Ps are a little unusual, because there is no clear center
		// for the bounding box.  Instead, we rotate around the center
		// of the square.
		case PieceCatalog.PENTO_CAT_P_REVERSE:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, X, o},
						{ X, X, X, o},
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, o, o},
						{o, X, X, o},
						{o, X, X, o},
						{o, o, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, X, X },
						{o, X, X, o},
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, X, o},
						{o, X, X, o},
						{o, o, X, o} } ;
 				break ;
			}
			break ;
			
		
		
		// Ys rotate very similarly to Ls, Ns, etc.
		case PieceCatalog.PENTO_CAT_Y_REVERSE:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, o, o},
						{ X, X, X, X },
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
				      { {o, X, o, o},
						{o, X, X, o},
						{o, X, o, o},
						{o, X, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{ X, X, X, X },
						{o, o, X, o},
						{o, o, o, o} } ;
				break ;
			case 3:
				readableBlocks = new int[][]
				      { {o, o, X, o},
						{o, o, X, o},
						{o, X, X, o},
						{o, o, X, o} } ;
 				break ;
			
			}
			break ;
			
		
		// Zs!  Repeat every other rotation state.  Unlike squigglies (TETRO_S, TETRO_Z)
		// there is no need to modify rotation; there is a clear center to rotate about.
		case PieceCatalog.PENTO_CAT_Z_REVERSE:
			switch( rotation ) {
			case 0:
			case 2:
				readableBlocks = new int[][]
				      { { X, o, o},
						{ X, X, X },
						{o, o, X } } ;
				break ;
			case 1:
			case 3:
				readableBlocks = new int[][]
				      { {o, X, X },
						{o, X, o},
						{ X, X, o} } ;
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
	
	
}
