package com.peaceray.quantro;

import java.util.Date;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.database.CustomGameModeSettingsDatabaseAdapter;
import com.peaceray.quantro.database.GameSettingsDatabaseAdapter;
import com.peaceray.quantro.database.GameStats;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.dialog.GlobalDialog;
import com.peaceray.quantro.model.GameSaver;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.CustomGameModeSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.Scores;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.freeplay.FreePlayGameManagerView;
import com.peaceray.quantro.view.options.OptionAvailability;

public class FreePlayGameManagerActivity extends QuantroActivity
		implements FreePlayGameManagerView.Delegate, GlobalDialog.DialogContext {

	private static final String TAG = "FreePlayGameManagerActivity" ;
	
	
	private static final int DIALOG_ID_TOO_MANY_CUSTOM_GAME_MODES = 0 ;
	
	// View 
	private FreePlayGameManagerView mFreePlayGameManagerView ;
	
	// Dialogs
	private DialogManager mDialogManager ;
	
	// Sound pool and controls
	ColorScheme mColorScheme ;
	QuantroSoundPool mSoundPool ;
	boolean mSoundControls ;
	
	boolean mStarted  ;
	boolean mModesSet ;
	
	@Override
	public synchronized void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupQuantroActivity( QUANTRO_ACTIVITY_MENU, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
        
        Log.d(TAG, "onCreate") ;
        
        
        // The FreePlay game manager uses GameViewMemoryCapabilities.  Make
        // sure such an object exists before inflating.
        ((QuantroApplication)getApplication()).getGameViewMemoryCapabilities(this) ;
        // LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
        setContentView(R.layout.free_play_game_manager_strip_view) ;
        
        mDialogManager = new DialogManager(this) ;
        
        mFreePlayGameManagerView = (FreePlayGameManagerView) findViewById(R.id.free_play_game_manager_strip_view) ;
        mFreePlayGameManagerView.setDelegate(this) ;
        
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
	}
	
	@Override
	public synchronized void onStart() {
		super.onStart() ;
		mStarted = true ;

		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		mSoundControls = QuantroPreferences.getSoundControls(this) ;
		mSoundPool = ((QuantroApplication)getApplication()).getSoundPool(this) ;
		
		mFreePlayGameManagerView.setColorScheme( mColorScheme ) ;
		mFreePlayGameManagerView.setSoundControls( mSoundControls ) ;
		mFreePlayGameManagerView.setSoundPool( mSoundPool ) ;
		
		mFreePlayGameManagerView.start() ;
		
		// load game modes...
		new PopulateGameModes().execute() ;
	}
	
	
	@Override
	public synchronized void onResume() {
		super.onResume() ;
		
		mDialogManager.revealDialogs() ;
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause() ;
		
		mDialogManager.hideDialogs() ;
	}
	
	

	
	@Override
	public synchronized void onStop() {
		super.onStop() ;
		mStarted = false ;
		
		mFreePlayGameManagerView.stop() ;
	}

	
	private class PopulateGameModes extends AsyncTask<Object, Object, Object> {
		@Override
		protected Object doInBackground(Object... arg0) {
			
			if ( !mModesSet ) {
				// worth loading game modes here.
				int [] modes = GameModes.getSinglePlayerFreePlayGameModes() ;
		    	int [] customModes = GameModes.getCustomGameModes(true, false) ;
		    	
		    	/*
		    	int [] allModes = new int[modes.length + customModes.length] ;
		    	OptionAvailability [] allAvailability = new OptionAvailability[allModes.length] ;
		    	boolean [] allHasResult = new boolean[allModes.length] ;
		    	int index = 0 ;
		    	for ( int i = 0; i < modes.length; i++ ) {
		    		allModes[index++] = modes[i] ;
		    	}
		    	for ( int i = 0; i < customModes.length; i++ ) {
		    		allModes[index++] = customModes[i] ;
		    	}
		    	
		    	for ( int i = 0; i < allModes.length; i++ ) {
		    		OptionAvailability availability = OptionAvailability.LOCKED_ENABLED ;
					if ( getPremiumLibrary().hasTrialOnlyGameMode(allModes[i]) )
						availability = OptionAvailability.TIMED_ENABLED ;
					else if ( getPremiumLibrary().hasGameMode(allModes[i]) )
						availability = OptionAvailability.ENABLED ;
					
					allAvailability[i] = availability ;
					allHasResult[i] = GameSaver.hasGameResult(FreePlayGameManagerActivity.this, GameSaver.freePlayGameModeToSaveKey(allModes[i])) ;
		    	}
		    	
		    	if ( !mStarted )
		    		return null ;
		    	
		    	mFreePlayGameManagerView.setGames(allModes, allAvailability, allHasResult) ;
		    	*/
		    	
		    	
				for ( int i = 0; i < modes.length; i++ ) {
					OptionAvailability availability = OptionAvailability.LOCKED_ENABLED ;
					if ( getPremiumLibrary().hasTrialOnlyGameMode(modes[i]) )
						availability = OptionAvailability.TIMED_ENABLED ;
					else if ( getPremiumLibrary().hasGameMode(modes[i]) )
						availability = OptionAvailability.ENABLED ;
					mFreePlayGameManagerView.addGame(
							modes[i],
							availability,
							GameSaver.hasGameResult(FreePlayGameManagerActivity.this, GameSaver.freePlayGameModeToSaveKey(modes[i]))) ;
					if ( !mStarted )
						return null ;
				}
				
				for ( int i = 0; i < customModes.length; i++ ) {
					OptionAvailability availability = OptionAvailability.LOCKED_ENABLED ;
					if ( getPremiumLibrary().hasTrialOnlyGameMode(customModes[i]) )
						availability = OptionAvailability.TIMED_ENABLED ;
					else if ( getPremiumLibrary().hasGameMode(customModes[i]) )
						availability = OptionAvailability.ENABLED ;
					mFreePlayGameManagerView.addGame(
							customModes[i],
							availability,
							GameSaver.hasGameResult(FreePlayGameManagerActivity.this, GameSaver.freePlayGameModeToSaveKey(customModes[i]))) ;
					if ( !mStarted )
						return null ;
				}
				
			
				mModesSet = true ;
			}
			
			return null ;
		}
		
		protected void onPostExecute( Object result ) {
			if ( mStarted ) {
				new PopulateGameModeSaves().execute() ;
			}
		}
	}
	
	
	private class PopulateGameModeSaves extends AsyncTask<Object, Object, Object> {
		@Override
		protected Object doInBackground(Object... arg0) {
			int [] modes = GameModes.getSinglePlayerFreePlayGameModes() ;
	    	int [] customModes = GameModes.getCustomGameModes(true, false) ;
			for ( int i = 0; i < modes.length; i++ ) {
				String key = GameSaver.freePlayGameModeToSaveKey(modes[i]) ;
				if ( GameSaver.hasGameResult(FreePlayGameManagerActivity.this, key) )
					mFreePlayGameManagerView.setGameResult(modes[i], GameSaver.loadGameResult(FreePlayGameManagerActivity.this, key)) ;
				if ( !mStarted )
					return null ;
			}
			
			for ( int i = 0; i < customModes.length; i++ ) {
				String key = GameSaver.freePlayGameModeToSaveKey(customModes[i]) ;
				if ( GameSaver.hasGameResult(FreePlayGameManagerActivity.this, key) )
					mFreePlayGameManagerView.setGameResult(customModes[i], GameSaver.loadGameResult(FreePlayGameManagerActivity.this, key)) ;
				if ( !mStarted )
					return null ;
			}
			
			return null ;
		}
	}
	
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	//Log.d(TAG, "onKeyDown bracket IN") ;
    	Log.d(TAG, "onKeyDown " + keyCode + ", event count " + event.getRepeatCount()) ;
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	
            if ( mSoundControls )
        		mSoundPool.menuButtonBack() ;
        	
        	finish() ;        	
            return true;
        }
        
        return super.onKeyDown(keyCode, event) ;
    }

	
	/*
	 * *************************************************************************
	 * 
	 * MENU CALLBACKS
	 * 
	 * For creating, displaying, and processing touches to an options menu.
	 * 
	 * *************************************************************************
	 */
    
    
    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
    	//Log.d(TAG, "onCreateOptionsMenu bracket IN") ;
    	super.onCreateOptionsMenu(menu) ;
    	
    	int id = 0 ;
    	switch( VersionSafe.getScreenSizeCategory(this) ) {
    	case VersionSafe.SCREEN_SIZE_SMALL:
    	case VersionSafe.SCREEN_SIZE_NORMAL:
        	id = R.menu.freeplay_overflow_normal ;
        	break ;
    	case VersionSafe.SCREEN_SIZE_LARGE:
    		id = R.menu.freeplay_overflow_large ;
        	break ;	
    	case VersionSafe.SCREEN_SIZE_XLARGE:
            break ;
    	}
    	
    	// disable for android 3.0.
    	if ( id != 0 && VersionCapabilities.supportsOptionsMenu() ) {
	    	MenuInflater inflater = getMenuInflater() ;
	    	inflater.inflate(id, menu) ;
	    	return true ;
    	}
    	
    	return false ;
    }
    
    
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch( item.getItemId() ) {
    	case R.id.overflow_options_menu_help:
    		mDialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
    		return true ;
    	case R.id.overflow_options_menu_settings:
    		// launch settings
    		this.startQuantroPreferencesActivity() ;
    		return true ;
    	}
    	return false ;
    }
    
    
    
	/*
	 * *************************************************************************
	 * 
	 * DIALOGS
	 * 
	 * *************************************************************************
	 */
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder ;
    	
    	if ( GlobalDialog.hasDialog(id) )
    		return GlobalDialog.onCreateDialog(this, id, mDialogManager) ;
    	
    	switch( id ) {
    	case DIALOG_ID_TOO_MANY_CUSTOM_GAME_MODES:
    		builder = new AlertDialog.Builder(this) ;
    		builder.setTitle(R.string.free_play_maximum_custom_games_reached_title) ;
    		builder.setMessage(R.string.free_play_maximum_custom_games_reached_description) ;
    		builder.setCancelable(true) ;
    		builder.setNegativeButton(R.string.free_play_maximum_custom_games_reached_button_ok,
    				new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mDialogManager.dismissDialog(DIALOG_ID_TOO_MANY_CUSTOM_GAME_MODES) ;
						}
    				}) ;
    		builder.setOnCancelListener( new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ID_TOO_MANY_CUSTOM_GAME_MODES) ;
				}
    		}) ;
    		return builder.create() ;
    		
		default:
			throw new IllegalArgumentException("Don't know how to make dialog id " + id) ;
    	}
    }
    
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
    	if ( GlobalDialog.hasDialog(id) )
    		GlobalDialog.onPrepareDialog(this, id, dialog) ;
    	else {
	    	// prepare whatever
    	}
    }
	
	
	////////////////////////////////////////////////////////////////////////////
	// LAUNCHING ACTIVITIES
	//
	
	//
	// GAME SETUP //////////////////////////////////////////////////////////////
	// 
	
	protected void startExamineGameResultActivity( int gameMode ) {
    	startExamineGameResultActivity( gameMode, GameSaver.freePlayGameModeToSaveKey(gameMode) ) ;
    }
    
    protected void startExamineGameResultActivity( int gameMode, String saveKey ) {
    	GameResult gr = GameSaver.loadGameResult(this, saveKey) ;
		GameSettings gs = GameSaver.hasGameSettings(this, saveKey) ? GameSaver.loadGameSettings(this, saveKey) : null ;
		
		// Launch an ExamineGameResultActivity.
		Intent intent = new Intent( this, ExamineGameResultActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_STYLE,
    			ExamineGameResultActivity.STYLE_SAVED_GAME) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_GAME_RESULT,
    			gr) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_GAME_SETTINGS,
    			gs) ;
    	intent.putExtra(
    			ExamineGameResultActivity.INTENT_EXTRA_ECHO,
    			GameModes.gameModeIntegerObject(gameMode)) ;
    	
    	intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP ) ;
    	
    	startActivityForResult(intent, IntentForResult.REQUEST_EXAMINE_GAME_RESULT) ;
    }
    
    protected void startNewGameSetupActivity( int gameMode ) {
    	Intent intent = new Intent( this, NewGameSettingsActivity.class ) ;
		intent.setAction( Intent.ACTION_MAIN ) ;
		intent.putExtra( 
				NewGameSettingsActivity.INTENT_EXTRA_STYLE,
				NewGameSettingsActivity.STYLE_NEW_SINGLE_PLAYER_GAME) ;
		intent.putExtra( 
				NewGameSettingsActivity.INTENT_EXTRA_GAME_MODE,
				gameMode) ;
		intent.putExtra( 
				NewGameSettingsActivity.INTENT_EXTRA_ECHO,
				GameModes.gameModeIntegerObject(gameMode)) ;
		
		intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP ) ;
		
		startActivityForResult(intent, IntentForResult.NEW_GAME_SETTINGS) ;
    }
	
	//
	// LAUNCHING GAMES /////////////////////////////////////////////////////////
	// 
	
	private String makeLoadToast( GameSettings gs ) {
    	// e.g. "Loading Quantro Endurance"
    	// We expect the resource string to be something like
    	// "Loading GM_N_XXXX"; replace the placeholder with the game mode name.
    	Resources res = getResources() ;
    	String placeholder_name = res.getString( R.string.placeholder_game_mode_name ) ;
    	
    	String base = res.getString( R.string.play_load ) ;
    	return base.replace(placeholder_name, GameModes.name(gs.getMode())) ;
    }
	
	private String makeNewGameToast( GameSettings gs ) {
    	// e.g. "Starting Quantro Progression"
		// or 	"Starting Quantro Progression: Level 5, Garbage --/4"
    	// We expect the resource string to be something like
    	// "Loading GM_N_XXXX"; replace the placeholder with the game mode name.
    	Resources res = getResources() ;
    	String placeholder_name = res.getString( R.string.placeholder_game_mode_name ) ;
    	if ( gs.hasDefaultsIgnoringDifficulty() ) 
    		return res.getString( R.string.play_new ).replace(placeholder_name, GameModes.name(gs.getMode())) ;
    	else {
    		// create custom string.
    		boolean has = false ;
    		StringBuilder sb = new StringBuilder() ;
    		
    		String sep = res.getString(R.string.play_list_separator) ;
    		
    		// Custom level?
    		if ( gs.hasLevel() ) {
    			String str ;
    			if ( gs.hasLevelLock() && gs.getLevelLock() )
    				str = res.getString(R.string.play_list_level_locked) ;
    			else
    				str = res.getString(R.string.play_list_level) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_level),
    					"" + gs.getLevel()) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		// Custom clears-per?
    		if ( gs.hasClearsPerLevel() ) {
    			String str = res.getString(R.string.play_list_clears_per_level) ;
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
    			String strDefault = res.getString(R.string.play_list_default) ;
    			String str = res.getString(R.string.play_list_garbage) ;
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
    		
    		
    		// Custom displacement?
    		if ( gs.hasDisplacementFixedRate() ) {
    			String str = res.getString(R.string.play_list_displacement_fixed_rate) ;
    			str = str.replace(
    					res.getString(R.string.placeholder_game_settings_custom_value_displacement_fixed_rate),
    					"" + String.format("%.2f", gs.getDisplacementFixedRate())) ;
    			if ( has )
    				sb.append(sep) ;
    			sb.append(str) ;
    			has = true ;
    		}
    		
    		
    		// now load the custom new string and place this customized list within it.
    		String str = res.getString( R.string.play_new_custom) ;
    		str = str.replace(
    				res.getString(R.string.placeholder_game_settings_custom_list),
    				sb.toString()) ;
    		str = str.replace(placeholder_name, GameModes.name(gs.getMode())) ;
    		return str ;
    	}
    }
	
	private void startGameActivity( int mode, String loadFromKey, String saveToKey, boolean showToast ) {
    	GameSettings gs = QuantroPreferences.getRememberCustomSetup(this)
    			? GameSettingsDatabaseAdapter.getMostRecentInDatabase(this, mode)
    			: new GameSettings(mode, 1).setImmutable() ;
    	
    	if ( showToast ) {
    		if ( loadFromKey != null )
    			Toast.makeText(this, this.makeLoadToast(gs), Toast.LENGTH_SHORT).show() ;
    		else
    			Toast.makeText(this, this.makeNewGameToast(gs), Toast.LENGTH_SHORT).show() ;
    	}
    	
    	Intent intent = new Intent( this, GameActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	intent.putExtra(GameActivity.INTENT_EXTRA_SAVE_THUMBNAIL, this.mFreePlayGameManagerView.supportsThumbnails()) ;
    	
    	GameIntentPackage gip = new GameIntentPackage( gs ) ;
    	if ( saveToKey != null )
    		gip.setConnectionAsLocal(loadFromKey, saveToKey) ;
    	
    	// We're about to start a game with the specified mode.  If 'loadFromKey'
    	// is null, this is a new game.  Make a note in our GameStats database.
    	if ( loadFromKey == null )
    		GameStats.DatabaseAdapter.addNewGameStartedToDatabase(this, mode) ;
    	
    	intent.putExtra( GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE, gip) ;
    	startActivityForResult(intent, IntentForResult.LAUNCH_GAME) ;
    	
    	// log
    	if ( QuantroPreferences.getAnalyticsActive(this) ) {
    		if ( loadFromKey == null ) {
    			// no load; a new game.
    			Analytics.logNewGame(gip.gameSettings, false) ;
    		} else {
    			// a load!
    			GameResult gr = GameSaver.loadGameResult(this, loadFromKey) ;
    			if ( gr != null ) {
    				Date dateStarted = gr.getDateStarted() ;
    				Date dateEnded = gr.getDateEnded() ;
    				long timeSinceStart = -1 ;
    				long timeSinceEnd = -1 ;
    				if ( dateStarted != null )
    					timeSinceStart = System.currentTimeMillis() - dateStarted.getTime() ;
    				if ( dateEnded != null )
    					timeSinceEnd = System.currentTimeMillis() - dateEnded.getTime() ;
    				Analytics.logLoadGame(gip.gameSettings, timeSinceStart, timeSinceEnd, false) ;
    			}
    		}
    	}
    }
    
    
    private void startGameActivity( String saveToKey, GameSettings gs, boolean showToast ) {
    	if ( gs == null )
    		throw new NullPointerException("Must provide a non-null GameSettings object.") ;
    	
    	if ( showToast )
    		Toast.makeText(this, this.makeNewGameToast(gs), Toast.LENGTH_SHORT).show() ;
    	
    	Intent intent = new Intent( this, GameActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	intent.putExtra(GameActivity.INTENT_EXTRA_SAVE_THUMBNAIL, this.mFreePlayGameManagerView.supportsThumbnails()) ;
    	
    	GameIntentPackage gip = new GameIntentPackage( gs ) ;
    	if ( saveToKey != null )
    		gip.setConnectionAsLocal(null, saveToKey) ;
    	
    	// We're about to start a game with the specified mode.  Make a note
    	// in our database.
    	GameStats.DatabaseAdapter.addNewGameStartedToDatabase(this, gs.getMode()) ;
    	
    	intent.putExtra( GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE, gip) ;
    	startActivityForResult(intent, IntentForResult.LAUNCH_GAME) ;
    	
    	// log
    	if ( QuantroPreferences.getAnalyticsActive(this) ) {
			// no load; a new game.
			Analytics.logNewGame(gs, false) ;
    	}
    	//Log.d(TAG, "startGameActivity bracket OUT") ;
    }
    
    
    
    //
	// CUSTOM GAME EDIT ////////////////////////////////////////////////////////
	// 
    
    
    protected void startCustomGameModeActivity( int gameMode ) {
    	// find the CGMS.
    	if ( GameModes.isCustom(gameMode) )
    		startCustomGameModeActivity( CustomGameModeSettingsDatabaseAdapter.get(this, GameModes.gameModeToCustomID(gameMode)) ) ;
    }
    
    protected void startCustomGameModeActivity( CustomGameModeSettings cgms ) {
    	
    	int id ;
    	boolean hasSaves = false ;
    	
    	if ( cgms == null ) {
    		id = GameModes.getFreeCustomGameModeSettingID() ;
    	} else {
    		id = cgms.getID() ;
    		int [] modes = GameModes.getCustomGameModes(id) ;
    		
    		for ( int i = 0; i < modes.length; i++ )
    			hasSaves = hasSaves || GameSaver.hasGameStates(this, GameSaver.freePlayGameModeToSaveKey(modes[i])) ;
    	}
    	
    	Intent i = new Intent( this, CustomGameModeActivity.class ) ;
    	if ( cgms != null )
    		i.putExtra(CustomGameModeActivity.INTENT_EXTRA_CGMS, cgms) ;
    	else
    		i.putExtra(CustomGameModeActivity.INTENT_EXTRA_CGMS_ID, id) ;
    	
    	i.putExtra(CustomGameModeActivity.INTENT_EXTRA_HAS_SAVES, hasSaves) ;
    	i.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP ) ;
    	
    	startActivityForResult( i, IntentForResult.CUSTOM_GAME_MODE ) ;
    	
    	// log
    	if ( QuantroPreferences.getAnalyticsActive(this) ) {
			// no load; a new game.
			Analytics.logCustomGameMode(id, cgms == null, hasSaves) ;
    	}
    }
    
    
    //
    // SETTINGS ACTIVITIES /////////////////////////////////////////////////////
    //
    
    protected void startQuantroPreferencesActivity() {
    	// launch settings
		Intent intent = new Intent( this, QuantroPreferences.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	startActivity(intent) ;
    	//Log.d(TAG, "onOptionsItemSelected bracket OUT") ;
    }
    
	
	//
	// END launching activities
	////////////////////////////////////////////////////////////////////////////
	
	
    
    
	////////////////////////////////////////////////////////////////////////////
	// ACTIVITY RESULT
	//
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if ( super.handleActivityResult(requestCode, resultCode, data) )
    		return ;
    	
    	//Log.d(TAG, "onActivityResult bracket IN") ;
    	Log.d(TAG, "onActivityResult") ;
    	switch( requestCode ) {
    	case IntentForResult.REQUEST_EXAMINE_GAME_RESULT:
    		if (resultCode == RESULT_OK) {  
            	
            	if ( data != null && data.hasExtra(ExamineGameResultActivity.INTENT_RESULT_EXTRA) ) {
            		int action = data.getIntExtra(ExamineGameResultActivity.INTENT_RESULT_EXTRA, -1) ;
            		if ( action == ExamineGameResultActivity.INTENT_RESULT_CONTINUE ) {
            			Integer gameMode = (Integer)data.getSerializableExtra(ExamineGameResultActivity.INTENT_EXTRA_ECHO) ;
            			String saveKey = GameSaver.freePlayGameModeToSaveKey(gameMode) ;
            			startGameActivity( gameMode, saveKey, saveKey, false ) ;
            			return ;
            		}
            		else if ( action == ExamineGameResultActivity.INTENT_RESULT_DELETE ) {
            			Integer gameMode = (Integer)data.getSerializableExtra(ExamineGameResultActivity.INTENT_EXTRA_ECHO) ;
            			String saveKey = GameSaver.freePlayGameModeToSaveKey(gameMode) ;
            			GameResult gr = GameSaver.loadGameResult(this, saveKey) ;
            			GameStats.DatabaseAdapter.addGameResultToDatabase(this, gr) ;
            			GameSaver.deleteGame(this, saveKey) ;
            			mFreePlayGameManagerView.setGameResult(gameMode, false) ;
            			mFreePlayGameManagerView.refreshView() ;
            			
            			if ( QuantroPreferences.getAnalyticsActive(this) )
            				doLogDelete( gameMode, gr, true ) ;
            		}
            	}
          
            } else {  
                // gracefully handle failure 
            } 
    		break ;
    		
    	case IntentForResult.NEW_GAME_SETTINGS:
    		if ( resultCode == RESULT_OK ) {
    			if ( data != null && data.hasExtra(NewGameSettingsActivity.INTENT_RESULT_EXTRA_ACTION) ) {
    				int action = data.getIntExtra(NewGameSettingsActivity.INTENT_RESULT_EXTRA_ACTION, -1) ;
    				if ( action == NewGameSettingsActivity.INTENT_RESULT_EXTRA_ACTION_PLAY ) {
    					Integer gameMode = (Integer)data.getSerializableExtra(NewGameSettingsActivity.INTENT_EXTRA_ECHO) ;
    					String saveKey = GameSaver.freePlayGameModeToSaveKey(gameMode) ;
    					
    					// get settings
    					GameSettings gs ;
    					if ( data.hasExtra( NewGameSettingsActivity.INTENT_RESULT_EXTRA_GAME_SETTINGS ) )
    						gs = (GameSettings)data.getSerializableExtra( NewGameSettingsActivity.INTENT_RESULT_EXTRA_GAME_SETTINGS ) ;
    					else
    						gs = QuantroPreferences.getRememberCustomSetup(this)
    								? GameSettingsDatabaseAdapter.getMostRecentInDatabase(this, gameMode)
    								: new GameSettings(gameMode, 1).setImmutable() ;

    					startGameActivity( saveKey, gs, false ) ;
    				}
    			}
    		} else {
    			// gracefully handle failure
    		}
    		break ;
    		
    	case IntentForResult.LAUNCH_GAME:
    		Log.d(TAG, "result_launch_game") ;
    		if ( data != null && data.hasExtra(GameActivity.INTENT_EXTRA_GAME_RESULT) ) {
    			GameResult gr = (GameResult)data.getSerializableExtra(GameActivity.INTENT_EXTRA_GAME_RESULT) ;
    			
    			// We can safely assume a single player game.  If the game
    			// is over, put it in our Records database and delete
    			// the save.  Regardless, update the GameResults.
    			if ( gr != null ) {
    				int gameMode = gr.getGameInformation(0).mode ;
    				boolean hasSave = true ;
    				String key = GameSaver.freePlayGameModeToSaveKey(gameMode) ;
    				
    				if ( gr.getTerminated() ) {
	    				// update our database...
	    				GameStats.DatabaseAdapter.addGameResultToDatabase(this, gr) ;
	    				// Delete the saved game.
	    				GameSaver.deleteGame(this, key) ;
	    				
	    				if ( QuantroPreferences.getAnalyticsActive(this) )
            				doLogDelete( gameMode, gr, false ) ;
            			
	    				hasSave = false ;
    				} else {
    					hasSave = GameSaver.hasGameStates(this, key) ;
    				}
    				
    				
    				mFreePlayGameManagerView.setGameResult(gameMode, !hasSave ? null : gr) ;
    				mFreePlayGameManagerView.refreshView() ;
        			
    			}
    			
    			// hey, why not GC?
    			System.gc() ;
    		}
    		break ;
    		
    	case IntentForResult.CUSTOM_GAME_MODE:
    		// If canceled, we do nothing.
    		if ( resultCode == Activity.RESULT_OK  && data != null ) {
    			CustomGameModeSettings cgms = (CustomGameModeSettings) data.getSerializableExtra(CustomGameModeActivity.INTENT_EXTRA_CGMS) ;
    			if ( cgms != null ) {
    				CustomGameModeSettingsDatabaseAdapter da = new CustomGameModeSettingsDatabaseAdapter(this) ;
    				da.open() ;
    				
    				
    				
    				// try / finally so we always close this
    				try {
    					CustomGameModeSettings cgmsPrev = da.getCustomGameModeSettings(cgms.getID()) ;
        				int action = data.getIntExtra(CustomGameModeActivity.INTENT_RESULT_EXTRA_ACTION, -1 ) ;
        				
	    				if ( action == CustomGameModeActivity.INTENT_RESULT_EXTRA_ACTION_SAVE ) {
	    					Log.d(TAG, "saving custom game mode...") ;
	    					// If this is more than a title/desc update, delete saves and records FIRST.
	    					// Then commit the update.  Finally, force-refresh the SP view.
	    					if ( cgmsPrev != null && !cgmsPrev.isEquivalent(cgms) ) {
	    						int [] modes = GameModes.getCustomGameModes(cgms.getID()) ;
	    						for ( int i = 0; i < modes.length; i++ ) {
	    							int gm = modes[i] ;
	    							GameSaver.deleteGame(this, GameSaver.freePlayGameModeToSaveKey(gm)) ;
	    							GameSettingsDatabaseAdapter.deleteFromDatabase(this, gm) ;
	    							GameStats.DatabaseAdapter.deleteStatsAndSummaryFromDatabase(this, gm) ;
	    							mFreePlayGameManagerView.setGameResult(gm, null) ;
	    						}
	    					}
	    					
	    					// place in DB...
	    					da.putCustomGameModeSettings(cgms) ;
	    				}
	    				else if ( action == CustomGameModeActivity.INTENT_RESULT_EXTRA_ACTION_DELETE ) {
	    					Log.d(TAG, "deleting custom game mode...") ;
	    					// delete everything.
							int [] modes = GameModes.getCustomGameModes(cgms.getID()) ;
							for ( int i = 0; i < modes.length; i++ ) {
								int gm = modes[i] ;
								GameSaver.deleteGame(this, GameSaver.freePlayGameModeToSaveKey(gm)) ;
								GameSettingsDatabaseAdapter.deleteFromDatabase(this, gm) ;
								GameStats.DatabaseAdapter.deleteStatsAndSummaryFromDatabase(this, gm) ;
								mFreePlayGameManagerView.removeGame(gm) ;
							}
	    					
							// remove it from the DB...
	    					da.deleteCustomGameModeSettings(cgms) ;
	    				}
	    				
	    				// either way, refresh game modes and the single player game list.
	    				GameModes.setCustomGameModeSettings(da.getAllCustomGameModeSettings()) ;
    				} finally {
    					da.close() ;
    				}
    				
    				int [] modes = GameModes.getCustomGameModes(cgms.getID()) ;
    				for ( int i = 0; i < modes.length; i++ ) {
    					OptionAvailability availability = OptionAvailability.LOCKED_ENABLED ;
    					if ( getPremiumLibrary().hasTrialOnlyGameMode(modes[i]) )
    						availability = OptionAvailability.TIMED_ENABLED ;
    					else if ( getPremiumLibrary().hasGameMode(modes[i]) )
    						availability = OptionAvailability.ENABLED ;
    					mFreePlayGameManagerView.addGame(
    							modes[i],
    							availability,
    							GameSaver.loadGameResult(this, GameSaver.freePlayGameModeToSaveKey(modes[i]))) ;
    				}
    				mFreePlayGameManagerView.refreshView() ;
        			
    			}
    		}
    	}
    }
    
    private void doLogDelete( int gameMode, GameResult gr, boolean userAction ) {
    	//Log.d(TAG, "doLogDelete bracket IN") ;
    	if ( gr == null ) {
    		//Log.d(TAG, "doLogDelete bracket OUT") ;
    		return ;
    	}
    	
    	long timeSinceStarted = -1 ;
    	long timeSinceLast = -1 ;
    	long timeSpent = gr.getTimeInGame() ;
    	
    	Date dateStarted = gr.getDateStarted() ;
    	Date dateEnded = gr.getDateEnded() ;
    	if ( dateStarted != null )
    		timeSinceStarted = System.currentTimeMillis() - dateStarted.getTime() ;
    	if ( dateEnded != null )
    		timeSinceLast = System.currentTimeMillis() - dateEnded.getTime() ;
    	
    	Analytics.logDeleteGame(gameMode, timeSinceStarted, timeSinceLast, timeSpent, userAction) ;
    	//Log.d(TAG, "doLogDelete bracket OUT") ;
    }
	
	//
	// end activity result
	////////////////////////////////////////////////////////////////////////////
    
    
    

	@Override
	public void fpgmv_newGame(FreePlayGameManagerView view, int gameMode,
			boolean setup) {
		
		if ( view != mFreePlayGameManagerView )
			return ;
		
		if ( !setup ) {
			this.startGameActivity(gameMode,
					null,
					GameSaver.freePlayGameModeToSaveKey(gameMode),
					true) ;
		} else {
			this.startNewGameSetupActivity(gameMode) ;
		}
		
	}

	@Override
	public void fpgmv_loadGame(FreePlayGameManagerView view, int gameMode,
			boolean examine) {
		
		if ( view != mFreePlayGameManagerView )
			return ;
		
		if ( !examine ) {
			this.startGameActivity(gameMode,
					GameSaver.freePlayGameModeToSaveKey(gameMode),
					GameSaver.freePlayGameModeToSaveKey(gameMode),
					true) ;
		} else {
			this.startExamineGameResultActivity(
					gameMode,
					GameSaver.freePlayGameModeToSaveKey(gameMode)) ;
		}
		
	}
	
	
	@Override
	public void fpgmv_achievements( FreePlayGameManagerView view ) {
		QuantroApplication qa = (QuantroApplication)getApplication() ;
		if ( qa.gpg_isSignedIn() ) {
			startActivityForResult(qa.gpg_getAchievementsIntent(), IntentForResult.UNUSED);
		} else {
			expandStinger(true) ;
			qa.gpg_showAlert(getResources().getString(R.string.gamehelper_achievements_not_signed_in)) ;
		}
	}
	
	
	private int fpgmv_leaderboard_gamemode ;
	@Override
	public void fpgmv_leaderboard( FreePlayGameManagerView view, int gameMode ) {
		String leaderboardID = null ;
		if ( gameMode >= 0 ) {
			leaderboardID = Scores.getLeaderboardID(gameMode) ;
		}
		QuantroApplication qa = (QuantroApplication)getApplication() ;
		if ( qa.gpg_isSignedIn() ) {
			if ( leaderboardID == null ) {
				startActivityForResult(qa.gpg_getAllLeaderboardsIntent(), IntentForResult.UNUSED);
			} else {
				startActivityForResult(qa.gpg_getLeaderboardIntent(leaderboardID), IntentForResult.UNUSED);
			}
		} else {
			expandStinger(true) ;
			qa.gpg_showAlert(getResources().getString(R.string.gamehelper_leaderboards_not_signed_in)) ;
		}
		
		/*
		if ( gameMode < 0 ) {
			Scores.askUserToAcceptTermsOfService(this, new Scores.Continuation<Boolean>() {
				@Override
				public void withValue(Boolean value, Exception error) {
					if ( value != null && value ) {
						Scores.submitLocalScores( null ) ;
						Scores.startLeaderboardActivity(FreePlayGameManagerActivity.this) ;
					}
				}
			}) ;
		} else {
			fpgmv_leaderboard_gamemode = gameMode ;
			Scores.askUserToAcceptTermsOfService(this, new Scores.Continuation<Boolean>() {
				@Override
				public void withValue(Boolean value, Exception error) {
					if ( value != null && value ) {
						Scores.submitLocalScores( null ) ;
						Scores.startLeaderboardActivity(FreePlayGameManagerActivity.this, fpgmv_leaderboard_gamemode) ;
					}
				}
			}) ;
		} */
	}
	

	@Override
	public void fpgmv_createGame(FreePlayGameManagerView view) {
		if ( view != mFreePlayGameManagerView )
			return ;
		
		if ( CustomGameModeSettingsDatabaseAdapter.count(this) >= getResources().getInteger(R.integer.custom_game_mode_max_saved) )
			mDialogManager.showDialog(DIALOG_ID_TOO_MANY_CUSTOM_GAME_MODES) ;
		else
			startCustomGameModeActivity(null) ;
	}

	@Override
	public void fpgmv_editGame(FreePlayGameManagerView view, int gameMode) {
		if ( view != mFreePlayGameManagerView )
			return ;
		
		startCustomGameModeActivity(gameMode) ;
	}
	
	@Override
	public void fpgmv_help( FreePlayGameManagerView view ) {
		mDialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
	}
	
	@Override
	public void fpgmv_openSettings( FreePlayGameManagerView view ) {
		// open settings!
		startQuantroPreferencesActivity() ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// GlobalDialog.DialogContext
	// 

	@Override
	public Context getContext() {
		return this ;
	}

	@Override
	public String getHelpDialogHTMLRelativePath() {
		return "help/free_play_activity.html" ;
	}
	
	@Override
	public String getHelpDialogContextName() {
		return getResources().getString(R.string.global_dialog_help_name_free_play_game_manager) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
}
