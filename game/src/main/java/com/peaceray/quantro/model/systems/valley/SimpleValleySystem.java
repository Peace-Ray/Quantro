package com.peaceray.quantro.model.systems.valley;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.model.state.EmptyState;
import com.peaceray.quantro.model.state.SerializableState;
import com.peaceray.quantro.model.state.VersionedState;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QInteractions;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;

/**
 * The Quantro valley system will drop S0, S1 blocks independently
 * 
 * It does not apply special settings to valley candidates; for example,
 * if an L-shaped hole appears, it will not rule out the column as
 * a candidate despite the fact that dropping a block into it will create
 * a hole in an otherwise solid shape.
 * 
 * @author Jake
 *
 */
public class SimpleValleySystem extends ValleySystem {
	
	public static final int VERSION = 0 ;
	// VERSION 0: The first to contain a non-empty state.
	
	
	public static final int Q_COMBINATIONS_QPANE = 0 ;		// uses S0, S1
	public static final int Q_COMBINATIONS_RAINBOW = 1 ;	// uses R0...R6.
	public static final int Q_COMBINATIONS_QPANE_UNSTABLE = 2 ;	// uses U0, U1.
	public static final int Q_COMBINATIONS_3D = 3 ;			// uses ST.
	public static final int Q_COMBINATIONS_NONE = 4 ;
	
	protected int [][] Q_COMBINATIONS ;
		// we will construct a block of this QCombination, selected using
		// Q_COMBINATIONS[qpane][ pseudorandomNumber ].
	protected boolean Q_PANES_INTERACT ;
	
	private GameInformation ginfo ;
	private QInteractions qi ;
	
	private SimpleValleySystemState state ;
	private boolean configured ;
	
	private int R, C ;
	private byte [][][] rowContents ;	// use this to position blocks in ascending
										// rows, to ensure that no two blocks touch.
										// "move up" every time we need to.
	
	private int [][] heights ;			// our heights map
	
	private byte [] tempQO ;
	
	
	public SimpleValleySystem( GameInformation ginfo, QInteractions qi ) {
		this.ginfo = ginfo ;
		this.qi = qi ;
		
		// allocate rowContents, heights, and call findBestCandidate to
		// allocate superclass structures.
		if ( ginfo != null ) {
			int qpanes = GameModes.numberQPanes(ginfo) ;
			R = GameModes.numberRows(ginfo) ;
			C = GameModes.numberColumns(ginfo) ;
			rowContents = new byte[qpanes][2][C] ;
			heights = new int[qpanes][C] ;
			findBestCandidate( Type.VALLEY, Q_PANES_INTERACT, heights, null, 0 ) ;
		}
		
		tempQO = new byte[2] ;
		
		this.state = new SimpleValleySystemState(VERSION) ;
		this.configured = false ;
	}
	
	public ValleySystem setBlockQCombinations( int type ) {
		int [][] qc = null ;
		boolean interact = false ;
		switch( type ) {
		case Q_COMBINATIONS_QPANE:
			qc = new int[][]{
					new int[]{ QOrientations.S0 },
					new int[]{ QOrientations.S1 }
			} ;
			break ;
		case Q_COMBINATIONS_RAINBOW:
			qc = new int[][]{
					new int[]{
							QOrientations.R0,
							QOrientations.R1,
							QOrientations.R2,
							QOrientations.R3,
							QOrientations.R4,
							QOrientations.R5,
							QOrientations.R6 },
					new int[]{
							QOrientations.R0,
							QOrientations.R1,
							QOrientations.R2,
							QOrientations.R3,
							QOrientations.R4,
							QOrientations.R5,
							QOrientations.R6 }
			} ;
			break ;
		case Q_COMBINATIONS_QPANE_UNSTABLE:
			qc = new int[][]{
					new int[]{ QOrientations.U0 },
					new int[]{ QOrientations.U1 }
			} ;
			break ;
		case Q_COMBINATIONS_3D:
			qc = new int[][]{
				new int[]{ QOrientations.ST_INACTIVE },
				new int[]{ QOrientations.ST_INACTIVE }
			} ;
			interact = true ;
			break ;
		case Q_COMBINATIONS_NONE:
			qc = null ;
			break ;
			
		default:
			throw new IllegalArgumentException("QCombination type " + type + " is not defined.") ;
		}
		
		return setBlockQCombinations( qc, interact ) ;
	}
	
	private ValleySystem setBlockQCombinations( int [][] qc, boolean interact ) {
		if ( configured )
			throw new IllegalStateException("Must set QCombination before finalizing configuration") ;
		
		Q_COMBINATIONS = qc ;
		Q_PANES_INTERACT = interact ;
		
		return this ;
	}

	@Override
	public ValleySystem setPseudorandom(int pseudorandom) {
		this.state.pseudorandom = Math.abs(pseudorandom) ;
		return this ;
	}

	@Override
	public GameInformation getGameInformation() {
		return ginfo ;
	}

	@Override
	public QInteractions getQInteractions() {
		return qi ;
	}

	
	@Override
	public int dropBlocksInValleys( int minBlocks, int maxBlocks, int minRow, byte [][][] field,
			ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks) {
		
		return dropBlocks( minBlocks, maxBlocks, minRow, field, chunks, offsets, numChunks, Type.VALLEY ) ;
	}
	
	@Override
	public int dropBlocksOnJunctions( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks ) {
		
		return dropBlocks( minBlocks, maxBlocks, minRow, field, chunks, offsets, numChunks, Type.JUNCTION ) ;
	}
	
	@Override
	public int dropBlocksOnPeaks( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks ) {
		
		return dropBlocks( minBlocks, maxBlocks, minRow, field, chunks, offsets, numChunks, Type.PEAK ) ;
	}
	
	@Override
	public int dropBlocksOnCorners( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks ) {
		
		return dropBlocks( minBlocks, maxBlocks, minRow, field, chunks, offsets, numChunks, Type.CORNER ) ;
	}
	
	
	/**
	 * Drops the specified number of blocks on the specified type of location, in order.
	 * We drop up to maxBlocks, and at least minBlocks, on the types of locations specified.
	 * 
	 * Given types[], for each block, we try:
	 * 
	 * For b in maxBlocks:
	 * 		dropped = false
	 * 		for t in types:
	 * 			loc = findBest( t )
	 * 			if loc exists:
	 * 				dropBlockOnLoc
	 * 				dropped = true
	 * 				break ;
	 * 		if not dropped:
	 * 			if b > minBlocks
	 * 				break ;
	 * 			drop on random location
	 * 
	 * @param minBlocks
	 * @param maxBlocks
	 * @param minRow
	 * @param field
	 * @param chunks
	 * @param offsets
	 * @param numChunks
	 * @param types
	 * @return
	 */
	public int dropBlocks( int minBlocks, int maxBlocks, int minRow,
			byte [][][] field, ArrayList<Piece> chunks, ArrayList<Offset> offsets, int numChunks,
			Type ... types ) {
		
		if ( !configured )
			throw new IllegalStateException("Must call finalizeConfiguration() first!") ;
		
		if ( Q_COMBINATIONS == null )
			return numChunks ;
		
		// set our 'heights' array.
		for ( int qp = 0; qp < heights.length; qp++ ) {
			for ( int c = 0; c < C; c++ ) {
				heights[qp][c] = 0 ;
				for ( int r = R-1; r >= 0; r-- ) {
					if ( field[qp][r][c] != QOrientations.NO ) {
						heights[qp][c] = r+1 ;
						break ;
					}
				}
			}
		}
		
		// empty our rowContents
		for ( int q = 0; q < rowContents.length; q++ )
			for ( int r = 0; r < 2; r++ )
				for ( int c = 0; c < C; c++ )
					rowContents[q][r][c] = QOrientations.NO ;
		
		// iterate to get blocks.
		for ( int i = 0; i < maxBlocks; i++ ) {
			int pos = -1 ;
			for ( int t = 0; t < types.length; t++ ) {
				pos = findBestCandidate( types[t], Q_PANES_INTERACT, heights, null, state.pseudorandom ) ;
				if ( pos >= 0 )
					break ;
			}
			
			if ( pos < 0 ) {
				// either break out, or pick a random column if we haven't hit 'min blocks' yet.
				if ( i < minBlocks ) {
					// first: pick a pseudorandom 
					pos = ValleySystem.randPosInt(heights, state.pseudorandom, i) % ( C * 2 ) ;
				}
				else
					break ;
			}
			
			int c = pos % C ;
			int qpane = pos / C ;
			
			int pr = ValleySystem.randPosInt(state.pseudorandom, i, heights[qpane][c]) ;
			int qc = Q_COMBINATIONS[qpane][ pr % Q_COMBINATIONS[qpane].length ] ;
			
			QCombinations.setAs(tempQO, qc) ;
			
			// Adjust height array.  The way we adjust depends on the QOrientation dropped.
			// Quick easy fix: increment whichever qPane the block exists in.  If the
			// two qPanes don't separate, then set them both to the max.
			if ( tempQO[0] != QOrientations.NO )
				heights[0][c]++ ;
			if ( tempQO[1] != QOrientations.NO )
				heights[1][c]++ ;
			if ( !qi.separatesFromWhenQuantum(tempQO[0], tempQO[1]) )
				heights[0][c] = heights[1][c] = Math.max( heights[0][c], heights[1][c] ) ;
			
			// see if this qc can be placed in row 0 of rowContents.  While it can't
			// be, shift everything to 1 and increment minRow.
			boolean placed = false ;
			while ( !placed ) {
				// will fit if does not land on what's below, and does not
				// stick to what's to the sides.
				boolean fits = true ;
				for ( int q = 0; q < 2; q++ ) {
					fits = fits && ( tempQO[q] == QOrientations.NO || rowContents[q][0][c] == QOrientations.NO ) ;
					fits = fits && ( c == 0 || qi.separatesFromWhenSide(tempQO[q], rowContents[q][0][c-1]) ) ;
					fits = fits && ( c == C-1 || qi.separatesFromWhenSide(tempQO[q], rowContents[q][0][c+1]) ) ;
					fits = fits && qi.separatesFromWhenAbove( tempQO[q], rowContents[q][1][c] ) ;
					fits = fits && qi.separatesFromWhenQuantum( tempQO[q], rowContents[(q+1)%2][0][c] ) ;
				}
				
				if ( fits ) {
					for ( int q = 0; q < 2; q++ )
						if ( tempQO[q] != QOrientations.NO )
							rowContents[q][0][c] = tempQO[q] ;
					
					// make a chunk.
					Piece p ;
					Offset o ;
					if ( chunks.size() <= numChunks ) {
						p = new Piece() ;
						o = new Offset() ;
						chunks.add( p ) ;
						offsets.add( o ) ;
					}
					else {
						p = chunks.get(numChunks) ;
						o = offsets.get(numChunks) ;
					}
					numChunks++ ;
					// Make sure 'blocks' is sufficient
					p.blocks = ArrayOps.allocateToMatchDimensions( p.blocks, field ) ;			// 3b
					ArrayOps.setEmpty( p.blocks ) ;
					
					// now set our blocks, bounds, and offset.
					p.setBounds(0, 0, 1, 1) ;
					for ( int q = 0; q < 2; q++ )
						p.blocks[q][0][0] = tempQO[q] ;
					o.x = c ;
					o.y = minRow ;
					
					placed = true ;
				}
				else {
					//System.err.println("SimpleValleySystem moving up") ;
					// doesn't fit.  Shift up one.
					minRow++ ;
					for ( int col = 0; col < C; col++ ) {
						for ( int q = 0; q < 2; q++ ) {
							rowContents[q][1][col] = rowContents[q][0][col] ;
							rowContents[q][0][col] = QOrientations.NO ;
						}
					}
				}
			}
		}
		
		return numChunks ;
		
	}

	
	////////////////////////////////////////////////////////////////
	//
	// SERIALIZABLE STATE
	//
	// These methods provide the implementation of the SerializableState
	// interface.
	//
	////////////////////////////////////////////////////////////////
	
	@Override
	public SerializableState finalizeConfiguration()
			throws IllegalStateException {
		if ( configured )
			throw new IllegalStateException("finalizeConfiguration() should only be called once!") ;
		configured = true ;
		return this ;
	}

	@Override
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
		return new SimpleValleySystemState( state ) ;
	}

	@Override
	public SerializableState setStateAsSerializable(Serializable in)
			throws IllegalStateException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		if ( in instanceof EmptyState ) {
			state = new SimpleValleySystemState(VERSION) ;
		} else
			state = (SimpleValleySystemState)in ;
		return this ;
	}

	@Override
	public void writeStateAsSerializedObject(ObjectOutputStream outStream)
			throws IllegalStateException, IOException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		outStream.writeObject(state) ;
	}

	@Override
	public SerializableState readStateAsSerializedObject(
			ObjectInputStream inStream) throws IllegalStateException,
			IOException, ClassNotFoundException {
		if ( !configured )
			throw new IllegalStateException("Must finalizeConfiguration() before setting state!") ;
		Object obj = inStream.readObject() ;
		if ( obj instanceof EmptyState ) {
			state = new SimpleValleySystemState(VERSION) ;
		} else
			state = (SimpleValleySystemState)obj ;
		return this ;
	}
	
	
	
	
	private class SimpleValleySystemState extends VersionedState {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = -7465961739050293279L;
		
		
		int pseudorandom ;
		
		public SimpleValleySystemState(int version) {
			super(version);
			pseudorandom = 0 ;
		}
		
		public SimpleValleySystemState(SimpleValleySystemState state) {
			super(state);
			pseudorandom = state.pseudorandom ;
		}

		/////////////////////////////////////////////
		// serializable methods
		private void writeObject(java.io.ObjectOutputStream stream) throws IOException { 
			stream.writeInt(version) ;
			stream.writeInt(pseudorandom) ;
		}
		
		private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
			version = stream.readInt() ;
			pseudorandom = stream.readInt() ;
		}
		
		@SuppressWarnings("unused")
		private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
			throw new ClassNotFoundException("Stream does not match required system state structure.") ;
		}
		
	}
	
	
}
