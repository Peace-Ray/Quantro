package com.peaceray.quantro.model.systems.rotation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.peaceray.quantro.exceptions.InvalidPieceException;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.state.VersionedState;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;

public class TetrominoRotationSystem extends RotationSystem {
	
	//private static final String TAG = "TetrominoRotationSystem" ;
	
	// info
	GameInformation ginfo ;
	QInteractions qi ;
	
	private byte [][][][][][] preallocatedTetrominoesByCategoryQOrientationRotation ;
	
	// This system does not maintain a "state": behavior is set
	// entirely by method parameters and the QInteractions object
	// provided.
	// Nevertheless, for completion's sake, we implement an empty state object.
	protected VersionedState state ;
	protected boolean configured ;
	
	public TetrominoRotationSystem(GameInformation ginfo, QInteractions qi, int [] types) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		state = new VersionedState( DEFAULT_VERSION ) ;
		configured = false ;
		
		// preallocate the tetrominos we will need.
		// For simplicity, use all of them - one for each QOrientation.
		allocateTetrominoes(types) ;
	}
	
	/**
	 * getGameInformation: returns a reference to the particular GameInformation
	 * instance used by this RotationSystem.
	 * 
	 * @return			An instance of GameInformation
	 */
	public GameInformation getGameInformation() {
		return ginfo ;
	}
	
	
	/**
	 * getQInteractions: Flips are determined using a QInteractions
	 * implementation.  This gets the implementation used for this system.
	 * 
	 * @return		An instance of QInteractions
	 */
	public QInteractions getQInteractions() {
		return qi ;
	}
	
	
	protected byte [][][] getPreallocatedBlocks( Piece p ) throws InvalidPieceException {
		int cat = PieceCatalog.getTetrominoCategory(p.type) ;
		int qo = PieceCatalog.getQCombination(p.type) ;
		int r = p.rotation ;
		
		byte [][][] result ;
		
		// Return the preallocated blocks for this tetromino category,
		// orientation and rotation.
		try {
			result = preallocatedTetrominoesByCategoryQOrientationRotation[cat][qo][r] ;
		} catch( NullPointerException npe ) {
			System.err.println("TetrominoRotationSystem: In-use allocation of blocks for piece type " + p.type) ;
			allocateTetrominoes( new int[]{p.type} ) ;
			result = preallocatedTetrominoesByCategoryQOrientationRotation[cat][qo][r] ;
		}
		return result ;
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

	
	// Does this rotation system support the specified piece type?
	public boolean supportsPiece( int type ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return PieceCatalog.isTetromino(type) ;
	}
	
	public boolean supportsPiece( Piece p ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return PieceCatalog.isTetromino(p.type) ;
	}
	
	// Here are the rotations available.
	// Left and right:
	public void turnCW( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Prepare for the rotation by noting current state,
		// then perform (for rotation-related fields)
		p.previousRotation = p.rotation ;
		p.rotationDirection = 1 ;		// Clockwise is "positive"
		p.rotation = (p.rotation + 1) % 4 ;
		
		// Set the piece's blocks array to the preallocated array for
		// piece type, then set its bounds naively (using the size of
		// the blocks array).
		p.blocks = getPreallocatedBlocks( p ) ;
		p.setBounds();
	}
	
	public void turnCCW( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Prepare for the rotation by noting current state,
		// then perform (for rotation-related fields)
		p.previousRotation = p.rotation ;
		p.rotationDirection = -1 ;		// Counterclockwise is "negative"
		// Make sure we don't go under 0 by looping to 3
		p.rotation = p.rotation > 0 ? p.rotation-1 : 3 ;
		
		// Set the piece's blocks array to the preallocated array for
		// piece type, then set its bounds naively (using the size of
		// the blocks array).
		p.blocks = getPreallocatedBlocks( p ) ;
		p.setBounds();
	}
	
	// 180 degrees
	public void turnCW180( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Turning 180 is simple enough.  Note the direction and previous
		// rotation, then update.
		p.previousRotation = p.rotation ;
		p.rotationDirection = 1 ;		// Clockwise is "positive"
		// +=2 and mod to turn 180 and not exceed 4.
		p.rotation = (p.rotation + 2) % 4 ;
		
		// Set the piece's locks and then naively set its bounds.
		p.blocks = getPreallocatedBlocks( p ) ;
		p.setBounds();
	}
	
	
	public void turnCCW180( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		// Turning 180 counter clockwise is so easy a caveman could do it.
		// Turning 180 is simple enough.  Note the direction and previous
		// rotation, then update.
		p.previousRotation = p.rotation ;
		p.rotationDirection = -1 ;		// Counterclockwise is "negative"
		// +=2 and mod to turn 180 and not exceed 4.
		p.rotation = (p.rotation + 2) % 4 ;
		
		// Set the piece's locks and then naively set its bounds.
		p.blocks = getPreallocatedBlocks( p ) ;
		p.setBounds();
	}
	
	// 0 turn - does nothing, but may be useful.
	// For instance, a rotation system can set
	// the block array based on piece type.
	public void turn0( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		p.blocks = getPreallocatedBlocks( p ) ;
		p.setBounds() ;
	}
	
	public void undoTurn( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		p.rotation = p.previousRotation ;
		
		p.blocks = getPreallocatedBlocks( p ) ;
		p.setBounds();
	}
	
	
	public void turnMinimumHorizontalProfile( Piece p ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		p.rotation = 1 ;	// Minimal for all tetrominos!
		this.turn0(p) ;
	}
	
	
	
	public void flipHorizontal( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( !PieceCatalog.isTetromino(p) )
			throw new InvalidPieceException( p.type, "Can't flip this piece!" ) ;
		
		// Flips this piece horizontally.  This might change its rotation
		// and/or convert it to a different piece.  Certain QCombinations also
		// flip themselves.
		int cat = PieceCatalog.getTetrominoCategory(p) ;
		int qc =  qi.flip( PieceCatalog.getQCombination(p) ) ;
		int rot = p.rotation ;
		switch( cat ) {
		case PieceCatalog.TETRO_CAT_LINE:
			// no change except QC.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			turn0(p) ;
			break ;
		case PieceCatalog.TETRO_CAT_GAMMA:
			// GAMMA is a backwards-7.  A horizontal flip transforms
			// it into a GUN.  If rotated at 0 or 2, no rotation or offset change.
			// Otherwise, take the opposite rotation AND shift this piece horizontally.
			// If rotation is 1, shift right.  If rotation is 3, shift left.
			standardFlipHorizontal( p, PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_GUN, qc ) ) ;
			if ( rot % 2 == 1 ) {
				o.x += rot == 1 ? 1 : -1 ;
			}
			break ;
		case PieceCatalog.TETRO_CAT_GUN:
			// GUN is a 7.  A horizontal flip transforms
			// it into a GAMMA.  If rotated at 0 or 2, no rotation or offset change.
			// Otherwise, take the opposite rotation AND shift this piece horizontally.
			// If rotation is 1, shift right.  If rotation is 3, shift left.
			standardFlipHorizontal( p, PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_GAMMA, qc ) ) ;
			if ( rot % 2 == 1 ) {
				o.x += rot == 1 ? 1 : -1 ;
			}
			break ;
		case PieceCatalog.TETRO_CAT_SQUARE:
			// no change except QC.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			turn0(p) ;
			break ;
		case PieceCatalog.TETRO_CAT_S:
			// S is the opposite of Z.
			cat = PieceCatalog.TETRO_CAT_Z ;
			// if rotation is 0 or 2, only change the type, not rotation.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			if ( rot % 2 == 1 ) {
				// shift one left to match S position.
				o.x -= 1 ;
			}
			turn0(p) ;
			break ;
		case PieceCatalog.TETRO_CAT_T:
			// If 1 or 3, change to the other rotation and shift
			// horizontally.  Otherwise, no change.
			standardFlipHorizontal( p, PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_T, qc ) ) ;
			if ( rot % 2 == 1 ) {
				o.x += rot == 1 ? 1 : -1 ;
			}
			break ;
		case PieceCatalog.TETRO_CAT_Z:
			// Z is the opposite of S.
			cat = PieceCatalog.TETRO_CAT_S ;
			// if rotation is 0 or 2, only change the type, not rotation.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			if ( rot % 2 == 1 ) {
				// shift one right to match Z position.
				o.x += 1 ;
			}
			turn0(p) ;
			break ;
		}
	}
	
	
	public void flipVertical( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( !PieceCatalog.isTetromino(p) )
			throw new InvalidPieceException( p.type, "Can't flip this piece!" ) ;
		
		// Flips this piece horizontally.  This might change its rotation
		// and/or convert it to a different piece.
		int cat = PieceCatalog.getTetrominoCategory(p) ;
		int qc = qi.flip( PieceCatalog.getQCombination(p) ) ;
		int rot = p.rotation ;
		switch( cat ) {
		case PieceCatalog.TETRO_CAT_LINE:
			// no change except QC.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			turn0(p) ;
			break ;
		case PieceCatalog.TETRO_CAT_GAMMA:
			// GAMMA is a backwards-7.  A vertical flip transforms
			// it into a GUN.  If rotation 1 or 3, the rotation
			// and position is retained.  Otherwise, we rotate 180
			// and shift up (for 0) or down (for 2).
			standardFlipVertical( p, PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_GUN, qc ) ) ;
			if ( rot % 2 == 0 ) {
				o.y += rot == 0 ? 1 : -1 ;
			}
			break ;
		case PieceCatalog.TETRO_CAT_GUN:
			// GUN is a 7.  A vertical flip transforms
			// it into a GAMMA.  If rotation 1 or 3, the rotation
			// and position is retained.  Otherwise, we rotate 180
			// and shift up (for 0) or down (for 2).
			standardFlipVertical( p, PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_GAMMA, qc ) ) ;
			if ( rot % 2 == 0 ) {
				o.y += rot == 0 ? 1 : -1 ;
			}
			break ;
		case PieceCatalog.TETRO_CAT_SQUARE:
			// no change except QC.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			turn0(p) ;
			break ;
		case PieceCatalog.TETRO_CAT_S:
			// S is the opposite of Z.
			cat = PieceCatalog.TETRO_CAT_Z ;
			// if rotation is 0 or 2, only change the type, not rotation.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			if ( rot % 2 == 1 ) {
				// shift one left to match S position.
				o.x -= 1 ;
			}
			turn0(p) ;
			break ;
		case PieceCatalog.TETRO_CAT_T:
			// If 0 or 2, change to the other rotation and shift
			// horizontally.  Otherwise, no change.
			standardFlipVertical( p, PieceCatalog.encodeTetromino( PieceCatalog.TETRO_CAT_T, qc ) ) ;
			if ( rot % 2 == 0 ) {
				o.y += rot == 0 ? 1 : -1 ;
			}
			break ;
		case PieceCatalog.TETRO_CAT_Z:
			// Z is the opposite of S.
			cat = PieceCatalog.TETRO_CAT_S ;
			// if rotation is 0 or 2, only change the type, not rotation.
			p.type = PieceCatalog.encodeTetromino( cat, qc ) ;
			if ( rot % 2 == 1 ) {
				// shift one right to match Z position.
				o.x += 1 ;
			}
			turn0(p) ;
			break ;
		}
	}
	
	
	/**
	 * Performs the nitty-gritty of a standard horizontal flip.
	 * 
	 * You are responsible for encoding the appropriate "flipped"
	 * piece type, which will be applied here.  This method will perform
	 * standard rotation to align with a horizontal flip.
	 * 
	 * Assumptions: the new piece type represents a horizontal flip
	 * of the previous, for rotations 0 and 2.  However, for rotations
	 * 1 and 3, the result is a 180-turn of the appropriate piece.
	 * 
	 * @param p
	 * @param flippedType
	 */
	protected void standardFlipHorizontal( Piece p, int flippedType ) {
		p.type = flippedType ;
		if ( p.rotation % 2 == 1 ) {
			if ( p.rotationDirection < 0 )
				turnCCW180(p) ;
			else
				turnCW180(p) ;
		} else
			turn0(p) ;
	}
	
	/**
	 * Performs the nitty-gritty of a rot-swapped flip.
	 * 
	 * You are responsible for encoding the appropriate "flipped"
	 * piece type, which will be applied here.  This method will perform
	 * rotation-substitution with the new place type.
	 * 
	 * Assumptions: the new piece type represents the 'flipped' result,
	 * which must then have rotations changed as:
	 * 
	 * 0 <--> 3
	 * 1 <--> 2
	 * 
	 * @param p
	 * @param flippedType
	 */
	protected void rotSwap0312Flip( Piece p, int flippedType ) {
		p.type = flippedType ;
		switch( p.rotation ) {
		case 0:
			p.rotation = 3 ;
			break ;
		case 1:
			p.rotation = 2 ;
			break ;
		case 2:
			p.rotation = 1 ;
			break ;
		case 3:
			p.rotation = 0 ;
			break ;
		}
		turn0(p) ;
	}
	
	/**
	 * Performs the nitty-gritty of a rot-swapped flip.
	 * 
	 * You are responsible for encoding the appropriate "flipped"
	 * piece type, which will be applied here.  This method will perform
	 * rotation-substitution with the new place type.
	 * 
	 * Assumptions: the new piece type represents the 'flipped' result,
	 * which must then have rotations changed as:
	 * 
	 * 0 <--> 1
	 * 2 <--> 3
	 * 
	 * @param p
	 * @param flippedType
	 */
	protected void rotSwap0123Flip( Piece p, int flippedType ) {
		p.type = flippedType ;
		switch( p.rotation ) {
		case 0:
			p.rotation = 1 ;
			break ;
		case 1:
			p.rotation = 0 ;
			break ;
		case 2:
			p.rotation = 3 ;
			break ;
		case 3:
			p.rotation = 2 ;
			break ;
		}
		turn0(p) ;
	}
	
	
	
	
	/**
	 * Performs the nitty-gritty of a standard vertical flip.
	 * 
	 * You are responsible for encoding the appropriate "flipped"
	 * piece type, which will be applied here.  This method will perform
	 * standard rotation to align with a vertical flip.
	 * 
	 * Assumptions: the new piece type represents a vertical flip
	 * of the previous, for rotations 1 and 3.  However, for rotations
	 * 0 and 2, the result is a 180-turn of the appropriate piece.
	 * 
	 * @param p
	 * @param flippedType
	 */
	protected void standardFlipVertical( Piece p, int flippedType ) {
		p.type = flippedType ;
		if ( p.rotation % 2 == 0 ) {
			if ( p.rotationDirection < 0 )
				turnCCW180(p) ;
			else
				turnCW180(p) ;
		} else
			turn0(p) ;
	}
	
	
	
	
	
	private void allocateTetrominoes(int [] types) {
		// Allocate the array for the number of categories...
		if ( preallocatedTetrominoesByCategoryQOrientationRotation == null )
			preallocatedTetrominoesByCategoryQOrientationRotation = new byte[PieceCatalog.NUMBER_TETROMINO_CATEGORIES][][][][][] ;
		
		byte [][][][][][] prealloc = preallocatedTetrominoesByCategoryQOrientationRotation ;
		
		// 'types' lists exactly those pieces types which we expect to
		// see within the game.  We allocate those which are tetrominoes.
		for ( int t = 0; t < types.length; t++ ) {
			int type = types[t] ;
			if ( PieceCatalog.isTetromino(type) ) {
				int cat = PieceCatalog.getTetrominoCategory(type) ;
				int qo = PieceCatalog.getQCombination(type) ;
				if ( prealloc[cat] == null )
					prealloc[cat] = new byte[QOrientations.NUM][][][][] ;
				
				if ( prealloc[cat][qo] == null ) {
					prealloc[cat][qo] = new byte[4][][][] ;
					for ( int r = 0; r < 4; r++ ) {
						// Call another function to create the blocks.
						prealloc[cat][qo][r] = allocateTetroBlocks(cat, qo, r) ;
					}
				}
			}
		}
	}
	
	/**
	 * Reallocates all tetrominoes (that have been previously allocated).
	 * Useful for cases where the versioned state has changed versions,
	 * such as when a game from a previous version is loaded; this prevents
	 * saved games from having pieces "jump" to new positions upon loading.
	 * 
	 * Has no effect if allocateTetrominoes was not called.
	 */
	private void reallocateTetrominoes() {
		byte [][][][][][] prealloc = preallocatedTetrominoesByCategoryQOrientationRotation ;
		if ( prealloc == null )
			return ;
		
		for ( int cat = 0; cat < PieceCatalog.NUMBER_TETROMINO_CATEGORIES; cat++ ) {
			for ( int qo = 0; qo < QCombinations.NUM; qo++ ) {
				if ( prealloc[cat][qo] != null ) {
					// re-allocate this!
					prealloc[cat][qo] = new byte[4][][][] ;
					for ( int r = 0; r < 4; r++ ) {
						// Call another function to create the blocks.
						prealloc[cat][qo][r] = allocateTetroBlocks(cat, qo, r) ;
					}
				}
			}
		}
	}
	
	
	
	private byte [][][] allocateTetroBlocks( int category, int qOrientation, int rotation ) {
		// To make things more human-readable, this first allocates a
		// 2D array with the qOrientation listed as the QCombination.
		// Then it uses QCombinations to fill it in.
		
		// Actually, we do this is three steps.  First we select a 
		// 0, QOrientation block array that is represented in a
		// human-readable way.  Then we allocate a vertically-flipped copy.
		// Finally we convert the result.
		
		// This long, complex process has one intended side-effect:
		// the tetromino represented is visually shown in the code below,
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
		// TODO: Consider changing this to 2-state rotation.
		// Note that kicks will need to be tweaked as well...
		case PieceCatalog.TETRO_CAT_LINE:
			if ( state.version == VERSION_LINE_PIECE_FLIP ) {
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
	 				      { {o, o, o, o},
	 						{o, o, o, o},
	 						{ X, X, X, X },
	 						{o, o, o, o} } ;
	 				break ;
				case 1:
					readableBlocks = new int[][]
	  				      { {o, X, o, o},
	  						{o, X, o, o},
	  						{o, X, o, o},
	  						{o, X, o, o} } ;
	  				break ;
				case 2:
					readableBlocks = new int[][]
					      { {o, o, o, o},
							{ X, X, X, X },
							{o, o, o, o},
							{o, o, o, o} } ;
					break ;
				case 3:
					readableBlocks = new int[][]
	 				      { {o, o, X, o},
	 						{o, o, X, o},
	 						{o, o, X, o},
	 						{o, o, X, o} } ;
	 				break ;
				}
			} else {
				switch( rotation ) {
				case 0:
					readableBlocks = new int[][]
					      { {o, o, o, o},
							{ X, X, X, X },
							{o, o, o, o},
							{o, o, o, o} } ;
					break ;
				case 1:
					readableBlocks = new int[][]
	 				      { {o, o, X, o},
	 						{o, o, X, o},
	 						{o, o, X, o},
	 						{o, o, X, o} } ;
	 				break ;
				case 2:
					readableBlocks = new int[][]
	 				      { {o, o, o, o},
	 						{o, o, o, o},
	 						{ X, X, X, X },
	 						{o, o, o, o} } ;
	 				break ;
				case 3:
					readableBlocks = new int[][]
	  				      { {o, X, o, o},
	  						{o, X, o, o},
	  						{o, X, o, o},
	  						{o, X, o, o} } ;
	  				break ;
				}
			}
			break ;
			
		// Gamma pieces are shaped like backwards 7s.
		case PieceCatalog.TETRO_CAT_GAMMA:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { { X, o, o, o},
						{ X, X, X, o},
						{o, o, o, o},
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
 				      { {o, X, X, o},
 						{o, X, o, o},
 						{o, X, o, o},
						{o, o, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
 				      { {o, o, o, o},
 						{ X, X, X, o},
 						{o, o, X, o},
						{o, o, o, o} } ;
 				break ;
			case 3:
				readableBlocks = new int[][]
  				      { {o, X, o, o},
  						{o, X, o, o},
  						{ X, X, o, o},
						{o, o, o, o} } ;
  				break ;
			}
			break ;
			
		// Gun pieces are shaped like normalwards 7s!
		case PieceCatalog.TETRO_CAT_GUN:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, o, X, o},
						{ X, X, X, o},
						{o, o, o, o},
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
 				      { {o, X, o, o},
 						{o, X, o, o},
 						{o, X, X, o},
						{o, o, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
 				      { {o, o, o, o},
 						{ X, X, X, o},
 						{ X, o, o, o},
						{o, o, o, o} } ;
 				break ;
			case 3:
				readableBlocks = new int[][]
  				      { { X, X, o, o},
  						{o, X, o, o},
  						{o, X, o, o},
						{o, o, o, o} } ;
  				break ;
			}
			break ;
			
			
		// Square pieces don't rotate.
		case PieceCatalog.TETRO_CAT_SQUARE:
			switch( rotation ) {
			case 0:
			case 1:
			case 2:
			case 3:
				readableBlocks = new int[][]
				      { {o, X, X, o},
						{o, X, X, o},
						{o, o, o, o},
						{o, o, o, o} } ;
				break ;
			}
			break ;
			
			
		// Reverse squigglies look like Esses!  Also,
		// they have only 2 rotation states.  I found the SRS
		// to be unnecessarily complex in terms of setting up
		// kicks.  2-state rotation may limit the options for
		// kicks, but is much more straightforward.  There's
		// still plenty of depth available with piece direction
		// buttons and such (hold a direction, then rotate).
		case PieceCatalog.TETRO_CAT_S:
			switch( rotation ) {
			case 0:
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
						{o, X, X, o},
						{ X, X, o, o},
						{o, o, o, o} } ;
				break ;
			case 1:
			case 3:
				readableBlocks = new int[][]
 				      { { X, o, o, o},
 						{ X, X, o, o},
 						{o, X, o, o},
						{o, o, o, o} } ;
 				break ;
			}
			break ;
			
			
		// T-Block!  Like a boss!
		case PieceCatalog.TETRO_CAT_T:
			switch( rotation ) {
			case 0:
				readableBlocks = new int[][]
				      { {o, X, o, o},
						{ X, X, X, o},
						{o, o, o, o},
						{o, o, o, o} } ;
				break ;
			case 1:
				readableBlocks = new int[][]
 				      { {o, X, o, o},
 						{o, X, X, o},
 						{o, X, o, o},
						{o, o, o, o} } ;
 				break ;
			case 2:
				readableBlocks = new int[][]
 				      { {o, o, o, o},
 						{ X, X, X, o},
 						{o, X, o, o},
						{o, o, o, o} } ;
 				break ;
			case 3:
				readableBlocks = new int[][]
  				      { {o, X, o, o},
  						{ X, X, o, o},
  						{o, X, o, o},
						{o, o, o, o} } ;
  				break ;
			}
			break ;
			
			// Squigglies look like Zees!  Also,
			// they have only 2 rotation states.  Read the
			// notes for reverse squigglies above.
			case PieceCatalog.TETRO_CAT_Z:
				switch( rotation ) {
				case 0:
				case 2:
					readableBlocks = new int[][]
					      { {o, o, o, o},
							{ X, X, o, o},
							{o, X, X, o},
							{o, o, o, o} } ;
					break ;
				case 1:
				case 3:
					readableBlocks = new int[][]
	 				      { {o, o, X, o},
	 						{o, X, X, o},
	 						{o, X, o, o},
							{o, o, o, o} } ;
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
	public TetrominoRotationSystem finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		configured = true ;
		return this ;
	}
	
	
	/**
	 * Returns, as a Serializable object, the current "state" of
	 * this object, which is assumed to be in "stateful use" phase.
	 * 
	 * Calling 'setStateAsParcelable' on this object at any future
	 * point, or on another instance of the object which had an identical
	 * configuration phase, should produce an object with identical
	 * behavior and state to the one whose Serializable state was
	 * extracted - no matter what state the object was in before
	 * setState... was called.
	 * 
	 * @return Current state as a Serializable
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public Serializable getStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return state ;
	}
	
	/**
	 * Returns as a Serializable object a clone of the current "state" of this
	 * object.  It is acceptable for 'getStateAsSerializable' to return a clone,
	 * but one should NOT rely on that assumption.  If you intend to make any changes
	 * to the resulting object, or if the callee will have mutators called after
	 * this method, always get a clone rather than getState...().
	 * 
	 * @return
	 * @throws IllegalStateException
	 */
	public Serializable getCloneStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return new VersionedState( state ) ;
	}
	
	
	/**
	 * Sets the object state according to the Serializable provided,
	 * which can be assumed to have been returned by 'getStateAsSerializable()'
	 * called on an object of the same class which underwent the same
	 * pre-"finalizeConfiguration" config. process.
	 * 
	 * POST-CONDITION: The receiver will have identical state and functionality
	 * to the object upon which "getStateAsParcelable" was called.
	 * 
	 * @param in A Serializable state from an object
	 * 
	 * @throws IllegalStateException If called before 'finalizeConfiguration'
	 */
	public SerializableState setStateAsSerializable( Serializable in ) throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		int previousVersion = state.version ;
		if ( in instanceof EmptyState )
			state = new VersionedState( VERSION_EMPTY ) ;
		else
			state = (VersionedState)in ;
		if ( previousVersion != state.version )
			this.reallocateTetrominoes() ;
		return this ;
	}
	
	
	/**
	 * Writes the current state, as a Serializable object, to the provided "outStream".
	 * The same assumptions and requirements of getStateAsParcelable are true here
	 * as well.
	 * 
	 * @param outStream	An output stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If writing to the stream fails
	 */
	public void writeStateAsSerializedObject( ObjectOutputStream outStream ) throws IllegalStateException, IOException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		outStream.writeObject(state) ;
	}
	
	/**
	 * Reads the current state, as a Serializable object, from the provided "inStream".
	 * The same assumptions and requirements of setStateAsParcelable are true here
	 * as well.
	 * 
	 * @param inStream An input stream for the Serialized object
	 * @throws IllegalStateException	If called before 'finalizeConfiguration'
	 * @throws IOException	If reading from the stream fails
	 * @throws ClassNotFoundException	If the stream does not contain the class representing
	 * 			this object's Serializable state.
	 */
	public RotationSystem readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		Object o = inStream.readObject() ;
		int previousVersion = state.version ;
		if ( o instanceof EmptyState )
			state = new VersionedState( VERSION_EMPTY ) ;
		else
			state = (VersionedState)o ;
		if ( previousVersion != state.version )
			this.reallocateTetrominoes() ;
		return this ;
	}
	
}
