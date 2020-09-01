package com.peaceray.quantro.view.game;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;

import com.peaceray.quantro.R ;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.view.colors.ColorOp;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.game.blocks.Consts;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigBlockGrid;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigCanvas;

/**
 * A class whose instances provide relatively stable values used in
 * drawing - for example, it provides bounds on the region of the canvas
 * in which to draw the blockField, the size of the blocks, etc.  One
 * instance should be associated with each draw "region" - e.g. the
 * blockField game, the next piece, etc.
 * 
 * As a data-store, this class provides direct access (read/write)
 * to public data members.  It also allows the use of methods to
 * automatically derive values.
 * 
 * TERMINOLOGY:
 * 
 * 		Some potentially confusing terms:
 * 		alpha_ : an int in {0, 1, ..., 255}
 * 		alphaMult : a float in [0, inf.) which is multiplied against the existing alpha value
 * 		alphaScale : a float in [-1, 1] which is applied as:
 * 			if alphaScale > 0:
 * 				alpha = (255 - alpha) * alphaScale + alpha
 * 			else:
 * 				alpha = alpha * (1 + alphaScale)
 * 			// thus alphaScale = 0 has no effect,
 * 			// positive alphaScale interpolates linearly to 255,
 * 			// negative alphaScale interpolates linearly to 0.
 * 
 * 
 * NOTE: As of 2/12/2013, we are starting to break this monolithic class into
 * 		more modular subcomponents, starting with BlockDrawerConfigCanvas.  The
 * 		motivating example for the initial change is to allow drawing or blitting
 * 		of slice content to different canvases under different circumstances.
 * 
 * 	Future adaptations will be meant to allow in-game changing and loading of skins,
 * 	backgrounds, etc. -- independently of each other -- and also allow more adaptability
 * 	and customization of skins.  DrawSettings is therefore being repurposed into a 
 * 	container object -- gradually -- and one that constructs its own set of component
 * 	objects.
 * 
 * @author Jake
 *
 */
public class DrawSettings {
	
	
	public static final int COLORBLINDNESS_NONE = 0 ;
	public static final int COLORBLINDNESS_PROTONOPIA = 1 ;
	public static final int COLORBLINDNESS_DEUTERANOPIA = 2 ;
	public static final int COLORBLINDNESS_TRITANOPIA = 3 ;
	public static final int COLORBLINDNESS_ACHROMATOPSIA = 4 ;
	
	private static final int SIMULATED_COLORBLINDNESS = COLORBLINDNESS_NONE ;
	
	private static final String TAG = "DrawSettings" ;

	// Level of detail to draw?
	// Draw mode; determines the detail of animation and draws
	public final static int DRAW_DETAIL_MINIMAL = 0 ;	// extremely low-fi.  Fast, but very, very ugly.
	public final static int DRAW_DETAIL_LOW = 1 ;		// very basic colors, no fancy shadows or bitmaps
	public final static int DRAW_DETAIL_MID = 2 ;		// lots of shadows and bitmaps with neat border shading
	public final static int DRAW_DETAIL_HIGH = 3 ;		// same as mid, but includes outside drop-shadows.
	public int drawDetail ;
	
	public final static int DRAW_ANIMATIONS_NONE = 0 ;
	public final static int DRAW_ANIMATIONS_STABLE_STUTTER = 1 ;	// stutter through "stable" states at a fixed rate; don't draw animated transitions.
	public final static int DRAW_ANIMATIONS_ALL = 2 ;
	public int drawAnimations ;
		// if false, we will only be drawing static states
	
	public final static int DRAW_EFFECTS_IN_SLICE = 0 ;				// draw effects in and only in their creating slice.
	public final static int DRAW_EFFECTS_THROUGH_SLICE_HELD = 1 ;	// draw effects from one slice "lingering" in the next slice, holding it until we're done.
	public final static int DRAW_EFFECTS_THROUGH_SLICE = 2 ;		// draw effects from one slice through the next while it continues to animate.  Only some slice types support this; others default to *_HELD.
	public int drawEffects ;
	
	public final static int IMAGES_SIZE_NONE = 0 ;
	public final static int IMAGES_SIZE_SMALL = 1 ;
	public final static int IMAGES_SIZE_MID = 2 ;
	public final static int IMAGES_SIZE_LARGE = 3 ;
	public final static int IMAGES_SIZE_HUGE = 4 ;		// supported for backgrounds only.  BG image is max of screen size and LARGE.
	public int loadImagesSize ;
	public int loadBackgroundSize ;
	
	public final static int GLOW_DRAW_STYLE_BOX = 0 ;	// draw a standard box
	public final static int GLOW_DRAW_STYLE_FADE = 1 ;	// draw an image that fades over time, with alphas never > 255.
	public final static int GLOW_DRAW_STYLE_FLARE = 2 ;	// uses a matrix color filter to flare the alpha >> 255.
														// previously the default "MID" detail draw, but causes
														// slowdown due to extremely high-overhead.
	private final static int NUM_GLOW_DRAW_STYLES = 3 ;
	
	public int glowDrawStyle ;
	
	
	
	public final static int BLIT_NONE = 0 ;		// draw directly to canvas.  Very inefficient.
	public final static int BLIT_FULL = 1 ;		// blit using a full-size image (mLastBitmap)
	public final static int BLIT_SEPTUPLE = 2 ;	// blits using up to 6 full-size images with very infrequent updates, and a 7th to hold the composed result w/ background.
													// VERY HIGH memory requirements.
	private int blit ;
	private int scale ;
	
	public boolean drawColorblindHelp ;
		// if true, we alter the draw behavior of certain QOs to help colorblind users
	
	public BlockDrawerConfigCanvas configCanvas ;
	public BlockDrawerConfigBlockGrid configBlocks ;
	
	// 	"Displacement" will produce a gradual push upward, punctuated by sudden
	// moments when everything shifts downward.  BlockDrawer can handle all that
	// easily.
	// 	However, our current settings place the "bottom row" of the game safely below
	// the bottom of the screen (so we can transfer-in rows before they enter view).
	// This has the unfortunate result of drawing a clear 'cutoff clip' near the top,
	// since we (by default) draw to the top edge of our inner bitmaps and then blit them
	// below the top of the screen.
	//	These values attempt to correct for this.  We add these offsets to our draw y position,
	// and blit y position, respectively.  These do not "adjust" as displacement changes; instead,
	// they merely offer a safe 'margin' so any reasonable displacement can be drawn without
	// the visible clip appearing.  Obviously extreme displacements will still present a 
	// problem but those are expected to very rare.
	public int displacementSafeMarginOffsetDraw ;
	public int displacementSafeMarginOffsetBlit ;
	
	private ColorScheme mColorScheme ;
	private Skin mSkin ;
	
	
	////////////////////
	// Differences:
	// RETRO:
	// 		DRAW_DETAIL_LOW:
	//			fill color interiors
	//			border color borders
	//		DRAW_DETAIL_MID:
	//			draws fill color interiors, then applies boundary shadows according to neighbors
	//			border color + "shine" gradient borders.
	
	// Game meta-info
	public int ROWS, COLS ;		// blockField size
	public int rowsToFit ;		// Number of rows the user wants to display.  We configure
									// block sizes to ensure at least this many rows are shown.
	public int displayedRows ;
	
	public int blockFieldOuterBuffer = 1 ;	
									// ROWS,COLS, displayedRows describes the data content
									// of int [][][] blockFields.  This value describes
									// the buffer of 0's surrounding the data.  If = 'b',
									// the content of the blockField is contained within
									// [0:1][b:ROWS+b][b:COLS+b]
									// and the blockField 3d array is sized
									// [2][ROWS+b*2][COLS+b*2].
	
	public int width ;
	public int height ;
	public boolean drawQ0 ;
	public boolean drawQ1 ;
	public boolean draw3D ;
	
	public int horizontalMargin = 0;	// Space between sides of drawable rectangle and sides of field
	public int verticalMargin = 0;	// Space between the top and bottom of the drawable rectangle and the field
	
	
	// OPTIMIZATION: these speed up getBlockXPosition and getBlockYPosition; these
	// two methods together account for over 10% of our CPU time!
	private int [][] blockXPositionCached_byQPane_X = null ;
	private int [][] blockYPositionCached_byQPane_Y = null ;
	private static final int INDEX_OFFSET_QPANE = 1 ;
	private static final int INDEX_OFFSET_X = 5 ;		// beyond boundaries by up to 5
	private static final int INDEX_OFFSET_Y = 5 ;		// beyond boundaries by up to 5
	
	
	
	// Information about how certain elements should be drawn.
	// Some info
	public boolean squareBlocks ;
	
	// QOs: These are the QOrientations which the BlockDrawer should be expected to draw
	// in each QPane.  Behavior in the case of 'null' is to be prepared for any QOrientation.
	// An empty array in a particular QPane indicates that nothing will be displayed there.
	// Regardless of the content of this array, QOrientations with 
	// BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER_VOLATILE border behavior will be drawn;
	// however, other border behaviors may fail.
	public byte [] qo ;
	
	// VEILS ONLY: Veil-generated DrawSettings objects have recommended QOrientations.
	public byte [] veil_qo ;
	public byte veil_qo_main ;		// if > -1, the main qo to tile: should be more prominent than veil_qo.
	
	// EXACT REPRESENTATION
	public int size_blockWidth ;
	public int size_blockHeight ;
	
	public int size_qXOffset ;					// Horiz. distance from q0 to q1.
	public int size_qYOffset ;					// Vert. distance from q0 to q1.
	
	public int size_borderWidth ;				// Width of border
	public int size_borderHeight ;				// Height of border
	
	public int size_smallPrismWidth ;			// X dimension of small rectangular prisms
	public int size_smallPrismHeight ;			// Y dimension
	
	public int size_innerBorderWidth ;
	public int size_innerBorderHeight ;
	
	public int size_innerBorderXInset ;		// Horiz. distance from block edge to inner border
	public int size_innerBorderYInset ;		// Vert. distance
	
	public int size_miniBorderWidth ;
	public int size_miniBorderHeight ;
	
	public int size_miniBorderXInset ;
	public int size_miniBorderYInset ;
	
	public float [] size_hashingLineWidth ;			// Width of any hashing we draw
	public float [] size_hashingLineSpacing ;		// Spacing between lines for our hashing
	public float [] size_hashingLineShadowLayerRadius ;
	
	// shadows
	public float [] size_innerShadowGaussianRadius ;	// the std. deviation of the gaussian blur applied to edge shadows
	public int [] size_innerShadowXOffset ;	// The distance to offset the "offset" shadow in X dim.
	public int [] size_innerShadowYOffset ;	// The distance to offset the "offset" shadow in X dim.
	
	public float size_extrudedWallShadowGaussianRadius ;
	
	public float [] size_outerShadowGaussianRadius ;	// the std. deviation of the gaussian blur applied to outer shadows
	public int [] size_outerShadowXOffset ;	// the std. deviation of the gaussian blur applied to outer shadows
	public int [] size_outerShadowYOffset ;	// the std. deviation of the gaussian blur applied to outer shadows
	
	// PROPRTIONAL REPRESENTATION
	
	// here are our default proportions.
	public float proportion_qXOffset ;		// qxOffset, as a proportion of block width.
	public float proportion_qYOffset ;		// qyOffset, as a proportion of block width.
	
	public float proportion_borderWidth ;	// Border width as a proportion of blockWidth
	public float proportion_borderHeight ;	// Border height as a proportion of blockHeight
	
	public float proportion_smallPrismWidth ;	// Small prism width as a proportion of blockWidth
	public float proportion_smallPrismHeight ;	// Small prism height as a proportion of blockHeight
	
	public float proportion_innerBorderWidth ;	// Width of a border drawn within a square
	public float proportion_innerBorderHeight ;	// Height of that border
	
	public float proportion_innerBorderXInset ;	// Distance from left of block to left inner border, as a proportion of block width
	public float proportion_innerBorderYInset ;	// Distance from top of block to top inner border, as a proportion of block height

	public float proportion_miniBorderWidth ;	// Used for unstable blocks - a very small area.
	public float proportion_miniBorderHeight ;
	
	public float proportion_miniBorderXInset ;
	public float proportion_miniBorderYInset ;
	
	public float [] proportion_hashingLineWidth ;			// Width of any hashing we draw
	public float [] proportion_hashingLineSpacing ;		// Spacing between lines for our hashing
	public float [] proportion_hashingLineShadowLayerRadius ;	// Applied to the paint
	
	public float proportion_columnUnlockerExtension ;	// Proportional extension of the X coordinate of a block to the corner of the column-unlocker.
	public float proportion_columnUnlockerInset ;		// Proportional inset of the X coordinate... these may be > 0, < 0, or 0.
	public float proportion_columnUnlockerHeight ;		// Proportional height of the column unlocker.
	
	// shadows.
	public float [] proportion_innerShadowGaussianRadius ;	// the std. deviation of the gaussian blur applied to edge shadows
	public float [] proportion_innerShadowXOffset ;	// The distance to offset the "offset" shadow in X dim.
	public float [] proportion_innerShadowYOffset ;	// The distance to offset the "offset" shadow in X dim.
	
	public float proportion_extrudedWallShadowGaussianRadius ;
	
	public float [] proportion_outerShadowGaussianRadius ;	// the std. deviation of the gaussian blur applied to outer shadows
	public float [] proportion_outerShadowXOffset ;	// the std. deviation of the gaussian blur applied to outer shadows
	public float [] proportion_outerShadowYOffset ;	// the std. deviation of the gaussian blur applied to outer shadows
	
	/**
	 * GlowColor: an enumeration for the various colors of glow which
	 * we might want to draw.
	 * 
	 * Glows are drawn (usually) as a  layered series of semi-tranparent
	 * bitmaps or rectangles, with each layer having a single color applied.
	 * This enumeration defines that color: in addition to several "fixed"
	 * or "explicit" color values, the colors belonging to the relevant
	 * qPane, triggering QOrientation, or "present" qOrientation, can be
	 * drawn.
	 * @author Jake
	 *
	 */
	public enum GlowColor {

		/**
		 * Fully white, #ffffff.
		 */
		WHITE,
		
		
		/**
		 * The color belonging to this qPane.  Usually redundant with 
		 * the fill or border color of S0, S1.
		 */
		QPANE,
		
		
		/**
		 * The QOrientation that "triggered" this glow.  Triggering is
		 * usually the most recent piece to enter the game when the glow
		 * was constructed.
		 */
		TRIGGER_QO,
		
		
		/**
		 * The QOrientation associated with the glow, set at the time
		 * it is created.  This may vary from instance to instance.
		 */
		QO
		
	}
	
	
	// GLOWS:  We lump all glow information together here, because there is a
	// particular way to process it.  These arrays are indexed first by
	// Consts.GLOW_*, then in draw order (so the later entries appear ON TOP
	// of earlier ones).  Each entry has a normalized PeakAlpha,
	// a gaussian radius, an associated "pixel size",
	// an alpha scale (it is appropriate to use high numbers, such as 5.0f-10.0f,
	// for FLARE types, but lower numbers for FADE or BOX), and a 'lightening factor'
	// (a float) between -1 and 1, which will apply lightening or darkening to
	// color, moving it towards white (if positive) or black (if negative) by
	// a linear interpolation.
	public GlowColor [][] glow_color ;
	public float [][] 	glow_lightenBy ;
	public float [][] glow_alphaPeakNormalized ;
	public float [][] 	glow_radius ;
	public int [][] 	glow_size ;
	
	
	// Hashing!
	public float [][] hashingVector_qPane ;
	
	// Draw alpha values.
	public int alpha_rowNegativeOneBorder ;
	
	public int alpha_lockedFill ;
	public int alpha_lockedBorder ;
	public int alpha_lockedTop ;
	
	public int alpha_pieceFill ;
	public int alpha_pieceBorder ;
	public int alpha_pieceTop ;
	
	public int alpha_ghostFill ;
	public int alpha_ghostBorder ;
	public int alpha_ghostTop ;
	
	public int alpha_displacementFill ;
	public int alpha_displacementBorder ;
	
	public int [] alpha_innerShadow ;
	public int alpha_extrudedWallShadow ;
	public int [] alpha_dropShadow ;
	
	
	// In Quantro, are emphasized before being cleared
	public int alpha_emphFill ;
	public int alpha_emphBorder ;
	public int alpha_emphTop ;
	
	// min/max (abs. value)
	public float [] alphaScale_lockedFillPulse ;
	public float [] alphaScale_pieceFillPulse ;
	
	public float [] alphaScale_lockedBoxPulse ;
	public float [] alphaScale_pieceBoxPulse ;
	
	// Background!
	private Background background ;
	
	// Paints!
	public int color_background_piece ;
	public int [][] color_qOrientationQPaneFill ;
	public int [][] color_qOrientationQPaneBorder ;
	public int [][] color_qOrientationQPaneFillShadow ;
	public int [][] color_qOrientationQPaneDropShadow ;
	public int [] color_qPaneBottomGuide ;
	public int [][] color_qPaneCornerBottomGuideShine ;
	
	// simplified!
	public int [][] colorSimplified_qOrientationQPaneFill ;
	public int [][] colorSimplified_qOrientationQPaneBorder ;
	
	
	public int color_prismSides ;
	public int color_prismEdges ;
	
	// Alpha multipliers!
	public float [] alphamult_qOrientationFill ;
	public float [] alphamult_qOrientationBorder ;
	public float [] alphamult_qOrientationEdge ;
	
	// Colors!
	// Corners are specified by direction, concavity, etc.
	// an INNER corner is a corner drawn within the boundaries
	// of the block it represents.
	// 		inner corner direction (e.g. LEFT_TOP) indicates
	//			the direction from the block center to reach
	//			the corner.
	//		concavity indictes whether the interior is concave
	//			or convex.  Convex corners point outward, towards
	//			other blocks than the one it is inside.
	//			Concave corners point inward, towards the block.
	// 			In other words, the top-left corner of block X is:
	//								[ ]
	//				[x]			 [ ][x]
	//
	//				convex		concave
	//			and in both cases it is drawn within the boundaries of x.
	public static final byte CORNER_INNER_LEFT_TOP_CONVEX = 0 ;
	public static final byte CORNER_INNER_RIGHT_TOP_CONVEX = 1 ;
	public static final byte CORNER_INNER_LEFT_BOTTOM_CONVEX = 2 ;
	public static final byte CORNER_INNER_RIGHT_BOTTOM_CONVEX = 3 ;
	public static final byte CORNER_INNER_LEFT_TOP_CONCAVE = 4 ;
	public static final byte CORNER_INNER_RIGHT_TOP_CONCAVE = 5 ;
	public static final byte CORNER_INNER_LEFT_BOTTOM_CONCAVE = 6 ;
	public static final byte CORNER_INNER_RIGHT_BOTTOM_CONCAVE = 7 ;
	public static final int NUM_CORNERS = 8 ;
	
	
	private int [][][] color_qOrientationQPaneCornerBorderShine ;
	public int [][] color_qOrientationQPaneGlowColor ;
	
	public int [][] color_qOrientationDetailColors ;
	public static final int DETAIL_FLASH_BURST = 0 ;
	public static final int DETAIL_FLASH_STRIPE = 1 ;
	public static final int NUM_DETAILS_FLASH = 2 ;
	public static final int DETAIL_CHEVRON = 0 ;
	public static final int NUM_DETAILS_CHEVRON = 1 ;
	
	// Drawing behavior?
	public static final int BEHAVIOR_BACKGROUND_ALL_EMPTY 		= 0 ;
	public static final int BEHAVIOR_BACKGROUND_BLOCK_FILL		= 1 ;
	public static final int BEHAVIOR_BACKGROUND_EMPTY_AND_PIECE = 2 ;
	public static final int BEHAVIOR_BACKGROUND_CLIP_OR_BLIT_IMAGE		= 3 ;		// allow the BlockDrawer to clip, or blit, the image as it chooses.
	public static final int BEHAVIOR_BACKGROUND_BLIT_IMAGE		= 4 ;				// always blit the background to canvas; 
	public int behavior_background ;
	
	public static final int BEHAVIOR_BORDER_NO_EDGES 			= 0 ;
	public static final int BEHAVIOR_BORDER_ROW_NEGATIVE_ONE 	= 1 ;
	public int behavior_border ;
	
	public static final int BEHAVIOR_HASHING_NONE 			= 0 ;
	public static final int BEHAVIOR_HASHING_SOME 			= 1 ;
	public int behavior_hashing ;
	
	public static final int BEHAVIOR_ALIGN_GRID				= 0 ;
	public static final int BEHAVIOR_ALIGN_CENTER_BLOCKS 	= 1 ;
	public static final int BEHAVIOR_ALIGN_CENTER_GRID		= 2 ;
	public int behavior_align_vertical ;
	public int behavior_align_horizontal ;
	
	public static final int BEHAVIOR_CLEAR_EMPH_OLD_BORDERS = 0 ;
	public static final int BEHAVIOR_CLEAR_EMPH_NEW_BORDERS = 1 ;
	public int behavior_clear_emph ;
	
	public static final int BEHAVIOR_DISPLACEMENT_NO 		= 0 ;
	public static final int BEHAVIOR_DISPLACEMENT_YES 		= 1 ;
	private int behavior_displacement ;
	
	// Painting behavior?
	public static final int BEHAVIOR_QO_FILL_PAINT_NONE 						= 0 ;	// No fill paint here
	public static final int BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION 				= 1 ;	// Fill with color_qOrientationFill[qo]
	public static final int BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_INSET 			= 2 ;	// Fill with color_qOrientationFill[qo] within the "inset border" region
	public static final int BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_FRIENDLY_BLEND = 3 ;	// Fill with color_qOrientationFill[qo] if all neighbors are identical; otherwise, attempt to blend the fill color with any "friendly" neighbors.
	public static final int BEHAVIOR_QO_FILL_PAINT_Q_PANE 						= 4 ;	// Fill with color_qOrientationFill[S0 or S1], depending on qPane.
	public static final int BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_MINI			= 5 ;
	public static final int BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_MINI_NONOVERLAP = 6 ;	// Fill with paint inside a "mini" area, but only those areas which no not overlap the other qOrientation.
	public int [] behavior_qo_fill ;		// Each entry is one of the above.
	
	public static final int BEHAVIOR_QO_HASHING_NONE 				= 0 ;	// No inner hashing
	public static final int BEHAVIOR_QO_HASHING_QPANE 				= 1 ;	// Hashing based on QPane.
	public int [] behavior_qo_hashing ;
	
	public static final int BEHAVIOR_QO_3D_NONE										= 0 ;	// No 3D representation
	public static final int BEHAVIOR_QO_3D_PAINT_Q_ORIENTATION_SMALL_PRISM 			= 1 ;	// Use QOrientation Fill and Edge colors to draw a small prism
	public static final int BEHAVIOR_QO_3D_PAINT_Q_ORIENTATION_SMALL_PRISM_SIDES_ONLY 	= 2 ;	// Use QOrientation Fill and Edge colors to draw a small prism, omitting the "top" and "bottom".
	public static final int BEHAVIOR_QO_3D_PAINT_STANDARD_SMALL_PRISM 				= 3 ;	// Use QOrientation Fill and Edge colors to draw a small prism
	public static final int BEHAVIOR_QO_3D_PAINT_STANDARD_SMALL_PRISM_SIDES_ONLY 	= 4 ;	// Use QOrientation Fill and Edge colors to draw a small prism, omitting the "top" and "bottom".
	public static final int BEHAVIOR_QO_3D_PAINT_BORDER_EXTRUDED_SIDES_ONLY 		= 5 ;	// "Extrude" the q0 layer's border layer into the space below, drawing only the "edges": i.e., drawing a line requires 4 sides, an L, 6.
	public static final int BEHAVIOR_QO_3D_PAINT_EXTRUDED_QO_PULSE_BOX				= 6 ;
	public int [] behavior_qo_3d ;			// Each entry is one of the above.
	
	public static final int BEHAVIOR_QO_TOP_NONE 										= 0 ;	// No "top" layer
	public static final int BEHAVIOR_QO_TOP_PAINT_Q_ORIENTATION_SMALL_PRISM_TOP 		= 1 ;	// Draw the top (or bottom) of a small prism, using the QOrientation fill color.
	public static final int BEHAVIOR_QO_TOP_PAINT_STANDARD_SMALL_PRISM_TOP 				= 2 ;	// Draw the top (or bottom) of a small prism, using the QOrientation fill color.
	public static final int BEHAVIOR_QO_TOP_CUSTOM_BITMAP 								= 3 ;	// Draw a custom bitmap, if one is available.  Drawn so as to be within block borders.
	public static final int BEHAVIOR_QO_TOP_PAINT_BLAND_FILL							= 4 ;	// STOPGAP.  TODO: change to something better.  Use the blandest available QOrientation fill color as a fill using top-opacity.
	public static final int BEHAVIOR_QO_TOP_PAINT_DETAIL_FILL							= 5 ;	// Draws the first detail color available.
	public int [] behavior_qo_top ;
	
	public static final int BEHAVIOR_QO_BORDER_NONE					= 0 ;	// Don't draw a border
	public static final int BEHAVIOR_QO_BORDER_Q_PANE_OUTER 		= 1 ;	// An outer border, colored with QOrientation color.
	public static final int BEHAVIOR_QO_BORDER_Q_PANE_INSET 		= 2 ;	// An "inset" border rectangle, just inside this square.
	public static final int BEHAVIOR_QO_BORDER_INNER 				= 3 ;	// Borders this exact block; does not extend to others.
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER 	= 4 ;	// A border just inside this block that follows edges
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER_VOLATILE = 5 ;	// A border just inside the block following the edges, whose color value should NOT be cached - it can change between calls.  Used for veils to prevent use of pre-allocated drawables.
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY = 6 ;	// A border just inside this block that follows edges between this and NO, but allows connected to all other blocks.
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY_BLEND = 7 ;	// A border just inside this block that follows edges between this and NO, but allows connected to all other blocks.
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_INSET 	= 8 ;	// A border "inset" within the block, colored according to QOrientation.
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI 	= 9 ;
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI_CHAIN_LINK_VERTICAL_OVER = 10 ;	// Draws a "mini" border which is linked with the other QPane.  Vertical bars go "over" horizontal bars.
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI_CHAIN_LINK_HORIZONTAL_OVER = 11 ;	// Draws a "mini" border which is linked with the other QPane.  Horizontal bars go "over" vertical bars.
	public static final int BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER_CLIQUE = 12 ;	// Joins with blocks in the same clique, defined as those with CLIQUE behavior and the same border color.
	public int [] behavior_qo_border ;
	
	public static final int BEHAVIOR_QO_BORDER_SHINE_NONE			= 0 ;
	public static final int BEHAVIOR_QO_BORDER_SHINE_CUSTOM			= 1 ;
	public int [] behavior_qo_border_shine ;
	
	public static final int BEHAVIOR_QO_LOCK_GLOW_NONE 				= 0 ;
	public static final int BEHAVIOR_QO_LOCK_GLOW_NORMAL 			= 1 ;
	public int [] behavior_qo_lock_glow ;
	
	// Pulse behavior:
	public static final int BEHAVIOR_QO_PULSE_NONE 					= 0 ;
	public static final int BEHAVIOR_QO_PULSE_FILL					= 1 ;
	public int [] behavior_qo_pulse ;
	
	
	// Metamorphosis behavior:
	public static final int BEHAVIOR_QO_METAMORPHOSIS_NONE 			= 0 ;
	public static final int BEHAVIOR_QO_METAMORPHOSIS_LOCK_GLOW		= 1 ;	// behaves as if the metamorph'd pieces just locked
	public static final int BEHAVIOR_QO_METAMORPHOSIS_GLOW 			= 2 ;	// a special metamorphosis glow
	public static final int BEHAVIOR_QO_METAMORPHOSIS_SMOOTH		= 3 ;	// normally we snap from one field to the other.  This "fades" between them.
	public int [] behavior_qo_metamorphosis_from ;
	public int [] behavior_qo_metamorphosis_to ;
	
	
	// "Falling" behavior.
	public static final int BEHAVIOR_QO_FALLING_CHUNK_STANDARD 		= 0 ;	// falls the specified distance, then locks in place using normal "lock" behavior.
	public static final int BEHAVIOR_QO_FALLING_CHUNK_PIECE_TYPE_PIECE_COMPONENT_RISE_AND_FADE	= 1 ;	// rises upward, with alpha -> 0.  Will rise the minimum of a height determined by animation settings and the number of blocks of space above.  Used if the chunk is a piece component, and the piece type QOrientation matches this behavior.
	public int [] behavior_qo_falling_chunk ;
			// later behaviors take precedence, if present in the falling chunk.
	
	
	public static final int BEHAVIOR_QO_BITMAP_NONE 			= 0 ;	// no custom bitmap
	public static final int BEHAVIOR_QO_BITMAP_FLASH_STRIPE 		= 1 ;	// the "flash" bitmap: a color burst accented with a vertical stripe.
	public static final int BEHAVIOR_QO_BITMAP_CHEVRON_TOP 			= 2 ;	// a unique color stripe -- a chevron.
	public static final int BEHAVIOR_QO_BITMAP_CHEVRON_BORDER		= 3 ;	// Chevron formed by the inner border, casting shadows inward.
																					// border, shadows, etc. colored as the block.
	public int [] behavior_qo_bitmap ;
	
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// DEFAULT SETTINGS.  These specify default information used to initialize
	// new DrawSettings objects.  Specifically, these may be useful in pre-caching
	// draw assets before creating a DrawSettings object.
	
	// DEFAULT INDEXES: these provide the first index into our DEFAULT_SETTINGS
	// arrays.  They are determined by the 'skin' provided.  For the most part,
	// skin templates share all these values, although there are some variations;
	// for example, 'clean' is considered a STANDARD template but differs w.r.t.
	// fill alphas.
	public static final int INDEX_QUANTRO_STANDARD = 0 ;
	public static final int INDEX_QUANTRO_STANDARD_NATURAL = 1 ;
	public static final int INDEX_RETRO_STANDARD = 2 ;
	public static final int INDEX_RETRO_STANDARD_CLEAN = 3 ;
	public static final int INDEX_RETRO_STANDARD_AUSTERE = 4 ;
	public static final int INDEX_RETRO_STANDARD_SEVERE = 5 ;
	public static final int INDEX_RETRO_NEON = 6 ;
	public static final int INDEX_RETRO_LIMBO = 7 ;		// same as neon, except ghosts show border and fill
	public static final int NUM_INDEX_SKINS = 8 ;
	
	private static final int getDefaultSettingIndex( Skin skin ) {
		switch( skin.getGame() ) {
		case QUANTRO:
			switch( skin.getTemplate() ) {
			case STANDARD:
			case COLORBLIND:
				switch( skin.getColor() ) {
				case NATURAL:
					return INDEX_QUANTRO_STANDARD_NATURAL ;
				default:
					return INDEX_QUANTRO_STANDARD ;
				}
			}
			break ;
		case RETRO:
			switch( skin.getTemplate() ) {
			case STANDARD:
				switch( skin.getColor() ) {
				case CLEAN:
					return INDEX_RETRO_STANDARD_CLEAN ;
				case AUSTERE:
					return INDEX_RETRO_STANDARD_AUSTERE ;
				case SEVERE:
					return INDEX_RETRO_STANDARD_SEVERE ;
				default:
					return INDEX_RETRO_STANDARD ;
				}
			case NEON:
				switch( skin.getColor() ) {
				case NEON:
					return INDEX_RETRO_NEON ;
				case LIMBO:
					return INDEX_RETRO_LIMBO ;
				}
				break ;
			}
			break ;
		}
		
		throw new IllegalArgumentException("Don't know the DefaultSettingIndex for skin " + skin) ;
	}
	
	
	public static final int INDEX_TYPE_LOCKED_FILL = 0 ;
	public static final int INDEX_TYPE_LOCKED_BORDER = 1 ;
	public static final int INDEX_TYPE_LOCKED_TOP = 2 ;
	public static final int INDEX_TYPE_PIECE_FILL = 3 ;
	public static final int INDEX_TYPE_PIECE_BORDER = 4 ;
	public static final int INDEX_TYPE_PIECE_TOP = 5 ;
	public static final int INDEX_TYPE_GHOST_FILL = 6 ;
	public static final int INDEX_TYPE_GHOST_BORDER = 7 ;
	public static final int INDEX_TYPE_GHOST_TOP = 8 ;
	public static final int INDEX_TYPE_EMPH_FILL = 9 ;
	public static final int INDEX_TYPE_EMPH_BORDER = 10 ;
	public static final int INDEX_TYPE_EMPH_TOP = 11 ;
	
	public static final int INDEX_TYPE_DISPLACEMENT_FILL = 12 ;
	public static final int INDEX_TYPE_DISPLACEMENT_BORDER = 13 ;
	
	public static final int NUM_INDEX_ALPHA_TYPES = 14 ;
	
	public static final int [][] DEFAULT_SETTING_BLOCK_ALPHA = new int[NUM_INDEX_SKINS][NUM_INDEX_ALPHA_TYPES] ;
	
	
	public static final int INDEX_TYPE_PULSE_LOCKED_FILL = 0 ;
	public static final int INDEX_TYPE_PULSE_PIECE_FILL = 1 ;
	public static final int INDEX_TYPE_PULSE_LOCKED_BOX = 2 ;
	public static final int INDEX_TYPE_PULSE_PIECE_BOX = 3 ;
	public static final int NUM_INDEX_PULSE_TYPES = 4 ;

	public static final float [][][] DEFAULT_SETTING_PULSE_ALPHA_SCALE = new float[NUM_INDEX_SKINS][NUM_INDEX_PULSE_TYPES][] ;
	
	
	public static final int INDEX_TYPE_INNER_SHADOW = 0 ;
	public static final int INDEX_TYPE_EXTRUDED_SHADOW = 1 ;
	public static final int INDEX_TYPE_OUTER_SHADOW = 2 ;
	public static final int INDEX_TYPE_LOCK_GLOW = 3 ;
	public static final int INDEX_TYPE_CLEAR_GLOW = 4 ;
	public static final int INDEX_TYPE_METAMORPHOSIS_GLOW = 5 ;
	public static final int INDEX_TYPE_UNLOCK_GLOW = 6 ;
	public static final int INDEX_TYPE_ENTER_GLOW = 7 ;
	public static final int NUM_INDEX_TYPES = 8 ;
	
	public static final int INDEX_X = 0 ;
	public static final int INDEX_Y = 1 ;
	
	
	public static final float [][][] DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS = new float[NUM_INDEX_SKINS][NUM_INDEX_TYPES][] ;
	public static final float [][][][] DEFAULT_SETTING_PROPORTION_OFFSET = new float [NUM_INDEX_SKINS][NUM_INDEX_TYPES][][] ;
	public static final int [][][] DEFAULT_SETTING_ALPHA = new int[NUM_INDEX_SKINS][NUM_INDEX_TYPES][] ;

	
	public static final GlowColor [][][][] DEFAULT_SETTING_GLOW_COLOR = new GlowColor[NUM_GLOW_DRAW_STYLES][NUM_INDEX_SKINS][Consts.NUM_GLOWS][] ;
	public static final float [][][][] DEFAULT_SETTING_GLOW_LIGHTENING = new float[NUM_GLOW_DRAW_STYLES][NUM_INDEX_SKINS][Consts.NUM_GLOWS][] ;
	public static final float [][][][] DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED = new float[NUM_GLOW_DRAW_STYLES][NUM_INDEX_SKINS][Consts.NUM_GLOWS][] ;
	public static final float [][][][] DEFAULT_SETTING_GLOW_RADIUS = new float[NUM_GLOW_DRAW_STYLES][NUM_INDEX_SKINS][Consts.NUM_GLOWS][] ;
	
	
	
	static {
		for ( int index = 0; index < NUM_INDEX_SKINS; index++ ) {
			// BLOCK ALPHA
			switch( index ) {
			case INDEX_QUANTRO_STANDARD:
				DEFAULT_SETTING_BLOCK_ALPHA[index] =
					new int[] { 102, 255, 255,		102, 255, 255,		76, 0, 76,		190, 255, 255,		128, 0 } ;
				break ;
			case INDEX_QUANTRO_STANDARD_NATURAL:
				DEFAULT_SETTING_BLOCK_ALPHA[index] =
					new int[] { 102, 255, 255,		102, 255, 255,		102, 0, 76,		190, 255, 255,		128, 0 } ;
				break ;
			case INDEX_RETRO_STANDARD:
				DEFAULT_SETTING_BLOCK_ALPHA[index] =
					new int[] { 255, 255, 255, 		255, 255, 255,		76, 0, 76,		255, 255, 255,		128, 0 } ;
				break ;
			case INDEX_RETRO_STANDARD_CLEAN:
				DEFAULT_SETTING_BLOCK_ALPHA[index] =
					new int[] { 0, 255, 255, 		0, 255, 255,		76, 0, 76,		0, 255, 255,		128, 0 } ;
				break ;
			case INDEX_RETRO_STANDARD_AUSTERE:
			case INDEX_RETRO_STANDARD_SEVERE:
				DEFAULT_SETTING_BLOCK_ALPHA[index] =
					new int[] { 0, 255, 255, 		0, 255, 255,		76, 76, 76,		0, 255, 255,		128, 128 } ;
				break ;
			case INDEX_RETRO_NEON:
				DEFAULT_SETTING_BLOCK_ALPHA[index] =
					new int[] { 0, 255, 255, 		0, 255, 255,		76, 0, 76,		0, 255, 255,		128, 0 } ;
				break ;
			case INDEX_RETRO_LIMBO:
				DEFAULT_SETTING_BLOCK_ALPHA[index] =
					new int[] { 0, 255, 255, 		0, 255, 255,		128, 128, 76,	0, 255, 255,		128, 128 } ;
				break ;
			}
			
			
			// PULSE ALPHA SCALE
			switch( index ) {
			case INDEX_QUANTRO_STANDARD:
			case INDEX_QUANTRO_STANDARD_NATURAL:
				DEFAULT_SETTING_PULSE_ALPHA_SCALE[index] =
					new float[][] {
						new float[] { 0, 0.706f },			// INDEX_TYPE_PULSE_LOCKED_FILL
						new float[] { 0, 0.706f },			// INDEX_TYPE_PULSE_PIECE_FILL
						new float[] { 0.1f, 0.4f },			// INDEX_TYPE_PULSE_LOCKED_BOX
						new float[] { 0.1f, 0.4f } } ;		// INDEX_TYPE_PULSE_PIECE_BOX
				break ;
			case INDEX_RETRO_STANDARD:
			case INDEX_RETRO_STANDARD_CLEAN:
			case INDEX_RETRO_STANDARD_AUSTERE:
			case INDEX_RETRO_STANDARD_SEVERE:
			case INDEX_RETRO_NEON:
			case INDEX_RETRO_LIMBO:
				DEFAULT_SETTING_PULSE_ALPHA_SCALE[index] =
					new float[][] {
						new float[] { 0, 0 },
						new float[] { 0, 0 },
						new float[] { 0, 0 },
						new float[] { 0, 0 } } ;
				
				break ;
			}
	
			
			// INNER SHADOWS
			int indexType = INDEX_TYPE_INNER_SHADOW ;
			switch( index ) {
			case INDEX_QUANTRO_STANDARD:
			case INDEX_QUANTRO_STANDARD_NATURAL:
				// For QuantroStandard, the shadow is very similar to RETRO_STANDARD, just
				// with a different offset and darker.
				// For now, we try the same for QUANTRO_STANDARD_CLEAN, although we may
				// want to emphasize the alpha a little bit later.
				DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][indexType] = new float[] { 0.3f, 0.5f } ;
				DEFAULT_SETTING_PROPORTION_OFFSET[index][indexType] = new float[][] {
						null,
						new float[] {-0.15f, -0.15f}
				} ;
				DEFAULT_SETTING_ALPHA[index][indexType] = new int[] { 127, 170 } ;
				break ;
			case INDEX_RETRO_STANDARD:
			case INDEX_RETRO_STANDARD_CLEAN:
			case INDEX_RETRO_STANDARD_AUSTERE:
			case INDEX_RETRO_STANDARD_SEVERE:
				// For RetroStandard, we have a edge shadow, and an offset shadow.
				// RetroClean matches this value.
				DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][indexType] = new float[] { 0.3f, 0.5f } ;
				DEFAULT_SETTING_PROPORTION_OFFSET[index][indexType] = new float[][] {
						null,
						new float[] {0.2f, 0.25f}
				} ;
				DEFAULT_SETTING_ALPHA[index][indexType] = new int[] { 50, 102 } ;
				break ;
			case INDEX_RETRO_NEON:
				// For RetroNeon, we have a much brighter, more prominent shadow (i.e., multiple layers).
				// The standard shadow was difficult to see and didn't have a "glowing" appearance.
				DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][indexType] = new float[] { 0.3f, 0.3f, 0.5f } ;
				DEFAULT_SETTING_PROPORTION_OFFSET[index][indexType] = new float[][] {
						null,
						null,
						new float[] {0.2f, 0.25f}
				} ;
				DEFAULT_SETTING_ALPHA[index][indexType] = new int[] { 255, 255, 102 } ;
				break ;
			case INDEX_RETRO_LIMBO:
				// Limbo looks a bit too "fuzzy" with NE0N settings.  We try to tighten it up a bit.
				DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][indexType] = new float[] { 0.2f, 0.2f, 0.5f } ;
				DEFAULT_SETTING_PROPORTION_OFFSET[index][indexType] = new float[][] {
						null,
						null,
						null /*new float[] {0.2f, 0.25f} */
				} ;
				DEFAULT_SETTING_ALPHA[index][indexType] = new int[] { 255, 255, 102 } ;
				break ;
			}
			
			
			// OUTER SHADOWS
			indexType = INDEX_TYPE_OUTER_SHADOW ;
    		switch( index ) {
			case INDEX_QUANTRO_STANDARD:
			case INDEX_QUANTRO_STANDARD_NATURAL:
			case INDEX_RETRO_STANDARD:
			case INDEX_RETRO_STANDARD_CLEAN:
			case INDEX_RETRO_STANDARD_AUSTERE:
			case INDEX_RETRO_STANDARD_SEVERE:
				// QuantroStandard has thin, but dark, drop shadows.  Clean matches
				// this exactly.  RetroStandard uses exactly the same settings.
				DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][indexType] = new float[] {0.1f, 0.1f } ;
				DEFAULT_SETTING_PROPORTION_OFFSET[index][indexType] = new float[][] { null, new float[] {0.05f, 0.05f} } ;
				DEFAULT_SETTING_ALPHA[index][indexType] = new int[] { 255, 255 } ;
				break ;
			case INDEX_RETRO_NEON:
				// RetroNeon has a much brighter, wider drop shadow.
				DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][indexType] = new float[] {0.3f, 0.3f, 0.3f } ;
				DEFAULT_SETTING_PROPORTION_OFFSET[index][indexType] = new float[][] { null, null, null } ;
				DEFAULT_SETTING_ALPHA[index][indexType] = new int[] { 255, 255, 128 } ;
				break ;
			case INDEX_RETRO_LIMBO:
				// Limbo looks a bit too "fuzzy" with NE0N settings.  Tighten
				// up the graphics on level 3.
				DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][indexType] = new float[] {0.2f, 0.2f, 0.3f } ;
				DEFAULT_SETTING_PROPORTION_OFFSET[index][indexType] = new float[][] { null, null, null } ;
				DEFAULT_SETTING_ALPHA[index][indexType] = new int[] { 255, 255, 128 } ;
				break ;
			}
    		
    		
    		// EXTRUDED SHADOWS
    		// Extruded shadows are always the same -- at least, right now.
			DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[index][INDEX_TYPE_EXTRUDED_SHADOW] = new float[] { 0.2f } ;
			DEFAULT_SETTING_PROPORTION_OFFSET[index][INDEX_TYPE_EXTRUDED_SHADOW] = new float[][] { null } ;
			
			// GLOWS
			// Glows are determined by GlowStyle, skin, and the specific glow type.
			for ( int glowStyle = 0; glowStyle < NUM_GLOW_DRAW_STYLES; glowStyle++ ) {
				for ( int glow = 0; glow < Consts.NUM_GLOWS; glow++ ) {
					
					switch( glowStyle ) {
					case GLOW_DRAW_STYLE_FLARE:
						switch( index ) {
						case INDEX_QUANTRO_STANDARD:
						case INDEX_QUANTRO_STANDARD_NATURAL:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Flare / Quantro Standard / Lock
								// A glow colored by the QPane we lock in.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0 } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 10.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Flare / Quantro Standard / Clear
								// QPane glow around the edges of white.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.0f, 1.0f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Flare / Quantro Standard / Metamorphosis
								// The QO Color itself.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0 } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Flare / Quantro Standard / Unlock
								// White tinged with the triggering color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.TRIGGER_QO, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 9.0f, 7.5f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 0.5f } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Flare / Quantro Standard / Enter
								// White rimmed with the qPane color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 5.0f } ;
								break ;
							}
							break ;
							
						
						case INDEX_RETRO_STANDARD:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Flare / Retro Standard / Lock
								// The plain QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0 } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Flare / Retro Standard / Clear
								// Plain white.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 6.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.0f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Flare / Retro Standard / Metamorphosis
								// There is no Retro metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Flare / Retro Standard / Unlock
								// There is no Retro unlock.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Flare / Retro Standard / Enter
								// White rimmed with the qo color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 5.0f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_STANDARD_CLEAN:
						case INDEX_RETRO_STANDARD_AUSTERE:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Flare / Retro Clean / Lock
								// To match the current color scheme, we use white with colored tinges.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 9.0f, 7.5f  } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Flare / Retro Clean / Clear
								// Plain white with black edges.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.0f, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.0f, 1.0f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Flare / Retro Clean / Metamorphosis
								// There is no Retro metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Flare / Retro Clean / Unlock
								// There is no Retro unlock.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Flare / Retro Clean / Enter
								// White rimmed with the qo color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 5.0f } ;
								break ;
							}
							break ;
							
							
						case INDEX_RETRO_STANDARD_SEVERE:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Flare / Retro Severe / Lock
								// White with black edges?
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { -1.0f, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 9.0f, 7.5f  } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Flare / Retro Severe / Clear
								// Black
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { -1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.0f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Flare / Retro Severe / Metamorphosis
								// There is no Retro metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Flare / Retro Severe / Unlock
								// There is no Retro unlock.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Flare / Retro Severe / Enter
								// White rimmed with black.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { -1.0f, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 5.0f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_NEON:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Flare / Retro NE0N / Lock
								// Color and whitened color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO, GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Flare / Retro NE0N / Clear
								// Plain white.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 6.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.0f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Flare / Retro NE0N / Metamorphosis
								// There is no Retro metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Flare / Retro NE0N / Unlock
								// There is no Retro unlock.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Flare / Retro NE0N / Enter
								// White rimmed with the qo color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 5.0f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_LIMBO:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Flare / Retro Limbo / Lock
								// White-rimmed grey?
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f, 0.7f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Flare / Retro Limbo / Clear
								// Black-rimmed white?
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { -1.0f, 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 7.0f, 3.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.0f, 1.0f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Flare / Retro Limbo / Metamorphosis
								// There is no Retro metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Flare / Retro Limbo / Unlock
								// There is no Retro unlock.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] {} ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] {} ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] {} ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Flare / Retro Limbo / Enter
								// White-rimmed grey?
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE, GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f, 0.7f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 6.0f, 5.0f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f, 0.5f } ;
								break ;
							}
							break ;
						}
						break ;
						
					case GLOW_DRAW_STYLE_FADE:
						switch( index ) {
						case INDEX_QUANTRO_STANDARD:
						case INDEX_QUANTRO_STANDARD_NATURAL:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Fade / Quantro Standard / Lock
								// A glow colored by the QPane we lock in.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0 } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Fade / Quantro Standard / Clear
								// Lightened QPane.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.0f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Fade / Quantro Standard / Metamorphosis
								// The QO Color itself -- lightened?  Is this just a holdover
								// from some other design?
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Fade / Quantro Standard / Unlock
								// White tinged with the triggering color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.TRIGGER_QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.5f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Fade / Quantro Standard / Enter
								// White rimmed with the qPane color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
							}
							break ;
							
						
							
						case INDEX_RETRO_STANDARD:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Fade / Retro Standard / Lock
								// A glow colored by the QPane we lock in.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0 } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Fade / Retro Standard / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.3f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Fade / Retro Standard / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Fade / Retro Standard / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Fade / Quantro Standard / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_STANDARD_CLEAN:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Fade / Retro Clean / Lock
								// Light-color QO.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Fade / Retro Clean / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.3f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Fade / Retro Clean / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Fade / Retro Clean / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Fade / Quantro Clean / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_STANDARD_AUSTERE:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Fade / Retro Austere / Lock
								// Light-color QO.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Fade / Retro Austere / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.3f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Fade / Retro Austere / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Fade / Retro Austere / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Fade / Quantro Austere / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
							}
							break ;
							
							
							
						case INDEX_RETRO_STANDARD_SEVERE:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Fade / Retro Severe / Lock
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Fade / Retro Severe / Clear
								// Black
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { -1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.3f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Fade / Retro Severe / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Fade / Retro Severe / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Fade / Quantro Severe / Enter
								// White.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_NEON:
						case INDEX_RETRO_LIMBO:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Fade / Retro NE0N / Lock
								// Lighened QO.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Fade / Retro NE0N / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 1.3f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Fade / Retro NE0N / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Fade / Retro NE0N / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Fade / Quantro NE0N / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.6f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.5f } ;
								break ;
							}
							break ;
						}
						break ;
						
					case GLOW_DRAW_STYLE_BOX:
						switch( index ) {
						case INDEX_QUANTRO_STANDARD:
						case INDEX_QUANTRO_STANDARD_NATURAL:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Box / Quantro Standard / Lock
								// A glow colored by the QPane we lock in.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.11f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Box / Quantro Standard / Clear
								// Lightened QPane.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.2f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Box / Quantro Standard / Metamorphosis
								// The QO Color itself -- lightened?  Is this just a holdover
								// from some other design?
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0 } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.11f } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Box / Quantro Standard / Unlock
								// White tinged with the triggering color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.TRIGGER_QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.5f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.11f } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Box / Quantro Standard / Enter
								// White rimmed with the qPane color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QPANE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.11f } ;
								break ;
							}
							break ;
							
							
							
						case INDEX_RETRO_STANDARD:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Box / Retro Standard / Lock
								// A glow colored by the QPane we lock in.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0 } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Box / Retro Standard / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.2f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Box / Retro Standard / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Box / Retro Standard / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Box / Quantro Standard / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_STANDARD_CLEAN:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Box / Retro Clean / Lock
								// Light-color QO.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Box / Retro Clean / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.2f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Box / Retro Clean / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Box / Retro Clean / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Box / Quantro Clean / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_STANDARD_AUSTERE:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Box / Retro Clean / Lock
								// Light-color QO.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Box / Retro Clean / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.2f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Box / Retro Clean / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Box / Retro Clean / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Box / Quantro Clean / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
							}
							break ;
							
							
						case INDEX_RETRO_STANDARD_SEVERE:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Box / Retro Severe / Lock
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Box / Retro Severe / Clear
								// Black
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { -1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.2f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Box / Retro Severe / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Box / Retro Severe / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Box / Quantro Severe / Enter
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
							}
							break ;
							
						case INDEX_RETRO_NEON:
						case INDEX_RETRO_LIMBO:
							switch( glow ) {
							case Consts.GLOW_LOCK:
								// Box / Retro NE0N / Lock
								// Lighened QO.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
								
							case Consts.GLOW_CLEAR:
								// Box / Retro NE0N / Clear
								// White
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.WHITE } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 1.0f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.2f } ;
								break ;
								
							
							case Consts.GLOW_METAMORPHOSIS:
								// Box / Retro NE0N / Metamorphosis
								// No metamorphosis.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_UNLOCK:
								// Box / Retro NE0N / Unlock
								// No unlocks.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { } ;
								break ;
								
							case Consts.GLOW_ENTER:
								// Box / Quantro NE0N / Enter
								// Whitened QO color.
								DEFAULT_SETTING_GLOW_COLOR[glowStyle][index][glow] = new GlowColor[] { GlowColor.QO } ;
								DEFAULT_SETTING_GLOW_LIGHTENING[glowStyle][index][glow] = new float[] { 0.8f } ;
								DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowStyle][index][glow] = new float[] { 1.3f } ;
								DEFAULT_SETTING_GLOW_RADIUS[glowStyle][index][glow] = new float[] { 0.1f } ;
								break ;
							}
							break ;
						}
						break ;
					}
				}
			}
		}
	}
	
	
	
	/**
	 * Indexed by ColorScheme.ShineShape.*.ordinal(), CORNER_INNER_*.
	 * Provides a scalar proportion between the shine's minimum and
	 * maximum alpha: for 0, the mininum alpha is used.  For 1, the 
	 * maximum alpha.
	 */
	private static float [][] SHINE_PROPORTION_BY_SHAPE_AND_CORNER ;
	
	/**
	 * Indexed by ColorScheme.ShineShape.*.ordinal(), CORNER_INNER_*,
	 * CORNER_INNER_*.  If non-null, provides the stepwise scalar proportion
	 * for a shine gradient from the first corner to the second.  The
	 * proportion is applied to ColorScheme's alpha range; 0 indicates
	 * minimum alpha; 1 is maximum.
	 * 
	 */
	private static float [][][][] SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER ;
	
	/**
	 * Corresponds to SHINE_PROPORTION.  Whereas SHINE_PROPORTION gives the
	 * alpha scalar for the border shine, this gives the relative position
	 * along the side where that color occurs.
	 */
	private static float [][][][] SHINE_POSITION_BY_SHAPE_CORNER_CORNER ;
	
	static {
		ColorScheme.ShineShape [] shineShapes = ColorScheme.ShineShape.values() ;
		
		SHINE_PROPORTION_BY_SHAPE_AND_CORNER = new float[shineShapes.length][NUM_CORNERS] ;
		SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER = new float[shineShapes.length][NUM_CORNERS][NUM_CORNERS][] ;
		SHINE_POSITION_BY_SHAPE_CORNER_CORNER = new float[shineShapes.length][NUM_CORNERS][NUM_CORNERS][] ;
		
		for ( int i = 0; i < shineShapes.length; i++ ) {
			ColorScheme.ShineShape shineShape = shineShapes[i] ;
			int ss = shineShape.ordinal() ;
			float [] cornerProportions = null ;
			float [][] sideProportions = null ;
			float [][] sidePositions = null ;
			
			switch( shineShape ) {
			case FULL_WRAP_TOP_LEFT:
			case FULL_WRAP_TOP_RIGHT:
			case FULL_WRAP_BOTTOM_LEFT:
			case FULL_WRAP_BOTTOM_RIGHT:
				// Full Wrap:  1, 0.66, 0.33, 0.
				// Wrap has max value at the specified corner, lowest at the opposite,
				// and matches high values / low values on the same top/bottom wall.
				// In other words, left and right walls have larger alpha changes.
				if ( shineShape == ColorScheme.ShineShape.FULL_WRAP_TOP_LEFT )
					cornerProportions = new float[] { 1.0f, 0.667f, 0.333f, 0.0f } ;
				else if ( shineShape == ColorScheme.ShineShape.FULL_WRAP_TOP_RIGHT )
					cornerProportions = new float[] { 0.667f, 1.0f, 0.0f, 0.333f } ;
				else if ( shineShape == ColorScheme.ShineShape.FULL_WRAP_BOTTOM_LEFT )
					cornerProportions = new float[] { 0.333f, 0.0f, 1.0f, 0.667f } ;
				else if ( shineShape == ColorScheme.ShineShape.FULL_WRAP_BOTTOM_RIGHT )
					cornerProportions = new float[] { 0.0f, 0.333f, 0.667f, 1.0f } ;
				setCornerProportions( SHINE_PROPORTION_BY_SHAPE_AND_CORNER[ss], cornerProportions ) ;
				// all corners use equidistant measurements.
				setCornerToCornerProportionsEquidistant(
						SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[ss], cornerProportions ) ;
				// leave shine positions 'null', as these are equidistant.
				break ;
				
			case HALF_WRAP_TOP_LEFT:
			case HALF_WRAP_TOP_RIGHT:
			case HALF_WRAP_BOTTOM_LEFT:
			case HALF_WRAP_BOTTOM_RIGHT:
				// 1, 0.5, -0.5, -1
				// Wrap has max value at specified corner, lowest at the opposite,
				// and puts the maximum alpha at the same height as the other 
				// nonzero.
				// We want scores to reach 0 more quickly along certain edges.
				// We accomplish this using NEGATIVE values; the moment it crosses 0 is
				// noted as a significant point along the curve.  Entries are lower-bounded by 0.
				//
				// Formula: corners a, b, c, d		// tl, tr, bl, br
				//	a = 1			// max alpha in top-left
				//	b + c = 0		// transition halfway between these two corners
				//	(2/3)b + (1/3)d = 0		// transition 1/3 of the way down the right side
				//  (1/3)a + (2/3)c = 0		// transition 2/3 of the way down the left side
				// Solved at: 
				//  a = 1
				//  b = 1/2
				//  c = -1/2
				//  d = -1
				if ( shineShape == ColorScheme.ShineShape.HALF_WRAP_TOP_LEFT )
					cornerProportions = new float[] { 1.0f, 0.5f, -0.5f, -1.0f } ;
				else if ( shineShape == ColorScheme.ShineShape.HALF_WRAP_TOP_RIGHT )
					cornerProportions = new float[] { 0.5f, 1.0f, -1.0f, -0.5f } ;
				else if ( shineShape == ColorScheme.ShineShape.HALF_WRAP_BOTTOM_LEFT )
					cornerProportions = new float[] { -0.5f, -1.0f, 1.0f, 0.5f } ;
				else if ( shineShape == ColorScheme.ShineShape.HALF_WRAP_BOTTOM_RIGHT )
					cornerProportions = new float[] { -1.0f, -0.5f, 0.5f, 1.0f } ;
				setCornerProportions( SHINE_PROPORTION_BY_SHAPE_AND_CORNER[ss], cornerProportions ) ;
				setCornerToCornerWithZeroOneTruncation(
						SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[ss],
						SHINE_POSITION_BY_SHAPE_CORNER_CORNER[ss],
						cornerProportions ) ;
				break ;
				
			case CAP_TOP_LEFT_LEANING:
			case CAP_TOP_RIGHT_LEANING:
			case CAP_BOTTOM_LEFT_LEANING:
			case CAP_BOTTOM_RIGHT_LEANING:
				// 1, 1, -2, -3
				// Cap covers one side completely (1 to 1), then fades to
				// min opacity less than 1/2 of the way down both adjacent
				// sides.  It favors one side (leaning).
				//
				// The favored side reaches min opacity 1/3 of the way down;
				// the other side reaches min 1/3 of the way down.
				if ( shineShape == ColorScheme.ShineShape.CAP_TOP_LEFT_LEANING )
					cornerProportions = new float[] { 1.0f, 1.0f, -2.0f, -3.0f } ;
				else if ( shineShape == ColorScheme.ShineShape.CAP_TOP_RIGHT_LEANING )
					cornerProportions = new float[] { 1.0f, 1.0f, -3.0f, -2.0f } ;
				else if ( shineShape == ColorScheme.ShineShape.CAP_BOTTOM_LEFT_LEANING )
					cornerProportions = new float[] { -2.0f, -3.0f, 1.0f, 1.0f } ;
				else if ( shineShape == ColorScheme.ShineShape.CAP_BOTTOM_RIGHT_LEANING )
					cornerProportions = new float[] { -3.0f, -2.0f, 1.0f, 1.0f } ;
				
				setCornerProportions( SHINE_PROPORTION_BY_SHAPE_AND_CORNER[ss], cornerProportions ) ;
				setCornerToCornerWithZeroOneTruncation(
						SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[ss],
						SHINE_POSITION_BY_SHAPE_CORNER_CORNER[ss],
						cornerProportions ) ;
				break ;
				
				
			case SPECULAR_LEFT_SIDE_LOW:
			case SPECULAR_RIGHT_SIDE_HIGH:
				// Specular shines are dedicated to a particular side, and all
				// possible corner combinations for that side.  In some cases
				// the shine proceeds beyond a corner and to an adjacent side,
				// but this behavior can be described in terms of corner-alphas.
				if ( shineShape == ColorScheme.ShineShape.SPECULAR_LEFT_SIDE_LOW ) {
					// Specular Left Side Low
					// Only the left side gets explicit shine alphas.  All other
					// sides use smooth corner transitions (basically only the
					// bottom does this).
					// Left, Top, Right, Bottom, same as Rect objects.
					sideProportions = new float[4][] ;
					sidePositions = new float[4][] ;
					sideProportions[0] = new float[] { 1, 1 } ;
					sidePositions[0] = new float[] { 0.6f, 0.8f } ;
					// The left side has a bright spot from 3/5 to 4/5 of the way down.
					
					// Corner: all corners have an alpha of zero, with two exceptions.
					// LEFT_BOTTOM_CONVEX has an alpha of 2/3rds.  Both RIGHT_BOTTOM_CONVEX
					// and LEFT_BOTTOM_CONCAVE, which are the options for the "right side
					// corner" of the bottom side, have an alpha of -1 to allow the
					// LEFT_BOTTOM_CONVEX alpha to proceed 2/5ths of the way across the side.
					cornerProportions = new float[NUM_CORNERS] ;
					cornerProportions[CORNER_INNER_LEFT_BOTTOM_CONVEX] = 0.6667f ;
					cornerProportions[CORNER_INNER_RIGHT_BOTTOM_CONVEX] = -1.0f ;
					cornerProportions[CORNER_INNER_LEFT_BOTTOM_CONCAVE] = -1.0f ;
				} else if ( shineShape == ColorScheme.ShineShape.SPECULAR_RIGHT_SIDE_HIGH ) {
					// Specular Right Side High
					// Only the right side gets explicit shine alphas.  All other
					// sides use smooth corner transitions (basically only the
					// top does this).
					// Left, Top, Right, Bottom, same as Rect objects.
					sideProportions = new float[4][] ;
					sidePositions = new float[4][] ;
					sideProportions[2] = new float[] { 1, 1 } ;
					sidePositions[2] = new float[] { 0.2f, 0.4f } ;
					// The right side has a bright spot from 1/5 to 2/5 of the way down.
					
					// Corner: all corners have an alpha of zero, with two exceptions.
					// RIGHT_TOP_CONVEX has an alpha of 2/3rds.  Both LEFT_TOP_CONVEX
					// and RIGHT_TOP_CONCAVE, which are the options for the "left side
					// corner" of the top side, have an alpha of -1 to allow the
					// RIGHT_TOP_CONVEX alpha to proceed 2/5ths of the way across the side.
					cornerProportions = new float[NUM_CORNERS] ;
					cornerProportions[CORNER_INNER_RIGHT_TOP_CONVEX] = 0.6667f ;
					cornerProportions[CORNER_INNER_LEFT_TOP_CONVEX] = -1.0f ;
					cornerProportions[CORNER_INNER_RIGHT_TOP_CONCAVE] = -1.0f ;
				}
				
				// apply these values into our specular collection.
				setCornerProportions( SHINE_PROPORTION_BY_SHAPE_AND_CORNER[ss], cornerProportions ) ;
				setCornerToCornerWithZeroOneTruncation(
						SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[ss],
						SHINE_POSITION_BY_SHAPE_CORNER_CORNER[ss],
						cornerProportions ) ;
				insertSideShines( 
						SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[ss],
						SHINE_POSITION_BY_SHAPE_CORNER_CORNER[ss],
						sideProportions, sidePositions ) ;
				
				break ;
				
				
			case SPECULAR_SPOT_TOP_SIDE_RIGHT:
			case SPECULAR_SPOT_BOTTOM_SIDE_LEFT:
				if ( shineShape == ColorScheme.ShineShape.SPECULAR_SPOT_TOP_SIDE_RIGHT ) {
					// Specular Spot top side right.  No corner alphas.  We explicitly
					// label the side alphas at 0, 1, 1, and 0.  This shine appears on
					// the top edge, towards the right.
					sideProportions = new float[4][] ;
					sidePositions = new float[4][] ;
					sideProportions[1] = new float[] { 0, 1, 1, 0 } ;
					sidePositions[1] = new float[] { 0.4f, 0.6f, 0.8f, 0.99999f } ;
					
					cornerProportions = new float[NUM_CORNERS] ;	// all zeroes
				} else if ( shineShape == ColorScheme.ShineShape.SPECULAR_SPOT_BOTTOM_SIDE_LEFT ) {
					// Specular Spot top side right.  No corner alphas.  We explicitly
					// label the side alphas at 0, 1, 1, and 0.  This shine appears on
					// the bottom edge, towards the left.
					sideProportions = new float[4][] ;
					sidePositions = new float[4][] ;
					sideProportions[3] = new float[] { 0, 1, 1, 0 } ;
					sidePositions[3] = new float[] { 0.000001f, 0.2f, 0.4f, 0.6f } ;
					
					cornerProportions = new float[NUM_CORNERS] ;	// all zeroes
				}
				
				// apply these values into our specular collection.
				setCornerProportions( SHINE_PROPORTION_BY_SHAPE_AND_CORNER[ss], cornerProportions ) ;
				setCornerToCornerWithZeroOneTruncation(
						SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[ss],
						SHINE_POSITION_BY_SHAPE_CORNER_CORNER[ss],
						cornerProportions ) ;
				insertSideShines( 
						SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[ss],
						SHINE_POSITION_BY_SHAPE_CORNER_CORNER[ss],
						sideProportions, sidePositions ) ;
				
				break ;
			}
		}
	}
	
	/**
	 * Provided with a 'proportions,' a length-4 array ordered as
	 * 'left_top', 'right_top', 'left_bottom', 'right_bottom,', sets
	 * the appropriate indices in 'corners', which has length NUM_CORNERS.
	 * 
	 * @param corners
	 * @param proportions
	 */
	static void setCornerProportions( float [] corners, float [] proportions ) {
		if ( proportions.length == 4 ) {
			// 'Proportions' is used as a 4-element corner set:
			// top-left, top-right, bottom-left, bottom-right.
			for ( byte i = 0; i < NUM_CORNERS; i++ ) {
				corners[i] = Math.min( 1, Math.max( 0, proportions[convexCorner(i)] ) ) ;
			}
		} else if ( proportions.length == NUM_CORNERS ) {
			for ( int i = 0; i < NUM_CORNERS; i++ ) {
				corners[i] = Math.min( 1, Math.max( 0, proportions[i] ) ) ;
			}
		} else {
			throw new IllegalArgumentException("Don't know what to do with proportions of length " + proportions.length ) ;
		}
	}
	
	
	static void setCornerToCornerProportionsEquidistant( float [][][] cornerToCorner, float [] proportions ) {
		for ( byte c0 = 0; c0 < NUM_CORNERS; c0++ ) {
			for ( byte c1 = 0; c1 < NUM_CORNERS; c1++ ) {
				cornerToCorner[c0][c1] = new float[] {
						Math.min( 1, Math.max( 0, proportions[ convexCorner(c0) ] ) ),
						Math.min( 1, Math.max( 0, proportions[ convexCorner(c1) ] ) ) } ;
			}
		}
	}

	static void setCornerToCornerWithZeroOneTruncation(
			float [][][] cornerToCornerProportion, float [][][] cornerToCornerPosition, float [] proportions ) {
		
		boolean convexInput = proportions.length == 4 ;
		
		float p0, p1 ;
		
		for ( byte c0 = 0; c0 < NUM_CORNERS; c0++ ) {
			int corner0 = convexInput ? convexCorner(c0) : c0 ;
			boolean nonNeg0 = corner0 >= 0 ;
			boolean nonPos0 = corner0 <= 0 ;
			
			boolean fullAbove0 = corner0 >= 1.0f ;
			boolean fullBelow0 = corner0 <= 1.0f ;
			for ( byte c1 = 0; c1 < NUM_CORNERS; c1++ ) {
				int corner1 = convexInput ? convexCorner(c1) : c1 ;
				boolean nonNeg1 = corner1 >= 0 ;
				boolean nonPos1 = corner1 <= 0 ;
				
				boolean fullAbove1 = corner1 >= 1.0f ;
				boolean fullBelow1 = corner1 <= 1.0f ;
				
				// if both corners are nonnegative, or both are negative,
				// there is no zero transition and we use equidistant.
				// That means a 2-element array and no explicit 'Position.'
				if (  ( (nonNeg0 && nonNeg1) || (nonPos0 && nonPos1) )
						&& ( (fullAbove0 && fullAbove1) || (fullBelow0 && fullBelow1) ) ) {
					cornerToCornerProportion[c0][c1] = new float[] {
							Math.max( 0, proportions[ corner0 ] ),
							Math.max( 0, proportions[ corner1 ] ) } ;
				} else {
					// At least one transition occurs.  Find them at p0, the
					// transition between negative and positive, and p1, the
					// transition between full and non-full.
					
					// p0 is the point where c0 + p0 * ( c1 - c0 ) = 0.
					if ( (nonNeg0 && nonNeg1) || (nonPos0 && nonPos1) )
						p0 = -proportions[corner0] / (proportions[corner1] - proportions[corner0]) ;
					else 
						p0 = -1 ;
					
					// p1 is the point where c0 + p * ( c1 - c0 ) = 1.
					if ( (fullAbove0 && fullAbove1) || (fullBelow0 && fullBelow1) )
						p1 = ( 1 - proportions[corner0] ) / (proportions[corner1] - proportions[corner0]) ;
					else
						p1 = -1 ;
					
					// put these in the right order...
					float [] gradProps ;
					float [] gradPos ;
					if ( p0 > -1 && p1 > -1 ) {
						// we cross both 0 and 1
						gradProps = new float[] {
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c0) ] ) ),
								p0 < p1 ? 0 : 1,
								p0 < p1 ? 1 : 0,
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c1) ] ) )
						} ;
						gradPos = new float[] {
								0,
								p0 < p1 ? p0 : p1,
								p0 < p1 ? p1 : p0,
								1
						} ;
					} else if ( p0 > -1 ) {
						// we cross only 0
						gradProps = new float[] {
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c0) ] ) ),
								0,
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c1) ] ) )
						} ;
						gradPos = new float[] { 0, p0, 1 } ;
					} else if ( p1 > -1 ) {
						// we cross only 1
						gradProps = new float[] {
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c0) ] ) ),
								1,
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c1) ] ) )
						} ;
						gradPos = new float[] { 0, p1, 1 } ;
					} else {
						// This should never happen, but just in case...
						gradProps = new float[] {
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c0) ] ) ),
								Math.min( 1, Math.max( 0, proportions[ convexCorner(c1) ] ) )
						} ;
						gradPos = new float[] { 0, 1 } ;
					}
					
					cornerToCornerProportion[c0][c1] = gradProps ;
					cornerToCornerPosition[c0][c1] = gradPos ;
				}
			}
		}
	}
	
	
	/**
	 * Given an existing set of corner-to-corner proportions, we insert
	 * the specified side shines wherever that side appears.  This
	 * insert does not overwrite existing colors, but instead is placed
	 * among them in the right order.  Behavior is unspecified if
	 * 'sidePosition' indicates a space that is currently occupied.
	 * 
	 * One cavaet: sideProportion is listed by side: left, top, right, bottom.
	 * Left and right sides flow DOWNWARDS in this listing: position 0.2 is,
	 * for example, 1/5th of the way DOWN the side (and thus 4/5ths of the way UP).
	 * 
	 * However, 'cornerToCorner' runs in both directions (just flip the corner indices).
	 * That means that, for some corner combinations, we need to reverse the
	 * side proportion orders when inserting them (and that means taking 1 minus
	 * the side position to get the new position).
	 * 
	 * @param cornerToCornerProportion
	 * @param cornerToCornerPosition
	 * @param sideProportion
	 * @param sidePosition
	 */
	static void insertSideShines(
			float [][][] cornerToCornerProportion, float [][][] cornerToCornerPosition, 
			float [][] sideProportion, float [][] sidePosition ) {
		
		for ( byte c0 = 0; c0 < NUM_CORNERS; c0++ ) {
			for ( byte c1 = 0; c1 < NUM_CORNERS; c1++ ) {
				// wall from c0 to c1.  Determine the wall.
				// First: convert to convex corners, for convenience.
				int convexC0 = convexCorner( c0 ) ;
				int convexC1 = convexCorner( c1 ) ;
				
				int wall = -1 ;
				boolean reverseDirection = false ;
				if ( convexC0 == CORNER_INNER_LEFT_TOP_CONVEX ) {
					// must go normal direction
					if ( convexC1 == CORNER_INNER_RIGHT_TOP_CONVEX )
						wall = 1 ;		// top
					else if ( convexC1 == CORNER_INNER_LEFT_BOTTOM_CONVEX )
						wall = 0 ;		// left
				} else if ( convexC0 == CORNER_INNER_RIGHT_TOP_CONVEX ) {
					if ( convexC1 == CORNER_INNER_LEFT_TOP_CONVEX ) {
						wall = 1 ;		// top
						reverseDirection = true ;		// right-to-left
					} else if ( convexC1 == CORNER_INNER_RIGHT_BOTTOM_CONVEX )
						wall = 2 ;		// right
				} else if ( convexC0 == CORNER_INNER_LEFT_BOTTOM_CONVEX ) {
					if ( convexC1 == CORNER_INNER_LEFT_TOP_CONVEX ) {
						wall = 0 ;		// left
						reverseDirection = true ;		// bottom-to-top
					} else if ( convexC1 == CORNER_INNER_RIGHT_BOTTOM_CONVEX )
						wall = 3 ;		// bottom
				} else if ( convexC0 == CORNER_INNER_RIGHT_BOTTOM_CONVEX ) {
					if ( convexC1 == CORNER_INNER_LEFT_BOTTOM_CONVEX ) {
						wall = 3 ;		// bottom
						reverseDirection = true ;		// right-to-left
					} else if ( convexC1 == CORNER_INNER_RIGHT_TOP_CONVEX ) {
						wall = 2 ; 		// right
						reverseDirection = true ;		// top-to-bottom
					}
				}
				
				if ( wall == -1 )
					continue ;
				
				if ( sideProportion[wall] == null || sideProportion[wall].length == 0 )
					continue ;
				
				float [] prop, pos ;
				if ( reverseDirection ) {
					prop = new float[sideProportion[wall].length] ;
					pos = new float[sidePosition[wall].length] ;
					for ( int i = 0; i < prop.length; i++ ) {
						prop[i] = sideProportion[wall][ prop.length - i - 1 ] ;
						pos[i] = 1 - sidePosition[wall][ pos.length - i - 1 ] ;
					}
				} else {
					prop = sideProportion[wall] ;
					pos = sidePosition[wall] ;
				}
				// don't alter the content of 'prop' or 'pos;' they MIGHT be direct
				// references to input parameters.  However, we don't need to worry
				// about reversed directions anymore; we can insert directly.
				
				float [] oldProp, oldPos ;
				oldProp = cornerToCornerProportion[c0][c1] ;
				if ( cornerToCornerPosition[c0][c1] == null ) {
					oldPos = new float[] { 0, 1 } ;
				} else {
					oldPos = cornerToCornerPosition[c0][c1] ;
				}
				// don't alter the content of 'oldProp' or 'oldPos:' they MIGHT
				// be direct references to input parameters.
				
				// Now insert directly.
				float [] newProp = new float[oldProp.length + prop.length] ;
				float [] newPos = new float[oldPos.length + pos.length] ;
				// these should be the same length.  Do a merge sort based
				// on POSITION values.
				int index = 0, oldIndex = 0 ;
				for ( int i = 0; i < newProp.length; i++ ) {
					if ( index < pos.length && ( oldIndex >= oldPos.length || pos[index] <= oldPos[oldIndex] ) ) {
						// current pos occurs BEFORE the current old position.  Use it and advance.
						newProp[i] = prop[index] ;
						newPos[i] = pos[index] ;
						index++ ;
					} else {
						// otherwise, use the old value and advance it.
						newProp[i] = oldProp[oldIndex] ;
						newPos[i] = oldPos[oldIndex] ;
						oldIndex++ ;
					}
				}
				
				// stick this into the array.
				cornerToCornerProportion[c0][c1] = newProp ;
				cornerToCornerPosition[c0][c1] = newPos ;
			}
		}
	}
	
	
	private static final byte convexCorner( byte corner ) {
		switch( corner ) {
		case CORNER_INNER_LEFT_TOP_CONCAVE:
			return CORNER_INNER_RIGHT_BOTTOM_CONVEX ;
		case CORNER_INNER_RIGHT_TOP_CONCAVE:
			return CORNER_INNER_LEFT_BOTTOM_CONVEX ;
		case CORNER_INNER_LEFT_BOTTOM_CONCAVE:
			return CORNER_INNER_RIGHT_TOP_CONVEX ;
		case CORNER_INNER_RIGHT_BOTTOM_CONCAVE:
			return CORNER_INNER_LEFT_TOP_CONVEX ;
		}
		return corner ;
	}
	
	
	public DrawSettings(
			Rect drawRegion,
			int ROWS, int COLS, int rowsToFit,
			int drawDetail, int animationDetail,
			Skin skin, Context context ) {
		
		this( drawRegion, false,
			ROWS, COLS, rowsToFit,
			drawDetail, animationDetail,
			skin, context ) ;
	}
	
	/**
	 * A constructor that uses default settings for proportion 
	 * and sizes.
	 */
	public DrawSettings(
			Rect drawRegion, boolean horizontalFlush,
			int ROWS, int COLS, int rowsToFit,
			int drawDetail, int animationDetail,
			Skin skin, Context context ) {
		this.width = drawRegion.width() ;
		this.height = drawRegion.height() ;
		this.ROWS = ROWS ;
		this.COLS = COLS ;
		this.rowsToFit = rowsToFit ;
		
		// 3D draw?
		if ( skin.getGame() == Skin.Game.QUANTRO ) {
			this.drawQ0 = true ;
			this.drawQ1 = true ;
			this.draw3D = true ;
			this.qo = new byte[]{
					QOrientations.S0,
					QOrientations.S1,
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
					QOrientations.S0_FROM_SL,
					QOrientations.S1_FROM_SL
				} ;
		}
		else if ( skin.getGame() == Skin.Game.RETRO ) {
			this.drawQ0 = true ;
			this.drawQ1 = false ;
			this.draw3D = false ;
			this.qo = new byte[]{
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
					QOrientations.PUSH_UP_ACTIVE
				} ;
		}
		
		veil_qo = null ;
		veil_qo_main = 0 ;
		
		this.drawDetail = drawDetail ;
		this.drawAnimations = animationDetail ;
		this.drawEffects = DRAW_EFFECTS_IN_SLICE ;
		this.drawColorblindHelp = skin.getTemplate() == Skin.Template.COLORBLIND ;
		this.mSkin = skin ;
		this.mColorScheme = new ColorScheme(context, skin) ;
		this.blit = BLIT_FULL ;
		this.scale = 1 ;
		this.loadImagesSize = IMAGES_SIZE_SMALL ;
		this.loadBackgroundSize = IMAGES_SIZE_SMALL ;
		
		// Make the standard canvas config.  Our default is 'BLIT_FULL.'
		this.configCanvas = new BlockDrawerConfigCanvas( drawRegion ) ;
		
		// by default there is no displacement, and thus no margin.
		this.displacementSafeMarginOffsetDraw = 0 ;
		this.displacementSafeMarginOffsetBlit = 0 ;
		
		if ( drawDetail == DRAW_DETAIL_LOW )
			glowDrawStyle = GLOW_DRAW_STYLE_BOX ;
		else if ( drawDetail == DRAW_DETAIL_MID )
			glowDrawStyle = GLOW_DRAW_STYLE_FADE ;
		else if ( drawDetail == DRAW_DETAIL_HIGH )
			glowDrawStyle = GLOW_DRAW_STYLE_FLARE ;
		
		
		// Default settings for proportions, block sizes, etc.
		squareBlocks = true ;
		
		Resources res = context.getResources() ;
		
		this.setDefaultBehaviors() ;
		
		this.setDefaultAlphas() ;
		this.setDefaultProportions() ;
		
		this.setDefaultGlows() ;
		
		this.setDefaultColors(res) ;
		
		this.setBlockSizesToFit(horizontalFlush) ;
		// this is automatically called this.setSizesFromProportions() ;
		
		this.setDefaultPaints(res) ;
		this.setDefaultAlphaMults() ;
		
		this.applySimulatedColorblindness( SIMULATED_COLORBLINDNESS ) ;
	}
	
	
	public DrawSettings ( DrawSettings ds ) {
		drawDetail = ds.drawDetail ;
		drawAnimations = ds.drawAnimations ;
		drawEffects = ds.drawEffects ;
		drawColorblindHelp = ds.drawColorblindHelp ;
		mSkin = ds.mSkin ;
		mColorScheme = ds.mColorScheme ;
		blit = ds.blit ;
		scale = ds.scale ;
		loadImagesSize = ds.loadImagesSize ;
		loadBackgroundSize = ds.loadBackgroundSize ;
		glowDrawStyle = ds.glowDrawStyle ;
		
		configCanvas = new BlockDrawerConfigCanvas( ds.configCanvas ) ;
		configBlocks = new BlockDrawerConfigBlockGrid( ds.configBlocks ) ;
		
		displacementSafeMarginOffsetDraw = ds.displacementSafeMarginOffsetDraw ;
		displacementSafeMarginOffsetBlit = ds.displacementSafeMarginOffsetBlit ;
		
		// qo_qps?
		qo = copy ( ds.qo ) ;
		
		// veil?
		veil_qo = copy( ds.veil_qo ) ;
		veil_qo_main = ds.veil_qo_main ;
		
		// Game meta-info
		ROWS = ds.ROWS ;
		COLS = ds.COLS ;
		rowsToFit = ds.rowsToFit ;
		displayedRows = ds.displayedRows ;
		blockFieldOuterBuffer = ds.blockFieldOuterBuffer ;
		
		width = ds.width ;
		height = ds.height ;
		drawQ0 = ds.drawQ0 ;
		drawQ1 = ds.drawQ1 ;
		draw3D = ds.draw3D ;
		
		horizontalMargin = ds.horizontalMargin ;
		verticalMargin = ds.verticalMargin ;
		
		squareBlocks = ds.squareBlocks ;
		
		// EXACT REPRESENTATION
		setSize_blockWidth(ds.getSize_blockWidth()) ;
		setSize_blockHeight(ds.getSize_blockHeight()) ;
		
		setSize_qXOffset(ds.getSize_qXOffset()) ;
		setSize_qYOffset(ds.getSize_qYOffset()) ;
		
		size_borderWidth = ds.size_borderWidth ;					// Width of border
		size_borderHeight = ds.size_borderHeight ;				// Height of border
		
		size_smallPrismWidth = ds.size_smallPrismWidth ;			// X dimension of small rectangular prisms
		size_smallPrismHeight = ds.size_smallPrismHeight ;			// Y dimension
		
		size_innerBorderWidth = ds.size_innerBorderWidth ;
		size_innerBorderHeight = ds.size_innerBorderHeight ;
		
		size_innerBorderXInset = ds.size_innerBorderXInset ;		// Horiz. distance from block edge to inner border
		size_innerBorderYInset = ds.size_innerBorderYInset ;		// Vert. distance
		
		size_miniBorderWidth = ds.size_miniBorderWidth ;
		size_miniBorderHeight = ds.size_miniBorderHeight ;
		
		size_miniBorderXInset = ds.size_miniBorderXInset ;
		size_miniBorderYInset = ds.size_miniBorderYInset ;
		
		size_hashingLineWidth = copy( ds.size_hashingLineWidth ) ;
		size_hashingLineSpacing = copy( ds.size_hashingLineSpacing ) ;
		size_hashingLineShadowLayerRadius = copy( ds.size_hashingLineShadowLayerRadius ) ;
		
		size_innerShadowGaussianRadius = copy( ds.size_innerShadowGaussianRadius ) ;
		size_innerShadowXOffset = copy( ds.size_innerShadowXOffset ) ;
		size_innerShadowYOffset = copy( ds.size_innerShadowYOffset ) ;
		
		size_extrudedWallShadowGaussianRadius = ds.size_extrudedWallShadowGaussianRadius ;
		
		size_outerShadowGaussianRadius = copy( ds.size_outerShadowGaussianRadius ) ;
		size_outerShadowXOffset = copy( ds.size_outerShadowXOffset ) ;
		size_outerShadowYOffset = copy( ds.size_outerShadowYOffset ) ;
		
		// PROPRTIONAL REPRESENTATION
		proportion_qXOffset = ds.proportion_qXOffset ;		// qxOffset, as a proportion of block width.
		proportion_qYOffset = ds.proportion_qYOffset ;		// qyOffset, as a proportion of block width.
		
		proportion_borderWidth = ds.proportion_borderWidth ;	// Border width as a proportion of blockWidth
		proportion_borderHeight = ds.proportion_borderHeight ;	// Border height as a proportion of blockHeight
		
		proportion_smallPrismWidth = ds.proportion_smallPrismWidth ;	// Small prism width as a proportion of blockWidth
		proportion_smallPrismHeight = ds.proportion_smallPrismHeight ;	// Small prism height as a proportion of blockHeight
		
		proportion_innerBorderWidth = ds.proportion_innerBorderWidth ;	// Width of a border drawn within a square
		proportion_innerBorderHeight = ds.proportion_innerBorderHeight ;	// Height of that border
		
		proportion_innerBorderXInset = ds.proportion_innerBorderXInset ;	// Distance from left of block to left inner border, as a proportion of block width
		proportion_innerBorderYInset = ds.proportion_innerBorderYInset ;	// Distance from top of block to top inner border, as a proportion of block height

		proportion_miniBorderWidth = ds.proportion_miniBorderWidth ;
		proportion_miniBorderHeight = ds.proportion_miniBorderHeight ;
		
		proportion_miniBorderXInset = ds.proportion_miniBorderXInset ;
		proportion_miniBorderYInset = ds.proportion_miniBorderYInset ;
		
		proportion_hashingLineWidth = copy( ds.proportion_hashingLineWidth ) ;
		proportion_hashingLineSpacing = copy( ds.proportion_hashingLineSpacing ) ;
		proportion_hashingLineShadowLayerRadius = copy( ds.proportion_hashingLineShadowLayerRadius ) ;
		
		proportion_columnUnlockerExtension = ds.proportion_columnUnlockerExtension ;	// Proportional extension of the X coordinate of a block to the corner of the column-unlocker.
		proportion_columnUnlockerInset = ds.proportion_columnUnlockerInset ;		// Proportional inset of the X coordinate... these may be > 0, < 0, or 0.
		proportion_columnUnlockerHeight = ds.proportion_columnUnlockerHeight ;		// Proportional height of the column unlocker.
		
		proportion_innerShadowGaussianRadius = copy( ds.proportion_innerShadowGaussianRadius ) ;
		proportion_innerShadowXOffset = copy( ds.proportion_innerShadowXOffset ) ;
		proportion_innerShadowYOffset = copy( ds.proportion_innerShadowYOffset ) ;
		
		proportion_extrudedWallShadowGaussianRadius = ds.proportion_extrudedWallShadowGaussianRadius ;
		
		proportion_outerShadowGaussianRadius = copy( ds.proportion_outerShadowGaussianRadius ) ;
		proportion_outerShadowXOffset = copy( ds.proportion_outerShadowXOffset ) ;
		proportion_outerShadowYOffset = copy( ds.proportion_outerShadowYOffset ) ;
		
		// Draw alpha values.
		alpha_rowNegativeOneBorder = ds.alpha_rowNegativeOneBorder ;
		
		alpha_lockedFill = ds.alpha_lockedFill ;
		alpha_lockedBorder = ds.alpha_lockedBorder ;
		alpha_lockedTop = ds.alpha_lockedTop ;
		
		alpha_pieceFill = ds.alpha_pieceFill ;
		alpha_pieceBorder = ds.alpha_pieceBorder ;
		alpha_pieceTop = ds.alpha_pieceTop ;
		
		alpha_ghostFill = ds.alpha_ghostFill ;
		alpha_ghostBorder = ds.alpha_ghostBorder ;
		alpha_ghostTop = ds.alpha_ghostTop ;
		
		alpha_displacementFill = ds.alpha_displacementFill ;
		alpha_displacementBorder = ds.alpha_displacementBorder ;
		
		// shadows for MID draw quality
		alpha_innerShadow = copy( ds.alpha_innerShadow ) ;
		alpha_extrudedWallShadow = ds.alpha_extrudedWallShadow ;
		alpha_dropShadow = copy( ds.alpha_dropShadow ) ;
		
		// Pieces are emphasized before being cleared
		alpha_emphFill = ds.alpha_emphFill ;
		alpha_emphBorder = ds.alpha_emphBorder ;
		alpha_emphTop = ds.alpha_emphTop ;
		
		alphaScale_lockedFillPulse = copy( ds.alphaScale_lockedFillPulse ) ;
		alphaScale_pieceFillPulse = ds.alphaScale_pieceFillPulse ;
		
		alphaScale_lockedBoxPulse = copy( ds.alphaScale_lockedBoxPulse ) ;
		alphaScale_pieceBoxPulse = copy( ds.alphaScale_pieceBoxPulse ) ;
		
		// Paints!
		color_background_piece = ds.color_background_piece ;
		background = ds.background ;
		
		color_qOrientationQPaneFill = copy(ds.color_qOrientationQPaneFill) ; 
		color_qOrientationQPaneBorder = copy(ds.color_qOrientationQPaneBorder) ; 
		color_qOrientationQPaneFillShadow = copy(ds.color_qOrientationQPaneFillShadow) ;
		color_qOrientationQPaneDropShadow = copy(ds.color_qOrientationQPaneDropShadow) ;
		color_qPaneBottomGuide = copy(ds.color_qPaneBottomGuide) ;
		color_qPaneCornerBottomGuideShine = copy(ds.color_qPaneCornerBottomGuideShine) ;
		//color_qOrientationQPaneEdge = copy(ds.color_qOrientationQPaneEdge) ; 
		//color_qOrientationUnlockRing = copy(ds.color_qOrientationUnlockRing) ; 
		
		colorSimplified_qOrientationQPaneFill = copy(ds.colorSimplified_qOrientationQPaneFill) ;
		colorSimplified_qOrientationQPaneBorder = copy(ds.colorSimplified_qOrientationQPaneBorder) ;
		
		color_prismSides = ds.color_prismSides ;
		color_prismEdges = ds.color_prismEdges ;
		
		// Alpha multipliers!
		alphamult_qOrientationFill = copy(ds.alphamult_qOrientationFill) ;
		alphamult_qOrientationBorder = copy(ds.alphamult_qOrientationBorder) ;
		alphamult_qOrientationEdge = copy(ds.alphamult_qOrientationEdge) ;
		
		// Glows
		glow_color = copy( ds.glow_color ) ;
		glow_lightenBy = copy( ds.glow_lightenBy ) ;
		glow_alphaPeakNormalized = copy( ds.glow_alphaPeakNormalized ) ;
		glow_radius = copy( ds.glow_radius ) ;
		glow_size = copy( ds.glow_size ) ;
		
		// Colors!
		color_qOrientationQPaneCornerBorderShine = new int[QOrientations.NUM][2][] ;
		for ( int i = 0; i < QOrientations.NUM; i++ )
			for ( int qp = 0; qp < 2; qp++ )
				color_qOrientationQPaneCornerBorderShine[i][qp] = copy( ds.color_qOrientationQPaneCornerBorderShine[i][qp] ) ;
		color_qOrientationQPaneGlowColor = copy( ds.color_qOrientationQPaneGlowColor ) ;
		color_qOrientationDetailColors = copy( ds.color_qOrientationDetailColors ) ;
		
		// hashing?
		hashingVector_qPane = copy( ds.hashingVector_qPane ) ;
		
		// Drawing behavior?
		behavior_background = ds.behavior_background ;
		
		behavior_border = ds.behavior_border ;
		
		behavior_hashing = ds.behavior_hashing ;
		
		behavior_align_vertical = ds.behavior_align_vertical ;
		behavior_align_horizontal = ds.behavior_align_horizontal ;
		
		behavior_clear_emph = ds.behavior_clear_emph ;
		
		behavior_displacement = ds.behavior_displacement ;
		
		// Painting behavior?
		behavior_qo_fill = copy(ds.behavior_qo_fill);		// Each entry is one of the above.
		behavior_qo_hashing = copy(ds.behavior_qo_hashing); 
		behavior_qo_3d = copy(ds.behavior_qo_3d) ;			// Each entry is one of the above.
		behavior_qo_top = copy(ds.behavior_qo_top) ;
		behavior_qo_border = copy(ds.behavior_qo_border) ;
		behavior_qo_border_shine = copy(ds.behavior_qo_border_shine) ;
		behavior_qo_lock_glow = copy(ds.behavior_qo_lock_glow) ;
		behavior_qo_pulse = copy(ds.behavior_qo_pulse) ;
		behavior_qo_metamorphosis_from = copy(ds.behavior_qo_metamorphosis_from) ;
		behavior_qo_metamorphosis_to = copy(ds.behavior_qo_metamorphosis_to) ;
		behavior_qo_falling_chunk = copy(ds.behavior_qo_falling_chunk) ; 
		behavior_qo_bitmap = copy(ds.behavior_qo_bitmap) ;
	}
	
	
	
	/**
	 * Constructs a DrawSettings object for imposing a "Veil" - a static structure designed
	 * to keep visual consistency but serve as a "image wall" for e.g. a game loss - 
	 * over the specified draw region.  The new object will extract some important settings,
	 * such as block colors, from this object.  Other settings (like certain Alpha values)
	 * will come from standard default settings.
	 * 
	 * Additionally, the field veil_qo is an array of "recommended" (i.e., "required")
	 * QOrientations to use in constructing a veil.  We don't guarantee correct draws
	 * for any QOrientation that does not appear.
	 * 
	 * Finally, although this DrawSettings object should function for any veil we would
	 * wish to draw using the provided veil_qo, we do NOT guarantee that the result is
	 * exactly equivalent to drawing those blocks with the provided drawSettings object.
	 * Specifically, because Quantro blocks are designed to be drawn in offset panes
	 * but the veil will hopefully occlude both, we draw them using a different art
	 * style.  We don't even guarantee that the same QCs will be used in Quantro and
	 * Retro, only that the result of drawing them will look visually consistent with
	 * standard draws.
	 *
	 * @return
	 */
	public DrawSettings makeVeil( Rect blitRegion, int ROWS, int COLS ) {
		
		// first: make a new draw settings object which copies from this one.
		DrawSettings ds = new DrawSettings( this ) ;
		
		// resize to fit the specified drawRegion and rows/cols.
		// we draw with only 1 qpane, so set the offset proportions to zero.
		ds.width = blitRegion.width() / scale ;
		ds.height = blitRegion.height() / scale ;
		ds.ROWS = ROWS ;
		ds.COLS = COLS ;
		ds.rowsToFit = 0 ;	// with no required rows, we can expand horizontally as much as we want.
		ds.proportion_qXOffset = ds.proportion_qYOffset = 0 ;
		ds.setBlockSizesToFit(true) ;	// YES horizontal flush!
		
		ds.configCanvas = new BlockDrawerConfigCanvas(blitRegion) ;
		
		// by default there is no displacement, and thus no margin.
		ds.displacementSafeMarginOffsetDraw = 0 ;
		ds.displacementSafeMarginOffsetBlit = 0 ;
		
		// we always draw 2d.
		ds.draw3D = false ;
		ds.drawQ1 = false ;
		ds.drawQ0 = true ;
		
		// alter the draw color for specific paints.  Here
		// are our general rules, and their specific results.
		// First, determine the fill alpha of a color, by:
		// 		if pulseFill, take 50% pulse value.
		//		otherwise, take the standard locked fill.
		//		NOTE: although some blocks have no standard fill (e.g. Flash),
		//		their fill paint is still set.
		// Second, determine the fill color by composing the alpha'd 
		//		fill color over the block background color.
		// We will set fill as this color with 100% opacity.
		int bgColor = ColorOp.setAlphaForColor(255, this.color_background_piece) ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			for ( int qp = 0; qp < 2; qp++ ) {
				int alpha = ds.alpha_lockedFill ;
				
				// TEST TEST TEST 
				alpha = Math.max(alpha, (int)Math.round(255 * ds.alphaScale_lockedFillPulse[1]) ) ;
				
				// set alpha and compose
				int c = color_qOrientationQPaneFill[qo][qp] ;
				c = ColorOp.setAlphaForColor( alpha, c ) ;
				c = ColorOp.composeAOverB(c, bgColor) ;
				ds.color_qOrientationQPaneFill[qo][qp] = c ;
			}
			
			ds.alphamult_qOrientationBorder[qo] = 1 ;
			ds.alphamult_qOrientationFill[qo] = 1 ;
			ds.alphamult_qOrientationEdge[qo] = 1 ;
			
		}
		ds.alpha_lockedFill = 255 ;
		ds.alpha_lockedBorder = 255 ;
		ds.alpha_lockedTop = 255 ;
		ds.alphaScale_lockedFillPulse = new float[]{0,0} ;
		ds.alphaScale_pieceFillPulse = new float[]{0,0} ;
		ds.alphaScale_lockedBoxPulse = new float[]{0,0} ;
		ds.alphaScale_pieceBoxPulse = new float[]{0,0} ;
		
		
		// ALL blocks are drawn with standard border and fill colors.
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			ds.behavior_qo_3d[qo] = BEHAVIOR_QO_3D_NONE ;
			ds.behavior_qo_border[qo] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER_VOLATILE ;
			ds.behavior_qo_fill[qo] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
			ds.behavior_qo_pulse[qo] = BEHAVIOR_QO_PULSE_NONE ;
			ds.behavior_qo_top[qo] = BEHAVIOR_QO_TOP_NONE ;
		}
		
		// certain block types get their Q1 fill and border
		// transfered to Q0 so it is drawn.  For instance, because
		// we expect F0/F1 and ST to have unique Q0/Q1 colors
		// which are tuned towards the general color scheme of the
		// pane, we move the ST_1 forward to Q0 and keep F0 where
		// it is.
		int [] forward_qo = new int[] {
				QOrientations.ST,				// use back-pane colors
				QOrientations.SL_INACTIVE} ;	// compare to SL.  We get front- and back-pane versions, so pull one forward.
		for ( int i = 0; i < forward_qo.length; i++ ) {
			int qo = forward_qo[i] ;
			ds.color_qOrientationQPaneBorder[qo][0] = ds.color_qOrientationQPaneBorder[qo][1] ;
			ds.color_qOrientationQPaneFill[qo][0] = ds.color_qOrientationQPaneFill[qo][1] ;
		}
		
		// NOTE: as a hack, we use FRIENDLY for the ST borders; they would normally be
		// drawn with ST q0 drawables, which do not change color here.
		int [] fill_as_border_qo = new int[] {
				QOrientations.ST
		} ;
		for ( int i = 0; i < fill_as_border_qo.length; i++ ) {
			int qo = fill_as_border_qo[i] ;
			ds.color_qOrientationQPaneBorder[qo][0] = this.color_qOrientationQPaneFill[qo][0] ;
		}
		
		// we never draw a row neg-one.
		ds.alpha_rowNegativeOneBorder = 0 ;
		
		// now set our veil.  This is probably the only thing based on 2d/3d
		// for the source object.
		if ( this.draw3D ) {
			ds.veil_qo = new byte[]{
					QOrientations.S0,
					QOrientations.S1,
					QOrientations.ST,
					//QOrientations.SL,
					//QOrientations.SL_INACTIVE,
					//QOrientations.SL_ACTIVE,
					//QOrientations.F0
					} ;
			ds.veil_qo_main = QOrientations.SL_ACTIVE ;
		}
		else {
			ds.veil_qo = new byte[]{
					QOrientations.R0,
					QOrientations.R1,
					QOrientations.R2,
					QOrientations.R3,
					QOrientations.R4,
					QOrientations.R5,
					QOrientations.R6
					} ;
			ds.veil_qo_main = QOrientations.RAINBOW_BLAND ;
		}
		
		// return it!
		return ds ;
	}
	
	private GlowColor [][] copy( GlowColor [][] ar ) {
		if ( ar == null )
			return null ;
		
		GlowColor [][] res = new GlowColor[ar.length][] ;
		for ( int i = 0; i < ar.length; i++ ) 
			res[i] = copy( ar[i] ) ;
		return res ;
	}
	
	private GlowColor [] copy( GlowColor [] ar ) {
		return ar == null ? null : ar.clone() ;
	}
	
	private float [][] copy( float [][] ar ) {
		if ( ar == null )
			return null ;
		
		float [][] res = new float[ar.length][] ;
		for ( int i = 0; i < ar.length; i++ ) 
			res[i] = copy( ar[i] ) ;
		return res ;
	}
	
	private float [] copy( float [] ar ) {
		return ar == null ? null : ar.clone() ;
	}
	
	private int [][] copy( int [][] ar ) {
		if ( ar == null )
			return null ;
		
		int [][] res = new int[ar.length][] ;
		for ( int i = 0; i < ar.length; i++ ) 
			res[i] = copy( ar[i] ) ;
		return res ;
	}
	
	private int [] copy( int [] ar ) {
		return ar == null ? null : ar.clone() ;
	}
	
	private byte [][] copy( byte [][] ar ) {
		if ( ar == null )
			return null ;
		
		byte [][] res = new byte[ar.length][] ;
		for ( int i = 0; i < ar.length; i++ ) 
			res[i] = copy( ar[i] ) ;
		return res ;
	}
	
	private byte [] copy( byte [] ar ) {
		return ar == null ? null : ar.clone() ;
	}
	
	
	public void setDefaultAlphas() {
		
		
		int defaultSettingIndex = getDefaultSettingIndex(mSkin) ;
		
		// BLOCK ALPHAS
		// These simple alphas are entirely determined by our default setting
		// index.
		alpha_lockedFill = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_LOCKED_FILL] ;
		alpha_lockedBorder = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_LOCKED_BORDER] ;
		alpha_lockedTop = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_LOCKED_TOP] ;
		alpha_pieceFill = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_PIECE_FILL] ;
		alpha_pieceBorder = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_PIECE_BORDER] ;
		alpha_pieceTop = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_PIECE_TOP] ;
		alpha_ghostFill = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_GHOST_FILL] ;
		alpha_ghostBorder = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_GHOST_BORDER] ;
		alpha_ghostTop = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_GHOST_TOP] ;
		
		alpha_emphFill = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_EMPH_FILL] ;
		alpha_emphBorder = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_EMPH_FILL] ;
		alpha_emphTop = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_EMPH_FILL] ;
		
		
		alpha_displacementFill = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_DISPLACEMENT_FILL] ;
		alpha_displacementBorder = DEFAULT_SETTING_BLOCK_ALPHA[defaultSettingIndex][INDEX_TYPE_DISPLACEMENT_BORDER] ;
		
		alphaScale_lockedFillPulse = copy( DEFAULT_SETTING_PULSE_ALPHA_SCALE[defaultSettingIndex][INDEX_TYPE_PULSE_LOCKED_FILL] ) ;
		alphaScale_pieceFillPulse = copy( DEFAULT_SETTING_PULSE_ALPHA_SCALE[defaultSettingIndex][INDEX_TYPE_PULSE_PIECE_FILL] ) ;
		alphaScale_lockedBoxPulse = copy( DEFAULT_SETTING_PULSE_ALPHA_SCALE[defaultSettingIndex][INDEX_TYPE_PULSE_LOCKED_BOX] ) ;
		alphaScale_pieceBoxPulse = copy( DEFAULT_SETTING_PULSE_ALPHA_SCALE[defaultSettingIndex][INDEX_TYPE_PULSE_PIECE_BOX] ) ;
		
		alpha_rowNegativeOneBorder = 127 ;
		
		// SHADOW ALPHAS
		// These more complex alphas are determined by both the skin and the current
		// draw detail.
		switch( drawDetail ) {
		case DRAW_DETAIL_HIGH:
		case DRAW_DETAIL_MID:
			alpha_innerShadow = copy( DEFAULT_SETTING_ALPHA[defaultSettingIndex][INDEX_TYPE_INNER_SHADOW] ) ;
			alpha_dropShadow = copy( DEFAULT_SETTING_ALPHA[defaultSettingIndex][INDEX_TYPE_OUTER_SHADOW] ) ;
			alpha_extrudedWallShadow = 127 ;
				// only drawn in QUANTRO mode
			
			break ;
			
		default:
			// otherwise, use a low-detail version of our shadows, containing only the first entry.
			alpha_innerShadow = new int[] { DEFAULT_SETTING_ALPHA[defaultSettingIndex][INDEX_TYPE_INNER_SHADOW][0] } ;
			alpha_dropShadow = new int[] { DEFAULT_SETTING_ALPHA[defaultSettingIndex][INDEX_TYPE_OUTER_SHADOW][0] } ;
			alpha_extrudedWallShadow = 127 ;
				// only drawn in QUANTRO mode
			
		}
	}
	
	
	public void setDefaultGlows() {
		// DEFAULT_SETTING_GLOW is indexed by glow draw style (flare, fade, etc.), skin,
		// and finally glow type (lock, enter, etc.).
		int defaultSettingIndex = getDefaultSettingIndex(mSkin) ;
		
		glow_color 		= copy( DEFAULT_SETTING_GLOW_COLOR[glowDrawStyle][defaultSettingIndex] );
		glow_lightenBy 	= copy( DEFAULT_SETTING_GLOW_LIGHTENING[glowDrawStyle][defaultSettingIndex] );
		glow_alphaPeakNormalized = copy( DEFAULT_SETTING_GLOW_ALPHA_PEAK_NORMALIZED[glowDrawStyle][defaultSettingIndex] );
		glow_radius 	= copy( DEFAULT_SETTING_GLOW_RADIUS[glowDrawStyle][defaultSettingIndex] );
	}
	
	
	public void setDefaultColors( Resources res ) {
		color_qOrientationQPaneCornerBorderShine
				= new int[QOrientations.NUM][2][NUM_CORNERS] ;
		color_qPaneCornerBottomGuideShine = new int[2][NUM_CORNERS] ;
		// As of 1/11/2012, we only applied border shine to
		// rainbow pieces.  The rest got a zero-alpha shine.
		// BlockDrawer knows not to draw zero alpha
		// shapes.
		//
		// Starting 1/12/2012, we are beginning to implement a new
		// graphical style in Quantro, including border shines.
		
		if ( drawDetail >= DrawSettings.DRAW_DETAIL_MID ) {
			for ( byte qo = 0; qo < QOrientations.NUM; qo++ )
				setQOrientationCornerBorderShine( qo ) ;
			setRowGuideBorderShine() ;
		}
		
		color_qOrientationQPaneGlowColor = new int [QOrientations.NUM][2] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) 
			for ( int qp = 0; qp < 2; qp++ )
				color_qOrientationQPaneGlowColor[qo][qp] = 0x00000000 ;
		// here are some glow colors, dude.
		for ( int qp = 0; qp < 2; qp++ ) {
			color_qOrientationQPaneGlowColor[QOrientations.R0][qp] = mColorScheme.getGlowColor(QOrientations.R0, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.R1][qp] = mColorScheme.getGlowColor(QOrientations.R1, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.R2][qp] = mColorScheme.getGlowColor(QOrientations.R2, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.R3][qp] = mColorScheme.getGlowColor(QOrientations.R3, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.R4][qp] = mColorScheme.getGlowColor(QOrientations.R4, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.R5][qp] = mColorScheme.getGlowColor(QOrientations.R5, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.R6][qp] = mColorScheme.getGlowColor(QOrientations.R6, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.RAINBOW_BLAND][qp] = mColorScheme.getGlowColor(QOrientations.RAINBOW_BLAND, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.PUSH_DOWN][qp] = mColorScheme.getGlowColor(QOrientations.PUSH_DOWN, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.PUSH_DOWN_ACTIVE][qp] = mColorScheme.getGlowColor(QOrientations.PUSH_DOWN_ACTIVE, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.PUSH_UP_ACTIVE][qp] = mColorScheme.getGlowColor(QOrientations.PUSH_UP_ACTIVE, qp) ;
			
			
			color_qOrientationQPaneGlowColor[QOrientations.S0][qp] = mColorScheme.getGlowColor(QOrientations.S0, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.S1][qp] = mColorScheme.getGlowColor(QOrientations.S1, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.ST][qp] = mColorScheme.getGlowColor(QOrientations.ST, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.ST_INACTIVE][qp] = mColorScheme.getGlowColor(QOrientations.ST_INACTIVE, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.F0][qp] = mColorScheme.getGlowColor(QOrientations.F0, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.F1][qp] = mColorScheme.getGlowColor(QOrientations.F1, qp) ;
			color_qOrientationQPaneGlowColor[QOrientations.SL_ACTIVE][qp] = mColorScheme.getGlowColor(QOrientations.SL_ACTIVE, qp) ;
		}
		
		color_qOrientationDetailColors = new int[QOrientations.NUM][] ;
		
		color_qOrientationDetailColors[QOrientations.F0] = new int[NUM_DETAILS_FLASH] ;
		color_qOrientationDetailColors[QOrientations.F1] = new int[NUM_DETAILS_FLASH] ;
		for ( int i = 0; i < NUM_DETAILS_FLASH; i++ ) {
			color_qOrientationDetailColors[QOrientations.F0][i] = mColorScheme.getDetailColor(QOrientations.F0, 0, i) ;
			color_qOrientationDetailColors[QOrientations.F1][i] = mColorScheme.getDetailColor(QOrientations.F0, 1, i) ;
		}
		
		color_qOrientationDetailColors[QOrientations.PUSH_DOWN] = new int[NUM_DETAILS_CHEVRON] ;
		color_qOrientationDetailColors[QOrientations.PUSH_DOWN_ACTIVE] = new int[NUM_DETAILS_CHEVRON] ;
		color_qOrientationDetailColors[QOrientations.PUSH_UP] = new int[NUM_DETAILS_CHEVRON] ;
		color_qOrientationDetailColors[QOrientations.PUSH_UP_ACTIVE] = new int[NUM_DETAILS_CHEVRON] ;
		for ( int i = 0; i < NUM_DETAILS_CHEVRON; i++ ) {
			color_qOrientationDetailColors[QOrientations.PUSH_DOWN][i] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN, 0, i) ;
			color_qOrientationDetailColors[QOrientations.PUSH_DOWN_ACTIVE][i] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN_ACTIVE, 0, i) ;
			color_qOrientationDetailColors[QOrientations.PUSH_UP][i] = mColorScheme.getDetailColor(QOrientations.PUSH_UP, 0, i) ;
			color_qOrientationDetailColors[QOrientations.PUSH_UP_ACTIVE][i] = mColorScheme.getDetailColor(QOrientations.PUSH_UP_ACTIVE, 0, i) ;
		}
	}
	
	
	private void setQOrientationCornerBorderShine( byte qo ) {
		behavior_qo_border_shine[qo] = BEHAVIOR_QO_BORDER_SHINE_NONE ;
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int corner = 0; corner < NUM_CORNERS; corner++ ) {
				color_qOrientationQPaneCornerBorderShine[qo][qp][corner] = 0x00 ;
			}
			
			for ( int shine = 0; shine < mColorScheme.getShineNumber(qo, qp); shine++ ) {
				ColorScheme.ShineShape shineShape = mColorScheme.getShineShape(qo, qp, shine) ;
				int minAlpha = mColorScheme.getShineAlphaMin(qo, qp, shine) ;
				int maxAlpha = mColorScheme.getShineAlphaMax(qo, qp, shine) ;
				int color = mColorScheme.getShineColor(qo, qp, shine) ;
				
				if ( maxAlpha > 0 ) {
					for ( int corner = 0; corner < NUM_CORNERS; corner++ ) {
						float p = SHINE_PROPORTION_BY_SHAPE_AND_CORNER[shineShape.ordinal()][corner] ;
						int alpha = minAlpha + Math.round( p * ( maxAlpha - minAlpha ) ) ;
						int shineColor = ColorOp.setAlphaForColor(alpha, color) ;
						color_qOrientationQPaneCornerBorderShine[qo][qp][corner] =
							ColorOp.composeAOverB(
									shineColor,
									color_qOrientationQPaneCornerBorderShine[qo][qp][corner]) ;
					}
					
					behavior_qo_border_shine[qo] = BEHAVIOR_QO_BORDER_SHINE_CUSTOM ;
				}
			}
		}
	}
	
	private void setRowGuideBorderShine() {
		for ( int qp = 0; qp < 2; qp++ ) {
			for ( int corner = 0; corner < NUM_CORNERS; corner++ ) {
				color_qPaneCornerBottomGuideShine[qp][corner] = 0x00 ;
			}
			
			ColorScheme.ShineShape shineShape ;
			int minAlpha, maxAlpha ;
			int color ;
			
			int numShines = mSkin.getGame() == Skin.Game.QUANTRO ?
					mColorScheme.getQuantroRowGuideShineNumber(qp)
					: mColorScheme.getRetroRowGuideShineNumber(qp) ;
			
			for ( int shine = 0; shine < numShines; shine++ ) {
				if ( mSkin.getGame() == Skin.Game.QUANTRO ) {
					shineShape = mColorScheme.getQuantroRowGuideShineShape(qp, shine) ;
					minAlpha = mColorScheme.getQuantroRowGuideShineAlphaMin(qp, shine) ;
					maxAlpha = mColorScheme.getQuantroRowGuideShineAlphaMax(qp, shine) ;
					color = mColorScheme.getQuantroRowGuideShineColor(qp, shine) ;
				} else {
					shineShape = mColorScheme.getRetroRowGuideShineShape(qp, shine) ;
					minAlpha = mColorScheme.getRetroRowGuideShineAlphaMin(qp, shine) ;
					maxAlpha = mColorScheme.getRetroRowGuideShineAlphaMax(qp, shine) ;
					color = mColorScheme.getRetroRowGuideShineColor(qp, shine) ;
				}
				
				if ( maxAlpha > 0 ) {
					for ( int corner = 0; corner < NUM_CORNERS; corner++ ) {
						float p = SHINE_PROPORTION_BY_SHAPE_AND_CORNER[shineShape.ordinal()][corner] ;
						int alpha = minAlpha + Math.round( p * ( maxAlpha - minAlpha ) ) ;
						int shineColor = ColorOp.setAlphaForColor(alpha, color) ;
						color_qPaneCornerBottomGuideShine[qp][corner] =
							ColorOp.composeAOverB(
									shineColor,
									color_qPaneCornerBottomGuideShine[qp][corner]) ;
					}
				}
			}
		}
	}
	
	
	public void setDefaultProportions() {
		if ( ( this.drawQ0 && this.drawQ1 ) || this.draw3D ) {
			proportion_qXOffset = 0.266f ;
			proportion_qYOffset = -0.266f ;
		}
		else {		// No 3D draw
			proportion_qXOffset = 0f ;
			proportion_qYOffset = 0f ;
		}
		
		proportion_borderWidth = 0.1f ;
		proportion_borderHeight = 0.1f ;
		
		// TODO: We are retiring this value.  We're not using small prisms anymore.
		// for now, we keep it for LOW settings and in the transitions.
		proportion_smallPrismWidth = 0.3f ;
		proportion_smallPrismHeight = 0.3f ;
		
		proportion_innerBorderWidth = 0.1f ;
		proportion_innerBorderHeight = 0.1f ;
		
		proportion_innerBorderXInset = 0.05f ;
		proportion_innerBorderYInset = 0.05f ;
		
		proportion_miniBorderWidth = 0.1f ;
		proportion_miniBorderHeight = 0.1f ;
		
		proportion_miniBorderXInset = 0.15f ;
		proportion_miniBorderYInset = 0.15f ;
		
		proportion_hashingLineWidth = new float[] { 0.15f, 0.3f } ;
		proportion_hashingLineSpacing = new float[] { 0.5f, 0.5f } ;
		proportion_hashingLineShadowLayerRadius = new float[] { 0, 0 } ;
		
		proportion_columnUnlockerExtension = 0.23f ;
		proportion_columnUnlockerInset = 0 ;
		proportion_columnUnlockerHeight = 0 ;
		
		// Both Retro and Quantro use edge shadows and
		// offset shadows, but our mock-ups use different values for each.
		// Maybe these will coallesce in testing, but for now...
		int defaultSettingIndex = getDefaultSettingIndex( mSkin ) ;
		proportion_innerShadowGaussianRadius = copy( DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[defaultSettingIndex][INDEX_TYPE_INNER_SHADOW] ) ;
		proportion_innerShadowXOffset = new float[proportion_innerShadowGaussianRadius.length] ;
		proportion_innerShadowYOffset = new float[proportion_innerShadowGaussianRadius.length] ;
		for ( int i = 0; i < proportion_innerShadowGaussianRadius.length; i++ ) {
			float [] offset = DEFAULT_SETTING_PROPORTION_OFFSET[defaultSettingIndex][INDEX_TYPE_INNER_SHADOW][i] ;
			if ( offset == null ) {
				proportion_innerShadowXOffset[i] = 0 ;
				proportion_innerShadowYOffset[i] = 0 ;
			} else {
				proportion_innerShadowXOffset[i] = offset[INDEX_X] ;
				proportion_innerShadowYOffset[i] = offset[INDEX_Y] ;
			}
		}
		
		proportion_extrudedWallShadowGaussianRadius = DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[defaultSettingIndex][INDEX_TYPE_EXTRUDED_SHADOW][0] ;
		
		if ( drawDetail >= DrawSettings.DRAW_DETAIL_MID ) {
			proportion_outerShadowGaussianRadius = copy( DEFAULT_SETTING_PROPORTION_GAUSSIAN_RADIUS[defaultSettingIndex][INDEX_TYPE_OUTER_SHADOW] ) ;
			proportion_outerShadowXOffset = new float[proportion_outerShadowGaussianRadius.length] ;
			proportion_outerShadowYOffset = new float[proportion_outerShadowGaussianRadius.length] ;
			for ( int i = 0; i < proportion_outerShadowGaussianRadius.length; i++ ) {
				float [] offset = DEFAULT_SETTING_PROPORTION_OFFSET[defaultSettingIndex][INDEX_TYPE_OUTER_SHADOW][i] ;
				if ( offset == null ) {
					proportion_outerShadowXOffset[i] = 0 ;
					proportion_outerShadowYOffset[i] = 0 ;
				} else {
					proportion_outerShadowXOffset[i] = offset[INDEX_X] ;
					proportion_outerShadowYOffset[i] = offset[INDEX_Y] ;
				}
			}
		} else {
			proportion_outerShadowGaussianRadius = new float[]{ 0.03f } ;
			proportion_outerShadowXOffset = new float[] { 0 } ;
			proportion_outerShadowYOffset = new float[] { 0 } ;
		}
	}
	
	
	
	public void setCanvasTarget( int blit, int scale, Rect region ) {
		setCanvasTarget( blit, scale, region.left, region.top, region.right, region.bottom ) ;
	}
	
	public void setCanvasTarget( int blit, int scale, int left, int top, int right, int bottom ) {
		// this is a transitional method between explicit drawRegion/blitRegion,
		// and configCanvas.
		this.blit = blit ;
		this.scale = scale ;
		// copy into draw region and blit region.
		this.width = (right - left) / scale ;
		this.height = (bottom - top) / scale ;
		
		// now adjust config canvas.
		if ( this.configCanvas.getClipRegion() == null )
			this.configCanvas.setRegion(left, top, right, bottom) ;
		else
			this.configCanvas.setRegionAndClip(left, top, right, bottom) ;
		
		this.resetDisplacementMargin() ;
	}
	
	
	public void setBlockSizesToFit() {
		setBlockSizesToFitWithMax( rowsToFit, COLS, Integer.MAX_VALUE, Integer.MAX_VALUE, false ) ;
	}
	
	public void setBlockSizesToFit(boolean horizontalFlush) {
		setBlockSizesToFitWithMax( rowsToFit, COLS, Integer.MAX_VALUE, Integer.MAX_VALUE, horizontalFlush ) ;
	}
	
	
	
	public void setBlockSizesToFitWithMax( int maxWidth, int maxHeight ) {
		setBlockSizesToFitWithMax( rowsToFit, COLS, maxWidth, maxHeight, false ) ;
	}
	
	public void setBlockSizesToFitWithMax( int maxWidth, int maxHeight, boolean horizontalFlush ) {
		setBlockSizesToFitWithMax( rowsToFit, COLS, maxWidth, maxHeight, horizontalFlush ) ;
	}
	
	public void setBlockSizesToFit( int numRows, int numCols ) {
		setBlockSizesToFitWithMax( numRows, numCols, Integer.MAX_VALUE, Integer.MAX_VALUE, false ) ;
	}
	
	public void setBlockSizesToFit( int numRows, int numCols, boolean horizontalFlush ) {
		setBlockSizesToFitWithMax( numRows, numCols, Integer.MAX_VALUE, Integer.MAX_VALUE, horizontalFlush ) ;
	}
	
	public void setBlockSizesToFitWithMax( int numRows, int numCols, int maxWidth, int maxHeight ) {
		setBlockSizesToFitWithMax( numRows, numCols, maxWidth, maxHeight, false ) ;
	}
	
	public void setBlockSizesToFitWithMax( int numRows, int numCols, int maxWidth, int maxHeight, boolean horizontalFlush ) {
		
		blockXPositionCached_byQPane_X = null  ;
		blockYPositionCached_byQPane_Y = null ;
		
		// How much horizontal/vertical space is needed?
    	// Recall that it makes the most sense to define qPane offset as
    	// a proportion of block dimensions.  Use that here to determine
    	// the actual block size, and the pixel-scale offset.
    	// The total horizontal space available to draw the game
    	// is the canvas width, minus 2 times the margin.  Within this
    	// space, we must represent COLS + (proportion) "columns".
    	float sizeForColumns = width - horizontalMargin*2 ;
    	size_blockWidth = (int)(sizeForColumns / ( numCols + Math.abs(proportion_qXOffset) )) ;
    	
    	float sizeForRows = height - verticalMargin*2 ;
    	setSize_blockHeight((int)(sizeForRows / ( numRows + Math.abs(proportion_qYOffset) ))) ;
    	
    	// 6/13 attempt to resize - we want to be flush, if reasonable possible.
    	if ( horizontalFlush && proportion_qXOffset != 0 ) {
    		// need magnitude at least 2 (for a 1 pixel drop shadow).
			while ( sizeForColumns - getSize_blockWidth() * numCols >= getSize_blockWidth()  )
				setSize_blockWidth(getSize_blockWidth() + 1) ;
			
			// the above will stop when < the size of a full block appears. However, growing
			// our blocks by 1 will increase the space used by COLS pixels.  Do this as long
			// as we get closer to the right amount of qXOffset.
			while ( 
					Math.abs( Math.abs( proportion_qXOffset ) - ( sizeForColumns - getSize_blockWidth() * numCols ) / getSize_blockWidth()  )
					> Math.abs( Math.abs( proportion_qXOffset ) - ( sizeForColumns - (getSize_blockWidth()+1) * numCols ) / (getSize_blockWidth()+1)  ) )
				setSize_blockWidth(getSize_blockWidth() + 1) ;
    	}
    	
		// do our best to size this thing...
		Rect configBlocksRect = new Rect(
				horizontalMargin,
				verticalMargin,
				width - horizontalMargin,
				height - verticalMargin ) ;
    	
		BlockDrawerConfigBlockGrid.Scale scaleX = horizontalFlush
				? BlockDrawerConfigBlockGrid.Scale.FLUSH
				: BlockDrawerConfigBlockGrid.Scale.BEST ;
		BlockDrawerConfigBlockGrid.Scale scaleY = BlockDrawerConfigBlockGrid.Scale.BEST ;
		
		BlockDrawerConfigBlockGrid.Align alignX = null, alignY = null ;
		switch( behavior_align_horizontal ) {
		case BEHAVIOR_ALIGN_GRID:
			alignX = BlockDrawerConfigBlockGrid.Align.GRID ;
			break ;
			
		case BEHAVIOR_ALIGN_CENTER_BLOCKS:
		case BEHAVIOR_ALIGN_CENTER_GRID:
			alignX = BlockDrawerConfigBlockGrid.Align.CENTER_ATTEMPT ;
			break ;
		}
		
		switch( behavior_align_vertical ) {
		case BEHAVIOR_ALIGN_GRID:
			alignY = BlockDrawerConfigBlockGrid.Align.GRID ;
			break ;
			
		case BEHAVIOR_ALIGN_CENTER_BLOCKS:
		case BEHAVIOR_ALIGN_CENTER_GRID:
			alignY = BlockDrawerConfigBlockGrid.Align.CENTER_ATTEMPT ;
			break ;
		}
		
		// MATCH TEST: we have change DrawSettings to not use
		// "center align"; current estimates are that this setting is,
		// effectively, never used (it always reverts to grid-aligned,
		// because we size the area smaller than the max block content
		// for reasons of drawing drop shadows).
		alignX = BlockDrawerConfigBlockGrid.Align.GRID ;
		alignY = BlockDrawerConfigBlockGrid.Align.GRID ;
		
    	configBlocks = new BlockDrawerConfigBlockGrid(
				ROWS, COLS,
				configBlocksRect, numRows, numCols,
				maxWidth, maxHeight, proportion_qXOffset, proportion_qYOffset,
				alignX, alignY, scaleX, scaleY ) ;
		

    	if ( squareBlocks )
    		setSize_blockWidth(setSize_blockHeight(Math.min(getSize_blockHeight(), getSize_blockWidth()))) ;
    	
    	setSize_blockWidth(Math.min( maxWidth, Math.max(1, getSize_blockWidth()) )) ;
    	setSize_blockHeight(Math.min( maxHeight, Math.max(1, getSize_blockHeight()) )) ;
    	
    	
    	displayedRows = calculateDisplayedRows() ;
    	
    	resetDisplacementMargin() ;
    	
    	// WHAWHAWHA!  We actually (currently) calculate Y position
    	// for ALIGN_CENTER types by offsetting by 'displayedRows'.  Specifically,
    	// we measure our displacement from the top as if there are only 'displayedRows' present.
    	// thus, row 'displayedRows-1' is placed as if it is row 'numRows-1'.  We need to offset by the difference.
    	//if ( behavior_align_vertical == BEHAVIOR_ALIGN_CENTER_BLOCKS || behavior_align_vertical == BEHAVIOR_ALIGN_CENTER_GRID ) {
    	//	configBlocks.setOffset(0, configBlocks.getWidth() * (displayedRows - rowsToFit)) ;
    	//}
    	
    	setSizesFromProportions() ;
    	
    	if ( horizontalFlush ) {
    		int spaceLeft = (int)Math.ceil( sizeForColumns - getSize_blockWidth() * numCols - Math.abs( getSize_qXOffset() ) ) ;
    		if ( spaceLeft > 0 ) {
    			if ( getSize_qXOffset() < 0 )
    				setSize_qXOffset(getSize_qXOffset() - spaceLeft) ;
    			else if ( getSize_qXOffset() > 0 )
    				setSize_qXOffset(getSize_qXOffset() + spaceLeft) ;
    		}
    	}
	}
	
	
	private int calculateDisplayedRows() {
		int rows ;
		if ( behavior_align_vertical == BEHAVIOR_ALIGN_CENTER_BLOCKS || behavior_align_vertical == BEHAVIOR_ALIGN_CENTER_GRID )
    		rows = rowsToFit ;
    	else {
    		rows = Math.min(ROWS, (int)Math.ceil(height / (double)getSize_blockHeight())) ;
    		// we display up to 1 "unused" rows when displacement is featured.
    		if ( this.getBehaviorIs_displacement() )
    			rows = Math.min(ROWS, rows+2) ;
    	}
		return rows ;
	}
	
	private void resetDisplacementMargin() {
		// by default there is no displacement, and thus no margin.
		this.displacementSafeMarginOffsetDraw = 0 ;
		this.displacementSafeMarginOffsetBlit = 0 ;
		
		if ( this.getBehaviorIs_displacement() ) {
			// if we need a displacement margin (i.e. using displacement
			// along with a blit style), we make it approximately 1.5 the
			// height of a block.  This is consistent with Game objects
			// maintaining a minimum displacement of -1.5.
			switch( blit ) {
			case BLIT_SEPTUPLE:
			case BLIT_FULL:
				// it may seem strange to not adjust the blit dimensions for scale.
				// We keep them the same because BlockDrawer sets canvas destination
				// using SOURCE-RELATIVE offsets (not destination relative).
				displacementSafeMarginOffsetDraw = (int)Math.ceil(size_blockHeight * 2) ;
				displacementSafeMarginOffsetBlit = -displacementSafeMarginOffsetDraw ;
				break ;
			case BLIT_NONE:
				// no margin.
				break ;
			}
		}
	}
	
	
	
	public void setSizesFromProportions() {
		setSize_qXOffset((int)Math.round(getSize_blockWidth() * proportion_qXOffset)) ;					
		setSize_qYOffset((int)Math.round(getSize_blockHeight() * proportion_qYOffset)) ;
		// need magnitude at least 2 (for a 1 pixel drop shadow).
		if ( Math.abs( proportion_qXOffset) != 0 )
			setSize_qXOffset((proportion_qXOffset > 0)
					? Math.max(getSize_qXOffset(), 2) 
					: Math.min(getSize_qXOffset(), -2)) ;
		if ( Math.abs( proportion_qYOffset) != 0 )	
			setSize_qYOffset((proportion_qYOffset > 0)
					? Math.max(getSize_qYOffset(), 2) 
					: Math.min(getSize_qYOffset(), -2)) ;
				
		
		size_borderWidth = Math.max(2, Math.round(getSize_blockWidth() * proportion_borderWidth) ) ;				
		size_borderHeight = Math.max(2, Math.round(getSize_blockHeight() * proportion_borderHeight) ) ;				
		
		size_smallPrismWidth = Math.max(1, Math.round(getSize_blockWidth() * proportion_smallPrismWidth) ) ;			
		size_smallPrismHeight = Math.max(1, Math.round(getSize_blockHeight() * proportion_smallPrismHeight) ) ;	
		
		size_innerBorderWidth = Math.max(1, Math.round(getSize_blockWidth() * proportion_innerBorderWidth) ) ;				
		size_innerBorderHeight = Math.max(1, Math.round(getSize_blockHeight() * proportion_innerBorderHeight) ) ;
		
		size_innerBorderXInset = (int)Math.ceil(getSize_blockWidth() * proportion_innerBorderXInset) ;		
		size_innerBorderYInset = (int)Math.ceil(getSize_blockHeight() * proportion_innerBorderYInset) ;	
		
		size_miniBorderWidth = Math.max(1, Math.round(getSize_blockWidth() * proportion_miniBorderWidth)) ;
		size_miniBorderHeight = Math.max(1, Math.round(getSize_blockHeight() * proportion_miniBorderHeight)) ;
		
		size_miniBorderXInset = Math.max(1, Math.round(getSize_blockWidth() * proportion_miniBorderXInset)) ;
		size_miniBorderYInset = Math.max(1, Math.round(getSize_blockHeight() * proportion_miniBorderYInset)) ;
		
		size_hashingLineWidth = new float[]{
				getSize_blockWidth() * proportion_hashingLineWidth[0],
				getSize_blockWidth() * proportion_hashingLineWidth[1]
			} ;
		size_hashingLineSpacing = new float[]{
				getSize_blockWidth() * proportion_hashingLineSpacing[0],
				getSize_blockWidth() * proportion_hashingLineSpacing[1]
			} ;
		size_hashingLineShadowLayerRadius = new float[] {
				getSize_blockWidth() * proportion_hashingLineShadowLayerRadius[0],
				getSize_blockWidth() * proportion_hashingLineShadowLayerRadius[1]
			} ;
		
		size_innerShadowGaussianRadius = new float[proportion_innerShadowGaussianRadius.length] ;
		size_innerShadowXOffset = new int[proportion_innerShadowGaussianRadius.length] ;
		size_innerShadowYOffset = new int[proportion_innerShadowGaussianRadius.length] ;
		for ( int i = 0; i < proportion_innerShadowGaussianRadius.length; i++ ) {
			size_innerShadowGaussianRadius[i] = getSize_blockWidth() * proportion_innerShadowGaussianRadius[i] ;
			size_innerShadowXOffset[i] = (int)Math.round(getSize_blockWidth() * proportion_innerShadowXOffset[i]) ;
			size_innerShadowYOffset[i] = (int)Math.round(getSize_blockHeight() * proportion_innerShadowYOffset[i]) ;
		}
		
		size_extrudedWallShadowGaussianRadius = getSize_blockWidth() * proportion_extrudedWallShadowGaussianRadius ;
		
		size_outerShadowGaussianRadius = new float[proportion_outerShadowGaussianRadius.length] ;
		size_outerShadowXOffset = new int[proportion_outerShadowGaussianRadius.length] ;
		size_outerShadowYOffset = new int[proportion_outerShadowGaussianRadius.length] ;
		for ( int i = 0; i < proportion_outerShadowGaussianRadius.length; i++ ) {
			size_outerShadowGaussianRadius[i] = getSize_blockWidth() * proportion_outerShadowGaussianRadius[i] ;
			size_outerShadowXOffset[i] = (int)Math.round(getSize_blockWidth() * proportion_outerShadowXOffset[i]) ;
			size_outerShadowYOffset[i] = (int)Math.round(getSize_blockHeight() * proportion_outerShadowYOffset[i]) ;
		}
		
		// size: multiply radius against block size.
		glow_size 		= new int[glow_radius.length][] ;
		for ( int i = 0; i < glow_radius.length; i++ ) {
			glow_size[i] = new int[glow_radius[i].length] ;
			for ( int j = 0; j < glow_radius[i].length; j++ ) {
				glow_size[i][j] = (int)Math.round(size_blockWidth * glow_radius[i][j]) ;
			}
		}
	}
	
	
	public void setDefaultPaints( Resources res ) {
		// Background paint
		color_background_piece = draw3D ? mColorScheme.getQuantroBlockBackgroundColor() : mColorScheme.getRetroBlockBackgroundColor() ;
		background = Background.get(Background.Template.NONE, Background.Shade.BLACK) ;
		
		
		color_prismSides = res.getColor(R.color.standard_prism_side) ;
		color_prismEdges = res.getColor(R.color.standard_prism_edge) ;
        
		// Simplified colors
		colorSimplified_qOrientationQPaneFill = new int[QOrientations.NUM][2] ;
		for ( int q = 0; q < 2; q++ ) {
	        colorSimplified_qOrientationQPaneFill[QOrientations.S0][q] = mColorScheme.getFillColorSimplified(QOrientations.S0, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.S1][q] = mColorScheme.getFillColorSimplified(QOrientations.S1, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.S0_FROM_SL][q] = mColorScheme.getFillColorSimplified(QOrientations.S0, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.S1_FROM_SL][q] = mColorScheme.getFillColorSimplified(QOrientations.S1, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.ST][q] = mColorScheme.getFillColorSimplified(QOrientations.ST, q) ; 
	        colorSimplified_qOrientationQPaneFill[QOrientations.ST_INACTIVE][q] = mColorScheme.getFillColorSimplified(QOrientations.ST_INACTIVE, q) ; 
	        colorSimplified_qOrientationQPaneFill[QOrientations.F0][q] = mColorScheme.getFillColorSimplified(QOrientations.F0, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.F1][q] = mColorScheme.getFillColorSimplified(QOrientations.F1, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.SL][q] = mColorScheme.getFillColorSimplified(QOrientations.SL, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.SL_ACTIVE][q] = mColorScheme.getFillColorSimplified(QOrientations.SL_ACTIVE, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.SL_INACTIVE][q] = mColorScheme.getFillColorSimplified(QOrientations.SL_INACTIVE, q) ;
        }
        colorSimplified_qOrientationQPaneFill[QOrientations.UL][0] = mColorScheme.getFillColorSimplified(QOrientations.S0, 0)  ;
    	colorSimplified_qOrientationQPaneFill[QOrientations.UL][1] = mColorScheme.getFillColorSimplified(QOrientations.S1, 1) ;
        colorSimplified_qOrientationQPaneFill[QOrientations.U0][0] = mColorScheme.getFillColorSimplified(QOrientations.S0, 0) ;
        colorSimplified_qOrientationQPaneFill[QOrientations.U1][1] = mColorScheme.getFillColorSimplified(QOrientations.S1, 1) ;
        
        // Retro blocks
        for ( int q = 0; q < 2; q++ ) {
	        colorSimplified_qOrientationQPaneFill[QOrientations.R0][q] = mColorScheme.getFillColorSimplified(QOrientations.R0, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.R1][q] = mColorScheme.getFillColorSimplified(QOrientations.R1, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.R2][q] = mColorScheme.getFillColorSimplified(QOrientations.R2, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.R3][q] = mColorScheme.getFillColorSimplified(QOrientations.R3, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.R4][q] = mColorScheme.getFillColorSimplified(QOrientations.R4, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.R5][q] = mColorScheme.getFillColorSimplified(QOrientations.R5, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.R6][q] = mColorScheme.getFillColorSimplified(QOrientations.R6, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.RAINBOW_BLAND][q] = mColorScheme.getFillColorSimplified(QOrientations.RAINBOW_BLAND, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.PUSH_DOWN][q] = mColorScheme.getFillColorSimplified(QOrientations.PUSH_DOWN, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][q] = mColorScheme.getFillColorSimplified(QOrientations.PUSH_DOWN_ACTIVE, q) ;
	        colorSimplified_qOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][q] = mColorScheme.getFillColorSimplified(QOrientations.PUSH_UP_ACTIVE, q) ;
	        
	        if ( drawDetail <= DRAW_DETAIL_LOW ) {
	        	colorSimplified_qOrientationQPaneFill[QOrientations.PUSH_DOWN][q] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN, q, 0) ;
		        colorSimplified_qOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][q] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN_ACTIVE, q, 0) ;
		        colorSimplified_qOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][q] = mColorScheme.getDetailColor(QOrientations.PUSH_UP_ACTIVE, q, 0) ;
	        } 
        }
		
        // qCombination backgrounds
        if ( drawDetail <= DRAW_DETAIL_LOW ) {
        	color_qOrientationQPaneFill = copy( colorSimplified_qOrientationQPaneFill ) ;
        	for ( int q = 0; q < 2; q++ ) {
	        	color_qOrientationQPaneFill[QOrientations.PUSH_DOWN][q] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN, q, 0) ;
		        color_qOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][q] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN_ACTIVE, q, 0) ;
		        color_qOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][q] = mColorScheme.getDetailColor(QOrientations.PUSH_UP_ACTIVE, q, 0) ;
        	}
        } else {
            color_qOrientationQPaneFill = new int[QOrientations.NUM][2] ;
	        // Quantro blocks; those for which Q0 is irrelevant, or which are only drawn in
	        // one Q Pane (because why not do it this way?  A Paint has very small overhead
	        // compared to a bitmap).
	        for ( int q = 0; q < 2; q++ ) {
		        color_qOrientationQPaneFill[QOrientations.S0][q] = mColorScheme.getFillColor(QOrientations.S0, q) ;
		        color_qOrientationQPaneFill[QOrientations.S1][q] = mColorScheme.getFillColor(QOrientations.S1, q) ;
		        color_qOrientationQPaneFill[QOrientations.S0_FROM_SL][q] = mColorScheme.getFillColor(QOrientations.S0, q) ;
		        color_qOrientationQPaneFill[QOrientations.S1_FROM_SL][q] = mColorScheme.getFillColor(QOrientations.S1, q) ;
		        color_qOrientationQPaneFill[QOrientations.ST][q] = mColorScheme.getFillColor(QOrientations.ST, q) ; 
		        color_qOrientationQPaneFill[QOrientations.ST_INACTIVE][q] = mColorScheme.getFillColor(QOrientations.ST_INACTIVE, q) ; 
		        color_qOrientationQPaneFill[QOrientations.F0][q] = mColorScheme.getFillColor(QOrientations.F0, q) ;
		        color_qOrientationQPaneFill[QOrientations.F1][q] = mColorScheme.getFillColor(QOrientations.F1, q) ;
		        color_qOrientationQPaneFill[QOrientations.SL][q] = mColorScheme.getFillColor(QOrientations.SL, q) ;
		        color_qOrientationQPaneFill[QOrientations.SL_ACTIVE][q] = mColorScheme.getFillColor(QOrientations.SL_ACTIVE, q) ;
		        color_qOrientationQPaneFill[QOrientations.SL_INACTIVE][q] = mColorScheme.getFillColor(QOrientations.SL_INACTIVE, q) ;
	        }
	        color_qOrientationQPaneFill[QOrientations.UL][0] = mColorScheme.getFillColor(QOrientations.S0, 0)  ;
	    	color_qOrientationQPaneFill[QOrientations.UL][1] = mColorScheme.getFillColor(QOrientations.S1, 1) ;
	        color_qOrientationQPaneFill[QOrientations.U0][0] = mColorScheme.getFillColor(QOrientations.S0, 0) ;
	        color_qOrientationQPaneFill[QOrientations.U1][1] = mColorScheme.getFillColor(QOrientations.S1, 1) ;
	        
	        // Retro blocks
	        for ( int q = 0; q < 2; q++ ) {
		        color_qOrientationQPaneFill[QOrientations.R0][q] = mColorScheme.getFillColor(QOrientations.R0, q) ;
		        color_qOrientationQPaneFill[QOrientations.R1][q] = mColorScheme.getFillColor(QOrientations.R1, q) ;
		        color_qOrientationQPaneFill[QOrientations.R2][q] = mColorScheme.getFillColor(QOrientations.R2, q) ;
		        color_qOrientationQPaneFill[QOrientations.R3][q] = mColorScheme.getFillColor(QOrientations.R3, q) ;
		        color_qOrientationQPaneFill[QOrientations.R4][q] = mColorScheme.getFillColor(QOrientations.R4, q) ;
		        color_qOrientationQPaneFill[QOrientations.R5][q] = mColorScheme.getFillColor(QOrientations.R5, q) ;
		        color_qOrientationQPaneFill[QOrientations.R6][q] = mColorScheme.getFillColor(QOrientations.R6, q) ;
		        color_qOrientationQPaneFill[QOrientations.RAINBOW_BLAND][q] = mColorScheme.getFillColor(QOrientations.RAINBOW_BLAND, q) ;
		        if ( drawDetail >= DRAW_DETAIL_MID ) {
			        color_qOrientationQPaneFill[QOrientations.PUSH_DOWN][q] = mColorScheme.getFillColor(QOrientations.PUSH_DOWN, q) ;
			        color_qOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][q] = mColorScheme.getFillColor(QOrientations.PUSH_DOWN_ACTIVE, q) ;
			        color_qOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][q] = mColorScheme.getFillColor(QOrientations.PUSH_UP_ACTIVE, q) ;
		        } else {
		        	color_qOrientationQPaneFill[QOrientations.PUSH_DOWN][q] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN, q, 0) ;
			        color_qOrientationQPaneFill[QOrientations.PUSH_DOWN_ACTIVE][q] = mColorScheme.getDetailColor(QOrientations.PUSH_DOWN_ACTIVE, q, 0) ;
			        color_qOrientationQPaneFill[QOrientations.PUSH_UP_ACTIVE][q] = mColorScheme.getDetailColor(QOrientations.PUSH_UP_ACTIVE, q, 0) ;
		        }
	        }
        }
        
        
        // HEY GUESS WHAT
        // If we're using LOW detail, we're going to shade these ourselves! 
        // Apply 15% dimming to any rainbow interior.
        // For MID or greater, we don't really need to do this, except that the lower-right
        // border corner (w/o a shine) tends to blend with the lower-right fill corner.  Apply
        // a baseline shine of 5%.
        if ( drawDetail == DrawSettings.DRAW_DETAIL_LOW ) {
	        for ( int qp = 0; qp < 2; qp++ ) {
	        	for ( int qo = QOrientations.R0; qo < QOrientations.R6+1; qo++ ) {
	        		int color = color_qOrientationQPaneFill[qo][qp] ;
	        		color = Color.argb(
	        				255,
	        				(int)Math.round( Color.red(color)*0.85f),
	        				(int)Math.round( Color.green(color)*0.85f),
	        				(int)Math.round( Color.blue(color)*0.85f) ) ;
	        		color_qOrientationQPaneFill[qo][qp] = color ;
	        	}
	        	byte [] qos = new byte[] {
	        		QOrientations.RAINBOW_BLAND,
	        		QOrientations.PUSH_DOWN,
	        		QOrientations.PUSH_DOWN_ACTIVE,
	        		QOrientations.PUSH_UP,
	        		QOrientations.PUSH_UP_ACTIVE } ;
	        	for ( int i = 0; i < qos.length; i++ ) {
	        		int qo = qos[i] ;
	        		int color = color_qOrientationQPaneFill[qo][qp] ;
	        		color = Color.argb(
	        				255,
	        				(int)Math.round( Color.red(color)*0.85f),
	        				(int)Math.round( Color.green(color)*0.85f),
	        				(int)Math.round( Color.blue(color)*0.85f) ) ;
	        		color_qOrientationQPaneFill[qo][qp] = color ;
	        	}
	        }
	        
	        // if not 3d, we darken the background color (just a bit...)
	        if ( !draw3D ) {
	        	int color = color_background_piece ;
	        	color = Color.argb(
        				255,
        				(int)Math.round( Color.red(color)*0.85f),
        				(int)Math.round( Color.green(color)*0.85f),
        				(int)Math.round( Color.blue(color)*0.85f) ) ;
	        	color_background_piece = color ;
	        }
        }
        
        
        // Simplified borders
        colorSimplified_qOrientationQPaneBorder = new int[QOrientations.NUM][2] ;
        for ( int q = 0; q < 2; q++ ) {
	        colorSimplified_qOrientationQPaneBorder[QOrientations.S0][q] = mColorScheme.getBorderColorSimplified(QOrientations.S0, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.S1][q] = mColorScheme.getBorderColorSimplified(QOrientations.S1, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.S0_FROM_SL][q] = mColorScheme.getBorderColorSimplified(QOrientations.S0_FROM_SL, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.S1_FROM_SL][q] = mColorScheme.getBorderColorSimplified(QOrientations.S1_FROM_SL, q) ;
	        
	        
	        // Rainbow blocks use their own colors as borders
	        colorSimplified_qOrientationQPaneBorder[QOrientations.R0][q] = mColorScheme.getBorderColorSimplified(QOrientations.R0, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.R1][q] = mColorScheme.getBorderColorSimplified(QOrientations.R1, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.R2][q] = mColorScheme.getBorderColorSimplified(QOrientations.R2, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.R3][q] = mColorScheme.getBorderColorSimplified(QOrientations.R3, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.R4][q] = mColorScheme.getBorderColorSimplified(QOrientations.R4, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.R5][q] = mColorScheme.getBorderColorSimplified(QOrientations.R5, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.R6][q] = mColorScheme.getBorderColorSimplified(QOrientations.R6, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][q] = mColorScheme.getBorderColorSimplified(QOrientations.RAINBOW_BLAND, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.PUSH_DOWN][q] = mColorScheme.getBorderColorSimplified(QOrientations.PUSH_DOWN, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.PUSH_DOWN_ACTIVE][q] = mColorScheme.getBorderColorSimplified(QOrientations.PUSH_DOWN_ACTIVE, q) ;
	        colorSimplified_qOrientationQPaneBorder[QOrientations.PUSH_UP_ACTIVE][q] = mColorScheme.getBorderColorSimplified(QOrientations.PUSH_UP_ACTIVE, q) ;
	        
	        
	        colorSimplified_qOrientationQPaneBorder[QOrientations.ST][q] = mColorScheme.getBorderColorSimplified(QOrientations.ST, q) ;
		    colorSimplified_qOrientationQPaneBorder[QOrientations.ST_INACTIVE][q] = mColorScheme.getBorderColorSimplified(QOrientations.ST, q) ;
		    colorSimplified_qOrientationQPaneBorder[QOrientations.SL][q] = mColorScheme.getBorderColorSimplified(QOrientations.SL, q) ;
		    colorSimplified_qOrientationQPaneBorder[QOrientations.SL_INACTIVE][q] = mColorScheme.getBorderColorSimplified(QOrientations.SL_INACTIVE, q) ;
		    colorSimplified_qOrientationQPaneBorder[QOrientations.SL_ACTIVE][q] = mColorScheme.getBorderColorSimplified(QOrientations.SL_ACTIVE, q) ;
		    
	    }
	    
	    for ( int qp = 0; qp < 2; qp++ ) {
		    colorSimplified_qOrientationQPaneBorder[QOrientations.F0][qp] = mColorScheme.getBorderColorSimplified(QOrientations.F0, qp) ;
		    colorSimplified_qOrientationQPaneBorder[QOrientations.F1][qp] = mColorScheme.getBorderColorSimplified(QOrientations.F1, qp) ;
	    }
	    colorSimplified_qOrientationQPaneBorder[QOrientations.U0][0] = mColorScheme.getBorderColorSimplified(QOrientations.S0, 0) ;
        colorSimplified_qOrientationQPaneBorder[QOrientations.U1][1] = mColorScheme.getBorderColorSimplified(QOrientations.S1, 1) ;
        colorSimplified_qOrientationQPaneBorder[QOrientations.UL][0] = mColorScheme.getBorderColorSimplified(QOrientations.S0, 0) ;
        colorSimplified_qOrientationQPaneBorder[QOrientations.UL][1] = mColorScheme.getBorderColorSimplified(QOrientations.S1, 1) ;
        
        
        
        // qCombinationBorders
        if ( drawDetail <= DRAW_DETAIL_LOW ) {
        	color_qOrientationQPaneBorder = copy( colorSimplified_qOrientationQPaneBorder ) ;
        } else {
	        color_qOrientationQPaneBorder = new int[QOrientations.NUM][2] ;
	        
		    for ( int q = 0; q < 2; q++ ) {
		        color_qOrientationQPaneBorder[QOrientations.S0][q] = mColorScheme.getBorderColor(QOrientations.S0, q) ;
		        color_qOrientationQPaneBorder[QOrientations.S1][q] = mColorScheme.getBorderColor(QOrientations.S1, q) ;
		        color_qOrientationQPaneBorder[QOrientations.S0_FROM_SL][q] = mColorScheme.getBorderColor(QOrientations.S0_FROM_SL, q) ;
		        color_qOrientationQPaneBorder[QOrientations.S1_FROM_SL][q] = mColorScheme.getBorderColor(QOrientations.S1_FROM_SL, q) ;
		        
		        
		        // Rainbow blocks use their own colors as borders
		        color_qOrientationQPaneBorder[QOrientations.R0][q] = mColorScheme.getBorderColor(QOrientations.R0, q) ;
		        color_qOrientationQPaneBorder[QOrientations.R1][q] = mColorScheme.getBorderColor(QOrientations.R1, q) ;
		        color_qOrientationQPaneBorder[QOrientations.R2][q] = mColorScheme.getBorderColor(QOrientations.R2, q) ;
		        color_qOrientationQPaneBorder[QOrientations.R3][q] = mColorScheme.getBorderColor(QOrientations.R3, q) ;
		        color_qOrientationQPaneBorder[QOrientations.R4][q] = mColorScheme.getBorderColor(QOrientations.R4, q) ;
		        color_qOrientationQPaneBorder[QOrientations.R5][q] = mColorScheme.getBorderColor(QOrientations.R5, q) ;
		        color_qOrientationQPaneBorder[QOrientations.R6][q] = mColorScheme.getBorderColor(QOrientations.R6, q) ;
		        color_qOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][q] = mColorScheme.getBorderColor(QOrientations.RAINBOW_BLAND, q) ;
		        color_qOrientationQPaneBorder[QOrientations.PUSH_DOWN][q] = mColorScheme.getBorderColor(QOrientations.PUSH_DOWN, q) ;
		        color_qOrientationQPaneBorder[QOrientations.PUSH_DOWN_ACTIVE][q] = mColorScheme.getBorderColor(QOrientations.PUSH_DOWN_ACTIVE, q) ;
		        color_qOrientationQPaneBorder[QOrientations.PUSH_UP_ACTIVE][q] = mColorScheme.getBorderColor(QOrientations.PUSH_UP_ACTIVE, q) ;
		        
		        
		        color_qOrientationQPaneBorder[QOrientations.ST][q] = mColorScheme.getBorderColor(QOrientations.ST, q) ;
			    color_qOrientationQPaneBorder[QOrientations.ST_INACTIVE][q] = mColorScheme.getBorderColor(QOrientations.ST, q) ;
			    color_qOrientationQPaneBorder[QOrientations.SL][q] = mColorScheme.getBorderColor(QOrientations.SL, q) ;
			    color_qOrientationQPaneBorder[QOrientations.SL_INACTIVE][q] = mColorScheme.getBorderColor(QOrientations.SL_INACTIVE, q) ;
			    color_qOrientationQPaneBorder[QOrientations.SL_ACTIVE][q] = mColorScheme.getBorderColor(QOrientations.SL_ACTIVE, q) ;
			    
		    }
		    
		    for ( int qp = 0; qp < 2; qp++ ) {
			    color_qOrientationQPaneBorder[QOrientations.F0][qp] = mColorScheme.getBorderColor(QOrientations.F0, qp) ;
			    color_qOrientationQPaneBorder[QOrientations.F1][qp] = mColorScheme.getBorderColor(QOrientations.F1, qp) ;
		    }
		    color_qOrientationQPaneBorder[QOrientations.U0][0] = mColorScheme.getBorderColor(QOrientations.S0, 0) ;
	        color_qOrientationQPaneBorder[QOrientations.U1][1] = mColorScheme.getBorderColor(QOrientations.S1, 1) ;
	        color_qOrientationQPaneBorder[QOrientations.UL][0] = mColorScheme.getBorderColor(QOrientations.S0, 0) ;
	        color_qOrientationQPaneBorder[QOrientations.UL][1] = mColorScheme.getBorderColor(QOrientations.S1, 1) ;
        }
        
        // HEY GUESS WHAT
        // If we're using LOW detail, we're going to border shine these
        // mofos ourselves!  Apply 25% border shine (white), the average shine
        // in MID detail.  The only exception is white borders (which normally
        // get a reversed black shine); we leave those as they are.
        if ( drawDetail == DrawSettings.DRAW_DETAIL_LOW ) {
	        for ( int qp = 0; qp < 2; qp++ ) {
	        	for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
	        		int color = color_qOrientationQPaneBorder[qo][qp] ;
	        		color = Color.argb(
	        				255,
	        				(int)Math.round( Color.red(color)*0.75f + 63.75),
	        				(int)Math.round( Color.green(color)*0.75f + 63.75),
	        				(int)Math.round( Color.blue(color)*0.75f + 63.75) ) ;
	        		color_qOrientationQPaneBorder[qo][qp] = color ;
	        	}
	        }
        } else {
        	// If not, we place a (very slight) shine on the borders by default, to
        	// help distinguish bottom-right border from bottom-right content.
        	for ( int qp = 0; qp < 2; qp++ ) {
	        	for ( int qo = QOrientations.R0; qo < QOrientations.R6+1; qo++ ) {
	        		int color = color_qOrientationQPaneBorder[qo][qp] ;
	        		color = Color.argb(
	        				255,
	        				(int)Math.round( Color.red(color)*0.95f ) + 13,
	        				(int)Math.round( Color.green(color)*0.95f) + 13,
	        				(int)Math.round( Color.blue(color)*0.95f) + 13 ) ;
	        		color_qOrientationQPaneBorder[qo][qp] = color ;
	        	}
	        	int color = color_qOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][qp] ;
        		color = Color.argb(
        				255,
        				(int)Math.round( Color.red(color)*0.95f) + 13,
        				(int)Math.round( Color.green(color)*0.95f) + 13,
        				(int)Math.round( Color.blue(color)*0.95f) + 13 ) ;
        		color_qOrientationQPaneBorder[QOrientations.RAINBOW_BLAND][qp] = color ;
	        }
        }
        
        // qCombinationShadows
        color_qOrientationQPaneFillShadow = new int[QOrientations.NUM][2] ;
        color_qOrientationQPaneDropShadow = new int[QOrientations.NUM][2] ;
        
        for ( byte qo = 0; qo < QOrientations.NUM; qo++ ) {
		    for ( int q = 0; q < 2; q++ ) {
		        color_qOrientationQPaneFillShadow[qo][q] = mColorScheme.getFillShadowColor(qo, q) ;
		        color_qOrientationQPaneDropShadow[qo][q] = mColorScheme.getDropShadowColor(qo, q) ;
		    }
        }
        
        color_qPaneBottomGuide = new int[2] ;
        
        for ( int qp = 0; qp < 2; qp++ ) {
        	color_qPaneBottomGuide[qp] = draw3D
        			? mColorScheme.getQuantroRowGuideColor(qp)
        			: mColorScheme.getRetroRowGuideColor(qp) ;
        }
	}
	
	
	public void setDefaultAlphaMults() {
        // qCombination backgrounds
        alphamult_qOrientationFill = new float[QOrientations.NUM] ;
        // By default, 1.0f.
        for ( int i = 0; i < QOrientations.NUM; i++ ) {
        	alphamult_qOrientationFill[i] = 1.0f ;
        }
        // 3D shapes get a minor tweak to reduce their alpha
        alphamult_qOrientationFill[QOrientations.ST] = 0.75f ;
        alphamult_qOrientationFill[QOrientations.ST_INACTIVE] = 0.75f ;
        /*	Holdover from when these pieces were 3D?
        alphamult_qOrientationFill[QOrientations.SL] = 0.75f ;
        alphamult_qOrientationFill[QOrientations.SL_ACTIVE] = 0.75f ;
        alphamult_qOrientationFill[QOrientations.SL_INACTIVE] = 0.75f ;
        alphamult_qOrientationFill[QOrientations.UL] = 0.75f ;
        alphamult_qOrientationFill[QOrientations.U0] = 0.75f ;
        alphamult_qOrientationFill[QOrientations.U1] = 0.75f ;
        */
        
        // If colorblind help is on, we use diagonal lines to indicate depth.  Scale back alpha to make
        // these lines a little more visible.
        if ( drawColorblindHelp ) {
        	alphamult_qOrientationFill[QOrientations.S0] = 0.5f ;
        	alphamult_qOrientationFill[QOrientations.S1] = 0.5f ;
        	alphamult_qOrientationFill[QOrientations.UL] = 0.5f ;
        	alphamult_qOrientationFill[QOrientations.U0] = 0.5f ;
        	alphamult_qOrientationFill[QOrientations.U1] = 0.5f ;
        	alphamult_qOrientationFill[QOrientations.S0_FROM_SL] = 0.5f ;
        	alphamult_qOrientationFill[QOrientations.S1_FROM_SL] = 0.5f ;
        }
        
        // qCombination borders
        alphamult_qOrientationBorder = new float[QOrientations.NUM] ;
        // By default, 1.0f.
        for ( int i = 0; i < QOrientations.NUM; i++ ) {
        	alphamult_qOrientationBorder[i] = 1.0f ;
        }
        
        
        // qCombination 3d edges
        alphamult_qOrientationEdge = new float[QOrientations.NUM] ;
        // By default, 1.0f.
        for ( int i = 0; i < QOrientations.NUM; i++ ) {
        	alphamult_qOrientationEdge[i] = 1.0f ;
        }
	}
	
	
	
	
	
	public void setDefaultBehaviors() {
		
		hashingVector_qPane = new float[][]{
			new float[]{ 1, -3 },
			new float[]{ 3, 1 }
		} ;
		
		behavior_background = BEHAVIOR_BACKGROUND_EMPTY_AND_PIECE ;
		
		behavior_border = BEHAVIOR_BORDER_NO_EDGES ;
		
		behavior_hashing = drawColorblindHelp
				? BEHAVIOR_HASHING_SOME
				: BEHAVIOR_HASHING_NONE ;
		
		behavior_align_vertical = BEHAVIOR_ALIGN_GRID ;
		behavior_align_horizontal = BEHAVIOR_ALIGN_GRID ;
		
		behavior_clear_emph = BEHAVIOR_CLEAR_EMPH_OLD_BORDERS ;
		
		behavior_displacement = BEHAVIOR_DISPLACEMENT_NO ;
		
		behavior_qo_fill = new int[QOrientations.NUM] ;
		// By default, use this qo's fill paint color.
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_fill[i] =  BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
		// Only "no" doesn't get painted at all.
		behavior_qo_fill[QOrientations.NO] = BEHAVIOR_QO_FILL_PAINT_NONE ;
		// Flashes get an "inset" fill for LOW detail, but are drawn with bitmaps
		// for anything higher (which goes in "TOP", so give no fill layer here)
		if ( drawDetail >= DRAW_DETAIL_MID ) {
			behavior_qo_fill[QOrientations.F0] = BEHAVIOR_QO_FILL_PAINT_NONE ;
			behavior_qo_fill[QOrientations.F1] = BEHAVIOR_QO_FILL_PAINT_NONE ;
		} else {
			behavior_qo_fill[QOrientations.F0] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_INSET ;
			behavior_qo_fill[QOrientations.F1] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_INSET ;
		}
		
		behavior_qo_fill[QOrientations.U0] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_MINI ;
		behavior_qo_fill[QOrientations.U1] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_MINI ;
		behavior_qo_fill[QOrientations.UL] = draw3D ? BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_MINI_NONOVERLAP : BEHAVIOR_QO_FILL_PAINT_NONE ;
		behavior_qo_fill[QOrientations.SL] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
		behavior_qo_fill[QOrientations.SL_ACTIVE] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
		behavior_qo_fill[QOrientations.SL_INACTIVE] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
		behavior_qo_fill[QOrientations.S0_FROM_SL] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_FRIENDLY_BLEND ;
		behavior_qo_fill[QOrientations.S1_FROM_SL] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION_FRIENDLY_BLEND ;
		
		// Pushes get an "inset" fill for LOW detail, but are drawn with bitmaps for
		// anything higher.
		behavior_qo_fill[QOrientations.PUSH_DOWN] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
		behavior_qo_fill[QOrientations.PUSH_DOWN_ACTIVE] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
		behavior_qo_fill[QOrientations.PUSH_UP_ACTIVE] = BEHAVIOR_QO_FILL_PAINT_Q_ORIENTATION ;
		
		behavior_qo_hashing = new int[QOrientations.NUM] ;
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) 
			behavior_qo_hashing[qo] = BEHAVIOR_QO_HASHING_NONE ;
		if ( drawColorblindHelp ) {
			// Everything we would expect to draw with the standard quantro fill colors
			// should get hashing.
			behavior_qo_hashing[QOrientations.S0]= BEHAVIOR_QO_HASHING_QPANE ;
			behavior_qo_hashing[QOrientations.S1]= BEHAVIOR_QO_HASHING_QPANE ;
			behavior_qo_hashing[QOrientations.U0]= BEHAVIOR_QO_HASHING_QPANE ;
			behavior_qo_hashing[QOrientations.U1]= BEHAVIOR_QO_HASHING_QPANE ;
			behavior_qo_hashing[QOrientations.UL]= BEHAVIOR_QO_HASHING_QPANE ;
			behavior_qo_hashing[QOrientations.S0_FROM_SL]= BEHAVIOR_QO_HASHING_QPANE ;
			behavior_qo_hashing[QOrientations.S1_FROM_SL]= BEHAVIOR_QO_HASHING_QPANE ;
		}
		
		behavior_qo_3d = new int[QOrientations.NUM] ;
		// By default, no 3D behavior.
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_3d[i] =  BEHAVIOR_QO_3D_NONE ;
	
		// Sticky blocks get extruded.
		behavior_qo_3d[QOrientations.ST] = BEHAVIOR_QO_3D_PAINT_EXTRUDED_QO_PULSE_BOX ;
		behavior_qo_3d[QOrientations.ST_INACTIVE] = BEHAVIOR_QO_3D_PAINT_BORDER_EXTRUDED_SIDES_ONLY ;
		if ( drawColorblindHelp ) {
			behavior_qo_3d[QOrientations.SL] = BEHAVIOR_QO_3D_PAINT_STANDARD_SMALL_PRISM_SIDES_ONLY ;
			behavior_qo_3d[QOrientations.SL_ACTIVE] = BEHAVIOR_QO_3D_PAINT_STANDARD_SMALL_PRISM_SIDES_ONLY ;
			behavior_qo_3d[QOrientations.SL_INACTIVE] = BEHAVIOR_QO_3D_PAINT_STANDARD_SMALL_PRISM_SIDES_ONLY ;
		}
		
		behavior_qo_top = new int[QOrientations.NUM] ;
		// By default, no top behavior.
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_top[i] =  BEHAVIOR_QO_TOP_NONE ;

		// SL pieces get linked explicitly with a prism if we are drawing colorblind help
		if ( drawColorblindHelp ) {
			// Everything we would expect to draw with the standard quantro fill colors
			// should get hashing.
			behavior_qo_top[QOrientations.SL] = BEHAVIOR_QO_TOP_PAINT_STANDARD_SMALL_PRISM_TOP ;
			behavior_qo_top[QOrientations.SL_ACTIVE] = BEHAVIOR_QO_TOP_PAINT_STANDARD_SMALL_PRISM_TOP ;
			behavior_qo_top[QOrientations.SL_INACTIVE] = BEHAVIOR_QO_TOP_PAINT_STANDARD_SMALL_PRISM_TOP ;
		}
		
		// Flash blocks and push blocks get a custom bitmap top, if using draw detail >= mid.
		if ( drawDetail >= DrawSettings.DRAW_DETAIL_MID ) {
			switch( mSkin.getGame() ) {
			case QUANTRO:
				// flash blocks.
				behavior_qo_top[QOrientations.F0] = BEHAVIOR_QO_TOP_CUSTOM_BITMAP ;
				behavior_qo_top[QOrientations.F1] = BEHAVIOR_QO_TOP_CUSTOM_BITMAP ;
				break ;
				
			case RETRO:
				switch( mSkin.getTemplate() ) {
				case STANDARD:
					// push blocks get the top-drawn
					// chevron stripe.  This has the appearance of a stripe
					// painted on the inside of the block; inner shadows are
					// then applied over it.
					behavior_qo_top[QOrientations.PUSH_DOWN] = BEHAVIOR_QO_TOP_CUSTOM_BITMAP ;
					behavior_qo_top[QOrientations.PUSH_DOWN_ACTIVE] = BEHAVIOR_QO_TOP_CUSTOM_BITMAP ;
					behavior_qo_top[QOrientations.PUSH_UP] = BEHAVIOR_QO_TOP_CUSTOM_BITMAP ;
					behavior_qo_top[QOrientations.PUSH_UP_ACTIVE] = BEHAVIOR_QO_TOP_CUSTOM_BITMAP ;
					break ;
					
				case NEON:
					// push blocks get the border-style
					// chevron stripe.  This has the appearance of the block border
					// cutting through the center of the block.  It matches border colors
					// and 
					behavior_qo_top[QOrientations.PUSH_DOWN] = BEHAVIOR_QO_TOP_NONE ;
					behavior_qo_top[QOrientations.PUSH_DOWN_ACTIVE] = BEHAVIOR_QO_TOP_NONE ;
					behavior_qo_top[QOrientations.PUSH_UP] = BEHAVIOR_QO_TOP_NONE ;
					behavior_qo_top[QOrientations.PUSH_UP_ACTIVE] = BEHAVIOR_QO_TOP_NONE ;
					break ;
				}
				break ;
			}
		} else {
			int index = getDefaultSettingIndex(mSkin) ;
			switch( index ) {
			case INDEX_RETRO_STANDARD:
			case INDEX_RETRO_STANDARD_AUSTERE:
			case INDEX_RETRO_STANDARD_SEVERE:
			case INDEX_RETRO_LIMBO:
				// Use the detail color.
				behavior_qo_top[QOrientations.PUSH_DOWN] = BEHAVIOR_QO_TOP_PAINT_DETAIL_FILL ;
				behavior_qo_top[QOrientations.PUSH_DOWN_ACTIVE] = BEHAVIOR_QO_TOP_PAINT_DETAIL_FILL ;
				behavior_qo_top[QOrientations.PUSH_UP_ACTIVE] = BEHAVIOR_QO_TOP_PAINT_DETAIL_FILL ;
				break ;
			
			case INDEX_RETRO_STANDARD_CLEAN:
			case INDEX_RETRO_NEON:
				// draw bland as a grey fill.
				behavior_qo_top[QOrientations.PUSH_DOWN] = BEHAVIOR_QO_TOP_PAINT_BLAND_FILL ;
				behavior_qo_top[QOrientations.PUSH_DOWN_ACTIVE] = BEHAVIOR_QO_TOP_PAINT_BLAND_FILL ;
				behavior_qo_top[QOrientations.PUSH_UP_ACTIVE] = BEHAVIOR_QO_TOP_PAINT_BLAND_FILL ;
				break ;
			}
		}
		
		behavior_qo_border = new int[QOrientations.NUM];
		behavior_qo_border_shine = new int[QOrientations.NUM] ;
		// By default, draw a normal border with a default shine.
		for ( int i = 0; i < QOrientations.NUM; i++ ) {
			behavior_qo_border[i] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER ;
			behavior_qo_border_shine[i] = BEHAVIOR_QO_BORDER_SHINE_NONE ;
		}
		// the exception is SL_ACTIVE, which uses a reverse border shine.
		behavior_qo_border_shine[QOrientations.SL_ACTIVE] = BEHAVIOR_QO_BORDER_SHINE_CUSTOM ;
		
		
		// EDIT: Implementing a new draw style for S0, S1; give it Q0 BORDER INNER.
		behavior_qo_border[QOrientations.S0] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER ;
		behavior_qo_border[QOrientations.S1] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER ;
		// Sticky blocks get an inner border now; the new draw style draws its 3D shape
		// WITHIN the border.
		behavior_qo_border[QOrientations.ST]= BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER_CLIQUE ; 
		behavior_qo_border[QOrientations.ST_INACTIVE]= BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER_CLIQUE ; 

		// Rainbow blocks get an inner border instead.
		for ( int i = QOrientations.R0; i <= QOrientations.R6; i++ )
			behavior_qo_border[i] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER ;
		behavior_qo_border[QOrientations.RAINBOW_BLAND] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER ;
		// Push blocks get the same, except it does not join.
		behavior_qo_border[QOrientations.PUSH_DOWN] = DrawSettings.BEHAVIOR_QO_BORDER_INNER ;
		behavior_qo_border[QOrientations.PUSH_DOWN_ACTIVE] = DrawSettings.BEHAVIOR_QO_BORDER_INNER ;
		behavior_qo_border[QOrientations.PUSH_UP_ACTIVE] = DrawSettings.BEHAVIOR_QO_BORDER_INNER ;
		
		
		// UL blocks have chain-link borders
		behavior_qo_border[QOrientations.UL] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI_CHAIN_LINK_HORIZONTAL_OVER ;
		behavior_qo_border[QOrientations.U0] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI ;
		behavior_qo_border[QOrientations.U1] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI ;
		
		// SL blocks either get a fancy gradient or are just normal colors.
		// Normal colors are already set; change for MID w/o colorblind help.
		if ( drawDetail >= DRAW_DETAIL_MID && !drawColorblindHelp ) {
			behavior_qo_border[QOrientations.SL] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY ;
			behavior_qo_border[QOrientations.S0_FROM_SL] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY_BLEND ;
			behavior_qo_border[QOrientations.S1_FROM_SL] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY_BLEND ;
		}
		
		// Flash blocks get an inset, which is QPANE is LOW but Q_ORIENTATION is MID.
		behavior_qo_border[QOrientations.F0] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INSET ;
		behavior_qo_border[QOrientations.F1] = BEHAVIOR_QO_BORDER_Q_ORIENTATION_INSET ;

		// Nothing has no border.
		behavior_qo_border[QOrientations.NO] = BEHAVIOR_QO_BORDER_NONE ; 
		
		
		behavior_qo_lock_glow = new int[QOrientations.NUM] ;
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_lock_glow[i] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		// For MID detail, everything glows except for a few pieces that
		// require special consideration.  Lately we are changing that to all draw detail levels.
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_lock_glow[i] = BEHAVIOR_QO_LOCK_GLOW_NORMAL ;
		
		behavior_qo_lock_glow[QOrientations.NO] = BEHAVIOR_QO_LOCK_GLOW_NONE ; 
		
		// We use 'lock glow' to indicate a lock, so unstable pieces
		// never have one.
		behavior_qo_lock_glow[QOrientations.UL] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		behavior_qo_lock_glow[QOrientations.U0] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		behavior_qo_lock_glow[QOrientations.U1] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		// SL pieces get special metamorphosis animations (PENDING); we don't
		// given them a lock glow.
		behavior_qo_lock_glow[QOrientations.SL] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		behavior_qo_lock_glow[QOrientations.S0_FROM_SL] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		behavior_qo_lock_glow[QOrientations.S1_FROM_SL] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		// Flashes have no glow; they just vanish.
		behavior_qo_lock_glow[QOrientations.F0] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		behavior_qo_lock_glow[QOrientations.F1] = BEHAVIOR_QO_LOCK_GLOW_NONE ;
		
		
		behavior_qo_pulse = new int[QOrientations.NUM] ;
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_pulse[i] = BEHAVIOR_QO_PULSE_NONE ;
		if ( drawAnimations == DRAW_ANIMATIONS_ALL ) {
			behavior_qo_pulse[QOrientations.SL_ACTIVE] = BEHAVIOR_QO_PULSE_FILL ;
		}
		
		behavior_qo_metamorphosis_from = new int[QOrientations.NUM] ;
		behavior_qo_metamorphosis_to = new int[QOrientations.NUM] ;
		for ( int i = 0; i < QOrientations.NUM; i++ ) {
			behavior_qo_metamorphosis_from[i] = BEHAVIOR_QO_METAMORPHOSIS_NONE ;
			behavior_qo_metamorphosis_to[i] = BEHAVIOR_QO_METAMORPHOSIS_NONE ;
		}
		if ( drawAnimations == DRAW_ANIMATIONS_ALL ) {
			// Most metamorphoses get a lock glow.
			behavior_qo_metamorphosis_from[QOrientations.S0_FROM_SL] = BEHAVIOR_QO_METAMORPHOSIS_LOCK_GLOW ;
			behavior_qo_metamorphosis_from[QOrientations.S1_FROM_SL] = BEHAVIOR_QO_METAMORPHOSIS_LOCK_GLOW ;
			behavior_qo_metamorphosis_to[QOrientations.SL_INACTIVE] = BEHAVIOR_QO_METAMORPHOSIS_LOCK_GLOW ;
			// Meta. to active gets a specialized glow.
			behavior_qo_metamorphosis_to[QOrientations.SL_ACTIVE] = BEHAVIOR_QO_METAMORPHOSIS_GLOW ;
			// ST gets a fade-out.
			behavior_qo_metamorphosis_from[QOrientations.ST] = DrawSettings.BEHAVIOR_QO_METAMORPHOSIS_SMOOTH ; 
		}
		
		behavior_qo_falling_chunk = new int [QOrientations.NUM] ;
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_falling_chunk[i] = BEHAVIOR_QO_FALLING_CHUNK_STANDARD ;
		// "flash" blocks get a different behavior; they rise and fade.
		behavior_qo_falling_chunk[QOrientations.F0] = BEHAVIOR_QO_FALLING_CHUNK_PIECE_TYPE_PIECE_COMPONENT_RISE_AND_FADE ;
		behavior_qo_falling_chunk[QOrientations.F1] = BEHAVIOR_QO_FALLING_CHUNK_PIECE_TYPE_PIECE_COMPONENT_RISE_AND_FADE ;
	
		
		behavior_qo_bitmap = new int [QOrientations.NUM] ;
		for ( int i = 0; i < QOrientations.NUM; i++ )
			behavior_qo_bitmap[i] = BEHAVIOR_QO_BITMAP_NONE ;
		if ( drawDetail >= DRAW_DETAIL_MID ) {
			switch( mSkin.getGame() ) {
			case QUANTRO:
				// flash blocks.
				behavior_qo_bitmap[QOrientations.F0] = BEHAVIOR_QO_BITMAP_FLASH_STRIPE ;
				behavior_qo_bitmap[QOrientations.F1] = BEHAVIOR_QO_BITMAP_FLASH_STRIPE ;
				break ;
				
			case RETRO:
				switch( mSkin.getTemplate() ) {
				case STANDARD:
					// push blocks get the top-drawn
					// chevron stripe.  This has the appearance of a stripe
					// painted on the inside of the block; inner shadows are
					// then applied over it.
					behavior_qo_bitmap[QOrientations.PUSH_DOWN] = BEHAVIOR_QO_BITMAP_CHEVRON_TOP ;
					behavior_qo_bitmap[QOrientations.PUSH_DOWN_ACTIVE] = BEHAVIOR_QO_BITMAP_CHEVRON_TOP ;
					behavior_qo_bitmap[QOrientations.PUSH_UP] = BEHAVIOR_QO_BITMAP_CHEVRON_TOP ;
					behavior_qo_bitmap[QOrientations.PUSH_UP_ACTIVE] = BEHAVIOR_QO_BITMAP_CHEVRON_TOP ;
					break ;
					
				case NEON:
					// push blocks get the border-style
					// chevron stripe.  This has the appearance of the block border
					// cutting through the center of the block.  It matches border colors
					// and 
					behavior_qo_bitmap[QOrientations.PUSH_DOWN] = BEHAVIOR_QO_BITMAP_CHEVRON_BORDER ;
					behavior_qo_bitmap[QOrientations.PUSH_DOWN_ACTIVE] = BEHAVIOR_QO_BITMAP_CHEVRON_BORDER ;
					behavior_qo_bitmap[QOrientations.PUSH_UP] = BEHAVIOR_QO_BITMAP_CHEVRON_BORDER ;
					behavior_qo_bitmap[QOrientations.PUSH_UP_ACTIVE] = BEHAVIOR_QO_BITMAP_CHEVRON_BORDER ;
					break ;
				}
				break ;
			}
		}
	}
	
	
	/**
	 * Filters all colors stored within to simulate the specified form
	 * of colorblindness.  If called several times
	 * with different forms, filters will applied in-sequence and the result
	 * will likely be nonsense.  Call no more than once on a DrawSetting
	 * instance or copy of one.
	 * 
	 * @param type
	 */
	public void applySimulatedColorblindness( int type ) {
		
		if ( type == COLORBLINDNESS_NONE )
			return ;
		
		color_background_piece = simulateColorblindness( color_background_piece, type ) ;
		
		
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			for ( int qp = 0; qp < 2; qp++ ) {
				color_qOrientationQPaneFill[qo][qp]
				        = simulateColorblindness( 
								color_qOrientationQPaneFill[qo][qp],
								type ) ;
				color_qOrientationQPaneBorder[qo][qp]
						= simulateColorblindness( 
								color_qOrientationQPaneBorder[qo][qp],
								type ) ;
				colorSimplified_qOrientationQPaneFill[qo][qp] =
					simulateColorblindness( 
							colorSimplified_qOrientationQPaneFill[qo][qp],
							type ) ;
				colorSimplified_qOrientationQPaneBorder[qo][qp] =
					simulateColorblindness( 
							colorSimplified_qOrientationQPaneBorder[qo][qp],
							type ) ;
				
			}
		}
		
		for ( int qo = 0; qo < QOrientations.NUM; qo++ ) {
			for ( int qp = 0; qp < 2; qp++ ) {
				if ( color_qOrientationQPaneCornerBorderShine[qo][qp] != null ) {
					for ( int j = 0; j < color_qOrientationQPaneCornerBorderShine[qo][qp].length; j++ ) {
						color_qOrientationQPaneCornerBorderShine[qo][qp][j] = 
							simulateColorblindness(
									color_qOrientationQPaneCornerBorderShine[qo][qp][j], type) ;
					}
				}
				
				color_qOrientationQPaneGlowColor[qo][qp] =
					simulateColorblindness(
							color_qOrientationQPaneGlowColor[qo][qp], type) ;
			}
			
			
			if ( color_qOrientationDetailColors[qo] != null ) {
				for ( int j = 0; j < color_qOrientationDetailColors[qo].length; j++ ) {
					color_qOrientationDetailColors[qo][j] = 
						simulateColorblindness(
								color_qOrientationDetailColors[qo][j], type) ;
				}
			}
		}
	}
	
	
	/**
	 * Returns whether the fill region of these two connect directly;
	 * if 'false', there is some border between them, or both or inset,
	 * or etc.
	 * @param qo1
	 * @param qo2
	 * @return
	 */
	public boolean fillRegionConnects( int qo1, int qo2 ) {
		switch( behavior_qo_border[qo1] ) {
		case BEHAVIOR_QO_BORDER_NONE:
			// dunno
			return false ;
		case BEHAVIOR_QO_BORDER_Q_PANE_OUTER:
			// blends with other Q_PANE_OUTERs.
			return behavior_qo_border[qo2] == BEHAVIOR_QO_BORDER_Q_PANE_OUTER ;
		case BEHAVIOR_QO_BORDER_Q_PANE_INSET:
			// inset; fill region won't touch
			return false ;
		case BEHAVIOR_QO_BORDER_Q_ORIENTATION_INNER:
			// blends with the exact same qOrientation.
			return qo1 == qo2 ;
		case BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY:
		case BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY_BLEND:
			// connects to other friendlies.
			return behavior_qo_border[qo2] == BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY
					|| behavior_qo_border[qo2] == BEHAVIOR_QO_BORDER_Q_ORIENTATION_FRIENDLY_BLEND ;
		case BEHAVIOR_QO_BORDER_Q_ORIENTATION_INSET:
		case BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI:
		case BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI_CHAIN_LINK_VERTICAL_OVER:
		case BEHAVIOR_QO_BORDER_Q_ORIENTATION_MINI_CHAIN_LINK_HORIZONTAL_OVER:
			// inset or small; fill regions don't even touch.
			return false ;
		default:
			return false ;
		}
	}
	
	
	public int getBlitBlockHeight() {
		return getSize_blockHeight() * scale;
	}
	
	public int getBlitBlockfieldWidth() {
		int width = Math.max(
				getBlockXPosition(0, COLS), 
				getBlockXPosition(1, COLS))
			- Math.min(
					getBlockXPosition(0, 0),
					getBlockXPosition(1, 0)) ;
		return width * scale;
	}
	
	public int getBlockXPosition( int qPane, int col ) {
		if ( blockXPositionCached_byQPane_X == null ) {
			blockXPositionCached_byQPane_X = new int[2+INDEX_OFFSET_QPANE][COLS+INDEX_OFFSET_X*2] ;
			for ( int i = 0; i < blockXPositionCached_byQPane_X.length; i++ )
				for ( int j = 0; j < blockXPositionCached_byQPane_X[i].length; j++ )
					blockXPositionCached_byQPane_X[i][j] = 
						getBlockXPositionWithoutCache( i - INDEX_OFFSET_QPANE, j - INDEX_OFFSET_X );
		}
		
		int cache_index_qPane = qPane + INDEX_OFFSET_QPANE ;
		int cache_index_col = col + INDEX_OFFSET_X ;
		
		return blockXPositionCached_byQPane_X[cache_index_qPane][cache_index_col] ;
	}
	
	public int getBlockXPositionWithoutCache( int qPane, int col ) {
		
		// Get the left edge of the qPane, add column-based offset.
		// leftOffset is the distance between the left edge of drawRegion and 
		// the left edge of the specified qPane.
		// This value is determined by taking the total space available and
		// subtracting the space for column display, then dividing the result
		// by two.  This gives the "offset" for a blockField with size_qXOffset = 0.
		// Finally, adjust left or right depending on size_qXOffset.
		// NOTE: Above description of procedure for getting leftOffset will
		// fail when blockSize is not grid-aligned, as when drawing with
		// exaggerated block sizes!  Use horizontalOffset explicitly.
		
		if ( qPane < 0 )
			return Math.min(getBlockXPositionWithoutCache(0, col), getBlockXPositionWithoutCache(1, col)) ;
		
		int leftOffset ;
		/*
		int blockSpace = getSize_blockWidth() * COLS ;
		if ( blockSpace < width )
			leftOffset = (int)Math.floor( ( width - blockSpace) / 2.0f + (qPane == 0 ? -1 : 1) * getSize_qXOffset()/2.0f ) ;
		else {
		*/
			// BUG: why are we dividing by 2 here?  I'm honestly not sure.
			leftOffset = Math.round(horizontalMargin) ;
			if ( qPane == 0 && getSize_qXOffset() < 0 || qPane == 1 && getSize_qXOffset() > 0 )
				leftOffset += Math.abs(getSize_qXOffset()) ;
			
		//}
		return leftOffset + getSize_blockWidth() * col ;
	}
	
	
	
	
	public int getBlockYPosition( int qPane, int row ) {
		if ( blockYPositionCached_byQPane_Y == null ) {
			blockYPositionCached_byQPane_Y = new int[2+INDEX_OFFSET_QPANE][ROWS+INDEX_OFFSET_Y*2] ;
			for ( int i = 0; i < blockYPositionCached_byQPane_Y.length; i++ )
				for ( int j = 0; j < blockYPositionCached_byQPane_Y[i].length; j++ )
					blockYPositionCached_byQPane_Y[i][j] = 
						getBlockYPositionWithoutCache( i - INDEX_OFFSET_QPANE, j - INDEX_OFFSET_Y );
		}
		
		int cache_index_qPane = qPane + INDEX_OFFSET_QPANE ;
		int cache_index_row = row + INDEX_OFFSET_Y ;
		
		return blockYPositionCached_byQPane_Y[cache_index_qPane][cache_index_row] ;
	}
	
	public int getBlockYPositionWithoutCache( int qPane, int row ) {
		// Similar to getBlockXPosition, except noting that we count
		// rows up from the bottom.
		
		// NOTE: Above description of procedure for getting leftOffset will
		// fail when blockSize is not grid-aligned, as when drawing with
		// exaggerated block sizes!  Use horizontalOffset explicitly.
		
		if ( qPane < 0 )
			return Math.min(getBlockYPositionWithoutCache(0, row), getBlockYPositionWithoutCache(1, row)) ;
		
		/*
		if ( behavior_align_vertical == BEHAVIOR_ALIGN_CENTER_BLOCKS || behavior_align_vertical == BEHAVIOR_ALIGN_CENTER_GRID ) {
			row = displayedRows - row - 1 ;
			
			int topOffset ;
			int blockSpace = getSize_blockHeight() * displayedRows ;
			if ( blockSpace < height ) {
				topOffset = (int)Math.floor( ( height - blockSpace) / 2.0f + (qPane == 0 ? -1 : 1) * getSize_qYOffset()/2.0f ) ;
			} else {
				// Why did we divide the YOffset by 2 in one direction but not the other?
				// No idea.  Remove the division.
				topOffset = Math.round(verticalMargin) ;
				if ( (qPane == 0 && getSize_qYOffset() < 0) || (qPane == 1 && getSize_qYOffset() > 0) )
					topOffset += Math.abs(getSize_qYOffset()) ;
			}
			return topOffset + getSize_blockHeight() * row ;
		} else {
		*/
			// Put row 0 at the very bottom, if we can.
			row += 1 ;
			int pos = height - verticalMargin - row * getSize_blockHeight() ;
			if ( qPane == 0 && getSize_qYOffset() > 0 )
				pos -= getSize_qYOffset() ;
			else if ( qPane == 1 && getSize_qYOffset() < 0 )
				pos += getSize_qYOffset() ;
			return pos ;
		//}
	}
	
	public static int simulateColorblindness( int color, int type ) {
		if ( type == COLORBLINDNESS_NONE )
			return color ;
		
		float r = Color.red(color) / 255.0f ;
		float g = Color.green(color) / 255.0f ;
		float b = Color.blue(color) / 255.0f ;
		
		if ( type == COLORBLINDNESS_ACHROMATOPSIA ) {
			int m = Math.round( 255 * (float)( 0.3*r + 0.59*g + 0.11*b ) ) ;
			return Color.argb(
					Color.alpha(color),
					m, m, m ) ;
		}
		
		float l, m, s ;
		float L = (17.8824f * r) + (43.5161f * g) + (4.11935f * b);
		float M = (3.45565f * r) + (27.1554f * g) + (3.86714f * b);
		float S = (0.0299566f * r) + (0.184309f * g) + (1.46709f * b);
		
		switch( type ) {
		case COLORBLINDNESS_PROTONOPIA:
			l = 0.0f * L + 2.02344f * M + -2.52581f * S;
			m = 0.0f * L + 1.0f * M + 0.0f * S;
			s = 0.0f * L + 0.0f * M + 1.0f * S;
			break ;
			
		case COLORBLINDNESS_DEUTERANOPIA:
			l = 1.0f * L + 0.0f * M + 0.0f * S;
			m = 0.494207f * L + 0.0f * M + 1.24827f * S;
			s = 0.0f * L + 0.0f * M + 1.0f * S;
			break ;
			
		case COLORBLINDNESS_TRITANOPIA:
			l = 1.0f * L + 0.0f * M + 0.0f * S;
			m = 0.0f * L + 1.0f * M + 0.0f * S;
			s = -0.395913f * L + 0.801109f * M + 0.0f * S;
			break ;
			
		default:
			return color ;
		}
		
		float R = (0.0809444479f * l) + (-0.130504409f * m) + (0.116721066f * s);
		float G = (-0.0102485335f * l) + (0.0540193266f * m) + (-0.113614708f * s);
		float B = (-0.000365296938f * l) + (-0.00412161469f * m) + (0.693511405f * s);
		
		return Color.argb(
				Color.alpha(color),
				Math.round( 255 * R ),
				Math.round( 255 * G ),
				Math.round( 255 * B )) ;
	}

	public void setBackground(Background background) {
		this.background = background;
	}

	public Background getBackground() {
		return background;
	}
	
	public Skin getSkin() {
		return mSkin ;
	}

	private void setSize_blockWidth(int size_blockWidth) {
		this.size_blockWidth = size_blockWidth;
	}

	public int getSize_blockWidth() {
		return size_blockWidth;
	}

	private int setSize_blockHeight(int size_blockHeight) {
		this.size_blockHeight = size_blockHeight;
		return size_blockHeight;
	}

	public int getSize_blockHeight() {
		return size_blockHeight;
	}

	public void setSize_qXOffset(int size_qXOffset) {
		this.size_qXOffset = size_qXOffset;
	}

	public int getSize_qXOffset() {
		return size_qXOffset;
	}

	public void setSize_qYOffset(int size_qYOffset) {
		this.size_qYOffset = size_qYOffset;
	}

	public int getSize_qYOffset() {
		return size_qYOffset;
	}
	
	public boolean getBehaviorIs_backgroundBlit() {
		return this.behavior_background == BEHAVIOR_BACKGROUND_BLIT_IMAGE ;
	}
	
	public boolean getBehaviorIs_displacement() {
		return this.behavior_displacement == BEHAVIOR_DISPLACEMENT_YES ;
	}
	
	public void setBehaviorIs_displacement( boolean disp ) {
		this.behavior_displacement = disp
				? BEHAVIOR_DISPLACEMENT_YES
				: BEHAVIOR_DISPLACEMENT_NO ;
		this.displayedRows = calculateDisplayedRows() ;
		resetDisplacementMargin() ;
	}
	
	public int getBlit() {
		return blit ;
	}
	
	public void setBlit( int blit, int scale ) {
		this.blit = blit ;
		this.scale = scale ;
		resetDisplacementMargin() ;
	}

	public int getScale() {
		return scale ;
	}
	
	/**
	 * Gets the border shine to be applied to the specified qpane/qorientation
	 * at the specified corner.
	 * 
	 * The shine returned has a meaningful alpha channel; it is the alpha
	 * value of the shine when applied at border opacity 255.
	 * 
	 * In cases with complex shines (i.e. a shine comprised of different
	 * color gradients in different directions) this method returns the
	 * "composite shine color" acquired after applying the shines in
	 * order.
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @param corner
	 * @return
	 */
	public int getColor_borderShine( int qPane, byte qOrientation, int corner ) {
		switch( behavior_qo_border_shine[qOrientation] ) {
		case BEHAVIOR_QO_BORDER_SHINE_CUSTOM:
			return this.color_qOrientationQPaneCornerBorderShine[qOrientation][qPane][corner] ;
		}
		
		return 0x00 ;		// no shine
	}
	
	/**
	 * Returns the NUMBER of distinct shines to be applied to this qPane/qO.
	 * 
	 * By default, shines are applied in-order to cover the complete border,
	 * edge-to-edge and corner-to-corner.  Future revisions may allow "parallel
	 * shines" but we don't bother thinking about that now.
	 * 
	 * Shines should be applied by iterating shineNum = { 0, ..., returnedVal-1 }.
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @return
	 */
	public int getNumber_borderShine( int qPane, byte qOrientation ) {
		switch( behavior_qo_border_shine[qOrientation] ) {
		case BEHAVIOR_QO_BORDER_SHINE_CUSTOM:
			return mColorScheme.getShineNumber(qOrientation, qPane) ;
		}
		
		return 0 ;		// no shine
	}
	
	
	/**
	 * Gets, as a multi-step gradient, the border shine to be applied to the
	 * specified border wall running from corner to corner.
	 * 
	 * The shine returned has a meaningful alpha channel; it is the alpha
	 * value of the shine when applied at border opacity 255.
	 * 
	 * The returned array, if non-null, will have at least 2 values.  The
	 * first value corresponds with the shine color at cornerFrom, the last
	 * is the shine color at cornerTo.
	 * 
	 * Do NOT modify the contents of the returned array.  Make a local copy
	 * if you plan on changing the values.
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @param cornerFrom
	 * @param cornerTo
	 * @return An array indicating the gradient colors for the specified border
	 * shine, or 'null' if no shine should be applied.
	 */
	public int [] getColor_borderShine( int qPane, byte qOrientation, int shineNum, int cornerFrom, int cornerTo ) {
		// remove when we support layered shines
		switch( behavior_qo_border_shine[qOrientation] ) {
		case BEHAVIOR_QO_BORDER_SHINE_CUSTOM:
			ColorScheme.ShineShape shineShape = mColorScheme.getShineShape(qOrientation, qPane, shineNum) ;
			float [] maxProportions = SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[shineShape.ordinal()][cornerFrom][cornerTo] ;
			
			int alphaMin = mColorScheme.getShineAlphaMin(qOrientation, qPane, shineNum) ;
			int alphaMax = mColorScheme.getShineAlphaMax(qOrientation, qPane, shineNum) ;
			int color = mColorScheme.getShineColor(qOrientation, qPane, shineNum) ;
			
			int [] shineColors = new int[ maxProportions.length ] ;
			
			for ( int i = 0; i < shineColors.length; i++ ) {
				float p = maxProportions[i] ;		// this is the proportion to MAX from MIN alpha.
				int alpha = alphaMin + Math.round( p * ( alphaMax - alphaMin ) ) ;
				shineColors[i] = ColorOp.setAlphaForColor(alpha, color) ;
			}
			
			return shineColors ;
		}
		
		return null ;		// no shine
	}
	
	
	/**
	 * Returns the "positions" for drawing gradient colors, suitable for use
	 * in the LinearGradient constructor.
	 * 
	 * Provides the positions for the color array returned by
	 * getColor_borderShine( ... ) called with the same arguments.
	 * 
	 * If getColor_* returns null, so will this method.
	 * 
	 * If getColor_* returns non-null, then this method will either return
	 * a float array the same length as getColor_*'s return, or 'null',
	 * indicating that getColor_*'s return has uniform placement.  In either
	 * case it can be passed to the LinearGradient constructor (note that
	 * the constructor accepts 'null' positions to indicate uniform placement
	 * as well).
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @param shineNum
	 * @param cornerFrom
	 * @param cornerTo
	 * @return
	 */
	public float [] getPositions_borderShine( int qPane, byte qOrientation, int shineNum, int cornerFrom, int cornerTo ) {
		// remove when we support layered shines
		switch( behavior_qo_border_shine[qOrientation] ) {
		case BEHAVIOR_QO_BORDER_SHINE_CUSTOM:
			ColorScheme.ShineShape shineShape = mColorScheme.getShineShape(qOrientation, qPane, shineNum) ;
			float [] positions = SHINE_POSITION_BY_SHAPE_CORNER_CORNER[shineShape.ordinal()][cornerFrom][cornerTo] ;
			
			return positions == null ? null : positions.clone() ;
		}
		
		return null ;		// no shine
	}
	
	
	/**
	 * Gets the border shine to be applied to the specified qpane/qorientation
	 * at the specified corner.
	 * 
	 * The shine returned has a meaningful alpha channel; it is the alpha
	 * value of the shine when applied at border opacity 255.
	 * 
	 * In cases with complex shines (i.e. a shine comprised of different
	 * color gradients in different directions) this method returns the
	 * "composite shine color" acquired after applying the shines in
	 * order.
	 * 
	 * @param qPane
	 * @param corner
	 * @return
	 */
	public int getColor_rowGuideBorderShine( int qPane, int corner ) {
		return this.color_qPaneCornerBottomGuideShine[qPane][corner] ;
	}
	
	/**
	 * Returns the NUMBER of distinct shines to be applied to this qPane/qO.
	 * 
	 * By default, shines are applied in-order to cover the complete border,
	 * edge-to-edge and corner-to-corner.  Future revisions may allow "parallel
	 * shines" but we don't bother thinking about that now.
	 * 
	 * Shines should be applied by iterating shineNum = { 0, ..., returnedVal-1 }.
	 * 
	 * @param qPane
	 * @param qOrientation
	 * @return
	 */
	public int getNumber_rowGuideBorderShine( int qPane, byte qOrientation ) {
		if ( mSkin.getGame() == Skin.Game.QUANTRO )
			return mColorScheme.getQuantroRowGuideShineNumber(qPane) ;
		else
			return mColorScheme.getRetroRowGuideShineNumber(qPane) ;
	}
	
	
	/**
	 * Gets, as a multi-step gradient, the border shine to be applied to the
	 * specified border wall running from corner to corner.
	 * 
	 * The shine returned has a meaningful alpha channel; it is the alpha
	 * value of the shine when applied at border opacity 255.
	 * 
	 * The returned array, if non-null, will have at least 2 values.  The
	 * first value corresponds with the shine color at cornerFrom, the last
	 * is the shine color at cornerTo.
	 * 
	 * Do NOT modify the contents of the returned array.  Make a local copy
	 * if you plan on changing the values.
	 * 
	 * @param qPane
	 * @param cornerFrom
	 * @param cornerTo
	 * @return An array indicating the gradient colors for the specified border
	 * shine, or 'null' if no shine should be applied.
	 */
	public int [] getColor_rowGuideBorderShine( int qPane, int shineNum, int cornerFrom, int cornerTo ) {
		// remove when we support layered shines
		ColorScheme.ShineShape shineShape ;
		int alphaMin, alphaMax ;
		int color ;
		if ( mSkin.getGame() == Skin.Game.QUANTRO ) {
			shineShape = mColorScheme.getQuantroRowGuideShineShape(qPane, shineNum) ;
			alphaMin = mColorScheme.getQuantroRowGuideShineAlphaMin(qPane, shineNum) ;
			alphaMax = mColorScheme.getQuantroRowGuideShineAlphaMax(qPane, shineNum) ;
			color = mColorScheme.getQuantroRowGuideShineColor(qPane, shineNum) ;
		} else {
			shineShape = mColorScheme.getRetroRowGuideShineShape(qPane, shineNum) ;
			alphaMin = mColorScheme.getRetroRowGuideShineAlphaMin(qPane, shineNum) ;
			alphaMax = mColorScheme.getRetroRowGuideShineAlphaMax(qPane, shineNum) ;
			color = mColorScheme.getRetroRowGuideShineColor(qPane, shineNum) ;
		}
		
		float [] maxProportions = SHINE_PROPORTION_BY_SHAPE_CORNER_CORNER[shineShape.ordinal()][cornerFrom][cornerTo] ;
		
		int [] shineColors = new int[ maxProportions.length ] ;
		
		for ( int i = 0; i < shineColors.length; i++ ) {
			float p = maxProportions[i] ;		// this is the proportion to MAX from MIN alpha.
			int alpha = alphaMin + Math.round( p * ( alphaMax - alphaMin ) ) ;
			shineColors[i] = ColorOp.setAlphaForColor(alpha, color) ;
		}
		
		return shineColors ;
	}
	
	
	/**
	 * Returns the "positions" for drawing gradient colors, suitable for use
	 * in the LinearGradient constructor.
	 * 
	 * Provides the positions for the color array returned by
	 * getColor_borderShine( ... ) called with the same arguments.
	 * 
	 * If getColor_* returns null, so will this method.
	 * 
	 * If getColor_* returns non-null, then this method will either return
	 * a float array the same length as getColor_*'s return, or 'null',
	 * indicating that getColor_*'s return has uniform placement.  In either
	 * case it can be passed to the LinearGradient constructor (note that
	 * the constructor accepts 'null' positions to indicate uniform placement
	 * as well).
	 * 
	 * @param qPane
	 * @param shineNum
	 * @param cornerFrom
	 * @param cornerTo
	 * @return
	 */
	public float [] getPositions_rowGuideBorderShine( int qPane, int shineNum, int cornerFrom, int cornerTo ) {
		// remove when we support layered shines
		ColorScheme.ShineShape shineShape ;
		if ( mSkin.getGame() == Skin.Game.QUANTRO )
			shineShape = mColorScheme.getQuantroRowGuideShineShape(qPane, shineNum) ;
		else
			shineShape = mColorScheme.getRetroRowGuideShineShape(qPane, shineNum) ;
			
		float [] positions = SHINE_POSITION_BY_SHAPE_CORNER_CORNER[shineShape.ordinal()][cornerFrom][cornerTo] ;
		
		return positions == null ? null : positions.clone() ;
	}
	
}
