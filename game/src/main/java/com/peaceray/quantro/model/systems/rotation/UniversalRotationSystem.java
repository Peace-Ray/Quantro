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
 * The UniversalRotationSystem extends TetracubeRotationSystem,
 * adding the capacity to rotate Special pieces.  Tetromino and tetracube rotation
 * is handled by the superclasses.
 * 
 * Note: for proper state recovery, any class implementing SerializableState
 * should take care to maintain a state that is robust against changes
 * to its superclasses.  However, in this case, none of the RotationSystems
 * in the class hierarchy (at time of writing) maintain a state,
 * instead using the provided "EmptyState" class.
 *
 * We therefore leave implementation of the SerializableState methods to
 * the superclasses, overriding only finalizeConfiguration;
 * if superclasses becomes stateful, UniversalRotationSystem
 * will not notice the change.
 * 
 * @author Jake
 *
 */
public class UniversalRotationSystem extends PolyominoRotationSystem {
	protected byte [][][][][][][] preallocatedSpecialPiecesByCategorySubcategoryQOrientationRotation ;
	
	public UniversalRotationSystem(GameInformation ginfo, QInteractions qi, int [] types ) {
		super(ginfo, qi, types);
		
		allocateSpecialPieces( types ) ;
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
		
		if ( PieceCatalog.isSpecial(p.type) && PieceCatalog.getSpecialCategory(p.type) == PieceCatalog.SPECIAL_CAT_FLASH )
			p.rotation = 0 ;	// Convention: show in red column, not blue.
		else
			p.rotation = 1 ;	// Minimal for all tetrominos and tetracubes.
		this.turn0(p) ;
	}
	
	
	public void flipHorizontal( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( !PieceCatalog.isSpecial(p) ) {
			super.flipHorizontal(p, o) ;
			return ;
		}
		
		int cat = PieceCatalog.getSpecialCategory(p) ;
		int qc = qi.flip( PieceCatalog.getQCombination(p) ) ;
		
		// perform the flip.
		switch( cat ) {
		case PieceCatalog.SPECIAL_CAT_FLASH:
			// a flip flips the QC (taken care of) and the rotation, which
			// should go 0<->1, 2<->3.
			this.rotSwap0123Flip( p, PieceCatalog.encodeSpecial(cat, qc) ) ;
			break ;
			
		case PieceCatalog.SPECIAL_CAT_BLOCK:
			// blocks are their own flips.
			this.standardFlipHorizontal( p, PieceCatalog.encodeSpecial(cat, qc) ) ;
			break ; 
			
		default:
			throw new InvalidPieceException(p.type, "Don't know how to flip this.") ;
		}
		
	}
	
	
	public void flipVertical( Piece p, Offset o ) throws InvalidPieceException {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( !PieceCatalog.isSpecial(p) ) {
			super.flipVertical(p, o) ;
			return ;
		}
		
		int cat = PieceCatalog.getSpecialCategory(p) ;
		int qc = qi.flip( PieceCatalog.getQCombination(p) ) ;
		
		// perform the flip.
		switch( cat ) {
		case PieceCatalog.SPECIAL_CAT_FLASH:
			// a flip flips the QC (taken care of) and the rotation, which
			// should go 0<->1, 2<->3.
			this.rotSwap0123Flip( p, PieceCatalog.encodeSpecial(cat, qc) ) ;
			break ;
			
		case PieceCatalog.SPECIAL_CAT_BLOCK:
			// blocks are their own flips.
			this.standardFlipVertical( p, PieceCatalog.encodeSpecial(cat, qc) ) ;
			break ; 
			
		default:
			throw new InvalidPieceException(p.type, "Don't know how to flip this.") ;
		}
		
	}
	
	
	@Override
	protected byte [][][] getPreallocatedBlocks( Piece p ) throws InvalidPieceException {
		byte [][][] blocks = null ;
		if ( PieceCatalog.isSpecial(p.type) ) {
			int cat = PieceCatalog.getSpecialCategory(p.type) ;
			int scat = PieceCatalog.getSpecialSubcategory(p.type) ;
			int qo = PieceCatalog.getQCombination(p.type) ;
			int r = p.rotation ;
			
			try {
				blocks = preallocatedSpecialPiecesByCategorySubcategoryQOrientationRotation[cat][scat][qo][r] ;
			} catch( NullPointerException npe ) {
				System.err.println("UniversalRotationSystem: In-use allocation of blocks for piece type " + p.type) ;
				allocateSpecialPieces( new int[]{p.type} ) ;
				blocks = preallocatedSpecialPiecesByCategorySubcategoryQOrientationRotation[cat][scat][qo][r] ;
			}
		} else
			blocks = super.getPreallocatedBlocks(p) ;
		
		// Maybe we failed.  In that case, perform a rotation around the
		// "center of gravity."; that is, around the square defined by
		// our bounds.
		// What if it's not a square?  Then... tough.  No rotation for you!
		if ( blocks == null ) {
			// TODO: Implement this to make the system truly universal.
			// blocks = performRotationOnArbitraryBlocks(piece)
		}
		
		return blocks ;
	}
	
	// Does this rotation system support the specified piece type?
	@Override
	public boolean supportsPiece( int type ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return PieceCatalog.isTetromino(type) || PieceCatalog.isTetracube(type) || PieceCatalog.isSpecial(type) ;
	}
	
	@Override
	public boolean supportsPiece( Piece p ) {
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		return this.supportsPiece(p.type) ;
	}
	
	// All turns are handled by the subclass; the methods 
	// call "getPreallocatedBlocks"
	
	private void allocateSpecialPieces( int [] types ) {
		// Allocate the array for the number of categories...
		if ( preallocatedSpecialPiecesByCategorySubcategoryQOrientationRotation == null )
			preallocatedSpecialPiecesByCategorySubcategoryQOrientationRotation = new byte[PieceCatalog.NUMBER_SPECIAL_CATEGORIES][][][][][][] ;
		byte [][][][][][][] prealloc = preallocatedSpecialPiecesByCategorySubcategoryQOrientationRotation ;
		
		// Allocate blocks for the specified types.
		for ( int t = 0; t < types.length; t++ ) {
			int type = types[t] ;
			if ( PieceCatalog.isSpecial(type) ) {
				int c = PieceCatalog.getSpecialCategory(type) ;
				int s = PieceCatalog.getSpecialSubcategory(type) ;
				int qo = PieceCatalog.getQCombination(type) ;
				if ( prealloc[c] == null )
					prealloc[c] = new byte[PieceCatalog.NUMBER_SPECIAL_SUBCATEGORIES[c]][][][][][] ;
				if ( prealloc[c][s] == null )
					prealloc[c][s] = new byte[QOrientations.NUM][][][][] ;
				if ( prealloc[c][s][qo] == null ) {
					prealloc[c][s][qo] = new byte[4][][][] ;
					for ( int r = 0; r < 4; r++ ) {
						// Call another function.
						prealloc[c][s][qo][r] = allocateSpecialBlocks(c, s, qo, r) ;
					}
				}
			}
		}
	}
	
	private static byte [][][] allocateSpecialBlocks( int category, int subcategory, int qOrientation, int rotation ) {
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
		
		final int o = QOrientations.NO ;
		final int X = qOrientation ;
		int V ;
		
		// Initialized to silence warnings
		int [][] readableBlocks = {{0}} ;
		
		// Allocate readable with rightside-up blocks.
		// Note that many are padded with an extra row at the bottom;
		// this allows a simple hueristic to position them well upon
		// entry to block field.  (hueristic = center {2,1:2} )
		switch( category ) {
		// A single block.  Rotation is irrelevant.
		case PieceCatalog.SPECIAL_CAT_BLOCK:
			readableBlocks = new int[][]
			      { {o, o, o, o},
					{o, X, o, o},
					{o, o, o, o},
					{o, o, o, o} } ;
			break ;
			
		// A flash block.  Rotation matters, since this
		// switches from F0 to F1.
		case PieceCatalog.SPECIAL_CAT_FLASH:
			if ( rotation == 0 || rotation == 2 )
				V = QCombinations.F0 ;
			else
				V = QCombinations.F1 ;
			readableBlocks = new int[][]
			      { {o, o, o, o},
			     	{o, V, o, o},
			     	{o, o, o, o},
			     	{o, o, o, o} } ;
			break ;
		
		case PieceCatalog.SPECIAL_CAT_ARBITRARY:
			return null ;
			
			
		case PieceCatalog.SPECIAL_CAT_PUSH_DOWN:
			readableBlocks = new int[][]
			      { {o, o, o, o},
			     	{o, X, o, o},
			     	{o, o, o, o},
			     	{o, o, o, o} } ;
			break ;
			
		// GALAXIES: Just a cloud of blocks.  None touch, but
		// hopefully don't take up too much space.
		case PieceCatalog.SPECIAL_CAT_GALAXY:
			switch( subcategory ) {
			case 0:
				readableBlocks = new int[][]
      			      { {o, o, o, o},
      			     	{o, o, o, o},
      			     	{o, o, o, o},
      			     	{o, o, o, o} } ;
      			break ;
			case 1:
				readableBlocks = new int[][]
     			      { {o, o, o, o},
     			     	{o, X, o, o},
     			     	{o, o, o, o},
     			     	{o, o, o, o} } ;
     			break ;
     		
			case 2:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, o, o, o} } ;
				break ;
				
			case 3:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o} } ;
				break ;
				
			case 4:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, X },
				     	{o, o, o, o},
				     	{o, X, o, X } } ;
				break ;
				
			case 5:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o} } ;
				break ;
				
			case 6:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, X },
				     	{o, o, o, o},
				     	{o, X, o, X },
				     	{o, o, o, o},
				     	{o, X, o, X } } ;
				break ;
				
			case 7:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o} } ;
				break ;
				
			case 8:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, X },
				     	{o, o, o, o},
				     	{o, X, o, X },
				     	{o, o, o, o},
				     	{o, X, o, X },
				     	{o, o, o, o},
				     	{o, X, o, X } } ;
				break ;
				
			case 9:
				readableBlocks = new int[][]
				      { {o, o, o, o},
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o},
				     	{o, o, o, X },
				     	{o, X, o, o} } ;
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
	public UniversalRotationSystem finalizeConfiguration() throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		super.finalizeConfiguration() ;
		return this ;
	}
}
