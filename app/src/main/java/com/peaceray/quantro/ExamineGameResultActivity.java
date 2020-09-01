package com.peaceray.quantro;

import java.io.Serializable;

import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.q.QOrientations;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.Scores;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.view.button.strip.DialogButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.game.BlockFieldView;
import com.peaceray.quantro.view.game.DrawSettings;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TextView;


/**
 * An ExamineGameResultActivity allows us to review GameResult object(s),
 * and perform some specific action regarding them.  However, we do not
 * directly perform that action; we instead send a result back to the
 * Activity which launched this Activity for a result.
 * 
 * Our format differs slightly between examination cases.  Our specific
 * examination styles are (as of 12/27):
 * 
 * STYLES:
 * Saved Game.	This is a single player game result representing a saved,
 * 		in-progress game.  We show the blockfield, current level and score,
 * 		and allow the user to "play," "delete," "cancel."
 * 
 * Single Player Game Over.  This indicates that a single player game has
 * 		just ended.  We show a snapshot of the final game state, and score
 * 		and level.  The user may "replay" (from some specific state) or
 * 		"quit."
 * 
 * Single Player Level Complete.  This indicates that a single player game has
 * 		just advanced a level.  Specifically, we use this to refer to a 
 * 		"progressive" game where the "next level" is a direct continuation of
 * 		the previous level, possibly with a change in timing or block distribution.
 * 		We require 2 (or more, but those >2 are ignored for now) GameResult
 * 		objects.  These should be provided in an array in reverse chronological
 * 		order; in other words, item 0 is the "current" GameResult, and item 1 is
 * 		the "previous" GameResult.  The user is given three options: "continue"
 * 		(play from the current state), "replay" (play from the previous state,
 * 		erasing the current result) and "quit" (quit from playing; possibly
 * 		the creator forces the user to have chosen "play," possibly the user will
 * 		get this same choice again when they return).
 * 
 * Multiplayer Game Over: This indicates that a multiplayer game has just
 * 		ended.  The GameResult object provided gives all the information we
 * 		need, including player names, scores, levels, who won, who lost,
 * 		etc.  Remember that the GameResult includes a "localPlayerSlot" to
 * 		indicate which information belongs to the current player.  Because
 * 		this game is over, and we can't simply start another one (multiplayer,
 * 		remember), there is only one option offered to the player - "Return to Lobby."
 * 
 * @author Jake
 *
 */
public class ExamineGameResultActivity extends QuantroActivity {
	
	private static final String TAG = "ExamineGameResult" ;
	
	// EXTRAS:
	// These extras are required.  One must specify a style, and either a 
	// GameResult or an array of them.  If a single GameResult is specified,
	// only it is examined.
	public static final String INTENT_EXTRA_STYLE 			= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_EXTRA_STYLE" ;
	public static final String INTENT_EXTRA_GAME_RESULT	 	= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_EXTRA_GAME_RESULT" ;
	public static final String INTENT_EXTRA_GAME_RESULT_ARRAY = "com.peaceray.quantro.ExamineGameResultActivity.INTENT_EXTRA_GAME_RESULT_ARRAY" ;
	public static final String INTENT_EXTRA_PLAYER_COLORS 	= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_EXTRA_PLAYER_COLORS" ;
	public static final String INTENT_EXTRA_ECHO 			= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_EXTRA_ECHO" ;
	public static final String INTENT_EXTRA_FULLSCREEN 		= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_EXTRA_FULLSCREEN" ;
	
	public static final String INTENT_EXTRA_GAME_SETTINGS 	= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_EXTRA_GAME_SETTINGS" ;
	
	
	// STYLES:
	// When launched, the user must specify a display style.  While
	// its possible that we could infer the situation from the provided
	// GameResult, we prefer this explicit method, because it allows better
	// forward-compatibility and error checking, and finally moves the responsibility
	// from determining style from this activity (where it must be determined
	// after the fact) to the creating Activity (where it should be very obvious).
	public static final int STYLE_SAVED_GAME = 0 ; 
	public static final int STYLE_SINGLE_PLAYER_GAME_OVER = 1 ; 
	public static final int STYLE_MULTI_PLAYER_GAME_OVER = 2 ; 
	public static final int STYLE_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER = 3 ; 
	public static final int STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER = 4 ; 
	
	
	// RESULTS:
	// When this activity ends, it will return a result (or the default canceled
	// result).  We store the "result" as an extra.
	public static final String INTENT_RESULT_EXTRA		= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_RESULT_EXTRA" ;
	public static final String INTENT_RESULT_GAME_RESULT_REPLAY_ARRAY_INDEX 	= "com.peaceray.quantro.ExamineGameResultActivity.INTENT_RESULT_GAME_RESULT_REPLAY_ARRAY_INDEX" ;
	// Result types
	public static final int INTENT_RESULT_CANCEL 	= 0 ;
	public static final int INTENT_RESULT_CONTINUE	= 1 ;
	public static final int INTENT_RESULT_REPLAY 	= 2 ;
	public static final int INTENT_RESULT_REWIND 	= 3 ;
	public static final int INTENT_RESULT_DELETE 	= 4 ;
	
	
	private static final int BUTTON_GONE 			= -1 ;
	private static final int BUTTON_TYPE_CANCEL 	= 0 ;
	private static final int BUTTON_TYPE_QUIT 		= 1 ;
	private static final int BUTTON_TYPE_QUIT_REWINDABLE 	= 2 ;
	private static final int BUTTON_TYPE_RETURN_TO_LOBBY = 3 ;
	private static final int BUTTON_TYPE_PLAY 		= 4 ;
	private static final int BUTTON_TYPE_CONTINUE	= 5 ;
	private static final int BUTTON_TYPE_REPLAY 	= 6 ;
	private static final int BUTTON_TYPE_REWIND		= 7 ;
	private static final int BUTTON_TYPE_DELETE 	= 8 ;
	
	
	private int mStyle ;
	private GameResult [] mGRArray ;
	private int [] mGRArrayOrder ;	// indexes into mGRArray, in the appropriate display order.
	private GameSettings mGameSettings ;
	
	// player colors 
	private int [] mPlayerColors ;
	
	// RESOURCES!
	private Resources res ;
	
	private boolean mMusicPlayingBeforePause ;
	private boolean mResolving ;
	// set upon the selection of an action.  Lets us know in onPause, etc., whether
	// we are finishing up because of a user selection or a sleep.
	
	// IDs!  We use these IDs to find views and set their content.
	private int mIDPlayerName ;
	private int mIDDifficulty ;
	private int mIDWinLose ;
	private int mIDConfig ;
	private int mIDBlockFieldView ;
	private int mIDTimeFull ;
	private int mIDTimeNumber ;
	private int mIDLevelFull ;
	private int mIDLevelNumber ;
	private int mIDScoreFull ;
	private int mIDScoreNumber ;
	private int mIDRowsFull ;
	private int mIDRowsNumber ;
	
	private DialogManager mDialogManager ;
	
	private Skin mSkin ;
	private ColorScheme mColorScheme ;
	
	private QuantroSoundPool mSoundPool ;
	private boolean mSoundControls ;
	
	
	////////////////////////////////////////////////////////////////////////////
	// SCORE SUBMISSION:
	//
	// For submitting scores to our global high-score list.
	// 
	
	private static final int SCORE_SUBMISSION_STATE_NONE = 0 ;
	private static final int SCORE_SUBMISSION_STATE_NO_LEADERBOARD = 1 ;	// there is no leaderboard for this result.
	private static final int SCORE_SUBMISSION_STATE_SIGNED_OUT = 2 ;		// there is a leaderboard but we are signed out.
	private static final int SCORE_SUBMISSION_STATE_SUBMITTING = 3 ;		// we are waiting for a submission response.
	private static final int SCORE_SUBMISSION_STATE_SUBMITTED = 4 ;			// submission is complete.  Check our result.

	private static final int SCORE_SUBMISSION_STATUS_OK = 0 ;	// TODO use GamesClient.STATUS_OK when/if fixed
	private static final int SCORE_SUBMISSION_STATUS_NETWORK_ERROR_OPERATION_DEFERRED = 1; 	// TODO use GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED when/if fixed
	
	private int mScoreSubmissionState = SCORE_SUBMISSION_STATE_NONE ;
	private String mScoreSubmissionLeaderboardID ;
	private long mScoreSubmissionRawScore ;
	private int mScoreSubmissionStatusCode = SCORE_SUBMISSION_STATUS_OK ;	// if REALLY OK, then SubmissionResult will be non-null.
	private Object mScoreSubmissionResult ;	// TODO was a SubmitScoreResult; now deprecated
	
	private boolean mAchievementsPushed = false ;
	
	// Score Submission areas.
	private ViewGroup mScoreSubmissionView ;
	private View mScoreSubmissionSpinner ;
	private View mScoreSubmissionSignInButton ;
	private View mScoreSubmissionBadge ;
	private TextView mScoreSubmissionText ;
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	@Override
	synchronized public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState) ;
		setupQuantroActivity( QUANTRO_ACTIVITY_UNKNOWN, QUANTRO_ACTIVITY_CONTENT_PERMANENT_DIALOG ) ;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.dialog) ;
		ViewGroup content = (ViewGroup)findViewById(R.id.dialog_content) ;
		getLayoutInflater().inflate(R.layout.examine_game_result, content) ;
		
		mDialogManager = new DialogManager(this) ;
		
		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		
		Intent intent = getIntent() ;
		
		// Load style, game result, etc.
		GameResult gr = null ;
		mStyle = intent.getIntExtra(INTENT_EXTRA_STYLE, -1) ;
		if ( mStyle == -1 ) {
			Log.e(TAG, "Style not set in launching Intent.") ;
			finish() ;
		}
		if ( intent.hasExtra(INTENT_EXTRA_GAME_RESULT) )
			gr = (GameResult)intent.getSerializableExtra(INTENT_EXTRA_GAME_RESULT) ;
		if ( intent.hasExtra(INTENT_EXTRA_GAME_RESULT_ARRAY) ) {
			Object [] objs = (Object [])intent.getSerializableExtra(INTENT_EXTRA_GAME_RESULT_ARRAY) ;
			mGRArray = new GameResult[objs.length] ;
			for ( int i = 0; i < mGRArray.length; i++ )
				mGRArray[i] = (GameResult)objs[i] ;
		}
		
		if ( gr == null && mGRArray == null ) {
			Log.e(TAG, "No GameResults provided") ;
			finish() ;
		}
		if ( gr != null && mGRArray != null ) {
			Log.e(TAG, "GameResults provided as both object and array; should provide only one.") ;
			finish() ;
		}
		
		if ( mGRArray == null ) {
			mGRArray = new GameResult[1] ;
			mGRArray[0] = gr ;
		}
		
		// ORGANIZE GR Array.
		mGRArrayOrder = sortedOrderPlayers( mGRArray[0] ) ;
		
		if ( intent.hasExtra(INTENT_EXTRA_GAME_SETTINGS) )
			mGameSettings = (GameSettings) intent.getSerializableExtra(INTENT_EXTRA_GAME_SETTINGS) ;
		
		mSkin = GameModes.numberQPanes( mGRArray[0].getGameInformation(0) ) == 1
				? QuantroPreferences.getSkinRetro(this)
				: QuantroPreferences.getSkinQuantro(this) ;
		
		mPlayerColors = intent.getIntArrayExtra(INTENT_EXTRA_PLAYER_COLORS) ;
		
		mResolving = false ;
		mMusicPlayingBeforePause = false ;
		
		res = getResources() ;
		mIDPlayerName = R.id.examine_game_result_name ;
		mIDDifficulty = R.id.examine_game_result_difficulty ;
		mIDWinLose = R.id.examine_game_result_win_lose ;
		mIDConfig = R.id.examine_game_result_config ;
		mIDBlockFieldView = R.id.examine_game_result_block_field_view ;
		mIDTimeFull = R.id.examine_game_result_time_full ;
		mIDTimeNumber = R.id.examine_game_result_time_number ;
		mIDLevelFull = R.id.examine_game_result_level_full ;
		mIDLevelNumber = R.id.examine_game_result_level_number ;
		mIDScoreFull = R.id.examine_game_result_score_full ;
		mIDScoreNumber = R.id.examine_game_result_score_number ;
		mIDRowsFull = R.id.examine_game_result_rows_full ;
		mIDRowsNumber = R.id.examine_game_result_rows_number ;
		
		loadExamineLayout( content ) ;
		// configure this layout
		setTitle( content ) ;
		setQueryString( content ) ;
		setFilterableContentColor( getFilterColor(), content ) ;
    	// set the content border.
    	Drawable background = this.getDialogBackground() ;
    	if ( background != null )
    		VersionSafe.setBackground( findViewById(R.id.dialog_content), background ) ;
    	// set buttons
    	DialogButtonStrip strip = (DialogButtonStrip)content.findViewById(R.id.dialog_content_button_strip) ;
    	switch( mStyle ) {
		case STYLE_SAVED_GAME:
			setButton(strip, DialogButtonStrip.BUTTON_POSITIVE, BUTTON_TYPE_PLAY) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEUTRAL, BUTTON_TYPE_DELETE) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEGATIVE, BUTTON_TYPE_CANCEL) ;
			break ;
		case STYLE_SINGLE_PLAYER_GAME_OVER:
			setButton(strip, DialogButtonStrip.BUTTON_POSITIVE, BUTTON_TYPE_REPLAY) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEUTRAL, BUTTON_TYPE_QUIT) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEGATIVE, BUTTON_GONE) ;
			break ;
		case STYLE_MULTI_PLAYER_GAME_OVER:
			setButton(strip, DialogButtonStrip.BUTTON_POSITIVE, BUTTON_TYPE_RETURN_TO_LOBBY) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEUTRAL, BUTTON_GONE) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEGATIVE, BUTTON_GONE) ;
			break ;
		case STYLE_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER:
			// Progression style level-up
			setButton(strip, DialogButtonStrip.BUTTON_POSITIVE, BUTTON_TYPE_CONTINUE) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEUTRAL, BUTTON_TYPE_REWIND) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEGATIVE, BUTTON_TYPE_QUIT) ;
			break ;
		case STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER:
			// Progression style game over
			setButton(strip, DialogButtonStrip.BUTTON_POSITIVE, BUTTON_TYPE_REWIND) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEUTRAL, BUTTON_TYPE_QUIT_REWINDABLE) ;
			setButton(strip, DialogButtonStrip.BUTTON_NEGATIVE, BUTTON_GONE) ;
			break ;
		}
    	
    	// Sound pool?
    	mSoundPool = ((QuantroApplication)getApplication()).getSoundPool(this) ;
    	
    	
    	// Score submission references?
    	mScoreSubmissionView = (ViewGroup) findViewById(R.id.examine_game_result_score_submission) ;
    	mScoreSubmissionSpinner = findViewById(R.id.examine_game_result_score_submission_spinner) ;
    	mScoreSubmissionSignInButton = findViewById(R.id.examine_game_result_score_submission_sign_in_button) ;
    	mScoreSubmissionBadge = findViewById(R.id.examine_game_result_score_submission_badge) ;
    	mScoreSubmissionText = (TextView)findViewById(R.id.examine_game_result_score_submission_text) ;
    	
    	// Score submission settings?
    	if ( mScoreSubmissionView != null
    			&& ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER || mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
    			&& Scores.isLeaderboardSupported(mGRArray[0]) ) {
    		mScoreSubmissionLeaderboardID = Scores.getLeaderboardID(mGRArray[0]) ;
    		mScoreSubmissionRawScore = Scores.getRawScore(mGRArray[0]) ;
    		mScoreSubmissionResult = null ;
    		
    		// set listeners...
    		mScoreSubmissionSignInButton.setOnClickListener( new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					((QuantroApplication)getApplication()).gpg_beginUserInitiatedSignIn() ;
				}
			}) ;
    		
    		mScoreSubmissionView.setOnClickListener( new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if ( mScoreSubmissionState == SCORE_SUBMISSION_STATE_SUBMITTED ) {
						startActivityForResult(
								((QuantroApplication)getApplication()).gpg_getLeaderboardIntent(mScoreSubmissionLeaderboardID),
								IntentForResult.UNUSED);
					}
				}
    		}) ;
    	} else {
    		mScoreSubmissionState = SCORE_SUBMISSION_STATE_NO_LEADERBOARD ;
    	}
    	
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume() ;
		if ( mMusicPlayingBeforePause )
			((QuantroApplication)getApplication()).getSoundPool(this).playMusic() ;
		
		if ( ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER || mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
				&& !((QuantroApplication)getApplication()).gpg_isSignedIn()
				&& mScoreSubmissionLeaderboardID != null ) {
			// we're not signed in, but this game result supports leaderboards.
			mScoreSubmissionState = SCORE_SUBMISSION_STATE_SIGNED_OUT ;
		}
		
		// Now's a good time to start a submission!
		if ( ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER || mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
				&& ((QuantroApplication)getApplication()).gpg_isSignedIn()
				&& mScoreSubmissionLeaderboardID != null
				&& ( mScoreSubmissionState == SCORE_SUBMISSION_STATE_NONE
						|| mScoreSubmissionState == SCORE_SUBMISSION_STATE_SUBMITTING ) ) {
			
			submitScore() ;
		}
		
		refreshExamineLayout(findViewById(R.id.examine_game_result)) ;
		refreshScoreSubmissionViews() ;
		
		onGooglePlaySignInUpdated( ((QuantroApplication)getApplication()).gpg_isSignedIn() ) ;
        
        // Reveal our dialogs, in case they were previously hidden.
        mDialogManager.revealDialogs() ;
        
        // Sound controls?
        mSoundControls = QuantroPreferences.getSoundControls(this) ;
	}
	
	protected void onPause() {
		super.onPause() ;
		
		if ( !mResolving ) {
			// not resolving an action; instead, we are pausing for whatever reason.
			// Pause the music too, if it's playing.
			mMusicPlayingBeforePause = mSoundPool.isPlayingMusic() ;
			if ( mMusicPlayingBeforePause )
				mSoundPool.pauseMusic() ;
			
	        // Reveal our dialogs, in case they were previously hidden.
	        mDialogManager.hideDialogs() ;
		}
	}
	
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	if ( mSoundControls )
        		mSoundPool.menuButtonBack() ;
            // Whelp, time to quit.  We should note that this is a resolution
        	// (albeit one with default behavior) so we don't accidentally pause
        	// music when it isn't appropriate to.
        	mResolving = true ;

            Intent i = new Intent() ;
			Intent intentIn = getIntent() ;
			if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
				Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
				i.putExtra(INTENT_EXTRA_ECHO, s) ;
			}
			i.putExtra(INTENT_RESULT_EXTRA, INTENT_RESULT_CANCEL) ;
	    	setResult(RESULT_OK, i) ;
	    	finish() ;
	    	return true ;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    
    /**
     * Sorts the game results from winningest-to-losingest.  List
     * first those game results which have 'won', then those which
     * 'lost'.  Within each group, we order by milliseconds spent
     * in-game (descending).  Thus we assume that, among those
     * players who won, the "best" winner was the one who lasted the
     * longest.
     * 
     * Players who quit (without winning or losing) get the worst treatment.
     * 
     * @param gr
     */
    private int [] sortedOrderPlayers( GameResult gr ) {
    	int [] order = new int[gr.getNumberOfPlayers()] ;
    	for ( int i = 0; i < gr.getNumberOfPlayers(); i++ ) {
    		order[i] = i ;
    	}
    	// just use a bubble sort.  We don't ever expect more than 6 players
    	// in one game, so efficiency isn't an issue.
    	boolean changed = true ;
    	while( changed ) {
    		changed = false ;
    		for ( int i = 0; i < gr.getNumberOfPlayers() -1; i++ ) {
    			// swap j, k
    			int j = order[i] ;
    			int k = order[i+1] ;
    			
    			int rankJ = rankPlayerOutcome( gr, j ) ;
    			int rankK = rankPlayerOutcome( gr, k ) ;
    			// Different "win / lost" status?
    			if ( rankJ < rankK || ( rankJ == rankK
    							&& gr.getTimeInGameTicks(j)
    									< gr.getTimeInGameTicks(k) ) ) {
    				changed = true ;
    				order[i] = k ;
    				order[i+1] = j ;
    			}
    		}
    	}
    	
    	for ( int i = 0; i < gr.getNumberOfPlayers(); i++ ) {
    		int player = order[i] ;
    		Log.d(TAG, "player " + player + " named " + gr.getName(player) + " has rank " + rankPlayerOutcome( gr, player ) + " and time in game ticks " + gr.getTimeInGameTicks(player) ) ;
    	}
    	
    	return order ;
    }
    
    
    /**
     * Returns an integer encoding the player's game outcome (won, lost, quit)
     * as an integer.  Higher numbers indicate a better result for the player:
     * winning is better than losing, which is better than quitting.
     * 
     * @param gr
     * @param player
     * @return
     */
    private int rankPlayerOutcome( GameResult gr, int player ) {
    	if ( gr.getWon(player) )
    		return 2 ;
    	if ( gr.getLost(player) )
    		return 1 ;
    	if ( !gr.getQuit(player) )
    		return 0 ;
    	return -1 ;
    }
    
    
    /**
     * Submits our GameResult as a score.
     */
    private void submitScore() {
    	if ( !isScoreSubmitted() ) {
	    	// submit.
			mScoreSubmissionState = SCORE_SUBMISSION_STATE_SUBMITTING ;
			((QuantroApplication)getApplication()).gpg_submitScoreImmediate(
					this, mScoreSubmissionLeaderboardID, mScoreSubmissionRawScore) ;
			// we also take this opportunity to sync achievements.  We
			// ignore rate-limiting for this, but only if it's the first
			// attempt.
			Achievements.push((QuantroApplication)getApplication(), !mAchievementsPushed) ;
			mAchievementsPushed = true ;
    	}
    }
    
    
    private boolean isScoreSubmitted() {
    	if ( mScoreSubmissionStatusCode == SCORE_SUBMISSION_STATUS_NETWORK_ERROR_OPERATION_DEFERRED )
    		return true ;
    	if ( mScoreSubmissionStatusCode == SCORE_SUBMISSION_STATUS_OK && this.mScoreSubmissionResult != null ) {
    		return true ;
    	}
    	
    	return false ;
    }
    
    
    private void refreshScoreSubmissionViews() {
    	if ( mScoreSubmissionView == null )
    		return ;
    	
    	switch ( mScoreSubmissionState ) {
    	case SCORE_SUBMISSION_STATE_NONE:
    		// spinner...?
    		mScoreSubmissionView.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionSpinner.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionSignInButton.setVisibility(View.GONE) ;
    		mScoreSubmissionBadge.setVisibility(View.GONE) ;
    		mScoreSubmissionText.setVisibility(View.GONE) ;
    		break ;
    	case SCORE_SUBMISSION_STATE_NO_LEADERBOARD:
    		// don't display anything.
    		mScoreSubmissionView.setVisibility(View.GONE) ;
    		break ;
    	case SCORE_SUBMISSION_STATE_SIGNED_OUT:
    		// sign-in button and some helpful text.
    		mScoreSubmissionView.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionSpinner.setVisibility(View.GONE) ;
    		mScoreSubmissionSignInButton.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionBadge.setVisibility(View.GONE) ;
    		mScoreSubmissionText.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionText.setText(R.string.gamehelper_examine_signed_out) ;
    		break ;
    	case SCORE_SUBMISSION_STATE_SUBMITTING:
    		// spinner...?
    		mScoreSubmissionView.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionSpinner.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionSignInButton.setVisibility(View.GONE) ;
    		mScoreSubmissionBadge.setVisibility(View.GONE) ;
    		mScoreSubmissionText.setVisibility(View.GONE) ;
    		break ;
    	case SCORE_SUBMISSION_STATE_SUBMITTED:
    		// badge and text describing status and score result.
			mScoreSubmissionText.setText(R.string.gamehelper_examine_deferred) ;
			// TODO restore custom text based on score result
			/*
    		if ( this.mScoreSubmissionStatusCode == SCORE_SUBMISSION_STATUS_NETWORK_ERROR_OPERATION_DEFERRED ) {
    			mScoreSubmissionText.setText(R.string.gamehelper_examine_deferred) ;
    		} else {
    			// set the appropriate "Best yet" text.
    			if ( mScoreSubmissionResult.getScoreResult(LeaderboardVariant.TIME_SPAN_ALL_TIME).newBest ) {
    				mScoreSubmissionText.setText(R.string.gamehelper_examine_top_ever) ;
    			} else if ( mScoreSubmissionResult.getScoreResult(LeaderboardVariant.TIME_SPAN_WEEKLY).newBest ) {
    				mScoreSubmissionText.setText(R.string.gamehelper_examine_top_weekly) ;
    			} else if ( mScoreSubmissionResult.getScoreResult(LeaderboardVariant.TIME_SPAN_DAILY).newBest ) {
    				mScoreSubmissionText.setText(R.string.gamehelper_examine_top_daily) ;
    			} else {
    				mScoreSubmissionText.setText(R.string.gamehelper_examine_top_none) ;
    			}
    		}
			 */
    		mScoreSubmissionView.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionSpinner.setVisibility(View.GONE) ;
    		mScoreSubmissionSignInButton.setVisibility(View.GONE) ;
    		mScoreSubmissionBadge.setVisibility(View.VISIBLE) ;
    		mScoreSubmissionText.setVisibility(View.VISIBLE) ;
    		break ;
    	}

    	// TODO: do we display anything at all?  Maybe
    	// there's a preference the user can set to not display this stuff?
    }

    
	
	private View loadExamineLayout(View v) {
		// Perform necessary settings for our views.  We don't expect that any of this
		// is going to change over time, so there's no reason not
		// to perform all our setup here.
		ViewGroup vg = (ViewGroup)v.findViewById(R.id.examine_game_result_field_view_group) ;
		// this (empty) view group provides a place for us to position
		// specific game information content.
		
		// Load a view to place within this view group according to our style and
		// GameResults.
		
		View contentView ;
		if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER || mGRArray.length > 1 ) {
			int numFields = Math.max( mGRArray.length, mGRArray[0].getNumberOfPlayers() ) ;
			// Load the appropriate field layout; either 2 or 6.
			if ( numFields <= 2 )
				contentView = getLayoutInflater().inflate(R.layout.examine_game_result_two_field, vg, true);
			else
				contentView = getLayoutInflater().inflate(R.layout.examine_game_result_six_field, vg, true) ;
			
			if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER ) {
				for ( int i = 0; i < 6; i++ ) {
					int idTextLayout = 0, idBlockField= 0, idBlockFieldContainer = 0 ;
					switch( playerRankOrderToExamineViewNumber( i, mGRArrayOrder.length ) ) {
					case 0:
						idTextLayout = R.id.examine_game_result_text_layout_first ;
						idBlockField = R.id.examine_game_result_block_field_view_first ;
						idBlockFieldContainer = R.id.examine_game_result_block_field_container_first ;
						break ;
					case 1:
						idTextLayout = R.id.examine_game_result_text_layout_second ;
						idBlockField = R.id.examine_game_result_block_field_view_second ;
						idBlockFieldContainer = R.id.examine_game_result_block_field_container_second ;
						break ;
					case 2:
						idTextLayout = R.id.examine_game_result_text_layout_third ;
						idBlockField = R.id.examine_game_result_block_field_view_third ;
						idBlockFieldContainer = R.id.examine_game_result_block_field_container_third ;
						break ;
					case 3:
						idTextLayout = R.id.examine_game_result_text_layout_4th ;
						idBlockField = R.id.examine_game_result_block_field_view_4th ;
						idBlockFieldContainer = R.id.examine_game_result_block_field_container_4th ;
						break ;
					case 4:
						idTextLayout = R.id.examine_game_result_text_layout_5th ;
						idBlockField = R.id.examine_game_result_block_field_view_5th ;
						idBlockFieldContainer = R.id.examine_game_result_block_field_container_5th ;
						break ;
					case 5:
						idTextLayout = R.id.examine_game_result_text_layout_6th ;
						idBlockField = R.id.examine_game_result_block_field_view_6th ;
						idBlockFieldContainer = R.id.examine_game_result_block_field_container_6th ;
						break ;
					}
					
					if ( i < mGRArrayOrder.length ) {
						int slot = mGRArrayOrder[i] ;
						int rank = mGRArray[0].getWon(slot) || mGRArray[0].getLost(slot)
								? i : -1 ;		// if didn't win or lose, is a quiter.
						setGameResultContent(
								contentView.findViewById(idTextLayout),
								contentView.findViewById(idBlockField),
								mGRArray[0], slot, rank ) ;
					} else {
						View view = contentView.findViewById(idTextLayout) ;
						if ( view != null )
							view.setVisibility(View.INVISIBLE) ;
						view = contentView.findViewById(idBlockField) ;
						if ( view != null )
							view.setVisibility(View.GONE) ;
						view = contentView.findViewById(idBlockFieldContainer) ;
						if ( view != null )
							view.setVisibility(View.INVISIBLE) ;
					}
				}
				
			}
			else {
				// Otherwise, there must be more than one GameResult.
				setGameResultContent( 
						contentView.findViewById(R.id.examine_game_result_text_layout_first),
						contentView.findViewById(R.id.examine_game_result_block_field_view_first),
						mGRArray[0], 0, -1 ) ;
				setGameResultContent( 
						contentView.findViewById(R.id.examine_game_result_text_layout_second),
						contentView.findViewById(R.id.examine_game_result_block_field_view_second),
						mGRArray[1], 0, -1 ) ;
			}
		}
		else {
			contentView = getLayoutInflater().inflate(R.layout.examine_game_result_one_field, vg, true);
			setGameResultContent( contentView, contentView, mGRArray[0], 0 , -1 ) ;
		}
		
		return v ;
	}
	
	
	/**
	 * We sometimes order game results differently that simple 1-1, 2-2, 3-3, etc.
	 * The reason for this is that different "view numbers" have dedicated screen
	 * locations designed for a "full roster", and it may not be appropriate
	 * to display in those exact locations with less-than-full rosters.
	 * 
	 * Note: we provied positions for rankOrders that exceed the total players.
	 * Why?  Because we might use this positioning data to set the relevant views
	 * as INVISIBLE or GONE.  We try to spread the players across all available
	 * views, and at least, we don't allow two different players to "double up"
	 * in the same view.
	 * 
	 * @param rankOrder  Player rank (in order of descending success), in the range
	 * 		[0, ..., 5].  Player 0 is the winner, 5 the losing-est.
	 * @param totalPlayers The total number of players in the game, [1, ..., 6].
	 * @return
	 */
	private int playerRankOrderToExamineViewNumber( int rankOrder, int totalPlayers ) {
		switch( rankOrder ) {
		case 0:
			return 0 ;
		case 1:
			return 1 ;
		case 2:
			return 2 ;
		case 3:
			if ( totalPlayers == 4 )
				return 4 ;		// centered between 3 and 5.
			return 3 ;			// left-side of 3,4,5.
		case 4:
			if ( totalPlayers == 4 )
				return 3 ;		// unused, but the players rank 3 should have been.
			else if ( totalPlayers == 5 )
				return 5 ;		// right-side of 3,4,5.
			return 4 ;			// centered between 3 and 5
		case 5:
			if ( totalPlayers == 5 )
				return 4 ;		// centered: used only when rank 4 takes our usual position.
			return 5 ;			// right-side.
		}
		
		return -1 ;
	}
	
	private void refreshExamineLayout( View layout ) {
		// we assume that the displayed info hasn't changed.
		// This method only updates DrawSettings; our draw preferences
		// may have changed.
		
		// Get a new color scheme
		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		mSkin = GameModes.numberQPanes( mGRArray[0].getGameInformation(0) ) == 1
				? QuantroPreferences.getSkinRetro(this)
				: QuantroPreferences.getSkinQuantro(this) ;
		
		
		if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER || mGRArray.length > 1 ) {
			// Load the "two field layout".
			if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER ) {
				setNewDrawSettings( layout.findViewById(R.id.examine_game_result_block_field_view_first), mGRArray[0], 0 ) ;
				setNewDrawSettings( layout.findViewById(R.id.examine_game_result_block_field_view_second), mGRArray[0], 1 ) ;
			}
			else {
				// Otherwise, there must be more than one GameResult.
				setNewDrawSettings( layout.findViewById(R.id.examine_game_result_block_field_view_first), mGRArray[0], 0 ) ;
				setNewDrawSettings( layout.findViewById(R.id.examine_game_result_block_field_view_second), mGRArray[1], 0 ) ;
			}
		}
		else {
			setNewDrawSettings( layout, mGRArray[0], 0 ) ;
		}
	}
	
	
	private void setTitle(ViewGroup layout) {
		int display = TextFormatting.DISPLAY_MENU ;
		int type = -1 ;
		int role = TextFormatting.ROLE_CLIENT ;
		
		if ( mStyle == STYLE_SAVED_GAME )
			type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_TITLE_SAVED_GAME ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER )
			type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_TITLE_SP_GAME_OVER ;
		else if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER )
			type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_TITLE_MP_GAME_OVER ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER )
			type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_TITLE_SP_LEVEL_OVER ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
			type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_TITLE_MP_GAME_OVER ;
		
		if ( res == null )
			res = getResources() ;
		
		TextView tv = (TextView)layout.findViewById(R.id.dialog_content_title) ;
		tv.setText(TextFormatting.format(this, res, display, type, role, mGRArray[0])) ;
	}
	
	private int getFilterColor() {
		return GameModeColors.primary(mColorScheme, mGRArray[0].getGameInformation(0).mode) ;
	}
	
	private void setFilterableContentColor( int color, ViewGroup vg ) {
		String tagColorFilterable = getResources().getString(R.string.tag_color_filterable) ;
		for ( int i = 0; i < vg.getChildCount(); i++ ) {
			View v = vg.getChildAt(i) ;
			if ( v instanceof ViewGroup )
				setFilterableContentColor( color, (ViewGroup)v ) ;
			if ( tagColorFilterable.equals( v.getTag() ) && v.getBackground() != null ) {
				v.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP) ;
			}
		}
	}
	
	private Drawable getDialogBackground() {
		int GOLD = R.drawable.block_background_gold ;
		int local = mGRArray[0].getLocalPlayerSlot() ;
		// Gold "Victory" Background!  Victory is defined differently by game
		// type.
		// MULTIPLAYER: the winner won.  The loser gets standard.
		// SP ENDURANCE: victory is defeat.  Game Over?  Gold metal.  	TODO: change this to gold for "record breaking?"
		// SP PROGRESSION: level-up.
		if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER && mGRArray[0].getWon(local) )
			return getResources().getDrawable( GOLD ) ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER )
				return getResources().getDrawable( GOLD ) ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER )
			return getResources().getDrawable( GOLD ) ;
		
		// If not, use the appropriate background, if we can find it.
		return GameModeColors.blockBackgroundDrawable(this, mColorScheme, mGRArray[0].getGameInformation(0).mode) ;
	}
	
	
	private void setQueryString( View v ) {
		String text = null ;
		if ( mStyle == STYLE_SAVED_GAME )
			text = res.getString(R.string.examine_game_result_query_saved) ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER )
			text = res.getString(R.string.examine_game_result_query_sp_game_over) ;
		else if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER )
			text = res.getString(R.string.examine_game_result_query_mp_game_over) ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER )
			text = res.getString(R.string.examine_game_result_query_sp_rewindable_level_over) ;
		else if ( mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
			text = res.getString(R.string.examine_game_result_query_sp_rewindable_game_over) ;
		TextView tv = (TextView)v.findViewById(R.id.examine_game_result_action_query) ;
		if ( tv != null )
			tv.setText( text ) ;
	}
	
	
	private void setGameResultContent( View textContentView, View blockFieldContentView, GameResult gameResult, int playerSlot, int playerRank ) {
		if ( textContentView == null && blockFieldContentView == null ) {
			Log.e(TAG, "trying to setGameResultContent with null view") ;
			return ;
		}
		if ( gameResult == null ) {
			if ( textContentView != null )
				textContentView.setVisibility(View.GONE) ;
			if ( blockFieldContentView != null )
				blockFieldContentView.setVisibility(View.GONE) ;
			return ;
		}
		if ( gameResult.getNumberOfPlayers() <= playerSlot || playerSlot < 0 ) {
			if ( textContentView != null )
				textContentView.setVisibility(View.GONE) ;
			if ( blockFieldContentView != null )
				blockFieldContentView.setVisibility(View.GONE) ;
			return ;
		}
		
		if ( res == null )
			res = getResources() ;
		
		GameInformation ginfo = gameResult.getGameInformation(playerSlot) ;
		
		// Set using our string tags; find TextViews with the appropriate tag,
		// set its content, and in the case of player name, set its color.
		int display = TextFormatting.DISPLAY_MENU ;
		int type = -1 ;
		int role = TextFormatting.ROLE_CLIENT ;
		int id ;
		
		if ( textContentView != null ) { 
			TextView tv = (TextView) textContentView.findViewById(mIDPlayerName) ;
			if ( tv != null ) {
				// If multiplayer, we put the player name with an appropriate color.
				// Otherwise, we use the name of the game mode.
				if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER ) {
					type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_PLAYER_NAME ;
					tv.setText(TextFormatting.format(this, res, display, type, role, gameResult.getName(playerSlot))) ;
					if ( mPlayerColors != null )
						tv.setTextColor(mPlayerColors[playerSlot]) ;
				} else {
					tv.setText(GameModes.name(mGRArray[0].getGameInformation(0).mode)) ;
				}
			}
			
			tv = (TextView) textContentView.findViewById(mIDDifficulty) ;
			if ( tv != null )  {
				// If multiplayer, just remove it.  Otherwise, set to a difficulty string.
				if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER ) {
					tv.setVisibility(View.GONE) ;
				} else {
					GameSettings gs = mGRArray[0].getGameSettings(0) ;
					int textId = R.string.examine_game_result_difficulty_normal ;
					switch ( gs.getDifficulty() ) {
					case GameInformation.DIFFICULTY_PRACTICE:
						textId = R.string.examine_game_result_difficulty_practice ;
						break ;
					case GameInformation.DIFFICULTY_NORMAL:
						textId = R.string.examine_game_result_difficulty_normal ;
						break ;
					case GameInformation.DIFFICULTY_HARD:
						textId = R.string.examine_game_result_difficulty_hard ;
						break ;
					}
					tv.setText(textId) ;
				}
			}
			
			tv = (TextView) textContentView.findViewById(mIDWinLose) ;
			if ( tv != null ) {
				// If multiplayer, put up whether this is won or lost.  Otherwise make GONE.
				if ( mStyle == STYLE_MULTI_PLAYER_GAME_OVER ) {
					id = 0 ;
					switch( playerRank ) {
					case -1:
						// quitter!
						id = gameResult.getNumberOfPlayers() == 2
								? R.string.examine_game_result_quit
								: R.string.examine_game_result_free4all_quit ;
						break ;
					case 0:
						// 1st place!
						id = gameResult.getNumberOfPlayers() == 2
								? R.string.examine_game_result_winner
								: R.string.examine_game_result_free4all_rank_1 ;
						break ;
					case 1:
						// 2nd place!
						id = gameResult.getNumberOfPlayers() == 2
								? R.string.examine_game_result_loser
								: R.string.examine_game_result_free4all_rank_2 ;
						break ;
					case 2:
						// 3rd place!
						id = R.string.examine_game_result_free4all_rank_3 ;
						break ;
					case 3:
						// 3rd place!
						id = R.string.examine_game_result_free4all_rank_4 ;
						break ;
					case 4:
						// 3rd place!
						id = R.string.examine_game_result_free4all_rank_5 ;
						break ;
					case 5:
						// 3rd place!
						id = R.string.examine_game_result_free4all_rank_6 ;
						break ;
						
					}
					
					tv.setText(id) ;
					if ( mPlayerColors != null )
						tv.setTextColor(mPlayerColors[playerSlot]) ;
				} else
					tv.setVisibility(View.GONE) ;
			}
			
			// Config
			tv = (TextView) textContentView.findViewById(mIDConfig) ;
			if ( tv != null ) {
				if ( mGameSettings == null || mGameSettings.hasDefaultsIgnoringDifficulty() )
					tv.setVisibility(View.GONE);
				else
					tv.setText( this.makeCustomSettingsText(mGameSettings) ) ;
			}
			// Level
			tv = (TextView) textContentView.findViewById(mIDLevelFull) ;
			if ( tv != null ) {
				type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_LEVEL_FULL ;
				tv.setText( TextFormatting.format(this, res, display, type, role, gameResult, playerSlot)) ;
			}
			tv = (TextView) textContentView.findViewById(mIDLevelNumber) ;
			if ( tv != null ) {
				type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_LEVEL_NUMBER ;
				tv.setText( TextFormatting.format(this, res, display, type, role, gameResult, playerSlot)) ;
			}
			// Time
			boolean showTime = GameModes.measurePerformanceBy(ginfo.mode)
					== GameModes.MEASURE_PERFORMANCE_BY_TIME ;
			tv = (TextView) textContentView.findViewById(mIDTimeFull) ;
			if ( tv != null ) {
				// only display time for single-player flood games.
				if ( showTime ) {
					type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_TIME_FULL ;
					tv.setText( TextFormatting.format(this, res, display, type, role, gameResult, playerSlot)) ;
				} else {
					tv.setVisibility(View.GONE) ;
				}
			}
			tv = (TextView) textContentView.findViewById(mIDTimeNumber) ;
			if ( tv != null ) {
				// only display time for single-player flood games.
				if ( showTime ) {
					type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_TIME_NUMBER ;
					tv.setText( TextFormatting.format(this, res, display, type, role, gameResult, playerSlot)) ;
				} else {
					tv.setVisibility(View.GONE) ;
				}
			}
			// Score
			tv = (TextView) textContentView.findViewById(mIDScoreFull) ;
			if ( tv != null ) {
				type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_SCORE_FULL ;
				tv.setText( TextFormatting.format(this, res, display, type, role, gameResult, playerSlot)) ;
			}
			tv = (TextView) textContentView.findViewById(mIDScoreNumber) ;
			if ( tv != null ) {
				type = TextFormatting.TYPE_EXAMINE_GAME_RESULT_SCORE_NUMBER ;
				tv.setText( TextFormatting.format(this, res, display, type, role, gameResult, playerSlot)) ;
			}
			tv = (TextView) textContentView.findViewById(mIDRowsFull) ;
			if ( tv != null ) {
				String rowsFullStr = res.getString(R.string.examine_game_result_rows_full_text) ;
				insertAndSetRowString( tv, rowsFullStr, ginfo ) ;
			}
			tv = (TextView) textContentView.findViewById(mIDRowsNumber) ;
			if ( tv != null ) {
				String rowsNumberStr = res.getString(R.string.examine_game_result_rows_number) ;
				insertAndSetRowString( tv, rowsNumberStr, ginfo ) ;
			}
		}
		
		
		if ( blockFieldContentView != null ) {
			// BlockFieldView.
			BlockFieldView bfv = (BlockFieldView) blockFieldContentView.findViewById(mIDBlockFieldView) ;
			if ( bfv == null )
				bfv = (BlockFieldView)blockFieldContentView ;
			// Set!
			if ( bfv != null ) {
				// By default, draw settings has a buffer of 1 around the block field edge.
				DrawSettings ds = new DrawSettings(
						new Rect(0,0,100,100),
						gameResult.getRows(playerSlot), 
						gameResult.getCols(playerSlot), 
						gameResult.getRows(playerSlot)/2,
						DrawSettings.DRAW_DETAIL_LOW,
						DrawSettings.DRAW_ANIMATIONS_NONE,	// No animations
						mSkin,
						this ) ;
				if ( VersionCapabilities.supportsBlockDrawerWithViewBehind() ) {
					ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_BLOCK_FILL ;
				} else {
					ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_EMPTY_AND_PIECE ;
				}
				
				ds.behavior_align_horizontal = DrawSettings.BEHAVIOR_ALIGN_CENTER_GRID ;
				ds.behavior_align_vertical = DrawSettings.BEHAVIOR_ALIGN_CENTER_GRID ;
				ds.setBlit(DrawSettings.BLIT_NONE, 1) ;
				
				byte [][][] unbufferedBlockField = gameResult.getBlockFieldReference(playerSlot) ;
				byte [][][] bufferedBlockField = new byte[2][unbufferedBlockField[0].length+2][unbufferedBlockField[0][0].length+2] ;
				ArrayOps.copyInto(unbufferedBlockField, bufferedBlockField, 1, 1) ;
				int pieceType = gameResult.getPieceType(playerSlot) ;
				if ( pieceType < 0 )
					bfv.setContent( ds, bufferedBlockField ) ;
				else {
					byte [][][] unbufferedPieceBlockField = gameResult.getPieceBlockFieldReference(playerSlot) ;
					byte [][][] bufferedPieceBlockField = new byte[2][unbufferedPieceBlockField[0].length+2][unbufferedPieceBlockField[0][0].length+2] ;
					ArrayOps.copyInto(unbufferedPieceBlockField, bufferedPieceBlockField, 1, 1) ;
					bfv.setContent(ds, bufferedBlockField, pieceType, bufferedPieceBlockField, 0, null) ;
				}
			}
		}
	}
	
	
	private void insertAndSetRowString( TextView tv, String templateStr, GameInformation ginfo ) {
		String rowsPlaceholder = res.getString(R.string.placeholder_game_result_rows) ;
		CharSequence fullText ;
		if ( GameModes.numberQPanes(ginfo.mode) == 1 ) {
			// plain text
			String rowsStr = "" + (ginfo.sLclears + ginfo.moclears) ;
			fullText = templateStr.replace(rowsPlaceholder, rowsStr) ;
		} else {
			// spannable text: color the row counts with the appropriate foreground
			// / background color.
			String rows0 = "" + (ginfo.s0clears + ginfo.moclears) ;
			String rows1 = "" + (ginfo.s1clears + ginfo.moclears) ;
			String sep = "  " ;
			String rows = rows0 + sep + rows1 ;
			int start0 = templateStr.indexOf(rowsPlaceholder) ;
			int start1 = start0 + rows0.length() + sep.length() ;
			SpannableString ss = new SpannableString( templateStr.replace(rowsPlaceholder, rows) ) ;
			ss.setSpan(new ForegroundColorSpan(mColorScheme.getFillColor(QOrientations.S0, 0)),
					start0, start0 + rows0.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE ) ;
			ss.setSpan(new ForegroundColorSpan(mColorScheme.getFillColor(QOrientations.S1, 1)),
					start1, start1 + rows1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE ) ;
			fullText = ss ;
		}
		tv.setText(fullText,
				fullText instanceof SpannableString ? TextView.BufferType.SPANNABLE : TextView.BufferType.NORMAL) ;
	}
	
	
	private String makeCustomSettingsText( GameSettings gs ) {
		Resources res = getResources() ;
    	String placeholder_name = res.getString( R.string.placeholder_game_mode_name ) ;
		// create custom string.
		boolean has = false ;
		StringBuilder sb = new StringBuilder() ;
		
		String sep = res.getString(R.string.examine_game_settings_list_separator) ;
		
		// Custom level?
		if ( gs.hasLevel() ) {
			String str = res.getString(R.string.examine_game_settings_list_level) ;
			str = str.replace(
					res.getString(R.string.placeholder_game_settings_custom_value_level),
					"" + gs.getLevel()) ;
			sb.append(str) ;
			has = true ;
		}
		
		// Custom clears-per?
		if ( gs.hasClearsPerLevel() ) {
			String str = res.getString(R.string.examine_game_settings_list_clears_per_level) ;
			str = str.replace(
					res.getString(R.string.placeholder_game_settings_custom_value_clears_per_level),
					"" + gs.getClearsPerLevel()) ;
			if ( has )
				sb.append(sep) ;
			sb.append(str) ;
			has = true ;
		}
		
		// Custom Garbage?
		if ( gs.hasGarbage() || gs.hasGarbagePerLevel() ) {
			String strDefault = res.getString(R.string.examine_game_settings_list_default) ;
			String str = res.getString(R.string.examine_game_settings_list_garbage) ;
			str = str.replace(
					res.getString(R.string.placeholder_game_settings_custom_value_garbage),
					gs.hasGarbage() ? "" + gs.getGarbage() : strDefault) ;
			str = str.replace(
					res.getString(R.string.placeholder_game_settings_custom_value_garbage_per_level),
					gs.hasGarbagePerLevel() ? "" + gs.getGarbagePerLevel() : strDefault) ;
			
			if ( has )
				sb.append(sep) ;
			sb.append(str) ;
			has = true ;
		}
		
		// Custom Displacement Rate?
		if ( gs.hasDisplacementFixedRate() ) {
			String str = res.getString(R.string.examine_game_settings_list_displacement_fixed_rate) ;
			str = str.replace(
					res.getString(R.string.placeholder_game_settings_custom_value_displacement_fixed_rate),
					"" + String.format("%.2f", gs.getDisplacementFixedRate())) ;
			if ( has )
				sb.append(sep) ;
			sb.append(str) ;
			has = true ;
		}
		
		
		// now load the custom new string and place this customized list within it.
		String str = res.getString(R.string.examine_game_settings) ;
		str = str.replace(
				res.getString(R.string.placeholder_game_settings_custom_list),
				sb.toString()) ;
		str = str.replace(placeholder_name, GameModes.name(gs.getMode())) ;
		return str ;
	}
	
	private void setNewDrawSettings( View contentView, GameResult gameResult, int playerSlot ) {
		// BlockFieldView.
		BlockFieldView bfv = (BlockFieldView) contentView.findViewById(mIDBlockFieldView) ;
		// Set!
		if ( bfv != null ) {
			// By default, draw settings has a buffer of 1 around the block field edge.
			DrawSettings ds = new DrawSettings(
					new Rect(0,0,100,100),
					gameResult.getRows(playerSlot), 
					gameResult.getCols(playerSlot), 
					gameResult.getRows(playerSlot)/2,
					DrawSettings.DRAW_DETAIL_LOW,
					DrawSettings.DRAW_ANIMATIONS_NONE,	// No animations
					mSkin,
					this ) ;
			if ( VersionCapabilities.supportsBlockDrawerWithViewBehind() ) {
				ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_BLOCK_FILL ;
			} else {
				ds.behavior_background = DrawSettings.BEHAVIOR_BACKGROUND_EMPTY_AND_PIECE ;
			}
			
			ds.behavior_align_horizontal = DrawSettings.BEHAVIOR_ALIGN_CENTER_GRID ;
			ds.behavior_align_vertical = DrawSettings.BEHAVIOR_ALIGN_CENTER_GRID ;
			ds.setBlit(DrawSettings.BLIT_NONE, 1) ;
			bfv.setDrawSettings(ds) ;
		}
	}
	
	
	private void setButton( DialogButtonStrip buttonStrip, int buttonNum, int buttonType ) {
		String text ;
		DialogInterface.OnClickListener listener ;
		
		switch( buttonType ) {
		case BUTTON_GONE:
			return ;
		case BUTTON_TYPE_CANCEL:
			text = res.getString(R.string.examine_game_result_button_cancel) ;
			listener = getOnClickListener( BUTTON_TYPE_CANCEL ) ;
			break ;
		case BUTTON_TYPE_QUIT:
			text = res.getString(R.string.examine_game_result_button_quit) ;
			listener = getOnClickListener( BUTTON_TYPE_QUIT ) ;
			break ;
		case BUTTON_TYPE_QUIT_REWINDABLE:
			text = res.getString(R.string.examine_game_result_button_rewindable_quit) ;
			listener = getOnClickListener( BUTTON_TYPE_QUIT_REWINDABLE ) ;
			break ;
		case BUTTON_TYPE_RETURN_TO_LOBBY:
			text = res.getString(R.string.examine_game_result_button_return_to_lobby) ;
			listener = getOnClickListener( BUTTON_TYPE_RETURN_TO_LOBBY ) ;
			break ;
		case BUTTON_TYPE_PLAY:
			text = res.getString(R.string.examine_game_result_button_play) ;
			listener = getOnClickListener( BUTTON_TYPE_PLAY ) ;
			break ;
		case BUTTON_TYPE_CONTINUE:
			text = res.getString(R.string.examine_game_result_button_continue) ;
			listener = getOnClickListener( BUTTON_TYPE_CONTINUE ) ;
			break ;
		case BUTTON_TYPE_REPLAY:
			text = res.getString(R.string.examine_game_result_button_replay) ;
			listener = getOnClickListener( BUTTON_TYPE_REPLAY ) ;
			break ;
		case BUTTON_TYPE_REWIND:
			text = res.getString(R.string.examine_game_result_button_rewind) ;
			listener = getOnClickListener( BUTTON_TYPE_REWIND ) ;
			break ;
		case BUTTON_TYPE_DELETE:
			text = res.getString(R.string.examine_game_result_button_delete) ;
			listener = getOnClickListener( BUTTON_TYPE_DELETE ) ;
			break ;
		default:
			throw new IllegalArgumentException("Unknown button type " + buttonType) ;
		}
		
		Integer color = getButtonColor( buttonType ) ;
		
		if ( color == null ) {
			switch( buttonNum ) {
			case DialogButtonStrip.BUTTON_POSITIVE:
				buttonStrip.setPositiveButton(text, listener) ;
				break ;
			case DialogButtonStrip.BUTTON_NEUTRAL:
				buttonStrip.setNeutralButton(text, listener) ;
				break ;
			case DialogButtonStrip.BUTTON_NEGATIVE:
				buttonStrip.setNegativeButton(text, listener) ;
				break ;
			}
		} else {
			switch( buttonNum ) {
			case DialogButtonStrip.BUTTON_POSITIVE:
				buttonStrip.setPositiveButton(text, color, listener) ;
				break ;
			case DialogButtonStrip.BUTTON_NEUTRAL:
				buttonStrip.setNeutralButton(text, color, listener) ;
				break ;
			case DialogButtonStrip.BUTTON_NEGATIVE:
				buttonStrip.setNegativeButton(text, color, listener) ;
				break ;
			}
		}
		
	}

	
	private Integer getButtonColor( int buttonType ) {
		switch( buttonType ) {
		case BUTTON_TYPE_CANCEL:
		case BUTTON_TYPE_QUIT:
		case BUTTON_TYPE_RETURN_TO_LOBBY:
		case BUTTON_TYPE_QUIT_REWINDABLE:
			return null ;	// no special color
		
		case BUTTON_TYPE_PLAY:
		case BUTTON_TYPE_CONTINUE:
		case BUTTON_TYPE_REPLAY:
			// primary color for this game mode.
			return GameModeColors.primary(mColorScheme, mGRArray[0].getGameInformation(0).mode) ;
			
		case BUTTON_TYPE_REWIND:
		case BUTTON_TYPE_DELETE:
			// secondary color for this game mode
			return GameModeColors.secondary(mColorScheme, mGRArray[0].getGameInformation(0).mode) ;
		}
		
		return null ;
	}
	
	
	private DialogInterface.OnClickListener getOnClickListener( int buttonType ) {
		switch( buttonType ) {
		case BUTTON_TYPE_CANCEL:
		case BUTTON_TYPE_QUIT:
		case BUTTON_TYPE_RETURN_TO_LOBBY:
		case BUTTON_TYPE_QUIT_REWINDABLE:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if ( mSoundControls )
		        		mSoundPool.menuButtonBack() ;
					mResolving = true ;
					/* mDialogManager.dismissAllDialogs() ; */
					Intent i = new Intent() ;
					Intent intentIn = getIntent() ;
					if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
						Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
						i.putExtra(INTENT_EXTRA_ECHO, s) ;
					}
					i.putExtra(INTENT_RESULT_EXTRA, INTENT_RESULT_CANCEL) ;
			    	setResult(RESULT_OK, i) ;
			    	finish() ;
				}
			} ;
			
		case BUTTON_TYPE_PLAY:
		case BUTTON_TYPE_CONTINUE:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if ( mSoundControls )
		        		mSoundPool.menuButtonClick() ;
					mResolving = true ;
					/* mDialogManager.dismissAllDialogs() ; */
					Intent i = new Intent() ;
					Intent intentIn = getIntent() ;
					if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
						Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
						i.putExtra(INTENT_EXTRA_ECHO, s) ;
					}
			    	i.putExtra(INTENT_RESULT_EXTRA, INTENT_RESULT_CONTINUE) ;
			    	setResult(RESULT_OK, i) ;
			    	finish() ;
				}
			} ;
			
		case BUTTON_TYPE_REPLAY:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if ( mSoundControls )
		        		mSoundPool.menuButtonClick() ;
					mResolving = true ;
					/* mDialogManager.dismissAllDialogs() ; */
					Intent i = new Intent() ;
					Intent intentIn = getIntent() ;
					if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
						Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
						i.putExtra(INTENT_EXTRA_ECHO, s) ;
					}
			    	i.putExtra(INTENT_RESULT_EXTRA, INTENT_RESULT_REPLAY) ;
			    	i.putExtra(INTENT_RESULT_GAME_RESULT_REPLAY_ARRAY_INDEX, mGRArray.length == 1 ? 0 : 1) ;
			    	setResult(RESULT_OK, i) ;
			    	finish() ;
				}
			} ;
			
		case BUTTON_TYPE_REWIND:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if ( mSoundControls )
		        		mSoundPool.menuButtonClick() ;
					mResolving = true ;
					/* mDialogManager.dismissAllDialogs() ; */
					Intent i = new Intent() ;
					Intent intentIn = getIntent() ;
					if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
						Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
						i.putExtra(INTENT_EXTRA_ECHO, s) ;
					}
			    	i.putExtra(INTENT_RESULT_EXTRA, INTENT_RESULT_REWIND) ;
			    	i.putExtra(INTENT_RESULT_GAME_RESULT_REPLAY_ARRAY_INDEX, mGRArray.length == 1 ? 0 : 1) ;
			    	setResult(RESULT_OK, i) ;
			    	finish() ;
				}
			} ;
			
		
			
		case BUTTON_TYPE_DELETE:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if ( mSoundControls )
		        		mSoundPool.menuButtonClick() ;
					mResolving = true ;
					/* mDialogManager.dismissAllDialogs() ; */
					Intent i = new Intent() ;
					Intent intentIn = getIntent() ;
					if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
						Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
						i.putExtra(INTENT_EXTRA_ECHO, s) ;
					}
			    	i.putExtra(INTENT_RESULT_EXTRA, INTENT_RESULT_DELETE) ;
			    	setResult(RESULT_OK, i) ;
			    	finish() ;
				}
			} ;
			
		default:
			return null ;
		}
	}


	
	@Override
	public void onGooglePlaySignInUpdated( boolean signedIn ) {
		super.onGooglePlaySignInUpdated(signedIn) ;
		
		if ( signedIn ) {
			if ( ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER || mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
					&& mScoreSubmissionLeaderboardID != null
					&& ( mScoreSubmissionState == SCORE_SUBMISSION_STATE_NONE
							|| mScoreSubmissionState == SCORE_SUBMISSION_STATE_SIGNED_OUT
							|| mScoreSubmissionState == SCORE_SUBMISSION_STATE_SUBMITTING )
					&& ( mScoreSubmissionStatusCode != SCORE_SUBMISSION_STATUS_NETWORK_ERROR_OPERATION_DEFERRED
							&& this.mScoreSubmissionResult == null ) ) {
				
				// Ready to submit a score.
				submitScore() ;
			} else if ( mScoreSubmissionStatusCode == SCORE_SUBMISSION_STATUS_NETWORK_ERROR_OPERATION_DEFERRED
							|| this.mScoreSubmissionResult != null ) {
				// Submitted!
				mScoreSubmissionState = SCORE_SUBMISSION_STATE_SUBMITTED ;
			}
		} else {
			if ( ( mStyle == STYLE_SINGLE_PLAYER_GAME_OVER || mStyle == STYLE_SINGLE_PLAYER_REWINDABLE_GAME_OVER )
					&& mScoreSubmissionLeaderboardID != null ) {
				// we just signed out.  Set our state accordingly.
				mScoreSubmissionState = SCORE_SUBMISSION_STATE_SIGNED_OUT ;
			}
		}
		
		// refresh our score submission view.
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				refreshScoreSubmissionViews() ;
			}
		}) ;
	}

	/* TODO listen for score submission
	@Override
	public void onScoreSubmitted(int statusCode, SubmitScoreResult result) {
		
		// double-submit.  Oops.
		if ( this.isScoreSubmitted() )
			return ;
		
		String title, message ;
		
		switch( statusCode ) {
		case GamesClient.STATUS_OK:
			// the score was successfully submitted!
			this.mScoreSubmissionStatusCode = statusCode ;
			this.mScoreSubmissionResult = result ;
			this.mScoreSubmissionState = SCORE_SUBMISSION_STATE_SUBMITTED ;
			break ;
			
		case GamesClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
			// the score was saved locally, and will be submitted later.
			// This counts as a submission, but our view may differ.
			this.mScoreSubmissionStatusCode = statusCode ;
			this.mScoreSubmissionResult = result ;
			this.mScoreSubmissionState = SCORE_SUBMISSION_STATE_SUBMITTED ;
			break ;
			
		case GamesClient.STATUS_CLIENT_RECONNECT_REQUIRED:
			// We require a reconnect.  Sign out, show an alert, and
			// set our status as SIGNED_OUT.
			this.mScoreSubmissionStatusCode = statusCode ;
			this.mScoreSubmissionResult = null ;
			this.mScoreSubmissionState = SCORE_SUBMISSION_STATE_SIGNED_OUT ;
			title = getResources().getString(R.string.gamehelper_submission_error_title) ;
			message = getResources().getString(R.string.gamehelper_submission_error_reconnect_required) ;
			((QuantroApplication)getApplication()).gpg_signOut(false) ;
			((QuantroApplication)getApplication()).gpg_showAlert(title, message) ;
			break ;
			
		case GamesClient.STATUS_LICENSE_CHECK_FAILED:
			// License check failed.  Sign out, show an alert, and
			// set our status as SIGNED_OUT.
			this.mScoreSubmissionStatusCode = statusCode ;
			this.mScoreSubmissionResult = null ;
			this.mScoreSubmissionState = SCORE_SUBMISSION_STATE_SIGNED_OUT ;
			title = getResources().getString(R.string.gamehelper_submission_error_title) ;
			message = getResources().getString(R.string.gamehelper_submission_error_license_check_failed) ;
			((QuantroApplication)getApplication()).gpg_signOut(false) ;
			((QuantroApplication)getApplication()).gpg_showAlert(title, message) ;
			break ;
			
		case GamesClient.STATUS_INTERNAL_ERROR:
			// License check failed.  Sign out, show an alert, and
			// set our status as SIGNED_OUT.
			this.mScoreSubmissionStatusCode = statusCode ;
			this.mScoreSubmissionResult = null ;
			this.mScoreSubmissionState = SCORE_SUBMISSION_STATE_SIGNED_OUT ;
			title = getResources().getString(R.string.gamehelper_submission_error_title) ;
			message = getResources().getString(R.string.gamehelper_submission_error_internal_error) ;
			((QuantroApplication)getApplication()).gpg_signOut(false) ;
			((QuantroApplication)getApplication()).gpg_showAlert(title, message) ;
			break ;
		}
		
		// refresh our score submission view.
		runOnUiThread( new Runnable() {
			@Override
			public void run() {
				refreshScoreSubmissionViews() ;
			}
		}) ;
	}
	 */

}
