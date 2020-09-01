package com.peaceray.quantro.model.pieces;

public final class Offset {
	
	public int x ;
	public int y ;
	
	public Offset() {
		super() ;
		x = 0 ;
		y = 0 ;
	}
	
	public Offset( int x, int y ) {
		this.x = x ;
		this.y = y ;
	}
	
	public Offset( Offset o ) {
		x = o.x ;
		y = o.y ;
	}
	
	@Override
	public boolean equals( Object o ) {
		if ( o == null || !(o instanceof Offset))
			return false ;
		
		return ((Offset)o).x == x && ((Offset)o).y == y ;
	}
	
	@Override
	public String toString() {
		return "Offset:(" + x + ", " + y + ")" ;
	}
	
	public void takeVals( Offset o) {
		x = o.x ;
		y = o.y ;
	}
	
	public void setXY( int x, int y ) {
		this.x = x ;
		this.y = y ;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getX() {
		return x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getY() {
		return y;
	}
	
	// Same as above, just different labels
	public void setRowCol( int row, int col ) {
		this.x = col ;
		this.y = row ;
	}
	
	public void setCol(int col) {
		this.x = col;
	}

	public int getCol() {
		return x;
	}

	public void setRow(int row) {
		this.y = row;
	}

	public int getRow() {
		return y;
	}
	
	public void offsetXY( int x, int y ) {
		this.x += x ;
		this.y += y ;
	}
	
}
