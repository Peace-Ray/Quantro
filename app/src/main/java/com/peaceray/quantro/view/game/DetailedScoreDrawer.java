package com.peaceray.quantro.view.game;

import com.peaceray.quantro.R;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.interpolation.Interpolation;
import com.peaceray.quantro.utils.interpolation.LinearInterpolation;
import com.peaceray.quantro.view.colors.ColorScheme;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.TypedValue;

/**
 * Draws a detailed view of the score, including game name, rows cleared,
 * etc.
 * 
 * We plan to add "game time" later on, but for now, we display:
 * 
 * 			GAME NAME
 * 			 LEVEL X
 * 		SCORE: 	YYYYYYY
 * 					+ Z		x W 
 * 		ROWS CLEARED:  U   V
 * 
 * 
 * ALTERNATIVE:  for "Flood" game modes, this is modified to:
 * 
 * 			GAME NAME
 * 			 LEVEL X
 * 		TIME:   H:MM:SS.mmm
 * 		SCORE:  YYYYYYY
 * 					+ Z		x W
 * 		ROWS CLEAR:  U   V
 * 		
 * Alignment:
 * 	GAME NAME is centered.
 * 	Level is centered.
 * 
 * 	Section labels are left-aligned.
 * 	Score is right-aligned based on the maximum length at the
 * 		current number of digits.  The distance between "SCORE:"
 * 		and the number may change slightly as digits change.
 *  Addition is right-aligned to exactly the right edge of the score.
 *  Multiplier is right-aligned some distance to the right of addition.
 * 
 *  The number of rows cleared will be a single int if S0 and S1 are both
 *  	0: it is SS + MO.  If either S0 or S1 are > 0, display (S0+SS+MO)
 *  	and (S1+SS+MO) as separate numbers.  Alignment shouldn't really
 *  	matter; just use a standard distance based on number of digits.
 *  	Try to right-align both based on this.
 *  
 *  
 *  Possible Alternative: this might be too wide for certain screens.
 *  	Although we can relatively easily expand vertically, it is
 *  	more difficult to expand horizontally.  Therefore, we
 *  	allow an alternative format:
 *  
 *  		GAME NAME
 * 			 LEVEL X
 * 		TIME:   
 * 		SCORE: 	YYYYYYY
 * 					+ Z	
 * 					x W 
 * 		ROWS:  U  
 * 			   V
 * 
 * We switch to vertically expanded format if we cannot fit content with
 * 		reasonable maximums w/in the space available; we then attempt to shrink
 * 		if we still cannot fit horizontally.
 * 
 * Note that this may put us beyond the boundaries of any DrawRegion provided.
 * 		We do NOT adjust to fit vertically; this would likely result in unreadable
 * 		text.  Recommended usage is to set a possible draw region, query
 * 		height() or size(), and adjust from there.
 * 
 * @author Jake
 *
 */
public class DetailedScoreDrawer extends ScoreDrawer {
	
	private static final String TAG = "DetailedScoreDrawer" ;
	
	private static final long GLOW_FADE_TIME = 1000 ;
	private static final int NUM_GLOW_REPS = 2 ;
	
	private static final int FLOAT_ADDITIONAL_DIGITS = 2 ;
	
	private static final int MAX_LONG_DIGITS = digits(Long.MAX_VALUE) - 2 ;
	
	private static final String SEP_STRING = " " ;
	private static final char SEP_CHAR = ' ' ;
	private static final String MAX_TIME_STRING = "HHHH:MM:SS.m" ;
	private static final String MAX_SCORE_STRING = "888,888,888,888" ;
	private static final String MAX_ROW_STRING = "888,888" ;
	private static final String MAX_LEVEL_STRING = "8,888" ;
	private static final String MAX_MULTIPLIER_STRING = "888.88" ;
	
	// Our local info
	private long time ;
	private int level ;
	private long score ;
	private int addition ;
	private float multiplier ;
	private int rows_s0 ;
	private int rows_s1 ;
	private int rows_ss ;
	private boolean separateRowDisplays ;
	
	private boolean localInfoChanged ;
	
	// GameInformation object
	private GameInformation ginfo ;
	
	// Paints for drawing things.  We adjust these paints in each update.
	private Paint [] mPaint ;
	private Paint [] mGlowPaint ;
	private boolean [] mGlowFinished ;
	private int mGlowColor ;
	private float mGlowRadius ;
	private Interpolation alphaInterpolation ;
	
	private static final int INDEX_TIME_LABEL = 0 ;
	private static final int INDEX_SCORE_LABEL = 1 ;
	private static final int INDEX_ROWS_LABEL = 2 ;
	
	private static final int INDEX_TIME_PREFIX = 3 ;
	private static final int INDEX_LEVEL_PREFIX = 4 ;
	private static final int INDEX_SCORE_PREFIX = 5 ;
	private static final int INDEX_ADDITION_PREFIX = 6 ;
	private static final int INDEX_MULTIPLIER_PREFIX = 7 ;
	private static final int INDEX_ROWS_PREFIX = 8 ;
	
	
	private static final int INDEX_TITLE = 9 ;
	private static final int INDEX_LEVEL = 10 ;
	private static final int INDEX_TIME = 11 ;
	private static final int INDEX_SCORE = 12 ;
	private static final int INDEX_ADDITION = 13 ;
	private static final int INDEX_MULTIPLIER = 14 ;
	private static final int INDEX_ROWS_S0 = 15 ;
	private static final int INDEX_ROWS_S1 = 16 ;
	private static final int INDEX_ROWS_SS = 17 ;
	
	private static final int NUM_INDICES_LABELS = 3 ;
	private static final int NUM_INDICES_PREFIXES = 6 ;
	private static final int NUM_INDICES_TEXT = 9 ;
	private static final int NUM_INDICES = 18 ;
	
	
	// Storage for strings.  Anything that updates is stored as a char []
	// to prevent reallocation of strings.
	private String [] mTextLabelString ;	// only stores those with _LABEL and _PREFIX indices.
	private char [][] mText ;
	private int [] mTextLength ;
	private int [] mTextHeight ;		// measures the maximum possible height of this text
	private int [][] mTextWidthWithDigits ;
	private int [] mTextMaxChars ;		// the maximum number of digits that can be displayed without abbreviation.
	private long [] mLastUpdate ;
	
	private float mTitleSize ;
	
	// Where do we draw?
	private Rect drawRegion ;
	// Are we drawing in a vertically-expanded format?  we prefer not to, but...
	private boolean mVerticallyExpanded ;
	// What colors do we use for QOrientation-specific stuff?
	private ColorScheme mColorScheme ;
	
	private int [] mMarginVertical ;
	private int [] mMarginHorizontal ;
	private int mMarginVerticalTopBottom ;
	
	private int [] mMarginVerticalIdeal ;
	private int [] mMarginHorizontalIdeal ;
	private int mMarginVerticalTopBottomIdeal ;
	
	
	private boolean mShowTime ;
	
	private Rect mTextBounds ;
	
	
	
	public DetailedScoreDrawer( GameInformation ginfo, Resources res, Rect region, ColorScheme cs, boolean bigText ) {
		this.ginfo = ginfo ;
		drawRegion = new Rect(region) ;
		
		mTextBounds = new Rect() ;
		
		mVerticallyExpanded = false ;
		
		mShowTime = ginfo == null ? false
				: GameModes.measurePerformanceBy(ginfo.mode) == GameModes.MEASURE_PERFORMANCE_BY_TIME ;
		
		loadDimensions(res) ;
		initializeTextMembers(res) ;
		loadPaintsAndColors(res, cs, bigText) ;
		setTextLimits(res) ;	// width, height, max
		
		// vertically expand?
		if ( width() > drawRegion.width() )
			mVerticallyExpanded = true ;
		
		
		
		// Force an update by setting to invalid values
		time = -1 ;
		level = -1 ;
		score = -1 ;
		addition = -999999999 ;
		multiplier = -1f ;
		rows_s0 = rows_s1 = rows_ss = -1 ;
		separateRowDisplays = ginfo == null ? true : GameModes.numberQPanes(ginfo) == 2 ;
		
		mLastUpdate = new long[NUM_INDICES] ;
		mGlowFinished = new boolean[NUM_INDICES] ;
		for ( int i = 0; i < NUM_INDICES; i++ )
			mGlowFinished[i] = true ;
		updateLocalInfo( 0 ) ;
		
		StringBuilder sb = new StringBuilder() ;
		sb.append(mText[INDEX_TITLE], 0, mTextLength[INDEX_TITLE]) ;
	}
	
	@Override
	public float titleSize() {
		return mTitleSize ;
	}
	
	public void setDrawRegion( Rect drawRegion ) {
		drawRegion = new Rect(drawRegion) ;
		mVerticallyExpanded = false ;
		if ( width() > drawRegion.width() )
			mVerticallyExpanded = true ;
	}

	@Override
	public void update(long currentTime) {
		updateLocalInfo( currentTime ) ;
	}
	
	@Override
	public int height() {
		int height = mMarginVerticalTopBottom*2 + mTextHeight[INDEX_TITLE] ;
		height += mTextHeight[INDEX_LEVEL] + mMarginVertical[INDEX_LEVEL] ;
		
		if ( mVerticallyExpanded ) {
			if ( mShowTime ) {
				height += Math.max(
						mTextHeight[INDEX_TIME] + mMarginVertical[INDEX_TIME],
						mTextHeight[INDEX_TIME_LABEL] + mMarginVertical[INDEX_TIME_LABEL] ) ;
			}
			height += Math.max(
					mTextHeight[INDEX_SCORE] + mMarginVertical[INDEX_SCORE],
					mTextHeight[INDEX_SCORE_LABEL] + mMarginVertical[INDEX_SCORE_LABEL] );
			// addition and multiplier on different lines
			height += mTextHeight[INDEX_ADDITION] + mMarginVertical[INDEX_ADDITION] ;
			height += mTextHeight[INDEX_MULTIPLIER] + mMarginVertical[INDEX_MULTIPLIER] ;
			// rows on separate lines.
			if ( !separateRowDisplays )
				height += Math.max(
						mTextHeight[INDEX_ROWS_SS] + mMarginVertical[INDEX_ROWS_SS],
						mTextHeight[INDEX_ROWS_LABEL] + mMarginVertical[INDEX_ROWS_LABEL] );
			else {
				height += Math.max(
						mTextHeight[INDEX_ROWS_S0] + mMarginVertical[INDEX_ROWS_S0],
						mTextHeight[INDEX_ROWS_LABEL] + mMarginVertical[INDEX_ROWS_LABEL] );
				height += mTextHeight[INDEX_ROWS_S1] + mMarginVertical[INDEX_ROWS_S1] ;
			}
		} else {
			if ( mShowTime ) {
				height += Math.max(
						mTextHeight[INDEX_TIME] + mMarginVertical[INDEX_TIME],
						mTextHeight[INDEX_TIME_LABEL] + mMarginVertical[INDEX_TIME_LABEL] ) ;
			}
			height += Math.max(
					mTextHeight[INDEX_SCORE] + mMarginVertical[INDEX_SCORE],
					mTextHeight[INDEX_SCORE_LABEL] + mMarginVertical[INDEX_SCORE_LABEL] );
			height += Math.max(
					mTextHeight[INDEX_ADDITION] + mMarginVertical[INDEX_ADDITION],
					mTextHeight[INDEX_MULTIPLIER] + mMarginVertical[INDEX_MULTIPLIER] );
			height += Math.max(
					mTextHeight[INDEX_ROWS_SS] + mMarginVertical[INDEX_ROWS_SS],
					mTextHeight[INDEX_ROWS_LABEL] + mMarginVertical[INDEX_ROWS_LABEL] );
		}
		
		return height ;
	}
	
	
	/**
	 * Returns the maximum width used to draw content.  This may exceed
	 * the available draw region.
	 * @return
	 */
	public int width() {
		// unlike height, we take the maximum of each row, now total.
		int width = 0 ;
		int lineWidth ;
		
		// title?
		String line = mTextLabelString[INDEX_TITLE] ;
		mPaint[INDEX_TITLE].getTextBounds(
				line, 0, line.length(), mTextBounds) ;
		width = Math.max( width, mTextBounds.width() + mMarginHorizontal[INDEX_TITLE] ) ;
		
		// level?
		line = mTextLabelString[INDEX_LEVEL_PREFIX] + SEP_STRING + MAX_LEVEL_STRING ;
		mPaint[INDEX_LEVEL_PREFIX].getTextBounds(
				line, 0, line.length(), mTextBounds) ;
		width = Math.max( width, mTextBounds.width() + mMarginHorizontal[INDEX_LEVEL_PREFIX] ) ;
		
		// time?
		line = mTextLabelString[INDEX_TIME_LABEL] ;
		mPaint[INDEX_TIME_LABEL].getTextBounds(
				line, 0, line.length(), mTextBounds) ;
		lineWidth = mTextBounds.width() + mMarginHorizontal[INDEX_TIME_LABEL] ;
		line = MAX_TIME_STRING ;
		mPaint[INDEX_TIME].getTextBounds(
				line, 0, line.length(), mTextBounds) ;
		lineWidth += mTextBounds.width() + mMarginHorizontal[INDEX_TIME] ;
		width = Math.max( width, lineWidth ) ;
		
		// score?  Two items to include - the score itself and the label thereof.
		// We also include horizontal margins.
		// label first:
		line = mTextLabelString[INDEX_SCORE_LABEL] ;
		mPaint[INDEX_SCORE_LABEL].getTextBounds(
				line, 0, line.length(), mTextBounds) ;
		lineWidth = mTextBounds.width() + mMarginHorizontal[INDEX_SCORE_LABEL] ;
		line = MAX_SCORE_STRING ;
		mPaint[INDEX_SCORE].getTextBounds(
				line, 0, line.length(), mTextBounds) ;
		lineWidth += mTextBounds.width() + mMarginHorizontal[INDEX_SCORE] ;
		// NOTE: we assume this value of lineWidth below.
		width = Math.max( width, lineWidth ) ;
		
		// addition / mult?  if expanded vertically, these are
		// both right-aligned with the current score, and we assume
		// they cannot extend the width.  If not, however, then
		// the multiplayer moves rightward from the score and 
		// extends the total length.
		if ( !mVerticallyExpanded ) {
			line = mTextLabelString[INDEX_MULTIPLIER_PREFIX] + SEP_STRING + MAX_MULTIPLIER_STRING ;
			mPaint[INDEX_MULTIPLIER].getTextBounds(
					line, 0, line.length(), mTextBounds) ;
			// NOTE: we assume lineWidth is set to the total "score" line width.
			lineWidth += mTextBounds.width() + mMarginHorizontal[INDEX_MULTIPLIER] ;
			width = Math.max( width, lineWidth ) ;
		}
		
		// rows!
		line = mTextLabelString[INDEX_ROWS_LABEL] ;
		mPaint[INDEX_ROWS_LABEL].getTextBounds(
				line, 0, line.length(), mTextBounds) ;
		lineWidth = mTextBounds.width() + mMarginHorizontal[INDEX_ROWS_LABEL] ;
		// 3 cases.  If not separateRowDisplays, use SS.
		// Else, if vertically expanded, use S0.
		// Else, use S0 AND S1, summing them together.
		if ( !separateRowDisplays ) {
			line = MAX_ROW_STRING ;
			mPaint[INDEX_ROWS_SS].getTextBounds(
					line, 0, line.length(), mTextBounds) ;
			lineWidth += mTextBounds.width() + mMarginHorizontal[INDEX_ROWS_SS] ;
		} else {
			line = MAX_ROW_STRING ;
			mPaint[INDEX_ROWS_S0].getTextBounds(
					line, 0, line.length(), mTextBounds) ;
			lineWidth += mTextBounds.width() + mMarginHorizontal[INDEX_ROWS_S0] ;
			if ( !mVerticallyExpanded ) {
				mPaint[INDEX_ROWS_S1].getTextBounds(
						line, 0, line.length(), mTextBounds) ;
				lineWidth += mTextBounds.width() + mMarginHorizontal[INDEX_ROWS_S1] ;
			}
		}
		width = Math.max( width, lineWidth ) ;
		
		return width ;
	}
	
	
	@Override
	public Rect size() {
		return size( null ) ;
	}
	
	public Rect size( Rect in ) {
		if ( in == null )
			in = new Rect() ;
		
		// We need width and height.  Height is easy - call 'height().'
		// Width is tougher, because we have to call 'width()' which uses
		// characters from the bottom-row of the keyboard.
		in.set(0, 0, width(), height()) ;
		return null ;
	}

	@Override
	public boolean needsToDraw() {
		for ( int i = 0; i < NUM_INDICES; i++ )
			if ( !mGlowFinished[i] )
				return true ;
		return localInfoChanged ;
	}

	@Override
	public void drawToCanvas(Canvas canvas, long currentTime) {
		// Clip!
		canvas.save() ;
		canvas.clipRect(drawRegion) ;
		
		// Set the shadows for our glow paints!
		for ( int i = INDEX_LEVEL; i < NUM_INDICES; i++ ) {
			long diff = currentTime - mLastUpdate[i] ;
			if ( diff < GLOW_FADE_TIME ) {
				int color = Color.argb(
						alphaInterpolation.intAt(diff),
						Color.red(mGlowColor),
						Color.green(mGlowColor),
						Color.blue(mGlowColor)) ;
				mGlowPaint[i].setShadowLayer(mGlowRadius, 0, 0, color) ;
			} else {
				mGlowFinished[i] = true ;
			}
		}
		
		// determine the total height, and center within the draw region
		int height = height() ;
		
		// Title and level are centered horizontally...
		int y = (drawRegion.height() - height)/2 + drawRegion.top ;
		int x = drawRegion.centerX() ;
		
		// Draw the title.  It never changes.
		y += mMarginVerticalTopBottom ;
		drawText( canvas, INDEX_TITLE, x, y, 0 ) ;
		
		// Level is below...
		y += mTextHeight[INDEX_TITLE] + mMarginVertical[INDEX_LEVEL] ;
		drawText( canvas, INDEX_LEVEL, x, y, mGlowFinished[INDEX_LEVEL] ? 0 : NUM_GLOW_REPS ) ;
		y += mTextHeight[INDEX_LEVEL] ;
		
		
		if ( mShowTime ) {
			// Time has a label; draw it left-aligned.
			y += Math.max(mTextHeight[INDEX_TIME_LABEL], mTextHeight[INDEX_TIME]) ;
			x = drawRegion.left + mMarginHorizontal[INDEX_TIME_LABEL] ;
			drawText( canvas, INDEX_TIME_LABEL, x, y, 0 ) ;
			// Score itself is right-aligned: offset by the width of the label,
			// the width of the score, and the horizontal margin before drawing.
			x += mTextWidthWithDigits[INDEX_TIME_LABEL][0] ;
			x += mMarginHorizontal[INDEX_TIME] ;
			drawText( canvas, INDEX_TIME, x, y, mGlowFinished[INDEX_TIME] ? 0 : NUM_GLOW_REPS ) ;
		}
		
		// Score has a label; draw it left-aligned.
		y += Math.max(mTextHeight[INDEX_SCORE_LABEL], mTextHeight[INDEX_SCORE] ) ;
		x = drawRegion.left + mMarginHorizontal[INDEX_SCORE_LABEL] ;
		drawText( canvas, INDEX_SCORE_LABEL, x, y, 0 ) ;
		// Score itself is right-aligned: offset by the width of the label,
		// the width of the score, and the horizontal margin before drawing.
		x += mTextWidthWithDigits[INDEX_SCORE_LABEL][0] ;
		x += mTextWidthWithDigits[INDEX_SCORE][ digits(score) ] ;
		x += mMarginHorizontal[INDEX_SCORE] ;
		drawText( canvas, INDEX_SCORE, x, y, mGlowFinished[INDEX_SCORE] ? 0 : NUM_GLOW_REPS ) ;
		
		// We align the right-edge of addition relative to the right-edge of score.
		x += mMarginHorizontal[INDEX_ADDITION] ;
		y += Math.max( mTextHeight[INDEX_SCORE], mTextHeight[INDEX_SCORE_LABEL] )
				+ Math.max( mMarginVertical[INDEX_ADDITION], mMarginVertical[INDEX_MULTIPLIER] ) ;
		drawText( canvas, INDEX_ADDITION, x, y, mGlowFinished[INDEX_ADDITION] ? 0 : NUM_GLOW_REPS ) ;
		
		// If mVerticallyExpanded, we draw multiplayer directly below this.
		// Otherwise, we step rightward by a horizontal margin and width and draw
		// (it is RIGHT aligned in both cases).
		if ( mVerticallyExpanded )
			y += mTextHeight[INDEX_ADDITION] + mMarginVertical[INDEX_ADDITION] ;
		else
			x += mTextWidthWithDigits[INDEX_MULTIPLIER][ digits(multiplier) + multiplier > 1 ? 2 : 0 ] + mMarginHorizontal[INDEX_MULTIPLIER] ;
		drawText( canvas, INDEX_MULTIPLIER, x, y, mGlowFinished[INDEX_MULTIPLIER] ? 0 : NUM_GLOW_REPS ) ;
		
		// Rows cleared!  
		y += Math.max(
				mVerticallyExpanded ? 0 : mTextHeight[INDEX_ADDITION],
				mTextHeight[INDEX_MULTIPLIER] )
				+ Math.max( mMarginVertical[INDEX_ROWS_LABEL], mMarginVertical[INDEX_ROWS_SS] ) ;
		x = drawRegion.left + mMarginHorizontal[INDEX_ROWS_LABEL] ;
		drawText( canvas, INDEX_ROWS_LABEL, x, y, 0 ) ;
		
		// Behavior differs based on whether we draw the rows separately.
		if ( separateRowDisplays ) {
			// rows s0 is right-aligned, as is rows s1.  We draw them one after another.
			x += mTextWidthWithDigits[INDEX_ROWS_LABEL][0] ;
			x += mMarginHorizontal[INDEX_ROWS_S0] + mTextWidthWithDigits[INDEX_ROWS_S0][ digits(rows_s0) ] ;
			// draw
			drawText( canvas, INDEX_ROWS_S0, x, y, mGlowFinished[INDEX_ROWS_S0] ? 0 : NUM_GLOW_REPS ) ;
			
			// If vertically expanded, we move to the next line before drawing, keeping
			// the same x.  Otherwise, we move rightward.
			if ( mVerticallyExpanded )
				y += Math.max( mTextHeight[INDEX_ROWS_LABEL], mTextHeight[INDEX_ROWS_S0] )
					+ mMarginVertical[INDEX_ROWS_S1] ;
			else
				x += mMarginHorizontal[INDEX_ROWS_S1] + mTextWidthWithDigits[INDEX_ROWS_S1][ digits(rows_s1) ] ;
			// draw
			drawText( canvas, INDEX_ROWS_S1, x, y, mGlowFinished[INDEX_ROWS_S1] ? 0 : NUM_GLOW_REPS ) ;
		} else {
			// right-aligned.
			x += mTextWidthWithDigits[INDEX_ROWS_LABEL][0] ;
			x += mMarginHorizontal[INDEX_ROWS_SS] + mTextWidthWithDigits[INDEX_ROWS_SS][ digits(rows_ss) ] ;
			// draw
			drawText( canvas, INDEX_ROWS_SS, x, y, mGlowFinished[INDEX_ROWS_SS] ? 0 : NUM_GLOW_REPS ) ;
		}
		
		canvas.restore();
		localInfoChanged = false ;
	}
	
	
	public void setColorScheme( ColorScheme colorScheme ) {
		mColorScheme = colorScheme ;
		// S0, S1 color for rows cleared...
		if ( mColorScheme != null ) {
			mPaint[INDEX_ROWS_S0].setColor(mColorScheme.getFillColor(QOrientations.S0, 0)) ;
			mPaint[INDEX_ROWS_S1].setColor(mColorScheme.getFillColor(QOrientations.S1, 1)) ;
		}
	}
	
	
	private void drawText( Canvas canvas, int index, int x, int y, int glowReps ) {
		for ( int i = 0; i < glowReps; i++ )
			canvas.drawText(
					mText[index], 0, mTextLength[index],
					x,
					y + mTextHeight[index],
					mGlowPaint[index]) ;
		canvas.drawText(
				mText[index], 0, mTextLength[index],
				x,
				y + mTextHeight[index],
				mPaint[index]) ;
	}
	
	
	private void updateLocalInfo( long updateTime ) {
		long tempTime = 0 ;
		int tempLevel = 0;
		long tempScore = 0 ;
		int tempAddition = 0 ;
		float tempMultiplier = 0;
		int tempRowS0 = 0, tempRowS1 = 0, tempRowSS = 0 ;
		if ( ginfo != null ) {
			synchronized( ginfo ) {
				tempTime = ginfo.milliseconds ;
				tempLevel = ginfo.level ;
				tempScore = ginfo.score ;
				tempAddition = ginfo.addition ;
				tempMultiplier = ginfo.multiplier ;
				tempRowSS = ginfo.sLclears + ginfo.moclears ;
				tempRowS0 = ginfo.s0clears + tempRowSS ;
				tempRowS1 = ginfo.s1clears + tempRowSS ;
			}
		}
		
		separateRowDisplays = separateRowDisplays || (tempRowS0 != tempRowSS || tempRowS1 != tempRowSS) ;
		
		localInfoChanged = ( tempLevel != level
				|| tempTime != time
				|| tempScore != score || tempAddition != addition
				|| tempMultiplier != multiplier
				|| tempRowS0 != rows_s0 || tempRowS1 != rows_s1 || tempRowSS != rows_ss) ;
		
		// We note every change EXCEPT a decreasing multiplier.
		// TODO: If a decreasing mulitplier is significant, change this.
		
		// Updates.
		if ( tempLevel != level ) {
			level = tempLevel ;
			mLastUpdate[INDEX_LEVEL] = updateTime ;
			mGlowFinished[INDEX_LEVEL] = false ;
			mTextLength[INDEX_LEVEL] = intToCharArray(
					level,
					mText[INDEX_LEVEL],
					mTextLength[INDEX_LEVEL_PREFIX] == 0 ? 0 : mTextLength[INDEX_LEVEL_PREFIX]+1,
					mTextMaxChars[INDEX_LEVEL],
					null, null ) ;
		}
		if ( tempTime != time ) {
			boolean newMinute = (time / (60*1000)) != (tempTime / (60*1000)) ;
			time = tempTime ;
			// only glow for a new minute.
			if ( newMinute ) {
				mLastUpdate[INDEX_TIME] = updateTime ;
				mGlowFinished[INDEX_TIME] = !newMinute ;
			}
			mTextLength[INDEX_TIME] = timeToCharArray(
					time,
					mText[INDEX_TIME],
					mTextLength[INDEX_TIME_PREFIX] == 0 ? 0 : mTextLength[INDEX_TIME_PREFIX]+1,
					mTextMaxChars[INDEX_TIME]) ;
		}
		if ( tempScore != score ) {
			score = tempScore ;
			mLastUpdate[INDEX_SCORE] = updateTime ;
			mLastUpdate[INDEX_ADDITION] = updateTime ;	// If the score changed, so did the addition, even if its value remained the same (it is a NEW addition of the same amount)
			mGlowFinished[INDEX_SCORE] = mGlowFinished[INDEX_ADDITION] = false ;
			mTextLength[INDEX_SCORE] = longToCharArray(
					score,
					mText[INDEX_SCORE],
					mTextLength[INDEX_SCORE_PREFIX] == 0 ? 0 : mTextLength[INDEX_SCORE_PREFIX]+1,
					mTextMaxChars[INDEX_SCORE],
					null, null ) ;
		}
		if ( tempAddition != addition ) {
			addition = tempAddition ;
			mLastUpdate[INDEX_ADDITION] = updateTime ;
			mGlowFinished[INDEX_ADDITION] = false ;
			mTextLength[INDEX_ADDITION] = intToCharArray(
					addition,
					mText[INDEX_ADDITION],
					mTextLength[INDEX_ADDITION_PREFIX] == 0 ? 0 : mTextLength[INDEX_ADDITION_PREFIX]+1,
					mTextMaxChars[INDEX_ADDITION],
					null, null ) ;
		}
		if ( tempMultiplier != multiplier ) {
			if ( tempMultiplier > multiplier )
				mLastUpdate[INDEX_MULTIPLIER] = updateTime ;
			multiplier = tempMultiplier ;
			mGlowFinished[INDEX_MULTIPLIER] = false ;
			mTextLength[INDEX_MULTIPLIER] = floatToCharArray(
					multiplier,
					mText[INDEX_MULTIPLIER],
					mTextLength[INDEX_MULTIPLIER_PREFIX] == 0 ? 0 : mTextLength[INDEX_MULTIPLIER_PREFIX]+1,
					mTextMaxChars[INDEX_MULTIPLIER],
					FLOAT_ADDITIONAL_DIGITS ) ;
		}
		if ( tempRowS0 != rows_s0 ) {
			rows_s0 = tempRowS0 ;
			mGlowFinished[INDEX_ROWS_S0] = false ;
			mLastUpdate[INDEX_ROWS_S0] = updateTime ;
			mTextLength[INDEX_ROWS_S0] = intToCharArray(
					rows_s0,
					mText[INDEX_ROWS_S0],
					mTextLength[INDEX_ROWS_PREFIX] == 0 ? 0 : mTextLength[INDEX_ROWS_PREFIX]+1,
					mTextMaxChars[INDEX_ROWS_S0],
					null, null ) ;
		}
		if ( tempRowS1 != rows_s1 ) {
			rows_s1 = tempRowS1 ;
			mGlowFinished[INDEX_ROWS_S1] = false ;
			mLastUpdate[INDEX_ROWS_S1] = updateTime ;
			mTextLength[INDEX_ROWS_S1] = intToCharArray(
					rows_s1,
					mText[INDEX_ROWS_S1],
					mTextLength[INDEX_ROWS_PREFIX] == 0 ? 0 : mTextLength[INDEX_ROWS_PREFIX]+1,
					mTextMaxChars[INDEX_ROWS_S1],
					null, null ) ;
		}
		if ( tempRowSS != rows_ss ) {
			rows_ss = tempRowSS ;
			mGlowFinished[INDEX_ROWS_SS] = false ;
			mLastUpdate[INDEX_ROWS_SS] = updateTime ;
			mTextLength[INDEX_ROWS_SS] = intToCharArray(
					rows_ss,
					mText[INDEX_ROWS_SS],
					mTextLength[INDEX_ROWS_PREFIX] == 0 ? 0 : mTextLength[INDEX_ROWS_PREFIX]+1,
					mTextMaxChars[INDEX_ROWS_SS],
					null, null ) ;
		}
	}
	
	
	private void loadDimensions(Resources res) {
		mMarginVerticalIdeal = new int [NUM_INDICES] ;
		mMarginHorizontalIdeal = new int [NUM_INDICES] ;
		
		int horiz = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_horizontal_margin_label) ;
		for ( int i = 0; i < NUM_INDICES; i++ )
			mMarginHorizontalIdeal[i] = horiz ;
		// labels get this offset
		mMarginHorizontalIdeal[INDEX_TIME] = res.getDimensionPixelOffset(R.dimen.game_interface_score_detailed_horizontal_margin_time) ;
		mMarginHorizontalIdeal[INDEX_SCORE] = res.getDimensionPixelOffset(R.dimen.game_interface_score_detailed_horizontal_margin_score) ;
		mMarginHorizontalIdeal[INDEX_ADDITION] = res.getDimensionPixelOffset(R.dimen.game_interface_score_detailed_horizontal_margin_addition) ;
		mMarginHorizontalIdeal[INDEX_MULTIPLIER] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_horizontal_margin_multiplier) ;
		mMarginHorizontalIdeal[INDEX_ROWS_S0] = res.getDimensionPixelOffset(R.dimen.game_interface_score_detailed_horizontal_margin_rows) ;
		mMarginHorizontalIdeal[INDEX_ROWS_S1] = res.getDimensionPixelOffset(R.dimen.game_interface_score_detailed_horizontal_margin_rows) ;
		mMarginHorizontalIdeal[INDEX_ROWS_SS] = res.getDimensionPixelOffset(R.dimen.game_interface_score_detailed_horizontal_margin_rows) ;
		
		for ( int i = 0; i < NUM_INDICES; i++ )
			mMarginVerticalIdeal[i] = 0 ;
		mMarginVerticalIdeal[INDEX_LEVEL] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_level) ;
		if ( mShowTime ) {
			mMarginVerticalIdeal[INDEX_TIME] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_time) ;
			mMarginVerticalIdeal[INDEX_SCORE] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_score_with_time) ;
		} else {
			mMarginVerticalIdeal[INDEX_TIME] = 0 ;
			mMarginVerticalIdeal[INDEX_SCORE] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_score_no_time) ;
		}
		mMarginVerticalIdeal[INDEX_ADDITION] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_addition) ;
		mMarginVerticalIdeal[INDEX_MULTIPLIER] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_multiplier) ;
		mMarginVerticalIdeal[INDEX_ROWS_S0] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_rows) ;
		mMarginVerticalIdeal[INDEX_ROWS_S1] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_rows_again) ;
		mMarginVerticalIdeal[INDEX_ROWS_SS] = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_rows) ;
		mMarginVerticalTopBottomIdeal = res.getDimensionPixelSize(R.dimen.game_interface_score_detailed_vertical_margin_top_bottom) ;
	
		setIdealMargins() ;
	}
	
	private void setIdealMargins() {
		mMarginVertical = ArrayOps.duplicate( mMarginVerticalIdeal ) ;
		mMarginHorizontal = ArrayOps.duplicate( mMarginHorizontalIdeal ) ;
		mMarginVerticalTopBottom =  mMarginVerticalTopBottomIdeal ;
	}
	
	
	private void initializeTextMembers( Resources res ) {
		mText = new char[NUM_INDICES][] ;
		mTextLength = new int[NUM_INDICES] ;
		
		// avoid null pointers
		for ( int i = 0; i < NUM_INDICES; i++ )
			mText[i] = new char[0] ;
		
		mTextLabelString = new String[NUM_INDICES] ;
		
		// load labels
		mTextLabelString[INDEX_TIME_LABEL] = res.getString(R.string.game_interface_score_detailed_label_time) ;
		mTextLabelString[INDEX_SCORE_LABEL] = res.getString(R.string.game_interface_score_detailed_label_score) ;
		mTextLabelString[INDEX_ROWS_LABEL] = res.getString(R.string.game_interface_score_detailed_label_rows) ;
		
		// load prefixes
		mTextLabelString[INDEX_LEVEL_PREFIX] = res.getString(R.string.game_interface_score_detailed_prefix_level) ;
		mTextLabelString[INDEX_TIME_PREFIX] = res.getString(R.string.game_interface_score_detailed_prefix_time) ;
		mTextLabelString[INDEX_SCORE_PREFIX] = res.getString(R.string.game_interface_score_detailed_prefix_score) ;
		mTextLabelString[INDEX_ADDITION_PREFIX] = res.getString(R.string.game_interface_score_detailed_prefix_addition) ;
		mTextLabelString[INDEX_MULTIPLIER_PREFIX] = res.getString(R.string.game_interface_score_detailed_prefix_multiplier) ;
		mTextLabelString[INDEX_ROWS_PREFIX] = res.getString(R.string.game_interface_score_detailed_prefix_rows) ;
		
		// load title
		mTextLabelString[INDEX_TITLE] =
				ginfo == null
					? "No Game Mode"
					: GameModes.name(ginfo.mode).toUpperCase() ;
		
		// Convert to char arrays
		for ( int i = 0; i < mTextLabelString.length; i++ )
			if ( mTextLabelString[i] != null )
				mText[i] = mTextLabelString[i].toCharArray() ;
		
		// init lengths
		for ( int i = 0; i < NUM_INDICES; i++ )
			mTextLength[i] = mText[i].length ;
		
		// Initialize main texts and add prefixes
		for ( int i = NUM_INDICES_LABELS + NUM_INDICES_PREFIXES + 1; i < NUM_INDICES; i++ )
			mText[i] = new char[MAX_LONG_DIGITS + 32] ;
		// add prefixes to the main texts
		mTextLength[INDEX_LEVEL] = addPrefix( INDEX_LEVEL_PREFIX, INDEX_LEVEL ) ;
		mTextLength[INDEX_TIME] = addPrefix( INDEX_SCORE_PREFIX, INDEX_TIME ) ;
		mTextLength[INDEX_SCORE] = addPrefix( INDEX_SCORE_PREFIX, INDEX_SCORE ) ;
		mTextLength[INDEX_ADDITION] = addPrefix( INDEX_ADDITION_PREFIX, INDEX_ADDITION ) ;
		mTextLength[INDEX_MULTIPLIER] = addPrefix( INDEX_MULTIPLIER_PREFIX, INDEX_MULTIPLIER ) ;
		mTextLength[INDEX_ROWS_S0] = addPrefix( INDEX_ROWS_PREFIX, INDEX_ROWS_S0 ) ;
		mTextLength[INDEX_ROWS_S1] = addPrefix( INDEX_ROWS_PREFIX, INDEX_ROWS_S1 ) ;
		mTextLength[INDEX_ROWS_SS] = addPrefix( INDEX_ROWS_PREFIX, INDEX_ROWS_SS ) ;
 	}
	
	
	private int addPrefix( int prefixIndex, int index ) {
		if ( mTextLength[prefixIndex] == 0 )
			return 0 ;
		
		for ( int i = 0; i < mTextLength[prefixIndex]; i++ )
			mText[index][i] = mText[prefixIndex][i] ;
		mText[index][ mTextLength[prefixIndex] ] = SEP_CHAR ;
		
		return mTextLength[prefixIndex] + 1 ;
	}
	
	
	private void loadPaintsAndColors( Resources res, ColorScheme cs, boolean bigText ) {
		mPaint = new Paint[NUM_INDICES] ;
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			mPaint[i] = new Paint() ;
			mPaint[i].setAntiAlias(true) ;
		}
		
		for ( int i = 0; i < NUM_INDICES; i++ )
			mPaint[i].setColor(res.getColor(R.color.game_interface_score_text_color_stable)) ;
		// set sizes.  All use standard sizes, except the title and labels.
		if ( bigText ) {
			for ( int i = 0; i < NUM_INDICES; i++ )
				mPaint[i].setTextSize(res.getDimension(R.dimen.game_interface_score_text_size_large)) ;
			for ( int i = 0; i < NUM_INDICES_LABELS; i++ )
				mPaint[i].setTextSize(res.getDimension(R.dimen.game_interface_score_header_text_size_large)) ;
		} else {
			for ( int i = 0; i < NUM_INDICES; i++ )
				mPaint[i].setTextSize(res.getDimension(R.dimen.game_interface_score_text_size)) ;
			for ( int i = 0; i < NUM_INDICES_LABELS; i++ )
				mPaint[i].setTextSize(res.getDimension(R.dimen.game_interface_score_header_text_size)) ;
		}
		if ( bigText )
			mPaint[INDEX_TITLE].setTextSize(res.getDimension(R.dimen.game_interface_score_title_text_size_large)) ;
		else
			mPaint[INDEX_TITLE].setTextSize(res.getDimension(R.dimen.game_interface_score_title_text_size)) ;
		mPaint[INDEX_TITLE].setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		
		mTitleSize = mPaint[INDEX_TITLE].getTextSize() ;
		
		// special case: S0, S1 rows are color-coded!
		mPaint[INDEX_ROWS_S0].setColor( cs == null ? res.getColor(R.color.standard_s0_fill) : cs.getFillColor(QOrientations.S0, 0) ) ;
		mPaint[INDEX_ROWS_S1].setColor(cs == null ? res.getColor(R.color.standard_s1_fill) : cs.getFillColor(QOrientations.S1, 1) ) ;
		
		
		// Set alignment.  Title and level are center-aligned; labels are left-aligned;
		// the rest are right-aligned.
		mPaint[INDEX_TITLE].setTextAlign(Paint.Align.CENTER) ;
		mPaint[INDEX_LEVEL].setTextAlign(Paint.Align.CENTER) ;
		mPaint[INDEX_TIME_LABEL].setTextAlign(Paint.Align.LEFT) ;
		mPaint[INDEX_SCORE_LABEL].setTextAlign(Paint.Align.LEFT) ;
		mPaint[INDEX_ROWS_LABEL].setTextAlign(Paint.Align.LEFT) ;
		mPaint[INDEX_TIME].setTextAlign(Paint.Align.LEFT) ;
		mPaint[INDEX_SCORE].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ADDITION].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_MULTIPLIER].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ROWS_S0].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ROWS_S1].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ROWS_SS].setTextAlign(Paint.Align.RIGHT) ;
		
		// Copy to glow paint to get the proper text formatting, set color and shadow layer.
		mGlowPaint = new Paint[NUM_INDICES] ;
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			mGlowPaint[i] = new Paint(mPaint[i]) ;
			mGlowPaint[i].setColor(0x00ffffff) ;
			mGlowPaint[i].setDither(true) ;
			mGlowPaint[i].setAntiAlias(true) ;
		}
		
		mGlowColor = res.getColor(R.color.game_interface_score_text_color_glow) ;
		mGlowRadius = (float)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
    			(float)4, res.getDisplayMetrics()) ;
		alphaInterpolation = new LinearInterpolation( new int[]{255,0}, new long[]{0, GLOW_FADE_TIME}, Interpolation.CLAMP) ;
		
		
		// Copy to glow paint to get text formatting, set color and shadow layer.
		mGlowPaint = new Paint[NUM_INDICES] ;
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			mGlowPaint[i] = new Paint(mPaint[i]) ;
			mGlowPaint[i].setColor(0x00ffffff) ;
			mGlowPaint[i].setDither(true) ;
			mGlowPaint[i].setAntiAlias(true) ;
		}
		
		mGlowColor = res.getColor(R.color.game_interface_score_text_color_glow) ;
		mGlowRadius = (float)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
    			(float)4, res.getDisplayMetrics()) ;
		alphaInterpolation = new LinearInterpolation( new int[]{255,0}, new long[]{0, GLOW_FADE_TIME}, Interpolation.CLAMP) ;
	}
	
	
	private void setTextLimits( Resources res ) {
		mTextHeight = new int[NUM_INDICES] ;
		mTextWidthWithDigits = new int[NUM_INDICES][] ;
		
		// most do not allow digits.  Set as the default.
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			// special case handling for a few...
			char [] text = mText[i] ;
			switch( i ) {
			case INDEX_TIME:
				text = MAX_TIME_STRING.toCharArray() ;
				break ;
			}
			mPaint[i].getTextBounds(text, 0, mTextLength[i], mTextBounds) ;
			mTextHeight[i] = mTextBounds.height() ;
			mTextWidthWithDigits[i] = new int[]{ mTextBounds.width() } ;
		}
		
		// get the maximum number of characters.  Do this by appending 8s; we
		// assume any char we use will have lower width than this.
		mTextMaxChars = new int[NUM_INDICES] ;
		for ( int i = 0; i < NUM_INDICES; i++ )
			mTextMaxChars[i] = Math.max(32 + MAX_LONG_DIGITS, mTextLength[i]) ;
		
		// only some can hold digits; do them now, overwriting the previous results.
		int [] indices = new int[] { INDEX_LEVEL, INDEX_SCORE, INDEX_ADDITION, INDEX_MULTIPLIER, INDEX_ROWS_S0, INDEX_ROWS_S1, INDEX_ROWS_SS } ;
		for ( int i = 0; i < indices.length; i++ ) {
			int index = indices[i] ;
			mTextWidthWithDigits[index] = new int[MAX_LONG_DIGITS] ;
			
			if ( mTextLength[index] > 0 ) {
				mPaint[index].getTextBounds(mText[index], 0, mTextLength[index], mTextBounds) ;
				mTextHeight[index] = Math.max( mTextHeight[index], mTextBounds.height() ) ;
				mTextWidthWithDigits[index][0] = mTextBounds.width();
			} else {
				mTextHeight[index] = 0 ;
				mTextWidthWithDigits[index][0] = 0 ;
			}
			
			// up to MAX_LONG_DIGITS digits, why not.
			for ( int digits = 1; digits < MAX_LONG_DIGITS; digits++ ) {
				long val = 0 ;
				for ( int d = 0; d < digits; d++ )
					val += 8*longMask(digits) ;
				
				int len = longToCharArray( val, mText[index], mTextLength[index], mTextMaxChars[i], null, null ) ;
				
				mPaint[index].getTextBounds(mText[index], 0, len, mTextBounds) ;
				mTextHeight[index] = Math.max( mTextHeight[index], mTextBounds.height() ) ;
				mTextWidthWithDigits[index][digits] = mTextBounds.width() ;
			}
		}
		
		// align text heights: labels and text get the maximum height between them.
		int [][] matches = new int [][] {
			new int[] { INDEX_TIME_LABEL, INDEX_TIME },
			new int[] { INDEX_SCORE_LABEL, INDEX_SCORE },
			new int[] { INDEX_ROWS_LABEL, INDEX_ROWS_S0, INDEX_ROWS_S1, INDEX_ROWS_SS }
		} ;
		for ( int i = 0; i < matches.length; i++ ) {
			int height = Integer.MIN_VALUE ;
			for ( int j = 0; j < matches[i].length; j++ ) {
				height = Math.max(height, mTextHeight[matches[i][j]]) ;
			}
			for ( int j = 0; j < matches[i].length; j++ ) {
				mTextHeight[matches[i][j]] = height ;
			}
		}
		
		// We now have the unbounded widths.  This is all we're going to use, for now.
	}

}
