package com.peaceray.quantro.view.game.blocks.asset.skin.component.neighborhood;

import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;


/**
 * A "NeighborhoodComponent" is primarily concerned with drawing extra detail to
 * the blocks that requires local neighborhood information.  Two passes are used
 * for this, because we expect to draw an identical block field many times
 * in a row, possibly with offsets.  The first pass analyzes the provided
 * blockfield and produces a short-encoded "neighborhood" value for each block.
 * The second pass provides the blockfield along with this neighborhood encoding
 * and asks us to draw.
 * 
 * In Standard skins, the Neighborhood component is used to draw inner and outer
 * shadows around walls.  Future responsibilities for this component will be any
 * that can define its operation using an encoding for each block, stored in a
 * short, probably representing the local-8 neighborhood.
 * 
 * Extensions of this class benefit from some inbuilt methods designed to collect
 * some standard "neighborhood" encodings.  Provide a behavior set and we can have
 * a grand old time.
 * 
 * USAGE NOTE: We plan to share the same SkinAsset instance (and its Components) between
 * BlockDrawers; e.g., a 'slave' BlockDrawer will keep a reference to its 'master'
 * BlockDrawer, and thus its associated SkinAssets.
 * 
 * Because of this, although any image (Bitmap) resource used by the skin necessarily
 * represents a specific block size, any block-size and block-position specific details
 * should be left outside of the Component (and of the SkinAsset itself).  This allows 
 * different BlockDrawer to pass different size information into the SkinAsset at draw 
 * time.
 * 
 * In this implementation, we keep block size information in BlockfieldMetadata.
 * 
 * @author Jake
 *
 */
public abstract class NeighborhoodComponent extends DrawComponent {
	
	
	/**
	 * 
	 * @author Jake
	 *
	 */
	public static class BlockfieldMetadata extends DrawComponent.BlockfieldMetadata {
		
		private short [][][][] mEncoding ;
		
		protected BlockfieldMetadata(
				int blockfieldRows, int blockfieldCols, int numEncodingSets ) {
			
			super( ) ;
			
			mEncoding = new short[numEncodingSets][2][blockfieldRows][blockfieldCols] ;
		}
		
		protected short [][][][] getEncoding() {
			return mEncoding ;
		}
		
		protected short [][][] getEncoding(int set) {
			return mEncoding[set] ;
		}
		
	}
	
}
