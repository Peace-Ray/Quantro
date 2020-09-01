package com.peaceray.quantro.model.pieces.bags;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.utils.ArrayOps;

public class RetroPieceBag extends PieceBag {
	
	private int [][] TROMINO_TYPES ;
	private int [][] TETROMINO_TYPES ;
	private int [][] PENTOMINO_TYPES ;
	
	private static final int UNIFORMLY_RANDOM_TROMINO = -1 ;
	private static final int UNIFORMLY_RANDOM_TETROMINO = -2 ;
	private static final int UNIFORMLY_RANDOM_PENTOMINO = -3 ;
	
	Random r ;
	
	// Makes it easy to do stuff
	private int [][] TYPES ;
	private int [] typesIndexArray ;
	
	private boolean hasTrominoes, hasTetrominoes, hasPentominoes ;
	
	private VersionedPieceBagState state ;
	private boolean configured ;
	
	public RetroPieceBag( boolean hasTrominoes, boolean hasTetrominoes, boolean hasPentominoes,
			boolean freeRotation, boolean freeReflection ) {
		
		this.hasTrominoes = hasTrominoes ;
		this.hasTetrominoes = hasTetrominoes ;
		this.hasPentominoes = hasPentominoes ;
		
		setUpTypeArrays( freeRotation, freeReflection ) ;
		
		r = new Random() ;
		
		state = new VersionedPieceBagState() ;
		configured = false ;
		
		// Refill!
		refill() ;
	}
	
	
	private void setUpTypeArrays( boolean freeRotation, boolean freeReflection ) {
		TROMINO_TYPES = PieceBag.getAllTrominoTypesAndRotations(freeRotation, freeReflection) ;
		TETROMINO_TYPES = PieceBag.getAllTetrominoTypesAndRotations(freeRotation, freeReflection) ;
		PENTOMINO_TYPES = PieceBag.getAllPentominoTypesAndRotations(freeRotation, freeReflection) ;
		
		// every set will include all trominoes and tetrominoes (if included).
		// It will also include N pentominoes, where N is the number of all the previous.
		// If none, use a standard pentomino shuffle.
		int num = 0 ;
		if ( hasTrominoes )
			num += TROMINO_TYPES.length ;
		if ( hasTetrominoes )
			num += TETROMINO_TYPES.length ;
		if ( hasPentominoes )
			num += num == 0 ? PENTOMINO_TYPES.length : 7 ;	// if mixed with other pieces, 7 per set.
		
		TYPES = new int[num][2] ;
		num = 0 ;
		if ( hasTrominoes ) {
			if ( !hasTetrominoes && !hasPentominoes )
				for ( int i = 0; i < TROMINO_TYPES.length; i++ )
					TYPES[num++] = new int[]{ UNIFORMLY_RANDOM_TROMINO, 0 } ;
			else
				for ( int i = 0; i < TROMINO_TYPES.length; i++ )
					TYPES[num++] = TROMINO_TYPES[i] ;
		}
		if ( hasTetrominoes )
			for ( int i = 0; i < TETROMINO_TYPES.length; i++ )
				TYPES[num++] = TETROMINO_TYPES[i] ;
		if ( hasPentominoes ) {
			if ( !hasTrominoes && !hasTetrominoes )
				for ( int i = 0; i < PENTOMINO_TYPES.length; i++ )
					TYPES[num++] = PENTOMINO_TYPES[i] ;
			else
				while( num < TYPES.length )
					TYPES[num++] = new int[]{ UNIFORMLY_RANDOM_PENTOMINO, 0 } ;
		}
		
		
		typesIndexArray = new int[TYPES.length] ;
		for ( int i = 0; i < typesIndexArray.length; i++ ) {
			typesIndexArray[i] = i ;
		}
	}
	
	private void refill() {
		int index ;
		
		ArrayOps.randomize( typesIndexArray ) ;
		for ( int i = 0; i < typesIndexArray.length; i++ ) {
			index = typesIndexArray[i] ;
			int type = TYPES[index][0] ;
			int rot = TYPES[index][1] ;
			if ( type == UNIFORMLY_RANDOM_TROMINO ) {
				int t = r.nextInt(TROMINO_TYPES.length) ;
				type = TROMINO_TYPES[t][0] ;
				rot = TROMINO_TYPES[t][1]  ;
			}
			else if ( type == UNIFORMLY_RANDOM_TETROMINO ) {
				int t = r.nextInt(TETROMINO_TYPES.length) ;
				type = TETROMINO_TYPES[t][0] ;
				rot = TETROMINO_TYPES[t][1]  ;
			}
			else if ( type == UNIFORMLY_RANDOM_PENTOMINO ) {
				int t = r.nextInt(PENTOMINO_TYPES.length) ;
				type = PENTOMINO_TYPES[t][0] ;
				rot = PENTOMINO_TYPES[t][1]  ;
			}
			
			state.pieceTypeStack.push( type ) ;
			state.pieceDefaultRotationStack.push( rot ) ;
		}
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
		// our contents are basically just TYPES.  Make a copy.
		int num = 0 ;
		if ( hasTrominoes )
			num += TROMINO_TYPES.length ;
		if ( hasTetrominoes )
			num += TETROMINO_TYPES.length ;
		if ( hasPentominoes )
			num += PENTOMINO_TYPES.length ;
		
		int [] types = new int[num] ;
		num = 0 ;
		if ( hasTrominoes )
			for ( int i = 0; i < TROMINO_TYPES.length; i++ )
				types[num++] = TROMINO_TYPES[i][0] ;
		if ( hasTetrominoes )
			for ( int i = 0; i < TETROMINO_TYPES.length; i++ )
				types[num++] = TETROMINO_TYPES[i][0] ;
		if ( hasPentominoes ) {
			for ( int i = 0; i < PENTOMINO_TYPES.length; i++ )
				types[num++] = PENTOMINO_TYPES[i][0] ;
		}
		
		return types ;
	}
	
	/**
	 * Returns whether the specified piece type is contained
	 * 		within this bag.  Equivalent to calling contents()
	 * 		and performing a linear search, although this call
	 * 		may be more efficient.
	 */
	public boolean has( int pieceType ) {
		// meh, just look.
		if ( PieceCatalog.isTromino(pieceType) ) {
			for ( int i = 0; i < TROMINO_TYPES.length; i++ )
				if ( TROMINO_TYPES[i][0] == pieceType )
					return true ;
		}
		
		else if ( PieceCatalog.isTetromino(pieceType) ) {
			for ( int i = 0; i < TETROMINO_TYPES.length; i++ )
				if ( TETROMINO_TYPES[i][0] == pieceType )
					return true ;
		}
		
		else if ( PieceCatalog.isPentomino(pieceType) ) {
			for ( int i = 0; i < PENTOMINO_TYPES.length; i++ )
				if ( PENTOMINO_TYPES[i][0] == pieceType )
					return true ;
		}
		
		return false ;
	}
	
	public boolean hasTrominoes() { return hasTrominoes ; }
	public boolean hasTetrominoes() { return hasTetrominoes ; }
	public boolean hasPentominoes() { return hasPentominoes ; }
	public boolean hasTetracubes() { return false ; }
	

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
	public void push(Piece p) {
		state.pieceTypeStack.push(p.defaultType) ;
		state.pieceDefaultRotationStack.push(p.defaultRotation) ;
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
	public RetroPieceBag finalizeConfiguration() throws IllegalStateException {
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
	public RetroPieceBag setStateAsSerializable( Serializable in ) throws IllegalStateException {
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
	public RetroPieceBag readStateAsSerializedObject( ObjectInputStream inStream ) throws IllegalStateException, IOException, ClassNotFoundException {
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