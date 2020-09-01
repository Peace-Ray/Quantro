package com.peaceray.quantro.model.pieces.bags;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.utils.IntStack;

public class VersionedPieceBagState implements Serializable {


	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5664086869207935822L;
	
	

	private static final int VERSION = 0 ;
	// First version; identical to SimplePieceBagState, excepts gets default rotation.

	public IntStack pieceTypeStack ;
	public IntStack pieceDefaultRotationStack ;
	public int last ;
	public int num ;
	
	
	/////////////////////////////////////////////
	// constructor
	public VersionedPieceBagState() {
		pieceTypeStack = new IntStack() ;
		pieceDefaultRotationStack = new IntStack() ;
		last = -1 ;
		num = 0 ;
	}
	
	public VersionedPieceBagState( VersionedPieceBagState spbs ) {
		pieceTypeStack = new IntStack( spbs.pieceTypeStack ) ;
		pieceDefaultRotationStack = new IntStack( spbs.pieceDefaultRotationStack ) ;
		last = spbs.last ;
		num = spbs.num ;
	}
	
	public VersionedPieceBagState( SimplePieceBagState spbs ) {
		pieceTypeStack = new IntStack( spbs.pieceTypeStack ) ;
		pieceDefaultRotationStack = new IntStack() ;
		for ( int i = 0; i < pieceTypeStack.count(); i++ )
			pieceDefaultRotationStack.push(0) ;
		last = spbs.last ;
		num = spbs.num ;
	}
	
	/////////////////////////////////////////////
	// serializable methods
	private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
		stream.writeInt(VERSION) ;
		stream.writeObject( pieceTypeStack.toString() ) ;
		stream.writeObject( pieceDefaultRotationStack.toString() ) ;
		stream.writeInt(last) ;
		stream.writeInt(num) ;
	}
	
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
		int version = stream.readInt() ;
		if ( version != VERSION )
			throw new IllegalArgumentException("Can't process version number " + version) ;
		pieceTypeStack = new IntStack( (String)stream.readObject() ) ;
		pieceDefaultRotationStack = new IntStack( (String)stream.readObject() ) ;
		last = stream.readInt();
		num = stream.readInt() ;
	}
	
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException, ClassNotFoundException {
		throw new ClassNotFoundException("Stream does not match required system state structure.") ;
	}
	
	public VersionedPieceBagState qUpconvert() {
		VersionedPieceBagState vpbs = new VersionedPieceBagState() ;
		
		IntStack tempStack = new IntStack() ;
		while( this.pieceTypeStack.count() > 0 )
			tempStack.push( pieceTypeStack.pop() ) ;
		while ( tempStack.count() > 0 ) {
			int type = tempStack.pop() ;
			pieceTypeStack.push(type) ;
			vpbs.pieceTypeStack.push( PieceCatalog.qUpconvert(type) ) ;
		}
		
		while( this.pieceDefaultRotationStack.count() > 0 )
			tempStack.push( pieceDefaultRotationStack.pop() ) ;
		while ( tempStack.count() > 0 ) {
			int rot = tempStack.pop() ;
			pieceDefaultRotationStack.push(rot) ;
			vpbs.pieceDefaultRotationStack.push( rot ) ;
		}
		
		// last is used by UniformSpecial to hold its own "special category"
		// value.  It does not correspond to a piece type.
		vpbs.last = this.last ;
		vpbs.num = this.num ;
		
		return vpbs ;
	}
	
}
