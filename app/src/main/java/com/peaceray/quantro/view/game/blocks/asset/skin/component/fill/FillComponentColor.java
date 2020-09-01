package com.peaceray.quantro.view.game.blocks.asset.skin.component.fill;

import java.io.IOException;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;
import com.peaceray.quantro.view.game.blocks.Consts.FieldType;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.Component;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigBlockGrid;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;


/**
 * SolidColorFillComponent: an implementation of FillComponent which draws solid
 * colors within each block.  There are a few important settings:
 * 
 * 1. The color to use for each qPane / qOrientation tuple.
 * 2. The 'alpha' value to use for filling blocks, pieces, and ghosts.
 * 3. Whether each qOrientation 'pulses.'  A 'pulse' is an animation
 * 		with one feature: the alpha value of the fill is at least partially
 * 		determined by the current time.  Block drawers can implement this
 * 		in two ways: first by drawing the fill with full alpha and then
 * 		blitting with its own scaled alpha, or second by requesting repeated
 * 		draws.
 * 4. The fill 'region' to apply to each qOrientation / qPane.
 * 		For now, we disallow custom sizes, instead using an enumeration:
 * 		we can in the future extend the enumeration with new values, or
 * 		include a 'custom' setting.
 * 
 * @author Jake
 *
 */
public class FillComponentColor extends FillComponent {


	/**
	 * No data needed.
	 * @author Jake
	 *
	 */
	protected static class BlockfieldMetadata extends DrawComponent.BlockfieldMetadata {
		
	}
	
	
	public enum FillType {
		
		/**
		 * The region exists, but we don't draw anything in it.
		 */
		NONE,
		
		/**
		 * Draws a solid color.
		 */
		SOLID,
		
		/**
		 * Draws a solid color which we pulse.
		 */
		SOLID_PULSE,
		
		/**
		 * "Friendly" represents a color blend from this block
		 * neighboring blocks.
		 */
		FRIENDLY	
	}
	
	
	public enum DrawType {
		
		/**
		 * Draws all fills.
		 */
		ALL,
		
		
		/**
		 * Draws only 'non pulsing' fills.
		 */
		NON_PULSE,
		
		
		/**
		 * Draws only 'pulse' fills.
		 */
		PULSE,
		
	}
	
	
	private ColorScheme mColorScheme ;
	
	private Paint mPaint ;
	
	private int [] mAlpha_byFieldType ;
	private int [][][] mAlpha_byQPaneQOrientationFieldType ;
	
	private FillType [][] mFillType_byQPaneQOrientation ;
	
	private int [][] mFillColor_byQPaneQOrientation ;
	private Drawable [][][][] mFillFriendlyDrawable_byQPaneQOrientationDirection ;
	
	private static final int INDEX_LEFT = 0 ;
	private static final int INDEX_RIGHT = 1 ;
	private static final int INDEX_UP = 2 ;
	private static final int INDEX_DOWN = 3 ;
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONSTRUCTION / CONFIGURATION
	//

	public FillComponentColor( ColorScheme colorScheme ) {
		if ( colorScheme == null )
			throw new NullPointerException("You gave a null colorscheme.") ;
		
		mColorScheme = colorScheme ;
		
		mPaint = new Paint() ;
		
		mAlpha_byFieldType = new int[FieldType.values().length] ;
		mAlpha_byQPaneQOrientationFieldType = new int[FieldType.values().length][2][QOrientations.NUM] ;
		
		setAlphas( 255, 255, 128 ) ;
		setDefaults() ;
	}
	
	
	public void setAlpha( FieldType fieldType, int alpha ) {
		int ord = fieldType.ordinal() ;
		mAlpha_byFieldType[ord] = alpha ;
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				mAlpha_byQPaneQOrientationFieldType[ord][qp][qo] = alpha ;
			}
		}
	}
	
	public void setAlphas( int fieldAlpha, int pieceAlpha, int ghostAlpha ) {
		setAlpha( FieldType.FIELD, fieldAlpha ) ;
		setAlpha( FieldType.PIECE, pieceAlpha ) ;
		setAlpha( FieldType.GHOST, ghostAlpha ) ;
	}
	
	public void setAlphaFade( int qOrientation, float fieldFade, float pieceFade, float ghostFade ) {
		setAlphaFade( 0, qOrientation, fieldFade, pieceFade, ghostFade ) ;
		setAlphaFade( 1, qOrientation, fieldFade, pieceFade, ghostFade ) ;
	}
	
	public void setAlphaFade( int qPane, int qOrientation, float fieldFade, float pieceFade, float ghostFade ) {
		setAlphaFade( qPane, qOrientation, FieldType.FIELD, fieldFade ) ;
		setAlphaFade( qPane, qOrientation, FieldType.PIECE, pieceFade ) ;
		setAlphaFade( qPane, qOrientation, FieldType.GHOST, ghostFade ) ;
	}
	
	public void setAlphaFade( int qPane, int qOrientation, FieldType fieldType, float fadeFactor ) {
		int ord = fieldType.ordinal() ;
		mAlpha_byQPaneQOrientationFieldType[ord][qPane][qOrientation] =
			(int)Math.round( mAlpha_byFieldType[ord]*fadeFactor ) ;
	}
	
	
	/**
	 * Sets the fill behavior for the specified qOrientation in both panes.
	 * 
	 * Returns this object for chaining.
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @param fillRegion
	 * @param fillType
	 * @return
	 */
	public void setFill( int qOrientation, BlockRegion fillRegion, FillType fillType ) {
		setFill( 0, qOrientation, fillRegion, fillType ) ;
		setFill( 1, qOrientation, fillRegion, fillType ) ;
	}
	
	
	/**
	 * Sets the fill behavior for the specified qPane / qOrientation.
	 * 
	 * Returns this object for chaining.
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @param fillRegion
	 * @param fillType
	 * @return
	 */
	public void setFill( int qPane, int qOrientation, BlockRegion blockRegion, FillType fillType ) {
		mFillType_byQPaneQOrientation[qPane][qOrientation] = fillType ;
		
		if ( fillType != FillType.FRIENDLY )
			mFillFriendlyDrawable_byQPaneQOrientationDirection[qPane][qOrientation] = null ;
		else if ( mFillFriendlyDrawable_byQPaneQOrientationDirection[qPane][qOrientation] == null )
			mFillFriendlyDrawable_byQPaneQOrientationDirection[qPane][qOrientation] = new Drawable[QOrientations.NUM][] ;
	}
	
	
	public void setColorFade( byte qOrientation, float fadeFactor ) {
		setColorFade( 0, qOrientation, fadeFactor ) ;
		setColorFade( 1, qOrientation, fadeFactor ) ;
	}
	
	public void setColorFade( int qPane, byte qOrientation, float fadeFactor ) {
		int color = mColorScheme.getFillColor(qOrientation, qPane) ;
		color = Color.argb(255,
				(int)Math.round( Color.red(color)  *fadeFactor),
				(int)Math.round( Color.green(color)*fadeFactor),
				(int)Math.round( Color.blue(color) *fadeFactor) ) ;
		mFillColor_byQPaneQOrientation[qPane][qOrientation] = color ;
	}
	
	
	public void setFriendly( int qOrientationFrom, int qOrientationTo, boolean friendly ) {
		setFriendly( 0, qOrientationFrom, qOrientationTo, friendly ) ;
		setFriendly( 1, qOrientationFrom, qOrientationTo, friendly ) ;
	}
	
	/**
	 * Sets that qOrientationFrom, which must have FillType FRIENDLY,
	 * should allow 'friendly transitions' to qOrientationTo.
	 * @param qPane
	 * @param qOrientationFrom
	 * @param qOrientationTo
	 * @return
	 */
	public void setFriendly( int qPane, int qOrientationFrom, int qOrientationTo, boolean friendly ) {
		
		if ( this.mFillType_byQPaneQOrientation[qPane][qOrientationFrom] != FillType.FRIENDLY )
			throw new IllegalArgumentException("QOrientation " + qOrientationFrom + " is not friendly.") ;
		
		if ( !friendly ) {
			mFillFriendlyDrawable_byQPaneQOrientationDirection[qPane][qOrientationFrom][qOrientationTo] = null ;
		} else {
			if ( mFillFriendlyDrawable_byQPaneQOrientationDirection[qPane][qOrientationFrom][qOrientationTo] == null )
				mFillFriendlyDrawable_byQPaneQOrientationDirection[qPane][qOrientationFrom][qOrientationTo] = new Drawable[4] ;
		
			// make the drawables.
			int colorFrom = mFillColor_byQPaneQOrientation[qPane][qOrientationFrom] ;
			int colorTo = mFillColor_byQPaneQOrientation[qPane][qOrientationTo] ;
			
			Drawable [] blends = mFillFriendlyDrawable_byQPaneQOrientationDirection[qPane][qOrientationFrom][qOrientationTo] ;
			
			blends[INDEX_LEFT] = new GradientDrawable(
					GradientDrawable.Orientation.RIGHT_LEFT,
					new int[] { colorFrom, colorTo });
			blends[INDEX_RIGHT] = new GradientDrawable(
					GradientDrawable.Orientation.LEFT_RIGHT,
					new int[] { colorFrom, colorTo });
			blends[INDEX_UP] = new GradientDrawable(
					GradientDrawable.Orientation.BOTTOM_TOP,
					new int[] { colorFrom, colorTo });
			blends[INDEX_DOWN] = new GradientDrawable(
					GradientDrawable.Orientation.TOP_BOTTOM,
					new int[] { colorFrom, colorTo });
		}
	}
	
	
	private void setDefaults() {
		setDefaultFillTypes() ;
		setDefaultColors() ;
		setDefaultFillDrawables() ;
	}
	
	
	private void setDefaultFillTypes() {
		if ( mFillType_byQPaneQOrientation == null )
			mFillType_byQPaneQOrientation = new FillType[2][QOrientations.NUM] ;
		
		// default to solid color
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				mFillType_byQPaneQOrientation[qp][qo] = FillType.SOLID ;
			}
		}
		
		// customization
		for ( int qp = 0; qp < 2; qp++ ) {
			// pulse!
			mFillType_byQPaneQOrientation[qp][QOrientations.SL_ACTIVE] = FillType.SOLID_PULSE ;
			
			// friendly!
			mFillType_byQPaneQOrientation[qp][QOrientations.S0_FROM_SL] = FillType.FRIENDLY ;
			mFillType_byQPaneQOrientation[qp][QOrientations.S1_FROM_SL] = FillType.FRIENDLY ;
		}
	}
	
	private void setDefaultColors() {
		if ( mFillColor_byQPaneQOrientation == null )
			mFillColor_byQPaneQOrientation = new int[2][QOrientations.NUM] ;
		
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
				mFillColor_byQPaneQOrientation[qp][qo] = mColorScheme.getFillColor(qo, qp) ;
			}
		}
	}
	
	private void setDefaultFillDrawables() {
		if ( mFillFriendlyDrawable_byQPaneQOrientationDirection == null )
			mFillFriendlyDrawable_byQPaneQOrientationDirection = new Drawable[2][QOrientations.NUM][][] ;
		
		setFriendly(QOrientations.S0_FROM_SL, QOrientations.SL, true) ;
		setFriendly(QOrientations.S1_FROM_SL, QOrientations.SL, true) ;
	}
	
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// DRAWING STUFF
	//
	// Drawing content to the Canvas.
	//
	
	
	
	public void draw( Canvas canvas, BlockDrawerConfigBlockGrid configBlocks,
			byte [][][] blockField, FieldType fieldType, int qPane, BlockDrawerConfigRange configRange,
			DrawType drawType, float pulseAlphaScale ) {
		
		if ( qPane != Consts.QPANE_0 && qPane != Consts.QPANE_1 ) {
			throw new IllegalArgumentException("Only supports QPane 0 and 1.") ;
		}
		
		BlockSize blockSize = configBlocks.getBlockSize() ;
		Regions regions = getPreparedRegions( blockSize ) ;
		
		int oppositeQPane = (qPane + 1) % 2 ;
		
		boolean skipPulse = drawType != DrawType.PULSE && drawType != DrawType.ALL ;
		boolean skipNonPulse = drawType != DrawType.NON_PULSE && drawType != DrawType.ALL ;
		
		for ( int row = configRange.rowFirst; row < configRange.rowBound; row++ ) {
			int r = row + configRange.insetRow ;
			int y = configBlocks.getY(qPane, row) ;
			
			for ( int col = configRange.columnFirst; col < configRange.columnBound; col++ ) {
				int c = col + configRange.insetColumn ;
				int qo = blockField[qPane][r][c] ;
				
				// Skip this draw?
				FillType fillType = this.mFillType_byQPaneQOrientation[qPane][qo] ;
				if ( ( skipPulse && fillType == FillType.SOLID_PULSE )
						|| ( skipNonPulse && fillType  != FillType.SOLID_PULSE )
						|| fillType == FillType.NONE ) {
					continue ;
				}
				
				BlockRegion blockRegion = regions.getBlockRegion(qPane, qo) ;
				if ( blockRegion == BlockRegion.NONE ) {
					continue ;
				}
				
				int alpha = mAlpha_byQPaneQOrientationFieldType[qPane][qo][fieldType.ordinal()] ;
				if ( fillType == FillType.SOLID_PULSE ) {
					if (pulseAlphaScale > 0)
						alpha = Math.round((255 - alpha) * pulseAlphaScale + alpha);
					else
						alpha = Math.round(alpha * (1.0f + pulseAlphaScale));
				}
				
				int x = configBlocks.getX(qPane, col) ;
				
				// draw in three steps.  First, clip a cut-out of the other
				// qPane, if we're drawing "NONOVERLAP."  Second, set our alpha and
				// color appropriately and draw this rectangle.  Third, unclip
				// if we clipped.
				boolean needsRestore = false ;
				if ( blockRegion == BlockRegion.INSET_MAJOR_NONOVERLAP ) {
					// perform the clip for the opposite end.
					int oppositeQO = blockField[oppositeQPane][r][c] ;
					BlockRegion oppositeBlockRegion = regions.getBlockRegion(oppositeQPane, oppositeQO) ;
					Rect oppositeRect = regions.fitFillRect(
							oppositeBlockRegion,
							configBlocks.getX(oppositeQPane, col),
							configBlocks.getY(oppositeQPane, row)) ;
					
					if ( oppositeRect != null ) {
						canvas.save() ;
						canvas.clipRect( oppositeRect, Region.Op.DIFFERENCE ) ;
						needsRestore = true ;
					}
				}
				
				// get the color / drawable to draw...
				int color = 0 ;
				Drawable drawable = null ;
				if ( fillType == FillType.FRIENDLY ) {
					// draw friendly!
					drawable = findFriendlyDrawable( blockField, qPane, r, c ) ;
				} else {
					color = mFillColor_byQPaneQOrientation[qPane][qo] ;
				}
				
				// aaaand draw!
				Rect rect = regions.fitFillRect(blockRegion, x, y) ;
				
				if ( drawable != null ) {
					drawable.setBounds(rect) ;
					drawable.setAlpha(alpha) ;
					drawable.draw(canvas) ;
				} else {
					mPaint.setColor(color) ;
					mPaint.setAlpha(alpha) ; 
					canvas.drawRect(rect, mPaint) ;
				}
				
				// unclip, if needed.
				if ( needsRestore ) {
					canvas.restore() ;
				}
			}
		}
	}
	
	
	private Drawable findFriendlyDrawable(byte[][][] blockField,
			int qPane, int r, int c ) {
		
		Drawable [][][][] drawables = this.mFillFriendlyDrawable_byQPaneQOrientationDirection ;
		
		int qo = blockField[qPane][r][c];
		Drawable d = null;

		if (drawables != null && drawables[qPane][qo] != null) {
			int qo1 = blockField[qPane][r + 1][c];
			if (drawables[qPane][qo][qo1][INDEX_UP] != null)
				d = drawables[qPane][qo][qo1][INDEX_UP];
			qo1 = blockField[qPane][r - 1][c];
			if (drawables[qPane][qo][qo1][INDEX_DOWN] != null)
				d = drawables[qPane][qo][qo1][INDEX_DOWN];
			qo1 = blockField[qPane][r][c - 1];
			if (drawables[qPane][qo][qo1][INDEX_LEFT] != null)
				d = drawables[qPane][qo][qo1][INDEX_LEFT];
			qo1 = blockField[qPane][r][c + 1];
			if (drawables[qPane][qo][qo1][INDEX_RIGHT] != null)
				d = drawables[qPane][qo][qo1][INDEX_RIGHT];
			if (d != null)
				return d;

			// now try to "extend" a friendly blend from a neighbor square.
			// Our proceedure is: if a neighbor is the same QO, it has
			// a friendly neighbor, and the two neighbor relationships
			// are perpendicular (NOT linear), then take the blend that we would
			// use.
			for (int direction = 0; direction < 4; direction++) {
				int qoNeighbor = 0;
				int rNeighbor = r, cNeighbor = c;
				switch (direction) {
				case INDEX_UP:
					rNeighbor--;
					break;
				case INDEX_DOWN:
					rNeighbor++;
					break;
				case INDEX_LEFT:
					cNeighbor--;
					break;
				case INDEX_RIGHT:
					cNeighbor++;
					break;
				}
				qoNeighbor = blockField[qPane][rNeighbor][cNeighbor];
				if (qoNeighbor == qo) {
					// whelp, try looking for the neighbor for this block.
					qo1 = blockField[qPane][rNeighbor + 1][cNeighbor];
					if ((direction == INDEX_LEFT || direction == INDEX_RIGHT)
							&& drawables[qPane][qo][qo1][INDEX_UP] != null)
						d = drawables[qPane][qo][qo1][INDEX_UP];
					qo1 = blockField[qPane][rNeighbor - 1][cNeighbor];
					if ((direction == INDEX_LEFT || direction == INDEX_RIGHT)
							&& drawables[qPane][qo][qo1][INDEX_DOWN] != null)
						d = drawables[qPane][qo][qo1][INDEX_DOWN];
					qo1 = blockField[qPane][rNeighbor][cNeighbor - 1];
					if ((direction == INDEX_UP || direction == INDEX_DOWN)
							&& drawables[qPane][qo][qo1][INDEX_LEFT] != null)
						d = drawables[qPane][qo][qo1][INDEX_LEFT];
					qo1 = blockField[qPane][rNeighbor][cNeighbor + 1];
					if ((direction == INDEX_UP || direction == INDEX_DOWN)
							&& drawables[qPane][qo][qo1][INDEX_RIGHT] != null)
						d = drawables[qPane][qo][qo1][INDEX_RIGHT];
				}
				if (d != null)
					return d;
			}
		}

		return d;
	}
	
	
	@Override
	public void prepare( Context context, BlockDrawerPreallocatedBitmaps preallocated, Bitmap scratch,
			Hashtable<BlockSize, Regions> regions,
			int imageSize, int loadSize ) throws IOException {
		
		super.prepare(context, preallocated, scratch, regions, imageSize, loadSize) ;
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
	public BlockfieldMetadata newBlockfieldMetadataInstance(
			int blockfieldRows, int blockfieldCols,
			BlockSize blockSize ) {
		
		return new BlockfieldMetadata() ;
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
