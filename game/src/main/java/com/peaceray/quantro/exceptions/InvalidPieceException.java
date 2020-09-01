package com.peaceray.quantro.exceptions;

import com.peaceray.quantro.model.pieces.Piece;

public class InvalidPieceException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1704607808794193339L;
	
	
	// Information from the piece
	public int type ;
	
	
	
	
	// Some constructors, given piece type.
	public InvalidPieceException( int type ) {
		this( type, "", null ) ;
	}
	
	public InvalidPieceException( int type, String mesg ) {
		this( type, mesg, null ) ;
	}
	
	public InvalidPieceException( int type, String mesg, Throwable cause ) {
		super( mesg, cause ) ;
		
		this.type = type ;
	}
	
	public InvalidPieceException( int type, Throwable cause ) {
		this( type, "", cause ) ;
	}
	
	// Some more constructors, given a real piece.
	public InvalidPieceException( Piece p ) {
		this( p, "", null ) ;
	}
	
	public InvalidPieceException( Piece p, String mesg ) {
		this( p, mesg, null ) ;
	}
	
	public InvalidPieceException( Piece p, String mesg, Throwable cause ) {
		super( mesg, cause ) ;
		
		this.type = p.type ;
	}
	
	public InvalidPieceException( Piece p, Throwable cause ) {
		this( p, "", cause ) ;
	}

}
