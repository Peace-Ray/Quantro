package com.peaceray.quantro.view.game.blocks.asset.skin.regions;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;

public class BlockRegions {
	
	// DEFAULTS:
	// Settings for BlockRegion, size information, fill-regions, border sizes,
	// etc.  May want to create your own for subclasses.
	private static final BlockRegion [][] BLOCK_REGION_BY_QPANE_QORIENTATION = new BlockRegion[2][QOrientations.NUM] ;
	
	static {
		// set default block regions for QOrientations.
		for ( int qp = 0; qp < 2; qp++ ) {
			// FULL by default.
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				BLOCK_REGION_BY_QPANE_QORIENTATION[qp][qo] = BlockRegion.FULL ;
			}
			
			// special configuration.  No fill for NO:
			BLOCK_REGION_BY_QPANE_QORIENTATION[qp][QOrientations.NO] = BlockRegion.NONE ;
			BLOCK_REGION_BY_QPANE_QORIENTATION[qp][QOrientations.F0] = BlockRegion.INSET_MINOR ; 
			BLOCK_REGION_BY_QPANE_QORIENTATION[qp][QOrientations.F1] = BlockRegion.INSET_MINOR ; 
			BLOCK_REGION_BY_QPANE_QORIENTATION[qp][QOrientations.U0] = BlockRegion.INSET_MAJOR ; 
			BLOCK_REGION_BY_QPANE_QORIENTATION[qp][QOrientations.U1] = BlockRegion.INSET_MAJOR ; 
			BLOCK_REGION_BY_QPANE_QORIENTATION[qp][QOrientations.UL] = BlockRegion.INSET_MAJOR_NONOVERLAP ; 
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	//
	// NOTE: OVERRIDE THESE TO CREATE A SUBCLASS OR DIFFERENT BEHAVIOR.
	
	protected BlockRegion [][] getDefaultBlockRegions_byQPaneQOrientation() {
		if ( this.getClass() != BlockRegions.class )
			throw new UnsupportedOperationException("This subclass did not override getDefaultBlockRegions_byQPaneQOrientation") ;
		return deepCopy( BLOCK_REGION_BY_QPANE_QORIENTATION ) ;
	}
	
	protected static final <T> T [][] deepCopy( T [][] ar ) {
		T [][] ar2 = ar.clone() ;
		for ( int i = 0; i < ar.length; i++ )
			ar2[i] = ar[i].clone() ;
		return ar2 ;
	}
	
	

	private BlockRegion [][] mBlockRegion_byQPaneQOrientation ;
	
	BlockRegions() {
		mBlockRegion_byQPaneQOrientation = getDefaultBlockRegions_byQPaneQOrientation() ;
	}
	
	
	public final void setBlockRegion( int qPane, int qOrientation, BlockRegion blockRegion ) {
		mBlockRegion_byQPaneQOrientation[qPane][qOrientation] = blockRegion ;
	}
	
	public final BlockRegion getBlockRegion( int qPane, int qOrientation ) {
		return mBlockRegion_byQPaneQOrientation[qPane][qOrientation] ;
	}
	
	
}
