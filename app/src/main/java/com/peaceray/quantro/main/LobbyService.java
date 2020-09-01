package com.peaceray.quantro.main;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;

import com.peaceray.quantro.QuantroActivity;
import com.peaceray.quantro.QuantroApplication;
import com.peaceray.quantro.LobbyActivity;
import com.peaceray.quantro.GameActivity;
import com.peaceray.quantro.GameIntentPackage;
import com.peaceray.quantro.LobbyIntentPackage;
import com.peaceray.quantro.QuantroPreferences;
import com.peaceray.quantro.R;
import com.peaceray.quantro.communications.MultipleMessageReader;
import com.peaceray.quantro.communications.SimpleSHA1;
import com.peaceray.quantro.communications.messagepassing.MessagePassingConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingDirectClientConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingDirectServerConnection;
import com.peaceray.quantro.communications.messagepassing.MessagePassingLayer;
import com.peaceray.quantro.communications.messagepassing.MessagePassingPairedConnection;
import com.peaceray.quantro.communications.messagepassing.matchseeker.MatchRouter;
import com.peaceray.quantro.communications.messagepassing.matchseeker.MatchRouter.UpdateListener;
import com.peaceray.quantro.communications.messagepassing.matchseeker.MessagePassingMatchSeekerConnection;
import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.communications.wrapper.AdministratedMessageAwareWrappedUDPSocketChannelAdministrator;
import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.database.GameStats;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.keys.KeyStorage;
import com.peaceray.quantro.lobby.InternetGameMaintenancePortfolio;
import com.peaceray.quantro.lobby.InternetLobbyGame;
import com.peaceray.quantro.lobby.InternetLobbyMaintainer;
import com.peaceray.quantro.lobby.LobbyAnnouncer;
import com.peaceray.quantro.lobby.LobbyClientCommunications;
import com.peaceray.quantro.lobby.LobbyCoordinator;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.LobbyLog;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.MutableInternetLobby;
import com.peaceray.quantro.lobby.WebConsts;
import com.peaceray.quantro.lobby.communications.LobbyMessage;
import com.peaceray.quantro.lobby.exception.CommunicationErrorException;
import com.peaceray.quantro.main.notifications.NotificationAdapter;
import com.peaceray.quantro.main.notifications.QuantroNotificationMaker;
import com.peaceray.quantro.model.game.GameSettings;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.premium.PremiumLibrary;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.AndroidID;
import com.peaceray.quantro.utils.AppVersion;
import com.peaceray.quantro.utils.Base64;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.WifiMonitor;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class LobbyService extends Service
	implements Lobby.Delegate, LobbyLog.Delegate, LobbyCoordinator.Delegate,
			LobbyClientCommunications.Delegate,
			InternetLobbyMaintainer.Delegate,
			InternetGameMaintenancePortfolio.Delegate, UpdateListener {
	
	private final static String TAG = "LobbyService" ;
	
	
	private Resources res ;
	
	// LOBBY ACTIVITY
	// The LobbyActivity will give us a reference to itself; we store as 
	// an instance of the LobbyUserInterface interface.
	boolean mUseUserInterface = false ;
	LobbyUserInterface lobbyUserInterface = null ;
	Activity activity = null ;
	
	// Notifications
	NotificationAdapter notificationAdapter = null ;
	
	// MY VARS
	LobbyIntentPackage lip ;
	
	// Local lobby objects
	Lobby lobby ;
	LobbyLog lobbyLog ;
	
	// Communication
	LobbyClientCommunications lobbyClientCommunications ;
	MessagePassingPairedConnection [] mppcPair ; 		// used purely for local connections (if any)
	
	// Lobby server
	LobbyCoordinator lobbyCoordinator ;
	MultipleMessageReader mmReader ;
	LobbyAnnouncer lobbyAnnouncer ;
	InternetLobbyMaintainer lobbyMaintainer ;
	InternetGameMaintenancePortfolio lobbyGamePortfolio ;
	
	// Host/Client status.
	int playersPresentInMediatorUponFirstConnect = -1 ;		// 0 or 1, depending on whether we got to the mediator first.
	boolean hasChangedRoles = false ;
	int slotForFutureHost ;
	
	// Internet communications?
	Handler mHandler ;
	Runnable mRunnableServerCommunicationFailure ;	// Can't contact server.
	Runnable mRunnableServerCommunicationOK ;		// We DID contact server!
	Runnable mRunnableLobbyFull ;			// Inet lobby is full (can't connect)
	Runnable mRunnableLobbyNotFull ;		// Inet lobby is NOT full!
	Runnable mRunnableLobbyClosed ;			// Inet lobby is closed.
	Runnable mRunnableGamesFull ;			// full of games
	Runnable mRunnableAchievementsUpdate ;
	boolean mServerCommunicationFailing = false ;
	boolean mServerLobbyClosed = false ;
	boolean mServerLobbyFull = false ;
	static final int SERVER_COMMUNICATION_FAILURE_COUNTDOWN = 45 * 1000 ;
	static final int SERVER_LOBBY_FULL_COUNTDOWN = 20 * 1000 ;
	
	// This is set to false most of the time; when the Activity Stops, we go to background
	// service to keep the connection up.  However, when this is 'true', we do NOT go to background;
	// instead, we have already stopped the lC and lCC, and allow the service to close down
	// if it needs to (i.e., if the Activity closes).  Additionally, when the Activity starts
	// again, we will start the lC and lCC.
	// When the user 'quits' the activity, this is set to true; the same is true when the user
	// enters a game from the lobby.  This value is reset false each time the Activity starts.
	boolean shouldStayInBackground ;
	
	boolean alwaysForeground ;		// InternetLobbies must be maintained at all times.
									// WiFi lobbies as well, now, if they have size > 2.
	
	boolean movingToGame ;			// Set to true when launching, to false when resumed.
	
	// LOBBY META DATA
	// These instance vars are very useful in tracking the current state of things.
	// Info on the Activity!
	int activityState ;
	private static final int ACTIVITY_STATE_NONE = -1 ;
	private static final int ACTIVITY_STATE_CREATED = 0 ;
	private static final int ACTIVITY_STATE_STARTED = 1 ;
	private static final int ACTIVITY_STATE_RESUMED = 2 ;
	
	// Analytics?
	private boolean mAnalyticsInSession = false ;

	@Override
	public IBinder onBind(Intent intent) {
		Log.d(TAG, "onBind") ;
		
		shouldStayInBackground = false ;
		alwaysForeground = false ;
		movingToGame = false ;
		
		// Android: onBind is called ONCE in the lifecycle of a Service,
		// regardless of how many times bindService() is called.  However, we
		// assume this Service is only bound to a single Activity (a LobbyActivity)
		// and no other LobbyActivity will launch during that time.
		
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
		// This method call performs the necessary setup to prevent failure.
		VersionSafe.prepareForUDPnio(this) ;
		
		// provide self as lobby service to QuantroApplication.
		((QuantroApplication)getApplication()).setLobbyService(this) ;
	}
	
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy") ;
		
		// stop anything running...
		if ( lobbyClientCommunications != null && lobbyClientCommunications.isStarted() ) {
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
		}
		if ( lobbyCoordinator != null && lobbyClientCommunications.isStarted()  )
			lobbyCoordinator.stop() ;
		if ( mmReader != null )
			mmReader.kill() ;
		if ( lobbyAnnouncer != null && lobbyClientCommunications.isStarted()  )
			lobbyAnnouncer.stop() ;
		if ( lobbyMaintainer != null && lobbyMaintainer.isStarted() )
			lobbyMaintainer.stop() ;
		if ( lobbyGamePortfolio != null && lobbyGamePortfolio.isStarted() )
			lobbyGamePortfolio.stop() ;
		
		// Immediately give up the WakeLock, if we have one.
		getLocksAndReleaseIfHeld() ;
		
		// make sure no notifications linger
		notificationAdapter.dismissForegroundNotification() ;
		
		if ( mAnalyticsInSession )
			Analytics.stopSession(this) ;
		
		// remove self as lobby service to QuantroApplication.
		((QuantroApplication)getApplication()).unsetLobbyService(this) ;
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
	
	public void allocatedAndInitializeClientConnection( Lobby lobby, LobbyLog lobbyLog, LobbyIntentPackage lip ) {
		// Now start a lobby client communications, lobby, and a lobby view,
        // hooking everything together.  We will start threads in "onResume".
		
		// First make our pair; we use this for any local connections.
		mppcPair = MessagePassingPairedConnection.newPair(
				lip.nonce,
				new Nonce[]{lip.personalNonce, lip.personalNonce},
				new String[]{lip.name, lip.name} ) ;
        
        try {
        	lobbyClientCommunications = new LobbyClientCommunications(
        			this, lobby, lobbyLog,
        			lip.nonce, lip.personalNonce, lip.name, getPremiumAuthTokens()) ;
        	// Direct clients use a direct client connection.  Mediated
        	// clients use a mediated connection.  Servers use our local pair.
        	MessagePassingConnection conn = null ;
        	if ( lip.isHost() ) {
        		conn = mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_CLIENT] ;
        	}
        	else if ( lip.connection == LobbyIntentPackage.LOBBY_DIRECT_CLIENT )
        		conn = new MessagePassingDirectClientConnection(
        				LobbyMessage.class,
        				lip.nonce, lip.personalNonce, lip.name, lip.socketAddress) ;
        	else {
        		String xl = null ;
	        	if ( KeyStorage.hasXLKey(this) )
	        		xl = KeyStorage.getXLKey(this).getKey() ;
	        	MatchRouter matchRouter = new MatchRouter(
	        			this,
	        			new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator(), 
	        			false,	// serial matching: it's a lobby, only fill 1 slot at a time!
	        			xl, lip.socketAddress[0], VersionCapabilities.isEmulator() ? new int[]{52050, 52050} : new int[]{52000, 53000},
	        			(InternetLobby)lobby, (byte)0, LobbyMessage.class,
	        			lip.personalNonce, lip.name,
	        			Base64.encodeBytes(SimpleSHA1.SHA1Bytes(lip.nonce.toString() + AndroidID.get(this))),
	        			null ) ;
	        	conn = new MessagePassingMatchSeekerConnection(
	        			matchRouter,
        				new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator(),
        				lip.nonce,
        				lip.personalNonce, lip.name,
        				null,
        				LobbyMessage.class) ;
        	}
        	conn.activate() ;
        	lobbyClientCommunications.setConnection(conn) ;
        } catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		updateAchievementsEnterLobby() ;
	}
	
	
	public void allocateAndInitializeCoordinatorAndAnnouncer(LobbyIntentPackage lip) {
		if ( lip.connection == LobbyIntentPackage.LOBBY_DIRECT_HOST ) {
	        lobbyAnnouncer = new LobbyAnnouncer( this, lip.nonce, lobby,
	        		lip.lobbyAnnouncerPort, lip.lobbyListenPort, lip.localPort ) ;
	    }
		
		lobbyMaintainer = null ;
		lobbyGamePortfolio = null ;
		
		if ( lip.connection == LobbyIntentPackage.LOBBY_MATCHSEEKER_HOST ) {
			lobbyMaintainer = InternetLobbyMaintainer.newMaintainer( (MutableInternetLobby) lobby, this ) ;
			lobbyMaintainer.hostEvery(60 * 1000) ;	// every minute
			lobbyMaintainer.maintainEvery(5 * 60 * 1000) ;	// every 5 minutes
			lobbyMaintainer.refreshEvery(60 * 1000) ;	// every minute
			
			// host and maintain immediately
			lobbyMaintainer.host().maintain().refresh() ;
			
			lobbyGamePortfolio = new InternetGameMaintenancePortfolio( (MutableInternetLobby) lobby, this ) ;
			lobbyGamePortfolio.openUpTo(lobby.getMaxPeople()) ;		// twice as many games as we expect to use
			lobbyGamePortfolio.maintainEvery(6 * 60 * 1000) ;	// every 6 minutes
			lobbyGamePortfolio.refreshEvery(60 * 1000) ;		// every minute
		} else if ( lip.connection == LobbyIntentPackage.LOBBY_MATCHSEEKER_CLIENT ) {
			lobbyMaintainer = InternetLobbyMaintainer.newRefresher((InternetLobby) lobby, this) ;
			lobbyMaintainer.refreshEvery(60 * 1000) ;	// every minute
			lobbyMaintainer.refresh() ;
		}
		
		if ( lip.isHost() ) {
	        // Now the lobby itself (and a client listener).  We always connect
			// in slot 0 by default.
	        lobbyCoordinator = new LobbyCoordinator(
	        		this,
	        		AppVersion.code(this),
	        		lip.nonce,
	        		lip.nonce,
	        		lip.lobbyName, 0, lip.name, true, lip.lobbySize ) ;
	        // TODO: This lobbyCoordinator broadcasts personal nonces,
	        // has a max of 2 people, and launches games IMMEDIATELY (without countdown).
	        // Change the constructor if this is not desired behavior.
	        
	        // MessagePassingLayer for this Coordinator.
	        MessagePassingLayer mpLayer = new MessagePassingLayer(lip.lobbySize) ;
	        mmReader = new MultipleMessageReader() ;
	        AdministratedMessageAwareWrappedUDPSocketChannelAdministrator administrator = new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator() ;
	        // self connection, and open connection.
	        try {
	        	// slot 0 is for us; use the server-end of the pair.
		        mpLayer.setConnection(0,
		        		mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_SERVER] ) ;
		        
		        String xl = null ;
	        	if ( KeyStorage.hasXLKey(this) )
	        		xl = KeyStorage.getXLKey(this).getKey() ;
	        	MatchRouter matchRouter = null ;
	        	if ( lip.connection == LobbyIntentPackage.LOBBY_MATCHSEEKER_HOST ) {
		        	try {
		        		matchRouter = new MatchRouter(
			        			this,
			        			new AdministratedMessageAwareWrappedUDPSocketChannelAdministrator(), 
			        			false,	// serial matching: it's a lobby, only fill 1 slot at a time!
			        			xl, lip.socketAddress[0], new int[]{52000, 53000},
			        			(InternetLobby)lobby, (byte)0, LobbyMessage.class,
			        			lip.personalNonce, lip.name,
			        			Base64.encodeBytes(SimpleSHA1.SHA1Bytes(lip.nonce.toString() + AndroidID.get(this))),
			        			null ) ;		// allow any personal nonce
		        	}  catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return ;
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return ;
					}
	        	}
		        
		        for ( int slot = 1; slot < lip.lobbySize; slot++ ) {
			        if ( lip.connection == LobbyIntentPackage.LOBBY_DIRECT_HOST ) {
			        	MessagePassingDirectServerConnection conn = new MessagePassingDirectServerConnection(
	        					lip.localPort,
	        					LobbyMessage.class,
	        					lip.nonce, null,
	        					false, true ) ;
			        	conn.setMultipleMessageReader(mmReader) ;
			        	mpLayer.setConnection(slot, conn) ;
			        }
			        else {
			        	MessagePassingMatchSeekerConnection conn = new MessagePassingMatchSeekerConnection(
			        			matchRouter, administrator,
			        			lip.nonce,
		        				lip.personalNonce, lip.name,
		        				null,		// don't link to a particular personal nonce
		        				LobbyMessage.class) ;
			        	conn.setMultipleMessageReader(mmReader) ;
			        	mpLayer.setConnection(slot, conn) ;
			        }
		        }
		        mpLayer.activate() ;
		        lobbyCoordinator.setMessagePassingLayer(mpLayer) ;
	        }  catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
        }
        else {
        	lobbyAnnouncer = null ;
        	lobbyCoordinator = null ;
        	mmReader = null ;
        }
	}
	
	
	private void allocateAndInitializePeaceRayServerCommunicationRunnables( LobbyIntentPackage lip ) {
		// set handler operation vars
		mServerCommunicationFailing = false ;
		mServerLobbyClosed = false ;
		
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
				LobbyUserInterface lui = lobbyUserInterface ;
				if ( !mServerCommunicationFailing && lui != null ) {
					mServerCommunicationFailing = true ;
					lui.lui_peacerayServerCommunicationFailure() ;
					
					// TODO: We've told the LUI, but it might be hidden.
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
				LobbyUserInterface lui = lobbyUserInterface ;
				if ( mServerCommunicationFailing && lui != null ) {
					mServerCommunicationFailing = false ;
					lui.lui_peacerayServerCommunicationOK() ;
					
					// TODO: We've told the LUI, but it might be hidden.
					// We also need to change the foreground notification.
				}
			}
		} ;
		

		mRunnableLobbyFull = new Runnable() {
			@Override
			public void run() {
				LobbyUserInterface lui = lobbyUserInterface ;
				if ( !mServerLobbyFull && lui != null ) {
					mServerLobbyFull = true ;
					lui.lui_peacerayServerLobbyFull() ;
					
					// TODO: We've told the LUI, but it might be hidden.
					// We also need to change the foreground notification.
				}
			}
		} ;
		
		
		mRunnableLobbyNotFull = new Runnable() {
			@Override
			public void run() {
				LobbyUserInterface lui = lobbyUserInterface ;
				if ( mServerLobbyFull && lui != null ) {
					mServerLobbyFull = false ;
					lui.lui_peacerayServerLobbyNotFull() ;
					
					// TODO: We've told the LUI, but it might be hidden.
					// We also need to change the foreground notification.
				}
			}
		} ;
		
		
		
		
		
		// mRunnableLobbyClosed ;			// Inet lobby is closed.
		// turns out the lobby we are hosting is closed.  This means we (pretty much)
		// need to SHUT.  DOWN.  EVERYTHING.  However, we probably want to 
		// tell all connected users what's happening?  Also, notify the GUI
		// and set mServerLobbyClosed to true.
		mRunnableLobbyClosed = new Runnable() {
			@Override
			public void run() {
				LobbyUserInterface lui = lobbyUserInterface ;
				if ( !mServerLobbyClosed && lui != null ) {
					mServerLobbyClosed = true ;
					if ( lobbyMaintainer != null && lobbyMaintainer.isStarted() )
						lobbyMaintainer.stop() ;
					if ( lobbyGamePortfolio != null && lobbyGamePortfolio.isStarted() )
						lobbyGamePortfolio.stop() ;
					lui.lui_peacerayServerLobbyClosed() ;
					// TODO: if hosting, send a nice message to everyone
					// connected and close things down.
					if ( lobbyCoordinator != null && lobbyCoordinator.isStarted() )
						lobbyCoordinator.stop() ;
					if ( mmReader != null )
						mmReader.kill() ;
					
					// TODO: We've told the LUI, but it might be hidden.
					// We also need to change the foreground notification.
				}
			}
		} ;
		
		// mRunnableGamesFull ;			// full of games
		// We're out of server space for Games.  This is highly unlikely
		// for well-behaved clients!  The best thing to do here is to close up
		// any games which are currently open (hosted) but which have connected
		// users who are not currently in the lobby.  We kindof hope that players
		// stay in the lobby while they're in a game.  If a communication snafu
		// comes up we generally leave their game open, but now we need the space, 
		// so...  first off, though, if a particular player is in > 1 game, we should
		// probably close the oldest one(s).  Lastly, this message might occur multiple
		// times if we're still having trouble so it might be worth it to only
		// close a few at a time.
		mRunnableGamesFull = new Runnable() {
			@Override
			public void run() {
				// the problem here is that we're trying to do something but
				// cannot because there are too many open games.  We are almost
				// certainly allowed more games than people in the lobby,
				// meaning that either some lobby members have more than one game to 
				// their name, or some previous members are holding open games
				// without being connected to the lobby.
				
				// This, we cannot allow.  Well, okay, we allow it.  But not
				// to a fault!
				
				// Close games which meet the following criteria:
				// 1. The game involves a player who is also involed in
				// 		a more recently created open game
				// 2. The game involves a player who is not currently a 
				// 		member of the lobby.
				ArrayList<InternetLobbyGame> games = lobbyGamePortfolio.getHostedGames() ;
				ArrayList<InternetGameMaintenancePortfolio.Record> records = new ArrayList<InternetGameMaintenancePortfolio.Record>() ;
				Iterator<InternetLobbyGame> game_iter = games.iterator() ;
				for ( ; game_iter.hasNext() ; )
					records.add(lobbyGamePortfolio.getRecord(game_iter.next())) ;
				
				// 1: close games with players who are in another game.
				for ( int i = 0; i < records.size(); i++ ) {
					InternetGameMaintenancePortfolio.Record ri = records.get(i) ;
					for ( int j = i+1; j < records.size(); j++ ) {
						// if i and j share a participant, close the older game.
						InternetGameMaintenancePortfolio.Record rj = records.get(j) ;
						boolean overlap = false ;
						
						if ( ri != null && rj != null ) {
							for ( int pl_i = 0; pl_i < ri.getNumPlayers(); pl_i++ ) {
								Nonce ni = ri.getPlayerPersonalNonce(pl_i) ;
								for ( int pl_j = 0; pl_j < rj.getNumPlayers(); pl_j++ ) {
									Nonce nj = rj.getPlayerPersonalNonce(pl_j) ;
									if ( ni.equals(nj) )
										overlap = true ;
								}
							}
						}
						
						if ( overlap ) {
							InternetGameMaintenancePortfolio.Record older_record = 
								ri.getHostedAge() > rj.getHostedAge() ? ri : rj ;
							lobbyGamePortfolio.close(older_record.getGame()) ;
							Log.d(TAG, "Closing lobbyGamePortfolio game for overlapping members " + older_record.getGame().getNonce()) ;
						}
					}
				}
				
				
				// 2: close games with non-lobby members.
				Nonce [] lobbyPersonalNonces = lobby.getPlayerPersonalNonces() ;
				Iterator<InternetGameMaintenancePortfolio.Record> iter = records.iterator() ;
				for ( ; iter.hasNext() ; ) {
					boolean closeThis = false ;
					InternetGameMaintenancePortfolio.Record record = iter.next() ;
					if ( record != null ) {
						for ( int i = 0; i < record.getNumPlayers(); i++ ) {
							Nonce personalNonce = record.getPlayerPersonalNonce(i) ;
							if ( personalNonce == null )
								closeThis = true ;
							else {
								boolean inLobby = false ;
								
								for ( int j = 0 ; j < lobbyPersonalNonces.length ; j++ )
									if ( personalNonce.equals(lobbyPersonalNonces[j]) )
										inLobby = true ;
								
								closeThis = closeThis || !inLobby ;
							}
						}
						
						if ( closeThis ) {
							lobbyGamePortfolio.close(record.getGame()) ;
							Log.d(TAG, "Closing lobbyGamePortfolio game for non-lobby members " + record.getGame().getNonce()) ;
						}
					}
				}
			}
		} ;
		
		
		// RUNNABLE ACHIEVEMENTS UPDATE:
		// Very simple!  Just tells the Achievements that we are still
		// in a lobby.
		mRunnableAchievementsUpdate = new Runnable() {
			@Override
			public void run() {
				if ( lobbyClientCommunications.isStarted() ) {
					mHandler.removeCallbacks(this) ;
					updateAchievementsStillInLobby() ;
					// repost
					mHandler.postDelayed(this, 60 * 1000) ;		// every minute.
				}
			}
		} ;
	}
	
	
	private void initializeLobbyMetaData( LobbyIntentPackage lip ) {
		activityState = ACTIVITY_STATE_NONE ;
	}
	
	
	private void resetAllMemberVariables() {
		lobbyUserInterface = null ;
		activity = null ;
		
		// Notifications
		if ( notificationAdapter != null ) {
    		notificationAdapter.dismissForegroundNotification() ;
    		notificationAdapter.setContext(null) ;
    	}
    	notificationAdapter = null ;
		
		// MY VARS
		lip = null ;
		
		// Local lobby objects
		lobby = null ;
		lobbyLog = null ;
		
		// Communication
		try {
			if ( lobbyClientCommunications != null ) {
				lobbyClientCommunications.stop() ;
				updateAchievementsLeaveLobby() ;
				mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			}
		} catch( Exception e ) { e.printStackTrace() ; }
		lobbyClientCommunications = null ;
		mppcPair = null ;
		
		// Lobby server
		try {
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
		} catch( Exception e ) { e.printStackTrace() ; }
		lobbyCoordinator = null ;
		try {
			if ( lobbyAnnouncer != null )
				lobbyAnnouncer.stop() ;
		} catch( Exception e ) { e.printStackTrace() ; }
		lobbyAnnouncer = null ;
		try {
			if ( mmReader != null )
				mmReader.kill() ;
		} catch( Exception e ) { e.printStackTrace() ; }
		mmReader = null ;
		
		// Lobby maintainer
		try {
			if ( lobbyMaintainer != null )
				lobbyMaintainer.stop() ;
		} catch( Exception e ) { e.printStackTrace() ; }
		lobbyMaintainer = null ;
		
		// portfolio
		try {
			if ( lobbyGamePortfolio != null )
				lobbyGamePortfolio.stop() ;
		} catch( Exception e ) { e.printStackTrace() ; }
		lobbyGamePortfolio = null ;
		
		// Host/Client status.
		playersPresentInMediatorUponFirstConnect = -1 ;		// 0 or 1, depending on whether we got to the mediator first.
		hasChangedRoles = false ;
		slotForFutureHost = -1 ;
		
		shouldStayInBackground = false ;
		alwaysForeground = false ;
	}
	
	
	/*
	 * *************************************************************************
	 * 
	 * WAKE LOCK COLLECTION
	 * 
	 * *************************************************************************
	 */
    
    private static final String LOCK_NAME_STATIC="com.peaceray.quantro.main.LobbyService";
	private volatile PowerManager.WakeLock lock=null;
	
    
    synchronized private PowerManager.WakeLock getLock(Context context) {
		if (lock==null) {
			PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
			    
			lock=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
			               						LOCK_NAME_STATIC);
			lock.setReferenceCounted(false);
		}
	  
		return(lock);
	}
    
    
    private static final String WIFI_LOCK_NAME_STATIC="com.peaceray.quantro.main.LobbyService.WIFI";
	private volatile WifiManager.WifiLock wifiLock=null;
	
    
    synchronized private WifiManager.WifiLock getWifiLock(Context context) {
		if (wifiLock==null) {
			WifiManager mgr=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			
			int mode = WifiManager.WIFI_MODE_FULL ;
    		if ( VersionCapabilities.getVersion() >= 12 ) {
    			// high-performance added in api 12
    			// it is a constant with value 0x3.
    			mode = 0x3 ;
    		}
			    
			wifiLock=mgr.createWifiLock(mode, WIFI_LOCK_NAME_STATIC);
			wifiLock.setReferenceCounted(false);
		}
	  
		return(wifiLock);
	}
    
    /**
     * Gets and "acquires" all necessary multiplayer locks.  This means a partial
     * wake lock, and (optionally) a WiFi lock.
     */
    synchronized private void getLocksAndAcquire() {
    	try {
			PowerManager.WakeLock wl = getLock(this) ;
			synchronized ( wl ) {
				wl.acquire() ;
			}
		} catch( Exception e ) { }
		
		// optional and user-controlled UNLESS lip is direct,
		// in which case we definitely want it.
		if ( lip.isDirect() || QuantroPreferences.getNetworkWifiLock(this) ) {
			try {
				WifiManager.WifiLock wl = getWifiLock(this) ;
				synchronized ( wl ) {
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
			PowerManager.WakeLock wl = getLock(this) ;
			synchronized ( wl ) {
				if ( wl.isHeld() )
					wl.release() ;
			}
		} catch( Exception e ) { }
		
		try {
			WifiManager.WifiLock wl = getWifiLock(this) ;
			synchronized ( wl ) {
				if ( wl.isHeld() )
					wl.release() ;
			}
		} catch ( Exception e ) { }
    }
    
    
    
    ////////////////////////////////////////////////////////////////////////////
    //
    // SIMPLE QUERYS
    
    public boolean getMovingToGame() {
    	return movingToGame ;
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
	
	
    private static final int NOTIFICATION_STATUS_CONNECTION_FAILURE = 0 ;
    private static final int NOTIFICATION_STATUS_CONNECTING = 1 ;
    private static final int NOTIFICATION_STATUS_NEGOTIATING = 2 ;
    private static final int NOTIFICATION_STATUS_IN_LOBBY = 3 ;
    private static final int NOTIFICATION_STATUS_LOBBY_CLOSED = 4 ;
    
    private static final int NOTIFICATION_TICKER_NONE = -1 ;
    private static final int NOTIFICATION_TICKER_CONNECTION_FAILURE = 0 ;
    private static final int NOTIFICATION_TICKER_MESSAGE = 1 ;
    private static final int NOTIFICATION_TICKER_PLAYER_ENTERED = 2 ;
    private static final int NOTIFICATION_TICKER_PLAYER_LEFT = 3 ;
    private static final int NOTIFICATION_TICKER_USER_IDLE = 4 ;
    private static final int NOTIFICATION_TICKER_USER_ACTIVE = 5 ;
    private static final int NOTIFICATION_TICKER_USER_IN_GAME = 6 ;
    
    
    
    // We display notifications which generally include the following text:
    // 'Connecting to <lobby name>'
    // 'Negotiating lobby connection'
    // 'Alone in <lobby name>'		or 		'With <name> in <lobby name>'
    // 'Lobby closed'
    
    // Additionally, these Notifications also include Ticker Text for important
    // events, such as lobby membership changing or a message being sent, e.g.,
    
    // <name>: <msg>
    // <name> entered <lobby name>
    // <name> left <lobby name>
    // Now idle in <lobby name>
    // Now active in <lobby name> 			<-- this is probably never used
    
    // When foregrounded, we place a Notification which may or may not include 
    // a Ticker.  While in the foreground we post new Notifications upon any
    // change; however, we only play a sound when Ticker updates occur
    // ('entered', 'left', 'msg').
    
    private int notificationStatus ;
    private boolean notificationForeground = false ;
    
    /**
     * The foreground toggle.
     */
    public void toggleForeground( boolean inForeground ) {
    	notificationForeground = inForeground ;
    	if ( notificationForeground ) {
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_NONE, null, null ) ) ;
    	}
    	else {
    		notificationAdapter.stopForegroundCompat( R.string.lobby_service_foreground_notification_id ) ;
    	}
    }
    
    
    /**
     * Terminal conneciton failure!
     */
    public void foregroundConnectionFailure() {
    	notificationStatus = NOTIFICATION_STATUS_CONNECTION_FAILURE ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_CONNECTION_FAILURE, null, null ) ) ;
    	
    }
    
    
    /**
     * Foreground set: connecting to server.
     */
    public void foregroundConnecting() {
    	notificationStatus = NOTIFICATION_STATUS_CONNECTING ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_NONE, null, null ) ) ;
    }
    
    /**
     * Foreground set: negotiating with server.
     */
    public void foregroundNegotiating() {
    	notificationStatus = NOTIFICATION_STATUS_NEGOTIATING ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_NONE, null, null ) ) ;
    }
    
    
    /**
     * Foreground set: connected to server.
     */
    public void foregroundConnected() {
    	notificationStatus = NOTIFICATION_STATUS_IN_LOBBY ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_NONE, null, null ) ) ;
    }
    
    
    /**
     * Foreground set: connected to server.
     */
    public void foregroundClosed() {
    	notificationStatus = NOTIFICATION_STATUS_LOBBY_CLOSED ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_NONE, null, null ) ) ;
    }
    
    
    
    
    public void foregroundMessage( String user, String msg ) {
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_MESSAGE, user, msg ) ) ;
    }
    
    public void foregroundPlayerEntered( String name ) {
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_PLAYER_ENTERED, name, null ) ) ;
    }
    
    public void foregroundPlayerLeft( String name ) {
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_PLAYER_LEFT, name, null ) ) ;
    }
    
    public void foregroundUserInGame(  ) {
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_USER_IN_GAME, null, null ) ) ;
    }
    
    public void foregroundUserIdle(  ) {
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_USER_IDLE, null, null ) ) ;
    }
     
    public void foregroundUserActive(  ) {
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_USER_ACTIVE, null, null ) ) ;
    }
    
    
    /**
     * Foreground set: we have disconnected FOREVER.  There will
     * be no reconnects.
     */
    public void foregroundDisconnectedForever() {
    	// TODO: Set a status for permanent disconnect.  May or
    	// may not be important.  Maybe there's a KICK message
    	// waiting for them?  Maybe the game's done and we have
    	// posted win/lose values?
    	notificationStatus = NOTIFICATION_STATUS_LOBBY_CLOSED ;
    	if ( notificationForeground )
    		notificationAdapter.startForegroundCompat( R.string.lobby_service_foreground_notification_id,
    				makeNotification( notificationStatus, NOTIFICATION_TICKER_NONE, null, null ) ) ;
    }
    
    
    /**
     * Makes and returns a Notification, appropriate for the specified
     * status at this time.
     * 
     * @param notificationStatus
     * @return
     */
    public Notification makeNotification( int notificationStatus, int tickerStatus, String tickerPlayerName, String tickerOtherText ) {
    	int role = ( lip.isClient() )
			? TextFormatting.ROLE_CLIENT 
			: TextFormatting.ROLE_HOST ;
    	
    	// Get the message text
    	String notificationText = "" ;
    	switch( notificationStatus ) {
    	case NOTIFICATION_STATUS_CONNECTION_FAILURE:
    		notificationText = res.getString(R.string.in_lobby_notification_connection_terminal_failure) ;
    		break ;
    	
    	case NOTIFICATION_STATUS_CONNECTING:
    		notificationText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_LOBBY_CONNECTING,
    				role, -1, lobby ) ;	
    		break ;
    	case NOTIFICATION_STATUS_NEGOTIATING:
    		notificationText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_LOBBY_NEGOTIATING,
    				role, -1, lobby ) ;	
    		break ;
    	case NOTIFICATION_STATUS_IN_LOBBY:
    		notificationText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_LOBBY_READY,
    				role, -1, lobby ) ;
    		break ;
    	case NOTIFICATION_STATUS_LOBBY_CLOSED:
    		notificationText = TextFormatting.format(this, res,
    				TextFormatting.DISPLAY_NOTIFICATION,
    				TextFormatting.TYPE_LOBBY_CLOSED_FOREVER,
    				role, -1, lobby ) ;	
    		break ;
    	}
    	
    	// get the ticker text (if any)
    	String tickerText = null ;
    	switch( tickerStatus ) {
    	case NOTIFICATION_TICKER_NONE:
    		break ;
    	case NOTIFICATION_TICKER_CONNECTION_FAILURE:
    		tickerText = res.getString(R.string.in_lobby_notification_ticker_connection_terminal_failure) ;
    		break ;
    	case NOTIFICATION_TICKER_MESSAGE:
    		tickerText = res.getString(R.string.in_lobby_notification_ticker_message) ;
    		break ;
    	case NOTIFICATION_TICKER_PLAYER_ENTERED:
    		tickerText = res.getString(R.string.in_lobby_notification_ticker_player_entered) ;
    		break ;
    	case NOTIFICATION_TICKER_PLAYER_LEFT:
    		tickerText = res.getString(R.string.in_lobby_notification_ticker_player_left) ;
    		break ;
    	case NOTIFICATION_TICKER_USER_IN_GAME:
    		tickerText = res.getString(R.string.in_lobby_notification_ticker_user_in_game) ;
    		break ;
    	case NOTIFICATION_TICKER_USER_IDLE:
    		tickerText = res.getString(R.string.in_lobby_notification_ticker_user_idle) ;
    		break ;
    	case NOTIFICATION_TICKER_USER_ACTIVE:
    		tickerText = res.getString(R.string.in_lobby_notification_ticker_user_active) ;
    		break ;
    	}
    	// substitute string1, string2, and lobby name.
    	String placeholder_lobbyName = res.getString(R.string.placeholder_lobby_name) ;
    	String placeholder_string1 = res.getString(R.string.placeholder_names_array_name_1) ;
    	String placeholder_string2 = res.getString(R.string.placeholder_names_array_name_2) ;
    	String lobbyName = lobby.getLobbyName() ;
    	if ( tickerText != null ) {
    		if ( lobbyName != null )
    			tickerText = tickerText.replace( placeholder_lobbyName, lobbyName ) ;
    		if ( tickerPlayerName != null )
    			tickerText = tickerText.replace( placeholder_string1, tickerPlayerName ) ;
    		if ( tickerOtherText != null )
    			tickerText = tickerText.replace( placeholder_string2, tickerOtherText ) ;
    	}
    	
    	String titleText = res.getString(R.string.in_lobby_notification_title) ;
    	
    	// TODO: Get the message icon, and a "big icon."
    	int icon = R.drawable.notification ;
    	Bitmap largeIcon = null ;
    	
    	// Get a sound URI for the ticker event.
    	Uri soundUri = null ;
    	QuantroSoundPool qsp = ((QuantroApplication)getApplication()).getSoundPool(null) ;
    	if ( qsp != null ) {
    	switch( tickerStatus ) {
	    	case NOTIFICATION_TICKER_MESSAGE:
	    		soundUri = Uri.parse("android.resource://com.peaceray.quantro/"
	    				+ qsp.getSoundResID(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_CHAT)) ; 
	    		break ;
	    	case NOTIFICATION_TICKER_PLAYER_ENTERED:
	    		soundUri = Uri.parse("android.resource://com.peaceray.quantro/"
	    				+ qsp.getSoundResID(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_JOIN)) ; 
	    		break ;
	    	case NOTIFICATION_TICKER_PLAYER_LEFT:
	    		soundUri = Uri.parse("android.resource://com.peaceray.quantro/"
	    				+ qsp.getSoundResID(QuantroSoundPool.SOUND_TYPE_LOBBY_OTHER_QUIT)) ; 
	    		break ;
	    	case NOTIFICATION_TICKER_USER_IDLE:
	    		soundUri = Uri.parse("android.resource://com.peaceray.quantro/"
	    				+ qsp.getSoundResID(QuantroSoundPool.SOUND_TYPE_LOBBY_USER_GO_INACTIVE)) ; 
	    		break ;
	    	case NOTIFICATION_TICKER_USER_ACTIVE:
	    		soundUri = Uri.parse("android.resource://com.peaceray.quantro/"
	    				+ qsp.getSoundResID(QuantroSoundPool.SOUND_TYPE_LOBBY_USER_GO_ACTIVE)) ; 
	    		break ;
	    	}
    	}
    	
    	// Use a QuantroNotificationMaker.
    	QuantroNotificationMaker maker = QuantroNotificationMaker.getNew(this) ;
    	
    	// Set icon and sound
    	maker.setSmallIcon(icon) ;
    	maker.setLargeIcon(largeIcon) ;
    	if ( !QuantroPreferences.getMuted(this) )
    		maker.setSound(soundUri) ;
    		// only set the sound if we are not currently muted.
    		// It's okay to use the preference and not the sound pool
    		// for this, because even if muteWithRinger is true, it
    		// still won't make a notification sound (because the ringer
    		// is muted!)
    	
    	// Set title and text
    	maker.setContentTitle( titleText ) ;
    	maker.setContentText(notificationText) ;
    	
    	// Set ticker ( if we have one )
    	if ( tickerText != null )
    		maker.setTicker(tickerText) ;
    	
    	// If includes ticker text, alert the user.  Otherwise don't.
    	maker.setOnlyAlertOnce( tickerText == null ) ;
    	
    	// What happens when clicked?  Well, basically, we return to this
    	// Activity -- UNLESS we are in a game, in which case we attempt to
    	// quit the game.
    	Intent notificationIntent ;
    	if ( movingToGame ) {
    		notificationIntent = new Intent(LobbyService.this, GameActivity.class ) ;
	    	notificationIntent.setAction( GameActivity.INTENT_ACTION_QUIT_TO_LOBBY ) ;
	    	notificationIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP ) ;
    	} else {
    		notificationIntent = new Intent(LobbyService.this, LobbyActivity.class ) ;
	    	notificationIntent.setAction( Intent.ACTION_MAIN ) ;
	    	notificationIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP ) ;
	    	notificationIntent.putExtra(res.getString( R.string.intent_extra_lobby_intent_package), lip) ;
	    	Log.d(TAG, "notification Intent is " + lip.connection) ;
    	}
    	// Consider using FLAG_CANCEL_CURRENT rather than FLAG_UPDATE_CURRENT.
    	// The difference is that with CANCEL_CURRENT, only and exactly this extra
    	// data will be available to only and exactly the recipient of this Intent.
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT) ;
    	maker.setContentIntent(contentIntent) ;
    	
    	// Oh, and when?  Now!
    	maker.setWhen(System.currentTimeMillis()) ;
    	
    	// Return 
    	return maker.getNotification() ;
    }
    
    
    private Hashtable<Integer, Serializable> getPremiumAuthTokens() {
    	PremiumLibrary premiumLibrary = ((QuantroActivity)activity).getPremiumLibrary() ;
    	
    	// for now, do a trivial, "trust everyone" approach.
    	// All we send is the Boolean value TRUE for premium content we have.
    	int [] modes = GameModes.getMultiPlayerGameModes() ;
    	Hashtable<Integer, Serializable> authTokens = new Hashtable<Integer, Serializable>() ;
    	for ( int i = 0; i < modes.length; i++ ) {
    		int gameMode = modes[i] ;
    		if ( premiumLibrary.hasPremiumGameMode(gameMode) )
    			authTokens.put(gameMode, Boolean.TRUE) ;
    	}
    	
    	return authTokens ;
    }
    
    private boolean isValidAuthToken( int gameMode, Serializable authToken ) {
    	return Boolean.TRUE.equals(authToken) ;
    }
	
    
    
	/*
	 * *************************************************************************
	 * 
	 * ACCESSORS
	 * 
	 * Accessors to fields.
	 * 
	 * *************************************************************************
	 */
	
    public Lobby getLobby() {
    	return lobby ;
    }
    
    public LobbyLog getLobbyLog() {
    	return lobbyLog ;
    }
    
    
    
    
	/*
	 * *************************************************************************
	 * 
	 * LOBBY INTERACTION
	 * 
	 * Some things a user does in a Lobby (interacting with a LobbyView) cannot
	 * be handled directly by the Lobby object.  Instead, we allow the Activity
	 * to call these methods, indirectly affecting our lobbyClientCommunications.
	 * 
	 * *************************************************************************
	 */
    
    
    public void sendName( String name ) {
    	lobbyClientCommunications.sendName(name) ;
    }
    
    
    public void sendVote( int gameMode, boolean voteFor ) {
    	//Log.d(TAG, "sendVote: " + gameMode + ", " + voteFor) ;
    	if ( voteFor )
    		lobbyClientCommunications.voteFor(gameMode) ;
    	else
    		lobbyClientCommunications.unvoteFor(gameMode) ;
    }
    
    public void sendTextMessage( String text ) {
    	lobbyClientCommunications.sendTextMessage(text) ; 
    }
    
    public void updateGameModeAuthorization( int gameMode, boolean hasAuthority, boolean forceUpdate ) {
    	Serializable authToken = null ;
    	if ( hasAuthority )
    		authToken = Boolean.TRUE ;
    	lobbyClientCommunications.updateGameModeAuthToken(gameMode, authToken, forceUpdate) ;
    }
    
    public boolean getGameModeIsAvailable( int gameMode ) {
    	PremiumLibrary premiumLibrary = ((QuantroActivity)activity).getPremiumLibrary() ;
    	return premiumLibrary.hasGameMode(gameMode)
    			|| this.isValidAuthToken( gameMode, lobbyClientCommunications.getCurrentAuthToken(gameMode) ) ;
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
	 * Additionally, when the user chooses to quit the activity, the lobbyActivityFinishing()
	 * method is called.  This has the effect of telling the Service that as the
	 * activity shuts down, it will not start back up - and that it will go all
	 * the way through to onDestroy().
	 * 
	 * Once we quit, we will only restart the lobby by building everything up
	 * from scatch again.
	 * 
	 * *************************************************************************
	 */
    
    
    /**
     * Provides a references to the Activity  (probably a LobbyActivity);
     * we will use this for setting up Intents (to Resume() the activity when
     * needed).
     */
    public void setActivity( Activity activity, LobbyIntentPackage lip ) {
    	
    	// quit any current games.
		GameService gs = ((QuantroApplication)getApplication()).getGameService() ;
		if ( gs != null ) {
			GameActivity ga = (GameActivity)gs.activity ;
			if ( ga != null )
				ga.quitGame(true) ;
		}
    	
    	if ( this.lip != null && this.lip.isSameLobbyIntent(lip) && alwaysForeground ) {
    		// This is the same lobby to which we are currently connected.
    		
    		Log.d(TAG, "setActivity: same lobby as before, and currently connected") ;
    		
    		// same lobby as before, and we are currently connected;
    		// it looks like this Activity was destroyed and recreated,
    		// or (maybe) created in a different Task.  It's becoming more
    		// and more important that we 
    		Activity a = this.activity ;
    		this.activity = activity ;
    		this.activityState = ACTIVITY_STATE_NONE ;
    		
    		try {
	    		if ( a != null )
	    			a.finish() ;
    		} catch( Exception e ) {
    			e.printStackTrace() ;
    		}
    		
    		// we require a re-welcome; otherwise the Activity
    		// doesn't have the right information.  Implement this.
    		if ( lobbyClientCommunications != null )
    			lobbyClientCommunications.sendRewelcomeRequest() ;
    		
    		return ;
    	}
    	
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
        this.lip = lip ;
    	
    	// LOCK!
    	this.getLocksAndAcquire() ;
    	
    	// STEP ZERO!  Because we want notifications to work on >= 2.0 and < 2.0, we
		// need to use special wrappers to those notifications.  We call this method
		// to set it up.  An instance var, set inside this method, ensures that the
		// operations only happen once (the first time it is called) in case of numerous
		// "onBind"s.
    	if ( notificationAdapter == null )
    		notificationAdapter = new NotificationAdapter(this) ;
		
		// We bind with a particular intent; that intent MUST include
		// a LobbyIntentPackage.
		
        // Set resources for GameModes.
        res = getResources() ;
		
        // STEP ONE:
        // Allocate our lobby object
        
        if ( !lip.isMatchseeker() ) {
	        lobby = new Lobby() ;
	        lobby.setDelegate(this) ;
	        lobby.setLobbyNonce(lip.nonce) ; 
	        lobby.setSessionNonce(lip.nonce) ;
	        if ( lip.isClient() )
	        	lobby.setDirectAddress(lip.socketAddress[0]) ;
	        else if ( lip.isHost() ) {
	        	String ipAddress = WifiMonitor.ipAddressToString(WifiMonitor.getWifiIpAddress(this)) ;
	        	lobby.setDirectAddress( new InetSocketAddress(ipAddress, lip.localPort ) ) ;
	        }
	        lobby.setLobbyName(lip.lobbyName) ;
	        if ( lip.isHost() ) {
	        	// This prevents a bug where the Announcer would prematurely broadcast
	        	// unset Lobby information, causing a crash in the receiver.
	        	// TODO: set max players if we allow more.
	        	lobby.setMaxPlayers(lip.lobbySize) ;
	        }
        } else {
        	lobby = lip.internetLobby.newInstance() ;
        	lobby.setDelegate(this) ;
        }
        
        lobbyLog = new LobbyLog() ;		
        lobbyLog.setDelegate(this) ;
        lobbyLog.allocate() ;		// pre-allocate events.
        
        alwaysForeground = lip.isMatchseeker() || lip.lobbySize > 2 ;
        
		mHandler = new Handler() ;
        
        // STEP TWO:
        // Client communications
        allocatedAndInitializeClientConnection( lobby, lobbyLog, lip ) ;
        
        // STEP THREE:
        // Server objects
        allocateAndInitializeCoordinatorAndAnnouncer( lip ) ;
        
        // STEP THREE POINT FIVE:
        // Server communication failure handlers and Runnables
        if ( lip.isMatchseeker() )
        	allocateAndInitializePeaceRayServerCommunicationRunnables( lip ) ;
        
        // STEP FOUR:
        // Initialize all our other variables
        initializeLobbyMetaData( lip ) ;
		
        // Construct and return a ServicePassingBinder.  The only function
        // of this binder is to allow the bound Activity direct access to
        // this Service instance.  The Activity will then call
        // setLobbyUserInterface and the appropriate state update methods.
    }

	/**
	 * Provides a reference to the GameUserInterface (probably a lobbyActivity)
	 * to the Service.  This is a necessary call if you expect anything to happen
	 * on screen!
	 */
	public void setLobbyUserInterface( LobbyUserInterface lui ) {
		mUseUserInterface = lui != null ;
		if ( lui != null )
			lobbyUserInterface = lui ;
	}
	
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onCreate method.
	 */
	public void lobbyActivityDidCreate( Activity activity ) {
		if ( activity != this.activity )
			return ;
		if ( this.activityState != ACTIVITY_STATE_NONE )
			return ;
		
		// Here in didCreate, we start our communications going.
		
		if ( lip.isHost() && mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInLobbyCreated(lip.isDirect()) ;
		
		if ( lobbyCoordinator != null && !lobbyCoordinator.isStarted() )
			lobbyCoordinator.start() ;
		if ( lobbyAnnouncer != null && !lobbyAnnouncer.isStarted() )
			lobbyAnnouncer.start( new WeakReference<Handler>(mHandler) ) ;
		if ( lobbyMaintainer != null && !lobbyMaintainer.isStarted() )
			lobbyMaintainer.start() ;
		if ( lobbyGamePortfolio != null && !lobbyGamePortfolio.isStarted() )
			lobbyGamePortfolio.start() ;
		
		if ( !lobbyClientCommunications.isStarted() ) {
			lobbyClientCommunications.start() ;
			updateAchievementsEnterLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			mHandler.postDelayed(mRunnableAchievementsUpdate, 60 * 1000) ;
		}
		
		// Note state change
		activityState = ACTIVITY_STATE_CREATED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onStart method.
	 */
	public void lobbyActivityDidStart( Activity activity ) {
		if ( activity != this.activity )
			return ;
		if ( this.activityState != ACTIVITY_STATE_CREATED )
			return ;
		
		// If we stayed in the background, start things up.
		if ( shouldStayInBackground && !alwaysForeground ) {
			if ( lobbyCoordinator != null )
				lobbyCoordinator.start() ;
			if ( lobbyAnnouncer != null )
				lobbyAnnouncer.start( new WeakReference<Handler>(mHandler) ) ;
			
			lobbyClientCommunications.start() ;
			updateAchievementsEnterLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			mHandler.postDelayed(mRunnableAchievementsUpdate, 60 * 1000) ;
		}
		
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInLobbyActivityStart(shouldStayInBackground) ;
		
		shouldStayInBackground = false ;
		
		// name changes?
		String name = QuantroPreferences.getMultiplayerName(this) ;
		lobbyClientCommunications.sendNameIfChanged(name) ;
		if ( lobbyCoordinator != null )
			lobbyCoordinator.setOwnerName(name) ;
		
		// Note state change
		activityState = ACTIVITY_STATE_STARTED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onResume method.
	 */
	public void lobbyActivityDidResume( Activity activity ) {
		if ( activity != this.activity )
			return ;
		if ( this.activityState != ACTIVITY_STATE_STARTED )
			return ;
		
		// If the game has resumed, it is ready to start things moving
		// again.  Now's a good time to unpause the game... IF
		// we paused it before!
		
		// If in the foreground, stop being in the foreground.
		this.toggleForeground(false) ;
		
		// If we have no reason to pause the game, tell the 
		// server to unpause it.  For now, the only way to pause
		// is to leave the activity.
		// Go active.
		lobbyClientCommunications.sendActive() ;
		
		movingToGame = false ;
		
		// Note state change
		activityState = ACTIVITY_STATE_RESUMED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onPause method.
	 */
	public void lobbyActivityDidPause( Activity activity ) {
		if ( activity != this.activity )
			return ;
		if ( this.activityState != ACTIVITY_STATE_RESUMED )
			return ;
		
		// The Activity is on-screen but partially obscured.
		// Dunno what causes this... a pop-up?
		
		// Go idle.
		if ( movingToGame )
			lobbyClientCommunications.sendInGame() ;
		else
			lobbyClientCommunications.sendInactive() ;
		
		// Make this Service a Foreground service - IF we are not going to the game,
		// AND we are not quiting.
		if ( !shouldStayInBackground || alwaysForeground )
			this.toggleForeground(true) ;
		
		
		// Note state change
		activityState = ACTIVITY_STATE_STARTED ;
	}
	
	/**
	 * Notifies the Service that the game Activity has completed
	 * - or almost completed - its onStop method.
	 */
	public void lobbyActivityDidStop( Activity activity ) {
		if ( activity != this.activity )
			return ;
		if ( this.activityState != ACTIVITY_STATE_STARTED )
			return ;
		
		// The activity has left the screen.
		
		if ( shouldStayInBackground && !alwaysForeground ) {
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
			if ( lobbyAnnouncer != null )
				lobbyAnnouncer.stop() ;
		}
		
		
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInLobbyActivityStop(shouldStayInBackground) ;
		
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
	public void lobbyActivityDidDestroy( Activity activity ) {
		if ( activity != this.activity )
			return ;
		if ( this.activityState != ACTIVITY_STATE_CREATED )
			return ;
		
		// Activity destroyed!
		// Should we do any clean-up here?  We expect to be unbound
		// very shortly, maybe we can handle that in our unbind method...
		
		// Always Foreground: this indicates that we always want to remain connected,
		// even if our Activity is destroyed.
		if ( !alwaysForeground ) {
			// Remove our notification.
			this.toggleForeground(false) ;
			
			// At the very least, we should disconnect everything and shut down
			// sockets / threads.
			// Stop() calls are safe so long as we have start()ed at least once,
			// which we have (we have to to even get here, based on how the
			// Activity calls these methods).
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
			if ( lobbyAnnouncer != null )
				lobbyAnnouncer.stop() ;
			
			// Immediately give up the WakeLock, if we have one.
			getLocksAndReleaseIfHeld() ;
		}
		
		this.activityState = ACTIVITY_STATE_NONE ;
	}
	
	
	public void lobbyActivityQuitting( Activity activity ) {
		Log.d(TAG, "lobbyActivityQuitting " + activity) ;
		if ( activity != this.activity )
			return ;
		
		
		// Always Foreground: this is when we close our connections.
		if ( alwaysForeground ) {
			
			if ( lip.isHost() && mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
				Analytics.logInLobbyClosed(lip.isDirect(), lobby.getAge(), lobby.numLaunches()) ;
			
			shouldStayInBackground = true ;
			// TODO: Bug.  This is sometimes not yet started, e.g. if still connecting to Internet Challenge.
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
			if ( lobbyAnnouncer != null )
				lobbyAnnouncer.stop() ;
			if ( lobbyMaintainer != null && lobbyMaintainer.isStarted() )
				lobbyMaintainer.stop() ;
			if ( lobbyGamePortfolio != null && lobbyGamePortfolio.isStarted() )
				lobbyGamePortfolio.stop() ;
			
			// Immediately give up the WakeLock, if we have one.
			getLocksAndReleaseIfHeld() ;
			
			alwaysForeground = false ;
		}
		
		// try closing the lobby.
		if ( lobby instanceof MutableInternetLobby )
			new ThreadCloseLobby( (MutableInternetLobby)lobby ).start() ;
	}
	
	
	/**
	 * We are quitting.
	 */
	public void lobbyActivityFinishing( Activity activity ) {
		if ( activity != this.activity )
			return ;
		
		// Always foreground indicates that we always want to remain connected,
		// even if the Activity is Finished.  Only upon ActivityQuitting do we close
		// our connections.
		if ( !alwaysForeground ) {
			if ( lip.isHost() && mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
				Analytics.logInLobbyClosed(lip.isDirect(), lobby.getAge(), lobby.numLaunches()) ;
			
			shouldStayInBackground = true ;
			// TODO: Bug.  This is sometimes not yet started, e.g. if still connecting to Internet Challenge.
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
			if ( lobbyAnnouncer != null )
				lobbyAnnouncer.stop() ;
			
			// Immediately give up the WakeLock, if we have one.
			getLocksAndReleaseIfHeld() ;
		}
	}
	
	
	
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	// LOBBY DELEGATE METHODS
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Called when a player joins the lobby.
	 * 
	 * @param slot The slot for the new player.
	 * @param name The name of the player who just joined, provided as a convenience.
	 * @param lobby
	 */
	public void ld_memberJoinedLobby( int slot, String name, Lobby lobby ) {
		// Log.d(TAG, "ld_memberJoinedLobby") ;
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyMembership() ;
		
		this.foregroundPlayerEntered(name) ;
		
		if ( lobbyMaintainer != null && lobbyMaintainer.canHost() )
			lobbyMaintainer.host() ;
	}
	
	
	/**
	 * Called when the specified player leaves the lobby.
	 * 
	 * @param slot The slot the player just left.
	 * @param name The name of the player who left the lobby, provided as a convenience.
	 * @param lobby
	 */
	public void ld_memberLeftLobby( int slot, String name, Lobby lobby ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyMembership() ;
		
		this.foregroundPlayerLeft(name) ;
		
		if ( lobbyMaintainer != null && lobbyMaintainer.canHost() )
			lobbyMaintainer.host() ;
	}
	
	
	/**
	 * Called when the specified player changes their name.
	 * This method is NOT called when a player joins.
	 * 
	 * @param slot The slot of the player whose name changed.
	 * @param oldName The old player name.
	 * @param newName The new name for the player.
	 * @param lobby
	 */
	public void ld_memberChangedName( int slot, String oldName, String newName, Lobby lobby ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyMembership() ;
	}
	
	
	/**
	 * The member changed status.  This is a forward-method, since we don't have
	 * status yet, but (for e.g.) a player pressing "Home" should put them in
	 * a particular status, such as "Idle" or something.
	 * 
	 * @param slot
	 * @param oldStatus
	 * @param newStatus
	 * @param lobby
	 */
	public void ld_memberChangedStatus( int slot, int oldStatus, int newStatus, Lobby lobby ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyMembership() ;
	}
	
	
	/**
	 * A new message!
	 * @param fromSlot The slot of the player who sent the message.
	 * @param msg The text of the message itself.
	 * @param lobby The Lobby object.
	 */
	public void ld_newMessage( int fromSlot, String msg, Lobby lobby ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyMessages() ;
	}
	
	
	
	/**
	 * The available game modes have changed.
	 * 
	 * @param lobby
	 */
	public void ld_updatedGameModes( Lobby lobby ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyGameModes() ;
	}
	
	
	/**
	 * The votes for the specified game mode have changed.
	 * 
	 * @param mode
	 * @param lobby
	 */
	public void ld_updatedGameModeVotes( int mode, Lobby lobby ) {
		// Log.d(TAG, "ld_updatedGameModeVotes") ;
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyVotes() ;
	}

	/**
	 * We have a new countdown.
	 * 
	 * @param countdownNumber
	 * @param gameMode
	 * @param lobby
	 */
	public void ld_newCountdown( int countdownNumber, int gameMode, Lobby lobby ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyCountdowns_newCountdown() ;
		
		if ( lobbyGamePortfolio != null && lobbyGamePortfolio.isStarted() )
			lobbyGamePortfolio.prepareForHostNewGame() ;
		
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInLobbyCountdownStart(lip.isDirect(), gameMode) ;
	}
	
	/**
	 * The specified countdown has updated.
	 * @param countdownNumber
	 * @param lobby
	 */
	public void ld_updatedCountdown( int countdownNumber, int gameMode, Lobby lobby ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyCountdowns() ;
		
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			if ( lobby.getCountdownStatus(countdownNumber) == Lobby.COUNTDOWN_STATUS_HALTED )
				Analytics.logInLobbyCountdownHalted(lip.isDirect(), gameMode) ;
	}
	
	
	/**
	 * We are REMOVING the specified countdown, but it has not yet been removed.
	 * The countdown may thus be accessed DURING this delegate call, but not
	 * afterwards.
	 * 
	 * @param countdownNumber
	 * @param gameMode
	 * @param lobby
	 */
	public void ld_removingCountdown( int countdownNumber, int gameMode, Lobby lobby, boolean failure ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyCountdowns_cancelCountdown( failure ) ;
		
		if ( mAnalyticsInSession && QuantroPreferences.getAnalyticsActive(this) )
			Analytics.logInLobbyCountdownAborted(lip.isDirect(), gameMode) ;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	// LOBBY COORDINATOR DELEGATE METHODS
	////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Do we need a valid Authorization Token for this game mode?  If 'true', we only
	 * begin countdowns for game modes for which we have a verified auth token.
	 */
	public boolean lcd_requestNeedsAuthToken( int gameMode ) {
		return ((QuantroActivity)activity).getPremiumLibrary().isPremiumGameMode(gameMode) ;
	}
	
	
	/**
	 * Requests that the provided Authorization token be verified as OK for use.
	 * 
	 * If 'true' is returned, we assume that the provided Auth token is valid to
	 * access the provided game mode and distribute it (if needed) to the connected
	 * clients.
	 * 
	 * If 'false' is returned, we do not store the token, and might even kick the
	 * sender.
	 * 
	 * For that reason, please assume that 'null' tokens (or Boolean "TRUE", for
	 * example) qualify as acceptable Authorization tokens for non-premium, 
	 * universal content.
	 */
	public boolean lcd_requestVerifyAuthToken( int gameMode, Serializable authToken ) {
		PremiumLibrary premiumLibrary = ((QuantroActivity)activity).getPremiumLibrary() ;
		if ( premiumLibrary.isPremiumGameMode(gameMode) )
			return isValidAuthToken( gameMode, authToken ) ;
		
		// If not premium, any auth token is ok.
		return true ;
	}
	
	
	
	/**
	 * Requests the default player color to apply to someone with the provided player slot,
	 * lobby nonce and personalNonce.  This method should always return the same color
	 * if provided the same slot, nonce and pnonce.  It is up to the delegate whether it
	 * returns the same color when given slightly different input (e.g., a different 
	 * player slot).
	 * 
	 * @returns A color for this player, which must not be 0 = 0x00000000.
	 */
	public int lcd_requestDefaultPlayerColor( int playerSlot, Nonce n, Nonce personalNonce ) {
		// Log.d(TAG, "lcd_requestDefaultPlayerColor") ;
		
		if ( !mUseUserInterface )
			return 0 ;
		
		return lobbyUserInterface.lui_getDefaultPlayerColor( !lip.isMatchseeker(), playerSlot, n, personalNonce ) ;
	}
	
	
	/**
	 * Requests the list of game modes available in this lobby.  The value returned
	 * should be an integer array of length at least 1, with the game mode numbers
	 * of all available game modes.  This method will generally be called once per
	 * LobbyCoordinator.  This list of game modes will be used to allocate structures
	 * in the LobbyCoordinator, and will be provided to connected clients.
	 */
	public int [] lcd_requestGameModesAvailable( LobbyCoordinator lobbyCoordinator ) {
		// Log.d(TAG, "lcd_requestGameModesAvailable") ;
		// TODO: Implement checks for availability?
		
		// For now, only included game types are available (no custom).
		ArrayList<Integer> modesList = new ArrayList<Integer>() ;
		Iterator<Integer> iter = GameModes.iteratorIncludedMultiPlayer() ;
		for ( ; iter.hasNext() ; ) {
			Integer mode = iter.next() ;
			if ( GameModes.minPlayers(mode) <= this.lobby.getMaxPeople() ) {
				// this lobby could theoretically hold enough people to qualify for
				// this game mode.
				modesList.add(mode) ;
			}
		}
		
		int [] modes = new int[ modesList.size() ] ;
		for ( int i = 0; i < modesList.size(); i++ )
			modes[i] = modesList.get(i).intValue() ;
		return modes ;
	}
	
	
	/**
	 * Requests the XML text describing the specified game mode.  We guarantee that
	 * this method will be called after lcd_requestGameModesAvailable (if this method
	 * is called at all) and that the gameMode specified was one of those provided 
	 * in the return value of that call.
	 * 
	 * @param lobbyCoordinator
	 * @param gameMode
	 * @return
	 */
	public String lcd_requestGameModeXMLText( LobbyCoordinator lobbyCoordinator, int gameMode ) {
		// TODO: Implement this.
		return null ;
	}
	
	
	/**
	 * Requests that the delegate prepare (or at least investigate) a Game server
	 * for an upcoming game, and set the relevant fields in the provided LobbyCoordinator.
	 * 
	 * If this method returns 'true', then the next Game server information has been
	 * determined, and the following methods have been called on the LobbyCoordinator:
	 * 		public void setNextGameNonce( Nonce nonce )
	 * 		public void setNextGamePort( int port ) 
	 * 		public void setNextGameAddress( String addrAsString ) 
	 * 		public void setNextGameHost( int slot ) 
	 * 		public void setNextGameHostIsMediated( boolean mediated ) 
	 * 		public void setNextGameHostIsMatchseeker( boolean matchseeker )
	 * 
	 * and if setNextGameHostIsMatchseeker( true ), we also call
	 * 
	 * 		public void setNextGameEditKey( Nonce editKey ) ;
	 * 			// this ILG has already been assigned and associated with the specified players.
	 * 
	 * @param lobbyCoordinator The LobbyCoordinator making the call
	 * @param gameMode: we will be running a server for the specified game mode.
	 * @param playerIncluded: A boolean array determining whether the specified player is
	 * 				included in the game launch.
	 * @param personalNonces: The PersonalNonces of all players, if relevant.  Specifically,
	 * 				you should note the personal nonces of those players specified in
	 * 				playerIncluded.
	 * @return
	 */
	public boolean lcd_requestGameAddressAndHostingInformation( LobbyCoordinator lobbyCoordinator, int gameMode, boolean [] playerIncluded, Nonce [] personalNonces, SocketAddress [] remoteSocketAddresses ) {
		// Log.d(TAG, "lcd_requestGameAddressAndHostingInformation(..., " + gameMode + ")") ;
		// We need an address for the game mode.  Since we are acting
		// as the host, we give our own address.
		try {
			SocketAddress nextGameAddr ;
			SocketAddress nextGameUDPAddr ;
			int nextGameHostSlot ;
			Nonce nextGameNonce ;
			Nonce nextGameEditKey ;
			if ( lip.isMatchseeker() ) {
				//Log.d(TAG, "requesting matchseeker server information") ;
				//for ( int i = 0; i < playerIncluded.length; i++ )
				//	Log.d(TAG, "player " + i + " included " + playerIncluded[i] + " pnonce " + personalNonces[i]) ;
				// InternetLobby!
				int numPlayers = 0 ;
				for ( int i = 0; i < playerIncluded.length; i++ )
					if ( playerIncluded[i] )
						numPlayers++ ;
				Nonce [] ppNonces = new Nonce[numPlayers] ;
				numPlayers = 0 ;
				int hostIndex = -1 ;
				nextGameHostSlot = -1 ;
				for ( int i = 0; i < playerIncluded.length; i++ ) {
					if ( playerIncluded[i] ) {
						if ( hostIndex == -1 ) {
							hostIndex = numPlayers ;
							nextGameHostSlot = i ;
						}
						ppNonces[numPlayers++] = personalNonces[i] ;
					}
				}
				InternetLobbyGame ilg = lobbyGamePortfolio.hostNewGame(ppNonces, hostIndex) ;
				
				if ( ilg == null ) {
					// Log.d(TAG, "no hostable game available") ;
					return false ;
				}
				
				nextGameAddr = new InetSocketAddress( "peaceray.com", ilg.getMediatorPort() ) ;
				nextGameUDPAddr = new InetSocketAddress( "peaceray.com", ilg.getMediatorPort() ) ;
				
				nextGameNonce = ilg.getNonce() ;
				nextGameEditKey = ilg.getEditKey() ;
			} else {
				// find an appropriate host
				nextGameHostSlot = -1 ;
				for ( int i = 0; i < playerIncluded.length; i++ ) {
					if ( playerIncluded[i] && nextGameHostSlot == -1 )
						nextGameHostSlot = i ;
				}
				
				// get the address from the list, or this user's address if
				// this is our local player slot.
				try {
					if ( nextGameHostSlot == lobby.getLocalPlayerSlot() ) {
						nextGameAddr = new InetSocketAddress(
								WifiMonitor.ipAddressToString( WifiMonitor.getWifiIpAddress(this) ),
								res.getInteger( R.integer.wifi_multiplayer_game_port ) ) ;
					} else {
						nextGameAddr = new InetSocketAddress(
								((InetSocketAddress)remoteSocketAddresses[nextGameHostSlot]).getAddress(),
								res.getInteger( R.integer.wifi_multiplayer_game_port ) ) ;
					}
				} catch ( Exception e ) {
					e.printStackTrace() ;
					return false ;
				}
				
				nextGameUDPAddr = nextGameAddr ;
				nextGameNonce = lip.nonce ;
				nextGameEditKey = null ;
			}
			//Log.d(TAG, "setting next game socket address as " + nextGameAddr) ;
			//Log.d(TAG, "setting next game UDP socket address as " + nextGameUDPAddr) ;
			lobbyCoordinator.setNextGameSocketAddress( nextGameAddr ) ;
			lobbyCoordinator.setNextGameUDPSocketAddress(nextGameUDPAddr) ;
			lobbyCoordinator.setNextGamePort( res.getInteger( R.integer.wifi_multiplayer_game_port ) ) ;
			lobbyCoordinator.setNextGameNonce( nextGameNonce ) ;
			lobbyCoordinator.setNextGameHost( nextGameHostSlot ) ;		// We are the host.
			lobbyCoordinator.setNextGameHostIsMatchseeker( lip.isMatchseeker() ) ;
			lobbyCoordinator.setNextGameEditKey( nextGameEditKey ) ;
			
			return true ;
		} catch( Exception e ) {
			// Problem?
			e.printStackTrace() ;
			//Log.d( TAG, "failed to get localhost IP" ) ;
			// TODO: Note that we failed this - maybe display something?
			return false ;
		}
	}
	
	
	/**
	 * A request from the Delegate of a host priority number.  This number should
	 * 1. be partially based on circumstances, e.g., if we have previously become
	 * a client to for some other host, maybe we should have a low priority now?,
	 * and 2. be partially randomized, to prevent deadlock.
	 * 
	 * For example, here is a proposed "host priority algorithm":
	 * 
	 * int k;
	 * If have not changed roles, k = (2-playersPresentInMediatorUponFirstConnect)
	 * If HAVE changed roles, and playersPresentInMediatorUponFirstConnect = 0, k = 0
	 * If HAVE changed roles, and playersPresent... = 1, k = 3.
	 * 
	 * return k * 100000 + Random.nextInt(100000) ;
	 * 
	 * @return
	 */
	public int lcd_requestHostPriority() {
		int k ;
		if ( hasChangedRoles )
			k = (2 - playersPresentInMediatorUponFirstConnect) ;
		else
			k = playersPresentInMediatorUponFirstConnect == 0 ? 0 : 3 ;
		
		int scale = 100000 ;
		return k * scale + new Random().nextInt(scale) ;
	}
	
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * CLIENT/HOST TRANSITION
	 * 
	 * Based on messages received from another party, and the host priority set
	 * by the delegate, we should transition from host to client.
	 * 
	 * *************************************************************************
	 */
	
	/**
	 * Tells the Delegate that we believe we should become a client, rather than
	 * act as the host.  The parameter is the currently connected "client" who
	 * will serve as the host for the game.
	 */
	public void lcd_shouldBecomeClient( int hostConnectionNumber ) {
		// We were a Client; we should become a client.
		hasChangedRoles = playersPresentInMediatorUponFirstConnect == 0 ;
		
		// Step 1: tell the LCD to stop and give us its connection.  We also
		// shut down our lobbyClientCommunications.
		lobbyClientCommunications.stop() ;
		updateAchievementsLeaveLobby() ;
		mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
		lobbyCoordinator.stopAndProvideOpenConnections() ;
		
		slotForFutureHost = hostConnectionNumber ;
		// see Step 2 in lcd_stoppedWithOpenConnections.
	}
	
	
	/**
	 * Tells the delegate we have stopped operation with a set of open connections, as requested.
	 * This method will only be called if stopAndProvideOpenConnections() is called to stop
	 * the Coordinator.
	 * @param layer
	 */
	public void lcd_stoppedWithOpenConnections( MessagePassingLayer layer ) {
		// We are transitioning from a host to a client.  Our connection to the 
		// "soon to be host" is layer.connection(slotForFutureHost)
		
		// Retrieve the appropriate connection from the layer; disconnect and
		// deactivate the rest.
		for ( int i = 0; i < layer.numConnections(); i++ ) {
			if ( i != slotForFutureHost ) {
				MessagePassingConnection conn = layer.connection(i) ;
				try {
					conn.disconnect();
				} catch (Exception e) { }
				try {
					conn.deactivate();
				} catch (Exception e) { }
			}
		}
		
		MessagePassingConnection conn = layer.connection(slotForFutureHost) ;
		
		// We are currently in the lobbyCoordinatorThread; this is its last action.
		// We shouldn't touch any of its members (heh), but we can set it to null.
		lobbyCoordinator = null ;
		
		lobbyClientCommunications.stop() ;
		updateAchievementsLeaveLobby() ;
		mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
		
		// Make a new lobbyClientCommunications.
		// TODO: Clear the log for this ClientCommunications?
		lobbyClientCommunications = new LobbyClientCommunications( this, lobby, lobbyLog, lip.nonce, lip.personalNonce, lip.name, getPremiumAuthTokens() ) ;
		lobbyClientCommunications.setConnection(conn) ;
		
		// Start up!
		lobbyClientCommunications.start() ;
		updateAchievementsEnterLobby() ;
		mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
		mHandler.postDelayed(mRunnableAchievementsUpdate, 60 * 1000) ;
		// Make sure we stay active, if appropriate.  The default seems to be NOT active.
		if ( activityState == ACTIVITY_STATE_RESUMED )
			lobbyClientCommunications.sendActive() ;
		else if ( movingToGame )
			lobbyClientCommunications.sendInGame() ;
		else
			lobbyClientCommunications.sendInactive() ;
		
		// Disconnect and deactivate our pair; we are no longer a Coordinator.
		// Note that, above, we deactivated the Coordinator side.
		try {
			mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_CLIENT].disconnect() ;
		} catch( Exception e ) { /*nothing*/ }
		try {
			mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_CLIENT].deactivate() ;
		} catch( Exception e ) { /*nothing*/ }
		
		
		// TODO: Tell the Activity that we are now a client.
	}
	
	
	
	/*
	 * ************************************************************************
	 * ************************************************************************
	 * 
	 * LOBBY CLIENT COMMUNICATIONS DELEGATE METHODS
	 * 
	 * *************************************************************************
	 * ************************************************************************
	 */
	
	
	/*
	 * ************************************************************************
	 * 
	 * CONNECTION UPDATES
	 * 
	 * Once the LobbyClientCommunications starts, we attempt to Connect to the 
	 * server.  These methods inform the delegate of our connection status
	 * changes, and request guidance as to our next move.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * Notifies the delegate that our connection status has updated,
	 * and provides the new status.  This call is made every time our
	 * Connection changes status.
	 */
	public void lccd_connectionStatusChanged( MessagePassingConnection.Status connectionStatus ) {
		//Log.d(TAG, "lccd_connectionStatusChanged to " + connectionStatus) ;
		
		// Here's some states.
		switch( connectionStatus ) {
		case NEVER_CONNECTED:
		case PENDING:
			if ( mUseUserInterface )
				lobbyUserInterface.lui_connectionToServerConnecting() ;
			this.foregroundConnecting() ;
			break ;
			
		case CONNECTED:
			// When we connect, we start negotiating.  After we're welcomed,
			// the server will send a welcome message; that will prompt us
			// to tell the GUI that we've "connected" for real.
			if ( mUseUserInterface )
				lobbyUserInterface.lui_connectionToServerNegotiating() ;
			this.foregroundNegotiating() ;
			break ;
			
		case FAILED:
			// Failed to connect.
			if ( mUseUserInterface )
				lobbyUserInterface.lui_connectionToServerFailedToConnect() ;
			this.foregroundConnecting() ;
			break ;
			
		case BROKEN:
		case PEER_DISCONNECTED:
			// Connection broke.  This is an unexpected disconnect.
			// We expect that if the server warned us the connection would
			// be going down (e.g., if we were kicked or the server
			// is closing) that we would have disconnected ourselves.
			if ( mUseUserInterface )
				lobbyUserInterface.lui_connectionToServerDisconnectedUnexpectedly() ;
			this.foregroundConnecting() ;
			break ;
			
		case DISCONNECTED:
			// This is a manual disconnect.  Does the GUI need to know?
			// probably not, but maybe add a Connecting() method call.
			break ;
		}
		//Log.d(TAG, "done") ;
	}
	
	
	/**
	 * We received garbage data which, for whatever reason, we could not
	 * process.  We will disconnect and attempt to reconnect; we just wanted
	 * to update you on what happened.
	 */
	public void lccd_connectionReceivedIllegalMessage() {
		//Log.d(TAG, "lccd_connectionReceivedIllegalMessage") ;
		
		// This basically means a broken connection.  The client will handle
		// reconnecting, but the GUI should probably be told.  BUT: we tell it
		// once the connection status changes, not right away.  This is just
		// an unexpected disconnection message.
	}
	
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * MESSAGES
	 * 
	 * Messages which can be handled entirely with an update to the Lobby object
	 * will be handled by such an update (the Lobby itself will notify its own
	 * delegate of the change), and LobbyClientCommunications will not tell its
	 * own delegate.  These methods apply in cases where additional action is required.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * We were welcomed to the server.  Awwww!  Thanks man!
	 * 
	 * This message will be received after a successful connection
	 * and negotiation.  In other words, when we transition to CONNECTED
	 * status, we should be considered "negotiating" until we get this
	 * message.
	 */
	public void lccd_messageWelcomeToServer() {
		//Log.d(TAG, "lccd_messageWelcomeToServer") ;
		
		// We've been welcomed!  The GUI should know about this, because it
		// may want to dismiss a displayed message, i.e. "negotiating" should
		// go away.
		lobby.resetLocalPlayerVotes() ;
		
		if ( mUseUserInterface )
			lobbyUserInterface.lui_connectionToServerConnected() ;
		this.foregroundConnected() ;
	}
	
	
	/**
	 * The server wants us to know about the host name.
	 * @param name
	 */
	public void lccd_messageHost( int slot, String name ) {
		//Log.d(TAG, "lccd_messageHostName") ;
		// Tell the GUI; there is no lobby callback for this.
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updateLobbyMembership() ;
	}
	
	
	/**
	 * The server told us the total number of player slots for this game.
	 * @param slots
	 */
	public void lccd_messageTotalPlayerSlots( int slots ) {
		//Log.d(TAG, "lccd_messageTotalPlayerSlots:" + slots) ;
		lobby.setMaxPlayers(slots) ;
	}
	
	
	/**
	 * The server told us which player slot is OUR player slot.
	 * @param slot
	 */
	public void lccd_messageLocalPlayerSlot( int slot ) {
		//Log.d(TAG, "lccd_messageLocalPlayerSlot:" + slot) ;
		lobby.setLocalPlayerSlot(slot) ;
	}
	
	
	
	
	/**
	 * The server provided an authorization token for the specified game mode.
	 * 
	 * If a previous token existed, this token should replace it.
	 * 
	 * @param gameMode
	 * @param authToken
	 */
	public void lccd_messageGameModeAuthToken( int gameMode, Serializable authToken ) {
		boolean ok = authToken != null && isValidAuthToken( gameMode, authToken ) ;
		lobbyUserInterface.lui_setPremiumGameModeAvailable(gameMode, ok) ;
	}
	
	
	/**
	 * The server has revoked the most recent game authorization token.
	 * 
	 * If this game mode is premium content, or otherwise requires an auth
	 * token to be used, it should be disabled.
	 * 
	 * @param gameMode
	 */
	public void lccd_messageGameModeRevokeAuthToken( int gameMode ) {
		lobbyUserInterface.lui_setPremiumGameModeAvailable(gameMode, false) ;
	}
	
	
	/**
	 * The server provided an XML file for a particular game mode.
	 * @param gameMode
	 * @param xml
	 */
	public void lccd_messageGameModeXML( int gameMode, String xml ) {
		//Log.d(TAG, "lccd_messageGameModeXML") ;
		// We don't do anything with this message.
	}
	
	
	/**
	 * A game is launching, but we aren't participating.
	 * 
	 * @param gameMode
	 * @param playersInLaunch
	 */
	public void lccd_messageGameModeLaunchAsAbsent( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers ) {
		//Log.d(TAG, "lccd_messageGameModeLaunchAsAbsent") ;
		lccd_helper_launch( countdownNumber, gameMode, playersInLaunch, null ) ;
	}
	
	
	/**
	 * A game is launching, and we participate.
	 * 
	 * @param gameMode
	 * @param playersInLaunch
	 */
	public void lccd_messageGameModeLaunchAsDirectClient( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
			Nonce nonce, SocketAddress socketAddress ) {
		//Log.d(TAG, "lccd_messageGameModeLaunchAsDirectClient") ;
		// If we're launching, we don't want any local votes..
		lobby.resetLocalPlayerVotes() ;
		// construct a GIP.
		GameIntentPackage gip = new GameIntentPackage( new GameSettings(gameMode, numPlayers).setImmutable() ) ;
		gip.setConnectionAsDirectClient(
				nonce, lip.personalNonce, QuantroPreferences.getMultiplayerName(this),
				lobby.getPlayerNames(playersInLaunch),
				socketAddress ) ;
		lccd_helper_launch( countdownNumber, gameMode, playersInLaunch, gip ) ;
	}
	
	
	/**
	 * A game is launching, and we participate.
	 * 
	 * @param gameMode
	 * @param playersInLaunch
	 */
	public void lccd_messageGameModeLaunchAsDirectHost( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
			Nonce nonce, SocketAddress socketAddress ) {
		//Log.d(TAG, "lccd_messageGameModeLaunchAsDirectHost") ;
		// If we're launching, we don't want any local votes..
		lobby.resetLocalPlayerVotes() ;
		// construct a GIP.
		GameIntentPackage gip = new GameIntentPackage( new GameSettings(gameMode, numPlayers).setImmutable() ) ;
		gip.setConnectionAsDirectServer(
				nonce, lip.personalNonce,
				QuantroPreferences.getMultiplayerName(this), socketAddress,
				lobby.getPlayerPersonalNonces(playersInLaunch),
				lobby.getPlayerNames(playersInLaunch) ) ;
		lccd_helper_launch( countdownNumber, gameMode, playersInLaunch, gip ) ;
	}
	
	
	
	/**
	 * A game is launching, and we participate.
	 * 
	 * @param gameMode
	 * @param playersInLaunch
	 */
	public void lccd_messageGameModeLaunchAsMatchseekerClient( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
			Nonce nonce, SocketAddress socketAddress ) {
		//Log.d(TAG, "lccd_messageGameModeLaunchAsMatchseekerClient") ;
		// If we're launching, we don't want any local votes..
		lobby.resetLocalPlayerVotes() ;
		// construct a GIP.
		GameIntentPackage gip = new GameIntentPackage( new GameSettings(gameMode, numPlayers).setImmutable() ) ;
		gip.setConnectionAsMatchseekerClient(
				InternetLobbyGame.newUnrefreshedInternetLobbyGame(nonce, null),
				(InternetLobby)lobby,
				lip.personalNonce,
				QuantroPreferences.getMultiplayerName(this),
				lobby.getPlayerNames(playersInLaunch),
				socketAddress) ;
		lccd_helper_launch( countdownNumber, gameMode, playersInLaunch, gip ) ;
	}
	
	
	/**
	 * A game is launching, and we participate.
	 * 
	 * @param gameMode
	 * @param playersInLaunch
	 */
	public void lccd_messageGameModeLaunchAsMatchseekerHost( int countdownNumber, int gameMode, boolean [] playersInLaunch, int numPlayers,
			Nonce nonce, Nonce editKey, SocketAddress socketAddress ) {
		//Log.d(TAG, "lccd_messageGameModeLaunchAsMatchseekerHost") ;
		// If we're launching, we don't want any local votes..
		lobby.resetLocalPlayerVotes() ;
		// construct a GIP.
		GameIntentPackage gip = new GameIntentPackage( new GameSettings(gameMode, numPlayers).setImmutable() ) ;
		gip.setConnectionAsMatchseekerServer(
				InternetLobbyGame.newUnrefreshedInternetLobbyGame(nonce, editKey),
				(InternetLobby)lobby,
				lip.personalNonce,
				QuantroPreferences.getMultiplayerName(this), socketAddress,
				lobby.getPlayerPersonalNonces(playersInLaunch),
				lobby.getPlayerNames(playersInLaunch) ) ;
		lccd_helper_launch( countdownNumber, gameMode, playersInLaunch, gip ) ;
	}
	
	
	private void lccd_helper_launch( int countdownNumber, int gameMode, boolean [] playersInLaunch, GameIntentPackage gip ) {
		//Log.d(TAG, "lccd_helper_launch") ;
		
		// SANITY CHECK
		if ( !GameModes.has(gameMode) )
			return ;
		
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updatingLobbyCountdownsForLaunch(true) ;
		lobby.setCountdownAborted(countdownNumber, false) ;
		if ( mUseUserInterface )
			lobbyUserInterface.lui_updatingLobbyCountdownsForLaunch(false) ;
		
		// Intent is provided: we should launch a GameActivity to capture this.
		if ( gip != null ) {
			lobby.setLocalPlayerVoteForGameMode(gameMode, false) ;
			
			// We're about to start a game with game mode 'gameMode.'  Put it 
			// in our database.
			GameStats.DatabaseAdapter.addNewGameStartedToDatabase(this, gameMode) ;
			
			movingToGame = true ;
			
			// Launch the activity.
			Intent intent = new Intent( activity, GameActivity.class ) ;
	    	intent.setAction( Intent.ACTION_MAIN ) ;
	    	intent.putExtra(GameActivity.INTENT_EXTRA_SAVE_THUMBNAIL, false) ;
			intent.putExtra( GameActivity.INTENT_EXTRA_GAME_INTENT_PACKAGE, gip) ;
			activity.startActivityForResult(intent, IntentForResult.PLAY) ;
			
			// Log?
			if ( QuantroPreferences.getAnalyticsActive(this) ) {
				// Log the launch...
				Analytics.logInLobbyLaunch(lip.isDirect(), gip.gameSettings) ;
				// Log this new game
				Analytics.logNewGame(gip.gameSettings, false) ;
			}
			
			if ( gip.isClient() && mUseUserInterface )
				lobbyUserInterface.lui_launchAsClient(gameMode, playersInLaunch) ;
			else if ( mUseUserInterface )
				lobbyUserInterface.lui_launchAsHost(gameMode, playersInLaunch) ;
		} else if ( mUseUserInterface )
			lobbyUserInterface.lui_launchAsAbsent(gameMode, playersInLaunch) ;
	}
	
	
	/**
	 * The server has kicked a player, and provided a reason.  If
	 * playerSlot is < 0, or is OUR slot number, then WE were kicked.
	 * @param playerSlot
	 * @param msg
	 */
	public void lccd_messageKick( int slot, String msg ) {
		//Log.d(TAG, "lccd_messageKick(" + slot + ", " + msg + ")") ;
		// TODO: Put up a "kicked as client" dialog.
	}
	
	
	/**
	 * A player has exited the server; this message indicates that
	 * the player left.
	 * 
	 * @param playerSlot
	 */
	public void lccd_messageQuit( int playerSlot ) {
		//Log.d(TAG, "lccd_messageQuit:" + playerSlot) ;
	}
	
	
	/**
	 * The server is going down FOREVER and nicely told us so.
	 */
	public void lccd_messageServerClosingForever() {
		//Log.d(TAG, "lccd_messageServerClosingForever") ;
		
		// NOTE: In a non-mediated setting, the lobby closing means the lobby is closed,
		// period.  In a mediated setting, the "lobby" exists in the Aether so long
		// as at least 1 person is connected.  Therefore, if mediated, simply become the host.
		// Otherwise, stop everything and tell the user why.
		lobbyClientCommunications.stop();
		updateAchievementsLeaveLobby() ;
		mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
		// Tell the gui.
		if ( mUseUserInterface )
			lobbyUserInterface.lui_connectionToServerServerClosedForever() ;
		this.foregroundDisconnectedForever() ;
		// TODO: More complex behavior with >2 people in a lobby.
	}
	
	
	/**
	 * The server wants us to use this color for this person.
	 * @param playerSlot
	 * @param color
	 */
	public void lccd_messagePreferredColor( int playerSlot, int color ) {
		//Log.d(TAG, "lccd_messagePreferredColor: " + playerSlot + ", " + color) ;
		// So what?  Well, the GUI needs to know, but that's really it, I think.
		// Maybe we should keep track of this ourselves?  For example, maybe
		// player color should be significant if we launch a game?
		
		// How about we hope the LUI can tell us the color later?  That way we
		// don't need to store the info in two different places.
		if ( mUseUserInterface )
			lobbyUserInterface.lui_setPlayerColor(playerSlot, color) ;
	}
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * INFORMATION REQUESTS
	 * 
	 * While almost all the delegate methods are "updates for delegate" type
	 * messages, with the LobbyClientCommunications continuing on without any problems,
	 * the Communications will occassionally require information from the delegate.
	 * These methods request that information.
	 * 
	 * *************************************************************************
	 */
	
	
	
	
	/**
	 * This method is called before each connection attempt.  We will delay the
	 * specified number of seconds before attempting a connection.  It
	 * is probably wise to return 0 the first time this is called, but
	 * to implement some kind of delay like exponential back-off after
	 * repeated calls.
	 * 
	 * BTW, the order of calls will look something like:
	 * 
	 * lccd_connectionStatusChanged( CONNECTED, BROKEN(or something else) )
	 * lccd_connectionStatusChanged( BROKEN(...), DISCONNECTED )
	 * lccd_requestDelayUntilConnect()
	 * lccd_connectionStatusChanged( DISCONNECTED, PENDING(or connected, or...) )
	 * 
	 * @return The number of seconds before our next reconnection.
	 */
	public long lccd_requestDelayUntilConnect() {
		//Log.d(TAG, "lccd_requestDelayUntilConnect") ;
		// TODO: Stub.  For now, wait 1 second.
		return 1000 ;
	}
	
	
	/**
	 * A request from the Delegate of a host priority number.  This number should
	 * 1. be partially based on circumstances, e.g., if we have previously become
	 * a client to for some other host, maybe we should have a low priority now?,
	 * and 2. be partially randomized, to prevent deadlock.
	 * 
	 * For example, here is a proposed "host priority algorithm":
	 * 
	 * int k;
	 * If have not changed roles, k = (2-playersPresentInMediatorUponFirstConnect)
	 * If HAVE changed roles, and playersPresentInMediatorUponFirstConnect = 0, k = 0
	 * If HAVE changed roles, and playersPresent... = 1, k = 3.
	 * 
	 * return k * 100000 + Random.nextInt(100000) ;
	 * 
	 * @return
	 */
	public int lccd_requestHostPriority() {
		int k ;
		if ( hasChangedRoles )
			k = (2 - playersPresentInMediatorUponFirstConnect) ;
		else
			k = playersPresentInMediatorUponFirstConnect == 0 ? 3 : 0 ;
		
		int scale = 100000 ;
		return k * scale + new Random().nextInt(scale) ;
	}
	
	
	
	
	/*
	 * ************************************************************************
	 * 
	 * CLIENT/HOST TRANSITION
	 * 
	 * Based on messages received from the other party, and the host priority set
	 * by the delegate, we should transition from client to host.
	 * 
	 * *************************************************************************
	 */
	
	/**
	 * Tells the Delegate that we believe we should become the host, rather than
	 * act as the client.  Our currently connected "host" should be a client instead.
	 */
	public void lccd_shouldBecomeHost() {
		//Log.d(TAG, "lccd_shouldBecomeHost") ;
		// We were previously a Client; we should become a host.
		hasChangedRoles = playersPresentInMediatorUponFirstConnect == 1 ;
		
		// Step 1: tell the LCCD to stop and give us its connection.
		lobbyClientCommunications.stopAndProvideOpenConnection() ;
		updateAchievementsLeaveLobby() ;
		mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
		
		// see Step 2 in lccd_stoppedWithOpenConnection.
	}
	
	
	/**
	 * Tells the delegate we have stopped operation with an open connection, as requested.
	 * This method will only be called if stopAndProvideOpenConnection() is called to stop
	 * the LCC.
	 * @param conn
	 */
	public void lccd_stoppedWithOpenConnection( MessagePassingConnection conn ) {
		//Log.d(TAG, "lccd_stoppedWithOpenConnection") ;
		// We have been passed an open connection.  This is a transitory method
		// between "acting as a client" and "acting as a host."  We are currently
		// in the last gasp of a LobbyClientCommunicationsThread, which will be automatically
		// terminating after this return.
		
		// That means we shouldn't try to touch the lcc, BUT we can safely create a new LCC
		// object.
		// Step 2: create a new LobbyClientCommunications object, which will communicate with
		// our soon-to-be-made Coordinator.
		// TODO: make a new lobbyLog?
		lobbyClientCommunications = new LobbyClientCommunications(this, lobby, lobbyLog, lip.nonce, lip.personalNonce, lip.name, getPremiumAuthTokens()) ;
		// The new LCC should have a local connection (paired).
		try {
			MessagePassingConnection mpc = mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_CLIENT] ;
			mpc.activate() ;
			lobbyClientCommunications.setConnection( mpc ) ;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		// Step 3: create a new LobbyCoordinator.  Give it a message passing layer with
		// our local connection in 0, the provided connection 'conn' in 1.
		lobbyCoordinator = new LobbyCoordinator(
				this, AppVersion.code(this),
				lip.nonce,
		        lip.nonce, 
				lip.lobbyName, 0, lip.name, true, lip.lobbySize ) ;
		MessagePassingLayer mpLayer = new MessagePassingLayer(lip.lobbySize) ;
		// Note: we CANNOT call 'activate' on the layer, because that puts all
		// connections in "just activated" state, whereas we want to keep the open
		// connection with 'conn'.  Instead, activate our own personal connection.
		try {
			MessagePassingConnection mpc = mppcPair[MessagePassingPairedConnection.PAIR_INDEX_CS_SERVER] ;
			mpc.activate() ;
			mpLayer.setConnection(0, mpc) ;
			mpLayer.setConnection(1, conn) ;
			// TODO: This is a problem.  What about additional connections?
			lobbyCoordinator.setMessagePassingLayer(mpLayer) ;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// We're basically done.  Start them both up.
		lobbyCoordinator.start() ;
		lobbyClientCommunications.start() ;
		updateAchievementsEnterLobby() ;
		mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
		mHandler.postDelayed(mRunnableAchievementsUpdate, 60 * 1000) ;
		if ( activityState == ACTIVITY_STATE_RESUMED )
			lobbyClientCommunications.sendActive() ;
		else if ( movingToGame )
			lobbyClientCommunications.sendInGame() ;
		else
			lobbyClientCommunications.sendInactive() ;
		
		// TODO: Tell the Activity that we are now a host.
	}
	
	
	/*
	 * ************************************************************************
	 * 
	 * MATCH ROUTER UPDATE LISTENER METHODS
	 * 
	 * Messages providing updates to our attempts to seek matches.
	 * 
	 * Replaces the MessagePassingMatchSeekerConnection.Delegate methods.
	 * 
	 * At present these are direct translations of the mpmscd_ methods, and
	 * could probably do with some revisions.  The main difficulty is that we'd
	 * like to be robust to lobbies and games with > 2 players.
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
		if ( mUseUserInterface )
			lobbyUserInterface.lui_matchmakingRequestingTicket() ;
		// nothing else to do here.
	}
	
	
	/**
	 * We got a matchticket.
	 * 
	 * @param mpmsc
	 */
	public void mrul_didReceiveMatchTicket( long serial ) {
		mHandler.removeCallbacks(mRunnableLobbyFull) ;
		mHandler.post(mRunnableLobbyNotFull) ;
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
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingLobbyClosed();
			return false ;
		case WebConsts.ERROR_NONE:
			throw new RuntimeException("mpmscd reported error, but gave ERROR_NONE as its code.") ;
		case WebConsts.ERROR_TIMEOUT:
			// lack of response.  User should be notified, but we keep trying.
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingNoTicketNoResponse() ;
			return true ;
		case WebConsts.ERROR_REFUSED:
			// WHAT!  What's the reason??  If 'full', this is recoverable.
			if ( reason == WebConsts.REASON_FULL )
				mHandler.postDelayed(mRunnableLobbyFull, SERVER_LOBBY_FULL_COUNTDOWN) ;
			Log.d( TAG, "mpmscd ticket refused, reason is " + WebConsts.reasonIntToString(reason) ) ;
			return false ;
		case WebConsts.ERROR_FAILED:
			throw new RuntimeException("mpmscd reported FAILURE.  This should never happen!") ;
		case WebConsts.ERROR_BLANK:
			// hm, communication error?
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingNoTicketNoResponse() ;
			return true ;
		case WebConsts.ERROR_MALFORMED:
			// hm, communication error?
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingNoTicketNoResponse() ;
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
		if ( mUseUserInterface )
			lobbyUserInterface.lui_matchmakingRequestingMatch() ;
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
		if ( mUseUserInterface )
			lobbyUserInterface.lui_matchmakingNoResponse() ;
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
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingRejectedFull() ;
			return true ;
			
			
		case INVALID_NONCE:
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingRejectedInvalidNonce() ;
			// This is unrecoverable.  We shouldn't keep trying to reconnect.
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
			foregroundConnectionFailure() ;
			return false ;
			
		case NONCE_IN_USE:
			Log.d(TAG, "mrul_didReceiveRejectionFromMatchmaker: REJECTION_REASON_NONCE_IN_USE") ;
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingRejectedNonceInUse() ;
			// This is unrecoverable.  We shouldn't keep trying to reconnect.
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
			foregroundConnectionFailure() ;
			return false ;
			
		case PORT_RANDOMIZATION:
			// we CAN recover from this.  mpmsc has already set a delay.
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingRejectedPortRandomization() ;
			return true ;
			
		case UNSPECIFIED:
			if ( mUseUserInterface )
				lobbyUserInterface.lui_matchmakingRejectedUnspecified() ;
			lobbyClientCommunications.stop() ;
			updateAchievementsLeaveLobby() ;
			mHandler.removeCallbacks(mRunnableAchievementsUpdate) ;
			if ( lobbyCoordinator != null )
				lobbyCoordinator.stop() ;
			foregroundConnectionFailure() ;
			return false ;
			
			
		case MATCHTICKET_ADDRESS:
		case MATCHTICKET_CONTENT:
		case MATCHTICKET_SIGNATURE:
		case MATCHTICKET_PROOF:
		case MATCHTICKET_EXPIRED:
			// recoverable and irrelevant.
			return true ;
			
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
		if ( playersPresentInMediatorUponFirstConnect == -1 )
			playersPresentInMediatorUponFirstConnect = 0 ;
		
		// Tell the LUI
		if ( mUseUserInterface )
			lobbyUserInterface.lui_matchmakingReceivedPromise() ;
	}
	
	
	/**
	 * We have been matched with a partner!  However, we have not yet begun the hole-punching
	 * attempt (or whatever) and do not have a direct connection with them.
	 * @param mpmc
	 */
	public void mrul_didMatchStart( long serial ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_matchmakingReceivedMatch() ;
		
		// it is possible that there is noone on the other end of this match.  If so,
		// we don't want to have to wait until the match expires.
		if ( playersPresentInMediatorUponFirstConnect == -1 )
			playersPresentInMediatorUponFirstConnect = 1 ;
		
		/* if ( lobbyCoordinator == null )
			lobbyClientCommunications.becomeHostIfNoConnectionIn(10 * 1000) ; */		// 10 seconds
	}
	

	/**
	 * We have completed our match with a partner, and now have a communication channel with them!
	 * @param mpmsc
	 */
	public void mrul_didMatchSuccess( long serial ) {
		if ( mUseUserInterface )
			lobbyUserInterface.lui_matchmakingComplete() ;
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
		
		if ( mUseUserInterface )
			lobbyUserInterface.lui_matchmakingFailed() ;
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
	
	
	
	/**
	 * This delegate method is called every time a new event is added 
	 * to the log ( if delegate is not null ).  It is called as the last
	 * operation of the add, so the LobbyLog is consistent with the state
	 * described in the parameters.
	 * 
	 * Take care not to 'log' anything in this delegate callback, since
	 * that might cause an infinite recursion.
	 * 
	 * If only one thread is writing to the LobbyLog (using log* methods)
	 * you can be assured that the contents of LobbyLog will not change during
	 * this method.
	 * 
	 * @param lobbyLog
	 * @param id
	 * @param type
	 */
	public void lld_newEvent( LobbyLog lobbyLog, int id, int type ) {
		if ( lobbyUserInterface != null && mUseUserInterface )
			lobbyUserInterface.lui_lobbyLogNewEvent(lobbyLog, id, type) ;
		// notification?
		LobbyLog.Event event = lobbyLog.getEvent(id) ;
		if ( type == LobbyLog.Event.CHAT && (!movingToGame || QuantroPreferences.getInGameChat(this)) ) {
			this.foregroundMessage(event.name, event.text) ;
		} else if ( type == LobbyLog.Event.PLAYER_STATUS_CHANGE ) {
			if ( event.slot == lobby.getLocalPlayerSlot() ) {
				if ( event.status == Lobby.PLAYER_STATUS_ACTIVE )
					this.foregroundUserActive() ;
				else
					this.foregroundUserIdle() ;
			}
		}
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
	public void ilmd_communicationSuccess( InternetLobbyMaintainer ilm, int action,
			long timeSpentFailing, int numFailures ) {
		
		if ( ilm == null || ilm != lobbyMaintainer )
			return ;
		
		// stop any countdowns or notifications for communication failure.
		mHandler.removeCallbacks(mRunnableServerCommunicationFailure) ;
		mHandler.post(mRunnableServerCommunicationOK) ;
		
		// do NOT stop runnables for other types, such as lobby closed.  It's
		// worth noting that 'refresh' actions can receive a false positive
		// communication success when the lobby is closed or removed, BUT
		// in that case we should put up a 'removed' runnable anyway.
		
		if ( action == InternetLobbyMaintainer.Delegate.ACTION_REFRESH
				&& ( ((InternetLobby)lobby).getStatus() == WebConsts.STATUS_CLOSED
						|| ((InternetLobby)lobby).getStatus() == WebConsts.STATUS_REMOVED ) ) {
			if ( !mServerLobbyClosed )
				mHandler.post(mRunnableLobbyClosed) ;
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
	public long ilmd_retryDelayAfterCommunicationFailure( InternetLobbyMaintainer ilm, int action, int error, int reason,
			long timeSpentFailing, int numFailures ) {
		
		if ( ilm == null || ilm != lobbyMaintainer )
			return 0 ;
		
		// Different types of failures require a different response.
		switch( error ) {
		case WebConsts.ERROR_MISMATCHED_WORK_ORDER:
		case WebConsts.ERROR_INCOMPLETE_WORK_ORDER:
			// nothing we can do about these.  The maintainer will retry.
			// print to log, because this should probably never happen.
			//Log.d(TAG, "InternetLobbyMaintainerDelegate: reported error mismatched or incomplete work order.") ;
			break ;
		
		case WebConsts.ERROR_NONE:
			// huh?
			Log.d(TAG, "InternetLobbyMaintainerDelegate: reported error NONE???") ;
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
			// The lobby state has changed, and we can no longer do that thing.
			// This basically means the lobby is closed.  Tell the user and
			// shut everything down, possibly even in that order.
			mHandler.post(mRunnableLobbyClosed) ;
			break ;
			
		case WebConsts.ERROR_REFUSED:
			// the server refused (for some reason).  Our response could be different
			// depending on what the action was and the server's reason for refusal.
			if ( action == InternetLobbyMaintainer.Delegate.ACTION_REFRESH ) {
				// regardless of reason, a refusal means the lobby has been removed.
				if ( !mServerLobbyClosed )
					mHandler.post(mRunnableLobbyClosed) ;
			} else if ( action == InternetLobbyMaintainer.Delegate.ACTION_HOST ) {
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
					if ( !mServerLobbyClosed )
						mHandler.post(mRunnableLobbyClosed) ;
				}
			} else if ( action == InternetLobbyMaintainer.Delegate.ACTION_MAINTAIN ) {
				// two possible reasons (this could be a MAINTAIN_REQUEST or a 
				// MAINTAIN_CONFIRM message).  If CLOSED, obviously the lobby is
				// closed.  If WORK, the work order was insufficient and
				// the Maintainer is going to try again.  Not a huge issue for
				// us, though.
				if ( reason == WebConsts.REASON_CLOSED ) {
					// lobby closed or removed.
					if ( !mServerLobbyClosed )
						mHandler.post(mRunnableLobbyClosed) ;
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
	
	
	////////////////////////////////////////////////////////////////////////////
	//
	// INTERNET GAME MAINTENANCE PORTFOLIO
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
	public void igmpd_communicationSuccess(
			InternetGameMaintenancePortfolio igmp,
			InternetGameMaintenancePortfolio.Record record,
			int action, long timeSpentFailing, int numFailures ) {
		
		if ( igmp == null || igmp != lobbyGamePortfolio )
			return ;
		
		// You might think this means communications are back online.  however,
		// there is one significant issue here: games, like lobbies, perform
		// a "fake refresh" once closed which is reported as a communication 
		// success despite not involving any communication.
		int status = WebConsts.STATUS_EMPTY ;
		if ( record.getGame() != null )
			status = record.getGame().getStatus() ;
		if ( action == InternetGameMaintenancePortfolio.Delegate.ACTION_REFRESH_GAME
				&& ( status == WebConsts.STATUS_CLOSED || status == WebConsts.STATUS_REMOVED ) )
			return ;		// DON'T ALTER OUR RUNNABLES!  THIS MAY HAVE BEEN A FAKE REFRESH!
		
		
		// communication is OK!  Stop any failure countdowns and run
		// the server OK runnable to recover if we reported failure before
		// (remember that communication failure is the recoverable failure;
		// Lobby Listing failure, where the lobby itself disappears from
		// the database, is non-recoverable).
		mHandler.removeCallbacks(mRunnableServerCommunicationFailure) ;
		mHandler.post(mRunnableServerCommunicationOK) ;
		
		// There is no other concern here.  The caller does not refresh Lobbies,
		// so 'success' can never mean a bad lobby.
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
	 */
	public long igmpd_retryDelayAfterCommunicationFailure(
			InternetGameMaintenancePortfolio igmp,
			InternetGameMaintenancePortfolio.Record record,
			int action,
			int error, int reason,
			long timeSpentFailing, int numFailures ) {
		
		if ( igmp == null || igmp != lobbyGamePortfolio )
			return 0 ;
		
		long bestDelay = 0 ;
		
		// Different types of failures require a different response.
		switch( error ) {
		case WebConsts.ERROR_MISMATCHED_WORK_ORDER:
		case WebConsts.ERROR_INCOMPLETE_WORK_ORDER:
			// nothing we can do about these.  The maintainer will retry.
			// print to log, because this should probably never happen.
			Log.d(TAG, "InternetGameMaintenancePortfolio: reported error mismatched or incomplete work order.") ;
			break ;
		
		case WebConsts.ERROR_NONE:
			// huh?
			Log.d(TAG, "InternetGameMaintenancePortfolio: reported error NONE???") ;
			break ;
			
		case WebConsts.ERROR_TIMEOUT:
		case WebConsts.ERROR_BLANK:
		case WebConsts.ERROR_MALFORMED:
		case WebConsts.ERROR_UNKNOWN:
			// Communication failure.  This should start a countdown to
			// user notification about this failure.
			if ( !mServerCommunicationFailing )		// fail in 45 seconds starting now
				mHandler.postDelayed(mRunnableServerCommunicationFailure, SERVER_COMMUNICATION_FAILURE_COUNTDOWN) ;
			// delay 1 second before our next attempt.
			bestDelay = 1000 ;
			break ;
			
		case WebConsts.ERROR_ILLEGAL_ACTION:
		case WebConsts.ERROR_ILLEGAL_STATE:
			// TODO: depending on the action, this might indicate that the LOBBY has
			// been closed, or only a particular GAME.  Handle those two things differently.
			// If a lobby is closed, then we basically quit out (post the appropriate Runnable).
			// If a game is closed, just close it officially.
			switch ( action ) {
			case InternetGameMaintenancePortfolio.Delegate.ACTION_OPEN_GAME:
				// lobby is closed!
				if ( !mServerLobbyClosed )
					mHandler.post(mRunnableLobbyClosed) ;
				break ;
			
			case InternetGameMaintenancePortfolio.Delegate.ACTION_MAINTAIN_GAME:
				// game is closed or removed.
				igmp.close(record.getGame()) ;
				break ;
				
			case InternetGameMaintenancePortfolio.Delegate.ACTION_REFRESH_GAME:
				// huh?  this should never happen.
				Log.d(TAG, "InternetGameMaintenancePortfolio reported ILLEGAL_ACTION or ILLEGAL_STATE for a REFRESH????") ;
				igmp.close(record.getGame()) ;
				break ;
				
			case InternetGameMaintenancePortfolio.Delegate.ACTION_CLOSE_GAME:
				// huh?  this should never happen.
				Log.d(TAG, "InternetGameMaintenancePortfolio reported ILLEGAL_ACTION or ILLEGAL_STATE for a CLOSE????") ;
				break ;
			}
			break ;
			
		case WebConsts.ERROR_REFUSED:
			// the server refused (for some reason).  Our response could be different
			// depending on what the action was and the server's reason for refusal.
			switch ( action ) {
			case InternetGameMaintenancePortfolio.Delegate.ACTION_OPEN_GAME:
				// if refused because "full," it's important that we
				// close some games right away.  If refused because "closed,"
				// the LOBBY is closed (not the game, which doesn't exist yet).
				// If refused because of some other reason - probably work-order
				// - the portfolio is going to handle the recovery.
				if ( reason == WebConsts.REASON_FULL ) {
					mHandler.post(mRunnableGamesFull) ;
					return 10 * 1000 ;	// some extra time to allow closing existing games
				} else if ( reason == WebConsts.REASON_CLOSED ) {
					// lobby closed or removed.
					if ( !mServerLobbyClosed )
						mHandler.post(mRunnableLobbyClosed) ;
				} else {
					// nothing.  Portfolio will handle it.
					Log.d(TAG, "InternetGameMaintenancePortfolio reported refusal to open game with reason " + WebConsts.reasonIntToString(reason)) ;
				}
				break ;
			
			case InternetGameMaintenancePortfolio.Delegate.ACTION_MAINTAIN_GAME:
				// could be "closed" (closed game, NOT lobby), or database ( ~= communication fail )
				// or "work" which the portfolio will handle itself.
				if ( reason == WebConsts.REASON_CLOSED ) {
					// game is closed
					igmp.close(record.getGame()) ;
				} else if ( reason == WebConsts.REASON_DATABASE ) {
					// communication problem
					if ( !mServerCommunicationFailing )		// fail in 45 seconds starting now
						mHandler.postDelayed(mRunnableServerCommunicationFailure, SERVER_COMMUNICATION_FAILURE_COUNTDOWN) ;
				} // otherwise, do nothing.
				break ;
				
			case InternetGameMaintenancePortfolio.Delegate.ACTION_REFRESH_GAME:
				// refusal to refresh a game means the game doesn't exist anymore.
				igmp.close(record.getGame()) ;
				break ;
				
			case InternetGameMaintenancePortfolio.Delegate.ACTION_CLOSE_GAME:
				// we never expect this to be refused, only to fail...
				Log.d(TAG, "InternetGameMaintenancePortfolio reported refusal to close game with reason " + WebConsts.reasonIntToString(reason)) ;
				break ;
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
		return bestDelay ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	
	
	
	
	private void updateAchievementsEnterLobby() {
		// Achievements: joining or creating a lobby.
		if ( this.lip.isHost() ) {
			// created
			if ( this.lip.isDirect() ) {
				// wifi
				Achievements.lobby_hostWiFi(getLobby()) ;
			} else if ( lip.internetLobby.isPublic() ) {
				// public internet
				Achievements.lobby_hostPublicInternet(getLobby()) ;
			} else if ( lip.internetLobby.isItinerant() ) {
				// roaming
				Achievements.lobby_hostRoamingInternet(getLobby(), lip.internetLobby.getOrigin() == InternetLobby.ORIGIN_CREATED) ;
			} else {
				Achievements.lobby_hostPrivateInternet(getLobby()) ;
			}
		} else {
			// join
			if ( this.lip.isDirect() ) {
				// wifi
				Achievements.lobby_joinWiFi(getLobby()) ;
			} else if ( lip.internetLobby.isPublic() ) {
				// public internet
				Achievements.lobby_joinPublicInternet(getLobby()) ;
			} else if ( lip.internetLobby.isItinerant() ) {
				// roaming
				Achievements.lobby_joinRoamingInternet(getLobby()) ;
			} else {
				Achievements.lobby_joinPrivateInternet(getLobby()) ;
			}
		}
	}
	
	private void updateAchievementsStillInLobby() {
		if ( this.lip.isDirect() ) {
			// wifi
			Achievements.lobby_updateWiFi(getLobby()) ;
		} else if ( lip.internetLobby.isPublic() ) {
			// public internet
			Achievements.lobby_updatePublicInternet(getLobby()) ;
		} else if ( lip.internetLobby.isItinerant() ) {
			// roaming
			Achievements.lobby_updateRoamingInternet(getLobby()) ;
		} else {
			Achievements.lobby_updatePrivateInternet(getLobby()) ;
		}
	}
	
	private void updateAchievementsLeaveLobby() {
		// Achievements: closing or leaving a lobby.
		if ( this.lip.isHost() ) {
			// closing
			if ( this.lip.isDirect() ) {
				// wifi
				Achievements.lobby_closeWiFi(getLobby()) ;
			} else if ( lip.internetLobby.isPublic() ) {
				// public internet
				Achievements.lobby_closePublicInternet(getLobby()) ;
			} else if ( lip.internetLobby.isItinerant() ) {
				// roaming
				Achievements.lobby_closeRoamingInternet(getLobby()) ;
			} else {
				Achievements.lobby_closePrivateInternet(getLobby()) ;
			}
		} else {
			// leaving
			if ( this.lip.isDirect() ) {
				// wifi
				Achievements.lobby_leaveWiFi(getLobby()) ;
			} else if ( lip.internetLobby.isPublic() ) {
				// public internet
				Achievements.lobby_leavePublicInternet(getLobby()) ;
			} else if ( lip.internetLobby.isItinerant() ) {
				// roaming
				Achievements.lobby_leaveRoamingInternet(getLobby()) ;
			} else {
				Achievements.lobby_leavePrivateInternet(getLobby()) ;
			}
		}
	}
	

	private class ThreadCloseLobby extends Thread {
		MutableInternetLobby mMutableInternetLobby ;
		
		private ThreadCloseLobby( MutableInternetLobby mil ) {
			mMutableInternetLobby = mil ;
		}

		@Override
		public void run() {
			try {
				mMutableInternetLobby.close() ;
			} catch (CommunicationErrorException e) {
				e.printStackTrace();
			}
		}
	}

}
