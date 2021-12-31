package com.peaceray.quantro;

import java.util.Collection;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.peaceray.quantro.adapter.controls.ControlsToActionsAdapter;
import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.content.Background;
import com.peaceray.quantro.content.Music;
import com.peaceray.quantro.content.Skin;
import com.peaceray.quantro.main.GameOptionsMenuFragment;
import com.peaceray.quantro.view.colors.ColorScheme;


/**
 * An extremely simple wrapper for displaying the 'game options' menu as its
 * own activity, for some breathing room.  This kind of display lets the GameActivity
 * surrender its block drawer objects and frees up a little memory for processing.
 * 
 * The GameOptions Fragment handles almost all of its own processing.  We don't actually
 * have much to do -- most of our delegate methods are irrelevant, because the GameActivity
 * will automatically respond to these kinds of things.
 * 
 * The GameActivity reloads mBackground and mSkin from current preferences in onStart.
 * In onResume we configure the Controls.
 * 
 * The only information that needs to be directly transferred is the current music
 * track.
 * 
 * @author Jake
 *
 */
public class GameOptionsMenuActivity extends QuantroActivity implements GameOptionsMenuFragment.Listener {

	private static final String TAG = "GameOptionsMenuActivity" ;
	
	public static final String INTENT_EXTRA_GAME_MODE = "GameOptionsMenuActivity.INTENT_EXTRA_GAME_MODE" ;
	public static final String INTENT_EXTRA_CURRENT_MUSIC = "GameOptionsMenuActivity.INTENT_EXTRA_CURRENT_MUSIC" ;
	
	
	public static final int RESULT_RESUME = 0 ;
	public static final int RESULT_QUIT = 1 ;
	
	private int mGameMode ;
	
	private Music mMusic ;
	
	private GameOptionsMenuFragment mGameOptionsMenuFragment ;
	
	private boolean mAcquirePremiumForSet = false ;
	private boolean mAcquirePremiumForShuffle = false ;
	
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		setupQuantroActivity( QUANTRO_ACTIVITY_GAME, QUANTRO_ACTIVITY_CONTENT_FULL ) ;
		
        // Force portrait layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) ;
        
        System.gc() ;
        
        // LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
		setContentView(R.layout.game_options_menu_activity);
        
        // Game mode
        mGameMode = getIntent().getIntExtra(INTENT_EXTRA_GAME_MODE, 0) ;
        
        // music?
        mMusic = Music.fromStringEncoding( getIntent().getStringExtra(INTENT_EXTRA_CURRENT_MUSIC) ) ;
        if ( mGameOptionsMenuFragment != null )
    		mGameOptionsMenuFragment.setCurrentMusic(mMusic) ;
        
        if ( savedInstanceState != null ) {
        	readStateFromBundle( savedInstanceState ) ;
        }
    }
    
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
    	//Log.d(TAG, "onKeyDown") ;
    	
    	
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	((QuantroApplication)getApplication()).getSoundPool(this).menuButtonBack() ;
        	// If the options menu is shown, tell it what happened.
        	// Otherwise, pause and put up the menu.
        	mGameOptionsMenuFragment.backButtonPressed() ;
        	return true ;
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
    }
    
    
    protected void readStateFromBundle( Bundle inState ) {
    	mMusic = Music.fromStringEncoding( inState.getString("GameOptionsMenuActivity.mMusic") ) ;
    }
    
    
    protected void writeStateToBundle( Bundle outState ) {
    	outState.putString("GameOptionsMenuActivity.mMusic", Music.toStringEncoding(mMusic)) ;
    }
    
    
    private void resumeGame() {
    	Intent data = new Intent() ;
    	data.putExtra(INTENT_EXTRA_CURRENT_MUSIC, Music.toStringEncoding(mMusic)) ;
    	
    	this.setResult(RESULT_RESUME, data) ;
    	finish() ;
    }
    
    private void quitGame() {
    	Intent data = new Intent() ;
    	data.putExtra(INTENT_EXTRA_CURRENT_MUSIC, Music.toStringEncoding(mMusic)) ;
    	
    	this.setResult(RESULT_QUIT, data) ;
    	finish(); 
    }
    
    
    
    
    ////////////////////////////////////////////////////////////////////////////
	//
	// GAME OPTIONS MENU FRAGMENT
	//

	@Override
	public void gomfl_onAttach(GameOptionsMenuFragment fragment) {
		mGameOptionsMenuFragment = fragment ;
		
		// necessary updates
		mGameOptionsMenuFragment.setGameMode(mGameMode) ;
		mGameOptionsMenuFragment.setCurrentBackground( QuantroPreferences.getBackgroundCurrent(this) ) ;
		mGameOptionsMenuFragment.setCurrentMusic(mMusic) ;
		mGameOptionsMenuFragment.setControlsThumbnail(null) ;
		
		mGameOptionsMenuFragment.show() ;
	}


	@Override
	public void gomfl_setCurrentSkinAndColors(Skin skin, ColorScheme colorScheme) {
		
	}


	@Override
	public void gomfl_setCurrentBackground(Background background) {
		
	}
	
	@Override
	public void gomfl_setBackgroundsInShuffle( Collection<Background> backgrounds ) {
		
	}
	
	@Override
	public void gomfl_setBackgroundShuffles( boolean shuffles ) {
		
	}
	
	@Override
	public void gomfl_setCurrentMusic(Music music) {
		// Set the current music track.
		// First check that we own this music.
		boolean owned = getPremiumLibrary().has(music) ;
		if ( owned && !Music.equals(music, mMusic) ) {
			mMusic = music ;
		}
	}
	
	@Override
	public void gomfl_setCurrentControlsGamepad() {
		
	}
	
	@Override
	public void gomfl_setCurrentControlsGesture() {
		
	}
	
	@Override
	public void gomfl_optionsMenuFragmentDismissed() {
		resumeGame() ;
	}


	@Override
	public void gomfl_quit() {
		// Dismiss the menu and quit.
		quitGame() ;
	}
	
	
}
