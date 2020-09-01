package com.peaceray.quantro.model.pieces.bags;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.utils.IntStack;


/**
 * The SimplePieceBagState is shared by all currently implemented PieceBags
 * (RetroPieceBag, BinomialTetracubePieceBag, and UniformSpecialPieceBag).
 *
 * These classes represent "state" as a stack of piece types (integers),
 * as well as some generally useful information, such as 'last' and 'num'
 * (which we expect represent the last piece type generated and the number
 * of pieces - or the number of piece "shuffles" - respectively, but could
 * be used for anything).
 * 
 * TODO: Either revise this state, or give PieceBags their own unique state
 * classes, when we try to make persistent Random objects to generate
 * cheat-protected random events.
 *
 * @author Jake
 *
 */
class SimplePieceBagState implements Serializable {

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5328293585481759616L;

	public IntStack pieceTypeStack ;
	public int last ;
	public int num ;
	
	
	/////////////////////////////////////////////
	// constructor
	public SimplePieceBagState() {
		pieceTypeStack = new IntStack() ;
		last = -1 ;
		num = 0 ;
	}
	
	public SimplePieceBagState( SimplePieceBagState spbs ) {
		pieceTypeStack = new IntStack( spbs.pieceTypeStack ) ;
		last = spbs.last ;
		num = spbs.num ;
	}
	
	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeObject( pieceTypeStack.toString() ) ;
		stream.writeInt(last) ;
		stream.writeInt(num) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		pieceTypeStack = new IntStack( (String)stream.readObject() ) ;
		last = stream.readInt();
		num = stream.readInt() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
	
	
	/////////////////////////////////////////////
	// qupconvert
	public SimplePieceBagState qUpconvert() {
		SimplePieceBagState state = new SimplePieceBagState(this) ;
		// i whip my ints back and forth
		IntStack is = new IntStack() ;
		while( state.pieceTypeStack.count() > 0 )
			is.push( state.pieceTypeStack.pop() ) ;
		while ( is.count() > 0 )
			state.pieceTypeStack.push( PieceCatalog.qUpconvert( is.pop() ) ) ;
		return state ;
	}
}
