package com.peaceray.quantro.view.game;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.TypedValue;

import com.peaceray.quantro.R;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.utils.interpolation.Interpolation;
import com.peaceray.quantro.utils.interpolation.LinearInterpolation;
import com.peaceray.quantro.view.colors.ColorScheme;

/**
 * The ScoreDrawer class provides a centralized location for calling "score draw"
 * functions.  It should be instanced, and the instance associated with a GameInformation
 * object.
 * 
 * Draw procedure follows three stages: an "update" with current time,
 * a call to "needsToDraw" to determine if the ScoreDrawer is producing
 * any animations or has updated, and a call to "draw" with a provided draw region.
 * 
 * @author Jake
 *
 */
public class AbbreviatedScoreDrawer extends ScoreDrawer {
	
	private static final String TAG = "AbbreviatedScoreDrawer" ;
	
	private static final int FLOAT_ADDITIONAL_DIGITS = 1 ;
	
	private static final long GLOW_FADE_TIME = 1000 ;
	private static final int NUM_GLOW_REPS = 2 ;
	
	// Our local info
	private long time ;
	private int level ;
	private long score ;
	private int addition ;
	private float multiplier ;
	private int rows_s0 ;
	private int rows_s1 ;
	private int rows_ss ;
	
	private boolean localInfoChanged ;
	
	// GameInformation object
	private GameInformation ginfo ;
	
	ColorScheme mColorScheme ;
	
	// Paints for drawing things.  We adjust these paints in each update.
	private Paint [] mPaint ;
	private Paint [] mGlowPaint ;
	private boolean [] mGlowFinished ;
	private int mGlowColor ;
	private float mGlowRadius ;
	private Interpolation alphaInterpolation ;
	
	private static final int INDEX_LEVEL_HEADER = 0 ;
	private static final int INDEX_TIME_HEADER = 1 ;
	private static final int INDEX_SCORE_HEADER = 2 ;
	private static final int INDEX_ADDITION_HEADER = 3 ;
	private static final int INDEX_MULTIPLIER_HEADER = 4 ;
	
	private static final int INDEX_LEVEL = 5 ;
	private static final int INDEX_TIME = 6 ;
	private static final int INDEX_SCORE = 7 ;
	private static final int INDEX_ADDITION = 8 ;
	private static final int INDEX_MULTIPLIER = 9 ;
	private static final int INDEX_ROWS_S0 = 10 ;
	private static final int INDEX_ROWS_S1 = 11 ;
	private static final int INDEX_ROWS_SS = 12 ;
	
	private static final int NUM_INDICES = 13 ; 
	
	
	// Storage for strings.  Anything that updates is stored as a char []
	// to prevent reallocation of strings.
	private char [][] mText ;
	private int [] mTextLength ;
	private int [] mTextHeight ;		// measures the maximum possible height of this text
	private int [] mTextMaxChars ;		// the maximum number of digits that can be displayed without abbreviation.
	private long [] mLastUpdate ;
	
	private float mTitleSize ;
	
	// Where do we draw?
	private Rect drawRegion ;
	
	private int mMarginHorizontal ;
	private int mMarginTime ;
	private int mMarginScore ;
	private int mMarginAddition ;
	private int mMarginMultiplier ;
	private int mMarginRows ;
	
	private int mMarginHorizontalIdeal ;
	private int mMarginTimeIdeal ;
	private int mMarginScoreIdeal ;
	private int mMarginAdditionIdeal ;
	private int mMarginMultiplierIdeal ;
	private int mMarginRowsIdeal ;
	
	// special row info
	private int mMarginBetweenRows ;
	private int mTextWidthRowsS1 ;
	
	private boolean mSeparateRowDisplays ;
	private boolean mShowTime ;
	
	private Rect mTextBounds ;
	
	public AbbreviatedScoreDrawer( GameInformation ginfo, Resources res, Rect region, boolean bigText ) {
		this.ginfo = ginfo ;
		drawRegion = new Rect(region) ;
		
		mTextBounds = new Rect() ;
		
		mShowTime = ginfo == null ? false
				: GameModes.measurePerformanceBy(ginfo.mode) == GameModes.MEASURE_PERFORMANCE_BY_TIME ;
		
		initializeTextMembers(res) ;
		
		loadDimensions(res) ;
		loadPaintsAndColors(res, bigText) ;
		
		setMaxCharacters(res) ;
		
		shrinkMargins(res) ;
		
		// Force an update by setting to invalid values
		time = -1 ;
		level = -1 ;
		score = -1 ;
		addition = -999999999 ;
		multiplier = -1f ;
		rows_s0 = rows_s1 = rows_ss = -1 ;
		mSeparateRowDisplays = ginfo == null ? true : GameModes.numberQPanes(ginfo) == 2 ;
		
		mLastUpdate = new long[NUM_INDICES] ;
		mGlowFinished = new boolean[NUM_INDICES] ;
		for ( int i = 0; i < NUM_INDICES; i++ )
			mGlowFinished[i] = true ;
		updateLocalInfo( 0 ) ;
	}
	
	@Override
	public float titleSize() {
		return mTitleSize ;
	}
	
	public void update( long currentTime ) {
		updateLocalInfo( currentTime ) ;
	}
	
	public int height() {
		int height = mTextHeight[INDEX_LEVEL] ;
		// We display either score, addition, mult, OR time, rows.
		if ( mShowTime ) {
			height += mMarginTime + mTextHeight[INDEX_TIME] ;
			height += mMarginRows + Math.max( mTextHeight[INDEX_ROWS_SS],
					Math.max( mTextHeight[INDEX_ROWS_S0], mTextHeight[INDEX_ROWS_S1] ) ) ;
		} else {
			height += mTextHeight[INDEX_SCORE] + mMarginScore ;
			height += mTextHeight[INDEX_ADDITION] + mMarginAddition ;
			height += mTextHeight[INDEX_MULTIPLIER] + mMarginMultiplier ;
		}
		return height ;
	}
	
	@Override
	public Rect size() {
		return size(null) ;
	}
	
	public Rect size( Rect in ) {
		if ( in == null )
			in = new Rect() ;
		
		in.set( 0, 0, drawRegion.width(), height() ) ;
		return in ;
	}
	
	public boolean needsToDraw() {
		for ( int i = 0; i < NUM_INDICES; i++ )
			if ( !mGlowFinished[i] )
				return true ;
		return localInfoChanged ;
	}
	
	char [] test_char_array = new char[] { 't', 'e', 's', 't' } ;
	
	public void drawToCanvas(Canvas canvas, long currentTime) {
		// Clip!
		canvas.save();
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
		
		// determine the total height, and center within the draw region.
		int height = height() ;
		
		// Level is centered horizontally...
		int y = (drawRegion.height() - height)/2 + drawRegion.top ;
		int x = drawRegion.centerX() ;
		drawText( canvas, INDEX_LEVEL, x, y, mGlowFinished[INDEX_LEVEL] ? 0 : NUM_GLOW_REPS ) ;
		y += mTextHeight[INDEX_LEVEL] ;
		
		// Reposition X: the others are right-aligned, not center.
		x = drawRegion.right - mMarginHorizontal ;
		
		if ( mShowTime ) {
			y += mMarginTime ;
			drawText( canvas, INDEX_TIME, x, y, mGlowFinished[INDEX_TIME] ? 0 : NUM_GLOW_REPS ) ;
			y += mTextHeight[INDEX_TIME] ;
			
			// draw rows...
			y += mMarginRows ;
			if ( mSeparateRowDisplays ) {
				drawText( canvas, INDEX_ROWS_S1, x, y, mGlowFinished[INDEX_ROWS_S1] ? 0 : NUM_GLOW_REPS ) ;
				x -= ( mMarginBetweenRows + mTextWidthRowsS1 ) ;
				drawText( canvas, INDEX_ROWS_S0, x, y, mGlowFinished[INDEX_ROWS_S0] ? 0 : NUM_GLOW_REPS ) ;
			} else {
				drawText( canvas, INDEX_ROWS_SS, x, y, mGlowFinished[INDEX_ROWS_SS] ? 0 : NUM_GLOW_REPS ) ;
			}
		} else {
			y += mMarginScore ;
			drawText( canvas, INDEX_SCORE, x, y, mGlowFinished[INDEX_SCORE] ? 0 : NUM_GLOW_REPS ) ;
			y += mTextHeight[INDEX_SCORE] ;
			
			// Draw addition, move down...
			y += mMarginAddition ;
			drawText( canvas, INDEX_ADDITION, x, y, mGlowFinished[INDEX_ADDITION] ? 0 : NUM_GLOW_REPS ) ;
			y += mTextHeight[INDEX_ADDITION] ;
			
			// Draw multiplier...
			y += mMarginMultiplier ;
			drawText( canvas, INDEX_MULTIPLIER, x, y, mGlowFinished[INDEX_MULTIPLIER] ? 0 : NUM_GLOW_REPS ) ;
		}
		
		canvas.restore() ;
		localInfoChanged = false ;
	}
	
	
	private void drawText( Canvas canvas, int index, int x, int y, int glowReps ) {
		for ( int i = 0; i < glowReps; i++ ) {
			canvas.drawText(
					mText[index], 0, mTextLength[index],
					x,
					y + mTextHeight[index],
					mGlowPaint[index]) ;
		}
		canvas.drawText(
				mText[index], 0, mTextLength[index],
				x,
				y + mTextHeight[index],
				mPaint[index]) ;
	}
	
	
	public void setColorScheme( ColorScheme colorScheme ) {
		mColorScheme = colorScheme ;
		// S0, S1 color for rows cleared...
		if ( mColorScheme != null ) {
			mPaint[INDEX_ROWS_S0].setColor(mColorScheme.getFillColor(QOrientations.S0, 0)) ;
			mPaint[INDEX_ROWS_S1].setColor(mColorScheme.getFillColor(QOrientations.S1, 1)) ;
		}
	}
	
	
	private void initializeTextMembers( Resources res ) {
		mText = new char[NUM_INDICES][] ;
		mTextLength = new int[NUM_INDICES] ;
		
		mText[INDEX_LEVEL_HEADER] = res.getString(R.string.game_interface_score_header_prefix_level).toCharArray() ;
		mText[INDEX_TIME_HEADER] = res.getString(R.string.game_interface_score_header_prefix_time).toCharArray() ;
		mText[INDEX_SCORE_HEADER] = res.getString(R.string.game_interface_score_header_prefix_score).toCharArray() ;
		mText[INDEX_ADDITION_HEADER] = res.getString(R.string.game_interface_score_prefix_addition).toCharArray() ;
		mText[INDEX_MULTIPLIER_HEADER] = res.getString(R.string.game_interface_score_prefix_multiplier).toCharArray() ;
		
		// Allocate some char arrays!
		mText[INDEX_LEVEL] = new char[32] ;
		mText[INDEX_TIME] = new char[32] ;
		mText[INDEX_SCORE] = new char[32] ;
		mText[INDEX_ADDITION] = new char[32] ;
		mText[INDEX_MULTIPLIER] = new char[32] ;
		mText[INDEX_ROWS_S0] = new char[32] ;
		mText[INDEX_ROWS_S1] = new char[32] ;
		mText[INDEX_ROWS_SS] = new char[32] ;

		initializeTextMembers_copyAndSetHeaderLength( INDEX_LEVEL_HEADER, INDEX_LEVEL ) ;
		initializeTextMembers_copyAndSetHeaderLength( INDEX_TIME_HEADER, INDEX_TIME ) ;
		initializeTextMembers_copyAndSetHeaderLength( INDEX_SCORE_HEADER, INDEX_SCORE ) ;
		initializeTextMembers_copyAndSetHeaderLength( INDEX_ADDITION_HEADER, INDEX_ADDITION ) ;
		initializeTextMembers_copyAndSetHeaderLength( INDEX_MULTIPLIER_HEADER, INDEX_MULTIPLIER ) ;
		
		mTextLength[INDEX_ROWS_S0] = 0 ;
		mTextLength[INDEX_ROWS_S1] = 0 ;
		mTextLength[INDEX_ROWS_SS] = 0 ;
 	}
	
	
	private void initializeTextMembers_copyAndSetHeaderLength( int header, int text ) {
		for ( int i = 0; i < mText[header].length; i++ )
			mText[text][i] = mText[header][i] ;
		if ( mText[header].length > 0 ) {
			mText[text][mText[header].length] = ' ' ;
			mTextLength[text] = mText[header].length + 1 ;
		}
		
		mTextLength[header] = mText[header].length ;
	}
	
	
	private void loadDimensions(Resources res) {
		mMarginHorizontalIdeal = res.getDimensionPixelSize( R.dimen.game_interface_score_abbr_horizontal_margin );
		mMarginTimeIdeal = res.getDimensionPixelSize( R.dimen.game_interface_score_abbr_vertical_margin_time ) ;
		mMarginScoreIdeal = res.getDimensionPixelSize( R.dimen.game_interface_score_abbr_vertical_margin_score ) ;
		mMarginAdditionIdeal = res.getDimensionPixelSize( R.dimen.game_interface_score_abbr_vertical_margin_addition ) ;
		mMarginMultiplierIdeal = res.getDimensionPixelSize( R.dimen.game_interface_score_abbr_vertical_margin_multiplier ) ;
		mMarginRowsIdeal = res.getDimensionPixelSize( R.dimen.game_interface_score_abbr_vertical_margin_rows ) ;
		
		mMarginHorizontal = mMarginHorizontalIdeal ;
		mMarginTime = mMarginTimeIdeal ;
		mMarginScore = mMarginScoreIdeal ;
		mMarginAddition = mMarginAdditionIdeal ;
		mMarginMultiplier = mMarginMultiplierIdeal ;
		mMarginRows = mMarginRowsIdeal ;
		
		mMarginBetweenRows = res.getDimensionPixelSize( R.dimen.game_interface_score_abbr_horizontal_margin_between_rows ) ;
	}
	
	
	private void updateLocalInfo( long updateTime ) {
		int tempLevel ;
		long tempTime ;
		long tempScore ;
		int tempAddition ;
		float tempMultiplier ;
		int tempRowS0, tempRowS1, tempRowSS ;
		synchronized( ginfo ) {
			tempLevel = ginfo.level ;
			tempTime = ginfo.milliseconds ;
			tempScore = ginfo.score ;
			tempAddition = ginfo.addition ;
			tempMultiplier = ginfo.multiplier ;
			tempRowSS = ginfo.sLclears + ginfo.moclears ;
			tempRowS0 = ginfo.s0clears + tempRowSS ;
			tempRowS1 = ginfo.s1clears + tempRowSS ;
		}
		
		localInfoChanged = ( tempLevel != level
				|| tempTime != time || tempRowSS != this.rows_ss
				|| tempRowS0 != rows_s0 || tempRowS1 != rows_s1
				|| tempScore != score || tempAddition != addition
				|| tempMultiplier != multiplier ) ;
		
		// We note every change EXCEPT a decreasing multiplier or a change
		// in 'time' that did not change the 'minutes.'
		// TODO: If a decreasing mulitplier is significant, change this.
		
		// Updates.
		if ( tempLevel != level ) {
			level = tempLevel ;
			mLastUpdate[INDEX_LEVEL] = updateTime ;
			mGlowFinished[INDEX_LEVEL] = false ;
			mTextLength[INDEX_LEVEL] = intToCharArray(
					level,
					mText[INDEX_LEVEL],
					mTextLength[INDEX_LEVEL_HEADER] == 0 ? 0 : mTextLength[INDEX_LEVEL_HEADER]+1,
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
					mTextLength[INDEX_TIME_HEADER] == 0 ? 0 : mTextLength[INDEX_TIME_HEADER]+1,
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
					mTextLength[INDEX_SCORE_HEADER] == 0 ? 0 : mTextLength[INDEX_SCORE_HEADER]+1,
					mTextMaxChars[INDEX_SCORE], null, null ) ;
		}
		if ( tempAddition != addition ) {
			addition = tempAddition ;
			mLastUpdate[INDEX_ADDITION] = updateTime ;
			mGlowFinished[INDEX_ADDITION] = false ;
			mTextLength[INDEX_ADDITION] = intToCharArray(
					addition,
					mText[INDEX_ADDITION],
					mTextLength[INDEX_ADDITION_HEADER] == 0 ? 0 : mTextLength[INDEX_ADDITION_HEADER]+1,
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
					mTextLength[INDEX_MULTIPLIER_HEADER] == 0 ? 0 : mTextLength[INDEX_MULTIPLIER_HEADER]+1,
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
					0,
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
					0,
					mTextMaxChars[INDEX_ROWS_S1],
					null, null ) ;
			mPaint[INDEX_ROWS_S1].getTextBounds(mText[INDEX_ROWS_S1], 0, mTextLength[INDEX_ROWS_S1], mTextBounds) ;
			this.mTextWidthRowsS1 = mTextBounds.width() ;
		}
		if ( tempRowSS != rows_ss ) {
			rows_ss = tempRowSS ;
			mGlowFinished[INDEX_ROWS_SS] = false ;
			mLastUpdate[INDEX_ROWS_SS] = updateTime ;
			mTextLength[INDEX_ROWS_SS] = intToCharArray(
					rows_ss,
					mText[INDEX_ROWS_SS],
					0,
					mTextMaxChars[INDEX_ROWS_SS],
					null, null ) ;
		}
	}
	
	

	
	private void loadPaintsAndColors( Resources res, boolean bigText ) {
		mPaint = new Paint[NUM_INDICES] ;
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			mPaint[i] = new Paint() ;
			mPaint[i].setAntiAlias(true) ;
			mPaint[i].setColor(res.getColor(R.color.game_interface_score_text_color_stable)) ;
			if ( bigText ) {
				mPaint[i].setTextSize(res.getDimension(R.dimen.game_interface_score_header_text_size_large)) ;
			} else {
				mPaint[i].setTextSize(res.getDimension(R.dimen.game_interface_score_header_text_size)) ;
			}
		}
		
		
		mTitleSize = mPaint[INDEX_LEVEL_HEADER].getTextSize() ;
	
		// align the level centered; put the rest right-aligned.
		mPaint[INDEX_LEVEL].setTextAlign(Paint.Align.CENTER) ;
		mPaint[INDEX_TIME].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_SCORE].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ADDITION].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_MULTIPLIER].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ROWS_S0].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ROWS_S1].setTextAlign(Paint.Align.RIGHT) ;
		mPaint[INDEX_ROWS_SS].setTextAlign(Paint.Align.RIGHT) ;
		
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
		
		mTextHeight = new int[NUM_INDICES] ;
		StringBuilder sb = new StringBuilder() ;
		sb.append("01234,56789.kmgt: kmgt") ;
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			if ( mText[i] != null )
				sb.append(mText[i], 0, mTextLength[i] ).append(" ") ;
		}
		String maxHeightString = sb.toString();
		
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			mPaint[i].getTextBounds(maxHeightString, 0, maxHeightString.length(), mTextBounds) ;
			mTextHeight[i] = mTextBounds.height() ;
		}
	}
	
	
	private void setMaxCharacters( Resources res ) {
		mTextMaxChars = new int[NUM_INDICES] ;
		
		int maxWidth = drawRegion.width() - mMarginHorizontal*2 ;
		
		// estimate using 8s as an assumed max-length integer.
		for ( int i = 0; i < NUM_INDICES; i++ ) {
			if ( i == INDEX_TIME || i == INDEX_LEVEL || i == INDEX_SCORE || i == INDEX_ADDITION || i == INDEX_MULTIPLIER ) {
				// check the maximum number of additional characters.
				// Time is formatted differently, but we expect "all 8s" is
				// a good overestimate of the actual formatted size.
				mTextMaxChars[i] = 32 ;
				for ( int j =  mTextLength[i]; j < mText[i].length; j++ ) {
					mText[i][j] = '8' ;
					mPaint[i].getTextBounds(mText[i], 0, j+1, mTextBounds) ;
					if ( mTextBounds.width() > maxWidth ) {
						mTextMaxChars[i] = j ;
						break ;
					}
				}
			} else if ( i == INDEX_ROWS_S0 || i == INDEX_ROWS_S1 || i == INDEX_ROWS_SS ) {
				// check the maximum number of additional characters.
				// if we display rows separately, double the length and add
				// the margin between.
				mTextMaxChars[i] = 32 ;
				for ( int j = mTextLength[i]; j < mText[i].length; j++ ) {
					mText[i][j] = '8' ;
					mPaint[i].getTextBounds(mText[i], 0, j+1, mTextBounds) ;
					if ( mTextBounds.width() * 2 + mMarginBetweenRows > maxWidth ) {
						mTextMaxChars[i] = j ;
						break ;
					}
				}
			}
		}
		
		// Quick fix: make sure the whole level number can fit.  If not, abbreviate the level.
		if ( mTextMaxChars[INDEX_LEVEL] < mTextLength[INDEX_LEVEL_HEADER] + 3 ) {
			mText[INDEX_LEVEL_HEADER] = res.getString(R.string.game_interface_score_header_prefix_level_abbr).toCharArray() ;
			mTextLength[INDEX_LEVEL_HEADER] = mText[INDEX_LEVEL_HEADER].length ;
			for ( int i = 0; i < mTextLength[INDEX_LEVEL_HEADER]; i++ )
				mText[INDEX_LEVEL][i] = mText[INDEX_LEVEL_HEADER][i] ;
			if ( mTextLength[INDEX_LEVEL_HEADER] > 0 ) {
				mText[INDEX_LEVEL][mTextLength[INDEX_LEVEL_HEADER]] = ' ' ;
				mTextLength[INDEX_LEVEL] = mTextLength[INDEX_LEVEL_HEADER] + 1 ;
			}
		}
	}
	
	private void shrinkMargins( Resources res ) {
		while ( height() > drawRegion.height() ) {
			boolean changed = false ;
			if ( mMarginTime > 1 ) {
				mMarginTime-- ;
				changed = true ;
			}
			if ( mMarginScore > 1 ) {
				mMarginScore-- ;
				changed = true ;
			}
			if ( mMarginAddition > 1 ) {
				mMarginAddition-- ;
				changed = true ;
			}
			if ( mMarginMultiplier > 1 ) {
				mMarginMultiplier-- ;
				changed = true ;
			}
			if ( mMarginRows > 1 ) {
				mMarginRows-- ;
				changed = true ;
			}
			if ( !changed )
				return ;
		}
	}
	
}
