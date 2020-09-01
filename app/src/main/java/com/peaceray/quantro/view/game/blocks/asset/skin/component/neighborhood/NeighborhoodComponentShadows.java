package com.peaceray.quantro.view.game.blocks.asset.skin.component.neighborhood;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.Consts.FieldType;
import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.Component;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.Render;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.BlockRegions;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.FillRegions;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;

public class NeighborhoodComponentShadows extends NeighborhoodComponent {
	
	
	/**
	 * BlockfieldMetadata: we store in the Metadata preconstructed "regions" showing
	 * the shadowed areas for different custom blocks and standard sheets.  Unlike the
	 * normal sheet bitmap, which can be easily sized to fit any block size config.,
	 * custom block bitmaps are a different story -- they basically need to be created from
	 * scratch for each size, because they must be matched very carefully to the dimensions
	 * (and, potentially, q-offset) or the shadow edges will not match the block edges when
	 * resized.
	 * 
	 * @author Jake
	 */
	public static class BlockfieldMetadata extends NeighborhoodComponent.BlockfieldMetadata {
		
		// BlockfieldMetadata includes Regions indicating the "low-fi" version of these
		// shadows.  They are drawn as black lines rather than alpha'd bitmaps.
		private Region [] mShadowRegion ;
		private Region [] mShadowCustomRegion ;
		
		protected BlockfieldMetadata( 
				int blockfieldRows, int blockfieldCols, int numEncodingSets,
				Region [] shadowRegion, Region [] shadowCustomRegion ) {
			
			super( blockfieldRows, blockfieldCols, numEncodingSets ) ;
			
			mShadowRegion = shadowRegion ;
			mShadowCustomRegion = shadowCustomRegion ;
		}
		
		protected Region [] getShadowRegions() {
			return mShadowRegion ;
		}
		
		protected Region [] getShadowCustomRegions() {
			return mShadowCustomRegion ;
		}
		
	}
	
	
	enum JoinType {
		
		/**
		 * Joins only to itself -- other blocks of the same QOrientation.
		 */
		SELF,
		
		/**
		 * Joins with nothing, not even itself.  Each block gets its
		 * own inner shadow of the four block boundaries.
		 */
		SOLO,
		
		/**
		 * Joins with a customized set of blocks.
		 */
		CUSTOM,
		
		/**
		 * No joins.  No encoding.  We can still have a custom shadow.
		 */
		EMPTY
		
	}
	
	
	
	private ColorScheme mColorScheme ;

	private boolean [] mQOrientationIncluded ;
	
	// shadow sheet is universal
	private Bitmap mShadowSheet ;
	private Rect mShadowSheetBounds ;
	private int mShadowSheetBlockWidth ;
	private int mShadowSheetBlockHeight ;
	// custom shadows are by-BlockSize
	private Hashtable<BlockSize, Bitmap []> mShadowCustom ;
	private Hashtable<BlockSize, Rect []> mShadowCustomBounds ;
	// shadow regions, included the sheet-substitution, are by-BlockSize.
	private Hashtable<BlockSize, Region []> mShadowRegion ;
	private Hashtable<BlockSize, Region []> mShadowCustomRegion ;
	
	private Render.ShadowDirection mShadowDirection ;
	private Render.SheetStyle [] mShadowStyle ;
	private float [][] mShadowOffset ;
	private float [] mShadowRadius ;
	private int [] mShadowAlpha ;
	private BorderStyle mNonoverlappingBorderStyle ;
	
	private JoinType [] mShadowJoinType ;
	private int [] mShadowColor ;
	private int [][] mQOShadowColorIndex ;
	private boolean [][] mEncodingCountNeighbor ;
	private EncodeBehavior [] mEncodingBehavior ;
	
	
	
	/**
	 * Constructs and returns a new NeighborhoodComponentShadows instance which is
	 * configured to draw standard Retro inner shadows.  This is a convenience method:
	 * further configuration can be performed as needed.
	 * 
	 * This method is equivalent to constructing a new instance and then making the
	 * appropriate calls to setShadows, setJoinType, and setCustomJoin.
	 * 
	 * NOTE: If you are implementing custom behavior, or in some way require careful
	 * control over shadow settings, you should probably start with a clean slate by
	 * using the Constructor rather than a factory method.
	 * 
	 * SECOND NOTE: the returned Component has NOT been rendered, to allow further
	 * 		configuration if needed.
	 * 
	 * @param fillRegions
	 * @param colorScheme
	 * @return
	 */
	public static NeighborhoodComponentShadows newRetroStandardInnerShadows( BlockRegions blockRegions, ColorScheme colorScheme ) {
		
		NeighborhoodComponentShadows nc = new NeighborhoodComponentShadows( Render.ShadowDirection.IN, blockRegions, colorScheme, Consts.QO_INCLUDED_RETRO ) ;
		nc.setRetroInnerShadowJoinTypes( blockRegions ) ;

		// shadow config.
		nc.clearShadows() ;
		nc.addShadow( Render.SheetStyle.SHADOW_OVERSIZED, 0, 	0, 		0.3f, 	50) ;
		nc.addShadow( Render.SheetStyle.SHADOW_OVERSIZED, 0.2f, 0.25f, 	0.5f, 	102) ;
		
		return nc ;
	}
	
	
	/**
	 * Constructs and returns a new NeighborhoodComponentShadows instance which is
	 * configured to draw NE0N Retro inner shadows.  This is a convenience method:
	 * further configuration can be performed as needed.
	 * 
	 * This method is equivalent to constructing a new instance and then making the
	 * appropriate calls to setShadows, setJoinType, and setCustomJoin.
	 * 
	 * NOTE: If you are implementing custom behavior, or in some way require careful
	 * control over shadow settings, you should probably start with a clean slate by
	 * using the Constructor rather than a factory method.
	 * 
	 * SECOND NOTE: the returned Component has NOT been rendered, to allow further
	 * 		configuration if needed.
	 * 
	 * @param fillRegions
	 * @param colorScheme
	 * @return
	 */
	public static NeighborhoodComponentShadows newRetroNeonInnerShadows( BlockRegions blockRegions, ColorScheme colorScheme ) {
		
		NeighborhoodComponentShadows nc = new NeighborhoodComponentShadows( Render.ShadowDirection.IN, blockRegions, colorScheme, Consts.QO_INCLUDED_RETRO ) ;
		nc.setRetroInnerShadowJoinTypes( blockRegions ) ;
		
		// shadow config.
		nc.clearShadows() ;
		nc.addShadow( Render.SheetStyle.SHADOW_OVERSIZED, 0, 	0, 		0.3f, 	255) ;
		nc.addShadow( Render.SheetStyle.SHADOW_OVERSIZED, 0, 	0, 		0.3f, 	255) ;
		nc.addShadow( Render.SheetStyle.SHADOW_OVERSIZED, 0.2f, 0.25f, 	0.5f, 	102) ;
		
		return nc ;
	}
	
	
	public static NeighborhoodComponentShadows newQuantroStandardInnerShadows( BlockRegions blockRegions, ColorScheme colorScheme ) {
		NeighborhoodComponentShadows nc = new NeighborhoodComponentShadows( Render.ShadowDirection.IN, blockRegions, colorScheme, Consts.QO_INCLUDED_QUANTRO ) ;
		nc.setQuantroInnerShadowJoinTypes( blockRegions ) ;

		// shadow config.
		nc.clearShadows() ;
		nc.addShadow( Render.SheetStyle.SHADOW_OVERSIZED, 0, 	0, 			0.3f, 	127) ;
		nc.addShadow( Render.SheetStyle.SHADOW_OVERSIZED, -0.15f, -0.15f,	0.5f, 	170) ;
		
		return nc ;
	}
	
	
	public static NeighborhoodComponentShadows newRetroStandardOuterShadows( BlockRegions blockRegions, ColorScheme colorScheme ) {
		
		NeighborhoodComponentShadows nc = new NeighborhoodComponentShadows( Render.ShadowDirection.OUT, blockRegions, colorScheme, Consts.QO_INCLUDED_RETRO ) ;
		nc.setRetroOuterShadowJoinTypes( blockRegions ) ;

		// shadow config.
		nc.clearShadows() ;
		nc.addShadow( Render.SheetStyle.SHADOW, 0, 	0, 			0.1f, 	255) ;
		nc.addShadow( Render.SheetStyle.SHADOW, 0.05f, 0.05f, 	0.1f, 	255) ;
		
		return nc ;
	}
	
	
	public static NeighborhoodComponentShadows newRetroNeonOuterShadows( BlockRegions blockRegions, ColorScheme colorScheme ) {
		
		NeighborhoodComponentShadows nc = new NeighborhoodComponentShadows( Render.ShadowDirection.OUT, blockRegions, colorScheme, Consts.QO_INCLUDED_RETRO ) ;
		nc.setRetroOuterShadowJoinTypes( blockRegions ) ;

		// shadow config.
		nc.clearShadows() ;
		nc.addShadow( Render.SheetStyle.SHADOW, 0, 	0, 	0.3f, 	255) ;
		nc.addShadow( Render.SheetStyle.SHADOW, 0, 	0, 	0.3f, 	255) ;
		nc.addShadow( Render.SheetStyle.SHADOW, 0, 	0, 	0.3f, 	128) ;
		
		return nc ;
	}
	
	public static NeighborhoodComponentShadows newQuantroStandardOuterShadows( BlockRegions blockRegions, ColorScheme colorScheme ) {
		
		NeighborhoodComponentShadows nc = new NeighborhoodComponentShadows( Render.ShadowDirection.OUT, blockRegions, colorScheme, Consts.QO_INCLUDED_QUANTRO ) ;
		nc.setQuantroOuterShadowJoinTypes( blockRegions ) ;

		// shadow config.
		nc.clearShadows() ;
		nc.addShadow( Render.SheetStyle.SHADOW, 0, 	0, 			0.1f, 	255) ;
		nc.addShadow( Render.SheetStyle.SHADOW, 0.05f, 0.05f, 	0.1f, 	255) ;
		
		return nc ;
	}
	
	
	private void setRetroInnerShadowJoinTypes( BlockRegions blockRegions ) {
		// joins.  Default to SELF for any Retro QO which is full-sized;
		// if not full-sized, use EMPTY.
		// Those QOs which are not used in Retro are set to 'null' join.
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			setJoinType(
					qo,
					Consts.QO_INCLUDED_RETRO[qo]
						? blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL
								? JoinType.SELF
								: JoinType.EMPTY
						: null) ;
		}
		
		// However, certain block types -- PUSH blocks -- do NOT join, even
		// if full.  They are SOLO.
		
		int [] pushQOs = new int[] {
				QOrientations.PUSH_DOWN,
				QOrientations.PUSH_DOWN_ACTIVE,
				QOrientations.PUSH_UP,
				QOrientations.PUSH_UP_ACTIVE
		} ;
		
		for ( int i = 0; i < pushQOs.length; i++ ) {
			int qo = pushQOs[i] ;
			if ( getJoinType(qo) == JoinType.SELF )
				setJoinType(qo, JoinType.SOLO) ;
		}
	}
	
	
	private void setQuantroInnerShadowJoinTypes( BlockRegions blockRegions ) {
		// joins.  More complex than RETRO.  *Most* join with SELF.
		// However, a few have customized joins, such as S0_FROM_SL
		// and ST / ST_INACTIVE.
		
		// Default to SELF for any QUANTRO QO which is full-sized;
		// if not full-sized, use EMPTY.
		// Those QOs which are not used in Retro are set to 'null' join.
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			setJoinType(
					qo,
					Consts.QO_INCLUDED_QUANTRO[qo]
						? blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL
								? JoinType.SELF
								: JoinType.EMPTY
						: null) ;
		}
		
		// HOWEVER, certain block types should remain disconnected, no matter what.
		// They SOLO or EMPTY.  They never connect.  Not ever.
		int [] disconnectedQOs = new int[] {
				QOrientations.F0,
				QOrientations.F1,
				QOrientations.U0,
				QOrientations.U1,
				QOrientations.UL,
		} ;
		
		for ( int i = 0; i < disconnectedQOs.length; i++ ) {
			int qo = disconnectedQOs[i] ;
			if ( blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL )
				setJoinType(qo, JoinType.SOLO) ;
		}
		
		
		// NEXT: certain block types merge together if possible.  Set them
		// all to CUSTOM (if possible) and then join them up where allowed.
		// Note that setting to CUSTOM will default to SELF connections, so
		// it is a safe set-up call even if we don't end up joining them.
		int [] customQOs = new int[] {
				QOrientations.S0_FROM_SL,
				QOrientations.S1_FROM_SL,
				QOrientations.SL,
				QOrientations.ST,
				QOrientations.ST_INACTIVE
		} ;
		
		// step 1: set to custom
		for ( int i = 0; i < customQOs.length; i++ ) {
			int qo = customQOs[i] ;
			if ( blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL )
				setJoinType(qo, JoinType.CUSTOM) ;
		}
		
		// step 2: join them up.
		if ( getJoinType(QOrientations.SL) == JoinType.CUSTOM ) {
			if ( getJoinType(QOrientations.S0_FROM_SL) == JoinType.CUSTOM )
				setCustomJoin(QOrientations.SL, QOrientations.S0_FROM_SL, true) ;
			if ( getJoinType(QOrientations.S1_FROM_SL) == JoinType.CUSTOM )
				setCustomJoin(QOrientations.SL, QOrientations.S1_FROM_SL, true) ;
		}
		
		if ( getJoinType(QOrientations.ST) == JoinType.CUSTOM
				&& getJoinType(QOrientations.ST_INACTIVE) == JoinType.CUSTOM ) {
			setCustomJoin(QOrientations.ST, QOrientations.ST_INACTIVE, true) ;
		}
	}
	
	
	private void setRetroOuterShadowJoinTypes( BlockRegions blockRegions ) {
		setOuterShadowJoinTypes( blockRegions, Consts.QO_INCLUDED_RETRO ) ;
	}
	
	private void setQuantroOuterShadowJoinTypes( BlockRegions blockRegions ) {
		setOuterShadowJoinTypes( blockRegions, Consts.QO_INCLUDED_QUANTRO ) ;
	}
	
	
	
	private void setOuterShadowJoinTypes( BlockRegions blockRegions, boolean [] qoIncluded ) {
		// The joins necessary to create Outer Shadows.  Unlike Inner Shadows,
		// Outer Shadows represent the projection of one block into the area of
		// another block.  For that reason, we use a slightly non-intuitive
		// join policy.  All FULL BlockRegions are assumed to fill the block space,
		// and thus have 'null' joins, representing no shadows being displayed.
		// All other BlockRegions types join with each other, and with NO.
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			if ( qoIncluded[qo] ) {
				BlockRegion blockRegion = blockRegions.getBlockRegion(0, qo) ;
				if ( blockRegion == BlockRegion.FULL ) {
					setJoinType(qo, null) ;
				} else {
					setJoinType(qo, JoinType.CUSTOM) ;
				}
			}
		}
		
		// now combine all CUSTOM policies.
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			if ( getJoinType(qo) != JoinType.CUSTOM )
				continue ;
			for ( int qo2 = qo+1; qo2 < QOrientations.NUM; qo2++ ) {
				if ( getJoinType(qo2) == JoinType.CUSTOM ) {
					setCustomJoin(qo, qo2, true) ;
				}
			}
		}
	}
	
	
	
	public NeighborhoodComponentShadows( Render.ShadowDirection shadowDirection, BlockRegions blockRegions, ColorScheme colorScheme, boolean [] qoIncluded ) {
		
		// Color Scheme and included QOrientations.
		mShadowDirection = shadowDirection ;
		mColorScheme = colorScheme ;
		mQOrientationIncluded = qoIncluded.clone() ;
		
		// Standard join types?  We create a temporary FillRegions object to determine
		// our default JoinType.
		mShadowJoinType = new JoinType[QOrientations.NUM] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			if ( blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL )
				mShadowJoinType[qo] = JoinType.SELF ;
			else
				mShadowJoinType[qo] = JoinType.EMPTY ;
		}
		
		// Count and set the shadow colors.
		mShadowColor = new int[0] ;
		mQOShadowColorIndex = new int[2][QOrientations.NUM] ;
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
				if ( qoIncluded[qo] ) {
					int color = mColorScheme.getFillShadowColor(qo, qp) ;
					int index = -1 ;
					for ( int c = 0; c < mShadowColor.length; c++ ) {
						if ( mShadowColor[c] == color ) {
							index = c ;
							break ;
						}
					}
					
					if ( index == -1 ) {
						// add this color!
						int [] colors = new int[mShadowColor.length + 1] ;
						for ( int i = 0; i < mShadowColor.length; i++ ) {
							colors[i] = mShadowColor[i] ;
						}
						colors[mShadowColor.length] = color ;
						index = colors.length -1 ;
					}
					
					mQOShadowColorIndex[qp][qo] = index ;
				}
			}
		}
		
		// Standard interactions and behavior.
		mEncodingCountNeighbor = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		// Default: count everything except the same QO.
		for ( int qo0 = 0; qo0 < QOrientations.NUM; qo0++ ) {
			for ( int qo1 = 0; qo1 < QOrientations.NUM; qo1++ ) {
				mEncodingCountNeighbor[qo0][qo1] = qo0 != qo1 ;
			}
		}
		
		// Only count those which have standard-size inner shadows.
		mEncodingBehavior = new EncodeBehavior[QOrientations.NUM] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			mEncodingBehavior[qo] =
				( mShadowJoinType[qo] == JoinType.SELF )
						? EncodeBehavior.STANDARD
						: EncodeBehavior.SKIP ;
		}
		
		mNonoverlappingBorderStyle = null ;
	}
	
	
	// setting join behavior.
	
	/**
	 * Sets the Join Type to use for this QOrientation.  Handles inner-configuration
	 * for the setting.
	 * 
	 * Allowed values:
	 * 
	 * SELF:
	 * 		will join only with itself (the exact same qOrientation)
	 * 
	 * SOLO:
	 * 		joins with nothing (each block gets its own standard square-shaped shadow)
	 * 
	 * CUSTOM:
	 * 		after this call, behavior will be identical to SELF, but further
	 * 		customization is allowed to connect (optionally) with other CUSTOM blocks.
	 * 
	 * EMPTY:
	 * 		will connect with nothing, and is skipped in the encoding step.
	 * 		Custom shadow images (such as inset shadows) are allowed.
	 * 
	 * null:
	 * 		As EMPTY, except that custom shadows will also be omitted.
	 */
	public void setJoinType( int qOrientation, JoinType joinType ) {
		
		if ( joinType == null ) {
			mShadowJoinType[qOrientation] = null ;
			mEncodingBehavior[qOrientation] = EncodeBehavior.SKIP ;
			// count this as a neighbor to all.
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				mEncodingCountNeighbor[qo][qOrientation] = true ;
			}
			return ;
		}
		
		mShadowJoinType[qOrientation] = joinType ;
		
		// additional effects based on joinType.
		switch( joinType ) {
		case SELF:
			// joins only with itself.
			mEncodingBehavior[qOrientation] = EncodeBehavior.STANDARD ;
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				mEncodingCountNeighbor[qo][qOrientation] = qo != qOrientation ;
				mEncodingCountNeighbor[qOrientation][qo] = qo != qOrientation ;
			}
			break ;

		case SOLO:
			// joins with nothing; not even itself.
			mEncodingBehavior[qOrientation] = EncodeBehavior.STANDARD ;
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				mEncodingCountNeighbor[qo][qOrientation] = true ;
				mEncodingCountNeighbor[qOrientation][qo] = true ;
			}
			break ;
		
		case CUSTOM:
			// joins with itself by default, but allows additional customization.
			mEncodingBehavior[qOrientation] = EncodeBehavior.STANDARD ;
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				mEncodingCountNeighbor[qo][qOrientation] = qo != qOrientation ;
				mEncodingCountNeighbor[qOrientation][qo] = qo != qOrientation ;
			}
			break ;
		
		case EMPTY:
			// skip this.  No joins with anything.
			mEncodingBehavior[qOrientation] = EncodeBehavior.SKIP ;
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				mEncodingCountNeighbor[qo][qOrientation] = true ;
				mEncodingCountNeighbor[qOrientation][qo] = true ;
			}
			break ;
		}
	}
	
	
	/**
	 * Returns the join type set for the provided qOrientation.
	 * @param qOrientation
	 * @return
	 */
	public JoinType getJoinType( int qOrientation ) {
		return mShadowJoinType[qOrientation] ;
	}
	
	
	
	/**
	 * Sets whether qo1 and qo2, which both must have JoinType CUSTOM,
	 * will join with each other.  This operation is symmetric.
	 * 
	 * @param qo1
	 * @param qo2
	 * @param doesJoin
	 */
	public void setCustomJoin( int qo1, int qo2, boolean doesJoin ) {
		if ( getJoinType(qo1) != JoinType.CUSTOM || getJoinType(qo2) != JoinType.CUSTOM )
			throw new IllegalStateException("qo1 and qo2 must both have JoinType CUSTOM") ;
		
		mEncodingCountNeighbor[qo1][qo2] = !doesJoin ;
		mEncodingCountNeighbor[qo2][qo1] = !doesJoin ;
	}
	
	
	
	public void setNonoverlappingBorderStyle( BorderStyle style ) {
		mNonoverlappingBorderStyle = style ;
	}
	
	
	public void clearShadows() {
		mShadowStyle = null ;
		mShadowOffset = null ;
		mShadowRadius = null ;
		mShadowAlpha = null ;
	}
	
	
	public void addShadow( Render.SheetStyle sheetStyle, float xOffset, float yOffset, float gaussianRadius, int alpha ) {
		mShadowStyle = append( mShadowStyle, sheetStyle ) ;
		mShadowOffset = append( mShadowOffset, xOffset, yOffset ) ;
		mShadowRadius = append( mShadowRadius, gaussianRadius ) ;
		mShadowAlpha = append( mShadowAlpha, alpha ) ;
	} 
	
	/**
	 * Returns an array equivalent to 'styles' with 'style' appended.
	 * 
	 * The array returned will be a new instance.
	 * 
	 * @param vals
	 * @param val
	 * @return
	 */
	private static Render.SheetStyle [] append( Render.SheetStyle [] styles, Render.SheetStyle style ) {
		Render.SheetStyle [] ar2 ;
		if ( styles == null )
			ar2 = new Render.SheetStyle[1] ;
		else {
			ar2 = new Render.SheetStyle[styles.length] ;
			for ( int i = 0; i < styles.length; i++ )
				ar2[i] = styles[i] ;
		}
		
		ar2[ar2.length-1] = style ;
		return ar2 ;
	}
	
	
	/**
	 * Returns an array equivalent to 'vals' with 'val' appended.
	 * 
	 * The array returned will be a new instance.
	 * 
	 * @param vals
	 * @param val
	 * @return
	 */
	private static float [] append( float [] vals, float val ) {
		float [] ar2 ;
		if ( vals == null )
			ar2 = new float[1] ;
		else {
			ar2 = new float[vals.length] ;
			for ( int i = 0; i < vals.length; i++ )
				ar2[i] = vals[i] ;
		}
			
		ar2[ar2.length-1] = val ;
		return ar2 ;
	}
	
	
	/**
	 * Returns an array equivalent to 'vals' with 'val' appended.
	 * 
	 * The array returned will be a new instance.
	 * 
	 * @param vals
	 * @param val
	 * @return
	 */
	private static int [] append( int [] vals, int val ) {
		int [] ar2 ;
		if ( vals == null )
			ar2 = new int[1] ;
		else {
			ar2 = new int[vals.length] ;
			for ( int i = 0; i < vals.length; i++ )
				ar2[i] = vals[i] ;
		}
			
		ar2[ar2.length-1] = val ;
		return ar2 ;
	}
	
	
	/**
	 * Returns an array equivalent to 'vals' with a new array containing 'val' appended.
	 * 
	 * The array returned will be a new instance.  However, each inner-array will
	 * be the reference copied from 'vals.'
	 * 
	 * Recommended usage is to replace references to the argument with the returned 
	 * value, e.g.
	 * 
	 * vals = append( vals, ... )
	 * 
	 * @param vals
	 * @param val
	 * @return
	 */
	private static float [][] append( float [][] vals, float ... val ) {
		float [][] ar2 ;
		if ( vals == null )
			ar2 = new float[1][] ;
		else {
			ar2 = new float[vals.length][] ;
			for ( int i = 0; i < vals.length; i++ )
				ar2[i] = vals[i] ;
		}
		
		ar2[ar2.length-1] = val ;
		return ar2 ;
	}
	
	
	
	public void setShadows( Render.SheetStyle [] sheetStyle, float [][] offset, float [] gaussianRadius, int [] alpha ) {
		
		if ( sheetStyle.length != offset[0].length || offset[0].length != gaussianRadius.length || gaussianRadius.length != alpha.length )
			throw new IllegalArgumentException("Length mis-match.") ;
		
		for ( int i = 0; i < sheetStyle.length; i++ ) {
			if ( sheetStyle[i] != Render.SheetStyle.SHADOW && sheetStyle[i] != Render.SheetStyle.SHADOW_OVERSIZED ) {
				throw new IllegalArgumentException("Sheet style must be one of SHADOW_* - " + sheetStyle[i] + " not supported.") ;
			}
		}
		
		// clone em
		mShadowStyle = sheetStyle.clone() ;
		mShadowOffset = new float[2][] ;
		for ( int i = 0; i < 2; i++ ) {
			mShadowOffset[i] = offset[i].clone() ;
		}
		mShadowRadius = gaussianRadius.clone() ;
		mShadowAlpha = alpha.clone() ;
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ABSTRACT CLASS METHOD IMPLEMENTATIONS
	
	/**
	 * A one-time call that loads and renders (and keeps references to) relevant content
	 * bitmaps.
	 * 
	 * @param blockWidth
	 * @param blockHeight
	 * @param imageSize
	 * @param loadImageSize
	 * @throws IOException 
	 */
	@Override
	public void prepare( Context context,
			BlockDrawerPreallocatedBitmaps preallocated,
			Bitmap scratch,
			Hashtable<BlockSize, Regions> regions,
			int imageSize, int loadSize ) throws IOException {
		
		super.prepare(context, preallocated, scratch, regions, imageSize, loadSize) ;
		
		// we prefer to create a shadow sheet based on maximum-size blocks,
		// but custom shadows that are specific to blockSize and corresponding
		// regions.
		//
		// Note that the shadowRegions (equivalent to the shadow sheet for
		// low-detail Skins) is by-BlockSize, whereas the bitmap Sheet is
		// universal.
		
		mShadowSheetBlockWidth = 0 ;
		mShadowSheetBlockHeight = 0 ;
		// Set these to the maximum among our BlockSizes.
		Enumeration<BlockSize> enumeration = regions.keys() ;
		for ( ; enumeration.hasMoreElements() ; ) {
			BlockSize bs = enumeration.nextElement() ;
			mShadowSheetBlockWidth = Math.max(mShadowSheetBlockWidth, bs.getBlockWidth()) ;
			mShadowSheetBlockHeight = Math.max(mShadowSheetBlockHeight, bs.getBlockHeight()) ;
		}

		
		// limit block size if necessary.
		double blockScaleFactor = 1 ;
		switch( imageSize ) {
		case Consts.IMAGES_SIZE_NONE:
			throw new IllegalArgumentException("This Component does not support image size NONE.") ;
		
		case Consts.IMAGES_SIZE_SMALL:
			// max 20 pixels on a side
			blockScaleFactor = Math.min( blockScaleFactor, 20.0 / mShadowSheetBlockWidth ) ;
			blockScaleFactor = Math.min( blockScaleFactor, 20.0 / mShadowSheetBlockHeight ) ;
			break ;
			
		case Consts.IMAGES_SIZE_MID:
			// max 40 pixels on a side
			blockScaleFactor = Math.min( blockScaleFactor, 40.0 / mShadowSheetBlockWidth ) ;
			blockScaleFactor = Math.min( blockScaleFactor, 40.0 / mShadowSheetBlockHeight ) ;
			break ;
			
		case Consts.IMAGES_SIZE_LARGE:
		case Consts.IMAGES_SIZE_HUGE:
			// max 80 pixels on a side
			blockScaleFactor = Math.min( blockScaleFactor, 80.0 / mShadowSheetBlockWidth ) ;
			blockScaleFactor = Math.min( blockScaleFactor, 80.0 / mShadowSheetBlockHeight ) ;
			break ;
		}
		
		// resize.
		mShadowSheetBlockWidth = (int)Math.round(mShadowSheetBlockWidth * blockScaleFactor) ;
		mShadowSheetBlockHeight = (int)Math.round(mShadowSheetBlockHeight * blockScaleFactor) ;
		
		// that might still be too large, if we have very low row/col #s.  Check
		// and resize.
		if ( mShadowSheetBlockWidth > preallocated.getBlockWidth()
				|| mShadowSheetBlockHeight > preallocated.getBlockHeight() ) {
			mShadowSheetBlockWidth = preallocated.getBlockWidth() ;
			mShadowSheetBlockHeight = preallocated.getBlockHeight() ;
		}
		mShadowSheetBounds = new Rect(0, 0, mShadowSheetBlockWidth * 16, mShadowSheetBlockHeight * 16);
		
		
		// make a key for sheet renders.  Key is determined
		// by in-sheet block dimensions, shadow style, 
		StringBuilder sb = new StringBuilder() ;
		sb.append("NeighborhoodComponentShadows_ShadowSheet") ;
		sb.append("_").append(mShadowSheetBlockWidth) ;
		sb.append("_").append(mShadowSheetBlockHeight) ;
		sb.append("_").append(mShadowDirection) ;
		for ( int i = 0; i < mShadowStyle.length; i++ ) {
			sb.append("_").append(mShadowStyle[i]) ;
			sb.append("_").append(mShadowOffset[0][i]) ;
			sb.append("_").append(mShadowOffset[1][i]) ;
			sb.append("_").append(mShadowRadius[i]) ;
			sb.append("_").append(mShadowAlpha[i]) ;
		}
		String key = sb.toString() ;
		
		boolean rendered = false ;
		if ( preallocated.hasSheetBitmapInUse(key)
				|| (preallocated.hasSheetBitmap(key) && preallocated.isRenderOK()) ) {
			rendered = true ;
			mShadowSheet = preallocated.getSheetBitmap(key) ;
		} else {
			mShadowSheet = preallocated.getSheetBitmap(key) ;
			if ( mShadowSheet == null ) {
				mShadowSheet = Bitmap.createBitmap(
						mShadowSheetBounds.width(),
						mShadowSheetBounds.height(),
						Bitmap.Config.ALPHA_8) ;
			}
		}
		
		// render the sheet.
		if ( !rendered ) {
			renderSheet( context, mShadowSheet, mShadowSheetBounds, scratch, loadSize ) ;
		}
		
		// For each unique BlockSize, render custom bitmaps for it,
		// and create custom regions.
		mShadowCustom = new Hashtable<BlockSize, Bitmap []> () ;
		mShadowCustomBounds = new Hashtable<BlockSize, Rect []> () ;
		// shadow regions, included the sheet-substitution, are by-BlockSize.
		mShadowRegion = new Hashtable<BlockSize, Region []>() ;
		mShadowCustomRegion = new Hashtable<BlockSize, Region []>() ;
		
		enumeration = regions.keys() ;
		for ( ; enumeration.hasMoreElements() ; ) {
		
			BlockSize bs = enumeration.nextElement() ;
			Regions sizeRegions = this.getPreparedRegions(bs) ;
			int blockWidth = bs.getBlockWidth() ;
			int blockHeight = bs.getBlockHeight() ;
			
			// render the custom bitmaps.
			Bitmap [] customBitmaps = new Bitmap[QOrientations.NUM] ;
			Rect [] customBitmapBounds = new Rect[QOrientations.NUM] ;
			
			// make a temporary fill regions for this.
			int numBlockRegions = BlockRegion.values().length ;
			Bitmap [] customByBlockRegion = new Bitmap[numBlockRegions] ;
			Rect [] customRectByBlockRegion = new Rect[numBlockRegions] ;
			
			// Make a key stub -- we will further configure based on insets.
			sb = new StringBuilder() ;
			sb.append("NeighborhoodComponentShadows_ShadowSquare") ;
			sb.append("_").append(blockWidth) ;
			sb.append("_").append(blockHeight) ;
			sb.append("_").append(mShadowDirection) ;
			for ( int i = 0; i < mShadowStyle.length; i++ ) {
				sb.append("_").append(mShadowStyle[i]) ;
				sb.append("_").append(mShadowOffset[0][i]) ;
				sb.append("_").append(mShadowOffset[1][i]) ;
				sb.append("_").append(mShadowRadius[i]) ;
				sb.append("_").append(mShadowAlpha[i]) ;
			}
			String key_stub = sb.toString() ;
			
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				BlockRegion blockRegion = sizeRegions.getBlockRegion(0, qo) ;
				if ( blockRegion != BlockRegion.FULL && blockRegion != BlockRegion.NONE && mQOrientationIncluded[qo] ) {
					// gets its own render.  See if we have it in a previous QO;
					// if so, use it.  If not, make and render.
					int ord = blockRegion.ordinal() ;
					if ( customByBlockRegion[ord] == null ) {
						// try to get a block-size bitmap from preallocated.  Because
						// we're not sure how the rendering is actually going to happen,
						// e.g. which fitRect we're going to use, include them both so
						// we get a unique key.
						sb = new StringBuilder() ;
						sb.append(key_stub) ;
						sb.append("_").append(sizeRegions.fitFillRect(blockRegion, 0, 0)) ;
						sb.append("_").append(sizeRegions.fitFillRectInsideBorder(blockRegion, 0, 0)) ;
						key = sb.toString() ;
						
						// retrieve and/or make, and render if needed.
						rendered = preallocated.hasBlockSizeBitmapInUse(key)
								|| (preallocated.hasBlockSizeBitmap(key) && preallocated.isRenderOK()) ;
						customByBlockRegion[ord] = preallocated.getBlockSizeBitmap(key, blockWidth, blockHeight, Bitmap.Config.ALPHA_8) ;
						if ( customByBlockRegion[ord] == null ) {
							customByBlockRegion[ord] = Bitmap.createBitmap(blockWidth, blockHeight, Bitmap.Config.ALPHA_8) ;
						}
						customRectByBlockRegion[ord] = new Rect( 0, 0, blockWidth, blockHeight ) ;
						
						// render?
						if ( !rendered ) {
							renderCustomShadowIn( context, customByBlockRegion[ord], scratch, sizeRegions.getFillRegions(), blockRegion, loadSize ) ;
						}
					}
					
					customBitmaps[qo] = customByBlockRegion[ord] ;
					customBitmapBounds[qo] = customRectByBlockRegion[ord] ;
				}
			}
			
			mShadowCustom.put(bs, customBitmaps) ;
			mShadowCustomBounds.put(bs, customRectByBlockRegion) ;
			
			
			// TODO:
			// create the custom shadow regions (substitute if we don't have
			// bitmaps)!
		}
	}
	
	
	protected void renderSheet( Context context, Bitmap sheet, Rect sheetBounds, Bitmap scratch, int loadImageSize ) throws IOException {
		int width = sheetBounds.width() / 16 ;
		int height = sheetBounds.height() / 16 ;
		
		// clear this sheet out and render the shadows within it.
		Canvas canvas = new Canvas(sheet) ;
		
		Paint clearPaint = new Paint() ;
		clearPaint.setColor(0x00000000);
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		
		canvas.drawPaint(clearPaint) ;
		
		for ( int i = 0; i < mShadowOffset[0].length; i++ ) {
			Render.shadowSheet( context, canvas, scratch,
					loadImageSize, mShadowStyle[i],
					width, height,
					mShadowOffset[0][i], mShadowOffset[1][i],
					mShadowRadius[i], mShadowAlpha[i] ) ;
		}
	}
	
	
	protected void renderCustomShadowOut( Context context, Bitmap custom, Bitmap scratch,
			FillRegions fillRegions, BlockRegion blockRegion, int qPane, int loadImageSize ) throws IOException {
		// unlike customShadowIns, which (as of now) use only and exactly
		// the shadow(s) with no offset, we include all shadows here.
		// It's also important that we clip the provided bitmap, since we
		// will be drawing a full-black square with shadow around it.
		
		Canvas canvas = new Canvas(custom) ;
		
		// clear it
		Paint clearPaint = new Paint() ;
		clearPaint.setColor(0x00000000);
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		
		canvas.drawPaint(clearPaint) ;
		
		// clip-out the center.
		Rect r = new Rect() ;
		r.set( fillRegions.fitRectInsideBorder(blockRegion, 0, 0) ) ;
		canvas.clipRect( r, Region.Op.DIFFERENCE ) ;
		
		// 
		
	}
	
	
	/**
	 * Render the custom shadow for the provided BlockRegion into the provided Bitmap.
	 * This method fills the bitmap from edge-to-edge, such that when the bitmap is
	 * drawn to exactly the block boundaries, the actual shadow pixels are where they
	 * need to be.
	 * 
	 * In other words, many of the edge pixels in 'custom' will be blank (alpha 0).
	 * 
	 * @param context
	 * @param custom
	 * @param fillRegions
	 * @param blockRegion
	 * @throws IOException 
	 */
	protected void renderCustomShadowIn( Context context, Bitmap custom, Bitmap scratch, FillRegions fillRegions, BlockRegion blockRegion, int loadImageSize ) throws IOException {
		// Edge shadow gaussian radius and shadow alpha?
		float radius = 0;
		int alpha = 0;
		Render.SheetStyle style = null ;

		for (int i = 0; i < mShadowRadius.length; i++) {
			if (mShadowOffset[0][i] == 0
					&& mShadowOffset[1][i] == 0) {
				radius = mShadowRadius[i];
				alpha = mShadowAlpha[i];
				style = mShadowStyle[i] ;
			}
		}
		
		if ( style == null )
			return ;
		
		// sanitize style / rect.  If style is OVERSIZED, we handle this by
		// rendering SHADOW using the 'inside border' fit.  If style is SHADOW,
		// we still render SHADOW, but use the 'bounds' fit.
		Rect bounds = null ;
		if ( style == Render.SheetStyle.SHADOW ) {
			bounds = fillRegions.fitRect(blockRegion, 0, 0) ;
		} else if ( style == Render.SheetStyle.SHADOW_OVERSIZED ) {
			bounds = fillRegions.fitRectInsideBorder(blockRegion, 0, 0) ;
			style = Render.SheetStyle.SHADOW ;
		}
		
		Canvas canvas = new Canvas(custom) ;
		
		Paint clearPaint = new Paint() ;
		clearPaint.setColor(0x00000000);
		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

		canvas.drawPaint(clearPaint) ;
		
		Render.shadowSquare(context, canvas, scratch, loadImageSize,
				style,
				fillRegions.getBlockWidth(),
				fillRegions.getBlockHeight(),
				bounds,
				radius, alpha) ;
	}
	
	
	
	
	
	/**
	 * 
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
	 * 
	 * Subclasses should implement their own subclasses of BlockfieldMetadata, containing
	 * what information they need.
	 * 
	 * @param blockfield
	 * @return
	 */
	@Override
	public BlockfieldMetadata newBlockfieldMetadataInstance(
			int blockfieldRows, int blockfieldCols,
			BlockSize blockSize ) {
		
		// TODO: Stopgap to remove error.
		Regions regions = getPreparedRegions(blockSize) ;
		
		// make regions for these shadows, just in case we want to
		// draw without Bitmaps.
		Region [] shadowRegions = null ;
		Region [] customRegions = null ;
		if ( mShadowSheet == null ) {
			int sW = estimateShadowWidth( blockSize ) ;
			int sH = estimateShadowHeight( blockSize ) ;
			
			int blockWidth = blockSize.getBlockWidth() ;
			int blockHeight = blockSize.getBlockHeight() ;
			
			boolean [][] tempConnected = new boolean[3][3] ;
			
			shadowRegions = new Region[256] ;
			customRegions = new Region[QOrientations.NUM] ;
			
			// construct the shadow regions...
			for ( int i = 0; i < 256; i++ ) {
				Render.sheetNeighborsByIndex(i, tempConnected, 1, 1);

				Region r = new Region();
				// include the appropriate rectangles. we try to use no more than 4.
				if (!tempConnected[0][1]) // left edge
					r.op(0, 0, sW, blockHeight, Region.Op.UNION);
				if (!tempConnected[1][0]) // top edge
					r.op(0, 0, blockWidth, sH, Region.Op.UNION);
				if (!tempConnected[2][1]) // right edge
					r.op(blockWidth - sW, 0,
							blockWidth, blockHeight, Region.Op.UNION);
				if (!tempConnected[1][2]) // bottom edge
					r.op(0, blockHeight - sH,
							blockWidth, blockHeight, Region.Op.UNION);

				// now the corners. Don't bother including if we already have
				// a side that covers them.
				if (!tempConnected[0][0]
						&& (tempConnected[0][1] && tempConnected[1][0])) // top-left
					r.op(0, 0, sW, sH, Region.Op.UNION);
				if (!tempConnected[2][0]
						&& (tempConnected[2][1] && tempConnected[1][0])) // top-right
					r.op(blockWidth - sW, 0,
							blockWidth, sH, Region.Op.UNION);
				if (!tempConnected[0][2]
						&& (tempConnected[0][1] && tempConnected[1][2])) // bottom-left
					r.op(0, blockHeight - sH, sW,
							blockHeight, Region.Op.UNION);
				if (!tempConnected[2][2]
						&& (tempConnected[2][1] && tempConnected[1][2])) // bottom-right
					r.op(blockWidth - sW, blockHeight - sH,
							blockWidth, blockHeight, Region.Op.UNION);

				shadowRegions[i] = r;
			}
			
			// now do custom...
			BlockRegion [] blockRegions = BlockRegion.values() ;
			Region [] customRegionByBlockRegion = new Region[blockRegions.length] ;
			Rect rect = new Rect() ;
			for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
				BlockRegion blockRegion = regions.getBlockRegion(0, qo) ;
				
				if ( this.mQOrientationIncluded[qo] &&
						( blockRegion != BlockRegion.FULL && blockRegion != BlockRegion.NONE ) ) {
					
					int ord = blockRegion.ordinal() ;
					
					if ( customRegionByBlockRegion[ord] == null ) {
						// construct the region.  Fairly easy -- this is necessarily a box drawn
						// around the block.  However, it might be WITHIN the block or WITHOUT,
						// extending beyond its bounds or within its border.
						Region r = new Region() ;
						if ( mShadowDirection == Render.ShadowDirection.IN ) {
							// project inward from inside border.
							rect.set( regions.fitFillRectInsideBorder(blockRegion, 0, 0) ) ;
							r.op(rect, Region.Op.REPLACE) ;
							rect.inset(sW, sH) ;
							r.op(rect, Region.Op.DIFFERENCE) ;
						} else if ( mShadowDirection == Render.ShadowDirection.OUT ) {
							rect.set( regions.fitFillRect(blockRegion, 0, 0) ) ;
							r.op(rect, Region.Op.REPLACE) ;
							rect.inset(-sW, -sH) ;
							r.op(rect, Region.Op.REVERSE_DIFFERENCE) ;
						}
						
						customRegionByBlockRegion[ord] = r ;
					}
					
					customRegions[qo] = customRegionByBlockRegion[ord] ;
				}
			}
		}
		
		return new BlockfieldMetadata(
				blockfieldRows, blockfieldCols,
				mShadowColor.length,
				shadowRegions, customRegions ) ;
	}
	
	
	private int estimateShadowWidth(BlockSize blockSize) {
		return estimateShadowSize( blockSize.getBlockWidth(), 0 ) ;
	}

	private int estimateShadowHeight(BlockSize blockSize) {
		return estimateShadowSize( blockSize.getBlockHeight(), 1 ) ;
	}
	
	private int estimateShadowSize( int blockSize, int dimensionIndex ) {
		int pixels = 1;
		for (int i = 0; i < mShadowRadius.length; i++) {
			pixels = (int)Math.max(pixels,
							Math.abs(Math.round(blockSize * mShadowOffset[i][dimensionIndex]))
							+ Math.min(blockSize / 6,
										(int) Math.round(blockSize * mShadowRadius[i])));
		}
		return pixels;
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
	 * @param configRange TODO
	 * @param blockfieldMetadata
	 */
	@Override
	public void getBlockfieldMetadata(
			byte [][][] blockField, FieldType fieldType, BlockDrawerConfigRange configRange, DrawComponent.BlockfieldMetadata blockfieldMetadata ) {
		
		BlockfieldMetadata meta = (BlockfieldMetadata) blockfieldMetadata ;

		// If we have more than one shadow color, we use the expansive
		// (and expansive) encoding method.  Otherwise we use the simple
		// version.
		if ( mShadowColor.length > 1 ) {
			for ( int qp = 0; qp < 2; qp++ ) {
				this.encodeNeighbors(blockField, qp, configRange,
						meta.getEncoding(), mEncodingCountNeighbor, mEncodingBehavior, this.mQOShadowColorIndex[qp]) ;
			}
		} else {
			encodeNeighbors(blockField, Consts.QPANE_ALL, configRange,
					meta.getEncoding(0), mEncodingCountNeighbor, mEncodingBehavior) ;
		}
		
	}
	
	
	//
	////////////////////////////////////////////////////////////////////////////
	
}
