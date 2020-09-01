package com.peaceray.quantro;

import java.io.Serializable;

import com.peaceray.quantro.database.GameSettingsDatabaseAdapter;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.Scores;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.generic.LabeledSeekBarAdapter;
import com.peaceray.quantro.view.dialog.AlertDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;



/**
 * A NewSinglePlayerGameActivity serves two functions.  First, it displays a description
 * of the game to the player, to give them an idea of what they're in for.  Second,
 * it allows certain parameters of the game mode to be configured in preparation for
 * a game launch.  For example, starting level and garbage rows can be configured
 * for Endurance and Progression game types, and Progression game modes also allow
 * the "number of row clears before level-up" to be changed.
 * 
 * For now, we don't attempt to handle multiplayer games, but we expect this
 * activity will be extended to allow setting handicaps etc. in multiplayer games.
 * 
 * This Activity does not perform any launches; it only reports as a result (if the
 * user indicated that they want to play) and the creating activity needs to handle
 * this.
 * 
 * @author Jake
 *
 */
public class NewGameSettingsActivity extends QuantroActivity
		implements LabeledSeekBarAdapter.OnSeekBarChangeListener, OnCheckedChangeListener, OnItemSelectedListener {
	
	private static final String TAG = "NewGameSettingsActivity" ;

	// Extras.
	public static final String INTENT_EXTRA_GAME_MODE 	= "com.peaceray.quantro.NewGameSettingsActivity.INTENT_EXTRA_GAME_MODE" ;
	public static final String INTENT_EXTRA_STYLE 		= "com.peaceray.quantro.NewGameSettingsActivity.INTENT_EXTRA_STYLE" ;
	public static final String INTENT_EXTRA_ECHO 		= "com.peaceray.quantro.NewGameSettingsActivity.INTENT_EXTRA_ECHO" ;
	
	// Styles.
	public static final int STYLE_NEW_SINGLE_PLAYER_GAME = 0 ;	// The default style.
	
	// Results
	public static final String INTENT_RESULT_EXTRA_ACTION = "com.peaceray.quantro.NewGameSettingsActivity.INTENT_RESULT_EXTRA_ACTION" ;
	public static final int INTENT_RESULT_EXTRA_ACTION_CANCEL = 0 ;
	public static final int INTENT_RESULT_EXTRA_ACTION_PLAY = 1 ;
	// Settings for level, clears, garbage, etc.
	public static final String INTENT_RESULT_EXTRA_GAME_SETTINGS = "com.peaceray.quantro.NewGameSettingsActivity.INTENT_RESULT_EXTRA_GAME_SETTINGS" ;
	
	
	// Button types for our dialog
	public static final int BUTTON_GONE = -1 ;
	public static final int BUTTON_TYPE_CANCEL = 0 ;
	public static final int BUTTON_TYPE_PLAY = 1 ;
	public static final int BUTTON_TYPE_DEFAULT = 2 ;
	
	
	
	public static final int DEFAULT_CLEARS_PER_LEVEL_ARRAY_INDEX = 5 ;		// 10 clears
	public static final int [] CLEARS_PER_LEVEL_ARRAY = new int[]{
			1,					//0
			2,
			3,
			4,
			5,
			10,					//5
			15,					
			20,
			30,
			40,
			50,					// 10
			75,
			100,
			200,
			300,
			400,				// 15
			500,
			1000,
			Integer.MAX_VALUE	// 18
	};
	
	
	public static final int DISPLACEMENT_FIXED_RATE_MIN = 1 ;
	public static final int DISPLACEMENT_FIXED_RATE_MAX = 100 ;
	public static final int DISPLACEMENT_FIXED_RATE_INTERVAL = 1 ;
	public static final int DISPLACEMENT_FIXED_RATE_EXPONENT = -2 ;		// so 0.01 to 1.00
	public static final double DISPLACEMENT_FIXED_RATE_DIVISOR = 100 ;		// so 0.01 to 1.00
	
	
	
	
	private Resources res ;
	
	private int mStyle ;
	private int mGameMode ;
	
	// Seek bar adapters
	private LabeledSeekBarAdapter mLevelBar ;
	private LabeledSeekBarAdapter mClearsBar ;
	private LabeledSeekBarAdapter mStartingGarbageBar ;
	private LabeledSeekBarAdapter mPerLevelGarbageBar ;
	private LabeledSeekBarAdapter mDisplacementFixedRateBar ;
	
	private View mLevelLockContainer ;
	private CheckBox mLevelLockCheckBox ;
	
	private View mDifficultyContainer ;
	private Spinner mDifficultySpinner ;
	private ArrayAdapter<CharSequence> mDifficultySpinnerAdapter ;
	
	private TextView mDifficultySkipAnimationsWarningView ;
	private TextView mLeaderboardSupportedTextView ;
	
	// All we do is display a dialog
	private static final int DIALOG_ID = 0 ;
	private AlertDialog mDialog ;
	
	private DialogManager mDialogManager ;
	
	private ColorScheme mColorScheme ;
	
	// our current best.  It's wiser to make one from scratch to use
	private GameSettings mCurrentGameSettings ;
	
	@Override
	synchronized public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState) ;
		setupQuantroActivity( QUANTRO_ACTIVITY_UNKNOWN, QUANTRO_ACTIVITY_CONTENT_EMPTY ) ;
		Log.d(TAG, "onCreate") ;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		res = getResources() ;
		
		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		
		mDialogManager = new DialogManager(this) ;
		
		// Load style, game result, etc.
		Intent intent = getIntent() ;
		mStyle = intent.getIntExtra(INTENT_EXTRA_STYLE, -1) ;
		mGameMode = intent.getIntExtra(INTENT_EXTRA_GAME_MODE, -1) ;
		if ( mStyle == -1 || mGameMode == -1 ) {
			Log.e(TAG, "Style or game mode not set in launching Intent.  Style " + mStyle + ", GameMode " + mGameMode) ;
			finish() ;
		}
		if ( !GameModes.has(mGameMode) ) {
			Log.e(TAG, "Game mode " + mGameMode + " is not valid") ;
			finish() ;
		}
		
		mCurrentGameSettings = new GameSettings(mGameMode) ;
		
		mDialogManager.hideDialogs() ;
		mDialogManager.showDialog(DIALOG_ID) ;
		
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}
	

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume") ;
		super.onResume() ;
		
    	
    	// Reveal our dialogs, in case they were previously hidden.
        mDialogManager.revealDialogs() ;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause") ;
        
        // Reveal our dialogs, in case they were previously hidden.
        mDialogManager.hideDialogs() ;
	}
	
	
	@Override
    protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder ;
		
		
		
		if ( id == DIALOG_ID ) {
			View layout = loadExamineLayout() ;
        	builder = new AlertDialog.Builder( this ) ;
        	builder.setView(layout) ;
			setGameModeDetails(layout) ;
			setSettingsControls(layout) ;
			setQueryString(layout) ;
        	builder.setFilterableContentColor( getFilterColor() ) ;
        	Drawable background = getDialogBackground() ;
        	if ( background != null )
        		builder.setBackground(background) ;
        	builder.setCancelable(true) ;		// cancelable so "back" button dismisses alert and quits activity
        	setTitle(builder) ;
        	switch( mStyle ) {
    		case STYLE_NEW_SINGLE_PLAYER_GAME:
    			setButton(builder, AlertDialog.BUTTON_POSITIVE, BUTTON_TYPE_PLAY) ;
    			setButton(builder, AlertDialog.BUTTON_NEUTRAL, BUTTON_TYPE_DEFAULT) ;	// Neutral button for "defaults"
    			setButton(builder, AlertDialog.BUTTON_NEGATIVE, BUTTON_TYPE_CANCEL) ;
    			break ;
    		}
        	builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					// same as cancel
					mDialogManager.dismissAllDialogs() ;
					Intent i = new Intent() ;
					Intent intentIn = getIntent() ;
					if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
						Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
						i.putExtra(INTENT_EXTRA_ECHO, s) ;
					}
					i.putExtra(INTENT_RESULT_EXTRA_ACTION, INTENT_RESULT_EXTRA_ACTION_CANCEL) ;
			    	setResult(RESULT_OK, i) ;
			    	finish() ;
				}
			}) ;
        	mDialog = builder.create() ;        	
        	return mDialog ;
		}
		
		return null ;
	}
	
	
	@Override
    protected void onPrepareDialog(int id, Dialog dialog) {
		
		if ( id == DIALOG_ID ) {
			((AlertDialog)dialog).setButtonEnabled(AlertDialog.BUTTON_NEUTRAL, !isSetToDefaults()) ;
		}
		
	}
	
	
	
	private View loadExamineLayout() {
		return getLayoutInflater().inflate(R.layout.new_game, null);
	}
	
	
	private void setGameModeDetails( View v ) {
		// We set the game view name and description.
		//TextView tv = (TextView)v.findViewById(R.id.new_game_name) ;
		//if ( tv != null )
		//	tv.setText( GameModes.name(mGameMode) ) ;
		TextView tv = (TextView)v.findViewById(R.id.new_game_description) ;
		if ( tv != null )
			tv.setText( GameModes.description(mGameMode) ) ;
		
		mDifficultySkipAnimationsWarningView = (TextView)v.findViewById(R.id.new_game_difficulty_skip_animations_warning) ;
		
		mLeaderboardSupportedTextView = (TextView)v.findViewById(R.id.new_game_supports_leaderboard) ;
		if ( mLeaderboardSupportedTextView != null ) {
			if ( Scores.isLeaderboardSupported(mGameMode) ) {
				mLeaderboardSupportedTextView.setText(R.string.new_game_leaderboard_supported_yes) ;
			} else {
				mLeaderboardSupportedTextView.setText(R.string.new_game_leaderboard_supported_no_game_mode) ;
			}
		}
	}
	
	
	private void setSettingsControls( View layout ) {
		// First we need to get references to the SeekBar layouts,
		// and construct adapters for them.
		View v ;
		v = layout.findViewById(R.id.new_game_labeled_seek_bar_level) ;
		mLevelBar = v == null ? null : new LabeledSeekBarAdapter( this, v ) ;
		v = layout.findViewById(R.id.new_game_labeled_seek_bar_clears) ;
		mClearsBar = v == null ? null : new LabeledSeekBarAdapter( this, v ) ;
		v = layout.findViewById(R.id.new_game_labeled_seek_bar_starting_garbage) ;
		mStartingGarbageBar = v == null ? null : new LabeledSeekBarAdapter( this, v ) ;
		v = layout.findViewById(R.id.new_game_labeled_seek_bar_per_level_garbage) ;
		mPerLevelGarbageBar = v == null ? null : new LabeledSeekBarAdapter( this, v ) ;
		v = layout.findViewById(R.id.new_game_labeled_seek_bar_displacement_fixed_speed) ;
		mDisplacementFixedRateBar = v == null ? null : new LabeledSeekBarAdapter( this, v ) ;
		
		mLevelLockContainer = layout.findViewById(R.id.new_game_level_lock_container) ;
		mLevelLockCheckBox = (CheckBox)layout.findViewById(R.id.new_game_level_lock_checkbox) ;
		
		mDifficultyContainer = layout.findViewById(R.id.new_game_difficulty_container) ;
		mDifficultySpinner = (Spinner)layout.findViewById(R.id.new_game_difficulty) ;
		
		// Now set the necessary parameters, including labels, values, maxes, etc.
		
		////////////////////////////////////////////////////////////////////////
		// levels first.
		int max = GameModes.maxStartingLevel(mGameMode) ;
		// range is from 1 to max.
		int [] vals = ArrayOps.range(1, max) ;
		String [] partValueText = toStrings(vals) ;
		mLevelBar.setExplicitValues(
				vals,
				res.getString( R.string.new_game_level_seek_bar_parts_label),
				partValueText,
				partValueText[0],
				partValueText[partValueText.length-1]) ;
		
		
		////////////////////////////////////////////////////////////////////////
		// clears next.  only certain game types allow it.
		// range is from 1 to max.
		vals = CLEARS_PER_LEVEL_ARRAY ;
		partValueText = toStrings(vals) ;
		mClearsBar.setExplicitValues(
				vals,
				res.getString( R.string.new_game_clears_seek_bar_parts_label),
				partValueText,
				partValueText[0],
				partValueText[partValueText.length-1]) ;
		
		////////////////////////////////////////////////////////////////////////
		// starting garbage last.
		max = ( GameModes.numberQPanes(mGameMode) * GameModes.numberRows(mGameMode) ) ;
		// range is from 0 to num rows - 2.
		vals = ArrayOps.range(0, max) ;
		partValueText = toStrings(vals) ;
		mStartingGarbageBar.setExplicitValues(
				vals,
				res.getString( R.string.new_game_starting_garbage_seek_bar_parts_label),
				partValueText,
				partValueText[0],
				partValueText[partValueText.length-1]) ;
		
		////////////////////////////////////////////////////////////////////////
		// per level garbage last.
		max = ( GameModes.numberQPanes(mGameMode) * GameModes.numberRows(mGameMode) ) ;
		// range is from 0 to num rows - 2.
		vals = ArrayOps.range(0, max) ;
		partValueText = toStrings(vals) ;
		mPerLevelGarbageBar.setExplicitValues(
				vals,
				res.getString( R.string.new_game_per_level_garbage_seek_bar_parts_label),
				partValueText,
				partValueText[0],
				partValueText[partValueText.length-1]) ;
		
		////////////////////////////////////////////////////////////////////////
		// displacement difficulty
		mDisplacementFixedRateBar.setRangeValues(
				DISPLACEMENT_FIXED_RATE_MIN,
				DISPLACEMENT_FIXED_RATE_MAX,
				DISPLACEMENT_FIXED_RATE_INTERVAL,
				DISPLACEMENT_FIXED_RATE_EXPONENT,
				res.getString(R.string.new_game_displacement_fixed_rate_seek_bar_parts_label),
				null, null) ;
		
		
		// Finally, set some as visible, some not.  Those which are
		// visible should have this Activity set as its listener.
		if ( GameModes.maxStartingLevel(mGameMode) > 1 ) {
			mLevelBar.setShow(true) ;
			mLevelBar.setShowMinMax(false) ;
			mLevelBar.setListener(this) ;
		} else
			mLevelBar.setShow(false) ;
		
		if ( GameModes.setClears(mGameMode) != GameModes.SET_CLEAR_NO ) {
			mClearsBar.setShow(true) ;
			mClearsBar.setShowMinMax(false) ;
			mClearsBar.setListener(this) ;
		}
		else
			mClearsBar.setShow(false) ;
		
		if ( GameModes.setStartingGarbage(mGameMode) != GameModes.SET_STARTING_GARBAGE_NO ) {
			mStartingGarbageBar.setShow(true) ;
			mStartingGarbageBar.setShowMinMax(false) ;
			mStartingGarbageBar.setListener(this) ;
		} else
			mStartingGarbageBar.setShow(false) ;
		
		if ( GameModes.setPerLevelGarbage(mGameMode) != GameModes.SET_PER_LEVEL_GARBAGE_NO ) {
			mPerLevelGarbageBar.setShow(true) ;
			mPerLevelGarbageBar.setShowMinMax(false) ;
			mPerLevelGarbageBar.setListener(this) ;
		} else
			mPerLevelGarbageBar.setShow(false) ;
		
		
		// Displacement rate?
		if ( GameModes.setDisplacementFixedRate(mGameMode) != GameModes.SET_DISPLACEMENT_FIXED_RATE_NO ) {
			mDisplacementFixedRateBar.setShow(true) ;
			mDisplacementFixedRateBar.setShowMinMax(false) ;
			mDisplacementFixedRateBar.setListener(this) ;
		} else {
			mDisplacementFixedRateBar.setShow(false) ;
		}
		
		// Level Locks?
		if ( GameModes.setLevelLock(mGameMode) == GameModes.SET_LEVEL_LOCK_NO ) {
			mLevelLockContainer.setVisibility(View.GONE) ;
		} else {
			mLevelLockCheckBox.setOnCheckedChangeListener(this) ;
		}
		
		// Difficulty?
		int setDifficulty = GameModes.setDifficulty(mGameMode) ;
		if ( setDifficulty == GameModes.SET_DIFFICULTY_NO_PRACTICE ) {
			mDifficultySpinnerAdapter = ArrayAdapter.createFromResource(this,
					R.array.new_game_difficulty_no_practice_array, android.R.layout.simple_spinner_item) ;
		} else {
			mDifficultySpinnerAdapter = ArrayAdapter.createFromResource(this,
					R.array.new_game_difficulty_array, android.R.layout.simple_spinner_item) ;
		}
		mDifficultySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) ;
		
		// apply the adapter
		mDifficultySpinner.setAdapter(mDifficultySpinnerAdapter) ;
		mDifficultySpinner.setSelection( this.difficultySpinnerValueToPosition(GameInformation.DIFFICULTY_NORMAL) ) ;	// value: quantro
		mDifficultySpinner.setOnItemSelectedListener(this) ;
		if ( setDifficulty == GameModes.SET_DIFFICULTY_NO ) {
			this.mDifficultyContainer.setVisibility(View.GONE) ;
		} else {
			this.mDifficultyContainer.setVisibility(View.VISIBLE) ;
		}
		
		if ( QuantroPreferences.getRememberCustomSetup(this) )
			restoreMostRecentSettings() ;
		else
			restoreDefaults() ;
	}
	
	private int difficultySpinnerValueToPosition( int diff ) {
		int setDifficulty = GameModes.setDifficulty(mGameMode) ;
		if ( setDifficulty == GameModes.SET_DIFFICULTY_YES ) {
			// right now, we go in practice, normal, hard order:
			// 0, 1, 2.
			switch( diff ) {
			case GameInformation.DIFFICULTY_PRACTICE:
				return 0 ;
			case GameInformation.DIFFICULTY_NORMAL:
				return 1 ;
			case GameInformation.DIFFICULTY_HARD:
				return 2 ;
			}
		} else if ( setDifficulty == GameModes.SET_DIFFICULTY_NO_PRACTICE ) {
			// right now, we go in normal, hard
			// 0, 1
			switch( diff ) {
			case GameInformation.DIFFICULTY_NORMAL:
				return 0 ;
			case GameInformation.DIFFICULTY_HARD:
				return 1 ;
			}
		}
		
		return 0 ;		// uh what?
	}
	
	private int difficultySpinnerPositionToValue( int pos ) {
		int setDifficulty = GameModes.setDifficulty(mGameMode) ;
		if ( setDifficulty == GameModes.SET_DIFFICULTY_YES ) {
			// right now, we go in practice, normal, hard order:
			// 0, 1, 2.
			switch( pos ) {
			case 0:
				return GameInformation.DIFFICULTY_PRACTICE ;
			case 1:
				return GameInformation.DIFFICULTY_NORMAL ;
			case 2:
				return GameInformation.DIFFICULTY_HARD ;
			}
		} else if ( setDifficulty == GameModes.SET_DIFFICULTY_NO_PRACTICE ) {
			// normal, hard: 0, 1
			switch( pos ) {
			case 0:
				return GameInformation.DIFFICULTY_NORMAL ;
			case 1:
				return GameInformation.DIFFICULTY_HARD ;
			}
		}
	
		
		return GameInformation.DIFFICULTY_NORMAL ;		// default normal
	}
	
	
	
	private int getFilterColor() {
		return GameModeColors.primary(mColorScheme, mGameMode) ;
	}
	
	private Drawable getDialogBackground() {
		return GameModeColors.blockBackgroundDrawable(this, mColorScheme, mGameMode) ;
	}
	
	
	private void setQueryString( View layout ) {
		// For now, we don't do anything.
	}
	
	private void setTitle( AlertDialog.Builder builder ) {
		int display = TextFormatting.DISPLAY_MENU ;
		int type = -1 ;
		int role = TextFormatting.ROLE_CLIENT ;
		
		if ( mStyle == STYLE_NEW_SINGLE_PLAYER_GAME )
			type = TextFormatting.TYPE_NEW_GAME_TITLE_NEW_GAME ;
		
		builder.setTitle(TextFormatting.format(this, res, display, type, role, mGameMode)) ;
	}
	
	
	private void setButton( AlertDialog.Builder builder, int buttonNum, int buttonType ) {
		String text ;
		DialogInterface.OnClickListener listener ;
		
		switch( buttonType ) {
		case BUTTON_GONE:
			return ;
		case BUTTON_TYPE_CANCEL:
			text = res.getString(R.string.new_game_button_cancel) ;
			listener = getOnClickListener( BUTTON_TYPE_CANCEL ) ;
			break ;
		case BUTTON_TYPE_PLAY:
			text = res.getString(R.string.new_game_button_play) ;
			listener = getOnClickListener( BUTTON_TYPE_PLAY ) ;
			break ;
		case BUTTON_TYPE_DEFAULT:
			text = res.getString(R.string.new_game_button_default) ;
			listener = getOnClickListener( BUTTON_TYPE_DEFAULT ) ;
			break ;
		default:
			throw new IllegalArgumentException("Unknown button type " + buttonType) ;
		}
		
		Integer color = getButtonColor( buttonType ) ;
		
		if ( color == null ) {
			switch( buttonNum ) {
			case AlertDialog.BUTTON_POSITIVE:
				builder.setPositiveButton(text, listener) ;
				break ;
			case AlertDialog.BUTTON_NEUTRAL:
				builder.setNeutralButton(text, listener) ;
				break ;
			case AlertDialog.BUTTON_NEGATIVE:
				builder.setNegativeButton(text, listener) ;
				break ;
			}
		} else {
			switch( buttonNum ) {
			case AlertDialog.BUTTON_POSITIVE:
				builder.setPositiveButton(text, color, listener) ;
				break ;
			case AlertDialog.BUTTON_NEUTRAL:
				builder.setNeutralButton(text, color, listener) ;
				break ;
			case AlertDialog.BUTTON_NEGATIVE:
				builder.setNegativeButton(text, color, listener) ;
				break ;
			}
		}
	}
	
	
	private Integer getButtonColor( int buttonType ) {
		switch( buttonType ) {
		case BUTTON_TYPE_CANCEL:
			return null ;
		case BUTTON_TYPE_DEFAULT:
			return GameModeColors.secondary(mColorScheme, mGameMode) ;
		case BUTTON_TYPE_PLAY:
			return GameModeColors.primary(mColorScheme, mGameMode) ;
		}
		
		return null ;
	}
	
	
	private DialogInterface.OnClickListener getOnClickListener( int buttonType ) {
		switch( buttonType ) {
		case BUTTON_TYPE_CANCEL:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mDialogManager.dismissAllDialogs() ;
					Intent i = new Intent() ;
					Intent intentIn = getIntent() ;
					if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
						Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
						i.putExtra(INTENT_EXTRA_ECHO, s) ;
					}
					i.putExtra(INTENT_RESULT_EXTRA_ACTION, INTENT_RESULT_EXTRA_ACTION_CANCEL) ;
			    	setResult(RESULT_OK, i) ;
			    	finish() ;
				}
			} ;
			
		case BUTTON_TYPE_DEFAULT:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					restoreDefaults() ;
					((AlertDialog)dialog).setButtonEnabled(which, false) ;
				}
			};
			
		case BUTTON_TYPE_PLAY:
			return new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					performClickPlay() ;
				}
			} ;
			
		default:
			return null ;
		}
	}
	
	private void performClickPlay() {
		mDialogManager.dismissAllDialogs() ;
		Intent i = new Intent() ;
		Intent intentIn = getIntent() ;
		if ( intentIn.hasExtra(INTENT_EXTRA_ECHO) ) {
			Serializable s = intentIn.getSerializableExtra(INTENT_EXTRA_ECHO) ;
			i.putExtra(INTENT_EXTRA_ECHO, s) ;
		}
    	i.putExtra(INTENT_RESULT_EXTRA_ACTION, INTENT_RESULT_EXTRA_ACTION_PLAY) ;
    	
    	GameSettings gs = new GameSettings(mGameMode) ;
    	updateMutableGameSettings( gs ) ;
    	
    	gs.setImmutable() ;
    	i.putExtra(INTENT_RESULT_EXTRA_GAME_SETTINGS, gs) ;
    	setResult(RESULT_OK, i) ;
    	finish() ;
	}
	
	private boolean isSetToDefaults() {
		int difficulty = GameModes.setDifficulty(mGameMode) == GameModes.SET_DIFFICULTY_NO
				? GameInformation.DIFFICULTY_NORMAL
				: difficultySpinnerPositionToValue(mDifficultySpinner.getSelectedItemPosition()) ;
		
		boolean defs = true ;
		defs = defs && mLevelBar.getProgress() == 0 ;
		defs = defs && ( GameModes.setClears(mGameMode) == GameModes.SET_CLEAR_NO
				|| mClearsBar.getProgress() == DEFAULT_CLEARS_PER_LEVEL_ARRAY_INDEX ) ;
		defs = defs && ( GameModes.setStartingGarbage(mGameMode) == GameModes.SET_STARTING_GARBAGE_NO
				|| mStartingGarbageBar.getProgress() == GameModes.defaultGarbage(mGameMode) ) ;
		defs = defs && ( GameModes.setPerLevelGarbage(mGameMode) == GameModes.SET_PER_LEVEL_GARBAGE_NO
				|| mPerLevelGarbageBar.getProgress() == GameModes.defaultGarbage(mGameMode) ) ;
		defs = defs && ( GameModes.setLevelLock(mGameMode) == GameModes.SET_LEVEL_LOCK_NO
				|| !mLevelLockCheckBox.isChecked() ) ;
		defs = defs && ( GameModes.setDifficulty(mGameMode) == GameModes.SET_DIFFICULTY_NO
				|| difficulty == GameInformation.DIFFICULTY_NORMAL ) ;
		defs = defs && ( GameModes.setDisplacementFixedRate(mGameMode) == GameModes.SET_DISPLACEMENT_FIXED_RATE_NO
				|| ( GameModes.setDisplacementFixedRate(mGameMode) == GameModes.SET_DISPLACEMENT_FIXED_RATE_PRACTICE
						&& difficulty != GameInformation.DIFFICULTY_PRACTICE )
				|| this.mDisplacementFixedRateBar.getProgress() == 0 ) ;
		
		return defs ;
	}
	
	
	private void updateMutableGameSettings( GameSettings gs ) {
		gs.setMode(mGameMode) ;
		
		if ( mLevelBar.intValue() != 1 )
    		gs.setLevel( mLevelBar.intValue() ) ;
		else
			gs.unsetLevel() ;
		
    	if ( GameModes.setClears(mGameMode) != GameModes.SET_CLEAR_NO && mClearsBar.getProgress() != DEFAULT_CLEARS_PER_LEVEL_ARRAY_INDEX  )
    		gs.setClearsPerLevel( mClearsBar.intValue() ) ;
    	else
    		gs.unsetClearsPerLevel() ;
    	
    	if ( GameModes.setStartingGarbage(mGameMode) != GameModes.SET_STARTING_GARBAGE_NO && mStartingGarbageBar.intValue() != GameModes.defaultGarbage(mGameMode) )
    		gs.setGarbage( mStartingGarbageBar.intValue() ) ;
    	else
    		gs.unsetGarbage() ;
    	
    	if ( GameModes.setPerLevelGarbage(mGameMode) != GameModes.SET_PER_LEVEL_GARBAGE_NO && mPerLevelGarbageBar.intValue() != GameModes.defaultGarbage(mGameMode ) )
    		gs.setGarbagePerLevel( mPerLevelGarbageBar.intValue() ) ;
    	else
    		gs.unsetGarbagePerLevel() ;
    	
    	if ( GameModes.setLevelLock(mGameMode) != GameModes.SET_LEVEL_LOCK_NO )
    		gs.setLevelLock( mLevelLockCheckBox.isChecked() ) ;
    	else
    		gs.unsetLevelLock() ;
    	
    	if ( GameModes.setDifficulty(mGameMode) != GameModes.SET_DIFFICULTY_NO )
    		gs.setDifficulty( difficultySpinnerPositionToValue( mDifficultySpinner.getSelectedItemPosition() ) ) ;
    	else
    		gs.unsetDifficulty() ;
    	
    	gs.unsetDisplacementFixedRate() ;
    	switch( GameModes.setDisplacementFixedRate(mGameMode) ) {
    	case GameModes.SET_DISPLACEMENT_FIXED_RATE_NO:
    		break ;
    	case GameModes.SET_DISPLACEMENT_FIXED_RATE_PRACTICE:
    		if ( gs.getDifficulty() != GameInformation.DIFFICULTY_PRACTICE )
    			break ;
    		// fall through to YES
    	case GameModes.SET_DISPLACEMENT_FIXED_RATE_YES:
    		gs.setDisplacementFixedRate( ((double)mDisplacementFixedRateBar.intValue()) / DISPLACEMENT_FIXED_RATE_DIVISOR ) ;
    		break ;
    	}
	}
	
	private void setAvailableFeaturesBasedOnDifficulty( int difficulty ) {
		int setDisplacementFixedRate = GameModes.setDisplacementFixedRate(mGameMode) ;
		boolean showDisplacementFixedRate = false ;
		switch( setDisplacementFixedRate ) {
		case GameModes.SET_DISPLACEMENT_FIXED_RATE_NO:
			showDisplacementFixedRate = false ;
			break ;
		case GameModes.SET_DISPLACEMENT_FIXED_RATE_PRACTICE:
			showDisplacementFixedRate = difficulty == GameInformation.DIFFICULTY_PRACTICE ;
			break ;
		case GameModes.SET_DISPLACEMENT_FIXED_RATE_YES:
			showDisplacementFixedRate = true ;
			break ;
		}
		mDisplacementFixedRateBar.setShow(showDisplacementFixedRate) ;
		
		boolean showSkipAnimationsWarning = difficulty == GameInformation.DIFFICULTY_HARD
				&& !QuantroPreferences.getGraphicsSkipAnimations(this) ;
		mDifficultySkipAnimationsWarningView.setVisibility(
				showSkipAnimationsWarning ? View.VISIBLE : View.GONE) ;
	}
	
	private void setLeaderboardString() {
		updateMutableGameSettings(mCurrentGameSettings) ;
		// three possibilities: no leaderboard (game mode), no leaderboard (settings),
		// OKAY leaderboard.
		
		if ( mLeaderboardSupportedTextView != null ) {
			if ( !Scores.isLeaderboardSupported( mCurrentGameSettings.getMode() ) ) {
				// this game mode does not support a leaderboard
				mLeaderboardSupportedTextView.setText(R.string.new_game_leaderboard_supported_no_game_mode) ;
			} else if ( !Scores.isLeaderboardSupported(mCurrentGameSettings) ) {
				// these game settings are the reason we don't support a leaderboard
				mLeaderboardSupportedTextView.setText(R.string.new_game_leaderboard_supported_no_settings) ;
			} else {
				mLeaderboardSupportedTextView.setText(R.string.new_game_leaderboard_supported_yes) ;
			}
		}
	}
	
	private void restoreDefaults() {
		// Finally, set some as visible, some not.  Those which are
		// visible should have this Activity set as its listener.
		mLevelBar.setProgress(0) ;
		
		mClearsBar.setProgress(DEFAULT_CLEARS_PER_LEVEL_ARRAY_INDEX) ;
		
		mStartingGarbageBar.setProgress( GameModes.defaultGarbage(mGameMode) ) ;	// # is also index
		
		mPerLevelGarbageBar.setProgress( GameModes.defaultGarbage(mGameMode) ) ;	// # is also index
		
		mLevelLockCheckBox.setChecked( false ) ;
		
		mDifficultySpinner.setSelection( this.difficultySpinnerValueToPosition(GameInformation.DIFFICULTY_NORMAL) ) ;
		
		mDisplacementFixedRateBar.setProgress(0) ;
		
		// set visibility of things based on difficulty...?
		setAvailableFeaturesBasedOnDifficulty( GameInformation.DIFFICULTY_NORMAL ) ;
		
		// set leaderboard based on settings...?
		updateMutableGameSettings(mCurrentGameSettings) ;
		setLeaderboardString() ;
	}
	
	private void restoreMostRecentSettings() {
		restoreDefaults() ;
		
		GameSettings gs = GameSettingsDatabaseAdapter.getMostRecentInDatabase(this, mGameMode) ;
		
		if ( gs.hasLevel() )
			mLevelBar.setProgress(gs.getLevel()-1) ;	// index is level -1.
		
		if ( gs.hasClearsPerLevel() ) {
			// find the nearest entry....
			int nearest = 0 ;
			for ( int i = 0; i < CLEARS_PER_LEVEL_ARRAY.length; i++ ) {
				if ( gs.getClearsPerLevel() >= CLEARS_PER_LEVEL_ARRAY[i] )
					nearest = i ;
				else
					break ;
			}
			mClearsBar.setProgress(nearest) ;
		}
		
		if ( gs.hasGarbage() )
			mStartingGarbageBar.setProgress( gs.getGarbage() ) ;
		
		if ( gs.hasGarbagePerLevel() )
			mPerLevelGarbageBar.setProgress( gs.getGarbagePerLevel() ) ;
		
		if ( gs.hasLevelLock() )
			mLevelLockCheckBox.setChecked( gs.getLevelLock() ) ;
		
		if ( gs.hasDifficulty() )
			mDifficultySpinner.setSelection( difficultySpinnerValueToPosition(gs.getDifficulty()) ) ;
		
		if ( gs.hasDisplacementFixedRate() )
			this.mDisplacementFixedRateBar.setIntValue( (int)Math.round(gs.getDisplacementFixedRate() * DISPLACEMENT_FIXED_RATE_DIVISOR) ) ; 
		
		updateMutableGameSettings(mCurrentGameSettings) ;
		setLeaderboardString() ;
	}
	
	private String [] toStrings( int [] vals ) {
		String [] s = new String[vals.length] ;
		for ( int i = 0; i < s.length; i++ ) {
			if ( vals[i] == Integer.MIN_VALUE )
				s[i] = "-\u221E" ;
			else if ( vals[i] == Integer.MAX_VALUE )
				s[i] = "\u221E" ;
			else
				s[i] = "" + vals[i] ;
		}
		return s ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// LABELED SEEK BAR ADAPTER LISTENER METHODS
	//
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Notification that the progress level has changed. Clients can use
	 * the fromUser parameter to distinguish user-initiated changes from
	 * those that occurred programmatically.
	 * 
	 * @param lsba The LabeledSeekBarAdapter object.
	 * @param seekBar The SeekBar whose progress has changed
	 * @param progress 	The current progress level. This will be in the range 0..max where max was set by setMax(int). (The default value for max is 100.)
	 * @param fromUser 	True if the progress change was initiated by the user. 
	 * @param value The value corresponding to this level of progress
	 */
	public void onProgressChanged( LabeledSeekBarAdapter lsba, SeekBar seekBar, int progress, boolean fromUser, int value ) {
		if ( mDialog != null ) {
			mDialog.setButtonEnabled(AlertDialog.BUTTON_NEUTRAL, !isSetToDefaults()) ;
			
			// update leaderboard string
			updateMutableGameSettings(mCurrentGameSettings) ;
			setLeaderboardString() ;
		}
	}
	
	/**
	 * Notification that the progress level has changed. Clients can use
	 * the fromUser parameter to distinguish user-initiated changes from
	 * those that occurred programmatically.
	 * 
	 * @param lsba The LabeledSeekBarAdapter object.
	 * @param seekBar The SeekBar whose progress has changed
	 * @param progress 	The current progress level. This will be in the range 0..max where max was set by setMax(int). (The default value for max is 100.)
	 * @param fromUser 	True if the progress change was initiated by the user. 
	 * @param value The value corresponding to this level of progress
	 */
	public void onProgressChanged( LabeledSeekBarAdapter lsba, SeekBar seekBar, int progress, boolean fromUser, float value ) {
		// we only use ints, so we don't bother with this right now.
	}
	
	/**
	 * Notification that the progress level has changed. Clients can use
	 * the fromUser parameter to distinguish user-initiated changes from
	 * those that occurred programmatically.
	 * 
	 * @param lsba The LabeledSeekBarAdapter object.
	 * @param seekBar The SeekBar whose progress has changed
	 * @param progress 	The current progress level. This will be in the range 0..max where max was set by setMax(int). (The default value for max is 100.)
	 * @param fromUser 	True if the progress change was initiated by the user. 
	 * @param value The value corresponding to this level of progress
	 */
	public void onProgressChanged( LabeledSeekBarAdapter lsba, SeekBar seekBar, int progress, boolean fromUser, Object value ) {
		// we only use ints, so we don't bother with this right now.
	}

	/**
	 * Notification that the user has started a touch gesture. Clients may want to use this to disable advancing the seekbar.
	 *
	 * @param lsba The LabeledSeekBarAdapter object
	 * @param seekBar 	The SeekBar in which the touch gesture began 
	 */
	public void onStartTrackingTouch( LabeledSeekBarAdapter lsba, SeekBar seekBar ) {
		// don't care!
	}
	
	/**
	 * Notification that the user has finished a touch gesture. Clients may want to use this to re-enable advancing the seekbar.
	 * 
	 * @param lsba The LabeledSeekBarAdapter object
	 * @param seekBar 	The SeekBar in which the touch gesture began 
	 */
	public void onStopTrackingTouch( LabeledSeekBarAdapter lsba, SeekBar seekBar ) {
		// don't care!
	}


	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		if ( mDialog != null ) {
			mDialog.setButtonEnabled(AlertDialog.BUTTON_NEUTRAL, !isSetToDefaults()) ;
			// set leaderboard value
			updateMutableGameSettings(mCurrentGameSettings) ;
			setLeaderboardString() ;
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// SPINNER ON_ITEM_SELECTED METHODS
	//
	////////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
			long arg3) {
		
		if ( arg0 == this.mDifficultySpinner ) {
			if ( mDialog != null ) {
				mDialog.setButtonEnabled(AlertDialog.BUTTON_NEUTRAL, !isSetToDefaults()) ;
			
				// Set difficulty
				setAvailableFeaturesBasedOnDifficulty( this.difficultySpinnerPositionToValue(pos) ) ;
				
				// update 'leaderboard' setting.
				updateMutableGameSettings(mCurrentGameSettings) ;
				setLeaderboardString() ;
			}
		}
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// do nothing
	}
	
}
