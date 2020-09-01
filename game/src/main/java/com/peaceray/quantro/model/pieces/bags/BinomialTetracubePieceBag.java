package com.peaceray.quantro.model.pieces.bags;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;

public class BinomialTetracubePieceBag extends PieceBag {
	
	private int [][] TROMINO_CATS ;
	private int [][] TETROMINO_CATS ;
	private int [][] PENTOMINO_CATS ;
	
	//private static final String TAG = "PrototypingPieceBag" ;
	
	private static final int TROMINO_OFFSET = 0 ;
	private static final int TETROMINO_OFFSET = 100 ;
	private static final int PENTOMINO_OFFSET = 200 ;
	private static final int RANDOM_TROMINO = -1 ;
	private static final int RANDOM_TETROMINO = -2 ;
	private static final int RANDOM_PENTOMINO = -3 ;
	private static final int RANDOM_TETRACUBE = -10 ;
	private static final int RANDOM_TETRACUBE_MAYBE = -11 ;
	
	// Our own random object
	private Random r ; 
	
	// Makes it easy to do stuff
	private int [][] TYPES ;
	private int [] typesIndexArray ;
	private int numPolyominoes ;
	
	// Draw probabilities
	private int ssC ;
	private int ssN ;
	private double ssP ;
	
	private int [] cachedContents = null ;
	
	boolean hasTrominoes, hasTetrominoes, hasPentominoes ;
	boolean freeRotation, freeReflection ;

	private VersionedPieceBagState state ;
	private boolean configured ;
	
	public BinomialTetracubePieceBag(int C, int N, float P,
			boolean hasTrominoes, boolean hasTetrominoes, boolean hasPentominoes,
			boolean freeRotation, boolean freeReflection ) {
		r = new Random() ;
		
		this.hasTrominoes = hasTrominoes ;
		this.hasTetrominoes = hasTetrominoes ;
		this.hasPentominoes = hasPentominoes ;

		this.freeRotation = freeRotation ;
		this.freeReflection = freeReflection ;
		
		setUpTypeArrayAndTetracubeDraws( freeRotation, freeReflection, C, N, P ) ;
		
		// NOTE: 
		
		state = new VersionedPieceBagState() ;
		configured = false ;
		
		// Refill!
		refill() ;
	}
	
	private void setUpTypeArrayAndTetracubeDraws( boolean freeRotation, boolean freeReflection, int C, int N, float P ) {
		
		TROMINO_CATS = PieceBag.getAllTrominoTypesAndRotations(freeRotation, freeReflection) ;
		TETROMINO_CATS = PieceBag.getAllTetrominoTypesAndRotations(freeRotation, freeReflection) ;
		PENTOMINO_CATS = PieceBag.getAllPentominoTypesAndRotations(freeRotation, freeReflection) ;
		
		for ( int i = 0; i < TROMINO_CATS.length; i++ ) {
			TROMINO_CATS[i][0] = PieceCatalog.getTrominoCategory(TROMINO_CATS[i][0]) ;
		}
		for ( int i = 0; i < TETROMINO_CATS.length; i++ ) {
			TETROMINO_CATS[i][0] = PieceCatalog.getTetrominoCategory(TETROMINO_CATS[i][0]) ;
		}
		for ( int i = 0; i < PENTOMINO_CATS.length; i++ ) {
			PENTOMINO_CATS[i][0] = PieceCatalog.getPentominoCategory(PENTOMINO_CATS[i][0]) ;
		}
		
				
		numPolyominoes = 0 ;
		if ( hasTrominoes )
			numPolyominoes += TROMINO_CATS.length ;
		if ( hasTetrominoes )
			numPolyominoes += TETROMINO_CATS.length ;
		if ( hasPentominoes )
			numPolyominoes += numPolyominoes > 0 ? 7 : PENTOMINO_CATS.length ;
			
		
		// NOTE: C, N and P represent tetracube frequencies.  'C' is the constant number
		// of tetracubes included per 7-tetromino cycle.  'N' is the number of additional
		// binomial draws (which may or may not end up being a tetracube), and 'P' is the
		// probability of each of those 'N' draws being a tetracube.  Together, these produce
		// a Tetracube behavior which is balanced for cycles of 7 tetrominoes.  Depending on
		// which polyomino types are included, we may want to adjust these values.  We want
		// to maintain (as close as possible)
		// 1. The minimum and maximum numbers of tetras per 7
		// 2. The expected number of tetras per 7
		// 3. The variance (lowest priority, really).
			
		// First, calculate expected.  Trivially easy.
		float expectedExceptPer7 = N*P ;
		float expectedPer7 = C + expectedExceptPer7 ;
		// This is per-7.  Convert to per-our cycle
		float perCycleMult = ((float)numPolyominoes) / 7 ;
		float expectedPerCycle = expectedPer7 * perCycleMult ;

		// set ssC based on this multiplier.
		ssC = (int)Math.round(perCycleMult * C) ;
		float expectedExceptPerCycle = expectedPerCycle - ssC ;
		// easiest thing would be to adjust N to the appropriate level...
		// but we actually want to get the EXPECTED VALUE as close as
		// possible.  We adjust N first to get as close as possible (w/out
		// hitting 0), then make the final adjustment to P.  To avoid
		// "constant-count" results, we over-estimate N.
		ssN = N ;
		if ( ssN > 0 && P > 0 ) {
			while ( ssN * P < expectedExceptPerCycle ) {
				ssN++ ;
			}
			while ( ssN > 1 && ( (ssN - 1) * P) >= expectedExceptPerCycle ) {
				ssN-- ;
			}
			
			// now adjust P.
			ssP = Math.max(0, Math.min( 1, expectedExceptPerCycle / ssN ) ) ;
		}
		
		System.err.println("BinomialTetracubePieceBag CNP in " + C + "," + N + "," + P + ", ssCNP out " + ssC + "," + ssN + "," + ssP) ;
		
		
		if ( TYPES == null || TYPES.length != numPolyominoes + ssN + ssC ) {
			TYPES = new int[numPolyominoes + ssN + ssC][2] ;
			int index = 0 ;
			if ( hasTrominoes ) {
				for ( int i = 0; i < TROMINO_CATS.length; i++ ) {
					TYPES[index][0] = TROMINO_CATS[i][0] + TROMINO_OFFSET ;
					TYPES[index][1] = TROMINO_CATS[i][1] ;
					index++ ;
				}
			}
			if ( hasTetrominoes ) {
				for ( int i = 0; i < TETROMINO_CATS.length; i++ ) {
					TYPES[index][0] = TETROMINO_CATS[i][0] + TETROMINO_OFFSET ;
					TYPES[index][1] = TETROMINO_CATS[i][1] ;
					index++ ;
				}
			}
			if ( hasPentominoes ) {
				for ( int i = 0; i < PENTOMINO_CATS.length; i++ ) {
					if ( hasTrominoes || hasTetrominoes ) {
						TYPES[index][0] = RANDOM_PENTOMINO ;
						TYPES[index][1] = 0 ;
					} else {
						TYPES[index][0] = PENTOMINO_CATS[i][0] + PENTOMINO_OFFSET ;
						TYPES[index][1] = PENTOMINO_CATS[i][1] ;
					}
					index++ ;
				}
			}
			
			for ( int i = numPolyominoes; i < TYPES.length; i++ ) {
				if ( i - numPolyominoes < ssC )
					TYPES[i][0] = RANDOM_TETRACUBE ;
				else
					TYPES[i][0] = RANDOM_TETRACUBE_MAYBE ;
				TYPES[i][1] = 0 ;
			}
		}
		
		typesIndexArray = new int [TYPES.length] ;
		for ( int i = 0; i < TYPES.length; i++ )
			typesIndexArray[i] = i ;
	}
	
	private void refill() {
		
		int catCode ;
		int cat ;
		int type ;
		int scat ;
		int qo ;
		int rot ;
		
		boolean introCycle = state.num == 0 && hasTetrominoes
			&& !hasTrominoes && !hasPentominoes ;
		boolean redFirst = r.nextDouble() <= 0.5 ;
		int polys = 0 ;
		
		ArrayOps.randomize(typesIndexArray) ;
		
		// First time: want S0 S0 S0 S1 S1 S1 S0 SS, or
		//					S1 S1 S1 S0 S0 S0 S1 SS. 
		// Remember that because it's a stack,
		// we push things in reverse order.
		if ( introCycle ) {
			qo = QCombinations.SS ;
			cat = PieceCatalog.randomTetracubeCategory() ;
			scat = freeReflection ? PieceCatalog.randomFreeTetracubeSubcategory(cat) : PieceCatalog.randomTetracubeSubcategory(cat) ;
			state.pieceTypeStack.push( PieceCatalog.encodeTetracube( cat, scat, qo ) ) ;
			state.pieceDefaultRotationStack.push( freeRotation ? 0 : r.nextInt(4) ) ;
		}
		
		for ( int i = 0; i < typesIndexArray.length; i++ ) {
			int index = typesIndexArray[i] ;
			catCode = TYPES[index][0] ;
			rot = TYPES[index][1] ;
			
			if ( catCode > RANDOM_TETRACUBE ) {
				// A polyomino type.
				if ( !introCycle )
					qo = r.nextDouble() <= 0.5 ? QOrientations.S0 : QOrientations.S1 ;
				else
					qo = (redFirst == ((numPolyominoes /2 < polys) || polys == 0 ))
							? QOrientations.S0 : QOrientations.S1 ;
				
				if ( catCode == RANDOM_TROMINO ) {
					cat = freeReflection ? PieceCatalog.randomFreeTrominoCategory() : PieceCatalog.randomTrominoCategory() ;
					type = PieceCatalog.encodeTromino(cat, qo) ;
				} else if ( catCode == RANDOM_TETROMINO ) {
					cat = freeReflection ? PieceCatalog.randomFreeTetrominoCategory() : PieceCatalog.randomTetrominoCategory() ;
					type = PieceCatalog.encodeTetromino(cat, qo) ;
				} else if ( catCode == RANDOM_PENTOMINO ) {
					cat = freeReflection ? PieceCatalog.randomFreePentominoCategory() : PieceCatalog.randomPentominoCategory() ;
					type = PieceCatalog.encodePentomino(cat, qo) ;
				} else if ( catCode >= PENTOMINO_OFFSET ) {
					cat = catCode - PENTOMINO_OFFSET ;
					type = PieceCatalog.encodePentomino(cat, qo) ;
				} else if ( catCode >= TETROMINO_OFFSET ) {
					cat = catCode - TETROMINO_OFFSET ;
					type = PieceCatalog.encodeTetromino(cat, qo) ;
				} else {
					cat = catCode - TROMINO_OFFSET ;
					type = PieceCatalog.encodeTromino(cat, qo) ;
				}
				state.pieceTypeStack.push( type ) ;
				state.pieceDefaultRotationStack.push( rot ) ;
				polys++ ;
			}
			else if ( catCode == RANDOM_TETRACUBE && !introCycle ) {
				// A GUARANTEED tetracube.
				qo = QCombinations.SS ;
				cat = PieceCatalog.randomTetracubeCategory() ;
				scat = freeReflection ? PieceCatalog.randomFreeTetracubeSubcategory(cat) : PieceCatalog.randomTetracubeSubcategory(cat) ;
				state.pieceTypeStack.push( PieceCatalog.encodeTetracube( cat, scat, qo ) ) ;
				state.pieceDefaultRotationStack.push( freeRotation ? 0 : r.nextInt(4) ) ;
			}
			else if ( catCode == RANDOM_TETRACUBE_MAYBE && r.nextDouble() < ssP && !introCycle ){
				// A tetracube.
				qo = QCombinations.SS ;
				cat = PieceCatalog.randomTetracubeCategory() ;
				scat = freeReflection ? PieceCatalog.randomFreeTetracubeSubcategory(cat) : PieceCatalog.randomTetracubeSubcategory(cat) ;
				state.pieceTypeStack.push( PieceCatalog.encodeTetracube( cat, scat, qo ) ) ;
				state.pieceDefaultRotationStack.push( freeRotation ? 0 : r.nextInt(4) ) ;
			}
		}

		state.num++ ;
	}
	
	
	
	

	/**
	 * Returns an array of the piece types which this 
	 * PieceBag returns.  There are only limited guarantees
	 * about this method.
	 * 
	 * 1. We do not guarantee a newly allocated array with
	 * 			every call.  In other words, if one caller
	 * 			makes changes to the returned array those
	 * 			changes may be reflected in the next call
	 * 			(although they do NOT affect the types
	 * 			of pieces which the bag can produce, so
	 * 			this will give an inaccurate view of its contents.
	 * 
	 * 2. We do not guarantee that this array is allocated ahead of
	 * 			time, so this call may cause a memory allocation.
	 * 
	 * @return An array of exactly those piece types which this
	 * 			bag can produce with pop().
	 */
	public int [] contents() {
		// Contents: every tetromino in S0, every tetromino in S1, every
		// tetracube in every subcategory in SS.
		if ( cachedContents == null ) {
			
			int length = 0 ;
			if ( hasTrominoes )
				length += TROMINO_CATS.length * 2 ;
			if ( hasTetrominoes )
				length += TETROMINO_CATS.length * 2 ;
			if ( hasPentominoes )
				length += PENTOMINO_CATS.length  * 2 ;
			for ( int i = 0; i < PieceCatalog.NUMBER_TETRACUBE_CATEGORIES; i++ )
				length += PieceCatalog.NUMBER_TETRACUBE_SUBCATEGORIES[i] ;
			
			cachedContents = new int[length] ;
			int index = 0 ;
			if ( hasTrominoes ) {
				for ( int i = 0; i < TROMINO_CATS.length; i++ ) {
					cachedContents[index++] = PieceCatalog.encodeTromino(TROMINO_CATS[i][0], QOrientations.S0) ;
					cachedContents[index++] = PieceCatalog.encodeTromino(TROMINO_CATS[i][0], QOrientations.S1) ;
				}
			}
			if ( hasTetrominoes ) {
				for ( int i = 0; i < TETROMINO_CATS.length; i++ ) {
					cachedContents[index++] = PieceCatalog.encodeTetromino(TETROMINO_CATS[i][0], QOrientations.S0) ;
					cachedContents[index++] = PieceCatalog.encodeTetromino(TETROMINO_CATS[i][0], QOrientations.S1) ;
				}
			}
			if ( hasPentominoes ) {
				for ( int i = 0; i < PENTOMINO_CATS.length; i++ ) {
					cachedContents[index++] = PieceCatalog.encodePentomino(PENTOMINO_CATS[i][0], QOrientations.S0) ;
					cachedContents[index++] = PieceCatalog.encodePentomino(PENTOMINO_CATS[i][0], QOrientations.S1) ;
				}
			}
			for ( int i = 0; i < PieceCatalog.NUMBER_TETRACUBE_CATEGORIES; i++ )
				for ( int j = 0; j < PieceCatalog.NUMBER_TETRACUBE_SUBCATEGORIES[i]; j++ )
					cachedContents[index++] = PieceCatalog.encodeTetracube(i, j, QCombinations.SS) ;
			// Includes one-sided subcategories even when using 'free' tetracubes.
		}
		
		return cachedContents ;
	}
	
	/**
	 * Returns whether the specified piece type is contained
	 * 		within this bag.  Equivalent to calling contents()
	 * 		and performing a linear search, although this call
	 * 		may be more efficient.
	 */
	public boolean has( int pieceType ) {
		throw new IllegalStateException("Unsupported now.  Re-implement if you want to use this.") ;
		/*
		// pretty simple.
		if ( PieceCatalog.isTetromino(pieceType) ) {
			int qo = PieceCatalog.getQCombination(pieceType) ;
			return qo == QOrientations.S0 || qo == QOrientations.S1 ;
		} else if ( PieceCatalog.isTetracube(pieceType) )
			return PieceCatalog.getQCombination(pieceType) == QCombinations.SS ;
		
		return false ;
		*/
	}
	
	
	public boolean hasTrominoes() { return hasTrominoes ; }
	public boolean hasTetrominoes() { return hasTetrominoes ; }
	public boolean hasPentominoes() { return hasPentominoes ; }
	public boolean hasTetracubes() { return true ; }
	

	@Override
	public int peek(int lookahead) {
		return -1 ;
	}

	@Override
	public void pop( Piece p ) {
		p.clear() ;
		p.type = p.defaultType = state.pieceTypeStack.pop() ;
		p.rotation = p.defaultRotation = state.pieceDefaultRotationStack.pop() ;
		if ( state.pieceTypeStack.count() == 0 ) {
			refill() ;
		}
	}

	@Override
	public void push( Piece p ) {
		state.pieceTypeStack.push( p.defaultType ) ;
		state.pieceDefaultRotationStack.push( p.defaultRotation ) ;
	}

	@Override
	public boolean canPush() {
		return true ;
	}

	@Override
	public boolean canPeek(int lookahead) {
		return false;
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
	public BinomialTetracubePieceBag finalizeConfiguration() throws IllegalStateException {
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
	public Serializable getCloneStateAsSerializable() throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before retrieving state!") ;
		return new VersionedPieceBagState( state ) ;
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
	public PieceBag setStateAsSerializable( Serializable in ) throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		if ( in instanceof SimplePieceBagState )
			state = new VersionedPieceBagState((SimplePieceBagState)in) ;
		else
			state = (VersionedPieceBagState)in ;
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
	public BinomialTetracubePieceBag readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		Object in = inStream.readObject() ;
		if ( in instanceof SimplePieceBagState )
			state = new VersionedPieceBagState((SimplePieceBagState)in) ;
		else
			state = (VersionedPieceBagState)in ;
		return this; 
	}
}