package com.peaceray.quantro.view.game.blocks.asset.skin.component.border;

import java.io.IOException;
import java.util.Hashtable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.colors.ColorOp;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;
import com.peaceray.quantro.view.game.blocks.BlockSize;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.Consts.FieldType;
import com.peaceray.quantro.view.game.blocks.asset.skin.Defs.BlockRegion;
import com.peaceray.quantro.view.game.blocks.asset.skin.component.DrawComponent;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.BlockRegions;
import com.peaceray.quantro.view.game.blocks.asset.skin.regions.Regions;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigRange;


/**
 * A BorderComponentGradient matches all existing Border draws before
 * the creation of SkinAssets.  Borders are drawn as gradients stretching
 * between corners, with small single-color squares at the corners themselves.
 * 
 * Gradient color is determined by two factors: the underlying BorderColor 
 * provided by a ColorScheme, and a "shine style" which is set on a 
 * per-qOrientation basis.
 * 
 * @author Jake
 *
 */
public class BorderComponentGradient extends BorderComponent {
	
	
	enum JoinType {
		
		/**
		 * Joins only to itself -- other blocks of the same QOrientation.
		 */
		SELF,
		
		/**
		 * Joins with nothing, not even itself.  Each block has its own
		 * self-containing border.
		 */
		SOLO,
		
		/**
		 * Joins with a customized set of blocks.
		 */
		CUSTOM,
		
		/**
		 * Joins with a customized set of blocks.  Blends its
		 * color into that of the adjoining block.
		 */
		CUSTOM_BLEND,
		
	}
	
	
	protected static final GradientDrawable.Orientation [] CORNER_COMBINATION_GRADIENT_ORIENTATION = new GradientDrawable.Orientation[CORNER_COMBINATION_DIRECTION.length] ;
	static {
		for ( int i = 0; i < CORNER_COMBINATION_DIRECTION.length; i++ ) {
			GradientDrawable.Orientation o = null ;
			switch( CORNER_COMBINATION_DIRECTION[i] ) {
			case DIRECTION_RIGHT:
				o = GradientDrawable.Orientation.LEFT_RIGHT ;
				break ;
			case DIRECTION_DOWN:
				o = GradientDrawable.Orientation.TOP_BOTTOM ;
				break ;
			case DIRECTION_LEFT:
				o = GradientDrawable.Orientation.RIGHT_LEFT ;
				break ;
			case DIRECTION_UP:
				o = GradientDrawable.Orientation.BOTTOM_TOP ;
				break ;
			}
			
			CORNER_COMBINATION_GRADIENT_ORIENTATION[i] = o ;
		}
	}
	
	
	// Most borders are drawn using these Bitmaps; either stretched, or not.
	
	/**
	 * Fixed-size bitmaps useful in drawing borders without resizing.  If non-null, 
	 * they represent exact-size representations of a border, and should be drawn 
	 * when the bitmap is available and the blocks are the same size as rendered.
	 * 
	 * This is the first-priority check: if available, this bitmap will be drawn.
	 * 
	 * Indexed by:
	 * 
	 * qPane
	 * qOrientation
	 * Corner (from)
	 * Corner (to)
	 * Direction
	 * Length (# of blocks)
	 */
	protected Bitmap [][][][][][] mBorderBitmapsUnstretchable_byQPaneQOrientationCornerCornerDirectionLength ;
	
	/**
	 * Gives the bounds of the unstretchable border pixels within the corresponding
	 * Bitmap.  This is the "source" rect for drawing the bitmap.
	 * 
	 * Indexed by:
	 * 
	 * qPane
	 * qOrientation
	 * Corner (from)
	 * Corner (to)
	 * Direction
	 * Length (# of blocks)
	 */
	protected Rect [][][][][][] mBorderBitmapsUnstretchableBounds_byQPaneQOrientationCornerCornerDirectionLength;
	
	
	/**
	 * Represents a "stretchable" bitmap that can be drawn to represent a border.  Unlike
	 * the 'unstretchable' version, this can be freely resized.
	 * 
	 * This is the second-priority check after mBorderBitmapsUnstretchable.
	 * 
	 * Indexed by:
	 * 
	 * qPane			-- the qpane in which it will be drawn
	 * qOrientation		-- the qOrientation for which this is a border
	 * Corner (from)	-- the first corner
	 * Corner (to)		-- the second corner
	 * Direction		-- the direction from 1st to 2nd corner.
	 */
	protected Bitmap [][][][][] mBorderBitmapsStretchable_byQPaneQOrientationCornerCornerDirection ;
	
	/**
	 * Gives the bounds of the border pixels within the corresponding
	 * Bitmap.  This is the "source" rect for drawing the bitmap.
	 * 
	 * Indexed by:
	 * 
	 * qPane			-- the qpane in which it will be drawn
	 * qOrientation		-- the qOrientation for which this is a border
	 * Corner (from)	-- the first corner
	 * Corner (to)		-- the second corner
	 * Direction		-- the direction from 1st to 2nd corner.
	 */
	protected Rect [][][][][] mBorderBitmapsStretchableBounds_byQPaneQOrientationCornerCornerDirection ;
	
	
	// Sometimes we can't do this, and revert to Drawables.
	// The full 'drawable' including shine, if present.
	
	/**
	 * A Resizeable GradientDrawable showing the border, including a 'baked-in' shine.
	 * 
	 * This is the third-priority draw, after mBorderBitmapsUnstretchable
	 * and mBorderBitmapsStretchable.
	 * 
	 * Indexed by:
	 * 
	 * qPane			-- the qpane in which it will be drawn
	 * qOrientation		-- the qOrientation for which this is a border
	 * Corner (from)	-- the first corner
	 * Corner (to)		-- the second corner
	 * Direction		-- the direction from 1st to 2nd corner.
	 */
	protected Drawable [][][][][] mBorderDrawable_byQPaneQOrientationCornerCornerDirection ;
	
	// Lastly, we sometimes need to customize these things by, e.g., projecting a
	// semi-transparent "shine" over an established color or gradient (for blends).
	
	/**
	 * A Resizeable GradientDrawable representing the Shine applied over a given
	 * border.  Unlike most other draw-objects in this class, indexed by ShineStyle,
	 * not QPane / QOrientation.  Check which style is to be used and select the
	 * right one.
	 * 
	 * This is part of the fourth and fifth-priority draw.  When no baked-together
	 * version is available (as a bitmap -- stretchable or not -- or a Drawable),
	 * we draw the underlying color (possibly as a friendly blend) and then place
	 * the shine gradient over it.
	 * 
	 * Indexed by:
	 * 
	 * qPane			-- the border shine style
	 * qOrientation		-- the qOrientation for which this is a border
	 * Corner (from)	-- the first corner
	 * Corner (to)		-- the second corner
	 * Direction		-- the direction from 1st to 2nd corner.
	 */
	protected Drawable [][][][][] mBorderShine_byQPaneQOrientationCornerCornerDirection ;
	
	/**
	 * A Resizeable GradientDrawable representing a blend between two qOrientations.
	 * The blend from the first QOrientation to the second proceeds in the specified
	 * direction.
	 * 
	 * Indexed by:
	 * 
	 * qPane			-- the qpane in which it will be drawn
	 * QOrientation (from) 	-- the blend from this...
	 * QOrientation (to)	-- ... to this.
	 * Direction		-- the direction from 1st to 2nd qOrientation.
	 */
	protected Drawable [][][][] mBorderColorBlend_byQPaneQOrientationFromToDirection ;
	
	
	
	public static BorderComponentGradient newRetroStandardBorders( BlockRegions blockRegions, ColorScheme colorScheme, boolean useBitmaps, boolean includeShines ) {
		
		BorderComponentGradient bc = new BorderComponentGradient( blockRegions, colorScheme, Consts.QO_INCLUDED_RETRO, useBitmaps, includeShines ) ;
		bc.setRetroBorderJoinTypes( blockRegions ) ;
		
		return bc ;
	}
	
	public static BorderComponentGradient newQuantroStandardBorders( BlockRegions blockRegions, ColorScheme colorScheme, boolean useBitmaps, boolean includeShines ) {
		
		BorderComponentGradient bc = new BorderComponentGradient( blockRegions, colorScheme, Consts.QO_INCLUDED_RETRO, useBitmaps, includeShines ) ;
		bc.setQuantroBorderJoinTypes( blockRegions ) ;
		
		return bc ;
	}
	
	
	private void setRetroBorderJoinTypes( BlockRegions blockRegions ) {
		// joins.  Default to SELF for any Retro QO which is full-sized;
		// if not full-sized, use EMPTY.
		// Those QOs which are not used in Retro are set to 'null' join.
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			setJoinType(
					qo,
					Consts.QO_INCLUDED_RETRO[qo]
						? blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL
								? JoinType.SELF
								: JoinType.SOLO
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
	
	
	private void setQuantroBorderJoinTypes( BlockRegions blockRegions ) {
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
								: JoinType.SOLO
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
				QOrientations.SL,
				QOrientations.ST,
				QOrientations.ST_INACTIVE
		} ;
		
		int [] customBlendQOs = new int[] {
				QOrientations.S0_FROM_SL,
				QOrientations.S1_FROM_SL,
		} ;
		
		// step 1: set to custom / custom blend
		for ( int i = 0; i < customQOs.length; i++ ) {
			int qo = customQOs[i] ;
			if ( blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL )
				setJoinType(qo, JoinType.CUSTOM) ;
		}
		for ( int i = 0; i < customBlendQOs.length; i++ ) {
			int qo = customBlendQOs[i] ;
			if ( blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL )
				setJoinType(qo, JoinType.CUSTOM_BLEND) ;
		}
		
		
		// step 2: join them up.
		if ( getJoinType(QOrientations.SL) == JoinType.CUSTOM ) {
			if ( getJoinType(QOrientations.S0_FROM_SL) == JoinType.CUSTOM
					|| getJoinType( QOrientations.S0_FROM_SL ) == JoinType.CUSTOM_BLEND )
				setCustomJoin(QOrientations.SL, QOrientations.S0_FROM_SL, true) ;
			if ( getJoinType(QOrientations.S1_FROM_SL) == JoinType.CUSTOM
					|| getJoinType( QOrientations.S1_FROM_SL ) == JoinType.CUSTOM_BLEND )
				setCustomJoin(QOrientations.SL, QOrientations.S1_FROM_SL, true) ;
		}
		
		if ( getJoinType(QOrientations.ST) == JoinType.CUSTOM
				&& getJoinType(QOrientations.ST_INACTIVE) == JoinType.CUSTOM ) {
			setCustomJoin(QOrientations.ST, QOrientations.ST_INACTIVE, true) ;
		}
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONFIGURATION: colors, regions, descriptions of how things join together
	
	private ColorScheme mColorScheme ;

	private boolean [] mQOrientationIncluded ;
	
	private EncodeBehavior [] mQOrientationEncodeBehavior ;
	private JoinType [] mQOrientationJoinType ;
	private boolean [][] mQOrientationJoins ;
	private BorderStyle mNonoverlappingBorderStyle ;
	
	private boolean mRenderBitmaps ;
	private boolean mIncludeShines ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	public BorderComponentGradient( BlockRegions blockRegions, ColorScheme colorScheme, boolean [] qoIncluded, boolean useBitmaps, boolean includeShines ) {
		
		mColorScheme = colorScheme ;
		
		mQOrientationIncluded = qoIncluded ;
		
		mRenderBitmaps = useBitmaps ;
		mIncludeShines = includeShines ;
		
		// set default skips.
		mQOrientationEncodeBehavior = new EncodeBehavior[QOrientations.NUM] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			mQOrientationEncodeBehavior[qo] = qoIncluded[qo] ? EncodeBehavior.STANDARD : EncodeBehavior.SKIP ;
		}
		// skip NO by default.
		mQOrientationEncodeBehavior[QOrientations.NO] = EncodeBehavior.SKIP ;
		
		
		// join types.  If we SKIP, the behavior is 'null' meaning no border
		// is drawn.  Otherwise, it is SELF (for FULL blocks), SOLO (for other sizes).
		mQOrientationJoinType = new JoinType[QOrientations.NUM] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			if ( mQOrientationEncodeBehavior[qo] == EncodeBehavior.STANDARD ) {
				mQOrientationJoinType[qo] = blockRegions.getBlockRegion(0, qo) == BlockRegion.FULL
						? JoinType.SELF
						: JoinType.SOLO ;
			} else {
				mQOrientationJoinType[qo] = null ;
			}
		}
		
		// set up JOIN based on join types.  Filled with falses, but self-joins if
		// type is SELF.
		mQOrientationJoins = new boolean[QOrientations.NUM][QOrientations.NUM] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			for ( int qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
				if ( qo == qo2 ) {
					mQOrientationJoins[qo][qo2] = mQOrientationJoinType[qo] == JoinType.SELF ;
				} else {
					mQOrientationJoins[qo][qo2] = false ;
				}
			}
		}
		
		mNonoverlappingBorderStyle = BorderStyle.OVER_HORIZONTAL ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// CONFIGURATION:
	// Allows customization beyond the default settings.
	
	public void setJoinType( int qo, JoinType joinType ) {
		if ( joinType == null ) {
			mQOrientationJoinType[qo] = null ;
			mQOrientationEncodeBehavior[qo] = EncodeBehavior.SKIP ;
			for ( int qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
				mQOrientationJoins[qo][qo2] = false ;
				mQOrientationJoins[qo2][qo] = false ;
			}
			
			return ;
		}
		
		mQOrientationJoinType[qo] = joinType ;
		mQOrientationEncodeBehavior[qo] = EncodeBehavior.STANDARD ;
		
		switch( joinType ) {
		case SELF:
		case CUSTOM:
		case CUSTOM_BLEND:
			// custom joins start out equivalent to "self" joins.
			for ( int qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
				mQOrientationJoins[qo][qo2] = qo == qo2 ;
				mQOrientationJoins[qo2][qo] = qo == qo2 ;
			}
			break ;

		case SOLO:
			for ( int qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
				mQOrientationJoins[qo][qo2] = false ;
				mQOrientationJoins[qo2][qo] = false ;
			}
			break ;
		}
	}
	
	
	public JoinType getJoinType( int qOrientation ) {
		return mQOrientationJoinType[qOrientation] ;
	}
	
	/**
	 * Sets whether qo1 and qo2 allow "custom joins" with each other.
	 * 
	 * PRECONDITION: Both qo1 and qo2 must be set to CUSTOM_* join types.
	 * 
	 * POSTCONDITION: These qorientations will join with each other (or not).
	 * 
	 * @param qo1
	 * @param qo2
	 * @param join
	 */
	public void setCustomJoin( int qo1, int qo2, boolean join ) {
		JoinType joinType1, joinType2 ;
		joinType1 = getJoinType(qo1) ;
		joinType2 = getJoinType(qo2) ;
		
		if ( ( joinType1 != JoinType.CUSTOM && joinType1 != JoinType.CUSTOM_BLEND )
				|| ( joinType2 != JoinType.CUSTOM && joinType2 != JoinType.CUSTOM_BLEND ) )
			throw new IllegalStateException("qo1 and qo2 must both have JoinType CUSTOM or CUSTOM_BLEND") ;
		
		mQOrientationJoins[qo1][qo2] = join ;
		mQOrientationJoins[qo2][qo1] = join ;
	}
	
	
	public void setNonoverlappingBorderStyle( BorderStyle style ) {
		mNonoverlappingBorderStyle = style ;
	}
	
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// ABSTRACT METHOD IMPLEMENTATIONS

	@Override
	public void prepare( 
			Context context,
			BlockDrawerPreallocatedBitmaps preallocated,
			Bitmap scratch,
			Hashtable<BlockSize, Regions> regions,
			int imageSize, int loadSize ) throws IOException {
		// makes all the necessary structures.  You might be surprised to learn
		// that this might be (almost) nothing at all!  We build things up bit-by-bit
		// as needed.
		
		// First, if we have any CUSTOM_BLEND joins and they actually join with
		// anything (other than itself) we create blend gradients for them.
		boolean hasFriendlyBlend = false ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			if ( mQOrientationJoinType[qo] == JoinType.CUSTOM_BLEND ) {
				for ( int qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
					if ( mQOrientationJoins[qo][qo2] && qo != qo2 ) {
						hasFriendlyBlend = true ;
					}
				}
			}
		}
		if ( hasFriendlyBlend )
			prepareBorderColorBlends() ;
		
		// Second, if we have shines, make them and the shine-applied drawables.
		if ( mIncludeShines ) {
			prepareShines() ;
			prepareBorderDrawables() ;
		}
		
		// Third, if we have shines and bitmaps, prepare the bitmaps.  Otherwise
		// (even if we have bitmaps) they are not needed and solid colors are fine.
		if ( mIncludeShines && mRenderBitmaps ) {
			prepareBitmaps() ;
		}
	}
	
	
	/**
	 * Prepares mBorderColorBlend_byQPaneQOrientationFromToDirection with blended border
	 * colors from every CUSTOM_BLEND qOrientation to all those with which it joins.
	 */
	private void prepareBorderColorBlends() {
		mBorderColorBlend_byQPaneQOrientationFromToDirection = new Drawable[2][QOrientations.NUM][][] ;
		
		for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
			if ( mQOrientationJoinType[qo] == JoinType.CUSTOM_BLEND ) {
				for ( int qp = 0; qp < 2; qp++ ) {
					int color = mColorScheme.getBorderColor(qo, qp) ;
					
					Drawable [][] blends = new Drawable[QOrientations.NUM][NUM_CONNECTED_DIRECTIONS] ;
					mBorderColorBlend_byQPaneQOrientationFromToDirection[qp][qo] = blends ;
					for ( byte qo2 = 0; qo2 < QOrientations.NUM; qo2++ ) {
						if ( mQOrientationJoins[qo][qo2] ) {
							// join from qo to qo2.
							int color2 = mColorScheme.getBorderColor(qo2, qp) ;
							
							int [] colors = new int[] { color, color2 } ;
							blends[qo2][DIRECTION_LEFT] = new GradientDrawable( GradientDrawable.Orientation.RIGHT_LEFT, colors ) ;
							blends[qo2][DIRECTION_RIGHT] = new GradientDrawable( GradientDrawable.Orientation.LEFT_RIGHT, colors ) ;
							blends[qo2][DIRECTION_UP] = new GradientDrawable( GradientDrawable.Orientation.BOTTOM_TOP, colors ) ;
							blends[qo2][DIRECTION_DOWN] = new GradientDrawable( GradientDrawable.Orientation.TOP_BOTTOM, colors ) ;
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * Prepares mBorderShine_byQPaneQOrientationCornerCornerDirection with gradients
	 * based on corners, directions, and color scheme.
	 */
	private void prepareShines() {
		mBorderShine_byQPaneQOrientationCornerCornerDirection = new Drawable[2][QOrientations.NUM][][][] ;
		
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
				if ( mQOrientationIncluded[qo] && mQOrientationEncodeBehavior[qo] != EncodeBehavior.SKIP ) {
					Drawable [][][] shines = new Drawable[NUM_CORNERS][NUM_CORNERS][NUM_CONNECTED_DIRECTIONS] ;
					mBorderShine_byQPaneQOrientationCornerCornerDirection[qp][qo] = shines ;
					
					for ( int cIndex = 0; cIndex < CORNER_COMBINATIONS.length; cIndex++ ) {
						int corner0 = CORNER_COMBINATIONS[cIndex][0] ;
						int corner1 = CORNER_COMBINATIONS[cIndex][1] ;
						
						int direction = CORNER_COMBINATION_DIRECTION[cIndex] ;
						GradientDrawable.Orientation o = CORNER_COMBINATION_GRADIENT_ORIENTATION[cIndex] ;
						
						// get the colors at these corners.  This value includes the alpha.
						int color0 = getShineColor( qp, qo, corner0 ) ;
						int color1 = getShineColor( qp, qo, corner1 ) ;
						
						int [] colors = new int[] { color0, color1 } ;
						shines[corner0][corner1][direction] = new GradientDrawable( o, colors ) ;
					}
				}
			}
		}
	}
	
	
	/**
	 * Determines and returns the shine to be applied to this qOrientation at the specified
	 * corner. 
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @param corner
	 * @return
	 */
	private int getShineColor( int qPane, byte qOrientation, int corner ) {
		// TODO: implement this.
		throw new IllegalStateException("This method not yet implemented.") ;
	}
	
	
	/**
	 * Prepares mBorderDrawable_byQPaneQOrientationCornerCornerDirection by filling it with 
	 * GradientDrawables.  These drawables are determined by layering the appropriate shine
	 * color over the qOrientation color.
	 * 
	 * Unlike prepareShines, this will create self-reference drawables for the corners
	 * themselves.  Although instantiated as GradientDrawables, they are equivalent to
	 * ColorDrawables (with the exception that they do not share the bounds-bug).
	 * 
	 */
	private void prepareBorderDrawables() {
		mBorderDrawable_byQPaneQOrientationCornerCornerDirection = new Drawable[2][QOrientations.NUM][][][] ;
		
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
				if ( mQOrientationIncluded[qo] && mQOrientationEncodeBehavior[qo] != EncodeBehavior.SKIP ) {
					Drawable [][][] borders = new Drawable[NUM_CORNERS][NUM_CORNERS][NUM_CONNECTED_DIRECTIONS] ;
					mBorderDrawable_byQPaneQOrientationCornerCornerDirection[qp][qo] = borders ;
					
					int color = mColorScheme.getBorderColor(qo, qp) ;

					for ( int corner = 0; corner < NUM_CORNERS; corner++ ) {
						int shine = getShineColor( qp, qo, corner ) ;
						int composite = ColorOp.composeAOverB(shine, color) ;
						Drawable drawable = new GradientDrawable( GradientDrawable.Orientation.LEFT_RIGHT, new int[] { composite, composite } ) ;
						for ( int d = 0 ; d < NUM_CONNECTED_DIRECTIONS; d++ ) {
							borders[corner][corner][d] = drawable ;
						}
					}
					
					for ( int cIndex = 0; cIndex < CORNER_COMBINATIONS.length; cIndex++ ) {
						int corner0 = CORNER_COMBINATIONS[cIndex][0] ;
						int corner1 = CORNER_COMBINATIONS[cIndex][1] ;
						
						int direction = CORNER_COMBINATION_DIRECTION[cIndex] ;
						GradientDrawable.Orientation o = CORNER_COMBINATION_GRADIENT_ORIENTATION[cIndex] ;
						
						// get the colors at these corners.  This value includes the alpha.
						int shine0 = getShineColor( qp, qo, corner0 ) ;
						int shine1 = getShineColor( qp, qo, corner1 ) ;
						
						int composite0 = ColorOp.composeAOverB(shine0, color) ;
						int composite1 = ColorOp.composeAOverB(shine1, color) ;
						
						
						int [] colors = new int[] { composite0, composite1 } ;
						borders[corner0][corner1][direction] = new GradientDrawable( o, colors ) ;
					}
				}
			}
		}
	}
	
	
	/**
	 * Prepares mBorderBitmapsUnstretchable_byQPaneQOrientationCornerCornerDirectionLength
	 * and mBorderBitmapsStretchable_byQPaneQOrientationCornerCornerDirection, and their
	 * associated Bounds arrays.
	 * 
	 * Bitmaps represent the same data as GradientDrawables.  They are used primarily because
	 * drawing Bitmaps is slightly faster.  However, note that BorderBitmaps are unnecessary
	 * and take up useable memory.  Therefore, we limit the number of QOrientations which 
	 * get Bitmap representation.
	 * 
	 * UnstretchableBitmaps match exactly the borders as they would be drawn.
	 * 
	 */
	private void prepareBitmaps() {
		
		// A few limiting factors: first, we make certain assumptions about which
		// QCombinations are likely to appear often enough that we benefit from
		// Bitmap versions.  We also limit the NUMBER of total Bitmaps that we are
		// allowed to generate.  We generate only those qorientations that appear
		// in our "whitelist" and are used in this game, and only until we reach "saturation."
		
		// TODO: We require support for preallocated bitmaps before we allow this.
		
	}

	
	@Override
	public BlockfieldMetadata newBlockfieldMetadataInstance(int blockfieldRows,
			int blockfieldCols, BlockSize blockSize ) {
		
		// We don't need any special info.
		return new BorderComponent.BlockfieldMetadata(blockfieldRows, blockfieldCols) ;
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

		// collect corners.
		this.findCorners( blockField, Consts.QPANE_ALL, configRange,
				meta.getCorners(), mQOrientationJoins, mQOrientationEncodeBehavior ) ;
		
	}
	
	//
	////////////////////////////////////////////////////////////////////////////

}
