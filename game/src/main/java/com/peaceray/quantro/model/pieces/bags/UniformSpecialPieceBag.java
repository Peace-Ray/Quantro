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

public class UniformSpecialPieceBag extends PieceBag {

//private static final String TAG = "PrototypingSpecialBag" ;
	
	private Random r ;
	
	private VersionedPieceBagState state ;
	private boolean configured ;
	
	boolean freeRotation, freeReflection ;
	boolean hasTrominoes, hasTetrominoes, hasPentominoes ;
	
	public UniformSpecialPieceBag( boolean hasTrominoes, boolean hasTetrominoes, boolean hasPentominoes,
			boolean freeRotation, boolean freeReflection ) {
		state = new VersionedPieceBagState() ;
		configured = false ;
		
		this.freeRotation = freeRotation ;
		this.freeReflection = freeReflection ;
		
		this.hasTrominoes = hasTrominoes ;
		this.hasTetrominoes = hasTetrominoes ;
		this.hasPentominoes = hasPentominoes ;
		
		r = new Random() ;
		
		pushOne() ;
	}
	
	/**
	 * pushOne Push a random special piece on the stack.
	 */
	private void pushOne() {
		
		double rnd = r.nextDouble() ;
		int cat, scat, qo ;
		int type ;
		
		// we want to avoid drawing the same overall category
		// twice in a row, so compare against the last.
		int specialCategory = -1 ;
		if ( state.last == -1 ) {
			if ( rnd < 0.25 )
				specialCategory = 0 ;
			else if ( rnd < 0.5 )
				specialCategory = 1 ;
			else if ( rnd < 0.75 )
				specialCategory = 2 ;
			else
				specialCategory = 3 ;
		} else {
			if ( rnd < 0.33333 )
				specialCategory = 0 ;
			else if ( rnd < 0.66667 )
				specialCategory = 1 ;
			else
				specialCategory = 2 ;
			
			if ( specialCategory >= state.last )
				specialCategory++ ;
		}
		
		if ( specialCategory == 0 ) {
			// A LI tetracube
			cat = PieceCatalog.randomTetracubeCategory() ;
			while ( cat == PieceCatalog.TETRA_CAT_RECT )
				cat = PieceCatalog.randomTetracubeCategory() ;
			scat = freeReflection ? PieceCatalog.randomFreeTetracubeSubcategory(cat) : PieceCatalog.randomTetracubeSubcategory(cat) ;
			qo = QOrientations.SL ;
			type = PieceCatalog.encodeTetracube(cat, scat, qo) ;
		} else if ( specialCategory == 1 ) {
			// A sticky polyomino
			type = encodeRandomPolyomino( QOrientations.ST ) ;
		} else if ( specialCategory == 2 ) {
			// An unstable, linked polyomino
			type = encodeRandomPolyomino( QOrientations.UL ) ;
		} else {
			// A flash
			cat = PieceCatalog.SPECIAL_CAT_FLASH ;
			qo = QOrientations.F0 ;
			type = PieceCatalog.encodeSpecial(cat, qo) ;
		}
		
		state.last = specialCategory ;
		state.pieceTypeStack.push(type) ;
		state.pieceDefaultRotationStack.push( freeRotation ? 0 : r.nextInt(4) ) ;
	}
	
	
	/**
	 * Encodes and returns a random Polyomino type using our settings,
	 * e.g. freeReflection, hasPentominoes, etc.
	 * @param qCombination
	 * @return
	 */
	private int encodeRandomPolyomino( int qCombination ) {
		// We attempt to maintain the same distribution of polyomino
		// types as other piece bags: in other words, we normalize the
		// following weights:
		// tro: 2, tetro: 7, pento: 7.
		int totalWeight = 0 ;
		if ( hasTrominoes )
			totalWeight += 2 ;
		if ( hasTetrominoes )
			totalWeight += 7 ;
		if ( hasPentominoes )
			totalWeight += 7 ;
		
		int n = r.nextInt(totalWeight) ;
		int cat ;
		if ( hasTrominoes ) {
			if ( n < 2 ) {
				cat = freeReflection
						? PieceCatalog.randomFreeTrominoCategory()
						: PieceCatalog.randomTrominoCategory() ;
				return PieceCatalog.encodeTromino(cat, qCombination) ;
			}
			n -= 2 ;
		}
		
		if ( hasTetrominoes ) {
			if ( n < 7 ) {
				cat = freeReflection
						? PieceCatalog.randomFreeTetrominoCategory()
						: PieceCatalog.randomTetrominoCategory() ;
				return PieceCatalog.encodeTetromino(cat, qCombination) ;
			}
			n -= 7 ;
		}
		
		
		cat = freeReflection
				? PieceCatalog.randomFreePentominoCategory()
				: PieceCatalog.randomPentominoCategory() ;
		return PieceCatalog.encodePentomino(cat, qCombination) ;
	}
	
	
	private int [] cachedContents = null ;
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
		if ( cachedContents == null ) {
			// This piece bag produces ST polyominoes, UL polyominoes, SL tetracubes,
			// and FL blocks.
			int length = 0 ;
			int polyCats = 0 ;
			if ( hasTrominoes )
				polyCats += PieceCatalog.NUMBER_TROMINO_CATEGORIES ;
			if ( hasTetrominoes )
				polyCats += PieceCatalog.NUMBER_TETROMINO_CATEGORIES ;
			if ( hasPentominoes )
				polyCats += PieceCatalog.NUMBER_PENTOMINO_CATEGORIES ;
			length += polyCats * 2 ;
			for ( int i = 0; i < PieceCatalog.NUMBER_TETRACUBE_CATEGORIES; i++ )
				length += PieceCatalog.NUMBER_TETRACUBE_SUBCATEGORIES[i] ;
			length += 1 ;		// flashes
			
			cachedContents = new int[length] ;
			int index = 0 ;
			for ( int qcIter = 0; qcIter < 2; qcIter++ ) {
				 int qc = qcIter == 0 ? QCombinations.ST : QCombinations.UL ;
				 if ( hasTrominoes ) {
					 for ( int i = 0; i < PieceCatalog.NUMBER_TROMINO_CATEGORIES; i++ )
							cachedContents[index++] = PieceCatalog.encodeTromino(i, qc) ;
				 }
				 if ( hasTetrominoes ) {
					 for ( int i = 0; i < PieceCatalog.NUMBER_TETROMINO_CATEGORIES; i++ )
							cachedContents[index++] = PieceCatalog.encodeTetromino(i, qc) ;
				 }
				 if ( hasPentominoes ) {
					 for ( int i = 0; i < PieceCatalog.NUMBER_PENTOMINO_CATEGORIES; i++ )
							cachedContents[index++] = PieceCatalog.encodePentomino(i, qc) ;
				 }
				 
			}
			
			for ( int i = 0; i < PieceCatalog.NUMBER_TETROMINO_CATEGORIES; i++ )
				for ( int j = 0; j < PieceCatalog.NUMBER_TETRACUBE_SUBCATEGORIES[i]; j++ )
					cachedContents[index++] = PieceCatalog.encodeTetracube(i, j, QOrientations.SL) ;
			cachedContents[index++] = PieceCatalog.encodeSpecial(PieceCatalog.SPECIAL_CAT_FLASH, QOrientations.F0) ;
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
		int qo = PieceCatalog.getQCombination(pieceType) ;
		if ( PieceCatalog.isTetromino(pieceType) )
			return qo == QOrientations.ST || qo == QOrientations.UL ;
		else if ( PieceCatalog.isTetracube(pieceType) )
			return qo == QOrientations.SL ;
		else if ( PieceCatalog.isSpecial(pieceType) )
			return qo == QOrientations.F0 ;
		return false ;
		*/
	}
	
	
	public boolean hasTrominoes() { return hasTrominoes ; }
	public boolean hasTetrominoes() { return hasTetrominoes ; }
	public boolean hasPentominoes() { return hasPentominoes ; }
	public boolean hasTetracubes() { return true ; }
	
	

	/**
	 * peek Peek into the PieceBag at the given lookahead.  This
	 * piece will be on top after the given number of pops.
	 * 
	 * @param lookahead How far into the future to gaze?
	 * @return What piece type will be drawn?
	 */
	public int peek( int lookahead ) {
		if ( lookahead == 0 )
			return state.pieceTypeStack.peek();
		return -1 ;
	}
	
	/**
	 * pop Pull a piece out of the bag.
	 * @return What piece type did we draw?
	 */
	public void pop( Piece p ) {
		p.clear() ;
		p.type = p.defaultType = state.pieceTypeStack.pop() ;
		p.rotation = p.defaultRotation = state.pieceDefaultRotationStack.pop() ;
		if ( state.pieceTypeStack.count() == 0 ) {
			pushOne() ;
		}
	}
	
	/**
	 * push Push a piece type on top of the bag.
	 * @param pieceType What piece to push?
	 */
	public void push( Piece p ) {
		state.pieceTypeStack.push( p.defaultType ) ;
		state.pieceDefaultRotationStack.push( p.defaultRotation ) ;
	}
	
	// PieceBag capabilities; may vary between pieces
	
	/**
	 * canPush Does the 'push' method function for this PieceBag?
	 * @return Does pushing work?
	 */
	public boolean canPush() {
		return true ;
	}
	
	/**
	 * canPeek Does the 'peek' method function with the provided lookahead?
	 * @param lookahead How far ahead we wish to look
	 * @return Is it appropriate to look this far ahead?
	 */
	public boolean canPeek( int lookahead ) {
		return lookahead == 0 ;
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
	public UniformSpecialPieceBag finalizeConfiguration() throws IllegalStateException {
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
	public UniformSpecialPieceBag readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
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
