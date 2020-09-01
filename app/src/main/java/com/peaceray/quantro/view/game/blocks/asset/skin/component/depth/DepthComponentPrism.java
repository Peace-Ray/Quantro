package com.peaceray.quantro.view.game.blocks.asset.skin.component.depth;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.Consts.FieldType;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.Component;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.Render;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;


/**
 * DepthComponentPrism allows geometric "prisms" to be represented as the
 * 3D component of a blockfield.  A "Prism" is represented in the geometric
 * sense -- a polygon which is translated between qPanes, and with an extruded
 * side for each edge.
 * 
 * NOTE: "Prism" is meant to represent a different visual object than the
 * "small prisms" used for SL blocks in colorblind mode.
 * 
 * Supports two types of prisms: a standard prism and a "pulsing" one.
 * 
 * Each prism is independently established.  To allow different QOs to join,
 * and to prevent overly complex representation and draw code, prisms have
 * a "master" QOrientation which determines the color for it, and a set
 * of "subordinate" QOrientations which freely join and become part of the
 * prism.
 * 
 * For example, standard Quantro uses two different prisms:
 * 
 * ST_INACTIVE: a standard prism which joins ST, ST_INACTIVE.
 * ST: a pulsing prism which only includes ST.
 * 
 * These prisms are both drawn together.  Further, lingering effects allow
 * the ST prism to "fade out" as a result of metamorphosis.
 * 
 * @author Jake
 *
 */
public class DepthComponentPrism extends DepthComponent {

	
	/**
	 * 
	 * @author Jake
	 *
	 */
	public static class BlockfieldMetadata extends DrawComponent.BlockfieldMetadata {
		
		private byte [][][][][] mPrismCorners ;
		
		protected BlockfieldMetadata( 
				int blockfieldRows, int blockfieldCols, int numPrisms ) {
			
			super(  ) ;
			
			mPrismCorners = new byte[numPrisms][2][NUM_INDEX_CORNERS][blockfieldRows][blockfieldCols] ;
		}
		
		protected byte [][][][] getCorners( int prismNum ) {
			return mPrismCorners[prismNum] ;
		}
	}
	
	
	public enum PrismType {
		/**
		 * A "Standard" prism is drawn with extruded shadows and walls
		 * at a constant alpha.  Standard prisms include extruded walls,
		 * optionally extruded shadows, and top-bottom fills (which are
		 * NOT shadowed).
		 */
		STANDARD,
		
		/**
		 * As "standard," except the top / bottom layers are left completely
		 * empty.  Probably they will be drawn by some other component, such
		 * as a FillComponent.
		 */
		STANDARD_FRAME,
		
		/**
		 * As "Standard," with the exception that rather than use a constant
		 * alpha, the alpha is "pulsed" between two alphas based on current
		 * unpaused time.
		 * 
		 * Pulses are between 0 and some nonzero value.
		 */
		PULSE,
		
		/**
		 * As "Pulse," except the top / bottom layers are left completely empty.
		 * 
		 * Pulses are between 0 and some nonzero value.
		 */
		PULSE_FRAME,
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONFIGURATION: colors, regions, descriptions of how things join together
	
	private ColorScheme mColorScheme ;
	
	// Prisms!  Each prisms is its own unique thing.  Prisms are defined as
	// a "master" qOrientation which determines color and size and such,
	// and a set of qOrientations which are "included" in the prism.  Thus
	// we can define a "ST_INACTIVE" prism (e.g.) which covers both ST and
	// ST_INACTIVE.
	
	// Prisms have identical construction (one blockfield will have a single
	// interpretation as prisms regardless of its field type), but might have
	// different behavior based on FieldType, including alpha values, shadows,
	// pulsing, etc.
	private int [] mPrismBaseQOrientation ;
	private boolean [][] mPrismIncludedQOrientation ;
	
	private int [][] mPrismAlpha_byFieldType ;
	private PrismType [][] mPrismType_byFieldType ;
	
	// useful for encoding.
	private boolean [][][] mPrismQOrientationConnects ;
	private EncodeBehavior [][] mPrismEncodeBehavior ;
	
	private Bitmap [] mPrismWallShadow ;
	private float [] mPrismWallShadowRadius ;
	private int [][] mPrismWallShadowAlpha_byFieldType ;
	// shadow bitmaps are rendered for exact-fit within the Bitmap,
	// so we don't need explicit bounds.
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	public DepthComponentPrism( ColorScheme colorScheme ) {
		if ( colorScheme == null )
			throw new NullPointerException("You gave a null colorscheme.") ;
		
		mColorScheme = colorScheme ;
	}
	
	
	public void clearPrisms( ) {
		mPrismBaseQOrientation = null ;
		mPrismIncludedQOrientation = null ;
		
		mPrismAlpha_byFieldType = null ;
		mPrismType_byFieldType = null ;
		
		mPrismQOrientationConnects = null ;
		mPrismEncodeBehavior = null ;
		
		mPrismWallShadowRadius = null ;
		mPrismWallShadowAlpha_byFieldType = null ;
	}
	
	
	public int addPrism( int baseQOrientation, PrismType prismType, int alpha ) {
		// premake some structures
		boolean [] qoIncluded = new boolean[QOrientations.NUM] ;
		for ( int qo = 0 ; qo < QOrientations.NUM; qo++ ) {
			qoIncluded[qo] = qo == baseQOrientation ;
		}
		boolean [][] qoConnects = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			for ( int qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
				qoConnects[qo][qo2] = qo == baseQOrientation && qo2 == baseQOrientation ;
			}
		}
		EncodeBehavior [] qoEncode = new EncodeBehavior[QOrientations.NUM] ;
		for ( int qo = 0 ; qo < QOrientations.NUM; qo++ ) {
			qoEncode[qo] = qo == baseQOrientation ? EncodeBehavior.STANDARD : EncodeBehavior.SKIP ;
		}
		
		
		mPrismBaseQOrientation = extend( mPrismBaseQOrientation, baseQOrientation ) ;
		mPrismIncludedQOrientation = extend( mPrismIncludedQOrientation, qoIncluded ) ;
		mPrismAlpha_byFieldType = extend( mPrismAlpha_byFieldType, new int[]{ alpha, alpha, alpha } ) ;
		mPrismType_byFieldType = extend( mPrismType_byFieldType, new PrismType[]{ prismType, prismType, prismType } ) ;
		mPrismQOrientationConnects = extend( mPrismQOrientationConnects, qoConnects ) ;
		mPrismEncodeBehavior = extend( mPrismEncodeBehavior, qoEncode ) ;
		mPrismWallShadowRadius = extend( mPrismWallShadowRadius, 0 ) ;
		mPrismWallShadowAlpha_byFieldType = extend( mPrismWallShadowAlpha_byFieldType, new int[]{ 0, 0, 0 } ) ;
		
		// return the prism number.
		return mPrismBaseQOrientation.length - 1 ;
	}
	
	private int [] extend( int [] ar, int val ) {
		int [] ar2 ;
		if ( ar == null ) {
			ar2 = new int[1] ;
		} else {
			ar2 = new int[ar.length+1] ;
			for ( int i = 0; i < ar.length; i++ ) {
				ar2[i] = ar[i] ;
			}
		}
		
		ar2[ar2.length -1] = val ;
		return ar2 ;
	}
	
	private float [] extend( float [] ar, float val ) {
		float [] ar2 ;
		if ( ar == null ) {
			ar2 = new float[1] ;
		} else {
			ar2 = new float[ar.length+1] ;
			for ( int i = 0; i < ar.length; i++ ) {
				ar2[i] = ar[i] ;
			}
		}
		
		ar2[ar2.length -1] = val ;
		return ar2 ;
	}
	
	private boolean [][] extend( boolean [][] ar, boolean [] val ) {
		boolean [][] ar2 ;
		if ( ar == null ) {
			ar2 = new boolean[1][] ;
		} else {
			ar2 = new boolean[ar.length+1][] ;
			for ( int i = 0; i < ar.length; i++ ) {
				ar2[i] = ar[i] ;
			}
		}
		
		ar2[ar2.length -1] = val ;
		return ar2 ;
	}
	
	private int [][] extend( int [][] ar, int [] val ) {
		int [][] ar2 ;
		if ( ar == null ) {
			ar2 = new int[1][] ;
		} else {
			ar2 = new int[ar.length+1][] ;
			for ( int i = 0; i < ar.length; i++ ) {
				ar2[i] = ar[i] ;
			}
		}
		
		ar2[ar2.length -1] = val ;
		return ar2 ;
	}
	
	private PrismType [][] extend( PrismType [][] ar, PrismType [] val ) {
		PrismType [][] ar2 ;
		if ( ar == null ) {
			ar2 = new PrismType[1][] ;
		} else {
			ar2 = new PrismType[ar.length+1][] ;
			for ( int i = 0; i < ar.length; i++ ) {
				ar2[i] = ar[i] ;
			}
		}
		
		ar2[ar2.length -1] = val ;
		return ar2 ;
	}
	
	private boolean [][][] extend( boolean [][][] ar, boolean [][] val ) {
		boolean [][][] ar2 ;
		if ( ar == null ) {
			ar2 = new boolean[1][][] ;
		} else {
			ar2 = new boolean[ar.length+1][][] ;
			for ( int i = 0; i < ar.length; i++ ) {
				ar2[i] = ar[i] ;
			}
		}
		
		ar2[ar2.length -1] = val ;
		return ar2 ;
	}
	
	private EncodeBehavior [][] extend( EncodeBehavior [][] ar, EncodeBehavior [] val ) {
		EncodeBehavior [][] ar2 ;
		if ( ar == null ) {
			ar2 = new EncodeBehavior[1][] ;
		} else {
			ar2 = new EncodeBehavior[ar.length+1][] ;
			for ( int i = 0; i < ar.length; i++ ) {
				ar2[i] = ar[i] ;
			}
		}
		
		ar2[ar2.length -1] = val ;
		return ar2 ;
	}
	

	public void addQOrientationToPrism( int prismNumber, int qo ) {
		boolean [] included = this.mPrismIncludedQOrientation[prismNumber] ;
		boolean [][] connects = this.mPrismQOrientationConnects[prismNumber] ;
		
		included[qo] = true ;
		// all included QOs connect.
		for ( int qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
			if ( included[qo2] ) {
				connects[qo][qo2] = connects[qo2][qo] = true ;
			}
		}
	}
	
	public void setPrismAlpha( int prismNumber, FieldType fieldType, int alpha ) {
		mPrismAlpha_byFieldType[prismNumber][fieldType.ordinal()] = alpha ;
	}
	
	public void setPrismAlpha( int prismNumber, int fieldAlpha, int pieceAlpha, int ghostAlpha ) {
		setPrismAlpha( prismNumber, FieldType.FIELD, fieldAlpha ) ;
		setPrismAlpha( prismNumber, FieldType.PIECE, pieceAlpha ) ;
		setPrismAlpha( prismNumber, FieldType.GHOST, ghostAlpha ) ;
	}
	
	public void setPrismType( int prismNumber, FieldType fieldType, PrismType prismType ) {
		mPrismType_byFieldType[prismNumber][fieldType.ordinal()] = prismType ;
	}
	
	public void setPrismType( int prismNumber, PrismType fieldPrismType, PrismType piecePrismType, PrismType ghostPrismType ) {
		setPrismType( prismNumber, FieldType.FIELD, fieldPrismType ) ;
		setPrismType( prismNumber, FieldType.PIECE, piecePrismType ) ;
		setPrismType( prismNumber, FieldType.GHOST, ghostPrismType ) ;
	}
	
	
	public void setPrismWallShadow( int prismNumber, float radius, int fieldAlpha, int pieceAlpha, int ghostAlpha ) {
		mPrismWallShadowRadius[prismNumber] = radius ;
		
		mPrismWallShadowAlpha_byFieldType[prismNumber][FieldType.FIELD.ordinal()] = fieldAlpha ;
		mPrismWallShadowAlpha_byFieldType[prismNumber][FieldType.PIECE.ordinal()] = pieceAlpha ;
		mPrismWallShadowAlpha_byFieldType[prismNumber][FieldType.GHOST.ordinal()] = ghostAlpha ;
	}
	
	
	@Override
	public void prepare( Context context,
			BlockDrawerPreallocatedBitmaps preallocated,
			Bitmap scratch,
			Hashtable<BlockSize, Regions> regions,
			int imageSize, int loadSize ) throws IOException {
		
		super.prepare(context, preallocated, scratch, regions, imageSize, loadSize) ;
		
		// the only preparation we need is to load bitmaps, and then
		// only if they have alphas > 0.  
		Hashtable<String, Bitmap> renderedBitmaps = new Hashtable<String, Bitmap>() ;
		
		mPrismWallShadow = new Bitmap[mPrismBaseQOrientation.length] ;
		
		// find the maximum dimensions among the regions.
		int blockWidth = 0, blockHeight = 0 ;
		Enumeration<BlockSize> enumeration = regions.keys() ;
		for ( ; enumeration.hasMoreElements() ; ) {
			BlockSize bs = enumeration.nextElement() ;
			blockWidth = Math.max( blockWidth, bs.getBlockWidth() ) ;
			blockHeight = Math.max( blockHeight, bs.getBlockHeight() ) ;
		}
		
		
		for ( int prismNum = 0; prismNum < mPrismWallShadow.length; prismNum++ ) {
			boolean hasPositiveAlpha = false ;
			for ( int i = 0; i < mPrismWallShadowAlpha_byFieldType[prismNum].length; i++ ) {
				if ( mPrismWallShadowAlpha_byFieldType[prismNum][i] > 0 ) {
					hasPositiveAlpha = true ;
				}
			}
			
			if ( hasPositiveAlpha ) {
				StringBuilder sb = new StringBuilder() ;
				sb.append("DepthComponentPrism_PrismWallShadow") ;
				sb.append("_").append(blockWidth) ;
				sb.append("_").append(blockHeight) ;
				sb.append("_").append(mPrismWallShadowRadius[prismNum]) ;
				String key = sb.toString() ;
				
				if ( !renderedBitmaps.containsKey(key) ) {
					// render and include.
					boolean rendered = preallocated.hasBlockSizeBitmapInUse(key)
							|| (preallocated.hasBlockSizeBitmap(key)
									&& preallocated.isRenderOK() );
					
					Bitmap b = preallocated.getBlockSizeBitmap(key, blockWidth, blockHeight, Bitmap.Config.ARGB_8888) ;
					if ( b == null ) {
						b = Bitmap.createBitmap(blockWidth, blockHeight, Bitmap.Config.ARGB_8888) ;
					}
					
					if ( !rendered ) {
						Canvas canvas = new Canvas(b) ;
						Rect rect = new Rect( 0, 0, b.getWidth(), b.getHeight() ) ;
						Render.shadowSquare(
								context, canvas, scratch,
								loadSize, Render.SheetStyle.SHADOW,
								blockWidth, blockHeight, rect, mPrismWallShadowRadius[prismNum], 255) ;
					}
					
					renderedBitmaps.put(key, b) ;
				}
				
				if ( renderedBitmaps.containsKey(key) ) {
					mPrismWallShadow[prismNum] = renderedBitmaps.get(key) ;
				}
			}
		}
	}
	
	
	
	@Override
	public BlockfieldMetadata newBlockfieldMetadataInstance( int blockfieldRows,
			int blockfieldCols, BlockSize blockSize ) {
		
		return new BlockfieldMetadata( blockfieldRows, blockfieldCols,
				mPrismBaseQOrientation.length ) ;
		
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
		
		BlockfieldMetadata meta = (BlockfieldMetadata)blockfieldMetadata ;
		
		// examine the blockfield for prisms.  This is equivalent to
		// collecting Corners; we already have the necessary connections
		// and encoding behavior.
		for ( int prism = 0; prism < mPrismBaseQOrientation.length; prism++ ) {
			findCorners(blockField, Consts.QPANE_0, configRange,
					meta.getCorners(prism),
					mPrismQOrientationConnects[prism],
					mPrismEncodeBehavior[prism]) ;
		}
		
	}

}
