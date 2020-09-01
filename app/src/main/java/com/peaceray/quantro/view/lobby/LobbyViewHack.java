package com.peaceray.quantro.view.lobby;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.peaceray.quantro.R;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.LobbyLog;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.view.button.strip.MultiPlayerGameLaunchButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.options.OptionAvailability;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LobbyViewHack extends RelativeLayout implements MultiPlayerGameLaunchButtonStrip.Delegate, LobbyViewAdapter {

	public static final String TAG = "LobbyViewHack" ;
	
	Lobby lobby ;
	LobbyLog lobbyLog ;
	WeakReference<Delegate> mwrDelegate ;
	
	boolean votedForR = false ;
	boolean votedForQ = false ;
	
	MultiPlayerGameLaunchButtonStrip retroButtonStrip ;
	MultiPlayerGameLaunchButtonStrip quantroButtonStrip ;
	
	private RefreshHandler refreshHandler = new RefreshHandler();
	
	int lastDisplayedLogId = -1 ;		// the last LobbyLog we've displayed.
	ArrayList<LobbyLog.Event> lobbyLogEvents ;

    class RefreshHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            LobbyViewHack.this.updateVisuals() ;
        }

        public void sleep(long delayMillis) {
        	this.removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMillis);
        }
    };
	
	// Inflate from XML
	public LobbyViewHack( Context context, AttributeSet attrs ) {
		super(context, attrs) ;
		lobbyLogEvents = new ArrayList<LobbyLog.Event>() ;
		
		// set up onclick listeners
		// setOnClickListeners() ;
	}

	public LobbyViewHack( Context context, AttributeSet attrs, int defStyle ) {
		super(context, attrs, defStyle) ;
		lobbyLogEvents = new ArrayList<LobbyLog.Event>() ;
		
		// set up onclick listeners
		// setOnClickListeners() ;
	}
	
	public void setup() {
		retroButtonStrip = (MultiPlayerGameLaunchButtonStrip)findViewById(R.id.lobby_view_hack_retro_button_strip) ;
		quantroButtonStrip = (MultiPlayerGameLaunchButtonStrip)findViewById(R.id.lobby_view_hack_quantro_button_strip) ;
		
		retroButtonStrip.setPlayerColor(  0, 0xff00ffff) ;
		quantroButtonStrip.setPlayerColor(0, 0xff00ffff) ;
		retroButtonStrip.setPlayerColor(  1, 0xffff00ff) ;
		quantroButtonStrip.setPlayerColor(1, 0xffff00ff) ;
		
		
		setOnClickListeners() ;
		update_lobby() ;
	}
	
	
	public void updateButtonText() {
		retroButtonStrip.refresh() ;
		quantroButtonStrip.refresh() ;
	}
	
	public void updateText() {
		String serverName = lobby.getLobbyName() ;
		String [] connectedPlayerNames = lobby.getPlayerNamesInLobby() ;
		
		String text = serverName + "'s lobby\nConnected: " ;
		for ( int i = 0; i < connectedPlayerNames.length; i++ )
			text = text + connectedPlayerNames[i] + " " ;
		
		TextView tv = (TextView) findViewById(R.id.lobby_view_hack_text_view) ;
		tv.setText(text) ;
	}
	
	public void updateVisuals() {
		updateButtonText() ;
		updateText() ;
		//refreshHandler.sleep(10) ;
	}
	
	private void setOnClickListeners() {
		retroButtonStrip.setDelegate(this) ;
		quantroButtonStrip.setDelegate(this) ;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// CONNECTIONS
	//
	// LobbyView needs a controller, and it needs read-access to LobbyClient
	// (although it will probably not make changes to it; that's the
	// Controller's job).
	//
	///////////////////////////////////////////////////////////////////////////	
	
	/**
	 * Sets the controller for this view.
	 */
	@Override
	public void setDelegate( LobbyViewAdapter.Delegate delegate ) {
		this.mwrDelegate = new WeakReference<Delegate> (delegate) ;
	}
	
	/**
	 * Sets the "model" for this view; the LobbyClient object.  This is intended
	 * to be a one-way information stream, with the LobbyView displaying what's
	 * in the LobbyClient, but using the Controller to actually make updates.
	 * @param lc
	 */
	@Override
	public void setLobby( Lobby lobby ) {
		this.lobby = lobby ;
		retroButtonStrip.setLobby(lobby) ;
		quantroButtonStrip.setLobby(lobby) ;
	}
	
	/**
	 * Sets the other half of the "model" for this view: the LobbyLog.  This, like
	 * Lobby, is intended as a one-way information stream, with the LobbyView
	 * displaying either the LobbyLog or a recent (perhaps partial) snapshot of
	 * it, using the Controller to make updates.
	 * @param lobbyLog
	 */
	@Override
	public void setLobbyLog( LobbyLog lobbyLog ) {
		this.lobbyLog = lobbyLog ;
	}
	
	
	/**
	 * Sets the availability of the specified game mode.  Note that the actual game
	 * modes are read from a lobby object; this method only specifies the
	 * button availability of an existing mode.
	 * 
	 * Note also that, by default, we follow "black list" behavior of "DEFAULT ALLOW."
	 * That is, a particular game mode is assumed to be OptionAvailability.ENABLED
	 * unless this method is called with a different parameter.
	 * 
	 * @param gameMode
	 * @param availability
	 */
	public void setGameModeAvailability( int gameMode, OptionAvailability availability ) {
		// do nothing
	}
	
	
	/**
	 * Sets whether this lobby is local Wifi only, the alternative being
	 * internet.
	 * 
	 * @param wifi
	 */
	@Override
	public void setIsWifiOnly( boolean wifi ) {
		// drop this on the floor
	}
	
	
	/**
	 * Sets whether this lobby user is acting as the host for the lobby,
	 * the alternative being client-only.
	 * 
	 * @param host
	 */
	@Override
	public void setIsHost( boolean host ) {
		// drop this on the floor
	}
	
	/**
	 * Sets whether the user has Android Beam available at this moment.
	 * @param enabled
	 */
	@Override
	public void setAndroidBeamIsEnabled( boolean enabled ) {
		// drop this on the floor
	}
	
	/**
	 * Sets the color scheme used by this lobby.  It might be ignored.
	 * @param cs
	 */
	public void setColorScheme( ColorScheme cs ) {
		// drop this on the floor
	}
	
	/**
	 * Sets the sound pool for this lobby view adapter.
	 * @param soundPool
	 */
	public void setSoundPool( QuantroSoundPool soundPool ) {
		// drop this on the floor
	}

	/**
	 * Sets the sound pool for this lobby view adapter.
	 * @param soundPool
	 */
	public void setSoundControls( boolean soundControls ) {
		// drop this on the floor
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// UPDATES
	//
	// LobbyView will receive update messages when LobbyClient changes.  These
	// updates are sent by the Controller.  It's up to the LobbyView to handle
	// retrieval of the actual info from LobbyClient and update its own display.
	//
	///////////////////////////////////////////////////////////////////////////	
	
	
	/**
	 * The most basic update: player slot and the like.  This should (probably)
	 * only be called once; however, it is called in 'update', so make sure performing
	 * this function doesn't trample other data.
	 */
	public void update_basics() {
		refreshHandler.sleep(10) ;
	}
	
	/**
	 * This player's votes have updated.  These updates reflect
	 * local changes - i.e., the LobbyClient knows we voted / unvoted
	 * for something, but the LobbyServer may not know yet.
	 */
	public void update_myVotes() {
		// Change button text.
		refreshHandler.sleep(10) ;
	}
	
	/**
	 * Votes have been updated!
	 */
	public void update_votes() {
		refreshHandler.sleep(10) ;
	}
	
	/**
	 * Game launches have updated.  Get new information.
	 */
	public void update_launches() {
		refreshHandler.sleep(10) ;
	}
	
	/**
	 * The players in the lobby have changed, either by who's
	 * present, or their names.  Perform an update.
	 */
	public void update_players() {
		refreshHandler.sleep(10) ;
	}
	
	/**
	 * Performs all updates.
	 */
	public void update_lobby() {
		update_basics() ;
		update_myVotes() ;
		update_votes() ;
		update_launches() ;
	}
	
	
	/**
	 * A new text message!  Update them.
	 */
	public void update_log() {
		System.err.println("update_log") ;
		if ( lobbyLog != null ) {
			System.err.println("update_log, lobbyLog is not null") ;
			int num = lobbyLog.getEventSliceFrom(lobbyLogEvents, lastDisplayedLogId+1) ;
			System.err.println("" + num + " no items") ;
			for ( int i = 0; i < num; i++ ) {
				LobbyLog.Event e = lobbyLogEvents.get(i) ;
				lastDisplayedLogId = Math.max( lastDisplayedLogId, e.id ) ;
				
				System.err.println(e.toString()) ;
			}
		}
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	//
	// THREADING
	//
	// If the LobbyView runs its own thread, these might be useful.  Otherwise,
	// they should do basically nothing.  Possibly 'start' and 'resume' should
	// handle resource allocation and initial state queries, since we are
	// guaranteed to have set a controller and LobbyClient by the time they
	// are called.
	//
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Starts the thread for a LobbyView
	 */
	public void start() {
		
	}
	
	/**
	 * Stops the lobby view thread, if one exists.  This will be 
	 * called before the activity is suspended.  The LobbyView should
	 * stop updating after this call, but should still respond to 'start()'
	 * or 'resume' without loss of functionality.
	 */
	public void stop() {
		// Nothing
	}
	
	/**
	 * Blocks until the LobbyView thread is finished.  If the LobbyView
	 * has not dedicated thread, this method should immediately return.
	 */
	public void join() {
		// nothing; there is no thread.
	}
	
	
	/**
	 * The user has short-clicked a button we for game launches.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param gameMode
	 * @param saveKey
	 */
	public boolean mpglbs_onButtonClick(
			MultiPlayerGameLaunchButtonStrip strip,
			int buttonNum, int buttonType, int gameMode ) {
		mwrDelegate.get().lvad_userSetVote(gameMode, !lobby.getLocalVote(gameMode)) ;
		return true ;
	}
	
	/**
	 * The user has long-clicked a button we for game launches.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param gameMode
	 * @param saveKey
	 */
	public boolean mpglbs_onButtonLongClick(
			MultiPlayerGameLaunchButtonStrip strip,
			int buttonNum, int buttonType, int gameMode ) {
		return false ;
	}
	
	/**
	 * The user has long-clicked a button we for game launches.  The
	 * button type, one of BUTTON_TYPE_*, is provided, so you can determine
	 * the right action.
	 * 
	 * @param strip
	 * @param buttonNum
	 * @param buttonType
	 * @param gameMode
	 * @param saveKey
	 */
	public boolean mpglbs_supportsLongClick(
			MultiPlayerGameLaunchButtonStrip strip,
			int buttonNum, int buttonType, int gameMode ) {
		return false ;
	}
	
}
