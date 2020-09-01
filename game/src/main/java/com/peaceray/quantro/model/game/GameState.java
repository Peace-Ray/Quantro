package com.peaceray.quantro.model.game;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.utils.ArrayOps;

/**
 * Game grew monolithicly from a far simpler class used in the original 
 * flash prototype.  I was going to try to make some kind of SkyNet
 * reference here but nothing seems appropriate.
 * 
 * Because of this, it has a very large and complex set of state variables,
 * only some of which are relevant at any given time (for example,
 * while components.size() will grow monotonically until it reaches the
 * minimum state required to hold game state information, at any given time
 * only the first 'numComponents' entries are relevant, and only they
 * should be considered as part of the current state).
 * 
 * The Game object 
 * 
 * @author Jake
 *
 */
public class GameState implements Serializable {
	
	// DO NOT EVER, EVER, EVER, EVER CHANGE THIS MAGIC NUMBER
	private static final int THIS_IS_VERSIONED_CODE = 1234567 ;
		// DO NOT EVER, EVER, EVER, EVER CHANGE THIS MAGIC NUMBER
	
	
	
	private static final int VERSION = 4 ;
	// 0: adds numBlocksForJunctions and other numBlocks fields
	// 1: removes "blockfieldBeforeMetamorphosis;" will use "blockfieldBefore" instead.
	//		also adds 'numberOfRowsPushedDown' (and renames 'numRowsAdded' to 'numPushedUp'),
	//		also adds 'deactivateBeforeEndCycle ; activateBeforeEndCycle'.
	// 2: adds chunkIsNewToBlockField.
	// 3: adds a blockfield for displacement rows and a place for measuring the
	//		number recently transferred.
	// 4: adds an explicit field for the number of rows pushed in which are themselves garbage.
	
	//private static final String TAG = "GameState" ;
	
	
	public static final int PIECE_LOOKAHEAD = 5 ;
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5481801383239090782L;
	
	
	// Some default initial values/sizes
	private static final int DEFAULT_INITIAL_SIZE_UNLOCK_COLUMN = 1 ;
	private static final int DEFAULT_INITIAL_SIZE_COMPONENTS = 4 ;
	private static final int DEFAULT_INITIAL_SIZE_CHUNKS = 16 ;
	
	// Member variables
	
	// Blockfield dimensions?
	public int R ;
	public int C ;
	
	// Piece that's currently falling?
	public Piece piece ;
	public Piece [] nextPieces ;
	public Piece [] reservePieces ;
	
	public Offset offset ;
	public boolean lockPiece ;		// This piece WILL be locked.
	public boolean lockThenActivate ;	// lock the piece - proceed through ALL steps - then activate.
	public boolean lockThenDeactivate ;	// lock the piece - proceed through ALL steps - then activate.
	public boolean activateThenEndCycle ;	// activate immediately before ending the cycle.
	public boolean deactivateThenEndCycle ;	// deactivates immediately before ending the cycle.
	public boolean usedReserve ;	// Have we used the reserve piece "this time?"  This is reset
							// when a piece LOCKS (so we can then use the reserve for the upcoming
							// piece during an animation).
	
	// Current state of the game?
	public int game_period ;		// prestart, ongoing, over
	public int state ;
	public int progressionState ;
	public int stateAfterProgression ;
	
	// Events since the last tick?
	public GameEvents geventsLastTick ;
	
	// Blockfield?
	public byte [][][] blockField ;
	public byte [][][] blockFieldBefore ;
	// Displacement blocks?
	public byte [][][] blockFieldDisplacementRows ;

	// Clear?
	public int clearCascadeNumber ;
	public int [] clearedRowsChromatic ;
	public boolean [] clearedRowsMonochromatic ;
	public byte [][][] blockFieldInverseClear ;
	
	// Unlock?
	public int numUnlockColumnAbove ;
	public int numUnlockColumnAboveAlreadyUnlocked ;
	public ArrayList<Offset> unlockColumnAbove ;
	// Column unlocked
	
	// Blocks to drop in valleys?
	public int numBlocksForValleys ;
	public int numBlocksForJunctions ;
	public int numBlocksForPeaks ;
	public int numBlocksForCorners ;
	public int numBlocksForTroll ;
	
	// Some arrays for storing piece components and chunks.
	public int numComponents ;
	public ArrayList<Piece> components ;
	public ArrayList<Offset> componentOriginalOffsets ;
	public ArrayList<Offset> componentFellOffsets ;
	public int numChunks ;
	public ArrayList<Piece> chunks ;
	public ArrayList<Offset> chunkOriginalOffsets ;
	public ArrayList<Offset> chunkFellOffsets ;
	public ArrayList<Boolean> chunkIsNewToBlockField ;
								// did this chunk "materialize?"  e.g., from a valley system?
	
	
	// Attacks?
	public boolean unleashAttackThisCycle ;
	
	public int numberOfRowsPushedUp ;			// For "push up" garbage and the like.
	public int numberOfRowsPushedDown ;
	public int numberOfRowsPushedUpThatAreGarbage ;
	
	// Displacement transfer.  Current displacement is handled by the System State;
	// this gives information about 1: upcoming transfers and 2: transfers that occurred.
	public int numberOfDisplacedRowsToTransferThisCycle ;
	public int numberOfDisplacedRowsTransferred ;
	
	// Is a reserve usage "queued up?"
	public boolean reserveQueuedForNextCycle ;
	
	// How many cycles have been completed?
	public long numActionCycles ;
	
	// Initializing constructor
	public GameState( int rows, int cols ) {
		this.R = rows*2 ;
		this.C = cols ;
		
		piece = new Piece() ;
		piece.type = -1 ;
		
		nextPieces = new Piece[PIECE_LOOKAHEAD] ;
		reservePieces = new Piece[PIECE_LOOKAHEAD] ;
		for ( int i = 0; i < PIECE_LOOKAHEAD; i++ ) {
			nextPieces[i] = new Piece() ;
			reservePieces[i] = new Piece() ;
			nextPieces[i].type = reservePieces[i].type = -1 ;
		}
		
		offset = new Offset() ;
		lockPiece = true ;
		lockThenActivate = false ;
		lockThenDeactivate = false ;
		activateThenEndCycle = false ;
		deactivateThenEndCycle = false ;
		usedReserve = false ;
		
		game_period = Game.PERIOD_PRESTART ;
		state = Game.STATE_INITIALIZING ;
		progressionState = Game.PROGRESSION_COMPONENTS_UNLOCK ;
		stateAfterProgression = Game.STATE_ENDING_CYCLE ;
		
		geventsLastTick = new GameEvents() ;
		geventsLastTick.finalizeConfiguration() ;
 		
		blockField = new byte[2][R][C] ;
		blockFieldBefore = new byte[2][R][C] ;
		blockFieldDisplacementRows = new byte[2][R][C] ;
		
		clearCascadeNumber = -1 ;
		clearedRowsChromatic = new int[R] ;
		clearedRowsMonochromatic = new boolean[R] ;
		blockFieldInverseClear = new byte[2][R][C] ;
		
		numUnlockColumnAbove = numUnlockColumnAboveAlreadyUnlocked = 0 ;
		unlockColumnAbove = new ArrayList<Offset>(DEFAULT_INITIAL_SIZE_UNLOCK_COLUMN) ;
		
		numBlocksForValleys = 0 ;
		numBlocksForJunctions = 0 ;
		numBlocksForPeaks = 0 ;
		numBlocksForCorners = 0 ;
		numBlocksForTroll = 0 ;
		
		numComponents = 0 ;
		components = new ArrayList<Piece>(DEFAULT_INITIAL_SIZE_COMPONENTS) ;
		componentOriginalOffsets = new ArrayList<Offset>(DEFAULT_INITIAL_SIZE_COMPONENTS) ;
		componentFellOffsets = new ArrayList<Offset>(DEFAULT_INITIAL_SIZE_COMPONENTS) ;
		numChunks = 0 ;
		chunks = new ArrayList<Piece>(DEFAULT_INITIAL_SIZE_CHUNKS) ;
		chunkOriginalOffsets = new ArrayList<Offset>(DEFAULT_INITIAL_SIZE_CHUNKS) ;
		chunkFellOffsets = new ArrayList<Offset>(DEFAULT_INITIAL_SIZE_CHUNKS) ;
		chunkIsNewToBlockField = new ArrayList<Boolean>(DEFAULT_INITIAL_SIZE_CHUNKS) ;
		
		unleashAttackThisCycle = false ;
		numberOfRowsPushedUp = 0 ;
		numberOfRowsPushedDown = -1 ;
		numberOfRowsPushedUpThatAreGarbage = 0 ;
		
		numberOfDisplacedRowsToTransferThisCycle = 0 ;
		numberOfDisplacedRowsTransferred = 0 ;
		
		reserveQueuedForNextCycle = false ;
		
		numActionCycles = 0 ;
	}
	
	
	public GameState( GameState gs ) {
		// Blockfield dimensions?
		R = gs.R ;
		C = gs.C ;
		
		// Piece that's currently falling?
		piece = new Piece( gs.piece.toString() ) ;
		nextPieces = new Piece[gs.nextPieces.length] ;
		for ( int i = 0; i < nextPieces.length; i++ )
			nextPieces[i] = new Piece( gs.nextPieces[i].toString() ) ;
		reservePieces = new Piece[gs.reservePieces.length] ;
		for ( int i = 0; i < reservePieces.length; i++ )
			reservePieces[i] = new Piece( gs.reservePieces[i].toString() ) ;
		
		offset = new Offset( gs.offset );
		lockPiece = gs.lockPiece ;
		lockThenActivate = gs.lockThenActivate ;
		lockThenDeactivate = gs.lockThenDeactivate ;
		activateThenEndCycle = gs.activateThenEndCycle ;
		deactivateThenEndCycle = gs.deactivateThenEndCycle ;
		usedReserve = gs.usedReserve ;
		
		// Current state of the game?
		game_period = gs.game_period ;
		state = gs.state ;
		progressionState = gs.progressionState ;
		stateAfterProgression = gs.stateAfterProgression ;
		
		// Events since the last tick?
		geventsLastTick = new GameEvents( gs.geventsLastTick );
		
		// Blockfield?
		blockField = ArrayOps.duplicate(gs.blockField);
		blockFieldBefore  = ArrayOps.duplicate(gs.blockFieldBefore);
		blockFieldDisplacementRows = ArrayOps.duplicate(gs.blockFieldDisplacementRows) ;
		
		// Clear?
		clearCascadeNumber = gs.clearCascadeNumber ;
		clearedRowsChromatic = ArrayOps.duplicate( gs.clearedRowsChromatic );
		clearedRowsMonochromatic = ArrayOps.duplicate(gs.clearedRowsMonochromatic);
		blockFieldInverseClear = ArrayOps.duplicate(gs.blockFieldInverseClear);
		
		// Unlock?
		numUnlockColumnAbove = gs.numUnlockColumnAbove ;
		numUnlockColumnAboveAlreadyUnlocked = gs.numUnlockColumnAboveAlreadyUnlocked ;
		unlockColumnAbove = new ArrayList<Offset>(numUnlockColumnAbove) ;
		for ( int i = 0; i < gs.numUnlockColumnAbove; i++ ) {
			Offset o = gs.unlockColumnAbove.get(i) ;
			unlockColumnAbove.add( new Offset( o.x, o.y ) ) ;
		}
		
		// Blocks to drop in valleys?
		numBlocksForValleys = gs.numBlocksForValleys ;
		numBlocksForJunctions = gs.numBlocksForJunctions ;
		numBlocksForPeaks = gs.numBlocksForPeaks ;
		numBlocksForCorners = gs.numBlocksForCorners ;
		numBlocksForTroll = gs.numBlocksForTroll ;
		
		// Some arrays for storing piece components and chunks.
		numComponents = gs.numComponents ;
		components = new ArrayList<Piece>(numComponents) ;
		componentOriginalOffsets = new ArrayList<Offset>(numComponents) ;
		componentFellOffsets = new ArrayList<Offset>(numComponents) ;
		for ( int i = 0; i < numComponents; i++ ) {
			components.add( new Piece( gs.components.get(i).toString() )) ;
			componentOriginalOffsets.add( new Offset( gs.componentOriginalOffsets.get(i) )) ;
			componentFellOffsets.add( new Offset( gs.componentFellOffsets.get(i) )) ;
			
		}
		
		numChunks = gs.numChunks ;
		chunks = new ArrayList<Piece>(numChunks) ;
		chunkOriginalOffsets = new ArrayList<Offset>(numChunks) ;
		chunkFellOffsets = new ArrayList<Offset>(numChunks) ;
		chunkIsNewToBlockField = new ArrayList<Boolean>(numChunks) ;
		for ( int i = 0; i < numChunks; i++ ) {
			chunks.add( new Piece( gs.chunks.get(i).toString() )) ;
			chunkOriginalOffsets.add( new Offset( gs.chunkOriginalOffsets.get(i) )) ;
			chunkFellOffsets.add( new Offset( gs.chunkFellOffsets.get(i) )) ;
			chunkIsNewToBlockField.add( gs.chunkIsNewToBlockField.get(i) ) ;
		}
		
		// Attacks?
		unleashAttackThisCycle = gs.unleashAttackThisCycle ;
		
		numberOfRowsPushedUp = gs.numberOfRowsPushedUp ;			// For "push up" garbage and the like.
		numberOfRowsPushedDown = gs.numberOfRowsPushedDown ;
		numberOfRowsPushedUpThatAreGarbage = gs.numberOfRowsPushedUpThatAreGarbage ;
		
		numberOfDisplacedRowsToTransferThisCycle = gs.numberOfDisplacedRowsToTransferThisCycle ;
		numberOfDisplacedRowsTransferred = gs.numberOfDisplacedRowsTransferred ;
		
		reserveQueuedForNextCycle = gs.reserveQueuedForNextCycle ;
		
		numActionCycles = gs.numActionCycles ;
	}
	
	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE 
	//
	// These methods provide the implementation of the Serializable
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
	
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		
		// Indicate with a magic number that this is versioned, and then
		// provide the version.
		stream.writeInt(THIS_IS_VERSIONED_CODE) ;
		stream.writeInt(VERSION) ;
		
		// Store all the details.
		// This method is very similar to writeToParcel, with a
		// few differences based on the different capabilities of
		// the systems.
		
		// Store R,C
		stream.writeInt(R) ;
		stream.writeInt(C) ;
		
		// piece, reservePiece, offset
		stream.writeObject(piece.toString()) ;
		for ( int i = 0; i < PIECE_LOOKAHEAD; i++ )
			stream.writeObject(nextPieces[i].toString()) ;
		for ( int i = 0; i < PIECE_LOOKAHEAD; i++ )
			stream.writeObject(reservePieces[i].toString()) ;
		stream.writeInt(offset.x) ;
		stream.writeInt(offset.y) ;
		stream.writeInt( lockPiece ? 1 : 0 ) ;
		stream.writeInt( lockThenActivate ? 1 : 0 ) ;
		stream.writeInt( lockThenDeactivate ? 1 : 0 ) ;
		stream.writeInt( activateThenEndCycle ? 1 : 0 ) ;
		stream.writeInt( deactivateThenEndCycle ? 1 : 0 ) ;
		stream.writeInt( usedReserve ? 1 : 0 ) ;
		
		// state, prog.state
		stream.writeInt(game_period) ;
		stream.writeInt(state) ;
		stream.writeInt(progressionState) ;
		stream.writeInt(stateAfterProgression) ;
		// latest events
		stream.writeObject(geventsLastTick.getStateAsSerializable()) ;
		
		// blockField
		stream.writeObject(ArrayOps.arrayToString(blockField)) ;
		stream.writeObject(ArrayOps.arrayToString(blockFieldBefore)) ;
		stream.writeObject(ArrayOps.arrayToString(blockFieldDisplacementRows)) ;
		
		// Clear?
		stream.writeInt(clearCascadeNumber) ;
		stream.writeObject(clearedRowsChromatic) ;
		stream.writeObject(clearedRowsMonochromatic) ;
		stream.writeObject(ArrayOps.arrayToString(blockFieldInverseClear)) ;
		
		// Unlock?
		// Write both numbers, then everything up to numUnlockColumnAbove.
		stream.writeInt(numUnlockColumnAbove) ;
		stream.writeInt(numUnlockColumnAboveAlreadyUnlocked) ;
		for ( int i = 0; i < numUnlockColumnAbove; i++ ) {
			Offset o = unlockColumnAbove.get(i) ;
			stream.writeInt(o.x) ;
			stream.writeInt(o.y) ;
		}
		stream.writeInt(numBlocksForValleys) ;
		stream.writeInt(numBlocksForJunctions) ;
		stream.writeInt(numBlocksForPeaks) ;
		stream.writeInt(numBlocksForCorners) ;
		stream.writeInt(numBlocksForTroll) ;
		
		// Some arrays for storing piece components and chunks.
		// As with the above, we first write the number of entries
		// that are currently relevant, then write that number of the
		// entries themselves.
		// NOTE: We interleave these (write a component and both its
		// offsets, then the next component with its offsets, etc.)
		// Technically we could try writeObject on each ArrayList itself,
		// but we haven't taken the time to ensure Pieces and Offsets can
		// write themselves to a stream, so...
		stream.writeInt(numComponents) ;
		for ( int i = 0; i < numComponents; i++ ) {
			stream.writeObject( components.get(i).toString() ) ;
			Offset o = componentOriginalOffsets.get(i) ;
			stream.writeInt(o.x) ;
			stream.writeInt(o.y) ;
			o = componentFellOffsets.get(i) ;
			stream.writeInt(o.x) ;
			stream.writeInt(o.y) ;
		}

		// Writing chunks is identical to writing components, down
		// to a remapping of variable names.
		stream.writeInt(numChunks) ;
		for ( int i = 0; i < numChunks; i++ ) {
			stream.writeObject( chunks.get(i).toString() ) ;
			Offset o = chunkOriginalOffsets.get(i) ;
			stream.writeInt(o.x) ;
			stream.writeInt(o.y) ;
			o = chunkFellOffsets.get(i) ;
			stream.writeInt(o.x) ;
			stream.writeInt(o.y) ;
			stream.writeBoolean(chunkIsNewToBlockField.get(i)) ;
		}
		
		// Finally, do we unleash an attack this cycle (supposing one is available)?
		stream.writeBoolean(unleashAttackThisCycle) ;
		stream.writeInt(numberOfRowsPushedUp) ;
		stream.writeInt(numberOfRowsPushedDown) ;
		stream.writeInt(numberOfRowsPushedUpThatAreGarbage) ;
		
		// Rows transferred?
		stream.writeInt(numberOfDisplacedRowsToTransferThisCycle) ;
		stream.writeInt(numberOfDisplacedRowsTransferred) ;
		
		// Is a reserve queued?
		stream.writeBoolean(reserveQueuedForNextCycle) ;
		
		// How many action cycles?
		stream.writeLong(numActionCycles) ;
		
		// That's everything!
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		// Retrieve all the details
		
		// Pre-versioned compatibility: we previously wrote neither the
		// versioned code or version number.  This is why we need the 
		// code; to indicate that this is a versioned object.
		// Set Code, Version, R, C.
		int code = stream.readInt() ;
		int version = stream.readInt() ;
		if ( code == THIS_IS_VERSIONED_CODE ) {
			R = stream.readInt() ;
			C = stream.readInt() ;
		} else {
			R = code ;
			C = version ;
			code = 0 ;
			version = -1 ;
		}
		
		// piece, reservePiece, offset
		piece = new Piece( (String)stream.readObject() ) ;
		nextPieces = new Piece[PIECE_LOOKAHEAD] ;
		for ( int i = 0; i < PIECE_LOOKAHEAD; i++ )
			nextPieces[i] = new Piece( (String)stream.readObject() ) ;
		reservePieces = new Piece[PIECE_LOOKAHEAD] ;
		for ( int i = 0; i < PIECE_LOOKAHEAD; i++ )
			reservePieces[i] = new Piece( (String)stream.readObject() ) ;
		offset = new Offset( stream.readInt(), stream.readInt() ) ;
		lockPiece = stream.readInt() == 1 ;
		if ( version >= 1 ) {
			// These were only added to stream i/o in version 1.
			lockThenActivate = stream.readInt() == 1 ;
			lockThenDeactivate = stream.readInt() == 1 ;
			activateThenEndCycle = stream.readInt() == 1 ;
			deactivateThenEndCycle = stream.readInt() == 1 ;
		}
		usedReserve = stream.readInt() == 1 ;
		
		// state, prog.state
		game_period = stream.readInt() ;
		state = stream.readInt() ;
		progressionState = stream.readInt() ;
		stateAfterProgression = stream.readInt() ;
		// events last tick
		geventsLastTick = new GameEvents().finalizeConfiguration() ;
 		geventsLastTick.setStateAsSerializable((Serializable)stream.readObject()) ;
		
		// blockField
		blockField = ArrayOps.byteArrayFromString( (String)stream.readObject() ) ;
		blockFieldBefore = ArrayOps.byteArrayFromString( (String)stream.readObject() ) ;
		if ( version < 1 )		// VERSION that removed blockFieldBeforeMetamorphosis
			stream.readObject() ;	// read and discard
		if ( version >= 3 )		// VERSION that added blockFieldDisplacementRows
			blockFieldDisplacementRows = ArrayOps.byteArrayFromString( (String)stream.readObject() ) ;
		else
			blockFieldDisplacementRows = new byte[2][R][C] ;
		
		// Clear?
		clearCascadeNumber = stream.readInt() ;
		clearedRowsChromatic = (int [])stream.readObject() ;
		clearedRowsMonochromatic = (boolean [])stream.readObject() ;
		blockFieldInverseClear = ArrayOps.byteArrayFromString( (String)stream.readObject() ) ;
		
		// Unlock?
		// Read both numbers, then everything up to numUnlockColumnAbove.
		numUnlockColumnAbove = stream.readInt() ;
		numUnlockColumnAboveAlreadyUnlocked = stream.readInt() ;
		unlockColumnAbove = new ArrayList<Offset>(DEFAULT_INITIAL_SIZE_UNLOCK_COLUMN) ;
		for ( int i = 0; i < numUnlockColumnAbove; i++ ) {
			Offset o = new Offset(stream.readInt(), stream.readInt()) ;
			unlockColumnAbove.add(o) ;
		}
		
		// blocks for valleys?
		numBlocksForValleys = stream.readInt() ;
		if ( version >= 0 ) {
			numBlocksForJunctions = stream.readInt() ;
			numBlocksForPeaks = stream.readInt() ;
			numBlocksForCorners = stream.readInt() ;
			numBlocksForTroll = stream.readInt() ;
		}
		
		// Some arrays for storing piece components and chunks.
		// As with the above, we first wrote the number of entries
		// that are currently relevant, then wrote that number of the
		// entries themselves.
		// NOTE: We interleaved these (writing a component and both its
		// offsets, then the next component with its offsets, etc.)
		numComponents = stream.readInt() ;
		components = new ArrayList<Piece>(Math.max( numComponents, DEFAULT_INITIAL_SIZE_COMPONENTS)) ;
		componentOriginalOffsets = new ArrayList<Offset>(Math.max( numComponents, DEFAULT_INITIAL_SIZE_COMPONENTS)) ;
		componentFellOffsets = new ArrayList<Offset>(Math.max( numComponents, DEFAULT_INITIAL_SIZE_COMPONENTS)) ;
		for ( int i = 0; i < numComponents; i++ ) {
			components.add( new Piece( (String)stream.readObject() ) ) ;
			Offset o = new Offset( stream.readInt(), stream.readInt() ) ;
			componentOriginalOffsets.add(o) ;
			o = new Offset( stream.readInt(), stream.readInt() ) ;
			componentFellOffsets.add(o) ;
		}

		// Reading chunks is identical to reading components, down
		// to a remapping of variable names.
		numChunks = stream.readInt() ;
		chunks = new ArrayList<Piece>(Math.max( numChunks, DEFAULT_INITIAL_SIZE_CHUNKS)) ;
		chunkOriginalOffsets = new ArrayList<Offset>(Math.max( numChunks, DEFAULT_INITIAL_SIZE_CHUNKS)) ;
		chunkFellOffsets = new ArrayList<Offset>(Math.max( numChunks, DEFAULT_INITIAL_SIZE_CHUNKS)) ;
		chunkIsNewToBlockField = new ArrayList<Boolean>( Math.max( numChunks, DEFAULT_INITIAL_SIZE_CHUNKS) ) ;
		for ( int i = 0; i < numChunks; i++ ) {
			chunks.add( new Piece( (String)stream.readObject() ) ) ;
			Offset o = new Offset( stream.readInt(), stream.readInt() ) ;
			chunkOriginalOffsets.add(o) ;
			o = new Offset( stream.readInt(), stream.readInt() ) ;
			chunkFellOffsets.add(o) ;
			if ( version >= 2 )
				chunkIsNewToBlockField.add( stream.readBoolean() ) ;
			else
				chunkIsNewToBlockField.add( Boolean.FALSE ) ;
		}
		
		// Finally, do we unleash an attack this cycle (supposing one is available)?
		unleashAttackThisCycle = stream.readBoolean() ;
		numberOfRowsPushedUp = stream.readInt() ;
		if ( version >= 1 )
			numberOfRowsPushedDown = stream.readInt() ;
		else
			numberOfRowsPushedDown = -1 ;
		if ( version >= 4 )
			numberOfRowsPushedUpThatAreGarbage = stream.readInt() ;
		else
			numberOfRowsPushedUpThatAreGarbage = 0 ;
		
		// Displaced rows transferred?
		if ( version >= 3 ) {
			numberOfDisplacedRowsToTransferThisCycle = stream.readInt() ;
			numberOfDisplacedRowsTransferred = stream.readInt() ;
		} else {
			numberOfDisplacedRowsToTransferThisCycle = 0 ;
			numberOfDisplacedRowsTransferred = 0 ;
		}
		
		// Is a reserve queued?  This fixes a previous bug where we
		// attempted to load this value from version -1 save data.
		if ( version >= 0 )
			reserveQueuedForNextCycle = stream.readBoolean() ;
		
		// How many action cycles?
		numActionCycles = stream.readLong() ;
		
		// That's everything!
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
	
	
	public GameState qUpconvert() {
		GameState gs = new GameState(this) ;
		
		// convert piece types.  There is no need to convert piece
		// blocks; upconversion affects QCombinations, not QOrientations.
		gs.piece.type = gs.piece.defaultType = PieceCatalog.qUpconvert(gs.piece.type) ;
		for ( int i = 0; i < gs.nextPieces.length; i++ ) {
			if ( gs.nextPieces[i] != null ) {
				gs.nextPieces[i].type = gs.nextPieces[i].defaultType = PieceCatalog.qUpconvert( gs.nextPieces[i].type ) ;
			}
		}
		for ( int i = 0; i < gs.reservePieces.length; i++ ) {
			if ( gs.reservePieces[i] != null ) {
				gs.reservePieces[i].type = gs.reservePieces[i].defaultType = PieceCatalog.qUpconvert( gs.reservePieces[i].type ) ;
			}
		}
		
		// We do not use PieceCatalog entries for chunks and components,
		// so no conversion is needed for them.
		return gs ;
	}
	
}



