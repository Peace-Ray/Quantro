package com.peaceray.quantro.model.game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.q.QSerialization;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.LooselyBoundedArray.LooselyBoundedArray;

/**
 * A data class for retrieving information from Game objects.  This data
 * represent the content of the main blockfield of a Game, including 
 * information that should be animated such as unlocking columns, falling
 * and locking chunks, row clears, etc.  It does NOT have a complete 
 * picture of the GameState; it does not include score, next piece previews,
 * etc., only a blockfield.
 * 
 * Although mainly intended for representing a game state, a GameBlocksSlice
 * can also represent a collection of Blocks that have no particular state
 * attached to them (for instance, they can represent a "next piece preview"
 * - in which case it is ONLY a next piece preview, without any main blockfield info).
 * 
 * Accessing, copying, and altering the fields of this class is the responsibility
 * of users.  The GameBlocksTimeSlice has very little in terms of functionality.
 * 
 * As of this implementation, the TimeSlice will represent only ONE of the 
 * following:
 * 
 * BLOCKS_STABLE		<- nothing happening, nothing changing, nothing falling
 * BLOCKS_PIECE_FALLING <- a user-controlled piece is currently descending
 * BLOCKS_CHUNKS_FALLING <- chunks are falling to lock in place.
 * BLOCKS_ROWS_CLEARING <- rows are being cleared.  Will be followed by either BLOCKS_STABLE
 * 								or BLOCKS_CHUNKS_FALLING, probably.
 * BLOCKS_METAMORPHOSIZING <- blocks becoming other blocks
 * BLOCKS_ADDING_ROWS <- rows are coming up from below
 * 
 * The TimeSlice also keeps a record of the time at which it was set to new content,
 * but only if the appropriate method is called.
 * 
 * 
 * EDIT 4/26/12: as a way to help quick in-game rendering, and reduce time spent in ArrayOps.setEmpty(),
 * 		LooselyBoundedArrays have been added.
 * 
 * 		These LBAS are *NOT* used when Slices are serialized in any way, including read from / written
 * 		to Streams or Strings.  After such an operation, the slice boundaries may be very different 
 * 		from the correct values.  To compensate for this, LBAs are set as "full" after every
 * 		serialized read.
 * 
 * @author Jake
 *
 */
public class GameBlocksSlice {
	
	// Stable, PieceFalling and ChunksFalling are used in the logo.
	// I don't recommend changing them.  The rest are probably fair game.
	public static final int BLOCKS_STABLE = 0 ;
	public static final int BLOCKS_PIECE_FALLING = 1 ;
	public static final int BLOCKS_CHUNKS_FALLING = 2 ;
	public static final int BLOCKS_ROWS_CLEARING = 3 ;
	public static final int BLOCKS_METAMORPHOSIZING = 4 ;
	public static final int BLOCKS_PUSHING_ROWS = 5 ;
	
	public static final int BLOCKS_PIECE_PREVIEW = 6 ;
	

	private int mBlocksState ;
	
	private byte [][][][] mBlockfields ;
	private byte [][][] mPreBlockfield ;	
	
	private byte [][][] mDisplacementBlockfield ;
	
	// blockfields for falling stuff!
	private byte [][][][] mChunks ;
	private byte [][][][] mChunksAggregated ;
	
	
	// The above fields and chunks, as LooselyBoundedArrays.
	private LooselyBoundedArray [] mLBABlockfields ;
	private LooselyBoundedArray mLBAPreBlockfield ;
	
	private LooselyBoundedArray mLBADisplacementBlockfield ;
	
	private LooselyBoundedArray [] mLBAChunks ;
	private LooselyBoundedArray [] mLBAChunksAggregated ;
	
	
	
	
	// special indices used for the above.
	private static final int INDEX_PIECE_PREVIEW_STABLE = 0 ;
	private static final int INDEX_PIECE_PREVIEW_MINIMUM_PROFILE = 1 ;
	
	private static final int INDEX_STABLE_STABLE = 0 ;
	
	private static final int INDEX_PIECE_FIELD = 0 ;		// field
	private static final int INDEX_PIECE_PIECE = 1 ;		// piece
	private static final int INDEX_PIECE_STABLE = 2 ;		// includes field and piece
	
	private static final int INDEX_CLEAR_STABLE = 0 ;		// after the clear
	private static final int INDEX_CLEAR_CLEARED = 1 ;		// those blocks which were cleared
	
	private static final int INDEX_METAMORPHOSIS_STABLE = 0 ;
	
	private static final int INDEX_PUSHING_STABLE = 0 ;
	
	
	
	// Additional information for particular states
	private int mPieceType ;
	
	// Specific information about falling chunks
	private int mNumChunks ;
	private int mFirstChunk ;
	private boolean mChunksArePieceComponents ;
	private boolean mIncludesAggregatedChunks ;
	private int [] mFallDistances ;
	private boolean [] mChunkIsNewToBlockField ;
	
	// Unlocked columns?
	private int mNumUnlockedColumns ;
	ArrayList<Offset> mUnlockedColumns ;
	
	// Rows added?
	private int mNumRowsPushedUp ;
	private int mNumRowsPushedDown ;
	private int mNumRowsPushedUpThatAreGarbage ;
	private int mNumRowsTransferredIn ;		// transferred into the blockfield from displacement.
	
	// Clear information?
	private int mClearCascadeNumber ;		// starts from 0, then 1, 2, etc.
	private int [] mClears ;
	private boolean [] mMonochromeClears ;
	
	// Metamorphosis information?
	private boolean mMetamorphosisPreIncludesNewBlocks ;
	
	// Current displacement.  Not transmitted when serialized.
	private double mDisplacement ;
	
	// metadata
	private int mR, mC ;
	private int mNum, mEdge ;
	
	
	/**
	 * Instantiates a new GameBlocksTimeSlice object.
	 * 
	 * @param R The number of rows to represent.
	 * @param C The number of columns to represent.
	 * @param num The number of extra blockfields we will allocate.
	 * 				If always STABLE, 0 is sufficient.
	 * @param edge The extra space around each block field.
	 */
	public GameBlocksSlice( int R, int C, int num, int edge ) {
		
		mR = R ;
		mC = C ;
		mNum = num ;
		mEdge = edge ;
		
		mBlocksState = BLOCKS_STABLE ;
		
		mBlockfields = new byte[num+1][2][R+edge*2][C+edge*2] ;
		mPreBlockfield = new byte[2][R+edge*2][C+edge*2] ;
		mDisplacementBlockfield = new byte[2][R+edge*2][C+edge*2] ;
		
		mChunks = new byte[num+1][2][R+edge*2][C+edge*2] ;
		mChunksAggregated = new byte[num+1][2][R+edge*2][C+edge*2] ;
		
		for ( int i = 0; i < num+1; i++ ) {
			ArrayOps.setEmpty(mBlockfields[i]) ;
			ArrayOps.setEmpty(mChunks[i]) ;
			ArrayOps.setEmpty(mChunksAggregated[i]) ;
		}
		ArrayOps.setEmpty(mPreBlockfield) ;
		
		createLooselyBoundedArrays() ;
		
		mPieceType = 0 ;
		mNumChunks = 0 ;
		mFirstChunk = 0 ;
		mChunksArePieceComponents = false ;
		mIncludesAggregatedChunks = false ;
		mFallDistances = new int[num+1];
		mChunkIsNewToBlockField = new boolean[num+1] ;
		
		// Unlocked columns?
		mNumUnlockedColumns = 0 ;
		mUnlockedColumns = new ArrayList<Offset>() ;
		for ( int i = 0; i < num; i++ )
			mUnlockedColumns.add(new Offset()) ;
		
		// Rows added?
		mNumRowsPushedUp = 0 ;
		mNumRowsPushedDown = -1 ;
		mNumRowsPushedUpThatAreGarbage = 0 ;
		mNumRowsTransferredIn = 0 ;
		
		// Clear information?
		mClearCascadeNumber = 0 ;
		mClears = new int[mR + mEdge] ;
		mMonochromeClears = new boolean[mR + mEdge] ;
		
		// Displacement is 0 by default.
		mDisplacement = 0 ;
	}
	
	
	/**
	 * Instantiates a new GameBlocksTimeSlice object as a copy
	 * of the provided, under certain reasonable assumptions about formatting.
	 */
	public GameBlocksSlice( GameBlocksSlice gbs ) {
		this( gbs.mR, gbs.mC, gbs.mNum, gbs.mEdge ) ;
		takeVals( gbs ) ;
	}
	
	
	/**
	 * Instantiates a new GameBlocksTimeSlice object from the provided
	 * string representation.  The new Slice will be functionally
	 * identical to the old, in that all metadata and special data will
	 * be copied (according to mBlocksState) and a reasonable guess of which
	 * blockfields are relevant will be used to copy a portion of them.
	 * 
	 * @throws IOException 
	 */
	public GameBlocksSlice( String rep ) throws IOException {
		read( null, null, rep, true ) ;
	}
	
	
	/**
	 * Instantiates a new GameBlocksTimeSlice object from the provided
	 * object input stream representation.  The new Slice will be functionally
	 * identical to that in the String, in that all metadata and special data will
	 * be copied (according to mBlocksState) and a reasonable guess of which
	 * blockfields are relevant will be used to copy a portion of them.
	 * 
	 * @throws IOException 
	 */
	public GameBlocksSlice( ObjectInputStream ois ) throws IOException {
		read( null, ois, null, true ) ;
	}
	
	
	/**
	 * Constructs a GameBlocksSlice using the provided (hopefully)
	 * minimum-length byte encoding.
	 * @param bytes
	 */
	public GameBlocksSlice( byte [] bytes ) {
		try {
			ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( bytes ) );
			read( null, ois, null, true ) ;
		} catch (IOException e) {
			throw new IllegalArgumentException("Cannot construct with invalid sequence") ;
		}
	}
	
	
	/**
	 * Instantiates a new GameBlocksTimeSlice object using the provided
	 * GBS as a 'template,' from which 'rep' represents the difference.
	 * 
	 * @throws IOException 
	 */
	public GameBlocksSlice( GameBlocksSlice template, String rep ) throws IOException {
		read( template, null, rep, true ) ;
	}
	
	
	/**
	 * Instantiates a new GameBlocksSlice object from the provided
	 * object input stream.  Uses the GBS given as a template, from
	 * which 'ois' holds a transformation.  The new Slice will be functionally
	 * identical to that stored in the stream, in that all metadata and special data will
	 * be copied (according to mBlocksState) and a reasonable guess of which
	 * blockfields are relevant will be used to copy a portion of them.
	 * 
	 * @throws IOException 
	 */
	public GameBlocksSlice( GameBlocksSlice template, ObjectInputStream ois ) throws IOException {
		read( template, ois, null, true ) ;
	}
	
	public int rows() {
		return mR ;
	}
	
	public int cols() {
		return mC ;
	}
	
	public int edge() {
		return mEdge ;
	}
	
	public int num() {
		return mNum ;
	}
	
	
	public boolean fits( GameBlocksSlice gbs ) {
		if ( gbs == null )
			return true ;
		return mR == gbs.mR && mC == gbs.mC && mNum >= gbs.mNum && mEdge == gbs.mEdge ;
	}
	
	
	private void createLooselyBoundedArrays() {
		mLBABlockfields = new LooselyBoundedArray[mBlockfields.length] ;
		for ( int i = 0; i < mBlockfields.length; i++ ) {
			mLBABlockfields[i] = LooselyBoundedArray.newLooselyBoundedArray(mBlockfields[i]) ;
		}
		mLBAPreBlockfield = LooselyBoundedArray.newLooselyBoundedArray(mPreBlockfield) ;
		
		mLBADisplacementBlockfield = LooselyBoundedArray.newLooselyBoundedArray(mDisplacementBlockfield) ;
		
		mLBAChunks = new LooselyBoundedArray[mChunks.length] ;
		for ( int i = 0; i < mChunks.length; i++ ) {
			mLBAChunks[i] = LooselyBoundedArray.newLooselyBoundedArray(mChunks[i]) ;
		}
		mLBAChunksAggregated = new LooselyBoundedArray[mChunksAggregated.length] ;
		for ( int i = 0; i < mChunksAggregated.length; i++ ) {
			mLBAChunksAggregated[i] = LooselyBoundedArray.newLooselyBoundedArray(mChunksAggregated[i]) ;
		}
	}
	
	private void boundAllLooselyBoundedArrays() {
		mLBAPreBlockfield.boundAll() ;
		mLBADisplacementBlockfield.boundAll() ;
		for ( int i = 0; i < mNum+1; i++ ) {
			mLBABlockfields[i].boundAll() ;
			mLBAChunks[i].boundAll();
			mLBAChunksAggregated[i].boundAll();
		}
	}
	
	
	public void takeVals( GameBlocksSlice gbs ) {
		if ( this == gbs )		// Already has its own vals.
			return ;
		
		if ( !fits(gbs) )
			throw new IllegalArgumentException("Provided Slice does not match basic dimensions.  My dims: " + mR + ", " + mC + ", " + mNum + ", " + mEdge + " vs. gbs dims: " + gbs.mR + ", " + gbs.mC + ", " + gbs.mNum + ", " + gbs.mEdge) ;
		
		if ( gbs == null )
			return ;
		
		mBlocksState = gbs.mBlocksState ;
		
		mPieceType = gbs.mPieceType ;
		mNumChunks = gbs.mNumChunks ;
		mFirstChunk = gbs.mFirstChunk ;
		mChunksArePieceComponents = gbs.mChunksArePieceComponents ;
		mIncludesAggregatedChunks = gbs.mIncludesAggregatedChunks ;
		
		// Unlocked columns?
		mNumUnlockedColumns = gbs.mNumUnlockedColumns ;
		
		// Rows added?
		mNumRowsPushedUp = gbs.mNumRowsPushedUp ;
		mNumRowsPushedDown = gbs.mNumRowsPushedDown ;
		mNumRowsPushedUpThatAreGarbage = gbs.mNumRowsPushedUpThatAreGarbage ;
		mNumRowsTransferredIn = gbs.mNumRowsTransferredIn ;
		
		// Copy information.
		ArrayOps.copyInto(gbs.mPreBlockfield, mPreBlockfield) ;
		mLBAPreBlockfield.boundNone().bound(gbs.mLBAPreBlockfield) ;
		ArrayOps.copyInto(gbs.mDisplacementBlockfield, mDisplacementBlockfield) ;
		mLBADisplacementBlockfield.boundNone().bound(gbs.mLBADisplacementBlockfield) ;
		// at most 3 blockfields are relevant, unless falling chunks
		int firstBlockfield = mBlocksState == BLOCKS_CHUNKS_FALLING ? mFirstChunk : 0 ;
		int lastBlockfield = mBlocksState == BLOCKS_CHUNKS_FALLING ? mFirstChunk + mNumChunks : Math.min(mNum+1, 3) ;
		for ( int i = firstBlockfield; i < lastBlockfield; i++ ) {
			ArrayOps.copyInto(gbs.mBlockfields[i], mBlockfields[i]) ;
			mLBABlockfields[i].boundNone().bound(gbs.mLBABlockfields[i]) ;
		}
		for ( int i = mFirstChunk; i < mFirstChunk + mNumChunks; i++ ) {
			ArrayOps.copyInto(gbs.mChunks[i], mChunks[i]) ;
			mLBAChunks[i].boundNone().bound(gbs.mLBAChunks[i]) ;
		}
		if ( mIncludesAggregatedChunks ) {
			for ( int i = mFirstChunk; i < mFirstChunk + mNumChunks; i++ ) {
				ArrayOps.copyInto(gbs.mChunksAggregated[i], mChunksAggregated[i]) ;
				mLBAChunksAggregated[i].boundNone().bound(gbs.mLBAChunksAggregated[i]) ;
			}
		}
		// fall distances, columns, clears
		for ( int i = 0; i < mNumChunks; i++ )
			mFallDistances[i] = gbs.mFallDistances[i] ;
		for ( int i = 0; i < mNumChunks; i++ )
			mChunkIsNewToBlockField[i] = gbs.mChunkIsNewToBlockField[i] ;
		for ( int i = 0; i < mNumUnlockedColumns; i++ )
			mUnlockedColumns.get(i).takeVals( gbs.mUnlockedColumns.get(i) ) ;
		mClearCascadeNumber = gbs.mClearCascadeNumber ;
		for ( int i = 0; i < mClears.length; i++ ) {
			mClears[i] = gbs.mClears[i] ;
			mMonochromeClears[i] = gbs.mMonochromeClears[i] ;
		}
		
		mDisplacement = gbs.mDisplacement ;
	}
	
	
	
	// State access / changes
	public int getBlocksState() {
		return mBlocksState ;
	}
	
	public String getBlocksStateString() {
		switch( mBlocksState ) {
		case BLOCKS_STABLE:
			return "Stable" ;
		case BLOCKS_PIECE_FALLING:
			return "PieceFalling" ;
		case BLOCKS_CHUNKS_FALLING:
			return "ChunksFalling" ;
		case BLOCKS_ROWS_CLEARING:
			return "RowsClearing" ;
		case BLOCKS_METAMORPHOSIZING:
			return "Metamorphosizing" ;
		case BLOCKS_PUSHING_ROWS:
			return "PushingRows" ;
		case BLOCKS_PIECE_PREVIEW:
			return "PiecePreview" ;
		}
		
		return "UNKNOWN" ;
	}
	
	public void setBlocksState( int state ) {
		mBlocksState = state ;
	}
	
	
	// Accessors
	public int getEdge() {
		return mEdge ;
	}
	
	// Blockfield accessors.  Used for raw data-access.
	
	/**
	 * Returns the "stable" blockfield.  If representing a change, this
	 * is the state after that change is complete.  For piece falling, this
	 * is the state with the piece at its current location.
	 */
	public byte [][][] getBlockfieldStable() {
		switch( mBlocksState ) {
		case BLOCKS_PIECE_PREVIEW:
			return mBlockfields[INDEX_PIECE_PREVIEW_STABLE] ;
		case BLOCKS_STABLE:
			return mBlockfields[INDEX_STABLE_STABLE] ;
		case BLOCKS_PIECE_FALLING:
			return mBlockfields[INDEX_PIECE_STABLE] ;
		case BLOCKS_CHUNKS_FALLING:
			return mBlockfields[mFirstChunk + mNumChunks - 1] ;
		case BLOCKS_ROWS_CLEARING:
			return mBlockfields[INDEX_CLEAR_STABLE] ;
		case BLOCKS_METAMORPHOSIZING:
			return mBlockfields[INDEX_METAMORPHOSIS_STABLE] ;
		case BLOCKS_PUSHING_ROWS:
			return mBlockfields[INDEX_PUSHING_STABLE] ;
		default:
			throw new IllegalStateException("State " + mBlocksState + " is not supported") ;
		}
	}
	
	
	/**
	 * Returns the "initial" blockfield.  If representing a change, this is the
	 * state before that change begins.  For piece falling, this is the field, without
	 * a piece present.  If no change, than this is the same as getBlockfieldStable().
	 * 
	 * Note: for normal Slice progression (i.e. w/o inconsistency), a slice's
	 * "initial" blockfield should be identical to the previous slices "stable"
	 * blockfield.  (remember that piece previews have no "consistent" progression)
	 * 
	 * @return
	 */
	public byte [][][] getBlockfieldInitial() {
		switch( mBlocksState ) {
		case BLOCKS_PIECE_PREVIEW:
			return mBlockfields[INDEX_PIECE_PREVIEW_STABLE] ;
		case BLOCKS_STABLE:
			return mBlockfields[INDEX_STABLE_STABLE] ;
		case BLOCKS_PIECE_FALLING:
			return mBlockfields[INDEX_PIECE_FIELD] ;
		case BLOCKS_CHUNKS_FALLING:
			return mPreBlockfield ;
		case BLOCKS_ROWS_CLEARING:
			return mPreBlockfield ;
		case BLOCKS_METAMORPHOSIZING:
			return mPreBlockfield ;
		case BLOCKS_PUSHING_ROWS:
			return mPreBlockfield ;
		default:
			throw new IllegalStateException("State " + mBlocksState + " is not supported") ;
		}
	}
	
	
	public byte [][][] getBlockfieldMinimumProfile() {
		if ( mBlocksState != BLOCKS_PIECE_PREVIEW )
			throw new IllegalStateException("State " + mBlocksState + " has no minimum profile") ;
		return mBlockfields[INDEX_PIECE_PREVIEW_MINIMUM_PROFILE] ;
	}
	
	
	public byte [][][][] getChunks() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " has no chunks") ;
		return mChunks ;
	}
	
	public byte [][][][] getChunksAggregated() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " has no chunks") ;
		return mChunksAggregated ;
	}
	
	
	
	public byte [][][][] getBlockfields() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have multiple blockfields") ;
		return mBlockfields ;
	}
	
	public byte [][][] getChunksFallingPreBlockfield() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		return mPreBlockfield ;
	}
	
	public byte [][][] getPieceFallingBlockfield() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mBlockfields[INDEX_PIECE_FIELD] ;
	}
	
	public byte [][][] getPieceFallingPiece() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mBlockfields[INDEX_PIECE_PIECE] ;
	}
	
	public byte [][][][] getPieceFallingGhosts() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mChunks ;
	}
	
	
	public byte [][][] getClearPreBlockfield() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a clear") ;
		
		return mPreBlockfield ;
	}
	
	
	public byte [][][] getClearPostBlockfield() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a clear") ;
		
		return mBlockfields[INDEX_CLEAR_STABLE] ;
	}
	
	public byte [][][] getClearClearedBlocks() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a clear") ;
		
		return mBlockfields[INDEX_CLEAR_CLEARED] ;
	}
	
	
	
	public byte [][][] getMetamorphosisPreBlockfield() {
		if ( mBlocksState != BLOCKS_METAMORPHOSIZING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a metamorphosis") ;
		
		return mPreBlockfield ;
	}
	
	
	public byte [][][] getMetamorphosisPostBlockfield() {
		if ( mBlocksState != BLOCKS_METAMORPHOSIZING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a metamorphosis") ;
		
		return mBlockfields[INDEX_METAMORPHOSIS_STABLE] ;
	}
	
	
	public byte [][][] getPushingRowsPreBlockfield() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a row-push") ;
		
		return mPreBlockfield ;
	}
	
	public byte [][][] getPushingRowsPostBlockfield() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a row-push") ;
		
		return mBlockfields[INDEX_PUSHING_STABLE] ;
	}
	
	public byte [][][] getDisplacementBlockfield() {
		return mDisplacementBlockfield ;
	}
	
	
	// LoooselyBoundedArray Blockfield accessors.  Used to bound raw-data access.
	// See notes regarding LBAs for accuracy guarantees; specifically, that there are none.
	
	/**
	 * Returns the "stable" blockfield.  If representing a change, this
	 * is the state after that change is complete.  For piece falling, this
	 * is the state with the piece at its current location.
	 */
	public LooselyBoundedArray getBlockfieldStableLBA() {
		switch( mBlocksState ) {
		case BLOCKS_STABLE:
			return mLBABlockfields[INDEX_STABLE_STABLE] ;
		case BLOCKS_PIECE_FALLING:
			return mLBABlockfields[INDEX_PIECE_STABLE] ;
		case BLOCKS_CHUNKS_FALLING:
			return mLBABlockfields[mFirstChunk + mNumChunks - 1] ;
		case BLOCKS_ROWS_CLEARING:
			return mLBABlockfields[INDEX_CLEAR_STABLE] ;
		case BLOCKS_METAMORPHOSIZING:
			return mLBABlockfields[INDEX_METAMORPHOSIS_STABLE] ;
		case BLOCKS_PUSHING_ROWS:
			return mLBABlockfields[INDEX_PUSHING_STABLE] ;
		default:
			throw new IllegalStateException("State " + mBlocksState + " is not supported") ;
		}
	}
	
	
	
	public LooselyBoundedArray [] getChunksLBA() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " has no chunks") ;
		return mLBAChunks ;
	}
	
	public LooselyBoundedArray [] getChunksAggregatedLBA() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " has no chunks") ;
		return mLBAChunksAggregated ;
	}
	
	
	
	public LooselyBoundedArray [] getBlockfieldsLBA() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have multiple blockfields") ;
		return mLBABlockfields ;
	}
	
	public LooselyBoundedArray getChunksFallingPreBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		return mLBAPreBlockfield ;
	}
	
	public LooselyBoundedArray getPieceFallingBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mLBABlockfields[INDEX_PIECE_FIELD] ;
	}
	
	public LooselyBoundedArray getPieceFallingPieceLBA() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mLBABlockfields[INDEX_PIECE_PIECE] ;
	}
	
	public LooselyBoundedArray [] getPieceFallingGhostsLBA() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mLBAChunks ;
	}
	
	
	public LooselyBoundedArray getClearPreBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a clear") ;
		
		return mLBAPreBlockfield ;
	}
	
	
	public LooselyBoundedArray getClearPostBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a clear") ;
		
		return mLBABlockfields[INDEX_CLEAR_STABLE] ;
	}
	
	public LooselyBoundedArray getClearClearedBlocksLBA() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a clear") ;
		
		return mLBABlockfields[INDEX_CLEAR_CLEARED] ;
	}
	
	
	
	public LooselyBoundedArray getMetamorphosisPreBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_METAMORPHOSIZING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a metamorphosis") ;
		
		return mLBAPreBlockfield ;
	}
	
	
	public LooselyBoundedArray getMetamorphosisPostBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_METAMORPHOSIZING )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a metamorphosis") ;
		
		return mLBABlockfields[INDEX_METAMORPHOSIS_STABLE] ;
	}
	
	public LooselyBoundedArray getPushingRowsPreBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a row-push") ;
		
		return mLBAPreBlockfield ;
	}
	
	
	public LooselyBoundedArray getPushingRowsPostBlockfieldLBA() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not represent a row-push") ;
		
		return mLBABlockfields[INDEX_PUSHING_STABLE] ;
	}
	
	public LooselyBoundedArray getDisplacementBlockfieldLBA() {
		return mLBADisplacementBlockfield ;
	}
	
	// Accessors/Setters for special states
	// PIECE FALLING:
	
	public int getPieceType() {
		return mPieceType ;
	}
	
	public void setPieceType( int type ) {
		mPieceType = type ;
	}
	
	public int getNumPieceGhosts() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mNumChunks ;
	}
	
	public void setNumPieceGhosts( int num ) {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		mNumChunks = num ;
	}
	
	
	public int getFirstPieceGhosts() {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mFirstChunk ;
	}
	
	
	public void setFirstPieceGhost( int num ) {
		if ( mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		mFirstChunk = num ;
	}
	
	
	public int getNumChunks() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mNumChunks ;
	}
	
	public void setNumChunks( int num ) {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		mNumChunks = num ;
	}
	
	public int getFirstChunk() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		return mFirstChunk ;
	}
	
	public void setFirstChunk( int num ) {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have a piece") ;
		
		mFirstChunk = num ;
	}

	public boolean getIncludesAggregatedChunks() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		return mIncludesAggregatedChunks ;
	}
	
	public void setIncludesAggregatedChunks( boolean aggregated ) {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING && mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		mIncludesAggregatedChunks = aggregated ;
	}
	
	public boolean getChunksArePieceComponents() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		return mChunksArePieceComponents ;
	}
	
	public void setChunksArePieceComponents( boolean pieceComponents ) {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING && mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		mChunksArePieceComponents = pieceComponents ;
	}
	
	public int [] getFallDistances() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING && mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		return mFallDistances ;
	}
	
	public boolean [] getNewToBlockField() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING && mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		return mChunkIsNewToBlockField ;
	}
	
	public int getFallDistancesMax() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING && mBlocksState != BLOCKS_PIECE_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		return mFallDistances[mFirstChunk + mNumChunks - 1] ;
	}
	
	
	public int getNumUnlockedColumns() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		return mNumUnlockedColumns ;
	}
	
	public void setNumUnlockedColumns(int num) {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		mNumUnlockedColumns = num ;
	}
	
	public ArrayList<Offset> getUnlockedColumns() {
		if ( mBlocksState != BLOCKS_CHUNKS_FALLING )
			throw new IllegalStateException("State " + mBlocksState + " does not have chunks") ;
		
		return mUnlockedColumns ;
	}
	
	public int getNumRowsAddedByPush() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed rows") ;
		
		return mNumRowsPushedDown < 0 ? mNumRowsPushedUp : mNumRowsPushedUp - mNumRowsPushedDown ;
	}
	
	public int getNumRowsPushedUp() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed rows") ;
		
		return mNumRowsPushedUp ;
	}
	
	public int getNumRowsPushedDown() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed rows") ;
		
		return mNumRowsPushedDown ;
	}
	
	public boolean getPushedRowsIncludesGarbage() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed/garbage rows") ;
		return mNumRowsPushedUpThatAreGarbage > 0 ;
	}
	
	public boolean getPushedRowsIncludesNonGarbage() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed/garbage rows") ;
		return mNumRowsPushedDown > 0 || mNumRowsPushedUp - mNumRowsPushedUpThatAreGarbage > 0 ;
	}
	
	public int getNumGarbageRowsAdded() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed/garbage rows") ;
		return mNumRowsPushedUpThatAreGarbage ;
	}
	
	public int getNumNonGarbageRowsAdded() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed/garbage rows") ;
		return getNumRowsAddedByPush() - mNumRowsPushedUpThatAreGarbage ;
	}
	
	
	
	public int getNumRowsAddedByTransfer() {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have transferred rows") ;
		
		return mNumRowsTransferredIn ;
	}
	
	
	public void setNumRowsPushedUp( int num ) {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed rows") ;
		if ( num < 0 )
			throw new IllegalArgumentException("To push down, use setNumRowsPushed()") ;
		
		mNumRowsPushedUp = num ;
		mNumRowsPushedDown = -1 ;
	}
	
	public void setNumRowsPushed( int numPushedDown, int numPushedUp ) {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed rows") ;
		if ( numPushedDown < 0 || numPushedUp < 0 )
			throw new IllegalArgumentException("Must provide nonnegative pushes.") ;
		
		mNumRowsPushedUp = numPushedUp ;
		mNumRowsPushedDown = numPushedDown ;
	}

	public void setNumPushedRowsThatAreGarbage( int num ) {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have pushed rows") ;
		if ( num < 0 )
			throw new IllegalArgumentException("Must provide nonnegative pushes.") ;
		mNumRowsPushedUpThatAreGarbage = num ;
	}
	
	public void setNumRowsTransferredIn( int transferredIn ) {
		if ( mBlocksState != BLOCKS_PUSHING_ROWS )
			throw new IllegalStateException("State " + mBlocksState + " does not have transferred rows") ;
		if ( transferredIn < 0 )
			throw new IllegalArgumentException("Must provide nonnegative transfers.") ;
		
		mNumRowsTransferredIn = transferredIn ;
	}
	
	public int getClearCascadeNumber() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not have clears") ;
		
		return mClearCascadeNumber ;
	}
	
	public void setClearCascadeNumber( int num ) {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not have clears") ;
		
		mClearCascadeNumber = num ;
	}
	
	
	public int [] getClears() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not have clears") ;
		
		return mClears ;
	}
	
	
	public boolean [] getMonochromeClears() {
		if ( mBlocksState != BLOCKS_ROWS_CLEARING )
			throw new IllegalStateException("State " + mBlocksState + " does not have clears") ;
		
		return mMonochromeClears ;
	}
	
	
	public void setMetamorphosisPreIncludesNewBlocks( boolean includes ) {
		if ( mBlocksState != BLOCKS_METAMORPHOSIZING )
			throw new IllegalStateException("State " + mBlocksState + " does not include new metamorphosis blocks") ;
		
		mMetamorphosisPreIncludesNewBlocks = includes ;
	}
	
	public boolean getMetamorphosisPreIncludesNewBlocks() {
		if ( mBlocksState != BLOCKS_METAMORPHOSIZING )
			throw new IllegalStateException("State " + mBlocksState + " does not include new metamorphosis blocks") ;
		
		return mMetamorphosisPreIncludesNewBlocks ;
	}
	
	
	public void setDisplacement( double displacement ) {
		mDisplacement = displacement ;
	}
	
	public double getDisplacement() {
		return mDisplacement ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SERIALIZATION!  READING, WRITING TO STRINGS AND OBJECT OUTPUT STREAMS
	//
	////////////////////////////////////////////////////////////////////////////
	
	public byte [] getBytes() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
			ObjectOutputStream oos = new ObjectOutputStream(baos) ;
			
			write( oos ) ;
			
			oos.close() ;
			return baos.toByteArray() ;
		} catch (IOException ioe ) {
			throw new IllegalStateException("Failed while writing as bytes.") ;
		}
	}
	
	public void write( ObjectOutputStream oos ) throws IOException {
		write( null, oos, null ) ;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder() ;
		try {
			write( null, null, sb ) ;
		} catch (IOException e) {
			throw new RuntimeException("whoops") ;
		}
		return sb.toString() ;
	}
	
	public void writeAsSliceAfter( GameBlocksSlice gbs, ObjectOutputStream oos ) throws IOException {
		write( gbs, oos, null ) ;
	}
	
	public String toStringAsSliceAfter( GameBlocksSlice gbs ) throws IOException {
		StringBuilder sb = new StringBuilder() ;
		try {
			write( gbs, null, sb ) ;
		} catch (IOException e) {
			throw new RuntimeException("whoops") ;
		}
		return sb.toString() ;
	}
	
	
	private static final String SPECIAL_OPEN = "<SPECIAL>" ;
	private static final String SPECIAL_CLOSE = "</SPECIAL>" ;
	
	private static final String FIELDS_OPEN = "<FIELDS>" ;
	private static final String FIELDS_CLOSE = "</FIELDS>" ;
	
	private static final String FIELD_OPEN = "<FIELD>" ;
	private static final String FIELD_CLOSE = "</FIELD>" ;
	
	private static final String SLICE_OPEN = "<GB_SLICE>" ;
	private static final String SLICE_CLOSE = "</GB_SLICE>" ;
	
	private static final String META_OPEN = "<META>" ;
	private static final String META_CLOSE = "</META>" ;
	
	private static final String NEWLINE = "\n" ;
	
	private static final String PREFIX_STATE = "BLOCK_STATE" ;
	private static final String PREFIX_R = "R" ;
	private static final String PREFIX_C = "C" ;
	private static final String PREFIX_NUM = "NUM" ;
	private static final String PREFIX_EDGE = "EDGE" ;
	private static final String PREFIX_PIECE_TYPE = "PIECE_TYPE" ;
	
	private static final String PREFIX_FIRST_CHUNK = "FIRST_CHUNK" ;
	private static final String PREFIX_NUM_CHUNKS = "NUM_CHUNKS" ;
	private static final String PREFIX_PIECE_COMPONENTS = "PIECE_COMPONENTS" ;
	private static final String PREFIX_INCLUDES_AGGREGATED_CHUNKS = "INCLUDES_AGGREGATED_CHUNKS" ;
	private static final String PREFIX_FALL_DISTANCES = "FALL_DISTANCES" ;
	private static final String PREFIX_IS_NEW = "IS_NEW" ;
	private static final String PREFIX_NUM_UNLOCKED_COLUMNS = "NUM_UNLOCKED_COLUMNS" ;
	private static final String PREFIX_UNLOCKED_COLUMNS = "UNLOCKED_COLUMNS" ;
	
	private static final String PREFIX_CLEAR_CASCADE_NUMBER = "CLEAR_CASCADE_NUMBER" ;
	private static final String PREFIX_CLEARS = "CLEARS" ;
	private static final String PREFIX_MONOCLEARS = "MONOCLEARS" ;
	
	private static final String PREFIX_METAMORPHOSIS_PRE_HAS_NEW = "NEW_PRE" ;
	
	private static final String PREFIX_NUM_ROWS = "NUM_ROWS" ;
	
	
	
	
	private void write( GameBlocksSlice gbs, ObjectOutputStream oos, StringBuilder sb ) throws IOException {
		////////////////////////////////////////////////////////////////////////
		// PREAMBLE
		// Drop an opener.
		append( sb, SLICE_OPEN ).append( sb, NEWLINE ) ;
		append( sb, META_OPEN ).append( sb, NEWLINE ) ;
		// Type?  Metadata?
		append( sb, PREFIX_STATE )		.write( oos, sb, mBlocksState )	.append( sb, NEWLINE ) ;
		append( sb, PREFIX_R )			.write( oos, sb, mR ) 			.append( sb, NEWLINE ) ;
		append( sb, PREFIX_C )			.write( oos, sb, mC ) 			.append( sb, NEWLINE ) ;
		append( sb, PREFIX_NUM )		.write( oos, sb, mNum )			.append( sb, NEWLINE ) ;
		append( sb, PREFIX_EDGE )		.write( oos, sb, mEdge ) 		.append( sb, NEWLINE ) ;
		// Piece type.  Almost always relevant, so why not write it?
		append( sb, PREFIX_PIECE_TYPE )	.write( oos, sb, mPieceType ) 	.append( sb, NEWLINE ) ;
		append( sb, META_CLOSE ).append( sb, NEWLINE ) ;
		
		////////////////////////////////////////////////////////////////////////
		// SPECIAL DATA
		// Certain block states will require additional data, which we don't
		// bother writing for other types.
		writeSpecialData( oos, sb ) ;
		
		////////////////////////////////////////////////////////////////////////
		// BLOCK FIELDS
		// Certain block states will require additional data, which we don't
		// bother writing for other types.
		writeBlockFields( gbs, oos, sb ) ;
		
		// Close the opener
		append( sb, SLICE_CLOSE ).append( sb, NEWLINE ) ;
	}
	
	
	private void writeSpecialData( ObjectOutputStream oos, StringBuilder sb ) throws IOException {
		// prefix
		append( sb, SPECIAL_OPEN ).append( sb, NEWLINE ) ;
		
		// contents are based on block state
		switch( mBlocksState ) {
		case BLOCKS_PIECE_PREVIEW:
			// we don't need no extra data
			// we don't need no thought control
			break ;
		case BLOCKS_STABLE:
			// we don't need any extra data
			break ;
		case BLOCKS_PIECE_FALLING:
			// first chunk, num chunks
			append( sb, PREFIX_FIRST_CHUNK ).write( oos, sb, mFirstChunk ) 	.append( sb, NEWLINE ) ;
			append( sb, PREFIX_NUM_CHUNKS )	.write( oos, sb, mNumChunks ) 	.append( sb, NEWLINE ) ;
			break ;
		case BLOCKS_CHUNKS_FALLING:
			// first chunk, num chunks, isPieceComponent?, fall distances, num columns, columnOffsets.
			append( sb, PREFIX_FIRST_CHUNK )		.write( oos, sb, mFirstChunk ) 	.append( sb, NEWLINE ) ;
			append( sb, PREFIX_NUM_CHUNKS )			.write( oos, sb, mNumChunks ) 	.append( sb, NEWLINE ) ;
			append( sb, PREFIX_PIECE_COMPONENTS )	.write( oos, sb, mChunksArePieceComponents ) 	.append( sb, NEWLINE ) ;
			append( sb, PREFIX_INCLUDES_AGGREGATED_CHUNKS ).write( oos, sb, mIncludesAggregatedChunks ).append( sb, NEWLINE ) ;
			append( sb, PREFIX_FALL_DISTANCES ) ;
			for ( int i = 0; i < mNumChunks; i++ )
				write( oos, sb, mFallDistances[i + mFirstChunk] ) ;
			append( sb, PREFIX_FALL_DISTANCES ) ;
			for ( int i = 0; i < mNumChunks; i++ )
				write( oos, sb, mChunkIsNewToBlockField[i + mFirstChunk] ) ;
			append( sb, NEWLINE) ;
			append( sb, PREFIX_NUM_UNLOCKED_COLUMNS ).write( oos, sb, mNumUnlockedColumns ).append( sb, NEWLINE ) ;
			append( sb, PREFIX_UNLOCKED_COLUMNS ) ;
			for ( int i = 0; i < mNumUnlockedColumns; i++ )
				write( oos, sb, mUnlockedColumns.get(i) ) ;
			append( sb, NEWLINE ) ;
			break ;
			
		case BLOCKS_ROWS_CLEARING:
			// cascade number
			append( sb, PREFIX_CLEAR_CASCADE_NUMBER ) 	.write( oos, sb, mClearCascadeNumber )	.append( sb, NEWLINE ) ;
			// clears, monoclears
			append( sb, PREFIX_CLEARS ) ;
			for ( int i = mEdge; i < mR + mEdge; i++ )
				write( oos, sb, mClears[i] ) ;
			append( sb, NEWLINE) ;
			append( sb, PREFIX_MONOCLEARS ) ;
			for ( int i = mEdge; i < mR + mEdge; i++ )
				write( oos, sb, mMonochromeClears[i] ) ;
			append( sb, NEWLINE) ;
			break ;
			
		case BLOCKS_METAMORPHOSIZING:
			// does this have new blocks in 'pre'?
			append( sb, PREFIX_METAMORPHOSIS_PRE_HAS_NEW )	.write( oos, sb, mMetamorphosisPreIncludesNewBlocks ) .append( sb, NEWLINE ) ;
			break ;
			
		case BLOCKS_PUSHING_ROWS:
			// # of new rows
			append( sb, PREFIX_NUM_ROWS ) ;
			write( oos, sb, mNumRowsPushedDown ).write( oos, sb, mNumRowsPushedUp ).write( oos, sb, mNumRowsTransferredIn ) ;
			write( oos, sb, mNumRowsPushedUpThatAreGarbage ).append( sb, NEWLINE ) ;
			break ;
		}
		
		// postfix
		append( sb, SPECIAL_CLOSE ).append( sb, NEWLINE ) ;
	}
	
	
	private void writeBlockFields( GameBlocksSlice gbs, ObjectOutputStream oos, StringBuilder sb ) throws IOException {
		
		// prefix
		append( sb, FIELDS_OPEN ) ;
		
		// some vars
		boolean compareStable ;
		boolean compareSame ;
		boolean comparePiece ;
		boolean compareChunks ;
		
		int gbsEdge = gbs == null ? 0 : gbs.mEdge ;
		
		// The blockfields we write will be entirely determined by slice.
		// In fact, slice determines which (if any) blockfield will be written.
		switch( mBlocksState ) {
		case BLOCKS_PIECE_PREVIEW:
			// compare stable against stable and, if gbs is also a preview, compare piece.
			writeBlockField( getBlockfieldStable(), mEdge, gbs == null ? null : gbs.getBlockfieldStable(), 	gbsEdge, oos, sb ) ;
			comparePiece = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_PREVIEW ;
			if ( comparePiece )
				writeBlockField( getBlockfieldMinimumProfile(), mEdge, gbs.getBlockfieldMinimumProfile(), 	gbsEdge, oos, sb ) ;
			else
				writeBlockField( getBlockfieldMinimumProfile(), mEdge, null, 	gbsEdge, oos, sb ) ;
			break ;
		
		case BLOCKS_STABLE:
			// We compare stable against stable.
			writeBlockField( getBlockfieldStable(), mEdge, gbs == null ? null : gbs.getBlockfieldStable(), 	gbsEdge, oos, sb ) ;
			break ;
			
		case BLOCKS_PIECE_FALLING:
			// Compare stable
			writeBlockField( getBlockfieldStable(), mEdge, gbs == null ? null : gbs.getBlockfieldStable(), 	gbsEdge, oos, sb ) ;
			// compare field (w/o piece)
			compareStable = gbs != null && gbs.mBlocksState != GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			writeBlockField( getPieceFallingBlockfield(), mEdge, 
					(compareStable ? gbs.getBlockfieldStable() : compareSame ? gbs.getPieceFallingBlockfield() : null),	gbsEdge,
					oos, sb ) ;
			// piece
			writeBlockField( getPieceFallingPiece(), mEdge, compareSame ? gbs.getPieceFallingPiece() : null, 	gbsEdge, oos, sb ) ;
			// chunks!
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			compareChunks = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING ;
			for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
				if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					writeBlockField( getPieceFallingGhosts()[i], 	mEdge, gbs.getPieceFallingGhosts()[i], 	gbsEdge, oos, sb ) ;
				else if ( compareChunks && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					writeBlockField( getPieceFallingGhosts()[i], 	mEdge, gbs.getChunks()[i], 	gbsEdge, oos, sb ) ;
				else
					writeBlockField( getPieceFallingGhosts()[i], 	mEdge, null, 				gbsEdge, oos, sb ) ;
			}
			break ;
			
		case BLOCKS_CHUNKS_FALLING:
			// Compare blockfields.  If gbs is the same, compare blockfields directly (up to the limit
			// of the chunks present).  If gbs is different, compare against stable.
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING ;
			for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
				if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					writeBlockField( getBlockfields()[i], mEdge, gbs.getBlockfields()[i], gbsEdge, oos, sb ) ;
				else if ( !compareSame && gbs != null && getBlockfields()[i] == getBlockfieldStable() )
					writeBlockField( getBlockfields()[i], mEdge, gbs.getBlockfieldStable(), gbsEdge, oos, sb ) ;
				else
					writeBlockField( getBlockfields()[i], mEdge, null, gbsEdge, oos, sb ) ;
			}
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING ;
			comparePiece = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			// compare piece falling vs. chunks falling
			for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
				if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					writeBlockField( getChunks()[i], mEdge, gbs.getChunks()[i], gbsEdge, oos, sb ) ;
				else if ( comparePiece && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					writeBlockField( getChunks()[i], mEdge, gbs.getPieceFallingGhosts()[i], gbsEdge, oos, sb ) ;
				else
					writeBlockField( getChunks()[i], mEdge, null, gbsEdge, oos, sb ) ;
			}
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING && gbs.mIncludesAggregatedChunks ;
			if ( mIncludesAggregatedChunks ) {
				for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
					if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
						writeBlockField( getChunksAggregated()[i], mEdge, gbs.getChunksAggregated()[i], gbsEdge, oos, sb ) ;
					else 
						writeBlockField( getChunksAggregated()[i], mEdge, null, gbsEdge, oos, sb ) ;
				}
			}
			// chunks, clear and metamorphosis have 'pre'.
			if ( gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING )
				writeBlockField( getChunksFallingPreBlockfield(), mEdge, gbs.getChunksFallingPreBlockfield(), 	gbsEdge, oos, sb ) ;
			else if ( gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_ROWS_CLEARING )
				writeBlockField( getChunksFallingPreBlockfield(), mEdge, gbs.getClearPreBlockfield(), 			gbsEdge, oos, sb ) ;
			else if ( gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_METAMORPHOSIZING )
				writeBlockField( getChunksFallingPreBlockfield(), mEdge, gbs.getMetamorphosisPreBlockfield(), 	gbsEdge, oos, sb ) ;
			else
				writeBlockField( getChunksFallingPreBlockfield(), mEdge, null, gbsEdge, oos, sb ) ;
			break ;
			
		case BLOCKS_ROWS_CLEARING:
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_ROWS_CLEARING ;
			// pre, post, cleared
			if ( compareSame ) {
				writeBlockField( getClearPreBlockfield(), 		mEdge, gbs.getClearPreBlockfield(), 	gbsEdge, oos, sb ) ;
				writeBlockField( getClearPostBlockfield(), 		mEdge, gbs.getClearPostBlockfield(), 	gbsEdge, oos, sb ) ;
				writeBlockField( this.getClearClearedBlocks(), 	mEdge, gbs.getClearClearedBlocks(), 	gbsEdge, oos, sb ) ;
			} else if ( gbs != null ) {
				writeBlockField( getClearPreBlockfield(), 		mEdge, gbs.getBlockfieldStable(), 		gbsEdge, oos, sb ) ;
				writeBlockField( getClearPostBlockfield(), 		mEdge, gbs.getBlockfieldStable(), 		gbsEdge, oos, sb ) ;
				writeBlockField( this.getClearClearedBlocks(), 	mEdge, gbs.getBlockfieldStable(), 		gbsEdge, oos, sb ) ;
			} else {
				writeBlockField( getClearPreBlockfield(), 		mEdge, null, 	gbsEdge, oos, sb ) ;
				writeBlockField( getClearPostBlockfield(), 		mEdge, null, 	gbsEdge, oos, sb ) ;
				writeBlockField( getClearClearedBlocks(), 		mEdge, null, 	gbsEdge, oos, sb ) ;
			}
			break ;
			
		case BLOCKS_METAMORPHOSIZING:
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_METAMORPHOSIZING ;
			if ( compareSame ) {
				writeBlockField( getMetamorphosisPreBlockfield(), 	mEdge, gbs.getMetamorphosisPreBlockfield(), 	gbsEdge, oos, sb ) ;
				writeBlockField( getMetamorphosisPostBlockfield(), 	mEdge, gbs.getMetamorphosisPostBlockfield(), 	gbsEdge, oos, sb ) ;
			} else if ( gbs != null ) {
				writeBlockField( getMetamorphosisPreBlockfield(), 	mEdge, gbs.getBlockfieldStable(), 	gbsEdge, oos, sb ) ;
				writeBlockField( getMetamorphosisPostBlockfield(), 	mEdge, gbs.getBlockfieldStable(), 	gbsEdge ,oos, sb ) ;
			} else {
				writeBlockField( getMetamorphosisPreBlockfield(), 	mEdge, null, 	gbsEdge ,oos, sb ) ;
				writeBlockField( getMetamorphosisPostBlockfield(), 	mEdge, null, 	gbsEdge, oos, sb ) ;
			}
			break ;
			
		case BLOCKS_PUSHING_ROWS:
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PUSHING_ROWS ;
			if ( compareSame ) {
				writeBlockField( getPushingRowsPreBlockfield(),		mEdge, gbs.getPushingRowsPreBlockfield(),		gbsEdge, oos, sb ) ;
				writeBlockField( getPushingRowsPostBlockfield(),	mEdge, gbs.getPushingRowsPostBlockfield(),		gbsEdge, oos, sb ) ;
			} else if ( gbs != null ) {
				writeBlockField( getPushingRowsPreBlockfield(),		mEdge, gbs.getBlockfieldStable(),		gbsEdge, oos, sb ) ;
				writeBlockField( getPushingRowsPostBlockfield(),	mEdge, gbs.getBlockfieldStable(),		gbsEdge, oos, sb ) ;
			} else {
				writeBlockField( getPushingRowsPreBlockfield(),		mEdge, null,		gbsEdge, oos, sb ) ;
				writeBlockField( getPushingRowsPostBlockfield(),	mEdge, null,		gbsEdge, oos, sb ) ;
			}
			break ;
		}
		
		// prefix
		append( sb, FIELDS_CLOSE ) ;
	}
	
	
	private void writeBlockField( byte [][][] bf, int edge, byte [][][] bf_pre, int edge_pre, ObjectOutputStream oos, StringBuilder sb ) throws IOException {
		// prefix
		append( sb, FIELD_OPEN ).append( sb, NEWLINE ) ;
		
		// write!
		if ( bf_pre == null || edge != edge_pre ) {
			if ( oos != null )
				QSerialization.write(bf, edge, bf[0].length - edge, edge, bf[0][0].length - edge, oos) ;
			if ( sb != null )
				sb.append( QSerialization.toString(bf, edge, bf[0].length - edge, edge, bf[0][0].length - edge) ).append(" ") ;
		} else {
			if ( oos != null )
				QSerialization.write(bf, bf_pre, edge, bf[0].length - edge, edge, bf[0][0].length - edge, oos) ;
			if ( sb != null )
				sb.append( QSerialization.toString(bf, bf_pre, edge, bf[0].length - edge, edge, bf[0][0].length - edge)).append(" ") ;
		}
		
		// postfix
		append( sb, FIELD_CLOSE ).append( sb, NEWLINE ) ;
	}
	
	
	private GameBlocksSlice write( ObjectOutputStream oos, StringBuilder sb, int num ) throws IOException {
		if ( oos != null )
			oos.writeInt(num) ;
		if ( sb != null )
			sb.append(num).append(" ") ;
		return this ;
	}
	
	private GameBlocksSlice write( ObjectOutputStream oos, StringBuilder sb, boolean b ) throws IOException {
		if ( oos != null )
			oos.writeBoolean(b) ;
		if ( sb != null )
			sb.append(b).append(" ") ;
		return this ;
	}
	
	private GameBlocksSlice write( ObjectOutputStream oos, StringBuilder sb, Offset o ) throws IOException {
		if ( oos != null ) {
			oos.writeInt(o.x) ;
			oos.writeInt(o.y) ;
		}
		if ( sb != null ) {
			sb.append(o.x).append(":") ;
			sb.append(o.y).append(" ") ;
		}
		return this ;
	}
	
	private GameBlocksSlice append( StringBuilder sb, String s ) {
		if ( sb != null )
			sb.append(s).append(" ") ;
		return this ;
	}
	
	private GameBlocksSlice append( StringBuilder sb, int i ) {
		if ( sb != null )
			sb.append(i).append(" ") ;
		return this ;
	}

	
	
	public void read( ObjectInputStream ois ) throws IOException {
		read( null, ois, null, false ) ;
		boundAllLooselyBoundedArrays() ;
	}
	
	public void fromString( String str ) throws IOException {
		read( null, null, str, false ) ;
		boundAllLooselyBoundedArrays() ;
	}
	
	public void readAsSliceAfter( GameBlocksSlice gbs, ObjectInputStream ois ) throws IOException {
		read( gbs, ois, null, false ) ;
		boundAllLooselyBoundedArrays() ;
	}
	
	public void fromStringAsSliceAfter( GameBlocksSlice gbs, String str ) throws IOException {
		read( gbs, null, str, false ) ;
		boundAllLooselyBoundedArrays() ;
	}
	
	private void read( GameBlocksSlice gbs, ObjectInputStream ois, String str, boolean allocate ) throws IOException {
		
		// get "section strings"
		String strMeta = substringWithin( str, META_OPEN, META_CLOSE ) ;
		String strSpecial = substringWithin( str, SPECIAL_OPEN, SPECIAL_CLOSE ) ;
		String strFields = substringWithin( str, FIELDS_OPEN, FIELDS_CLOSE ) ;
		
		////////////////////////////////////////////////////////////////////////
		// PREAMBLE
		// Take the metadata.
		readMetaData( ois, strMeta, allocate ) ;
		
		////////////////////////////////////////////////////////////////////////
		// SPECIAL DATA
		// Certain block states will require additional data, which we don't
		// bother writing for other types.
		readSpecialData( ois, strSpecial ) ;
		
		////////////////////////////////////////////////////////////////////////
		// BLOCK FIELDS
		// Certain block states will require additional data, which we don't
		// bother writing for other types.
		readBlockFields( gbs, ois, strFields ) ;
	}
	
	
	private void readMetaData( ObjectInputStream ois, String str, boolean allocate ) throws IOException {
		String [] strArray = null ;
		int index = 0 ;
		if ( str != null ) {
			strArray = str.split("\\s+") ;
			while ( strArray[index].length() == 0 )
				index++ ;
		}
		
		int state, R, C, num, edge, pieceType ;
		
		eat( PREFIX_STATE, strArray, index++ ) ;		state = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_R, strArray, index++ ) ;			R = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_C, strArray, index++ ) ;			C = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_NUM, strArray, index++ ) ;			num = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_EDGE, strArray, index++ ) ;			edge = readInt( ois, strArray, index++ ) ;
		eat( PREFIX_PIECE_TYPE, strArray, index++ ) ;	pieceType = readInt( ois, strArray, index++ ) ;
		
		// compare against our dimensions - or, alternately, allocate.
		if ( allocate ) {
			mR = R ;
			mC = C ;
			mNum = num ;
			mEdge = edge ;
			
			mBlockfields = new byte[num+1][2][R+edge*2][C+edge*2] ;
			mPreBlockfield = new byte[2][R+edge*2][C+edge*2] ;
			
			mChunks = new byte[num+1][2][R+edge*2][C+edge*2] ;
			mChunksAggregated = new byte[num+1][2][R+edge*2][C+edge*2] ;
			
			createLooselyBoundedArrays() ;
			
			mFallDistances = new int[num+1] ;
			mChunkIsNewToBlockField = new boolean[num+1] ;
			for ( int i =0; i < num+1; i++ )
				mUnlockedColumns.add(new Offset()) ;
			
			mClears = new int[mR + mEdge] ;
			mMonochromeClears = new boolean[mR + mEdge] ;
			
		} else {
			if ( mR != R )
				throw new IllegalArgumentException("Bad row dimensions") ;
			if ( mC != C )
				throw new IllegalArgumentException("Bad column dimensions") ;
			if ( mNum != num )
				throw new IllegalArgumentException("Bad number") ;
			if ( mEdge != edge )
				throw new IllegalArgumentException("Bad number") ;
			
		}
		
		mBlocksState = state ;
		mPieceType = pieceType ;
	}
	
	
	private void readSpecialData( ObjectInputStream ois, String str ) throws IOException {
		String [] strArray = null ;
		int index = 0 ;
		if ( str != null ) {
			strArray = str.split("\\s+") ;
			while ( index < strArray.length && strArray[index].length() == 0 )
				index++ ;
		}
		
		switch( mBlocksState ) {
		case BLOCKS_PIECE_PREVIEW:
			// nothing
			break ;
		case BLOCKS_STABLE:
			// nothing
			break ;
		case BLOCKS_PIECE_FALLING:
			// first and num chunks
			eat( PREFIX_FIRST_CHUNK, strArray, index++ ) ;	mFirstChunk = readInt( ois, strArray, index++ ) ;
			eat( PREFIX_NUM_CHUNKS, strArray, index++ ) ;	mNumChunks = readInt( ois, strArray, index++ ) ;
			break ;
		case BLOCKS_CHUNKS_FALLING:
			// first chunk, num chunks, isPieceComponent?, fall distances, num columns, columnOffsets.
			eat( PREFIX_FIRST_CHUNK, strArray, index++ ) ;		mFirstChunk = readInt( ois, strArray, index++ ) ;
			eat( PREFIX_NUM_CHUNKS, strArray, index++ ) ;		mNumChunks = readInt( ois, strArray, index++ ) ;
			eat( PREFIX_PIECE_COMPONENTS, strArray, index++ ) ;	mChunksArePieceComponents = readBoolean( ois, strArray, index++ ) ;
			eat( PREFIX_INCLUDES_AGGREGATED_CHUNKS, strArray, index++ ) ; mIncludesAggregatedChunks = readBoolean( ois, strArray, index++ ) ;
			eat( PREFIX_FALL_DISTANCES, strArray, index++ ) ;
			for ( int i = 0; i < mNumChunks; i++ )
				mFallDistances[i + mFirstChunk] = readInt( ois, strArray, index++ ) ;
			eat( PREFIX_IS_NEW, strArray, index++ ) ;
			for ( int i = 0; i < mNumChunks; i++ )
				mChunkIsNewToBlockField[i + mFirstChunk] = readBoolean( ois, strArray, index++ ) ;
			eat( PREFIX_NUM_UNLOCKED_COLUMNS, strArray, index++ ) ;		mNumUnlockedColumns = readInt( ois, strArray, index++ ) ;
			eat( PREFIX_UNLOCKED_COLUMNS, strArray, index++ ) ;
			for ( int i = 0; i < mNumUnlockedColumns; i++ )
				readOffset( mUnlockedColumns.get(i), ois, strArray, index++ ) ;
			break ;
		case BLOCKS_ROWS_CLEARING:
			// cascade number
			eat ( PREFIX_CLEAR_CASCADE_NUMBER, strArray, index++ ) ;	mClearCascadeNumber = readInt( ois, strArray, index++ ) ;
			// clears, monoclears
			eat( PREFIX_CLEARS, strArray, index++ ) ;
			for ( int i = mEdge; i < mR + mEdge; i++ )
				mClears[i] = readInt( ois, strArray, index++ ) ;
			eat( PREFIX_MONOCLEARS, strArray, index++ ) ;
			for ( int i = mEdge; i < mR + mEdge; i++ )
				mMonochromeClears[i] = readBoolean( ois, strArray, index++ ) ;
			break ;
		case BLOCKS_METAMORPHOSIZING:
			// does this have new blocks in 'pre'?
			eat( PREFIX_METAMORPHOSIS_PRE_HAS_NEW, strArray, index++ ) ;	mMetamorphosisPreIncludesNewBlocks = readBoolean( ois, strArray, index++ ) ;
			break ;
			
		case BLOCKS_PUSHING_ROWS:
			// # of new rows
			eat( PREFIX_NUM_ROWS, strArray, index++ ) ;
			mNumRowsPushedDown = readInt( ois, strArray, index++ ) ;
			mNumRowsPushedUp = readInt( ois, strArray, index++ ) ;
			mNumRowsTransferredIn = readInt( ois, strArray, index++ ) ;
			mNumRowsPushedUpThatAreGarbage = readInt( ois, strArray, index++ ) ;
			break ;
		}
	}
	
	
	private void readBlockFields( GameBlocksSlice gbs, ObjectInputStream ois, String strFields ) throws IOException {
		
		String [] fieldStr = null ;
		
		if ( strFields != null ) {
			int num = -1 ;
			int lastIndex = 0 ;
			while ( lastIndex != -1 ) {
				lastIndex = strFields.indexOf( FIELD_CLOSE, lastIndex+1 ) ;
				num++ ;
			}
			
			fieldStr = new String[num] ;
			int startIndex = 0 ;
			for ( int i = 0; i < num; i++ ) {
				fieldStr[i] = substringWithin( strFields, FIELD_OPEN, FIELD_CLOSE, startIndex ) ;
				startIndex = strFields.indexOf( FIELD_CLOSE, startIndex+1 ) ;
			}
		}
		
		int index = 0 ;
		boolean compareStable, compareSame, comparePiece, compareChunks ;
		
		// if provided with a string of fields, now have individual fields represented
		// as strings.
		
		int gbsEdge = gbs == null ? 0 : gbs.mEdge ;
		switch ( mBlocksState ) {
		case BLOCKS_PIECE_PREVIEW:
			// Compare stable against stable, and if a piece preview, preview
			// against preview.
			readBlockField( getBlockfieldStable(), mEdge, gbs == null ? null : gbs.getBlockfieldStable(), gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			comparePiece = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_PREVIEW ;
			if ( comparePiece )
				readBlockField( getBlockfieldMinimumProfile(), mEdge, gbs.getBlockfieldMinimumProfile(), gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			else
				readBlockField( getBlockfieldMinimumProfile(), mEdge, null, gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			break ;
			
		case BLOCKS_STABLE:
			// We compare stable against stable.
			readBlockField( getBlockfieldStable(), mEdge, gbs == null ? null : gbs.getBlockfieldStable(), gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			break ;
			
		case BLOCKS_PIECE_FALLING:
			// Compare stable
			readBlockField( getBlockfieldStable(), mEdge, gbs == null ? null : gbs.getBlockfieldStable(), gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			compareStable = gbs != null && gbs.mBlocksState != GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			readBlockField( getPieceFallingBlockfield(), mEdge, 
					(compareStable ? gbs.getBlockfieldStable() : compareSame ? gbs.getPieceFallingBlockfield() : null),	gbsEdge,
					ois, fieldStr == null ? null : fieldStr[index++] ) ;
			// piece
			readBlockField( getPieceFallingPiece(), mEdge, compareSame ? gbs.getPieceFallingPiece() : null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			// ghosts!
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			compareChunks = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING ;
			for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
				if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					readBlockField( getPieceFallingGhosts()[i], 	mEdge, gbs.getPieceFallingGhosts()[i], 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				else if ( compareChunks && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					readBlockField( getPieceFallingGhosts()[i], 	mEdge, gbs.getChunks()[i], 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				else
					readBlockField( getPieceFallingGhosts()[i], 	mEdge, null, 				gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			}
			break ;
			
		case BLOCKS_CHUNKS_FALLING:
			// Compare blockfields.  If gbs is the same, compare blockfields directly (up to the limit
			// of the chunks present).  If gbs is different, compare against stable.
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING ;
			for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
				if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					readBlockField( getBlockfields()[i], mEdge, gbs.getBlockfields()[i], gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				else if ( !compareSame && gbs != null && getBlockfields()[i] == getBlockfieldStable() )
					readBlockField( getBlockfields()[i], mEdge, gbs.getBlockfieldStable(), gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				else
					readBlockField( getBlockfields()[i], mEdge, null, gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			}
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING ;
			comparePiece = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PIECE_FALLING ;
			// compare piece falling vs. chunks falling
			for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
				if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					readBlockField( getChunks()[i], mEdge, gbs.getChunks()[i], gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				else if ( comparePiece && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
					readBlockField( getChunks()[i], mEdge, gbs.getPieceFallingGhosts()[i], gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				else
					readBlockField( getChunks()[i], mEdge, null, gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			}
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING && gbs.mIncludesAggregatedChunks ;
			if ( mIncludesAggregatedChunks ) {
				for ( int i = mFirstChunk; i < mNumChunks + mFirstChunk; i++ ) {
					if ( compareSame && gbs.mFirstChunk <= i && i < gbs.mFirstChunk + gbs.mNumChunks )
						readBlockField( getChunksAggregated()[i], mEdge, gbs.getChunksAggregated()[i], gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
					else
						readBlockField( getChunksAggregated()[i], mEdge, null, gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				}
			}
			// chunks, clear and metamorphosis have pre.
			if ( gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_CHUNKS_FALLING )
				readBlockField( getChunksFallingPreBlockfield(), mEdge, gbs.getChunksFallingPreBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			else if ( gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_ROWS_CLEARING )
				readBlockField( getChunksFallingPreBlockfield(), mEdge, gbs.getClearPreBlockfield(), 			gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			else if ( gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_METAMORPHOSIZING )
				readBlockField( getChunksFallingPreBlockfield(), mEdge, gbs.getMetamorphosisPreBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			else
				readBlockField( getChunksFallingPreBlockfield(), mEdge, null, gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			break ;
			
		case BLOCKS_ROWS_CLEARING:
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_ROWS_CLEARING ;
			// pre, post, cleared
			if ( compareSame ) {
				readBlockField( getClearPreBlockfield(), 		mEdge, gbs.getClearPreBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getClearPostBlockfield(), 		mEdge, gbs.getClearPostBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( this.getClearClearedBlocks(), 	mEdge, gbs.getClearClearedBlocks(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			} else if ( gbs != null ) {
				readBlockField( getClearPreBlockfield(), 		mEdge, gbs.getBlockfieldStable(), 		gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getClearPostBlockfield(), 		mEdge, gbs.getBlockfieldStable(), 		gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( this.getClearClearedBlocks(), 	mEdge, gbs.getBlockfieldStable(), 		gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			} else {
				readBlockField( getClearPreBlockfield(), 		mEdge, null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getClearPostBlockfield(), 		mEdge, null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getClearClearedBlocks(), 		mEdge, null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			}
			break ;
			
		case BLOCKS_METAMORPHOSIZING:
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_METAMORPHOSIZING ;
			if ( compareSame ) {
				readBlockField( getMetamorphosisPreBlockfield(), 	mEdge, gbs.getMetamorphosisPreBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getMetamorphosisPostBlockfield(), 	mEdge, gbs.getMetamorphosisPostBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			} else if ( gbs != null ) {
				readBlockField( getMetamorphosisPreBlockfield(), 	mEdge, gbs.getBlockfieldStable(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getMetamorphosisPostBlockfield(), 	mEdge, gbs.getBlockfieldStable(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			} else {
				readBlockField( getMetamorphosisPreBlockfield(), 	mEdge, null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getMetamorphosisPostBlockfield(), 	mEdge, null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			}
			break ;
			
		case BLOCKS_PUSHING_ROWS:
			compareSame = gbs != null && gbs.mBlocksState == GameBlocksSlice.BLOCKS_PUSHING_ROWS ;
			if ( compareSame ) {
				readBlockField( getPushingRowsPreBlockfield(), 		mEdge, gbs.getPushingRowsPreBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getPushingRowsPostBlockfield(), 	mEdge, gbs.getPushingRowsPostBlockfield(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			} else if ( gbs != null ) {
				readBlockField( getPushingRowsPreBlockfield(), 	mEdge, gbs.getBlockfieldStable(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getPushingRowsPostBlockfield(), mEdge, gbs.getBlockfieldStable(), 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			} else {
				readBlockField( getPushingRowsPreBlockfield(), 	mEdge, null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
				readBlockField( getPushingRowsPostBlockfield(), mEdge, null, 	gbsEdge, ois, fieldStr == null ? null : fieldStr[index++] ) ;
			}
			break ;
		}
	}
	
	private void readBlockField( byte [][][] bf, int edge, byte [][][] bf_pre, int edge_pre, ObjectInputStream ois, String str ) throws IOException {
		// read!
		if ( bf_pre == null || edge != edge_pre ) {
			if ( ois != null )
				QSerialization.read(bf, edge, bf[0].length - edge, edge, bf[0][0].length - edge, ois) ;
			if ( str != null )
				QSerialization.read(bf, edge, bf[0].length - edge, edge, bf[0][0].length - edge, str) ;
		} else {
			if ( ois != null )
				QSerialization.read(bf, bf_pre, edge, bf[0].length - edge, edge, bf[0][0].length - edge, ois) ;
			if ( str != null )
				QSerialization.read(bf, bf_pre, edge, bf[0].length - edge, edge, bf[0][0].length - edge, str) ;
		}
	}
	
	private String substringWithin( String src, String left, String right ) {
		return substringWithin( src, left, right, 0 ) ;
	}
	
	private String substringWithin( String src, String left, String right, int startIndex ) {
		if ( src == null )
			return null ;
		
		int leftIndex = src.indexOf(left, startIndex) ;
		int rightIndex = src.indexOf(right, leftIndex+1) ;
		
		if ( leftIndex == -1 || rightIndex == -1 ) {
			System.err.println(src) ;
			throw new IllegalArgumentException("Provided string does not have tag(s) " + left + ", " + right) ;
		}
		
		return src.substring( leftIndex + left.length(), rightIndex ) ;
	}
	
	
	/**
	 * Reads and returns an integer value represented in the provided input objects.
	 * For string representations, both explicit integers and QCombinations "string"
	 * representations are acceptable.
	 * 
	 * If exactly one provided input source is non-null, it will be read from; in
	 * more than one is non-null, than exactly one will be read from and it will
	 * be deterministically chosen, but we do not specify which, so this may change
	 * between implementations.
	 * 
	 * Throws an exception if a valid integer is not found in the non-null input stream
	 * from which we read, or if all inputs are null.
	 * 
	 * @param ois
	 * @param strArray
	 * @param strArrayIndex
	 * @return
	 * @throws IOException 
	 */
	private final int readInt( ObjectInputStream ois, String [] strArray, int strArrayIndex ) throws IOException {
		
		if ( ois != null )
			return ois.readInt() ;
		
		if ( strArray != null ) {
			return Integer.parseInt( strArray[strArrayIndex] ) ;
		}
		
		throw new NullPointerException("Only null object inputs given") ;
	}
	
	
	/**
	 * Reads and returns a boolean value represented in the provided input objects.
	 * For string representations, both explicit integers and QCombinations "string"
	 * representations are acceptable.
	 * 
	 * If exactly one provided input source is non-null, it will be read from; in
	 * more than one is non-null, than exactly one will be read from and it will
	 * be deterministically chosen, but we do not specify which, so this may change
	 * between implementations.
	 * 
	 * Throws an exception if a valid integer is not found in the non-null input stream
	 * from which we read, or if all inputs are null.
	 * 
	 * @param ois
	 * @param strArray
	 * @param strArrayIndex
	 * @return
	 * @throws IOException 
	 */
	private final boolean readBoolean( ObjectInputStream ois, String [] strArray, int strArrayIndex ) throws IOException {
		
		if ( ois != null )
			return ois.readBoolean() ;
		
		if ( strArray != null ) {
			return Boolean.parseBoolean(strArray[strArrayIndex]) ;
		}
		
		throw new NullPointerException("Only null object inputs given") ;
	}
	
	
	/**
	 * Reads and returns a boolean value represented in the provided input objects.
	 * For string representations, both explicit integers and QCombinations "string"
	 * representations are acceptable.
	 * 
	 * If exactly one provided input source is non-null, it will be read from; in
	 * more than one is non-null, than exactly one will be read from and it will
	 * be deterministically chosen, but we do not specify which, so this may change
	 * between implementations.
	 * 
	 * Throws an exception if a valid integer is not found in the non-null input stream
	 * from which we read, or if all inputs are null.
	 * 
	 * @param ois
	 * @param strArray
	 * @param strArrayIndex
	 * @return
	 * @throws IOException 
	 */
	private final void readOffset( Offset o, ObjectInputStream ois, String [] strArray, int strArrayIndex ) throws IOException {
		
		if ( ois != null ) {
			o.x = ois.readInt();
			o.y = ois.readInt();
			return ;
		}
		
		if ( strArray != null ) {
			int colonIndex = strArray[strArrayIndex].indexOf(":") ;
			o.x = Integer.parseInt( strArray[strArrayIndex].substring(0, colonIndex) ) ;
			o.y = Integer.parseInt( strArray[strArrayIndex].substring(colonIndex+1) ) ;
			return ;
		}
		
		throw new NullPointerException("Only null object inputs given") ;
	}
	
	
	private void eat( String expected, String [] strArray, int strArrayIndex ) {
		if ( strArray == null )
			return ;
		if ( !expected.equals(strArray[strArrayIndex]) )
			throw new IllegalArgumentException("Expected string " + expected + " not found; instead " + ( (strArray == null || strArrayIndex < 0 || strArray.length <= strArrayIndex) ? "invalid size" : strArray[strArrayIndex] ) ) ;
	}
	
	

	
}
