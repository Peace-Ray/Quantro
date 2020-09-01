package com.peaceray.quantro.view.game;

import java.util.ArrayList;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import com.peaceray.quantro.R;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.model.pieces.history.PieceHistory;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.q.QOrientations;

public class TooltipDrawer {
	
	private static final String TAG = "TooltipDrawer" ;
	
	//////////////////PIECE CATEGORIES
	// tetromino categories
	protected static final int INDEX_CATEGORY_TETRO_LINE 	= 0 ;
	protected static final int INDEX_CATEGORY_TETRO_GAMMA 	= 1 ;
	protected static final int INDEX_CATEGORY_TETRO_GUN	 	= 2 ;
	protected static final int INDEX_CATEGORY_TETRO_SQUARE 	= 3 ;
	protected static final int INDEX_CATEGORY_TETRO_S	 	= 4 ;
	protected static final int INDEX_CATEGORY_TETRO_Z	 	= 5 ;
	protected static final int INDEX_CATEGORY_TETRO_T	 	= 6 ;
	// tetracube categories
	protected static final int INDEX_CATEGORY_TETRA_L		= 7 ;
	protected static final int INDEX_CATEGORY_TETRA_RECT	= 8 ;
	protected static final int INDEX_CATEGORY_TETRA_ZIG_ZAG	= 9 ;
	protected static final int INDEX_CATEGORY_TETRA_3DT		= 10 ;
	protected static final int INDEX_CATEGORY_TETRA_BRANCH	= 11 ;
	protected static final int INDEX_CATEGORY_TETRA_SCREW	= 12 ;
	protected static final int INDEX_CATEGORY_TETRA_DEXTER 	= 13 ;
	protected static final int INDEX_CATEGORY_TETRA_SINISTER= 14 ;
	// special categories
	protected static final int INDEX_CATEGORY_SPECIAL_FLASH = 15 ;
	protected static final int INDEX_CATEGORY_SPECIAL_PUSH 	= 16 ;
	protected static final int INDEX_CATEGORY_SPECIAL_NONE  = 17 ;
	// tromino categories
	protected static final int INDEX_CATEGORY_TRO_LINE		= 18 ;
	protected static final int INDEX_CATEGORY_TRO_V			= 19 ;
	// pentomino categories
	protected static final int INDEX_CATEGORY_PENTO_F = 20 ;
	protected static final int INDEX_CATEGORY_PENTO_I = 21 ;
	protected static final int INDEX_CATEGORY_PENTO_L = 22 ;
	protected static final int INDEX_CATEGORY_PENTO_N = 23 ;
	protected static final int INDEX_CATEGORY_PENTO_P = 24 ;
	protected static final int INDEX_CATEGORY_PENTO_T = 25 ;
	protected static final int INDEX_CATEGORY_PENTO_U = 26 ;
	protected static final int INDEX_CATEGORY_PENTO_V = 27 ;
	protected static final int INDEX_CATEGORY_PENTO_W = 28 ;
	protected static final int INDEX_CATEGORY_PENTO_X = 29 ;
	protected static final int INDEX_CATEGORY_PENTO_Y = 30 ;
	protected static final int INDEX_CATEGORY_PENTO_Z = 31 ;
	protected static final int INDEX_CATEGORY_PENTO_F_REVERSE = 32 ;
	protected static final int INDEX_CATEGORY_PENTO_L_REVERSE = 33 ;
	protected static final int INDEX_CATEGORY_PENTO_N_REVERSE = 34 ;
	protected static final int INDEX_CATEGORY_PENTO_P_REVERSE = 35 ;
	protected static final int INDEX_CATEGORY_PENTO_Y_REVERSE = 36 ;
	protected static final int INDEX_CATEGORY_PENTO_Z_REVERSE = 37 ;
	
	protected static final int NUM_INDEX_CATEGORIES  = 38 ;
	
	////////////////// QCombination
	protected static final int INDEX_QCOMBINATION_S0	 	= 0 ;
	protected static final int INDEX_QCOMBINATION_S1	 	= 1 ;
	protected static final int INDEX_QCOMBINATION_SS	 	= 2 ;
	protected static final int INDEX_QCOMBINATION_ST	 	= 3 ;
	protected static final int INDEX_QCOMBINATION_UL	 	= 4 ;
	protected static final int INDEX_QCOMBINATION_SL	 	= 5 ;
	protected static final int INDEX_QCOMBINATION_F	 		= 6 ;
	protected static final int INDEX_QCOMBINATION_PUSH		= 7 ;
	protected static final int INDEX_QCOMBINATION_NONE 		= 8 ;
	protected static final int NUM_INDEX_QCOMBINATIONS 		= 9 ;
	
	
	//////////////////TOOLTIP POLICIES
	public static final int POLICY_FIRST_QC = 0 ;
	public static final int POLICY_AFTER_FAILURE_QC = 1 ;
	public static final int POLICY_AFTER_NON_SUCCESS_QC = 2 ;
	public static final int POLICY_EVERY_QC = 3 ;
	public static final int NUM_POLICIES = 4 ;
	
	private Resources mResources ;
	private boolean [][] mPolicyQC ;
	private Rect mDrawRegion ;
	private boolean mLabelColors ;
	
	private long mTooltipLingerMilliseconds ;		// if positive, gives the # of milliseconds to linger before dismissing
	private long mTooltipDismissMilliseconds ;		// the # of milliseconds to "dismiss" a tooltip.
	// Tooltips have two sections: header and body.  They get indexed
	// by category (as listed above) and qcombination.
	private String [][][] mTextHeader ;		// an array of lines.  Can't rely on newline characters; Android fails to render
	private String [][][] mTextBody ;		// an array of lines.  Can't rely on newline characters; Android fails to render
	
	private String [][][] mTextHeaderWords ;	// an array of words
	private String [][][] mTextBodyWords ;		// an array of words; those separated by whitespace
	
	private String [] mCurrentHeader ;
	private String [] mCurrentBody ;
	
	private Rect mBoundsHeader = new Rect() ;
	private Rect mBoundsBody = new Rect() ;
	
	Paint [] mPaintHeader ;
	Paint [] mPaintBody ;
	Paint [] mPaintSeparator ;
	Paint [][] mPaintArrays ;
	
	float [] mShadowRadius ;
	float [] mShadowX ;
	float [] mShadowY ;
	int [] mShadowColor ;
	
	Rect mTextBounds ;
	
	private int mTextSizeHeader ;
	private int mTextSizeBody ;
	private int mMarginTextHorizontal ;
	private int mMarginSeparatorHorizontal ;
	private int mMarginSeparatorVertical ;
	private int mHeightSeparator ;
	
	boolean mCurrentDismissed ;
	long mCurrentMillisecondsSpentDisplaying ;
	long mCurrentTimeWhenDisplayed ;
	

	public TooltipDrawer( Resources res, Rect drawRegion, boolean bigText ) {
		mResources = res ;
		mPolicyQC = new boolean[NUM_POLICIES][QCombinations.NUM] ;
		mDrawRegion = new Rect(drawRegion) ;
		mLabelColors = true ;
		
		mTextBounds = new Rect() ;
		
		mTooltipLingerMilliseconds = 0 ;
		mTooltipDismissMilliseconds = 1000 ;
		
		mTextHeader = new String[NUM_INDEX_CATEGORIES][NUM_INDEX_QCOMBINATIONS][] ;
		mTextBody = new String[NUM_INDEX_CATEGORIES][NUM_INDEX_QCOMBINATIONS][] ;
		
		mTextHeaderWords = new String[NUM_INDEX_CATEGORIES][NUM_INDEX_QCOMBINATIONS][] ;
		mTextBodyWords = new String[NUM_INDEX_CATEGORIES][NUM_INDEX_QCOMBINATIONS][] ;
		
		mCurrentHeader = mCurrentBody = null ;
		
		createPaints(bigText) ;
		loadTooltipText() ;
		int timesShrunk = formatToFit() ;
		if ( timesShrunk > 0 )
			Log.d(TAG, "Shrunk text " + timesShrunk + " times to fit") ;
	}
	
	public void setDrawRegion( Rect drawRegion ) {
		mDrawRegion.set(drawRegion) ;
		
		// reset text sizes
		for ( int j = 0; j < mPaintHeader.length; j++ ) {
			mPaintHeader[j].setTextSize(mTextSizeHeader) ;
			mPaintBody[j].setTextSize(mTextSizeBody) ;
		}
		
		int timesShrunk = formatToFit() ;
		if ( timesShrunk > 0 )
			Log.d(TAG, "Shrunk text " + timesShrunk + " times to fit") ;
	}
	
	public void setLabelColors( boolean labelColors ) {
		if ( mLabelColors != labelColors ) {
			mLabelColors = labelColors ;
			loadTooltipText() ;
			int timesShrunk = formatToFit() ;
			if ( timesShrunk > 0 )
				Log.d(TAG, "Shrunk text " + timesShrunk + " times to fit") ;
		}
	}
	
	public void clearPolicies() {
		for ( int i = 0; i < NUM_POLICIES; i++ )
			for ( int j = 0; j < QCombinations.NUM; j++ )
				mPolicyQC[i][j] = false ;
	}
	
	public void addPolicy( int policy, int qcombination ) {
		mPolicyQC[policy][qcombination] = true ;
	}
	
	/**
	 * The most basic update method.  Informs the TooltipDrawer that a new piece
	 * has entered, provides that piece type, as well as our piece history to determine
	 * if our policies apply.
	 * 
	 * We apply our policies to determine if this new piece deserves a tooltip.  If so,
	 * we will display that tooltip with subsequent calls to drawToCanvas.  Tooltips
	 * will eventually fade away, and the call drawToCanvas is very efficient if no
	 * tooltips exist to be displayed.
	 * 
	 * If a tooltip was previously displayed, this method serves as a cue to dismiss it
	 * (assuming it did not already expire according to our own machinations).  Should
	 * the new piece not prompt a tooltip, we will smoothly fade the previous.  Should
	 * the new piece require a tooltip of its own, it will immediately appear
	 * (likely replacing the last one).
	 * 
	 * 
	 * @param pieceType
	 * @param pieceHistory
	 */
	public void pieceEntered( int pieceType, PieceHistory pieceHistory ) {
		// dismiss previous if not already
		if ( !mCurrentDismissed ) {
			mCurrentDismissed = true ;
			mCurrentMillisecondsSpentDisplaying = 0 ;
			mCurrentTimeWhenDisplayed = System.currentTimeMillis() ;
		}
		
		if ( pieceType < 0 )
			return ;
		int qc = PieceCatalog.getQCombination(pieceType) ;
		
		// check policies
		boolean tooltip = false ;
		tooltip = tooltip || ( mPolicyQC[POLICY_FIRST_QC][qc] && pieceHistory.getNumberLandedWithQCombination(qc) == 0 ) ;
		tooltip = tooltip || ( mPolicyQC[POLICY_AFTER_FAILURE_QC][qc] && pieceHistory.getDidFailLastWithQCombination(qc) ) ;
		tooltip = tooltip || ( mPolicyQC[POLICY_AFTER_NON_SUCCESS_QC][qc] && !pieceHistory.getDidSucceedLastWithQCombination(qc) ) ;
		tooltip = tooltip || ( mPolicyQC[POLICY_EVERY_QC][qc] ) ;
		
		if ( tooltip ) {
			mCurrentDismissed = false ;
			mCurrentMillisecondsSpentDisplaying = 0 ;
			mCurrentTimeWhenDisplayed = System.currentTimeMillis() ;
			
			int catIndex = getCategoryIndex( pieceType) ;
			int qcIndex = getQCombinationIndex( pieceType ) ;
			
			mCurrentHeader = mTextHeader[catIndex][qcIndex] ;
			mCurrentBody = mTextBody[catIndex][qcIndex] ;
		}
	}
	
	
	public void drawToCanvas( Canvas canvas, long millisSinceLastDraw ) {
		mCurrentMillisecondsSpentDisplaying += millisSinceLastDraw ;
		if ( !mCurrentDismissed
				&& mTooltipLingerMilliseconds > 0
				&& mCurrentMillisecondsSpentDisplaying > mTooltipLingerMilliseconds ) {
			mCurrentDismissed = true ;
			mCurrentMillisecondsSpentDisplaying = 0 ;
			mCurrentTimeWhenDisplayed = System.currentTimeMillis() ;
		}
		
		// First: determine if we have any tooltips to draw.
		if ( mCurrentHeader == null || mCurrentBody == null )
			return ;
		if ( mCurrentHeader.length == 0 && mCurrentBody.length == 0 )
			return ;
		if ( mCurrentDismissed && mCurrentMillisecondsSpentDisplaying > mTooltipDismissMilliseconds )
			return ;
		
		// Second: determine the total height needed to store everything.  This is
		// the summed height of each line of time, plus the separator height, plus
		// 2 times the separator vertical margin.  We do this so we can center the
		// whole thing within our draw area.
		int totalHeight
				= mBoundsHeader.height() * mCurrentHeader.length
				+ mBoundsBody.height() * mCurrentBody.length ;
		totalHeight += mHeightSeparator + 2 * mMarginSeparatorVertical ;
		
		// Third: prepare by setting alpha
		for ( int i = 0; i < mPaintArrays.length; i++ ) {
			for ( int j = mPaintArrays[i].length - 1; j >= 0; j-- ) {
				if ( !mCurrentDismissed ) {
					mPaintArrays[i][j].setAlpha(255) ;
					mPaintArrays[i][j].setShadowLayer(
							mShadowRadius[j],
							mShadowX[j],
							mShadowY[j],
							0xff000000 | ( mShadowColor[j] & 0x00ffffff ) ) ;
				} else if ( mCurrentMillisecondsSpentDisplaying < mTooltipDismissMilliseconds ) {
					int alpha = Math.round( 255 * ( (float)(mTooltipDismissMilliseconds - mCurrentMillisecondsSpentDisplaying) ) / mTooltipDismissMilliseconds ) ;
					int alphaC = Color.argb(alpha, 0, 0, 0) ;
					mPaintArrays[i][j].setAlpha(alpha) ;
					mPaintArrays[i][j].setShadowLayer(
							mShadowRadius[j],
							mShadowX[j],
							mShadowY[j],
							alphaC | ( mShadowColor[j] & 0x00ffffff ) ) ;
				} else {
					mPaintArrays[i][j].setAlpha(0) ;
					mPaintArrays[i][j].setShadowLayer(
							mShadowRadius[j],
							mShadowX[j],
							mShadowY[j],
							0 ) ;
					// nothing to draw!
					return ;
				}
			}
		}
		
		// Forth: draw text and separator.
		canvas.save() ;
		canvas.clipRect(mDrawRegion) ;
		
		int y = (mDrawRegion.height() - totalHeight)/2 + mDrawRegion.top ;
		int x = mDrawRegion.centerX() ;
		
		// header...
		for ( int i = 0; i < mCurrentHeader.length; i++ ) {
			y -= mBoundsHeader.top ;
			for ( int j = mPaintHeader.length-1; j >= 0; j-- )
				canvas.drawText(mCurrentHeader[i], x, y, mPaintHeader[j]) ;
			y += mBoundsHeader.bottom ;
		}
		// separator...
		y += mMarginSeparatorVertical ;
		for ( int j = mPaintSeparator.length-1; j >= 0; j-- )
			canvas.drawLine(
					mDrawRegion.left + mMarginSeparatorHorizontal,
					y,
					mDrawRegion.right - mMarginSeparatorHorizontal,
					y, mPaintSeparator[j]) ;
		y += mMarginSeparatorVertical ;
		y += mHeightSeparator ;
		// body...
		for ( int i = 0; i < mCurrentBody.length; i++ ) {
			y -= mBoundsBody.top ;
			for ( int j = mPaintBody.length-1; j >= 0; j-- )
				canvas.drawText(mCurrentBody[i], x, y, mPaintBody[j]) ;
			y += mBoundsBody.bottom ;
		}
		
		canvas.restore() ;
	}
	
	
	protected int getCategoryIndex( int type ) {
		if ( PieceCatalog.isTetromino(type) ) {
			int cat = PieceCatalog.getTetrominoCategory(type) ;
			switch( cat ) {
			case PieceCatalog.TETRO_CAT_LINE:
				return INDEX_CATEGORY_TETRO_LINE ;
			case PieceCatalog.TETRO_CAT_GAMMA:
				return INDEX_CATEGORY_TETRO_GAMMA ;
			case PieceCatalog.TETRO_CAT_GUN:
				return INDEX_CATEGORY_TETRO_GUN ;
			case PieceCatalog.TETRO_CAT_SQUARE:
				return INDEX_CATEGORY_TETRO_SQUARE ;
			case PieceCatalog.TETRO_CAT_S:
				return INDEX_CATEGORY_TETRO_S ;
			case PieceCatalog.TETRO_CAT_Z:
				return INDEX_CATEGORY_TETRO_Z ;
			case PieceCatalog.TETRO_CAT_T:
				return INDEX_CATEGORY_TETRO_T ;
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		} else if ( PieceCatalog.isTetracube(type) ) {
			int cat = PieceCatalog.getTetracubeCategory(type) ;
			int scat = PieceCatalog.getTetracubeSubcategory(type) ;
			switch( cat ) {
			case PieceCatalog.TETRA_CAT_L:
				return INDEX_CATEGORY_TETRA_L ;
			case PieceCatalog.TETRA_CAT_RECT:
				return INDEX_CATEGORY_TETRA_RECT ;
			case PieceCatalog.TETRA_CAT_S:
				return INDEX_CATEGORY_TETRA_ZIG_ZAG ;
			case PieceCatalog.TETRA_CAT_T:
				return INDEX_CATEGORY_TETRA_3DT ;
			case PieceCatalog.TETRA_CAT_BRANCH:
				return INDEX_CATEGORY_TETRA_BRANCH ;
			case PieceCatalog.TETRA_CAT_SCREW:
				return INDEX_CATEGORY_TETRA_SCREW ;
			case PieceCatalog.TETRA_CAT_CORNER:
				if ( scat == 0 || scat == 3 )
					return INDEX_CATEGORY_TETRA_SINISTER ;
				else
					return INDEX_CATEGORY_TETRA_DEXTER ;	
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		} else if ( PieceCatalog.isSpecial(type) ) {
			int cat = PieceCatalog.getSpecialCategory(type) ;
			switch ( cat ) {
			case PieceCatalog.SPECIAL_CAT_FLASH:
				return INDEX_CATEGORY_SPECIAL_FLASH ;
			case PieceCatalog.SPECIAL_CAT_PUSH_DOWN:
				return INDEX_CATEGORY_SPECIAL_PUSH ;
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		} else if ( PieceCatalog.isTromino(type) ) {
			int cat = PieceCatalog.getTrominoCategory(type) ;
			switch( cat ) {
			case PieceCatalog.TRO_CAT_LINE:
				return INDEX_CATEGORY_TRO_LINE ;
			case PieceCatalog.TRO_CAT_V:
				return INDEX_CATEGORY_TRO_V ;
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		} else if ( PieceCatalog.isPentomino(type) ) {
			int cat = PieceCatalog.getPentominoCategory(type) ;
			switch( cat ) {
			case PieceCatalog.PENTO_CAT_F:
				return INDEX_CATEGORY_PENTO_F ;
			case PieceCatalog.PENTO_CAT_I:
				return INDEX_CATEGORY_PENTO_I ;
			case PieceCatalog.PENTO_CAT_L:
				return INDEX_CATEGORY_PENTO_L ;
			case PieceCatalog.PENTO_CAT_N:
				return INDEX_CATEGORY_PENTO_N ;
			case PieceCatalog.PENTO_CAT_P:
				return INDEX_CATEGORY_PENTO_P ;
			case PieceCatalog.PENTO_CAT_T:
				return INDEX_CATEGORY_PENTO_T ;
			case PieceCatalog.PENTO_CAT_U:
				return INDEX_CATEGORY_PENTO_U ;
			case PieceCatalog.PENTO_CAT_V:
				return INDEX_CATEGORY_PENTO_V ;
			case PieceCatalog.PENTO_CAT_W:
				return INDEX_CATEGORY_PENTO_W ;
			case PieceCatalog.PENTO_CAT_X:
				return INDEX_CATEGORY_PENTO_X ;
			case PieceCatalog.PENTO_CAT_Y:
				return INDEX_CATEGORY_PENTO_Y ;
			case PieceCatalog.PENTO_CAT_Z:
				return INDEX_CATEGORY_PENTO_Z ;
			case PieceCatalog.PENTO_CAT_F_REVERSE:
				return INDEX_CATEGORY_PENTO_F_REVERSE ;
			case PieceCatalog.PENTO_CAT_L_REVERSE:
				return INDEX_CATEGORY_PENTO_L_REVERSE ;
			case PieceCatalog.PENTO_CAT_N_REVERSE:
				return INDEX_CATEGORY_PENTO_N_REVERSE ;
			case PieceCatalog.PENTO_CAT_P_REVERSE:
				return INDEX_CATEGORY_PENTO_P_REVERSE ;
			case PieceCatalog.PENTO_CAT_Y_REVERSE:
				return INDEX_CATEGORY_PENTO_Y_REVERSE ;
			case PieceCatalog.PENTO_CAT_Z_REVERSE:
				return INDEX_CATEGORY_PENTO_Z_REVERSE ;
			default:
				return INDEX_CATEGORY_SPECIAL_NONE ;
			}
		}
		
		else
			return INDEX_CATEGORY_SPECIAL_NONE ;
	}
	
	protected int getQCombinationIndex( int type ) {
		int qc = PieceCatalog.getQCombination(type) ;
		
		if ( qc == QOrientations.S0 )
			return INDEX_QCOMBINATION_S0 ;
		if ( qc == QOrientations.S1 )
			return INDEX_QCOMBINATION_S1 ;
		if ( qc == QCombinations.SS )
			return INDEX_QCOMBINATION_SS ;
		if ( qc == QOrientations.ST )
			return INDEX_QCOMBINATION_ST ;
		if ( qc == QCombinations.UL )
			return INDEX_QCOMBINATION_UL ;
		if ( qc == QCombinations.SL )
			return INDEX_QCOMBINATION_SL ;
		if ( qc == QOrientations.F0 || qc == QOrientations.F1 )
			return INDEX_QCOMBINATION_F ;
		if ( qc == QOrientations.PUSH_DOWN || qc == QOrientations.PUSH_DOWN_ACTIVE || qc == QOrientations.PUSH_UP || qc == QOrientations.PUSH_UP_ACTIVE )
			return INDEX_QCOMBINATION_PUSH ;
			
		return INDEX_QCOMBINATION_NONE ;
	}
	
	
	private void createPaints( boolean bigText ) {
		if ( bigText ) {
			mTextSizeHeader = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_header_text_size_large) ;
			mTextSizeBody = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_body_text_size_large) ;
		} else {
			mTextSizeHeader = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_header_text_size) ;
			mTextSizeBody = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_body_text_size) ;
		}
		
		mMarginTextHorizontal = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_horizontal_margin) ;
		mMarginSeparatorHorizontal = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_separator_horizontal_margin) ;
		mMarginSeparatorVertical = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_separator_vertical_margin) ;
		mHeightSeparator = mResources.getDimensionPixelSize(R.dimen.game_interface_tooltip_separator_height) ;
		

		int textColor = mResources.getColor(R.color.game_interface_tooltip_text_color) ;
		int shadowColor = mResources.getColor(R.color.game_interface_tooltip_text_shadow_color) ;
		
		float shadowRadius = mResources.getDimensionPixelOffset(R.dimen.game_interface_tooltip_shadow_radius) ;
		float shadowOffsetX = mResources.getDimensionPixelOffset(R.dimen.game_interface_tooltip_shadow_offset_x) ;
		float shadowOffsetY = mResources.getDimensionPixelOffset(R.dimen.game_interface_tooltip_shadow_offset_y) ;
		if ( shadowRadius == 0 )
			shadowRadius = 1.0f ;
		if ( shadowOffsetX == 0 )
			shadowOffsetX = 1 ;
		if ( shadowOffsetY == 0 )
			shadowOffsetY = 1 ;
		
		mShadowRadius = new float[]{ shadowRadius, shadowRadius } ;
		mShadowX = new float[]{ 0, shadowOffsetX } ;
		mShadowY = new float[]{ 0, shadowOffsetY } ;
		mShadowColor = new int []{ shadowColor, shadowColor } ;
		
		mPaintHeader = new Paint[2] ;
		mPaintBody = new Paint[2] ;
		mPaintSeparator = new Paint[2] ;
		mPaintArrays = new Paint[][]{
			mPaintHeader,
			mPaintBody,
			mPaintSeparator
		} ;
		
		for ( int j = 0; j < 2; j++ ) {
			mPaintHeader[j] = new Paint() ;
			mPaintHeader[j].setAntiAlias(true) ;
			mPaintHeader[j].setTextSize(mTextSizeHeader) ;
			mPaintHeader[j].setTextAlign(Paint.Align.CENTER) ;
			mPaintHeader[j].setColor(textColor) ;
			mPaintHeader[j].setShadowLayer(mShadowRadius[j], mShadowX[j], mShadowY[j], mShadowColor[j]) ;
			
			mPaintBody[j] = new Paint() ;
			mPaintBody[j].setAntiAlias(true) ;
			mPaintBody[j].setTextSize(mTextSizeBody) ;
			mPaintBody[j].setTextAlign(Paint.Align.CENTER) ;
			mPaintBody[j].setColor(textColor) ;
			mPaintBody[j].setShadowLayer(mShadowRadius[j], mShadowX[j], mShadowY[j], mShadowColor[j]) ;
			
			mPaintSeparator[j] = new Paint() ;
			mPaintSeparator[j].setAntiAlias(true) ;
			mPaintSeparator[j].setStrokeWidth(mHeightSeparator) ;
			mPaintSeparator[j].setStyle(Paint.Style.STROKE) ;
			mPaintSeparator[j].setColor(textColor) ;
			mPaintSeparator[j].setShadowLayer(mShadowRadius[j], mShadowX[j], mShadowY[j], mShadowColor[j]) ;
		}
	}
	
	
	/**
	 * Loads tooltip texts and populates mTextHeaderWords / mTextHeaderBody 
	 * with individual words from that text (determined by spliting on whitespace).
	 */
	private void loadTooltipText() {
		set_CASTS( mResources ) ;
		
		// Trominoes!
		constructAndSetTrominoStrings( INDEX_CATEGORY_TRO_LINE ) ;
		constructAndSetTrominoStrings( INDEX_CATEGORY_TRO_V ) ;
		
		// Tetrominoes!
		constructAndSetTetrominoStrings( INDEX_CATEGORY_TETRO_LINE ) ;
		constructAndSetTetrominoStrings( INDEX_CATEGORY_TETRO_GAMMA ) ;
		constructAndSetTetrominoStrings( INDEX_CATEGORY_TETRO_GUN ) ;
		constructAndSetTetrominoStrings( INDEX_CATEGORY_TETRO_SQUARE ) ;
		constructAndSetTetrominoStrings( INDEX_CATEGORY_TETRO_S ) ;
		constructAndSetTetrominoStrings( INDEX_CATEGORY_TETRO_Z ) ;
		constructAndSetTetrominoStrings( INDEX_CATEGORY_TETRO_T ) ;
		
		// Pentominoes!
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_F ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_I ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_L ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_N ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_P ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_T ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_U ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_V ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_W ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_X ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_Y ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_Z ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_F_REVERSE ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_L_REVERSE ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_N_REVERSE ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_P_REVERSE ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_Y_REVERSE ) ;
		constructAndSetPentominoStrings( INDEX_CATEGORY_PENTO_Z_REVERSE ) ;
		
		// Tetracubes!
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_L ) ;
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_RECT ) ;
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_ZIG_ZAG ) ;
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_3DT ) ;
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_BRANCH ) ;
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_SCREW ) ;
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_DEXTER ) ;
		constructAndSetTetracubeStrings( INDEX_CATEGORY_TETRA_SINISTER ) ;
		
		// Other specials!  (flash!)
		constructAndSetSpecialStrings( INDEX_CATEGORY_SPECIAL_FLASH ) ;
		constructAndSetSpecialStrings( INDEX_CATEGORY_SPECIAL_PUSH ) ;
	}
	
	protected int formatToFit() {
		
		formatTooltipText() ;
		
		int timesShrunk = 0 ;
		while ( !tooltipTextFits() ) {
			shrinkContent() ;
			formatTooltipText() ;
			timesShrunk++ ;
		}
		
		return timesShrunk ;
	}
	
	
	/**
	 * Returns whether all tooltip text displays will fit in the specified draw region.
	 * @return
	 */
	public boolean tooltipTextFits() {
		return height() <= mDrawRegion.height() ;
	}
	
	
	/**
	 * Returns the maximum height required to render the tooltip within
	 * the specified DrawRegion.  Except in pathological cases, this value
	 * will always be <= the height of the current draw region.  Because text
	 * will shrink to fit within the specified draw region, if you are interested
	 * in the "idealized height" set the maximum possible draw region before calling
	 * this method.
	 * 
	 * @return
	 */
	public int height() {
		int maxHeight = -1 ;
		for ( int i = 0; i < mTextHeader.length; i++ ) {
			for ( int j = 0; j < mTextHeader[i].length; j++ ) {
				if ( mTextHeader[i][j] != null ) {
					// calculate its full length...
					int height
							= mBoundsHeader.height() * mTextHeader[i][j].length
							+ mBoundsBody.height() * mTextBody[i][j].length ;
					height += mHeightSeparator + 2 * mMarginSeparatorVertical ;
					
					maxHeight = Math.max( maxHeight, height ) ;
					
				}
			}
		}
		
		return maxHeight ;
	}
	
	
	/**
	 * Shrinks the display size of its content.
	 */
	protected void shrinkContent() {
		
		if ( mPaintBody[0].getTextSize() < 1 ) {
			throw new IllegalStateException("Can't shrink past 1 point font!") ;
		}
		
		// For now, do the completely naive thing: reduce all text sizes by an appropriate margin
		// (equivalent of 1 point in the text body).
		float reduction = 1.0f / mPaintBody[0].getTextSize() ;
		for ( int j = 0; j < 2; j++ ) {
			mPaintHeader[j].setTextSize( mPaintHeader[j].getTextSize() * ( 1.0f - reduction ) ) ;
			mPaintBody[j].setTextSize( mPaintBody[j].getTextSize() - 1 ) ;
		}
	}
	
	
	/**
	 * Uses the draw region, current paints, and tooltip words
	 * (as found in mTextHeaderWords and mTextBodyWords) to format tooltip
	 * strings which fit within our horizontal bounds by inserting spaces and
	 * line breaks.  The results will populate mTextHeader / mTextBody.
	 */
	protected void formatTooltipText() {
		formatTooltipText( mTextHeaderWords, mTextHeader, mPaintHeader[0] ) ;
		formatTooltipText( mTextBodyWords, mTextBody, mPaintBody[0] ) ;
		
		// get text bounds.
		String allChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz , . ( ) < > : ?" ;
		mPaintHeader[0].getTextBounds(allChars, 0, allChars.length(), mBoundsHeader) ;
		mPaintBody[0].getTextBounds(allChars, 0, allChars.length(), mBoundsBody) ;
	}
	
	/**
	 * Iterates through the 'words' array.  Any non-null 3rd dimension
	 * (i.e., i,j s.t. words[i][j] != null) will be formatted as a space
	 * and newline-separated string of words and placed in text[i][j].
	 * 
	 * nulls will be moved to text[i][j].
	 * 
	 * @param words
	 * @param text
	 * @param p
	 */
	protected void formatTooltipText( String [][][] words, String [][][] text, Paint p ) {
		for ( int i = 0; i < NUM_INDEX_CATEGORIES; i++ ) {
			for ( int j = 0; j < NUM_INDEX_QCOMBINATIONS; j++ ) {
				if ( words[i][j] != null )
					text[i][j] = formatTooltipText( words[i][j], p ) ;
				else
					text[i][j] = null ;
			}
		}
	}
	
	/**
	 * Given an array of words, will format them as a space- separated strings
	 * such that each string is rendered smaller than the available width and
	 * the minimum possible number of strings is returned.
	 * 
	 * Returns the result.
	 * 
	 * @param words
	 * @param p
	 */
	protected String [] formatTooltipText( String [] words, Paint p ) {
		ArrayList<String> al = new ArrayList<String>() ;
		StringBuilder sb = new StringBuilder() ;
		
		// Iterate through the words, adding them one at a time.
		boolean firstWordOnLine = true ;
		for ( int i = 0; i < words.length; i++ ) {
			if ( !firstWordOnLine )
				sb.append(" ") ;
			sb.append( words[i] ) ;
			
			// check length
			String temp = sb.toString() ;
			float width = p.measureText(temp) ;
			
			//Log.d(TAG, "width is " + width + " string is " + sb.toString()) ;
			
			// newline instead if necessary
			if ( !firstWordOnLine && width > mDrawRegion.width() - 2 * mMarginTextHorizontal ) {
				sb.delete(sb.length() - words[i].length() - 1, sb.length()) ;
				al.add(sb.toString()) ;
				sb.delete(0, sb.length()) ;
				firstWordOnLine = true ;
				i-- ;		// back up to put this word on its own line
			} else
				firstWordOnLine = false ;
		}
		
		if ( sb.length() > 0 )
			al.add( sb.toString() ) ;
		
		String [] strs = new String[al.size()] ;
		return al.toArray(strs) ;
	}
	
	protected void constructAndSetTrominoStrings( int catIndex ) {
		// serves as a wrapper for the additional-arguments version.
		String category = null ;
		int categID ;
		
		switch( catIndex ) {
		case INDEX_CATEGORY_TRO_LINE:
			categID = R.string.game_interface_piece_category_tro_line ;
			break ;
		case INDEX_CATEGORY_TRO_V:
			categID = R.string.game_interface_piece_category_tro_v ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize tromino index " + catIndex) ;
		}
		
		category = mResources.getString(categID) ;
		constructAndSetPolyominoStrings(
				catIndex, category ) ;
	}
	
	protected void constructAndSetTetrominoStrings( int catIndex ) {
		// serves as a wrapper for the additional-arguments version.
		String category = null ;
		int categID ;
		
		switch( catIndex ) {
		case INDEX_CATEGORY_TETRO_LINE:
			categID = R.string.game_interface_piece_category_tetro_line ;
			break ;
		case INDEX_CATEGORY_TETRO_GUN:
			categID = R.string.game_interface_piece_category_tetro_gun ;
			break ;
		case INDEX_CATEGORY_TETRO_GAMMA:
			categID = R.string.game_interface_piece_category_tetro_gamma ;
			break ;
		case INDEX_CATEGORY_TETRO_SQUARE:
			categID = R.string.game_interface_piece_category_tetro_square ;
			break ;
		case INDEX_CATEGORY_TETRO_S:
			categID = R.string.game_interface_piece_category_tetro_s ;
			break ;
		case INDEX_CATEGORY_TETRO_Z:
			categID = R.string.game_interface_piece_category_tetro_z ;
			break ;
		case INDEX_CATEGORY_TETRO_T:
			categID = R.string.game_interface_piece_category_tetro_t ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize tetromino index " + catIndex) ;
		}
		
		category = mResources.getString(categID) ;
		constructAndSetPolyominoStrings(
				catIndex, category ) ;
	}
	
	
	protected void constructAndSetPentominoStrings( int catIndex ) {
		// serves as a wrapper for the additional-arguments version.
		String category = null ;
		int categID ;
		
		switch( catIndex ) {
		case INDEX_CATEGORY_PENTO_F:
			categID = R.string.game_interface_piece_category_pento_f ;
			break ;
		case INDEX_CATEGORY_PENTO_I:
			categID = R.string.game_interface_piece_category_pento_i ;
			break ;
		case INDEX_CATEGORY_PENTO_L:
			categID = R.string.game_interface_piece_category_pento_l ;
			break ;
		case INDEX_CATEGORY_PENTO_N:
			categID = R.string.game_interface_piece_category_pento_n ;
			break ;
		case INDEX_CATEGORY_PENTO_P:
			categID = R.string.game_interface_piece_category_pento_p ;
			break ;
		case INDEX_CATEGORY_PENTO_T:
			categID = R.string.game_interface_piece_category_pento_t ;
			break ;
		case INDEX_CATEGORY_PENTO_U:
			categID = R.string.game_interface_piece_category_pento_u ;
			break ;
		case INDEX_CATEGORY_PENTO_V:
			categID = R.string.game_interface_piece_category_pento_v ;
			break ;
		case INDEX_CATEGORY_PENTO_W:
			categID = R.string.game_interface_piece_category_pento_w ;
			break ;
		case INDEX_CATEGORY_PENTO_X:
			categID = R.string.game_interface_piece_category_pento_x ;
			break ;
		case INDEX_CATEGORY_PENTO_Y:
			categID = R.string.game_interface_piece_category_pento_y ;
			break ;
		case INDEX_CATEGORY_PENTO_Z:
			categID = R.string.game_interface_piece_category_pento_z ;
			break ;
		case INDEX_CATEGORY_PENTO_F_REVERSE:
			categID = R.string.game_interface_piece_category_pento_f_reverse ;
			break ;
		case INDEX_CATEGORY_PENTO_L_REVERSE:
			categID = R.string.game_interface_piece_category_pento_l_reverse ;
			break ;
		case INDEX_CATEGORY_PENTO_N_REVERSE:
			categID = R.string.game_interface_piece_category_pento_n_reverse ;
			break ;
		case INDEX_CATEGORY_PENTO_P_REVERSE:
			categID = R.string.game_interface_piece_category_pento_p_reverse ;
			break ;
		case INDEX_CATEGORY_PENTO_Y_REVERSE:
			categID = R.string.game_interface_piece_category_pento_y_reverse ;
			break ;
		case INDEX_CATEGORY_PENTO_Z_REVERSE:
			categID = R.string.game_interface_piece_category_pento_z_reverse ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize pentomino index " + catIndex) ;
		}
		
		category = mResources.getString(categID) ;
		constructAndSetPolyominoStrings(
				catIndex, category ) ;
	}
	
	protected void constructAndSetPolyominoStrings( int catIndex, String category ) {
		
		mTextHeaderWords[catIndex][INDEX_QCOMBINATION_S0] = s0_cw.replace(placeholder, category).trim().split("\\s+") ;
		mTextHeaderWords[catIndex][INDEX_QCOMBINATION_S1] = s1_cw.replace(placeholder, category).trim().split("\\s+") ;
		mTextHeaderWords[catIndex][INDEX_QCOMBINATION_ST] = st_cw.replace(placeholder, category).trim().split("\\s+") ;
		mTextHeaderWords[catIndex][INDEX_QCOMBINATION_UL] = ul_cw.replace(placeholder, category).trim().split("\\s+") ;
		
		mTextBodyWords[catIndex][INDEX_QCOMBINATION_S0] = s0_tt.trim().split("\\s+") ;
		mTextBodyWords[catIndex][INDEX_QCOMBINATION_S1] = s1_tt.trim().split("\\s+") ;
		mTextBodyWords[catIndex][INDEX_QCOMBINATION_ST] = st_tt.trim().split("\\s+") ;
		mTextBodyWords[catIndex][INDEX_QCOMBINATION_UL] = ul_tt.trim().split("\\s+") ;
	}
	
	
	protected void constructAndSetTetracubeStrings( int catIndex ) {
		// serves as a wrapper for the additional-arguments version.
		String category = null ;
		int categID ;
		
		switch( catIndex ) {
		case INDEX_CATEGORY_TETRA_L:
			categID = R.string.game_interface_piece_category_tetra_l ;
			break ;
		case INDEX_CATEGORY_TETRA_RECT:
			categID = R.string.game_interface_piece_category_tetra_rect ;
			break ;
		case INDEX_CATEGORY_TETRA_ZIG_ZAG:
			categID = R.string.game_interface_piece_category_tetra_s ;
			break ;
		case INDEX_CATEGORY_TETRA_3DT:
			categID = R.string.game_interface_piece_category_tetra_t ;
			break ;
		case INDEX_CATEGORY_TETRA_BRANCH:
			categID = R.string.game_interface_piece_category_tetra_branch ;
			break ;
		case INDEX_CATEGORY_TETRA_SCREW:
			categID = R.string.game_interface_piece_category_tetra_screw ;
			break ;
		case INDEX_CATEGORY_TETRA_DEXTER:
			categID = R.string.game_interface_piece_category_tetra_corner_right ;
			break ;
		case INDEX_CATEGORY_TETRA_SINISTER:
			categID = R.string.game_interface_piece_category_tetra_corner_left ;
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize tetracube index " + catIndex) ;
		}
		
		category = mResources.getString(categID) ;
		constructAndSetTetracubeStrings(
				catIndex, category  ) ;
	}
	
	
	protected void constructAndSetTetracubeStrings( int catIndex, String category ) {
		
		mTextHeaderWords[catIndex][INDEX_QCOMBINATION_SS] = ss_cw.replace(placeholder, category).trim().split("\\s+") ;
		mTextHeaderWords[catIndex][INDEX_QCOMBINATION_SL] = sl_cw.replace(placeholder, category).trim().split("\\s+") ;
		
		mTextBodyWords[catIndex][INDEX_QCOMBINATION_SS] = ss_tt.trim().split("\\s+") ;
		mTextBodyWords[catIndex][INDEX_QCOMBINATION_SL] = sl_tt.trim().split("\\s+") ;
	}
	
	
	protected void constructAndSetSpecialStrings( int catIndex ) {
		switch( catIndex ) {
		case INDEX_CATEGORY_SPECIAL_FLASH:
			mTextHeaderWords[catIndex][INDEX_QCOMBINATION_F] = fl_cw.trim().split("\\s+") ;
			mTextBodyWords[catIndex][INDEX_QCOMBINATION_F] = fl_tt.trim().split("\\s+") ; 
			break ;
		case INDEX_CATEGORY_SPECIAL_PUSH:
			mTextHeaderWords[catIndex][INDEX_QCOMBINATION_PUSH] = ps_cw.trim().split("\\s+") ;
			mTextBodyWords[catIndex][INDEX_QCOMBINATION_PUSH] = ps_tt.trim().split("\\s+") ; 
			break ;
		default:
			throw new IllegalArgumentException("Don't recognize special index " + catIndex) ;
		}
	}
	
	
	String s0_tt, s0_cw ;
	String s1_tt, s1_cw ;
	String ss_tt, ss_cw ;
	String fl_tt, fl_cw ;
	String ul_tt, ul_cw ;
	String st_tt, st_cw ;
	String sl_tt, sl_cw ;
	String ps_tt, ps_cw ;
	String placeholder ;
	
	protected void set_CASTS( Resources res ) {
		if ( mLabelColors ) {
			s0_tt = res.getString(R.string.game_interface_piece_qpane_0_tooltip) ;
			s0_cw = res.getString(R.string.game_interface_piece_qpane_0_wrapper) ;
			
			s1_tt = res.getString(R.string.game_interface_piece_qpane_1_tooltip) ;
			s1_cw = res.getString(R.string.game_interface_piece_qpane_1_wrapper) ;
			
			ss_tt = res.getString(R.string.game_interface_piece_qpane_both_tooltip) ;
			ss_cw = res.getString(R.string.game_interface_piece_qpane_both_wrapper) ;
			
			fl_tt = res.getString(R.string.game_interface_piece_special_f_tooltip) ;
			fl_cw = res.getString(R.string.game_interface_piece_special_f_wrapper) ;
			
			ul_tt = res.getString(R.string.game_interface_piece_special_ul_tooltip) ;
			ul_cw = res.getString(R.string.game_interface_piece_special_ul_wrapper) ;
			
			st_tt = res.getString(R.string.game_interface_piece_special_st_tooltip) ;
			st_cw = res.getString(R.string.game_interface_piece_special_st_wrapper) ;
			
			sl_tt = res.getString(R.string.game_interface_piece_special_sl_tooltip) ;
			sl_cw = res.getString(R.string.game_interface_piece_special_sl_wrapper) ;
			
			ps_tt = res.getString(R.string.game_interface_piece_special_push_tooltip) ;
			ps_cw = res.getString(R.string.game_interface_piece_special_push_wrapper) ;
		} else {
			s0_tt = res.getString(R.string.game_interface_piece_qpane_0_tooltip_no_color) ;
			s0_cw = res.getString(R.string.game_interface_piece_qpane_0_wrapper_no_color) ;
			
			s1_tt = res.getString(R.string.game_interface_piece_qpane_1_tooltip_no_color) ;
			s1_cw = res.getString(R.string.game_interface_piece_qpane_1_wrapper_no_color) ;
			
			ss_tt = res.getString(R.string.game_interface_piece_qpane_both_tooltip_no_color) ;
			ss_cw = res.getString(R.string.game_interface_piece_qpane_both_wrapper_no_color) ;
			
			fl_tt = res.getString(R.string.game_interface_piece_special_f_tooltip_no_color) ;
			fl_cw = res.getString(R.string.game_interface_piece_special_f_wrapper_no_color) ;
			
			ul_tt = res.getString(R.string.game_interface_piece_special_ul_tooltip_no_color) ;
			ul_cw = res.getString(R.string.game_interface_piece_special_ul_wrapper_no_color) ;
			
			st_tt = res.getString(R.string.game_interface_piece_special_st_tooltip_no_color) ;
			st_cw = res.getString(R.string.game_interface_piece_special_st_wrapper_no_color) ;
			
			sl_tt = res.getString(R.string.game_interface_piece_special_sl_tooltip_no_color) ;
			sl_cw = res.getString(R.string.game_interface_piece_special_sl_wrapper_no_color) ;
			
			ps_tt = res.getString(R.string.game_interface_piece_special_push_tooltip_no_color) ;
			ps_cw = res.getString(R.string.game_interface_piece_special_push_wrapper_no_color) ;
		}
		
		placeholder = res.getString(R.string.placeholder_piece_category) ;

	}
	
	protected void empty_CASTS( Resources res ) {
		s0_tt = s0_cw = null ;
		
		s1_tt = s1_cw = null ;
		
		ss_tt = ss_cw = null ;
		
		fl_tt = fl_cw = null ;
		
		ul_tt = ul_cw = null ;
		
		st_tt = st_cw = null ;
		
		sl_tt = sl_cw = null ;
		
		ps_tt = ps_cw = null ;
		
		placeholder = null ;
	}
	
}
