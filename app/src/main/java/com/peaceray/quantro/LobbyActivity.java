package com.peaceray.quantro;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.peaceray.quantro.communications.nonce.Nonce;
import com.peaceray.quantro.consts.IntentForResult;
import com.peaceray.quantro.database.GameStats;
import com.peaceray.quantro.dialog.GlobalDialog;
import com.peaceray.quantro.dialog.TextFormatting;
import com.peaceray.quantro.dialog.DialogManager;
import com.peaceray.quantro.lobby.InternetLobby;
import com.peaceray.quantro.lobby.Lobby;
import com.peaceray.quantro.lobby.LobbyLog;
import com.peaceray.quantro.lobby.LobbyStringEncoder;
import com.peaceray.quantro.main.LobbyService;
import com.peaceray.quantro.main.LobbyUserInterface;
import com.peaceray.quantro.main.ServicePassingBinder;
import com.peaceray.quantro.model.game.GameResult;
import com.peaceray.quantro.model.modes.GameModes;
import com.peaceray.quantro.premium.PremiumLibrary;
import com.peaceray.quantro.sound.QuantroSoundPool;
import com.peaceray.quantro.utils.Achievements;
import com.peaceray.quantro.utils.Analytics;
import com.peaceray.quantro.utils.ArrayOps;
import com.peaceray.quantro.utils.VersionCapabilities;
import com.peaceray.quantro.utils.VersionSafe;
import com.peaceray.quantro.utils.WifiMonitor;
import com.peaceray.quantro.utils.clipboard.Clipboard;
import com.peaceray.quantro.utils.nfc.NdefMessage;
import com.peaceray.quantro.utils.nfc.NdefRecord;
import com.peaceray.quantro.utils.nfc.NfcAdapter;
import com.peaceray.quantro.view.button.QuantroButton;
import com.peaceray.quantro.view.button.strip.LobbyTitleBarButtonStrip;
import com.peaceray.quantro.view.colors.ColorScheme;
import com.peaceray.quantro.view.colors.GameModeColors;
import com.peaceray.quantro.view.lobby.LobbyViewAdapter;
import com.peaceray.quantro.view.lobby.LobbyViewComponentAdapter;
import com.peaceray.quantro.view.options.OptionAvailability;
import com.peaceray.quantro.view.dialog.AlertDialog;
import com.peaceray.quantro.view.dialog.ProgressDialog;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ShareCompat;

public class LobbyActivity extends QuantroActivity implements
		ServiceConnection, LobbyViewAdapter.Delegate, LobbyUserInterface,
		WifiMonitor.Listener, LobbyTitleBarButtonStrip.Delegate,
		GlobalDialog.DialogContext {

	public static final String TAG = "LobbyActivity";
	public static final String TAG_LCC = "LobbyActivity as LobbyClientController";
	public static final String TAG_LC = "LobbyActivity as LobbyController";
	

	// /////////////////////////////////////////////////////////////////////////
	//
	// DIALOGS
	//
	// Some important meta events (connecting to lobby, trying to quit,
	// etc.) are important enough for a dialog to be displayed to the user.
	// Here we define some of them; i.e., we define their letter codes.
	//

	// Lobby setup and handshaking
	public static final int DIALOG_CONNECTING_TO_LOBBY_ID = 0;
	public static final int DIALOG_TALKING_TO_LOBBY_ID = 1;

	// Game launching
	public static final int DIALOG_LAUNCHING_GAME_ID = 2;

	// Warning for leaving the lobby
	public static final int DIALOG_LEAVING_LOBBY_ID = 3;

	// Warning that the lobby has closed down.
	public static final int DIALOG_HOST_CLOSED_LOBBY_FOREVER_ID = 4;

	// MATCHMAKING: Requesting, and receiving, a matchmaking connection.
	public static final int DIALOG_MATCHMAKING_NO_RESPONSE_ID = 10;
	public static final int DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID = 11;
	public static final int DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID = 12;

	public static final int DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID = 13;
	public static final int DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID = 14;
	public static final int DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID = 15;

	// WIFI: Only relevant for WiFi lobbies. Warns the user that they do not
	// seem to have a WiFi connection.
	public static final int DIALOG_NO_WIFI_CONNECTION = 17;

	// WEB CGI_BIN: Communication with the cgi interface for quantro_lobby_web
	public static final int DIALOG_LOBBY_WEB_COMMUNICATION_FAILURE = 18;
	public static final int DIALOG_LOBBY_WEB_CLOSED = 19;
	public static final int DIALOG_LOBBY_WEB_FULL = 20 ;
	
	// INFO!  Lobby information.
	public static final int DIALOG_LOBBY_INFORMATION = 21 ;
	
	// SWITCH!  When a new Intent comes in, we prompt before switching to the new, pending lobby LIP.
	public static final int DIALOG_SWITCH_TO_PENDING_LOBBY_ID = 22 ;

	// Dialog tiers
	public static final int DIALOG_TIER_NETWORK_STATUS = 0;
	public static final int DIALOG_TIER_MATCHMAKING_PROBLEMS = 1;

	public static final int DIALOG_TIER_WIFI_PROBLEMS = 2; // note: WiFi and
															// Mediator are
															// INCOMPATIBLE. We
															// only ever use one
															// or the other.
	public static final int DIALOG_TIER_INTERACTIVE = 3;
	
	public static final int DIALOG_TIER_SWITCH_LOBBIES = 4 ;

	// Dialogs grouped by tier.
	public static final int[] DIALOGS_NETWORK_STATUS = new int[] {
			DIALOG_CONNECTING_TO_LOBBY_ID, DIALOG_TALKING_TO_LOBBY_ID,
			DIALOG_LAUNCHING_GAME_ID };


	public static final int[] DIALOGS_MATCHMAKING_PROBLEMS = new int[] {
			DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID,
			DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID,
			DIALOG_MATCHMAKING_NO_RESPONSE_ID,
			DIALOG_LOBBY_WEB_COMMUNICATION_FAILURE,
			DIALOG_LOBBY_WEB_FULL };

	public static final int[] DIALOGS_WIFI_PROBLEMS = new int[] { DIALOG_NO_WIFI_CONNECTION };

	public static final int[] DIALOGS_INTERACTIVE = new int[] {
			DIALOG_LEAVING_LOBBY_ID, DIALOG_HOST_CLOSED_LOBBY_FOREVER_ID,
			DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID,
			DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID,
			DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID,
			DIALOG_LOBBY_WEB_CLOSED };
	
	public static final int [] DIALOGS_SWITCH_LOBBIES = new int[] {
		DIALOG_SWITCH_TO_PENDING_LOBBY_ID
			} ;

	// ACTIVITY STATE:
	// To interact with GameService, we must send it a complete update as
	// to our current state (started, resumed, paused, etc.) immediately
	// upon binding. These state variables serve one purpose - allowing
	// us to perform that update.
	// NOTE: THE VALUE OF THESE VARIABLES IS IMPORTANT! WE USE THE FACT
	// THAT THEY ARE VALUED IN ASCENDING ORDER.
	int activityState;
	public static final int ACTIVITY_STATE_NONE = -1 ;
	public static final int ACTIVITY_STATE_CREATED = 0;
	public static final int ACTIVITY_STATE_STARTED = 1;
	public static final int ACTIVITY_STATE_RESUMED = 2;
	public static final int ACTIVITY_STATE_PAUSED = 3;
	public static final int ACTIVITY_STATE_STOPPED = 4;
	public static final int ACTIVITY_STATE_DESTROYED = 5;

	LobbyService lobbyService;

	Resources res;
	DialogManager dialogManager;

	// Intent package
	LobbyIntentPackage mLip;
	LobbyIntentPackage mPendingLip ;	// a new LIP just came in?

	// Color scheme, sound pool
	ColorScheme mColorScheme;
	QuantroSoundPool mQuantroSoundPool;
	boolean mControlsSound;

	// A self-reference for being the UserInterface.
	LobbyUserInterface lobbyUserInterface;
	boolean mLUICountdownUpdateForLaunch = false;

	// Here are our GUI fields.
	LobbyViewAdapter lobbyViewAdapter;
	LobbyTitleBarButtonStrip lobbyTitleBarButtonStrip;
	AlertDialog lobbyInformationDialog ;
	View lobbyInformationContentView ;
	// A direct reference to the lobby object.
	Lobby lobby;
	// A direct reference to the lobby log object.
	LobbyLog lobbyLog;

	boolean[] launchingWithPlayers;
	String lobbyOwner;

	boolean activityShown;

	boolean didClaimHostRole = false;
	
	boolean mShowLobbyInformationOnConnect = false ;

	// A note on WiFi connectivity. For mediated games, we put up messages
	// when we have trouble connecting to the mediator. That should be
	// sufficient
	// for them. For WiFi games, though, it isn't enough that we have a network
	// connection and can query arbitrary sites - we must be on WIFI or things
	// are
	// broken.
	// The Service doesn't need to know this. It uses "Connections" which are
	// either
	// connected or not, irrespective of antennas in-use. This is purely
	// feedback
	// for the player.
	Handler mHandler;
	WifiMonitor mWifiMonitor = null ; // will be null for non-WiFi games.
	boolean mNoWifiDialogDisplayed = false;
	private static final int WIFI_MONITOR_UPDATE_FREQUENCY = 1000; // every
																	// second
	
	// NdefMessage!  We register every time this message changes.
	private NdefMessage mLastMessageSet = null ;

	
	// The user wants to play a premium game mode.  Which mode?  This mode.
	private int mPremiumGameModeNeeded ;
	
	
	/**
	 * Called when the LobbyActivity is currently running, and a new
	 * Intent has come in for it.  This method needs to handle several
	 * cases: 1, the Lobby has just been brought back to top with the
	 * original launching Intent (as when the Service icon is tapped).
	 * 2, the previous Lobby has been closed and we should immediately
	 * move to a new one.
	 * 3, both lobbies are open.  We should put up a warning dialog,
	 * allowing the user to stay in the current lobby or move to the new
	 * one.
	 */
	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent") ;
		String lipExtraName = res.getString(R.string.intent_extra_lobby_intent_package) ;
		if ( intent.hasExtra( lipExtraName ) ) {
			LobbyIntentPackage lip = (LobbyIntentPackage) intent.getSerializableExtra( lipExtraName ) ;
			
			// same lobby intent as before?
			if ( lip.isSameLobbyIntent(mLip) ) {
				// do nothing.
				Log.d(TAG, "onNewIntent: is same lobby.") ;
				return ;
			}
			
			// Dialog!  Examine our current Lip and the new one to construct
			// the appropriate dialog to show.  Generally speaking, we could be:
			// 1: hosting a lobby and moving to become a client,
			// 2: hosting a lobby and moving to host a different one,
			// 3: client in a lobby and moving to host one
			// 4: client in a lobby and moving to a different one
			Log.d(TAG, "onNewIntent: switch is pending") ;
			mPendingLip = lip ;
			dialogManager.showDialog(DIALOG_SWITCH_TO_PENDING_LOBBY_ID) ;
		}

		super.onNewIntent(intent);
	}
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		activityState = ACTIVITY_STATE_NONE ;
		
		Log.d(TAG, "onCreate") ;
		super.onCreate(savedInstanceState);
		setupQuantroActivity(QUANTRO_ACTIVITY_LOBBY,
				QUANTRO_ACTIVITY_CONTENT_FULL);

		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		mQuantroSoundPool = ((QuantroApplication) getApplication())
				.getSoundPool(this);
		mControlsSound = QuantroPreferences.getSoundControls(this);

		// user interface
		lobbyUserInterface = this;
		lobbyService = null;

		activityShown = false;

		// Set resources, dialog manager
		res = getResources();
		dialogManager = new DialogManager(this, new int[][] {
				DIALOGS_NETWORK_STATUS,
				DIALOGS_MATCHMAKING_PROBLEMS,
				DIALOGS_WIFI_PROBLEMS, DIALOGS_INTERACTIVE, DIALOGS_SWITCH_LOBBIES });

		// LOAD LAYOUT - our superclass, QuantroActivity,
        // sets stinger and ads as appropriate.
		setContentView(R.layout.lobby_layout);

		// Force portrait layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Get game mode, save_to, load_from.
		mLip = (LobbyIntentPackage) getIntent().getSerializableExtra(
				res.getString(R.string.intent_extra_lobby_intent_package));
		
		// CONTENT VIEW ADAPTER
		lobbyViewAdapter = new LobbyViewComponentAdapter(this);
		lobbyViewAdapter.setDelegate(this);
		lobbyViewAdapter.setColorScheme(mColorScheme);
		// Set components. The LobbyViewComponentAdapter needs access to various
		// Views and such to
		// perform its functions. Set those components now.
		setLobbyViewComponentAdapterComponents((LobbyViewComponentAdapter) lobbyViewAdapter);
		lobbyViewAdapter.setSoundControls(mControlsSound);
		lobbyViewAdapter.setSoundPool(mQuantroSoundPool);
		
		// set the default availability.
		int [] modes = GameModes.getMultiPlayerGameModes() ;
		PremiumLibrary premiumLibrary = getPremiumLibrary() ;
    	for ( int i = 0; i < modes.length; i++ ) {
    		int gameMode = modes[i] ;
    		if ( premiumLibrary.hasGameMode(gameMode) )
    			lobbyViewAdapter.setGameModeAvailability(gameMode, OptionAvailability.ENABLED) ;
    		else if ( PremiumLibrary.isPremiumGameMode(gameMode) )
    			lobbyViewAdapter.setGameModeAvailability(gameMode, OptionAvailability.LOCKED_ENABLED) ;
    	}
		
		////////////////////////////////////////////////////////////////////////
		// SET LOBBY-SPECIFIC DETAILS
		initializeLobbySpecificViewDetails() ;
		
		mHandler = new Handler() ;
	
		
		// /////////////////////////////////////////////
		// REGISTER FOR ANDROID BEAM
		updateAndroidBeamRegistration() ;
		
		// Lobby info?
		mShowLobbyInformationOnConnect = mLip.isHost() ;
		
		////////////////////////////////////////////////////////////////////////
		// BIND TO SERVICE
		bindToService() ;
		
		this.activityState = ACTIVITY_STATE_CREATED ;
	}
	
	
	private void bindToService() {
		Intent serviceIntent = new Intent(this, LobbyService.class);
		serviceIntent
				.putExtra(res
						.getString(R.string.intent_extra_lobby_intent_package),
						mLip);

		bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
	}
	 
	
	/**
	 * Construct the LobbyInfo dialog.  This does NOT perform any customization;
	 * i.e., it does not need to be repeated if lobby information changes.
	 * @return
	 */
	private AlertDialog makeLobbyInformationDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this) ;
		builder.setView(R.layout.dialog_alert_lobby_information) ;
		builder.setTitle(R.string.in_lobby_dialog_information_title) ;
		builder.setIcon(R.drawable.icon_about_baked) ;
		builder.setCancelable(true);
		// game.
		builder.setPositiveButton(R.string.in_lobby_dialog_information_button_share,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialogManager.dismissDialog(DIALOG_LOBBY_INFORMATION) ;
						userShare() ;
					}
				}) ;
		builder.setNegativeButton(R.string.in_lobby_dialog_information_button_dismiss,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialogManager.dismissDialog(DIALOG_LOBBY_INFORMATION) ;
					}
				}) ;
		
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				dialogManager.dismissDialog(DIALOG_LOBBY_INFORMATION);
			}
		});

		return builder.create();
	}
	

	private void setLobbyViewComponentAdapterComponents(
			LobbyViewComponentAdapter lvca) {
		// Set membership, population, launch description
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_MEMBERSHIP_PAIR_1,
				findViewById(R.id.lobby_membership_pair_1_text_view));
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_MEMBERSHIP_PAIR_2,
				findViewById(R.id.lobby_membership_pair_2_text_view));
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_MEMBERSHIP_PAIR_3,
				findViewById(R.id.lobby_membership_pair_3_text_view));
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_MEMBERSHIP_PAIR_4,
				findViewById(R.id.lobby_membership_pair_4_text_view));
		// Set vote list
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_GAME_MODE_LIST,
				findViewById(R.id.lobby_game_mode_list));
		// Set log list
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOG_LIST,
				findViewById(R.id.lobby_event_list));
		// Chat edit text and post button
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_CHAT_EDIT_TEXT,
				findViewById(R.id.lobby_chat_edit_text));
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_CHAT_POST_BUTTON,
				findViewById(R.id.lobby_chat_post_button));

		// Set vote, etc.
		lvca.setComponent(
				LobbyViewComponentAdapter.COMPONENT_INSTRUCTIONS_CHAT,
				findViewById(R.id.lobby_instructions_chat_text_view));
	}
	
	private void setLobbyViewComponentAdapterComponentsInfoView( LobbyViewComponentAdapter lvca, View infoView ) {
////	////////////////////////////////////////////////////////////////////
		// INFO TAB / DIALOG / SECTION
		// Set lobby name, lobby description.  This is info-tab stuff.
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_NAME,
				infoView.findViewById(R.id.lobby_information_name_text_view));
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_AVAILABILITY_DESCRIPTION,
				infoView.findViewById(R.id.lobby_information_availability_text_view));
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_INVITATION_DESCRIPTION,
				infoView.findViewById(R.id.lobby_information_invitation_text_view));
		lvca.setComponent(LobbyViewComponentAdapter.COMPONENT_LOBBY_DETAILS,
				infoView.findViewById(R.id.lobby_information_details_text_view));
		
		
		lvca.setComponent(
				LobbyViewComponentAdapter.COMPONENT_INSTRUCTIONS_GENERAL,
				infoView.findViewById(R.id.lobby_information_instructions_general_text_view));
	}

	private void setupTitleBarStrip() {

		lobbyTitleBarButtonStrip = (LobbyTitleBarButtonStrip) findViewById(R.id.lobby_title_bar_button_strip);
		if (lobbyTitleBarButtonStrip != null) {
			lobbyTitleBarButtonStrip.setIsHost(mLip.isHost());
			if ( mLip.isDirect() )
				lobbyTitleBarButtonStrip.setIsWifi() ;
			else {
				InternetLobby il = mLip.internetLobby ;
				if ( il.isPublic() )
					lobbyTitleBarButtonStrip.setIsInternetPublic() ;
				else if ( il.isItinerant() )
					lobbyTitleBarButtonStrip.setIsInternetItinerant() ;
				else
					lobbyTitleBarButtonStrip.setIsInternetPrivate() ;
			}
			lobbyTitleBarButtonStrip
					.setActiveTab(LobbyTitleBarButtonStrip.BUTTON_TAB_VOTE);
			lobbyTitleBarButtonStrip.setDelegate(this);
			lobbyTitleBarButtonStrip.refresh();
			
			if ( lobbyViewAdapter instanceof LobbyViewComponentAdapter )
				((LobbyViewComponentAdapter)lobbyViewAdapter).setLobbyTitleBarButtonStrip(lobbyTitleBarButtonStrip) ;
		}
	}

	// Some storage for these views.
	View mChatTabContent = null;
	View mVoteTabContent = null;

	private void setupContentTabs() {
		// We have two tab buttons - chat and vote. Set onClick listeners
		// for each that 1. set themselves "glow" and the rest "glow off,"
		// and 2. set their corresponding content "VISIBLE" and the rest "GONE."

		// Load the views...
		mChatTabContent = findViewById(R.id.lobby_tab_chat);
		mVoteTabContent = findViewById(R.id.lobby_tab_vote);
		
		// Default: start with votes.
		mChatTabContent.setVisibility(View.GONE);
		mVoteTabContent.setVisibility(View.VISIBLE);
		if (lobbyTitleBarButtonStrip != null)
			lobbyTitleBarButtonStrip
					.setActiveTab(LobbyTitleBarButtonStrip.BUTTON_TAB_VOTE);
	}

	private void setupColoredElements() {
		int colorResID ;
		if ( mLip.isDirect() )
			colorResID = R.color.lobby_wifi ;
		else {
			InternetLobby l = mLip.internetLobby ;
			if ( l.isPublic() )
				colorResID = R.color.internet_lobby_public ;
			else if ( l.isItinerant() )
				colorResID = R.color.internet_lobby_itinerant ;
			else
				colorResID = R.color.lobby_internet ;
		}
		
		int color = getResources().getColor( colorResID ) ;
	}

	private void setupButtonContent() {
		QuantroButton chatPostButton = (QuantroButton) findViewById(R.id.lobby_chat_post_button);
		if (chatPostButton != null) {
			((ImageView) chatPostButton.getContentView().findViewById(
					R.id.button_content_image_drawable)).setImageDrawable(res
					.getDrawable(R.drawable.action_send_baked));
		}
	}

	private void setupButtonColor() {
		QuantroButton chatPostButton = (QuantroButton) findViewById(R.id.lobby_chat_post_button);
		if (chatPostButton != null) {
			chatPostButton
					.setColor(((LobbyViewComponentAdapter) lobbyViewAdapter)
							.getPlayerColor(lobby.getLocalPlayerSlot()));
		}
	}
	
	
	private void initializeLobbySpecificViewDetails() {
		// set lobby-specific details
		lobbyViewAdapter.setLobby(mLip.internetLobby == null ? new Lobby()
				: mLip.internetLobby); // provide a fake lobby to prevent
										// crashes
		lobbyViewAdapter.setLobbyLog(new LobbyLog()); // again, a fake lobbyLog
														// to prevent crashes.
		lobbyViewAdapter.setIsHost(isHost());
		lobbyViewAdapter.setIsWifiOnly(mLip.isDirect());

		// TABS FOR CONTENT, EXTRA BUTTON CONTENT
		// These calls might base their operation on mLip.
		setupContentTabs();
		setupButtonContent();
		setupColoredElements();
		setupTitleBarStrip();

		// We should monitor Wifi status, if indeed this is a Wifi game.
		if (mLip.isDirect()) {
			mWifiMonitor = new WifiMonitor(this, this);
		} else {
			mWifiMonitor = null;
		}
	}
	
	
	
	private void updateAndroidBeamRegistration() {
		// we do most of this in an AsyncTask.  Only the actual registration
		// takes place on the main thread, because we want to make sure the Activity
		// hasn't been destroyed.
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		boolean hasBeam = nfcAdapter != null && nfcAdapter.isEnabled() && VersionCapabilities.supportsNfcNdefPushMessage() ;
		if ( hasBeam ) {
			new AsyncTask<Object, Object, NdefMessage>() {
				@Override
				protected NdefMessage doInBackground(Object... params) {
					return makeNdefMessage() ;
				}
				
				@Override
				protected void onPostExecute(NdefMessage message) {
					if ( message != null && activityState != ACTIVITY_STATE_DESTROYED
							&& (mLastMessageSet == null || !mLastMessageSet.equals(message) )) {
						NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(LobbyActivity.this) ;
						if ( nfcAdapter != null && nfcAdapter.isEnabled() ) {
							Log.d(TAG, "AndroidBeam message set: " + message) ;
							// set a callback for Android Beam
							nfcAdapter.setNdefPushMessage(message, LobbyActivity.this) ;
							mLastMessageSet = message ;
						}
			        }
			    }
			}.execute() ;
		}
	}
	
	
	private NdefMessage makeNdefMessage() {
		NdefMessage msg = null ;
		
		// If Matchseeker, send a standard lobby invitation.
		if ( mLip.isMatchseeker() ) {
			// construct a message with the following content:
			// 0 lobby URI
			// 1 application code
			LobbyService service = lobbyService ;
			Lobby lobby = service == null ? null : service.getLobby() ;
			InternetLobby il = lobby == null || !(lobby instanceof InternetLobby)
					? null : (InternetLobby) lobby ;
			String encodedLobby = il == null
					? null
					: LobbyStringEncoder.toFullString(il) ;
			
			if ( encodedLobby != null ) {
				msg = NdefMessage.newNdefMessage(
						new NdefRecord[] {
								NdefRecord.createUri(encodedLobby),
								NdefRecord.createApplicationRecord(res.getString(R.string.package_name))
						} ) ;
			}
		} else {
			// WiFi!  Send the lobby WiFi address.
			WifiMonitor monitor = mWifiMonitor ;
			LobbyService service = lobbyService ;
			Lobby lobby = service == null ? null : service.getLobby() ;
			if ( monitor != null ) {
				int ip_int = monitor.getWifiIpAddress() ;
				String ip = ip_int == 0 ? null : WifiMonitor.ipAddressToString(ip_int) ;
				Nonce lobbyNonce = lobby == null ? null : lobby.getLobbyNonce() ;
				String lobbyName = lobby == null ? null : lobby.getLobbyName() ;
				String hostName = lobby == null || lobby.getMaxPeople() > 1 ? null : lobby.getPlayerName(0) ;
				if ( ip != null ) {
					String encodedURI = LobbyStringEncoder.encodeWiFiLobby(ip, lobbyNonce, lobbyName, hostName) ;
					if ( encodedURI != null ) {
						NdefRecord [] records = new NdefRecord[] {
								NdefRecord.createUri(encodedURI),
								NdefRecord.createApplicationRecord(res.getString(R.string.package_name))
						} ;
						for ( int i = 0; i < records.length; i++ ) {
							Log.d(TAG, "making a new NDEF message with record " + i + " " + records[i]) ;
						}
						msg = NdefMessage.newNdefMessage( records ) ;
					}
				}
			}
		}
		
		return msg ;
	}
	
	
	private void resetWifiMonitor() {
		if (mHandler != null && mWifiMonitor != null) {
			mHandler.removeCallbacks(mWifiMonitor) ;
			if (activityShown)
				mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_UPDATE_FREQUENCY);
		}
	}
	
	
	
	/**
	 * Switches this activity from its current Lip to mPendingLip.  Among
	 * other things, this resets the associated Service (If connected...),
	 * dismisses all currently displayed dialogs, and switches up the views.
	 */
	synchronized private void switchToPendingLip() {
		// dismiss all dialogs.
		dialogManager.dismissAllDialogs() ;
		
		// switch off WifiMonitoring.
		if ( mWifiMonitor != null )
			mHandler.removeCallbacks(mWifiMonitor) ;
		
		// switch LIP.
		mLip = mPendingLip ;
		mPendingLip = null ;
		
		// Reset lobby-specific views.
		initializeLobbySpecificViewDetails() ;
		
		// Wifi monitoring?
		resetWifiMonitor() ;
		
		// Lobby info?
		mShowLobbyInformationOnConnect = mLip.isHost() ;
		
		// register with service
		if ( lobbyService != null )
			resetServiceConnection() ;
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(TAG, "Activity onStart");

		// Reveal any previously hidden dialogs.
		dialogManager.revealDialogs();

		activityShown = true;
		
		// update availability
		if ( this.lobbyService != null ) {
			int [] modes = GameModes.getMultiPlayerGameModes() ;
			PremiumLibrary premiumLibrary = getPremiumLibrary() ;
	    	for ( int i = 0; i < modes.length; i++ ) {
	    		int gameMode = modes[i] ;
	    		if ( premiumLibrary.hasPremiumGameMode(gameMode) )
	    			lobbyService.updateGameModeAuthorization(gameMode, true, false) ;
	    		else if ( premiumLibrary.isPremiumGameMode(gameMode) )
	    			lobbyService.updateGameModeAuthorization(gameMode, false, false) ;
	    	}
		}

		activityState = ACTIVITY_STATE_STARTED;
		if (lobbyService != null)
			lobbyService.lobbyActivityDidStart(this);
	}

	// SYNCHRONIZED so we can easily access mHandler vs.
	// the WifiMonitor run() callback.
	@Override
	synchronized protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");

		lobbyViewAdapter.start();

		// start wifi monitor.
		if (mHandler != null && mWifiMonitor != null) {
			mWifiMonitor.run();
			mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_UPDATE_FREQUENCY);
		}

		// Refresh preferences
		mColorScheme = new ColorScheme(
				this,
				QuantroPreferences.getSkinQuantro(this).getColor(),
				QuantroPreferences.getSkinRetro(this).getColor() ) ;
		mControlsSound = QuantroPreferences.getSoundControls(this);
		NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this) ;
		boolean hasBeam = nfcAdapter != null && nfcAdapter.isEnabled() && VersionCapabilities.supportsNfcNdefPushMessage() ;
		lobbyViewAdapter.setColorScheme(mColorScheme);
		lobbyViewAdapter.setSoundControls(mControlsSound);
		lobbyViewAdapter.setAndroidBeamIsEnabled( hasBeam ) ;
		
		Log.d(TAG, "setAndroidBeamIsEnabled to " + hasBeam) ;
		
		if (lobby != null) {
			String oldName = lobby.getPlayerName(lobby.getLocalPlayerSlot());
			String name = QuantroPreferences.getMultiplayerName(this);
			if (!name.equals(oldName))
				lvad_userSetName(name);
		}
		
		// /////////////////////////////////////////////
		// REGISTER FOR ANDROID BEAM
		updateAndroidBeamRegistration() ;

		activityState = ACTIVITY_STATE_RESUMED;
		if (lobbyService != null)
			lobbyService.lobbyActivityDidResume(this);
	}

	@Override
	synchronized protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");

		lobbyViewAdapter.stop();

		// stop wifi monitor.
		if (mHandler != null && mWifiMonitor != null)
			mHandler.removeCallbacks(mWifiMonitor);

		activityState = ACTIVITY_STATE_PAUSED;
		if (lobbyService != null)
			lobbyService.lobbyActivityDidPause(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");

		activityShown = false;

		// Hide any dialogs.
		dialogManager.hideDialogs();

		activityState = ACTIVITY_STATE_STOPPED;
		if (lobbyService != null)
			lobbyService.lobbyActivityDidStop(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");

		activityState = ACTIVITY_STATE_DESTROYED;
		if (lobbyService != null)
			lobbyService.lobbyActivityDidDestroy(this);

		unbindService(this);
	}

	/*
	 * *************************************************************************
	 * 
	 * INTENT RESULT
	 * 
	 * When we launch games, we get the result here.
	 * 
	 * *************************************************************************
	 */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ( super.handleActivityResult(requestCode, resultCode, data) )
    		return ;
    	
		
		Log.d(TAG, "onActivityResult: " + requestCode + " " + resultCode);
		// Dismiss the launching dialog.
		dialogManager.dismissDialog(DIALOG_LAUNCHING_GAME_ID);

		switch (requestCode) {
		case IntentForResult.PLAY:
			if (resultCode == GameActivity.RESULT_CODE_FINISHED) {
				GameResult gr = (GameResult) data
						.getSerializableExtra(GameActivity.INTENT_EXTRA_GAME_RESULT);

				// We assume the game is terminated. Note it in our database.
				try {
				if ( gr != null )
					GameStats.DatabaseAdapter.addGameResultToDatabase(this, gr);
				} catch ( Exception e ) {
					e.printStackTrace() ;
				}
				
				// Achievements!
				updateAchievementsGameOver( gr ) ;

				// TODO: Report the game result, or display it to the screen, or
				// something. A game just happened!

				// clean up that game that just happened
				System.gc();
			}
			break;
			
		}
	}

	/*
	 * #RealXboxOneFacts
	 * 
	 * TV shows change your channels for you
	 * All games fee to play
	 * Telescreen receives and transmits simultaneously
	 * Mocap dog
	 * 
	 * *************************************************************************
	 * 
	 * OUR ROLE IN THIS LOBBY
	 * 
	 * These methods slightly abstract our lobby role.
	 * 
	 * *************************************************************************
	 * 
	 */

	public boolean isClient() {
		return mLip.isClient();
	}

	public boolean isHost() {
		return mLip.isHost();
	}

	/*
	 * *************************************************************************
	 * 
	 * MESSAGE PASSING MEDIATED CONNECTION DELEGATE METHODS
	 * 
	 * These methods help us determine what role to play in this lobby - host or
	 * client. If we are Mediated, then we're using a
	 * MessagePassingMediatedConnection.
	 * 
	 * Procedure: send a LobbyHostPriority with an integer determined by the
	 * "# connected" response from the server. Include a random value to prevent
	 * accidental deadlock. e.g., if the number is 0, use a random int from 0 to
	 * 999; if 1, use 1000 to 1999.
	 * 
	 * Mediated Connections: upon connecting, send the LobbyHostPriority
	 * message. Upon receive such a message, check whether their priority is >
	 * ours. If ours is greater, we become the Host, and the connection is to
	 * the Client. If ours is lower, we become the Client, and the connection is
	 * to the Host. If they are =, resend.
	 * 
	 * *************************************************************************
	 */

	/*
	 * *************************************************************************
	 * 
	 * SERVICE CONNECTION METHODS
	 * 
	 * These methods define our ServiceConnection interface. The GameService
	 * handles most of our processing and game logic, whereas the GameActivity
	 * handles the GUI.
	 * 
	 * *************************************************************************
	 */

	/**
	 * This method is called when the GameService connects to this Activity. Now
	 * is a good time to collect the references we need, such as game objects,
	 * and connect them with our views and stuff. In addition, we provide the
	 * Service with a reference to ourself and give it
	 */
	public void onServiceConnected(ComponentName name, IBinder service) {

		// We ignore the component name, because we only bind with LobbyService.
		// Get the service from the binder, and store a reference.
		lobbyService = (LobbyService) (((ServicePassingBinder) service)
				.getService());
		resetServiceConnection() ;
	}
	
	
	private void resetServiceConnection() {
		// Give the service a reference to this, and make any state update
		// calls needed.
		lobbyService.setActivity(this, mLip);
		lobbyService.setLobbyUserInterface(this);

		// Get our lobby
		lobby = lobbyService.getLobby();
		lobbyLog = lobbyService.getLobbyLog();
		// Modify slightly
		lobbyViewAdapter.setLobby(lobby);
		lobbyViewAdapter.setLobbyLog(lobbyLog);
		if (lobbyTitleBarButtonStrip != null) {
			lobbyTitleBarButtonStrip.setLobby(lobby);
			lobbyTitleBarButtonStrip.refresh();
		}
		
		int [] modes = GameModes.getMultiPlayerGameModes() ;
		PremiumLibrary premiumLibrary = getPremiumLibrary() ;
    	for ( int i = 0; i < modes.length; i++ ) {
    		int gameMode = modes[i] ;
    		if ( premiumLibrary.hasPremiumGameMode(gameMode) )
    			lobbyService.updateGameModeAuthorization(gameMode, true, false) ;
    		else if ( premiumLibrary.isPremiumGameMode(gameMode) )
    			lobbyService.updateGameModeAuthorization(gameMode, false, false) ;
    	}

		// We make successive calls using activityState; if activityState is
		// RESUMED,
		// for example, we make all calls up-to and including the call for
		// resume.
		// This would be easy if we made calls in REVERSE (just do a
		// fall-through switch)
		// but they need to be in-order.

		// Instead, exploit the fact that the ACTIVITY_STATE values are in
		// ascending order.
		if (activityState >= ACTIVITY_STATE_CREATED)
			lobbyService.lobbyActivityDidCreate(this);
		if (activityState >= ACTIVITY_STATE_STARTED)
			lobbyService.lobbyActivityDidStart(this);
		if (activityState >= ACTIVITY_STATE_RESUMED)
			lobbyService.lobbyActivityDidResume(this);
		if (activityState >= ACTIVITY_STATE_PAUSED)
			lobbyService.lobbyActivityDidPause(this);
		if (activityState >= ACTIVITY_STATE_STOPPED)
			lobbyService.lobbyActivityDidStop(this);
		if (activityState >= ACTIVITY_STATE_DESTROYED)
			lobbyService.lobbyActivityDidDestroy(this);
		
		// /////////////////////////////////////////////
		// REGISTER FOR ANDROID BEAM
		updateAndroidBeamRegistration() ;
	}

	/**
	 * The service has disconnected. Why? We ONLY unbind upon destruction!
	 */
	public void onServiceDisconnected(ComponentName className) {
		// Meh.
		lobbyService = null;
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
    		id = mLip.isDirect()
    				? R.menu.lobby_overflow_wifi_normal
    				: R.menu.lobby_overflow_internet_normal ;
        	break ;
    	case VersionSafe.SCREEN_SIZE_LARGE:
    		id = mLip.isDirect()
					? R.menu.lobby_overflow_wifi_large
					: R.menu.lobby_overflow_internet_large ;
        	break ;	
    	case VersionSafe.SCREEN_SIZE_XLARGE:
    		id = mLip.isDirect()
					? R.menu.lobby_overflow_wifi_xlarge
					: R.menu.lobby_overflow_internet_xlarge ;
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
		switch (item.getItemId()) {
		case R.id.overflow_options_menu_invite:
			// Can only share InternetLobbies.
			if ( lobby == null || !(lobby instanceof InternetLobby) )
				return false ;
			userShare() ;
			return true ;
		case R.id.overflow_options_menu_lobby_info:
			dialogManager.showDialog(DIALOG_LOBBY_INFORMATION) ;
			return true ;
		case R.id.overflow_options_menu_help:
			dialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP);
			return true;
		case R.id.overflow_options_menu_settings:
			userActionLaunchSettings();
			return true;
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		ProgressDialog pd;
		AlertDialog.Builder builder;
		
		if ( GlobalDialog.hasDialog(id) )
			return GlobalDialog.onCreateDialog(this, id, dialogManager) ;

		// TODO: We are migrating access to dialog strings form direct
		// res.getString access to the DialogFormatting class. Perform
		// these changes here (see onPrepareDialog for examples).

		int display = TextFormatting.DISPLAY_DIALOG;

		switch (id) {
			// Spinning pinwheel for "connecting;" differs depending
			// on whether we are the host or a client.
		case DIALOG_CONNECTING_TO_LOBBY_ID:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage("");
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.networkDialogCanceled();
				}
			});
			return pd;

			// Spinning pinwheel for "talking to"
		case DIALOG_TALKING_TO_LOBBY_ID:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage("");
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.networkDialogCanceled();
				}
			});
			return pd;

			// "Waiting for" dialog. Since this dialog is NOT generic, we
			// instantiate
			// everything but the message shown, which will be edited in
			// onPrepareDialog.
		case DIALOG_LAUNCHING_GAME_ID:
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(""); // Message is set in onPrepareDialog. This call
								// resolves a display bug (so Alert text is not
								// set to invisible).
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.launchDialogCanceled();
				}
			});
			return pd;

			// Leaving lobby is a generic message (i.e., we only care
			// whether we are host or client, which we know on creation).
		case DIALOG_LEAVING_LOBBY_ID:
			// This is a true dialog - with choices.
			builder = new AlertDialog.Builder(this);
			builder.setTitle("") ;
			builder.setMessage("");
			builder.setCancelable(true); // cancelable so "back" button
											// dismisses without leaving the
											// game.
			builder.setPositiveButton(
					"Stub",		// set in onPrepareDialog
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialogManager
									.dismissDialog(DIALOG_LEAVING_LOBBY_ID);
							mHandler.post(new Runnable() {
								@Override
								public void run() {
									LobbyActivity.this.quitLobby(false);
								}
							}) ;
							mHandler.postDelayed(new Runnable() {
								@Override
								public void run() {
									LobbyActivity.this.quitLobby(true);
								}
							}, 500);
						}
					});

			// This only applies for hosted lobbies, so will be set visible / enabled
			// in onPrepareDialog appropriately.
			builder.setNeutralButton(R.string.in_lobby_dialog_quit_as_host_home_button,
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialogManager
							.dismissDialog(DIALOG_LEAVING_LOBBY_ID);
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							Intent startMain = new Intent(Intent.ACTION_MAIN);
							startMain.addCategory(Intent.CATEGORY_HOME);
							startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(startMain);
						}
					}, 500);
				}
			});

			builder.setNegativeButton(
					"Stub",		// set in onPrepareDialog
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialogManager
									.dismissDialog(DIALOG_LEAVING_LOBBY_ID);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					dialogManager.dismissDialog(DIALOG_LEAVING_LOBBY_ID);
				}
			});

			return builder.create();

			// Lobby was closed.
		case DIALOG_HOST_CLOSED_LOBBY_FOREVER_ID:
			// This is a true dialog, but with only one choice. However,
			// because WHO QUIT is significant, we leave it to onPrepareDialog
			// to actually configure the message.
			builder = new AlertDialog.Builder(this);
			builder.setCancelable(false); // NOT cancelable - there is no lobby
											// to interact with after this.
			builder.setPositiveButton(
					res.getString(R.string.in_lobby_dialog_lobby_closed_forever_quit_button),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							LobbyActivity.this.quitLobby(true);
						}
					});
			builder.setMessage(""); // Resolves a bug - if instanted
									// with 'null' message, i.e. if no
									// call to 'setMessage' here, then
									// message visibily will be set to
									// 'false' and a message set later
									// will not display.
			return builder.create();

		case DIALOG_MATCHMAKING_NO_RESPONSE_ID:
			// A spinny pinwheel.
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(res
					.getString(R.string.in_lobby_dialog_matchmaking_no_response));
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.queryOrQuit();
				}
			});
			return pd;

		case DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID:
			// A spinny pinwheel.
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(res
					.getString(R.string.in_lobby_dialog_matchmaking_reject_full));
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.queryOrQuit();
				}
			});
			return pd;

		case DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID:
			// A spinny pinwheel.
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(res
					.getString(R.string.in_lobby_dialog_matchmaking_reject_port_randomization));
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.queryOrQuit();
				}
			});
			return pd;

		case DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID:
			// An interactive dialog; the button quits.
			// Nothing here is dependent on player names or any other
			// session-specific info.
			builder = new AlertDialog.Builder(this);
			builder.setCancelable(true); // Cancelable. Hitting "back" will
											// quit.
			builder.setTitle(R.string.in_lobby_dialog_matchmaking_reject_title);
			builder.setMessage(R.string.in_lobby_dialog_matchmaking_reject_invalid_nonce);
			builder.setNegativeButton(
					R.string.in_lobby_dialog_matchmaking_reject_button_quit,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							quitLobby(true);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					quitLobby(true);
				}
			});
			return builder.create();

		case DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID:
			// An interactive dialog; the button quits.
			// Nothing here is dependent on player names or any other
			// session-specific info.
			builder = new AlertDialog.Builder(this);
			builder.setCancelable(true); // Cancelable. Hitting "back" will
											// quit.
			builder.setTitle(R.string.in_lobby_dialog_matchmaking_reject_title);
			builder.setMessage(R.string.in_lobby_dialog_matchmaking_reject_nonce_in_use);
			builder.setNegativeButton(
					R.string.in_lobby_dialog_matchmaking_reject_button_quit,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							quitLobby(true);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					quitLobby(true);
				}
			});
			return builder.create();

		case DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID:
			// An interactive dialog; the button quits.
			// Nothing here is dependent on player names or any other
			// session-specific info.
			builder = new AlertDialog.Builder(this);
			builder.setCancelable(true); // Cancelable. Hitting "back" will
											// quit.
			builder.setTitle(R.string.in_lobby_dialog_matchmaking_reject_title);
			builder.setMessage(R.string.in_lobby_dialog_matchmaking_reject_unspecified);
			builder.setNegativeButton(
					R.string.in_lobby_dialog_matchmaking_reject_button_quit,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							quitLobby(true);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					quitLobby(true);
				}
			});
			return builder.create();

		case DIALOG_NO_WIFI_CONNECTION:
			// A progress dialog.
			display = TextFormatting.DISPLAY_DIALOG;
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(TextFormatting.format(this, res,
					TextFormatting.DISPLAY_DIALOG,
					TextFormatting.TYPE_LOBBY_NO_WIFI,
					TextFormatting.ROLE_CLIENT, (String[]) null));
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.queryOrQuit();
				}
			});
			return pd;
			
        case DIALOG_LOBBY_WEB_COMMUNICATION_FAILURE:
			// trouble communicating.
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(R.string.in_lobby_dialog_cgi_communication_failure_message) ;
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.queryOrQuit();
				}
			});
			return pd;
			
			
		case DIALOG_LOBBY_WEB_CLOSED:
			// permanent communication failure
			builder = new AlertDialog.Builder(this);
			builder.setCancelable(true); // Cancelable. Hitting "back" will
											// quit.
			builder.setTitle(R.string.in_lobby_dialog_cgi_game_closed_title) ;
			builder.setMessage(R.string.in_lobby_dialog_cgi_game_closed_message) ;
			builder.setNegativeButton(
					R.string.in_lobby_dialog_cgi_game_closed_button_quit,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							quitLobby(true);
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					quitLobby(true);
				}
			});
			return builder.create();
			
			
		case DIALOG_LOBBY_WEB_FULL:
			// trouble communicating.
			pd = new ProgressDialog(this);
			pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pd.setCancelable(true); // Cancelable so 'back' button still works
									// to quit
			pd.setMessage(R.string.in_lobby_dialog_cgi_full_message) ;
			pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					LobbyActivity.this.queryOrQuit();
				}
			});
			return pd;
			
			
		case DIALOG_LOBBY_INFORMATION:
			// Information about this lobby.
			if ( lobbyInformationDialog == null ) {
				lobbyInformationDialog = makeLobbyInformationDialog() ;
				lobbyInformationContentView = lobbyInformationDialog.getContentView() ;
				setLobbyViewComponentAdapterComponentsInfoView(
						(LobbyViewComponentAdapter) lobbyViewAdapter,
						lobbyInformationContentView) ;
				lobbyViewAdapter.update_basics() ;
			}
			return lobbyInformationDialog ;
			
			
		case DIALOG_SWITCH_TO_PENDING_LOBBY_ID:
			// switch from this lobby to a new one.
			// Message, title, and button-colors will be set in onPrepare,
			// since they depend on the current and pending LIP settings.
			builder = new AlertDialog.Builder(this);
			builder.setCancelable(true); // Cancelable. Hitting "back" will
											// quit.
			builder.setTitle(R.string.in_lobby_dialog_switch_lobby_title) ;
			builder.setMessage("Stub") ;
			builder.setPositiveButton(
					res.getString(R.string.in_lobby_dialog_switch_lobby_button_yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialogManager.dismissDialog(DIALOG_SWITCH_TO_PENDING_LOBBY_ID) ;
							switchToPendingLip() ;
							mPendingLip = null ;
						}
					});
			builder.setNegativeButton(
					res.getString(R.string.in_lobby_dialog_switch_lobby_button_no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialogManager.dismissDialog(DIALOG_SWITCH_TO_PENDING_LOBBY_ID) ;
							mPendingLip = null ;
						}
					});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					dialogManager.dismissDialog(DIALOG_SWITCH_TO_PENDING_LOBBY_ID) ;
					mPendingLip = null ;
				}
			});
			return builder.create();
			

		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		Log.d(TAG, "onPrepareDialog " + id);
		// THIS METHOD IS DEPRECIATED
		// (and I don't care! the DialogFragment class will call through
		// to this method for compatibility)

		if ( GlobalDialog.hasDialog(id) ) {
			GlobalDialog.onPrepareDialog(this, id, dialog) ;
			return ;
		}
			
		// display, role for DialogFormatting
		int display = TextFormatting.DISPLAY_DIALOG;
		int role = (this.isHost()) ? TextFormatting.ROLE_HOST
				: TextFormatting.ROLE_CLIENT;
		int type;
		boolean isHost = this.isHost() ;

		String text;
		switch (id) {
		case DIALOG_CONNECTING_TO_LOBBY_ID:
			type = TextFormatting.TYPE_LOBBY_CONNECTING;
			text = TextFormatting.format(this, res, display, type, role, -1,
					lobby);
			((ProgressDialog) dialog).setMessage(text);
			break;
		case DIALOG_TALKING_TO_LOBBY_ID: // Talking is a generic dialog.
			type = TextFormatting.TYPE_LOBBY_CONNECTING;
			text = TextFormatting.format(this, res, display, type, role, -1,
					lobby);
			((ProgressDialog) dialog).setMessage(text);
			break;

		// "Launching game" dialog. This is not generic; we display the
		// opponent's name.
		case DIALOG_LAUNCHING_GAME_ID:
			if (launchingWithPlayers == null)
				text = "";
			else {
				boolean[] opponent = new boolean[this.launchingWithPlayers.length];
				for (int i = 0; i < this.launchingWithPlayers.length; i++)
					opponent[i] = this.launchingWithPlayers[i]
							&& i != lobby.getLocalPlayerSlot();
				
				for ( int i = 0; i < this.launchingWithPlayers.length; i++ ) {
					Log.d(TAG, "DIALOG_LAUNCHING " +i + lobby.getPlayerNames()[i]+ " : " + opponent[i]) ;
				}
				
				// Set as the appropriate string, with placeholder replaced.
				text = TextFormatting.format(this, res, display,
						TextFormatting.TYPE_LOBBY_LAUNCHING, role,
						ArrayOps.clone(lobby.getPlayerNames()), opponent);
			}
			((ProgressDialog) dialog).setMessage(text);
			break;

		// Leaving lobby: a generic dialog.
		case DIALOG_LEAVING_LOBBY_ID:
			type = TextFormatting.TYPE_LOBBY_QUIT;
			text = TextFormatting.format(this, res, display, type, role, -1,
					lobby);
			((AlertDialog) dialog).setMessage(text);
			((AlertDialog) dialog).setTitle(
					role == TextFormatting.ROLE_HOST
					? R.string.in_lobby_dialog_quit_as_host_title
					: R.string.in_lobby_dialog_quit_as_client_title ) ;
			((AlertDialog) dialog).setButtonTitle(
					AlertDialog.BUTTON_POSITIVE,
					(isHost)
							? res.getString(R.string.in_lobby_dialog_quit_as_host_yes_button)
							: res.getString(R.string.in_lobby_dialog_quit_as_client_yes_button)) ;
			
			((AlertDialog) dialog).setButtonEnabled(AlertDialog.BUTTON_NEUTRAL, isHost) ;
			((AlertDialog) dialog).setButtonVisible(AlertDialog.BUTTON_NEUTRAL, isHost) ;
			
			((AlertDialog) dialog).setButtonTitle(
					AlertDialog.BUTTON_NEGATIVE,
					(isHost)
							? res.getString(R.string.in_lobby_dialog_quit_as_host_no_button)
							: res.getString(R.string.in_lobby_dialog_quit_as_client_no_button) ) ;
			break;

		// Lobby owner has closed the lobby.
		case DIALOG_HOST_CLOSED_LOBBY_FOREVER_ID:
			// Set as the appropriate string, with placeholder replaced.
			text = TextFormatting.format(this, res, display,
					TextFormatting.TYPE_LOBBY_CLOSED_FOREVER, role, -1, lobby);
			((AlertDialog) dialog).setMessage(text);
			break;

		case DIALOG_NO_WIFI_CONNECTION:
			break;
			
		case DIALOG_LOBBY_WEB_COMMUNICATION_FAILURE:
		case DIALOG_LOBBY_WEB_CLOSED:
			break ;
			
		case DIALOG_LOBBY_INFORMATION:
			lobbyViewAdapter.update_basics() ;
			int shareColor = 0 ;
			boolean shareEnabled = false ;
			if ( lobby instanceof InternetLobby ) {
				shareEnabled = true ;
				if ( ((InternetLobby)lobby).isPublic() )
					shareColor = res.getColor(R.color.internet_lobby_public) ;
				else if ( ((InternetLobby)lobby).isItinerant() )
					shareColor = res.getColor(R.color.internet_lobby_itinerant) ;
				else
					shareColor = res.getColor(R.color.internet_lobby_invited) ;
			}
			lobbyInformationDialog.setButtonColor(AlertDialog.BUTTON_POSITIVE, shareColor) ;
			lobbyInformationDialog.setButtonEnabled(AlertDialog.BUTTON_POSITIVE, shareEnabled) ;
			lobbyInformationDialog.setButtonVisible(AlertDialog.BUTTON_POSITIVE, shareEnabled) ;
			break ;
			
		case DIALOG_SWITCH_TO_PENDING_LOBBY_ID:
			// Set the Title, Message, button text and button
			// color according to the current Lobby (LIP) and the 
			// pending LIP.  Button function has already been
			// set; just do the asthetics.
			String name1 = lobby == null ? null : lobby.getLobbyName() ;
			String name2 = mPendingLip.lobbyName ;
			String name1_placeholder = res.getString(R.string.placeholder_names_array_name_1) ;
			String name2_placeholder = res.getString(R.string.placeholder_names_array_name_2) ;
			String message ;
			// set message?
			if ( name1 == null ) {
				message = mPendingLip.isHost()
						? res.getString(R.string.in_lobby_dialog_switch_lobby_message_unknown_to_host)
						: res.getString(R.string.in_lobby_dialog_switch_lobby_message_unknown_to_client) ;
			} else if ( mLip.isHost() ) {
				message = mPendingLip.isHost()
						? res.getString(R.string.in_lobby_dialog_switch_lobby_message_host_to_host)
						: res.getString(R.string.in_lobby_dialog_switch_lobby_message_host_to_client) ;
			} else {
				message = mPendingLip.isHost()
						? res.getString(R.string.in_lobby_dialog_switch_lobby_message_client_to_host)
						: res.getString(R.string.in_lobby_dialog_switch_lobby_message_client_to_client) ;
			}
			
			if ( name1 != null )
				message = message.replace(name1_placeholder, name1) ;
			message = message.replace(name2_placeholder, name2) ;
			
			int color = res.getColor(R.color.lobby_join) ;
			if ( mPendingLip.isDirect() )
				color = res.getColor(R.color.lobby_wifi) ;
			else if ( mPendingLip.isMatchseeker() ) {
				if ( mPendingLip.internetLobby.isPublic() )
					color = res.getColor(R.color.internet_lobby_public) ;
				else if ( mPendingLip.internetLobby.isItinerant() )
					color = res.getColor(R.color.internet_lobby_itinerant) ;
				else
					color = res.getColor(R.color.internet_lobby_invited) ;
			}
			
			((AlertDialog)dialog).setTitle(R.string.in_lobby_dialog_switch_lobby_title) ;
			((AlertDialog)dialog).setMessage(message) ;
			((AlertDialog)dialog).setButtonColor(AlertDialog.BUTTON_POSITIVE, color) ;
			break ;
			
		// The huh?
		default:
			return;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			// We might put up a dialog; we might just quit.
			queryOrQuit();
			if (mControlsSound)
				mQuantroSoundPool.menuButtonBack();
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Quits the current game. If multiplayer, sends appropriate messages first.
	 * If 'callFinish' is true, this method will explicitly call this.finish()
	 * once everything else is taken care of.
	 * 
	 * If 'callFinish' is false, we assume some other process is finishing, and
	 * this method call occurred in the middle of it - we do not "finish".
	 */
	protected void quitLobby(boolean callFinish) {
		// TODO: Implement quit.

		// tell teh service we are finishing.
		if (lobbyService != null)
			lobbyService.lobbyActivityQuitting(this);

		if (callFinish)
			this.finish();
	}

	@Override
	public void finish() {
		if (lobbyService != null)
			lobbyService.lobbyActivityFinishing(this);

		super.finish();
	}

	// ///////////////////////////////////////////////////////////////////////
	//
	// DIALOG HELPERS
	//
	// Methods to help with dialogs, including callbacks.
	//
	// ////////////////////////////////////////////////////////////////////////

	/**
	 * This method is called if a "network dialog" is canceled by the user
	 * hitting "back". It's highly likely that we should show a quit dialog now.
	 */
	public void networkDialogCanceled() {
		queryOrQuit();
	}

	/**
	 * This method is called if a "launch dialog" is canceled by the user
	 * hitting "back." It's highly likely that we should show a quit dialog now.
	 */
	public void launchDialogCanceled() {
		queryOrQuit();
	}

	/**
	 * Attempts to quit the lobby. In some cases, this quit is immediate. In
	 * others, we instead show a "quit" dialog and allow the appropriate button
	 * to perform the quit.
	 * 
	 * Our behavior is as follows:
	 * 
	 * @return
	 */
	public void queryOrQuit() {
		if (this.isHost())
			dialogManager.showDialog(DIALOG_LEAVING_LOBBY_ID);
		else {
			quitLobby(false) ;
			this.mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					quitLobby(true);
				}
				
			}, 500) ;
		}
	}

	// //////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////
	// LOBBY USER INTERFACE METHODS
	// //////////////////////////////////////////////////////////////////////////

	/*
	 * ************************************************************************
	 * 
	 * GAME CONNECTION UPDATES
	 * 
	 * Methods for informing the UI about our efforts to connect to the server.
	 * For single player games, we will likely skip right to 'connected,' so
	 * don't rely on a smooth progression between states.
	 * 
	 * For convenience, we provide updates upon each connection attempt. The UI
	 * shouldn't really do anything about this - the service determines our
	 * reconnection policy - but it might be worth informing the player about
	 * what's happening.
	 * 
	 * *************************************************************************
	 */

	/**
	 * We are requesting a ticket in preparation for a match request.
	 */
	public void lui_matchmakingRequestingTicket() {
		//Log.d(TAG, "lui_matchmakingRequestingTicket");

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRequestTicket(false);
	}

	/**
	 * We received no response from the server when requesting a ticket. We're
	 * going to keep trying, but you may want to let the user know.
	 */
	public void lui_matchmakingNoTicketNoResponse() {
		//Log.d(TAG, "lui_matchmakingNoTicketNoResponse");
		dialogManager.showDialog(DIALOG_MATCHMAKING_NO_RESPONSE_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingNoTicketNoResponse(false);
	}
	

	/**
	 * The lobby is closed.
	 */
	public void lui_matchmakingLobbyClosed() {
		//Log.d(TAG, "lui_matchmakingRejectedInvalidNonce");
		dialogManager.showDialog(DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRejectedNonceInvalid(false);
	}

	/**
	 * We are requesting a match from the matchmaker.
	 */
	public void lui_matchmakingRequestingMatch() {
		//Log.d(TAG, "lui_matchmakingRequestingMatch");

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRequest(false);
	}

	/**
	 * We received a match promise from the matchmaker.
	 */
	public void lui_matchmakingReceivedPromise() {
		//Log.d(TAG, "lui_matchmakingReceivedPromise");
		dialogManager.dismissDialogAtTier(DIALOG_TIER_MATCHMAKING_PROBLEMS);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingPromise(false);
	}

	/**
	 * We successfully matched and are now connected (probably) to our match!
	 */
	public void lui_matchmakingReceivedMatch() {
		//Log.d(TAG, "lui_matchmakingReceivedMatch");
		dialogManager.dismissDialogAtTier(DIALOG_TIER_MATCHMAKING_PROBLEMS);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingMatch(false);
	}

	/**
	 * We successfully matched and are now connected (probably) to our match!
	 */
	public void lui_matchmakingComplete() {
		//Log.d(TAG, "lui_matchmakingComplete");
		dialogManager.dismissDialogAtTier(DIALOG_TIER_MATCHMAKING_PROBLEMS);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingUDPHolePunchSucceeded(false);
	}

	/**
	 * We have failed to communicate with the matchmaker. Because this command
	 * is quite serious, it is unlikely to be called unless numerous connection
	 * attempts have failed, or we have remained in a "disconnected" state for a
	 * long period of time.
	 */
	public void lui_matchmakingNoResponse() {
		//Log.d(TAG, "lui_matchmakingNoResponse");
		dialogManager.showDialog(DIALOG_MATCHMAKING_NO_RESPONSE_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingNoResponse(false);
	}

	/**
	 * Rejected by the Matchmaker; it is too busy. Distinct from "NoResponse" in
	 * that we DID receive information from the matchmaker, we just shouldn't
	 * expect to be matched.
	 */
	public void lui_matchmakingRejectedFull() {
		//Log.d(TAG, "lui_matchmakingRejectedFull");
		dialogManager.showDialog(DIALOG_MATCHMAKING_REJECTED_TOO_BUSY_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRejectedTooBusy(false);
	}

	/**
	 * Rejected by the Matchmaker; our Nonce is invalid. This is not an error we
	 * can recover from.
	 */
	public void lui_matchmakingRejectedInvalidNonce() {
		//Log.d(TAG, "lui_matchmakingRejectedInvalidNonce");
		dialogManager.showDialog(DIALOG_MATCHMAKING_REJECTED_INVALID_NONCE_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRejectedNonceInvalid(false);
	}

	/**
	 * This is a strange error. Just display a message, stop connected, and be
	 * done with it.
	 */
	public void lui_matchmakingRejectedNonceInUse() {
		//Log.d(TAG, "lui_matchmakingRejectedNonceInUse");
		dialogManager.showDialog(DIALOG_MATCHMAKING_REJECTED_NONCE_IN_USE_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRejectedNonceInUse(false);
	}

	/**
	 * The Matchmaker is refusing to match us, because our chosen communication
	 * port does not match the port from which our request was sent. This means
	 * some form of port randomization or re-mapping is taking place on our end;
	 * it also means that UDP hole-punching will fail as currently implemented.
	 * 
	 * This is not completely unrecoverable. For example, if the user can switch
	 * from a WiFi network to their data plan (or vice-versa) this problem might
	 * be resolved.
	 * 
	 */
	public void lui_matchmakingRejectedPortRandomization() {
		//Log.d(TAG, "lui_matchmakingRejectedPortRandomization");
		dialogManager
				.showDialog(DIALOG_MATCHMAKING_REJECTED_PORT_RANDOMIZATION_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRejectedPortRandomization(false);
	}

	/**
	 * An unspecified error.
	 */
	public void lui_matchmakingRejectedUnspecified() {
		//Log.d(TAG, "lui_matchmakingRejectedUnspecified");
		dialogManager.showDialog(DIALOG_MATCHMAKING_REJECTED_UNSPECIFIED_ID);

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingRejectedUnspecified(false);
	}

	/**
	 * We received a match, but then were unable to form a connection with that
	 * match.
	 */
	public void lui_matchmakingFailed() {
		//Log.d(TAG, "lui_matchmakingFailed");

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logMatchmakingUDPHolePunchFailed(false);
	}

	/**
	 * Hey, guess what? If we were having problems communicating with the
	 * server, everything's OK now. This is only a guaranteed call in the event
	 * that 1. Server communication seems OK, and 2. We previously called
	 * lui_peacerayServerCommunicationFailure()
	 */
	public void lui_peacerayServerCommunicationOK() {
		// this is a safe call, even if the specified dialog is not displayed.
		dialogManager.dismissDialog(DIALOG_LOBBY_WEB_COMMUNICATION_FAILURE);
	}

	/**
	 * We are having trouble communicating with the peaceray servers.
	 */
	public void lui_peacerayServerCommunicationFailure() {
		// put up a spinner? We don't want much happening
		// while this is going on...
		dialogManager.showDialog(DIALOG_LOBBY_WEB_COMMUNICATION_FAILURE);
	}

	/**
	 * According to the peaceray server(s), this lobby is closed.
	 */
	public void lui_peacerayServerLobbyClosed() {
		dialogManager.showDialog(DIALOG_LOBBY_WEB_CLOSED);
	}
	
	
	/**
	 * According to the peaceray server(s), this lobby is full.
	 */
	public void lui_peacerayServerLobbyFull() {
		dialogManager.showDialog(DIALOG_LOBBY_WEB_FULL) ;
	}
	
	
	/**
	 * According to the peaceray server(s), this lobby is not full.
	 */
	public void lui_peacerayServerLobbyNotFull() {
		dialogManager.dismissDialog(DIALOG_LOBBY_WEB_FULL) ;
	}
	

	/**
	 * We are attempting to connect to the server. This method is called every
	 * time we begin a conneciton attempt.
	 */
	public void lui_connectionToServerConnecting() {
		// Put up a connecting message
		dialogManager.showDialog(DIALOG_CONNECTING_TO_LOBBY_ID);
	}

	/**
	 * We failed to connect to the server (once). This method is called every
	 * time a connection attempt fails before we can connect.
	 */
	public void lui_connectionToServerFailedToConnect() {
		// TODO: Inform the user of the failure?
	}

	/**
	 * We have failed enough times that we're giving up connecting. Calling this
	 * method might prompt the Activity to end, or put up a "quit" message, or
	 * something.
	 */
	public void lui_connectionToServerGaveUpConnecting() {
		// TODO: Inform the user we've given up?
	}

	/**
	 * We have connected to the server (finally?) and we are currently
	 * negotiating the particulars of our connection. It is HIGHLY likely that
	 * gameConnectionConnected() will be called immediately after this method,
	 * since negotiation usually happens very quickly.
	 */
	public void lui_connectionToServerNegotiating() {
		// Put up a connecting message
		dialogManager.showDialog(DIALOG_TALKING_TO_LOBBY_ID);
	}

	/**
	 * We have successfully connect! The server has welcomed us.
	 */
	public void lui_connectionToServerConnected() {
		// Dismiss any network messages
		dialogManager.dismissDialogAtTier(DIALOG_TIER_NETWORK_STATUS);

		// We have a slot and a color; now's a reasonable time to update
		// buttons.
		setupButtonColor();

		// Nows the time to update the lobbyView.
		lobbyViewAdapter.update_lobby();
		
		if ( mShowLobbyInformationOnConnect ) {
			dialogManager.showDialog(DIALOG_LOBBY_INFORMATION) ;
			mShowLobbyInformationOnConnect = false ;
		}
	}

	/**
	 * This method is called if the Connection drops without warning. We call
	 * this method if we are Negotiating or Connected, and the connection
	 * unexpectedly drops.
	 */
	public void lui_connectionToServerDisconnectedUnexpectedly() {
		// TODO: Specific server disconnect message?
	}

	/**
	 * This method is called if the server kicked us. This is a Disconnect
	 * message; our Connection is broken. The user might be interested in seeing
	 * the message.
	 * 
	 * @param msg
	 */
	public void lui_connectionToServerKickedByServer(String msg) {
		Log.d(TAG, "lui_connectionToServerKickedByServer: " + msg);
		// TODO: Fill this in.
	}

	/**
	 * Another disconnect message; the server has closed forever. We will make
	 * no further attempts to connect because, as noted, the server closed
	 * FOREVER.
	 */
	public void lui_connectionToServerServerClosedForever() {
		Log.d(TAG, "lui_connectionToServerServerClosedForever");

		// TODO: Might need more complex behavior when a 3rd party
		// server closes a lobby.
		dialogManager.showDialog(DIALOG_HOST_CLOSED_LOBBY_FOREVER_ID);
		// STUB
	}

	/*
	 * ************************************************************************
	 * 
	 * LOBBY DISPLAY SPECIFICS
	 * 
	 * Allows the Service to set, retrieve, unset specific display information
	 * that is relevant to the server connection and possibly game launches. For
	 * example, player color comes in from the server and is sent directly to
	 * the LUI (not through a Lobby instance, because it is irrelevant to Lobby
	 * objects). We may want to retrieve that color later, for the purposes of
	 * game launches.
	 * 
	 * *************************************************************************
	 */
	
	
	/**
	 * Informs the GUI that the specified gameMode, which itself is among the lobby's
	 * game modes, should be available.
	 * 
	 * If the provided game mode is not premium content, this call should be ignored;
	 * all non-premium game modes are available by default.
	 * 
	 * If it is premium content, the availability should be determined by the
	 * provided boolean.  If 'false' you may want to either dim the VOTE button,
	 * or 
	 * 
	 */
	public void lui_setPremiumGameModeAvailable( int gameMode, boolean available ) {
		// the specified, premium game mode is available (according to 'available').
		boolean localAvailable = getPremiumLibrary().hasGameMode(gameMode) ;
		
		OptionAvailability oa = OptionAvailability.ENABLED ;
		if ( available && !localAvailable )
			oa = OptionAvailability.PROXY_UNLOCKED_ENABLED ;
		else if ( !available )
			oa = OptionAvailability.LOCKED_ENABLED ;
		
		lobbyViewAdapter.setGameModeAvailability(gameMode, oa) ;
	}
	

	/**
	 * Sets the color for use in displaying this player. Should be remembered
	 * until the next call to setPlayerColor.
	 */
	public void lui_setPlayerColor(int slot, int color) {
		((LobbyViewComponentAdapter) lobbyViewAdapter).setPlayerColor(slot,
				color);
		if (slot == lobby.getLocalPlayerSlot())
			setupButtonColor();
	}

	/**
	 * Retrieves the color most recently set for this player slot, or 0 if no
	 * color has been set.
	 * 
	 * @param slot
	 * @return
	 */
	public int lui_getPlayerColor(int slot) {
		return ((LobbyViewComponentAdapter) lobbyViewAdapter)
				.getPlayerColor(slot);
	}

	// used for selecting default colors.
	int[] mDefaultColors = null;

	/**
	 * Returns the default player color for this slot using the provided nonce
	 * and pNonce. The result of this function should be deterministic based on
	 * its parameters.
	 * 
	 * @param slot
	 * @param nonce
	 * @param personalNonce
	 * @return
	 */
	public int lui_getDefaultPlayerColor(boolean isWifi, int slot, Nonce nonce,
			Nonce personalNonce) {
		// Set the possible colors. We scramble up the R colors to avoid very
		// similar
		// colors together.
		if (mDefaultColors == null) {
			mDefaultColors = new int[] {
					res.getColor(R.color.standard_s0_fill),
					res.getColor(R.color.standard_s1_fill),
					res.getColor(R.color.standard_st_fill),
					res.getColor(R.color.standard_f0_fill),
					res.getColor(R.color.standard_r0_fill),
					res.getColor(R.color.standard_r4_fill),
					res.getColor(R.color.standard_r1_fill),
					res.getColor(R.color.standard_r5_fill),
					res.getColor(R.color.standard_r2_fill),
					res.getColor(R.color.standard_r6_fill),
					res.getColor(R.color.standard_r4_fill) };
		}

		// Simple enough. We use n as a random number seed, and add playerSlot
		// to the result.
		Random r = new Random((long) nonce.directByteAccess(0)
				* (long) nonce.directByteAccess(1));

		// For now, use S0, S1, ST, F0, R0-R6. Total is 11.
		int c = (r.nextInt(mDefaultColors.length) + slot)
				% mDefaultColors.length;
		// Log.d(TAG, "lcd_requestDefaultPlayerColor: c is " + c + " color is "
		// + mDefaultColors[c]) ;
		return mDefaultColors[c];
	}

	/*
	 * ************************************************************************
	 * 
	 * LOBBY CHANGES
	 * 
	 * These methods allow the UI to be informed of changes to the Lobby. It can
	 * thus update whatever views are displaying the Lobby information. Note the
	 * UI may want to make direct changes to the Lobby - and that's fine! These
	 * callbacks will be triggered (if appropriate) in this case, so the UI
	 * doesn't need to handle the display update immediately; things can go like
	 * User touch -> UI -> Service call -> Lobby change -> Lobby callback -> LUI
	 * call -> Display update.
	 * 
	 * *************************************************************************
	 */

	/**
	 * Called when the membership of the lobby has changed; someone entered or
	 * left, or a name change occurred.
	 */
	public void lui_updateLobbyMembership() {
		lobbyViewAdapter.update_players();
	}

	/**
	 * Called when new message(s) have appeared in the Lobby.
	 */
	public void lui_updateLobbyMessages() {
		// This data appears in the log, and we
		// have a callback for this specifically.
	}

	/**
	 * Called when the lobby's game modes have changed.
	 */
	public void lui_updateLobbyGameModes() {
		lobbyViewAdapter.update_basics();
	}

	/**
	 * Called when the lobby votes have changed.
	 */
	public void lui_updateLobbyVotes() {
		Log.d(TAG, "lui_updateLobbyVotes");
		lobbyViewAdapter.update_votes();
	}

	/**
	 * Called when lobby countdowns have updated.
	 */
	public void lui_updateLobbyCountdowns() {
		lobbyViewAdapter.update_launches();
	}

	/**
	 * Called when a new countdown has been added. Treat this as a call to
	 * "lui_updateLobbyCountdowns", but provides some additional information
	 * (for e.g. playing a sound effect).
	 */
	public void lui_updateLobbyCountdowns_newCountdown() {
		if (!mLUICountdownUpdateForLaunch && (activityShown || !lobbyService.getMovingToGame()) )
			mQuantroSoundPool.lobbyLaunchCountdownStart();
		lobbyViewAdapter.update_launches();
	}

	/**
	 * Called when a countdown has been CANCELED (NOT just removed, for example
	 * in preparation for a launch). Treat this as a call to
	 * "lui_updateLobbyCountdowns", but provides some additional information
	 * (for e.g. playing a sound effect).
	 */
	public void lui_updateLobbyCountdowns_cancelCountdown( boolean failure ) {
		Log.d(TAG, "cancel countdown");
		if (!mLUICountdownUpdateForLaunch && (activityShown || !lobbyService.getMovingToGame()))
			mQuantroSoundPool.lobbyLaunchCountdownCancel();
		lobbyViewAdapter.update_launches();
		// cancel toast.
		if ( activityShown && failure ) 
			Toast.makeText(this, R.string.in_lobby_toast_game_launch_failed, Toast.LENGTH_SHORT).show() ;
	}

	/**
	 * If we are going to make lobby countdown updates for the purposes of a
	 * game launch, we call this first and after.
	 * 
	 * @param forLaunch
	 */
	public void lui_updatingLobbyCountdownsForLaunch(boolean forLaunch) {
		mLUICountdownUpdateForLaunch = forLaunch;
	}

	/*
	 * ************************************************************************
	 * 
	 * LOBBY LAUNCHES
	 * 
	 * These methods inform the Activity of a Game launch. Note that the
	 * previous set of methods imply a change in Lobby data - users connected,
	 * votes changed, etc. - while these methods describe launches, which are
	 * major changes to the Lobby itself.
	 * 
	 * Convention: we leave the Service to handle most operations, with the
	 * Activity displaying useful messages and informing the Service of changes.
	 * 
	 * *************************************************************************
	 */

	/**
	 * A launch is occurring for which we will act as the host. Note that this
	 * is a USER INTERFACE call; we should display something appropriate to the
	 * fact that we are launching, but not actually launch the activity.
	 */
	public void lui_launchAsHost(int gameMode, boolean[] players) {
		// Display a "launching" dialog?
		mQuantroSoundPool.lobbyLaunch();
		if ( players == null )
			launchingWithPlayers = null ;
		else {
			launchingWithPlayers = new boolean[players.length] ;
			for ( int i = 0; i < players.length; i++ )
				launchingWithPlayers[i] = players[i] ;
		}
		dialogManager.showDialog(DIALOG_LAUNCHING_GAME_ID);
	}

	/**
	 * A launch is occurring for which we will participate as a client.
	 * 
	 * @param gameMode
	 * @param players
	 */
	public void lui_launchAsClient(int gameMode, boolean[] players) {
		mQuantroSoundPool.lobbyLaunch();
		if ( players == null )
			launchingWithPlayers = null ;
		else {
			launchingWithPlayers = new boolean[players.length] ;
			for ( int i = 0; i < players.length; i++ )
				launchingWithPlayers[i] = players[i] ;
		}
		dialogManager.showDialog(DIALOG_LAUNCHING_GAME_ID);
	}

	/**
	 * A launch is occurring for which we will not participate (we'll probably
	 * stay in the lobby during this time?)
	 * 
	 * @param gameMode
	 * @param players
	 */
	public void lui_launchAsAbsent(int gameMode, boolean[] players) {
		if ( activityShown ) {
			mQuantroSoundPool.lobbyLaunch() ;
			String msg = TextFormatting.format(
					this,
					res,
					TextFormatting.DISPLAY_TOAST,
					TextFormatting.TYPE_LOBBY_LAUNCHING_ABSENT,
					this.isHost() ? TextFormatting.ROLE_HOST : TextFormatting.ROLE_CLIENT,
					ArrayOps.clone(lobby.getPlayerNames()),
					ArrayOps.clone(players)) ;
			Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() ;
		}
	}

	/*
	 * ************************************************************************
	 * 
	 * LOBBY LOG
	 * 
	 * There is only one generic callback for LobbyLog updates. The LobbyService
	 * registers itself as the delegate, and will pass the message along to the
	 * LUI at that time.
	 * 
	 * *************************************************************************
	 */

	public void lui_lobbyLogNewEvent(LobbyLog lobbyLog, int id, int type) {
		lobbyViewAdapter.update_log();

		LobbyLog.Event event = lobbyLog.getEvent(id);
		if (type == LobbyLog.Event.CHAT && lobby != null
				&& event.slot != lobby.getLocalPlayerSlot()
				&& this.activityShown && mChatTabContent != null
				&& mChatTabContent.getVisibility() != View.VISIBLE) {
			Toast.makeText(this, event.name + ": " + event.text,
					Toast.LENGTH_SHORT).show();
		}

		if (QuantroPreferences.getAnalyticsActive(this))
			Analytics.logInLobbyLogEvent(mLip.isDirect(),
					LobbyLog.Event.eventTypeName(type));
	}

	// //////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////
	// LOBBY VIEW DELEGATE METHODS
	// //////////////////////////////////////////////////////////////////////////

	/**
	 * The user has changed their name.
	 */
	public void lvad_userSetName(String name) {
		Log.d(TAG, "lvd_userSetName(" + name + ")");
		// Set in the lobby client, and send the message.
		lobby.setLocalPlayerName(name);
		lobby.setName(lobby.getLocalPlayerSlot(), name);
		// TODO: Log the set name event in the log, once that event is
		// supported.
		lobbyViewAdapter.update_players();

		// Pass this to a service method for communication
		lobbyService.sendName(name);
	}

	/**
	 * The user has voted, or unvoted, for the specified game mode.
	 * 
	 * @param gameMode
	 * @param v
	 */
	public void lvad_userSetVote(int gameMode, boolean v) {
		Log.d(TAG, "lvd_userSetVote(" + gameMode + ", " + v + ")");
		// If this game mode is non-premium, or premium and availabily,
		// or the user is unvoting, set the vote.
		// Otherwise, prompt the user to purchase this game.
		boolean available = lobbyService.getGameModeIsAvailable( gameMode ) ;
		boolean availableLocalOnly = !available && getPremiumLibrary().hasGameMode(gameMode) ;
		
		if ( available || !v ) {
			// Set in the lobby client, and send the message.
			lobby.setLocalPlayerVoteForGameMode(gameMode, v);
			lobbyViewAdapter.update_myVotes();
	
			// Pass this to a service method for communication
			lobbyService.sendVote(gameMode, v);
		} else if ( availableLocalOnly ) {
			lobby.setLocalPlayerVoteForGameMode(gameMode, v) ;
			lobbyViewAdapter.update_myVotes() ;
			
			// Pass this to the service; make sure we force a premium update as well.
			lobbyService.updateGameModeAuthorization(gameMode, true, true) ;
			lobbyService.sendVote(gameMode, v) ;
		} 
	}

	/**
	 * The user has sent a text message.
	 * 
	 * @param msg
	 */
	public void lvad_userSentTextMessage(String msg) {
		Log.d(TAG, "lvd_userSentTextMessage(" + msg + ")");

		// We do NOT update the lobbyClient; we wait for the message to come
		// back.

		// Pass this to a service method for communication
		lobbyService.sendTextMessage(msg);
	}

	// //////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////
	// LOBBY TITLE BAR BUTTON STRIP LISTENER
	// //////////////////////////////////////////////////////////////////////////

	/**
	 * The user has short-clicked a button on this strip. If buttonNum == 0, be
	 * assured that it is the main button. If > 0, it is some other button, and
	 * as mentioned in the class description, the content and effect are
	 * completely up to the outside world to determine. If you need to check
	 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
	 * 
	 * The menuItemNumber provided is the currently displayed menu item
	 * according to the strip's records.
	 * 
	 * @param strip
	 * @param buttonType
	 * @param asOverflow
	 */
	public boolean ltbbs_onButtonClick(LobbyTitleBarButtonStrip strip,
			int buttonType, boolean asOverflow) {

		switch( buttonType ) {
		case LobbyTitleBarButtonStrip.BUTTON_TAB_CHAT:
				mChatTabContent.setVisibility(View.VISIBLE);
				mVoteTabContent.setVisibility(View.GONE);
				lobbyTitleBarButtonStrip.setActiveTab(buttonType);
				if (mControlsSound && !asOverflow)
					mQuantroSoundPool.menuButtonFlip();
				return true;
		case LobbyTitleBarButtonStrip.BUTTON_TAB_VOTE:
				mChatTabContent.setVisibility(View.GONE);
				mVoteTabContent.setVisibility(View.VISIBLE);
				lobbyTitleBarButtonStrip.setActiveTab(buttonType);
				if (mControlsSound && !asOverflow)
					mQuantroSoundPool.menuButtonFlip();
				return true;
		case LobbyTitleBarButtonStrip.BUTTON_SHARE:
				// Can only share InternetLobbies.
				if ( lobby == null || !(lobby instanceof InternetLobby) )
					return false ;
				
				userShare() ;
				
				if (mControlsSound && !asOverflow)
					mQuantroSoundPool.menuButtonClick();
				return true;
		case LobbyTitleBarButtonStrip.BUTTON_INFO:
			// show info!
			dialogManager.showDialog(DIALOG_LOBBY_INFORMATION) ;
			if (mControlsSound && !asOverflow)
				mQuantroSoundPool.menuButtonClick();
			return true ;
		case LobbyTitleBarButtonStrip.BUTTON_HELP:
			dialogManager.showDialog(GlobalDialog.DIALOG_ID_HELP) ;
			if (mControlsSound && !asOverflow)
				mQuantroSoundPool.menuButtonClick();
			return true;
			
		case LobbyTitleBarButtonStrip.BUTTON_SETTINGS:
			userActionLaunchSettings() ;
			if (mControlsSound && !asOverflow)
				mQuantroSoundPool.menuButtonClick();
			return true;
		}

		return false;
	}

	
	/**
	 * The user has long-clicked a button on this strip. If buttonNum == 0, be
	 * assured that it is the main button. If > 0, it is some other button, and
	 * as mentioned in the class description, the content and effect are
	 * completely up to the outside world to determine. If you need to check
	 * content, ID, or whatnot, you can use strip.getButton( buttonNum ).
	 * 
	 * The menuItemNumber provided is the currently displayed menu item
	 * according to the strip's records.
	 * 
	 * @param strip
	 * @param buttonType
	 */
	public boolean ltbbs_onButtonLongClick(LobbyTitleBarButtonStrip strip,
			int buttonType) {
		
		if ( buttonType == LobbyTitleBarButtonStrip.BUTTON_SHARE ) {
			userShareClipboard() ;
			if (mControlsSound)
				mQuantroSoundPool.menuButtonHold();
			return true ;
		}

		// otherwise long press does nothing. NOTHING!
		return false;
	}
	
	public void ltbbs_onOverflowClicked( LobbyTitleBarButtonStrip strip ) {
		if (mControlsSound)
			mQuantroSoundPool.menuButtonClick();
	}

	// //////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////
	// WIFI MONITOR CALLBACK
	// //////////////////////////////////////////////////////////////////////////

	private int wml_hasWifiIpAddress_lastIP = 0 ;
	@Override
	synchronized public void wml_hasWifiIpAddress(boolean hasIP, int ip) {
		if ( mWifiMonitor == null )
			return ;
		
		// oh look, an update. Put up a dialog, or don't.
		if ( hasIP && (mNoWifiDialogDisplayed || ip != wml_hasWifiIpAddress_lastIP) ) {
			dialogManager.dismissDialog(DIALOG_NO_WIFI_CONNECTION);
			mNoWifiDialogDisplayed = false;

			asyncIPUpdate( hasIP, ip ) ;
			wml_hasWifiIpAddress_lastIP = ip ;
			
			// /////////////////////////////////////////////
			// REGISTER FOR ANDROID BEAM
			updateAndroidBeamRegistration() ;
		} else if (!hasIP && !mNoWifiDialogDisplayed) {
			dialogManager.showDialog(DIALOG_NO_WIFI_CONNECTION);
			mNoWifiDialogDisplayed = true;

			asyncIPUpdate( hasIP, ip ) ;
		}

		mHandler.removeCallbacks(mWifiMonitor) ;
		if (activityShown)
			mHandler.postDelayed(mWifiMonitor, WIFI_MONITOR_UPDATE_FREQUENCY);
	}
	
	
	private void asyncIPUpdate( boolean hasIP, int ip ) {
		// we perform the update in an AsyncTask, because it's likely
		// to involve creating and accessing an InetSocketAddress.
		new AsyncTask<Integer, Object, Integer>() {
			@Override
			protected Integer doInBackground(Integer... params) {
				// our background task is to construct a InetSocketAddress
				// and set it for our lobby's DirectAddress.  Then, in
				// the foreground, we update the lobby basics.
				if ( params != null && params[0] != null ) {
					int ip = params[0].intValue() ;
					if (LobbyActivity.this.isHost()) {
						InetSocketAddress address = new InetSocketAddress(WifiMonitor
								.ipAddressToString(ip), mLip.localPort) ;
						Lobby lobby = LobbyActivity.this.lobby ;
						if ( lobby != null ) {
							LobbyActivity.this.lobby.setDirectAddress( address );
						}
					}
					
					return params[0] ;
				}
				return null ;
			}
			
			protected void onPostExecute(Integer result) {
				lobbyViewAdapter.update_basics();
			}
		}.execute(hasIP ? Integer.valueOf(ip) : null) ;
	}

	// //////////////////////////////////////////////////////////////////////////
	//
	// USER GUI ACTIONS
	//
	// Easy methods to allow certain user actions.
	//
	// //////////////////////////////////////////////////////////////////////////

	synchronized private void userActionLaunchSettings() {
		// User is opening settings! Silence music, pause,
		// open settings menu. Note that this will happen automagically
		// if we just open up the intent.
		Intent intent = new Intent(this, QuantroPreferences.class);
		intent.setAction(Intent.ACTION_MAIN);
		startActivity(intent);
	}
	
	
	synchronized private void userShareClipboard() {
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return ;
		
		// Get invitation string...
		String encodedLobby = LobbyStringEncoder.toFullString( (InternetLobby) lobby ) ;
		String label = res.getString(R.string.lobby_invitation_subject) ;
		String copyToast = res.getString(R.string.lobby_invitation_clipboard_toast) ;
		
		Clipboard.setText(this, label, encodedLobby) ;
		Toast.makeText(this, copyToast, Toast.LENGTH_SHORT).show() ;
	}
	
	
	synchronized private void userShare() {
		if ( lobby == null || !(lobby instanceof InternetLobby) )
			return ;
		
		// Get invitation string...
		String encodedLobby = LobbyStringEncoder.toFullString( (InternetLobby) lobby ) ;
		// The raw invitation can be clicked to access the lobby,
		// but it is NOT a good invitation message!  Include something
		// like "Let's play Quantro multiplayer!"
		// We have two different versions: 
		String invite = res.getString(R.string.lobby_invitation) ;
		String full_invite = res.getString(R.string.lobby_invitation_full) ;
		String subject = res.getString(R.string.lobby_invitation_subject) ;
		String encodedPlaceholder = res.getString(R.string.placeholder_lobby_string_encoding) ;
		String playLinkPlaceholder = res.getString(R.string.placeholder_google_play_link) ;
		String playLink = res.getString(R.string.google_play_link) ;
		invite = invite.replace(encodedPlaceholder, encodedLobby)
						.replace(playLinkPlaceholder, playLink) ;
		full_invite = full_invite.replace(encodedPlaceholder, encodedLobby)
						.replace(playLinkPlaceholder, playLink) ;
		
		// make an intent picker
		String query = res.getString(R.string.send_lobby_invitation_intent_query) ;
		String [] packageNamesFullInviteSupported = res.getStringArray(R.array.package_name_array_supports_long_invitation) ;
		String [] packageSubstringFullInviteSupported = res.getStringArray(R.array.package_substring_array_supports_long_invitation) ;
		
		ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this) ;
		builder.setText(invite) ;
		builder.setSubject(subject) ;
		// builder.setStream(iconURI) ;
		builder.setType("text/plain") ;
		Intent intent = builder.getIntent() ;
		
		// We have configured a short, subjectless Share Text intent.
		// Open all the resolvers to add subject and, for whitelisted
		// packages, the long intent data form.
		// Thanks to http://stackoverflow.com/questions/5734678/custom-filtering-of-intent-chooser-based-on-installed-android-package-name
		// for this implementation!
		List<Intent> targetedShareIntents = new ArrayList<Intent>();
        List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(intent, 0);
        if (!resInfo.isEmpty()){
            for (ResolveInfo resolveInfo : resInfo) {
                String packageName = resolveInfo.activityInfo.packageName;
                Intent targetedShareIntent = (Intent)intent.clone() ;
                	// we clone instead of creating a new Intent to copy
                	// the "from" information.  This info is used for certain
                	// apps, e.g. Google+, to tag posts with "From Quantro."
                	// I'm not sure what extras to manually include to duplicate this.
                targetedShareIntent.setType("text/plain");
                targetedShareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                // if packageName is in packageNamesFullInviteSupported,
                // set the EXTRA_TEXT to 'full_invite.'  Otherwise,
                // set it to 'invite.'
                boolean supportsFullInvitation = false ;
                for ( int i = 0; i < packageNamesFullInviteSupported.length; i++ ) {
                	if ( packageNamesFullInviteSupported[i] != null 
                			&& packageNamesFullInviteSupported[i].equals(packageName) )
                		supportsFullInvitation = true ;
                }
                for ( int i = 0; i < packageSubstringFullInviteSupported.length; i++ ) {
                	if ( packageName != null 
                			&& packageName.contains(packageSubstringFullInviteSupported[i]) )
                		supportsFullInvitation = true ;
                }
                
                if ( supportsFullInvitation ) 
                	targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, full_invite) ;
                else
                	targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, invite) ;

                
                targetedShareIntent.setPackage(packageName);
                targetedShareIntents.add(targetedShareIntent);
            }
            Intent chooserIntent = Intent.createChooser(
            		targetedShareIntents.remove(targetedShareIntents.size()-1), query);

            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));

            startActivity(chooserIntent);
        }
        else {
			// Open the chooser without any complications.
			startActivity( Intent.createChooser(intent, query) ) ;
        }
	}
	
	
	
	private void updateAchievementsGameOver( GameResult gr ) {
		if ( this.mLip.isDirect() ) {
			// wifi
			Achievements.lobby_gameOverWiFi(lobby, gr, gr.getLocalPlayerSlot()) ;
		} else if ( mLip.internetLobby.isPublic() ) {
			// public internet
			Achievements.lobby_gameOverPublicInternet(lobby, gr, gr.getLocalPlayerSlot()) ;
		} else if ( mLip.internetLobby.isItinerant() ) {
			// roaming
			Achievements.lobby_gameOverRoamingInternet(lobby, gr, gr.getLocalPlayerSlot()) ;
		} else {
			Achievements.lobby_gameOverPrivateInternet(lobby, gr, gr.getLocalPlayerSlot()) ;
		}
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
		return "help/lobby_activity.html" ;
	}
	
	@Override
	public String getHelpDialogContextName() {
		return getResources().getString(R.string.global_dialog_help_name_lobby) ;
	}
	
	//
	////////////////////////////////////////////////////////////////////////////
	

}
