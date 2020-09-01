package com.peaceray.quantro.exceptions;

import com.peaceray.quantro.model.pieces.Piece;
import com.peaceray.quantro.q.QCombinations;

public class GameSystemException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1704607808794193339L;
	
	// Carries a great deal of information from the game.
	public byte [][][] blockField ;
	public Piece piece ;
	
	
	
	
	// Some constructors, given piece type.
	public GameSystemException( byte [][][] blockField, Piece piece ) {
		this( blockField, piece, "", null ) ;
	}
	
	public GameSystemException( byte [][][] blockField, Piece piece, String mesg ) {
		this( blockField, piece, mesg, null ) ;
	}
	
	public GameSystemException( byte [][][] blockField, Piece piece, String mesg, Throwable cause ) {
		super( mesg, cause ) ;
		
		this.blockField = blockField ;
		this.piece = piece ;
	}
	
	public GameSystemException( byte [][][] blockField, Piece piece, Throwable cause ) {
		this( blockField, piece, "", cause ) ;
	}
	
	public void printContents() {
		// Represent blockField as a string...
		String bfs = "" ;
		for ( int r = blockField[0].length-1; r >= 0; r-- ) {
			for ( int c = 0; c < blockField[0][0].length; c++ ) {
				try {
					bfs = bfs + QCombinations.encode(blockField, r, c) + "\t" ;
				} catch (Exception e) {
					bfs = bfs + "_" + "\t" ;
				}
			}
			bfs = bfs + "\n" ;
		}
		
		System.err.print(bfs) ;
		System.err.print("" + piece.toString() + "\n" ) ;
		System.err.print("" + getMessage() + "\n" ) ;
	}

}
