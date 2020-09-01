package com.peaceray.quantro.view.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.R;
import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.exceptions.GameSystemException;
import com.peaceray.quantro.exceptions.QOrientationConflictException;
import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameBlocksSlice;
import com.peaceray.quantro.model.game.GameEvents;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.model.pieces.Offset;
import com.peaceray.quantro.model.pieces.PieceCatalog;
import com.peaceray.quantro.q.QCombinations;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.ThreadSafety;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.controls.InvisibleControls;
import com.peaceray.quantro.view.drawable.RectsDrawable;
import com.peaceray.quantro.view.game.blocks.BlockDrawer;
import com.peaceray.quantro.view.game.blocks.BlockDrawerAsynchronousPrerenderer;
import com.peaceray.quantro.view.game.blocks.BlockDrawerPreallocatedBitmaps;
import com.peaceray.quantro.view.game.blocks.BlockDrawerSliceTime;
import com.peaceray.quantro.view.game.blocks.config.BlockDrawerConfigCanvas;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
	
	public static final float GAME_FIELD_MAX_WIDTH = 0.8f ;
	
	private boolean TEST_VEIL_veiled = false;
	private long TEST_VEIL_nextVeilOn;
	private long TEST_VEIL_nextVeilOff;
	
	private long TEST_SHUFFLE_BG_nextShufflePreload = 3000 ;
	private long TEST_SHUFFLE_BG_nextShuffleGo = Long.MAX_VALUE ;
	private Background TEST_SHUFFLE_BG_background ;

	private static final String TAG = "GameView";

	class GameBlockFieldViewThread extends Thread implements BlockDrawerAsynchronousPrerenderer.Listener {
		
		private Random r = new Random() ;

		// Structures given at construction
		private SurfaceHolder surfaceHolder;
		private Handler handler;

		// Here's a block drawer
		private BlockDrawer blockDrawer;
		private BlockDrawerPreallocatedBitmaps blockDrawerPreallocated ;
		private Bitmap blockDrawerLoadIntoSheetBitmap ;
		private Bitmap blockDrawerLoadIntoBackgroundBitmap ;
		// ...and one mini block-drawer!
		private BlockDrawer blockDrawerMini;
		
		// Here's how we preload.  Hopefully we can keep the app more responsive
		// during loads?
		BlockDrawerAsynchronousPrerenderer mPrerenderer = null ;
		boolean mPrerenderCycleWorking = false ;
		boolean mPrerenderCycleFinished = false ;
		Object mPrerendererMutex = new Object() ;

		// Current state

		// State of preparation
		private boolean canvasReady;
		private boolean gamesReady;
		private boolean controlsReady;
		private boolean canvasBigEnough;
		private boolean running;
		private boolean started ;
		private Object startMutex ;
		private boolean paused;
		private boolean loaded;

		private boolean newDrawSettings;
		private boolean newBackground;

		private boolean forceUpdate;
		private boolean firstUpdate;
		private boolean firstPausedUpdate;
		private boolean firstPausedIteration ;

		// Links to game and related objects
		int gameMode;
		int numPlayers;
		String [] playerNames ;
		PlayerGameStatusDisplayed [] playerStatusDisplayed ;
		
		private boolean[] tempGameWonLost;
		private Game[] game;
		private GameInformation[] ginfo;
		private GameEvents[] gevents;
		private boolean[] gameOver;
		int displayedGame;
		int thumbnailGame;
		int defaultDisplayedGame;
		int localPlayerGame = -1 ;
		public static final int DEFAULT_GAME = -1;

		// Drawing settings; drawing objects.
		DrawSettings blockFieldDrawSettings;
		DrawSettings blockFieldMiniDrawSettings;
		ScoreDrawer[] scoreDrawer; // standard side-panel score
		ScoreDrawer[] scoreDrawerDetailed;
		TooltipDrawer tooltipDrawer;
		PlayerStatusDrawer playerStatusDrawer ;

		// It's important to have access to the controls, since
		// we draw things behind them.
		private InvisibleControls view_invisibleControls;
		// Also need a sound pool for to play sounds on.
		private QuantroSoundPool soundPool;
		private GameEvents[] soundEvents;
		private GameEvents[] achievementEvents;
		private Skin skin = null;
		private ColorScheme colorScheme = null ;
		private int drawDetail = DrawSettings.DRAW_DETAIL_MID;
		private Background background = null;
		private ArrayList<Background> backgroundShuffle = null ;
		private boolean piecePreviewMinimumProfile = true ;
		private int loadImagesSize = DrawSettings.IMAGES_SIZE_SMALL;
		private int loadBackgroundSize = DrawSettings.IMAGES_SIZE_SMALL;
		private boolean soundControls = true;
		private int blit = DrawSettings.BLIT_FULL;
		private int scale = 1;
		private boolean recycleToVeil = false;
		private int animations = DrawSettings.DRAW_ANIMATIONS_ALL;
		private int pieceTips = PIECE_TIPS_OCCASIONALLY;
		private boolean displayAlternatePanelForOpponent ;
		private boolean displayNextPreviewOnLeft ;
		private boolean displayOpponentThumbnailOnLeft ;
		
		// Shuffle parameters.
		private boolean shuffleParam_shuffleBackgrounds = false ;
		private long shuffleParam_shuffleBackgroundMinGap = 0 ;
		private long shuffleParam_shuffleBackgroundMaxGap = 0 ;
		private boolean shuffleParam_shuffleOnLevelUp = false ;
		// Shuffle data.
		private Background shuffle_nextBackground = null ;
		private long shuffle_scheduled = -1 ;

		// Bounds for left, right panels and the main game area.
		private Rect rect_gameArea;
		private Rect rect_panelLeft;
		private Rect rect_panelRight;
		private Rect rect_panelScore;
		private Rect rect_panelScoreDrawableRegion;
		private Rect rect_tooltips;
		private Rect rect_panelOpponentPressedNeg ;
		// Convert to 565 Bitmaps for time-efficiency. Space efficiency? Who
		// needs it?
		private ArrayList<Bitmap> bitmap_panelFrame ;
		private ArrayList<Offset> bitmap_panelFramePosition ;
		private ArrayList<Bitmap> bitmap_panelLeft;
		private ArrayList<Offset> bitmap_panelLeftPosition;
		private ArrayList<Bitmap> bitmap_panelRight;
		private ArrayList<Offset> bitmap_panelRightPosition;
		private ArrayList<Bitmap> bitmap_panelScore;
		private ArrayList<Offset> bitmap_panelScorePosition;
		private ArrayList<Bitmap> bitmap_over;
		private ArrayList<Offset> bitmap_overPosition;
		private ArrayList<Integer> bitmap_overFlag ;
		private ArrayList<Bitmap> bitmap_panelOpponentPressed ;
		private ArrayList<Offset> bitmap_panelOpponentPressedPosition ;
		// to draw the left-right panels, fill rect_panelLeft, rect_panelRight
		// with the background color, then iterate through the corresponding
		// Drawable arrays. To then add the score panel, fill it with background
		// and iterate through the drawable_panelScore array.
		private ArrayList<Integer> text_panelOpponentPressedNumbersIndex ;
		private ArrayList<String> text_panelOpponentPressedNumbers ;
		private ArrayList<Offset> text_panelOpponentPressedNumbersPosition ;
		private Paint text_panelOpponentPressedNumbersActivePaint ;
		private Paint text_panelOpponentPressedNumbersInactivePaint ;

		private Path tempPath = new Path();
		
		private static final int BITMAP_FLAGS_ALWAYS = 0x0 ;		// if == 0, we always draw
		private static final int BITMAP_FLAG_NEGATE = 0x01 ;		// if lowest bit is 0, we negate the rest, and draw when they are NOT the case.
		private static final int BITMAP_FLAG_CAN_USE_RESERVE = 0x02 ;	// we draw if the reserve is available to us.
		private static final int BITMAP_FLAG_SHOWING_DEFAULT_GAME = 0x04 ;	// we draw when the default game is displayed (not any other)
		
		// Bounding rectangles for user controls. Store them as an array,
		// with indices.
		private String[] name_button;
		private Rect[] rect_button ;
		private Rect[] rect_touchable_button;
		private Drawable[] icon_button;
		private Drawable[] icon_button_alt ;
		private final static int INDEX_BUTTON_L = 0;
		private final static int INDEX_BUTTON_R = 1;
		private final static int INDEX_BUTTON_D = 2;
		private final static int INDEX_BUTTON_CW = 3;
		private final static int INDEX_BUTTON_CCW = 4;
		private final static int INDEX_BUTTON_FLIP = 5;
		private final static int INDEX_BUTTON_SCORE = 6;
		private final static int INDEX_BUTTON_RESERVE = 7;
		private final static int INDEX_BUTTON_OPPONENT = 8;
		private final static int NUM_INDEX_BUTTONS = 9;
		
		// Bounding rects for "player select slide" regions.
		private Rect[] rect_opponentSelectionRegion ;

		// Subrects for drawing content - put all content within these
		// rectangles.
		private Rect[] rect_drawRegion;
		private final static int INDEX_DRAW_REGION_SCORE = 0;
		private final static int INDEX_DRAW_REGION_NEXT = 1;
		private final static int INDEX_DRAW_REGION_RESERVE = 2;
		private final static int INDEX_DRAW_REGION_OPPONENT = 3;

		private int[] mColor;
		private final static int INDEX_PANEL_BACKGROUND = 0;
		private final static int INDEX_PANEL_OUTER_EDGE_TOP = 1;
		private final static int INDEX_PANEL_OUTER_EDGE_BOTTOM = 2;
		private final static int INDEX_PANEL_INNER_EDGE_TOP = 3;
		private final static int INDEX_PANEL_INNER_EDGE_BOTTOM = 4;
		private static final int INDEX_MINI_BUTTON_DIAGRAM = 5;
		private static final int INDEX_MINI_BUTTON_ICON = 6;
		private final static int NUM_COLOR_INDICES = 7;

		private int[] mSize;
		private final static int SIZE_INDEX_OUTER_EDGE_WIDTH = 0;
		private final static int SIZE_INDEX_INNER_EDGE_WIDTH = 1;
		private final static int SIZE_INDEX_EDGE_SHADOW_WIDTH = 2;
		private final static int SIZE_INDEX_TOTAL_EDGE_WIDTH = 3;
		private final static int NUM_SIZE_INDICES = 4;

		// Game meta-info
		private int ROWS, COLS; // blockField size
		private int displayedRows;
		private boolean displaces ;		// the game may be gradually displaced by new rows

		// Information about the canvas
		private int canvasWidth = 1;
		private int canvasHeight = 1;

		// Cacheing objects for game information
		// NOTE: although it may not be, strictly speaking,
		// a necessary use of space, we keep these values
		// for EACH GAME OBJECT, regardless of which is being
		// displayed at any one time.
		private GameBlocksSlice[] slices;
		private boolean newThumbnailGameBlocksSlice;
		private boolean [] newSlice ;
		private int [] lastTickPieceType ;

		private int bf_outer_buffer = 1;
		public int[] gameState;
		private boolean[] animatingSignificantEvent;
		private boolean[] didAnimateSignificantEvent; // the last draw included
														// animation
		private BlockDrawerSliceTime[] mSliceTime ;

		
		private int veiledFramesSkipped = 0 ;
		private long currentTime;
		private static final int GAME_STATE_IS_UNKNOWN = -1;
		private static final int GAME_STATE_IS_STABLE = 0;
		private static final int GAME_STATE_IS_PIECE_FALLING = 1;
		private static final int GAME_STATE_IS_COMPONENTS_FALLING = 2;
		private static final int GAME_STATE_IS_CHUNKS_FALLING = 3;
		private static final int GAME_STATE_IS_CLEARING = 4;
		private static final int GAME_STATE_IS_ADDING_ROWS = 5;
		private static final int GAME_STATE_IS_METAMORPHOSIZING = 6;

		// Next piece, and reserve piece.
		// CONVENTION: these are 4x4.
		private int bf_piece_outer_buffer = 2;
		private GameBlocksSlice[] nextPieceSlices;
		private GameBlocksSlice[] reservePieceSlices;
		private boolean[] canUseReserve;
		private DrawSettings nextPieceDrawSettings;
		private DrawSettings reservePieceDrawSettings;
		private BlockDrawer nextPieceBlockDrawer;
		private BlockDrawer reservePieceBlockDrawer;
		private BlockDrawerPreallocatedBitmaps nextPieceBlockDrawerPreallocated = null ;
		private BlockDrawerPreallocatedBitmaps reservePieceBlockDrawerPreallocated = null ;
		private boolean changedNextPieceSlice;
		private boolean changedReservePieceSlice;
		private BlockDrawerSliceTime nextPieceSliceTime ;
		private BlockDrawerSliceTime reservePieceSliceTime ;

		public GameBlockFieldViewThread(SurfaceHolder surfaceHolder,
				Handler handler) {
			// Log.d(TAG, "thread constructor") ;
			// get handles to some important objects
			this.surfaceHolder = surfaceHolder;
			this.handler = handler;

			blockDrawer = null;
			blockDrawerMini = null;
			// no settings for this object
			
			mPrerenderer = new BlockDrawerAsynchronousPrerenderer() ;

			canvasReady = false;
			gamesReady = false;
			controlsReady = false;
			canvasBigEnough = false;
			running = false;
			started = false;
			startMutex = new Object();
			paused = true;
			loaded = false;

			firstPausedUpdate = true;
			firstPausedIteration = true ;

			newDrawSettings = true; // upon run, we reconnect block drawers and
									// draw settings

			this.displayedGame = 0;
			this.defaultDisplayedGame = 0;
			this.localPlayerGame = -1 ;

			// null some things
			gameMode = -1;
			game = null;
			ginfo = null;
			gevents = null;
			soundEvents = null;
			achievementEvents = null;
			rect_button = null;
			rect_touchable_button = null;
			rect_drawRegion = null;

			slices = null; // This value is checked for 'null' late
			lastTickPieceType = null ;

			Resources res = getContext().getResources();
			loadPaints(res);
			loadBitmaps(res);
			setSizes();
		}

		private void loadPaints(Resources res) {

			mColor = new int[NUM_COLOR_INDICES];
			mColor[INDEX_PANEL_BACKGROUND] = res
					.getColor(R.color.game_interface_side_panel_background);
			mColor[INDEX_PANEL_OUTER_EDGE_TOP] = res
					.getColor(R.color.game_interface_side_panel_outer_edge_top);
			mColor[INDEX_PANEL_OUTER_EDGE_BOTTOM] = res
					.getColor(R.color.game_interface_side_panel_outer_edge_bottom);
			mColor[INDEX_PANEL_INNER_EDGE_TOP] = res
					.getColor(R.color.game_interface_side_panel_inner_edge_top);
			mColor[INDEX_PANEL_INNER_EDGE_BOTTOM] = res
					.getColor(R.color.game_interface_side_panel_inner_edge_bottom);
			mColor[INDEX_MINI_BUTTON_DIAGRAM] = res
					.getColor(R.color.game_interface_button_diagram);
			mColor[INDEX_MINI_BUTTON_ICON] = res
					.getColor(R.color.game_interface_button_icon);

			// sanity check: convert colors to RGB 565 equiv.
			for (int i = 0; i < mColor.length; i++) {
				Bitmap b = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
				Canvas c = new Canvas(b);
				c.drawColor(mColor[i]);
				mColor[i] = b.getPixel(0, 0);
				b.recycle();
			}
		}

		private void loadBitmaps(Resources res) {
			icon_button = new Drawable[NUM_INDEX_BUTTONS];
			icon_button_alt = new Drawable[NUM_INDEX_BUTTONS] ;

			// left, right, cw, ccw, reserve
			icon_button[INDEX_BUTTON_L] = res
					.getDrawable(R.drawable.arrow_left_alt4);
			icon_button[INDEX_BUTTON_R] = res
					.getDrawable(R.drawable.arrow_right_alt4);
			icon_button[INDEX_BUTTON_CCW] = res
					.getDrawable(R.drawable.curved_arrow_ccw);
			icon_button[INDEX_BUTTON_CW] = res
					.getDrawable(R.drawable.curved_arrow);
			icon_button[INDEX_BUTTON_FLIP] = res.getDrawable(R.drawable.arrow_up_down) ;
			icon_button[INDEX_BUTTON_D] = res.getDrawable(R.drawable.download) ;
			icon_button[INDEX_BUTTON_RESERVE] = res
					.getDrawable(R.drawable.loop_alt4);
			
			icon_button_alt[INDEX_BUTTON_RESERVE] = res
					.getDrawable(R.drawable.bolt) ;
		}

		private void releaseBitmaps() {
			if (icon_button == null) {
				for (int i = 0; i < icon_button.length; i++)
					icon_button[i] = null;
			}
		}

		private void setSizes() {
			mSize = new int[NUM_SIZE_INDICES];
			mSize[SIZE_INDEX_OUTER_EDGE_WIDTH] = 2;
			mSize[SIZE_INDEX_INNER_EDGE_WIDTH] = 1;
			mSize[SIZE_INDEX_EDGE_SHADOW_WIDTH] = 7;
			mSize[SIZE_INDEX_TOTAL_EDGE_WIDTH] = 3;
		}

		public void setCanvasOK(boolean running) {
			synchronized( surfaceHolder ) {
				Log.d(TAG, "setCanvasOK");
				canvasReady = running;
				Log.d(TAG, "tryToStart from setCanvasOK");
				tryToStart();
			}
		}

		public void forceUpdate() {
			// Log.d(TAG, "forceUpdate") ;
			synchronized (surfaceHolder) {
				forceUpdate = true;
			}
		}

		public void pause() {
			// Log.d(TAG, "pause") ;
			synchronized (surfaceHolder) {
				paused = true;
				firstPausedUpdate = true;
				firstPausedIteration = true ;
			}
		}

		public void unpause() {
			// Log.d(TAG, "unpause") ;
			synchronized (surfaceHolder) {
				paused = false;
				firstPausedUpdate = false;
				firstPausedIteration = false ;
			}
		}

		public void setNumberOfPlayers(int numPlayers) {
			// Log.d(TAG, "setNumberOfPlayers") ;
			this.numPlayers = numPlayers;
			this.playerNames = new String[numPlayers] ;
			this.playerStatusDisplayed = new PlayerGameStatusDisplayed[numPlayers] ;
			this.blockDrawer = null;
			this.blockDrawerMini = null;
			this.tempGameWonLost = new boolean[numPlayers];
			this.gameOver = new boolean[numPlayers];
			this.game = new Game[numPlayers];
			this.ginfo = new GameInformation[numPlayers];
			this.gevents = new GameEvents[numPlayers];
			this.blockFieldDrawSettings = null;
			blockFieldMiniDrawSettings = null;
			this.scoreDrawer = new AbbreviatedScoreDrawer[numPlayers];
			this.scoreDrawerDetailed = new DetailedScoreDrawer[numPlayers];

			this.soundEvents = new GameEvents[numPlayers];
			this.achievementEvents = new GameEvents[numPlayers] ;

			this.nextPieceDrawSettings = null;
			this.reservePieceDrawSettings = null;

			this.nextPieceBlockDrawer = null;
			this.reservePieceBlockDrawer = null;

			this.gameState = new int[numPlayers];
			this.animatingSignificantEvent = new boolean[numPlayers];
			this.didAnimateSignificantEvent = new boolean[numPlayers];
			this.mSliceTime = new BlockDrawerSliceTime[numPlayers] ;
			for (int i = 0; i < numPlayers; i++) {
				this.gameState[i] = GAME_STATE_IS_UNKNOWN;
				this.animatingSignificantEvent[i] = false;
				this.didAnimateSignificantEvent[i] = false;
				
				this.mSliceTime[i] = new BlockDrawerSliceTime() ;
			}

			this.slices = new GameBlocksSlice[numPlayers];
			this.newSlice = new boolean[numPlayers] ;
			this.lastTickPieceType = new int[numPlayers] ;
			this.nextPieceSlices = new GameBlocksSlice[numPlayers];
			this.reservePieceSlices = new GameBlocksSlice[numPlayers];
			this.nextPieceSliceTime = new BlockDrawerSliceTime() ;
			this.reservePieceSliceTime = new BlockDrawerSliceTime() ;
			this.canUseReserve = new boolean[numPlayers];
			
			this.displayAlternatePanelForOpponent = numPlayers > 2 ;
		}

		public void setGame(int playerNum, Game game, GameInformation ginfo,
				GameEvents gevents) {
			synchronized (surfaceHolder) {
				// Log.d(TAG, "setGame") ;
				gameMode = ginfo.mode;

				this.game[playerNum] = game;
				this.ginfo[playerNum] = ginfo;
				this.gevents[playerNum] = gevents;

				this.soundEvents[playerNum] = new GameEvents()
						.finalizeConfiguration();
				this.achievementEvents[playerNum] = new GameEvents().finalizeConfiguration() ;

				// Resize our blockfields. We do this ONLY ONCE.
				// TODO: If different Game objects in the same multiplayer
				// game can have different Row/Col dimensions, we may need
				// to rethink this.

				if (slices[playerNum] == null) {
					ROWS = game.R();
					COLS = game.C();
					displayedRows = ROWS / 2 + 1; // Display 1/2 the game rows +
													// 1; the bottom-edge of
													// pieces enter at this
													// level.
					displaces = game.getDisplaces() ;
					slices[playerNum] = new GameBlocksSlice(ROWS, COLS, ROWS,
							bf_outer_buffer);
					nextPieceSlices[playerNum] = new GameBlocksSlice(4, 4, 1,
							bf_piece_outer_buffer);
					reservePieceSlices[playerNum] = new GameBlocksSlice(4, 4, 1,
							bf_piece_outer_buffer);
					canUseReserve[playerNum] = true ;
					
					lastTickPieceType[playerNum] = -1 ;
				}

				// Significant game events!
				setGameEventsSignificance(gevents) ;

				// Specialty game elements, draw elements, etc.
				blockDrawer = BlockDrawer.newPreAllocatedBlockDrawer(getContext(),
						null, null);
				AnimationSettings as = new AnimationSettings() ;
				if ( ginfo.difficulty == GameInformation.DIFFICULTY_HARD ) {
					// hard mode, for Endurance at least, is about 3 times as
					// fast as normal mode.
					// (level 2: ~1 row/second vs. ~3 rows/second
					//  level 20: ~10 rows/second vs. ~30 rows/second)
					// Accelerate movement animations to match this increase.
					as.accelerateMovementAnimations(3.0) ;
				}
				blockDrawer.setAnimationSettings(as);

				gamesReady = true;
				for (int i = 0; i < this.game.length; i++)
					gamesReady = gamesReady && this.game[i] != null;

				tryToStart();
			}
		}
		
		
		private void setPlayerNames( String [] names ) {
			synchronized( surfaceHolder ) {
				boolean changed = false ;
				for ( int i = 0; i < playerNames.length; i++ ) {
					if ( (playerNames[i] == null) != (names[i] == null)
							|| (playerNames[i] != null && !playerNames[i].equals(names[i])) ) {
						changed = true ;
						playerNames[i] = names[i] ;
					}
				}
				if ( changed && this.playerStatusDrawer != null ) {
					playerStatusDrawer.setPlayerNames(playerNames) ;
				}
			}
		}
		
		private void setPlayerStatuses( PlayerGameStatusDisplayed [] statuses ) {
			synchronized( surfaceHolder ) {
				boolean changed = false ;
				for ( int i = 0; i < playerStatusDisplayed.length; i++ ) {
					if ( (playerStatusDisplayed[i] == null) != (statuses[i] == null)
							|| (playerStatusDisplayed[i] != null && !playerStatusDisplayed[i].equals(statuses[i])) ) {
						changed = true ;
						playerStatusDisplayed[i] = statuses[i] ;
					}
				}
				if ( changed && this.playerStatusDrawer != null ) {
					playerStatusDrawer.setPlayerStatus(playerStatusDisplayed) ;
				}
			}
		}
		
		private void setGameEventsSignificance( GameEvents gevents ) {
			if ( gevents != null ) {
				// Our two animation methods for the game are ALL and STABLE_STUTTER.
				// They behave slightly differently from our perspective.  ALL needs
				// to know when anything that "changes the game state" (other than
				// as sequence of moves) occurs.  STUTTER only needs to know about
				// things that "have a different starting point from last drawn."
				// The main difference here is that we don't bother drawing falling
				// components.
				gevents.clearSignificance() ;
				gevents.setSignificance(GameEvents.EVENT_PIECE_ENTERED, true);
				gevents.setSignificance(GameEvents.EVENT_CHUNKS_FELL, true);
				gevents.setSignificance(GameEvents.EVENT_CLEAR, true);
				gevents.setSignificance(GameEvents.EVENT_ATTACK_PUSH_ROWS, true);
				gevents.setSignificance(GameEvents.EVENT_GAME_LEVEL_UP, true);
				if ( animations == DrawSettings.DRAW_ANIMATIONS_ALL ) {
					// animate falling components and "lock then metamorphosis."
					// also animate displacement transfer.
					gevents.setSignificance(GameEvents.EVENT_COMPONENTS_FELL, true);
					gevents.setSignificance(
							GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS, true);
					gevents.setSignificance(GameEvents.EVENT_DISPLACEMENT_TRANSFERRED, true) ;
					gevents.setSignificance(GameEvents.EVENT_METAMORPHOSIS, true);
				}
			}
		}

		public void setControls(InvisibleControls controls) {
			synchronized (surfaceHolder) {
				// Log.d(TAG, "setControls") ;
				this.view_invisibleControls = controls;
				controlsReady = controls != null ;
				if (controls == null)
					return;

				tryToStart();
			}
		}

		private void setQuantroSoundPool(QuantroSoundPool pool) {
			this.soundPool = pool;
		}
		
		
		/**
		 * A single-step method that sets all provided graphical parameters.
		 * 
		 * The individual set* methods are useful when these elements change
		 * in the course of gameplay (such as the user selecting a new skin
		 * or background).  However, it is unwise to call them in succession
		 * in onResume, since each might individually re-load game content
		 * redundantly.  Call this method instead.
		 * 
		 * @param drawDetail
		 * @param piecePreviewMinimumProfile
		 * @param loadImageSize
		 * @param loadBackgroundSize
		 * @param blit
		 * @param recycleToVeil
		 * @param animations
		 * @param skin
		 * @param colorScheme
		 * @param background
		 * @param backgroundShuffle
		 * @param shuffleParam_shuffleBackgrounds
		 * @param shuffleParam_shuffleBackgroundMinGap
		 * @param shuffleParam_shuffleBackgroundMaxGap
		 * @param shuffleParam_shuffleOnLevelUp
		 */
		private void setAllGraphics( int drawDetail,
				boolean piecePreviewMinimumProfile,
				int loadImageSize, int loadBackgroundSize, int blit, int scale, boolean recycleToVeil,
				int animations,
				Skin skin, ColorScheme colorScheme,
				Background background, ArrayList<Background> backgroundShuffle,
				boolean shuffleParam_shuffleBackgrounds,
				long shuffleParam_shuffleBackgroundMinGap,
				long shuffleParam_shuffleBackgroundMaxGap,
				boolean shuffleParam_shuffleOnLevelUp ) {
			
			synchronized (surfaceHolder) {
				boolean didChange = false ;
				
				didChange = setGraphicsPreferences_noSyncNoStart(
						drawDetail, piecePreviewMinimumProfile,
						loadImageSize, loadBackgroundSize, blit, scale, recycleToVeil, animations) ;
				didChange = setSkinAndColors_noSyncNoStart( skin, colorScheme ) || didChange ;
				didChange = setBackgrounds_noSyncNoStart( background, backgroundShuffle ) || didChange ;
				setShuffleParameters(
						shuffleParam_shuffleBackgrounds,
						shuffleParam_shuffleBackgroundMinGap,
						shuffleParam_shuffleBackgroundMaxGap,
						shuffleParam_shuffleOnLevelUp ) ;
				
				// Start-up if appropriate.
				if ( didChange )
					tryToStart();
			}
			
		}
		
		private void setGraphicsPreferences(
				int drawDetail,
				boolean piecePreviewMinimumProfile,
				int loadImageSize, int loadBackgroundSize, int blit, int scale, boolean recycleToVeil,
				int animations) {
			
			synchronized (surfaceHolder) {
				boolean didChange = setGraphicsPreferences_noSyncNoStart(
						drawDetail, piecePreviewMinimumProfile,
						loadImageSize, loadBackgroundSize, blit, scale, recycleToVeil, animations) ;
				
				if ( didChange )
					tryToStart();
			}
			
		}

		
		/**
		 * Sets the provided graphics preference, returning whether any element was
		 * changed.
		 * 
		 * This method does NOT synchronize (so any calls to it should be), and does
		 * not attempt to start a new thread or re-load draw structures.
		 * 
		 * @param drawDetail
		 * @param piecePreviewMinimumProfile
		 * @param loadImageSize
		 * @param loadBackgroundSize
		 * @param blit
		 * @param recycleToVeil
		 * @param animations
		 * @return
		 */
		private boolean setGraphicsPreferences_noSyncNoStart(
				int drawDetail,
				boolean piecePreviewMinimumProfile,
				int loadImageSize, int loadBackgroundSize, int blit, int scale,
				boolean recycleToVeil, int animations) {

			boolean didChange = false;
			blit = GlobalTestSettings.setBlit(blit) ;
			scale = GlobalTestSettings.setScale(scale);
			if (this.drawDetail != drawDetail) {
				didChange = true;
				this.drawDetail = drawDetail;
			}
			if ( this.piecePreviewMinimumProfile != piecePreviewMinimumProfile ) {
				didChange = true ;
				this.piecePreviewMinimumProfile = piecePreviewMinimumProfile ;
			}
			if (this.loadImagesSize != loadImageSize) {
				didChange = true;
				this.loadImagesSize = loadImageSize;
			}
			if (this.loadBackgroundSize != loadBackgroundSize) {
				didChange = true;
				this.loadBackgroundSize = loadBackgroundSize;
			}
			if (this.blit != blit) {
				didChange = true;
				this.blit = blit;
			}
			if (this.scale != scale) {
				didChange = true;
				this.scale = scale;
			}
			if (this.recycleToVeil != recycleToVeil) {
				didChange = true;
				this.recycleToVeil = recycleToVeil;
			}
			if (this.animations != animations) {
				didChange = true;
				this.animations = animations;
				// changing animation settings alters the significance of certain
				// in-game events.  We need to update our records.
				if ( this.gevents != null ) {
					for ( int i = 0; i < this.gevents.length; i++ ) {
						this.setGameEventsSignificance(gevents[i]) ;
					}
				}
			}

			Log.d(TAG, "Set blit scale to " + blit + " @ " + scale);
			
			return didChange ;
		}
		
		
		private void setSkinAndColors( Skin skin, ColorScheme colorScheme ) {
			synchronized( surfaceHolder ) {
				boolean didChange = setSkinAndColors_noSyncNoStart( skin, colorScheme ) ;
				
				if ( didChange )
					tryToStart();
			}
		}
		
		
		/**
		 * Sets the current colorScheme and Skin.  This method will update metadata
		 * and draw structures, including toolTip and scoreDrawers.
		 * @param skin
		 * @param colorScheme
		 * @return
		 */
		private boolean setSkinAndColors_noSyncNoStart( Skin skin, ColorScheme colorScheme ) {
			Log.d(TAG, "setSkinAndColors_noSyncNoStart") ;
			boolean didChange = false ;

			if ( !Skin.equals( this.skin, skin ) ) {
				didChange = true;
				this.skin = skin;
			}
			if ( this.colorScheme == null || !this.colorScheme.equals( colorScheme ) ) {
				didChange = true;
				this.colorScheme = colorScheme ;
				// Uses foreground/background instead of red/blue. Remove
				// "false &&" to revert.
				if (this.tooltipDrawer != null) {
					this.tooltipDrawer.setLabelColors(false);
				}
				if (this.scoreDrawer != null) {
					for (int i = 0; i < scoreDrawer.length; i++) {
						if (scoreDrawer[i] != null)
								scoreDrawer[i].setColorScheme(colorScheme);
					}
					if (this.scoreDrawerDetailed != null) {
						for (int i = 0; i < scoreDrawerDetailed.length; i++) {
							if (scoreDrawerDetailed[i] != null)
								scoreDrawerDetailed[i]
										.setColorScheme(colorScheme);
						}
					}
				}
			}
			
			return didChange ;
		}
		
		
		/**
		 * Sets the current background and background shuffle.
		 * 
		 * If not currently running, this method has the side effect of *attempting*
		 * to start the thread (which only succeeds if all other necessary information
		 * has been set).
		 * 
		 * If currently running, this method will cause an in-place update of
		 * the current background and shuffling, without reloading other
		 * game view elements.
		 * 
		 * @param background
		 * @param backgroundShuffle
		 */
		private void setBackgrounds( Background background, ArrayList<Background> backgroundShuffle ) {
			synchronized( surfaceHolder ) {
				boolean didChange = setBackgrounds_noSyncNoStart( background, backgroundShuffle ) ; 
				// this call sets metadata that will be noticed by a currently
				// running Thread, allowing it to swap backgrounds.
				
				// If the Thread is not running, try to start.
				if ( didChange && !running )
					tryToStart();
			}
		}
		
		
		
		/**
		 * Sets the background and shuffle.  Notes the change in relevant metadata,
		 * but does NOT synchronize this change (ONLY CALL THIS METHOD WITHIN
		 * "synchronized( surfaceHolder )") and does not attempt to start the thread
		 * if it isn't running.
		 * 
		 * @param background
		 * @param backgroundShuffle
		 * @return
		 */
		private boolean setBackgrounds_noSyncNoStart( Background background, ArrayList<Background> backgroundShuffle ) {
			boolean didChange = false ;
			
			if ( !Background.equals(background, this.background) ) {
				didChange = true;
				this.background = background;
				this.newBackground = true ;
			}
			if ( backgroundShuffle != null || this.backgroundShuffle != null ) {
				boolean newShuffle = false ;
				if ( ( backgroundShuffle == null ) != ( this.backgroundShuffle == null ) ) {
					newShuffle = true ;
				} else if ( backgroundShuffle.size() != this.backgroundShuffle.size() ) {
					newShuffle = true ;
				} else {
					for ( int i = 0; i < backgroundShuffle.size(); i++ ) {
						if ( !backgroundShuffle.get(i).equals(this.backgroundShuffle.get(i)) ) {
							newShuffle = true ;
						}
					}
				}
				
				if ( newShuffle ) {
					didChange = true ;
					if ( backgroundShuffle == null ) {
						this.backgroundShuffle = null ;
					} else {
						if ( this.backgroundShuffle != null )
							this.backgroundShuffle.clear() ;
						else
							this.backgroundShuffle = new ArrayList<Background>() ;
						for ( Background bg : backgroundShuffle )
							this.backgroundShuffle.add( bg ) ;
					}
				}
				
				// if we have a preloaded 'next shuffle' background, make sure it's still
				// part of our shuffle.
				if ( this.shuffle_nextBackground != null && !this.backgroundShuffle.contains(shuffle_nextBackground) )
					this.shuffle_nextBackground = null ;
			}
			
			return didChange ;
		}
		
		private void setShuffleParameters(
				boolean shuffleParam_shuffleBackgrounds,
				long shuffleParam_shuffleBackgroundMinGap,
				long shuffleParam_shuffleBackgroundMaxGap,
				boolean shuffleParam_shuffleOnLevelUp ) {
			
			this.shuffleParam_shuffleBackgrounds = shuffleParam_shuffleBackgrounds ;
			this.shuffleParam_shuffleBackgroundMinGap = shuffleParam_shuffleBackgroundMinGap ;
			this.shuffleParam_shuffleBackgroundMaxGap = shuffleParam_shuffleBackgroundMaxGap ;
			this.shuffleParam_shuffleOnLevelUp = shuffleParam_shuffleOnLevelUp ;
			
		}

		private void refresh() {
			synchronized (surfaceHolder) {
				this.forceUpdate = true;
			}
		}

		private void setSoundPreferences(boolean soundControls) {
			this.soundControls = soundControls;
		}

		private void setGamePreferences(int pieceTips) {
			this.pieceTips = pieceTips;
			synchronized (surfaceHolder) {
				if (tooltipDrawer != null)
					setPolicy(tooltipDrawer, pieceTips);
			}
		}

		private void tryToStart() {
			Log.d(TAG, "tryToStart");

			boolean shouldBeRunning = gamesReady && canvasReady
					&& controlsReady && canvasBigEnough && mColorScheme != null
					&& background != null && backgroundShuffle != null && backgroundShuffle.size() > 0 ;
			// regenerate all our stuff
			try {
				if (shouldBeRunning) {
					synchronized (surfaceHolder) {
						resizeAndRepositionScreenDrawRegions(displayedRows, COLS,
								canvasWidth, canvasHeight);
						reallocateGameDrawSettingsObjects();
						resizeGameElements();
						reallocateSpecialtyDrawObjects();
						prerenderUIElements();
						reallocateAdditionalDrawSettings();
	
						newDrawSettings = true;
					}
				}
			} catch ( Exception e ) {
				e.printStackTrace() ;
				return ;
			}

			synchronized( startMutex ) {
				if ( !running && shouldBeRunning && !started ) {
					running = true;
					started = true;
					Log.d(TAG, "STARTING") ;
					//new Exception("here").printStackTrace() ;
					this.start();
				} else {
					if ( running && !shouldBeRunning )
						Log.e(TAG, " ***************************************** in tryToStart() halting running thread!") ;
					running = shouldBeRunning;
					Log.d(TAG, "set running to " + running);
				}
			}
		}

		public void setDisplayedGame(int playerNum, int thumbnailNum) {
			Log.d(TAG, "setDisplayedGame to " + playerNum + ", thumbnail is " + thumbnailNum) ;
			// Log.d(TAG, "START   Synchronization in setDisplayedGame") ;
			synchronized (surfaceHolder) {
				if ( playerNum == displayedGame && thumbnailNum == thumbnailGame )
					return ;
				
				if (thumbnailNum != thumbnailGame)
					newThumbnailGameBlocksSlice = true;
				
				boolean gameChange = playerNum != displayedGame ;
				boolean thumbnailChange = thumbnailNum != thumbnailGame ;

				// Log.d(TAG, "setting displayed game to " + playerNum) ;
				if (playerNum == DEFAULT_GAME)
					displayedGame = defaultDisplayedGame;
				else
					displayedGame = playerNum;

				if (thumbnailNum == DEFAULT_GAME)
					thumbnailGame = defaultDisplayedGame;
				else
					thumbnailGame = thumbnailNum;

				// Change visibility.
				if (blockDrawer != null && gameChange) {
					blockDrawer.setVisible(true);
					blockDrawer.nextGameBlocksSliceBreaksSequence();
				}
				if ( blockDrawerMini != null && thumbnailChange ) {
					blockDrawerMini.setVisible(true) ;
					blockDrawerMini.nextGameBlocksSliceBreaksSequence() ;
				}
				if (nextPieceBlockDrawer != null && gameChange) {
					changedNextPieceSlice = true ;
					nextPieceBlockDrawer.setVisible(true);
					nextPieceBlockDrawer
							.nextGameBlocksSliceBreaksSequence();
					changedNextPieceSlice = true ;
				}
				if (reservePieceBlockDrawer != null && gameChange) {
					changedReservePieceSlice = true ;
					reservePieceBlockDrawer.setVisible(true);
					reservePieceBlockDrawer
							.nextGameBlocksSliceBreaksSequence();
					changedReservePieceSlice = true ;
				}
				
				if ( playerStatusDrawer != null ) {
					playerStatusDrawer.setDisplayedPlayer(playerNum, playerNum == this.defaultDisplayedGame) ;
				}
			}
			// Log.d(TAG, "-END-   Synchronization in setDisplayedGame") ;
		}

		public void setDefaultDisplayedGame(int playerNum, boolean isPlayer) {
			// Log.d(TAG, "setDefaultDisplayedGame") ;
			defaultDisplayedGame = playerNum;
			if ( isPlayer ) {
				localPlayerGame = playerNum ;
				if ( playerStatusDrawer != null ) {
					playerStatusDrawer.setLocalPlayer(playerNum) ;
				}
				resetPanelOpponentPressedNumbersText() ;
			}
			else
				localPlayerGame = -1 ;
			if (handler != null) {
				handler.sendEmptyMessage(WHAT_SLICE_FALLING_UPDATE);
				handler.sendEmptyMessage(WHAT_SLICE_NEXT_UPDATE);
				handler.sendEmptyMessage(WHAT_SLICE_RESERVE_UPDATE);
			}
		}

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) {
			// Log.d(TAG, "setSurfaceSize") ;
			// synchronized to make sure these all change atomically
			synchronized (surfaceHolder) {
				canvasWidth = width;
				canvasHeight = height;
				

				// Changing the surface size changes block size and
				// block placement.

				canvasBigEnough = canvasWidth > 20 && canvasHeight > 20;
				if (!canvasBigEnough)
					Log.e(TAG, "setSurfaceSize to " + canvasWidth + ", "
							+ canvasHeight + " is not large enough");

				tryToStart();
			}
		}

		/**
		 * Given displayedRows, COLS, canvasHeight, canvasWidth, determines the
		 * appropriate sizes for rect_gameArea, rect_panelLeft and
		 * rect_panelRight.
		 * 
		 * POSTCONDITION: rect_gameArea, rect_panelLeft, rect_panelRight have
		 * been set to the appropriate sizes within the canvas.
		 * 
		 * This method is a required call before anything will be displayed. As
		 * a minimum Precondition, we should know the number of onscreen rows
		 * and columns, and the canvas size available.
		 * 
		 */
		private void resizeAndRepositionScreenDrawRegions(int screenRows,
				int screenCols, int canvasWidth, int canvasHeight) {

			// Log.d(TAG, "resizeAndRepositionScreenDrawRegions") ;
			int gameAreaWidth = Math.round(Math.min(canvasHeight
					* ((float) screenCols / screenRows), canvasWidth * GAME_FIELD_MAX_WIDTH));
			// Shrink this slightly to get the edges flush and scale integer, if possible.
			if (gameAreaWidth % 2 != canvasWidth % 2) {
				gameAreaWidth++; // Ensure canvasWidth - gameAreaWidth is even, for l/r panels
			}
			// Shrink so blocks are upscaled pixel-perfect (no blurry edges)
			Log.d(TAG, "width " + gameAreaWidth + " scale " + scale + " shrinking game area by " + (gameAreaWidth % scale) + " pixels for exact integer upscale");
			gameAreaWidth -= (gameAreaWidth % scale);

			int panelWidth = (canvasWidth - gameAreaWidth) / 2;
			this.rect_gameArea = new Rect(panelWidth, 0, panelWidth
					+ gameAreaWidth, canvasHeight);

			this.rect_panelLeft = new Rect(0, 0, panelWidth, canvasHeight);
			this.rect_panelRight = new Rect(panelWidth + gameAreaWidth, 0,
					canvasWidth, canvasHeight);
			

			Log.d(TAG, "gameAreaWidth is " + gameAreaWidth
					+ " for canvas dimensions " + canvasWidth + ", "
					+ canvasHeight);

			if (this.gameMode > -1 && this.ginfo[0] != null && canvasWidth > 10
					&& canvasHeight > 10) {
				// this call uses rect_gameArea
				DrawSettings ds = newDrawSettings(ginfo[0],
						drawDetail, animations, false);
				// Get the total horizontal space required.
				int width = ds.getBlitBlockfieldWidth();

				// check qXOffset; compare size with proportion. Any extra
				// pixels?
				int qXOffset_best = Math.round(ds.proportion_qXOffset
						* ds.getSize_blockWidth());
				Log.d(TAG, "qXOffset is " + ds.getSize_qXOffset()
						+ ", versus best-approx. " + qXOffset_best);
				if (Math.abs(ds.getSize_qXOffset()) > Math.abs(qXOffset_best)) {
					Log.d(TAG, "width was " + width);
					width -= Math.max(
							0,
							Math.abs(ds.getSize_qXOffset())
									- Math.abs(qXOffset_best) - 2);
					Log.d(TAG, "shrunk to " + width);
				}

				Log.d(TAG, "default width is " + rect_gameArea.width()
						+ " compared to necessary block width " + width);

				// shrink rect_gameArea, grow rect_panelLeft/Right by this
				// amount.
				int diff = rect_gameArea.width() - width;
				int halfDiff = diff / 2;
				int diffLeft, diffRight;
				if (rect_panelLeft.width() <= rect_panelRight.width()) {
					diffLeft = halfDiff + diff % 2;
					diffRight = halfDiff;
				} else {
					diffLeft = halfDiff;
					diffRight = halfDiff + diff % 2;
				}

				if (diff > 0) {
					rect_gameArea.left += diffLeft;
					rect_gameArea.right -= diffRight;

					rect_panelLeft.right += diffLeft;
					rect_panelRight.left -= diffRight;
				}

				Log.d(TAG, "resized width is " + rect_gameArea.width()
						+ " compared to necessary block width " + width);

				Log.d(TAG,
						"in resizeAndRepositionScreenDrawRegions, resized to fit block width "
								+ ds.getSize_blockWidth() + ", qOffset "
								+ ds.getSize_qXOffset());
			}
		}

		private void reallocateGameDrawSettingsObjects() {
			// Log.d(TAG, "reallocateGameDrawSettingsObject") ;
			if (ginfo != null && ginfo[0] != null) {
				blockFieldDrawSettings = newDrawSettings(ginfo[0],
						drawDetail,
						animations, true);
				
				int mult = blockFieldDrawSettings.getScale() ;
				controls.setMovementGrid(
						blockFieldDrawSettings.getSize_blockWidth() * mult,
						blockFieldDrawSettings.getSize_blockHeight() * mult ) ;
			}
		}

		/**
		 * Constructs and returns a DrawSettings object for the specified game
		 * information. Uses fields:
		 * 
		 * this.ROWS this.COLS this.displayedRows this.rect_gameArea
		 * this.context
		 * 
		 * The returned DrawSettings object has 'draw' set to True by default.
		 * 
		 * @param ginfo
		 * @return
		 */
		private DrawSettings newDrawSettings(GameInformation ginfo,
				int drawDetail, int animationDetail, boolean horizontalFlush) {
			if (ginfo == null)
				return null;
			if (this.rect_gameArea == null)
				return null;
			DrawSettings ds = new DrawSettings(this.rect_gameArea,
					horizontalFlush, ROWS, COLS, displayedRows,
					drawDetail, animationDetail,
					mSkin, getContext());
			ds.drawEffects = DrawSettings.DRAW_EFFECTS_THROUGH_SLICE ;

			ds.setBackground(this.background);
			this.newBackground = false ;
			
			ds.setBehaviorIs_displacement(displaces) ;
			
			ds.loadImagesSize = loadImagesSize;
			ds.loadBackgroundSize = loadBackgroundSize;

			ds.setCanvasTarget(this.blit, this.scale, this.rect_gameArea) ;
			if (this.scale > 1) {
				ds.setBlockSizesToFit();
			}
			
			if (ds.getBackground() != null) {
				if ( displaces && ds.getBlit() != DrawSettings.BLIT_NONE )
					ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_BLIT_IMAGE ;
				else
					ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_CLIP_OR_BLIT_IMAGE;
			}

			if (GameModes.numberQPanes(ginfo) == 2 || displaces)
				ds.behavior_border = DrawSettings.BEHAVIOR_BORDER_ROW_NEGATIVE_ONE;
			else
				ds.behavior_border = DrawSettings.BEHAVIOR_BORDER_NO_EDGES;

			Log.d(TAG, "newDrawSettings: fit with block width "
					+ ds.getSize_blockWidth() + ", qXOffset " + ds.getSize_qXOffset()
					+ " to fill space " + this.rect_gameArea.width());

			return ds;
		}

		private void resizeGameElements() {
			// Log.d(TAG, "resizeGameElements") ;
			// Can't do anything yet...
			if (this.view_invisibleControls == null || gameMode == -1)
				return;

			// Get the rectangles for each button.
			// Log.d(TAG, "START   Synchronization in resizeGameElements") ;
			synchronized (surfaceHolder) {
				Resources res = getContext().getResources();
				// We will call this very rarely (when a game starts or screen
				// dimensions change)
				// it's not a problem to allocate and allow GC.
				this.rect_button = ( numPlayers > 1 || GlobalTestSettings.GAME_SELF_THUMBNAIL )
						? new Rect[INDEX_BUTTON_OPPONENT + 1]
				        : new Rect[INDEX_BUTTON_OPPONENT];
				this.rect_touchable_button = (numPlayers > 1 || GlobalTestSettings.GAME_SELF_THUMBNAIL)
						? new Rect[INDEX_BUTTON_OPPONENT + 1]
						: new Rect[INDEX_BUTTON_OPPONENT];
				this.name_button = (numPlayers > 1 || GlobalTestSettings.GAME_SELF_THUMBNAIL)
						? new String[INDEX_BUTTON_OPPONENT + 1]
						: new String[INDEX_BUTTON_OPPONENT];
				name_button[INDEX_BUTTON_L] = res
						.getString(R.string.controls_left_button_name);
				name_button[INDEX_BUTTON_R] = res
						.getString(R.string.controls_right_button_name);
				name_button[INDEX_BUTTON_D] = res
						.getString(R.string.controls_down_button_name);
				name_button[INDEX_BUTTON_CW] = res
						.getString(R.string.controls_cw_button_name);
				name_button[INDEX_BUTTON_CCW] = res
						.getString(R.string.controls_ccw_button_name);
				name_button[INDEX_BUTTON_FLIP] = res
						.getString(R.string.controls_flip_button_name) ;
				name_button[INDEX_BUTTON_SCORE] = res
						.getString(R.string.controls_score_button_name);
				name_button[INDEX_BUTTON_RESERVE] = res
						.getString(R.string.controls_reserve_button_name);
				if (numPlayers > 1
						|| GlobalTestSettings.GAME_SELF_THUMBNAIL)
					name_button[INDEX_BUTTON_OPPONENT] = res
							.getString(R.string.controls_opponent_button_name);
				
				// HEY GUESS WHAT??  Now's a good time to set the appropriate
				// icon for the reserve button.  'alt' is attacks.
				view_invisibleControls.setUseAlt(
						game[0].hasSpecialReserveAttack(),
						name_button[INDEX_BUTTON_RESERVE]) ;

				// TODO: Ensure the rightmost buttons align fully with the right
				// edge of the canvas.
				for (int i = 0; i < rect_button.length; i++ )
					rect_button[i] = this.view_invisibleControls
							.getButtonBoundsByName(name_button[i]) ;
				for (int i = 0; i < rect_touchable_button.length; i++)
					rect_touchable_button[i] = this.view_invisibleControls
							.getTouchableButtonBoundsByName(name_button[i]);
				if ( this.displayAlternatePanelForOpponent )
					rect_opponentSelectionRegion = this.view_invisibleControls.getSlideRegionsByName(name_button[INDEX_BUTTON_OPPONENT]) ;
				else
					rect_opponentSelectionRegion = null ;
				// Tell the controls how much of these buttons intersects
				// with our side panels.
				Rect visibleRect = new Rect() ;
				boolean touchableChanged = false ;
				for ( int i = 0; i < rect_touchable_button.length; i++ )  {
					Rect r = rect_touchable_button[i] ;
					if ( r == null )
						continue ;
					visibleRect.set(r) ;
					if ( Rect.intersects(rect_panelLeft, r) )
						visibleRect.right = rect_panelLeft.right ;
					else if ( Rect.intersects(rect_panelRight, r) )
						visibleRect.left = rect_panelRight.left ;
					else
						visibleRect.set(0,0,0,0) ;
					if ( rect_touchable_button[i] != null ) {
						touchableChanged = view_invisibleControls.setButtonVisibleBoundsByName(
								name_button[i], visibleRect ) || touchableChanged ;
					}
				}
				if ( touchableChanged ) {
					view_invisibleControls.remeasure() ;
					for (int i = 0; i < rect_button.length; i++ )
						rect_button[i] = this.view_invisibleControls
								.getButtonBoundsByName(name_button[i]) ;
					for (int i = 0; i < rect_touchable_button.length; i++)
						rect_touchable_button[i] = this.view_invisibleControls
								.getTouchableButtonBoundsByName(name_button[i]);
				}

				// How much, in pixels, should we shrink each rect button to
				// obtain the
				// dip margin for button rects? (we shrink each rect by 1/2 the
				// margin,
				// and thus get approx. the correct amount between buttons)
				int margin_px = res
						.getDimensionPixelOffset(R.dimen.margin_game_button);

				/*
				// We move all rectangle boundaries inward, EXCEPT those which
				// lie against
				// the edges of the canvas (or go beyond it, why not).
				for (int i = 0; i < rect_button.length; i++) {
					Rect r = rect_button[i];
					if (r != null) {
						if (r.left > 0)
							r.left += margin_px;
						if (r.top > 0)
							r.top += margin_px;
						if (r.right < canvasWidth - 1)
							r.right -= margin_px;
						if (r.bottom < canvasHeight - 1)
							r.bottom -= margin_px;
					}
				} */

				// A few simple conventions for draw regions. For those that
				// have a dedicated button area, match that area, then shrink
				// the sides to be entirely outside of the game field's draw
				// region.
				// Those that don't have dedicate areas have their own special
				// procedure.
				// After this, shrink the margins further.
				this.rect_drawRegion = (numPlayers > 1 || GlobalTestSettings.GAME_SELF_THUMBNAIL) ? new Rect[INDEX_DRAW_REGION_OPPONENT + 1]
						: new Rect[INDEX_DRAW_REGION_RESERVE + 1];

				// Step one: get the "button rectangle" for each (even those
				// without a button)
				if (this.rect_touchable_button[INDEX_BUTTON_SCORE] != null)
					this.rect_drawRegion[INDEX_DRAW_REGION_SCORE] = new Rect(
							this.rect_touchable_button[INDEX_BUTTON_SCORE]);
				if (this.rect_touchable_button[INDEX_BUTTON_RESERVE] != null)
					this.rect_drawRegion[INDEX_DRAW_REGION_RESERVE] = new Rect(
							this.rect_touchable_button[INDEX_BUTTON_RESERVE]);
				if ((numPlayers > 1 || GlobalTestSettings.GAME_SELF_THUMBNAIL)
						&& this.rect_touchable_button[INDEX_BUTTON_OPPONENT] != null)
					this.rect_drawRegion[INDEX_DRAW_REGION_OPPONENT] = new Rect(
							this.rect_touchable_button[INDEX_BUTTON_OPPONENT]);
				
				// shrink the draw region to fit within either the left or right panel.
				int [] drawRegionIndices = new int[]{
						INDEX_DRAW_REGION_SCORE,
						INDEX_DRAW_REGION_RESERVE,
						INDEX_DRAW_REGION_OPPONENT
				} ;
				for ( int i = 0; i < drawRegionIndices.length; i++ ) {
					int index = drawRegionIndices[i] ;
					if ( index >= this.rect_drawRegion.length )
						continue ;
					Rect r = this.rect_drawRegion[index] ;
					if ( r != null ) {
						boolean left = true ;
						if ( r.centerX() >= canvasWidth/2 )
							left = false ;
						if ( left )
							r.right = this.rect_panelLeft.right ;
						else
							r.left = this.rect_panelRight.left ;
					}
				}

				// Change 12/14/2012: we put the "next" piece region opposite
				// the "reserve" region.  Previously we just put it on the left.
				// Now, estimate the "reserve" side using its center (left or right
				// of GameView center?) and align "Next" to the opposite top corner,
				// and set the bottom edge to the highest element in that panel.
				boolean left = true ;
				if ( this.rect_drawRegion[INDEX_DRAW_REGION_RESERVE].centerX() < canvasWidth/2 )
					left = false ;
				displayNextPreviewOnLeft = left ;
				
				Rect nextPieceRect ;
				if ( left ) {
					nextPieceRect = new Rect(margin_px, margin_px,
							rect_panelLeft.right - margin_px, canvasHeight
									- margin_px);
				} else {
					nextPieceRect = new Rect(
							rect_panelRight.left + margin_px,
							margin_px,
							rect_panelRight.right - margin_px,
							canvasHeight - margin_px) ;
				}
				for (int i = 0; i < rect_button.length; i++) {
					Rect r = rect_button[i];
					if (r != null) {
						// Test for left panel.
						if ( left && this.rect_panelLeft.left <= r.left
								&& r.left < this.rect_panelLeft.right) {
							// both this rect and nextPiece are left-panel aligned.
							nextPieceRect.bottom = Math
									.min(nextPieceRect.bottom, r.top
											- margin_px * 2);
						} else if ( !left && this.rect_panelRight.right >= r.right
								&& r.right > this.rect_panelRight.left ) {
							// both this rect and nextPiece are right-panel aligned
							nextPieceRect.bottom = Math
									.min(nextPieceRect.bottom, r.top
											- margin_px * 2);
						}
					}
				}
				this.rect_drawRegion[INDEX_DRAW_REGION_NEXT] = nextPieceRect;
				Log.d(TAG, "Next Piece draw region is " + nextPieceRect) ;

				// Now shrink all of them further. Ensure left/right bounds are
				// appropriate...
				for (int i = 0; i < rect_drawRegion.length; i++) {
					Rect r = rect_drawRegion[i];
					if (r != null) {
						if (this.rect_panelLeft.left <= r.left
								&& r.left < this.rect_panelLeft.right)
							r.right = r.right - mSize[SIZE_INDEX_TOTAL_EDGE_WIDTH];
						else
							r.left = r.left + mSize[SIZE_INDEX_TOTAL_EDGE_WIDTH];
					}
				}

				// ...and apply the draw region margin to all.
				margin_px = res
						.getDimensionPixelOffset(R.dimen.margin_game_drawable);

				for (int i = 0; i < rect_drawRegion.length; i++) {
					Rect r = rect_drawRegion[i];
					if (r != null) {
						r.inset(margin_px, margin_px);
					}
				}

				// finally, if possible, shrink the 'NEXT' and 'RESERVE' draw
				// regions.
				int maxBlockWidth = blockFieldDrawSettings == null ? Integer.MAX_VALUE
						: blockFieldDrawSettings.getSize_blockWidth();
				int maxBlockHeight = blockFieldDrawSettings == null ? Integer.MAX_VALUE
						: blockFieldDrawSettings.getSize_blockHeight();
				for (int i = 0; i < rect_drawRegion.length; i++) {
					if (i == INDEX_DRAW_REGION_NEXT
							|| i == INDEX_DRAW_REGION_RESERVE) {
						Rect r = rect_drawRegion[i];
						DrawSettings ds = newPiecePreviewDrawSettings(r,
								maxBlockWidth, maxBlockHeight);
						int heightNeeded = ds.getBlockYPosition(-1, -1)
								- ds.getBlockYPosition(-1, 4);
						heightNeeded *= ds.getScale();	// scale size up scaling
						
						int diff = r.height() - heightNeeded;
						if (diff > 0) {
							r.top += diff / 2;
							r.bottom -= diff / 2;
						}
					}
				}
				
				// Score region: top and bottom determined by the score button;
				// left and right is the panel plus edge.
				rect_panelScore = new Rect(rect_panelLeft.right,
						rect_touchable_button[INDEX_BUTTON_SCORE].top,
						rect_panelRight.left,
						rect_touchable_button[INDEX_BUTTON_SCORE].bottom);
				
				displayOpponentThumbnailOnLeft = true ;
				if ( INDEX_BUTTON_OPPONENT < rect_touchable_button.length && rect_touchable_button[INDEX_BUTTON_OPPONENT].centerX() > canvasWidth/2 )
					displayOpponentThumbnailOnLeft = false ;
				if ( displayOpponentThumbnailOnLeft ) {
					rect_panelOpponentPressedNeg = new Rect(rect_panelLeft.right,
							this.rect_gameArea.top, rect_panelRight.right, rect_gameArea.bottom) ;
				} else {
					rect_panelOpponentPressedNeg = new Rect(rect_panelLeft.left,
							this.rect_gameArea.top, rect_panelRight.left, rect_gameArea.bottom) ;
				}

				rect_tooltips = new Rect(rect_panelScore);
			}
			// Log.d(TAG, "-END-   Synchronization in resizeGameElements") ;
		}

		/**
		 * Renders and prepares drawable_panelLeft, drawable_panelRight,
		 * drawable_panelScore, and drawable_over. Additionally, returns a
		 * Region which clips all side-panel elements except the background
		 * color itself. This region does NOT include those elements drawn in
		 * the "score region," or those drawn in the "over" layer.
		 * 
		 * @return
		 */
		private synchronized Region prerenderUIElements() {
			Log.d(TAG, "prerenderUIElements") ;
			synchronized (recycleMutex) {
				Log.d(TAG, "prerenderUIElements: releasingUIElements") ;
				releaseUIElements();
			}

			if (rect_touchable_button == null || !canvasBigEnough)
				return null;
			
			Log.d(TAG, "prerenderUIElements: prerendering...") ;
			
			Resources res = getResources();
			DisplayMetrics displayMetrics = res.getDisplayMetrics() ;

			Canvas c;

			bitmap_panelFrame = new ArrayList<Bitmap>() ;
			bitmap_panelFramePosition = new ArrayList<Offset>() ;
			bitmap_panelLeft = new ArrayList<Bitmap>();
			bitmap_panelLeftPosition = new ArrayList<Offset>();
			bitmap_panelRight = new ArrayList<Bitmap>();
			bitmap_panelRightPosition = new ArrayList<Offset>();
			bitmap_panelScore = new ArrayList<Bitmap>();
			bitmap_panelScorePosition = new ArrayList<Offset>();
			bitmap_over = new ArrayList<Bitmap>();
			bitmap_overPosition = new ArrayList<Offset>();
			bitmap_overFlag = new ArrayList<Integer>();
			
			bitmap_panelOpponentPressed = new ArrayList<Bitmap>() ;
			bitmap_panelOpponentPressedPosition = new ArrayList<Offset>() ;
			
			this.text_panelOpponentPressedNumbers = new ArrayList<String>() ;
			this.text_panelOpponentPressedNumbersIndex = new ArrayList<Integer>() ;
			this.text_panelOpponentPressedNumbersPosition = new ArrayList<Offset>() ;

			ArrayList<ArrayList<Bitmap>> bitmap_panel = new ArrayList<ArrayList<Bitmap>>();
			bitmap_panel.add(bitmap_panelLeft);
			bitmap_panel.add(bitmap_panelRight);

			ArrayList<ArrayList<Offset>> bitmap_panelPosition = new ArrayList<ArrayList<Offset>>();
			bitmap_panelPosition.add(bitmap_panelLeftPosition);
			bitmap_panelPosition.add(bitmap_panelRightPosition);

			Region region = new Region();

			// Left panel?
			GradientDrawable outerEdgeGradient = new GradientDrawable(
					Orientation.TOP_BOTTOM, new int[] {
							mColor[INDEX_PANEL_OUTER_EDGE_TOP],
							mColor[INDEX_PANEL_OUTER_EDGE_BOTTOM] });
			GradientDrawable innerEdgeGradient = new GradientDrawable(
					Orientation.TOP_BOTTOM, new int[] {
							mColor[INDEX_PANEL_INNER_EDGE_TOP],
							mColor[INDEX_PANEL_INNER_EDGE_BOTTOM] });

			// We use 2 pixels, 1 pixel for these.
			outerEdgeGradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
			outerEdgeGradient.setDither(true);
			innerEdgeGradient.setGradientType(GradientDrawable.LINEAR_GRADIENT);
			innerEdgeGradient.setDither(true);

			Rect rect = new Rect();

			// We draw these gradients for later reference: we match colors
			// against the result when setting up drawable_scorePanel.
			Bitmap outerEdgeGradientBitmap = Bitmap.createBitmap(
					mSize[SIZE_INDEX_OUTER_EDGE_WIDTH],
					rect_panelLeft.height(), Bitmap.Config.RGB_565);
			c = new Canvas(outerEdgeGradientBitmap);
			rect.set(0, 0, c.getWidth(), c.getHeight());
			outerEdgeGradient.setBounds(rect);
			outerEdgeGradient.draw(c);

			// Include in "frame panel bitmaps"
			bitmap_panelFrame.add(outerEdgeGradientBitmap);
			bitmap_panelFramePosition.add(new Offset(rect_panelLeft.width()
					- mSize[SIZE_INDEX_OUTER_EDGE_WIDTH], 0));

			// Include region at actual boundaries
			rect.set(rect_panelLeft.width()
					- mSize[SIZE_INDEX_OUTER_EDGE_WIDTH], 0,
					rect_panelLeft.width(), rect_panelLeft.height());
			region.op(rect, Region.Op.UNION);

			// Inner bitmap
			// We draw these gradients for later reference: we match colors
			// against the result when setting up drawable_scorePanel.
			Bitmap innerEdgeGradientBitmap = Bitmap.createBitmap(
					mSize[SIZE_INDEX_INNER_EDGE_WIDTH],
					rect_panelLeft.height(), Bitmap.Config.RGB_565);
			c = new Canvas(innerEdgeGradientBitmap);
			rect.set(0, 0, c.getWidth(), c.getHeight());
			innerEdgeGradient.setBounds(rect);
			innerEdgeGradient.draw(c);

			// Include in "frame panel bitmaps"
			bitmap_panelFrame.add(innerEdgeGradientBitmap);
			bitmap_panelFramePosition.add(new Offset(rect_panelLeft.width()
					- mSize[SIZE_INDEX_OUTER_EDGE_WIDTH]
					- mSize[SIZE_INDEX_INNER_EDGE_WIDTH], 0));

			// Add to our region.
			rect.set(
					rect_panelLeft.width() - mSize[SIZE_INDEX_OUTER_EDGE_WIDTH]
							- mSize[SIZE_INDEX_INNER_EDGE_WIDTH],
					0,
					rect_panelLeft.width() - mSize[SIZE_INDEX_OUTER_EDGE_WIDTH],
					rect_panelLeft.height());
			region.op(rect, Region.Op.UNION);

			// Include in "frame panel bitmaps"
			bitmap_panelFrame.add(outerEdgeGradientBitmap);
			bitmap_panelFramePosition.add(new Offset(rect_panelRight.left, 0));

			// Add to our region
			rect.set(0, 0, mSize[SIZE_INDEX_OUTER_EDGE_WIDTH],
					rect_panelRight.height());
			rect.offset(rect_panelRight.left, 0);
			region.op(rect, Region.Op.UNION);

			// Include in "frame panel bitmaps"
			bitmap_panelFrame.add(innerEdgeGradientBitmap);
			bitmap_panelFramePosition.add(new Offset(rect_panelRight.left
					+ mSize[SIZE_INDEX_OUTER_EDGE_WIDTH], 0));

			// Add to our region
			rect.set(mSize[SIZE_INDEX_OUTER_EDGE_WIDTH], 0,
					mSize[SIZE_INDEX_OUTER_EDGE_WIDTH]
							+ mSize[SIZE_INDEX_INNER_EDGE_WIDTH],
					rect_panelRight.height());
			rect.offset(rect_panelRight.left, 0);
			region.op(rect, Region.Op.UNION);

			// Fill the score panel...
			// the score panel needs to override the edges, so include those
			// edges in its size.

			// Quick check: we may need to expand this region to hold detailed
			// score.
			DetailedScoreDrawer dsd = new DetailedScoreDrawer(null,
					getContext().getResources(), rect_panelScore, colorScheme,
					screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE);
			int necessaryHeight = dsd.height();
			if (necessaryHeight > rect_panelScore.height()) {
				// We need room! Expand the rect_panelScore.
				int extraPixels = (int) Math
						.ceil((rect_panelScore.height() - necessaryHeight) / 2);
				rect_panelScore.inset(0, -extraPixels);
				Log.d(TAG, "expanding detailed panel score by " + extraPixels
						+ " in both top and bottom");
			}
			rect_panelScoreDrawableRegion = new Rect(rect_panelScore);
			rect_panelScoreDrawableRegion.left -= (mSize[SIZE_INDEX_OUTER_EDGE_WIDTH] + mSize[SIZE_INDEX_INNER_EDGE_WIDTH]);
			rect_panelScoreDrawableRegion.right += (mSize[SIZE_INDEX_OUTER_EDGE_WIDTH] + mSize[SIZE_INDEX_INNER_EDGE_WIDTH]);

			// Draw the top edges. Because we drew a gradient, we sample
			// the existing color from the bitmaps. Sample from the right panel;
			// it is easier.
			int y_outer, y_inner;
			y_outer = rect_panelScore.top - mSize[SIZE_INDEX_OUTER_EDGE_WIDTH]
					- mSize[SIZE_INDEX_INNER_EDGE_WIDTH];
			y_inner = rect_panelScore.top - mSize[SIZE_INDEX_INNER_EDGE_WIDTH];
			int colorOuter = outerEdgeGradientBitmap.getPixel(0, y_outer);
			int colorInner = innerEdgeGradientBitmap.getPixel(0, y_inner);

			// Inner edge: region and bitmap.
			Bitmap scoreTopInnerEdgeBitmap = Bitmap.createBitmap(
					rect_panelScoreDrawableRegion.width(),
					mSize[SIZE_INDEX_INNER_EDGE_WIDTH], Bitmap.Config.RGB_565);
			c = new Canvas(scoreTopInnerEdgeBitmap);
			c.drawColor(colorInner);
			bitmap_panelScore.add(scoreTopInnerEdgeBitmap);
			bitmap_panelScorePosition.add(new Offset(
					rect_panelScoreDrawableRegion.left, y_inner));

			// Outer edge: region and bitmap
			Bitmap scoreTopOuterEdgeBitmap = Bitmap.createBitmap(
					rect_panelScoreDrawableRegion.width()
							- mSize[SIZE_INDEX_INNER_EDGE_WIDTH] * 2,
					mSize[SIZE_INDEX_OUTER_EDGE_WIDTH], Bitmap.Config.RGB_565);
			c = new Canvas(scoreTopOuterEdgeBitmap);
			c.drawColor(colorOuter);
			bitmap_panelScore.add(scoreTopOuterEdgeBitmap);
			bitmap_panelScorePosition.add(new Offset(
					rect_panelScoreDrawableRegion.left
							+ mSize[SIZE_INDEX_INNER_EDGE_WIDTH], y_outer));

			// Bottom gradients
			y_outer = rect_panelScore.bottom
					+ mSize[SIZE_INDEX_INNER_EDGE_WIDTH];
			y_inner = rect_panelScore.bottom;
			colorOuter = outerEdgeGradientBitmap.getPixel(0, y_outer);
			colorInner = innerEdgeGradientBitmap.getPixel(0, y_inner);

			// Inner edge: region and bitmap
			Bitmap scoreBottomInnerEdgeBitmap = Bitmap.createBitmap(
					rect_panelScoreDrawableRegion.width(),
					mSize[SIZE_INDEX_INNER_EDGE_WIDTH], Bitmap.Config.RGB_565);
			c = new Canvas(scoreBottomInnerEdgeBitmap);
			c.drawColor(colorInner);
			bitmap_panelScore.add(scoreBottomInnerEdgeBitmap);
			bitmap_panelScorePosition.add(new Offset(
					rect_panelScoreDrawableRegion.left, y_inner));

			// Outer edge: region and bitmap
			Bitmap scoreBottomOuterEdgeBitmap = Bitmap.createBitmap(
					rect_panelScoreDrawableRegion.width()
							- mSize[SIZE_INDEX_INNER_EDGE_WIDTH] * 2,
					mSize[SIZE_INDEX_OUTER_EDGE_WIDTH], Bitmap.Config.RGB_565);
			c = new Canvas(scoreBottomOuterEdgeBitmap);
			c.drawColor(colorOuter);
			bitmap_panelScore.add(scoreBottomOuterEdgeBitmap);
			bitmap_panelScorePosition.add(new Offset(
					rect_panelScoreDrawableRegion.left
							+ mSize[SIZE_INDEX_INNER_EDGE_WIDTH], y_outer));

			// Draw dividers. We draw horizontal dividers centered horizontally
			// within the panel, at the vertical position of the top and bottom edge of
			// each button.  We omit a divider iff:
			// 1. The button does not intersect with the panel
			// 2. The position puts the divider within 5 pixels of the top or bottom edge
			// 3. The divider is at the BOTTOM edge of a button, and the
			//		button itself is within 5 pixels (vertical alignment)
			//		of another that intersects the same panel.
			int midColor = mColor[INDEX_PANEL_OUTER_EDGE_BOTTOM];
			int endColor = Color.argb(0, Color.red(midColor),
					Color.green(midColor), Color.blue(midColor));

			int dividerInset = (int) (0.1 * rect_panelLeft.width());
			int edgeWidth = mSize[SIZE_INDEX_OUTER_EDGE_WIDTH]
					+ mSize[SIZE_INDEX_INNER_EDGE_WIDTH];

			Bitmap dividerBitmap = Bitmap.createBitmap(rect_panelLeft.width()
					- dividerInset * 2 - edgeWidth, 1, Bitmap.Config.RGB_565);
			c = new Canvas(dividerBitmap);
			c.drawColor(mColor[INDEX_PANEL_BACKGROUND]);

			// apply a gradient drawable
			GradientDrawable divider = new GradientDrawable(
					Orientation.LEFT_RIGHT, new int[] { endColor, midColor,
							endColor });
			divider.setDither(true);
			divider.setBounds(0, 0, c.getWidth(), c.getHeight());
			// draw
			divider.draw(c);

			for (int i = 0; i < 2; i++) {
				Rect panelRect;
				if (i == 0)
					panelRect = rect_panelLeft;
				else
					panelRect = rect_panelRight;

				for (int j = 0; j < rect_touchable_button.length; j++) {
					Rect r = rect_touchable_button[j];
					
					if ( r == null )
						continue ;
					
					// two dividers.  Omit a divider if:
					// 1. The button does not intersect with the panel
					// 2. The position puts the divider within 5 pixels of the top or bottom edge
					// 3. The divider is at the BOTTOM edge of a button, and the
					//		button itself is within 5 pixels (vertical alignment)
					//		of another that intersects the same panel.
					boolean hasTop = true, hasBottom = true ;
					if ( r == null || !Rect.intersects(panelRect, r) )
						hasTop = hasBottom = false ;
					if ( Math.abs( r.top - panelRect.top ) <= 5 )
						hasTop = false ;
					if ( Math.abs( r.bottom - panelRect.bottom ) <= 5 )
						hasBottom = false ;
					for ( int k = 0; k < rect_touchable_button.length && hasBottom; k++ ) {
						if ( j != k ) {
							Rect r2 = rect_touchable_button[k] ;
							if ( r2 != null && Rect.intersects(panelRect, r2) ) {
								// same panel...
								if ( Math.abs( r.bottom - r2.top ) <= 5 )
									hasBottom = false ;
							}
						}
					}
					

					// put 0, 1 or 2 dividers here.
					if ( hasTop ) {
						rect.set(
								dividerInset + (i == 0 ? 0 : edgeWidth),
								r.top,
								panelRect.width() - dividerInset - (i == 1 ? 0 : edgeWidth),
								r.top + 1 );
						rect.offset(panelRect.left, 0);
						region.op(rect, Region.Op.UNION);
	
						bitmap_panel.get(i).add(dividerBitmap);
						bitmap_panelPosition.get(i).add(
								new Offset(
										dividerInset + (i == 0 ? 0 : edgeWidth)
												+ panelRect.left,
										r.top ));
					}
					
					if ( hasBottom ) {
						rect.set(
								dividerInset + (i == 0 ? 0 : edgeWidth),
								r.bottom,
								panelRect.width() - dividerInset - (i == 1 ? 0 : edgeWidth),
								r.bottom + 1 );
						rect.offset(panelRect.left, 0);
						region.op(rect, Region.Op.UNION);
	
						bitmap_panel.get(i).add(dividerBitmap);
						bitmap_panelPosition.get(i).add(
								new Offset(
										dividerInset + (i == 0 ? 0 : edgeWidth)
												+ panelRect.left,
										r.bottom ));
					}
				}
			}

			// some mini rectangles!
			// What's the proportion from "button rectangle" to "mini button
			// rectangle?"
			int miniMaxWidth = (int) ((rect_panelLeft.width() - edgeWidth) * 0.7f);
			int miniMaxHeight = (int) (rect_panelRight.width() * 0.7f);
			if ( rect_touchable_button[INDEX_BUTTON_L] != null )
				miniMaxHeight = Math.min(
						(int) (rect_touchable_button[INDEX_BUTTON_L].height() * 0.7f),
						miniMaxHeight);
			if ( rect_touchable_button[INDEX_BUTTON_R] != null )
				miniMaxHeight = Math.min(
						(int) (rect_touchable_button[INDEX_BUTTON_R].height() * 0.7f),
						miniMaxHeight);
			if ( rect_touchable_button[INDEX_BUTTON_CW] != null )
				miniMaxHeight = Math.min(
						(int) (rect_touchable_button[INDEX_BUTTON_CW].height() * 0.7f),
						miniMaxHeight);
			if ( rect_touchable_button[INDEX_BUTTON_CCW] != null )
				miniMaxHeight = Math.min(
						(int) (rect_touchable_button[INDEX_BUTTON_CCW].height() * 0.7f),
						miniMaxHeight);
			if ( rect_touchable_button[INDEX_BUTTON_FLIP] != null )
				miniMaxHeight = Math.min(
						(int) (rect_touchable_button[INDEX_BUTTON_FLIP].height() * 0.7f),
						miniMaxHeight);
			if ( rect_touchable_button[INDEX_BUTTON_D] != null )
				miniMaxHeight = Math.min(
						(int) (rect_touchable_button[INDEX_BUTTON_D].height() * 0.7f),
						miniMaxHeight);
			

			// Get a multiplier; multiply width or height to this value.
			float miniMult = Math.min(
					(float) miniMaxWidth / (float) canvasWidth,
					(float) miniMaxHeight / (float) canvasHeight);

			// this is for the pause-screen thumbnail
			float pauseMiniMult = 0.4f * Math.min(
					(float)canvasWidth / (float)canvasHeight,
					(float)canvasHeight / (float)canvasWidth ) ;
			
			Rect[] rect_miniButton = this.makeButtonThumbnailRects(miniMult, 1) ;
			Rect[] rect_pauseMiniButton = this.makeButtonThumbnailRects(pauseMiniMult, 2) ;
			
				
			// Make little bitmaps for left, right, cw, ccw.
			Bitmap[] miniBitmaps = new Bitmap[rect_touchable_button.length];
			for (int i = 0; i < rect_touchable_button.length; i++) {
				miniBitmaps[i] = Bitmap.createBitmap(
						(int) (canvasWidth * miniMult),
						(int) (canvasHeight * miniMult), Bitmap.Config.RGB_565);
				c = new Canvas(miniBitmaps[i]);
				c.drawColor(mColor[INDEX_PANEL_BACKGROUND]);

				// We draw each mini rect filled with INDEX_MINI_BUTTON_DIAGRAM,
				// except for 'i', which we clip at one pixel in and then draw.
				for (int j = 0; j < rect_miniButton.length; j++) {
					Rect miniR = rect_miniButton[j];
					if ( miniR == null )
						continue ;
					c.save();
					c.clipRect(miniR);
					if (i == j) {
						c.clipRect(miniR.left + 1, miniR.top + 1,
								miniR.right - 1, miniR.bottom - 1,
								Region.Op.DIFFERENCE);
					}
					c.drawColor(mColor[INDEX_MINI_BUTTON_DIAGRAM]);
					c.restore();
				}
			}
			
			
			int inset = (int)Math.ceil( TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7.5f, displayMetrics) );
			int border = (int)Math.ceil( TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, displayMetrics) );
			int spacing = (int)Math.ceil( TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, displayMetrics) );
			
			RectsDrawable rd = new RectsDrawable( getContext() ) ;
			Rect pauseThumbnailBounds = new Rect(
					inset, inset,
					(int) (canvasWidth * pauseMiniMult) + border*2 + spacing*2 + inset,
					(int) (canvasHeight * pauseMiniMult) + border*2 + spacing*2 + inset ) ;
			rd.addRect( RectsDrawable.Type.SOLID,
					mColor[INDEX_MINI_BUTTON_DIAGRAM], 0, pauseThumbnailBounds ) ;
			rd.setRectHasShadow(getContext(), 0, true) ;
			pauseThumbnailBounds.inset(border, border) ;
			rd.addRect( RectsDrawable.Type.SOLID,
					mColor[INDEX_PANEL_BACKGROUND], 0, pauseThumbnailBounds ) ;
			for (int j = 0; j < rect_pauseMiniButton.length; j++) {
				if ( rect_pauseMiniButton[j] == null )
					continue ;
				Rect r = new Rect(rect_pauseMiniButton[j]);
				r.offset(border+spacing+inset, border+spacing+inset);
				
				rd.addRect( RectsDrawable.Type.SOLID,
						mColor[INDEX_MINI_BUTTON_DIAGRAM], 0, r ) ;
			}
			
			rd.setIntrinsicSizeToCenterContent() ;
			drawable_controlsThumbnail = rd ;
			if (handler != null)
				handler.sendEmptyMessage(WHAT_CONTROLS_THUMBNAIL_UPDATE);
			

			// pixels between items and the icon? Let's say 2dp.
			int topMargin = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, (float) 2, getResources()
							.getDisplayMetrics());
			int sideMargin = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, (float) 4, getResources()
							.getDisplayMetrics());
			// here's a color filter
			ColorFilter iconFilter = new ColorMatrixColorFilter(
					new float[] {
							Color.red(mColor[INDEX_MINI_BUTTON_ICON]) / 255f,
							0, 0, 0, 0, 0,
							Color.green(mColor[INDEX_MINI_BUTTON_ICON]) / 255f,
							0, 0, 0, 0, 0,
							Color.blue(mColor[INDEX_MINI_BUTTON_ICON]) / 255f,
							0, 0, 0, 0, 0,
							Color.alpha(mColor[INDEX_MINI_BUTTON_ICON]) / 255f,
							0 });

			// We place "topMargin" pixels between the icon and the miniDiagram,
			// and at least "sideMargin" pixels between the icon and the sides
			// of
			// the panel. If need be, we shrink the bitmap to fit.

			// okeydokey, we have a bitmap for every button.
			Paint iconPaint = new Paint();
			iconPaint.setColorFilter(iconFilter);
			iconPaint.setAntiAlias(true);
			Paint iconPaintWithAlpha = new Paint() ;
			iconPaint.setColorFilter(iconFilter);
			iconPaint.setAntiAlias(true);
			for (int i = 0; i < 2; i++) {
				Rect panelRect;
				if (i == 0) {
					panelRect = rect_panelLeft;
				} else
					panelRect = rect_panelRight;

				for (int j = 0; j < rect_touchable_button.length; j++) {
					Rect r = rect_touchable_button[j];
					Rect miniR = rect_miniButton[j];
					
					if ( r == null || miniR == null )
						continue ;

					if (j != INDEX_BUTTON_L && j != INDEX_BUTTON_R
							&& j != INDEX_BUTTON_CW && j != INDEX_BUTTON_CCW
							&& j != INDEX_BUTTON_FLIP && j != INDEX_BUTTON_D )
						continue;

					if (Rect.intersects(panelRect, r)) {
						// require within at most 3 pixels of sides.
						if ( Math.abs( panelRect.left - r.left ) > 3 && Math.abs( panelRect.right - r.right ) > 3 )
							continue ;
						
						// Where do we draw this? Position halfway down. Get y
						// as top + (height() - totalHeight)/2.
						// Also halfway in. Get x as
						// panel_left + (panel_width() - edgeWidth - imgWidth)/2
						// + (i == 1 ? edgeWidth : 0)
						//
						// Total height is a combination of miniBitmap and the
						// icon
						// drawable.

						int y = r.top
								+ (r.height() - miniBitmaps[j].getHeight()) / 2;
						int x = (panelRect.width() - edgeWidth - miniBitmaps[j]
								.getWidth()) / 2 + (i == 1 ? edgeWidth : 0);

						// These are the positions if drawn with no icon. Shift
						// up to fit a
						// icon below, but we need to know its size.
						Drawable icon = icon_button[j];
						int iconWidth = icon.getIntrinsicWidth(); // works
																	// because
																	// it is a
																	// Bitmap
						int iconHeight = icon.getIntrinsicHeight(); // so this
																	// returns a
																	// positive
																	// value

						// Make sure this fits...
						int scaledIconHeight = Math.min(
								r.height() - miniR.height() - topMargin * 3,
								iconHeight);
						int scaledIconWidth = Math.min(r.width() - edgeWidth
								- sideMargin * 2, iconWidth);
						// if we reduced the size, try to keep it square.
						float heightProp = scaledIconHeight
								/ (float) iconHeight;
						float widthProp = scaledIconWidth / (float) iconWidth;

						heightProp = widthProp = Math
								.min(heightProp, widthProp);

						iconWidth *= widthProp;
						iconHeight *= heightProp;
						
						int shiftedY = y - (iconHeight + 2 * topMargin) / 2 ;
						
						if ( shiftedY < r.top ) {
							// shifted up too high.  Just use the miniRect bitmap
							// without the "what this button does" icon.
							rect.set(x + panelRect.left, y,
									x + miniBitmaps[j].getWidth() + panelRect.left,
									y + miniBitmaps[j].getHeight());
							region.op(rect, Region.Op.UNION);

							// Store as a bitmap
							bitmap_panel.get(i).add(miniBitmaps[j]);
							bitmap_panelPosition.get(i).add(
									new Offset(x + panelRect.left, y));
						}
						else {
							// shift the mini bitmap up to provide room.
							y = shiftedY ;
	
							// Here's where the bitmap goes.
							rect.set(x + panelRect.left, y,
									x + miniBitmaps[j].getWidth() + panelRect.left,
									y + miniBitmaps[j].getHeight());
							region.op(rect, Region.Op.UNION);
	
							// Store as a bitmap
							bitmap_panel.get(i).add(miniBitmaps[j]);
							bitmap_panelPosition.get(i).add(
									new Offset(x + panelRect.left, y));
	
							// The icon is position slightly below
							x = (panelRect.width() - edgeWidth - iconWidth) / 2
									+ (i == 1 ? edgeWidth : 0);
							y += miniBitmaps[j].getHeight() + topMargin;
							rect.set(x + panelRect.left, y, x + iconWidth
									+ panelRect.left, y + iconHeight);
							region.op(rect, Region.Op.UNION);
	
							// Store as a bitmap
							Bitmap coloredIcon = Bitmap.createBitmap(iconWidth,
									iconHeight, Bitmap.Config.RGB_565);
							c = new Canvas(coloredIcon);
							c.drawColor(mColor[INDEX_PANEL_BACKGROUND]);
							rect.set(0, 0, iconWidth, iconHeight);
							c.drawBitmap(((BitmapDrawable) icon).getBitmap(), null,
									rect, iconPaint);
							bitmap_panel.get(i).add(coloredIcon);
							bitmap_panelPosition.get(i).add(
									new Offset(x + panelRect.left, y));
						}
					}
				}
			}

			int drawRegionMargin = getContext().getResources()
					.getDimensionPixelOffset(R.dimen.margin_game_drawable);

			// Set the bounds and color filter for the reserve icon.
			for (int i = 0; i < 2; i++) {
				Rect panelRect;
				if (i == 0) {
					panelRect = rect_panelLeft;
				} else
					panelRect = rect_panelRight;

				Rect r = rect_touchable_button[INDEX_BUTTON_RESERVE];
				
				if ( r == null )
					continue ;

				if (Rect.intersects(panelRect, r)) {
					// Find the right height/width for this...
					Drawable icon = game[0].hasSpecialReserveAttack()
							? icon_button_alt[INDEX_BUTTON_RESERVE]
							: icon_button[INDEX_BUTTON_RESERVE] ;
					int iconWidth = icon.getIntrinsicWidth(); // works because
																// it is a
																// Bitmap
					int iconHeight = icon.getIntrinsicHeight(); // so this
																// returns a
																// positive
																// value

					// Make sure this fits...
					int scaledIconHeight = Math.min(r.height() - topMargin * 2,
							iconHeight);
					int scaledIconWidth = Math.min(r.width() - edgeWidth
							- sideMargin * 2, iconWidth);
					// if we reduced the size, try to keep it square.
					float heightProp = scaledIconHeight / (float) iconHeight;
					float widthProp = scaledIconWidth / (float) iconWidth;

					iconWidth *= widthProp;
					iconHeight *= heightProp;

					// Try two positions: right below the reserve draw area
					// (separated by topMargin) and just above the 'r's bottom
					// (also separated by topMargin). Keep the higher one.

					// position x centered, y just above the bottom.
					int y = Math.min(r.bottom - iconHeight - topMargin,
							rect_drawRegion[INDEX_DRAW_REGION_RESERVE].bottom
									+ topMargin);
					int x = (panelRect.width() - edgeWidth - iconWidth) / 2
							+ (i == 1 ? edgeWidth : 0) + panelRect.left;

					// DON'T INCLUDE BOUNDS IN REGION

					// Store as a bitmap twice - one for can-use-reserve, one otherwise.
					Bitmap coloredIcon = Bitmap.createBitmap(iconWidth,
							iconHeight, Bitmap.Config.RGB_565);
					c = new Canvas(coloredIcon);
					c.drawColor(mColor[INDEX_PANEL_BACKGROUND]);
					rect.set(0, 0, iconWidth, iconHeight);
					c.drawBitmap(((BitmapDrawable) icon).getBitmap(), null,
							rect, iconPaint);
					bitmap_over.add(coloredIcon);
					bitmap_overPosition.add(new Offset(x, y));
					bitmap_overFlag.add(Integer.valueOf(BITMAP_FLAG_CAN_USE_RESERVE)) ;
					
					// we apply additional transparency.  Do this by drawing twice:
					coloredIcon = Bitmap.createBitmap(iconWidth,
							iconHeight, Bitmap.Config.RGB_565);
					c = new Canvas(coloredIcon);
					c.drawColor(mColor[INDEX_PANEL_BACKGROUND]);
					rect.set(0, 0, iconWidth, iconHeight);
					iconPaintWithAlpha.setAlpha( 128 ) ;
					c.drawBitmap(((BitmapDrawable) icon).getBitmap(), null,
							rect, iconPaintWithAlpha);
					bitmap_over.add(coloredIcon);
					bitmap_overPosition.add(new Offset(x, y));
					bitmap_overFlag.add(Integer.valueOf(BITMAP_FLAG_CAN_USE_RESERVE | BITMAP_FLAG_NEGATE)) ;
					

					// reserve piece rect! We want 'topMargin' pixels between y
					// and
					// the draw region bottom. Get the difference...
					int diff = (y - topMargin) - rect_drawRegion[INDEX_DRAW_REGION_RESERVE].bottom;
					// If this is negative, we need to move up.
					if (diff < 0) {
						// move up...
						int dY = Math.min(-diff,
								rect_drawRegion[INDEX_DRAW_REGION_RESERVE].top
										- (r.top + drawRegionMargin));
						if (dY > 0) {
							// move up this far...
							diff += dY;
							rect_drawRegion[INDEX_DRAW_REGION_RESERVE].offset(
									0, -dY);
						}
					}
					if (diff < 0) {
						// now move up the bottom to make up the difference.
						rect_drawRegion[INDEX_DRAW_REGION_RESERVE].bottom += diff;
					}
				}
			}

			// Draw the word "NEXT."
			Rect r = rect_drawRegion[INDEX_DRAW_REGION_NEXT];
			boolean left = rect_drawRegion[INDEX_DRAW_REGION_NEXT].left < this.rect_panelLeft.right ;
			int bottom = canvasHeight;
			int topFromNext = r.bottom;
			for (int j = 0; j < rect_touchable_button.length; j++) {
				Rect rb = rect_touchable_button[j];
				if (rb != null) {
					// Test for left panel.
					if ( left && this.rect_panelLeft.left <= rb.left
							&& rb.left < this.rect_panelLeft.right) {
						bottom = Math.min(bottom, rb.top);
					} else if ( !left && this.rect_panelRight.right >= rb.right
							&& rb.right > this.rect_panelRight.left) {
						bottom = Math.min(bottom, rb.top);
					}
				}
			}
			// Paint the text...
			String text = res.getString(R.string.game_interface_next);
			Paint p = new Paint();
			p.setAntiAlias(true);
			p.setDither(true);
			p.setColor(res
					.getColor(R.color.game_interface_score_text_color_stable)); // match
																				// score
																				// text
																				// color
			if ( scoreDrawer[0] != null ) {
				p.setTextSize(this.scoreDrawer[0].titleSize()); 						// match
																						// score
																						// size
			}
			else
				p.setTextSize(res.getDimension(R.dimen.game_interface_score_header_text_size)) ;
			p.setTextAlign(Paint.Align.CENTER);
			// try to center. What are the bounds?
			Rect bounds = new Rect();
			Rect drawableBounds = new Rect();
			p.getTextBounds(text, 0, text.length(), bounds);
			p.getTextBounds(text, 0, text.length(), drawableBounds);

			int x = left
					? (rect_panelLeft.width() - this.mSize[SIZE_INDEX_TOTAL_EDGE_WIDTH]) / 2
					: rect_panelRight.left + (rect_panelRight.width()) / 2 ;
			int y = Math.min(bottom - topMargin - Math.abs(bounds.bottom),
					topFromNext - Math.abs(bounds.top) + topMargin);

			bottom = y + bounds.top - topMargin;
			// offset to put the CENTER X at x.
			bounds.offset(x - bounds.width() / 2, y);
			bounds.inset(-1, -1);

			// DONT INCLUDE IN REGION

			// make a bitmap to do this
			Bitmap textBitmap = Bitmap.createBitmap(bounds.width(),
					bounds.height(), Bitmap.Config.RGB_565);
			x -= left ? rect_panelLeft.left : rect_panelRight.left ;
			y -= left ? rect_panelLeft.top : rect_panelRight.top ;
			Canvas textCanvas = new Canvas(textBitmap);
			textCanvas.drawColor(this.mColor[INDEX_PANEL_BACKGROUND]);
			textCanvas.drawText(text, drawableBounds.centerX(),
					-drawableBounds.top, p);

			// placement of the word "NEXT"
			bitmap_over.add(textBitmap);
			bitmap_overPosition.add(new Offset(bounds.left, bounds.top));
			bitmap_overFlag.add(BITMAP_FLAGS_ALWAYS) ;

			// Try moving NEXT up, then shrinking.
			int diff = bottom - r.bottom;
			// If this is negative, we need to move up.
			if (diff < 0) {
				// move up...
				int dY = Math.min(-diff, r.top
						- (rect_panelLeft.top + drawRegionMargin));
				if (dY > 0) {
					// move up this far...
					diff += dY;
					rect_drawRegion[INDEX_DRAW_REGION_NEXT].offset(0, -dY);
				}
			}
			if (diff < 0) {
				// now move up the bottom to make up the difference.
				rect_drawRegion[INDEX_DRAW_REGION_NEXT].bottom += diff;
			}
			
			
			// We have a slightly different approach when the opponent button
			// is pressed.  If we use an alternate panel, we display no panel elements,
			// just the alternative: a series of numbers.  We use "player numbers,"
			// which are player slot + 1, displayed in order top-to-bottom.
			// The default displayed game is omitted from this list.
			if ( displayAlternatePanelForOpponent ) {
				// we evenly divide the space for the number of players.
				// Paint the text...
				text_panelOpponentPressedNumbersActivePaint = new Paint();
				text_panelOpponentPressedNumbersActivePaint.setAntiAlias(true);
				text_panelOpponentPressedNumbersActivePaint.setDither(true);
				
				// match the score color and size (same as "next")
				text_panelOpponentPressedNumbersActivePaint.setColor(res
						.getColor(R.color.game_interface_player_number_text_color)); 
				text_panelOpponentPressedNumbersActivePaint.setTextSize(res.getDimension(R.dimen.game_interface_player_slide_text_size)) ;
				text_panelOpponentPressedNumbersActivePaint.setTextAlign(Paint.Align.CENTER);
				
				text_panelOpponentPressedNumbersInactivePaint = new Paint(
						text_panelOpponentPressedNumbersActivePaint ) ;
				text_panelOpponentPressedNumbersInactivePaint.setAlpha(96) ;
				
				// try to center. What are the bounds?
				bounds = new Rect();
				drawableBounds = new Rect();
				
				left = rect_drawRegion[INDEX_DRAW_REGION_NEXT].left < this.rect_panelLeft.right ;
				
				x = left
						? (rect_panelLeft.width() - this.mSize[SIZE_INDEX_TOTAL_EDGE_WIDTH]) / 2
						: rect_panelRight.left + (rect_panelRight.width()) / 2 ;
				
				int rectIndex = -1 ;
				for ( int slot = 0; slot < numPlayers; slot++ ) {
					if ( slot == this.defaultDisplayedGame ) {
						continue ;
					}
					rectIndex++ ;
					
					text = "" + (slot + 1) ; 
					
					text_panelOpponentPressedNumbersActivePaint.getTextBounds(text, 0, text.length(), bounds);
					text_panelOpponentPressedNumbersActivePaint.getTextBounds(text, 0, text.length(), drawableBounds);
					
					// height is midpoint in onscreen rect.
					y = (rect_opponentSelectionRegion[rectIndex].top
							+ rect_opponentSelectionRegion[rectIndex].bottom) / 2 ;
					// shift up or down depending on where the center of the text bounds
					// is.
					y -= bounds.centerY() ;
					
					// that's it.
					this.text_panelOpponentPressedNumbers.add(text) ;
					this.text_panelOpponentPressedNumbersIndex.add(Integer.valueOf(slot)) ;
					this.text_panelOpponentPressedNumbersPosition.add(new Offset(x,y)) ;
				}
				
				rectIndex = -1 ;
				for ( int slot = 0; slot < numPlayers; slot++ ) {
					if ( slot == this.defaultDisplayedGame ) {
						continue ;
					}
					rectIndex++ ;
					
					if ( rectIndex > 0 ) {
						// put a divider at the top of this rect, within the 
						// appropriate panel.
						Rect panelRect = left ? this.rect_panelLeft : this.rect_panelRight ;
						x = dividerInset + (left ? 0 : edgeWidth) + panelRect.left ;
						y = rect_opponentSelectionRegion[rectIndex].top ;
						bitmap_panelOpponentPressed.add(dividerBitmap);
						bitmap_panelOpponentPressedPosition.add( new Offset( x, y ) );
					}
				}
			}
			
			// Return the region
			return region;
		}
		
		
		private void resetPanelOpponentPressedNumbersText() {
			int rectIndex = -1 ;
			String text ;
			if ( text_panelOpponentPressedNumbersActivePaint != null && text_panelOpponentPressedNumbers != null && text_panelOpponentPressedNumbers.size() > 0 ) {
				this.text_panelOpponentPressedNumbers.clear() ;
				this.text_panelOpponentPressedNumbersIndex.clear() ;
				for ( int slot = 0; slot < numPlayers; slot++ ) {
					if ( slot == this.defaultDisplayedGame ) {
						continue ;
					}
					rectIndex++ ;
					
					text = "" + (slot + 1) ; 
					
					// that's it.
					this.text_panelOpponentPressedNumbers.add(text) ;
					this.text_panelOpponentPressedNumbersIndex.add(Integer.valueOf(slot)) ;
				}
			}
		}
		
		
		
		/**
		 * Makes and returns, indexed by INDEX_BUTTON_*, an array of 
		 * Rects representing a "downscaled thumbnail" of 
		 * rect_touchable_button.  Also "cleans up" edges to make sure
		 * they match.
		 * 
		 * @param scale
		 * @return
		 */
		private Rect [] makeButtonThumbnailRects( float scale, int insetInnerSidesBy ) {
			Rect[] rect_miniButton = new Rect[rect_touchable_button.length];
			for (int i = 0; i < rect_touchable_button.length; i++) {
				Rect r = rect_touchable_button[i];
				if ( r == null )
					continue ;
				Rect newR = new Rect((int) (r.left * scale),
						(int) (r.top * scale), (int) (r.right * scale),
						(int) (r.bottom * scale));
				rect_miniButton[i] = newR;
			}

			int maxRight = 0;
			for (int i = 0; i < rect_miniButton.length; i++)
				if ( rect_miniButton[i] != null )
					maxRight = Math.max(maxRight, rect_miniButton[i].right);

			// First: a very simple "size matching" heuristic. Make sure
			// L, R have the same width, along with CW and CCW.
			if ( rect_miniButton[INDEX_BUTTON_L] != null && rect_miniButton[INDEX_BUTTON_R] != null ) {
	 			int diff = rect_miniButton[INDEX_BUTTON_L].width()
						- rect_miniButton[INDEX_BUTTON_R].width();
				if (diff > 0)
					rect_miniButton[INDEX_BUTTON_R].left -= diff;
				else if (diff < 0)
					rect_miniButton[INDEX_BUTTON_L].right -= diff; // subtract a
																	// negative ->
																	// shift right
			}
			if ( rect_miniButton[INDEX_BUTTON_CW] != null && rect_miniButton[INDEX_BUTTON_CCW] != null ) {
				int diff = rect_miniButton[INDEX_BUTTON_CCW].width()
						- rect_miniButton[INDEX_BUTTON_CW].width();
				if (diff > 0)
					rect_miniButton[INDEX_BUTTON_CW].left -= diff;
				else if (diff < 0)
					rect_miniButton[INDEX_BUTTON_CCW].right -= diff; // subtract a
																		// negative
																		// -> shift
																		// right
			}
			// Align the CW/CCW buttons and the "down" button.
			boolean move_inline_down = rect_miniButton[INDEX_BUTTON_L] != null
					&& rect_miniButton[INDEX_BUTTON_R] != null
					&& rect_miniButton[INDEX_BUTTON_D] != null
					&& Math.abs( rect_miniButton[INDEX_BUTTON_R].bottom - rect_miniButton[INDEX_BUTTON_D].bottom ) < 3 ;
			boolean turn_inline_down = rect_miniButton[INDEX_BUTTON_CCW] != null
					&& rect_miniButton[INDEX_BUTTON_CW] != null
					&& rect_miniButton[INDEX_BUTTON_D] != null
					&& Math.abs( rect_miniButton[INDEX_BUTTON_CCW].bottom - rect_miniButton[INDEX_BUTTON_D].bottom ) < 3 ;
			boolean move_inline_flip = rect_miniButton[INDEX_BUTTON_L] != null
					&& rect_miniButton[INDEX_BUTTON_R] != null
					&& rect_miniButton[INDEX_BUTTON_FLIP] != null
					&& Math.abs( rect_miniButton[INDEX_BUTTON_R].bottom - rect_miniButton[INDEX_BUTTON_FLIP].bottom ) < 3 ;
			boolean turn_inline_flip = rect_miniButton[INDEX_BUTTON_CCW] != null
					&& rect_miniButton[INDEX_BUTTON_CW] != null
					&& rect_miniButton[INDEX_BUTTON_FLIP] != null
					&& Math.abs( rect_miniButton[INDEX_BUTTON_CCW].bottom - rect_miniButton[INDEX_BUTTON_FLIP].bottom ) < 3 ;
	
	
			if ( !turn_inline_down && !turn_inline_flip ) {
				if ( rect_miniButton[INDEX_BUTTON_CW] != null && rect_miniButton[INDEX_BUTTON_CCW] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_CW].left - rect_miniButton[INDEX_BUTTON_CCW].right ) < 3 )
					rect_miniButton[INDEX_BUTTON_CW].left = rect_miniButton[INDEX_BUTTON_CCW].right;
			}
			if ( !move_inline_down && !move_inline_flip ) {
				if ( rect_miniButton[INDEX_BUTTON_R] != null && rect_miniButton[INDEX_BUTTON_L] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_R].left - rect_miniButton[INDEX_BUTTON_L].right ) < 3 )
					rect_miniButton[INDEX_BUTTON_R].left = rect_miniButton[INDEX_BUTTON_L].right;
			}
			if ( move_inline_down ) {
				if ( rect_miniButton[INDEX_BUTTON_D] != null && rect_miniButton[INDEX_BUTTON_L] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_D].left - rect_miniButton[INDEX_BUTTON_L].right ) < 3)
					rect_miniButton[INDEX_BUTTON_D].left = rect_miniButton[INDEX_BUTTON_L].right;
				if ( rect_miniButton[INDEX_BUTTON_D] != null && rect_miniButton[INDEX_BUTTON_R] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_D].right - rect_miniButton[INDEX_BUTTON_R].left ) < 3 )
					rect_miniButton[INDEX_BUTTON_D].right = rect_miniButton[INDEX_BUTTON_R].left;
			}
			if ( turn_inline_down ) {
				if ( rect_miniButton[INDEX_BUTTON_D] != null && rect_miniButton[INDEX_BUTTON_CCW] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_D].left - rect_miniButton[INDEX_BUTTON_CCW].right ) < 3)
					rect_miniButton[INDEX_BUTTON_D].left = rect_miniButton[INDEX_BUTTON_CCW].right;
				if ( rect_miniButton[INDEX_BUTTON_D] != null && rect_miniButton[INDEX_BUTTON_CW] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_D].right - rect_miniButton[INDEX_BUTTON_CW].left ) < 3 )
					rect_miniButton[INDEX_BUTTON_D].right = rect_miniButton[INDEX_BUTTON_CW].left;
			}
			if ( move_inline_flip ) {
				if ( rect_miniButton[INDEX_BUTTON_FLIP] != null && rect_miniButton[INDEX_BUTTON_L] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_FLIP].left - rect_miniButton[INDEX_BUTTON_L].right ) < 3)
					rect_miniButton[INDEX_BUTTON_FLIP].left = rect_miniButton[INDEX_BUTTON_L].right;
				if ( rect_miniButton[INDEX_BUTTON_FLIP] != null && rect_miniButton[INDEX_BUTTON_R] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_FLIP].right - rect_miniButton[INDEX_BUTTON_R].left ) < 3 )
					rect_miniButton[INDEX_BUTTON_FLIP].right = rect_miniButton[INDEX_BUTTON_R].left;
			}
			if ( turn_inline_flip ) {
				if ( rect_miniButton[INDEX_BUTTON_FLIP] != null && rect_miniButton[INDEX_BUTTON_CCW] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_FLIP].left - rect_miniButton[INDEX_BUTTON_CCW].right ) < 3)
					rect_miniButton[INDEX_BUTTON_FLIP].left = rect_miniButton[INDEX_BUTTON_CCW].right;
				if ( rect_miniButton[INDEX_BUTTON_FLIP] != null && rect_miniButton[INDEX_BUTTON_CW] != null
						&& Math.abs( rect_miniButton[INDEX_BUTTON_FLIP].right - rect_miniButton[INDEX_BUTTON_CW].left ) < 3 )
					rect_miniButton[INDEX_BUTTON_FLIP].right = rect_miniButton[INDEX_BUTTON_CW].left;
			}
			// CW and CCW or L and R align left/right. Use this vertical line to align
			// RESERVE, SCORE, OPPONENT.
			Rect[] rects;
			if (INDEX_BUTTON_OPPONENT < rect_miniButton.length)
				rects = new Rect[] { rect_miniButton[INDEX_BUTTON_RESERVE],
						rect_miniButton[INDEX_BUTTON_SCORE],
						rect_miniButton[INDEX_BUTTON_OPPONENT] };
			else
				rects = new Rect[] { rect_miniButton[INDEX_BUTTON_RESERVE],
						rect_miniButton[INDEX_BUTTON_SCORE] };

			for (int i = 0; i < rects.length; i++) {
				Rect r = rects[i] ;
				if (r.left > 2 && ( rect_miniButton[INDEX_BUTTON_CW] != null || rect_miniButton[INDEX_BUTTON_R] != null ) )
					r.left = Math.min(
							rect_miniButton[INDEX_BUTTON_CW] == null ? Integer.MAX_VALUE : rect_miniButton[INDEX_BUTTON_CW].left,
							rect_miniButton[INDEX_BUTTON_R] == null ? Integer.MAX_VALUE : rect_miniButton[INDEX_BUTTON_R].left );
				else if ( rect_miniButton[INDEX_BUTTON_CCW] != null || rect_miniButton[INDEX_BUTTON_L] != null )
					r.right = Math.max(
							rect_miniButton[INDEX_BUTTON_CCW] == null ? 0 : rect_miniButton[INDEX_BUTTON_CCW].right,
							rect_miniButton[INDEX_BUTTON_L] == null ? 0 : rect_miniButton[INDEX_BUTTON_L].right ) ;
			}

			// Attempt to align heights. Raise/lower bottoms.
			for (int i = 0; i < rect_miniButton.length; i++) {
				Rect r1 = rect_miniButton[i];
				if ( r1 == null )
					continue ;
				
				for (int j = 0; j < rect_miniButton.length; j++) {
					Rect r2 = rect_miniButton[j];
					if ( r2 == null )
						continue ;

					if (Math.abs(r1.bottom - r2.top) == 1) {
						if (r1.bottom < r2.top)
							r1.bottom += 1;
						else if (r1.bottom > r2.top)
							r1.bottom -= 1;
					}
				}
			}

			// shrink. For now, shrink all sides that aren't against the edge.
			for (int i = 0; i < rect_touchable_button.length; i++) {
				Rect r = rect_touchable_button[i];
				Rect miniR = rect_miniButton[i];
				
				if ( r == null || miniR == null )
					continue ;

				if (r.left != 0)
					miniR.left += insetInnerSidesBy;
				if (r.right != canvasWidth)
					miniR.right -= insetInnerSidesBy;
				if (r.top != 0)
					miniR.top += insetInnerSidesBy;
				if (r.bottom != canvasHeight)
					miniR.bottom -= insetInnerSidesBy;
			}
			
			return rect_miniButton ;
		}
		
		

		private void releaseUIElements() {

			ArrayList<ArrayList<Bitmap>> bals = new ArrayList<ArrayList<Bitmap>>();
			bals.add(this.bitmap_panelFrame); 
			bals.add(this.bitmap_panelLeft);
			bals.add(this.bitmap_panelRight);
			bals.add(this.bitmap_panelScore);
			bals.add(this.bitmap_over);
			bals.add(this.bitmap_panelOpponentPressed) ;

			Iterator<ArrayList<Bitmap>> iter = bals.iterator();
			for (; iter.hasNext();) {
				ArrayList<Bitmap> al = iter.next();
				if (al != null) {
					Iterator<Bitmap> b_iter = al.iterator();
					for (; b_iter.hasNext();) {
						Bitmap b = b_iter.next();
						if (b != null && !b.isRecycled())
							b.recycle();
						b_iter.remove();
					}
				}
			}

			bitmap_panelFrame = null ;
			bitmap_panelLeft = null;
			bitmap_panelRight = null;
			bitmap_panelScore = null;
			bitmap_over = null;
			bitmap_panelOpponentPressed = null ;
			bitmap_panelFramePosition = null;
			bitmap_panelLeftPosition = null;
			bitmap_panelRightPosition = null;
			bitmap_panelScorePosition = null;
			bitmap_overPosition = null;
			bitmap_panelOpponentPressedPosition = null ;
		}

		private void reallocateSpecialtyDrawObjects() {
			Log.d(TAG, "reallocateSpecialtyDrawObjects") ;
			if (this.view_invisibleControls == null || gameMode == -1)
				return;

			// Log.d(TAG,
			// "START   Synchronization in reallocateSpecialtyDrawObjects") ;
			synchronized (surfaceHolder) {
				if (mColorScheme != null) {
					// Allocate 'scoreDrawer's.
					Resources res = getContext().getResources();
					if (ginfo != null) {

						// 6/19/12: we now allow the score drawer and tooltips
						// to extend beyond the
						// limit of the "score button" height. We still want to
						// maintain the visual
						// consistency of the score covering the tooltips (it is
						// okay if the tooltip
						// rect is smaller than the score rect).

						// First: find the maximum height required by both.
						Rect fullRect = new Rect(0, 0, rect_panelScore.width(),
								canvasHeight);
						// First size and position the DetailedScoreDrawer.
						DetailedScoreDrawer dsd = new DetailedScoreDrawer(null,
								res, fullRect, colorScheme,
								screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE);
						int heightNeeded = Math.max(dsd.height(),
								rect_panelScore.height());
						if (heightNeeded > canvasHeight) {
							Log.d(TAG,
									"WARNING WARNING WARNING: Height needed for DetailedScoreDrawer exceeds canvas height!");
							heightNeeded = canvasHeight;
						}
						// score drawer position: if possible, we prefer to
						// center-align
						// with the score button. However, if this extends
						// beyond the top,
						// we center within the canvas instead.
						int centerLine = rect_panelScore.height() / 2
								+ rect_panelScore.top;
						if (heightNeeded / 2 + 6 > centerLine) {
							Log.d(TAG,
									"centering score drawer in screen with height "
											+ heightNeeded);
							// 6 pixel margin for safety. This exceeds the top
							// of the
							// screen, so center WITHIN the screen instead.
							int horizMargin = (canvasHeight - heightNeeded) / 2;
							// left/right are already set, so just do top and
							// bottom.
							rect_panelScore.top = horizMargin;
							rect_panelScore.bottom = horizMargin + heightNeeded;
						} else if (heightNeeded > rect_panelScore.height()) {
							Log.d(TAG,
									"expanding score drawer in screen with height "
											+ heightNeeded);
							Log.d(TAG, "score drawer was " + rect_panelScore
									+ " with center line " + centerLine);
							// Center to the center line - we have the room for
							// it.
							rect_panelScore.top = centerLine - heightNeeded / 2;
							rect_panelScore.bottom = rect_panelScore.top
									+ heightNeeded;
							Log.d(TAG, "score drawer is now " + rect_panelScore);
						}

						// Now size the tooltip drawer centered within this
						// rectangle.
						// However, we also impose a hard maximum.
						rect_tooltips.set(rect_panelScore);
						// TODO: inset vertically for a hard maximum.

						// Now that we have new bounds for everything, make our
						// drawers.
						Log.d(TAG, "allocate ScoreDrawers") ;
						for (int i = 0; i < ginfo.length; i++) {
							if (ginfo[i] != null && !GlobalTestSettings.GAME_OMIT_SCORE_DRAWER ) {
								if ( !GlobalTestSettings.GAME_OMIT_SCORE_DRAWER_ABBREVIATED ) {
									scoreDrawer[i] = new AbbreviatedScoreDrawer(
											ginfo[i],
											res,
											this.rect_drawRegion[INDEX_DRAW_REGION_SCORE],
											screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE);
									if ( colorScheme != null ) {
										scoreDrawer[i].setColorScheme(colorScheme);
									}
								}
								if ( !GlobalTestSettings.GAME_OMIT_SCORE_DRAWER_DETAILED ) {
									scoreDrawerDetailed[i] = new DetailedScoreDrawer(
											ginfo[i], res, this.rect_panelScore, colorScheme,
											screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE);
									if (colorScheme != null) {
										scoreDrawerDetailed[i].setColorScheme(colorScheme);
									}
								}
							}
						}
						
						if ( !GlobalTestSettings.GAME_DISABLE_TOOLTIP_DRAWER ) {
							tooltipDrawer = new TooltipDrawer(res,
									this.rect_tooltips, screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE);
							// for now, use "foreground" and "background" always.
							// to use "red / blue" when appropriate, remove
							// "false &&"
							tooltipDrawer
									.setLabelColors(false
											/* && mColorScheme != null
											&& mColorScheme.getScheme() == ColorScheme.COLOR_SCHEME_STANDARD */ );
							setPolicy(tooltipDrawer, this.pieceTips);
						}
						
						if ( this.numPlayers > 1 ) {
							playerStatusDrawer = new PlayerStatusDrawer( res, this.rect_gameArea, this.rect_panelScore, playerNames ) ;
							playerStatusDrawer.setDisplayedPlayer(defaultDisplayedGame, true) ;
							playerStatusDrawer.setLocalPlayer(defaultDisplayedGame) ;
						} else {
							playerStatusDrawer = null ;
						}
					}
				}
			}
			// Log.d(TAG,
			// "-END-   Synchronization in reallocateSpecialtyDrawObjects") ;
		}

		private void reallocateAdditionalDrawSettings() {
			// Log.d(TAG, "reallocateSpecialtyDrawObjects") ;
			if (this.view_invisibleControls == null || gameMode == -1)
				return;

			// Log.d(TAG,
			// "START   Synchronization in reallocateSpecialtyDrawObjects") ;
			synchronized (surfaceHolder) {
				if (mColorScheme != null) {
					int maxWidth = blockFieldDrawSettings == null ? Integer.MAX_VALUE
							: blockFieldDrawSettings.getSize_blockWidth();
					int maxHeight = blockFieldDrawSettings == null ? Integer.MAX_VALUE
							: blockFieldDrawSettings.getSize_blockHeight();

					nextPieceDrawSettings = newPiecePreviewDrawSettings(
							rect_drawRegion[INDEX_DRAW_REGION_NEXT], maxWidth,
							maxHeight);
					if (nextPieceBlockDrawer == null)
						nextPieceBlockDrawer = BlockDrawer.newPreAllocatedBlockDrawer(getContext(), null,
								null);

					// no bigger than nextPiece
					maxWidth = Math.min(maxWidth,
							nextPieceDrawSettings.getSize_blockWidth());
					maxHeight = Math.min(maxHeight,
							nextPieceDrawSettings.getSize_blockHeight());

					reservePieceDrawSettings = newPiecePreviewDrawSettings(
							rect_drawRegion[INDEX_DRAW_REGION_RESERVE],
							maxWidth, maxHeight);
					if (reservePieceBlockDrawer == null)
						reservePieceBlockDrawer = BlockDrawer.newPreAllocatedBlockDrawer(getContext(), null,
								null);

					// Mini draw settings and block drawer
					if (numPlayers > 1 || GlobalTestSettings.GAME_SELF_THUMBNAIL) {
						Log.d(TAG, "allocating blockDrawerMini");
						blockFieldMiniDrawSettings = new DrawSettings(
								this.rect_drawRegion[INDEX_DRAW_REGION_OPPONENT],
								ROWS,
								COLS,
								displayedRows,
								DrawSettings.DRAW_DETAIL_LOW,
								DrawSettings.DRAW_ANIMATIONS_NONE,
								mSkin, getContext());
						blockFieldMiniDrawSettings.behavior_align_vertical = DrawSettings.BEHAVIOR_ALIGN_CENTER_GRID;
						blockFieldMiniDrawSettings.behavior_align_horizontal = DrawSettings.BEHAVIOR_ALIGN_CENTER_GRID;
						blockFieldMiniDrawSettings.setBlockSizesToFit();
						blockFieldMiniDrawSettings.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_BLOCK_FILL;
						if (blockDrawerMini == null)
							blockDrawerMini = new BlockDrawer(getContext(), null,
									null);
					}
				}
			}
		}

		private void setPolicy(TooltipDrawer td, int pieceTips) {
			td.clearPolicies();
			switch (pieceTips) {
			case PIECE_TIPS_OFTEN:
				// force "foreground/background" instead of "red/blue."
				td.setLabelColors(false
						/* && mColorScheme != null
						&& mColorScheme.getScheme() == ColorScheme.COLOR_SCHEME_STANDARD */);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.S0);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.S1);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.SS);

				td.addPolicy(TooltipDrawer.POLICY_EVERY_QC, QCombinations.ST);
				td.addPolicy(TooltipDrawer.POLICY_EVERY_QC, QCombinations.SL);
				td.addPolicy(TooltipDrawer.POLICY_EVERY_QC, QCombinations.F0);
				td.addPolicy(TooltipDrawer.POLICY_EVERY_QC, QCombinations.UL);
				td.addPolicy(TooltipDrawer.POLICY_EVERY_QC, QCombinations.PUSH_DOWN) ;
				break;

			case PIECE_TIPS_OCCASIONALLY:
				// force "foreground/background" instead of "red/blue."
				td.setLabelColors(false
						/* && mColorScheme != null
						&& mColorScheme.getScheme() == ColorScheme.COLOR_SCHEME_STANDARD */ );
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.S0);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.S1);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.SS);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.ST);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.SL);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.UL);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.F0);
				td.addPolicy(TooltipDrawer.POLICY_FIRST_QC, QCombinations.PUSH_DOWN) ;

				td.addPolicy(TooltipDrawer.POLICY_AFTER_NON_SUCCESS_QC,
						QCombinations.ST);
				td.addPolicy(TooltipDrawer.POLICY_AFTER_NON_SUCCESS_QC,
						QCombinations.SL);
				td.addPolicy(TooltipDrawer.POLICY_AFTER_NON_SUCCESS_QC,
						QCombinations.F0);
				td.addPolicy(TooltipDrawer.POLICY_AFTER_NON_SUCCESS_QC,
						QCombinations.PUSH_DOWN) ;
				break;
			}
		}

		private DrawSettings newPiecePreviewDrawSettings(Rect drawRegion,
				int maxBlockWidth, int maxBlockHeight) {
			
			int dimMax = 1, dimMin = 1 ;
			if ( game != null ) {
				for ( int i = 0; i < game.length; i++ ) {
					if ( game[i] != null ) {
						// Biggest galaxy is 3x3.
						if ( game[i].hasSpecialReserve() ) {
							dimMax = Math.max( dimMax, 3 ) ;
							dimMin = Math.max( dimMin, 3 ) ;
						}
						
						if ( game[i].hasPentominoes() ) {
							dimMax = Math.max( dimMax, 5 ) ;
							dimMin = Math.max( dimMin, 3 ) ;
						} else if ( game[i].hasTetrominoes() ) {
							dimMax = Math.max( dimMax, 4 ) ;
							dimMin = Math.max( dimMin, 2 ) ;
						} else if ( game[i].hasTrominoes() ) {
							dimMax = Math.max( dimMax, 3 ) ;
							dimMin = Math.max( dimMin, 2 ) ;
						}
						if ( !GameModes.hasRotation(ginfo[i]) ) {
							dimMin = dimMax ;
						}
					}
				}
			}
			
			DrawSettings ds = new DrawSettings(drawRegion, 6, 6, 6,
					drawDetail, DrawSettings.DRAW_ANIMATIONS_NONE,
					mSkin, getContext());

			ds.loadImagesSize = loadImagesSize;
			/*
			if (this.scale > 1) {
				ds.setCanvasTarget(ds.getBlit(), this.scale, drawRegion);
			}
			*/

			ds.behavior_align_horizontal = DrawSettings.BEHAVIOR_ALIGN_CENTER_BLOCKS;
			ds.behavior_align_vertical = DrawSettings.BEHAVIOR_ALIGN_CENTER_BLOCKS;
			if ( piecePreviewMinimumProfile )
				ds.setBlockSizesToFitWithMax(dimMax, dimMin, maxBlockWidth, maxBlockHeight);
			else
				ds.setBlockSizesToFitWithMax(dimMin, dimMax, maxBlockWidth, maxBlockHeight);
			ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_BLOCK_FILL;

			Log.d(TAG, "newPiecePreviewDrawSettings: fit with block size "
					+ ds.getSize_blockWidth() + ", " + ds.getSize_blockHeight()
					+ ", fits within max " + maxBlockWidth + ", "
					+ maxBlockHeight);

			return ds;
		}

		@Override
		public void run() {
			// Log.d(TAG, "run") ;

			Context context = getContext() ;
			if ( context instanceof Activity ) {
				Activity activity = (Activity)context ;
				blockDrawerPreallocated = ((QuantroApplication) activity
						.getApplication()).getBlockDrawerPreallocatedBitmaps(activity);
				blockDrawerLoadIntoSheetBitmap = ((QuantroApplication) activity
						.getApplication()).getBlockDrawerLoadIntoSheetBitmap(activity) ;
				blockDrawerLoadIntoBackgroundBitmap = ((QuantroApplication) activity
						.getApplication()).getBlockDrawerLoadIntoBackgroundBitmap(activity) ;
				activity = null ;
			} else {
				blockDrawerPreallocated = ((QuantroApplication) context
						.getApplicationContext()).getBlockDrawerPreallocatedBitmaps(null);
				blockDrawerLoadIntoSheetBitmap = ((QuantroApplication) context
						.getApplicationContext()).getBlockDrawerLoadIntoSheetBitmap(null) ;
				blockDrawerLoadIntoBackgroundBitmap = ((QuantroApplication) context
						.getApplicationContext()).getBlockDrawerLoadIntoBackgroundBitmap(null) ;
			}
			context = null ;
			
			
			firstUpdate = true;

			boolean changedByTick;

			long timeLastTickEnded = -1;
			long lastTime = -1;
			long timeDifference;

			long[] timeThisTick = new long[NUM_INDICES_TIME];
			long[] timeTotal = new long[NUM_INDICES_TIME];
			long[] timeMax = new long[NUM_INDICES_TIME];
			
			long shuffle_lastTime = mSliceTime[0].getUnpaused() ;

			// Wrap the entire thing in a Try. We ALWAYS want to recycle!
			try {
				while (running) {
					// Log.d(TAG, "run loop") ;
					Canvas c = null;

					if ((newDrawSettings || !blockDrawer.hasDrawSettings())
							&& (!veiled || !recycleToVeil)) {
						try {
							synchronized (surfaceHolder) {
								if ( recycled ) {
									throw new IllegalStateException("This GameView is recycled") ;
								}
								
								// Only proceed if we are not currently working to prerender.
								synchronized ( mPrerendererMutex ) {
									if ( !mPrerenderer.busy() && !mPrerenderCycleWorking ) {
										
										loaded = false;
										handler.sendEmptyMessage(WHAT_START_LOADING);
										// first recycle all...
										blockDrawer.recycle();
										nextPieceBlockDrawer.recycle();
										reservePieceBlockDrawer.recycle();
										if (blockDrawerMini != null)
											blockDrawerMini.recycle();
										
										// do we need new preallocated bitmaps?
										if ( needsNewPreviewPreallocatedBitmaps() ) {
											// even if only one needs to be changed, we recycle and null
											// BOTH of them.  We want to build our set of preallocated bitmaps from
											// the very "bottom" of memory, rather than on top of anything
											// else.
											nextPieceBlockDrawer.receivePreallocatedBitmaps(null) ;
											reservePieceBlockDrawer.receivePreallocatedBitmaps(null) ;
											if ( nextPieceBlockDrawerPreallocated != null )
												nextPieceBlockDrawerPreallocated.recycle() ;
											if ( reservePieceBlockDrawerPreallocated != null )
												reservePieceBlockDrawerPreallocated.recycle() ;
											System.gc() ;
											makePreviewPreallocatedBitmapsIfNeeded() ;
										}
		
										// now load all
										Log.d(TAG,
												"setting and prerendering with region width "
														+ blockFieldDrawSettings
																.getBlitBlockfieldWidth()
														+ ", blit width "
														+ blockFieldDrawSettings.width);
										// HACK HACK HACK: Better to have this passed
										// in! Why is GameView
										// aware of QuantroApplication??
										try {
											if ( recycled ) {
												throw new IllegalStateException("This GameView is recycled") ;
											}
											blockDrawer.receivePreallocatedBitmaps(blockDrawerPreallocated);
											blockDrawer.receiveLoadIntoBitmapSheet(blockDrawerLoadIntoSheetBitmap) ;
											blockDrawer.receiveLoadIntoBitmapBackground(blockDrawerLoadIntoBackgroundBitmap) ;
										} catch ( Exception e ) {
											// any exception and our prerendered bitmaps are invalid.
											blockDrawerPreallocated.reset() ;
											
											throw e ;
										}
										
										// start an asynchronous render.
										newDrawSettings = false ;
										mPrerenderCycleWorking = true ;
										mPrerenderCycleFinished = false ;
										if ( this.mPrerenderer.prerender(getContext(), this, null,
												blockDrawer, blockFieldDrawSettings) ) {
											// success!  We are working.  Don't need to do anything.
										} else {
											// failure... huh?
											Log.d(TAG, "unexpected failure in Prerenderer.  Will retry.") ;
											newDrawSettings = true ;
											mPrerenderCycleWorking = true ;
										}
										
										newBackground = false ;
										shuffle_nextBackground = null ;
										
										// remember, we need to re-draw everything now
										// (can't use efficient blitting!)
										newThumbnailGameBlocksSlice = true;
										changedNextPieceSlice = true;
										changedReservePieceSlice = true;
										
									}
								}
							}
						} catch (Exception e) {
							Log.e(TAG, " ***************************************** Exception caught in 'load graphics' stage") ;
							e.printStackTrace();
							Log.e(TAG,
									"quiting; failed to set draw settings.  It is likely that the user pressed 'back' and we set our Context to 'null' in response");
							running = false;
							return;
						}
					}
					
					if ( newBackground ) {
						blockDrawer.setToImmediately(getContext(), null, background) ;
						shuffle_nextBackground = blockDrawer.getShufflePreloadedBackground() ;
						if ( background.equals(shuffle_nextBackground) )
							shuffle_nextBackground = null; 
						
						newBackground = false ;
						handler.sendMessage(handler.obtainMessage(WHAT_BACKGROUND_CHANGED, shuffle_nextBackground) ) ;
					}

					// because we now load asynchronously, we can't be sure that
					// we're ready to draw when we get here.  Skip this section if
					// we don't plan to draw anything here.
					if ( !this.newDrawSettings && this.mPrerenderCycleFinished && !this.mPrerenderCycleWorking ) {
						try {
							c = surfaceHolder.lockCanvas();
							// There's a strange bug here, where we might lockCanvas
							// after terminating the thread (running is false at
							// this point).
							if (c != null) {
								// empty our time this tick
								for (int i = 0; i < NUM_INDICES_TIME; i++)
									timeThisTick[i] = 0;
	
								// Current time? Time passed?
								currentTime = GlobalTestSettings.getCurrentGameScaledTime() ;
								
								// skip frames while veiling?
								if ( lastTime > -1 ) {
									if ( blockDrawer.veilOnScreen(mSliceTime[displayedGame])
											&& currentTime - lastTime > 250
											&& veiledFramesSkipped < 10 ) {
										blockDrawer.rewindVeil(currentTime - lastTime - 1) ;
										veiledFramesSkipped++ ;
									}
								}
	
								if (timeLastTickEnded > -1)
									timeThisTick[INDEX_TIME_BETWEEN_FRAMES] = currentTime
											- timeLastTickEnded;
	
								if (lastTime == -1) {
									// no game update the first tick
									lastTime = currentTime;
								}
	
								timeDifference = currentTime - lastTime;
								lastTime = currentTime;
	
								// //Log.d(TAG, "trying to draw") ;
								// Try a game update.
								double secondsPassed = ((double) timeDifference) / 1000;
								// Log.d(TAG,
								// "about to enter !paused game advance block") ;
								changedByTick = false;
								try {
									// Log.d(TAG, "tick: " + secondsPassed ) ;
									long timeBefore = GlobalTestSettings.getCurrentGameScaledTime() ;
	
									if (!paused
											&& !blockDrawer
													.veilOnScreen(mSliceTime[displayedGame])) {
										// Tick all games forward.
										for (int i = 0; i < game.length; i++) {
											synchronized (game[i]) {
												if ( game[i].stillPlaying() ) {
													boolean changed = game[i].tick(secondsPassed) ;
													if ( changed ) {
														updateAchievements(i) ;
													}
													changedByTick = changed || changedByTick ;
												}
											}
										}
									} else {
										// drop any queued actions.
										for (int i = 0; i < game.length; i++) {
											synchronized (game[i]) {
												game[i].fakeTick();
											}
										}
									}
									timeThisTick[INDEX_TIME_GAME_UPDATE]
									        = GlobalTestSettings.getCurrentGameScaledTime()
									        - timeBefore;
								} catch (GameSystemException e) {
									Log.e(TAG, " ***************************************** GameSystemException caught when ticking.") ;
									e.printContents();
									e.printStackTrace();
									notifyListenerCanceled();
									return;
								} catch (Exception e) {
									// TODO Auto-generated catch block
									Log.e(TAG, " ***************************************** Exception caught when ticking.") ;
									e.printStackTrace();
									notifyListenerCanceled();
									return;
								}
	
								// Log.d(TAG, "START   Synchronization in run") ;
								long timeBeforeSync = GlobalTestSettings.getCurrentGameScaledTime() ;
								
								try {
									synchronized (surfaceHolder) {
										timeThisTick[INDEX_TIME_SYNC]
										        = GlobalTestSettings.getCurrentGameScaledTime()
												- timeBeforeSync;
	
										boolean detailedScore = controls
												.getButtonPressedByName(name_button[INDEX_BUTTON_SCORE]);
	
										// Log.d(TAG, "update and draw methods") ;
										long timeBefore = GlobalTestSettings.getCurrentGameScaledTime() ;
										getUpdateFromGames();
										timeThisTick[INDEX_TIME_GAME_DATA]
										             = GlobalTestSettings.getCurrentGameScaledTime()
										             - timeBefore;
										
										timeBefore = GlobalTestSettings.getCurrentGameScaledTime() ;
										advanceBlockDrawerSliceTimes( timeDifference ) ;
										timeThisTick[INDEX_TIME_ADVANCE_SLICES]
										             = GlobalTestSettings.getCurrentGameScaledTime()
										             - timeBefore;
	
										timeBefore = GlobalTestSettings.getCurrentGameScaledTime() ;
										drawGameInterface(c, detailedScore,
												timeDifference);
										timeThisTick[INDEX_TIME_DRAW_UI]
										         = GlobalTestSettings.getCurrentGameScaledTime()
										         - timeBefore;
	
										timeBefore = GlobalTestSettings.getCurrentGameScaledTime() ;
										drawNextAndReserveBlocks(c);
										drawOpponentBlocks(c);
										drawBlockFieldsAndUpdateAnimations(c);
										timeThisTick[INDEX_TIME_DRAW_BLOCKS]
										        = GlobalTestSettings.getCurrentGameScaledTime()
										        - timeBefore;
	
										timeBefore = GlobalTestSettings.getCurrentGameScaledTime() ;
										playEventSounds(!firstUpdate);
										timeThisTick[INDEX_TIME_PLAY_SOUNDS]
										        = GlobalTestSettings.getCurrentGameScaledTime()
										        - timeBefore;
	
										timeBefore = GlobalTestSettings.getCurrentGameScaledTime() ;
										drawOverElements(c, detailedScore,
												timeDifference);
										timeThisTick[INDEX_TIME_DRAW_UI]
										        += GlobalTestSettings.getCurrentGameScaledTime()
										        - timeBefore;
										
										drawThumbnailIfNeeded() ;
										
										if ( shuffleIfNeeded( mSliceTime[0].getUnpaused(), shuffle_lastTime ) )
											shuffle_lastTime = mSliceTime[0].getUnpaused() ;
	
										boolean notify = !changedByTick;
										for (int i = 0; i < numPlayers; i++)
											notify = notify
													&& !didAnimateSignificantEvent[i];
	
										if (notify)
											notifyListenerTickedWithoutChange();
	
										for (int i = 0; i < game.length; i++) {
											if (!gameOver[i] && !game[i].stillPlaying()) {
												gameOver[i] = true;
												// Update won/lost lists and send to
												// controller.
												boolean atLeastOne = false;
												for (int j = 0; j < game.length; j++) {
													tempGameWonLost[j] = game[j].hasLost();
													atLeastOne = atLeastOne
															|| tempGameWonLost[i];
												}
												// TODO: Should we do anything,
												// knowing there was a loss?
												// if ( atLeastOne )
	
												atLeastOne = false;
												for (int j = 0; j < game.length; j++) {
													tempGameWonLost[j] = game[j]
															.hasWon();
													atLeastOne = atLeastOne
															|| tempGameWonLost[i];
												}
												// TODO: Should we do anything,
												// knowing there was a victory?
												// if ( atLeastOne )
												
												atLeastOne = false ;
												boolean atLeastTwo = false ;
												for ( int j = 0; j < game.length; j++ ) {
													atLeastTwo = atLeastOne && game[j].stillPlaying() ;
													atLeastOne = atLeastOne || game[j].stillPlaying() ;
												}
												// At least one (or two) people still playing?
												// Play a sound now.
												if ( atLeastTwo ) {
													if ( i == localPlayerGame ) {
														if ( game[i].hasLost() )
															soundPool.gameLoss() ;
														else
															soundPool.gameWin() ;
													} else {
														if ( game[i].hasLost() )
															soundPool.gameWin() ;
														else
															soundPool.gameLoss() ;
													}
												}
											}
										}
	
										// total and count for this frame...
										long frameTime = GlobalTestSettings.getCurrentGameScaledTime()
												- currentTime;
										timeThisTick[INDEX_TIME_NUMBER_FRAMES] = 1;
										timeThisTick[INDEX_TIME_FRAME] = frameTime;
										if (this.slices[displayedGame]
												.getBlocksState() == GameBlocksSlice.BLOCKS_PIECE_FALLING) {
											timeThisTick[INDEX_TIME_NUMBER_FALLING_FRAMES] = 1;
											timeThisTick[INDEX_TIME_FALLING_FRAME] = frameTime;
											timeThisTick[INDEX_TIME_BETWEEN_FALLING_FRAMES] = timeThisTick[INDEX_TIME_BETWEEN_FRAMES];
										}
	
										// include this tick in max, total.
										for (int i = 0; i < NUM_INDICES_TIME; i++) {
											timeTotal[i] += timeThisTick[i];
											timeMax[i] = Math.max(timeMax[i],
													timeThisTick[i]);
										}
	
										// Send frame states to View.
										if (timeTotal[INDEX_TIME_NUMBER_FRAMES] >= 200) {
											if (GlobalTestSettings.GAME_SHOW_FRAME_RATE) {
												Message msg = handler
														.obtainMessage(WHAT_FRAME_RATE);
												Bundle b = new Bundle();
	
												for (int i = 0; i < NUM_INDICES_TIME; i++) {
													mTimeTotal[i] = timeTotal[i];
													mTimeMax[i] = timeMax[i];
												}
												
												mDisplacement[INDEX_DISPLACEMENT_CURRENT] = game[0].getDisplacedRows() ;
												mDisplacement[INDEX_DISPLACEMENT_AND_TRANSFERRED] = game[0].getDisplacedAndTransferredRows() ;
												
	
												msg.setData(b);
												handler.sendMessage(msg);
											}
	
											for (int i = 0; i < NUM_INDICES_TIME; i++) {
												timeTotal[i] = 0;
												timeMax[i] = 0;
											}
										}
										
										// update "last tick piece type"
										for ( int i = 0; i < numPlayers; i++ ) {
											lastTickPieceType[i] = slices[i] == null ? -1 : slices[i].getPieceType() ;
										}
	
										firstPausedIteration = false ;
									}
								} catch (Exception e) {
									Log.e(TAG, " ***************************************** Exception caught in after-tick operations (drawing, sound, etc.)") ;
									e.printStackTrace();
									Log.e(TAG,
											"Exception thrown, possibly due to recycle");
									running = false;
									notifyListenerCanceled() ;
									return;
								}
								// Log.d(TAG,
								// "-END-   Synchronization in reallocateSpecialtyDrawObjects")
								// ;
	
								firstUpdate = false;
								timeLastTickEnded = GlobalTestSettings.getCurrentGameScaledTime() ;
							} else {
								// Log.d(TAG, "skipping draw due to 'null' canvas")
								// ;
								try {
									Thread.sleep(10);
									// TODO: check whether we should self-terminate.
									// For example, if the GameActivity
									// is not front-displayed after a large number
									// of loops...
								} catch (InterruptedException e) {
								}
							}
						} finally {
							// do this in a finally so that if an exception is
							// thrown
							// during the above, we don't leave the Surface in an
							// inconsistent state
							if (c != null) {
								// Log.d(TAG, "unlock and post") ;
								surfaceHolder.unlockCanvasAndPost(c);
							}
						}
					}
				}
			} finally {

				synchronized (recycleMutex) {
					blockDrawer.recycle();
					nextPieceBlockDrawer.recycle();
					reservePieceBlockDrawer.recycle();
					if (blockDrawerMini != null)
						blockDrawerMini.recycle();

					if ( nextPieceBlockDrawerPreallocated != null )
						nextPieceBlockDrawerPreallocated.recycle() ;
					if ( reservePieceBlockDrawerPreallocated != null )
						reservePieceBlockDrawerPreallocated.recycle() ;
					
					releaseBitmaps();
					releaseUIElements();
				}

				blockDrawerPreallocated = null ;
				
				System.gc();

				notifyListenerCanceled();
				Log.d(TAG, "exiting");
			}
		}
		

		private void makePreviewPreallocatedBitmapsIfNeeded() {
			if ( needsNewPreallocatedBitmaps( nextPieceDrawSettings, nextPieceBlockDrawerPreallocated ) )
				nextPieceBlockDrawerPreallocated = newPreallocatedBitmapsForSlave( nextPieceDrawSettings ) ;
			if ( needsNewPreallocatedBitmaps( reservePieceDrawSettings, reservePieceBlockDrawerPreallocated ) )
				reservePieceBlockDrawerPreallocated = newPreallocatedBitmapsForSlave( reservePieceDrawSettings ) ;
		}

		private BlockDrawerPreallocatedBitmaps newPreallocatedBitmapsForSlave( DrawSettings ds ) {
			return BlockDrawer.preallocateBitmapsForSlave(
					ds.configCanvas.region.width(),
					ds.configCanvas.region.height(),
					ds.getSize_blockWidth(),
					ds.getSize_blockHeight(),
					ds.getBlit(),
					ds.getScale(),
					QuantroPreferences.supportsMidDetailGraphics( getContext() ) ? DrawSettings.DRAW_DETAIL_HIGH : DrawSettings.DRAW_DETAIL_LOW,
					DrawSettings.DRAW_ANIMATIONS_NONE,
					((QuantroApplication)getContext().getApplicationContext()).getGameViewMemoryCapabilities(null).getLoadImagesSize(),
					((QuantroApplication)getContext().getApplicationContext()).getGameViewMemoryCapabilities(null).getBackgroundImageSize()) ;
		}
		
		private boolean needsNewPreviewPreallocatedBitmaps() {
			return needsNewPreallocatedBitmaps( nextPieceDrawSettings, nextPieceBlockDrawerPreallocated )
					|| needsNewPreallocatedBitmaps( reservePieceDrawSettings, reservePieceBlockDrawerPreallocated ) ;
		}
		
		private boolean needsNewPreallocatedBitmaps(
				DrawSettings ds, BlockDrawerPreallocatedBitmaps preallocated ) {
			
			boolean remake = preallocated == null || preallocated.isRecycled() ;
			remake = remake
					|| ds.getSize_blockWidth()
						> preallocated.getBlockWidth() ;
			remake = remake
					|| ds.getSize_blockHeight()
						> preallocated.getBlockHeight() ;
			if ( !remake ) {
				int blitWidth = preallocated.getBlitWidth() ;
				int blitHeight = preallocated.getBlitHeight() ;
				remake = ds.width > blitWidth * ds.getScale()
					|| ds.height > blitHeight * ds.getScale() ;
			}
			
			return remake ;
		}
		

		/**
		 * Ensures that our current game info caches accurately reflect the game
		 * state.
		 * 
		 * @return Did our information change from last time?
		 */
		private boolean getUpdateFromGames() {
			// Log.d(TAG, "getUpdateFromGames") ;

			boolean visibleBlockFieldUpdateOccurred = forceUpdate;

			// Iterate through all available games. We need to track
			// significant events through all games. However, because we
			// only actually *draw* the currently displayed game, we
			// only bother copying the block field for that game.

			// A note: we DO need to draw a thumbnail of the "thumbnail"
			// game, which is probably a different game than the displayed
			// one. For that, copy a "blockfield" which contains the
			// falling piece, suspended chunks, etc.; we don't care about
			// animating them.

			for (int i = 0; i < game.length; i++) {
				
				boolean forceThisUpdate = false ;
				
				newSlice[i] = false ;

				synchronized (game[i]) {
					if (gevents[i].getHappened(GameEvents.EVENT_NEXT_PIECE_CHANGED)
							|| gevents[i].getHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED)
							|| firstUpdate || forceUpdate) {
						// game[i].copyNextPieceBlocks(bf_next_piece,
						// this.bf_piece_outer_buffer,
						// this.bf_piece_outer_buffer) ;
						game[i].copyNextPieceGameBlocksSlice(nextPieceSlices[i]);
						gevents[i]
								.forgetHappened(GameEvents.EVENT_NEXT_PIECE_CHANGED);
						forceThisUpdate = changedNextPieceSlice = true;
						if (i == localPlayerGame)
							handler.sendEmptyMessage(WHAT_SLICE_NEXT_UPDATE);
						changedNextPieceSlice = true;
					}
					if (gevents[i].getHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED)
							|| gevents[i].getHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED)
							|| firstUpdate || forceUpdate) {
						// game[i].copyReservePieceBlocks(bf_reserve_piece,
						// this.bf_piece_outer_buffer,
						// this.bf_piece_outer_buffer) ;
						game[i].copyReservePieceGameBlocksSlice(reservePieceSlices[i]);
						gevents[i]
								.forgetHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED);
						forceThisUpdate = changedReservePieceSlice = true;
						if (i == localPlayerGame)
							handler.sendEmptyMessage(WHAT_SLICE_RESERVE_UPDATE);
						changedReservePieceSlice = true;
					}
					canUseReserve[i] = game[i].couldUseReserveWhenPieceFalling() ;
					
					boolean syncHappened = game[i].gevents.getHappened(GameEvents.EVENT_SYNCHRONIZED)
							|| game[i].gevents.getHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED) ;
					boolean fullSyncHappened = game[i].gevents.getHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED) ;
					
					// Displacement.  We need to basically update this all the time,
					// no matter what.
					if ( displaces ) {
						if ( game[i].s.geventsLastTick.getHappened( GameEvents.EVENT_DISPLACEMENT_TRANSFERRED )
								|| game[i].s.geventsLastTick.getHappened( GameEvents.EVENT_DISPLACEMENT_PREFILLED )
								|| forceUpdate || forceThisUpdate || firstUpdate || syncHappened ) {
							game[i].copyDisplacementBlocks(slices[i]) ;
							visibleBlockFieldUpdateOccurred = visibleBlockFieldUpdateOccurred || i == displayedGame ;
							if ( game[i].s.geventsLastTick.getHappened( GameEvents.EVENT_DISPLACEMENT_PREFILLED ) ) {
								blockDrawer.nextGameBlocksSliceMayBeInconsistent() ;
								blockDrawer.nextGameBlocksSliceDisplacementChanged() ;
							}
						}
						game[i].updateDisplacement(slices[i]) ;
					} else {
						slices[i].setDisplacement(0) ;
					}

					// First thing: look for updates that imply a change in game
					// state.
					// If one is found, update state and copy all necessary
					// info.
					// If none found, then check "minor events," which change
					// the
					// current internal state of the game but are NOT considered
					// significant, such as piece_move.

					
					if (animatingSignificantEvent[i] && !forceUpdate && !forceThisUpdate
							&& !firstUpdate && !fullSyncHappened)
						continue; // No further update required for game 'i'
					if (!gevents[i].eventHappened() && !forceUpdate && !forceThisUpdate
							&& !firstUpdate && !fullSyncHappened)
						continue; // No further update required for game 'i'
					if (paused && !firstPausedUpdate && !fullSyncHappened)
						continue; // Have already updated for this pause; don't
									// keep grabbing.
									// Since we are not advancing games, this
									// can produce a terrible sound bug.

					// "Significant events" are those which change our
					// estimation of game
					// state. "Minor events" are those which possibly altered
					// the information
					// we need to show, but didn't change the game state, such
					// as "piece falling", etc.

					// Significant events!
					int newState = -1;
					if (gevents[i].getHappened(GameEvents.EVENT_PIECE_PREPARED))
						newState = GAME_STATE_IS_PIECE_FALLING;
					else if (animations == DrawSettings.DRAW_ANIMATIONS_ALL
							&& gevents[i].getHappened(GameEvents.EVENT_COMPONENTS_FELL))
						newState = GAME_STATE_IS_COMPONENTS_FALLING;
					else if (gevents[i]
							.getHappened(GameEvents.EVENT_CHUNKS_FELL))
						newState = GAME_STATE_IS_CHUNKS_FALLING;
					else if (gevents[i].getHappened(GameEvents.EVENT_CLEAR))
						newState = GAME_STATE_IS_CLEARING;
					else if (gevents[i].getHappened(GameEvents.EVENT_ATTACK_PUSH_ROWS))
						newState = GAME_STATE_IS_ADDING_ROWS;
					else if (gevents[i]
							.getHappened(GameEvents.EVENT_METAMORPHOSIS))
						newState = GAME_STATE_IS_METAMORPHOSIZING;
					else if (gevents[i]
							.getHappened(GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS))
						newState = GAME_STATE_IS_METAMORPHOSIZING;
					else
						newState = GAME_STATE_IS_STABLE; // no state change, but
															// be sure we
															// consider this as
															// "animating significant"
															// so that we clear
															// the events after
															// a draw.
															// necessary so we
															// get at least one
															// tick of
															// "just a level gain"
															// in Progression
															// mode.

					if (newState > -1) {
						/*if (gameState[i] != newState
								|| !animatingSignificantEvent[i]) {*/
							gameState[i] = newState;
							animatingSignificantEvent[i] = true;
						/*}*/
					}

					boolean newPiece = firstUpdate;
					if (!newPiece && game[i].s.geventsLastTick != null) {
						newPiece = game[i].s.geventsLastTick
								.getHappened(GameEvents.EVENT_PIECE_ENTERED)
								|| game[i].s.geventsLastTick
										.getHappened(GameEvents.EVENT_RESERVE_INSERTED)
								|| game[i].s.geventsLastTick
										.getHappened(GameEvents.EVENT_RESERVE_PIECE_CHANGED)
								|| game[i].s.geventsLastTick
										.getHappened(GameEvents.EVENT_RESERVE_SWAPPED);
					}

					// Get an update of the blockField(s) and other structures
					// necessary to draw the current game state. Note that
					// although we
					// only draw this for the displayedGame, we need
					// metainformation
					// in order to correctly time "undisplayed animations".
					if (i == displayedGame) {
						visibleBlockFieldUpdateOccurred = visibleBlockFieldUpdateOccurred
								|| gevents[i].eventHappened();
					}

					// slice it
					try {
						game[i].copyGameBlocksSlice(slices[i]);
						soundEvents[i].takeVals(game[i].s.geventsLastTick);
						achievementEvents[i].takeVals(game[i].s.geventsLastTick) ;
						newSlice[i] = true ;
						if (i == localPlayerGame && newPiece) {
							handler.sendEmptyMessage(WHAT_SLICE_FALLING_UPDATE);
							if ( tooltipDrawer != null ) {
								tooltipDrawer.pieceEntered(game[i].getPieceType(),
										game[i].getPieceHistory());
							}
						}
						
						// New pieces are (hopefully) appropriately handled by ANIMATIONS_ALL.
						// Are they handled by STUTTER?  Guess we'll find out.

						// Synchronization might make our slices inconsistent.
						// This happens
						// VERY rarely (just as a new piece comes in).
						if (i == displayedGame && syncHappened) {
							blockDrawer.nextGameBlocksSliceMayBeInconsistent();
							game[i].gevents.forgetHappened(GameEvents.EVENT_SYNCHRONIZED);
							game[i].gevents.forgetHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED) ;
						}
						if (i == thumbnailGame && syncHappened && blockDrawerMini != null) {
							blockDrawerMini.nextGameBlocksSliceMayBeInconsistent();
							game[i].gevents.forgetHappened(GameEvents.EVENT_SYNCHRONIZED);
							game[i].gevents.forgetHappened(GameEvents.EVENT_FULL_SYNCHRONIZATION_APPLIED) ;
						} else if (i == thumbnailGame
								&& game[i].s.geventsLastTick.eventHappened() && blockDrawerMini != null) {
							blockDrawerMini.nextGameBlocksSliceMayBeInconsistent();
						}

						if (i == thumbnailGame)
							newThumbnailGameBlocksSlice = true;
					} catch (QOrientationConflictException e) {
						// TODO Auto-generated catch block
						Log.d(TAG, "caught a QOrientationConflictException for game " + i);
						e.printStackTrace();
					} catch (GameSystemException e) {
						// TODO Auto-generated catch block
						Log.d(TAG, "caught a GameSystemException for game " + i);
						e.printStackTrace();
					}
				}
			}

			if (paused)
				firstPausedUpdate = false;
			forceUpdate = false;
			return visibleBlockFieldUpdateOccurred;
		}
		
		
		/**
		 * Update achievements for the specified game, which just
		 * ticked and had its state change.
		 * @param gameNum
		 */
		private void updateAchievements(int gameNum) {
			// only update if the player is participating.
			if ( gameNum == localPlayerGame ) {
				// From our perspective, we care about only two
				// things: piece success and piece entering. 
				// We handle them in that order:
				// Success (for the previous piece) before the next
				// piece enters.
				if ( this.game[gameNum].s.geventsLastTick.getHappened(GameEvents.EVENT_PIECE_SUCCESS) ) {
					Achievements.game_pieceSuccessfulForPlayer() ;
				}
				
				if ( this.game[gameNum].s.geventsLastTick.getHappened(GameEvents.EVENT_PIECE_ENTERED) ) {
					Achievements.game_pieceEnteredForPlayer(
							game[gameNum].s.piece.type,
							ginfo[gameNum]) ;
				}
			}
		}

		
		private void advanceBlockDrawerSliceTimes( long millis ) {
			for ( int i = 0; i < mSliceTime.length; i++ ) {
				if ( i == displayedGame ) {
					if ( this.newSlice[i] )
						blockDrawer.advanceNewSliceInForeground(slices[i], mSliceTime[i], millis, paused) ;
					else
						blockDrawer.advanceSliceInForeground(slices[i], mSliceTime[i], millis, paused) ;
				} else {
					if ( this.newSlice[i] )
						blockDrawer.advanceNewSliceInBackground(slices[i], mSliceTime[i], millis, paused) ;
					else
						blockDrawer.advanceSliceInBackground(slices[i], mSliceTime[i], millis, paused) ;
				}
				
				this.newSlice[i] = false ;
			}
		}
		
		
		/**
		 * 
		 * @param canvas
		 * @param detailedScore
		 * @param timePassedSinceLastDraw
		 */
		private void drawGameInterface(Canvas canvas, boolean detailedScore,
				long timePassedSinceLastDraw) {

			// Log.d(TAG, "drawGameInterface") ;
			// Cover the whole world with the nonbutton color, then
			// draw buttons.
			/*
			 * canvas.clipRect( 0, 0, canvasWidth, canvasHeight ) ;
			 * canvas.drawBitmap(bitmap_panelLeft, 0, 0, null) ;
			 * canvas.drawBitmap(bitmap_panelRight, rect_panelRight.left, 0,
			 * null) ;
			 */
			// Color the panels EXCEPT for the bitmapped area.
			// Bitmap is fully-opaque.
			tempPath.reset();
			tempPath.addRect(rect_panelLeft.left, rect_panelLeft.top, rect_panelLeft.right, rect_panelLeft.bottom, Path.Direction.CW);
			tempPath.addRect(rect_panelRight.left, rect_panelRight.top, rect_panelRight.right, rect_panelRight.bottom, Path.Direction.CW);

			canvas.save();
			canvas.clipPath(tempPath, Region.Op.INTERSECT);
			canvas.drawColor(mColor[INDEX_PANEL_BACKGROUND]);
			canvas.restore();
			
			// Draw the frame
			for ( int i = 0; i < bitmap_panelFrame.size(); i++ ) {
				Bitmap b = bitmap_panelFrame.get(i) ;
				Offset o = bitmap_panelFramePosition.get(i) ;
				canvas.drawBitmap(b, o.x, o.y, null);
			}
			
			// draw our left/right panel drawables
			// for ( int i = 0; i < drawable_panelLeft.size(); i++ )
			// drawable_panelLeft.get(i).draw(canvas) ;
			// for ( int i = 0; i < drawable_panelRight.size(); i++ )
			// drawable_panelRight.get(i).draw(canvas) ;
			
			boolean opponentPressed = name_button.length > INDEX_BUTTON_OPPONENT 
					&& view_invisibleControls.getButtonPressedByName(name_button[INDEX_BUTTON_OPPONENT]) ;

			// cut out alternate panel (if necessary).
			canvas.save() ;
			if ( displayAlternatePanelForOpponent && opponentPressed
					&& this.defaultDisplayedGame != this.displayedGame ) {
				canvas.clipRect(rect_panelOpponentPressedNeg, Region.Op.INTERSECT) ;
			}
			
			for (int i = 0; i < bitmap_panelLeft.size(); i++) {
				Bitmap b = bitmap_panelLeft.get(i);
				Offset o = bitmap_panelLeftPosition.get(i);
				canvas.drawBitmap(b, o.x, o.y, null);
			}
			for (int i = 0; i < bitmap_panelRight.size(); i++) {
				Bitmap b = bitmap_panelRight.get(i);
				Offset o = bitmap_panelRightPosition.get(i);
				canvas.drawBitmap(b, o.x, o.y, null);
			}
			canvas.restore() ;

			// Draw the score.
			// TODO: only perform the above overdraw of its rectangle, and the
			// below drawToCanvas, if the score has an update ready to draw.
			for (int i = 0; i < scoreDrawer.length; i++) {
				if (!paused) {
					if ( scoreDrawer[i] != null )
						scoreDrawer[i].update(currentTime);
					if ( scoreDrawerDetailed[i] != null )
						scoreDrawerDetailed[i].update(currentTime);
				}
			}
			if ( !detailedScore && scoreDrawer[displayedGame] != null )
				scoreDrawer[displayedGame].drawToCanvas(canvas, currentTime);
		}

		private void drawNextAndReserveBlocks(Canvas canvas) {
			boolean opponentPressed = name_button.length > INDEX_BUTTON_OPPONENT 
					&& view_invisibleControls.getButtonPressedByName(name_button[INDEX_BUTTON_OPPONENT]) ;
			// NOTE: "if requires_draw_*" commented out, since the above loop
			// will always draw over this space. Make the change when optimizing
			// to only blank and redraw when necessary.
			// Draw other specialized info, e.g. other pieces.
			if  ( (displayNextPreviewOnLeft != displayOpponentThumbnailOnLeft) || !displayAlternatePanelForOpponent
					|| this.displayedGame == this.defaultDisplayedGame
					|| !opponentPressed ) {
				if (changedNextPieceSlice) {
					nextPieceBlockDrawer.nextGameBlocksSliceBreaksSequence();
					// draw Stable or MinimumProfile based on settings.
					if ( piecePreviewMinimumProfile )
						nextPieceBlockDrawer.drawMinimumProfileGameBlocksSlice(canvas,
								nextPieceSlices[displayedGame], nextPieceSliceTime);
					else
						nextPieceBlockDrawer.drawStableGameBlocksSlice(canvas,
								nextPieceSlices[displayedGame], nextPieceSliceTime); 
					changedNextPieceSlice = false;
				} else
					nextPieceBlockDrawer.reblitLastGameBlocksSlice(canvas);
			}

			if ( (displayNextPreviewOnLeft == displayOpponentThumbnailOnLeft) || !displayAlternatePanelForOpponent
					|| this.displayedGame == this.defaultDisplayedGame
					|| !opponentPressed ) {
				if (changedReservePieceSlice) {
					reservePieceBlockDrawer.nextGameBlocksSliceBreaksSequence();
					// draw Stable or MinimumProfile based on settings.
					if ( piecePreviewMinimumProfile )
						reservePieceBlockDrawer.drawMinimumProfileGameBlocksSlice(canvas,
								reservePieceSlices[displayedGame], reservePieceSliceTime, canUseReserve[displayedGame] ? 255 : 128);
					else
						reservePieceBlockDrawer.drawStableGameBlocksSlice(canvas,
								reservePieceSlices[displayedGame], reservePieceSliceTime, canUseReserve[displayedGame] ? 255 : 128);
					changedReservePieceSlice = false;
				} else
					reservePieceBlockDrawer.reblitLastGameBlocksSlice(canvas, canUseReserve[displayedGame] ? 255 : 128);
			}
		}

		private int drawOpponentBlocks_lastDrawnOpponent = -1 ;
		private void drawOpponentBlocks(Canvas canvas) {
			boolean opponentPressed = name_button.length > INDEX_BUTTON_OPPONENT 
					&& view_invisibleControls.getButtonPressedByName(name_button[INDEX_BUTTON_OPPONENT]) ;
			
			// HACK HACK HACK: I can't determine the reason for this,
			// but when reestablishing a GameView thread after
			// pausing-and-resuming the game, somehow all .draw properties
			// get set to true. It appears that they objects get
			// constructed BEFORE a call to setDisplayedGame,
			// which should set to 'false' all those not currently
			// displayed. I have used Eclipse to find all
			// references to .draw, and that (and the constructor)
			// seem to be the only places where it is set.
			// This bug does not affect player 2 (slot=1), because
			// slot 1 is drawn last, but player 1 (slot=0) will
			// have their game overdrawn by player 2's.
			if (blockDrawerMini != null &&
					( !displayAlternatePanelForOpponent || this.displayedGame == this.defaultDisplayedGame
							|| !opponentPressed ) ) {
				if (this.rect_touchable_button.length > INDEX_BUTTON_OPPONENT && rect_touchable_button[INDEX_BUTTON_OPPONENT] != null ) {
					int opponentIndex = thumbnailGame % numPlayers ;
					if ( drawOpponentBlocks_lastDrawnOpponent != opponentIndex ) {
						Log.d(TAG, "switching opponent thumbnail from " + drawOpponentBlocks_lastDrawnOpponent + " to " + opponentIndex) ;
						drawOpponentBlocks_lastDrawnOpponent = opponentIndex ;
					}
					if (newThumbnailGameBlocksSlice) {
						//Log.d(TAG, "drawing thumbnail " + (thumbnailGame % numPlayers) + " formed of " + thumbnailGame + " % " + numPlayers) ;
						blockDrawerMini.nextGameBlocksSliceMayBeInconsistent() ;
						blockDrawerMini.drawStableGameBlocksSlice(canvas,
								slices[opponentIndex], mSliceTime[opponentIndex]);
						newThumbnailGameBlocksSlice = false;
					} else
						blockDrawerMini.reblitLastGameBlocksSlice(canvas);
				}
			}
		}

		private void drawOverElements(Canvas canvas, boolean detailedScore,
				long timePassedSinceLastDraw) {
			
			boolean opponentPressed = name_button.length > INDEX_BUTTON_OPPONENT 
					&& view_invisibleControls.getButtonPressedByName(name_button[INDEX_BUTTON_OPPONENT]) ;

			canvas.save();
			canvas.clipRect(0, 0, canvasWidth, canvasHeight, Region.Op.INTERSECT);

			// Draw Tooltips
			if ( tooltipDrawer != null && this.displayedGame == this.defaultDisplayedGame ) {
				tooltipDrawer.drawToCanvas(canvas, paused ? 0
						: timePassedSinceLastDraw);
			}

			// Draw detailed score
			if (detailedScore) {
				canvas.save();
				canvas.clipRect(rect_panelScoreDrawableRegion, Region.Op.INTERSECT);
				canvas.drawColor(mColor[INDEX_PANEL_BACKGROUND]);
				canvas.restore();
				// for ( int i = 0; i < drawable_panelScore.size(); i++ )
				// drawable_panelScore.get(i).draw(canvas) ;
				for (int i = 0; i < bitmap_panelScore.size(); i++) {
					Bitmap b = bitmap_panelScore.get(i);
					Offset o = bitmap_panelScorePosition.get(i);
					canvas.drawBitmap(b, o.x, o.y, null);
				}

				if ( scoreDrawerDetailed[displayedGame] != null ) {
					scoreDrawerDetailed[displayedGame].drawToCanvas(canvas,
							currentTime);
				}
			}

			// draw the "reserve" icon. Its color and bounds have been set,
			// hopefully.
			canvas.save() ;
			if ( displayAlternatePanelForOpponent && opponentPressed
					&& this.defaultDisplayedGame != this.displayedGame ) {
				canvas.clipRect(rect_panelOpponentPressedNeg, Region.Op.INTERSECT) ;
			}
			for (int i = 0; i < bitmap_over.size(); i++) {
				Bitmap b = bitmap_over.get(i);
				Offset o = bitmap_overPosition.get(i);
				boolean draw = drawWithFlags( bitmap_overFlag.get(i) ) ;
				if ( draw )
					canvas.drawBitmap(b, o.x, o.y, null);
			}
			canvas.restore() ;
			// icon_button[INDEX_BUTTON_RESERVE].draw(canvas) ;
			
			// Draw player number
			if ( playerStatusDrawer != null ) {
				boolean veilVis = blockDrawer.veilOnScreen(mSliceTime[this.displayedGame]) ;
				playerStatusDrawer.drawToCanvas(
						canvas,
						paused ? 0 : timePassedSinceLastDraw,
						!veilVis,
						!veilVis && !detailedScore);
			}
			
			// Draw alternative left panel?
			if ( displayAlternatePanelForOpponent
					&& this.displayedGame != this.defaultDisplayedGame
					&& opponentPressed ) {
				for (int i = 0; i < bitmap_panelOpponentPressed.size(); i++) {
					Bitmap b = bitmap_panelOpponentPressed.get(i);
					Offset o = bitmap_panelOpponentPressedPosition.get(i);
					canvas.drawBitmap(b, o.x, o.y, null);
				}
				for ( int i = 0; i < this.text_panelOpponentPressedNumbers.size(); i++ ) {
					int index = text_panelOpponentPressedNumbersIndex.get(i).intValue() ;
					Paint p = text_panelOpponentPressedNumbersActivePaint ;
					switch( this.playerStatusDisplayed[index] ) {
					case LOST:
					case KICKED:
					case QUIT:
					case INACTIVE:
						p = text_panelOpponentPressedNumbersInactivePaint ;
						break ;
					}
					// TODO: if inactive status (Lost, Quit), switch to InactivePaint.
					String s = this.text_panelOpponentPressedNumbers.get(i) ;
					Offset o = this.text_panelOpponentPressedNumbersPosition.get(i) ;
					canvas.drawText(s, o.x, o.y, p) ;
				}
			}

			canvas.restore();
		}
		
		private boolean drawWithFlags( int flags ) {
			if ( flags == BITMAP_FLAGS_ALWAYS )
				return true ;
			
			// check flags and current state.
			boolean draw = false ;
			draw = draw || ( ( flags & BITMAP_FLAG_CAN_USE_RESERVE ) != 0
								&& canUseReserve[displayedGame] ) ;
			draw = draw || ( ( flags & BITMAP_FLAG_SHOWING_DEFAULT_GAME ) != 0
								&& this.displayedGame == this.defaultDisplayedGame ) ;
			
			return (flags & BITMAP_FLAG_NEGATE) != 0 ? !draw : draw ;
		}
		
		
		private void drawThumbnailIfNeeded() {
			if ( bitmap_gameThumbnail == null )
				return ;
			if ( this.numPlayers > 1 )
				return ;
			if ( !this.firstPausedIteration
					&& !game[0].s.geventsLastTick.getHappened(GameEvents.EVENT_GAME_LEVEL_UP)
					&& !GlobalTestSettings.GAME_THUMBNAIL_EVERY_TICK )
				return ;
			synchronized( bitmap_gameThumbnail ) {
				blockDrawer.redrawLastGameBlocksSlice(
						canvas_bitmap_gameThumbnail,
						canvas_bitmap_gameThumbnail_configCanvas) ;
			}
			handler.sendMessage(handler.obtainMessage(WHAT_THUMBNAIL_UPDATED)) ;
		}
		
		
		private boolean shuffleIfNeeded( long currentTime, long lastShuffle ) {
			if ( !this.shuffleParam_shuffleBackgrounds || this.backgroundShuffle == null || this.backgroundShuffle.size() == 0
					|| (this.backgroundShuffle.size() == 1 && this.backgroundShuffle.equals(this.background)) )
				return false ;
			
			boolean preloadBackground = false ;
			long timeSince = currentTime - lastShuffle ;
			
			// check if we should preload.
			if ( this.shuffleParam_shuffleBackgrounds ) {
				if ( this.shuffle_nextBackground == null && timeSince > this.shuffleParam_shuffleBackgroundMinGap/2 ) {
					//Log.d(TAG, "shuffleIfNeeded: preloading; last shuffle was " + ((float)timeSince)/1000 + " seconds ago") ;
					// prepare (and preload) the next background.
					shuffle_nextBackground = shuffleBackgrounds() ;
					preloadBackground = shuffle_nextBackground != null ;
				}
				
				if ( preloadBackground )
					blockDrawer.shufflePreload(getContext(), null, shuffle_nextBackground) ;

				// schedule?
				if ( shuffle_scheduled < 0 && this.shuffle_nextBackground != null ) {
					boolean schedule = false ;
					if ( timeSince >= shuffleParam_shuffleBackgroundMaxGap
							&& game[defaultDisplayedGame].s.geventsLastTick.getHappened(GameEvents.EVENT_PIECE_ENTERED) ) {
						// maximum gap and a piece just entered.
						schedule = true ;
					} else if ( timeSince >= shuffleParam_shuffleBackgroundMinGap
							&& this.shuffleParam_shuffleOnLevelUp
							&& game[defaultDisplayedGame].s.geventsLastTick.getHappened(GameEvents.EVENT_GAME_LEVEL_UP) ) {
						// at least minimum gap, and a level-up.
						schedule = true ;
					}
					
					if ( schedule ) {
						//Log.d(TAG, "shuffleIfNeeded: scheduling; last shuffle was " + ((float)timeSince)/1000 + " seconds ago") ;
						shuffle_scheduled = currentTime + 1000 ;
					}
				}

				if ( this.shuffle_scheduled >= 0 && currentTime > shuffle_scheduled ) {
					//Log.d(TAG, "shuffleIfNeeded: SHUFFLE!  last shuffle was " + ((float)timeSince)/1000 + " seconds ago") ;
					blockDrawer.shuffleTo(getContext(), null, shuffle_nextBackground, mSliceTime[defaultDisplayedGame]) ;
					handler.sendMessage(handler.obtainMessage(WHAT_BACKGROUND_SHUFFLE, shuffle_nextBackground) ) ;
					background = shuffle_nextBackground ;
					
					shuffle_nextBackground = null ;
					this.shuffle_scheduled = -1 ;
					
					return true ;
				}
			}
			
			return false ;
		}
		

		/**
		 * Draws the currently displayed game's blockField. We assume that no
		 * direct query of the game object is necessary for this draw - by the
		 * time this is called, all our cache objects contain the necessary
		 * information.
		 * 
		 * This method has a strange design which, if changes are made
		 * elsewhere, may produce bizarre behavior. To allow all animations to
		 * advance, it makes a call to some BlockDrawer.draw... variant for EACH
		 * Game object, game[0] through game[length-1]. For animated events,
		 * such as chunks falling, this ensures that the animations carry out in
		 * real-time *even when the game is not displayed*. Thus a player can
		 * switch back-and-forth between his and his opponent's games, and each
		 * will appear at the correct stage of animation when they are
		 * displayed.
		 * 
		 * One should take care to ensure that the DrawSettings objects for
		 * these games are properly set. i.e., that all games for which it is
		 * inappropriate to draw fancy animations have drawSettings[i].draw set
		 * to 'false'.
		 * 
		 * This method will NOT explicitly use the values displayedGame and
		 * thumbnailGame. It will make calls for ALL games, and update their
		 * animation metadata accordingly, regardless of whether their
		 * drawSettings objects result in anything appearing on the screen.
		 * 
		 * (As an aside, note that the calls to "BlockDrawer.draw" will use
		 * whatever value is currently stored in such structures as
		 * bf_blockField, bf_falling_blockfields
		 * 
		 * @param canvas
		 *            The canvas on which to draw.
		 */
		private void drawBlockFieldsAndUpdateAnimations(Canvas canvas) {

			// /////////////////////////////////////////////////////////////////////
			// / VEIL TEST! May have bad interaction with actual 'veil'
			// behavior!
			if (GlobalTestSettings.GAME_CYCLE_VEIL) {
				if (slices[0].getBlocksState() == GameBlocksSlice.BLOCKS_STABLE
						|| slices[0].getBlocksState() == GameBlocksSlice.BLOCKS_PIECE_FALLING) {
					if (!TEST_VEIL_veiled && TEST_VEIL_nextVeilOn < mSliceTime[displayedGame].getTotal()) {
						Log.d(TAG, "Cycle Veil On") ;
						TEST_VEIL_veiled = true;
						blockDrawer.veil(mSliceTime[displayedGame]);
						TEST_VEIL_nextVeilOff = mSliceTime[displayedGame].getTotal() + 10000; // 10
																		// seconds
					} else if (TEST_VEIL_veiled
							&& TEST_VEIL_nextVeilOff < mSliceTime[displayedGame].getTotal()) {
						Log.d(TAG, "Cycle Veil Off") ;
						TEST_VEIL_veiled = false;
						blockDrawer.unveil(mSliceTime[displayedGame]);
						TEST_VEIL_nextVeilOn = mSliceTime[displayedGame].getTotal() + 20000; // 20
																	// seconds
					}
				}
			}
			
			// /////////////////////////////////////////////////////////////////////
			// / BG SHUFFLE TEST! May have bad interaction with actual 'background shuffle'
			// behavior!
			if ( GlobalTestSettings.GAME_CYCLE_BACKGROUND_SHUFFLE ) {
				if ( mSliceTime[displayedGame].getTotal() > TEST_SHUFFLE_BG_nextShufflePreload ) {
					Log.d(TAG, "GAME_CYCLE_BACKGROUND_SHUFFLE: preloading") ;
					Background.Template template = Background.Template.PIECES ;
					Background.Shade shade = Background.Shade.WHITE ;
					
					if ( TEST_SHUFFLE_BG_background != null ) {
						switch( TEST_SHUFFLE_BG_background.getTemplate() ) {
						case PIECES:
							template = Background.Template.SPIN ;
							break ;
						case SPIN:
							template = Background.Template.ARGYLE ;
							break ;
						case ARGYLE:
							template = Background.Template.RHOMBI ;
							break ;
						case RHOMBI:
							template = Background.Template.TARTAN ;
							break ;
						case TARTAN:
							template = Background.Template.TILTED_TARTAN ;
							break ;
						case TILTED_TARTAN:
							template = Background.Template.PIECES ;
							break ;
						}
						switch( TEST_SHUFFLE_BG_background.getShade() ) {
						case WHITE:
							shade = Background.Shade.LIGHT ;
							break ;
						case LIGHT:
							shade = Background.Shade.DARK ;
							break ;
						case DARK:
							shade = Background.Shade.BLACK ;
							break ;
						case BLACK:
							shade = Background.Shade.WHITE ;
							break ;
						}
					}
					
					TEST_SHUFFLE_BG_background = Background.get(template, shade) ;
					
					Log.d(TAG, "shufflePreload at " + System.currentTimeMillis()) ;
					blockDrawer.shufflePreload(getContext(), null, TEST_SHUFFLE_BG_background) ;
					Log.d(TAG, "shufflePreload finished at " + System.currentTimeMillis()) ;
					TEST_SHUFFLE_BG_nextShufflePreload = Long.MAX_VALUE ;
					TEST_SHUFFLE_BG_nextShuffleGo = mSliceTime[displayedGame].getTotal() + 10000 ;
				}
				
				if ( mSliceTime[displayedGame].getTotal() > TEST_SHUFFLE_BG_nextShuffleGo && TEST_SHUFFLE_BG_background != null ) {
					Log.d(TAG, "GAME_CYCLE_BACKGROUND_SHUFFLE: GO") ;
					blockDrawer.shuffleTo(getContext(), null, TEST_SHUFFLE_BG_background, mSliceTime[displayedGame]) ;
					TEST_SHUFFLE_BG_nextShufflePreload = mSliceTime[displayedGame].getTotal() + 30000 ;
					TEST_SHUFFLE_BG_nextShuffleGo = Long.MAX_VALUE ;
				}
			}

			boolean drawBlank = blockDrawer.veiled() && recycleToVeil;

			// apply the veil?
			if (veiled != blockDrawer.veiled() && !GlobalTestSettings.GAME_CYCLE_VEIL) {
				if (veiled) {
					veiledFramesSkipped = 0 ;
					if (recycleToVeil) {
						blockDrawer.recycle();
						drawBlank = true;
					}
					blockDrawer.veil(mSliceTime[displayedGame]);
				} else {
					// If we recycle to veil, we want to unveil in a single step
					// (fully unviel immediately).
					veiledFramesSkipped = 0 ;
					blockDrawer.unveil(mSliceTime[displayedGame]);
					if (recycleToVeil)
						newDrawSettings = true;
				}
			}

			if (drawBlank) {
				canvas.save();
				canvas.clipRect(blockFieldDrawSettings.configCanvas.region);
				canvas.drawColor(0xff444444);
				canvas.restore();
				return;
			}

			// Log.d(TAG, "drawBlockFieldsAndUpdateAnimations") ;
			// boolean report = new Random().nextInt(10) == 0 ;
			// if ( report )
			// System.out.println("reporting in GameView: drawBlockFieldsAndUpdateAnimations")
			// ;
			for (int i = 0; i < numPlayers; i++) {
				didAnimateSignificantEvent[i] = animatingSignificantEvent[i];
				if (i == displayedGame) {
					if (animatingSignificantEvent[i]) {
						boolean finished = blockDrawer.drawGameBlocksSlice(
								canvas, soundPool, slices[i],
								mSliceTime[i]);

						if (finished) {
							gevents[i].clearHappened();
							animatingSignificantEvent[i] = false;
						}
					} else {
						blockDrawer.drawGameBlocksSlice(canvas, soundPool,
								slices[i],
								mSliceTime[i]);
					}
					
					didAnimateSignificantEvent[i] =
						didAnimateSignificantEvent[i] || blockDrawer.drawGameBlocksSliceLastDrawDidAnimateGame() ;
				} else {
					if ( animatingSignificantEvent[i] && mSliceTime[i].finished() ) {
						gevents[i].clearHappened() ;
						animatingSignificantEvent[i] = false ;
					}
				}
				
				/*
				if ( mSliceTime[i].timeSinceFinished() > 2000 ) {
					Log.d(TAG, "mSliceTime[i] " + i + " finished for " + mSliceTime[i].timeSinceFinished() + " with animating " + animatingSignificantEvent[i] + " and events " + gevents[i].eventHappened() + " sig events " + gevents[i].significantEventHappened()) ;
					Log.d(TAG, "mSliceTime[i], game has state " + game[i].s.state + " progression state " + game[i].s.progressionState + " slice is type " + slices[i].getBlocksStateString()) ;
				}
				*/
			}
		}

		private void playEventSounds(boolean audible) {
			GameEvents sevents = soundEvents[displayedGame];
			GameBlocksSlice slice = slices[displayedGame];
			int prevPieceType = lastTickPieceType[displayedGame] ;

			if (audible) {
				
				// STUTTER SOUND EFFECTS
				// We stutter past some events that would be significant (and thus handled
				// by BlockDrawer) if we are in a Stutter Animations behavior.  To compensate,
				// we need to be willing to player certain sound effects here.
				if ( this.animations == DrawSettings.DRAW_ANIMATIONS_STABLE_STUTTER ) {
					if ( sevents.getHappened(GameEvents.EVENT_PIECE_LOCKED)
							|| sevents.getHappened(GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS) ) {
						// There are four possible sound effects we could play here.  It could be
						// Piece Lock, Piece Land, RiseAndFade, or GreyscaleMetamorphosis.  We
						// check 'prevPieceType' (along with the event) to get the type.
						if ( PieceCatalog.isSpecial( prevPieceType ) && PieceCatalog.getSpecialCategory( prevPieceType ) == PieceCatalog.SPECIAL_CAT_FLASH ) {
							// rise and fade!
							soundPool.pieceRiseFade(prevPieceType) ;
						} else if ( PieceCatalog.isTetracube( prevPieceType ) && PieceCatalog.getQCombination( prevPieceType ) == QCombinations.SL
								&& sevents.getHappened(GameEvents.EVENT_PIECE_LOCK_THEN_METAMORPHOSIS)
								&& sevents.getHappened(GameEvents.EVENT_METAMORPHOSIS_DID_ACTIVATE)) {
							soundPool.metamorphosis( QCombinations.SL, -1) ;
						} else {
							// we default to a "land" sound.  No "lock" sounds for now...
							// Note that this sound is VERY dissonant when also played with the "down"
							// sound.  We'd like to distinguish between pieces that land and pieces that
							// are dropped, so we DON'T play the 'land' sound if the user dropped the
							// piece this tick.
							if ( !sevents.getHappened(GameEvents.EVENT_PIECE_DROPPED) || !soundControls ) {
								soundPool.land(prevPieceType, true, PieceCatalog.getQCombination(prevPieceType)) ;
							}
						}
					}
					
					/*	// We no longer use this sound effect, as part of shifting displacement down 1 row visually
					 *  // (so fully-formed blocks creep on to the screen, rather than appear).
					if ( sevents.getHappened(GameEvents.EVENT_DISPLACEMENT_TRANSFERRED) ) {
						// Displacement blocks entered the game.
						soundPool.enter(-1) ;
					}
					*/
				}
				
				
				
				// moving left (or failing)
				if (soundControls
						&& sevents
								.getHappened(GameEvents.EVENT_PIECE_MOVED_LEFT)
						&& sevents
								.getHappened(GameEvents.EVENT_PIECE_FAILED_MOVE_LEFT))
					soundPool.moveLeftFailed(slice.getPieceType());
				else if (soundControls
						&& sevents
								.getHappened(GameEvents.EVENT_PIECE_MOVED_LEFT))
					soundPool.moveLeft(slice.getPieceType());
				// moving right (or failing)
				if (soundControls
						&& sevents
								.getHappened(GameEvents.EVENT_PIECE_MOVED_RIGHT)
						&& sevents
								.getHappened(GameEvents.EVENT_PIECE_FAILED_MOVE_RIGHT))
					soundPool.moveRightFailed(slice.getPieceType());
				else if (soundControls
						&& sevents
								.getHappened(GameEvents.EVENT_PIECE_MOVED_RIGHT))
					soundPool.moveRight(slice.getPieceType());
				// turning CW
				if (soundControls
						&& (sevents
								.getHappened(GameEvents.EVENT_PIECE_TURNED_CW) || sevents
								.getHappened(GameEvents.EVENT_PIECE_TURNED_CW_180)))
					soundPool.turnCW(slice.getPieceType());
				// turning CCW
				if (soundControls
						&& (sevents
								.getHappened(GameEvents.EVENT_PIECE_TURNED_CCW) || sevents
								.getHappened(GameEvents.EVENT_PIECE_TURNED_CCW_180)))
					soundPool.turnCCW(slice.getPieceType());
				// flipping piece
				if (soundControls
						&& (sevents.getHappened(GameEvents.EVENT_PIECE_FLIPPED)))
					soundPool.flip(slice.getPieceType()) ;
				// dropping piece.
				if (soundControls
						&& sevents.getHappened(GameEvents.EVENT_PIECE_DROPPED))
					soundPool.drop(slice.getPieceType());
				// level up
				if (sevents.getHappened(GameEvents.EVENT_GAME_LEVEL_UP))
					soundPool.levelUp(ginfo[displayedGame].level);
				// reserve
				boolean dequeued = sevents.getHappened(GameEvents.EVENT_RESERVE_DEQUEUED) ;
				// for now, make a sound when reserve is QUEUED, and when used (if NOT dequeued).
				if (soundControls ) {
					// Performed action.
					if ( ( sevents.getHappened(GameEvents.EVENT_RESERVE_ATTACK) && !dequeued )
							|| sevents.getHappened(GameEvents.EVENT_RESERVE_ATTACK_QUEUED) ) {
						soundPool.attack( -1 ) ;
					}
					else if ( ( sevents.getHappened(GameEvents.EVENT_RESERVE_INSERTED) && !dequeued )
							|| sevents.getHappened(GameEvents.EVENT_RESERVE_INSERT_QUEUED) ) {
						soundPool.reserve(0, slice.getPieceType()) ;
					}
					else if ( ( sevents.getHappened(GameEvents.EVENT_RESERVE_PUSHED) && !dequeued )
							|| sevents.getHappened(GameEvents.EVENT_RESERVE_PUSH_QUEUED) ) {
						soundPool.reserve(0, slice.getPieceType()) ;
					}
					else if ( ( sevents.getHappened(GameEvents.EVENT_RESERVE_SWAPPED) && !dequeued )
							|| sevents.getHappened(GameEvents.EVENT_RESERVE_SWAP_QUEUED) ) {
						soundPool.reserve(0, slice.getPieceType()) ;
					}
				}
				
				// special got upgraded
				if (sevents.getHappened(GameEvents.EVENT_SPECIAL_UPGRADE)) {
					soundPool.specialUpgraded(-1, -1) ;
				}
				// Oh noes!  dropping blocks!
				if (sevents.getHappened(GameEvents.EVENT_ATTACK_DROP_BLOCKS)) {
					soundPool.attackedByBlocks(-1) ;
				}
				// Oh noes!  displacing blocks!
				if (sevents.getHappened(GameEvents.EVENT_ATTACK_DISPLACEMENT_ACCEL)) {
					soundPool.displacementAccel(0) ;
				}
			}
			// Okay, that's all the sounds we play right here. Clear our sound
			// events.
			for (int i = 0; i < soundEvents.length; i++) {
				soundEvents[i].clearHappened();
			}
		}
		
		/**
		 * Returns a new background to shuffle in.  Will return 'null'
		 * if we are not shuffling, or the current background is the only
		 * background available.
		 * 
		 * @return
		 */
		protected Background shuffleBackgrounds() {
			//Log.d(TAG, "shuffleBackgrounds; number to shuffle is " + backgroundShuffle.size()) ;
			if ( backgroundShuffle == null ||
					backgroundShuffle.size() == 0 ||
					( backgroundShuffle.size() == 1 && backgroundShuffle.get(0).equals(background) ) ) {
				return null ;
			}
			
			int skipIndex = -1 ;
			for ( int i = 0; i < backgroundShuffle.size(); i++ ) {
				if ( backgroundShuffle.get(i).equals( background ) ) {
					skipIndex = i ;
				}
			}
			
			int randIndex ;
			if ( skipIndex == -1 ) 
				randIndex = r.nextInt( backgroundShuffle.size() ) ;
			else {
				randIndex = r.nextInt( backgroundShuffle.size() -1 ) ;
				if ( randIndex >= skipIndex )
					randIndex++ ;
			}
			
			return backgroundShuffle.get(randIndex) ;
		}

		@Override
		protected void finalize() throws Throwable {
			try {
				Log.d(TAG, "finalizing gameView.Thread");
			} finally {
				super.finalize();
			}
		}

		@Override
		public void bdapl_prerendered(BlockDrawerAsynchronousPrerenderer bdap,
				BlockDrawer bd, Object tag) {
			// our behavior is dependent on which block drawer object we just
			// finished, as well as whether we have a new draw settings.
			// If we have new draw settings, we just start everything over.
			// NOTE: we MUST be running.
			synchronized( mPrerendererMutex ) {
				// If the thread is finished, we want to end this as quickly as possible.
				// Same thing if we recycled.  If there's a new drawSettings, we leave
				// it to the main loop to start from scratch.
				if ( !running || recycled || newDrawSettings ) {
					Log.d(TAG, "bdapl_prerendered: ending work early.  Running " + running  + ", recycled " + recycled + ", newDrawSettings " + newDrawSettings) ;
					mPrerenderCycleWorking = false ;
					return ;
				}
				
				// Otherwise, take one more step along the cycle.
				BlockDrawer nextBD = null ;
				BlockDrawer masterBD = null ;
				BlockDrawerPreallocatedBitmaps preallocatedBitmaps = null ;
				DrawSettings nextDS = null ;
				
				if ( bd == this.blockDrawer ) {
					// we rendered the main (master) block drawer, which
					// has preallocated structures.  The next step is 
					// to render the 'next piece preview' using the main
					// BD as master.
					Log.d(TAG, "bdapl_prerendered: blockDrawer rendered.  Starting next piece.") ;
					preallocatedBitmaps = nextPieceBlockDrawerPreallocated ;
					nextBD = nextPieceBlockDrawer ;
					nextDS = nextPieceDrawSettings ;
					masterBD = blockDrawer ;
				} else if ( bd == nextPieceBlockDrawer ) {
					// we rendered the next piece preview.  Next is the
					// reserve piece preview; it's pretty much the same setup.
					Log.d(TAG, "bdapl_prerendered: next piece rendered.  Starting reserve piece.") ;
					preallocatedBitmaps = reservePieceBlockDrawerPreallocated ;
					nextBD = reservePieceBlockDrawer ;
					nextDS = reservePieceDrawSettings ;
					masterBD = blockDrawer ;
				} else if ( bd == reservePieceBlockDrawer ) {
					// We rendered the reserve piece block drawer.  Next is
					// mini -- IF we have it.
					Log.d(TAG, "bdapl_prerendered: reserve piece rendered.  Starting mini field (if any).") ;
					nextBD = blockDrawerMini ;
					nextDS = blockFieldMiniDrawSettings ;
				}
				
				// If we get here, assume: 1., prerendering is continuing
				// normally (w/o an error or a reset), 2. the thread is still
				// waiting for the result, and 3. the next step (bd, ds, etc.)
				// has been configured.  If bd is null, we're actually finished.
				if ( nextBD != null ) {
					// set preallocated bitmaps...
					if ( preallocatedBitmaps != null )
						nextBD.receivePreallocatedBitmaps(preallocatedBitmaps) ;
					// asynchronously set the draw settings.
					bdap.prerender(getContext(), this, null, nextBD, masterBD, nextDS) ;
				} else {
					Log.d(TAG, "bdapl_prerendered: prerender cycle complete.  Now drawing.") ;
					// the loop is finished.
					mPrerenderCycleWorking = false ;
					mPrerenderCycleFinished = true ;
					
					loaded = true;
					handler.sendEmptyMessage(WHAT_DONE_LOADING);
				}
			}
		}
		
		
		@Override
		public void bdapl_prerenderError(
				BlockDrawerAsynchronousPrerenderer bdap,
				BlockDrawer bd,
				Object tag, Exception exception ) {
			Log.d(TAG, "bdapl_prerenderError: terminating thread.") ;
			Log.d(TAG, Log.getStackTraceString(exception)) ;
			synchronized( mPrerendererMutex ) {
				// reset the appropriate preallocated bitmap object; a prerender
				// error invalidates its contents.
				if ( bd == blockDrawer ) {
					blockDrawerPreallocated.reset() ;
				} else if ( bd == nextPieceBlockDrawer ) {
					nextPieceBlockDrawerPreallocated.reset() ;
				} else if ( bd == reservePieceBlockDrawer ) {
					reservePieceBlockDrawerPreallocated.reset() ;
				}
				
				// the cycle isn't working, and neither is the thread running.
				this.mPrerenderCycleWorking = false ;
				this.running = false ;
			}
		}
		

		@Override
		public void bdapl_recycled(BlockDrawerAsynchronousPrerenderer bdap,
				BlockDrawer bd, Object tag) {
			// huh?  We don't recycle using the prerenderer...
		}

	}

	// ///////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////
	//
	// The surrounding view object
	//
	// ///////////////////////////////////////////////////////////////////////
	// ///////////////////////////////////////////////////////////////////////

	// For handlers
	public static final int WHAT_FRAME_RATE = 0;
	public static final int WHAT_SLICE_FALLING_UPDATE = 1;
	public static final int WHAT_SLICE_NEXT_UPDATE = 2;
	public static final int WHAT_SLICE_RESERVE_UPDATE = 3;
	public static final int WHAT_CONTROLS_THUMBNAIL_UPDATE = 4;
	public static final int WHAT_START_LOADING = 5;
	public static final int WHAT_DONE_LOADING = 6;
	public static final int WHAT_THUMBNAIL_UPDATED = 7 ;
	public static final int WHAT_BACKGROUND_SHUFFLE = 8 ;
	public static final int WHAT_BACKGROUND_CHANGED = 9 ;

	// For time information in frame rate display
	public static final int INDEX_TIME_NUMBER_FRAMES = 0;
	public static final int INDEX_TIME_FRAME = 1;
	public static final int INDEX_TIME_NUMBER_FALLING_FRAMES = 2;
	public static final int INDEX_TIME_FALLING_FRAME = 3;
	public static final int INDEX_TIME_BETWEEN_FRAMES = 4;
	public static final int INDEX_TIME_BETWEEN_FALLING_FRAMES = 5;
	public static final int INDEX_TIME_SYNC = 6;
	public static final int INDEX_TIME_GAME_UPDATE = 7;
	public static final int INDEX_TIME_GAME_DATA = 8;
	public static final int INDEX_TIME_UPDATE_ACHIEVEMENTS = 9 ;
	public static final int INDEX_TIME_ADVANCE_SLICES = 10 ;
	public static final int INDEX_TIME_DRAW_UI = 11 ;
	public static final int INDEX_TIME_DRAW_BLOCKS = 12 ;
	public static final int INDEX_TIME_PLAY_SOUNDS = 13 ;
	public static final int NUM_INDICES_TIME = 14 ;

	protected long[] mTimeTotal = new long[NUM_INDICES_TIME];
	protected long[] mTimeMax = new long[NUM_INDICES_TIME];
	
	public static final int INDEX_DISPLACEMENT_CURRENT = 0 ;
	public static final int INDEX_DISPLACEMENT_AND_TRANSFERRED = 1 ;
	public static final int NUM_INDICES_DISPLACEMENT = 2 ;
	protected double[] mDisplacement = new double[NUM_INDICES_DISPLACEMENT] ;

	private static String frameRateString(long[] total, long[] max, double [] displacement) {
		// constructs and returns a string, fit for display in a TextView,
		// describing the frame rate and time needed to display various
		// information.
		// Format:
		// max millis, avg. millis, % of total frame time, FPS
		// max fall millis, avg. fall millis, % of total fall frame time, fall
		// FPS
		// max. time, avg. time., % of frame time

		// we will divide by these, so...

		StringBuilder sb = new StringBuilder();
		sb.append(max[INDEX_TIME_FRAME])
				.append(", \t")
				.append(roundTo((double) total[INDEX_TIME_FRAME]
						/ (double) total[INDEX_TIME_NUMBER_FRAMES], 1))
				.append(", \t")
				.append(roundTo(
						100
								* (double) total[INDEX_TIME_FRAME]
								/ (double) (total[INDEX_TIME_FRAME] + total[INDEX_TIME_BETWEEN_FRAMES]),
						1))
				.append("%, \t")
				.append(roundTo(
						1 / (((total[INDEX_TIME_FRAME] + total[INDEX_TIME_BETWEEN_FRAMES]) / Math
								.max(1, total[INDEX_TIME_NUMBER_FRAMES])) / 1000.0),
						1)).append("\n");
		sb.append(max[INDEX_TIME_FALLING_FRAME])
				.append(", \t")
				.append(roundTo((double) total[INDEX_TIME_FALLING_FRAME]
						/ (double) total[INDEX_TIME_NUMBER_FALLING_FRAMES], 1))
				.append(", \t")
				.append(roundTo(
						100
								* (double) total[INDEX_TIME_FALLING_FRAME]
								/ (double) (total[INDEX_TIME_FALLING_FRAME] + total[INDEX_TIME_BETWEEN_FALLING_FRAMES]),
						1))
				.append("%, \t")
				.append(roundTo(
						1 / (((total[INDEX_TIME_FALLING_FRAME] + total[INDEX_TIME_BETWEEN_FALLING_FRAMES]) / Math
								.max(1, total[INDEX_TIME_NUMBER_FALLING_FRAMES])) / 1000.0),
						1)).append("\n");

		String[] labels = new String[] { "sync", "tick", "data", "ui  ",
				"blks", "snds" };
		for (int i = 0; i < labels.length; i++) {
			sb.append(labels[i]).append(" \t");
			int index = i + INDEX_TIME_SYNC;
			sb.append(max[index]).append(", \t");
			sb.append(
					roundTo((double) total[index]
							/ (double) total[INDEX_TIME_NUMBER_FRAMES], 1))
					.append(", \t");
			sb.append(
					roundTo(100
							* (double) total[index]
							/ (double) (Math.max(1, total[INDEX_TIME_FRAME]
									+ total[INDEX_TIME_BETWEEN_FRAMES])), 1))
					.append("%\n");
		}
		
		sb.append("disp ").append(displacement[INDEX_DISPLACEMENT_CURRENT]).append("\n") ;
		sb.append("totl ").append(displacement[INDEX_DISPLACEMENT_AND_TRANSFERRED]).append("\n") ;
		

		return sb.toString();
	}

	private static double roundTo(double val, double precision) {
		return Math.round(val / precision) * precision;
	}

	Object recycleMutex = new Object();

	// References to things the thread might need.
	boolean recycled = false ;
	SurfaceHolder holder;
	boolean surfaceCreated = false;

	// surface size
	int surfaceWidth = -1;
	int surfaceHeight = -1;
	
	// screen size?
	int screenSize ;

	// Game info
	Game[] game;
	GameInformation[] ginfo;
	GameEvents[] gevents;
	
	String[] playerNames ;
	PlayerGameStatusDisplayed[] playerStatuses ;

	// Controls - needed to draw "controls" rectangles
	InvisibleControls controls;
	// Sound pool for playing sounds.
	QuantroSoundPool soundPool;
	// PauseViewComponentAdapter
	PauseViewComponentAdapter pauseViewComponentAdapter;

	// These may be changed based on user preferences
	boolean mGraphicsSet = false ;
	// Detail
	int mDetail ;
	// Minimum profile for pieces?
	boolean mPiecePreviewMinimumProfile ;
	// Load large (40x40) images (alternative is 20x20 only)
	int mLoadImagesSize;
	// Load large (480x800), mid (240x400), small (120x200) images for the
	// background?
	int mLoadBackgroundSize;
	// Recycle on veil?
	boolean mRecycleToVeil;
	// Animation quality? (probably STUTTER or ALL)
	int mAnimations;
	// Play sound for user controls?
	boolean mSoundControls;
	// Blit
	int mBlit = DrawSettings.BLIT_FULL;
	// Scale
	int mScale = 1;
	
	// Skin and Color Scheme
	Skin mSkin ;
	ColorScheme mColorScheme ;
	// Background?
	Background mBackground = null ;
	ArrayList<Background> mBackgroundShuffle = new ArrayList<Background>() ;

	
	// Shuffle parameters.
	private boolean mShuffleParam_shuffleBackgrounds = false ;
	private long mShuffleParam_shuffleBackgroundMinGap = 0 ;
	private long mShuffleParam_shuffleBackgroundMaxGap = 0 ;
	private boolean mShuffleParam_shuffleOnLevelUp = false ;
	
	
	Drawable drawable_controlsThumbnail; // special thumbnail with a border and
										// everything.
	
	Bitmap bitmap_gameThumbnail ;	// thumbnail for showing the current game state.
	ChangeListener changeListener ;
	Canvas canvas_bitmap_gameThumbnail ;
	BlockDrawerConfigCanvas canvas_bitmap_gameThumbnail_configCanvas ;
	long unpausedTimeGoalpost = -1 ;

	// Piece tips?
	int mPieceTips;
	public static final int PIECE_TIPS_NEVER = 0;
	public static final int PIECE_TIPS_OCCASIONALLY = 1;
	public static final int PIECE_TIPS_OFTEN = 2;

	// Thread
	GameBlockFieldViewThread thread;
	int[] threadGameState;
	boolean paused;

	int playerGame ;
	int opponentGame ;
	boolean fullScreenOpponent = false ;
	
	boolean veiled;

	// FPS info
	private TextView fpsTextView;
	
	
	public interface ChangeListener {
		public void gvcl_thumbnailUpdated( Bitmap bitmap ) ;
		public void gvcl_backgroundChanged( Background background ) ;
		public void gvcl_controlsUpdated( Drawable drawable ) ;
	}
	

	/**
	 * 12/29/11.
	 * 
	 * Certain components of a game can "finish" before those changed catch up
	 * to what's on screen. For example, the GameCoordinator of a multiplayer
	 * game will sometimes realize that a game is over before any of the
	 * player's games have reached that state (because the Coordinator does not
	 * delay game events for animation). In cases like this, we don't want to
	 * immediately halt all displayed games, since it won't be clear to the
	 * player what happened; however, wo DO want to halt them as soon as our
	 * visual representation catches up with the server state.
	 * 
	 * The call-and-response "notifyWhenTickedGamesWithoutChange" method is
	 * meant to account for that. Externally, GameActivity, GameService, and
	 * other controllers should make whatever adjustments are needed to ensure
	 * that the Game objects don't keep running indefinitely: for example,
	 * deactivate Game controls and tell the ActionAdapter that Games should not
	 * tick. Then call notifyWhen... and provide a Listener.
	 * 
	 * The GameView guarantees that exactly one of the Listener methods will be
	 * called - exactly once - after (or during) this method call. Because these
	 * methods MAY be called from within the GameView's internal thread, take
	 * care that these methods are extremely truncated in their operation - if
	 * necessary, pass information to a separate work thread to allow the method
	 * to return quickly. Also, note that these methods pass through GameView as
	 * a synchronization layer, providing another reason to quickly finish up
	 * those methods.
	 * 
	 * One reason - although not the ONLY reason - why gvl_canceled() may be
	 * called is that a second call to notifyWhenTickedGamesWithoutChange was
	 * called, replacing the previous Listener; we must call gvl_canceled() to
	 * maintain the guarantee of 1 call per notification request.
	 * 
	 * @author Jake
	 * 
	 */
	public interface Listener {
		public void gvl_tickedGamesWithoutChange();

		public void gvl_canceled();
	}

	// callbacks are made to this listener when not null.
	private Listener listener = null;
	private Object listenerSynchronizationObject = new Object();

	/**
	 * Sets a listener to be notified the next time the thread ticks its games
	 * without change - i.e., when the Game objects themselves do not update,
	 * and when we are not animating any significant events on-screen. If 'true'
	 * is returned, we guarantee (as much as we are able) that exactly one of
	 * the Listener methods will be called.
	 * 
	 * To cancel, call this method with "null". It will return 'false' after
	 * calling gvl_canceled() on the previous listener (if set).
	 * 
	 * @param l
	 * @return
	 */
	public boolean notifyWhenTickWithoutChange(Listener l) {
		synchronized (listenerSynchronizationObject) {
			notifyListenerCanceled(); // safe if no listener set

			if (l == null)
				return false;

			// GameView must be running.
			if (thread == null || !thread.running || !thread.isAlive())
				return false;

			listener = l;
			return true;
		}
	}

	/**
	 * A simple, synchronized wrapper to notify the listener of no change. If no
	 * listener is waiting, this method has no effect and should immediately
	 * return. Otherwise, the listener receives their one (and only)
	 * notification and is no longer considered "listening." In other words,
	 * multiple calls to this method are completely safe.
	 */
	private void notifyListenerTickedWithoutChange() {
		synchronized (listenerSynchronizationObject) {
			if (listener != null) {
				Log.d(TAG,
						"notifyListenerTickedGamesWithoutChange: listener not null");
				listener.gvl_tickedGamesWithoutChange();
			}
			listener = null;
		}
	}

	private void notifyListenerCanceled() {
		synchronized (listenerSynchronizationObject) {
			if (listener != null)
				listener.gvl_canceled();
			listener = null;
		}
	}

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// Log.d(TAG, "constructor context, attrs") ;

		// set some stuff, null others
		this.holder = getHolder();
		this.holder.addCallback(this);

		this.game = null;
		this.ginfo = null;
		this.gevents = null;

		this.controls = null;

		this.mSkin = null;
		this.mDetail = DrawSettings.DRAW_DETAIL_MID;
		this.mPiecePreviewMinimumProfile = true ;
		mSoundControls = true;
		mPieceTips = PIECE_TIPS_OCCASIONALLY;

		this.thread = null;
		this.threadGameState = null;
		this.playerGame = 0 ;
		this.opponentGame = 0 ;
		this.fullScreenOpponent = false ;

		this.paused = true;

		setFocusable(false); // make sure we don't get key events
		
		Resources res = context.getResources();
		int screenLayout = res.getConfiguration().screenLayout;
		screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

		// We create the thread later, in the surface callback.
		// This prevents "start" from being called on the same thread
		// object multiple times.
	}

	public GameView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// Log.d(TAG, "constructor context, attrs, defstyle") ;

		// set some stuff, null others
		this.holder = getHolder();
		this.holder.addCallback(this);

		this.game = null;
		this.ginfo = null;
		this.gevents = null;

		this.controls = null;

		this.thread = null;
		this.paused = true;

		this.playerGame = 0 ;
		this.opponentGame = 0 ;
		this.fullScreenOpponent = false ;

		setFocusable(false); // make sure we don't get key events
		setKeepScreenOn(true); // Don't let the screen turn off
		
		Resources res = context.getResources();
		int screenLayout = res.getConfiguration().screenLayout;
		screenSize = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

		// We create the thread later, in the surface callback.
		// This prevents "start" from being called on the same thread
		// object multiple times.
	}

	public synchronized void setNumberOfPlayers(int numPlayers) {
		// Log.d(TAG, "setNumberOfPlayers") ;
		this.game = new Game[numPlayers];
		this.ginfo = new GameInformation[numPlayers];
		this.gevents = new GameEvents[numPlayers];
		this.playerNames = new String[numPlayers];
		this.playerStatuses = new PlayerGameStatusDisplayed[numPlayers] ;

		if (thread != null)
			thread.setNumberOfPlayers(numPlayers);
	}
	
	public synchronized void setPlayerName( int playerNum, String name ) {
		this.playerNames[playerNum] = name ;
		if ( thread != null )
			thread.setPlayerNames(playerNames) ;
	}
	
	public synchronized void setPlayerStatus( int playerNum, boolean won, boolean lost, boolean kicked, boolean quit, boolean spectator ) {
		PlayerGameStatusDisplayed status = PlayerGameStatusDisplayed.ACTIVE ;
		if ( won )
			status = PlayerGameStatusDisplayed.WON ;
		else if ( lost )
			status = PlayerGameStatusDisplayed.LOST ;
		else if ( kicked )
			status = PlayerGameStatusDisplayed.KICKED ;
		else if ( quit )
			status = PlayerGameStatusDisplayed.QUIT ;
		else if  ( spectator )
			status = PlayerGameStatusDisplayed.INACTIVE ;
		this.playerStatuses[playerNum] = status ;
		if ( thread != null )
			thread.setPlayerStatuses(playerStatuses) ;
	}

	public synchronized void setFPSView(TextView view) {
		fpsTextView = view;
	}

	public synchronized void setGames(Game[] game, GameInformation[] ginfo,
			GameEvents[] gevents) {
		// Log.d(TAG, "setGames") ;
		for (int i = 0; i < game.length; i++) {
			this.game[i] = game[i];
			this.ginfo[i] = ginfo[i];
			this.gevents[i] = gevents[i];
		}

		// If thread is running, call set.
		if (thread != null) {
			for (int i = 0; i < game.length; i++)
				thread.setGame(i, game[i], ginfo[i], gevents[i]);
		} else if (surfaceCreated) {
			try {
				makeThread();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void setControls(InvisibleControls controls) {
		// Log.d(TAG, "setControls") ;
		this.controls = controls;

		if (thread != null)
			thread.setControls(controls);
	}

	public synchronized void setQuantroSoundPool(QuantroSoundPool pool) {
		this.soundPool = pool;
		if (thread != null)
			thread.setQuantroSoundPool(pool);
	}
	
	public synchronized void setAllGraphics(int detail,
			boolean piecePreviewMinimumProfile,
			int loadImageSize, int loadBackgroundSize, int blit, int scale,
			boolean recycleToVeil, int animations,
			Skin skin, ColorScheme colorScheme,
			Background background, Collection<Background> backgroundShuffle,
			boolean shuffleBackground, long minimumShuffleGap,
			long maximumShuffleGap, boolean shuffleLevelUp ) {
		
		this.mGraphicsSet = true ;
		this.mDetail = detail;
		this.mPiecePreviewMinimumProfile = piecePreviewMinimumProfile;
		this.mLoadImagesSize = loadImageSize;
		this.mLoadBackgroundSize = loadBackgroundSize;
		this.mBlit = blit;
		this.mScale = scale;
		this.mRecycleToVeil = recycleToVeil;
		this.mAnimations = animations;
		
		this.mSkin = skin ;
		this.mColorScheme = colorScheme ;
		
		if ( background != null )
			mBackground = background ;
		if ( backgroundShuffle != null ) {
			this.mBackgroundShuffle.clear() ;
			for ( Background bg : backgroundShuffle ) {
				if ( !mBackgroundShuffle.contains(bg) )
					mBackgroundShuffle.add(bg) ;
			}
		}
		
		mShuffleParam_shuffleBackgrounds = shuffleBackground ;
		mShuffleParam_shuffleBackgroundMinGap = minimumShuffleGap ;
		mShuffleParam_shuffleBackgroundMaxGap = maximumShuffleGap ;
		mShuffleParam_shuffleOnLevelUp = shuffleLevelUp ;
		
		if ( thread != null ) {
			thread.setAllGraphics(
					detail, piecePreviewMinimumProfile,
					loadImageSize, loadBackgroundSize, blit, scale, recycleToVeil, animations,
					skin, colorScheme,
					this.mBackground, this.mBackgroundShuffle,
					shuffleBackground, minimumShuffleGap,
					maximumShuffleGap, shuffleLevelUp ) ;
		}
	}

	public synchronized void setGraphicsPreferences(int detail,
			boolean piecePreviewMinimumProfile,
			int loadImageSize, int loadBackgroundSize, int blit, int scale,
			boolean recycleToVeil, int animations) {
		
		this.mGraphicsSet = true ;
		this.mDetail = detail;
		this.mPiecePreviewMinimumProfile = piecePreviewMinimumProfile;
		this.mLoadImagesSize = loadImageSize;
		this.mLoadBackgroundSize = loadBackgroundSize;
		this.mBlit = blit;
		this.mScale = scale;
		this.mRecycleToVeil = recycleToVeil;
		this.mAnimations = animations;
		if (thread != null)
			thread.setGraphicsPreferences(
					detail,
					piecePreviewMinimumProfile,
					loadImageSize, loadBackgroundSize, blit, scale,
					recycleToVeil, animations);
	}
	
	
	public synchronized void setSkinAndColors( Skin skin, ColorScheme colorScheme ) {
		this.mSkin = skin ;
		this.mColorScheme = colorScheme ;
		if ( thread != null )
			thread.setSkinAndColors( mSkin, colorScheme ) ;
	}
	
	
	/**
	 * Sets the current background and those being shuffled.
	 * 
	 * Passing a 'null' for either parameter will have no effect on its
	 * current assignment.  However, non-shuffling behavior should be
	 * represented as setBackgrounds( bg, Collection{ bg } ), i.e. passing
	 * a shuffle with exactly one element -- the background also provided.
	 * 
	 * Failure to do this will prevent the GameView from ever instantiating itself.
	 * 
	 * Behavior is unspecified if the provided (or current) background is not
	 * present in the backgroundShuffle.
	 * 
	 * @param background
	 * @param backgroundShuffle
	 */
	public synchronized void setBackgrounds(
			Background background, Collection<Background> backgroundShuffle) {
		
		if ( background != null )
			mBackground = background ;
		if ( backgroundShuffle != null ) {
			this.mBackgroundShuffle.clear() ;
			for ( Background bg : backgroundShuffle ) {
				if ( !mBackgroundShuffle.contains(bg) )
					mBackgroundShuffle.add(bg) ;
			}
		}
		
		if ( thread != null )
			thread.setBackgrounds( mBackground, mBackgroundShuffle ) ;
	}
	
	
	public synchronized void setShuffleParameters(
			boolean shuffleBackground,
			long minimumShuffleGap, long maximumShuffleGap,
			boolean shuffleLevelUp ) {
		
		mShuffleParam_shuffleBackgrounds = shuffleBackground ;
		mShuffleParam_shuffleBackgroundMinGap = minimumShuffleGap ;
		mShuffleParam_shuffleBackgroundMaxGap = maximumShuffleGap ;
		mShuffleParam_shuffleOnLevelUp = shuffleLevelUp ;
		
		if ( thread != null ) {
			thread.setShuffleParameters(
					mShuffleParam_shuffleBackgrounds,
					mShuffleParam_shuffleBackgroundMinGap,
					mShuffleParam_shuffleBackgroundMaxGap,
					mShuffleParam_shuffleOnLevelUp
					) ;
		}
	}

	public synchronized void setSoundPreferences(boolean soundControls) {
		this.mSoundControls = soundControls;
		if (thread != null)
			thread.setSoundPreferences(soundControls);
	}

	public synchronized void setGamePreferences(int pieceTips) {
		this.mPieceTips = pieceTips;
		if (thread != null)
			thread.setGamePreferences(pieceTips);
	}

	public synchronized void setPauseViewComponentAdapter(
			PauseViewComponentAdapter adapter) {
		this.pauseViewComponentAdapter = adapter;
	}
	
	
	public synchronized void setPlayerGame( int playerNum ) {
		playerGame = playerNum ;
		if (thread != null) {
			// set the displayed game.
			thread.setDefaultDisplayedGame(playerNum, true) ;
			if (GlobalTestSettings.GAME_SELF_THUMBNAIL)
				thread.setDisplayedGame(playerGame, playerGame);
			else if ( fullScreenOpponent )
				thread.setDisplayedGame(opponentGame, playerGame);
			else
				thread.setDisplayedGame(playerGame, opponentGame);
		}
	}
	
	public synchronized void setOpponentGame( int opponentNum ) {
		opponentGame = opponentNum ;
		if (thread != null) {
			// set the displayed game.
			if (GlobalTestSettings.GAME_SELF_THUMBNAIL)
				thread.setDisplayedGame(playerGame, playerGame);
			else if ( fullScreenOpponent )
				thread.setDisplayedGame(opponentGame, playerGame);
			else
				thread.setDisplayedGame(playerGame, opponentGame);
		}
	}
	
	
	
	public synchronized void setDisplayedGames( int playerNum, int opponentNum ) {
		playerGame = playerNum ;
		opponentGame = opponentNum ;
		
		if (thread != null) {
			// set the displayed game.
			thread.setDefaultDisplayedGame(playerNum, true) ;
			if (GlobalTestSettings.GAME_SELF_THUMBNAIL)
				thread.setDisplayedGame(playerGame, playerGame);
			else if ( fullScreenOpponent )
				thread.setDisplayedGame(opponentGame, playerGame);
			else
				thread.setDisplayedGame(playerGame, opponentGame);
		}
	}
	
	public synchronized void setDisplayOpponentGame( boolean opponent ) {
		fullScreenOpponent = opponent ;
		
		if (thread != null) {
			// set the displayed game.
			if (GlobalTestSettings.GAME_SELF_THUMBNAIL)
				thread.setDisplayedGame(playerGame, playerGame);
			else if ( fullScreenOpponent )
				thread.setDisplayedGame(opponentGame, playerGame);
			else
				thread.setDisplayedGame(playerGame, opponentGame);
		}
	}
	
	public synchronized void setGameThumbnail( Bitmap thumbnail ) {
		this.bitmap_gameThumbnail = thumbnail ;
		if ( bitmap_gameThumbnail != null ) {
			this.canvas_bitmap_gameThumbnail = new Canvas(bitmap_gameThumbnail) ;
			this.canvas_bitmap_gameThumbnail_configCanvas = 
				new BlockDrawerConfigCanvas( 0, 0, thumbnail.getWidth(), thumbnail.getHeight() ) ;
			canvas_bitmap_gameThumbnail_configCanvas.setScale(
					BlockDrawerConfigCanvas.Scale.FIT_X) ;
			canvas_bitmap_gameThumbnail_configCanvas.setAlignment(
					BlockDrawerConfigCanvas.Alignment.CONTENT_TOP_EMPTY) ;
			canvas_bitmap_gameThumbnail_configCanvas.setBackground(
					BlockDrawerConfigCanvas.Background.CLEAR) ;
		}
	}
	
	public synchronized void setChangeListener( ChangeListener listener ) {
		this.changeListener = listener ;
	}

	/* Callback invoked when the surface dimensions change. */
	public synchronized void surfaceChanged(SurfaceHolder holder, int format,
			int width, int height) {
		Log.d(TAG, "surfaceChanged");

		this.surfaceWidth = width;
		this.surfaceHeight = height;
		if (thread != null)
			thread.setSurfaceSize(width, height);
	}

	public synchronized void pause() {
		// Log.d(TAG, "pause") ;
		paused = true;
		if (thread != null)
			thread.pause();
	}

	public synchronized void unpause() {
		paused = false;
		if (thread != null)
			thread.unpause();
	}

	public synchronized void refresh() {
		if (thread != null)
			thread.refresh();
	}

	public synchronized boolean veiledFully() {
		if (thread != null) {
			BlockDrawer bd = thread.blockDrawer;
			BlockDrawerSliceTime sliceTime = thread.mSliceTime == null ? null
					: thread.mSliceTime[thread.defaultDisplayedGame] ;
			if (bd != null) {
				return bd.veiledFully(sliceTime);
			}
		}

		return false;
	}
	
	public synchronized boolean running() {
		return thread != null && ( thread.running && thread.isAlive() ) ;
	}

	public synchronized boolean veiled() {
		return veiled;
	}

	public synchronized void veil() {
		veiled = true;
	}

	public synchronized void unveil() {
		veiled = false;
	}

	/**
	 * Returns whether this GameView is ready to immediately begin ticking when
	 * it is unpaused (or is currently unpaused and ticking).
	 * 
	 * @return
	 */
	public synchronized boolean ready() {
		if (thread == null)
			return false;
		else
			return thread.running && thread.loaded;
	}

	public synchronized boolean isPaused() {
		Log.d(TAG, "isPaused");
		return paused;
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 * 
	 * DANGER! DANGER! Pre-Service refactor, a GameView would have all its Game
	 * objects set during the Activity's onCreate. This would always happen
	 * BEFORE the surface was created (I'm not sure whether that's guaranteed,
	 * but it always worked out that way).
	 * 
	 * With the new Service changes, this method gets called BEFORE we have set
	 * Game objects. Because this method instantiated and started our work
	 * thread, this causes all kinds of null pointer exceptions: almost all
	 * methods in the GameBlockFieldViewThread assume that our game objects have
	 * been set.
	 * 
	 * Fix: as a fix for this, we now simply retain a reference to the
	 * SurfaceHolder, getting things going IF our game references have been set.
	 */
	public synchronized void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated");
		this.holder = holder;
		surfaceCreated = true;

		// Start the thread IF our games have been set.
		if (game != null && game[0] != null) {
			try {
				makeThread();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private synchronized void makeThread() {
		Log.d(TAG, "makeThread");
		// Create, configure, and start the thread.
		thread = new GameBlockFieldViewThread(holder, new Handler() {

			@Override
			public void handleMessage(Message m) {

				GameBlocksSlice slice = null;
				Bitmap b = null;
				Background bg = null ;
				Drawable d = null ;

				switch( m.what ) {
				case WHAT_FRAME_RATE:
					// process frame rate info.
					Log.d(TAG, "handleMessage: WHAT_FRAME_RATE") ;
					if (GlobalTestSettings.GAME_SHOW_FRAME_RATE)
						fpsTextView.setText(frameRateString(mTimeTotal, mTimeMax, mDisplacement));
					else
						fpsTextView.setText(null);
					break ;
					
				case WHAT_SLICE_FALLING_UPDATE:
					try {
						slice = thread.slices[playerGame];
					} catch (Exception e) {
					}
					if (pauseViewComponentAdapter != null)
						pauseViewComponentAdapter.setSlice(slice);
					break ;
				case WHAT_SLICE_NEXT_UPDATE:
					try {
						slice = thread.nextPieceSlices[playerGame];
					} catch (Exception e) {
					}
					if (pauseViewComponentAdapter != null)
						pauseViewComponentAdapter.setSliceNextPiece(slice);
					break ;
				case WHAT_SLICE_RESERVE_UPDATE:
					try {
						slice = thread.reservePieceSlices[playerGame];
					} catch (Exception e) {
					}
					if (pauseViewComponentAdapter != null)
						pauseViewComponentAdapter.setSliceReservePiece(slice);
					break ;
				case WHAT_CONTROLS_THUMBNAIL_UPDATE:
					try {
						d = drawable_controlsThumbnail;
					} catch (Exception e) {
					}
					if (pauseViewComponentAdapter != null)
						pauseViewComponentAdapter.setControlsThumbnail(d);
					if ( d != null && changeListener != null )
						changeListener.gvcl_controlsUpdated(d) ;
					break ;
				case WHAT_START_LOADING:
					boolean isLoading = false;
					try {
						isLoading = thread.loaded == false;
					} catch (Exception e) {
					}
					if (isLoading && pauseViewComponentAdapter != null)
						pauseViewComponentAdapter.setStartLoading();
					break ;
				case WHAT_DONE_LOADING:
					boolean doneLoading = false;
					try {
						doneLoading = thread.loaded;
					} catch (Exception e) {
					}
					if (doneLoading && pauseViewComponentAdapter != null)
						pauseViewComponentAdapter.setFinishedLoading();
					break ;
				case WHAT_THUMBNAIL_UPDATED:
					try {
						if ( bitmap_gameThumbnail != null && changeListener != null ) {
							changeListener.gvcl_thumbnailUpdated( bitmap_gameThumbnail ) ;
						}
					}  catch (Exception e) {}
					break ;
				case WHAT_BACKGROUND_SHUFFLE:
				case WHAT_BACKGROUND_CHANGED:
					try {
						bg = (Background) m.obj ;
						if ( bitmap_gameThumbnail != null && changeListener != null ) {
							changeListener.gvcl_backgroundChanged( bg ) ;
						}
					}  catch (Exception e) {}
					break ;
				}
			}
		});
		// Log.d(TAG, "thread created") ;

		if (surfaceHeight != -1) {
			thread.setSurfaceSize(surfaceWidth, surfaceHeight);
		}

		if (this.game != null) {
			thread.setNumberOfPlayers(game.length);
			for (int i = 0; i < this.game.length; i++) {
				if (this.game[i] != null)
					thread.setGame(i, game[i], ginfo[i], gevents[i]);
			}
		}
		if (this.playerNames != null)
			thread.setPlayerNames(playerNames) ;
		if (this.playerStatuses != null)
			thread.setPlayerStatuses(playerStatuses) ;
		if (this.soundPool != null)
			thread.setQuantroSoundPool(soundPool);
		if (this.controls != null)
			thread.setControls(controls);
		if ( this.mGraphicsSet && this.mSkin != null && this.mBackground != null ) {
			// call the unified method if we can
			thread.setAllGraphics(
					mDetail, mPiecePreviewMinimumProfile,
						mLoadImagesSize, mLoadBackgroundSize, mBlit, mScale, mRecycleToVeil, mAnimations,
					mSkin, mColorScheme,
					mBackground, mBackgroundShuffle,
					mShuffleParam_shuffleBackgrounds, mShuffleParam_shuffleBackgroundMinGap,
						mShuffleParam_shuffleBackgroundMaxGap, mShuffleParam_shuffleOnLevelUp) ;
		} else {
			// call the individual piece-wise versions
			if (this.mGraphicsSet)
				thread.setGraphicsPreferences(
						mDetail, mPiecePreviewMinimumProfile,
						mLoadImagesSize, mLoadBackgroundSize, mBlit, mScale, mRecycleToVeil, mAnimations);
			if (this.mSkin != null)
				thread.setSkinAndColors( mSkin, mColorScheme ) ;
			if ( this.mBackground != null && this.mBackgroundShuffle.size() > 0 )
				thread.setBackgrounds( mBackground, mBackgroundShuffle ) ;
			thread.setShuffleParameters(
					mShuffleParam_shuffleBackgrounds,
					mShuffleParam_shuffleBackgroundMinGap,
					mShuffleParam_shuffleBackgroundMaxGap,
					mShuffleParam_shuffleOnLevelUp
					) ;
		}
		
		thread.setSoundPreferences(mSoundControls);
		thread.setGamePreferences(mPieceTips);
		
		thread.setDefaultDisplayedGame(playerGame, true) ;
		if (GlobalTestSettings.GAME_SELF_THUMBNAIL)
			thread.setDisplayedGame(playerGame, playerGame);
		else if ( fullScreenOpponent )
			thread.setDisplayedGame(opponentGame, playerGame);
		else
			thread.setDisplayedGame(playerGame, opponentGame);

		if (this.paused)
			thread.pause();
		else
			thread.unpause();

		if (threadGameState != null) {
			for (int i = 0; i < threadGameState.length; i++) {
				if (this.threadGameState[i] > Integer.MIN_VALUE)
					thread.gameState[i] = this.threadGameState[i];
			}
		}

		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.forceUpdate();
		thread.setCanvasOK(true);
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	public synchronized void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed");
		surfaceCreated = false;
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		if (thread != null) {
			Log.d(TAG, "pausing");
			thread.pause();
			Log.d(TAG, "setting canvas OK false");
			thread.setCanvasOK(false);
			Log.d(TAG, "waiting for thread to terminate");
			ThreadSafety.waitForThreadToTerminate(thread, 2000);
			Log.d(TAG, "terminated");
			// Null out the thread.
			threadGameState = thread.gameState; // Faster to just keep a
												// reference. This is an array
												// of ints.
		}
		thread = null;
		holder = null ;
	}

	public void recycle( boolean permanent ) {
		Log.d(TAG, "recycle") ;
		GameBlockFieldViewThread localThread = thread;
		if (localThread != null) {
			localThread.running = false;
			BlockDrawer bd;

			synchronized (recycleMutex) {
				bd = localThread.blockDrawer;
				if (bd != null)
					bd.recycle();
				bd = localThread.nextPieceBlockDrawer;
				if (bd != null)
					bd.recycle();
				bd = localThread.reservePieceBlockDrawer;
				if (bd != null)
					bd.recycle();
				bd = localThread.blockDrawerMini;
				if (bd != null)
					bd.recycle();

				thread.releaseBitmaps();
				thread.releaseUIElements();
			}

			try {
				if (this.pauseViewComponentAdapter != null)
					pauseViewComponentAdapter.setControlsThumbnail(null);
				if (drawable_controlsThumbnail != null) {
					if ( drawable_controlsThumbnail instanceof BitmapDrawable ) {
						Bitmap b = ((BitmapDrawable)drawable_controlsThumbnail).getBitmap() ;
						if ( !b.isRecycled() )
							b.recycle() ;
					}
				}
				drawable_controlsThumbnail = null;
			} catch (Exception e) {
			}

			localThread.view_invisibleControls = null;
			controls = null;

			recycled = true ;

			System.gc();

			// forget all our outward-facing references?
		}
		
		thread = null ;
		
		if ( permanent ) {
			// Game info
			game = null ;
			ginfo = null ;
			gevents = null ;
			
			playerNames = null ;
			playerStatuses = null ;

			controls = null ;
			soundPool = null ;
			pauseViewComponentAdapter = null ;

			// Skin and Color Scheme
			mColorScheme = null ;
			
			drawable_controlsThumbnail = null ;
			
			bitmap_gameThumbnail = null ;
			ChangeListener changeListener = null ;
			canvas_bitmap_gameThumbnail = null ;
			canvas_bitmap_gameThumbnail_configCanvas = null ;
			
			fpsTextView = null ;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			Log.d(TAG, "finalizing gameView");
		} finally {
			super.finalize();
		}
	}

}
