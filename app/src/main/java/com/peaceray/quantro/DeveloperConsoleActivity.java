package com.peaceray.quantro;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.peaceray.quantro.R;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.database.GameStats;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.keys.KeyStorage;
import com.peaceray.quantro.keys.QuantroXLKey;
import com.peaceray.quantro.model.GameSaver;
import com.peaceray.quantro.model.game.GameBlocksSliceSequence;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.utils.AssetAccessor;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.dialog.TimePickerDialog;
import com.peaceray.quantro.view.dialog.ProgressDialog;
import com.peaceray.quantro.view.dialog.WebViewDialog;

import android.app.Dialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

public class DeveloperConsoleActivity extends QuantroActivity implements OnTimeSetListener, OnClickListener  {

	private static final String TAG = "DeveloperConsoleActivity" ;
	
	private static final int DIALOG_PROGRESS = 0 ;
	private static final int DIALOG_ALERT = 1 ;
	private static final int DIALOG_DURATION_PICKER = 2 ;
	private static final int DIALOG_WHATS_NEW = 3 ;
	
	
	
	private RefreshHandler refreshHandler = new RefreshHandler();
	private DialogManager mDialogManager ;
	
	
	// Slice picker (choosing between slice styles).
	private static final String SLICE_Q = "Q" ;
	private static final String SLICE_Q_STACK = "Q Stack" ;
	private static final String SLICE_ACHV_SPECIAL = "Achv. Special" ;
	private static final String SLICE_ACHV_SPECIAL_FAIL = "Achv. Special Fail" ;
	private static final String SLICE_ACHV_SWISS_CHEESE_CLEARS = "Achv. Cheese Clears" ;
	private static final String SLICE_ACHV_SHORT_CLEARS = "Achv. Short Clears" ;
	
	private static final ArrayList<String> SLICE_STYLES = new ArrayList<String>() ;
	static {
		SLICE_STYLES.add(SLICE_Q) ;
		SLICE_STYLES.add(SLICE_Q_STACK) ;
		SLICE_STYLES.add(SLICE_ACHV_SPECIAL) ;
		SLICE_STYLES.add(SLICE_ACHV_SPECIAL_FAIL) ;
		SLICE_STYLES.add(SLICE_ACHV_SWISS_CHEESE_CLEARS) ;
		SLICE_STYLES.add(SLICE_ACHV_SHORT_CLEARS) ;
	}
	

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            DeveloperConsoleActivity.this.update() ;
        }

        public void sleep(long delayMillis) {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
        
        public void clear() {
        	this.removeMessages(0) ;
        }
    };
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupQuantroActivity( QUANTRO_ACTIVITY_MENU, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
        
        setContentView(R.layout.developer_console_layout) ;
        
        View v ;
        
        // LEFT SIDE 
        
        // Main menu activity launcher
        View mainMenuButton = findViewById(R.id.main_menu_hack_main_menu) ;
        mainMenuButton.setOnClickListener(this) ;
        
    	// SP FreePlay activity launcher
        View freePlayButton = findViewById(R.id.main_menu_hack_freeplay) ;
        freePlayButton.setOnClickListener(this) ;
        
        // Settings
        View settingsButton = findViewById(R.id.main_menu_hack_settings_button) ;
        settingsButton.setOnClickListener(this) ;
        
        // GameStats
        View statsButton = findViewById(R.id.main_menu_hack_log_stats_button) ;
        statsButton.setOnClickListener(this) ;
        
        // Show Logo
        View showLogoButton = findViewById(R.id.main_menu_hack_show_logo) ;
        showLogoButton.setOnClickListener(this) ;
        // associated spinner (skin)
        Spinner showLogoSkinSpinner = (Spinner)findViewById(R.id.main_menu_hack_show_logo_skin_spinner) ;
        List<String> spinnerArray =  new ArrayList<String>();
        Skin [] skins = Skin.getAllSkins() ;
        for ( int i = 0; i < skins.length; i++ )
        	spinnerArray.add( skins[i].getName() ) ;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        showLogoSkinSpinner.setAdapter(adapter);
        // associated spinner (style)
        Spinner showLogoStyleSpinner = (Spinner)findViewById(R.id.main_menu_hack_show_logo_style_spinner) ;
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, SLICE_STYLES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        showLogoStyleSpinner.setAdapter(adapter);
        
        // Delete saves (does this even do anything anymore?)
        View deleteSavesButton = findViewById(R.id.main_menu_hack_delete_saves) ;
        deleteSavesButton.setOnClickListener(this) ;
        
        
        // RIGHT SIDE
        
        // Local Retro test
        View retroServerTestButton = findViewById(R.id.main_menu_hack_retro_wifi_local_server_test_button) ;
        retroServerTestButton.setOnClickListener(this) ;
        
        
        // Dialogs!
        // progress
        v = findViewById(R.id.main_menu_hack_show_progress_dialog_button) ;
        v.setOnClickListener(this) ;
        // alert
        v = findViewById(R.id.main_menu_hack_show_alert_dialog_button) ;
        v.setOnClickListener(this) ;
        // duration_picker
        v = findViewById(R.id.main_menu_hack_show_duration_picker_dialog_button) ;
        v.setOnClickListener(this) ;
        v = findViewById(R.id.main_menu_hack_show_webview_dialog_button) ;
        v.setOnClickListener(this) ;
        
        refreshButtons() ;
        
        
        // DO NOT REGISTER.  We don't currently use C2DM, so there's no need to register.
        // C2DMReceiver.refreshAppC2DMRegistrationState(this) ;
        
        mDialogManager = new DialogManager(this) ;
        
        Log.d(TAG, "onCreate") ;
        
        ////////////////////////////////////////////////////////////////////////
		// DISABLE ANDROID BEAM
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		if ( nfcAdapter != null ) {
			nfcAdapter.setNdefPushMessage(null, this) ;
		}
    }
    
    
    @Override
    protected Dialog onCreateDialog( int dialog ) {


    	switch( dialog ) {
    	case DIALOG_PROGRESS:
    		ProgressDialog pd = new ProgressDialog(this) ;
    		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER) ;
    		//pd.setTitle("Progress") ;
    		pd.setMessage("") ;
    		pd.setCancelable(true) ;
    		pd.setOnCancelListener( new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface arg0) {
					mDialogManager.dismissDialog(DIALOG_PROGRESS) ;
				}
    		}) ;
    		return pd ;

    		
    	case DIALOG_ALERT:
    		AlertDialog.Builder builder = new AlertDialog.Builder(this) ;
    		builder.setTitle("Alert") ;
    		builder.setMessage("") ;
    		builder.setCancelable(true) ;
    		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Toast.makeText(DeveloperConsoleActivity.this, "Yes", Toast.LENGTH_SHORT).show() ;
					mDialogManager.dismissDialog(DIALOG_ALERT) ;
				}
    		}) ;
    		builder.setNeutralButton("Maybe", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Toast.makeText(DeveloperConsoleActivity.this, "Maybe", Toast.LENGTH_SHORT).show() ;
					mDialogManager.dismissDialog(DIALOG_ALERT) ;
				}
    		}) ;
    		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Toast.makeText(DeveloperConsoleActivity.this, "No", Toast.LENGTH_SHORT).show() ;
					mDialogManager.dismissDialog(DIALOG_ALERT) ;
				}
    		}) ;
    		builder.setOnCancelListener( new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mDialogManager.dismissDialog(DIALOG_ALERT) ;
				}
    		}) ;
    		return builder.create() ;
    		
    	
    	case DIALOG_DURATION_PICKER:
    		TimePickerDialog tpd = new TimePickerDialog(this, null, 4, 0, true) ;
        	// for "set", we will manually call "dismissDialog" in the callback method here.
        	tpd.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int which) {
        	    	mDialogManager.dismissDialog(DIALOG_DURATION_PICKER) ;
        	    }
        	});
        	tpd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_DURATION_PICKER) ;
				}
			}) ;
        	tpd.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					mDialogManager.forgetDialog(DIALOG_DURATION_PICKER) ;
				}
			}) ;
        	return tpd ;
        	
    	case DIALOG_WHATS_NEW:
    		WebViewDialog.Builder wvbuilder = new WebViewDialog.Builder(this) ;
    		wvbuilder.setTitle("What's New?") ;
    		String url = "file:///android_asset/html/whats_new/"
    				+ "version_" + AppVersion.code(this) + "_" + AppVersion.name(this)
    				+ ".html" ;
    		wvbuilder.setURL(url) ;
    		wvbuilder.setCancelable(true) ;
    		wvbuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
        	    public void onClick(DialogInterface dialog, int which) {
        	    	mDialogManager.dismissDialog(DIALOG_WHATS_NEW) ;
        	    }
        	}) ;
    		wvbuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel( DialogInterface dialog ) {
					mDialogManager.dismissDialog(DIALOG_WHATS_NEW) ;
				}
			}) ;
    		return wvbuilder.create() ;
    	}
    	
    	return null ;
    }
    
    
    @Override
    protected void onPrepareDialog( int id, Dialog dialog ) {
    	Random r = new Random() ;
    	switch( id ) {
    	case DIALOG_PROGRESS:
    		ProgressDialog pd = (ProgressDialog)dialog ;
    		pd.setMessage("Randomized message number is " + r.nextInt()) ;
    		break ;
    		
    	case DIALOG_ALERT:
    		AlertDialog ad = (AlertDialog)dialog ;
    		Button b ;
    		int [] buttons = new int[]{ AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEUTRAL, AlertDialog.BUTTON_NEGATIVE } ;
    		for ( int i = 0; i < buttons.length; i++ ) {
    			ad.setButtonEnabled(buttons[i], false) ;
    			ad.setButtonVisible(buttons[i], false) ;
    		}
    		
    		StringBuilder sb = new StringBuilder() ;
    		int buttonCase = r.nextInt(7) ;
    		sb.append("Visible case " + buttonCase + ": ") ;
    		switch( buttonCase ) {
    		case 0:		// yes, maybe, no
    			ad.setButtonVisible(buttons[0], true) ;
    			sb.append("yes, ") ;
    		case 1:		// maybe, no
    			ad.setButtonVisible(buttons[1], true) ;
    			sb.append("maybe, ") ;
    		case 2:		// no
    			ad.setButtonVisible(buttons[2], true) ;
    			sb.append("no") ;
    			break ;
    			
    		case 3:		// yes, maybe
    			ad.setButtonVisible(buttons[0], true) ;
    			sb.append("yes, ") ;
    		case 4:		// maybe
    			ad.setButtonVisible(buttons[1], true) ;
    			sb.append("maybe") ;
    			break ;
    			
    		case 5: 	// yes, no
    			ad.setButtonVisible(buttons[0], true) ;
    			ad.setButtonVisible(buttons[2], true) ;
    			sb.append("yes, no") ;
    			break ;
    			
    		case 6: 	// yes
    			ad.setButtonVisible(buttons[0], true) ;
    			sb.append("yes") ;
    			break ;
    		}
    		
    		sb.append("\n") ;
    		
    		buttonCase = r.nextInt(7) ;
    		sb.append("Enabled case " + buttonCase + ": ") ;
    		switch( buttonCase ) {
    		case 0:		// yes, maybe, no
    			ad.setButtonEnabled(buttons[0], true) ;
    			sb.append("yes, ") ;
    		case 1:		// maybe, no
    			ad.setButtonEnabled(buttons[1], true) ;
    			sb.append("maybe, ") ;
    		case 2:		// no
    			ad.setButtonEnabled(buttons[2], true) ;
    			sb.append("no") ;
    			break ;
    			
    		case 3:		// yes, maybe
    			ad.setButtonEnabled(buttons[0], true) ;
    			sb.append("yes, ") ;
    		case 4:		// maybe
    			ad.setButtonEnabled(buttons[1], true) ;
    			sb.append("maybe") ;
    			break ;
    			
    		case 5: 	// yes, no
    			ad.setButtonEnabled(buttons[0], true) ;
    			ad.setButtonEnabled(buttons[2], true) ;
    			sb.append("yes, no") ;
    			break ;
    			
    		case 6: 	// yes
    			ad.setButtonEnabled(buttons[0], true) ;
    			sb.append("yes") ;
    			break ;
    		}
    		
    		sb.append("\n") ;
    		
    		if ( r.nextInt(3) == 0 ) {
    			for ( int i = 0; i < 50; i++ )
    				sb.append("long message").append("\n") ;
    		} else
    			sb.append("short message") ;
    		
    		ad.setMessage(sb.toString()) ;
    		
    		break ;
    		
    	case DIALOG_DURATION_PICKER:
    		TimePickerDialog tpd = (TimePickerDialog)dialog ;
    		tpd.setLabel("Random label " + r.nextInt(10) + " ") ;
    		break ;
    	}
    }
    
    /**
     * onClick
     * Upon a click of a view...
     */
    public void onClick(View v) {
    	switch( v.getId() ) {

    	case R.id.main_menu_hack_main_menu:
    		startMainMenu() ;
    		break ;
    		
    	case R.id.main_menu_hack_freeplay:
    		startFreePlay() ;
    		break ;
    		
    	case R.id.main_menu_hack_settings_button:
    		startSettings() ;
    		break ;
    		
    	case R.id.main_menu_hack_log_stats_button:
    		Iterator<Integer> iter = GameModes.iteratorIncluded() ;
    		for ( ; iter.hasNext() ; )
    			logStats(iter.next()) ;
    		break ;
    		
    	case R.id.main_menu_hack_show_logo:
    		showLogo() ;
    		break ;
    		
    	case R.id.main_menu_hack_delete_saves:
    		boolean success1 = GameSaver.deleteGame(this, "quantro_hack") ;
    		boolean success2 = GameSaver.deleteGame(this, "retro_hack") ;
    		refreshButtons() ;
    		break ;
    	
    	case R.id.main_menu_hack_retro_wifi_local_server_test_button:
    		startGame( GameModes.GAME_MODE_SP_RETRO_A, null, null ) ;
    		break ;
    	
    	case R.id.main_menu_hack_show_progress_dialog_button:
    		mDialogManager.showDialog(DIALOG_PROGRESS) ;
    		break ;
    		
    	case R.id.main_menu_hack_show_alert_dialog_button:
    		mDialogManager.showDialog(DIALOG_ALERT) ;
    		break ;
    		
    	case R.id.main_menu_hack_show_duration_picker_dialog_button:
    		mDialogManager.showDialog(DIALOG_DURATION_PICKER) ;
    		break ;
    		
    	case R.id.main_menu_hack_show_webview_dialog_button:
    		mDialogManager.showDialog(DIALOG_WHATS_NEW) ;
    		break ;
    	}
    }
    
    
    private void showLogo() {
    	// TODO: load the current Skin setting from the spinner, determine
    	// its associated game, and load the appropriate Q.
    	
    	int skinIndex = ((Spinner)findViewById(R.id.main_menu_hack_show_logo_skin_spinner)).getSelectedItemPosition() ;
    	Skin skin = Skin.getAllSkins()[skinIndex] ;
    	
    	int styleIndex = ((Spinner)findViewById(R.id.main_menu_hack_show_logo_style_spinner)).getSelectedItemPosition() ;
    	String slice = SLICE_STYLES.get(styleIndex) ;
    	
    	GameBlocksSliceSequence gbss = null ;
    	if ( skin.getGame() == Skin.Game.QUANTRO ) {
    		if ( SLICE_Q.equals(slice) )	// standard logo
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/q/q_quantro.txt") ) ;
    		else if ( SLICE_Q_STACK.equals(slice) )	 // logo on a stack
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/q/q_quantro_stack.txt") ) ;
    		else if ( SLICE_ACHV_SPECIAL.equals(slice) )
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/achievement/specials.txt") ) ;
    		else if ( SLICE_ACHV_SPECIAL_FAIL.equals(slice) )
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/achievement/specials_fail.txt") ) ;
    		else if ( SLICE_ACHV_SWISS_CHEESE_CLEARS.equals(slice) )
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/achievement/swiss_cheese_clears_quantro.txt") ) ;
    	} else {
    		if ( SLICE_Q.equals(slice) )	// standard logo
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/q/q_retro.txt") ) ;
    		else if ( SLICE_Q_STACK.equals(slice) )	 // logo on a stack
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/q/q_retro_stack.txt") ) ;
    		else if ( SLICE_ACHV_SWISS_CHEESE_CLEARS.equals(slice) )
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/achievement/swiss_cheese_clears_retro.txt") ) ;
    		else if ( SLICE_ACHV_SHORT_CLEARS.equals(slice) )
    			gbss = new GameBlocksSliceSequence( AssetAccessor.assetToString(this, "sequence/achievement/swiss_cheese_clears_short_retro.txt") ) ;
    	}
    	
    	if ( gbss != null ) {
	    	Intent intent = new Intent( this, SliceDrawerActivity.class ) ;
	    	intent.setAction( Intent.ACTION_MAIN ) ;
	    	
	    	intent.putExtra( SliceDrawerActivity.INTENT_EXTRA_SEQUENCE_BYTES, gbss.getBytes() ) ;
	    	intent.putExtra( SliceDrawerActivity.INTENT_EXTRA_SKIN_STRING_ENCODING, Skin.toStringEncoding(skin) ) ;
	    	intent.putExtra( SliceDrawerActivity.INTENT_EXTRA_SLICE_NAME, slice ) ;
	    
	    	startActivity(intent) ;
    	} else {
    		Toast.makeText(this, "That skin / slice combination is invalid.  Try switching between Retro / Quantro skins.", Toast.LENGTH_SHORT).show() ;
    	}
    }
    
    
    private void startGame( int mode, String loadFromKey, String saveToKey ) {
    	startGame( mode, loadFromKey, saveToKey, true ) ;
    }

    private void startGame( int mode, String loadFromKey, String saveToKey, boolean asServer ) {
    	Log.d(TAG, "Intent for game mode " + mode + ", loading from " + loadFromKey + ", saving to " + saveToKey) ;
    	
    	Resources res = getResources() ;
    	
    	Intent intent = new Intent( this, GameActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	GameIntentPackage gaip = new GameIntentPackage( new GameSettings(mode, GameModes.minPlayers(mode)).setImmutable() ) ;
    	if ( saveToKey != null )
    		gaip.setConnectionAsLocal(loadFromKey, saveToKey) ;
    	else if ( asServer ){
    		SocketAddress myAddr = new InetSocketAddress( "localhost", res.getInteger(R.integer.wifi_multiplayer_game_port) ) ;
    		gaip.setConnectionAsDirectServer( new Nonce(), new Nonce(), "player",
    				myAddr, null, null) ;
    	}
    	
    	intent.putExtra( GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE, gaip) ;
    	
    	startActivity(intent) ;
    }
    
    
    private void startMainMenu() {
    	// Launch!
    	Intent intent = new Intent( this, MainMenuActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	startActivity(intent) ;
    }
    
    private void startFreePlay() {
    	// Launch!
    	Intent intent = new Intent( this, FreePlayGameManagerActivity.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	startActivity(intent) ;
    }
    
    
    
    private void startSettings() {
    	// Launch!
    	Intent intent = new Intent( this, QuantroPreferences.class ) ;
    	intent.setAction( Intent.ACTION_MAIN ) ;
    	
    	startActivity(intent) ;
    }
    

	
	/**
	 * Prints to Log.d the Summary and various Record lists for the specified
	 * game mode.
	 * 
	 * @param gameMode
	 */
	private void logStats( int gameMode ) {
		// update our database...
		GameStats.DatabaseAdapter gsda = new GameStats.DatabaseAdapter(this) ;
		gsda.open() ;
		
		Log.d(TAG, "logStats for gameMode " + gameMode) ;
		
		// summary?
		GameStats.Summary s = gsda.getSummary(gameMode) ;
		Log.d(TAG, s != null ? s.toString() : "Summary is null") ;
			
		for ( int i = 0; i < GameStats.Record.NUM_VALUES; i++ ) {
			switch( i ) {
			case GameStats.Record.VALUE_SCORE:
				Log.d(TAG, "Best 10 records by SCORE:") ;
				break ;
			case GameStats.Record.VALUE_MAX_MULTIPLIER:
				Log.d(TAG, "Best 10 records by HIGHEST MULTIPLIER:") ;
				break ;
			case GameStats.Record.VALUE_TOTAL_CLEARS:
				Log.d(TAG, "Best 10 records by CLEARS:") ;
				break ;
			case GameStats.Record.VALUE_BEST_CASCADE:
				Log.d(TAG, "Best 10 records by LONGEST CASCADE:") ;
				break ;
			case GameStats.Record.VALUE_LEVEL:
				Log.d(TAG, "Best 10 records by LEVEL:") ;
				break ;
			case GameStats.Record.VALUE_LEVEL_UPS:
				Log.d(TAG, "Best 10 records by LEVEL UPS:") ;
				break ;
			case GameStats.Record.VALUE_GAME_LENGTH:
				Log.d(TAG, "Best 10 records by LENGTH") ;
				break ;
			case GameStats.Record.VALUE_TIME_ENDED:
				Log.d(TAG, "Best 10 records by RECENCY:") ;
				break ;
			}
			
			GameStats.Record [] records = gsda.getBestRecordsForValueCode(10, gameMode, i) ;
			
			for ( int j = 0; j < records.length; j++ )
				Log.d(TAG, "" + records[j]) ;
		}
			
		gsda.close() ;
	}
    
    private void logXLKey() {
    	QuantroXLKey key = KeyStorage.getXLKey(this) ;
    	
    	Log.d(TAG, "QuantroXLKey: " + key) ;
    	if ( key != null ) {
    		Log.d(TAG, "JSON          : " + key.getJSON()) ;
    		Log.d(TAG, "JSONSignature : " + key.getJSONSignature()) ;
    		Log.d(TAG, "KeyValue      : " + key.getKey()) ;
    		Log.d(TAG, "KeyTag        : " + key.getKeyTag()) ;
    		Log.d(TAG, "KeySignature  : " + key.getKeySignature()) ;
    		Log.d(TAG, "isValid       : " + key.isValid()) ;
    		Log.d(TAG, "isInvalid     : " + key.isInvalid()) ;
    		Log.d(TAG, "isActivated   : " + key.isActivated()) ;
    		Log.d(TAG, "isNotActivated: " + key.isNotActivated()) ;
    		
    		// Now try re-checking validity...
    		Log.d(TAG, "online validity check...") ;
    		QuantroXLKey.Updater updater = key.newUpdater() ;
    		if ( updater.updateValidity(2000) ) {
    			key = (QuantroXLKey) updater.getKey() ;
        		Log.d(TAG, "isValid       : " + key.isValid()) ;
        		Log.d(TAG, "isInvalid     : " + key.isInvalid()) ;
    		} else {
    			Log.d(TAG, "failed, error code " + updater.getLastError()) ;
    		}
    	}
    }
    
    
    
    private void forgetXLKey() {
    	// Overwrite the current key with a blank one.
    	KeyStorage.forgetXLKey(this) ;
    	
    	this.refreshButtons() ;
    }
    
    
    
    private void refreshButtons() {
    	
    	boolean retroResume = GameSaver.hasGameStates(this, "retro_hack") ;
    	boolean quantroResume = GameSaver.hasGameStates(this, "quantro_hack") ;
    	
    	View deleteSavesButton = findViewById(R.id.main_menu_hack_delete_saves) ;
    	deleteSavesButton.setEnabled( retroResume || quantroResume ) ;
    }
    
    private void update() {
    	this.refreshButtons() ;
    	refreshHandler.sleep(5000) ;
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	// Check for saved games; disable buttons.
    	refreshButtons() ;
    	
    	// Start a finder
    	//finder.start();
    	// HACK HACK HACK
    	refreshHandler.sleep(5000) ;
    	
    	// invalidate content view
    	View v = this.findViewById(R.layout.developer_console_layout) ;
    	if ( v != null )
    		v.invalidate() ;
    	
    	// Log some dev content
    	/*
    	Log.d(TAG, "BOARD " + android.os.Build.BOARD) ;
    	Log.d(TAG, "BRAND " + android.os.Build.BRAND) ;
    	Log.d(TAG, "DEVICE " + android.os.Build.DEVICE) ;
    	Log.d(TAG, "DISPLAY " + android.os.Build.DISPLAY) ;
    	Log.d(TAG, "MANUFACTURER " + android.os.Build.MANUFACTURER) ;
    	Log.d(TAG, "MODEL " + android.os.Build.MODEL) ;
    	Log.d(TAG, "PRODUCT " + android.os.Build.PRODUCT) ;
    	*/
    	
    	mDialogManager.revealDialogs() ;
    }
    
    @Override
    protected void onPause() {
    	super.onPause() ;
    	
    	// Stop the finder
    	// finder.stop();
    	refreshHandler.clear() ;
    	
    	mDialogManager.hideDialogs() ;
    }
    

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		Toast.makeText(this, "onTimeSet: " + hourOfDay + ":" + minute, Toast.LENGTH_SHORT).show() ;
	}
}

