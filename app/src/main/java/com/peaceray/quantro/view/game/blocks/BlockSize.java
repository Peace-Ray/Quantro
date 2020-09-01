package com.peaceray.quantro.view.game.blocks;

public class BlockSize {

	@SuppressWarnings("unused")
	private static final String TAG = "BlockDrawerConfigBlockGrid" ;
	
	int mBlockWidth ;
	int mBlockHeight ;
	
	int mQXOffset ;
	int mQYOffset ;
	
	public BlockSize( BlockSize config ) {
		this( config.mBlockWidth, config.mBlockHeight, config.mQXOffset, config.mQYOffset ) ;
	}
	
	public BlockSize( int width, int height, int qXOffset, int qYOffset ) {
		mBlockWidth = width ;
		mBlockHeight = height ;
		
		mQXOffset = qXOffset ;
		mQYOffset = qYOffset ;
	}
	
	public BlockSize( int width, int height, float qXOffset, float qYOffset ) {
		this( width, height, (int)Math.round( qXOffset * width), (int)Math.round( qYOffset * height ) ) ;
	}
	
	public int getBlockWidth() {
		return mBlockWidth ;
	}
	
	public int getBlockHeight() {
		return mBlockHeight ;
	}
	
	public int getQXOffset() {
		return mQXOffset ;
	}
	
	public int getQYOffset() {
		return mQYOffset ;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj )
			return true ;
		if ( !(obj instanceof BlockSize) )
			return false ;
		BlockSize bdcb = (BlockSize)obj ;
		return mBlockWidth == bdcb.mBlockWidth
				&& mBlockHeight == bdcb.mBlockHeight
				&& mQXOffset == bdcb.mQXOffset
				&& mQYOffset == bdcb.mQYOffset ;
	}
}
