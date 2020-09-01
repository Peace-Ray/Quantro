package com.peaceray.quantro.view.game;

import com.peaceray.quantro.R;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class PlayerStatusDrawer {
	
	private static final String TAG = "PlayerStatusDrawer" ;
	
	private String [] mPlayerNameText ;
	private String [] mPlayerNumberText ;
	private String [] mStatusDisplayedText ;
	private long mLingerMilliseconds ;
	private long mDismissMilliseconds ;
	
	// text to display upon a game-over.
	private String mGameOverTextTitle ;
	private String mGameOverTextSubtitle ;
	
	// The full-width game region.
	private Rect mGameRegion ;
	private Rect mGameOverRegion ;
	// Our self-set portion of the game region, within which we place the number text.
	private Rect mTextNumberDrawRegion ;
	private Rect mTextBounds ;
	private Rect mGameOverTextTitleBounds ;
	private Rect mGameOverTextSubtitleBounds ;
	
	private Resources mResources ;
	
	
	// text drawing detail
	private int mTextSize ;
	private int mMarginHorizontal ;
	private int mMarginVertical ;
	
	// Text colors / sizes: we iterate through to draw shadows.
	private float [] mShadowRadius ;
	private float [] mShadowX ;
	private float [] mShadowY ;
	private int [] mShadowColor ;
	private int mTextColor ;
	private int mTextColorInactive ;
	// Paints
	private Paint [] mPaintText ;
	
	// Game over paint.
	private Paint [] mPaintGameOverTitleText ;
	private Paint [] mPaintGameOverSubtitleText ;
	
	
	// Player number fades out after display.
	int mCurrentPlayerNumber ;
	long mCurrentMillisecondsSpentDisplaying ;
	long mCurrentTimeWhenDisplayed ;
	boolean mCurrentShouldDismiss ;
	private long mLocalMillisecondsSpentGameOver ;
	PlayerGameStatusDisplayed [] mPlayerGameStatus ;
	
	int mPlayerSlot ;
	
	public PlayerStatusDrawer( Resources res, Rect gameRegion, Rect gameOverRegion, String [] playerNames ) {
		mResources = res ;
		
		mLingerMilliseconds = 2000 ;
		mDismissMilliseconds = 1000 ;
		
		mGameRegion = new Rect( gameRegion ) ;
		mGameOverRegion = new Rect( gameOverRegion ) ;
		mTextNumberDrawRegion = new Rect() ;
		mTextBounds = new Rect() ;
		
		mGameOverTextTitleBounds = new Rect() ;
		mGameOverTextSubtitleBounds = new Rect() ;
		
		// player text!  Player index 0 is written as 1, etc.
		mPlayerNameText = playerNames.clone() ;
		mPlayerNumberText = new String[mPlayerNameText.length] ;
		mStatusDisplayedText = new String[PlayerGameStatusDisplayed.values().length] ;
		for ( int i = 0; i < mPlayerNumberText.length; i++ ) {
			mPlayerNumberText[i] = "" + (i + 1) ;
		}
		
		mPlayerGameStatus = new PlayerGameStatusDisplayed[mPlayerNameText.length] ;
		for ( int i = 0; i < mPlayerGameStatus.length; i++ ) {
			mPlayerGameStatus[i] = PlayerGameStatusDisplayed.ACTIVE ;
		}
		
		if ( playerNames.length > 2 ) {
			mGameOverTextTitle = res.getString(R.string.game_interface_status_drawer_game_over_title) ;
			mGameOverTextSubtitle = res.getString(R.string.game_interface_status_drawer_game_over_subtitle_is_host) ;
		} else {
			mGameOverTextTitle = null ;
			mGameOverTextSubtitle = null ;
		}
		
		mLocalMillisecondsSpentGameOver = 0 ;
		
		setDisplayedPlayer(-1, true) ;
		
		createPaints() ;
		createStatusDisplayedText(res) ;
		
		formatToFit() ;
	}
	
	
	public void setGameRegion( Rect gameRegion ) {
		mGameRegion.set(gameRegion) ;
		
		// reset text sizes
		for ( int j = 0; j < mPaintText.length; j++ ) {
			mPaintText[j].setTextSize(mTextSize) ;
		}
		
		formatToFit() ;
	}
	
	
	public void setPlayerNames( String [] names ) {
		mPlayerNameText = names.clone() ;
		mPlayerNumberText = new String[mPlayerNameText.length] ;
		for ( int i = 0; i < mPlayerNumberText.length; i++ ) {
			mPlayerNumberText[i] = "" + (i + 1) ;
		}
		
		// reset text sizes
		for ( int j = 0; j < mPaintText.length; j++ ) {
			mPaintText[j].setTextSize(mTextSize) ;
		}
		
		if ( names.length > 2 ) {
			mGameOverTextTitle = mResources.getString(R.string.game_interface_status_drawer_game_over_title) ;
			mGameOverTextSubtitle = mPlayerSlot == 0
					? mResources.getString(R.string.game_interface_status_drawer_game_over_subtitle_is_host)
					: mResources.getString(R.string.game_interface_status_drawer_game_over_subtitle_is_client) ;
		} else {
			mGameOverTextTitle = null ;
			mGameOverTextSubtitle = null ;
		}
		
		formatToFit() ;
	}
	
	public void setPlayerStatus( int player, PlayerGameStatusDisplayed status ) {
		this.mPlayerGameStatus[player] = status ;
	}
	
	public void setPlayerStatus( PlayerGameStatusDisplayed [] statuses ) {
		for ( int i = 0; i < this.mPlayerGameStatus.length; i++ ) {
			this.mPlayerGameStatus[i] = statuses[i] ;
		}
	}
	
	
	public void setLocalPlayer( int num ) {
		boolean changed = mPlayerSlot != num ;
		if ( changed ) {
			mPlayerSlot = num ;
			if ( mPlayerSlot == 0 && mPlayerNameText.length > 2 ) {
				// host?
				mGameOverTextSubtitle = mResources.getString(R.string.game_interface_status_drawer_game_over_subtitle_is_host) ;
			} else if ( mPlayerNameText.length > 2 ) {
				// client?
				mGameOverTextSubtitle = mResources.getString(R.string.game_interface_status_drawer_game_over_subtitle_is_client) ;
			}
			formatToFit() ;
		}
	}
	
	public void setDisplayedPlayer( int num, boolean dismissAutomatically ) {
		mCurrentPlayerNumber = num ;
		mCurrentMillisecondsSpentDisplaying = 0 ;
		mCurrentTimeWhenDisplayed = System.currentTimeMillis() ;
		
		mCurrentShouldDismiss = dismissAutomatically ;
	}
	
	
	
	public void drawToCanvas( Canvas canvas, long millisSinceLastDraw, boolean showPlayer, boolean showGameOver ) {
		// Clip the draw region
		canvas.save() ;
		canvas.clipRect(mGameRegion) ;
		
		if ( showPlayer )
			drawToCanvasDisplayedPlayer( canvas, millisSinceLastDraw ) ;
		if ( showGameOver )
			drawToCanvasLocalPlayer( canvas, millisSinceLastDraw ) ;
			
		// Done.
		canvas.restore() ;
	}
	
	
	private void drawToCanvasDisplayedPlayer( Canvas canvas, long millisSinceLastDraw ) {
		// First: determine if we have any tooltips to draw.
		if ( mCurrentPlayerNumber < 0 || mCurrentPlayerNumber >= mPlayerNumberText.length )
			return ;
		if ( mCurrentShouldDismiss && mCurrentMillisecondsSpentDisplaying >= mLingerMilliseconds + mDismissMilliseconds )
			return ;
		
		mCurrentMillisecondsSpentDisplaying += millisSinceLastDraw ;
		
		// Second: prepare for a draw by setting alpha values.
		int color = mTextColor ;
		switch( this.mPlayerGameStatus[mCurrentPlayerNumber] ) {
		case LOST:
		case KICKED:
		case QUIT:
		case INACTIVE:
			color = mTextColorInactive ;
		}
		for ( int j = 0; j < mPaintText.length; j++ ) {
			if ( !mCurrentShouldDismiss || mCurrentMillisecondsSpentDisplaying < mLingerMilliseconds ) {
				mPaintText[j].setColor(color) ;
				mPaintText[j].setAlpha(255) ;
				mPaintText[j].setShadowLayer(
						mShadowRadius[j],
						mShadowX[j],
						mShadowY[j],
						0xff000000 | ( mShadowColor[j] & 0x00ffffff ) ) ;
			} else if ( mCurrentShouldDismiss ) {
				int alpha = Math.round( 255 *
						( (float)(mDismissMilliseconds -
								(mCurrentMillisecondsSpentDisplaying-mLingerMilliseconds)) )
								/ mDismissMilliseconds ) ;
				int alphaC = Color.argb(alpha, 0, 0, 0) ;
				mPaintText[j].setColor(color) ;
				mPaintText[j].setAlpha(alpha) ;
				mPaintText[j].setShadowLayer(
						mShadowRadius[j],
						mShadowX[j],
						mShadowY[j],
						alphaC | ( mShadowColor[j] & 0x00ffffff ) ) ;
			}
		}
		
		// Fourth: find x/y coordinates.
		int x = mTextNumberDrawRegion.right - this.mMarginHorizontal ;
		int y = mTextNumberDrawRegion.centerY() ;
		
		// Draw!
		for ( int j = mPaintText.length-1; j >= 0; j-- )
			canvas.drawText(mPlayerNumberText[mCurrentPlayerNumber], x, y, mPaintText[j]) ;
		if ( mPlayerNameText[mCurrentPlayerNumber] != null ) {
			x = mTextNumberDrawRegion.left ;
			for ( int j = mPaintText.length-1; j >= 0; j-- )
				canvas.drawText(mPlayerNameText[mCurrentPlayerNumber], x, y, mPaintText[j]) ;
		}
		
		// Draw status...
		x = mTextNumberDrawRegion.right - this.mMarginHorizontal ;
		y -= mTextNumberDrawRegion.height() - this.mMarginVertical ;
		for ( int j = mPaintText.length-1; j >= 0; j-- ) {
			canvas.drawText(
					mStatusDisplayedText[mPlayerGameStatus[mCurrentPlayerNumber].ordinal()],
					x, y, mPaintText[j]) ;
		}
	}
	
	private void drawToCanvasLocalPlayer( Canvas canvas, long millisSinceLastDraw ) {
		int x, y ;
		if ( mGameOverTextTitle != null && mPlayerGameStatus[mPlayerSlot] != PlayerGameStatusDisplayed.ACTIVE ) {
			// set opacity: we "fade in" when game over occurs
			int alpha ;
			if ( mLocalMillisecondsSpentGameOver >= mDismissMilliseconds  ) {
				alpha = 255 ;
			} else {
				alpha = Math.round( 255 *
						( 1.0f - ( ((float)(mDismissMilliseconds - mLocalMillisecondsSpentGameOver))
								/ mDismissMilliseconds ) ) ) ;
			}
			int alphaC = Color.argb(alpha, 0, 0, 0) ;
			for ( int j = 0; j < mPaintGameOverTitleText.length; j++ ) {
				mPaintGameOverTitleText[j].setAlpha(alpha) ;
				mPaintGameOverTitleText[j].setShadowLayer(
						mShadowRadius[j],
						mShadowX[j],
						mShadowY[j],
						alphaC | ( mShadowColor[j] & 0x00ffffff ) ) ;
			}
			for ( int j = 0; j < mPaintGameOverSubtitleText.length; j++ ) {
				mPaintGameOverSubtitleText[j].setAlpha(alpha) ;
				mPaintGameOverSubtitleText[j].setShadowLayer(
						mShadowRadius[j],
						mShadowX[j],
						mShadowY[j],
						alphaC | ( mShadowColor[j] & 0x00ffffff ) ) ;
			}
			
			
			// draw the title and subtitle.
			int height = this.mGameOverTextTitleBounds.height() + this.mGameOverTextSubtitleBounds.height() ;
			x = this.mGameOverRegion.centerX() ;
			y = this.mGameOverRegion.centerY() - height/2 ;
			// y is the top of where we draw; step down to position the title.
			y -= mGameOverTextTitleBounds.top ;
			for ( int j = mPaintGameOverTitleText.length-1; j >= 0; j-- ) {
				canvas.drawText(
						mGameOverTextTitle,
						x, y, mPaintGameOverTitleText[j]) ;
			}
			y += mGameOverTextTitleBounds.bottom ;
			y -= mGameOverTextSubtitleBounds.top ;
			for ( int j = mPaintGameOverSubtitleText.length-1; j >= 0; j-- ) {
				canvas.drawText(
						mGameOverTextSubtitle,
						x, y, mPaintGameOverSubtitleText[j]) ;
			}
			
			mLocalMillisecondsSpentGameOver += millisSinceLastDraw ;
		}
	}
	
	
	/**
	 * Attempts to fit our player numbers within the provided game region,
	 * by first 1. setting the text bounds to fit the text at its current size,
	 * 2. shrinking it -- if necessary -- to fit within the game region, and
	 * 3. shrinking the text to fit within the new boundary.
	 * @return
	 */
	private void formatToFit() {
		measureText( mTextBounds ) ;
		
		// fit bounds to contain this text...
		
		if ( mTextBounds.width() <= mGameRegion.width() && mTextBounds.height() <= mGameRegion.height() ) {
			// fits within the game region; we're done, and it
			// took 0 text resizes.
			setTextNumberDrawRegion() ;
		} else {
			int timesShrunk = 0 ;
			while( timesShrunk < mTextSize ) {
				timesShrunk++ ;
				for ( int j = 0; j < mPaintText.length; j++ ) {
					mPaintText[j].setTextSize(mTextSize - timesShrunk) ;
				}
				measureText( mTextBounds ) ;
				
				// if it fits, that's the end.
				if ( mTextBounds.width() <= mGameRegion.width() && mTextBounds.height() <= mGameRegion.height() ) {
					setTextNumberDrawRegion() ;
					break ;
				}
			}
		}
		
		if ( mGameOverTextTitle != null ) {
			// Game Over text is different.  We start from mTextSize and shrink (or grow)
			// until we take between 80 and 100 % of the score region width.
			for ( int j = 0; j < mPaintGameOverTitleText.length; j++ ) {
				mPaintGameOverTitleText[j].setTextSize(mTextSize) ;
			}
			// grow
			int grownBy = 0 ;
			measureText( mGameOverTextTitleBounds, mPaintGameOverTitleText, mGameOverTextTitle ) ;
			while ( mGameOverTextTitleBounds.width() < 0.8 * mGameOverRegion.width() ) {
				grownBy++ ;
				for ( int j = 0; j < mPaintGameOverTitleText.length; j++ ) {
					mPaintGameOverTitleText[j].setTextSize(mTextSize + grownBy) ;
				}
				measureText( mGameOverTextTitleBounds, mPaintGameOverTitleText, mGameOverTextTitle ) ;
			}
			// shrink
			while ( mGameOverTextTitleBounds.width() > mGameOverRegion.width() ) {
				grownBy-- ;
				for ( int j = 0; j < mPaintGameOverTitleText.length; j++ ) {
					mPaintGameOverTitleText[j].setTextSize(mTextSize + grownBy) ;
				}
				measureText( mGameOverTextTitleBounds, mPaintGameOverTitleText, mGameOverTextTitle ) ;
			}
			
			// now grow the subtitle until it exceeds (or matches) the width of the title.
			for ( int j = 0; j < mPaintGameOverSubtitleText.length; j++ ) {
				mPaintGameOverSubtitleText[j].setTextSize(mTextSize) ;
			}
			// grow
			grownBy = 0 ;
			measureText( mGameOverTextSubtitleBounds, mPaintGameOverSubtitleText, mGameOverTextSubtitle ) ;
			while ( mGameOverTextSubtitleBounds.width() < mGameOverTextTitleBounds.width() ) {
				grownBy++ ;
				for ( int j = 0; j < mPaintGameOverSubtitleText.length; j++ ) {
					mPaintGameOverSubtitleText[j].setTextSize(mTextSize + grownBy) ;
				}
				measureText( mGameOverTextSubtitleBounds, mPaintGameOverSubtitleText, mGameOverTextSubtitle ) ;
			}
			// shrink as long as larger
			while ( mGameOverTextSubtitleBounds.width() > mGameOverTextTitleBounds.width() ) {
				grownBy-- ;
				for ( int j = 0; j < mPaintGameOverSubtitleText.length; j++ ) {
					mPaintGameOverSubtitleText[j].setTextSize(mTextSize + grownBy) ;
				}
				measureText( mGameOverTextSubtitleBounds, mPaintGameOverSubtitleText, mGameOverTextSubtitle ) ;
			}
		}
	}
	
	private void setTextNumberDrawRegion() {
		measureTextNumber( mTextBounds ) ;
		mTextNumberDrawRegion.right = mGameRegion.right ;
		mTextNumberDrawRegion.bottom = mGameRegion.bottom ;
		mTextNumberDrawRegion.left = mTextNumberDrawRegion.right - mTextBounds.width() ;
		mTextNumberDrawRegion.top = mTextNumberDrawRegion.bottom - mTextBounds.height() ;
	}
	
	
	Rect mMeasureTextNameRect = new Rect() ;
	Rect mMeasureTextNumberRect = new Rect() ;
	private void measureText( Rect bounds ) {
		bounds.set(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE) ;
		for ( int i = 0; i < mPlayerNumberText.length; i++ ) {
			for ( int j = 0; j < mPaintText.length; j++ ) {
				if ( mPlayerNameText[i] != null ) {
					mPaintText[j].getTextBounds(
							mPlayerNameText[i],
							0, mPlayerNameText[i].length(),
							mMeasureTextNameRect) ;
				} else {
					// no space needed
					mMeasureTextNameRect.set(0,0,0,0) ;
				}
				mPaintText[j].getTextBounds(
						mPlayerNumberText[i],
						0, mPlayerNumberText[i].length(),
						mMeasureTextNumberRect) ;
				// formatting: keep 'right' where it is for the measure number
				// rect.  Extend top / bottom to their respective maxes in both
				// rects.  Left is number left minus text width.
				if ( mMeasureTextNameRect.width() == 0 ) {
					bounds.left = Math.min( bounds.left, mMeasureTextNumberRect.left ) ;
					bounds.top = Math.min( bounds.top, mMeasureTextNumberRect.top ) ;
					bounds.right = Math.max( bounds.right, mMeasureTextNumberRect.right ) ;
					bounds.bottom = Math.max( bounds.bottom, mMeasureTextNumberRect.bottom ) ;
				} else {
					bounds.left = Math.min( bounds.left, mMeasureTextNumberRect.left - mMeasureTextNameRect.width() - mMarginHorizontal ) ;
					bounds.right = Math.max( bounds.right, mMeasureTextNameRect.right ) ;
					
					if ( mMeasureTextNameRect.height() > mMeasureTextNumberRect.height() ) {
						bounds.top = Math.min( bounds.top, mMeasureTextNameRect.top ) ;
						bounds.bottom = Math.max( bounds.bottom, mMeasureTextNameRect.bottom ) ;
					} else {
						bounds.top = Math.min( bounds.top, mMeasureTextNumberRect.top ) ;
						bounds.bottom = Math.max( bounds.bottom, mMeasureTextNumberRect.bottom ) ;
					}
				}
			}
		}
		
		// now extend top and left to fit statuses.  Assume they appear above,
		// aligned to the right, with vertical margin between.
		mMeasureTextNumberRect.set(bounds) ;
		for ( int i = 0; i < this.mStatusDisplayedText.length; i++ ) {
			for ( int j = 0; j < mPaintText.length; j++ ) {
				mPaintText[j].getTextBounds(
						mStatusDisplayedText[i],
						0, mStatusDisplayedText[i].length(),
						mMeasureTextNameRect) ;
				
				// extend left
				if ( bounds.width() < mMeasureTextNameRect.width() ) {
					bounds.left -= mMeasureTextNameRect.width() - bounds.width() ;
				}
				// extend top
				bounds.top = Math.min( bounds.top, mMeasureTextNumberRect.top - mMeasureTextNameRect.height() - mMarginVertical) ;
			}
		}
		
		// let it out by margin size...
		bounds.inset(-mMarginHorizontal, -mMarginVertical) ;
	}
	
	private void measureTextNumber( Rect bounds ) {
		measureText( bounds, mPaintText, mPlayerNumberText ) ;
	}
	
	private void measureText( Rect bounds, Paint [] paints, String ... text ) {
		bounds.set(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE) ;
		for ( int i = 0; i < text.length; i++ ) {
			for ( int j = 0; j < paints.length; j++ ) {
				paints[j].getTextBounds(
						text[i],
						0, text[i].length(),
						mMeasureTextNumberRect) ;
				// formatting: keep 'right' where it is for the measure number
				// rect.  Extend top / bottom to their respective maxes in both
				// rects.  Left is number left minus text width.
				bounds.left = Math.min( bounds.left, mMeasureTextNumberRect.left ) ;
				bounds.top = Math.min( bounds.top, mMeasureTextNumberRect.top ) ;
				bounds.right = Math.max( bounds.right, mMeasureTextNumberRect.right ) ;
				bounds.bottom = Math.max( bounds.bottom, mMeasureTextNumberRect.bottom ) ;
			}
		}
		
		// let it out by margin size...
		bounds.inset(-mMarginHorizontal, -mMarginVertical) ;
	}
	
	private void createPaints() {
		mTextSize = mResources.getDimensionPixelSize(R.dimen.game_interface_player_number_text_size) ;
		
		mMarginHorizontal = mResources.getDimensionPixelSize(R.dimen.game_interface_player_number_horizontal_margin) ;
		mMarginVertical = mResources.getDimensionPixelSize(R.dimen.game_interface_player_number_vertical_margin) ;

		mTextColor = mResources.getColor(R.color.game_interface_player_number_text_color) ;
		mTextColorInactive = mResources.getColor(R.color.game_interface_player_number_text_inactive_color) ;
		int shadowColor = mResources.getColor(R.color.game_interface_player_number_text_shadow_color) ;
		
		float shadowRadius = mResources.getDimensionPixelOffset(R.dimen.game_interface_player_number_shadow_radius) ;
		float shadowOffsetX = mResources.getDimensionPixelOffset(R.dimen.game_interface_player_number_shadow_offset_x) ;
		float shadowOffsetY = mResources.getDimensionPixelOffset(R.dimen.game_interface_player_number_shadow_offset_y) ;
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
		
		mPaintText = new Paint[2] ;
		
		for ( int j = 0; j < 2; j++ ) {
			mPaintText[j] = new Paint() ;
			mPaintText[j].setAntiAlias(true) ;
			mPaintText[j].setTextSize(mTextSize) ;
			mPaintText[j].setTextAlign(Paint.Align.RIGHT) ;
			mPaintText[j].setColor(mTextColor) ;
			mPaintText[j].setShadowLayer(mShadowRadius[j], mShadowX[j], mShadowY[j], mShadowColor[j]) ;
		}
		
		// make "game over" title / subtitle paints.  They are centered.
		mPaintGameOverTitleText = new Paint[2] ;
		mPaintGameOverSubtitleText = new Paint[2] ;
		Paint [][] paints = new Paint[][]{ mPaintGameOverTitleText, mPaintGameOverSubtitleText } ;
		for ( int i = 0; i < 2; i++ ) {
			for ( int j = 0; j < 2; j++ ) {
				paints[i][j] = new Paint() ;
				paints[i][j].setAntiAlias(true) ;
				paints[i][j].setTextSize(mTextSize) ;
				paints[i][j].setTextAlign(Paint.Align.CENTER) ;
				paints[i][j].setColor(mTextColor) ;
				paints[i][j].setShadowLayer(mShadowRadius[j], mShadowX[j], mShadowY[j], mShadowColor[j]) ;
			}
		}
	}
	
	private void createStatusDisplayedText( Resources res ) {
		mStatusDisplayedText[PlayerGameStatusDisplayed.ACTIVE.ordinal()] = res.getString(R.string.game_interface_name_drawer_status_active) ;
		mStatusDisplayedText[PlayerGameStatusDisplayed.WON.ordinal()] = res.getString(R.string.game_interface_name_drawer_status_won) ;
		mStatusDisplayedText[PlayerGameStatusDisplayed.LOST.ordinal()] = res.getString(R.string.game_interface_name_drawer_status_lost) ;
		mStatusDisplayedText[PlayerGameStatusDisplayed.KICKED.ordinal()] = res.getString(R.string.game_interface_name_drawer_status_kicked) ;
		mStatusDisplayedText[PlayerGameStatusDisplayed.QUIT.ordinal()] = res.getString(R.string.game_interface_name_drawer_status_quit) ;
		mStatusDisplayedText[PlayerGameStatusDisplayed.INACTIVE.ordinal()] = res.getString(R.string.game_interface_name_drawer_status_inactive) ;
	}
}
