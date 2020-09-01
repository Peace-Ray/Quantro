package com.peaceray.quantro.view.colors;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.Log;

import com.peaceray.quantro.R;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.q.QOrientations;

public class ColorScheme {
	
	private static final String TAG = "ColorScheme" ;
	
	
	private static final int [] QUANTRO_QOS = new int[] {
			QOrientations.S0,
			QOrientations.S1,
			
			// Some special blocks
			QOrientations.SL,
			QOrientations.SL_ACTIVE,
			QOrientations.SL_INACTIVE,
			QOrientations.ST,
			QOrientations.ST_INACTIVE,
			QOrientations.F0,
			QOrientations.F1,
			QOrientations.U0,
			QOrientations.U1,
			QOrientations.UL,
			
			// Even more special: blocks connected to SL.
			QOrientations.S0_FROM_SL,
			QOrientations.S1_FROM_SL,
	} ;
	
	/**
	 * Defines exactly those Quantro QOrientations that are related
	 * to SL (called "fused" in-game) that is not activated.  In Standard
	 * Quantro color scheme, these are those blocks which are colored
	 * or tinted a low-saturation grey, but NOT the glowing-white
	 * activated SL.
	 */
	private static final int [] QUANTRO_NON_ACTIVE_SL_QOS = new int[] {
		QOrientations.SL,
		QOrientations.SL_INACTIVE,
		QOrientations.S0_FROM_SL,
		QOrientations.S1_FROM_SL
	} ;
	
	
	private static final int [] RETRO_QOS = new int[] {
			QOrientations.R0,
			QOrientations.R1,
			QOrientations.R2,
			QOrientations.R3,
			QOrientations.R4,
			QOrientations.R5,
			QOrientations.R6,
			QOrientations.RAINBOW_BLAND,
			QOrientations.PUSH_DOWN,
			QOrientations.PUSH_DOWN_ACTIVE,
			QOrientations.PUSH_UP,
			QOrientations.PUSH_UP_ACTIVE
	} ;
	
	

	protected Skin.Color mColorQuantro ;
	protected Skin.Color mColorRetro ;
	
	protected int mBlockBackgroundColorQuantro ;
	protected int mBlockBackgroundColorRetro ;
	
	protected int [][] mColorQOrientationQPaneFill ;
	protected int [][] mColorQOrientationQPaneBorder ;
	protected int [][] mColorQOrientationQPaneInnerShadow ;
	protected int [][] mColorQOrientationQPaneOuterShadow ;
	protected int [][][] mColorQOrientationQPaneDetail ;
	
	protected int [][] mColorQOrientationQPaneGlow ;
	protected int [][] mColorQOrientationQPaneSimplifiedFill ;
	protected int [][] mColorQOrientationQPaneSimplifiedBorder ;
		// unlike the rest, these are special null-allowed
		// array.  If 'null', use a standard response color.
		// If non-null, give the explicit color available.
	
	// border shines...
	protected int [][][] mShineColorQOrientationQPane ;
	protected ShineShape [][][] mShineShapeQOrientationQPane ;
	protected int [][][][] mShineAlphaQOrientationQPaneMinMax ;
	
	protected int [][] mBottomGuideColor_modeQPane ;
	// row guide shines...
	protected int [][][] mBottomGuideShineColor_modeQPane ;
	protected ShineShape [][][] mBottomGuideShineShape_modeQPane ;
	protected int [][][][] mBottomGuideShineAlpha_modeQPaneMinMax ;
	
	
	/**
	 * The shape described by the shine applied to some qOrientation block.
	 * 
	 * This is intended as a partial replacement for the SHINE_STYLE constants,
	 * in that it describes whether _INVERSE appears.  It allows further customization
	 * using other shapes.
	 * 
	 * Shine shapes are described in terms of "min alpha" and "max alpha."  Individual
	 * block types (qorientations) may have the same shine color applied in the 
	 * same shape, but may differ regarding their min and max alphas.
	 * 
	 * @author Jake
	 *
	 */
	public static enum ShineShape {
		
		/**
		 * Shine wraps from the top-left corner of a block, at max saturation,
		 * to the bottom-right, at minimum saturation.
		 * 
		 * This wrap prioritizes the top-right corner over the bottom left, by
		 * a factor of about 2 to 1.
		 */
		FULL_WRAP_TOP_LEFT,
		
		
		/**
		 * Shine wraps from the top-right corner of a block, at max saturation,
		 * to the bottom-left, at minimum saturation.
		 * 
		 * This wrap prioritizes the top-left corner over the bottom right, by
		 * a factor of about 2 to 1.
		 */
		FULL_WRAP_TOP_RIGHT,
		
		
		/**
		 * Shine wraps from the bottom-left corner of a block, at max saturation,
		 * to the top-right, at minimum saturation.
		 * 
		 * This wrap prioritizes the bottom-right corner over the top-left, by
		 * a factor of about 2 to 1.
		 * 
		 * This is the "inverse" of FULL_WRAP_TOP_RIGHT.
		 */
		FULL_WRAP_BOTTOM_LEFT,
		
		
		/**
		 * Shine wraps from the bottom-right corner of a block, at max saturation,
		 * to the top-left, at minimum saturation.
		 * 
		 * This wrap prioritizes the bottom-left corner over the top-right, by
		 * a factor of about 2 to 1.
		 * 
		 * This is the "inverse" of FULL_WRAP_TOP_LEFT.
		 */
		FULL_WRAP_BOTTOM_RIGHT,

		
		
		/**
		 * Shine wraps from the top-left corner of a block, at max saturation,
		 * halfway around to the bottom-right corner.
		 * 
		 * At the top-right corner, it is 75% of the way to min saturation.  It
		 * reaches minimum saturation 2/3rds of the way down the left edge, and
		 * 1/3rd of the way down the right side.  From this point to the bottom-right
		 * edge, we have the minimum shine.
		 */
		HALF_WRAP_TOP_LEFT,
		
		
		/**
		 * Shine wraps from the top-right corner of a block, at max saturation,
		 * halfway around to the bottom-left corner.
		 * 
		 * At the top-left corner, it is 75% of the way to min saturation.  It
		 * reaches minimum saturation 2/3rds of the way down the right edge, and
		 * 1/3rd of the way down the left side.  From this point to the bottom-left
		 * edge, we have the minimum shine.
		 */
		HALF_WRAP_TOP_RIGHT,
		
		
		/**
		 * Shine wraps from the bottom-left corner of a block, at max saturation,
		 * halfway around to the top-right corner.
		 * 
		 * At the bottom-right corner, it is 75% of the way to min saturation.  It
		 * reaches minimum saturation 2/3rds of the way up the left edge, and
		 * 1/3rd of the way up the right side.  From this point to the top-right
		 * edge, we have the minimum shine.
		 * 
		 * This is the "inverse" of HALF_WRAP_TOP_RIGHT.
		 */
		HALF_WRAP_BOTTOM_LEFT,
		
		
		/**
		 * Shine wraps from the bottom-right corner of a block, at max saturation,
		 * halfway around to the top-left corner.
		 * 
		 * At the bottom-left corner, it is 75% of the way to min saturation.  It
		 * reaches minimum saturation 2/3rds of the way up the right edge, and
		 * 1/3rd of the way up the left side.  From this point to the top-left
		 * edge, we have the minimum shine.
		 * 
		 * This is the "inverse" of HALF_WRAP_TOP_LEFT.
		 */
		HALF_WRAP_BOTTOM_RIGHT,

		
		
		/**
		 * A "Cap" shine does not fade evenly on all sides.  Instead, it
		 * covers the indicated side at full opacity, and quickly drops
		 * to min opacity on the two adjacent sides: that is, less than
		 * half of the two adjacent sides is covered before we read min
		 * opacity.
		 * 
		 * This cap covers the TOP of the block; the LEFT side get slightly
		 * favorable treatment (greater distance to fade opacity).
		 */
		CAP_TOP_LEFT_LEANING,
		
		
		/**
		 * A "Cap" shine does not fade evenly on all sides.  Instead, it
		 * covers the indicated side at full opacity, and quickly drops
		 * to min opacity on the two adjacent sides: that is, less than
		 * half of the two adjacent sides is covered before we read min
		 * opacity.
		 * 
		 * This cap covers the TOP of the block; the RIGHT side get slightly
		 * favorable treatment (greater distance to fade opacity).
		 */
		CAP_TOP_RIGHT_LEANING,
		
		
		/**
		 * A "Cap" shine does not fade evenly on all sides.  Instead, it
		 * covers the indicated side at full opacity, and quickly drops
		 * to min opacity on the two adjacent sides: that is, less than
		 * half of the two adjacent sides is covered before we read min
		 * opacity.
		 * 
		 * This cap covers the BOTTOM of the block; the LEFT side get slightly
		 * favorable treatment (greater distance to fade opacity).
		 */
		CAP_BOTTOM_LEFT_LEANING,
		
		
		/**
		 * A "Cap" shine does not fade evenly on all sides.  Instead, it
		 * covers the indicated side at full opacity, and quickly drops
		 * to min opacity on the two adjacent sides: that is, less than
		 * half of the two adjacent sides is covered before we read min
		 * opacity.
		 * 
		 * This cap covers the BOTTOM of the block; the RIGHT side get slightly
		 * favorable treatment (greater distance to fade opacity).
		 */
		CAP_BOTTOM_RIGHT_LEANING,
		
		
		
		/**
		 * "Specular" shines are extreme, but small, shines.  There are
		 * three MAJOR differences between speculars and most other shine
		 * styles.
		 * 
		 * 1. Speculars have large and explicit areas of max alpha.  No other
		 * 		shine (at time of writing) uses wide patches of max alpha
		 * 		-- except CAP, which places full alpha on an entire side.
		 * 		Speculars (we expect) have about 1/5th of their main
		 * 		side at full alpha, then their alpha value fades.
		 * 
		 * 2. Speculars are based around a specific SIDE, and their behavior
		 * 		cannot be reduced to smooth transitions between corners.  For
		 * 		example, SPECULAR_LEFT puts non-min, non-max alphas at the top
		 * 		and bottom left corners, but the alphas between those two are
		 * 		NOT bounded by those two corners (as is the case for all other
		 * 		shines).
		 * 
		 * 3. Speculars do not have matching alpha for "inverse" corners,
		 * 		for example top-left-convex and bottom-right-concave (think
		 * 		about this for a moment to see how these two are matching
		 * 		opposites -- they fit snugly together).
		 * 
		 * 		Speculars do NOT impose the same coloration to these two
		 * 		states.
		 * 
		 * 
		 * Specular Left Side Low: A specular highlight on left edges.  It's
		 * maximum value is low-down on the edge.  If might spread around the bottom-left
		 * corner, but not the top-left corner.
		 */
		SPECULAR_LEFT_SIDE_LOW,
		
		
		/**
		 * "Specular" shines are extreme, but small, shines.  There are
		 * three MAJOR differences between speculars and most other shine
		 * styles.
		 * 
		 * 1. Speculars have large and explicit areas of max alpha.  No other
		 * 		shine (at time of writing) uses wide patches of max alpha
		 * 		-- except CAP, which places full alpha on an entire side.
		 * 		Speculars (we expect) have about 1/5th of their main
		 * 		side at full alpha, then their alpha value fades.
		 * 
		 * 2. Speculars are based around a specific SIDE, and their behavior
		 * 		cannot be reduced to smooth transitions between corners.  For
		 * 		example, SPECULAR_LEFT puts non-min, non-max alphas at the top
		 * 		and bottom left corners, but the alphas between those two are
		 * 		NOT bounded by those two corners (as is the case for all other
		 * 		shines).
		 * 
		 * 3. Speculars do not have matching alpha for "inverse" corners,
		 * 		for example top-left-convex and bottom-right-concave (think
		 * 		about this for a moment to see how these two are matching
		 * 		opposites -- they fit snugly together).
		 * 
		 * 		Speculars do NOT impose the same coloration to these two
		 * 		states.
		 * 
		 * 
		 * Specular Right Side High: A specular highlight on right edges.  It's
		 * maximum value is high-up on the edge.  If might spread around the top-right
		 * corner, but not the bottom-right corner.
		 */
		SPECULAR_RIGHT_SIDE_HIGH,
		
		
		/**
		 * "Specular spot" shines are extreme and small shines, even smaller
		 * than standard SPECULAR shines.  A Specular Spot does NOT extend
		 * beyond the specified side.
		 */
		SPECULAR_SPOT_TOP_SIDE_RIGHT,
		
		/**
		 * "Specular spot" shines are extreme and small shines, even smaller
		 * than standard SPECULAR shines.  A Specular Spot does NOT extend
		 * beyond the specified side.
		 */
		SPECULAR_SPOT_BOTTOM_SIDE_LEFT,
		
	}
	
	
	private static final int SHINE_INDEX_STANDARD = 0 ;
	private static final int SHINE_INDEX_BOLD = 1 ;
	private static final int SHINE_INDEX_EXTREME = 2 ;
	private static final int SHINE_INDEX_WEAK = 3 ;
	private static final int NUM_SHINE_INDICES = 4 ;
	
	/**
	 * Most shines use standard min/max alpha strengths, rather than
	 * custom and uniquely "tweaked" alphas.  This array stores the
	 * standard alpha values, indexed by SHINE_INDEX_*.
	 */
	private static int [][] SHINE_ALPHA_MIN_MAX = new int[NUM_SHINE_INDICES][] ;
	static {
		SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] 	= new int[]{ 0x00, 0x80 } ;
		SHINE_ALPHA_MIN_MAX[SHINE_INDEX_BOLD] 		= new int[]{ 0x00, 0xc0 } ;
		SHINE_ALPHA_MIN_MAX[SHINE_INDEX_EXTREME] 	= new int[]{ 0xcd, 0xe6 } ;
		SHINE_ALPHA_MIN_MAX[SHINE_INDEX_WEAK] 		= new int[]{ 0x00, 0x40 } ;
	}
	
	
	
	
	private static final int INVERSE_GREY_CHANNEL_THRESHOLD = 240 ;	// if all 3 channels are >= this, use INVERSE_GREY by default.
	
	
	private static String RESOURCE_TYPE_NAME_INTEGER = null ;
	private static String RESOURCE_TYPE_NAME_INT_ARRAY = null ;
	
	
	
	public ColorScheme( Context context, Skin.Color quantroColor, Skin.Color retroColor ) {
		
		if ( RESOURCE_TYPE_NAME_INTEGER == null ) {
			Resources res = context.getResources() ;
			RESOURCE_TYPE_NAME_INTEGER = res.getResourceTypeName(R.integer.color_scheme_resource_type) ;
			RESOURCE_TYPE_NAME_INT_ARRAY = res.getResourceTypeName(R.array.color_scheme_resource_type) ;
		}
		
		mColorQOrientationQPaneFill = new int[QOrientations.NUM][2] ;
		mColorQOrientationQPaneBorder = new int[QOrientations.NUM][2] ;
		mColorQOrientationQPaneInnerShadow = new int[QOrientations.NUM][2] ;
		mColorQOrientationQPaneOuterShadow = new int[QOrientations.NUM][2] ;
		mColorQOrientationQPaneDetail = new int [QOrientations.NUM][2][] ;
		mShineColorQOrientationQPane = new int[QOrientations.NUM][2][] ;
		mShineShapeQOrientationQPane = new ShineShape[QOrientations.NUM][2][] ;
		mShineAlphaQOrientationQPaneMinMax = new int[QOrientations.NUM][2][][] ;
		
		mBottomGuideColor_modeQPane = new int[2][2] ;
		mBottomGuideShineColor_modeQPane = new int[2][2][] ;
		mBottomGuideShineShape_modeQPane = new ShineShape[2][2][] ;
		mBottomGuideShineAlpha_modeQPaneMinMax = new int[2][2][][] ;
		
		
		setScheme( context, quantroColor, retroColor ) ;
	}
	
	public ColorScheme( Context context, ColorScheme cs ) {
		this( context, cs.mColorQuantro, cs.mColorRetro ) ;
	}
	
	public ColorScheme( Context context, Skin skin ) {
		this( context, skin.getColor(Skin.Game.QUANTRO), skin.getColor(Skin.Game.RETRO) ) ;
	}
	
	@Override
	public boolean equals( Object obj ) {
		if ( obj == null || !(obj instanceof ColorScheme) )
			return false ;
		ColorScheme cs = (ColorScheme) obj ;
		return mColorQuantro == cs.mColorQuantro && mColorRetro == cs.mColorRetro ;
	}
	
	public Skin.Color getSchemeQuantro() {
		return mColorQuantro ;
	}
	
	public Skin.Color getSchemeRetro() {
		return mColorRetro ;
	}
	
	public int getFillColor( byte qOrientation, int qPane ) {
		int c = mColorQOrientationQPaneFill[qOrientation][qPane] ;
		return Color.argb(255, Color.red(c), Color.green(c), Color.blue(c)) ;
	}
	
	public int getBorderColor( byte qOrientation, int qPane ) {
		return mColorQOrientationQPaneBorder[qOrientation][qPane] ;
	}
	
	public int getFillShadowColor( byte qOrientation, int qPane ) {
		return mColorQOrientationQPaneInnerShadow[qOrientation][qPane] ;
	}
	
	public int getDropShadowColor( byte qOrientation, int qPane ) {
		return mColorQOrientationQPaneOuterShadow[qOrientation][qPane] ;
	}
	
	public int getGlowColor( byte qOrientation, int qPane ) {
		if ( mColorQOrientationQPaneGlow != null && 
				mColorQOrientationQPaneGlow[qOrientation] != null )
			return mColorQOrientationQPaneGlow[qOrientation][qPane] ;
		
		// Otherwise, default to some other color.
		// Special cases: use 'detail' color.
		if ( qOrientation == QOrientations.PUSH_DOWN || qOrientation == QOrientations.PUSH_DOWN_ACTIVE ||
				qOrientation == QOrientations.PUSH_UP || qOrientation == QOrientations.PUSH_UP_ACTIVE ) {
			return getDetailColor(qOrientation, qPane, 0) ;
		}
		
		// Special case: use fill color, not border color.
		if ( qOrientation == QOrientations.ST || qOrientation == QOrientations.ST_INACTIVE ||
				qOrientation == QOrientations.F0 || qOrientation == QOrientations.F1 ||
				qOrientation == QOrientations.SL_ACTIVE ) {
			return getFillColor( qOrientation, qPane ) ;
		}
		
		// Otherwise, default to Border Color.
		return getBorderColor( qOrientation, qPane ) ;
	}
	
	public int getFillColorSimplified( byte qOrientation, int qPane ) {
		int c ;
		if ( mColorQOrientationQPaneSimplifiedFill != null &&
				mColorQOrientationQPaneSimplifiedFill[qOrientation] != null )
			c = mColorQOrientationQPaneSimplifiedFill[qOrientation][qPane] ;
		else
			c = mColorQOrientationQPaneFill[qOrientation][qPane] ;
		
		return Color.argb(255, Color.red(c), Color.green(c), Color.blue(c)) ;
	}
	
	public int getBorderColorSimplified( byte qOrientation, int qPane ) {
		int c ;
		if ( mColorQOrientationQPaneSimplifiedBorder != null &&
				mColorQOrientationQPaneSimplifiedBorder[qOrientation] != null )
			c = mColorQOrientationQPaneSimplifiedBorder[qOrientation][qPane] ;
		else
			c = mColorQOrientationQPaneBorder[qOrientation][qPane] ;
		
		return Color.argb(255, Color.red(c), Color.green(c), Color.blue(c)) ;
	}
	
	
	
	public int getDetailColor( byte qOrientation, int qPane, int detail ) {
		return mColorQOrientationQPaneDetail[qOrientation][qPane][detail] ;
	}
	
	public int getShineNumber( byte qOrientation, int qPane ) {
		return mShineColorQOrientationQPane[qOrientation][qPane] == null ?
				0 : mShineColorQOrientationQPane[qOrientation][qPane].length ;
	}
	
	public int getShineColor( byte qOrientation, int qPane, int shineNumber ) {
		return mShineColorQOrientationQPane[qOrientation][qPane] == null ?
				0 : mShineColorQOrientationQPane[qOrientation][qPane][shineNumber] ;
	}
	
	public ShineShape getShineShape( byte qOrientation, int qPane, int shineNumber ) {
		return mShineShapeQOrientationQPane[qOrientation][qPane] == null ?
				null : mShineShapeQOrientationQPane[qOrientation][qPane][shineNumber] ;
	}
	
	public int [] getShineAlphaRange( byte qOrientation, int qPane, int shineNumber ) {
		return mShineAlphaQOrientationQPaneMinMax[qOrientation][qPane] == null ?
				null : mShineAlphaQOrientationQPaneMinMax[qOrientation][qPane][shineNumber].clone() ;
	}
	
	public int getShineAlphaMin( byte qOrientation, int qPane, int shineNumber ) {
		return mShineAlphaQOrientationQPaneMinMax[qOrientation][qPane] == null ?
				0 : mShineAlphaQOrientationQPaneMinMax[qOrientation][qPane][shineNumber][0] ;
	}
	
	public int getShineAlphaMax( byte qOrientation, int qPane, int shineNumber ) {
		return mShineAlphaQOrientationQPaneMinMax[qOrientation][qPane] == null ?
				0 : mShineAlphaQOrientationQPaneMinMax[qOrientation][qPane][shineNumber][1] ;
	}
	
	public int getQuantroBlockBackgroundColor() {
		return mBlockBackgroundColorQuantro ;
	}
	
	public int getRetroBlockBackgroundColor() {
		return mBlockBackgroundColorRetro ;
	}
	
	public int getQuantroRowGuideColor( int qp ) {
		return this.mBottomGuideColor_modeQPane[0][qp] ;
	}
	
	public int getRetroRowGuideColor( int qp ) {
		return this.mBottomGuideColor_modeQPane[1][qp] ;
	}
	
	public int getQuantroRowGuideShineNumber( int qp ) {
		return this.mBottomGuideShineColor_modeQPane[0][qp].length ;
	}
	
	public int getQuantroRowGuideShineColor( int qp, int shineNumber ) {
		return this.mBottomGuideShineColor_modeQPane[0][qp][shineNumber] ;
	}
	
	public int getRetroRowGuideShineNumber( int qp ) {
		return this.mBottomGuideShineColor_modeQPane[1][qp].length ;
	}
	
	public int getRetroRowGuideShineColor( int qp, int shineNumber ) {
		return this.mBottomGuideShineColor_modeQPane[1][qp][shineNumber] ;
	}
	
	public ShineShape getQuantroRowGuideShineShape( int qp, int shineNumber ) {
		return this.mBottomGuideShineShape_modeQPane[0][qp][shineNumber] ;
	}
	
	public ShineShape getRetroRowGuideShineShape( int qp, int shineNumber ) {
		return this.mBottomGuideShineShape_modeQPane[1][qp][shineNumber] ;
	}
	
	public int getQuantroRowGuideShineAlphaMin( int qp, int shineNumber ) {
		return this.mBottomGuideShineAlpha_modeQPaneMinMax[0][qp][shineNumber][0] ;
	}
	
	public int getQuantroRowGuideShineAlphaMax( int qp, int shineNumber ) {
		return this.mBottomGuideShineAlpha_modeQPaneMinMax[0][qp][shineNumber][1] ;
	}
	
	public int getRetroRowGuideShineAlphaMin( int qp, int shineNumber ) {
		return this.mBottomGuideShineAlpha_modeQPaneMinMax[1][qp][shineNumber][0] ;
	}
	
	public int getRetroRowGuideShineAlphaMax( int qp, int shineNumber ) {
		return this.mBottomGuideShineAlpha_modeQPaneMinMax[1][qp][shineNumber][1] ;
	}
	
	
	public void setScheme( Context context, Skin.Color colorQuantro, Skin.Color colorRetro ) {
		Resources res = context.getResources() ;
		
		// Load the standard colors FIRST.
		if ( colorQuantro != Skin.Color.QUANTRO )
			setQuantroColors( res, Skin.Color.QUANTRO ) ;
		if ( colorRetro != Skin.Color.RETRO )
			setRetroColors( res, Skin.Color.RETRO ) ;
		
		mColorQuantro = colorQuantro ;
		mColorRetro = colorRetro;
		
		if ( colorQuantro != null )
			setQuantroColors( res, colorQuantro ) ;
		if ( colorRetro != null )
			setRetroColors( res, colorRetro ) ;
	}
	
	
	private void setQuantroColors( Resources res, Skin.Color color ) {
		
		// Holders for Shine values
		// Standard...
		int [] s0ShineColor, s1ShineColor ;
		ShineShape [] s0ShineShape, s1ShineShape ;
		int [][] s0ShineAlphaRange, s1ShineAlphaRange ;
		
		// SL shines...
		int [] sl_s0ShineColor, sl_s1ShineColor ;
		ShineShape [] sl_s0ShineShape, sl_s1ShineShape ;
		int [][] sl_s0ShineAlphaRange, sl_s1ShineAlphaRange ;
		
		int s0_color, s1_color ;
		
		int [] qos ;
		
		switch( color ) {
		case QUANTRO:
			
			// background color
			mBlockBackgroundColorQuantro = res.getColor(R.color.standard_block_background) ;
			
			// many colors are independent of qpane
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = res.getColor(R.color.standard_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = res.getColor(R.color.standard_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.standard_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.standard_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = res.getColor(R.color.standard_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.standard_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = res.getColor(R.color.standard_ul_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = res.getColor(R.color.standard_u0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = res.getColor(R.color.standard_u1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = res.getColor(R.color.standard_f0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.standard_f1_fill) ;
			}
			mColorQOrientationQPaneFill[QOrientations.SL][0] = res.getColor(R.color.standard_sl_s0_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL][1] = res.getColor(R.color.standard_sl_s1_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.standard_sl_s0_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.standard_sl_s1_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.standard_sl_active_s0_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.standard_sl_active_s1_fill) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = res.getColor(R.color.standard_s0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = res.getColor(R.color.standard_s1_border) ;
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.standard_s0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.standard_s1_border) ;
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] = res.getColor(R.color.standard_f0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.F1][qp] = res.getColor(R.color.standard_f1_border) ;
			}
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = res.getColor(R.color.standard_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = res.getColor(R.color.standard_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.standard_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.standard_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][0] = res.getColor(R.color.standard_sl_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][1] = res.getColor(R.color.standard_sl_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.standard_sl_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.standard_sl_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.standard_sl_active_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.standard_sl_active_s1_border) ;
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.standard_f0_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.standard_f0_detail_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.standard_f1_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.standard_f1_detail_stripe) ;
			}
			
			// Shadow Colors.
			int shadow = res.getColor(R.color.standard_fill_shadow) ;
			for ( int i = 0; i < QUANTRO_QOS.length; i++ ) {
				int qo = QUANTRO_QOS[i] ;
				for ( int qp = 0; qp < 2; qp++ )
					mColorQOrientationQPaneInnerShadow[qo][qp] = shadow ;
			}
			shadow = res.getColor(R.color.standard_drop_shadow) ;
			for ( int i = 0; i < QUANTRO_QOS.length; i++ ) {
				int qo = QUANTRO_QOS[i] ;
				for ( int qp = 0; qp < 2; qp++ )
					mColorQOrientationQPaneOuterShadow[qo][qp] = shadow ;
			}
			
			// Shines.
			for ( int i = 0; i < QUANTRO_QOS.length; i++ ) {
				int qo = QUANTRO_QOS[i] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					setDefaultBorderShine( qo, qp ) ;
				}
			}
			
			break ;
			
		case PROTANOPIA:
			// Set those colors which differ from STANDARD.
			// many colors are independent of qpane.
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.protonopia_s0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.protonopia_s1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = res.getColor(R.color.protonopia_s0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = res.getColor(R.color.protonopia_s1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = res.getColor(R.color.protonopia_st_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.protonopia_st_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = res.getColor(R.color.protonopia_ul_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = res.getColor(R.color.protonopia_u0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = res.getColor(R.color.protonopia_u1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = res.getColor(R.color.protonopia_f0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.protonopia_f1_fill_color) ;
			}
			mColorQOrientationQPaneFill[QOrientations.SL][0] = res.getColor(R.color.protonopia_sl_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL][1] = res.getColor(R.color.protonopia_sl_s1_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.protonopia_sl_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.protonopia_sl_s1_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.protonopia_sl_active_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.protonopia_sl_active_s1_fill_color) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.protonopia_s0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.protonopia_s1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = res.getColor(R.color.protonopia_s0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = res.getColor(R.color.protonopia_s1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] = res.getColor(R.color.protonopia_f0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.F1][qp] = res.getColor(R.color.protonopia_f1_border_color) ;
			}
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = res.getColor(R.color.protonopia_st_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = res.getColor(R.color.protonopia_st_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.protonopia_st_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.protonopia_st_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][0] = res.getColor(R.color.protonopia_sl_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][1] = res.getColor(R.color.protonopia_sl_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.protonopia_sl_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.protonopia_sl_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.protonopia_sl_active_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.protonopia_sl_active_s1_border_color) ;
			
			// detail colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.protonopia_f0_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.protonopia_f0_detail_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.protonopia_f1_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.protonopia_f1_detail_stripe) ;
			}
			
			break ;
			
		case DEUTERANOPIA:
			// many colors are independent of qpane
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = res.getColor(R.color.deuteranopia_s0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = res.getColor(R.color.deuteranopia_s1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.deuteranopia_s0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.deuteranopia_s1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = res.getColor(R.color.deuteranopia_st_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.deuteranopia_st_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = res.getColor(R.color.deuteranopia_ul_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = res.getColor(R.color.deuteranopia_u0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = res.getColor(R.color.deuteranopia_u1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = res.getColor(R.color.deuteranopia_f0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.deuteranopia_f1_fill_color) ;
			}
			mColorQOrientationQPaneFill[QOrientations.SL][0] = res.getColor(R.color.deuteranopia_sl_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL][1] = res.getColor(R.color.deuteranopia_sl_s1_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.deuteranopia_sl_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.deuteranopia_sl_s1_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.deuteranopia_sl_active_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.deuteranopia_sl_active_s1_fill_color) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = res.getColor(R.color.deuteranopia_s0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = res.getColor(R.color.deuteranopia_s1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.deuteranopia_s0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.deuteranopia_s1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] = res.getColor(R.color.deuteranopia_f0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.F1][qp] = res.getColor(R.color.deuteranopia_f1_border_color) ;
			}
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = res.getColor(R.color.deuteranopia_st_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = res.getColor(R.color.deuteranopia_st_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.deuteranopia_st_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.deuteranopia_st_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][0] = res.getColor(R.color.deuteranopia_sl_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][1] = res.getColor(R.color.deuteranopia_sl_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.deuteranopia_sl_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.deuteranopia_sl_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.deuteranopia_sl_active_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.deuteranopia_sl_active_s1_border_color) ;
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.deuteranopia_f0_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.deuteranopia_f0_detail_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.deuteranopia_f1_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.deuteranopia_f1_detail_stripe) ;
			}
			
			break ;
			
		case TRITANOPIA:
			// many colors are independent of qpane
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = res.getColor(R.color.tritanopia_s0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = res.getColor(R.color.tritanopia_s1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.tritanopia_s0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.tritanopia_s1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = res.getColor(R.color.tritanopia_st_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.tritanopia_st_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = res.getColor(R.color.tritanopia_ul_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = res.getColor(R.color.tritanopia_u0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = res.getColor(R.color.tritanopia_u1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = res.getColor(R.color.tritanopia_f0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.tritanopia_f1_fill_color) ;
			}
			mColorQOrientationQPaneFill[QOrientations.SL][0] = res.getColor(R.color.tritanopia_sl_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL][1] = res.getColor(R.color.tritanopia_sl_s1_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.tritanopia_sl_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.tritanopia_sl_s1_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.tritanopia_sl_active_s0_fill_color) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.tritanopia_sl_active_s1_fill_color) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = res.getColor(R.color.tritanopia_s0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = res.getColor(R.color.tritanopia_s1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.tritanopia_s0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.tritanopia_s1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] = res.getColor(R.color.tritanopia_f0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.F1][qp] = res.getColor(R.color.tritanopia_f1_border_color) ;
			}
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = res.getColor(R.color.tritanopia_st_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = res.getColor(R.color.tritanopia_st_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.tritanopia_st_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.tritanopia_st_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][0] = res.getColor(R.color.tritanopia_sl_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][1] = res.getColor(R.color.tritanopia_sl_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.tritanopia_sl_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.tritanopia_sl_s1_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.tritanopia_sl_active_s0_border_color) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.tritanopia_sl_active_s1_border_color) ;
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.tritanopia_f0_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.tritanopia_f0_detail_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.tritanopia_f1_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.tritanopia_f1_detail_stripe) ;
			}
			
			break ;
			
			
		case NILE:
			// many colors are independent of qpane
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = res.getColor(R.color.nile_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = res.getColor(R.color.nile_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.nile_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.nile_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = res.getColor(R.color.nile_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.nile_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = res.getColor(R.color.nile_ul_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = res.getColor(R.color.nile_u0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = res.getColor(R.color.nile_u1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = res.getColor(R.color.nile_f0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.nile_f1_fill) ;
			}
			mColorQOrientationQPaneFill[QOrientations.SL][0] = res.getColor(R.color.nile_sl_s0_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL][1] = res.getColor(R.color.nile_sl_s1_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.nile_sl_s0_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.nile_sl_s1_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.nile_sl_active_s0_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.nile_sl_active_s1_fill) ;
			
			s0_color = res.getColor(R.color.nile_s0_border) ;
			s1_color = res.getColor(R.color.nile_s1_border) ; 
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = s1_color ;
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.U0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.U1][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] = res.getColor(R.color.nile_f0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.F1][qp] = res.getColor(R.color.nile_f1_border) ;
			}
			
			mColorQOrientationQPaneBorder[QOrientations.UL][0] = s0_color ;
			mColorQOrientationQPaneBorder[QOrientations.UL][1] = s1_color ;
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = res.getColor(R.color.nile_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = res.getColor(R.color.nile_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.nile_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.nile_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][0] = res.getColor(R.color.nile_sl_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][1] = res.getColor(R.color.nile_sl_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.nile_sl_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.nile_sl_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.nile_sl_active_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.nile_sl_active_s1_border) ;
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.nile_f0_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.nile_f0_detail_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.nile_f1_detail_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.nile_f1_detail_stripe) ;
			}
			
			
			// Shadows
			int shadowSL = res.getColor(R.color.nile_sl_active_inner_shadow) ;
			this.mColorQOrientationQPaneInnerShadow[QOrientations.SL_ACTIVE][0] = shadowSL ;
			this.mColorQOrientationQPaneInnerShadow[QOrientations.SL_ACTIVE][1] = shadowSL ;
			
			
			// Shines!
			s0ShineColor = new int[] { res.getColor(R.color.nile_s0_shine) } ;
			s0ShineShape = new ShineShape[] { this.shineShapeFromResource(res, R.integer.nile_s0_shine_shape) } ;
			s0ShineAlphaRange = new int[][] { this.shineAlphaRangeFromResource(res, R.integer.nile_s0_shine_alpha) } ;
			
			s1ShineColor = new int[] { res.getColor(R.color.nile_s1_shine) } ;
			s1ShineShape = new ShineShape[] { this.shineShapeFromResource(res, R.integer.nile_s1_shine_shape) } ;
			s1ShineAlphaRange = new int[][] { this.shineAlphaRangeFromResource(res, R.integer.nile_s1_shine_alpha) } ;
			
			sl_s0ShineColor = new int[] { res.getColor(R.color.nile_sl_0_shine) } ;
			sl_s0ShineShape = new ShineShape[] { this.shineShapeFromResource(res, R.integer.nile_sl_0_shine_shape) } ;
			sl_s0ShineAlphaRange = new int[][] { this.shineAlphaRangeFromResource(res, R.integer.nile_sl_0_shine_alpha) } ;
			
			sl_s1ShineColor = new int[] { res.getColor(R.color.nile_sl_1_shine) } ;
			sl_s1ShineShape = new ShineShape[] { this.shineShapeFromResource(res, R.integer.nile_sl_1_shine_shape) } ;
			sl_s1ShineAlphaRange = new int[][] { this.shineAlphaRangeFromResource(res, R.integer.nile_sl_1_shine_alpha) } ;
			
			// Easy way to set all this crap: first set all to the default s0 / s1 shine,
			// then set the special QOrientations (SL, SX_FROM_SL, and SL_INACTIVE)
			// to sl_* values.  SL_ACTIVE gets the default shine.
			
			for ( int index = 0; index < QUANTRO_QOS.length; index++ ) {
				int qo = QUANTRO_QOS[index] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			// Special values: everything related to non-ACTIVE SL gets the
			// SL shine color.
			for ( int index = 0; index < QUANTRO_NON_ACTIVE_SL_QOS.length; index++ ) {
				int qo = QUANTRO_NON_ACTIVE_SL_QOS[index] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? sl_s0ShineColor : sl_s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? sl_s0ShineShape : sl_s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? sl_s0ShineAlphaRange : sl_s1ShineAlphaRange ;
				}
			}
			
			// Finally, SL_ACTIVE gets default settings.
			for ( int qp = 0; qp < 2; qp++ ) {
				setDefaultBorderShine(QOrientations.SL_ACTIVE, qp) ;
			}
			break ;
			
			
			
		case NATURAL:
			// many colors are independent of qpane
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = res.getColor(R.color.natural_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = res.getColor(R.color.natural_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.natural_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.natural_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = res.getColor(R.color.natural_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.natural_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = res.getColor(R.color.natural_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = res.getColor(R.color.natural_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = 
					qp == 0 ? mColorQOrientationQPaneFill[QOrientations.U0][qp] :
						mColorQOrientationQPaneFill[QOrientations.U1][qp] ;
				
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = res.getColor(R.color.natural_flash_fill) ;
				mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.natural_flash_fill) ;
			}
			mColorQOrientationQPaneFill[QOrientations.SL][0] = res.getColor(R.color.natural_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL][1] = res.getColor(R.color.natural_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.natural_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.natural_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.natural_sl_active_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.natural_sl_active_fill) ;
			
			s0_color = res.getColor(R.color.natural_s0_border) ;
			s1_color = res.getColor(R.color.natural_s1_border) ; 
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = s1_color ;
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.U0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.U1][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] = res.getColor(R.color.natural_f0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.F1][qp] = res.getColor(R.color.natural_f1_border) ;
			}
			
			mColorQOrientationQPaneBorder[QOrientations.UL][0] = s0_color ;
			mColorQOrientationQPaneBorder[QOrientations.UL][1] = s1_color ;
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = res.getColor(R.color.natural_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = res.getColor(R.color.natural_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.natural_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.natural_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][0] = res.getColor(R.color.natural_sl_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][1] = res.getColor(R.color.natural_sl_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.natural_sl_inactive_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.natural_sl_inactive_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.natural_sl_active_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.natural_sl_active_border) ;
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.natural_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.natural_flash_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.natural_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.natural_flash_stripe) ;
			}
			
			
			
			s0_color = res.getColor(R.color.natural_s0_glow) ;
			s1_color = res.getColor(R.color.natural_s1_glow) ;
			if ( mColorQOrientationQPaneGlow == null )
				mColorQOrientationQPaneGlow = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneGlow[QOrientations.S0] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S1] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S0_FROM_SL] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S1_FROM_SL] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.U0] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.U1] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.UL] = new int[] {
					mColorQOrientationQPaneGlow[QOrientations.U0][0],
					mColorQOrientationQPaneGlow[QOrientations.U1][1] } ;
			
			
			
			
			// Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.natural_s0_border_shine_0),
					res.getColor(R.color.natural_s0_border_shine_1) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.natural_s0_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.natural_s0_border_shine_1_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_1_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.natural_s1_border_shine_0),
					res.getColor(R.color.natural_s1_border_shine_1) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.natural_s1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.natural_s1_border_shine_1_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_1_alpha) } ;
			
			// Easy way to set all this crap: first set all to the default s0 / s1 shine,
			// then set the special QOrientations (SL, SX_FROM_SL, and SL_INACTIVE)
			// to sl_* values.  SL_ACTIVE gets the default shine.
			
			for ( int index = 0; index < QUANTRO_QOS.length; index++ ) {
				int qo = QUANTRO_QOS[index] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			// these shine colors are applied to everything but SL_ACTIVE.
			// However, they may get different alpha values.
			// SL:
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][0] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_0_alpha_sl),
					shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_1_alpha_sl) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][1] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_0_alpha_sl),
					shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_1_alpha_sl) } ;
			for ( int qp = 0; qp < 2; qp++ ) {
				mShineAlphaQOrientationQPaneMinMax[QOrientations.S0_FROM_SL][qp] =
					mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][qp] ;
				mShineAlphaQOrientationQPaneMinMax[QOrientations.S1_FROM_SL][qp] =
					mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][qp] ;
			}
			// SL inactive:
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_INACTIVE][0] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_0_alpha_sl_inactive),
					shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_1_alpha_sl_inactive) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_INACTIVE][1] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_0_alpha_sl_inactive),
					shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_1_alpha_sl_inactive) } ;
			// ST and ST INACTIVE
			mShineAlphaQOrientationQPaneMinMax[QOrientations.ST][0] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.ST_INACTIVE][0] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_0_alpha_st),
							shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_1_alpha_st) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.ST][1] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.ST_INACTIVE][1] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_0_alpha_st),
							shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_1_alpha_st) } ;
			// Flash: F0 and F1
			mShineAlphaQOrientationQPaneMinMax[QOrientations.F0][0] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.F1][0] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_0_alpha_flash),
							shineAlphaRangeFromResource(res, R.integer.natural_s0_border_shine_1_alpha_flash) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.F0][1] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.F1][1] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_0_alpha_flash),
							shineAlphaRangeFromResource(res, R.integer.natural_s1_border_shine_1_alpha_flash) } ;
			
			// Finally, ST_ACTIVE gets special consideration,
			// not color-coded shines.
			for ( int qp = 0; qp < 2; qp++ ) {
				mShineColorQOrientationQPane[QOrientations.SL_ACTIVE][qp] =
					new int[] {
						res.getColor(R.color.natural_sl_active_border_shine_0),
						res.getColor(R.color.natural_sl_active_border_shine_1)
					} ;
				mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_ACTIVE][qp] =
					new int[][] {
						shineAlphaRangeFromResource(res, R.integer.natural_sl_active_border_shine_0_alpha),
						shineAlphaRangeFromResource(res, R.integer.natural_sl_active_border_shine_1_alpha) } ;
			}
			
			break ;
			
			
		case ZEN:
			// many colors are independent of qpane
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = res.getColor(R.color.zen_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = res.getColor(R.color.zen_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = res.getColor(R.color.zen_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = res.getColor(R.color.zen_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = res.getColor(R.color.zen_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.zen_st_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = res.getColor(R.color.zen_s0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = res.getColor(R.color.zen_s1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = 
					qp == 0 ? mColorQOrientationQPaneFill[QOrientations.U0][qp] :
						mColorQOrientationQPaneFill[QOrientations.U1][qp] ;
				
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = res.getColor(R.color.zen_flash_fill) ;
				mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.zen_flash_fill) ;
			}
			mColorQOrientationQPaneFill[QOrientations.SL][0] = res.getColor(R.color.zen_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL][1] = res.getColor(R.color.zen_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.zen_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.zen_sl_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.zen_sl_active_fill) ;
			mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.zen_sl_active_fill) ;
			
			s0_color = res.getColor(R.color.zen_s0_border) ;
			s1_color = res.getColor(R.color.zen_s1_border) ; 
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = s1_color ;
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.U0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.U1][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] = res.getColor(R.color.zen_f0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.F1][qp] = res.getColor(R.color.zen_f1_border) ;
			}
			
			mColorQOrientationQPaneBorder[QOrientations.UL][0] = s0_color ;
			mColorQOrientationQPaneBorder[QOrientations.UL][1] = s1_color ;
			
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = res.getColor(R.color.zen_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = res.getColor(R.color.zen_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.zen_st_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.zen_st_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][0] = res.getColor(R.color.zen_sl_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL][1] = res.getColor(R.color.zen_sl_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.zen_sl_inactive_s0_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.zen_sl_inactive_s1_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][0] = res.getColor(R.color.zen_sl_active_border) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][1] = res.getColor(R.color.zen_sl_active_border) ;
			
			
			// Simplified colors
			s0_color = res.getColor(R.color.zen_s0_fill_simplified) ;
			s1_color = res.getColor(R.color.zen_s1_fill_simplified) ;
			mColorQOrientationQPaneSimplifiedFill = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneSimplifiedFill[QOrientations.S0] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S1] = 
				mColorQOrientationQPaneSimplifiedFill[QOrientations.U0] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.U1] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.UL] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S0_FROM_SL] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S1_FROM_SL] =
					new int[] { s0_color, s1_color } ;
			
			s0_color = res.getColor(R.color.zen_s0_border_simplified) ;
			s1_color = res.getColor(R.color.zen_s1_border_simplified) ;
			mColorQOrientationQPaneSimplifiedBorder = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneSimplifiedBorder[QOrientations.S0] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S1] = 
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.U0] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.U1] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.UL] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S0_FROM_SL] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S1_FROM_SL] =
					new int[] { s0_color, s1_color } ;
			
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.zen_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.zen_flash_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.zen_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.zen_flash_stripe) ;
			}
			
			// Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.zen_s0_border_shine) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.zen_s0_border_shine_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.zen_s0_border_shine_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.zen_s1_border_shine_0),
					res.getColor(R.color.zen_s1_border_shine_1) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.zen_s1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.zen_s1_border_shine_1_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_1_alpha) } ;
			
			// Easy way to set all this crap: first set all to the default s0 / s1 shine,
			// then set the special QOrientations (SL, SX_FROM_SL, and SL_INACTIVE)
			// to sl_* values.  SL_ACTIVE gets the default shine.
			
			for ( int index = 0; index < QUANTRO_QOS.length; index++ ) {
				int qo = QUANTRO_QOS[index] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			// these shine colors are applied to everything but SL_ACTIVE.
			// However, they may get different alpha values.
			// SL:
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][0] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.zen_s0_border_shine_alpha_sl) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][1] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_0_alpha_sl),
					shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_1_alpha_sl) } ;
			for ( int qp = 0; qp < 2; qp++ ) {
				mShineAlphaQOrientationQPaneMinMax[QOrientations.S0_FROM_SL][qp] =
					mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][qp] ;
				mShineAlphaQOrientationQPaneMinMax[QOrientations.S1_FROM_SL][qp] =
					mShineAlphaQOrientationQPaneMinMax[QOrientations.SL][qp] ;
			}
			// SL inactive:
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_INACTIVE][0] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.zen_s0_border_shine_alpha_sl_inactive) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_INACTIVE][1] = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_0_alpha_sl_inactive),
					shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_1_alpha_sl_inactive) } ;
			// ST and ST INACTIVE
			mShineAlphaQOrientationQPaneMinMax[QOrientations.ST][0] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.ST_INACTIVE][0] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.zen_s0_border_shine_alpha_st) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.ST][1] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.ST_INACTIVE][1] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_0_alpha_st),
							shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_1_alpha_st) } ;
			// Flash: F0 and F1
			mShineAlphaQOrientationQPaneMinMax[QOrientations.F0][0] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.F1][0] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.zen_s0_border_shine_alpha_flash) } ;
			mShineAlphaQOrientationQPaneMinMax[QOrientations.F0][1] = 
					mShineAlphaQOrientationQPaneMinMax[QOrientations.F1][1] = new int[][] {
							shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_0_alpha_flash),
							shineAlphaRangeFromResource(res, R.integer.zen_s1_border_shine_1_alpha_flash) } ;
			
			// Finally, ST_ACTIVE gets special consideration,
			// not color-coded shines.
			for ( int qp = 0; qp < 2; qp++ ) {
				mShineColorQOrientationQPane[QOrientations.SL_ACTIVE][qp] =
					new int[] {
						res.getColor(R.color.zen_sl_active_border_shine_0),
						res.getColor(R.color.zen_sl_active_border_shine_1)
					} ;
				mShineShapeQOrientationQPane[QOrientations.SL_ACTIVE][qp] =
					new ShineShape[] {
						shineShapeFromResource(res, R.integer.zen_sl_active_border_shine_0_shape),
						shineShapeFromResource(res, R.integer.zen_sl_active_border_shine_1_shape) } ;
				mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_ACTIVE][qp] =
					new int[][] {
						shineAlphaRangeFromResource(res, R.integer.zen_sl_active_border_shine_0_alpha),
						shineAlphaRangeFromResource(res, R.integer.zen_sl_active_border_shine_1_alpha) } ;
			}
			
			break ;
			
			
			
		case DECADENCE:
			// many colors are independent of qpane
			s0_color = res.getColor(R.color.decadence_s0_fill) ;
			s1_color = res.getColor(R.color.decadence_s1_fill) ;
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = s0_color ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = s1_color ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = s0_color ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = s1_color ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = s0_color ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = s1_color ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = 
					qp == 0 ? mColorQOrientationQPaneFill[QOrientations.U0][qp] :
						mColorQOrientationQPaneFill[QOrientations.U1][qp] ;
					
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = 
					mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.decadence_st_fill) ;
				
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = 
					mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.decadence_flash_fill) ;
			
				mColorQOrientationQPaneFill[QOrientations.SL][qp] = 
					mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][qp] = res.getColor(R.color.decadence_sl_fill) ;
			
				mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][qp] = res.getColor(R.color.decadence_sl_active_fill) ;
			}
			
			s0_color = res.getColor(R.color.decadence_s0_border) ;
			s1_color = res.getColor(R.color.decadence_s1_border) ;
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = s1_color ;
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.U0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.U1][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.SL][qp] = res.getColor(R.color.decadence_sl_border) ;
				mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][qp] = res.getColor(R.color.decadence_sl_active_border) ;
			}
			mColorQOrientationQPaneBorder[QOrientations.UL][0] = s0_color ;
			mColorQOrientationQPaneBorder[QOrientations.UL][1] = s1_color ;
			
			mColorQOrientationQPaneBorder[QOrientations.ST][0] = 
				mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][0] = res.getColor(R.color.decadence_st_border_q0) ;
			mColorQOrientationQPaneBorder[QOrientations.ST][1] = 
				mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][1] = res.getColor(R.color.decadence_st_border_q1) ;
			
			mColorQOrientationQPaneBorder[QOrientations.F0][0] = 
				mColorQOrientationQPaneBorder[QOrientations.F1][0] = res.getColor(R.color.decadence_flash_border_q0) ;
			mColorQOrientationQPaneBorder[QOrientations.F0][1] = 
				mColorQOrientationQPaneBorder[QOrientations.F1][1] = res.getColor(R.color.decadence_flash_border_q1) ;
			
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][0] = res.getColor(R.color.decadence_sl_inactive_border_q0) ;
			mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][1] = res.getColor(R.color.decadence_sl_inactive_border_q1) ;
			
			
			// Simplified colors
			s0_color = res.getColor(R.color.decadence_s0_fill_simplified) ;
			s1_color = res.getColor(R.color.decadence_s1_fill_simplified) ;
			mColorQOrientationQPaneSimplifiedFill = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneSimplifiedFill[QOrientations.S0] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S1] = 
				mColorQOrientationQPaneSimplifiedFill[QOrientations.U0] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.U1] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.UL] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S0_FROM_SL] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S1_FROM_SL] =
					new int[] { s0_color, s1_color } ;
			
			s0_color = res.getColor(R.color.decadence_s0_border_simplified) ;
			s1_color = res.getColor(R.color.decadence_s1_border_simplified) ;
			mColorQOrientationQPaneSimplifiedBorder = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneSimplifiedBorder[QOrientations.S0] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S1] = 
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.U0] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.U1] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.UL] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S0_FROM_SL] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S1_FROM_SL] =
					new int[] { s0_color, s1_color } ;
			
			
			
			// DETAIL: flash
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.decadence_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.decadence_flash_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.decadence_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.decadence_flash_stripe) ;
			}
			
			
			s0_color = res.getColor(R.color.decadence_s0_glow) ;
			s1_color = res.getColor(R.color.decadence_s1_glow) ;
			if ( mColorQOrientationQPaneGlow == null )
				mColorQOrientationQPaneGlow = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneGlow[QOrientations.S0] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S1] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S0_FROM_SL] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S1_FROM_SL] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.U0] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.U1] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.UL] = new int[] {
					mColorQOrientationQPaneGlow[QOrientations.U0][0],
					mColorQOrientationQPaneGlow[QOrientations.U1][1] } ;
			
			s0_color = s1_color = res.getColor(R.color.decadence_sl_active_glow) ;
			mColorQOrientationQPaneGlow[QOrientations.SL_ACTIVE] = new int[] { s0_color, s1_color } ;
			
			
			// Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.decadence_s0_border_shine_0),
					res.getColor(R.color.decadence_s0_border_shine_1),
					res.getColor(R.color.decadence_s0_border_shine_2) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.decadence_s0_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.decadence_s0_border_shine_1_shape),
					shineShapeFromResource(res, R.integer.decadence_s0_border_shine_2_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.decadence_s0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_s0_border_shine_1_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_s0_border_shine_2_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.decadence_s1_border_shine_0),
					res.getColor(R.color.decadence_s1_border_shine_1),
					res.getColor(R.color.decadence_s1_border_shine_2) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.decadence_s1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.decadence_s1_border_shine_1_shape),
					shineShapeFromResource(res, R.integer.decadence_s1_border_shine_2_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.decadence_s1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_s1_border_shine_1_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_s1_border_shine_2_alpha) } ;
			
			
			
			// Easy way to set all this crap: first set all to the default s0 / s1 shine,
			// then set the special QOrientations.  Finally set ST_ACTIVE.
			
			for ( int index = 0; index < QUANTRO_QOS.length; index++ ) {
				int qo = QUANTRO_QOS[index] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			
			// Alt Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.decadence_alt_q0_border_shine_0),
					res.getColor(R.color.decadence_alt_q0_border_shine_1) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.decadence_alt_q0_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.decadence_alt_q0_border_shine_1_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.decadence_alt_q0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_alt_q0_border_shine_1_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.decadence_alt_q1_border_shine_0),
					res.getColor(R.color.decadence_alt_q1_border_shine_1),
					res.getColor(R.color.decadence_alt_q1_border_shine_2) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.decadence_alt_q1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.decadence_alt_q1_border_shine_1_shape),
					shineShapeFromResource(res, R.integer.decadence_alt_q1_border_shine_2_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.decadence_alt_q1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_alt_q1_border_shine_1_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_alt_q1_border_shine_2_alpha) } ;
			
			
			// here are the alts.
			qos = new int[] {
					QOrientations.F0,
					QOrientations.F1,
					QOrientations.ST,
					QOrientations.ST_INACTIVE,
					QOrientations.SL,
					QOrientations.SL_INACTIVE,
					QOrientations.S0_FROM_SL,
					QOrientations.S1_FROM_SL
			} ;
			
			for ( int i = 0; i < qos.length; i++ ) {
				int qo = qos[i] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			
			// Unique shines in SL.
			s0ShineColor = new int[] {
					res.getColor(R.color.decadence_sl_active_border_shine_0),
					res.getColor(R.color.decadence_sl_active_border_shine_1),
					res.getColor(R.color.decadence_sl_active_border_shine_2) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.decadence_sl_active_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.decadence_sl_active_border_shine_1_shape),
					shineShapeFromResource(res, R.integer.decadence_sl_active_border_shine_2_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.decadence_sl_active_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_sl_active_border_shine_1_alpha),
					shineAlphaRangeFromResource(res, R.integer.decadence_sl_active_border_shine_2_alpha) } ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mShineColorQOrientationQPane[QOrientations.SL_ACTIVE][qp] = s0ShineColor ;
				mShineShapeQOrientationQPane[QOrientations.SL_ACTIVE][qp] = s0ShineShape ;
				mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_ACTIVE][qp] = s0ShineAlphaRange ;
			}
			
			break ;
			
			
		case DAWN:
			// many colors are independent of qpane
			s0_color = res.getColor(R.color.dawn_s0_fill) ;
			s1_color = res.getColor(R.color.dawn_s1_fill) ;
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.S0][qp] = s0_color ;
				mColorQOrientationQPaneFill[QOrientations.S1][qp] = s1_color ;
				mColorQOrientationQPaneFill[QOrientations.S0_FROM_SL][qp] = s0_color ;
				mColorQOrientationQPaneFill[QOrientations.S1_FROM_SL][qp] = s1_color ;
				mColorQOrientationQPaneFill[QOrientations.U0][qp] = s0_color ;
				mColorQOrientationQPaneFill[QOrientations.U1][qp] = s1_color ;
				mColorQOrientationQPaneFill[QOrientations.UL][qp] = 
					qp == 0 ? mColorQOrientationQPaneFill[QOrientations.U0][qp] :
						mColorQOrientationQPaneFill[QOrientations.U1][qp] ;
					
				mColorQOrientationQPaneFill[QOrientations.ST][qp] = 
					mColorQOrientationQPaneFill[QOrientations.ST_INACTIVE][qp] = res.getColor(R.color.dawn_st_fill) ;
				
				mColorQOrientationQPaneFill[QOrientations.F0][qp] = 
					mColorQOrientationQPaneFill[QOrientations.F1][qp] = res.getColor(R.color.dawn_flash_fill) ;
			
				mColorQOrientationQPaneFill[QOrientations.SL][qp] =
					mColorQOrientationQPaneFill[QOrientations.SL_INACTIVE][qp] = res.getColor(R.color.dawn_sl_fill) ;
				mColorQOrientationQPaneFill[QOrientations.SL_ACTIVE][qp] = res.getColor(R.color.dawn_sl_active_fill) ;
			}
			
			
			s0_color = res.getColor(R.color.dawn_s0_border) ;
			s1_color = res.getColor(R.color.dawn_s1_border) ;
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.S0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.S1][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.U0][qp] = s0_color ;
				mColorQOrientationQPaneBorder[QOrientations.U1][qp] = s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.UL][qp] = qp == 0 ? s0_color : s1_color ;
				
				mColorQOrientationQPaneBorder[QOrientations.S0_FROM_SL][qp] = res.getColor( R.color.dawn_s0_from_sl_border ) ;
				mColorQOrientationQPaneBorder[QOrientations.S1_FROM_SL][qp] = res.getColor( R.color.dawn_s1_from_sl_border ) ;
				
				mColorQOrientationQPaneBorder[QOrientations.ST][qp] =
					mColorQOrientationQPaneBorder[QOrientations.ST_INACTIVE][qp] =
						res.getColor( qp == 0 ? R.color.dawn_st_border_q0 : R.color.dawn_st_border_q1 ) ;
				
				mColorQOrientationQPaneBorder[QOrientations.F0][qp] =
					mColorQOrientationQPaneBorder[QOrientations.F1][qp] =
						res.getColor( qp == 0 ? R.color.dawn_flash_border_q0 : R.color.dawn_flash_border_q1 ) ;
				
				mColorQOrientationQPaneBorder[QOrientations.SL][qp] = res.getColor( R.color.dawn_sl_border ) ;
				mColorQOrientationQPaneBorder[QOrientations.SL_ACTIVE][qp] = res.getColor( R.color.dawn_sl_active_border ) ;
				
				mColorQOrientationQPaneBorder[QOrientations.SL_INACTIVE][qp] =
					res.getColor( qp == 0 ? R.color.dawn_sl_inactive_border_q0 : R.color.dawn_sl_inactive_border_q1 ) ;
			}
			
			
			// Simplified colors
			s0_color = res.getColor(R.color.dawn_s0_fill_simplified) ;
			s1_color = res.getColor(R.color.dawn_s1_fill_simplified) ;
			mColorQOrientationQPaneSimplifiedFill = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneSimplifiedFill[QOrientations.S0] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S1] = 
				mColorQOrientationQPaneSimplifiedFill[QOrientations.U0] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.U1] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.UL] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S0_FROM_SL] =
				mColorQOrientationQPaneSimplifiedFill[QOrientations.S1_FROM_SL] =
					new int[] { s0_color, s1_color } ;
			
			s0_color = res.getColor(R.color.dawn_s0_border_simplified) ;
			s1_color = res.getColor(R.color.dawn_s1_border_simplified) ;
			mColorQOrientationQPaneSimplifiedBorder = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneSimplifiedBorder[QOrientations.S0] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S1] = 
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.U0] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.U1] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.UL] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S0_FROM_SL] =
				mColorQOrientationQPaneSimplifiedBorder[QOrientations.S1_FROM_SL] =
					new int[] { s0_color, s1_color } ;
			
			
			
			
			
			// DETAIL: flash
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.F0][qp] = new int[2] ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp] = new int[2] ;
				
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][0] = res.getColor(R.color.dawn_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F0][qp][1] = res.getColor(R.color.dawn_flash_stripe) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][0] = res.getColor(R.color.dawn_flash_burst) ;
				mColorQOrientationQPaneDetail[QOrientations.F1][qp][1] = res.getColor(R.color.dawn_flash_stripe) ;
			}
			
			
			s0_color = res.getColor(R.color.dawn_s0_glow) ;
			s1_color = res.getColor(R.color.dawn_s1_glow) ;
			if ( mColorQOrientationQPaneGlow == null )
				mColorQOrientationQPaneGlow = new int[QOrientations.NUM][] ;
			mColorQOrientationQPaneGlow[QOrientations.S0] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S1] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S0_FROM_SL] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.S1_FROM_SL] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.U0] = new int[] { s0_color, s0_color } ;
			mColorQOrientationQPaneGlow[QOrientations.U1] = new int[] { s1_color, s1_color } ;
			mColorQOrientationQPaneGlow[QOrientations.UL] = new int[] {
					mColorQOrientationQPaneGlow[QOrientations.U0][0],
					mColorQOrientationQPaneGlow[QOrientations.U1][1] } ;
			
			s0_color = res.getColor(R.color.dawn_sl_active_glow) ;
			mColorQOrientationQPaneGlow[QOrientations.SL_ACTIVE] = new int[] { s0_color, s0_color } ;
			
			
			// Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.dawn_s0_border_shine_0),
					res.getColor(R.color.dawn_s0_border_shine_1) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_s0_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_s0_border_shine_1_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_s0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_s0_border_shine_1_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.dawn_s1_border_shine_0),
					res.getColor(R.color.dawn_s1_border_shine_1) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_s1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_s1_border_shine_1_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_s1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_s1_border_shine_1_alpha) } ;
			
			
			
			// Easy way to set all this crap: first set all to the default s0 / s1 shine,
			// then set the special QOrientations.  Finally set ST_ACTIVE.
			
			for ( int index = 0; index < QUANTRO_QOS.length; index++ ) {
				int qo = QUANTRO_QOS[index] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			
			// Alt Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.dawn_alt_q0_border_shine_0),
					res.getColor(R.color.dawn_alt_q0_border_shine_1) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_alt_q0_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_alt_q0_border_shine_1_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_alt_q0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_alt_q0_border_shine_1_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.dawn_alt_q1_border_shine_0),
					res.getColor(R.color.dawn_alt_q1_border_shine_1) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_alt_q1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_alt_q1_border_shine_1_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_alt_q1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_alt_q1_border_shine_1_alpha) } ;
			
			
			// here are the alts.  We don't use SL or variants.
			qos = new int[] {
					QOrientations.F0,
					QOrientations.F1,
					QOrientations.ST,
					QOrientations.ST_INACTIVE,
			} ;
			
			for ( int i = 0; i < qos.length; i++ ) {
				int qo = qos[i] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			
			// SL Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.dawn_sl_q0_border_shine_0),
					res.getColor(R.color.dawn_sl_q0_border_shine_1),
					res.getColor(R.color.dawn_sl_q0_border_shine_2) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_sl_q0_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_q0_border_shine_1_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_q0_border_shine_2_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_q0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_q0_border_shine_1_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_q0_border_shine_2_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.dawn_sl_q1_border_shine_0),
					res.getColor(R.color.dawn_sl_q1_border_shine_1),
					res.getColor(R.color.dawn_sl_q1_border_shine_2) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_sl_q1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_q1_border_shine_1_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_q1_border_shine_2_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_q1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_q1_border_shine_1_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_q1_border_shine_2_alpha) } ;
			
			qos = new int[] {
					QOrientations.S0_FROM_SL,
					QOrientations.S1_FROM_SL,
					QOrientations.SL } ;
			
			for ( int i = 0; i < qos.length; i++ ) {
				int qo = qos[i] ;
				for ( int qp = 0; qp < 2; qp++ ) {
					mShineColorQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineColor : s1ShineColor ;
					mShineShapeQOrientationQPane[qo][qp] =
						qp == 0 ? s0ShineShape : s1ShineShape ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] =
						qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
				}
			}
			
			
			// SL INACTIVE Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.dawn_sl_inactive_q0_border_shine_0),
					res.getColor(R.color.dawn_sl_inactive_q0_border_shine_1) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_sl_inactive_q0_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_inactive_q0_border_shine_1_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_inactive_q0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_inactive_q0_border_shine_1_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.dawn_sl_inactive_q1_border_shine_0),
					res.getColor(R.color.dawn_sl_inactive_q1_border_shine_1) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_sl_inactive_q1_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_inactive_q1_border_shine_1_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_inactive_q1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_inactive_q1_border_shine_1_alpha) } ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mShineColorQOrientationQPane[QOrientations.SL_INACTIVE][qp] =
					qp == 0 ? s0ShineColor : s1ShineColor ;
				mShineShapeQOrientationQPane[QOrientations.SL_INACTIVE][qp] =
					qp == 0 ? s0ShineShape : s1ShineShape ;
				mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_INACTIVE][qp] =
					qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
			}
			
			
			// SL ACTIVE Shines!
			s0ShineColor = new int[] {
					res.getColor(R.color.dawn_sl_active_border_shine_0),
					res.getColor(R.color.dawn_sl_active_border_shine_1) } ;
			s0ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_sl_active_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_active_border_shine_1_shape) } ;
			s0ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_active_q0_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_active_q0_border_shine_1_alpha) } ;
			
			s1ShineColor = new int[] {
					res.getColor(R.color.dawn_sl_active_border_shine_0),
					res.getColor(R.color.dawn_sl_active_border_shine_1) } ;
			s1ShineShape = new ShineShape[] {
					shineShapeFromResource(res, R.integer.dawn_sl_active_border_shine_0_shape),
					shineShapeFromResource(res, R.integer.dawn_sl_active_border_shine_1_shape) } ;
			s1ShineAlphaRange = new int[][] {
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_active_q1_border_shine_0_alpha),
					shineAlphaRangeFromResource(res, R.integer.dawn_sl_active_q1_border_shine_1_alpha) } ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mShineColorQOrientationQPane[QOrientations.SL_ACTIVE][qp] =
					qp == 0 ? s0ShineColor : s1ShineColor ;
				mShineShapeQOrientationQPane[QOrientations.SL_ACTIVE][qp] =
					qp == 0 ? s0ShineShape : s1ShineShape ;
				mShineAlphaQOrientationQPaneMinMax[QOrientations.SL_ACTIVE][qp] =
					qp == 0 ? s0ShineAlphaRange : s1ShineAlphaRange ;
			}
			
			break ;
			
			
		default:
			throw new IllegalArgumentException("Don't know what to do with Quantro color " + color) ;
		}
		
		
		// row guides
		setQuantroRowGuide() ;
	}
	
	private void setQuantroRowGuide() {
		//Set row guide to match S0, S1.
		mBottomGuideColor_modeQPane[0][0] = getBorderColorSimplified(QOrientations.S0, 0) ;
		mBottomGuideColor_modeQPane[0][1] = getBorderColorSimplified(QOrientations.S1, 1) ;
		
		mBottomGuideShineColor_modeQPane[0][0] = new int[]{ 0xffffffff } ;
		mBottomGuideShineColor_modeQPane[0][1] = new int[]{ 0xffffffff } ;

		mBottomGuideShineShape_modeQPane[0][0] = new ShineShape[] { ShineShape.FULL_WRAP_TOP_LEFT } ;
		mBottomGuideShineShape_modeQPane[0][1] = new ShineShape[] { ShineShape.FULL_WRAP_TOP_LEFT } ;
		
		mBottomGuideShineAlpha_modeQPaneMinMax[0][0] = new int[][] { SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] } ;
		mBottomGuideShineAlpha_modeQPaneMinMax[0][1] = new int[][] { SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] } ;
	}
	
	
	private void setRetroColors( Resources res, Skin.Color color ) {
		
		// simple storage
		int colorFill ;
		int colorBorder ;
		int colorShadow ;
		
		int colorShine ;
		ShineShape shineShape ;
		int [] shineAlphaRange ;
		
		switch( color ) {
		case RETRO:
			// background color
			mBlockBackgroundColorRetro = res.getColor(R.color.standard_block_background) ;
			
			// Rainbow colors.  All are independent of QPane.
			int shadow = res.getColor(R.color.standard_fill_shadow) ;
			int dropShadow = res.getColor(R.color.standard_drop_shadow) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.standard_r0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.standard_r1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.standard_r2_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.standard_r3_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.standard_r4_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.standard_r5_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.standard_r6_fill) ;
				mColorQOrientationQPaneFill[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.standard_rainbow_bland_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.standard_rainbow_bland_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.standard_rainbow_bland_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP][qp] = res.getColor(R.color.standard_rainbow_bland_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.standard_rainbow_bland_fill) ;
				
				
				
				mColorQOrientationQPaneBorder[QOrientations.R0][qp] = res.getColor(R.color.standard_r0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R1][qp] = res.getColor(R.color.standard_r1_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R2][qp] = res.getColor(R.color.standard_r2_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R3][qp] = res.getColor(R.color.standard_r3_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R4][qp] = res.getColor(R.color.standard_r4_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R5][qp] = res.getColor(R.color.standard_r5_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R6][qp] = res.getColor(R.color.standard_r6_border) ;
				mColorQOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.standard_rainbow_bland_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.standard_rainbow_bland_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.standard_rainbow_bland_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_UP][qp] = res.getColor(R.color.standard_rainbow_bland_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.standard_rainbow_bland_border) ;
				
				
				// Detail Colors
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp] = new int[1] ;

				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp][0] = res.getColor(R.color.standard_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp][0] = res.getColor(R.color.standard_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp][0] = res.getColor(R.color.standard_push_up_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp][0] = res.getColor(R.color.standard_push_up_chevron) ;
				
				mColorQOrientationQPaneInnerShadow[QOrientations.R0][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R1][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R2][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R3][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R4][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R5][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R6][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.RAINBOW_BLAND][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_DOWN][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_DOWN_ACTIVE][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_UP_ACTIVE][qp] = shadow ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_UP][qp] = shadow ;
				
				// Shadow Colors.
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					mColorQOrientationQPaneInnerShadow[qo][qp] = shadow ;
				}
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					mColorQOrientationQPaneOuterShadow[qo][qp] = dropShadow ;
				}
				
				// Shines.
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					setDefaultBorderShine( qo, qp ) ;
				}
			}
			
			break ;
			
		case PROTANOPIA:
			//  Retro: all colors are indendent of QPane.  Set only those which differ from Standard.
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.protonopia_r0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.protonopia_r1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.protonopia_r2_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.protonopia_r3_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.protonopia_r4_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.protonopia_r5_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.protonopia_r6_fill_color) ;
				
				mColorQOrientationQPaneBorder[QOrientations.R0][qp] = res.getColor(R.color.protonopia_r0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R1][qp] = res.getColor(R.color.protonopia_r1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R2][qp] = res.getColor(R.color.protonopia_r2_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R3][qp] = res.getColor(R.color.protonopia_r3_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R4][qp] = res.getColor(R.color.protonopia_r4_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R5][qp] = res.getColor(R.color.protonopia_r5_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R6][qp] = res.getColor(R.color.protonopia_r6_border_color) ;
			}
			
			break ;
			
		case DEUTERANOPIA:
			//  Retro: all colors are indendent of QPane.  Set only those which differ from Standard.
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.deuteranopia_r0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.deuteranopia_r1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.deuteranopia_r2_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.deuteranopia_r3_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.deuteranopia_r4_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.deuteranopia_r5_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.deuteranopia_r6_fill_color) ;
				
				mColorQOrientationQPaneBorder[QOrientations.R0][qp] = res.getColor(R.color.deuteranopia_r0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R1][qp] = res.getColor(R.color.deuteranopia_r1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R2][qp] = res.getColor(R.color.deuteranopia_r2_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R3][qp] = res.getColor(R.color.deuteranopia_r3_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R4][qp] = res.getColor(R.color.deuteranopia_r4_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R5][qp] = res.getColor(R.color.deuteranopia_r5_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R6][qp] = res.getColor(R.color.deuteranopia_r6_border_color) ;
			}
			
			break ;
			
		case TRITANOPIA:
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.tritanopia_r0_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.tritanopia_r1_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.tritanopia_r2_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.tritanopia_r3_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.tritanopia_r4_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.tritanopia_r5_fill_color) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.tritanopia_r6_fill_color) ;
				
				mColorQOrientationQPaneBorder[QOrientations.R0][qp] = res.getColor(R.color.tritanopia_r0_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R1][qp] = res.getColor(R.color.tritanopia_r1_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R2][qp] = res.getColor(R.color.tritanopia_r2_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R3][qp] = res.getColor(R.color.tritanopia_r3_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R4][qp] = res.getColor(R.color.tritanopia_r4_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R5][qp] = res.getColor(R.color.tritanopia_r5_border_color) ;
				mColorQOrientationQPaneBorder[QOrientations.R6][qp] = res.getColor(R.color.tritanopia_r6_border_color) ;
			}
			
			break ;
			
		case NEON:
			// block background
			mBlockBackgroundColorRetro = res.getColor(R.color.ne0n_block_field_background) ;
			
			// set those which differ from standard.
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.ne0n_r0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.ne0n_r1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.ne0n_r2_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.ne0n_r3_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.ne0n_r4_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.ne0n_r5_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.ne0n_r6_fill) ;
				mColorQOrientationQPaneFill[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.ne0n_rainbow_bland_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.ne0n_push_down_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.ne0n_push_down_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP][qp] = res.getColor(R.color.ne0n_push_up_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.ne0n_push_up_fill) ;
				
				
				mColorQOrientationQPaneBorder[QOrientations.R0][qp] = res.getColor(R.color.ne0n_r0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R1][qp] = res.getColor(R.color.ne0n_r1_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R2][qp] = res.getColor(R.color.ne0n_r2_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R3][qp] = res.getColor(R.color.ne0n_r3_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R4][qp] = res.getColor(R.color.ne0n_r4_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R5][qp] = res.getColor(R.color.ne0n_r5_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R6][qp] = res.getColor(R.color.ne0n_r6_border) ;
				mColorQOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.ne0n_rainbow_bland_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.ne0n_push_down_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.ne0n_push_down_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_UP][qp] = res.getColor(R.color.ne0n_push_up_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.ne0n_push_up_border) ;
				
				
				mColorQOrientationQPaneInnerShadow[QOrientations.R0][qp] = res.getColor(R.color.ne0n_r0_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R1][qp] = res.getColor(R.color.ne0n_r1_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R2][qp] = res.getColor(R.color.ne0n_r2_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R3][qp] = res.getColor(R.color.ne0n_r3_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R4][qp] = res.getColor(R.color.ne0n_r4_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R5][qp] = res.getColor(R.color.ne0n_r5_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.R6][qp] = res.getColor(R.color.ne0n_r6_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.ne0n_rainbow_bland_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.ne0n_push_down_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.ne0n_push_down_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_UP][qp] = res.getColor(R.color.ne0n_push_up_inner_shadow) ;
				mColorQOrientationQPaneInnerShadow[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.ne0n_push_up_inner_shadow) ;
				
				
				mColorQOrientationQPaneOuterShadow[QOrientations.R0][qp] = res.getColor(R.color.ne0n_r0_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.R1][qp] = res.getColor(R.color.ne0n_r1_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.R2][qp] = res.getColor(R.color.ne0n_r2_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.R3][qp] = res.getColor(R.color.ne0n_r3_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.R4][qp] = res.getColor(R.color.ne0n_r4_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.R5][qp] = res.getColor(R.color.ne0n_r5_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.R6][qp] = res.getColor(R.color.ne0n_r6_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.ne0n_rainbow_bland_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.ne0n_push_down_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.ne0n_push_down_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.PUSH_UP][qp] = res.getColor(R.color.ne0n_push_up_outer_shadow) ;
				mColorQOrientationQPaneOuterShadow[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.ne0n_push_up_outer_shadow) ;
				
				
				int neonShineColor = res.getColor(R.color.ne0n_border_shine_color) ;
				ShineShape neonShineShape = this.shineShapeFromResource(res, R.integer.ne0n_border_shine_shape) ;
				int [] neonShineAlphaRange = this.shineAlphaRangeFromResource(res, R.integer.ne0n_border_shine_alpha_range) ;
				
				// apply these universally.
				for ( int index = 0; index < RETRO_QOS.length; index++ ) {
					int qo = RETRO_QOS[index] ;
					mShineColorQOrientationQPane[qo][qp] = new int[] { neonShineColor } ;
					mShineShapeQOrientationQPane[qo][qp] = new ShineShape[] { neonShineShape } ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] = new int[][] { neonShineAlphaRange } ;
				}
			}
			
			break ;
			
			
		case CLEAN:
			mBlockBackgroundColorRetro = res.getColor(R.color.clean_block_background) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.clean_rainbow_bland) ;
				mColorQOrientationQPaneFill[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.clean_rainbow_bland) ;
				
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp] = new int[1] ;
				
				// Push Colors
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp][0] = 
					mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp][0] = 
					mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN][qp] = 
					mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN_ACTIVE][qp] =
						res.getColor(R.color.clean_push_down) ;
				
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp][0] = 
					mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp][0] = 
					mColorQOrientationQPaneBorder[QOrientations.PUSH_UP][qp] = 
					mColorQOrientationQPaneBorder[QOrientations.PUSH_UP_ACTIVE][qp] =
						res.getColor(R.color.clean_push_up) ;
				
			}
			
			break ;
			
		
		case PRIMARY:
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.primary_r0_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.primary_r1_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.primary_r2_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.primary_r3_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.primary_r4_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.primary_r5_fill) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.primary_r6_fill) ;
				mColorQOrientationQPaneFill[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.primary_rainbow_bland_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.primary_push_down_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.primary_push_down_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP][qp] = res.getColor(R.color.primary_push_up_fill) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.primary_push_up_fill) ;
				
				
				mColorQOrientationQPaneBorder[QOrientations.R0][qp] = res.getColor(R.color.primary_r0_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R1][qp] = res.getColor(R.color.primary_r1_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R2][qp] = res.getColor(R.color.primary_r2_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R3][qp] = res.getColor(R.color.primary_r3_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R4][qp] = res.getColor(R.color.primary_r4_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R5][qp] = res.getColor(R.color.primary_r5_border) ;
				mColorQOrientationQPaneBorder[QOrientations.R6][qp] = res.getColor(R.color.primary_r6_border) ;
				mColorQOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.primary_rainbow_bland_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.primary_push_down_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.primary_push_down_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_UP][qp] = res.getColor(R.color.primary_push_up_border) ;
				mColorQOrientationQPaneBorder[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.primary_push_up_border) ;
			
				
				// Detail Colors
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp] = new int[1] ;
	
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp][0] = res.getColor(R.color.primary_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp][0] = res.getColor(R.color.primary_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp][0] = res.getColor(R.color.primary_push_up_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp][0] = res.getColor(R.color.primary_push_up_chevron) ;
				
				// Shines.
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					setDefaultBorderShine( qo, qp ) ;
				}
			}
			break ;
			
		
		case RED:
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.shade_red_0) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.shade_red_1) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.shade_red_2) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.shade_red_3) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.shade_red_4) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.shade_red_5) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.shade_red_6) ;
				mColorQOrientationQPaneFill[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.shade_grey) ;
				
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					mColorQOrientationQPaneBorder[qo][qp] = mColorQOrientationQPaneFill[qo][qp] ;
				}
				
				// Shines.
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					setDefaultBorderShine( qo, qp ) ;
				}
			}
			break ;
			
		case GREEN:
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.shade_green_0) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.shade_green_1) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.shade_green_2) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.shade_green_3) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.shade_green_4) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.shade_green_5) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.shade_green_6) ;
				mColorQOrientationQPaneFill[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.shade_grey) ;
				
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					mColorQOrientationQPaneBorder[qo][qp] = mColorQOrientationQPaneFill[qo][qp] ;
				}
				
				// Shines.
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					setDefaultBorderShine( qo, qp ) ;
				}
			}
			break ;
			
		case BLUE:
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneFill[QOrientations.R0][qp] = res.getColor(R.color.shade_blue_0) ;
				mColorQOrientationQPaneFill[QOrientations.R1][qp] = res.getColor(R.color.shade_blue_1) ;
				mColorQOrientationQPaneFill[QOrientations.R2][qp] = res.getColor(R.color.shade_blue_2) ;
				mColorQOrientationQPaneFill[QOrientations.R3][qp] = res.getColor(R.color.shade_blue_3) ;
				mColorQOrientationQPaneFill[QOrientations.R4][qp] = res.getColor(R.color.shade_blue_4) ;
				mColorQOrientationQPaneFill[QOrientations.R5][qp] = res.getColor(R.color.shade_blue_5) ;
				mColorQOrientationQPaneFill[QOrientations.R6][qp] = res.getColor(R.color.shade_blue_6) ;
				mColorQOrientationQPaneFill[QOrientations.RAINBOW_BLAND][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP][qp] = res.getColor(R.color.shade_grey) ;
				mColorQOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][qp] = res.getColor(R.color.shade_grey) ;
				
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					mColorQOrientationQPaneBorder[qo][qp] = mColorQOrientationQPaneFill[qo][qp] ;
				}
				
				// Shines.
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					setDefaultBorderShine( qo, qp ) ;
				}
			}
			break ;
			
			
			
		case AUSTERE:
			mBlockBackgroundColorRetro = res.getColor(R.color.austere_retro_block_background) ;
			// set those which differ from standard.
			colorFill = res.getColor(R.color.austere_retro_fill) ;
			colorBorder = res.getColor(R.color.austere_retro_border) ;
			for ( int qp = 0; qp < 2; qp++ ) {
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					
					mColorQOrientationQPaneFill[qo][qp] = colorFill ;
					mColorQOrientationQPaneBorder[qo][qp] = colorBorder ;
					
					// Shines.
					setDefaultBorderShine( qo, qp ) ;
				}
			}
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp] = new int[1] ;
	
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp][0] = res.getColor(R.color.austere_retro_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp][0] = res.getColor(R.color.austere_retro_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp][0] = res.getColor(R.color.austere_retro_push_up_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp][0] = res.getColor(R.color.austere_retro_push_up_chevron) ;
			}
			
			
			break ;
			
			
		case SEVERE:
			// set those which differ from standard.
			mBlockBackgroundColorRetro = res.getColor(R.color.severe_retro_block_background) ;
			
			colorFill = res.getColor(R.color.severe_retro_fill) ;
			colorBorder = res.getColor(R.color.severe_retro_border) ;
			
			colorShine = res.getColor(R.color.severe_retro_border_shine) ;
			shineShape = this.shineShapeFromResource(res, R.integer.severe_retro_border_shine_shape) ;
			shineAlphaRange = this.shineAlphaRangeFromResource(res, R.integer.severe_retro_border_shine_alpha_range) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					
					mColorQOrientationQPaneFill[qo][qp] = colorFill ;
					mColorQOrientationQPaneBorder[qo][qp] = colorBorder ;
					
					// Shines.
					mShineColorQOrientationQPane[qo][qp] = new int[] { colorShine } ;
					mShineShapeQOrientationQPane[qo][qp] = new ShineShape[] { shineShape } ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] = new int[][] { shineAlphaRange } ;
				}
			}
			
			// Detail Colors
			for ( int qp = 0; qp < 2; qp++ ) {
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp] = new int[1] ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp] = new int[1] ;
	
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN][qp][0] = res.getColor(R.color.severe_retro_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_DOWN_ACTIVE][qp][0] = res.getColor(R.color.severe_retro_push_down_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP][qp][0] = res.getColor(R.color.severe_retro_push_up_chevron) ;
				mColorQOrientationQPaneDetail[QOrientations.PUSH_UP_ACTIVE][qp][0] = res.getColor(R.color.severe_retro_push_up_chevron) ;
			}
			
			break ;
			
			
			
		case LIMBO:
			mBlockBackgroundColorRetro = res.getColor(R.color.limbo_block_background) ;
			// set those which differ from standard.
			colorFill = res.getColor(R.color.limbo_fill) ;
			colorBorder = res.getColor(R.color.limbo_border) ;
			colorShadow = res.getColor(R.color.limbo_shadow) ;
			
			colorShine = res.getColor(R.color.limbo_border_shine) ;
			shineShape = this.shineShapeFromResource(res, R.integer.limbo_border_shine_shape) ;
			shineAlphaRange = this.shineAlphaRangeFromResource(res, R.integer.limbo_border_shine_alpha_range) ;
			
			for ( int qp = 0; qp < 2; qp++ ) {
				for ( int i = 0; i < RETRO_QOS.length; i++ ) {
					int qo = RETRO_QOS[i] ;
					
					mColorQOrientationQPaneFill[qo][qp] = colorFill ;
					mColorQOrientationQPaneBorder[qo][qp] = colorBorder ;
					mColorQOrientationQPaneInnerShadow[qo][qp] = colorShadow ;
					mColorQOrientationQPaneOuterShadow[qo][qp] = colorShadow ;
					
					// Shines.
					mShineColorQOrientationQPane[qo][qp] = new int[] { colorShine } ;
					mShineShapeQOrientationQPane[qo][qp] = new ShineShape[] { shineShape } ;
					mShineAlphaQOrientationQPaneMinMax[qo][qp] = new int[][] { shineAlphaRange } ;
				}
			}
			break ;
			
			
		default:
			throw new IllegalArgumentException("Don't know what to do with Retro color " + color) ;
		}
		
		setRetroRowGuide( color ) ;
	}
	
	private void setRetroRowGuide( Skin.Color color ) {
		switch( color ) {
		case RETRO:
		case PRIMARY:
		case SEVERE:
		case PROTANOPIA:
		case DEUTERANOPIA:
		case TRITANOPIA:
		case LIMBO:
			// white with an inverse grey shine.
			mBottomGuideColor_modeQPane[1][0] = 0xffffffff ;
			mBottomGuideColor_modeQPane[1][1] = 0xffffffff ;
			
			mBottomGuideShineColor_modeQPane[1][0] = new int[] { 0xffa0a0a0 } ;
			mBottomGuideShineColor_modeQPane[1][1] = new int[] { 0xffa0a0a0 } ;
			
			mBottomGuideShineShape_modeQPane[1][0] = new ShineShape[] { ShineShape.FULL_WRAP_BOTTOM_RIGHT } ;
			mBottomGuideShineShape_modeQPane[1][1] = new ShineShape[] { ShineShape.FULL_WRAP_BOTTOM_RIGHT } ;
			
			mBottomGuideShineAlpha_modeQPaneMinMax[1][0] = new int[][] { SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] } ;
			mBottomGuideShineAlpha_modeQPaneMinMax[1][1] = new int[][] { SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] } ;
			break ;
			
		case CLEAN:
		case AUSTERE:
		case NEON:
			// black with an inverse white shine
			mBottomGuideColor_modeQPane[1][0] = 0xff000000 ;
			mBottomGuideColor_modeQPane[1][1] = 0xff000000 ;
			
			mBottomGuideShineColor_modeQPane[1][0] = new int[]{ 0xffffffff } ;
			mBottomGuideShineColor_modeQPane[1][1] = new int[]{ 0xffffffff } ;

			mBottomGuideShineShape_modeQPane[1][0] = new ShineShape[] { ShineShape.FULL_WRAP_TOP_LEFT } ;
			mBottomGuideShineShape_modeQPane[1][1] = new ShineShape[] { ShineShape.FULL_WRAP_TOP_LEFT } ;
			
			mBottomGuideShineAlpha_modeQPaneMinMax[1][0] = new int[][] { SHINE_ALPHA_MIN_MAX[SHINE_INDEX_WEAK] } ;
			mBottomGuideShineAlpha_modeQPaneMinMax[1][1] = new int[][] { SHINE_ALPHA_MIN_MAX[SHINE_INDEX_WEAK] } ;
			
			break ;
		}
	}
	
	
	/**
	 * Using the provided resource IDs, sets the shine for the provided qOrientation
	 * and qPane.
	 * 
	 * This method is efficient for QO / QPanes that have their own unique shine data;
	 * it is NOT efficient when the same shine style is applied to multiple block types.
	 * 
	 * @param qOrientation
	 * @param qPane
	 * @param shineShapeResourceID
	 * @param shineAlphaRangeResourceID
	 */
	@SuppressWarnings("unused")
	private void setShineFromResources( int qOrientation, int qPane,
			Resources res,
			int shineNum,
			int shineColorResourceID, int shineShapeResourceID, int shineAlphaRangeResourceID ) {
		
		this.mShineColorQOrientationQPane[qOrientation][qPane][shineNum] =
			res.getColor(shineColorResourceID) ;
		this.mShineShapeQOrientationQPane[qOrientation][qPane][shineNum] =
			shineShapeFromResource( res, shineShapeResourceID ) ;
		this.mShineAlphaQOrientationQPaneMinMax[qOrientation][qPane][shineNum] =
			shineAlphaRangeFromResource( res, shineAlphaRangeResourceID ) ;
	}
	
	
	private static ShineShape [] SHINE_SHAPE_BY_RESOURCE_INT = null ;
	private static void populateShineShapeByResourceInt( Resources res ) {
		SHINE_SHAPE_BY_RESOURCE_INT = new ShineShape[ShineShape.values().length] ;
		
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_full_wrap_top_left)] 		= ShineShape.FULL_WRAP_TOP_LEFT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_full_wrap_top_right)] 		= ShineShape.FULL_WRAP_TOP_RIGHT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_full_wrap_bottom_left)] 		= ShineShape.FULL_WRAP_BOTTOM_LEFT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_full_wrap_bottom_right)] 	= ShineShape.FULL_WRAP_BOTTOM_RIGHT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_half_wrap_top_left)] 		= ShineShape.HALF_WRAP_TOP_LEFT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_half_wrap_top_right)] 		= ShineShape.HALF_WRAP_TOP_RIGHT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_half_wrap_bottom_left)] 		= ShineShape.HALF_WRAP_BOTTOM_LEFT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_half_wrap_bottom_right)] 	= ShineShape.HALF_WRAP_BOTTOM_RIGHT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_cap_top_left_leaning)] 		= ShineShape.CAP_TOP_LEFT_LEANING ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_cap_top_right_leaning)] 		= ShineShape.CAP_TOP_RIGHT_LEANING ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_cap_bottom_left_leaning)] 	= ShineShape.CAP_BOTTOM_LEFT_LEANING ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_cap_bottom_right_leaning)] 	= ShineShape.CAP_BOTTOM_RIGHT_LEANING ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_specular_left_side_low)] 	= ShineShape.SPECULAR_LEFT_SIDE_LOW ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_specular_right_side_high)] 	= ShineShape.SPECULAR_RIGHT_SIDE_HIGH ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_specular_spot_top_side_right)] 	= ShineShape.SPECULAR_SPOT_TOP_SIDE_RIGHT ;
		SHINE_SHAPE_BY_RESOURCE_INT[res.getInteger(R.integer.block_shine_shape_specular_spot_bottom_side_left)] = ShineShape.SPECULAR_SPOT_BOTTOM_SIDE_LEFT ;
	}
	
	private ShineShape shineShapeFromResource ( Resources res, int resourceID ) {
		if ( SHINE_SHAPE_BY_RESOURCE_INT == null )
			populateShineShapeByResourceInt(res) ;
		
		int index = res.getInteger(resourceID) ;
		return SHINE_SHAPE_BY_RESOURCE_INT[index] ;
	}
	
	private int [] shineAlphaRangeFromResource ( Resources res, int resourceID ) {
		String resourceTypeName = res.getResourceTypeName(resourceID) ;
		
		// this typed value could be an alpha range (integer array [min, max]),
		// an explicit maximum alpha (integer >= 0), or a reference to a standard
		// alpha range (integer < 0).  Other values are errors.
		if ( RESOURCE_TYPE_NAME_INTEGER.equals(resourceTypeName) ) {
			int integer = res.getInteger(resourceID) ;
			if ( integer >= 0 )		// this is the maximum alpha.
				return new int[] { 0, integer } ;
			switch( integer ) {
			case -1:
				return SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] ;
			case -2:
				return SHINE_ALPHA_MIN_MAX[SHINE_INDEX_BOLD] ;
			case -3:
				return SHINE_ALPHA_MIN_MAX[SHINE_INDEX_EXTREME] ;
			case -4:
				return SHINE_ALPHA_MIN_MAX[SHINE_INDEX_WEAK] ;
			}
		}
		
		else if ( RESOURCE_TYPE_NAME_INT_ARRAY.equals(resourceTypeName) ) {
			// explicit array.
			return res.getIntArray(resourceID) ;
		}

		throw new IllegalArgumentException("Resource ID " + resourceID + " of type " + resourceTypeName + " does not match our expectations.") ;
	}
	
	
	
	
	/**
	 * Sets the DEFAULT border shine for the provided qo / qp.
	 * 
	 * "Default" shine is a whitish-emphasis near the top left, with darker
	 * border near the bottom right.  The specifics of this are determined by
	 * the currently set border color for this qo / qp.
	 * 
	 * If anything other than very light grey or white (i.e., if any color channel
	 * has value less than INVERSE_GREY_CHANNEL_THRESHOLD), the default shine
	 * is FULL_WRAP_TOP_LEFT, using color 0xffffffff, with STANDARD min/max alphas.
	 * 
	 * Otherwise, if the color is very light grey or white, the default shine is
	 * FULL_WRAP_BOTTOM_RIGHT, using color 0xff000000, with the STANDARD min/max alphas.
	 * 
	 * @param qo
	 * @param qp
	 */
	private void setDefaultBorderShine( int qo, int qp ) {
		int c = this.mColorQOrientationQPaneBorder[qo][qp] ;
		if ( Color.red(c) < INVERSE_GREY_CHANNEL_THRESHOLD
				|| Color.green(c) < INVERSE_GREY_CHANNEL_THRESHOLD
				|| Color.blue(c) < INVERSE_GREY_CHANNEL_THRESHOLD ) {
			mShineColorQOrientationQPane[qo][qp] = new int[]{ 0xffffffff } ;
			mShineShapeQOrientationQPane[qo][qp] = new ShineShape[]{ ShineShape.FULL_WRAP_TOP_LEFT } ;
			mShineAlphaQOrientationQPaneMinMax[qo][qp] = new int[][]{ SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] } ;
		}
		else {
			mShineColorQOrientationQPane[qo][qp] = new int[]{ 0xffa0a0a0 } ;
			mShineShapeQOrientationQPane[qo][qp] = new ShineShape[] { ShineShape.FULL_WRAP_TOP_LEFT } ;
			mShineAlphaQOrientationQPaneMinMax[qo][qp] = new int[][] { SHINE_ALPHA_MIN_MAX[SHINE_INDEX_STANDARD] } ;
		}
	}
	
}
