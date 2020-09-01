package com.peaceray.quantro.view.game.blocks.asset.skin.component;

import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts.FieldType;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;

public abstract class DrawComponent extends Component {

	
	

	/**
	 * 
	 * @author Jake
	 */
	public static class BlockfieldMetadata {
		
	}
	
	
	/**
	 * Returns a new BlockfieldMetadata instance, which can be used to represent
	 * both information regarding global draw settings (e.g. block size) and 
	 * blockfield-specific data (e.g. the encoded value of each block's neighborhood). 
	 * 
	 * One instance should be created for every unique BlockField used by the BlockDrawer.
	 * That means (e.g.) the Ghost, the Piece, and the Stable blocks should each (probably)
	 * get their own Metadata Instance, 
	 * 
	 * Slave BlockDrawers, which share access to the same SkinAsset as their master,
	 * should create their own MetadataInstances (since the MetadataInstance stores
	 * 
	 * Subclasses should implement their own subclasses of BlockfieldMetadata, containing
	 * what information they need.  It's okay if that's "nothing at all" and all
	 * pixels can be easily determined by the provided blockfield at draw time.
	 * 
	 * @param blockfield
	 * @return
	 */
	public abstract BlockfieldMetadata newBlockfieldMetadataInstance(
			int blockfieldRows, int blockfieldCols,
			BlockSize blockSize ) ;
	
	
	/**
	 * Provides a blockfield and the field type; sets the provided
	 * BlockfieldMetadata object to whatever metadata is necessary
	 * to describe the blockfield.
	 * 
	 * This field and metadata will be provided (potentially multiple times)
	 * to the 'draw' method.
	 * 
	 * @param blockField
	 * @param fieldType
	 * @param configRange TODO
	 * @param blockfieldMetadata
	 */
	public abstract void getBlockfieldMetadata(
			byte [][][] blockField, FieldType fieldType, BlockDrawerConfigRange configRange, BlockfieldMetadata blockfieldMetadata ) ;
	

}
