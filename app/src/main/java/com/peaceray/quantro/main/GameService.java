package com.peaceray.quantro.main;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;

import com.peaceray.quantro.GameActivity;
import com.peaceray.quantro.GameIntentPackage;
import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.R;
import com.peaceray.quantro.adapter.action.ActionAdapter;
import com.peaceray.quantro.adapter.action.ActionAdapterWithGameIO;
import com.peaceray.quantro.communications.MultipleMessageReader;
import com.peaceray.quantro.communications.SimpleSHA1;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingDirectClientConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingDirectServerConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingLayer;
import com.peaceray.quantro.communications.messagepassing.MessagePassingPairedConnection;
import com.peaceray.quantro.communications.messagepassing.matchseeker.MatchRouter;
import com.peaceray.quantro.communications.messagepassing.matchseeker.MessagePassingMatchSeekerConnection;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.AdministratedMessageAwareWrappedUDPSocketChannelAdministrator;
import com.peaceray.quantro.consts.GlobalTestSettings;
import com.peaceray.quantro.database.GameSettingsDatabaseAdapter;
import com.peaceray.quantro.database.GameStats;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.keys.KeyStorage;
import com.peaceray.quantro.lobby.InternetLobbyGame;
import com.peaceray.quantro.lobby.InternetLobbyGameMaintainer;
import com.peaceray.quantro.lobby.WebConsts;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;
import com.peaceray.quantro.main.notifications.NotificationAdapter;
import com.peaceray.quantro.main.notifications.QuantroNotificationMaker;
import com.peaceray.quantro.model.GameSaver;
import com.peaceray.quantro.model.communications.ClientCommunications;
import com.peaceray.quantro.model.communications.GameMessage;
import com.peaceray.quantro.model.communications.MultiplayerClientCommunications;
import com.peaceray.quantro.model.communications.NoOpClientCommunications;
import com.peaceray.quantro.model.game.BrokenGameCopy;
import com.peaceray.quantro.model.game.Game;
import com.peaceray.quantro.model.game.GameEvents;
import com.peaceray.quantro.model.game.GameInformation;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.server.GameCoordinator;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.AndroidID;
import com.peaceray.quantro.utils.Base64;
import com.peaceray.quantro.utils.DeviceModel;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;


/**
 * A GameService handles most game operations, updating the bound
 * game Activity when necessary, connecting to the server and/or
 * clients, etc.
 * 
 * The reason for separating part of the functionality into a Service is 
 * so that Connections do not automatically break when the user presses
 * "Home".
 * 
 * For now, we do not unbind our service when pausing or stopping
 * the activity.  Hopefully that means the Service won't stop, unless
 * the Activity is destroyed, and that recreating the Activity will
 * also recreate the Service?
 * @author Jake
 *
 */
public class GameService extends Service
		implements ClientCommunications.Delegate,
					InternetLobbyGameMaintainer.Delegate, MatchRouter.UpdateListener {
	
	private final static String TAG = "GameService" ;
	
	
	private Resources res ;
	
	// GAME ACTIVITY
	// The GameActivity will give us a reference to itself; we store as 
	// an instance of the GameUserInterface interface.
	GameUserInterface gameUserInterface = null ;
	Activity activity = null ;
	boolean pausedByPlayer = false ;
	boolean pausedByDemoExpired = false ;
	
	boolean shouldStayInBackground ;
	// in onStart, this is determined by our game status - Local or Multiplayer.
	// Multiplayer games usually have this set to 'false'.  However, some other
	// actions can set this to 'true'; for instance, if the game activity is
	// finishing, we don't want to pop up a "in game" message even for a second.
	boolean alwaysForeground ;		// InternetLobbyGame must be maintained at all times.
	
	
	// Notifications
	NotificationAdapter notificationAdapter = null ;
	
	// MY VARS
	GameIntentPackage gip ;
	
	
	// Local game objects
	public GameInformation [] ginfo ;
	public GameEvents [] gevents ;
	public Game [] game ;
	public GameSettings gameSettings ;
	// Our GameResult builder.
	public GameResult.Builder grbuilder_private ;
	
	// Adapters
	public ActionAdapter [] actionAdapter = null ;
	// Communication
	ClientCommunications clientCommunications ;
	// Paired connection.  We create this early on,
	// and apply it as-needed for any self communication.
	MessagePassingPairedConnection [] mppcPair ;
	
	
	// Player info
	int numberOfPlayers ;
	int playerSlot = -1 ;
	String [] playerNames = null ;
	boolean [] playerWon = null ;
	boolean [] playerLost = null ;
	boolean [] playerQuit = null ;
	boolean [] playerKicked = null ;
	boolean [] playerSpectating = null ;
	
	// Game server
	GameCoordinator gameCoordinator ;
	MultipleMessageReader mmReader ;
	// Maintainer
	InternetLobbyGameMaintainer gameMaintainer ;
	InternetLobbyGame internetLobbyGame ;
	
	
	// GAME META DATA
	// These instance vars are very useful in tracking the current state of things.
	// Info on the Activity!
	int activityState ;
	private static final int ACTIVITY_STATE_NONE = -1 ;
	private static final int ACTIVITY_STATE_CREATED = 0 ;
	private static final int ACTIVITY_STATE_STARTED = 1 ;
	private static final int ACTIVITY_STATE_RESUMED = 2 ;
	
	// Info on the Game!
	int gameState ;
	private static final int GAME_STATE_WAITING = 0 ;
	private static final int GAME_STATE_PAUSED = 1 ;
	private static final int GAME_STATE_GO = 2 ;
	private static final int GAME_STATE_OVER = 3 ;
	boolean [] gameStatePlayers ;
	boolean resolving = false ;		// are we resolving something?
	
	
	// Info on resolution!
	boolean saveOnQuit ;		// almost always "true."  Only "false" when we are trying to resolve
								// a "to-continue" game.
	
	Handler mHandler ;
	// Internet communications?
	Runnable mRunnableServerCommunicationFailure ;	// Can't contact server.
	Runnable mRunnableServerCommunicationOK ;		// We DID contact server!
	Runnable mRunnableGameClosed ;			// Inet lobby is closed.
	boolean mServerCommunicationFailing = false ;
	boolean mServerGameClosed = false ;
	boolean mServerGameFull = false ;
	static final int SERVER_COMMUNICATION_FAILURE_COUNTDOWN = 45 * 1000 ;

	
	// Analytics?
	private boolean mAnalyticsInSession = false ;
	
	@Override
	synchronized public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind") ;

		shouldStayInBackground = false ;
		alwaysForeground = false ;
		
		// Android: onBind is called ONCE in the lifecycle of a Service,
		// regardless of how many times bindService() is called.  However, we
		// have noticed an issue where occassionally the GameService will
		// linger around from the last GameActivity, and bind to the next one.
		// Therefore, we have moved most of the data allocation and setup to
		// the 'setActivity' method.
		
		mHandler = new Handler() ;
        IBinder bind = new ServicePassingBinder( this ) ;
        return bind ;
	}
	
	
	/**
	 * This service is bound; this method is called by OS upon the initial binding.
	 */
	public void onCreate() {
		super.onCreate() ;
		
		if ( QuantroPreferences.getAnalyticsActive(this) ) {
			mAnalyticsInSession = true ;
			Analytics.startSession(this) ;
		}
		
		// We're using nio UDP sockets, selectors, etc.  This shouldn't
		// be a problem on most version of Android, but 2.2 fails.
		// This method callo performs the necessary setup to prevent failure.
		VersionSafe.prepareForUDPnio(this) ;
		
		// provide self as lobby service to QuantroApplication.
		((QuantroApplication)getApplication()).setGameService(this) ;
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy") ;
		
		recycle() ;
		
		// Release the wake lock, if we have one.
		this.getLocksAndReleaseIfHeld() ;
	    
		// Make sure we aren't displaying a notification...
		if ( notificationAdapter != null )
			notificationAdapter.dismissForegroundNotification() ;
		
		// kill communications 
		try {
			if ( clientCommunications != null ) {
				clientCommunications.stop() ;
			}
		} catch ( Exception e ) { }
		
		try {
			if ( gameCoordinator != null ) {
				gameCoordinator.stop() ;
			}
		} catch ( Exception e ) { }
		
		try {
			if ( mmReader != null )
				mmReader.kill() ;
		} catch ( Exception e ) { }
		
		try {
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
		} catch ( Exception e ) { }
		
		
		
		try {
			if ( clientCommunications != null )
				clientCommunications.join() ;
		} catch ( Exception e ) { }
		
		try {
			if ( gameCoordinator != null )
				gameCoordinator.join() ;
		} catch ( Exception e ) { }
		
		if ( mAnalyticsInSession )
			Analytics.stopSession(this) ;
		
		try {
			if ( clientCommunications != null ) {
				clientCommunications.recycle() ;
			}
		} catch ( Exception e ) { }
		
		try {
			if ( gameCoordinator != null ) {
				gameCoordinator.recycle() ;
			}
		} catch ( Exception e ) { }
		
		// remove self as lobby service in QuantroApplication.
		((QuantroApplication)getApplication()).unsetGameService(this) ;
	}
	
	
	/*
	 * *************************************************************************
	 * 
	 * BINDING HELPER METHODS
	 * 
	 * These methods are called by onBind to help streamline the construction
	 * of our necessary structures.  The below could be inlined at no cost but
	 * readability.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
     * Allocates fresh Game (and GameInformation etc.) objects, the
     * number of which is determined by the game mode.
     */
	synchronized private void allocateAndInitializeLocalGameObjects( int mode, GameSettings gs ) {
		gameSettings = gs ;
    	numberOfPlayers = gs.getPlayers() ;
    	playerNames = new String[numberOfPlayers] ;
    	playerWon = new boolean[numberOfPlayers] ;
    	playerLost = new boolean[numberOfPlayers] ;
    	playerQuit = new boolean[numberOfPlayers] ;
    	playerKicked = new boolean[numberOfPlayers] ;
    	playerSpectating = new boolean[numberOfPlayers] ;
        
        // Allocate array structures for Game objects!
        ginfo = new GameInformation[ numberOfPlayers ] ;
        gevents = new GameEvents[ numberOfPlayers ] ;
        game = new Game[ numberOfPlayers ] ;
        
        for ( int i = 0; i < numberOfPlayers; i++ ) {
	    	ginfo[i] = new GameInformation( mode, gs.hasLevel() ? gs.getLevel() : 1 ).finalizeConfiguration() ;
	    	ginfo[i].firstGarbage = gs.hasGarbage() ? gs.getGarbage() : GameModes.defaultGarbage(mode) ;
	    	ginfo[i].garbage = gs.hasGarbagePerLevel() ? gs.getGarbagePerLevel() : GameModes.defaultGarbage(mode) ;
	    	ginfo[i].levelLock = gs.hasLevelLock() ? gs.getLevelLock() : false ;
	    	
	    	if ( GameModes.setClears(mode) == GameModes.SET_CLEAR_ANY )
	    		ginfo[i].nyclearsSinceLevelForLevelUp = gs.hasClearsPerLevel() ? gs.getClearsPerLevel() : -1 ;
	    	else if ( GameModes.setClears(mode) == GameModes.SET_CLEAR_TOTAL )
	    		ginfo[i].tlclearsSinceLevelForLevelUp = gs.hasClearsPerLevel() ? gs.getClearsPerLevel() : -1 ;
	    	
	    	// fixed rate and difficulty
	    	ginfo[i].difficulty = gs.hasDifficulty() ? gs.getDifficulty() : GameInformation.DIFFICULTY_NORMAL ;
	    	if ( gs.hasDisplacementFixedRate() ) {
	    		int setDisp = GameModes.setDisplacementFixedRate(mode) ;
	    		switch( setDisp ) {
	    		case GameModes.SET_DISPLACEMENT_FIXED_RATE_NO:
	    			break ;
	    		case GameModes.SET_DISPLACEMENT_FIXED_RATE_PRACTICE:
	    			if ( ginfo[i].difficulty == GameInformation.DIFFICULTY_PRACTICE ) {
	    				ginfo[i].displacementFixedRate = gs.getDisplacementFixedRate() ;
	    			}
	    			break ;
	    		case GameModes.SET_DISPLACEMENT_FIXED_RATE_YES:
	    			ginfo[i].displacementFixedRate = gs.getDisplacementFixedRate() ;
	    			break ;
	    		}
	    	}
	    	
	    		
	    	gevents[i] = new GameEvents().finalizeConfiguration() ;
	    	
	    	game[i] = new Game( GameModes.numberRows(ginfo[i]), GameModes.numberColumns(ginfo[i]) ) ;
	    	game[i].setGameInformation(ginfo[i]) ;
	    	game[i].setGameEvents( gevents[i] ) ;
	    	game[i].setSystemsFromSerializables(null) ;
	    	game[i].setPseudorandom(gip.nonce.smallInt()) ;
	    	
	    	game[i].makeReady() ;
	    	game[i].finalizeConfiguration() ;
	    	
	    	if ( gip.playerNames != null )
	    		playerNames[i] = gip.playerNames.length > i ? gip.playerNames[i] : null ;
	    	playerWon[i] = false ;
	    	playerLost[i] = false ;
	    	playerQuit[i] = false ;
	    	playerKicked[i] = false ;
	    	playerSpectating[i] = false ;
        }
        
        // tell the Achievements: a new game.
        Achievements.game_new() ;
        
        if ( gip.internetLobbyGame != null )
        	internetLobbyGame = gip.internetLobbyGame.newInstance() ;
    }
    
    
    /**
     * Initializes and connects adapters to our local games.
     * 
     * @param gip
     */
	synchronized private void allocateAndConnectLocalGameAdapters( ) {
    	actionAdapter = new ActionAdapter[ numberOfPlayers ] ;
        for ( int i = 0; i < numberOfPlayers ; i++ ) {
        	actionAdapter[i] = new ActionAdapterWithGameIO( game[0].R(), game[0].C() ) ;
        	game[i].setActionAdapter(actionAdapter[i]) ;
        }
    }
    
    
    /**
     * Initializes the ClientCommunications layer
     * 
     * This method does NOT create or in any way affect a local Server
     * in the case of WifiServer connection - create it yourself before
     * running clientCommunications.connect().
     * 
     * Speaking of, this method does NOT run clientCommunications.connect(),
     * but the clientCommunications objects should be ready for this call
     * once this method returns 'true'.
     * 
     * @param gip
     * @return Whether initialization was successful.  A return of 'false' indicates
     * that the server hostname provided by gaip could not be found.
     */
	synchronized private boolean initializeAndActivateClientCommunications( GameIntentPackage gip ) {
    	// We do everything but 'connect', which we leave for later.
		// We always, no matter what, instantiate a MessagePassingPairedConnection pair.
		// This will be used for self-communication when we talk to ourselves, and
		// left unused when we don't.
		//Log.d(TAG, "initializeAndActivateClientCommunications with pnonce " + gip.personalNonce) ;
		mppcPair = MessagePassingPairedConnection.newPair(
				gip.nonce,
				new Nonce[]{gip.personalNonce, gip.personalNonce},
				new String[]{gip.name, gip.name} ) ;
        
        if ( gip.connection == GameIntentPackage.CONNECTION_LOCAL )
        	clientCommunications = new NoOpClientCommunications() ;
        else
        	clientCommunications = new MultiplayerClientCommunications() ;
        	// At present, the only other connection types are WifiServer
        	// and Client, both of require that local games communicate
        	// through this MP layer, although in WifiServer it will use
        	// a loopback network address.
        
        
        clientCommunications.sendPauseRequest(this.pausedByPlayer) ;
        clientCommunications.setGameNonce(gip.nonce) ;
        clientCommunications.setMyNonce(gip.personalNonce) ;
        clientCommunications.setName(gip.name) ;
        clientCommunications.setDelegate(this) ;
        for ( int i = 0; i < numberOfPlayers; i++ ) {
        	// Hook up to our action adapters.
        	clientCommunications.setActionAdapter(actionAdapter[i], i) ;
        	actionAdapter[i].setGameActionListener(clientCommunications) ;
        }
        
        // Local connections need go no further.
        if ( gip.connection == GameIntentPackage.CONNECTION_LOCAL )
        	return true ;
        
        // Servers will use the "client" half of the connected pair.
        if ( gip.isServer() ) {
        	mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_CLIENT].activate() ;
        	clientCommunications.setConnection(mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_CLIENT]) ;
        	return true ;
        }
        
        SocketAddress connectToAddress = gip.socketAddress ;
        
        try {
        	MessagePassingConnection conn = null ;
        	if ( gip.connection == GameIntentPackage.CONNECTION_DIRECT_CLIENT ) {
        		conn = new MessagePassingDirectClientConnection(
					GameMessage.class,
					gip.nonce, gip.personalNonce, gip.name, connectToAddress) ;
        	} else if ( gip.connection == GameIntentPackage.CONNECTION_MATCHSEEKER_CLIENT ) {
        		String xl = null ;
	        	if ( KeyStorage.hasXLKey(this) )
	        		xl = KeyStorage.getXLKey(this).getKey() ;
	        	MatchRouter matchRouter = new MatchRouter(
	        			this,
	        			new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator(), 
	        			true,	// parallel matching: fill all slots at once, for different players!
	        			xl, connectToAddress, VersionCapabilities.isEmulator() ? new int[]{52051, 52051} : new int[]{52000, 53000},
	        			internetLobbyGame, (byte)1, GameMessage.class,
	        			gip.personalNonce, gip.name,
	        			Base64.encodeBytes(SimpleSHA1.SHA1Bytes(gip.nonce.toString() + AndroidID.get(this))),
	        			null ) ;		// no reserved nonces for client
	        	Log.d(TAG, "Starting MatchSeeker connection with intent 1 as CLIENT") ;
	        	conn = new MessagePassingMatchSeekerConnection(
	        			matchRouter,
        				new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator(),
        				gip.nonce,
        				gip.personalNonce, gip.name,
        				null,
        				GameMessage.class) ;
        	} else {
        		Log.d(TAG, "NOT creatirng a MPC!  connection type is " + gip.connection) ;
        	}
        	conn.activate() ;
			clientCommunications.setConnection( conn ) ;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false ;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false ;
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false ;
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        // That should be all the setup we need here...
        return true ;
    }
    
    
    /**
     * Initialize and activate our GameCoordinator - if appropriate, i.e., if we're
     * a server.
     * 
     * 
     * @param gip
     * @return Whether gameCoordinator was successfully placed in the appropriate
     * state.  If 'true', than gameCoordinator == null IFF we are a local game or a client,
     * and is allocated and ready for start() if a server.  If 'false', something went wrong.
     */
	synchronized private boolean initializeAndActivateGameCoordinator( GameIntentPackage gip ) {
    	if ( gip.isServer() ) {
        	// Set up a MessagePassingLayer with our nonce/personal nonces
        	// using DirectServerConnections.
        	MessagePassingLayer mpLayer = new MessagePassingLayer( numberOfPlayers ) ;
        	mmReader = new MultipleMessageReader() ;
        	AdministratedMessageAwareWrappedUDPSocketChannelAdministrator administrator =
        		new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator() ;
        	String xl = null ;
        	if ( KeyStorage.hasXLKey(this) )
        		xl = KeyStorage.getXLKey(this).getKey() ;
        	MatchRouter matchRouter = null ;
        	if ( gip.connection == GameIntentPackage.CONNECTION_MATCHSEEKER_SERVER ) {
	        	try {
	        		matchRouter = new MatchRouter(this,
	            			new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator(), 
	            			true,	// parallel matching: fill all slots at once, for different players!
	            			xl, gip.socketAddress, new int[]{52000, 53000},
	            			internetLobbyGame, (byte)1, GameMessage.class,
	            			gip.personalNonce, gip.name,
	            			Base64.encodeBytes(SimpleSHA1.SHA1Bytes(gip.nonce.toString() + AndroidID.get(this))),
	            			gip.playerNonces ) ;		// reserved nonces for all our clients
	        	}  catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false ;
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false ;
				}
        	}
        			
        	for ( int i = 0; i < numberOfPlayers; i++ ) {
        		// If we have playerNonce...
        		Nonce clientPNonce = gip.playerNonces.length > i ? gip.playerNonces[i] : null ;
        		//Log.d(TAG, "initializeAndActivateGameCoordinator: player " + i + " has pnonce " + pnonce) ;
        		try {
        			MessagePassingConnection mpc = null ;
        			// Slot 0 is OURS if we are a server; provide the server end of
        			// the mppcPair.
        			if ( i == 0 ) {
        				mpc = mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_SERVER] ;
        			}
        			
        			// Other slots are direct if we are a DIRECT server.
        			else if ( gip.connection == GameIntentPackage.CONNECTION_DIRECT_SERVER ) {
        				mpc = new MessagePassingDirectServerConnection(
								((InetSocketAddress)gip.socketAddress).getPort(), GameMessage.class,
								gip.nonce, clientPNonce, true, false) ;
        				((MessagePassingDirectServerConnection)mpc).setMultipleMessageReader(mmReader) ;
        			}
        			else if ( gip.connection == GameIntentPackage.CONNECTION_MATCHSEEKER_SERVER ){
        				// Outer 'if' guarantees we are a server; inner 'else' means we
        				// are mediated, and this is not slot 0.
        	        	Log.d(TAG, "Starting MatchSeeker connection with intent 1 as HOST") ;
        	        	mpc = new MessagePassingMatchSeekerConnection(
        	        			matchRouter,
        	        			administrator,
                				gip.nonce,
                				gip.personalNonce, gip.name,
                				clientPNonce,
                				GameMessage.class) ;
        	        	((MessagePassingMatchSeekerConnection)mpc).setMultipleMessageReader(mmReader) ;
        			}
        			
        			mpLayer.setConnection(i, mpc) ;
        			
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false ;
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false ;
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false ;
				}
        	}
        	mpLayer.activate() ;
        	gameCoordinator = new GameCoordinator( gip.gameSettings.getMode(), numberOfPlayers, gip.nonce.smallInt() ) ;
        	gameCoordinator.setMessagePassingLayer(mpLayer) ;
        	
        	gameMaintainer = null ;
        	if ( gip.isMatchseeker() ) {
        		gameMaintainer = InternetLobbyGameMaintainer.newMaintainer( internetLobbyGame, this ) ;
        		gameMaintainer.hostEvery( 60 * 1000 ) ;	// every minute
    			gameMaintainer.maintainEvery( 10 * 60 * 1000 ) ;	// every 10 minutes
    			gameMaintainer.refreshEvery(60 * 1000) ;	// every minute
    			
    			// we don't know how long it's been since the Portfolio refreshed,
    			// maintained or hosted this game.  Do all of them right now.
    			gameMaintainer.host().maintain().refresh() ;
        	}
        }
    	else {
    		gameCoordinator = null ;
    		mmReader = null ;
    		gameMaintainer = null ;
    		if ( gip.isMatchseeker() ) {
    			gameMaintainer = InternetLobbyGameMaintainer.newRefresher( internetLobbyGame, this ) ;
    			gameMaintainer.refreshEvery(60 * 1000) ;	// every minute
    			gameMaintainer.refresh() ;
    		}
    	}
    	return true ;
    }
	
	
	private void allocateAndInitializePeaceRayServerCommunicationHandlers( GameIntentPackage gip ) {
		// make a handler...
		mHandler = new Handler() ;
		// set handler operation vars
		mServerCommunicationFailing = false ;
		mServerGameClosed = false ;
		
		// create custom Runnables.  When our LobbyMaintainer or
		// InternetGameMaintenancePortfolio
		// runs into a problem that needs fixing, these do the necessaries.
		// mRunnableServerCommunicationFailure : Can't contact server.
		// this runnable is queued (at a delay) upon a communication failure,
		// indicating that either we can't get a good response from the server,
		// or the server is having DB issues.  When it runs it should, at a 
		// minimum, notify the GUI of the problem and set mServerCommunicationFailing
		// to true.
		mRunnableServerCommunicationFailure = new Runnable() {
			@Override
			public void run() {
				GameUserInterface gui = gameUserInterface ;
				if ( !mServerCommunicationFailing && gui != null ) {
					mServerCommunicationFailing = true ;
					gui.gui_peacerayServerCommunicationFailure() ;
					
					// TODO: We've told the GUI, but it might be hidden.
					// We also need to change the foreground notification.
					
				}
			}
		} ;
		
		
		// mRunnableServerCommunicationOK :		We DID contact server!
		// this runnable is run immediately upon a successful communication.  It
		// should have NO EFFECT if mServerCommunicationFailing is false.  Otherwise,
		// it should at a minimum notify the GUI that communication seems to be
		// OK again, and set mServerCommunicationFailing to false.
		mRunnableServerCommunicationOK = new Runnable() {
			@Override
			public void run() {
				GameUserInterface gui = gameUserInterface ;
				if ( mServerCommunicationFailing && gui != null ) {
					mServerCommunicationFailing = false ;
					gui.gui_peacerayServerCommunicationOK() ;
					
					// TODO: We've told the GUI, but it might be hidden.
					// We also need to change the foreground notification.
				}
			}
		} ;
		
		
		
		// mRunnableLobbyClosed ;			// Inet lobby is closed.
		// turns out the lobby we are hosting is closed.  This means we (pretty much)
		// need to SHUT.  DOWN.  EVERYTHING.  However, we probably want to 
		// tell all connected users what's happening?  Also, notify the GUI
		// and set mServerLobbyClosed to true.
		mRunnableGameClosed = new Runnable() {
			@Override
			public void run() {
				GameUserInterface gui = gameUserInterface ;
				if ( !mServerGameClosed && gui != null ) {
					mServerGameClosed = true ;
					if ( gameMaintainer.isStarted() )
						gameMaintainer.stop() ;
					gui.gui_peacerayServerLobbyClosed() ;
					// TODO: if hosting, send a nice message to everyone
					// connected and close things down.
					
					// TODO: We've told the GUI, but it might be hidden.
					// We also need to change the foreground notification.
				}
			}
		} ;
	}
	
    
    /**
     * Initializes our meta data variables.
     * @param gap
     */
	synchronized private void initializeGameMetaData( GameIntentPackage gip ) {
    	activityState = ACTIVITY_STATE_NONE ;
    	
    	// Info on the Game!
    	gameState = GAME_STATE_WAITING ;
    	
    	// Players.  Init to 'false' for all
    	gameStatePlayers = new boolean[numberOfPlayers] ;
    	for ( int i = 0; i < numberOfPlayers; i++ )
    		gameStatePlayers[i] = false ; 
    	
        // We have already loaded the game in progress, if indeed there was
    	// a saved game.  If not, we make a new GameResult builder.
    	// Note that this method is only called from setActivity, after the
    	// following have occurred:
    	// grbuilder = null ;
    	// if ( hasSavedGame )
    	//		grbuilder = new GameResult.Builder(savedGameResult) ;
    	
    	// This method call will create and initialize a GameResult.Builder
    	// IF the object does not already exist.  Otherwise, it will have no real
    	// effect.
    	getGameResultBuilder() ;
    	
    	// Should we go completely into the background when the Activity is stopped?
		shouldStayInBackground = gip.isLocal() ;
		alwaysForeground = gip.isMatchseeker() ;
    }
    
    
    synchronized private void resetAllMemberVariables() {
    	gameUserInterface = null ;
    	activity = null ;
    	
    	shouldStayInBackground = false ;
    	alwaysForeground = false ;
    	// in onStart, this is determined by our game status - Local or Multiplayer.
    	// Multiplayer games usually have this set to 'false'.  However, some other
    	// actions can set this to 'true'; for instance, if the game activity is
    	// finishing, we don't want to pop up a "in game" message even for a second.
    	
    	// Notifications
    	if ( notificationAdapter != null ) {
    		notificationAdapter.dismissForegroundNotification() ;
    		notificationAdapter.setContext(null) ;
    	}
    	notificationAdapter = null ;
    	
    	// Release the wake lock, if we have one.
    	this.getLocksAndReleaseIfHeld() ;
    	
    	gip = null ;
    	
    	// Local game objects
    	ginfo = null ;
    	gevents = null ;
    	game = null ;
    	grbuilder_private = null ;
    	
    	// Adapters
    	actionAdapter = null ;
    	
    	try {
	    	if ( clientCommunications != null )
	    		clientCommunications.stop() ;
    	} catch (Exception e) { e.printStackTrace() ; }
    	clientCommunications = null ;
    	mppcPair = null ;
    	
    	// Player info
    	numberOfPlayers = -1 ;
    	playerSlot = -1 ;
    	playerNames = null ;
    	
    	// Internet Lobby Game; set to null.
    	internetLobbyGame = null ;
    	
    	// Game server
    	try {
	    	if ( gameCoordinator != null )
	    		gameCoordinator.stop() ;
    	} catch (Exception e) { e.printStackTrace() ; }
    	gameCoordinator = null ;
    	
    	try {
    		if ( mmReader != null )
    			mmReader.kill() ;
    	} catch (Exception e) { e.printStackTrace() ; }
    	mmReader = null ;
    	
    	try {
    		if ( gameMaintainer != null )
    			gameMaintainer.stop() ;
    	} catch (Exception e) { e.printStackTrace() ; }
    	gameMaintainer = null ;
    	
    	// GAME META DATA
    	// These instance vars are very useful in tracking the current state of things.
    	// Info on the Activity!
    	activityState = -1 ;
    	
    	// Info on the Game!
    	gameState = 0 ;
    	gameStatePlayers = null ;
    	
    	pausedByPlayer = false ;
    	pausedByDemoExpired = false ;
    }
    
    
	/*
	 * *************************************************************************
	 * 
	 * WAKE LOCK COLLECTION
	 * 
	 * *************************************************************************
	 */
    
    private static final String LOCK_NAME_PARTIAL_STATIC="quantro:game_partial";
    private static final String LOCK_NAME_FULL_STATIC="quantro:game_full" ;
	private volatile PowerManager.WakeLock lockPartial=null;
	private volatile PowerManager.WakeLock lockFull=null ;
    
    synchronized private PowerManager.WakeLock getPartialLock(Context context) {
		if (lockPartial==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lockPartial=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
										LOCK_NAME_PARTIAL_STATIC);
			lockPartial.setReferenceCounted(false);
		}
	  
		return(lockPartial);
	}
    
    
    private static final String LOCK_NAME_WIFI_STATIC="com.peaceray.quantro.main.GameService.WIFI_LOCK" ;
    private volatile WifiManager.WifiLock lockWifi=null ;
    
    synchronized private WifiManager.WifiLock getWifiLock(Context context) {
    	if ( lockWifi==null ) {
    		WifiManager mgr=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		    
    		int mode = WifiManager.WIFI_MODE_FULL ;
    		if ( VersionCapabilities.getVersion() >= 12 ) {
    			// high-performance added in api 12
    			mode = 0x3 ;
    		}
    		
    		lockWifi=mgr.createWifiLock(mode, LOCK_NAME_WIFI_STATIC) ;
			lockWifi.setReferenceCounted(false);
		}
	  
		return(lockWifi);
    }
	
    
    /**
     * Gets and "acquires" all necessary multiplayer locks.  This means a partial
     * wake lock, and (optionally) a WiFi lock.
     */
    synchronized private void getLocksAndAcquireIfMultiplayer() {
    	try {
			PowerManager.WakeLock wl = getPartialLock(this) ;
			synchronized ( wl ) {
				if ( !gip.isLocal() )
					wl.acquire() ;
			}
		} catch( Exception e ) { }
		
		// optional and user-controlled
		if ( QuantroPreferences.getNetworkWifiLock(this) ) {
			try {
				WifiManager.WifiLock wl = getWifiLock(this) ;
				synchronized ( wl ) {
					if ( !gip.isLocal() )
						wl.acquire() ;
				}
			} catch ( Exception e ) { }
		}
    }
    
    
    /**
     * Gets and "acquires" all necessary multiplayer locks.  This means a partial
     * wake lock, and (optionally) a WiFi lock.
     */
    synchronized private void getLocksAndReleaseIfHeld() {
    	try {
			PowerManager.WakeLock wl = getPartialLock(this) ;
			synchronized ( wl ) {
				if ( wl.isHeld() )
					wl.release() ;
			}
		} catch( Exception e ) { }
		
		// TODO: make this optional and user-controlled
		try {
			WifiManager.WifiLock wl = getWifiLock(this) ;
			synchronized ( wl ) {
				if ( wl.isHeld() )
					wl.release() ;
			}
		} catch ( Exception e ) { }
    }
    
    
    
	/*
	 * *************************************************************************
	 * 
	 * NOTIFICATION HELPER METHODS
	 * 
	 * These methods help in creating, displaying, and updating notifications
	 * for when the Activity is not currently running or being displayed.
	 * Compare these methods against those far below, in the
	 * FOREGROUND SERVICES API section, which wrap API calls to allow foreground
	 * notifications regardless of API version, but do NOT track our notification
	 * status or know what kind of messages should be displayed.
	 * 
	 * The methods below are, essentially, callbacks for state updates.  They are
	 * of two basic types: 1., a toggle for whether our Service should be currently
	 * running in the foreground, and 2., methods for updating the CONTENT of
	 * our foreground notification, if indeed one should be displayed.  Because
	 * these things are expected to change approximately independently
	 * (one is based on the Activity, the other possibly on the state of a remote
	 * server), we want to be able to call these methods in any order and in
	 * any state.  Therefore:
	 * 
	 * 1. If foreground is toggled ON, we immediately display the most recent
	 * 		notification set.  If it is toggled OFF, we stop displaying 
	 * 		notifications until it is toggled ON again.  Repeated calls to On
	 * 		or OFF have no effect.
	 * 
	 * 2. When notification content is set, if foreground is toggled ON, the content
	 * 		of the notification is immediately updated.  If foreground is toggled
	 * 		OFF, we only note the content of the notification as the "most recent
	 * 		set" - no notification is displayed.
	 * 
	 * The two types of methods use instance vars to track changes and method calls.
	 * They might touch, but will never alter, other instance vars set for this
	 * object.
	 * 
	 * Foreground statuses for games roughly describe a subset of Activity updates.
	 * For now, at least, we DON'T give Waiting or Paused updates in the notification,
	 * only connection status.  Why bother telling the user exactly HOW they've
	 * inconvenienced people?  Just get back in the game, bro.
	 * 
	 * *************************************************************************
	 */
    
    
    // We use notifications of this basic format:
    // Connecting to <name>.
    // Talking to <name>.
    // Game against <name> is paused.
    // Game against <name> is over.
    
    // Unlike in Lobbies, there really isn't any other important information
    // to add to these Notifications - i.e., no ticker text.
    //
    // TODO: If we add something to allow games to unpause before players are
    // ready, that might be a good ticker-text opportunity.
    
    private static final int NOTIFICATION_STATUS_CONNECTION_FAILURE = 0 ;
    private static final int NOTIFICATION_STATUS_CONNECTING = 1 ;
    private static final int NOTIFICATION_STATUS_NEGOTIATING = 2 ;
    private static final int NOTIFICATION_STATUS_IN_GAME = 3 ;
    private static final int NOTIFICATION_STATUS_GAME_OVER = 4 ;
    
    private int notificationStatus ;
    private boolean notificationForeground = false ;
    
    /**
     * The foreground toggle.
     */
    synchronized private void toggleForeground( boolean inForeground ) {
    	if ( notificationAdapter == null )
    		return ;
    	
    	notificationForeground = inForeground ;
    	if ( notificationForeground ) {
    		notificationAdapter.startForegroundCompat( R.string.game_service_foreground_notification_id,
    				makeNotification( notificationStatus ) ) ;
    	}
    	else {
    		notificationAdapter.stopForegroundCompat( R.string.game_service_foreground_notification_id ) ;
    	}
    }
    
    
    /**
     * Foreground set: connecting to server.
     */
    synchronized private void foregroundConnectionFailuer() {
    	if ( notificationAdapter == null )
    		return ;
    	
    	notificationStatus = NOTIFICATION_STATUS_CONNECTION_FAILURE ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.game_service_foreground_notification_id,
    				makeNotification( notificationStatus ) ) ;
    }
    
    
    /**
     * Foreground set: connecting to server.
     */
    synchronized private void foregroundConnecting() {
    	if ( notificationAdapter == null )
    		return ;
    	
    	notificationStatus = NOTIFICATION_STATUS_CONNECTING ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.game_service_foreground_notification_id,
    				makeNotification( notificationStatus ) ) ;
    }
    
    /**
     * Foreground set: negotiating with server.
     */
    synchronized private void foregroundNegotiating() {
    	if ( notificationAdapter == null )
    		return ;
    	
    	notificationStatus = NOTIFICATION_STATUS_NEGOTIATING ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.game_service_foreground_notification_id,
    				makeNotification( notificationStatus ) ) ;
    }
    
    
    /**
     * Foreground set: connected to server.
     */
    synchronized private void foregroundConnected() {
    	if ( notificationAdapter == null )
    		return ;
    	
    	notificationStatus = NOTIFICATION_STATUS_IN_GAME ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.game_service_foreground_notification_id,
    				makeNotification( notificationStatus ) ) ;
    }
    
    
    /**
     * Foreground set: we have disconnected FOREVER.  There will
     * be no reconnects.
     */
    synchronized private void foregroundDisconnectedForever() {
    	if ( notificationAdapter == null )
    		return ;
    	
    	// TODO: Set a status for permanent disconnect.  May or
    	// may not be important.  Maybe there's a KICK message
    	// waiting for them?  Maybe the game's done and we have
    	// posted win/lose values?
    	notificationStatus = NOTIFICATION_STATUS_GAME_OVER ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.game_service_foreground_notification_id,
    				makeNotification( notificationStatus ) ) ;
    }
    
    
    /**
     * Makes and returns a Notification, appropriate for the specified
     * status at this time.
     * 
     * @param notificationStatus
     * @return
     */
    synchronized private Notification makeNotification( int notificationStatus ) {
    	int role = ( gip.isLocal() || gip.isClient() )
			? TextFormatting.ROLE_CLIENT 
			: TextFormatting.ROLE_HOST ;
    	
    	// Get the message text
    	String contentText = "" ;
    	String tickerText = null ;
    	String opponentName = (playerSlot < 0) ? null : playerNames[(playerSlot+1)%numberOfPlayers] ;
    	switch( notificationStatus ) {
    	case NOTIFICATION_STATUS_CONNECTION_FAILURE:
    		contentText = res.getString(R.string.in_game_notification_connection_terminal_failure) ;
    		tickerText = res.getString(R.string.in_game_notification_ticker_connection_terminal_failure) ;
    		break ;
    	case NOTIFICATION_STATUS_CONNECTING:
    		contentText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_GAME_CONNECTING,
    				role, opponentName ) ;	
    		break ;
    	case NOTIFICATION_STATUS_NEGOTIATING:
    		contentText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_GAME_NEGOTIATING,
    				role, opponentName ) ;	
    		break ;
    	case NOTIFICATION_STATUS_IN_GAME:
    		// Provide opponent's name TODO: Fix for > 2 players
    		
    		contentText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_GAME_READY,
    				role, opponentName ) ;
    		break ;
    	case NOTIFICATION_STATUS_GAME_OVER:
    		contentText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_GAME_OVER,
    				role, opponentName ) ;	
    		break ;
    	}
    	
    	String contextTitle = res.getString(R.string.in_game_notification_title) ;
    	
    	// TODO: Get the message icon, and a "big icon."
    	int icon = R.drawable.notification ;
    	Bitmap largeIcon = null ;
    	
    	// Use a QuantroNotificationMaker.
    	QuantroNotificationMaker maker = QuantroNotificationMaker.getNew(this) ;
    	
    	// Set icon; no sound, as no tickers.
    	maker.setSmallIcon(icon) ;
    	maker.setLargeIcon(largeIcon) ;
    	
    	// Set title and text
    	maker.setContentTitle( contextTitle ) ;
    	maker.setContentText( contentText ) ;
    	
    	// Ticker text?
    	if ( tickerText != null )
    		maker.setTicker(tickerText) ;
    	
    	// What happens when clicked?  Well, basically, we return to this Activity.
    	Intent notificationIntent = new Intent(GameService.this, GameActivity.class ) ;
    	notificationIntent.setAction( Intent.ACTION_MAIN ) ;
    	notificationIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP ) ;
    	notificationIntent.putExtra(GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE, gip) ;
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT) ;
    	maker.setContentIntent(contentIntent) ;
    	// Consider using FLAG_CANCEL_CURRENT rather than FLAG_UPDATE_CURRENT.
    	// The difference is that with CANCEL_CURRENT, only and exactly this extra
    	// data will be available to only and exactly the recipient of this Intent.
    	
    	// Oh, and when?  Now!
    	maker.setWhen(System.currentTimeMillis()) ;
    	
    	// Return 
    	return maker.getNotification() ;
    }
    
    
	/*
	 * *************************************************************************
	 * 
	 * ACTIVITY STATUS
	 * 
	 * Update methods regarding the current status of the Activity.  These
	 * methods will be called upon state change, as expected.
	 * 
	 * Additionally, the Activity will immediately call the "set GameUserInterface"
	 * method when it becomes aware of the Service, i.e., once it successfully
	 * binds, followed by setGameObjectReferences.
	 * As a convention, after binding, we will make the "queued method calls"
	 * so that the Service will always move through Create to Start to Resume,
	 * and then (optionally) to Pause, Stop, etc.  We will take as many steps in the
	 * progression as are necessary to reach the most recent of these methods.
	 * In other words:
	 * 
	 * create() -> start() -> resume() -> pause() -> stop() -> destroy()
	 * 1			2			3			4			5		6
	 * 
	 * The Activity will make sequential calls in-order until it meets its
	 * current state.
	 * 
	 * The exception is gameActivityFinishing.  Upon this call, we can be assured
	 * that the Activity is shutting down and should do everything possible to
	 * expedite this process.
	 * 
	 * *************************************************************************
	 */
    
    
    /**
     * Provides a references to the Activity  (probably a GameActivity);
     * we will use this for setting up Intents (to Resume() the activity when
     * needed).
     */
    public synchronized void setActivity( Activity activity, GameIntentPackage gip ) {
    	Log.d(TAG, "setActivity") ;
    	
    	if ( this.activity != null && this.activity != activity ) {
    		// the lobby does not match (we failed the check above); in addition,
    		// this is not the same Activity as before.  Close the previous activity.
    		// First set our activity reference to null, just in case a new
    		// activity call comes in due to our "finish" call (we don't want to respond
    		// to it because we still have the old Activity's reference).
    		Activity a = this.activity ;
    		this.activity = null ;
    		// Now close the activity.
    		a.finish() ;
    	}
    	
    	resetAllMemberVariables() ;
    	
    	this.activity = activity ;
    	this.gip = gip ;
    	
		// Set resources for GameModes.
        res = getResources() ;
        
        saveOnQuit = true ;		// by default, we DO save on quit.
		
		// STEP NEGATIVE ONE!  Multiplayer games maintain a consistent connection.
		// One of the requirements is that we prevent device sleep (screen off
		// is okay, but not processor off) when multiplayer.
        this.getLocksAndAcquireIfMultiplayer() ;
		
		// STEP ZERO!  Because we want notifications to work on >= 2.0 and < 2.0, we
		// need to use special wrappers to those notifications.  We call this method
		// to set it up.  An instance var, set inside this method, ensures that the
		// operations only happen once (the first time it is called) in case of numerous
		// "onBind"s.
		notificationAdapter = new NotificationAdapter(this) ;
		
		// We bind with a particular intent; that intent MUST include
		// a GameIntentPackage.
		
        int gameMode = gip.gameSettings.getMode() ;
        //Log.d(TAG, "have game mode: " + gameMode + ", saveTo: " + gap.saveToKey + ", loadFrom " + gap.loadFromKey ) ;
        
        // FIRST: Allocate and initialize local game elements Games.
        allocateAndInitializeLocalGameObjects( gameMode, gip.gameSettings ) ;
        // After this call, game, gevents, ginfo, numberOfPlayers, internetLobbyGame
        // have been initialized and set, and playerNames has been initialized
        // (but no entries filled just yet).
        
        // SECOND: Load a previous game state into our local objects.
        if ( gip.loadFromKey != null )
        	this.loadGame(gip.loadFromKey) ;
        GameSettingsDatabaseAdapter.updateMostRecentInDatabase(this, gameSettings) ;
        // Whether we loaded or not, now's the time to update our settings.
        
        // THIRD: Allocate and connect control structures for our local game elements.
        allocateAndConnectLocalGameAdapters( ) ;
        // After this call, we have created ActionAdapters and connected them to our games.
        
        // FOURTH: Create and connect our client communications (which might be NoOp).
        if ( !initializeAndActivateClientCommunications( gip ) ) {
        	// TODO: Handle failure to get SocketAddress from Hostname.
        }
        // After this call, clientCommunications has been allocated and activated,
        // and is ready to start().
        
        // FIFTH: Create our gameCoordinator (for multiplayer servers).
        if ( !initializeAndActivateGameCoordinator( gip ) ) {
        	// TODO: Handle failure to set up game coordinator.
        }
        // After this call, either gameCoordinator is null, or it is ready 
        // to start().
        
        // STEP FIVE POINT FIVE:
        // Server communication failure handlers and Runnables
        if ( gip.isMatchseeker() )
        	allocateAndInitializePeaceRayServerCommunicationHandlers( gip ) ;
        
        // SIXTH: Initialize all our itty-bitty variables.
        initializeGameMetaData( gip ) ;
        
        // If we didn't load a game, now's a good time to set up a checkpoint.
        // However, we have noticed a bug based on load/save behavior:
        // We do not save a New Game unless at least a score of 1 is acheived
        // This is to prevent saved games from being accidentally overwritten
        // by a mis-press of "new game" (the user can always quit with the "back"
        // button before a piece is dropped).
        //
        // However, we DO want to save a checkpoint right away, before the game
        // starts getting away from us.  We want this checkpoint to follow the
        // same model: if we end up keeping this game save, we want to keep the
        // checkpoint too.  If we end up deleting (i.e., not saving) this game,
        // we want the checkpoint to disappear - we ESPECIALLY do not want the
        // checkpoint to overwrite a checkpoint that may already exist for the
        // game save.
        //
        // There are two basic ways to handle this.  The first is for the GameService
        // to keep track of checkpointed Game data, in whatever form, and make the
        // decision AT SOME LATER POINT of whether to write this data to GameSaver.
        // The second is for GameSaver to directly support this behavior.  We choose
        // option 2.
        //
        // An "ephemeral checkpoint" functions exactly like a checkpoint for the purpose
        // of loading/saving checkpoints.  In fact, once an ephemeral checkpoint is
        // saved, there is no call to "loadEphemeralCheckpoint" - rather, "loadCheckpoint.."
        // will retrieve the ephemeral data.  However, the PREVIOUS checkpoint data is
        // still retained.  Then:
        //
        // If the game is LOADED all ephemeral checkpoints are destroyed, and the previous
        //		checkpoints are restored.
        // If the game is SAVED all ephemeral checkpoints are made permanent, overwriting
        //		any previous checkpoints.
        //
        // This behavior allows our later consideration for "do we save or not?" to determine
        // the permanance of the checkpoint saved at this moment.
        if ( gip.saveToKey != null && gip.loadFromKey == null ) {
        	saveEphemeralCheckpoint(gip.saveToKey) ;
        }
    }

	/**
	 * Provides a reference to the GameUserInterface (probably a GameActivity)
	 * to the Service.  This is a necessary call if you expect anything to happen
	 * on screen!
	 */
	public synchronized void setGameUserInterface( Activity activity, GameUserInterface gui ) {
		if ( this.activity != activity )
			return ;
		
		Log.d(TAG, "setGameUserInterface") ;
		
		gameUserInterface = gui ;
	}
	
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onCreate method.
	 */
	public synchronized void gameActivityDidCreate(Activity activity) {
		if ( this.activity != activity )
			return ;
		
		Log.d(TAG, "gameActivityDidCreate") ;
		// Here in didCreate, we start our communications going.
		// We obviously don't tell the Activity to start advancing stuff, though...
		
		// If we should NOT stay in the background, then now is the time to
		// start.  Otherwise, if we should, we will start in onStart (when we
		// are no longer in the background).
		if ( !shouldStayInBackground || alwaysForeground ) {
			if ( gameCoordinator != null )
				gameCoordinator.start() ;
			if ( gameMaintainer != null )
				gameMaintainer.start() ;
			
			if ( clientCommunications != null )
				clientCommunications.start() ;
		}
		
		// Note state change
		activityState = ACTIVITY_STATE_CREATED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onStart method.
	 */
	public synchronized void gameActivityDidStart(Activity activity) {
		if ( this.activity != activity )
			return ;
		
		Log.d(TAG, "gameActivityDidStart") ;
		// 'start' implies that the Activity is on the screen, but
		// possibly paused.  Now is a good time to un-foreground our
		// Service - if we are Foregrounded, that is.
		
		if ( (shouldStayInBackground && !alwaysForeground) && gameState != GAME_STATE_OVER && !resolving ) {
			// Start things going.
			if ( clientCommunications != null )
				clientCommunications.start() ;
			if ( gameCoordinator != null )
				gameCoordinator.start() ;
			if ( gameMaintainer != null )
				gameMaintainer.start() ;
		}
		
		// If in the foreground, stop being in the foreground.
		this.toggleForeground(false) ;
		
		
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameActivityStart(gip.gameSettings.getMode(), (shouldStayInBackground && !alwaysForeground)) ;
		
		// Note state change
		activityState = ACTIVITY_STATE_STARTED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onResume method.
	 */
	public synchronized void gameActivityDidResume(Activity activity) {
		if ( this.activity != activity )
			return ;
		
		Log.d(TAG, "gameActivityDidResume") ;
		
		// If the game has resumed, it is ready to start things moving
		// again.  Now's a good time to unpause the game... IF
		// we paused it before!
		
		// If we have no reason to pause the game, tell the 
		// server to unpause it.  For now, the only way to pauseb
		// is to leave the activity.
		if ( !pausedByPlayer && clientCommunications != null )
			clientCommunications.sendPauseRequest(false) ;

		// If the game state is GO, tell the GUI to start the game.
		// Compare against the Service callback for the Activity resuming.
		// RACE CONDITION: Here we check GAME_STATE_GO and set activityState.
		// In 'ccd_messageGo' we check activityState and set GAME_STATE_GO.
		// If we check-then-set, there is a race condition which allows
		// both variables to be set in a "game should advance" state, without
		// the call to gui_toggleAdvancingGames ever being made.
		// Therefore we note the state change first, then unpause if appropriate.
		activityState = ACTIVITY_STATE_RESUMED ;
		if ( gameState == GAME_STATE_GO && !resolving ) {
			if ( gameUserInterface != null )
				gameUserInterface.gui_toggleAdvancingGames(true) ;
			getGameResultBuilder().setGameGo() ;
		}
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onPause method.
	 */
	public synchronized void gameActivityDidPause(Activity activity) {
		if ( this.activity != activity )
			return ;
		
		Log.d(TAG, "gameActivityDidPause") ;
		
		// The Activity is on-screen but partially obscured.
		// Dunno what causes this... a pop-up?
		
		// Tell the server we want to pause.
		if ( clientCommunications != null )
			clientCommunications.sendPauseRequest(true) ;
		
		// Try saving here, but only if someone has > 0 points.
		long maxScore = 0 ;
		for ( int i = 0; i < ginfo.length; i++ )
			maxScore = Math.max(ginfo[i].score, maxScore) ;
		if ( gip.saveToKey != null && maxScore > 0 && gameState != GAME_STATE_OVER && saveOnQuit )
			this.saveGame(gip.saveToKey) ;
			// Only save if we have not lost.
		
		// Note state change
		activityState = ACTIVITY_STATE_STARTED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onStop method.
	 */
	public synchronized void gameActivityDidStop(Activity activity) {
		if ( this.activity != activity )
			return ;
		
		Log.d(TAG, "gameActivityDidStop") ;
		// The activity has left the screen.
		
		// Make this Service a Foreground service - IF we are multiplayer.
		// Otherwise, suspend all the operations we're doing (kill threads, etc.)
		// so we don't place an unnecessary burden on the phone.
		if ( shouldStayInBackground && !alwaysForeground ) {
			// Single player, or quitting.
			if ( clientCommunications != null )
				clientCommunications.stop();
			if ( gameCoordinator != null )
				gameCoordinator.stop() ;
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
		}
		else
			this.toggleForeground(true) ;
			// Go foreground - probably multiplayer with 'home' just pressed.
		
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameActivityStop(gip.gameSettings.getMode(), (shouldStayInBackground && !alwaysForeground) ) ;
		
		// When the activity is stopped, the gameView won't be running.
		// We don't need to tell it not to tick forward; we reserve that for
		// reports from the ClientCommunicator.
		
		// Note state change
		activityState = ACTIVITY_STATE_CREATED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onDestroy method.
	 */
	public synchronized void gameActivityDidDestroy(Activity activity) {
		if ( this.activity != activity )
			return ;
		
		// Release the hounds!  I mean, lock.
		this.getLocksAndReleaseIfHeld() ;
		
		Log.d(TAG, "gameActivityDidDestroy") ;
		// Activity destroyed!
		// Should we do any clean-up here?  We expect to be unbound
		// very shortly, maybe we can handle that in our unbind method...
		
		if ( !alwaysForeground ) {
			// Remove our notification.
			this.toggleForeground(false) ;
			
			// At the very least, we should disconnect everything and shut down
			// sockets / threads.
			// Stop() calls are safe so long as we have start()ed at least once,
			// which we have (we have to to even get here, based on how the
			// Activity calls these methods).
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			if ( gameCoordinator != null )
				gameCoordinator.stop() ;
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
		}
		
		// unlink
		if ( actionAdapter != null ) {
			for ( int i = 0; i < actionAdapter.length; i++ ) {
				ActionAdapter aa = actionAdapter[i] ;
				if ( aa != null ) {
					aa.setGameActionListener(null) ;
					Game g = game[i] ;
					if ( g != null )
						g.setActionAdapter(null) ;
					if ( clientCommunications != null )
						clientCommunications.setActionAdapter(null, i) ;
				}
			}
		}
		
		recycle() ;
	}
	
	
	public synchronized void gameActivityQuitting(Activity activity) {
		if ( this.activity != activity )
			return ;
		
		if ( alwaysForeground ) {
			// Release the hounds!  I mean, lock.
			this.getLocksAndReleaseIfHeld() ;
			
			Log.d(TAG, "gameActivityQuitting") ;
			shouldStayInBackground = true ;
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			if ( gameCoordinator != null )
				gameCoordinator.stop() ;
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
			
			Log.d(TAG, "toggle foreground") ;
			this.toggleForeground(false) ;
		}
		
		// try closing the game.
		if ( internetLobbyGame != null && internetLobbyGame.getEditKey() != null )
			new ThreadCloseGame( internetLobbyGame ).start() ;
		
		// unlink
		if ( actionAdapter != null ) {
			for ( int i = 0; i < actionAdapter.length; i++ ) {
				ActionAdapter aa = actionAdapter[i] ;
				if ( aa != null ) {
					aa.setGameActionListener(null) ;
					Game g = game[i] ;
					if ( g != null )
						g.setActionAdapter(null) ;
					if ( clientCommunications != null )
						clientCommunications.setActionAdapter(null, i) ;
				}
			}
		}
	}
	
	
	public synchronized GameResult gameActivityFinishing(Activity activity) {
		if ( this.activity != activity )
			return null ;
		
		if ( !alwaysForeground ) {
			// Release the hounds!  I mean, lock.
			this.getLocksAndReleaseIfHeld() ;
			
			Log.d(TAG, "gameActivityFinishing") ;
			shouldStayInBackground = true ;
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			if ( gameCoordinator != null )
				gameCoordinator.stop() ;
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
			
			Log.d(TAG, "toggle foreground") ;
			this.toggleForeground(false) ;
		}
		
		// Try saving here, but only if someone has > 0 points.
		Log.d(TAG, "trying to save") ;
		long maxScore = 0 ;
		for ( int i = 0; i < ginfo.length; i++ )
			maxScore = Math.max(ginfo[i].score, maxScore) ;
		boolean falseStart = true ;
		if ( maxScore == 0 && game != null )
			for ( int i = 0; i < game.length; i++ )
				falseStart = falseStart 
						&& ( game[i] == null || game[i].s == null || game[i].s.numActionCycles == 0 ) ;
		if ( gip.saveToKey != null && ( maxScore > 0 || !falseStart ) && gameState != GAME_STATE_OVER && saveOnQuit )
			this.saveGame(gip.saveToKey) ;
		
		GameResult gr = getGameResult() ;
		
		// Tell the achievements.
		Achievements.game_over(gr, playerSlot) ;
		
		Log.d(TAG, "recycling") ;
		recycle() ;
		
		return gr ;
	}
	
	
	
	synchronized private void recycle() {
		// no more games
		for ( int i = 0; i < numberOfPlayers; i++ ) {
			game[i] = null ;
			ginfo[i] = null ;
			gevents[i] = null ;
			
			actionAdapter[i] = null ;
		}
		
		if ( clientCommunications != null )
			clientCommunications.setDelegate(null) ;
		
		activity = null ;
		gameUserInterface = null ;
		
		if ( notificationAdapter != null ) {
			notificationAdapter.dismissForegroundNotification() ;
			notificationAdapter.setContext(null) ;
			notificationAdapter = null ;
		}
	}
	
	public boolean getPlayerWon() {
		return getPlayerWon(playerSlot) ;
	}
	
	public boolean getPlayerLost() {
		return getPlayerLost(playerSlot) ;
	}
	
	public boolean getPlayerSpectating() {
		return getPlayerSpectating(playerSlot) ;
	}
	
	public boolean getPlayerWon(int playerSlot) {
		return get( playerSlot, playerWon ) ;
	}
	
	public boolean getPlayerLost(int playerSlot) {
		return get( playerSlot, playerLost ) ;
	}
	
	public boolean getPlayerQuit(int playerSlot) {
		return get( playerSlot, playerQuit ) ;
	}
	
	public boolean getPlayerKicked(int playerSlot) {
		return get( playerSlot, playerKicked ) ;
	}
	
	public boolean getPlayerSpectating(int playerSlot) {
		return get( playerSlot, playerSpectating ) ;
	}
	
	public boolean get( int index, boolean [] ar ) {
		return ar == null || index < 0 || index >= ar.length ? false : ar[index] ;
	}
	
	
	/**
	 * Returns the current GameResultBuilder instance.  If none exists, makes
	 * a new one.
	 * 
	 * @return
	 */
	synchronized private GameResult.Builder getGameResultBuilder() {
		if ( grbuilder_private == null ) {
	        grbuilder_private = new GameResult.Builder() ;
	        grbuilder_private.setNumberOfPlayers(numberOfPlayers)
	        		 .setNames(playerNames)
	        		 .setGameIdle()
	        		 .setNonce(gip.nonce) ;
    	}
		return grbuilder_private ;
	}
	
	
	synchronized public GameResult getGameResult() {
		GameResult.Builder grbuilder = getGameResultBuilder() ;
		grbuilder.setGameInformation(ginfo) ;
		for ( int i = 0; i < numberOfPlayers; i++ ) {
			int r = game[i].R();
			int c = game[i].C();
			byte [][][] bfield = new byte[2][r][c] ;
			byte [][][] pbfield = new byte[2][r][c] ;
			boolean hasPiece = game[i].copySimplifiedBlockField(bfield, pbfield, 0, 0) ;
			grbuilder.setBlockField(i, bfield) ;
			grbuilder.setPieceBlockField(i, hasPiece ? game[i].getCurrentPieceType() : -1, pbfield) ;
			grbuilder.setTotalTimeInGameTicks(i, game[i].getTotalMillisecondsTicked()) ;
			if ( gameSettings != null )
				grbuilder.setGameSettings(i, gameSettings) ;
		}
		
		grbuilder.setDateEnded() ;
		return grbuilder.build() ;
	}
	
	synchronized public GameResult getCheckpointResult() {
		String loadKey = gip.saveToKey ; 	// gip.loadFromKey is only present for loaded games
		GameResult gr = GameSaver.loadCheckpointResult(this, loadKey, 0) ;
		return gr ;
	}
	
	synchronized public GameSettings getGameSettings() {
		return gameSettings ;
	}
	
	
    /////////////////////////////////////////////////////////////////////////
    //
    // SAVING / LOADING GAMES
    //
    ////////////////////////////////////////////////////////////////////////// 
    
    
    /**
     * Attempts to load game states (into games which have already
     * been allocated and initialized) using the provided key.
     * 
     * Returns whether the load was successful.
     * 
     * @param loadFromKey
     * @return
     */
	synchronized private boolean loadGame( String loadFromKey ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameLoad(gip.gameSettings.getMode(), false) ;
			
		synchronized( game[0] ) {
	    	boolean success = true ;
			success = success && GameSaver.loadGameStates(this, loadFromKey, game[0]) ;
			game[0].refresh() ;
			
			// Achievements need to know this.
			if ( success ) {
				Achievements.game_load() ;
			}
			
			if ( GameSaver.hasGameSettings(this, loadFromKey) )
				gameSettings = GameSaver.loadGameSettings( this, loadFromKey ) ;
			GameResult gr = GameSaver.loadGameResult(this, loadFromKey) ;
			if ( success && gr != null ) {
				Log.d(TAG, "setting grbuilder to continue previous result") ;
				grbuilder_private = new GameResult.Builder(gr) ;
			}
			else {
				Log.d(TAG, "couldn't load grbuilder") ;
				success = false ;
			}
	    	return success ;
		}
    }
    
    /**
     * Attempts to save game states using the provided key.
     * 
     * Returns whether the save was successful.
     * 
     * @param saveToKey
     * @return
     */
	synchronized private boolean saveGame( String saveToKey ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSave(gip.gameSettings.getMode(), false) ;
		
    	boolean success = true ;
    	
    	// If overwriting a DIFFERENT game (i.e. with a different start time), put that
    	// game in our records.
    	GameResult prevGR = GameSaver.loadGameResult(this, saveToKey) ;
    	GameResult gr = getGameResult() ;
    	if ( prevGR != null && !prevGR.getDateStarted().equals(gr.getDateStarted()) ) 
    		GameStats.DatabaseAdapter.addGameResultToDatabase(this, prevGR) ;
    	
    	success = success && GameSaver.saveGame(this, saveToKey, game[0], gr, gameSettings,
    			gameUserInterface == null ? null : gameUserInterface.gui_getGameThumbnail()) ;
    	if ( gameUserInterface != null )
    		gameUserInterface.gui_didSaveGame() ;
    	return success ;
    }
	
	/**
	 * Attempts to save game states using the provided key.
	 * 
	 * This attempt is performed asynchronously; i.e., it uses a different 
	 * thread to perform the actual saves.  The provided listener and tag
	 * is passed to a GameSaver instance; 'null' may be safely passed.
	 *
	 * This method uses the actual Game object, NOT a copy.  For that reason,
	 * it is highly recommended that you attach a Listener and avoid altering
	 * the current game object until it returns.
	 * 
	 * @param saveToKey
	 * @param listener
	 * @param tag
	 */
	synchronized private void saveGameAsynchronously( String saveToKey, GameSaver.Listener listener, Object tag ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSave(gip.gameSettings.getMode(), true) ;
		
		// If overwriting a DIFFERENT game (i.e. with a different start time), put that
    	// game in our records.
    	GameResult prevGR = GameSaver.loadGameResult(this, saveToKey) ;
    	GameResult gr = getGameResult() ;
    	if ( prevGR != null && !prevGR.getDateStarted().equals(gr.getDateStarted()) ) 
    		GameStats.DatabaseAdapter.addGameResultToDatabase(this, prevGR) ;
    	
    	// Asynchronous save.
    	// TODO: add thumbnail?
    	new GameSaver()
    			.start(this, listener, tag)
    			.saveGame(saveToKey, game[0], gr, gameSettings,
    					gameUserInterface == null ? null : gameUserInterface.gui_getGameThumbnail())
    			.stop() ;
    	if ( gameUserInterface != null )
    		gameUserInterface.gui_didSaveGame() ;
    	// inform the GUI immediately.
	}
	
	/**
	 * Attempts to save game states using the provided key.
	 * 
	 * This attempt is performed asynchronously; i.e., it uses a different 
	 * thread to perform the actual saves.  The provided listener and tag
	 * is passed to a GameSaver instance; 'null' may be safely passed.
	 *
	 * This method will (in this thread) make a copy of the current game state
	 * before beginning its asynchronous work.  Once this method returns,
	 * you may safely restart any game operation (including those which
	 * alter game state) without interfering with the save.
	 * 
	 * For that reason, it is safe to call this method with 'null'
	 * listener and proceed immediately after it returns, as long as
	 * to changes happen to the Game object during this method call.
	 * 
	 * @param saveToKey
	 * @param listener
	 * @param tag
	 */
	synchronized private void saveGameCopyAsynchronously( String saveToKey, GameSaver.Listener listener, Object tag ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSave(gip.gameSettings.getMode(), true) ;
		
		// If overwriting a DIFFERENT game (i.e. with a different start time), put that
    	// game in our records.
    	GameResult prevGR = GameSaver.loadGameResult(this, saveToKey) ;
    	GameResult gr = getGameResult() ;
    	if ( prevGR != null && !prevGR.getDateStarted().equals(gr.getDateStarted()) ) 
    		GameStats.DatabaseAdapter.addGameResultToDatabase(this, prevGR) ;
    	
    	// Asynchronous save.
    	// TODO: add thumbnail?
    	new GameSaver()
    			.start(this, listener, tag)
    			.saveGame(saveToKey, new BrokenGameCopy(game[0]), gr, gameSettings,
    					gameUserInterface == null ? null : gameUserInterface.gui_getGameThumbnail())
    			.stop() ;
    	if ( gameUserInterface != null )
    		gameUserInterface.gui_didSaveGame() ;
	}
	
	
	
	
	/**
	 * Loads the specified checkpoint.  If 'rewind,' the checkpoint is applied as a "rewind"
	 * to the current GameResult.Builder.  Otherwise, it is fully loaded and the previous
	 * result is overwritten.
	 * 
	 * We expect that Progression games will 'rewind,' while Endurance games will overwrite.
	 * 
	 * @param loadFromKey
	 * @param rewind
	 * @return
	 */
	synchronized private boolean loadCheckpoint( String loadFromKey, boolean rewind ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameLoadCheckpoint(gip.gameSettings.getMode(), false) ;
		
		synchronized( game[0] ) {
			boolean success = true ;
			success = success && GameSaver.loadCheckpointStates(this, loadFromKey, 0, game[0]) ;
			game[0].refresh() ;
			
			// Achievements need to know this.
			if ( success ) {
				Achievements.game_load() ;
			}
			
			GameResult gr = GameSaver.loadCheckpointResult(this, loadFromKey, 0) ;
			if ( success && gr != null ) {
				if ( rewind )
					getGameResultBuilder().rewind(gr) ;
				else
					grbuilder_private = new GameResult.Builder(gr) ;
			}
			else 
				success = false ;
	    	return success ;
		}
	}
	
	
	/**
	 * Attempts to save game states using the provided key as checkpoint 0.
	 * 
	 * Returns whether the save was successful.
	 * 
	 * @param saveToKey
	 * @return
	 */
	synchronized private boolean saveCheckpoint( String saveToKey ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSaveCheckpoint(gip.gameSettings.getMode(), false, false) ; // is not ephemeral, is not asynchronous
		
		boolean success = true ;
		success = success && GameSaver.saveCheckpoint(this, saveToKey, 0, game[0], getGameResult()) ;
		return success ;
	}
	
	/**
	 * Attempts to save game states using the provided key as checkpoint 0.
	 * 
	 * Saves occur asynchronously.  Provide a Listener (and tag) if you are
	 * interested in the result.
	 * 
	 * Finally, although this method is asynchronous, it reads the game object
	 * directly after returning.  It is highly recommended that you provide
	 * a Listener and only perform changes to the Game object once the Listener
	 * is told that this operation is complete.
	 * 
	 * @param saveToKey
	 */
	synchronized private void saveCheckpointAsynchronously( String saveToKey, GameSaver.Listener listener, Object tag ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSaveCheckpoint(gip.gameSettings.getMode(), false, true) ; // is not ephemeral, is asynchronous
		
		
		new GameSaver()
				.start( this, listener, tag )
				.saveCheckpoint(saveToKey, 0, game[0], getGameResult())
				.stop() ;
	}
	
	
	/**
	 * Attempts to save game states using the provided key as checkpoint 0.
	 * 
	 * Saves occur asynchronously.  Provide a Listener (and tag) if you are
	 * interested in the result.
	 * 
	 * In this thread, a copy is made of the game object.  It is that copy
	 * which is saved.  Therefore, after this method returns, it is safe to
	 * make changeds to the Game object regardless of whether the Listener
	 * has been informed of our completion.
	 * 
	 * @param saveToKey
	 */
	synchronized private void saveCheckpointCopyAsynchronously( String saveToKey, GameSaver.Listener listener, Object tag ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSaveCheckpoint(gip.gameSettings.getMode(), false, true) ; // is not ephemeral, is asynchronous
		
		new GameSaver()
				.start( this, listener, tag )
				.saveCheckpoint(saveToKey, 0, new BrokenGameCopy( game[0] ), getGameResult())
				.stop() ;
	}
	
	
	/**
	 * Attempts to save game states using the provided key as checkpoint 0.
	 * 
	 * Returns whether the save was successful.
	 * 
	 * @param saveToKey
	 * @return
	 */
	synchronized private boolean saveEphemeralCheckpoint( String saveToKey ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSaveCheckpoint(gip.gameSettings.getMode(), true, false) ; // is ephemeral, is not asynchronous
		
		boolean success = true ;
		success = success && GameSaver.saveEphemeralCheckpoint(this, saveToKey, 0, game[0], getGameResult()) ;
		return success ;
	}
	
	/**
	 * Attempts to save game states using the provided key as checkpoint 0.
	 * 
	 * Saves occur asynchronously.  Provide a Listener (and tag) if you are
	 * interested in the result.
	 * 
	 * Finally, although this method is asynchronous, it reads the game object
	 * directly after returning.  It is highly recommended that you provide
	 * a Listener and only perform changes to the Game object once the Listener
	 * is told that this operation is complete.
	 * 
	 * @param saveToKey
	 */
	synchronized private void saveEphemeralCheckpointAsynchronously( String saveToKey, GameSaver.Listener listener, Object tag ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSaveCheckpoint(gip.gameSettings.getMode(), true, true) ; // is ephemeral and asynchronous
		
		new GameSaver()
				.start( this, listener, tag )
				.saveEphemeralCheckpoint(saveToKey, 0, game[0], getGameResult())
				.stop() ;
	}
	
	
	/**
	 * Attempts to save game states using the provided key as checkpoint 0.
	 * 
	 * Saves occur asynchronously.  Provide a Listener (and tag) if you are
	 * interested in the result.
	 * 
	 * In this thread, a copy is made of the game object.  It is that copy
	 * which is saved.  Therefore, after this method returns, it is safe to
	 * make changeds to the Game object regardless of whether the Listener
	 * has been informed of our completion.
	 * 
	 * @param saveToKey
	 */
	synchronized private void saveEphemeralCheckpointCopyAsynchronously( String saveToKey, GameSaver.Listener listener, Object tag ) {
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInGameSaveCheckpoint(gip.gameSettings.getMode(), true, true) ; // is ephemeral and asynchronous
		
		
		new GameSaver()
				.start( this, listener, tag )
				.saveEphemeralCheckpoint(saveToKey, 0, new BrokenGameCopy( game[0] ), getGameResult())
				.stop() ;
	}
	
    
	
	synchronized public void readStateFromBundle( Bundle inState ) {
		// read the game itself
    	readGameFromBundle( inState ) ;
    	
    	// GAME ACTIVITY
    	// The GameActivity will give us a reference to itself; we store as 
    	// an instance of the GameUserInterface interface.
    	pausedByPlayer = inState.getBoolean("GameService.pausedByPlayer") ;
    	pausedByDemoExpired = inState.getBoolean("GameService.pausedByDemoExpired") ;
    	
    	shouldStayInBackground = inState.getBoolean("GameService.shouldStayInBackground") ;
    	alwaysForeground = inState.getBoolean("GameService.alwaysForeground") ;
    	
    	// Player info
    	numberOfPlayers = inState.getInt("GameService.numberOfPlayers") ;
    	playerSlot = inState.getInt("GameService.playerSlot") ;
    	playerNames = inState.getStringArray("GameService.playerNames") ;
    	
    	gameState = inState.getInt("GameService.gameState") ;
    	gameStatePlayers = inState.getBooleanArray("GameService.gameStatePlayers") ;
    	resolving = inState.getBoolean("GameService.resolving") ;
    	
    	saveOnQuit = inState.getBoolean("GameService.saveOnQuit") ;
	}
	
    /**
     * Reads game objects from the provided bundle.
     * 
     * @param inState
     */
    synchronized public void readGameFromBundle( Bundle inState ) {
    	for ( int i = 0; i < numberOfPlayers; i++ ) {
	    	ginfo[i].setStateAsSerializable( inState.getSerializable("ginfo_" + i) ) ;
	    	gevents[i].setStateAsSerializable( inState.getSerializable("gevents_" + i) ) ;
	    	game[i].setStateAsSerializable( inState.getSerializable("game_" + i) ) ;
	    	
	    	int numSystems = Game.numSystems() ;
	    	Serializable [] systemArray = new Serializable[numSystems] ;
	    	for ( int j = 0; j < numSystems; j++ ) {
	    		systemArray[j] = inState.getSerializable("system_" + i + "_" + j) ;
	    	}
	    	game[i].setSystemsFromSerializables( systemArray ) ;
	    	game[i].setPseudorandom(gip.nonce.smallInt()) ;
	
	    	// And of course, refresh
	    	game[i].refresh() ;
    	}
    	// Finally, current game result.
    	grbuilder_private = new GameResult.Builder( (GameResult) inState.getSerializable("gresult") ) ;
    }
    
    
    synchronized public void writeStateToBundle( Bundle outState ) {
    	// write the game itself
    	writeGameToBundle( outState ) ;
    	
    	// GAME ACTIVITY
    	// The GameActivity will give us a reference to itself; we store as 
    	// an instance of the GameUserInterface interface.
    	outState.putBoolean("GameService.pausedByPlayer", pausedByPlayer) ;
    	outState.putBoolean("GameService.pausedByDemoExpired", pausedByDemoExpired) ;
    	
    	outState.putBoolean("GameService.shouldStayInBackground", shouldStayInBackground) ;
    	outState.putBoolean("GameService.alwaysForeground", alwaysForeground) ;
    	
    	// Player info
    	outState.putInt("GameService.numberOfPlayers", numberOfPlayers) ;
    	outState.putInt("GameService.playerSlot", playerSlot) ;
    	outState.putStringArray("GameService.playerNames", playerNames) ;
    	
    	outState.putInt("GameService.gameState", gameState) ;
    	outState.putBooleanArray("GameService.gameStatePlayers", gameStatePlayers) ;
    	outState.putBoolean("GameService.resolving", resolving) ;
    	
    	outState.putBoolean("GameService.saveOnQuit", saveOnQuit) ;
    }
    
    /**
     * Writes game objects to the provided bundle.
     * 
     * @param outState
     */
    synchronized public void writeGameToBundle( Bundle outState ) {
    	// TODO: Advance the game!
    	// TODO: Determine if there is any significant advantage to putting
    	// the game in a "stable" state before saving, now that all info will
    	// be retained.
    	
    	for ( int i = 0; i < numberOfPlayers; i++ ) {
	    	outState.putSerializable("ginfo_" + i, ginfo[i].getStateAsSerializable()) ;
	    	outState.putSerializable("gevents_" + i, gevents[i].getStateAsSerializable()) ;
	    	outState.putSerializable("game_" + i, game[i].getStateAsSerializable()) ;
	    	
	    	Serializable [] systemArray = game[i].getSerializablesFromSystems() ;
	    	for ( int j = 0; j < systemArray.length; j++ )
	    		outState.putSerializable("system_" + i + "_" + j, systemArray[j]) ;
    	}
    	outState.putSerializable("gresult", getGameResult()) ;
    }
    
    
    /////////////////////////////////////////////////////////////////////////
    //
    // PAUSING / RESUMING GAMES
    //
    ////////////////////////////////////////////////////////////////////////// 
    
    
    /**
     * Called from the GameActivity or a View.  The player is trying to pause
     * the game.  If successful, the gui_ will be notified (eventually) through
     * its delegate method.
     */
    synchronized public void pauseByPlayer() {
    	pausedByPlayer = true ;
    	if ( clientCommunications != null )
    		clientCommunications.sendPauseRequest(true) ;
    }
    
    /**
     * Called from the GameActivity or a View.  The player is trying to unpause
     * the game.  If successful, the gui_ will be notified (eventually) through
     * its delegate method.
     */
    synchronized public void unpauseByPlayer() {
    	pausedByPlayer = false ;
    	if ( activityState == ACTIVITY_STATE_RESUMED && !pausedByDemoExpired
    			&& clientCommunications != null ) {
    		clientCommunications.sendPauseRequest(false) ;
    	}
    }
    
    
    synchronized public boolean isPausedByPlayer() {
    	return pausedByPlayer ;
    }
    
    
    synchronized public void pauseByTimedDemoExpired() {
    	pausedByDemoExpired = true ;
    	if ( clientCommunications != null )
    		clientCommunications.sendPauseRequest(true) ;
    }
    
    synchronized public void unpauseByDemoExpired() {
    	pausedByDemoExpired = false ;
    	if ( activityState == ACTIVITY_STATE_RESUMED && !pausedByPlayer
    			&& clientCommunications != null )
    		clientCommunications.sendPauseRequest(false) ;
    }
    
    synchronized public boolean isPausedByTimedDemoExpired() {
    	return pausedByDemoExpired ;
    }
    
    
    /////////////////////////////////////////////////////////////////////////
    //
    // RESOLVING GAME STATE CHANGES
    //
    // Sometimes we tell the GUI to resolve a game result.  If the resolution
    // is to quit, the GUI handles that for us, so there is no corresponding
    // Service callback.  Otherwise, we tell the Service what must be done,
    // and go from there.
    //
    ////////////////////////////////////////////////////////////////////////// 
    
    
    /**
     * The player has resolved to "continue," the meaning of which differs
     * between circumstances, but (for example) it may involve going up
     * a level and merely continuing from the next level, possibly saving
     * another checkpoint or reseting certain values.
     */
    public void resolvedToContinue() {
    	Log.d(TAG, "resolvedToContinue") ;
    	// This is called by the "Activity Intent Returned With Result" thread.
    	// Handling the actual code directly here causes a skip in the music.
    	// We use a Runnable instead.
    	mHandler.post( new ResolvedToContinueRunnable() ) ;
    }
    
    private class ResolvedToContinueRunnable implements Runnable {
    	@Override
    	public void run() {
    		resolving = false ;
        	saveOnQuit = true ;
        	// we set a checkpoint here and set to aTickin'.
        	saveCheckpointCopyAsynchronously(gip.saveToKey, null, null) ;
        	
        	if ( gameUserInterface != null )
				gameUserInterface.gui_continueAfterResolvingResult() ;
        	for ( int i = 0; i < actionAdapter.length; i++ )
        		actionAdapter[i].set_gameShouldTick(true) ;
        	
        	// start up client communications.
        	if ( clientCommunications != null )
        		clientCommunications.start() ;
        	// no need to start a GameCoordinator; this is necessarily a
        	// one player game.
    	}
    }
    
    /**
     * The player has resolved to "replay" from the start.  The previous
     * game is over and should possibly be included in our records.
     * ASSUMPTION: Single player game.
     */
    public void resolvedToReplay() {
    	Log.d(TAG, "resolvedToReplay") ;
    	mHandler.post( new ResolvedToReplayRunnable() ) ;
    }
    
    private class ResolvedToReplayRunnable implements Runnable {
    	@Override
    	public void run() {
    		resolving = false ;
        	saveOnQuit = true ;
        	// empty action adapters
        	for ( int i = 0; i < actionAdapter.length; i++ )
        		actionAdapter[i].emptyAllQueues() ;
        	
        	// Note that "save game" will automatically add the previous game
        	// to our records if the start dates don't match.  To prevent double
        	// or non-Recording, do some manual updates here.
        	
        	GameResult.Builder grbuilder = getGameResultBuilder() ;
        	
        	// First, save this game (the one that ended and which we are now replaying)
        	// to our records.
        	grbuilder.terminate();
        	GameResult gr = getGameResult() ;
        	GameStats.DatabaseAdapter.addGameResultToDatabase(GameService.this, gr) ;
        	
        	// Second, it is possible that this was a NEW GAME and a previously saved
        	// game exists.  Load the save, compare start dates, and if necessary
        	// add THAT GAME to the records as well.  NOTE: we will shortly be deleting
        	// that game.
        	GameResult oldGR = GameSaver.loadGameResult(GameService.this, gip.saveToKey) ;
        	if ( oldGR != null && !oldGR.getDateStarted().equals(gr.getDateStarted()) )
        		GameStats.DatabaseAdapter.addGameResultToDatabase(GameService.this, oldGR) ;
        	
        	// Third, load our checkpoint.  We are not rewinding so this overwrites our
        	// previous grbuilder.
        	if ( !loadCheckpoint( gip.saveToKey, false ) )	// NOT rewinding.
        		Log.e(TAG, "couldn't load checkpoint!") ;
        	
        	// Fourth, delete the saved game we've been using.  We don't want to accidentally
        	// re-Record it later.
        	if ( !GameSaver.deleteGame(GameService.this, gip.saveToKey) )
        		Log.e(TAG, "couldn't delete save!") ;
        	
        	// Fifth: we are starting a new game.  Reset the "date started" for our grbuilder.
        	grbuilder.setDateStarted() ;
        	
        	// Sixth: we need to checkpoint right now.
        	saveCheckpoint(gip.saveToKey) ;
        	
        	// tell the activity to undo its resolution changes
        	if ( gameUserInterface != null )
				gameUserInterface.gui_continueAfterResolvingResult() ;
        	// Tell the ActionAdapters to tick
        	for ( int i = 0; i < actionAdapter.length; i++ )
        		actionAdapter[i].set_gameShouldTick(true) ;
        	
        	// start up client communications.
        	if ( clientCommunications != null )
        		clientCommunications.start() ;
        	// no need to start a GameCoordinator; this is necessarily a
        	// one player game.
    	}
    }
    
    
    /**
     * The player has resolved to "rewind" from a checkpoint.  We do not add
     * this game to our records; it is still ongoing.
     * 
     * ASSUMPTION: Single player game.
     */
    public void resolvedToRewind() {
    	Log.d(TAG, "resolvedToRewind") ;
    	mHandler.post( new ResolvedToRewindRunnable() ) ;
    }
    
    
    private class ResolvedToRewindRunnable implements Runnable {
    	@Override
    	public void run() {
    		resolving = false ;
        	saveOnQuit = true ;
        	// empty action adapters
        	for ( int i = 0; i < actionAdapter.length; i++ )
        		actionAdapter[i].emptyAllQueues() ;
        	// load our checkpoint!
        	if ( !loadCheckpoint( gip.saveToKey, true ) )	// rewinding
        		Log.d(TAG, "couldn't load checkpoint!") ;
        	// tell the activity to undo its resolution changes
        	if ( gameUserInterface != null )
				gameUserInterface.gui_continueAfterResolvingResult() ;
        	// Tell the ActionAdapters to tick
        	for ( int i = 0; i < actionAdapter.length; i++ )
        		actionAdapter[i].set_gameShouldTick(true) ;
        	
        	// start up client communications.
        	if ( clientCommunications != null )
        		clientCommunications.start() ;
        	// no need to start a GameCoordinator; this is necessarily a
        	// one player game.
    	}
    }
    
    
	
	
	/*
	 * *************************************************************************
	 * 
	 * CLIENT COMMUNICATIONS DELEGATE METHODS
	 * 
	 * Note: we only start() our client communications AFTER the Activity has 
	 * been created and we have a reference to it.  It is safe to access this
	 * reference in all of the below.
	 * 
	 * *************************************************************************
	 */
	
	
	@Override
	synchronized public void ccd_connectionStatusChanged(ClientCommunications cc, MessagePassingConnection.Status connectionStatus) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_connectionStatusChanged to " + connectionStatus ) ;
		//new Exception().printStackTrace() ;
		
		// Our reaction depends on the previous and new status.
		// Transitioning "forward", from connecting to negotiating to
		// connected, is pretty straight-forward as to how we handle it.
		// Transitioning to failure states is also simple.
		
		// If our game was going, put our connection status changed, we should
		// probably pause.
		if ( gameState == GAME_STATE_GO ) {
			// Put ourselves in waiting state and tell the GUI to stop
			gameState = GAME_STATE_WAITING ;
			getGameResultBuilder().setGameIdle() ;
			if ( gameUserInterface != null )
				gameUserInterface.gui_toggleAdvancingGames(false) ;
		}
		
		// Here's some states.
		switch( connectionStatus ) {
		case NEVER_CONNECTED:
		case PENDING:
			if ( gameUserInterface != null )
				gameUserInterface.gui_connectionToServerConnecting() ;
			this.foregroundConnecting() ;
			break ;
			
		case CONNECTED:
			// When we connect, we start negotiating.  After we're welcomed,
			// the server will send a welcome message; that will prompt us
			// to tell the GUI that we've "connected" for real.
			if ( gameUserInterface != null )
				gameUserInterface.gui_connectionToServerNegotiating() ;
			this.foregroundNegotiating() ;
			break ;
			
		case FAILED:
			// Failed to connect.
			if ( gameUserInterface != null )
				gameUserInterface.gui_connectionToServerFailedToConnect() ;
			this.foregroundConnecting() ;
			break ;
			
		case BROKEN:
		case PEER_DISCONNECTED:
			// Connection broke.  This is an unexpected disconnect.
			// We expect that if the server warned us the connection would
			// be going down (e.g., if we were kicked or the server
			// is closing) that we would have disconnected ourselves.
			if ( gameUserInterface != null )
				gameUserInterface.gui_connectionToServerDisconnectedUnexpectedly() ;
			this.foregroundConnecting() ;
			break ;
			
		case DISCONNECTED:
			// This is a manual disconnect.  Does the GUI need to know?
			// probably not, but maybe add a Connecting() method call.
			break ;
		
		}
		
	}

	@Override
	synchronized public void ccd_connectionReceivedIllegalMessage(ClientCommunications cc) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_connectionReceivedIllegalMessage") ;
		
		// This basically means a broken connection.  The client will handle
		// reconnecting, but the GUI should probably be told.  BUT: we tell it
		// once the connection status changes, not right away.  This is just
		// an unexpected disconnection message.
	}

	@Override
	synchronized public void ccd_messageWelcomeToServer(ClientCommunications cc) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageWelcomeToServer") ;
		
		// We've been welcomed!  The GUI should know about this, because it
		// may want to dismiss a displayed message, i.e. "negotiating" should
		// go away.
		if ( gameUserInterface != null )
			gameUserInterface.gui_connectionToServerConnected() ;
		this.foregroundConnected() ;
	}

	@Override
	synchronized public void ccd_messageHost(ClientCommunications cc, int slot, String name) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageHost:" + slot + " " + name) ;
		
		// Put this info in the game result builder.
		GameResult.Builder grbuilder = getGameResultBuilder() ;
		if ( slot >= 0 ) {
			grbuilder.setHost(slot) ;
			if ( name != null )
				grbuilder.setName(slot, name) ;
		}
		if ( gameUserInterface != null )
			gameUserInterface.gui_updateHostName(name) ;
	}

	@Override
	synchronized public void ccd_messageTotalPlayerSlots(ClientCommunications cc, int slots) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageTotalPlayerSlots:" + slots) ;
		
		// We came into this knowing the number of players - 1 or 2.
		// This message / method is available for future >2 player games,
		// which might start with fewer players.
		// TODO: If adding game types with variable # of players, this message
		// suddenly become important.  Make changes here.
		getGameResultBuilder().setNumberOfPlayers(slots) ;
		if ( gameUserInterface != null )
			gameUserInterface.gui_updateNumberOfPlayers(slots) ;
	}

	@Override
	synchronized public void ccd_messageLocalPlayerSlot(ClientCommunications cc, int slot) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageLocalPlayerSlot:" + slot) ;
		
		// For us, player slot is largely a convenience (helps determine whether
		// a kick methed was meant for us, e.g.), but the GUI absolutely needs
		// this to determine which game should be directly controlled and
		// advanced.  TODO: If we add background game-ticks for multiplayer,
		// we will need this value to.  We retain and pass to the GUI.
		playerSlot = slot ;
		getGameResultBuilder().setLocalPlayer(slot) ;
		if ( gameUserInterface != null )
			gameUserInterface.gui_updatePlayerLocalSlot(slot) ;
	}

	@Override
	synchronized public void ccd_messageKick(ClientCommunications cc, int playerSlot, String msg) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageKick:" + playerSlot + ", " + msg) ;
		
		this.playerKicked[playerSlot] = true ;
		
		// This method is called when a kick MSG is received, regardless
		// of its target (us or some other player).  There are separate
		// GUI methods for each.
		if ( playerSlot == this.playerSlot ) {
			// WE were kicked.  SHUT DOWN EVERYTHING.
			gameState = GAME_STATE_OVER ;
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			
			// Tell the GUI.  Stopping the game is always safe, regardless
			// of previous state.
			if ( gameUserInterface != null ) {
				gameUserInterface.gui_toggleAdvancingGames(false) ;
				gameUserInterface.gui_connectionToServerKickedByServer(msg) ;
			}
			getGameResultBuilder().setGameIdle() ;
		}
		else {
			if ( this.gameState != GAME_STATE_OVER )
				getGameResultBuilder().setQuit(playerSlot) ;
			// SOMEONE ELSE was kicked.  All we need to do is tell the gui.
			if ( gameUserInterface != null )
				gameUserInterface.gui_gameStatePlayerKicked(playerSlot, msg) ;
		}
	}
	
	
	@Override
	synchronized public void ccd_messageKickWarning( ClientCommunications cc, int playerSlot, String msg, long atTime ) {
		if ( cc != clientCommunications )
			return ;
		
		// we don't actually need to do anything here, other than tell the gui.
		if ( gameUserInterface != null )
			gameUserInterface.gui_gameStatePlayerKickWarning(playerSlot, msg, atTime) ;
	}
	
	
	@Override
	synchronized public void ccd_messageKickWarningRetraction( ClientCommunications cc, int playerSlot ) {
		if ( cc != clientCommunications )
			return ;
		
		// just tell the GUI, let it handle this.
		if ( gameUserInterface != null )
			gameUserInterface.gui_gameStatePlayerKickWarningRetraction(playerSlot) ;
	}
	

	@Override
	synchronized public void ccd_messageQuit(ClientCommunications cc, int playerSlot, boolean fatal) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageQuit:" + playerSlot) ;
		
		// The specified player quit.
		// We don't necessarily assume this ends the game, but the GUI should
		// be told.
		playerQuit[playerSlot] = true ;
		if ( this.gameState != GAME_STATE_OVER )
			getGameResultBuilder().setQuit(playerSlot) ;
		if ( gameUserInterface != null )
			gameUserInterface.gui_gameStatePlayerQuit(playerSlot, fatal) ;
	}
	
	
	@Override
	synchronized public void ccd_messageWon(ClientCommunications cc, int playerSlot) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageWon:" + playerSlot) ;
		
		// The specified player quit.
		// We don't necessarily assume this ends the game, but the GUI should
		// be told.
		playerWon[playerSlot] = true ;
		if ( this.gameState != GAME_STATE_OVER )
			getGameResultBuilder().setWon(playerSlot) ;
		if ( gameUserInterface != null )
			gameUserInterface.gui_gameStatePlayerWon(playerSlot) ;
	}
	
	
	@Override
	synchronized public void ccd_messageLost(ClientCommunications cc, int playerSlot) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageLost:" + playerSlot) ;
		
		// The specified player quit.
		// We don't necessarily assume this ends the game, but the GUI should
		// be told.
		playerLost[playerSlot] = true ;
		if ( this.gameState != GAME_STATE_OVER )
			getGameResultBuilder().setLost(playerSlot) ;
		if ( gameUserInterface != null )
			gameUserInterface.gui_gameStatePlayerLost(playerSlot) ;
	}
	
	
	@Override
	synchronized public void ccd_messagePlayerIsSpectator(ClientCommunications cc, int playerSlot) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messagePlayerIsSpectator:" + playerSlot) ;
		
		// The specified player quit.
		// We don't necessarily assume this ends the game, but the GUI should
		// be told.
		playerSpectating[playerSlot] = true ;
		if ( gameUserInterface != null )
			gameUserInterface.gui_gameStatePlayerIsSpectator(playerSlot) ;
	}
	
	

	@Override
	synchronized public void ccd_messageServerClosingForever(ClientCommunications cc) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageServerClosingForever") ;
		
		this.foregroundDisconnectedForever() ;
		
		// Update game state
		gameState = GAME_STATE_OVER ;
		
		// If the server is closing, we should close our connections (and
		// deactivate them).
		if ( clientCommunications != null )
			clientCommunications.stop() ;
		// TODO: Deactivate connection?
		
		// If the game isn't over, the host just quit.
		GameResult.Builder grbuilder = getGameResultBuilder() ;
		grbuilder.setGameIdle() ;
		if ( this.gameState != GAME_STATE_OVER ) {
			grbuilder.setQuit(playerSlot).terminate() ;
		}
		
		// Stop advancing the game; let the GUI know.
		if ( gameUserInterface != null ) {
			gameUserInterface.gui_toggleAdvancingGames(false) ;
			gameUserInterface.gui_connectionToServerServerClosedForever() ;
		}
	}

	@Override
	synchronized public void ccd_messagePlayerName(ClientCommunications cc, int slot, String name) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messagePlayerName " + slot + ":" + name) ;
		
		// Update our metadata and inform the GUI.  GUI will use this value to
		// configure "waiting" and "paused" displays, and maybe "won/lost" later.
		playerNames[slot] = name ;
		getGameResultBuilder().setName(slot,name) ;
		
		if ( gameUserInterface != null )
			gameUserInterface.gui_updatePlayerNames(playerNames) ;
	}

	@Override
	synchronized public void ccd_messageWaiting(ClientCommunications cc, boolean[] players) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageWaiting") ;
		
		// Update our game state
		gameState = GAME_STATE_WAITING ;
		for ( int i = 0; i < numberOfPlayers; i++ )
			gameStatePlayers[i] = players[i] ;
		
		GameResult.Builder grbuilder = getGameResultBuilder() ;
		grbuilder.setWaitingFor(players) ;
		grbuilder.setGameIdle() ;
		
		// Tell the GUI, stop advancing the game.  The call to stop advancing
		// is safe regardless of activity or previous game state.
		if ( gameUserInterface != null ) {
			gameUserInterface.gui_toggleAdvancingGames(false) ;
			gameUserInterface.gui_gameStateIsWaiting(players) ;
		}
	}

	@Override
	synchronized public void ccd_messagePaused(ClientCommunications cc, boolean[] players) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messagePaused") ;
		//new Exception().printStackTrace() ;
		
		// Update our game state
		GameResult.Builder grbuilder = getGameResultBuilder() ;
		gameState = GAME_STATE_PAUSED ;
		if ( players != null ) {
			for ( int i = 0; i < numberOfPlayers; i++ )
				gameStatePlayers[i] = players[i] ; 
			grbuilder.setPausedBy(players) ;
		}
		grbuilder.setGameIdle() ;
		
		// Tell the GUI.  Stop advancing the game.  We can
		// safely make this call even if not advancing previously, and regardless
		// of the activity state.  After this, tell it about the game state.
		if ( gameUserInterface != null ) {
			gameUserInterface.gui_toggleAdvancingGames(false) ;
			gameUserInterface.gui_gameStateIsPaused(players) ;
		}
		
		// Save flood mode games -- IF it's been enough time since
		// the last time, AND we've been playing for at least 10 seconds.
		if ( gip.isLocal() && gip.saveToKey != null
				&& !GlobalTestSettings.GAME_DISABLE_SAVE_ON_LEVEL_UP
				&& GameModes.measurePerformanceBy(gameSettings.getMode()) == GameModes.MEASURE_PERFORMANCE_BY_TIME
				&& game[0] != null && game[0].getTotalSecondsTicked() > 10 ) {
			long minTimeBetweenSaves = 20 * 1000 ;		// 20 seconds for most...
			if ( DeviceModel.is( DeviceModel.Name.NEXUS_S ) ) {
				minTimeBetweenSaves = 60 * 1000 ;		// 1 minute on Nexus S, a problem device.
			}
			
			if ( System.currentTimeMillis() - ccd_message_lastLocalSaveTime > minTimeBetweenSaves ) {
				// auto-save just in case we crash.  We do this no more than once every minute.
				// Why not more frequently?  Because autosaves seem to be correlated with
				// weird, unpredictable "lib.c SIGSEGV" crashes, at least on the Nexus S.
				ccd_message_lastLocalSaveTime = System.currentTimeMillis() ;
				this.saveGameCopyAsynchronously(gip.saveToKey, null, null) ;
			}
		}
	}

	@Override
	synchronized public void ccd_messageGo(ClientCommunications cc) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageGo") ;
		
		// Update our state.
		// RACE CONDITION: We set GAME_STATE_GO *before* checking the activity
		// state.  Compare against activityDidResume.  If we checked-then-set,
		// we might enter a state where both methods are called but neither 
		// toggleAdvancing.
		gameState = GAME_STATE_GO ;
		
		// Tell the GUI.  If the activity is resumed, start the game.
		// Compare against the Service callback for the Activity resuming.
		if ( gameUserInterface != null )
			gameUserInterface.gui_gameStateIsReady() ;
		
		if ( numberOfPlayers > 1 ) {
			mHandler.postDelayed(new Runnable() {
				public void run() {
					if ( gameState == GAME_STATE_GO && activityState == ACTIVITY_STATE_RESUMED ) {
						if ( gameUserInterface != null )
							gameUserInterface.gui_toggleAdvancingGames(true) ;
						getGameResultBuilder().setGameGo() ;
					}
				}
			}, 2000) ;
		} else {
			if ( gameState == GAME_STATE_GO && activityState == ACTIVITY_STATE_RESUMED ) {
				if ( gameUserInterface != null )
					gameUserInterface.gui_toggleAdvancingGames(true) ;
				getGameResultBuilder().setGameGo() ;
			}
		}
		
		// TODO: Add support for advancing game even when the activity is not
		// ongoing; e.g., for forcibly unpausing an MP game when one player
		// refuses to come back.  Probably only important in > 2p games.
	}

	@Override
	synchronized public void ccd_messageGameOver(ClientCommunications cc, boolean[] winningPlayers) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageGameOver") ;
		
		this.gameState = GAME_STATE_OVER ;
		
		GameResult.Builder grbuilder = getGameResultBuilder() ;
		
		// assume the opposite are losers
		if ( winningPlayers != null ) {
			for ( int i = 0; i < Math.min(winningPlayers.length, numberOfPlayers); i++ ) {
				if ( winningPlayers[i] ) {
					playerWon[i] = true ;
					grbuilder.setWon(i) ;
				} else if ( !grbuilder.directAccess().getWon(i) && grbuilder.directAccess().getQuit(i) ) {
					playerLost[i] = true ;
					grbuilder.setLost(i) ;
				}
			}
		}
		grbuilder.terminate() ;
		
		// Close down connections.  Game's done, after all.
		if ( clientCommunications != null )
			clientCommunications.stop() ;
		// we keep our gameCoordinator running. Why?
		// Because sometimes a multiplayer opponent was disconnected
		// at the moment the game ended.  This lets them (for a brief window)
		// reconnect and get an update that the game is over.  However,
		// we shut down our Maintainer -- no need to maintain a game that
		// is actually over.
		/*
		if ( gameCoordinator != null )
			gameCoordinator.stop() ;
		*/
		if ( gameMaintainer != null )
			gameMaintainer.stop() ;
		// Tell our action adapters not to tick
		for ( int i = 0; i < numberOfPlayers; i++ )
			actionAdapter[i].set_gameShouldTick(false) ;
	
		// Tell the achievements.
		Achievements.game_over(getGameResult(), playerSlot) ;
		
		// Try for a push?
		Achievements.push((QuantroApplication)getApplication(), false) ;
		
		resolving = true ;
		if ( gameUserInterface != null ) {
			gameUserInterface.gui_gameStateIsOver( winningPlayers ) ;
			if ( gip.isLocal() && GameModes.levelUp(gip.gameSettings.getMode()) == GameModes.LEVEL_UP_SMOOTH ) {
				// NOTE: We delete existing saves when and if we return to FreePlayGameManagerActivity.
				// This allows users to circumvent the ScoreLoop submission in Endurance mode by
				// killing the process before the game is deleted.  To prevent this, we delete the
				// game immediately.  Note that saving in this "game over" state is probably preferred,
				// but that doesn't seem to work when the game is loaded, unfortunately.
				// TODO: Fix this 'save game-over and load' problem so we can save the state here
				// and load it later to re-examine (but not change score).
				GameSaver.deleteGameButKeepCheckpoints(this, gip.saveToKey) ;
				gameUserInterface.gui_resolveResult(GameUserInterface.RESULT_SINGLE_PLAYER_GAME_OVER ) ;
			}
			else if ( gip.isLocal() )
				gameUserInterface.gui_resolveResult(GameUserInterface.RESULT_SINGLE_PLAYER_REWINDABLE_GAME_OVER ) ;
			else
				gameUserInterface.gui_resolveResult(GameUserInterface.RESULT_MULTI_PLAYER_GAME_OVER ) ;
		}
		
		for ( int i = 0; i < numberOfPlayers; i++ )
			actionAdapter[i].emptyActionQueues() ;
	}
	
	@Override
	synchronized public void ccd_messageLevelUp(ClientCommunications cc, int playerSlot) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageLevelUp") ;
		
		// We only care if this game has a "break" levelup policy.
		if ( gip.isLocal() && GameModes.levelUp( gip.gameSettings.getMode() ) == GameModes.LEVEL_UP_BREAK ) {
			saveOnQuit = false ;
			// Close down communications.  Very little effect, but this is in-keeping
			// with how the resolvedTo...() methods function.
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			// Tell our action adapters not to tick
			for ( int i = 0; i < numberOfPlayers; i++ )
				actionAdapter[i].set_gameShouldTick(false) ;
			
			resolving = true ;
			if ( gameUserInterface != null )
				gameUserInterface.gui_resolveResult(GameUserInterface.RESULT_SINGLE_PLAYER_REWINDABLE_LEVEL_OVER ) ;
		
			for ( int i = 0; i < numberOfPlayers; i++ )
				actionAdapter[i].emptyActionQueues() ;
		} else if ( gip.isLocal() && gip.saveToKey != null
				&& !GlobalTestSettings.GAME_DISABLE_SAVE_ON_LEVEL_UP
				&& GameModes.measurePerformanceBy(gameSettings.getMode()) != GameModes.MEASURE_PERFORMANCE_BY_TIME ) {
			long minTimeBetweenSaves = 20 * 1000 ;		// 20 seconds for most...
			if ( DeviceModel.is( DeviceModel.Name.NEXUS_S ) ) {
				minTimeBetweenSaves = 60 * 1000 ;		// 1 minute on Nexus S, a problem device.
			}
			
			if ( System.currentTimeMillis() - ccd_message_lastLocalSaveTime > minTimeBetweenSaves ) {
				// auto-save just in case we crash.  We do this no more than once every minute.
				// Why not more frequently?  Because autosaves seem to be correlated with
				// weird, unpredictable "lib.c SIGSEGV" crashes, at least on the Nexus S.
				ccd_message_lastLocalSaveTime = System.currentTimeMillis() ;
				this.saveGameCopyAsynchronously(gip.saveToKey, null, null) ;
			}
		}
	}
	
	long ccd_message_lastLocalSaveTime = 0 ;
	
	/**
	 * The specified player is about to go up a level.  Probably this will be ignored,
	 * but maybe not.
	 * 
	 * @param cc
	 * @param playerSlot
	 */
	synchronized public void ccd_messageAboutToLevelUp( ClientCommunications cc, int playerSlot ) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageLevelUp") ;
		// We only care if this game has a "break" levelup policy.
		if ( gip.isLocal() && GameModes.levelUp( gip.gameSettings.getMode() ) == GameModes.LEVEL_UP_BREAK ) {
			// Saving here is necessary; when we load the game next, we want to
			// be able to load from this spot.
			saveGame(gip.saveToKey) ;
		}
	}
	
	
	/**
	 * The specified player initialized.  Probably this will be ignored, but maybe not.
	 * 
	 * @param cc
	 * @param playerSlot
	 */
	@Override
	synchronized public void ccd_messageInitialized( ClientCommunications cc, int playerSlot ) {
		if ( cc != clientCommunications )
			return ;
		
		Log.d(TAG, "ccd_messageInitialized") ;
		// We only care if this game has a "break" levelup policy.  Checkpoint here.
		if ( gip.isLocal() && GameModes.levelUp( gip.gameSettings.getMode() ) == GameModes.LEVEL_UP_BREAK ) {
			saveOnQuit = false ;
			// Close down communications.  Very little effect, but this is in-keeping
			// with how the resolvedTo...() methods function.
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			// Tell our action adapters not to tick
			for ( int i = 0; i < numberOfPlayers; i++ )
				actionAdapter[i].set_gameShouldTick(false) ;
			
			resolving = true ;
			if ( gameUserInterface != null )
				gameUserInterface.gui_resolveResult(GameUserInterface.RESULT_SINGLE_PLAYER_CHECKPOINT ) ;
		
			for ( int i = 0; i < numberOfPlayers; i++ )
				actionAdapter[i].emptyActionQueues() ;
		}
	}

	@Override
	synchronized public long ccd_requestDelayUntilConnect(ClientCommunications cc) {
		if ( cc != clientCommunications )
			return -1 ;
		
		Log.d(TAG, "ccd_requestDelayUntilConnect") ;
		// For now, try again in 1 second.
		return 1000 ;
	}

	@Override
	synchronized public Game ccd_requestGameObject(ClientCommunications cc, int playerSlot) {
		if ( cc != clientCommunications )
			return null ;
		
		Log.d(TAG, "ccd_requestGameObject") ;
		return game[playerSlot] ;
	}
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * MATCH ROUTER UPDATE LISTENER METHODS
	 * 
	 * Updates received from a MatchRouter.  These updates are direct translations of
	 * MessagePassingMatchSeekingConnection.Delegate methods; there is probably
	 * a much nicer way of handling them, but for now, 
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * A new requester has come in, requesting a serial.
	 * 
	 * @param serial
	 */
	public void mrul_didStartMatchRequest( long serial ) {
		// nothing
	}
	
	
	/**
	 * We are beginning a fresh match ticket acquisition attempt.  This method will be called
	 * after connect(), or during connect().  Matchticket acquisitions don't happen every time
	 * we need to connect - a single matchticket can cover multiple connection
	 * attempts.
	 * @param mpmsc
	 */
	public void mrul_didBeginMatchTicketAcquisitionAttempt( long serial ) {
		if ( gameUserInterface != null )
			gameUserInterface.gui_matchmakingRequestingTicket() ;
		// nothing else to do here.
	}
	
	
	/**
	 * We got a matchticket.
	 * 
	 * @param mpmsc
	 */
	public void mrul_didReceiveMatchTicket( long serial ) {
		// nuttin'
		// if we cared abut FULL rejections, here's where we would undo the "Is full" message.
	}
	
	
	/**
	 * We failed to acquire a MatchTicket.  We provide an error code and a reason code,
	 * as listed in WebConsts.
	 * 
	 * Return whether to continue or connection attempts.  If 'false' is returned,
	 * we stop and transition to FAILED status.
	 * 
	 * If 'true' is returned, we start a new attempt.
	 * 
	 * @param mpmsc
	 * @param error
	 * @param reason
	 */
	public boolean mrul_didFailMatchTicketAcquisitionAttempt( long serial, int error, int reason ) {
		// our reaction depends on the kind of error.
		switch( error ) {
		case WebConsts.ERROR_ILLEGAL_STATE:
			// the lobby is closed.  Nothing more to do.
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingGameClosed();
			return false ;
		case WebConsts.ERROR_NONE:
			throw new RuntimeException("mpmscd reported error, but gave ERROR_NONE as its code.") ;
		case WebConsts.ERROR_TIMEOUT:
			// lack of response.  User should be notified, but we keep trying.
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingNoTicketNoResponse() ;
			return true ;
		case WebConsts.ERROR_REFUSED:
			// WHAT!  What's the reason??
			Log.d( TAG, "mpmscd ticket refused, reason is " + WebConsts.reasonIntToString(reason) ) ;
			return false ;
		case WebConsts.ERROR_FAILED:
			throw new RuntimeException("mpmscd reported FAILURE.  This should never happen!") ;
		case WebConsts.ERROR_BLANK:
			// hm, communication error?
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingNoTicketNoResponse() ;
			return true ;
		case WebConsts.ERROR_MALFORMED:
			// hm, communication error?
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingNoTicketNoResponse() ;
			return true ;
		case WebConsts.ERROR_ILLEGAL_ACTION:
			throw new RuntimeException("mpmscd reported ILLEGAL ACTION.  This should never happen!") ;
		case WebConsts.ERROR_UNKNOWN:
			throw new RuntimeException("mpmscd reported UNKNOWN ERROR.  This should never happen!") ;
		}
		
		// I DON'T KNOW WHAT TO DO WITH THIS ERROR!
		throw new RuntimeException("mpmscd reported strange error code: " + error) ;
	}
	
	
	/**
	 * We are beginning a fresh match seeking attempt.  This method will be called shortly
	 * after connect(), or during connect().
	 * 
	 * @param mpmsc
	 */
	public void mrul_didBeginMatchSeekingAttempt( long serial ) {
		if ( gameUserInterface != null )
			gameUserInterface.gui_matchmakingRequestingMatch() ;
	}
	
	/**
	 * We have failed to communicate with the matchmaker, and are giving up.  We will
	 * transition to "failed" status immediately after this method call.
	 * 
	 * Internally, this results after a series of UDP timeouts in requesting a match.
	 * 
	 * @param mpmc
	 */
	public void mrul_didFailToConnectToMatchmaker( long serial ) {
		if ( gameUserInterface != null )
			gameUserInterface.gui_matchmakingNoResponse() ;
	}
	
	/**
	 * We received a rejection message from the matchmaker.  The reason is provided,
	 * which is one of REJECTION_REASON_*.
	 * 
	 * @param mpmsc
	 * @param rejectionReason
	 * @return If true, continues to attempt mediation, staying in "pending" status.
	 * 			If false, transitions to "failed" status.
	 */
	public boolean mrul_didReceiveRejectionFromMatchmaker(
			long serial,
			MatchRouter.UpdateListener.Rejection rejectionReason ) {
		switch( rejectionReason ) {
		case FULL:
			// the connection will automatically wait before posting the next request.
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingRejectedFull() ;
			return true ;
			
			
		case INVALID_NONCE:
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingRejectedInvalidNonce() ;
			// This is unrecoverable.  We shouldn't keep trying to reconnect.
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			if ( gameCoordinator != null )
				gameCoordinator.stop() ;
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
			return false ;
			
		case NONCE_IN_USE:
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingRejectedNonceInUse() ;
			// This is unrecoverable.  We shouldn't keep trying to reconnect.
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			if ( gameCoordinator != null )
				gameCoordinator.stop() ;
			return false ;
			
		case PORT_RANDOMIZATION:
			// we CAN recover from this.  mpmsc has already set a delay.
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingRejectedPortRandomization() ;
			return true ;
			
		case UNSPECIFIED:
			if ( gameUserInterface != null )
				gameUserInterface.gui_matchmakingRejectedUnspecified() ;
			if ( clientCommunications != null )
				clientCommunications.stop() ;
			if ( gameCoordinator != null )
				gameCoordinator.stop() ;
			if ( gameMaintainer != null )
				gameMaintainer.stop() ;
			return false ;
			
		}
		return false ;
	}
	
	
	/**
	 * We have received information from the Matchmaker which implies that we are the first
	 * to arrive for the match (e.g., a Promise message).  We don't know how long we'll be
	 * waiting, so it might be wise to transition from Lobby client to Lobby host in a bit (for example).
	 * 
	 * For safety's sake, you should be robust to this method being called multiple times in sequence.
	 * @param mpmc
	 */
	public void mrul_didBeginWaitingForMatch( long serial ) {
		// Tell the LUI
		if ( gameUserInterface != null )
			gameUserInterface.gui_matchmakingReceivedPromise() ;
	}
	
	
	/**
	 * We have been matched with a partner!  However, we have not yet begun the hole-punching
	 * attempt (or whatever) and do not have a direct connection with them.
	 * @param mpmc
	 */
	public void mrul_didMatchStart( long serial ) {
		if ( gameUserInterface != null )
			gameUserInterface.gui_matchmakingReceivedMatch() ;
	}
	
	
	/**
	 * We have completed our match with a partner, and now have a communication channel with them!
	 * @param mpmsc
	 */
	public void mrul_didMatchSuccess( long serial ) {
		if ( gameUserInterface != null )
			gameUserInterface.gui_matchmakingComplete() ;
	}
	
	
	/**
	 * We started a match attempt, but it failed before completion.  This probably means UDP
	 * hole-punching failed.
	 * 
	 * @param mpmc
	 * @return If true, we restart the matchmaking process, requesting another match from the server.
	 * 			If false, transitions to "failed" status.
	 */
	public boolean mrul_didMatchFailure( long serial, int reason ) {
		if ( gameUserInterface != null )
			gameUserInterface.gui_matchmakingFailed() ;
		// TODO: Backoff our match attempts?  Maybe extend the hole-punching time?
		return true ;
	}
	
	
	/**
	 * A request has ended.
	 * 
	 * @param serial
	 */
	public void mrul_didEndMatchRequest( long serial ) {
		// nothing
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INTERNET LOBBY MAINTAINER DELEGATE
	//
	
	/**
	 * A successful communication has just occurred with the server while performing 
	 * the specified action.  This is probably of no concern of yours and you should
	 * best ignore it.
	 * 
	 * The only reason this method exists is to notify you of when a series of failures
	 * has ended.  A successful communication can usually be taken to mean a problem
	 * interfering with communication has been resolved.
	 * 
	 * @param ilm
	 * @param action
	 * @param timeSpentFailing What is the length of time between the first in the
	 * 			most recent of failure messages and now?
	 * @param numFailures How many consecutive failures have we seen before this success?
	 */
	public void ilgmd_communicationSuccess( InternetLobbyGameMaintainer ilgm, int action,
			long timeSpentFailing, int numFailures ) {
		
		if ( ilgm == null || ilgm != gameMaintainer )
			return ;
		
		// stop any countdowns or notifications for communication failure.
		mHandler.removeCallbacks(mRunnableServerCommunicationFailure) ;
		mHandler.post(mRunnableServerCommunicationOK) ;
		
		// do NOT stop runnables for other types, such as lobby closed.  It's
		// worth noting that 'refresh' actions can receive a false positive
		// communication success when the lobby is closed or removed, BUT
		// in that case we should put up a 'removed' runnable anyway.
		
		if ( action == InternetLobbyGameMaintainer.Delegate.ACTION_REFRESH
				&& ( (internetLobbyGame).getStatus() == WebConsts.STATUS_CLOSED
						|| (internetLobbyGame).getStatus() == WebConsts.STATUS_REMOVED ) ) {
			if ( !mServerGameClosed ) {
				Log.d(TAG, "mRunnableGameClosed: ilgmd_communicationSuccess for refresh; is closed or removed.") ;
				new Exception("Closed").printStackTrace() ;
				mHandler.post(mRunnableGameClosed) ;
			}
		}
	}
	
	/**
	 * A failure has occurred when performing the specified action.  Feel
	 * free to ignore certain values, such as a refusal based on 'time':
	 * we will be attempting recovery.
	 * 
	 * The 'long' value returned will be considered as an ADDITIONAL delay
	 * on top of the standard delay we would have otherwise used.  It will
	 * be applied ONLY to retry attempt for this failed action.  If you are stopping
	 * the maintainer, feel free to return a massive value; it will have no
	 * effect on normal operation after it is resumed.
	 * 
	 * However, do NOT return extreme delays if you intend operation to continue.
	 * Usually the retry for this action will be the next time the action
	 * is attempted, so extreme delays can interfere with our future attempts.
	 * 
	 * Feel free to change regular update/maintenance schedules or even stop
	 * the maintainer during this call.
	 * 
	 * @param ilm
	 * @param action
	 * @param error
	 * @param reason
	 * @param timeSpentFailing What is the length of time between the first in the
	 * 			most recent of failure messages and now?  If 0, then this is the first
	 * 			failure of the series (which may end up being length 1).
	 * @param numFailures How many consecutive failures have we seen, including this one?
	 */
	public long ilgmd_retryDelayAfterCommunicationFailure( InternetLobbyGameMaintainer ilgm, int action, int error, int reason,
			long timeSpentFailing, int numFailures ) {
		
		if ( ilgm == null || ilgm != gameMaintainer || this.gameState == GAME_STATE_OVER ) {
			if ( ilgm != null )
				ilgm.stop() ;
			return 0 ;
		}
		
		// Different types of failures require a different response.
		switch( error ) {
		case WebConsts.ERROR_MISMATCHED_WORK_ORDER:
		case WebConsts.ERROR_INCOMPLETE_WORK_ORDER:
			// nothing we can do about these.  The maintainer will retry.
			// print to log, because this should probably never happen.
			Log.d(TAG, "InternetLobbyGameMaintainerDelegate: reported error mismatched or incomplete work order.") ;
			break ;
		
		case WebConsts.ERROR_NONE:
			// huh?
			Log.d(TAG, "InternetLobbyGameMaintainerDelegate: reported error NONE???") ;
			break ;
			
		case WebConsts.ERROR_TIMEOUT:
		case WebConsts.ERROR_BLANK:
		case WebConsts.ERROR_MALFORMED:
		case WebConsts.ERROR_UNKNOWN:
			// Communication failure.  This should start a countdown to
			// user notification about this failure.
			if ( !mServerCommunicationFailing )		// fail in 45 seconds starting now
				mHandler.postDelayed(mRunnableServerCommunicationFailure, SERVER_COMMUNICATION_FAILURE_COUNTDOWN) ;
			break ;
			
		case WebConsts.ERROR_ILLEGAL_ACTION:
		case WebConsts.ERROR_ILLEGAL_STATE:
			// The game state has changed, and we can no longer do that thing.
			// This basically means the lobby is closed.  Tell the user and
			// shut everything down, possibly even in that order.
			Log.d(TAG, "mRunnableGameClosed: ilgmd_retryDelayAfterCommunicationFailure: IllegalAction or IllegalState") ;
			new Exception("Closed").printStackTrace() ;
			mHandler.post(mRunnableGameClosed) ;
			break ;
			
		case WebConsts.ERROR_REFUSED:
			// the server refused (for some reason).  Our response could be different
			// depending on what the action was and the server's reason for refusal.
			if ( action == InternetLobbyGameMaintainer.Delegate.ACTION_REFRESH ) {
				// regardless of reason, a refusal means the lobby has been removed.
				if ( !mServerGameClosed ) {
					Log.d(TAG, "mRunnableGameClosed: ilgmd_retryDelayAfterCommunicationFailure: Refresh Refused.") ;
					new Exception("Closed").printStackTrace() ;
					mHandler.post(mRunnableGameClosed) ;
				}
			} else if ( action == InternetLobbyGameMaintainer.Delegate.ACTION_HOST ) {
				// three types of refusal.  PARAMS: huh?  DATABASE: edit error.
				// communication?  NoReason: lobby can't be changed, probably a lobby
				// problem.  Use 'communication' if a reason is given (this is probably
				// the opposite of what you might expect, but as of 10/12 it is correct
				// behavior).
				if ( reason != WebConsts.REASON_NONE ) {
					if ( !mServerCommunicationFailing )		// fail in 45 seconds starting now
						mHandler.postDelayed(mRunnableServerCommunicationFailure, SERVER_COMMUNICATION_FAILURE_COUNTDOWN) ;
				} else {
					// lobby closed or removed.
					if ( !mServerGameClosed ) {
						Log.d(TAG, "mRunnableGameClosed: ilgmd_retryDelayAfterCommunicationFailure: Host Refused for reason 'None'") ;
						new Exception("Closed").printStackTrace() ;
						mHandler.post(mRunnableGameClosed) ;
					}
				}
			} else if ( action == InternetLobbyGameMaintainer.Delegate.ACTION_MAINTAIN ) {
				// two possible reasons (this could be a MAINTAIN_REQUEST or a 
				// MAINTAIN_CONFIRM message).  If CLOSED, obviously the lobby is
				// closed.  If WORK, the work order was insufficient and
				// the Maintainer is going to try again.  Not a huge issue for
				// us, though.
				if ( reason == WebConsts.REASON_CLOSED ) {
					// lobby closed or removed.
					if ( !mServerGameClosed ) {
						Log.d(TAG, "mRunnableGameClosed: ilgmd_retryDelayAfterCommunicationFailure: Maintain Refused for reason 'Closed'") ;
						new Exception("Closed").printStackTrace() ;
						mHandler.post(mRunnableGameClosed) ;
					}
				}
			}
			break ;
			
		case WebConsts.ERROR_FAILED:
			// communication worked, but the action failed.  Could mean
			// the server is having problems, or something is wrong with our 
			// lobby (maybe it's been closed?).  In other words, although this
			// technically represents a response from the server, it means that
			// something is going wrong.
			if ( !mServerCommunicationFailing )		// fail in 45 seconds starting now
				mHandler.postDelayed(mRunnableServerCommunicationFailure, SERVER_COMMUNICATION_FAILURE_COUNTDOWN) ;
			break ;
		}
		
		// No change to delay.
		return 0 ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	private class ThreadCloseGame extends Thread {
		InternetLobbyGame mInternetLobbyGame ;
		
		private ThreadCloseGame( InternetLobbyGame ilg ) {
			mInternetLobbyGame = ilg ;
		}

		@Override
		public void run() {
			try {
				mInternetLobbyGame.close() ;
			} catch (CommunicationErrorException e) {
				e.printStackTrace();
			}
		}
	}

}
