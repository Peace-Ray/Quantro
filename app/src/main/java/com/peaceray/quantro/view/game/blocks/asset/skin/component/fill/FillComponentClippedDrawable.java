package com.peaceray.quantro.view.game.blocks.asset.skin.component.fill;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.Consts.FieldType;
import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.Component;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigBlockGrid;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;

/**
 * A fill component which contains a list of custom drawables.  Each
 * drawable is stored associated with a qPane and a list of qOrientations.
 * When drawing, for each drawable, the area covered by each block in
 * the associate list of drawables is clipped (as in, all pixels outside
 * of these are disallowed) and the drawable, set so its bounds are
 * the visible bounds of the block range, is drawn there.
 * 
 * @author Jake
 *
 */
public class FillComponentClippedDrawable extends FillComponent {
	
	/**
	 * 
	 * @author Jake
	 *
	 */
	protected static class BlockfieldMetadata extends DrawComponent.BlockfieldMetadata {
		
	}

	private Hashtable<BlockSize, ArrayList<ArrayList<DrawableData>>> mDrawableDatas ;
	private ArrayList<ArrayList<DrawableData>> mGlobalDrawableDatas ;
	
	
	public FillComponentClippedDrawable() {
		mDrawableDatas = new Hashtable<BlockSize, ArrayList<ArrayList<DrawableData>>>() ;
		
		mGlobalDrawableDatas = new ArrayList<ArrayList<DrawableData>>() ;
		mGlobalDrawableDatas.add( new ArrayList<DrawableData>() ) ;
		mGlobalDrawableDatas.add( new ArrayList<DrawableData>() ) ;
	}
	
	
	public void clear() {
		mDrawableDatas.clear() ;
		mGlobalDrawableDatas.clear() ;
	}
	
	public void addDrawable( Drawable d, int ...qorientations ) {
		if ( qorientations == null || qorientations.length == 0 )
			return ;
			
		DrawableData dd = new DrawableData(d, qorientations) ;
		addDrawableData(dd) ;
	}
	
	public void addDrawable( BlockSize bs, Drawable d, int ...qorientations ) {
		if ( qorientations == null || qorientations.length == 0 )
			return ;
			
		DrawableData dd = new DrawableData(d, qorientations) ;
		// if the block size is not available in the list, add it.
		addBlockSizeIfNeeded( bs ) ;
		addDrawableData( bs, 0, dd ) ;
		addDrawableData( bs, 1, dd ) ;
	}
	
	public void addDrawable( int qPane, Drawable d, int ... qorientations ) {
		if ( qorientations == null || qorientations.length == 0 )
			return ;
			
		DrawableData dd = new DrawableData(d, qorientations) ;
		addDrawableData( qPane, dd ) ;
	}
	
	public void addDrawable( BlockSize bs, int qPane, Drawable d, int ...qorientations ) {
		if ( qorientations == null || qorientations.length == 0 )
			return ;
			
		DrawableData dd = new DrawableData(d, qorientations) ;
		// if the block size is not available in the list, add it.
		addBlockSizeIfNeeded( bs ) ;
		addDrawableData( bs, qPane, dd ) ;
	}
	
	/**
	 * Adds the provided drawable data to all qPanes for all
	 * block sizes.
	 * 
	 * @param dd
	 */
	private void addDrawableData( DrawableData dd ) {
		addDrawableData( 0, dd ) ;
		addDrawableData( 1, dd ) ;
	}
	
	/**
	 * Adds the provided drawable data to the provided qPane
	 * for all block sizes.
	 * 
	 * @param qPane
	 * @param dd
	 */
	private void addDrawableData( int qPane, DrawableData dd ) {
		// add to globals...
		mGlobalDrawableDatas.get(qPane).add(dd) ;
		// add to block-size specific...
		Enumeration<BlockSize> enumeration = mDrawableDatas.keys() ;
		for ( ; enumeration.hasMoreElements() ; ) {
			BlockSize bs = enumeration.nextElement() ;
			addDrawableData( bs, qPane, dd ) ;
		}
	}
	
	/**
	 * Adds the provided drawable data to the provided qPane for only the
	 * specified block size.
	 * 
	 * @param bs
	 * @param qPane
	 * @param dd
	 */
	private void addDrawableData( BlockSize bs, int qPane, DrawableData dd ) {
		ArrayList<ArrayList<DrawableData>> list = mDrawableDatas.get(bs) ;
		list.get(qPane).add(dd) ;
	}
	
	
	/**
	 * Creates space in mDrawableData which contains all of
	 * the universal 
	 * @param bs
	 */
	private void addBlockSizeIfNeeded( BlockSize bs ) {
		if ( mDrawableDatas.containsKey(bs) )
			return ;
		
		ArrayList<ArrayList<DrawableData>> newList = new ArrayList<ArrayList<DrawableData>>() ;
		// we don't clone directly, because that
		// would keep references to the inner lists, and we
		// want them to change independently.
		newList.add( (ArrayList<DrawableData>)mGlobalDrawableDatas.get(0).clone() ) ;
		newList.add( (ArrayList<DrawableData>)mGlobalDrawableDatas.get(1).clone() ) ;
		
		mDrawableDatas.put(bs, newList) ;
	}
	
	
	Path mDrawablePath = new Path() ;
	Path mTempPath = new Path() ;
	Path mTempPath2 = new Path() ;
	Rect mTempRect = new Rect() ;

	public void draw( Canvas canvas, BlockDrawerConfigBlockGrid configBlocks,
			byte [][][] blockField, int qPane, BlockDrawerConfigRange configRange  ) {
		
		if ( qPane != Consts.QPANE_0 && qPane != Consts.QPANE_1 ) {
			throw new IllegalArgumentException("Only supports QPane 0 and 1.") ;
		}
		
		BlockSize blockSize = configBlocks.getBlockSize() ;
		Regions regions = getPreparedRegions( blockSize ) ;
			
		int oppositeQPane = (qPane + 1) % 2 ;

		mTempRect.set(
				configBlocks.getX(qPane, configRange.columnFirst),
				configBlocks.getY(qPane, configRange.rowFirst),
				configBlocks.getX(qPane, configRange.columnBound),
				configBlocks.getY(qPane, configRange.rowBound)) ;
		
		ArrayList<DrawableData> ar = mDrawableDatas.get(configBlocks.getBlockSize()).get(qPane) ;
		
		for ( int i = 0; i < ar.size(); i++ ) {
			DrawableData dd = ar.get(i) ;
			
			// Our goal is to create a region clip for 'dd', including all the
			// qOrientations listed.  We iterate through, populating mDrawableRegion.
			mDrawablePath.reset() ;
			
			for ( int row = configRange.rowFirst; row < configRange.rowBound; row++ ) {
				int r = row + configRange.insetRow ;
				int y = configBlocks.getY(qPane, row) ;
				
				for ( int col = configRange.columnFirst; col < configRange.columnBound; col++ ) {
					int c = col + configRange.insetColumn ;
					int qo = blockField[qPane][r][c] ;
					
					if ( !dd.hasQOrientation(qo) )
						continue ;
					
					// Skip this draw?
					BlockRegion blockRegion = regions.getBlockRegion(qPane, qo) ;
					if ( blockRegion == BlockRegion.NONE ) {
						continue ;
					}
					
					int x = configBlocks.getX(qPane, col) ;
					
					Rect rect = regions.fitFillRect(blockRegion, x, y) ;
					
					// clip this region.  If it's 'nonoverlap', we do a three-step:
					// clip to mTempRegion, difference with the overlapping region,
					// and then clip to mDrawableRegion.  Otherwise, we clip directly
					// to mDrawableRegion.
					if ( blockRegion == BlockRegion.INSET_MAJOR_NONOVERLAP ) {
						mTempPath.reset();
						mTempPath.addRect(rect.left, rect.top, rect.right, rect.bottom, Path.Direction.CW) ;
						int oppositeQO = blockField[oppositeQPane][r][c] ;
						BlockRegion oppositeFillRegion = regions.getBlockRegion(oppositeQPane, oppositeQO) ;

						Rect oppositeRect = regions.fitFillRect(
								oppositeFillRegion,
								configBlocks.getX(oppositeQPane, col),
								configBlocks.getY(oppositeQPane, row)) ;
						mTempPath2.reset();
						mTempPath2.addRect(
								oppositeRect.left, oppositeRect.top,
								oppositeRect.right, oppositeRect.bottom,
								Path.Direction.CW);

						mTempPath.op(mTempPath2, Path.Op.DIFFERENCE) ;
						mDrawablePath.op(mTempPath, Path.Op.UNION) ;
					} else {
						mDrawablePath.addRect(rect.left, rect.top, rect.right, rect.bottom, Path.Direction.CW) ;
					}
				}
			}
			
			// okay!  Draw the drawable.
			if ( !mDrawablePath.isEmpty() ) {
				canvas.save() ;
				canvas.clipPath(mDrawablePath) ;
				
				Drawable d = dd.getDrawable() ;
				d.setBounds(mTempRect) ;
				d.draw(canvas) ;
				
				canvas.restore() ;
 			}
		}
	}
	
	
	
	private class DrawableData {
		
		Drawable mDrawable ;
		int [] mQOrientations ;
		
		private DrawableData( Drawable d, int [] qorientations ) {
			mDrawable = d ;
			mQOrientations = qorientations ;
		}
		
		private Drawable getDrawable() {
			return mDrawable ;
		}
		
		@SuppressWarnings("unused")
		private int [] getQOrientations() {
			return mQOrientations ;
		}
		
		private boolean hasQOrientation( int qo ) {
			for ( int i = 0; i < mQOrientations.length; i++ ) {
				if ( qo == mQOrientations[i] )
					return true ;
			}
			return false ;
		}
	}
	
	
	@Override
	public void prepare( Context context, BlockDrawerPreallocatedBitmaps preallocated, Bitmap scratch,
			Hashtable<BlockSize, Regions> regions,
			int imageSize, int loadSize ) throws IOException {
		
		super.prepare(context, preallocated, scratch, regions, imageSize, loadSize) ;
		
		// check that there are no drawable lists for unprepared block sizes.
		Enumeration<BlockSize> enumeration = mDrawableDatas.keys() ;
		for ( ; enumeration.hasMoreElements() ; ) {
			BlockSize bs = enumeration.nextElement() ;
			if ( !regions.containsKey(bs) ) {
				throw new IllegalStateException("Drawables given for BlockSizes that were not prepared.") ;
			}
		}
		
		// get a list for every block size.
		enumeration = regions.keys() ;
		for ( ; enumeration.hasMoreElements() ; ) {
			BlockSize bs = enumeration.nextElement() ;
			addBlockSizeIfNeeded(bs) ;
		}
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
	 */
	public BlockfieldMetadata newBlockfieldMetadataInstance(
			int blockfieldRows, int blockfieldCols,
			BlockSize blockSize ) {
		
		return new BlockfieldMetadata( ) ;
	}
	
	
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
	 * @param blockfieldMetadata
	 */
	public void getBlockfieldMetadata(
			byte [][][] blockField, FieldType fieldType, BlockDrawerConfigRange configRange, DrawComponent.BlockfieldMetadata blockfieldMetadata ) {
		
		// nothing to do.
		
	}
	
}
